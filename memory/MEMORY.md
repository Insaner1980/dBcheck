# dBcheck Memory

## 2026-05-25 - Single source of truth ja Room-rajojen tiukennus

- `DbCheckDatabase.DATABASE_NAME` on tietokannan nimen yksi runtime-lahde. `DatabaseModule`, `LocalBackupManager` ja
  backup-testit viittaavat samaan vakioon.
- `ExportFileCache` omistaa FileProviderin authority-suffixin seka `cache/exports/`-polun nimet. Manifest/XML/runtime
  ja testit vertaavat samaa sopimusta.
- `HearingTestPolicy` omistaa kuulotestin taajuuslistan ja tone timing -arvot. `HearingRating` omistaa rating-koodit,
  joita UI mapittaa string-resursseihin ja vareihin.
- `NoiseAlertPolicy` omistaa exposure-alertin 30 minuutin keston ja 120 dB peak-warning-rajan. Settings-copy on
  parametroitu policy-arvoilla.
- `AudioSessionManager`, Session Detail ja widget eivat importtaa Room DAO/entity -tyyppeja. `SessionMeasurement` on
  service->repository mittausrivimalli, `MeasurementRepository.getReportMeasurementsForSession(...)` palauttaa
  report-mallit ja widget lukee viimeisimman session `SessionRepository`n kautta.

## 2026-05-09 - Meter- ja kuulotestitulosten share-dataflow

- `ShareResultsGenerator` keskittaa nakyvat jakosisallot. Meter share palauttaa `text/plain`-intentin nykyisilla
  avg/peak/duration-arvoilla, ja Hearing Test Results share palauttaa PNG-kortin seka saatetekstin.
- Image-share-polku kirjoittaa kuvat cacheen ja jakaa ne FileProviderin `content://`-URIlla. Intenttiin asetetaan
  `EXTRA_STREAM`, `ClipData` ja `FLAG_GRANT_READ_URI_PERMISSION`, jotta vastaanottaja saa valiaikaisen lukuoikeuden.
- `MeterViewModel.createShareIntent()` estaa jakamisen ennen ensimmaista mittaussamplea ja antaa UI-virheen, jos
  generator tai Sharesheet-kaynnistys epaonnistuu.
- `ResultsViewModel.createShareIntent()` jakaa reittiargumentilla ladatun kuulotestituloksen score/rating-arvoilla.
  Results-ruutu kaynnistaa chooserin itse, joten navissa ei ole enaa tyhjaa `onShare`-callbackia.
- `HearingTestActiveScreen` navigoi tuloksiin vasta, kun `ActiveTestViewModel` on saanut
  `HearingTestService.saveCompletedTest(...)`-kutsusta tallennetun tuloksen ID:n. Results-reitti on
  `hearing_test/results/{testId}`, ja `ResultsViewModel` hakee tuloksen `HearingTestRepository.getResultById(...)`
  -polulla `SavedStateHandle`-argumentin perusteella.
- Analyticsin ja Historyn tyhjatilan CTA:t navigoivat Meteriin. Analyticsin ja Historyn ylapalkin actionit navigoivat
  Settingsiin; Settingsissa ei nayteta action-ikonia ilman toimintoa.
- `DurationFormatter.formatClockDuration(...)` on kellomuotoisen keston yhteinen helper Meter sharelle, lockscreen
  notificationille, PDF-raportille ja Session Detailille.

## 2026-05-09 - Pro-oston UI-kytkentä ja entitlement-flow

- `billing/BillingGateway.kt` erottaa Settingsin ostovirran testattavaksi rajapinnaksi. `BillingManager` toteuttaa
  rajapinnan, kysyy `ProductDetails`-datan juuri ennen ostovirtaa ja palauttaa `PurchaseLaunchResult`-tuloksen.
- Billingin käynnistystila on kolmivaiheinen: `BillingManager.isPurchased` alkaa `null`-arvosta, joka tarkoittaa
  "ei vielä varmistettu". `ProFeatureManager` synkkaa DataStoreen vain varmistetun `true`/`false`-tilan, jotta appin
  käynnistys tai epäonnistunut Play Billing -kysely ei ylikirjoita aiemmin tallennettua Pro-oikeutta Free-tilaan.
- `BillingManager.purchaseEvents` välittaa ostotapahtumat Settingsiin arvoilla `Completed`, `Cancelled`,
  `AlreadyOwned` ja `Failed`. Onnistunut ostos asetetaan ostetuksi ja acknowledge tehdään vasta
  `PurchaseState.PURCHASED`-tilassa.
- Pro-oikeuden laskenta on keskitetty `domain/entitlement/ProEntitlementPolicy.kt`-tiedostoon. Release-buildissa Pro määräytyy
  ostotilan mukaan. Debug-buildissa käyttäjä on oletuksena Pro, mutta `debugForceFreeEnabled` pakottaa Free-tilan.
- `UserPreferencesDataStore` tallentaa ostotilan ja debug force-free -asetuksen erikseen. `UserPreferences.isProUser`
  on aina effective entitlement, jota UI ja feature-gatet lukevat.
- `SettingsScreen` käynnistää Google Play Billing -ostovirran omasta Pro-kortistaan ja Settingsissä näkyvistä
  ProLockOverlay-painikkeista. Muiden näyttöjen Upgrade-painikkeet navigoivat edelleen Settingsin Pro-korttiin.
- `DbCheckApplication` injektoi `ProFeatureManager`in, jotta billing-tilan synkkaus DataStoreen käynnistyy sovelluksen
  käynnistyksessä eikä riipu foreground servicestä. Sama entitlement-flow päivittää Glance-widgetit, kun Pro-oikeus
  muuttuu.

## 2026-05-10 - Startup-initialisoinnin siivous

- `MainActivity` odottaa ensimmäistä `UserPreferences`-emissiota ennen `DbCheckTheme`/`DbCheckNavHost`-sisällön
  piirtämistä. `StartupThemeState` erottaa odotustilan ratkaistusta dark/light-teemasta, jotta tallennettu teema ei
  välähdä system-teemanä ensimmäisessä framessa.
- Meter on edelleen navigation graphin start destination, mutta käynnistyksen lupapolitiikka pyytää vain puuttuvan
  mikrofoniluvan. Android 13+ `POST_NOTIFICATIONS` -lupa pyydetään vasta käyttäjän käynnistäessä mittauksen, koska
  foreground service voi käynnistyä ilman lupaa ja notification-prompt on parempi sitoa käyttäjätoimintoon.
- Startup-korjauksille on regressiotestit: `ProFeatureManagerStartupTest`, `MainActivityThemeTest` ja
  `MeterStartupPermissionPolicyTest`.

## 2026-05-09 - Phase 12.1 Health Connect -integraatio

- `sync/HealthConnectManager.kt` on Health Connect -integraation infrastruktuurirajapinta. UI käyttää sen päällä olevaa
  `service/HealthConnectService.kt`-porttia, joka mapittaa statuksen, permissionit ja sykearvot service-malleiksi.
- Health Connect 1.1.0 stable on kaytossa. Androidin nykyisessa Health Connect -datamallissa ei ole natiivia
  melualtistus- tai audiometriadatarecordia, joten melu kirjataan `EXERCISE_TYPE_OTHER_WORKOUT`-sessiona
  `Metadata.clientRecordId`-tunnisteella `noise_dose_<date>_session_<id>`. Metadata on `activelyRecorded`, koska
  mittaus kaynnistyy kayttajan toiminnolla, ja notes lukee `SessionReportData`sta equivalent-level-labelin ja arvon,
  maxin, LCpeakin seka painotuksen naytettavan labelin. Kuulotestin Health Connect -kirjoitus on tietoinen no-op,
  kunnes tuettu audiometriatyyppi tai FHIR-polku suunnitellaan erikseen.
