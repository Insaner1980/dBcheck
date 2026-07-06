# dBcheck NEXT

## Pysyvä konteksti

- Nykyinen arkkitehtuuri: MVVM + Hilt + Room + DataStore + Compose Navigation.
- Mittauslogiikka kuuluu `domain/audio` ja `service/AudioSessionManager` -polkuihin.
- UI ei laske mittausarvoja, vaan lukee valmiita state-malleja.
- Pro-ominaisuudet gateataan UI:n lisäksi execution/data-polussa.
- Älä aja `lc` tai `sc`, ellei käyttäjä pyydä.
- Arkkitehtuurimuutoksissa päivitä `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md` ja suunnitelma.
- Jokaisen tehtävän lopuksi päivitä etenemisloki ja tämä `NEXT.md`.

## Nykyinen tehtävä

Ei aktiivista tehtävää.

Tila: SUUNNITELMA VALMIS 2026-06-29.

Viimeksi valmis:
- Osa 100 - Documentation sync.
- `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md` ja suunnitelmaloki on synkronoitu Osa89-Osa99-toteutusten, Osa99:n raporttitulosten ja release-riskien kanssa.
- Deferred-kohdat on kirjattu perusteluineen: fyysinen USB/Bluetooth-mikrofonitesti, Sleep long-running device smoke, device-level permission smoke, Play Console / purchase QA, signed Play-ready AAB, Play upload ja real Qodana run.

## Seuraava tehtävä

Ei määritelty - Osa 100 on tämän suunnitelman viimeinen tehtävä.
