Käytännössä: tee dBcheckille oma privacy-policy ennen Play Console -julkaisua. Yhteinen nettisivu on täysin ok, mutta Play-linkin pitää mennä suoraan dBcheckin omaan policyyn, ei yleiselle etusivulle eikä toisen sovelluksen policyyn.

Tarkka lista:

1. Lisää sivustolle uusi sivu, esim. `https://sinunsivu.fi/dbcheck/privacy`
   - Otsikko selvästi: `dBcheck Privacy Policy`
   - Ei PDF, ei kirjautumista, ei geo-blokkausta, ei muokattavaa dokumenttia.
   - Sama kehittäjänimi kuin Play Console -tilillä.
   - Sähköposti tai muu privacy-yhteydenottotapa.
   - Google Play vaatii privacy-policy-linkin sekä Play Consolessa että sovelluksen sisällä. Lähde: [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311).

2. Kirjoita policy nimenomaan dBcheckin nykyisistä datavirroista:
   - mikrofoni: ääntä käytetään melutason laskentaan
   - kerro tallennetaanko raakaa audiota; nykyisen katselmuksen perusteella älä väitä raakaaudiota tallennettavan, jos näin ei tehdä
   - mittaushistoria/sessiot tallennetaan laitteelle
   - sound detection: live-luokitus käyttää mikrofonista johdettuja YAMNet-windoweja, raakaaudiota ei tallenneta, ja detection-eventtien pysyvä tallennus on erillinen opt-in; tallennettava tieto on vain session id, aikaleima, luokan label ja confidence
   - CSV/PDF/PNG-exportit ja jako tapahtuvat käyttäjän omasta toiminnosta
   - CSV-exporttiin sisältyvät myös opt-inillä tallennetut aggregoidut sound detection -eventit; delete semantics on sama kuin paikallisella mittaushistorialla eli session poistaminen / sovellusdatan poisto / uninstall poistaa myös siihen liittyvät detection-eventit
   - Health Connect: melusessioiden kirjoitus ja sykkeen luku vain käyttäjän luvalla
   - session location: optional approximate location session metadataa varten vain käyttäjän luvalla; ei precise locationia, ei background locationia, ei jatkuvaa seurantaa
   - Play Billing: ostot käsittelee Google Play
   - varmuuskopiot: paikalliset backupit laitteen sovellusdataan
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
   - Lyhyt UI-copy: `Add approximate location to this session`
   - Permission rationale: `dBcheck can save an approximate location with this measurement so you can recognize where the session happened. Precise and background location are not used.`
   - Settings/privacy copy: `Session location is optional. dBcheck uses approximate foreground location only when you choose to add it to a measurement session. The app does not request precise location, background location, or continuous tracking.`

Tärkein seuraava konkreettinen työ on siis: tee dBcheckille oma privacy-policy-sivu ja lisää sovelluksen Settingsiin klikattava Privacy-linkki siihen. Koska appi ei ole vielä julkaisuvalmis, Terms-sivua ei tarvitse ratkaista samalla ellei footerissa haluta pitää `Terms`-tekstiä näkyvissä; muuten poistaisin tai tekisin senkin oikeaksi ennen julkaisua.
