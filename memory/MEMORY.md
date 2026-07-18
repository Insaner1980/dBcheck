# dBcheck Memory

## 2026-07-18 - Settings graph, page ownership, and shared state

- `settings` is a parent navigation graph starting at `settings/home`, with calibration, calibration/octave,
  notifications, data_privacy, display, and pro_about children. The legacy `settings?showPro={showPro}` route is a
  redirect only.
- Every Settings child resolves one `SettingsViewModel` from the `settings` graph back stack entry. Reselecting the
  Settings top-level destination returns a current child to home while cross-stack state restoration remains enabled.
- Notifications exclusively owns passive-monitoring permission launchers. Data & privacy owns location, CSV
  Sharesheet, Health Connect, backup/restore, clear-history, and lockscreen-privacy UI flows. Calibration owns audio
  controls and profiles, its octave child owns band controls, Display owns appearance and feature toggles, and Pro &
  About owns purchase/debug/version/about presentation.
- Voice Baseline is Hearing-only. Settings no longer carries Voice Baseline state, rendering, or capture actions;
  `SettingsViewModel` keeps `AudioSessionManager` for backup/restore and clear-history guards and audible-alarm preview.
- Every purchase-capable Settings page renders shared `SettingsPurchaseFeedback` and clears purchase feedback only
  after it is visible. Other transient message effects are page-specific. The octave child uses the same
  `ProLockOverlay` gate as calibration and gives Free users a rich, non-editable slider preview.
- `settingsLegacyRedirectPlan(...)` is the pure source for legacy home/pro_about sequencing.
  `TopLevelNavigationPolicy.navigationRoute` sends same-stack Settings child reselects to `settings/home` while
  cross-stack restore continues to target the parent graph.

## 2026-07-18 - Five top-level destinations and Hearing return contract

- The compact bottom bar and the >=600 dp navigation rail share `BottomNavDestination.entries`: Meter, Trends on the
  internal `analytics` route, Hearing on `hearing`, History, then Settings. Hearing feature routes remain fullscreen
  non-top-level routes and continue to hide shared navigation.
- Trends opens the Hearing root through its single Hearing handoff. The Hearing hub opens the existing hearing test,
  recovery, tinnitus, ambient, and sleep routes. Locked upgrades use `settingsLegacyRedirectPlan(showPro = true)` and
  navigate first to `settings/home`, then `settings/pro_about`. Separately, the route contract
  `Screen.Settings.createRoute(true)` resolves to `settings/pro_about`, while the false variant resolves to home.
- Successful Hearing Test Results save/back and Hearing Recovery completion return to the Hearing root. Meter, Trends,
  and History no longer expose separate Settings shortcuts; Settings remains available from shared top-level navigation.

## 2026-07-18 - Trends and Hearing responsibility boundary

- The compatibility route remains `analytics`, and the package, `AnalyticsViewModel`, and section enums retain their
  internal Analytics names. User-visible title and navigation copy are Trends.
- Trends owns exposure, trend/report, Spectral, and Environment content only. Its ViewModel no longer depends on
  hearing-test or hearing-recovery repositories and no longer carries recovery, tinnitus, or Sleep tool state.
- `HearingHealthSummaryCalculator` is the nullable shared source for hearing-health status. Trends renders only the
  Hearing-owned tokenized `HearingStatusRow` and its single `onNavigateToHearing` handoff; missing usable samples are
  shown honestly as no data, never SAFE.

## 2026-07-18 - Hearing hub UI ownership

- `ui/hearing/HearingScreen.kt` owns the Hearing hub content and collects `HearingViewModel.uiState` with the
  lifecycle-aware Compose collector. Its order is hearing status plus latest test, hearing test, recovery, tinnitus
  pitch, Voice Baseline, then tools with effective Sleep Monitor visibility before Ambient Sounds.
- `HearingHealthCard`, `HearingTestCta`, `HearingRecoveryCard`, `TinnitusPitchCard`, `AmbientSoundCard`, and the compact
  `HearingStatusRow` live under `ui/hearing/components`. Trends uses only the compact status row; full Hearing/tool
  cards render in the Hearing hub.
- `ui/hearing/components/VoiceBaselineCard.kt` is the single Voice Baseline Compose implementation and Hearing is its
  only UI owner. Calibration logic and the Pro + active measurement + Sound Detection gate live in `HearingViewModel`.

## 2026-07-09 - Material 3 shared UI -jarjestelma

- `ui/theme/Spacing.kt`, `Shape.kt`, `Motion.kt` ja `ChartTokens.kt` ovat Material 3 -viimeistelyn token-lahteet.
  Uudet ruudut ja komponentit kayttavat `groupGap` 12dp-, `sectionGap` 32dp-, `cardPadding`-, `tilePadding`-,
  `DbCheckRadii`- ja `ChartTokens`-arvoja ennen paikallisia kovakoodattuja spacing/shape/chart-arvoja.
- `DbCheckCard`/`DbCheckCardEmphasis` ja `DbCheckChip`/`DbCheckChipDensity` ovat korttien ja chipien ensisijaiset
  jaetut komponentit. Pintahierarkiaa ei pideta ruutukohtaisissa `surfaceContainer*` + radius + padding -kopioissa.
- Pro-lukitut previewt kulkevat `ProLockOverlay`n kautta: yksi 0.68 alpha -scrim, 48dp tonal lock circle, 48dp CTA ja
  normaali preview-sisalto overlayn alla. Lukittu state ei ole disabled-state.
