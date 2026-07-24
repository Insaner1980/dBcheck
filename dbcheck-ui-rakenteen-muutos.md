# dBcheckin UI-rakenteen uudistus

## Yhteenveto

Toteutus yhdistää [Fable-suunnitelman](C:\Dev\dBcheck\dbcheck-ui-restructure-plan.md), 21 käyttäjän toimittamaa kuvakaappausta ja nykyisen Compose-koodin. Muutos järjestää olemassa olevat ominaisuudet uudelleen, mutta ei muuta mittaus-, tallennus-, Pro-entitlement-, tietokanta-, export-, backup-, Health Connect- tai notification-logiikkaa.

Uusi päärakenne on **Meter · Trends · Hearing · History · Settings**. Tämä on Androidin nykyisen 3–5 tasavertaisen kohteen [navigation bar -ohjeen](https://developer.android.com/develop/ui/compose/components/navigation-bar) mukainen. Nykyinen Material 3 -teema, tokenit ja shared-komponentit säilyvät.

## Toteutusmuutokset

### 1. Navigaatio ja vastuualueet

- Lisää top-level `Screen.Hearing` reitillä `hearing`. Bottom barin ja navigation railin järjestys on Meter, Trends, Hearing, History, Settings.
- Säilytä Analyticsin sisäinen reitti `analytics`, package ja ViewModel-nimi yhteensopivuuden vuoksi, mutta vaihda kaikki käyttäjälle näkyvät nimet muotoon **Trends**.
- Poista Meterin, Trendsin ja Historyn oikean yläkulman Settings/Profile-oikotiet. Settingsiin kuljetaan yhdestä johdonmukaisesta bottom bar/rail -kohteesta.
- Kaikki Hearing Test-, Recovery-, Tinnitus-, Ambient- ja Sleep-reitit säilyvät non-top-level-reitteinä nykyisine Pro-route- ja execution-gateineen. Niiden paluunavigointi ohjataan jatkossa Hearing-hubiin Analyticsin sijasta.
- Laajenna top-level route -politiikka tunnistamaan Hearing sekä kaikki `settings/...`-alisivut. Top-level-kohteen uudelleenvalinta palauttaa kyseisen pinon juureen.
- Säilytä nykyinen compact bar / ≥600 dp rail -ratkaisu; uutta adaptive navigation -riippuvuutta ei lisätä.

### 2. Meter, Trends, Hearing ja History

**Meter**

- Poista Sleep Monitor -kortti Meteristä ja sen Meter-kohtainen UI-state; varsinainen Sleep-toiminto ja näkyvyysasetus säilyvät.
- Järjestys on mode-valinta, session tila, mittari, valitun moden dosimeter/live-yhteenveto, tiiviit tilastot, Sound Reference ja kiinteät mittauskontrollit.
- Säilytä nykyinen live-kaavio ja aaltomuoto kompaktin, oletuksena suljetun **Live details** -rivin takana. Avattuna sisältö saa vierittyä; suljettuna tavallisen 360 × 800 dp puhelimen ydinnäkymä mahtuu yhteen ruutuun.
- Muuta Sound Reference suljettuna yhdeksi vähintään 48 dp:n riviksi: lähin vertailu, sen dB-arvo, nykyinen arvo ja avausikoni. Avattuna näytetään nykyinen rail ja koko vertailulista.
- Näytä idle-tilassa lyhyt mittauksen käynnistämiseen ohjaava teksti ilman uutta CTA:ta; kiinteä Play-painike säilyy ensisijaisena toimintona.

**Trends**

- Overview sisältää vain mittausdataa: exposure-yhteenveto, weekly/monthly-valinta, trendit, yearly report sekä kompakti Hearing status -rivi, joka avaa Hearing-hubin.
- Spectral ja Environment säilyvät nykyisine live-, empty-, error- ja Pro-tiloineen.
- Poista Trendsin varsinaisesta korttilistasta Hearing Test, Recovery, Tinnitus, Ambient ja Sleep.
- Kevennä `AnalyticsViewModel` poistamalla siitä recovery-, tinnitus- ja sleep-omistus. Erota nykyinen hearing-health-laskenta yhteiseksi puhtaaksi laskuriksi, jotta Trends ja Hearing eivät kopioi viikkokeskiarvo-, today-vs-week- tai statuslogiikkaa. Nykyiset raja-arvot säilyvät muuttumattomina, ja datattomalle tilalle ei tuoteta virheellistä SAFE-statusta.

**Hearing**

- Lisää `HearingScreen`, `HearingViewModel`, `HearingUiState` ja `HearingScreenActions`.
- Sisältöjärjestys:
  1. Hearing status ja viimeisin vertailu
  2. Check your hearing
  3. Hearing recovery
  4. Tinnitus pitch
  5. Voice baseline
  6. Tools: Sleep Monitor ja Ambient Sounds
- Siirrä hearing/tool-komponentit Analytics-paketista Hearing-vastuualueelle ja päivitä kaikki kutsujat `rg`:llä. Nykyisiä domain-, repository- tai service-rajapintoja ei siirretä.
- Siirrä Voice Baseline -UI ja kalibrointitoiminto Settingsistä HearingViewModeliin. Ehto säilyy täsmälleen `Pro && aktiivinen mittaus && sound detection käytössä`; tallennus käyttää edelleen `AudioSessionManager.captureVoiceBaseline(...)`- ja `PreferencesRepository.updateVoiceBaseline(...)`-polkuja.
- Säilytä Free/Pro-previewt, recovery-baseline-vaatimus, sleep visibility -asetus ja kaikki defense-in-depth-gatet ennallaan.

**History**

- Kun viimeiseltä 24 tunnilta ei ole dataa, näytä matala empty-state-kortti ilman tyhjää kaaviopinta-alaa ja akselia. Datan kanssa nykyinen kaavio säilyy.
- Anna session nimelle enemmän tilaa pienentämällä trailing PEAK/AVG-esitystä nykyisillä `dataMd`/`labelSm`-tyyleillä ja tiiviimmällä tokenoidulla välillä. Edit-painike ja emoji säilyvät vähintään 48 dp:n kohteina.
- Session emojit säilyvät tarkoituksellisina käyttäjän metadataelementteinä; niitä ei korvata yleisillä Material-kuvakkeilla.

### 3. Settings-hubi ja alisivut

- Muuta `settings` navigation graphiksi, jonka alireitit ovat:
  - `settings/home`
  - `settings/calibration`
  - `settings/calibration/octave`
  - `settings/notifications`
  - `settings/data_privacy`
  - `settings/display`
  - `settings/pro_about`
- Settingsin juuri näyttää viisi Material-kuvakkeellista riviä: Calibration, Notifications & alerts, Data & privacy, Display, Pro & About. Emoji-otsikot poistuvat.
- Kaikki alisivut käyttävät Settings-graphille scopettua yhteistä `SettingsViewModel`-instanssia `getBackStackEntry("settings")` + `hiltViewModel(parentEntry)` -mallilla, jonka Androidin [ViewModel scoping -ohje](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis) dokumentoi.
- Laajenna `DbCheckTopAppBar` tukemaan valinnaista takaisin-painiketta. Hubissa näkyy logo; alisivuilla takaisin-painike ja sivun otsikko.
- Sivujen omistus:
  - **Calibration:** mic sensitivity, weighting, response, input ja profiilit; Octave Calibration avautuu omalle syvemmälle sivulle.
  - **Octave Calibration:** valitun profiilin bandisäätimet ja reset; sama ViewModel-state säilyy.
  - **Notifications & alerts:** nykyiset exposure-, peak-, audible-, TTS-, passive monitoring-, threshold- ja schedule-toiminnot.
  - **Data & privacy:** Health Connect, CSV, WAV, location, backups/restore, clear history ja lockscreen privacy.
  - **Display:** theme, waveform, refresh rate sekä technical metadata-, dosimeter-, sound detection- ja sleep visibility -asetukset.
  - **Pro & About:** Pro-kortti/oston tila, debug force-free nykyisin ehdoin sekä versio- ja about-tiedot.
- `Screen.Settings.createRoute(false)` avaa graphin/hubin ja `createRoute(true)` avaa suoraan Pro & About -sivun. Nykyinen `settings?showPro={showPro}` säilytetään redirect-yhteensopivuusreittinä.
- Pilko nykyiset Settings-sectionit sivukohtaisiksi ilman preference- tai service-logiikan kopiointia. `SettingsViewModel` säilyttää `AudioSessionManager`-riippuvuuden backup/restore- ja clear-history-estojen vuoksi, vaikka Voice Baseline ja sen `isRecording`-UI-state siirtyvät Hearingille.
- Pitkät privacy-tekstit tiivistetään oletustilassa yhdeksi riviksi ja 48 dp:n info-painikkeeksi, joka avaa täyden nykyisen tekstin `DbCheckAlertDialogissa`. Riskin ollessa aktiivinen:
  - WAV-varoitus näkyy kokonaan, kun raakaaudiotallennus on päällä.
  - Lockscreen-varoitus näkyy kokonaan, kun lukitusnäytön julkiset lukemat ovat päällä.
  - Passive monitoring näyttää täyden disclosure-tekstin käynnistysvahvistuksessa ja aktiivisessa tilassa.
- Säilytä nykyiset `DbCheckChip`- ja `DbCheckSlider`-komponentit: koodi jo erottaa selected/unselected-tilat ja antaa päivävalinnoille saavutettavuuskuvaukset. Schedule-slider ja Sound Reference -rail pysyvät eri komponentteina, koska niillä on eri käyttötarkoitukset.
- Poista `DbCheckButton`-tertiary-tyylin automaattinen uppercase-muunnos. Käytä normaalia kirjainkokoa sivu-, kortti- ja toimintoteksteissä; säilytä lyhyet overline-otsikot sekä tekniset lyhenteet kuten CSV, WAV, TWA ja NIOSH.

## Rajapinnat, yhteensopivuus ja dokumentaatio

- Uudet UI-rajapinnat ovat `Screen.Hearing`, Settings-alireitit, `HearingUiState`, `HearingScreenActions` ja yhteinen hearing-health-yhteenvetolaskuri.
- Room-schemaan, DataStore-avaimiin, repository/service-rajapintoihin, export-formaatteihin tai entitlement-policyyn ei tehdä muutoksia eikä migraatioita.
- Nykyiset top-level state restoration-, fullscreen route-, Pro redirect-, Sharesheet-, permission-, backup/restore- ja activity-result-polut säilytetään.
- Päivitä toteutuksen yhteydessä `UI-SPEC.md` vastaamaan koodia. Koska top-level-rakenne, Settings-dataflow ja UI-vastuut muuttuvat arkkitehtonisesti, päivitä myös projektin `AGENTS.md` ja `memory/MEMORY.md`.
- Säilytä käyttäjän nykyiset muutokset tiedostossa `dBcheck_codex_review_questions.md` ja untracked Fable-suunnitelma; niitä ei siivota tai ylikirjoiteta.

## Testaus ja hyväksymiskriteerit

- Tee jokainen kokonaisuus testivetoisesti ja pidä navigaatio, screen ownership, Settings graph sekä visuaalinen tiivistys erillisinä tarkistettavina muutoksina.
- Navigaatiotestit todistavat viisi top-level-kohdetta, oikean järjestyksen, Trends-labelin, Hearing-paluut, Settings-alireittien valinnan, reselect-to-root-käytöksen sekä bottom bar/railin piiloutumisen fullscreen-reiteillä.
- ViewModel-testit kattavat:
  - Trends ilman tool-korttien riippuvuuksia
  - Hearing Free/Pro-, empty/data-, recovery- ja tinnitus-tilat
  - Voice Baseline -kalibroinnin kaikki kolme gate-ehtoa ja tallennuksen
  - olemassa olevien preference-, entitlement- ja execution-gatejen muuttumattomuuden
- Compose-/contract-testit kattavat Meterin compact/expanded-tilat, compact History emptyn, session nimen tilan, Settings-sivujen sisältöomistuksen, mixed selected/unselected -päivächipit ja warning-dialogit.
- Lisää screenshot-baselinet vähintään 360 × 800 dp:n dark/light-tiloille:
  - Meter idle, recording ja dosimeter
  - Trends Overview, Spectral ja Environment
  - Hearing Free ja Pro
  - History empty ja sessions
  - Settings hub sekä kuusi alisivua
- Vertaa uudet saman viewportin ja tilan screenshotit rinnakkain käyttäjän 21 lähdekuvan kanssa. Hyväksymisehdot: Meterin oletustila ei vierity, Settings on selkeä hubi, ominaisuuksia ei katoa, teksti ei leikkaudu 1.5× fontilla, valinnat erottuvat sekä kaikki interaktiiviset kohteet täyttävät Androidin [48 dp:n vähimmäiskoon](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
- Tarkistuskomennot:
  - `.\gradlew.bat :app:testDebugUnitTest`
  - `.\gradlew.bat :app:assembleDebug`
  - `.\gradlew.bat :app:previewScreenshot`
  - `.\gradlew.bat :app:validateDebugScreenshotTest`
  - `updateDebugScreenshotTest` vain, kun uudet vertailukuvat on hyväksytty tarkoituksellisiksi baseline-muutoksiksi.
- Käyttäjä ajaa lopuksi `lc`:n. `sc`:tä ei ajeta ilman erillistä pyyntöä.
- Oletuksena live-kaavio säilytetään Meterissä suljettavan Live details -rivin takana. Uusi pyyntö syrjäyttää vanhan neljän top-level-välilehden ja tertiary-uppercase-käytännön, mutta muut nykyisen Material 3 -viimeistelysuunnitelman tokeni- ja shared-component-sopimukset säilyvät.
