# Task 9 - Settings privacy disclosures and tertiary casing

## Tila

Valmis. Muutos on rajattu Settings-esitykseen, jaettuun disclosure-komponenttiin, `DbCheckButton`in tertiary-tekstiesitykseen, resursseihin ja kohdennettuihin testeihin. Preference-, permission-, notification-, foreground-service-, repository-, audio- ja entitlement-polkuja ei muutettu.

## Muutetut tiedostot

- `app/src/main/java/com/dbcheck/app/ui/settings/components/CompactDisclosureInfo.kt`
  - uusi yhteinen kompakti disclosure-rivi, 48 dp teematokeniin sidottu info-painike ja `DbCheckAlertDialog`.
- `app/src/main/java/com/dbcheck/app/ui/settings/components/DataExportSection.kt`
  - WAV-varoitus kompakti OFF-tilassa ja kokonainen inline ON-tilassa.
- `app/src/main/java/com/dbcheck/app/ui/settings/components/LockscreenMeterSection.kt`
  - public readings -varoitus kompakti OFF-tilassa ja kokonainen inline ON-tilassa.
- `app/src/main/java/com/dbcheck/app/ui/settings/components/NoiseNotificationsSection.kt`
  - passive disclosure kompakti inactive-tilassa ja kokonainen inline active-tilassa.
  - Start avaa paikallisen confirmation-dialogin; Confirm välittää nykyiseen start-callbackiin, Cancel vain sulkee dialogin.
- `app/src/main/java/com/dbcheck/app/ui/components/DbCheckButton.kt`
  - tertiary ei enää muuta lokalisoitua tekstiä automaattisesti uppercase-muotoon.
- `app/src/main/res/values/strings.xml`
  - Close-toiminto ja kolme kompaktia disclosure-labelia; täydet alkuperäiset varoitustekstit säilyivät muuttumattomina.
- `app/src/main/res/values-fi/strings.xml`
  - Close-toiminto, kompaktit labelit ja täydet disclosure-tekstit suomeksi.
- `app/src/test/java/com/dbcheck/app/ui/settings/components/SettingsDisclosureContractTest.kt`
  - disclosure-haarat, yhteinen dialogi, 48 dp token, passive confirmation, tertiary-casing sekä schedule/chip/slider/accessibility-suojat.

## RED / GREEN

- RED-komento:
  - `./gradlew :app:testDebugUnitTest --tests com.dbcheck.app.ui.settings.components.SettingsDisclosureContractTest --no-daemon`
  - tulos: 9 testiä, 8 odotettua epäonnistumista puuttuvista disclosure/casing/confirmation-muutoksista; olemassa olevan schedule/chip/slider-sopimuksen testi oli vihreä.
- GREEN-komento: sama kohdennettu komento.
  - ensimmäinen toteutusajo: 8/9 vihreä; yksi liian tarkka mutta behavioriltaan vastaava source-assertio korjattiin ilman sopimuksen heikentämistä.
  - lopullinen tulos: 9/9 vihreä, `BUILD SUCCESSFUL`.

## Regressio- ja auditointitulokset

- Kohdennettu Settings/component/passive/localization/accessibility-paketti: `BUILD SUCCESSFUL`.
  - `SettingsDisclosureContractTest`
  - `NoiseNotificationsSectionCopyTest`
  - `SettingsScreenStructureTest`
  - `SettingsViewModelPassiveMonitoringTest`
  - `SettingsViewModelDisplayPreferenceTest`
  - `SettingsViewModelNotificationScheduleTest`
  - `LocalizationBaselineTest`
  - `AccessibilityAuditPolicyTest`
  - `PluralAccessibilityResourceTest`
  - `UserPreferencesDataStoreMappingTest`
  - `PreferencesRepositoryTest`
- `:app:compileDebugScreenshotTestKotlin`: `BUILD SUCCESSFUL`.
- `:app:detekt`: ensimmäinen ajo löysi vain kaksi uuden testin formatointihavaintoa; ne korjattiin. Lopullinen ajo `BUILD SUCCESSFUL`.
- `:app:ktlintCheck`: `BUILD SUCCESSFUL`.
- `git diff --check`: ei whitespace-virheitä.

## Warning-, casing- ja accessibility-auditointi

- WAV: full warning inline vain, kun raw WAV -tallennuksen effective toggle on ON; OFF näyttää kompaktin infon.
- Lockscreen: full warning inline vain, kun public readings on ON; OFF näyttää kompaktin infon. Lockscreen master-toggle ja public-toggle eivät muuttuneet.
- Passive: inactive näyttää kompaktin infon; start confirmation ja active inline käyttävät täsmälleen samaa täyttä resource-tekstiä.
- Passive Confirm kutsuu nykyistä `onStartPassiveMonitoring`-callbackia, joka jatkaa olemassa olevaan permission/notification/ViewModel-polkuun. Cancel ja dialogin dismiss eivät kutsu starttia.
- Yhteinen compact-info käyttää yhtä dialogi- ja layout-toteutusta. Info-painikkeen minimi on `DbCheckTheme.spacing.space12` (48 dp) sekä leveydelle että korkeudelle.
- Tertiary renderöi annetun tekstin sellaisenaan. CSV-, WAV-, TWA- ja NIOSH-resursseihin tai overline-teksteihin ei tehty casing-muutoksia.
- Schedule-day-chipien valittu/valitsematon tila, `contentDescription` ja `stateDescription` säilyivät. Schedule käyttää edelleen `DbCheckSlider`ia ja Sound Reference omaa Canvas-railiaan.

## Commit

- Viesti: `Viimeistele asetusten yksityisyystiedot`
- Tämä raportti kuuluu samaan atomiseen Task 9 -committiin; lopullinen hash raportoidaan orkestroijalle commitin jälkeen.

## Huomiot

- Ei avoimia toiminnallisia huolia.
- Gradle tulosti projektissa ennestään olevat experimental screenshot-, deprecation- ja Gradle 10 -yhteensopivuusvaroitukset; ne eivät johdu tästä muutoksesta.
