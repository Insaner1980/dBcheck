# dBcheck - Claude Code -ohjeet

## Projektin yleiskuvaus

Android-desibellimittari Kotlin/Jetpack Compose. ~100 Kotlin-tiedostoa, MVVM-arkkitehtuuri, Hilt DI, Room DB. Katso `PROJECT.md` kattava kuvaus ja `dBcheck_design_spec.md` design-spesifikaatio.

## Kriittiset saannot

1. **`dBcheck_design_spec.md` on ainoa totuuden lahde.** Jos HTML-mockupit tai PNG-kuvat ovat ristiriidassa, seuraa specia.
2. **Ei 1px-bordereita sektioinnissa.** Syvyys luodaan tonaalisen kerrostumisen kautta (surface -> surface-container -> surface-container-high). Poikkeus: Pro upsell -kortin gradient-reunus.
3. **Ei Material elevation -varjoja.** Kayta tonal layering + ambient shadow (4% opacity, 12dp offset, 24dp blur).
4. **Min corner radius 8dp** kaikissa containereissa.
5. **Manrope** teksteille, **Space Grotesk** kaikille numeroille (ml. displayLg ja displayMd). Ei poikkeuksia.
6. **Ei dynamicDarkColorScheme/dynamicLightColorScheme.** Brandivarit aina.
7. **Pro-ominaisuudet** nayttavat esikatselun ProLockOverlay:lla, eivat piilota sisaltoa kokonaan. Upgrade-napit navigoivat Settings Pro -korttiin.
8. **Molemmat teemat** (dark/light) testattava jokaiselle naytolle ja komponentille.

## Arkkitehtuurikonventiot

- **Single Activity** + Compose Navigation, ei fragmentteja
- **MVVM:** Screen -> ViewModel (StateFlow<UiState>) -> Repository -> Room/DataStore
- **Hilt:** @HiltViewModel, @AndroidEntryPoint, @Singleton. Widget kayttaa @EntryPoint.
- **KSP** Room ja Hilt compiler-prosessointiin
- **Coroutines:** collectAsStateWithLifecycle() UI:ssa, Dispatchers.Default audio-prosessointiin
- **Billing:** KTX suspend-funktiot (queryProductDetails, acknowledgePurchase, queryPurchasesAsync). Alustetaan DbCheckApplication.onCreate():ssa.

## Pakettirakenne

```
com.dbcheck.app/
  di/          - Hilt-modulit (AppModule, DatabaseModule)
  ui/theme/    - DbCheckTheme, DbCheckColorScheme, DbCheckTypography, DbCheckSpacing
  ui/components/ - 13 uudelleenkaytettavaa composable-komponenttia (ProLockOverlay, DbCheckButton, SessionCard, jne.)
  ui/navigation/ - Screen sealed class (Settings ROUTE_WITH_ARGS), DbCheckNavHost, BottomNavDestination
  ui/{meter,analytics,history,settings}/ - Nayttokohtaiset Screen + ViewModel + state + components
  ui/hearingtest/ - setup/, active/, results/ (full-screen flow)
  data/local/db/ - Room: entity/, dao/, DbCheckDatabase
  data/local/preferences/ - DataStore (UserPreferencesDataStore)
  data/repository/ - SessionRepository, MeasurementRepository, PreferencesRepository
  data/model/ - Domain-mallit (Session, NoiseLevel)
  domain/audio/ - AudioEngine (@Volatile isRecording), DecibelCalculator, FrequencyWeightingFilter (kaskaadi-biquad),
                  FFTProcessor (power-of-2 pakotettu), ToneGenerator, AudioSessionManager (thread-safe, widget-paivitys)
  domain/usecase/ - ExportCsvUseCase
  service/ - MeasurementForegroundService (live 2s paivitys), NotificationHelper
  billing/ - BillingManager (KTX suspend), ProFeatureManager
  widget/ - Glance: DbCheckWidget (@EntryPoint DI), DbCheckWidgetReceiver (updateAllWidgets)
  sync/ - CloudBackupManager
  util/ - HapticFeedbackHelper, ShareResultsGenerator
```

## Design system -viittaukset

- **Varit:** `ui/theme/Color.kt` - 18+ tokenia per teema (spec section 3)
- **Typografia:** `ui/theme/Type.kt` - DbCheckTypography 12 tokenilla. displayLg/displayMd = SpaceGrotesk.
- **Gradientit:** `ui/theme/Gradient.kt` - signatureButtonGradient(), signatureSweepGradient()
- **Spacing:** `ui/theme/Spacing.kt` - 8dp grid, DbCheckSpacing data class
- **Teema:** `ui/theme/Theme.kt` - DbCheckTheme.colorScheme/.typography/.spacing. materialTypography() displayLarge/Medium = SpaceGrotesk.

## Audio

- AudioRecord 44.1kHz mono PCM16, 4096 sample buffer
- dB-laskenta: RMS -> 20*log10(rms/32768) + 90 + kalibraatio. rms < 1.0 -> 0 dB.
- A-painotus: 3 kaskaadi-biquad IIR (IEC 61672). C-painotus: 2 kaskaadi-biquad. Z = passthrough.
- FFT: 4096-point Cooley-Tukey, Hann window, `Integer.highestOneBit()` power-of-2. DC-bin skip.
- Batch-kirjoitus tietokantaan ~1s valein (10 lukemaa). Thread-safe `synchronizedList`.
- Foreground service: live dB + kesto paivitys 2s valein notifikaatiossa.

## Build

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug
```

## CI/CD

GitHub Actions: CodeQL (blocked — Kotlin 2.3.20), SonarCloud, Semgrep + OWASP, Qodana (blocked — AGP 9).
SonarCloud project: `Insaner1980_dBcheck`. Linear project: "dBcheck" (Finnvek, High, In Progress).

## Tilan seuranta

Kaikki 3 vaihetta (MVP, Enhancement, Polish) toteutettu. Koodiauditointi tehty: hallusinoidut API:t, design spec -vastaavuus, audio-matematiikka, kuulotesti-algoritmi, oikeudet, billing ja widget korjattu. Buildaa puhtaasti, lint lapaisy ilman virheita. Yksikkotestit puuttuvat. Aktiivinen kehitys, testaus, bugikorjaukset ja turvallisuusskannaukset jatkuvat.