- `SettingsScreen` sisaltaa `HealthSyncSection`-osion. Free-kayttaja voi sallia Health Connect -melusynkkauksen, ja
  Pro-kayttaja voi sallia erillisen heart rate overlayn, joka pyytaa vain `READ_HEART_RATE`-permissionin.
- Health Connectin exportatut manifest-entrypointit ovat vain privacy/disclosure-kayttoon:
  `HealthConnectPermissionsRationaleActivity` ja `HealthConnectPermissionUsageActivity` targetoivat
  `HealthConnectPermissionDisclosureActivity`a. Disclosure-activity nayttaa staattisen tekstin eika kayta MainActivityn
  navigaatiota, billing-refreshia, Settings-toimintoja tai dataa muuttavia polkuja.
- `AudioSessionManager.stopSession()` kutsuu `HealthConnectManager.writeNoiseDose(...)`, jos `healthConnectEnabled` on
  paalla. Ennen kirjoitusta se rakentaa `SessionReportCalculator`illa raportin flushatuista mittausriveista ja antaa
  saman `SessionReportData`n Health Connect -adapterille, joten notesin equivalent-level, max, LCpeak ja weighting-label
  tulevat samasta raporttimallista kuin PDF/PNG/Session Detail. Kirjoituksen `Failed`-tulos emittoidaan
  `AudioSessionManager.healthConnectSyncFailures`-virtaan ja Meter UI nayttaa sen virheviestina ilman, etta valmis
  sessio- ja navigointivirta blokkaantuu.
- Session Detail lukee sykearvot `HealthConnectService`-portin kautta, kun kayttaja on Pro ja heart rate overlay on paalla.
  `ui/analytics/components/HeartRateOverlay.kt` piirtaa sykedatan time-series-korttiin.

## 2026-05-09 - Phase 12.4 Session Detail + tieteellinen PDF-raportti

- Session Detail lisattiin reitiksi `history/detail/{sessionId}`. History-kortit ja valmistuneet Meter-mittaukset
  navigoivat samaan reittiin.
- `AudioSessionManager` emittoi valmistuneen session id:n `completedSessionIds`-virtaan; `MeterViewModel` muuntaa sen
  kertakayttoiseksi navigointitilaksi.
- `SessionReportCalculator` on tieteellisen raporttidatan lahde: LAeq, LCpeak,
  time-series-pisteet ja 85 dBA peak event -jaksot. LAeq-energia-average kulkee yhteisen
  `domain/noise/DecibelMath.energyAverageDb(...)`-helperin kautta, jota myos analytiikkalaskenta kayttaa.
- `domain/noise/DosimeterCalculator.kt` on dosimeter-altistuksen yhteinen laskuri NIOSH_REL- ja OSHA_PEL-standardeille.
  Se palauttaa TWA-, dose-, projected dose- ja remaining exposure time -arvot; completed report kayttaa sita
  nykyisiin NIOSH_REL TWA/dose -kenttiin ja live flow kytketaan samaan laskuriin.
- NIOSH 8h TWA, NIOSH dose ja 85 dBA peak event -lista vaativat A-painotetun session.
  `SessionReportData.aWeightedExposureMetricsAvailable` ohjaa Detail/PDF/PNG-outputtia; muilla painotuksilla TWA/dose
  ovat `null`, peak event -lista on tyhja ja output nayttaa arvon puuttuvana.
- `util/ExportPdfReportUseCase` kirjoittaa viisivuisen natiivin Android `PdfDocument`-raportin kayttajan valitsemaan
  `Uri`:in, jonka Compose saa `ActivityResultContracts.CreateDocument("application/pdf")`-contractilta. Kun Session
  Detailin Health Connect -sykeoverlay on aktiivinen, `ReportHeartRateSection` valittaa samat sykepisteet PDF:lle ja
  raporttiin lisataan kuudes Heart Rate -sivu.
- PDF:n Report Context -blokki nayttaa app-version, Android-laitetiedon, persisted response time -summaroinnin,
  export-hetken effective calibration offsetin ja disclaimerin. Response time kulkee
  `MeasurementRepository` -> `ReportMeasurement` -> `SessionReportData.responseTimeSummary` -polkua. Kalibrointioffset
  on export-metadataa eika viela historiallinen session field ennen upstream-persistointia.
- PDF:n sivut ovat summary, metrics, data availability, time series,
  peak events ja optional heart rate.
- PDF:n Data Availability -sivu nayttaa vain valmiin upstream-datan: session locationin, A-painotetun completed-reportin
  NIOSH-standardin, projected dosen ja persisted sound detection -eventeista koostetun sound type -yhteenvedon.
  Octave breakdown pysyy N/A-tilassa, ellei `SessionReportData.octaveBreakdownAvailable` tai non-zero
  `octaveCalibrationOffsets` kerro saatavasta octave-kontekstista. RTA time-series -dataa ei viela persistöidä.
  Puuttuvat upstream-lahteet naytetaan N/A-tekstina eika nollina.
- `PdfChartRenderer` keskittaa kaavion koordinaattimuunnoksen seka PDF Canvas -renderointiin etta staattiseen
  Compose-kaavioon.
- `ShareResultsGenerator` generoi nyt Session Detailin PNG-jakokortin.
- Health Connect -sykekayra on kytketty Phase 12.1:ssa Session Detailin time-series-korttiin.

## 2026-05-09 - Session metadata, tagit ja export-dataflow

- `domain/session/SessionMetadata.kt` on session nimen, emojin, tagien ja export-slugien keskitetty normalisointipiste.
  Tagit tallennetaan edelleen nykyiseen `sessions.tags`-tekstikenttaan pilkuilla eroteltuna, mutta custom-tagin pilkut
  korvataan valilyonneilla ennen tallennusta.
- `SessionDao.updateSessionMetadata(...)` paivittaa vain nimen, emojin ja tagit. `AudioSessionManager.stopSession()`
  kayttaa nyt `completeSession(...)`-partial updatea, jotta session lopetus ei tyhjenna metadataa.
- History ja Session Detail avaavat saman `SessionNamingSheet`-komponentin Pro-kayttajalle. Free-kayttajan
  edit/lock-toiminto vie Settingsin Pro-ostovirtaan.
- `SessionReportData` kantaa custom-nimen, emojin ja tagit. Session Detail, PDF-title block, PDF-tiedostonimi ja
  Session Detailin PNG-jakokortti lukevat metadataa samasta raporttimallista.
- `data/export/ExportCsvUseCase` jakaa nyt kolme CSV-tiedostoa samalla Sharesheetilla: session summary CSV:n,
  measurement CSV:n ja optional sound detection -event CSV:n. Kaikissa on session metadata, ja CSV escaping on
  `CsvEscaper`-helperissa.
- CSV-export kirjoittaa tiedostot streamina `ExportFileCache`n cache-polkuun ja hakee mittausrivit per sessio sivuina
  `MeasurementDao.getMeasurementsForSessionExportPage(...)`-polulla, joten export ei rakenna koko measurement-aineistoa
  yhdeksi muistissa olevaksi merkkijonoksi eikä käytä yhtä isoa `IN (:sessionIds)` -kyselyä.
  Sound detection -eventit haetaan samoin sivuina `SoundDetectionEventDao.getEventsForSessionExportPage(...)`-polulla.
- `CsvExportSelection` tukee all-sessions- ja selected-session-id -batch-exportia. Settingsin CSV-painike käyttää
  all-sessions-polun, mutta valittujen sessioiden batch-export käyttää samaa streaming/FileProvider/ClipData-sopimusta.
- Settingsin `DataExportSection` on CSV-viennin UI-kytkentä. Free-käyttäjä näkee ProLockOverlay-previewn, Pro-käyttäjän
  `Export CSV` -painike pyytää `SettingsViewModel.createCsvExportIntent()`-metodia muodostamaan share-intentin ja avaa
  Android Sharesheetin.
