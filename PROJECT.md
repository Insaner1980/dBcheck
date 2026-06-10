# dBcheck

**Premium Android-desibellimittari ja kuuloterveys-sovellus.**

Paivitetty nykyisen checkoutin perusteella: **2026-06-07**.

dBcheck on Kotlin / Jetpack Compose -sovellus, joka mittaa ympariston melua
reaaliajassa, tallentaa melualtistussessioita, nayttaa analytiikkaa, tarjoaa
Pro-gatetun suhteellisen kuulotestin ja rakentaa sessioista jaettavia raportteja.
Visuaalinen identiteetti on "Auditory Observatory": rauhallinen, editorial
wellness -henkinen mittari, ei geneerinen tyokaluapp.

Nykytila: runko ja iso osa v1.0-ominaisuuksista on toteutettu. Meter,
Analytics, History, Session Detail, Settings, Health Connect, local backup,
CSV/PDF/PNG-exportit, Pro-entitlement ja hearing-test-flow ovat koodissa
kytkettyja. Sovellus ei ole viela julkaisukypsa ilman laitetason audio- ja
foreground-service-verifiointia, saavutettavuusauditointia, kielenkaannoksia,
Play Billing / release-signing -tuotantotarkistuksia ja akustisten/klinisten
rajojen lopullista dokumentointia.

Tama dokumentti kuvaa nykyista koodia, ei tavoitetilaa.

---

## Ulkoiset tarkistukset 2026-06-07

Projektin ohjeen mukaan ulkoisesti muuttuvat Android-kaytannot tarkistettiin
virallisista lahteista ennen dokumenttipaivitysta:

- Lahteet:
  [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types),
  [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types).
- Android 14+ vaatii foreground servicelle sopivan service-tyypin ja siihen
  liittyvan foreground-service-permissionin. Mikrofoni-service kayttaa
  `android:foregroundServiceType="microphone"`, manifest-permissionia
  `FOREGROUND_SERVICE_MICROPHONE` ja `startForeground()`-tyyppia
  `FOREGROUND_SERVICE_TYPE_MICROPHONE`. `RECORD_AUDIO` on while-in-use
  -runtime-lupa, joten backgroundista kaynnistettavaa mikrofonipalvelua koskee
  rajoituksia.
- Health Connectin nykyinen datatyyppilista sisaltaa `ExerciseSessionRecord`-
  ja `HeartRateRecord`-tyypit. Nykyisesta virallisesta listasta ei loydy
  dBcheckin kayttotarpeeseen natiivia melualtistus- tai audiometriatietuetta,
  joten koodin nykyinen malli kirjoittaa melun exercise sessionina ja jattaa
  kuulotestin Health Connect -kirjoituksen tietoisesti no-opiksi.

---

## Tekniikkapino

Versiot on tarkistettu tiedostoista `gradle/libs.versions.toml`,
`app/build.gradle.kts`, `build.gradle.kts` ja
`gradle/wrapper/gradle-wrapper.properties`.

| Teknologia | Versio | Kayttotarkoitus |
|---|---:|---|
| Kotlin | 2.3.20 | Kieli ja Compose compiler plugin |
| Android Gradle Plugin | 9.1.0 | Android build |
| Gradle wrapper | 9.4.1 | Build tool |
| JVM / Java target | 21 | Compile target |
| Compose BOM | 2026.03.00 | Compose-kirjastojen versiohallinta |
| Material 3 | BOM | UI-komponentit custom-teeman paalla |
| AndroidX Core KTX | 1.16.0 | Android Kotlin extensions |
| Activity Compose | 1.10.1 | Compose activity integration |
| Lifecycle | 2.9.0 | ViewModel, runtime ja runtime-compose |
| Navigation Compose | 2.9.7 | Compose-reititys |
| Hilt | 2.59.2 | Dependency injection |
| Hilt Navigation Compose | 1.2.0 | `hiltViewModel()` navigaatiossa |
| KSP | 2.3.6 | Room/Hilt annotation processing |
| Room | 2.8.4 | Lokaali tietokanta |
| DataStore Preferences | 1.2.1 | Asetukset ja Pro-entitlement |
| Coroutines | 1.10.2 | Async/Flow |
| Google Play Billing KTX | 8.3.0 | Kertaosto Pro-tuotteelle |
| Health Connect client | 1.1.0 | Melusessioiden synkkaus ja sykkeen luku |
| Glance | 1.1.1 | Kotinayton widget |
| WorkManager | 2.11.2 | Glance-riippuvuuden korjattu constraint |
| Guava Android | 33.6.0-android | Health Connect / transitiivinen constraint |
| Detekt | 2.0.0-alpha.3 | Staattinen analyysi |
| Detekt Compose rules | 0.5.8 | Compose-saannot |
| Compose Stability Analyzer | 0.7.4 | Compose-stabiliteettidumpit |
| Android Security Lints | 1.0.4 | Android security lintChecks |
| Screenshot test plugin/API | 0.0.1-alpha14 | Compose preview screenshot -testit |
| Sentry Android Core | 8.43.1 | Debug-only crash-diagnostiikka, ei release-riippuvuutta |
| OWASP Dependency-Check Gradle plugin | 12.2.2 | CVE-skannaus |
| SonarQube Gradle plugin | 7.3.0.8198 | SonarCloud-analyysi |
| JaCoCo | 0.8.14 | Unit-test coverage |
| Min SDK | 26 | Android 8.0 |
| Compile / Target SDK | 36 | Android API |

Testikirjastot: JUnit 4.13.2, MockK 1.13.16, Turbine 1.2.0,
AndroidX Test Core 1.6.1, Robolectric 4.16.1 ja Coroutines Test 1.10.2.

Vico on poistettu. Kaaviot ovat custom Canvas / Android Canvas -toteutuksia.

---

## Arkkitehtuuri

Single Activity + Compose Navigation + MVVM. Riippuvuudet injektoidaan Hiltilla.
Korkean tason vastuunjako nykyisessa paketissa:

```text
com.dbcheck.app/
├── DbCheckApplication.kt     App startup: debug Sentry, billing, interrupted-session recovery,
│                             widget refresh Pro-oikeuden muuttuessa
├── MainActivity.kt           Edge-to-edge Compose host, theme bootstrap,
│                             billing refresh, restore restart
├── di/                       AppModule, DatabaseModule, BillingModule,
│                             SyncModule, CoroutineDispatchers
├── billing/                  BillingManager, BillingGateway,
│                             ProFeatureManager
├── data/
│   ├── export/               ExportCsvUseCase, CsvExportFormatter,
│   │                         ExportFileCache
│   ├── local/db/             Room database, schema, migrations, DAOt, entities
│   ├── local/preferences/    UserPreferencesDataStore and typed preference models
│   ├── model/                Room -> domain mappings
│   └── repository/           Session, Measurement, Preferences, HearingTest
├── domain/
│   ├── analytics/            ExposureAnalyticsCalculator and models
│   ├── audio/                AudioEngine, DecibelCalculator,
│   │                         FrequencyWeightingFilter, FFTProcessor,
│   │                         SpectralAnalyzer, ToneGenerator,
│   │                         AudioRecordPolicies
│   ├── entitlement/          ProEntitlementPolicy
│   ├── hearingtest/          Hughson-Westlake procedure, codec, scoring
│   ├── noise/                NoiseLevel and 40/70/85 dB boundaries
│   ├── report/               SessionReportCalculator and report models
│   └── session/              Session, SessionMetadata, SessionHistoryPolicy
├── service/                  AudioSessionManager, MeasurementForegroundService,
│                             MeasurementPersistenceSampler, NotificationHelper,
│                             NotificationPrivacyPolicy, NoiseAlertEvaluator,
│                             HealthConnectService, HearingTestService,
│                             BackupService
├── sync/                     HealthConnectManager, HealthConnectModels,
│                             BackupGateway, LocalBackupManager,
│                             BackupDatabaseValidator
├── ui/
│   ├── analytics/            Analytics screen, Pro analytics cards
│   ├── components/           Shared Compose components
│   ├── hearingtest/          Setup -> Active -> Results
│   ├── history/              Session history and naming sheet
│   ├── history/detail/       Session Detail, PDF and PNG report actions
│   ├── meter/                Live meter
│   ├── navigation/           Screen, DbCheckNavHost, BottomNavDestination
│   ├── settings/             Settings, Pro, Health Connect, backup/export
│   └── theme/                Color, Type, Shape, Spacing, Gradient, Theme
├── util/                     ShareResultsGenerator, ExportPdfReportUseCase,
│                             PdfChartRenderer, ReportTextFormatter,
│                             StringResourceIds, UserFacingError
└── widget/                   Glance widget and receiver
```

