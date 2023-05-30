// Copyright (c) 2012- PPSSPP Project.

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 2.0 or later versions.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
// GNU General Public License 2.0 for more details.

// A copy of the GPL 2.0 should have been included with the program.
// If not, see http://www.gnu.org/licenses/

// Official git repository and contact information can be found at
// https://github.com/hrydgard/ppsspp and http://www.ppsspp.org/.

// NativeApp implementation for platforms that will use that framework, like:
// Android, Linux, MacOSX.
//
// Native is a cross platform framework. It's not very mature and mostly
// just built according to the needs of my own apps.
//
// Windows has its own code that bypasses the framework entirely.

#include "ppsspp_config.h"

// Background worker threads should be spawned in NativeInit and joined
// in NativeShutdown.

#include <locale.h>
#include <algorithm>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <thread>

#if defined(_WIN32)
#include "Windows/WindowsAudio.h"
#include "Windows/MainWindow.h"
#endif

#if defined(_WIN32) && !PPSSPP_PLATFORM(UWP)
#include "Windows/CaptureDevice.h"
#endif

#include "Common/Net/HTTPClient.h"
#include "Common/Net/Resolve.h"
#include "Common/Net/URL.h"
#include "Common/Render/TextureAtlas.h"
#include "Common/Render/Text/draw_text.h"
#include "Common/GPU/OpenGL/GLFeatures.h"
#include "Common/GPU/thin3d.h"
#include "Common/UI/UI.h"
#include "Common/UI/Screen.h"
#include "Common/UI/Context.h"
#include "Common/UI/View.h"
#include "android/jni/app-android.h"

#include "Common/System/Display.h"
#include "Common/System/Request.h"
#include "Common/System/System.h"
#include "Common/System/NativeApp.h"

#include "Common/Data/Text/I18n.h"
#include "Common/Input/InputState.h"
#include "Common/Math/math_util.h"
#include "Common/Math/lin/matrix4x4.h"
#include "Common/Profiler/Profiler.h"
#include "Common/Data/Encoding/Utf8.h"
#include "Common/File/VFS/VFS.h"
#include "Common/File/VFS/ZipFileReader.h"
#include "Common/File/VFS/DirectoryReader.h"
#include "Common/CPUDetect.h"
#include "Common/File/FileUtil.h"
#include "Common/TimeUtil.h"
#include "Common/StringUtils.h"
#include "Common/LogManager.h"
#include "Common/MemArena.h"
#include "Common/GraphicsContext.h"
#include "Common/OSVersion.h"
#include "Common/GPU/ShaderTranslation.h"
#include "Common/VR/PPSSPPVR.h"

#include "Core/ControlMapper.h"
#include "Core/Config.h"
#include "Core/ConfigValues.h"
#include "Core/Core.h"
#include "Core/FileLoaders/DiskCachingFileLoader.h"
#include "Core/KeyMap.h"
#include "Core/Reporting.h"
#include "Core/SaveState.h"
#include "Core/Screenshot.h"
#include "Core/System.h"
#include "Core/HLE/__sceAudio.h"
#include "Core/HLE/sceCtrl.h"
#include "Core/HLE/sceUsbCam.h"
#include "Core/HLE/sceUsbGps.h"
#include "Core/HLE/proAdhoc.h"
#include "Core/HW/MemoryStick.h"
#include "Core/Util/GameManager.h"
#include "Core/Util/AudioFormat.h"
#include "Core/WebServer.h"
#include "Core/TiltEventProcessor.h"
#include "Core/ThreadPools.h"

#include "GPU/GPUInterface.h"
#include "UI/AudioCommon.h"
#include "UI/BackgroundAudio.h"
#include "UI/ControlMappingScreen.h"
#include "UI/DiscordIntegration.h"
#include "UI/EmuScreen.h"
#include "UI/GameInfoCache.h"
#include "UI/GPUDriverTestScreen.h"
#include "UI/MiscScreens.h"
#include "UI/MemStickScreen.h"
#include "UI/OnScreenDisplay.h"
#include "UI/RemoteISOScreen.h"
#include "UI/Theme.h"

#if defined(USING_QT_UI)
#include <QFontDatabase>
#endif
#if PPSSPP_PLATFORM(UWP)
#include <dwrite_3.h>
#endif
#if PPSSPP_PLATFORM(ANDROID)
#include "android/jni/app-android.h"
#endif

#if PPSSPP_ARCH(ARM) && defined(__ANDROID__)
#include "../../android/jni/ArmEmitterTest.h"
#elif PPSSPP_ARCH(ARM64) && defined(__ANDROID__)
#include "../../android/jni/Arm64EmitterTest.h"
#endif

#if PPSSPP_PLATFORM(IOS)
#include "ios/iOSCoreAudio.h"
#elif defined(__APPLE__)
#include <mach-o/dyld.h>
#endif

#if PPSSPP_PLATFORM(IOS) || PPSSPP_PLATFORM(MAC)
#include "UI/DarwinFileSystemServices.h"
#endif

#include <Core/HLE/Plugins.h>

ScreenManager *g_screenManager;
std::string config_filename;

// Really need to clean this mess of globals up... but instead I add more :P
bool g_TakeScreenshot;
static bool isOuya;
static bool resized = false;
static bool restarting = false;

static int renderCounter = 0;

struct PendingMessage {
	std::string msg;
	std::string value;
};

static std::mutex pendingMutex;
static std::vector<PendingMessage> pendingMessages;
static Draw::DrawContext *g_draw;
static Draw::Pipeline *colorPipeline;
static Draw::Pipeline *texColorPipeline;
static UIContext *uiContext;

#ifdef _WIN32
WindowsAudioBackend *winAudioBackend;
#endif

std::thread *graphicsLoadThread;

class PrintfLogger : public LogListener {
public:
	void Log(const LogMessage &message) override {
		// Log with simplified headers as Android already provides timestamp etc.
		switch (message.level) {
		case LogTypes::LVERBOSE:
		case LogTypes::LDEBUG:
		case LogTypes::LINFO:
			printf("INFO [%s] %s", message.log, message.msg.c_str());
			break;
		case LogTypes::LERROR:
			printf("ERR  [%s] %s", message.log, message.msg.c_str());
			break;
		case LogTypes::LWARNING:
			printf("WARN [%s] %s", message.log, message.msg.c_str());
			break;
		case LogTypes::LNOTICE:
		default:
			printf("NOTE [%s] !!! %s", message.log, message.msg.c_str());
			break;
		}
	}
};

// globals
static LogListener *logger = nullptr;
Path boot_filename;

