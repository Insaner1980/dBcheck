# dBcheck Missing Features - pilkottu toteutussuunnitelma

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Toteuttaa `dbcheck_missing_features_audit.md`-auditin puuttuvat dBcheck-ominaisuudet pieninä, keskeytystä kestävinä osina.

**Architecture:** Nykyinen MVVM + Hilt + Room + DataStore + Compose Navigation pidetään. Mittauslogiikka keskitetään `domain/audio`- ja `service/AudioSessionManager`-polkuihin; UI lukee valmiita state-malleja eikä tee mittauslaskentaa. Jokainen Pro-ominaisuus gateataan UI:n lisäksi execution/data-polussa.

**Tech Stack:** Kotlin 2.3.20, AGP 9.1.0, Compose BOM 2026.03.00, Room 2.8.4, DataStore 1.2.1, Hilt 2.59.2, Health Connect 1.1.0, CameraX 1.6.1 sekä LiteRT/TFLite-audio osien mukaisesti virallisista lähteistä tarkistetuilla versioilla.

---

## Pakollinen jatkomuisti jokaisen osan lopussa

Jokaisen toteutusosan lopuksi toteuttajan pitää kirjoittaa työn tila ylös ennen kuin konteksti vaihtuu, testi jää kesken tai keskustelu katkeaa.

Kirjaa joko tämän tiedoston loppuun `## Toteutuksen etenemisloki` -kohtaan tai erilliseen `STATUS.md`-päivitykseen:

```markdown
### YYYY-MM-DD HH:mm - Osa N
- Valmis:
- Kesken jäi:
- Seuraava tehtävä:
- Seuraava komento:
- Ajetut testit:
- Muutetut tiedostot:
- Huomiot käyttäjälle:
```

Jos osa jää kesken, seuraavan tekijän pitää pystyä jatkamaan aloittamatta laajaa uudelleenkartoitusta.

## Yleiset toteutussäännöt

- Ennen jokaista osaa lue `AGENTS.md`, tämän suunnitelman nykyinen etenemisloki ja kyseisen osan lähdetiedostot.
- Älä aja `lc` tai `sc` itse, ellei käyttäjä erikseen pyydä. Kun käyttäjä ajaa ne, lue `reports/`-tulokset.
- Uudet Android-, CameraX-, LiteRT/TFLite-, Play-, Health Connect- ja permission-käytännöt tarkistetaan virallisista lähteistä juuri ennen kyseistä osaa.
- Tee mieluummin yksi pieni commit per osa tai per kaksi tiiviisti liittyvää osaa. Commit-viesti suomeksi.
- Arkkitehtuurimuutoksissa päivitä `AGENTS.md`, `memory/MEMORY.md`, `PROJECT.md` ja tämä suunnitelma samassa kokonaisuudessa.

## A. Valmistelu ja suoja-aidat

### Osa 1 - Nykytilan lukitus
- [ ] Lue `PROJECT.md`, `dbcheck_missing_features_audit.md`, `AGENTS.md` ja tämä suunnitelma.
- [ ] Tarkista `git status --short`.
- [ ] Kirjaa etenemislokiin nykyinen branch, dirty files ja ensimmäinen toteutettava osa.
- [ ] Hyväksyntä: ei koodimuutoksia, vain kirjattu lähtötila.

### Osa 2 - Roadmap-statusrakenne
- [ ] Lisää tähän tiedostoon etenemisloki, jos sitä ei vielä ole.
- [ ] Päätä jokaiselle auditin isolle kokonaisuudelle status: `not-started`, `in-progress`, `blocked`, `done`, `deferred`.
- [ ] Hyväksyntä: seuraava tekijä näkee yhdellä silmäyksellä, mikä on valmis ja mikä seuraavaksi.

### Osa 3 - Testikomentojen peruslista
- [ ] Kirjaa suunnitelmaan pienet verifiointikomennot: domain unit -testi, ViewModel-testi, migration-testit, screenshot validation, `lintDebug`.
- [ ] Merkitse, että `lc`/`sc` ovat käyttäjän ajamia.
- [ ] Hyväksyntä: jokaisella myöhemmällä osalla on valmis testivalikko.

## B. Mittausydin

### Osa 4 - ResponseTime-domainmalli
- [x] Lisää `ResponseTime` Fast 200 ms, Slow 500 ms, Impulse 50 ms.
- [x] Lisää normalisointi `UserPreferenceDefaults`-polkuun.
- [x] Lisää unit-testit arvoille, preference fallbackille ja Free effective defaultille.
- [x] Hyväksyntä: malli on olemassa, mutta ei vielä muuta mittausta.

### Osa 5 - ResponseTime DataStoreen ja Settings-stateen
- [x] Lisää DataStore key ja `UserPreferences.responseTime`.
- [x] Lisää `PreferencesRepository.updateResponseTime`.
- [x] Lisää `SettingsUiState.responseTime`.
- [x] Testaa DataStore mapping ja SettingsViewModel state.
- [x] Hyväksyntä: asetus persistoi, mutta mittaus ei vielä käytä sitä.

### Osa 6 - Response-time laskuri domainiin
- [x] Lisää energy-domain smoothing -luokka `domain/audio`.
- [x] Laske smoothing aikaleimojen perusteella, ei chunk-countin perusteella.
- [x] Testaa askelvaste Fast/Slow/Impulse, tyhjä input ja epäjärjestyksessä oleva timestamp.
- [x] Hyväksyntä: laskuri toimii erillään `AudioEngine`sta.

### Osa 7 - AudioEngine käyttää response timea
- [x] Vie effective response time `AudioSessionManager`ilta `AudioEngine`lle ennen session starttia.
- [x] Free-käyttäjä käyttää aina Fast-arvoa.
- [x] LCpeak pysyy chunk peakina, response time vaikuttaa RMS/equivalent-lukemiin.
- [x] Testaa, että UI refresh rate ei muuta response timea.
- [x] Hyväksyntä: mittauskäyttäytyminen muuttuu vain response-time-asetuksella.

### Osa 8 - A-painotettu rinnakkaisvirta
- [x] Lisää `DecibelReading`iin A-painotettu dB-arvo valitun weighting-arvon lisäksi.
- [x] Lisää AudioEngineen oma A-weighting-filter-instanssi.
- [x] Testaa, että C-weighted LCpeak ja selected weighted RMS eivät muutu.
- [x] Hyväksyntä: dosimeter voi käyttää A-dataa ilman UI-weightingin pakottamista.

### Osa 9 - MeasurementEntity core-migraatio
- [x] Nosta Room schema v3 -> v4.
- [x] Lisää mittausriveille A-weighted dB ja response-time metadata, jos raportointi tarvitsee niitä.
- [x] Lisää migration ja schema JSON.
- [x] Testaa migration vanhasta schemasta.
- [x] Hyväksyntä: vanha data säilyy ja uusi data tallentuu.

### Osa 10 - DosimeterStandard-domainmalli
- [x] Lisää `DosimeterStandard.NIOSH_REL` ja `OSHA_PEL`.
- [x] Lisää DataStore key, fallback ja Settings state.
- [x] Testaa standardin persistointi ja Free/Pro effective policy.
- [x] Hyväksyntä: standardi on asetuksissa saatavilla, mutta laskenta ei vielä käytä sitä.

### Osa 11 - DosimeterCalculator
- [x] Siirrä nykyinen NIOSH dose/TWA-logiikka erilliseen domain-luokkaan.
- [x] Lisää OSHA PEL -laskenta.
- [x] Lisää projected dose ja remaining exposure time.
- [x] Testaa hiljaisuus, nollakesto, kovat arvot, lyhyt sessio, 8h referenssi, NIOSH/OSHA erot.
- [x] Hyväksyntä: completed report ja live flow voivat käyttää samaa laskuria.

### Osa 12 - LiveExposureState AudioSessionManageriin
- [x] Laajenna `SessionStats` tai lisää rinnakkainen `LiveExposureState`.
- [x] Päivitä state jokaisesta decibel readingista.
- [x] Pidä Room-persistenssin 1s cadence ennallaan.
- [x] Testaa session start/stop/reset ja process recovery.
- [x] Hyväksyntä: ViewModel voi lukea live LAeq/dose/projection-arvot.

## C. Meter P0 pieninä osina

### Osa 13 - Meter measurement mode state
- [x] Lisää `MeasurementMode.DB_METER` ja `DOSIMETER`.
- [x] Lisää `MeterUiState.measurementMode`.
- [x] Lisää ViewModel-funktio mode switchille.
- [x] Testaa, että mode switch ei käynnistä tai pysäytä AudioRecordia.
- [x] Hyväksyntä: state vaihtuu ilman UI-muutosta.

### Osa 14 - Meter mode chip row
- [x] Lisää chip row Meterin yläosaan käyttäen nykyistä `DbCheckChip`-tyyliä.
- [x] Free-käyttäjällä Dosimeter chip näyttää lock-tilan tai avaa upgrade-polun.
- [x] Lisää accessibility-label selected/locked-tilalle.
- [x] Screenshot-testit Free ja Pro.
- [x] Hyväksyntä: chipit näkyvät ja toimivat.

### Osa 15 - Live 30s chart buffer
- [x] Lisää rajattu rolling buffer ViewModeliin tai domain helperiin.
- [x] Trimmaa pisteet timestampin mukaan, ei listan koon mukaan.
- [x] Reset tyhjentää bufferin, pause jäädyttää sen.
- [x] Unit-testit bufferille.
- [x] Hyväksyntä: data on valmis Canvas-komponentille.

### Osa 16 - Live 30s chart UI
- [x] Lisää Canvas-komponentti 0-130 dB akselilla.
- [x] Lisää 85 dB threshold line ja peak markerit.
- [x] Lisää empty, active ja paused state.
- [x] Screenshot-testit molemmissa teemoissa.
- [x] Hyväksyntä: dB Meter näyttää rolling chartin ilman layout-hyppyjä.

### Osa 17 - Sound reference data
- [x] Lisää yksi lähde referenssiäänille ja dB-arvoille.
- [x] Lisää nearest-reference laskenta ja marker-position laskenta.
- [x] Unit-testit clampingille ja nearest matchille.
- [x] Hyväksyntä: data/logiikka valmis UI:lle.

### Osa 18 - Sound reference card UI
- [x] Lisää expandable/collapsible card Meteriin.
- [x] Korosta lähin referenssi muutenkin kuin värillä.
- [x] Lisää TalkBack-kuvaus nykyisestä lähimmästä referenssistä.
- [x] Screenshot-testit collapsed/expanded.
- [x] Hyväksyntä: Free ja Pro näkevät referenssikortin.

### Osa 19 - Live LAeq Meteriin
- [x] Lisää MeterUiStateen equivalent-level live-arvo ja label.
- [x] Näytä Pro-käyttäjälle LAeq/LCeq/LZeq label oikein.
- [x] Free saa lukitun previewn tai nykyisen yksinkertaisen stat-rivin.
- [x] Testaa, että stopin jälkeen Session Detail arvo vastaa toleranssilla.
- [x] Hyväksyntä: active session equivalent level näkyy.

### Osa 20 - Dosimeter UI data mapping
- [x] Mapita `LiveExposureState` `DosimeterUiState`ksi.
- [x] Lisää unavailable-tila vanhalle datalle tai puuttuvalle A-dataan perustuvalle laskennalle.
- [x] Testaa Pro/Free/unavailable.
- [x] Hyväksyntä: UI saa valmiin mallin ilman laskentaa.

### Osa 21 - Dosimeter gauge UI
- [x] Lisää dose percentage gauge.
- [x] Näytä TWA, LAeq, dose %, projected dose ja remaining time.
- [x] Näytä standard label ja reference info.
- [x] Screenshot-testit low/near-limit/over-limit.
- [x] Hyväksyntä: Dosimeter mode on käytettävä Pro-näkymä.

### Osa 22 - Meter session info bar
- [x] Lisää REC, duration, effective weighting, response time.
- [x] Näytä Prolle sample rate ja input device.
- [x] Testaa timer taustalta paluun jälkeen.
- [x] Hyväksyntä: aktiivisen session metatiedot näkyvät yhtenäisesti.

## D. Analytics ja spectral

### Osa 23 - AnalyticsSection state
- [x] Lisää `AnalyticsSection.OVERVIEW`, `SPECTRAL`, `ENVIRONMENT`.
- [x] Lisää ViewModel/UI state chip-valinnalle.
- [x] Testaa selection persist recompositionin yli.
- [x] Hyväksyntä: section-state toimii ilman korttien siirtoa.

### Osa 24 - Analytics chip row UI
- [x] Lisää chip row Analyticsin headerin alle.
- [x] Free-käyttäjä näkee Pro-sectionit lukittuina, ei piilotettuina.
- [x] Screenshot-testit.
- [x] Hyväksyntä: navigointi sectioneihin toimii.

### Osa 25 - Analytics card regroup
- [x] Siirrä weekly/monthly/yearly/hearing health Overviewiin.
- [x] Siirrä spectral card Spectral-sectioniin.
- [x] Siirrä Environment Mix Environment-sectioniin.
- [x] Testaa, ettei nykyinen dataflow muutu.
- [x] Hyväksyntä: näkymä vastaa auditin rakennetta.

### Osa 26 - Weekly/Monthly toggle
- [x] Lisää Overviewiin Weekly/Monthly-toggle.
- [x] Weekly on Free, Monthly Pro.
- [x] Käytä nykyistä `ExposureAnalyticsCalculator`ia.
- [x] Testaa Free/Pro/empty.
- [x] Hyväksyntä: monthly ei laske tai näytä oikeaa Pro-dataa Free-tilassa.

### Osa 27 - SpectralMode Bars
- [x] Tee nykyisestä spectral cardista `SpectralMode.BARS`.
- [x] Lisää mode state ja UI toggle.
- [x] Testaa nykyisen bars-käyttäytymisen säilyminen.
- [x] Hyväksyntä: ei regressiota nykyiseen spektriin.

### Osa 28 - Spectrogram buffer
- [x] Lisää waterfall ring buffer spectral frameistä.
- [x] Rajaa pituus ajalla tai rivimäärällä.
- [x] Unit-testit trimmauslogiikalle.
- [x] Hyväksyntä: spectrogram data valmis UI:lle.

### Osa 29 - Spectrogram UI
- [x] Lisää Canvas-waterfall Spectral-sectioniin.
- [x] Lisää idle/locked/live accessibility text.
- [x] Screenshot-testit.
- [x] Hyväksyntä: Pro käyttäjä näkee live spectrogramin.

### Osa 30 - Octave-band RTA domain
- [x] Lisää octave/third-octave band calculator nykyisen FFT:n päälle.
- [x] Testaa band edge, center frequency ja normalized amplitude.
- [x] Hyväksyntä: RTA-data on laskettu domainissa.

### Osa 31 - RTA UI
- [x] Lisää RTA mode Spectral-sectioniin.
- [x] Näytä octave bars ja stat pills.
- [x] Screenshot-testit.
- [x] Hyväksyntä: Bars/Spectrogram/RTA toggle toimii.

### Osa 32 - Spectral stat pills
- [x] Lisää dominant frequency, bandwidth, peak band, live/idle.
- [x] Käytä yhtä formatteria Hz/kHz-arvoille.
- [x] Testaa formatterit ja UI-state.
- [x] Hyväksyntä: statsit eivät ole kovakoodattua copya.

### Osa 33 - Real-time Environment Mix accumulator
- [x] Lisää aktiivisen session category distribution helper.
- [x] Käytä `NoiseLevel`-rajoja yhdestä lähteestä.
- [x] Unit-testit prosenttipyöristykselle.
- [x] Hyväksyntä: realtime mix data on valmis UI:lle.

### Osa 34 - Real-time Environment Mix UI
- [x] Lisää Environment-sectioniin active-session mix.
- [x] Säilytä 7 päivän historiallinen mix erillisenä.
- [x] Testaa recording/non-recording/Free.
- [x] Hyväksyntä: realtime ja historical mix eivät sekoitu.

## E. YAMNet ja sound detection

### Osa 35 - Dependency research ja lukitus
- [x] Tarkista virallisista lähteistä LiteRT/TFLite Android audio classification ja YAMNet asset -jakelutapa.
- [x] Lisää version catalog -merkinnät vain tarkistetuilla versioilla.
- [x] Päivitä dependency lock/verification metadata tarvittaessa.
- [x] Hyväksyntä: build riippuvuuksilla onnistuu ilman classifier-koodia.

### Osa 36 - Model asset ja labels
- [x] Lisää YAMNet model asset ja label-tiedosto.
- [x] Lisää license/attribution dokumenttiin.
- [x] Testaa assetin löytyminen JVM/Robolectric-polulla, jos mahdollista.
- [x] Hyväksyntä: assetit paketoituvat release buildiin.

### Osa 37 - Audio window adapter
- [x] Lisää raw PCM -> 16 kHz mono float window -adapteri.
- [x] Älä tallenna raakaaudiota.
- [x] Unit-testit normalisoinnille ja window pituudelle.
- [x] Hyväksyntä: classifier saa oikean inputin.

### Osa 38 - SoundClassifier-portti
- [x] Lisää testattava interface ja tuotantototeutus.
- [x] Lisää fake classifier unit/ViewModel-testeihin.
- [x] Testaa confidence threshold ja empty output.
- [x] Hyväksyntä: inference on erillinen AudioEnginestä.

### Osa 39 - Sound detection live state
- [x] Kytke classifier raw-audio fanoutiin Pro + toggle -ehdolla.
- [x] Emittoi current type, confidence ja recent detections.
- [x] Testaa Free, Pro off, Pro on.
- [x] Hyväksyntä: sound detection ei aja inferenceä Free-tilassa.

### Osa 40 - Environment UI sound detection
- [x] Lisää Environment-sectioniin sound type card.
- [x] Lisää locked/idle/live/error states.
- [x] Screenshot-testit.
- [x] Hyväksyntä: live detection näkyy ilman raw audion tallennusta.

### Osa 41 - Optional detection persistence
- [x] Lisää Room table vain aggregoiduille detection-eventeille.
- [x] Lisää DataStore opt-in tallennukselle.
- [x] Lisää delete/export semantics privacy policyyn.
- [x] Migration-testit.
- [x] Hyväksyntä: persistence on opt-in ja poistettavissa.

