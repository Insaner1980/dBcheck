# dBcheck - Codex Code Review Questions


# 1. Build, dependencies & Gradle configuration

## 1.1 Version catalog consistency

**Review question:** In `gradle/libs.versions.toml`, `app/build.gradle.kts`,
`build.gradle.kts` and `gradle/wrapper/gradle-wrapper.properties`, confirm the
declared versions are internally consistent and that nothing references a version
alias that isn't defined (Kotlin 2.3.20, AGP 9.1.0, Gradle wrapper 9.4.1, Compose
BOM 2026.03.00, KSP 2.3.6, Room 2.8.4, Hilt 2.59.2). Flag only real mismatches
(e.g. a hardcoded version that contradicts the catalog), not cosmetic ordering.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 1.2 KSP vs Kotlin compatibility

**Review question:** KSP is pinned at 2.3.6 while Kotlin is 2.3.20. Verify this
KSP version is actually compatible with this Kotlin version for Room and Hilt
annotation processing, and that the Compose compiler plugin is sourced from the
Kotlin plugin (not a separate stale version). Only flag a genuine incompatibility
you can substantiate.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 1.3 Dependency locking & pinned transitives

**Review question:** Dependency locking is enabled at the root `allprojects` level
and `app/build.gradle.kts` pins several transitive build/scanner dependencies
(e.g. WorkManager 2.11.2, Guava 33.6.0-android) to satisfy security-check. Confirm
these pins are still needed (i.e. they actually resolve a real conflict or CVE
constraint) and that none of them force a version that breaks a direct dependency.
Don't recommend removing a pin unless you can show it's inert.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 1.4 Sentry is debug-only

**Review question:** Verify that Sentry Android Core (8.43.1) is wired only into
the debug source set / classpath and is genuinely a no-op in release - i.e. there
is no `io.sentry` reference reachable from a release build, no Sentry Gradle
plugin, and no release crash reporting. Confirm `DbCheckApplication.onCreate()`'s
`SentryInit` path is source-set specific.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 1.5 minSdk / targetSdk / compileSdk

**Review question:** With `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`,
confirm there are no API calls used unconditionally that require an API level
above 26 without a version guard (especially around foreground service types,
notification channels, `POST_NOTIFICATIONS`, and any API-33+/34+ behavior).
Report only concrete unguarded calls, with the API level they require.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 2. Architecture & layering boundaries

## 2.1 Domain layer purity

**Review question:** The contract is that `domain/` never imports `data/`,
`service/`, `sync/`, `ui/`, `billing/` or `widget/`. Scan every file under
`domain/` and confirm there are no imports that violate this, and no Android
framework imports leaking into pure-domain classes (e.g. `android.media.*`,
`androidx.room.*`, `android.content.Context`). List only real violations.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 2.2 No Room entities above the data layer

**Review question:** UI, widget and service code must not handle Room entities
directly - repositories and service ports map data/sync models into domain /
report / UI-facing models. Check `ui/`, `widget/` and `service/` for any direct
references to Room `@Entity` types or DAOs, and confirm mapping happens in the
repository layer. Flag only genuine leaks.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 2.3 Single source for database name

**Review question:** `DbCheckDatabase.DATABASE_NAME` is meant to be the single
source of the Room database filename, referenced by the Room builder,
`LocalBackupManager` and backup tests. Confirm there is no second place that
hardcodes `"dbcheck.db"` (or similar) independently, which could drift from the
constant. Report each independent hardcode if present.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 2.4 FileProvider authority/path ownership

**Review question:** `ExportFileCache` is meant to own the FileProvider authority
suffix and the `cache/exports/` directory names, and `file_paths.xml` additionally
exposes the app-private `files/wav_recordings/` path only for WAV sharing. Confirm
manifest authority, `file_paths.xml`, the `ExportFileCache` constants and runtime
usage all agree, with no divergent authority string or path literal elsewhere.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 2.5 Report data single source

**Review question:** `domain/report/SessionReportCalculator` is meant to be the
one calculation source feeding Session Detail UI, PDF export, PNG share and Health
Connect notes. Confirm none of those four consumers recompute equivalent level,
LCpeak, TWA/dose or peak events independently in a way that could diverge from the
calculator. Flag any parallel computation you actually find.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 3. Dependency injection (Hilt)

## 3.1 Scope correctness

**Review question:** Review the Hilt modules (`AppModule`, `DatabaseModule`,
`BillingModule`, `SyncModule`, `CoroutineDispatchers`). Confirm singletons that
must be process-wide (Room database, DataStore, `BillingManager`,
`ProFeatureManager`, `AudioEngine`/`AudioSessionManager` if singleton-scoped) are
`@Singleton`, and that nothing accidentally creates multiple instances of a
stateful component that is assumed to be shared. Report only real scoping defects.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 3.2 Dispatcher qualifiers

**Review question:** Dispatchers are provided via Hilt qualifiers
`@DefaultDispatcher`, `@IoDispatcher`, `@MainDispatcher` from `AppModule`. Confirm
no class hardcodes `Dispatchers.IO`/`Default`/`Main` directly where it should
inject the qualified dispatcher (which would break test substitution), and that
the qualifiers are bound to the correct dispatchers. Flag only real hardcodes that
matter for testability or correctness.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 3.3 Context injection

**Review question:** Check that `@ApplicationContext` is used wherever a Context is
injected for a singleton, and that no singleton captures an Activity context
(leak risk). Confirm `BillingManager`, file/cache helpers, notification helpers and
DataStore use the application context. Report only genuine leak-prone injections.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 3.4 Interface bindings

**Review question:** Several seams are interface/implementation pairs (e.g.
`BillingGateway`/`BillingManager`, `SoundClassifier`/`MediaPipeSoundClassifier`,
`SessionLocationCapturePort`, `AudioInputDeviceDiscoveryPort`,
`BackupGateway`/`LocalBackupManager`). Confirm each interface is actually bound to
its production implementation in a Hilt module (`@Binds`/`@Provides`) and that
production code depends on the interface, not the concrete class. Flag any missing
or wrong binding.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 4. Coroutines, Flow & concurrency

## 4.1 AudioSessionManager mutex coverage

**Review question:** `AudioSessionManager` uses `Mutex`(es) for session lifecycle
and measurement flush, and stop/completion is supposed to wait for an in-progress
flush. Verify there is no path where start, stop, reset, recover and flush can
interleave to corrupt session state - e.g. a stop that proceeds while a flush
holds the lock, or a recover that runs concurrently with a live session. Describe
any concrete race you can trace, otherwise confirm the locking is sound.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 4.2 Flow collection lifecycle

**Review question:** In ViewModels collecting hot flows (Analytics, History,
Meter, Settings), confirm collection happens in `viewModelScope` and that
`stateIn`/`SharingStarted` choices won't drop or duplicate emissions, and that
nothing collects a flow on the main dispatcher doing heavy work. Flag only
collections that can actually leak, block, or miss state.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 4.3 StateFlow vs SharedFlow for events

**Review question:** Events that should fire once (completed-session navigation via
`completedSessionIds`, `SettingsEvent.RestartAfterRestore`, share results,
metadata error) must not be modeled as replayable state that re-triggers on
recomposition or config change. Confirm one-shot events use an appropriate channel
/ SharedFlow / consumed-state pattern and won't double-navigate or double-restart.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 4.4 Cancellation & cleanup

**Review question:** When a measurement session, hearing test, ambient playback or
passive monitoring is cancelled (scope cancelled, screen left, service stopped),
confirm native resources (AudioRecord, AudioTrack, TFLite classifier) are released
in a `finally` / `withContext(NonCancellable)` where needed, so cancellation
doesn't leak a recorder or leave a half-written file. Report only real leak paths.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 4.5 Blocking calls on the wrong dispatcher

**Review question:** Look for blocking I/O on `Dispatchers.Main` or
`@DefaultDispatcher`: file copies in backup/restore, PDF rendering, CSV paging,
WAV header rewrites, FileProvider operations, Room synchronous queries. Confirm
each runs on `@IoDispatcher`. Flag only real misplacements that could cause jank
or ANR.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 5. Audio capture & AudioRecord lifecycle

## 5.1 Permission gate before recording

**Review question:** `AudioEngine` checks the `RECORD_AUDIO` permission before
constructing/starting `AudioRecord` and annotates with `@RequiresPermission`.
Confirm there is no code path (passive monitoring start, camera-overlay live
readout, recovery, etc.) that can reach `AudioRecord.startRecording()` without the
permission actually being granted at runtime. Report concrete unguarded paths.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 5.2 Buffer sizing

**Review question:** `AudioRecordPolicies` centralizes buffer sizing and read
interpretation, with the capture buffer larger than the PCM16 read chunk
(`CHUNK_SIZE = 4096`). Confirm the chosen buffer size is at least
`AudioRecord.getMinBufferSize(...)` for 44100 Hz / mono / PCM16, that short reads
are handled (read can return fewer samples than requested), and that a
`getMinBufferSize` error code (e.g. `ERROR_BAD_VALUE`) is handled, not used as a
size. Flag only real defects.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 5.3 Start confirmation contract

**Review question:** `AudioSessionManager.startSession()` should return `true` only
after `AudioEngine.startRecording(...)` has actually put AudioRecord into recording
state and emitted `onRecordingStarted`. Verify there's no early `true` return
before the recorder is confirmed running, and that an AudioRecord that initializes
but fails to start (`recordingState != RECORDSTATE_RECORDING`) is treated as a
failure. Report only a real gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 5.4 Recorder release on every exit path

**Review question:** Confirm `AudioRecord` is `stop()`-ed and `release()`-d on
every exit: normal stop, failure during start, exception mid-read, session reset,
and process-recovery completion. A recorder left un-released blocks the mic for the
whole device. Trace each exit and report any path that can skip release.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 5.5 Sound-detection fanout is live-only

