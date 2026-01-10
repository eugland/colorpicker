Here is what I need you to do change the ColorServices so that it reads the source of truth from
assets/color.json which features:
[
{
"name": "Absolute Zero",
"hex": "#0048BA"
},
...
]
then for color name build up, and so on use file from this. Also you should try to fetch the latest
color value get color name from https://eugland.github.io/color-picker-pages/colors/en.json if
possible refer to InfoContentService
the last one path 'en' is locale (refer to how this app gets copyright, ), do not refresh cache if
got in last 7 days, the use this to calculate
the bucket to look up name with hex rgb values etc basically replace all features.

I want you to remove ColorNameIndex ColorNameLookup and ColorNameService these legacy service as
the outcome