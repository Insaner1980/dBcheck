<claude-mem-context>
# Memory Context

# [dBcheck] recent context, 2026-05-21 2:49pm GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (23,295t read) | 1,895,358t work | 99% savings

### May 8, 2026
5277 4:42p 🟣 B-weighting frequency filter fully implemented
S768 Clarifying question about cross-project impact of adding ktlint_code_style to .editorconfig (May 8, 4:43 PM)
5281 4:51p 🔵 Lint check reveals 2039 code quality violations blocking build
5282 4:52p 🔵 2009 of 2039 lint violations are indentation errors across 20+ files
5284 " 🔵 Detekt baseline exists but excludes all 2009 indentation violations
5285 " 🔵 EditorConfig lacks indentation settings causing 2009 formatting violations
S770 Resolve 2039 lint violations blocking commit of Block 12.3 B-weighting implementation (May 8, 4:52 PM)
S767 Diagnose lint-check failures blocking commit of Block 12.3 B-weighting implementation (May 8, 4:52 PM)
S772 Implemented ktlint_code_style configuration fix to resolve lint build failures (May 8, 4:53 PM)
S769 Confirm EditorConfig project isolation before applying ktlint_code_style fix (May 8, 4:58 PM)
S771 Applied ktlint_code_style configuration fix to resolve 2009 indentation violations (May 8, 4:58 PM)
S775 Verified lint-check still failing after corrected ktlint_code_style configuration (May 8, 5:00 PM)
5287 5:01p 🔴 Corrected ktlint_code_style value from android_studio to android
S774 Corrected ktlint_code_style configuration value to valid android identifier (May 8, 5:01 PM)
S773 Applied ktlint configuration fix and investigating why violations persist in old report (May 8, 5:01 PM)
5288 5:08p ⚖️ Disabled ktlint formatting rules in detekt config after EditorConfig approach failed
5289 5:09p 🔵 Detekt config fix successfully reduced violations from 2039 to 9
5290 5:10p 🔴 Fixed MaxLineLength violation in HistoryScreen hourOfDay function
5291 " 🔴 Fixed 2 MaxLineLength violations in MeasurementRepository
S776 Resolved 2031 lint violations and deciding how to handle remaining 8 pre-existing issues (May 8, 5:11 PM)
5292 5:12p 🔴 Fixed MaxLineLength violation in ExportCsvUseCase CSV header
5293 5:13p 🔴 Fixed MaxLineLength violation in NotificationHelper exposure alert
5294 " 🔴 Fixed MaxLineLength violation in HearingHealthCard when expression
5304 " 🔴 Fixed ReturnCount violation in DecibelCalculator.calculateDb
5305 10:50p 🔴 Fixed final ReturnCount violation in AudioEngine.createAudioRecord
### May 9, 2026
5314 2:39a 🔵 Pro Purchase UI Integration Current Architecture
5315 " ⚖️ Debug Override Mechanism for Pro Purchase Testing
5316 2:42a 🔵 Pro Feature Gating Pattern in Notification Service
5317 3:08a 🔵 Pro Purchase UI Integration Gap Identified
### May 12, 2026
5335 10:54a 🔵 SonarQube scan identified 64 open code quality issues
5336 " 🔵 dBcheck Android project configured with SonarQube integration
5337 " 🔵 SonarQube analysis reveals code quality baseline with 64 issues across dBcheck Android app
5338 " 🔵 SonarQube Scan Identified 64 Open Issues
5339 " 🔵 SonarQube Scan Identified 64 Open Code Quality Issues
5340 10:55a 🔵 SonarQube scan identified 64 open issues in codebase
5341 11:13a 🔵 SonarQube Scan Identified 64 Open Issues
5342 11:47a 🔵 SonarQube Scan Identified 64 Open Issues
5343 11:48a 🔄 Refactored Coroutine Dispatchers to Use Hilt Dependency Injection
5345 2:50p 🔴 Privacy hardening implemented with TDD regression tests
5346 " 🔵 SharedFlow migration broke existing share intent tests
### May 16, 2026
5354 5:12p ✅ Pushed dBcheck security hardening and UI optimization changes to GitHub
### May 17, 2026
5372 10:48a 🔵 Navigation Architecture Review Completed
5393 1:16p 🟣 Published adaptive UI fixes to GitHub with updated .gitignore
### May 18, 2026
5402 12:28a 🔐 dBcheck Deepsec scan shows zero active security findings after remediation
5405 " 🔵 Deepsec candidates are expected security patterns already reviewed and resolved
5406 " 🔴 Restored atomic database backup and restore operations with SQLite validation
5420 2:45p 🔵 dBcheck Android lint failure rooted in Gradle dependency verification
5421 " 🔴 Android lint task failure caused by missing Gradle dependency verification entries, not lint violations
5427 3:17p 🔵 dBcheck Android app has minimal localization infrastructure with 200+ hardcoded UI strings
5428 3:21p 🔵 dBcheck app lacks localization infrastructure
5429 " 🔵 Localization gap quantified: 202 hardcoded string assignments across 41 files
### May 19, 2026
5438 2:25p 🔄 Fixed Kotlin lint violations across History and Settings UI components
5445 2:27p 🔴 Fixed All Lint Check Failures Across ktlint, detekt, and Android Lint
5450 3:20p 🔵 Lock-screen Meter Custom Notification Implementation Review
5452 " 🔵 Lock-screen meter custom notification implementation verified
**5458** 7:43p 🔵 **File and Backup Security Architecture Mapped**
Comprehensive code search mapped all file handling, backup, export, and sharing mechanisms in the dBcheck app. FileProvider scope is limited to cache/exports/ directory, preventing unintended exposure of internal app data. Local backups reside in private filesDir/backups/ managed by LocalBackupManager. CSV and PNG exports temporarily expose files via FileProvider with explicit read-only grants. PDF exports use system document picker (CreateDocument contract) allowing user-controlled save locations. Android's automatic backup system is disabled to prevent cloud/device-transfer data leakage. The restore flow includes validation before and after operations, safety backups, and WAL/SHM cleanup to maintain database integrity.
~427t 🔍 16,669

