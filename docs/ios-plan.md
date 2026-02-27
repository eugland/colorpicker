# iOS Architecture Design (Tailored to Your Color Picker App)

## 1. Product Overview & Core Features
Your app is a **color discovery and palette tool** with a camera-driven picker, photo-based sampling, manual sliders, palettes, and info/settings surfaces. The Android app exposes these major product areas:

- **Bottom‑tab navigation** with Palette, Camera, and Explore tabs, plus tool/detail routes for slider, color‑blind enhancer, palette details, color details, and info pages.【F:app/src/main/java/com/primortex/color/app/ColorApp.kt†L33-L227】【F:app/src/main/java/com/primortex/color/app/Routes.kt†L1-L55】
- **Camera and photo workflows**: Live camera picker, photo picker, and tools (color slider + color‑blind enhancer).【F:app/src/main/java/com/primortex/color/screens/PickColorScreen.kt†L1-L197】【F:app/src/main/java/com/primortex/color/screens/LiveCameraScreen.kt†L1-L200】【F:app/src/main/java/com/primortex/color/screens/PhotoPickScreen.kt†L1-L200】
- **Palette management** with creation, update, and persistence of palettes made of picked colors.【F:app/src/main/java/com/primortex/color/service/PaletteService.kt†L1-L200】
- **Color metadata**: Name lookup, conversions, similar colors, and color relationships (complements/triads/analogous).【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L1-L199】【F:app/src/main/java/com/primortex/color/service/ColorDetailsService.kt†L1-L79】
- **Recent picks & saved colors** with persistence and analytics events for user actions.【F:app/src/main/java/com/primortex/color/service/RecentPicksService.kt†L1-L200】【F:app/src/main/java/com/primortex/color/analytics/AnalyticsTracker.kt†L1-L51】
- **Settings** including theme mode, picker sensitivity, crosshair options, and language selection with cached locale override.【F:app/src/main/java/com/primortex/color/service/SettingsService.kt†L1-L205】【F:app/src/main/java/com/primortex/color/i18n/LocaleUtil.kt†L1-L28】【F:app/src/main/java/com/primortex/color/i18n/LanguageCache.kt†L1-L18】
- **Info/Legal content** (privacy, terms, usage, copyright) fetched remotely with local caching + TTL.【F:app/src/main/java/com/primortex/color/info/InfoContentService.kt†L1-L160】

---

## 2. Proposed iOS Architecture (High Level)

Use a **layered architecture** with SwiftUI + MVVM + Services:

```
App Layer (SwiftUI)
└─ Feature UI (Tabs, Screens, Components)
   └─ ViewModels (ObservableObject/State)
      └─ Domain/Use Cases
         └─ Services & Repositories
            └─ Persistence / Network / Device APIs
```

This mirrors your existing structure where UI screens call services directly, but iOS should centralize logic in ViewModels or use cases for clarity and testability.

---

## 3. iOS Module Breakdown (Mapped to Current Android Code)

### 3.1 App Entry & Dependency Bootstrapping
**Android:** Application starts analytics and service singletons.【F:app/src/main/java/com/primortex/color/ColorAppApplication.kt†L1-L18】  
**iOS Proposal:**

- `@main App` with an `AppDelegate` (or `UIApplicationDelegateAdaptor`) to initialize:
  - **Firebase Analytics** (same as Android tracker).【F:app/src/main/java/com/primortex/color/analytics/AnalyticsTracker.kt†L1-L51】
  - **Service container** (e.g., `AppServices` singleton or DI container).
- Initialize language + settings cache early (mirrors `SettingsService.init`).【F:app/src/main/java/com/primortex/color/service/SettingsService.kt†L1-L205】

### 3.2 Navigation
**Android:** `NavHost` with tab routes and tool/detail routes, plus bottom nav.【F:app/src/main/java/com/primortex/color/app/ColorApp.kt†L33-L227】  
**iOS Proposal:**