Arkkitehtuurisopimukset:

- `domain/` ei importtaa `data/`, `service/`, `sync/`, `ui/`, `billing/` tai
  `widget/` -kerroksia.
- UI-, widget- ja service-koodi ei kasittele Room-entityja suoraan.
  Repositoryt ja service-portit mapittavat data/sync-mallit domain-, report-
  tai UI-facing-malleiksi. `AudioSessionManager` jonottaa
  `domain/session/SessionMeasurement`-riveja, ja `SessionRepository` mapittaa
  ne transaktiossa `MeasurementEntity`-riveiksi.
- `DbCheckDatabase.DATABASE_NAME` on Room-tietokannan nimen lahde. Room builder,
  LocalBackupManager ja backup-testit viittaavat samaan vakioon.
- `ExportFileCache` omistaa FileProviderin authority-suffixin ja
  `cache/exports/`-hakemiston nimet. Manifest/XML/runtime/testit pidetaan
  samassa sopimuksessa.
- `domain/hearingtest/HearingTestPolicy` ja `HearingRating` omistavat
  kuulotestin taajuuslistan, tone timing -arvot ja rating-koodit.
- `domain/noise/NoiseAlertPolicy` omistaa noise notificationien exposure-
  keston ja peak-warning-rajan.
- `util/UserFacingError.kt` keskittaa teknisten `Throwable`-viestien
  suodatuksen kayttajalle naytettaviksi fallback-resurssiteksteiksi. UI ei saa
  nayttaa raakaa exception-viestia esimerkiksi share-, export-, Health
  Connect-, history- tai hearing-test-virheissa.
- Health Connectin status, hallintaintentit ja sykedata kulkevat
  `service/HealthConnectService.kt`-portin kautta, mutta Settingsin
  `HealthSyncSection` kayttaa AndroidX Health Connect
  `PermissionController`-result-contractia permission-pyyntojen
  kaynnistamiseen.
- Coroutine dispatcherit tulevat Hiltista qualifiereilla
  `DefaultDispatcher`, `IoDispatcher` ja `MainDispatcher`. `AppModule` on
  niiden provider-lahde.
- Raportoinnissa on yksi laskennan lahde:
  `domain/report/SessionReportCalculator.kt` rakentaa `SessionReportData`-
  mallin. Session Detail UI, PDF-export, PNG-jako ja Health Connect -notes
  nojaavat samaan raporttidataan.
- Room-kirjoitusten ja mittaussession completionin koordinointi kuuluu
  `SessionRepository`lle ja `AudioSessionManager`ille, ei UI:lle.

---

## Startup ja prosessilifecycle

- `DbCheckApplication.onCreate()` kutsuu source-set-kohtaista `SentryInit`-polkua; debug voi alustaa Sentry Android Coren `DBCHECK_SENTRY_DSN`-/`SENTRY_DSN`-ympäristömuuttujalla tai ignored `debug.credentials.properties` -tiedoston `sentry.dsn`-arvolla, release on no-op
- `DbCheckApplication.onCreate()` kaynnistaa Billing-yhteyden
  `BillingManager.startConnection()`-polulla.
- Sama startup kaynnistaa `AudioSessionManager.recoverInterruptedSession()`-
  tehtavan. Jos edellisen prosessin jaljilta Roomissa on aktiivinen sessio,
  se viimeistellaan hiljaisesti persistoiduista mittausriveista ilman
  auto-navigointia.
- `DbCheckApplication` seuraa `ProFeatureManager.isProUser`-virtaa ja paivittaa
  Glance-widgetit, kun Pro-oikeus muuttuu ensimmaisen emission jalkeen.
- `MainActivity` odottaa ensimmaista `UserPreferences`-emissiota ennen
  `DbCheckTheme`/`DbCheckNavHost`-sisallon piirtamista. Tama estaa tallennetun
  teeman valahdyksen system-teemana.
- `MainActivity.onResume()` kutsuu `BillingManager.refreshPurchases()`, jotta
  Play Billingin ulkopuolella valmistuneet tai pending-tilasta valmistuneet
  ostot kasitellaan foregroundiin palatessa.
- Restore-flow kaynnistaa sovelluksen uudelleen `AlarmManager` +
  immutable `PendingIntent` + `finishAffinity()` + `Process.killProcess()` -
  polulla, koska suljettua Room-instanssia ei kayteta turvallisesti samassa
  prosessissa.

---

## Manifest, oikeudet ja privaattidata

Manifestin keskeiset faktat:

- `applicationId` / namespace: `com.dbcheck.app`
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- `MainActivity` on ainoa launcher activity ja `android:exported="true"`.
- `HealthConnectPermissionDisclosureActivity` on `exported=false`.
- Health Connectin exported entrypointit ovat activity-aliaksia:
  `.HealthConnectPermissionsRationaleActivity` ja
  `.HealthConnectPermissionUsageActivity`. Ne targetoivat staattista
  `HealthConnectPermissionDisclosureActivity`a, eivat varsinaista
  navigation/data-muutosflow'ta.
- `MeasurementForegroundService` on `exported=false` ja
  `android:foregroundServiceType="microphone"`.
- `DbCheckWidgetReceiver` on `exported=false`.
- `FileProvider` on `exported=false`, `grantUriPermissions=true`, ja
  `file_paths.xml` rajaa jaettavat tiedostot vain `cache/exports/`-polkuun.
- `android:allowBackup="false"`, `backup_rules.xml` ja
  `data_extraction_rules.xml` sulkevat appin root-datan pois cloud backupista
  ja device transferista.
- `android:usesCleartextTraffic="false"`.

Manifest-oikeudet:

- `RECORD_AUDIO` - mikrofoni, runtime-pyynto Meterissa.
- `POST_NOTIFICATIONS` - Android 13+ ilmoitukset, pyydetaan mittauksen
  kaynnistyksen yhteydessa tarvittaessa.
- `FOREGROUND_SERVICE` ja `FOREGROUND_SERVICE_MICROPHONE` - mikrofonin
  foreground service.
- `VIBRATE` - haptiikka.
- `com.android.vending.BILLING` - Google Play Billing.
- `android.permission.health.WRITE_EXERCISE` - Health Connect
  melusessiosynkkaus.
- `android.permission.health.READ_HEART_RATE` - Health Connect sykeoverlay.

---

## Design system ja tekstiresurssit

- Varit: dark/light-tokenit `ui/theme/Color.kt`:ssa. Paagradientti ja
  tonaaliset surface-tasot tulevat teeman kautta.