std::string NativeQueryConfig(std::string query) {
	char temp[128];
	if (query == "screenRotation") {
		INFO_LOG(G3D, "g_Config.screenRotation = %d", g_Config.iScreenRotation);
		snprintf(temp, sizeof(temp), "%d", g_Config.iScreenRotation);
		return std::string(temp);
	} else if (query == "immersiveMode") {
		return std::string(g_Config.bImmersiveMode ? "1" : "0");
	} else if (query == "hwScale") {
		int scale = g_Config.iAndroidHwScale;
		// Override hw scale for TV type devices.
		if (System_GetPropertyInt(SYSPROP_DEVICE_TYPE) == DEVICE_TYPE_TV)
			scale = 0;

		if (scale == 1) {
			// If g_Config.iInternalResolution is also set to Auto (1), we fall back to "Device resolution" (0). It works out.
			scale = g_Config.iInternalResolution;
		} else if (scale >= 2) {
			scale -= 1;
		}

		int max_res = std::max(System_GetPropertyInt(SYSPROP_DISPLAY_XRES), System_GetPropertyInt(SYSPROP_DISPLAY_YRES)) / 480 + 1;
		snprintf(temp, sizeof(temp), "%d", std::min(scale, max_res));
		return std::string(temp);
	} else if (query == "sustainedPerformanceMode") {
		return std::string(g_Config.bSustainedPerformanceMode ? "1" : "0");
	} else if (query == "androidJavaGL") {
		// If we're using Vulkan, we say no... need C++ to use Vulkan.
		if (GetGPUBackend() == GPUBackend::VULKAN) {
			return "false";
		}
		// Otherwise, some devices prefer the Java init so play it safe.
		return "true";
	} else {
		return "";
	}
}

int NativeMix(short *audio, int numSamples, int sampleRateHz) {
	return __AudioMix(audio, numSamples, sampleRateHz);
}

// This is called before NativeInit so we do a little bit of initialization here.
void NativeGetAppInfo(std::string *app_dir_name, std::string *app_nice_name, bool *landscape, std::string *version) {
	*app_nice_name = "PPSSPP";
	*app_dir_name = "ppsspp";
	*landscape = true;
	*version = PPSSPP_GIT_VERSION;

#if PPSSPP_ARCH(ARM) && defined(__ANDROID__)
	ArmEmitterTest();
#elif PPSSPP_ARCH(ARM64) && defined(__ANDROID__)
	Arm64EmitterTest();
#endif
}

#if defined(USING_WIN_UI) && !PPSSPP_PLATFORM(UWP)
static bool CheckFontIsUsable(const wchar_t *fontFace) {
	wchar_t actualFontFace[1024] = { 0 };

	HFONT f = CreateFont(0, 0, 0, 0, FW_LIGHT, 0, FALSE, FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, PROOF_QUALITY, VARIABLE_PITCH, fontFace);
	if (f != nullptr) {
		HDC hdc = CreateCompatibleDC(nullptr);
		if (hdc != nullptr) {
			SelectObject(hdc, f);
			GetTextFace(hdc, 1024, actualFontFace);
			DeleteDC(hdc);
		}
		DeleteObject(f);
	}

	// If we were able to get the font name, did it load?
	if (actualFontFace[0] != 0) {
		return wcsncmp(actualFontFace, fontFace, ARRAY_SIZE(actualFontFace)) == 0;
	}
	return false;
}
#endif

bool CreateDirectoriesAndroid();

void PostLoadConfig() {
	// On Windows, we deal with currentDirectory in InitSysDirectories().
#if !PPSSPP_PLATFORM(WINDOWS)
	if (g_Config.currentDirectory.empty()) {
		g_Config.currentDirectory = g_Config.defaultCurrentDirectory;
	}
#endif

	// Allow the lang directory to be overridden for testing purposes (e.g. Android, where it's hard to
	// test new languages without recompiling the entire app, which is a hassle).
	const Path langOverridePath = GetSysDirectory(DIRECTORY_SYSTEM) / "lang";

	// If we run into the unlikely case that "lang" is actually a file, just use the built-in translations.
	if (!File::Exists(langOverridePath) || !File::IsDirectory(langOverridePath))
		g_i18nrepo.LoadIni(g_Config.sLanguageIni);
	else
		g_i18nrepo.LoadIni(g_Config.sLanguageIni, langOverridePath);

#if PPSSPP_PLATFORM(ANDROID)
	CreateDirectoriesAndroid();
#endif
}

bool CreateDirectoriesAndroid() {
	// TODO: We should probably simply use this as the shared function to create memstick directories.

	Path pspDir = g_Config.memStickDirectory;
	if (pspDir.GetFilename() != "PSP") {
		pspDir /= "PSP";
	}

	INFO_LOG(IO, "Creating '%s' and subdirs:", pspDir.c_str());
	File::CreateFullPath(pspDir);
	if (!File::Exists(pspDir)) {
		INFO_LOG(IO, "Not a workable memstick directory. Giving up");
		return false;
	}

	File::CreateFullPath(GetSysDirectory(DIRECTORY_CHEATS));
	File::CreateFullPath(GetSysDirectory(DIRECTORY_SAVEDATA));
	File::CreateFullPath(GetSysDirectory(DIRECTORY_SAVESTATE));
	File::CreateFullPath(GetSysDirectory(DIRECTORY_GAME));
	File::CreateFullPath(GetSysDirectory(DIRECTORY_SYSTEM));
	File::CreateFullPath(GetSysDirectory(DIRECTORY_TEXTURES));
	File::CreateFullPath(GetSysDirectory(DIRECTORY_PLUGINS));

	// Avoid media scanners in PPSSPP_STATE and SAVEDATA directories,
	// and in the root PSP directory as well.
	File::CreateEmptyFile(GetSysDirectory(DIRECTORY_SAVESTATE) / ".nomedia");
	File::CreateEmptyFile(GetSysDirectory(DIRECTORY_SAVEDATA) / ".nomedia");
	File::CreateEmptyFile(GetSysDirectory(DIRECTORY_SYSTEM) / ".nomedia");
	File::CreateEmptyFile(GetSysDirectory(DIRECTORY_TEXTURES) / ".nomedia");
	File::CreateEmptyFile(GetSysDirectory(DIRECTORY_PLUGINS) / ".nomedia");
	return true;
}

