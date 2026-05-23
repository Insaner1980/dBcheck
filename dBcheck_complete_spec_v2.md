# dBcheck — Complete Feature & Architecture Spec (v2)

> **For Claude Code.** This document supersedes all previous feature planning and phase documents. It describes the COMPLETE feature set, navigation architecture, Pro/Free gating, and pricing for dBcheck. All features ship in v1.0 — there are no phases. The existing `dBcheck_design_spec.md` remains the source of truth for visual design (colors, typography, spacing, components). This document defines WHAT the app contains and HOW it's organized.
>
> **Context:** dBcheck is a decibel meter and hearing health app for Android. It competes with Decibel X, Decibel Pro, dB Meter, and others. Its key differentiators are: (1) significantly better design than any competitor, (2) one-time purchase with no ads and no subscription — every competitor uses subscriptions or aggressive ads, (3) a genuinely useful free tier, and (4) more features than any single competitor in the category.
>
> **Owner does not code.** Claude Code must own both planning and implementation. Break work into logical chunks, implement and test each chunk, provide plain-language summaries of what changed. Pause before large batches of changes.

---

## 1. Navigation Architecture

### Bottom Navigation: 4 tabs (unchanged)

```
┌─────────────────────────────────────────┐
│                                         │
│            [Screen Content]             │
│                                         │
├─────────┬─────────┬─────────┬───────────┤
│  Meter  │ Analtic │ History │ Settings  │
└─────────┴─────────┴─────────┴───────────┘
```

The bottom nav NEVER changes. It is always visible (except in full-screen flows). 4 tabs maximum.

### Internal Navigation (within tabs)

Complexity lives INSIDE tabs, not in the bottom nav. Users discover deeper features by scrolling and tapping, never by hunting for hidden tabs.

**Meter tab:** Segmented control / chip row at top for measurement modes:
- `dB Meter` (default) — standard decibel measurement
- `Dosimeter` [PRO] — TWA, noise dose, projected dose

**Analytics tab:** Chip row at top for sub-sections:
- `Overview` (default) — weekly/monthly charts, hearing health
- `Spectral` [PRO] — spectrum analyzer, spectrogram, RTA
- `Environment` [PRO] — environment mix, sound type detection

**History tab:** No internal sub-navigation. Single scrollable list with filters.

**Settings tab:** No internal sub-navigation. Single scrollable list with sections.

### Full-Screen Flows (separate from tabs)

These open as full-screen experiences and return to the previous tab when closed:

- **Camera Overlay** [PRO] — accessed from Meter tab (camera icon near controls)
- **Sleep Monitor** [PRO] — accessed from Analytics tab (card CTA) or History tab
- **Hearing Test** [PRO] — accessed from Analytics tab (card CTA)

Pattern: each flow has its own top bar with a close/back button. Bottom nav is hidden during these flows.

---

## 2. Complete Feature List by Screen

### 2.1 Meter Tab

#### dB Meter Mode (default)

**Session info bar** [partially PRO]
- Recording indicator: 6dp pulsing red dot + "REC" label (FREE)
- Session duration timer: HH:MM:SS counting up (FREE)
- Frequency weighting indicator: "A-WEIGHT" / "C-WEIGHT" / "Z-WEIGHT" [PRO — free users always see A-WEIGHT]
- Sample rate display: "44.1k" or "48k" [PRO]

**Circular gauge** (FREE)
- Gradient arc showing current dB level
- Glassmorphic circular container
- Dashed tick marks around circumference
- Inside: "DECIBELS" label, large dB number, "dB" unit, noise level pill
- Arc color shifts by level: green (safe) → accent (moderate) → amber (loud) → red (dangerous)
- Smooth animation (~200ms ease-out), breathing pulse at rest

**Noise level labels** (FREE)
- Contextual pill inside gauge: "Whisper", "Normal Conversation", "Busy Traffic", "Dangerous"
- Thresholds: 0-40 Quiet/Whisper, 40-70 Normal, 70-85 Elevated, 85+ Dangerous

