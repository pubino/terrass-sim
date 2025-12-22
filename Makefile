# Territory Assignment Makefile
#
# Targets:
#   make          - Build JAR file (default)
#   make jar      - Build JAR file
#   make app      - Build macOS .app bundle (unsigned)
#   make sign     - Sign and notarize for distribution
#   make run      - Build and run the JAR
#   make run-app  - Build and run the .app bundle
#   make clean    - Remove build artifacts
#   make distclean - Remove all generated files including dist
#
# Requirements:
#   - Java 14+ (for jpackage)
#   - For signing: Apple Developer certificate and notarization credentials

SHELL := /bin/bash

# Check for Java availability (actually test javac, not just if it exists)
JAVA_WORKS := $(shell javac -version 2>&1 | grep -q "javac" && echo yes)

.PHONY: check-java
check-java:
ifneq ($(JAVA_WORKS),yes)
	@echo "Error: Java compiler (javac) not found."
ifeq ($(shell uname),Darwin)
	@echo "Install Java on macOS with brew install openjdk and add it to your PATH."
else
	@echo "Please install a Java Development Kit (JDK)."
endif
	@exit 1
endif

# Directories
PROJECT_DIR := $(shell pwd)
BUILD_DIR := $(PROJECT_DIR)/build
DIST_DIR := $(PROJECT_DIR)/dist
SCRIPTS_DIR := $(PROJECT_DIR)/scripts

# Java settings
JAVAC := javac
JAVA := java
JAR := jar
JPACKAGE := jpackage
JAVAC_FLAGS := -Xlint:-deprecation -Xlint:-removal

# Output files
JAR_FILE := $(DIST_DIR)/TerritoryAssignment.jar
APP_BUNDLE := $(DIST_DIR)/TerritoryAssignment.app
MAIN_CLASS := terrass.TerrAss

# Source files
SOURCES := \
	terrass/TerrAss.java \
	terrass/POINT.java \
	terrass/Sfc.java \
	myutil/AppletFrame.java \
	myutil/Comparable.java \
	myutil/GL.java \
	myutil/SL.java \
	myutil/Format.java \
	myutil/FileIO.java \
	myutil/GridPanel.java \
	myutil/Memory.java \
	myutil/Sort.java \
	myutil/Search.java

# App bundle settings
APP_NAME := TerritoryAssignment
DISPLAY_NAME := Territory Assignment
BUNDLE_ID := edu.princeton.vanderbei.terrass-sim
VERSION := 1.0.0

# Default target
.PHONY: all
all: jar

# Compile Java sources and create JAR
.PHONY: jar
jar: check-java $(JAR_FILE)

$(JAR_FILE): $(SOURCES)
	@echo "=== Compiling Java sources ==="
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)
	$(JAVAC) -d $(BUILD_DIR) -sourcepath . $(JAVAC_FLAGS) $(SOURCES)
	@echo "=== Creating JAR file ==="
	cd $(BUILD_DIR) && $(JAR) cfe $(JAR_FILE) $(MAIN_CLASS) terrass/ myutil/
	@echo "Build complete: $(JAR_FILE)"
	@read -p "Would you like to run the application? [y/N] " answer; \
	if [ "$$answer" = "y" ] || [ "$$answer" = "Y" ]; then \
		if command -v java >/dev/null 2>&1; then \
			echo "=== Running $(JAR_FILE) ==="; \
			java -jar $(JAR_FILE); \
		else \
			echo "Error: Java not found in PATH"; \
			echo ""; \
			echo "To install Java via Homebrew:"; \
			echo "  brew install openjdk"; \
			echo ""; \
			echo "Or install a specific version:"; \
			echo "  brew install openjdk@21"; \
			echo ""; \
			echo "After installation, you may need to add it to your PATH:"; \
			echo "  echo 'export PATH=\"/opt/homebrew/opt/openjdk/bin:\$$PATH\"' >> ~/.zshrc"; \
		fi \
	fi

# Build macOS .app bundle (unsigned)
.PHONY: app
app: $(APP_BUNDLE)

$(APP_BUNDLE): $(JAR_FILE)
	@echo "=== Creating macOS App Bundle ==="
	@rm -rf $(APP_BUNDLE)
	$(JPACKAGE) \
		--type app-image \
		--name "$(APP_NAME)" \
		--input $(DIST_DIR) \
		--main-jar TerritoryAssignment.jar \
		--main-class $(MAIN_CLASS) \
		--dest $(DIST_DIR) \
		--app-version $(VERSION) \
		--vendor "Vanderbei" \
		--mac-package-identifier $(BUNDLE_ID) \
		--mac-package-name "$(DISPLAY_NAME)" \
		--java-options "-Xmx512m"
	@echo "App bundle created: $(APP_BUNDLE)"
	@read -p "Would you like to run the application? [y/N] " answer; \
	if [ "$$answer" = "y" ] || [ "$$answer" = "Y" ]; then \
		echo "=== Running $(APP_BUNDLE) ==="; \
		open $(APP_BUNDLE); \
	fi

# Sign and notarize for distribution
.PHONY: sign
sign: app
	@echo "=== Signing and Notarizing ==="
	@if [ -x "$(SCRIPTS_DIR)/sign-and-notarize.sh" ]; then \
		$(SCRIPTS_DIR)/sign-and-notarize.sh; \
	else \
		echo "Error: sign-and-notarize.sh not found or not executable"; \
		exit 1; \
	fi

# Run the JAR
.PHONY: run
run: jar
	@echo "=== Running Territory Assignment ==="
	$(JAVA) -jar $(JAR_FILE)

# Run the app bundle
.PHONY: run-app
run-app: app
	@echo "=== Running Territory Assignment App ==="
	open $(APP_BUNDLE)

# Clean build artifacts
.PHONY: clean
clean:
	@echo "=== Cleaning build artifacts ==="
	rm -rf $(BUILD_DIR)
	rm -f $(JAR_FILE)
	@echo "Clean complete"

# Clean everything including dist
.PHONY: distclean
distclean: clean
	@echo "=== Removing all generated files ==="
	rm -rf $(DIST_DIR)
	@echo "Distclean complete"

# Help
.PHONY: help
help:
	@echo "Territory Assignment Makefile"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  all        - Build JAR file (default)"
	@echo "  jar        - Build JAR file"
	@echo "  app        - Build macOS .app bundle (unsigned)"
	@echo "  sign       - Sign and notarize for distribution"
	@echo "  run        - Build and run the JAR"
	@echo "  run-app    - Build and run the .app bundle"
	@echo "  clean      - Remove build artifacts"
	@echo "  distclean  - Remove all generated files including dist"
	@echo "  help       - Show this help message"