static void CheckFailedGPUBackends() {
#ifdef _DEBUG
	// If you're in debug mode, you probably don't want a fallback. If you're in release mode, use IGNORE below.
	NOTICE_LOG(LOADER, "Not checking for failed graphics backends in debug mode");
	return;
#endif

#if PPSSPP_PLATFORM(ANDROID)
	if (System_GetPropertyInt(SYSPROP_SYSTEMVERSION) >= 30) {
		// In Android 11 or later, Vulkan is as stable as OpenGL, so let's not even bother.
		// Have also seen unexplained issues with random fallbacks to OpenGL for no good reason,
		// especially when debugging.
		return;
	}
#endif

	// We only want to do this once per process run and backend, to detect process crashes.
	// If NativeShutdown is called before we finish, we might call this multiple times.
	static int lastBackend = -1;
	if (lastBackend == g_Config.iGPUBackend) {
		return;
	}
	lastBackend = g_Config.iGPUBackend;

	Path cache = GetSysDirectory(DIRECTORY_APP_CACHE) / "FailedGraphicsBackends.txt";

	if (System_GetPropertyBool(SYSPROP_SUPPORTS_PERMISSIONS)) {
		std::string data;
		if (File::ReadFileToString(true, cache, data))
			g_Config.sFailedGPUBackends = data;
	}

	// Use this if you want to debug a graphics crash...
	if (g_Config.sFailedGPUBackends == "IGNORE")
		return;
	else if (!g_Config.sFailedGPUBackends.empty())
		ERROR_LOG(LOADER, "Failed graphics backends: %s", g_Config.sFailedGPUBackends.c_str());

	// Okay, let's not try a backend in the failed list.
	g_Config.iGPUBackend = g_Config.NextValidBackend();
	if (lastBackend != g_Config.iGPUBackend) {
		std::string param = GPUBackendToString((GPUBackend)lastBackend) + " -> " + GPUBackendToString((GPUBackend)g_Config.iGPUBackend);
		System_GraphicsBackendFailedAlert(param);
		WARN_LOG(LOADER, "Failed graphics backend switched from %s (%d to %d)", param.c_str(), lastBackend, g_Config.iGPUBackend);
	}
	// And then let's - for now - add the current to the failed list.
	if (g_Config.sFailedGPUBackends.empty()) {
		g_Config.sFailedGPUBackends = GPUBackendToString((GPUBackend)g_Config.iGPUBackend);
	} else if (g_Config.sFailedGPUBackends.find("ALL") == std::string::npos) {
		g_Config.sFailedGPUBackends += "," + GPUBackendToString((GPUBackend)g_Config.iGPUBackend);
	}

	if (System_GetPropertyBool(SYSPROP_SUPPORTS_PERMISSIONS)) {
		// Let's try to create, in case it doesn't exist.
		if (!File::Exists(GetSysDirectory(DIRECTORY_APP_CACHE)))
			File::CreateDir(GetSysDirectory(DIRECTORY_APP_CACHE));
		File::WriteStringToFile(true, g_Config.sFailedGPUBackends, cache);
	} else {
		// Just save immediately, since we have storage.
		g_Config.Save("got storage permission");
	}
}

static void ClearFailedGPUBackends() {
	if (g_Config.sFailedGPUBackends == "IGNORE")
		return;

	// We've successfully started graphics without crashing, hurray.
	// In case they update drivers and have totally different problems much later, clear the failed list.
	g_Config.sFailedGPUBackends.clear();
	if (System_GetPropertyBool(SYSPROP_SUPPORTS_PERMISSIONS) || System_GetPropertyBool(SYSPROP_ANDROID_SCOPED_STORAGE)) {
		File::Delete(GetSysDirectory(DIRECTORY_APP_CACHE) / "FailedGraphicsBackends.txt");
	} else {
		g_Config.Save("clearFailedGPUBackends");
	}
}