- **`TabView`** with `Palette`, `Camera`, `Explore`.
- **`NavigationStack`** inside each tab for:
  - Palette Detail, Color Detail, Info Details, Language Settings, Slider, Color Blind Enhancer, etc.【F:app/src/main/java/com/primortex/color/app/Routes.kt†L1-L55】

### 3.3 Data Models
**Android models:** `PickedColor`, `Palette` with IDs, tags, note, timestamps.【F:app/src/main/java/com/primortex/color/app/AppState.kt†L1-L25】  
**iOS Proposal:**

```swift
struct PickedColor: Codable, Identifiable {
    let id: String // UUID or argb string
    let argb: Int
    let name: String
}

struct Palette: Codable, Identifiable {
    let id: String
    var name: String
    var colors: [PickedColor]
    var tags: [String]
    var note: String
    var createdAt: Date
    var updatedAt: Date
}
```

---

## 4. Core Services (iOS Equivalents)

### 4.1 Color Dataset + Naming Service
**Android:** `ColorService` loads bundled `colors.json`, can refresh from remote, caches with TTL, and does nearest‑name lookup (LAB distance).【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L1-L199】  
**iOS Proposal:**

- `ColorNameService`
  - Load from bundled `colors.json`.
  - Fetch from remote URL (same base):  
    `https://eugland.github.io/color-picker-pages/colors/<lang>.json`.【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L80-L126】
  - Store cached payload in disk (e.g., `FileManager` or `UserDefaults`) with `fetchedAt`.
  - Maintain in-memory dataset for fast lookups.
  - Nearest color uses CIE Lab ΔE (port from `ColorService` + `ColorMath`).【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L137-L199】

### 4.2 Color Details Service
**Android:** `ColorDetailsService` computes RGB/HSV/HSL, luminance, similar colors, complements, triads, analogous.【F:app/src/main/java/com/primortex/color/service/ColorDetailsService.kt†L1-L79】  
**iOS Proposal:**

- `ColorDetailsService`
  - Inputs ARGB; returns name, hex, conversions, luminance, and related colors.
  - Similar colors derived by Euclidean RGB distance (as implemented).【F:app/src/main/java/com/primortex/color/service/ColorDetailsService.kt†L44-L77】
  - Used by Color Details UI and related/preview chips.

### 4.3 Palette Service
**Android:** `PaletteService` stores palettes in DataStore, seeds defaults, exposes `StateFlow` for UI updates, logs analytics.【F:app/src/main/java/com/primortex/color/service/PaletteService.kt†L1-L200】  
**iOS Proposal:**

- `PaletteStore` (ObservableObject)
  - Use `@Published var palettes`.
  - Persist using `Codable` to a JSON file (Documents/Library) or `UserDefaults`.
  - Seed initial palettes once (use stored `seeded` flag).
  - Analytics events on create/update.

### 4.4 Recent Picks Service
**Android:** `RecentPicksService` stores history + saved colors with limits and analytics events.【F:app/src/main/java/com/primortex/color/service/RecentPicksService.kt†L1-L200】  
**iOS Proposal:**

- `RecentPicksStore`
  - `@Published var history`, `@Published var saved`.
  - Persist in file or `UserDefaults`.
  - Use a max size (100).
  - Add/remove/toggle as in Android.

### 4.5 Settings + Language
**Android:** `SettingsService` stores theme mode, crosshair size/shape, picker sensitivity, and language; syncs locale via platform APIs.【F:app/src/main/java/com/primortex/color/service/SettingsService.kt†L1-L205】  
**iOS Proposal:**

- `SettingsStore` (ObservableObject)
  - `@AppStorage` or persisted file for:
    - Theme mode (system/light/dark).【F:app/src/main/java/com/primortex/color/service/SettingsService.kt†L34-L41】
    - Crosshair size/shape.
    - Picker sensitivity.
    - App language tag.
  - Apply `Locale` by providing environment `Locale` in SwiftUI when user selects language.

### 4.6 Info Content Service
**Android:** `InfoContentService` fetches legal/usage content from remote URLs with caching + TTL.【F:app/src/main/java/com/primortex/color/info/InfoContentService.kt†L1-L160】  
**iOS Proposal:**

