
# V 1.0: Setting Design

## Pick Quality (Low / Medium / High)


# V2.0 Setting Desgin
##  Setting Camera Quality
Camera sampling rate: offer low/ medium / High, 
   1. high = sample every 2 frame
   2. medium = sample every 3 frames
   3. Low = sample every 10 frames

| Preset | UI label             | `minIntervalMs` | `minRgbDistance` | Analysis resolution | Sample window |
| ------ | -------------------- | --------------: | ---------------: | ------------------- | ------------- |
| LOW    | Stable / battery     |          180 ms |               22 | 320×240             | 21×21 avg     |
| MEDIUM | Balanced             |          100 ms |               14 | 640×480             | 13×13 avg     |
| HIGH   | Precise / responsive |           60 ms |                8 | 1280×720            | 7×7 avg       |

## Change focus Cross Hair
1. Choose size
2. Choose shape
2. Language: 
3. 