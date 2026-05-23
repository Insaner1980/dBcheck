# dBcheck

**Premium Android-desibellimittari ja kuuloterveys-sovellus.**

Päivitetty koodin perusteella: **2026-05-09**.

dBcheck on Kotlin / Jetpack Compose -sovellus, joka mittaa ympäristön melua reaaliajassa,
tallentaa melualtistussessioita, näyttää analytiikkaa, tarjoaa kuulotestin ja rakentaa
sessioista jaettavia raportteja. Visuaalinen identiteetti on "Auditory Observatory":
rauhallinen, editorial wellness -henkinen mittari, ei geneerinen työkaluapp.

Nykytila: runko ja iso osa ominaisuuksista on toteutettu, mutta sovellus ei ole vielä
julkaisukypsä. Keskeiset Meter-, Analytics-, History-, Settings-, Hearing Test- ja
Session Detail -polut ovat kytkettyjä, mutta julkaisu vaatii vielä laitetason
verifiointia, saavutettavuusauditin, lokalisoinnin ja kliinisten/akustisten oletusten
tarkennuksen.

---

## Tekniikkapino

Versiot on tarkistettu tiedostoista `gradle/libs.versions.toml`,
`app/build.gradle.kts` ja `gradle/wrapper/gradle-wrapper.properties`.

| Teknologia | Versio | Käyttötarkoitus |
|---|---:|---|
| Kotlin | 2.3.20 | Kieli ja Compose compiler plugin |
| Android Gradle Plugin | 9.1.0 | Android build |
| Gradle wrapper | 9.4.1 | Build tool |
| JVM / Java target | 21 | Compile target |
| Compose BOM | 2026.03.00 | Compose-kirjastojen versiohallinta |
| Material 3 | BOM | UI-komponentit custom-teeman päällä |
| Activity Compose | 1.10.1 | Compose activity integration |
| Lifecycle | 2.9.0 | runtime, ViewModel Compose, runtime-compose |
| Navigation Compose | 2.9.7 | Näyttöreititys |
| Hilt | 2.59.2 | Dependency injection |
| Hilt Navigation Compose | 1.2.0 | `hiltViewModel()` navigaatiossa |
| KSP | 2.3.6 | Room/Hilt annotation processing |
| Room | 2.8.4 | Lokaali tietokanta |
| DataStore Preferences | 1.1.4 | Asetukset ja Pro-tila |
| Coroutines | 1.10.2 | Async/Flow |
| Google Play Billing KTX | 8.3.0 | Kertaosto Pro-tuotteelle |
| Health Connect client | 1.1.0 | Melusessioiden synkkaus ja sykkeen luku |
| Glance | 1.1.1 | Kotinäytön widget |
| Detekt | 1.23.8 | Staattinen analyysi |
| Detekt Compose rules | 0.5.6 | Compose-säännöt |
| Screenshot test plugin | 0.0.1-alpha13 | Compose preview screenshot -testit |
| Dependency Analysis | 3.7.1 | Riippuvuusanalyysi |
| Min SDK | 26 | Android 8.0 |
| Compile / Target SDK | 36 | Android API |

Testikirjastot: JUnit 4.13.2, MockK 1.13.16, Turbine 1.2.0, Room testing 2.8.4,
AndroidX JUnit 1.2.1 ja Espresso 3.6.1.

Vico on poistettu. Kaaviot ovat custom Canvas -toteutuksia.

---

## Arkkitehtuuri

Single Activity + Compose Navigation + MVVM. Riippuvuudet injektoidaan Hiltillä.

