# V2.0 Must haves

## Settings: Language:

1. Setting language should only Support display language:
2. English, Mandarin Chinese, Hindi, Spanish, Arabic, French, Bengali, Portuguese, Russian, and
   Indonesian/Malay

## Palette: Edit palette

1. Clicking on palette on the palette screen should open up detailed palette view
2. The detail palettee view should offer details information of the palettee and the color it contains. 
3. It should also feature a couple of buttons: copy all hex, and delete (like we already have on the list in )
2. Palette support adding new color from recent service, or remove new colors
3. Should support editing tags, names of the palette

## palette: saved color

1. A new display area called saved color the user should be able to save colors to my color
2. The color detailed service should have 1 more button that saves color to my color
3. All color added to a palette should be saved to saved color

## Analytics: Collect analytics

1. Firebase analytics integrated into the app
2. Track key user flows: camera picking → save → palette actions → explore/search → export/copy.
3. Enable debugging/validation during development (DebugView / logcat) and ensure release builds
   still report events.
   S

# Future

## Setting Camera Quality

Camera sampling rate: offer low/ medium / High,

1. high = sample every 2 frame
2. medium = sample every 3 frames
3. Low = sample every 10 frames

| Preset | UI label             | `minIntervalMs` | `minRgbDistance` | Analysis resolution | Sample window |
|--------|----------------------|----------------:|-----------------:|---------------------|---------------|
| LOW    | Stable / battery     |          180 ms |               22 | 320×240             | 21×21 avg     |
| MEDIUM | Balanced             |          100 ms |               14 | 640×480             | 13×13 avg     |
| HIGH   | Precise / responsive |           60 ms |                8 | 1280×720            | 7×7 avg       |

## Language support search

1. Search should support multi lingual

Thank you, but I do not like the way it is, case in point the small text in the paddinged block are too small and tight to read let the user click on each such as copyright, privacy Statement and open new page to read them Also what saves more memory? string in resources or in assets load when needed? For use guide you may need 2 levels: click then show index pick color, save color etc.