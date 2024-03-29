#include "Common/System/System.h"
#include "Core/HW/StereoResampler.h"  // TODO: doesn't belong in Core/HW...
#include "UI/AudioCommon.h"

StereoResampler g_resampler;

// numFrames is number of stereo frames.
// This is called from *outside* the emulator thread.
int __AudioMix(int16_t *outstereo, int numFrames, int sampleRate) {
	return g_resampler.Mix(outstereo, numFrames, false, sampleRate);
}

void System_AudioGetDebugStats(char *buf, size_t bufSize) {
	if (buf) {
		g_resampler.GetAudioDebugStats(buf, bufSize);
	} else {
		g_resampler.ResetStatCounters();
	}
}

void System_AudioClear() {
	g_resampler.Clear();
}

void System_AudioPushSamples(const int32_t *audio, int numSamples) {
	if (audio) {
		g_resampler.PushSamples(audio, numSamples);
	} else {
		g_resampler.Clear();
	}
}
