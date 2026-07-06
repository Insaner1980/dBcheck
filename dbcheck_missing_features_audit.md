# dBcheck Missing and Incomplete Feature Audit

**Generated:** 2026-06-07
**Language:** English
**Purpose:** Convert the missing or incomplete items from the planning/design documents into a concrete implementation backlog.

## 0. Scope and methodology

This audit compares the planning/design documents in the project folder against `PROJECT.md`, which is treated as the current implementation description.

Reviewed planning/design documents:

1. `dBcheck_complete_spec_v2.md`
2. `dBcheck_design_spec.md`
3. `dBcheck_competitive_features_addendum.md`

Current-state source:

1. `PROJECT.md`, updated from the current checkout on 2026-06-07.

Important interpretation rule:

- `PROJECT.md` is **not** a plan. It describes the current codebase.
- `dBcheck_complete_spec_v2.md` explicitly says it is historical/planning material and that `PROJECT.md` / the live checkout should be treated as the source of truth when there is disagreement.
- Therefore, anything required by the planning/design documents but not described as implemented in `PROJECT.md` is classified here as either:
  - **Missing** — no current implementation is described.
  - **Partial** — some underlying data/model/service exists, but the planned user-facing feature is incomplete.
  - **Deviation** — implemented intentionally or accidentally differently from the plan.
  - **Validation gap** — implementation may exist, but `PROJECT.md` states that release-grade verification is still missing.
  - **Documentation/code verification needed** — `PROJECT.md` does not prove the item exists, so it should be checked directly in code before closing.

This file intentionally does **not** relist features that are clearly implemented, except when they are related to a partial gap.

---

## 1. Executive summary

The current dBcheck implementation is strong. The app already has a working Kotlin/Compose architecture, live meter, foreground service, Room/DataStore persistence, Pro gating, Health Connect session writing, heart-rate overlay, CSV/PDF/PNG exports, widget, hearing test flow, session detail reporting, local backup, B/C/Z/ITU-R 468 weighting, and a sizeable test suite.

However, the planning documents describe a larger and more competitive product than the current codebase. The largest remaining gaps are:

1. **Dosimeter Mode as a Meter-tab mode**
2. **Camera Overlay**
3. **Sleep Monitor**
4. **Full Spectral toolset: Spectrogram + RTA + mode toggle + complete stat pills**
5. **YAMNet / TensorFlow Lite Sound Type Detection**
6. **Real-time Environment Mix during active measurement**
7. **Meter 30-second time-series chart**
8. **Sound reference card**
9. **Response-time selector/default that affects RMS calculation**
10. **Session location**
11. **dB distribution histogram**
12. **WAV/audio recording**
13. **Octave-band calibration and calibration profiles**
14. **External microphone support**
15. **NIOSH/OSHA dosimeter standard selection**
16. **Extended exposure alerts and notification schedule**
17. **Display/feature toggles for technical metadata, dosimeter, sound detection, and sleep card**
18. **Always-On Display / Keep Awake**
19. **Hearing protection tips**
20. **Searchable/filterable Pro history**
21. **Scientific PDF report completion**
22. **Public lock-screen visibility difference from competitive addendum**
23. **Audible Threshold Alarm**
24. **Voice Volume Detection**
25. **Passive Background Monitoring**
26. **TTS Detection**
27. **Tinnitus tools**
28. **Full accessibility audit**
29. **Localization**
30. **Device-level audio / foreground-service / permission / billing / share / release validation**

A practical priority order is:

1. Finish launch-critical v1.0 gaps from the complete spec: Dosimeter, Camera Overlay, Sleep Monitor, Spectral, Sound Detection, History/Export missing pieces, Settings missing pieces.
2. Close P0 competitive gaps: Scientific PDF completeness and lock-screen visibility decision.
3. Complete release-readiness validation: device tests, accessibility audit, localization, billing/signing/policy verification.
4. Queue P1/P2/P3 competitive features based on product strategy.

---

## 2. Status legend

| Status | Meaning |
|---|---|
| **Missing** | Required by a planning document but not described in `PROJECT.md` as implemented. |
| **Partial** | Some lower-level support exists, but the planned feature is incomplete. |
| **Deviation** | Current implementation differs from the plan. This may be acceptable, but the choice must be explicit. |
| **Validation gap** | Current implementation may exist, but release-grade verification is not complete. |
| **Needs code verification** | `PROJECT.md` does not confirm the item. Check the live code before deciding. |

---

## 3. Launch-critical product gaps from `dBcheck_complete_spec_v2.md`

### 3.1 Meter tab segmented measurement modes

**Status:** Missing / partial navigation gap
**Source intent:** The Meter tab should have an internal segmented control / chip row with:

- `dB Meter` as the default mode
- `Dosimeter` as a Pro mode

**Current state from `PROJECT.md`:**

- The Meter route is described as live gauge, waveform, Min/Avg/Max/Peak, Play/Pause, Reset, Share, permissions, and foreground service.
- No Meter-tab mode chip row is described.
- Dosimeter values exist in Session Detail for A-weighted sessions, but this is not the same as a Meter-tab Dosimeter mode.

**Why this matters:**

The complete spec organizes measurement complexity inside the Meter tab. Without the mode switch, users cannot access the planned dosimeter experience during active measurement.

**Implementation requirements:**

- Add a top chip row to Meter.
- Maintain `dB Meter` as default.
- Add `Dosimeter` chip as Pro-gated.
- Free users should see a locked preview or a lock affordance, not have the feature hidden.
- Preserve bottom navigation; do not add a fifth tab.
- Save the selected mode if desired, but default should remain simple for new users.

**Acceptance criteria:**

- Meter displays `dB Meter` and `Dosimeter` chips.
- Selecting `dB Meter` shows the current dB gauge experience.
- Selecting `Dosimeter` as Pro shows dose-focused UI.
- Selecting `Dosimeter` as Free opens Pro purchase/upsell flow or locked preview.
- Navigation state survives basic recomposition and orientation changes where feasible.
- Accessibility labels explain the selected mode and locked state.

**Likely code areas:**

- `ui/meter/`
- `ui/components/DbCheckChip`
- `domain/entitlement/ProEntitlementPolicy`
- `billing/ProFeatureManager`
- Navigation state / ViewModel for Meter

**Testing checklist:**

- Free user sees Dosimeter locked.
- Pro user can switch modes.
- Switching modes does not stop the active AudioRecord session unless explicitly designed to do so.
- Screen-reader state announces selected/locked chips.
- Screenshot tests for Free and Pro states in both themes.

---

### 3.2 Dosimeter Mode UI and live calculations

**Status:** Partial
**Source intent:** Dosimeter Mode should replace the normal circular dB gauge with a dose-focused display, including:

- Noise dose percentage gauge
- TWA
- Noise Dose %
- Projected Dose
- LAeq
- Exposure time remaining
- Dose time-series chart
- NIOSH/OSHA standard label
- Dose reference info card

**Current state from `PROJECT.md`:**

- Session Detail calculates NIOSH 8h TWA, dose, and 85 dBA peak events only for A-weighted sessions.
- There is no described Meter-tab Dosimeter Mode.
- There is no described dose gauge, dose chart, projected dose, exposure time remaining, or live standards display.

**What is already reusable:**

- `SessionReportCalculator` already calculates some dose-related values for completed A-weighted sessions.
- `MeasurementPersistenceSampler` stores enough measurement data for post-session calculations.
- `NoiseAlertPolicy` and report models may provide useful boundary logic.
- Existing Pro gating patterns can be reused.

**Missing parts:**

- Real-time dose accumulation during an active session.
- Projected dose extrapolation.
- Exposure time remaining.
- Meter UI variant.
- NIOSH/OSHA standard selection from Settings.
- Dose reference educational card.
- Tests for 3 dB and 5 dB exchange-rate behavior.

**Implementation requirements:**

- Create a `DosimeterCalculator` or extend report calculation into a reusable domain service.
- Keep calculation logic in `domain/`, not UI.
- Support NIOSH and OSHA formulas.
- Emit live dosimeter state from the active session flow.
- Display current standard clearly.
- Do not show false zeros for unsupported weightings; explain when dose is unavailable because the session is not A-weighted if that restriction remains.
- Decide whether dosimetry should require A-weighting only. If yes, force A-weighting in Dosimeter mode or show an explicit warning.

**Acceptance criteria:**

- Pro users can enter Dosimeter Mode during measurement.
- Dose % increases over time based on sound exposure.
- TWA, LAeq, projected dose, and exposure time remaining update during the session.
- 100% dose threshold is clearly marked.
- Settings-selected NIOSH/OSHA standard affects calculations.
- Session Detail remains consistent with live Dosimeter calculations after stopping.
- Unit tests cover formula examples, edge cases, silence, very short sessions, and loud peaks.

**Risks:**

- Dose math must be conservative and clearly documented.
- The app should not imply it is a certified occupational dosimeter unless calibration requirements are met.
- Calculation differences between live and post-session reports would damage trust.

---

### 3.3 Meter 30-second real-time time-series chart

**Status:** Missing / not described in current state
**Source intent:** The default dB Meter mode should show a scrolling line/area chart of the last 30 seconds, with:

- 10 fps update rate
- Y-axis 0–120 dB
- 85 dB threshold line
- Peak markers
- Empty and paused states

**Current state from `PROJECT.md`:**

- Meter currently has live gauge, waveform, stats, Play/Pause, Reset, Share.
- A distinct 30-second dB time-series chart is not described.
- Session Detail has time-series reporting after completion, but that is not the same as the live Meter chart.

**Implementation requirements:**

- Add a reusable live chart component.
- Maintain a rolling in-memory buffer for the last 30 seconds.
- Use throttled UI readings, not Room persistence cadence, for smoothness.
- Keep AudioRecord and persistence cadence unchanged.
- Add empty and paused states.
- Use design tokens and custom Canvas charting style.

**Acceptance criteria:**

- Starting measurement begins populating chart points.
- Pausing freezes the chart and shows a paused indicator.
- Reset clears the buffer.
- Threshold line is visible at 85 dB.
- Peak markers appear when a new session max occurs.
- Performance remains stable on lower-end Android devices.

**Testing checklist:**

- Chart buffer never grows unbounded.
- Unit test rolling-window trimming.
- Screenshot tests for empty, active, paused, and high-noise states.
- Manual device performance test at High/Standard/Low UI refresh rates.

---

### 3.4 Meter sound reference card