## F. Camera Overlay

### Osa 42 - CameraX dependency research
- [x] Tarkista CameraX virallinen release ja artifacts.
- [x] Lisää version catalog ja Gradle-dependencies.
- [x] Päivitä dependency verification/lockfile.
- [x] Hyväksyntä: build menee läpi ilman UI:ta.

### Osa 43 - CAMERA permission
- [x] Lisää manifestiin `CAMERA`.
- [x] Lisää permission policy/helper runtime requestille.
- [x] Testaa granted/denied/permanent denied.
- [x] Hyväksyntä: permission ei koske Meter-starttia.

### Osa 44 - Camera route
- [x] Lisää `Screen.CameraOverlay`.
- [x] Lisää NavHost route ja piilota bottom nav kyseisellä routella.
- [x] Lisää Meteristä Pro-gatettu entry.
- [x] Hyväksyntä: route avautuu ja palaa turvallisesti.

### Osa 45 - Camera preview shell
- [x] Lisää fullscreen preview UI fake/static previewllä testattavaksi.
- [x] Lisää close button, overlay slots ja permission denial UI.
- [x] Screenshot-testit.
- [x] Hyväksyntä: UI runko valmis ennen CameraX sessionia.

### Osa 46 - CameraX preview binding
- [x] Kytke lifecycle camera preview.
- [x] Käsittele camera unavailable.
- [x] Manual device smoke.
- [x] Hyväksyntä: preview näkyy laitteella.

### Osa 47 - Live dB overlay
- [x] Lue current dB AudioEngine/AudioSessionManager statesta.
- [x] Näytä dB, label ja timestamp overlaynä.
- [x] Testaa ilman aktiivista mittausta ja aktiivisen mittauksen aikana.
- [x] Hyväksyntä: overlay toimii ilman measurement-session rikkomista.

### Osa 48 - Photo capture burned-in overlay
- [x] Tallenna kuva MediaStoreen tai export cacheen.
- [x] Piirrä overlay bitmapiin.
- [x] Jaa `content://`-URIlla.
- [x] Manual share test.
- [x] Hyväksyntä: jaetussa kuvassa overlay on mukana.

### Osa 49 - Silent video capture
- [x] Lisää CameraX video ilman audio trackia oletuksena.
- [x] Lisää visual overlay mukaan tallenteeseen tai dokumentoi, jos tekninen polku vaatii erillisen renderöinnin.
- [x] Lisää privacy copy: ääntä ei tallenneta camera-videoon.
- [x] Hyväksyntä: video ei lisää uutta raw audio -keräystä.

## G. Storage, History, Report ja Export

### Osa 50 - Session location design
- [x] Tarkista Android location permission -ohjeet.
- [x] Lukitse foreground approximate-only.
- [x] Lisää privacy copy ja Data Safety -muistiinpano.
- [x] Hyväksyntä: ei koodimuutosta ennen permission-scopea.

### Osa 51 - Session location schema
- [x] Lisää session tableen optional location fields.
- [x] Lisää migration ja tests.
- [x] Hyväksyntä: vanhat sessiot toimivat ilman locationia.

### Osa 52 - One-shot location capture
- [x] Lisää service/portti location capturelle session start/stop -hetkeen.
- [x] Käsittele denied/unavailable ilman session failia.
- [x] Testaa fake location providerilla.
- [x] Hyväksyntä: location on optional metadata.

### Osa 53 - History search query
- [x] Lisää DAO query name/tag/date/level/weighting/location -filttereille.
- [x] Säilytä Free 7 päivän policy.
- [x] Testaa deterministic ordering.
- [x] Hyväksyntä: repository palauttaa filtteröidyt sessiot.

### Osa 54 - History search UI
- [x] Lisää search field ja filter chips Pro-käyttäjälle.
- [x] Free saa locked previewn tai upgrade affordancen.
- [x] Screenshot-testit.
- [x] Hyväksyntä: History filteröityy ilman direct-open gate regressiota.

### Osa 55 - Histogram calculator
- [x] Lisää dB bucket calculator.
- [x] Testaa tyhjä data, edge bucketit ja prosentit.
- [x] Hyväksyntä: histogram data valmis Session Detailille.

### Osa 56 - Histogram UI
- [x] Lisää Session Detailiin Pro histogram card.
- [x] Lisää accessibility text.
- [x] Screenshot-testit.
- [x] Hyväksyntä: käyttäjä näkee dB-jakauman.

### Osa 57 - WAV opt-in preference
- [x] Lisää per-session opt-in tai Settings default OFF.
- [x] Lisää privacy warning.
- [x] Testaa Free/Pro gating.
- [x] Hyväksyntä: raakaaudiota ei voi tallentaa vahingossa.

### Osa 58 - WAV writer
- [ ] Lisää streamaava PCM/WAV writer cache/app storageen.
- [ ] Kytke raw-audio fanoutiin vain opt-in + Pro.
- [ ] Testaa header, duration, cleanup.
- [ ] Hyväksyntä: WAV syntyy ilman muistipiikkiä.

### Osa 59 - WAV export/delete UI
- [ ] Lisää Session Detail export/delete controls.
- [ ] Jaa FileProvider/MediaStore URI turvallisesti.
- [ ] Manual share test.
- [ ] Hyväksyntä: käyttäjä voi viedä ja poistaa audion.

### Osa 60 - PDF available-fields completion
- [ ] Lisää nykyisin jo saatavilla olevat kentät PDF:ään: device info, calibration offset, response time, app version, footer/disclaimer.
- [ ] Testaa `ExportPdfReportUseCase`.
- [ ] Hyväksyntä: PDF paranee ilman upstream featurejä.

### Osa 61 - PDF upstream fields
- [ ] Lisää PDF:ään location, dosimeter standard, projected dose, octave breakdown ja sound type vasta kun kukin upstream osa on valmis.
- [ ] Lisää fallback text unavailable-arvoille.
- [ ] Hyväksyntä: PDF ei näytä harhaanjohtavia nollia.

### Osa 62 - CSV batch/clear history
- [ ] Lisää clear history flow safety confirmationilla.
- [ ] Lisää batch export valitut/all sessions.
- [ ] Testaa Free/Pro gate ja FileProvider oikeudet.
- [ ] Hyväksyntä: export ja delete eivät riko backup/restore-polkuja.

## H. Settings ja advanced measurement

### Osa 63 - Advanced settings section split
- [ ] Jaa Settings pienempiin osioihin, jos tiedosto kasvaa.
- [ ] Lisää Display & Features -section.
- [ ] Hyväksyntä: nykyiset asetukset toimivat ennallaan.

### Osa 64 - Feature toggles
- [ ] Lisää togglet: technical metadata, dosimeter card, sound detection, sleep card.
- [ ] Gate: toggle ei ohita Pro-entitlementia.
- [ ] Testaa DataStore ja UI.
- [ ] Hyväksyntä: togglet persistöivät ja vaikuttavat näkyvyyteen.

### Osa 65 - Calibration profile schema
- [ ] Lisää Room table calibration profiles.
- [ ] Lisää selected profile preference.
- [ ] Migration-testit.
- [ ] Hyväksyntä: profiilit voivat tallentua ilman UI:ta.

### Osa 66 - Calibration profile UI
- [ ] Lisää create/select/rename/delete.
- [ ] Estä viimeisen default-profiilin poisto.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: profiilit hallittavissa Settingsissä.

### Osa 67 - Octave calibration model
- [ ] Lisää per-band offsets profileen.
- [ ] Lisää reset-to-zero.
- [ ] Unit-testit clampingille.
- [ ] Hyväksyntä: RTA/PDF voi lukea offsetit.

### Osa 68 - Octave calibration UI
- [ ] Lisää band list/sliderit.
- [ ] Käytä theme-tokeneita, ei inline-värejä.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: Pro voi säätää band-offsetit.

### Osa 69 - External mic discovery
- [ ] Tarkista Android AudioDeviceInfo/AudioRecord official docs.
- [ ] Lisää input-device listaus service-porttiin.
- [ ] Testaa fake device list.
- [ ] Hyväksyntä: UI saa device-listan.

### Osa 70 - External mic selection
- [ ] Käytä `AudioRecord.setPreferredDevice`.
- [ ] Käsittele unplug fallback internal miciin.
- [ ] Lisää report metadata selected device.
- [ ] Manual USB/Bluetooth test.
- [ ] Hyväksyntä: valittu input vaikuttaa AudioRecordiin.

### Osa 71 - Notification schedule model
- [ ] Lisää active days/hours model.
- [ ] Lisää DataStore persistence.
- [ ] Unit-testit crossing-midnight caseille.
- [ ] Hyväksyntä: schedule-logiikka toimii ilman notification UI:ta.

### Osa 72 - Notification schedule UI
- [ ] Lisää Settings UI.
- [ ] Lisää accessibility labels.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: käyttäjä voi rajata alert-ajat.

### Osa 73 - Extended exposure alerts
- [ ] Lisää dose/projected-dose alert evaluator.
- [ ] Kunnioita schedulea.
- [ ] Testaa no-repeat/cooldown.
- [ ] Hyväksyntä: alerts eivät spammaa.

### Osa 74 - Keep Awake
- [ ] Käytä Meterissä ensisijaisesti `FLAG_KEEP_SCREEN_ON`.
- [ ] Lisää Sleepiin tarvittaessa lifecycle-safe wakelock manager.
- [ ] Testaa acquire/release.
- [ ] Hyväksyntä: wake lock ei vuoda.

### Osa 75 - Lockscreen public visibility choice
- [ ] Lisää erillinen `show_lockscreen_meter_publicly` default OFF.
- [ ] Näytä privacy warning.
- [ ] Apply public visibility vain Pro + lockscreen meter + public toggle.
- [ ] Testaa private/public/free.
- [ ] Hyväksyntä: kilpailukykyominaisuus on opt-in.

## I. Sleep Monitor

### Osa 76 - Sleep route shell
- [ ] Lisää Sleep setup route ja Meter/Analytics CTA.
- [ ] Pro-gate execution-polussa.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: flow avautuu ilman mittausta.

### Osa 77 - Sleep session schema
- [ ] Lisää sleep metadata ja notable event -table.
- [ ] Migration-testit.
- [ ] Hyväksyntä: sleep data ei sotke tavallista session tablea.

### Osa 78 - Sleep setup
- [ ] Lisää duration/keep awake/options UI.
- [ ] Lisää privacy/battery copy.
- [ ] Testaa settings mapping.
- [ ] Hyväksyntä: käyttäjä voi valmistella session.

### Osa 79 - Sleep active recording
- [ ] Käytä foreground service -mallia.
- [ ] Lisää sleep-specific state, screen behavior ja notification copy.
- [ ] Manual long-running smoke.
- [ ] Hyväksyntä: sleep mittaa ilman UI:n päällä pysymisen oletusta.

### Osa 80 - Sleep results
- [ ] Lisää results screen: LAeq, max, peak events, loud periods, histogram.
- [ ] Lisää History/Session Detail linkitys.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: valmis sleep sessio on löydettävä historiasta.

### Osa 81 - Sleep export/report
- [ ] Lisää PDF/CSV sleep fields.
- [ ] Lisää fallbackit puuttuvalle datalle.
- [ ] Testaa export.
- [ ] Hyväksyntä: sleep data on jaettavissa.

## J. P1-P3 jatkokehitys pieninä osina

### Osa 82 - Audible alarm policy
- [ ] Lisää threshold/duration/cooldown model.
- [ ] Unit-testit trigger/cooldownille.
- [ ] Hyväksyntä: ei vielä äänen toistoa.

### Osa 83 - Audible alarm playback
- [ ] Lisää bundled sounds ja preview.
- [ ] Käytä `SoundPool` tai `MediaPlayer` `USAGE_ALARM`-attribuutilla.
- [ ] Lisää screen-on/pocket guard.
- [ ] Hyväksyntä: alarm toimii Pro-gatella.

### Osa 84 - Voice baseline
- [ ] Riippuu valmiista YAMNetista.
- [ ] Lisää baseline calibration flow.
- [ ] Älä tallenna raw voice audioa.
- [ ] Hyväksyntä: baseline tallentuu turvallisesti.

### Osa 85 - Voice volume warnings
- [ ] Lisää speech-only sustained detection.
- [ ] Lisää haptic/notification feedback.
- [ ] Testaa false-positive guardit.
- [ ] Hyväksyntä: warning toimii vain kun puhe tunnistetaan.

### Osa 86 - Passive monitoring design gate
- [ ] Tarkista Android background microphone -rajoitukset uudelleen.
- [ ] Kirjaa Play policy/privacy riskit.
- [ ] Hyväksyntä: toteutus ei ala ilman erillistä päätöstä.

### Osa 87 - Passive monitoring implementation
- [ ] Lisää opt-in short foreground sampling.
- [ ] Kirjoita vain aggregate samples Roomiin.
- [ ] Lisää daily summary.
- [ ] Hyväksyntä: käyttäjä voi poistaa käytöstä heti.

### Osa 88 - TTS trigger
- [ ] Riippuu YAMNet + Dosimeter + hearing baseline.
- [ ] Lisää risk event trigger.
- [ ] Testaa baseline missing -polku.
- [ ] Hyväksyntä: ei tee terveysväitteitä.

### Osa 89 - TTS short hearing check
- [ ] Lisää lyhennetty hearing-test flow.
- [ ] Lisää recovery result table.
- [ ] Lisää Analytics recovery card.
- [ ] Hyväksyntä: copy on varovaista ja ei-diagnostista.

### Osa 90 - Tinnitus planning gate
- [ ] Päätä kuuluuko v1.5/v2.0:aan.
- [ ] Kirjaa scope ennen koodia.
- [ ] Hyväksyntä: ei sekoitu v1.0-releaseen.

### Osa 91 - Tinnitus pitch matcher
- [ ] Käytä nykyistä `ToneGenerator`ia turvallisesti.
- [ ] Lisää ear-specific profile.
- [ ] Testaa playback limits.
- [ ] Hyväksyntä: pitch profile tallentuu.

### Osa 92 - Tinnitus sound therapy
- [ ] Lisää noise generators, background playback service, media notification ja sleep timer.
- [ ] Lisää journal vasta erillisessä osassa.
- [ ] Hyväksyntä: playback pysyy hallittavana ja turvallisena.

## K. Julkaisuvalmius

### Osa 93 - Accessibility audit
- [ ] Käy Meter, Analytics, History, Session Detail, Settings, Camera, Sleep.
- [ ] Korjaa missing labels, clipped text, chart semantics.
- [ ] Testaa font scale.
- [ ] Hyväksyntä: kriittiset flowt ovat TalkBack-käytettäviä.

### Osa 94 - Localization baseline
- [ ] Päätä launch-kielet.
- [ ] Lisää resursoimattomien tekstien scan.
- [ ] Lisää ensimmäiset `values-xx` resurssit päätetyille kielille.
- [ ] Hyväksyntä: ei kovakoodattuja user-facing tekstejä uusissa osissa.

### Osa 95 - Permission/device QA matrix
- [ ] Kirjaa Android-versiot ja laitteet/emulaattorit.
- [ ] Testaa mic, notification, camera, location, foreground service, lockscreen.
- [ ] Hyväksyntä: release-riskit on kirjattu.

### Osa 96 - Billing production QA
- [ ] Tarkista Play Console `dbcheck_pro`.
- [ ] Testaa purchase, pending, cancelled, already-owned, restore.
- [ ] Hyväksyntä: release build ei avaa debug Prota.

### Osa 97 - Release signing QA
- [ ] Testaa AAB build signing secretseillä.
- [ ] Tarkista jarsigner/apksigner ja Play upload.
- [ ] Hyväksyntä: release artifact hyväksytään.

### Osa 98 - Qodana/CI compatibility
- [ ] Tarkista Qodana AGP 9.1.0 yhteensopivuus.
- [ ] Poista `continue-on-error` vain jos workflow on vakaa.
- [ ] Hyväksyntä: CI-status ei piilota release-riskia.

### Osa 99 - Final reports pass
- [ ] Aja tavalliset Gradle-testit.
- [ ] Pyydä käyttäjää ajamaan `lc` ja `sc`.
- [ ] Lue `reports/ktlint.txt`, `reports/detekt.txt`, `reports/lint.txt`, `reports/security-code.txt`, `reports/security-deps.txt`.
- [ ] Hyväksyntä: kaikki failuret on korjattu tai eksplisiittisesti kirjattu.

### Osa 100 - Documentation sync
- [ ] Päivitä `PROJECT.md` nykykoodin perusteella.
- [ ] Päivitä `AGENTS.md` ja `memory/MEMORY.md` arkkitehtuurimuutoksista.
- [ ] Päivitä tämä suunnitelma: valmiit osat `done`, deferred-osat perusteltu.
- [ ] Hyväksyntä: dokumentit eivät lupaa eri tuotetta kuin koodi.

## Toteutuksen etenemisloki

### 2026-06-07 - Suunnitelma pilkottu
- Valmis: Vanha liian laaja suunnitelma korvattu 100 pienemmällä toteutusosalla.
- Kesken jäi: Varsinaista ominaisuuskoodia ei muutettu.
- Seuraava tehtävä: Aloita Osa 1 - Nykytilan lukitus.
- Seuraava komento: `git status --short`
- Ajetut testit: Ei ajettu, suunnitelmadokumentin päivitys.
- Muutetut tiedostot: `dBcheck Missing Features -toteutussuunnitelma.md`
- Huomiot käyttäjälle: Jokaisen osan jälkeen pitää kirjata jatkomuisti ennen seuraavaan osaan siirtymistä.

### 2026-06-09 15:58 - Osa 4
- Valmis: `ResponseTime`-domainmalli lisätty arvoilla Fast 200 ms, Slow 500 ms ja Impulse 50 ms; `UserPreferenceDefaults` normalisoi response time -preference-arvon; Free effective default lukittu `ProAudioPreferencePolicy`-polkuun.
- Kesken jäi: DataStore-avain, `UserPreferences.responseTime`, Settings-state ja mittauspolun käyttö kuuluvat seuraaviin osiin.
- Seuraava tehtävä: Osa 5 - ResponseTime DataStoreen ja Settings-stateen.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.domain.audio.ResponseTimeTest" --tests "com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaultsTest" --tests "com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicyTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/audio/ResponseTime.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaults.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/ProAudioPreferencePolicy.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/ResponseTimeTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaultsTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/model/ProAudioPreferencePolicyTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: Mittauslogiikkaa ei muutettu tässä osassa.