- Typografia: Manrope yleistekstissa ja Space Grotesk numeerisessa/datanaytossa.
- Muodot ja spacing: `Shape.kt` ja `Spacing.kt`; spacing nojaa 8dp-gridiin.
- Komponentit: mm. `DbCheckButton`, `DbCheckCard`, `DbCheckChip`,
  `DbCheckSlider`, `DbCheckToggle`, `ProLockOverlay`, `SessionCard`,
  `BottomNavBar`, `SkeletonLoader` ja `EmptyState`.
- Uudet design-arvot tulee keskittaa teemaan. Inline-varit, spacingit,
  animaatiokesto- ja card-oletukset ovat koodintarkistuksessa punaisia lippuja,
  jos niille on jo token.
- `app/src/main/res/values/strings.xml` sisaltaa nykyisin laajan
  default-English-resurssipohjan: 393 `string`-merkintaa ja 4
  `plurals`-merkintaa, mukaan lukien saavutettavuuskuvaukset.
- Kaannosarvokansioita ei ole: arvo-/teemakansioista loytyvat vain `values`
  ja `values-night`. Muut nykyiset `res`-hakemistot ovat `drawable`, `font`,
  `layout`, `mipmap-anydpi-v26` ja `xml`.

---

## Navigaatio

`DbCheckNavHost` kayttaa bottom navigationia puhelimella ja NavigationRailia,
kun nayton leveys on vahintaan 600dp.

| Reitti | Naytto | Nykyinen kayttaytyminen |
|---|---|---|
| `meter` | Meter | Start destination. Live gauge, waveform, Min/Avg/Max/Peak, Play/Pause, Reset ja Share. Pyytää `RECORD_AUDIO`-luvan ja Android 13+ ilmoitusluvan mittauksen kaynnistyksen yhteydessa. Kaynnistaa `MeasurementForegroundService`n; valmis normaali stop navigoi Session Detailiin `completedSessionIds`-eventista. |
| `analytics` | Analytics | Viikon energia-average-altistus Room-datasta, kuuloterveysstatus, Pro-gatettu live-spektri, Pro-gatettu 7 paivan Environment Mix, Pro-gatettu 30 paivan trendi, Pro-gatettu 12 kuukauden raportti ja hearing-test CTA. Free-kayttajalle Pro-kortit ovat locked-previewta ilman oikeaa Pro-dataa. |
| `history` | History | 24h-hourly chart, safe hours, viimeisimmat sessiot, View All -tila, SessionNamingSheet ja Session Detail -avaus. Free-kayttajan historia rajataan 7 paivaan `SessionHistoryPolicy`n kautta. |
| `history/detail/{sessionId}` | Session Detail | Sessioraportti, metadata, LAeq/equivalent-level-label, LCpeak, A-painotetuille sessioille TWA/dose/85 dBA peak events, time-series, PNG-jako, Pro-gatettu PDF-export ja Pro Health Connect -sykeoverlay. Suora reitti vanhaan sessioon lukitaan Free-kayttajalta. |
| `settings?showPro={showPro}` | Settings | Kalibrointi, frequency weighting, notifications, lock-screen meter, Health Connect, local backups, Pro-gatettu CSV-export, display/theme ja Pro-upsell. `showPro=true` scrollaa Pro-korttiin. Debug-buildissa Pro-kortissa on Force Free -toggle. |
| `hearing_test/setup` | Hearing Test Setup | Kuulotestin aloitusnaytto. Setup-ruutu ei itse lue Pro-tilaa; varsinainen testin suoritus estyy Free-tilassa `ActiveTestViewModel`issa. |
| `hearing_test/active` | Hearing Test Active | Pro-kayttajan tone-playback ja Hughson-Westlake-tyyppinen threshold-flow. Free-tilassa execution estetaan ViewModelissa. |
| `hearing_test/results/{testId}` | Hearing Test Results | Lataa ensisijaisesti route-argumentin `testId` tuloksen; fallback on latest result. Free-tilassa result-dataa ei nayteta eika jaeta. Share Results luo PNG-kortin ja tekstin Android Sharesheetiin. |

Top-level navigation palauttaa valitun stackin rootiin konservatiivisesti:
samassa top-level stackissa statea ei palauteta, eri top-level stackissa
`saveState`/`restoreState` on kaytossa.

---

## Free vs Pro

| Ominaisuus | Free | Pro | Nykytila koodissa |
|---|:---:|:---:|---|
| Live dB-mittari, waveform ja session stats | x | x | Kytketty Meterissa |
| Foreground measurement notification | x | x | Kytketty `MeasurementForegroundService`ssa |
| Melutasoilmoitukset ja threshold-asetus | x | x | Asetukset ja `NoiseAlertEvaluator` ovat koodissa; notification-policy on rajattu |
| Dark / Light / System -teema | x | x | DataStore + startup theme bootstrap |
| Waveform style Line/Filled/Bars | x | x | Free-asetus, vaikuttaa Meter UI:hin |
| Meter refresh rate High/Standard/Low | x | x | Free-asetus, vaikuttaa vain Meter UI -paivitysvali, ei AudioRecordiin tai Room-kadenssiin |
| 7 paivan historia | x | x | `SessionHistoryPolicy.FREE_HISTORY_WINDOW_MILLIS` |
| Rajoittamaton historia |  | x | History ja Session Detail rajaavat Free-kayttajan nakyman 7 paivaan; repositoryssa on seka raw-all-kyselyita etta gated listauspolkuja |
| Viikon altistumiskaavio ja kuuloterveys | x | x | Kytketty Room-dataan |
| Health Connect -melusessiosynkkaus | x | x | Free-kayttajallekin sallittu Settingsista |
| Mikrofoniherkkyyden kalibrointi |  | x | `ProAudioPreferencePolicy` ja Settings gate |
| Frequency weighting A/B/C/Z/ITU-R 468 |  | x | `ProAudioPreferencePolicy` ja AudioEngine |
| Dosimeter standard NIOSH REL / OSHA PEL |  | x | `DosimeterStandard`, DataStore, Settings state ja `DosimeterCalculator` NIOSH/OSHA-laskennalle |
| Lock-screen live meter |  | x | Custom RemoteViews notification |
| Health Connect -sykeoverlay |  | x | Session Detail + PDF heart-rate page |
| PDF-raportti |  | x | `CreateDocument("application/pdf")` + `ExportPdfReportUseCase` |
| Session Detail PNG -jakokortti | x | x | `ShareResultsGenerator.shareSessionReportCard()` |
| Kotinayton widget |  | x | Glance-widget Pro-gatella |
| Kuulotesti |  | x | Analytics CTA overlay, execution, save, results ja share gateattu; setup-ruutu ei itse gatea Pro-tilaa |
| CSV-vienti |  | x | Settings Data & Export |
| Session-nimeaminen ja tagit |  | x | History ja Session Detail |
| Live-spektrianalyysi |  | x | Raw PCM -datasta, ei persistointia |
| Environment Mix |  | x | 7 paivan Room-jakauma; Free saa locked-previewn |
| 30 paivan trendi |  | x | `ExposureAnalyticsCalculator` |
| 12 kuukauden raportti |  | x | `ExposureAnalyticsCalculator` + session count |

---

## Billing ja entitlement

- `BillingGateway.kt` on Settingsin ostovirran testattava rajapinta.
- `BillingManager` on gatewayn tuotantototeutus ja kasittelee yhden INAPP-
  tuotteen: `dbcheck_pro`.
- `BillingManager.isPurchased` alkaa arvosta `null`. `ProFeatureManager`
  synkkaa DataStoreen vain varmistetun `true`/`false`-ostotilan, jotta appin
  kaynnistys tai Play Billing -haun virhe ei ylikirjoita aiemmin tallennettua
  Pro-oikeutta Free-tilaan.