**Status:** Missing / not described in current state
**Source intent:** The Meter screen should include an expandable sound reference card with:

- Collapsed gradient bar
- Triangle marker at current dB
- Expanded reference list from breathing through thunder/pain
- Closest match highlighted

**Current state from `PROJECT.md`:**

- Noise level labels exist through `domain/noise/NoiseLevel`.
- No sound reference card is described in Meter.

**Implementation requirements:**

- Add a `SoundReferenceCard` Compose component.
- Define reference entries in a domain or UI model.
- Use current dB to determine marker position and closest match.
- Make the card accessible; do not rely only on color.
- Persist expanded/collapsed state only if useful.

**Acceptance criteria:**

- The collapsed card appears below stat cards or in the planned location.
- Tapping expands/collapses the card.
- Closest dB reference is highlighted.
- The marker updates with current reading.
- Free and Pro users both see it.

**Testing checklist:**

- dB-to-marker position calculations.
- Current value below/above range clamps gracefully.
- Screenshot tests in both themes.
- TalkBack announces the nearest reference.

---

### 3.5 Response time selector and RMS calculation window

**Status:** Missing
**Source intent:** Pro users should be able to select:

- Slow: 500 ms
- Fast: 200 ms
- Impulse: 50 ms

This should affect RMS calculation. Free users should be locked to Fast.

**Current state from `PROJECT.md`:**

- `MeterRefreshRate` exists, but it only affects Meter UI update frequency.
- `PROJECT.md` explicitly says refresh rate does not change AudioRecord sample rate, filter state, or Room persistence cadence.
- No response-time RMS setting is described.

**Implementation requirements:**

- Add a distinct `ResponseTime` preference and domain model.
- Do not reuse UI refresh rate for this.
- Modify dB calculation pipeline to aggregate/smooth samples according to response time.
- Ensure weighted RMS, raw RMS, and LCpeak semantics remain correct.
- Add Settings default selector and Meter quick selector if desired by spec.
- Gate to Pro; Free effective value is Fast.

**Acceptance criteria:**

- Pro response time setting changes measured response behavior.
- Free users cannot affect response time.
- UI refresh rate remains independent.
- Reports include response time where required.
- PDF includes response time.
- Unit tests verify smoothing/window differences.

**Risks:**

- Incorrectly mixing response time with UI refresh rate would be a measurement credibility bug.
- Impulse mode may be noisy and should be documented clearly.

---

### 3.6 Meter LAeq display

**Status:** Partial
**Source intent:** Pro users should see LAeq during the active session, updated every second.

**Current state from `PROJECT.md`:**

- Session Detail reports equivalent-level / LAeq-style values.
- No Meter live LAeq display is described.

**Implementation requirements:**

- Reuse or extract equivalent-level calculation from `SessionReportCalculator`.
- Maintain an energy-average accumulator for the active session.
- Display label appropriate to weighting: LAeq, LCeq, LZeq, etc., if current architecture supports multiple weighting labels.
- Update at a stable cadence, likely 1 Hz.

**Acceptance criteria:**

- Active Meter session shows equivalent level for Pro users.
- Free users see locked preview or no Pro data according to existing Pro UX.
- Value matches Session Detail after stop within expected tolerance.
- Non-A-weighted labels are accurate.

**Testing checklist:**

- Unit tests for logarithmic averaging.
- UI test for Pro/Free visibility.
- Cross-check active LAeq vs completed report.

---

### 3.7 Complete session info bar

**Status:** Needs code verification / partial
**Source intent:** Meter dB mode should show:

- Pulsing red `REC` indicator
- Session duration timer
- Frequency weighting indicator for Pro
- Sample rate display for Pro

**Current state from `PROJECT.md`:**

- Meter has live gauge, waveform, stats and controls.
- It starts measurement and foreground service.
- The exact session info bar and sample-rate display are not confirmed.

**Implementation requirements:**

- Verify whether the current Meter UI has a complete session info bar.
- If missing, add it above the gauge or in the planned quick-info area.
- Keep sample rate display Pro-gated.
- Ensure Free users do not see misleading hidden technical metadata if Pro gates are intended.

**Acceptance criteria:**

- Recording state is obvious.
- Timer is accurate and pauses/resumes as designed.
- Weighting indicator reflects effective weighting, not merely stored preference.
- Sample rate shows actual AudioRecord sample rate.
- Accessibility labels include session duration and recording state.

---

### 3.8 Camera Overlay full-screen flow

**Status:** Missing
**Source intent:** A Pro full-screen flow from Meter should provide:

- CameraX camera preview
- Real-time dB overlay
- Noise level label
- Timestamp
- Photo capture with overlay burned in
- Video recording with live dB overlay
- Share or save to gallery
- CAMERA permission with rationale
- Minimal full-screen UI with close button

**Current state from `PROJECT.md`:**

- No camera route is described.
- No `CAMERA` permission is listed in manifest permissions.
- No CameraX dependency is listed in the current stack.
- Meter controls do not list camera entry.

**Implementation requirements:**

- Add CameraX dependencies.
- Add `CAMERA` manifest permission.
- Add permission rationale and denial handling.
- Add full-screen `camera_overlay` route.
- Hide bottom nav during this flow.
- Use live dB readings from current AudioEngine/session state.
- Implement photo capture with burned-in overlay.
- Decide whether video recording is required for v1.0 or can be staged; planning spec says it belongs in v1.0.
- Use MediaStore or Storage Access Framework as appropriate.
- Ensure no audio is recorded in video unless explicitly intended and disclosed.

**Acceptance criteria:**

- Pro user can open Camera Overlay from Meter.
- Free user sees Pro lock/upsell.
- First use requests camera permission with rationale.
- Denied permission shows recoverable UI.
- Preview displays live dB overlay.
- Captured photo includes visible dB, label, and timestamp burned into the image.
- Video capture, if implemented, includes live dB overlay or is explicitly excluded from v1.0 scope by product decision.
- Share flow uses safe `FileProvider` / MediaStore URI behavior.
- No crash when camera is unavailable.

**Testing checklist:**

- Manual test on at least two devices.
- Permission denial / permanent denial.
- Rotation/orientation behavior.
- Low-light camera behavior.
- Active measurement + camera resource interaction.
- Free/Pro gating.
- Screenshot or fake-preview tests for overlay UI.

**Risks:**

- Camera + microphone foreground behavior must respect Android privacy indicators and permissions.
- Video with audio could create privacy implications. The spec says dB overlay; it does not clearly require recording audio.

---

### 3.9 Analytics internal chip row

**Status:** Missing / not described
**Source intent:** Analytics should have internal chips:

- `Overview`
- `Spectral` Pro
- `Environment` Pro

**Current state from `PROJECT.md`:**

- Analytics route displays weekly exposure, hearing health, Pro live spectrum, Environment Mix, 30-day trend, 12-month report, hearing-test CTA.
- No Analytics sub-section chips are described.

**Implementation requirements:**

- Add chip navigation within Analytics.
- Keep bottom navigation unchanged.
- Move relevant cards into the planned sub-sections.
- Free users should see Pro sections as locked previews.
- Preserve current analytics data flows.

**Acceptance criteria:**

- Overview contains weekly/monthly charts, hearing health, hearing tips, Sleep Monitor CTA, Hearing Test CTA.
- Spectral contains spectral visualization modes.
- Environment contains Environment Mix and Sound Type Detection.
- Chip selection is accessible and visually consistent.
- Back stack behavior remains simple.

---

### 3.10 Weekly/monthly analytics toggle and monthly chart

**Status:** Partial
**Source intent:** Overview analytics should have a `Weekly` / `Monthly` chip selector. Weekly is Free; Monthly is Pro.

**Current state from `PROJECT.md`:**

- Weekly exposure chart exists.
- Pro 30-day trend and 12-month report exist.
- A direct weekly/monthly toggle matching the spec is not described.

**Implementation requirements:**

- Decide whether current 30-day and 12-month cards satisfy the Monthly requirement.
- If not, implement a monthly exposure chart under the `Weekly` / `Monthly` toggle.
- Gate Monthly to Pro.
- Reuse `ExposureAnalyticsCalculator` where possible.

**Acceptance criteria:**

- Free users can view Weekly.
- Free users see Monthly as locked preview.
- Pro users can switch to Monthly.
- Monthly chart has clear date axis and average dB/day or equivalent metric.
- Values match repository calculations.

---

### 3.11 Hearing protection tips

**Status:** Missing / not described
**Source intent:** Hearing Health card should include `VIEW TIPS` opening:

- Practical advice
- Safe exposure limits
- Hearing protector selection guidance
- Common-scenario exposure reduction tips

**Current state from `PROJECT.md`:**

- Hearing health status exists.
- No hearing tips bottom sheet, expanded section, or content model is described.

**Implementation requirements:**

- Add tips content, preferably as resource strings.
- Add `VIEW TIPS` CTA to Hearing Health card.
- Use non-alarmist language.
- Include medical disclaimer: this is educational information, not clinical diagnosis.
- Ensure tips are localizable.

**Acceptance criteria:**

- CTA is visible in Hearing Health card.
- Tips open in bottom sheet or expandable area.
- Content covers safe limits and practical protection.
- Content is accessible and resourced.
- Works in both themes.

---

### 3.12 Sleep Monitor CTA and full-screen flow

**Status:** Missing
**Source intent:** Pro Sleep Monitor should include:

Setup screen:

- Explanation
- Optional alarm/auto-stop time
- Sample sensitivity: every 10 / 30 / 60 seconds
- Start button

Active monitoring screen:

- Dark minimal UI
- Large clock
- Current dB
- Recording indicator
- Stop button
- Foreground service
- Reduced battery mode
- Screen dims or runs screen-off with wake lock depending on settings

Results screen:

- Duration
- Average dB
- Peak dB
- Number of noise events
- Timeline chart
- Notable events with timestamps
- Save to history as Sleep session
- Export PDF/CSV

**Current state from `PROJECT.md`:**

- No Sleep Monitor route is described.
- No `WAKE_LOCK` manifest permission is listed.
- No Sleep Monitor service or UI is described.
- History may contain tagged sessions, but no sleep-specific flow.

**Implementation requirements:**

- Add full-screen navigation flow:
  - `sleep_monitor/setup`
  - `sleep_monitor/active`
  - `sleep_monitor/results/{sessionId}`