### 2026-06-09 16:08 - Osa 5
- Valmis: `response_time` DataStore-avain, `UserPreferences.responseTime`, `PreferencesRepository.updateResponseTime` ja `SettingsUiState.responseTime` lisätty. SettingsViewModel käyttää Pro-käyttäjälle tallennettua response timea ja Free-käyttäjälle effective defaultia.
- Kesken jäi: Mittauslogiikka ei vielä käytä response timea; domainin smoothing-laskuri kuuluu seuraavaan osaan.
- Seuraava tehtävä: Osa 6 - Response-time laskuri domainiin.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.data.local.preferences.UserPreferencesDataStoreMappingTest" --tests "com.dbcheck.app.data.repository.PreferencesRepositoryTest" --tests "com.dbcheck.app.ui.settings.SettingsViewModelDisplayPreferenceTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStore.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferences.kt`, `app/src/main/java/com/dbcheck/app/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/SettingsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/state/SettingsUiState.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStoreMappingTest.kt`, `app/src/test/java/com/dbcheck/app/data/repository/PreferencesRepositoryTest.kt`, `app/src/test/java/com/dbcheck/app/ui/settings/SettingsViewModelDisplayPreferenceTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: UI-kontrollia ei lisätty tässä osassa; Settings-state ja persistenssipolku ovat valmiina seuraavia osia varten.

### 2026-06-09 16:21 - Osa 6
- Valmis: `ResponseTimeSmoother` ja `ResponseTimeSample` lisätty `domain/audio`-pakettiin. Laskuri tekee dB-arvojen response-time-smoothingin energy-domainissa ja käyttää aikaleimoista laskettua elapsed timea.
- Kesken jäi: `AudioEngine` ei vielä käytä laskuria; effective response time pitää viedä `AudioSessionManager`ilta mittauspolkuun seuraavassa osassa.
- Seuraava tehtävä: Osa 7 - AudioEngine käyttää response timea.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.domain.audio.ResponseTimeSmootherTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/audio/ResponseTimeSmoother.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/ResponseTimeSmootherTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: Laskuri toimii erillään `AudioEngine`sta; out-of-order timestamp hylätään `IllegalArgumentException`-virheellä.

### 2026-06-09 16:38 - Osa 7
- Valmis: `AudioSessionManager` välittää effective response time -arvon `AudioEngine`lle ennen session starttia. Free-käyttäjälle asetetaan aina Fast. `AudioEngine` käyttää `ResponseTimedDecibelReadingProcessor`ia RMS-arvoihin ennen `decibelFlow`-emittiä.
- Kesken jäi: A-painotettu rinnakkaisvirta ei ole vielä mukana; se kuuluu seuraavaan osaan.
- Seuraava tehtävä: Osa 8 - A-painotettu rinnakkaisvirta.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.domain.audio.ResponseTimedDecibelReadingProcessorTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/audio/AudioEngine.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/ResponseTimedDecibelReadingProcessor.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/ResponseTimedDecibelReadingProcessorTest.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: LCpeak ja peak amplitude säilyvät nykyisen chunkin arvoina; response time koskee vain `instantDb`- ja `weightedDb`-RMS-arvoja.

### 2026-06-09 16:58 - Osa 8
- Valmis: `DecibelReading.aWeightedDb` lisätty selected `weightedDb`-arvon rinnalle. `AudioEngine` käyttää omaa A-weighting-filter-instanssia, ja response-time-prosessori silottaa myös A-weighted RMS:n ilman että selected weighted RMS tai C-weighted LCpeak muuttuu.
- Kesken jäi: Room schema / mittausrivien persistointi ja metadata kuuluvat Osa 9:ään.
- Seuraava tehtävä: Osa 9 - MeasurementEntity core-migraatio.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.domain.audio.ResponseTimedDecibelReadingProcessorTest" --tests "com.dbcheck.app.domain.audio.AudioEngineRuntimePreferenceTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/audio/AudioEngine.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/ResponseTimedDecibelReadingProcessor.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/ResponseTimedDecibelReadingProcessorTest.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/AudioEngineRuntimePreferenceTest.kt`, `AGENTS.md`, `memory/MEMORY.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `aWeightedDb` on rinnakkaisdata dosimeterille; se ei vielä tallennu Roomiin tässä osassa.

### 2026-06-09 17:12 - Osa 9
- Valmis: Room schema nostettu v4:ään. `measurements`-riveille lisättiin `aWeightedDb` ja `responseTime`; `MIGRATION_3_4` backfillaa vanhoille riveille `aWeightedDb = dbWeighted` ja `responseTime = FAST`. Uusi v4 schema JSON generoitiin, Room builder käyttää migrationia, ja backup-validaattorin tuettu identity hash päivitettiin.
- Kesken jäi: Dosimeter-standardin asetukset ja laskenta eivät vielä käytä uutta rividataa; ne kuuluvat seuraaviin osiin.
- Seuraava tehtävä: Osa 10 - DosimeterStandard-domainmalli.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.data.local.db.RoomSchemaContractTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest" --tests "com.dbcheck.app.sync.BackupDatabaseValidatorTest" --tests "com.dbcheck.app.data.repository.MeasurementRepositoryRollingWindowTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckDatabase.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckMigrations.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/entity/MeasurementEntity.kt`, `app/src/main/java/com/dbcheck/app/data/repository/MeasurementRepository.kt`, `app/src/main/java/com/dbcheck/app/data/repository/SessionRepository.kt`, `app/src/main/java/com/dbcheck/app/di/DatabaseModule.kt`, `app/src/main/java/com/dbcheck/app/domain/session/Session.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/main/java/com/dbcheck/app/sync/BackupDatabaseValidator.kt`, `app/src/test/java/com/dbcheck/app/data/local/db/RoomSchemaContractTest.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `app/src/test/java/com/dbcheck/app/sync/BackupDatabaseValidatorTest.kt`, `app/schemas/com.dbcheck.app.data.local.db.DbCheckDatabase/4.json`, `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu; migrationin datansäilytys on varmistettu backfill-SQL-kontraktilla ja schema JSON -kontraktilla tässä unit-testitasossa.

### 2026-06-09 17:26 - Osa 10
- Valmis: `DosimeterStandard` lisätty `domain/noise`-pakettiin arvoilla `NIOSH_REL` ja `OSHA_PEL`. DataStore-avain `dosimeter_standard`, fallback `NIOSH_REL`, `UserPreferences.dosimeterStandard`, repository-update ja `SettingsUiState.dosimeterStandard` / `SettingsViewModel.updateDosimeterStandard(...)` lisätty. Free-käyttäjän effective standard pysyy defaultissa `ProAudioPreferencePolicy`n kautta.
- Kesken jäi: Standardi ei vielä vaikuta dosimeter-laskentaan; laskuri kuuluu Osa 11:een.
- Seuraava tehtävä: Osa 11 - DosimeterCalculator.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.domain.noise.DosimeterStandardTest" --tests "com.dbcheck.app.data.local.preferences.UserPreferencesDataStoreMappingTest" --tests "com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaultsTest" --tests "com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicyTest" --tests "com.dbcheck.app.data.repository.PreferencesRepositoryTest" --tests "com.dbcheck.app.ui.settings.SettingsViewModelDisplayPreferenceTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/noise/DosimeterStandard.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStore.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferences.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaults.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/ProAudioPreferencePolicy.kt`, `app/src/main/java/com/dbcheck/app/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/state/SettingsUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/SettingsViewModel.kt`, `app/src/test/java/com/dbcheck/app/domain/noise/DosimeterStandardTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStoreMappingTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaultsTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/model/ProAudioPreferencePolicyTest.kt`, `app/src/test/java/com/dbcheck/app/data/repository/PreferencesRepositoryTest.kt`, `app/src/test/java/com/dbcheck/app/ui/settings/SettingsViewModelDisplayPreferenceTest.kt`, `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: UI-kontrollia ja laskennan kytkentää ei lisätty tässä osassa; Settings-polku on valmis seuraavalle laskurivaiheelle.

### 2026-06-09 18:09 - Osa 11
- Valmis: `DosimeterCalculator` ja `DosimeterExposure` lisätty `domain/noise`-pakettiin. Laskuri tukee NIOSH_REL- ja OSHA_PEL-standardeja sekä palauttaa TWA-, dose-, projected dose- ja remaining exposure time -arvot. `SessionReportCalculator` käyttää samaa laskuria nykyisiin completed reportin NIOSH TWA/dose -kenttiin.
- Kesken jäi: Live altistustilan julkaisu `AudioSessionManager`ista kuuluu Osa 12:een.
- Seuraava tehtävä: Osa 12 - LiveExposureState AudioSessionManageriin.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.domain.noise.DosimeterCalculatorTest" --tests "com.dbcheck.app.domain.report.SessionReportCalculatorTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/noise/DosimeterCalculator.kt`, `app/src/main/java/com/dbcheck/app/domain/report/SessionReportCalculator.kt`, `app/src/test/java/com/dbcheck/app/domain/noise/DosimeterCalculatorTest.kt`, `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu. Laskenta perustuu tarkistettuihin NIOSH/OSHA-parametreihin: NIOSH 85 dBA / 8h / 3 dB exchange / 80 dBA threshold ja OSHA PEL 90 dBA / 8h / 5 dB exchange / 90 dBA threshold.

### 2026-06-09 18:54 - Osa 12
- Valmis: `LiveExposureState` lisätty `AudioSessionManager`in julkiseksi `StateFlow`ksi. State päivittyy jokaisesta `DecibelReading.aWeightedDb`-lukemasta, laskee live LAeq:n ja käyttää effective `DosimeterStandard` + `DosimeterCalculator` -polkua TWA-, dose-, projected dose- ja remaining exposure time -arvoihin.
- Kesken jäi: Meter UI ei vielä valitse DB Meter / Dosimeter -modea; state-kytkentä UI:hin alkaa Osa 13:ssa.
- Seuraava tehtävä: Osa 13 - Meter measurement mode state.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu. Room-persistenssin 1s cadence pysyy `MeasurementPersistenceSampler`in vastuulla; live state päivittyy tiheämmin vain muistissa.

### 2026-06-09 19:01 - Osa 13
- Valmis: `MeasurementMode` lisätty arvoilla `DB_METER` ja `DOSIMETER`. `MeterUiState.measurementMode` defaulttaa `DB_METER`iin, ja `MeterViewModel.setMeasurementMode(...)` vaihtaa moden ilman mittauspalvelun start/stop-sivuvaikutuksia.
- Kesken jäi: Varsinainen chip row ja Free/Pro lock-käytös kuuluvat Osa 14:ään.
- Seuraava tehtävä: Osa 14 - Meter mode chip row.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelForegroundServiceTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelForegroundServiceTest.kt`, `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu. UI:hin ei lisätty näkyviä kontrollia tässä osassa.

### 2026-06-09 19:45 - Osa 14
- Valmis: Meterin yläosaan lisätty `DbCheckChip`-pohjainen mode-rivi. Free-käyttäjän Dosimeter-chip näyttää Pro-lukituksen ja ohjaa Settings/upgrade-polkuun; Pro-käyttäjä voi valita Dosimeter-moden. Chipit saivat selected/locked accessibility-labelit ja Free/Pro screenshot-previewt referenssikuvineen.
- Kesken jäi: Live 30s chart buffer kuuluu Osa 15:een.
- Seuraava tehtävä: Osa 15 - Live 30s chart buffer.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelForegroundServiceTest"`, `.\gradlew.bat :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.MeterModeChipRow*"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/MeterModeChipRowFreePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/MeterModeChipRowProPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelForegroundServiceTest.kt`, `app/gradle.lockfile`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu. Täysi screenshot-validointi näyttää edelleen vanhoja unrelated drift-failureita (`MonthlyTrend*`, `YearlyReport*`, `SessionCardPreview`), joten tässä osassa validointi rajattiin uusiin Meter mode chip -previewihin. `debugScreenshotTestRuntimeClasspath` vaati lockfileen puuttuvat debug-runtime-konfiguraatiot ennen screenshot-taskin ajoa.

### 2026-06-09 20:58 - Osa 15
- Valmis: Lisätty `LiveChartBuffer` ja `LiveChartPointUiState` Meterin state-pakettiin. Buffer pitää live chart -pisteet 30 sekunnin timestamp-ikkunassa, ei listakokorajan perusteella. `MeterViewModel` julkaisee `MeterUiState.liveChartPoints`-listan vain mittauksen ollessa käynnissä, joten pause jäädyttää viimeisimmän listan ja reset tyhjentää sen.
- Kesken jäi: Canvas-komponentti ja varsinainen dB Meter -chart UI kuuluvat Osa 16:een.
- Seuraava tehtävä: Osa 16 - Live 30s chart UI.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.state.LiveChartBufferTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/state/LiveChartBuffer.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/state/LiveChartBufferTest.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelShareTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu. Tässä vaiheessa ei vielä lisätty uutta näkyvää UI-komponenttia; data on valmis Osa 16:n Canvasia varten.

### 2026-06-09 21:44 - Osa 16
- Valmis: Lisätty `LiveSoundLevelChart` Canvas-komponentti Meteriin dB Meter -modeen. Chart käyttää kiinteää 116 dp korkeutta, 0-130 dB akselia, 85 dB threshold-viivaa, yli thresholdin pisteille peak-markerit sekä empty/active/paused state -esitykset. Geometria on erillisessä testattavassa helperissä ja screenshot-previewt kattavat light/dark-teemat.
- Kesken jäi: Sound reference -data kuuluu Osa 17:ään.
- Seuraava tehtävä: Osa 17 - Sound reference data.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.components.LiveSoundLevelChartGeometryTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest"`, `.\gradlew.bat :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.LiveSoundLevelChart*"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/components/LiveSoundLevelChart.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/components/LiveSoundLevelChartGeometry.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/LiveSoundLevelChartActivePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/LiveSoundLevelChartActiveDarkPreview_4b098843_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/LiveSoundLevelChartEmptyPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/LiveSoundLevelChartPausedDarkPreview_4b098843_0.png`, `app/src/test/java/com/dbcheck/app/ui/meter/components/LiveSoundLevelChartGeometryTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu. Täysi screenshot-validointi jätettiin edelleen ajamatta vanhojen unrelated screenshot-driftien takia; uusi chart validoitiin rajatulla `LiveSoundLevelChart*`-filtterillä.

### 2026-06-09 21:55 - Osa 17
- Valmis: `SoundReferenceCatalog` lisätty domain/noise-polkuun CDC:n referenssidatalla, lähimmän referenssin valinnalla ja markeripositioilla. 0-130 dB näyttöasteikko keskitettiin `SoundLevelDisplayScale`en, ja Meterin gauge sekä live chart käyttävät samaa asteikkoa.
- Kesken jäi: Referenssikortin Compose UI, expanded/collapsed-tila ja TalkBack-kuvaus kuuluvat seuraavaan osaan.
- Seuraava tehtävä: Osa 18 - Sound reference card UI.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.domain.noise.SoundReferenceCatalogTest" --tests "com.dbcheck.app.ui.meter.components.LiveSoundLevelChartGeometryTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/noise/SoundLevelDisplayScale.kt`, `app/src/main/java/com/dbcheck/app/domain/noise/SoundReferenceCatalog.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/components/CircularGauge.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/components/LiveSoundLevelChartGeometry.kt`, `app/src/test/java/com/dbcheck/app/domain/noise/SoundReferenceCatalogTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti; katalogi tallentaa sekä UI:ssa käytettävän edustavan dB-arvon että CDC:n alkuperäisen dB-alueen.

### 2026-06-09 22:19 - Osa 18
- Valmis: Meteriin lisätty `SoundReferenceCard`, joka näyttää lähimmän referenssiäänen, nykyisen dB-lukeman, current/reference-markerit ja expanded-listan. Lähin rivi korostuu värin lisäksi check-ikonilla ja `Closest`-badgella. TalkBack-kuvaus kertoo nykyisen lähimmän referenssin ja kortin expanded/collapsed-tilan.
- Kesken jäi: Live equivalent-level -arvo ja Pro/Free-esitystapa kuuluvat Osa 19:ään.
- Seuraava tehtävä: Osa 19 - Live LAeq Meteriin.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest" --tests "com.dbcheck.app.domain.noise.SoundReferenceCatalogTest"`, `.\gradlew.bat :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SoundReferenceCard*"`, `.\gradlew.bat :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SoundReferenceCard*"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/components/SoundReferenceCard.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SoundReferenceCardCollapsedPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SoundReferenceCardExpandedDarkPreview_4b098843_0.png`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelShareTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Ensimmäinen rinnakkainen screenshot-ajo osui Windowsin Gradle-välitiedoston lukkoon, joten screenshot-validointi ajettiin uudelleen yksinään ja se meni läpi.

