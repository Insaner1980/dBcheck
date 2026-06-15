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

Osa 58 - WAV writer

Tehtävät:
- Lisää streamaava PCM/WAV writer cache/app storageen.
- Kytke raw-audio fanoutiin vain opt-in + Pro.
- Testaa header, duration, cleanup.

Hyväksyntä:
- WAV syntyy ilman muistipiikkiä.

## Seuraava tehtävä

Osa 59 - WAV export/delete UI
