# dBcheck Agent Instructions

## Global Instructions

- Search current official docs, versions, and best practices before implementation when the change depends on external Android, Gradle, Play, or library behavior.
- Do not assume version numbers or API syntax from memory when a file or official source can verify them.
- Use Finnish in commit messages and when summarizing work to the user.
- When making architectural changes, update `AGENTS.md` and `memory/MEMORY.md` to reflect the new state.

## Code Quality Rules

- No duplicates: before adding code, check whether equivalent logic already exists.
- Centralize design tokens: colors, spacing, typography, animation specs, and card defaults belong in theme files when tokens exist.
- One source of truth per concept: shared values, policies, routes, and query patterns should be defined once.
- Delete dead code in the same edit that makes it unused.
- Check all callers on move or rename with `rg` and update imports.
- Verify the impact of shared-code changes against all consumers.

## Lint & Static Analysis

- `lint-check` / `lc`: user-run wrapper for ktlint, detekt, and Android lint. Results are written under `reports/`.
- `security-check` / `sc`: user-run wrapper for dependency verification, OSV, OWASP Dependency-Check, Gitleaks, TruffleHog, Semgrep secrets, and Semgrep Kotlin light. Results are written under `reports/`.
- `sentry`: verifies debug-only Sentry wiring. Debug must contain `io.sentry`, release must not contain `io.sentry`, and results are written to `reports/sentry.txt`.
- GitHub Actions skips the long OWASP Dependency-Check execution; run local `security-check` / `sc` when OWASP evidence is needed.
- When asked to read lint results, inspect `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`.
- When asked to read security results, inspect `reports/security-summary.txt`, `reports/security-deps.txt`, `reports/security-deps-raw.txt`, `reports/osv.txt`, `reports/semgrep-kotlin.txt`, `reports/semgrep-secrets.txt`, `reports/gitleaks.txt`, and `reports/trufflehog.txt`. `reports/security-code.txt` is not produced by the current wrapper.
- Do not run `lc` or `sc` yourself unless the user explicitly asks.
- `reports/` is gitignored and must not be committed.

## Project Architecture Notes

### 2026-06-08 - Debug-only Sentry diagnostics

- `DbCheckApplication.onCreate()` kutsuu source-set-kohtaista `SentryInit`-polkua.
- Debug source set alustaa `io.sentry:sentry-android-core`-riippuvuuden vain, jos `DBCHECK_SENTRY_DSN`, `SENTRY_DSN` tai ignored `debug.credentials.properties` sisältää DSN-arvon.
- Release source set on no-op, Sentry on vain `debugImplementation`-luokkapolussa, ja `tools\sentry.ps1` tarkistaa ettei `releaseRuntimeClasspath` sisällä `io.sentry`-riippuvuuksia.
- Älä lisää Sentry Gradle -pluginia, release crash reportingia, analyticsia, tracingia, replayta, source-context-uploadia tai logcat breadcrumbseja ilman uutta eksplisiittistä product/security-päätöstä.

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
- `BillingRuntimeGateway` on startup/resume-lifecycleportti ja `BillingEntitlementSource` on ostotilan stream-portti.
  `BillingManager` on näiden porttien tuotantototeutus; appin tuotantokuluttajat injektoivat billingin rajapintoina.
- Billingin käynnistystila on kolmivaiheinen: `BillingEntitlementSource.isPurchased` alkaa `null`-arvosta, joka tarkoittaa
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
  Metadata on `activelyRecorded`, koska mittaus kaynnistyy kayttajan toiminnolla. Notes-kentassa ovat
  `SessionReportData`sta luettu equivalent-level-label ja arvo, max, LCpeak seka kayttajalle naytettava
  weighting-label. Kuulotestin Health Connect -kirjoitus on tietoisesti no-op kunnes Android tarjoaa tuetun
  audiometriatyypin tai erikseen suunnitellun FHIR-polun.
- `SettingsScreen` nayttaa `HealthSyncSection`-osion. Free-kayttaja voi sallia melusession Health Connect -synkkauksen;
  Pro-kayttajalle on erillinen heart rate overlay -asetus, joka pyytaa vain `READ_HEART_RATE`-permissionin.
- Health Connectin manifest-entrypointit (`HealthConnectPermissionsRationaleActivity` ja
  `HealthConnectPermissionUsageActivity`) ovat exportattuja vain Health Connectin privacy/disclosure-polkuja varten.
  Molemmat aliakset targetoivat `HealthConnectPermissionDisclosureActivity`a, joka nayttaa staattisen disclosure-nakyman
  eika kayta MainActivityn navigaatiota, billingia, Settings-toimintoja tai dataa muuttavia polkuja.
- `AudioSessionManager.stopSession()` paivittaa valmiin session `frequencyWeighting`-arvon ja kutsuu
  `HealthConnectManager.writeNoiseDose(...)`, jos `healthConnectEnabled` on paalla. Ennen Health Connect -kirjoitusta
  se rakentaa `SessionReportCalculator`illa raportin flushatuista mittausriveista ja antaa saman
  `SessionReportData`n Health Connect -adapterille, joten notesin equivalent-level, max, LCpeak ja weighting-label
  tulevat samasta raporttimallista kuin PDF/PNG/Session Detail. Synkkaus ei blokkaa session valmistumisen
  navigointivirtaa; `Failed`-tulos emittoidaan `healthConnectSyncFailures`-virtaan, jota Meter UI nayttaa
  virheviestina.
- Session Detail lukee sykearvot `HealthConnectService.readHeartRateForSession(...)`-funktiolla, kun kayttaja on Pro,
  heart rate overlay on paalla ja Health Connect -permissionit on myonnetty. Piirto tapahtuu
  `ui/analytics/components/HeartRateOverlay.kt`-komponentilla.

### 2026-05-09 - Phase 12.4 Session Detail + PDF-raportti

- Session Detail sijaitsee reitissä `history/detail/{sessionId}`. Historyn sessiokortit navigoivat sinne, ja
  `MeterViewModel` kuuntelee `AudioSessionManager.completedSessionIds`-virtaa, jotta valmis mittaus avaa saman
  detail-nakyman.
- Tieteellisen raportin mittareilla on yksi lahde: `domain/report/SessionReportCalculator.kt`, joka rakentaa
  `SessionReportData`-mallin `domain/session/Session`- ja `domain/report/ReportMeasurement`-datasta. PDF, PNG-jako,
  Health Connect -notes ja UI lukevat samasta mallista eivatka laske equivalent-level- tai peak event -arvoja uudelleen.
- `domain/noise/DosimeterCalculator.kt` on TWA-, dose-, projected dose- ja remaining exposure time -laskennan lahde.
  Completed report kayttaa sita nykyisille NIOSH_REL-arvoille, ja live-dosimeter-flow'n kuuluu kayttaa samaa laskuria.
- NIOSH 8h TWA, NIOSH dose ja "85 dBA" peak event -lista ovat saatavilla vain A-painotetulle sessiolle.
  `SessionReportData.aWeightedExposureMetricsAvailable` kertoo saatavuuden; muilla painotuksilla TWA/dose ovat `null`,
  peak event -lista on tyhja, ja Session Detail/PDF/PNG nayttavat arvon puuttuvana eivatka laskettuna nollana.
- LAeq-energia-average on `domain/noise/DecibelMath.energyAverageDb(...)`-helperissa, jota raportti- ja analytiikkalaskenta
  kayttavat.
- PDF-export on `util/ExportPdfReportUseCase.kt`-luokassa. Compose kaynnistaa
  `ActivityResultContracts.CreateDocument("application/pdf")`-polun, jonka palauttamaan `Uri`:in use case kirjoittaa
  viisivuisen natiivin `PdfDocument`-raportin tai kuusivuisen raportin, jos Session Detailin Health Connect
  -sykeoverlay on kaytossa. `ReportHeartRateSection` valittaa PDF:lle samat sykepisteet, jotka UI nayttaa overlayssa.
- PDF:n Report Context -blokki lukee app-version ja Android-laitetiedot `PdfReportExportMetadata`sta, persisted
  response time -summaroinnin `SessionReportData.responseTimeSummary`sta ja nykyisen effective kalibrointioffsetin
  export-metadatana. Kalibrointioffset ei ole viela historiallinen session field, joten sita ei saa kayttaa
  menneiden sessioiden pysyvana mittausmetadatana ennen erillista upstream-persistointia.
- PDF:n Data Availability -sivu lukee locationin `SessionReportData.location`sta, A-painotetun completed-reportin
  NIOSH-standardin ja projected dosen `DosimeterCalculator`ilta seka persisted sound detection -eventtien yhteenvedon
  `SoundDetectionRepository.getReportSoundEventsForSession(...)` -polusta. Octave breakdown pysyy N/A-tilassa, ellei
  `SessionReportData.octaveBreakdownAvailable` tai non-zero `octaveCalibrationOffsets` kerro saatavasta octave-
  kontekstista; RTA time-series -dataa ei viela persistöidä. Puuttuvat upstream-lahteet naytetaan N/A-tekstina eika
  nollina.
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
- CSV-export muodostaa kolme FileProviderin kautta jaettavaa tiedostoa: sessioyhteenvedon, mittausrivit ja optional
  sound detection -eventit. Kaikissa on `session_name`, `session_emoji` ja `session_tags`, ja CSV-field escaping on
  keskitetty `data/export/CsvEscaper`-helperiin.
- `ExportCsvUseCase` kirjoittaa CSV:t streamina cache-tiedostoihin ja hakee mittausrivit per sessio sivuina
  `MeasurementDao.getMeasurementsForSessionExportPage(...)`-polulla, jotta export ei rakenna koko raw measurement
  -aineistoa muistiin eikä käytä yhtä isoa `IN (:sessionIds)` -kyselyä.
  Sound detection -eventit haetaan samoin sivuina `SoundDetectionEventDao.getEventsForSessionExportPage(...)`-polulla.
- `CsvExportSelection` tukee all-sessions- ja selected-session-id -batch-exportia. Settingsin nykyinen CSV-painike käyttää
  all-sessions-polun, mutta valitut sessiot käyttävät samaa streamaus-, FileProvider- ja ClipData-sopimusta.