- Add Pro gate.
- Add Settings toggle to show/hide Sleep Monitor card.
- Add Sleep session metadata defaults: moon icon, `Sleep` tag.
- Add wake lock behavior if required.
- Add foreground service behavior compatible with Android microphone service restrictions.
- Define battery-conscious sampling behavior.
- Add export integration.

**Acceptance criteria:**

- Free users cannot run Sleep Monitor.
- Pro users can start and stop overnight monitoring.
- Measurement continues with correct foreground notification while screen is off.
- Results are saved to History as Sleep session.
- Timeline and notable events are shown.
- PDF/CSV export works for Sleep sessions.
- Permission denial and service failure show user-facing errors.
- Device battery and Doze behavior are tested.

**Risks:**

- Long-running microphone foreground service must be verified on real devices.
- Wake lock and screen behavior can create battery drain and Play policy review concerns.
- Sleep Monitor should not imply clinical sleep analysis.

---

### 3.13 Spectral mode architecture

**Status:** Partial
**Source intent:** Analytics Spectral sub-section should have:

- Mode toggle: `Bars` / `Spectrogram` / `RTA`
- Bars mode
- Waterfall Spectrogram
- Octave-band RTA
- Stat pills
- Live capture indicator

**Current state from `PROJECT.md`:**

- Pro live spectrum exists via `AudioEngine.spectralFrame`.
- `FFTProcessor` and `SpectralAnalyzer` exist.
- `SpectralAnalyzer` produces 24 logarithmic 20 Hz–20 kHz bands, dominant frequency, and bandwidth class.
- No Spectrogram waterfall or RTA mode is described.
- No mode toggle is described.

**Implementation requirements:**

- Add a `Spectral` internal analytics sub-section.
- Add mode toggle.
- Keep existing live spectrum as Bars mode if suitable.
- Implement Spectrogram data buffer.
- Implement RTA octave-band analyzer.
- Add stat pill model.
- Use brand color ramp, not rainbow.
- Do not persist spectral data unless product decides to.

**Acceptance criteria:**

- Pro users can switch between Bars, Spectrogram, and RTA.
- Free users see locked preview.
- Bars mode updates live.
- Spectrogram scrolls over time and uses time/frequency/amplitude mapping.
- RTA displays standard octave bands and peak hold markers.
- Stat pills show dominant frequency, bandwidth, peak frequency, and noise floor.
- Performance remains acceptable.

---

### 3.14 Spectrogram waterfall

**Status:** Missing
**Source intent:** Spectrogram should show:

- X = time, 10-second window
- Y = frequency, 250 Hz to 16 kHz log scale
- Color = amplitude
- Brand color ramp
- ~100 ms per column

**Current state from `PROJECT.md`:**

- There is live spectral frame support but no waterfall buffer or spectrogram UI described.

**Implementation requirements:**

- Add a rolling 2D buffer of spectral columns.
- Decide frequency bin mapping.
- Normalize amplitude in a stable and visually meaningful way.
- Use Canvas drawing.
- Provide empty and paused states.
- Use accessible text summary for screen readers.

**Acceptance criteria:**

- Spectrogram updates while measurement is active.
- Buffer keeps approximately the last 10 seconds.
- Frequency labels appear at planned anchors.
- Color ramp follows dBcheck tokens.
- No unbounded memory growth.
- Works in dark and light themes.

---

### 3.15 Octave-band RTA

**Status:** Missing
**Source intent:** RTA should show standard octave bands:

- 31.5 Hz
- 63 Hz
- 125 Hz
- 250 Hz
- 500 Hz
- 1 kHz
- 2 kHz
- 4 kHz
- 8 kHz
- 16 kHz

Each bar shows current level in dB, with peak hold markers.

**Current state from `PROJECT.md`:**

- `SpectralAnalyzer` uses 24 logarithmic bands, not necessarily ISO octave bands.
- No RTA mode or peak hold markers are described.

**Implementation requirements:**

- Add octave-band calculation model.
- Decide whether to calculate from FFT bins or time-domain filters.
- Implement peak hold with decay or reset behavior.
- Display bands with clear labels.
- Add tests for band mapping.

**Acceptance criteria:**

- RTA bars match planned center frequencies.
- Peak hold markers appear and behave consistently.
- Values update live.
- UI remains readable on phone width.
- Free/Pro gating is enforced.

---

### 3.16 Complete spectral stat pills

**Status:** Partial
**Source intent:** Stat pills below visualization should show:

- Dominant Frequency
- Bandwidth
- Peak Frequency
- Noise Floor

**Current state from `PROJECT.md`:**

- `SpectralAnalyzer` produces dominant frequency and bandwidth class.
- Peak frequency and noise floor are not described.
- The 2x2 stat pill UI is not confirmed.

**Implementation requirements:**

- Extend spectral model to include peak frequency and noise floor.
- Add UI 2x2 pill grid.
- Keep labels and units styled with numeric typography.
- Define how noise floor is measured during session.

**Acceptance criteria:**

- All four pills are visible in Pro Spectral.
- Values update with live spectral data.
- Empty states show placeholders.
- Values are announced accessibly.

---

### 3.17 YAMNet / TensorFlow Lite Sound Type Detection

**Status:** Missing
**Source intent:** Environment sub-section should include ML sound classification using Google YAMNet / TensorFlow Lite:

- 500+ sound events
- Current detected sound type with confidence
- Recent detections with timestamps
- Toggleable in Settings
- Battery impact note
- Transient processing only unless explicitly stored as metadata

**Current state from `PROJECT.md`:**

- No TensorFlow Lite dependency is listed.
- No YAMNet model or classifier is described.
- Environment Mix is based on Room dB distribution, not ML classification.

**Implementation requirements:**

- Add TensorFlow Lite dependency.
- Add YAMNet model asset and label file.
- Verify model license and attribution requirements.
- Build on-device inference pipeline from transient audio buffers.
- Add Settings toggle.
- Add Environment UI card.
- Add detection history model if recent detections are persisted.
- Add privacy note: audio buffers are discarded unless WAV recording is enabled.

**Acceptance criteria:**

- Pro user can enable/disable sound detection.
- App displays detected sound label and confidence.
- Recent detections show timestamps.
- Free users see locked preview.
- Battery impact explanation is shown before enabling.
- No raw audio is persisted by default.
- App behaves gracefully when model load fails.

**Testing checklist:**

- Unit tests for label mapping and threshold filtering.
- Instrumented/device test for model loading.
- Battery/performance smoke test.
- Privacy review.
- Accessibility labels for detected result.

**Risks:**

- ML labels can be wrong. UI must say “Detected” or “Likely,” not present classification as fact.
- Inference cost may be too high on low-end devices unless throttled.

---

### 3.18 Real-time Environment Mix during active measurement

**Status:** Partial / deviation from planned behavior
**Source intent:** Environment Mix should update in real time during active measurement and show time distribution:

- Quiet
- Moderate
- Loud
- Critical

**Current state from `PROJECT.md`:**

- Pro Environment Mix reads 7-day Room counts.
- It is not described as real-time during active session.

**Implementation requirements:**

- Decide whether Environment Mix is meant to be:
  - Current-session real-time distribution,
  - 7-day historical distribution,
  - or both.
- If both, label them clearly.
- Add active-session accumulator if real-time is required.
- Preserve current 7-day analytics as historical Environment Mix or rename it.

**Acceptance criteria:**

- During active measurement, Environment Mix visibly changes as the environment changes.
- Historical Environment Mix remains available or is intentionally replaced.
- Percentages sum to 100% or handle no-data state clearly.
- Free/Pro gating is consistent.

---

### 3.19 Session location

**Status:** Missing
**Source intent:** Pro sessions should optionally store location:

- GPS/coarse coordinates
- Reverse-geocoded location name
- Display on session card and report
- Optional permission with rationale

**Current state from `PROJECT.md`:**

- Manifest permissions do not list `ACCESS_COARSE_LOCATION`.
- Session entity fields do not include location.
- Session cards and report data do not describe location.
- PDF can only include location if data exists, but no current location source is described.

**Implementation requirements:**

- Add location permission.
- Add Settings toggle or per-session prompt.
- Add data model fields for coordinates and display name.
- Add optional reverse geocoding with failure handling.
- Add privacy explanation.
- Add export/report inclusion.
- Make location optional and never block measurement.

**Acceptance criteria:**

- Pro user can enable session location.
- Permission is requested only with rationale.
- Denied permission leaves app fully usable.
- Location appears on session card, Session Detail, CSV/PDF where appropriate.
- Location can be omitted from share/export if privacy controls are added.
- Migration is safe.

**Risks:**

- Location increases privacy sensitivity.
- Reverse geocoding may be unavailable/offline; app must degrade gracefully.

---

### 3.20 Searchable and filterable Pro history

**Status:** Partial
**Source intent:** Pro users should have unlimited history that is searchable and filterable, including tag filtering.

**Current state from `PROJECT.md`:**

- Unlimited Pro history exists.
- Session naming and tags exist.
- View All mode exists.
- No search UI or tag filtering is described.

**Implementation requirements:**

- Add search field in View All or History.
- Add tag chips/filter sheet.
- Support date/range/noise-level filters if desired.
- Ensure queries use Room efficiently.
- Free user history remains limited by policy.

**Acceptance criteria:**

- Pro users can search by session name.
- Pro users can filter by tag.
- Filtered results update deterministically.
- Empty state explains no matching sessions.
- Free users cannot bypass 7-day policy via direct search/filter route.

---

### 3.21 dB distribution histogram

**Status:** Missing
**Source intent:** Session Detail should include a Pro histogram showing percentage of time in 10 dB buckets.

**Current state from `PROJECT.md`:**

- Session Detail includes report metrics, time-series, peak events, PNG, PDF, heart-rate overlay.
- Histogram is not described.

**Implementation requirements:**

- Add domain calculation for histogram buckets.
- Decide bucket range, likely 0–130 dB in 10 dB intervals.
- Add chart component to Session Detail.
- Add export/report inclusion if required.
- Gate to Pro.

**Acceptance criteria:**

- Session Detail Pro shows histogram.
- Free users see locked preview or no Pro section according to UX.
- Bucket percentages sum correctly.
- Empty/short sessions handled.
- PDF can optionally include histogram if the scientific report requires it.

---

### 3.22 WAV raw audio recording and export

**Status:** Missing
**Source intent:** Pro users should be able to enable audio recording alongside measurements and export raw audio as WAV.

**Current state from `PROJECT.md`:**