### 2026-06-10 17:36 - Osa 19
- Valmis: `MeterUiState.equivalentLevelDb` lisätty ja `MeterViewModel` mapittaa sen aktiivisen session `SessionStats.avgDb`-energia-averagesta. Pro-käyttäjällä Meterin keskimmäinen stat-kortti näyttää effective weightingin mukaisen equivalent-labelin (`LAeq`/`LCeq`/`LZeq` jne.), Free-käyttäjälle säilyy nykyinen `AVG`-stat-rivi. Stop-polulle lisättiin regressiotesti, joka varmistaa completed session summaryn `avgDb`-arvon vastaavan live-statistiikan equivalent-arvoa.
- Kesken jäi: Varsinainen dosimeter UI -mallinnus kuuluu Osa 20:een.
- Seuraava tehtävä: Osa 20 - Dosimeter UI data mapping.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `java "-Dorg.gradle.appname=gradlew" -classpath ".\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest.sessionStatsPublishLiveEquivalentLevelForMeterUi" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest.stopSessionPersistsSameEquivalentLevelAsLiveStats"` kaatui odotetusti puuttuvaan `equivalentLevelDb`-kenttään. GREEN: sama rajattu komento meni läpi. Lopullinen relevantti verifiointi: `java "-Dorg.gradle.appname=gradlew" -classpath ".\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest" --tests "com.dbcheck.app.domain.report.SessionReportCalculatorTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelShareTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Ensimmäinen `.\gradlew.bat`-ajo kaatui ennen buildia virheeseen `config.distribution is null`, joten RED/GREEN-kierros ajettiin suoraan `GradleWrapperMain`-classpath-kutsulla. Myöhempi uudelleentarkistus vahvisti, että `.\gradlew.bat --version` ja sama rajattu testikomento toimivat normaalisti, joten wrapper-skriptiä ei muutettu.

### 2026-06-10 18:14 - Osa 20
- Valmis: `DosimeterUiState` lisätty Meterin state-pakettiin tiloilla `LockedPreview`, `Unavailable` ja `Data`. `LiveExposureState.toDosimeterUiState(...)` mapittaa AudioSessionManagerin live-dosimeter-arvot valmiiksi UI-malliksi ilman Compose-laskentaa. `MeterViewModel` kuuntelee `liveExposureState`-virtaa ja päivittää `MeterUiState.dosimeter`-arvon Pro/Free-tilan mukaan; Free saa locked-previewn, Pro saa unavailable-tilan ilman sampleja ja data-tilan live-altistuksella.
- Kesken jäi: Dosimeter gauge ja varsinainen näkyvä UI kuuluvat Osa 21:een.
- Seuraava tehtävä: Osa 21 - Dosimeter gauge UI.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelDosimeterTest"` kaatui odotetusti puuttuvaan `DosimeterUiState`/`MeterUiState.dosimeter`-APIin. GREEN: sama komento meni läpi. Lopullinen relevantti verifiointi: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelDosimeterTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest" --tests "com.dbcheck.app.domain.noise.DosimeterCalculatorTest"`
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/state/DosimeterUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelDosimeterTest.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelTestHarness.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tässä osassa ei lisätty näkyvää gauge UI:ta, vaan ainoastaan Osa 21:n tarvitsema valmis UI-state-malli.

### 2026-06-10 19:00 - Osa 21
- Valmis: Meterin Pro Dosimeter-mode näyttää nyt `DosimeterGaugeCard`-kortin live `DosimeterUiState`-mallista. Kortti näyttää dose %-gaugella riskivärin low/near-limit/over-limit-tiloille, TWA:n, LAeq:n, projected dosen, remaining time -arvon, standard-labelin ja NIOSH/OSHA-reference-infon. Free-käyttäjällä Meter jatkaa DB Meter -chartissa, koska Dosimeter-chip pysyy locked upgrade -polkuna.
- Kesken jäi: Aktiivisen session info bar kuuluu Osa 22:een.
- Seuraava tehtävä: Osa 22 - Meter session info bar.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.components.DosimeterGaugeCardTest"` kaatui odotetusti puuttuvaan `DosimeterGaugeFormatter`/`DosimeterGaugeRiskLevel`-APIin. GREEN/verifiointi: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.components.DosimeterGaugeCardTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelDosimeterTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest"`, `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.DosimeterGauge*"`, `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.DosimeterGauge*"`. Testiapurin siivouksen jälkeen `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.components.DosimeterGaugeCardTest"` ajettiin uudelleen läpi. Lisäksi rajattu trailing whitespace -tarkistus ja rajattu `git diff --check -- ...` menivät läpi; `git diff --check` koko repolle näyttää edelleen checkoutin lukusuojattujen unrelated tiedostojen permission denied -rivejä.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/meter/components/DosimeterGaugeCard.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/DosimeterGaugeLowPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/DosimeterGaugeNearLimitPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/DosimeterGaugeOverLimitDarkPreview_4b098843_0.png`, `app/src/test/java/com/dbcheck/app/ui/meter/components/DosimeterGaugeCardTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Täysi screenshot-suitea ei ajettu; validointi rajattiin uuteen `DosimeterGauge*`-previewperheeseen. Tarkistin myös low- ja over-limit-referenssikuvat visuaalisesti.

### 2026-06-11 15:34 - Osa 22
- Valmis: Meter näyttää aktiivisen session aikana `MeterSessionInfoBar`-rivin, jossa on REC-tila, kesto, effective weighting ja response time. Pro-käyttäjä näkee lisäksi sample raten ja input devicen. `AudioEngine.audioInputInfo` julkaisee aktiivisen tallennuksen input-metadatan ja `MeterViewModel` yhdistää sen `MeterSessionInfoUiState`-malliin yhdessä `AudioSessionManager`in tallennus-/ajastintilan sekä `ProAudioPreferencePolicy`n effective-asetusten kanssa.
- Kesken jäi: AnalyticsSection state kuuluu Osa 23:een.
- Seuraava tehtävä: Osa 23 - AnalyticsSection state.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelSessionInfoTest" --tests "com.dbcheck.app.ui.meter.components.MeterSessionInfoBarTest" --tests "com.dbcheck.app.util.StringResourceIdsTest"` kaatui odotetusti puuttuviin `AudioInputInfo`-, `sessionInfo`-, `MeterSessionInfoFormatter`- ja response time -string APIeihin. GREEN/verifiointi: sama rajattu komento meni läpi. Screenshot-verifiointi: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.MeterSessionInfoBar*"` ja `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.MeterSessionInfoBar*"`. Lopullinen relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.meter.MeterViewModelSessionInfoTest" --tests "com.dbcheck.app.ui.meter.components.MeterSessionInfoBarTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelForegroundServiceTest" --tests "com.dbcheck.app.ui.meter.MeterViewModelShareTest" --tests "com.dbcheck.app.util.StringResourceIdsTest"`.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/domain/audio/AudioEngine.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/AudioInputInfo.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/components/MeterSessionInfoBar.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterSessionInfoUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/state/MeterUiState.kt`, `app/src/main/java/com/dbcheck/app/util/StringResourceIds.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/MeterSessionInfoBarFreePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/MeterSessionInfoBarProDarkPreview_4b098843_0.png`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelSessionInfoTest.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterViewModelTestHarness.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/components/MeterSessionInfoBarTest.kt`, `app/src/test/java/com/dbcheck/app/util/StringResourceIdsTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Viralliset Android-lähteet tarkistettiin `AudioRecord`/`AudioRouting`-reititystiedon sekä Compose state/state-holder -mallin osalta. `.\gradlew.bat` toimi tämän osan verifioinneissa; wrapperia ei muutettu.

### 2026-06-11 18:05 - Osa 23
- Valmis: `AnalyticsSection` lisätty arvoilla `OVERVIEW`, `SPECTRAL` ja `ENVIRONMENT`. `AnalyticsUiState.Success` julkaisee `selectedSection`-kentän ja `AnalyticsViewModel.onSectionSelected(...)` päivittää valinnan tulevaa chip UI:ta varten. Valinta säilyy omassa ViewModel-state-lähteessä, joten analytics-datan uudelleenemissio ei palauta sitä oletukseen. Korttien järjestystä tai näkyvyyttä ei muutettu tässä vaiheessa.
- Kesken jäi: Varsinainen Analytics chip row UI kuuluu Osa 24:ään.
- Seuraava tehtävä: Osa 24 - Analytics chip row UI.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest"` kaatui odotetusti puuttuviin `AnalyticsSection`-, `selectedSection`- ja `onSectionSelected`-APIeihin. GREEN: sama komento meni läpi. Lopullinen relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelRollingWindowTest"`.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose state-, state hoisting- ja lifecycle-aware collection -ohjeet ennen toteutusta.

### 2026-06-11 18:27 - Osa 24
- Valmis: `AnalyticsSectionChipRow` lisätty Analytics-headerin alle ja kytketty `AnalyticsViewModel.onSectionSelected(...)`-polkuun. Free-käyttäjällä Spectral ja Env Mix näkyvät lukkoikoneilla eivätkä piiloudu; Pro-käyttäjällä samat sectionit näkyvät ilman lukkoja. `DbCheckChip` tukee nyt valinnaista leading iconia ja tiukempaa horizontal paddingia, jotta lukitut chipit käyttävät samaa chip-komponenttia ilman erillistä kopiota.
- Kesken jäi: Varsinainen Analytics-korttien ryhmittely valitun sectionin mukaan kuuluu Osa 25:een.
- Seuraava tehtävä: Osa 25 - Analytics card regroup.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionChipRowTest"` kaatui odotetusti puuttuvaan `analyticsSectionChipItems`-APIin. GREEN: sama komponenttitesti meni läpi. Relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionChipRowTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelRollingWindowTest"`. Screenshot-verifiointi: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.AnalyticsSectionChipRow*"` ja `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.AnalyticsSectionChipRow*"`.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionChipRow.kt`, `app/src/main/java/com/dbcheck/app/ui/components/DbCheckChip.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/AnalyticsSectionChipRowFreePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/AnalyticsSectionChipRowProDarkPreview_4b098843_0.png`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionChipRowTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose chip- ja state hoisting -ohjeet ennen toteutusta. Tarkistin myös Free- ja Pro-dark-screenshotit visuaalisesti tekstin mahtumisen ja lukkoikonien osalta.

### 2026-06-11 18:40 - Osa 25
- Valmis: Analytics-korttien renderöinti kulkee nyt `analyticsSectionCards(...)`-helperin kautta. Overview näyttää weekly exposure -slotin, Hearing Healthin, Monthly Trendin, Yearly Reportin ja Hearing Test CTA:n. Spectral-section näyttää vain `SpectralAnalysisCard`in ja Environment-section vain `EnvironmentMixCard`in. `AnalyticsViewModel` ja repository-flow't jätettiin ennalleen, joten section-valinta muuttaa vain renderöitävää korttilistaa.
- Kesken jäi: Overviewin Weekly/Monthly-toggle kuuluu Osa 26:een.
- Seuraava tehtävä: Osa 26 - Weekly/Monthly toggle.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest"` kaatui odotetusti puuttuviin `AnalyticsSectionCard`- ja `analyticsSectionCards`-APIeihin. Lisä-RED varmisti erillisen `HEARING_HEALTH`-korttipaikan. GREEN: sama helper-testi meni läpi. Lopullinen relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest" --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionChipRowTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelRollingWindowTest"`.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContent.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContentTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose state/state-holder -ohjeet ennen toteutusta.

### 2026-06-11 18:56 - Osa 26
- Valmis: Overviewissa on nyt erillinen Weekly/Monthly-valinta. `AnalyticsOverviewRange` ja `AnalyticsUiState.Success.selectedOverviewRange` säilyvät `AnalyticsViewModel`in omassa state-lähteessä, ja `AnalyticsOverviewRangeChipRow` näyttää Weeklyn Free-käyttäjälle auki sekä Monthlyn Pro-lukittuna. `analyticsSectionCards(...)` vaihtaa Overviewissa Weekly-tilassa weekly exposure + hearing health -kortteihin ja Monthly-tilassa monthly trend -korttiin; yearly report ja hearing-test CTA säilyvät Overviewissa molemmissa rangeissa. Pro-laskenta käyttää edelleen nykyistä `ExposureAnalyticsCalculator`-polkua.
- Kesken jäi: SpectralMode Bars kuuluu Osa 27:ään.
- Seuraava tehtävä: Osa 27 - SpectralMode Bars.
- Seuraava komento: `Get-Content -Raw -LiteralPath .\NEXT.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest" --tests "com.dbcheck.app.ui.analytics.components.AnalyticsOverviewRangeChipRowTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest"` kaatui odotetusti puuttuviin `AnalyticsOverviewRange`-, `selectedOverviewRange`-, `onOverviewRangeSelected`- ja `analyticsOverviewRangeChipItems`-APIeihin. GREEN: sama rajattu kokonaisuus lisätyllä `AnalyticsSectionChipRowTest`-regressiolla meni läpi. Screenshot-tarkistus: muokattuun chip-renderöintiin kohdistettu `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.AnalyticsSectionChipRow*"` meni läpi. Jälkikorjaus päivitti vanhentuneet `MonthlyTrend*`, `YearlyReport*` ja `SessionCardPreview` -baseline-kuvat, minkä jälkeen koko `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest` meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsOverviewRangeChipRow.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionChipRow.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContent.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSelectableChip.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/AnalyticsOverviewRangeChipRowTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContentTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Free-käyttäjän Monthly-valinta pysyy locked-previewssa eikä kutsu Pro-mittausten tai yearly session countin repository-kyselyitä.

### 2026-06-11 19:51 - Osa 27
- Valmis: Nykyinen spectral card on nyt eksplisiittisesti `SpectralMode.BARS` -tila. `AnalyticsUiState.Success.selectedSpectralMode` ja `AnalyticsViewModel`in `selectedSpectralMode`-state säilyttävät mode-valinnan dataemissioiden yli. `SpectralModeChipRow` näyttää Bars-valinnan kortissa ja `SpectralAnalysisCard` renderöi nykyisen bar-kaavion `SpectralMode.BARS` -haarasta.
- Kesken jäi: Spectrogram buffer kuuluu Osa 28:aan.
- Seuraava tehtävä: Osa 28 - Spectrogram buffer.
- Seuraava komento: `rg -n "SpectralFrame|SpectralMode|spectrogram|buffer" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.components.SpectralModeChipRowTest"` kaatui odotetusti puuttuviin `SpectralMode`-, `selectedSpectralMode`-, `onSpectralModeSelected`- ja `spectralModeChipItems`-APIeihin. GREEN: sama rajattu unit-testikokonaisuus meni läpi. Screenshot-tarkistus: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SpectralAnalysis*"` kaatui odotetusti vanhoihin `SpectralAnalysis*`-baselineihin, `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SpectralAnalysis*"` päivitti kolme spectral-kuvaa, ja sama `validateDebugScreenshotTest` meni sen jälkeen läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralAnalysisCard.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralModeChipRow.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisIdlePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLivePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLockedPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/SpectralModeChipRowTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Audio-domainia, Pro-gatingia tai spektridatan persistointia ei muutettu.

### 2026-06-11 21:33 - Osa 28
- Valmis: `SpectrogramBuffer` muodostaa `AudioEngine.spectralFrame`-emissioista `SpectrogramUiState.Data` -waterfall-rivit. Bufferi rajautuu 60 viimeisimpään riviin, ohittaa saman timestampin uudelleenemissiot ja tyhjentyy, kun käyttäjä ei ole Pro tai live-frame puuttuu. `AnalyticsUiState.Success.spectrogram` julkaisee datan tulevaa UI:ta varten.
- Kesken jäi: Varsinainen spectrogram Canvas/UI kuuluu Osa 29:ään.
- Seuraava tehtävä: Osa 29 - Spectrogram UI.
- Seuraava komento: `rg -n "SpectrogramUiState|SpectrogramBuffer|SpectralAnalysisCard|Canvas" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.state.SpectrogramBufferTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest"` kaatui odotetusti puuttuviin `SpectrogramBuffer`-, `SpectrogramUiState`- ja `spectrogram`-APIeihin. GREEN: sama rajattu unit-testikokonaisuus meni läpi. Ensimmäinen GREEN-ajo aikakatkaistiin 120 sekunnissa, mutta XML-raportit näyttivät 18/18 ja 4/4 läpäistyä testiä; sama komento ajettiin uudelleen 300 sekunnin aikarajalla ja se palautti `BUILD SUCCESSFUL`.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/SpectrogramBuffer.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/state/SpectrogramBufferTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Spektridataa ei vieläkään persistöidä Roomiin, eikä Osa 28 muuta Pro-gatingin execution-polkuja.

### 2026-06-11 22:01 - Osa 29
- Valmis: `SpectralAnalysisCard` piirtää nyt Bars-näkymään `SpectrogramWaterfall` Canvasin. Live-tila käyttää `SpectrogramUiState.Data` -rivejä, locked-tila käyttää vakaata preview-rivistöä ja idle-tila näyttää tyhjän taustan. Canvasin solut muodostetaan `SpectrogramCanvasModel`-helperillä, joka clippaa amplitudit 0..1-välille. Idle-, locked- ja live-tiloille on erilliset accessibility-tekstit.
- Kesken jäi: Octave/third-octave RTA domain kuuluu Osa 30:een.
- Seuraava tehtävä: Osa 30 - Octave-band RTA domain.
- Seuraava komento: `rg -n "FFTProcessor|SpectralAnalyzer|octave|third" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.SpectrogramCanvasModelTest" --tests "com.dbcheck.app.ui.accessibility.PluralAccessibilityResourceTest.spectrogramDescriptionFormatsSingularAndPluralRows"` kaatui odotetusti puuttuviin `spectrogramRowsFor`-, `spectrogramCanvasCells`-, `SpectrogramCanvasCell`- ja `a11y_spectrogram_live`-rajapintoihin. GREEN: sama testi meni läpi. Screenshot: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SpectralAnalysis*"` kaatui odotetusti kolmeen vanhaan spectral-baselineen, `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SpectralAnalysis*"` päivitti baselinet, ja sama validate meni läpi. Lopuksi `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.SpectrogramCanvasModelTest" --tests "com.dbcheck.app.ui.analytics.state.SpectrogramBufferTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.accessibility.PluralAccessibilityResourceTest"` meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralAnalysisCard.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectrogramCanvasModel.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisIdlePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLivePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLockedPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/ui/accessibility/PluralAccessibilityResourceTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/SpectrogramCanvasModelTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Spektrogrammi on edelleen live-only UI-dataa, eikä Room-persistointia lisätty.

