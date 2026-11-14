# LZ/HZ Civic 2006–2011 compatibility

Profiles: **15729093 LOW**, **15794629 HIGH**. They share FYT callback
`Callback_0453_LZ_Honda_06Civic` and MainServer decoder `j0/ip` in the supplied FYT atlas.
This is a separate climate decoder, not Accord emulation. The optional Accord override
cannot enable Accord audio/command semantics for these known Civic profiles.

## Evidence and scope

The supplied LOW capture contains **54 valid TYPE 28 frames (6 payload bytes)**.
It exercises OFF, AUTO, 16–28°C, LOW/HI, fan levels 0–7, A/C and directions.
All right temperatures are unavailable. No TYPE 35, radio, playback or OEM menu frames
were captured. HIGH shares the OEM handler but has only been exercised synthetically.
Audio support is not established by this climate-only log. No CAN box hardware model
can be inferred solely from the selected FYT profile.

| Value | Interpretation |
|---|---|
| TYPE 28 payload[0] bit 6 | A/C, FYT field 11 |
| TYPE 28 payload[0] bit 3 | AUTO, FYT field 13 |
| TYPE 28 payload[1] bits 7/6/5 | Glass / face / feet, FYT fields 18/19/20 |
| TYPE 28 payload[1] low nibble | Fan 0–7, FYT field 21 |
| FYT fields 27/28 | Temperature in tenths; -2 LOW, -3 HI, -1 unavailable |
| TYPE 28 payload[2]/[3] = FF | Corresponding temperature unavailable |
| TYPE 28 payload[4] bit 0 | OEM unit flag, FYT field 37; no Fahrenheit sample captured |
| FYT field 98 | Distance in km, from TYPE 35 payload bytes 6–8 (big endian), **not audio source** |

The display uses FYT's normalized temperature callbacks rather than inventing an unverified
raw temperature conversion curve. Raw TYPE 28 supplies flags and inferred activity.
There is no confirmed dedicated power bit: AUTO, nonzero fan, or either available
raw temperature keeps climate active. OFF in the capture has AUTO=0, fan=0 and both
temperatures FF. This heuristic needs physical validation with additional variants.
If the right temperature is unavailable, both display positions and their change indicators mirror the left setpoint. DUAL is hidden on the Civic panel. Raw field28 and DUAL remain unchanged/unknown; a real right-zone value takes precedence. No Accord automatic windshield fallback
is applied when Civic reports no direction. Valid climate frames are no longer unknown
messages; unfamiliar Civic types remain visible instead of borrowing Accord meanings.

## OEM cross-check

Public decompiled FYT sources (commit `65fbd59a6bb6ac24196d6630770ab6246014f8bc`):

- [Callback](https://github.com/vasyl91/FYT-Launcher-Mod/blob/65fbd59a6bb6ac24196d6630770ab6246014f8bc/decompiled_com.syu.canbus/classes.dex.new/sources/com/syu/module/canbus/Callback_0453_LZ_Honda_06Civic.java)
  declares `U_CARINF_D35_D6_D7_D8 = 98` and registers codes 0–98.
- [Car Info](https://github.com/vasyl91/FYT-Launcher-Mod/blob/65fbd59a6bb6ac24196d6630770ab6246014f8bc/decompiled_com.syu.canbus/classes.dex.new/sources/com/syu/carinfo/rzc/sanlin/LZHonda06CivicCarInfo.java)
  subscribes to 98 and appends `Km` without scaling. The available Java does not distinguish
  odometer vs trip distance; retain the neutral label “distance”. Older `classes.dex`
  names the same constant at index **86**, so callback indices are firmware-dependent.
- [Climate UI](https://github.com/vasyl91/FYT-Launcher-Mod/blob/65fbd59a6bb6ac24196d6630770ab6246014f8bc/decompiled_com.syu.canbus/classes.dex.new/sources/com/syu/ui/air/AIR_0453_LZ_Honda_06Civic.java)
  confirms /10 temperatures, LOW/HI/unavailable, fields 11/13/18/19/20/21/37/79 and seven fan levels.
  Field 79's visual meaning is not inferred from bitmap coordinates alone.

## Verification

Run `python3 tests/run_civic_tests.py` with the project's Android SDK/JDK.
The numeric-only fixture preserves callback/raw ordering without device identifiers.
It replays all 54 frames for LOW and the shared HIGH route and tests malformed frames,
checksum rejection, AUTO activity, profile/session changes, log reset, unknown-message
classification and separation of field 98 from audio. Existing clock, USB/iPod,
recording and CD/radio volume checks run alongside it. No APK is produced or installed.

## Visible replay

`CarInfoActivity` shows a Civic-specific **Distance** card for positive 24-bit field98
values. Missing/negative/default-zero callbacks remain unknown. The card updates live;
Accord's source98 is never interpreted as kilometres.

Prepare the ignored local asset with `python3 tools/prepare_civic_replay.py /path/to/civic-log.json`,
build the app/test APKs, and install only on a disposable emulator. Use a 1280×800,
160 dpi landscape display and grant Honda Box overlay access. Start:

```sh
adb -s emulator-5560 shell am instrument -w com.crewbeat.hondabox.tests/com.crewbeat.hondabox.tests.CivicReplay
```

The emulator-only harness replays the capture without opening an app screen. Add
`-e verifyCarInfo true` to explicitly run the Car Info UI checks first (temporary synthetic
distance, cleared before playback; the test screen is closed afterward). It replays the capture at 1× into the production state and
climate overlay classes. The visible replay banner provides pause, restart and day/night.
It repeats up to 20 cycles and does not bind to FYT or send car commands. Stop by
force-stopping Honda Box in that emulator. The local climate log has no distance sample;
its replay leaves the distance card unknown. The test changes only disposable emulator
preferences; it is not intended to preserve personal emulator settings.

The Civic compact row uses an AUTO badge whenever climate is active but no direction
is reported, including AUTO=0 gaps in the capture. This is a display fallback, not an
inferred AUTO flag; the large panel retains the actual mode and direction flags.
