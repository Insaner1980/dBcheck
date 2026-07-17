Käytännössä: tee dBcheckille oma privacy-policy ennen Play Console -julkaisua. Yhteinen nettisivu on täysin ok, mutta Play-linkin pitää mennä suoraan dBcheckin omaan policyyn, ei yleiselle etusivulle eikä toisen sovelluksen policyyn.

Tarkka lista:

1. Lisää sivustolle uusi sivu, esim. `https://sinunsivu.fi/dbcheck/privacy`
   - Otsikko selvästi: `dBcheck Privacy Policy`
   - Ei PDF, ei kirjautumista, ei geo-blokkausta, ei muokattavaa dokumenttia.
   - Sama kehittäjänimi kuin Play Console -tilillä.
   - Sähköposti tai muu privacy-yhteydenottotapa.
   - Google Play vaatii privacy-policy-linkin sekä Play Consolessa että sovelluksen sisällä. Lähde: [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311).

2. Kirjoita policy nimenomaan dBcheckin nykyisistä datavirroista:
   - mikrofoni: ääntä käytetään melutason laskentaan, käyttäjän käynnistämään passive monitoring -sampleen ja opt-in sound detection -luokitukseen
   - raaka mikrofoniääni käsitellään normaalisti vain muistissa eikä sitä tallenneta; Pro-käyttäjän erikseen käyttöön ottama, oletuksena pois päältä oleva WAV-asetus tallentaa tulevien mittaussessioiden PCM16-raakaaudion app-private `filesDir/wav_recordings` -hakemistoon, josta käyttäjä voi jakaa tai poistaa tallenteen
   - mittaushistoria/sessiot tallennetaan laitteelle
   - hearing test: full hearing test -tulokset tallennetaan paikalliseen Room-tietokantaan, mukaan lukien korvakohtaiset threshold-tiedot, score, rating ja johdetut yhteenvetoarvot; tuloksia ei kirjoiteta Health Connectiin
   - hearing recovery: lyhyt recovery check tallentaa paikalliseen Room-tietokantaan full hearing test -baselineen viittaavat korvakohtaiset threshold-deltat ja aggregate-yhteenvedot, ei uutta raakaaudiota eikä Health Connect -tietoa
   - passive monitoring: käyttäjän käynnistämä foreground-sample käyttää mikrofonia vain samplen ajan ja tallentaa paikalliseen Room-tietokantaan vain aikavälin, reading countin sekä min/average/max/peak- ja total energy -aggregaatit; raakaaudiota tai yksittäisiä mittausrivejä ei tallenneta
   - sound detection: live-luokitus käyttää mikrofonista johdettuja YAMNet-windoweja, raakaaudiota ei tallenneta, ja detection-eventtien pysyvä tallennus on erillinen opt-in; tallennettava tieto on vain session id, aikaleima, luokan label ja confidence
   - CSV/PDF/PNG-exportit ja jako tapahtuvat käyttäjän omasta toiminnosta
   - CSV-exporttiin sisältyvät myös opt-inillä tallennetut aggregoidut sound detection -eventit; delete semantics on sama kuin paikallisella mittaushistorialla eli session poistaminen / sovellusdatan poisto / uninstall poistaa myös siihen liittyvät detection-eventit
   - Health Connect: melusessioiden kirjoitus ja sykkeen luku vain käyttäjän luvalla
   - session location: käyttäjä myöntää Settingsissä optional approximate location -luvan; luvan ollessa voimassa dBcheck yrittää liittää viimeksi tunnetun coarse location -sijainnin automaattisesti jokaiseen uuteen mittaussessioon session alussa ja tarvittaessa uudelleen sen valmistuessa; sijaintia ei välttämättä tallennu, jos viimeksi tunnettua sijaintia ei ole saatavilla; ei precise locationia, background locationia, aktiivista sijaintipäivitystä tai jatkuvaa seurantaa
   - Play Billing: ostot käsittelee Google Play; dBcheck kysyy ostotilan Google Playlta ja tallentaa laitteelle vain effective Pro-entitlementin boolean-tilan
   - varmuuskopiot: käyttäjän luomat paikalliset backupit kopioivat Room-tietokannan app-private `filesDir/backups` -hakemistoon; ne eivät sisällä DataStore-asetuksia tai WAV-tiedostoja eikä niitä lähetetä pilveen
   - datan poisto: käyttäjä voi poistaa sovellusdatan / uninstalloida; lisää appiin myöhemmin selkeämpi delete/export-flow jos haluat