### 2026-06-11 22:21 - Osa 30
- Valmis: `OctaveBandRtaCalculator` lisätty domain/audio-kerrokseen nykyisen `FFTProcessor`in päälle. Laskuri muodostaa `RtaFrame`-datan octave- ja third-octave-resoluutioille, käyttää IEC/ANSI base-10-kaavaa keskitaajuuksiin ja band edgeihin, aggregoi FFT-binien magnitudit bandikohtaisesti ja normalisoi amplitudit vahvimpaan RTA-bandiin. `FFTProcessor.binFrequency(...)` on nyt FFT-binitaajuuden yhteinen lähde, ja `SpectralAnalyzer` käyttää samaa helperiä.
- Kesken jäi: RTA mode, octave bars ja stat pills kuuluvat Osa 31:een.
- Seuraava tehtävä: Osa 31 - RTA UI.
- Seuraava komento: `rg -n "Rta|SpectralMode|SpectralAnalysisCard|RTA" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.OctaveBandRtaCalculatorTest"` kaatui odotetusti puuttuviin `OctaveBandRtaCalculator`- ja `RtaResolution`-rajapintoihin. GREEN: sama uusi testiluokka meni läpi. Relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.OctaveBandRtaCalculatorTest" --tests "com.dbcheck.app.domain.audio.SpectralAnalyzerTest" --tests "com.dbcheck.app.domain.audio.FFTProcessorTest"` meni läpi. Lisäksi kosketetuille domain-tiedostoille tehty rivipituustarkistus ei palauttanut yli 120 merkin rivejä.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/domain/audio/FFTProcessor.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/OctaveBandRtaCalculator.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/SpectralAnalyzer.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/OctaveBandRtaCalculatorTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin IEC/ANSI fractional-octave-lähteet ennen toteutusta. Osa 30 ei kytke RTA-dataa UI:hin, Pro-gatingiin, `AudioEngine.spectralFrame`-virtaan tai Room-persistointiin.

### 2026-06-11 22:48 - Osa 31
- Valmis: `SpectralMode` tukee nyt `BARS`, `SPECTROGRAM` ja `RTA` -valintoja. `AudioEngine.rtaFrame` julkaisee `OctaveBandRtaCalculator`in octave-RTA-datan samassa Pro-gatetyssa live-spektripolussa kuin `spectralFrame`, ja `AnalyticsViewModel` mapittaa sen `RtaUiState`-malliksi. `SpectralAnalysisCard` renderöi Bars-, Spectrogram- ja RTA-haarat erikseen: RTA näyttää octave-baarit, taajuuslabelit sekä PEAK/BANDS-stat pillit. RTA Canvasin ja stat pillien data muodostetaan `RtaBarsModel`-helperillä, ja Hz/kHz-muotoilu on yhteisessä `formatSpectralFrequency(...)`-helperissä.
- Kesken jäi: Laajemmat spectral stat pillsit, dominant frequency, bandwidth, peak band ja live/idle kuuluvat Osa 32:een.
- Seuraava tehtävä: Osa 32 - Spectral stat pills.
- Seuraava komento: `rg -n "SpectralMetric|formatSpectralFrequency|RtaStatPill|dominantFrequency|peak" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.SpectralModeChipRowTest" --tests "com.dbcheck.app.ui.analytics.components.RtaBarsModelTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.accessibility.PluralAccessibilityResourceTest.rtaBarsDescriptionFormatsSingularAndPluralBands"` kaatui odotetusti puuttuviin `SPECTROGRAM`/`RTA`-, `RtaUiState`-, `AudioEngine.rtaFrame`-, RTA-mallifunktio- ja `a11y_rta_bars_live`-rajapintoihin. GREEN: sama rajattu testijoukko meni läpi. Relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.SpectralModeChipRowTest" --tests "com.dbcheck.app.ui.analytics.components.RtaBarsModelTest" --tests "com.dbcheck.app.ui.analytics.components.SpectrogramCanvasModelTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelRollingWindowTest" --tests "com.dbcheck.app.domain.audio.AudioEngineRuntimePreferenceTest" --tests "com.dbcheck.app.domain.audio.OctaveBandRtaCalculatorTest" --tests "com.dbcheck.app.ui.accessibility.PluralAccessibilityResourceTest"` meni läpi. Screenshot: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SpectralAnalysis*"` kaatui odotetusti uusiin/muuttuneisiin baselineihin, `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SpectralAnalysis*"` päivitti ne, ja sama validate meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/domain/audio/AudioEngine.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/RtaBarsModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralAnalysisCard.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralFormatters.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralModeChipRow.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisIdlePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLivePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLockedPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisRtaPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisSpectrogramPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/domain/audio/AudioEngineRuntimePreferenceTest.kt`, `app/src/test/java/com/dbcheck/app/ui/accessibility/PluralAccessibilityResourceTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelRollingWindowTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/RtaBarsModelTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/SpectralModeChipRowTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Compose Canvas-, semantics- ja state hoisting -ohjeet ennen toteutusta. RTA-data pysyy live-only UI-polussa eikä sitä persistöidä Roomiin.

### 2026-06-11 23:14 - Osa 32
- Valmis: Spectral Analysis -kortin yleiset mittarit ovat nyt yhteisiä stat-pillejä: DOMINANT, BANDWIDTH, PEAK BAND ja STATUS. `SpectralStatPillsModel` muodostaa arvot `SpectralAnalysisUiState`sta, bandwidth/status-copyt pysyvät string-resursseissa ja taajuudet kulkevat yhden `formatSpectralFrequency(...)`-helperin kautta. `SpectralBandUiState` kuljettaa nyt `centerFrequencyHz`-arvon live- ja spectrogram-polussa, joten peak band voidaan johtaa live-datasta eikä kovakoodatusta tekstistä. RTA:n PEAK/BANDS-pillimalli käyttää samaa `SpectralStatPill`-rakennetta.
- Kesken jäi: Realtime Environment Mix -kertymä ja prosenttipyöristys kuuluvat Osa 33:een.
- Seuraava tehtävä: Osa 33 - Real-time Environment Mix accumulator.
- Seuraava komento: `rg -n "EnvironmentMix|NoiseLevel|isRecording|MeasurementPersistenceSampler|SessionStats" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.SpectralStatPillsModelTest" --tests "com.dbcheck.app.ui.analytics.components.SpectralFormattersTest" --tests "com.dbcheck.app.ui.analytics.components.RtaBarsModelTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest"` kaatui odotetusti puuttuviin `SpectralStatPill`-, `spectralStatPillsFor`-, `centerFrequencyHz`- ja uusiin string-resurssirajapintoihin. GREEN: sama rajattu testijoukko meni läpi. Screenshot: `.\gradlew.bat :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTests*SpectralAnalysis*"` kaatui odotetusti vanhoihin spectral-baselineihin, `.\gradlew.bat :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTests*SpectralAnalysis*"` päivitti baselinet, ja sama validate meni läpi. Lopuksi sama rajattu unit-testijoukko meni uudelleen läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/RtaBarsModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralAnalysisCard.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralFormatters.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SpectralStatPillsModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/SpectrogramBuffer.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisIdlePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLivePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisLockedPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisRtaPreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SpectralAnalysisSpectrogramPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/RtaBarsModelTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/SpectralFormattersTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/SpectralStatPillsModelTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Compose state hoisting-, semantics-, Flow layouts- ja modifier-ohjeet ennen toteutusta; toteutuksessa päädyttiin kahden sarakkeen rivimalliin ilman uutta experimental FlowRow -riippuvuutta.

### 2026-06-12 10:06 - Osa 33
- Valmis: `ExposureAnalyticsCalculator` sisältää nyt aktiivisen session Environment Mix -helperit: `buildEnvironmentMixCounts(...)`, `environmentMixPercentages(...)` ja `EnvironmentExposureMixCounts.withWeightedDb(...)`. Countit käyttävät `NoiseLevel.fromDb(...)`-luokittelua, joten realtime ja historiallinen mix nojaavat samaan dB-rajalähteeseen. `AudioSessionManager` julkaisee `liveEnvironmentMixCounts`-StateFlow'n ja kasvattaa sitä jokaisesta aktiivisen session weighted-lukemasta. `AnalyticsViewModel` mapittaa tämän `activeEnvironmentMix`-kentäksi Pro+recording-tilassa, joten data on valmis Osa 34:n UI:lle.
- Kesken jäi: Environment-sectionin varsinainen active-session UI ja historical/realtime-erottelu kuuluvat Osa 34:ään.
- Seuraava tehtävä: Osa 34 - Real-time Environment Mix UI.
- Seuraava komento: `rg -n "activeEnvironmentMix|EnvironmentMixCard|EnvironmentMixUiState|selectedSection|ENVIRONMENT" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.domain.analytics.ExposureAnalyticsCalculatorTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest.liveEnvironmentMixCountsTrackWeightedReadingsFromActiveSession" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest.proUserReceivesActiveEnvironmentMixWhileRecording" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest.freeUserDoesNotReceiveActiveEnvironmentMixWhileRecording"` kaatui odotetusti puuttuviin `buildEnvironmentMixCounts`-, `environmentMixPercentages`-, `liveEnvironmentMixCounts`- ja `activeEnvironmentMix`-rajapintoihin. GREEN: sama uusi rajattu joukko ja `AnalyticsViewModelRollingWindowTest` menivät läpi. Relevantti regressio: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.domain.analytics.ExposureAnalyticsCalculatorTest" --tests "com.dbcheck.app.service.SessionStatsCalculatorTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelRollingWindowTest"` meni läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/analytics/ExposureAnalyticsCalculator.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/test/java/com/dbcheck/app/domain/analytics/ExposureAnalyticsCalculatorTest.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelRollingWindowTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Flow/UI-state- ja Kotlin collection aggregation/grouping -ohjeet ennen toteutusta. Osa 33 ei lisää uutta UI-komponenttia eikä muuta historical Environment Mix -kortin renderöintiä.

### 2026-06-12 10:15 - Osa 34
- Valmis: Environment-section näyttää nyt Pro-käyttäjän aktiivisen mittauksen aikana erillisen `LIVE ENVIRONMENT MIX` -kortin ennen historiallista `7-DAY ENVIRONMENT MIX` -korttia. `AnalyticsSectionContent` erottaa `ACTIVE_ENVIRONMENT_MIX`- ja `ENVIRONMENT_MIX`-kortit, ja `AnalyticsScreen` lukee live-kortille `activeEnvironmentMix`-tilaa sekä historiakortille edelleen `environmentMix`-tilaa. Free-käyttäjälle aktiivista live-korttia ei renderöidä, joten lukitun näkymän alle ei viedä realtime-dataa.
- Kesken jäi: LiteRT/TFLite- ja YAMNet-riippuvuuksien virallinen tarkistus sekä lukitus kuuluvat Osa 35:een.
- Seuraava tehtävä: Osa 35 - Dependency research ja lukitus.
- Seuraava komento: `rg -n "libs.versions.toml|tensorflow|tflite|litert|yamnet|dependencyLocking|verification" .`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest"` kaatui odotetusti puuttuviin `isRecording`/`isProUser`-parametreihin ja `ACTIVE_ENVIRONMENT_MIX`-korttiin. GREEN: sama testi meni läpi. Relevantti regressio: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelRollingWindowTest"` meni läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContent.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/EnvironmentMixCard.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContentTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Compose list-, state-, semantics-, accessibility- ja testing-ohjeet ennen toteutusta. Osa 34 ei muuta live Environment Mix -accumulatoria eikä historiallista 7 päivän queryä.

### 2026-06-12 10:32 - Osa 35
- Valmis: Virallisista lähteistä tarkistettiin LiteRT/TFLite-audioluokittelun polku ennen riippuvuuden lisäämistä. Google AI Edge AudioClassifier -ohje käyttää Task Libraryn audio-riippuvuutena `org.tensorflow:tensorflow-lite-task-audio:0.4.4`, ja LiteRT-migraatio-ohje rajaa Task Libraryn edelleen TensorFlow Lite -paketteihin. Maven Centralin metadata vahvisti `tensorflow-lite-task-audio`-artifactin uusimmaksi julkaistuksi versioksi `0.4.4`. Appiin lisättiin version catalog -alias `tensorflow-lite-task-audio` ja `implementation(libs.tensorflow.lite.task.audio)` ilman classifier-koodia. Gradle lockfileen lisättiin tämän polun runtime/compile-artefaktit, ja dependency verificationiin lisättiin vain raportissa nimetyt TensorFlow/ODML-artefaktit: Task Audio, Task Base, Support API, Lite API ja ODML image.
- Tutkimushuomio Osa 36:een: Google AI Edge ohjaa pretrained LiteRT-mallien jakelussa Kaggle Models -lähteeseen, ja vanha TFHub YAMNet TFLite -URL ohjaa Kaggleen. TensorFlow Hubin YAMNet-ohjeen mukaan YAMNet ennustaa 521 AudioSet-luokkaa, käyttää 16 kHz mono-audiota ja labelit ovat mallin asseteissa class map -tiedostona. Osa 36:n pitää lisätä malli- ja label-assetit sekä attribution, ei rakentaa inferenceä vielä.
- Kesken jäi: varsinaisen YAMNet model assetin ja label-tiedoston lisääminen sekä license/attribution kuuluvat Osa 36:een.
- Seuraava tehtävä: Osa 36 - Model asset ja labels.
- Seuraava komento: `rg -n "assets|yamnet|tflite|label|license|attribution|class_map|noCompress" app/src/main AndroidManifest.xml licenses .`
- Ajetut testit: RED/metadata: `.\gradlew.bat :app:compileDebugKotlin :app:compileReleaseKotlin --write-locks` kaatui ensin dependency verificationiin 10 uudella TensorFlow/ODML-artefaktilla ja toisella ajolla kahteen `tensorflow-lite-api:2.13.0` artifactiin. GREEN: verification metadata ja lockfile paivitettiin rajatusti, minkä jälkeen `.\gradlew.bat :app:compileDebugKotlin :app:compileReleaseKotlin --write-locks` meni läpi. Lopullinen varmistus ilman päivityslippuja: `.\gradlew.bat :app:compileDebugKotlin :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `app/build.gradle.kts`, `app/gradle.lockfile`, `gradle/libs.versions.toml`, `gradle/verification-metadata.xml`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Play services -LiteRT-riippuvuutta ei lisätty, koska nykyinen seuraava tarve on Task Libraryn `AudioClassifier` ja YAMNet asset -paketointi; inference-, raw-audio-adapteri- ja UI-kytkennät kuuluvat myöhempiin osiin.

### 2026-06-12 10:52 - Osa 36
- Valmis: YAMNet `classification-tflite/1` -malli lisätty Android assetiksi polkuun `app/src/main/assets/sound_detection/yamnet.tflite`, ja AudioSet class map lisätty label-tiedostoksi polkuun `app/src/main/assets/sound_detection/yamnet_class_map.csv`. Asset-polut keskitettiin `YamnetModelAssets`-objektiin, jotta myöhemmät classifier-osat eivät kovakoodaa samoja polkuja useaan paikkaan.
- Valmis: Attribution lisätty tiedostoon `licenses/yamnet/ATTRIBUTION.md`. Dokumentti kirjaa Kaggle/TFHub-mallilähteen, TensorFlow Models class map -lähteen, Apache 2.0 -lisenssin ja SHA-256-tunnisteet.
- Kesken jäi: raw PCM -> 16 kHz mono float window -adapteri kuuluu Osa 37:ään. Inferenceä, live-fanoutia, UI:ta tai raaka-audion persistointia ei lisätty tässä osassa.
- Seuraava tehtävä: Osa 37 - Audio window adapter.
- Seuraava komento: `rg -n "AudioProcessingConfig|sampleRate|PCM|FloatArray|AudioEngine|DecibelReading|YamnetModelAssets" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.YamnetModelAssetsTest"` kaatui ensin odotetusti unit-testikonfiguraatioiden puuttuviin TensorFlow lock -riveihin; lockit päivitettiin `debugUnitTestRuntimeClasspath`- ja `debugUnitTestCompileClasspath`-konfiguraatioille. Sama RED kaatui tämän jälkeen odotetusti `FileNotFoundException`iin puuttuvista YAMNet asseteista. GREEN: sama `YamnetModelAssetsTest` meni läpi assettien lisäämisen jälkeen. Release-paketointi: `.\gradlew.bat :app:mergeReleaseAssets` meni läpi, ja release-välituotteesta löytyivät `sound_detection/yamnet.tflite` sekä `sound_detection/yamnet_class_map.csv`.
- Muutetut tiedostot: `app/gradle.lockfile`, `app/src/main/assets/sound_detection/yamnet.tflite`, `app/src/main/assets/sound_detection/yamnet_class_map.csv`, `app/src/main/java/com/dbcheck/app/domain/audio/YamnetModelAssets.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/YamnetModelAssetsTest.kt`, `licenses/yamnet/ATTRIBUTION.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Google AI Edge AudioClassifier-, TensorFlow YAMNet- ja Kaggle/TFHub-lähteet ennen assettien lisäämistä. AGP 4.1+ lisää `.tflite`-tiedostot noCompress-listalle oletuksena, joten erillistä Gradle `noCompress` -asetusta ei lisätty.

### 2026-06-12 11:13 - Osa 37
- Valmis: `YamnetAudioConfig` keskittää YAMNetin 16 kHz target-sample raten ja 15 600 näytteen input-windowin. `YamnetAudioWindowAdapter` ottaa nykyisen PCM16 `ShortArray`-audion, resamplaa sen jatkuvalla 44,1 kHz -> 16 kHz -kadenssilla, normalisoi arvot `[-1.0, 1.0]`-alueelle ja palauttaa vasta täyden classifier-windowin.
- Valmis: Adapteri ei persistoi raakaaudiota eikä sitä kytketty Roomiin, service-kerrokseen, `AudioEngine`-fanoutiin tai UI:hin. Streaming-tila säilyttää vain viimeisimmän float-windowin, output-sample-positionin ja yhden edellisen normalisoidun float-samplen chunk-rajalla tapahtuvaa interpolointia varten.
- Kesken jäi: Varsinainen `SoundClassifier`-rajapinta, tuotantototeutus ja fake-testiluokitus kuuluvat Osa 38:aan.
- Seuraava tehtävä: Osa 38 - SoundClassifier-portti.
- Seuraava komento: `rg -n "YamnetAudio|AudioClassifier|SoundClassifier|Classifications|tensorflow-lite-task-audio" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.YamnetAudioWindowAdapterTest"` kaatui odotetusti puuttuviin `YamnetAudioWindowAdapter`- ja `YamnetAudioConfig`-tyyppeihin. Ensimmäinen GREEN meni läpi, minkä jälkeen lisätty kadenssitesti `appendPcm16PreservesTargetSampleCadenceAcrossAudioChunks` kaatui odotetusti per-chunk-pyöristyksen 15 999 target-samplen driftaukseen. Resampler korjattiin jatkuvaksi chunkkien yli, ja `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.YamnetAudioWindowAdapterTest"` meni läpi. Relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.YamnetAudioWindowAdapterTest" --tests "com.dbcheck.app.domain.audio.YamnetModelAssetsTest" --tests "com.dbcheck.app.domain.audio.AudioEngineRuntimePreferenceTest"` meni läpi. Release-käännös: `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/audio/YamnetAudioConfig.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/YamnetAudioWindowAdapter.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/YamnetAudioWindowAdapterTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Google AI Edge AudioClassifier- ja TensorFlow YAMNet -ohjeet ennen toteutusta. Rinnakkainen Gradle-testi aiheutti Windowsissa välitiedostolukon ja yksi timeoutiin jäänyt `--rerun-tasks`-prosessi ehti päättyä ennen pysäytystä; lopulliset verifikaatiot ajettiin yksittäin `--no-daemon`illa.

### 2026-06-12 11:26 - Osa 38
- Valmis: `SoundClassifier`-portti, `SoundClassification`-domain-malli ja `SoundClassificationPolicy` erottavat threshold/empty-output-logiikan inference-toteutuksesta. `TfliteSoundClassifier` käyttää YAMNet-assetin `AudioClassifier.createFromFileAndOptions(...)` -polkua, lataa 16 kHz float-windowin `TensorAudio.load(float[])` -kutsulla ja mapittaa Task Audio -kategoriat domain-malliin. Hilt-provider palauttaa portin `SoundClassifier`-rajapintana, mutta live-fanoutia ei vielä kytketty.
- Valmis: Unit/ViewModel-testejä varten lisättiin uudelleenkäytettävä `FakeSoundClassifier`, joka palauttaa jonotetut tulokset ja kopioi luokitellut input-windowit testien tarkistettavaksi. `SoundClassificationPolicyTest` kattaa empty outputin, confidence thresholdin alituksen, thresholdin inklusiivisen rajan ja korkeimman confidence-tuloksen valinnan.
- Kesken jäi: Raw-audio fanout, Pro/toggle-gating, current sound type -live state ja recent detections kuuluvat Osa 39:ään.
- Seuraava tehtävä: Osa 39 - Sound detection live state.
- Seuraava komento: `rg -n "SoundClassifier|YamnetAudioWindowAdapter|raw audio|isProUser|sound detection|recent" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.SoundClassificationPolicyTest" --tests "com.dbcheck.app.domain.audio.FakeSoundClassifierTest"` kaatui odotetusti puuttuviin `SoundClassifier`-, `SoundClassification`-, `SoundClassificationPolicy`- ja `SoundClassifierConfig`-tyyppeihin. GREEN: sama rajattu testijoukko meni läpi portin ja fake-luokan lisäämisen jälkeen. Release-käännös: `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/di/AppModule.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/SoundClassifier.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/TfliteSoundClassifier.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/FakeSoundClassifier.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/FakeSoundClassifierTest.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/SoundClassificationPolicyTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin Google AI Edge Task Audio -dokumentaation ja paikallisen `tensorflow-lite-task-audio:0.4.4` AARin class-signatuurit ennen adapteria. Osa 38 ei muuta `AudioEngine`ä, ei aja inferenceä Free/Pro-polussa eikä persistoi raw-audiota.

