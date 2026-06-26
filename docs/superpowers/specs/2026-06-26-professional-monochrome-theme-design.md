# dBcheckin ammattimainen monokrominen ilme

## Tavoite

Tee dBcheckista ammattimaisemman näköinen korvaamalla nykyinen neon-lime brändi-ilme hillityllä mustan, valkoisen ja harmaan järjestelmällä. Dark theme seuraa hyväksyttyä B-suuntaa. Light theme seuraa puhdasta ja kliinistä C-suuntaa.

Tämä on teema- ja resurssitason visuaalinen päivitys. Tämä ei ole layout-uudistus, navigaatiomuutos tai ominaisuuden muutos.

## Hyväksytty suunta

- Dark theme: musta tausta, valkoiset päätoiminnot, harmaat pinnat, harmaat kaaviot, ei neon-brändiaksentteja.
- Light theme: valkoinen ja pehmeän harmaa tausta, mustat päätoiminnot, harmaat kortit ja rajat.
- Statusvärit voivat jäädä vain riski-, varoitus-, virhe- ja onnistumistiloihin, ja niiden pitää olla hillittyjä eikä neonmaisia.
- Sovellusikonin pitää sopia samaan ammattimaiseen linjaan eikä käyttää neon-limejä waveform-palkkeja.

## Rajaus

Mukana:

- Korvataan neon-lime ja keltavihreät arvot keskitetystä Compose-teemapaletista.
- Muutetaan `signatureGradient` niin, ettei päätoiminnoista synny neon-gradienttia.
- Päivitetään launcher-ikonin vector-värit musta-valko-harmaaseen linjaan.
- Tarkistetaan notificationien XML-värit ja pidetään ne linjassa siellä missä se on järkevää.
- Tarkistetaan user-facing share/export-polkujen kovakoodatut vanhat lime-värit ja siirretään ne uuteen palettiin, jos ne näkyvät käyttäjälle.
- Päivitetään muutoksesta rikkoutuvat screenshot-referenssit.
- Lisätään kohdennettu varmistus, ettei vanhoja neon-brändivärejä jää teema- tai launcher-resursseihin.

Ei mukana:

- Meterin, Analyticsin, Historyn, Settingsin, Cameran, Sleepin tai Hearing Testin layoutien uudelleenrakennus.
- Feature-käytöksen, navigaation, billingin, lupien, datamallin, exporttien tai analytiikkalaskennan muuttaminen.
- Kaikkien semanttisten statusvärien poistaminen varoitus-, virhe- tai onnistumistiloista.
- Typografian tai sovellustekstien vaihtaminen, ellei väririvin nimi vaadi resurssinimeen selvennystä.

## Design system

Nykyinen `ui/theme`-paketti pysyy ilmeen lähteenä. Toteutuksen pitää ensisijaisesti muuttaa keskitettyjä tokeneita yksittäisten näyttöjen paikkaamisen sijaan.

Dark palette:

- Tausta pysyy lähes mustana.
- Pinnat käyttävät hallittua harmaaskaalaa mustasta tummaan hiileen.
- `primary` muuttuu valkoiseksi tai lähes valkoiseksi.
- `primaryContainer` muuttuu tummaksi tai keskiharmaaksi selected-state-pinnaksi.
- `onPrimary` ja `onPrimaryContainer` valitaan kontrastin mukaan mustaksi tai valkoiseksi.
- `secondary` muuttuu neutraaliksi harmaaksi eikä toiseksi kirkkaaksi aksentiksi.
- `success`, `warning` ja `error` muuttuvat hillityiksi semanttisiksi väreiksi.

Light palette:

- Tausta on puhdas valkoinen tai lähes valkoinen.
- Pinnat käyttävät pehmeitä neutraaleja harmaita.
- `primary` muuttuu mustaksi tai lähes mustaksi.
- `primaryContainer` muuttuu vaaleaksi harmaaksi.
- `secondary` ja `tertiary` pysyvät neutraaleina.
- `success`, `warning` ja `error` pysyvät hillittyinä ja saavutettavina.

Gradientit:

- Nykyiset gradienttihelperit voivat jäädä yhteensopivuuden takia, mutta niiden käyttämien värien pitää olla niin neutraaleja, että painikkeet ja kaaviot näyttävät ammattimaisilta eivätkä neonilta.
- Jos valkoinen-harmaa-gradientti tekee dark theme -kontrastista heikon, käytetään saman sävyperheen neutraaleja arvoja uuden aksenttivärin sijaan.

## Sovellusikoni

Launcher-ikoni säilyttää nykyisen waveform-konseptin, koska se on tunnistettava ja on jo kytketty adaptive icon -resursseihin.

Pakolliset muutokset:

- Tausta pysyy mustana tai lähes mustana.
- Waveform-palkit muutetaan neon limestä ja keltavihreästä valkoiseksi ja neutraaliksi harmaaksi.
- Androidin themed icons -yhteensopiva monochrome adaptive icon -polku pidetään toimivana.
- Uutta bitmap-ikoniputkea ei lisätä, ellei vector-resurssi osoittaudu riittämättömäksi.

## Käyttäjälle näkyvät pinnat

Odotettu näkyvä muutos:

- Meterin valittu tab, gauge-aksentti, sliderit, bottom nav -valinta ja primary-painikkeet käyttävät valkoista tai harmaata neon limen sijaan.
- Analyticsin kaaviot ja distribution barit siirtyvät lime/keltavihreästä harmaaskaalaan, ellei segmentti ole aidosti semanttinen riskitila.
- History-kortit säilyttävät rakenteen ja spacingin, mutta neon action -teksti poistuu.
- Settingsin togglet, calibration slider, valittu weighting-chip ja section-aksentit muuttuvat monokromisiksi.
- Light theme muuttuu puhtaaksi professional-vaihtoehdoksi eikä ole vanhan brändin vaaleanvihreä versio.

## Testaus ja varmistus

Minimivarmistus:

- Ajetaan kapein hyödyllinen Gradle compile -tarkistus tokeni- ja resurssimuutosten jälkeen.
- Ajetaan screenshot-testien päivitys ja validointi muuttuville component previewille.
- Lisätään tai päivitetään kohdennettu testi tai resource scan, joka epäonnistuu jos vanhoja neon-brändivärejä jää keskitettyyn teemaan tai launcher-resursseihin.
- Ajetaan `git diff --check`.

`lc`- tai `sc`-wrappereita ei ajeta ilman käyttäjän erillistä pyyntöä.

## Riskit

- Screenshot-referenssejä muuttuu laajasti, koska keskitetyt värit vaikuttavat moniin previewihin.
- Osa kaavioista nojaa värieroihin merkityksen välittämisessä. Harmaaskaalaan siirtyessä vierekkäisten segmenttien kontrasti pitää säilyttää.
- Statusvärejä ei saa litistää niin pitkälle, että varoituksia tai virheitä on vaikea tunnistaa.
- Työpuu on jo valmiiksi likainen muusta ominaisuustyöstä. Stageen ja commitiin otetaan vain hyväksytyt suunnitteluartefaktit, ellei käyttäjä erikseen hyväksy toteutusta.
