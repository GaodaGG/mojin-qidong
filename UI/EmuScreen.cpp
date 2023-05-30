// Copyright (c) 2012- PPSSPP Project.

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 2.0 or later versions.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License 2.0 for more details.

// A copy of the GPL 2.0 should have been included with the program.
// If not, see http://www.gnu.org/licenses/

// Official git repository and contact information can be found at
// https://github.com/hrydgard/ppsspp and http://www.ppsspp.org/.

#include "ppsspp_config.h"

#include <algorithm>
#include <functional>

using namespace std::placeholders;

#include "Common/Render/TextureAtlas.h"
#include "Common/GPU/OpenGL/GLFeatures.h"
#include "Common/Render/Text/draw_text.h"
#include "Common/Battery/Battery.h"

#include "Common/UI/Root.h"
#include "Common/UI/UI.h"
#include "Common/UI/Context.h"
#include "Common/UI/Tween.h"
#include "Common/UI/View.h"
#include "Common/UI/AsyncImageFileView.h"
#include "Common/VR/PPSSPPVR.h"

#include "Common/Data/Text/I18n.h"
#include "Common/Input/InputState.h"
#include "Common/Log.h"
#include "Common/System/Display.h"
#include "Common/System/System.h"
#include "Common/System/NativeApp.h"
#include "Common/System/Request.h"
#include "Common/Profiler/Profiler.h"
#include "Common/Math/curves.h"
#include "Common/TimeUtil.h"

#ifndef MOBILE_DEVICE
#include "Core/AVIDump.h"
#endif
#include "Core/Config.h"
#include "Core/ConfigValues.h"
#include "Core/CoreTiming.h"
#include "Core/CoreParameter.h"
#include "Core/Core.h"
#include "Core/CwCheat.h"
#include "Core/KeyMap.h"
#include "Core/MemFault.h"
#include "Core/Reporting.h"
#include "Core/System.h"
#include "GPU/GPUState.h"
#include "GPU/GPUInterface.h"
#include "GPU/Common/FramebufferManagerCommon.h"
#if !PPSSPP_PLATFORM(UWP)
#include "GPU/Vulkan/DebugVisVulkan.h"
#endif
#include "Core/HLE/sceCtrl.h"
#include "Core/HLE/sceSas.h"
#include "Core/Debugger/SymbolMap.h"
#include "Core/SaveState.h"
#include "Core/MIPS/MIPS.h"
#include "Core/HLE/__sceAudio.h"
#include "Core/HLE/proAdhoc.h"
#include "Core/HLE/Plugins.h"
#include "Core/HW/Display.h"

#include "UI/BackgroundAudio.h"
#include "UI/OnScreenDisplay.h"
#include "UI/GamepadEmu.h"
#include "UI/PauseScreen.h"
#include "UI/MainScreen.h"
#include "UI/EmuScreen.h"
#include "UI/DevScreens.h"
#include "UI/GameInfoCache.h"
#include "UI/MiscScreens.h"
#include "UI/ControlMappingScreen.h"
#include "UI/DisplayLayoutScreen.h"
#include "UI/GameSettingsScreen.h"
#include "UI/ProfilerDraw.h"
#include "UI/DiscordIntegration.h"
#include "UI/ChatScreen.h"

#include "Core/Reporting.h"

#if PPSSPP_PLATFORM(WINDOWS) && !PPSSPP_PLATFORM(UWP)
#include "Windows/MainWindow.h"
#endif

#ifndef MOBILE_DEVICE
static AVIDump avi;
#endif

// TODO: Ugly!
static bool frameStep_;
static int lastNumFlips;
static bool startDumping;

extern bool g_TakeScreenshot;

static void __EmuScreenVblank()
{
	auto sy = GetI18NCategory(I18NCat::SYSTEM);

	if (frameStep_ && lastNumFlips != gpuStats.numFlips)
	{
		frameStep_ = false;
		Core_EnableStepping(true, "ui.frameAdvance", 0);
		lastNumFlips = gpuStats.numFlips;
	}
#ifndef MOBILE_DEVICE
	if (g_Config.bDumpFrames && !startDumping)
	{
		avi.Start(PSP_CoreParameter().renderWidth, PSP_CoreParameter().renderHeight);
		osm.Show(sy->T("AVI Dump started."), 0.5f);
		startDumping = true;
	}
	if (g_Config.bDumpFrames && startDumping)
	{
		avi.AddFrame();
	}
	else if (!g_Config.bDumpFrames && startDumping)
	{
		avi.Stop();
		osm.Show(sy->T("AVI Dump stopped."), 1.0f);
		startDumping = false;
	}
#endif
}

// Handles control rotation due to internal screen rotation.
static void SetPSPAnalog(int stick, float x, float y) {
	switch (g_Config.iInternalScreenRotation) {
	case ROTATION_LOCKED_HORIZONTAL:
		// Standard rotation. No change.
		break;
	case ROTATION_LOCKED_HORIZONTAL180:
		x = -x;
		y = -y;
		break;
	case ROTATION_LOCKED_VERTICAL:
	{
		float new_y = -x;
		x = y;
		y = new_y;
		break;
	}
	case ROTATION_LOCKED_VERTICAL180:
	{
		float new_y = y = x;
		x = -y;
		y = new_y;
		break;
	}
	default:
		break;
	}
	__CtrlSetAnalogXY(stick, x, y);
}

EmuScreen::EmuScreen(const Path &filename)
	: gamePath_(filename) {
	saveStateSlot_ = SaveState::GetCurrentSlot();
	__DisplayListenVblank(__EmuScreenVblank);
	frameStep_ = false;
	lastNumFlips = gpuStats.numFlips;
	startDumping = false;
	controlMapper_.SetCallbacks(
		std::bind(&EmuScreen::onVKey, this, _1, _2),
		std::bind(&EmuScreen::onVKeyAnalog, this, _1, _2),
		[](uint32_t bitsToSet, uint32_t bitsToClear) {
			__CtrlUpdateButtons(bitsToSet, bitsToClear);
		},
		&SetPSPAnalog,
		nullptr);

	// Make sure we don't leave it at powerdown after the last game.
	// TODO: This really should be handled elsewhere if it isn't.
	if (coreState == CORE_POWERDOWN)
		coreState = CORE_STEPPING;

	OnDevMenu.Handle(this, &EmuScreen::OnDevTools);
	OnChatMenu.Handle(this, &EmuScreen::OnChat);

	// Usually, we don't want focus movement enabled on this screen, so disable on start.
	// Only if you open chat or dev tools do we want it to start working.
	UI::EnableFocusMovement(false);
}

bool EmuScreen::bootAllowStorage(const Path &filename) {
	// No permissions needed.  The easy life.
	if (filename.Type() == PathType::HTTP)
		return true;

	if (!System_GetPropertyBool(SYSPROP_SUPPORTS_PERMISSIONS))
		return true;

	PermissionStatus status = System_GetPermissionStatus(SYSTEM_PERMISSION_STORAGE);
	switch (status) {
	case PERMISSION_STATUS_UNKNOWN:
		System_AskForPermission(SYSTEM_PERMISSION_STORAGE);
		return false;

	case PERMISSION_STATUS_DENIED:
		stopRender_ = true;
		screenManager()->switchScreen(new MainScreen());
		return false;

	case PERMISSION_STATUS_PENDING:
		// Keep waiting.
		return false;

	case PERMISSION_STATUS_GRANTED:
		return true;
	}

	_assert_(false);
	return false;
}