- `InlineStatusRow`, `DbCheckAlertDialog` ja `DbCheckSetupScaffold` keskittavat statusviestit, Settings-dialogit ja
  fullscreen setup-flow'n back/header/content/CTA-rakenteen. Hearing, Sleep, Tinnitus ja Ambient setupit noudattavat
  samaa scaffold-rytmiä.
- Meterin live-sankari on `LiveActivityCard`, ja Analytics/History/Session Detail -kaaviot nojaavat `ChartTokens`in
  grid/stroke/dash/radius/alpha-kielioppiin. Uusi chart ei saa maaritella omaa kaaviogrammaria, jos tokeni on olemassa.
- `util/ExternalBrand.kt` on ulkoisten pintojen brand-lahde: wordmark, 80px share-margin, panel radius, Manrope/
  Space Grotesk -paint-helperit ja NoiseLevel-varimapping. `ShareResultsGenerator`, camera burn-in, widgetin
  level-label ja custom notificationin level-label lukevat sita. PDF sailyttaa oman printtipalettinsa mutta pysyy
  samassa fontti-/wordmark-perheessa.

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
- Trendsin ja Historyn tyhjatilan CTA:t navigoivat Meteriin. Meterillä, Trendsillä ja Historylla ei ole
  Settings-oikotietä, ja Settingsissa ei näytetä action-ikonia ilman toimintoa.
- `DurationFormatter.formatClockDuration(...)` on kellomuotoisen keston yhteinen helper Meter sharelle, lockscreen
  notificationille, PDF-raportille ja Session Detailille.

## 2026-05-09 - Pro-oston UI-kytkentä ja entitlement-flow

- `billing/BillingGateway.kt` erottaa Settingsin ostovirran testattavaksi rajapinnaksi. `BillingManager` toteuttaa
  rajapinnan, kysyy `ProductDetails`-datan juuri ennen ostovirtaa ja palauttaa `PurchaseLaunchResult`-tuloksen.
- `BillingRuntimeGateway` erottaa startup/resume-refreshin ja `BillingEntitlementSource` erottaa ostotilan streamin.
  `BillingManager` on porttien tuotantototeutus; appin tuotantokuluttajat injektoivat billingin rajapintoina.
- Billingin käynnistystila on kolmivaiheinen: `BillingEntitlementSource.isPurchased` alkaa `null`-arvosta, joka tarkoittaa
  "ei vielä varmistettu". `ProFeatureManager` synkkaa DataStoreen vain varmistetun `true`/`false`-tilan, jotta appin
  käynnistys tai epäonnistunut Play Billing -kysely ei ylikirjoita aiemmin tallennettua Pro-oikeutta Free-tilaan.
- `BillingManager.purchaseEvents` välittaa ostotapahtumat Settingsiin arvoilla `Completed`, `Cancelled`,
  `AlreadyOwned` ja `Failed`. Onnistunut ostos asetetaan ostetuksi ja acknowledge tehdään vasta
  `PurchaseState.PURCHASED`-tilassa.
- Pro-oikeuden laskenta on keskitetty `domain/entitlement/ProEntitlementPolicy.kt`-tiedostoon. Release-buildissa Pro määräytyy
  ostotilan mukaan. Debug-buildissa käyttäjä on oletuksena Pro, mutta `debugForceFreeEnabled` pakottaa Free-tilan.
- `UserPreferencesDataStore` tallentaa ostotilan ja debug force-free -asetuksen erikseen. `UserPreferences.isProUser`
  on aina effective entitlement, jota UI ja feature-gatet lukevat.
- Settingsin Pro & About -sivu käynnistää Google Play Billing -ostovirran Pro-kortista, ja Settings-sivujen
  ProLockOverlay-painikkeet käyttävät samaa graph-scoped ViewModel -ostovirtaa. Muiden näyttöjen Upgrade-painikkeet
  navigoivat edelleen Settingsin Pro-korttiin.
- `DbCheckApplication` injektoi `ProFeatureManager`in, jotta billing-tilan synkkaus DataStoreen käynnistyy sovelluksen
  käynnistyksessä eikä riipu foreground servicestä. Sama entitlement-flow päivittää Glance-widgetit, kun Pro-oikeus
  muuttuu.

## 2026-05-10 - Startup-initialisoinnin siivous

- `MainActivity` odottaa ensimmäistä `UserPreferences`-emissiota ennen `DbCheckTheme`/`DbCheckNavHost`-sisällön
  piirtämistä. Preference-flow synkronoi Android 12+:n package-kohtaisen night moden tallennetusta `ThemeMode`-arvosta
  ennen emission julkaisemista Compose-sisällölle. Android 11:n ja vanhempien `values`/`values-night`-teemat poistavat
  system-selected startup-previewn. `StartupThemeState` pitää sisällön lataustilassa, kunnes ensimmäinen frame voidaan
  piirtää tallennetulla teemalla ilman system-teeman välähdystä.
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
- Settingsin Data & privacy -sivu sisaltaa `HealthSyncSection`-osion. Free-kayttaja voi sallia Health Connect -melusynkkauksen, ja
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
- Onnistunut restore kutsuu Settingsin restore-confirm-polusta annettua `onRestartAfterRestore`-callbackia suoraan
  `SettingsViewModel.confirmRestoreBackup(...)`-korutiinissa. `MainActivity` ajastaa puhtaan uudelleenkäynnistyksen ja
  lopettaa nykyisen prosessin, jotta suljettua Room-instanssia ei käytetä samassa prosessissa. Restart ei kulje
  `SettingsEvent`/`SharedFlow`-collector-väylän kautta.