3. Lisää appiin toimiva Privacy-linkki Settingsiin.
   Nykyinen footer näyttää `Privacy · Terms`, mutta katselmuksessa sitä ei löytynyt klikattavana linkkinä. Korjaus voi olla pieni: Settingsissä `Privacy` avaa saman dBcheck-policy-URL:n selaimeen tai omaan in-app privacy-näkymään.

4. Päivitä Health Connect -rationale vastaamaan samaa policyä.
   Health Connectin dokumentaatio sanoo, että manifestin rationale-activity näyttää appin privacy policy / permission rationale -sisällön, ja Play Console Health Connect -julkaisussa policy pitää olla sama, jonka käyttäjä näkee Health Connectin privacy policy -linkistä. Lähde: [Health Connect get started](https://developer.android.com/health-and-fitness/health-connect/get-started) ja [Declare Health Connect access](https://developer.android.com/health-and-fitness/health-connect/declare-access).

5. Kun julkaisu lähestyy, täytä Play Consolessa:
   - Privacy policy URL: dBcheckin oma policy-sivu
   - Data Safety: täsmää policyyn, älä lupaa “no data” jos Health Connect / billing / exportit käsitellään tavalla joka pitää ilmoittaa
   - Session location -Data Safety -muistiinpano nykyisen scope-lukituksen perusteella:
     - Data type: Location -> Approximate location.
     - Required/optional: optional, koska käyttäjä voi käyttää mittausta ilman sijaintia.
     - Purpose: App functionality, eli mittaussession paikallinen konteksti/haku/raportointi.
     - Shared: no, ellei käyttäjä itse vie tai jaa raporttia/exporttia.
     - Collection wording: jos sijainti pysyy vain laitteen paikallisessa tietokannassa eikä lähde kehittäjän palvelimelle, älä väitä palvelinpuolen keräystä; Play-lomakkeen lopullinen vastaus pitää tarkistaa siinä vaiheessa, kun export/cloud-sync/share-polut on lukittu.
     - Ei Precise locationia, ei Background locationia, ei advertising/analytics-purposea.
   - Health apps / Health Connect declaration
   - Foreground service declaration, koska app käyttää microphone foreground serviceä; Play vaatii Android 14+ FGS-tyypeille Console-ilmoituksen ja yleensä perustelun/demo-videon. Lähde: [Foreground service requirements](https://support.google.com/googleplay/android-developer/answer/13392821).

6. Tuleva Session location -käyttäjäcopy:
   - Lyhyt UI-copy: `Allow approximate location for measurement sessions`
   - Permission rationale: `If you allow approximate location, dBcheck will try to attach the device's last known approximate location to each new measurement session. Precise and background location are not used.`
   - Settings/privacy copy: `Session location is optional. While approximate location permission is granted, dBcheck automatically tries to attach the device's last known approximate foreground location to each new measurement session. A session may have no location if none is available. The app does not request precise location, background location, active location updates, or continuous tracking.`

Tärkein seuraava konkreettinen työ on siis: tee dBcheckille oma privacy-policy-sivu ja lisää sovelluksen Settingsiin klikattava Privacy-linkki siihen. Koska appi ei ole vielä julkaisuvalmis, Terms-sivua ei tarvitse ratkaista samalla ellei footerissa haluta pitää `Terms`-tekstiä näkyvissä; muuten poistaisin tai tekisin senkin oikeaksi ennen julkaisua.