**Review question:** `SoundDetectionWindowFanout` feeds YAMNet windows from
`AudioEngine`'s live raw audio only when `isProUser && soundDetectionEnabled`, and
`AudioEngine` itself must not run classifier inference or persist raw audio.
Confirm the fanout is strictly live, that toggling the flag off mid-session stops
fanout cleanly, and that no raw PCM/float window is written anywhere.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 6. Acoustic / DSP math

## 6.1 Decibel reference and offset

**Review question:** `DecibelCalculator` maps RMS/peak to dB with reference
`32768.0`, a `+90` offset, the calibration offset, and a clamp to 0-130 dB.
Verify the dB formula is `20*log10(rms/reference) + 90 + calibrationOffset` (or
equivalent), that log of zero/near-zero RMS is guarded (no `-Infinity`/`NaN`
reaching the clamp), and that the clamp order relative to the offset is intentional.
Report only a real numerical defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.2 Weighted signal stays double precision

**Review question:** `FrequencyWeightingFilter` (A/B/C/Z/ITU-R 468) is a set of
44.1 kHz SOS/biquad cascades, and the weighted signal is supposed to stay a
`DoubleArray` through to dB calculation so positive gains don't clip into the
PCM16 range. Confirm there's no intermediate cast back to `Short`/PCM16 before the
RMS, and that filter state (per-biquad delays) is preserved across chunks and reset
when weighting changes. Flag only real precision/state issues.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.3 Raw vs weighted vs C-peak separation

**Review question:** `AudioEngine.DecibelReading` carries raw RMS (`instantDb`),
the selected-weighting RMS (`weightedDb`) and a C-weighted peak (`peakDb`). Verify
reports and the dosimeter never use raw RMS where a weighted value is required:
LCpeak must be C-weighted, the A-weighted event/TWA path must use the A-weighted
value, and `SessionStats.avgDb`/`minDb`/`maxDb` must be weighted while `peakDb` is
C-weighted LCpeak. Report any place these are crossed.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.4 Energy averaging (Leq), not arithmetic mean

**Review question:** `SessionStats.avgDb` and the daily/weekly "energy-average"
analytics should average sound *energy* (i.e. average of 10^(L/10) then convert
back to dB), not the arithmetic mean of dB values. Confirm `AudioSessionManager`,
`MeasurementRepository.getDailyAveragesLast7Days()` and
`ExposureAnalyticsCalculator` all use energy averaging consistently. Flag any
arithmetic-mean-of-dB that should be energy-based.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.5 FFT correctness

**Review question:** `FFTProcessor` is a 4096-point radix-2 FFT with a Hann window
and DC-bin skip for dominant-frequency search. Confirm the Hann window is applied
before the transform, the magnitude/normalization is consistent, the bin→frequency
mapping uses `binIndex * sampleRate / fftSize`, and the dominant-frequency search
ignores DC (and ideally Nyquist mirror). Report only a real FFT defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.6 Octave/third-octave band math

**Review question:** `OctaveBandRtaCalculator` uses IEC/ANSI base-10 fractional-octave formulas for octave and third-octave center frequencies and band edges, aggregates FFT magnitudes per band, can read `OctaveCalibrationOffsets`, and normalizes to the strongest calibrated band. Verify that the implementation distinguishes the octave ratio from the per-band step for a fractional octave, so a third-octave sequence uses the appropriate fractional power between adjacent third-octave centers rather than treating the whole octave ratio as the adjacent-band step. Confirm the edge multipliers, per-band FFT aggregation, calibration-offset application and the live-only zero-offset-until-profile-wired behavior. Flag only genuine mathematical or dataflow errors.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.7 Dosimeter exchange rate & TWA

**Review question:** `DosimeterCalculator` computes NIOSH REL and OSHA PEL TWA,
dose, projected dose and remaining exposure time. Confirm the exchange rates and
criterion levels are correct and not swapped (NIOSH: 85 dBA criterion, 3 dB
exchange; OSHA: 90 dBA criterion, 5 dB exchange), that the 8-hour TWA and dose
percentage formulas are right, and that "A-weighted only" is enforced so non-A
sessions show absent (not zero) values. Report only a real mismatch against the
standard.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 6.8 YAMNet window adapter resampling

**Review question:** `YamnetAudioWindowAdapter` converts a 44.1 kHz PCM16 chunk
stream into 16 kHz float windows for YAMNet without persisting raw audio. Confirm
the resampling/decimation from 44100→16000 is handled correctly (it's a
non-integer ratio), the float normalization range matches what YAMNet expects
(typically -1..1), and window length/overlap match the model's expected input.
Flag only a real correctness issue.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 7. Session orchestration (AudioSessionManager)

## 7.1 Live exposure state source

**Review question:** `AudioSessionManager.liveExposureState` updates from each
`DecibelReading.aWeightedDb`, computes A-weighted LAeq and reads NIOSH/OSHA TWA,
dose, projected dose and remaining time from `DosimeterCalculator`. Confirm it
feeds the calculator A-weighted values (not raw/selected-weighting), and that the
running LAeq accumulation is energy-based and reset per session. Report only a real
defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 7.2 Measurement-mode toggle is UI-only

**Review question:** `MeterViewModel.setMeasurementMode(...)` should only change UI
state (`DB_METER` vs `DOSIMETER`) and must not start/stop measurement. Confirm
toggling mode never touches AudioRecord, the foreground service, or Room cadence.
Flag any side effect on the actual measurement.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 7.3 Completion navigation only on normal stop

**Review question:** `completedSessionIds` should drive Session Detail navigation
only after a normal stop. Reset and failure paths must finalize silently with no
auto-navigation, and `EXTRA_EMIT_COMPLETED=false` from
`MeasurementForegroundService.stopIntent(...)` must suppress completion emission.
Confirm there's no path where a reset or failure still emits a completed id and
navigates. Report only a real leak of the completion event.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 7.4 Refresh rate throttles UI only

**Review question:** `MeterRefreshRate` (HIGH 100ms / STANDARD 250ms / LOW 1000ms)
must throttle only Meter UI updates and must not affect the 44.1 kHz sample rate,
4096 chunk, weighting filter state or the 1s Room persistence cadence. Confirm the
throttle is applied at the UI/state layer downstream of capture and persistence.
Flag any leakage into capture/persistence.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 7.5 Single active session invariant

**Review question:** The schema enforces one active session slot (`activeSlot`,
`MIGRATION_1_2` closes extras). Confirm `AudioSessionManager.startSession()` cannot
create a second concurrent active session if one is already active (e.g. double-tap
Play, service restart racing with recovery), and that `createActiveSession` plus
recovery cannot both open a slot. Report only a real way to end up with two active
sessions.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 8. Foreground service & process lifecycle

## 8.1 startForeground before capture

**Review question:** `MeasurementForegroundService` must call
`ServiceCompat.startForeground(...)` first and only start
`AudioSessionManager.startSession()` if foreground promotion succeeds. Confirm
there is no path where AudioRecord starts before `startForeground`, and that a
`ForegroundServiceStartNotAllowedException` / promotion failure cleanly aborts
without leaving a recorder running. Report only a real ordering or failure-handling
gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 8.2 Foreground service type alignment