**Real-time time-series chart** (FREE)
- Scrolling line/area chart showing dB over last 30 seconds
- Line: 2dp stroke, `primary` color, area fill at 8-10% opacity
- 85dB threshold: dashed horizontal line in `warning` color
- Y-axis: 0-120dB, gridlines at 20dB intervals, labels in `label-sm`
- X-axis: -30s, -20s, -10s, NOW — scrolling
- Peak markers: dot/pulse when new session MAX detected
- Container: `surface-container`, `rounded-xl`, ~160dp height
- Update rate: 10fps (new point every 100ms)
- Empty state: flat line at 0 + "Start measuring to see live data"
- Paused state: line freezes, "PAUSED" label top-right

**MIN / AVG / MAX stat cards** (FREE)
- Three equal-width cards in a row
- Labels: `label-md` uppercase (Space Grotesk)
- Values: `data-lg` (Space Grotesk) + `unit` token for "dB"
- Peak values >85dB shown in `warning` or `error` color

**Sound reference card** (FREE)
- Expandable card below stat cards
- Collapsed (default): horizontal gradient bar (green→yellow→red) + triangle marker at current dB, ~48dp height
- Expanded (tap): full reference list with icons
- Reference data:
  | dB  | Label               | Icon (Material icon in production) |
  |-----|---------------------|------------------------------------|
  | 10  | Breathing           | air                                |
  | 20  | Rustling leaves     | eco                                |
  | 30  | Whisper             | hearing                            |
  | 40  | Quiet library       | local_library                      |
  | 50  | Quiet office        | business                           |
  | 60  | Normal conversation | chat_bubble                        |
  | 70  | Busy restaurant     | restaurant                         |
  | 80  | Busy traffic        | directions_car                     |
  | 85  | Damage threshold    | warning                            |
  | 90  | Motorcycle          | two_wheeler                        |
  | 100 | Subway train        | subway                             |
  | 110 | Rock concert        | music_note                         |
  | 120 | Thunder / Pain      | flash_on                           |
- Closest match highlighted with `primary` accent

**Response time selector** [PRO]
- Small chip group or dropdown near session info bar or in a quick-settings area
- Options: Slow (500ms), Fast (200ms), Impulse (50ms)
- Affects RMS calculation window
- Free users locked to Fast (200ms) as default

**LAeq display** [PRO]
- Shown in session info bar or as additional stat below MIN/AVG/MAX
- LAeq = equivalent continuous sound level over session duration
- Formula: standard ISO 1996 logarithmic average (public domain)
- Updates every second

**Controls** (FREE)
- Reset button (left, secondary circular)
- Play/Pause FAB (center, large, gradient fill)
- Share button (right, secondary circular)
- Camera Overlay entry point [PRO]: small camera icon, opens full-screen camera flow

#### Dosimeter Mode [PRO]

Activated via segmented control at top of Meter tab. Replaces the circular gauge with a dose-focused display.

**Dose gauge**
- Circular gauge reused but now shows noise dose percentage (0-200%)
- Arc color: green (0-50%), accent (50-100%), warning (100-150%), error (150%+)
- Center: large dose percentage number, "%" unit, "NOISE DOSE" label

**Dose stats**
- TWA (Time-Weighted Average): calculated per NIOSH formula (public domain, CDC website)
  - Formula: TWA = 10 × log₁₀(D/100 × T/8) where D = dose percentage, T = measurement hours
- Noise Dose %: cumulative exposure as percentage of maximum allowable (100% = NIOSH REL)
- Projected Dose: extrapolation of current rate to 8 hours
- LAeq: equivalent continuous level for the session
- Exposure time remaining: how many minutes at current level before reaching 100% dose

**Dose time-series chart**
- Same chart component as dB meter mode but Y-axis shows cumulative dose % instead of instantaneous dB
- Threshold line at 100% dose

**Standards compliance display**
- Small label showing which standard is being used: "NIOSH" or "OSHA"
- NIOSH: 85 dB REL, 3dB exchange rate
- OSHA: 90 dB PEL, 5dB exchange rate
- Selectable in Settings; default NIOSH

**Dose reference info**
- Expandable card (like dB reference) explaining what noise dose means
- "100% = maximum safe daily exposure per NIOSH guidelines"
- Brief explanation of TWA and dose calculations

#### Camera Overlay [PRO] — Full-Screen Flow

Accessed from camera icon on Meter tab.