- `BillingManager.refreshPurchases()` kasittelee startup-/resume-snapshotit.
  `PURCHASED`-ostot acknowledgeataan tarvittaessa myos reconnect/refresh-
  polussa.
- `PurchaseEvent`: `Completed`, `Pending`, `Cancelled`, `AlreadyOwned` ja
  `Failed(reason)`.
- `ITEM_ALREADY_OWNED` laukaisee ostosnapshotin haun, jotta token ja mahdollinen
  acknowledge-puute saadaan kasiteltya.
- `PurchaseState.PENDING` ei avaa Pro-oikeutta.
- `domain/entitlement/ProEntitlementPolicy.kt` on effective entitlementin
  ainoa policy-lahde: release kayttaa ostotilaa; debug on Pro oletuksena,
  ellei debug-only `debugForceFreeEnabled` pakota Free-tilaa.
- `UserPreferences.isProUser` on UI:n ja domain-policyjen effective Pro-arvo.

---

## Audio engine ja mittaussessio

Audio-domain:

- `AudioProcessingConfig`: `SAMPLE_RATE = 44100`, `CHUNK_SIZE = 4096`,
  `FFT_SIZE = 4096`.
- `AudioRecordPolicies`: keskittaa AudioRecord-bufferin mitoituksen ja read-
  tulosten tulkinnan. Capture-buffer on suurempi kuin PCM16-read-chunk.
- `AudioEngine`: AudioRecord mono PCM16, permission check ennen tallennusta,
  `@RequiresPermission` AudioRecord-luonnissa, `StateFlow<SpectralFrame?>`
  live-spektrille.
- `DecibelCalculator`: RMS/peak -> dB, referenssi `32768.0`, offset `+90`,
  kalibrointioffset ja clamp 0-130 dB.
- `FrequencyWeightingFilter`: `A`, `B`, `C`, `Z`, `ITUR468`. A/B/C/ITU-R 468
  ovat 44.1 kHz:n SOS/biquad-kaskadeja. Painotettu signaali pysyy
  `DoubleArray`na dB-laskentaan asti, jotta positiiviset vahvistukset eivat
  leikkaudu PCM16-alueeseen.
- `AudioEngine.DecibelReading` kuljettaa raw RMS -arvon (`instantDb`),
  valitulla painotuksella lasketun RMS-arvon (`weightedDb`) ja C-painotetun
  peak-arvon (`peakDb`).
- `FFTProcessor`: 4096-point radix-2 FFT, Hann window, DC-bin ohitus
  dominanttitaajuushaussa.
- `SpectralAnalyzer`: 24 logaritmista 20 Hz-20 kHz bandia, dominantti taajuus
  ja bandwidth-luokka raw PCM16 -chunkista.
- `ToneGenerator`: AudioTrack MODE_STATIC sine wave ja 50 ms fade in/out
  kuulotestille.

Session orchestration:

- `MeasurementForegroundService` kutsuu `ServiceCompat.startForeground(...)`
  ensin ja kaynnistaa `AudioSessionManager.startSession()`-polun vasta, jos
  foreground-promootio onnistuu.
- Foreground service palauttaa onnistuneestakin kaynnistyksesta
  `START_NOT_STICKY`; prosessin tappamisen jalkeen AudioRecord-sessiota ei
  yriteta herattaa automaattisesti.
- `AudioSessionManager.startSession()` palauttaa `true` vasta, kun
  `AudioEngine.startRecording(...)` on saanut AudioRecordin kayntiin ja
  julkaissut `onRecordingStarted`-callbackin.
- `AudioSessionManager` kayttaa `Mutex`eja session lifecycleen ja measurement
  flushiin. Stop/completion odottaa kaynnissa olevan flushin loppuun.
- `SessionStats.avgDb` on energia-average painotetuista lukemista.
  `minDb`/`maxDb` ovat weighted-arvoja ja `peakDb` on C-painotettu LCpeak.
- `AudioSessionManager.liveExposureState` on aktiivisen session live-dosimeter-
  tila. Se paivittyy jokaisesta `DecibelReading.aWeightedDb`-lukemasta,
  laskee A-painotetun LAeq-arvon ja lukee NIOSH_REL/OSHA_PEL TWA-, dose-,
  projected dose- ja remaining exposure time -arvot `DosimeterCalculator`ista.
- `MeterUiState.measurementMode` kertoo Meterin `DB_METER` / `DOSIMETER`
  -valinnan. `MeterViewModel.setMeasurementMode(...)` paivittaa vain UI-statea;
  se ei kaynnista tai pysayta mittausta.
- `MeasurementPersistenceSampler` tallentaa Roomiin kiintealla 1s cadencella,
  mutta pakottaa persistoinnin ensimmaiselle lukemalle,
  `NoiseLevel.ELEVATED.maxDb` / 85 dB boundary-crossingille, uudelle weighted
  maxille, uudelle LCpeak maxille ja stopin viimeiselle tallentamattomalle
  lukemalle.
- `MeterRefreshRate` (`HIGH = 100 ms`, `STANDARD = 250 ms`, `LOW = 1000 ms`)
  throttlettaa vain Meter UI -paivityksia. Se ei muuta AudioRecordin 44.1 kHz
  sample ratea, 4096 sample chunkia, painotusfiltterin tilaa tai Roomin 1s
  persistointikadenssia.
- `AudioSessionManager.completedSessionIds` ajaa normaalin stopin jalkeisen
  Session Detail -navigoinnin. Reset- ja failure-polut viimeistelevat session
  hiljaisesti ilman auto-navigointia.

---

## Tietokanta ja preferenssit

Room database: `DbCheckDatabase`, `SCHEMA_VERSION = 4`, `exportSchema = true`.
Skeematiedostot ovat `app/schemas/.../1.json`, `2.json`, `3.json` ja `4.json`.

Migraatiot:

- `MIGRATION_1_2`: lisaa `sessions.activeSlot`, varmistaa yhden aktiivisen
  session slotin, sulkee ylimaaraiset aktiiviset sessiot ja luo deterministiset
  indeksit sessioille, mittauksille ja hearing-test-resultseille.
- `MIGRATION_2_3`: lisaa `measurements.peakDb` -sarakkeen ja backfillaa vanhat
  rivit `dbWeighted`-arvolla.
- `MIGRATION_3_4`: lisaa `measurements.aWeightedDb`- ja `measurements.responseTime`
  -sarakkeet. Vanhat rivit backfillataan arvoilla `aWeightedDb = dbWeighted` ja
  `responseTime = FAST`.

Entiteetit:

- `sessions`: `id`, `startTime`, `endTime`, `minDb`, `avgDb`, `maxDb`,
  `peakDb`, `name`, `emoji`, `tags`, `isActive`, `activeSlot`,
  `frequencyWeighting`.
- `measurements`: `id`, `sessionId`, `timestamp`, `dbValue`, `dbWeighted`,
  `peakDb`, optional `frequencyData`.
- `hearing_test_results`: `id`, `timestamp`, `overallScore`, `rating`,
  `leftEarData`, `rightEarData`, `speechClarity`, `highFreqLimit`,
  `avgThreshold`.

Repository/dataflow:

- `SessionRepository.recordActiveSessionMeasurements(...)` kirjoittaa
  measurement-rivit ja aktiivisen session runtime-summaryn samassa Room
  transactionissa.
- `SessionRepository.completeSessionWithMeasurements(...)` kirjoittaa
  viimeiset rivit ja sulkee session samassa transactionissa.