- Export formats currently described: CSV, PDF, PNG.
- No audio recording toggle is described.
- No WAV export is described.
- Privacy notes state that no audio is recorded unless user explicitly enables WAV export, but current implementation does not describe that feature.

**Implementation requirements:**

- Add explicit Pro setting: Audio Recording / Record WAV alongside measurements.
- Add prominent privacy and storage warning.
- Persist PCM data only when enabled.
- Write valid WAV file with header.
- Tie WAV export to session detail or batch export.
- Make default OFF.
- Add retention/deletion behavior.
- Consider storage quota and large-file warnings.
- Ensure no background/passive/YAMNet features accidentally persist audio.

**Acceptance criteria:**

- Audio recording is off by default.
- User must explicitly enable it.
- Active sessions record audio only when enabled.
- WAV exports are playable.
- WAV export is available only for sessions that have recorded audio.
- User can delete recorded audio.
- CSV/PDF/PNG exports continue working without WAV.
- Privacy policy and in-app copy match actual behavior.

**Risks:**

- Recording audio changes the privacy posture of the app substantially.
- Large WAV files can quickly consume storage.
- Android 13+ media permissions may not be needed if using app storage + SAF, but export behavior must be verified.

---

### 3.23 Octave-band calibration

**Status:** Missing
**Source intent:** Settings should include Pro per-band offset adjustment for octave bands based on ISO center frequencies.

**Current state from `PROJECT.md`:**

- Microphone sensitivity offset exists.
- Frequency weighting exists.
- Octave-band calibration is not described.
- RTA/octave-band analysis is also missing.

**Implementation requirements:**

- Implement octave-band calibration model.
- Add DataStore or Room-backed calibration values.
- Add Settings detail screen with sliders per band.
- Apply offsets correctly in spectral/RTA/report calculations.
- Keep global mic sensitivity separate from band-specific offsets.

**Acceptance criteria:**

- Pro user can adjust each octave-band offset.
- Values persist.
- Reset to defaults works.
- Calibration affects appropriate spectral/RTA values.
- Free users cannot apply stored Pro-only calibration values through old preferences.

---

### 3.24 Calibration profiles

**Status:** Missing
**Source intent:** Pro users should be able to save/load named calibration profiles for different microphones or environments.

**Current state from `PROJECT.md`:**

- DataStore stores a single mic sensitivity offset and frequency weighting.
- No named profiles are described.

**Implementation requirements:**

- Add profile data model.
- Decide storage location:
  - DataStore for small JSON/proto-like preference blob, or
  - Room if profiles become structured and searchable.
- Add create, rename, delete, duplicate, and select operations.
- Include global offset, octave-band offsets, external microphone identifier if available.
- Add migration/default profile.

**Acceptance criteria:**

- Pro user can create named profile.
- Active profile affects measurement/calibration.
- Switching profile updates effective calibration.
- Deleting active profile falls back safely.
- Exports/reports include active profile or calibration offset where needed.

---

### 3.25 External microphone support

**Status:** Missing
**Source intent:** Pro users should be able to detect/select USB/Bluetooth audio inputs and use per-mic calibration profiles.

**Current state from `PROJECT.md`:**

- AudioRecord mono PCM16 exists.
- No input device selection is described.
- No external mic preference or profile mapping is described.

**Implementation requirements:**

- Add audio input device discovery.
- Add Settings UI to choose input.
- Use Android audio APIs to bind AudioRecord to selected device where supported.
- Handle hot-plug/unplug.
- Add per-device calibration profile mapping.
- Display active input in technical metadata if enabled.

**Acceptance criteria:**

- External microphone appears in Settings when connected.
- User can select it.
- Measurement uses selected device when supported.
- Unplugging falls back safely and informs user.
- Reports identify input device if scientific report requires it.
- Manual tests cover USB-C mic and Bluetooth if product supports Bluetooth.

**Risks:**

- Bluetooth microphones may have AGC/noise suppression and poor SPL reliability.
- Android device support varies. UI should communicate limitations.

---

### 3.26 Frequency weighting matrix mismatch: ITU-R 468 and B

**Status:** Mostly implemented, but spec/UI consistency check needed
**Source intent:** The complete spec originally lists A/C/Z in one Settings section, while the competitive addendum adds B and the current project implements A/B/C/Z/ITU-R 468.

**Current state from `PROJECT.md`:**

- A/B/C/Z/ITU-R 468 are implemented.
- Free/Pro table includes A/B/C/Z/ITU-R 468.
- Some planning tables still mention A/C/Z only.

**Implementation requirements:**

- Ensure UI copy, help text, exports, PDF labels, and settings chips consistently include all implemented weighting options.
- Ensure Free users are effectively locked to A-weighting if that is the intended behavior.
- Ensure reports label B and ITU-R 468 correctly.

**Acceptance criteria:**

- No UI still says only A/C/Z if B and ITU-R 468 are available.
- CSV/PDF/Health Connect notes use correct weighting label.
- Unit tests cover each weighting label path.

---

### 3.27 Dosimeter standard selector: NIOSH / OSHA

**Status:** Partial
**Source intent:** Settings should include a Pro `NIOSH` / `OSHA` toggle, default NIOSH.

**Current state from `PROJECT.md`:**

- NIOSH TWA/dose calculations are available for A-weighted Session Detail.
- No `dosimeter_standard` DataStore key is listed.
- No NIOSH/OSHA settings selector is described.
- OSHA 5 dB exchange-rate calculation is not described.

**Implementation requirements:**

- Add preference key.
- Add Settings UI.
- Add OSHA calculation support.
- Surface selected standard in Dosimeter Mode and PDF.
- Decide how historical sessions store standard used.

**Acceptance criteria:**

- Pro user can switch NIOSH/OSHA.
- Dose calculations change according to standard.
- Session records preserve the standard used at recording/report time.
- PDF includes selected standard.
- Tests cover both formula paths.

---

### 3.28 Extended exposure alerts

**Status:** Missing
**Source intent:** Pro users should have extended exposure alerts that monitor dosage over 8-hour periods.

**Current state from `PROJECT.md`:**

- Exposure alerts, peak warnings, notification threshold, and `NoiseAlertEvaluator` exist.
- Notification policy is described as limited.
- No extended 8-hour dose alert is described.

**Implementation requirements:**

- Build dose-based alert evaluator.
- Decide whether it runs only during active measurement or across passive/day windows.
- Add notification copy and settings controls.
- Avoid alarm fatigue.
- Connect to Dosimeter standard preference.

**Acceptance criteria:**

- Pro user can enable extended exposure alerts.
- Alerts trigger based on cumulative/projection logic, not only instantaneous dB.
- User can understand why an alert fired.
- Cooldown and notification schedule are respected if implemented.

---

### 3.29 Notification schedule

**Status:** Missing
**Source intent:** Pro users should configure when alerts are active.

**Current state from `PROJECT.md`:**

- Notification threshold and alerts exist.
- No schedule preference is listed.

**Implementation requirements:**

- Add schedule model.
- Add Settings UI for active hours/days.
- Apply schedule in alert evaluator.
- Decide behavior for critical peak warnings outside schedule.

**Acceptance criteria:**

- Alerts are suppressed outside schedule, except for explicitly allowed critical warnings.
- Time zone and daylight saving changes are handled.
- UI explains current schedule status.
- Tests cover schedule boundaries.

---

### 3.30 Feature toggles for Display & Features

**Status:** Missing
**Source intent:** Settings should include Pro toggles:

- Show Technical Metadata
- Enable Dosimeter Mode
- Enable Sound Type Detection
- Show Sleep Monitor Card

**Current state from `PROJECT.md`:**

- DataStore preferences include theme, alerts, threshold, mic sensitivity, frequency weighting, waveform style, refresh rate, lockscreen meter, Health Connect, heart rate overlay, debug force free, and Pro user.
- These feature toggles are not listed.

**Implementation requirements:**

- Add preference keys.
- Add Settings controls.
- Apply toggles to UI:
  - Hide/disable technical metadata.
  - Hide/show Dosimeter chip.
  - Turn ML inference on/off.
  - Hide/show Sleep Monitor card.
- Ensure Pro gating and toggles do not conflict.

**Acceptance criteria:**

- Toggles persist and apply immediately.
- Free users cannot enable Pro-only capabilities.
- Disabled Pro features do not run expensive background work.
- UI remains discoverable; hiding Pro features should not violate “never hide locked Pro features” unless user explicitly hid them.

---

### 3.31 Always-On Display and Keep Awake

**Status:** Missing
**Source intent:** Pro settings should include:

- Always-On Display during measurement
- Keep Awake / wake lock during long measurements

**Current state from `PROJECT.md`:**

- No `WAKE_LOCK` permission is listed.
- No Always-On or Keep Awake preference is listed.
- Sleep Monitor is missing.

**Implementation requirements:**

- Add `WAKE_LOCK` permission if using wake locks.
- Add settings.
- Use Activity window flags for keep-screen-on where possible.
- Use wake locks conservatively and only when necessary.
- Add visible battery warning.
- Integrate with Sleep Monitor active state.

**Acceptance criteria:**

- Pro user can keep screen on during active measurement.
- Wake lock is acquired/released reliably.
- No wake lock leak after stop, crash, or app background.
- Battery impact is explained.
- Tests or manual verification cover lifecycle interruptions.

---

### 3.32 Data & Export: clear history and batch export completeness

**Status:** Needs code verification / partial
**Source intent:** Settings Data & Export should include clear history and batch export.

**Current state from `PROJECT.md`:**

- CSV export from Settings exists.
- Local backup/restore exists.
- PDF and PNG are per-session from Session Detail.
- Clear history is not described.
- Batch export beyond CSV is not described.

**Implementation requirements:**

- Verify whether clear history exists in code.
- If missing, add a destructive action with confirmation and optional backup reminder.
- Decide supported batch export formats:
  - CSV all sessions,
  - ZIP containing CSV/PDF/PNG,
  - or only CSV.
- Make scope explicit in UI.

**Acceptance criteria:**

- User can clear history only after confirmation.
- Active measurement blocks clear/restore operations.
- Batch export behavior is documented and tested.
- Free users cannot use Pro-only exports if gated.

---

### 3.33 Hearing test room-noise and headphone pre-check

**Status:** Needs code verification / possible partial
**Source intent:** Hearing test setup should require headphones and perform a room noise check before starting.

**Current state from `PROJECT.md`:**

- Hearing test flow is implemented and Pro-gated.
- It is described as relative and non-clinical.
- The current description does not confirm a headphone requirement or room-noise pre-check.

