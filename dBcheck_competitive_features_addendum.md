# dBcheck — Competitive Features Addendum

> **Purpose:** This document supplements `dBcheck_complete_spec_v2.md` and `dBcheck_design_spec.md`. It defines additional features identified through competitive analysis (Decibel X, Decibel Pro, NIOSH SLM, Apple Watch Noise, Decibel app, SoundPrint, Too Noisy) that close gaps versus best-in-class apps and add genuine differentiation. None of these features require a backend, server, or external API — everything runs on-device, optionally syncing through Health Connect.
>
> All features in this document are additive to the existing v1.0 scope in `dBcheck_complete_spec_v2.md`. Each feature is annotated with a priority tier so Claude Code knows what to ship in v1.0 and what is queued for later.
>
> **Hierarchy:** Visual design rules in `dBcheck_design_spec.md` apply unchanged. Feature/navigation rules in `dBcheck_complete_spec_v2.md` apply unchanged. This document only adds.

---

## Priority Tiers

| Tier | Meaning | Ship by |
|------|---------|---------|
| **P0** | Closes a critical premium-tier gap; must be in v1.0 launch | Pre-launch |
| **P1** | Strong differentiator; ship in v1.0 if blocks 1–11 finish in time | Pre-launch if possible |
| **P2** | Post-launch enhancement; queue for v1.x update (1–3 months after launch) | Post-launch |
| **P3** | Strategic future feature; v2.0 territory | 6+ months out |

---

## 1. Health Connect Integration [P0]

### Why this matters

Apple-side competitors (NIOSH SLM, Decibel) integrate with Apple Health. Android's equivalent is Health Connect, which is native on Android 14+ (including Pixel 9, Samsung Galaxy, etc.) and bridges to Samsung Health, Fitbit, Garmin, Whoop, and Google Fit. A premium hearing-health app on Android in 2026 without Health Connect support is a conspicuous gap. This single integration positions dBcheck inside the entire Android health ecosystem.

### What it does

- Writes completed dBcheck measurement sessions to Health Connect as exercise entries
- Stores noise metrics in the Health Connect entry notes: equivalent sound level, maximum, LCpeak, and frequency weighting
- Reads heart rate data (with permission) to correlate elevated heart rate with loud events in Pro analytics
- Does not write hearing test results to Health Connect because there is no supported Health Connect audiometry record

### Technical approach

- Library: `androidx.health.connect:connect-client` (use the project version catalog; current implementation is documented in `PROJECT.md`)
- Data types to write:
  - Completed measurement sessions: `ExerciseSessionRecord / OTHER_WORKOUT`
  - Noise metrics are written to entry notes because Health Connect does not provide a native noise exposure record
  - Hearing tests are intentionally skipped until Android provides a supported audiometry data type or a separate FHIR path is designed
- Data types to read (Pro only):
  - `HeartRateRecord` during measurement sessions
- Permissions: standard Health Connect permission flow with rationale dialog
- Privacy: opt-in only, off by default, clear toggle in Settings

### UI/UX

- New Settings section: "Health & Sync" with Health Connect toggle
- First-run flow when toggling on:
  1. Brief explanation card ("dBcheck writes completed measurement sessions as Health Connect exercise entries")
  2. Permission request screen
  3. Confirmation
- In Analytics, when Health Connect is enabled and heart rate data is available: optional overlay on session detail charts ("BPM during this session")

### Pro/Free gating

- Free: write completed measurement sessions to Health Connect as exercise entries
- Pro: read heart rate data and overlay in analytics

### Dependencies

- None blocking. Fits into Block 8 (Settings) and Block 4 (Analytics — Overview enhancement).

---

## 2. Persistent Live Lock-Screen Widget [P0]

### Why this matters

Decibel Pro on iOS uses Live Activities to show a live dB reading on the lock screen during a measurement session. This is the "this is a real instrument" moment — your phone becomes a sound meter you glance at. Spec V2 already lists "Lock screen notification (live dB)" in the Pro matrix and Block 10, but this feature deserves elevated priority because it is one of the strongest perceived premium differentiators.

### What it does

- During an active measurement session (foreground service), display a custom-rendered notification on the lock screen showing:
  - Current dB reading (large, Space Grotesk, tabular figures)
  - Session duration
  - Peak dB so far
  - Noise level color indicator (green/yellow/red dot)
- Notification updates at 1 Hz (every second)
- Tap notification → opens Meter screen
- Optional: Material You / monochrome variant for Android 12+ themed icons

### Technical approach