void EmuScreen::bootGame(const Path &filename) {
	if (PSP_IsRebooting())
		return;
	if (PSP_IsInited()) {
		bootPending_ = false;
		invalid_ = false;
		bootComplete();
		return;
	}

	if (PSP_IsIniting()) {
		std::string error_string;
		bootPending_ = !PSP_InitUpdate(&error_string);
		if (!bootPending_) {
			invalid_ = !PSP_IsInited();
			if (invalid_) {
				errorMessage_ = error_string;
				ERROR_LOG(BOOT, "%s", errorMessage_.c_str());
				return;
			}
			bootComplete();
		}
		return;
	}

	g_BackgroundAudio.SetGame(Path());

	// Check permission status first, in case we came from a shortcut.
	if (!bootAllowStorage(filename))
		return;

	auto sc = GetI18NCategory(I18NCat::SCREEN);

	invalid_ = true;

	// We don't want to boot with the wrong game specific config, so wait until info is ready.
	std::shared_ptr<GameInfo> info = g_gameInfoCache->GetInfo(nullptr, filename, 0);
	if (!info || info->pending)
		return;

	SetExtraAssertInfo((info->id + " " + info->GetTitle()).c_str());

	if (!info->id.empty()) {
		g_Config.loadGameConfig(info->id, info->GetTitle());
		// Reset views in case controls are in a different place.
		RecreateViews();

		g_Discord.SetPresenceGame(info->GetTitle().c_str());
	} else {
		g_Discord.SetPresenceGame(sc->T("Untitled PSP game"));
	}

	CoreParameter coreParam{};
	coreParam.cpuCore = (CPUCore)g_Config.iCpuCore;
	coreParam.gpuCore = GPUCORE_GLES;
	switch (GetGPUBackend()) {
	case GPUBackend::DIRECT3D11:
		coreParam.gpuCore = GPUCORE_DIRECTX11;
		break;
#if !PPSSPP_PLATFORM(UWP)
#if PPSSPP_API(ANY_GL)
	case GPUBackend::OPENGL:
		coreParam.gpuCore = GPUCORE_GLES;
		break;
#endif
	case GPUBackend::DIRECT3D9:
		coreParam.gpuCore = GPUCORE_DIRECTX9;
		break;
	case GPUBackend::VULKAN:
		coreParam.gpuCore = GPUCORE_VULKAN;
		break;
#endif
	}

	// Preserve the existing graphics context.
	coreParam.graphicsContext = PSP_CoreParameter().graphicsContext;
	coreParam.enableSound = g_Config.bEnableSound;
	coreParam.fileToStart = filename;
	coreParam.mountIso.clear();
	coreParam.mountRoot.clear();
	coreParam.startBreak = !g_Config.bAutoRun;
	coreParam.headLess = false;

	if (g_Config.iInternalResolution == 0) {
		coreParam.renderWidth = g_display.pixel_xres;
		coreParam.renderHeight = g_display.pixel_yres;
	} else {
		if (g_Config.iInternalResolution < 0)
			g_Config.iInternalResolution = 1;
		coreParam.renderWidth = 480 * g_Config.iInternalResolution;
		coreParam.renderHeight = 272 * g_Config.iInternalResolution;
	}
	coreParam.pixelWidth = g_display.pixel_xres;
	coreParam.pixelHeight = g_display.pixel_yres;

	std::string error_string;
	if (!PSP_InitStart(coreParam, &error_string)) {
		bootPending_ = false;
		invalid_ = true;
		errorMessage_ = error_string;
		ERROR_LOG(BOOT, "%s", errorMessage_.c_str());
	}

	if (PSP_CoreParameter().compat.flags().RequireBufferedRendering && g_Config.bSkipBufferEffects) {
		auto gr = GetI18NCategory(I18NCat::GRAPHICS);
		System_NotifyUserMessage(gr->T("BufferedRenderingRequired", "Warning: This game requires Rendering Mode to be set to Buffered."), 15.0f);
	}

	if (PSP_CoreParameter().compat.flags().RequireBlockTransfer && g_Config.bSkipGPUReadbacks) {
		auto gr = GetI18NCategory(I18NCat::GRAPHICS);
		System_NotifyUserMessage(gr->T("BlockTransferRequired", "Warning: This game requires Simulate Block Transfer Mode to be set to On."), 15.0f);
	}

	if (PSP_CoreParameter().compat.flags().RequireDefaultCPUClock && g_Config.iLockedCPUSpeed != 0) {
		auto gr = GetI18NCategory(I18NCat::GRAPHICS);
		System_NotifyUserMessage(gr->T("DefaultCPUClockRequired", "Warning: This game requires the CPU clock to be set to default."), 15.0f);
	}

	loadingViewColor_->Divert(0xFFFFFFFF, 0.75f);
	loadingViewVisible_->Divert(UI::V_VISIBLE, 0.75f);

	screenManager()->getDrawContext()->ResetStats();
}

void EmuScreen::bootComplete() {
	UpdateUIState(UISTATE_INGAME);
	System_Notify(SystemNotification::BOOT_DONE);
	System_Notify(SystemNotification::DISASSEMBLY);

	NOTICE_LOG(BOOT, "Loading %s...", PSP_CoreParameter().fileToStart.c_str());
	autoLoad();

	auto sc = GetI18NCategory(I18NCat::SCREEN);

#ifndef MOBILE_DEVICE
	if (g_Config.bFirstRun) {
		osm.Show(sc->T("PressESC", "Press ESC to open the pause menu"), 3.0f);
	}
#endif

#if !PPSSPP_PLATFORM(UWP)
	if (GetGPUBackend() == GPUBackend::OPENGL) {
		const char *renderer = gl_extensions.model;
		if (strstr(renderer, "Chainfire3D") != 0) {
			osm.Show(sc->T("Chainfire3DWarning", "WARNING: Chainfire3D detected, may cause problems"), 10.0f, 0xFF30a0FF, -1, true);
		} else if (strstr(renderer, "GLTools") != 0) {
			osm.Show(sc->T("GLToolsWarning", "WARNING: GLTools detected, may cause problems"), 10.0f, 0xFF30a0FF, -1, true);
		}

		if (g_Config.bGfxDebugOutput) {
			osm.Show("WARNING: GfxDebugOutput is enabled via ppsspp.ini. Things may be slow.", 10.0f, 0xFF30a0FF, -1, true);
		}
	}
#endif

	if (Core_GetPowerSaving()) {
		auto sy = GetI18NCategory(I18NCat::SYSTEM);
#ifdef __ANDROID__
		osm.Show(sy->T("WARNING: Android battery save mode is on"), 2.0f, 0xFFFFFF, -1, true, "core_powerSaving");
#else
		osm.Show(sy->T("WARNING: Battery save mode is on"), 2.0f, 0xFFFFFF, -1, true, "core_powerSaving");
#endif
	}

	saveStateSlot_ = SaveState::GetCurrentSlot();

	loadingViewColor_->Divert(0x00FFFFFF, 0.2f);
	loadingViewVisible_->Divert(UI::V_INVISIBLE, 0.2f);
}

EmuScreen::~EmuScreen() {
	if (!invalid_ || bootPending_) {
		// If we were invalid, it would already be shutdown.
		PSP_Shutdown();
	}

	SetExtraAssertInfo(nullptr);

#ifndef MOBILE_DEVICE
	if (g_Config.bDumpFrames && startDumping)
	{
		avi.Stop();
		osm.Show("AVI Dump stopped.", 1.0f);
		startDumping = false;
	}
#endif

	if (GetUIState() == UISTATE_EXIT)
		g_Discord.ClearPresence();
	else
		g_Discord.SetPresenceMenu();
}

void EmuScreen::dialogFinished(const Screen *dialog, DialogResult result) {
	// TODO: improve the way with which we got commands from PauseMenu.
	// DR_CANCEL/DR_BACK means clicked on "continue", DR_OK means clicked on "back to menu",
	// DR_YES means a message sent to PauseMenu by NativeMessageReceived.
	if (result == DR_OK || quit_) {
		screenManager()->switchScreen(new MainScreen());
		quit_ = false;
	}
	// Returning to the PauseScreen, unless we're stepping, means we should go back to controls.
	if (Core_IsActive())
		UI::EnableFocusMovement(false);
	RecreateViews();
}

static void AfterSaveStateAction(SaveState::Status status, const std::string &message, void *) {
	if (!message.empty() && (!g_Config.bDumpFrames || !g_Config.bDumpVideoOutput)) {
		osm.Show(message, status == SaveState::Status::SUCCESS ? 2.0 : 5.0);
	}
}

static void AfterStateBoot(SaveState::Status status, const std::string &message, void *ignored) {
	AfterSaveStateAction(status, message, ignored);
	Core_EnableStepping(false);
	System_Notify(SystemNotification::DISASSEMBLY);
}