```text
com.dbcheck.app/
├── MainActivity.kt / DbCheckApplication.kt
├── di/                     Hilt-modulit: AppModule, DatabaseModule
├── billing/                BillingManager, BillingGateway, ProFeatureManager,
│                           ProFeature
├── data/
│   ├── local/db/           Room: entities, DAOt, DbCheckDatabase
│   ├── local/preferences/  UserPreferencesDataStore + UserPreferences
│   ├── export/             ExportCsvUseCase, CsvExportFormatter
│   ├── model/              Room -> domain -mappaukset
│   └── repository/         Session, Measurement, Preferences, HearingTest
├── domain/
│   ├── audio/              AudioEngine, DecibelCalculator, FrequencyWeightingFilter,
│   │                       FFTProcessor, SpectralAnalyzer, ToneGenerator
│   ├── analytics/          ExposureAnalyticsCalculator + domain-input-mallit
│   ├── entitlement/        ProEntitlementPolicy
│   ├── hearingtest/        Hughson-Westlake-proseduuri, codec ja pisteytys
│   ├── noise/              NoiseLevel ja 40/70/85 dB -rajat
│   ├── report/             SessionReportCalculator + raporttimallit
│   └── session/            Session ja SessionMetadata
├── service/                MeasurementForegroundService, NotificationHelper,
│                           NotificationNoiseLevel, AudioSessionManager,
│                           MeasurementPersistenceSampler, HearingTestService,
│                           HealthConnectService, BackupService
├── sync/                   HealthConnectManager, HealthConnectModels,
│                           BackupGateway, LocalBackupManager
├── ui/
│   ├── theme/              Color, Type, Shape, Spacing, Gradient, Theme
│   ├── components/         Jaetut UI-komponentit
│   ├── navigation/         Screen, NavHost, BottomNavDestination
│   ├── meter/              Live-mittari
│   ├── analytics/          Viikkoanalytiikka ja Pro-previewt
│   ├── history/            Sessiohistoria
│   ├── history/detail/     Session Detail, PDF- ja PNG-raportointi
│   ├── settings/           Asetukset, Pro, Health Connect, lock-screen meter
│   └── hearingtest/        Setup -> Active -> Results
├── util/                   HapticFeedbackHelper, ShareResultsGenerator,
│                           PdfChartRenderer, ExportPdfReportUseCase
└── widget/                 Glance-widget
```

Raportoinnissa on yksi laskennan lähde: `SessionReportCalculator` rakentaa
`SessionReportData`-mallin Session- ja Measurement-datasta. Session Detail UI,
PDF-export ja PNG-jakokortti käyttävät samaa mallia.

---

## Design system

- Värit: dark/light-tokenit `ui/theme/Color.kt`:ssa. Päägradientti ja tonaaliset
  surface-tasot ovat teeman kautta, ei inline-arvoina.
- Typografia: Manrope yleistekstissä ja Space Grotesk numeerisessa/datanäytössä.
- Muodot ja spacing: `Shape.kt` ja `Spacing.kt`; spacing on 8dp-gridiin nojaava.
- Komponentit: mm. `DbCheckButton`, `DbCheckCard`, `DbCheckChip`, `DbCheckSlider`,
  `DbCheckToggle`, `ProLockOverlay`, `SessionCard`, `BottomNavBar`,
  `SkeletonLoader` ja `EmptyState`.
- Glassmorphism-tyyli ja gradientit ovat teematiedostoissa. Uudet design-arvot
  tulee keskittää teemaan eikä kovakoodata käyttöpaikkoihin.

---

## Näyttörakenne

### Pääreitit

| Reitti | Näyttö | Toteutunut käyttäytyminen |
|---|---|---|
| `meter` | Meter | Pyöreä live-gauge, waveform, Min/Avg/Max, Play/Pause/Reset/Share. Share rakentaa Android Sharesheet -intentit mittauksen nykyisistä arvoista. Mikrofoni- ja ilmoitusluvat pyydetään Meterissä. Valmis mittaus navigoi automaattisesti Session Detailiin. |
| `analytics` | Analytics | Viikon keskiarvot Room-datasta, kuuloterveysstatus, Pro-gatettu live-spektrianalyysi, 7 päivän ympäristömix Room-datasta, 30 päivän trendi, 12 kuukauden raportti ja Pro-gatettu kuulotesti-CTA. Free-käyttäjille Pro-kortit näyttävät locked-previewn ilman oikeaa analytiikkadataa. |
| `history` | History | 24h-kaavio, viimeisimmät 20 sessiota, Free-käyttäjälle 7 päivän filtteri, session-kortit avaavat Session Detailin. "View All" vaihtaa listan kaikkiin käyttäjälle sallittuihin valmiisiin sessioihin. |
| `history/detail/{sessionId}` | Session Detail | Sessioraportti, LAeq/LCpeak/TWA/dose, aikasarjakaavio, peak event -lista, Pro-gatettu PDF-export, PNG-jakokortti ja Pro Health Connect -sykeoverlay. |
| `settings?showPro={showPro}` | Settings | Kalibrointi ja taajuuspainotus Pro-gatella, ilmoitusasetukset, lock-screen meter, Health Connect, Pro-gatettu Data & Export, teema ja Pro-upsell. Settingsin Pro-kortti ja sen sisäiset ProLockOverlayt käynnistävät Google Play Billing -ostovirran. Debug-buildissä Pro-kortissa on force-free-toggle testausta varten. |
| `hearing_test/setup` | Hearing Test Setup | Kuulotestin aloitusnäkymä. |
| `hearing_test/active` | Hearing Test Active | 12 vaihetta: 6 taajuutta x 2 korvaa. Modified Hughson-Westlake: heard -> -10 dB, not heard -> +5 dB. |
| `hearing_test/results/{testId}` | Hearing Test Results | Näyttää uusimman tallennetun tuloksen audiogrammina ja mittareina. Share Results rakentaa PNG-kortin ja saatetekstin Android Sharesheetiin. |