**Review question:** The mic service declares `foregroundServiceType="microphone"`
with `FOREGROUND_SERVICE_MICROPHONE`, and ambient playback uses a separate
`mediaPlayback` service with `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. Confirm the
`startForeground(..., type)` argument matches the manifest type for each service,
the matching runtime permission is declared, and the two services never share a
type. Flag only a real manifest/runtime mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 8.3 START_NOT_STICKY semantics

**Review question:** The service returns `START_NOT_STICKY` even on successful
start, so a killed process is not auto-revived into recording. Confirm
`onStartCommand` handles a null/redelivered intent safely (no NPE, no accidental
auto-start) and that the stop action and passive-monitoring action are
distinguished by action/extras, not by intent presence alone. Report only a real
defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 8.4 Interrupted-session recovery is silent

**Review question:** `AudioSessionManager.recoverInterruptedSession()` (called at
startup) should finalize a leftover active session silently from persisted
measurement rows, with no auto-navigation and no false completion event. Confirm a
recovered session with zero persisted rows is handled (closed/cleaned, not crashed
or left active), and recovery never races a freshly started session. Report only a
real gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 8.5 Restart-after-restore flow

**Review question:** Restore restarts the app via `AlarmManager` + immutable
`PendingIntent` + `finishAffinity()` + `Process.killProcess(...)` because the closed
Room instance can't be safely reused in-process. Confirm the `PendingIntent` flags
are correct for the target API (immutability), the alarm is short and one-shot, and
there's no window where the app keeps running against the swapped DB file. Report
only a real restart defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 8.6 MainActivity theme bootstrap timing

**Review question:** `MainActivity` waits for the first `UserPreferences` emission
before drawing `DbCheckTheme`/`DbCheckNavHost` to avoid a saved-theme flash.
Confirm this wait can't deadlock or hang the splash if DataStore is slow/empty
(there is a default emission), and that `onResume()`'s
`BillingManager.refreshPurchases()` doesn't run before billing is connected in a
way that throws. Report only a real timing defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

# 9. Measurement persistence & sampler

## 9.1 Forced-persist boundaries

**Review question:** `MeasurementPersistenceSampler` persists at a fixed 1s cadence
but forces persistence for: the first reading, the 85 dB / `NoiseLevel.ELEVATED.maxDb`
boundary crossing, a new weighted max, a new LCpeak max, and the final unsaved
reading at stop. Confirm each forced trigger fires exactly when intended (e.g. the
boundary crossing detects both up and the documented direction, the "new max"
comparisons use the right weighted/peak fields), and that the final-reading flush
can't be lost if stop races the 1s tick. Report only a real miss.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 9.2 Measurement + summary in one transaction

**Review question:** `SessionRepository.recordActiveSessionMeasurements(...)` and
`completeSessionWithMeasurements(...)` are supposed to write measurement rows and
the session summary in a single Room transaction. Confirm both are genuinely
`@Transaction` (or wrapped in `withTransaction`) so a crash can't leave rows
without an updated summary or a closed session without its final rows. Report only
a real non-atomic path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 9.3 Summary aggregation correctness

**Review question:** Confirm the running session summary (`minDb`, `avgDb`, `maxDb`,
`peakDb`) is computed from the correct fields - weighted for min/avg/max, C-weighted
LCpeak for peak - and that `avgDb` is energy-averaged across all persisted readings,
matching what `SessionReportCalculator` later recomputes for the report. Flag any
inconsistency between the live summary and the report calculation.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 10.2 Migration SQL correctness

**Review question:** Review each migration's SQL (`MIGRATION_1_2` through `MIGRATION_11_12`) for: columns added with correct types/nullability, NOT NULL columns given a valid DEFAULT (e.g. `calibration_profiles.octaveBandOffsets` TEXT NOT NULL default empty string), documented backfills applied (e.g. `peakDb` from `dbWeighted`, `aWeightedDb = dbWeighted`, `responseTime = FAST`), and new indices created. Flag only a real SQL defect that would fail or corrupt a migration.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 10.3 Foreign keys & cascades

**Review question:** Confirm the FK/cascade contracts hold: `sound_detection_events`→`sessions.id` cascade, `sleep_sessions`→`sessions.id` cascade, `sleep_notable_events`→`sleep_sessions.sessionId`, `hearing_recovery_results`→`hearing_test_results.id` cascade, and that `passive_monitoring_samples` intentionally has no session FK. Verify the entity `@ForeignKey(onDelete = CASCADE)` declarations match the migration SQL. Report only a real mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 10.4 Indices & deterministic ordering

**Review question:** DAO queries are supposed to have deterministic `ORDER BY` tie-breakers using the primary key in addition to a timestamp, and the migrations create indices for export/session/deletion queries. Confirm the heavy queries (history, export paging, analytics windows) are backed by an index and that every list query has a deterministic order. Flag only a real missing tie-breaker or unindexed hot query.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 10.5 BackupDatabaseValidator hash list

**Review question:** `BackupDatabaseValidator` must accept the Room identity hash for every supported schema version (v8..v12 hashes are listed). Confirm the validator's supported-hash set covers all versions a user could realistically restore from, and that an unknown/newer hash is rejected rather than silently accepted. Report only a real omission.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 10.6 DAO query parameterization

**Review question:** Confirm all DAO `@Query` strings are parameterized (`:param`) with no string concatenation of user input, especially `SessionDao.searchSessions(...)` used by History search. Verify `LIKE` filters escape/handle wildcards as intended and that nullable filter arguments are handled (a null filter means "no filter", not "match null"). Flag only a real injection or null-filter defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 10.7 Export paging queries

**Review question:** `MeasurementDao.getMeasurementsForSessionExportPage(...)` and `SoundDetectionEventDao.getEventsForSessionExportPage(...)` page rows so export doesn't load everything into memory. Confirm the paging uses a stable order + limit/offset (or keyset) that can't skip or duplicate rows between pages, and that the loop terminates. Report only a real paging correctness bug.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 11. DataStore preferences & defaults

## 11.1 Effective Pro measurement values

**Review question:** Pro measurement values (calibration, frequency weighting, response time, dosimeter standard, input device) are read as *effective* values through `ProAudioPreferencePolicy`, so a Free user's previously stored Pro settings must not affect the measurement path. Confirm every measurement consumer reads the effective value, not the raw stored preference. Flag any path that reads the raw preference directly.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 11.2 Defaults & normalization

**Review question:** `UserPreferenceDefaults` centralizes defaults and normalization. Confirm enum-like preferences parse unknown/legacy stored strings to a safe default (no exception), numeric preferences are clamped to valid ranges, and reading a never-written key returns the default rather than crashing. Report only a real default/parse gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 11.3 is_pro_user write discipline

**Review question:** `BillingManager.isPurchased` starts `null`, and `ProFeatureManager` must sync only a confirmed `true`/`false` to the `is_pro_user` DataStore key so a startup or billing-query error can't overwrite a previously stored Pro entitlement back to Free. Confirm no code writes `false` on an unknown/error state. Report only a real overwrite path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 11.4 Notification schedule encoding

**Review question:** `NoiseNotificationSchedule` persists active days as ISO-8601 `DayOfWeek.value` and hours as minute-of-day, where equal start/end means the whole day and start>end crosses midnight (the after-midnight part belongs to the previous active day). Confirm the encode/decode round-trips and the overnight window logic matches that rule. Flag only a real off-by-one or window defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 11.5 New-key default safety

**Review question:** For every DataStore key listed in the preferences section, confirm a missing key yields the documented default and that adding a key didn't require a data migration that's absent. Report only a real case where a missing key would crash or yield a wrong effective value.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 12. Billing & Pro entitlement

## 12.1 Confirmed-only entitlement sync

**Review question:** Confirm `ProFeatureManager` only promotes to Pro on a confirmed PURCHASED+acknowledged state, and that `UserPreferences.isProUser` (the effective value for UI and domain policies) reflects `ProEntitlementPolicy`, not a raw or optimistic flag. Report only a real way to unlock Pro without a confirmed purchase.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 12.2 Acknowledge on refresh/reconnect

**Review question:** `BillingManager.refreshPurchases()` handles startup/resume snapshots and PURCHASED purchases are acknowledged when needed, including on the reconnect/refresh path. Confirm an unacknowledged PURCHASED purchase is always acknowledged (Play auto-refunds unacknowledged purchases after 3 days), with no path that grants entitlement but skips acknowledge. Report only a real missed-acknowledge path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 12.3 PENDING does not unlock

**Review question:** `PurchaseState.PENDING` must not unlock Pro. Confirm there is no branch where a pending purchase is treated as entitlement, and that a pending purchase that later completes is picked up on resume/refresh. Flag only a real premature unlock.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 12.4 ITEM_ALREADY_OWNED handling

**Review question:** `ITEM_ALREADY_OWNED` should trigger a purchase-snapshot fetch so the token and any missing acknowledge are handled. Confirm this path actually re-queries purchases and reconciles entitlement rather than just showing an error. Report only a real gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 12.5 PurchaseEvent coverage

**Review question:** `PurchaseEvent` has `Completed`, `Pending`, `Cancelled`, `AlreadyOwned`, `Failed(reason)`. Confirm the Settings purchase flow handles each case with a resourced user message (no raw billing response text) and that a `Failed` reason is surfaced without unlocking Pro. Flag only a real unhandled case.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 12.6 Debug force-free isolation

**Review question:** `ProEntitlementPolicy` is the single effective-entitlement source: release uses purchase state; debug is Pro by default unless `debugForceFreeEnabled`. Confirm the debug force-free toggle and debug-default-Pro behavior cannot affect a release build. Report only a real leak of debug entitlement logic into release.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 12.7 Product id constant

**Review question:** The single INAPP product id `dbcheck_pro` should be one constant referenced by the query and the purchase flow. Confirm it isn't hardcoded in two places that could diverge, and that the product-details query uses it. Flag only a real divergence.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 13. Pro gating coverage (UI vs execution vs data)

## 13.1 Hearing test execution gate

**Review question:** The hearing-test setup screen does not gate Pro, but execution must be blocked in `ActiveTestViewModel` for Free users and `HearingTestService.saveCompletedTest(...)` must re-check Pro before saving. Confirm a Free user cannot run or save a test even via deep link. Report only a real bypass.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.2 Recovery save gate + baseline

**Review question:** `HearingRecoveryService.saveCompletedRecoveryCheck(...)` requires Pro and a latest full hearing-test baseline, failing with a resourced baseline-required message otherwise. Confirm both checks exist and a missing baseline can't produce a saved/garbage result. Flag only a real gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.3 Export & settings execution gates

**Review question:** Confirm CSV export, PDF export, session metadata writes, and Pro audio settings (calibration, weighting, response time, dosimeter standard, input device) are gated at the execution/data layer, not only by hiding UI. A Free user reaching the action via state restoration or deep link must still be blocked. Report only a real UI-only gate.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.4 Pro feature execution gates

**Review question:** Confirm sound detection, WAV recording, ambient playback, tinnitus pitch preview/save, and voice baseline / TTS prompt all enforce Pro at execution (not just UI). Each should refuse to run its core action for a Free user. Flag only a real bypass.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.5 History/Session Detail window lock

**Review question:** Free users are limited to a 7-day window (`SessionHistoryPolicy.FREE_HISTORY_WINDOW_MILLIS`) in both History and a direct Session Detail open of an old session. Confirm the lock is applied on the data/read path (not just list filtering) so a deep link to an old `sessionId` is also locked for Free. Report only a real way for Free to view older data.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.6 Locked previews vs real data across Pro surfaces

**Review question:** Free users must receive locked-preview or locked/empty effective states for Pro-only features instead of real Pro data covered by an overlay. Confirm this policy holds across Pro analytics, widget state, lock-screen meter, Session Detail Pro sections, hearing recovery, tinnitus pitch, ambient playback and other Pro-only surfaces. Flag only a real data leak where Free code computes, emits or exposes real Pro-only data that should have been replaced with a locked/effective-empty state.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.7 Widget & lockscreen gates

**Review question:** The Glance widget and the lock-screen live meter are Pro-only. Confirm the widget shows a locked state for Free (no real session data leaked into widget state) and that public lock-screen visibility requires Pro + both `lockscreen_meter` and `show_lockscreen_meter_publicly`. Report only a real leak.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 13.8 Deep-link redirect to Settings Pro

**Review question:** Non-top-level Pro routes (`sleep/setup`, `ambient/playback`, `tinnitus/pitch`, hearing recovery) should redirect Free/deep-link entry to the Settings Pro card via the execution gate. Confirm the redirect happens before any Pro work runs. Flag only a real unguarded entry.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 14. Hearing test (procedure & scoring)

## 14.1 Hughson-Westlake step direction

**Review question:** In `HearingTestProcedure`, verify the Hughson-Westlake-style step directions and values are exactly as intended: classically down 10 dB after a response and up 5 dB after no response. Do not flag the 10 dB down / 5 dB up asymmetry merely because it is asymmetric; only flag swapped direction, wrong constants, termination logic that can hang, or threshold behavior that biases results. Report the actual step values you find and whether they match the intended procedure; if they match, say so plainly.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 14.2 Amplitude gate & threshold rule

**Review question:** Confirm the tone amplitude is clamped to the valid output range (no values below the floor or above the ceiling that would silently saturate or mute), and that the threshold-acceptance rule (e.g. responses at a level a set number of times) is implemented as designed. Verify a frequency that never gets a response terminates with a sane "not heard" threshold rather than looping. Flag only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 14.3 Score formula direction

**Review question:** Verify the overall-score formula is not inverted: better hearing (lower thresholds) must yield a higher/better score and rating. Trace the mapping from thresholds → `overallScore`/`rating` and confirm the sign/direction. If it's correct, confirm that explicitly; only report an inversion you can demonstrate.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 14.4 Audiogram axis orientation

**Review question:** In the results audiogram, a standard audiogram plots lower (better) thresholds higher on the chart (inverted Y). Verify the Y-axis orientation and the threshold-to-pixel mapping render in the conventional direction and aren't flipped. Report only a real orientation bug, citing the Canvas mapping.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 14.5 Tone generation accuracy

**Review question:** `ToneGenerator` uses AudioTrack MODE_STATIC with a 50 ms fade in/out. Confirm the generated sine frequency matches the requested test frequency at 44.1 kHz (no off-by-one in the phase increment), the fade prevents clicks, and the buffer is sized/looped correctly for the tone duration. Flag only a real synthesis defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 14.6 Result load order

**Review question:** The results screen loads the `hearing_test/results/{testId}` argument first and falls back to `getLatestResult()` only if the argument is missing. Confirm a present-but-not-found `testId` shows a missing-result state rather than silently showing the latest (which would mislead). Report only a real fallback defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 14.7 Relative-level disclaimer

**Review question:** Results are relative app tone-output / dBFS levels, not calibrated dB HL. Confirm the UI/PDF/share text presents this as personal tracking with the non-clinical disclaimer and makes no diagnostic claim. Flag only a real missing/over-claiming statement.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 15. Hearing recovery

## 15.1 Baseline requirement

**Review question:** `HearingRecoveryCalculator` compares against the latest full hearing-test baseline. Confirm the baseline used is actually the most recent full test (not a previous recovery check), and that a missing baseline blocks the check with the resourced message. Report only a real baseline-selection bug.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 15.2 Frequency subset

**Review question:** Recovery mode (`HearingTestMode.RECOVERY`) tests only 1/4/8 kHz for both ears. Confirm the procedure restricts to exactly those frequencies and both ears, and that the calculator only matches ear/frequency pairs present in both baseline and recovery. Flag only a real mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 15.3 Shift status thresholds

**Review question:** Confirm `averageShiftDb`/`maxShiftDb` are computed over matching ear/frequency deltas and the `STABLE`/`SMALL_SHIFT`/`ELEVATED_SHIFT` status thresholds are applied with correct comparison directions (a larger positive shift = worse). Report only a real threshold/sign defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 15.4 Aggregate-only persistence

**Review question:** `HearingRecoveryRepository` persists only the aggregate result to `hearing_recovery_results` (no new tone-audio or clinical audiometry data). Confirm only aggregate shift fields + ear shift data are stored and nothing writes raw audio or per-tone clinical detail. Flag only a real over-persistence.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 16. Calibration

## 16.1 Offset clamp range

**Review question:** `CalibrationOffsetPolicy` is the shared +/-10 dB clamp/default source for the flat mic-sensitivity offset and octave-band offsets. Confirm both flat and per-band offsets are clamped to the same documented range everywhere they're applied, with no path that applies an unclamped stored value. Report only a real unclamped path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 16.2 Octave offset codec round-trip

**Review question:** `OctaveCalibrationOffsets` defines the supported center-frequency list and a deterministic Room TEXT codec. Confirm encode→decode round-trips exactly (order, separators, missing bands default to zero) and a malformed stored string decodes to a safe zero profile rather than crashing. Flag only a real codec defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 16.3 Last default profile protection

**Review question:** `CalibrationProfileRepository` must prevent deleting the last `isDefault` profile at the data layer. Confirm deletion of the final default is rejected in the repository (not only disabled in UI). Report only a real way to delete it.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 16.4 Selected-profile fallback

**Review question:** When the user deletes the currently selected profile, the selection should fall back to a valid profile, and the ViewModel bootstraps a `Device default` profile only after the first Room emission. Confirm there's no window where the selected id points to a deleted profile and measurement reads a null/garbage offset. Flag only a real dangling-selection bug.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 16.5 Free user calibration ineffective

**Review question:** A Free user's stored calibration must not affect measurement (effective offset zero via `ProAudioPreferencePolicy`). Confirm `DecibelCalculator`'s applied offset is zero for Free regardless of stored profile. Report only a real leak of calibration into the Free measurement path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 17. Analytics

## 17.1 Selection persistence across emissions

**Review question:** `AnalyticsViewModel` keeps `AnalyticsSection`, `AnalyticsOverviewRange` and `SpectralMode` in its own state sources and republishes them in `AnalyticsUiState.Success` so data emissions / recomposition don't reset the user's selection to default. Confirm a new data emission preserves the current selections. Flag only a real reset.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 17.2 Analytics locked previews don't compute real datasets

**Review question:** Within `AnalyticsViewModel` and Analytics UI models, confirm Free `LockedPreview` states for spectral, environment mix, monthly trend and yearly report do not carry real computed Pro values and do not trigger the heavy Pro-only dataset computation for Free users. This question is specifically about Analytics implementation, not the whole app-wide Pro preview policy. Report only a real Analytics data leak or unnecessary Free-user computation.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 17.3 Spectrogram buffer bounds

**Review question:** `SpectrogramBuffer` keeps at most 60 rows, skips duplicate timestamps, and a null frame clears the live buffer. Confirm the cap is enforced (old rows dropped, no unbounded growth), duplicates are actually skipped, and clearing on null frame can't leave a stale waterfall. Flag only a real bounds/clear defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 17.4 Environment mix window

**Review question:** `MeasurementRepository.getEnvironmentMixLast7Days()` feeds the Pro Environment Mix. Confirm the 7-day window boundary is computed consistently (inclusive/exclusive) and counts map to the right categories, with a Free locked preview. Report only a real window/category defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 17.5 Trend/report windows on tick

**Review question:** 30-day trend and 12-month report from `ExposureAnalyticsCalculator` update on a minute tick. Confirm each window's start is recomputed against current time (so it slides correctly) and the tick doesn't recompute heavy work more often than needed. Flag only a real windowing or performance defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 17.6 Health status thresholds

**Review question:** `AnalyticsViewModel` derives `SAFE`/`WARNING`/`DANGER` and a today-vs-week percent. Confirm the thresholds and the percent formula (divide-by-zero when week total is zero) are correct and match the documented bands. Report only a real threshold/formula bug.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 17.7 Analytics error mapping

**Review question:** An analytics load failure maps to `AnalyticsUiState.Error` showing a resourced fallback message and a CTA to the Meter. Confirm the error path doesn't show raw exception text and doesn't get stuck (a later successful emission recovers). Flag only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 18. History & search

## 18.1 Free window enforcement

**Review question:** History uses raw-all queries plus a local 7-day clamp for Free, while `SessionRepository.getFilteredSessions(...)` keeps the Free lower bound and gives Pro full history. Confirm both the search path and the plain listing enforce the Free window, so Free can't see older sessions through search filters. Report only a real bypass.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 18.2 Search filter mapping

**Review question:** `getFilteredSessions(SessionHistoryQuery)` maps name/tag/date/avg dB/weighting/location filters to `SessionDao.searchSessions(...)`. Confirm each filter maps to the intended SQL predicate, combined filters AND correctly, an empty query returns the (windowed) full list, and ordering is `startTime DESC, id DESC`. Flag only a real mapping defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 18.3 Metadata save gate

**Review question:** `HistoryViewModel.saveSessionMetadata(...)` must not write metadata unless the user is Pro, and a metadata save error is shown as a separate `metadataErrorMessage` inside successful History content. Confirm the Pro check precedes the write and the error doesn't replace the whole screen. Report only a real gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 18.4 Metadata normalization

**Review question:** `SessionMetadata` normalizes name, emoji, tags and export slug: tags limited to six, 24 chars each, case-insensitive de-dup. Confirm the limits and de-dup are applied on save (not just display) and the slug is filesystem-safe. Flag only a real normalization defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 19. Session Detail & reporting

## 19.1 Arg & unavailable states

**Review question:** `SessionDetailViewModel` reads `sessionId` from `SavedStateHandle`. Confirm missing-session and Free-locked states are explicit unavailable states (not a crash or an empty report), and a non-existent id shows missing rather than the latest. Report only a real state defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 19.2 Weighting label

**Review question:** `equivalentLevelLabelForWeighting(...)` distinguishes A/B/C/Z/ITU-R 468 in report text. Confirm the label matches the session's actual `frequencyWeighting` and that LAeq vs Leq labeling is correct per weighting. Flag only a real label mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 19.3 A-weighted-only dosimetry

**Review question:** NIOSH 8h TWA, NIOSH dose and the 85 dBA peak-event list are available only for A-weighted sessions; other weightings must show the values as absent (N/A), not zero. Confirm the report and Data Availability page render N/A for non-A sessions rather than 0. Report only a real zero-instead-of-absent bug.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 19.4 Heart-rate overlay gating

**Review question:** The heart-rate overlay loads only when Pro + setting on + Health Connect available + `READ_HEART_RATE` granted. Confirm all four conditions are required and a denied permission degrades gracefully (no crash, overlay simply absent). Flag only a real gating gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 20. Export & sharing (PDF / PNG / CSV / WAV)

## 20.1 PDF page composition

**Review question:** `ExportPdfReportUseCase` writes 5 pages normally and 6 with `ReportHeartRateSection.enabled`. Confirm the page set (summary, metrics, data availability, time series, peak events, optional heart rate) is produced in order, each page is added once, and the optional page is only added when enabled. Report only a real page-composition bug.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.2 Data Availability N/A semantics

**Review question:** The Data Availability page shows only ready upstream data and renders missing sources as `N/A`, not zeros (location, A-weighted NIOSH standard, projected dose, persisted sound detection, octave breakdown). Confirm missing data is N/A and the octave breakdown stays N/A unless `octaveBreakdownAvailable` or non-zero offsets indicate context. Flag only a real zero-instead-of-N/A case.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.3 PNG share grant

**Review question:** PNG shares write to `cache/exports/` and publish a FileProvider `content://` URI with a temporary read grant via both `EXTRA_STREAM` and `ClipData` / `FLAG_GRANT_READ_URI_PERMISSION`. Confirm the grant is present on the intent (some targets read only ClipData) and the URI is from the FileProvider, not a raw file path. Report only a real grant/URI defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.4 CSV file set & paging