- Uses CameraX for camera preview
- Real-time dB reading overlaid on camera view
- Overlay shows: current dB value, noise level label, timestamp
- Capture photo: saves image with dB overlay burned in
- Record video: saves video with live dB overlay
- Share directly to social media or save to gallery
- Permissions: CAMERA (requested with rationale)
- Minimal UI: dB overlay + capture button + close button

---

### 2.2 Analytics Tab

#### Overview Sub-section (default)

**Weekly/Monthly toggle** (FREE for weekly, PRO for monthly)
- Chip selector: `Weekly` / `Monthly`

**Exposure Summary card** (FREE for weekly)
- Bar chart: daily average dB for the period (Mon-Sun or monthly)
- Bars use signature gradient, current day/period highlighted
- Right-aligned stat: "68.4 AVG DB/DAY" in `data-xl`
- Label: "LAST 7 DAYS (DB AVERAGE)" in `label-md`
- Chart implementation: custom Compose Canvas charts

**Hearing Health card** (FREE)
- Status icon: checkmark (safe), warning (elevated), error (dangerous)
- Message: "Your hearing is in the Safe Zone" (or appropriate warning)
- Body text about exposure relative to weekly average
- "VIEW TIPS" primary gradient button → opens hearing protection tips

**Hearing tips content**
- Expandable section or bottom sheet with practical advice
- Information about safe exposure limits (NIOSH/OSHA guidelines — public domain)
- Hearing protector selection guidance (NIOSH published recommendations)
- Tips for reducing exposure in common scenarios

**Sleep Monitor CTA card** [PRO]
- "Sleep Monitor — Track overnight noise levels"
- Subtitle: "Monitor your sleep environment"
- "START SLEEP MONITOR" button → opens full-screen Sleep Monitor flow
- Dismissible (can be hidden, re-enabled in Settings)

**Hearing Test CTA card** [PRO]
- "Check Your Hearing"
- Subtitle: "Quick 3-minute assessment"
- "START TEST" button → opens full-screen Hearing Test flow

#### Spectral Sub-section [PRO]

**Mode toggle**
- Segmented control: `Bars` / `Spectrogram` / `RTA`

**Bars mode (FFT)**
- Vertical frequency bars showing amplitude per frequency band
- X-axis: 20Hz to 20kHz (logarithmic)
- Y-axis: amplitude in dB
- Real-time update
- Dominant frequency highlighted

**Spectrogram mode (waterfall)**
- Scrolling heat map: X = time (10s window), Y = frequency (250Hz-16kHz, log), color = amplitude
- Color ramp: `surface-container` (silence) → `primary-dim` (low) → `primary` (moderate) → `secondary` (loud) → `warning` (peak)
- Brand colors only, NOT rainbow
- Y-axis labels: 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
- Each column ~100ms of data

**RTA mode (Octave Band Real-Time Analyzer)**
- Bars for standard octave bands: 31.5Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
- Band center frequencies and widths per ISO 266 (public domain)
- Each bar shows current level in dB for that octave band
- Peak hold markers

**Stat pills (below any visualization mode)**
- Dominant Frequency: e.g. "2.4 kHz"
- Bandwidth: "Narrow" / "Medium" / "Wide"
- Peak Frequency: highest amplitude frequency in current frame
- Noise Floor: lowest measured ambient level in session
- 2x2 grid layout, same card style as MIN/AVG/MAX

**"LIVE CAPTURE" indicator**
- Small green dot + "LIVE CAPTURE" label-md at top of card

#### Environment Sub-section [PRO]

**Environment Mix card**
- Pie chart or horizontal bars showing time distribution:
  - Quiet (0-40 dB): percentage + colored dot
  - Moderate (40-70 dB): percentage + colored dot
  - Loud (70-85 dB): percentage + colored dot
  - Critical (85+ dB): percentage + colored dot
- Based on session data, updates in real-time during active measurement

**Sound Type Detection card** [PRO, toggleable in Settings]
- ML-based sound classification using Google's YAMNet model (TensorFlow Lite)
- YAMNet: Apache 2.0 license, available on TensorFlow Hub, ~3-4MB model
- Recognizes 500+ sound events: speech, music, traffic, construction, animals, etc.
- Display: current detected sound type with confidence percentage
- History: recent detections in a small scrollable list with timestamps
- Note: accuracy varies; display as "Detected: Traffic (78%)" not as absolute fact
- Battery impact: runs inference on audio frames; toggleable to save power
- Empty state when disabled: "Enable sound detection in Settings"

