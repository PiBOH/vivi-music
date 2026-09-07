# Screenshots

Real app screenshots live in **`images/screenshots/`**. They are shown
automatically on the **Screenshots** page (`screenshots.html`), sorted by file
name — you never edit that page to add one.

## Mandatory rules

1. **Format: `.webp`** — anything else (`.png`, `.jpg`, …) is ignored by the
   gallery. Please convert before uploading.
2. **Aspect ratio: 16:9, exactly** (that is the only ratio accepted).
3. Recommended resolutions (16:9):
   - **1920 × 1080** — best quality (full screen),
   - **1280 × 720** — fine for most shots,
   - **2560 × 1440** — only if the detail matters.
4. Keep each file **under 500 KB** — .webp at quality ~80 looks identical and
   keeps the page fast.

## How to add one

1. Take the screenshot, crop it to **16:9** and export it as **`.webp`**.
2. Name it with a short, descriptive name, e.g. `full-player.webp`,
   `library.webp`, `synced-lyrics.webp`, `device-sync.webp`.
3. Put the file in `images/screenshots/` and push it on `vivi-music-de`.
4. The gallery page picks it up automatically after the Pages deploy.

## Tips

- Filenames become the captions (`full-player.webp` → “Full Player”).
- Want to hide a shot? Just delete the file.
- The little previews on the home page are CSS mockups — they never need
  updating and are not related to these files.
