# dBcheck - Projektin tilannekatsaus

**Paivitetty:** 2026-03-28

---

## Yhteenveto

dBcheck on Android-desibellimittari ja kuuloterveys-sovellus (Kotlin/Jetpack Compose). Kaikki kolme toteutusvaihetta (MVP, Enhancement, Polish) on koodattu ja koodiauditointi tehty. **Sovellus buildaa**, mutta se ei ole viela tuotantovalmis. Alla tarkempi erittely.

---

## Toteutetut ominaisuudet

### Phase 1 - MVP (valmis)

| Ominaisuus | Tila | Huomiot |
|---|---|---|
| Projektirakenne + Hilt DI | OK | AppModule, DatabaseModule |
| Design system (Color, Type, Shape, Spacing, Gradient) | OK | Manrope + Space Grotesk fontit mukana |
| 13 uudelleenkaytettavaa UI-komponenttia | OK | ProLockOverlay, DbCheckButton, SessionCard jne. |
| Meter-naytto (live dB, gauge, MIN/AVG/MAX) | OK | CircularGauge, NoiseLevelPill, StatCard, WaveformVisualization |
| Analytics-naytto (viikkokaavio, kuuloterveys) | OK | WeeklyBarChart, HearingHealthCard, ExposureSummaryCard |
| History-naytto (24h-kaavio, sessiolista) | OK | Last24HoursChart, WeeklyTrendCard, SafeHoursCard |
| Settings-naytto (teema, ilmoitukset, Pro-kortti) | OK | DisplayAppearanceSection, NoiseNotificationsSection, ProUpsellCard |
| Audio engine (AudioRecord, dB-laskenta) | OK | 44.1kHz mono PCM16, 4096 sample buffer |
| FrequencyWeightingFilter (A/C/Z) | OK | 3 kaskaadi-biquad A:lle, 2 C:lle |
| Room-tietokanta (3 entiteettia, 3 DAO:a) | OK | sessions, measurements, hearing_test_results |
| DataStore-preferenssit | OK | UserPreferencesDataStore |
| Google Play Billing (KTX suspend) | OK | BillingManager, ProFeatureManager, acknowledge |
| Bottom navigation (4 tabia) | OK | + NavigationRail tableteille (>=600dp) |
| Foreground service (live-paivitys) | OK | MeasurementForegroundService, NotificationHelper |
| AndroidManifest oikeudet | OK | RECORD_AUDIO, POST_NOTIFICATIONS, FOREGROUND_SERVICE_MICROPHONE, VIBRATE, BILLING |

### Phase 2 - Enhancement (valmis)

| Ominaisuus | Tila | Huomiot |
|---|---|---|
| Kuulotesti (3-vaiheinen flow) | OK | Setup -> Active -> Results, Hughson-Westlake |
| FFT/spektrianalyysi | OK | FFTProcessor 4096-point, SpectralAnalysisCard |
| Ymparistomix | OK | EnvironmentMixCard |
| Session-nimeaminen (bottom sheet) | OK | SessionNamingSheet (emoji picker, tag chips) |
| CSV-vienti | OK | ExportCsvUseCase + FileProvider |
| Glance-widget | OK | DbCheckWidget, DbCheckWidgetReceiver, 3 tilaa |
| ToneGenerator kuulotestiin | OK | AudioTrack MODE_STATIC, fade in/out |

### Phase 3 - Polish (valmis)

| Ominaisuus | Tila | Huomiot |
|---|---|---|
| Lokaali backup | OK | CloudBackupManager (kopiointi, listaus, palautus) |
| Sosiaalinen jakaminen | OK | ShareResultsGenerator (teksti + kuva-kortti) |
| Kuukausi/vuosianalytiikka | OK | MonthlyTrendChart, YearlyReportCard |

### Koodiauditointi (tehty)

- Hallusinoidut API-kutsut korjattu (Billing KTX, FFT, painotuskertoimet)
- Design spec -vastaavuus tarkistettu
- Audio-matematiikka auditoitu
- Kuulotestin algoritmi korjattu
- Oikeudet, service ja billing kytketty
- Lint lapaisy ilman virheita

---

## Tekemattomat / keskeneraiset asiat

### Kriittiset (estaa julkaisun)

| Asia | Kuvaus | Prioriteetti |
|---|---|---|
| **Yksikkotestit puuttuvat kokonaan** | Ei yhtaan unit- tai instrumentaatiotestia. `app/src/test/` ja `app/src/androidTest/` ovat tyhjia. Vain screenshot-testeja (ComponentScreenshotTests.kt). Audio-matematiikka ja billing-logiikka suurimmat regressioriskit. | P0 |
| **Share-napit eivat toimi** | MeterScreen `onShare = { }` ja HearingTestResultsScreen `onShare = { }` ovat tyhjia lambdoja. ShareResultsGenerator on toteutettu mutta ei kytketty. | P0 |
| **View All -nappi ei toimi** | HistoryScreen "View All" -nappi `onClick = { }` on tyhja lambda. | P1 |
| **A-painotuskertoimet verifoimatta** | IIR-kertoimet ovat likimaaraisia. Tuotantoversio vaatii scipy/MATLAB-verifioinnin. | P1 |
| **Kuulotesti dBFS vs dB HL** | Kynnykset ovat dBFS:ssa, eivat kalibroituja dB HL -arvoja. Suhteellinen seuranta toimii, absoluuttinen audiometria ei. | P1 |

