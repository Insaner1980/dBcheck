# dBcheck UI Redesign Specification — "The Auditory Observatory" Instrument Panel

**Target:** Kotlin + Jetpack Compose, Material 3 (Expressive motion), dark theme first.
**Scope:** Full visual redesign of all screens + P0 bug fixes. Everything in this spec ships in v1.0. Nothing is optional and nothing is deferred to a later version.

---

## Rules of engagement for the implementer

1. **Audit before implementing.** Inspect the actual repository state first. Some components, theme tokens, or infrastructure described here may already exist in some form. Reuse and refactor existing code where it matches this spec; do not duplicate.
2. **Use the exact token values in this spec.** Do not substitute colors, sizes, spacings, or animation parameters with your own choices. If a value is missing, pick the nearest value from the token tables and note the decision in your summary.
3. **Do not simplify.** If a component spec says gradient + glow + peak hold, implement all three. Partial implementations are the reason previous redesigns failed.
4. **Do not invent features.** This spec restyles and fixes existing functionality. The only new UI surfaces are the ones explicitly specced here (waveform strip, share card, idle starfield).
5. **All measurement logic, thresholds, and data sources stay unchanged.** This is a presentation-layer redesign. Existing classification thresholds (Quiet / Moderate / Loud / Critical), dosimetry standards, and calculations are correct — only their visual presentation changes.
6. Centralize every color, dimension, type style, and spring in a theme/token file. No hardcoded hex values or dp literals inside composables.

---

## 1. Style direction

The app adopts the same "precision instrument panel" identity as the dBcheck website: dark observatory atmosphere, hairline structure, uppercase letter-spaced labels, engineered numerals. Base is Material 3 with a fully custom `darkColorScheme`; motion follows Material 3 Expressive spring physics. Dynamic color (Material You) is **not** used by default — the brand palette below is the default. (A "Use system colors" toggle may exist in Settings > Display only if it already exists; do not add one.)

Design intent in one sentence: **the app should look and feel like a living scientific instrument, not a template dashboard.**

---

## 2. Design tokens

### 2.1 Color — foundations