- `InfoContentService`
  - Fetch JSON for the selected language:
    `https://eugland.github.io/color-picker-pages/<page>/<lang>.json`.【F:app/src/main/java/com/primortex/color/info/InfoContentService.kt†L52-L70】
  - Cache by page + language; update if stale.

### 4.7 Analytics
**Android:** `AnalyticsTracker` sends events to Firebase for color picks, saves, and palette creates.【F:app/src/main/java/com/primortex/color/analytics/AnalyticsTracker.kt†L1-L51】  
**iOS Proposal:**

- Mirror events in `AnalyticsTracker` using Firebase Analytics iOS SDK:
  - `color_pick`
  - `color_saved`
  - `palette_create`

---

## 5. Feature Screens (iOS UI Mapping)

### 5.1 Camera Tab
**Android:** Camera screen provides live picker, album, and tool shortcuts.【F:app/src/main/java/com/primortex/color/screens/PickColorScreen.kt†L1-L197】  
**iOS Proposal:**

- `CameraTabView`
  - **Live Camera Picker** (push to `LiveCameraView`).
  - **Photo Library Picker** (push to `PhotoPickView`).
  - **Color Slider** and **Color Blind Enhancer** tools.

### 5.2 Live Camera Picker
**Android:** Uses CameraX `PreviewView`, allows zoom, torch, freeze, recents sheet, and palette bar. It samples center color from camera frames and updates picks.【F:app/src/main/java/com/primortex/color/screens/LiveCameraScreen.kt†L1-L200】  
**iOS Proposal:**

- **AVFoundation** camera session with `AVCaptureVideoDataOutput`.
- Compute sampled pixel color at center (or crosshair).
- Overlay crosshair, zoom/torch, freeze button.
- Bottom sheet for recents + palette builder (use SwiftUI `sheet` or `bottomSheet`).

### 5.3 Photo Picker
**Android:** PhotoPick uses Coil + tap sampling of bitmap, records recents, and shows bottom sheet for recents and color details.【F:app/src/main/java/com/primortex/color/screens/PhotoPickScreen.kt†L1-L200】  
**iOS Proposal:**

- Use `PhotosUI` (`PHPickerViewController`) or SwiftUI `PhotosPicker`.
- Display chosen image in SwiftUI; map tap location to image pixel.
- Add to recents and allow palette creation.

### 5.4 Color Slider Tool
**Android:** Slider tool supports RGB/HSL/HSV/CMYK, copies values, and logs recents.【F:app/src/main/java/com/primortex/color/screens/ColorSliderScreen.kt†L1-L189】  
**iOS Proposal:**

- `ColorSliderView`
  - Segmented control for modes.
  - Sliders update ARGB in real time.
  - Clipboard copy and recents tracking.

### 5.5 Palette + Details
**Android:** Palette list and palette detail screens are navigated from Palette tab and from camera/photo workflows; palettes are created/updated via `PaletteService` and use `PaletteDetailScreen`.【F:app/src/main/java/com/primortex/color/app/ColorApp.kt†L129-L185】【F:app/src/main/java/com/primortex/color/service/PaletteService.kt†L1-L200】  
**iOS Proposal:**

- `PaletteListView` (list of palettes)
- `PaletteDetailView` with edit mode
- Integration with recents and color details

### 5.6 Color Details
**Android:** Color details route shows computed values and related colors via `ColorDetailsService`.【F:app/src/main/java/com/primortex/color/app/ColorApp.kt†L169-L206】【F:app/src/main/java/com/primortex/color/service/ColorDetailsService.kt†L1-L79】  
**iOS Proposal:**

- `ColorDetailsView`
  - Show name, hex, RGB/HSL/HSV, luminance, related colors
  - Use `ColorDetailsService`

### 5.7 Explore / Info / Language
**Android:** Explore tab routes to info pages (privacy/terms/usage/copyright) and language selection.【F:app/src/main/java/com/primortex/color/app/ColorApp.kt†L200-L227】  
**iOS Proposal:**