### 2026-06-12 11:59 - Osa 39
- Valmis: `SoundDetectionWindowFanout` kytkettiin `AudioEngine`en live-only raw-audio fanoutiksi. Se muuntaa PCM16-chunkit YAMNet-float-windoweiksi vain, kun sound detection on päällä, käyttää pudottavaa yhden windowin `SharedFlow`-bufferia eikä tallenna raakaaudiota. `AudioEngine` ei kutsu classifieria.
- Valmis: `UserPreferences.soundDetectionEnabled` lisättiin DataStoreen ja repositoryyn. `AudioSessionManager` laskee effective ehdon `isProUser && soundDetectionEnabled`, ohjaa `AudioEngine.setSoundDetectionEnabled(...)` -porttia ja kerää `audioEngine.soundDetectionWindows`-virtaa erillisessä jobissa. `SoundClassifier` ajetaan vain effective-tilassa, ja `soundDetectionState` julkaisee `isEnabled`, current detectionin sekä uusimmat detectionit.
- Kesken jäi: Environment-sectionin sound type -kortti sekä locked/idle/live/error UI-statejen renderöinti kuuluvat Osa 40:een.
- Seuraava tehtävä: Osa 40 - Environment UI sound detection.
- Seuraava komento: `rg -n "SoundDetectionState|soundDetectionState|EnvironmentMixCard|AnalyticsSectionContent|sound type|sound_detection" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.domain.audio.SoundDetectionWindowFanoutTest" --tests "com.dbcheck.app.data.local.preferences.UserPreferencesDataStoreMappingTest" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest.freeUserDoesNotRunSoundDetectionInferenceEvenWhenToggleIsEnabled" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest.proUserWithSoundDetectionToggleOffDoesNotRunInference" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest.proUserWithSoundDetectionToggleOnPublishesCurrentAndRecentDetections"` kaatui odotetusti puuttuviin `SoundDetectionWindowFanout`-, `soundDetectionEnabled`-, `SoundDetectionState`-, `setSoundDetectionEnabled`- ja `soundDetectionState`-rajapintoihin. Ensimmäinen GREEN paljasti testisynkronoinnin puutteen fanout-collectorissa, joka korjattiin lisäämällä `runCurrent()` collector-startin jälkeen. Lopullinen rajattu GREEN meni läpi.
- Relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest" --tests "com.dbcheck.app.domain.audio.SoundDetectionWindowFanoutTest" --tests "com.dbcheck.app.domain.audio.YamnetAudioWindowAdapterTest" --tests "com.dbcheck.app.domain.audio.SoundClassificationPolicyTest" --tests "com.dbcheck.app.domain.audio.AudioEngineRuntimePreferenceTest" --tests "com.dbcheck.app.data.local.preferences.UserPreferencesDataStoreMappingTest" --tests "com.dbcheck.app.data.repository.PreferencesRepositoryTest"` meni läpi. Release-käännös: `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStore.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaults.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferences.kt`, `app/src/main/java/com/dbcheck/app/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/AudioEngine.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/SoundDetectionState.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/SoundDetectionWindowFanout.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStoreMappingTest.kt`, `app/src/test/java/com/dbcheck/app/data/repository/PreferencesRepositoryTest.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/AudioEngineRuntimePreferenceTest.kt`, `app/src/test/java/com/dbcheck/app/domain/audio/SoundDetectionWindowFanoutTest.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin Androidin virallisen StateFlow/SharedFlow-ohjeen ennen live-state/SharedFlow-kytkentää. Osa 39 ei lisää UI-korttia, error-statea, detection-persistointia tai raw-audion tallennusta.

### 2026-06-12 12:27 - Osa 40
- Valmis: Environment-section näyttää nyt `SOUND TYPE` -kortin ennen active/historical Environment Mix -kortteja. `AnalyticsViewModel` lukee `AudioSessionManager.soundDetectionState`-virtaa live-dataflowssa ja mapittaa Free-käyttäjälle locked previewn, Pro-käyttäjälle idle/live/error-tilat sekä current/recent detectionit valmiiksi UI-malliksi. `SoundDetectionCard` näyttää tilat vakaalla korttikorkeudella, eikä UI laske tai tallenna raakaaudiota.
- Valmis: Classifier-poikkeus ei enää kaada sound detection -collector-polun työtä, vaan `AudioSessionManager` julkaisee `SoundDetectionError.CLASSIFICATION_UNAVAILABLE` -tilan. ViewModel muuntaa tämän resurssipohjaiseksi error-tekstiksi.
- Kesken jäi: Detection-eventtien aggregoitu opt-in-persistointi, Room-taulu, delete/export-semantics ja migration-testit kuuluvat Osa 41:een.
- Seuraava tehtävä: Osa 41 - Optional detection persistence.
- Seuraava komento: `rg -n "SoundDetection|soundDetection|Room|Migration|privacy|export|delete" app/src/main/java app/src/test/java app/src/main/res dbcheck-privacy-policy.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest.freeUserReceivesLockedSoundDetectionPreview" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest.proUserReceivesIdleSoundDetectionWhenNoCurrentTypeExists" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest.proUserReceivesLiveSoundDetectionFromSessionManager" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest.proUserReceivesSoundDetectionErrorState" --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest.soundDetectionClassifierFailurePublishesErrorState"` kaatui odotetusti puuttuviin `SOUND_DETECTION`-, `SoundDetectionUiState`-, `SoundDetectionError`- ja `soundDetection`-rajapintoihin. GREEN: sama rajattu testijoukko meni läpi toteutuksen jälkeen.
- Ajetut testit: Screenshot-baselinet generoitiin komennolla `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SoundDetection*"`. Ensimmäinen screenshot-ajo vaati `--write-locks`, koska `debugScreenshotTestCompileClasspath`-lukosta puuttuivat Osa 35:n YAMNet-riippuvuuden transitiiviset rivit. Lopullinen `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.SoundDetection*"` meni läpi, ja locked/idle/live/error-previewt ovat saman korkuisia.
- Relevantti regressio: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests "com.dbcheck.app.service.AudioSessionManagerAudioStartTest" --tests "com.dbcheck.app.ui.analytics.AnalyticsViewModelSpectralTest" --tests "com.dbcheck.app.ui.analytics.components.AnalyticsSectionContentTest" --tests "com.dbcheck.app.domain.audio.SoundDetectionWindowFanoutTest" --tests "com.dbcheck.app.domain.audio.SoundClassificationPolicyTest"` meni läpi. `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi. `git diff --check` ei raportoinut whitespace-virheitä.
- Muutetut tiedostot: `app/gradle.lockfile`, `app/src/main/java/com/dbcheck/app/domain/audio/SoundDetectionState.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/AnalyticsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContent.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/components/SoundDetectionCard.kt`, `app/src/main/java/com/dbcheck/app/ui/analytics/state/AnalyticsUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SoundDetectionErrorDarkPreview_4b098843_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SoundDetectionIdlePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SoundDetectionLivePreview_74131fac_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/SoundDetectionLockedPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/TestStringContext.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/AnalyticsViewModelSpectralTest.kt`, `app/src/test/java/com/dbcheck/app/ui/analytics/components/AnalyticsSectionContentTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose state-hoisting-, Compose testing- ja Compose Preview Screenshot Testing -ohjeet ennen UI-toteutusta. Osa 40 ei lisää detection-persistointia eikä raakaaudion tallennusta.

### 2026-06-12 13:08 - Osa 41
- Valmis: Room-skeema nostettu v5:een ja `sound_detection_events`-taulu lisätty vain aggregoiduille sound detection -eventeille (`sessionId`, timestamp, label, confidence). Taulu cascadoituu sessioon, eikä siinä ole raakaaudiota tai YAMNet-float-windowia. `SoundDetectionRepository` on uusi kirjoitusportti.
- Valmis: DataStoreen lisätty erillinen `sound_detection_persistence` opt-in. `AudioSessionManager` tallentaa eventtejä vain ehdolla `isProUser && soundDetectionEnabled && soundDetectionPersistenceEnabled`, ja aggregoi per aktiivinen sessio label-vaihdoksiin, jotta samaa labelia ei kirjoiteta jokaisesta classifier-windowista.
- Valmis: CSV-export kirjoittaa nyt session-, measurement- ja `dBcheck_sound_detections_*.csv` -tiedostot samaan Sharesheetiin. Sound detection -CSV sisältää vain aggregoidut timestamp/label/confidence-rivit ja käyttää samaa FileProvider/cache-politiikkaa kuin muut exportit. `dbcheck-privacy-policy.md` täsmensi opt-in-, export- ja delete-semanticsin.
- Kesken jäi: CameraX-riippuvuuksien virallinen tarkistus ja lukitus kuuluvat Osa 42:een.
- Seuraava tehtävä: Osa 42 - CameraX dependency research.
- Seuraava komento: `rg -n "CameraX|camera|libs.versions.toml|dependencyLocking|verification" .`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.data.local.preferences.UserPreferencesDataStoreMappingTest --tests com.dbcheck.app.data.repository.PreferencesRepositoryTest --tests com.dbcheck.app.data.export.CsvExportFormatterTest --tests com.dbcheck.app.data.export.ExportCsvUseCaseTest --tests com.dbcheck.app.data.local.db.RoomSchemaContractTest --tests com.dbcheck.app.service.AudioSessionManagerAudioStartTest` kaatui odotetusti puuttuviin `SoundDetectionEventEntity`-, `SoundDetectionEventDao`-, `SoundDetectionRepository`-, `SoundDetectionEvent`-, `soundDetectionPersistenceEnabled`-, CSV- ja migration-rajapintoihin. GREEN: sama kohdennettu testijoukko meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `dbcheck-privacy-policy.md`, `app/schemas/com.dbcheck.app.data.local.db.DbCheckDatabase/5.json`, `app/src/main/java/com/dbcheck/app/data/export/CsvExportFormatter.kt`, `app/src/main/java/com/dbcheck/app/data/export/ExportCsvUseCase.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckDatabase.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckMigrations.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckSchema.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/dao/SoundDetectionEventDao.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/entity/SoundDetectionEventEntity.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStore.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaults.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferences.kt`, `app/src/main/java/com/dbcheck/app/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/dbcheck/app/data/repository/SoundDetectionRepository.kt`, `app/src/main/java/com/dbcheck/app/di/DatabaseModule.kt`, `app/src/main/java/com/dbcheck/app/domain/audio/SoundDetectionEvent.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/test/java/com/dbcheck/app/data/export/CsvExportFormatterTest.kt`, `app/src/test/java/com/dbcheck/app/data/export/ExportCsvUseCaseTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/db/RoomSchemaContractTest.kt`, `app/src/test/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStoreMappingTest.kt`, `app/src/test/java/com/dbcheck/app/data/repository/PreferencesRepositoryTest.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Room migration-, DataStore- ja Google Play User Data -ohjeet ennen toteutusta. Osa 41 ei lisää Settings UI -kytkintä opt-inille; se lisää DataStore/repository-portin ja execution-gaten seuraavia UI-osia varten.

### 2026-06-12 23:27 - Osa 42
- Valmis: Tarkistin virallisesta AndroidX CameraX release-dokumentaatiosta ja CameraX-ohjeista tämän hetken production-käyttöön sopivan stable-julkaisun. Osa 42 lukitsee CameraX-version `1.6.1`; uudempaa alpha-julkaisua ei otettu production-polkuun.
- Valmis: Version catalogiin lisättiin yksi `cameraX`-versiolähde sekä CameraX-aliakset `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` ja `camera-video`. `app/build.gradle.kts` käyttää näitä aliaksia tulevia preview-, photo- ja video-overlay-osia varten.
- Valmis: Dependency lock state ja `gradle/verification-metadata.xml` päivitettiin Gradlen omilla `--write-locks` ja `--write-verification-metadata sha256` -poluilla. Uusi `CameraDependencyContractTest` lukitsee version catalogin ja appin riippuvuussopimuksen.
- Kesken jäi: `CAMERA` manifest-permission, runtime permission -policy/helper ja denied/permanent denied -testit kuuluvat Osa 43:een. UI:ta, routea tai CameraX preview-sessionia ei lisätty tässä osassa.
- Seuraava tehtävä: Osa 43 - CAMERA permission.
- Seuraava komento: `rg -n "CAMERA|Manifest.permission.CAMERA|rememberLauncherForActivityResult|Permission" app/src/main/java app/src/test/java app/src/main/AndroidManifest.xml`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.build.CameraDependencyContractTest` kaatui odotetusti puuttuviin `cameraX`-version catalog -riviin ja app-dependencyihin. Ensimmäinen GREEN vaati dependency lock staten päivityksen; lock- ja verification-metadata päivitettiin komennoilla `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.build.CameraDependencyContractTest --write-locks` ja `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.build.CameraDependencyContractTest --write-locks --write-verification-metadata sha256`. Lopullinen `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.build.CameraDependencyContractTest` meni läpi ilman päivityslippuja. Build-hyväksyntä `.\gradlew.bat :app:compileDebugKotlin :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/build.gradle.kts`, `app/gradle.lockfile`, `app/src/test/java/com/dbcheck/app/build/CameraDependencyContractTest.kt`, `gradle/libs.versions.toml`, `gradle/verification-metadata.xml`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. CameraX-riippuvuudet ovat nyt valmiina, mutta sovellus ei vielä pyydä kameran lupaa eikä avaa kameraa.

### 2026-06-12 23:46 - Osa 43
- Valmis: `AndroidManifest.xml` deklaroi nyt `android.permission.CAMERA`-luvan Camera Overlay -polkua varten.
- Valmis: `CameraPermissionPolicy` antaa tulevalle kamera-route UI:lle testattavan runtime permission -päätöksen: initial request, granted, denied/rationale ja permanently denied/settings. Policy käyttää `Manifest.permission.CAMERA` -lupaa, eikä se koske Meterin mikrofonilupaa tai mittauksen start-politiikkaa.
- Valmis: Regressiotesti varmistaa manifest-luvan, granted/denied/permanently denied -tilat sekä sen, että `MeterStartupPermissionPolicy` pyytää edelleen vain mikrofonia startupissa.
- Kesken jäi: `Screen.CameraOverlay`, NavHost route, bottom navin piilotus ja Meteristä avattava Pro-gatettu entry kuuluvat Osa 44:ään.
- Seuraava tehtävä: Osa 44 - Camera route.
- Seuraava komento: `rg -n "Screen\\.|DbCheckNavHost|BottomNav|MeterControls|ProLockOverlay|CameraOverlay" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraPermissionPolicyTest --tests com.dbcheck.app.privacy.PrivacyConfigTest.manifestDeclaresCameraPermissionForCameraOverlay --tests com.dbcheck.app.ui.meter.MeterStartupPermissionPolicyTest` kaatui odotetusti puuttuviin `CameraPermissionPolicy`- ja `CameraPermissionStatus`-tyyppeihin. GREEN: sama rajattu testijoukko meni läpi toteutuksen jälkeen.
- Muutetut tiedostot: `PROJECT.md`, `AGENTS.md`, `memory/MEMORY.md`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraPermissionPolicy.kt`, `app/src/test/java/com/dbcheck/app/privacy/PrivacyConfigTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraPermissionPolicyTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android runtime permission -ohjeet ennen muutosta. Osa 43 ei lisää kamera-UI:ta, nav routea, CameraX preview bindingia eikä photo/video capturea.