- Settingsin Clear history -kortti on Free- ja Pro-käyttäjille sallittu datanhallintatoiminto. Se vaatii
  vahvistusdialogin, estyy aktiivisen mittauksen aikana ja kutsuu `HistoryClearService.clearHistory()` -polkua.
  `SessionRepository.clearInactiveHistory()` poistaa inactive-sessiot Room-transactionissa; measurements- ja
  sound_detection_events-rivit poistuvat foreign-key cascaden kautta. `WavRecordingFileStore` poistaa vain poistettujen
  sessioiden WAV-tiedostot, eika clear history koske `filesDir/backups`-paikallisbackuppeja.

## 2026-05-09 - Paikallinen backup UI ja restore-flow

- `sync/BackupGateway.kt` on backup-infrastruktuurin testattava rajapinta. Settings käyttää `service/BackupService.kt`
  -porttia, ja `LocalBackupManager` toteuttaa vain paikalliset `filesDir/backups`-tietokantakopiot.
- Backupin luonti ajaa Roomille WAL checkpointin ja kopioi `dbcheck.db`-tiedoston ilman, että `DbCheckDatabase`-singleton
  suljetaan. Restore validoi valitun backupin, tekee `dbcheck_pre_restore_*`-turvakopion, sulkee Roomin, poistaa
  `dbcheck.db-wal`/`dbcheck.db-shm`-sivut ja korvaa tietokannan backupilla.
- Onnistunut restore emittoi `SettingsEvent.RestartAfterRestore`-eventin. `MainActivity` ajastaa puhtaan
  uudelleenkäynnistyksen ja lopettaa nykyisen prosessin, jotta suljettua Room-instanssia ei käytetä samassa prosessissa.
- `DataExportSection` näyttää Local backups -kortin CSV-viennin rinnalla. Paikallinen backup/restore on Free-käyttäjillekin
  sallittu dataturvatoiminto, mutta CSV-vienti pysyy ProLockOverlayn takana.
- `SettingsViewModel` estää backupin ja restoren aktiivisen mittauksen aikana viestillä
  `Stop recording before managing backups`, jotta keskeneräistä mittaussessiota ei kopioida tai korvata.

## 2026-05-09 - Live-spektrianalyysi Analyticsiin

- `AudioProcessingConfig` keskittää audio-domainin sample rate- ja chunk-koot: 44.1 kHz, 4096 samplea ja 4096 point FFT.
- `SpectralAnalyzer` käyttää `FFTProcessor`ia ja muodostaa raw PCM16 -chunkista 24 logaritmista 20 Hz-20 kHz bandia,
  dominanttitaajuuden sekä `SpectralBandwidth`-luokan.
- `FFTProcessor.binFrequency(...)` on FFT-binien taajuusmuunnoksen yhteinen lähde. `SpectralAnalyzer` ja
  `OctaveBandRtaCalculator` käyttävät samaa helperiä, jotta bin-taajuudet eivät driftää eri analytiikkapolkujen välillä.
- `OctaveBandRtaCalculator` tuottaa domain-tason octave/third-octave RTA-datan nykyisen `FFTProcessor`in magnitudi-
  spektristä. Se käyttää IEC/ANSI base-10-kaavaa keskitaajuuksiin ja band edgeihin, aggregoi bandin FFT-magnitudit,
  voi lukea `OctaveCalibrationOffsets`-mallin octave-resoluutiolle ja normalisoi amplitudit vahvimpaan kalibroituun
  RTA-bandiin. AudioEngine käyttää toistaiseksi zero-offset-oletusta, kunnes runtime-kytkentä valittuun profiiliin on
  valmis.
- `AudioEngine.spectralFrame` ja `AudioEngine.rtaFrame` ovat live-only-tilavirtoja. Ne päivittyvät vain, kun
  `setSpectralAnalysisEnabled(true)` on voimassa, tyhjenevät stopissa tai Pro-gaten poistuessa, eikä RTA-dataa
  persistöidä Roomiin.
- `AudioSessionManager` lukee `UserPreferences.isProUser`-arvon preference-collectorissa ja ohjaa spektrilaskennan päälle
  vain Pro-käyttäjälle. Meterin käynnistyspolku ei muutu.
- `AnalyticsViewModel` yhdistää historiadatan, recording-tilan, Pro-oikeuden ja `spectralFrame`-virran.
  Jos historiadataa ei vielä ole mutta mittaus on käynnissä, Analytics palauttaa `Success`-tilan live-korttia varten.
- `SpectralAnalysisCard` lukee `SpectralAnalysisUiState`-tilaa. Free-käyttäjälle annetaan staattinen locked-preview eikä
  oikeaa live-framea välitetä UI:lle. `MeasurementEntity.frequencyData` jää edelleen persistointia varten käyttämättä.

## 2026-05-09 - Waveform style ja refresh rate kytketty

- `UserPreferenceDefaults` keskittaa preferenssien default-arvot DataStorelle, `UserPreferences`-mallille,
  Settings UI-statelle ja uuden session default-painotukselle.
- `WaveformStyle` (`LINE`, `FILLED`, `BARS`) ja `MeterRefreshRate` (`HIGH`, `STANDARD`, `LOW`) ovat typed preference
  -enumit. DataStore käyttää edelleen string-avaimia, mutta palauttaa fallbackeilla typed-arvot UI/domain-kerroksille.
- Settingsin `DisplayAndFeaturesSection` tarjoaa theme-, waveform style- ja refresh rate -chipit sekä Pro-gatetun
  lock-screen meter -featurekortin samassa osiossa. Refresh rate vaikuttaa vain UI-päivityksiin, ei mikrofonin sample
  rateen tai mittausrivien tallennuscadenceen.
- Sama osio tarjoaa Pro-gatetut feature togglet `technical_metadata`, `dosimeter_card`, `sound_detection` ja
  `sleep_card`. Free-käyttäjän Settings-state näyttää ne effective OFF -tilassa, eikä ViewModel anna Free-käyttäjän
  enabloida niitä. `technical_metadata` ohjaa Meterin Pro-teknisiä session info -kenttiä, `dosimeter_card` ohjaa
  Meterin Pro-dosimeter modea/korttia, `sound_detection` käyttää samaa DataStore-avainta kuin
  `AudioSessionManager`in inference-gate ja Analytics-kortin näkyvyys, ja `sleep_card` on persisted visibility-asetus
  tuleville Sleep Monitor -pinnoille.
- `MeterViewModel` throttlettaa `currentDb`-, `noiseLevel`- ja `waveformData`-UI-päivityksiä
  `MeterRefreshRate.uiIntervalMs`-arvolla, mutta käsittelee jokaisen raw-lukeman haptiikkaa ja threshold-signaaleja varten.
- `service/AudioSessionManager` päivittää `SessionStats`-arvot jokaisesta raw-lukemasta. Room-persistointi kulkee
  `service/MeasurementPersistenceSampler`in kautta, joka tallentaa kiinteällä 1s cadencella refresh rate -asetuksesta
  riippumatta sekä pakottaa talteen ensimmäisen lukeman, `NoiseLevel.ELEVATED.maxDb` threshold-crossingit, uudet session
  maxit ja stopin viimeisen tallentamattoman lukeman.
- `AudioSessionManager` ei sisällytä `refreshRate`-arvoa runtime-audio-preferensseihin, joten refresh-only muutos ei
  kutsu `AudioEngine.setWeighting(...)`-polkua eikä resetoi painotusfiltterin tilaa kesken session.
- `AudioEngine` ja `AudioProcessingConfig` pysyivät muuttumattomina: 44.1 kHz sample rate, 4096 sample chunk,
  painotusfiltterit ja FFT-koko eivät riipu `refreshRate`-asetuksesta.

