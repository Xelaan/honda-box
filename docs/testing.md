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
adb -s emulator-5554 install -r dist/Honda-Box-0.8.33.apk
adb -s emulator-5554 install -r dist/TEST-ONLY-probe-tests.apk
adb -s emulator-5554 shell appops set com.crewbeat.hondabox SYSTEM_ALERT_WINDOW allow
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.RadioDebugTests
```

Replace the main APK version when the manifest changes. Other existing standalone suites include `TitleHelpTests`, `PlayerControlsHelpTests`, `SetupGuideTests`, `PlayerClimateSpaceTests`, `PlayerResponsiveTests`, `ClimateSettingsButtonTests`, `ClimateSideButtonsTests`, `CompactClimateNavigationTests`, `TrackTitleCycleTests`, `AudioForegroundTests` and `SourcePowerOverlayTests`. Registered entry points are listed in `tests/runner/AndroidManifest.xml`.

A head-unit preview uses 1280×480 at density 160. `PlayerResponsiveTests` uses that preview to resize the actual player layout through 1280, 800, 640 and 480 dp, then restore its width. It checks all four sources in both themes and languages, long-title truncation, retained selection, fully visible timer/details/presets/controls, single-line Previous/Next labels without ellipsis in English/Russian, and absence of scrolling containers. On a taller tablet preview (at least 640 dp of content height), the same suite additionally resizes CD between 400 and 640 dp tall at three widths. It verifies one/two disc rows, complete labels/glyphs, and retained disc selection. This exercises measured window-size behavior; it does not emulate DUDU’s window manager. UI tests create synthetic source/climate values; these are not live vehicle readings. They may change emulator preferences while running and restore the preferences handled by their suite.

## Local replay and FYT fixture

`tests/runner/assets/` is intentionally ignored because it contains real vehicle captures. The broad historical `ProbeTests` replay suite is for the owner's local environment and requires those assets. It is not the clean-checkout default.

`TEST-ONLY-fyt-fixture.apk` provides a synthetic `com.syu.ms` service for IPC tests. Install it only in a disposable emulator, never on a head unit: it uses the same package identity as the real FYT service. `build_tests.py` compiles this fixture but never installs it automatically. Standalone suites listed above do not need that fixture.

An emulator verifies parsing and UI behavior; it cannot prove that a particular firmware/BSJ adapter accepts an outgoing audio command. Keep hardware results separate from synthetic tests.

`SetupGuideTests` opens all three setup guides in RU/EN and day/night on the head-unit emulator. It checks scrolling to the final instructions, closing, compact entry buttons, and a monitored fallback to Android Settings when FYT Settings is absent. It requires no FYT fixture and verifies that reading help does not start the service or dispatch commands.

## Live track-announcement text

`TrackOverlayRefreshTests` checks real overlay windows for CD, USB and iPod: a late TITLE replaces the track-number fallback, selecting another TITLE field updates the same card and accessibility caption, and neither refresh restarts the four-second lifetime. A new track counter cannot overwrite the preceding card before its own announcement; an expired card stays hidden on a non-track TITLE update. Run only in a disposable emulator, with overlay permission granted; no FYT fixture or vehicle commands are involved.

```sh
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.TrackOverlayRefreshTests
```

Verified: 29 checks passed on API34. `TrackTitleCycleTests` separately checks announcement detection and iPod TITLE rotation.

## OEM menu dismissal

`MenuCloseOverlayTests` exercises actual overlay windows on a disposable emulator with synthetic BSJ packets. It checks CD, USB and iPod (including iPod with either source2 or source4): explicit TYPE03 bit6=0 dismisses the menu; subsequent playback05 and TITLE07 updates leave it closed; new menu09 content can reopen it and a repeated explicit close dismisses it again. It sends no vehicle commands and requires no FYT fixture.

```sh
adb -s emulator-5554 shell appops set com.crewbeat.hondabox SYSTEM_ALERT_WINDOW allow
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.MenuCloseOverlayTests
```

Verified: 51 checks passed on API34. This confirms the existing shared close handler for all media players; it does not establish that every firmware emits the close packet on every iPod selection.

## Service recovery integration

Before starting a different suite in the disposable emulator, force-stop `com.crewbeat.hondabox` to cancel pending recovery work from the previous instrumentation process. This isolates test-process replacement from the background recovery job; it is not a head-unit setup step.

`BackgroundRecoveryTests` requires the separately signed FYT fixture. Run only in a disposable emulator; it refuses non-emulator hardware before changing service state or preferences. Never install the fixture on a head unit.

```sh
adb -s emulator-5554 install -r dist/TEST-ONLY-fyt-fixture.apk
adb -s emulator-5554 shell am force-stop com.crewbeat.hondabox
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.BackgroundRecoveryTests
```

It uses cross-UID ACC broadcasts from the fixture, an actual system `JobScheduler` execution through the emulator shell, service destruction/recovery, stale/fresh connections, disabled recovery, boot/update/ACC policy, cache persistence and the headless launcher. It asserts manual Stop is respected and no vehicle commands were emitted. The suite does not force-stop or reboot its own instrumentation process; real firmware process removal and deep sleep remain hardware checks. See [background operation](background-operation.md).

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