- Settingsin `DataExportSection` kytkee CSV-viennin UI:hin. Free-käyttäjä saa ProLockOverlay-previewn, Pro-käyttäjän
  `Export CSV` -painike kutsuu `SettingsViewModel.createCsvExportIntent()`-polkua ja avaa Android Sharesheetin.
- Settingsin Clear history -kortti on Free- ja Pro-käyttäjille sallittu datanhallintatoiminto. Se vaatii
  vahvistusdialogin, estyy aktiivisen mittauksen aikana ja kutsuu `HistoryClearService.clearHistory()` -polkua.
  `SessionRepository.clearInactiveHistory()` poistaa inactive-sessiot Room-transactionissa; measurements- ja
  sound_detection_events-rivit poistuvat foreign-key cascaden kautta. `WavRecordingFileStore` poistaa vain poistettujen
  sessioiden WAV-tiedostot, eika clear history koske `filesDir/backups`-paikallisbackuppeja.

### 2026-05-09 - Paikallinen backup UI ja restore-flow

- `sync/BackupGateway.kt` on backup-infrastruktuurin testattava rajapinta. `service/BackupService.kt` on Settingsin
  UI-facing backup-portti, ja `LocalBackupManager` toteuttaa varsinaiset paikalliset `filesDir/backups`
  -tietokantakopiot; Google Drive- tai muu pilvisynkka ei kuulu nykyiseen flow'hun.
- Backupin luonti tekee Roomille WAL checkpointin ennen `dbcheck.db`-tiedoston kopiointia, mutta ei sulje
  `DbCheckDatabase`-singletonia. Restore sulkee tietokannan vasta, kun valittu backup on validoitu ja nykytilasta on tehty
  `dBcheck_pre_restore_*`-turvakopio.
- Restore poistaa vanhat `dbcheck.db-wal`- ja `dbcheck.db-shm`-sivut ennen korvaavaa kopiointia. Onnistuneen restoren
  jälkeen `SettingsViewModel.confirmRestoreBackup(...)` kutsuu Settingsin restore-confirm-polusta saamaansa
  `onRestartAfterRestore`-callbackia suoraan, jotta restart ei riipu Compose-event-collectorista. `MainActivity`
  toteuttaa callbackin `AlarmManager` + immutable `PendingIntent` + `finishAffinity()` + `Process.killProcess()`
  -polulla, koska suljettua Room-instanssia ei voi käyttää turvallisesti samassa prosessissa.
- `SettingsViewModel` estää backup- ja restore-toiminnot aktiivisen mittauksen aikana ja näyttää saman inline-viestipolun
  kautta onnistumiset ja virheet kuin CSV-vienti.
- `DataExportSection` näyttää Free-käyttäjillekin Local backups -kortin CSV-viennin rinnalla. CSV pysyy Pro-gatettuna,
  mutta paikallinen backup/restore on kaikkien käyttäjien dataturvatoiminto.

### 2026-05-09 - Live-spektrianalyysin dataflow

- `AudioProcessingConfig` keskittää audio-domainin 44.1 kHz sample raten, 4096 sample chunkin ja 4096 point FFT-koon.
- `SpectralAnalyzer` on `FFTProcessor`in päälle rakennettu live-spektrin domain-muunnin. Se tuottaa 24 logaritmista
  20 Hz-20 kHz bandia, dominanttitaajuuden ja bandwidth-luokan raw PCM16 -chunkista.
- `FFTProcessor.binFrequency(...)` on FFT-binien taajuusmuunnoksen yhteinen lähde. `SpectralAnalyzer` ja
  `OctaveBandRtaCalculator` käyttävät samaa helperiä, jotta bin-taajuudet eivät driftää eri analytiikkapolkujen välillä.
- `OctaveBandRtaCalculator` tuottaa domain-tason octave/third-octave RTA-datan nykyisen `FFTProcessor`in magnitudi-
  spektristä. Se käyttää IEC/ANSI base-10-kaavaa keskitaajuuksiin ja band edgeihin, aggregoi bandin FFT-magnitudit,
  voi lukea `OctaveCalibrationOffsets`-mallin octave-resoluutiolle ja normalisoi amplitudit vahvimpaan kalibroituun
  RTA-bandiin. AudioEngine käyttää toistaiseksi zero-offset-oletusta, kunnes runtime-kytkentä valittuun profiiliin on
  valmis.
- `AudioEngine.spectralFrame` ja `AudioEngine.rtaFrame` julkaisevat vain live-only-spektritilaa. Spektri ja octave-RTA
  lasketaan raw, unweighted mikrofonidatasta ja tyhjennetään stopissa tai kun Pro-oikeus poistuu. RTA-dataa ei
  persistöidä Roomiin.
- `service/AudioSessionManager` ohjaa `AudioEngine.setSpectralAnalysisEnabled(...)`-asetusta
  `UserPreferences.isProUser`-arvolla.
  Meter käynnistää edelleen mittauksen normaalin session manager -polun kautta; Analytics vain lukee live-tilaa.
- `AnalyticsViewModel` yhdistää historiakeskiarvot, Pro-oikeuden, recording-tilan ja `spectralFrame`-virran.
  Ensimmäisen mittauksen aikana Analytics voi näyttää spektrikortin, vaikka Roomissa ei vielä ole historiadataa.
- `SpectralAnalysisCard` käyttää ViewModelilta tulevaa `SpectralAnalysisUiState`-tilaa. Free-käyttäjän kortti saa vain
  staattisen locked-previewn; live-spektriä ei välitetä overlayn alle.
- `measurements.frequencyData` on edelleen käyttämätön eikä spektridataa persistöidä tässä vaiheessa.

### 2026-06-12 - Sound detection -infrastruktuuri

- YAMNet-assetit ovat Android assets -polussa `sound_detection/yamnet.tflite` ja
  `sound_detection/yamnet_class_map.csv`; polut omistaa `YamnetModelAssets`.
- `YamnetAudioWindowAdapter` muuntaa nykyisen 44.1 kHz PCM16 -chunk-virran YAMNetin 16 kHz float-windowiksi eikä
  persistoi raakaaudiota.
- `SoundClassifier` on testattava inference-portti. `TfliteSoundClassifier` on tuotantototeutus, joka käyttää
  TensorFlow Lite Task Audio `AudioClassifier`ia ja mapittaa tulokset `SoundClassification`-domain-malliin
  `SoundClassificationPolicy`n confidence thresholdin kautta.
- `SoundDetectionWindowFanout` on `AudioEngine`n live-only raw-audio fanout YAMNet-windoweille. Se on päällä vain
  `AudioSessionManager`in effective-ehdolla `isProUser && soundDetectionEnabled`, käyttää pudottavaa yhden windowin
  `SharedFlow`-bufferia eikä persistoi raakaaudiota.
- `AudioSessionManager.soundDetectionState` julkaisee sound detection -live-tilan: enabled, current detection ja
  recent detections. Classifier-kutsu tapahtuu managerissa erillisessä collector-jobissa; `AudioEngine` ei tee inferenceä.
- Environment UI -kortti renderöi locked/idle/live/error-tilat. Optional detection persistence on erillinen DataStore
  opt-in `soundDetectionPersistenceEnabled`, ja `AudioSessionManager` tallentaa vain aggregoidut label-vaihdos-eventit
  `SoundDetectionRepository`n kautta Room-tauluun `sound_detection_events`. Raakaaudiota tai YAMNet-float-windowia ei
  persistöidä.
- Settingsin feature toggle `sound_detection` käyttää samaa DataStore-avainta kuin inference-gate. Pro-käyttäjän OFF
  pysäyttää live-inferencen ja piilottaa Analyticsin Environment-osion sound detection -kortin; Free-käyttäjän effective
  tila pysyy OFF eikä toggle avaa Pro-dataa.

### 2026-06-12 - CameraX-riippuvuuspohja

- CameraX on lukittu version catalogissa stable-versioon `1.6.1`, joka tarkistettiin virallisesta AndroidX Camera
  release-dokumentaatiosta ennen lisäystä.
- App dependencyt ovat `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` ja
  `camera-video`. Kaikki käyttävät samaa `cameraX`-version catalog -lähdettä.
- Dependency lock state ja Gradle dependency verification metadata sisältävät CameraX:n ja sen transitiiviset
  riippuvuudet. Älä lisää CameraX-artifactia ilman lockfile- ja verification-metadata-päivitystä.
- `CAMERA`-manifest-lupa on deklaroitu, ja `ui/camera/CameraPermissionPolicy.kt` omistaa kamera-routea varten
  runtime permission -tilat: initial request, granted, denied/rationale ja permanently denied/settings.
- `Screen.CameraOverlay` / `camera_overlay` on Pro-gatettu Meterin kamera-entryn takaa. Reitti ei ole top-level-
  kohde, joten bottom nav ja navigation rail piiloutuvat fullscreen overlayllä.
- `ui/camera/CameraOverlayRoute.kt` pyytää kameran runtime-luvan, sitoo `PreviewView` + CameraX `Preview`,
  `ImageCapture` ja `VideoCapture<Recorder>` -use caset `ProcessCameraProvider.bindToLifecycle(...)` -polkuun
  `CameraSelector.DEFAULT_BACK_CAMERA`lla ja näyttää camera-unavailable fallbackin, jos provider tai bind epäonnistuu.
- `CameraOverlayViewModel` lukee overlayn live-readoutin `AudioEngine.decibelFlow`sta vain aktiivisen mittauksen aikana
  (`AudioSessionManager.isRecording` ja `activeSessionStartTimeMs`). Idle-tilassa overlay näyttää tyhjän dB-arvon eikä
  edellisen session replay-lukemaa. Painotuslabel tulee effective preference -polusta
  `ProAudioPreferencePolicy.weighting(...)` + `equivalentLevelLabelForWeighting(...)`.
