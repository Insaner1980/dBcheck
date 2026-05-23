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
   - CSV/PDF/PNG-exportit ja jako tapahtuvat käyttäjän omasta toiminnosta
   - Health Connect: melusessioiden kirjoitus ja sykkeen luku vain käyttäjän luvalla
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
   - Health apps / Health Connect declaration
   - Foreground service declaration, koska app käyttää microphone foreground serviceä; Play vaatii Android 14+ FGS-tyypeille Console-ilmoituksen ja yleensä perustelun/demo-videon. Lähde: [Foreground service requirements](https://support.google.com/googleplay/android-developer/answer/13392821).

Tärkein seuraava konkreettinen työ on siis: tee dBcheckille oma privacy-policy-sivu ja lisää sovelluksen Settingsiin klikattava Privacy-linkki siihen. Koska appi ei ole vielä julkaisuvalmis, Terms-sivua ei tarvitse ratkaista samalla ellei footerissa haluta pitää `Terms`-tekstiä näkyvissä; muuten poistaisin tai tekisin senkin oikeaksi ennen julkaisua.