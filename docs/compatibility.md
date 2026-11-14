# Supported cars and CAN boxes

Compatibility is **car + wiring + physical adapter + selected FYT profile + firmware**, not the brand name alone. A profile label such as XC(BSJ), XBS or LZ(HZ) does not identify the adapter hardware/firmware. Honda Box reads the FYT service; it does not connect directly to Honda CAN or bypass the box.

| Vehicle / FYT selection | Profile | Adapter evidence | Support in 0.8.36 |
| --- | --- | --- | --- |
| Accord VIII / Acura TSX II / Spirior 2009–2013, XC(BSJ) | **6291908** | Owner-tested BSJ V704, identification `XC-GBJR-0722V704` | Main supported configuration: climate, OEM source/volume, radio/CD/USB/iPod display and menu. Outgoing audio commands remain firmware-dependent. |
| Second Accord / pre-facelift capture, same XC(BSJ) selection | **6291908** | BSJ-framed data; no identification frame confirming the physical model | AUTO climate and AUX/volume fallbacks checked against capture. Radio/CD/USB/iPod not exercised in that recording. See [second-car evidence](second-car-compatibility.md). |
| Accord/Acura owner's captures, FYT “XBS Honda Accord 8” | **9175479** | XBS profile; hardware model unconfirmed | **Partial / diagnostic**. Climate packets and clock candidates captured; intermediate temperature/fan completeness not established. Do not describe this as full Acura audio/climate support. Accord processing is opt-in and cannot recover data absent from the adapter. |
| Civic 2006–2011 LOW, LZ(HZ) | **15729093** | LZ(HZ) profile; physical model unconfirmed | **Climate supported from a real capture + emulator replay**. Separate decoder; AUTO/OFF, A/C, fan, temperatures, airflow. Missing right temperature mirrors left; no DUAL. Field98 Distance is based on OEM code, not verified distance from this capture. No OEM audio/menu evidence. |
| Civic 2006–2011 HIGH, LZ(HZ) | **15794629** | Shares LOW's FYT handler | Same climate route implemented and synthetically tested; **no HIGH vehicle capture yet**. |
| Accord 8 XC(BSJ), Crosstour 2011–2012 XC(BSJ), Accord 8 HIWORLD | **3604932 / 6357444 / 12321220** | Related handler in examined FYT code | **Candidates, not verified support**. Shared code does not establish wiring, packet equivalence or working commands. Requires capture and review; optional Accord processing is not automatic hardware detection. |
| Other cars / CAN-box manufacturers | Other IDs | Not established | No blanket compatibility claim. Generic FYT callbacks may be visible; a matching decoder requires evidence. |

Model years are **FYT profile labels**, not proof that every market/body style (including Civic 8G hatchback) uses the same protocol. Choose a profile correct for the actual adapter and car; never switch a Civic to Accord solely to enable Honda Box. The known Civic LOW/HIGH IDs always use their dedicated decoder, even if “Process as Honda Accord” is enabled.

## Feature limits

- Civic raw TYPE28 flags and normalized FYT temperatures use their own schema. AUTO can report fan0/no direction; the compact AUTO badge is a display fallback. Full climate retains the actual AUTO flag. [Decoder evidence and replay](civic-lz-compatibility.md).
- Civic field98 is kilometres, while Accord field98 is an audio source. Car Info deliberately calls it Distance; trip versus odometer is unconfirmed. It does not open automatically.
- Android MediaSession track overlays work independently of the OEM decoder when AUX/unknown source is active, subject to notification access and a compatible player. This does not add OEM media support to a climate-only profile.
- Experimental TYPE0A clock candidates are retained in Debug with raw bytes. Confirmed-looking HH:mm patterns are not a universal clock protocol or clock-setting command.
- Door support is outside project scope. Recirculation/rear-defrost indicators are omitted where not reliable. RDS station text and raw Honda CAN IDs are not established by BSJ framing alone.

## Requesting support

Include the vehicle year/body/market, FYT profile ID from Debug, adapter label/firmware if known, head-unit firmware, and a short diagnostic recording. Enable recording and the available raw packet subscription in Debug, reset the log, then exercise only the relevant feature: climate OFF/AUTO/manual, intermediate temperatures/LOW/HI, fan and directions; or radio/CD/USB/iPod/AUX in a known order. Note actions and what the OEM display showed. Export after the test; ordinary recording-off summaries are not equivalent to a raw capture. Review personal media/device information before sharing a log. Never publish firmware or signing keys with a report.