**5459** 7:44p 🔵 **Backup Validation and Export Cleanup Mechanisms Confirmed**
Direct code inspection confirmed the security implementation details for backup validation and export file handling. BackupDatabaseValidator uses multi-layered SQLite checks including structure validation, version compatibility, and Room schema hash verification against a known-good whitelist. The ExportFileCache utility automatically purges stale files older than 24 hours from the exports cache directory, preventing indefinite accumulation of sensitive data. The LocalBackupManager employs defensive file operations with Mutex serialization, fsync for crash safety, atomic moves, and canonical path validation to prevent directory traversal. FileProvider scope is strictly limited to the cache/exports/ subdirectory per file_paths.xml configuration. System backup is disabled at multiple layers: manifest attribute, backup rules, and data extraction rules all block automatic cloud and device-transfer backup.
~559t 🔍 38,213

### May 21, 2026
**5492** 10:08a 🔄 **dBcheck error handling and failure recovery hardening**
This refactor hardens dBcheck against crashes from external service failures and persistence errors. BillingManager isolates Play Billing coroutine failures with SupervisorJob and wraps refresh, launch, and callback operations in runCatching to log errors without crashing. AudioSessionManager catches persistence failures during active measurement collection, stops recording, emits PersistenceFailed, and preserves pending measurements for retry on next stopSession. MeasurementForegroundService reports foreground start failures as AudioRecordingFailure.StartFailed so they become observable errors instead of silent failures. SessionDetailViewModel catches Health Connect read failures and shows an unavailable message instead of crashing. DbCheckApplication wraps startup tasks to prevent widget update or session recovery failures from blocking app launch. HearingTestThresholdCodec skips malformed entries instead of throwing on corrupted data. Several helpers were extracted for testability: BillingFlowParamsFactory, widgetContentMode, MeterNotificationPermissionPolicy. UI components were refactored for readability and maintainability. New tests cover billing failures, audio persistence failures, widget failures, Health Connect failures, and malformed hearing test data. The work follows the existing error-handling patterns from recent AudioSessionManager and HealthConnectManager fixes but extends them across billing, foreground services, widgets, and startup code.
~935t 🛠️ 102,670

**5493** 10:11a 🔵 **AudioSessionManager error handling has comprehensive test coverage**
Code inspection revealed AudioSessionManager has three distinct failure channels (recording, persistence, Health Connect sync) that are already comprehensively tested. The test suite includes specific scenarios for database unavailability with retry logic, active measurement flush failures that stop recording, and Health Connect write failures that don't block session completion. Test infrastructure uses configurable mock objects like TestBillingGateway with injectable failures. Permission policy tests confirm Android 13+ notification permission handling. This baseline coverage assessment informs which additional test gaps need addressing.
~410t 🔍 10,229

