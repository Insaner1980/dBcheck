# Pro-oston UI-kytkennän toteutussuunnitelma

## Yhteenveto

Kytketään Settingsin Pro-upsell ja Settingsissä näkyvien ProLockOverlayjen Upgrade-napit käynnistämään Google Play Billing -ostovirta tuotteelle `dbcheck_pro`. Ulkopuolisista näytöistä, kuten Analytics ja Session Detail, Upgrade vie edelleen Settingsin Pro-korttiin. Lisätään debug-only Free override, jotta ostovirta ja Pro-gatet voidaan testata debug-buildissä ilman että nykyinen debug-Pro-oletus rikotaan.

Referenssit: [Google Play Billing integration](https://developer.android.com/google/play/billing/integrate.html), [BillingClient.launchBillingFlow](https://developer.android.com/reference/com/android/billingclient/api/BillingClient#launchBillingFlow(android.app.Activity,com.android.billingclient.api.BillingFlowParams)).

## Keskeiset muutokset

- Laajenna billing-rajapintaa niin, että `BillingManager.launchPurchaseFlow(activity)` palauttaa selkeän tuloksen:
  `Started`, `AlreadyOwned`, `Unavailable`, `Failed`.
- Lisää `BillingManager`iin purchase-event stream:
  `Completed`, `Cancelled`, `AlreadyOwned`, `Failed`.
- `SettingsViewModel` injektoi BillingManagerin, käynnistää ostovirran, ylläpitää loading/error/message-tilaa ja kuuntelee purchase-eventtejä.
- `SettingsUiState` saa kentät:
  `isPurchaseLaunching`, `purchaseMessage`, `purchaseErrorMessage`, `debugForceFreeEnabled`.
- `SettingsScreen` hakee Activityn `LocalContext`-ketjusta ja välittää saman `onStartProPurchase`-toiminnon:
  `AudioCalibrationSection`, `LockscreenMeterSection`, `HealthSyncSection` ja `ProUpsellCard`.
- `ProUpsellCard` näyttää disabled/loading-tilan ostovirran avauksen aikana sekä onnistumis- tai virheviestin.
- Lisää debug-only toggle Pro-korttiin: “Debug: Force Free mode”.
  Toggle tallennetaan DataStoreen, mutta sitä käytetään vain `BuildConfig.DEBUG`-buildissä.
- Korjaa Pro-entitlementin lähde niin, että debugissä effective Pro on:
  `true`, ellei debug force free ole päällä; releasessa vain ostettu Pro avaa ominaisuudet.
- Varmista, että `ProFeatureManager` alustuu sovelluksen käynnistyksessä, esimerkiksi injektoimalla se `DbCheckApplication`iin, jotta billing -> preferences -synkkaus ei riipu foreground servicestä.

## Toteutuspolku

1. Lisää billing-domainiin pienet sealed-mallit ostovirran launch-tulokselle ja purchase-eventeille.
2. Päivitä `BillingManager`:
   - kysy `ProductDetails` juuri ennen launchia, ei cachea
   - palauta launch-tulos `BillingResult.responseCode`n mukaan
   - käsittele `ITEM_ALREADY_OWNED` asettamalla Pro ostetuksi
   - emittoi eventit `onPurchasesUpdated`-polusta
   - acknowledge ostot vasta kun `PurchaseState.PURCHASED`
3. Lisää debug force free DataStoreen:
   - uusi preference-avain
   - uusi `UserPreferences.debugForceFreeEnabled`
   - uusi repository update -funktio
   - pure policy-funktio effective Pro -laskentaan, jotta se voidaan unit-testata ilman Androidia
4. Päivitä Settings:
   - ViewModeliin `launchProPurchase(activity)`, `clearPurchaseMessages()` ja `updateDebugForceFree(enabled)`
   - Composableen `findActivity()` helper
   - korvaa Settingsin sisäiset `onNavigateToUpgrade`-callbackit ostovirran käynnistyksellä
   - jätä NavHostin ulkopuolinen Upgrade-käytös ennalleen: muilta näytöiltä navigoidaan Settingsin Pro-korttiin
5. Päivitä dokumentaatio:
   - `PROJECT.md`: Pro-oston UI-kytkentä ei ole enää puute
   - `memory/MEMORY.md` ja `AGENTS.md`: lyhyt arkkitehtuurimuisti entitlement-flowsta ja debug force free -kytkimestä

## Testisuunnitelma

- Unit-testaa effective Pro -policy:
  - release + ei ostoa -> false
  - release + osto -> true
  - debug + ei force free -> true
  - debug + force free -> false
  - debug + ostettu + force free -> false
- Unit-testaa `SettingsViewModel` fake BillingManagerilla:
  - launch `Started` -> loading poistuu ilman erroria
  - launch `Unavailable` -> error näkyy
  - purchase `Completed` -> success message ja loading false
  - purchase `Cancelled` -> loading false, ei pysyvää virhettä
- Aja:
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat assembleDebug`
- Manuaalinen debug-tarkistus:
  - avaa Settings
  - laita “Debug: Force Free mode” päälle
  - varmista että Pro-kortti ja ProLockOverlayt näkyvät
  - paina Upgrade ja varmista, että ostovirran launch-yritys antaa joko Play Billing -dialogin tai ymmärrettävän “not available” -virheen testilaitteesta riippuen
  - ota debug force free pois ja varmista, että Pro-gatet poistuvat debugissä

## Oletukset

- Tuote-ID pysyy nykyisenä: `dbcheck_pro`.
- Ei lisätä backend-verifikaatiota tässä vaiheessa; toteutus pysyy nykyisen client-only Billing-arkkitehtuurin tasolla.
- Ulkopuolisten ProLockOverlayjen Upgrade-painike ei käynnistä ostoa suoraan, vaan vie Settingsin Pro-korttiin. Ostovirta alkaa vain Settingsissä.
- Debug force free on vain kehittäjätestausta varten eikä vaikuta release-buildiin.
