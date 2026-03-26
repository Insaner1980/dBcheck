# dBcheck - Claude Code -ohjeet

## Projektin yleiskuvaus

Android-desibellimittari Kotlin/Jetpack Compose. 97 Kotlin-tiedostoa, MVVM-arkkitehtuuri, Hilt DI, Room DB. Katso `PROJECT.md` kattava kuvaus ja `dBcheck_design_spec.md` design-spesifikaatio.

## Kriittiset saannot

1. **`dBcheck_design_spec.md` on ainoa totuuden lahde.** Jos HTML-mockupit tai PNG-kuvat ovat ristiriidassa, seuraa specia.
2. **Ei 1px-bordereita sektioinnissa.** Syvyys luodaan tonaalisen kerrostumisen kautta (surface -> surface-container -> surface-container-high).
3. **Ei Material elevation -varjoja.** Kayta tonal layering + ambient shadow (4% opacity, 12dp offset, 24dp blur).
4. **Min corner radius 8dp** kaikissa containereissa.
5. **Manrope** teksteille, **Space Grotesk** kaikille numeroille. Ei poikkeuksia.
6. **Ei dynamicDarkColorScheme/dynamicLightColorScheme.** Brandivarit aina.
7. **Pro-ominaisuudet** nayttavat previewin ProLockOverlay:lla, eivat piilota sisaltoa kokonaan.
8. **Molemmat teemat** (dark/light) testattava jokaiselle nautolle ja komponentille.

## Arkkitehtuurikonventiot

- **Single Activity** + Compose Navigation, ei fragmentteja
- **MVVM:** Screen -> ViewModel (StateFlow<UiState>) -> Repository -> Room/DataStore
- **Hilt:** @HiltViewModel, @AndroidEntryPoint, @Singleton
- **KSP** Room ja Hilt compiler-prosessointiin
- **Coroutines:** collectAsStateWithLifecycle() UI:ssa, Dispatchers.Default audio-prosessointiin

## Pakettiraeknne

```
com.dbcheck.app/
  di/          - Hilt-modulit (AppModule, DatabaseModule)
  ui/theme/    - DbCheckTheme, DbCheckColorScheme, DbCheckTypography, DbCheckSpacing
  ui/components/ - 13 uudelleenkaytettavaa composable-komponenttia
  ui/navigation/ - Screen sealed class, DbCheckNavHost, BottomNavDestination
  ui/{meter,analytics,history,settings}/ - Nayttokohtaiset Screen + ViewModel + state + components
  ui/hearingtest/ - setup/, active/, results/ (full-screen flow)
  data/local/db/ - Room: entity/, dao/, DbCheckDatabase
  data/local/preferences/ - DataStore
  data/repository/ - SessionRepository, MeasurementRepository, PreferencesRepository
  data/model/ - Domain-mallit
  domain/audio/ - AudioEngine, DecibelCalculator, FrequencyWeightingFilter, FFTProcessor, ToneGenerator, AudioSessionManager
  domain/usecase/ - ExportCsvUseCase
  service/ - MeasurementForegroundService, NotificationHelper
  billing/ - BillingManager, ProFeatureManager
  widget/ - Glance: DbCheckWidget, DbCheckWidgetReceiver
  sync/ - CloudBackupManager
  util/ - HapticFeedbackHelper, ShareResultsGenerator
```

## Design system -viittaukset

- **Varit:** `ui/theme/Color.kt` - 18+ tokenia per teema
- **Typografia:** `ui/theme/Type.kt` - DbCheckTypography 12 tokenilla
- **Gradientit:** `ui/theme/Gradient.kt` - signatureButtonGradient(), signatureSweepGradient()
- **Spacing:** `ui/theme/Spacing.kt` - 8dp grid, DbCheckSpacing data class
- **Teema:** `ui/theme/Theme.kt` - DbCheckTheme.colorScheme/.typography/.spacing

## Audio

- AudioRecord 44.1kHz mono PCM16
- dB-laskenta: RMS -> 20*log10(rms/32768) + 90 + kalibraatio
- A-painotus IIR-suodatin oletuksena, C/Z Pro-ominaisuuksia
- FFT: 4096-point, Hann window, Cooley-Tukey
- Batch-kirjoitus tietokantaan 1s valein (ei per-sample)

## Build

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk gradle assembleDebug
```

## Tilan seuranta

Kaikki 3 vaihetta (MVP, Enhancement, Polish) toteutettu. Koodi on kirjoitettu mutta ei viela taydellisesti buildattu/testattu - kompilointivirheita voi esiintya.