- `CameraOverlayShareGenerator` omistaa photo capture -jakopolun: raw JPG kirjoitetaan väliaikaisesti
  `ExportFileCache`n export-cacheen, nykyinen readout poltetaan PNG-bittikarttaan, raakakuva poistetaan ja jaettava
  PNG julkaistaan FileProviderin `content://`-URIlla, `ClipData`lla ja väliaikaisella read grantilla.
- Silent video capture kirjoittaa `dBcheck_camera_silent_video_*.mp4` -tiedoston samaan export-cacheen
  `FileOutputOptions`illa. Tallennus ei kutsu CameraX `withAudioEnabled()` -polkua, ei pyydä videoon
  `RECORD_AUDIO`-lupaa eikä lisää uutta raakaaudion keräystä. Compose-overlay ei ole poltettuna MP4:ään; se vaatii
  erillisen renderöinti- tai post-processing-polun ennen kuin videoon voidaan sisällyttää sama burned-in readout kuin
  PNG-jakoon.
- Photo capture, silent video, permission-policy ja live-readout eivät muuta Meterin mikrofonilupaa,
  startup-lupapolitiikkaa tai mittauksen käynnistystä.

### 2026-06-20 - WAV recording opt-in, writer ja export/delete

- `wav_recording_default` on Pro-gatettu DataStore-asetus, joka on default OFF. Settingsin Data & Export -osio näyttää
  asetuksen sekä privacy-warningin raakaaudiosta.
- `SettingsViewModel.updateWavRecordingDefaultEnabled(...)` estää Free-käyttäjän enable-päivityksen ja UI-state näyttää
  Free-tilassa effective OFF -arvon, vaikka DataStoreen olisi joskus tallennettu true.
- `PcmWavWriter` kirjoittaa mono PCM16 WAV -tiedostoa streamina ilman koko tallenteen puskurointia. Header kirjoitetaan
  alussa placeholderina ja paivitetaan close-polussa oikealla RIFF/data-koolla.
- `WavRecordingFileStore` luo WAV-tiedostot app-private `filesDir/wav_recordings` -hakemistoon muodossa
  `dBcheck_wav_session_<sessionId>_<startedAtMs>.wav`.
- `AudioSessionManager` kaynnistaa WAV-kirjoituksen vain effective-ehdolla
  `isProUser && wavRecordingDefaultEnabled`. Normaali stop sulkee WAV-headerin, ja failure/cleanup kutsuu
  `AudioEngine.abortWavRecording()` -polkua partial-tiedoston poistamiseksi.
- `WavRecordingFileStore` hakee viimeisimmän session WAV:n, luo `audio/wav` `ACTION_SEND` -jaon FileProviderin
  `content://`-URIlla, `ClipData`lla ja `FLAG_GRANT_READ_URI_PERMISSION` -lipulla sekä poistaa vain kyseisen session
  WAV-tiedoston.
- `file_paths.xml` julkaisee app-private `filesDir/wav_recordings` -hakemiston FileProviderille. WAV-tiedostoa ei
  kopioida MediaStoreen, vaan jaetaan vain käyttäjän käynnistämällä Sharesheetilla.
- Session Detail näyttää WAV-kortin, kun avattavalla sessiolla on WAV-tiedosto. Share on Pro-gatettu; delete poistaa
  tiedoston ilman uutta raakaaudion käsittelyä. Manual share smoke ajettiin `Pixel_9_Pro`-emulaattorilla: Sharesheet
  avautui WAV-tiedostolle ja delete tyhjensi app-private `files/wav_recordings` -hakemiston.

### 2026-06-24 - Sleep Monitor schema

- Room schema v10 lisää `sleep_sessions`- ja `sleep_notable_events`-taulut. Tavalliseen `sessions`-tauluun ei lisätä
  sleep-spesifisiä sarakkeita; Sleep Monitor metadata pidetään erillisessä one-to-one-taulussa `sessionId`-avaimella.
- `sleep_sessions` sisältää `sessionId`, `targetDurationMinutes`, `keepAwakeEnabled` ja `createdAt`. Se viittaa
  `sessions.id`-avaimeen cascade-FK:lla, joten tavallisen session poisto poistaa myös Sleep-metadatan.
- `sleep_notable_events` sisältää `sessionId`, `timestamp`, `eventType`, optional `levelDb` ja optional `durationMs`.
  Taulu viittaa `sleep_sessions.sessionId`-avaimeen, joten Sleep-notable eventtiä ei voi tallentaa ei-Sleep-sessiolle
  ilman Sleep metadata -riviä. Indeksit ovat `sessionId,timestamp` ja `timestamp`.
- `SleepSessionDao` on schema-tason DAO metadata- ja event-riveille. Runtime sleep recording, results UI ja export
  tulevat myöhemmissä osissa eivätkä vielä kirjoita näihin tauluihin.
- `MIGRATION_9_10` luo taulut ja indeksit. Exportattu schema on `app/schemas/.../10.json`, ja v10 identity hash
  `e4c97360fab833b6bc30549ab7e8075f` on `BackupDatabaseValidator`in sallituissa hasheissa.

### 2026-06-24 - Sleep setup state

- `SleepSetupViewModel` julkaisee `SleepSetupUiState`-datamallin, jossa `availability` tulee effective
  `UserPreferences.isProUser` -arvosta. `sleep_card` on vain Meter/Analytics CTA:n visibility-asetus; se ei lukitse
  Pro-käyttäjän suoraa `sleep/setup`-valmisteluruutua.
- Pro-käyttäjän Sleep setup -valinnat ovat `targetDurationMinutes` vaihtoehdoista 6h/8h/10h sekä
  `keepAwakeEnabled`. Free-tilassa ViewModel ei muuta näitä arvoja.
- `SleepSetupScreen` näyttää duration-chipit, keep screen awake -kytkimen sekä privacy- ja battery-copyt. Osa 79 kytki
  ruutuun käyttäjän käynnistämän aktiivisen Sleep recording -polun.

### 2026-06-25 - Sleep active recording

- `domain/sleep/SleepRecordingConfig` on Sleep Monitorin 6h/8h/10h target-duration -vaihtoehtojen ja
  `keepAwakeEnabled`-valinnan yhteinen domain-lähde. `SleepSetupDefaults` lukee samat arvot tästä mallista.
- `SleepSetupViewModel.startSleepRecording()` käynnistää `MeasurementForegroundService.startSleepIntent(...)` -polun
  vain Pro-ready-tilassa ja mikrofoniluvan jälkeen. ViewModel ei kutsu `AudioSessionManager.startSession()`-polkua
  suoraan; varsinainen audio-start tapahtuu foreground servicessä käyttäjän näkyvästä toiminnosta.
- `MeasurementForegroundService` tukee `MeasurementRecordingMode.Meter`-, `MeasurementRecordingMode.Sleep`- ja
  `MeasurementRecordingMode.Passive`-moodeja. Sleep-mode käyttää samaa `FOREGROUND_SERVICE_TYPE_MICROPHONE` -serviceä
  kuin Meter, rakentaa Sleep-spesifisen notification copyn ja pysäyttää session automaattisesti, kun valittu target
  duration täyttyy. Passive-mode on erillinen aggregate-only foreground sample, ei Sleep-session polku.
- `AudioSessionManager.startSleepSession(...)` käyttää samaa lifecycle-, mittaus-, stop- ja completion-polkuja kuin
  tavallinen mittaus, mutta kirjoittaa luodun session ID:n `SleepSessionRepository`n kautta `sleep_sessions`-tauluun.
  Tavalliseen `sessions`-tauluun ei lisätä sleep-spesifisiä sarakkeita.
- `ui/common/KeepScreenOnEffect` omistaa `FLAG_KEEP_SCREEN_ON` -lipun acquire/release-logiikan. Meter käyttää sitä aina
  aktiivisessa mittauksessa; Sleep käyttää sitä vain ehdolla `isRecording && keepAwakeEnabled`, joten Sleep-mittaus
  jatkuu foreground servicessä ilman UI:n päällä pysymisen oletusta.

### 2026-06-25 - Sleep results

- `domain/sleep/SleepSession` on `sleep_sessions`-rivin domain-malli. `SleepSessionRepository` tarjoaa read-polut
  yksittäiselle Sleep-session metadatalle sekä Historyn tarvitseman Sleep-session ID-joukon.
- `domain/sleep/SleepResultsCalculator` rakentaa Sleep results -yhteenvedon olemassa olevasta `SessionReportData`sta
  eikä laske mittausarvoja UI:ssa. Yhteenveto sisältää target/recorded-kestot, equivalent levelin, maxin, LCpeakin,
  peak-event-countin, loud-period-countin ja histogram bucketit.
- History ei muuta tavallista `Session`-mallia Sleep-spesifiksi. `HistoryViewModel` yhdistää Sleep-ID-flow'n
  UI-stateen, ja `SessionCard` näyttää Sleep-badgen vain renderöintitasolla.
- Session Detail näyttää `SleepResultsCard`in vain, kun avattu session ID löytyy `sleep_sessions`-taulusta. Tavallinen
  histogrammi- ja peak-event-kortti pysyy samassa report-dataflow'ssa.
- Persistöity notable event -analyysi ei vielä kuulu tähän polkuun; se alkaa Osa 82+.

### 2026-06-25 - Sleep export/report

- `domain/report/ReportSleepSection` on Sleep-yhteenvetojen report/export-malli. `SessionDetailViewModel` kopioi
  `SleepResultsCalculator`in tuloksen `SessionReportData.sleep`-kenttään, jotta PDF-vienti lukee saman yhteenvedon kuin
  Session Detailin Sleep Results -kortti.
- `CsvExportFormatter` lisää sessions CSV -tiedostoon sleep-sarakkeet: `is_sleep_session`, `sleep_target_minutes`,
  `sleep_keep_awake` ja `sleep_created_at`. Tavalliset sessiot saavat `false` + tyhjät fallback-kentät; mittaus- ja
  sound detection -CSV:t pysyvät ennallaan.
- `ExportCsvUseCase` hakee Sleep metadata -rivit `SleepSessionDao.getSleepSessionsForCsvExportByIds(...)` -kyselyllä
  samoille session ID:ille, jotka valittu all/selected CSV-export kirjoittaa.
