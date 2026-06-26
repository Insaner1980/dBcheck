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

Osa 89 - TTS short hearing check

Tehtävät:
- Lisää lyhennetty hearing-test flow.
- Lisää recovery result table.
- Lisää Analytics recovery card.
- Hyväksyntä: copy on varovaista ja ei-diagnostista.

Hyväksyntä:
- Copy on varovaista ja ei-diagnostista.

## Seuraava tehtävä

Osa 90 - Tinnitus planning gate
