# dBcheck Missing Features - pilkottu toteutussuunnitelma

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Toteuttaa `dbcheck_missing_features_audit.md`-auditin puuttuvat dBcheck-ominaisuudet pieninä, keskeytystä kestävinä osina.

**Architecture:** Nykyinen MVVM + Hilt + Room + DataStore + Compose Navigation pidetään. Mittauslogiikka keskitetään `domain/audio`- ja `service/AudioSessionManager`-polkuihin; UI lukee valmiita state-malleja eikä tee mittauslaskentaa. Jokainen Pro-ominaisuus gateataan UI:n lisäksi execution/data-polussa.

**Tech Stack:** Kotlin 2.3.20, AGP 9.1.0, Compose BOM 2026.03.00, Room 2.8.4, DataStore 1.2.1, Hilt 2.59.2, Health Connect 1.1.0, CameraX/LiteRT lisätään vasta niiden omissa osissa virallisista lähteistä tarkistetulla versiolla.

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
- [ ] Lisää MeterUiStateen equivalent-level live-arvo ja label.
- [ ] Näytä Pro-käyttäjälle LAeq/LCeq/LZeq label oikein.
- [ ] Free saa lukitun previewn tai nykyisen yksinkertaisen stat-rivin.
- [ ] Testaa, että stopin jälkeen Session Detail arvo vastaa toleranssilla.
- [ ] Hyväksyntä: active session equivalent level näkyy.

### Osa 20 - Dosimeter UI data mapping
- [ ] Mapita `LiveExposureState` `DosimeterUiState`ksi.
- [ ] Lisää unavailable-tila vanhalle datalle tai puuttuvalle A-dataan perustuvalle laskennalle.
- [ ] Testaa Pro/Free/unavailable.
- [ ] Hyväksyntä: UI saa valmiin mallin ilman laskentaa.

### Osa 21 - Dosimeter gauge UI
- [ ] Lisää dose percentage gauge.
- [ ] Näytä TWA, LAeq, dose %, projected dose ja remaining time.
- [ ] Näytä standard label ja reference info.
- [ ] Screenshot-testit low/near-limit/over-limit.
- [ ] Hyväksyntä: Dosimeter mode on käytettävä Pro-näkymä.

### Osa 22 - Meter session info bar
- [ ] Lisää REC, duration, effective weighting, response time.
- [ ] Näytä Prolle sample rate ja input device.
- [ ] Testaa timer taustalta paluun jälkeen.
- [ ] Hyväksyntä: aktiivisen session metatiedot näkyvät yhtenäisesti.

## D. Analytics ja spectral

### Osa 23 - AnalyticsSection state
- [ ] Lisää `AnalyticsSection.OVERVIEW`, `SPECTRAL`, `ENVIRONMENT`.
- [ ] Lisää ViewModel/UI state chip-valinnalle.
- [ ] Testaa selection persist recompositionin yli.
- [ ] Hyväksyntä: section-state toimii ilman korttien siirtoa.

### Osa 24 - Analytics chip row UI
- [ ] Lisää chip row Analyticsin headerin alle.
- [ ] Free-käyttäjä näkee Pro-sectionit lukittuina, ei piilotettuina.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: navigointi sectioneihin toimii.

### Osa 25 - Analytics card regroup
- [ ] Siirrä weekly/monthly/yearly/hearing health Overviewiin.
- [ ] Siirrä spectral card Spectral-sectioniin.
- [ ] Siirrä Environment Mix Environment-sectioniin.
- [ ] Testaa, ettei nykyinen dataflow muutu.
- [ ] Hyväksyntä: näkymä vastaa auditin rakennetta.

### Osa 26 - Weekly/Monthly toggle
- [ ] Lisää Overviewiin Weekly/Monthly-toggle.
- [ ] Weekly on Free, Monthly Pro.
- [ ] Käytä nykyistä `ExposureAnalyticsCalculator`ia.
- [ ] Testaa Free/Pro/empty.
- [ ] Hyväksyntä: monthly ei laske tai näytä oikeaa Pro-dataa Free-tilassa.

### Osa 27 - SpectralMode Bars
- [ ] Tee nykyisestä spectral cardista `SpectralMode.BARS`.
- [ ] Lisää mode state ja UI toggle.
- [ ] Testaa nykyisen bars-käyttäytymisen säilyminen.
- [ ] Hyväksyntä: ei regressiota nykyiseen spektriin.

