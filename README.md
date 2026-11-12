# Household Help Register

Attendance, payments, planner and journal for household help. One HTML file, installable as an app.

## Use it online / install as an app (GitHub Pages)
After enabling Pages (Settings > Pages > Deploy from a branch > `main` / root), open
`https://YOUR-USERNAME.github.io/YOUR-REPO/` in Chrome or Edge and choose **Install app**.
Works on Windows and Android. Data stays on your own device (browser storage); it is not stored in this repo.

## Get the Windows .exe and Android .apk
Actions tab > **Build Windows installer** and **Build Android APK** > Run workflow (each separately). When it finishes, download
`windows-installer` and `android-apk` from the run's Artifacts. Pushing a tag such as `v1.0` also
publishes both files under Releases.

## Updating
Edit `index.html` in the repo root and commit. In `sw.js` change `VERSION` (`v1` to `v2`) so installed copies refresh.
The Windows and Android builds copy the root files automatically.
