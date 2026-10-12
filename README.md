# Honda Box

An Android companion for Honda's original audio and climate system on FYT head units with a BSJ CAN adapter. Native Java, built without Gradle or a cloud service. [Русский](README.ru.md).

Developed against **Honda Accord VIII / Spirior 2009–2013**, FYT profile **6291908**, and BSJ V704. Compatibility with other profiles is not assumed. Requires **Android 7.0 / API 24+**. The main package is `com.crewbeat.hondabox`.

## Features

*Actual app screens captured in a 1280×480 emulator with synthetic data, not live vehicle readings. Open an image for full size. Screenshots use English; Russian is also supported.*

### Source selector

Radio, CD and a shared USB/iPod screen for the original Honda media ports. The current source is highlighted; an active iPod changes the card artwork and device label.

**Source selector / iPod**

| Light theme | Dark theme |
| --- | --- |
| ![Source selector / iPod — light](docs/images/features/selector-day.png) | ![Source selector / iPod — dark](docs/images/features/selector-night.png) |

### Radio: FM and AM

FM1/FM2 show MHz, six saved presets and the selected station, with stereo and playback status. AM1 adds MW to the heading; AM2 adds LW, and both use kHz.

**FM2 with selected preset**

| Light theme | Dark theme |
| --- | --- |
| ![FM2 with selected preset — light](docs/images/features/radio-day.png) | ![FM2 with selected preset — dark](docs/images/features/radio-night.png) |

**AM1 / MW**

| Light theme | Dark theme |
| --- | --- |
| ![AM1 / MW — light](docs/images/features/radio-am-day.png) | ![AM1 / MW — dark](docs/images/features/radio-am-night.png) |

### CD changer

Six discs with the current disc highlighted, the selected TITLE field, combined OEM text, track number and elapsed time. Previous/Next and source selection depend on the head-unit firmware; the in-player guide explains the car controls.

**CD with disc 2 selected**

| Light theme | Dark theme |
| --- | --- |
| ![CD with disc 2 selected — light](docs/images/features/cd-day.png) | ![CD with disc 2 selected — dark](docs/images/features/cd-night.png) |

### USB and iPod

One player adapts its artwork and content to the detected device. USB displays track/time information; iPod shows Playing without an unsupported timer or track counter. TITLE stays as the combined field supplied by the car, without inventing separate artist/album fields.

**USB player**

| Light theme | Dark theme |
| --- | --- |
| ![USB player — light](docs/images/features/usb-day.png) | ![USB player — dark](docs/images/features/usb-night.png) |

**iPod player**

| Light theme | Dark theme |
| --- | --- |
| ![iPod player — light](docs/images/features/ipod-day.png) | ![iPod player — dark](docs/images/features/ipod-night.png) |

### Full climate panel

A full-width bottom popup appears on climate changes. It displays both temperatures, airflow direction, fan level, power, AUTO, A/C and DUAL, with day/night cabin artwork. Its settings icon opens Honda Box settings. Unsupported recirculation and rear-defrost states are omitted.

**Full climate popup**

| Light theme | Dark theme |
| --- | --- |
| ![Full climate popup — light](docs/images/features/climate-full-day.png) | ![Full climate popup — dark](docs/images/features/climate-full-night.png) |

### Compact climate and folding

An independently enabled strip stays centered at the bottom while the climate is on. Optional side buttons open the full panel or minimize the strip. Folding leaves a persistent translucent handle; tap it to reopen. Climate changes can temporarily unfold it, and manual-opening/button autohide behavior is configurable.

**Compact climate with side controls**

| Light theme | Dark theme |
| --- | --- |
| ![Compact climate with side controls — light](docs/images/features/climate-compact-day.png) | ![Compact climate with side controls — dark](docs/images/features/climate-compact-night.png) |

**Persistent folded handle**

| Light theme | Dark theme |
| --- | --- |
| ![Persistent folded handle — light](docs/images/features/climate-folded-day.png) | ![Persistent folded handle — dark](docs/images/features/climate-folded-night.png) |

### Volume overlay

A translucent top indicator shows OEM volume on a 0–40 scale, the current source and its current displayed title/frequency. It appears on volume changes and can optionally remain visible.

**OEM volume / current title**

| Light theme | Dark theme |
| --- | --- |
| ![OEM volume / current title — light](docs/images/features/volume-overlay-day.png) | ![OEM volume / current title — dark](docs/images/features/volume-overlay-night.png) |

### Track and station announcements