**5494** 10:13a ✅ **Test infrastructure enhanced with Robolectric and coverage adjusted**
Build configuration modified to support Android component testing with Robolectric and androidx-test-core. Robolectric 4.16.1 enables JVM-based Android framework testing without emulators, while androidx-test-core 1.6.1 provides Android test APIs. Jacoco coverage configuration changed to include test classes in reports by removing **/*Test*.* exclusion pattern. Security scanning configuration updated to suppress false-positive CVE match where compose-stability-runtime-android was incorrectly flagged as GitHub Enterprise Server (cpe:/a:github:github). These changes prepare the test infrastructure for writing Android-specific unit tests that require framework APIs.
~355t 🛠️ 19,488


Access 1895k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>

## Project Architecture Notes

### 2026-05-09 - Meter- ja kuulotestitulosten share-dataflow

- `ShareResultsGenerator` on jakosisaltojen keskitetty lahde. Meterin share rakentaa `text/plain`-Sharesheet-intentin
  nykyisista mittausarvoista, ja Hearing Test Results rakentaa PNG-kortin seka saatetekstin samaan `ACTION_SEND`
  -jakopolkuun.
- PNG-jaot kayttavat yhteista helperia, joka kirjoittaa kuvan cacheen, julkaisee sen FileProviderin `content://`-URIlla
  ja antaa vastaanottajalle valiaikaisen lukuoikeuden `ClipData` + `FLAG_GRANT_READ_URI_PERMISSION` -asetuksilla.
- `MeterViewModel.createShareIntent()` on Meterin share-portti. Se jakaa vain, kun sessiosta on vahintaan yksi sample,
  ja nayttaa UI-virheen, jos mittausta ei viela ole tai Sharesheetia ei voida avata.
- `ResultsViewModel.createShareIntent()` on kuulotestitulosten share-portti. Results-ruutu kaynnistaa Android
  Sharesheetin itse, joten navigation layerissa ei ole enaa tyhjaa `onShare`-callbackia. Results ViewModel lukee
  kuulotestin `testId`-reittiargumentin `SavedStateHandle`sta ja hakee tuloksen `HearingTestRepository.getResultById(...)`
  -polulla; `getLatestResult()` on vain fallback, jos reittiargumentti puuttuu.
- `ActiveTestViewModel` julkaisee kuulotestin valmistumisnavigoinnin vasta, kun `HearingTestService.saveCompletedTest(...)`
  on palauttanut tallennetun tuloksen ID:n. `HearingTestActiveScreen` navigoi `hearing_test/results/{testId}`-reitille
  `ActiveTestState.completedTestId`-arvon perusteella.
- Analyticsin ja Historyn tyhjatilan CTA:t navigoivat Meteriin. Analyticsin ja Historyn ylapalkin actionit navigoivat
  Settingsiin; Settingsissa ei nayteta action-ikonia ilman toimintoa.
- `DurationFormatter.formatClockDuration(...)` on kellomuotoisen keston yhteinen helper Meter sharelle, lockscreen
  notificationille, PDF-raportille ja Session Detailille.

### 2026-05-09 - Pro-oston UI-kytkentä ja entitlement-flow

- `billing/BillingGateway.kt` on Settingsin ostovirran testattava rajapinta. `BillingManager` toteuttaa sen, kysyy
  `ProductDetails`-datan juuri ennen ostovirran käynnistystä ja palauttaa `PurchaseLaunchResult`-tuloksen.
- Billingin käynnistystila on kolmivaiheinen: `BillingManager.isPurchased` alkaa `null`-arvosta, joka tarkoittaa
  "ei vielä varmistettu". `ProFeatureManager` synkkaa DataStoreen vain varmistetun `true`/`false`-tilan, jotta appin
  käynnistys tai epäonnistunut Play Billing -kysely ei ylikirjoita aiemmin tallennettua Pro-oikeutta Free-tilaan.
- `BillingManager.purchaseEvents` välittää ostotapahtumat Settingsiin (`Completed`, `Cancelled`, `AlreadyOwned`,
  `Failed`). Ostos kuitataan Google Playlle vasta `PurchaseState.PURCHASED`-tilassa.
- `domain/entitlement/ProEntitlementPolicy.kt` on Pro-oikeuden ainoa policy-lähde: release käyttää ostotilaa, debug on Pro
  oletuksena, ja debug-only `debugForceFreeEnabled` pakottaa Free-tilan Pro-gatejen testausta varten.
- `UserPreferences.isProUser` on effective entitlement. Ostotila ja debug force-free tallennetaan DataStoreen erillisinä
  arvoina.
- `SettingsScreen` käynnistää ostovirran Settingsin Pro-kortista ja Settingsissä näkyvistä ProLockOverlay-painikkeista.
  Muiden näyttöjen Upgrade-polku navigoi edelleen Settingsin Pro-korttiin.
- `DbCheckApplication` injektoi `ProFeatureManager`in, jotta billing-tilan synkkaus DataStoreen alustuu sovelluksen
  käynnistyksessä. Sama entitlement-flow päivittää Glance-widgetit, kun Pro-oikeus muuttuu.

### 2026-05-10 - Startup-initialisoinnin siivous

- `MainActivity` odottaa ensimmäistä `UserPreferences`-emissiota ennen `DbCheckTheme`/`DbCheckNavHost`-sisällön
  piirtämistä. `StartupThemeState` erottaa odotustilan ratkaistusta dark/light-teemasta, jotta tallennettu teema ei
  välähdä system-teemanä ensimmäisessä framessa.
- Meter on edelleen navigation graphin start destination, mutta käynnistyksen lupapolitiikka pyytää vain puuttuvan
  mikrofoniluvan. Android 13+ `POST_NOTIFICATIONS` -lupa pyydetään vasta käyttäjän käynnistäessä mittauksen, koska
  foreground service voi käynnistyä ilman lupaa ja notification-prompt on parempi sitoa käyttäjätoimintoon.
- Startup-korjauksille on regressiotestit: `ProFeatureManagerStartupTest`, `MainActivityThemeTest` ja
  `MeterStartupPermissionPolicyTest`.

### 2026-05-09 - Phase 12.1 Health Connect -integraatio

- Health Connect -integraation infrastruktuuri on `sync/HealthConnectManager.kt`. UI:n sisaanmeno on
  `service/HealthConnectService.kt`, joka mapittaa statuksen, permissionit ja sykearvot service-malleiksi ennen
  Settings- ja Session Detail -ViewModeleita.
- Health Connectissa ei ole natiivia melualtistus- tai audiometriadatarecordia. dBcheck mallintaa melualtistuksen
  `EXERCISE_TYPE_OTHER_WORKOUT`-sessioksi, jonka `Metadata.clientRecordId` on `noise_dose_<date>_session_<id>`.
  Metadata on `activelyRecorded`, koska mittaus kaynnistyy kayttajan toiminnolla. Notes-kentassa ovat LAeq, max, peak
  ja kayttajalle naytettava weighting-label. Kuulotestin Health Connect -kirjoitus on tietoisesti no-op kunnes Android
  tarjoaa tuetun audiometriatyypin tai erikseen suunnitellun FHIR-polun.
- `SettingsScreen` nayttaa `HealthSyncSection`-osion. Free-kayttaja voi sallia melusession Health Connect -synkkauksen;
  Pro-kayttajalle on erillinen heart rate overlay -asetus, joka pyytaa vain `READ_HEART_RATE`-permissionin.
- Health Connectin manifest-entrypointit (`HealthConnectPermissionsRationaleActivity` ja
  `HealthConnectPermissionUsageActivity`) ovat exportattuja vain Health Connectin privacy/disclosure-polkuja varten.
  Molemmat aliakset targetoivat `HealthConnectPermissionDisclosureActivity`a, joka nayttaa staattisen disclosure-nakyman
  eika kayta MainActivityn navigaatiota, billingia, Settings-toimintoja tai dataa muuttavia polkuja.
- `AudioSessionManager.stopSession()` paivittaa valmiin session `frequencyWeighting`-arvon ja kutsuu
  `HealthConnectManager.writeNoiseDose(...)`, jos `healthConnectEnabled` on paalla. Ennen Health Connect -kirjoitusta
  se rakentaa `SessionReportCalculator`illa raportin flushatuista mittausriveista, joten synkkausnotesiin kirjattava
  LAeq tulee samasta laskennasta kuin PDF/PNG/Session Detail. Synkkaus ei blokkaa session valmistumisen navigointivirtaa;
  `Failed`-tulos emittoidaan `healthConnectSyncFailures`-virtaan, jota Meter UI nayttaa virheviestina.
- Session Detail lukee sykearvot `HealthConnectService.readHeartRateForSession(...)`-funktiolla, kun kayttaja on Pro,
  heart rate overlay on paalla ja Health Connect -permissionit on myonnetty. Piirto tapahtuu
  `ui/analytics/components/HeartRateOverlay.kt`-komponentilla.

### 2026-05-09 - Phase 12.4 Session Detail + PDF-raportti

- Session Detail sijaitsee reitissä `history/detail/{sessionId}`. Historyn sessiokortit navigoivat sinne, ja
  `MeterViewModel` kuuntelee `AudioSessionManager.completedSessionIds`-virtaa, jotta valmis mittaus avaa saman
  detail-nakyman.
- Tieteellisen raportin mittareilla on yksi lahde: `domain/report/SessionReportCalculator.kt`, joka rakentaa
  `SessionReportData`-mallin `domain/session/Session`- ja `domain/report/ReportMeasurement`-datasta. PDF, PNG-jako,
  Health Connect -notes ja UI lukevat samasta mallista eivatka laske LAeq-, TWA-, dose- tai peak event -arvoja uudelleen.
- NIOSH 8h TWA, NIOSH dose ja "85 dBA" peak event -lista ovat saatavilla vain A-painotetulle sessiolle.
  `SessionReportData.aWeightedExposureMetricsAvailable` kertoo saatavuuden; muilla painotuksilla TWA/dose ovat `null`,
  peak event -lista on tyhja, ja Session Detail/PDF/PNG nayttavat arvon puuttuvana eivatka laskettuna nollana.
- LAeq-energia-average on `domain/noise/DecibelMath.energyAverageDb(...)`-helperissa, jota raportti- ja analytiikkalaskenta
  kayttavat.
- PDF-export on `util/ExportPdfReportUseCase.kt`-luokassa. Compose kaynnistaa
  `ActivityResultContracts.CreateDocument("application/pdf")`-polun, jonka palauttamaan `Uri`:in use case kirjoittaa
  nelisivuisen natiivin `PdfDocument`-raportin tai viisivuisen raportin, jos Session Detailin Health Connect
  -sykeoverlay on kaytossa. `ReportHeartRateSection` valittaa PDF:lle samat sykepisteet, jotka UI nayttaa overlayssa.
- Kaavion koordinaattimuunnos on keskitetty `util/PdfChartRenderer.kt`-tiedostoon; samaa muunnosta kayttavat PDF Canvas
  -renderointi ja Session Detailin staattinen Compose-kaavio.
- `ShareResultsGenerator` vastaa nyt myos Session Detailin PNG-jakokortin generoinnista aiempien teksti- ja
  hearing-test-jakopolkujen lisaksi.
- Health Connect -sykekayra on kytketty Phase 12.1:ssa Session Detailin time-series-osioon.

### 2026-05-09 - Session metadata, tagit ja export-dataflow

- Session metadata normalisoidaan keskitetysti `domain/session/SessionMetadata.kt`-tiedostossa. Sama helper trimmaa nimen,
  normalisoi emojin, rajaa tagit kuuteen 24 merkin tagiin, poistaa duplicate-tagit case-insensitive ja muodostaa
  export-tiedostonimien slug-arvon.
- `SessionDao.updateSessionMetadata(...)` on nimen, emojin ja tagien tallennuspolku. `AudioSessionManager.stopSession()`
  kayttaa `SessionDao.completeSession(...)`-partial updatea, jotta session lopetus ei ylikirjoita jo tallennettua
  metadataa koko `SessionEntity`-oliolla.
- Session nimeaminen ja tagitus on Pro-gatettu Historyssa ja Session Detailissa. Free-kayttajan edit/lock-toiminto
  ohjaa samaan Settingsin Pro-ostovirtaan kuin muut ProLockOverlay-kohdat.
- `SessionReportData` sisaltaa session display-nimen lisaksi custom-nimen, emojin ja tagit. Detail UI, PDF ja PNG-jako
  lukevat ne samasta raporttimallista.
- CSV-export muodostaa kaksi FileProviderin kautta jaettavaa tiedostoa: sessioyhteenvedon ja mittausrivit. Molemmissa
  on `session_name`, `session_emoji` ja `session_tags`, ja CSV-field escaping on keskitetty
  `data/export/CsvEscaper`-helperiin.
- `ExportCsvUseCase` kirjoittaa CSV:t streamina cache-tiedostoihin ja hakee mittausrivit per sessio sivuina
  `MeasurementDao.getMeasurementsForSessionExportPage(...)`-polulla, jotta export ei rakenna koko raw measurement
  -aineistoa muistiin eikä käytä yhtä isoa `IN (:sessionIds)` -kyselyä.
- Settingsin `DataExportSection` kytkee CSV-viennin UI:hin. Free-käyttäjä saa ProLockOverlay-previewn, Pro-käyttäjän
  `Export CSV` -painike kutsuu `SettingsViewModel.createCsvExportIntent()`-polkua ja avaa Android Sharesheetin.

### 2026-05-09 - Paikallinen backup UI ja restore-flow

- `sync/BackupGateway.kt` on backup-infrastruktuurin testattava rajapinta. `service/BackupService.kt` on Settingsin
  UI-facing backup-portti, ja `LocalBackupManager` toteuttaa varsinaiset paikalliset `filesDir/backups`
  -tietokantakopiot; Google Drive- tai muu pilvisynkka ei kuulu nykyiseen flow'hun.
- Backupin luonti tekee Roomille WAL checkpointin ennen `dbcheck.db`-tiedoston kopiointia, mutta ei sulje
  `DbCheckDatabase`-singletonia. Restore sulkee tietokannan vasta, kun valittu backup on validoitu ja nykytilasta on tehty
  `dbcheck_pre_restore_*`-turvakopio.
- Restore poistaa vanhat `dbcheck.db-wal`- ja `dbcheck.db-shm`-sivut ennen korvaavaa kopiointia. Onnistuneen restoren
  jälkeen `SettingsEvent.RestartAfterRestore` käynnistää sovelluksen uudelleen, koska suljettua Room-instanssia ei voi
  käyttää turvallisesti samassa prosessissa.
- `SettingsViewModel` estää backup- ja restore-toiminnot aktiivisen mittauksen aikana ja näyttää saman inline-viestipolun
  kautta onnistumiset ja virheet kuin CSV-vienti.
- `DataExportSection` näyttää Free-käyttäjillekin Local backups -kortin CSV-viennin rinnalla. CSV pysyy Pro-gatettuna,
  mutta paikallinen backup/restore on kaikkien käyttäjien dataturvatoiminto.

### 2026-05-09 - Live-spektrianalyysin dataflow

- `AudioProcessingConfig` keskittää audio-domainin 44.1 kHz sample raten, 4096 sample chunkin ja 4096 point FFT-koon.
- `SpectralAnalyzer` on `FFTProcessor`in päälle rakennettu live-spektrin domain-muunnin. Se tuottaa 24 logaritmista
  20 Hz-20 kHz bandia, dominanttitaajuuden ja bandwidth-luokan raw PCM16 -chunkista.
- `AudioEngine.spectralFrame` julkaisee vain live-only-spektritilan. Spektri lasketaan raw, unweighted mikrofonidatasta
  ja tyhjennetään stopissa tai kun Pro-oikeus poistuu.
- `service/AudioSessionManager` ohjaa `AudioEngine.setSpectralAnalysisEnabled(...)`-asetusta
  `UserPreferences.isProUser`-arvolla.
  Meter käynnistää edelleen mittauksen normaalin session manager -polun kautta; Analytics vain lukee live-tilaa.
- `AnalyticsViewModel` yhdistää historiakeskiarvot, Pro-oikeuden, recording-tilan ja `spectralFrame`-virran.
  Ensimmäisen mittauksen aikana Analytics voi näyttää spektrikortin, vaikka Roomissa ei vielä ole historiadataa.
- `SpectralAnalysisCard` käyttää ViewModelilta tulevaa `SpectralAnalysisUiState`-tilaa. Free-käyttäjän kortti saa vain
  staattisen locked-previewn; live-spektriä ei välitetä overlayn alle.
- `measurements.frequencyData` on edelleen käyttämätön eikä spektridataa persistöidä tässä vaiheessa.

### 2026-05-09 - Waveform style ja refresh rate -dataflow

- `UserPreferenceDefaults` keskittaa preferenssien default-arvot DataStorelle, `UserPreferences`-mallille,
  Settings UI-statelle ja uuden session default-painotukselle.
- `WaveformStyle` ja `MeterRefreshRate` ovat typed preference -enumit `data/local/preferences/model`-paketissa.
  DataStore tallentaa edelleen string-arvot (`default`/`filled`/`bars` ja `high`/`standard`/`low`), mutta UI ja domain
  lukevat typed-arvoja fallbackeilla.
- Settingsin `DisplayAppearanceSection` kytkee Free-käyttäjille waveform-tyylit Line, Filled ja Bars sekä refresh rate
  -valinnat High, Standard ja Low power. Refresh rate vaikuttaa vain ruutupäivityksiin, ei mikrofonin sample rateen tai
  mittausrivien tallennuscadenceen.
- `MeterViewModel` lukee `PreferencesRepository.userPreferences`-virtaa ja throttlettaa vain Meterin UI-päivityksiä
  `MeterRefreshRate.uiIntervalMs`-arvolla. Jokainen raw dB -lukema käsitellään edelleen haptiikkaa ja safety-signaaleja
  varten.
- `service/AudioSessionManager` pitää `SessionStats`-tilastot kaikkien raw-lukemien perusteella, mutta
  `service/MeasurementPersistenceSampler` harventaa Roomiin tallennettavia `MeasurementEntity`-rivejä
  kiinteällä 1s cadencella refresh rate -asetuksesta riippumatta. Ensimmäinen lukema, `NoiseLevel.ELEVATED.maxDb`
  threshold-crossing, uusi session max ja stopin viimeisin tallentamaton lukema pakottavat persistoinnin.
- `AudioSessionManager` ei sisällytä `refreshRate`-arvoa runtime-audio-preferensseihin, joten refresh-only muutos ei
  kutsu `AudioEngine.setWeighting(...)`-polkua eikä resetoi painotusfiltterin tilaa kesken session.
- `AudioRecord`-polku pysyy ennallaan: 44.1 kHz sample rate, 4096 sample chunk, painotusfiltterit ja FFT-koko eivät muutu
  refresh rate -asetuksen mukaan.

### 2026-05-09 - Environment Mix Analytics -dataflow

- `MeasurementDao.getEnvironmentMixCounts(...)` laskee viimeisen 7 päivän `measurements.dbWeighted`-sampleista Quiet,
  Moderate, Loud ja Critical -luokkien lukumäärät sekä kokonaismäärän Room-projektiona.
- `MeasurementRepository.getEnvironmentMixLast7Days()` on Environment Mixin ainoa historiadata-portti. Repository
  mapittaa DAO-projektion `domain/analytics/EnvironmentExposureMixCounts`-malliksi, ja dB-rajat tulevat
  `domain/noise/NoiseLevel`-mallista.
- `AnalyticsViewModel` yhdistää Environment Mix -countit samaan Analytics-flow'hun kuin weekly averages, Pro-oikeuden,
  recording-tilan ja live-spektrin. Pro-käyttäjän prosentit pyöristetään niin, että näkyvien rivien summa on 100.
- `EnvironmentMixCard` lukee `EnvironmentMixUiState`-tilaa. Free-käyttäjälle näytetään vain staattinen locked-preview;
  oikeita prosentteja ei välitetä blur-overlayn alle.

### 2026-05-09 - MonthlyTrendChart ja YearlyReportCard dataflow

- `domain/analytics/ExposureAnalyticsCalculator.kt` on kuukausi- ja vuosianalytiikan mittarilaskennan lähde. Se laskee
  LAeq-arvot energia-average-menetelmällä, rolling 30 päivän päiväpisteet, rolling 12 kuukauden vuosiraportin sekä
  meluvyöhykejakauman samoilla `NoiseLevel`-rajoilla kuin muu analytiikka.
- `MeasurementDao.getWeightedMeasurementsInRange(...)` palauttaa data-kerroksessa kevyen `WeightedMeasurementPoint`-
  projektion, jonka `MeasurementRepository` mapittaa `domain/analytics/WeightedExposureMeasurement`-malliksi.
- `SessionRepository.getCompletedSessionCountInRange(...)` laskee valmiit sessiot rolling-vuosiraportin Sessions-mittariin
  nykyisen `SessionDao.getSessionsInRange(...)`-polun kautta.
- `AnalyticsViewModel` gateaa kuukausi- ja vuosiflow't `UserPreferences.isProUser`-arvolla. Free-käyttäjälle UI saa vain
  `LockedPreview`-tilat eikä oikeita 30 päivän tai 12 kuukauden mittareita lasketa tai välitetä.
- `MonthlyTrendChart` lukee `MonthlyTrendUiState`-tilaa ja piirtää 30 päivän LAeq-trendin Canvasilla niin, että tyhjät
  päivät näkyvät puuttuvina pisteinä eivätkä nolla-dB-arvoina.
- `YearlyReportCard` lukee `YearlyReportUiState`-tilaa ja näyttää Sessions-, 12mo LAeq-, Loudest- sekä vyöhykejakauman
  Pro-overlayn takana.

### 2026-05-09 - Arkkitehtuurirajojen siivous

- `domain/` ei importtaa enaa `data/`, `sync/`, `service/`, `ui/`, `widget/` tai `billing/`-paketteja. Domain-mallit
  ovat `domain/session`, `domain/noise`, `domain/hearingtest`, `domain/report`, `domain/analytics`,
  `domain/audio` ja `domain/entitlement` -paketeissa.
- `NoiseLevel.fromDb(...)` lukee luokkarajat enum-arvojen `maxDb`-kentista, jotta 40/70/85 dB -rajat pysyvat yhdessä
  domain-lahteessa.
- `AudioSessionManager` ja `MeasurementPersistenceSampler` ovat sovellusorkestrointia `service/`-paketissa. Ne saavat
  kayttaa repositoryja, Health Connectia ja widget-paivitysta; `domain/audio` jaa audio primitiveihin kuten
  `AudioEngine`, `DecibelCalculator`, `FrequencyWeightingFilter`, `FFTProcessor`, `SpectralAnalyzer` ja `ToneGenerator`.
- Kuulotestin Hughson-Westlake-tila, pisteytys ja threshold-serialisointi ovat `domain/hearingtest`-paketissa.
  `ActiveTestViewModel` ohjaa vain kayttajavastetta ja tone playbackia, `HearingTestService` tallentaa tuloksen ja
  hoitaa Health Connect no-op -synkkauksen, ja `HearingTestRepository` mapittaa Room-entityn domain-malliin.
- CSV-export on `data/export`-paketissa, koska se lukee Room-entityja ja kirjoittaa FileProvider-jaettavat CSV:t.
  PDF-renderointi on `util/ExportPdfReportUseCase.kt`-tiedostossa, koska se on Android Canvas/PdfDocument
  -presentaatiota eika domain-laskentaa.
- UI-pääkoodi ei importtaa suoraan Room DAO/entity -projektioita tai `sync/`-paketin integraatiomalleja. Repositoryt
  palauttavat domain-malleja (`DailyExposureAverage`, `HourlyExposureAverage`, `WeightedExposureMeasurement`), service
  mapittaa Health Connectin ja backupin UI-facing-malleiksi, ja ViewModelit muodostavat ruutukohtaiset UI-state-mallit.

### 2026-05-12 - Sonar-korjausten dispatcher- ja Intent-event dataflow

- Coroutine dispatcherit injektoidaan Hiltin kautta `di/CoroutineDispatchers.kt`-qualifiereilla
  (`DefaultDispatcher`, `IoDispatcher`, `MainDispatcher`). `AppModule` on dispatcherien ainoa provider-lahde, ja
  audio-, billing-, sync-, export- ja service-kerrokset eivät kovakoodaa `Dispatchers.*`-arvoja suoraan.
- Share/export-polut eivät palauta julkisista ViewModel-funktioista suspend-arvoja. `MeterViewModel`,
  `ResultsViewModel`, `SessionDetailViewModel` ja `SettingsViewModel` käynnistävät työn `viewModelScope`ssa ja julkaisevat
  valmiit Android `Intent`it `SharedFlow`-eventteinä, jotka Compose-ruutu kerää ja avaa chooserilla.
- `SessionCardState` sekä `ProUpsellCardState`/`ProUpsellCardActions` pienentävät Compose-komponenttien parametripintaa
  ilman että UI-state lasketaan komponentin sisällä uudelleen.
- Gradle dependency locking on käytössä root-projektissa `allprojects { dependencyLocking { lockAllConfigurations() } }`
  -asetuksella. Resoluutio lukitaan `settings-gradle.lockfile`- ja `app/gradle.lockfile`-tiedostoihin.

### 2026-05-12 - Foreground service -mittausvastuu

- `MeasurementForegroundService` on mittaussession foreground-portti: se kutsuu `ServiceCompat.startForeground(...)`
  ensin ja käynnistää `AudioSessionManager.startSession()` -polun vasta, jos foreground-promootio onnistuu.
- `AudioSessionManager.startSession()` on suspend-käynnistysportti, joka palauttaa `true` vasta kun
  `AudioEngine.startRecording(...)` on saanut `AudioRecord.startRecording()` -kutsun läpi ja julkaissut
  `onRecordingStarted`-callbackin. Epäonnistunut AudioRecord-startti ei luo Room-sessiota eikä aseta
  `isRecording`-tilaa.
- `MeterViewModel` ei käynnistä audiosessiota suoraan `startForegroundService(...)`-paluuarvon perusteella. Se pyytää
  palvelun käyntiin ja peilaa Meterin `isRecording`-UI-tilan `AudioSessionManager.isRecording`-virrasta.
  `MeterViewModel.onCleared()` ei pysäytä mittauspalvelua; aktiivinen foreground-mittaus pysähtyy eksplisiittisestä
  stop-komennosta, palvelun tuhoutumisesta tai AudioRecord-failuresta.
- `AudioSessionManager.activeSessionStartTimeMs` julkaisee käynnissä olevan session alkuhetken Meter UI:n
  uudelleenkytkentää varten. Uusi Meter ViewModel käyttää tätä arvoa session kestoajastimeen, jotta taustalta tai
  notificationista palaava UI ei nollaa näkyvää kestoa.
- Mittauspalvelu palauttaa onnistuneesta käynnistyksestä `START_NOT_STICKY`, koska prosessin tappamisen jälkeen nykyistä
  `AudioRecord`-sessiota ei palauteta eikä vanhaa ilmoitusta saa herättää ilman aktiivista mittausta.
- `DbCheckApplication` kutsuu käynnistyksessä `AudioSessionManager.recoverInterruptedSession()`-polkua. Jos edellisen
  prosessin jäljiltä Roomissa on `isActive = 1` -sessio, manager sulkee sen hiljaisesti viimeisen persistoidun mittauksen
  aikaleimaan ja laskee summary-arvot persistoiduista `dbWeighted`-riveistä ilman auto-navigointia Session Detailiin.
- `domain/audio/AudioRecordPolicies.kt` keskittää AudioRecord-bufferin mitoituksen ja read-tulosten tulkinnan. Capture
  buffer on aina suurempi kuin PCM16-read-chunk, ja `ERROR_*`-readit palautuvat `AudioRecordingFailure`-tuloksina.
- `AudioSessionManager.stopSession(emitCompleted = ...)` snapshottaa completion-datan synkronisesti, jotta failure-,
  reset- ja normaali stop -polut ovat idempotentteja. Normaali stop emittoi `completedSessionIds`-eventin; reset ja
  AudioRecord-failure viimeistelevät session hiljaisesti ilman auto-navigointia.
- Uuden session mittauscollector ohittaa `AudioEngine.decibelFlow`n replay-arvot, joiden timestamp on ennen session
  käynnistysaikaa. Tämä estää edellisen session viimeisen lukeman päätymisen uuden session statseihin tai Room-riveihin.

### 2026-05-12 - dB-laskennan summary- ja peak-dataflow

- `DecibelCalculator.calculateDb(...)` laskee chunkin RMS-tason, ja `calculatePeakDb(...)` laskee peak-tason
  suurimmasta PCM-amplitudista samalla kalibrointioffsetilla ja 0-130 dB clampilla.
- `FrequencyWeightingFilter.applyWeighting(...)` palauttaa painotetut samplet `DoubleArray`-muodossa. Painotettu
  signaali ei palaudu PCM16-alueelle ennen `DecibelCalculator`ia, jotta A/B/C/ITU-R 468 -vasteet ja C-painotettu
  peak-laskenta eivat leikkaudu positiivisen vahvistuksen kohdissa.
- A-, B-, C- ja ITU-R 468 -suodinkertoimet ovat 44.1 kHz:n SOS-kaskadeja, jotka verifioidaan referenssitaajuuspisteilla
  `FrequencyWeightingFilterTest`issa.
- `AudioEngine.DecibelReading` kuljettaa erikseen raw RMS -arvon (`instantDb`), valitulla painotuksella lasketun
  RMS-arvon (`weightedDb`) ja C-painotetun peak-arvon (`peakDb`). LCpeak-raportointi, peak warningit ja session
  `peakDb` käyttävät `peakDb`-arvoa, eivät raw RMS -arvoa.
- `SessionStats.avgDb` on energia-average painotetuista lukemista. Session `minDb`/`maxDb` ovat weighted-arvoja, ja
  session `peakDb` on C-painotettu peak. `sessions`-taulun summary-arvot ovat valmiin session headline-mittareiden
  lähde.
- `SessionReportCalculator` käyttää headline-mittareissa session summarya (`avgDb` -> LAeq, `minDb`, `maxDb`,
  `peakDb` -> LCpeak). Persistoidut measurement-rivit ovat raportin time-series-lähde; A-painotetuissa sessioissa
  `dbWeighted` muodostaa 85 dBA peak event -jaksot. Rivikohtainen `peakDb` ei muodosta 85 dBA eventtejä, koska se on
  C-painotettu LCpeak-arvo.
- Historyn hourly/daily summaryt muodostetaan repositoryssa `MeasurementBucketAverages`-helperilla energia-averageena;
  DAO ei käytä enää SQL:n aritmeettista `AVG(dbWeighted)`-laskentaa näihin summaryihin.

### 2026-05-12 - Privacy-sensitive data handling

- Android system backup on poistettu käytöstä manifestissa, ja `backup_rules.xml`/`data_extraction_rules.xml` sulkevat
  appin root-datan pois sekä cloud backupista että device-transferista. Käyttäjän manuaaliset paikalliset backupit jäävät
  `filesDir/backups`-polkuun `LocalBackupManager`in hallintaan.
- `FileProvider` rajautuu vain `cache/exports/`-hakemistoon. `ExportFileCache` on CSV- ja PNG-jakotiedostojen yhteinen
  cache-polku ja poistaa yli 24 tuntia vanhat export-tiedostot seuraavan export/share-operaation yhteydessä.
- CSV-exportin `ACTION_SEND_MULTIPLE` antaa jaettaville `content://`-URI:lle väliaikaisen lukuoikeuden sekä
  `EXTRA_STREAM`in että `ClipData`n kautta.
- Mittausnotificationien lukitusnäkyvyys keskitetään `NotificationPrivacyPolicy`yn, ja live dB -sisältö ei käytä
  `VISIBILITY_PUBLIC`-asetusta.
- Settingsin Health Connect -kortti tarjoaa Manage-toiminnon Health Connectin hallintanäkymään. Noise sync ja heart rate
  overlay pyytävät edelleen omat suppeat permission-settinsä.

### 2026-05-12 - Billing lifecycle recovery

- `BillingManager.queryExistingPurchases()` ei enää pelkästään aseta Pro-tilaa, vaan prosessoi Play Billingin palauttaman
  ostosnapshotin saman ostotuotteen käsittelypolun kautta. `PURCHASED` `dbcheck_pro` acknowledgeataan tarvittaessa myös
  startup-/reconnect-kyselyn jälkeen, mutta kyselypolku ei julkaise käyttäjälle uutta `Completed`-eventtiä jokaisella
  käynnistyksellä.
- `BillingManager.refreshPurchases()` on ostosnapshotin julkinen refresh-portti. `MainActivity.onResume()` kutsuu sitä,
  jotta appi käsittelee Play Billingin ulkopuolella valmistuneet tai pending-tilasta valmistuneet ostot foregroundiin
  palatessa.
- `ITEM_ALREADY_OWNED` ei jää pelkkään paikalliseen `_isPurchased = true` -tilaan. Se julkaisee `AlreadyOwned`-eventin ja
  käynnistää `queryExistingPurchases()`-haun, jotta Playn ostotoken saadaan ja mahdollinen acknowledge-puute voidaan
  korjata.
- `PurchaseState.PENDING` ei avaa Pro-oikeutta. Billing julkaisee `PurchaseEvent.Pending`-eventin, ja Settings näyttää
  pending-viestin ilman pysyvää virhetilaa.
- Settingsin purchase-virhetilat nollaavat aiemman success/pending-viestin, jotta Pro-kortti ei näytä ristiriitaisia
  ostoviestejä saman aikaisesti.

### 2026-05-12 - Pro feature gate hardening

- `data/local/preferences/model/ProAudioPreferencePolicy.kt` keskittää Pro-audioasetusten effective-arvot. Free-käyttäjän
  calibration offset ja frequency weighting palautuvat aina `UserPreferenceDefaults`-arvoihin, vaikka DataStoressa olisi
  aiemmin tallennettuja Pro-arvoja.
- `SettingsViewModel` ei persistoi calibration- tai frequency weighting -muutoksia, ellei `isProUser` ole tosi.
  `AudioSessionManager` käyttää samaa `ProAudioPreferencePolicy`-polkua ennen kuin se kutsuu
  `AudioEngine.setCalibrationOffset(...)`- ja `AudioEngine.setWeighting(...)`-metodeja.
- `AudioSessionManager.startSession()` lukee ensimmäiset effective Pro-audioasetukset synkronisesti ennen
  `AudioEngine.startRecording(...)`-kutsua, jotta edellisen session calibration offset ei ehdi vaikuttaa uuteen
  mittaukseen ennen preference-collectorin ensimmäistä emissiota.
- `domain/session/SessionHistoryPolicy` on Free-historian 7 päivän ikkunan lähde. History-listaus, vanhojen sessioiden
  cleanup ja Session Detailin suora `history/detail/{sessionId}` -reitti käyttävät samaa ikkunaa, joten Free-käyttäjä ei
  voi avata vanhaa sessiota pelkällä session id:llä.
- Hearing test on gateattu UI-entryn lisäksi execution- ja data-polussa: `ActiveTestViewModel` ei käynnistä tone
  playbackia Free-tilassa, `HearingTestService.saveCompletedTest(...)` ei tallenna Free-tulosta, ja
  `ResultsViewModel` ei lataa tai jaa hearing-test-resultia, ellei käyttäjä ole Pro.

### 2026-05-13 - DAO-aikarajat ja deterministinen järjestys

- DAO-kyselyiden `ORDER BY` -ehdot käyttävät aikaleiman lisäksi primary key -tie-breakeriä (`id DESC` latest/recent
  -listoissa, `id ASC` measurement time-series -riveissä), jotta samaan millisekuntiin osuvat sessiot, mittaukset ja
  kuulotestitulokset palautuvat deterministisesti.
- `MeasurementRepository` päivittää 24h/7d-rullaavat measurement-ikkunat minuutin välein
  `rollingWindowRanges(...)` + `flatMapLatest` -polulla. Historyn hourly-summary, Analyticsin daily-summary ja
  Environment Mix eivät jää ViewModelin luontihetken `since`-arvoon, eivätkä tulevaisuuteen osuvat timestampit päädy
  "last 7 days" / "last 24 hours" -yhteenvetoihin.
- Analyticsin daily-summary bucketoi viikon päiväkeskiarvot käyttäjän paikallisen aikavyöhykkeen päivän alkuun.
  `DailyExposureUiState.isToday` kertoo erikseen, mikä bucket on oikeasti kuluva paikallinen päivä; puuttuvaa tämän
  päivän bucketia ei enää tulkita eilisen tai viimeisimmän mittauspäivän perusteella.
- Pro-analytiikan 30 päivän ja 12 kuukauden ikkunat päivittyvät `AnalyticsViewModel`issa samalla minuutin tickillä:
  query-parametrit (`monthStartMs`, `yearStartMs`, `nowMs`) lasketaan uudelleen ja Room Flow -kyselyt resubscribataan.
  Pro-käyttäjän monthly/yearly-analytiikka pitää Analytics-näkymän `Success`-tilassa myös silloin, kun viimeisen 7 päivän
  weekly-dataa ei ole.

### 2026-05-14 - Session repositoryn transaktiokirjoitukset

- `SessionRepository` omistaa mittausrivien ja session summaryn yhteiskirjoitukset. `recordActiveSessionMeasurements(...)`
  kirjoittaa flushatut measurement-rivit ja aktiivisen session runtime-summaryn samassa Room `withTransaction`-blokissa.
  `completeSessionWithMeasurements(...)` kirjoittaa viimeiset pending-rivit ja sulkee session samassa transaktiossa.
- `AudioSessionManager` tallentaa aktiivisen session luonnissa nykyisen effective frequency weighting -arvon.
  Flush-polku päivittää aktiivisen session `minDb`/`avgDb`/`maxDb`/`peakDb`-arvot, jotta interrupted-session recovery
  voi palauttaa myös LCpeak-summaryn.
- `MeasurementRepository` on nyt read/analytics-repository: mittausrivien write-portit eivät ole siellä, ja
  aggregointien Flow-mappaukset (`groupBy`, energia-average ja domain-projektiot) siirtyvät injektoidulle
  `DefaultDispatcher`ille.
- Room-skeema v3 lisää `measurements.peakDb`-sarakkeen. `MIGRATION_2_3` backfillaa vanhoille riveille `dbWeighted`-arvon,
  ja uudet rivit tallentavat C-painotetun LCpeak-arvon. CSV ja raportin LCpeak-polku lukevat rivikohtaisen peak-arvon,
  mutta 85 dBA peak event -ryhmittely käyttää vain A-painotetun session `dbWeighted`-arvoja.
- `MeasurementPersistenceSampler` pakottaa persistoinnin myös uuden session LCpeak-huipun kohdalla. Samplerin 1s cadence
  pysyy ennallaan, mutta threshold crossing, weighted max, LCpeak max ja stopin viimeisin lukema voivat lisätä rivejä.
- `AudioSessionManager` sarjallistaa measurement flushin ja session completionin `Mutex`illa. Käynnissä oleva flush
  suoritetaan loppuun ennen session sulkemista, jotta stop ei tyhjennä tai ohita jo flushiin valittuja pending-rivejä.
- `AudioSessionManager.startSession()` ja startupin `recoverInterruptedSession()` jakavat yhden interrupted-session
  recovery -portin. Ensimmäinen uusi mittaus sulkee mahdollisen edellisen prosessin aktiiviseksi jättämän session ennen
  uuden `SessionEntity`n luontia, ja recovery ei viimeistele muistissa jo käynnissä olevaa nykyistä sessiota.
- `MeasurementForegroundService` käsittelee mittauksen pysäytyksen eksplisiittisenä stop-komentona
  (`ACTION_STOP_MEASUREMENT` + `EXTRA_EMIT_COMPLETED`). Meterin reset-polku lähettää stopin `emitCompleted=false`, jotta
  service teardown ei julkaise `completedSessionIds`-navigointieventtiä resetistä.
- `MeasurementBucketAverages` laskee hourly/daily energia-averaget aikaleimaväleillä painotettuna, jotta forced-rivit
  eivät saa samaa painoa kuin normaali persistence-cadence.