- Custom `RemoteViews` for the foreground service notification (already mandatory for the measurement service)
- Use `NotificationCompat.Builder` with a custom contentView and bigContentView
- Update via `notificationManager.notify(NOTIFICATION_ID, builder.build())` from a 1-second timer in `MeasurementForegroundService`
- Minimum SDK already 26, so all required APIs are available
- Lock screen visibility: `setVisibility(VISIBILITY_PUBLIC)` so content shows on lock screen
- Add `USE_FULL_SCREEN_INTENT` if peak warnings should override DND (Pro option)

### UI/UX

- Settings toggle: "Live lock-screen meter" (Pro)
- When toggled on and a session is running, the notification adopts the rich layout
- When toggled off, falls back to a simple "dBcheck is measuring" notification

### Pro/Free gating

- Pro only. Free users get a basic "Measuring..." notification.

### Dependencies

- Already partially implemented as part of MeasurementForegroundService. This is an upgrade of an existing notification, not a new component. Belongs in Block 10.

---

## 3. B-Weighting Filter [P0]

### Why this matters

Decibel X supports A, B, C, and Z weighting plus ITU-R 468. The dBcheck plan currently includes A, C, Z, and ITU-R 468. Adding B-weighting brings dBcheck to full parity. The audio architecture per the recent ITU-R 468 spec update already supports N filters, so adding one more is trivial.

### What it does

- Adds B-weighting as a fourth selectable frequency weighting alongside A, C, Z, and ITU-R 468
- B-weighting is rarely used today in occupational contexts but is sometimes preferred for certain music and broadcast applications

### Technical approach

- Add `BWeightingFilter` class implementing the same biquad cascade pattern as `AWeightingFilter` and `CWeightingFilter`
- Coefficients per IEC 61672 Class 2 specification (publicly available)
- Register in the filter factory / weighting selector
- No architectural change required

### UI/UX

- Settings → Audio Calibration → Frequency Weighting chip selector now shows: A / B / C / Z / ITU-R 468

### Pro/Free gating

- Pro feature (matches existing weighting gating)

### Dependencies

- Filter architecture refactor (must be in place before this) is already planned in the ITU-R 468 spec update

---

## 4. Scientific PDF Report Format [P0]

### Why this matters

NIOSH SLM and Decibel X PRO produce structured reports usable for occupational compliance, workplace audits, and noise complaints. Spec V2 lists "Export: PDF report" as Pro but does not specify format. To compete with NIOSH SLM (the gold standard for credibility), the PDF must follow scientific reporting conventions.

### What it does

Generates a multi-page PDF report per session containing:

**Page 1 — Summary**
- Session name, date/time, duration, location (if enabled)
- Device info (manufacturer, model, OS version)
- Calibration offset applied
- Frequency weighting used
- Response time used
- Dosimeter standard (NIOSH or OSHA) if dosimeter mode

**Page 2 — Metrics**
- LAeq (or weighting equivalent)
- TWA (8-hour projected)
- Dose %, projected dose %
- LCpeak (C-weighted peak)
- MIN, MAX, AVG dB
- Octave band breakdown (if Pro and recorded)
- Compliance assessment: under or over NIOSH REL / OSHA PEL

**Page 3 — Time Series Graph**
- High-resolution chart of dB over session duration
- Y-axis 0–130 dB, gridlines at 10 dB
- X-axis time, with peak events marked

**Page 4+ — Peak Events Timeline (if any)**
- Timestamp, peak dB, duration of each peak event
- Sound type (if YAMNet detected something during the peak — Pro)

**Footer on every page**
- "Generated by dBcheck v[version] — Not a calibrated Class 1/2 instrument unless used with verified external microphone"
- Page number

### Technical approach

- Use Android's native `PdfDocument` API (no external library needed)
- Render each page as a `Canvas` with text and shapes
- Match the dBcheck typography (Manrope + Space Grotesk via embedded fonts) for brand consistency
- For the time series chart: render the same Canvas-based chart used in the app, sized for A4

### UI/UX

- Available from session detail screen via "Export Report (PDF)" button
- Save via `Intent.ACTION_CREATE_DOCUMENT` so user picks location
- Share via `Intent.ACTION_SEND` after generation

### Pro/Free gating

- Pro only

### Dependencies

- Replaces or upgrades the existing PDF export in Block 7

---

## 5. Audible Threshold Alarm [P1]

### Why this matters

Too Noisy app and similar classroom-management apps use audible alarms as the core feature. dBcheck adding this opens entirely new use cases without adding a new screen:

- Classroom management (teachers)
- Daycare and preschool noise control
- Baby monitor / nursery sound watch
- Recording studio peak detection
- Home theater calibration
- Workshop and machinery noise alerting

These are real markets currently served by single-purpose apps. Adding an audible alarm to dBcheck makes it the obvious choice across all these use cases without any UI clutter.

### What it does

- When dB exceeds user-set threshold for X consecutive seconds, play an audible warning sound
- User configures threshold (60–110 dB), duration (1–30 seconds), and alarm style

### Technical approach

- Background check inside `AudioSessionManager` measurement loop
- When threshold + duration condition is met, trigger `MediaPlayer` or `SoundPool` to play alarm sound
- Bundled alarm sounds (3–5 styles): single beep, repeating beep, chime, alert tone, custom recorded
- Custom recording: simple "record up to 5 seconds" UI — user records own alarm message
- Cooldown: don't re-trigger within 10 seconds of last alarm
- Volume: alarm plays at user-set volume independent of media volume (use `AudioAttributes.USAGE_ALARM`)

### Important constraint

- **Must NOT trigger while phone is in pocket / face down.** Use proximity sensor + accelerometer to detect "in use" state, OR add a setting "Only alarm when screen is on"
- Default behavior: only alarm when screen is on, to prevent embarrassing pocket alarms

### UI/UX

- New Settings section: "Threshold Alarm"
  - Toggle: Audible alarm on/off
  - Slider: Threshold (60–110 dB)
  - Slider: Duration before alarm (1–30 sec)
  - Selector: Alarm sound (with preview button per option)
  - Toggle: "Only alarm when screen is on" (default ON)
  - Slider: Alarm volume

### Pro/Free gating

- Pro feature

### Dependencies

- Standalone. Belongs as Block 12 or as part of expanded Block 8 (Settings).

---

## 6. Voice Volume / "Speaking Too Loud" Detection [P2]

### Why this matters

The Decibel app on iOS launched a "speaking too loud" warning for Apple Watch. The use cases are real and underserved:

- Tinnitus sufferers (who often speak louder than necessary)
- People with hearing loss (same)
- Users with autism or ADHD who don't always self-regulate vocal volume
- General social awareness during phone calls, video meetings, restaurants

On Android, this is technically harder than on Apple Watch (phone is usually farther from the mouth) but doable with YAMNet's "Speech" classification combined with dB threshold.

### What it does

- When the user is speaking (YAMNet detects "Speech" with confidence > 70%) AND the dB level at the phone exceeds a learned baseline + offset, show a discreet notification or vibration
- User can calibrate by speaking at "normal volume" once during setup — this becomes the personal baseline
- Optional: track voice volume trend over time (Pro analytics)

### Technical approach

- Reuse YAMNet integration from Block 6
- Add a "voice baseline" calibration flow: "Read this sentence at normal volume" → record average dB during speech-classified frames
- Detection: speech frames sustained > 2 sec at baseline + 8 dB → trigger
- Notification or haptic feedback (user-selectable)
- Optional small persistent overlay (system alert window permission) showing live voice volume during enabled sessions

### Important constraint

- Pixel 9's bottom-firing microphone can be 60+ cm from mouth when phone is on a table. Reliability varies with phone position.
- Setup must clearly state: "For best results, place phone within 1 meter and not in pocket"

### UI/UX

- New full-screen flow: "Voice Volume Setup" (one-time calibration)
- Setting toggle: "Voice volume warnings" with sub-options for sensitivity and feedback type
- Results in Analytics (Pro): "Your voice volume trend over the last 7 days"

### Pro/Free gating

- Pro feature

### Dependencies

- Requires Block 6 (YAMNet) complete. Belongs in v1.x post-launch.

---

## 7. Background Passive Monitoring [P2]

### Why this matters

Apple Watch's Noise app monitors ambient sound in the background throughout the day with no user action required. When you go to a loud restaurant, your watch silently logs it. At end of day, you have an exposure summary. dBcheck currently requires the user to open the app and start a measurement — the user must remember to monitor. Passive monitoring flips this to "always there, always knowing."

### What it does

- When enabled, dBcheck samples ambient sound in short bursts (e.g., 30 seconds every 5–10 minutes) throughout the day
- Logs the LAeq for each sample window
- End-of-day notification: "Your average exposure today was X dB. Y minutes above 85 dB. Highest period: 14:30 (subway commute, 92 dB)"
- Daily exposure graph in Analytics (passive data combined with active session data)
- Auto-detects loud events (sustained >85 dB for >5 min) and offers to start a full session

### Technical approach

