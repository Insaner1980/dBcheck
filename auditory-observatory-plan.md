# dBcheck “The Auditory Observatory” v1.0 — kattava toteutussuunnitelma

## Yhteenveto

Toteutetaan [dbcheck-ui-redesign-spec.md](C:/Dev/dBcheck/dbcheck-ui-redesign-spec.md) kokonaisuudessaan yhtenä dark-theme-painotteisena UI-uudistuksena. Kaikki yhdeksän P0-korjausta, uudet mittarin waveform-, idle starfield- ja share card -pinnat sekä määritellyt liikkeet, hapticsit, widget-muutokset ja standardit tilat kuuluvat samaan v1.0-toimitukseen.

Uudistus muuttaa vain esityskerrostا ja sen UI-state-adaptereita:

- Nykyiset mittausalgoritmit, dB-luokittelurajat, audio-, repository-, Room-, billing-, Pro-gate-, backup-, Health Connect- ja export-laskennat säilyvät.
- Nykyinen viiden top-level-kohteen järjestys Meter → Trends → Hearing → History → Settings, reitit ja back stack -omistus säilyvät.
- Theme-asetuksen toimintaan ei kosketa. Tarkka uusi visuaalinen sopimus koskee dark themea; light theme pidetään toimivana regressiopolku­na, mutta sitä ei redesignata.
- Uusia kirjastoja tai versiopäivityksiä ei tarvita. Nykyinen Compose UI 1.11.4 tukee `TextAutoSize.StepBased`-ratkaisua, ja nykyinen Material 3 riittää segmented controls-, NavigationBar- ja motion-toteutuksiin.
- PDF-, CSV-, Health Connect- ja hearing-test-share-formaatteja ei redesignata. Meterin nykyinen share-toiminto muuttuu kuvan jakavaksi poluksi.

## 1. Turvallinen toteutuslähtökohta

Nykyinen checkout on haarassa `codex/security-coderabbit-fixes` ja erittäin likainen: UI-tiedostojen lisäksi muutoksia on workflow-, työkalu-, dokumentaatio-, screenshot-reference- ja turvallisuuspoluissa. Toteutusta ei saa aloittaa suoraan tähän työpuuhun.

1. Tallenna ennen toteutusta read-only-baseline:

   - `git status --short --branch`
   - `git rev-parse HEAD`
   - `git diff --stat`
   - `git diff --name-only`
   - untracked-tiedostojen luettelo
   - nykyinen `validateDebugScreenshotTest`-tila ilman baseline-päivitystä

2. Pyydä erikseen lupa isolated worktreen luontiin. Luvan jälkeen:

   - Luo haara `codex/ui-auditory-observatory` nykyisen haaran HEAD-commitista.
   - Älä stashaa, resetoi tai muuta alkuperäistä työpuuta.
   - Tuo uuteen worktreehen vain nykyiset UI-uudistukseen liittyvät keskeneräiset muutokset: `app/src/main/java/.../ui`, widget/share-esityskerros, strings, UI-testit, screenshot-testit ja UI-dokumentaatio.
   - Älä tuo `.github/workflows`, `tools/sonar.ps1`- tai muita turvallisuusmuutoksia UI-haaraan.
   - Vertaa jokainen tuotu UI-hunkki spesifikaatioon: säilytä jo oikein toteutettu työ, muokkaa ristiriitainen kohta pienimmällä korjauksella ja poista korvautuva kuollut toteutus.

3. Älä tee committeja, pushia tai PR:ää ilman erillistä lupaa. Mahdolliset commit-viestit kirjoitetaan suomeksi.

4. Älä aja `lc`- tai `sc`-wrappereita. Suunnitelmassa mainitut suorat Gradle-tehtävät ovat sallittuja eivätkä tarkoita `reports/`-tiedostojen päivittämistä.

## 2. Sisäiset rajapinnat ja yhden lähteen malli

Julkisia tietokanta-, domain-, navigation- tai palvelurajapintoja ei muuteta. Seuraavat sisäiset UI-rajapinnat lisätään tai tarkennetaan:

| Rajapinta | Vastuu |
|---|---|
| `DbCheckColorTokens` ja `DbCheckLevelVisuals` | Tarkat foundation-, semantic-, container- ja jatkuvan dB-gradientin värit |
| `DbCheckDimensions`, `GaugeTokens`, `ChartTokens`, `MotionTokens` | Kaikki dp-, sp-, alpha-, geometria-, nopeus- ja animaatioarvot |
| `MagmaColorLut` | 256-värinen lineaarisessa RGB:ssä interpoloitu spectrogram-LUT |
| `UiRuntimePolicy` | `animationsEnabled`, `batterySaverEnabled` ja Low power -tila Compose-puun yhteisenä policy-lähteenä |
| `DbCheckSegmentedControl<T>` | Yhden containerin, liikkuvan valintapillin ja radio-semanticsin segmented control |
| `DbCheckAutoSizeNumericText` | Tabular Space Grotesk -arvot, jotka pienenevät enintään 70 prosenttiin |
| `WaveformSampleUiState(timestampMs, db)` | Meterin UI-tasoinen, aikaleimattu waveform-näyte |
| `WeeklyExposureDayUiState` | Täsmälleen seitsemän kalenteripäivää; puuttuvat mittaukset nullable-arvoina |
| `PeakHoldTracker` ja `RtaPeakHoldTracker` | Testattava 3 s hold + 1,5 dB/s decay ilman mittauslaskennan muuttamista |
| `MeterHapticPolicy` ja `MeterHapticEvent` | 85 dB crossing- ja 100 % dose -tapahtumien rate limit/reset-säännöt |
| `MeterShareCardData` | Nykyinen arvo, luokitus, min/LAeq/max, päiväys ja waveform share-kuvan piirtoa varten |

Nykyinen `NoiseLevel` säilyy luokitusten ja niiden rajojen lähteenä. UI-mäppäys on:

