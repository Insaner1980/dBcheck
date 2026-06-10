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

Osa 19 - Live LAeq Meteriin

Tehtävät:
- Lisää MeterUiStateen equivalent-level live-arvo ja label.
- Näytä Pro-käyttäjälle LAeq/LCeq/LZeq label oikein.
- Free saa lukitun previewn tai nykyisen yksinkertaisen stat-rivin.
- Testaa, että stopin jälkeen Session Detail arvo vastaa toleranssilla.

Hyväksyntä:
- active session equivalent level näkyy.

## Seuraava tehtävä

Osa 20 - Dosimeter UI data mapping