- PDF:n Data Availability -sivu näyttää Sleep target-, recorded-, keep-awake-, loud-period- ja peak-event-rivit. Kun
  sessio ei ole Sleep-session, arvot näytetään `N/A`:na eikä nollina.

### 2026-06-25 - Sleep insights

- `domain/sleep/SleepInsightsCalculator` analysoi `SessionReportData.timeSeries`-sarjasta loud period -jaksot
  Sleep-notable event -yhteenvedoiksi. Se palauttaa `MissingMeasurements`-tilan, jos time-series puuttuu, jotta
  Sleep-yhteenvedot eivät näytä puuttuvaa analyysiä nollana.
- `SleepResultsCalculator` omistaa edelleen Sleep Results -yhteenvedon, mutta sen peak-event-, loud-period- ja
  sample-count-arvot ovat nullable-arvoja, kun insight-analyysi ei ole saatavilla. Saatavilla oleva mutta hiljainen
  mittaus voi edelleen näyttää aidon `0`-arvon.
- `SessionDetailViewModel` mapittaa `SleepInsightsSummary`n `SleepInsightsUiState`ksi. `SessionDetailScreen` näyttää
  Sleep Insights -kortin Sleep Results -kortin jälkeen: unavailable-copy puuttuvalle datalle, quiet-summary aidosti
  hiljaiselle datalle ja notable/loudest-period-yhteenvedon, kun loud period -jaksoja löytyy.

### 2026-06-25 - Audible alarm policy

- `domain/noise/AudibleAlarmPolicy` omistaa audible alarm -oletukset: 90 dB threshold, 30 s yhtäjaksoinen kesto ja
  5 min cooldown. Malli on puhdasta domain-koodia eikä sisällä Android audio-, notification- tai service-riippuvuuksia.
- `AudibleAlarmEvaluator` on stateful domain-evaluator, joka palauttaa `BelowThreshold`, `Waiting`, `CoolingDown` tai
  `Trigger` -päätöksen. Thresholdin alitus resetoi kestoikkunan, ja cooldownin päätyttyä vaaditaan uusi yhtäjaksoinen
  duration-ikkuna ennen seuraavaa triggeriä.

### 2026-06-25 - Audible alarm playback

- `audible_alarm` on Pro-gatettu DataStore-asetus, jonka default on OFF. Settingsin Noise Notifications -kortti näyttää
  toggle- ja preview-polun vain effective Pro -tilassa; Free-käyttäjän effective tila pysyy OFF eikä ViewModel kirjoita
  enable-arvoa.
- `SoundPoolAudibleAlarmPlayer` omistaa bundled `res/raw/audible_alarm.wav` -äänen toiston. Se käyttää
  `AudioAttributes.USAGE_ALARM`- ja `CONTENT_TYPE_SONIFICATION` -attribuutteja eikä pyydä erillistä audiofocusta.
- `AudibleAlarmPlaybackController` yhdistää `AudibleAlarmEvaluator`in Android playback-porttiin. `AudioSessionManager`
  välittää live weighted dB -lukemat controllerille ja käynnistää guard-monitoroinnin session ajaksi vain runtime
  effective `isProUser && audibleAlarmEnabled` -ehdolla.
- `AndroidAudibleAlarmPlaybackGuard` estää alarm-toiston, jos näyttö ei ole interactive-tilassa tai proximity-sensori
  on peitetty. Guard pysäytetään stop-, failure-, cleanup- ja completion-polkujen yhteydessä.

### 2026-06-25 - Voice baseline

- `domain/voice/VoiceBaselineCalibrator` aggregoi vain YAMNetin `Speech`-luokittelemien live-jaksojen weighted dB
  -lukemat energiapohjaiseksi keskiarvoksi. Se ei näe eikä tallenna PCM-bufferia, YAMNet-windowia tai muuta
  raakaaudiota.
- `AudioSessionManager.captureVoiceBaseline(...)` on baseline-capturen runtime-portti. Se palauttaa arvon vain Pro-
  käyttäjälle, käynnissä olevan mittauksen aikana ja kun Sound Detection on effective runtime -tilassa päällä.
- DataStore-avainkolmikko `voice_baseline_level_db`, `voice_baseline_sample_count` ja
  `voice_baseline_captured_at_ms` on baseline-persistoinnin ainoa lähde. Room-skeemaa ei muuteta voice baselinea varten.
- Settingsin Display & Features -osio näyttää Pro-gatetun Voice Baseline -kortin. Tallennuspainike on käytössä vain
  käynnissä olevassa Sound Detection -mittauksessa.

### 2026-06-26 - Voice volume warnings

- `domain/voice/VoiceVolumeWarningEvaluator` on voice warning -päätösten ainoa domain-lähde. Se vaatii tallennetun
  voice-baselinen, nykyisen `Speech`-luokituksen, baseline + 8 dB -ylityksen 3 sekunnin ajaksi ja 60 sekunnin
  cooldownin ennen seuraavaa triggeriä.
- `AudioSessionManager` syöttää evaluatorille samat Sound Detection -luokitukset kuin Voice Baseline -polulle ja
  välittää live weighted dB -lukemat vain effective Pro + Sound Detection + valid baseline -tilassa.
- Trigger dispatchaa best-effort haptic-palautteen `HapticFeedbackHelper.mediumClick()` -polulla ja
  `NotificationHelper.sendVoiceVolumeWarning(...)` -notificationin nykyiselle alerts-kanavalle. Notificationilla on oma
  ID eikä se ylikirjoita measurement-, exposure- tai peak-notificationeita.
- Voice volume warnings ei lisää raakaaudion tallennusta, uutta Room-skeemaa tai background microphone -polkua.

### 2026-06-26 - TTS risk prompt

- `tts_risk_prompt` on Pro-gatettu DataStore-opt-in ja default OFF. Settingsin Noise Notifications -osio näyttää
  Spoken risk prompt -kytkimen Pro-käyttäjälle; Free-käyttäjän effective tila pysyy OFF eikä ViewModel kirjoita enableä.
- `domain/voice/TtsRiskPromptEvaluator` on TTS-riskipromptin päätöslähde. Se hyväksyy vain dosimeter-pohjaiset
  `DOSE`/`PROJECTED_DOSE` -riskieventit, vaatii Sound Detection -saatavuuden sekä latest hearing-test-baselinen ja
  käyttää 30 minuutin cooldownia.
- `AudioSessionManager` seuraa `HearingTestRepository.getLatestResult()` -polkua session aikana vain baseline-
  olemassaolon booleanina. Se ei kirjoita hearing-test-dataa eikä käynnistä uutta mittaus- tai tallennuspolkua.
- `TtsRiskPromptController` välittää hyväksytyn triggerin `TtsPromptPlayer`-porttiin. Tuotantototeutus
  `AndroidTextToSpeechPlayer` käyttää Android `TextToSpeech` -APIa `QUEUE_FLUSH`-toistolla ja manifestin
  `android.intent.action.TTS_SERVICE` -queryllä Android 11+ package visibilityä varten.
- Spoken risk prompt -copy ei tee diagnoosi-, kuulovaurio-, pysyvyys- tai turvallisuusväitteitä. Polku ei persistoi
  raakaaudiota, PCM-bufferia, YAMNet-windowia, TTS-utterancea tai uutta Room-dataa.

### 2026-06-27 - TTS short hearing recovery check

- `HearingTestMode.RECOVERY` on lyhyt Pro-gatettu hearing-check moodi TTS-riskipromptin jatkoksi. Se käyttää samaa
  `HearingTestProcedure` / `ActiveTestViewModel` / `HearingTestActiveScreen` -polkua kuin full hearing test, mutta
  `HearingTestPolicy.RECOVERY_CHECK_FREQUENCIES` rajaa testin 1 kHz, 4 kHz ja 8 kHz -pisteisiin molemmille korville.
- `HearingRecoveryService` vaatii Pro-oikeuden ja latest full hearing-test-baselinen. Se ei luo uutta full
  hearing-test-tulosta, vaan laskee `HearingRecoveryCalculator`illa matching ear/frequency -threshold-deltat baselineen
  verrattuna ja tallentaa aggregate-tuloksen `HearingRecoveryRepository`n kautta.
- Room schema v12 lisää `hearing_recovery_results`-taulun: baseline-testin FK, timestamp, tested count, average/max
  shift, status ja left/right shift data. Taulu ei sisällä raakaaudiota, PCM-bufferia, YAMNet-windowia eikä kliinistä
  audiometriadataa. V12 identity hash on mukana `BackupDatabaseValidator`in sallituissa hasheissa.
- Analytics Overview näyttää `HearingRecoveryCard`in. Missing-baseline-tila ohjaa full hearing testiin, ready/result-tila
  avaa `hearing_test/recovery/setup` -> `hearing_test/recovery/active` -polun, ja Free-käyttäjä saa locked-previewn ilman
  recovery-dataa. Copy pysyy personal tracking -tasolla eikä tee diagnoosi-, kuulovaurio- tai turvallisuusväitteitä.

### 2026-06-28 - Tinnitus planning gate

- Tinnitus ei kuulu v1.0-releaseen. Osa 91 saa edetä aikaisintaan v1.5-tason personal tracking -pitch profileksi:
  käyttäjän itse käynnistämä ToneGenerator-pohjainen pitch matching, ear-specific profiili ja playback limits.
- Osa 91 ei saa sisältää diagnoosia, hoitoa, oireiden vähentämis-/parantamisväitteitä, kuulovaurio- tai
  turvallisuusväitteitä, Health Connect -kirjausta, background playbackia, sound therapyä tai automaattisia triggereitä.
- Vanha Osa 92 sound therapy -scope on yhä pois rajauksesta: ei diagnoosia, hoitoa, relief/cure/safety-copya,
  oireseurantaa, Health Connect -kirjausta tai automaattisia triggereitä.
- Osa 92 saa toteutua vain erillisellä rajatulla ambient sound playback -päätöksellä: Pro-gatettu, käyttäjän Play-
  toiminnosta käynnistyvä paikallinen taustaäänen toisto näkyvällä Stop-kontrollilla.
