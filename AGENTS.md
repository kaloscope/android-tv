# Kaloscope Android TV Agent Guide

Native Android TV client for [kaloscope/kaloscope](https://github.com/kaloscope/kaloscope).
The server is user-managed and is not part of this repository.

## Working rules

1. Run `git status --short` before editing; preserve unrelated user changes.
2. Use `rg` to inspect the affected implementation, callers, resources, and
   tests. Start with the code map below rather than scanning the entire project.
3. For client behavior, follow the user's requirements, then current production
   code and tests. For build assumptions, read the Gradle files. For API changes,
   verify routes, encoding, DTOs, and behavior against public upstream source or
   documentation; local fixtures alone do not prove the server contract.
4. Implement the smallest complete change. Avoid unrelated refactors,
   speculative abstractions, empty screens, and placeholder repositories.
5. Run relevant targeted checks, inspect the final diff, and report what changed,
   what was verified, and any remaining uncertainty. Never invent API contracts.

Keep the project portable: do not make it depend on sibling repositories,
absolute paths, local accounts, browser sessions, private servers, or
machine-specific tools. Do not assume a server, device, emulator, or signing key
is available. Prototype content is visual reference only; production uses real
repositories. Fixtures and sample data belong only in tests or previews.

Never commit or log credentials, tokens, cookies, private server URLs, media
paths, keystores, or local configuration. Use synthetic data in tests and
examples. Do not log authorization headers or response bodies containing private
data, or inspect private configuration to obtain test credentials.

## Code map and architecture

Kotlin paths below are relative to `app/src/main/java/org/kaloscope/tv/`;
resource paths are relative to the repository root.

| Area | Start here |
| --- | --- |
| Startup and authentication | `app/KaloscopeApp.kt`, `app/KaloscopeViewModel.kt`, `app/bootstrap/`, `app/RootStateInspection.kt` |
| Shell and navigation | `app/MainShell.kt`, `app/MainShellActions.kt`, `app/navigation/MainNavigation.kt` |
| Server accounts and tokens | `feature/server/`, `data/server/`, `data/auth/`, `core/storage/` |
| Browsing and resource resolution | `feature/home/`, `feature/search/`, `feature/library/`, `feature/detail/`; `data/history/`, `data/media/`, `data/search/` |
| Playback | `core/player/`, `feature/player/` |
| Image and text reading | `core/reader/`, `feature/reader/`, `data/reader/` |
| Settings | `core/model/TvSettings.kt`, `feature/settings/`, `data/settings/` |
| HTTP and dependency injection | `core/network/KaloscopeApi.kt`, `core/network/ApiClientFactory.kt`, `core/network/NetworkCall.kt`, `app/di/AppModule.kt` |
| Shared UI | `core/designsystem/`, `app/KaloscopeTheme.kt`, `app/src/main/res/` |

Keep the package boundaries: `app` wires the shell, bootstrap, navigation, and
DI; `core` holds shared models, storage, networking, design system, and playback
and reading policies; `data` implements repositories, mapping, and persistence;
`feature` owns screen UI, ViewModels, and coordinators. Add feature DTOs under
`data/<area>/remote/`; shared envelopes and some existing API DTOs live in
`KaloscopeApi.kt`. Do not move those merely to satisfy a layering preference.
UI must not call Retrofit or DataStore directly. Communicate between features
through models, callbacks, routes, and IDs, not another screen's internals.

Established stack:

- One `app` module, one Activity, Kotlin, Java 17, minimum API 23, and
  application ID/namespace/root package `org.kaloscope.tv`.
- Jetpack Compose and TV Material, Navigation 3 with serializable route keys,
  Hilt, StateFlow, and testable coordinators for complex state transitions.
- Retrofit, OkHttp, Kotlinx Serialization, Preferences DataStore, Android
  Keystore token encryption, Coil, Media3 (ExoPlayer, MediaSession, HLS, DASH,
  Compose UI), and AkDanmaku.

Read `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, and
`gradle/wrapper/gradle-wrapper.properties` for current SDK, plugin, and dependency
versions. Use the wrapper and version catalog. The build uses AGP's built-in
Kotlin and `com.android.legacy-kapt`; preserve the documented `BuildConfig`,
`kotlin-metadata-jvm` kapt dependency, and in-process compiler workarounds unless
replacing them is in scope and the replacement is verified.

Do not introduce Leanback UI, Fragment/XML primary UI, Room, another HTTP or DI
framework, a service locator, multiple modules, an event bus, or WebView product
flows without explicit approval and a clear migration need. The manifest's
Leanback TV feature and launcher declarations are not the Leanback UI toolkit.

## Product and navigation invariants

- Server setup/login and the authenticated shell are mutually exclusive root
  states. Saved servers retain separate tokens. Authentication failure clears
  the affected session; ordinary network failures and forbidden access do not.
  Keep nested feature errors covered by `app/RootStateInspection.kt`.
- Home shows only `video` watch history. Network video and reader content must
  not create local `MediaItem` history.
- Search selects the first available real indexer and automatically searches
  when a keyword is not required. Indexer configuration determines available
  filters and request fields; preserve `IndexerSearchRequestFactory` mapping.
- `SearchCoordinator` resolves a result through `NetworkResourceRepository`:
  video opens Player; image/text opens Reader. Do not insert a network detail
  screen or assume every search result is video.
- Library selects the first real library on initial load. Library media cards
  open media detail before playback.
- Settings is available from the main shell and Back returns to its caller.
  Preserve the saved start-page selection in `MainNavigation.kt`.
- `PlayerRoute` and `ReaderRoute` carry only `requestId`. Their in-memory request
  stores hold payloads scoped to a server; never put tokens, DTOs, media URLs, or
  manifests in routes or saved navigation state. Handle missing or wrong-server
  requests explicitly, remove requests on close, and clear them on server exit.

## State, concurrency, and settings

- Each screen ViewModel exposes one primary immutable `uiState`; keep it thin
  when a coordinator owns transitions. Represent loading, empty, content, error,
  and retry behavior explicitly. Retain existing content on refresh, pagination,
  or reader chapter failure, with a separate operation error.
- ViewModels must not retain Activity, navigation/focus controllers, ExoPlayer,
  MediaSession, Surface, or other UI/runtime objects.
- Cancel jobs when the server, indexer, request, or chapter changes. Preserve
  generation checks where cancellation alone cannot prevent stale completions.
  Always rethrow `CancellationException` after required cleanup.
- Preserve focused business-object IDs and `GridViewportSnapshot` when returning
  from detail, Player, or Reader. Reset them when the query or data source
  changes, not on ordinary recomposition. Use stable IDs for lazy layout keys.
- Keep pending navigation in state and consume it by request ID, as Search does;
  do not add one-shot event wrappers or a global navigation event bus.
- `SettingsCoordinator` serializes/coalesces writes and restores the last saved
  settings on failure. Preserve existing preference keys, defaults, validation,
  and migrations in `PreferencesSettingsRepository`.
- Player subtitle/danmaku appearance changes use the dedicated preference
  setters; temporary enable toggles, subtitle selection/offset, and playback
  speed must not overwrite global defaults. Reader image/text setting callbacks
  update both current reader state and the settings repository.

## TV UI and Kotlin conventions

- All primary flows work with D-pad directions, Center, and Back. Prefer natural
  two-dimensional focus traversal; add `focusProperties` only for a reproduced
  navigation problem. Reserve `FocusRequester` for initial/restored focus and
  modal traps.
- Reuse `core/designsystem` controls, side panels, dialogs, loading layouts,
  colors, motion, and layout tokens. Custom focusable controls need semantics,
  a visible focused state, and a disabled state. Allow space for focus scaling
  so cards and indicators are not clipped.
- Loading overlays block covered controls. Dialogs and drawers contain focus
  and restore it to their trigger. After data removal or replacement, focus a
  stable nearby item. Verify initial focus, all four directions, modal behavior,
  and Back restoration for focus changes.
- Keep reader Back handling layered: choice dialog, drawer, controls, then
  reader exit. Preserve equivalent player overlay/exit policies instead of
  adding competing Back handlers.
- Use four-space Kotlin formatting, immutable data classes, and sealed state
  and policy models. Avoid `!!`; explain an unavoidable invariant in English.
  Do not swallow exceptions or leave empty catches; use `AppResult`/`AppError`.
- Collect state with lifecycle-aware APIs. Launch Composable work through
  lifecycle/effect APIs. Reusable Composables take data and callbacks rather
  than obtaining screen ViewModels.
- Put user-facing text in `app/src/main/res/values/strings.xml` (preview examples
  may be local). Keep comments and KDoc in English and explain intent or
  constraints. Avoid vague `Utils`/`Helpers`/`Manager` containers; introduce an
  interface only for a substitutable boundary or multiple implementations.

## Networking and security

- `ServerUrlNormalizer` accepts HTTP(S) origins, rejects credentials, paths,
  queries, and fragments, and normalizes host/scheme/trailing slashes.
  `ApiClientFactory` builds `<origin>/_api/`. Connection redirects must follow
  `ServerConnectionOriginPolicy`: only a same-host HTTP-to-HTTPS upgrade may
  change the chosen origin.
- Authorization uses `Token <token>`. Bind API calls to the selected origin;
  use `OriginAuthPolicy` for absolute resource URLs (scheme, host, effective
  port). Never send a Kaloscope token to third-party URLs, including redirect
  targets.
- Preserve authorization for same-origin images, media, subtitles, manifests,
  and segments. Reuse `ServerImageResolver`/`ServerImage` and
  `ReaderImageRequestFactory`; do not attach a global token to Coil requests.
- Login is form-encoded. Ordinary JSON uses `ApiEnvelope`/`NullableApiEnvelope`
  and the existing `dataOrThrow` boundary. Do not envelope-decode redirects,
  empty responses, streams, HLS, DASH, VTT, or images. Preserve unknown-field
  tolerance and nullable workflow resource fields.
- Tokens reach DataStore only through `SecureSessionStore` encryption and stay
  keyed by server ID. Preserve disabled backups and the backup-rule resources.
  Debug form defaults may come from ignored `local.properties`; release defaults
  remain empty. Never copy these values into source, output, or test fixtures.
- Local HTTP support currently comes from `android:usesCleartextTraffic="true"`
  in `app/src/main/AndroidManifest.xml`. Preserve support for user-managed local
  servers; never disable certificate/hostname validation or weaken HTTPS.

## Playback and reading

- Local Auto starts direct and may fall back once to HLS only for classified
  source/decoder failures. Direct never silently transcodes; Transcode requests
  HLS immediately. Keep this in `PlaybackSourcePolicy` and its failure/fallback
  policies.
- Network video uses resolved indexer sources, definitions, and chapters; it
  must not call local-media transcoding APIs. Preserve codec-aware definition
  selection and matching Media3 source types. Resolve inline DASH API base URLs
  before encoding the manifest as a data URI in `PlaybackSourceResolver`.
- `PlayerScreen` owns the screen-scoped `PlaybackController`, which owns and
  releases ExoPlayer and MediaSession with the screen lifecycle. Record final
  progress before controller release. Local progress is periodic and also
  recorded at pause, seek, item change, exit, and error boundaries; network
  playback is excluded. Danmaku uses milliseconds and resynchronizes after seek
  and episode changes.
- Keep source, buffering, timeout/stall, subtitle, chapter, progress, settings,
  key, and overlay decisions in existing policy/coordinator classes where
  practical. Device behavior still needs device verification.
- Reader loads image/text requests through `ReaderCoordinator` and
  `ReaderContentLoader`; network chapter and image-page resolution belongs in
  `DefaultNetworkResourceRepository`. Preserve content and position on chapter
  failure, deduplicate appended images, and discard stale chapter/page results.
- Reuse reader chapter/key/scroll/preload policies. Image preloading is bounded
  to the next image and uses the same authenticated request factory as display.
  Keep text-setting bounds and units consistent with `ReaderSettingsPolicy`,
  `TextReaderDimensions`, and persisted preference migrations.

## Verification

Add or update tests at the layer matching changed behavior and run the smallest
relevant checks. JVM tests use JUnit, coroutines-test, and MockWebServer; Compose
UI and golden tests run on Android. Full regression is not the default.

| Change | Verification entry point |
| --- | --- |
| Policies, mapping, coordinator transitions, persistence | Matching classes under `app/src/test/java/org/kaloscope/tv/` with lightweight fakes |
| HTTP routes, encoding, envelopes, headers, errors | `core/network/KaloscopeApiContractTest.kt` and repository tests under the JVM test root; fixtures in `app/src/test/resources/fixtures/api/` |
| Navigation, clicks, remote keys, focus, modals | Matching Compose UI tests under `app/src/androidTest/java/org/kaloscope/tv/` |
| Visual changes | `app/src/androidTest/java/org/kaloscope/tv/test/golden/` and `app/src/androidTest/assets/goldens/`; reuse `test/DeviceScreenshot.kt` capture helpers |
| Media3, image loading, performance | Relevant device tests and an available Android TV smoke test |

Run a JVM class or a small set of classes from the repository root, for example:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'org.kaloscope.tv.feature.search.SearchCoordinatorTest' \
  --tests 'org.kaloscope.tv.feature.reader.ReaderCoordinatorTest'
```

For already installed matching app/test APKs, run only the relevant device test:

```bash
adb -s "$tv_serial" shell am instrument -w \
  -e class org.kaloscope.tv.feature.reader.ReaderScreenTest \
  org.kaloscope.tv.test/androidx.test.runner.AndroidJUnitRunner
```

Select `tv_serial` from `adb devices`; do not hardcode a developer's device.
Use `ClassName#methodName` to narrow instrumentation further. Inspect test
results for failures and confirm tests actually ran; command exit alone is not
proof. DTO changes require corresponding fixture and parsing/contract coverage.

Full unit suites, lint, APK builds, and unfiltered connected tests require the
user's explicit permission after explaining which checks are needed and why.
This includes `:app:testDebugUnitTest` without `--tests`, `:app:lintDebug`,
`:app:lintRelease`, `:app:assembleDebug`, `:app:assembleRelease`, and unfiltered
`:app:connectedDebugAndroidTest`. Existing authorization for named checks need
not be repeated. Documentation-only edits need no Android build unless they
change build instructions or make claims requiring build verification.

### Preserve device state and golden baselines

- Treat the existing authenticated installation and device/AVD state as
  persistent. Reuse it for changes unrelated to setup, login, session handling,
  bootstrap, or logout. Do not log out or repeat login for unrelated work.
- Never use `adb uninstall`, `adb shell pm clear`, Gradle uninstall tasks,
  emulator `-wipe-data`, factory reset, AVD delete/recreate, or a clean snapshot
  as an ordinary test step. If a rebuilt APK is needed, update with
  `adb install -r`; otherwise relaunch the installed build.
- For required clean-state auth coverage, prefer a separate device/AVD/snapshot.
  Do not extract or copy credentials, tokens, server addresses, or other private
  app data to preserve a session. Batch deterministic remote input where safe;
  capture meaningful checkpoints instead of recreating unrelated login flows.
- `scripts/verify-tv-goldens.sh` builds/installs APKs and checks 720p, 1080p, and
  4K at density 320/font scale 1.0. `scripts/update-tv-goldens.sh` additionally
  requires API 28 and replaces baseline assets. Both require exactly one
  connected device and fall under the build permission rule above.
- Both scripts reset size/density overrides on exit rather than restoring prior
  overrides. Record and restore existing display overrides when reusing a
  persistent device. Regenerate baselines only for an intended visual change;
  inspect actual/diff images instead of updating goldens to hide a failure.

## Release and handoff

- `app/build.gradle.kts` leaves release unsigned if all four signing variables
  are absent, fails fast for partial configuration, and signs only when
  `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and
  `ANDROID_KEY_PASSWORD` are all supplied.
- `.github/workflows/release.yml` publishes only from pushed version tags
  matching its trigger. Before release, increment `versionCode` and match
  `versionName` to the tag without `v`. Preserve unit tests, release lint, signed
  assembly, and `apksigner` verification as publication gates.
- Keep `ANDROID_KEYSTORE_BASE64` only in the GitHub `release` Environment's
  secrets; restore under `${{ runner.temp }}` and delete with `always()` cleanup.
  Never print, cache, commit, or upload signing material. Publish the versioned
  APK and SHA-256 checksum; retain the R8 mapping as a workflow artifact.
- Pin third-party Actions to full commit SHAs and use minimum permissions.
  Do not create/push release tags or publish a release without explicit approval.
- Do not commit, push, create a branch, or open a PR unless explicitly asked.
  Stage only requested files. Review `git diff --check` and the final diff;
  exclude unrelated edits, generated output (except intended golden assets),
  local configuration, unexplained TODOs, placeholders, and skipped tests.
- Report checks actually executed and any unavailable command/device or
  unverified behavior. Never claim a check passed unless it ran successfully.
- After each feature or fix, suggest an English Conventional Commit message:
  `<type>[scope]: <description>`. Allowed types: `feat`, `fix`, `build`, `bump`,
  `chore`, `ci`, `docs`, `perf`, `refactor`, `revert`, `style`, `test`. Keep the
  description within 50 characters, no trailing period; add a body only for
  substantial changes. Do not run `git commit` unless asked.