void EmuScreen::sendMessage(const char *message, const char *value) {
	// External commands, like from the Windows UI.
	if (!strcmp(message, "pause") && screenManager()->topScreen() == this) {
		screenManager()->push(new GamePauseScreen(gamePath_));
	} else if (!strcmp(message, "stop")) {
		// We will push MainScreen in update().
		PSP_Shutdown();
		bootPending_ = false;
		stopRender_ = true;
		invalid_ = true;
		System_Notify(SystemNotification::DISASSEMBLY);
	} else if (!strcmp(message, "reset")) {
		PSP_Shutdown();
		bootPending_ = true;
		invalid_ = true;
		System_Notify(SystemNotification::DISASSEMBLY);

		std::string resetError;
		if (!PSP_InitStart(PSP_CoreParameter(), &resetError)) {
			ERROR_LOG(LOADER, "Error resetting: %s", resetError.c_str());
			stopRender_ = true;
			screenManager()->switchScreen(new MainScreen());
			return;
		}
	} else if (!strcmp(message, "boot")) {
		const char *ext = strrchr(value, '.');
		if (ext != nullptr && !strcmp(ext, ".ppst")) {
			SaveState::Load(Path(value), -1, &AfterStateBoot);
		} else {
			PSP_Shutdown();
			bootPending_ = true;
			gamePath_ = Path(value);
			// Don't leave it on CORE_POWERDOWN, we'll sometimes aggressively bail.
			Core_UpdateState(CORE_POWERUP);
		}
	} else if (!strcmp(message, "config_loaded")) {
		// In case we need to position touch controls differently.
		RecreateViews();
	} else if (!strcmp(message, "control mapping") && screenManager()->topScreen() == this) {
		UpdateUIState(UISTATE_PAUSEMENU);
		screenManager()->push(new ControlMappingScreen(gamePath_));
	} else if (!strcmp(message, "display layout editor") && screenManager()->topScreen() == this) {
		UpdateUIState(UISTATE_PAUSEMENU);
		screenManager()->push(new DisplayLayoutScreen(gamePath_));
	} else if (!strcmp(message, "settings") && screenManager()->topScreen() == this) {
		UpdateUIState(UISTATE_PAUSEMENU);
		screenManager()->push(new GameSettingsScreen(gamePath_));
	} else if (!strcmp(message, "gpu dump next frame")) {
		if (gpu)
			gpu->DumpNextFrame();
	} else if (!strcmp(message, "clear jit")) {
		currentMIPS->ClearJitCache();
		if (PSP_IsInited()) {
			currentMIPS->UpdateCore((CPUCore)g_Config.iCpuCore);
		}
	} else if (!strcmp(message, "window minimized")) {
		if (!strcmp(value, "true")) {
			gstate_c.skipDrawReason |= SKIPDRAW_WINDOW_MINIMIZED;
		} else {
			gstate_c.skipDrawReason &= ~SKIPDRAW_WINDOW_MINIMIZED;
		}
	} else if (!strcmp(message, "chat screen")) {
		if (g_Config.bEnableNetworkChat) {
			if (!chatButton_)
				RecreateViews();

#if defined(USING_WIN_UI)
			//temporary workaround for hotkey its freeze the ui when open chat screen using hotkey and native keyboard is enable
			if (g_Config.bBypassOSKWithKeyboard) {
				osm.Show("Disable windows native keyboard options to use ctrl + c hotkey", 2.0f);
			} else {
				UI::EventParams e{};
				OnChatMenu.Trigger(e);
			}
#else
			UI::EventParams e{};
			OnChatMenu.Trigger(e);
#endif
		}
	} else if (!strcmp(message, "app_resumed") && screenManager()->topScreen() == this) {
		if (System_GetPropertyInt(SYSPROP_DEVICE_TYPE) == DEVICE_TYPE_TV) {
			if (!KeyMap::IsKeyMapped(DEVICE_ID_PAD_0, VIRTKEY_PAUSE) || !KeyMap::IsKeyMapped(DEVICE_ID_PAD_1, VIRTKEY_PAUSE)) {
				// If it's a TV (so no built-in back button), and there's no back button mapped to a pad,
				// use this as the fallback way to get into the menu.

				screenManager()->push(new GamePauseScreen(gamePath_));
			}
		}
	}
}

void EmuScreen::UnsyncTouch(const TouchInput &touch) {
	Core_NotifyActivity();

	if (chatMenu_ && chatMenu_->GetVisibility() == UI::V_VISIBLE) {
		// Avoid pressing touch button behind the chat
		if (chatMenu_->Contains(touch.x, touch.y)) {
			chatMenu_->Touch(touch);
			return;
		} else if ((touch.flags & TOUCH_DOWN) != 0) {
			chatMenu_->Close();
			if (chatButton_)
				chatButton_->SetVisibility(UI::V_VISIBLE);
			UI::EnableFocusMovement(false);
		}
	}

	if (root_) {
		root_->Touch(touch);
	}
}

void EmuScreen::onVKey(int virtualKeyCode, bool down) {
	auto sc = GetI18NCategory(I18NCat::SCREEN);
	auto mc = GetI18NCategory(I18NCat::MAPPABLECONTROLS);

	switch (virtualKeyCode) {
	case VIRTKEY_FASTFORWARD:
		if (down) {
			if (coreState == CORE_STEPPING) {
				Core_EnableStepping(false);
			}
			PSP_CoreParameter().fastForward = true;
		} else {
			PSP_CoreParameter().fastForward = false;
		}
		break;

	case VIRTKEY_SPEED_TOGGLE:
		if (down) {
			// Cycle through enabled speeds.
			if (PSP_CoreParameter().fpsLimit == FPSLimit::NORMAL && g_Config.iFpsLimit1 >= 0) {
				PSP_CoreParameter().fpsLimit = FPSLimit::CUSTOM1;
				osm.Show(sc->T("fixed", "Speed: alternate"), 1.0);
			} else if (PSP_CoreParameter().fpsLimit == FPSLimit::CUSTOM1 && g_Config.iFpsLimit2 >= 0) {
				PSP_CoreParameter().fpsLimit = FPSLimit::CUSTOM2;
				osm.Show(sc->T("SpeedCustom2", "Speed: alternate 2"), 1.0);
			} else if (PSP_CoreParameter().fpsLimit == FPSLimit::CUSTOM1 || PSP_CoreParameter().fpsLimit == FPSLimit::CUSTOM2) {
				PSP_CoreParameter().fpsLimit = FPSLimit::NORMAL;
				osm.Show(sc->T("standard", "Speed: standard"), 1.0);
			}
		}
		break;

	case VIRTKEY_SPEED_CUSTOM1:
		if (down) {
			if (PSP_CoreParameter().fpsLimit == FPSLimit::NORMAL) {
				PSP_CoreParameter().fpsLimit = FPSLimit::CUSTOM1;
				osm.Show(sc->T("fixed", "Speed: alternate"), 1.0);
			}
		} else {
			if (PSP_CoreParameter().fpsLimit == FPSLimit::CUSTOM1) {
				PSP_CoreParameter().fpsLimit = FPSLimit::NORMAL;
				osm.Show(sc->T("standard", "Speed: standard"), 1.0);
			}
		}
		break;
	case VIRTKEY_SPEED_CUSTOM2:
		if (down) {
			if (PSP_CoreParameter().fpsLimit == FPSLimit::NORMAL) {
				PSP_CoreParameter().fpsLimit = FPSLimit::CUSTOM2;
				osm.Show(sc->T("SpeedCustom2", "Speed: alternate 2"), 1.0);
			}
		} else {
			if (PSP_CoreParameter().fpsLimit == FPSLimit::CUSTOM2) {
				PSP_CoreParameter().fpsLimit = FPSLimit::NORMAL;
				osm.Show(sc->T("standard", "Speed: standard"), 1.0);
			}
		}
		break;

	case VIRTKEY_PAUSE:
		if (down) {
			// Trigger on key-up to partially avoid repetition problems.
			// This is needed whenever we pop up a menu since the mapper
			// might miss  the key-up. Same as VIRTKEY_OPENCHAT.
			pauseTrigger_ = true;
			controlMapper_.ForceReleaseVKey(virtualKeyCode);
		}
		break;

	case VIRTKEY_FRAME_ADVANCE:
		if (down) {
			// If game is running, pause emulation immediately. Otherwise, advance a single frame.
			if (Core_IsStepping()) {
				frameStep_ = true;
				Core_EnableStepping(false);
			} else if (!frameStep_) {
				Core_EnableStepping(true, "ui.frameAdvance", 0);
			}
		}
		break;

	case VIRTKEY_OPENCHAT:
		if (down && g_Config.bEnableNetworkChat) {
			UI::EventParams e{};
			OnChatMenu.Trigger(e);
			controlMapper_.ForceReleaseVKey(virtualKeyCode);
		}
		break;

	case VIRTKEY_AXIS_SWAP:
		if (down) {
			controlMapper_.ToggleSwapAxes();
			osm.Show(mc->T("AxisSwap"));  // best string we have.
		}
		break;

	case VIRTKEY_DEVMENU:
		if (down) {
			UI::EventParams e{};
			OnDevMenu.Trigger(e);
		}
		break;

#ifndef MOBILE_DEVICE
	case VIRTKEY_RECORD:
		if (down) {
			if (g_Config.bDumpFrames == g_Config.bDumpAudio) {
				g_Config.bDumpFrames = !g_Config.bDumpFrames;
				g_Config.bDumpAudio = !g_Config.bDumpAudio;
			} else {
				// This hotkey should always toggle both audio and video together.
				// So let's make sure that's the only outcome even if video OR audio was already being dumped.
				if (g_Config.bDumpFrames) {
					AVIDump::Stop();
					AVIDump::Start(PSP_CoreParameter().renderWidth, PSP_CoreParameter().renderHeight);
					g_Config.bDumpAudio = true;
				} else {
					WAVDump::Reset();
					g_Config.bDumpFrames = true;
				}
			}
		}
		break;
#endif

	case VIRTKEY_REWIND:
		if (down) {
			if (SaveState::CanRewind()) {
				SaveState::Rewind(&AfterSaveStateAction);
			} else {
				osm.Show(sc->T("norewind", "No rewind save states available"), 2.0);
			}
		}
		break;
	case VIRTKEY_SAVE_STATE:
		if (down)
			SaveState::SaveSlot(gamePath_, g_Config.iCurrentStateSlot, &AfterSaveStateAction);
		break;
	case VIRTKEY_LOAD_STATE:
		if (down)
			SaveState::LoadSlot(gamePath_, g_Config.iCurrentStateSlot, &AfterSaveStateAction);
		break;
	case VIRTKEY_PREVIOUS_SLOT:
		if (down) {
			SaveState::PrevSlot();
			NativeMessageReceived("savestate_displayslot", "");
		}
		break;
	case VIRTKEY_NEXT_SLOT:
		if (down) {
			SaveState::NextSlot();
			NativeMessageReceived("savestate_displayslot", "");
		}
		break;
	case VIRTKEY_TOGGLE_FULLSCREEN:
		if (down)
			System_ToggleFullscreenState("");
		break;

	case VIRTKEY_SCREENSHOT:
		if (down)
			g_TakeScreenshot = true;
		break;

	case VIRTKEY_TEXTURE_DUMP:
		if (down) {
			g_Config.bSaveNewTextures = !g_Config.bSaveNewTextures;
			if (g_Config.bSaveNewTextures) {
				osm.Show(sc->T("saveNewTextures_true", "Textures will now be saved to your storage"), 2.0);
				NativeMessageReceived("gpu_configChanged", "");
			} else {
				osm.Show(sc->T("saveNewTextures_false", "Texture saving was disabled"), 2.0);
			}
		}
		break;
	case VIRTKEY_TEXTURE_REPLACE:
		if (down) {
			g_Config.bReplaceTextures = !g_Config.bReplaceTextures;
			if (g_Config.bReplaceTextures)
				osm.Show(sc->T("replaceTextures_true", "Texture replacement enabled"), 2.0);
			else
				osm.Show(sc->T("replaceTextures_false", "Textures no longer are being replaced"), 2.0);
			NativeMessageReceived("gpu_configChanged", "");
		}
		break;
	case VIRTKEY_RAPID_FIRE:
		__CtrlSetRapidFire(down);
		break;
	case VIRTKEY_MUTE_TOGGLE:
		if (down)
			g_Config.bEnableSound = !g_Config.bEnableSound;
		break;
	case VIRTKEY_SCREEN_ROTATION_VERTICAL:
		if (down)
			g_Config.iInternalScreenRotation = ROTATION_LOCKED_VERTICAL;
		break;
	case VIRTKEY_SCREEN_ROTATION_VERTICAL180:
		if (down)
			g_Config.iInternalScreenRotation = ROTATION_LOCKED_VERTICAL180;
		break;
	case VIRTKEY_SCREEN_ROTATION_HORIZONTAL:
		if (down)
			g_Config.iInternalScreenRotation = ROTATION_LOCKED_HORIZONTAL;
		break;
	case VIRTKEY_SCREEN_ROTATION_HORIZONTAL180:
		if (down)
			g_Config.iInternalScreenRotation = ROTATION_LOCKED_HORIZONTAL180;
		break;
	}
}

