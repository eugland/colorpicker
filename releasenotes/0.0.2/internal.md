# 📦 Release Notes — What’s New

## ✨ New Features

### 🎨 Color-Blind Enhancer (NEW)

* Added a **real-time camera-based color-blind enhancement mode**
* Supports **Protanopia, Deuteranopia, and Tritanopia**
* Adjustable **intensity**, optional **edge enhancement**, and **camera flip**
* Uses **runtime shaders (Android 13+)** for high-performance live rendering
* Accessible directly from the Camera tab

### 📜 Terms of Service Page

* Introduced a dedicated **Terms of Service** screen
* Fully localized and integrated with existing Info pages
* Covers usage, limitations, and liability

---

## 🧭 Navigation & Architecture Improvements

### 🧩 New Navigation Abstraction

* Introduced `ColorNavigator` interface and `NavColorNavigator` implementation
* Centralizes navigation logic and removes direct `NavController` coupling from UI
* Improves maintainability and testability

### 🧭 Improved Routing

* Added new routes for:

    * Color-Blind Enhancer tool
    * Terms of Service page
* Refined tab navigation behavior for cleaner back-stack handling

---

## 🧪 Quality & Testing

### ✅ New Instrumentation Test Suite

* Added **`ColorMathTest`** with comprehensive coverage for:

    * ARGB / RGB / HSL / HSV / LAB conversions
    * Delta-E color distance
    * Hue shifting, luminance, normalization, and hex parsing
* Ensures correctness and guards against regressions in color math

---

## 🎨 Visual & Asset Updates

### 🖼️ Updated Branding Assets

* Added a new **Play Store feature graphic banner**
* Updated app icon for Play Store
* Moved SVG assets to `assets/` for cleaner resource organization
* Simplified logo visuals for better small-size readability

---

## 🛠️ UI & UX Enhancements

* Integrated a **global Snackbar service** using CompositionLocals
* Improved screen-level feedback consistency
* Added Material 3 SnackbarHost support
* Minor layout and animation refinements across navigation transitions

---

## 🔧 Dependency & Build Changes

* Added `androidx.browser:browser` dependency
* Improved `.gitignore` rules to exclude IDE and release artifacts
* Cleaned up project structure and removed misplaced resource files

---

## 🧹 Internal Improvements

* Migrated locale handling to `LocalConfiguration`
* Improved Info content loading and structure
* Better separation of concerns between UI, navigation, and content services

---

## 🚀 Summary

This release introduces a **major new accessibility feature (Color-Blind Enhancer)**, strengthens *
*navigation architecture**, improves **test coverage**, and refreshes **visual assets** in
preparation for Play Store distribution. Overall stability, maintainability, and user experience are
significantly improved.


