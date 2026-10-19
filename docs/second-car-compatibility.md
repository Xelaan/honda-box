# Second Accord: AUTO climate and incomplete FYT audio callbacks

Analysis of the owner's private `fyt-probe-1792424201985.json` export (Honda Box 0.8.34).
The recording itself is not included in this repository.

## Confirmed observations

- Actual CAN profile is **6291908**; the app's Accord compatibility override was enabled.
- The stream uses the same BSJ framing and Honda field layout. No identification packet `7F` was captured, so the physical adapter model/firmware cannot be confirmed as V704.
- Powered AUTO is `02: 88 10 14 14 00 00` (later also `88 10 20 2A 00 00`): power=1, AUTO=1, fan=0, valid set temperatures. This adapter does not supply a numeric fan level in this AUTO state. Manual fan levels 1–3 appear normally.
- Real OFF is `02: 00 10 00 00 00 00`. The original car also has a different OFF pattern: power may remain 1 while AUTO=0, fan=0 and temperatures are unset. Both OFF patterns must stay off.
- All 36 captured `03` frames have byte0=`82`: car audio on, source **3 / AUX**, menu closed. The AUX label was customized to Android. Source field98 remains the FYT startup zero, so raw source interpretation is necessary.
- Raw volume varies from 24 to 40 and ends at 27. FYT field132 remains 27 during those later changes. Reading only the logical FYT callback loses real volume changes.

### Frames in the recorded segment

| Type | Count | Interpretation |
| --- | ---: | --- |
| `01` | 38 | Car button events; do not guess a resulting source from a key alone |
| `02` | 24 | Climate |
| `03` | 36 | Source, audio power, volume, menu visibility |
| `0A` | 4 | Unassigned: `01 F2 39` through `01 F2 42` |
| `0B` | 4 | Unassigned: `00 00 00` or `10 00 00` |
| `11` | 140 | Ignored status/door traffic; door support is out of scope |

There are **246 complete checksum-valid frames** in the raw segment, reconstructed from 247 chunks.
Two `7B` samples (`10 00 00 80 00 00 00 00`) are also retained in the export's unidentified-message context from before the recording. Their purpose is unconfirmed.

No `04` radio, `05` playback, `06` changer, `07` title, `09` menu or `7F` adapter-identification packets were captured. The initial/default zero media callbacks are not proof that CD, USB or iPod is unsupported. The owner has not yet confirmed whether those sources were exercised in this run.

## Additional correlations and display limits

These are hypotheses from this recording, not newly decoded functions:

- TYPE0A intervals are 60.291, 59.989 and 60.056 seconds. Its last byte advances `39 → 40 → 41 → 42`, consistent with BCD minutes. A clock packet is plausible; the first two bytes and time format are not established.
- TYPE0B=`00 00 00` occurs 105/140 ms after the first volume packet in two adjustment bursts. TYPE0B=`10 00 00` occurs 1.400/1.119 seconds after the last adjustment, followed 142/35 ms later by TYPE03 volume with byte1 bit7 cleared. This is consistent with entering/leaving the OEM volume display, but could be a more general temporary-display state. No screen/menu behavior is driven from this hypothesis.
- TYPE03 byte1 bit7 is set during those adjustment bursts and cleared in the subsequent stable volume packets. The lower seven bits remain the volume value, matching the stock handler. Bit7's display-state meaning is not independently verified.
- Callback1049 contains 249 arrays `[11,64]` and 43 arrays `[6,0]` in the recorded callback history. Neither is the known subtype0 steering value. They are retained in diagnostics without being labelled RPM, speed, steering angle or gear.

The current airflow display has a deliberate legacy fallback: no direction bits + powered climate + valid temperature highlights the windshield icon. In this car's AUTO state all direction bits are zero, so this icon is a fallback rather than evidence of windshield airflow. Automatic fan speed is also unavailable in this state. DUAL remains inferred from unequal valid left/right set temperatures when no verified DUAL field is supplied. Recirculation/rear defrost remain unconfirmed and hidden.

## Shared app corrections

1. In Accord processing, powered AUTO with a valid set temperature stays on when fan=0, and displays AUTO instead of inventing a fan speed. A missing AUTO flag, missing temperature, or explicit power=0 does not bypass the original fan-based OFF workaround.
2. Player volume and the volume overlay share one getter, preferring validated BSJ `03` over the logical field132. Logical-only operation remains supported, and the original FYT callback values remain available in Debug/export.
3. The source label reads the same raw-or-logical radio band as the radio player, so raw-only `04` data can identify FM1/FM2/AM1/AM2.
4. BSJ `06` decodes all six disc states into display fields106–111 using the stock Honda handler's high-nibble/low-nibble order. The verified iPod-marker logic remains separate; slot states never select a source by themselves.
5. Debug → CAN data received shows connection-level receipt counts, last lengths and ages, plus source/volume transport. These bounded counters work with diagnostic recording off and retain no packet history. Log reset preserves them; reconnect/profile change resets them. A received packet is evidence of transport, not proof of successful decoding or feature support.

These paths apply automatically to profile6291908 and through the existing explicit Accord-processing option for other profiles. They do not alter the head unit's configured CAN profile or send additional vehicle commands. Other profiles using a different protocol still need hardware verification.

## Hardware validation still needed

A short recording covering Radio → CD → USB/iPod → AUX via the car's MODE button, a few TITLE presses, and volume changes would establish whether playback packets are absent or whether a different layout is used. Keep an action order or manual markers. Emulator replay verifies parser/UI behavior, not second-car hardware compatibility or command acceptance.
