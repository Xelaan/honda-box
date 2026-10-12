# Contributing

Use the build and checks in README.md. Keep changes profile-aware: unknown values remain unknown, reconnect/source freshness must not replay stale data, and incoming BSJ types are separate from FYT callback codes.

Do not add a guessed bus-command sweep or send hardware commands from a normal rendering/lifecycle path. Put deliberate command experiments behind Debug and document their evidence. For protocol work, provide a minimal redacted capture or a synthetic regression case and describe the hardware profile/adapter/firmware used. Keep personal captures, signing keys and OEM firmware out of Git.

UI work should be checked at 1280×480 in both themes and languages, including long titles and overlay coexistence. Keep the foreground service/headless startup behavior intact. Contributions are under the project's MIT license.
