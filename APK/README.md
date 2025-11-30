APK placement and fetch instructions

This folder is intended to hold the built APK file(s) for quick access.

Because this repository's CI builds the APK and uploads it as an artifact, use the script `fetch-artifact.sh` to download the latest `app-debug-apk` artifact and place the APK into this folder.

Requirements:
- `gh` (GitHub CLI) authenticated with access to this repository.

Quick use:
1. Install and authenticate `gh`:
   - https://cli.github.com/
   - `gh auth login`
2. Run the fetch script from the repository root:
   ```bash
   ./APK/fetch-artifact.sh
   ```

If the script cannot find an artifact, open the Actions tab and download the `app-debug-apk` artifact manually and put `app-debug.apk` into this folder.