| Token | Value | Usage |
|---|---|---|
| `bg/deep` | `#0A0D12` | Root background (blue-black, never pure #000) |
| `bg/surface1` | `#11151C` | Cards, bottom nav, list containers |
| `bg/surface2` | `#181E27` | Nested cards, stat tiles, input fields |
| `bg/surface3` | `#212936` | Pressed/hover states, elevated menus, dialogs |
| `border/hairline` | `#FFFFFF` @ 8% | 1dp card and tile borders |
| `border/focus` | `#FFFFFF` @ 16% | Focused/selected outlines |
| `text/primary` | `#ECEFF4` | Headings, values |
| `text/secondary` | `#9BA3B0` | Body, descriptions |
| `text/muted` | `#5C6470` | Section labels, axis labels, disabled |
| `brand/primary` | `#34D399` | Interactive accent: buttons, active nav, selected chips, sliders |
| `brand/onPrimary` | `#06281B` | Text/icons on `brand/primary` |
| `brand/primaryContainer` | `#123B2C` | Selected chip fill, active nav pill |
| `error` | `#F87171` | Errors, destructive actions |

Background treatment: the root background is not flat. Apply a very subtle vertical radial gradient — `#0D1118` at top center fading to `#0A0D12` at 60% height. It must be barely perceptible (this is atmosphere, not decoration).

### 2.2 Color — dB semantic scale (the core of the redesign)

One color scale used **everywhere** sound level appears: gauge, waveform, charts, environment mix, history, badges, share card, widget. Buckets map to the app's existing classification thresholds.

| Token | Value | Bucket | Container (12% tint on surface) |
|---|---|---|---|
| `level/quiet` | `#34D399` | Quiet | `#122B22` |
| `level/moderate` | `#FBBF24` | Moderate | `#2E2712` |
| `level/loud` | `#FB923C` | Loud | `#2F2114` |
| `level/critical` | `#F87171` | Critical | `#301717` |

Continuous gradient (for gauge arc and waveform), stops positioned along the 0–120 dB scale:

```
0 dB   → #34D399   (quiet green)
55 dB  → #34D399
70 dB  → #FBBF24   (amber)
85 dB  → #FB923C   (orange)
100 dB → #F87171   (red)
120 dB → #F87171
```

Rule: `brand/primary` and `level/quiet` are intentionally the same green. Amber/orange/red are **only** used with semantic meaning — never decoratively.

### 2.3 Color — spectrogram colormap (magma LUT)

The spectrogram maps intensity through a perceptually uniform magma LUT instead of gray:

```
0.000 → #000004
0.125 → #140E36
0.250 → #3B0F70
0.375 → #641A80
0.500 → #8C2981
0.625 → #B73779
0.750 → #DE4968
0.875 → #F7705C
0.950 → #FE9F6D
1.000 → #FCFDBF
```

Implement as a 256-entry precomputed color LUT (interpolate between stops in linear RGB). The same LUT is reused for any future heatmap.

### 2.4 Typography

Two families:

- **Space Grotesk** (bundle Regular 400, Medium 500, Bold 700; SIL OFL license — include license file): display numerals, big values, brand wordmark, tab labels.
- **Roboto / system default**: everything else.

| Style | Font | Size / weight | Notes |
|---|---|---|---|
| `display/gauge` | Space Grotesk 700 | 64sp | Main dB readout. `fontFeatureSettings "tnum"` |
| `display/stat` | Space Grotesk 500 | 28sp | Card hero values (36.4, 50, 0%) — tabular |
| `title/screen` | Space Grotesk 500 | 24sp | Screen titles (Meter, Trends…) |
| `title/card` | Roboto 500 | 16sp | Card titles in sentence case |
| `label/section` | Roboto 500 | 11sp, letterSpacing 1.5sp, UPPERCASE | Section labels (SESSIONS, TOOLS…) |
| `body` | Roboto 400 | 14sp / lineHeight 20sp | Descriptions |
| `body/emphasis` | Roboto 500 | 14sp | Chip labels, buttons |
| `caption` | Roboto 400 | 12sp | Axis labels, units, timestamps — tabular for numbers |

Rules: every numeric value in the app uses tabular figures. Reduce the current oversized ALL-CAPS usage: uppercase is only for `label/section` and small badges — card titles change to sentence case (`title/card`).

### 2.5 Spacing, shape, layout

- Grid: 8dp base. Allowed values: 4, 8, 12, 16, 20, 24, 32, 40.
- Screen horizontal padding: 20dp.
- Card padding: 20dp. Gap between cards: 12dp. Gap between sections: 32dp.
- Corner radius: cards 20dp, nested tiles 14dp, chips/buttons fully rounded, sheets/dialogs 28dp top.
- Cards: `bg/surface1` fill + 1dp `border/hairline`. Nested tiles: `bg/surface2`, no border.
- Edge-to-edge enforced; content respects system bar insets; bottom nav uses `bg/surface1` with hairline top border.
- Touch targets ≥ 48dp.

### 2.6 Motion tokens (Material 3 Expressive)

| Token | Spec | Usage |
|---|---|---|
| `spring/track` | dampingRatio 0.90, stiffness 120 | Gauge arc + needle following live level |
| `spring/snappy` | dampingRatio 0.80, stiffness 380 | Chip selection, toggle, nav pill |
| `spring/gentle` | dampingRatio 1.0, stiffness 200 | Card content changes, expansion |
| `roll/digit` | 160ms, emphasized easing | Digit roll on value change (vertical slide + fade) |
| `pulse/rec` | 1200ms sinusoidal, alpha 1.0 ↔ 0.45, infinite | REC dot |
| `transition/screen` | Fade-through 220ms | Bottom-nav destination changes |
| `breathe/idle` | 3000ms sinusoidal, subtle scale 1.0 ↔ 1.015 + alpha | Idle gauge arc |

Respect the system "remove animations" accessibility setting: replace springs/loops with instant or single-fade equivalents.

---

## 3. Core component: the Gauge (hero of the app)

Replace the current thin gray arc completely.

### 3.1 Geometry

- Size: screen width minus 2×32dp, capped at 320dp diameter. Centered.
- Arc: 270° sweep, from 135° (bottom-left) clockwise to 45° (bottom-right). Scale 0–120 dB.
- Track: 14dp stroke, rounded caps, color `#FFFFFF` @ 6%.
- Value arc: 14dp stroke, rounded caps, drawn over the track from 0 to current dB using the **dB gradient** (§2.2) as a sweep gradient locked to the scale (so 90 dB always sits in orange territory regardless of current value).
- Glow: behind the value arc's leading tip, draw a radial gradient circle (radius 28dp, center at arc tip) of the tip's current color at 35% alpha fading to transparent. This is the "live" indicator.
- Ticks: major tick every 20 dB (2dp × 10dp, `text/muted`), minor every 5 dB (1dp × 5dp, `text/muted` @ 50%). Labels at 0/20/40/60/80/100/120 in `caption`, tabular, positioned outside the arc.
- Peak-hold marker: a 2dp × 14dp radial tick in `text/primary` at the session max position. When a new max occurs it jumps there instantly, holds 3s, then decays toward the current level at 1.5 dB/s. Classic SPL-meter behavior.

### 3.2 Center content — active measurement

- Current value in `display/gauge` (64sp), integer, digit-roll animation on change (`roll/digit`), centered slightly above geometric center.
- "dB" unit in `caption`, `text/secondary`, right of the value baseline.
- Below the value: classification badge — pill, fill = current bucket's container color, text = bucket color, `body/emphasis`, label from existing classification (WHISPER, MODERATE…). Badge crossfades (180ms) when bucket changes; never overlaps the arc.
- Arc + glow follow the live level with `spring/track` (smooth, instrument-like, no jitter).

### 3.3 Center content — idle state

Remove the "Tap Play to measure." text from inside the gauge. Idle state:

- Track + faint value arc at 0, breathing with `breathe/idle`.
- Center: microphone-off glyph 28dp `text/muted`, below it "Ready" in `body`, `text/secondary`.
- The Play FAB (existing control row) is the only call to action; add label "Start measuring" under the control row in `caption`, `text/muted`, idle only.

### 3.4 Ambient reactivity (signature behavior)

While measuring, the gauge glow alpha = 0.20 + (normalizedLevel × 0.40), where normalizedLevel = currentDb/120 clamped 0–1. The whole instrument visibly "breathes with the room". Disabled when Refresh Rate = Low power.

---

## 4. Core component: Live waveform strip

New component on the Meter screen, directly below the gauge.

- Full content width, height 64dp, no card background (drawn on `bg/deep`).
- Vertical bars 2dp wide, 2dp gap, right-to-left scroll; one bar per 50ms RMS sample (reuse the existing UI-level amplitude stream; do not add new audio processing).
- Bar height: normalized dB mapped to 4–64dp. Bar color: the dB gradient color for that sample's level.
- Newest bar enters at full alpha; alpha fades linearly to 25% at the oldest (left) edge.
- Idle: flat 2dp center line in `text/muted` @ 30%, slow breathe.
- Low power refresh rate: update at 5 fps instead of 20 fps.

---

## 5. Screen-by-screen specifications

### 5.1 Meter — dB Meter mode

Layout top to bottom:

1. **Header** (all screens): brand glyph 24dp + screen title in `title/screen`. Remove the oversized logo+title lockup.
2. **Mode chips**: "dB Meter" / "Dosimeter" as M3 segmented control (not two floating pills): one container `bg/surface2` fully rounded, selected segment `brand/primaryContainer` fill + `brand/primary` text, `spring/snappy` slide of the selection.
3. **REC status** (measuring only): pill overlaid at top-right of the gauge area — red dot 8dp with `pulse/rec` + elapsed time in `caption` tabular. **Fixes the current bug where the REC row scrolls under the header and clips.** The pill never scrolls out while measuring.
4. **Gauge** (§3).
5. **Waveform strip** (§4).
6. **Technical metadata row** (if toggle on): WEIGHTING / RESPONSE / SAMPLE RATE / INPUT as a 4-column single-row grid of `bg/surface2` tiles, 14dp radius, label in `label/section` + value in `body/emphasis`. Currently 2×2 with inconsistent widths — make it one row, equal widths, or 2×2 with identical tile heights if width < 360dp.
7. **Live details card**: unchanged structurally; MIN / LAEQ / MAX values switch to `display/stat` tabular; when no data show "–" in `text/muted`, not green "0" (a green zero reads as a measured value — it is not).
8. **Sound references row**: unchanged structurally, restyle per card tokens.
9. **Control row**: keep 4 controls. Play/Pause FAB 64dp `brand/primary` with `brand/onPrimary` icon; secondary buttons 48dp `bg/surface2` with hairline border. On press: M3 Expressive shape-morph (circle → rounded-square, `spring/snappy`).

### 5.2 Meter — Dosimeter mode

Redesign the unbalanced Dosimeter card (currently: ring left, two floating tiles right, dead space):

- Card title row: "Dosimeter" in `title/card` + standard badge chip (NIOSH REL etc.) right-aligned, `brand/primaryContainer` fill, `brand/primary` text.
- Content: two equal columns, 12dp gap.
  - Left: dose ring 120dp — same visual language as the main gauge (track 10dp, value arc with dB gradient mapped 0–100% where 0–50 green, 50–80 amber, 80–100 orange, >100 red), center = dose % in `display/stat` + "DOSE" in `label/section`.
  - Right: 2×2 tile grid filling the column: TWA, LAEQ, REMAINING, PROJECTED (use the four stats the app already computes; if only three exist, use a 1×3 vertical stack — do not leave dead space). Tiles `bg/surface2`, value `display/stat` 20sp, label `label/section`.
- No-data state: values "–" in `text/muted`; ring at 0 with muted track. Never bare em-dashes floating in empty tiles.

### 5.3 Trends — Overview

- Filter chips (Overview/Spectral/Env Mix + Weekly/Monthly): two scrollable chip rows, selected = `brand/primaryContainer` + `brand/primary` text. Never truncate labels — chips size to content.
- **Exposure card (P0 bug):** the current empty white rectangle must never render. Implement a proper bar chart:
  - 7 columns (day initials Mon–Sun in `caption`), bar width ~24dp, rounded 6dp tops.
  - Bar color = that day's average-level bucket color; bar height normalized to the week's max; today's bar gets a hairline `text/primary` outline.
  - Header: "Last 7 days" `title/card` left; average value `display/stat` + "AVG dB/DAY" `label/section` right.
  - Tap a bar → value label appears above it in a small `bg/surface3` chip.
  - Empty/insufficient data: chart area replaced by the standard empty state (§8) — never a blank or white box.
- Hearing status card + Reports card: restyle to standard card tokens. In the 12-month report the distribution bar becomes a single stacked bar (8dp tall, fully rounded) with segments in the four bucket colors, and the legend rows use the same colors (**fixes Quiet and Moderate currently both being green**).

### 5.4 Trends — Spectral (the showpiece screens)

Shared: mode chips **Bars / Spectrogram / RTA** — full labels, scrollable row, no truncation (**fixes "Spectrogr"**). "LIVE CAPTURE" indicator becomes a pill with pulsing dot (`pulse/rec` in `brand/primary`).

- **Bars:** height 240dp (from ~150dp). Each frequency bar colored by its own band level through the dB gradient. Bar tops rounded 2dp. Baseline hairline. Axis labels 20 Hz / 1 kHz / 20 kHz in `caption`. Bars animate with `spring/track`.
- **Spectrogram:** height 280dp. Render through the magma LUT (§2.3) — this alone transforms the screen. 14dp corner clip. Frequency axis labels below. Add a 4dp-tall horizontal legend strip under the axis showing the magma gradient with "quiet → loud" caption endpoints.
- **RTA:** height 240dp. Band bars colored by band level (same rule as Bars). Peak-hold: 2dp line above each bar in `text/primary` @ 80%, decay 1.5 dB/s. Stat tiles below (PEAK, BANDS, DOMINANT, BANDWIDTH, PEAK BAND, STATUS) in a 2-column grid of `bg/surface2` tiles, equal heights.
- Remove the large dead space below the card: the spectral card grows to fit its content and the section breathes with standard 32dp spacing.

### 5.5 Trends — Env Mix

- Live + 7-day cards: each row = colored dot 10dp (bucket color — four distinct colors, not two greens), label `body`, percentage `display/stat` 20sp tabular right-aligned.
- Add an 8dp stacked distribution bar at the top of each card (same component as §5.3 Reports) so the mix is visible at a glance, not just as numbers.

### 5.6 Hearing

- Hero card ("Start with a baseline hearing test"): title `title/screen` 20sp, body `body`, CTA = primary button (fully rounded, `brand/primary` fill, `brand/onPrimary` text, 52dp height). This is the only filled-primary button on the screen.
- Recovery check / Tinnitus pitch / Voice baseline cards: standard card tokens; secondary CTAs = outlined buttons (transparent, 1dp `border/focus`, `text/primary` label). Disabled state: `bg/surface2` fill, `text/muted` label, no border.
- "Avg. change / Largest change" placeholder em-dashes: `text/muted`, with helper caption "Awaiting baseline" underneath.

### 5.7 Pitch matcher & Ambient sound (sub-screens)

- Back-arrow + title header per §5.1 pattern. Titles must not truncate ("Personal tracking pitch p…"): shorten screen title to "Pitch profile"; keep the long form as body text.
- Ear selector: segmented control (§5.1 mode chips pattern), not two pills.
- Sliders (pitch frequency, volume): M3 Expressive slider — 6dp track, `brand/primary` active track, 16dp thumb with 4dp inner dot; value label above thumb while dragging.
- Sound-type chips (White/Pink/Brown/Fan) and timer chips: single scrollable row each, selected = `brand/primaryContainer` + `brand/primary` text.
- Play (primary filled) / Stop (outlined) buttons: 52dp, equal widths.
- Disclaimer line ("For personal tracking only. This is not a medical test.") in `caption`, `text/muted`.

### 5.8 History

- **24h context chart (P0 bug — currently one floating dot):** area chart. 1.5dp line in `brand/primary`; area fill = vertical gradient `brand/primary` 18% → 0%; time axis labels as today. Max point: 6dp dot + small chip with value. If fewer than 2 data points: standard empty state (§8) with "Not enough data yet — levels appear here as the day fills in."
- "LAST 24 HOURS / Average 38 dB · Stable" header: average in `display/stat`, trend word as a small chip with a subtle trend icon (→ stable, ↑ rising, ↓ falling).
- Search field: `bg/surface2`, 14dp radius, hairline border, focus = `brand/primary` border.
- Filter chips: standard chips; "No matching sessions" becomes the standard empty state (icon + message + "Clear filters" text button) instead of a bare string.
- Summary tiles: value `display/stat`, tabular; "+0%" must use `text/secondary` when the value is neutral (amber/orange only for meaningful warning levels — a +0% in amber currently signals a problem that does not exist).

### 5.9 Settings / Display

- Group cards per standard tokens; toggles = M3 Switch with `brand/primary` checked state.
- Segmented option rows (Refresh Rate, Dark Mode): segmented control pattern.
- **Dark Mode setting: no bug here, leave the logic as is.** The theme setting works correctly — a "force dark theme" override was active when reference screenshots were taken, which is why the UI rendered dark with "Light" selected. Do not change the theme-switching logic in this pass. This redesign implements the **dark theme only**; the light theme will be revisited in a separate pass after the dark redesign has been reviewed on device. Restyle the setting's controls (segmented control per this spec) but leave its behavior untouched.

---

## 6. Navigation & app-wide chrome

- **Bottom nav:** M3 NavigationBar on `bg/surface1`, hairline top border. Active item: pill indicator `brand/primaryContainer` behind the icon (`spring/snappy` slide between items), icon+label `brand/primary`. Inactive: `text/muted`. Labels in `caption`.
- **Screen transitions:** fade-through 220ms between bottom-nav destinations; standard M3 shared-axis for drill-ins (Ambient sound, Pitch profile).
- **Status bar / nav bar:** transparent, edge-to-edge, light icons.
- **Glance widget:** update colors to `bg/surface1` background, dB value in Space Grotesk tabular, classification badge in bucket colors — the widget must look like the app.

---

## 7. Signature features (new, small, high-impact)

### 7.1 Idle starfield

Behind the gauge, idle state only: ~60 static particles, 1–2dp, `text/primary` at random alpha 5–15%, slow drift (≤4dp/s) with gentle twinkle (alpha oscillation, randomized 4–8s periods). The observatory signature. Constraints: Canvas-drawn, ≤30fps, disabled when Low power refresh rate, battery saver, or system reduced-motion is active. Fades out (300ms) when measurement starts.

### 7.2 Share card

The existing share action generates a branded image (1080×1350 px) instead of/in addition to whatever it exports now:

- `bg/deep` background with the subtle gradient, brand glyph + "dBcheck" top-left, date/time top-right in `caption`.
- Center: large dB value (Space Grotesk 700) + classification badge in bucket colors.
- Below: MIN / LAEQ / MAX row and a static waveform snippet of the session colored by the dB gradient.
- Footer: "Measured with dBcheck" in `text/muted`.
- Rendered via Compose graphics into a bitmap; shared through the system share sheet.

### 7.3 Threshold haptics

- Crossing 85 dB upward during live measurement: single `CONFIRM`-class haptic tick (rate-limited: max once per 10s).
- Dosimeter dose reaching 100%: double tick.
- Respect system haptic settings; no separate in-app toggle unless one already exists.

---

## 8. Standard states (apply to every data surface)

**Empty state pattern:** centered column — icon 32dp `text/muted`, title `body/emphasis` `text/primary`, one-line body `body` `text/secondary`, optional text button in `brand/primary`. Max width 280dp. Used for: empty charts, no sessions, no baseline, no matching filters.

**Loading:** skeleton shimmer (`bg/surface2` base, `bg/surface3` highlight sweep, 1200ms) matching final layout; spinners only for user-triggered actions.

**No-measurement values:** always "–" in `text/muted` — never green zeros, never bare em-dashes without context, never blank tiles.

**Error:** card with `error`-tinted icon, plain-language message, retry action.

**Overflow:** chip labels never truncate (scrollable rows); long titles wrap to 2 lines max then ellipsize; stat values use `AutoSize` down to 70% before ellipsizing.

---

## 9. P0 bug-fix checklist (all fixed by the work above — verify each explicitly)

1. Trends 7-day exposure chart renders as an empty white rectangle → themed bar chart + empty state (§5.3).
2. History 24h chart shows a single floating dot → area chart + empty state (§5.8).
3. "Spectrogr" truncated chip label → scrollable, content-sized chips (§5.4).
4. REC row clips under the header while scrolling → pinned REC pill (§5.1).
5. Dosimeter card dead space / asymmetric tiles → two-column layout (§5.2).
6. Env Mix Quiet and Moderate share the same green → four distinct bucket colors (§5.5, §2.2).
7. "Personal tracking pitch p…" truncated screen title → "Pitch profile" (§5.7).
8. Green "0 dB" values shown before any measurement → muted "–" placeholders (§8).
9. Neutral "+0%" rendered in warning amber → neutral color for neutral values (§5.8).

Note: the Dark Mode setting is **not** a bug (a force-dark override was active in reference screenshots) — see §5.9. Do not modify theme-switching logic.

---

## 10. Implementation order

Work in this order so each step is verifiable on device. This is a working order, not a release plan — everything ships together in v1.0.

1. **Theme foundation:** token file (colors, type incl. Space Grotesk assets, shapes, springs), root background, cards, nav bar, chips, buttons, segmented controls. App-wide restyle falls out of this step.
2. **P0 bugs** (§9) — quick wins, immediately visible.
3. **Gauge** (§3) + waveform strip (§4) + Meter screen layouts (§5.1–5.2).
4. **Charts:** 7-day bars, 24h area chart, stacked distribution bars, Env Mix (§5.3, 5.5, 5.8).
5. **Spectral:** magma LUT spectrogram, colored Bars/RTA + peak hold (§5.4).
6. **Sub-screens & Settings** (§5.6–5.9).
7. **Signature features:** starfield, share card, haptics, widget refresh (§6–7).
8. **State pass:** every screen checked against §8 (empty/loading/error/overflow).

---

## 11. Acceptance checklist

- [ ] No hardcoded colors/dimensions in composables; all values come from the token file
- [ ] Every sound-level surface uses the §2.2 semantic scale; no gray or single-green level displays remain
- [ ] Gauge: gradient arc, tip glow, peak-hold marker, digit-roll value, idle breathe — all present
- [ ] Waveform strip live on Meter screen, colored by level
- [ ] Spectrogram renders through the magma LUT; Bars and RTA are level-colored; RTA has peak hold
- [ ] All nine §9 bugs verified fixed on device
- [ ] All numerals tabular; big values in Space Grotesk
- [ ] Every chart and list has designed empty/loading states; nothing renders blank or white
- [ ] Chip labels never truncate on a 360dp-wide device
- [ ] Animations respect reduced-motion and Low power settings
- [ ] Edge-to-edge with correct insets; touch targets ≥48dp
- [ ] Text contrast meets WCAG AA against its actual background
- [ ] Widget matches the new visual language
- [ ] Screenshots of Meter (active), Spectrogram, and Trends look Play-Store-ready

---

*End of specification.*


