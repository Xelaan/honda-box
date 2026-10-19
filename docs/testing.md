# Testing

## Host checks

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tests/run_solar_tests.py
python3 tests/run_aux_playback_tests.py
```

The decoder tests generate packets locally. SolarTimes tests require a JDK but no Android device. GitHub Actions runs these checks and compiles the app, launchers and test APKs.

## Standalone Android suites

Use an emulator with overlays enabled. Build first:

```sh
python3 build.py
python3 build_tests.py
adb -s emulator-5554 install -r dist/Honda-Box-0.8.35.apk
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

`TrackOverlayRefreshTests` checks real overlay windows for CD, USB and iPod: a late TITLE replaces the track-number fallback, selecting another TITLE field updates the same card and accessibility caption, and neither refresh restarts the five-second lifetime. A new track counter cannot overwrite the preceding card before its own announcement; an expired card stays hidden on a non-track TITLE update. Run only in a disposable emulator, with overlay permission granted; no FYT fixture or vehicle commands are involved.

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

## Android playback around AUX

`AuxPlaybackTests` is an Android-free policy suite covering real source transitions, already-paused playback, all three return modes, disabling pause, manual playback override, asynchronous/ignored PAUSE, session changes, disconnection and AUX remapping. Run it with `python3 tests/run_aux_playback_tests.py`.

`AuxPlaybackIntegrationTests` runs only on an emulator. It uses a real Android `MediaSession` and a muted looping `AudioTrack` to verify media-key Pause/Play delivery, return policies, and the compact settings dialog in RU/EN and both themes. No FYT fixture or vehicle commands are required.

```sh
adb -s emulator-5554 shell am force-stop com.crewbeat.hondabox
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.AuxPlaybackIntegrationTests
```

Verified on API34: 41 host policy checks and 137 native settings/media checks passed.

Playback control is independent of automatic player-window and overlay settings. It uses the configured raw AUX source, so menu-based iPod detection alone cannot cause media commands. Connection, invalid source, profile change, service stop and ACC sleep discard pause ownership; restoring a connection never invents a return to AUX. Default resume requires an observed playing→auto-pause sequence in the current service session. An ignored pause expires after two seconds; independently resumed playback cancels ownership of a later user pause. Android routes media keys to its current media-button consumer, so firmware/player behavior still needs a head-unit check.

## Optional Accord processing

The previous automatic fan fallback for every supported Honda profile has been replaced with the bottom of Settings → Process as Honda Accord (default OFF). Native 6291908 retains its verified behavior. The override applies the same handling to any positive actual CAN profile while preserving field1000 and normalized callback values in snapshots/exports. Missing/invalid profiles do not gain inferred support.

`ClimatePowerFallbackTests` exercises power missing/OFF/ON, fan missing/invalid/0..7, supported and otherwise unknown profiles, both override choices, compact eligibility, large-panel power indication, and stopped/disconnected states.

`AccordCompatibilityTests` compares effective source, radio presets, DUAL, CD text retention during volume changes, iPod menu/marker inference and experimental packet capture against native 6291908. It checks profile-isolated caches, clearing derived state on toggle, the real settings checkbox in EN/RU and both themes, persistent defaults, and real service reconnects through the cross-UID emulator fixture without vehicle commands.

```sh
adb -s emulator-5554 shell am force-stop com.crewbeat.hondabox
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.ClimatePowerFallbackTests
adb -s emulator-5554 shell am force-stop com.crewbeat.hondabox
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.AccordCompatibilityTests
```

The override is protocol interpretation, not a head-unit CAN configuration write. Another profile using a different protocol is not guaranteed compatible. Analyzer experiment windows continue to retain and filter on actual profile IDs.

Verified on API34: 2,163 climate checks and 165 compatibility/settings/reconnect checks passed. These are emulator checks with synthetic data; another CAN profile still needs hardware verification.

## AUTO with no numeric fan speed

A second Accord with profile 6291908 reports powered AUTO as power10=1, AUTO13=1, fan21=0, with valid set temperatures. The compact strip must remain visible and display AUTO in place of a numeric zero. The original car’s OFF sample has power10 stuck at1, AUTO13=0, fan21=0 and unset temperatures; it must remain OFF. AUTO without a set temperature, a missing AUTO flag, or explicit power0 does not bypass the original zero-fan OFF fallback.

`ClimateAutoPowerTests` covers both patterns, reported numeric fan speeds in AUTO, valid LOW/HI temperatures, disconnected data, native/opt-in/disabled profiles, and actual compact/full panel windows in both themes. It uses synthetic values and does not include the private vehicle recording or send CAN commands.

```sh
adb -s emulator-5554 shell am force-stop com.crewbeat.hondabox
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.ClimateAutoPowerTests
```

Verified on API34: 117 AUTO/window checks, 2,163 existing power/compact checks, 35 compact side-button checks and 9 offline decoder checks passed. The old-car OFF pattern remains hidden, while powered AUTO with fan0 stays visible and shows AUTO.

## Raw BSJ / logical FYT compatibility

`BsjFallbackTests` verifies source and volume with stale FYT startup/default values, raw-only radio/CD/USB/menu data, TYPE06's six nibble-packed disc states, logical-only operation, the existing CD-to-iPod marker, AUX isolation, malformed lengths/checksums, disconnected values, and connection/profile evidence resets. It also checks actual volume/player/Debug views, including RU/EN receipt descriptions. No fixture service or vehicle commands are needed. The standalone suite passed 132 checks on API34.

```sh
adb -s emulator-5554 shell am force-stop com.crewbeat.hondabox
adb -s emulator-5554 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.BsjFallbackTests
```

An optional private asset supplied with `-e replay second-car-bsj-20261019.json` contains only the second car's raw transport chunks. This asset remains ignored and is not required by the standalone suite. The local replay checks all 247 chunks / 246 complete frames, every raw volume update despite fixed logical volume27, stable AUX, and absence of invented player packets. **436 checks passed** with this optional replay on API34. Missing radio/playback packets in this capture are not evidence that the second car supports or lacks those functions.

See [second-car-compatibility.md](second-car-compatibility.md) for the observed packets, compatibility corrections and hardware validation still needed.

After the shared fallbacks, API34 regression suites also passed: ClimateAutoPowerTests 117, ClimatePowerFallbackTests 2,163, RadioDebugTests 40, TrackTitleCycleTests 37, MenuCloseOverlayTests 51, SourcePowerOverlayTests 20, AccordCompatibilityTests 165, and TrackOverlayRefreshTests 29. The offline decoder passed 9 tests. These results do not establish second-car playback or command support.
