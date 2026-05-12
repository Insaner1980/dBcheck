<claude-mem-context>
# Memory Context

# [dBcheck] recent context, 2026-05-12 4:52pm GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (17,696t read) | 796,646t work | 98% savings

### May 7, 2026
5249 12:02p 🔵 Frequency weighting implementation uses dual type system
5250 " 🔵 MeterViewModel starts recording without applying user preferences to AudioEngine
5251 12:03p ⚖️ Resuming staged feature development workflow
5252 12:07p 🟣 AudioSessionManager now dynamically applies user preference changes to AudioEngine
5253 " ✅ Added preference observer cleanup and weighting parser to AudioSessionManager
5256 " 🔵 ktlintCheck Gradle task not found in dBcheck project
5257 2:54p 🔵 Detekt found pre-existing code quality violations in HistoryScreen and BillingManager
5258 2:55p 🔵 AudioSessionManager.kt has 128 indentation violations but is not in detekt baseline
5259 " 🔵 Detekt configuration has no indentation rule customization
5260 " 🔵 Detekt uses formatting and Compose rules plugins with baseline file
5261 " 🔵 Android lint failed with MissingPermission error
5262 2:56p 🔵 AudioEngine.kt line 87 creates AudioRecord without explicit permission check
5263 " 🔵 No lint-baseline.xml file exists; only detekt-baseline.xml present
5264 " 🔴 Added @RequiresPermission annotation to AudioEngine.createAudioRecord()
5265 3:40p 🔵 lint-check script located at ~/bin/lint-check runs standalone ktlint, not Gradle task
5266 " 🔵 Two versions of lint-check script exist with different implementations
5267 3:41p 🔵 None of the lint-check script variants use ktlintCheck Gradle task
5268 " 🔵 lc function defined in PowerShell profiles as wrapper for lint-check script
5269 3:42p 🔵 PowerShell profile executes ~/bin bash scripts via Git Bash wrapper functions
### May 8, 2026
5276 4:34p 🔵 dBcheck Block 12 implementation partially complete
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
**5341** 11:13a 🔵 **SonarQube Scan Identified 64 Open Issues**
A SonarQube code quality scan was performed on the project, identifying 64 open issues. These issues represent code quality, security, or maintainability concerns detected by SonarQube's static analysis. The issues will need to be reviewed, prioritized, and addressed to improve code health.
~133t 🔍 7,413

**5342** 11:47a 🔵 **SonarQube Scan Identified 64 Open Issues**
A SonarQube static analysis scan was executed on the codebase, identifying 64 open issues. These issues represent code quality, security, or maintainability concerns flagged by SonarQube's rule engine. The findings establish a baseline for code quality improvement efforts and provide actionable items for remediation.
~162t 🔍 7,413

**5343** 11:48a 🔄 **Refactored Coroutine Dispatchers to Use Hilt Dependency Injection**
A SonarQube scan identified 64 open issues in the dBcheck Android codebase. The primary cluster addressed was hardcoded coroutine dispatcher usage across billing, service, and audio session management layers. The refactoring introduced Hilt qualifier annotations (@DefaultDispatcher, @IoDispatcher, @MainDispatcher) defined in di/CoroutineDispatchers.kt, with AppModule providing the actual Dispatchers.* instances. BillingManager, ProFeatureManager, and AudioSessionManager were updated to receive dispatchers via constructor injection. MeasurementForegroundService, as an Android Service requiring field injection, uses lateinit var with @Inject and @MainDispatcher annotations and initializes the CoroutineScope in onCreate. Additional detekt rule violations were resolved by extracting data classes to separate files matching their declaration names and fixing import statement ordering. The changes improve testability by allowing test code to inject TestDispatchers, follow Android dependency injection best practices, and eliminate direct coupling to kotlinx.coroutines.Dispatchers singleton. Build verification confirmed all Kotlin compilation, Android Lint, and detekt analysis passed successfully. Documentation was updated in AGENTS.md and memory/MEMORY.md to record the dispatcher injection pattern and Gradle dependency locking configuration.
~610t 🛠️ 92,154

**5345** 2:50p 🔴 **Privacy hardening implemented with TDD regression tests**
Privacy hardening was implemented following test-driven development. Three new test classes were added (PrivacyConfigTest, ExportFileCacheTest, NotificationPrivacyPolicyTest) that failed initially due to missing implementation. Implementation then added: disabled system auto-backup in manifest, created backup rules that exclude all app data, restricted FileProvider to exports/ subdirectory, created ExportFileCache utility for isolated cache management with 24h retention, made measurement notifications private on lock screen, and clarified export/backup UI copy. The targeted privacy tests all passed. Full test suite revealed unrelated failures in MeterViewModelShareTest and ResultsViewModelShareTest due to SharedFlow refactoring, following the same pattern as the earlier CSV export test failures (methods now emit to flows instead of returning values).
~604t 🛠️ 90,090

**5346** " 🔵 **SharedFlow migration broke existing share intent tests**
The full test suite revealed that the SharedFlow refactoring for share intents affected more than just CSV export. MeterViewModelShareTest and ResultsViewModelShareTest are failing with the same pattern: tests call methods expecting Intent? return values, but the methods now emit to SharedFlows and return Unit. The CSV export tests were fixed by using Turbine's test {} block to collect from the SharedFlow, verifying emissions with awaitItem() or expectNoEvents(). The same fix pattern will need to be applied to MeterViewModel and ResultsViewModel share tests.
~412t 🔍 90,090