`DbCheckNavHost` käyttää bottom navigationia puhelimella ja NavigationRailia, kun
`screenWidthDp >= 600`.

---

## Ominaisuudet: Free vs Pro

| Ominaisuus | Free | Pro | Tila koodissa |
|---|:---:|:---:|---|
| Live dB-mittari, waveform, Min/Avg/Max | x | x | Kytketty |
| Foreground measurement notification | x | x | Kytketty |
| Melutasoilmoitukset ja threshold-slider | x | x | Asetukset kytketty; varsinaiset exposure-alert-säännöt ovat rajalliset |
| Dark/Light/System-teema | x | x | Kytketty |
| 7 päivän historia | x | x | Free-filtteri koodissa |
| Rajoittamaton historia |  | x | Pro-filtteri koodissa |
| Viikon altistumiskaavio ja kuuloterveys | x | x | Kytketty |
| Health Connect -melusessiosynkkaus | x | x | Free-käyttäjällekin sallittu asetuksista |
| Mikrofoniherkkyyden kalibrointi |  | x | Pro-gatettu ja kytketty AudioEngineen |
| Taajuuspainotus A/B/C/Z/ITU-R 468 |  | x | Pro-gatettu ja kytketty AudioEngineen |
| Lock-screen live meter |  | x | Pro-gatettu custom RemoteViews -notification |
| Health Connect -sykeoverlay |  | x | Pro-gatettu Session Detailiin |
| Tieteellinen PDF-raportti |  | x | Pro-gatettu, CreateDocument-polku kytketty |
| Session Detail PNG -jakokortti | x | x | Kytketty |
| Kotinäytön widget |  | x | Glance-widget kytketty Pro-gatella |
| Kuulotesti |  | x | CTA Pro-gatettu; active/results-flow toteutettu |
| CSV-vienti |  | x | Pro-gatettu Settingsin Data & Export -osioon |
| Session-nimeäminen ja tagit |  | x | Pro-gatettu Historyssa ja Session Detailissa |
| Spektrianalyysi |  | x | Pro-gatettu live-spektri raw PCM -datasta; Free saa locked-previewn |
| Ympäristömix |  | x | Pro-gatettu 7 päivän Room-dataan perustuva jakauma; Free saa locked-previewn |

Monetisaatio: `BillingManager` tukee yhtä INAPP-tuotetta `dbcheck_pro`,
käyttää Billing KTX suspend -funktioita, pending purchases -asetusta,
auto service reconnectionia ja acknowledgePurchase-kutsua. Settingsin
Pro-upsell ja Settingsissä näkyvät ProLockOverlayjen Upgrade-napit kutsuvat
`BillingGateway.launchPurchaseFlow(...)`-polkua. Billing-tapahtumat välittyvät
Settingsiin `PurchaseEvent`-virtana.

Pro-entitlementin laskenta on keskitetty `ProEntitlementPolicy`-luokkaan.
Releasessa Pro määräytyy ostotilan mukaan. Debug-buildissä käyttäjä on oletuksena
Pro, ellei debug-only "Force Free mode" ole päällä.

---

## Audio engine

- `AudioProcessingConfig`: keskitetty 44.1 kHz sample rate, 4096 sample chunk
  ja 4096 point FFT-koko audio-domainin käyttöön.
- `AudioEngine`: AudioRecord 44.1 kHz mono PCM16, 4096 sample read chunk,
  permission check ennen tallennusta, `@RequiresPermission` AudioRecord-luonnissa.
  Julkaisee Pro-gatetun `spectralFrame`-tilavirran raw PCM -chunkista laskettua
  live-spektriä varten ja tyhjentää sen stopissa tai Pro-gaten poistuessa.