**Implementation requirements:**

- Verify current setup screen and ViewModel.
- If missing, add headphone guidance.
- Add ambient noise pre-check using AudioEngine before tones.
- Block or warn if ambient noise is too high.
- Keep clinical disclaimer prominent.

**Acceptance criteria:**

- User sees headphone guidance before starting.
- Ambient room noise is checked.
- Loud environments show warning or prevent test start.
- Results remain clearly “relative personal tracking,” not clinical audiogram.

---

### 3.34 Pro lock previews for all planned Pro features

**Status:** Partial
**Source intent:** Pro features should never be hidden completely. They should show preview with lock overlay and upgrade CTA.

**Current state from `PROJECT.md`:**

- Analytics Pro cards use locked previews.
- `ProLockOverlay` exists.
- Features that are not implemented cannot yet show previews.

**Implementation requirements:**

- For each Pro feature, define one of:
  - locked preview,
  - disabled card with upgrade CTA,
  - settings row with lock.
- Ensure hidden-user-toggle behavior does not conflict with discoverability.
- Standardize copy.

**Acceptance criteria:**

- Free users can discover Dosimeter, Camera Overlay, Sleep Monitor, Spectral, Environment, Sound Detection, PDF, CSV, widget, lock-screen meter, calibration, etc.
- Locked previews do not compute or expose Pro data.
- Tapping lock opens the purchase sheet.

---

## 4. Competitive addendum gaps

### 4.1 Scientific PDF report completion

**Status:** Partial
**Source intent:** Competitive P0 PDF should include:

Page 1 Summary:

- Session name
- Date/time
- Duration
- Location if enabled
- Device manufacturer/model/OS version
- Calibration offset
- Frequency weighting
- Response time
- Dosimeter standard if dosimeter mode

Page 2 Metrics:

- LAeq or weighting equivalent
- TWA 8-hour projected
- Dose %
- Projected dose %
- LCpeak
- MIN/MAX/AVG
- Octave-band breakdown if recorded
- Compliance assessment under/over NIOSH REL or OSHA PEL

Page 3 Time Series:

- High-resolution chart
- Y-axis 0–130 dB
- 10 dB gridlines
- Peak events marked

Page 4+ Peak Events:

- Timestamp
- Peak dB
- Duration
- Sound type if YAMNet detected something

Footer:

- Generated by dBcheck version
- Calibration disclaimer
- Page number

**Current state from `PROJECT.md`:**

- Native `PdfDocument` export exists.
- PDF has summary, metrics, time series, peak events, and optional heart-rate page.
- The missing source data/features include location, response time, dosimeter standard selection, octave-band breakdown, and YAMNet sound type.
- Device info, calibration offset, compliance assessment, and footer are not confirmed.

**Implementation requirements:**

- Audit current `ExportPdfReportUseCase`.
- Add all missing fields that are already available.
- Implement missing upstream features needed for unavailable fields.
- Add compliance assessment logic.
- Add calibration disclaimer footer to every page.
- Ensure PDF text uses safe fallback values when optional data is absent.

**Acceptance criteria:**

- PDF includes required summary and metrics fields.
- PDF clearly states limitations.
- PDF never shows blank/misleading zeros for unavailable values.
- Page numbering and footer appear on every page.
- Tests cover report text formatting and field presence.
- Manual PDF open/share test passes on device.

---

### 4.2 Lock-screen live meter public visibility decision

**Status:** Deviation
**Source intent:** Competitive addendum says custom lock-screen notification should use `VISIBILITY_PUBLIC` so live content shows on lock screen.

**Current state from `PROJECT.md`:**

- Current `NotificationPrivacyPolicy.measurementLockscreenVisibility()` returns `VISIBILITY_PRIVATE`.
- Pro lock-screen meter uses custom RemoteViews, but live dB content is not published as public lock-screen content.

**Decision required:**

Choose one of these paths:

1. **Follow addendum:** Use public visibility when Pro lock-screen meter is enabled.
2. **Keep current privacy-first behavior:** Keep private visibility and update the planning/addendum language.
3. **Add user choice:** “Show live dB on lock screen” with a privacy warning, default OFF.

**Recommended path:**

Use option 3. It preserves privacy by default while allowing the competitive premium experience for users who intentionally enable it.

**Implementation requirements if choosing option 3:**

- Add separate setting:
  - `lockscreen_meter_enabled`
  - `show_lockscreen_meter_publicly`
- Default public visibility OFF.
- Explain that anyone seeing the lock screen can read current dB/session data.
- Apply public visibility only when both settings are enabled and user is Pro.

**Acceptance criteria:**

- Free notification remains basic/private.
- Pro private mode shows rich notification only after unlock or according to Android private behavior.
- Pro public mode shows live dB on lock screen.
- User can turn public visibility off.
- Privacy behavior is covered by tests.

---

### 4.3 Audible Threshold Alarm

**Status:** Missing
**Priority in addendum:** P1
**Source intent:** Pro feature for audible warning when sound exceeds threshold for a configured duration.

Planned controls:

- Audible alarm on/off
- Threshold 60–110 dB
- Duration 1–30 seconds
- Alarm sound selector
- Preview per sound
- Custom recorded alarm
- Cooldown
- Alarm volume
- Only alarm when screen is on / pocket guard

**Current state from `PROJECT.md`:**

- Noise notifications and threshold setting exist.
- No audible alarm, SoundPool/MediaPlayer alarm playback, alarm settings section, custom alarm recording, or pocket guard is described.

**Implementation requirements:**

- Add `ThresholdAlarmPolicy`.
- Add alarm evaluator in measurement loop.
- Use `SoundPool` or `MediaPlayer` with `AudioAttributes.USAGE_ALARM`.
- Add bundled sounds.
- Add custom recording only if product accepts the added privacy/storage scope.
- Add screen-on/proximity guard.
- Add cooldown logic.
- Add Settings section.

**Acceptance criteria:**

- Alarm triggers only when threshold is exceeded for configured duration.
- Alarm does not repeatedly fire without cooldown.
- Default guard prevents pocket/face-down surprises.
- User can preview alarm sounds.
- Alarm respects Pro gating.
- Alarm errors do not crash measurement.

**Risks:**

- Audible alarms can be socially disruptive.
- Custom recorded alarm means storing user audio; privacy copy must be updated.

---

### 4.4 Voice Volume Detection

**Status:** Missing
**Priority in addendum:** P2
**Source intent:** Detect when the user is speaking too loudly using YAMNet Speech classification plus a personal baseline.

**Current state from `PROJECT.md`:**

- YAMNet is not implemented.
- No voice baseline flow exists.
- No voice-volume warnings are described.

**Implementation requirements:**

- Complete YAMNet first.
- Add voice baseline calibration flow.
- Add speech-frame sustained detection.
- Add haptic or notification feedback.
- Add position/reliability guidance.
- Add Analytics trend if desired.

**Acceptance criteria:**

- User can calibrate normal voice baseline.
- App warns only when speech is detected and volume exceeds baseline + offset.
- User can adjust sensitivity.
- UI clearly states reliability depends on phone position.
- No raw voice audio is stored.

---

### 4.5 Passive Background Monitoring

**Status:** Missing
**Priority in addendum:** P2
**Source intent:** Periodically sample ambient sound in short bursts during the day, then summarize exposure.

**Current state from `PROJECT.md`:**

- WorkManager exists only as a dependency/constraint.
- No passive monitoring worker/service, daily summary, or periodic microphone capture is described.
- `PROJECT.md` notes Android microphone foreground-service restrictions.

**Implementation requirements:**

- Add explicit Pro toggle and first-run explanation.
- Use short foreground microphone service pattern.
- Sample for defined windows.
- Compute LAeq and discard audio buffers.
- Write passive samples to Room.
- Add daily summary notification.
- Add Analytics card.

**Acceptance criteria:**

- Passive monitoring only runs after explicit opt-in.
- Audio is not persisted.
- Battery impact is disclosed.
- Android 14+ restrictions are respected.
- OEM limitations are documented.
- User can disable it immediately.

**Risks:**

- Android background microphone restrictions are strict.
- Some OEMs will kill background tasks.
- Play policy scrutiny is higher for background microphone use.

---

### 4.6 TTS Detection — Temporary Threshold Shift Monitoring

**Status:** Missing
**Priority in addendum:** P2
**Source intent:** Use YAMNet + dosimeter + hearing test baseline to prompt a short hearing check after risky loud events.

**Current state from `PROJECT.md`:**

- Hearing test exists.
- Dosimeter is only partial in Session Detail.
- YAMNet is missing.
- No TTS checks, post-session worker, baseline comparison, or recovery tracker is described.

**Implementation requirements:**

- Complete YAMNet.
- Complete Dosimeter Mode/dose threshold logic.
- Require at least one full hearing test baseline.
- Add TTS trigger logic.
- Add post-event notification.
- Add shortened hearing check flow.
- Add Room table for TTS results.
- Add Analytics recovery tracker.

**Acceptance criteria:**

- Risk events are detected only when dose + sound type criteria are met.
- No baseline means user is invited to take full hearing test.
- Shortened test compares against baseline.
- Results use cautious, non-diagnostic language.
- Recovery trend is visible to Pro users.

**Risks:**

- This is health-adjacent and must be very carefully worded.
- False reassurance is dangerous; disclaimers and thresholds must be conservative.

---

### 4.7 Tinnitus Matcher and Sound Therapy

**Status:** Missing
**Priority in addendum:** P3
**Source intent:** Future tinnitus feature set:

- Tinnitus pitch matcher
- Ear-specific profile
- White/pink/brown noise
- Notched noise
- Nature sounds
- Custom mix
- Background playback
- Sleep timer
- Tinnitus journal
- Exposure correlation

**Current state from `PROJECT.md`:**

- `ToneGenerator` exists for hearing test.
- No tinnitus UI, journal, sound therapy, background playback, or notched-noise engine is described.

**Implementation requirements:**

- Decide whether this belongs in v1.5/v2.0, not v1.0.
- Preserve 4-tab navigation by using full-screen flow/CTA, unless product deliberately changes navigation.
- Build tone/noise generation.
- Add playback service and media notification.
- Add journal data model.
- Add safety copy: tinnitus care is not medical treatment.

**Acceptance criteria:**

- User can match tinnitus pitch.
- Sound therapy plays reliably in background.
- Sleep timer stops playback.
- Journal stores severity/triggers if enabled.
- Audio output levels are safe and user-controlled.
- The feature does not undermine the core sound-meter positioning.