## 2026-05-09 - Environment Mix Analytics oikeaan dataan

- `MeasurementDao.getEnvironmentMixCounts(...)` aggregoi viimeisen 7 päivän `measurements.dbWeighted`-samplet Quiet,
  Moderate, Loud ja Critical -luokkiin sekä palauttaa `totalCount`-arvon Room-projektiona.
- `MeasurementRepository.getEnvironmentMixLast7Days()` keskittää Environment Mix -hakupolun ja mapittaa DAO-projektion
  `domain/analytics/EnvironmentExposureMixCounts`-malliksi. Luokkarajat luetaan `NoiseLevel`-mallista.
- `AnalyticsViewModel` yhdistää Environment Mix -countit weekly analytics -flow'hun ja mapittaa ne
  `EnvironmentMixUiState`-tilaksi. Free-käyttäjä saa vain `LockedPreview`-tilan, eikä oikeaa dataa välitetä lukon alle.
- Pro-käyttäjän prosentit lasketaan sample-counttien perusteella ja pyöristetään largest remainder -mallilla niin, että
  näkyvien rivien summa on aina 100, kun `totalCount > 0`.

## 2026-05-09 - MonthlyTrendChart ja YearlyReportCard oikeaan dataan

- `domain/analytics/ExposureAnalyticsCalculator.kt` keskittää 30 päivän trendin ja 12 kuukauden raportin laskennan.
  Laskenta käyttää `measurements.dbWeighted`-sampleista energia-average LAeq -arvoa eikä session summary -keskiarvoja.
- `MeasurementDao.getWeightedMeasurementsInRange(...)` antaa data-kerroksessa vain `timestamp` + `dbWeighted` -projektion,
  jonka `MeasurementRepository` mapittaa `WeightedExposureMeasurement`-domain-malliksi.
  `SessionRepository.getCompletedSessionCountInRange(...)` antaa vuosiraportin Sessions-luvun valmiista sessioista.
- `AnalyticsViewModel` hakee monthly/yearly-dataa vain Pro-käyttäjälle `flatMapLatest`-gatingilla. Free-käyttäjälle
  palautetaan `MonthlyTrendUiState.LockedPreview` ja `YearlyReportUiState.LockedPreview` ilman oikean datan laskentaa.
- `MonthlyTrendChart` näyttää rolling 30 päivän LAeq-trendin Canvasilla. Päivät ilman sampleja jäävät puuttuviksi
  pisteiksi, jotta niitä ei tulkita 0 dB -mittauksiksi.
- `YearlyReportCard` näyttää rolling 12 kuukauden Sessions-, 12mo LAeq-, Loudest- ja meluvyöhykejakauman. Vyöhykkeet
  käyttävät samoja `NoiseLevel`-rajoja kuin Environment Mix.

## 2026-05-09 - Arkkitehtuurirajat siivottu

- `domain/` ei importtaa enää `data/`, `sync/`, `service/`, `ui/`, `widget/` tai `billing/`-paketteja. Domain-mallit ja
  laskenta ovat nyt `domain/session`, `domain/noise`, `domain/hearingtest`, `domain/report`, `domain/analytics`,
  `domain/audio` ja `domain/entitlement` -paketeissa.
- `AudioSessionManager` ja `MeasurementPersistenceSampler` siirtyivät `service/`-pakettiin, koska ne orkestroivat
  repositoryja, Health Connectia ja widget-päivityksiä. `domain/audio` jäi audio primitiveille ja DSP-logiikalle.
- Kuulotestin Hughson-Westlake-proseduuri, threshold-codec ja pisteytys siirtyivät `domain/hearingtest`-pakettiin.
  `ActiveTestViewModel` ohjaa vain käyttäjävasteita ja tone playbackia; `HearingTestService` tallentaa tuloksen ja
  kutsuu Health Connect no-op -synkkausta; `HearingTestRepository` mapittaa Room-entityn domain-malliksi.
- `SessionReportCalculator` käyttää `domain/report/ReportMeasurement`-mallia Room `MeasurementEntity`n sijaan.
  `ExposureAnalyticsCalculator` käyttää `WeightedExposureMeasurement`-mallia DAO-projektion sijaan. Adapterimappaus on
  ViewModel/repository-rajalla.
- `NoiseLevel` on `domain/noise`-paketissa ja toimii 40/70/85 dB -rajojen yhtenä lähteenä notificationeille,
  analytiikalle, historylle, PDF/PNG-kaavioille ja meter UI:lle. `fromDb(...)` lukee rajat enum-arvojen `maxDb`-kentista.
- CSV-export on `data/export`-paketissa, koska se lukee Room-entityja ja kirjoittaa FileProvider-jaettavat tiedostot.
  PDF-renderöinti on `util/ExportPdfReportUseCase.kt`, koska se on Android `PdfDocument`/Canvas-presentaatiota.
- UI-pääkoodi ei importtaa suoraan Room DAO/entity -projektioita tai `sync/`-paketin integraatiomalleja. Repositoryt
  palauttavat domain-malleja, `BackupService` ja `HealthConnectService` mapittavat integraatiot service-rajalle, ja
  ViewModelit muodostavat ruutukohtaiset UI-state-mallit.

## 2026-05-12 - Sonar open issues -siivous

- Sonarin 64 avoimen issuen siivous keskitettiin toistuviin klustereihin: dispatcher-injektio, ViewModelin share/export
  -eventit, Compose-parametripinnat, locale-formatointi, Android lint -resurssit, GitHub Actions -permissions sekä Gradle
  dependency locking.
- `di/CoroutineDispatchers.kt` määrittää Hilt-qualifierit `DefaultDispatcher`, `IoDispatcher` ja `MainDispatcher`.
  `AppModule` tarjoaa niiden arvot, ja aiemmin hardcoded-dispatchereita käyttäneet managerit/use caset saavat ne
  konstruktorista tai Android service -field injectionilla.
- Meter-, kuulotesti-, session detail- ja CSV-export-polut julkaisevat valmiit Android `Intent`it `SharedFlow`-eventteinä.
  Compose-ruudut keräävät eventit ja avaavat chooserin, joten ViewModelien julkinen API ei exposeaa suspend-funktioita.
- `SessionCardState`, `ProUpsellCardState` ja `ProUpsellCardActions` kokoavat UI-komponenttien aiemmin pitkät
  parametrilistat yhteen lähteeseen. Callerit rakentavat state-objektit ruutukohtaisesta UI-statesta.
- Gradle dependency locking on käytössä, ja lock state on generoitu `settings-gradle.lockfile`- sekä
  `app/gradle.lockfile`-tiedostoihin.

## 2026-05-12 - Foreground service omistaa mittaussession käynnistyksen

- `MeasurementForegroundService` tekee foreground-promootion ennen audiosession käynnistämistä. Jos
  `ServiceCompat.startForeground(...)` epäonnistuu, `AudioSessionManager.startSession()` ei enää käynnisty ViewModelin
  kautta erillisenä fallback-polkuina.
- `AudioSessionManager.startSession()` on suspend-rajapinta ja palauttaa onnistumisen vasta, kun `AudioEngine` on saanut
  `AudioRecord.startRecording()`-kutsun läpi. AudioRecord-start failure ei enää luo Room-sessiota eikä julkaise
  `isRecording = true` -tilaa.
- `domain/audio/AudioRecordPolicies.kt` keskittää PCM16-read-chunkin ja capture-bufferin mitoituksen sekä
  `AudioRecord.read(...)`-error-koodien tulkinnan. `ERROR_DEAD_OBJECT`, `ERROR_BAD_VALUE`, `ERROR_INVALID_OPERATION` ja
  muut negatiiviset read-tulokset pysäyttävät mittauspolun hallitusti `AudioRecordingFailure`-tuloksena.
