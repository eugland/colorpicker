# App Critique TODO

Updated: 2026-02-24

## Critical
- [ ] Fix potential crash on Android versions below API 33 in color blind enhancer.
  - Issue: `RuntimeShader` is instantiated without guarding for unsupported API levels.
  - Files:
    - `app/src/main/java/com/primortex/color/features/colorblind/ColorBlindEnhancerScreen.kt` (around lines 255, 280)
  - Done when:
    - Screen no longer crashes below API 33.
    - Feature is either hidden/disabled below API 33 or has a working fallback path.

## High
- [ ] Remove debug signing from release builds.
  - Issue: Release build currently uses debug signing config.
  - File:
    - `app/build.gradle.kts` (line 35)
  - Done when:
    - Release uses a proper release signing config.
    - CI/release process can produce correctly signed artifacts.

- [ ] Make language/catalog refresh deterministic after language changes.
  - Issue: Catalog refresh is async fire-and-forget, so stale colors can be read immediately after switching.
  - File:
    - `app/src/main/java/com/primortex/color/service/ColorCatalogService.kt` (around lines 48, 60)
  - Done when:
    - UI paths that depend on catalog data wait for/observe completion.
    - Post-language-switch reads are consistent.

## Medium
- [ ] Replace silent camera binding failure catches with observability and handling.
  - Issue: `catch (_: Exception) {}` hides camera startup failures.
  - Files:
    - `app/src/main/java/com/primortex/color/features/livecamera/LiveCameraScreen.kt` (around line 502)
    - `app/src/main/java/com/primortex/color/features/colorblind/ColorBlindEnhancerScreen.kt` (around line 1052)
  - Done when:
    - Failures are logged with context and surfaced to UI where appropriate.
    - Recovery path exists (retry/back/permission guidance).

- [ ] Make unit test suite green and stable.
  - Current state: `42 tests completed, 4 failed` on `:app:testDebugUnitTest`.
  - Failures seen:
    - `ColorCatalogJsonIntegrityTest.everyValuesLocale_hasMatchingRawLocaleCatalog`
    - `ColorCatalogServicesUnitTest.coordinator_appliesLoadedCatalog_toColorService`
    - `RecentPicksServiceUnitTest.addPick_deduplicatesAndPlacesMostRecentFirst`
    - `RecentPicksServiceUnitTest.toggleSaved_addsAndRemovesSavedColor`
  - Files:
    - `app/src/test/java/com/primortex/color/service/ColorCatalogJsonIntegrityTest.kt`
    - `app/src/test/java/com/primortex/color/service/ColorCatalogServicesUnitTest.kt`
    - `app/src/test/java/com/primortex/color/service/RecentPicksServiceUnitTest.kt`
    - `app/src/main/java/com/primortex/color/service/ColorCatalogService.kt`
    - `app/src/main/java/com/primortex/color/service/RecentPicksService.kt`
  - Done when:
    - `:app:testDebugUnitTest` passes consistently.
    - Tests no longer rely on timing sleeps/polling for async DB/coordinator behavior.

- [ ] Define explicit backup/data-extraction policy.
  - Issue: `allowBackup=true` with sample/default backup rule files.
  - Files:
    - `app/src/main/AndroidManifest.xml` (application backup flags)
    - `app/src/main/res/xml/backup_rules.xml`
    - `app/src/main/res/xml/data_extraction_rules.xml`
  - Done when:
    - Include/exclude rules match product privacy/data-retention intent.
    - Behavior is validated on supported Android versions.

## Product Decisions Needed
- [ ] Decide API support policy for color blind enhancer:
  - Option A: API 33+ only (hide/disable on lower versions)
  - Option B: Provide fallback implementation for lower versions

- [ ] Decide backup scope:
  - Include/exclude palettes, recent picks, saved picks, and settings explicitly.
