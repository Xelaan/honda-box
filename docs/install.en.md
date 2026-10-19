# Honda Box 0.8.35 — FYT / DUDU installation

Supported configuration: Honda Accord VIII / Spirior 2009–2013, FYT CAN profile **6291908**, BSJ V704. Requires **Android 7.0 / API 24+**. This is an Android companion for the original Honda audio/climate system, not a head-unit or CAN-adapter firmware update.


If an old **Probe** (`dev.alex.fyt.probe`) is still installed, save any logs you need, restore any stock-overlay permission it changed, then stop its service and remove that old app. It is a separate installation and can otherwise keep showing old settings and duplicate overlays. Updating an existing `com.crewbeat.hondabox` installation does not require uninstalling it.

## Files

| File | Purpose |
| --- | --- |
| `Honda-Box-0.8.35.apk` | Honda Box Service, UI and overlays |
| `Honda-Box-Launcher-1.3.apk` | Honda Box selector icon |
| `Honda-Radio-Launcher-1.5.apk` | Radio icon |
| `Honda-CD-Launcher-1.5.apk` | CD icon |
| `Honda-USB-Launcher-1.6.apk` | Honda USB icon: one shared USB/iPod player |
| `shizuku-v13.6.0.r1086.2650830c-release.apk` | Optional official Shizuku for stock overlay suppression |
| `INSTALL-ADB.py` | Optional installation of the five Honda APKs over ADB |
| `stock_overlay_adb.py` | Optional ADB stock-overlay helper with original-mode backup/restore |
| `SHA256SUMS.txt`, `BUILD-VERIFICATION.json` | Checksums, package IDs, versions, certificates and file verification |

Russian/English guides, release notes and licenses are included. No separate iPod APK is needed. Test APKs, OEM firmware, personal captures and signing keys are excluded.

## Migrating from older local builds

The new package IDs start with **`com.crewbeat.hondabox`**. They install separately from `dev.alex.fyt.probe` and its old launchers. Old preferences, permissions and Shizuku authorization do not transfer automatically.

1. If the old Honda Box suppressed stock CANBUS overlays, restore its saved original permission before uninstalling it or clearing its data.
2. Stop the old service and disable its app/DUDU autostart to avoid duplicate overlays.
3. Install/configure this pack. Once verified, optionally remove the old Honda apps/launchers in Android settings; doing so deletes their old data.

Installing over the same new package with our release certificate keeps preferences. Self-built development APKs use a different key and cannot update over this release. For a signature conflict, note your preferences, uninstall only the conflicting Honda app and install again; that app's data is lost. Do not remove system `com.syu.ms` / `com.syu.canbus`.

## Install on the head unit

1. Extract the Setup Pack ZIP on your computer, copy its APKs to a USB drive and open them in Android's file manager. Do not use the firmware-update mechanism.
2. Install **Honda-Box-0.8.35.apk** first. Allow installation from the file manager if requested.
3. Install **Honda-Box-Launcher-1.3.apk** and the desired Radio/CD/Honda USB launchers. All four may be installed. USB and iPod share one launcher.
4. Open **Honda Box**. **Honda Box Service** starts headlessly; the separate Honda Box icon opens the UI. FYT connection starts automatically. Confirm the unit already uses profile 6291908; Honda Box does not change it.
5. Settings → **Allow display over other apps**: grant overlay permission. On Android 13+, allow notifications if prompted.
6. Allow background operation/battery-optimization exemption. Add **Honda Box Service** to DUDU's autostart and sleep retention/Keep alive list. Refresh/restart the launcher if the new icons have not appeared.
7. Choose language, appearance and accent. Large climate and compact climate are enabled **independently**, with behavior notes and preview buttons. Set your location for sunrise/sunset mode; location permission is only needed to obtain your current location.
8. Verify MODE switching, volume and climate in the car. If firmware does not support outgoing source/Previous/Next commands, use the car controls. Recording is off by default; enable/reset/export it from Debug when needed.

iPod detection uses observed adapter markers and can take a few seconds after connection. Data not transmitted by the adapter cannot be recovered. This release is checked in an emulator; firmware-specific behavior and DUDU sleep retention require a head-unit check.

## Boot and sleep setup

Open **Honda Box → Settings → Background operation** for offline **FYT setup**, **DUDU setup** and **Check & help** guides. On stock FYT, add **Honda Box Service** in **Settings → User → Application Settings** for boot and deep-sleep wake. On DUDU OS, use the available startup and sleep Keep alive lists; DUDU Launcher on stock FYT may still require the FYT settings route. Menu availability varies by firmware.

Boot/ignition startup and automatic service recovery default to on. Recovery is a best-effort periodic check while awake, not a guarantee against vendor force-stop. **Stop** deliberately pauses automatic restarts; open the **Honda Box UI** to resume. Verify a full Android reboot and an ACC OFF/deep-sleep/ACC ON cycle separately. See the included [background operation guide](background-operation.md).

## Optional: hide stock overlays with Shizuku

Shizuku is **not required** for ordinary FYT reading or Honda Box overlays. It is only needed to manage the stock `com.syu.canbus` overlay permission.

1. Install the included official Shizuku if needed. Keep an existing compatible/newer installation rather than downgrading it.
2. Open Shizuku's Start by connecting to a computer → View command. Connect the head unit over ADB and run the **command supplied by your installed Shizuku version**. Wireless debugging can be used where supported. [Official setup guide](https://shizuku.rikka.app/guide/setup/).
3. Honda Box → Settings → Stock CANBUS windows → Connect / refresh Shizuku; authorize Honda Box.
4. Enable Hide stock CANBUS popups. Honda Box saves the original AppOps mode and verifies application; disabling restores that saved mode.
5. Restore before uninstalling Honda Box or clearing its data. After reboot, Shizuku may need restarting to change the mode.

This covers **all** `com.syu.canbus` overlays, including menu, Car Vol and climate. It does not control the separate `com.syu.ms` system volume bar or disable FYT/CANBUS services. Firmware using a different window type may not be suppressed.

Computer alternative: `python3 stock_overlay_adb.py status --serial SERIAL`; `hide`/`restore` require `--state original-canbus-overlay.json`. Keep this backup for restoration on the same SERIAL. This helper is unnecessary when using the Shizuku setting.

## Optional: install over ADB

Use Python 3 and current [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools). Enable ADB on the head unit, authorize the computer, and use `adb devices -l` to find its serial/IP:port.

From the extracted pack:

```sh
python3 INSTALL-ADB.py --serial SERIAL
```

The helper verifies SHA-256 and installs the five Honda APKs without incremental install. It does not install Shizuku automatically, remove apps or modify firmware/CAN profiles. Then grant the permissions and configure autostart above. Use `--adb /path/to/adb` for a custom executable.

## Verify downloads

After extraction: `sha256sum -c SHA256SUMS.txt` (Linux) or `shasum -a 256 -c SHA256SUMS.txt` (macOS). Windows: `Get-FileHash .\Honda-Box-0.8.35.apk -Algorithm SHA256`, then compare with SHA256SUMS.txt. The release also supplies a `.zip.sha256` file for the archive.

Source/releases: [Xelaan/honda-box](https://github.com/Xelaan/honda-box). Original code is MIT; third-party licenses and Shizuku provenance are included.