- Onnistunut mittauspalvelun käynnistys palauttaa `START_NOT_STICKY`. Palvelu ei yritä palautua prosessin tappamisen
  jälkeen, koska nykyistä `AudioRecord`-sessiota ei rehydroida.
- Sovelluksen käynnistyksessä `DbCheckApplication` kutsuu `AudioSessionManager.recoverInterruptedSession()`-polkua. Jos
  Roomissa on edellisen prosessin jäljiltä aktiiviseksi jäänyt sessio, se suljetaan hiljaisesti viimeisen persistoidun
  mittauksen aikaleimaan ja summary-arvot lasketaan persistoiduista `dbWeighted`-riveistä.
- `MeterViewModel` käynnistää vain `MeasurementForegroundService`n ja seuraa `AudioSessionManager.isRecording`-virtaa
  Meterin UI-ajastimelle ja `isRecording`-tilalle. ViewModelin `onCleared()` ei pysäytä mittauspalvelua; pysäytys kulkee
  eksplisiittisen stop-komennon, palvelun tuhoutumisen tai AudioRecord-failuren kautta.
- `AudioSessionManager.activeSessionStartTimeMs` julkaisee aktiivisen session alkuhetken Meter UI:n uudelleenkytkentää
  varten, jotta taustalta tai notificationista palaava Meter ViewModel ei nollaa näkyvää session kestoa.
- `AudioSessionManager.stopSession(emitCompleted = ...)` snapshottaa completion-datan synkronisesti. Normaali stop
  julkaisee `completedSessionIds`-eventin, mutta reset ja AudioRecord-failure viimeistelevät session ilman
  auto-navigointia.
- Uuden session collector ohittaa ennen session käynnistysaikaa emittoidut `AudioEngine.decibelFlow` replay -lukemat,
  jotta edellisen session viimeinen lukema ei vääristä uuden session statseja tai Room-mittausrivejä.

## 2026-05-12 - dB-laskennan summary- ja peak-dataflow

- `DecibelCalculator.calculateDb(...)` laskee chunkin RMS-tason, ja `calculatePeakDb(...)` laskee peak-tason suurimmasta
  PCM-amplitudista samalla kalibrointioffsetilla ja 0-130 dB clampilla.
- `FrequencyWeightingFilter.applyWeighting(...)` palauttaa painotetun signaalin `DoubleArray`na, jotta taajuuspainotus
  ei leikkaudu takaisin PCM16-alueelle ennen `DecibelCalculator`-laskentaa. `DecibelCalculator`issa on ShortArray- ja
  DoubleArray-polut samalle RMS-/peak-dB-kaavalle.
- A-, B-, C- ja ITU-R 468 -painotukset ovat 44.1 kHz:n SOS-kaskadeja, jotka verifioidaan referenssitaajuuspisteilla
  `FrequencyWeightingFilterTest`issa, mukaan lukien ITU-R 468:n ylapaan pisteet ja 6.3 kHz:n +12.2 dB boost.
- `AudioEngine.DecibelReading` erottaa raw RMS -arvon (`instantDb`), valitun painotuksen RMS-arvon (`weightedDb`),
  rinnakkaisen A-painotetun RMS-arvon (`aWeightedDb`) ja C-painotetun peak-arvon (`peakDb`). `aWeightedDb` on
  runtime-dataa dosimeter-polulle eikä korvaa valittua `weightedDb`-arvoa. Session `peakDb`, peak warningit,
  notification peak sekä PDF/PNG-raportin LCpeak lukevat C-painotettua `peakDb`-arvoa.
- `SessionStats.avgDb` lasketaan energia-averageena painotetuista lukemista. Valmiin session `sessions`-taulun
  `avgDb` toimii Session Detailin LAeq-headline-mittarin lähteenä, jotta Meterissä näytetty/persistoitu summary ei eroa
  raportin headline-arvosta.
- `SessionReportCalculator` käyttää headline-mittareissa session summarya (`avgDb`, `minDb`, `maxDb`, `peakDb`) ja
  measurement-rivejä vain time-series- ja A-painotettuun peak event -listaan. Rivikohtainen C-painotettu `peakDb` pysyy
  LCpeak-lähteenä eikä muodosta 85 dBA eventtejä.
- Historyn hourly/daily summaryt muodostetaan `MeasurementBucketAverages`-helperilla energia-averageena. `MeasurementDao`
  ei käytä enää aritmeettista `AVG(dbWeighted)`-SQL-laskentaa näihin summaryihin.

## 2026-05-12 - Privacy-sensitive data handling

- Android system backup on kytketty pois dBcheckilta `AndroidManifest.xml`ssa ja backup-konfiguraatiot
  `backup_rules.xml`/`data_extraction_rules.xml` sulkevat appin root-datan pois sekä cloud backupista että
  device-transferista. Varsinainen käyttäjän käynnistämä paikallinen backup pysyy `filesDir/backups`-polussa.
- `FileProvider` julkaisee vain `cache/exports/`-hakemiston. CSV- ja PNG-jakotiedostot luodaan `ExportFileCache`n kautta
  tähän alihakemistoon, ja yli 24 tuntia vanhat export-cache-tiedostot poistetaan best-effort-mallilla seuraavan
  export/share-operaation yhteydessä.
- CSV `ACTION_SEND_MULTIPLE` lisää kaikki jaettavat `content://`-URI:t sekä `EXTRA_STREAM`iin että `ClipData`an ja antaa
  vain väliaikaisen `FLAG_GRANT_READ_URI_PERMISSION`-luvan.
- Mittausnotificationin live dB -sisältö ei ole enää `VISIBILITY_PUBLIC`; `NotificationPrivacyPolicy` keskittää
  measurement-notificationien lukitusnäkyvyyden private-tasolle.
- Settingsin Health Connect -kortissa on Health Connectin hallintanäkymään vievä Manage-toiminto. Sync toggle pyytää
  edelleen vain write exercise -permissionin, ja heart rate overlay vain read heart rate -permissionin.

## 2026-06-14 - Session location permission scope

- Session location is optional metadata, not required for measurement.
- The manifest contains only foreground approximate `ACCESS_COARSE_LOCATION`; do not add `ACCESS_FINE_LOCATION`,
  `ACCESS_BACKGROUND_LOCATION`, or a foreground service `location` type without a new product/privacy decision.
- Runtime permission must be requested only in context after the user chooses the location feature. The current
  `AndroidSessionLocationCapturePort` returns `null` when runtime permission is missing, the provider is unavailable, or
  the location API cannot return a fix.
- `SessionLocationCapturePort` is the service-level fake-testable port. `AudioSessionManager` tries to persist
  `SessionLocationMetadata` after active session creation and again on stop only as a fallback if start had no location.
  Capture failures or `SessionRepository.updateSessionLocation(...)` failures must not fail session start/stop/completion.
- Privacy copy: location is optional, approximate-only, used to help identify measurement sessions; dBcheck does not use
  precise location, background location, continuous tracking, advertising-purpose location or analytics-purpose location.

## 2026-05-12 - Billing lifecycle recovery

- `BillingManager.queryExistingPurchases()` prosessoi Play Billingin ostosnapshotin: valmis `dbcheck_pro`-osto asettaa
  Pro-tilan ja acknowledgeataan tarvittaessa myös startup-/reconnect-kyselyn jälkeen. Snapshot-polku ei julkaise
  `PurchaseEvent.Completed`-eventtiä, jotta olemassa oleva ostos ei näytä uutta unlock-viestiä jokaisella käynnistyksellä.
- `BillingManager.refreshPurchases()` on ostosnapshotin julkinen refresh-portti. `MainActivity.onResume()` kutsuu sitä,
  jotta appi käsittelee Play Billingin ulkopuolella valmistuneet tai pending-tilasta valmistuneet ostot foregroundiin
  palatessa.