### Tarkeita (julkaisua edeltavia)

| Asia | Kuvaus | Prioriteetti |
|---|---|---|
| **Google Drive -integraatio puuttuu** | CloudBackupManager tukee vain lokaaleja varmuuskopioita. Google Drive on placeholder-kommenttina. | P2 |
| **ProGuard-saannot minimaaliset** | Vain Room, Hilt ja Billing katettu. Compose, Navigation, DataStore, Glance puuttuvat. Release-buildissa voi tulla ongelmia. | P2 |
| **App-ikoni placeholder** | Vain adaptive icon XML:t (ic_launcher.xml/ic_launcher_round.xml), eivat varsinaisia bitmap-resursseja (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi puuttuvat). | P2 |
| **FileProvider file_paths.xml** | Olemassa mutta sisaltoa ei verifioitu - CSV-vienti ja kuva-jakaminen vaativat oikeat polut. | P2 |
| **speechClarity ja highFreqLimit arvioita** | Kuulotestin tulokset perustuvat estimaatteihin, eivat mitattuihin arvoihin. | P2 |

### Hyva olla (jatkokehitys)

| Asia | Kuvaus | Prioriteetti |
|---|---|---|
| **Onboarding-flow puuttuu** | Ei ensimmaisenkaynnistyksen ohjausta tai mikrofoniluvan pyyntoa ennen Meter-nayttoa. | P3 |
| **Deep link -tuki** | Ei toteutettu. | P3 |
| **Accessibility** | Ei testattu TalkBack-yhteensopivuutta, contentDescription-arvoja puuttunee. | P3 |
| **Lokalisointi** | Kaikki tekstit kovakoodattu englanniksi. Ei strings.xml-kaantoja (fi, jne.). | P3 |
| **Play Store -materiaali** | Ei kuvakaappauksia, feature graphic:ia tai store-kuvausta. | P3 |
| **CI/CD** | Ei automaattista build/test-putkea (GitHub Actions tms.). | P3 |
| **Detekt-saantojen hienosäato** | detekt.yml ja baseline olemassa mutta saannot eivat ehka ole optimaaliset. | P3 |
| **Crash-raportointi** | Ei Firebase Crashlytics:ia tai vastaavaa integraatiota. | P3 |
| **Analytics** | Ei Firebase Analytics:ia tai vastaavaa kayttoanalytiikkaa. | P3 |

---

## Tekninen tila

| Osa-alue | Tila |
|---|---|
| **Kotlin-tiedostoja** | 92 kpl |
| **Build** | Compiloi (assembleDebug) |
| **Lint** | Lapaisy ilman virheita |
| **Detekt** | Konfiguroitu, baseline olemassa |
| **Screenshot-testit** | 4 preview-testia (Button dark/light, Card dark/light) |
| **Yksikkotestit** | 0 |
| **Instrumentaatiotestit** | 0 |
| **Versio** | 1.0.0 (versionCode 1) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 36 |
| **JDK** | 21 |

### Riippuvuudet (paaversiot)

| Riippuvuus | Versio |
|---|---|
| AGP | 9.1.0 |
| Kotlin | 2.3.20 |
| Compose BOM | 2026.03.00 |
| Hilt | 2.59.2 |
| Room | 2.8.4 |
| Billing KTX | 8.3.0 |
| Glance | 1.1.1 |
| Navigation Compose | 2.9.7 |

---

## Suositeltu etenemisjarjestys

1. **Kytke Share-toiminnallisuus** - ShareResultsGenerator on valmis, pitaa vain kytkea MeterScreen ja HearingTestResultsScreen lambdoihin
2. **Toteuta View All -nappi** - Nayta kaikki sessiot (ei vain viimeisimmat)
3. **Kirjoita yksikkotestit** - Aloita kriittisimmista: DecibelCalculator, FrequencyWeightingFilter, FFTProcessor, BillingManager, AudioSessionManager
4. **Verifoi A-painotuskertoimet** - Vertaa scipy/MATLAB-referenssiarvoihin
5. **Taydenna ProGuard-saannot** - Lisaa Compose, Navigation, DataStore, Glance
6. **Suunnittele app-ikoni** - Luo bitmap-resurssit kaikille tiheysluokille
7. **Testaa release-build** - Varmista R8/ProGuard ei riko mitaan