- `DataExportSection` näyttää Local backups -kortin CSV-viennin rinnalla. Paikallinen backup/restore on Free-käyttäjillekin
  sallittu dataturvatoiminto, mutta CSV-vienti pysyy ProLockOverlayn takana.
- `MeasurementDatabaseGate` on `AudioSessionManager`in mittauselinkaaren ja `LocalBackupManager`in backup/restore-
  operaatioiden yhteinen exclusivity-portti. Mittaus pitää gaten käynnistyksestä session Room-completioniin asti;
  backup/restore pitää saman gaten koko tietokantaoperaation ajan oman sarjallistavan mutexinsa sisällä. Settingsin
  `Stop recording before managing backups` -ennakkotarkistus säilyy käyttäjäpalautteena, mutta concurrency-turva ei
  enää riipu UI:n ja käynnistetyn korutiinin välisestä ajoituksesta.

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
  Meterin ja Hearing-hubin Sleep Monitor CTA:lle. `Screen.SleepSetup` / `sleep/setup` on non-top-level route
  ja Free/deep-link execution-polku ohjataan Settingsin Pro-korttiin. Pro-käyttäjä voi valmistella 6h/8h/10h
  target-keston ja keep screen awake -option sekä käynnistää aktiivisen Sleep recordingin foreground service -polussa.
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

- `domain/` ei importtaa enää `data/`, `sync/`, `service/`, `ui/`, `widget/` tai `billing/`-paketteja eikä Android/
  AndroidX-frameworkia. Domain-mallit ja laskenta ovat nyt `domain/session`, `domain/noise`, `domain/hearingtest`,
  `domain/report`, `domain/analytics`, `domain/audio` ja `domain/entitlement` -paketeissa.
- `AudioSessionManager`, `MeasurementPersistenceSampler`, `AudioEngine`, `ToneGenerator`, `MediaPipeSoundClassifier` ja
  `AndroidAudioInputDeviceRouter` ovat `service/`-paketissa, koska ne orkestroivat repositoryja, Health Connectia,
  widget-päivityksiä tai Android audio/API -tyyppejä. `domain/audio` jäi audio-malleille, porteille ja DSP-logiikalle
  kuten `DecibelCalculator`, `FrequencyWeightingFilter`, `FFTProcessor`, `SpectralAnalyzer`, `SoundClassifier`,
  `AudioInputDeviceMapper` ja `AudioInputDeviceRouteResolver`.
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
- Gradle dependency locking on käytössä. Projektikonfiguraatioiden lock state on `settings-gradle.lockfile`- ja
  `app/gradle.lockfile`-tiedostoissa, ja root-buildscriptin plugin-/scanner-classpath on lukittu erilliseen
  `buildscript-gradle.lockfile`-tiedostoon. `settings.gradle.kts` pysäyttää Gradle-ajon, jos buildscript-lock tai
  `gradle/verification-metadata.xml` puuttuu tai on tyhjä.

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
- `domain/audio/DecibelReading` erottaa raw RMS -arvon (`instantDb`), valitun painotuksen RMS-arvon (`weightedDb`),
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
- Mittausnotificationin live dB -sisällön lukitusnäkyvyys kulkee `NotificationPrivacyPolicy`n kautta. Public-taso on
  sallittu vain ehdolla Pro + lockscreen meter + erillinen `show_lockscreen_meter_publicly` -opt-in; muuten measurement
  notification pysyy private-tasolla.
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
- `BillingRuntimeGateway.refreshPurchases()` on ostosnapshotin julkinen refresh-portti. `MainActivity.onResume()` kutsuu sitä,
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

- `domain/audio/AudioInputInfo` on `service/AudioEngine`in live input metadata -state. `AudioEngine.audioInputInfo` julkaisee
  kiintean `AudioProcessingConfig.SAMPLE_RATE` -arvon, effective selected inputin ja `AudioRecord`in routed input
  -nimen vasta, kun `AudioRecord.startRecording()` on onnistunut; state palautuu defaulttiin stop/release-polussa,
  koska Androidin routing-tieto on luotettava vain aktiivisen tallennuksen aikana.
- `domain/audio/AudioInputDevice`, `AudioInputDeviceType`, `AudioInputDeviceMapper` ja
  `AudioInputDeviceRouteResolver` muodostavat external mic -dataflow'n puhtaan domain-osan.
  `service/AndroidAudioInputDeviceDiscoveryPort` ja `service/AndroidAudioInputDeviceRouter` lukevat
  `AudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)` -source-laitteet ja reitittavat Android `AudioRecord`in.
  Pro-kayttajan `selected_audio_input_device_id` vaikuttaa vain execution-polussa, jossa `AudioSessionManager` valittaa
  effective valinnan `AudioEngine.setPreferredAudioInputDeviceId(...)` -metodille ennen starttia; Free-kayttajan
  effective arvo on null. Jos valittu external input puuttuu, resolver fallbackaa built-in mikrofoniin eika ylikirjoita
  raw preferencea.