- Ennen tinnitus-ominaisuuden julkaisua tarkista Google Playn health content / user data -vaatimukset, health disclaimer
  / declaration -tarve ja FDA:n device software -käyttötarkoitusrajaus.

### 2026-06-28 - Tinnitus pitch matcher

- `domain/tinnitus/TinnitusPitchProfile` tallentaa vasemman ja oikean korvan pitch-arvot sekä optional päivitysajan.
  `TinnitusPitchPolicy` normalisoi arvot nykyisen hearing-test-taajuusalueen 250-8000 Hz sisään 50 Hz stepillä ja pitää
  preview-amplitudin kiinteänä -36 dB:nä.
- DataStore-avaimet ovat `tinnitus_left_pitch_hz`, `tinnitus_right_pitch_hz` ja `tinnitus_pitch_updated_at_ms`. Room-
  skeemaa ei muutettu. `PreferencesRepository.updateTinnitusPitchProfile(...)` on ainoa UI-facing write-portti.
- Analytics Overview näyttää `TinnitusPitchCard`in, joka avaa non-top-level `tinnitus/pitch` -reitin. Free-käyttäjän
  effective profiili on tyhjä/locked, eikä `TinnitusPitchMatcherViewModel` previewaa tai tallenna profiilia ilman
  Pro-oikeutta.
- Pitch matcher käyttää olemassa olevaa `ToneGenerator`ia vain käyttäjän painamasta Preview-toiminnosta. Toteutus ei
  lisää diagnosis/treatment-copya, background playbackia, foreground serviceä, media notificationia, sound therapyä,
  Health Connect -kirjausta, raakaaudiota tai automaattisia triggereitä.

### 2026-06-28 - Ambient sound playback

- `domain/ambient/AmbientSoundPolicy` omistaa Osa 92:n local-only asetukset: presetit `WHITE_NOISE`, `PINK_NOISE`,
  `BROWN_NOISE`, `FAN`, volume clamp `0.05f..1.0f` defaultilla `0.35f` sekä timer-vaihtoehdot `0/15/30/60/120`
  defaultilla `30`.
- DataStore-avaimet ovat `ambient_sound_preset`, `ambient_sound_volume` ja `ambient_sound_timer_minutes`. Room-skeemaa,
  playback-historiaa, raakaaudiota, pilvisynkkaa tai Health Connect -kirjausta ei lisätä.