**Review question:** `ExportCsvUseCase` writes three CSVs (session summary, measurements, optional sound detection), reading measurements/events in pages, shared via `ACTION_SEND_MULTIPLE` + FileProvider URIs. Confirm all three are attached, paging is correct (no skipped/duplicated rows), and the optional file is omitted when empty. Flag only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.5 CSV columns

**Review question:** CSV columns include metadata (`session_name`, `session_emoji`, `session_tags`), measurements include `peak_db`, and the sound-detection export includes only aggregated `timestamp`, `label`, `confidence` (no raw audio). Confirm CSV escaping handles commas/quotes/newlines in names/tags. Report only a real escaping or column defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.6 WAV writer condition & headers

**Review question:** The streaming PCM16 WAV writer starts only when `isProUser && wavRecordingDefaultEnabled`, writes to app-private `filesDir/wav_recordings`, updates RIFF/data headers on normal stop, and deletes the partial file on failure/cleanup. Confirm the header byte counts are correct after stop, the effective condition is enforced, and an abnormal stop can't leave a corrupt header. Flag only a real WAV defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.7 WAV share/delete

**Review question:** WAV share builds an `audio/wav` Sharesheet intent from a FileProvider `content://` URI with `ClipData` + temporary read grant; delete removes the session's WAV from app-private storage; the file is never copied to MediaStore. Confirm share uses only the WAV FileProvider root and delete actually removes the file. Report only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 20.8 Native resource closing in export