- `AndroidAudioInputDeviceRouter` kutsuu `AudioRecord.setPreferredDevice(...)` ennen `AudioRecord.startRecording()`-
  kutsua. `sessions`-taulu tallentaa schema v9:ssa valitun/routed inputin nullable metadatan, joka kulkee
  `SessionAudioInputDeviceMetadata` -> `SessionReportData` -polkuun ja PDF Report Contextin Audio input -riviin.
- `MeterViewModel` rakentaa `MeterSessionInfoUiState`n `AudioSessionManager.isRecording` /
  `activeSessionStartTimeMs` -virroista, `ProAudioPreferencePolicy`n effective weighting/response time -arvoista seka
  `AudioEngine.audioInputInfo`sta.
- `MeterSessionInfoBar` nakyy vain aktiivisen Meter-session aikana. Free-kayttaja nakee REC-tilan, keston,
  effective weightingin ja response timen; Pro-kayttaja nakee lisaksi sample raten ja input devicen.
- `MeterScreen` keeps the display awake during active recording with `FLAG_KEEP_SCREEN_ON` through the shared
  `KeepScreenOnEffect` / `KeepScreenOnController`. The controller clears the flag when recording stops or the
  composable leaves composition. Sleep setup uses the same helper only for `isRecording && keepAwakeEnabled` and still
  does not add a separate `PowerManager.WakeLock` manager.
- `ui/common/ContextActivity.findActivity()` is the shared ContextWrapper-to-Activity helper for Compose routes that need
  an Activity or Window. Use it instead of adding more private `Context.findActivity()` copies.

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
- `analyticsSectionCards(...)` owns Analytics section and range card grouping. The Overview Weekly range renders `WEEKLY_EXPOSURE`, compact `HEARING_STATUS`, and `YEARLY_REPORT`; Monthly renders `MONTHLY_TREND`, compact `HEARING_STATUS`, and `YEARLY_REPORT`. The full status card and hearing-test action belong to the Hearing hub, not Overview. Spectral renders `SpectralAnalysisCard`;
  Environment renderoi `EnvironmentMixCard`in. Tama ei muuta `AnalyticsViewModel`in dataflow'ta tai Pro-gatingia: kaikki
  nykyiset UI-state-kentat rakennetaan edelleen samalla tavalla.

## 2026-06-12 - Sound detection infrastructure

- YAMNet assets live under `app/src/main/assets/sound_detection/`, and `YamnetModelAssets` owns the model/class-map
  asset paths.
- `YamnetAudioWindowAdapter` converts the existing 44.1 kHz PCM16 chunk stream into 16 kHz normalized float windows
  without persisting raw audio.
- `SoundClassifier` is the testable inference port. `MediaPipeSoundClassifier` is the production adapter over MediaPipe
  Tasks Audio `AudioClassifier`, and `SoundClassificationPolicy` owns confidence-threshold and empty-output mapping.
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

## 2026-06-24 - Sleep Monitor schema

- Room schema v10 adds separate `sleep_sessions` and `sleep_notable_events` tables. Ordinary session rows do not gain
  sleep-specific columns; Sleep Monitor metadata is kept in a one-to-one table keyed by `sessionId`.
- `sleep_sessions` stores `sessionId`, `targetDurationMinutes`, `keepAwakeEnabled` and `createdAt`, and cascades from
  `sessions.id` when the parent session is deleted.
- `sleep_notable_events` stores Sleep-only event rows with `sessionId`, `timestamp`, `eventType`, optional `levelDb`
  and optional `durationMs`. It references `sleep_sessions.sessionId`, so notable events cannot be saved for ordinary
  non-Sleep sessions unless a Sleep metadata row exists. The table has `sessionId,timestamp` and `timestamp` indexes.
- `SleepSessionDao` is the schema-level DAO for metadata and event rows. Active Sleep recording now writes
  `sleep_sessions` metadata through `SleepSessionRepository`; results UI and export are still future parts.
- `MIGRATION_9_10` creates the tables and indexes. Exported schema is `app/schemas/.../10.json`, and v10 identity hash
  `e4c97360fab833b6bc30549ab7e8075f` is accepted by `BackupDatabaseValidator`.

## 2026-06-24 - Sleep setup state

- `SleepSetupViewModel` publishes `SleepSetupUiState`; `availability` comes from effective
  `UserPreferences.isProUser`. `sleep_card` remains only the Meter and Hearing hub CTA visibility preference and does not
  lock a Pro user's direct `sleep/setup` preparation screen.
- Pro setup options are `targetDurationMinutes` from 6h/8h/10h and `keepAwakeEnabled`. Free state stays locked and the
  ViewModel ignores setup option changes.
- `SleepSetupScreen` shows duration chips, a keep screen awake toggle, privacy/battery copy, and active start/stop
  controls. Start launches `MeasurementForegroundService.startSleepIntent(...)` after microphone permission.

## 2026-06-25 - Sleep active recording

- `domain/sleep/SleepRecordingConfig` is the single source for Sleep Monitor target-duration options
  (6h/8h/10h) and the `keepAwakeEnabled` option consumed by setup and service start.
- `MeasurementForegroundService` supports `MeasurementRecordingMode.Meter`, `MeasurementRecordingMode.Sleep` and
  `MeasurementRecordingMode.Passive`. Sleep mode uses the same microphone foreground service as Meter, shows
  Sleep-specific notification copy, and stops automatically when the selected target duration is reached. Passive mode
  is a separate aggregate-only foreground sample, not the Sleep session path.
