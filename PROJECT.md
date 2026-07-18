# dBcheck

**Premium Android-desibelimittari ja kuuloterveys-sovellus.**

Paivitetty nykyisen checkoutin perusteella: **2026-07-16**.

dBcheck on Kotlin / Jetpack Compose -sovellus, joka mittaa ympariston melua
reaaliajassa, tallentaa melualtistussessioita, nayttaa analytiikkaa, tarjoaa
Pro-gatetun suhteellisen kuulotestin ja recovery-checkin, rakentaa sessioista
jaettavia raportteja ja sisaltaa useita rajattuja Pro-lisapolkuja, kuten
Camera Overlayn, WAV-exportin, ambient sound playbackin, tinnitus pitch
-profiilin, live sound detectionin ja voice/TTS-riskikehotteet.
Visuaalinen identiteetti on "Auditory Observatory": rauhallinen, editorial
wellness -henkinen mittari, ei geneerinen tyokaluapp.

Nykytila: runko ja iso osa v1.0-ominaisuuksista on toteutettu. Meter,
Trends, Hearing-hubi, History, Session Detail, Settings, Health Connect, local backup,
CSV/PDF/PNG/WAV-exportit, Pro-entitlement, hearing-test-flow, Sleep Monitor,
passive monitoring ja paikallinen ambient playback ovat koodissa kytkettyja.
Sovellus ei ole viela julkaisukypsa ilman laitetason audio-, permission-,
foreground-service-, Billing-, Play Console-, release-signing- ja
saavutettavuusverifiointia seka akustisten/klinisten rajojen lopullista
dokumentointia.

Tama dokumentti kuvaa nykyista koodia, ei tavoitetilaa. Väitteiden lähdehierarkia
on: nykyinen tuotantokoodi ja build-konfiguraatio -> nykyiset testit, Room-skeemat
ja QA-artifactit -> tämä dokumentti. Vanhemmat suunnitelmat ja speksit eivät
ohita toteutusta. Koodintarkistuksessa symboli, tiedostopolku ja testi pitää aina
varmistaa nykyisestä checkoutista, koska tämä tiedosto on tarkka tilannekuva,
ei itsenäinen rajapintatakuu tuleville muutoksille.

---

## Ulkoiset tarkistukset 2026-07-16

Projektin ohjeen mukaan ulkoisesti muuttuvat Android-kaytannot tarkistettiin
virallisista lahteista ennen dokumenttipaivitysta:

- Lahteet:
  [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types),
  [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types),
  [Android 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes) ja
  [MediaPipe Audio Classifier for Android](https://ai.google.dev/edge/mediapipe/solutions/audio/audio_classifier/android).
- Android 14+ vaatii foreground servicelle sopivan service-tyypin ja siihen
  liittyvan foreground-service-permissionin. Mikrofoni-service kayttaa
  `android:foregroundServiceType="microphone"`, manifest-permissionia
  `FOREGROUND_SERVICE_MICROPHONE` ja `startForeground()`-tyyppia
  `FOREGROUND_SERVICE_TYPE_MICROPHONE`. Ambient playback kayttaa erillista
  `android:foregroundServiceType="mediaPlayback"` -servicea ja
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` -manifest-permissionia. `RECORD_AUDIO` on
  while-in-use -runtime-lupa, joten backgroundista kaynnistettavaa
  mikrofonipalvelua koskee rajoituksia.
- Health Connectin nykyinen datatyyppilista sisaltaa `ExerciseSessionRecord`-
  ja `HeartRateRecord`-tyypit. Nykyisesta virallisesta listasta ei loydy
  dBcheckin kayttotarpeeseen natiivia melualtistus- tai audiometriatietuetta,
  joten koodin nykyinen malli kirjoittaa melun exercise sessionina ja jattaa
  kuulotestin Health Connect -kirjoituksen tietoisesti no-opiksi.
- Sovellus paketoi MediaPipe Tasks Audion native-kirjastoja. Androidin virallinen
  16 KB -ohje edellyttää native-kirjastojen ZIP- ja ELF-segmenttikohdistuksen
  tarkistamista; projektin QA-artifactit ja `NativeLibraryCompatibilityTest`
  käsittelevät tämän erillisenä release-sopimuksena.

---

## Tekniikkapino

Versiot on tarkistettu tiedostoista `gradle/libs.versions.toml`,
`app/build.gradle.kts`, `build.gradle.kts` ja
`gradle/wrapper/gradle-wrapper.properties`.

| Teknologia | Versio | Kayttotarkoitus |
|---|---:|---|
| Kotlin | 2.3.20 | Kieli ja Compose compiler plugin |
| Android Gradle Plugin | 9.2.1 | Android build |
| Gradle wrapper | 9.6.1 | Build tool |
| JVM / Java target | 21 | Compile target |
| Compose BOM | 2026.03.00 | Compose-kirjastojen versiohallinta |
| Material 3 | BOM | UI-komponentit custom-teeman paalla |
| AndroidX Core KTX | 1.19.0 | Android Kotlin extensions |
| Activity Compose | 1.13.0 | Compose activity integration |
| Lifecycle | 2.11.0 | ViewModel, saved state, runtime ja runtime-compose |
| Navigation Compose | 2.9.7 | Compose-reititys |
| Hilt | 2.59.2 | Dependency injection |
| Hilt Navigation Compose | 1.2.0 | `hiltViewModel()` navigaatiossa |
| KSP | 2.3.9 | Room/Hilt annotation processing |
| Room | 2.8.4 | Lokaali tietokanta |
| DataStore Preferences | 1.2.1 | Asetukset ja Pro-entitlement |
| Coroutines | 1.11.0 | Async/Flow |
| Google Play Billing KTX | 8.3.0 | Kertaosto Pro-tuotteelle |
| Health Connect client | 1.1.0 | Melusessioiden synkkaus ja sykkeen luku |
| CameraX | 1.6.1 | Camera overlay -preview, live dB readout, photo share burned-in overlay ja silent video capture |
| Glance | 1.1.1 | Kotinayton widget |
| WorkManager | 2.11.2 | Glance-riippuvuuden korjattu constraint |
| Guava Android | 33.6.0-android | Health Connect / transitiivinen constraint |
| Detekt | 2.0.0-alpha.5 | Staattinen analyysi |
| Detekt Compose rules | 0.5.8 | Compose-saannot |
| Compose Stability Analyzer | 0.7.4 | Compose-stabiliteettidumpit |
| Android Security Lints | 1.0.4 | Android security lintChecks |
| Screenshot test plugin/API | 0.0.1-alpha14 | Compose preview screenshot -testit |
| Sentry Android Core | 8.48.0 | Debug-only crash-diagnostiikka, ei release-riippuvuutta |
| MediaPipe Tasks Audio | 0.10.35 | 16 KB -yhteensopiva YAMNet sound detection -inference |
| OWASP Dependency-Check Gradle plugin | 12.2.2 | CVE-skannaus |
| SonarQube Gradle plugin | 7.3.0.8198 | SonarCloud-analyysi |
| JaCoCo | 0.8.14 | Unit-test coverage |
| Min SDK | 26 | Android 8.0 |
| Compile SDK | 37 | Android build API |
| Target SDK | 36 | Android runtime behavior target |

Testikirjastot: JUnit 4.13.2, MockK 1.14.11, Turbine 1.2.0,
AndroidX Test Core 1.7.0, Robolectric 4.16.1 ja Coroutines Test 1.11.0.

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
│                             BillingRuntimeGateway,
│                             BillingEntitlementSource, ProFeatureManager
├── data/
│   ├── export/               ExportCsvUseCase, CsvExportFormatter,
│   │                         ExportFileCache
│   ├── local/db/             Room database, schema, migrations, DAOt, entities
│   ├── local/preferences/    UserPreferencesDataStore and typed preference models
│   ├── model/                Room -> domain mappings
│   └── repository/           Session, Measurement, SoundDetection,
│                             Preferences, HearingTest, HearingRecovery,
│                             SleepSession, PassiveMonitoring
├── domain/
│   ├── analytics/            ExposureAnalyticsCalculator and models
│   ├── ambient/              AmbientSoundPolicy, AmbientSoundGenerator
│   ├── audio/                DecibelCalculator, audio record -policyt,
│   │                         FrequencyWeightingFilter, FFTProcessor,
│   │                         SpectralAnalyzer, OctaveBandRtaCalculator,
│   │                         SoundClassifier, SoundDetectionWindowFanout,
│   │                         YamnetAudioWindowAdapter, PcmWavWriter,
│   │                         AudioInputDevice ja audio-domain-mallit
│   ├── calibration/          CalibrationProfile, CalibrationOffsetPolicy,
│   │                         OctaveCalibrationOffsets
│   ├── entitlement/          ProEntitlementPolicy
│   ├── hearingtest/          Hughson-Westlake procedure, codec, scoring,
│   │                         HearingRecoveryCalculator
│   ├── noise/                NoiseLevel, NoiseAlertPolicy,
│   │                         NoiseNotificationSchedule,
│   │                         AudibleAlarmPolicy,
│   │                         AudibleAlarmEvaluator
│   ├── passive/              PassiveMonitoringAggregator and aggregate models
│   ├── report/               SessionReportCalculator and report models
│   ├── session/              Session, SessionMetadata, SessionLocationMetadata,
│                             SessionAudioInputDeviceMetadata,
│                             SessionHistoryQuery, SessionHistoryPolicy
│   ├── sleep/                SleepRecordingConfig, SleepResultsCalculator,
│                             SleepInsightsCalculator
│   ├── tinnitus/             TinnitusPitchProfile, TinnitusPitchPolicy
│   └── voice/                VoiceBaseline, VoiceVolumeWarning and TTS risk policies
├── service/                  AudioEngine, AudioSessionManager,
│                             MeasurementForegroundService, ToneGenerator,
│                             MediaPipeSoundClassifier, AudioInputDeviceRouter,
│                             MeasurementPersistenceSampler, NotificationHelper,
│                             NotificationPrivacyPolicy, NoiseAlertEvaluator,
│                             HealthConnectService, HearingTestService,
│                             BackupService, SessionLocationCapturePort,
│                             AudioInputDeviceDiscoveryPort,
│                             HearingRecoveryService,
│                             PassiveMonitoringManager,
│                             AudibleAlarmPlaybackController,
│                             TtsRiskPromptController,
│                             AmbientSoundPlaybackService,
│                             AmbientSoundPlaybackController,
│                             AmbientSoundPlayer
├── sync/                     HealthConnectManager, HealthConnectModels,
│                             BackupGateway, LocalBackupManager,
│                             BackupDatabaseValidator, MeasurementDatabaseGate
├── ui/
│   ├── ambient/              Ambient sound playback route
│   ├── analytics/            Trends screen, exposure/Spectral/Environment cards
│   ├── common/               Context/Window helpers, KeepScreenOnEffect
│   ├── components/           Shared Compose components
│   ├── hearingtest/          Setup -> Active -> Results
│   ├── history/              Session history and naming sheet
│   ├── history/detail/       Session Detail, PDF and PNG report actions
│   ├── meter/                Live meter
│   ├── navigation/           Screen, DbCheckNavHost, BottomNavDestination
│   ├── settings/             Settings, Pro, Health Connect, backup/export
│   ├── sleep/                Sleep setup route, options state, CTA and active start/stop
│   ├── tinnitus/             Tinnitus pitch matcher route
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
  `domain/session/SessionMeasurement`-riveja ja optional
  `SessionLocationMetadata`-metadatan, ja `SessionRepository` mapittaa ne
  Room-kirjoituksiin.
- `DbCheckDatabase.DATABASE_NAME` on Room-tietokannan nimen lahde. Room builder,
  LocalBackupManager ja backup-testit viittaavat samaan vakioon.
- `ExportFileCache` omistaa FileProviderin authority-suffixin ja
  `cache/exports/`-hakemiston nimet. `file_paths.xml` julkaisee lisäksi
  app-private `files/wav_recordings/`-polun vain WAV-sharelle. Manifest/XML/
  runtime/testit pidetaan samassa sopimuksessa.
- `domain/hearingtest/HearingTestPolicy` ja `HearingRating` omistavat
  kuulotestin taajuuslistan, tone timing -arvot ja rating-koodit.
- `domain/noise/NoiseAlertPolicy` omistaa noise notificationien exposure-
  keston ja peak-warning-rajan. `NoiseNotificationSchedule` omistaa
  notificationien active day/hour -aikaikkunan ilman UI- tai Android
  notification -riippuvuutta.
- `domain/noise/AudibleAlarmPolicy` ja `AudibleAlarmEvaluator` omistavat
  audible alarm -threshold/duration/cooldown-päätökset puhtaana domain-koodina.
  Ne eivät toista ääntä, pyydä audio focusta tai koske Android notification
  -polkuihin.
- `domain/voice/*` omistaa voice baseline-, voice volume warning- ja TTS risk
  prompt -päätökset puhtaana domain-koodina. Android TextToSpeech, notification
  delivery ja haptic/audio playback pysyvät `service/`-kerroksessa.
- `domain/passive/PassiveMonitoringAggregator` koostaa käyttäjän käynnistämän
  passiivisen sample-jakson aggregate-arvot. Se ei luo sessioita, measurement-
  rivejä tai raakaaudion persistointia.
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
  `BillingRuntimeGateway.startConnection()`-polulla.
- Sama startup kaynnistaa `AudioSessionManager.recoverInterruptedSession()`-
  tehtavan. Jos edellisen prosessin jaljilta Roomissa on aktiivinen sessio,
  se viimeistellaan hiljaisesti persistoiduista mittausriveista ilman
  auto-navigointia.
- `DbCheckApplication` seuraa `ProFeatureManager.isProUser`-virtaa ja paivittaa
  Glance-widgetit, kun Pro-oikeus muuttuu ensimmaisen emission jalkeen.
- `MainActivity` odottaa ensimmaista `UserPreferences`-emissiota ennen
  `DbCheckTheme`/`DbCheckNavHost`-sisallon piirtamista. Ennen emission julkaisemista
  UI:lle Android 12+:n package-kohtainen night mode synkronoidaan tallennetusta
  `ThemeMode`-arvosta. Android 11:n ja vanhempien resurssiteemat poistavat
  system-selected startup-previewn. Nama estavat tallennetun teeman valahdyksen.
- `MainActivity.onResume()` kutsuu `BillingRuntimeGateway.refreshPurchases()`, jotta
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
- `minSdk = 26`, `compileSdk = 37`, `targetSdk = 36`
- `MainActivity` on ainoa launcher activity ja `android:exported="true"`.
- `HealthConnectPermissionDisclosureActivity` on `exported=false`.
- Health Connectin exported entrypointit ovat activity-aliaksia:
  `.HealthConnectPermissionsRationaleActivity` ja
  `.HealthConnectPermissionUsageActivity`. Ne targetoivat staattista
  `HealthConnectPermissionDisclosureActivity`a, eivat varsinaista
  navigation/data-muutosflow'ta.
- `MeasurementForegroundService` on `exported=false` ja
  `android:foregroundServiceType="microphone"`.
- `AmbientSoundPlaybackService` on `exported=false` ja
  `android:foregroundServiceType="mediaPlayback"`.
- `DbCheckWidgetReceiver` on `exported=false`.
- `FileProvider` on `exported=false`, `grantUriPermissions=true`, ja
  `file_paths.xml` rajaa jaettavat tiedostot `cache/exports/`-polkuun ja
  WAV-jakoa varten app-private `files/wav_recordings/`-polkuun.
- `android:allowBackup="false"`, `backup_rules.xml` ja
  `data_extraction_rules.xml` sulkevat appin root-datan pois cloud backupista
  ja device transferista.
- `android:usesCleartextTraffic="false"`.

Manifest-oikeudet:

- `RECORD_AUDIO` - mikrofoni, runtime-pyynto Meterissa.
- `CAMERA` - Camera Overlay -polun runtime-lupa; route pyytaa luvan ennen
  CameraX preview -sidontaa.
- `POST_NOTIFICATIONS` - Android 13+ ilmoitukset, pyydetaan mittauksen
  kaynnistyksen yhteydessa tarvittaessa.
- `FOREGROUND_SERVICE` ja `FOREGROUND_SERVICE_MICROPHONE` - mikrofonin
  foreground service.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - ambient sound playbackin foreground
  service.
- `VIBRATE` - haptiikka.
- `com.android.vending.BILLING` - Google Play Billing.
- `android.permission.ACCESS_COARSE_LOCATION` - optional approximate session
  location metadata.
- `android.permission.health.WRITE_EXERCISE` - Health Connect
  melusessiosynkkaus.
- `android.permission.health.READ_HEART_RATE` - Health Connect sykeoverlay.
- Manifestin `<queries>` sallii Health Connect -paketin ja Android 11+
  TextToSpeech service -intenttien näkyvyyden.
- Kamera on deklaroitu optional-featureina: `android.hardware.camera.any` ja
  `android.hardware.camera`, molemmat `required=false`.

Session location -scope:

- Session sijainti on optional metadata, ei mittauksen vaatimus.
- Manifestissa on vain `ACCESS_COARSE_LOCATION` approximate metadataa varten.
- `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` ja foreground service
  `location` -tyyppi eivät kuulu nykyiseen scopeen.
- Runtime-pyyntö näytetään vasta käyttäjän valitessa Settingsin Data & Export
  -osiosta approximate session location -toiminnon; nykyinen adapteri palauttaa
  `null`, jos runtime-lupaa ei ole myönnetty.
- Jos sijainti on denied/unavailable tai stop tapahtuu ilman foreground-
  käyttötilannetta, sessio jatkuu ja sijainti jätetään tyhjäksi.

---

## Design system ja tekstiresurssit

- Varit: dark/light-tokenit `ui/theme/Color.kt`:ssa. Paagradientti ja
  tonaaliset surface-tasot tulevat teeman kautta.
- Typografia: Manrope yleistekstissa ja Space Grotesk numeerisessa/datanaytossa.
- Muodot ja spacing: `Shape.kt` ja `Spacing.kt`. `DbCheckSpacing` sisältää
  4/8/12/16/20/24/32/40/48/64 dp -asteikon sekä semanttiset `pageMargin = 20dp`,
  `groupGap = 12dp`, `sectionGap = 32dp`, `cardPadding = 20dp`,
  `heroPadding = 24dp` ja `tilePadding = 16dp` -tokenit. `DbCheckRadii`
  keskittää Card 24dp-, Tile 16dp-, Row 12dp- ja ModalSheet 28dp -säteet.
- Motion: `Motion.kt` keskittää `StateChange = 150 ms`, `Content = 250 ms`,
  `Screen = 400 ms`, `GaugeSweep = 200 ms`, `Shimmer = 1200 ms` ja
  `Breathing = 3000 ms` -kestot.
- Kaaviot: `ChartTokens.kt` keskittää grid-, line-, live-line- ja threshold-
  strokeleveydet, point/bar-radiukset, threshold-dash-patternin ja area-alphan.
  Uusi kaavio ei saa luoda rinnakkaista paikallista chart grammar -lähdettä.
- Komponentit: mm. `DbCheckButton`, `DbCheckCard` + `DbCheckCardEmphasis`,
  `DbCheckChip` + `DbCheckChipDensity`, `DbCheckSlider`, `DbCheckToggle`,
  `ProLockOverlay`, `InlineStatusRow`, `DbCheckAlertDialog`,
  `DbCheckSetupScaffold`, `LiveActivityCard`, `SessionCard`, `BottomNavBar`,
  `SkeletonLoader` ja `EmptyState`.
- `ProLockOverlay` pitää locked-previewn sisällön normaalina esikatseluna
  yhteisen scrimin ja upgrade-CTA:n alla. Setup-flow't käyttävät
  `DbCheckSetupScaffold`ia, Settings-dialogit `DbCheckAlertDialog`ia ja
  success/error/info/warning-rivit `InlineStatusRow`ta.
- Uudet design-arvot tulee keskittaa teemaan. Inline-varit, spacingit,
  animaatiokesto- ja card-oletukset ovat koodintarkistuksessa punaisia lippuja,
  jos niille on jo token.
- `app/src/main/res/values/strings.xml` sisaltaa nykyisin laajan
  default-English-resurssipohjan: 780 `string`-merkintaa ja 11
  `plurals`-merkintaa, mukaan lukien saavutettavuuskuvaukset.
- `app/src/main/res/values-fi/strings.xml` on rajattu Finnish launch -baseline:
  72 `string`-merkintaa ja 2 `plurals`-merkintaa. Se kattaa nykyisessa
  checkoutissa erityisesti ambient soundin, hearing recoveryn, tinnitus pitchin
  ja muutaman yleisen/a11y/notification-tekstin; koko sovellus ei ole viela
  lokalisoitu.
- Arvo-/teemakansioista loytyvat `values`, `values-fi` ja `values-night`. Muut
  nykyiset `res`-hakemistot ovat `drawable`, `font`, `layout`,
  `mipmap-anydpi-v26`, `raw` ja `xml`.

---

## Navigaatio

`DbCheckNavHost` kayttaa bottom navigationia puhelimella ja NavigationRailia,
kun nayton leveys on vahintaan 600dp.

Viisi top-level-kohdetta ovat samassa jarjestyksessa molemmissa navigaatioissa: Meter, Trends (`analytics`), Hearing,
History ja Settings. Varsinaiset fullscreen-reitit eivat nayta yhteista navigaatiota.
`history/detail/{sessionId}` kuuluu History-valintaan ja nayttaa yhteisen bottom barin tai navigation railin.
`selectedTopLevelRouteFor(...)` palauttaa sille Historyn, joten `showNavigation = selectedTopLevelRoute != null`.

| Reitti | Naytto | Nykyinen kayttaytyminen |
|---|---|---|
| `meter` | Meter | Start destination. Live gauge, waveform, Min/Avg/Max/Peak, Play/Pause, Reset ja Share. Pyytää `RECORD_AUDIO`-luvan ja Android 13+ ilmoitusluvan mittauksen kaynnistyksen yhteydessa. Kaynnistaa `MeasurementForegroundService`n; valmis normaali stop navigoi Session Detailiin `completedSessionIds`-eventista. |
| `analytics` | Trends | Mittaus-/altistustrendit, kompakti Hearing-statushandoff, Pro-gatettu live-spektri, 7 paivan Environment Mix, 30 paivan trendi ja 12 kuukauden raportti. Trends ei omista hearing tool -kortteja tai hearing repositoryja. |
| `hearing` | Hearing | Top-level-hubi kuuloterveysstatukselle, latest testille, hearing test/recovery-, tinnitus-, Voice Baseline-, optional Sleep Monitor- ja Ambient Sounds -siirtymille. Varsinaiset tyokalut avautuvat olemassa oleviin fullscreen-flow'hin. |
| `history` | History | 24h-hourly chart, safe hours, viimeisimmat sessiot, View All -tila, SessionNamingSheet ja Session Detail -avaus. Free-kayttajan historia rajataan 7 paivaan `SessionHistoryPolicy`n kautta. |
| `history/detail/{sessionId}` | Session Detail | Valitsee Historyn ja nayttaa yhteisen bottom barin/railin. Sisalto on sessioraportti, metadata, LAeq/equivalent-level-label, LCpeak, A-painotetuille sessioille TWA/dose/85 dBA peak events, time-series, PNG-jako, Pro-gatettu PDF-export ja Pro Health Connect -sykeoverlay. Suora reitti vanhaan sessioon lukitaan Free-kayttajalta. |
| `settings/home` | Settings | Top-level-hubi sivuille Calibration, Notifications & alerts, Data & privacy, Display ja Pro & About. |
| `settings/calibration` | Calibration | Audioasetukset, response/frequency weighting, input device ja calibration profiles. |
| `settings/calibration/octave` | Octave Calibration | Valitun profiilin octave-bandisäätimet ja sama Pro-gate kuin Calibrationissa. |
| `settings/notifications` | Notifications & alerts | Exposure/peak-alertit, audible alarm, TTS risk prompt, passive monitoring, threshold ja schedule. |
| `settings/data_privacy` | Data & privacy | Health Connect, CSV, WAV-disclosure, local backup/restore, clear history ja lockscreen privacy -asetukset. |
| `settings/display` | Display | Teema, waveform, refresh rate ja feature togglet. |
| `settings/pro_about` | Pro & About | Purchase/debug force-free, version ja about-sisalto. |
| `settings?showPro={showPro}` | Settings legacy | Vain yhteensopivuusredirect `settings/home`- tai `settings/pro_about`-reitille; ei omaa sivua. |
| `sleep/setup` | Sleep Setup | Non-top-level Sleep Monitor -route, joka avautuu Meterin ja Hearing-hubin Pro-effective `sleep_card` -CTA:sta. Free/deep-link -polku ohjataan Settingsin Pro-korttiin `SleepSetupViewModel`in execution-gatella. Pro-kayttaja voi valmistella 6h/8h/10h target-keston ja keep screen awake -option seka kaynnistaa Sleep recordingin foreground service -polun kautta. Sleep-start kirjoittaa `sleep_sessions`-metadatan luodulle tavalliselle session ID:lle; History nayttaa Sleep-badgen ja Session Detail avaa Sleep Results -kortin samalle session ID:lle. |
| `hearing_test/setup` | Hearing Test Setup | Kuulotestin aloitusnaytto. Setup-ruutu ei itse lue Pro-tilaa; varsinainen testin suoritus estyy Free-tilassa `ActiveTestViewModel`issa. |
| `hearing_test/active` | Hearing Test Active | Pro-kayttajan tone-playback ja Hughson-Westlake-tyyppinen threshold-flow. Free-tilassa execution estetaan ViewModelissa. |
| `hearing_test/recovery/setup` | Hearing Recovery Setup | Pro-kayttajan lyhyen recovery-checkin aloitusnaytto. Copy rajaa checkin personal tracking -vertailuksi full hearing-test-baselineen, ei diagnoosiksi. |
| `hearing_test/recovery/active` | Hearing Recovery Active | Kayttaa samaa `HearingTestActiveScreen` / `ActiveTestViewModel` -polkua `HearingTestMode.RECOVERY`-moodilla. Moodissa testataan vain 1/4/8 kHz molemmille korville ja valmis tulos tallennetaan `HearingRecoveryService`n kautta. |
| `hearing_test/results/{testId}` | Hearing Test Results | Lataa ensisijaisesti route-argumentin `testId` tuloksen; fallback on latest result. Free-tilassa result-dataa ei nayteta eika jaeta. Share Results luo PNG-kortin ja tekstin Android Sharesheetiin. |
| `tinnitus/pitch` | Tinnitus Pitch Matcher | Non-top-level Pro-gatettu personal tracking -pitch profile. Tallentaa DataStoreen vain vasemman/oikean korvan pitch-arvot ja päivitysajan, käyttää käyttäjän painamasta Preview-toiminnosta olemassa olevaa `ToneGenerator`ia eikä käynnistä taustapalvelua, sound therapyä, Health Connect -kirjausta tai automaattisia triggereitä. |
| `ambient/playback` | Ambient Sound Playback | Non-top-level Pro-gatettu local playback -route. Käynnistää käyttäjän Play-toiminnolla erillisen `AmbientSoundPlaybackService` mediaPlayback foreground servicen, vaatii Android 13+ notification-luvan, tarjoaa näkyvän Stop-kontrollin ja ei käytä mikrofonia, Room-skeemaa, Health Connectiä tai therapy/safety-copya. |

Top-level navigation palauttaa valitun stackin rootiin konservatiivisesti:
samassa top-level stackissa statea ei palauteta, eri top-level stackissa
`saveState`/`restoreState` on kaytossa.

Settings-childit kayttavat yhteista graph-scoped `SettingsViewModel` -instanssia. Hub omistaa vain sivuvalinnan ja
child-sivut omat launcherinsa/toimintonsa. Settingsin top-level-uudelleenvalinta childilta kayttaa reselect-to-home-
kaytosta ja avaa `settings/home`-juuren; eri top-level-stackista palattaessa state restore sailyy.

Non-top-level Pro-routejen entry-sopimus:

- `ProRouteAccessGate.kt` kerää nullable entitlementin
  `ProRouteAccessViewModel.isProUser`-virrasta. `null` ei renderöi sisältöä eikä
  redirectaa, `false` ohjaa `settings/pro_about`-reitille ja vain `true`
  rakentaa Pro-sisällön.
- Gate ympäröi `tinnitus/pitch`, `ambient/playback`,
  `hearing_test/recovery/setup` ja `hearing_test/recovery/active` -reitit.
- `sleep/setup` käyttää erillistä `SleepSetupAvailability.Loading/Locked/Ready`
  -entrytilaa ja `SleepSetupEntryPolicy`a. `startSleepRecording()` tarkistaa
  `Ready`-tilan uudelleen ennen foreground servicen käynnistystä.
- Navigation-gate ei korvaa ViewModel-, service- tai domain-tason execution-
  gatea. Free-dataa ei saa ladata tai Pro-toimintoa suorittaa sillä perusteella,
  että route on UI:ssa piilotettu.

---

## Free vs Pro

| Ominaisuus | Free | Pro | Nykytila koodissa |
|---|:---:|:---:|---|
| Live dB-mittari, waveform ja session stats | x | x | Kytketty Meterissa |
| Aktiivisen session info bar | x | x | REC, kesto, effective weighting ja response time; Prolle sample rate ja input device |
| Foreground measurement notification | x | x | Kytketty `MeasurementForegroundService`ssa |
| Melutasoilmoitukset ja threshold-asetus | x | x | `NoiseAlertEvaluator` tukee threshold-, dose-, projected-dose- ja peak-alertteja schedulella ja cooldownilla |
| Passive monitoring 5 min aggregate sample | x | x | Settingsin Noise Notifications -kortista käyttäjän käynnistämä foreground-service sample; tallentaa vain aggregate-rivit `passive_monitoring_samples`-tauluun |
| Dark / Light / System -teema | x | x | DataStore + startup theme bootstrap |
| Waveform style Line/Filled/Bars | x | x | Free-asetus, vaikuttaa Meter UI:hin |
| Meter refresh rate High/Standard/Low | x | x | Free-asetus, vaikuttaa vain Meter UI -paivitysvali, ei AudioRecordiin tai Room-kadenssiin |
| 7 paivan historia | x | x | `SessionHistoryPolicy.FREE_HISTORY_WINDOW_MILLIS` |
| Rajoittamaton historia |  | x | History ja Session Detail rajaavat Free-kayttajan nakyman 7 paivaan; repositoryssa on seka raw-all-kyselyita etta gated listauspolkuja |
| Viikon altistumiskaavio ja kuuloterveys | x | x | Kytketty Room-dataan |
| Health Connect -melusessiosynkkaus | x | x | Free-kayttajallekin sallittu Settingsista |
| Mikrofoniherkkyyden kalibrointi |  | x | `ProAudioPreferencePolicy`, Settings gate ja Room-backed calibration profiles; Settingsissä octave-band sliderit ja reset valitulle profiilille |
| Frequency weighting A/B/C/Z/ITU-R 468 |  | x | `ProAudioPreferencePolicy` ja AudioEngine |
| Dosimeter standard NIOSH REL / OSHA PEL |  | x | `DosimeterStandard`, DataStore, Settings state ja `DosimeterCalculator` NIOSH/OSHA-laskennalle |
| Lock-screen live meter |  | x | Custom RemoteViews notification |
| Health Connect -sykeoverlay |  | x | Session Detail + PDF heart-rate page |
| PDF-raportti |  | x | `CreateDocument("application/pdf")` + `ExportPdfReportUseCase` |
| Session Detail PNG -jakokortti | x | x | `ShareResultsGenerator.shareSessionReportCard()` |
| Kotinayton widget |  | x | Glance-widget Pro-gatella |
| Kuulotesti |  | x | Hearing-hubin CTA overlay, execution, save, results ja share gateattu; setup-ruutu ei itse gatea Pro-tilaa |
| Hearing recovery check |  | x | Full hearing-test-baselineen vertaava 1/4/8 kHz short check; tallentaa vain aggregate-shiftit v12-tauluun |
| CSV-vienti |  | x | Settings Data & Export |
| WAV recording writer/export |  | x | Pro+opt-in-gatettu PCM16 WAV app storageen; Session Detail FileProvider share/delete, manual share smoke ajettu |
| Session-nimeaminen ja tagit |  | x | History ja Session Detail |
| Live-spektrianalyysi |  | x | Raw PCM -datasta, ei persistointia |
| Live sound detection |  | x | YAMNet/MediaPipe Tasks Audio live inference; optional persistence tallentaa vain label-vaihdos-eventit |
| Audible alarm |  | x | Settings opt-in, 90 dB / 30 s / 5 min policy, proximity/interactive guard ja bundled alarm WAV |
| Voice baseline ja voice warning |  | x | Vaatii Pro + aktiivinen mittaus + sound detection; tallentaa vain baseline aggregate -arvot DataStoreen |
| Spoken TTS risk prompt |  | x | OFF oletuksena; triggeröi vain dosimeter dose/projected-dose -riskieventeistä, kun sound detection ja hearing baseline ovat saatavilla |
| Tinnitus pitch profile |  | x | User-started ToneGenerator preview ja ear-specific DataStore-profiili; ei taustatoistoa, terapiaa tai Health Connect -kirjausta |
| Ambient sound playback |  | x | User-started local AudioTrack playback erillisessä mediaPlayback foreground servicessä; ei mikrofonia tai Room-dataa |
| Environment Mix |  | x | 7 paivan Room-jakauma; Free saa locked-previewn |
| 30 paivan trendi |  | x | `ExposureAnalyticsCalculator` |
| 12 kuukauden raportti |  | x | `ExposureAnalyticsCalculator` + session count |

---

## Billing ja entitlement

- `BillingGateway.kt` on Settingsin ostovirran testattava rajapinta.
- `BillingRuntimeGateway` on appin startup/resume-lifecycleportti ja
  `BillingEntitlementSource` on ostotilan stream-portti. Tuotantokoodi
  injektoi billingia naiden rajapintojen kautta; `BillingManager` on vain
  tuotantototeutus ja Hilt-bindingien parametri.
- `BillingManager` on gatewayn tuotantototeutus ja kasittelee yhden INAPP-
  tuotteen: `dbcheck_pro`.
- `BillingEntitlementSource.isPurchased` alkaa arvosta `null`. `ProFeatureManager`
  synkkaa DataStoreen vain varmistetun `true`/`false`-ostotilan, jotta appin
  kaynnistys tai Play Billing -haun virhe ei ylikirjoita aiemmin tallennettua
  Pro-oikeutta Free-tilaan.
- `BillingRuntimeGateway.refreshPurchases()` kasittelee startup-/resume-snapshotit.
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
- `service/AudioEngine`: AudioRecord mono PCM16, permission check ennen tallennusta,
  `@RequiresPermission` AudioRecord-luonnissa, `StateFlow<SpectralFrame?>`
  live-spektrille ja `StateFlow<AudioInputInfo>` aktiivisen tallennuksen
  input-metadatalle.
- `AudioInputDevice` ja `AudioInputDeviceType`: Androidista riippumaton input-
  device-listausmalli. `AudioInputDeviceMapper` on listauksen ja routing-
  fallbackin yhteinen lähde. `AndroidAudioInputDeviceDiscoveryPort` lukee
  `AudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)` -source-laitteet,
  mapittaa USB/Bluetooth/wired/built-in-tyypit ja julkaisee normalisoidut
  display-nimet, external-lipun, sample rate -listat ja channel count -listat
  Settings UI-statea varten. `selected_audio_input_device_id` on Pro-
  effective DataStore-valinta; Free-käyttäjän execution-polku saa aina
  effective null -arvon.
- `AudioInputDeviceRouteResolver` valitsee tallennetun device-id:n, mutta jos
  valittu external input ei ole enää listassa, se fallbackaa built-in-
  mikrofoniin ylikirjoittamatta tallennettua preferenceä. `AndroidAudioInputDeviceRouter`
  kutsuu `AudioRecord.setPreferredDevice(...)` ennen `AudioRecord.startRecording()`-
  kutsua ja julkaisee routed-device-nimen `AudioInputInfo`n kautta.
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
  dominanttitaajuushaussa ja yhteinen FFT-binien taajuusmuunnos.
- `SpectralAnalyzer`: 24 logaritmista 20 Hz-20 kHz bandia, dominantti taajuus
  ja bandwidth-luokka raw PCM16 -chunkista.
- `OctaveBandRtaCalculator`: nykyisen `FFTProcessor`in päälle rakennettu
  octave/third-octave RTA-domain-laskuri. Se käyttää IEC/ANSI base-10-kaavaa
  keskitaajuuksiin ja band edgeihin, aggregoi FFT-magnitudit bandikohtaisesti,
  voi lukea `OctaveCalibrationOffsets`-mallin octave-resoluutiolle ja normalisoi
  amplitudit vahvimpaan kalibroituun RTA-bandiin. `AudioEngine.rtaFrame`
  julkaisee octave-RTA-datan live-only Analytics UI -polkuun zero-offset-
  oletuksella, kunnes runtime-kytkentä valittuun profiiliin on valmis; Room-persistointia
  ei tehdä.
- `YamnetAudioConfig`: 16 kHz sample rate, 15 600 samplen window, 7 680
  samplen hop ja 7 500 Hz mel-yläraja.
- `YamnetAudioWindowAdapter`: muuntaa 44.1 kHz PCM16 -chunk-virran 16 kHz
  float-windowiksi YAMNetille ilman raw-audion persistointia. Downsampling
  käyttää 96-tap windowed-sinc anti-alias -suodatusta, jonka cutoff on enintään
  Nyquist tai YAMNetin mel-yläraja; jatkuvuus chunk-rajojen yli säilytetään
  source history- ja absolute source position -tilassa.
- `domain/audio/SoundClassifier.kt` on testattava inference-portti.
  `service/MediaPipeSoundClassifier.kt` käyttää MediaPipe Tasks Audio
  `AudioClassifier`ia YAMNet-assetilla, luo monokanavaisen 16 kHz
  `AudioData`-syötteen ja mapittaa kategoriat `SoundClassificationPolicy`n
  confidence thresholdin kautta. Runtime luodaan laiskasti ensimmäisessä
  ei-tyhjässä classify-kutsussa, classify/close sarjallistetaan samalla lockilla
  ja `close()` vapauttaa native-runtimen idempotentisti.
- `SoundDetectionWindowFanout`: `AudioEngine`n live-only raw-audio fanout
  YAMNet-windoweille. `AudioSessionManager` ohjaa sitä effective-ehdolla
  `isProUser && soundDetectionEnabled`, ja manager julkaisee
  `soundDetectionState`-tilassa current detectionin sekä recent detections
  -listan. `AudioEngine` ei tee classifier-inferenceä eikä raw-audiota
  persistöidä.
- `SoundDetectionRepository`: tallentaa vain erillisellä
  `soundDetectionPersistenceEnabled`-opt-inillä aggregoidut detection-eventit
  (`sessionId`, timestamp, label, confidence). Sama label tallennetaan
  aktiivisen session aikana uudelleen vasta, kun detected label vaihtuu tai
  classifier palauttaa välissä tyhjän tuloksen; raakaaudiota tai float-windowia
  ei tallenneta.
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
- `MeterUiState.sessionInfo` on aktiivisen session infobar-malli.
  `MeterViewModel` rakentaa sen `AudioSessionManager.isRecording`- ja
  `activeSessionStartTimeMs`-virroista, `ProAudioPreferencePolicy`n effective
  weighting/response time -arvoista seka `AudioEngine.audioInputInfo`sta.
  Free-kayttaja nakee REC-tilan, keston, weightingin ja response timen; Pro
  nakee lisaksi sample raten ja input devicen.
- `MeterScreen` käyttää aktiivisen mittauksen aikana yhteistä `KeepScreenOnEffect` /
  `KeepScreenOnController` -polkua `FLAG_KEEP_SCREEN_ON` -window flagille.
  Controller clearataan recordingin päättyessä tai composable-disposessa, eikä
  nykyisessä checkoutissa ole `PowerManager.WakeLock`-polkua. Sleep setup käyttää
  samaa helperiä vain `isRecording && keepAwakeEnabled` -ehdolla.
  `ui/common/ContextActivity.findActivity()` on yhteinen Activity-resolver
  Settingsille, Camera overlaylle, Meterille ja Sleep setupille.
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

Room database: `DbCheckDatabase`, `SCHEMA_VERSION = 13`, `exportSchema = true`.
Skeematiedostot ovat `app/schemas/.../1.json` ... `13.json`.

Migraatiot:

- `MIGRATION_1_2`: lisaa `sessions.activeSlot`, varmistaa yhden aktiivisen
  session slotin, sulkee ylimaaraiset aktiiviset sessiot ja luo deterministiset
  indeksit sessioille, mittauksille ja hearing-test-resultseille.
- `MIGRATION_2_3`: lisaa `measurements.peakDb` -sarakkeen ja backfillaa vanhat
  rivit `dbWeighted`-arvolla.
- `MIGRATION_3_4`: lisaa `measurements.aWeightedDb`- ja `measurements.responseTime`
  -sarakkeet. Vanhat rivit backfillataan arvoilla `aWeightedDb = dbWeighted` ja
  `responseTime = FAST`.
- `MIGRATION_4_5`: lisaa `sound_detection_events`-taulun aggregoiduille
  sound detection -eventeille ja indeksit `sessionId,timestamp`-export-/session
  -kyselyille seka `timestamp`-poistopolitiikoille. Taulu cascadoituu
  `sessions.id`-avaimeen.
- `MIGRATION_5_6`: lisaa nullable session location -metadatasarakkeet
  `sessions.locationLatitude`, `locationLongitude`, `locationAccuracyMeters` ja
  `locationCapturedAt`. Vanhoja riveja ei backfillata; location on optional.
- `MIGRATION_6_7`: lisaa `calibration_profiles`-taulun ja
  `index_calibration_profiles_name`-indeksin. Profiilit ovat UI:sta riippumaton
  Room-data source calibration profile -pinnoille.
- `MIGRATION_7_8`: lisaa `calibration_profiles.octaveBandOffsets` TEXT NOT NULL
  -sarakkeen default-arvolla tyhja string. V8:n Room identity hash on lisatty
  `BackupDatabaseValidator`in tuettuihin hasheihin, jotta v8-backupit
  lapaisisivat restore-validaation.
- `MIGRATION_8_9`: lisaa nullable selected/routed audio input -metadatasarakkeet
  `sessions.selectedAudioInputDeviceId`, `selectedAudioInputDeviceName` ja
  `routedAudioInputDeviceName`. V9:n Room identity hash on lisatty
  `BackupDatabaseValidator`in tuettuihin hasheihin.
- `MIGRATION_9_10`: lisaa Sleep Monitorin erilliset `sleep_sessions`- ja
  `sleep_notable_events`-taulut. Sleep metadata ei lisaa sarakkeita tavalliseen
  `sessions`-tauluun. V10:n Room identity hash
  `e4c97360fab833b6bc30549ab7e8075f` on lisatty
  `BackupDatabaseValidator`in tuettuihin hasheihin.
- `MIGRATION_10_11`: lisaa `passive_monitoring_samples`-taulun vain aggregate
  passive monitoring -sampleille. Taulu ei viittaa `sessions`-tauluun eikä
  sisalla raakaaudiota, PCM-bufferia tai YAMNet-windoweita. V11:n Room identity
  hash `716c7f0bf6a88b295970a3f5459e7cbf` on lisatty
  `BackupDatabaseValidator`in tuettuihin hasheihin.
- `MIGRATION_11_12`: lisaa `hearing_recovery_results`-taulun short recovery
  check -tuloksille. Taulu viittaa `hearing_test_results.id`-baselineen
  cascade-FK:lla, ja indeksit ovat `timestamp` sekä `baselineTestId`. V12:n
  Room identity hash `f73f218710d7988e02fb65939ff4fd56` on lisatty
  `BackupDatabaseValidator`in tuettuihin hasheihin.
- `MIGRATION_12_13`: lisaa nullable `sessions.startUtcOffsetSeconds`- ja
  `sessions.endUtcOffsetSeconds`-sarakkeet. Uudet sessiot tallentavat alku- ja
  loppuhetken historialliset UTC-offsetit; vanhoja riveja ei backfillata
  vientihetken aikavyohykkeella. Interrupted-session recovery ei keksi
  menneelle loppuajalle nykyista offsetia, vaan jattaa sen tuntemattomaksi. V13:n Room identity hash
  `3b7807f7ad6982a9676bda9f07ed3a2d` on lisatty
  `BackupDatabaseValidator`in tuettuihin hasheihin.

Entiteetit:

- `sessions`: `id`, `startTime`, `endTime`, `startUtcOffsetSeconds`,
  `endUtcOffsetSeconds`, `minDb`, `avgDb`, `maxDb`,
  `peakDb`, `name`, `emoji`, `tags`, `isActive`, `activeSlot`,
  `frequencyWeighting`, `locationLatitude`, `locationLongitude`,
  `locationAccuracyMeters`, `locationCapturedAt`, `selectedAudioInputDeviceId`,
  `selectedAudioInputDeviceName`, `routedAudioInputDeviceName`.
- `measurements`: `id`, `sessionId`, `timestamp`, `dbValue`, `dbWeighted`,
  `peakDb`, `aWeightedDb`, `responseTime`, optional `frequencyData`.
- `hearing_test_results`: `id`, `timestamp`, `overallScore`, `rating`,
  `leftEarData`, `rightEarData`, `speechClarity`, `highFreqLimit`,
  `avgThreshold`.
- `hearing_recovery_results`: `id`, `baselineTestId`, `timestamp`,
  `testedFrequencyCount`, `averageShiftDb`, `maxShiftDb`, `status`,
  `leftEarShiftData` ja `rightEarShiftData`. Taulu tallentaa vain aggregate-
  shiftit, ei uutta tone-audio- tai kliinista audiometriadataa.
- `sound_detection_events`: `id`, `sessionId`, `timestamp`, `label`,
  `confidence`. Taulu ei sisalla raakaaudiota, PCM-windowia tai float-windowia.
- `calibration_profiles`: `id`, `name`, `micSensitivityOffset`,
  `octaveBandOffsets`, `isDefault`, `createdAt`, `updatedAt`.
- `sleep_sessions`: one-to-one Sleep Monitor -metadata `sessions.id`-avaimeen
  sarakkeilla `sessionId`, `targetDurationMinutes`, `keepAwakeEnabled` ja
  `createdAt`. Taulu cascadoituu, kun parent-session poistetaan.
- `sleep_notable_events`: Sleep-session event-rivit sarakkeilla `id`,
  `sessionId`, `timestamp`, `eventType`, optional `levelDb` ja optional
  `durationMs`. Taulu viittaa `sleep_sessions.sessionId`-avaimeen, joten
  notable eventteja ei voi tallentaa tavalliselle ei-Sleep-sessiolle ilman
  Sleep metadata -rivia.
- `passive_monitoring_samples`: käyttäjän käynnistämien Passive monitoring
  -samplejen aggregate-rivit sarakkeilla `id`, `startedAtMs`, `endedAtMs`,
  `readingCount`, `minDb`, `averageDb`, `maxDb`, `peakDb` ja `totalEnergy`.
  Taulu ei sisalla session ID:tä, raakaaudiota, PCM-bufferia tai YAMNet-windowia.

Repository/dataflow:

- `SessionRepository.recordActiveSessionMeasurements(...)` kirjoittaa
  measurement-rivit ja aktiivisen session runtime-summaryn samassa Room
  transactionissa.
- `SessionRepository.completeSessionWithMeasurements(...)` kirjoittaa
  viimeiset rivit ja sulkee session samassa transactionissa.
- `SessionRepository.createActiveSession(...)` tallentaa active-session
  alkuhetken `SessionTimeZoneOffsetResolver`illa ratkaistun historiallisen
  UTC-offsetin sekä valitun ja Androidin raportoiman routed audio input -metadatan,
  jos `AudioEngine.audioInputInfo` julkaisi sen onnistuneen AudioRecord-startin
  jalkeen.
- Normaali completion ratkaisee `endUtcOffsetSeconds`-arvon loppuhetken
  `ZoneId`-säännöillä. `completeRecoveredSessionWithMeasurements(...)` jättää
  loppuoffsetin `null`-arvoksi, koska prosessin kaatumisen jälkeen mennyttä
  device-zone-kontekstia ei voida todistaa nykyisestä aikavyöhykkeestä.
- `SessionTimeZoneOffsets.offsetForTimestamp(...)` käyttää start-offsetia
  alku- ja end-offsetia loppurajalla. Session keskellä sama offset on turvallinen
  vain, jos alku- ja loppuoffset ovat samat; DST-/zone-muutoksessa väliajan
  offset palautuu tuntemattomaksi, jolloin formatter käyttää eksplisiittistä UTC:ta.
- `SessionRepository.updateSessionLocation(...)` on optional
  `SessionLocationMetadata` -kirjoitusportti. `AudioSessionManager` kutsuu sita
  startissa ja stop-fallbackissa `SessionLocationCapturePort`in tuloksella;
  locationin puuttuminen tai capture/update-virhe ei kaada sessiota.
- `SessionRepository.getFilteredSessions(SessionHistoryQuery)` on Historyn
  hakudatan portti. Se säilyttää Free-käyttäjän 7 päivän policy-alarajan,
  antaa Pro-käyttäjälle koko historian ja mapittaa name/tag/date/avg dB/
  weighting/location-filtterit `SessionDao.searchSessions(...)` -kyselyyn.
  SQL-order on deterministinen `startTime DESC, id DESC`.
- `MeasurementRepository` on nykyisin read/analytics-repository. Se palauttaa
  hourly/daily/weighted/environment mix -virtoja ja tekee energia-average-
  mappaukset domain-malleihin.
- `SoundDetectionRepository.recordEvent(...)` on optional detection
  persistence -kirjoitusportti. `AudioSessionManager` kutsuu sita vain, kun
  kayttaja on Pro, live sound detection on paalla ja erillinen persistence-opt-in
  on paalla.
- `PassiveMonitoringRepository.recordSample(...)` kirjoittaa vain aggregate
  passive monitoring -samplet `passive_monitoring_samples`-tauluun.
  `observeDailySummary(...)` koostaa Settingsin daily summaryn samasta
  aggregate-datasta ilman `measurements`-riveja.
- `CalibrationOffsetPolicy` on flat mic sensitivity- ja octave-band-offsetien
  yhteinen +/-10 dB clamp/default-lahde. `OctaveCalibrationOffsets` omistaa
  octave-band-offsetien supported center frequency -listan, reset-to-zero-
  mallin ja deterministisen Room TEXT -codec-muodon.
- `CalibrationProfileRepository` mapittaa `CalibrationProfileDao`n Room-entityt
  domain-malliksi ja tarjoaa UI:sta riippumattomat `createProfile(...)`,
  `observeProfiles()`, `getProfile(...)`, `renameProfile(...)`,
  `deleteProfile(...)`, `updateOctaveBandOffsets(...)` ja
  `resetOctaveBandOffsets(...)` -polut. Se normalisoi flat
  `micSensitivityOffset`-arvon ja octave-offsetit `CalibrationOffsetPolicy`n
  kautta ja estaa viimeisen `isDefault`-profiilin poiston data-kerroksessa.
- Settingsin `AudioCalibrationSection` hallitsee calibration profile
  -profiileja ProLockOverlayn takana. `SettingsViewModel` mapittaa
  repository-virran `CalibrationProfileUiState`-riveiksi, joihin sisältyvät
  valitun profiilin octave-band-offsetit `OctaveCalibrationBandUiState`-listana.
  Settings näyttää valitulle profiilille `DbCheckSlider`-bandisäätimet ja reset-
  ikonipainikkeen; update/reset kirjoittaa `CalibrationProfileRepository`n
  `updateOctaveBandOffsets(...)`- ja `resetOctaveBandOffsets(...)` -polkuihin.
  ViewModel bootstrappaa Pro-kayttajalle `Device default` -profiilin vasta
  ensimmaisen Room-profiiliemission jalkeen, tallentaa selectin
  `selected_calibration_profile_id`-avaimeen ja vaihtaa valitun profiilin
  fallbackiin, jos kayttaja poistaa nykyisen valinnan. Free-kayttaja ei voi
  create/select/rename/delete/update/reset-profiileja ViewModelin kautta.
- DAO-kyselyissa on deterministiset `ORDER BY` -tie-breakerit, joissa
  aikaleiman lisaksi kaytetaan primary keyta.

DataStore-preferenssit:

- `theme_mode`
- `exposure_alerts`
- `peak_warnings`
- `notification_threshold`
- `notification_schedule_active_days`
- `notification_schedule_start_minute`
- `notification_schedule_end_minute`
- `mic_sensitivity_offset`
- `frequency_weighting`
- `response_time`
- `dosimeter_standard`
- `selected_calibration_profile_id`
- `selected_audio_input_device_id`
- `waveform_style`
- `refresh_rate`
- `lockscreen_meter`
- `show_lockscreen_meter_publicly`
- `health_connect`
- `heart_rate_overlay`
- `technical_metadata`
- `dosimeter_card`
- `sound_detection`
- `sound_detection_persistence`
- `sleep_card`
- `wav_recording_default`
- `audible_alarm`
- `tts_risk_prompt`
- `ambient_sound_preset`
- `ambient_sound_volume`
- `ambient_sound_timer_minutes`
- `tinnitus_left_pitch_hz`
- `tinnitus_right_pitch_hz`
- `tinnitus_pitch_updated_at_ms`
- `voice_baseline_level_db`
- `voice_baseline_sample_count`
- `voice_baseline_captured_at_ms`
- `debug_force_free`
- `is_pro_user`

`UserPreferenceDefaults` keskittaa defaultit ja normalisoinnin. Pro-mittausarvot
luetaan effective-arvoina `ProAudioPreferencePolicy`n kautta, joten Free-
kayttajan tallennettu vanha calibration, weighting, response time tai dosimeter
standard ei vaikuta mittauspolkuihin.

---

## Trends, Hearing, History ja Session Detail

Trends (sisainen `analytics`-reitti):

- `MeasurementRepository.getDailyAveragesLast7Days()` tuottaa viikon
  energia-average-paivapisteet.
- `AnalyticsViewModel` laskee weekly average -arvon ja kayttaa nullable
  `HearingHealthSummaryCalculator`ia compact Hearing-statushandoffin datalle. Sama laskuri palvelee Hearing-hubia;
  puuttuva sample-data ei muutu SAFE-arvioksi.
- `AnalyticsSection` omistaa Analyticsin section-valinnan (`OVERVIEW`,
  `SPECTRAL`, `ENVIRONMENT`), `AnalyticsOverviewRange` omistaa Overviewin
  `WEEKLY` / `MONTHLY` -range-valinnan, ja `SpectralMode` omistaa spektrikortin
  `BARS` / `SPECTROGRAM` / `RTA` -renderointitilan. `AnalyticsViewModel` sailyttaa nama
  omissa state-lahteissaan ja julkaisee ne `AnalyticsUiState.Success` -kentissa,
  jotta dataemissiot tai Compose-recomposition eivat palauta valintoja
  oletukseen.
- `AnalyticsSectionChipRow` nayttaa section-valinnan Analytics-headerin alla.
  Free-kayttajalle Spectral ja Env Mix ovat nakyvissa lukkoikonilla, eivat
  piilotettuina.
- `AnalyticsOverviewRangeChipRow` nakyy vain Overview-sectionissa. Weekly on
  Free-kayttajalle auki; Monthly nakyy Free-kayttajalle Pro-lukittuna, mutta
  valinta saa silti nayttaa locked-preview-kortin.
- `SpectralModeChipRow` renderoityy `SpectralAnalysisCard`issa. `SpectralMode`
  -arvot ovat `BARS`, `SPECTROGRAM` ja `RTA`; valinta säilyy
  `AnalyticsViewModel`in state-lähteessä dataemissioiden yli.
- `SpectrogramBuffer` on Analytics ViewModelin live-only UI-bufferi. Se
  muodostaa `AudioEngine.spectralFrame`-emissioista `SpectrogramUiState`-
  waterfall-rivit, sailyttaa enintaan 60 viimeisinta riviä ja ohittaa saman
  timestampin uudelleenemissiot.
- `SpectralAnalysisCard` renderöi Bars-, Spectrogram- ja RTA-haarat erikseen.
  `SpectrogramCanvasModel` muuntaa waterfall-rivit piirrettäviksi soluiksi.
  `RtaBarsModel` muuntaa `RtaUiState`-bandit octave-bar Canvasille ja PEAK/BANDS
  -stat pillien arvoiksi. `formatSpectralFrequency(...)` on UI:n yhteinen
  Hz/kHz-muotoilija.
- `analyticsSectionCards(...)` on Trendsin section-kohtaisen korttiryhmittelyn
  UI-lahde. Overviewin Weekly-range renderoi weekly exposure-, compact Hearing status- ja yearly report -kortit;
  Monthly-range renderoi monthly trend-, compact Hearing status- ja yearly report -kortit. Spectral renderoi
  live-spektrikortin; Environment renderoi Sound Detection-, optional active mix- ja Environment Mix -kortit.
  ViewModel hakee ja julkaisee samat UI-state-kentat riippumatta valitusta
  sectionista tai range-valinnasta.
- Trends ei omista hearing test-, recovery-, tinnitus-, Voice Baseline-, Sleep- tai Ambient-kortteja eika
  `HearingTestRepository`/`HearingRecoveryRepository`-riippuvuuksia. `onNavigateToHearing` on sen ainoa Hearing-handoff.
- Pro-kayttajalle Environment Mix lukee 7 paivan Room-countit
  `MeasurementRepository.getEnvironmentMixLast7Days()`-polusta.
- Pro-kayttajalle 30 paivan trendi ja 12 kuukauden raportti lasketaan
  `ExposureAnalyticsCalculator`illa. Ikkunat paivittyvat minuutin tickilla.
- Free-kayttajalle Pro-analytiikka palauttaa `LockedPreview`-tilat, ei oikeaa
  dataa overlayn alle.
- Live-spektri naytetaan Pro-kayttajalle `AudioEngine.spectralFrame`-virrasta;
  spektria tai spectrogram-bufferia ei persistoda `measurements.frequencyData`
  -kenttaan. Free-kayttajan spectrogram saa `LockedPreview`-tilan, ja null-frame
  tyhjentaa live-bufferin.
- Trendsin datavirran latausvirhe mapataan `AnalyticsUiState.Error`-
  tilaksi, joka nayttaa resursoidun fallback-viestin ja CTA:n Meteriin.

Hearing:

- `HearingScreen` on top-level-hubi ja `HearingViewModel` julkaisee sen yhden `HearingUiState`-tilan.
- `HearingUiState` sisaltaa Pro-tilan, yhteisen hearing-health-yhteenvedon, latest hearing testin, recovery-tilan,
  tinnitusprofiilin, effective Sleep-kortin nakyvyyden seka Voice Baseline -aggregaatit ja capture-gaten.
- `HearingScreenActions` keskittaa siirtymat olemassa oleviin hearing test-, recovery-, tinnitus-, ambient- ja
  Sleep fullscreen-flow'hin seka upgrade-polulle.
- Sisaltojarjestys on status + latest test, hearing test, recovery, tinnitus pitch, Voice Baseline ja tools, jossa
  optional Sleep Monitor tulee ennen Ambient Soundsia.
- `HearingHealthSummaryCalculator` on nullable yhteinen laskentalahde Hearing-hubille ja Trendsin kompaktilla
  `HearingStatusRow`-handoffille.
- Voice Baseline on Hearingin yksinomainen UI-vastuu. `HearingViewModel` vaatii capturelle Pro-oikeuden, aktiivisen
  mittauksen ja Sound Detectionin; Settings ei omista baseline-statea tai capture-toimintoa.

History:

- `HistoryViewModel` yhdistaa 24h-hourly-averaget, sessiot, Pro-tilan ja
  View All -tilan.
- Empty/error on compact app-shell-rakenne: top app bar + `EmptyState`, jonka CTA avaa Meterin. Successissa
  `HistorySuccessContent` saa `weight(1f)`-tilan ja renderoi ryhmat Today context, Sessions ja Summary yhteisen
  bottom barin ylapuolelle.
- Session-lista kayttaa kompaktia `SessionCard`-rakennetta; pitkille nimille/metadatalle on ellipsis ja kortissa
  sailyvat Sleep-badge, peak/average-arvot seka edit/lock-affordance.
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
  - `startZoneOffset` ja `endZoneOffset` tulevat session persistoiduista alku- ja
    loppuhetken UTC-offseteista; legacy-session tuntematon offset pysyy `null`-arvona.
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
- Hearing recovery käyttää samaa `HearingTestProcedure` / `HearingTestActiveScreen`
  -polkua `HearingTestMode.RECOVERY`-moodilla. Moodin frekvenssit ovat 1 kHz,
  4 kHz ja 8 kHz molemmille korville.
- `HearingRecoveryService.saveCompletedRecoveryCheck(...)` vaatii Pro-oikeuden
  ja viimeisimmän full hearing-test -baseline-tuloksen. Jos baseline puuttuu,
  tallennus epäonnistuu resursoidulla baseline-required-viestillä.
- `HearingRecoveryCalculator` laskee matching ear/frequency -threshold-deltat,
  `averageShiftDb`-, `maxShiftDb`- ja `STABLE` / `SMALL_SHIFT` /
  `ELEVATED_SHIFT` -statusarvon. `HearingRecoveryRepository` persistoi vain
  aggregate-tuloksen `hearing_recovery_results`-tauluun.

---

## Raportointi, export ja jakaminen

Session report:

- `SessionReportCalculator` laskee equivalent-level-arvon, durationin, LCpeakin,
  A-painotetun TWA/dosen, time-series-pisteet ja A-painotetut 85 dBA
  peak-event-jaksot.
- `SessionReportData` sisaltaa myos session custom-nimen, emojin, tagit seka
  `SessionTimeZoneOffsets`-metadatan.
- `PdfChartRenderer` keskittaa PDF Canvas -kaavion ja Session Detailin staattisen
  Compose-kaavion koordinaattimuunnoksen.

PDF:

- Session Detail kayttaa `ActivityResultContracts.CreateDocument("application/pdf")`.
- `ExportPdfReportUseCase` kirjoittaa natiivin `PdfDocument`in:
  5 sivua normaalisti, 6 sivua kun `ReportHeartRateSection.enabled` on true.
- Sivut: summary, metrics, data availability, time series, peak events ja optional heart rate.
- Metrics-sivun Report Context nayttaa app-version, Android-laitetiedon,
  persisted response time -summaroinnin, export-hetken effective calibration
  offsetin ja disclaimerin. Kalibrointioffset on export-metadataa, ei viela
  historiallinen session field ennen upstream-persistointia.
- Data Availability -sivu nayttaa vain valmiin upstream-datan: session location,
  A-painotetun NIOSH dosimeter standardin, projected dosen ja persisted sound detection -yhteenvedon.
  Octave breakdown pysyy N/A-tilassa, ellei `SessionReportData.octaveBreakdownAvailable` tai non-zero
  `octaveCalibrationOffsets` kerro saatavasta octave-kontekstista; RTA time-series -dataa ei viela persistöidä.
  Puuttuvat lahteet naytetaan `N/A`-tekstina, ei nollina.
- PDF:n date range, chartin aikarajat ja peak-event-ajat kayttavat session
  persistoidun alku-/loppuoffsetin mukaista aikaa ja nayttavat UTC-kontekstin.
  Legacy-session tuntematon offset renderoidaan eksplisiittisesti UTC:ssa.

PNG / Sharesheet:

- `ShareResultsGenerator.shareSessionStats(...)` on Meterin text/plain-share.
- `ShareResultsGenerator.shareHearingTestResults(...)` rakentaa hearing-test
  PNG-kortin.
- `ShareResultsGenerator.shareSessionReportCard(...)` rakentaa Session Detailin
  PNG-raporttikortin.
- Session Detailin PNG-kortin paivamaara kayttaa session persistointiaikaista
  start-offsetia; tuntematon legacy-offset naytetaan UTC:ssa.
- PNG-jaot kirjoitetaan `cache/exports/`-hakemistoon ja julkaistaan
  `FileProvider`in `content://`-URIlla.
- Jakointentit antavat valiaikaisen lukuoikeuden seka `EXTRA_STREAM`in etta
  `ClipData`n / `FLAG_GRANT_READ_URI_PERMISSION`in kautta.

CSV:

- Settingsin Data & Export kutsuu `SettingsViewModel.createCsvExportIntent()` all-sessions exportille.
- `ExportCsvUseCase` kirjoittaa kolme CSV-tiedostoa:
  sessioyhteenvedon, mittausrivit ja optional sound detection -eventit.
- `CsvExportSelection` tukee sekä all-sessions- että selected-session-id -batch-exportia.
  Settings käyttää all-sessions-polun; valitut sessiot käyttävät samaa tiedostojen, sivutuksen
  ja FileProviderin sopimusta.
- CSV-sarakkeissa ovat metadata-kentat `session_name`, `session_emoji` ja
  `session_tags`; measurement-exportissa myos `peak_db`, ja sound detection
  -exportissa vain aggregoidut `timestamp_utc`, `label` ja `confidence`.
- CSV:n `start_time_utc`, `end_time_utc`, `timestamp_utc` ja
  `sleep_created_at_utc` ovat locale-riippumattomia ISO-8601 UTC-instanteja
  (`DateTimeFormatter.ISO_INSTANT`, esimerkiksi `2026-07-15T09:00:00Z`).
  Numeroarvot pysyvat pisteellisessa koneformaatissa.
- Mittausrivit luetaan sivuina
  `MeasurementDao.getMeasurementsForSessionExportPage(...)`-polulla, jotta
  export ei rakenna koko raw-aineistoa muistiin.
- Sound detection -eventit luetaan sivuina
  `SoundDetectionEventDao.getEventsForSessionExportPage(...)`-polulla.
- CSV-jako kayttaa `ACTION_SEND_MULTIPLE`-intentia ja FileProvider-URIja.
- Settingsin Clear history -toiminto on kaikkien kayttajien datanhallintatoiminto. Se vaatii
  vahvistusdialogin, estyy aktiivisen mittauksen aikana ja kutsuu `HistoryClearService.clearHistory()`
  -polkua. `SessionRepository.clearInactiveHistory()` poistaa inactive-sessiot Room-transactionissa,
  child-rivit poistuvat foreign-key cascaden kautta, ja `WavRecordingFileStore` poistaa poistettujen
  sessioiden WAV-tiedostot. `filesDir/backups` ei kuulu clear history -poistoon.

Settings Display & Features:

- Settings on nested graph: hub on `settings/home`, ja kaikki child-sivut kayttavat samaa graph-scoped
  `SettingsViewModel`-instanssia. Hub omistaa vain sivuvalinnan; childit omistavat omat sectioninsa, launcherinsa ja
  toimintonsa.
- WAV recording, public lockscreen meter ja passive monitoring kayttavat samaa `CompactDisclosureInfo`-mallia:
  disclosure naytetaan inline vain toiminnon ollessa aktiivinen/opt-in paalla; muuten kompakti privacy-label nakyy ja
  erillinen info-IconButton avaa dialogin. Tertiary-painikkeet sailyttavat resurssitekstin normaalin kirjainkoon.
- `DisplayAndFeaturesSection` omistaa Settingsin theme-, waveform style- ja refresh rate -chipit seka
  feature togglet `settings/display`-sivulla. `settings/data_privacy` omistaa erillisen
  `LockscreenMeterSection(showTitle = false)` -kortin ja sen ProLockOverlay-gaten.
- `show_lockscreen_meter_publicly` on lock-screen meterin erillinen default OFF -opt-in. Settings nayttaa
  privacy-warningin live dB -lukemien nakymisesta lukitusnaytolla, ja `SettingsViewModel` nayttaa public-asetuksen
  effective ON -tilassa vain Pro-kayttajalle, kun myos `lockscreen_meter` on effective paalla.
- Feature togglet ovat DataStore-pohjaiset `technical_metadata`, `dosimeter_card`, `sound_detection` ja `sleep_card`.
  `SettingsViewModel` nayttaa Pro-only-togglet Free-tilassa effective OFF -arvoina eika anna Free-kayttajan enabloida
  niita ViewModelin kautta.
- `technical_metadata` ohjaa Meterin session info -kortin Pro-teknisia tietoja kuten sample rate ja input device.
  `dosimeter_card` ohjaa Meterin Pro-dosimeter modea ja korttia, ja jos arvo poistuu paalta, ViewModel palauttaa
  mittaustilan DB meter -tilaan. Free-kayttajan lukittu dosimeter-chip ei nayta Pro-dataa.
- `sound_detection` kayttaa samaa avainta kuin `AudioSessionManager`in inference-gate; Trends piilottaa Environment
  -osion sound detection -kortin, kun toggle ei ole effective paalla. `sleep_card` on persisted Pro-gatettu visibility
  -asetus Sleep Monitor -kortille: Meter ja Hearing-hubi nayttavat `SleepSetupCta`-kortin vain effective Pro ON
  -tilassa, ja `Screen.SleepSetup` / `sleep/setup` gateaa Free/deep-link -execution-polun upgradeen.
- `SleepSetupViewModel` hoistaa Sleep setup -ruudun valmistelutilan: Pro-readiness tulee effective `isProUser`-arvosta,
  ei `sleep_card`-visibility-asetuksesta. Valmisteltavat valinnat ovat 6h/8h/10h target-kesto ja `keepAwakeEnabled`.
  Free-tila pysyy locked-tilassa eika ViewModel muuta setup-valintoja.
- Sleep active recording kayttaa samaa `MeasurementForegroundService`- ja `AudioSessionManager`-mittauspolkua kuin Meter.
  `MeasurementRecordingMode.Sleep` valitsee Sleep notification copyn ja target-duration auto-stopin, ja
  `AudioSessionManager.startSleepSession(...)` kirjoittaa `SleepSessionRepository`n kautta `sleep_sessions`-metadatan
  luodulle tavalliselle session ID:lle. `MeasurementRecordingMode.Passive` on erillinen aggregate-only foreground
  sample eikä käytä Sleep-session polkua.
- `KeepScreenOnEffect` on yhteinen Window-flag-helper. Meter pitaa ruudun hereilla aktiivisessa mittauksessa; Sleep
  pitaa ruudun hereilla vain kayttajan `keepAwakeEnabled`-opt-inilla, muuten foreground service jatkaa mittausta ilman
  UI:n paalla pysymista.
- Sleep results lukee `sleep_sessions`-metadatan `SleepSessionRepository`n read-flow'illa. History saa erillisen Sleep
  session ID -joukon UI-stateen ja nayttaa Sleep-badgen `SessionCard`issa muuttamatta tavallista `Session`-mallia.
- Session Detail nayttaa Sleep Results -kortin vain Sleep-session metadatalle. `SleepResultsCalculator` muodostaa
  target/recorded-keston, equivalent levelin, maxin, LCpeakin, peak-event-countin, loud-period-countin ja histogram
  bucketit olemassa olevasta `SessionReportData`sta.
- Sleep export/report käyttää samaa report-dataflow'ta: `SessionDetailViewModel` tallentaa Sleep-yhteenvedon
  `SessionReportData.sleep` / `ReportSleepSection` -kenttiin, PDF:n Data Availability -sivu näyttää Sleep-rivit
  `N/A`-fallbackeilla ja sessions CSV hakee `sleep_sessions`-metadatan `SleepSessionDao`n export-kyselyllä.
- Sleep insights on report-pohjainen domain-analyysi: `SleepInsightsCalculator` muuntaa `SessionReportData.timeSeries`
  -sarjan loud-period notable event -yhteenvedoiksi ja palauttaa `MissingMeasurements`, kun time-series puuttuu.
  `SleepResultsCalculator` jättää peak/loud/sample-countit nullable-arvoiksi unavailable-tilassa, jotta Session Detail
  näyttää `N/A`-fallbackin eikä nollaa.
- Audible alarm policy on pure domain -kerroksessa: `AudibleAlarmPolicy` omistaa 90 dB / 30 s / 5 min oletukset ja
  `AudibleAlarmEvaluator` palauttaa `BelowThreshold`, `Waiting`, `CoolingDown` tai `Trigger` -päätöksen. Thresholdin
  alitus resetoi duration-ikkunan, ja cooldownin jälkeen vaaditaan uusi duration-ikkuna ennen seuraavaa triggeriä.
- Audible alarm playback on Pro-gatettu runtime-polku: `audible_alarm` DataStore-default on OFF, Settingsin Noise
  Notifications -kortti tarjoaa toggle- ja preview-polun Pro-käyttäjälle, `MediaPlayerAudibleAlarmPlayer` soittaa bundled
  `res/raw/audible_alarm.wav` -äänen transientilla audio focus -pyynnöllä ja `USAGE_ALARM`-attribuutilla sekä vapauttaa
  playerin ja audio focuksen completion-, error-, focus-loss- ja start-failure-polkujen jälkeen.
  `AndroidAudibleAlarmPlaybackGuard` estää toiston,
  jos näyttö ei ole interactive-tilassa tai proximity-sensori on peitetty. `AudioSessionManager` välittää live weighted
  dB -lukemat `AudibleAlarmPlaybackController`ille ja pysäyttää guardin kaikissa session stop/failure/cleanup-polkuissa.
- Voice baseline käyttää olemassa olevaa YAMNet/Sound Detection -polkua: `VoiceBaselineCalibrator` aggregoi vain
  `Speech`-luokittelemien live-jaksojen weighted dB -lukemat, `AudioSessionManager.captureVoiceBaseline(...)` palauttaa
  capturen vain Pro + aktiivinen mittaus + Sound Detection -ehdolla, ja DataStore tallentaa vain
  `voice_baseline_level_db`, `voice_baseline_sample_count` ja `voice_baseline_captured_at_ms` -arvot. Raakaaudiota,
  PCM-bufferia tai YAMNet-windowia ei persistöidä baselinea varten.
- Voice volume warnings käyttää samaa YAMNet/Sound Detection -live-luokitusta ja tallennettua baseline-aggregaattia:
  `VoiceVolumeWarningEvaluator` vaatii `Speech`-luokituksen, baseline + 8 dB -ylityksen 3 sekunniksi ja 60 sekunnin
  cooldownin. `AudioSessionManager` dispatchaa triggerissä best-effort haptic-palautteen sekä
  `NotificationHelper.sendVoiceVolumeWarning(...)` -alert-kanavan notificationin. Polku ei lisää raakaaudion
  tallennusta, uutta Room-skeemaa tai background microphone -toteutusta.
- TTS risk prompt on Pro-gatettu opt-in-polku: `tts_risk_prompt` DataStore-default on OFF, Settingsin Noise
  Notifications -kortti näyttää Spoken risk prompt -kytkimen, ja `TtsRiskPromptEvaluator` triggeröi vain
  dosimeter-pohjaisista `DOSE`/`PROJECTED_DOSE` -riskieventeistä. `AudioSessionManager` kutsuu
  `TtsRiskPromptController`ia vain olemassa olevan mittauksen riskipäätöksistä ja antaa sille effective Pro +
  opt-in-, Sound Detection -saatavuus- ja latest hearing-test-baseline -tilat. `AndroidTextToSpeechPlayer` käyttää
  Android `TextToSpeech` -APIa `QUEUE_FLUSH`-toistolla, ja manifestin `<queries>` sisältää Android 11+ TTS service
  -näkyvyysdeklaraation. Spoken copy on varovainen melualtistuskehotus eikä tee diagnoosi-, kuulovaurio- tai
  turvallisuusväitteitä; polku ei persistoi raakaaudiota, YAMNet-windowia, hearing-test-muutosta tai uutta Room-dataa.
- Hearing recovery check on Pro-gatettu lyhyt kuulotestipolku full hearing-test-baselineen verrattavaksi.
  `HearingTestMode.RECOVERY`
  käyttää samaa `HearingTestProcedure`- ja `HearingTestActiveScreen` -toteutusta kuin full hearing test, mutta rajaa
  frekvenssit arvoihin 1 kHz, 4 kHz ja 8 kHz molemmille korville. `HearingRecoveryService` vaatii Pro-oikeuden ja latest
  full hearing-test-baselinen, laskee `HearingRecoveryCalculator`illa vain matching ear/frequency -threshold-deltat ja
  tallentaa aggregate-tuloksen `HearingRecoveryRepository`n kautta. Room schema v12 lisää
  `hearing_recovery_results`-taulun: `baselineTestId`, timestamp, tested count, average/max shift, status sekä left/right
  shift data; taulu ei sisällä raakaaudiota, PCM-bufferia, YAMNet-windowia tai uutta kliinistä audiometriadataa.
- Hearing-hubi näyttää `HearingRecoveryCard`in. Missing-baseline-tila ohjaa full hearing testiin, ready/result-tila
  avaa short recovery setup -polun, ja Free-käyttäjä näkee locked-previewn ilman recovery-dataa. Recovery-copy kuvaa
  tuloksia vain personal tracking -vertailuna eikä diagnoosi-, kuulovaurio- tai turvallisuusväitteenä.
- Tinnitus scope gate 2026-06-28: tinnitus ei kuulu v1.0-releaseen. Osa 91 saa edetä aikaisintaan v1.5-tason
  personal tracking -pitch profileksi: käyttäjän itse käynnistämä ToneGenerator-pohjainen pitch matching, ear-specific
  profiili ja playback limits. Se ei saa sisältää diagnoosia, hoitoa, oireiden vähentämis-/parantamisväitteitä,
  kuulovaurio- tai turvallisuusväitteitä, Health Connect -kirjausta, background playbackia, sound therapyä tai
  automaattisia triggereitä. Vanha Osa 92 sound therapy -scope pysyy pois rajauksesta; Osa 92:n hyväksytty toteutus on
  erikseen rajattu ambient sound playback ilman medical/therapy-väitteitä, oireseurantaa, Health Connectiä tai
  automaattisia triggereitä. Ennen tinnitus-ominaisuuden julkaisua tarkista Google Playn
  health content / user data -vaatimukset, health disclaimer / declaration -tarve ja FDA:n device software
  -käyttötarkoitusrajaus.
- Tinnitus pitch matcher on toteutettu Osa 91:n rajattuna v1.5 personal tracking -ominaisuutena. `domain/tinnitus`
  omistaa `TinnitusPitchProfile`-mallin ja `TinnitusPitchPolicy`n, joka normalisoi pitch-arvot nykyisen
  hearing-test-taajuusalueen 250-8000 Hz sisään 50 Hz stepillä ja käyttää previewlle kiinteää -36 dB amplitudia.
  DataStore-avaimet ovat `tinnitus_left_pitch_hz`, `tinnitus_right_pitch_hz` ja `tinnitus_pitch_updated_at_ms`;
  Room-skeemaa ei muutettu. Hearing-hubi näyttää `TinnitusPitchCard`in, joka avaa `tinnitus/pitch`-reitin.
  Free-käyttäjän effective pitch profile on tyhjä/locked, eikä `TinnitusPitchMatcherViewModel` previewaa tai tallenna
  profiilia ilman Pro-oikeutta. Toteutus ei lisää background playbackia, serviceä, media notificationia, sound therapyä,
  Health Connect -kirjausta, raakaaudiota tai automaattisia triggereitä.
- Ambient sound playback on Osa 92:n rajattu Pro-ominaisuus: Hearing-hubi näyttää `AmbientSoundCard`in ja avaa
  non-top-level `ambient/playback` -reitin. `AmbientSoundPlaybackViewModel` gateaa Playn Pro-oikeuteen,
  käyttäjätoimintoon ja Android 13+ notification-lupaan; Free-käyttäjä ei voi käynnistää playbackia eikä persistöidä
  ambient-asetuksia.
- Ambient playbackin DataStore-avaimet ovat `ambient_sound_preset`, `ambient_sound_volume` ja
  `ambient_sound_timer_minutes`; `AmbientSoundPolicy` normalisoi presetit `WHITE_NOISE`/`PINK_NOISE`/`BROWN_NOISE`/`FAN`,
  volume-alueen `0.05f..1.0f` ja timer-vaihtoehdot `0/15/30/60/120`.
- `AmbientSoundPlaybackService` on erillinen `mediaPlayback` foreground service omalla
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissionilla ja low-importance playback notification channelilla. Se ei käytä
  `MeasurementForegroundService`ä, `RECORD_AUDIO`-lupaa, mikrofonityyppiä, Room-skeemaa, playback-historiaa,
  raakaaudiota, pilvisynkkaa tai Health Connect -kirjausta.
- `AmbientSoundPlayer` generoi white/pink/brown/fan PCM16-äänen paikallisesti `AudioTrack.MODE_STREAM` -toistoon
  `USAGE_MEDIA` / `CONTENT_TYPE_MUSIC` -attribuuteilla. Audio focus permanent loss pysäyttää, transient loss pausettaa,
  ja sleep timer vain pysäyttää jo käyttäjän käynnistämän playbackin.
- Passive monitoring on käyttäjän Settingsistä käynnistämä lyhyt foreground-service sample. Settingsin Noise
  Notifications -kortti näyttää disclosure-copyt, pyytää mikrofoniluvan käyttäjätoiminnolla ja käynnistää
  `MeasurementForegroundService.startPassiveMonitoringIntent(...)` -polun; notificationissa on ongoing Stop-toiminto.
- `MeasurementRecordingMode.Passive` ei käytä `AudioSessionManager.startSession()`ia. `PassiveMonitoringManager` lukee
  live dB -arvot `AudioEngine`sta, pitää runtime-tilastot muistissa ja pysäytyksessä persistoi vain aggregate-samplen.
  Se ei luo sessiota, ei kirjoita `measurements`-rivejä, ei käynnistä WAV-, Sound Detection-, spectral-, audible alarm-,
  voice warning- tai alert-trigger-polkuja eikä emittoi completed-session navigointia.
- Room schema v11 lisää `passive_monitoring_samples` -taulun aggregate-kentille (`startedAtMs`, `endedAtMs`,
  `readingCount`, min/avg/max/peak ja `totalEnergy`). `PassiveMonitoringRepository.observeDailySummary(...)` tuottaa
  Settingsin daily summaryn. Clear history poistaa myös passive monitoring -summaryt.
- Ilman uutta eksplisiittistä product/privacy-päätöstä dBcheck ei saa lisätä bootista, ajastimesta, receiveristä,
  WorkManagerista tai muusta taustatriggeristä alkavaa mikrofonisamplingia eikä raakaaudion, PCM-bufferien tai
  YAMNet-windowien persistointia.

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
- Backupin `copyCheckpointedDatabase(...)` tekee Roomille
  `PRAGMA wal_checkpoint(TRUNCATE)` -checkpointin, aloittaa sen jälkeen
  SQLite-transactionin kirjoittajien poissulkemiseksi ja kopioi vasta, kun
  WAL-sidecar on tyhjä. Snapshotia yritetään enintään kolme kertaa; jatkuva
  WAL-kirjoituskilpa päättyy hallittuun
  `Database changed while starting backup snapshot` -virheeseen eikä
  mahdollisesti epäjohdonmukaiseen backupiin.
- Tiedostokopio kirjoitetaan temp-tiedostoon `FileOutputStream`in kautta,
  `output.fd.sync()` kutsutaan ennen sulkemista ja valmis tiedosto
  siirretään korvaavasti ensisijaisesti atomisella movella. Backup validoidaan
  ennen final movea.
- Restore validoi valitun backupin ennen nykyisen tietokannan korvaamista.
- Restore luo `dBcheck_pre_restore_*`-turvakopion ennen korvausta ja validoi
  myos safety backupin.
- Restore poistaa vanhat `dbcheck.db-wal`- ja `dbcheck.db-shm`-sidecarit ennen
  korvaavaa tietokantatiedostoa.
- Backup/restore-operaatiot sarjallistetaan `LocalBackupManager`in `Mutex`illa.
- `MeasurementDatabaseGate` estaa atomisesti mittauksen elinkaaren ja backup/restore-operaation paallekkaisyyden.
  `AudioSessionManager` pitaa gaten session kaynnistyksesta Room-completioniin; `LocalBackupManager` ottaa saman gaten
  koko backupin/restoren ajaksi. Settingsin aktiivisen mittauksen tarkistus on nopea UI-palaute, ei ainoa concurrency-
  suoja.
- Gaten kaksisuuntainen sopimus on regressiotestattu: aktiivinen mittaus estää
  backupin/restoren, backup-permit estää audio-startin ennen AudioRecordia ja
  valmis session completion vapauttaa gaten seuraavalle backupille.
- Onnistunut restore kutsuu Settingsin restore-confirm-polusta annettua
  `onRestartAfterRestore`-callbackia suoraan `SettingsViewModel.confirmRestoreBackup(...)`
  -korutiinissa. `MainActivity` toteuttaa callbackin prosessin restartilla.
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
- Notification channelit ovat `measurement_channel`, `alerts_channel` ja
  `ambient_playback_channel`. Ambient playback -kanava on low-importance,
  ongoing ja private; alert-kanavaa kayttavat exposure/peak/voice warning
  -notificationit.
- `NotificationPrivacyPolicy.measurementLockscreenVisibility(...)` palauttaa public-visibilityn vain ehdolla
  Pro + `lockscreenMeterEnabled` + `showLockscreenMeterPublicly`; muuten measurement notification pysyy
  `NotificationCompat.VISIBILITY_PRIVATE` -tasolla.
- Pro + `lockscreenMeterEnabled` kayttaa custom collapsed/expanded
  `RemoteViews`-layoutteja, joissa nakyvat current dB, peak dB, kesto ja
  noise-level-piste.
- Free tai lockscreen-asetus pois paalta kayttaa tavallista private
  measurement notificationia.
- `NoiseNotificationSchedule` on DataStoreen persistöity malli active
  days/hours -rajaukselle. Active days tallennetaan ISO-8601 `DayOfWeek.value`
  -arvoina, tunnit minute-of-day -arvoina. Sama start/end tarkoittaa koko
  valittua paivaa; start > end ylittaa yon ja aamuyon osuus kuuluu edellisen
  aktiivisen paivan ikkunaan. Settingsin Noise Notifications -kortti lukee
  `SettingsUiState.notificationSchedule`-arvon, paivittaa aktiiviset paivat
  chip-rivilla ja start/end-tunnit slidereilla, ja kirjoittaa muutokset
  `SettingsViewModel`in kautta `PreferencesRepository.updateNotificationSchedule(...)`
  -porttiin. `AudioSessionManager` valittaa schedule-arvon alert-runtimeen ja
  `NoiseAlertEvaluator` kunnioittaa sita ennen exposure- tai peak-alertin
  yritysta.
- Extended exposure alertit voivat laueta 30 minuutin threshold-average-
  saannosta, 100 % actual dosesta tai 100 % projected dosesta. `NoiseAlertPolicy`
  omistaa nama rajat, 120 dB peak-rajan ja 30 minuutin retry-cooldownin.
  Onnistuneen deliveryn jalkeen sama alert-tyyppi ei toistu session aikana;
  epaonnistunut delivery voi retryta cooldownin jalkeen.
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

- `app/src/test/java/com/dbcheck/app` sisaltaa **223 Kotlin-lahdetiedostoa**
  unit-testien ja testiapurien alla.
- Kattavuusalueet: Billing, ProFeatureManager startup, CSV/export/cache,
  Room schema/DAO/query contract, History search filters, DataStore mapping,
  repository rolling windows/transactions/history policy, domain audio/math/
  weighting/FFT/spectral, hearing-test procedure/result scoring, hearing
  recovery, tinnitus pitch, ambient sound policy/playback, passive monitoring,
  audible alarm, voice baseline/warnings, TTS risk prompt, report calculator,
  session metadata, privacy config, foreground service policy,
  AudioSessionManager start/failure, notification policy/helper/noise-level,
  Health Connect payload/manager/mapper, LocalBackupManager, accessibility
  plural resources, analytics/history/meter/settings ViewModelit, navigation
  policy, localization baseline, release QA document contracts, Gradle wrapper
  checksum pinning, PDF chart rendering, report text, share generation, string
  resource ids, user-facing error mapping and widget state.

Screenshot-testit:

- `ComponentScreenshotTests.kt` sisaltaa 56 komponenttipreviewta.
- `FullScreenScreenshotTests.kt` sisaltaa 34 light/dark full-screen -tilaa ja 5 fontScale = 1.5f -previewta.
- Rekursiivisesti tiedostojarjestelmasta laskettuna source setissa on yhteensa 95 `@PreviewTest`-funktiota ja
  `app/src/screenshotTestDebug/reference/...`-puussa 95 baseline-PNG:ta (56 komponenttia + 39 full-screen-referencea).
- Screenshot-source set on kytketty AGP:n kokeellisella
  `android.experimental.enableScreenshotTest = true` -asetuksella.
- UI-komponenttien animaatioita voi poistaa screenshot-determinismia varten
  esim. `animationsEnabled=false`-parametreilla.

Keskeisia nykyisia regressiosuojia:

- `RoomSchemaContractTest` - Room schema version/migrations/schema contract.
- `SessionTimeZoneSchemaContractTest`, `SessionTimeZoneOffsetsTest`,
  `SessionMappersTest`, `CsvExportFormatterTest`, `ReportTextFormatterTest` ja
  `HealthConnectNoiseDosePayloadTest` - v13-offsettien schema-, mapping-,
  UTC-export-, user-facing format- ja Health Connect -sopimus.
- `SessionRepositoryTransactionContractTest` - session summary + measurement
  write transaction contract.
- `AudioSessionManagerAudioStartTest` - AudioRecord start/failure behavior.
  Sama testiluokka todentaa myös shared database gaten backup-blockin ja
  completionin jälkeisen vapautuksen.
- `MeasurementForegroundServicePolicyTest` - foreground service start/stop policy.
- `PassiveMonitoringManagerTest`, `PassiveMonitoringRepositoryTest` ja
  `PassiveMonitoringAggregatorTest` - passive aggregate sample -polku ilman
  session/measurement- tai raw-audio-persistointia.
- `MeterStartupPermissionPolicyTest` - startup permission prompts.
- `ProAudioPreferencePolicyTest` - Free/Pro effective audio preferences.
- `HearingTestServiceProGateTest` - hearing-test execution/save gate.
- `HearingRecoveryServiceTest`, `HearingRecoveryRepositoryTest` ja
  `HearingRecoveryCalculatorTest` - recovery baseline, aggregate-shift ja
  v12-tallennuspolku.
- `ResultsViewModelShareTest` - hearing-test share gate and intent path.
- `SessionDetailScreenActionTest` and `SessionDetailViewModelMetadataTest` -
  PDF/metadata/Pro action contracts.
- `PrivacyConfigTest` - backup/fileprovider/privacy config.
- `LocalBackupManagerTest` - local backup/restore validation.
- `HealthConnectManagerTest`, `HealthConnectNoiseDosePayloadTest`,
  `HealthConnectHeartRateMapperTest` - Health Connect contracts.
- `AmbientSoundPlaybackServicePolicyTest`, `AmbientSoundPlaybackViewModelTest`,
  `AmbientSoundPolicyTest` ja `AmbientSoundGeneratorTest` - user-started local
  mediaPlayback -polun gate, policy ja generointi.
- `MediaPipeSoundClassifierTest`, `YamnetAudioWindowAdapterTest` ja
  `NativeLibraryCompatibilityTest` - MediaPipe-runtime lifecycle/category
  mapping, anti-alias-resampling/window-hop sekä legacy 4 KB Task Audio
  -riippuvuuden poissaolo.
- `NavigationRoutePolicyTest`, `ProRouteAccessViewModelTest` ja
  `SleepSetupEntryPolicyTest` - non-top-level-routejen näkyvyys, nullable
  entitlement -entry ja Sleepin Loading/Locked/Ready-ohjaus.
- `AudibleAlarmPlaybackControllerTest`, `MediaPlayerAudibleAlarmPlayerContractTest`,
  `VoiceBaselineCalibratorTest`, `VoiceVolumeWarningPolicyTest`,
  `TtsRiskPromptPolicyTest`, `TtsRiskPromptControllerTest` ja
  `AndroidTextToSpeechPlayerContractTest` - audible/voice/TTS-riskipolkujen
  domain- ja service-sopimukset.
- `TinnitusPitchPolicyTest`, `TinnitusPitchMatcherViewModelTest` ja
  `TinnitusPitchMatcherScopeTest` - pitch normalisointi, Pro-gate ja scope guardit.
- `PluralAccessibilityResourceTest` - pluralized accessibility strings.
- `AccessibilityAuditPolicyTest` - Osa 93:n source-level touch target, role ja selected-state guardit.
- `LocalizationBaselineTest` - Osa 94:n `values-fi` baseline, placeholder-pariteetti ja uusien UI-pintojen inline-tekstiscanni.
- `PermissionDeviceQaMatrixTest`, `BillingProductionQaTest`, `ReleaseSigningQaTest` ja `QodanaCiCompatibilityTest` - Osa 95-98 QA-dokumenttien ja release-riskien sopimukset.
- `FontLicenseNoticeTest` - paketoitujen Manrope/Space Grotesk -fonttien
  copyright- ja OFL-noticejen mukanaolo `app/src/main/assets/licenses/`-polussa.
- `GradleWrapperSecurityTest` - Gradle distribution checksum pinning.
- `UserFacingErrorTest` - teknisia exception-viesteja ei kayteta
  kayttajalle naytettavina virheina.

Taman `PROJECT.md`-paivityksen yhteydessa ei ajettu uutta Gradle-testisuitea
eika projektin `lc`/`sc` wrapper-skripteja. Laskennalliset faktat tarkistettiin
nykyisista lähde-, schema-, manifest-, workflow- ja resource-tiedostoista.

---

## Lint, analyysi ja paikalliset wrapperit

Projektin AGENTS.md ohjeistaa:

- `lint-check` / `lc`: kayttajan ajama skripti, joka ajaa ktlint + detekt +
  Android lint ja kirjoittaa tulokset `reports/`-hakemistoon.
- `security-check` / `sc`: kayttajan ajama skripti, joka ajaa dependency
  verificationin, OSV:n, OWASP Dependency-Checkin, Gitleaksin, TruffleHogin,
  Semgrep secretsin ja Semgrep Kotlin lightin ja kirjoittaa tulokset
  `reports/`-hakemistoon.
- Kun kayttaja sanoo "lue lint-tulokset", luetaan `reports/ktlint.txt`,
  `reports/detekt.txt` ja `reports/lint.txt`.
- Kun kayttaja sanoo "lue security-tulokset", luetaan
  `reports/security-summary.txt`, `reports/security-deps.txt`,
  `reports/security-deps-raw.txt`, `reports/osv.txt`,
  `reports/semgrep-kotlin.txt`, `reports/semgrep-secrets.txt`,
  `reports/gitleaks.txt` ja `reports/trufflehog.txt`. Nykyinen wrapper ei
  tuota `reports/security-code.txt`-tiedostoa.
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
- Dependency locking on paalla root-projektin `allprojects`-tasolla, ja root-buildscriptin plugin-/scanner-classpath
  lukitaan erikseen `buildscript-gradle.lockfile`-tiedostoon.
- `settings.gradle.kts` pysayttaa Gradle-ajon, jos `gradle/verification-metadata.xml` tai
  `buildscript-gradle.lockfile` puuttuu tai on tyhja, joten CI ei voi jatkaa ilman dependency verificationia tai
  buildscript-lockausta.
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
| Qodana | `.github/workflows/qodana.yml` | JetBrains Qodana action v2026.1.3, ei-blokkaava `Qodana Analysis (non-blocking AGP 9.2 risk)` -status ja `continue-on-error: true` kunnes Qodana-yhteensopivuus paatetaan nostaa blokkaavaksi |
| Android Release Build | `.github/workflows/release-build.yml` | PR:ssa unsigned release APK/AAB; push ja manual dispatch vaativat kaikki release signing -secretit ja tuottavat signed buildin; apksigner/jarsigner verification |

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
- workflow kirjoittaa AGP 9.2.1 -yhteensopivuusriskin `GITHUB_STEP_SUMMARY`yn eikä `continue-on-error`-asetusta saa poistaa
  ennen erillista paatosta muuttaa Qodana blokkaavaksi.

Release signing:

- Release signing lukee Gradle propertyt tai environment-muuttujat:
  `DBCHECK_RELEASE_STORE_FILE`, `DBCHECK_RELEASE_STORE_PASSWORD`,
  `DBCHECK_RELEASE_KEY_ALIAS`, `DBCHECK_RELEASE_KEY_PASSWORD`.
- Jos osa release signing -arvoista on annettu mutta ei kaikkia, Gradle failaa
  eksplisiittisesti.
- Non-PR release workflow vaatii `DBCHECK_RELEASE_KEYSTORE_BASE64`-,
  `DBCHECK_RELEASE_STORE_PASSWORD`-, `DBCHECK_RELEASE_KEY_ALIAS`- ja
  `DBCHECK_RELEASE_KEY_PASSWORD`-secretit ja failaa ennen buildia, jos yksikin
  puuttuu. Vain PR-polku rakentaa tarkoituksella unsigned APK/AAB-artifactit.
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

Vaatii JDK 21:n, compileSdk 37:n tarjoavan Android SDK:n ja targetSdk 36:n
runtime-käytäntöjen huomioinnin.

---

## Toteutusvaiheet nykykoodin perusteella

### Phase 1 - MVP

Toteutettu paaosin:

- Projektirakenne, Hilt, Room v13, DataStore.
- Design system ja komponenttikirjasto.
- Meter, Trends, Hearing, History ja Settings.
- AudioRecord-pohjainen live-mittaus.
- Foreground service mikrofonityypilla.
- Google Play Billing -backend, Settingsin ostovirta ja Pro-gating.
- Debug-only Force Free -toggle Pro-gatejen testaamiseen.

### Phase 2 - Enhancement

Toteutettu tai kytketty merkittavilta osin:

- Kuulotesti-flow ja tulosten tallennus Pro-kayttajalle.
- Hearing recovery -short check baselineen verrattuna.
- FFTProcessor ja SpectralAnalyzer Pro-gatettuun live-spektrikorttiin.
- SessionNamingSheet Historyssa ja Session Detailissa.
- CSV-export Settingsissa.
- Glance-widget Pro-gatella.
- Hearing Results -jakaminen PNG-share-polulla.
- Laaja strings.xml-resursointi default English -kielella ja rajattu
  `values-fi` launch-baseline.

### Phase 3 - Polish

Osittain toteutettu:

- LocalBackupManager paikallisiin backup/restore-toimintoihin.
- ShareResultsGenerator tekstille, hearing-test PNG:lle ja Session Detail PNG:lle.
- MonthlyTrendChart ja YearlyReportCard Pro-gatettuun analytics-dataflow'hun.
- PDF-reportti Session Detailista.
- Screenshot baseline -testit kriittisille Compose-komponenteille.
- Passive monitoring 5 minuutin aggregate sample -polkuna.
- Audible alarm, voice baseline / warning ja spoken risk prompt rajattuina
  Pro-opt-in -polkuina.
- Ambient sound playback erillisessä mediaPlayback foreground servicessä.

Puute: pilvi-/Google Drive -backupia ei ole.

### Phase 12 - kilpailukykyominaisuudet

Osittain toteutettu:

- Health Connect -melusessiosynkkaus ja sykeoverlay.
- B-painotus ja ITU-R 468 -painotus A/C/Z:n lisaksi.
- Lock-screen live meter custom notificationina.
- Session Detail -nakymä.
- PDF-raportti ja Session Detail PNG-jako.
- LCpeak ja `measurements.peakDb` schema v3:ssa.
- Camera Overlay photo/video share -perusta.
- Sleep Monitor schema, setup, active recording, results, insights ja export.
- Live sound detection YAMNet/MediaPipe Tasks Audio -polulla ja optional aggregate event
  -persistoinnilla.
- Tinnitus pitch profile rajattuna personal tracking -ominaisuutena.

Puute: Health Connectissa ei ole natiivia melu- tai audiometriatietuetta, joten
melu mallinnetaan exercise sessionina ja kuulotestin synkkaus skipataan.

---

## Koodintarkistuksen kannalta kriittiset sopimukset

Nama ovat hyvia kysymysaiheita tuleviin code review -kierroksiin:

| Tarkastusalue | Tuotantokoodin omistaja | Invariantti | Keskeinen regressioevidenssi |
|---|---|---|---|
| Kerrosrajat | `domain/**`, repositoryt ja service-portit | Domain ei importtaa Androidia tai ulompia kerroksia; UI/service/widget eivät käsittele Room-entityjä suoraan | `DataBoundaryContractTest`, `BillingInterfaceBindingContractTest` |
| Mittauksen start/stop | `MeasurementForegroundService`, `AudioSessionManager`, `AudioEngine` | Foreground-promootio ennen AudioRecord-startia; failure sulkee osittaiset resurssit; normaali completion ja silent recovery ovat eri eventtipolkuja | `MeasurementForegroundServicePolicyTest`, `AudioSessionManagerAudioStartTest` |
| Mittaus vs tietokantahuolto | `MeasurementDatabaseGate`, `AudioSessionManager`, `LocalBackupManager` | Mittaus ja backup/restore eivät voi olla päällekkäin; kaikki start-, failure- ja completion-polut vapauttavat omistajakohtaisen permitin | `LocalBackupManagerTest`, `AudioSessionManagerAudioStartTest` |
| Session atomisuus | `SessionRepository`, `SessionDao`, `MeasurementDao` | Pending measurementit ja runtime-summary/completion kirjoitetaan samassa Room-transactionissa; yksi aktiivinen session slot | `SessionRepositoryTransactionContractTest`, `RoomSchemaContractTest` |
| Historiallinen aika | `SessionTimeZoneOffsets`, `SessionRepository`, `ReportTextFormatter`, export-adapterit | Uusi session tallentaa alku-/loppuoffsetit; legacy/recovery ei keksi tuntematonta offsetia; kone-CSV pysyy UTC ISO instant -muodossa | `SessionTimeZoneSchemaContractTest`, `SessionTimeZoneOffsetsTest`, `CsvExportFormatterTest`, `ReportTextFormatterTest` |
| Pro-oikeus | `ProEntitlementPolicy`, `ProRouteAccessGate`, feature-ViewModelit ja servicet | Nullable startup-entitlement ei saa välähtää Free- tai Pro-sisältönä; route-gate ei korvaa execution/data-gatea | `ProEntitlementPolicyTest`, `ProRouteAccessViewModelTest`, featurekohtaiset ViewModel/service-testit |
| Raporttilaskenta | `SessionReportCalculator`, `DosimeterCalculator`, `DecibelMath` | UI/PDF/PNG/Health Connect lukevat saman report-mallin; A-painotukseen sidottuja TWA/dose/eventtejä ei lasketa muille painotuksille | `SessionReportCalculatorTest`, `DosimeterCalculatorTest`, `ExportPdfReportUseCaseTest`, `ShareResultsGeneratorTest` |
| Sound detection | `YamnetAudioWindowAdapter`, `SoundDetectionWindowFanout`, `MediaPipeSoundClassifier`, `AudioSessionManager` | 44.1 kHz -> 16 kHz anti-alias-resampling; inference vain effective Pro+toggle-tilassa; persistointi vain opt-in aggregate label-change -eventteinä | `YamnetAudioWindowAdapterTest`, `MediaPipeSoundClassifierTest`, `SoundDetectionRepositoryTest`, `AudioEngineRuntimePreferenceTest` |
| Tiedostot ja jako | `ExportFileCache`, `ShareResultsGenerator`, `ExportPdfReportUseCase`, `WavRecordingFileStore` | Exportit ovat rajatuissa cache/app-private-rooteissa; intentissä sekä stream että ClipData read grant; WAV ei siirry MediaStoreen | `PrivacyConfigTest`, `ShareResultsGeneratorTest`, `ExportPdfReportUseCaseTest`, `WavRecordingFileStoreTest` |
| Health Connect | `HealthConnectService`, `HealthConnectManager`, `HealthConnectModels` | Melu on actively recorded other-workout; kuulotesti no-op; syke on read-only overlay; session zone offsetit mapitetaan vain kun tunnetaan | `HealthConnectManagerTest`, `HealthConnectNoiseDosePayloadTest`, `HealthConnectHeartRateMapperTest` |
| Ilmoitukset ja hälytykset | `NoiseAlertEvaluator`, `NoiseNotificationSchedule`, `NotificationHelper`, audible/TTS-controllerit | Schedule ennen evaluointia; dedup alert-tyypeittäin/sessionittain; epäonnistunut toimitus retry-cooldownilla; TTS/audible ovat erillisiä opt-in-polkuja | `NoiseAlertEvaluatorTest`, `NoiseNotificationScheduleTest`, `NotificationPrivacyPolicyTest`, controller-testit |
| Release/native | Gradle catalog/lockit, verification metadata, release workflow | MediaPipe korvaa legacy Task Audio -runtimen; 16 KB native alignment tarkistetaan artifacteista; non-PR release vaatii kaikki signing-secretit | `NativeLibraryCompatibilityTest`, `ReleaseSigningQaTest`, `PermissionDeviceQaMatrixTest` |

- Foreground service: kutsutaanko `startForeground()` ennen AudioRecord-session
  aloitusta, ja kasitellaanko Android 14+ microphone/while-in-use-rajoitus
  oikein?
- Pro gates: onko gate UI:n lisaksi execution/data-polussa? Erityisesti
  hearing test, hearing recovery, CSV, PDF, metadata, Pro-audioasetukset,
  sound detection, WAV, ambient playback, tinnitus pitch, voice baseline/TTS ja
  history direct-open.
- Audio math: erotetaanko raw RMS, weighted RMS ja C-painotettu LCpeak?
  Eivatko raportit kayta raw RMS:aa LCpeak- tai A-weighted event -laskentaan?
- Refresh rate: vaikuttaako muutos vain UI-paivitykseen, ei AudioRecordiin,
  filter-stateen tai Room-persistointiin?
- Room consistency: kirjoitetaanko measurement-rivit ja session summary samassa
  transactionissa completion/flush-polussa?
- Recovery: suljetaanko edellisen prosessin aktiivinen sessio hiljaisesti
  ilman valheellista completion-navigointia?
- File sharing: jaetaanko PNG/PDF/CSV/Camera-exportit vain `cache/exports/`
  FileProvider-URIlla, ja annetaanko lukuoikeus seka `EXTRA_STREAM`in etta
  `ClipData`n kautta? WAV-jakoon saa kayttaa vain app-private
  `files/wav_recordings/` FileProvider-rootia.
- Backup/restore: validoidaanko backup ennen korvausta, tehdaanko safety backup
  ja poistetaanko WAL/SHM-sidecarit? Sulkeeko snapshot transaction kirjoittajat
  checkpointin ja kopion välisestä race-ikkunasta, ja epäonnistuuko se
  hallitusti, jos WAL aktivoituu toistuvasti?
- Historiallinen aika: säilyvätkö session alku- ja loppuoffsetit nullable-
  metadata-na läpi entity -> domain -> report -> PDF/PNG/Health Connect -ketjun?
  Pysyvätkö CSV-aikaleimat aina ISO_INSTANT UTC -muodossa riippumatta localesta?
- Route gates: renderöidäänkö non-top-level Pro-sisältö vasta varmistetussa
  `true`-tilassa ja säilyvätkö featurekohtaiset execution-gatet route-suojan
  lisäksi?
- Health Connect: pysyyko noise sync `ExerciseSessionRecord`-mallissa ja
  hearing test no-opina, ellei Android tarjoa oikeaa datatyyppia?
- Passive monitoring: pysyyko polku käyttäjän käynnistämänä foreground sample
  -toimintona, joka tallentaa vain aggregate-arvot eikä luo sessioita,
  measurements-riveja, raw-audiota tai taustatriggereita?
- Ambient playback: pysyyko se erillisessä `mediaPlayback`-servicessä ilman
  mikrofonilupaa, Room-dataa, terapia-/health-väitteitä tai automaattisia
  triggereitä?
- Voice/TTS: vaatiiko voice baseline aktiivisen Pro + Sound Detection
  -mittauksen, triggeröityykö TTS vain dosimeter dose/projected-dose
  -riskistä, ja pysyvätkö hearing baseline / sound detection -guardit mukana?
- Hearing recovery: käytetäänkö latest full hearing-test baselinea, rajataanko
  taajuudet 1/4/8 kHz:iin ja tallennetaanko vain aggregate-shiftit
  `hearing_recovery_results`-tauluun?
- Tinnitus pitch: pysyykö scope personal tracking -profiilina ilman
  diagnoosi-, terapia-, oireiden vähentämis-, background playback-,
  Health Connect- tai automaattitriggeriväitteitä?
- Localization: jos uusi UI-teksti lisätään, päivittyvätkö default
  `values/strings.xml` ja tarkoituksella rajattu `values-fi`-baseline tai
  dokumentoidaanko, miksi fi-teksti ei kuulu nykyiseen launch-baselineen?
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
- Camera Overlay -reitti, permission UI, CameraX preview/ImageCapture/VideoCapture
  binding, live dB readout, photo share burned-in overlay ja silent video capture
  ovat paikallaan. Live readout lukee `AudioEngine.decibelFlow`sta vain aktiivisen
  mittauksen aikana eika ohjaa mittaussession kaynnistysta tai pysaytysta. Photo
  share kirjoittaa valiaikaisen raw JPG:n export-cacheen, polttaa readoutin
  jaettavaan PNG:hen ja julkaisee sen FileProviderin `content://`-URIlla. Silent
  video kirjoittaa MP4:n export-cacheen ilman CameraX `withAudioEnabled()`-polkua;
  Compose-overlayn burned-in-renderointi videoon vaatii erillisen renderöinti- tai
  post-processing-polun.
- WAV-raakaaudion tallennusta varten on Pro-gatettu Settings-oletus
  `wav_recording_default`, joka on default OFF ja näyttää privacy-warningin.
  `AudioSessionManager` kaynnistaa streamaavan PCM16 WAV -writerin vain, kun
  effective-ehto `isProUser && wavRecordingDefaultEnabled` toteutuu. WAV:t
  kirjoitetaan app-private `filesDir/wav_recordings` -hakemistoon, normaali stop
  paivittaa RIFF/data-headerit ja failure/cleanup poistaa partial-tiedoston.
  Session Detail näyttää WAV-kortin, jos avattavalla sessiolla on WAV-tiedosto.
  Pro-käyttäjän share muodostaa FileProviderin `content://`-URIin perustuvan
  `audio/wav` Sharesheet-intentin `ClipData`lla ja väliaikaisella read grantilla;
  delete poistaa session WAV-tiedoston app-private storage -polusta. WAV-tiedostoa
  ei kopioida MediaStoreen. Manual share smoke ajettiin `Pixel_9_Pro`-emulaattorilla:
  Sharesheet avautui WAV-tiedostolle ja delete tyhjensi app-private
  `files/wav_recordings` -hakemiston.
- Session location on approximate-only foreground -metadataa: ei precise
  locationia, ei background locationia, ei jatkuvaa seurantaa, eikä sijainti saa
  rikkoa mittauksen start/stop-flow'ta. Room v6 sisältää nullable
  `sessions.locationLatitude`, `locationLongitude`, `locationAccuracyMeters` ja
  `locationCapturedAt` -sarakkeet. `AudioSessionManager` kytkee one-shot
  last-known capture -polun startiin ja stop-fallbackiin. Settingsin Data & Export
  -osion käyttäjätoiminto pyytää vain `ACCESS_COARSE_LOCATION`-runtime-luvan.
- Google Drive -backupia ei ole; nykyinen backup on paikallinen
  `filesDir/backups`-ratkaisu.
- `androidTest`-instrumentaatiotesteja ei ole nykyisessa checkoutissa.
- Screenshot-testit ovat olemassa, mutta ne eivat korvaa laitetason
  navigation/permission/share/billing-testausta.
- Default-English-tekstit on laajasti resursoitu, ja Osa94 lisasi ensimmaisen
  rajatun Finnish launch -baselinen `values-fi/strings.xml`-tiedostoon. Koko
  sovelluksen lokalisointi, Play-copyt ja kaikki maat/kielet eivät ole valmiita.
- Osa93 teki kriittisille uusille pinnoille source-/preview-tason accessibility-
  auditin ja guardit, mutta täysi manuaalinen TalkBack- ja laitetason sign-off
  pitää tehdä erikseen ennen releasea.
- Qodana workflow on `continue-on-error`, vaikka AGP 9.2.1 -PR:ssa saatiin CI-pass.
  CI-status tekee ei-blokkaavan tilan nakyvaksi nimella
  `Qodana Analysis (non-blocking AGP 9.2 risk)` ja workflow summarylla.
- Release signing on konfiguroitu, mutta Play Store -julkaisua varten
  tarvittavat salaisuudet, tuoteasetukset, policy-tekstit ja laitetason
  regressioverifiointi tulee tarkistaa erikseen.
- Osa95-98 QA-dokumentit kirjaavat release-riskit: device smoke, Play Console
  `dbcheck_pro` -todennus, signed Play-ready AAB, Play upload ja Qodana-run ovat
  erillisiä release sign-off -todisteita, eivät paikallisen unit-testauksen
  korvikkeita.
- 2026-07-15 API 36 -emulaattorin device smoke todensi Meterin oikean
  `AudioRecord`/microphone-FGS-polun, notification deny/grant -tilat, CameraX
  preview/photo/silent-video-polut, FileProvider Sharesheet -read grantin ja
  Health Connectin installed-provider permission-flow'n. Täysi TalkBack,
  Play Billing -testiosto ja signed release AAB -asennus ovat edelleen avoimia.
  Fyysisellä API 37 -laitteella havaittu Androidin 16 KB -yhteensopivuusvaroitus
  korjattiin vaihtamalla 4 KB -kohdistettu `tensorflow-lite-task-audio:0.4.4`
  MediaPipe Tasks Audio 0.10.35 -runtimeen. Korjattu debug-APK läpäisi
  `zipalign -P 16` -tarkistuksen, kaikki ARM64 `PT_LOAD` -kohdistukset ovat
  `0x4000`, fyysinen laite käynnistyi ilman compatibility-dialogia ja
  emulaattorin YAMNet-inference latasi `libmediapipe_tasks_jni.so`:n sekä mallin.
- Osa99 final reports pass oli vihreä failure-tasolla: `ktlintCheck`, `detekt`
  ja Android lint olivat `BUILD SUCCESSFUL`, Android lintissa oli 0 erroria ja
  36 ei-blokkaavaa warningia, ja `sc`-raportit näyttivät 0 dependency-, OSV-,
  Semgrep-, Gitleaks- ja TruffleHog-löydöstä.

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
