# Osa 92 - Ambient Sound Playback Implementation Plan

## Summary
Toteutetaan Osa 92 uudelleen rajattuna nimellä **Ambient sound playback**, ei “tinnitus therapy”. Ominaisuus on Pro-gatettu, käyttäjän itse käynnistämä paikallinen taustaäänen toisto: generoidut white/pink/brown/fan-äänet, volume, sleep timer, erillinen media playback foreground service ja välitön Stop appista sekä notificationista.

Ei lisätä diagnoosi-, hoito-, relief-, cure-, safety- tai hearing-protection-copya. Ei automaattisia triggereitä, Health Connectiä, journalia, oireseurantaa, mikkiä, raakaaudiota, pilvisynkkaa tai playback-historiaa.

## Key Changes
- Lisää uusi non-top-level route `ambient/playback`, joka näkyy Analytics Overviewssa Pro-gatettuna korttina lähellä tinnitus pitch -korttia.
- Lisää DataStoreen vain local-only asetukset:
  - `ambient_sound_preset`: `WHITE_NOISE`, `PINK_NOISE`, `BROWN_NOISE`, `FAN`
  - `ambient_sound_volume`: clamp `0.05f..1.0f`, default `0.35f`
  - `ambient_sound_timer_minutes`: `0`, `15`, `30`, `60`, `120`, default `30`; `0` tarkoittaa no timer.
- Lisää erillinen `AmbientSoundPlaybackService`, ei muutoksia `MeasurementForegroundService`en.
  - Manifest: `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission ja `android:foregroundServiceType="mediaPlayback"`.
  - `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)`.
  - Ei `RECORD_AUDIO`-riippuvuutta eikä mikrofonityyppiä.
- Lisää `AmbientSoundPlayer`, joka streamaa generoitua PCM16-ääntä `AudioTrack.MODE_STREAM`illa.
  - `AudioAttributes.USAGE_MEDIA`, `CONTENT_TYPE_MUSIC`.
  - Audio focus pyydetään startissa; permanent loss pysäyttää, transient loss pausettaa, focus gain jatkaa vain jos service pausetti transientin takia.
- Notification:
  - oma low-importance playback channel tai nykyisen helperin laajennus erillisellä channelilla.
  - ongoing notification, title “Ambient sound”, preset-nimi, timer-jäljellä jos käytössä.
  - actionit: `Stop`; app UI:ssä myös `Stop`.
  - Android 13+ pyydetään `POST_NOTIFICATIONS` käyttäjän Play-toiminnosta; jos lupa evätään, playbackia ei käynnistetä, jotta taustatoisto ei jää ilman näkyvää stop-kontrollia.

## Implementation Notes
- Nimeä koodi ja copy ambient/sound playback -termeillä. Älä käytä UI:ssa “therapy”, “treatment”, “relief”, “reduce tinnitus”, “cure”, “safe” tai “hearing protection”.
- Äänet generoidaan paikallisesti ilman assetteja ja ilman uutta media-kirjastoriippuvuutta:
  - white noise: satunnaisnäytteet
  - pink noise: suodatettu white noise
  - brown noise: integroitu/clampattu noise
  - fan: pehmeä suodatettu noise + matala hum-komponentti
- ViewModel on execution gate:
  - Free-käyttäjä ei voi käynnistää playbackia.
  - Settings/DataStore-arvot saavat näkyä vain effective Pro -polussa.
  - Settings-toggle ei käynnistä ääntä; vain Play-painike route UI:ssa.
- Sleep timer vain pysäyttää jo käyttäjän käynnistämän playbackin. Se ei saa koskaan käynnistää ääntä ajastetusti.
- Päivitä `NEXT.md`: Osa 92 valmis, seuraavaksi Osa 93 Accessibility audit.
- Päivitä `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md` ja toteutussuunnitelman etenemisloki, koska lisätään uusi service/dataflow.

## Test Plan
- RED ensin:
  - `AmbientSoundPolicyTest`: preset enum, volume clamp, timer options, no invalid values.
  - `AmbientSoundGeneratorTest`: jokainen preset tuottaa finite PCM16-dataa, ei ylitä `Short.MIN_VALUE..Short.MAX_VALUE`, output ei ole tyhjä/silent.
  - `AmbientSoundPlaybackServicePolicyTest`: start vaatii Pro + user action, stop intent pysäyttää, timer pysäyttää muttei käynnistä, foreground type on media playback.
  - `AmbientSoundPlaybackViewModelTest`: Free ei starttaa, Pro starttaa Playsta, notification permission denied estää startin, DataStore update toimii.
  - `AmbientSoundScopeTest`: ambient-polku ei sisällä kiellettyä copya eikä viittauksia `HealthConnect`, `RECORD_AUDIO`, `MeasurementForegroundService`, auto-triggeriin tai journal-oireseurantaan.
  - `NavigationRoutePolicyTest`: `ambient/playback` ei näy top-level navigationissa ja route on rekisteröity.
- GREEN/regressio:
  - kohdennettu `:app:testDebugUnitTest` ambient-, preferences-, analytics- ja navigation-testeillä.
  - `:app:compileReleaseKotlin`.
  - `git diff --check`.
- `lc` ja `sc` jätetään ajamatta, ellei käyttäjä erikseen pyydä.

## Assumptions
- Toteutus käyttää native `AudioTrack` + omaa foreground serviceä, ei Media3/ExoPlayeria, koska generoidut paikalliset äänet eivät tarvitse uutta riippuvuutta.
- Google Play -riskin pienin toteutus on local-only ambient playback ilman health claimseja tai uutta sensitive-dataa. Data Safety pitää silti pitää ajantasaisena koko appin todellisen datankäytön mukaan.
- Viralliset tarkistetut rajat: Android media playback foreground service vaatii `mediaPlayback`-tyypin ja `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissionin; runtime prerequisites ovat “None” ([Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)). API 34+ manifest-deklaraatio on pakollinen käytännössä ([Declare foreground services](https://developer.android.com/develop/background-work/services/fgs/declare)). Playn health policy kieltää harhaanjohtavan health functionalityn ja vaatii disclosure/declaration-polut health/medical-scopeen ([Google Play Health Content and Services](https://support.google.com/googleplay/android-developer/answer/16679511)); Data Safety pitää kuvata totuudenmukaisesti ([Google Play Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)).

## Etenemisloki

### 2026-06-28 - Toteutettu
- Toteutettu Pro-gatettu ambient sound playback -polku: `ambient/playback`, Analyticsin `AmbientSoundCard`, DataStore-arvot, local PCM16-generointi, erillinen `AmbientSoundPlaybackService`, AudioTrack-player, audio focus, notification channel/action ja stop timer.
- Rajaus säilyi: ei tinnitus therapyä, diagnosis/treatment/relief/cure/safety-copya, hearing-protection-väitettä, Health Connectiä, journalia, oireseurantaa, mikrofonia, raakaaudiota, pilvisynkkaa, playback-historiaa tai automaattisia triggereitä.
- RED: kohdennettu ambient-testiajo kaatui odotetusti puuttuviin ambient-domain-, service-, ViewModel- ja route-rajapintoihin.
- GREEN/regressio: kohdennettu `:app:testDebugUnitTest` ambient-, preferences-, analytics- ja navigation-testeillä meni läpi.
- Release/verifikaatio: `.\gradlew.bat :app:compileReleaseKotlin` meni läpi ja `git diff --check` palautti exit 0 vain LF/CRLF-varoituksilla.
- `NEXT.md` siirrettiin Osa 93 Accessibility audit -tehtävään.
