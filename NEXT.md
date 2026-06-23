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

Osa 69 - External mic discovery

Tehtävät:
- Tarkista Android AudioDeviceInfo/AudioRecord official docs.
- Lisää input-device listaus service-porttiin.
- Testaa fake device list.

Hyväksyntä:
- UI saa device-listan.

## Seuraava tehtävä

Osa 70 - External mic selection