- `AudioSessionManager.startSleepSession(...)` uses the ordinary measurement lifecycle but writes a matching
  `sleep_sessions` row through `SleepSessionRepository` for the created `sessions.id`. Ordinary `sessions` rows still
  do not gain Sleep-specific columns.
- `ui/common/KeepScreenOnEffect` owns the `FLAG_KEEP_SCREEN_ON` acquire/release logic. Meter uses it for any active
  recording; Sleep uses it only when `isRecording && keepAwakeEnabled`, so Sleep recording continues through the
  foreground service without requiring the UI to stay awake by default.

## 2026-06-25 - Sleep results

- `domain/sleep/SleepSession` is the domain model for `sleep_sessions` metadata. `SleepSessionRepository` now exposes
  read flows for a single Sleep session and for the Sleep session ID set used by History.
- `domain/sleep/SleepResultsCalculator` builds Sleep results from existing `SessionReportData`; UI does not recalculate
  sound metrics. The summary includes target/recorded duration, equivalent level, max, LCpeak, peak event count, loud
  period count and histogram buckets.
- History keeps ordinary `Session` domain rows unchanged. `HistoryViewModel` combines the separate Sleep ID flow into
  `HistoryUiState`, and `SessionCard` renders the Sleep badge only at presentation level.
- Session Detail shows `SleepResultsCard` only when the opened session has `sleep_sessions` metadata. Existing report
  histogram and peak-event cards remain the source for the detailed distribution/event views.
- Persisted notable-event analysis remains future Osa 82+ work.

## 2026-06-25 - Sleep export/report

- `domain/report/ReportSleepSection` is the report/export model for Sleep summaries. `SessionDetailViewModel` copies the
  `SleepResultsCalculator` output into `SessionReportData.sleep`, so PDF export reads the same summary as the Session
  Detail Sleep Results card.
- `CsvExportFormatter` adds Sleep columns to the sessions CSV file: `is_sleep_session`, `sleep_target_minutes`,
  `sleep_keep_awake` and `sleep_created_at`. Non-Sleep sessions export `false` plus blank fallback fields; measurement
  and sound detection CSV files stay unchanged.
- `ExportCsvUseCase` reads Sleep metadata through `SleepSessionDao.getSleepSessionsForCsvExportByIds(...)` for the same
  session IDs selected by all/selected CSV export.
- The PDF Data Availability page includes Sleep target, recorded duration, keep-awake, loud-period and peak-event rows.
  Non-Sleep sessions show `N/A` values instead of zeros.

## 2026-06-25 - Sleep insights

- `domain/sleep/SleepInsightsCalculator` analyzes `SessionReportData.timeSeries` into loud-period Sleep notable event
  summaries. It returns `MissingMeasurements` when no time-series exists, so Sleep summaries do not present missing
  analysis as zero.
- `SleepResultsCalculator` still owns the Sleep Results summary, but peak event, loud period and sample counts are
  nullable when insight analysis is unavailable. Available quiet data can still show a real zero.
- `SessionDetailViewModel` maps `SleepInsightsSummary` into `SleepInsightsUiState`. `SessionDetailScreen` shows a Sleep
  Insights card after Sleep Results with unavailable copy for missing data, quiet summary for available no-loud-period
  data, and notable/loudest-period summary when loud periods exist.

## 2026-06-25 - Audible alarm policy

- `domain/noise/AudibleAlarmPolicy` owns audible alarm defaults: 90 dB threshold, 30 s sustained duration and 5 min
  cooldown. The model is pure domain code and has no Android audio, notification or service dependencies.
- `AudibleAlarmEvaluator` is a stateful domain evaluator that returns `BelowThreshold`, `Waiting`, `CoolingDown` or
  `Trigger`. Falling below the threshold resets the duration window, and once cooldown ends a fresh sustained-duration
  window is required before the next trigger.

## 2026-06-25 - Audible alarm playback

- `audible_alarm` is a Pro-gated DataStore preference with default OFF. Settings exposes the toggle and preview in the
  Noise Notifications card, while Free effective state remains OFF and the ViewModel refuses enable writes.
- `MediaPlayerAudibleAlarmPlayer` plays bundled `res/raw/audible_alarm.wav` through a per-play `MediaPlayer` with
  `AudioAttributes.USAGE_ALARM` and `CONTENT_TYPE_SONIFICATION`. It requests transient audio focus and releases both
  the player and focus after completion, error, focus loss or start failure. Preview uses the same lifecycle.
- `AudibleAlarmPlaybackController` bridges `AudibleAlarmEvaluator` to the Android playback port. `AudioSessionManager`
  dispatches live weighted dB readings only through the effective `isProUser && audibleAlarmEnabled` runtime state.
- `AndroidAudibleAlarmPlaybackGuard` suppresses playback when the screen is not interactive or the proximity sensor is
  covered. Guard monitoring starts with a created recording session and stops on stop, failure, cleanup and completion.

## 2026-06-25 - Voice baseline

- `domain/voice/VoiceBaselineCalibrator` aggregates weighted dB readings only while YAMNet/Sound Detection currently
  classifies live audio as `Speech`. It stores no PCM buffer, YAMNet float window or other raw voice audio.
