# Boot, FYT sleep and service recovery

Configure the head unit to launch **Honda Box Service** (`com.crewbeat.hondabox`) in FYT **Settings → User → Application Settings** after boot/wake. Where supported, also add the service package to the firmware/DUDU keep-alive list and allow unrestricted Android background operation. The separate **Honda Box** companion launcher opens the UI and is not the startup target.

## In-app setup guides

Open **Honda Box → Settings → Background operation**. The **FYT setup**, **DUDU setup** and **Check & help** buttons open offline, scrollable instructions in the selected app language and theme. Reading a guide does not start the service or modify platform settings. Settings navigation happens only when its button is tapped.

The FYT guide can open the installed `com.syu.settings` launcher; when unavailable it explains the fallback and opens Android Settings. The system-settings button does not claim to deep-link into a DUDU-specific page.

For stock FYT, the boot/deep-sleep startup route is documented in the [XDA FYT Q&A](https://xdaforums.com/t/general-fyt-based-spreadtrum-uis7862-s-unisoc-ums512-uis8581a-sc9863-q-a-mods-tips-firmware.4396339/post-90341403). DUDU OS users report **Vehicle → Startup Settings** with startup and sleep keep-alive lists in the [DUDU 3.6 discussion](https://forum.dudu-auto.com/d/1412-duduos-36-official-release-elevate-your-driving-experience250515?page=14). DUDU support also describes an ignition-triggered **Automatic Task** in [this startup discussion](https://forum.dudu-auto.com/d/3083-carplay-autostart); availability varies by firmware. DUDU Launcher on stock FYT should use the FYT route if those firmware menus are absent.

The checklist covers a real Android reboot separately from ACC deep sleep, intentional Stop, unwanted player windows, missing overlays and missing vehicle data. Short diagnostic capture instructions explain resetting, recording and exporting the log without enabling continuous logging for normal use.

## App behavior

- **Start after boot / ignition** defaults to on. Android boot, package update and the best-effort manifest FYT `ACCON` receiver request a foreground service. The headless service launcher uses the same automatic-start policy.
- **Recover service automatically** defaults to on. A single native `JobScheduler` job checks whether a user-requested service has stopped. The minimum interval is 15 minutes; Android can delay execution. A running but quiet media source does not trigger a reconnect on each periodic check.
- The service listens at runtime for FYT `com.fyt.boot.ACCON` / `ACCOFF` and Android `SCREEN_ON`, alongside its existing FYT main-screen callback. Duplicate wake signals are coalesced. Fresh subscriptions and binds in progress are retained; stale subscriptions are reconnected after wake or a repeated service start.
- On `ACCOFF`, the app records the sleep state, cancels the recovery job and reconnect/connection-timeout callbacks, captures the last received menu state, and flushes menu/preset caches. It does not stop or force-stop itself. On wake the periodic job is re-established if enabled.
- Periodic recovery requires both the awake flag and an interactive display. It never sends source, playback or climate commands. There are no app wake locks, exact alarms or busy restart loops.
- **Stop** pauses all automatic paths and cancels the periodic job, including starts queued just before Stop. The headless launcher also respects this pause. Open the **Honda Box UI** or use its explicit connect/start action to resume.
- Turning boot/ignition autostart off does not stop an already running, manually requested session. Recovery can protect that session. Jobs are intentionally not persisted across a reboot, so they do not bypass the disabled boot setting.

`lastStartReason`, `lastStartMs`, `lastWakeMs` and `lastSleepMs` are retained in the app's background preferences for diagnostics. Start reasons include `manual`, `boot`, `package_replaced`, `fyt_launcher`, `acc_on`, `watchdog` and `sticky_restart`.

## Limits and hardware verification

`START_STICKY`, a foreground notification and a periodic job cannot guarantee survival of vendor force-stop. Android restrictions can prevent a custom implicit ACC broadcast from starting a manifest receiver. The firmware's own autostart is the primary recovery entry after process removal; the manifest ACC receiver is an additional path, not a replacement.

Recovery starts after user unlock. `LOCKED_BOOT_COMPLETED` is intentionally not registered: the app's current settings and caches use credential-protected storage, and full Direct Boot support would require a separate storage/lifecycle design.

Verify cold boot and a genuine deep-sleep ACC OFF/ON cycle separately on the head unit. Confirm headless startup, source overlay, restored FYT data and preserved station/menu cache. Then stop from Honda Box and confirm ACC/startup/job triggers do not undo that deliberate pause. Emulator tests validate Android IPC and lifecycle behavior, not a particular FYT firmware's broadcast or keep-alive policy.

## Русский

В **Honda Box → Настройки → Фоновая работа** добавлены кнопки **«Настройка FYT»**, **«Настройка DUDU»** и **«Проверка и помощь»**. Инструкции доступны без интернета, поддерживают русский/английский и обе темы. Они не меняют разрешения или списки автозапуска за пользователя. Кнопка FYT открывает штатное приложение настроек, а при его отсутствии — настройки Android с пояснением.

Руководства различают прошивку DUDU OS и DUDU Launcher поверх штатной FYT. Названия и наличие пунктов зависят от прошивки. В проверке отдельно описаны настоящая перезагрузка Android, глубокий сон, ручная остановка, разрешения оверлеев, нежелательное открытие плеера и короткая запись лога.

В штатном FYT автозапуске после загрузки и сна выберите **Honda Box Service**, пакет `com.crewbeat.hondabox`. Отдельный ярлык **Honda Box** открывает интерфейс. При наличии списка Keep alive добавьте туда пакет службы и разрешите фоновую работу Android без ограничений.

Два переключателя фоновой работы включены по умолчанию: запуск после загрузки/зажигания и автоматическое восстановление службы. Периодическая проверка использует встроенный `JobScheduler` с минимальным интервалом 15 минут; Android может задержать выполнение. При ACC OFF задание и повторные подключения отменяются, меню и станции сохраняются. После пробуждения проверка возвращается. Приложение не использует постоянный wake lock и не отправляет команды машине при восстановлении.

Ручная остановка блокирует все автоматические запуски, в том числе ярлык службы. Для возобновления откройте интерфейс **Honda Box**. Задание не переносится через перезагрузку и не обходит отключённый запуск после загрузки. После принудительной остановки прошивкой всё ещё нужен штатный FYT autostart. Работу до разблокировки Android эта реализация не заявляет.
