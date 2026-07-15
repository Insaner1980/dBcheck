# dBcheck — Material 3 Refinement Plan

**Style direction:** Material Design 3 (Android), constrained to dBcheck's existing brand system: monochrome dark/light palettes, Manrope + Space Grotesk, tonal surface layering instead of borders and elevation shadows, minimum 8dp corner radius, signature gradient reserved for brand moments. This plan refines the existing implementation described in `UI-SPEC.md`; it does not redesign the product.

Source of truth for "current state" throughout this document: `UI-SPEC.md` (code-derived). Reference screenshots (Google Health, Pixel Weather, Pixel Tips) were used only for feel: calm rhythm, clear card grouping, one dominant element per screen, soft large surfaces.

---

## 1. Overall diagnosis

**The app is well-built but visually "flat in hierarchy": almost every element sits at the same emphasis level, on the same surface, with the same spacing.** Specifics:

- **Uniform surface = uniform importance.** Nearly every card is `surfaceContainerHigh` at 24dp radius with 20dp padding (`DbCheckCard` defaults, spec §6.2, §2.3). The Meter hero, a Settings toggle group, an analytics chart, and a session row all read as the same object. Nothing recedes, so nothing advances. This is the single biggest reason the app feels like a tool panel rather than a modern Android app.
- **Uniform spacing = no rhythm.** Screens use one vertical gap — `space4` (16dp) — between everything (Meter §7.3, Analytics §8.1, History §9.1, Session Detail §10.1, Settings §11.1). Related items (chart + its stats) and unrelated items (chart vs. next feature card) are separated identically. Modern Material screens breathe by varying gaps: tight inside a group, wide between groups. dBcheck currently has density without grouping.
- **Shape zoo.** 24dp cards, 16dp StatCard, 14dp MetricValueTile and SoundReference rows, 12dp text fields, 10dp histogram background, 8dp stat pills/bucket chips/recent rows, plus canvas-level corners of 4f/5f/8f/12f/28f. Ten radii where four would do. This is subtle but reads as "assembled", not "designed".
- **Label wallpaper.** Uppercase `labelMd` Space Grotesk titles head almost every card, and `labelSm`/`labelMd` appear inside cards for metrics, badges, pills, and footers. Combined with `dataLg`/`dataXl` numbers everywhere, several screens (Session Detail, Analytics) approach a wall of small caps + numerals with few resting points of plain Manrope body text.
- **Charts are raw.** Grid alphas vary per chart (0.32, 0.24, 0.35, ghostBorder), some charts paint their own background fill (`surfaceContainerHighest.copy(alpha=0.46f)`) inside a card that already has a background, corner radii on canvas differ from the design system, and stroke widths differ (2/3/4). Each chart looks slightly like a different library.
- **Meter screen is a stack, not a composition.** Gauge → chart → sound reference → waveform → stats arrive in equal slices (§7.3). The 288dp gauge is large but is not framed as the hero; the waveform is a second live visualization competing with the live chart directly above it.
- **Settings is one long undifferentiated scroll.** Section titles are 12sp uppercase labels with 12dp below and 16dp above — visually weaker than the cards they title. Direct `8.dp`/`12.dp` literals mix with tokens (§4.2, §11.4).
- **Where Material 3 is already strong:** tonal layering instead of borders/shadows is the correct M3 posture and already enforced; edge-to-edge + inset handling is correct; navigation rail at 600dp is correct; chips and toggles use proper M3 color roles and semantics; ProLockOverlay's blur-preview approach is genuinely good (preview, don't hide); the ghost border concept is a tasteful low-contrast affordance; accessibility semantics coverage (§21) is above average.
- **Where M3 is present but not fully expressed:** surface container ladder exists but only two rungs are really used (High everywhere, Highest for inner bits); the shape scale exists in theme but components bypass it with literals; state layers exist only as pressed alphas on buttons; motion is minimal and inconsistent (one 200ms gauge tween, one 1200ms shimmer, defaults elsewhere).

**Verdict:** the data density is fine — the problem is that the density is unranked. The fix is hierarchy (surface roles + spacing rhythm + one hero per screen), not removal of information.

---

## 2. Target Material 3 direction for dBcheck

After refinement, dBcheck should feel like **a serious measurement instrument presented with the calm of a Pixel-era health app**:

- **One hero per screen.** Meter: the gauge and the live dB number, full stop. Analytics: the exposure summary. History: the recent-sessions list. Session Detail: the session summary + KPI grid. Settings: the section structure itself. Everything else visibly supports the hero by sitting on a quieter surface with smaller type.
- **Instrument in the middle, product around it.** The circular gauge stays a precise instrument (ticks, arc, thresholds), but the surfaces around it become softer, larger-radius, lower-contrast supporting cards. The credibility comes from the gauge and the numbers; the calm comes from everything else.
- **Grouped, not listed.** Screens read as 3–5 titled groups with wide gaps between groups and tight gaps inside them — like Google Health's "Key metrics" grid — instead of N equal cards at 16dp intervals.
- **Monochrome by default, color as signal.** The near-monochrome palette is a strength: when success/warning/error appear, they mean something acoustic (quiet/elevated/dangerous). Refinement should reduce incidental color (dim gradients, tinted chips) so threshold colors carry more weight.
- **Quiet, professional, trustworthy.** No playfulness, no decoration. Pro upsells feel like an invitation into a lab's premium tier, not a paywall slammed over content.
- **Identical hierarchy in both themes.** The dark theme's surface ladder (#080808 → #101010 → #171717 → #202020 → #2A2A2A) and the light theme's (#FAFAFA → #FFFFFF → #F0F0EF → #E6E6E3 → #DADAD7) must express the same ranking decisions; a card that is "quiet" in dark mode must be equally quiet in light mode.

---

## 3. Design principles for dBcheck

Practical rules a developer can apply during implementation:

1. **Live meter:** the current dB value is the only `displayLg` in the app. Nothing on the Meter screen may be visually louder than the gauge. Supporting cards on Meter use `surfaceContainer` (not High) so the gauge area pops against them.
2. **Measurement credibility:** never round away precision that exists today; never animate a number in a way that misrepresents it (gauge sweep may ease, the numeric readout snaps). Threshold colors (success/warning/error) map 1:1 to `NoiseLevel` everywhere — no decorative use of those roles.
3. **Data hierarchy:** each card gets at most one `dataXl`. Secondary metrics inside the same card use `dataMd` + `labelSm`. If two numbers are equally important, neither gets `dataXl`.
4. **Card grouping:** cards that belong to one concept share a group: 12dp between them, one section header above, 32dp before the next group. Never separate two cards in the same group with the same gap used between groups.
5. **Rounded surfaces:** four radii only — 24dp (cards, overlays), 16dp (inner tiles, nav indicator, buttons that aren't pills), 12dp (inner rows, fields, chart-internal shapes), and CircleShape (chips, pill buttons, round controls). Modal sheet top 28dp stays as the single exception.
6. **Gradients:** signature gradient appears in exactly three places — the primary button, the central record button, and the gauge's NORMAL arc. Plus the existing Pro upsell 1dp gradient border (the sanctioned border exception). Nowhere else; bar-chart fills switch to solid primary with dim variants.
7. **Monochrome surfaces:** hierarchy comes from the surface container ladder, never from borders (ghostBorder is for hairline affordances on fields/dividers-of-last-resort, not for sectioning) and never from shadows (AmbientShadow only under the floating record control if used at all).
8. **Success/warning/error restraint:** these colors appear only as (a) NoiseLevel semantics, (b) inline validation/status text, (c) destructive-action confirmation accents. Never as card backgrounds; at most 14% alpha tints behind a small chip or dot.
9. **Pro locked states:** always preview real-looking content under a consistent scrim; one lock glyph, one sentence, one button. The locked card is the same size and same surface as its unlocked twin — locking changes content visibility, never layout.
10. **Setup flows:** one column, one `headlineLg`, instruction cards on `surfaceContainer`, a single full-width primary CTA anchored after `space16` of air. Progression is shown with a labeled step indicator, not with extra chrome.
11. **Reports, history, analytics:** charts are frameless inside their card (no inner background fill), share one grid style, and every chart card carries a plain-language `bodyMd` takeaway line where one exists — numbers plus one sentence of meaning.
12. **Theme parity:** every surface-role decision is made once, in role terms (`surfaceContainer` etc.), never per-theme hex. Any alpha overlay (locked scrim, glass circle) must be verified on both #080808 and #FAFAFA before merging.

---

## 4. Token-level recommendations

Do-not-touch: font families, type scale, palette hexes, navigation model.

| Current token / pattern | Proposed adjustment | Reason | Expected visual effect | Implementation note |
|---|---|---|---|---|
| Page horizontal padding 20dp everywhere | Keep 20dp, but make it a named token (`pageMargin`) | It's a good width on phones; the problem is it's a literal repeated ~15 times | None visually; consistency insurance | Add `DbCheckSpacing.pageMargin = 20.dp`, replace literals |
| Vertical gap between cards: `space4` (16dp) universally | Within a group: `space3` (12dp). Between groups/sections: `space8` (32dp) | Single-gap layouts have no rhythm | Screens read as titled clusters; perceived calm without losing density | Change `Arrangement.spacedBy` per screen; introduce `groupGap`/`sectionGap` aliases |
| Vertical gap between major sections: also `space4` | `space8` (32dp), section header included in the section (header + `space3` + cards) | Section boundaries currently invisible | Clear chapters on Analytics/Settings/History | Wrap each section in its own Column |
| Card internal padding 20dp | Keep 20dp default; dense inner tiles 16dp; hero cards 24dp | Padding should scale with card rank | Hero feels generous, tiles feel efficient | `DbCheckCard(padding=…)` already parameterized |
| Setup screen spacing (scaffold 20dp h-padding, ad-hoc verticals) | Standardize: header block, `space8`, content cards `space3` apart, `space16` before CTA | Hearing setup already close to this; sleep/tinnitus/ambient drift | All setup flows feel like one family | Encode in `DbCheckSetupScaffold` slots |
| Meter screen spacing (space8/space6/space4 mix, §7.3) | See §9: gauge zone gets `space8` above and below; support cards `space3` apart | Give the hero air; tighten the support stack | Gauge reads as hero | Meter column arrangement change only |
| Analytics/History screen spacing `space4` list | Group per §10/§11; `space3` in-group, `space8` between groups | Same rhythm rule | Scannable modules | LazyColumn: use `item {}` groups with spacers |
| Settings section spacing (title labelMd + 12dp + card, 16dp between sections) | Title gets `space8` above (except first), `space3` below; keep cards inside section `space3` apart | Section titles must own more space than cards | Long page becomes chaptered | Also fixes the "endless scroll" feel with zero content change |
| Card corner radius 24dp | Keep 24dp as the only card radius | Already correct, just enforce | — | Point `MaterialTheme.shapes.extraLarge` and DbCheckCard at one token |
| Smaller tile corner radius 14dp/16dp mixed | 16dp for all inner tiles (StatCard, MetricValueTile, KPI inner tiles) | Kill the 14dp oddball | Tiles look intentional | Two-line changes in StatCard/MetricValueTile |
| Chip shape & density (CircleShape; h-padding 8/10/16dp variants) | Keep CircleShape; two densities only: default 16dp h-padding, compact 10dp; min height stays 48dp (touch), visual pill height 32dp within it | Three paddings = accidental | Chip rows align across screens | Add `ChipDensity` enum; schedule-day chips use compact |
| Button height 56dp (some 44/48dp) | Two sizes: 56dp primary/CTA, 48dp secondary/inline. Remove the 44dp (`DbCheckLockedCtaCard`) | 44dp is below M3 comfortable and unique in the app | Buttons stop jittering between cards | One-line fix in locked CTA card |
| Icon circle sizes (48dp checklist, 48dp emoji, 64dp empty state) | Keep 48dp as standard icon circle; 64dp reserved for full-screen states | Already nearly consistent | — | Name them: `iconCircle=48dp`, `stateIcon=64dp` |
| Bottom nav selected indicator (16dp-radius box, primary α0.12, h-padding 16/v 4) | M3-style pill: height 32dp, CircleShape, width ~64dp, `primary.copy(alpha=0.12f)` stays; icon centered | Current box shape is almost-M3; full pill is the recognizable M3 signature | Instantly reads as modern Material | Pure NavItem change; keep label-only-on-selected behavior |
| Navigation rail spacing (centered cluster) | Keep; add 12dp vertical gap between items and the same 32dp-pill indicator | Parity with bottom bar | Consistent tablet feel | Shared indicator composable |
| Divider usage (HorizontalDivider outlineVariant in settings lists) | Replace full-strength outlineVariant with `ghostBorder`; prefer 12dp spacing over dividers when rows ≤3 | outlineVariant #8C8C8C at 1dp is the harshest line in the app | Softer lists | Search-and-replace divider color |
| Ghost border usage (field borders, TimeSeries grid, HR baseline) | Sanctioned uses: unfocused field outline, chart grid, last-resort list divider. Never around cards | Keeps the no-border sectioning rule intact | — | Document in theme file comment |
| Surface container layering (cards High, tiles Highest) | Rebalance: default card `surfaceContainer`; hero/interactive cards `surfaceContainerHigh`; inner tiles `surfaceContainerHigh`; `surfaceContainerHighest` only for selected/pressed/emphasis | Creates a third visible rung; heroes gain contrast headroom | The single highest-impact change in this plan | Change `DbCheckCard` default + explicit High on hero cards; verify both themes |
| Gradient usage (buttons, bars, sweep) | Restrict per principle 6; weekly bars → solid primary (today) + `primaryDim` (other days) | Gradient scarcity = brand strength | Charts calmer, buttons more special | WeeklyBarChart paint change |
| Shadow usage (AmbientShadow, rare) | Keep the tonal-only rule; optionally AmbientShadow under the 80dp record button only | Matches CLAUDE.md rule 3 | — | No-op or one modifier |
| Empty state padding (48dp, icon 64dp) | Keep for full-screen; add compact in-card variant: 24dp padding, 32dp icon, bodyMd only | Full-size EmptyState inside cards is oversized | In-card empties stop dominating | `EmptyState(size = Compact)` |
| Locked overlay padding & opacity (24dp, surface α0.6; history variant α0.62) | Standardize: scrim `surface.copy(alpha = 0.68f)`, padding 24dp, radius inherits host card | Two alphas today; 0.6 is slightly too transparent over busy charts in light theme | Locked states look like one system | Constant in ProLockOverlay; delete local variant |
| Skeleton shape & height (24dp radius; heights 200/120/180/80 ad hoc) | Skeletons mirror the real component: expose `SkeletonSpec` per card type (e.g., chartCard=200dp, listRow=80dp, tile=112dp) | Ad-hoc heights cause layout jump on load | Loading→loaded transition stops shifting | Small enum + reuse |
| Motion timing (200ms gauge, 1200ms shimmer, defaults elsewhere) | Standard durations: 150ms state changes, 250ms `EaseOutCubic` content/expansion, 400ms screen-level; keep 3000ms breathing; respect reduced-motion everywhere (NoiseLevelPill already does) | Perceived polish is mostly motion consistency | Transitions feel deliberate | Central `DbCheckMotion` object with tokens |

---

## 5. Color and surface usage

No new palette. Better deployment of the existing one.

**Surface role map (both themes):**

- `background` — page canvas, setup scaffolds, hearing active/results full screens, camera fallback container. Nothing else.
- `surface` — app chrome only: top app bar area (implicit), bottom navigation, navigation rail, modal bottom sheet container, locked-overlay scrim base, gauge glass circle base.
- `surfaceContainer` — **new default card background** (Settings group cards, analytics feature cards, instruction cards, notes cards, report action cards). Today this rung is nearly unused (only SessionCard emoji circle).
- `surfaceContainerHigh` — promoted surfaces: Meter support cards during recording, `SessionCard` (it's the tappable hero of History), `ExposureSummaryCard`, `Last24HoursChart` card, KPI cards, inner tiles that sit on `surfaceContainer` cards, unselected chips.
- `surfaceContainerHighest` — selected/active/emphasis only: pressed states, selected list rows, slider inactive tracks, secondary button fill, spectrogram/RTA inner fills (see §16 for reduction), skeleton shimmer peak.
- `surfaceContainerLowest` — toggle checked thumb (current use is good); dark-theme "cutout" moments like the burned-in overlay panel equivalent. Avoid as a card background.

**Default card background:** move `DbCheckCard` default from `surfaceContainerHigh` to `surfaceContainer`, then explicitly re-promote the ~6 hero/interactive cards listed above. This is deliberate: demote by default, promote by exception. In dark theme that's #202020 → #171717 for most cards — quieter against #080808 while still clearly layered; in light theme #E6E6E3 → #F0F0EF — noticeably airier.

**Hero surfaces vs. regular cards:** heroes differ by (a) `surfaceContainerHigh`, (b) 24dp internal padding instead of 20dp, (c) owning the only `dataXl`/`displayLg` in view. Heroes never differ by border, shadow, or gradient (Pro upsell border stays the lone exception).

**Accent discipline:**

- `primary` (near-white/near-black): live data lines, current markers, selected states, primary text emphasis. Because primary is monochrome, it can be used more liberally than in colorful M3 apps — it *is* the neutral emphasis channel.
- `primaryDim`: de-emphasized data (non-today bars, past series), never for text below 4.5:1 contrast.
- `secondary`: second data series (right ear in audiogram, gradient tail). Not for text on small sizes in dark theme (#8F8F8F on #171717 is borderline; keep `onSurfaceVariant` for captions).
- `tertiary`: waveform and truly ambient visuals only — it's the "barely there" data color. Correctly used today.
- `success`/`warning`/`error`: NoiseLevel semantics + status text + destructive confirmation, per principle 8. Concretely: keep `NoiseLevelPill`, gauge arcs, histogram bucket colors, peak-value warnings; **remove** warning color from the WeeklyTrendCard percentage (an upward trend is information, not danger — use `onSurface` with an up/down glyph) and keep HR overlay's warning-colored line (it's a distinct series color there; acceptable as the single exception, or switch to `secondary` for purity).
- `ghostBorder`: unfocused field outlines, chart grids, last-resort dividers. Never card outlines.

**Signature gradient:** primary actions only (primary button, record button) + gauge NORMAL arc + Pro upsell border. Remove from weekly chart bars. Hero visuals should be monochrome-with-threshold-color, not gradient-branded — the gradient is a *control* signature, meaning "this acts".

**Dark/light parity:** the promotion/demotion above holds in both themes by construction (role-based). Two watch-points: (1) light theme's `surfaceContainerHighest` #DADAD7 on `surfaceContainer` #F0F0EF is a small step — verify selected rows still read as selected (add the existing `primaryContainer` tint for selection instead of relying on Highest alone); (2) locked scrim `surface.copy(0.68f)` over light content needs the blur to stay on (S+); on the alpha-only fallback, raise content alpha dim to 0.35f in light theme for equivalent legibility.

---

## 6. Typography usage consistency

System unchanged (Manrope + Space Grotesk, existing scale). Deployment rules:

- **`displayLg` (56sp):** the live dB value in `CircularGauge`. Only there. The camera overlay readout keeps `displayMd`.
- **`displayMd` (44sp):** camera overlay dB readout and the hearing-test frequency circle. Only those two full-screen instrument moments.
- **`dataXl` (32sp):** one per card maximum — exposure average, Last24Hours max, KPI value, dosimeter dose, SafeHours/WeeklyTrend value. If a card currently shows two `dataXl`s, demote the secondary to `dataLg`.
- **`dataLg` (24sp):** paired stats (PEAK/AVG on SessionCard, StatCard values, recovery metrics). The workhorse number.
- **`dataMd` (16sp):** table-style values (KeyMetrics rows, sleep metrics, peak-event values, slider readouts). Never as a lone hero number.
- **`headlineLg` (32sp):** page titles only (Analytics/History/Settings headers, setup flow titles, hearing results rating). One per screen.
- **`headlineMd` (24sp):** dialog titles, sheet titles, full-screen state titles, Pro upsell title, current-sound detection value. Not for card titles.
- **Uppercase policy:** `labelMd` uppercase = card/section titles (the established convention — keep it universal: every card title is uppercase `labelMd` in `onSurfaceVariant`, no exceptions like HearingHealthCard's `bodyLg` title, which should become a body line under a labelMd title or stay as the card's takeaway text). `labelSm` uppercase = metric captions under/over numbers. Buttons: only Tertiary stays uppercase (current behavior); Primary/Secondary remain `bodyLg` sentence case.
- **Card titles:** one style everywhere — uppercase `labelMd`, `onSurfaceVariant`, letter-spacing as defined. This is dBcheck's "eyebrow" and its consistency is what lets numbers below vary safely.
- **Scannability without shouting:** the pattern per card is eyebrow (`labelMd`) → primary figure (`dataXl` or `dataLg`) → one `bodyMd` sentence of meaning. That body sentence is the antidote to the wall-of-numbers feel; add it wherever a plain-language summary exists (exposure card, hearing health, sleep insights already have them — extend to histogram, trend, environment mix).
- **Keeping dB dominant:** because `displayLg` is unique to the gauge and `dataXl` is capped at one per card, the live reading always wins the size contest. Secondary metrics stay in `dataMd`/`dataLg` + muted captions, and prose stays Manrope — three distinct text textures (caps eyebrow, Grotesk number, Manrope sentence) instead of an undifferentiated numeric wall.

---

## 7. Card and surface system

Radii: card = 24dp, tile = 16dp, row = 12dp. "Eyebrow" = uppercase `labelMd` `onSurfaceVariant` title. No borders or shadows anywhere below except the two sanctioned exceptions (Pro upsell gradient border; optional AmbientShadow under the record button).

| Role | Surface | Padding | Radius | Border/Shadow | Icon treatment | Dividers | Internal spacing | Content |
|---|---|---|---|---|---|---|---|---|
| Page background | `background` | 20dp page margin | — | none | — | never | groups per §4 | screens |
| Main meter hero surface | none — gauge sits on `background` | `space8` above/below | — | none | — | — | 6dp pill gap | gauge, dB value, NoiseLevelPill |
| Live measurement support card (chart+waveform, session bar, dosimeter) | `surfaceContainerHigh` | 20dp | 24dp | none | none | none | 12dp | live chart, waveform strip, REC info |
| Default content card | `surfaceContainer` | 20dp | 24dp | none | 24dp icon, `primary` tint when leading | avoid; ghostBorder if >3 rows | 12dp | feature content, notes, CTAs |
| Analytics chart card | `surfaceContainer` (hero exposure card: High) | 20dp | 24dp | none | none | none | eyebrow, 16dp, chart, 12dp, takeaway | one chart + one takeaway line |
| History session card | `surfaceContainerHigh` | 20dp | 24dp | none | 48dp emoji circle on `surfaceContainer` | none | 16dp row gap | title, metadata, PEAK/AVG |
| Detail KPI card | `surfaceContainerHigh` | 20dp | 24dp | none | none | none | 8dp | eyebrow + one `dataXl` |
| Settings group card | `surfaceContainer` | 20dp | 24dp | none | leading 24dp `primary` icons optional | ghostBorder between radio rows only | 16dp (`space4`) | rows, toggles, chip groups |
| Setup instruction card | `surfaceContainer` | 20dp | 24dp | none | 48dp icon circle `surfaceContainerHigh`, icon `primary` 24dp | none | 12dp | checklist / options / notes |
| Pro locked card | same as unlocked twin | twin's padding | twin's radius | none | 32dp lock in 48dp tonal circle | none | overlay: icon, 8dp, text, 16dp, button | blurred preview + scrim + one CTA |
| Pro upsell card | `surfaceContainer` | 20dp | 24dp | **1dp signatureGradient border** (the exception) | none | none | 8/16dp per spec | title, subtitle, purchase button |
| Empty state card (in-card variant) | inherits host | 24dp | inherits | none | 32dp icon α0.5 | none | 8dp | icon, bodyMd, optional tertiary action |
| Error/warning card | `surfaceContainer` + 24dp leading icon in `error`/`warning`; **not** a tinted background | 20dp | 24dp | none | status icon colored, text `onSurface` | none | 12dp | message + recovery action |
| Camera overlay panel | `surface.copy(alpha≈0.72f)` (match close button) | 16dp | 16dp | none | white iconography | none | 4–8dp | readout block, capture errors |
| Widget surface | `GlanceTheme.widgetBackground` | 16dp | system | none | none | none | 2–4dp | brand label, dB, level, age |
| Notification surface | custom #0E0E0E / #201F1F per spec | 12dp | system | none | 12dp status dot | none | 6dp | value, peak/duration, level panel |
| Share image card | dark #0E0E0E (hearing) / light #F9F9F9 (session) — unify per §15 | 80px margins | 28f inner cards | none | none | none | fixed layout | score/metrics + brand line |
| PDF report card style | white page, card `rgb(236,238,238)` | 18f | 14f | 1f hairline borders acceptable in print | none | rules ok in print | 42f table rhythm | report-grade layout (see §15) |

Composition rules: a tile (16dp) may sit on a card (24dp); a row (12dp) may sit on a tile or card; never nest 24dp inside 24dp. Surface must step up exactly one rung when nesting (`surfaceContainer` card → `surfaceContainerHigh` tile → `surfaceContainerHighest` selected state).

---

## 8. Shared component review

**`DbCheckButton`**
- Works: three clear tiers, gradient primary is a strong brand asset, 48dp touch minimum, pressed-alpha states.
- Feels dated/inconsistent: heights drift across call sites (44/48/56); pressed alpha is the only state feedback (no state layer on secondary/tertiary hover-equivalent).
- Change: lock sizes to 56dp (CTA) / 48dp (inline); keep CircleShape; add a subtle press state layer to Secondary (`onSurface.copy(0.08f)` overlay) instead of whole-button alpha, which dims the label; Tertiary keeps text-button behavior.
- Direction: calmer, more consistent. Accessibility: disabled state must not rely on alpha alone — pair with `onSurfaceVariant` content color (current disabled share button at α0.4 is borderline; use 0.38 content-color convention).

**`DbCheckCard`**
- Works: single shape, single padding, no borders/shadows — the system's backbone.
- Inconsistent: default `surfaceContainerHigh` flattens hierarchy (see §5).
- Change: default → `surfaceContainer`; add a `CardEmphasis(Default, Elevated)` parameter mapping to Container/ContainerHigh so promotion is explicit and greppable.

**`DbCheckChip`**
- Works: proper selected semantics, 48dp targets, primaryContainer selection.
- Dated: three ad-hoc horizontal paddings; equal-width chip rows (Analytics sections, presets) stretch pills into odd proportions on wide screens.
- Change: two densities (16dp / 10dp); stop `weight(1f)`-stretching chips — use a scrollable row or FlowRow with natural widths; selected chip may add a 18dp leading check icon in `onPrimaryContainer` (M3 filter-chip signature) where space allows; locked chips keep the 14dp lock icon.
- Direction: more compact and native. Accessibility: monochrome selection (light-grey pill on grey) needs the check glyph in dark theme — color difference alone is weak.

**`DbCheckSlider`**
- Works: correct role colors, labeled variant.
- Change: adopt M3 expressive track (thicker 8dp inactive track with stop indicator) only if Material library version allows; otherwise keep, but always pair with a live `dataMd` value readout (calibration already does; threshold/hour sliders should match). Add tick marks only for stepped sliders (hours, volume steps).

**`DbCheckToggle`**
- Works: fully on-role M3 switch. Keep as-is. Only rule: toggle rows must be 48dp min height with the whole row clickable, not just the switch.

**Top app bar**
- Works: light, custom, brand mark + name.
- Dated: it's identical on all four tabs, so screens rely on a second in-content header (`labelMd` + `headlineLg`), duplicating identity.
- Change: keep the slim brand bar on Meter; on Analytics/History/Settings let the in-content `headlineLg` header be *the* title (it already is) and drop the redundant brand row, or keep the bar but make it fade/scrim on scroll (surface at scroll offset >0). Minimal, behavior-free polish: add scroll-edge tonal change (background → surface) so content doesn't collide with the status bar area.

**Bottom navigation**
- Works: tabs, roles, insets, selected tint.
- Dated: 16dp-radius rectangle indicator; label appears only under the selected item, which shifts icon vertical position.
- Change: 32dp-tall CircleShape pill indicator behind the icon; reserve label space in all items (invisible label placeholder) so icons don't jump; keep label-on-selected. Row height 64dp keeps. This is the highest-visibility "recognizably M3" fix.

**Navigation rail**
- Works: correct breakpoint and centering.
- Change: share the pill indicator; item vertical gap 12dp; align top with content header baseline rather than pure centering if trivial — otherwise keep centering.

**`ProLockOverlay`**
- Works: blur-preview philosophy; fallback alpha for pre-S.
- Inconsistent: scrim alphas 0.6 vs 0.62; padded 24dp always even in small cards; fixed-height locked twins (352dp/360dp/164dp) drift from real content.
- Change: single scrim `surface.copy(0.68f)`; overlay radius inherits host card; locked previews get height from the same composable as unlocked content (render preview data through the real card); center a 48dp tonal circle with 24dp lock icon instead of a bare 32dp icon — reads premium, not punitive.

**`ProUpgradePrompt`**
- Works: compact icon/title/button stack.
- Change: title `bodyMd` → keep, but max 2 lines and one fixed phrasing pattern ("Preview — unlock with Pro"); button 48dp always; never show price inside overlays (keep price on the Settings Pro card only).

**`DbCheckLockedCtaCard`**
- Change: button 44dp → 48dp; otherwise conforms after card-default change.

**`EmptyState`**
- Works: generous, centered, correct muted icon.
- Change: add Compact variant (§4); full variant reserved for whole-screen empties (History empty, Analytics empty). Description max width ~280dp for line-length comfort.

**`SkeletonLoader`**
- Works: shimmer implementation is fine.
- Change: per-card `SkeletonSpec` heights to avoid load jump; shimmer respects reduced-motion (fall back to static `surfaceContainerHigh` fill).

**`DbCheckSetupScaffold`**
- Works: consistent back button, scroll, 20dp margins.
- Change: add optional slots — step/phase indicator row, pinned bottom CTA area (so hearing/sleep/tinnitus/ambient stop hand-rolling CTA spacing), and standard header block (phase label, `space2`, title, `space3`, description). All four flows then inherit identical rhythm.

---

## 9. Meter screen redesign guidance

**Primary focal point:** the gauge with the live value — explicitly zoned. Structure the screen as three zones:

- **Zone A — Instrument (hero):** mode chips, gauge, NoiseLevelPill. Sits directly on `background`. `space6` after chips, gauge, `space8` below. No card. The 288dp `CircularGauge` stays a large circular instrument — do not soften it into a blob; its precision (ticks, 270° arc, threshold colors) is the credibility anchor. Two refinements: glass-circle alpha 0.6 → 0.55 in light theme (value contrast), and tick color from onSurfaceVariant-equivalent to `ghostBorder`-strength so ticks frame rather than compete.
- **Zone B — Live context (one card):** merge `LiveSoundLevelChart` + `WaveformVisualization` into a single `surfaceContainerHigh` card ("LIVE" eyebrow, chart 116dp, 12dp, waveform 32dp). The waveform becomes visually quieter: keep `tertiary` α0.2, reduce height 48dp → 32dp, and place it as a texture strip under the chart rather than a standalone module. The chart gains a frame by being in the card — no inner canvas background. Dosimeter mode swaps this card's content (see below).
- **Zone C — Reference & stats:** `SoundReferenceCard` (collapsed by default, on `surfaceContainer`), 12dp, stats row, 12dp, optional sleep CTA. Stats stay three tiles.

**Spacing exact:** top `space6` (was space8 — chips move closer to the app bar), chips, `space6`, [session bar + `space3` when recording], gauge, `space8`, Zone B card, `space3`, SoundReferenceCard, `space3`, stats row, `space4`, sleep CTA, bottom controls area.

**How much before scroll:** on a typical 640dp+ viewport: Zone A + Zone B + stats must fit; SoundReferenceCard may fall below the fold (it's reference, not live data). Below 640dp keep the existing scroll strategy but let Zone A compress first (gauge 288 → 240dp) before pushing content off-screen.

**Stats row:** keep three tiles but downshift surface to `surfaceContainerHigh`-on-background with 16dp radius (already 16dp) and `dataLg` values (already). Smaller tiles, not larger — MIN/AVG/PEAK are supporting actors.

**Recording state:** recording should be visible without reading text: (1) `MeterSessionInfoBar` slides in above the gauge (existing position fine) with the red dot + duration; (2) Zone B card's eyebrow switches to "LIVE • REC" in `error`; (3) the central 80dp button switches from gradient to `error` fill with stop icon; (4) optional 1dp progress ring around the record button. Everything else stays put — layout stability signals instrument stability.

**Central controls:** keep 48/80/48 layout and gap `space6`. Camera and share side buttons stay `surfaceContainerHighest` circles; disabled share uses `onSurfaceVariant` icon color instead of alpha 0.4.

**Mode chips:** keep DB Meter / Dosimeter as chips, compact density, natural width, centered. Locked Dosimeter chip: lock glyph + normal label color at α1 (dimming locked chips makes free users feel punished; the glyph is enough).

**Dosimeter fit:** dosimeter is a *mode of Zone B*, not a second app: `DosimeterGaugeCard` replaces the Live card content (same card frame, "DOSIMETER" eyebrow, gauge 140dp + TWA/LAeq tiles as today). The main circular gauge stays live dB in both modes — that keeps one instrument identity. Standard badge stays; radius of badge pill CircleShape.

**Permission denied prompt:** conforms to full-screen state pattern (§17): 48dp padding, 64dp icon, headline, body, primary + secondary 48dp buttons, `space3` gaps — this matches current implementation; only align icon alpha (0.5) and add the settings deep-link behavior unchanged.

---

## 10. Analytics screen guidance

**Grouping.** Replace the flat card list with titled groups (in-group 12dp, between groups 32dp):

1. **Exposure** (hero): ExposureSummaryCard (Weekly) or MonthlyTrendChart (Monthly) — `surfaceContainerHigh`.
2. **Hearing**: HearingHealthCard, HearingRecoveryCard, HearingTestCta.
3. **Reports**: YearlyReportCard.
4. **Tools**: TinnitusPitchCard, AmbientSoundCard, optional Sleep setup CTA.
5. Spectral section: SpectralAnalysisCard alone. Environment section: SoundDetectionCard, EnvironmentMixCard(s).

**Header controls:** keep label + `headlineLg`. Section chips (Overview/Spectral/Environment): stop equal-width stretching; compact chips in a start-aligned row — this alone removes most of the "control panel" feel. Range chips (Weekly/Monthly): these are two mutually exclusive values — render as a small 2-segment group of chips right-aligned on the Exposure group header line, not as a second full-width chip row. Two rows of full-width controls collapse into one chip row + one inline segment.

**Pro locked previews feel premium:** locked cards render *real-looking preview data through the real card composable* (the spec's formula/preset preview values are good) under the standardized scrim + tonal lock circle. Copy pattern: eyebrow stays visible above the blur (title outside the locked region) so users know what the module is; only the data blurs. One "Unlock with Pro" 48dp button. Never dim the card title.

**Chart density:** MonthlyTrendChart — grid to 3 lines at `ghostBorder`, drop empty-point markers (2.5f dots for nulls add noise; gaps communicate missing data better), keep 4f data points only at min/max/today. SpectralAnalysisCard — one visualization visible at a time (already modal via chips); reduce stat pills to a single row of 2 (dominant frequency, level), moving the rest behind the RTA mode where they belong; inner canvas backgrounds (`surfaceContainerHighest α0.46`) removed — bars sit directly on the card.

**Mode separation:** Weekly vs Monthly differ by the hero card swap (bar chart vs trend line) — that's enough; don't restyle the rest per range. Spectral and Environment get their own eyebrow group titles so switching sections visibly changes chapter, not just content.

**Empty states intentional:** every analytics card with no data uses the Compact empty variant (32dp icon, one bodyMd line, optional tertiary action like "Start a measurement") *inside its normal card frame at natural height* — never a bare "0%" table (EnvironmentMixCard currently shows zero-rows; replace with compact empty). SoundDetectionCard drops its fixed 352dp height; content defines height in all states.

**HeartRateOverlay:** keep as an optional series inside the exposure/time-series context; footer min–max stays `labelSm`. See §5 note on its color.

---

## 11. History and session detail guidance

**History grouping (top→bottom):** Header → **Today context group** (Last24HoursChart hero on `surfaceContainerHigh`) → **Sessions group** (search controls + recent sessions list) → **Summary group** (WeeklyTrendCard + SafeHoursCard row). 12dp in-group / 32dp between groups. Recent-sessions header (label + View all tertiary) stays.

**SessionCard:** keep it a Material 3 list-card hybrid — full-width `surfaceContainerHigh` cards at 12dp intervals (not a divided list; sessions are substantial objects worth card weight). Refinements: metadata line (`labelSm` uppercase) carries date • duration • sleep badge; **tags move into the metadata line** as plain `#tag` text (max 2, primary color, `labelSm`) instead of a separate visual layer — less noise; PEAK/AVG columns keep `dataLg`, but AVG value in `onSurfaceVariant` when PEAK is the warning-colored one so only one number shouts. Emoji circle 48dp on `surfaceContainer` stays.

**Search controls:** the search field + filter chips live in one `surfaceContainer` card (current structure), compact chips, FlowRow 8dp — fine. Locked variant: standardized overlay; keep the field visible-but-blurred (current approach correct).

**SessionNamingSheet:** conforms already (28dp sheet, 48dp emoji circles, radio semantics). Only: group titles `labelLg` → uppercase `labelMd` for consistency; Save button 48dp full width pinned after content.

**KPI hierarchy:** KpiGrid 2×2 with 12dp gaps stays; each KpiCard = eyebrow + one `dataXl` (§7). If more KPIs exist than 4, prefer a second labeled group ("SCIENTIFIC METRICS" table card with `dataMd` rows) over shrinking the grid — grids of 6+ big numbers are the wall-of-numbers trap.

**Charts polished:** TimeSeriesCard and DbHistogramCard adopt shared chart rules (§16): ghostBorder grid, no inner canvas background on histogram (drop `surfaceContainerHighest α0.46` + 10dp inner radius), bar corner 4dp constant, bucket chips become a single legend row (dot 8dp + `labelSm`) without per-chip tinted backgrounds — tint chips only on touch/selection if filtering is ever added.

**Report/export actions trustworthy:** ReportActions becomes one "EXPORT & SHARE" group: PDF card, PNG share card, WAV card, 12dp apart, each `surfaceContainer` with a leading 24dp `primary` icon, title `bodyLg`, subtitle `bodyMd`, one 48dp button (or two equal-weight for WAV share/delete — delete stays Secondary with `error` content color, not an error-filled button). Exporting state disables the button and swaps label (current behavior good). Success/error ActionMessage becomes an inline status row with leading icon instead of a lone centered colored sentence.

**Locked Pro sections in Session Detail:** DbHistogramCard and PDF export follow the standard locked pattern (title visible, content blurred at natural height, one CTA). Remove fixed 360dp/164dp locked heights.

**Session detail top bar:** keep back + title + edit; add the session emoji before the title for continuity from the list.

---

## 12. Settings screen guidance

**Structure — chapters, not a scroll:** each section = uppercase `labelMd` title with `space8` above / `space3` below, then its card(s). Order unchanged. This plus the `surfaceContainer` demotion makes Settings feel half as long without removing a single row.

**Grouping vs. row hierarchy:** keep one card per section (SettingsCardColumn), but standardize the *row grammar* inside: every control is either (a) DescriptionRow (+ toggle), 48dp min height; (b) control group = `bodyLg` group label, `space2`, control (slider/chips/list); (c) action = full-width 48dp secondary button as the card's last element. Groups inside a card separate by `space5`; plain rows by `space3`. Replace the Noise Notifications section's literal `8.dp`/`12.dp` values with `space2`/`space3` — same visual result, tokenized.

**Dense controls:**
- Sliders: always show current value as `dataMd` on the label row (threshold slider currently shows min/default/max only — add live value; keep the min/max `labelSm` row).
- Chip groups (weighting, theme, waveform, refresh rate): compact chips, FlowRow `space2` — current, fine.
- Device/profile lists: radio rows with ghostBorder dividers (§4), title `bodyMd` SemiBold + `labelMd` subtitle — current structure good; only divider softening.
- Schedule day chips: 7 compact chips in one wrapping FlowRow instead of the fixed 4-per-row grid (frees vertical space); keep state descriptions.

**Pro locked settings:** SettingsLockedCardSection keeps title outside the lock (already does — good, make this the app-wide pattern). Within Noise Notifications, the two small locked sub-controls (audible alarm, TTS) blur only their own rows — current approach fine with the standardized scrim.

**Destructive actions calm but serious:** Clear history / Delete WAV / Restore: Secondary button with `error` content color (not error background), confirmation dialog with plain `bodyMd` consequence sentence, confirm TextButton in `error` (current), cancel first in reading order. Never place a destructive button adjacent to a primary CTA without 12dp separation.

**Dialog consistency (all AlertDialogs):** title `headlineMd`, body `bodyMd` `onSurfaceVariant`, optional leading 24dp `primary` icon, actions as TextButtons (confirm may be `error`), text fields inside dialogs use the standard field style (12dp radius, ghostBorder unfocused, primary α0.3 focused). Profile editor, delete, restore, clear-history, and Health permission dialogs all already approximate this — codify it as one `DbCheckAlertDialog` wrapper so drift stops.

**Pro upsell card:** stays the crown of the screen — gradient border, `surfaceContainer` fill, `headlineMd` title. Add `space8` above it regardless of preceding section so it reads as its own chapter. Purchase states (success/pending/error) as inline status rows with icons.

---

## 13. Setup and task flow screens

Shared scaffold rhythm for all flows (via `DbCheckSetupScaffold` slots, §8): back button, header block (phase `labelMd` primary → `space2` → `headlineLg` → `space3` → `bodyLg` description), `space8`, content cards `space3` apart on `surfaceContainer`, `space16`, full-width 56dp primary CTA, `space8` bottom.

**Sleep setup** — keep simple: duration, keep-awake, action, notes. Improvements: duration chips (6h/8h/10h) compact, natural width; recording action card merges error text + button; notes card gets a "BEFORE YOU SLEEP" eyebrow with privacy note in `onSurfaceVariant` and battery note as an inline warning row (icon + text, warning color on icon only). Recording state flips CTA to Secondary "Stop" — keep, plus the REC dot in the header phase label.

**Hearing setup / recovery setup** — already the best-structured flow. Keep checklist (48dp icon circles). Tokenize the literal 16dp/4dp gaps (`space4`/`space1`). Add a subtle step indicator ("STEP 1 OF 3") in the phase label to improve progression sense across setup → active → results.

**Hearing active / recovery active** — must stay distraction-free: no cards, pure `background`, progress bar + ear indicator + 200dp frequency circle + two response buttons. Improvements: LinearProgressIndicator gets rounded stroke caps and `surfaceContainerHigh` track (current — fine); frequency circle background `surfaceContainerHigh` → add a 1.02 breathing scale synced to tone playback *only if reduced-motion off*; response buttons keep 56dp, gap `space3`, and are the only interactive elements on screen. Error + retry appear between instruction and buttons as an inline status row, not a color block.

**Hearing results** — credible and calm: success icon, "ANALYSIS COMPLETE" eyebrow, rating `headlineLg` colored by outcome (existing mapping), audiogram card, key metrics card, disclaimer, actions. Improvements: audiogram card gets one `bodyMd` takeaway sentence under the eyebrow ("Left ear thresholds are within typical range"); disclaimer text stays `bodyMd` `onSurfaceVariant` centered — do not box it in a warning card (disclaimers are legal texture, not alerts); Save primary + Share secondary 56dp stacked with `space3` (current).

**Tinnitus pitch matcher** — keep one card. Ear selector: replace the two stretched AssistChips with two compact selectable chips, natural width, centered; selected ear uses `primaryContainer` + check icon (disabling the selected chip, as now, reads as broken — make it selected-but-enabled). Slider card: frequency value becomes the card's `dataXl` moment ("3,200 Hz"), slider under it, Preview/Save 48dp row. All `labelSm` status/disclaimer strings: keep size, ensure `onSurfaceVariant`.

**Ambient sound playback** — same card grammar: preset chips compact (icon + label, natural width, wrap), volume slider with live % `dataMd`, timer chips compact. Play primary / Stop secondary 48dp. Playing state: swap card eyebrow to "PLAYING • <preset>" with a small equalizer glyph rather than recoloring anything.

**Health Connect disclosure activity** — keep the plain document look (it's a consent surface; chrome would undermine it): `headlineMd` title, `bodyLg` item titles, `bodyMd` `onSurfaceVariant` bodies, 24dp margins. Only alignment with dialog/typography rules; no cards needed.

**Warnings/privacy/disclaimers everywhere:** one pattern — inline row, 20dp icon in `warning` (or `onSurfaceVariant` for neutral privacy notes), `bodyMd` text in `onSurface`/`onSurfaceVariant`. Text itself is never fully colored warning-yellow (current sleep battery note and WAV privacy warning color the whole sentence — recolor icon only).

---

## 14. Camera overlay guidance

Context: content sits over live camera video; this is the one legitimately dark-only surface set.

- **Permission states:** follow §17 full-screen state pattern on `background` (current 320dp max width, 48dp icon, headline, body, primary button is right). Permanently-denied variant deep-links to app settings (existing behavior).
- **Panels over video:** put the readout and the capture-control cluster on real panels instead of raw text over video: `surface.copy(alpha = 0.72f)` (matching the close button), radius 16dp, padding 16dp, with the existing 24dp screen margins. 0.72 alpha over video keeps legibility in bright scenes without feeling like a letterbox; do not exceed ~0.8 or the camera feels covered.
- **Readout dominant, not obstructive:** bottom-start panel: status `labelMd` primaryContainer, dB `displayMd`, level `bodyMd`, timestamp `labelMd` — current type is right; the panel constrains its width to content (max ~60% screen width) so the viewfinder center stays clear.
- **Capture controls Android-native:** bottom-end cluster; the *photo* button becomes the big one — 72dp circle, `primary` fill (video 56dp above it) — mirroring camera-app conventions (big shutter, smaller mode buttons). Video-recording state keeps `error` fill + Stop icon. Disabled: `surface.copy(0.56f)` fill + `onSurfaceVariant` icon (current). Privacy caption `labelMd` stays above the cluster.
- **Close button:** keep 48dp circle, top-start, `surface α0.72`.
- **Static preview fallback / unavailable:** keep the dedicated overlay palette (`0xFF0B1114` etc.) — it's a deliberate "camera-like" dark scene; align its title/body styles with the state pattern.
- **Burned-in share overlay:** relate to brand without weight — keep the bottom-left panel but: corner radius 18f→24f·scale (matches app cards), panel fill `0xB0000000` stays, and add a small "dBcheck" wordmark line in the panel's top-right in the timestamp color (16f·scale). Text hierarchy (status/dB/level/timestamp) already mirrors the app; do not add the gradient or logos beyond the wordmark — a share photo should look like a measurement, not an ad.

---

## 15. Widget, notification, share image, and PDF guidance

**How much brand is enough:** outside the app, brand = (1) the "dBcheck" wordmark in Manrope/medium, (2) Space Grotesk numerals, (3) the monochrome-plus-threshold-color scheme. That trio is sufficient everywhere; never export the gradient, ghost borders, or tonal ladder to external surfaces.

- **Glance widget:** conforms well. Keep `GlanceTheme.widgetBackground` (dynamic system corner/color = correct for home screens). Align: noise-level label color follows NoiseLevel (success/warning/error) instead of flat `primary` — the widget's one glance-value is "how loud", color should carry it. Pro-locked widget: lock emoji → proper lock icon resource if Glance version allows; copy stays 2 lines max.
- **Measurement notification (collapsed):** 48sp Space Grotesk value + peak/duration row is right. Keep custom palette (`#0E0E0E`/`#201F1F`) — it approximates the dark theme; add nothing. Verify the layout at fontScale 1.3: the 48sp value must not clip (`includeFontPadding=false` already set; test).
- **Measurement notification (expanded):** right panel (92dp, dot + level) is good; ensure dot color mapping (SAFE/ELEVATED/DANGEROUS) matches app NoiseLevel names 1:1. Session name 13sp Manrope stays. Both layouts: minimum text 12sp — the 10sp/11sp sizes exist only in the widget, keep them out of notifications.
- **Ambient playback notification:** standard template notification (no custom layout needed) — title, preset, stop action. Silent, ongoing, private visibility (current).
- **Alert notifications:** system template, high priority; the only color use is the small-icon tint. No custom red backgrounds.
- **Hearing test share card (1080×1080, dark):** modernize composition, keep seriousness: switch `sans-serif`/`sans-serif-medium` typefaces to the bundled Space Grotesk (score) and Manrope (labels) so shares match the brand; add the wordmark bottom-left; keep the dark `0xFF0E0E0E` background.
- **Session report share card (1080×1080, light):** the light card is fine (share contexts are often light feeds) but it currently uses no brand fonts either — same font swap. Metric cards at 28f radius on `0xFFECEEEE` echo the app's tile system; keep. **Unify the two share cards into one template family** (same margins 80px, same wordmark placement, same label/value type ramp) — dark for hearing, light for session reports is acceptable as long as typography and layout match.
- **PDF report:** stay report-like, do not import the app UI palette. The PDF's own palette (white page, `rgb(70,105,6)` accent, grey cards) is appropriate for print/professional contexts where a dark UI palette would waste toner and look off. Alignment instead of adoption: it already uses Space Grotesk/Manrope paints — good; ensure chart threshold/series colors in the PDF match the *meaning* (threshold dashed = warning meaning) even though hexes differ. Add the wordmark in the header (exists: app name brandPaint) — sufficient.
- **Avoiding mismatch:** create one internal reference sheet (constants file) listing the type ramp and NoiseLevel color mapping used by widget/notification/share/PDF renderers, so external surfaces evolve together. Mismatch risk is highest in the three hand-painted canvases (share cards, PDF, burn-in) — they should all consume the same `ExternalBrand` constants.

---

## 16. Data visualization guidance

**Shared chart rules (apply to every canvas):**

- **Grid:** `ghostBorder`, 1dp, 3–4 horizontal lines, no vertical grid except time-axis end ticks. One grid style app-wide (kills the 0.32/0.24/0.35 zoo).
- **No inner canvas backgrounds.** Charts sit directly on their card surface (remove `surfaceContainerHighest α0.46` fills from spectrogram, RTA, histogram). The card is the frame.
- **Lines:** primary series `primary` 3dp round cap (unify the 2/3/4dp strokes; the live meter chart may keep 4dp as the "live" exception). Second series `secondary` 3dp. Area fills: series color α0.12 max (Last24Hours' α0.3 → 0.12).
- **Bars:** solid `primary`; de-emphasized bars `primaryDim` (α-dimming replaced by the token); corner radius 4dp constant; bar gap ≥ 2dp.
- **Points:** 4dp radius, only at semantically meaningful positions (current/today/min/max/single-datum). No dots on every sample and no dots on nulls.
- **Thresholds:** 85 dB and friends = `error` α0.72 dashed (8,6) 2dp — adopt the PDF's dash pattern in-app so threshold lines are distinguishable from data lines. Threshold gets a tiny right-aligned `labelSm` tag ("85"). This is the *only* red allowed inside charts; bars/zones use the bucket mapping already defined.
- **Axes/labels:** `labelSm` `onSurfaceVariant`; label only edges (first/last time, min/max value) plus "now" where relevant; interior labels only when a chart is the screen hero (exposure weekly day letters stay).
- **Warning/error without alarm:** color area = data ink only (line segments above threshold, buckets, zone bar segments); never tint the chart background by zone.
- **Missing data:** gaps in lines (no interpolation, no null markers); for bar charts, hairline `ghostBorder` placeholder bar of 2dp height. Label "No data" only when the whole chart is empty (compact empty state).
- **Locked preview data:** always plausible preset values (existing spec presets are good) rendered identically to real data, then blurred — never a different "fake-looking" style.

**Per component:**

- **Circular gauge:** stays the precision instrument (§9). Track `outlineVariant α0.32` → `ghostBorder`-consistent; arc colors per NoiseLevel unchanged; 200ms EaseOut sweep unchanged.
- **Live sound level chart:** 4dp live stroke (exception), threshold per rule, peak markers ≥85 keep `error` dots (semantic points).
- **Waveform:** `tertiary α0.2`, 32dp height, BAR/LINE/FILLED user styles kept.
- **Dosimeter gauge:** track to ghostBorder; progress color success/warning/error mapping stays (it's a true threshold instrument).
- **Weekly bar chart:** solid bars per rule; today `primary`, others `primaryDim`; day letters stay.
- **Monthly trend:** 3 grid lines, gaps for nulls, endpoint labels only.
- **Zone distribution bar:** 24dp height, bucket colors, 12dp radius (align to row radius) — plus `labelSm` percentages in the legend rows (existing).
- **Environment mix rows:** dot 8dp + `bodyMd` + `dataMd` percent — conforms; empty → compact empty state instead of 0% rows.
- **Spectral bars/spectrogram/RTA:** remove inner backgrounds; bars `primary α0.7` ok; spectrogram color lerp primary→tertiary stays (it's a heat scale, monochrome-appropriate); RTA lerp primary→secondary stays.
- **Sound detection confidence meter:** 8dp track `surfaceContainerHighest`, `primary` fill — conforms; add % `labelMd` at row end (exists).
- **Last 24 hours chart:** fill α0.3 → 0.12; 5 time labels stay.
- **Time series (detail):** ghostBorder grid (already), 3dp stroke (already), heart-rate overlay as second series.
- **Histogram:** no inner background; bucket colors stay; legend chips → plain legend rows (§11).
- **Audiogram:** left `primary` / right `secondary` (already); 6f points fine; keep log2 x-scale; legend dots 12dp; add threshold-zone shading only if audiologically standard — otherwise leave clean.
- **Heart rate overlay:** series color decision per §5; baseline ghostBorder (already).
- **PDF charts:** keep print palette; adopt the same "grid 1f, points at meaning, dashed thresholds" grammar (mostly already true).

---

## 17. State design guidance

**One state system, four containers:**

| Container | Use for | Anatomy |
|---|---|---|
| Full-screen state | permission denied (mic/camera), whole-screen empty/error (Analytics, History), hearing results loading/locked/missing | 48dp padding, 64dp icon α0.5, `headlineMd`, `bodyMd` (max ~280dp width), primary 48dp + optional secondary, `space3` gaps |
| In-card state | empty chart cards, no-devices, no-backups, no-detections | Compact: 24dp padding, 32dp icon, `bodyMd`, optional tertiary action |
| Inline status row | export success/error, purchase pending/success/error, calibration messages, share errors, validation | 20dp leading icon (success/warning/error/`primary`), `bodyMd` `onSurface`, appears with 150ms fade, auto-dismiss 3000ms where currently timed |
| Dialog | destructive confirmations, permission rationale, profile edit | `DbCheckAlertDialog` grammar (§12) |

**Specific states:**

- **Loading:** skeletons matching real component heights (§4); never spinners for content, spinners (18dp, inside button) only for in-button progress (exporting, purchasing).
- **Permission denied vs permanently denied:** same full-screen layout; button label switches (Try again / Open settings) — never guilt copy.
- **Locked Pro:** not a "state" in the error sense — standardized preview overlay (§8). Locked ≠ disabled: locked content stays visually rich under the scrim. Avoid punishment: title never blurred, preview always plausible, one calm CTA, no lock icons in section titles (one lock per locked region).
- **Disabled controls:** content color `onSurfaceVariant` + 38% state convention; never blur (blur is reserved for locked).
- **Recording active / passive monitoring active:** persistent inline indicator pattern — red/`primary` 8dp dot + `labelMd` + duration `dataMd` (MeterSessionInfoBar grammar); passive monitoring in Settings shows the same dot row in its card ("MONITORING" in `labelMd` `onSurfaceVariant`).
- **Exporting / backup / restore in progress:** button-internal progress + disabled siblings; never modal-block the whole screen for a file export.
- **Purchase success/pending/error:** inline status rows under the purchase button; success may use `success` icon + text; error uses `error` icon, `onSurface` text, retry stays available.
- **Calibration edit/delete dialogs:** per dialog grammar; delete confirm TextButton `error` (current).
- **Camera permission states:** full-screen state on `background` (§14).
- **Red restraint:** in any error state, red appears once (icon or confirm button), text stays `onSurface`/`onSurfaceVariant`. A screen may never show more than one red element per state.

---

## 18. Accessibility and semantics review

The refinement must not regress the existing semantics layer (spec §21). Checklist against each change:

- **Touch targets:** all changes preserve 48dp minimums — compact chips reduce *visual* pill height to 32dp but keep 48dp touch via existing min-height behavior; bottom-nav pill is decorative, item target unchanged; icon buttons stay 48dp.
- **Chip selected state:** monochrome selection is the weakest contrast point in dark theme (`primaryContainer` #EDEDED vs `surfaceContainerHigh` #202020 is fine — inverted and strong; verify light theme #E4E4E1 vs #E6E6E3 which is nearly identical → in light theme selected chips must keep the check icon and/or `onPrimaryContainer` text weight change; this is a real current-state risk the redesign should fix, not create).
- **Bottom navigation:** role Tab retained; adding reserved label space improves stability for TalkBack focus order; selected state announced via existing state descriptions.
- **Radio rows:** role RadioButton + full-row click targets retained in calibration and emoji picker.
- **Chart content descriptions:** required on every chart touched by §16 changes — live chart, audiogram, and schedule chips already set them; extend the same pattern to weekly bars, monthly trend, histogram, time series ("Weekly exposure chart. Monday 62 dB … Sunday 71 dB, average 65.2 dB").
- **Camera overlay readout:** semantic block (status/dB/level/timestamp) retained; the new panel improves visual contrast over bright scenes (fixes a latent issue, since raw text over video can silently fail contrast).
- **Notification day chips / schedule:** state descriptions retained after compact-density change.
- **Audiogram:** legend + per-ear content description retained; left/right must also differ by point shape (circle vs. X marker) if feasible — color-only ear distinction is a colorblind risk worth removing during §16 work.
- **Contrast:** re-verify after surface demotion: `onSurfaceVariant` #B8B8B8 on `surfaceContainer` #171717 ≈ 9.5:1 (fine); light theme #5F5F5F on #F0F0EF ≈ 5.2:1 (fine). ghostBorder is decorative-only, exempt. `secondary` as text: avoid below 14sp (borderline in dark).
- **Color + text/icon pairing:** every NoiseLevel color use pairs with a label (pill text, legend, dot+word) — maintain in zone bars and new legend rows; trend direction adds ▲/▼ glyph per §5.
- **Reduced motion:** NoiseLevelPill already gates animation; extend the same gate to shimmer, breathing scale, gauge sweep (fall back to instant), and `animateContentSize` on SoundReferenceCard.

No visual recommendation in this plan requires weakening any semantic; where visual and a11y conflicted (locked chip dimming, color-only selection), a11y won.

---

## 19. Before and after examples

**1. Meter gauge area**
- Current pattern: space8 → chip row → space6 → gauge 288dp → space6 → chart, all on background, equal-weight neighbors above and below.
- Problem: the hero has no extra air relative to support content; chart begins immediately, so the gauge reads as "first item in a list".
- Proposed pattern: Zone A (chips + gauge + pill) isolated with `space8` below the gauge before the merged Live card; gauge glass and ticks softened per §9.
- Implementation: reorder spacers in `MeterScreen` column; no component API changes.
- Improvement: instant hero focus; the screen reads instrument-first.

**2. Mode chip row**
- Current: DB Meter / Dosimeter chips at 16dp padding, locked variant dimmed text.
- Problem: locked dimming feels punitive; chips visually identical to filter chips elsewhere.
- Proposed: compact chips, natural width, centered; locked chip = normal label + 14dp lock glyph, no dimming.
- Implementation: `DbCheckChip(density = Compact)`; remove alpha on locked label.
- Improvement: mode switcher looks like a mode switcher; Pro upsell feels invitational.

**3. Live chart + waveform area**
- Current: LiveSoundLevelChart (116dp, frameless) → space4 → SoundReferenceCard → space4 → Waveform (48dp, frameless) — two naked visualizations separated by a card.
- Problem: raw canvases float on the page; waveform competes as a second live module; the sandwich order interleaves live and reference content.
- Proposed: one "LIVE" `surfaceContainerHigh` card containing chart (116dp) + 12dp + waveform strip (32dp); SoundReferenceCard moves below it.
- Implementation: new `LiveActivityCard` composable wrapping both existing canvases; reorder in MeterScreen.
- Improvement: charts feel like designed modules; live vs. reference content separates cleanly.

**4. Stats row (MIN/AVG/PEAK)**
- Current: three StatCards, `surfaceContainerHigh`, radius 16dp, gap 12dp.
- Problem: same surface as every card above → equal rank with the hero content.
- Proposed: keep geometry; after the default-card demotion, these stay `surfaceContainerHigh` intentionally (they're live values) — the surrounding demotion alone re-ranks them correctly.
- Implementation: none beyond the DbCheckCard default change (StatCard already explicit).
- Improvement: hierarchy without touching the component.

**5. Analytics header controls**
- Current: label + headlineLg + full-width equal-weight section chips + second full-width range chip row.
- Problem: two stacked stretched control rows = control panel; stretched chips look non-native.
- Proposed: one start-aligned compact chip row for sections; Weekly/Monthly as a compact 2-chip segment on the Exposure group header line.
- Implementation: remove `weight(1f)` from chip rows; move range chips into the Exposure section header Row (SpaceBetween).
- Improvement: header loses one full control row; feels like a modern Android filter bar.

**6. Session Detail KPI grid**
- Current: 2×2 KpiCards, each labelMd + dataXl, min height 112dp, gaps 12dp — followed by more cards at 16dp gaps.
- Problem: the grid is good, but it sits at the same rhythm as everything after it; four huge numbers + subsequent numeric cards = numeric wall.
- Proposed: KPI grid promoted to `surfaceContainerHigh` as the detail hero (with SessionSummary); charts and tables below demoted to `surfaceContainer` in their own "DETAILS" group at 32dp.
- Implementation: emphasis parameter on KpiCard's DbCheckCard; regroup LazyColumn items with section spacers.
- Improvement: summary-then-details narrative; numbers get ranked.

**7. Settings Noise Notifications card**
- Current: one card, internal Column gap 12dp with literal 8dp/12dp values, seven control blocks stacked (toggles, alarm, TTS, passive monitoring, threshold, schedule).
- Problem: the densest card in the app; sub-controls (alarm preview button, schedule day grid, two sliders) crowd without internal grouping.
- Proposed: keep one section; inside the card, group blocks with `space5` between control groups and `space3` within; day chips one wrapping FlowRow; threshold slider gains live value readout; tokens replace literals.
- Implementation: spacing refactor only; no control changes.
- Improvement: the card reads as five labeled groups instead of one dense stack.

**8. History session card**
- Current: emoji circle + title + metadata + separate tag row + PEAK/AVG columns, both values `dataLg` in onSurface/warning.
- Problem: four content layers with tags as an extra line; two equally loud numbers.
- Proposed: tags fold into the metadata line; AVG demotes to `onSurfaceVariant` when PEAK carries warning color; everything else stays.
- Implementation: SessionCard row merge + one color conditional.
- Improvement: one line less per card ≈ 10–15% shorter list, faster scanning, warning value pops.

**9. Locked SoundDetectionCard (Analytics)**
- Current: outer height fixed 352dp, whole card (title included) under blur+scrim.
- Problem: fixed height diverges from content; blurred title hides what the module even is.
- Proposed: title rendered above the locked region; preview content (confidence 82, Speech/Music/Vehicle rows) at natural height under standardized scrim + 48dp tonal lock circle + one CTA.
- Implementation: move title out of ProLockOverlay child; delete the fixed height.
- Improvement: locked module is self-explanatory and feels premium.

**10. Measurement notification (expanded)**
- Current: 48sp value + right panel 92dp with dot + level label, custom dark palette.
- Problem: solid, but level color appears only as a 12dp dot; at a glance the notification is monochrome.
- Proposed: keep layout; level label text adopts the dot's color (green/yellow/red text on #201F1F passes contrast for these hexes — verify #C9A24D ≈ 6.4:1 ✓, #E07A7A ≈ 5.9:1 ✓, #8EA58E ≈ 6.9:1 ✓).
- Implementation: one setTextColor per state in NotificationHelper.
- Improvement: glanceable loudness state from the lock screen.

**11. Settings Pro upsell card**
- Current: gradient-bordered card, headlineMd + body + 48dp purchase button, messages appended below.
- Problem: after demotion of neighbors it already wins; messages as bare colored text feel bolted on.
- Proposed: `space8` isolation above; purchase status as inline icon rows; keep border as the app's only border.
- Implementation: spacing + status-row composable reuse.
- Improvement: the upsell reads as a considered offer, not a banner.

**12. Hearing results audiogram card**
- Current: card with labelMd title, legend dots, 150dp chart, left/right in primary/secondary.
- Problem: ear distinction is color-only; no plain-language takeaway.
- Proposed: circle vs. X point markers per ear; one `bodyMd` takeaway sentence under the title; chart rules from §16.
- Implementation: marker branch in audiogram Canvas; string from existing rating data.
- Improvement: credible, readable, colorblind-safe result.

---

## 20. Compose implementation plan

### Phase 1: Token and surface cleanup
- First: add `pageMargin`, `groupGap` (12dp), `sectionGap` (32dp) aliases to `DbCheckSpacing`; central `DbCheckMotion` durations; unify shape usage to 24/16/12/Circle; change `DbCheckCard` default to `surfaceContainer` + add emphasis parameter; standardize ProLockOverlay scrim constant; document gradient/border/ghostBorder rules in theme files.
- Affects: `ui/theme/*`, `DbCheckCard`, `ProLockOverlay`.
- Watch for: every card silently demoting — audit each screen and re-promote heroes (SessionCard, ExposureSummary, Last24Hours, KPI, StatCard, Live card) in the same PR; light-theme contrast of demoted cards; chip selected state in light theme.
- Safe: pure visual, no behavior. Both themes screenshot-tested per screen.

### Phase 2: Shared component updates
- First: DbCheckChip densities + natural-width rows; button size cleanup (44→48); bottom-nav pill indicator + reserved label space; nav rail parity; EmptyState compact variant; SkeletonSpec heights; `DbCheckAlertDialog` wrapper; inline status-row composable; locked overlay tonal lock circle.
- Affects: `ui/components/*`, `ui/navigation/*`.
- Watch for: chip rows that relied on `weight(1f)` widths (Analytics sections, tinnitus ears, ambient presets/timers); TalkBack focus after nav changes; skeleton/content height parity.
- Safe: all behavior-free except chip-row layout, which needs small-screen wrap testing.

### Phase 3: Meter screen refinement
- First: zone spacing reorder; `LiveActivityCard` (chart + waveform merge, waveform 32dp); SoundReferenceCard repositioned + `surfaceContainer`; recording-state treatments (eyebrow, record button error fill); dosimeter as Zone B content swap; sub-640dp gauge compression.
- Affects: `ui/meter/*`.
- Watch for: low-height devices (chart+waveform card must not push controls off-screen — verify at 592dp height); recording start/stop layout stability; gauge readout semantics unchanged.
- Safe: everything except the dosimeter content-swap, which touches mode logic presentation (keep effective-mode logic untouched).

### Phase 4: Analytics, History, and Session Detail refinement
- First: section grouping (headers + 32/12dp rhythm) on all three screens; chart-rule pass (§16) across weekly/monthly/spectral/histogram/last24h/timeseries canvases; locked-card natural heights + titles-outside-blur; SessionCard tag merge; KPI hero promotion; ReportActions group + status rows; header-controls consolidation.
- Affects: `ui/analytics/*`, `ui/history/*`, plus canvas painters.
- Watch for: LazyColumn scroll position restoration after item regrouping; chart pixel changes needing new screenshot baselines; locked previews rendering real composables (make sure preview data can't leak into real state).
- Safe: chart repaints are safe; regrouping needs a11y traversal re-check.

### Phase 5: Settings and setup flow refinement
- First: Settings chapter spacing + literal→token sweep (Noise Notifications); row grammar; day-chip FlowRow; slider value readouts; dialog wrapper adoption; setup scaffold slots (header/step/CTA) adopted by sleep/hearing/tinnitus/ambient; warning-text recolor (icon-only color); tinnitus ear-chip selected-not-disabled fix.
- Affects: `ui/settings/*`, `ui/sleep/*`, `ui/hearingtest/*` (setup/results), `ui/tinnitus/*`, `ui/ambient/*`.
- Watch for: scrollToProCard offset after spacing changes; hearing active screen must remain visually untouched except progress-track token; disclosure activity only typography-aligned.
- Safe: all visual; the ear-chip enablement change is the one behavior tweak (selected chip stays enabled, click = no-op).

### Phase 6: External surfaces refinement
- First: `ExternalBrand` constants file; widget level-color mapping; notification level-text color + fontScale test; share-card font swap to bundled typefaces + unified template + wordmark; burn-in radius + wordmark; PDF grammar alignment (already close).
- Affects: `widget/*`, `service/NotificationHelper`, `util/ShareResultsGenerator`, `util/ExportPdfReportUseCase`, `util/PdfChartRenderer`, notification XMLs.
- Watch for: RemoteViews constraints (custom fonts in XML layouts need font resources referenced correctly); bitmap text measurement after font swap (ellipsizing positions); Glance version constraints for icon.
- Safe: all output-only; verify share images on both light and dark recipient contexts.

### Phase 7: Final consistency and accessibility pass
- First: dark/light screenshot matrix of every screen and state; contrast spot-checks listed in §18; touch-target audit (compact chips, nav items, side buttons); chart contentDescription completion; reduced-motion gate on shimmer/breathing/sweep/contentSize; TalkBack walkthrough of Meter, locked Analytics, Settings, hearing flow; RTL smoke test.
- Affects: everything; produces fixes, not features.
- Watch for: regressions introduced by phases 3–5 grouping (focus order), light-theme selected-chip issue (must be closed here at latest).
- Safe: by definition.

---

## 21. Prioritized recommendations

**Priority 1 — highest impact, do first**
1. **Surface re-ranking (default card → `surfaceContainer`, heroes promoted).** Why: single change that creates hierarchy app-wide. Screens: all. Type: tokens + shared component. Risk: medium (needs per-screen audit, light-theme checks).
2. **Spacing rhythm (12dp in-group / 32dp between groups + section headers own their space).** Why: turns lists into compositions; biggest "calm" gain. Screens: Meter, Analytics, History, Detail, Settings. Type: screen layout. Risk: low.
3. **Meter Zone A/B/C restructure (gauge air + Live card merge).** Why: the flagship screen must read instrument-first. Screens: Meter. Type: screen layout + one new wrapper component. Risk: low-medium (small screens).
4. **Bottom navigation M3 pill indicator + stable labels.** Why: highest-visibility "recognizably M3" signal, trivial cost. Screens: all top-level. Type: shared component. Risk: low.
5. **Chart rules pass (shared grid/stroke/fill/threshold grammar, no inner canvas backgrounds).** Why: charts are dBcheck's face; consistency = credibility. Screens: Meter, Analytics, History, Detail. Type: shared painters. Risk: low.

**Priority 2 — good polish**
6. Locked-state standardization (scrim, natural heights, titles outside blur, tonal lock circle). Screens: Analytics, History, Detail, Settings. Type: shared component. Risk: low.
7. Chip system (two densities, natural widths, selected check icon, light-theme selection fix). Screens: Meter, Analytics, History, Settings, tinnitus, ambient. Type: shared component. Risk: medium (layout reflow).
8. Settings chapters + Noise Notifications internal grouping + token sweep. Screens: Settings. Type: screen layout. Risk: low.
9. Typography deployment rules (one dataXl per card, universal eyebrow, takeaway sentences). Screens: all card screens. Type: screen layout. Risk: low.
10. State system (inline status rows, compact empty states, dialog wrapper). Screens: all. Type: shared components. Risk: low.

**Priority 3 — later refinements**
11. Setup scaffold slots + flow alignment (sleep/hearing/tinnitus/ambient). Type: shared component + screens. Risk: low.
12. Camera overlay panels + capture-control hierarchy + burn-in wordmark. Type: screen layout + renderer. Risk: low.
13. External surface alignment (widget level colors, notification level text color, share-card fonts/template, PDF grammar). Type: renderers/XML. Risk: medium (RemoteViews/bitmap text metrics).
14. Motion tokens + reduced-motion gates everywhere. Type: tokens + sweep. Risk: low.
15. Audiogram ear markers + extended chart content descriptions. Type: painters + semantics. Risk: low.

---

## 22. Do-not-change list

Binding constraints for every phase above:

- Do not change the app name or brand capitalization — **dBcheck**, exactly.
- Do not change the app's purpose: sound level, exposure, hearing, sleep, tinnitus, ambient sound, history, analytics, reporting.
- Do not invent new product features; every recommendation above restyles existing content only.
- Do not remove measurement detail to simplify — regroup and re-rank it instead.
- Do not replace the existing dark and light palettes; all color guidance uses existing tokens and roles.
- Do not replace Manrope or Space Grotesk, and do not alter the type scale.
- Do not introduce a new navigation model; top-level stays meter / analytics / history / settings.
- Do not remove top-level destinations or alter route structure.
- Do not discard the existing bottom navigation / navigation rail behavior (600dp breakpoint, hidden nav on fullscreen routes, edge-to-edge insets).
- Do not copy the reference screenshots' colors, branding, content, assets, or layouts — feel only.
- Do not make the app playful or social; tone stays professional, quiet, trustworthy.
- Do not let warning/error colors dominate — NoiseLevel semantics, status rows, and destructive accents only; one red element per state.
- Do not make locked Pro states punitive — preview + calm CTA, titles never blurred, no dimmed locked chips.
- Do not sacrifice accessibility: 48dp targets, selected-state clarity beyond color, chart descriptions, reduced-motion respect, and contrast in both themes are non-negotiable (§18).
- Additionally, per project rules: no 1px sectioning borders (Pro upsell gradient border is the sole exception), no Material elevation shadows (tonal layering + AmbientShadow only), minimum 8dp corner radius, no dynamic color schemes.