---

## 5. Design and interaction gaps from `dBcheck_design_spec.md`

### 5.1 Full design compliance across all screens

**Status:** Validation gap
**Source intent:** Every screen should follow:

- Auditory Observatory creative direction
- Tonal layering instead of borders
- Custom theme tokens
- Breathing room
- Editorial typography
- Organic shapes
- Signature gradient
- Technical credibility through data transparency

**Current state from `PROJECT.md`:**

- Design system and component library are implemented.
- Screenshot baselines exist for components.
- `PROJECT.md` does not prove every screen follows every visual rule.

**Implementation requirements:**

- Audit every screen in both dark and light themes.
- Remove remaining hard-coded colors/spacing if any.
- Verify no divider/border-heavy layouts violate the no-line rule.
- Verify all cards use appropriate tonal surfaces and radius tokens.
- Verify Pro lock overlays look consistent.

**Acceptance criteria:**

- Screen-by-screen design audit is documented.
- All screens use theme tokens where tokens exist.
- No hard-coded visual values remain without reason.
- Both themes preserve identical layout.

---

### 5.2 Tabular numeric figures

**Status:** Needs code verification
**Source intent:** All numeric data should use Space Grotesk with tabular figures (`tnum`), and number/unit styling should be separated.

**Current state from `PROJECT.md`:**

- Manrope and Space Grotesk are used.
- It does not confirm OpenType tabular figure feature usage everywhere.
- It does not confirm all numeric values split unit styling exactly as spec says.

**Implementation requirements:**

- Audit numeric composables.
- Add text style helper for tabular figures if missing.
- Ensure dB, Hz, kHz, %, BPM, time, and date metrics use data typography.
- Split units using `unit` style where visually appropriate.

**Acceptance criteria:**

- Numeric values do not shift width while updating.
- Meter live number is stable.
- Stat cards and charts use consistent numeric typography.
- Screenshot diff confirms no jitter-prone layout.

---

### 5.3 Complete empty/loading/error states

**Status:** Partial / validation gap
**Source intent:** Every screen should have:

- Empty states
- Loading/skeleton states
- Error states
- Microphone denied flow
- Measurement failed retry

**Current state from `PROJECT.md`:**

- `SkeletonLoader` and `EmptyState` exist.
- Analytics/History/Session Detail/Hearing Test have some explicit error handling.
- Full coverage for all current and planned screens is not proven.
- Camera/Sleep/YAMNet/etc. are missing and therefore their states are also missing.

**Implementation requirements:**

- Create state coverage matrix by screen.
- Add missing empty/loading/error states.
- Ensure user-facing error messages use resource IDs and `UserFacingError` mapping.
- Avoid raw exception text in UI.

**Acceptance criteria:**

- Every screen has a defined state model.
- Every async operation has loading and failure state.
- All user-facing text is resourced.
- Screenshot tests cover representative states.

---

### 5.4 Pull-to-refresh

**Status:** Missing / not described
**Source intent:** Analytics and History should support pull-to-refresh with custom waveform ripple animation.

**Current state from `PROJECT.md`:**

- Analytics and History data flows exist.
- Pull-to-refresh is not described.

**Implementation requirements:**

- Decide if pull-to-refresh makes sense with local Room data.
- If implemented, refresh derived data and external Health Connect state if relevant.
- Use custom animation or align with current design system.

**Acceptance criteria:**

- Pull gesture works on Analytics and History.
- Refresh state is visible and accessible.
- It does not fight normal scrolling.
- Tests cover state changes where feasible.

---

### 5.5 Haptics

**Status:** Partial / needs verification
**Source intent:** Haptics should include:

- Light haptic on 85 dB threshold crossing
- Medium haptic on new MAX peak detection

**Current state from `PROJECT.md`:**

- `VIBRATE` permission is listed.
- The exact threshold/peak haptic behavior is not confirmed.

**Implementation requirements:**

- Audit current haptic behavior.
- Add threshold crossing haptic with cooldown.
- Add peak haptic if not too noisy.
- Add setting if users need to disable haptics.

**Acceptance criteria:**

- Haptic triggers once per meaningful crossing, not every frame.
- Peak haptic does not spam.
- Behavior can be disabled if necessary for accessibility/user preference.
- Manual device test confirms it works.

---

### 5.6 Animation polish

**Status:** Validation gap
**Source intent:** Gauge arc, breathing pulse, peak flash/glow, card fade/slide, and transitions should feel premium.

**Current state from `PROJECT.md`:**

- Design system exists.
- Screenshot tests may disable animations.
- Full animation polish is not verified.

**Implementation requirements:**

- Audit animation behavior on device.
- Verify gauge easing and update cadence.
- Verify peak flash/glow.
- Verify reduced motion/accessibility if supported.
- Avoid animation jank during measurement.

**Acceptance criteria:**

- Meter updates smoothly under active AudioRecord load.
- Animations do not hurt battery or accessibility.
- Reduced motion setting is respected if implemented.
- No infinite animation causes screenshot nondeterminism without a deterministic test path.

---

### 5.7 Accessibility audit

**Status:** Validation gap / not complete
**Source intent:** App should be accessible, with semantic descriptions for charts/buttons and resourced strings.

**Current state from `PROJECT.md`:**

- Accessibility-related strings and plural tests exist.
- `PROJECT.md` explicitly says a comprehensive accessibility audit has not been done.

**Implementation requirements:**

- Run TalkBack audit.
- Check contrast in both themes.
- Add semantic descriptions for all charts.
- Ensure controls have roles/states.
- Ensure Pro locks are understandable without color.
- Ensure dynamic dB updates are not too noisy for screen readers.
- Verify touch target sizes.

**Acceptance criteria:**

- Accessibility audit checklist is completed.
- Critical flows work with TalkBack:
  - Meter start/stop
  - Session Detail export/share
  - Settings Pro purchase
  - Hearing Test
  - Health Connect toggle
- Charts expose textual summaries.
- Contrast passes WCAG guidance where applicable.

---

### 5.8 Localization

**Status:** Missing / validation gap
**Source intent:** Text should be resource-based and production-ready.

**Current state from `PROJECT.md`:**

- Default English strings are broadly resourced.
- No translation resource folders exist.
- `PROJECT.md` states localization is not ready.

**Implementation requirements:**

- Decide launch locales.
- Freeze English copy before translation.
- Add locale folders.
- Avoid hard-coded strings.
- Verify pluralization.
- Check layout expansion for longer languages.

**Acceptance criteria:**

- All user-facing strings are externalized.
- Target launch translations exist if required.
- No clipped text in translated layouts.
- Store listing language matches in-app language plan.

---

## 6. Technical, permissions, and architecture gaps

### 6.1 Missing manifest permissions for planned features

**Status:** Missing
**Planned permissions not present in current manifest description:**

- `CAMERA` for Camera Overlay
- `ACCESS_COARSE_LOCATION` for session location
- `WAKE_LOCK` for Keep Awake / Sleep Monitor

**Implementation requirements:**

- Add only when the corresponding feature is implemented.
- Use runtime requests with rationale for dangerous permissions.
- Keep optional features optional.
- Update privacy policy and Play Data Safety disclosures.

**Acceptance criteria:**

- Permissions are minimal and justified.
- Denial states are graceful.
- Play policy copy matches actual behavior.

---

### 6.2 Missing libraries for planned features

**Status:** Missing
**Planned dependencies not present in current stack description:**

- CameraX for Camera Overlay
- TensorFlow Lite for YAMNet
- Potential media/playback support for alarms/tinnitus therapy

**Implementation requirements:**

- Add dependencies through version catalog.
- Update dependency locking.
- Run security/dependency scans.
- Add license attribution where required.

**Acceptance criteria:**

- Build succeeds.
- Dependency lockfiles/verification updated.
- Licenses are documented.
- No unacceptable CVEs or policy risks.

---

### 6.3 Room schema expansion

**Status:** Missing for several planned features
**Features requiring schema changes:**

- Session location
- WAV/audio recording metadata
- Passive monitoring samples
- YAMNet detections if persisted
- TTS check results
- Tinnitus journal
- Calibration profiles if using Room
- Sleep Monitor specific metadata/events

**Implementation requirements:**

- Plan migrations carefully.
- Keep deterministic DAO orderings.
- Preserve backup/restore validation.
- Add schema JSON exports.
- Add migration tests.

**Acceptance criteria:**

- Database version increments correctly.
- Migration tests pass from all prior versions.
- Existing data survives.
- Backup/restore still validates.

---

### 6.4 Pro gate enforcement in execution/data paths

**Status:** Ongoing requirement
**Current strength:** `PROJECT.md` emphasizes Pro gate checks in execution/data paths for several features.

**New risk:** Every missing Pro feature must follow the same rule.

**Implementation requirements for new Pro features:**

- Do not gate only UI.
- Gate actual execution:
  - Camera capture
  - Dosimeter live calculations if considered Pro-only
  - Sleep Monitor service start
  - Sound detection inference
  - WAV recording
  - External mic selection
  - Calibration profiles
  - Passive monitoring
  - Audible alarms
  - TTS detection
- Ensure old stored preferences cannot activate Pro behavior for Free users.

**Acceptance criteria:**

- Free users cannot run Pro code paths through deep links, restored state, or stored preferences.
- Pro unlock applies immediately.
- Debug force-free catches gate regressions.

---

### 6.5 Privacy policy and Data Safety updates

**Status:** Validation gap for planned features
**Current state from `PROJECT.md`:**

- Privacy-sensitive config exists.
- Current implementation avoids raw audio recording except planned WAV feature.
- Health Connect behavior is documented.

**Planned features that require privacy review:**

- WAV audio recording
- Camera Overlay
- Session location
- YAMNet sound detection
- Voice Volume Detection
- Passive Monitoring
- TTS Detection
- Tinnitus Journal
- Custom alarm recording
- Public lock-screen content

**Implementation requirements:**

- Update privacy policy.
- Update Play Data Safety.
- Add in-app explanations.
- Keep opt-in defaults for sensitive features.
- Make deletion/export behavior clear.

**Acceptance criteria:**

- Policy text matches actual data collection.
- No hidden microphone/camera/location behavior.
- Sensitive features have explicit consent.
- User can disable and delete relevant data.

---

## 7. Release-readiness gaps explicitly stated by `PROJECT.md`