**Review question:** Confirm `PdfDocument` is closed in a `finally`, all output/input streams and the bitmap used for PNG encoding are closed/recycled, and a FileProvider write failure cleans up the partial file. Flag only a real unclosed-resource or leftover-file path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 21. FileProvider & sharing security

## 21.1 Exposed paths only

**Review question:** `file_paths.xml` should expose only `cache/exports/` and the app-private `files/wav_recordings/` path. Confirm no broader root (whole `files/`, external storage, cache root) is exposed that would let a share grant access to unrelated app data. Report only a real over-exposed path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 21.2 Temp grant lifecycle

**Review question:** Confirm share intents use temporary read grants (not persistable) and don't broadly `grantUriPermission` to all packages indefinitely. Verify the provider is `exported=false` with `grantUriPermissions=true`. Flag only a real over-broad/persistent grant.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 21.3 Stale file cleanup

**Review question:** `ExportFileCache` deletes export/share files older than 24h on the next export/share. Confirm the cleanup actually runs on those operations and only targets the export cache directory (never user data or backups). Report only a real cleanup defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 22. Health Connect

## 22.1 Availability check

**Review question:** Availability is checked via `HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")`. Confirm an unavailable/needs-update status is surfaced as a resourced status (not a crash), and feature paths no-op when unavailable. Flag only a real handling gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 22.2 Permission sets

**Review question:** Confirm noise sync requests `getWritePermission(ExerciseSessionRecord::class)` and heart rate requests `getReadPermission(HeartRateRecord::class)`, matching the manifest health permissions, with no broader health permission requested. Report only a real mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 22.3 Noise write conditions

**Review question:** `writeNoiseDose(...)` runs only on normal completion and only when `healthConnectEnabled`. Confirm the `ExerciseSessionRecord` payload is correct (`EXERCISE_TYPE_OTHER_WORKOUT`, `clientRecordId = noise_dose_<date>_session_<id>`, `RECORDING_METHOD_ACTIVELY_RECORDED`, notes from `SessionReportData`) and a write failure doesn't crash the completion path. Flag only a real payload/condition defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 22.4 Hearing test no-op