- `ITEM_ALREADY_OWNED` julkaisee edelleen `AlreadyOwned`-eventin, mutta käynnistää lisäksi `queryExistingPurchases()`-haun
  ostotokenin ja mahdollisen acknowledge-puutteen korjaamiseksi.
- `PurchaseState.PENDING` julkaistaan `PurchaseEvent.Pending`-eventtinä eikä avaa Pro-oikeutta. Settings näyttää
  pending-maksuviestin ja tyhjentää ostovirran loading/error-tilan.
- Settingsin purchase-virhetilat nollaavat aiemman success/pending-viestin, jotta Pro-kortti ei näytä ristiriitaisia
  ostoviestejä saman aikaisesti.

## 2026-05-12 - Pro feature gate hardening

- `data/local/preferences/model/ProAudioPreferencePolicy.kt` keskittää Pro-mittausasetusten effective-arvot.
  Free-käyttäjän calibration offset, frequency weighting, response time ja dosimeter standard palautuvat aina
  `UserPreferenceDefaults`-arvoihin, vaikka DataStoressa olisi aiemmin tallennettuja Pro-arvoja.
- `SettingsViewModel` ei persistoi calibration- tai frequency weighting -muutoksia, ellei `isProUser` ole tosi.
  `AudioSessionManager` käyttää samaa policyä ennen kuin se kutsuu `AudioEngine.setCalibrationOffset(...)`- ja
  `AudioEngine.setWeighting(...)`-metodeja.
- `AudioSessionManager.startSession()` lukee ensimmäiset effective Pro-audioasetukset synkronisesti ennen
  `AudioEngine.startRecording(...)`-kutsua, jotta edellisen session calibration offset ei ehdi vaikuttaa uuteen
  mittaukseen ennen preference-collectorin ensimmäistä emissiota.
- `domain/session/SessionHistoryPolicy` on Free-historian 7 päivän ikkunan lähde. History-listaus, vanhojen sessioiden
  cleanup ja Session Detailin suora `history/detail/{sessionId}` -reitti käyttävät samaa ikkunaa, joten Free-käyttäjä ei
  voi avata vanhaa sessiota pelkällä session id:llä.
- Hearing test on gateattu UI-entryn lisäksi execution- ja data-polussa: `ActiveTestViewModel` ei käynnistä tone
  playbackia Free-tilassa, `HearingTestService.saveCompletedTest(...)` ei tallenna Free-tulosta, ja
  `ResultsViewModel` ei lataa tai jaa hearing-test-resultia, ellei käyttäjä ole Pro.

## 2026-06-09 - Dosimeter-standard preference ja laskenta

- `domain/noise/DosimeterStandard.kt` omistaa typed standardit: `NIOSH_REL` on default ja `OSHA_PEL` on Pro-käyttäjälle
  tallennettava vaihtoehto. DataStore-avain on `dosimeter_standard`, ja tuntematon arvo normalisoituu `NIOSH_REL`iin.
- `SettingsUiState.dosimeterStandard` ja `SettingsViewModel.updateDosimeterStandard(...)` tuovat standardin
  Settings-stateen.
- `domain/noise/DosimeterCalculator.kt` laskee NIOSH_REL- ja OSHA_PEL-standardien TWA-, dose-, projected dose- ja
  remaining exposure time -arvot. Completed report kayttaa laskuria nykyisille NIOSH_REL-kentille.
- `AudioSessionManager.liveExposureState` julkaisee aktiivisen session live-dosimeter-tilan jokaisesta
  `DecibelReading.aWeightedDb`-lukemasta. State laskee A-painotetun LAeq-arvon, effective standardin mukaiset
  dose/projection-arvot ja nollautuu startissa/resetissa; Room-persistointi pysyy `MeasurementPersistenceSampler`in
  1s cadencella.
- `ui/meter/state/MeasurementMode` omistaa Meterin `DB_METER` / `DOSIMETER` -modevalinnan. `MeterUiState.measurementMode`
  defaulttaa `DB_METER`iin, ja `MeterViewModel.setMeasurementMode(...)` paivittaa vain statea ilman foreground-service-
  tai AudioSessionManager-start/stop-sivuvaikutuksia.

## 2026-06-11 - Meter active session info bar

- `domain/audio/AudioInputInfo` on `AudioEngine`in live input metadata -state. `AudioEngine.audioInputInfo` julkaisee
  kiintean `AudioProcessingConfig.SAMPLE_RATE` -arvon ja `AudioRecord.routedDevice.productName` -nimen vasta, kun
  `AudioRecord.startRecording()` on onnistunut; state palautuu defaulttiin stop/release-polussa, koska Androidin
  routing-tieto on luotettava vain aktiivisen tallennuksen aikana.
- `MeterViewModel` rakentaa `MeterSessionInfoUiState`n `AudioSessionManager.isRecording` /
  `activeSessionStartTimeMs` -virroista, `ProAudioPreferencePolicy`n effective weighting/response time -arvoista seka
  `AudioEngine.audioInputInfo`sta.
- `MeterSessionInfoBar` nakyy vain aktiivisen Meter-session aikana. Free-kayttaja nakee REC-tilan, keston,
  effective weightingin ja response timen; Pro-kayttaja nakee lisaksi sample raten ja input devicen.

## 2026-06-11 - Analytics section state

- `ui/analytics/state/AnalyticsSection` omistaa Analyticsin section-valinnan arvot `OVERVIEW`, `SPECTRAL` ja
  `ENVIRONMENT`. `AnalyticsOverviewRange` omistaa Overviewin `WEEKLY` / `MONTHLY` -range-valinnan.
- `AnalyticsViewModel` sailyttaa section-, overview range- ja spectral mode -valinnat omissa `MutableStateFlow`
  -lahteissaan ja julkaisee ne `AnalyticsUiState.Success.selectedSection`-, `selectedOverviewRange`- ja
  `selectedSpectralMode`-kentissa. `onSectionSelected(...)`, `onOverviewRangeSelected(...)` ja
  `onSpectralModeSelected(...)` paivittavat nykyisen Success-staten heti, ja seuraavat analytics-dataemissiot kayttavat
  samoja valintoja eivatka palauta niita oletukseen.
- `AnalyticsSectionChipRow` renderoi section-valinnan Analytics-headerin alle. Free-kayttajalla Spectral ja Env Mix
  nakyvat lukkoikonilla, eivat piilotettuina, ja ne voivat silti olla valittuina.
- `AnalyticsOverviewRangeChipRow` renderoityy vain Overview-sectionissa. Weekly on Free-kayttajalle auki; Monthly nakyy
  Free-kayttajalle Pro-lukittuna, mutta voi silti olla valittuna locked-preview-nakymaa varten.
- `SpectralMode` omistaa spektrikortin renderointimoden arvoilla `BARS`, `SPECTROGRAM` ja `RTA`.
  `SpectralModeChipRow` renderoi kaikki kolme valintaa `SpectralAnalysisCard`in sisalla.
- `SpectrogramBuffer` on `AnalyticsViewModel`in live-only UI-bufferi. Se muodostaa `AudioEngine.spectralFrame`
  -emissioista `SpectrogramUiState`-waterfall-rivit, sailyttaa enintaan 60 viimeisinta rivia, ohittaa saman timestampin
  uudelleenemissiot ja tyhjentyy, kun kayttaja ei ole Pro tai live-frame puuttuu.
- `SpectralAnalysisCard` renderoi Bars-, Spectrogram- ja RTA-haarat erikseen. `SpectrogramCanvasModel` omistaa
  spectrogram Canvas-solujen muodostuksen ja locked-preview-rivit. `RtaBarsModel` omistaa RTA Canvas -barit seka
  PEAK/BANDS-stat pillien arvot. `formatSpectralFrequency(...)` on UI:n yhteinen Hz/kHz-muotoilija.