Access 797k tokens of past work via get_observations([IDs]) or mem-search skill.
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
  käynnistyksessä.

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
  `EXERCISE_TYPE_OTHER_WORKOUT`-sessioksi, jonka `Metadata.clientRecordId` on `noise_dose_<date>_session_<id>` ja jonka
  notes-kentassa ovat LAeq, max, peak ja weighting. Kuulotestin Health Connect -kirjoitus on tietoisesti no-op kunnes
  Android tarjoaa tuetun audiometriatyypin tai erikseen suunnitellun FHIR-polun.
- `SettingsScreen` nayttaa `HealthSyncSection`-osion. Free-kayttaja voi sallia melusession Health Connect -synkkauksen;
  Pro-kayttajalle on erillinen heart rate overlay -asetus, joka pyytaa vain `READ_HEART_RATE`-permissionin.
- `AudioSessionManager.stopSession()` paivittaa valmiin session `frequencyWeighting`-arvon ja kutsuu
  `HealthConnectManager.writeNoiseDose(...)`, jos `healthConnectEnabled` on paalla. Ennen Health Connect -kirjoitusta
  se rakentaa `SessionReportCalculator`illa raportin flushatuista mittausriveista, joten synkkausnotesiin kirjattava
  LAeq tulee samasta laskennasta kuin PDF/PNG/Session Detail. Synkkaus ei blokkaa session valmistumisen navigointivirtaa.
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
- LAeq-energia-average on `domain/noise/DecibelMath.energyAverageDb(...)`-helperissa, jota raportti- ja analytiikkalaskenta
  kayttavat.
- PDF-export on `util/ExportPdfReportUseCase.kt`-luokassa. Compose kaynnistaa
  `ActivityResultContracts.CreateDocument("application/pdf")`-polun, jonka palauttamaan `Uri`:in use case kirjoittaa
  nelisivuisen natiivin `PdfDocument`-raportin.
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
- Settingsin `DataExportSection` kytkee CSV-viennin UI:hin. Free-käyttäjä saa ProLockOverlay-previewn, Pro-käyttäjän
  `Export CSV` -painike kutsuu `SettingsViewModel.createCsvExportIntent()`-polkua ja avaa Android Sharesheetin.

### 2026-05-09 - Paikallinen backup UI ja restore-flow

- `sync/BackupGateway.kt` on backup-infrastruktuurin testattava rajapinta. `service/BackupService.kt` on Settingsin
  UI-facing backup-portti, ja `CloudBackupManager` toteuttaa varsinaiset paikalliset `filesDir/backups`
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
  -valinnat High, Standard ja Low power.
- `MeterViewModel` lukee `PreferencesRepository.userPreferences`-virtaa ja throttlettaa vain Meterin UI-päivityksiä
  `MeterRefreshRate.uiIntervalMs`-arvolla. Jokainen raw dB -lukema käsitellään edelleen haptiikkaa ja safety-signaaleja
  varten.
- `service/AudioSessionManager` pitää `SessionStats`-tilastot kaikkien raw-lukemien perusteella, mutta
  `service/MeasurementPersistenceSampler` harventaa Roomiin tallennettavia `MeasurementEntity`-rivejä
  `persistenceIntervalMs`-arvolla. Ensimmäinen lukema, `NoiseLevel.ELEVATED.maxDb` threshold-crossing, uusi session max
  ja stopin viimeisin tallentamaton lukema pakottavat persistoinnin.
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
- `MeterViewModel` ei käynnistä audiosessiota suoraan `startForegroundService(...)`-paluuarvon perusteella. Se pyytää
  palvelun käyntiin ja peilaa Meterin `isRecording`-UI-tilan `AudioSessionManager.isRecording`-virrasta.
- Mittauspalvelu palauttaa onnistuneesta käynnistyksestä `START_NOT_STICKY`, koska prosessin tappamisen jälkeen nykyistä
  `AudioRecord`-sessiota ei palauteta eikä vanhaa ilmoitusta saa herättää ilman aktiivista mittausta.
- Palvelun `onDestroy()` pysäyttää aktiivisen `AudioSessionManager`-session, jotta normaali `stopService(...)`-polku,
  palvelun alasajo ja UI:n poistuminen käyttävät samaa cleanup-reittiä.

### 2026-05-12 - Privacy-sensitive data handling

- Android system backup on poistettu käytöstä manifestissa, ja `backup_rules.xml`/`data_extraction_rules.xml` sulkevat
  appin root-datan pois sekä cloud backupista että device-transferista. Käyttäjän manuaaliset paikalliset backupit jäävät
  `filesDir/backups`-polkuun `CloudBackupManager`in hallintaan.
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
- `domain/session/SessionHistoryPolicy` on Free-historian 7 päivän ikkunan lähde. History-listaus, vanhojen sessioiden
  cleanup ja Session Detailin suora `history/detail/{sessionId}` -reitti käyttävät samaa ikkunaa, joten Free-käyttäjä ei
  voi avata vanhaa sessiota pelkällä session id:llä.
- Hearing test on gateattu UI-entryn lisäksi execution- ja data-polussa: `ActiveTestViewModel` ei käynnistä tone
  playbackia Free-tilassa, `HearingTestService.saveCompletedTest(...)` ei tallenna Free-tulosta, ja
  `ResultsViewModel` ei lataa tai jaa hearing-test-resultia, ellei käyttäjä ole Pro.