---

### 2.3 History Tab

**Last 24 Hours card** (FREE)
- Area/line chart showing dB over 24 hours
- Gradient fill under line
- Stats: "Average 42 dB · Stable" left, "68 PEAK DB" right in `data-2xl`
- X-axis: 00:00, 06:00, 12:00, 18:00, NOW
- Peak moments highlighted with dots

**Recent Sessions section** (FREE for last 7 days, PRO for unlimited)
- "RECENT SESSIONS" label + "View All" tertiary button
- Session cards showing:
  - Contextual icon in tinted circle (auto-assigned based on time of day and noise level)
  - Session name: auto-generated for free users (timestamp-based), custom naming [PRO]
  - Date, time, duration metadata
  - PEAK and AVG dB values (right-aligned)
  - Peak >85dB in `warning`/`error` color
  - Location label if available [PRO]
- Cards separated by spacing, NEVER divider lines
- Free users: last 7 days only, older sessions hidden with "Unlock unlimited history with Pro" CTA
- Pro users: unlimited, searchable, filterable

**Session tags** [PRO]
- Users can add tags to sessions: "Work", "Commute", "Sleep", "Concert", etc.
- Predefined tag set + custom tags
- Filter sessions by tag

**Session location** [PRO]
- GPS coordinates saved with each session (requires ACCESS_COARSE_LOCATION)
- Displayed as location name (reverse geocoded) on session card
- Permission requested with rationale; optional — app works without it

**Summary cards** (FREE)
- Two cards side by side at bottom:
  - Weekly Trend: percentage change vs last week, e.g. "-12% Quieter than last week"
  - Safe Hours: total hours within safe limits, e.g. "22.4h Within safe dB limits"

**Histogramm (dB distribution)** [PRO]
- Available in session detail view
- Bar chart showing percentage of time at each dB range (10dB buckets)
- Helps understand noise profile of a session

**Export options** [PRO]
- Available per session or batch:
  - CSV: timestamped dB readings
  - PDF report: summary with chart, date, location, LAeq, MIN/MAX, dose info
  - PNG: high-res graph export
  - WAV: raw audio recording from session (if audio recording was enabled)
    - WAV = PCM data + 44-byte header, no external library needed
    - Note: file sizes can be large; warn user about storage

---

### 2.4 Settings Tab

**Section: Audio Calibration**

- Microphone Sensitivity [PRO]: slider -10dB to +10dB offset
  - Subtitle: "Adjust device mic for accuracy"
  - Current value shown in accent color
- Octave Band Calibration [PRO]: per-band offset adjustment
  - Opens detail screen with sliders for each octave band
  - Based on ISO 266 center frequencies
- Calibration Profiles [PRO]: save/load named calibration profiles
  - Useful for different microphones or environments
  - Stored in DataStore
- External Microphone [PRO]: toggle + settings
  - Android detects USB/Bluetooth audio inputs via AudioRecord
  - Select input source
  - Per-mic calibration profiles
- Frequency Weighting [PRO]: chip selector A / C / Z
  - A-weighting: default, models human hearing sensitivity
  - C-weighting: flatter, used for peak measurements
  - Z-weighting: flat/unweighted, raw measurement
  - Coefficients are published standards (IEC 61672)
- Response Time Default [PRO]: Slow (500ms) / Fast (200ms) / Impulse (50ms)
- Dosimeter Standard [PRO]: NIOSH / OSHA toggle
  - NIOSH: 85dB REL, 3dB exchange rate (default)
  - OSHA: 90dB PEL, 5dB exchange rate

**Section: Noise Notifications**

- Exposure Alerts (FREE): toggle, "Notify when noise exceeds 85dB for over 30 mins"
- Peak Warnings (FREE): toggle, "Instant alert for sudden sounds above 120dB"
- Notification Threshold [PRO]: slider 60dB-110dB, custom alert level
- Extended Exposure Alerts [PRO]: "Monitor dosage over 8-hour periods"
- Notification Schedule [PRO]: configure when alerts are active

**Section: Display & Features**