### Osa 28 - Spectrogram buffer
- [ ] Lisää waterfall ring buffer spectral frameistä.
- [ ] Rajaa pituus ajalla tai rivimäärällä.
- [ ] Unit-testit trimmauslogiikalle.
- [ ] Hyväksyntä: spectrogram data valmis UI:lle.

### Osa 29 - Spectrogram UI
- [ ] Lisää Canvas-waterfall Spectral-sectioniin.
- [ ] Lisää idle/locked/live accessibility text.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: Pro käyttäjä näkee live spectrogramin.

### Osa 30 - Octave-band RTA domain
- [ ] Lisää octave/third-octave band calculator nykyisen FFT:n päälle.
- [ ] Testaa band edge, center frequency ja normalized amplitude.
- [ ] Hyväksyntä: RTA-data on laskettu domainissa.

### Osa 31 - RTA UI
- [ ] Lisää RTA mode Spectral-sectioniin.
- [ ] Näytä octave bars ja stat pills.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: Bars/Spectrogram/RTA toggle toimii.

### Osa 32 - Spectral stat pills
- [ ] Lisää dominant frequency, bandwidth, peak band, live/idle.
- [ ] Käytä yhtä formatteria Hz/kHz-arvoille.
- [ ] Testaa formatterit ja UI-state.
- [ ] Hyväksyntä: statsit eivät ole kovakoodattua copya.

### Osa 33 - Real-time Environment Mix accumulator
- [ ] Lisää aktiivisen session category distribution helper.
- [ ] Käytä `NoiseLevel`-rajoja yhdestä lähteestä.
- [ ] Unit-testit prosenttipyöristykselle.
- [ ] Hyväksyntä: realtime mix data on valmis UI:lle.

### Osa 34 - Real-time Environment Mix UI
- [ ] Lisää Environment-sectioniin active-session mix.
- [ ] Säilytä 7 päivän historiallinen mix erillisenä.
- [ ] Testaa recording/non-recording/Free.
- [ ] Hyväksyntä: realtime ja historical mix eivät sekoitu.

## E. YAMNet ja sound detection

### Osa 35 - Dependency research ja lukitus
- [ ] Tarkista virallisista lähteistä LiteRT/TFLite Android audio classification ja YAMNet asset -jakelutapa.
- [ ] Lisää version catalog -merkinnät vain tarkistetuilla versioilla.
- [ ] Päivitä dependency lock/verification metadata tarvittaessa.
- [ ] Hyväksyntä: build riippuvuuksilla onnistuu ilman classifier-koodia.

### Osa 36 - Model asset ja labels
- [ ] Lisää YAMNet model asset ja label-tiedosto.
- [ ] Lisää license/attribution dokumenttiin.
- [ ] Testaa assetin löytyminen JVM/Robolectric-polulla, jos mahdollista.
- [ ] Hyväksyntä: assetit paketoituvat release buildiin.

### Osa 37 - Audio window adapter
- [ ] Lisää raw PCM -> 16 kHz mono float window -adapteri.
- [ ] Älä tallenna raakaaudiota.
- [ ] Unit-testit normalisoinnille ja window pituudelle.
- [ ] Hyväksyntä: classifier saa oikean inputin.

### Osa 38 - SoundClassifier-portti
- [ ] Lisää testattava interface ja tuotantototeutus.
- [ ] Lisää fake classifier unit/ViewModel-testeihin.
- [ ] Testaa confidence threshold ja empty output.
- [ ] Hyväksyntä: inference on erillinen AudioEnginestä.

### Osa 39 - Sound detection live state
- [ ] Kytke classifier raw-audio fanoutiin Pro + toggle -ehdolla.
- [ ] Emittoi current type, confidence ja recent detections.
- [ ] Testaa Free, Pro off, Pro on.
- [ ] Hyväksyntä: sound detection ei aja inferenceä Free-tilassa.

### Osa 40 - Environment UI sound detection
- [ ] Lisää Environment-sectioniin sound type card.
- [ ] Lisää locked/idle/live/error states.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: live detection näkyy ilman raw audion tallennusta.

### Osa 41 - Optional detection persistence
- [ ] Lisää Room table vain aggregoiduille detection-eventeille.
- [ ] Lisää DataStore opt-in tallennukselle.
- [ ] Lisää delete/export semantics privacy policyyn.
- [ ] Migration-testit.
- [ ] Hyväksyntä: persistence on opt-in ja poistettavissa.

## F. Camera Overlay

### Osa 42 - CameraX dependency research
- [ ] Tarkista CameraX virallinen release ja artifacts.
- [ ] Lisää version catalog ja Gradle-dependencies.
- [ ] Päivitä dependency verification/lockfile.
- [ ] Hyväksyntä: build menee läpi ilman UI:ta.

