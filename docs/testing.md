# Testing

## Host checks

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tests/run_solar_tests.py
```

The decoder tests generate packets locally. SolarTimes tests require a JDK but no Android device. GitHub Actions runs these checks and compiles the app, launchers and test APKs.

## Standalone Android suites

Use an emulator with overlays enabled. Build first:

```sh
python3 build.py
python3 build_tests.py
adb -s emulator-5554 install -r dist/Honda-Box-0.8.29.apk
adb -s emulator-5554 install -r dist/TEST-ONLY-probe-tests.apk
adb -s emulator-5554 shell appops set com.crewbeat.hondabox SYSTEM_ALERT_WINDOW allow
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.RadioDebugTests
```

Replace the main APK version when the manifest changes. Other existing standalone suites include `TitleHelpTests`, `PlayerControlsHelpTests`, `PlayerClimateSpaceTests`, `ClimateSettingsButtonTests`, `ClimateSideButtonsTests`, `TrackTitleCycleTests`, `AudioForegroundTests` and `SourcePowerOverlayTests`. Registered entry points are listed in `tests/runner/AndroidManifest.xml`.

A head-unit preview uses 1280×480 at density 160. UI tests create synthetic source/climate values; these are not live vehicle readings. They may change emulator preferences while running and restore the preferences handled by their suite.

## Local replay and FYT fixture

`tests/runner/assets/` is intentionally ignored because it contains real vehicle captures. The broad historical `ProbeTests` replay suite is for the owner's local environment and requires those assets. It is not the clean-checkout default.

`TEST-ONLY-fyt-fixture.apk` provides a synthetic `com.syu.ms` service for IPC tests. Install it only in a disposable emulator, never on a head unit: it uses the same package identity as the real FYT service. `build_tests.py` compiles this fixture but never installs it automatically. Standalone suites listed above do not need that fixture.

An emulator verifies parsing and UI behavior; it cannot prove that a particular firmware/BSJ adapter accepts an outgoing audio command. Keep hardware results separate from synthetic tests.

## Documentation screenshots

The checked-in feature gallery uses real UI with synthetic source/climate packets. It does not send vehicle commands. `DocumentationScreenshots` is restricted to an Android emulator and expects 1280×480 pixels at density 160. It stops the emulator service, seeds the model, captures both fixed themes in English with the original accent, and restores the saved appearance/overlay/background/diagnostic preferences. No vehicle logs or FYT fixture APK are required.

After building and installing the main app and test runner with the same development key:

```sh
adb -s emulator-5554 shell wm size 1280x480
adb -s emulator-5554 shell wm density 160
adb -s emulator-5554 shell appops set com.crewbeat.hondabox SYSTEM_ALERT_WINDOW allow
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.DocumentationScreenshots
adb -s emulator-5554 pull /sdcard/Android/data/com.crewbeat.hondabox/files/documentation/. docs/images/features/
```

Run only in a disposable emulator. The capture ends with the service stopped; open Honda Box again to reconnect. Review the images before replacing the gallery. Both README files link to the same PNGs; maintain matching `-day` and `-night` pairs. These documentation captures show interface states, not a hardware compatibility test.