void EmuScreen::onVKeyAnalog(int virtualKeyCode, float value) {
	if (virtualKeyCode != VIRTKEY_SPEED_ANALOG) {
		return;
	}

	// We only handle VIRTKEY_SPEED_ANALOG here.

	// Xbox controllers need a pretty big deadzone here to not leave behind small values
	// on occasion when releasing the trigger. Still feels right.
	static constexpr float DEADZONE_THRESHOLD = 0.2f;
	static constexpr float DEADZONE_SCALE = 1.0f / (1.0f - DEADZONE_THRESHOLD);

	FPSLimit &limitMode = PSP_CoreParameter().fpsLimit;
	// If we're using an alternate speed already, let that win.
	if (limitMode != FPSLimit::NORMAL && limitMode != FPSLimit::ANALOG)
		return;
	// Don't even try if the limit is invalid.
	if (g_Config.iAnalogFpsLimit <= 0)
		return;

	// Apply a small deadzone (against the resting position.)
	value = std::max(0.0f, (value - DEADZONE_THRESHOLD) * DEADZONE_SCALE);

	// If target is above 60, value is how much to speed up over 60.  Otherwise, it's how much slower.
	// So normalize the target.
	int target = g_Config.iAnalogFpsLimit - 60;
	PSP_CoreParameter().analogFpsLimit = 60 + (int)(target * value);

	// If we've reset back to normal, turn it off.
	limitMode = PSP_CoreParameter().analogFpsLimit == 60 ? FPSLimit::NORMAL : FPSLimit::ANALOG;
}

bool EmuScreen::UnsyncKey(const KeyInput &key) {
	Core_NotifyActivity();

	if (UI::IsFocusMovementEnabled()) {
		if (UIScreen::UnsyncKey(key)) {
			return true;
		} else if ((key.flags & KEY_DOWN) != 0 && UI::IsEscapeKey(key)) {
			if (chatMenu_)
				chatMenu_->Close();
			if (chatButton_)
				chatButton_->SetVisibility(UI::V_VISIBLE);
			UI::EnableFocusMovement(false);
			return true;
		}
	}

	return controlMapper_.Key(key, &pauseTrigger_);
}

void EmuScreen::UnsyncAxis(const AxisInput &axis) {
	Core_NotifyActivity();

	return controlMapper_.Axis(axis);
}

class GameInfoBGView : public UI::InertView {
public:
	GameInfoBGView(const Path &gamePath, UI::LayoutParams *layoutParams) : InertView(layoutParams), gamePath_(gamePath) {
	}

	void Draw(UIContext &dc) override {
		// Should only be called when visible.
		std::shared_ptr<GameInfo> ginfo = g_gameInfoCache->GetInfo(dc.GetDrawContext(), gamePath_, GAMEINFO_WANTBG);
		dc.Flush();

		// PIC1 is the loading image, so let's only draw if it's available.
		if (ginfo && ginfo->pic1.texture) {
			Draw::Texture *texture = ginfo->pic1.texture->GetTexture();
			if (texture) {
				dc.GetDrawContext()->BindTexture(0, texture);

				double loadTime = ginfo->pic1.timeLoaded;
				uint32_t color = alphaMul(color_, ease((time_now_d() - loadTime) * 3));
				dc.Draw()->DrawTexRect(dc.GetBounds(), 0, 0, 1, 1, color);
				dc.Flush();
				dc.RebindTexture();
			}
		}
	}

	std::string DescribeText() const override {
		return "";
	}

	void SetColor(uint32_t c) {
		color_ = c;
	}

protected:
	Path gamePath_;
	uint32_t color_ = 0xFFC0C0C0;
};

// TODO: Shouldn't actually need bounds for this, Anchor can center too.
static UI::AnchorLayoutParams *AnchorInCorner(const Bounds &bounds, int corner, float xOffset, float yOffset) {
	using namespace UI;
	switch (g_Config.iChatButtonPosition) {
	case 0:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, xOffset, NONE, NONE, yOffset, true);
	case 1:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, bounds.centerX(), NONE, NONE, yOffset, true);
	case 2:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, NONE, NONE, xOffset, yOffset, true);
	case 3:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, xOffset, yOffset, NONE, NONE, true);
	case 4:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, bounds.centerX(), yOffset, NONE, NONE, true);
	case 5:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, NONE, yOffset, xOffset, NONE, true);
	case 6:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, xOffset, bounds.centerY(), NONE, NONE, true);
	case 7:  return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, NONE, bounds.centerY(), xOffset, NONE, true);
	default: return new AnchorLayoutParams(WRAP_CONTENT, WRAP_CONTENT, xOffset, NONE, NONE, yOffset, true);
	}
}

