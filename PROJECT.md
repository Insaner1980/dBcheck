# dBcheck

**Premium Android-desibellimittari ja kuuloterveys-sovellus.**

Kotlin / Jetpack Compose -sovellus joka mittaa ympariston melunsaa reaaliajassa, seuraa altistumishistoriaa, tarjoaa analytiikkaa ja kuulotestin. Visuaalinen identiteetti: "Auditory Observatory" - rauhallinen, editorial wellness-instrumentti, ei geneerinen tyokaluapp.

---

## Tekniikkapino

| Teknologia | Versio | Kayttotarkoitus |
|---|---|---|
| Kotlin | 2.3.20 | Kieli |
| Jetpack Compose | BOM 2026.03.00 | UI |
| Material 3 | (BOM) | Komponenttikirjasto (custom-teemat) |
| AGP | 9.1.0 | Build |
| Gradle | 9.4.1 | Build tool |
| Hilt | 2.59.2 | Dependency injection |
| KSP | 2.3.6 | Annotation processing |
| Room | 2.8.4 | Lokaali tietokanta |
| DataStore | 1.1.4 | Asetukset/preferenssit |
| Navigation Compose | 2.9.7 | Nayttoreititys |
| Google Play Billing KTX | 8.3.0 | Pro-ominaisuuksien osto |
| Glance | 1.1.1 | Kotinayton widget |
| JVM | 21 | Target |
| Min SDK | 26 (Android 8.0) | |
| Target SDK | 36 | |

> Vico 3.0.1 poistettu — kaikki kaaviot toteutettu custom Canvas-composableilla.

---

## Arkkitehtuuri

Single Activity + Compose Navigation. MVVM-pattern.

```
com.dbcheck.app/
├── MainActivity.kt / DbCheckApplication.kt
├── di/                     Hilt DI -modulit
├── ui/
│   ├── theme/              Design system (Color, Type, Shape, Spacing, Gradient, Theme)
│   ├── components/         13 uudelleenkaytettavaa komponenttia
│   ├── navigation/         Screen routes, NavHost, BottomNavDestination
│   ├── meter/              Paaruutu: live dB-mittari
│   ├── analytics/          Viikkoanalytiikka, kuuloterveys, Pro-ominaisuudet
│   ├── history/            Sessiohistoria, 24h-kaavio, trendit
│   ├── settings/           Kalibrointi, ilmoitukset, ulkoasu, Pro-upsell
│   └── hearingtest/        Kuulotesti-flow (setup -> active -> results)
├── data/
│   ├── local/db/           Room: entities, DAOs, database
│   ├── local/preferences/  DataStore
│   ├── repository/         SessionRepository, MeasurementRepository, PreferencesRepository
│   └── model/              Domain-mallit (Session, NoiseLevel)
├── domain/
│   ├── audio/              AudioEngine, DecibelCalculator, FrequencyWeightingFilter,
│   │                       FFTProcessor, ToneGenerator, AudioSessionManager
│   └── usecase/            ExportCsvUseCase
├── service/                MeasurementForegroundService, NotificationHelper
├── billing/                BillingManager, ProFeatureManager
├── widget/                 Glance-widget (DbCheckWidget, DbCheckWidgetReceiver)
├── sync/                   CloudBackupManager
└── util/                   HapticFeedbackHelper, ShareResultsGenerator
```

---

## Design system