- `WorkManager` periodic worker, every 5–10 minutes
- Worker spawns a brief `AudioRecord` capture (30 sec), computes LAeq, writes to Room, releases mic
- Foreground service is NOT held continuously — only during the 30-sec capture
- Battery impact transparency: target <3% additional drain per day (must measure and tune)
- Privacy: never record audio, only compute dB and discard buffer
- Android 15/16 background restrictions: must run as a `FOREGROUND_SERVICE_TYPE_MICROPHONE` short service (max 30 sec) to comply with new policies

### Critical constraints

- Microphone access from background is heavily restricted in Android 14+. Must use the short foreground service pattern.
- User must explicitly enable with clear explanation of battery and privacy implications
- Some OEMs (Xiaomi, Oppo) aggressively kill background services — document this limitation

### UI/UX

- Settings toggle: "Passive monitoring (battery impact ~2-3% per day)"
- First-run flow explaining what it does, what it doesn't record, and the battery cost
- Daily summary notification at user-set time (default 9 PM)
- New Analytics card: "Today's Passive Summary"

### Pro/Free gating

- Pro feature

### Dependencies

- Standalone. Belongs in v1.x post-launch.

---

## 8. TTS Detection — Temporary Threshold Shift Monitoring [P2]

### Why this matters

This is the radical synthesis feature. It uses YAMNet (loud event classification), the dosimeter, and the existing hearing test infrastructure together — three components that currently exist in isolation — to do something no consumer sound meter app on either platform does: detect temporary threshold shift after loud exposure events.

TTS is the early warning sign of permanent hearing damage. Detecting it transforms dBcheck from "a sound meter that warns about loud environments" into "a hearing health monitor that knows when your ears need rest."

### What it does

1. **Trigger detection:** During or after a session, when YAMNet identifies the environment as a loud event type (concert, club, construction, traffic, sports event) AND the cumulative dose exceeds a threshold (e.g., 50% NIOSH dose in <1 hour), the app flags the session as a "TTS risk event"
2. **Post-event prompt:** 30–60 minutes after the event ends, push notification: "Loud environment detected — 92 dB peak for 47 minutes. Quick 30-second hearing check?"
3. **Shortened test:** A reduced version of the full hearing test (3–4 frequencies, both ears, ~30 seconds total) using the existing `ToneGenerator` and Hughson-Westlake flow
4. **Compare to baseline:** Compare measured thresholds to the user's most recent full hearing test result
5. **Result:**
   - <5 dB shift: "Your hearing is unchanged. Good."
   - 5–10 dB shift: "Slight temporary shift detected. Rest your ears for a few hours and stay in quiet environments."
   - >10 dB shift: "Significant temporary shift detected. Avoid loud environments for 24–48 hours. If shift persists after rest, consult an audiologist."
6. **Trend tracking:** Logs each TTS check over time. Pro analytics shows trend: "Are TTS events becoming more frequent? Is recovery slowing?"

### Technical approach

- Reuses existing `ToneGenerator`, hearing test flow, audiogram data structures
- New table `tts_check_results` in Room (linked to baseline `hearing_test_results`)
- Trigger logic in `AudioSessionManager` and a post-session worker
- New full-screen flow: shortened version of `hearingtest/` flow

### Critical requirement

- Requires baseline: user must have completed at least one full hearing test before TTS Detection can run
- If no baseline exists, the post-event prompt instead invites them to take the full test

### UI/UX

- Settings: "TTS Detection" toggle (default ON for Pro users with completed baseline)
- New Analytics card: "Hearing Recovery Tracker" showing TTS event history and recovery times
- Push notification with one-tap shortcut to start the test
- Test flow: minimal UI, identical visual language to full hearing test

### Pro/Free gating

- Pro feature (hearing test is already Pro)

### Dependencies

- Requires Block 6 (YAMNet) and Block 9 (Hearing Test) complete
- Best implemented as a v1.x post-launch feature so the underlying systems are stable

---

## 9. Tinnitus Matcher & Sound Therapy [P3]

### Why this matters

Tinnitus is a major health concern that intersects directly with hearing health. The tinnitus community is large, underserved by quality apps, and willing to pay. Most tinnitus apps are subscription-only and poorly designed. Adding tinnitus tools to dBcheck:

- Extends "Auditory Observatory" brand into a new dimension naturally
- Opens a new audience (tinnitus sufferers) who would not otherwise look at a sound meter app
- Reuses existing `ToneGenerator` infrastructure
- Differentiates from every competitor (no decibel meter app has tinnitus tools)

### What it does