void NativeInit(int argc, const char *argv[], const char *savegame_dir, const char *external_dir, const char *cache_dir) {
	net::Init();  // This needs to happen before we load the config. So on Windows we also run it in Main. It's fine to call multiple times.

	ShaderTranslationInit();

	g_threadManager.Init(cpu_info.num_cores, cpu_info.logical_cpu_count);

	// Make sure UI state is MENU.
	ResetUIState();

	bool skipLogo = false;
	setlocale( LC_ALL, "C" );
	std::string user_data_path = savegame_dir;
	pendingMessages.clear();
	g_requestManager.Clear();

	// external_dir has all kinds of meanings depending on platform.
	// on iOS it's even the path to bundled app assets. It's a mess.

	// We want this to be FIRST.
#if PPSSPP_PLATFORM(IOS) || PPSSPP_PLATFORM(MAC)
	// Packed assets are included in app
	g_VFS.Register("", new DirectoryReader(Path(external_dir)));
#endif
#if defined(ASSETS_DIR)
	g_VFS.Register("", new DirectoryReader(Path(ASSETS_DIR)));
#endif
#if !defined(MOBILE_DEVICE) && !defined(_WIN32) && !PPSSPP_PLATFORM(SWITCH)
	g_VFS.Register("", new DirectoryReader(File::GetExeDirectory() / "assets"));
	g_VFS.Register("", new DirectoryReader(File::GetExeDirectory()));
	g_VFS.Register("", new DirectoryReader(Path("/usr/local/share/ppsspp/assets")));
	g_VFS.Register("", new DirectoryReader(Path("/usr/local/share/games/ppsspp/assets")));
	g_VFS.Register("", new DirectoryReader(Path("/usr/share/ppsspp/assets")));
	g_VFS.Register("", new DirectoryReader(Path("/usr/share/games/ppsspp/assets")));
#endif

#if PPSSPP_PLATFORM(SWITCH)
	Path assetPath = Path(user_data_path) / "assets";
	g_VFS.Register("", new DirectoryReader(assetPath));
#else
	g_VFS.Register("", new DirectoryReader(Path("assets")));
#endif
	g_VFS.Register("", new DirectoryReader(Path(savegame_dir)));

	g_Config.defaultCurrentDirectory = Path("/");
	g_Config.internalDataDirectory = Path(savegame_dir);

#if PPSSPP_PLATFORM(ANDROID)
	// In Android 12 with scoped storage, due to the above, the external directory
	// is no longer the plain root of external storage, but it's an app specific directory
	// on external storage (g_extFilesDir).
	if (System_GetPropertyBool(SYSPROP_ANDROID_SCOPED_STORAGE)) {
		// There's no sensible default directory. Let the user browse for files.
		g_Config.defaultCurrentDirectory.clear();
	} else {
		g_Config.memStickDirectory = Path(external_dir);
		g_Config.defaultCurrentDirectory = Path(external_dir);
	}

	// Might also add an option to move it to internal / non-visible storage, but there's
	// little point, really.

	g_Config.flash0Directory = Path(external_dir) / "flash0";

	Path memstickDirFile = g_Config.internalDataDirectory / "memstick_dir.txt";
	if (File::Exists(memstickDirFile)) {
		INFO_LOG(SYSTEM, "Reading '%s' to find memstick dir.", memstickDirFile.c_str());
		std::string memstickDir;
		if (File::ReadFileToString(true, memstickDirFile, memstickDir)) {
			Path memstickPath(memstickDir);
			if (!memstickPath.empty() && File::Exists(memstickPath)) {
				g_Config.memStickDirectory = memstickPath;
				INFO_LOG(SYSTEM, "Memstick Directory from memstick_dir.txt: '%s'", g_Config.memStickDirectory.c_str());
			} else {
				ERROR_LOG(SYSTEM, "Couldn't read directory '%s' specified by memstick_dir.txt.", memstickDir.c_str());
				if (System_GetPropertyBool(SYSPROP_ANDROID_SCOPED_STORAGE)) {
					// Ask the user to configure a memstick directory.
					INFO_LOG(SYSTEM, "Asking the user.");
					g_Config.memStickDirectory.clear();
				}
			}
		}
	} else {
		INFO_LOG(SYSTEM, "No memstick directory file found (tried to open '%s')", memstickDirFile.c_str());
	}

	// Attempt to create directories after reading the path.
	if (!System_GetPropertyBool(SYSPROP_ANDROID_SCOPED_STORAGE)) {
		CreateDirectoriesAndroid();
	}

#elif PPSSPP_PLATFORM(IOS)
	g_Config.defaultCurrentDirectory = g_Config.internalDataDirectory;
	g_Config.memStickDirectory = DarwinFileSystemServices::appropriateMemoryStickDirectoryToUse();
	g_Config.flash0Directory = Path(std::string(external_dir)) / "flash0";
#elif PPSSPP_PLATFORM(MAC)
	g_Config.defaultCurrentDirectory = Path(getenv("HOME"));
	g_Config.memStickDirectory = DarwinFileSystemServices::appropriateMemoryStickDirectoryToUse();
	g_Config.flash0Directory = Path(std::string(external_dir)) / "flash0";
#elif PPSSPP_PLATFORM(SWITCH)
	g_Config.memStickDirectory = g_Config.internalDataDirectory / "config/ppsspp";
	g_Config.flash0Directory = g_Config.internalDataDirectory / "assets/flash0";
#elif !PPSSPP_PLATFORM(WINDOWS)
	std::string config;
	if (getenv("XDG_CONFIG_HOME") != NULL)
		config = getenv("XDG_CONFIG_HOME");
	else if (getenv("HOME") != NULL)
		config = getenv("HOME") + std::string("/.config");
	else // Just in case
		config = "./config";

	g_Config.memStickDirectory = Path(config) / "ppsspp";
	g_Config.flash0Directory = File::GetExeDirectory() / "assets/flash0";
	if (getenv("HOME") != nullptr) {
		g_Config.defaultCurrentDirectory = Path(getenv("HOME"));
	} else {
		// Hm, should probably actually explicitly set the current directory..
		// Though it's not many platforms that'll land us here.
		g_Config.currentDirectory = Path(".");
	}
#endif

#if (PPSSPP_PLATFORM(WINDOWS) && !PPSSPP_PLATFORM(UWP)) || PPSSPP_PLATFORM(MAC)
	if (g_Config.currentDirectory.empty()) {
		g_Config.currentDirectory = Path("/");
	}
#endif

	if (cache_dir && strlen(cache_dir)) {
		g_Config.appCacheDirectory = Path(cache_dir);
		DiskCachingFileLoaderCache::SetCacheDir(g_Config.appCacheDirectory);
	}

	if (!LogManager::GetInstance()) {
		LogManager::Init(&g_Config.bEnableLogging);
	}

#if !PPSSPP_PLATFORM(WINDOWS)
	g_Config.SetSearchPath(GetSysDirectory(DIRECTORY_SYSTEM));

	// Note that if we don't have storage permission here, loading the config will
	// fail and it will be set to the default. Later, we load again when we get permission.
	g_Config.Load();
#endif

	LogManager *logman = LogManager::GetInstance();

	const char *fileToLog = 0;
	Path stateToLoad;

	bool gotBootFilename = false;
	bool gotoGameSettings = false;
	bool gotoTouchScreenTest = false;
	boot_filename.clear();

	// Parse command line
	LogTypes::LOG_LEVELS logLevel = LogTypes::LINFO;
	bool forceLogLevel = false;
	const auto setLogLevel = [&logLevel, &forceLogLevel](LogTypes::LOG_LEVELS level) {
		logLevel = level;
		forceLogLevel = true;
	};

	for (int i = 1; i < argc; i++) {
		if (argv[i][0] == '-') {
#if defined(__APPLE__)
			// On Apple system debugged executable may get -NSDocumentRevisionsDebugMode YES in argv.
			if (!strcmp(argv[i], "-NSDocumentRevisionsDebugMode") && argc - 1 > i) {
				i++;
				continue;
			}
#endif
			switch (argv[i][1]) {
			case 'd':
				// Enable debug logging
				// Note that you must also change the max log level in Log.h.
				setLogLevel(LogTypes::LDEBUG);
				break;
			case 'v':
				// Enable verbose logging
				// Note that you must also change the max log level in Log.h.
				setLogLevel(LogTypes::LVERBOSE);
				break;
			case 'j':
				g_Config.iCpuCore = (int)CPUCore::JIT;
				g_Config.bSaveSettings = false;
				break;
			case 'i':
				g_Config.iCpuCore = (int)CPUCore::INTERPRETER;
				g_Config.bSaveSettings = false;
				break;
			case 'r':
				g_Config.iCpuCore = (int)CPUCore::IR_JIT;
				g_Config.bSaveSettings = false;
				break;
			case '-':
				if (!strncmp(argv[i], "--loglevel=", strlen("--loglevel=")) && strlen(argv[i]) > strlen("--loglevel="))
					setLogLevel(static_cast<LogTypes::LOG_LEVELS>(std::atoi(argv[i] + strlen("--loglevel="))));
				if (!strncmp(argv[i], "--log=", strlen("--log=")) && strlen(argv[i]) > strlen("--log="))
					fileToLog = argv[i] + strlen("--log=");
				if (!strncmp(argv[i], "--state=", strlen("--state=")) && strlen(argv[i]) > strlen("--state="))
					stateToLoad = Path(std::string(argv[i] + strlen("--state=")));
#if !defined(MOBILE_DEVICE)
				if (!strncmp(argv[i], "--escape-exit", strlen("--escape-exit")))
					g_Config.bPauseExitsEmulator = true;
#endif
				if (!strncmp(argv[i], "--pause-menu-exit", strlen("--pause-menu-exit")))
					g_Config.bPauseMenuExitsEmulator = true;
				if (!strcmp(argv[i], "--fullscreen")) {
					g_Config.iForceFullScreen = 1;
					System_ToggleFullscreenState("1");
				}
				if (!strcmp(argv[i], "--windowed")) {
					g_Config.iForceFullScreen = 0;
					System_ToggleFullscreenState("0");
				}
				if (!strcmp(argv[i], "--touchscreentest"))
					gotoTouchScreenTest = true;
				if (!strcmp(argv[i], "--gamesettings"))
					gotoGameSettings = true;
				if (!strncmp(argv[i], "--appendconfig=", strlen("--appendconfig=")) && strlen(argv[i]) > strlen("--appendconfig=")) {
					g_Config.SetAppendedConfigIni(Path(std::string(argv[i] + strlen("--appendconfig="))));
					g_Config.LoadAppendedConfig();
				}
				break;
			}
		} else {
			// This parameter should be a boot filename. Only accept it if we
			// don't already have one.
			if (!gotBootFilename) {
				gotBootFilename = true;
				INFO_LOG(SYSTEM, "Boot filename found in args: '%s'", argv[i]);

				bool okToLoad = true;
				bool okToCheck = true;
				if (System_GetPropertyBool(SYSPROP_SUPPORTS_PERMISSIONS)) {
					PermissionStatus status = System_GetPermissionStatus(SYSTEM_PERMISSION_STORAGE);
					if (status == PERMISSION_STATUS_DENIED) {
						ERROR_LOG(IO, "Storage permission denied. Launching without argument.");
						okToLoad = false;
						okToCheck = false;
					} else if (status != PERMISSION_STATUS_GRANTED) {
						ERROR_LOG(IO, "Storage permission not granted. Launching without argument check.");
						okToCheck = false;
					} else {
						INFO_LOG(IO, "Storage permission granted.");
					}
				}
				if (okToLoad) {
					std::string str = std::string(argv[i]);
					// Handle file:/// URIs, since you get those when creating shortcuts on some Android systems.
					if (startsWith(str, "file:///")) {
						str = UriDecode(str.substr(7));
						INFO_LOG(IO, "Decoding '%s' to '%s'", argv[i], str.c_str());
					}

					boot_filename = Path(str);
					skipLogo = true;
				}
				if (okToLoad && okToCheck) {
					std::unique_ptr<FileLoader> fileLoader(ConstructFileLoader(boot_filename));
					if (!fileLoader->Exists()) {
						fprintf(stderr, "File not found: %s\n", boot_filename.c_str());
#if defined(_WIN32) || defined(__ANDROID__)
						// Ignore and proceed.
#else
						// Bail.
						exit(1);
#endif
					}
				}
			} else {
				fprintf(stderr, "Can only boot one file");
#if defined(_WIN32) || defined(__ANDROID__)
				// Ignore and proceed.
#else
				// Bail.
				exit(1);
#endif
			}
		}
	}

	if (fileToLog)
		LogManager::GetInstance()->ChangeFileLog(fileToLog);

	if (forceLogLevel)
		LogManager::GetInstance()->SetAllLogLevels(logLevel);

	PostLoadConfig();

#if PPSSPP_PLATFORM(ANDROID)
	logger = new AndroidLogger();
	logman->AddListener(logger);
#elif (defined(MOBILE_DEVICE) && !defined(_DEBUG))
	// Enable basic logging for any kind of mobile device, since LogManager doesn't.
	// The MOBILE_DEVICE/_DEBUG condition matches LogManager.cpp.
	logger = new PrintfLogger();
	logman->AddListener(logger);
#endif

	if (System_GetPropertyBool(SYSPROP_SUPPORTS_PERMISSIONS)) {
		if (System_GetPermissionStatus(SYSTEM_PERMISSION_STORAGE) != PERMISSION_STATUS_GRANTED) {
			System_AskForPermission(SYSTEM_PERMISSION_STORAGE);
		}
	}

	auto des = GetI18NCategory(I18NCat::DESKTOPUI);
	// Note to translators: do not translate this/add this to PPSSPP-lang's files.
	// It's intended to be custom for every user.
	// Only add it to your own personal copies of PPSSPP.
#if PPSSPP_PLATFORM(UWP)
	// Roboto font is loaded in TextDrawerUWP.
	g_Config.sFont = des->T("Font", "Roboto");
#elif defined(USING_WIN_UI) && !PPSSPP_PLATFORM(UWP)
	// TODO: Could allow a setting to specify a font file to load?
	// TODO: Make this a constant if we can sanely load the font on other systems?
	AddFontResourceEx(L"assets/Roboto-Condensed.ttf", FR_PRIVATE, NULL);
	// The font goes by two names, let's allow either one.
	if (CheckFontIsUsable(L"Roboto Condensed")) {
		g_Config.sFont = des->T("Font", "Roboto Condensed");
	} else {
		g_Config.sFont = des->T("Font", "Roboto");
	}
#elif defined(USING_QT_UI)
	size_t fontSize = 0;
	uint8_t *fontData = g_VFS.ReadFile("Roboto-Condensed.ttf", &fontSize);
	if (fontData) {
		int fontID = QFontDatabase::addApplicationFontFromData(QByteArray((const char *)fontData, fontSize));
		delete [] fontData;

		QStringList fontsFound = QFontDatabase::applicationFontFamilies(fontID);
		if (fontsFound.size() >= 1) {
			// Might be "Roboto" or "Roboto Condensed".
			g_Config.sFont = des->T("Font", fontsFound.at(0).toUtf8().constData());
		}
	} else {
		// Let's try for it being a system font.
		g_Config.sFont = des->T("Font", "Roboto Condensed");
	}
#endif

	// TODO: Load these in the background instead of synchronously.
	g_BackgroundAudio.LoadSamples();

	if (!boot_filename.empty() && stateToLoad.Valid()) {
		SaveState::Load(stateToLoad, -1, [](SaveState::Status status, const std::string &message, void *) {
			if (!message.empty() && (!g_Config.bDumpFrames || !g_Config.bDumpVideoOutput)) {
				osm.Show(message, status == SaveState::Status::SUCCESS ? 2.0 : 5.0);
			}
		});
	}

	DEBUG_LOG(SYSTEM, "ScreenManager!");
	g_screenManager = new ScreenManager();
	if (g_Config.memStickDirectory.empty()) {
		INFO_LOG(SYSTEM, "No memstick directory! Asking for one to be configured.");
		g_screenManager->switchScreen(new LogoScreen(AfterLogoScreen::MEMSTICK_SCREEN_INITIAL_SETUP));
	} else if (gotoGameSettings) {
		g_screenManager->switchScreen(new LogoScreen(AfterLogoScreen::TO_GAME_SETTINGS));
	} else if (gotoTouchScreenTest) {
		g_screenManager->switchScreen(new MainScreen());
		g_screenManager->push(new TouchTestScreen(Path()));
	} else if (skipLogo) {
		g_screenManager->switchScreen(new EmuScreen(boot_filename));
	} else {
		g_screenManager->switchScreen(new LogoScreen(AfterLogoScreen::DEFAULT));
	}

	// Easy testing
	// screenManager->push(new GPUDriverTestScreen());

	if (g_Config.bRemoteShareOnStartup && g_Config.bRemoteDebuggerOnStartup)
		StartWebServer(WebServerFlags::ALL);
	else if (g_Config.bRemoteShareOnStartup)
		StartWebServer(WebServerFlags::DISCS);
	else if (g_Config.bRemoteDebuggerOnStartup)
		StartWebServer(WebServerFlags::DEBUGGER);

	std::string sysName = System_GetProperty(SYSPROP_NAME);
	isOuya = KeyMap::IsOuya(sysName);

	// We do this here, instead of in NativeInitGraphics, because the display may be reset.
	// When it's reset we don't want to forget all our managed things.
	CheckFailedGPUBackends();
	SetGPUBackend((GPUBackend) g_Config.iGPUBackend);
	renderCounter = 0;

	// Must be done restarting by now.
	restarting = false;
}

