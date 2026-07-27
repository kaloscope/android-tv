# Kaloscope Android TV Agent Guide

## 1. Project identity

This repository contains the native Android TV client for
[kaloscope/kaloscope](https://github.com/kaloscope/kaloscope).

The client connects to a user-managed Kaloscope server. Keep the repository
portable and safe to publish:

- Do not depend on sibling repositories, absolute filesystem paths, local
  accounts, saved browser sessions, private servers, or machine-specific tools.
- Do not assume that a Kaloscope server, Android device, emulator, or signing
  key is available.
- Never add credentials, tokens, cookies, server addresses, media paths,
  keystores, or local configuration to source control.
- When server behavior must be verified, use the public upstream repository,
  published documentation, fixtures, or information supplied by the user. If
  the contract still cannot be verified, report the uncertainty instead of
  inventing fields or endpoints.

For the current client implementation, prefer evidence in this order:

1. The user's explicit requirements for the task.
2. Tests and production code in this repository.
3. Gradle configuration and the version catalog.
4. The public Kaloscope server source and documentation.
5. Prototype or example content, for visual reference only.

## 2. Working in the repository

Before changing code:

1. Run `git status --short` and preserve all existing user changes.
2. Use `rg` to inspect the implementation, tests, callers, and related
   resources.
3. Check the relevant Gradle configuration before changing dependencies or
   platform assumptions.
4. For API work, verify routes, request models, response models, and the
   server's actual behavior against public upstream sources.
5. Reduce the task to the smallest complete vertical slice.

Avoid speculative abstractions, batches of empty screens or repositories, and
unrelated refactors. Follow established patterns unless the task explicitly
requires a deliberate architectural change.

## 3. Current architecture and stack

Treat these as established project constraints unless the user explicitly asks
to change them:

- One Android application module: `app`.
- Kotlin with a Java 17 toolchain.
- Application ID, namespace, and root package: `org.kaloscope.tv`.
- Minimum Android version: API 23.
- Single-activity Jetpack Compose UI using Compose for TV and TV Material.
- Navigation 3 with serializable route keys.
- Hilt for dependency injection.
- StateFlow and immutable UI state exposed by ViewModels.
- Testable coordinator classes for non-trivial feature state transitions.
- Retrofit, OkHttp, and Kotlinx Serialization for server communication.
- Preferences DataStore and Android Keystore-backed token storage.
- Coil for images.
- Media3 ExoPlayer, MediaSession, HLS, DASH, and Compose player UI.
- JUnit, coroutines-test, MockWebServer, Compose UI tests, screenshot tests, and
  targeted device tests.

Keep the existing package boundaries:

- `app`: application shell, bootstrap, dependency injection, and navigation.
- `core`: shared models, networking, storage, design system, and playback
  policies.
- `data`: repositories, remote DTOs, mapping, and persistence adapters.
- `feature`: screen UI, ViewModels, and feature coordinators.

DTOs belong in the remote data layer. UI code must not call Retrofit or
DataStore directly. Feature packages communicate through stable models, route
keys, and IDs rather than by sharing screen internals.

Do not introduce Leanback, Fragment/XML-based primary UI, Room, another HTTP
client, another dependency injection framework, a service locator, multiple
Gradle modules, an event bus, WebView-based product flows, or disabled TLS
validation without explicit approval and a clear migration need.

## 4. Established product behavior

Preserve these client behaviors unless the task explicitly changes the product:

- Release code displays real server data. Fixtures, previews, and sample data
  are limited to tests and previews.
- Search selects the first available real indexer. If it does not require a
  keyword, the initial search runs automatically.
- Selecting a network search result resolves its details and opens the player;
  it does not create a separate network detail screen.
- Library selection uses real server libraries and selects the first available
  library on initial load.
- Selecting a library media card opens the media detail screen before playback.
- Recent watching uses only `video` media history.
- Network search playback must not be recorded as local `MediaItem` history.
- Settings remain a root-level destination available from the main shell.
- Login/server setup and the authenticated main shell are mutually exclusive
  root states.
- Player navigation passes only a playback `requestId`, never a token, DTO,
  media URL, or manifest.

Do not add unrelated product features while completing a focused task.

## 5. State, navigation, and concurrency

- Each screen-level ViewModel exposes one primary `uiState`.
- Model loading, content, empty, and error states explicitly.
- Preserve existing content when a refresh or pagination request fails.
- Keep ViewModels thin when a coordinator already owns feature transitions.
- ViewModels must not retain an Activity, NavController, FocusRequester,
  ExoPlayer, MediaSession, Surface, or other UI/runtime object.
- Cancel origin-scoped or source-scoped work when the active server or data
  source changes, so stale results cannot replace current state.
- Always rethrow `CancellationException` after any necessary state cleanup.
- Use stable IDs in routes and lazy layout keys.
- Preserve list/grid viewport and focused business-object IDs when returning
  from detail or player screens.
- Do not introduce one-shot event wrappers or a global navigation event bus.

## 6. TV interaction and focus

Every primary flow must work with D-pad directions, Center, and Back only.

- Prefer natural two-dimensional focus traversal.
- Add `focusProperties` only after a real navigation problem is reproduced.
- Use `FocusRequester` only for initial focus, focus restoration, and modal
  focus traps.
- Focusable custom components require semantics, visible focused state, and a
  disabled state.
- Loading overlays must prevent interaction with covered controls.
- Drawers and dialogs must contain focus and restore it to the triggering
  control when dismissed.
- After deleting, filtering, paging, or refreshing content, move focus to a
  stable nearby item instead of the screen root.
- For focus changes, verify initial focus, all four directions, modal behavior,
  and Back restoration.

Use Compose UI tests for route, click, key handling, and focus behavior. Use a
connected TV device or emulator for behavior that cannot be validated reliably
on the JVM.

## 7. Networking and security

- Normalize a user-provided server to its origin and build the API base URL as
  `<origin>/_api/`.
- Send the Kaloscope authorization token only to the matching server origin.
- Never attach that token to third-party absolute image or playback URLs.
- Attach authorization to same-origin API media, proxied images, subtitles,
  manifests, and segments when required by the server contract.
- Login uses form encoding; do not silently change it to JSON.
- Parse ordinary JSON responses through the server envelope.
- Do not envelope-decode redirects, empty responses, media streams, HLS, DASH,
  VTT, or image payloads.
- Keep Kotlinx Serialization tolerant of unknown fields.
- Treat optional workflow resource fields as nullable.
- Clear the active session on authentication failure, not on ordinary network
  failure.
- Never log authorization headers, cookies, passwords, tokens, full sensitive
  URLs, or response bodies containing private data.
- Support permitted local-network HTTP through the declared Android network
  security policy. Never implement `trustAllCerts`, disable hostname
  verification, or weaken HTTPS validation.

Tests and examples must use obviously synthetic origins, credentials, IDs, and
media paths.

## 8. Playback

- Local Auto mode prefers direct playback and falls back to HLS only for
  classified, recoverable source or decoder failures.
- Direct mode must not silently transcode.
- Transcode mode requests HLS directly.
- Network playback uses the source returned by indexer details and does not call
  local-media transcoding APIs.
- Use the matching Media3 source module for HLS, DASH, and progressive media.
- Resolve an inline DASH manifest against its same-origin base URL before
  converting it to a data URI.
- Keep playback source selection, failure classification, fallback, buffering,
  subtitles, chapters, settings, and progress rules in testable Kotlin policy
  classes where practical.
- The player owns ExoPlayer and MediaSession through its screen-scoped playback
  controller and releases both with the screen lifecycle.
- Record local playback progress periodically and at important lifecycle
  boundaries, including immediately before release.
- Danmaku timing uses milliseconds and must be resynchronized after seek or
  episode changes.

Player controls must remain fully operable by remote. Verify key handling,
overlays, focus traps, error recovery, and state restoration.

## 9. Kotlin and Compose conventions

- Follow official Kotlin formatting with four-space indentation.
- Use immutable data classes and sealed interfaces for state and policy models.
- Avoid `!!`. If a compiler-unprovable invariant makes it unavoidable, explain
  that invariant with a short English comment.
- Do not swallow exceptions or leave empty `catch` blocks.
- Map failures through the project's `AppResult` and `AppError` boundary.
- Do not launch uncontrolled work from a Composable body.
- Collect screen state with lifecycle-aware APIs.
- Reusable Composables receive data and callbacks; they do not obtain a screen
  ViewModel directly.
- Put user-facing text in string resources. Preview-only examples may remain
  local to previews.
- Keep comments and KDoc in English. Explain intent, constraints, trade-offs, or
  non-obvious behavior, not syntax.
- Update or remove comments when behavior changes.
- Do not create vague `Utils`, `Helpers`, or `Manager` containers.
- Add interfaces only at boundaries that need substitution in tests or have
  more than one meaningful implementation.

## 10. Testing strategy

Match tests to the changed behavior:

- Pure policies, mappers, URL rules, and coordinator transitions: JVM unit
  tests.
- Repository behavior and DTO mapping: JVM tests with fakes and canonical
  fixtures.
- HTTP paths, encoding, envelopes, headers, and error mapping: MockWebServer
  contract tests.
- Navigation, clicks, TV keys, focus, drawers, and dialogs: Compose UI tests.
- Visual regressions: existing golden screenshot infrastructure.
- Media3 integration and device-specific focus/performance: emulator or real TV
  smoke tests when such a target is available.

Prefer lightweight fakes over heavyweight mocking for state tests. Fixtures may
exist only under test, androidTest, or preview source paths. When a DTO changes,
update the corresponding fixture and parsing/contract tests.

For code changes, run only the relevant targeted tests by default. Do not
automatically run full regression commands such as:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

When a change is large or cross-cutting enough to justify full regression
testing, explain which full checks are recommended and why, then obtain the
user's explicit permission before running them.

### Persistent device and emulator state

When an emulator or real Android TV device is available, treat its existing
logged-in app installation as a persistent test environment:

- Preserve application data, the authenticated session, and device or AVD state
  by default. Reuse the existing installation instead of setting up and logging
  in again for every test run.
- Never run `adb uninstall`, `adb shell pm clear`, Gradle uninstall tasks,
  emulator `-wipe-data`, an AVD factory reset, or an AVD delete/recreate as part
  of the ordinary test workflow. Do not replace the logged-in state with a
  clean snapshot.
- When changed code requires a new APK, update the installed app in place with
  a data-preserving operation such as `adb install -r`, then relaunch it. Do not
  uninstall the old APK first. If the required build is already installed,
  relaunch the app without reinstalling it.
- Changes outside server setup, login, authentication, token or session
  storage, authenticated bootstrap, and logout must be tested from the existing
  authenticated state. Do not log out, clear app data, or repeat the login flow
  for unrelated business logic changes.
- A fresh unauthenticated state and a repeated login flow are needed only when
  the changed behavior directly concerns login or session handling, or when the
  task explicitly requires clean-state coverage. Prefer a separate clean AVD,
  device, or snapshot for those tests so the persistent logged-in environment
  remains intact.
- Do not reproduce an unrelated login manually through a long sequence of
  individual ADB commands and screenshots. Batch deterministic remote-control
  input where safe, and capture screenshots only at meaningful validation
  checkpoints.
- Preserving a session does not permit extracting, displaying, logging, or
  copying its token, credentials, server address, or other private data.

If a command or required device is unavailable, state that clearly. Never claim
that a check passed unless it was actually executed successfully.

Documentation-only changes do not require an Android build unless they change
build instructions or make claims that need build verification.

## 11. Completion criteria

A change is complete only when:

- It follows the established product behavior or documents the approved change.
- Production paths use real repositories and contain no test data.
- API behavior is verified rather than guessed.
- Loading, empty, error, retry, and retained-content behavior are handled where
  applicable.
- Remote-control-only interaction and focus restoration remain correct.
- Tokens and private server data cannot cross origins or enter logs/source
  control.
- New or changed behavior has tests at the appropriate layer.
- Relevant verification commands were run and their results were reviewed.
- There are no unexplained TODOs, placeholders, skipped tests, or empty catches.
- The final diff contains no generated artifacts or unrelated user changes.

## 12. Git workflow

- Never discard, overwrite, or reformat unrelated user changes.
- Do not run `git commit`, push, create a branch, or open a pull request unless
  the user explicitly asks.
- Stage only files within the requested scope.
- Do not commit build output, local configuration, credentials, private server
  details, or media data.
- After each feature or bug fix, suggest an English Conventional Commit message:

```text
<type>[scope]: <description>
```

Use one of `feat`, `fix`, `build`, `bump`, `chore`, `ci`, `docs`, `perf`,
`refactor`, `revert`, `style`, or `test`. Keep the description within 50
characters and omit the trailing period. Add a body only for substantial
changes.