**Tinnitus Pitch Matcher**
- User adjusts frequency (125 Hz – 16 kHz) and amplitude until the generated tone matches their tinnitus
- Also selects character: pure tone / narrowband noise / multiple tones
- Saves matched profile per ear (left/right can differ)

**Sound Therapy Library**
- White noise, pink noise, brown noise (generated, not playback)
- Notched audio: pink noise with a frequency band notched out around the user's tinnitus pitch (research-backed approach for some tinnitus types)
- Nature sounds (bundled audio assets, pre-licensed)
- Custom mix: user adjusts levels of multiple sources

**Background Playback**
- Plays through normal Android media controls
- Lock screen notification with play/pause
- Integration with sleep timer (auto-stop after X minutes)

**Tinnitus Journal**
- Optional: log severity, triggers, sleep quality
- Chart trend over time
- Correlate with noise exposure data from dBcheck (loud event yesterday → worse tinnitus today?)

### Technical approach

- ToneGenerator already exists
- Notched noise: implement biquad notch filter in real time over generated pink noise
- Audio playback via `AudioTrack` MODE_STREAM
- New top-level area: either a fifth bottom nav tab "Tinnitus" (breaks the 4-tab rule) OR a new full-screen flow accessed from Analytics

### Critical decision

- This is the only feature in this document that may justify breaking the 4-tab rule. Two options:
  1. Add as a 5th bottom nav tab (breaks the rule but reflects significance)
  2. Keep within Analytics or as a top-level full-screen flow accessible from Settings
- Recommendation: keep as a full-screen flow accessed from a prominent CTA in Analytics, similar to Hearing Test. This preserves the navigation discipline.

### Pro/Free gating

- Pitch matcher: free
- Sound therapy library: Pro
- Journal and trend correlation: Pro

### Dependencies

- Significant scope. Best as a v1.5 or v2.0 feature, after launch and after the core sound meter features have settled.

---

## Implementation Order

Below is the suggested sequence for these additions, mapped onto the existing Block structure in `dBcheck_complete_spec_v2.md`.

### Pre-launch (must ship in v1.0)

**Block 12: Health Connect & Lock Screen** (P0)
- 12.1 Health Connect integration (write completed measurement sessions as exercise entries; read heart rate)
- 12.2 Live lock-screen widget (rich foreground service notification)
- 12.3 B-weighting filter (added to existing weighting selector)
- 12.4 Scientific PDF report format (replaces basic PDF export from Block 7)

### Pre-launch if time permits (otherwise v1.x)

**Block 13: Audible Alarm** (P1)
- 13.1 Threshold alarm with bundled and custom alarm sounds
- 13.2 Proximity / screen state guard

### Post-launch v1.x (1–3 months after launch)

**Block 14: TTS Detection** (P2)
- 14.1 TTS trigger logic (YAMNet + dose threshold)
- 14.2 Shortened hearing test flow
- 14.3 Baseline comparison and severity classification
- 14.4 Trend tracking in Analytics

**Block 15: Voice & Background Monitoring** (P2)
- 15.1 Voice volume detection (YAMNet speech classification + baseline)
- 15.2 Passive background monitoring (WorkManager-based sampling)
- 15.3 Daily exposure summary notification

### Post-launch v1.5 / v2.0 (6+ months out)

**Block 16: Tinnitus Tools** (P3)
- 16.1 Tinnitus pitch matcher
- 16.2 Sound therapy library (white/pink/brown noise + notched audio)
- 16.3 Background playback with sleep timer
- 16.4 Optional tinnitus journal with exposure correlation

---

## Notes for Claude Code

1. **None of this work should begin until Blocks 1–11 of `dBcheck_complete_spec_v2.md` are complete and tested.** This addendum is additive, not a rewrite.
2. **Visual design rules in `dBcheck_design_spec.md` apply to every new screen and component without exception.** Use existing tokens, typography, components.
3. **Each new feature must respect the existing Pro/Free gating philosophy:** Pro features show preview with `ProLockOverlay`, never hidden completely.
4. **Each new feature must support both light and dark themes** with identical layout, only color tokens changing.
5. **Each new feature must include empty states, loading states, and error states** matching the patterns in existing screens.
6. **For features requiring permissions** (Health Connect, etc.), follow the pattern established for `RECORD_AUDIO` — clear rationale, graceful degradation if denied.
7. **Privacy first:** no audio is ever recorded or persisted unless the user explicitly enables WAV export. Sound classification, voice detection, and passive monitoring all compute on transient buffers that are discarded.
8. **Battery transparency:** any feature with measurable battery impact (passive monitoring, voice detection) must show a clear estimate to the user before they enable it.
