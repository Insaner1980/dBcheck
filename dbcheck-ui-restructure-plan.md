# dBcheck UI/UX Restructure Plan

**Source:** This plan is based solely on an analysis of 21 app screenshots (Meter, Analytics, History, Settings tabs). It has NOT been verified against the codebase.

**Instructions for the implementing agent (read first):**

1. Before planning any change, verify in the code where each screen, card, and component actually lives. Some observations below may be slightly off or already handled differently in code.
2. If something described here already works as proposed, or a simpler mechanism already exists (e.g. the Settings "Feature toggles" section already controls visibility of the Dosimeter card and Sleep card), **do not invent work** — mark it as already done and move on.
3. Prefer the simplest change that achieves the goal. Do not refactor navigation frameworks, state management, or theming systems unless strictly required by a step. Moving a card from one screen to another should not become an architecture rewrite.
4. This is a reorganization of existing features. Do not add new features, and do not remove any functionality — only relocate, group, and restyle.
5. Produce your own concrete implementation plan (file-level) after inspecting the code, using this document as the requirements spec.

---

## Problem summary

The app's core issue is information architecture, not individual visuals: screens contain content that doesn't belong to their primary task, the same cards appear in multiple places, and the Settings screen is one extremely long scroll. Visual styling is mostly consistent but has several discrepancies (button styles, icons, casing, sliders, selection states).

---

## Phase 1 — Information architecture

### 1.1 Bottom navigation

Current: 4 tabs — Meter, Analytics, History, Settings. A top-right icon duplicates Settings on some screens and shows a profile icon on Analytics.

Target: 5 tabs — **Meter | Trends | Hearing | History | Settings**

- "Trends" replaces "Analytics" (data only, see 1.3).
- New "Hearing" tab collects hearing-health tools (see 1.4).
- Top-right header icon: remove the Settings gear duplication. Either remove the top-right icon entirely or use one consistent icon on every screen. One entry point to Settings only.

If a 5th tab is problematic in the current implementation, an acceptable fallback: keep 4 tabs and make Hearing a hub page reachable from Trends via a single clearly-labeled link — but the 5-tab layout is preferred (Material 3 supports 3–5 destinations).

### 1.2 Meter screen (main screen) — remove scrolling

Keep ONLY:

- dB Meter / Dosimeter mode toggle
- The gauge (current dB + status badge, e.g. WHISPER)
- MIN / LCEQ (or AVG) / MAX row
- Transport controls (reset, start/stop, screenshot, share)
- When Dosimeter mode is active: the dosimeter info/status card

Changes:

- **Remove the Sleep Monitor card** from this screen (it also exists on Analytics — one home only, see 1.4).
- **Sound References card:** replace the full inline card with a compact single row under the gauge (e.g. "Nearest: Whispering · 30 dB"). Tapping it opens the full reference view as a bottom sheet or expandable section. Collapsed by default.
- Idle state: when no measurement is running, show a short "start measuring" hint inside or near the gauge area so the play button isn't the only affordance.

Goal: everything fits on one screen without scrolling on a typical phone viewport.

### 1.3 Trends screen (formerly Analytics) — data only

Keep:

- Overview / Spectral / Env Mix chips and Weekly / Monthly chips
- Exposure charts, averages
- Environment mix (Quiet/Moderate/Loud/Critical breakdown)
- 12-month report / summaries
- The short hearing-status summary card ("Weekly noise exposure is in the lower range") may stay as a status line, but it should link to the Hearing tab rather than hosting tools.

Move OUT (to Hearing tab): Check Your Hearing (test), Recovery Check, Tinnitus pitch matcher, Ambient Sound tool, Sleep Monitor card, and the whole "Tools" section.

### 1.4 New Hearing tab

Contents (all existing features, relocated):

- Hearing status summary (the check-mark card)
- Check Your Hearing (3-minute test)
- Recovery Check
- Tinnitus pitch matcher
- Voice Baseline (currently buried in Settings — verify; if it is a tool rather than a preference, it belongs here)
- Tools group: Sleep Monitor, Ambient Sound

Layout: simple stacked cards, one section "Checks" and one section "Tools". No duplication of these cards anywhere else in the app.

### 1.5 History screen — compact empty states

- The "Last 24 hours" chart currently occupies roughly half the screen even when empty ("No chart samples available"). When there is no data, collapse it to a compact single-line card; render the full chart only when samples exist.
- Session list rows: session names truncate ("Evening Se…"). Give the title more width — reduce the visual weight/size of PEAK/AVG numbers or move them to a second line.

---

## Phase 2 — Settings restructure

Current: one very long scroll containing calibration (including 10 octave-band sliders), notifications, alert schedule, quiet hours, Health Connect, data export, display options, feature toggles, lock-screen options, and the Pro upsell.

Target: a short top-level Settings page that is only a grouped list of navigation items, each opening its own subpage:

1. **Calibration** — mic sensitivity, frequency weighting, audio input, calibration profiles. Octave calibration goes one level deeper (its own screen) — it is an advanced feature.
2. **Notifications & alerts** — exposure alerts, peak warnings, audible alarm, spoken risk prompt, passive monitoring, threshold, alert schedule, quiet hours.
3. **Data & privacy** — Health Connect, CSV export, WAV recording default, session location, backups, clear history, lock-screen visibility options.
4. **Display** — dark mode, waveform style, refresh rate, feature toggles (technical metadata, dosimeter card, sound detection, sleep card).
5. **Pro / About** — upgrade card, version info.

Notes:

- Long yellow warning paragraphs (WAV recording, passive monitoring, lock-screen privacy) should be shortened to one line with an info affordance revealing the full text, or shown contextually only when the user enables the option.
- Keep all existing settings — nothing is removed, only regrouped.

---

## Phase 3 — Visual consistency pass

Follow Material Design 3 conventions (Android app). Apply after Phases 1–2 so styling lands on the final structure.

1. **Buttons — define two levels and use them consistently:**
   - Primary CTA: the current light/gradient pill (used for Start Test, Open sleep setup) — exactly one per card/section.
   - Secondary: the dark gray pill (Export CSV, Open pitch matcher style).
   - Replace odd one-offs like the bare uppercase "ADD" text button with the standard secondary/text-button style used elsewhere.
2. **Icons:** remove emoji used as section icons in Settings (bell, palette, sliders emoji). Use the same line-icon set as the bottom navigation.
3. **Text casing:** reduce ALL-CAPS usage. Keep small caps-style overline labels only for short section labels; card titles in Title case; body text in sentence case. Verify one consistent rule and apply it.
4. **Selection states:** day-of-week picker (Mon–Sun) currently shows all days as filled white circles — selected and unselected states are indistinguishable. Ensure a clear visual difference (filled vs. outlined). Audit all chip groups for sufficient unselected-state contrast.
5. **Sliders:** unify to one slider style. Currently at least three variants exist (thick white-filled, dotted-track time pickers, and a reference slider with a yellow marker).
6. **Session list icons:** emoji thumbnails (coffee cup, moon, cityscape) are acceptable if intentional as category markers — keep or replace consistently, don't mix approaches.

---

## Phase 4 — Verification

- Screenshot each tab after changes and compare against the goals: Meter fits one viewport; no card appears on two screens; Settings top level fits ~one viewport; all previously existing features remain reachable.
- Check touch targets ≥ 48dp and text contrast (WCAG AA) on the dark theme, especially for gray-on-dark secondary text and unselected chips.

## Suggested implementation order

Phase 1 (highest impact) → Phase 2 → Phase 3 → Phase 4. Each phase should be a separately reviewable change set.