void RenderOverlays(UIContext *dc, void *userdata);
bool CreateGlobalPipelines();

bool NativeInitGraphics(GraphicsContext *graphicsContext) {
	INFO_LOG(SYSTEM, "NativeInitGraphics");

	_assert_(g_screenManager);

	// We set this now so any resize during init is processed later.
	resized = false;

	Core_SetGraphicsContext(graphicsContext);
	g_draw = graphicsContext->GetDrawContext();

	_assert_(g_draw);

	if (!CreateGlobalPipelines()) {
		ERROR_LOG(G3D, "Failed to create global pipelines");
		return false;
	}

	ui_draw2d.SetAtlas(GetUIAtlas());
	ui_draw2d.SetFontAtlas(GetFontAtlas());
	ui_draw2d_front.SetAtlas(GetUIAtlas());
	ui_draw2d_front.SetFontAtlas(GetFontAtlas());

	uiContext = new UIContext();
	uiContext->theme = GetTheme();
	UpdateTheme(uiContext);

	ui_draw2d.Init(g_draw, texColorPipeline);
	ui_draw2d_front.Init(g_draw, texColorPipeline);

	uiContext->Init(g_draw, texColorPipeline, colorPipeline, &ui_draw2d, &ui_draw2d_front);
	if (uiContext->Text())
		uiContext->Text()->SetFont("Tahoma", 20, 0);

	g_screenManager->setUIContext(uiContext);
	g_screenManager->setDrawContext(g_draw);
	g_screenManager->setPostRenderCallback(&RenderOverlays, nullptr);
	g_screenManager->deviceRestored();

#ifdef _WIN32
	winAudioBackend = CreateAudioBackend((AudioBackendType)g_Config.iAudioBackend);
#if PPSSPP_PLATFORM(UWP)
	winAudioBackend->Init(0, &NativeMix, 44100);
#else
	winAudioBackend->Init(MainWindow::GetHWND(), &NativeMix, 44100);
#endif
#endif

#if defined(_WIN32) && !PPSSPP_PLATFORM(UWP)
	if (IsWin7OrHigher()) {
		winCamera = new WindowsCaptureDevice(CAPTUREDEVIDE_TYPE::VIDEO);
		winCamera->sendMessage({ CAPTUREDEVIDE_COMMAND::INITIALIZE, nullptr });
		winMic = new WindowsCaptureDevice(CAPTUREDEVIDE_TYPE::AUDIO);
		winMic->sendMessage({ CAPTUREDEVIDE_COMMAND::INITIALIZE, nullptr });
	}
#endif

	g_gameInfoCache = new GameInfoCache();

	if (gpu) {
		gpu->DeviceRestore(g_draw);
	}

	INFO_LOG(SYSTEM, "NativeInitGraphics completed");

	return true;
}

