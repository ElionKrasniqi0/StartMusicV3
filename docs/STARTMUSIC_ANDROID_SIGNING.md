# StartMusic Android production signing

Safe reference for local release builds and CI. **Never commit passwords, keystores, or `secrets.properties`.**

## Product / package

| Item | Value |
|------|--------|
| Product name | **StartMusic** |
| Repository | StartMusicV3 |
| Application ID (release) | `io.startmusic.android` |
| Application ID (debug) | `io.startmusic.android.debug` |
| Upload key alias | `startmusic` |
| Certificate DN | `CN=StartMusic, OU=Mobile, O=StartMusic, C=US` |
| Certificate SHA-256 | `319fb7dfb59df505e9017aa93a5d8903520b3135dfd57fad58fff60bb482e6bd` |

Debug and release use **different** application IDs, so a debug install and a release install can coexist on one device.

## Where the keystore lives

| Location | Purpose |
|----------|---------|
| `C:\Users\IT\Documents\StartMusic-release-signing\` | Primary local backup (outside the Git repo) |
| Second local copy | Keep an independent encrypted/offline copy of the same folder |
| `signing/startmusic-release.p12` | Working copy for Gradle (**gitignored**) |
| `secrets.properties` | Local `android.signing.*` properties (**gitignored**) |

Do **not** put the keystore or credential document inside a published release zip or commit them to GitHub.

## Local release signing

Gradle reads (env wins over properties; see `AndroidSigning.kt`):

- `android.signing.storeFile` / `YMUSIC_ANDROID_STORE_FILE`
- `android.signing.storePassword` / `YMUSIC_ANDROID_STORE_PASSWORD`
- `android.signing.keyAlias` / `YMUSIC_ANDROID_KEY_ALIAS`
- `android.signing.keyPassword` / `YMUSIC_ANDROID_KEY_PASSWORD`

Typical local setup (paths relative to repo root):

```properties
android.signing.storeFile=signing/startmusic-release.p12
android.signing.keyAlias=startmusic
# passwords only in the gitignored secrets.properties / password manager — never in docs
```

Build:

```bat
gradlew.bat :androidApp:assembleRelease -PandroidAbis=arm64-v8a -Pproduction=true
```

Confirm the build log does **not** warn that release is debug-signed.

## Restore from backup

1. Copy `startmusic-release.p12` from the backup folder into `signing/` (create the directory if needed).
2. Recreate gitignored `secrets.properties` with the four `android.signing.*` keys (passwords from your password manager / backup credential sheet).
3. Rebuild release and verify `apksigner` certificate SHA-256 matches the fingerprint above.

## Existing `secrets.enc`

The repository already contains an older encrypted `secrets.enc` (Sentry / remote config / other secrets may be inside).

- Do **not** overwrite `secrets.enc` with a seal that contains only the Android keystore.
- To merge: decrypt with `SECRETS_PASSPHRASE` / `./decrypt.sh`, preserve every existing entry, add Android signing files if appropriate, then re-encrypt with `./encrypt.sh` only after verifying a successful decrypt of the new archive.
- CI expects GitHub Actions secret **`SECRETS_PASSPHRASE`** to run `./decrypt.sh` before release assemble (see `.github/workflows/release.yml`).

## Never commit or publish

- `signing/`
- `secrets.properties`
- `*.p12` / `*.jks` / private keys
- Credential sheets / passwords / tokens
- Decrypted copies of `secrets.enc` contents