- `DecibelCalculator`: RMS -> `20*log10(rms/32768) + 90 + calibrationOffset`,
  hiljaisuus -> 0 dB, tulos rajataan 0-130 dB.
- `FrequencyWeightingFilter`: A-, B-, C-, Z- ja ITU-R 468 -painotukset.
  A/B/C/ITU-R toteutetaan kaskadoiduilla biquad-sektioilla 44.1 kHz:lle,
  Z palauttaa raakabufferin.
- `service/AudioSessionManager`: session lifecycle, preferenssien live-seuranta
  (`frequencyWeighting`, `micSensitivityOffset`, `isProUser`, `refreshRate`),
  typed refresh-rate-politiikkaan perustuva mittausrivien persistointi,
  Pro-gatetun spektrilaskennan ohjaus, weighted min/avg/max, instant peak,
  session completion -eventti ja widgetin päivitys.
- `MeasurementForegroundService`: käynnistyy mittauksen ajaksi
  `FOREGROUND_SERVICE_TYPE_MICROPHONE`-tyypillä ja päivittää ilmoitusta sekunnin
  välein.
- `FFTProcessor`: 4096-point radix-2 FFT, Hann window, DC-bin ohitus
  dominanttitaajuushaussa.
- `SpectralAnalyzer`: muuntaa raw PCM16 -chunkin 24 logaritmiseksi
  20 Hz-20 kHz bandiksi, dominanttitaajuudeksi ja bandwidth-luokaksi.
  Analyticsin `SpectralAnalysisCard` lukee live-only-tilaa tästä polusta;
  spektriä ei vielä tallenneta `measurements.frequencyData`-kenttään.
- `ToneGenerator`: AudioTrack MODE_STATIC sine wave, 50 ms fade in/out
  kuulotestiä varten.

---

## Health Connect

Integraatioadapteri on `sync/HealthConnectManager.kt`. UI käyttää sitä
`service/HealthConnectService.kt`-portin kautta, jotta Settings ja Session
Detail eivät kanna `sync`-paketin malleja.

- Saatavuus tarkistetaan `HealthConnectClient.getSdkStatus(...)`-kutsulla.
- Melusessioiden kirjoitus käyttää `ExerciseSessionRecord`-tietuetta tyypillä
  `EXERCISE_TYPE_OTHER_WORKOUT`.
- `Metadata.clientRecordId` on muodossa `noise_dose_<date>_session_<id>`.
- `notes` sisältää LAeq-, max-, peak- ja weighting-arvot.
- Sykedata luetaan `HeartRateRecord`-sampleista valitun session aikavälille ja
  näytetään Session Detailin Pro-overlayna.
- Kuulotestin Health Connect -kirjoitus on tietoisesti no-op, koska nykyisestä
  Health Connect -datatyyppilistasta puuttuu natiivisti tuettu audiometria- tai
  melualtistusrecord.

Manifestissa on Health Connect -oikeudet:
`android.permission.health.WRITE_EXERCISE` ja
`android.permission.health.READ_HEART_RATE`, Health Connect -pakettikysely sekä
permission rationale / permission usage -activity aliakset.

---

## Raportointi, export ja jakaminen

- `SessionReportCalculator` laskee LAeq:n energia-average-menetelmällä,
  8h TWA:n, NIOSH dose -prosentin, LCpeak-arvon, time series -pisteet ja
  vähintään 85 dBA peak event -jaksot.
- `util/ExportPdfReportUseCase` kirjoittaa nelisivuisen natiivin `PdfDocument`-
  raportin: summary, scientific metrics, time series ja peak events.
- `PdfChartRenderer` keskittää PDF- ja Compose-kaavion koordinaattimuunnoksen.
- Session Detailin PDF-export käyttää
  `ActivityResultContracts.CreateDocument("application/pdf")`.
- `ShareResultsGenerator.shareSessionReportCard()` luo Session Detailin PNG-
  jakokortin.
- `ShareResultsGenerator.shareSessionStats()` ja
  `shareHearingTestResults()` ovat Meterin ja Hearing Results -näyttöjen
  jakopolut.
- Settingsin Data & Export -osio kutsuu `data/export/ExportCsvUseCase`-polkua ja avaa
  Android Sharesheetin kahdelle FileProviderin kautta jaettavalle CSV-tiedostolle.

---

