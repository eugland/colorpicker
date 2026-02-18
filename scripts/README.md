# Scripts

## Build `colors.db` from `assets/colors/colors.json`

Use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-colors-db.ps1
```

What it does:

- Reads `app/src/main/assets/colors/colors.json`
- Recreates `app/src/main/assets/colors.db`
- Creates table `color_catalog_entries`
- Creates indexes expected by Room
- Inserts all colors into catalog `default`, language `en`

Expected output:

- `Generated ...\app\src\main\assets\colors.db`
- `Rows inserted: <n> (from colors.json entries: <n>)`

Notes:

- The script expects `sqlite3.exe` at:
  `C:\Users\eugen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe`
- If your `sqlite3.exe` is elsewhere, edit the `$sqlite` path in `scripts/build-colors-db.ps1`.