- `AmbientSoundPlaybackService` on erillinen `mediaPlayback` foreground service, jolla on oma
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` -manifest permission ja oma low-importance notification channel.
  `MeasurementForegroundService` ja mikrofonityypit eivät kuulu tähän polkuun.
- `AmbientSoundPlayer` generoi white/pink/brown/fan PCM16-äänen paikallisesti `AudioTrack.MODE_STREAM` -toistoon
  `USAGE_MEDIA` / `CONTENT_TYPE_MUSIC` -attribuuteilla. Audio focus pyydetään startissa; permanent loss pysäyttää ja
  transient loss pausettaa, jonka jälkeen focus gain jatkaa vain service-paussista.
- `AmbientSoundPlaybackViewModel` on execution gate: Free-käyttäjä ei voi käynnistää eikä tallentaa ambient-asetuksia,
  Android 13+ notification-luvan denial estää Play-toiminnon, ja sleep timer vain pysäyttää jo käyttäjän käynnistämän
  playbackin.
- Analytics Overview näyttää Pro-gatetun `AmbientSoundCard`in tinnitus pitch -kortin lähellä ja avaa non-top-level
  `ambient/playback` -reitin. Copy käyttää ambient/local playback -termejä eikä sisällä therapy-, treatment-, relief-,
  cure-, safety- tai hearing-protection-väitteitä.

### 2026-06-29 - Release-readiness QA and report outputs

- Osa 93 accessibility audit is source- and preview-contract level: Meter controls expose button roles, SessionCard edit
  target stays 48 dp, ambient selection chips expose selected state, and large-font screenshot references cover the
  corrected surfaces. Full manual TalkBack/device sign-off remains a release QA activity.
- Osa 94 localization baseline is default English plus a first `values-fi/strings.xml` launch subset for Osa 89-92
  surfaces and shared action/accessibility strings. `LocalizationBaselineTest` guards placeholder parity and scans new
  UI surfaces for hardcoded user-facing Compose text; full-app localization is not complete.
- Osa 95-98 release-readiness evidence lives in `docs/qa/permission-device-qa-matrix.md`,
  `docs/qa/billing-production-qa.md`, `docs/qa/release-signing-qa.md`, and
  `docs/qa/qodana-ci-compatibility.md`. Device smoke, Play Console product verification, signed Play-ready AAB
  verification, Play upload, and real Qodana execution are documented as release risks when not run.
- Qodana remains non-blocking until a real Qodana run proves AGP 9.1.0 compatibility. The workflow job name is
  `Qodana Analysis (non-blocking AGP 9.1 risk)`, `continue-on-error: true` remains intentional, and the workflow summary
  records the risk.
- Current `sc` code-security outputs are split across `security-summary.txt`, `semgrep-kotlin.txt`,
  `semgrep-secrets.txt`, `gitleaks.txt`, and `trufflehog.txt`, with dependency outputs in `security-deps.txt`,
  `security-deps-raw.txt`, and `osv.txt`. `security-code.txt` is documentation drift if it appears in active guidance.

### 2026-06-26 - Passive monitoring foreground sample

- Passive monitoring on vain käyttäjän Settingsistä käynnistämä lyhyt foreground-service sample. Settingsin Noise
  Notifications -kortti näyttää disclosure-copyt, pyytää mikrofoniluvan käyttäjätoiminnolla ja käynnistää
  `MeasurementForegroundService.startPassiveMonitoringIntent(...)` -polun.
- `MeasurementRecordingMode.Passive` käyttää samaa `foregroundServiceType="microphone"` -palvelua ja ongoing
  notificationia kuin Meter/Sleep, mutta stop kutsuu `PassiveMonitoringManager.stopMonitoring()` -polkua eikä emittoi
  valmistunutta sessiota.
- `PassiveMonitoringManager` ei kutsu `AudioSessionManager.startSession()`ia, ei luo `SessionEntity`a eikä kirjoita
  `measurements`-rivejä. Se kytkee Sound Detectionin ja spectral-laskennan pois, ei käynnistä WAV-kirjoitusta,
  audible alarmia, voice warningia tai noise alert -triggereitä ja persistoi vain aggregate-samplen.
- Room schema v11 lisää `passive_monitoring_samples` -taulun aggregate-kentille: alku/loppu, reading count,
  min/avg/max/peak ja `totalEnergy`. `PassiveMonitoringRepository.observeDailySummary(...)` tuottaa Settingsin daily
  summaryn, ja Clear history poistaa myös passive monitoring -summaryt.
- Kielletty ilman uutta eksplisiittistä päätöstä: bootista, ajastimesta, receiveristä, WorkManagerista tai muusta
  taustatriggeristä alkava mikrofonisampling; piilotettu/passive always-on -kuuntelu; raakaaudion, PCM-bufferien tai
  YAMNet-windowien persistointi.

### 2026-06-24 - Noise notification schedule model

- `domain/noise/NoiseNotificationSchedule` on notificationien active day/hour -aikaikkunan ainoa domain-lähde. Malli
  käyttää `java.time.DayOfWeek` -arvoja, start/end minute-of-day -arvoja ja `isActiveAt(ZonedDateTime)` -logiikkaa ilman
  UI- tai Android notification -riippuvuutta.
- Sama start- ja end-minuutti tarkoittaa koko valittua päivää. `startMinuteOfDay < endMinuteOfDay` on saman päivän
  ikkuna ja end on eksklusiivinen. `startMinuteOfDay > endMinuteOfDay` ylittää yön; aamuyön osuus kuuluu edellisen
  aktiivisen päivän ikkunaan.
- DataStore-avaimet ovat `notification_schedule_active_days`, `notification_schedule_start_minute` ja
  `notification_schedule_end_minute`. Default on kaikki päivät ja koko päivä. Päivät tallennetaan ISO-8601
  `DayOfWeek.value` -arvoina; tyhjä string tarkoittaa ei aktiivisia päiviä, invalidi ei-tyhjä lista fallbackaa kaikkiin
  päiviin ja minuutit clampataan välille 0..1439.
- Settingsin Noise Notifications -kortti lukee `SettingsUiState.notificationSchedule`-arvon, näyttää aktiiviset päivät
  chip-rivillä ja start/end-tunnit slidereilla sekä kirjoittaa muutokset `NoiseNotificationUpdate.NotificationSchedule`
  -polun kautta `PreferencesRepository.updateNotificationSchedule(...)` -porttiin. UI käyttää tuntivalintaa, mutta
  DataStore/domain-malli säilyttää minuuttiresoluution.
- `NoiseAlertEvaluator` kunnioittaa `NoiseNotificationSchedule`-ikkunaa ennen exposure- tai peak-alertin yritystä.
  `AudioSessionManager` välittää schedule-arvon runtime-alert-preferensseihin ja antaa evaluatorille live
  `LiveExposureState` -dosimeter-arvot.
- Extended exposure alertit voivat laueta 30 minuutin threshold-average-säännöstä, 100 % actual dosesta tai 100 %
  projected dosesta. `NoiseAlertPolicy` omistaa nämä rajat sekä 30 minuutin retry-cooldownin. Jos delivery ei onnistu,
  sama alert-tyyppi yrittää uudelleen cooldownin jälkeen; onnistuneen deliveryn jälkeen se ei spämmää samaa sessiota.

### 2026-05-09 - Waveform style ja refresh rate -dataflow

- `UserPreferenceDefaults` keskittaa preferenssien default-arvot DataStorelle, `UserPreferences`-mallille,
  Settings UI-statelle ja uuden session default-painotukselle.
- `WaveformStyle` ja `MeterRefreshRate` ovat typed preference -enumit `data/local/preferences/model`-paketissa.
  DataStore tallentaa edelleen string-arvot (`default`/`filled`/`bars` ja `high`/`standard`/`low`), mutta UI ja domain
  lukevat typed-arvoja fallbackeilla.
- Settingsin `DisplayAndFeaturesSection` kytkee theme-valinnan, waveform-tyylit Line, Filled ja Bars, refresh rate
  -valinnat High, Standard ja Low power sekä Pro-gatetun lock-screen meter -featurekortin samaan Settings-osioon.
  Refresh rate vaikuttaa vain ruutupäivityksiin, ei mikrofonin sample rateen tai mittausrivien tallennuscadenceen.
- Sama osio omistaa Pro-gatetut feature togglet `technical_metadata`, `dosimeter_card`, `sound_detection` ja
  `sleep_card`. `technical_metadata` nayttaa/piilottaa Meterin Pro-tekniset session info -kentat; `dosimeter_card`
  nayttaa/piilottaa Pro-dosimeter moden ja palauttaa moden DB meter -tilaan, kun toggle ei ole effective paalla.
  `sleep_card` nayttaa Meterin ja Analytics Overview'n Sleep Monitor CTA:n vain effective Pro ON -tilassa.
  `Screen.SleepSetup` / `sleep/setup` on non-top-level route, jonka Free/deep-link execution-polku ohjataan
  Settingsin Pro-korttiin. Pro-käyttäjä voi valmistella 6h/8h/10h target-keston ja keep screen awake -option sekä
  käynnistää Sleep recordingin foreground service -polun kautta.
- `MeterViewModel` lukee `PreferencesRepository.userPreferences`-virtaa ja throttlettaa vain Meterin UI-päivityksiä
  `MeterRefreshRate.uiIntervalMs`-arvolla. Jokainen raw dB -lukema käsitellään edelleen haptiikkaa ja safety-signaaleja
  varten.
- `service/AudioSessionManager` pitää `SessionStats`-tilastot kaikkien raw-lukemien perusteella, mutta
  `service/MeasurementPersistenceSampler` harventaa repositorylle annettavia `SessionMeasurement`-rivejä
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

### 2026-05-25 - Single source of truth ja Room-rajojen tiukennus

- `DbCheckDatabase.DATABASE_NAME` on tietokannan nimen ainoa runtime-lahde.
  `DatabaseModule`, `LocalBackupManager` ja backup-testit eivat kovakoodaa
  `dbcheck.db`-arvoa erikseen.
- `ExportFileCache` omistaa FileProviderin authority-suffixin ja
  `cache/exports/`-polun nimet. Manifestin `${applicationId}.fileprovider`,
  `file_paths.xml`, CSV-export ja PNG-share-polut pidetaan samassa sopimuksessa.
- `HearingTestPolicy` omistaa kuulotestin taajuudet ja tone playback -kestot.
  `HearingRating` omistaa rating-koodit; UI mapittaa ratingin erikseen
  string-resursseihin ja vareihin.
- `NoiseAlertPolicy` omistaa exposure-alertin 30 minuutin keston, 120 dB
  peak-warning-rajan, 100 % dose/projected-dose -alerttirajat ja 30 minuutin
  retry-cooldownin. Settings-copy kayttaa parametroituja string-resursseja,
  joten teksti ei driftaa policy-arvoista.
- `AudioSessionManager`, Session Detail ja widget eivat importtaa Room DAO- tai
  entity-tyyppeja. Mittausjonon domain-malli on `SessionMeasurement`,
  Session Detail lukee `MeasurementRepository.getReportMeasurementsForSession`,
  ja widget lukee viimeisimman session `SessionRepository`n kautta.

### 2026-05-09 - Arkkitehtuurirajojen siivous

- `domain/` ei importtaa enaa `data/`, `sync/`, `service/`, `ui/`, `widget/` tai `billing/`-paketteja eika Android/
  AndroidX-frameworkia. Domain-mallit ovat `domain/session`, `domain/noise`, `domain/hearingtest`, `domain/report`,
  `domain/analytics`, `domain/audio` ja `domain/entitlement` -paketeissa.
- `NoiseLevel.fromDb(...)` lukee luokkarajat enum-arvojen `maxDb`-kentista, jotta 40/70/85 dB -rajat pysyvat yhdessä
  domain-lahteessa.
- `AudioSessionManager`, `MeasurementPersistenceSampler`, `AudioEngine`, `ToneGenerator`, `TfliteSoundClassifier` ja
  `AndroidAudioInputDeviceRouter` ovat sovellus-/Android-orkestrointia `service/`-paketissa. Ne saavat kayttaa
  repositoryja, Health Connectia, widget-paivitysta ja Android audio/API -tyyppeja; `domain/audio` jaa puhtaille audio-
  malleille, porteille ja DSP-logiikalle kuten `DecibelCalculator`, `FrequencyWeightingFilter`, `FFTProcessor`,
  `SpectralAnalyzer`, `SoundClassifier`, `AudioInputDeviceMapper` ja `AudioInputDeviceRouteResolver`.
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
- `domain/audio/DecibelReading` kuljettaa erikseen raw RMS -arvon (`instantDb`), valitulla painotuksella lasketun
  RMS-arvon (`weightedDb`), rinnakkaisen A-painotetun RMS-arvon (`aWeightedDb`) ja C-painotetun peak-arvon (`peakDb`).
  `aWeightedDb` on runtime-dataa dosimeter-polulle eikä korvaa valittua `weightedDb`-arvoa. LCpeak-raportointi,
  peak warningit ja session `peakDb` käyttävät `peakDb`-arvoa, eivät raw RMS -arvoa.
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
- Mittausnotificationien lukitusnäkyvyys keskitetään `NotificationPrivacyPolicy`yn. Live dB -sisältö käyttää
  `VISIBILITY_PUBLIC`-tasoa vain ehdolla Pro + lockscreen meter + `show_lockscreen_meter_publicly`; muuten notification
  pysyy private-tasolla.
- Settingsin Health Connect -kortti tarjoaa Manage-toiminnon Health Connectin hallintanäkymään. Noise sync ja heart rate
  overlay pyytävät edelleen omat suppeat permission-settinsä.

### 2026-06-14 - Session location permission scope

- Session location on optional metadata -ominaisuus, ei mittauksen edellytys.
- Manifestissa on vain foreground approximate `ACCESS_COARSE_LOCATION`; älä lisää `ACCESS_FINE_LOCATION`-,
  `ACCESS_BACKGROUND_LOCATION`- tai foreground service `location` -tyyppiä ilman uutta product/privacy-päätöstä.
- Runtime-lupa pyydetään vasta käyttäjän sijaintitoiminnon yhteydessä. Nykyinen `AndroidSessionLocationCapturePort`
  palauttaa `null`, jos runtime-lupaa ei ole, provider puuttuu tai location API ei anna sijaintia.
- `SessionLocationCapturePort` on service-tason fake-testattava portti. `AudioSessionManager` yrittää tallentaa
  `SessionLocationMetadata`n aktiivisen session luonnin jälkeen ja stopissa vain fallbackina, jos startissa ei saatu
  sijaintia. Capture- tai `SessionRepository.updateSessionLocation(...)` -virhe ei saa kaataa start/stop/completion-
  polkua.
- Privacy-copy: sijainti on valinnainen, approximate-only, session tunnistamista varten; dBcheck ei tee precise locationia,
  background locationia, jatkuvaa seurantaa, advertising-purposea tai analytics-purposea.

### 2026-05-12 - Billing lifecycle recovery

- `BillingManager.queryExistingPurchases()` ei enää pelkästään aseta Pro-tilaa, vaan prosessoi Play Billingin palauttaman
  ostosnapshotin saman ostotuotteen käsittelypolun kautta. `PURCHASED` `dbcheck_pro` acknowledgeataan tarvittaessa myös
  startup-/reconnect-kyselyn jälkeen, mutta kyselypolku ei julkaise käyttäjälle uutta `Completed`-eventtiä jokaisella
  käynnistyksellä.
- `BillingRuntimeGateway.refreshPurchases()` on ostosnapshotin julkinen refresh-portti. `MainActivity.onResume()` kutsuu sitä,
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

- `data/local/preferences/model/ProAudioPreferencePolicy.kt` keskittää Pro-mittausasetusten effective-arvot.
  Free-käyttäjän calibration offset, frequency weighting, response time ja dosimeter standard palautuvat aina
  `UserPreferenceDefaults`-arvoihin, vaikka DataStoressa olisi aiemmin tallennettuja Pro-arvoja.
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

### 2026-06-09 - Dosimeter standard ja laskenta

- `domain/noise/DosimeterStandard.kt` omistaa dosimeter-standardin typed arvot: `NIOSH_REL` on default ja `OSHA_PEL`
  on Pro-käyttäjälle tallennettava vaihtoehto.
- DataStore tallentaa standardin string-avaimeen `dosimeter_standard`; `UserPreferenceDefaults.normalizeDosimeterStandard`
  tekee fallbackin `NIOSH_REL`-arvoon tuntemattomille arvoille.
- `SettingsUiState.dosimeterStandard` ja `SettingsViewModel.updateDosimeterStandard(...)` tuovat standardin
  Settings-polkuun.
- `domain/noise/DosimeterCalculator.kt` laskee NIOSH_REL- ja OSHA_PEL-altistuksen samoilla outputeilla:
  TWA, dose percent, projected dose percent ja remaining exposure time. Completed report lukee nykyiset NIOSH-kentät
  tästä laskurista.
- `AudioSessionManager.liveExposureState` julkaisee aktiivisen session live-dosimeter-tilan jokaisesta
  `DecibelReading.aWeightedDb`-lukemasta. State käyttää effective `DosimeterStandard` -arvoa ja samaa
  `DosimeterCalculator`ia kuin completed report; tämä ei muuta `MeasurementPersistenceSampler`in 1s Room-kadenssia.
- `ui/meter/state/MeasurementMode` omistaa Meterin `DB_METER` / `DOSIMETER` -valinnan. `MeterViewModel.setMeasurementMode`
  vaihtaa vain `MeterUiState.measurementMode`-arvoa eikä käynnistä tai pysäytä foreground-mittauspalvelua.

### 2026-06-11 - Meter active session info bar

- `domain/audio/AudioInputInfo` on `service/AudioEngine`in live input metadata -state. `AudioEngine.audioInputInfo` julkaisee
  kiinteän `AudioProcessingConfig.SAMPLE_RATE` -arvon, effective selected inputin ja `AudioRecord`in routed input
  -nimen vasta, kun `AudioRecord.startRecording()` on onnistunut; state palautuu defaulttiin stop/release-polussa,
  koska Androidin routing-tieto on luotettava vain aktiivisen tallennuksen aikana.
- `domain/audio/AudioInputDevice`, `AudioInputDeviceType`, `AudioInputDeviceMapper` ja
  `AudioInputDeviceRouteResolver` muodostavat external mic -dataflow'n puhtaan domain-osan.
  `service/AndroidAudioInputDeviceDiscoveryPort` ja `service/AndroidAudioInputDeviceRouter` lukevat
  `AudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)` -source-laitteet ja reitittavat Android `AudioRecord`in.
  Pro-käyttäjän `selected_audio_input_device_id` vaikuttaa vain execution-polussa, jossa `AudioSessionManager` välittää
  effective valinnan `AudioEngine.setPreferredAudioInputDeviceId(...)` -metodille ennen starttia; Free-käyttäjän
  effective arvo on null. Jos valittu external input puuttuu, resolver fallbackaa built-in mikrofoniin eikä ylikirjoita
  raw preferenceä.
- `AndroidAudioInputDeviceRouter` kutsuu `AudioRecord.setPreferredDevice(...)` ennen `AudioRecord.startRecording()`-
  kutsua. `sessions`-taulu tallentaa schema v9:ssä valitun/routed inputin nullable metadatan, joka kulkee
  `SessionAudioInputDeviceMetadata` -> `SessionReportData` -polkuun ja PDF Report Contextin Audio input -riviin.
- `MeterViewModel` rakentaa `MeterSessionInfoUiState`n `AudioSessionManager.isRecording` /
  `activeSessionStartTimeMs` -virroista, `ProAudioPreferencePolicy`n effective weighting/response time -arvoista sekä
  `AudioEngine.audioInputInfo`sta.
- `MeterSessionInfoBar` näkyy vain aktiivisen Meter-session aikana. Free-käyttäjä näkee REC-tilan, keston,
  effective weightingin ja response timen; Pro-käyttäjä näkee lisäksi sample raten ja input devicen.
- `MeterScreen` pitää näytön hereillä aktiivisen mittauksen aikana `FLAG_KEEP_SCREEN_ON` -window flagilla yhteisen
  `KeepScreenOnEffect` / `KeepScreenOnController` -polussa. Controller clearataan, kun recording päättyy tai composable
  poistuu kompositiosta. Sleep setup käyttää samaa helperiä vain `isRecording && keepAwakeEnabled` -ehdolla eikä lisää
  erillistä `PowerManager.WakeLock` -manageria.
- `ui/common/ContextActivity.findActivity()` on yhteinen ContextWrapper-purku Activitya tarvitseville Compose-reiteille
  kuten Settings, Camera overlay ja Meter; älä lisää uusia yksityisiä kopioita samaan tarpeeseen.

### 2026-06-11 - Analytics section state

- `ui/analytics/state/AnalyticsSection` omistaa Analyticsin section-valinnan arvot `OVERVIEW`, `SPECTRAL` ja
  `ENVIRONMENT`. `AnalyticsOverviewRange` omistaa Overviewin `WEEKLY` / `MONTHLY` -range-valinnan.
- `AnalyticsViewModel` säilyttää section-, overview range- ja spectral mode -valinnat omissa `MutableStateFlow`
  -lähteissään ja julkaisee ne `AnalyticsUiState.Success.selectedSection`-, `selectedOverviewRange`- ja
  `selectedSpectralMode`-kentissä. `onSectionSelected(...)`, `onOverviewRangeSelected(...)` ja
  `onSpectralModeSelected(...)` päivittävät nykyisen Success-staten heti, ja seuraavat analytics-dataemissiot käyttävät
  samoja valintoja eivätkä palauta niitä oletukseen.
- `AnalyticsSectionChipRow` renderöi section-valinnan Analytics-headerin alle. Free-käyttäjällä Spectral ja Env Mix
  näkyvät lukkoikonilla, eivät piilotettuina, ja ne voivat silti olla valittuina.
- `AnalyticsOverviewRangeChipRow` renderöityy vain Overview-sectionissa. Weekly on Free-käyttäjälle auki; Monthly näkyy
  Free-käyttäjälle Pro-lukittuna, mutta voi silti olla valittuna locked-preview-näkymää varten.
- `SpectralMode` omistaa spektrikortin renderöintimoden arvoilla `BARS`, `SPECTROGRAM` ja `RTA`.
  `SpectralModeChipRow` renderöi kaikki kolme valintaa `SpectralAnalysisCard`in sisällä.
- `SpectrogramBuffer` on `AnalyticsViewModel`in live-only UI-bufferi. Se muodostaa `AudioEngine.spectralFrame`
  -emissioista `SpectrogramUiState`-waterfall-rivit, säilyttää enintään 60 viimeisintä riviä, ohittaa saman timestampin
  uudelleenemissiot ja tyhjentyy, kun käyttäjä ei ole Pro tai live-frame puuttuu.
- `SpectralAnalysisCard` renderöi Bars-, Spectrogram- ja RTA-haarat erikseen. `SpectrogramCanvasModel` omistaa
  spectrogram Canvas-solujen muodostuksen ja locked-preview-rivit. `RtaBarsModel` omistaa RTA Canvas -barit sekä
  PEAK/BANDS-stat pillien arvot. `formatSpectralFrequency(...)` on UI:n yhteinen Hz/kHz-muotoilija.
- `DbCheckChip` tukee nyt valinnaista leading iconia ja säädettävää horizontal paddingia, jotta lukitut chipit voivat
  käyttää samaa design-tokenoitua chip-komponenttia ilman erillistä kopiota.
- `AnalyticsSelectableChip` on Analyticsin lukkoikonia käyttävien chip-rivien yhteinen render-helper.
- `analyticsSectionCards(...)` omistaa Analyticsin section- ja range-kohtaisen korttiryhmittelyn. Overviewin Weekly-range
  renderöi weekly exposure- ja hearing health -kortit, Monthly-range renderöi `MonthlyTrendChart`in, ja yearly report
  sekä hearing-test CTA pysyvät Overviewissa molemmissa rangeissa. Spectral renderöi `SpectralAnalysisCard`in;
  Environment renderöi `EnvironmentMixCard`in. Tämä ei muuta `AnalyticsViewModel`in dataflow'ta tai Pro-gatingia: kaikki
  nykyiset UI-state-kentät rakennetaan edelleen samalla tavalla.

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
  mapittaa flushatut `SessionMeasurement`-domainrivit `MeasurementEntity`-riveiksi ja paivittaa aktiivisen session
  runtime-summaryn samassa Room `withTransaction`-blokissa. `completeSessionWithMeasurements(...)` kirjoittaa viimeiset
  pending-rivit ja sulkee session samassa transaktiossa.
- `AudioSessionManager` tallentaa aktiivisen session luonnissa nykyisen effective frequency weighting -arvon.
  Flush-polku päivittää aktiivisen session `minDb`/`avgDb`/`maxDb`/`peakDb`-arvot, jotta interrupted-session recovery
  voi palauttaa myös LCpeak-summaryn.
- `MeasurementRepository` on nyt read/analytics-repository: mittausrivien write-portit eivät ole siellä, ja
  aggregointien Flow-mappaukset (`groupBy`, energia-average ja domain-projektiot) siirtyvät injektoidulle
  `DefaultDispatcher`ille.
- Room-skeema v8 lisää `calibration_profiles.octaveBandOffsets` TEXT -sarakkeen v7-profiilitaulun (`id`, `name`,
  `micSensitivityOffset`, `isDefault`, `createdAt`, `updatedAt`) rinnalle. `CalibrationProfileRepository` tarjoaa
  UI:sta riippumattomat `createProfile(...)`, `observeProfiles()`, `getProfile(...)`, `renameProfile(...)`,
  `deleteProfile(...)`, `updateOctaveBandOffsets(...)` ja `resetOctaveBandOffsets(...)` -polut, normalisoi flat- ja
  octave-offsetit yhteisella `CalibrationOffsetPolicy`lla ja estää viimeisen `isDefault`-profiilin poiston data-
  kerroksessa. V8:n Room identity hash on lisätty `BackupDatabaseValidator`in sallittuihin hasheihin, jotta uudet
  paikallisbackupit voidaan validoida restore-polussa.
- Room-skeema v9 lisää `sessions.selectedAudioInputDeviceId`, `selectedAudioInputDeviceName` ja
  `routedAudioInputDeviceName` -sarakkeet external mic -raportointia varten. V9:n identity hash
  `5b73e542adc2464266a32a6c3d216e15` on mukana `BackupDatabaseValidator`in sallituissa hasheissa.
- Room-skeema v10 lisää Sleep Monitorin erilliset `sleep_sessions`- ja `sleep_notable_events`-taulut. Sleep metadataa
  ei lisätä tavalliseen `sessions`-tauluun, ja v10:n identity hash `e4c97360fab833b6bc30549ab7e8075f` on mukana
  `BackupDatabaseValidator`in sallituissa hasheissa.
- Valittu calibration profile tallennetaan DataStore-avaimeen `selected_calibration_profile_id`. Arvo normalisoidaan
  positiiviseksi `Long`-ID:ksi tai `null`iksi; varsinainen runtime-kalibroinnin sovellus tulee myöhemmässä osassa.
- Settingsin `AudioCalibrationSection` näyttää calibration profile -hallinnan ProLockOverlayn takana. `SettingsViewModel`
  mapittaa repository-virran `CalibrationProfileUiState`-riveiksi, joihin sisältyvät valitun profiilin octave-band-offsetit
  `OctaveCalibrationBandUiState`-listana. Settings näyttää valitulle profiilille `DbCheckSlider`-bandisäätimet ja
  reset-ikonipainikkeen; update/reset kirjoittaa `CalibrationProfileRepository`n `updateOctaveBandOffsets(...)`- ja
  `resetOctaveBandOffsets(...)` -polkuihin. ViewModel bootstrappaa Pro-käyttäjälle `Device default` -profiilin vasta
  ensimmäisen Room-profiiliemission jälkeen, tallentaa selectin `selected_calibration_profile_id`-avaimeen ja valitsee
  fallback-profiilin, jos nykyinen valinta poistetaan. Free-käyttäjä ei voi
  create/select/rename/delete/update/reset-profiileja ViewModelin kautta.
- Room-skeema v6 lisäsi nullable session location -metadatasarakkeet `sessions.locationLatitude`,
  `locationLongitude`, `locationAccuracyMeters` ja `locationCapturedAt`; `MIGRATION_5_6` ei backfillaa vanhoja rivejä,
  koska location on optional. `SessionDao.updateSessionLocation(...)` ja `SessionRepository.updateSessionLocation(...)`
  päivittävät location-metadatan partial update -polulla erillään measurement/summary-transaktioista. Room-skeema v5 lisäsi
  `sound_detection_events`-taulun aggregoiduille detection-eventeille.
- `SessionHistoryQuery` on Historyn repository-hakumalli. `SessionRepository.getFilteredSessions(...)` säilyttää Free-
  käyttäjän 7 päivän history policy -alarajan, antaa Pro-käyttäjälle koko historian ja mapittaa name/tag/date/avg dB/
  weighting/location-filtterit `SessionDao.searchSessions(...)` -kyselyyn. Query order on aina
  `startTime DESC, id DESC`.
  `MIGRATION_4_5` luo taulun, `sessionId,timestamp`- ja `timestamp`-indeksit sekä `sessions.id`-cascade-viiteavaimen.
  Room v4 lisäsi aiemmin `measurements.aWeightedDb`- ja `measurements.responseTime`-sarakkeet. `MIGRATION_3_4`
  backfillaa vanhoille riveille `aWeightedDb = dbWeighted` ja `responseTime = FAST`, koska vanhassa skeemassa ei ollut
  erillistä A-painotettua tai response-time-rivimetadataa. `MIGRATION_2_3` lisäsi `measurements.peakDb`-sarakkeen ja
  backfillasi sen `dbWeighted`-arvolla.
- Uudet mittausrivit tallentavat selected weighted RMS:n (`dbWeighted`), rinnakkaisen A-weighted RMS:n (`aWeightedDb`),
  C-painotetun LCpeak-arvon (`peakDb`) sekä mittaushetken effective response time -nimen (`responseTime`). CSV ja
  raportin nykyinen LCpeak-polku lukevat rivikohtaisen peak-arvon, mutta 85 dBA peak event -ryhmittely käyttää edelleen
  nykyistä raporttimallia.
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


<claude-mem-context>
# Memory Context

# [dBcheck] recent context, 2026-06-26 1:16pm GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (25,558t read) | 2,422,897t work | 99% savings

### May 8, 2026
S768 Clarifying question about cross-project impact of adding ktlint_code_style to .editorconfig (May 8, 4:43 PM)
S770 Resolve 2039 lint violations blocking commit of Block 12.3 B-weighting implementation (May 8, 4:52 PM)
S767 Diagnose lint-check failures blocking commit of Block 12.3 B-weighting implementation (May 8, 4:52 PM)
S772 Implemented ktlint_code_style configuration fix to resolve lint build failures (May 8, 4:53 PM)
S769 Confirm EditorConfig project isolation before applying ktlint_code_style fix (May 8, 4:58 PM)
S771 Applied ktlint_code_style configuration fix to resolve 2009 indentation violations (May 8, 4:58 PM)
S775 Verified lint-check still failing after corrected ktlint_code_style configuration (May 8, 5:00 PM)
S774 Corrected ktlint_code_style configuration value to valid android identifier (May 8, 5:01 PM)
S773 Applied ktlint configuration fix and investigating why violations persist in old report (May 8, 5:01 PM)
S776 Resolved 2031 lint violations and deciding how to handle remaining 8 pre-existing issues (May 8, 5:11 PM)
### May 9, 2026
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
5458 7:43p 🔵 File and Backup Security Architecture Mapped
5459 7:44p 🔵 Backup Validation and Export Cleanup Mechanisms Confirmed
### May 21, 2026
5492 10:08a 🔄 dBcheck error handling and failure recovery hardening
5493 10:11a 🔵 AudioSessionManager error handling has comprehensive test coverage
5494 10:13a ✅ Test infrastructure enhanced with Robolectric and coverage adjusted
5496 12:46p 🔴 Fixed osv-scanner failure by updating Gradle dependency verification metadata
5497 2:50p ✅ Verified dBcheck security fixes ready for GitHub publish after clean scan results
5498 2:52p ✅ Staged all verified changes including CI security improvements and screenshot test enhancements
5500 " ✅ Verified Kotlin compilation success for all staged changes before commit
5501 2:54p 🔵 Confirmed Android screenshot testing infrastructure with dedicated Gradle tasks and source sets
5502 2:55p ✅ Screenshot tests passed successfully validating all baseline images match rendered UI components
5503 " ✅ Committed CI security improvements and screenshot test enhancements to local branch
5504 " ✅ Pushed verified security fixes and screenshot tests to GitHub origin branch
5506 10:26p 🔴 Fixed CI Build Failures Due to Stale Gradle Verification Metadata and OWASP False Positive
5507 " 🔵 NVD API Key Configuration Found in security-check.ps1 Scripts
**5508** 10:28p 🔴 **Fixed Gradle Dependency Verification Metadata Causing CI Build Failures**
The PR branch codex/fix-deepsec-dependabot had four failing CI checks (lint, release build, CodeQL, Sonar) all sharing the same root cause: Gradle's dependency verification was blocking builds because verification-metadata.xml was out of sync with the dependencies resolved in CI. The fix involved regenerating the metadata file using Gradle's built-in --write-verification-metadata flag across the key CI build paths (lint, assembleRelease, assembleDebug), then carefully patching to preserve legacy entries that the auto-generation removed but lint still needed. All local CI-equivalent builds now pass without the --write-verification-metadata flag, confirming the metadata is correct.
~352t 🛠️ 9,209

5509 " 🔴 Added OWASP Dependency-Check Suppression for Dagger Lint AAR False Positive
5510 " 🔵 NVD API Key Configuration Pattern in Security Check Scripts
**5511** 10:50p ✅ **Added NVD API Key to GitHub Actions Security Workflow**
The OWASP Dependency-Check job in GitHub Actions was taking excessive time (nearly 2 hours) due to NVD database synchronization without an API key. The security.yml workflow was updated to pass the NVD_API_KEY from GitHub Secrets to the dependencyCheckAnalyze task environment. This matches the existing local setup where the key is stored in Windows User environment variables and read by security-check.ps1 wrapper scripts. The repository owner will need to add the NVD_API_KEY to GitHub repository secrets for this to take effect in CI.
~257t 🛠️ 176,664

**5512** " 🔵 **Dagger Lint AAR Suppression Not Preventing Build Failure**
Despite adding a suppression rule to config/dependency-check-suppressions.xml targeting the Dagger lint AAR false positive (where Dependency-Check incorrectly matches it to distribution:distribution and distribution_project:distribution CPEs), the local dependencyCheckAnalyze task still reports these CVEs as active rather than suppressed. The suppression XML correctly references both CPE patterns and uses regex matching for the package URL, yet the vulnerabilities remain un-suppressed in the JSON report. This suggests either a pattern mismatch, incorrect suppression scope, or a requirement to suppress by CVE ID rather than by CPE. The build continues to fail the CVSS threshold check.
~356t 🔍 176,664

### Jun 9, 2026
**5605** 3:52p 🟣 **Live exposure state tracking added to AudioSessionManager**
AudioSessionManager now tracks and publishes real-time dosimeter exposure metrics during active measurement sessions. The new LiveExposureState data class captures the current dosimeter standard, LAeq (computed from A-weighted energy average), elapsed duration, TWA, dose percent, projected 8-hour dose percent, remaining safe exposure time, and sample count. The state is published via a StateFlow that updates on every incoming DecibelReading, using the aWeightedDb field to maintain consistent LAeq calculation independent of the session's configured frequency weighting. DosimeterCalculator is called on each update to derive TWA and dose metrics according to the effective dosimeter standard (NIOSH_REL or OSHA_PEL) from user preferences, respecting Pro gating. The implementation preserves the existing 1-second Room persistence cadence while enabling sub-second live exposure updates for ViewModel consumption. State lifecycle follows session lifecycle: reset on start/stop/reset/cleanup, and excluded from interrupted session recovery. When the dosimeter standard preference changes mid-session, the exposure metrics are recalculated from the current LAeq and duration without discarding accumulated samples. Four new tests verify OSHA_PEL calculation, sub-second update frequency without triggering extra database writes, reset behavior after silent completion, and recovery exclusion.
~635t 🛠️ 95,702

### Jun 26, 2026
**5653** 11:53a 🔵 **dBcheck lint-check reveals 168 detekt formatting and complexity issues**
The dBcheck project's lint-check wrapper (which runs ktlint → detekt → Android lint in sequence) failed with 168 detekt issues. The ktlint task itself didn't run because it depends on detekt passing first. The issues span multiple categories: formatting (FunctionSignature/ClassSignature whitespace expectations), architecture (TooManyFunctions on data layer classes), complexity (cyclomatic complexity, return count, method length), and style (import ordering, max line length, when-condition formatting). The most significant architectural issues are UserPreferencesDataStore and PreferencesRepository both exceeding the 25-function limit with 28 functions each, SettingsViewModel being flagged as LargeClass, and several policy evaluation functions (evaluate, applyPreferredDevice, resolveSelectedAudioInputDeviceId) exceeding the 2-return limit with 3-7 returns. Memory search shows this repo has a history of lint-check summary fixes, indicating ongoing code quality maintenance work.
~487t 🔍 50,685


Access 2423k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
