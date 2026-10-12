# Publishing

The source repository is public at [Xelaan/honda-box](https://github.com/Xelaan/honda-box), with `main` as its default branch. Source publication and production APK releases are separate steps; build/preparation scripts do not publish either automatically.

## First push

Create an empty repository named `honda-box` under the intended GitHub account. Do not pre-create a README/license there. Replace the example owner below with your account:

```sh
git remote add origin git@github.com:YOUR_ACCOUNT/honda-box.git
git push -u origin HEAD:main
```

The local preparation branch is `codex/github-prep`; `HEAD:main` publishes that commit as the remote main branch. Set the remote default branch to main. The source code license is MIT; third-party material keeps the licenses described in NOTICE.md.

## Source review

`.gitignore` uses an explicit public-tree allowlist. Build outputs, APK/setup archives, keys, private vehicle recordings, old reports, replay assets and OEM/decompiled analysis remain local. Before adding new paths, check `git status --short` and `git diff --cached`. Do not use `git add -f` for captures, secrets or firmware. Debug logs can contain media names and other vehicle/user information; review/redact anything shared in an issue.

## Releases

Update the application version/code in AndroidManifest.xml and its displayed/exported version references together. Build with the original release key using HONDA_KEYSTORE/HONDA_KEYSTORE_PASSWORD; fresh development keys cannot produce compatible upgrades. Build companion launchers from their own manifests. Test installation and source/climate/metadata behavior, then attach the intended production APKs and SHA-256 files to a GitHub Release. TEST-ONLY files and keys do not belong in releases.

The current local 0.8.29 release is retained outside Git; later UI edits are in the prepared source tree. No new release/version/tag was created during repository preparation.
