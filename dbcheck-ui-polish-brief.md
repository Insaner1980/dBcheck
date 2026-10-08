# dBcheck UI Polish Brief — Visual Credibility Pass

**Scope:** Visual and interaction polish on top of the completed UI restructure. This is
**not** a restructure. Navigation, information architecture, screen ownership, ViewModels,
domain logic, Room, Pro entitlement, export, backup, Health Connect and notification paths
all stay exactly as they are.

**Source:** 20 device screenshots of the current build (Pixel 9, dark theme, Android 17
beta) reviewed against `PROJECT.md`, `UI-SPEC.md`, `dbcheck-ui-rakenteen-muutos.md` and the
current Compose code.

---

## Rules of engagement — read before planning

1. **The existing code is real and working.** Every change below is a deliberate design
   decision, not a bug report. Do not treat unlisted code as broken.
2. **Verify in code before you plan.** Some observations below are derived from screenshots.
   If a change is already implemented, or a simpler mechanism already exists, mark it
   **already done** and move on. Do not invent work.
3. **"Already correct as written" is a complete and valid outcome** for any individual item.
4. **Prefer the simplest change that achieves the stated goal.** Do not refactor theming,
   navigation, state management or component APIs unless a step strictly requires it.
   Moving a colour into a token must not become an architecture rewrite.
5. **Do not add features and do not remove functionality.** This pass only changes how
   existing things look and behave visually.
6. **Centralise design values.** Every colour, spacing, radius, duration and typography
   value introduced here goes into `ui/theme/`. Inline literals in screen or component code
   are a defect in this pass.
7. Produce your own file-level implementation plan after inspecting the code, using this
   document as the requirements spec. Cite exact files and functions in that plan.
8. Hex values below are a **starting point**. Verify WCAG AA contrast (4.5:1 for text,
   3:1 for UI components) for both themes and adjust within the same hue if a value fails.
   Report any value you had to change and why.

---

## Design intent

dBcheck must read as a **precision instrument for hearing health** — calm, editorial, and
unmistakably measuring something real. Two things currently work against that:

- The palette in `ui/theme/Color.kt` is fully achromatic (`#080808`–`#F7F7F7` plus three
  semantic colours that appear almost nowhere). With no accent, the app reads as an
  unfinished wireframe rather than a premium product.
- `signatureGradient` (`#F7F7F7 → #8F8F8F`) is applied to every primary button. In Android,
  a desaturated grey gradient is the established *disabled* signal. Every main call to
  action currently looks unavailable.

The target: one restrained sage accent carries interaction, and a separate four-step dB
level ramp carries measurement meaning. Everything else stays neutral.

---

# P0 — Critical

## P0.1 Introduce an accent and a dB level ramp

**File:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`

Add to `Color.kt`:

```kotlin
// Dark — accent (interaction)
val DarkAccent = Color(0xFF9CBFA3)
val DarkAccentDim = Color(0xFF6E8F76)
val DarkOnAccent = Color(0xFF08120C)
val DarkAccentContainer = Color(0xFF1B2A20)
val DarkOnAccentContainer = Color(0xFFC9E0CE)

// Dark — dB level ramp (measurement meaning)
val DarkLevelQuiet = Color(0xFF7E9C86)
val DarkLevelNormal = Color(0xFF9CBFA3)
val DarkLevelElevated = Color(0xFFD6A94F)
val DarkLevelDangerous = Color(0xFFE07A7A)

// Light — accent
val LightAccent = Color(0xFF2F5D43)
val LightAccentDim = Color(0xFF4C7A5E)
val LightOnAccent = Color(0xFFFFFFFF)
val LightAccentContainer = Color(0xFFDCEADF)
val LightOnAccentContainer = Color(0xFF17301F)