bool CreateGlobalPipelines() {
	using namespace Draw;

	ShaderModule *vs_color_2d = g_draw->GetVshaderPreset(VS_COLOR_2D);
	ShaderModule *fs_color_2d = g_draw->GetFshaderPreset(FS_COLOR_2D);
	ShaderModule *vs_texture_color_2d = g_draw->GetVshaderPreset(VS_TEXTURE_COLOR_2D);
	ShaderModule *fs_texture_color_2d = g_draw->GetFshaderPreset(FS_TEXTURE_COLOR_2D);

	if (!vs_color_2d || !fs_color_2d || !vs_texture_color_2d || !fs_texture_color_2d) {
		ERROR_LOG(G3D, "Failed to get shader preset");
		return false;
	}

	InputLayout *inputLayout = ui_draw2d.CreateInputLayout(g_draw);
	BlendState *blendNormal = g_draw->CreateBlendState({ true, 0xF, BlendFactor::SRC_ALPHA, BlendFactor::ONE_MINUS_SRC_ALPHA });
	DepthStencilState *depth = g_draw->CreateDepthStencilState({ false, false, Comparison::LESS });
	RasterState *rasterNoCull = g_draw->CreateRasterState({});

	PipelineDesc colorDesc{
		Primitive::TRIANGLE_LIST,
		{ vs_color_2d, fs_color_2d },
		inputLayout, depth, blendNormal, rasterNoCull, &vsColBufDesc,
	};
	PipelineDesc texColorDesc{
		Primitive::TRIANGLE_LIST,
		{ vs_texture_color_2d, fs_texture_color_2d },
		inputLayout, depth, blendNormal, rasterNoCull, &vsTexColBufDesc,
	};

	colorPipeline = g_draw->CreateGraphicsPipeline(colorDesc, "global_color");
	if (!colorPipeline) {
		// Something really critical is wrong, don't care much about correct releasing of the states.
		return false;
	}

	texColorPipeline = g_draw->CreateGraphicsPipeline(texColorDesc, "global_texcolor");
	if (!texColorPipeline) {
		// Something really critical is wrong, don't care much about correct releasing of the states.
		return false;
	}

	// Release these now, reference counting should ensure that they get completely released
	// once we delete both pipelines.
	inputLayout->Release();
	rasterNoCull->Release();
	blendNormal->Release();
	depth->Release();
	return true;
}

void NativeShutdownGraphics() {
	INFO_LOG(SYSTEM, "NativeShutdownGraphics");

	if (g_screenManager) {
		g_screenManager->deviceLost();
	}

	if (gpu)
		gpu->DeviceLost();

#if PPSSPP_PLATFORM(WINDOWS)
	delete winAudioBackend;
	winAudioBackend = nullptr;
#endif

#if PPSSPP_PLATFORM(WINDOWS) && !PPSSPP_PLATFORM(UWP)
	if (winCamera) {
		winCamera->waitShutDown();
		delete winCamera;
		winCamera = nullptr;
	}
	if (winMic) {
		winMic->waitShutDown();
		delete winMic;
		winMic = nullptr;
	}
#endif

	UIBackgroundShutdown();

	delete g_gameInfoCache;
	g_gameInfoCache = nullptr;

	delete uiContext;
	uiContext = nullptr;

	ui_draw2d.Shutdown();
	ui_draw2d_front.Shutdown();

	if (colorPipeline) {
		colorPipeline->Release();
		colorPipeline = nullptr;
	}
	if (texColorPipeline) {
		texColorPipeline->Release();
		texColorPipeline = nullptr;
	}

	INFO_LOG(SYSTEM, "NativeShutdownGraphics done");
}

void TakeScreenshot() {
	g_TakeScreenshot = false;

	Path path = GetSysDirectory(DIRECTORY_SCREENSHOT);
	if (!File::Exists(path)) {
		File::CreateDir(path);
	}

	// First, find a free filename.
	int i = 0;

	std::string gameId = g_paramSFO.GetDiscID();

	Path filename;
	while (i < 10000){
		if (g_Config.bScreenshotsAsPNG)
			filename = path / StringFromFormat("%s_%05d.png", gameId.c_str(), i);
		else
			filename = path / StringFromFormat("%s_%05d.jpg", gameId.c_str(), i);
		File::FileInfo info;
		if (!File::Exists(filename))
			break;
		i++;
	}

	bool success = TakeGameScreenshot(filename, g_Config.bScreenshotsAsPNG ? ScreenshotFormat::PNG : ScreenshotFormat::JPG, SCREENSHOT_OUTPUT);
	if (success) {
		osm.Show(filename.ToVisualString());
	} else {
		auto err = GetI18NCategory(I18NCat::ERRORS);
		osm.Show(err->T("Could not save screenshot file"));
	}
}

void RenderOverlays(UIContext *dc, void *userdata) {
	// Thin bar at the top of the screen.
	std::vector<float> progress = g_DownloadManager.GetCurrentProgress();
	if (!progress.empty()) {
		static const uint32_t colors[4] = {
			0xFFFFFFFF,
			0xFFCCCCCC,
			0xFFAAAAAA,
			0xFF777777,
		};

		dc->Begin();
		int h = 5;
		for (size_t i = 0; i < progress.size(); i++) {
			float barWidth = 10 + (dc->GetBounds().w - 10) * progress[i];
			Bounds bounds(0, h * i, barWidth, h);
			UI::Drawable solid(colors[i & 3]);
			dc->FillRect(solid, bounds);
		}
		dc->Flush();
	}

	if (g_TakeScreenshot) {
		TakeScreenshot();
	}
}