void EmuScreen::CreateViews() {
	using namespace UI;

	auto dev = GetI18NCategory(I18NCat::DEVELOPER);
	auto n = GetI18NCategory(I18NCat::NETWORKING);
	auto sc = GetI18NCategory(I18NCat::SCREEN);

	const Bounds &bounds = screenManager()->getUIContext()->GetLayoutBounds();
	InitPadLayout(bounds.w, bounds.h);

	// Devices without a back button like iOS need an on-screen touch back button.
	bool showPauseButton = !System_GetPropertyBool(SYSPROP_HAS_BACK_BUTTON) || g_Config.bShowTouchPause;

	root_ = CreatePadLayout(bounds.w, bounds.h, &pauseTrigger_, showPauseButton, &controlMapper_);
	if (g_Config.bShowDeveloperMenu) {
		root_->Add(new Button(dev->T("DevMenu")))->OnClick.Handle(this, &EmuScreen::OnDevTools);
	}

	LinearLayout *buttons = new LinearLayout(Orientation::ORIENT_HORIZONTAL, new AnchorLayoutParams(bounds.centerX(), NONE, NONE, 60, true));
	buttons->SetSpacing(20.0f);
	root_->Add(buttons);

	resumeButton_ = buttons->Add(new Button(dev->T("Resume")));
	resumeButton_->OnClick.Handle(this, &EmuScreen::OnResume);
	resumeButton_->SetVisibility(V_GONE);

	resetButton_ = buttons->Add(new Button(dev->T("Reset")));
	resetButton_->OnClick.Handle(this, &EmuScreen::OnReset);
	resetButton_->SetVisibility(V_GONE);

	cardboardDisableButton_ = root_->Add(new Button(sc->T("Cardboard VR OFF"), new AnchorLayoutParams(bounds.centerX(), NONE, NONE, 30, true)));
	cardboardDisableButton_->OnClick.Handle(this, &EmuScreen::OnDisableCardboard);
	cardboardDisableButton_->SetVisibility(V_GONE);
	cardboardDisableButton_->SetScale(0.65f);  // make it smaller - this button can be in the way otherwise.

	if (g_Config.bEnableNetworkChat) {
		if (g_Config.iChatButtonPosition != 8) {
			AnchorLayoutParams *layoutParams = AnchorInCorner(bounds, g_Config.iChatButtonPosition, 80.0f, 50.0f);
			ChoiceWithValueDisplay *btn = new ChoiceWithValueDisplay(&newChatMessages_, n->T("Chat"), layoutParams);
			root_->Add(btn)->OnClick.Handle(this, &EmuScreen::OnChat);
			chatButton_ = btn;
		}
		chatMenu_ = root_->Add(new ChatMenu(screenManager()->getUIContext()->GetBounds(), new LayoutParams(FILL_PARENT, FILL_PARENT)));
		chatMenu_->SetVisibility(UI::V_GONE);
	} else {
		chatButton_ = nullptr;
		chatMenu_ = nullptr;
	}

	saveStatePreview_ = new AsyncImageFileView(Path(), IS_FIXED, new AnchorLayoutParams(bounds.centerX(), 100, NONE, NONE, true));
	saveStatePreview_->SetFixedSize(160, 90);
	saveStatePreview_->SetColor(0x90FFFFFF);
	saveStatePreview_->SetVisibility(V_GONE);
	saveStatePreview_->SetCanBeFocused(false);
	root_->Add(saveStatePreview_);
	onScreenMessagesView_ = root_->Add(new OnScreenMessagesView(new AnchorLayoutParams((Size)bounds.w, (Size)bounds.h)));

	GameInfoBGView *loadingBG = root_->Add(new GameInfoBGView(gamePath_, new AnchorLayoutParams(FILL_PARENT, FILL_PARENT)));
	TextView *loadingTextView = root_->Add(new TextView(sc->T(PSP_GetLoading()), new AnchorLayoutParams(bounds.centerX(), NONE, NONE, 40, true)));
	loadingTextView_ = loadingTextView;

	static const ImageID symbols[4] = {
		ImageID("I_CROSS"),
		ImageID("I_CIRCLE"),
		ImageID("I_SQUARE"),
		ImageID("I_TRIANGLE"),
	};

	Spinner *loadingSpinner = root_->Add(new Spinner(symbols, ARRAY_SIZE(symbols), new AnchorLayoutParams(NONE, NONE, 45, 45, true)));
	loadingSpinner_ = loadingSpinner;

	loadingBG->SetTag("LoadingBG");
	loadingTextView->SetTag("LoadingText");
	loadingSpinner->SetTag("LoadingSpinner");

	// Don't really need this, and it creates a lot of strings to translate...
	loadingTextView->SetVisibility(V_GONE);
	loadingTextView->SetShadow(true);

	loadingViewColor_ = loadingSpinner->AddTween(new CallbackColorTween(0x00FFFFFF, 0x00FFFFFF, 0.2f, &bezierEaseInOut));
	loadingViewColor_->SetCallback([loadingBG, loadingTextView, loadingSpinner](View *v, uint32_t c) {
		loadingBG->SetColor(c & 0xFFC0C0C0);
		loadingTextView->SetTextColor(c);
		loadingSpinner->SetColor(alphaMul(c, 0.7f));
	});
	loadingViewColor_->Persist();

	// We start invisible here, in case of recreated views.
	loadingViewVisible_ = loadingSpinner->AddTween(new VisibilityTween(UI::V_INVISIBLE, UI::V_INVISIBLE, 0.2f, &bezierEaseInOut));
	loadingViewVisible_->Persist();
	loadingViewVisible_->Finish.Add([loadingBG, loadingSpinner](EventParams &p) {
		loadingBG->SetVisibility(p.v->GetVisibility());

		// If we just became invisible, flush BGs since we don't need them anymore.
		// Saves some VRAM for the game, but don't do it before we fade out...
		if (p.v->GetVisibility() == V_INVISIBLE) {
			g_gameInfoCache->FlushBGs();
			// And we can go away too.  This means the tween will never run again.
			loadingBG->SetVisibility(V_GONE);
			loadingSpinner->SetVisibility(V_GONE);
		}
		return EVENT_DONE;
	});
	// Will become visible along with the loadingView.
	loadingBG->SetVisibility(V_INVISIBLE);
}

UI::EventReturn EmuScreen::OnDevTools(UI::EventParams &params) {
	DevMenuScreen *devMenu = new DevMenuScreen(gamePath_, I18NCat::DEVELOPER);
	if (params.v)
		devMenu->SetPopupOrigin(params.v);
	screenManager()->push(devMenu);
	return UI::EVENT_DONE;
}

UI::EventReturn EmuScreen::OnDisableCardboard(UI::EventParams &params) {
	g_Config.bEnableCardboardVR = false;
	return UI::EVENT_DONE;
}

UI::EventReturn EmuScreen::OnChat(UI::EventParams &params) {
	if (chatButton_ != nullptr && chatButton_->GetVisibility() == UI::V_VISIBLE) {
		chatButton_->SetVisibility(UI::V_GONE);
	}
	if (chatMenu_ != nullptr) {
		chatMenu_->SetVisibility(UI::V_VISIBLE);

#if PPSSPP_PLATFORM(WINDOWS) || defined(USING_QT_UI) || defined(SDL)
		UI::EnableFocusMovement(true);
		root_->SetDefaultFocusView(chatMenu_);

		chatMenu_->SetFocus();
		UI::View *focused = UI::GetFocusedView();
		if (focused) {
			root_->SubviewFocused(focused);
		}
#endif
	}
	return UI::EVENT_DONE;
}

UI::EventReturn EmuScreen::OnResume(UI::EventParams &params) {
	if (coreState == CoreState::CORE_RUNTIME_ERROR) {
		// Force it!
		Memory::MemFault_IgnoreLastCrash();
		coreState = CoreState::CORE_RUNNING;
	}
	return UI::EVENT_DONE;
}

UI::EventReturn EmuScreen::OnReset(UI::EventParams &params) {
	if (coreState == CoreState::CORE_RUNTIME_ERROR) {
		NativeMessageReceived("reset", "");
	}
	return UI::EVENT_DONE;
}

void EmuScreen::update() {
	using namespace UI;

	UIScreen::update();
	onScreenMessagesView_->SetVisibility(g_Config.bShowOnScreenMessages ? V_VISIBLE : V_GONE);
	resumeButton_->SetVisibility(coreState == CoreState::CORE_RUNTIME_ERROR && Memory::MemFault_MayBeResumable() ? V_VISIBLE : V_GONE);
	resetButton_->SetVisibility(coreState == CoreState::CORE_RUNTIME_ERROR ? V_VISIBLE : V_GONE);

	if (chatButton_ && chatMenu_) {
		if (chatMenu_->GetVisibility() != V_GONE) {
			chatMessages_ = GetChatMessageCount();
			newChatMessages_ = 0;
		} else {
			int diff = GetChatMessageCount() - chatMessages_;
			// Cap the count at 50.
			newChatMessages_ = diff > 50 ? 50 : diff;
		}
	}

	if (bootPending_) {
		bootGame(gamePath_);
	}

	// Simply forcibly update to the current screen size every frame. Doesn't cost much.
	// If bounds is set to be smaller than the actual pixel resolution of the display, respect that.
	// TODO: Should be able to use g_dpi_scale here instead. Might want to store the dpi scale in the UI context too.

#ifndef _WIN32
	const Bounds &bounds = screenManager()->getUIContext()->GetBounds();
	PSP_CoreParameter().pixelWidth = g_display.pixel_xres * bounds.w / g_display.dp_xres;
	PSP_CoreParameter().pixelHeight = g_display.pixel_yres * bounds.h / g_display.dp_yres;
#endif

	if (!invalid_) {
		UpdateUIState(coreState != CORE_RUNTIME_ERROR ? UISTATE_INGAME : UISTATE_EXCEPTION);
	}

	if (errorMessage_.size()) {
		auto err = GetI18NCategory(I18NCat::ERRORS);
		std::string errLoadingFile = gamePath_.ToVisualString() + "\n";
		errLoadingFile.append(err->T("Error loading file", "Could not load game"));
		errLoadingFile.append(" ");
		errLoadingFile.append(err->T(errorMessage_.c_str()));

		screenManager()->push(new PromptScreen(gamePath_, errLoadingFile, "OK", ""));
		errorMessage_.clear();
		quit_ = true;
		return;
	}

	if (invalid_)
		return;

	controlMapper_.Update();

	if (pauseTrigger_) {
		pauseTrigger_ = false;
		screenManager()->push(new GamePauseScreen(gamePath_));
	}

	if (saveStatePreview_ && !bootPending_) {
		int currentSlot = SaveState::GetCurrentSlot();
		if (saveStateSlot_ != currentSlot) {
			saveStateSlot_ = currentSlot;

			Path fn;
			if (SaveState::HasSaveInSlot(gamePath_, currentSlot)) {
				fn = SaveState::GenerateSaveSlotFilename(gamePath_, currentSlot, SaveState::SCREENSHOT_EXTENSION);
			}

			saveStatePreview_->SetFilename(fn);
			if (!fn.empty()) {
				saveStatePreview_->SetVisibility(UI::V_VISIBLE);
				saveStatePreviewShownTime_ = time_now_d();
			} else {
				saveStatePreview_->SetVisibility(UI::V_GONE);
			}
		}

		if (saveStatePreview_->GetVisibility() == UI::V_VISIBLE) {
			double endTime = saveStatePreviewShownTime_ + 2.0;
			float alpha = clamp_value((endTime - time_now_d()) * 4.0, 0.0, 1.0);
			saveStatePreview_->SetColor(colorAlpha(0x00FFFFFF, alpha));

			if (time_now_d() - saveStatePreviewShownTime_ > 2) {
				saveStatePreview_->SetVisibility(UI::V_GONE);
			}
		}
	}
}

