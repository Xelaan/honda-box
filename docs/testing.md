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
adb -s emulator-5554 shell appops set dev.alex.fyt.probe SYSTEM_ALERT_WINDOW allow
adb -s emulator-5554 shell am instrument -w dev.alex.fyt.probe.tests/dev.alex.fyt.probe.tests.RadioDebugTests
```

Replace the main APK version when the manifest changes. Other existing standalone suites include `TitleHelpTests`, `PlayerControlsHelpTests`, `PlayerClimateSpaceTests`, `ClimateSettingsButtonTests`, `ClimateSideButtonsTests`, `TrackTitleCycleTests`, `AudioForegroundTests` and `SourcePowerOverlayTests`. Registered entry points are listed in `tests/runner/AndroidManifest.xml`.

A head-unit preview uses 1280×480 at density 160. UI tests create synthetic source/climate values; these are not live vehicle readings. They may change emulator preferences while running and restore the preferences handled by their suite.

## Local replay and FYT fixture

`tests/runner/assets/` is intentionally ignored because it contains real vehicle captures. The broad historical `ProbeTests` replay suite is for the owner's local environment and requires those assets. It is not the clean-checkout default.

`TEST-ONLY-fyt-fixture.apk` provides a synthetic `com.syu.ms` service for IPC tests. Install it only in a disposable emulator, never on a head unit: it uses the same package identity as the real FYT service. `build_tests.py` compiles this fixture but never installs it automatically. Standalone suites listed above do not need that fixture.

An emulator verifies parsing and UI behavior; it cannot prove that a particular firmware/BSJ adapter accepts an outgoing audio command. Keep hardware results separate from synthetic tests.