void NativeRender(GraphicsContext *graphicsContext) {
	_dbg_assert_(graphicsContext != nullptr);
	_dbg_assert_(g_screenManager != nullptr);

	g_GameManager.Update();

	if (GetUIState() != UISTATE_INGAME) {
		// Note: We do this from NativeRender so that the graphics context is
		// guaranteed valid, to be safe - g_gameInfoCache messes around with textures.
		g_BackgroundAudio.Update();
	}

	float xres = g_display.dp_xres;
	float yres = g_display.dp_yres;

	// Apply the UIContext bounds as a 2D transformation matrix.
	// TODO: This should be moved into the draw context...
	Matrix4x4 ortho;
	switch (GetGPUBackend()) {
	case GPUBackend::VULKAN:
		ortho.setOrthoD3D(0.0f, xres, 0, yres, -1.0f, 1.0f);
		break;
	case GPUBackend::DIRECT3D9:
		ortho.setOrthoD3D(0.0f, xres, yres, 0.0f, -1.0f, 1.0f);
		Matrix4x4 translation;
		// Account for the small window adjustment.
		translation.setTranslation(Vec3(
			-0.5f * g_display.dpi_scale_x / g_display.dpi_scale_real_x,
			-0.5f * g_display.dpi_scale_y / g_display.dpi_scale_real_y, 0.0f));
		ortho = translation * ortho;
		break;
	case GPUBackend::DIRECT3D11:
		ortho.setOrthoD3D(0.0f, xres, yres, 0.0f, -1.0f, 1.0f);
		break;
	case GPUBackend::OPENGL:
	default:
		ortho.setOrtho(0.0f, xres, yres, 0.0f, -1.0f, 1.0f);
		break;
	}

	// Compensate for rotated display if needed.
	if (g_display.rotation != DisplayRotation::ROTATE_0) {
		ortho = ortho * g_display.rot_matrix;
	}

	ui_draw2d.PushDrawMatrix(ortho);
	ui_draw2d_front.PushDrawMatrix(ortho);

	g_screenManager->getUIContext()->SetTintSaturation(g_Config.fUITint, g_Config.fUISaturation);

	Draw::DebugFlags debugFlags = Draw::DebugFlags::NONE;
	if (g_Config.bShowGpuProfile)
		debugFlags |= Draw::DebugFlags::PROFILE_TIMESTAMPS;
	if (g_Config.bGpuLogProfiler)
		debugFlags |= Draw::DebugFlags::PROFILE_SCOPES;
	g_screenManager->getDrawContext()->SetDebugFlags(debugFlags);

	// All actual rendering happen in here.
	g_screenManager->render();
	if (g_screenManager->getUIContext()->Text()) {
		g_screenManager->getUIContext()->Text()->OncePerFrame();
	}

	if (resized) {
		INFO_LOG(G3D, "Resized flag set - recalculating bounds");
		resized = false;

		if (uiContext) {
			// Modifying the bounds here can be used to "inset" the whole image to gain borders for TV overscan etc.
			// The UI now supports any offset but not the EmuScreen yet.
			uiContext->SetBounds(Bounds(0, 0, g_display.dp_xres, g_display.dp_yres));

			// OSX 10.6 and SDL 1.2 bug.
#if defined(__APPLE__) && !defined(USING_QT_UI)
			static int dp_xres_old = g_display.dp_xres;
			if (g_display.dp_xres != dp_xres_old) {
				dp_xres_old = g_display.dp_xres;
			}
#endif
		}

		graphicsContext->Resize();
		g_screenManager->resized();

		// TODO: Move this to the GraphicsContext objects for each backend.
#if !PPSSPP_PLATFORM(WINDOWS) && !defined(ANDROID)
		PSP_CoreParameter().pixelWidth = g_display.pixel_xres;
		PSP_CoreParameter().pixelHeight = g_display.pixel_yres;
		NativeMessageReceived("gpu_displayResized", "");
#endif
	} else {
		// INFO_LOG(G3D, "Polling graphics context");
		graphicsContext->Poll();
	}

	ui_draw2d.PopDrawMatrix();
	ui_draw2d_front.PopDrawMatrix();

	if (renderCounter < 10 && ++renderCounter == 10) {
		// We're rendering fine, clear out failure info.
		ClearFailedGPUBackends();
	}
}

void HandleGlobalMessage(const std::string &msg, const std::string &value) {
	InputDeviceID nextInputDeviceID = DEVICE_ID_ANY;
	if (msg == "inputDeviceConnectedID") {
		nextInputDeviceID = (InputDeviceID)parseLong(value);
	}
	else if (msg == "inputDeviceConnected") {
		KeyMap::NotifyPadConnected(nextInputDeviceID, value);
	}
	else if (msg == "savestate_displayslot") {
		auto sy = GetI18NCategory(I18NCat::SYSTEM);
		std::string msg = StringFromFormat("%s: %d", sy->T("Savestate Slot"), SaveState::GetCurrentSlot() + 1);
		// Show for the same duration as the preview.
		osm.Show(msg, 2.0f, 0xFFFFFF, -1, true, "savestate_slot");
	}
	else if (msg == "gpu_displayResized") {
		if (gpu) {
			gpu->NotifyDisplayResized();
		}
	}
	else if (msg == "gpu_renderResized") {
		if (gpu) {
			gpu->NotifyRenderResized();
		}
	}
	else if (msg == "gpu_configChanged") {
		if (gpu) {
			gpu->NotifyConfigChanged();
		}
		Reporting::UpdateConfig();
	}
	else if (msg == "core_powerSaving") {
		if (value != "false") {
			auto sy = GetI18NCategory(I18NCat::SYSTEM);
#if PPSSPP_PLATFORM(ANDROID)
			osm.Show(sy->T("WARNING: Android battery save mode is on"), 2.0f, 0xFFFFFF, -1, true, "core_powerSaving");
#else
			osm.Show(sy->T("WARNING: Battery save mode is on"), 2.0f, 0xFFFFFF, -1, true, "core_powerSaving");
#endif
		}
		Core_SetPowerSaving(value != "false");
	}
	else if (msg == "permission_granted" && value == "storage") {
#if PPSSPP_PLATFORM(ANDROID)
		CreateDirectoriesAndroid();
#endif
		// We must have failed to load the config before, so load it now to avoid overwriting the old config
		// with a freshly generated one.
		// NOTE: If graphics backend isn't what's in the config (due to error fallback, or not matching the default
		// and then getting permission), it will get out of sync. So we save and restore g_Config.iGPUBackend.
		// Ideally we should simply reinitialize graphics to the mode from the config, but there are potential issues
		// and I can't risk it before 1.9.0.
		int gpuBackend = g_Config.iGPUBackend;
		INFO_LOG(IO, "Reloading config after storage permission grant.");
		g_Config.Reload();
		PostLoadConfig();
		g_Config.iGPUBackend = gpuBackend;
	} else if (msg == "app_resumed" || msg == "got_focus") {
		// Assume that the user may have modified things.
		MemoryStick_NotifyWrite();
	}
}