Top cards announce detected track/station changes while a player is not foreground. Saved stations include their preset number before the frequency. Tap an announcement to open the current source screen. Detection uses the counters/text actually received from the adapter.

**New track announcement**

| Light theme | Dark theme |
| --- | --- |
| ![New track announcement — light](docs/images/features/track-overlay-day.png) | ![New track announcement — dark](docs/images/features/track-overlay-night.png) |

**Saved station announcement**

| Light theme | Dark theme |
| --- | --- |
| ![Saved station announcement — light](docs/images/features/station-overlay-day.png) | ![Saved station announcement — dark](docs/images/features/station-overlay-night.png) |

### Source and audio power status

A source overlay appears on changes and service start/wake. Verified OEM audio power status is shown as a separate OFF chip on the selector; the source overlay can remain visible while audio is off. AUX can be renamed in settings, for example to Android.

**Selector / audio OFF**

| Light theme | Dark theme |
| --- | --- |
| ![Selector / audio OFF — light](docs/images/features/selector-off-day.png) | ![Selector / audio OFF — dark](docs/images/features/selector-off-night.png) |

**Persistent source OFF overlay**

| Light theme | Dark theme |
| --- | --- |
| ![Persistent source OFF overlay — light](docs/images/features/source-off-day.png) | ![Persistent source OFF overlay — dark](docs/images/features/source-off-night.png) |

### OEM menu and player help

A centered menu combines the received OEM item with a source-specific reconstructed list and highlights the active row. Settings are changed with the car controls. Player help explains source selection, tracks/folders, TITLE and menu navigation.

**CD settings / SVC**

| Light theme | Dark theme |
| --- | --- |
| ![CD settings / SVC — light](docs/images/features/oem-menu-day.png) | ![CD settings / SVC — dark](docs/images/features/oem-menu-night.png) |

**USB control guide**

| Light theme | Dark theme |
| --- | --- |
| ![USB control guide — light](docs/images/features/player-help-day.png) | ![USB control guide — dark](docs/images/features/player-help-night.png) |

### Appearance and independent panel settings

Day, night, Android system or automatic sunrise/sunset theme; accent presets/custom color; English, Russian or system language. Large and compact climate panels have separate switches, behavior notes and preview buttons. Other settings cover overlays, headless boot/wake reconnection and optional stock-overlay suppression through Shizuku.

**Language / appearance settings**

| Light theme | Dark theme |
| --- | --- |
| ![Language / appearance settings — light](docs/images/features/settings-day.png) | ![Language / appearance settings — dark](docs/images/features/settings-night.png) |

**Independent climate settings**

| Light theme | Dark theme |
| --- | --- |
| ![Independent climate settings — light](docs/images/features/climate-settings-day.png) | ![Independent climate settings — dark](docs/images/features/climate-settings-night.png) |

### Diagnostics

Debug separates FYT callbacks from decoded BSJ fields, retains the latest values and makes unconfirmed channels explicit. Recording is opt-in, with reset/export and packet analysis. Experimental outgoing commands are triggered manually; screenshots do not validate their support on a car.

**Radio callback / BSJ diagnostics**

| Light theme | Dark theme |
| --- | --- |
| ![Radio callback / BSJ diagnostics — light](docs/images/features/debug-radio-day.png) | ![Radio callback / BSJ diagnostics — dark](docs/images/features/debug-radio-night.png) |

## Data and limitations

The service reads FYT Binder callbacks and adapter packets exposed by MainServer. These **BSJ packets are not a confirmed raw Honda CAN-ID/DLC stream**. There is no direct UART connection in this app.

USB/iPod detection includes observed adapter markers and inference for this profile; it is not a universal hardware flag. TITLE text is kept as the combined field actually supplied by the car. Missing or corrupted characters cannot be recovered. OEM audio power on/off is supported. Climate controls are displayed rather than sent to the car; unsupported recirculation/rear-defrost indicators are omitted. RDS settings are displayed, but station-name/RadioText transmission is not confirmed for this adapter. Car Info remains experimental: trip values are not presented as decoded when their meaning is unverified. Audio-command support depends on firmware; use the car controls where required.

## Install and start