- Dark Mode (FREE): System default / Dark / Light
- Waveform Style (FREE): visual style of background waveform
- Refresh Rate (FREE): High (60fps) / Medium (30fps) / Low (15fps)
- Show Technical Metadata [PRO toggle]: show/hide session info bar details
- Enable Dosimeter Mode [PRO toggle]: show/hide dosimeter in Meter tab chips
- Enable Sound Type Detection [PRO toggle]: on/off for ML inference
- Show Sleep Monitor Card [PRO toggle]: show/hide in Analytics

**Section: Data & Export**

- Data Storage & Export [PRO]: clear history, batch export
- Audio Recording: toggle to record WAV alongside measurements [PRO]
  - Warning about storage usage
- Always-On Display [PRO]: keep screen on during measurement sessions
- Keep Awake [PRO]: prevent device sleep during long measurements (wake lock)

**Section: General**

- About dBcheck: version, build, legal
- Privacy Policy: link
- Terms of Service: link
- Help & FAQ: link or in-app content
- dBcheck Pro upsell card (for free users):
  - Distinctive card with gradient accent
  - "Unlock All Features"
  - "One-time purchase · €12.99"
  - "UPGRADE TO PRO" gradient button

**Footer:** "dBcheck v1.0.0 · Privacy · Terms" centered, `label-sm`

---

### 2.5 Full-Screen Flows

#### Sleep Monitor [PRO]

Accessed from Analytics CTA card or History tab.

**Setup screen:**
- Brief explanation: "Track noise levels while you sleep"
- Alarm time setting (optional): auto-stop monitoring
- Sensitivity setting: how often to sample (every 10s / 30s / 60s)
- "START MONITORING" button

**Active monitoring screen:**
- Minimal UI: dark background, large clock display
- Current dB level (small, not distracting)
- "Recording..." indicator with pulsing dot
- Stop button
- Runs as foreground service with persistent notification
- Low battery optimization: reduced sample rate, no FFT, no ML
- Screen dims but stays on if Always-On Display enabled, otherwise screen off with wake lock for audio

**Results screen (on stop):**
- Summary: duration, average dB, peak dB, number of noise events
- Timeline chart: dB over entire sleep period
- Notable events: spikes above threshold with timestamps
- Save to history as a "Sleep" session with moon icon
- Export options (PDF, CSV)

#### Hearing Test [PRO]

Already specified in existing design spec + previous hearing test implementation work. Key points:

- Full-screen flow: Setup → Test → Results
- Hughson-Westlake method (confirmed correct in code review)
- Tests both ears at fixed tone frequencies for relative personal tracking
- Produces a relative threshold chart, not a clinical audiogram
- Results use personal score ranges only; they are not calibrated clinical hearing-level classifications
- Sensitivity Breakdown: Low frequencies, speech-range tones, high frequencies
- Key Metrics: Average relative level and tested range
- Disclaimer: "This test provides relative hearing thresholds for personal tracking. For clinical diagnosis, consult an audiologist."
- Save results to profile, share results
- Requires headphones; room noise check before starting

#### Camera Overlay [PRO]

Described in Meter tab section above. Full-screen camera view with dB overlay.

---

## 3. Pro vs Free Feature Matrix

### Pricing

**Free tier:** No ads, no time limits, no session count limits within the 7-day history window. Genuinely useful.

**Pro tier:** One-time purchase. €12.99 introductory launch price. Target price increase to €16.99 once the app has established reviews and downloads. Existing purchasers keep Pro forever regardless of future price changes.

**Monetization:** Google Play Billing Library (already in stack). Single in-app purchase product: "pro_unlock". No subscription, no consumables.

### Complete Matrix

