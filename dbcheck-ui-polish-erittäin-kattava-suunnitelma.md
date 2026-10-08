# dBcheck UI Polish – erittäin kattava toteutussuunnitelma

## 1. Tavoite ja rajaus

Toteutetaan [dbcheck-ui-polish-brief.md](C:/Dev/dBcheck/dbcheck-ui-polish-brief.md) visuaalisena uskottavuus- ja käytettävyyskierroksena nykyisen arkkitehtuurin päälle.

Toteutus ei muuta:

- navigaatiograafia, reittejä tai viittä top-level-kohdetta
- ViewModelien UI-state-rakenteita
- domain-logiikkaa, `NoiseLevel`-rajoja tai dosimetriaa
- audio routingia tai tallennettua laite-ID:tä
- Pro-portteja, Roomia, exportteja, widgettejä tai ilmoituksia
- riippuvuuksia, lockfileja tai verification metadataa

Nykyinen dirty worktree säilytetään. Ennen muokkausta tarkistetaan `git status` ja kohdistetut diffit, eikä mitään resetoida tai siivota. Erityisesti Meterin keskeneräiset Sleep Monitor -kytkennät `MeterScreen.kt`:ssa, `MeterViewModelissa`, `MeterUiStatessa`, `DbCheckNavHostissa`, testeissä ja `PROJECT.md`:ssä integroidaan muutoksiin sellaisinaan.

P0 ja P1 toteutetaan ensin. P2 toteutetaan kokonaan vasta, kun P0/P1 läpäisevät kohdistetut testit ja 360 × 800 dp -screenshot-katselmoinnin. Jos P0/P1:ssa havaitaan regressio, se korjataan ennen P2:een siirtymistä.

## 2. Vahvistetut lähtökohdat ja lukitut päätökset

Koodista vahvistettiin:

- Meterin perusrakenne on jo oikea: scrollaava readout on kiinteän controls-alueen yläpuolella. Vain reunakäsittely puuttuu.
- Kaikki nykyiset sliderit käyttävät jo `DbCheckSlideria`. Erot syntyvät yhteisen Material 3 -sliderin default-thumbista, tickeistä, end stopista ja kutsujien layoutista; ruutukohtaisia slider-toteutuksia ei rakenneta.
- `SettingsChipGroup` käyttää jo `FlowRow`ta. Korjaus kohdistuu Ambientin painotettuihin `Row`-riveihin ja `DbCheckChipin` ellipsis-sopimukseen.
- Audio input -tyyppikartoitus ja routing-fallback ovat jo oikein. Korjaus rajataan UI-esityksen ryhmittelyyn ja valinnan näkyvyyteen.
- Hearing-testin `ActiveTestState.isPlayingTone` on jo olemassa. Pulssi sidotaan siihen ilman playback- tai testiprotokollamuutoksia.
- Navigation rail näyttää jo labelit. Muutos koskee compact bottom baria.
- Nykyinen `UI-SPEC.md` dokumentoi vanhan harmaan gradientin, valitun-only-nav-labelin ja ellipsis-chipin, joten se päivitetään toteutuksen mukana.