- `MeasurementRepository` on nykyisin read/analytics-repository. Se palauttaa
  hourly/daily/weighted/environment mix -virtoja ja tekee energia-average-
  mappaukset domain-malleihin.
- DAO-kyselyissa on deterministiset `ORDER BY` -tie-breakerit, joissa
  aikaleiman lisaksi kaytetaan primary keyta.

DataStore-preferenssit:

- `theme_mode`
- `exposure_alerts`
- `peak_warnings`
- `notification_threshold`
- `mic_sensitivity_offset`
- `frequency_weighting`
- `dosimeter_standard`
- `waveform_style`
- `refresh_rate`
- `lockscreen_meter`
- `health_connect`
- `heart_rate_overlay`
- `debug_force_free`
- `is_pro_user`

`UserPreferenceDefaults` keskittaa defaultit ja normalisoinnin. Pro-mittausarvot
luetaan effective-arvoina `ProAudioPreferencePolicy`n kautta, joten Free-
kayttajan tallennettu vanha calibration, weighting, response time tai dosimeter
standard ei vaikuta mittauspolkuihin.

---

## Analytics, History ja Session Detail

Analytics:

- `MeasurementRepository.getDailyAveragesLast7Days()` tuottaa viikon
  energia-average-paivapisteet.
- `AnalyticsViewModel` laskee weekly average -arvon, kuuloterveysstatuksen
  (`SAFE`, `WARNING`, `DANGER`) ja today-vs-week-prosentin.
- Pro-kayttajalle Environment Mix lukee 7 paivan Room-countit
  `MeasurementRepository.getEnvironmentMixLast7Days()`-polusta.
- Pro-kayttajalle 30 paivan trendi ja 12 kuukauden raportti lasketaan
  `ExposureAnalyticsCalculator`illa. Ikkunat paivittyvat minuutin tickilla.
- Free-kayttajalle Pro-analytiikka palauttaa `LockedPreview`-tilat, ei oikeaa
  dataa overlayn alle.
- Live-spektri naytetaan Pro-kayttajalle `AudioEngine.spectralFrame`-virrasta;
  spektria ei persistoda `measurements.frequencyData`-kenttaan.
- Analyticsin datavirran latausvirhe mapataan `AnalyticsUiState.Error`-
  tilaksi, joka nayttaa resursoidun fallback-viestin ja CTA:n Meteriin.

History:

- `HistoryViewModel` yhdistaa 24h-hourly-averaget, sessiot, Pro-tilan ja
  View All -tilan.
- `SessionRepository.getSessions()` on Pro-aware listauspolku, mutta
  `HistoryViewModel` kayttaa nykyisin myos raw-all-polkuja ja rajaa
  Free-kayttajan sessiolistan paikallisesti
  `SessionHistoryPolicy.FREE_HISTORY_WINDOW_MILLIS` -ikkunaan.
- Session Detail lukee routeargumentin session ensin raw-kyselylla ja lukitsee
  vanhan session raporttinakyman Free-kayttajalta saman 7 paivan policy-ikkunan
  perusteella.
- `HistoryViewModel.saveSessionMetadata(...)` ei kirjoita metadataa, ellei
  kayttaja ole Pro.
- Historyn latausvirhe mapataan `HistoryUiState.Error`-tilaksi. Metadata-
  tallennuksen virhe naytetaan erillisena `metadataErrorMessage`-viestina
  onnistuneen History-sisallon sisalla.
- `SessionMetadata` normalisoi nimen, emojin, tagit ja export-slugit.
  Tagit rajataan kuuteen 24 merkin tagiin ja duplicate-tagit poistetaan
  case-insensitive.

Session Detail:

- `SessionDetailViewModel` lukee `sessionId`-routeargumentin `SavedStateHandle`sta.
- Raportti syntyy `SessionReportCalculator`illa sessiosta ja measurement-
  riveista.
- `equivalentLevelLabelForWeighting(...)` erottaa A/B/C/Z/ITU-R 468 -tasot
  raporttiteksteissa.
- `domain/noise/DosimeterCalculator.kt` laskee NIOSH_REL- ja OSHA_PEL-altistuksen
  TWA-, dose-, projected dose- ja remaining exposure time -arvot yhteista
  completed report / live flow -kayttoa varten. Completed report nayttaa
  nykyiset NIOSH_REL TWA/dose -kentat, ja `AudioSessionManager.liveExposureState`
  kayttaa samaa laskuria aktiivisen session live-arvoihin.
- NIOSH 8h TWA, NIOSH dose ja 85 dBA peak-event-lista ovat saatavilla vain
  A-painotetuille sessioille. Muilla painotuksilla arvot puuttuvat tietoisesti
  eivatka nay nollina.
- Heart-rate overlay latautuu vain, kun kayttaja on Pro, asetus on paalla,
  Health Connect on saatavilla ja `READ_HEART_RATE` on myonnetty.
- Session Detail sailyttaa lukitus-/puuttuva-sessio-tilat eksplisiittisina
  unavailable-tiloina ja mapittaa load/share/PDF/metadata-virheet
  resursoituihin `errorMessage`-viesteihin.

---

## Health Connect

Integraatioadapteri on `sync/HealthConnectManager.kt`. UI kayttaa sita
`service/HealthConnectService.kt`-portin kautta.

- Saatavuus tarkistetaan `HealthConnectClient.getSdkStatus(context,
  "com.google.android.apps.healthdata")`-polulla.
- Permission-setit:
  - Noise sync: `HealthPermission.getWritePermission(ExerciseSessionRecord::class)`
  - Heart rate read: `HealthPermission.getReadPermission(HeartRateRecord::class)`
- Melusession kirjoitus:
  - record type: `ExerciseSessionRecord`
  - `exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT`
  - `Metadata.clientRecordId = "noise_dose_<date>_session_<session.id>"`
  - `Metadata.recordingMethod = RECORDING_METHOD_ACTIVELY_RECORDED`
  - notes-kenttaan kirjataan `SessionReportData`sta equivalent-level-label ja arvo, max, LCpeak seka weighting-label.
- `AudioSessionManager.publishCompletionSideEffects(...)` kutsuu
  `HealthConnectManager.writeNoiseDose(...)` vain normaalissa completionissa ja
  vain, jos `healthConnectEnabled` on paalla.
- Kuulotestin `HealthConnectManager.writeHearingTestResult(...)` palauttaa
  tarkoituksella `Skipped`, koska natiivia audiometriatietuetta ei ole.
- `HealthConnectService.readHeartRateForSession(...)` mapittaa Health Connectin
  samplet Session Detailin UI-stateen ja PDF:n `ReportHeartRateSection`iin.
- Health Connect -status kantaa `errorMessage`-kenttaa, jos saatavuus- tai
  permission-tarkistus epaonnistuu. Settings ja Session Detail nayttavat sen
  resursoituna kayttajaviestina eivatka piilota status-tarkistuksen virhetta.
- Settingsissa Install/Update-toiminto avaa Health Connectin Play Store
  -sivulle `market://details?id=com.google.android.apps.healthdata` -intentilla,
  jos Health Connect puuttuu tai vaatii paivityksen.
- Settingsissa Manage-toiminto avaa Health Connectin hallintanakymaan
  `HealthConnectClient.getHealthConnectManageDataIntent(...)`-intentilla.

---

## Kuulotesti

- Domain-proseduuri on `domain/hearingtest/HearingTestProcedure.kt`.
- `ActiveTestViewModel` ohjaa tone playbackia ja kayttajavastetta; se ei
  kaynnista testia Free-tilassa.