void NativeUpdate() {
	PROFILE_END_FRAME();

	std::vector<PendingMessage> toProcess;
	{
		std::lock_guard<std::mutex> lock(pendingMutex);
		toProcess = std::move(pendingMessages);
		pendingMessages.clear();
	}

	for (const auto &item : toProcess) {
		HandleGlobalMessage(item.msg, item.value);
		g_screenManager->sendMessage(item.msg.c_str(), item.value.c_str());
	}

	g_requestManager.ProcessRequests();

	g_DownloadManager.Update();
	g_screenManager->update();

	g_Discord.Update();
	g_BackgroundAudio.Play();

	UI::SetSoundEnabled(g_Config.bUISound);
}

bool NativeIsAtTopLevel() {
	// This might need some synchronization?
	if (!g_screenManager) {
		ERROR_LOG(SYSTEM, "No screen manager active");
		return false;
	}
	Screen *currentScreen = g_screenManager->topScreen();
	if (currentScreen) {
		bool top = currentScreen->isTopLevel();
		INFO_LOG(SYSTEM, "Screen toplevel: %i", (int)top);
		return currentScreen->isTopLevel();
	} else {
		ERROR_LOG(SYSTEM, "No current screen");
		return false;
	}
}

void NativeTouch(const TouchInput &touch) {
	if (!g_screenManager) {
		return;
	}

	// Brute force prevent NaNs from getting into the UI system.
	// Don't think this is actually necessary in practice.
	if (my_isnan(touch.x) || my_isnan(touch.y)) {
		return;
	}
	g_screenManager->touch(touch);
}

bool NativeKey(const KeyInput &key) {
	// VR actions
	if (IsVREnabled() && !UpdateVRKeys(key)) {
		return false;
	}

	// INFO_LOG(SYSTEM, "Key code: %i flags: %i", key.keyCode, key.flags);
#if !defined(MOBILE_DEVICE)
	if (g_Config.bPauseExitsEmulator) {
		std::vector<int> pspKeys;
		pspKeys.clear();
		if (KeyMap::InputMappingToPspButton(InputMapping(key.deviceId, key.keyCode), &pspKeys)) {
			if (std::find(pspKeys.begin(), pspKeys.end(), VIRTKEY_PAUSE) != pspKeys.end()) {
				System_ExitApp();
				return true;
			}
		}
	}
#endif

	bool retval = false;
	if (g_screenManager) {
		HLEPlugins::SetKey(key.keyCode, (key.flags & KEY_DOWN) ? 1 : 0);
		retval = g_screenManager->key(key);
	}

	// The Mode key can have weird consequences on some devices, see #17245.
	if (key.keyCode == NKCODE_BUTTON_MODE) {
		// Tell the caller that we handled the key.
		retval = true;
	}

	return retval;
}

void NativeAxis(const AxisInput &axis) {
	// VR actions
	if (IsVREnabled() && !UpdateVRAxis(axis)) {
		return;
	}

	if (!g_screenManager) {
		// Too early.
		return;
	}

	using namespace TiltEventProcessor;

	// only do special handling of tilt events if tilt is enabled.
	HLEPlugins::PluginDataAxis[axis.axisId] = axis.value;
	g_screenManager->axis(axis);

	if (g_Config.iTiltInputType == TILT_NULL) {
		// if tilt events are disabled, don't do anything special.
		return;
	}

	// figure out what the current tilt orientation is by checking the axis event
	// This is static, since we need to remember where we last were (in terms of orientation)
	static float tiltX;
	static float tiltY;
	static float tiltZ;

	switch (axis.axisId) {
		case JOYSTICK_AXIS_ACCELEROMETER_X: tiltX = axis.value; break;
		case JOYSTICK_AXIS_ACCELEROMETER_Y: tiltY = axis.value; break;
		case JOYSTICK_AXIS_ACCELEROMETER_Z: tiltZ = axis.value; break;
		default: break;
	}

	// create the base coordinate tilt system from the calibration data.
	float tiltBaseAngleY = g_Config.fTiltBaseAngleY;

	// Figure out the sensitivity of the tilt. (sensitivity is originally 0 - 100)
	// We divide by 50, so that the rest of the 50 units can be used to overshoot the
	// target. If you want precise control, you'd keep the sensitivity ~50.
	// For games that don't need much control but need fast reactions,
	// then a value of 70-80 is the way to go.
	float xSensitivity = g_Config.iTiltSensitivityX / 50.0;
	float ySensitivity = g_Config.iTiltSensitivityY / 50.0;

	// x and y are flipped if we are in landscape orientation. The events are
	// sent with respect to the portrait coordinate system, while we
	// take all events in landscape.
	// see [http://developer.android.com/guide/topics/sensors/sensors_overview.html] for details
	bool landscape = g_display.dp_yres < g_display.dp_xres;
	// now transform out current tilt to the calibrated coordinate system
	ProcessTilt(landscape, tiltBaseAngleY, tiltX, tiltY, tiltZ,
		g_Config.bInvertTiltX, g_Config.bInvertTiltY,
		xSensitivity, ySensitivity);
}

void NativeMessageReceived(const char *message, const char *value) {
	std::lock_guard<std::mutex> lock(pendingMutex);
	PendingMessage pendingMessage;
	pendingMessage.msg = message;
	pendingMessage.value = value;
	pendingMessages.push_back(pendingMessage);
}

void System_PostUIMessage(const std::string &message, const std::string &value) {
	NativeMessageReceived(message.c_str(), value.c_str());
}

void System_NotifyUserMessage(const std::string &message, float duration_s, u32 color, const char *id) {
	osm.Show(message, duration_s, color, -1, true, id);
}

void NativeResized() {
	// NativeResized can come from any thread so we just set a flag, then process it later.
	VERBOSE_LOG(G3D, "NativeResized - setting flag");
	resized = true;
}

void NativeSetRestarting() {
	restarting = true;
}

bool NativeIsRestarting() {
	return restarting;
}

void NativeShutdown() {
	if (g_screenManager) {
		g_screenManager->shutdown();
		delete g_screenManager;
		g_screenManager = nullptr;
	}

	g_Config.Save("NativeShutdown");

	INFO_LOG(SYSTEM, "NativeShutdown called");

	g_i18nrepo.LogMissingKeys();

	ShutdownWebServer();

#if PPSSPP_PLATFORM(ANDROID) || PPSSPP_PLATFORM(IOS)
	System_ExitApp();
#endif

	net::Shutdown();

	g_Discord.Shutdown();

	ShaderTranslationShutdown();

	// Avoid shutting this down when restarting core.
	if (!restarting)
		LogManager::Shutdown();

	if (logger) {
		delete logger;
		logger = nullptr;
	}

	g_threadManager.Teardown();

#if !(PPSSPP_PLATFORM(ANDROID) || PPSSPP_PLATFORM(IOS))
	System_ExitApp();
#endif

	// Previously we did exit() here on Android but that makes it hard to do things like restart on backend change.
	// I think we handle most globals correctly or correct-enough now.
}