| Feature | Free | Pro |
|---------|:----:|:---:|
| **METER** | | |
| Real-time dB gauge | ✓ | ✓ |
| Noise level labels | ✓ | ✓ |
| Real-time time-series chart | ✓ | ✓ |
| MIN / AVG / MAX stats | ✓ | ✓ |
| Sound reference chart | ✓ | ✓ |
| Play / Pause / Reset / Share | ✓ | ✓ |
| Recording indicator + timer | ✓ | ✓ |
| Weighting & sample rate display | | ✓ |
| Response time selection | | ✓ |
| LAeq display | | ✓ |
| Dosimeter mode (TWA, dose, projected) | | ✓ |
| Camera overlay | | ✓ |
| **ANALYTICS** | | |
| Weekly exposure chart | ✓ | ✓ |
| Hearing health status card | ✓ | ✓ |
| Hearing protection tips | ✓ | ✓ |
| Monthly exposure chart | | ✓ |
| Spectral analysis (FFT bars) | | ✓ |
| Spectrogram (waterfall) | | ✓ |
| Octave band RTA | | ✓ |
| Environment Mix | | ✓ |
| Sound type detection (ML) | | ✓ |
| Sleep Monitor | | ✓ |
| Hearing Test + relative threshold chart | | ✓ |
| **HISTORY** | | |
| Last 24 hours chart | ✓ | ✓ |
| Sessions — last 7 days | ✓ | ✓ |
| Weekly trend + safe hours | ✓ | ✓ |
| Sessions — unlimited | | ✓ |
| Session naming & tagging | | ✓ |
| Session location | | ✓ |
| dB distribution histogram | | ✓ |
| Export: CSV | | ✓ |
| Export: PDF report | | ✓ |
| Export: PNG graph | | ✓ |
| Export: WAV audio | | ✓ |
| **SETTINGS** | | |
| Dark / Light / System theme | ✓ | ✓ |
| Waveform style | ✓ | ✓ |
| Refresh rate | ✓ | ✓ |
| Basic exposure alert (85dB) | ✓ | ✓ |
| Peak warning (120dB) | ✓ | ✓ |
| Custom notification threshold | | ✓ |
| Extended exposure alerts | | ✓ |
| Microphone sensitivity calibration | | ✓ |
| Octave band calibration + profiles | | ✓ |
| External microphone support | | ✓ |
| Frequency weighting (A/C/Z) | | ✓ |
| Response time default | | ✓ |
| Dosimeter standard (NIOSH/OSHA) | | ✓ |
| Always-on display | | ✓ |
| Keep awake | | ✓ |
| Audio recording (WAV) | | ✓ |
| Feature toggles (customize UI) | | ✓ |
| **OTHER** | | |
| Home screen widget | | ✓ |
| Lock screen notification (live dB) | | ✓ |

### Pro Gating UX

- Pro features show a preview with blurred/frosted overlay + lock icon + "Unlock with Pro" CTA
- Tapping any locked feature opens the Pro purchase sheet
- NEVER hide Pro features completely — always show what the user is missing
- No dark patterns, no aggressive upselling, no fake urgency
- Pro upsell card in Settings is the primary purchase entry point
- After purchase: all features unlock instantly, no restart needed

---

## 4. Design Evolution Notes

These changes should be applied to `dBcheck_design_spec.md` by Claude Code:

### Creative Direction Update

Add to Key Principles:
- "Technical credibility through data transparency — the app should feel like it actually measures something real"

Update North Star to incorporate "Credible Precision":
> "The Auditory Observatory" — a premium digital health instrument that combines immersive calm with credible precision. Data floats within atmosphere, but it's real data presented with the rigor of a professional instrument.

### Typography Additions

Add to type scale:

| Token | Font | Size | Weight | Letter Spacing | Usage |
|-------|------|------|--------|----------------|-------|
| `data-2xl` | Space Grotesk | 2.5rem (40sp) | 700 | -0.02em | Hero stats |
| `unit` | Space Grotesk | 0.625rem (10sp) | 500 | 0.12em | Unit labels (dB, Hz, kHz, %) |

Add typography rule:
> "All numeric data must use tabular (monospaced) figures via the OpenType `tnum` feature. Numbers and units are styled separately — number in data token, unit suffix in `unit` token at smaller size and `on-surface-variant` color."

### Spectral Visualization

Update Analytics Spectral Analysis card to include three modes (Bars, Spectrogram, RTA) as described in section 2.2.

---

## 5. Technical Notes

### Data Sources — Everything is Public Domain or Open License

| Feature | Data/Formula Source | License |
|---------|-------------------|---------|
| A/C/Z weighting coefficients | IEC 61672 (published values widely available) | Standard |
| NIOSH TWA/Dose formulas | CDC/NIOSH publications | Public domain (US govt) |
| OSHA PEL calculations | OSHA regulations | Public domain (US govt) |
| Octave band center frequencies | ISO 266 | Standard (values public) |
| LAeq formula | ISO 1996 | Standard (formula public) |
| Relative hearing score bands | App-defined personal tracking scale | Proprietary copy |
| YAMNet sound classification model | TensorFlow Hub | Apache 2.0 |
| Hughson-Westlake hearing test method | Published audiological method | Public domain |

