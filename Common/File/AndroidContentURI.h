#pragma once

#include <string>

#include "Common/StringUtils.h"
#include "Common/Net/URL.h"
#include "Common/Log.h"

// Utility to deal with Android storage URIs of the forms:
// content://com.android.externalstorage.documents/tree/primary%3APSP%20ISO
// content://com.android.externalstorage.documents/tree/primary%3APSP%20ISO/document/primary%3APSP%20ISO

// This file compiles on all platforms, to reduce the need for ifdefs.

// I am not 100% sure it's OK to rely on the internal format of file content URIs.
// On the other hand, I'm sure tons of apps would break if these changed, so I think we can
// consider them pretty stable. Additionally, the official Document library just manipulates the URIs
// in similar ways...
class AndroidContentURI {
private:
	std::string provider;
	std::string root;
	std::string file;
public:
	AndroidContentURI() {}
	explicit AndroidContentURI(const std::string &path) {
		Parse(path);
	}

	bool Parse(const std::string &path);

	AndroidContentURI WithRootFilePath(const std::string &filePath);
	AndroidContentURI WithComponent(const std::string &filePath);
	AndroidContentURI WithExtraExtension(const std::string &extension);
	AndroidContentURI WithReplacedExtension(const std::string &oldExtension, const std::string &newExtension) const;
	AndroidContentURI WithReplacedExtension(const std::string &newExtension) const;

	bool CanNavigateUp() const;

	// Only goes downwards in hierarchies. No ".." will ever be generated.
	bool ComputePathTo(const AndroidContentURI &other, std::string &path) const;

	std::string GetFileExtension() const;
	std::string GetLastPart() const;

	bool NavigateUp();

	bool TreeContains(const AndroidContentURI &fileURI) {
		if (root.empty()) {
			return false;
		}
		return startsWith(fileURI.file, root);
	}

	std::string ToString() const;

	// Never store the output of this, only show it to the user.
	std::string ToVisualString() const {
		return file;
	}

	const std::string &FilePath() const {
		return file;
	}

	const std::string &RootPath() const {
		return root.empty() ? file : root;
	}

	bool IsTreeURI() const {
		return !root.empty();
	}
};