Materiaaliympäristö käyttää paikallisesti ratkaistua Material 3 `1.4.0` -versiota. Slider toteutetaan tämän version APIlla. Ratkaisu noudattaa Androidin virallisia [Slider](https://developer.android.com/develop/ui/compose/components/slider)-, [FlowRow](https://developer.android.com/develop/ui/compose/layouts/flow)- ja [ScrollableState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/ScrollableState)-sopimuksia.

### Lukittu väripaletti

Briefin dark-värit hyväksytään sellaisinaan. Light-teeman kaksi mittausväriä säädetään WCAG AA -tekstikontrastin vuoksi:

- `LightLevelElevated`: `#9A7A33` → `#8A6C2D`
- `LightLevelDangerous`: `#B45F5F` → `#A95353`

Alkuperäiset arvot jäävät alle 4.5:1-kontrastin `#FAFAFA`-taustaa vasten. Uudet arvot pysyvät samoissa kulta- ja punasävyissä ja ylittävät rajan. Muut briefin accent- ja level-värit säilyvät.

Dark-level-fillien sisältöväri on `#08120C`; light-level-fillien sisältöväri on valkoinen. `success`, `warning` ja `error` säilyvät erillisinä semanttisina tokeneina, vaikka jokin heksiarvo olisi sama.

Accent saa näkyä vain:

- primary-painikkeissa
- valituissa nav-, chip-, segmented-, slider-, toggle- ja focus-tiloissa
- aktiivisessa REC/LIVE-indikaattorissa

Mitattua äänenvoimakkuutta ilmaisevat pinnat käyttävät erillistä level rampia. Section-titleja, tavallisia ikoneita, listoja, badgeja, yleisiä kaavioita tai korttitaustoja ei väritetä accentilla.

## 3. Sisäisten komponenttirajapintojen muutokset

Ulkoisia tai domain-tason API-muutoksia ei tehdä. Sisäisiä Compose-rajapintoja muutetaan seuraavasti:

- `DbCheckColorScheme` saa `accent`, `accentDim`, `onAccent`, `accentContainer`, `onAccentContainer` ja `NoiseLevelColors`-holderin.
- `NoiseLevelColors` tarjoaa sekä `colorFor(level)`- että `contentColorFor(level)`-haun.
- `DbCheckSlider` saa pakolliset presentation-arvot `valueLabel`, `minLabel` ja `maxLabel`; olemassa olevat range-, steps-, enabled- ja semantics-parametrit säilyvät.
- `DbCheckTopAppBar` tukee kahta eksplisiittistä muotoa: top-level `logo + title` ja pushed `back + title`.
- `DbCheckSetupScaffold` ottaa reittitittelin app baria varten. Mahdollinen phase/description jää varsinaiseen sisältöön ilman toista suurta sivuotsikkoa.
- `CircularGauge` saa eksplisiittisen idle/recording-tiedon, jotta idle-copy, live-lukema ja animaatiot eivät päättele tilaa arvosta `0`.
- `EmptyState` saa optional preview-slotin Trendsin tarkoituksellista no-data-esikatselua varten.
- Audio input -UI saa puhtaan presentation-ryhmittelyn, joka säilyttää ryhmän jäsen-ID:t mutta ei muuta tallennettua valintaa.
- `UiNumberFormatter` keskittää UI-lukemien pisteellisen desimaaliformaatin.

## 4. Toteutusvaiheet

### Vaihe 0 – lähtötilan suojaus ja vertailupinta

- Tallenna tarkastelua varten `git status --short` ja kohdistetut diffit kaikista muutettavista dirty-tiedostoista.
- Älä luo uutta worktreetä, branchia tai committia ilman erillistä valtuutusta. Toteuta nykyiseen checkoutiin, jotta keskeneräiset Sleep-muutokset eivät jää pois.
- Säilytä 20 Pixel 9 -screenshotia visuaalisena ongelmareferenssinä.
- Renderöi nykyiset relevantit screenshot-testit ennen baselinejen muuttamista, jotta erot voidaan luokitella tarkoituksellisiksi.
- Älä aja `lc`- tai `sc`-wrappereita.

### Vaihe 1 – P0: teema, painikkeet ja Meterin scroll-raja

#### Teematokenit

Muuta `ui/theme/Color.kt`, `Theme.kt`, tarvittaessa `Gradient.kt`, `Spacing.kt` ja `Motion.kt`:

- Lisää briefin accent- ja level-tokenit sekä kaksi korjattua light-level-arvoa.
- Mapita Material `primary`, `onPrimary`, `primaryContainer` ja `onPrimaryContainer` accent-rooleihin.
- Lisää level ramp erillisenä holderina; älä aliasoi sitä semantic success/warning/error -väreihin.
- Lisää puuttuvat semanttiset komponenttitokenit, esimerkiksi sliderin pyöreä thumb-koko ja Meterin 28 dp edge fade.
- Pidä kaikki uudet värit, mitat, alfat ja kestot teematiedostoissa.

Koska Material `primary` vaihtuu harmaasta accentiksi, tee koko `ui/`-puun primary-role-audit. Jokainen osuma luokitellaan:

- sallittu interaction role → accent
- äänenvoimakkuus → level ramp
- confirmation/error/warning → semantic token
- tavallinen otsikko, ikoni, badge, yleiskaavio tai koriste → `onSurface`, `onSurfaceVariant`, `secondary`, `ghostBorder` tai tarkoituksenmukainen neutral token

Erityisesti neutralisoidaan app bar -logo, setup-eyebrowt, yleiset chart-linjat, listojen ikonit, standard-badget, camera-koristeet ja status-labelit, jos ne nykyisin nojaavat `primaryyn`.

#### Primary-painike

Muuta `DbCheckButton.kt`:

- Primary-fill on yhtenäinen `accent`.
- Sisältö on `onAccent`.
- Painettu tila käyttää `accentDim`-väriä.
- Disabled-fill on neutral `surfaceContainerHighest` ja sisältö `onSurfaceVariant`.
- `enabled` vaikuttaa sekä klikkaukseen että visuaaliseen tyyliin.
- Secondary- ja tertiary-painikkeet eivät peri accentia painettuun tai idle-tilaansa.
- Lisää enabled/disabled light/dark -component-previewt rinnakkaista vertailua varten.

Poista `signatureGradient`:

- `DbCheckButtonista`
- Meterin primary-controlista
- `ProUpsellCard`-reunasta
- kaikista muista löytyvistä kutsuista

`signatureGradient` jää ainoastaan `CircularGauge`n idle/inactive-trackin hillityksi sweepiksi. Aktiivinen gauge-arc käyttää P1:ssä level rampia.

Lopuksi `rg signatureGradient` saa löytää vain tokenin määrittelyn ja `CircularGauge`-kuluttajan.

#### Meterin scroll-edge

Muuta `MeterScreen.kt` nykyistä rakennetta rikkomatta:

- Kääri scrollaava alue `Boxiin`.
- Näytä alareunassa 28 dp transparent → background -gradientti vain, kun `scrollState.canScrollForward`.
- Näytä yläreunassa background → transparent -gradientti vain, kun `scrollState.canScrollBackward`.
- Fade-overlayt eivät saa kaapata pointer inputia tai accessibility-focusta.
- Lisää fixed `MeterControlsSection`ille `surfaceContainerLowest`-tausta ja `ghostBorder`-värinen hairline-divider.
- Säilytä controls erillisenä layout-lohkona; älä muuta sitä floating/overlay-bariksi.
- Säilytä nykyinen optional `SleepSetupCta` scrollaavassa readoutissa.

Lisää esikatselut:

- collapsed idle, jossa ydinsisältö mahtuu ilman scrollia
- Live details expanded
- Sound Reference expanded
- molemmat expanded
- 360 × 800 dp, light/dark ja 1.3 font scale

### Vaihe 2 – P1: yhteinen interaction-kieli

#### Kaksi header-mallia

Muuta `DbCheckTopAppBar` ja kaikki kutsujat:

Top-level-malli:

- neutral logo mark
- saman rivin screen title
- screenit: Meter, Trends, Hearing, History, Settings
- poista Trendsin, Hearingin ja Settings-hubin erilliset suuret sivuotsikot

Pushed-malli:

- back arrow
- yksi inline route title
- sama koko, padding ja alignment kaikilla pushed-reiteillä

Setup-reiteiltä poistetaan uppercase eyebrow + toinen suuri route-title. Varsinainen vaihe- tai ohjeteksti voi jäädä sisällön alkuun neutral `label`-tyylillä, jos se kertoo testin etenemisestä eikä vain toista reitin nimeä.

Sovella pushed-mallia ainakin:

- tinnitus pitch
- ambient playback
- hearing test setup/active/results
- hearing recovery setup/active/results
- sleep setup
- camera route silloin, kun sen nykyinen shell näyttää app barin

Poista Hearing-hubin otsikon `🎧`-emoji; käytä puhdasta tekstinimeä. Session metadata -emojit säilyvät.

#### Yksi slider

Muuta `DbCheckSlider.kt`:

- conventional, jatkuva track
- pyöreä theme-tokeniin perustuva thumb
- active track `accent`
- inactive track neutral
- stepped snapping säilyy, mutta visuaaliset tick-dots ja Materialin end stop poistetaan 1.4.0-yhteensopivan track-overloadin avulla
- nykyarvo sliderin yläpuolella
- min/max-labelit trackin alapuolella tasattuina päihin
- disabled-tila säilyy selvästi erillisenä
- semantics sisältää nykyarvon ja valittavan alueen

Päivitä kaikki kuusi kutsuryhmää samaan APIin:

- tinnitus: `250 Hz` – `8.0 kHz`
- ambient volume: `5%` – `100%`
- microphone sensitivity: `−10.0 dB` – `+10.0 dB`
- octave calibration: policy-min/max
- notification threshold: nykyiset policy-min/max
- notification schedule: `00:00` – `23:00`

Range ja steps tulevat edelleen nykyisistä policy/default-lähteistä. Niitä ei kopioida UI:hin.

#### Chipit

Muuta `DbCheckChip` ja Ambientin chip-rivit:

- Chipin label pysyy yhdellä rivillä mutta saa luonnollisen leveyden eikä ellipsisoi.
- Ambientin sound preset- ja timer-rivit vaihtuvat `FlowRow`ksi.
- Poista `weight(1f)`-pakotus yksittäisistä chipeistä.
- Käytä yhteisiä horizontal/vertical gap -tokeneita.
- Säilytä täysi nykyinen tekstisisältö.
- Auditoi `SettingsChipGroup`, frequency weighting, notification-dayt ja muut chip-ryhmät default- ja 1.3-fontilla.
- Jos rivi ei mahdu, se wrapataan; vaakasuuntaista leikkausta tai lyhennettyä copya ei hyväksytä.

#### Bottom navigation

Muuta compact `BottomNavBar`:

- Kaikkien viiden kohteen label näkyy aina.
- Kohteet säilyvät tasalevyisinä ja `Role.Tab`-semantiikka säilyy.
- Valittu pill käyttää `accentContaineria`; valittu ikoni ja label käyttävät accentia.
- Valitsemattomat ikonit ja labelit käyttävät `onSurfaceVariantia`.
- Säädä tokenoituja icon-, gap- ja vertical-padding-arvoja niin, että kaikki labelit mahtuvat 360 dp leveyteen ja 1.3-fontilla.
- Älä muuta navigation railia, destination-järjestystä tai reititystä.

### Vaihe 3 – P1: instrumenttidata ja numeroformaatti

#### dB level ramp

Kytke `NoiseLevelColors`:

- `CircularGauge`: aktiivinen arc seuraa nykyistä `NoiseLevelia`
- `NoiseLevelPill`: fill ja luettava content color seuraavat tasoa
- `StatCard`: MIN, LAEQ ja MAX lasketaan ja väritetään kukin omasta arvostaan
- `LiveSoundLevelChart`: line ja area-fill seuraavat viimeisintä tasoa; 85 dB threshold käyttää dangerous-tokenia
- yksittäiset chart-markerit käyttävät niiden omaan arvoon kuuluvaa level-väriä silloin, kun ne ilmaisevat mittausta

Käytä `animateColorAsState`a ja `DbCheckMotion.StateChange`-kestoa. `NoiseLevel.fromDb` ja nykyiset 40/70/85 dB -rajat säilyvät ainoana luokittelulähteenä.

Lisää rajatestit:

- 39.9 → QUIET
- 40.0 → NORMAL
- 69.9 → NORMAL
- 70.0 → ELEVATED
- 84.9 → ELEVATED
- 85.0 → DANGEROUS

Lisää deterministisiin screenshotteihin animaatiot ohittava presentation-parametri, jos nykyinen screenshot-infrastruktuuri sitä tarvitsee.

#### Numeroformaatti

Lisää UI-kerrokseen `UiNumberFormatter`:

- käyttää eksplisiittistä `Locale.US`-desimaaliformaattia
- tarjoaa vähintään kokonais-, yhden desimaalin, etumerkillisen yhden desimaalin, Hz/kHz-, prosentti- ja tarvittavan file-size-formatin
- ei formatoi päivämääriä, kellonaikoja tai käyttäjän metadataa
- ei korvaa CSV/PDF/PNG/export-formaatteja

Korvaa ruutukohtaiset `String.format`, `Locale.getDefault` ja vastaavat decimal-readout-polut Meterissä, Hearingissä, Tinnituksessa, Calibrationissa, Trendsissä, Dosimeterissa, Session Detailissa ja muissa käyttäjälle näkyvissä mittausarvoissa.

Tarvittaessa muuta string-resurssien numeerinen placeholder `%f`:stä `%s`:ksi, jotta resurssi ei formatoi lukua uudelleen laitteen localella. Säilytä default- ja Finnish-resurssien placeholder-pariteetti.

Testaa formatteri ainakin `fi-FI`- ja `en-US`-default-localella. Molempien tulee tuottaa näissä UI-readouteissa pisteellinen desimaali eikä mikään ruutu saa sekoittaa pilkkua ja pistettä.

### Vaihe 4 – P1: audio input -presentation

Lisää puhdas UI-presentation-mapper Calibration-komponenttien yhteyteen:

- Ryhmittelyavain on normalisoitu `productName + AudioInputDeviceType`.
- Saman nimen ja saman tyypin Android-ID:t muodostavat yhden käyttäjälle näkyvän rivin.
- Ryhmä säilyttää kaikki jäsen-ID:t.
- Jos tallennettu selected ID kuuluu ryhmään, rivi näkyy valittuna ja sama ID säilyy.
- Jos valintaa ei ole ja built-in input on saatavilla, built-in-rivi näytetään visuaalisena fallback-valintana ilman preference-writeä.
- Jos built-in inputia ei ole eikä tallennettua valintaa ole, mitään muuta laitetta ei merkitä valituksi.
- Rivin valinta tallentaa ryhmän deterministisen edustaja-ID:n vain käyttäjän eksplisiittisestä napautuksesta.
- Olemassa olevaa validia tallennettua ID:tä ei normalisoida, migroida tai kirjoiteta uudelleen ryhmittelyn vuoksi.

Subtitle määräytyy tyypistä:

- Built-in microphone
- Wired headset
- USB audio
- Bluetooth
- External audio input vain aidosti luokittelemattomalle ulkoiselle tyypille

Älä muuta:

- `AudioInputDeviceRouteResolveria`
- `AndroidAudioInputDeviceDescriptorMapping`-routing-semanticsia
- built-in fallbackia
- DataStore-avainta tai nullable-sopimusta
- audio capture -asetuksia

Lisää unit-testit duplicate-ID-ryhmitykselle, eri tyyppien erottelulle, tallennetun jäsen-ID:n valinnalle, built-in visual fallbackille sekä sille, ettei mapperi tuota preference-writeä.

### Vaihe 5 – P1: purposeful empty states

#### Trends

Laajenna `EmptyState` optional preview-slotilla ja rakenna Trendsille oma no-data-sisältö:

- yksi matalan korostuksen preview-kortti
- placeholderit Weekly exposure, Monthly trend ja Reports
- kaikki tuntemattomat arvot `—`; ei keksittyjä mittauslukuja
- neutral mini-chart scaffold, joka kertoo tulevan sisällön luonteen
- yksi Start Measuring -CTA
- error-tila säilyy erillisenä eikä käytä data-previewtä

Empty-preview käyttää `surfaceContainerLowest`-/subdued-emphasista, ei populated-cardin samaa filliä ja painoa.

#### Hearing

Kun latest hearing test puuttuu:

- älä näytä erillisiä tyhjiä Hearing status- ja Latest test -kortteja
- näytä yksi primary “Start with a baseline hearing test” -kortti
- säilytä Recovery, Tinnitus Pitch, Voice Baseline, Sleep ja Ambient -entryt
- esitä jäljelle jäävät entryt subdued/secondary-korteissa
- säilytä kaikki Pro-, measurement- ja sound-detection-gatet
- poista yksittäisen kortin nimeä toistavat ulkoiset section-headerit
- säilytä Tools-header, koska se ryhmittelee useita eri työkaluja

Kun hearing-dataa on, nykyinen populated status/latest-rakenne säilyy normaalilla emphasis-tasolla.

Päivitä copyt string-resursseihin ja poista Hearing-testin otsikkoemoji.

### Vaihe 6 – P2: Meterin instrumenttinäkymä

Tämä vaihe käynnistyy vasta P0/P1-gaten jälkeen.

#### CircularGauge

- Lisää 0/40/80/120-asteikkolabelit nykyisen `SoundLevelDisplayScale`-koordinaattimallin ympärille.
- Käytä `rememberTextMeasurer`ia ja tokenoitua label-offsetia; älä rakenna rinnakkaista dB-skaalaa.
- Idle/inactive-track käyttää ainoana kuluttajana hillittyä `signatureGradientia`.
- Aktiivinen arc käyttää aina level rampia.
- Renderöi lukema ja `dB` samalle riville, samaan Space Grotesk -perheeseen; unit noin kolmasosa numeron koosta.
- Lisää `"tnum"` display/data-tyyleihin `Type.kt`:ssa, jotta lukeman leveys ei heilu.
- Kun measurement on idle, korvaa `0 dB` mittausohjeella gauge-keskuksessa.
- Kun measurement alkaa, idle-copy poistuu ja live-lukema palaa.
- Poista MeterSessionStatusin erillinen pysyvä “Tap Play…” -kappale, mutta säilytä virheet ja session metadata.

#### Dosimeter idle

`DosimeterGaugeCard.Unavailable` näyttää varsinaisen mittarirakenteen:

- Dose %
- TWA
- Remaining exposure
- kaikissa arvo `—`
- NIOSH REL -badge säilyy mutta neutralisoidaan standardibadgeksi, ei accent-pinnaksi
- ei kuvailevaa idle-paragrafia
- locked Free-preview ja Pro-gate säilyvät ennallaan

### Vaihe 7 – P2: Hearing active ja Camera

#### Hearing tone presence

- Sido pulsing ring `ActiveTestState.isPlayingTone`-arvoon.
- Pulssi alkaa vain tone-playbackin aikana ja pysähtyy heti tilan vaihtuessa.
- Käytä `DbCheckMotion`-tokeneita ja enintään yhtä infinite transitionia.
- Pulssi ei muuta taajuutta, voimakkuutta, tone durationia, Hughson–Westlake-logiikkaa tai napin enabled-tilaa.
- Korvaa nykyinen determinate progress standardilla lineaarisella indikaattorilla.
- Poista Materialin stray end stop 1.4.0-yhteensopivalla no-op stop-indicatorilla.
- Progressin aktiiviväri on neutral `onSurface`; accentia ei käytetä yleisenä etenemisvärinä.

#### Camera bottom control bar

Refaktoroi vain `CameraOverlayRoute`n presentation-shell:

- yhdistä dB-readout, privacy/status-hint ja capture controls yhdeksi bottom bariksi
- yksi surface, yksi radius ja yksi reunalinja
- tasatut paneelireunat
- photo/video/stop-painikkeille sama theme-tokenoitu kosketus- ja visuaalinen koko
- active recording stop voi käyttää semantic error -väriä
- muut aktiiviset capture-toiminnot voivat käyttää primary accentia
- 360 dp leveydellä sisältö saa jakautua kahteen sisäiseen riviin, mutta se pysyy yhden surface-barın sisällä
- säilytä kaikki callbackit, CameraX-binding, permission-state, photo/video-state ja share-flow ennallaan

Tee CameraX:n vuoksi myös oikean laitteen smoke: preview, permission granted, recording start/stop ja unavailable/denied shell. Muutos ei saa aloittaa audiota tai mittaussessiota.

## 5. Testi- ja hyväksyntäsuunnitelma

### Unit- ja contract-testit

Lisää tai päivitä:

- theme resource -testi uusille accent- ja level-arvoille sekä contrast-ratioille
- `NoiseLevelColors`-boundary-testit 40/70/85 dB
- `UiNumberFormatterTest` vähintään fi-FI/en-US-default-localelle
- audio input presentation grouping/selection -testit
- header contract -testit kahdelle sallitulle mallille
- `MeterScreenLayoutContractTest` fade- ja fixed controls -rakenteelle, Sleep-muutokset säilyttäen
- button contract/screenshot enabled- ja disabled-stateille
- chip/slider/bottom-nav source- ja semantics-testit
- Hearing empty/onboarding -contract
- Camera bottom-bar -contract
- localization placeholder -pariteettitestit

Vanha `ProfessionalMonochromeThemeResourceTest` nimetään uudelleen vastaamaan accent-instrument-teemaa, jos se edelleen olettaa täysin monokromaattisen primaryn.

### Screenshot-matriisi

Kaikki kriittiset pinnat renderöidään koossa 360 × 800 dp:

| Pinta | Light/Dark | Default | 1.3 font | Erityistilat |
|---|---:|---:|---:|---|
| Primary button | kyllä | kyllä | kyllä | enabled, pressed-preview, disabled |
| Meter | kyllä | kyllä | kyllä | idle, recording, live expanded, reference expanded, dosimeter unavailable |
| Level ramp | kyllä | kyllä | ei pakollinen | 39.9, 40, 69.9, 70, 84.9, 85 dB |
| Trends | kyllä | kyllä | kyllä | empty, error, populated smoke |
| Hearing | kyllä | kyllä | kyllä | onboarding, populated, Free/Pro |
| Bottom nav | kyllä | kyllä | kyllä | kaikki viisi labelia |
| Slider | kyllä | kyllä | kyllä | tinnitus, ambient, calibration, schedule |
| Chips | kyllä | kyllä | kyllä | ambient presets/timer, weighting, schedule |
| Calibration input | kyllä | kyllä | kyllä | duplicates grouped, selected fallback |
| Hearing active | kyllä | kyllä | kyllä | tone off/on, progress 1/12 |
| Camera bar | kyllä | kyllä | kyllä | idle, recording, error/disabled controls |
| Headers | kyllä | kyllä | kyllä | top-level ja pushed |

Baseline-PNG:t päivitetään vasta, kun jokainen diff on katsottu ja todettu briefin mukaiseksi. Baseline update ei ole virheen ohitus.

### Manuaalinen hyväksyntä

Pixel 9 / 360 × 800 dp, portrait:

- dark ja light
- default ja 1.3 font scale
- kaikki viisi nav-labelia ilman leikkausta
- yksikään chip ei ellipsisoi
- sliderit näyttävät samalta ja min/max ovat luettavia
- Meter collapsed ei scrollaa
- expanded Meter näyttää oikean top/bottom-faden
- level-värit vaihtuvat yhdessä rajojen yli
- audio input -listalla ei ole käyttäjälle erottamattomia rivejä
- TalkBack lukee navin, sliderin, chipin, audiolaitteen ja expanded-cardin tilan
- Camera bottom bar toimii oikean previewn päällä
- touch targetit ovat vähintään 48 dp

Lisäksi yksi ≥600 dp smoke varmistaa, ettei rail-layout muuttunut.

### Laatugatet

Aja kohdistetut testit ensin ja sen jälkeen:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:previewScreenshot
.\gradlew.bat :app:validateDebugScreenshotTest
.\gradlew.bat :app:ktlintMainSourceSetCheck
.\gradlew.bat :app:ktlintTestSourceSetCheck
.\gradlew.bat :app:detekt
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:stabilityCheck
.\gradlew.bat :app:assembleDebug
```

Jos hyväksytyt screenshotit muuttuvat tarkoituksellisesti, aja vasta visuaalisen katselmoinnin jälkeen:

```powershell
.\gradlew.bat :app:updateDebugScreenshotTest
.\gradlew.bat :app:validateDebugScreenshotTest
```

Lopulliset source-auditit:

- `rg signatureGradient` → vain token + `CircularGauge`
- primary/accent-osumat → vain hyväksytyt interaction-roolit
- `rg` uusille inline `Color(...)`, `dp`, radius-, alpha- ja duration-literal-arvoille `ui/`-puussa → ei uusia screen-/component-tason design-arvoja
- kaikki slider-kutsut → `DbCheckSlider`
- kaikki chip-ryhmät → ei ellipsis-/painotettua leikkausta
- ei uusia riippuvuuksia tai Gradle-muutoksia

## 6. Dokumentointi ja valmistumisen raportointi

Päivitä toteutuksen jälkeen kirurgisesti:

- `UI-SPEC.md`: uusi accent/level-palette, gradientin ainoa käyttökohde, painike-, header-, slider-, chip-, nav-, empty-state-, Meter-, Hearing- ja Camera-sopimukset sekä uusi screenshot-matriisi
- `PROJECT.md`: vain design system-, UI state- ja testimääräosuudet, jotka muuttuivat; säilytä nykyiset dirty Sleep/Camera/QA-päivitykset

Älä päivitä `AGENTS.md`:ää tai `memory/MEMORY.md`:ää, koska navigation, dataflow, moduuliomistus ja arkkitehtuuri eivät muutu.

Valmistumisraportissa eritellään:

- täyttyikö jokainen briefin 12 hyväksymiskriteeriä
- kaksi WCAG:n vuoksi muutettua light-level-heksiä ja perustelu
- kaikki poistetut `signatureGradient`-kuluttajat
- screenshot-baselinet, jotka muuttuivat tarkoituksellisesti
- ajetut komennot ja niiden tulokset
- mahdolliset täyttymättömät kriteerit ilman kaunistelua
- nykyiseen dirty worktreehen kuuluneet käyttäjän muutokset, jotka säilytettiin

## 7. Oletukset

- Toteutetaan koko brief, myös P2, kun P0/P1-gate on vihreä.
- English-first UI-readout käyttää pisteellistä desimaalia myös Finnish-device-localella; tämä ei ole koko sovelluksen Finnish-localisointi.
- Audio input -deduplikointi on presentation-only eikä kirjoita preferenceä ilman käyttäjän valintaa.
- Trends-preview ei esitä keksittyä dataa.
- Hearingin entry pointteja tai Pro-gateja ei poisteta.
- Uusia kirjastoja, adaptive navigationia tai theme-arkkitehtuuria ei lisätä.
- Commit, push ja PR eivät kuulu toteutukseen ilman erillistä pyyntöä.
