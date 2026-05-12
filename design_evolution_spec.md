# dBcheck Design Evolution — "Technical Instrument Layer"

> **For Claude Code.** This document describes design changes to apply to the existing `dBcheck_design_spec.md`. The goal is to add a layer of technical credibility and real-instrument feel on top of the existing modern, editorial base design. Think: YouTube's clean hierarchy meets a professional audio meter's data density — but never sacrificing the breathing room and tonal layering that defines dBcheck.
>
> **Direction:** The app should feel like it *actually measures something*. Right now the design is beautiful but could be a wellness mockup. After these changes, a user should open the Meter screen and immediately feel "this is a real instrument" — while still thinking "this is the best-designed app on my phone."
>
> **Important:** These changes UPDATE sections of the existing design spec. Anything not mentioned here remains unchanged. Apply changes to the spec file first, then implement in code.

---

## Guiding Principle

**"Credible Precision"** — Every visual element should reinforce that this app produces real, trustworthy measurements. The key tension to manage: dense technical data presented with generous space and modern aesthetics. If something looks like a 2015 utility app, you've gone too far. If something looks like a wellness concept with no real data, you haven't gone far enough.

**Reference points (for mood, NOT for copying):**
- Professional audio meters: scrolling time-series graphs, real-time spectrograms, unit labels (dB-A, Hz, kHz)
- YouTube / modern Google apps: clean card hierarchy, pill chips, tonal surface layering, content-first
- High-end watch complications: small, precise data readouts in elegant frames

---

## Change 1: Real-Time dB Time-Series Graph (Meter Screen)

### What changes
The current spec describes a "subtle animated waveform line behind the min/avg/max stats" as decorative texture. **Replace this with a functional, real-time scrolling dB time-series chart.**

### Updated Meter Screen layout

```
┌─────────────────────────────────────┐
│ ⦿ dBcheck                       ⚙  │  ← Top bar
│                                     │
│         ┌───────────────┐           │
│        │    DECIBELS     │          │
│        │      63         │          │
│        │       dB        │          │
│        │ NORMAL CONVERS. │          │
│         └───────────────┘           │
│      (circular gauge with arc)      │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  dB                         │   │  ← Y-axis label
│   │  80─ ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈  │   │  ← Danger threshold (dashed)
│   │  60─ ──────/\───/\──────── │   │  ← Live line with gradient fill
│   │  40─ ─────/  ──/  \─────── │   │
│   │  20─                        │   │
│   │    -30s  -20s  -10s    NOW  │   │  ← X-axis, scrolling time window
│   └─────────────────────────────┘   │
│                                     │
│   ┌─MIN──┐  ┌─AVG──┐  ┌─MAX──┐     │
│   │  32  │  │  58  │  │  84  │     │
│   │  dB  │  │  dB  │  │  dB  │     │
│   └──────┘  └──────┘  └──────┘     │
│                                     │
│      ↻        ▶ ||        ⤴        │
│             (large FAB)             │
│                                     │
├─────────┬─────────┬─────────┬───────┤
│ METER   │ ANALYT  │ HISTORY │ SETT  │
└─────────┴─────────┴─────────┴───────┘
```

### Spec details

- **Chart type:** Area/line chart. Smooth line (cubic bezier interpolation) showing dB level over time.
- **Time window:** Last 30 seconds, scrolling. X-axis labels: `-30s`, `-20s`, `-10s`, `NOW`.
- **Y-axis:** 0–120 dB range. Show gridlines at 20dB intervals. Y-axis labels in `label-sm` (Space Grotesk).
- **Line style:** 2dp stroke width, `primary` color. Area fill below the line using `primary` at 8% opacity (dark) or 10% opacity (light).
- **Threshold line:** Dashed horizontal line at 85dB in `warning` color at 40% opacity. Label "85 dB" right-aligned on the line in `label-sm`.
- **Peak markers:** When a new session MAX is detected, show a brief dot/pulse on the line at that point.
- **Container:** `surface-container` background, `rounded-xl` corners, internal padding `space-4`. No border.
- **Height:** Approximately 160dp — enough to read trends but not dominating the screen.
- **Data update rate:** Line draws a new point every 100ms (10fps to match dB value update rate from the existing spec). Chart scrolls smoothly.
- **Empty state:** Before measurement starts, show flat line at 0 with muted text "Start measuring to see live data".