### 7.1 Device-level audio and foreground-service verification

**Status:** Validation gap
**Current state:** `PROJECT.md` states that the app is not release-ready without device-level audio and foreground-service verification.

**Required verification:**

- First mic permission flow.
- Start/stop/reset measurement.
- Foreground notification creation.
- Android 14+ foreground service microphone type behavior.
- Process death and interrupted-session recovery.
- Long-running active sessions.
- Notification stop action.
- Lock-screen behavior.
- Background/foreground transitions.

**Acceptance criteria:**

- Test matrix covers at least representative Android versions and OEMs.
- No service start crashes.
- No microphone capture before foreground promotion.
- Recovery behavior matches documented policy.

---

### 7.2 Play Billing production verification

**Status:** Validation gap
**Current state:** Billing backend and Settings purchase flow exist, but production Play Billing setup must be verified.

**Required verification:**

- Product ID `dbcheck_pro` exists in Play Console.
- Product type is one-time INAPP.
- Purchase, pending, cancelled, already-owned, and failed flows work.
- Acknowledgement works.
- Restore works.
- Effective entitlement is correct after app restart.
- Release build uses real billing environment.

**Acceptance criteria:**

- License tester purchase succeeds.
- Pending does not unlock Pro.
- Already-owned restores Pro.
- Purchase is acknowledged.
- No debug force-free/pro behavior leaks into release.

---

### 7.3 Release signing and Play Store packaging

**Status:** Validation gap
**Current state:** Release signing is configured, but secrets and release verification must be checked.

**Required verification:**

- Keystore secrets configured in CI.
- AAB builds successfully.
- jarsigner/apksigner verification passes.
- Version code/name are correct.
- R8/minification behavior is acceptable.
- Native/model/assets, if added later, are included correctly.

**Acceptance criteria:**

- Release AAB is signed.
- Play Console accepts upload.
- No signing secrets are committed.
- CI fails explicitly if partial signing config exists.

---

### 7.4 Acoustic and clinical limitations documentation

**Status:** Validation gap
**Current state:** `PROJECT.md` lists known limitations:

- Device microphone measurement is not certified SPL without calibration.
- Hearing test is relative, not calibrated dB HL audiometry.
- Weighting coefficients have unit tests but no full reference-instrument verification.

**Required work:**

- Finalize in-app disclaimers.
- Finalize PDF footer disclaimer.
- Finalize Play Store description language.
- Add help/FAQ explaining accuracy limits.
- Avoid medical claims.
- Avoid occupational compliance claims unless calibrated external mic workflow is completed.

**Acceptance criteria:**

- Meter, Hearing Test, PDF, and store listing use consistent limitation language.
- No copy says the phone is a certified Class 1/2 meter by default.
- Hearing test results never imply diagnosis.

---

### 7.5 Instrumentation tests

**Status:** Missing
**Current state:** `PROJECT.md` states there is no `androidTest` source set.

**Required work:**

- Add instrumentation tests for permission flows, navigation, share/export intents, and service lifecycle where feasible.
- Keep unit tests for pure logic.
- Use fake services or test doubles for hard-to-control Android APIs.

**Acceptance criteria:**

- Critical user flows have at least smoke-level device/emulator coverage.
- CI can run a stable subset if infrastructure allows.
- Manual test plan covers what cannot be automated.

---

### 7.6 Gradle test suite execution for final audit

**Status:** Validation gap
**Current state:** `PROJECT.md` says Gradle tests were not run during that documentation update.

**Required work:**

- Run:
  - unit tests
  - lint
  - detekt/ktlint
  - screenshot validation
  - security checks where applicable
- Record results in project status.

**Acceptance criteria:**

- Latest test run is documented.
- Failures are triaged.
- Release branch has green required checks.

---

### 7.7 Qodana workflow compatibility

**Status:** Validation gap / known risk
**Current state:** Qodana workflow is `continue-on-error` due to AGP compatibility issue.

**Required work:**

- Revisit Qodana version compatibility.
- Remove `continue-on-error` only when stable.
- Keep security/static analysis coverage through other workflows until then.

**Acceptance criteria:**

- Qodana status is intentionally documented.
- Release quality does not depend on a silently failing check.

---

## 8. Priority implementation roadmap

This roadmap is based on launch value, dependency order, and risk.

### 8.1 Highest priority: core v1.0 scope completion

These are the features most directly promised by the complete spec as v1.0 app capabilities.

1. Meter internal mode chips
2. Dosimeter Mode
3. Meter 30-second time-series chart
4. Sound reference card
5. Response time selector/default
6. Live LAeq in Meter
7. Analytics chips
8. Spectral mode completion: Bars / Spectrogram / RTA
9. YAMNet Sound Type Detection
10. Real-time Environment Mix
11. Camera Overlay
12. Sleep Monitor
13. Session location
14. Histogram
15. WAV recording/export
16. Settings gaps:
    - octave-band calibration
    - calibration profiles
    - external mic
    - dosimeter standard
    - extended alerts
    - notification schedule
    - feature toggles
    - Always-On / Keep Awake
17. History search/filter
18. Hearing tips
19. Scientific PDF completion

### 8.2 Highest priority validation before Play Store release

1. Device-level audio + foreground service testing
2. Billing production verification
3. Release signing verification
4. Permission denial/permanent denial flows
5. Export/share flows
6. Accessibility audit
7. Localization decision and implementation
8. Acoustic/clinical limitation copy
9. Latest Gradle test/lint/security run

### 8.3 Competitive addendum queue

P0 / pre-launch:

1. Scientific PDF completion
2. Lock-screen visibility decision
3. B-weighting is already implemented
4. Health Connect is already implemented within current Health Connect data-type limitations

P1 / pre-launch if capacity allows:

1. Audible Threshold Alarm

P2 / post-launch v1.x:

1. Voice Volume Detection
2. Passive Background Monitoring
3. TTS Detection

P3 / v1.5-v2.0:

1. Tinnitus Matcher and Sound Therapy

---

## 9. Feature-by-feature dependency map

| Feature | Depends on | Blocks / affects |
|---|---|---|
| Dosimeter Mode | Dose calculator, Pro gating, NIOSH/OSHA setting | PDF, TTS Detection, extended alerts |
| Response Time | Audio calculation pipeline | PDF, Meter metadata, scientific credibility |
| Camera Overlay | CameraX, CAMERA permission, live dB state | Privacy policy, Play Data Safety |
| Sleep Monitor | Foreground service robustness, wake/screen behavior | History, exports, Settings toggles |
| Spectrogram | FFT/spectral frame pipeline | Spectral sub-section |
| RTA | Octave-band analyzer | Octave calibration, scientific PDF |
| YAMNet | TFLite, model asset, transient audio frames | Sound Detection, Voice Volume, TTS, PDF peak labels |
| Session Location | Location permission, schema migration | History cards, PDF, CSV |
| WAV Export | Audio recording storage, schema/metadata, privacy copy | Export UI, Play Data Safety |
| Calibration Profiles | Data model, Pro gating | External mic support, PDF |
| External Mic | Audio device selection, profiles | Accuracy claims, calibration |
| Passive Monitoring | Android background mic constraints | Daily summary, Analytics, privacy |
| TTS Detection | YAMNet, Dosimeter, Hearing Test baseline | Hearing recovery analytics |
| Tinnitus Tools | ToneGenerator/audio playback | New journal/storage, media notification |

---

## 10. Suggested implementation tickets

The following ticket list is written in a format that can be copied into Linear/GitHub issues.

### Ticket 1 — Add Meter mode chips

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add `dB Meter` / `Dosimeter` segmented chips to Meter.

**Tasks:**

- Add Meter mode UI state.
- Add chip row.
- Gate Dosimeter chip to Pro.
- Preserve active measurement session while switching modes.
- Add tests and screenshots.

**Done when:**

- Free and Pro states work.
- Dosimeter chip exists and is discoverable.
- No navigation regression.

---

### Ticket 2 — Implement live Dosimeter Mode

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Build live dose gauge, TWA, dose %, projected dose, LAeq, exposure time remaining, dose chart, and standard display.

**Tasks:**

- Add dosimeter domain calculator.
- Add NIOSH and OSHA calculation support.
- Add live accumulator.
- Add UI.
- Add report consistency tests.

**Done when:**

- Pro Meter can display live dose.
- Values match completed Session Detail report.
- Tests cover formulas and edge cases.

---

### Ticket 3 — Add NIOSH/OSHA Settings selector

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Add Pro setting for dosimeter standard.

**Tasks:**

- Add DataStore key.
- Add UI toggle.
- Add effective preference policy.
- Add report and PDF inclusion.
- Add tests.

**Done when:**

- Standard selection affects live and completed dose calculations.

---

### Ticket 4 — Add live 30-second dB chart to Meter

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add rolling live time-series chart in dB Meter mode.

**Tasks:**

- Add rolling buffer.
- Add Canvas chart.
- Add threshold line and peak markers.
- Add empty/paused states.
- Add screenshot tests.

**Done when:**

- Chart behaves correctly under start/pause/reset and does not grow memory.

---

### Ticket 5 — Add Sound Reference Card

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add expandable dB reference card below Meter stats.

**Tasks:**

- Add reference model.
- Add collapsed/expanded UI.
- Add current marker.
- Add nearest-reference highlight.
- Add accessibility text.

**Done when:**

- Free and Pro users can use the card and understand current dB context.

---

### Ticket 6 — Add Response Time setting and calculation support

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add Slow/Fast/Impulse RMS response time independent of UI refresh rate.

**Tasks:**

- Add domain enum and DataStore preference.
- Modify audio calculation pipeline.
- Add Meter quick selector if required.
- Add Settings selector.
- Add tests.

**Done when:**

- Response time changes measurement behavior and reports show selected value.

---

### Ticket 7 — Add live LAeq to Meter

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Show equivalent level during active session for Pro users.

**Tasks:**

- Extract active energy average.
- Add UI.
- Add Pro gate.
- Match Session Detail value after stop.

**Done when:**

- Live LAeq/equivalent value is stable, correct, and labeled by weighting.

---

### Ticket 8 — Add Analytics sub-section chips

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Split Analytics into Overview, Spectral, and Environment.

**Tasks:**

- Add chip state.
- Move cards to matching sections.
- Add locked previews.
- Preserve existing data.

**Done when:**

- Analytics structure matches planned navigation.

---

### Ticket 9 — Complete Spectral feature set

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Add Bars/Spectrogram/RTA modes and stat pills.