void EmuScreen::checkPowerDown() {
	if (PSP_IsRebooting()) {
		bootPending_ = true;
		invalid_ = true;
	}

	if (coreState == CORE_POWERDOWN && !PSP_IsIniting() && !PSP_IsRebooting()) {
		if (PSP_IsInited()) {
			PSP_Shutdown();
		}
		INFO_LOG(SYSTEM, "SELF-POWERDOWN!");
		screenManager()->switchScreen(new MainScreen());
		bootPending_ = false;
		invalid_ = true;
	}
}

static void DrawDebugStats(UIContext *ctx, const Bounds &bounds) {
	FontID ubuntu24("UBUNTU24");

	float left = std::max(bounds.w / 2 - 20.0f, 550.0f);
	float right = bounds.w - left - 20.0f;

	char statbuf[4096];

	ctx->Flush();
	ctx->BindFontTexture();
	ctx->Draw()->SetFontScale(.7f, .7f);

	__DisplayGetDebugStats(statbuf, sizeof(statbuf));
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + 11, bounds.y + 31, left, bounds.h - 30, 0xc0000000, FLAG_DYNAMIC_ASCII | FLAG_WRAP_TEXT);
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + 10, bounds.y + 30, left, bounds.h - 30, 0xFFFFFFFF, FLAG_DYNAMIC_ASCII | FLAG_WRAP_TEXT);

	__SasGetDebugStats(statbuf, sizeof(statbuf));
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + left + 21, bounds.y + 31, right, bounds.h - 30, 0xc0000000, FLAG_DYNAMIC_ASCII | FLAG_WRAP_TEXT);
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + left + 20, bounds.y + 30, right, bounds.h - 30, 0xFFFFFFFF, FLAG_DYNAMIC_ASCII | FLAG_WRAP_TEXT);

	ctx->Draw()->SetFontScale(1.0f, 1.0f);
	ctx->Flush();
	ctx->RebindTexture();
}

static const char *CPUCoreAsString(int core) {
	switch (core) {
	case 0: return "Interpreter";
	case 1: return "JIT";
	case 2: return "IR Interpreter";
	default: return "N/A";
	}
}

static void DrawCrashDump(UIContext *ctx, const Path &gamePath) {
	const MIPSExceptionInfo &info = Core_GetExceptionInfo();

	auto sy = GetI18NCategory(I18NCat::SYSTEM);
	FontID ubuntu24("UBUNTU24");

	int x = 20 + System_GetPropertyFloat(SYSPROP_DISPLAY_SAFE_INSET_LEFT);
	int y = 20 + System_GetPropertyFloat(SYSPROP_DISPLAY_SAFE_INSET_TOP);

	ctx->Flush();
	if (ctx->Draw()->GetFontAtlas()->getFont(ubuntu24))
		ctx->BindFontTexture();
	ctx->Draw()->SetFontScale(1.1f, 1.1f);
	ctx->Draw()->DrawTextShadow(ubuntu24, sy->T("Game crashed"), x, y, 0xFFFFFFFF);

	char statbuf[4096];
	char versionString[256];
	snprintf(versionString, sizeof(versionString), "%s", PPSSPP_GIT_VERSION);

	char crcStr[16]{};
	if (Reporting::HasCRC(gamePath)) {
		u32 crc = Reporting::RetrieveCRC(gamePath);
		snprintf(crcStr, sizeof(crcStr), "CRC: %08x\n", crc);
	} else {
		// Queue it for calculation, we want it!
		// It's OK to call this repeatedly until we have it, which is natural here.
		Reporting::QueueCRC(gamePath);
	}

	// TODO: Draw a lot more information. Full register set, and so on.

#ifdef _DEBUG
	char build[] = "debug";
#else
	char build[] = "release";
#endif

	std::string sysName = System_GetProperty(SYSPROP_NAME);
	int sysVersion = System_GetPropertyInt(SYSPROP_SYSTEMVERSION);

	// First column
	y += 65;

	int columnWidth = (ctx->GetBounds().w - x - 10) / 2;
	int height = ctx->GetBounds().h;

	ctx->PushScissor(Bounds(x, y, columnWidth, height));

	// INFO_LOG(SYSTEM, "DrawCrashDump (%d %d %d %d)", x, y, columnWidth, height);

	snprintf(statbuf, sizeof(statbuf), R"(%s
%s (%s)
%s (%s)
%s v%d (%s)
%s
)",
		ExceptionTypeAsString(info.type),
		g_paramSFO.GetDiscID().c_str(), g_paramSFO.GetValueString("TITLE").c_str(),
		versionString, build,
		sysName.c_str(), sysVersion, GetCompilerABI(),
		crcStr
	);

	ctx->Draw()->SetFontScale(.7f, .7f);
	ctx->Draw()->DrawTextShadow(ubuntu24, statbuf, x, y, 0xFFFFFFFF);
	y += 160;

	if (info.type == MIPSExceptionType::MEMORY) {
		snprintf(statbuf, sizeof(statbuf), R"(
Access: %s at %08x (sz: %d)
PC: %08x
%s)",
			MemoryExceptionTypeAsString(info.memory_type),
			info.address,
			info.accessSize,
			info.pc,
			info.info.c_str());
		ctx->Draw()->DrawTextShadow(ubuntu24, statbuf, x, y, 0xFFFFFFFF);
		y += 180;
	} else if (info.type == MIPSExceptionType::BAD_EXEC_ADDR) {
		snprintf(statbuf, sizeof(statbuf), R"(
Destination: %s to %08x
PC: %08x
RA: %08x)",
			ExecExceptionTypeAsString(info.exec_type),
			info.address,
			info.pc,
			info.ra);
		ctx->Draw()->DrawTextShadow(ubuntu24, statbuf, x, y, 0xFFFFFFFF);
		y += 180;
	} else if (info.type == MIPSExceptionType::BREAK) {
		snprintf(statbuf, sizeof(statbuf), R"(
BREAK
PC: %08x
)", info.pc);
		ctx->Draw()->DrawTextShadow(ubuntu24, statbuf, x, y, 0xFFFFFFFF);
		y += 180;
	} else {
		snprintf(statbuf, sizeof(statbuf), R"(
Invalid / Unknown (%d)
)", (int)info.type);
		ctx->Draw()->DrawTextShadow(ubuntu24, statbuf, x, y, 0xFFFFFFFF);
		y += 180;
	}

	std::string kernelState = __KernelStateSummary();

	ctx->Draw()->DrawTextShadow(ubuntu24, kernelState.c_str(), x, y, 0xFFFFFFFF);

	y += 40;

	ctx->Draw()->SetFontScale(.5f, .5f);

	ctx->Draw()->DrawTextShadow(ubuntu24, info.stackTrace.c_str(), x, y, 0xFFFFFFFF);

	ctx->Draw()->SetFontScale(.7f, .7f);

	ctx->PopScissor();

	// Draw some additional stuff to the right.

	x += columnWidth + 10;
	y = 85;
	snprintf(statbuf, sizeof(statbuf),
		"CPU Core: %s (flags: %08x)\n"
		"Locked CPU freq: %d MHz\n"
		"Cheats: %s, Plugins: %s\n",
		CPUCoreAsString(g_Config.iCpuCore), g_Config.uJitDisableFlags,
		g_Config.iLockedCPUSpeed,
		CheatsInEffect() ? "Y" : "N", HLEPlugins::HasEnabled() ? "Y" : "N");

	ctx->Draw()->DrawTextShadow(ubuntu24, statbuf, x, y, 0xFFFFFFFF);
	ctx->Flush();
	ctx->Draw()->SetFontScale(1.0f, 1.0f);
	ctx->RebindTexture();
}