**Varijarjestelma:** 18+ token-paria dark/light-teemoille. Paagradientti: lime (#C5FE00) -> lemon (#DFEC60) dark / (#466906 -> #954B00) light. Ei 1px-bordereita, syvyys tonaalisen kerrostumisen kautta.

**Typografia:** Manrope (headline, body, nav) + Space Grotesk (kaikki numerot, ml. displayLg/displayMd). 12 typografia-tokenia display-lg:sta (56sp) label-sm:aan (11sp).

**Muotoilu:** Min corner radius 8dp. Spacing 8dp-gridilla (4dp-64dp). Glassmorphism kelluvissa elementeissa (blur API31+, fallback solid).

---

## Naytorakenne

### 4-tab bottom navigation

| Tab | Naytto | Kuvaus |
|---|---|---|
| 1 | **Meter** | Live dB-mittari: pyorea gauge (gradient arc, breathing pulse, noise level -pill gaugen sisalla), MIN/AVG/MAX-kortit, Play/Pause/Reset/Share. Melukynnykset: 0-40 Quiet, 40-70 Normal, 70-85 Elevated, 85+ Dangerous. Mic denied -prompt Settings-linkilla. |
| 2 | **Analytics** | Viikon altistumisyhteenveto (bar chart paivatunnisteilla), kuuloterveys-kortti, spektrianalyysi (Pro, esikatselulla), ymparistomix (Pro, kategoriajakauma), kuulotesti-CTA (Pro). Upgrade-napit navigoivat Settings Pro -korttiin. |
| 3 | **History** | 24h area/line chart (aikaksimerkinnat), sessiolista (aika-perusteiset automaattinimet free-kayttajille), viikkotrendi + turvalliset tunnit. Free-kayttajat: 7pv rajoitus. |
| 4 | **Settings** | Mikrofoni kalibrointi (Pro, arvo rivin oikealla puolella), taajuuspainotus A/C/Z (Pro), ilmoituskynnys (vaihteluvalikkomerkinnot), teema (system/dark/light chips), Pro-upsell-kortti (gradient-reunus), versio + privacy/terms footer. Person-ikoni oikealla. |

### Erillinen flow

| Flow | Kuvaus |
|---|---|
| **Hearing Test** | Full-screen 3-vaiheinen: Setup (ohjeet) -> Active (12 vaihetta: 6 taajuutta x 2 korvaa, Hughson-Westlake 10-down/5-up) -> Results (audiogrammi, key metrics, arvioitu speech clarity + high freq, disclaimer). Pro-ominaisuus. |

---

## Ominaisuudet (Free vs Pro)

| Ominaisuus | Free | Pro |
|---|---|---|
| Live dB-mittari (MIN/AVG/MAX) | x | x |
| Melutasoilmoitukset (85dB) | x | x |
| Dark/Light-teema | x | x |
| 7 paivan historia | x | x |
| Viikon altistumiskaavio | x | x |
| Kuuloterveys-status | x | x |
| Rajoittamaton historia | | x |
| Spektrianalyysi | | x |
| Ymparistomix | | x |
| Session-nimeaminen & -taggert | | x |
| CSV-vienti | | x |
| Mikrofoni kalibrointi | | x |
| Taajuuspainotus (C/Z) | | x |
| Kotinayton widget | | x |
| Kuulotesti | | x |

**Monetisaatio:** Kertaosto (INAPP), ei tilausta, ei mainoksia. Google Play Billing KTX 8.3.0 suspend-funktioilla. acknowledgePurchase vaaditaan. BillingManager alustetaan DbCheckApplication.onCreate():ssa.

**Pro UI -saanto:** Pro-ominaisuudet nayttavat esikatselun ProLockOverlay:lla (blur/dim + lukko + Upgrade-nappi). Sisaltoa ei koskaan piiloteta kokonaan. Upgrade-napit navigoivat Settings-nayton Pro-korttiin (auto-scroll).

---

## Audio engine

- **AudioRecord** 44.1kHz mono PCM16, 4096 sample buffer, `@Volatile isRecording`
- **DecibelCalculator:** RMS -> 20*log10(rms/32768) + 90 + kalibraatio. Silence -> 0 dB (ei -infinity). Vaihteluvalilla 0-130 dB.
- **FrequencyWeightingFilter:** 3 kaskaadi-biquad-sektiota A-painotukselle (IEC 61672:2003), 2 sektiota C-painotukselle. Per-sektion tila. Kertoimet likimaaraiset — tuotantoversiolle verifioitava scipy/MATLAB:lla.
- **FFTProcessor:** 4096-point Cooley-Tukey, Hann window. Power-of-2 pakotettu `Integer.highestOneBit()`:lla. DC-bin ohitetaan dominanttitaajuushaussa.
- **ToneGenerator:** AudioTrack MODE_STATIC sine wave, 50ms linear fade in/out, kuulotestia varten.
- **AudioSessionManager:** Session lifecycle, batch DB writes ~1s valein (10 lukemaa). MIN/AVG/MAX (weighted) + PEAK (instant) seuranta. Thread-safe `synchronizedList`. `startTime` sailytetaan oikein Room @Update:ssa. Paivittaa kotinayton widgetin session paattyessa.
- **ForegroundService:** FOREGROUND_SERVICE_TYPE_MICROPHONE. Live-paivitys 2s valein (dB + kesto). Kaynnistetaan/pysaytetaan MeterViewModel:sta.

---

## Oikeudet

- `RECORD_AUDIO` — mikrofoni (runtime-pyynto, denied-prompt Settings-linkilla)
- `POST_NOTIFICATIONS` — ilmoitukset (runtime-pyynto Android 13+, app toimii ilmankin)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` — taustamittaus
- `VIBRATE` — haptiikka
- `BILLING` — Google Play ostot

---

## Tietokanta

**Room** kolmella entiteetilla:
- `sessions` - mittaussessiot (start/end, min/avg/max/peak dB, nimi, emoji, tagit)
- `measurements` - yksittaiset mittaukset (FK session, timestamp, dB, weighted dB)
- `hearing_test_results` - kuulotestin tulokset (score, rating, vasen/oikea korva data, speech clarity)

**DataStore** preferensseille: teema, ilmoitukset, kalibraatio, Pro-status (debug-buildissa oletuksena Pro).

---

## Widget

Glance 1.1.1 -widget `GlanceTheme`-wrapperilla. Kolme tilaa:
- **Pro + sessiodata:** Viimeinen mitattu dB, melutasotunniste, suhteellinen aika
- **Pro + ei dataa:** "No data yet — Tap to start measuring"
- **Free:** Lukko-ikoni + "dBcheck Pro" / "Upgrade to unlock"

Paivittyy: 15min (updatePeriodMillis) + heti session paattyessa (`DbCheckWidgetReceiver.updateAllWidgets()`).

---

## Toteutusvaiheet

### Phase 1 - MVP (toteutettu)
- Projektirakenne, design system, komponenttikirjasto
- 4 nayttoa: Meter, Analytics, History, Settings
- Audio engine, Room DB, DataStore
- Pro billing (Google Play Billing), feature gating

### Phase 2 - Enhancement (toteutettu)
- Kuulotesti (3-vaiheinen flow, Hughson-Westlake algoritmi)
- FFT/spektrianalyysi
- Ymparistomix
- Session-nimeaminen (bottom sheet, emoji picker, tag chips)
- CSV-vienti (FileProvider + Intent.ACTION_SEND)
- Glance-widget

### Phase 3 - Polish (toteutettu)
- Cloud backup (lokaali backup, Google Drive placeholder)
- Sosiaalinen jakaminen (teksti + kuva)
- Kuukausi/vuosianalytiikka (MonthlyTrendChart, YearlyReportCard)

### Koodiauditointi (tehty)
- Hallusinoidut API:t korjattu (Billing KTX, A-painotuskerroin, FFT)
- Design spec -vastaavuus tarkistettu ja korjattu kaikille naytoille
- Audio engine -matematiikka ja logiikka auditoitu ja korjattu
- Kuulotestin Hughson-Westlake-algoritmi korjattu
- Oikeudet, foreground service ja billing-alustus kytketty
- Widget toteutettu oikealla datalla ja Pro-gatella
- TODO-kommentit siivottu, lint lapaisy ilman virheita

---

## Referenssitiedostot

| Tiedosto | Tarkoitus |
|---|---|
| `dBcheck_design_spec.md` | Ainoa totuuden lahde designille |
| `html/*.html` | Tailwind-mockupit variarvojen poimimiseen |
| `images/*.png` | Visuaaliset referenssit mittasuhteille |

---

## Kehitysymparisto

```bash
# Build
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug

# Lint
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew lintDebug

# Asenna laitteelle
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Vaatii: JDK 21, Android SDK (API 36), Gradle 9.4.1.

---

## Tunnetut rajoitukset

- **A-painotuskertoimet** ovat likimaaraisia — oikea rakenne (3 kaskaadi-biquad) mutta tarkat arvot verifioitava scipy/MATLAB:lla ennen tuotantoa
- **Kuulotestin kynnykset** ovat dBFS:ssa (ei dB HL) — suhteellinen seuranta toimii, absoluuttinen audiometria vaatisi kuulokekohtaisen kalibraation
- **speechClarity** ja **highFreqLimit** ovat arvioita testidatan perusteella, eivat mitattuja
- **Yksikkotestit puuttuvat** — audio-matematiikka ja billing-logiikka ovat suurimmat regressioriskit
- **Share- ja View All -napit** eivat viela tee mitaan (tyhjat lambdat)