**Tasks:**

- Keep existing spectrum as Bars.
- Add spectrogram buffer and UI.
- Add octave-band RTA.
- Add stat pills.
- Add performance tests.

**Done when:**

- Pro users can switch modes and all modes update live.

---

### Ticket 10 — Add YAMNet Sound Type Detection

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add on-device ML sound classification in Environment section.

**Tasks:**

- Add TFLite/YAMNet dependencies and model.
- Add classifier pipeline.
- Add Settings toggle.
- Add UI and recent detections.
- Add privacy/battery copy.

**Done when:**

- Detection works, can be disabled, and never persists raw audio by default.

---

### Ticket 11 — Make Environment Mix real-time

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Add active-session Environment Mix or clearly separate real-time and historical mix.

**Tasks:**

- Add active distribution accumulator.
- Add UI update flow.
- Rename historical card if needed.

**Done when:**

- Users can see real-time noise category distribution during active measurement.

---

### Ticket 12 — Implement Camera Overlay

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add full-screen Pro camera preview with dB overlay, photo capture, video capture if required, and sharing.

**Tasks:**

- Add CameraX.
- Add CAMERA permission.
- Add route and UI.
- Add overlay rendering.
- Add capture/share.
- Add tests/manual verification.

**Done when:**

- Pro users can capture and share media with visible dB overlay.

---

### Ticket 13 — Implement Sleep Monitor

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add full-screen overnight noise-monitoring flow.

**Tasks:**

- Add setup/active/results screens.
- Add foreground service behavior.
- Add sleep session metadata.
- Add notable events.
- Add exports.
- Add wake/screen settings integration.

**Done when:**

- Pro users can record overnight noise and see results in History.

---

### Ticket 14 — Add Session Location

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add optional Pro location tagging for sessions.

**Tasks:**

- Add permission.
- Add schema migration.
- Add reverse geocoding.
- Add UI/report/export fields.
- Add privacy controls.

**Done when:**

- Sessions can include optional location without blocking app use.

---

### Ticket 15 — Add Session Histogram

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add Pro dB distribution histogram to Session Detail.

**Tasks:**

- Add histogram calculator.
- Add chart.
- Add report/export support if needed.
- Add tests.

**Done when:**

- Users can understand percentage of session time in dB buckets.

---

### Ticket 16 — Add WAV Recording and Export

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add opt-in Pro raw audio recording and WAV export.

**Tasks:**

- Add setting and privacy warning.
- Add PCM storage path.
- Add WAV writer.
- Add export action.
- Add deletion behavior.
- Update privacy policy.

**Done when:**

- WAV export works only for sessions explicitly recorded with audio.

---

### Ticket 17 — Add Octave-Band Calibration

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add per-octave offset calibration.

**Tasks:**

- Add model and persistence.
- Add Settings detail screen.
- Apply to spectral/RTA/report.
- Add tests.

**Done when:**

- Pro users can tune per-band offsets and reset them.

---

### Ticket 18 — Add Calibration Profiles

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add named calibration profiles.

**Tasks:**

- Add data model.
- Add create/select/rename/delete UI.
- Integrate with mic sensitivity and octave calibration.
- Add external mic mapping later.

**Done when:**

- Profiles persist and selected profile affects measurement.

---

### Ticket 19 — Add External Microphone Support

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add input selection and per-mic calibration.

**Tasks:**

- Discover audio devices.
- Bind AudioRecord to selected device.
- Handle unplug/fallback.
- Add UI and report fields.
- Test USB/Bluetooth.

**Done when:**

- User can select supported external input and measurement uses it.

---

### Ticket 20 — Add Extended Exposure Alerts

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add dose-based 8-hour exposure alerts.

**Tasks:**

- Add evaluator.
- Add settings.
- Add notification copy.
- Integrate with dosimeter standard.

**Done when:**

- Alerts trigger from cumulative/projection logic.

---

### Ticket 21 — Add Notification Schedule

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add active hours/days for alerts.

**Tasks:**

- Add schedule model.
- Add UI.
- Apply in alert evaluator.
- Add tests.

**Done when:**

- Alerts respect schedule reliably.

---

### Ticket 22 — Add Display/Feature Toggles

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add toggles for technical metadata, Dosimeter, Sound Detection, Sleep card.

**Tasks:**

- Add DataStore keys.
- Add settings rows.
- Apply UI and execution behavior.
- Add tests.

**Done when:**

- Toggles persist, apply immediately, and cannot bypass Pro gates.

---

### Ticket 23 — Add Always-On / Keep Awake

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add screen/wake behavior controls for long sessions.

**Tasks:**

- Add permission if needed.
- Add settings.
- Add lifecycle-safe acquisition/release.
- Add battery warning.

**Done when:**

- No wake locks leak and settings behave as expected.

---

### Ticket 24 — Add Hearing Tips

**Type:** Feature
**Priority:** P0
**Status:** Missing
**Summary:** Add View Tips content from Hearing Health card.

**Tasks:**

- Add content.
- Add bottom sheet or expanded section.
- Add resources and accessibility.
- Add disclaimer.

**Done when:**

- Users can access practical hearing protection guidance.

---

### Ticket 25 — Add History Search and Tag Filtering

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Add Pro search/filter for unlimited history.

**Tasks:**

- Add search UI.
- Add tag filter.
- Add DAO query support.
- Add empty states.

**Done when:**

- Pro users can search/filter sessions without bypassing Free history policy.

---

### Ticket 26 — Complete Scientific PDF Report

**Type:** Feature
**Priority:** P0
**Status:** Partial
**Summary:** Add all competitive report fields, compliance assessment, and footer.

**Tasks:**

- Audit existing PDF.
- Add missing available fields.
- Add fallback copy for unavailable fields.
- Add compliance logic.
- Add footer/page numbers.
- Add tests.

**Done when:**

- PDF meets the competitive addendum format as far as source data allows.

---

### Ticket 27 — Decide and implement lock-screen public visibility policy

**Type:** Product/Privacy decision + implementation
**Priority:** P0
**Status:** Deviation
**Summary:** Resolve conflict between competitive addendum public lock-screen content and current private policy.

**Tasks:**

- Choose privacy policy.
- Add user setting if public mode is allowed.
- Update docs and tests.

**Done when:**

- Behavior is intentional, tested, and documented.

---

### Ticket 28 — Add Audible Threshold Alarm

**Type:** Feature
**Priority:** P1
**Status:** Missing
**Summary:** Add Pro audible warning when dB exceeds threshold for duration.

**Tasks:**

- Add settings.
- Add evaluator.
- Add alarm playback.
- Add cooldown and screen-on guard.
- Add tests.

**Done when:**

- Alarm is useful without being socially dangerous.

---

### Ticket 29 — Add Voice Volume Detection

**Type:** Feature
**Priority:** P2
**Status:** Missing
**Summary:** Add speaking-too-loud detection.

**Tasks:**

- Reuse YAMNet.
- Add baseline calibration.
- Add notification/haptic feedback.
- Add reliability guidance.

**Done when:**

- Feature works in controlled conditions and clearly states limitations.

---

### Ticket 30 — Add Passive Background Monitoring

**Type:** Feature
**Priority:** P2
**Status:** Missing
**Summary:** Add opt-in short-burst ambient sound sampling.

**Tasks:**

- Add foreground short-service sampling.
- Add Room passive samples.
- Add daily summary.
- Add battery/privacy copy.
- Verify Android restrictions.

**Done when:**

- Monitoring is opt-in, policy-compliant, and battery-conscious.

---

### Ticket 31 — Add TTS Detection

**Type:** Feature
**Priority:** P2
**Status:** Missing
**Summary:** Add post-loud-event temporary threshold shift checks.

**Tasks:**

- Add dose+YAMNet trigger.
- Add baseline requirement.
- Add shortened hearing test.
- Add Room table and analytics.
- Add cautious health copy.

**Done when:**

- Users can track post-event hearing recovery without clinical overclaims.

---

### Ticket 32 — Add Tinnitus Tools

**Type:** Feature
**Priority:** P3
**Status:** Missing
**Summary:** Add tinnitus pitch matching, sound therapy, playback, sleep timer, journal.

**Tasks:**

- Decide navigation placement.
- Add tone/noise generation.
- Add playback service.
- Add journal.
- Add safety copy.

**Done when:**

- Feature is safe, useful, and does not disrupt v1 core app.

---

## 11. Recommended closure criteria for this audit

Do not mark this audit “complete” until each row below has a decision:

| Area | Required closure action |
|---|---|
| Dosimeter | Implement or explicitly descope from v1.0. |
| Camera Overlay | Implement or explicitly descope from v1.0. |
| Sleep Monitor | Implement or explicitly descope from v1.0. |
| Spectrogram/RTA | Implement or explicitly descope from v1.0. |
| YAMNet | Implement or explicitly descope from v1.0. |
| WAV | Implement or remove from v1.0 matrix. |
| Location | Implement or remove from v1.0 matrix. |
| Histogram | Implement or remove from v1.0 matrix. |
| Calibration profiles/external mic | Implement or remove from v1.0 matrix. |
| Scientific PDF | Complete missing fields or document partial scope. |
| Lock-screen public visibility | Make privacy/product decision. |
| P1/P2/P3 addendum features | Assign target release versions. |
| Accessibility | Complete audit. |
| Localization | Decide launch languages and implement. |
| Billing/release | Verify production setup. |
| Device tests | Complete manual/instrumented verification. |

---

## 12. Bottom-line conclusion

The current app is not merely a skeleton; it already contains many important systems and a substantial portion of the intended v1.0 product. But the planning documents still describe many features that are not present in the current `PROJECT.md` implementation state.

The most important distinction is this:

- **Implemented today:** a polished core dB meter with history, analytics, reports, Pro gating, Health Connect, widget, hearing test, and several advanced audio/reporting systems.
- **Still missing from the planned full product:** the “premium instrument” differentiators that would make dBcheck exceed competitors in breadth: Dosimeter Mode, Camera Overlay, Sleep Monitor, full Spectral tools, ML Sound Detection, advanced calibration, WAV/location/histogram exports, and several post-launch health-monitoring features.

For release planning, the team should either:

1. Implement the remaining v1.0 features from the planning documents, or
2. Update the planning documents and public scope so they match the actual launch product.

The worst option would be to leave the planning documents saying “all features ship in v1.0” while the codebase and `PROJECT.md` clearly do not yet contain those features.