### 2026-06-13 00:03 - Osa 44
- Valmis: `Screen.CameraOverlay` lisättiin reitiksi `camera_overlay`, ja `DbCheckNavHost` rekisteröi sille oman `CameraOverlayRoute`-composablen. Reitti ei kuulu `BottomNavDestination`-listaan eikä `selectedTopLevelRouteFor(...)` palauta sille top-level-kohdetta, joten bottom nav ja navigation rail piiloutuvat overlayllä.
- Valmis: Kamera-overlayn nykyinen route-sisältö on rajattu turvalliseen suljettavaan shelliin: close-ikoni kutsuu `navController.popBackStack()` -polkua. Varsinainen fullscreen preview, permission denial UI ja CameraX-sidonta jätettiin seuraaviin osiin.
- Valmis: Meterin controls-riville lisättiin kamera-entry. `MeterCameraOverlayEntryPolicy` ohjaa Pro-käyttäjän kamera-overlay-reitille ja Free-käyttäjän nykyiseen Pro-upgrade-polkuun.
- Kesken jäi: fullscreen fake/static preview UI, overlay slotit, permission denial UI ja screenshot-testit kuuluvat Osa 45:een.
- Seuraava tehtävä: Osa 45 - Camera preview shell.
- Seuraava komento: `rg -n "CameraOverlayRoute|CameraPermissionPolicy|PreviewView|CameraX|MeterControls|PhotoCamera" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat :app:testDebugUnitTest --tests com.dbcheck.app.ui.navigation.NavigationRoutePolicyTest --tests com.dbcheck.app.ui.meter.MeterCameraOverlayEntryPolicyTest` kaatui odotetusti puuttuviin `Screen.CameraOverlay`-, `MeterCameraOverlayEntryDestination`- ja `MeterCameraOverlayEntryPolicy`-symboleihin. GREEN: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.navigation.NavigationRoutePolicyTest --tests com.dbcheck.app.ui.meter.MeterCameraOverlayEntryPolicyTest` meni läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/navigation/Screen.kt`, `app/src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterCameraOverlayEntryPolicy.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/meter/components/MeterControls.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/ui/navigation/NavigationRoutePolicyTest.kt`, `app/src/test/java/com/dbcheck/app/ui/meter/MeterCameraOverlayEntryPolicyTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Navigation Compose -ohjeet ennen reittikytkentää. Osa 44 ei avaa kameraa, sido CameraX-previewtä, pyydä kameran runtime-lupaa route-UI:ssa tai lisää capture-toimintoja.

### 2026-06-13 10:19 - Osa 45
- Valmis: `CameraOverlayScreen` on nyt fullscreen Compose-shell, jossa on staattinen fake camera preview, testattavat `previewContent`- ja `overlayContent`-slotit sekä sulkunappi. Default-overlay näyttää tulevaa live dB -readoutia vastaavan staattisen `82 dB / A-weighted` -kerroksen, mutta ei lue mittausdataa eikä sido CameraX:ää.
- Valmis: Permission-denial UI käsittelee `ShouldRequest`/`Denied`-tilat retry-CTA:lla ja `PermanentlyDenied`-tilan Settings-CTA:lla. `CameraOverlayRoute` käyttää tässä vaiheessa granted-shelliä eikä vielä pyydä runtime-lupaa.
- Valmis: Lisätty screenshot-previewt granted-, denied- ja permanently denied dark -tiloille. Ensimmäinen baseline-tarkistus paljasti granted-layoutissa close/readout-overlapin, joka korjattiin ennen lopullisten baselinejen validointia.
- Kesken jäi: lifecycle CameraX `Preview`/`PreviewView` -sidonta, camera unavailable -tila ja manual device smoke kuuluvat Osa 46:een.
- Seuraava tehtävä: Osa 46 - CameraX preview binding.
- Seuraava komento: `rg -n "CameraOverlayScreen|CameraStaticPreview|CameraPermissionStatus|PreviewView|ProcessCameraProvider|LifecycleCameraController|CameraSelector" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest` kaatui odotetusti puuttuviin preview/overlay slotteihin ja denied-tilojen käsittelyyn. GREEN: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest --tests com.dbcheck.app.ui.navigation.NavigationRoutePolicyTest` meni läpi. Screenshot RED: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` kaatui odotetusti puuttuviin baseline-kuviin. Baseline-päivitys: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` meni läpi. Lopullinen screenshot GREEN: sama `validateDebugScreenshotTest`-komento meni läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/CameraOverlayDeniedPreview_80830277_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/CameraOverlayGrantedPreview_80830277_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/CameraOverlayPermanentlyDeniedDarkPreview_ad40a636_0.png`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayShellContractTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Compose Preview Screenshot Testing-, Compose semantics- ja Compose state-hoisting -ohjeet ennen UI-shelliä. Osa 45 ei lisää CameraX-sessionia, photo/video capturea, MediaStore-kirjoitusta tai raakaaudion käsittelyä.

### 2026-06-13 14:00 - Osa 46
- Valmis: `CameraOverlayRoute` pyytää nyt kameran runtime-luvan `CameraPermissionPolicy`n kautta, päivittää luvan tilan `ON_RESUME`ssa ja näyttää granted-tilassa oikean `CameraXPreviewContent`-sisällön. `CameraXPreviewContent` rakentaa `PreviewView`n, hakee `ProcessCameraProvider`in ja sitoo `Preview.Builder().build()` -use casen `bindToLifecycle(..., CameraSelector.DEFAULT_BACK_CAMERA, preview)` -polkuun.
- Valmis: Camera unavailable -tila on oma fallback: jos providerin haku tai lifecycle-bind epäonnistuu, route vaihtaa `CameraPreviewUnavailableContent`-näkymään eikä jätä overlay-readoutia rikkinäisen previewn päälle. Fallbackille lisättiin screenshot-preview ja baseline.
- Valmis: Manual smoke ajettiin `Pixel_10`-emulaattorilla. `:app:installDebug` asensi APK:n, `CAMERA` ja `RECORD_AUDIO` grantattiin adb:llä, camera-entry avattiin Meteriltä, ja `build/camera-smoke-overlay.png` näyttää CameraX:n virtual scene -previewn sekä `READY / 82 dB / A-weighted` -overlayn. Logcat vahvisti kameravirran rivillä `CameraService: Start camera streaming for com.dbcheck.app`.
- Kesken jäi: live-mittausarvon lukeminen overlayyn, photo/video capture ja burned-in overlay kuuluvat seuraaviin osiin.
- Seuraava tehtävä: Osa 47 - Live dB overlay.
- Seuraava komento: `rg -n "CameraOverlayReadout|DecibelReading|currentDb|weightedDb|activeSession|MeterUiState|AudioSessionManager" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraXPreviewBindingContractTest` kaatui odotetusti puuttuviin CameraX-binding-, permission route- ja unavailable fallback -sopimuksiin. GREEN: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraXPreviewBindingContractTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest` meni läpi. Screenshot RED: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` kaatui odotetusti puuttuvaan `CameraOverlayUnavailablePreview`-baselineen. Baseline-päivitys: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` meni läpi. Lopulliset tarkistukset: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"`, `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin`, `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraXPreviewBindingContractTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest --tests com.dbcheck.app.ui.camera.CameraPermissionPolicyTest` ja `.\gradlew.bat --no-daemon --console=plain :app:installDebug` menivät läpi.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/CameraOverlayUnavailablePreview_80830277_0.png`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraXPreviewBindingContractTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android CameraX Preview- ja CameraX Architecture -ohjeet ennen lifecycle-bindingin toteutusta. Smoke-emulaattori sammutettiin testin jälkeen komennolla `adb -s emulator-5554 emu kill`.

### 2026-06-13 14:19 - Osa 47
- Valmis: `CameraOverlayViewModel` lukee live dB -arvon `AudioEngine.decibelFlow`sta vain, kun `AudioSessionManager.isRecording` on tosi ja lukeman timestamp on aktiivisen session `activeSessionStartTimeMs` -arvon jälkeen. Näin overlay ei näytä edellisen session replay-lukemaa idle-tilassa eikä käynnistä tai pysäytä mittausta itse.
- Valmis: `CameraOverlayRoute` kerää ViewModelin `uiState`n `collectAsStateWithLifecycle()`-polulla ja välittää sen `CameraOverlayReadout`ille. Readout näyttää `READY / -- dB / <label> / No live reading` ilman aktiivista mittausta ja `LIVE / <dB> / <label> / Updated HH:mm:ss` aktiivisesta lukemasta.
- Valmis: Painotuslabel tulee samasta effective preference -polusta kuin Meterin live-state (`ProAudioPreferencePolicy.weighting(...)` + `equivalentLevelLabelForWeighting(...)`), joten Free-käyttäjän overlay putoaa default-painotukseen eikä UI laske mittausarvoja itse.
- Kesken jäi: photo capture, burned-in overlay, `content://`-jako ja manual share test kuuluvat Osa 48:aan. Silent video capture kuuluu Osa 49:ään.
- Seuraava tehtävä: Osa 48 - Photo capture burned-in overlay.
- Seuraava komento: `rg -n "ImageCapture|MediaStore|ShareResultsGenerator|ExportFileCache|FileProvider|CameraOverlayReadout" app/src/main/java app/src/test/java app/src/screenshotTest/kotlin`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraOverlayViewModelTest` kaatui odotetusti puuttuviin `CameraOverlayViewModel`/state-symboleihin. GREEN: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraOverlayViewModelTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest --tests com.dbcheck.app.ui.camera.CameraXPreviewBindingContractTest --tests com.dbcheck.app.ui.camera.CameraPermissionPolicyTest` meni läpi. Screenshot RED: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` kaatui odotetusti `CameraOverlayGrantedPreview`-baselineen, koska idle-readout vaihtui. Baseline-päivitys: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` meni läpi. Lopulliset tarkistukset: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"`, `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` ja sama camera-unit-testikomento menivät läpi.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayViewModel.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayShellContractTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayViewModelTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose state- ja lifecycle-aware state collection -ohjeet ennen ViewModel/UI-kytkentää. Laitetason aktiivimittaus-smokea ei ajettu tässä osassa; aktiivinen ja idle-mittauspolku on katettu ViewModel-testeillä. Myöhempi yhdistetty `:app:testDebugUnitTest :app:validateDebugScreenshotTest` -yritys laajensi unit-ajon koko testisettiin ja kaatui kahteen `AnalyticsViewModelRollingWindowTest`-testiin; Osa 47:n rajattu camera-unit-ajo, camera-screenshot-ajo ja release-käännös ajettiin sen jälkeen erikseen onnistuneesti.

### 2026-06-13 16:58 - Osa 48
- Valmis: `CameraOverlayRoute` sitoo nyt CameraX `ImageCapture` -use casen previewn rinnalle ja välittää capture-hallinnan ViewModelille. Capture-painike kirjoittaa raakakuvan export-cacheen väliaikaisena JPG-tiedostona.
- Valmis: `CameraOverlayShareGenerator` polttaa nykyisen overlay-readoutin PNG-bittikarttaan, kirjoittaa sen `ExportFileCache`n `cache/exports`-polkuun, poistaa väliaikaisen raakakuvan ja julkaisee jaon `FileProvider`in `content://`-URIlla, `ClipData`lla ja `FLAG_GRANT_READ_URI_PERMISSION` -luvalla.
- Valmis: Manual share smoke ajettiin fyysisellä `46190DLAQ001B3` / Pixel 9 - 17 -laitteella. Camera privacy promptin `Unblock`-valinnan jälkeen CameraX-preview aukesi, capture avasi Android Sharesheetin tekstillä `dBcheck camera overlay`, ja `adb exec-out run-as com.dbcheck.app cat cache/exports/dBcheck_camera_overlay_20260613_165554.png > build\dBcheck_camera_overlay_smoke.png` vahvisti, että jaettava PNG sisältää burned-in `READY / -- dB / LAeq / No live reading` -overlayn.
- Kesken jäi: silent video capture kuuluu Osa 49:ään.
- Seuraava tehtävä: Osa 49 - Silent video capture.
- Seuraava komento: `rg -n "Recorder|VideoCapture|AudioConfig|QualitySelector|camera-video|CameraOverlayRoute|ImageCapture" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraOverlayShareGeneratorTest --tests com.dbcheck.app.ui.camera.CameraOverlayViewModelTest --tests com.dbcheck.app.ui.camera.CameraXPreviewBindingContractTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest` kaatui odotetusti puuttuviin photo capture/share -symboleihin. GREEN ja lopulliset tarkistukset: sama camera-unit-testikomento meni läpi, `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` meni läpi, `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi ja `.\gradlew.bat --no-daemon --console=plain :app:installDebug` meni läpi. Manual smoke avasi Sharesheetin ja pullasi PNG-todisteen; `/sdcard`-kopio epäonnistui laitteen käyttöoikeuksiin, mutta `exec-out run-as cat` onnistui.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayViewModel.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayShareGeneratorTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayShellContractTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayViewModelTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraXPreviewBindingContractTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android CameraX ImageCapture-, MediaStore/shared media- ja FileProvider-ohjeet ennen toteutusta. Osa 48 ei lisää uutta raakaaudion keräystä, ei muuta mittaussession käynnistystä eikä tee pysyvää MediaStore-kirjoitusta.

### 2026-06-14 08:35 - Osa 49
- Valmis: `CameraOverlayRoute` sitoo nyt CameraX `VideoCapture<Recorder>` -use casen `Preview`n ja `ImageCapture`n rinnalle. Tallennus kirjoittaa `FileOutputOptions`illa export-cacheen `dBcheck_camera_silent_video_*.mp4` -tiedoston ja käyttää `prepareRecording(context, outputOptions).start(...)` -polkua ilman `withAudioEnabled()`-kutsua.
- Valmis: Overlayyn lisättiin video start/stop -painike ja privacy-copy `Video records camera only. Microphone audio is not saved.` Photo capture disabloidaan videon tallennuksen aikana, jotta use case -yhdistelmä pysyy rajattuna.
- Valmis: Visual overlay -rajauksen dokumentointi: nykyinen silent-video tallentaa CameraX:n kamerastreamin ilman Compose-overlayn burned-in-renderöintiä. Overlayn polttaminen MP4:ään vaatii erillisen renderöinti- tai post-processing-polun, kuten CameraX effect / Media3 / frame pipeline -suunnittelun, eikä sitä lisätä tähän Osa 49 -rajaukseen.
- Kesken jäi: Session location design kuuluu Osa 50:een.
- Seuraava tehtävä: Osa 50 - Session location design.
- Seuraava komento: `rg -n "ACCESS_COARSE_LOCATION|ACCESS_FINE_LOCATION|Location|location|Data Safety|privacy" app/src/main/AndroidManifest.xml app/src/main/java PROJECT.md dbcheck-privacy-policy.md`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.camera.CameraOverlayShareGeneratorTest --tests com.dbcheck.app.ui.camera.CameraOverlayViewModelTest --tests com.dbcheck.app.ui.camera.CameraXPreviewBindingContractTest --tests com.dbcheck.app.ui.camera.CameraOverlayShellContractTest` kaatui odotetusti puuttuviin `createSilentVideoFile`-, video-state- ja route-symboleihin. GREEN: sama camera-unit-testikomento meni läpi 21 testillä. Lisäksi `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` ja `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.CameraOverlay*"` menivät läpi.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt`, `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayViewModel.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayShareGeneratorTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayShellContractTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraOverlayViewModelTest.kt`, `app/src/test/java/com/dbcheck/app/ui/camera/CameraXPreviewBindingContractTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin virallisen Android CameraX VideoCapture -ohjeen sekä paikallisen CameraX 1.6.1 API-pinnan ennen toteutusta. `adb devices` ei listannut laitetta tämän osan lopussa, joten uutta laitetason video-smokea tai `ffprobe`-audio-stream-varmistusta ei voitu ajaa tässä turnissa.

### 2026-06-14 08:43 - Osa 50
- Valmis: Androidin viralliset location permission -ohjeet, approximate-only codelab, runtime permission -ohje, Play background location -ohje, permission minimization -ohje ja Play Data Safety -ohje tarkistettiin ennen scope-lukitusta.
- Valmis: Session location lukittiin optional foreground approximate-only -metadataksi. Myöhemmässä toteutuksessa manifestiin saa lisätä korkeintaan `ACCESS_COARSE_LOCATION`; `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` ja foreground service `location` -tyyppi eivät kuulu nykyiseen scopeen ilman uutta product/privacy-päätöstä.
- Valmis: Runtime-pyyntö rajattiin käyttäjän sijaintitoiminnon yhteyteen. Denied/unavailable/ei-foreground stop -tilanteessa mittaussession tallennus ei saa epäonnistua, vaan sijainti jää tyhjäksi.
- Valmis: Privacy-copy ja Data Safety -muistiinpano lisättiin `dbcheck-privacy-policy.md`-luonnokseen. Copy: sijainti on valinnainen, approximate-only ja session tunnistamista varten; dBcheck ei käytä precise locationia, background locationia, jatkuvaa seurantaa, mainontaa tai analytiikkaa sijaintidatan tarkoituksena.
- Kesken jäi: Session location schema, migration ja testit kuuluvat Osa 51:een.
- Seuraava tehtävä: Osa 51 - Session location schema.
- Seuraava komento: `rg -n "SessionEntity|SessionDao|DbCheckMigrations|DATABASE_VERSION|schema|session.*location|location.*session" app/src/main/java app/src/test/java app/schemas`
- Ajetut testit: Ei Gradle-testejä, koska Osa 50 teki vain dokumentti-/scope-päätöksiä eikä lisännyt location-manifestia tai Kotlin-tuotantokoodia. Verifiointi tehtiin `rg`- ja `git diff` -tarkistuksilla: source/manifestiin ei lisätty `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` tai location-koodia tässä osassa.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `dbcheck-privacy-policy.md`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tämä osa ei vielä toteuta sijaintischemaa tai permission-pyyntöä; se lukitsee permission- ja privacy-scopeen, jonka mukaan Osa 51/52 voidaan toteuttaa.