### Interaction
- The chart is display-only in the Meter screen (no tap/zoom). Detailed history graphs live in the History tab.
- When measurement is paused, the chart freezes and the line stops at the pause point. "PAUSED" label appears top-right of chart in `on-surface-variant`.

---

## Change 2: dB Reference Context (Meter Screen)

### What changes
Add a contextual reference indicator that maps the current dB reading to familiar real-world sounds. This replaces the simple noise level pill label inside the gauge with a richer reference system.

### Design

The existing noise level pill inside the gauge ("NORMAL CONVERSATION") stays but becomes the **primary label**. Below the MIN/AVG/MAX stat cards, add an expandable reference strip:

```
┌─────────────────────────────────────┐
│  📊 SOUND REFERENCE                 │  ← label-md, tappable header
│                                     │
│  ▐░░░░░░░░░░░░░░░░░░░░░▌           │  ← Horizontal scale bar
│  0    20    40    60    80   100 120 │
│               ▲                     │  ← Current dB marker (triangle)
│              63 dB                  │
│                                     │
│  🍃 Rustling leaves      20 dB     │  ← Reference items, muted
│  💬 Normal conversation  60 dB     │  ← Highlighted (closest match)
│  🚗 Busy traffic         80 dB     │
│  ⚠️  Damage threshold    85 dB     │  ← Warning color
│  🎸 Rock concert        110 dB     │
└─────────────────────────────────────┘
```

### Spec details

- **Collapsed state (default):** Only the horizontal scale bar + current dB marker is visible. Compact, about 48dp height. Shows as a thin card below the stats.
- **Expanded state:** Tap to expand, revealing the full reference list. Smooth height animation (250ms, ease-out).
- **Scale bar:** Horizontal gradient bar from `success` (left/quiet) through `primary` (moderate) through `warning` to `error` (right/dangerous). 4dp height, full width, `rounded-full` corners.
- **Marker:** Small inverted triangle or diamond shape on the scale bar at the current dB position. Uses `on-surface` color. Animates smoothly as dB changes.
- **Reference list:** 5–7 common reference sounds. The one closest to current dB is highlighted with `on-surface` text + `primary` accent. Others use `on-surface-variant`.
- **Reference data:**

| dB | Label | Icon |
|----|-------|------|
| 10 | Breathing | 🌬 |
| 20 | Rustling leaves | 🍃 |
| 30 | Whisper | 🤫 |
| 40 | Quiet library | 📚 |
| 50 | Quiet office | 🏢 |
| 60 | Normal conversation | 💬 |
| 70 | Busy restaurant | 🍽 |
| 80 | Busy traffic | 🚗 |
| 85 | Damage threshold | ⚠️ |
| 90 | Motorcycle | 🏍 |
| 100 | Subway train | 🚇 |
| 110 | Rock concert | 🎸 |
| 120 | Thunder / Pain | ⛈ |

- **Container:** `surface-container-high`, `rounded-xl`, padding `space-4`.
- Use actual Material icons or similar vector icons, NOT emoji in production. The emoji above is for this spec only.

---

## Change 3: Technical Metadata in Meter Screen

### What changes
Add small, precise technical readouts to the Meter screen that signal "real instrument" without cluttering the layout.

### New elements

**Session info bar** — a subtle row between the top bar and the gauge:

```
┌─────────────────────────────────────┐
│ ⦿ dBcheck                       ⚙  │
│                                     │
│  ● REC  00:04:32    A-WEIGHT  44.1k │  ← Session info bar
│                                     │
│         ┌───────────────┐           │
│         │  gauge...      │          │
```

### Spec details