- `ExploreView` with links to:
  - `InfoDetailView`
  - `LanguageSelectionView`
- Content fetched via `InfoContentService` with caching.

---

## 6. Data Flow & State Management

### 6.1 State Ownership
- **Global stores** (`SettingsStore`, `RecentPicksStore`, `PaletteStore`, `ColorNameService`)
- **Screen ViewModels**:
  - `LiveCameraViewModel` (streaming sampling, torch/zoom)
  - `PhotoPickerViewModel`
  - `ColorSliderViewModel`

### 6.2 Data Flow Example (Live Camera)
1. Camera frames analyzed → sample ARGB color.
2. `ColorNameService` resolves nearest color name from dataset.【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L35-L56】
3. `RecentPicksStore.addPick` persists the sample and logs analytics.【F:app/src/main/java/com/primortex/color/service/RecentPicksService.kt†L103-L151】
4. UI renders active color, recents list, and palette builder.

---

## 7. Persistence Strategy (iOS)
Mirror Android’s DataStore usage:

| Data Type | Android | iOS Recommendation |
|---|---|---|
| Palettes | DataStore JSON | JSON file in App Support or `UserDefaults` |
| Recents/Saved | DataStore JSON | JSON file or `UserDefaults` |
| Settings | DataStore | `AppStorage` / `UserDefaults` |
| Cached content | SharedPreferences | File cache with TTL |

Settings and cached content are already structured in Android as simple string payloads, making JSON file storage in iOS straightforward.【F:app/src/main/java/com/primortex/color/service/PaletteService.kt†L1-L200】【F:app/src/main/java/com/primortex/color/service/RecentPicksService.kt†L1-L200】

---

## 8. i18n & Localization

- Android uses cached language tags, wraps base context, and syncs with platform locales.【F:app/src/main/java/com/primortex/color/i18n/LanguageCache.kt†L1-L18】【F:app/src/main/java/com/primortex/color/i18n/LocaleUtil.kt†L1-L28】
- iOS should:
  - Save chosen language tag.
  - Provide `Locale` override in SwiftUI:
    ```swift
    .environment(\.locale, Locale(identifier: settings.languageTag ?? "en"))
    ```
  - Keep system‑default behavior when language is nil.

---

## 9. Networking & Caching

**Endpoints in Android:**
- Colors dataset: `https://eugland.github.io/color-picker-pages/colors/<lang>.json`【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L80-L126】
- Info content: `https://eugland.github.io/color-picker-pages/<page>/<lang>.json`【F:app/src/main/java/com/primortex/color/info/InfoContentService.kt†L52-L70】

**iOS Implementation:**
- `URLSession` (async/await).
- Cache payload with TTL (7 days as in Android).【F:app/src/main/java/com/primortex/color/service/ColorService.kt†L126-L130】【F:app/src/main/java/com/primortex/color/info/InfoContentService.kt†L112-L119】

---

## 10. Security & Privacy Considerations

- Camera permission required for live picker (iOS: `NSCameraUsageDescription`).  
- Photo library permission for image sampling (iOS: `NSPhotoLibraryUsageDescription`).
- Minimal analytics payloads (color name/ARGB) similar to Android. Avoid storing raw images.

---

## 11. Suggested iOS Directory Structure

```
ColorPickerApp/
├─ App/
│  ├─ ColorPickerApp.swift
│  ├─ AppDelegate.swift
│  └─ AppServices.swift
├─ Features/
│  ├─ Camera/
│  ├─ Palette/
│  ├─ Explore/
│  ├─ ColorDetails/
│  └─ Tools/
├─ Services/
│  ├─ ColorNameService.swift
│  ├─ ColorDetailsService.swift
│  ├─ PaletteStore.swift
│  ├─ RecentPicksStore.swift
│  ├─ SettingsStore.swift
│  └─ InfoContentService.swift
├─ Models/
│  ├─ PickedColor.swift
│  └─ Palette.swift
├─ UI/
│  ├─ Components/
│  ├─ Theme/
└─ Resources/
   └─ colors.json
```
