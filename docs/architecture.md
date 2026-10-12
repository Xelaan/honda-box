# Architecture

`ProbeService` owns the FYT connection, callback subscriptions, reconnection and overlay controllers. `ProbeState` preserves received values and session/source freshness. MainServer callbacks include an adapter-byte channel; `BsjStream` parses framed adapter data before the profile-specific model interprets it. These frames are a different protocol layer from vehicle CAN identifiers.

The UI consists of `MainActivity`, `AudioPlayerActivity`, `SettingsActivity`, `DebugActivity` and experimental `CarInfoActivity`. `ThemeSettings`, `I18n` and shared controls provide appearance/localization. Separate `WindowManager` overlays display climate, volume, source, OEM menu and track changes. The compact climate strip and its side controls share one window and animation lifecycle.

The companion launchers are tiny independent packages that open exported UI/alias entry points. They do not connect to the bus or reproduce the service. The main launcher entry starts headlessly.

Reading and explicit command transmission are separate. `AudioCommand`/`AudioDebugOptions` and `ProbeService` implement deliberate command experiments. Opening overlays, theme changes and lifecycle wakeup do not send climate commands. Firmware/profile restrictions are checked before dispatch. Metadata is kept as a combined OEM field; missing text is not reconstructed from unrelated titles.

The service uses no web backend. Recording is opt-in; exports are local. Real captures and proprietary OEM analysis are retained only in the owner's ignored working files.