### 2026-06-14 08:55 - Osa 51
- Valmis: Room `sessions`-tauluun lisättiin nullable optional location -metadatasarakkeet `locationLatitude`, `locationLongitude`, `locationAccuracyMeters` ja `locationCapturedAt`. Kentät ovat entityssä nullable-oletuksilla, joten nykyiset insert-kutsut ja vanhat sessiot toimivat ilman locationia.
- Valmis: `DbCheckDatabase.SCHEMA_VERSION` nostettiin versioon 6, `MIGRATION_5_6` lisää neljä nullable-saraketta ilman defaultteja/backfilliä, ja `DatabaseModule` rekisteröi migrationin.
- Valmis: Room schema export `app/schemas/com.dbcheck.app.data.local.db.DbCheckDatabase/6.json` generoitiin ja `RoomSchemaContractTest` tarkistaa version 6:n, nullable location -sarakkeet, migration SQL:n ja migrationin rekisteröinnin.
- Kesken jäi: One-shot location capture, runtime permission ja denied/unavailable-polut kuuluvat Osa 52:een.
- Seuraava tehtävä: Osa 52 - One-shot location capture.
- Seuraava komento: `rg -n "ACCESS_COARSE_LOCATION|Location|locationLatitude|locationLongitude|locationAccuracyMeters|locationCapturedAt|SessionRepository|AudioSessionManager|SessionEntity" app/src/main/java app/src/test/java app/src/main/AndroidManifest.xml`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.data.local.db.RoomSchemaContractTest` kaatui odotetusti puuttuviin version 6-, `MIGRATION_5_6`-, schema 6- ja entity-kenttäkohtiin. GREEN: sama Room schema -testikomento meni läpi. Lopputarkistus: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.data.local.db.RoomSchemaContractTest` exit 0 ja `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` exit 0.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/data/local/db/entity/SessionEntity.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckDatabase.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/DbCheckMigrations.kt`, `app/src/main/java/com/dbcheck/app/di/DatabaseModule.kt`, `app/src/test/java/com/dbcheck/app/data/local/db/RoomSchemaContractTest.kt`, `app/schemas/com.dbcheck.app.data.local.db.DbCheckDatabase/6.json`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Room migration/schema -ohjeet ennen toteutusta. Osa 51 ei lisää manifestiin location-permissionia eikä tee runtime location capturea.

### 2026-06-14 09:12 - Osa 52
- Valmis: Lisättiin `SessionLocationCapturePort` ja Android-adapteri `AndroidSessionLocationCapturePort`, joka lukee vain manifestissa olevan `ACCESS_COARSE_LOCATION`/mahdollisen foreground location -luvan jälkeen parhaan last-known-sijainnin `LocationManager`ilta. Permissionin, providerin tai API:n puuttuessa adapteri palauttaa `null`.
- Valmis: `AudioSessionManager` yrittää tallentaa optional session location -metadatan aktiivisen session luonnin jälkeen ja stopissa vain fallbackina, jos startissa ei saatu sijaintia. Sijainticapture ja `SessionRepository.updateSessionLocation(...)` on eristetty niin, etteivät denied/unavailable/update-virheet kaada mittauksen start/stop- tai completion-polkuja.
- Valmis: `SessionLocationMetadata` lisättiin domain-malliin ja `SessionEntity` -> `Session` -mapperiin. Room-päivitys kulkee `SessionDao.updateSessionLocation(...)` -partial update -kyselyllä eikä muuta session measurement/summary-transaktioita.
- Valmis: Manifestiin lisättiin vain `ACCESS_COARSE_LOCATION`. `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` ja foreground service `location` -tyyppi jätettiin edelleen pois Osa 50:n product/privacy-rajauksen mukaisesti.
- Valmis: Fake location provider -testit kattavat start-capturen, stop-fallbackin ja tilanteen, jossa sijaintia ei saada eikä sessio epäonnistu.
- Kesken jäi: UI/runtime permission -pyyntö ja käyttäjän sijaintitoiminnon affordance eivät kuulu tähän osaan; ilman myönnettyä runtime-lupaa Android-adapteri jättää location-metadatan tyhjäksi.
- Seuraava tehtävä: Osa 53 - History search query.
- Seuraava komento: `rg -n "getSessions|SessionDao|SessionRepository|History|Search|Filter|locationLatitude|locationLongitude|frequencyWeighting|startTime|NoiseLevel" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.data.repository.SessionRepositoryMutationTest --tests com.dbcheck.app.service.AudioSessionManagerAudioStartTest` kaatui odotetusti puuttuviin `SessionLocationMetadata`-, `SessionLocationCapturePort`- ja `updateSessionLocation`-symboleihin. GREEN: sama kohdennettu testikomento meni läpi. Lopputarkistus: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.data.repository.SessionRepositoryMutationTest --tests com.dbcheck.app.service.AudioSessionManagerAudioStartTest --tests com.dbcheck.app.data.local.db.RoomSchemaContractTest --tests com.dbcheck.app.architecture.DataBoundaryContractTest` exit 0 ja `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` exit 0.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `dbcheck-privacy-policy.md`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/dbcheck/app/di/AppModule.kt`, `app/src/main/java/com/dbcheck/app/domain/session/Session.kt`, `app/src/main/java/com/dbcheck/app/data/model/SessionMappers.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/dao/SessionDao.kt`, `app/src/main/java/com/dbcheck/app/data/repository/SessionRepository.kt`, `app/src/main/java/com/dbcheck/app/service/AudioSessionManager.kt`, `app/src/main/java/com/dbcheck/app/service/SessionLocationCapturePort.kt`, `app/src/test/java/com/dbcheck/app/data/repository/SessionRepositoryMutationTest.kt`, `app/src/test/java/com/dbcheck/app/service/AudioSessionManagerAudioStartTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android location permission-, LocationManager- ja foreground service type -ohjeet ennen toteutusta.

### 2026-06-14 14:38 - Osa 53
- Valmis: Lisättiin `SessionHistoryQuery` domainiin sekä `SessionDao.searchSessions(...)` completed-history -query, joka tukee name/tag `LIKE` -hakua, date rangea, avg dB -tasorajaa, exact frequency weighting -filtteriä ja has-location -filtteriä.
- Valmis: `SessionRepository.getFilteredSessions(...)` mapittaa queryn DAO-parametreiksi, escapeaa `%`, `_` ja `\` -merkit literal `LIKE` -patterniin ja säilyttää Free-käyttäjän 7 päivän history policy -alarajan. Pro-käyttäjälle history-raja on `Long.MIN_VALUE`.
- Valmis: Deterministinen järjestys on `ORDER BY startTime DESC, id DESC`, sama tie-breaker kuin muissa History-kyselyissä.
- Valmis: Robolectric Room -testi todentaa SQL-filtterit ja stable orderingin; repository-testit todentavat Free-policy-mappauksen ja filtteröityjen domain-sessioiden palautuksen.
- Kesken jäi: History UI:n search field, filter chips, Free locked preview ja screenshot-testit kuuluvat Osa 54:ään.
- Seuraava tehtävä: Osa 54 - History search UI.
- Seuraava komento: `rg -n "HistoryScreen|HistoryViewModel|HistoryUiState|SessionHistoryQuery|getFilteredSessions|ProLockOverlay|Filter|Search|SessionCard" app/src/main/java app/src/test/java app/src/screenshotTest`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.data.local.db.SessionDaoHistorySearchQueryTest --tests com.dbcheck.app.data.repository.SessionRepositoryHistoryPolicyTest` kaatui odotetusti puuttuviin `searchSessions`-, `SessionHistoryQuery`- ja `getFilteredSessions`-symboleihin. GREEN: sama testikomento meni läpi uudelleenajossa pidemmällä aikarajalla. Lopputarkistus: sama kohdennettu testikomento exit 0 ja `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` exit 0.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/domain/session/Session.kt`, `app/src/main/java/com/dbcheck/app/data/local/db/dao/SessionDao.kt`, `app/src/main/java/com/dbcheck/app/data/repository/SessionRepository.kt`, `app/src/test/java/com/dbcheck/app/data/local/db/SessionDaoHistorySearchQueryTest.kt`, `app/src/test/java/com/dbcheck/app/data/repository/SessionRepositoryHistoryPolicyTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Room DAO/query -ohjeet ennen toteutusta.

### 2026-06-15 11:45 - Osa 54
- Valmis: History UI sai Pro-käyttäjälle search fieldin sekä filter chipit `All`, `A-weighted`, `Loud` ja `Location`. ViewModel mapittaa kontrollit `SessionHistoryQuery`-malliin ja käyttää `SessionRepository.getFilteredSessions(...)` -polkua vain, kun Pro-käyttäjällä on aktiivinen hakuehto.
- Valmis: Free-käyttäjä saa lukitun preview-kortin upgrade-affordancella. Free-polku ei kutsu filtteröityä repository-hakua, ja lopullinen `visibleToUser(...)`-rajaus säilyy History UI-statessa direct-open gate -regression estämiseksi.
- Valmis: Aktiivinen Pro-haku näyttää `No matching sessions` -tilan, jos filtteri ei palauta sessioita mutta History-ruutu on muuten käytettävissä.
- Valmis: Lisättiin screenshot-previewt ja baseline-kuvat Pro- ja Free-locked History search -tiloille.
- Seuraava tehtävä: Osa 55 - Histogram calculator.
- Seuraava komento: `rg -n "SessionReportCalculator|SessionDetail|Histogram|bucket|ReportMeasurement|WeightedExposureMeasurement" app/src/main/java app/src/test/java`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.history.HistoryViewModelViewAllTest` kaatui odotetusti puuttuviin `HistorySearchFilter`-, `updateSearchQuery`- ja `selectSearchFilter`-symboleihin. GREEN: sama History ViewModel -testi meni läpi. Screenshot RED: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.HistorySearchControls*"` kaatui odotetusti puuttuviin baseline-kuviin. Baseline-päivitys: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.HistorySearchControls*"` meni läpi. Lopputarkistukset: sama HistorySearch screenshot -validointi ja `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` menivät läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/history/HistoryScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/history/HistoryViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/history/components/HistorySearchControls.kt`, `app/src/main/java/com/dbcheck/app/ui/history/state/HistoryUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/HistorySearchControlsLockedDarkPreview_4b098843_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/HistorySearchControlsProPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/ui/history/HistoryViewModelViewAllTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose chip -ohjeet ennen UI-toteutusta ja käytin lopulta projektin omaa `DbCheckChip`-komponenttia yhdenmukaisuuden vuoksi.

### 2026-06-15 13:55 - Osa 55
- Valmis: Lisättiin domain/report-tason `DbHistogramCalculator`, joka palauttaa vakaan 0-130 dB histogrammidatan 10 dB bucketeissa. Alle 0 dB arvot menevät ensimmäiseen buckettiin, 130 dB ja yli menevät viimeiseen buckettiin.
- Valmis: `DbHistogramBucket` sisältää bucketin `minDb`/`maxDb`, sample-määrän ja kokonaisprosentin. Prosentit pyöristetään deterministisesti niin, että ei-tyhjän datan näkyvät bucket-prosentit summautuvat 100:aan.
- Valmis: `SessionReportCalculator` liittää histogrammin `SessionReportData.dbHistogramBuckets` -kenttään samasta järjestetystä `ReportMeasurement`-listasta kuin time series ja peak events, joten Session Detail voi lukea histogrammidatan suoraan raporttimallista.
- Seuraava tehtävä: Osa 56 - Histogram UI.
- Seuraava komento: `rg -n "dbHistogramBuckets|SessionDetailScreen|ProLockOverlay|Histogram|ReportPoint|PeakEventsCard|KpiGrid" app/src/main/java app/src/test/java app/src/screenshotTest`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.domain.report.DbHistogramCalculatorTest --tests com.dbcheck.app.domain.report.SessionReportCalculatorTest` kaatui odotetusti puuttuviin `DbHistogramCalculator`-, `DbHistogramBucket`- ja `dbHistogramBuckets`-symboleihin. GREEN: sama komento meni läpi. Lopputarkistus: `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/domain/report/DbHistogramCalculator.kt`, `app/src/main/java/com/dbcheck/app/domain/report/SessionReportCalculator.kt`, `app/src/main/java/com/dbcheck/app/domain/report/SessionReportData.kt`, `app/src/test/java/com/dbcheck/app/domain/report/DbHistogramCalculatorTest.kt`, `app/src/test/java/com/dbcheck/app/domain/report/SessionReportCalculatorTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin Kotlinin viralliset collection aggregate/grouping -ohjeet ennen laskurilogiikkaa; toteutus ei lisää Android-, Gradle- tai ulkoisia library-riippuvuuksia.

### 2026-06-15 14:17 - Osa 56
- Valmis: Session Detail näyttää Pro-käyttäjälle `SessionReportData.dbHistogramBuckets` -datasta piirretyn dB-jakaumakortin Time Series -kortin jälkeen. Histogrammi piirtää 0-130 dB bucketit Canvasilla ja näyttää näkyvät bucket-prosentit kompakteina riveinä.
- Valmis: Free-käyttäjän kortti käyttää lukittua preview-dataa eikä näytä raportin todellisia bucket-arvoja overlayn alla. Lukitun kortin korkeus rajattiin eksplisiittisesti screenshot-tarkastuksessa löytyneen overlay-ylikasvun korjaamiseksi.
- Valmis: Histogrammille lisättiin ruudunlukijan content description tyhjälle, lukitulle ja datalliselle tilalle. `dbHistogramAccessibilitySummary(...)` listaa vain näkyvät bucketit ja sille lisättiin unit-testit.
- Valmis: Lisättiin screenshot-previewt ja baseline-kuvat Pro- ja Free-locked histogrammikortille.
- Seuraava tehtävä: Osa 57 - WAV opt-in preference.
- Seuraava komento: `rg -n "raw audio|WAV|wav|opt-in|record audio|soundDetectionPersistence|DataStore|ProLockOverlay|AudioEngine|SoundDetectionWindowFanout" app/src/main/java app/src/test/java app/src/main/res/values/strings.xml`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.ui.history.detail.SessionDetailScreenActionTest` kaatui odotetusti puuttuvaan `dbHistogramAccessibilitySummary`-symboliin. Screenshot RED: `.\gradlew.bat --no-daemon --console=plain :app:validateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.DbHistogramCard*"` kaatui odotetusti puuttuviin baseline-kuviin. Baseline-päivitys: `.\gradlew.bat --no-daemon --console=plain :app:updateDebugScreenshotTest --tests "com.dbcheck.app.ComponentScreenshotTestsKt.DbHistogramCard*"` meni läpi. Lopputarkistukset: sama unit-testi, sama screenshot-validointi ja `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` menivät läpi.
- Muutetut tiedostot: `app/src/main/java/com/dbcheck/app/ui/history/detail/SessionDetailScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/DbHistogramCardLockedDarkPreview_4b098843_0.png`, `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/DbHistogramCardPreview_74131fac_0.png`, `app/src/test/java/com/dbcheck/app/ui/history/detail/SessionDetailScreenActionTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android Compose Canvas- ja semantics-ohjeet ennen UI-toteutusta.

### 2026-06-15 14:37 - Osa 57
- Valmis: Lisättiin Pro-gatettu Settings default OFF -asetus `wav_recording_default` / `wavRecordingDefaultEnabled`. DataStore mapping, defaults, repository ja Settings UI-state kulkevat `PreferencesRepository`n kautta.
- Valmis: Data & Export -osioon lisättiin ProLockOverlayn takana `WAV recording default` -toggle ja privacy-warning, joka kertoo että raw microphone audio voi sisältää speech/identifiable sounds. Free-käyttäjä näkee effective OFF -tilan eikä voi enabloida arvoa ViewModelin kautta.
- Valmis: Tämä osa ei lisää WAV writeria, raw-audio fanoutia, tiedostokirjoitusta tai export/delete UI:ta. Seuraavan writer-vaiheen pitää edellyttää sekä Pro-oikeutta että `wavRecordingDefaultEnabled`-opt-iniä ennen raakaaudion persistointia.
- Seuraava tehtävä: Osa 58 - WAV writer.
- Seuraava komento: `rg -n "wavRecordingDefaultEnabled|wav_recording_default|AudioEngine|raw audio|PCM|WAV|FileOutputStream|RandomAccessFile|ExportFileCache" app/src/main/java app/src/test/java app/src/main/res/values/strings.xml`
- Ajetut testit: RED: `.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest --tests com.dbcheck.app.data.local.preferences.UserPreferencesDataStoreMappingTest --tests com.dbcheck.app.data.repository.PreferencesRepositoryTest --tests com.dbcheck.app.ui.settings.SettingsViewModelDisplayPreferenceTest --tests com.dbcheck.app.ui.settings.components.NoiseNotificationsSectionCopyTest` kaatui odotetusti puuttuviin `WAV_RECORDING_DEFAULT_ENABLED`-, `wavRecordingDefaultEnabled`- ja `updateWavRecordingDefaultEnabled`-symboleihin. GREEN: sama komento meni läpi. Lopputarkistus: `.\gradlew.bat --no-daemon --console=plain :app:compileReleaseKotlin` meni läpi.
- Muutetut tiedostot: `AGENTS.md`, `PROJECT.md`, `memory/MEMORY.md`, `app/src/main/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStore.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferenceDefaults.kt`, `app/src/main/java/com/dbcheck/app/data/local/preferences/model/UserPreferences.kt`, `app/src/main/java/com/dbcheck/app/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/SettingsScreen.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/SettingsViewModel.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/components/DataExportSection.kt`, `app/src/main/java/com/dbcheck/app/ui/settings/state/SettingsUiState.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/dbcheck/app/data/local/preferences/UserPreferencesDataStoreMappingTest.kt`, `app/src/test/java/com/dbcheck/app/data/repository/PreferencesRepositoryTest.kt`, `app/src/test/java/com/dbcheck/app/ui/settings/SettingsViewModelDisplayPreferenceTest.kt`, `app/src/test/java/com/dbcheck/app/ui/settings/components/NoiseNotificationsSectionCopyTest.kt`, `dBcheck Missing Features -toteutussuunnitelma.md`, `NEXT.md`.
- Huomiot käyttäjälle: `lc`/`sc` ei ajettu ohjeen mukaisesti. Tarkistin viralliset Android DataStore-, Compose Switch- ja Compose accessibility/semantics -ohjeet ennen toteutusta.