## Tietokanta ja preferenssit

Room database: `DbCheckDatabase`, versio 1, `exportSchema = true`.

Entiteetit:

- `sessions`: start/end, min/avg/max/peak dB, nimi, emoji, tagit, active-tila,
  `frequencyWeighting`.
- `measurements`: FK sessioniin, timestamp, raw dB, weighted dB, optional
  `frequencyData`.
- `hearing_test_results`: score, rating, vasen/oikea korva data,
  speech clarity, high frequency limit, average threshold.

DataStore-preferenssit:

- teema
- exposure alerts
- peak warnings
- notification threshold
- microphone sensitivity offset
- frequency weighting
- waveform style
- refresh rate
- lockscreen meter
- Health Connect
- heart rate overlay
- Pro-status

`waveformStyle` ja `refreshRate` ovat Settingsin Free-asetuksia. Waveform
vaihtaa Meterin visualisoinnin Line/Filled/Bars-tyyliä. Refresh rate
throttlettaa Meterin UI-päivityksiä ja Roomiin tallennettavien mittausrivien
tiheyttä, mutta ei muuta `AudioRecord`in 44.1 kHz sample ratea tai 4096 sample
chunk-kokoa.

---

## Oikeudet

- `RECORD_AUDIO` - mikrofoni, runtime-pyyntö Meterissä
- `POST_NOTIFICATIONS` - Android 13+ ilmoitukset, best effort
- `FOREGROUND_SERVICE` ja `FOREGROUND_SERVICE_MICROPHONE` - taustamittaus
- `VIBRATE` - haptiikka
- `com.android.vending.BILLING` - Google Play Billing
- `android.permission.health.WRITE_EXERCISE` - Health Connect melusessiosynkkaus
- `android.permission.health.READ_HEART_RATE` - Health Connect sykeoverlay

---

## Widget ja ilmoitukset

Glance-widget `DbCheckWidget`:

- Pro + sessiodata: näyttää viimeisimmän avg dB -arvon, melutasotunnisteen ja
  suhteellisen ajan.
- Pro + ei sessiodataa: "No data yet" / "Tap to start measuring".
- Free: lukko, "dBcheck Pro" ja "Upgrade to unlock".
- Päivitys: `updatePeriodMillis = 1800000` (30 min) ja manuaalinen update
  session päättyessä sekä Pro-oikeuden muuttuessa.

Lock-screen/live notification:

- Tavallinen measurement notification näkyy kaikille mittauksen aikana.
- Pro + lockscreen meter enabled käyttää custom collapsed/expanded RemoteViews-
  näkymiä, joissa näkyvät current dB, peak dB, kesto ja melutasoa kuvaava
  vihreä/keltainen/punainen piste.

---

## Testit

Nykyiset paikalliset testitiedostot:

- `ProEntitlementPolicyTest` - debug/release Pro-oikeuden policy
- `MeterDisplayPreferenceTest` - typed waveform- ja refresh-rate-preferenssit
- `SessionMetadataTest` - session nimen, emojin, tagien ja slugien normalisointi
- `HearingTestProcedureTest` - Hughson-Westlake-proseduurin threshold-eteneminen
- `HearingTestResultCalculatorTest` - kuulotestin pisteytys ja threshold-codec
- `ExposureAnalyticsCalculatorTest` - kuukausi- ja vuosianalytiikan LAeq- ja jakaumalaskenta
- `FFTProcessorTest` - FFT-perusominaisuudet
- `FrequencyWeightingFilterTest` - ITU-R 468 vaste 1 kHz ja 6.3 kHz
- `MeasurementPersistenceSamplerTest` - mittausrivien harvennus ja pakotettu persistointi
- `SpectralAnalyzerTest` - live-spektrin bandit, dominanttitaajuus ja bandwidth-luokka
- `SessionReportCalculatorTest` - LAeq, NIOSH dose, TWA ja peak event -ryhmittely
- `CsvExportFormatterTest` - CSV-fieldien escaping ja vientimuoto
- `NotificationNoiseLevelTest` - lock-screen thresholdit
- `LocalBackupManagerTest` - paikallisen backup/restore-polun validointi
- `HealthConnectNoiseDosePayloadTest` - Health Connect payloadin muodostus
- `HealthConnectHeartRateMapperTest` - sykearvojen aikavälisuodatus ja lajittelu
- `AnalyticsViewModelSpectralTest` - Pro-gatettu analytics-dataflow, spektri ja previewt
- `ResultsViewModelShareTest` - kuulotestitulosten share-intent
- `SessionDetailViewModelMetadataTest` - Session Detailin metadata-tallennus ja Pro-gate
- `HistoryViewModelViewAllTest` - Historyn View All vaihtaa 20 viimeisimmästä kaikkiin sallittuihin sessioihin
- `MeterViewModelShareTest` - Meterin share-intent ja virhetilat
- `SettingsViewModelBackupTest` - backup/restore-toimintojen UI-tila ja aktiivisen mittauksen esto
- `SettingsViewModelCsvExportTest` - CSV-viennin Pro-gate ja share-intent
- `SettingsViewModelDisplayPreferenceTest` - waveform-, refresh-rate- ja ilmoitusasetusten tallennus
- `SettingsViewModelPurchaseTest` - Settingsin ostovirta ja ostotapahtumat
- `PdfChartRendererTest` - aikasarjan koordinaattimuunnos
- `ShareResultsGeneratorTest` - share-tekstien muodostus
- `ComponentScreenshotTests` - 4 Compose preview screenshot -testiä