- `AudioSessionManager.captureVoiceBaseline(...)` returns a capture only for effective Pro users during an active
  recording with runtime Sound Detection enabled. The aggregate resets during session preparation, stop/failure/cleanup
  and sound detection reset paths.
- Voice baseline persistence is DataStore-only: `voice_baseline_level_db`, `voice_baseline_sample_count` and
  `voice_baseline_captured_at_ms`. Room schema is unchanged for this feature.
- Hearing exclusively exposes the Pro-gated Voice Baseline card. Its save action is enabled only during an active
  Sound Detection measurement; Settings owns no baseline state or action.

## 2026-06-26 - Voice volume warnings

- `domain/voice/VoiceVolumeWarningEvaluator` is the single domain source for voice-volume warning decisions. It requires
  a stored voice baseline, current `Speech` classification, baseline + 8 dB for 3 seconds, and a 60 second cooldown.
- `AudioSessionManager` feeds the evaluator with the same Sound Detection classifications used by Voice Baseline and
  evaluates live weighted dB only in the effective Pro + Sound Detection + valid baseline runtime path.
- A trigger dispatches best-effort `HapticFeedbackHelper.mediumClick()` feedback and
  `NotificationHelper.sendVoiceVolumeWarning(...)` on the existing alerts notification channel. The voice warning has a
  dedicated notification ID separate from measurement, exposure and peak warnings.
- Voice volume warnings do not persist raw voice audio, PCM buffers or YAMNet windows, and they do not add a Room schema
  change or background microphone path.

## 2026-06-26 - TTS risk prompt

- `tts_risk_prompt` is a Pro-gated DataStore opt-in and defaults OFF. Settings Noise Notifications exposes the Spoken
  risk prompt toggle for Pro users; Free effective state stays OFF and the ViewModel does not persist enable attempts.
- `domain/voice/TtsRiskPromptEvaluator` is the TTS risk-prompt decision source. It accepts only dosimeter-backed
  `DOSE`/`PROJECTED_DOSE` risk events, requires Sound Detection availability plus an existing latest hearing-test
  baseline, and applies a 30 minute cooldown.
- `AudioSessionManager` observes `HearingTestRepository.getLatestResult()` during an active session only as a baseline
  existence boolean. It does not write hearing-test data or start a new recording/export path.
- `TtsRiskPromptController` bridges accepted triggers to the `TtsPromptPlayer` port. Production
  `AndroidTextToSpeechPlayer` uses Android `TextToSpeech` with `QUEUE_FLUSH`, and the manifest declares the
  `android.intent.action.TTS_SERVICE` query required for Android 11+ package visibility.
- Spoken risk prompt copy avoids diagnosis, hearing-damage, permanence and safety claims. The path does not persist raw
  audio, PCM buffers, YAMNet windows, TTS utterances or new Room data.

## 2026-06-27 - TTS short hearing recovery check

- `HearingTestMode.RECOVERY` reuses the existing full hearing test active flow but limits
  `HearingTestProcedure` to `HearingTestPolicy.RECOVERY_CHECK_FREQUENCIES` (1 kHz, 4 kHz and 8 kHz for both ears).
- `HearingRecoveryService` is the save port for short recovery checks. It requires Pro entitlement and a latest full
  hearing-test baseline, then stores only `HearingRecoveryCalculator` threshold deltas through
  `HearingRecoveryRepository`. It does not create a new full hearing-test result.
- Room schema v12 adds `hearing_recovery_results` with baseline FK, timestamp, tested count, average/max shift, status
  and serialized left/right shift data. The table stores no raw audio, PCM buffers, YAMNet windows or clinical
  audiometry records, and v12 identity hash `f73f218710d7988e02fb65939ff4fd56` is allowed by backup validation.
- Hearing hub renders `HearingRecoveryCard`. Missing-baseline state routes to the full hearing test; ready/result
  state routes through `hearing_test/recovery/setup` to `hearing_test/recovery/active`; Free users see a locked preview.
  Recovery copy must remain cautious personal tracking copy, not diagnosis, hearing-damage or safety language.

## 2026-06-28 - Tinnitus planning gate

- Tinnitus is out of v1.0 release scope. Osa 91 may proceed only as a v1.5-level personal tracking pitch profile:
  user-started ToneGenerator pitch matching, ear-specific profile and playback limits.
- Osa 91 must not include diagnosis, treatment, cure/reduction claims, hearing-damage or safety claims, Health Connect
  writes, background playback, sound therapy or automatic triggers.
- Old Osa 92 sound therapy scope remains excluded: no diagnosis, treatment, relief/cure/safety copy, symptom tracking,
  Health Connect writes or automatic triggers. The accepted Osa 92 implementation is separately scoped as ambient sound
  playback: Pro-gated, user-started local playback with visible Stop control.
- Before shipping tinnitus features, recheck Google Play health content / user data requirements, health disclaimer /
  declaration needs and FDA device software intended-use boundaries.

## 2026-06-28 - Tinnitus pitch matcher

- `domain/tinnitus/TinnitusPitchProfile` stores left/right ear pitch values and an optional updated timestamp.
  `TinnitusPitchPolicy` normalizes values into the existing hearing-test 250-8000 Hz range with 50 Hz steps and keeps
  preview amplitude fixed at -36 dB.