static void DrawAudioDebugStats(UIContext *ctx, const Bounds &bounds) {
	FontID ubuntu24("UBUNTU24");

	char statbuf[4096] = { 0 };
	System_AudioGetDebugStats(statbuf, sizeof(statbuf));

	ctx->Flush();
	ctx->BindFontTexture();
	ctx->Draw()->SetFontScale(0.7f, 0.7f);
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + 11, bounds.y + 31, bounds.w - 20, bounds.h - 30, 0xc0000000, FLAG_DYNAMIC_ASCII | FLAG_WRAP_TEXT);
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + 10, bounds.y + 30, bounds.w - 20, bounds.h - 30, 0xFFFFFFFF, FLAG_DYNAMIC_ASCII | FLAG_WRAP_TEXT);
	ctx->Draw()->SetFontScale(1.0f, 1.0f);
	ctx->Flush();
	ctx->RebindTexture();
}

static void DrawControlDebug(UIContext *ctx, const ControlMapper &mapper, const Bounds &bounds) {
	FontID ubuntu24("UBUNTU24");

	char statbuf[4096] = { 0 };
	mapper.GetDebugString(statbuf, sizeof(statbuf));

	ctx->Flush();
	ctx->BindFontTexture();
	ctx->Draw()->SetFontScale(0.5f, 0.5f);
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + 11, bounds.y + 31, bounds.w - 20, bounds.h - 30, 0xc0000000, FLAG_DYNAMIC_ASCII);
	ctx->Draw()->DrawTextRect(ubuntu24, statbuf, bounds.x + 10, bounds.y + 30, bounds.w - 20, bounds.h - 30, 0xFFFFFFFF, FLAG_DYNAMIC_ASCII);
	ctx->Draw()->SetFontScale(1.0f, 1.0f);
	ctx->Flush();
	ctx->RebindTexture();
}

static void DrawFPS(UIContext *ctx, const Bounds &bounds) {
	FontID ubuntu24("UBUNTU24");
	float vps, fps, actual_fps;
	__DisplayGetFPS(&vps, &fps, &actual_fps);

	char fpsbuf[256]{};
	if (g_Config.iShowStatusFlags == ((int)ShowStatusFlags::FPS_COUNTER | (int)ShowStatusFlags::SPEED_COUNTER)) {
		snprintf(fpsbuf, sizeof(fpsbuf), "%0.0f/%0.0f (%0.1f%%)", actual_fps, fps, vps / (59.94f / 100.0f));
	} else {
		if (g_Config.iShowStatusFlags & (int)ShowStatusFlags::FPS_COUNTER) {
			snprintf(fpsbuf, sizeof(fpsbuf), "FPS: %0.1f", actual_fps);
		}
		if (g_Config.iShowStatusFlags & (int)ShowStatusFlags::SPEED_COUNTER) {
			snprintf(fpsbuf, sizeof(fpsbuf), "%s Speed: %0.1f%%", fpsbuf, vps / (59.94f / 100.0f));
		}
	}

#ifdef CAN_DISPLAY_CURRENT_BATTERY_CAPACITY
	if (g_Config.iShowStatusFlags & (int)ShowStatusFlags::BATTERY_PERCENT) {
		snprintf(fpsbuf, sizeof(fpsbuf), "%s Battery: %d%%", fpsbuf, getCurrentBatteryCapacity());
	}
#endif

	ctx->Flush();
	ctx->BindFontTexture();
	ctx->Draw()->SetFontScale(0.7f, 0.7f);
	ctx->Draw()->DrawText(ubuntu24, fpsbuf, bounds.x2() - 8, 20, 0xc0000000, ALIGN_TOPRIGHT | FLAG_DYNAMIC_ASCII);
	ctx->Draw()->DrawText(ubuntu24, fpsbuf, bounds.x2() - 10, 19, 0xFF3fFF3f, ALIGN_TOPRIGHT | FLAG_DYNAMIC_ASCII);
	ctx->Draw()->SetFontScale(1.0f, 1.0f);
	ctx->Flush();
	ctx->RebindTexture();
}

static void DrawFrameTimes(UIContext *ctx, const Bounds &bounds) {
	FontID ubuntu24("UBUNTU24");
	int valid, pos;
	double *sleepHistory;
	double *history = __DisplayGetFrameTimes(&valid, &pos, &sleepHistory);
	int scale = 7000;
	int width = 600;

	ctx->Flush();
	ctx->BeginNoTex();
	int bottom = bounds.y2();
	for (int i = 0; i < valid; ++i) {
		double activeTime = history[i] - sleepHistory[i];
		ctx->Draw()->vLine(bounds.x + i, bottom, bottom - activeTime * scale, 0xFF3FFF3F);
		ctx->Draw()->vLine(bounds.x + i, bottom - activeTime * scale, bottom - history[i] * scale, 0x7F3FFF3F);
	}
	ctx->Draw()->vLine(bounds.x + pos, bottom, bottom - 512, 0xFFff3F3f);

	ctx->Draw()->hLine(bounds.x, bottom - 0.0333 * scale, bounds.x + width, 0xFF3f3Fff);
	ctx->Draw()->hLine(bounds.x, bottom - 0.0167 * scale, bounds.x + width, 0xFF3f3Fff);

	ctx->Flush();
	ctx->Begin();
	ctx->BindFontTexture();
	ctx->Draw()->SetFontScale(0.5f, 0.5f);
	ctx->Draw()->DrawText(ubuntu24, "33.3ms", bounds.x + width, bottom - 0.0333 * scale, 0xFF3f3Fff, ALIGN_BOTTOMLEFT | FLAG_DYNAMIC_ASCII);
	ctx->Draw()->DrawText(ubuntu24, "16.7ms", bounds.x + width, bottom - 0.0167 * scale, 0xFF3f3Fff, ALIGN_BOTTOMLEFT | FLAG_DYNAMIC_ASCII);
	ctx->Draw()->SetFontScale(1.0f, 1.0f);
	ctx->Flush();
	ctx->RebindTexture();
}

void EmuScreen::preRender() {
	using namespace Draw;
	DrawContext *draw = screenManager()->getDrawContext();
	draw->BeginFrame();
	// Here we do NOT bind the backbuffer or clear the screen, unless non-buffered.
	// The emuscreen is different than the others - we really want to allow the game to render to framebuffers
	// before we ever bind the backbuffer for rendering. On mobile GPUs, switching back and forth between render
	// targets is a mortal sin so it's very important that we don't bind the backbuffer unnecessarily here.
	// We only bind it in FramebufferManager::CopyDisplayToOutput (unless non-buffered)...
	// We do, however, start the frame in other ways.

	if ((g_Config.bSkipBufferEffects && !g_Config.bSoftwareRendering) || Core_IsStepping()) {
		// We need to clear here already so that drawing during the frame is done on a clean slate.
		if (Core_IsStepping() && gpuStats.numFlips != 0) {
			draw->BindFramebufferAsRenderTarget(nullptr, { RPAction::KEEP, RPAction::DONT_CARE, RPAction::DONT_CARE }, "EmuScreen_BackBuffer");
		} else {
			draw->BindFramebufferAsRenderTarget(nullptr, { RPAction::CLEAR, RPAction::CLEAR, RPAction::CLEAR, 0xFF000000 }, "EmuScreen_BackBuffer");
		}

		Viewport viewport;
		viewport.TopLeftX = 0;
		viewport.TopLeftY = 0;
		viewport.Width = g_display.pixel_xres;
		viewport.Height = g_display.pixel_yres;
		viewport.MaxDepth = 1.0;
		viewport.MinDepth = 0.0;
		draw->SetViewport(viewport);
	}
	draw->SetTargetSize(g_display.pixel_xres, g_display.pixel_yres);
}

void EmuScreen::postRender() {
	Draw::DrawContext *draw = screenManager()->getDrawContext();
	if (!draw)
		return;
	if (stopRender_)
		draw->WipeQueue();
	draw->EndFrame();
}