- `DbCheckChip` tukee nyt valinnaista leading iconia ja saadettavaa horizontal paddingia, jotta lukitut chipit voivat
  kayttaa samaa design-tokenoitua chip-komponenttia ilman erillista kopiota.
- `AnalyticsSelectableChip` on Analyticsin lukkoikonia kayttavien chip-rivien yhteinen render-helper.
- `analyticsSectionCards(...)` omistaa Analyticsin section- ja range-kohtaisen korttiryhmittelyn. Overviewin Weekly-range
  renderoi weekly exposure- ja hearing health -kortit, Monthly-range renderoi `MonthlyTrendChart`in, ja yearly report
  seka hearing-test CTA pysyvat Overviewissa molemmissa rangeissa. Spectral renderoi `SpectralAnalysisCard`in;
  Environment renderoi `EnvironmentMixCard`in. Tama ei muuta `AnalyticsViewModel`in dataflow'ta tai Pro-gatingia: kaikki
  nykyiset UI-state-kentat rakennetaan edelleen samalla tavalla.

## 2026-06-12 - Sound detection infrastructure

- YAMNet assets live under `app/src/main/assets/sound_detection/`, and `YamnetModelAssets` owns the model/class-map
  asset paths.
- `YamnetAudioWindowAdapter` converts the existing 44.1 kHz PCM16 chunk stream into 16 kHz normalized float windows
  without persisting raw audio.
- `SoundClassifier` is the testable inference port. `TfliteSoundClassifier` is the production adapter over TensorFlow
  Lite Task Audio `AudioClassifier`, and `SoundClassificationPolicy` owns confidence-threshold and empty-output mapping.
- `SoundDetectionWindowFanout` is the `AudioEngine` live-only raw-audio fanout for YAMNet windows. `AudioSessionManager`
  enables it only for the effective condition `isProUser && soundDetectionEnabled`.
- `AudioSessionManager.soundDetectionState` publishes enabled/current/recent detection state. Classifier calls happen in
  the manager collector job; `AudioEngine` does not run inference, and raw audio is not persisted.
- Environment UI rendering and locked/idle/live/error card states are wired. Optional detection persistence is a separate
  DataStore opt-in (`soundDetectionPersistenceEnabled`) and stores only aggregated label-change events in
  `sound_detection_events` through `SoundDetectionRepository`; raw audio and YAMNet float windows are never persisted.

## 2026-06-12 - CameraX dependency baseline

- CameraX is locked in the version catalog to stable `1.6.1`, verified against the official AndroidX Camera release
  notes before implementation.
- The app declares `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` and
  `camera-video` through the single `cameraX` version catalog source.
- Gradle dependency locking and dependency verification metadata include the CameraX artifacts and transitives. Any new
  CameraX artifact must update both lock state and verification metadata.
- The `CAMERA` manifest permission is declared, and `ui/camera/CameraPermissionPolicy.kt` owns the future camera route
  runtime permission states: initial request, granted, denied/rationale and permanently denied/settings.
- `Screen.CameraOverlay` / `camera_overlay` is opened from the Pro-gated Meter camera entry and is not a top-level
  destination, so bottom nav and navigation rail are hidden on the fullscreen overlay.
- `ui/camera/CameraOverlayRoute.kt` requests the camera runtime permission, binds `PreviewView` + CameraX `Preview`,
  `ImageCapture` and `VideoCapture<Recorder>` through `ProcessCameraProvider.bindToLifecycle(...)` with
  `CameraSelector.DEFAULT_BACK_CAMERA`, and shows a camera-unavailable fallback if provider lookup or binding fails.
- `CameraOverlayViewModel` reads the live overlay readout from `AudioEngine.decibelFlow` only while
  `AudioSessionManager.isRecording` is true and the reading timestamp is at or after `activeSessionStartTimeMs`. Idle
  overlay state shows an empty dB value instead of stale replay data. The level label comes from the effective preference
  path `ProAudioPreferencePolicy.weighting(...)` + `equivalentLevelLabelForWeighting(...)`.
- `CameraOverlayShareGenerator` owns the photo capture share path: raw JPG is written temporarily to the
  `ExportFileCache` export cache, the current readout is burned into a PNG bitmap, the raw capture is deleted, and the
  PNG is shared with a FileProvider `content://` URI, `ClipData` and a temporary read grant.
- Silent video capture writes `dBcheck_camera_silent_video_*.mp4` files to the same export cache with
  `FileOutputOptions`. The recording path does not call CameraX `withAudioEnabled()`, does not request `RECORD_AUDIO`
  for video, and does not add raw audio collection. Compose overlay is not burned into MP4 output; that requires a
  separate rendering or post-processing path before video can include the same burned-in readout as PNG share.
- Photo capture, silent video, camera permission policy and the live readout do not change Meter microphone permission,
  startup permission policy or measurement start behavior.

## 2026-06-20 - WAV recording opt-in, writer, and export/delete

- `wav_recording_default` is a Pro-gated DataStore preference and defaults to OFF. Settings Data & Export shows the
  toggle with an explicit raw microphone audio privacy warning.
- `SettingsViewModel.updateWavRecordingDefaultEnabled(...)` blocks Free users from enabling the preference, and
  `SettingsUiState.wavRecordingDefaultEnabled` is effective-OFF for Free users even if the stored preference is true.
- `PcmWavWriter` writes mono PCM16 WAV files as a stream without buffering the whole recording. It writes a placeholder
  RIFF/WAVE header first and patches RIFF/data sizes on close.
- `WavRecordingFileStore` stores WAV files in app-private `filesDir/wav_recordings` using
  `dBcheck_wav_session_<sessionId>_<startedAtMs>.wav` names.
- `AudioSessionManager` starts WAV persistence only when effective `isProUser && wavRecordingDefaultEnabled` is true.
  Normal stop finalizes the WAV header; recording failure and startup cleanup abort the writer and delete the partial
  file through `AudioEngine.abortWavRecording()`.
- `WavRecordingFileStore` resolves the latest WAV for a session, creates `audio/wav` `ACTION_SEND` share intents with a
  FileProvider `content://` URI, `ClipData` and `FLAG_GRANT_READ_URI_PERMISSION`, and deletes only the matching session
  WAV file.
- `file_paths.xml` exposes app-private `filesDir/wav_recordings` through FileProvider. WAV files are not copied to
  MediaStore; sharing is user-initiated through the Android Sharesheet.
- Session Detail shows a WAV card when the opened session has a WAV file. Share is Pro-gated; delete removes the WAV
  file without adding new raw-audio processing. Manual share smoke passed on the `Pixel_9_Pro` emulator: Android
  Sharesheet opened for the WAV file and delete emptied app-private `files/wav_recordings`.

## 2026-05-13 - DAO-aikarajat ja deterministinen järjestys

- DAO-listauksissa käytetään aikaleiman lisäksi primary key -tie-breakeriä: sessioiden ja kuulotestien latest/recent
  -kyselyissä `id DESC`, measurement time-series -kyselyissä `id ASC`. Tämä estää saman millisekunnin rivejä
  palautumasta SQLite-suunnitelmasta riippuvassa järjestyksessä.
- `MeasurementRepository` ei sido 24h/7d-rullaavia aikarajoja enää flow'n luontihetkeen. `getLast24HoursMeasurements`,
  `getHourlyAveragesLast24H`, `getDailyAveragesLast7Days` ja `getEnvironmentMixLast7Days` resubscribaavat DAO-kyselyihin
  minuutin välein uudella `startTime..endTime`-ikkunalla, joten tulevaisuuteen osuvat timestampit eivät päädy rullaaviin
  yhteenvetoihin.