### Osa 43 - CAMERA permission
- [ ] Lisää manifestiin `CAMERA`.
- [ ] Lisää permission policy/helper runtime requestille.
- [ ] Testaa granted/denied/permanent denied.
- [ ] Hyväksyntä: permission ei koske Meter-starttia.

### Osa 44 - Camera route
- [ ] Lisää `Screen.CameraOverlay`.
- [ ] Lisää NavHost route ja piilota bottom nav kyseisellä routella.
- [ ] Lisää Meteristä Pro-gatettu entry.
- [ ] Hyväksyntä: route avautuu ja palaa turvallisesti.

### Osa 45 - Camera preview shell
- [ ] Lisää fullscreen preview UI fake/static previewllä testattavaksi.
- [ ] Lisää close button, overlay slots ja permission denial UI.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: UI runko valmis ennen CameraX sessionia.

### Osa 46 - CameraX preview binding
- [ ] Kytke lifecycle camera preview.
- [ ] Käsittele camera unavailable.
- [ ] Manual device smoke.
- [ ] Hyväksyntä: preview näkyy laitteella.

### Osa 47 - Live dB overlay
- [ ] Lue current dB AudioEngine/AudioSessionManager statesta.
- [ ] Näytä dB, label ja timestamp overlaynä.
- [ ] Testaa ilman aktiivista mittausta ja aktiivisen mittauksen aikana.
- [ ] Hyväksyntä: overlay toimii ilman measurement-session rikkomista.

### Osa 48 - Photo capture burned-in overlay
- [ ] Tallenna kuva MediaStoreen tai export cacheen.
- [ ] Piirrä overlay bitmapiin.
- [ ] Jaa `content://`-URIlla.
- [ ] Manual share test.
- [ ] Hyväksyntä: jaetussa kuvassa overlay on mukana.

### Osa 49 - Silent video capture
- [ ] Lisää CameraX video ilman audio trackia oletuksena.
- [ ] Lisää visual overlay mukaan tallenteeseen tai dokumentoi, jos tekninen polku vaatii erillisen renderöinnin.
- [ ] Lisää privacy copy: ääntä ei tallenneta camera-videoon.
- [ ] Hyväksyntä: video ei lisää uutta raw audio -keräystä.

## G. Storage, History, Report ja Export

### Osa 50 - Session location design
- [ ] Tarkista Android location permission -ohjeet.
- [ ] Lukitse foreground approximate-only.
- [ ] Lisää privacy copy ja Data Safety -muistiinpano.
- [ ] Hyväksyntä: ei koodimuutosta ennen permission-scopea.

### Osa 51 - Session location schema
- [ ] Lisää session tableen optional location fields.
- [ ] Lisää migration ja tests.
- [ ] Hyväksyntä: vanhat sessiot toimivat ilman locationia.

### Osa 52 - One-shot location capture
- [ ] Lisää service/portti location capturelle session start/stop -hetkeen.
- [ ] Käsittele denied/unavailable ilman session failia.
- [ ] Testaa fake location providerilla.
- [ ] Hyväksyntä: location on optional metadata.

### Osa 53 - History search query
- [ ] Lisää DAO query name/tag/date/level/weighting/location -filttereille.
- [ ] Säilytä Free 7 päivän policy.
- [ ] Testaa deterministic ordering.
- [ ] Hyväksyntä: repository palauttaa filtteröidyt sessiot.

### Osa 54 - History search UI
- [ ] Lisää search field ja filter chips Pro-käyttäjälle.
- [ ] Free saa locked previewn tai upgrade affordancen.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: History filteröityy ilman direct-open gate regressiota.

### Osa 55 - Histogram calculator
- [ ] Lisää dB bucket calculator.
- [ ] Testaa tyhjä data, edge bucketit ja prosentit.
- [ ] Hyväksyntä: histogram data valmis Session Detailille.

### Osa 56 - Histogram UI
- [ ] Lisää Session Detailiin Pro histogram card.
- [ ] Lisää accessibility text.
- [ ] Screenshot-testit.
- [ ] Hyväksyntä: käyttäjä näkee dB-jakauman.

### Osa 57 - WAV opt-in preference
- [ ] Lisää per-session opt-in tai Settings default OFF.
- [ ] Lisää privacy warning.
- [ ] Testaa Free/Pro gating.
- [ ] Hyväksyntä: raakaaudiota ei voi tallentaa vahingossa.

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