- `QUIET` → quiet
- `NORMAL` → moderate
- `ELEVATED` → loud
- `DANGEROUS` → critical

Jatkuvaa 0–120 dB -gradienttia käytetään vain visuaaliseen interpolointiin. Se ei määritä tai muuta domain-luokitusta.

## 3. Toteutusvaiheet

### Vaihe 1 — Theme-, token- ja runtime-policy-perusta

Keskeiset tiedostot ovat [Color.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/Color.kt), [Type.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/Type.kt), [Spacing.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/Spacing.kt), [Shape.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/Shape.kt), [Motion.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/Motion.kt), [ChartTokens.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/ChartTokens.kt) ja [Theme.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt).

1. Keskitä tarkka dark-palette:

   - `bg/deep #0A0D12`
   - `surface1 #11151C`
   - `surface2 #181E27`
   - `surface3 #212936`
   - hairline `#FFFFFF` 8 %
   - focus `#FFFFFF` 16 %
   - primary text `#ECEFF4`
   - secondary text `#9BA3B0`
   - muted `#5C6470`
   - primary `#34D399`
   - onPrimary `#06281B`
   - primaryContainer `#123B2C`
   - error `#F87171`

2. Toteuta yksi semantic level -asteikko ja container-värit:

   - Quiet `#34D399` / `#122B22`
   - Moderate `#FBBF24` / `#2E2712`
   - Loud `#FB923C` / `#2F2114`
   - Critical `#F87171` / `#301717`
   - Jatkuvat pysäkit: 0/55 green, 70 amber, 85 orange, 100/120 red.
   - Interpolointi tapahtuu lineaarisessa RGB:ssä ja clampaantuu välille 0–120.

3. Toteuta `MagmaColorLut` 256 alkiolla. Ankkurit ovat täsmälleen:

   - 0.000 `#000004`
   - 0.125 `#140E36`
   - 0.250 `#3B0F70`
   - 0.375 `#641A80`
   - 0.500 `#8C2981`
   - 0.625 `#B73779`
   - 0.750 `#DE4968`
   - 0.875 `#F7705C`
   - 0.950 `#FE9F6D`
   - 1.000 `#FCFDBF`

4. Vaihda dark-typography:

   - Space Grotesk 400/500/700: display-numerot, suuret arvot, wordmark ja tab labels.
   - Roboto/system default: muu teksti.
   - Gauge 64sp/700, stat 28sp/500, screen title 24sp/500, card title 16sp/500, section label 11sp/500 + 1.5sp letter spacing, body 14/20sp, emphasis 14sp/500, caption 12sp.
   - Lisää `"tnum"` kaikkiin numeerisiin tyyleihin ja numeerisiin caption-variantteihin.
   - Säilytä fonttien nykyinen OFL-lisenssitiedosto ja sen testi.

5. Toteuta spacing/shape-sopimus:

   - Sallitut perusvälit 4, 8, 12, 16, 20, 24, 32 ja 40dp.
   - Screen padding 20dp, card padding 20dp, card gap 12dp, section gap 32dp.
   - Card radius 20dp, tile 14dp, chip/button täysin pyöristetty, sheet/dialog top radius 28dp.
   - Card `surface1` + 1dp hairline; nested tile `surface2` ilman borderia.
   - Interaktiivinen minimikoko 48dp.

6. Lisää tarkat motion-tokenit:

   - track spring 0.90/120
   - snappy spring 0.80/380
   - gentle spring 1.0/200
   - digit roll 160ms emphasized easing
   - REC pulse 1200ms, alpha 1.0↔0.45
   - screen transition 220ms
   - idle breathe 3000ms, scale 1.0↔1.015
   - classification crossfade 180ms
   - starfield fade-out 300ms