Download the complete **Setup Pack ZIP** from [GitHub Releases](https://github.com/Xelaan/honda-box/releases/latest). It contains the main APK, four companion launchers, Russian/English installation guides and SHA-256 checksums. Optional Shizuku is included with its source/license notice. This source tree does not include release APKs or a signing key.

1. Install the main `Honda-Box-<version>.apk`.
2. Install **Honda Box**, **Radio**, **CD** and **USB** launcher APKs as needed. One USB launcher covers both USB and iPod.
3. Open Honda Box, allow **display over other apps**, and configure the large/compact climate panels independently.
4. Confirm the head unit uses profile 6291908. Allow foreground/background operation; add Honda Box to DUDU's keep-alive list if needed.
5. Optional: install/start [Shizuku](https://shizuku.rikka.app/guide/setup/) via ADB and grant Honda Box access to manage the stock overlay setting.

The service icon starts headlessly. The separate Honda Box launcher opens the selector. Location is only needed for sunrise/sunset setup. Logging is off by default and can be enabled/reset in Debug. The app cannot guarantee survival of vendor task killers.

## Package identifiers

| App | Android package |
| --- | --- |
| Honda Box Service | `com.crewbeat.hondabox` |
| Honda Box selector launcher | `com.crewbeat.hondabox.launcher` |
| Radio launcher | `com.crewbeat.hondabox.radio` |
| CD launcher | `com.crewbeat.hondabox.cd` |
| USB/iPod launcher | `com.crewbeat.hondabox.usb` |

The new identifiers install separately from the old `dev.alex.fyt.probe` app and its launchers. This is not an in-place upgrade; Android does not transfer the old app's preferences or permissions automatically. The optional legacy iPod launcher source is not part of the standard build; the shared USB launcher is sufficient.

## Build

Python 3.9+, a JDK (17+ recommended), and an installed Android SDK are required. The build uses **platform android-33** and **Build Tools 36.0.0**. Install those SDK packages with Android Studio or `sdkmanager`; no dependency downloads occur during the build. Shizuku/AndroidX libraries are vendored with their licenses and checksums.

```sh
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"  # use your SDK path
export JAVA_HOME="/path/to/your/jdk"
python3 build.py
python3 build_launchers.py
```

On macOS, the scripts also detect Android Studio's bundled JDK. Other systems can use `JAVA_HOME` or the JDK on PATH. APKs and SHA-256 files go to **dist/**; intermediate files go to **build/**. The application/launcher versions are read from their manifests.

| Variable | Purpose |
| --- | --- |
| `ANDROID_SDK_ROOT` / `ANDROID_HOME` | SDK directory |
| `JAVA_HOME` | JDK directory |
| `ANDROID_BUILD_TOOLS` | Build Tools version (default 36.0.0) |
| `ANDROID_COMPILE_SDK` | Compile API (default 33) |
| `HONDA_BUILD_DIR`, `HONDA_OUTPUT_DIR` | Override build/output locations |
| `HONDA_KEYSTORE`, `HONDA_KEYSTORE_PASSWORD` | Existing signing key and password |

A fresh checkout creates an ignored local development key under `.local/signing/`. The owner's existing local signing key is preserved for future builds. **An APK signed with a different key cannot update an existing installation.** Keep the original key outside Git and use the signing variables for release builds. Locally generated development builds are not official releases.

## Tests

Portable checks using synthetic data:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tests/run_solar_tests.py
python3 build_tests.py
```

`build_tests.py` compiles the test runner and a **test-only FYT fixture**; it never installs them. See [testing](docs/testing.md) for emulator-only installation and standalone suites. Real vehicle captures and legacy replay assets are deliberately excluded from Git. The legacy broad replay suite requires those local captures; the documented standalone suites and host checks do not. Emulator samples are synthetic, and hardware checks are separate.

## Repository layout

```text
src/                 Android service, model, packet decoder and native UI
res/, assets/        Resources, generated artwork and bundled license notices
launchers/           Honda Box / Radio / CD / USB companion launchers
third_party/         Vendored Shizuku/AndroidX dependencies and licenses
tests/               Synthetic host checks, emulator runner and fixture
tools/              Log decoder and stock-overlay ADB helper
docs/               Architecture, testing and publishing notes
```

See [architecture](docs/architecture.md), [publishing](docs/publishing.md), and [contributing](CONTRIBUTING.md). The first public release is **0.8.30**, with the `com.crewbeat.hondabox` identifiers and the current UI. See [installation](docs/install.en.md) and [release notes](docs/releases/v0.8.30.md).

## License

Author-created code is licensed under [MIT](LICENSE). Dependency licenses and illustration provenance are documented in [NOTICE.md](NOTICE.md). Personal captures, signing keys, OEM firmware, decompiled applications, historical APKs and build outputs are excluded from the Git tree.