- **Layout:** Single row, horizontal, centered vertically. Height: 32dp. Full width with `space-4` horizontal padding.
- **Background:** Transparent (sits on `background`).
- **Elements (left to right):**
  - **Recording indicator:** Small 6dp filled circle in `error` color that pulses (opacity 100% → 50%, 1.5s cycle) when actively measuring. When paused, show a static `on-surface-variant` circle. Label "REC" or "PAUSED" in `label-sm`, `on-surface-variant`.
  - **Session duration:** `data-md` (Space Grotesk), `on-surface-variant` color. Format: `HH:MM:SS`. Counts up from session start.
  - **Spacer** (flexible)
  - **Weighting indicator:** `label-sm`, `on-surface-variant`. Shows current frequency weighting: "A-WEIGHT", "C-WEIGHT", or "Z-WEIGHT". This is a passive display, not a control (changing it is in Settings). Pro users see their selected weighting; free users always see "A-WEIGHT".
  - **Sample rate:** `label-sm`, `on-surface-variant`. Shows "44.1k" or "48k" depending on device. Purely informational.
- **Interaction:** Tapping the session duration does nothing (it's informational). The weighting label could be tappable to jump to Settings for Pro users — but this is optional, not required for MVP.

### Why this matters
Professional audio apps always show recording status, duration, and technical parameters. These tiny details are the difference between "toy app" and "instrument app". They cost almost nothing in screen space but massively increase perceived quality.

---

## Change 4: Enhanced Spectral Analysis (Analytics Screen)

### What changes
The existing spec describes spectral analysis as a simple frequency bar chart. Upgrade it to a more visually impressive and technically credible visualization.

### Updated Spectral Analysis card

```
┌── Spectral Analysis ──────────────────┐
│  ● LIVE CAPTURE              [PRO] 🔒 │  ← label-md
│                                        │
│  kHz                                   │
│  16─  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│   8─  ░░░░░░░░░▓▓▓░░░░░░░░░░░░░░░░░  │  ← Scrolling spectrogram
│   4─  ░░░░░▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░  │     (heat map style)
│   2─  ░░░▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░  │
│   1─  ░░▓▓▓▓██████▓▓▓▓▓▓░░░░░░░░░░░  │
│ 500─  ░▓▓▓██████████▓▓▓▓▓░░░░░░░░░░  │
│ 250─  ▓▓▓▓██████████▓▓▓▓▓▓░░░░░░░░░  │
│        -10s  -8s  -6s  -4s  -2s  NOW  │
│                                        │
│  ┌─ DOMINANT ─┐  ┌─ BANDWIDTH ─┐      │
│  │  2.4 kHz   │  │  Wide       │      │
│  └────────────┘  └─────────────┘      │
│                                        │
│  ┌─ PEAK FREQ ─┐ ┌─ NOISE FLOOR ─┐   │
│  │  1.8 kHz    │ │  32 dB         │   │
│  └─────────────┘ └────────────────┘   │
└────────────────────────────────────────┘
```

### Spec details

**Dual visualization mode:**
1. **Bar mode (default, simpler):** Classic vertical frequency bars as in the current spec. Good for quick glance.
2. **Spectrogram mode (Pro, advanced):** Scrolling heat map (waterfall spectrogram) where X = time, Y = frequency, color intensity = amplitude. This is the "wow" visualization.

- **Spectrogram colors:** Use a custom ramp from `surface-container` (silence) → `primary-dim` (low) → `primary` (moderate) → `secondary` (loud) → `warning` (peak). This keeps it on-brand rather than using generic rainbow colors.
- **Scrolling:** Time scrolls left, newest data appears at right edge. 10-second visible window.
- **Y-axis:** Logarithmic frequency scale (standard for audio). Labels: 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz. Use `label-sm`.
- **Resolution:** Each column represents ~100ms of data. Visual resolution depends on FFT size (implementation detail for Claude Code).

**Additional stat pills (below the visualization):**
- **Peak Frequency:** The frequency with the highest amplitude in the current frame.
- **Noise Floor:** The lowest measured ambient level in the current session.
- These use the same stat card style as MIN/AVG/MAX on the Meter screen.

**Toggle between modes:** Small segmented control above the visualization: `[BARS]` / `[SPECTROGRAM]`. Uses chip styling (selected = `primary-container`, unselected = `surface-container-high`).

**Free user view:** Shows a blurred/frosted preview of the spectrogram with a centered lock overlay and "Unlock with Pro" CTA. The blur should be enough to hint at the richness of the feature without giving it away.

---

## Change 5: Instrument Typography Refinements

### What changes
Push the existing Space Grotesk usage further to create a stronger "instrument" identity. The current spec already uses Space Grotesk for data — these changes make it more prominent and precise.

### Updated type tokens

Add these new tokens to the existing type scale:

| Token | Font | Size | Weight | Letter Spacing | Usage |
|-------|------|------|--------|----------------|-------|
| `data-2xl` | Space Grotesk | 2.5rem (40sp) | 700 | -0.02em | Hero stat on History 24h card, future dashboard |
| `unit` | Space Grotesk | 0.625rem (10sp) | 500 | 0.12em | Unit labels (dB, Hz, kHz, %) always shown with data values |

### Specific typography changes

1. **dB unit display:** Whenever a decibel value is shown (63 dB, 84 dB, etc.), the number and the unit should be styled differently:
   - Number: `data-lg` or `data-xl` (as currently specified), full `on-surface` color
   - "dB" unit: `unit` token, `on-surface-variant` color, baseline-aligned with the number but smaller
   - This creates the "instrument readout" pattern: **63** ^dB where the unit is clearly subordinate

2. **Frequency values:** Same pattern for Hz and kHz values:
   - "2.4" in `data-lg`, "kHz" in `unit` token
   - "1000" in `data-xl`, "Hz" in `unit` token

3. **Percentage values:** Same split for all percentages:
   - "42" in `data-lg`, "%" in `unit` token

4. **Tracked-out labels for technical metadata:** The session info bar labels (REC, A-WEIGHT, 44.1k) and chart axis labels should use wider letter-spacing than regular labels. Use `label-sm` with letter-spacing `0.12em` instead of `0.05em`.

5. **Tabular figures:** ALL numeric data displays (dB values, percentages, frequencies, durations) must use tabular figures (monospaced numerals) so digits don't shift when values change. Space Grotesk supports this via OpenType `tnum` feature. This is critical for the Meter screen where numbers update rapidly — proportional figures cause visual jitter.

### Typography rule addition
Add to the Typography Rules section:
> "All numeric data must use tabular (monospaced) figures via the OpenType `tnum` feature. This prevents visual jitter when values update in real-time and reinforces the instrument aesthetic."

---

## Implementation Order

These changes should be applied in this sequence:

1. **Typography refinements (Change 5)** — affects everything else, do first
2. **Session info bar (Change 3)** — small, self-contained addition to Meter
3. **Time-series chart (Change 1)** — major Meter screen addition
4. **Reference chart (Change 2)** — expandable card below the chart
5. **Spectral analysis (Change 4)** — Analytics screen Pro feature

Each change should be implemented and tested (both themes) before moving to the next.

---

## What NOT to change

- **Color system** — no changes. The existing palette stays.
- **Navigation structure** — still 4 tabs, hearing test still in Analytics.
- **Circular gauge** — stays as the hero element. The time-series chart supplements it, doesn't replace it.
- **Spacing scale** — no changes. Use existing tokens generously.
- **Corner radius** — no changes. Minimum 8dp rule stays.
- **No-line rule** — still no 1px borders for sectioning.
- **Pro/Free matrix** — no changes to what's gated. The new spectral features are all Pro.
- **Card and component styling** — unchanged. New elements use the same card patterns.

---

## Design Spec Update Instructions

Claude Code should apply these changes by editing `dBcheck_design_spec.md` directly:

1. Update **Section 1 (Creative Direction)** — change the North Star description to incorporate the "credible precision" principle. Add "Technical credibility through data transparency" to Key Principles.
2. Update **Section 4 (Typography)** — add the new `data-2xl` and `unit` tokens to the type scale table. Add the tabular figures rule.
3. Update **Section 9.1 (Meter Screen)** — replace the waveform description with the time-series chart spec. Add the session info bar. Add the reference chart as a new expandable element.
4. Update **Section 9.2 (Analytics Screen)** — replace the Spectral Analysis card description with the enhanced version including spectrogram mode.
5. Do NOT change any other sections unless absolutely necessary for consistency.

After updating the spec, proceed with code implementation following the Implementation Order above.