// Light — dB level ramp
val LightLevelQuiet = Color(0xFF607460)
val LightLevelNormal = Color(0xFF3F7350)
val LightLevelElevated = Color(0xFF9A7A33)
val LightLevelDangerous = Color(0xFFB45F5F)
```

In `Theme.kt`, extend `DbCheckColorScheme` with `accent`, `accentDim`, `onAccent`,
`accentContainer`, `onAccentContainer` and a `levelColor(level: NoiseLevel): Color` accessor
(or a `NoiseLevelColors` holder — your call, whichever is simpler given the existing
structure).

Map Material `primary` to the accent so that M3 components pick it up by default. Keep
`onPrimary` correct for the new fill.

**Where the accent is allowed to appear — and nowhere else:**

- Primary button fill
- Selected bottom-nav pill and its label
- Selected chip / segmented option fill
- Active slider track and thumb
- Focus and selection indicators
- The REC / LIVE indicator during an active measurement

**Where the level ramp is used:** anything that expresses a measured sound level (see P1.5).

Do not colour section headers, body copy, card backgrounds, icons in lists, or dividers.

**Existing `success` / `warning` / `error`:** keep them. `success` remains for non-level
confirmation states. The level ramp is a separate concern even where hex values overlap —
do not collapse them into one token set.

---

## P0.2 Remove the gradient from primary buttons

**File:** `ui/components/DbCheckButton.kt` (~line 82–90), `ui/theme/Theme.kt`

`DbCheckButtonStyle.Primary` currently fills with `colors.signatureGradient`. Replace with a
solid `colors.accent` fill and `colors.onAccent` content.

- Pressed state: `accentDim` (or a state-layer overlay — match whatever the other button
  styles already do).
- Disabled state must be **visually distinct from enabled**. This is the reason for the
  change; verify it side by side.

**`signatureGradient` is retained but its only remaining consumer is the meter gauge arc**
(see P2.1). Remove it from every other call site. Run `rg signatureGradient` and report
every hit you changed.

Affected surfaces to check visually afterwards: Start Measuring (Trends empty state),
Start Test (hearing setup), I Hear It (hearing test active), Save profile (tinnitus pitch),
Play (ambient sound), Take full baseline test (Hearing).

---

## P0.3 Fix content clipping at the meter scroll edge

**File:** `ui/meter/MeterScreen.kt` (`MeterContent`, ~line 325–363)

The layout is correct — a `weight(1f)` scrolling `Column` above a fixed `MeterControlsSection`.
The problem is that the scroll viewport clips content with a hard edge and no affordance, so
an expanded Live details chart or Sound Reference list appears sliced in half. On screen this
reads as a rendering fault.

Required:

1. Add a bottom **scroll edge fade** on the scrolling column — a background-to-transparent
   vertical gradient mask, roughly 24–32 dp, visible only when the list can scroll further.
   Add a matching top fade when scrolled away from the top.
2. Give `MeterControlsSection` a defined surface boundary: `surfaceContainerLowest` (or
   background) with a hairline top divider at `ghostBorder`, so it reads as a control bar
   rather than as floating buttons.
3. Verify at 360 × 800 dp that with **both** expanders collapsed the core readout fits with
   no scroll, and with either expanded the fade appears and nothing is ever ambiguously cut.

Do not switch to overlaying the controls or add bottom padding under a floating bar — the
current structure is right, only the edge treatment is missing.

---

# P1 — High

## P1.1 One header pattern

Three patterns exist today:

| Pattern | Screens |
|---|---|
| Logo mark only, no title | Meter, Trends, Hearing, History, Settings home |
| `←` + inline title, small | Settings subpages (e.g. Calibration) |
| `←` + uppercase eyebrow + large headline | Tinnitus pitch, Ambient sound, Hearing test setup |

Standardise on **two** patterns and use `ui/components/DbCheckTopAppBar.kt` for both:

- **Top-level destinations:** logo mark + screen title in the same style on every tab.
  Currently Meter and Trends show only the logo while Hearing and Settings show a large
  headline below it — unify these.
- **Pushed routes:** back arrow + title, one size, one alignment. The uppercase eyebrow +
  large headline treatment on setup-style routes may stay **only** if applied to every pushed
  full-screen route consistently; otherwise drop it.

Also: `ui/hearing/HearingScreen.kt` — the `🎧` emoji glyph inside the "Check Your Hearing"
heading is off-brand. Replace with the existing headphone `ImageVector` used elsewhere, or
remove it. (Session emojis in History stay — those are deliberate user metadata.)

---

## P1.2 One slider component

**File:** `ui/components/DbCheckSlider.kt` and its call sites

Three visually different sliders ship today:

- Tinnitus pitch (`ui/tinnitus/TinnitusPitchMatcherScreen.kt`): a small pill at the far
  left, a tall bar, then a thin track. Does not read as a slider at all.
- Ambient volume (`ui/ambient/AmbientSoundPlaybackScreen.kt`): dotted track segments.
- Mic sensitivity (`ui/settings/components/AudioCalibrationSection.kt`): solid filled bar,
  tall thumb, stray dot at the right end.

Required: every slider in the app uses `DbCheckSlider`, rendering as a conventional M3
slider — continuous track, circular thumb, accent-coloured active track. Add to the
component:

- Current value label
- **Min and max labels** at the track ends

The tinnitus pitch slider in particular gives the user no idea of its range; at 1.0 kHz the
thumb sits near the far left with nothing indicating the upper bound.

Investigate whether the odd renderings come from custom track/thumb lambdas or from
`interactionSource` / width constraints, and remove the divergence rather than patching each
screen.

---

## P1.3 Fix truncated chip labels

**File:** `ui/ambient/AmbientSoundPlaybackScreen.kt`, `ui/components/DbCheckChip.kt`

Ambient sound chips render as `W…`, `Pi…`, `B…`, `Fan`, and stop-timer chips as `No …`,
`15 …`, `30 …`, `60 …`, `12…`. Unusable.

Fix by allowing the chip rows to wrap (`FlowRow`) with full labels, or by making the row
horizontally scrollable at natural chip width. Do not shorten the strings to fit.

Audit every other chip group for the same failure at larger font scales — at minimum
`ui/settings/components/SettingsChipGroup.kt` and the frequency-weighting group.

---

## P1.4 Deduplicate the audio input list

**Files:** `service/AndroidAudioInputDeviceDescriptorMapping.kt`,
`domain/audio/AudioInputDeviceMapping.kt`,
`ui/settings/components/AudioCalibrationSection.kt`

The Calibration screen lists five indistinguishable options: `Pixel 9 / Audio input` ×3 and
`Pixel 9 / Built-in microphone` ×2. `AudioManager.getDevices()` returns several source types
that all carry the same `productName`, and `toAudioInputDeviceType()` collapses them into the
same display strings. The user cannot choose meaningfully.

Required:

1. Collapse entries that are functionally the same input into one row.
2. Where genuinely distinct inputs remain, disambiguate the subtitle with the underlying
   device type (e.g. "Built-in microphone", "Wired headset", "USB audio", "Bluetooth"), not a
   generic "Audio input".
3. **Do not change routing behaviour.** `AudioInputDeviceRouteResolver` and the built-in-mic
   fallback stay exactly as they are; this is a presentation fix. If dedup requires changing
   which `id` is stored in `selected_audio_input_device_id`, stop and flag it rather than
   silently altering persisted preferences.

Also: no radio option appears selected in the list even though a calibration profile is
active. Verify the selected-state binding and make the current input explicit.

---

## P1.5 Apply the dB level ramp

Wire `NoiseLevel` (`domain/noise/NoiseLevel.kt` — QUIET/NORMAL/ELEVATED/DANGEROUS) to the
new level colours. This is the single change that makes the meter feel like an instrument.

Apply to:

- `ui/meter/components/CircularGauge.kt` — the arc colour follows the current level
- `ui/meter/components/NoiseLevelPill.kt` — pill background follows the level (it already
  varies; confirm it now uses the ramp tokens)
- `ui/meter/components/MetricValueTile.kt` / `StatCard.kt` — MIN / LAEQ / MAX values tinted
  by their own level
- `ui/meter/components/LiveSoundLevelChart.kt` — line/fill gradient and the 85 dB threshold
  line

Colour transitions must be animated (`animateColorAsState` with a token from
`ui/theme/Motion.kt`), not snapped, so crossing a boundary reads as a smooth change rather
than a flicker.

Do not change `NoiseLevel` thresholds. Do not introduce a fifth level.

---

## P1.6 Rebuild the empty states

Every "no data" state currently uses the same grey card as real content, so screens with no
data read as a list of error messages.

**File:** `ui/components/EmptyState.kt` and its call sites.

- **Trends** (`ui/analytics/`): "No Data Yet" on a 75% empty screen. Give it one purposeful
  empty state that previews what Trends will show once data exists, with a single action.
- **Hearing** (`ui/hearing/HearingScreen.kt`): five stacked empty cards — hearing status,
  latest test, recovery check, tinnitus pitch, voice baseline. Collapse to **one** primary
  card ("Start with a baseline hearing test") plus the remaining items in a visibly
  secondary, lower-emphasis treatment. Do not remove any of the entry points.
- Section headers currently duplicate card titles: `HEARING RECOVERY` → card title
  `RECOVERY CHECK`, `TINNITUS PITCH` → card title `TINNITUS PITCH`. Remove one of the two.

Empty states must not use the same elevation and fill as populated cards.

---

## P1.7 Bottom navigation labels

**File:** `ui/components/BottomNavBar.kt` (~line 123–136)

The label renders only for the selected item. Five abstract icons (waveform, sparkle chart,
headphones, clock, gear) with no text is poor recognition and is not a pattern Android users
expect.

Show **all five labels, always**. Reduce icon size or bar padding if vertical space is tight.
Keep the existing selected-pill treatment and switch it to the accent colour.

Keep the current custom implementation and the ≥600 dp navigation rail behaviour — do not
migrate to `NavigationBar` or add an adaptive-navigation dependency for this.

---

## P1.8 Number formatting consistency

The UI is English but numbers render with a Finnish decimal comma: `+0,0 dB`, `1,0 kHz`.
This is correct locale behaviour but reads as a defect against English strings.

Decide once and apply everywhere: either format numeric readouts with an explicit locale
matching the string resources, or complete the Finnish localisation for the affected screens.
Given that `values-fi/strings.xml` is currently a 68-string partial baseline, **use an
explicit locale for numeric formatting** as the simpler fix. Centralise it — do not scatter
`String.format` calls across screens.

---

# P2 — Optional, only if P0 and P1 land cleanly

## P2.1 Meter gauge readout

**File:** `ui/meter/components/CircularGauge.kt`

- Idle arc is `#2A2A2A` on `#080808` — effectively invisible. Raise it to a visible track.
- Add scale labels (0 / 40 / 80 / 120) around the arc. Without them the arc communicates
  nothing.
