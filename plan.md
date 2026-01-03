# V2.0 Must haves

## Settings: Change focus Cross Hair

1. Can choose size
2. Can choose shape

## Settings: Language:

1. Setting language should only Support display language:
2. English, Mandarin Chinese, Hindi, Spanish, Arabic, French, Bengali, Portuguese, Russian, and
   Indonesian/Malay

## Setting: CopyRight Notice

1. declare Copyright used
2. declare used 3rd party apps

## Settings: Usage Guide

1. Write the entire usage guide

## Settings: Theme

1. Choose dark or light theme

## Palette: Edit palette

1. Clicking on palette open detailed palette view
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