#pragma once

// I18N = I....18..dots.....N = INTERNATIONALIZATION

// Super simple I18N library.
// Just enough to be useful and usable.
// Spits out easy-to-edit utf-8 .INI files.

// As usual, everything is UTF-8. Nothing else allowed.

#include <map>
#include <mutex>
#include <string>
#include <vector>
#include <memory>

#include "Common/Common.h"
#include "Common/File/Path.h"

// Reasonably thread safe.

class I18NRepo;
class IniFile;
class Section;

enum class I18NCat : uint8_t {
	AUDIO = 0,
	CONTROLS,
	CWCHEATS,
	DESKTOPUI,
	DEVELOPER,
	DIALOG,
	ERRORS,  // Can't name it ERROR, clashes with many defines.
	GAME,
	GRAPHICS,
	INSTALLZIP,
	KEYMAPPING,
	MAINMENU,
	MAINSETTINGS,
	MAPPABLECONTROLS,
	NETWORKING,
	PAUSE,
	POSTSHADERS,
	PSPCREDITS,
	MEMSTICK,
	REMOTEISO,
	REPORTING,
	SAVEDATA,
	SCREEN,
	SEARCH,
	STORE,
	SYSINFO,
	SYSTEM,
	TEXTURESHADERS,
	THEMES,
	UI_ELEMENTS,
	UPGRADE,
	VR,
	CATEGORY_COUNT,
	NONE = CATEGORY_COUNT,
};

struct I18NEntry {
	I18NEntry(const std::string &t) : text(t), readFlag(false) {}
	I18NEntry() : readFlag(false) {}
	std::string text;
	bool readFlag;
};

struct I18NCandidate {
	I18NCandidate() : key(0), defVal(0) {}
	I18NCandidate(const char *k, const char *d) : key(k), defVal(d) {}
	const char *key;
	const char *defVal;
};

class I18NCategory {
public:
	I18NCategory() {}
	explicit I18NCategory(const Section &section);

	const char *T(const char *key, const char *def = nullptr);
	const char *T(const std::string &key) {
		return T(key.c_str(), nullptr);
	}

	const std::map<std::string, std::string> Missed() const {
		std::lock_guard<std::mutex> guard(missedKeyLock_);
		return missedKeyLog_;
	}

	const std::map<std::string, I18NEntry> &GetMap() { return map_; }
	void ClearMissed() { missedKeyLog_.clear(); }
	void Clear();

private:
	I18NCategory(I18NRepo *repo, const char *name) {}
	void SetMap(const std::map<std::string, std::string> &m);

	std::map<std::string, I18NEntry> map_;
	mutable std::mutex missedKeyLock_;
	std::map<std::string, std::string> missedKeyLog_;

	// Noone else can create these.
	friend class I18NRepo;
};

class I18NRepo {
public:
	I18NRepo();
	bool IniExists(const std::string &languageID) const;
	bool LoadIni(const std::string &languageID, const Path &overridePath = Path()); // NOT the filename!

	std::string LanguageID();

	std::shared_ptr<I18NCategory> GetCategory(I18NCat category);
	std::shared_ptr<I18NCategory> GetCategoryByName(const char *name);

	const char *T(I18NCat category, const char *key, const char *def = nullptr) {
		if (category == I18NCat::NONE)
			return def ? def : key;
		return cats_[(size_t)category]->T(key, def);
	}

	void LogMissingKeys() const;

private:
	Path GetIniPath(const std::string &languageID) const;
	void Clear();

	mutable std::mutex catsLock_;
	std::shared_ptr<I18NCategory> cats_[(size_t)I18NCat::CATEGORY_COUNT];
	std::string languageID_;
};

extern I18NRepo g_i18nrepo;

// These are simply talking to the one global instance of I18NRepo.

std::shared_ptr<I18NCategory> GetI18NCategory(I18NCat cat);

inline const char *T(I18NCat category, const char *key, const char *def = nullptr) {
	return g_i18nrepo.T(category, key, def);
}