**Review question:** `writeHearingTestResult(...)` intentionally returns `Skipped` because no native audiometry record exists. Confirm it truly no-ops (doesn't write a misleading exercise/other record for hearing tests). Report only a real unintended write.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 22.5 Heart-rate read mapping

**Review question:** `readHeartRateForSession(...)` maps samples into Session Detail UI state and the PDF heart-rate section, only when granted. Confirm timestamp alignment to the session window and graceful handling of empty/denied results. Flag only a real mapping defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 22.6 Manage/install intents

**Review question:** Settings opens Health Connect's Play page via `market://details?id=com.google.android.apps.healthdata` when missing/outdated, and Manage via `getHealthConnectManageDataIntent(...)`. Confirm both intents are guarded for resolvability (no crash if no handler). Report only a real unguarded intent.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 23. Local backup & restore

## 23.1 WAL checkpoint before copy

**Review question:** Backup runs `PRAGMA wal_checkpoint(TRUNCATE)` before copying `dbcheck.db`. Confirm the checkpoint completes before the copy and the copy captures a consistent file (no concurrent writes during copy, given the mutex and active-measurement block). Flag only a real consistency defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 23.2 Restore validation order

**Review question:** Restore validates the chosen backup (including identity hash in the supported set) before replacing the current DB, and also creates and validates a `dBcheck_pre_restore_*` safety backup before replacement. Confirm validation precedes any destructive replace and a failed validation aborts without touching the live DB. Report only a real ordering defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 23.3 Sidecar cleanup

**Review question:** Restore deletes stale `dbcheck.db-wal` and `dbcheck.db-shm` sidecars before swapping in the replacement DB file. Confirm both sidecars are removed (a leftover WAL against a new DB file corrupts state). Flag only a real missing sidecar deletion.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 23.4 Serialization & blocking

**Review question:** Backup/restore are serialized with a `Mutex` and blocked during active measurement. Confirm there's no path to start a backup/restore while a measurement (or another backup/restore) is running. Report only a real concurrency hole.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 23.5 Restart emission

**Review question:** A successful restore emits `SettingsEvent.RestartAfterRestore`, handled by `MainActivity` with a process restart. Confirm the event is one-shot (no double restart) and the app doesn't continue using the old closed Room instance before restart. Flag only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 24. Widget (Glance)

## 24.1 Receiver & update interval

**Review question:** `DbCheckWidgetReceiver` is `exported=false` and `widget_info.xml` sets a 30-min update interval. Confirm the receiver isn't exported and the provider XML is valid. Report only a real config defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 24.2 State branches

**Review question:** Confirm the widget shows: Pro + session data → last session avg dB / noise label / relative time; Pro + no data → empty state; Free → Pro-locked; read failure → widget error state. Verify Free never receives real session data in widget state. Flag only a real branch/leak defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 24.3 Refresh triggers

**Review question:** The widget refreshes on session completion and on Pro-entitlement change (after the first emission). Confirm both triggers fire and the Pro-change refresh doesn't fire spuriously on the initial emission. Report only a real refresh defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 24.4 Off-main data read

**Review question:** Glance widget data reads (preferences + session) should run off the main thread and map to UI-safe models (no Room entity leakage into Glance). Confirm the read is on an IO context and handles failure. Flag only a real threading/leakage defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 25. Notifications & alerts

## 25.1 Channel creation

**Review question:** Channels `measurement_channel`, `alerts_channel`, `ambient_playback_channel` should be created idempotently with correct importance (ambient low + ongoing + private; alerts for exposure/peak/voice). Confirm creation is safe to call repeatedly and importance/visibility match intent. Report only a real channel defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 25.2 Lockscreen visibility policy

**Review question:** `NotificationPrivacyPolicy.measurementLockscreenVisibility(...)` returns public only when Pro + `lockscreenMeterEnabled` + `showLockscreenMeterPublicly`; otherwise `VISIBILITY_PRIVATE`. Confirm all three conditions are required for public and the default is private. Flag only a real policy defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 25.3 Custom RemoteViews scope

**Review question:** Custom collapsed/expanded `RemoteViews` (current dB, peak dB, duration, noise-level dot) are used only for Pro + lockscreen meter; Free or disabled uses the standard private notification. Confirm the branch and that RemoteViews fields are bound safely (no missing id NPE). Report only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 25.4 Schedule respected

**Review question:** `NoiseAlertEvaluator` must respect `NoiseNotificationSchedule` (active days/hours, overnight window) before attempting an exposure or peak alert. Confirm the schedule gate precedes alert delivery and the overnight boundary is applied. Flag only a real schedule-bypass.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 25.5 Alert dedup & cooldown

**Review question:** `NoiseAlertPolicy` owns the 30-min threshold-average rule, 100% actual/projected dose triggers, 120 dB peak limit and a 30-min retry cooldown; a successfully delivered alert type doesn't repeat in a session, while a failed delivery may retry after cooldown. Confirm per-type, per-session dedup and that a failed delivery is retried (not permanently suppressed). Report only a real dedup/cooldown defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 25.6 POST_NOTIFICATIONS handling

**Review question:** On Android 13+, `POST_NOTIFICATIONS` is requested at measurement start when needed. Confirm a denied notification permission doesn't break the foreground service start (the service still runs; the notification may be suppressed by the OS) and there's no crash. Flag only a real handling gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 26. Sleep Monitor

## 26.1 One-to-one sleep metadata

**Review question:** `sleep_sessions` is one-to-one with `sessions.id` and cascades on parent delete. Confirm sleep metadata is keyed to a created normal session id and a session delete removes its sleep rows. Report only a real keying/cascade defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 26.2 Notable events FK

**Review question:** `sleep_notable_events` references `sleep_sessions.sessionId`, so notable events can't be attached to a non-sleep session without a sleep metadata row. Confirm the FK prevents orphan notable events. Flag only a real FK gap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 26.3 Keep-awake scope

**Review question:** Sleep setup uses the shared `KeepScreenOnEffect`/`KeepScreenOnController` only under `isRecording && keepAwakeEnabled` (no `PowerManager.WakeLock`). Confirm the flag is cleared on stop/dispose and isn't held when not recording. Report only a real flag-leak.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 26.4 Setup state & start

**Review question:** Confirm target durations (6h/8h/10h) and the keep-awake option map to the recording config, and Sleep start writes `sleep_sessions` metadata for the created session id, with the Free/deep-link path redirected to Settings Pro via `SleepSetupViewModel`. Flag only a real config/gate defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 27. Passive monitoring

## 27.1 User-started foreground only

**Review question:** Passive monitoring is a user-started short foreground-service sample from the Settings Noise Notifications card, requesting the mic permission on user action and starting `MeasurementForegroundService.startPassiveMonitoringIntent(...)` with an ongoing Stop action. Confirm it can't start from boot/alarm/receiver/WorkManager or any background trigger. Report only a real background-start path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 27.2 No session/measurement/raw audio

**Review question:** `MeasurementRecordingMode.Passive` must not call `AudioSessionManager.startSession()`, create a session, write `measurements` rows, persist raw audio, or trigger WAV/sound-detection/spectral/alarm/voice paths. `PassiveMonitoringManager` keeps runtime stats in memory and persists only an aggregate sample on stop. Flag any path that creates a session/measurement or starts those subsystems.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 27.3 Aggregate-only persistence

**Review question:** `PassiveMonitoringRepository.recordSample(...)` writes only aggregate fields to `passive_monitoring_samples` (no sessionId, no raw audio/PCM/YAMNet window). Confirm the table and writes contain only aggregates and `observeDailySummary(...)` derives the summary from them. Report only a real over-persistence.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 27.4 Clear history coverage

**Review question:** Clear history must also remove passive monitoring summaries. Confirm `clearHistory` deletes `passive_monitoring_samples` while not deleting `filesDir/backups`. Flag only a real coverage defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 28. Ambient sound playback

## 28.1 Service isolation

**Review question:** `AmbientSoundPlaybackService` is a separate `mediaPlayback` foreground service with its own `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission and low-importance channel, using no `MeasurementForegroundService`, no `RECORD_AUDIO`, no mic type, no Room, no playback history, no raw audio, no cloud/Health Connect. Confirm none of those couplings exist. Report only a real coupling.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 28.2 Audio focus & timer

**Review question:** `AmbientSoundPlayer` uses `AudioTrack.MODE_STREAM` with `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC`; permanent focus loss stops, transient loss pauses, and the sleep timer only stops already-user-started playback. Confirm the focus listener handles each case and the timer can't auto-start playback. Flag only a real focus/timer defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 28.3 Pro & permission gate

**Review question:** `AmbientSoundPlaybackViewModel` gates on Pro, user action, and Android 13+ notification permission; Free can't start playback or persist ambient settings. Confirm the gate precedes service start and settings writes. Report only a real bypass.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 28.4 Normalization

**Review question:** `AmbientSoundPolicy` normalizes presets (`WHITE_NOISE`/`PINK_NOISE`/`BROWN_NOISE`/`FAN`), volume to `0.05f..1.0f`, and timer to `0/15/30/60/120`. Confirm out-of-range stored values normalize safely. Flag only a real normalization defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 28.5 AudioTrack release

**Review question:** Confirm the `AudioTrack` is stopped, flushed and released on stop, focus-loss stop, timer stop and service destroy, with abandon of audio focus. Report only a real leak/missed-release.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 29. Sound detection / YAMNet

## 29.1 Inference scope

**Review question:** Live inference runs only when Pro + `soundDetectionEnabled`; `AudioEngine` itself does no classifier inference and persists no raw audio. Confirm `AudioSessionManager` controls the fanout with that effective condition and publishes current + recent detections without persisting raw/float windows. Flag only a real scope leak.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 29.2 Persistence opt-in & dedup

**Review question:** `SoundDetectionRepository` persists only with a separate `soundDetectionPersistenceEnabled` opt-in, storing aggregated events (`sessionId`, timestamp, label, confidence); the same label is re-stored only after the detected label changes or the classifier returns empty in between. Confirm the dedup logic matches that rule and never stores raw audio. Report only a real dedup/persistence defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 29.3 Classifier lifecycle

**Review question:** `MediaPipeSoundClassifier` loads the YAMNet asset via MediaPipe Tasks Audio and maps categories through `SoundClassificationPolicy`'s confidence threshold. Confirm the classifier/runtime is closed/released when sound detection stops, asset-load failure is handled, and the confidence threshold mapping is applied. Flag only a real lifecycle/threshold defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 30. Audible alarm

## 30.1 Domain purity

**Review question:** `AudibleAlarmPolicy`/`AudibleAlarmEvaluator` make threshold/duration/cooldown decisions as pure domain code and must not play audio, request focus, or touch notifications. Confirm playback/focus stay in `service/`. Report only a real domain-layer side effect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 30.2 Thresholds & guards

**Review question:** Confirm the documented 90 dB / 30 s / 5 min policy, the proximity/interactive guard before playing, and the bundled alarm WAV path. Verify the cooldown prevents repeated alarms and the guard suppresses playback when the device indicates it shouldn't sound. Flag only a real threshold/guard defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 30.3 Playback release

**Review question:** Confirm the alarm playback releases its player and abandons focus after playing, and the Pro opt-in gate is enforced before any alarm sounds. Report only a real leak or ungated alarm.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 31. Voice baseline & TTS risk prompt

## 31.1 Baseline preconditions

**Review question:** Voice baseline requires Pro + an active measurement + sound detection, and stores only aggregate baseline values to DataStore. Confirm all preconditions are enforced and only aggregates (level, sample count, captured-at) are stored - no raw audio. Flag only a real precondition/persistence defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 31.2 TTS trigger conditions

**Review question:** The spoken TTS risk prompt is OFF by default and must trigger only when all runtime gates are satisfied: effective Pro, `tts_risk_prompt` opt-in enabled, a dosimeter `DOSE` or `PROJECTED_DOSE` risk event, sound detection available, and a latest hearing-test baseline available. Confirm it cannot trigger from other alert events and that the Pro, opt-in, sound-detection and baseline guards are checked at the moment of each possible trigger. Report only a real trigger-condition defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 31.3 Domain/service split & TTS release

**Review question:** `domain/voice/*` makes the decisions; Android `TextToSpeech`, notification delivery and haptic/audio playback stay in `service/`. Confirm the split holds and `TextToSpeech` is initialized once and `shutdown()` on teardown (no leak, no use-before-init). Flag only a real split/lifecycle defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 32. Tinnitus pitch

## 32.1 Pro gate & scope

**Review question:** `TinnitusPitchMatcherViewModel` gates on Pro (Free effective profile empty/locked, no preview/save) and the feature adds no background playback, service, media notification, sound therapy, Health Connect write, raw audio, or auto triggers. Confirm the gate and that none of those couplings exist. Report only a real bypass/coupling.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 32.2 Normalization & amplitude

**Review question:** `TinnitusPitchPolicy` normalizes pitch into 250-8000 Hz at 50 Hz steps and uses a fixed -36 dB preview amplitude. Confirm out-of-range pitches clamp/snap correctly and the preview amplitude is fixed. Flag only a real normalization defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 32.3 Storage scope

**Review question:** Only DataStore keys (`tinnitus_left_pitch_hz`, `tinnitus_right_pitch_hz`, `tinnitus_pitch_updated_at_ms`) are written; no Room schema change. Confirm nothing writes tinnitus data to Room and preview uses the existing `ToneGenerator` from a user-initiated Preview only. Report only a real over-persistence or auto-play.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 33. Camera overlay

## 33.1 Permission before bind

**Review question:** The camera route requests `CAMERA` before binding CameraX preview. Confirm the permission is granted before `bindToLifecycle`/preview, and a denied permission shows a graceful state (no crash). Flag only a real unguarded bind.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 33.2 Passive live readout

**Review question:** The live dB readout reads `AudioEngine.decibelFlow` only during an active measurement and must not start or stop a measurement session itself. Confirm the camera overlay never drives session start/stop. Report only a real side effect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 33.3 Photo & silent video

**Review question:** Photo share writes a temporary raw JPG to the export cache, burns the readout into a shareable PNG, and publishes via FileProvider; silent video writes an MP4 without CameraX `withAudioEnabled()` (no audio capture). Confirm the video path has no audio and the temp JPG is cleaned up. Flag only a real audio-capture or leftover-temp defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 33.4 CameraX lifecycle

**Review question:** Confirm the `ProcessCameraProvider` is unbound and resources released when leaving the overlay, with no leaked use-cases or surfaces. Report only a real lifecycle/leak defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 34. Audio input device routing

## 34.1 Pro-effective selection

**Review question:** `selected_audio_input_device_id` is a Pro-effective DataStore choice; the Free execution path always gets effective null. Confirm a Free user can't route to an external device even with a stored id. Flag only a real leak.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 34.2 Fallback resolver

**Review question:** `AudioInputDeviceRouteResolver` falls back to the built-in mic when the selected external input is no longer present, without overwriting the stored preference. Confirm the stored id is preserved across an unplug and routing falls back cleanly. Report only a real resolver defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 34.3 Preferred device timing

**Review question:** `AndroidAudioInputDeviceRouter` calls `AudioRecord.setPreferredDevice(...)` before `startRecording()` and publishes the routed device name via `AudioInputInfo`. Confirm the order and that a failed `setPreferredDevice` degrades to built-in rather than failing the session. Flag only a real ordering/handling defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 34.4 Discovery mapping

**Review question:** `AndroidAudioInputDeviceDiscoveryPort` reads `getDevices(GET_DEVICES_INPUTS)`, maps USB/Bluetooth/wired/built-in types, and publishes display names, external flag, sample-rate and channel lists. Confirm the type mapping is correct and unknown device types are handled. Report only a real mapping defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 35. Navigation

## 35.1 Start destination & rail breakpoint

**Review question:** `DbCheckNavHost` uses bottom navigation on phones and a NavigationRail at ≥600dp, with `meter` as start. Confirm the breakpoint logic and start destination, and that rotating across the breakpoint doesn't lose nav state. Flag only a real defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 35.2 Back-stack root behavior

**Review question:** Top-level navigation returns the selected stack to its root conservatively (same stack: don't restore state; different stack: `saveState`/`restoreState`). Confirm the `saveState`/`restoreState`/`launchSingleTop`/`popUpTo` flags implement that intent without creating duplicate destinations. Report only a real back-stack defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 35.3 Route arg safety

**Review question:** Route args (`sessionId`, `testId`, `showPro`) must be parsed null-safely with a sane fallback when missing/malformed. Confirm no `!!` or unchecked parse that could crash on a bad deep link. Flag only a real arg-parsing defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 36. Compose UI correctness

## 36.1 State hoisting & ownership

**Review question:** Confirm screen state is owned by the ViewModel and composables are largely stateless/driven by `UiState`, with `collectAsStateWithLifecycle` for flows (not `collectAsState` for lifecycle-sensitive streams). Report only a real state-ownership or lifecycle-collection defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 36.2 Recomposition cost

**Review question:** Look for heavy work done directly in composition (parsing, formatting, allocations) that should be in `remember`/`derivedStateOf` or the ViewModel, especially in chart canvases and lists. Flag only a real per-recomposition cost that would matter, not micro-optimizations.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 36.3 Effect keys

**Review question:** Confirm `LaunchedEffect`/`DisposableEffect` keys are correct - not `Unit` where they must re-run on a changing input, and not a frequently changing key that restarts an effect every frame. Report only a real missed-run or restart-storm.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 36.4 List keys

**Review question:** Confirm `LazyColumn`/`LazyRow` items use stable keys (session id, etc.) rather than index, so item state and scroll position survive data changes. Flag only a real index-as-key issue causing state loss.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 36.5 Canvas empty-data safety

**Review question:** Custom Canvas charts (waveform, spectrogram, RTA bars, audiogram, monthly/yearly) must handle empty or single-point data without dividing by zero or crashing. Confirm each chart guards empty input. Report only a real crash path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 37. Theme & design tokens

## 37.1 Token usage

**Review question:** New design values should be centralized in the theme (`Color.kt`, `Shape.kt`, `Spacing.kt`, gradient). Flag inline colors/spacing/animation durations/card defaults that duplicate an existing token - but only where a token already exists; don't invent tokens for one-off values.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 37.2 Typography roles

**Review question:** Manrope is for general text and Space Grotesk for numeric/data display, identical across themes. Confirm numeric displays (dB readout, stats) use the numeric typeface and body text uses Manrope. Report only a real role mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 37.3 Light/dark parity

**Review question:** Confirm `values` / `values-night` and the dark/light token sets are in parity (no token defined for one theme only) and the startup theme bootstrap applies the stored theme without a flash. Flag only a real parity/bootstrap defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 38. Resource & memory leaks

## 38.1 Streams & cursors

**Review question:** Confirm every `InputStream`/`OutputStream`/`FileChannel`/`Cursor` opened in export, backup/restore, WAV, PDF and FileProvider paths is closed via `use {}`/try-finally, including on the error path. Report only a real unclosed resource.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 38.2 Bitmaps

**Review question:** Confirm bitmaps created for PNG share cards and PDF charts are recycled (or scoped) after encoding so large share/report operations don't retain memory. Flag only a real un-recycled large bitmap.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 38.3 Listeners & callbacks

**Review question:** Confirm billing listeners, audio-focus listeners, audio device callbacks, TTS listeners and any registered receivers are unregistered/abandoned on teardown. Report only a real un-unregistered listener that leaks.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 38.4 Context retention

**Review question:** Confirm no singleton/static field retains an Activity/View context, and long-lived components use the application context. Flag only a real retained-context leak.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 39. Error handling & user-facing errors

## 39.1 No raw exception text

**Review question:** `util/UserFacingError` centralizes filtering of technical `Throwable` messages into resourced fallback text; UI must not show raw exception text in share/export/Health Connect/history/hearing-test errors. Confirm new error paths route through `toUserFacingMessage(...)`. Report only a real raw-message leak to the UI.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 39.2 Cancellation not swallowed

**Review question:** Confirm `catch` blocks don't swallow `CancellationException` (which would break structured concurrency) and don't catch `Throwable` so broadly that they hide programming errors. Flag only a real cancellation-swallow or over-broad catch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 39.3 Sealed result states

**Review question:** Confirm load/share/PDF/metadata operations expose explicit success/error/locked/missing states rather than nullable ambiguity, so the UI can't silently render a blank screen on failure. Report only a real ambiguous-state path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 40. Localization & accessibility

## 40.1 No inline UI strings

**Review question:** Confirm new user-facing text uses string resources (no inline literals in composables), and the default `values/strings.xml` is updated alongside the intentionally limited `values-fi` baseline (or it's documented why fi is excluded). Flag only a real inline string or missing default resource.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 40.2 Placeholder parity

**Review question:** Confirm `values-fi` entries have the same placeholder count/types as their default counterparts (no missing/extra `%1$s`/`%1$d`) per `LocalizationBaselineTest`'s intent. Report only a real placeholder mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 40.3 Plurals

**Review question:** Confirm count-bearing strings use `plurals` (not `%d items`) and accessibility plural strings exist where needed (`PluralAccessibilityResourceTest`). Flag only a real non-pluralized count string.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 40.4 Semantics for non-text UI

**Review question:** Confirm charts and icon-only actions have content descriptions / semantic roles, touch targets meet ≈48dp, and selected-state is exposed to TalkBack (per the Osa93 accessibility guards). Report only a real missing semantic/target.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 41. Manifest, permissions & privacy

## 41.1 Exported flags

**Review question:** Confirm only `MainActivity` is `exported=true`; `MeasurementForegroundService`, `AmbientSoundPlaybackService`, `DbCheckWidgetReceiver`, `FileProvider` and `HealthConnectPermissionDisclosureActivity` are `exported=false`; the Health Connect rationale/usage entry points are activity-aliases targeting the static disclosure activity. Flag only a real wrong export flag.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 41.2 Backup exclusion

**Review question:** `android:allowBackup="false"` with `backup_rules.xml` and `data_extraction_rules.xml` exclude app root data from cloud backup and device transfer. Confirm the rules don't accidentally include the database/preferences and that local backup is an explicit user action only. Report only a real exclusion defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 41.3 Location scope minimality

**Review question:** Only `ACCESS_COARSE_LOCATION` is declared (no FINE/BACKGROUND, no foreground-service `location` type). Confirm no code requires FINE/BACKGROUND or a location FGS type, the runtime request appears only on the user's location action, and a denied/unavailable location leaves the field empty without breaking start/stop. Flag only a real over-scope or flow break.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 41.4 Queries & cleartext

**Review question:** Confirm `<queries>` only includes the Health Connect package and Android 11+ TextToSpeech intents (minimal), `usesCleartextTraffic="false"`, and camera features are `required=false`. Report only a real over-broad query or config defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 42. Security & secrets

## 42.1 No committed secrets

**Review question:** Confirm no keystore, signing password, or DSN is committed; release signing reads `DBCHECK_RELEASE_*` from Gradle properties/env and fails explicitly if only some values are present; Sentry DSN comes from env or an ignored `debug.credentials.properties`. Flag only a real committed secret or weak fallback.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 42.2 No sensitive logging

**Review question:** Confirm purchase tokens, location coordinates, and file paths to user data aren't logged at a level shipping in release. Report only a real sensitive-log statement reachable in release.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 42.3 WAV not in MediaStore

**Review question:** Confirm WAV files stay in app-private storage and are never copied to MediaStore/shared storage, and sharing is only via FileProvider temp grant. Flag only a real export-to-shared-storage path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 43. CI/CD & static analysis

## 43.1 Dependency verification updates

**Review question:** When build/scanner dependencies change, the dependency lockfile / verification metadata and pinned versions must stay consistent (dependency locking is enabled). Confirm a changed dependency wouldn't leave a stale lock that fails CI or, worse, silently bypasses verification. Report only a real inconsistency.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 43.2 SARIF paths

**Review question:** Confirm the Semgrep and OWASP Dependency-Check SARIF upload paths in the security workflow match the files those tasks actually produce. Flag only a real path mismatch that would upload nothing.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 43.3 Wrapper checksum pinning

**Review question:** Confirm `gradle-wrapper.properties` pins the distribution by checksum (`distributionSha256Sum`) per `GradleWrapperSecurityTest`. Report only a real missing/incorrect checksum.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 43.4 ktlintCheck alias

**Review question:** `ktlintCheck` is an alias that depends on `detekt`, and detekt's ktlint wrapper disables purely stylistic rules that don't match Android Studio formatting. Confirm the alias/dependency wiring is intact and detekt config (`LongMethod 80`, `MaxLineLength 120`, MagicNumber off, no wildcard imports) is applied. Flag only a real config/wiring defect.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 44. Testing gaps & quality

## 44.1 Coverage of critical paths

**Review question:** Pick one critical runtime path from the component under review, identify its expected observable behavior, and confirm whether a unit test, screenshot test, source-level policy test, release QA document or documented device-QA item covers that behavior. Report only a real untested critical path. Do not demand tests for trivial mapping code, purely visual one-off layout, or behavior already covered by a broader contract test.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 44.2 Tests assert behavior

**Review question:** Confirm tests assert observable behavior/contracts rather than over-mocking internals in a way that would pass even if the behavior broke. Flag only a real test that can't fail on the bug it's meant to guard.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 44.3 Coroutine/flow tests

**Review question:** Confirm coroutine tests use a test dispatcher (no real delays) and Turbine flow tests assert the terminal/expected emissions (no missed `awaitComplete`/`cancelAndConsumeRemainingEvents`). Report only a real flaky/incomplete test.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 44.4 Screenshot determinism

**Review question:** Confirm screenshot `@PreviewTest`s disable animations (`animationsEnabled=false`) and use fixed data so baselines are deterministic. Flag only a real nondeterminism source.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 45. Simplicity & over-engineering (meta-review)

## 45.1 Simpler construct available

**Review question:** Identify places where a simpler standard construct would do the same job as added machinery - but only where the current code is genuinely more complex than needed. Do NOT rewrite correct, working code for taste, and do NOT add complexity. If the current approach is already appropriately simple, say so.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 45.2 Duplicated logic

**Review question:** Look for logic duplicated instead of reusing a documented single source (report calculation in `SessionReportCalculator`, the calibration clamp in `CalibrationOffsetPolicy`, file paths in `ExportFileCache`, dispatchers from Hilt). Flag only real duplication that can drift; don't merge things that are intentionally separate.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 45.3 Unnecessary abstraction

**Review question:** Flag any interface/indirection/generic with a single implementation and no test or DI reason to exist - but recognize that the testable ports here (gateways, classifier, capture/discovery ports) exist deliberately for testing/DI. Only report abstraction that genuinely earns its keep nowhere.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 45.4 Dead code & unused flags

**Review question:** Identify clearly dead code, unreachable branches, or unused parameters/flags. Confirm a flag is truly unused before recommending removal (it may be read in a place you haven't checked). Report only what you can show is unused.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 45.5 Final discipline check

**Review question:** For this whole review: re-read your own findings and remove anything that is a hallucinated fault, a style preference dressed as a bug, or a complexity-adding rewrite of working code. A review that concludes "these areas are correct as written" is a successful, valuable review.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---


# 46. Release readiness, policy & device sign-off

## 46.1 Source-of-truth drift

**Review question:** Confirm `PROJECT.md`, the current code, release QA docs and any remaining historical specs do not create contradictory launch-scope expectations. Historical specs must not force features or behavior that `PROJECT.md` marks as current-code-only, future, out of scope, or requiring an explicit release decision. Flag only real contradictions that could cause Codex, CI or a human reviewer to implement or release the wrong scope.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.2 Release-scope gating for non-v1.0 features

**Review question:** For features marked as current-code-but-not-v1.0 or requiring an explicit product/privacy decision, especially `tinnitus/pitch`, confirm the release build either hides/disables the route and CTA or the release documentation has been updated to explicitly include it. Do not report a defect merely because the code exists; report only a real mismatch between release-scope decision, route visibility, Pro gating and user-facing copy.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.3 Health and medical-claim copy audit

**Review question:** Scan user-facing strings, PDFs, PNG/share text, notification text, Settings copy, Analytics cards, hearing test/recovery copy, tinnitus copy, ambient playback copy, privacy policy and Play-copy drafts if present. Confirm dBcheck does not claim clinical diagnosis, calibrated audiometry, hearing-loss detection, tinnitus treatment, symptom reduction, guaranteed safety, or therapeutic effect. Confirm relative/personal-tracking disclaimers are present where needed. Flag only concrete over-claiming text with exact resource/file references.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.4 Privacy policy and Play Data Safety consistency

**Review question:** Compare `AndroidManifest.xml` permissions, Room entities, DataStore keys, WAV storage, Health Connect writes/reads, FileProvider sharing, local backup behavior and any privacy-policy / Play Data Safety drafts in the repo. Confirm the declared data collection/sharing matches what the app actually stores or shares. Pay special attention to microphone audio, WAV opt-in, approximate location, Health Connect, purchase state, heart rate, hearing-test data, passive monitoring aggregates, sound-detection labels and local backups. Flag only a real mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.5 Runtime permission UX matrix

**Review question:** Confirm every runtime permission path has a complete user-visible flow: `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `CAMERA`, `ACCESS_COARSE_LOCATION`, Health Connect write exercise and Health Connect read heart rate. For each permission, verify first request, denial, permanent denial / system settings path where applicable, unavailable provider/device state, and later retry. Report only a real state where the app crashes, silently does nothing, starts restricted work without permission, or leaves the user stuck.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.6 Device-QA evidence for flows unit tests cannot prove

**Review question:** Inspect the release QA docs/tests for evidence that device-level smoke coverage exists or is explicitly required for flows that unit tests and screenshot tests cannot prove: real microphone recording, foreground-service promotion on supported Android versions, notification permission denial, CameraX preview/photo/video, Android Sharesheet grants, Play Billing test purchase, Health Connect permission flow, TalkBack navigation and release AAB install. Flag only missing sign-off evidence for a release-blocking path.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.7 Play Billing production configuration contract

**Review question:** Confirm code, string resources, release QA docs and Play Console checklist agree on the single INAPP product `dbcheck_pro`: product id, entitlement behavior, one-time purchase wording, no-subscription wording, pending purchase handling, already-owned handling, acknowledge behavior and refund/revocation refresh behavior. Do not require Play Console access; inspect only repo-visible docs/config and flag only real contradictions or missing release-signoff items.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.8 Release artifact and signing workflow

**Review question:** Verify `.github/workflows/release-build.yml`, Gradle signing config and release QA docs agree on unsigned PR artifacts, signed push artifacts, required `DBCHECK_RELEASE_*` secrets, AAB/APK output paths, signing verification and failure behavior when only some signing values are present. Flag only a real path mismatch, weak fallback, missing verification, or documentation contradiction.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.9 Qodana non-blocking risk

**Review question:** Qodana is intentionally non-blocking until AGP 9.1.0 compatibility is proven. Confirm the workflow status name, `continue-on-error`, summary text and QA docs make this risk visible, and that no release checklist treats Qodana as fully green unless there is a successful current Qodana run. Report only a real case where the non-blocking status could be mistaken for release sign-off.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.10 Camera video claim vs implementation

**Review question:** Camera Overlay silent video currently records MP4 without CameraX audio. Confirm user-facing copy does not claim the live dB overlay is burned into video unless the code actually renders or post-processes that overlay into the MP4. Confirm photo overlay burn-in and video silent/no-audio behavior are accurately described. Flag only a real copy/implementation mismatch.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.11 Export time, locale and numeric formatting stability

**Review question:** Confirm CSV, PDF, PNG/share text and Health Connect notes use stable, locale-safe formatting where required: timestamps include enough timezone/context to be understandable, CSV numeric values use a predictable decimal separator, exported rows are ordered deterministically, and user-facing localized formatting does not break machine-readable CSV. Flag only a real ambiguity, locale bug or interoperability issue.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.12 Third-party license and attribution coverage

**Review question:** Confirm bundled assets and libraries that require attribution or license disclosure are covered in repo-visible notices or About/legal screens where appropriate: fonts, YAMNet/TensorFlow Lite assets, bundled alarm WAV, ambient generated/bundled assets if any, icons and major OSS libraries. Flag only a real missing license notice for a dependency or asset that requires one.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---

## 46.13 Performance and battery regression hotspots

**Review question:** Review the highest-risk runtime loops and update paths for avoidable CPU/battery regressions: AudioRecord loop, weighting filters, FFT/RTA/spectrogram, YAMNet inference, 1 Hz notification updates, passive monitoring sample, ambient playback generation, PDF/CSV export and large history queries. Confirm throttling, dispatcher use and feature toggles prevent unnecessary work when the feature is disabled or the user is Free. Flag only a concrete hot loop or repeated work path that can run unnecessarily.

**Rules of engagement (read this before answering):** This is real, working
production code, not a puzzle with a planted bug. LLMs frequently hallucinate
problems that don't exist and "fix" code that is already correct, so verify every
claim directly against the actual source before reporting it. Source hierarchy:
the live checkout and `PROJECT.md` describe the current implementation state.
Historical specs and addenda are context only. If an older spec conflicts with
`PROJECT.md` or the actual code, do not treat the older spec as a defect by
itself. "No issue found - this is correct as written" is a complete, valid, and
welcome answer; you do not need to find a problem. Don't invent faults, don't
flag pure style as a defect, and don't propose a more complex solution where the
existing simpler one already works. If you do report something, cite the exact
file, function and line, and state the concrete failure case it causes.

---