void EmuScreen::render() {
	using namespace Draw;

	DrawContext *thin3d = screenManager()->getDrawContext();
	if (!thin3d)
		return;  // shouldn't really happen but I've seen a suspicious stack trace..

	if (invalid_) {
		// Loading, or after shutdown?
		if (loadingTextView_->GetVisibility() == UI::V_VISIBLE)
			loadingTextView_->SetText(PSP_GetLoading());

		// It's possible this might be set outside PSP_RunLoopFor().
		// In this case, we need to double check it here.
		checkPowerDown();
		thin3d->BindFramebufferAsRenderTarget(nullptr, { RPAction::CLEAR, RPAction::CLEAR, RPAction::CLEAR }, "EmuScreen_Invalid");
		renderUI();
		return;
	}

	// Freeze-frame functionality (loads a savestate on every frame).
	if (PSP_CoreParameter().freezeNext) {
		PSP_CoreParameter().frozen = true;
		PSP_CoreParameter().freezeNext = false;
		SaveState::SaveToRam(freezeState_);
	} else if (PSP_CoreParameter().frozen) {
		std::string errorString;
		if (CChunkFileReader::ERROR_NONE != SaveState::LoadFromRam(freezeState_, &errorString)) {
			ERROR_LOG(SAVESTATE, "Failed to load freeze state (%s). Unfreezing.", errorString.c_str());
			PSP_CoreParameter().frozen = false;
		}
	}

	Core_UpdateDebugStats(g_Config.bShowDebugStats || g_Config.bLogFrameDrops);

	PSP_BeginHostFrame();

	PSP_RunLoopWhileState();

	// Hopefully coreState is now CORE_NEXTFRAME
	switch (coreState) {
	case CORE_NEXTFRAME:
		// Reached the end of the frame, all good. Set back to running for the next frame
		coreState = CORE_RUNNING;
		break;
	case CORE_STEPPING:
	case CORE_RUNTIME_ERROR:
	{
		// If there's an exception, display information.
		const MIPSExceptionInfo &info = Core_GetExceptionInfo();
		if (info.type != MIPSExceptionType::NONE) {
			// Clear to blue background screen
			bool dangerousSettings = !Reporting::IsSupported();
			uint32_t color = dangerousSettings ? 0xFF900050 : 0xFF900000;
			thin3d->BindFramebufferAsRenderTarget(nullptr, { RPAction::CLEAR, RPAction::DONT_CARE, RPAction::DONT_CARE, color }, "EmuScreen_RuntimeError");
			// The info is drawn later in renderUI
		} else {
			// If we're stepping, it's convenient not to clear the screen entirely, so we copy display to output.
			// This won't work in non-buffered, but that's fine.
			thin3d->BindFramebufferAsRenderTarget(nullptr, { RPAction::CLEAR, RPAction::DONT_CARE, RPAction::DONT_CARE }, "EmuScreen_Stepping");
			// Just to make sure.
			if (PSP_IsInited()) {
				gpu->CopyDisplayToOutput(true);
			}
		}
		break;
	}
	default:
		// Didn't actually reach the end of the frame, ran out of the blockTicks cycles.
		// In this case we need to bind and wipe the backbuffer, at least.
		// It's possible we never ended up outputted anything - make sure we have the backbuffer cleared
		thin3d->BindFramebufferAsRenderTarget(nullptr, { RPAction::CLEAR, RPAction::CLEAR, RPAction::CLEAR }, "EmuScreen_NoFrame");
		break;
	}

	PSP_EndHostFrame();

	// This must happen after PSP_EndHostFrame so that things like push buffers are end-frame'd before we start destroying stuff.
	checkPowerDown();

	if (invalid_)
		return;

	if (hasVisibleUI()) {
		// In most cases, this should already be bound and a no-op.
		thin3d->BindFramebufferAsRenderTarget(nullptr, { RPAction::KEEP, RPAction::DONT_CARE, RPAction::DONT_CARE }, "EmuScreen_UI");
		cardboardDisableButton_->SetVisibility(g_Config.bEnableCardboardVR ? UI::V_VISIBLE : UI::V_GONE);
		screenManager()->getUIContext()->BeginFrame();
		renderUI();
	}

	if (chatMenu_ && (chatMenu_->GetVisibility() == UI::V_VISIBLE)) {
		SetVRAppMode(VRAppMode::VR_DIALOG_MODE);
	} else {
		SetVRAppMode(screenManager()->topScreen() == this ? VRAppMode::VR_GAME_MODE : VRAppMode::VR_DIALOG_MODE);
	}
}

bool EmuScreen::hasVisibleUI() {
	// Regular but uncommon UI.
	if (saveStatePreview_->GetVisibility() != UI::V_GONE || loadingSpinner_->GetVisibility() == UI::V_VISIBLE)
		return true;
	if (!osm.IsEmpty() || g_Config.bShowTouchControls || g_Config.iShowStatusFlags != 0)
		return true;
	if (g_Config.bEnableCardboardVR || g_Config.bEnableNetworkChat)
		return true;
	// Debug UI.
	if (g_Config.bShowDebugStats || g_Config.bShowDeveloperMenu || g_Config.bShowAudioDebug || g_Config.bShowFrameProfiler || g_Config.bShowControlDebug)
		return true;

	// Exception information.
	if (coreState == CORE_RUNTIME_ERROR || coreState == CORE_STEPPING) {
		return true;
	}

	return false;
}

void EmuScreen::renderUI() {
	using namespace Draw;

	DrawContext *thin3d = screenManager()->getDrawContext();
	UIContext *ctx = screenManager()->getUIContext();
	ctx->BeginFrame();
	// This sets up some important states but not the viewport.
	ctx->Begin();

	Viewport viewport;
	viewport.TopLeftX = 0;
	viewport.TopLeftY = 0;
	viewport.Width = g_display.pixel_xres;
	viewport.Height = g_display.pixel_yres;
	viewport.MaxDepth = 1.0;
	viewport.MinDepth = 0.0;
	thin3d->SetViewport(viewport);

	if (root_) {
		UI::LayoutViewHierarchy(*ctx, root_, false);
		root_->Draw(*ctx);
	}

	if (!invalid_) {
		if (g_Config.bShowDebugStats) {
			DrawDebugStats(ctx, ctx->GetLayoutBounds());
		}

		if (g_Config.bShowAudioDebug) {
			DrawAudioDebugStats(ctx, ctx->GetLayoutBounds());
		}

		if (g_Config.iShowStatusFlags) {
			DrawFPS(ctx, ctx->GetLayoutBounds());
		}

		if (g_Config.bDrawFrameGraph) {
			DrawFrameTimes(ctx, ctx->GetLayoutBounds());
		}

		if (g_Config.bShowControlDebug) {
			DrawControlDebug(ctx, controlMapper_, ctx->GetLayoutBounds());
		}
	}

#if !PPSSPP_PLATFORM(UWP) && !PPSSPP_PLATFORM(SWITCH)
	if ((g_Config.iGPUBackend == (int)GPUBackend::VULKAN || g_Config.iGPUBackend == (int)GPUBackend::OPENGL) && g_Config.bShowAllocatorDebug) {
		DrawGPUMemoryVis(ctx, gpu);
	}

	if ((g_Config.iGPUBackend == (int)GPUBackend::VULKAN || g_Config.iGPUBackend == (int)GPUBackend::OPENGL) && g_Config.bShowGpuProfile) {
		DrawGPUProfilerVis(ctx, gpu);
	}

#endif

#ifdef USE_PROFILER
	if (g_Config.bShowFrameProfiler && !invalid_) {
		DrawProfile(*ctx);
	}
#endif

	if (coreState == CORE_RUNTIME_ERROR || coreState == CORE_STEPPING) {
		const MIPSExceptionInfo &info = Core_GetExceptionInfo();
		if (info.type != MIPSExceptionType::NONE) {
			DrawCrashDump(ctx, gamePath_);
		} else {
			// We're somehow in ERROR or STEPPING without a crash dump. This case is what lead
			// to the bare "Resume" and "Reset" buttons without a crash dump before, in cases
			// where we were unable to ignore memory errors.
		}
	}

	ctx->Flush();
}

void EmuScreen::autoLoad() {
	int autoSlot = -1;

	//check if save state has save, if so, load
	switch (g_Config.iAutoLoadSaveState) {
	case (int)AutoLoadSaveState::OFF: // "AutoLoad Off"
		return;
	case (int)AutoLoadSaveState::OLDEST: // "Oldest Save"
		autoSlot = SaveState::GetOldestSlot(gamePath_);
		break;
	case (int)AutoLoadSaveState::NEWEST: // "Newest Save"
		autoSlot = SaveState::GetNewestSlot(gamePath_);
		break;
	default: // try the specific save state slot specified
		autoSlot = (SaveState::HasSaveInSlot(gamePath_, g_Config.iAutoLoadSaveState - 3)) ? (g_Config.iAutoLoadSaveState - 3) : -1;
		break;
	}

	if (g_Config.iAutoLoadSaveState && autoSlot != -1) {
		SaveState::LoadSlot(gamePath_, autoSlot, &AfterSaveStateAction);
		g_Config.iCurrentStateSlot = autoSlot;
	}
}

void EmuScreen::resized() {
	RecreateViews();
}
