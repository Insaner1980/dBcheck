# dBcheck — Unified Design & Implementation Specification

> **Purpose:** This document is the single source of truth for the dBcheck Android app.
> Claude Code must follow this spec. The HTML mockup files and PNG screenshots in the project folder are **style references only** — use them for color values, spacing feel, and visual tone, but do NOT copy their layout or content if it conflicts with this spec.

---

## 1. Creative Direction

**Style Direction:** Custom Android (Kotlin / Jetpack Compose) — inspired by Material 3 but with a distinct editorial wellness-instrument aesthetic. Not standard Material You.

**North Star:** "The Auditory Observatory" — a premium digital health instrument that feels immersive and calm, not utilitarian. Data floats within atmosphere rather than sitting in boxes. The app should feel like a high-end wearable's companion, not a generic tool.

**Key Principles:**
- Breathing room over density — generous whitespace between all elements
- Tonal layering over borders — depth through background color shifts, never 1px lines
- Editorial typography — high-contrast scale (large display + small tracked labels)
- Organic shapes — minimum 12dp corner radius on all containers
- Signature gradient — lime-to-lemon accent as the app's "pulse of energy"

---

## 2. App Structure & Navigation

### Navigation: Bottom Navigation Bar (4 tabs)

```
┌─────────────────────────────────────────┐
│                                         │
│            [Screen Content]             │
│                                         │
├─────────┬─────────┬─────────┬───────────┤
│  ✏ Meter │ ≋ Analy │ ⟳ Hist │ ⚙ Settings│
└─────────┴─────────┴─────────┴───────────┘
```

| Tab | Label | Icon | Description |
|-----|-------|------|-------------|
| 1 | Meter | Waveform/pen icon | Live dB measurement — the primary screen |
| 2 | Analytics | Chart icon | Weekly/monthly exposure data & health insights |
| 3 | History | Clock/history icon | Session log and trend data |
| 4 | Settings | Gear icon | Calibration, notifications, appearance, about |

**Active tab:** Icon + label visible, accent color applied.
**Inactive tab:** Icon only (or icon + muted label), `on-surface-variant` color.

The hearing test is NOT a tab. It lives as a card/CTA within Analytics ("Check Your Hearing") and opens as a full-screen flow (Phase 2 feature).

### Top App Bar (per screen)

Each screen has a minimal top bar:
- Left: dBcheck logo (waveform icon + "dBcheck" text in Manrope SemiBold)
- Right: Contextual action (gear icon on Meter screen links to Settings, profile/notification icon elsewhere)
- Style: Transparent/surface background, no elevation shadow

---

## 3. Color System

### Design Principle: "No-Line Rule"
**1px solid borders are prohibited for sectioning.** Boundaries are created through background color shifts. The only exception is the "Ghost Border" (see Elevation section) for inputs and accessibility edge cases.

### Token Mapping (Dark ↔ Light)

Both themes use the same token names. Only values change.

| Token | Dark | Light | Usage |
|-------|------|-------|-------|
| `background` | #0e0e0e | #f9f9f9 | Base layer, infinite canvas |
| `surface` | #131313 | #f3f4f4 | Sectional grouping areas |
| `surface-container` | #1a1a1a | #eceeee | Secondary grouped content |
| `surface-container-high` | #201f1f | #e0e3e4 | Cards, actionable modules |
| `surface-container-highest` | #262626 | #d8dbdc | Focused elements, selected states |
| `surface-container-lowest` | #000000 | #ffffff | Recessed/carved areas (dark) or lifted cards (light) |
| `on-surface` | #e8e4e0 | #2f3334 | Primary text |
| `on-surface-variant` | #adaaaa | #5c6060 | Secondary text, labels |
| `primary` | #c5fe00 | #466906 | Main accent — CTAs, active states |
| `primary-dim` | #b9ef00 | #5a8a0a | Accent variant for glows/tints |
| `primary-container` | #c5fe00 | #d4f5a0 | Chip selected bg, badges |
| `on-primary-container` | #1a2600 | #1a2600 | Text on primary containers |
| `secondary` | #dfec60 | #954b00 | Gradient endpoint, warm accent |
| `tertiary` | #5f5f5f | #5f5f5f | Waveform strokes, muted elements |
| `tertiary-fixed-dim` | #2a2a2a | #f3f0f0 | Background waveform texture |
| `outline-variant` | rgba(175,178,179, 0.15) | rgba(175,178,179, 0.20) | Ghost borders only |
| `error` | #f87171 | #ba1a1a | Excessive noise, alerts |
| `warning` | #fbbf24 | #cc7700 | Elevated noise levels |
| `success` | #4ade80 | #2d7a2d | Safe zone, optimal status |

