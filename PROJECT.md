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
| Vico | 3.0.1 | Kaaviot (bar chart, line chart) |
| Google Play Billing | 8.3.0 | Pro-ominaisuuksien osto |
| Glance | 1.1.1 | Kotinayton widget |
| JVM | 21 | Target |
| Min SDK | 26 (Android 8.0) | |
| Target SDK | 36 | |

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

**Typografia:** Manrope (headline, body, nav) + Space Grotesk (kaikki numerot). 12 typografia-tokenia display-lg:sta (56sp) label-sm:aan (11sp).

**Muotoilu:** Min corner radius 8dp. Spacing 8dp-gridilla (4dp-64dp). Glassmorphism kelluvissa elementeissa (blur API31+, fallback solid).

---

## Naytorakenne

### 4-tab bottom navigation

| Tab | Naytto | Kuvaus |
|---|---|---|
| 1 | **Meter** | Live dB-mittari: pyorea gauge (gradient arc, breathing pulse), MIN/AVG/MAX, Play/Pause/Reset/Share. Melukynnykset: 0-40 Quiet, 40-70 Normal, 70-85 Elevated, 85+ Dangerous |
| 2 | **Analytics** | Viikon altistumisyhteenveto (bar chart), kuuloterveys-kortti, spektrianalyysi (Pro), ymparistomix (Pro), kuulotesti-CTA |
| 3 | **History** | 24h area/line chart, sessiolista (emoji + nimi + metadata + stats), viikkotrendi + turvalliset tunnit |
| 4 | **Settings** | Mikrofoni kalibrointi (Pro), taajuuspainotus A/C/Z (Pro), ilmoituskynnys, teema (system/dark/light), Pro-upsell |

### Erillinen flow

| Flow | Kuvaus |
|---|---|
| **Hearing Test** | Full-screen 3-vaiheinen: Setup (ohjeet) -> Active (12 vaihetta: 6 taajuutta x 2 korvaa, Hughson-Westlake) -> Results (audiogrammi, key metrics, save/share) |

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
| Taajuuspainotus (A/C/Z) | | x |
| Kotinayton widget | | x |
| Kuulotesti | | x |

**Monetisaatio:** Kertaosto, ei tilausta, ei mainoksia.

---

## Audio engine

- **AudioRecord** 44.1kHz mono PCM16, 4096 sample buffer
- **DecibelCalculator:** RMS -> dB SPL (~90dB offset + kalibraatio)
- **FrequencyWeightingFilter:** IIR A/C/Z-painotus (IEC 61672:2003)
- **FFTProcessor:** 4096-point Cooley-Tukey, Hann window, magnitude spectrum
- **ToneGenerator:** AudioTrack sine wave, fade in/out, kuulotestia varten
- **AudioSessionManager:** Session lifecycle, batch DB writes 1s valein, MIN/AVG/MAX/PEAK seuranta
- **ForegroundService:** FOREGROUND_SERVICE_TYPE_MICROPHONE, persistent notification

---

## Tietokanta

**Room** kolmella entiteetilla:
- `sessions` - mittaussessiot (start/end, min/avg/max/peak dB, nimi, emoji, tagerst)
- `measurements` - yksittaiset mittaukset (FK session, timestamp, dB, weighted dB, FFT data)
- `hearing_test_results` - kuulotestin tulokset (score, rating, vasen/oikea korva data, speech clarity)

**DataStore** preferensseille: teema, ilmoitukset, kalibraatio, Pro-status.

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
JAVA_HOME=/usr/lib/jvm/java-21-openjdk gradle assembleDebug

# Asenna laitteelle
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Vaatii: JDK 21, Android SDK (API 36), Gradle 9.4.1.
