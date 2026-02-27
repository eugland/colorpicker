# Color

Android color picking app.

## Supported Languages (Multilingual Strings)

The app currently includes localized `strings.xml` files for these locales:

- `values` (default)
- `values-ar` (Arabic)
- `values-bn` (Bengali)
- `values-cs` (Czech)
- `values-da` (Danish)
- `values-de` (German)
- `values-el` (Greek)
- `values-es` (Spanish)
- `values-fi` (Finnish)
- `values-fil` (Filipino)
- `values-fr` (French)
- `values-he` (Hebrew)
- `values-hi` (Hindi)
- `values-hu` (Hungarian)
- `values-id` (Indonesian)
- `values-it` (Italian)
- `values-ja` (Japanese)
- `values-ko` (Korean)
- `values-ms` (Malay)
- `values-nb` (Norwegian Bokmal)
- `values-nl` (Dutch)
- `values-pl` (Polish)
- `values-pt` (Portuguese)
- `values-ro` (Romanian)
- `values-ru` (Russian)
- `values-sv` (Swedish)
- `values-th` (Thai)
- `values-tr` (Turkish)
- `values-uk` (Ukrainian)
- `values-ur` (Urdu)
- `values-vi` (Vietnamese)
- `values-zh` (Chinese)
- `values-zh-rCN` (Chinese, China)
- `values-zh-rTW` (Chinese, Taiwan)

## Supported Color Catalog Maps

Color catalog JSON maps currently available:

- `res/raw/colors.json`: default catalog map (used as fallback for most locales)
- `res/raw-ja/colors.json`: Japanese catalog map

## Project Docs

- Script usage: [`scripts-readme.md`](scripts-readme.md)

## Reliability Test Plan

This plan focuses on failures that would break core app trust: wrong color mapping, broken locale catalogs, and silent data parsing regressions.

### 1) Unit Tests (Fast Gate)

Run on every change:

- `./gradlew :app:testDebugUnitTest`

Critical coverage:

- `ColorService` input validation:
  - invalid hex values are dropped
  - blank names are ignored
- normalization + lookup behavior:
  - case-insensitive/trimmed name lookup
  - `setColors()` replaces catalog and invalidates old lookup state
- search correctness:
  - `startsWith` matches rank before `contains`
  - blank queries return no results
  - result `limit` is respected
- nearest-color mapping:
  - deterministic nearest-name result for close ARGB inputs

Pass criteria:

- all unit tests pass
- no flaky/non-deterministic assertions

### 2) Instrumented Tests (Android Resource + Locale Gate)

Run before release and after locale/catalog changes:

- `./gradlew :app:connectedDebugAndroidTest`

Critical coverage:

- `ColorCatalogImportService` default catalog load is non-empty
- locale-tag loading remains functional for representative locales:
  - `ja`, `fr`, `es`, `it`, `zh`, `zh-CN`, `zh-TW`
- unknown locale tag still yields a usable catalog (fallback behavior)

Pass criteria:

- all instrumented tests pass on at least one CI/device API level
- no locale returns an empty catalog unless explicitly intentional

### 3) Resource Integrity Checks (Catalog Quality Gate)

Run after editing any `res/raw*/colors.json`:

- verify JSON parseability for all locale files
- verify UTF-8 without BOM
- verify expected entry counts for locale strategy:
  - full locales match base catalog size
  - copied/specialized locales match their source strategy by design
- verify duplicate policy:
  - names and hex duplicates are either intentionally allowed or explicitly resolved

### 4) Suggested CI Order

1. `:app:testDebugUnitTest`
2. `:app:assembleDebug`
3. `:app:connectedDebugAndroidTest` (device/emulator lane)

### 5) Reliability Bar for Merge

- no failing tests
- no new untested behavior in color parsing/search/locale loading
- locale catalog edits include at least one validation run in CI logs