### Signature Gradient
Used for primary buttons and the active meter arc:
- **Dark:** `primary` (#c5fe00) → `secondary` (#dfec60)
- **Light:** `primary` (#466906) → `secondary` (#954b00)

Direction: top-left to bottom-right (135°) for buttons, circular sweep for meter arc.

### Surface Tint Rule
- **Dark:** No tint needed — the darkness is the canvas.
- **Light:** Apply `primary` (#466906) at 2–3% opacity over white sections to create a "warm white" rather than clinical white.

### Glassmorphism (floating elements only)
Used for: bottom sheet overlays, the meter's circular container, modal headers.
- **Dark:** `surface` at 60% opacity + `backdrop-blur: 20px`
- **Light:** `surface` at 70% opacity + `backdrop-blur: 20px`

---

## 4. Typography

### Font Stack
| Role | Font | Usage |
|------|------|-------|
| **Primary** | Manrope | Headlines, body text, navigation, UI labels |
| **Data** | Space Grotesk | All numeric values: dB readings, percentages, frequencies, timestamps, stats |

Both fonts are Google Fonts and must be used in **both** themes identically.

### Type Scale (Material 3 adapted)

| Token | Font | Size | Weight | Letter Spacing | Usage |
|-------|------|------|--------|----------------|-------|
| `display-lg` | Manrope | 3.5rem (56sp) | 700 | -0.02em | Live dB readout |
| `display-md` | Manrope | 2.75rem (44sp) | 700 | -0.02em | Wellness scores, hero numbers |
| `headline-lg` | Manrope | 2rem (32sp) | 600 | -0.01em | Screen titles ("Analytics", "History") |
| `headline-md` | Manrope | 1.5rem (24sp) | 600 | 0 | Section headers ("Exposure Summary") |
| `body-lg` | Manrope | 1rem (16sp) | 400 | 0 | Instructional text, descriptions |
| `body-md` | Manrope | 0.875rem (14sp) | 400 | 0 | Card content, list items |
| `label-lg` | Space Grotesk | 0.875rem (14sp) | 500 | 0.05em | Prominent data labels ("PEAK", "AVG") |
| `label-md` | Space Grotesk | 0.75rem (12sp) | 500 | 0.08em | Uppercase tags, metadata ("LAST 7 DAYS") |
| `label-sm` | Space Grotesk | 0.6875rem (11sp) | 400 | 0.05em | Fine print, axis labels |
| `data-xl` | Space Grotesk | 2rem (32sp) | 600 | -0.01em | Large stat values (68.4, 42.8) |
| `data-lg` | Space Grotesk | 1.5rem (24sp) | 600 | 0 | Medium stat values (84 dB, 92%) |
| `data-md` | Space Grotesk | 1rem (16sp) | 500 | 0 | Small stat values in lists |

### Typography Rules
- Never use pure black (#000000) for text. Use `on-surface` tokens.
- Headlines use negative letter-spacing for a "tight, premium" feel.
- Labels use positive letter-spacing for a "tracked-out, instrument" feel.
- Uppercase is reserved for `label-md` and `label-sm` only (tags, metadata).
- Line height: 1.3 for display/headline, 1.5 for body, 1.2 for labels/data.

---

## 5. Elevation & Depth

### Tonal Layering (primary method)
Depth is achieved by nesting surfaces. A `surface-container-high` card on a `surface` background creates natural lift without shadows.

```
┌── background ─────────────────────────┐
│                                       │
│  ┌── surface ──────────────────────┐  │
│  │                                 │  │
│  │  ┌── surface-container-high ─┐  │  │
│  │  │  Card content             │  │  │
│  │  └───────────────────────────┘  │  │
│  │                                 │  │
│  └─────────────────────────────────┘  │
│                                       │
└───────────────────────────────────────┘
```

### Ambient Shadows (floating elements only)
For the FAB, floating bottom nav, and modal overlays:
- **Dark:** `offset-y: 12px`, `blur: 24px`, color: `primary-dim` at 4% opacity
- **Light:** `offset-y: 12px`, `blur: 24px`, color: `on-surface` at 4% opacity

**Never** use standard Material elevation shadows. They look heavy in both themes.

### Ghost Border
When a container needs an edge for accessibility (e.g., input fields, same-color-on-same-color situations):
- Use `outline-variant` at 15–20% opacity
- Max 1px width
- Never 100% opaque

### Active Glows (dark theme only)
When noise peaks or alerts trigger:
- 12px outer glow using `primary-dim` at 15% opacity
- Creates an "energy emission" effect that doesn't work in light theme

### Waveform Texture (background decoration)
Subtle waveform shapes as background texture:
- **Dark:** `tertiary-fixed-dim` (#2a2a2a) stroke, sits behind content
- **Light:** `tertiary-fixed-dim` (#f3f0f0) stroke, sits behind content
- Always behind content, never competing for attention

---

## 6. Spacing Scale

Based on 8dp grid (Android standard):

| Token | Value | Usage |
|-------|-------|-------|
| `space-1` | 4dp | Inline icon-to-text gap |
| `space-2` | 8dp | Tight padding (chips, small elements) |
| `space-3` | 12dp | Default internal padding |
| `space-4` | 16dp | Card internal padding, list item spacing |
| `space-5` | 20dp | Section internal padding |
| `space-6` | 24dp | Between related sections |
| `space-8` | 32dp | Between major sections |
| `space-10` | 40dp | Screen top/bottom breathing room |
| `space-12` | 48dp | Between functional blocks |
| `space-16` | 64dp | Hero spacing (above/below meter) |

**Breathing Room Rule:** If spacing looks "enough", add one more step. Wellness apps need generous whitespace.

---

## 7. Corner Radius Scale

| Token | Value | Usage |
|-------|-------|-------|
| `rounded-sm` | 8dp | Small chips, badges |
| `rounded-md` | 12dp | Input fields, small cards |
| `rounded-lg` | 16dp | Standard cards, containers |
| `rounded-xl` | 24dp | Large cards, section wrappers |
| `rounded-2xl` | 28dp | Bottom sheets, prominent cards |
| `rounded-full` | 9999dp | Buttons, FAB, circular elements |

**Rule:** No sharp corners anywhere. Minimum is `rounded-sm` (8dp).

---

## 8. Components

### 8.1 Buttons

**Primary (CTA):**
- Shape: `rounded-full` (pill)
- Background: Signature gradient (`primary` → `secondary`)
- Text: `on-primary-container` (#1a2600), Manrope SemiBold
- Height: 56dp (large) or 48dp (standard)
- No shadow, no border
- Pressed state: reduce opacity to 85%

**Secondary:**
- Shape: `rounded-full`
- Background: `surface-container-highest`
- Text: `on-surface`
- No border
- Pressed state: darken background 8%

**Tertiary (text button):**
- Text-only, `primary` color
- Style: `label-lg`, uppercase
- No background
- Pressed state: add `primary` at 8% opacity as background

### 8.2 Cards

- Background: `surface-container-high` (or `surface-container-lowest` in light theme for "lifted" cards)
- Corner radius: `rounded-xl` (24dp)
- Padding: `space-5` (20dp)
- **No borders, no dividers inside cards**
- Separate cards with `space-4` (16dp) vertical gap
- Items within a card: separate with `space-3` (12dp) vertical gap, no divider lines

### 8.3 Chips (filters/tags)

**Selected:**
- Background: `primary-container`
- Text: `on-primary-container`, `label-lg`
- Shape: `rounded-full`

**Unselected:**
- Background: `surface-container-high`
- Text: `on-surface-variant`
- Shape: `rounded-full`
- No border

### 8.4 Input Fields

- Background: `surface-container` (resting), `surface-container-lowest` (focused)
- Shape: `rounded-md` (12dp)
- No visible border at rest
- Focus state: Ghost Border in `primary` at 30% opacity
- Text: `on-surface`, placeholder: `on-surface-variant`
- Height: 48dp

### 8.5 Sliders

- Track inactive: `surface-container-highest`
- Track active: Signature gradient
- Thumb: `primary` color, 24dp circle, subtle ambient shadow
- Value label: `data-md` (Space Grotesk) shown above thumb

### 8.6 Toggle Switches

- Off: `surface-container-highest` track, `on-surface-variant` thumb
- On: `primary` track, `surface-container-lowest` thumb
- Shape: Material 3 standard

### 8.7 Bottom Navigation Bar

- Background: Glassmorphism (`surface` at 60%/70% + blur)
- Height: 64dp + safe area inset
- Active item: `primary` color icon + label, subtle `primary` background tint behind icon
- Inactive item: `on-surface-variant` icon, no label (or muted label)
- Shape: items have `rounded-lg` indicator behind active icon

### 8.8 Session Cards (History list)

```
┌─────────────────────────────────────┐
│  🌙  Late Night Study       PEAK AVG│
│      OCT 24 · 23:14         58  32  │
│                              dB  dB │
└─────────────────────────────────────┘
```

- Icon: Contextual emoji/icon in a tinted circle (`surface-container` bg)
- Title: `body-lg` Manrope
- Metadata: `label-sm` Space Grotesk
- Stats: `data-lg` Space Grotesk, right-aligned
- Stat labels: `label-md` Space Grotesk uppercase

---

## 9. Screen Specifications

### 9.1 Meter Screen (Home / Default)

The primary screen. Shows live decibel measurement.

```
┌─────────────────────────────────────┐
│ ⦿ dBcheck                       ⚙  │  ← Top bar
│                                     │
│                                     │
│         ┌───────────────┐           │
│        │    DECIBELS     │          │  ← label-md, uppercase
│        │                 │          │
│        │      63         │          │  ← display-lg, Space Grotesk
│        │       dB        │          │  ← data-lg
│        │                 │          │
│        │ NORMAL CONVERS. │          │  ← label-md in chip/pill
│         └───────────────┘           │
│      (circular gauge with arc)      │
│                                     │
│    ∿∿∿∿ waveform visualization ∿∿∿∿ │  ← Subtle, behind stats
│                                     │
│   ┌─MIN──┐  ┌─AVG──┐  ┌─MAX──┐     │
│   │  32  │  │  58  │  │  84  │     │  ← data-lg Space Grotesk
│   │  dB  │  │  dB  │  │  dB  │     │
│   └──────┘  └──────┘  └──────┘     │
│                                     │
│                                     │
│      ↻        ▶ ||        ⤴        │  ← Reset, Play/Pause, Share
│             (large FAB)             │
│                                     │
├─────────┬─────────┬─────────┬───────┤
│ METER   │ ANALYT  │ HISTORY │ SETT  │
└─────────┴─────────┴─────────┴───────┘
```

**Elements:**
- **Circular Gauge:** Glass container (glassmorphism), gradient arc shows current level, dashed marks around circumference. The arc sweeps from bottom-left to top-right. Color shifts: green (safe) → gradient (moderate) → `warning` (loud) → `error` (dangerous).
- **Noise Level Label:** Shows contextual text in a pill: "Whisper", "Normal Conversation", "Busy Traffic", "Dangerous".
- **Waveform:** Subtle animated line behind the min/avg/max stats. `tertiary` color, very low opacity.
- **Stat Cards:** Three equal-width mini-cards showing MIN/AVG/MAX. Labels in `label-md` uppercase, values in `data-lg`.
- **Controls:** Reset (left, secondary button), Play/Pause (center, large primary FAB with gradient), Share (right, secondary button).

**Noise Level Thresholds:**
| Range | Label | Color |
|-------|-------|-------|
| 0–40 dB | Quiet / Whisper | `success` |
| 40–70 dB | Normal Conversation | `primary` |
| 70–85 dB | Busy / Elevated | `warning` |
| 85+ dB | Dangerous / Harmful | `error` |

### 9.2 Analytics Screen

Weekly and monthly exposure insights.

```
┌─────────────────────────────────────┐
│ ⦿ dBcheck                       👤  │
│                                     │
│ WEEKLY PERFORMANCE                  │  ← label-md
│ Analytics                           │  ← headline-lg
│                                     │
│ ┌── Exposure Summary ─────────────┐ │
│ │ LAST 7 DAYS (DB AVERAGE)  68.4  │ │  ← label-md + data-xl
│ │                        AVG DB/DAY│ │
│ │                                  │ │
│ │  ▊ ▊ ▊ ▊ ▊ ▊ ▊  (bar chart)    │ │
│ │  M  T  W  T  F  S  S            │ │
│ └──────────────────────────────────┘ │
│                                     │
│ ┌── Hearing Health ───────────────┐ │
│ │ ✓ Your hearing is in the        │ │
│ │   Safe Zone.                    │ │
│ │                                  │ │
│ │   Exposure today is 12% below   │ │
│ │   your weekly average.          │ │
│ │                                  │ │
│ │   [ VIEW TIPS ]                 │ │  ← Primary button
│ └──────────────────────────────────┘ │
│                                     │
│ ┌── Spectral Analysis ────────────┐ │
│ │ ● LIVE CAPTURE         label-md │ │
│ │                                  │ │
│ │  |||||||||| (frequency bars)    │ │
│ │  20Hz    1kHz         20kHz     │ │
│ │                                  │ │
│ │  DOMINANT    BANDWIDTH           │ │  ← [PRO]
│ │  2.4 kHz     Wide                │ │
│ └──────────────────────────────────┘ │
│                                     │
│ ┌── Environment Mix ──────────────┐ │
│ │ ● Quiet      52%                │ │
│ │ ● Moderate   34%                │ │  ← [PRO]
│ │ ● Loud       12%                │ │
│ │ ● Critical    2%                │ │
│ └──────────────────────────────────┘ │
│                                     │
│ ┌── Hearing Test CTA ─────────────┐ │
│ │ 🎧 Check Your Hearing           │ │  ← Phase 2
│ │ Quick 3-minute assessment       │ │
│ │ [ START TEST → ]                │ │
│ └──────────────────────────────────┘ │
│                                     │
├─────────┴─────────┴─────────┴───────┤
```

**Notes:**
- "Spectral Analysis" and "Environment Mix" are **Pro** features. Free users see a preview with a lock overlay and "Unlock with Pro" CTA.
- The bar chart uses the signature gradient for bars, with the current day highlighted.
- "Hearing Health" card uses `success` color for the checkmark when in safe zone, `warning` or `error` otherwise.

### 9.3 History Screen

Session log and trend overview.

```
┌─────────────────────────────────────┐
│ ⦿ dBcheck                       ⚙  │
│                                     │
│ EXPOSURE INSIGHTS                   │  ← label-md
│ History                             │  ← headline-lg
│                                     │
│ ┌── Last 24 Hours ────────────────┐ │
│ │ Average 42 dB · Stable     68   │ │
│ │                          PEAK DB │ │
│ │                                  │ │
│ │  📈 (24h line/area chart)       │ │
│ │  00:00  06:00  12:00  18:00 NOW │ │
│ └──────────────────────────────────┘ │
│                                     │
│ RECENT SESSIONS          View All → │  ← label-md + tertiary btn
│                                     │
│ ┌──────────────────────────────────┐ │
│ │ 🌙 Late Night Study    58  32   │ │
│ │    OCT 24 · 23:14      PEAK AVG │ │
│ ├──────────────────────────────────┤ │  ← spacing, NOT a line
│ │ ☕ Coffee Shop          74  65   │ │
│ │    OCT 24 · 14:20      PEAK AVG │ │
│ ├──────────────────────────────────┤ │
│ │ 🚇 Subway Commute      92  84   │ │
│ │    OCT 24 · 08:45      PEAK AVG │ │
│ ├──────────────────────────────────┤ │
│ │ 🌙 Sleep Monitor        42  28   │ │
│ │    OCT 23 · 22:30      PEAK AVG │ │
│ └──────────────────────────────────┘ │
│                                     │
│ ┌─ Weekly Trend ─┐ ┌─ Safe Hours ─┐ │
│ │   -12%         │ │   22.4h      │ │
│ │ Quieter than   │ │ Within safe  │ │
│ │ last week      │ │ dB limits    │ │
│ └────────────────┘ └──────────────┘ │
│                                     │
├─────────┴─────────┴─────────┴───────┤
```

**Notes:**
- Session naming and tagging is a **Pro** feature. Free users see sessions with auto-generated names (timestamps).
- "View All" links to a full session list.
- 24h chart: area/line chart with gradient fill. Peak moments highlighted.
- Summary cards at bottom are side-by-side, equal width.
- Session list items are separated by `space-4` spacing, never by divider lines.
- Peak dB values > 85 shown in `warning` or `error` color.

### 9.4 Settings Screen

```
┌─────────────────────────────────────┐
│ ⦿ dBcheck                       👤  │
│                                     │
│ SYSTEM PREFERENCES                  │  ← label-md
│ Settings                            │  ← headline-lg
│                                     │
│ 🎛 AUDIO CALIBRATION               │  ← label-md section header
│                                     │
│ ┌──────────────────────────────────┐ │
│ │ Microphone Sensitivity   +4.2dB │ │  ← [PRO]
│ │ ═══════════●══════               │ │  ← Slider
│ │ Adjust device mic for accuracy  │ │
│ │                                  │ │
│ │ Frequency Weighting              │ │  ← [PRO]
│ │ [A-Weight] [C-Weight] [Z-Weight]│ │  ← Chips
│ └──────────────────────────────────┘ │
│                                     │
│ 🔔 NOISE NOTIFICATIONS             │
│                                     │
│ ┌──────────────────────────────────┐ │
│ │ Exposure Alerts           [ON]  │ │
│ │ Notify when > 85dB for 30min    │ │
│ │                                  │ │
│ │ Peak Warnings             [OFF] │ │
│ │ Alert for sudden > 120dB        │ │
│ │                                  │ │
│ │ Notification Threshold          │ │
│ │ ═══════●════════                 │ │
│ │ 60 dB    85 (SAFE)     110 dB  │ │
│ └──────────────────────────────────┘ │
│                                     │
│ 🎨 DISPLAY & APPEARANCE            │
│                                     │
│ ┌──────────────────────────────────┐ │
│ │ Dark Mode               [SYS] → │ │
│ │ Waveform Style                 → │ │
│ │ Refresh Rate                   → │ │
│ └──────────────────────────────────┘ │
│                                     │
│ ┌── dBcheck Pro ──────────────────┐ │
│ │ Unlock All Features             │ │
│ │ One-time purchase · €X.XX       │ │
│ │ [ UPGRADE TO PRO ]              │ │  ← Primary gradient button
│ └──────────────────────────────────┘ │
│                                     │
│ dBcheck v1.0.0 · Privacy · Terms   │  ← label-sm, centered
│                                     │
├─────────┴─────────┴─────────┴───────┤
```

**Notes:**
- Section headers: icon + `label-md` uppercase, `on-surface-variant` color.
- Settings groups in cards with `surface-container-high` background.
- Items within a group separated by spacing, not dividers.
- Pro upsell card uses a distinctive style — gradient border or subtle gradient background.
- "Microphone Sensitivity" and "Frequency Weighting" are Pro features with lock indicators for free users.

---

## 10. Pro vs Free Feature Matrix

| Feature | Free | Pro |
|---------|------|-----|
| Real-time dB meter (MIN/AVG/MAX) | ✓ | ✓ |
| Noise level labels (Whisper, Conversation...) | ✓ | ✓ |
| Play/Pause/Reset controls | ✓ | ✓ |
| Basic notifications (85dB alert) | ✓ | ✓ |
| Dark/Light theme | ✓ | ✓ |
| History — last 7 days | ✓ | ✓ |
| Weekly exposure chart (Analytics) | ✓ | ✓ |
| Hearing health status card | ✓ | ✓ |
| History — unlimited | | ✓ |
| CSV data export | | ✓ |
| Spectral analysis / frequency analyzer | | ✓ |
| Environment Mix breakdown | | ✓ |
| Session naming & tagging | | ✓ |
| Microphone calibration tools | | ✓ |
| Frequency weighting (A/C/Z) | | ✓ |
| Home screen widget | | ✓ |
| Hearing test (Phase 2) | | ✓ |

**Monetization:** One-time purchase, no subscription, no ads. Free version is genuinely useful — no dark patterns, no aggressive upselling.

---

## 11. State Coverage

### Empty States
- **History (no sessions):** Illustration + "No sessions yet. Start measuring to see your history here." + primary CTA to go to Meter.
- **Analytics (no data):** "Start your first measurement to unlock insights." + Meter CTA.

### Loading States
- Skeleton loaders matching card shapes and sizes
- The meter gauge animates a "connecting" state while microphone initializes
- Charts show shimmer placeholders

### Error States
- **Microphone denied:** Full-screen prompt explaining why mic access is needed + button to open system settings.
- **Measurement failed:** Inline error in meter screen with retry button.

### Overflow
- Session titles: single-line, truncate with ellipsis
- Long stat labels: abbreviate (e.g., "Moderate" not "Moderate Noise Level")

---

## 12. Interaction & Animation Notes

### Meter Gauge
- Arc animates smoothly as dB changes (ease-out, ~200ms)
- At rest, a subtle "breathing" pulse on the gradient arc (scale 1.0 → 1.02, 3s cycle)
- Peak detection: brief flash/glow effect when new MAX is set

### Screen Transitions
- Standard Material 3 container transform between tabs
- Cards use subtle fade-in + slide-up (200ms, ease-out) when scrolling into view

### Pull to Refresh
- Analytics and History support pull-to-refresh
- Custom animation: waveform ripple effect rather than standard Material spinner

### Haptics
- Light haptic on noise threshold crossing (85dB)
- Medium haptic on peak detection (new MAX)

---

## 13. Android-Specific Implementation Notes

### Architecture
- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** Latest stable

### Theme Implementation
- Custom `ColorScheme` for both themes (do NOT use `dynamicDarkColorScheme` — we have our own brand colors)
- Respect system dark mode setting as default, with manual override in Settings
- Custom `Typography` object mapping our type scale to Material 3 roles

### Audio Capture
- Use `AudioRecord` API or `MediaRecorder` for dB measurement
- Request `RECORD_AUDIO` permission with rationale dialog
- Background measurement via Foreground Service with persistent notification
- A-weighted dB(A) as default measurement unit

### Permissions
- `RECORD_AUDIO` — required for core functionality
- `POST_NOTIFICATIONS` — for exposure alerts (Android 13+)
- `FOREGROUND_SERVICE` — for background measurement sessions

### Data Storage
- Room database for session history and measurements
- DataStore for user preferences and settings
- No cloud sync in v1 (potential Pro feature later)

### Performance
- Meter refresh: 60fps for gauge animation, 10fps for dB value update
- Spectral analysis: FFT computed on background thread
- Charts: Use a Compose-compatible charting library (e.g., Vico)

---

## 14. Phase Plan

### Phase 1 — MVP
- Meter screen (full functionality)
- Basic History (7-day, auto-named sessions)
- Basic Analytics (weekly chart, health status)
- Settings (theme, basic notifications)
- Pro purchase flow (in-app billing)
- Pro features: unlimited history, calibration, frequency weighting

### Phase 2 — Enhancement
- Hearing test (full flow: setup → test → results)
- Spectral analysis / frequency analyzer
- Environment Mix
- Session naming & tagging
- CSV export
- Home screen widget

### Phase 3 — Polish
- Wear OS companion (optional)
- Cloud backup (optional Pro feature)
- Social sharing of hearing test results
- Advanced analytics (monthly trends, yearly report)

---

## 15. Claude Code Instructions

When implementing this app:

1. **This spec is the source of truth.** If HTML mockup files or PNG screenshots conflict with this document, follow this document.
2. **Use the HTML files for style extraction only** — pull exact hex values, font sizes, padding values, gradient angles from the CSS. Do not replicate their layout structure.
3. **Both themes must have identical layout and components.** Only color tokens change between dark and light.
4. **Typography is consistent across both themes.** Manrope for text, Space Grotesk for numbers. No exceptions.
5. **No 1px borders for sectioning.** Use tonal surface shifts.
6. **No standard Material elevation shadows.** Use tonal layering and ambient shadows as described.
7. **Minimum corner radius is 8dp** on any container element.
8. **Test both themes** for every screen and component.
9. **Pro features** should be functional but gated — show a preview with lock overlay and upgrade CTA, never hide them completely.