- Analyticsin weekly daily-summary bucketoi mittausrivit käyttäjän paikallisen aikavyöhykkeen päivän alkuun.
  `DailyExposureUiState.isToday` erottaa todellisen kuluvan paikallisen päivän viimeisimmästä saatavilla olevasta
  mittauspäivästä, joten eilistä ei käytetä "today vs week" -vertailuna.
- `AnalyticsViewModel` päivittää Pro-analytiikan 30 päivän ja 12 kuukauden query-parametrit minuutin välein, joten
  monthly trend, yearly report ja yearly session count eivät jää screenin avaamishetken `nowMs`-ylärajaan.
  Pro-käyttäjän monthly/yearly-data pitää Analytics-näkymän näkyvissä, vaikka viimeisen 7 päivän weekly-data puuttuisi.

## 2026-05-14 - Session repositoryn transaktiokirjoitukset

- `SessionRepository` omistaa nyt mittausrivien ja session summaryn yhteiskirjoitukset Roomin `withTransaction`-poluilla.
  `recordActiveSessionMeasurements(...)` mapittaa flushatut `SessionMeasurement`-domainrivit `MeasurementEntity`-riveiksi
  ja päivittää aktiivisen session runtime-summaryn samassa transaktiossa. `completeSessionWithMeasurements(...)`
  kirjoittaa viimeiset pending-rivit ja sulkee session samassa transaktiossa.
- `AudioSessionManager` luo aktiivisen `SessionEntity`n nykyisellä effective frequency weighting -arvolla eikä pelkällä
  defaultilla. Measurement flush päivittää aktiivisen session `minDb`/`avgDb`/`maxDb`/`peakDb`-summaryä, jotta
  `recoverInterruptedSession()` voi palauttaa myös LCpeak-arvon eikä pelkästään measurement-riveistä laskettavia
  weighted summaryjä.
- `MeasurementRepository` ei enää exposeaa write-portteja mittausriveille. Se lukee measurement-dataa ja tekee
  analytiikka-aggregointien Flow-mappaukset injektoidulla `DefaultDispatcher`illa, jotta History/Analytics-keräilijät
  eivät tee groupBy/energia-average-laskentaa Main-kontekstissa.
- Room-skeema on v8. `calibration_profiles` tallentaa UI:sta riippumattomat calibration profile -rivit: `id`, `name`,
  `micSensitivityOffset`, `octaveBandOffsets`, `isDefault`, `createdAt` ja `updatedAt`. `CalibrationProfileRepository`
  mapittaa Room-rivit domain-malliksi, tarjoaa `createProfile(...)`, `observeProfiles()`, `getProfile(...)`,
  `renameProfile(...)`, `deleteProfile(...)`, `updateOctaveBandOffsets(...)` ja `resetOctaveBandOffsets(...)` -polut,
  normalisoi flat- ja octave-offsetit yhteisella `CalibrationOffsetPolicy`lla ja estää viimeisen `isDefault`-profiilin
  poiston data-kerroksessa. V8:n Room identity hash on lisätty `BackupDatabaseValidator`in sallittuihin hasheihin, jotta
  uudet paikallisbackupit läpäisevät restore-validaation.
- Valittu calibration profile tallennetaan DataStore-avaimeen `selected_calibration_profile_id`, joka normalisoidaan
  positiiviseksi `Long`-ID:ksi tai `null`iksi. Runtime-kalibroinnin kytkentä valittuun profiiliin tulee myöhemmässä osassa.
- Settingsin `AudioCalibrationSection` näyttää calibration profile -hallinnan ProLockOverlayn takana. `SettingsViewModel`
  mapittaa repository-virran `CalibrationProfileUiState`-riveiksi, joihin sisältyvät valitun profiilin octave-band-offsetit
  `OctaveCalibrationBandUiState`-listana. Settings näyttää valitulle profiilille `DbCheckSlider`-bandisäätimet ja
  reset-ikonipainikkeen; update/reset kirjoittaa `CalibrationProfileRepository`n `updateOctaveBandOffsets(...)`- ja
  `resetOctaveBandOffsets(...)` -polkuihin. ViewModel bootstrappaa Pro-käyttäjälle `Device default` -profiilin vasta
  ensimmäisen Room-profiiliemission jälkeen, tallentaa selectin `selected_calibration_profile_id`-avaimeen ja valitsee
  fallback-profiilin, jos nykyinen valinta poistetaan. Free-käyttäjä ei voi
  create/select/rename/delete/update/reset-profiileja ViewModelin kautta.
- Room v6:n `sessions` sisältää nullable optional location -metadatasarakkeet `locationLatitude`,
  `locationLongitude`, `locationAccuracyMeters` ja `locationCapturedAt`; `MIGRATION_5_6` ei backfillaa vanhoja rivejä.
  `SessionDao.updateSessionLocation(...)` ja `SessionRepository.updateSessionLocation(...)` päivittävät sijainnin partial
  update -polulla erillään measurement/summary-transaktioista. Room v5:n `sound_detection_events` tallentaa vain optional opt-in -sound detection eventit: `sessionId`, timestamp,
  label ja confidence, cascade-viitteella sessioon. `MIGRATION_4_5` luo taulun ja indeksit export-/session-kyselyille.
- `SessionHistoryQuery` on Historyn repository-hakumalli. `SessionRepository.getFilteredSessions(...)` säilyttää Free-
  käyttäjän 7 päivän history policy -alarajan, antaa Pro-käyttäjälle koko historian ja mapittaa name/tag/date/avg dB/
  weighting/location-filtterit `SessionDao.searchSessions(...)` -kyselyyn. Query order on aina
  `startTime DESC, id DESC`.
  Room v4:ssa `measurements.aWeightedDb` tallentaa rinnakkaisen A-painotetun RMS-arvon ja `measurements.responseTime`
  mittaushetken effective response time -nimen. `MIGRATION_3_4` backfillaa vanhoille riveille `aWeightedDb = dbWeighted`
  ja `responseTime = FAST`, koska v3:ssa ei ollut erillistä A- tai response-time-rivimetadataa. `MIGRATION_2_3` lisäsi
  aiemmin rivikohtaisen C-painotetun `peakDb`-LCpeak-arvon ja backfillasi sen `dbWeighted`-arvolla.
- `MeasurementPersistenceSampler` force-persistoi uuden session LCpeak-huipun samalla tavalla kuin uuden weighted maxin.
  Tämä ei muuta 1s peruscadencea, mutta estää lyhyttä peak-lukemaa jäämästä pelkästään in-memory session summaryyn.
- `AudioSessionManager` suojaa measurement flushin ja session completionin samalla `Mutex`illa. Stop/failure-polku odottaa
  käynnissä olevan flushin valmistumisen ennen `completeSessionWithMeasurements(...)`-kutsua, joten pending-rivit eivät
  katoa flushin ja stopin väliseen peruutusikkunaan.
- `AudioSessionManager.startSession()` ja startupin `recoverInterruptedSession()` käyttävät yhteistä interrupted-session
  recovery -porttia. Uusi mittaus recoveryttää edellisen prosessin aktiivisen session ennen uuden aktiivisen rivin luontia,
  ja recovery ohitetaan, jos managerilla on jo muistissa käynnissä oleva nykyinen sessio.
- `MeasurementForegroundService` pysäytetään nyt eksplisiittisellä stop-komennolla, joka kantaa `emitCompleted`-arvon.
  Meter reset lähettää `emitCompleted=false`, joten service `onDestroy()` ei voi kilpailemalla julkaista completion-
  navigointia resetistä.
- `MeasurementBucketAverages` painottaa hourly/daily energia-averagea mittausrivien aikaleimaväleillä. Forced-rivit eivät
  enää saa samaa painoa kuin normaali 1s persistence-rivi rullaavissa History/Analytics-yhteenvedoissa.