- DataStore keys are `tinnitus_left_pitch_hz`, `tinnitus_right_pitch_hz` and `tinnitus_pitch_updated_at_ms`. Room schema
  is unchanged. `PreferencesRepository.updateTinnitusPitchProfile(...)` is the UI-facing write port.
- Hearing hub shows `TinnitusPitchCard`, which opens the non-top-level `tinnitus/pitch` route. Free users see an
  empty/locked effective profile, and `TinnitusPitchMatcherViewModel` does not preview or persist without Pro entitlement.
- The matcher uses the existing `ToneGenerator` only from the user-triggered Preview action. It adds no
  diagnosis/treatment copy, background playback, foreground service, media notification, sound therapy, Health Connect
  writes, raw audio persistence or automatic triggers.

## 2026-06-28 - Ambient sound playback

- `domain/ambient/AmbientSoundPolicy` owns the local-only Osa 92 settings: presets `WHITE_NOISE`, `PINK_NOISE`,
  `BROWN_NOISE`, `FAN`, volume clamp `0.05f..1.0f` with default `0.35f`, and timer options `0/15/30/60/120` with
  default `30`.
- DataStore keys are `ambient_sound_preset`, `ambient_sound_volume` and `ambient_sound_timer_minutes`. There is no Room
  schema change, playback history, raw audio persistence, cloud sync or Health Connect write path.