- `HearingTestService.saveCompletedTest(...)` tarkistaa Pro-oikeuden ennen
  tallennusta.
- `HearingTestRepository` tarjoaa `getResultById(id)` ja `getLatestResult()`.
- Results-naytto lataa ensisijaisesti `hearing_test/results/{testId}`-
  reittiargumentin tuloksen. `getLatestResult()` on fallback, jos argumentti
  puuttuu.
- Results-naytto erottaa latausvirheen, Pro-lukituksen ja puuttuvan tuloksen
  omiksi content modeiksi. Share- ja tone playback -virheet naytetaan
  resursoituina fallback-viesteina.
- Share Results rakentaa PNG-kortin ja saatetekstin
  `ShareResultsGenerator.shareHearingTestResults(...)`-polulla.
- Tulokset ovat suhteellisia appin tone-output / dBFS -tasoja, eivat
  kalibroitua kliinista dB HL -audiometriaa.

---

## Raportointi, export ja jakaminen

Session report:

- `SessionReportCalculator` laskee equivalent-level-arvon, durationin, LCpeakin,
  A-painotetun TWA/dosen, time-series-pisteet ja A-painotetut 85 dBA
  peak-event-jaksot.
- `SessionReportData` sisaltaa myos session custom-nimen, emojin ja tagit.
- `PdfChartRenderer` keskittaa PDF Canvas -kaavion ja Session Detailin staattisen
  Compose-kaavion koordinaattimuunnoksen.

PDF:

- Session Detail kayttaa `ActivityResultContracts.CreateDocument("application/pdf")`.
- `ExportPdfReportUseCase` kirjoittaa natiivin `PdfDocument`in:
  4 sivua normaalisti, 5 sivua kun `ReportHeartRateSection.enabled` on true.
- Sivut: summary, metrics, time series, peak events ja optional heart rate.

PNG / Sharesheet:

- `ShareResultsGenerator.shareSessionStats(...)` on Meterin text/plain-share.
- `ShareResultsGenerator.shareHearingTestResults(...)` rakentaa hearing-test
  PNG-kortin.
- `ShareResultsGenerator.shareSessionReportCard(...)` rakentaa Session Detailin
  PNG-raporttikortin.
- PNG-jaot kirjoitetaan `cache/exports/`-hakemistoon ja julkaistaan
  `FileProvider`in `content://`-URIlla.
- Jakointentit antavat valiaikaisen lukuoikeuden seka `EXTRA_STREAM`in etta
  `ClipData`n / `FLAG_GRANT_READ_URI_PERMISSION`in kautta.

CSV:

- Settingsin Data & Export kutsuu `SettingsViewModel.createCsvExportIntent()`.
- `ExportCsvUseCase` kirjoittaa kaksi CSV-tiedostoa:
  sessioyhteenvedon ja mittausrivit.
- CSV-sarakkeissa ovat metadata-kentat `session_name`, `session_emoji` ja
  `session_tags`; measurement-exportissa myos `peak_db`.
- Mittausrivit luetaan sivuina
  `MeasurementDao.getMeasurementsForSessionExportPage(...)`-polulla, jotta
  export ei rakenna koko raw-aineistoa muistiin.
- CSV-jako kayttaa `ACTION_SEND_MULTIPLE`-intentia ja FileProvider-URIja.

Export cache:

- `ExportFileCache` kayttaa `cache/exports/`-hakemistoa ja omistaa seka
  FileProvider authority suffixin etta XML-polun runtime-sopimuksen.
- Yli 24 tuntia vanhat export/share-tiedostot poistetaan seuraavan exportin tai
  share-operaation yhteydessa.

---

## Local backup ja restore

- `sync/BackupGateway.kt` on backup-infrastruktuurin testattava rajapinta.
- `service/BackupService.kt` on Settingsin UI-facing backup-portti.
- `sync/LocalBackupManager.kt` toteuttaa varsinaiset paikalliset backupit
  `filesDir/backups`-hakemistoon.
- Backup tekee Roomille `PRAGMA wal_checkpoint(TRUNCATE)` ennen
  `dbcheck.db`-tiedoston kopiointia.
- Restore validoi valitun backupin ennen nykyisen tietokannan korvaamista.
- Restore luo `dBcheck_pre_restore_*`-turvakopion ennen korvausta ja validoi
  myos safety backupin.
- Restore poistaa vanhat `dbcheck.db-wal`- ja `dbcheck.db-shm`-sidecarit ennen
  korvaavaa tietokantatiedostoa.
- Backup/restore-operaatiot sarjallistetaan `Mutex`illa.
- Settings estaa backup- ja restore-toiminnot aktiivisen mittauksen aikana.
- Onnistunut restore emittoi `SettingsEvent.RestartAfterRestore`, jonka
  `MainActivity` toteuttaa prosessin restartilla.
- Google Drive -backupia ei ole nykyisessa koodissa.

---

## Widget ja ilmoitukset

Glance-widget:

- Receiver: `DbCheckWidgetReceiver`, `exported=false`.
- Widget provider XML: `app/src/main/res/xml/widget_info.xml`.
- Paivitysvali XML:ssa: 30 min.
- Pro + sessiodata: nayttaa viimeisimman session avg dB -arvon,
  melutasotunnisteen ja suhteellisen ajan.
- Pro + ei sessiodataa: nayttaa tyhjatilan.
- Free: nayttaa Pro-lukitun tilan.
- Latausvirhe: nayttaa erillisen widget error -tilan, jos preferenssi- tai
  sessiodatan luku epaonnistuu.
- Widget paivitetaan session completionin ja Pro-oikeuden muuttumisen yhteydessa.

Notificationit:

- `NotificationHelper` rakentaa measurement notificationin.
- `NotificationPrivacyPolicy.measurementLockscreenVisibility()` palauttaa
  `NotificationCompat.VISIBILITY_PRIVATE`; live dB -sisaltoa ei julkaista
  public-lockscreen-sisaltna.
- Pro + `lockscreenMeterEnabled` kayttaa custom collapsed/expanded
  `RemoteViews`-layoutteja, joissa nakyvat current dB, peak dB, kesto ja
  noise-level-piste.
- Free tai lockscreen-asetus pois paalta kayttaa tavallista private
  measurement notificationia.
- `MeasurementForegroundService.stopIntent(...)` kayttaa
  `ACTION_STOP_MEASUREMENT`ia ja `EXTRA_EMIT_COMPLETED`-lippua, jotta reset ei
  julkaise normaalia completion-navigointia.

---

## Testit

Source setit nykyisessa checkoutissa:

- `main`
- `test`
- `screenshotTest`
- `screenshotTestDebug`

`androidTest`-hakemistoa ei ole nykyisessa checkoutissa.

Unit-testit:

- `app/src/test/java/com/dbcheck/app` sisaltaa **91 Kotlin-lahdetiedostoa**
  unit-testien ja testiapurien alla.
- Kattavuusalueet: Billing, ProFeatureManager startup, CSV/export/cache,
  Room schema/DAO/query contract, DataStore mapping, repository rolling
  windows/transactions/history policy, domain audio/math/weighting/FFT/spectral,
  hearing-test procedure/result scoring, report calculator, session metadata,
  privacy config, foreground service policy, AudioSessionManager start/failure,
  notification policy/helper/noise-level, Health Connect payload/manager/mapper,
  LocalBackupManager, accessibility plural resources, analytics/history/meter/
  settings ViewModelit, navigation policy, PDF chart rendering, report text,
  share generation, string resource ids, user-facing error mapping and widget
  state.

Screenshot-testit:

- `ComponentScreenshotTests.kt` sisaltaa **17 `@PreviewTest`-funktiota**.
- `app/src/screenshotTestDebug/reference/...` sisaltaa **17 baseline-PNG:tä**.
- Screenshot-source set on kytketty AGP:n kokeellisella
  `android.experimental.enableScreenshotTest = true` -asetuksella.
- UI-komponenttien animaatioita voi poistaa screenshot-determinismia varten
  esim. `animationsEnabled=false`-parametreilla.

Keskeisia nykyisia regressiosuojia:

- `RoomSchemaContractTest` - Room schema version/migrations/schema contract.
- `SessionRepositoryTransactionContractTest` - session summary + measurement
  write transaction contract.
- `AudioSessionManagerAudioStartTest` - AudioRecord start/failure behavior.
- `MeasurementForegroundServicePolicyTest` - foreground service start/stop policy.
- `MeterStartupPermissionPolicyTest` - startup permission prompts.
- `ProAudioPreferencePolicyTest` - Free/Pro effective audio preferences.
- `HearingTestServiceProGateTest` - hearing-test execution/save gate.
- `ResultsViewModelShareTest` - hearing-test share gate and intent path.
- `SessionDetailScreenActionTest` and `SessionDetailViewModelMetadataTest` -
  PDF/metadata/Pro action contracts.
- `PrivacyConfigTest` - backup/fileprovider/privacy config.
- `LocalBackupManagerTest` - local backup/restore validation.
- `HealthConnectManagerTest`, `HealthConnectNoiseDosePayloadTest`,
  `HealthConnectHeartRateMapperTest` - Health Connect contracts.
- `PluralAccessibilityResourceTest` - pluralized accessibility strings.
- `UserFacingErrorTest` - teknisia exception-viesteja ei kayteta
  kayttajalle naytettavina virheina.

Taman dokumenttipaivityksen yhteydessa **ei ajettu Gradle-testisuitea** eika
projektin `lc`/`sc` wrapper-skripteja.

---

## Lint, analyysi ja paikalliset wrapperit

Projektin AGENTS.md ohjeistaa:

- `lint-check` / `lc`: kayttajan ajama skripti, joka ajaa ktlint + detekt +
  Android lint ja kirjoittaa tulokset `reports/`-hakemistoon.
- `security-check` / `sc`: kayttajan ajama skripti, joka ajaa Semgrepin ja
  OWASP Dependency-Checkin ja kirjoittaa tulokset `reports/`-hakemistoon.
- Kun kayttaja sanoo "lue lint-tulokset", luetaan `reports/ktlint.txt`,
  `reports/detekt.txt` ja `reports/lint.txt`.
- Kun kayttaja sanoo "lue security-tulokset", luetaan
  `reports/security-code.txt` ja `reports/security-deps.txt`.
- `sentry` tarkistaa debug-only Sentryn: debug-luokkapolussa pitää olla
  `io.sentry`, release-luokkapolussa ei saa olla `io.sentry`a, ja raportti
  kirjoitetaan `reports/sentry.txt`-tiedostoon.
- Agentti ei aja `lc`/`sc`-skripteja itse ilman kayttajan pyyntoa.
- `reports/` on gitignoressa eika sita commitoida.

Repo-local wrapperit `tools/`-hakemistossa delegoivat
`C:\Dev\Android-check\tools\InvokeProjectCheck.ps1` -polkuun. Nykyinen
wrapper-inventaario: `ac`, `ad`, `cr`, `cs`, `db`, `dc`, `ds`, `ga`, `lc`,
`ms`, `os`, `pc`, `ql`, `sc`, `security-check`, `sentry`, `sonar`, `ss`.

Staattinen konfiguraatio:

- `.editorconfig`: `ktlint_code_style = android` ja Compose-funktioiden
  nimeamissaannon annotated-poikkeus.
- `config/detekt/detekt.yml`: LongMethod 80, MaxLineLength 120, MagicNumber
  pois, wildcard imports pois, UnusedPrivate* paalla, Compose-funktioiden
  nimeamissaanto rajattu UI:sta.
- Detektin ktlint-wrapperista poistetaan kaytosta puhtaasti tyylillisia
  formatointisaantoja, joiden oletukset eivat vastaa Android Studio
  -formatointia.
- `app/build.gradle.kts`: `ktlintCheck` on alias, joka riippuu `detekt`-
  taskista.
- Dependency locking on paalla root-projektin `allprojects`-tasolla.
- `app/build.gradle.kts` pinnaa useita transitiivisia build-/scanner-
  riippuvuuksia korjattuihin versioihin security-checkin vaatimusten vuoksi.

---

## CI/CD

GitHub Actions -workflowt nykyisessa repossa:

| Workflow | Tiedosto | Tarkoitus |
|---|---|---|
| Android Static Checks | `.github/workflows/lint.yml` | `:app:ktlintCheck`, `:app:detekt`, `:app:lint` main-pushissa, PR:ssa ja manual dispatchissa |
| CodeQL | `.github/workflows/codeql.yml` | Java/Kotlin CodeQL, JDK 21, Android SDK, manual `assembleDebug`, maanantain schedule |
| Security Analysis | `.github/workflows/security.yml` | Semgrep pinned container + project config + SARIF upload; OWASP Dependency-Check Gradle task + SARIF upload, maanantain schedule |
| SonarCloud | `.github/workflows/sonar.yml` | `assembleDebug`, `jacocoDebugUnitTestReport`, Gradle `sonar` |
| Qodana | `.github/workflows/qodana.yml` | JetBrains Qodana action v2026.1.0, `continue-on-error: true` AGP 9.1.0 -yhteensopivuuskommentin vuoksi |
| Android Release Build | `.github/workflows/release-build.yml` | PR:ssa unsigned release APK/AAB; pushissa signed build jos release secrets ovat olemassa; apksigner/jarsigner verification |

Sonar:

- `sonar.projectKey = Insaner1980_dBcheck`
- `sonar.organization = insaner1980`
- coverage XML:
  `app/build/reports/jacoco/debugUnitTest/jacocoDebugUnitTestReport.xml`
- root `build.gradle.kts` antaa Sonarille Gradle-managed source/binary/coverage
  -polut ja lukee muut arvot `sonar-project.properties`-tiedostosta.

Qodana:

- `qodana.yaml`: `jetbrains/qodana-jvm-android:2026.1`
- profiili: `qodana.recommended`
- mukana `CheckDependencyLicenses`.

Release signing:

- Release signing lukee Gradle propertyt tai environment-muuttujat:
  `DBCHECK_RELEASE_STORE_FILE`, `DBCHECK_RELEASE_STORE_PASSWORD`,
  `DBCHECK_RELEASE_KEY_ALIAS`, `DBCHECK_RELEASE_KEY_PASSWORD`.
- Jos osa release signing -arvoista on annettu mutta ei kaikkia, Gradle failaa
  eksplisiittisesti.
- Salaisuuksia tai keystorea ei saa commitoida.
- Debug-only Sentry DSN kuuluu `DBCHECK_SENTRY_DSN`-/`SENTRY_DSN`-ympäristömuuttujaan tai ignored `debug.credentials.properties` -tiedostoon avaimella `sentry.dsn`; Sentry Gradle -pluginia, replayta, tracingia, logcat breadcrumbseja tai release crash reportingia ei ole kytketty.

---

## Kehitysymparisto

Windows/PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat :app:validateDebugScreenshotTest
```

Linux/WSL:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew lintDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:validateDebugScreenshotTest
```

Asennus laitteelle:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Vaatii JDK 21:n ja Android SDK:n API 36 -tuen.

---

## Toteutusvaiheet nykykoodin perusteella