7. Lisää `UiRuntimePolicy` appin juureen:

   - `ValueAnimator.areAnimatorsEnabled()` luetaan käynnistyksessä ja jokaisella lifecycle-resumella.
   - `PowerManager.isPowerSaveMode` luetaan käynnistyksessä ja päivitetään `ACTION_POWER_SAVE_MODE_CHANGED`-broadcastista.
   - Jatkuvat loopit ovat sallittuja vain, kun animaatiot ovat päällä eikä kyseinen komponentti ole Low power -poikkeuksen piirissä.
   - Reduced motionissa springs/rolls/crossfades siirtyvät suoraan lopputilaan tai käyttävät yhden lyhyen faden; shimmer, pulse, breathe ja starfield eivät looppaudu.
   - Virallisen Android API:n mukaan `ValueAnimator.areAnimatorsEnabled()` huomioi järjestelmätason animaatioiden poiston ja battery saver -tilan. [Android ValueAnimator](https://developer.android.com/reference/android/animation/ValueAnimator), [PowerManager](https://developer.android.com/reference/android/os/PowerManager.html).

8. Toteuta root-tausta `DbCheckBackground`-komponentilla:

   - top-center radial gradient `#0D1118` → `#0A0D12`
   - gradientin säde/raja 60 % ruudun korkeudesta
   - käytetään kaikkien dark-screenien juurena
   - ei dynaamista Material You -palettia oletuksena

9. Lukitse käyttäjän valitsema kontrastipolitiikka:

   - `#5C6470` säilyy muted-tokenina, mutta sitä käytetään vain koristeisiin, disabled-tiloihin ja ei-informatiivisiin glyph-elementteihin.
   - Informatiivinen 11–12sp teksti, kuten akselit, ajat, yksiköt, disclaimerit ja luettavat helperit, käyttää vähintään `#9BA3B0`-väriä.
   - Lisää automaattinen kontrastitesti, joka vaatii normaalilta tekstiltä vähintään 4,5:1 ja suurelta tekstiltä vähintään 3:1 todellista taustaa vasten.

Vaiheen testit:

- Palette-, interpolation-, typography-, shape-, Magma-anchor- ja contrast-unit-testit.
- Motion policyn testit normal/reduced/battery-saver-yhdistelmille.
- Dark theme -component screenshotit.
- Light theme -regressiotesti varmistamaan, ettei asetuksen toiminta muutu.

### Vaihe 2 — Yhteiset komponentit, app chrome ja standardit tilat

Päivitä `C:\Dev\dBcheck\app\src\main\java\com\dbcheck\app\ui\components` kokonaisuutena ennen screen-kohtaisia muutoksia.

1. `DbCheckCard`:

   - oletus `surface1`, 20dp radius, 1dp hairline, 20dp padding
   - nested-variantti `surface2`, 14dp radius, ei borderia
   - poistetaan paikalliset surface/shape/padding-yhdistelmät kutsujilta

2. `DbCheckButton`:

   - Primary: green/onPrimary, 52dp normaalikorkeus
   - Secondary: transparent + 1dp focus-border + primary text
   - Disabled: surface2 + muted, ei borderia
   - Meterin hero FAB: 64dp
   - Side control: 48dp
   - pressed-tila morphaa ympyrästä rounded-squareksi snappy-springillä; reduced motionissa muoto vaihtuu heti

3. `DbCheckChip`:

   - sisältöön skaalautuva leveys
   - 48dp kosketusalue
   - selected container/primary
   - badge- ja section-label-variantit erikseen
   - ei `weight`-pohjaista pakotettua lyhentämistä

4. Lisää `DbCheckSegmentedControl<T>`:

   - yksi surface2-container
   - tasa- tai sisältöleveyksiset segmentit käyttöpaikan mukaan
   - valittu primaryContainer/primary
   - valintapilli liikkuu snappy-springillä
   - `Role.RadioButton`, valittu-state ja yksittäiset content descriptionit
   - horizontal scroll, jos koko sisältö ei mahdu; tekstiä ei koskaan katkaista
   - käytetään Meter-moodissa, ear selectorissa, refresh-ratessa ja dark/light-asetuksessa

5. `DbCheckSlider`:

   - 6dp track
   - 16dp thumb + 4dp inner dot
   - primary active track
   - arvolabel näkyy thumbin yläpuolella vain drag-interaktion aikana
   - säilyttää nykyiset min/max-, step- ja callback-sopimukset

6. `DbCheckAutoSizeNumericText`:

   - toteutetaan `BasicText` + `TextAutoSize.StepBased` -ratkaisuna
   - max font size tulee typography-tokenista
   - min font size on täsmälleen 70 % max-koosta
   - step 0.25sp
   - arvo yrittää pienentyä ennen ellipsiä
   - API on nykyisessä Compose Foundationissa vakaa: [TextAutoSize](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/TextAutoSize).

7. Standardit data-tilat:

   - Empty: 32dp icon, emphasis title, yhden rivin body, optional text CTA, max width 280dp.
   - Loading: lopullisen layoutin muotoinen skeleton, surface2/surface3, 1200ms; reduced motionissa staattinen base.
   - No measurement: aina `–`, ei `0`, `--`, blankkia tai vihreää placeholderia.
   - Error: error-tinted icon, selkokielinen viesti, retry.
   - No matching filters: sama EmptyState + Clear filters -text button.
   - Pitkä otsikko: max kaksi riviä ja vasta sen jälkeen ellipsis.

8. `DbCheckTopAppBar`:

   - top-level: 24dp brand glyph + 24sp screen title
   - pushed routes: back arrow + max kahden rivin title
   - poista ylisuuri logo/title-lockup

9. Navigation:

   - Säilytä nykyiset route- ja back stack -policyt.
   - Bottom bar käyttää Material 3 `NavigationBar`-rakennetta surface1-taustalla ja hairline-top-borderilla.
   - Default-indicator tehdään läpinäkyväksi ja yhteinen mitattu indicator-pill liikkuu kohteiden välillä snappy-springillä.
   - Aktiivinen icon/label primary; muut muted/decorative ja informatiiviselle labelille kontrastin täyttävä secondary tarvittaessa.
   - Vähintään 600dp rail saa vastaavan active-indicator- ja token-päivityksen.
   - Top-level fade-through: outgoing fade 90ms, incoming fade 130ms 90ms viiveellä, yhteensä 220ms.
   - Ambient- ja Pitch profile -drill-in käyttää 220ms horizontal shared-axis -liikettä + fadea; back on käänteinen.
   - Reduced motionissa siirtymä on instant tai yksi fade.

10. System bars:

   - edge-to-edge säilyy
   - dark theme: transparent bars + vaaleat iconit
   - light theme: nykyinen icon-policy säilyy luettavana
   - screenit käyttävät samoja inset-helppereitä, eivät paikallisia status bar -paddingeja

Vaiheen testit:

- Card-, button-, chip-, segmented-control-, slider-, EmptyState-, auto-size-, top bar- ja navigation-semantics-testit.
- 360dp screenshotit pitkille chip- ja title-teksteille.
- Vähintään 600dp screenshot navigation railille.
- Touch target -sopimustestit kaikille yhteisille interaktioille.

### Vaihe 3 — Yhdeksän P0-korjausta lukittuna erilliseksi portiksi

P0:t toteutetaan yhteisen foundationin jälkeen ja jokaiselle lisätään oma regressiotesti:

1. Trendsin valkoinen/tyhjä viikkokaavio korvataan teemallisella seitsemän pylvään kaaviolla tai standardilla empty-statella.
2. Historyn yhden pisteen kaavio ei piirrä irrallista pistettä; alle kaksi pistettä näyttää täsmälleen määritellyn empty-tekstin.
3. `Spectrogr` poistuu: Spectrogram-label näkyy kokonaan scrollattavassa sisältöleveyksisessä rivissä.
4. REC irrotetaan scrollattavasta sisällöstä ja ankkuroidaan Meter-viewportin gauge-alueen oikeaan yläkulmaan.
5. Dosimeter saa symmetrisen kaksipalstaisen layoutin ja neljä olemassa olevaa stat-arvoa.
6. Environment Mix käyttää neljää erillistä semantic bucket -väriä.
7. Tinnitus-sivun otsikoksi tulee `Pitch profile`; pitkä nimi säilyy body copyssa.
8. Ennen ensimmäistä mittausta kaikki numeeriset mittausarvot näyttävät `–`.
9. `+0%` käyttää neutral/secondary-väriä; warning-väriä käytetään vain merkitykselliselle poikkeamalle.

P0-portti hyväksytään vasta, kun kaikki yhdeksän näkyvät erillisissä screenshot- tai device-skenaarioissa. Dark Mode -asetuksen logiikkaan ei kosketa P0-korjauksena.

### Vaihe 4 — Meterin UI-state, gauge, waveform ja layout

Keskeiset tiedostot ovat [MeterViewModel.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt), [MeterUiState.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt), [MeterScreen.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt) ja `C:\Dev\dBcheck\app\src\main\java\com\dbcheck\app\ui\meter\components`.

#### 4.1 UI-dataflow

1. Säilytä audio-engine ja mittauslaskenta ennallaan.
2. Erota viimeisin `weightedDb` waveformin UI-kellosta:

   - normal/high: UI-bin lisätään 50ms välein eli enintään 20fps
   - Low power: 200ms välein eli 5fps
   - jos audio-engine ei tuota uutta lukemaa 50ms sisällä, käytetään viimeisintä olemassa olevaa RMS/dB-lukemaa uudessa UI-binissä
   - kyse on sample-and-hold-esityksestä, ei uudesta RMS-, FFT- tai audio-prosessoinnista
   - bufferi tyhjennetään session reset/start-rajojen mukaisesti ja ei persistoidu

3. `MeterUiState` saa:

   - `waveformSamples`
   - `peakHoldDb`
   - selkeän `hasMeasurement = sampleCount > 0` -tilan
   - nykyiset min/LAeq/max/session metadata -kentät säilyvät

4. Nykyinen raw peak-amplitudeen perustuva `WaveformVisualization` poistetaan Meterin renderipuusta. Jos sillä ei ole muita kutsujia, poista tiedosto ja testit samassa muutoksessa.

#### 4.2 Gauge

Korvaa [CircularGauge.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/meter/components/CircularGauge.kt) kokonaan:

- Diameter `min(availableWidth - 64dp, 320dp)`.
- 270° arc, start 135°, scale 0–120.
- Track 14dp, rounded cap, white 6 %.
- Value arc 14dp ja scaleen lukittu sweep gradient; 90 dB pysyy oranssissa riippumatta nykyarvosta.
- Tip glow 28dp radius, tipin väri, 35 % max alpha.
- Measuring-glow alpha `0.20 + clamp(db/120) * 0.40`; Low powerissa ambient-reactivity pois.
- Major ticks 20 dB välein 2×10dp.
- Minor ticks 5 dB välein 1×5dp ja 50 % muted alpha.
- Labels 0/20/40/60/80/100/120, tabular caption.
- `PeakHoldTracker`:

  - uusi huippu hyppää heti
  - hold 3000ms
  - decay 1,5 dB/s kohti nykyarvoa
  - ei laske nykyarvon alle
  - reset uuden session/resetin yhteydessä
  - session varsinainen `maxDb` säilyy erillisenä eikä decaya

- Aktiivinen arvo: integer, 64sp Space Grotesk Bold, 160ms vertical slide+fade per muuttuva digit.
- `dB` samalla baseline-linjalla secondary-captionina.
- Luokitusbadge käyttää domainin nykyistä labelia ja semantic container/text -värejä; bucket-vaihdossa 180ms crossfade.
- Idle:

  - faint 0-arc
  - mic-off 28dp
  - `Ready`
  - ei gauge-sisäistä CTA-tekstiä
  - 3000ms breathe vain, jos animaatiot sallittu

Gauge-geometria erotetaan puhtaiksi funktioiksi: angle↔dB, tick endpoints, tip center, label anchors ja gradient stops. Ne testataan ilman Compose-runtimea.

#### 4.3 Live waveform

Lisää `LiveWaveformStrip`:

- 64dp korkea, full content width, ei card-taustaa.
- 2dp bar + 2dp gap.
- Uusin oikealla, vanhin vasemmalla.
- Korkeus lineaarisesti 4–64dp clamped 0–120 dB:stä.
- Barin väri `DbCheckLevelVisuals.colorAtDb(sample.db)`.
- Alpha 1.0 oikealla → 0.25 vasemmalla.
- Idle center line 2dp, 30 % decorative-muted, breathe vain motion-policyllä.
- Leveys määrää bufferista piirrettävien binien määrän; ylimääräiset vanhimmat tiputetaan.
- Semantics kuvaa viimeisimmän dB-arvon ja aikajänteen, ei jokaista baria erillisenä accessibility-elementtinä.

#### 4.4 Meter-layout

Järjestys:

1. standardi header
2. segmented dB Meter / Dosimeter
3. viewporttiin kiinnitetty REC overlay measuring-tilassa
4. gauge
5. waveform
6. technical metadata
7. Live details
8. Sound references
9. fixed control row

REC-pill:

- Ei kuulu LazyColumniin.
- 8dp red dot, 1200ms pulse.
- Tabular elapsed time.
- Pysyy näkyvissä mittauksen ajan eikä voi scrollata headerin alle.
- Reduced motionissa dot on staattinen.

Technical metadata:

- 4 yhtä leveää saraketta ≥360dp.
- Alle 360dp 2×2, kaikilla tileillä sama korkeus.
- WEIGHTING / RESPONSE / SAMPLE RATE / INPUT.
- Tile surface2, 14dp radius, ei borderia.

Live details:

- MIN / LAEQ / MAX 28sp tabular.
- `sampleCount == 0` → `–`.
- Nykyinen sisältörakenne ja laskentalähteet säilyvät.

Controls:

- neljä nykyistä toimintoa säilyvät
- Play/Pause 64dp
- muut 48dp
- idle-only `Start measuring` kontrollirivin alla
- shape morph pressed-tilassa
- callback-, permission- ja session-flow säilyvät

#### 4.5 Dosimeter

- Card title + standard badge.
- Kaksi yhtä leveää palstaa, 12dp väli.
- Vasen 120dp ring, 10dp stroke.
- Dose-värit: 0–50 green, 50–80 amber, 80–100 orange, yli 100 red.
- Keskellä dose % + `DOSE`.
- Oikealla 2×2: TWA, LAEQ, REMAINING, PROJECTED.
- Tile-arvo 20sp Space Grotesk/stat-variantti.
- Ei dataa → `–`, muted ring, ei irrallisia dash-merkkejä.

Meter-vaiheen testit:

- Gradientin stopit ja gauge-geometria.
- Peak hold: new max, 2999/3000ms, decay-nopeus, current floor, reset.
- Waveform: 20fps/5fps, sample-and-hold, järjestys, bufferileikkaus, alpha ja korkeudet.
- Idle/active/no-data/quiet/moderate/loud/critical/peak screenshotit.
- 359dp/360dp metadata-layout.
- REC-scroll-regressio.
- Dosimeterin 0/49/50/79/80/99/100/101 % rajat.

### Vaihe 5 — Trends, raportit, Environment Mix ja History

#### 5.1 Trends-filterit

- Overview/Spectral/Env Mix ja Weekly/Monthly ovat kaksi erillistä horizontal scroll -riviä.
- Chipit ovat sisältöleveyksisiä.
- Valittu tila käyttää primaryContainer/primary.
- Kohteita ei lyhennetä millään leveydellä.

#### 5.2 Seitsemän päivän exposure

`AnalyticsViewModel` normalisoi repositoryn tuloksen UI:ssa seitsemäksi paikalliseksi kalenteripäiväksi:

- päiväjärjestys Mon–Sun
- puuttuva päivä sisältää `avgDb = null`, `maxDb = null`
- puuttuva päivä ei ole 0 dB eikä osallistu viikon keskiarvoon
- domainin nykyiset aggregation- ja hearing-health-laskennat säilyvät

Kaavio:

- seitsemän noin 24dp pylvästä
- 6dp rounded top
- korkeus viikon olemassa olevasta maksimiarvosta
- väri päivän avg-bucketista
- puuttuva päivä käyttää pelkkää baseline/empty slottia
- tänään hairline primary-text outline
- header `Last 7 days` + avg + `AVG dB/DAY`
- barin napautus valitsee päivän `rememberSaveable(dayStartMs)`-tilaan ja näyttää surface3-arvochipin
- tyhjä tai riittämätön data käyttää standardia EmptyStatea

#### 5.3 Raportit ja Environment Mix

Lisää yhteinen `DbCheckDistributionBar`:

- 8dp korkea
- täysin pyöristetty ulkoreuna
- segmentit Quiet/Moderate/Loud/Critical-järjestyksessä
- osuudet normalisoidaan vain piirtoa varten
- tyhjä total näyttää empty/no-data-tilan, ei vihreää 100 % segmenttiä

Käytä samaa komponenttia:

- 12-month Reports cardissa
- Environment Mixin Live-kortissa
- Environment Mixin 7-day-kortissa

Environment row:

- 10dp semantic dot
- body-label
- 20sp tabular percentage
- neljä erillistä väriä

#### 5.4 History

24h chart:

- alle kaksi pistettä → standardi EmptyState tekstillä `Not enough data yet — levels appear here as the day fills in.`
- vähintään kaksi pistettä → 1.5dp primary-line
- area fill primary 18 % → 0 %
- nykyiset aika-akselin labelit säilyvät
- suurin piste 6dp dot + value chip
- headerissa avg 28sp + stable/rising/falling-chip ja →/↑/↓ icon
- nykyinen laskenta tuottaa toistaiseksi stable-tilan; uutta trendialgoritmia ei keksitä

Search/filter:

- search surface2, 14dp, hairline, focus primary
- no matches → EmptyState + Clear filters
- nykyinen search debounce/filter-domain säilyy

Summary:

- arvot 28sp tabular
- prosenttiväritys johdetaan puhtaasta `TrendVisualState.fromPercent`
- 0 → neutral secondary
- positiivinen/negatiivinen saa semantic-varoitusvärin vain nykyisen UI-politiikan osoittamassa merkityksellisessä tilanteessa

Vaiheen testit:

- seitsemän päivän täyttö aikavyöhykkeen, viikonvaihteen ja DST:n yli
- puuttuvat päivät nullableina
- today-outline ja selection
- report/env percentage normalization
- Historyn 0/1/2/N pistettä
- max marker
- `+0%` neutral
- no matches + clear callback
- 360dp dark screenshotit kaikille tiloille

### Vaihe 6 — Spectral “showpiece” -toteutus

Keskeinen tiedosto on [SpectralAnalysisCard.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralAnalysisCard.kt).

1. Mode row:

   - Bars / Spectrogram / RTA kokonaisina labelina
   - scrollattava
   - sisältöön skaalautuva
   - `LIVE CAPTURE` pill + pulsing primary-dot
   - reduced motionissa staattinen dot

2. Bars:

   - 240dp piirtoalue
   - rounded 2dp top
   - hairline baseline
   - labelit 20 Hz / 1 kHz / 20 kHz
   - track-spring korkeuksille
   - nykyinen normalized amplitude muunnetaan vain visualisointia varten asteikolle 0–120 (`amplitude * 120`) ja sen avulla valitaan dB-gradientin väri
   - tätä arvoa ei näytetä dB-lukemana eikä viedä domainiin

3. Spectrogram:

   - 280dp
   - 14dp clip
   - jokainen intensiteetti mapataan 256-väriseen Magma LUTiin
   - frequency labels alapuolella
   - 4dp Magma-legend `quiet` → `loud`
   - spectrogram-bufferin nykyinen live-only- ja max-row-politiikka säilyy

4. RTA:

   - 240dp
   - sama visual-level-mäppäys kuin Barsissa
   - per-band `RtaPeakHoldTracker`
   - peak line 2dp, primary text 80 %
   - hold 3s ja decay 1,5 visual-dB/s
   - tracker käyttää frame timestampia ja nollautuu resolution/band-listan vaihtuessa
   - kuusi yhtä korkeaa tileä kahdessa sarakkeessa: PEAK, BANDS, DOMINANT, BANDWIDTH, PEAK BAND, STATUS

5. Poista kiinteät min-heightit tai spacerit, jotka luovat cardin alle tyhjää aluetta. Cardin korkeus määräytyy valitun moodin sisällöstä ja section gap on 32dp.

Testit:

- Magma LUT 256, kaikki ankkurit ja väli-interpolointi.
- Normalized visual-level clamp 0/1 ja NaN-suojaus.
- RTA peak-hold per band, timestamp- ja reset-skenaariot.
- Bars/Spectrogram/RTA live/idle/locked/error screenshotit.
- `Spectrogram`-label kokonaisena 360dp leveydessä.
- Spectrogramin renderöity kuva ei sisällä vanhaa gray/primary-tertiary-gradienttia.

### Vaihe 7 — Hearing, Pitch profile, Ambient ja Settings

#### Hearing

- Hero käyttää 20sp Space Grotesk -titlea, bodya ja ainoaa 52dp filled-primary CTA:ta.
- Recovery, Tinnitus, Voice Baseline ja muut supporting cardit käyttävät standard cardia.
- Supporting CTAt ovat outlined.
- Disabled CTA surface2/muted ilman borderia.
- Avg/Largest change -placeholder on `–`; alla `Awaiting baseline`.
- Nykyiset Pro-, recording- ja Sound Detection -gatet säilyvät.

#### Pitch profile

- Pushed header + `Pitch profile`.
- Pitkä `Personal tracking pitch...` säilyy body-selityksessä.
- Ear selector yhteisellä segmented controlilla.
- Frequency/volume yhteisellä sliderilla.
- Personal tracking -disclaimer secondary-kontrastivärillä, koska se on informatiivinen 12sp teksti.
- Reitit, playback ja tallennus eivät muutu.

#### Ambient sound

- Sound type- ja timer-chipit omissa scrollattavissa riveissä.
- Play primary ja Stop outlined, molemmat 52dp ja yhtä leveät.
- Nykyiset audio lifecycle-, Pro- ja route-gatet säilyvät.

#### Settings / Display

- Ryhmät standard cardeiksi.
- M3 Switch käyttää primary checked -tilaa.
- Refresh rate ja Dark/Light yhteisellä segmented controlilla.
- Nykyinen tallennus-, effective theme- ja force-dark-debug-logiikka säilyy täysin.
- Muiden Settings-childien dialogit, lock-previewt ja transient message -omistus säilyvät.

#### Muut screenit

Tee token/state/overflow-pass myös niille screeneille, joille dokumentti ei määritä uutta rakennetta:

- Session Detail
- Hearing test setup/active/results
- Hearing recovery
- Sleep setup/results
- Camera overlay
- kaikki Settings-childit
- Health Connect disclosure
- locked/loading/error-previewt

Näissä ei keksitä uutta informaatiota tai flow’ta. Korvataan vain vanhat paikalliset värit, typografia, cardit, napit, välit, placeholderit ja state-rakenteet yhteisillä tokeneilla.

### Vaihe 8 — Idle starfield ja haptics

#### Idle starfield

Lisää Canvas-pohjainen `IdleStarfield` gauge-taustalle:

- vakioitu seed, jotta screenshotit ovat deterministisiä
- noin 60 partikkelia
- koko 1–2dp
- alpha 5–15 %
- drift enintään 4dp/s
- twinkle-periodi deterministisesti 4–8s
- renderöinti enintään 30fps
- fade-out 300ms mittauksen alkaessa
- kokonaan pois, jos:

  - Refresh Rate = Low power
  - battery saver on
  - system animations pois

- screenshot/previews käyttävät pysäytettyä clockia

#### Haptics

Korvaa nykyiset yleiset new-max-hapticsit määritellyllä policyllä:

- 85 dB upward crossing:

  - `previous < 85 && current >= 85`
  - vain aktiivisessa mittauksessa
  - max kerran 10 sekunnissa
  - ei laukea jatkuvasti rajan yläpuolella

- dose 100 %:

  - laukeaa kerran, kun dose ylittää alhaalta 100 %
  - double tick 80ms välein
  - reset uuden session/resetin yhteydessä

- UI-event käsitellään näkyvässä Meterissä `View.performHapticFeedback`-kutsulla ilman `IGNORE_GLOBAL_SETTING`-flagia:

  - API 30+: `CONFIRM`
  - API 26–29: `CONTEXT_CLICK`
  - dose tekee kaksi samaa kutsua
  - tarkistetaan `view.isHapticFeedbackEnabled`

Tämä käyttää Androidin suosittelemaa View-pohjaista APIa, joka kunnioittaa käyttäjän järjestelmäasetuksia: [Android haptic feedback](https://developer.android.com/develop/ui/views/haptics/haptic-feedback), [HapticFeedbackConstants](https://developer.android.com/reference/android/view/HapticFeedbackConstants.html).

Testit:

- kaikki crossing-, cooldown-, reset- ja dose-skenaariot unit-testeinä
- ei hapticia new max -tapahtumasta
- fyysisellä laitteella system haptics on/off
- ei hapticia pelkässä preview/test-renderissä

### Vaihe 9 — Meter share card ja Glance-widget

#### Share card

Laajenna [ShareResultsGenerator.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt) Meterille:

- Nykyinen Meterin text/plain-share korvataan `image/png`-jaolla.
- `EXTRA_TEXT` säilyttää lyhyen tekstuaalisen yhteenvedon saavutettavuutta ja vastaanottajia varten.
- `MeterShareCardData` syntyy share-napin painallushetkellä:

  - center value = viimeisin nykyinen dB
  - classification = nykyisen arvon domain-luokitus
  - min/LAeq/max = nykyisen session arvot
  - date/time = share-hetki käyttäjän localessa
  - waveform = nykyinen UI-waveform-bufferi
  - ilman mittausnäytteitä käytetään nykyistä UI-error-politiikkaa eikä luoda kuvaa

- Renderöi 1080×1350:

  - deep + subtle gradient
  - glyph + dBcheck vasemmalla ylhäällä
  - päivä/aika oikealla
  - suuri Space Grotesk Bold dB-arvo
  - semantic badge
  - MIN/LAEQ/MAX
  - static gradient-colored waveform
  - `Measured with dBcheck` footer

- Käytä Compose `GraphicsLayer.record` + `toImageBitmap` -polkua ja nykyistä cache/FileProvider/ClipData/read-permission-helperiä. Virallinen Compose graphics API tukee offscreen capturea: [Graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers), [GraphicsLayer](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/layer/GraphicsLayer).
- Älä muuta Session Detail-, Hearing Results-, PDF- tai CSV-share-sopimuksia.

#### Widget

Päivitä [DbCheckWidget.kt](C:/Dev/dBcheck/app/src/main/java/com/dbcheck/app/widget/DbCheckWidget.kt):

- background surface1
- semantic badge ja level-värit samasta token-lähteestä
- dB-numero Space Grotesk tabular
- koska Glance 1.1.1 ei tarjoa samaa custom-fonttipolkua kuin Compose `Text`, renderöi numero läpinäkyväksi bitmapiksi bundled Space Grotesk -fontilla ja näytä `ImageProvider`illa
- muuta widgetin data- tai refresh-politiikkaa vain sen verran, että uusi bitmap päivittyy nykyisten dB/widget-update-eventtien mukana
- ei uusia background workereita

`ExternalBrand` säilyy widgetin, notificationin, camera burn-inin ja jaettavien pintojen adapterina, mutta semantic värit delegoidaan yhteiseen token-lähteeseen. PDF:n printtipaletti jätetään ennalleen.

Testit:

- Share-kuvan koko täsmälleen 1080×1350.
- Pixel-samplet background-, badge- ja level-stop-kohdista.
- FileProvider URI, MIME, `ClipData` ja read flag.
- No-data share estyy.
- Widgetin Quiet/Moderate/Loud/Critical/unknown-tilat.
- Widget font/render-snapshot vähintään kahdessa widget-koossa.
- Sharesheet ja widget tarkistetaan oikealla laitteella.

### Vaihe 10 — Koko sovelluksen state-, token-, overflow- ja accessibility-pass

1. Hae koko tuotannon Compose-koodi:

   - hardcoded `Color(0x...)`
   - `.dp` ja `.sp` composable-funktioiden sisällä
   - paikalliset `spring`, `tween` ja alpha-litteraalit
   - card/surface/shape-yhdistelmät, joille on shared-komponentti
   - `--`, `0 dB` ja blankit placeholderit
   - `maxLines = 1` screen/card-titleissa
   - weighted chip-rivit ja lyhennetyt labelit

2. Siirrä kaikki visuaaliset arvot token-tiedostoihin. Poikkeukset sallitaan vain:

   - tokenien määrittelytiedostoissa
   - puhtaissa preview/test-fixtureissä
   - datasta laskettavissa Canvas-pikseleissä, joille dp-token muunnetaan densityllä

3. Tarkista jokainen data surface:

   - Loading
   - Empty/insufficient
   - Data
   - Error/retry
   - Locked preview
   - No measurement
   - Long content
   - Reduced motion

4. Accessibility:

   - 48dp targetit
   - selkeät Role/state semantics
   - chartille yhdistetty kuvaus ja tärkeät arvot
   - waveform/starfieldin dekoratiiviset elementit eivät täytä accessibility-puuta
   - content description ei toista vieressä olevaa näkyvää tekstiä
   - 360dp, font scale 1.0, 1.3 ja 2.0
   - screen title max kaksi riviä
   - numerot pienenevät enintään 70 prosenttiin ennen ellipsiä
   - kontrastit todellista dark-taustaa vasten

5. Poista samassa vaiheessa kaikki korvautuneet komponentit, importit, formatterit, värihelperit ja duplicate layout -funktiot. Tarkista kaikki kutsujat `rg`:llä ennen poistamista.

## 4. Testaus- ja hyväksyntästrategia

### Automaattiset portit vaiheittain

Kunkin vaiheen jälkeen ajetaan kohdennetut unit-testit. Lopullinen suora tarkistusjärjestys:

1. `.\gradlew.bat :app:testDebugUnitTest`
2. `.\gradlew.bat :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck`
3. `.\gradlew.bat :app:detekt :app:lintDebug`
4. `.\gradlew.bat :app:compileDebugScreenshotTestKotlin`
5. `.\gradlew.bat :app:previewScreenshot`
6. tarkista tuotetut kuvat manuaalisesti spesifikaatiota vasten
7. `.\gradlew.bat :app:validateDebugScreenshotTest`
8. `.\gradlew.bat :app:stabilityCheck`
9. `.\gradlew.bat :app:assembleDebug`

`updateDebugScreenshotTest` ajetaan vasta, kun jokainen muuttunut kuva on tarkastettu. Baselinea ei päivitetä testin vihertämiseksi. Päivityksen jälkeen `validateDebugScreenshotTest` ajetaan uudelleen puhtaan tuloksen vahvistamiseksi.

### Screenshot-matriisi

Vähintään:

- Meter idle, quiet, moderate, loud, critical
- Meter recording + pinned REC
- Meter peak hold
- waveform live/idle/Low power
- Dosimeter no-data/50/80/100/>100 %
- Trends weekly full/partial/empty/selected day
- Reports distribution
- Environment Live/7-day
- Spectral Bars/Spectrogram/RTA/locked/idle
- History 0/1/2/monta pistettä
- History no matches
- Hearing Free/Pro/baseline placeholders
- Pitch profile ja Ambient pitkällä fontilla
- kaikki Settings-childit
- Navigation bottom bar ja rail
- share card
- widget

Dark 360×800 on varsinainen visuaalinen acceptance-koko. Light-kuvat säilyvät regressioina, mutta niitä ei käytetä dark-redesignin visuaalisena tavoitteena.

### Device-matriisi

Oikealla emulaattorilla tai laitteella tarkistetaan:

- compact 360dp
- tavallinen puhelin
- ≥600dp rail-layout
- font scale 1.0/1.3/2.0
- reduced motion
- battery saver
- Low power refresh
- edge-to-edge ja system barit
- haptics järjestelmäasetukset päällä/pois
- Sharesheet URI-lukuoikeus
- widget useassa koossa

Jos oikeaa laitetta/emulaattoria ei ole, kyseiset kohdat raportoidaan eksplisiittisesti `unverified`; screenshot- tai JVM-testiä ei nimetä device-evidenceksi.

### Lopullinen acceptance

Toimitus hyväksytään vain, kun:

- Kaikki yhdeksän P0-kohtaa on todistettu korjatuiksi.
- Gauge sisältää gradientin, glow’n, peak holdin, digit rollin ja idle breathen.
- Waveform käyttää semantic värejä ja oikeita 20/5fps-politiikkoja.
- Spectrogram käyttää Magma LUTia; Bars/RTA käyttävät level-väritystä ja RTA peak holdia.
- Jokainen sound-level-pinta käyttää yhteistä semantic sourcea, lukuun ottamatta nimenomaisesti Magma-spectrogramia.
- No-data-arvot ovat `–`.
- Numerot ovat tabular ja suuret arvot Space Groteskilla.
- Ei jää valkoisia/blankkeja data-alueita.
- Chip-labelit eivät lyhene 360dp leveydessä.
- Motion-policy toimii normal/reduced/battery/Low power -tiloissa.
- Kaikki interaktiiviset kohteet ovat vähintään 48dp.
- Informatiivinen teksti täyttää WCAG AA:n todellista taustaa vasten.
- Widget vastaa uutta visuaalista kieltä.
- Meter active-, Spectrogram- ja Trends-kuvat ovat Play Store -julkaisulaatuisia.

## 5. Dokumentaatio ja arkkitehtuurimerkinnät

Koska yhteinen visual token -lähde, runtime motion policy, waveform-dataflow ja Meter share -polku ovat vastuumuutoksia, päivitä toteutuksen yhteydessä:

- [AGENTS.md](C:/Dev/dBcheck/AGENTS.md)
- [UI-SPEC.md](C:/Dev/dBcheck/UI-SPEC.md)
- [PROJECT.md](C:/Dev/dBcheck/PROJECT.md)
- [memory/MEMORY.md](C:/Dev/dBcheck/memory/MEMORY.md)

Kirjaa vähintään:

- dark Auditory Observatory -tokenien omistus
- `NoiseLevel` domain-lähteen ja `DbCheckLevelVisuals` presentation-vastuun ero
- `UiRuntimePolicy` ja reduced-motion/battery-saver-säännöt
- UI-only waveform sample-and-hold -polku
- Meterin Compose graphics → FileProvider -share-dataflow
- widgetin semantic token- ja Space Grotesk bitmap -polku
- light theme -redesignin tietoinen rajaus

Dokumentaatiomuutokset tehdään pieninä hunkeina nykyisten likaisten dokumenttimuutosten päälle; muuta keskeneräistä sisältöä ei korvata.

## 6. Lukitut oletukset ja rajaukset

- Dark theme on tämän toimituksen tarkka visuaalinen kohde. Light theme pysyy toimivana mutta visuaalisesti ennallaan.
- Käyttäjän valinnan mukaisesti WCAG AA menee pienen informatiivisen tekstin muted-värimäärityksen edelle.
- Nykyiset domain-luokitukset säilyvät nimineen ja rajoineen.
- Spectral/RTA normalized amplitude → 0–120 -muunnosta käytetään vain värin ja pylväskorkeuden visualisointiin; sitä ei esitetä mitattuna dB-arvona.
- Waveformin 50ms UI-bin käyttää viimeisintä olemassa olevaa RMS/dB-lukemaa; audio-engineä tai chunk-kokoa ei muuteta.
- Weekly exposure näyttää aina seitsemän kalenteripäivää ja käsittelee puuttuvan päivän nullina, ei nollana.
- Meter share -kuvan pääarvo on share-hetken viimeisin dB-lukema; min/LAeq/max tulevat nykyisen session state-lähteistä.
- Historyn nykyiseen stable-trendiin ei keksitä uutta analytiikkaa.
- Ei uusia ominaisuuksia spesifikaation waveform-, starfield- ja share-card-pintojen ulkopuolelta.
- Ei Room-migraatiota, uutta persistointia, audio- tai mittausalgoritmien muutosta, reittimuutosta, light-theme-redesignia tai PDF-redesignia.
- Ei commit-, push-, PR- tai alkuperäisen likaisen työpuun mutaatioita ilman erillistä lupaa.