Instrumentaatiotestihakemistoa ei ole. `testDebugUnitTest` on ajettu viimeksi
2026-05-09 tämän dokumenttipäivityksen yhteydessä. Projektin ohjeiden mukaisia
`lint-check`- tai `security-check`-skriptejä ei ajettu.

---

## Lint, analyysi ja paikalliset työkalut

Projektin ohjeiden mukaan:

- `lint-check` / `lc`: käyttäjän ajama skripti, joka ajaa ktlint + detekt +
  Android lint ja kirjoittaa tulokset `reports/`-hakemistoon.
- `security-check` / `sc`: käyttäjän ajama skripti, joka ajaa Semgrepin ja
  OWASP Dependency-Checkin ja kirjoittaa tulokset `reports/`-hakemistoon.
- `reports/` on gitignoressa eikä sitä commitoida.

Staattinen konfiguraatio:

- `.editorconfig`: `ktlint_code_style = android`
- `config/detekt/detekt.yml`: LongMethod 80, MaxLineLength 120, MagicNumber pois,
  UnusedPrivateMember päällä, Compose-funktioiden nimeämissääntö rajattu UI:sta,
  osa ktlint-formatointisäännöistä pois päältä Android Studio -formaatin vuoksi.
- `app/build.gradle.kts`: `ktlintCheck` alias riippuu `detekt`-taskista.

---

## CI/CD Pipeline

GitHub Actions -workflowt nykyisessä repossa:

| Workflow | Tiedosto | Tarkoitus |
|---|---|---|
| Android Static Checks | `.github/workflows/lint.yml` | `ktlintCheck`, `:app:detekt` ja `:app:lint` CI:ssä |
| CodeQL | `.github/workflows/codeql.yml` | Java/Kotlin CodeQL, JDK 21, Android SDK, manual Gradle build |
| SonarCloud | `.github/workflows/sonar.yml` | Build + unit-test coverage + Gradle `sonar` -task |
| Security Analysis | `.github/workflows/security.yml` | Projektin Semgrep-konfig + Gradle OWASP Dependency-Check SARIF upload |
| Qodana | `.github/workflows/qodana.yml` | JetBrains Qodana action v2026.1 |

SonarCloud:

- project key: `Insaner1980_dBcheck`
- organization: `insaner1980`
- coverage: `jacocoDebugUnitTestReport` tuottaa XML-raportin polkuun `app/build/reports/jacoco/debugUnitTest/jacocoDebugUnitTestReport.xml`

Qodana config:

- `qodana.yaml`: `jetbrains/qodana-jvm-android:2026.1`

---

## Kehitysympäristö