- `AmbientSoundPlaybackService` is a separate `mediaPlayback` foreground service with
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` manifest permission and a low-importance playback notification channel. It does not
  use `MeasurementForegroundService`, `RECORD_AUDIO` or the microphone foreground-service type.
- `AmbientSoundPlayer` generates white/pink/brown/fan PCM16 locally and streams it through `AudioTrack.MODE_STREAM` using
  `USAGE_MEDIA` / `CONTENT_TYPE_MUSIC`. Audio focus permanent loss stops playback; transient loss pauses and focus gain
  resumes only that service pause.
- `AmbientSoundPlaybackViewModel` is the execution gate: Free users cannot start or persist settings, Android 13+
  notification permission denial blocks Play, and the timer only stops already user-started playback.
- Hearing hub shows `AmbientSoundCard` near tinnitus pitch and opens the non-top-level `ambient/playback` route.
  Copy uses ambient/local playback terms and avoids therapy, treatment, relief, cure, safety and hearing-protection
  claims.

## 2026-06-29 - Release-readiness QA and documentation sync

- Osa 93 accessibility audit is source/preview-contract level, not a full manual TalkBack sign-off. Guarded surfaces
  include Meter custom controls, SessionCard edit target sizing and ambient sound selected-state chips.
- Osa 94 launch localization baseline is default English plus a first `values-fi/strings.xml` subset for Osa 89-92
  surfaces and shared action/accessibility resources. `LocalizationBaselineTest` guards placeholder parity and scans new
  UI surfaces for hardcoded Compose text; full-app localization remains incomplete.
- Osa 95-98 QA evidence is document-backed in `docs/qa/permission-device-qa-matrix.md`,
  `docs/qa/billing-production-qa.md`, `docs/qa/release-signing-qa.md` and `docs/qa/qodana-ci-compatibility.md`.
  Device smoke, Play Console product verification, signed Play-ready AAB verification, Play upload and real Qodana
  execution remain release risks when marked `NOT RUN` or `NOT VERIFIED`.
- Qodana remains intentionally non-blocking for the AGP 9.1.0 project until a real Qodana run proves compatibility. The
  workflow exposes this through `Qodana Analysis (non-blocking AGP 9.1 risk)`, `continue-on-error: true` and a
  `GITHUB_STEP_SUMMARY` risk note.
- Current `sc` output does not include `reports/security-code.txt`. Read code-security results from
  `security-summary.txt`, `semgrep-kotlin.txt`, `semgrep-secrets.txt`, `gitleaks.txt` and `trufflehog.txt`; read
  dependency/security dependency results from `security-deps.txt`, `security-deps-raw.txt` and `osv.txt`.

## 2026-06-26 - Passive monitoring foreground sample

- Passive monitoring is implemented only as a user-started short foreground-service sample from Settings. The Noise
  Notifications card shows disclosure copy, requests microphone permission from the user action, and starts
  `MeasurementForegroundService.startPassiveMonitoringIntent(...)`.
- `MeasurementRecordingMode.Passive` uses the existing microphone foreground service and ongoing notification, but stop
  calls `PassiveMonitoringManager.stopMonitoring()` and does not emit a completed session.
- `PassiveMonitoringManager` does not call `AudioSessionManager.startSession()`, does not create `SessionEntity`, and
  does not write `measurements` rows. It disables Sound Detection and spectral processing, does not start WAV writing,
  audible alarm, voice warning or noise alert triggers, and persists only aggregate samples.
- Room schema v11 adds `passive_monitoring_samples` with started/ended timestamps, reading count, min/avg/max/peak and
  `totalEnergy`. `PassiveMonitoringRepository.observeDailySummary(...)` powers the Settings daily summary, and Clear
  history deletes passive monitoring summaries.
- Do not add microphone sampling from boot, timers, receivers, WorkManager or other background triggers without a new
  explicit decision. Do not persist raw audio, PCM buffers or YAMNet windows for passive monitoring.

## 2026-06-24 - Noise notification schedule model

- `domain/noise/NoiseNotificationSchedule` is the single domain source for notification active day/hour windows. It uses
  `java.time.DayOfWeek`, start/end minute-of-day values and `isActiveAt(ZonedDateTime)` without UI or Android
  notification dependencies.
- Matching start and end minutes mean the full selected day. `startMinuteOfDay < endMinuteOfDay` is a same-day window
  with an exclusive end. `startMinuteOfDay > endMinuteOfDay` crosses midnight; the morning segment belongs to the
  previous active day.
- DataStore keys are `notification_schedule_active_days`, `notification_schedule_start_minute` and
  `notification_schedule_end_minute`. The default is all days, full day. Days are persisted as ISO-8601
  `DayOfWeek.value` values; a blank string means no active days, an invalid non-blank list falls back to all days, and
  minutes are clamped to 0..1439.
- The Settings Noise Notifications card reads `SettingsUiState.notificationSchedule`, shows active days as chips and
  start/end hours as sliders, and writes changes through `NoiseNotificationUpdate.NotificationSchedule` to
  `PreferencesRepository.updateNotificationSchedule(...)`. The UI offers hour-level controls while the DataStore/domain
  model keeps minute-of-day precision.
- `AudioSessionManager` now passes `NoiseNotificationSchedule` and live `LiveExposureState` dosimeter values into
  `NoiseAlertEvaluator`. The evaluator checks the schedule before attempting exposure or peak alerts.
- Extended exposure alerts can trigger from the 30 minute threshold average, 100% actual dose, or 100% projected dose.
  `NoiseAlertPolicy` owns those limits plus the 120 dB peak limit and 30 minute retry cooldown. Failed delivery retries
  after cooldown; successful delivery suppresses the same alert type for the rest of the session.

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
- Room-skeema on v10. V8:ssa `calibration_profiles` tallentaa UI:sta riippumattomat calibration profile -rivit: `id`, `name`,
  `micSensitivityOffset`, `octaveBandOffsets`, `isDefault`, `createdAt` ja `updatedAt`. `CalibrationProfileRepository`
  mapittaa Room-rivit domain-malliksi, tarjoaa `createProfile(...)`, `observeProfiles()`, `getProfile(...)`,
  `renameProfile(...)`, `deleteProfile(...)`, `updateOctaveBandOffsets(...)` ja `resetOctaveBandOffsets(...)` -polut,
  normalisoi flat- ja octave-offsetit yhteisella `CalibrationOffsetPolicy`lla ja estää viimeisen `isDefault`-profiilin
  poiston data-kerroksessa. V8:n Room identity hash on lisätty `BackupDatabaseValidator`in sallittuihin hasheihin, jotta
  uudet paikallisbackupit läpäisevät restore-validaation.
- Room-skeema v9 lisää `sessions.selectedAudioInputDeviceId`, `selectedAudioInputDeviceName` ja
  `routedAudioInputDeviceName` -sarakkeet external mic -raportointia varten. V9:n identity hash
  `5b73e542adc2464266a32a6c3d216e15` on mukana `BackupDatabaseValidator`in sallituissa hasheissa.
- Room-skeema v10 lisää Sleep Monitorin erilliset `sleep_sessions`- ja `sleep_notable_events`-taulut. Sleep metadataa
  ei lisätä tavalliseen `sessions`-tauluun, ja v10:n identity hash `e4c97360fab833b6bc30549ab7e8075f` on mukana
  `BackupDatabaseValidator`in sallituissa hasheissa.
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

### Jul 10, 2026

- Non-top-level Pro-routejen yhteinen `ProRouteAccessGate` odottaa ratkaistua entitlementia ennen sisällön luontia.
  Free-entry reiteille `tinnitus/pitch`, `ambient/playback`, `hearing_test/recovery/setup` ja
  `hearing_test/recovery/active` ohjataan Settingsin Pro-korttiin. ViewModel- ja service-tason execution-gatet
  säilyvät toisena suojakerroksena. Sleep setup käyttää edelleen omaa Loading/Locked/Ready-entrytilaansa.

### Jul 15, 2026 - Export-aikojen locale- ja aikavyöhykevakaus

- Room-skeema v13 lisää nullable `sessions.startUtcOffsetSeconds`- ja `endUtcOffsetSeconds`-sarakkeet. Uuden session
  alkuoffset tallennetaan `SessionRepository.createActiveSession(...)`-polussa ja loppuoffset completion-transaktiossa.
  Legacy-rivejä ei backfillata nykyisellä aikavyöhykkeellä, koska alkuperäistä offsetia ei voida päätellä luotettavasti.
  Interrupted-session recovery jättää menneen loppuhetken offsetin `null`-arvoksi erillisellä recovered-completion-polulla.
- `SessionTimeZoneOffsets` on historiallisen offsetin domain-lähde `Session` -> `SessionReportData` -dataflow'lle.
  PDF ja Session Detail PNG näyttävät persistoidun offsetin; tuntematon offset fallbackaa eksplisiittiseen UTC-aikaan.
  Health Connectin `ExerciseSessionRecord.startZoneOffset` ja `endZoneOffset` käyttävät samoja persistoidun session
  arvoja.
- `CsvExportFormatter` kirjoittaa `_utc`-aikasarakkeet `DateTimeFormatter.ISO_INSTANT` -muodossa. CSV-numeroiden
  pisteellinen koneformaatti ja DAO-kyselyiden deterministiset `timestamp,id`-tie-breakerit säilyvät ennallaan.