- The unit `dB` renders at near-heading size stacked below the number and appears to use a
  different face than the numeral. Put the unit inline beside the value at roughly one third
  of its size, in the same family.
- Ensure numeric readouts use `SpaceGroteskFamily` with tabular figures
  (`FontFeatureSettings("tnum")`) so values do not jitter during measurement.
- `signatureGradient` may be used here for the arc — this is its one remaining home.

## P2.2 Dosimeter idle state

**File:** `ui/meter/components/DosimeterGaugeCard.kt`

Idle currently shows a paragraph ("Start measuring to calculate dose, TWA and remaining
exposure time.") and a `NIOSH REL` chip. Replace with the actual metric slots — Dose %, TWA,
Remaining exposure — rendered with `—` placeholders. A real dosimeter shows its fields
empty; it does not describe them in prose. Keep the standard chip.

## P2.3 Hearing test tone indicator

**File:** `ui/hearing/active/`

During an active test the circle is static, so nothing indicates a tone is playing. Add a
tone-presence indicator (a pulsing ring or level arc synchronised with `ToneGenerator`
playback). Purely visual — do not modify tone timing, `HearingTestPolicy`, or the
Hughson-Westlake procedure.

Also: the 1-of-12 progress bar renders as a small nub with a stray dot at the far right end.
Use a standard determinate progress indicator.

## P2.4 Camera overlay panel alignment

**File:** `ui/camera/CameraOverlayRoute.kt`

The dB readout card and the hint card are two panels with different corner radii whose edges
collide, and the capture buttons are different sizes. Merge into one bottom control bar with
a single radius, aligned edges, and equally sized capture buttons.

## P2.5 Meter idle copy

"Tap Play to start measuring." occupies prime space between the mode toggle and the gauge
permanently. Move it inside the gauge (replacing the `0` readout when idle) or into the
control bar.

---

# Out of scope

Do not change in this pass:

- Navigation graph, routes, top-level destinations, or `selectedTopLevelRouteFor(...)`
- Any ViewModel state shape, repository, DAO, migration, or domain policy
- `NoiseLevel` thresholds, dosimetry standards, calibration clamps, hearing-test procedure
- Pro entitlement gates, `ProLockOverlay` placement, or Free/Pro previews
- Audio routing, `AudioRecord` configuration, persistence cadence
- Export, PDF, PNG share, widget, notification layouts
- Adding any new dependency

---

# Acceptance criteria

Verify each on a 360 × 800 dp device in **both** dark and light themes, at default and
1.3× font scale:

1. No primary button is a grey gradient. Enabled and disabled primary buttons are
   unambiguously distinguishable.
2. The accent colour appears on: primary buttons, selected nav item, selected chips, active
   slider tracks, REC indicator — and on nothing else.
3. Meter with both expanders collapsed fits without scrolling. With either expanded, a scroll
   fade is visible and no content is ambiguously cut.
4. Gauge arc, status pill, MIN/LAEQ/MAX and the live chart all change colour together when
   the measured level crosses 40, 70 and 85 dB, with animated transitions.
5. Every screen uses one of the two approved header patterns.
6. Every slider renders identically and shows value plus min/max labels.
7. No chip label is truncated at default or 1.3× font scale.
8. The audio input list contains no two rows a user cannot tell apart, and the active input
   is visibly selected.
9. All five bottom-nav labels are visible at all times.
10. No screen mixes comma and period decimal separators.
11. Trends and Hearing empty states do not read as stacked error messages.
12. `rg` finds no new inline colour, spacing, radius or duration literals in `ui/` outside
    `ui/theme/`.

Run existing Compose screenshot tests and Detekt (including the Compose rules) before
reporting completion. Report any acceptance criterion you could not meet and why.