Windows/PowerShell-ympäristössä käytetään projektin wrapperia:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Linux/WSL-esimerkit:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew lintDebug
```

Asennus laitteelle:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Vaatii JDK 21:n ja Android SDK:n API 36 -tuen.

---

## Toteutusvaiheet

### Phase 1 - MVP

Toteutettu pääosin:

- Projektirakenne, Hilt, Room, DataStore
- Design system ja komponenttikirjasto
- 4 päävälilehteä: Meter, Analytics, History, Settings
- AudioRecord-pohjainen live-mittaus
- Foreground service
- Google Play Billing -backend, Settingsin ostovirta ja Pro-gating-rakenne
- Debug-only force-free-toggle Pro-gatejen testaamiseen

### Phase 2 - Enhancement

Osittain toteutettu:

- Kuulotesti-flow ja tulosten tallennus
- FFTProcessor ja SpectralAnalyzer on kytketty Pro-gatettuun live-spektrikorttiin
- SessionNamingSheet on kytketty Historyyn ja Session Detailiin Pro-gatella
- ExportCsvUseCase on kytketty Settingsin Data & Export -osioon
- Glance-widget on kytketty
- Hearing Results -jakaminen on kytketty ShareResultsGeneratorin PNG-share-polkuun

### Phase 3 - Polish

Osittain toteutettu:

- LocalBackupManager tekee lokaaleja varmuuskopioita
- ShareResultsGenerator tekee tekstin, kuulotestin PNG-kortin ja Session Detail
  PNG-kortin
- MonthlyTrendChart ja YearlyReportCard ovat kytkettyjä Pro-gatettuun
  ExposureAnalyticsCalculator-dataflow'hun
- Meterin ja Hearing Resultsin share-napit ovat UI:ssa kytkettyjä

Puute: Google Drive -backupia ei ole; LocalBackupManager tekee tällä hetkellä
vain paikallisia backup-tiedostoja.

### Phase 12 - kilpailukykyominaisuudet

Osittain toteutettu:

- Health Connect -melusessiosynkkaus ja sykeoverlay
- B-painotus ja ITU-R 468 -painotus
- Lock-screen live meter custom notificationina
- Session Detail -näkymä
- PDF-raportti ja Session Detail PNG-jako

Puute: Health Connectissa ei ole natiivia melu- tai audiometriatietuetta, joten
melu mallinnetaan exercise sessionina ja kuulotestin synkkaus skipataan.

---

## Referenssitiedostot

| Tiedosto | Tarkoitus |
|---|---|
| `dBcheck_design_spec.md` | Designin nykyinen referenssi |
| `dBcheck_complete_spec_v2.md` | Laajempi tuotemäärittely |
| `dBcheck_competitive_features_addendum.md` | Kilpailukykyominaisuuksien lisämäärittely |
| `design_evolution_spec.md` | Design-kehityksen lisämuistiinpanot |
| `images/*.png` | Visuaaliset referenssit |
| `AGENTS.md` | Paikalliset työskentely-, lint- ja memory-ohjeet |
| `memory/MEMORY.md` | Projektin arkkitehtuuri- ja sessionmuisti |

---

## Tunnetut rajoitukset ja riskit

- Meterin Share ja Hearing Resultsin Share Results ovat kytkettyjä, mutta
  share-polut vaativat vielä kattavamman laitetason verifioinnin Android Sharesheetia vasten.
- Free-käyttäjien Pro-kortit näyttävät tarkoituksella locked-preview-dataa;
  oikeaa Pro-analytiikkadataa ei välitetä overlayn alle.
- Health Connect -melu tallennetaan exercise sessionina, koska natiivia
  melualtistuksen recordia ei ole. Kuulotestin Health Connect -kirjoitus on
  no-op.
- Kuulotestin kynnykset ovat dBFS-arvoja, eivät kalibroituja dB HL -arvoja.
  Tulokset sopivat suhteelliseen seurantaan, eivät kliiniseen audiometriaan.
- `speechClarity` ja `highFreqLimit` ovat arvioita/simplifikaatioita.
- A/B/C/ITU-R-painotusten kertoimet ovat koodissa, mutta kattava
  scipy/MATLAB- tai mittalaitereferenssiverifiointi puuttuu. Nykyinen unit-testi
  kattaa vain ITU-R 468:n kaksi referenssipistettä.
- LocalBackupManager on lokaali backup; Google Drive -integraatiota ei ole.
- ProGuard-säännöt kattavat vain Roomin, Hiltin ja Billingin.
- Lokalisointi puuttuu: lähes kaikki UI-tekstit ovat kovakoodattua englantia,
  `strings.xml` sisältää käytännössä vain app-nimen.
- Accessibilityä ei ole auditoitu kattavasti.
- Instrumentaatiotestejä ei ole.

---

## Project management

- GitHub: https://github.com/Insaner1980/dBcheck
- Linear project: https://linear.app/loikka1/project/dbcheck-0336faa49e71
- Milestone: v1.0 - Play Store Release