### Phase 1 - MVP

Toteutettu paaosin:

- Projektirakenne, Hilt, Room v3, DataStore.
- Design system ja komponenttikirjasto.
- Meter, Analytics, History ja Settings.
- AudioRecord-pohjainen live-mittaus.
- Foreground service mikrofonityypilla.
- Google Play Billing -backend, Settingsin ostovirta ja Pro-gating.
- Debug-only Force Free -toggle Pro-gatejen testaamiseen.

### Phase 2 - Enhancement

Toteutettu tai kytketty merkittavilta osin:

- Kuulotesti-flow ja tulosten tallennus Pro-kayttajalle.
- FFTProcessor ja SpectralAnalyzer Pro-gatettuun live-spektrikorttiin.
- SessionNamingSheet Historyssa ja Session Detailissa.
- CSV-export Settingsissa.
- Glance-widget Pro-gatella.
- Hearing Results -jakaminen PNG-share-polulla.
- Laaja strings.xml-resursointi default English -kielella.

### Phase 3 - Polish

Osittain toteutettu:

- LocalBackupManager paikallisiin backup/restore-toimintoihin.
- ShareResultsGenerator tekstille, hearing-test PNG:lle ja Session Detail PNG:lle.
- MonthlyTrendChart ja YearlyReportCard Pro-gatettuun analytics-dataflow'hun.
- PDF-reportti Session Detailista.
- Screenshot baseline -testit kriittisille Compose-komponenteille.

Puute: pilvi-/Google Drive -backupia ei ole.

### Phase 12 - kilpailukykyominaisuudet

Osittain toteutettu:

- Health Connect -melusessiosynkkaus ja sykeoverlay.
- B-painotus ja ITU-R 468 -painotus A/C/Z:n lisaksi.
- Lock-screen live meter custom notificationina.
- Session Detail -nakymä.
- PDF-raportti ja Session Detail PNG-jako.
- LCpeak ja `measurements.peakDb` schema v3:ssa.

Puute: Health Connectissa ei ole natiivia melu- tai audiometriatietuetta, joten
melu mallinnetaan exercise sessionina ja kuulotestin synkkaus skipataan.

---

## Koodintarkistuksen kannalta kriittiset sopimukset

Nama ovat hyvia kysymysaiheita tuleviin code review -kierroksiin:

- Foreground service: kutsutaanko `startForeground()` ennen AudioRecord-session
  aloitusta, ja kasitellaanko Android 14+ microphone/while-in-use-rajoitus
  oikein?
- Pro gates: onko gate UI:n lisaksi execution/data-polussa? Erityisesti
  hearing test, CSV, PDF, metadata, Pro-audioasetukset ja history direct-open.
- Audio math: erotetaanko raw RMS, weighted RMS ja C-painotettu LCpeak?
  Eivatko raportit kayta raw RMS:aa LCpeak- tai A-weighted event -laskentaan?
- Refresh rate: vaikuttaako muutos vain UI-paivitykseen, ei AudioRecordiin,
  filter-stateen tai Room-persistointiin?
- Room consistency: kirjoitetaanko measurement-rivit ja session summary samassa
  transactionissa completion/flush-polussa?
- Recovery: suljetaanko edellisen prosessin aktiivinen sessio hiljaisesti
  ilman valheellista completion-navigointia?
- File sharing: jaetaanko vain `cache/exports/` FileProvider-URIja, ja annetaanko
  lukuoikeus seka `EXTRA_STREAM`in etta `ClipData`n kautta?
- Backup/restore: validoidaanko backup ennen korvausta, tehdaanko safety backup
  ja poistetaanko WAL/SHM-sidecarit?
- Health Connect: pysyyko noise sync `ExerciseSessionRecord`-mallissa ja
  hearing test no-opina, ellei Android tarjoa oikeaa datatyyppia?
- User-facing errors: kayttavatko uudet virhepolut resursoituja fallback-
  viesteja `toUserFacingMessage(...)`-polun kautta, eivat raakaa exception-
  tekstia?
- Localization/accessibility: ovatko uudet user-facing tekstit resursoituja ja
  onko kaavioille/ikonitoiminnoille semanttinen kuvaus?
- CI/security: paivitetaanko dependency verification / lockfile / SARIF-polut,
  jos build- tai scanner-riippuvuuksia muutetaan?

---

## Tunnetut rajoitukset ja riskit

- dB-laskenta perustuu laitteen mikrofoniin ja sovelluksen laskennalliseen
  kalibrointiin. Ilman laitekohtaista kalibrointia tuloksia ei pideta
  mittalaitetasoisina SPL-arvoina.
- Kuulotestin kynnykset ovat suhteellisia appin tone-output / dBFS -arvoja,
  eivat kalibroitua dB HL -audiometriaa. Tulokset sopivat korkeintaan
  henkilokohtaiseen seurantaan, eivat kliiniseen diagnoosiin.
- `speechClarity` ja `highFreqLimit` ovat sovelluksen arvioita/simplifikaatioita.
- A/B/C/ITU-R-painotusten kertoimet ovat koodissa ja niille on unit-testeja,
  mutta kattava mittalaitereferenssi- tai scipy/MATLAB-verifiointi puuttuu.
- Health Connect -melu tallennetaan exercise sessionina, koska natiivia
  melualtistusrecordia ei ole. Kuulotestin Health Connect -kirjoitus on no-op.
- Google Drive -backupia ei ole; nykyinen backup on paikallinen
  `filesDir/backups`-ratkaisu.
- `androidTest`-instrumentaatiotesteja ei ole nykyisessa checkoutissa.
- Screenshot-testit ovat olemassa, mutta ne eivat korvaa laitetason
  navigation/permission/share/billing-testausta.
- Default-English-tekstit on laajasti resursoitu, mutta kaannosresursseja ei
  ole. Lokalisointi ei ole valmis.
- Saavutettavuudelle on semanttisia chart-/button-kuvauksia ja plural-resource-
  testejä, mutta kattavaa accessibility-auditointia ei ole tehty.
- Qodana workflow on `continue-on-error`, koska nykyisessa workflow-kommentissa
  todetaan Qodana 2026.1:n Android-importin tukevan enintaan AGP 9.0.0-alpha06,
  kun projekti kayttaa AGP 9.1.0:aa.
- Release signing on konfiguroitu, mutta Play Store -julkaisua varten
  tarvittavat salaisuudet, tuoteasetukset, policy-tekstit ja laitetason
  regressioverifiointi tulee tarkistaa erikseen.

---

## Referenssitiedostot

| Tiedosto | Tarkoitus |
|---|---|
| `AGENTS.md` | Paikalliset tyoskentely-, lint- ja memory-ohjeet |
| `STATUS.md` | Projektin tilanne-/jatkomuisti |
| `dBcheck_design_spec.md` | Designin nykyinen referenssi |
| `dBcheck_complete_spec_v2.md` | Laajempi tuotemaarittely |
| `dBcheck_competitive_features_addendum.md` | Kilpailukykyominaisuuksien lisamaarittely |
| `design_evolution_spec.md` | Design-kehityksen lisamuistiinpanot |
| `dbcheck-privacy-policy.md` | Privacy policy -luonnos |
| `pro-kytkentä.md` | Pro-kytkennan muistiinpano |
| `memory/MEMORY.md` | Projektin arkkitehtuuri- ja sessionmuisti |
| `images/*.png` | Visuaaliset referenssit |

---

## Project management

- GitHub: https://github.com/Insaner1980/dBcheck
- Linear project: https://linear.app/loikka1/project/dbcheck-0336faa49e71
- Milestone: v1.0 - Play Store Release