### Permissions Required

| Permission | Required for | When to request |
|-----------|-------------|-----------------|
| `RECORD_AUDIO` | Core functionality | On first launch, with rationale |
| `POST_NOTIFICATIONS` | Exposure alerts (Android 13+) | When user enables notifications |
| `FOREGROUND_SERVICE` | Background measurement, sleep monitor | Automatically (manifest) |
| `CAMERA` | Camera overlay | When user first opens camera overlay |
| `ACCESS_COARSE_LOCATION` | Session location tagging | When user enables location in settings |
| `WAKE_LOCK` | Keep awake, sleep monitor | Automatically (manifest) |

### Key Libraries (confirmed in stack)

- Kotlin 2.3.20, Jetpack Compose BOM 2026.03.00
- Hilt 2.59.2 (DI)
- Room 2.8.4 (database)
- DataStore 1.1.4 (preferences)
- Navigation Compose 2.9.7
- Custom Compose Canvas charts
- Glance 1.1.1 (widgets)
- Google Play Billing 8.3.0
- CameraX (for camera overlay)
- TensorFlow Lite (for YAMNet sound classification)

### Implementation Order

Build in this sequence. Each block should be implemented, compiled, and tested before moving to the next.

**Block 1: Core infrastructure**
- Updated theme (typography additions: `data-2xl`, `unit`, tabular figures)
- Updated navigation (4 tabs + internal chip navigation)
- Pro/Free gating infrastructure (billing, ProLockOverlay, purchase flow)

**Block 2: Meter — dB Meter mode**
- Session info bar
- Circular gauge (existing, verify)
- Time-series chart (new)
- MIN/AVG/MAX stats
- Sound reference card (new)
- Controls (existing, verify)

**Block 3: Meter — Dosimeter mode**
- Dose gauge variant
- TWA/Dose/Projected dose calculations
- Dose chart
- NIOSH/OSHA standard switching

**Block 4: Analytics — Overview**
- Weekly chart (existing, verify)
- Monthly chart (Pro extension)
- Hearing health card
- Hearing tips content
- Sleep Monitor CTA card
- Hearing Test CTA card

**Block 5: Analytics — Spectral**
- FFT bars mode
- Spectrogram mode
- RTA mode
- Stat pills

**Block 6: Analytics — Environment**
- Environment Mix
- Sound Type Detection (YAMNet integration)

**Block 7: History**
- 24h chart, session list, summary cards (existing, verify)
- Unlimited history (Pro)
- Session naming, tagging, location
- Histogram
- All export formats (CSV, PDF, PNG, WAV)

**Block 8: Settings**
- All calibration options
- All notification options
- All display/feature toggles
- Pro upsell card

**Block 9: Full-screen flows**
- Camera Overlay
- Sleep Monitor
- Hearing Test (existing, verify and integrate)

**Block 10: Widget & notifications**
- Home screen widget (Glance)
- Lock screen notification with live dB
- Foreground service notification updates

**Block 11: Polish**
- Empty states for all screens
- Loading states / skeleton loaders
- Error states (mic denied, measurement failed)
- Both themes tested for every screen
- Haptics (threshold crossing, peak detection)
- Animations (gauge, screen transitions, pull-to-refresh)

---

## 6. Competitive Positioning Summary

For reference — this is what dBcheck offers that competitors don't (all in one app):

vs. **Decibel X**: Better design, one-time purchase (not subscription), sleep monitor, hearing test, sound type detection, dosimeter mode with NIOSH/OSHA switching, hearing health analytics
vs. **Decibel Pro**: Better design, Android-native (not cross-platform), more advanced spectral tools (spectrogram + RTA), environment mix, sound type detection
vs. **NIOSH SLM**: Available on Android (NIOSH is iOS-only), modern design, hearing test, spectral analysis, history/analytics, camera overlay
vs. **All competitors**: No ads in free version, genuinely useful free tier, premium editorial design, one-time purchase model

**Key marketing message:** "The most beautiful and capable sound meter on Android. One price. Yours forever."
