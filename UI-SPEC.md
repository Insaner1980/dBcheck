# dBcheck UI Specification

Tama dokumentti kuvaa nykyisen kayttoliittyman koodista johdetun visuaalisen sopimuksen. Lahteina ovat sovelluksen Compose-ruudut, jaetut komponentit, teematiedostot, widget, notification-layoutit, share-kuva- ja PDF-renderoijat seka niihin liittyvat XML-resurssit. Dokumentti ei kuvaa uutta tavoitetilaa; kaikki mitat, varit, tilat ja poikkeukset alla ovat nykyisen toteutuksen mukaisia.

## 1. UI-teknologia ja paapolut

- Paakayttoliittyma on Jetpack Compose + Material 3.
- App kaynnistyy `MainActivity`ssa `enableEdgeToEdge()`-tilaan.
- Sisalto piirtyy vasta, kun ensimmainen `UserPreferences`-emissio on saatu. Ennen sita Compose-sisalto on tyhja `Unit`, jotta valittu teema ei valahda system-teemana.
- Teema valitaan `ThemeMode`-arvosta:
  - `DARK` pakottaa tumman teeman.
  - `LIGHT` pakottaa vaalean teeman.
  - `SYSTEM` lukee `isSystemInDarkTheme()`.
- Navigaation start destination on `meter`.
- Top-level-kohteet ovat `meter`, `analytics`, `history` ja `settings`.
- Fullscreen/ei-top-level-kohteita ovat `history/detail/{sessionId}`, `camera_overlay`, `sleep/setup`, `hearing_test/setup`, `hearing_test/recovery/setup`, `hearing_test/active`, `hearing_test/recovery/active`, `tinnitus/pitch`, `ambient/playback` ja `hearing_test/results/{testId}`.
- Bottom navigation nakyy vain top-level-kohteissa. Kamera, sleep, hearing, tinnitus, ambient ja hearing results eivat nayta bottom baria tai navigation railia.
- `history/detail/{sessionId}` kuuluu valituksi top-level-kohteeksi `history`, mutta se ei nayta bottom navigationia, koska reitti ei ole top-level-root.
- `settings?showPro={showPro}` kuuluu valituksi top-level-kohteeksi `settings`.
- Jos ikkunan leveys on vahintaan `600dp`, top-level-navigaatio muuttuu navigation railiksi.
- Kun rail on kaytossa tai navigaatio on piilossa, Scaffold kayttaa `WindowInsets.navigationBars`-sisaltoinsetteja. Kun bottom bar on kaytossa, content insets ovat nolla ja bottom bar hoitaa navigation bar -tilan.

## 2. Varipaletti

### 2.1 Tumma teema

- Background: `#080808`
- Surface: `#101010`
- Surface container: `#171717`
- Surface container high: `#202020`
- Surface container highest: `#2A2A2A`
- Surface container lowest: `#000000`
- On surface: `#F5F5F5`
- On surface variant: `#B8B8B8`
- Primary: `#F7F7F7`
- Primary dim: `#CFCFCF`
- Primary container: `#EDEDED`
- On primary container: `#080808`
- Secondary: `#8F8F8F`
- Tertiary: `#5E5E5E`
- Tertiary fixed dim: `#242424`
- Outline variant: `#8C8C8C`
- Error: `#E07A7A`
- Warning: `#C9A24D`
- Success: `#8EA58E`
- Ghost border: outline variant alpha `0.15f`
- Signature gradient: primary -> secondary

### 2.2 Vaalea teema

- Background: `#FAFAFA`
- Surface: `#FFFFFF`
- Surface container: `#F0F0EF`
- Surface container high: `#E6E6E3`
- Surface container highest: `#DADAD7`
- Surface container lowest: `#FFFFFF`
- On surface: `#111111`
- On surface variant: `#5F5F5F`
- Primary: `#111111`
- Primary dim: `#4A4A4A`
- Primary container: `#E4E4E1`
- On primary container: `#111111`
- Secondary: `#6E6E6E`
- Tertiary: `#8A8A86`
- Tertiary fixed dim: `#F2F2F0`
- Outline variant: `#8E8E8A`
- Error: `#B45F5F`
- Warning: `#9A7A33`
- Success: `#607460`
- Ghost border: outline variant alpha `0.20f`
- Signature gradient: primary -> secondary

### 2.3 Material-skeeman kaytto

- `DbCheckTheme.colorScheme.material` on Material `ColorScheme`.
- Custom-lisat ovat `warning`, `success`, `primaryDim`, `surfaceContainerLowest`, `tertiaryFixedDim`, `signatureGradient` ja `ghostBorder`.
- Korttien oletustausta on useimmiten `surfaceContainerHigh`.
- Tehoste- ja lukitustilat kayttavat `primaryContainer`, `surfaceContainerHighest`, `warning`, `success` ja `error` -vareja.
- Camera overlay kayttaa erillisia overlay-vareja:
  - Preview background `0xFF0B1114`
  - Preview band `0xFF26343D`
  - Overlay text `0xFFF2F5F1`
  - Overlay secondary text `0xFFC8D0CA`

## 3. Typografia

### 3.1 Fontit

- Paatekstiperhe: Manrope.
- Display/data-perhe: Space Grotesk.
- Fonttiresurssit:
  - `manrope_regular.ttf`
  - `manrope_medium.ttf`
  - `manrope_semibold.ttf`
  - `manrope_bold.ttf`
  - `space_grotesk_regular.ttf`
  - `space_grotesk_medium.ttf`
  - `space_grotesk_semibold.ttf`
  - `space_grotesk_bold.ttf`

### 3.2 dbCheck-tekstityylit

- `displayLg`: Space Grotesk Bold, `56sp`, line height `72.8sp`, letter spacing `-0.02sp`.
- `displayMd`: Space Grotesk Bold, `44sp`, line height `57.2sp`, letter spacing `-0.02sp`.
- `headlineLg`: Manrope SemiBold, `32sp`, line height `41.6sp`, letter spacing `-0.01sp`.
- `headlineMd`: Manrope SemiBold, `24sp`, line height `31.2sp`.
- `bodyLg`: Manrope Regular, `16sp`, line height `24sp`.
- `bodyMd`: Manrope Regular, `14sp`, line height `21sp`.
- `labelLg`: Space Grotesk Medium, `14sp`, line height `16.8sp`, letter spacing `0.05sp`.
- `labelMd`: Space Grotesk Medium, `12sp`, line height `14.4sp`, letter spacing `0.08sp`.
- `labelSm`: Space Grotesk Medium, `11sp`, line height `13.2sp`, letter spacing `0.05sp`.
- `dataXl`: Space Grotesk SemiBold, `32sp`, line height `38.4sp`, letter spacing `-0.01sp`.
- `dataLg`: Space Grotesk SemiBold, `24sp`, line height `28.8sp`.
- `dataMd`: Space Grotesk Medium, `16sp`, line height `19.2sp`.

### 3.3 MaterialTypography-silta

- `displayLarge` vastaa Space Grotesk Bold `56sp`.
- `displayMedium` vastaa Space Grotesk Bold `44sp`.
- `headlineLarge` vastaa Manrope SemiBold `32sp`.
- `headlineMedium` vastaa Manrope SemiBold `24sp`.
- `bodyLarge` vastaa Manrope Regular `16sp`.
- `bodyMedium` vastaa Manrope Regular `14sp`.
- `labelLarge` vastaa Space Grotesk Medium `14sp`.
- `labelMedium` vastaa Space Grotesk Medium `12sp`.
- `labelSmall` vastaa Space Grotesk Medium `11sp`.

## 4. Spacing, muodot ja varjot

### 4.1 Spacing-tokenit

- `space1`: `4dp`
- `space2`: `8dp`
- `space3`: `12dp`
- `space4`: `16dp`
- `space5`: `20dp`
- `space6`: `24dp`
- `space8`: `32dp`
- `space10`: `40dp`
- `space12`: `48dp`
- `space16`: `64dp`

### 4.2 Yleiset kayttotavat

- Paasivujen horisontaalinen content padding on yleensa `20dp`.
- Setup-tyyppisissa ruuduissa `DbCheckSetupScaffold` kayttaa `20dp` horisontaalista paddingia.
- Kameraoverlayn reunapadding on `24dp` kontrolliryhmissa ja readoutissa.
- Health Connect disclosure -activity kayttaa `24dp` horisontaalista paddingia ja `space8` vertikaalista paddingia.
- Mikrofoniestokehotus kayttaa `48dp` paddingia joka suunnasta.
- Settingsin Noise Notifications -osiossa on koodissa suoria `8.dp` ja `12.dp` valiarvoja tokenien rinnalla.
- Hearing setup -checklistissa ikonirivin gap on suoraan `16dp`, otsikon ja kuvauksen valissa `4dp`.
- Results disclaimer ja error-tekstit kayttavat lisaksi `12dp` horisontaalista sisapaddingia.

### 4.3 Shapes

- Material small shape: `8dp`
- Material medium shape: `12dp`
- Material large shape: `16dp`
- Material extra large shape: `24dp`
- `RoundedXxl`: `28dp`
- Yleinen korttikulma on `24dp`.
- Pienemmat metric tilet ovat yleensa `14dp` tai `16dp`.
- Chipit ja primary/secondary-painikkeet kayttavat `CircleShape`a.
- Bottom navin valittu ikonitausta kayttaa `16dp` pyoristysta.
- Modal bottom sheetin ylakulmat ovat `28dp`.

### 4.4 Varjot ja gradientit

- `AmbientShadow`: offsetY `12dp`, blur `24dp`, color `primaryDim` alpha `0.04f`.
- Signature button gradient on lineaarinen gradientti kulmassa 135 astetta, primary -> secondary.
- Signature sweep gradient on primary -> secondary -> primary.
- Signature vertical gradient on primary -> secondary.
- Nykyinen Compose UI kayttaa varjoja hillitysti; paapainikkeen vaikutelma tulee gradientista ja painetun tilan alphasta.

## 5. Motion ja animaatiot

- Primary `DbCheckButton` painettuna alpha `0.85f`.
- Secondary `DbCheckButton` painettuna alpha `0.92f`.
- Tertiary `DbCheckButton` painettuna tausta `primary.copy(alpha = 0.08f)`.
- `SkeletonLoader` shimmer:
  - Infinite transition.
  - `tween(1200, LinearEasing)`.
  - Translation `0f..1000f`.
  - Gradient surfaceContainerHigh -> surfaceContainerHighest -> surfaceContainerHigh.
  - Gradient start `translate - 200`, end `translate + 200`.
- `CircularGauge`:
  - Sweep animoi `animateFloatAsState`-animaatiolla `200ms`, easing `EaseOut`.
  - Hengitys-/breathing-scale kayttaa infinite transitionia `1f..1.02f`.
  - Breathing duration `3000ms`, easing `EaseInOut`, repeat mode reverse.
- `NoiseLevelPill` kayttaa `AnimatedContent`ia ja `fadeIn() togetherWith fadeOut()` -transitionia, ellei motion ole erikseen pois paalta.
- `SoundReferenceCard` kayttaa `animateContentSize()` laajennuksen ja tiivistetyn tilan valilla.
- Settingsin Pro-korttiin scrollaus kayttaa `scrollState.animateScrollTo(scrollState.maxValue)`.
- Pro-lukitut previewt:
  - Android S+ kayttaa `blur(4.dp)`.
  - Vanhemmilla versioilla fallback on `alpha(0.4f)`.
- History searchin lukittu preview kayttaa samaa `blur(4.dp)` / `alpha(0.4f)` -periaatetta.
- Muita ruutusiirtyma-animaatioita ei ole koodissa eksplisiittisesti maaritelty; navigation kayttaa Compose Navigationin oletuksia.

## 6. Jaetut komponentit

### 6.1 dbCheckButton

- Tyylit: Primary, Secondary, Tertiary.
- Oletuskorkeus `56dp`.
- Minimikosketusala `48dp`.
- Sisapadding oletuksena horizontal `32dp`.
- Primary:
  - Korkeus max(pyydetty korkeus, `48dp`).
  - Min width `48dp`.
  - Shape `CircleShape`.
  - Tausta signature gradient.
  - Painettu alpha `0.85f`.
  - Teksti `bodyLg` + SemiBold, vari `onPrimary`.
- Secondary:
  - Shape `CircleShape`.
  - Tausta `surfaceContainerHighest`.
  - Painettu alpha `0.92f`.
  - Teksti `bodyLg`, vari `onSurface`.
- Tertiary:
  - Min width ja height `48dp`.
  - Tausta transparent, painettuna primary alpha `0.08f`.
  - Teksti uppercase, style `labelLg`, vari `primary`.

### 6.2 DbCheckCard

- Shape `RoundedCornerShape(24.dp)`.
- Oletustausta `material.surfaceContainerHigh`.
- Oletussisapadding `20dp`.
- Kortti ei oletuksena piirra reunusta tai varjoa.

### 6.3 DbCheckChip

- Min width `48dp`, min height `48dp`.
- Shape `CircleShape`.
- Valittu tausta `primaryContainer`, sisalto `onPrimaryContainer`.
- Ei-valittu tausta `surfaceContainerHigh`, sisalto `onSurfaceVariant`.
- Semantics role `Checkbox`.
- State description `selected` / `not selected`.
- Oletuspadding horizontal `16dp`, vertical `8dp`.
- Leading iconin ja labelin vali `4dp`.
- Label style `labelLg`, maxLines `1`, overflow ellipsis.
- `horizontalPadding` voidaan ohittaa, esimerkiksi tiiviissa chip-riveissa `8dp` tai schedule-paivissa `10dp`.

### 6.4 DbCheckSlider

- Column full width.
- Valinnainen label kayttaa `dataMd`-tyylia ja `onSurface`-varia.
- Material Slider on full width.
- Thumb ja active track ovat `primary`.
- Inactive track on `surfaceContainerHighest`.
- Disabled thumb on `onSurfaceVariant`.
- Disabled active on `onSurfaceVariant.copy(alpha = 0.38f)`.
- Disabled inactive on `surfaceContainerHighest`.

### 6.5 DbCheckToggle

- Switch checked track `primary`.
- Checked thumb `surfaceContainerLowest`.
- Unchecked track `surfaceContainerHighest`.
- Unchecked thumb `onSurfaceVariant`.

### 6.6 Top app bar

- Row full width.
- Padding horizontal `20dp`, vertical `12dp`.
- Sisalto `SpaceBetween`.
- Vasemmalla `GraphicEq` `24dp`, vari `primary`, seka app-nimi `bodyLg` Manrope SemiBold.
- Oikealla optional action `IconButton`, ikoninkoko `24dp`, vari `onSurfaceVariant`.
- Jos action puuttuu, oikealle tulee `Spacer(48.dp)`.

### 6.7 Bottom navigation

- Root `Box` full width, background `material.surface`.
- Kayttaa `WindowInsets.navigationBars`-paddingia.
- Row korkeus `64dp`.
- Kohteet ovat tasalevyisia `weight(1f)`.
- Kohteen role on `Tab`.
- Ei ripple-indicationia.
- Kohteen vertical padding `8dp`.
- Ikonilaatikko padding horizontal `16dp`, vertical `4dp`.
- Valittu ikonilaatikko: `RoundedCornerShape(16.dp)`, tausta `primary.copy(alpha = 0.12f)`.
- Valittu label nakyy vain valitussa itemissa, style `labelSm`, vari `primary`.
- Ei-valittu kohde nayttaa vain outline-ikonin ilman labelia.

### 6.8 Navigation rail

- Kaytossa leveydesta `600dp` alkaen.
- Container `material.surface`.
- Kayttaa `statusBarsPadding()`.
- Kohteet keskitetaan pystysuunnassa kahdella `Spacer(weight(1f))`.
- Ikoni on selected/unselected Material icon, label nakyy rail itemissa.

### 6.9 ProLockOverlay ja locked CTA

- Unlocked-tilassa renderoi sisallon sellaisenaan.
- Locked-tilassa sisalto blurrataan Android S+ laitteilla `4dp`; muilla alpha `0.4f`.
- Overlay:
  - Tayttaa sisallon.
  - Clip `24dp`.
  - Tausta `material.surface.copy(alpha = 0.6f)`.
  - Padding `24dp`.
  - Sisalto keskitetaan.
- `ProUpgradePrompt`:
  - Oletusikoni `32dp`, vari `onSurfaceVariant`.
  - Title `bodyMd`, top margin `8dp`, bottom margin `16dp`.
  - Upgrade button primary, korkeus `48dp`.
- `DbCheckLockedCtaCard`:
  - Card title `headlineMd`.
  - Subtitle `bodyMd`, valissa `space2`.
  - Painike primary, korkeus `44dp`, ylapuolella `space4`.

### 6.10 EmptyState

- Full width.
- Padding `48dp`.
- Sisalto keskitetty.
- Ikoni `64dp`, vari `onSurfaceVariant.copy(alpha = 0.5f)`.
- Ikonin jalkeen `24dp`, title `headlineMd`, sitten `8dp`, description `bodyMd`.
- CTA-painike `48dp`, ylapuolella `24dp`.

### 6.11 SkeletonLoader

- Oletuskorkeus `120dp`.
- Full width.
- Oletuskulma `24dp`.
- Tausta shimmer-gradientilla, ks. motion.

### 6.12 Setup scaffold

- Full-size background `material.background`.
- Sisalto verticalScroll.
- Back `IconButton` padding `space3`.
- Content column full width.
- Horizontal padding `20dp`.
- Vertikaalinen arrangement oletuksena `Top`, ruudut voivat antaa `Arrangement.spacedBy(...)`.

## 7. Mittari

### 7.1 MeterScreen-rakenne

- Root Column tayttaa ruudun, horizontal alignment center.
- Ylaosassa `DbCheckTopAppBar`, action `Settings`.
- Jos mikrofoniestokehotus on aktiivinen, naytetaan kokoruudun lupakehotus.
- Muuten sisalto jakautuu mittarisisaltoon ja alareunan kontrolliosioon.
- Jos kaytettava korkeus on alle `640dp`, mittarisisalto muuttuu scrollattavaksi ja kontrollit tulevat sisallon jalkeen.
- Jos korkeus on vahintaan `640dp`, mittarisisalto tayttaa ruudun ja kontrollit ankkuroituvat alas `Spacer(weight(1f))`-ratkaisulla.

### 7.2 Mikrofoniestokehotus

- Column full size, padding `48dp`, centered.
- Mic-ikoni `64dp`, vari `onSurfaceVariant` alpha `0.5f`.
- Ikonin jalkeen `space6`.
- Title `headlineMd`, centered.
- Description `bodyMd`, centered, valissa `space3`.
- Painikkeet:
  - Open Settings primary, korkeus `48dp`.
  - Try Again secondary, korkeus `48dp`.
  - Painikkeiden valissa `space3`.

### 7.3 Meter readout -jarjestys

- Alkuun `space8`.
- Mode chip row horizontal padding `20dp`, chipien vali `space3`.
- Seuraavaksi `space6`.
- Jos tallennus on kaynnissa, `MeterSessionInfoBar` horizontal padding `20dp`, sitten `space4`.
- `CircularGauge`.
- Seuraavaksi `space6`.
- Jos dosimeter on kaytossa ja mode on DOSIMETER, naytetaan `DosimeterGaugeCard` horizontal padding `20dp`; muuten `LiveSoundLevelChart`.
- Chartin jalkeen `space4`.
- `SoundReferenceCard` horizontal padding `20dp`.
- Sen jalkeen `space4`.
- `WaveformVisualization` horizontal padding `20dp`.
- Sen jalkeen `space4`.
- Error message full width horizontal padding `20dp`, jos error on olemassa.
- Stats row horizontal padding `20dp`, kolmen kortin vali `12dp`.
- Sleep CTA naytetaan, jos sleepCardEnabled; ylapuolella `space4`, horizontal padding `20dp`.

### 7.4 Meter mode chipit

- DB Meter chip on aina nakyvissa.
- Dosimeter chip naytetaan, jos dosimeterCardEnabled tai kayttaja ei ole Pro.
- Free-kayttajan Dosimeter-teksti on locked-variantti ja click ohjaa upgrade-polkuun.
- Kun dosimeterCardEnabled on false, effective mode on aina DB_METER.

### 7.5 CircularGauge

- Kokonaiskoko `288dp`.
- Canvas piirtaa:
  - Arc track start angle `135`.
  - Sweep `270`.
  - Stroke width `12dp`.
  - Stroke cap round.
- Sweep perustuu `SoundLevelDisplayScale.positionForDb(currentDb) * 270`.
- Keskella glass circle: `material.surface.copy(alpha = 0.6f)`, radius `0.85 * gauge radius`.
- Tick marks:
  - 28 kappaletta.
  - Outer radius `radius + stroke/2 + 4dp`.
  - Inner radius `outer - 6dp`.
  - Stroke `1.5f`.
- Arc-varit:
  - QUIET: success + success alpha `0.6`.
  - NORMAL: primary -> secondary.
  - ELEVATED: warning + alpha `0.8`.
  - DANGEROUS: error + alpha `0.8`.
- Keskisisalto:
  - Label "decibels": `labelMd`.
  - Nykyinen arvo kokonaislukuna: `displayLg`.
  - "dB": `dataLg`.
  - NoiseLevelPill alapuolella, vali `6dp`.

### 7.6 NoiseLevelPill

- Shape `CircleShape`.
- Padding horizontal `16dp`, vertical `6dp`.
- Teksti uppercase `labelMd`.
- Vari:
  - QUIET success.
  - NORMAL primary.
  - ELEVATED warning.
  - DANGEROUS error.
- Tekstivari valitaan kontrastin mukaan `onPrimary` / `onPrimaryContainer`.
- Label vaihtuu fade-animaatiolla.

### 7.7 LiveSoundLevelChart

- Box full width, korkeus `116dp`.
- Canvas:
  - Nelja vaakagridiviivaa.
  - Grid vari `outlineVariant.copy(alpha = 0.32f)`, stroke `1`.
  - 85 dB threshold line `error.copy(alpha = 0.72f)`, stroke `2`.
  - Data path vari `primary`, stroke `4`, cap round.
  - Yksi piste piirretaan secondary-varilla ympyrana.
  - Peak markers db >= 85 punaisina, radius `5f`.
- Tyhjassa tilassa keskitetty `bodyMd`-teksti.
- Paused-label on `labelSm` oikeassa ylakulmassa.

### 7.8 WaveformVisualization

- Canvas full width, korkeus `48dp`.
- Wave vari `tertiary.copy(alpha = 0.2f)`.
- BAR-tila piirtää pyoreapaaiset pystylinjat, stroke `2dp`, korkeus `amplitude * midY * 0.8`.
- LINE-tila piirtää pathin, stroke `2dp`.
- FILLED-tila tayttaa pathin alle alpha `0.08`.

### 7.9 Meter controls

- Root Row, gap `space6`.
- Vasemman ja oikean sivupainikkeen koko `48dp`.
- Sivupainikkeet ovat ympyroita, tausta `surfaceContainerHighest`, icon `24dp`.
- Keskipainikkeen koko `80dp`.
- Keskipainike on ympyra signature gradientilla, icon `36dp`, icon vari `onPrimary`.
- Camera side button alpha `1f` tai `0.55f`.
- Share side button alpha `1f` tai `0.4f`, disabled jos sampleCount == 0.

### 7.10 MeterSessionInfoBar

- `DbCheckCard` semantics-sisallolla.
- Ylarivi:
  - RecordingIndicator red dot `8dp`.
  - "REC" `labelMd`, red/error.
  - Duration `dataMd`.
  - SpaceBetween.
- Ylarivin jalkeen `space3`.
- Pillit riveissa, gap `space3`.
- Peruspillit: weighting ja response.
- Pro details -tilassa toinen rivi sisaltaa sample rate pillin weight `0.8` ja input pillin weight `1.2`.
- `MetricValueTile` shape `14dp`, value maxLines `2`.

### 7.11 DosimeterGaugeCard

- Locked/unavailable-tilat nayttavat viestikortin.
- Data-tilassa:
  - Headerissa labelMd ja optional standard badge.
  - Badge: pyoristetty `999dp`, background `primaryContainer`, padding horizontal `10dp`, vertical `5dp`, text `labelSm`.
  - Main row: gauge `140dp`, spacer `space4`, TWA/LAeq tilet.
  - Gauge arc:
    - Track `outlineVariant.copy(alpha = 0.32f)`.
    - Start angle `140`, sweep `260`.
    - Stroke `14dp`, cap round.
    - Progress vari success alle 80%, warning 80-99.999%, error 100%+.
  - Keskella dose `dataXl` ja label `labelSm`.
  - Projected dose / remaining row gap `space3`.
  - Lopussa standard reference `labelSm`.

### 7.12 SoundReferenceCard

- `DbCheckCard` full width, `animateContentSize`.
- Header label + expand icon.
- Summary nayttaa nearest/current-tiedon.
- Rail:
  - Korkeus `24dp`.
  - Track stroke `4dp`, round cap, vari `outlineVariant`.
  - Nearest marker warning, radius `5dp`.
  - Current marker primary, radius `7dp`.
- Expanded rows:
  - Min height `48dp`.
  - Shape `14dp`.
  - Background `primaryContainer` nearest-rivilla, muuten `surfaceContainerHighest`.
  - Padding horizontal `12dp`, vertical `10dp`.
  - Optional check icon `18dp`.
  - Closest badge käyttää surface-alpha-taustaa, padding horizontal `8dp`, vertical `4dp`.

### 7.13 StatCard

- Shape `16dp`.
- Tausta `surfaceContainerHigh`.
- Padding horizontal `20dp`, vertical `16dp`.
- Sisainen gap `4dp`.
- Sisalto keskitetty.
- Label uppercase `labelMd`.
- Value `dataLg`.
- Unit `labelSm`.

## 8. Analytics

### 8.1 Rakenne

- Root Column.
- Top app barissa Person-action Settingsiin.
- Loading-tila: Column padding `20dp`, gap `16dp`, skeletonit `200dp` ja `120dp`.
- Empty ja Error kayttavat `EmptyState`a.
- Success:
  - VerticalScroll.
  - Horizontal padding `20dp`.
  - Vertical arrangement `space4`.
  - Header controls, sitten `space2`, sitten section-kortit, lopussa `space4`.

### 8.2 Header controls

- Label `labelMd`.
- Title `headlineLg`.
- Section chip row:
  - Chipit equal weights.
  - Gap `space2`.
  - Spectral ja Environment ovat locked Free-kayttajalle.
- Overview range chip row nakyy vain Overview-sectionissa.
- Weekly on Free-kayttajalle kaytettavissa, Monthly locked Free-kayttajalle.
- Locked chip voi sisaltaa Lock-ikonin `14dp`.

### 8.3 Section card -jarjestys

- Overview Weekly: Weekly Exposure, Hearing Health, Yearly Report, Hearing Test, Hearing Recovery, Tinnitus Pitch, Ambient Sound, optional Sleep Setup.
- Overview Monthly: Monthly Trend, Yearly Report, Hearing Test, Hearing Recovery, Tinnitus Pitch, Ambient Sound, optional Sleep Setup.
- Spectral: Spectral Analysis.
- Environment: Sound Detection jos enabled, Active Environment Mix jos recording && Pro, Environment Mix aina.

### 8.4 ExposureSummaryCard

- `DbCheckCard`.
- Header SpaceBetween.
- Avg value `dataXl`, yksi desimaali.
- WeeklyBarChart full width, korkeus `120dp`, ylapuolella `16dp`.
- Chart:
  - Bar gradient primary -> secondary.
  - Ei-tanaan olevat palkit dim alpha `0.6`.
  - Label size `11sp`.
  - Label bottom padding `4sp`.
  - Bar gap `8f`.
  - Bar max height `avg/max * chartHeight * 0.85`.
  - Bar corner `8f`.
  - Paivalabel on ensimmainen kirjain uppercasena.

### 8.5 HearingHealthCard

- Icon `28dp`.
- Icon valitaan Check/Warning/Error ja varitetaan success/warning/error.
- Iconin jalkeen `12dp`.
- Title `bodyLg`.
- Description/comparison `bodyMd`, ylapuolella `8dp`.

### 8.6 HearingRecoveryCard ja HearingTestCta

- `HearingRecoveryCard` on ProLockOverlayn sisalla.
- Card gap `space3`.
- Title `labelMd`.
- Description `bodyMd`.
- Metrics row gap `16dp`.
- Metric label `labelSm`, value `dataLg`.
- Action button:
  - MissingBaseline -> primary.
  - Muutoin secondary.
  - Full width, korkeus `48dp`.
- `HearingTestCta` kayttaa locked CTA -korttia.

### 8.7 MonthlyTrendChart

- ProLockOverlay.
- Card header SpaceBetween.
- Canvas full width, korkeus `148dp`.
- Grid 4 lines, alpha `0.24`.
- Tyhjien pisteiden radius `2.5f`.
- Trend path stroke `4`, cap round.
- Data point radius `4f`.
- Vertical padding fraction `0.08`.
- Locked preview: 30 datapistetta formula-arvoilla, LAeq `68.4`.
- Empty preview: 30 null-pistetta, LAeq `--`.

### 8.8 YearlyReportCard

- ProLockOverlay.
- Card gap `16dp`.
- Title `labelMd`.
- Kolme stat-saraketta: sessions, 12mo LAeq, loudest.
- Subtitle `bodyMd`.
- ZoneDistributionBar:
  - Korkeus `24dp`.
  - Track `outlineVariant.copy(alpha = 0.35f)`.
  - Corner radius `12f`.
- Zone rows gap `8dp`.
- Dot `8dp`.
- Locked preview:
  - sessions `86`
  - LAeq `67.8`
  - loudest `94 dB`
  - zones Quiet 34, Moderate 42, Loud 18, Critical 6.

### 8.9 EnvironmentMixCard

- ProLockOverlay.
- Card title `labelMd`, spacer `16dp`.
- Rows gap `12dp`.
- Row dot `8dp`.
- Label `bodyMd`.
- Percent `dataMd`.
- Varit: Quiet success, Moderate primary, Loud warning, Critical error.
- Locked preview: Quiet 52, Moderate 34, Loud 12, Critical 2.
- Empty rows nayttavat 0%.

### 8.10 AmbientSoundCard ja TinnitusPitchCard

- Molemmat ovat ProLockOverlayn sisalla.
- Card gap `space3`.
- Title `labelMd`.
- Description `bodyMd`.
- Secondary button full width, korkeus `48dp`.
- Tinnitus locked preview nayttaa left `1000Hz` ja right `4000Hz`, summary `dataMd`.

### 8.11 SpectralAnalysisCard

- ProLockOverlay.
- Card header SpaceBetween, title `labelMd`, status label `labelMd` primary.
- Status kertoo live capture / waiting -tilan.
- Mode chip row ylapuolella ja alapuolella `16dp`.
- Bars mode:
  - Canvas korkeus `64dp`.
  - Gap `3f`.
  - Bars primary alpha `0.7`.
  - Corner `4f`.
  - FrequencyLabels alla.
- Spectrogram mode:
  - Canvas korkeus `72dp`.
  - Background `surfaceContainerHighest.copy(alpha = 0.46f)`, corner `8f`.
  - 12 rows x 24 bands.
  - Cell gap `1f`.
  - Cell color lerp: low primary alpha `0.14` -> high tertiary alpha `0.86`.
- RTA mode:
  - Canvas korkeus `96dp`.
  - Background `surfaceContainerHighest.copy(alpha = 0.46f)`, corner `8f`.
  - Gap `4f`.
  - Bars lerp primary alpha `0.24` -> secondary alpha `0.92`.
  - Corner `5f`.
  - RTA stat pill rows ylapuolella `12dp`.
- Spectral stat pill rows:
  - 2 pillia per rivi.
  - Gap `space2`.
  - Shape `8dp`.
  - Background `surfaceContainerHigh`.
  - Padding horizontal `12dp`, vertical `10dp`.
  - Label `labelSm`, value `labelMd`.

### 8.12 SoundDetectionCard

- Outer height fixed `352dp`.
- ProLockOverlay pitaa saman korkeuden.
- Card:
  - Header status `labelMd`.
  - Spacer `16dp`.
  - Current sound block gap `4dp`.
  - Label `labelSm`, current value `headlineMd` ellipsis, description `bodyMd`.
  - Spacer `16dp`.
  - Confidence meter:
    - Label row SpaceBetween.
    - Track height `8dp`, rounded `4dp`, background `surfaceContainerHighest`.
    - Fill primary, leveys confidence/100.
  - Spacer `16dp`.
  - Recent detections label `labelSm`.
  - Empty text `bodyMd`.
  - Max 3 recent rows.
  - Recent row shape `8dp`, bg `surfaceContainerHigh`, padding horizontal `12dp`, vertical `8dp`.
  - Recent label `bodyMd` ellipsis, confidence `labelMd`.
- Locked preview: confidence 82, recent Speech 82, Music 61, Vehicle 47.

### 8.13 HeartRateOverlay

- Ei renderoi, jos samples puuttuu tai range on virheellinen.
- Column gap `space2`.
- Header `labelMd`, sisaltaa latest BPM.
- Canvas full width, height `space16` eli `64dp`.
- Min BPM pakotetaan enintaan 60, max vahintaan 120.
- Baseline piirretaan `ghostBorder`-varilla.
- Heart rate path vari `warning`, stroke `space1` eli `4dp`, cap round.
- Footer `labelSm`, muodossa min-max BPM.

## 9. History

### 9.1 Rakenne

- Top app barissa Settings-action.
- Loading: padding `20dp`, gap `16dp`, skeletonit `180dp`, `80dp`, `80dp`.
- Empty/Error kayttavat `EmptyState`a.
- Success:
  - `LazyColumn`.
  - Horizontal padding `20dp`.
  - Vertical gap `space4`.
  - Optional `SessionNamingSheet`.
  - Header, optional metadata error, Last24HoursChart, search controls, recent sessions, stat-kortit, bottom spacer.

### 9.2 Header ja listat

- Header label `labelMd`, title `headlineLg`, valissa `space2`.
- Metadata error `bodyMd` error-varilla.
- Recent sessions header SpaceBetween:
  - Label `labelMd`.
  - Tertiary button View all / Showing all.
- Search no-results text `bodyMd`, `onSurfaceVariant`.
- Bottom row sisaltaa WeeklyTrendCard ja SafeHoursCard equal weights, gap `12dp`.

### 9.3 SessionCard

- Full width row.
- Clip `24dp`.
- Clickable role `Button`.
- Tausta `surfaceContainerHigh`.
- Padding `20dp`.
- Sisainen gap `16dp`.
- Emoji circle:
  - Koko `48dp`.
  - Background `surfaceContainer`.
- Title `bodyLg`, ellipsis.
- Metadata uppercase `labelSm`.
- Sleep badge optional metadata-rivilla.
- Tags naytetaan max 2:
  - Teksti `#tag`.
  - Style `labelSm`.
  - Vari `primary`.
  - Gap `6dp`.
- Stats:
  - Kaksi saraketta PEAK/AVG.
  - Gap `16dp`.
  - Value `dataLg`.
  - Label `labelMd`.
  - Peak value warning-varilla, jos arvo >= `NoiseAlertPolicy.PEAK_WARNING_DB`; muuten `onSurface`.
- Optional edit IconButton:
  - Koko `48dp`.
  - Icon lock/edit `20dp`.

### 9.4 HistorySearchControls

- Card lukittuna:
  - Sisalto blur `4dp` Android S+ tai alpha `0.4f`.
  - Overlay matchParent.
  - Clip `24dp`.
  - Background `surface.copy(alpha = 0.62f)`.
  - Padding `20dp`.
  - Keskitetty ProUpgradePrompt, icon `28dp`.
- Card column gap `space3`.
- Search field:
  - `OutlinedTextField` full width.
  - Single line.
  - Shape `12dp`.
  - Leading Search icon.
  - Trailing Close icon, jos unlocked ja query ei ole blank.
  - Focused border `primary.copy(alpha = 0.3f)`.
  - Unfocused border `ghostBorder`.
- Filters:
  - FlowRow gap horizontal/vertical `8dp`.
  - Chipit: All, A-weighted, Loud, With location.
  - Kayttaa `DbCheckChip`.

### 9.5 SessionNamingSheet

- `ModalBottomSheet`.
- Container `material.surface`.
- Ylakulmat `28dp`.
- Sisalto verticalScroll.
- Padding horizontal `20dp`, vertical `8dp`.
- Bottom spacer `32dp`.
- Title `headlineMd`.
- Name field ylapuolella `16dp`, shape `12dp`.
- Emoji group:
  - 12 emojia.
  - Jokainen `48dp` ympyra.
  - Selected background `primaryContainer`, muuten `surfaceContainerHigh`.
  - Role `RadioButton`.
- Tags FlowRow:
  - Predefined tags `R.array.session_predefined_tags`.
  - Gap `8dp x 8dp`.
- Custom tag row:
  - Gap `8dp`.
  - Text field weight `1`.
  - Add secondary button height `56dp`.
  - Enabled vain, jos custom tag ei ole blank.
- Save button full width height `48dp`, enabled vain muutoksilla.
- Group title `labelLg`, sitten `8dp`, sitten FlowRow.

### 9.6 Last24HoursChart

- `DbCheckCard`.
- Header row:
  - Title `labelMd`.
  - Subtitle `bodyMd`.
  - Max `dataXl` + unit `labelSm`.
- Spacer `16dp`.
- Canvas full width height `100dp`.
- Jos dataa:
  - Fill gradient primary alpha `0.3` -> transparent.
  - Line primary stroke `2dp`, cap round.
  - Geometry käyttää avgDb / maxDb * height * `0.85`.
  - Yksi piste piirretaan ympyrana, radius `3dp`.
- Jos ei dataa: korkeus `100dp` ja tekstisisalto.
- X-axis labels:
  - Ylapuolella `8dp`.
  - 5 labelia: nelja aikaa + now.

### 9.7 WeeklyTrendCard ja SafeHoursCard

- Molemmat kayttavat `DbCheckCard`.
- SafeHoursCard value `dataXl`, vari success, formaatti `%.1fh`.
- WeeklyTrendCard value `dataXl`, vari success jos percent < 0, muuten warning.

## 10. Session Detail

### 10.1 Rakenne

- Top bar:
  - Padding horizontal `8dp`, vertical `12dp`.
  - Back IconButton.
  - Title `bodyLg`, ellipsis, weight `1`.
  - Optional metadata IconButton lock/edit.
- Viestit poistuvat `3000ms` jalkeen.
- Loading: centered `onSurfaceVariant` text.
- Locked ja missing detail:
  - Centered column, gap `12dp`.
  - Icon `48dp`.
  - Title `headlineMd`.
  - Message `bodyMd`.
  - Primary upgrade/back button.
- Loaded:
  - `LazyColumn`.
  - Horizontal padding `20dp`.
  - Vertical gap `space4`.
  - Jarjestys: SessionSummary, KpiGrid, optional SleepResults, optional SleepInsights, TimeSeriesCard, DbHistogramCard, PeakEventsCard, ReportActions, bottom spacer `space4`.

### 10.2 SessionSummary

- Column gap `6dp`.
- Label `labelMd`.
- Row gap `8dp`.
- Emoji `headlineLg`.
- Session name `headlineLg`, ellipsis.
- Tags FlowRow gap horizontal `8dp`, vertical `6dp`, style `labelMd`, vari `primary`.
- Date range `bodyMd`.
- Duration `bodyMd`.

### 10.3 KpiGrid ja KpiCard

- Kaksi riviä.
- Rivien ja korttien gap `12dp`.
- Kaksi korttia per rivi equal weights.
- KpiCard:
  - `DbCheckCard`.
  - Minimum height `112dp`.
  - Column gap `8dp`.
  - Label uppercase `labelMd`.
  - Value `dataXl`.

### 10.4 SleepResultsCard

- `DbCheckCard` full width.
- Column gap `16dp`.
- Title uppercase `labelMd`.
- Nelja metric-rivia, jokaisessa kaksi metricia, gap `12dp`.
- Rivit:
  - target / recorded duration
  - equivalent level / max
  - LCpeak / peak events
  - loud periods / samples
- Metric:
  - Column gap `4dp`.
  - Label uppercase `labelSm`.
  - Value `dataMd`, ellipsis.

### 10.5 SleepInsightsCard

- `DbCheckCard`.
- Column gap `12dp`.
- Title uppercase `labelMd`.
- Missing-data: `bodyMd`.
- Quiet summary: `bodyMd`.
- Jos notable eventteja:
  - Row gap `12dp`.
  - Metricit notable events ja loudest period.
  - Loudest label sisaltaa duration / oneDecimal(maxDb) dB.

### 10.6 TimeSeriesCard

- `DbCheckCard` full width.
- Column gap `16dp`.
- Title uppercase `labelMd`.
- Empty state `bodyMd`.
- Chart:
  - Canvas full width, height `168dp`.
  - Sama koordinaattimuunnos kuin PDF: `PdfChartRenderer.mapTimeSeries`.
  - Min dB `report.minDb.coerceAtMost(NoiseLevel.QUIET.maxDb)`.
  - Max dB `report.maxDb.coerceAtLeast(100f)`.
  - 4 grid lines `ghostBorder`, stroke `1dp`.
  - Yksi piste: primary circle radius `4dp`.
  - Useampi piste: primary path stroke `3dp`, cap round.
- Heart rate overlay naytetaan ehdollisesti; unavailable/samples-empty-tilat ovat `bodyMd`.

### 10.7 DbHistogramCard

- ProLockOverlay.
- Locked modifier ja sisalto height `360dp`, full width.
- Card gap `12dp`.
- Title uppercase `labelMd`.
- Empty state `bodyMd`.
- Canvas:
  - Full width.
  - Height `132dp`.
  - Background `surfaceContainerHighest.copy(alpha = 0.46f)`.
  - Rounded `10dp`.
  - 4 grid lines.
  - Bar gap `4dp`.
  - Bar corner `5dp`.
  - Min visible height `2dp`, jos bucketissa sampleja.
- Bucket chips:
  - FlowRow gap `8dp x 8dp`.
  - Shape `8dp`.
  - Background bucket color alpha `0.14`.
  - Padding horizontal `10dp`, vertical `6dp`.
  - Dot `8dp`, rounded `4dp`.
  - Label `labelSm`.
- Histogram bucket color midpointin mukaan:
  - QUIET primary alpha `0.62`.
  - NORMAL success alpha `0.78`.
  - ELEVATED warning alpha `0.88`.
  - DANGEROUS error alpha `0.88`.
- Locked preview bucketit:
  - 0-10 0%, 10-20 0%, 20-30 4%, 30-40 8%, 40-50 15%, 50-60 23%, 60-70 19%, 70-80 15%, 80-90 8%, 90-100 4%, 100-110 4%, 110-120 0%, 120-130 0%.

### 10.8 PeakEventsCard

- `DbCheckCard`.
- Column gap `12dp`.
- Title uppercase `labelMd`.
- Jos A-weighted metrics unavailable: `bodyMd`.
- Jos ei eventteja: `bodyMd`.
- Muuten naytetaan enintaan 5 rowta.
- Row SpaceBetween:
  - Time `bodyMd`, vari `onSurfaceVariant`.
  - Max dB `dataMd`, vari warning.

### 10.9 ReportActions

- Column gap `12dp`.
- PDF export:
  - ProLockOverlay.
  - Locked height `164dp`, full width.
  - ExportPdfCard height `164dp`.
  - Card column gap `16dp`.
  - Row icon PictureAsPdf primary + label `labelMd`.
  - Button full width, oletuskorkeus `56dp`, disabled ja teksti muuttuu exportin aikana.
- PNG share:
  - `DbCheckCard`.
  - Row gap `12dp`.
  - Share icon primary.
  - Secondary button full width.
- WAV recording card:
  - Naytetaan vain, jos session WAV on olemassa.
  - Card column gap `12dp`.
  - Row icon + title/subtitle.
  - Action row gap `8dp`.
  - Share WAV ja Delete WAV -painikkeet equal weights.
  - Share WAV primary Pro-kayttajalle, muuten secondary.
- ActionMessage centered `bodyMd`, success tai error.

## 11. Settings

### 11.1 Rakenne

- Top app bar ilman actionia.
- Content verticalScroll.
- Horizontal padding `20dp`.
- Vertical gap `space4`.
- Jarjestys:
  - SettingsHeader
  - AudioCalibrationSection
  - NoiseNotificationsSection
  - HealthSyncSection
  - DataExportSection
  - DisplayAndFeaturesSection
  - ProUpsellCard, jos naytettava
  - Footer
- `scrollToProCard` scrollaa maksimiin animateScrollTo-animaatiolla.
- Purchase/csv/backup/health/calibration/passive-viestit poistuvat `3000ms` jalkeen.
- Header:
  - Label `labelMd`.
  - Title `headlineLg`.
  - Spacer `space2`.
- Footer:
  - App version `labelSm`, centered.
  - Vertical padding `space6`.

### 11.2 SettingsRows

- `SettingsDescriptionRow`:
  - Full width row.
  - Gap `space3`.
  - CenterVertically.
  - Optional leading icon tint primary.
  - Text column weight `1`, gap `space1`.
  - Title default `bodyLg`.
  - Subtitle default `bodyMd`.
- `SettingsCardColumn`:
  - `DbCheckCard` full width.
  - Column gap default `space4`.
- `SettingsActionCard`:
  - Description row.
  - Secondary button full width, height `space12` (`48dp`).
- `SettingsToggleDescriptionRow`:
  - Description row + trailing `DbCheckToggle`.
- `SettingsLockedCardSection`:
  - Title `labelMd`.
  - Spacer `space3`.
  - ProLockOverlay around `DbCheckCard`.

### 11.3 AudioCalibrationSection

- Koko osio on `SettingsLockedCardSection`, locked jos !Pro.
- Sisaltojarjestys:
  - MicSensitivityControls.
  - `space5`.
  - FrequencyWeightingControls.
  - `space5`.
  - AudioInputDeviceControls.
  - `space5`.
  - CalibrationProfileControls.
  - Optional `space5` + OctaveCalibrationControls, jos valitulla profililla octaveBandOffsets ei ole tyhja.
- Mic sensitivity:
  - Header row SpaceBetween.
  - Label `bodyLg`.
  - Value `dataMd`, format `+/-X.X dB`.
  - Slider range tulee `UserPreferenceDefaults`.
  - Helper `bodyMd`.
- Frequency weighting:
  - Label `bodyLg`.
  - Spacer `space2`.
  - FlowRow gap `space2 x space2`.
  - WeightingType-chipit `DbCheckChip`.
- Audio input:
  - `CalibrationControlGroup`, gap `space3`.
  - Empty state `bodyMd`.
  - Device rows erotetaan `HorizontalDivider(outlineVariant)`.
  - Row role `RadioButton`, vertical padding `space2`, gap `space2`.
  - RadioButton + text column + optional trailing.
  - Title `bodyMd` SemiBold, subtitle `labelMd`.
- Calibration profiles:
  - Add button tertiary, height `space12`.
  - Rows kuten audio input.
  - Row trailing: edit ja delete IconButton.
  - Delete disabled, jos profile.canDelete false.
  - Error message `bodyMd` error-varilla.
- Octave calibration:
  - Reset IconButton trailing, enabled vain custom offseteilla.
  - Jokainen band slider erotetaan dividerilla.
  - Band slider padding vertical `space2`, gap `space1`.
  - Label row SpaceBetween, band label `bodyMd`, offset `dataMd`.
  - Slider range `CalibrationOffsetPolicy.MIN_OFFSET_DB..MAX_OFFSET_DB`.
- Profile editor dialog:
  - AlertDialog.
  - OutlinedTextField single line.
  - Leading Add icon.
  - Focused border primary alpha `0.3`.
  - Unfocused border `ghostBorder`.
  - Save enabled vain trimmed name nonblank.
- Delete dialog:
  - Confirm TextButton error-varilla.

### 11.4 NoiseNotificationsSection

- Section title `labelMd`, spacer `12dp`.
- Sisalto `DbCheckCard` full width.
- Sisainen Column gap `12dp`.
- Jarjestys:
  - Exposure alerts toggle.
  - Peak warnings toggle.
  - Audible alarm controls.
  - TTS risk -puhekehotteen controls.
  - Passive monitoring controls.
  - Notification threshold control.
  - Notification schedule control.
- Audible alarm:
  - ProLockOverlay.
  - Inner Column gap `8dp`.
  - Toggle row.
  - Preview secondary button full width, height `space12`.
- TTS risk -puhekehote:
  - ProLockOverlay around toggle row.
- Passive monitoring:
  - Column gap `space2`.
  - Description row.
  - Disclosure `bodyMd`, warning-varilla.
  - Summary `labelMd`, `onSurfaceVariant`.
  - Error `bodyMd`, error-varilla.
  - Start/stop secondary button full width, height `space12`.
- Threshold:
  - Title `bodyLg`.
  - Slider integer-arvoilla.
  - Min/default/max labels row SpaceBetween, style `labelSm`.
- Schedule:
  - Column gap `12dp`.
  - Title `bodyLg`, description `bodyMd`, summary `labelMd` primary.
  - Day chips:
    - Column gap `8dp`.
    - 4 chipia per row.
    - Row gap `8dp`.
    - Chip horizontal padding `10dp`.
  - Start/end hour sliders:
    - Slider range `0..23`.
    - Steps `22`.
    - Value label sisaltaa kellonajan `HH:mm`.

### 11.5 HealthSyncSection

- Section title `labelMd`, spacer `space3`.
- `HealthSyncCard` on `DbCheckCard` full width, Column gap `space4`.
- Status row:
  - SpaceBetween.
  - Text column gap `space1`.
  - Title `bodyLg`, status `bodyMd`.
  - Install/Manage secondary button height `space12`.
- Noise sync toggle on kaikille saatavilla, enabled jos Health Connect available.
- Heart rate overlay toggle on ProLockOverlayn sisalla Free-kayttajalle.
- Error message sectionin alla spacer `space2`, style `bodyMd`, error-varilla.
- Permission dialog:
  - AlertDialog.
  - FavoriteBorder icon primary.
  - Title `headlineMd`.
  - Body `bodyMd`, `onSurfaceVariant`.
  - Continue/Cancel TextButtons.

### 11.6 DataExportSection

- Section title `labelMd`, spacer `space3`.
- CSV export:
  - ProLockOverlay locked jos !Pro.
  - `SettingsActionCard`.
  - Secondary button height `48dp`.
  - Exporting-tilassa disabled ja teksti preparing.
- CSV message/error:
  - Spacer `space3`.
  - `bodyMd`, success tai error.
- WAV recording default:
  - Spacer `space4` CSV-osioon.
  - ProLockOverlay locked jos !Pro.
  - `SettingsCardColumn` gap `space3`.
  - Description row + toggle.
  - Privacy warning `bodyMd`, warning-varilla.
- Backup:
  - Spacer `space4`.
  - `SettingsCardColumn`.
  - Description row.
  - Create backup secondary button full width, height `space12`.
  - Empty backups `bodyMd`, `onSurfaceVariant`.
  - Backup list rows gap `space2`; rivien valissa divider `outlineVariant`.
  - Backup row title `bodyMd` SemiBold, subtitle `labelMd`.
  - Restore TextButton.
- Clear history:
  - Spacer `space4`.
  - `SettingsActionCard`.
  - Button disabled clearing-tilassa.
- Restore ja clear history dialogit:
  - AlertDialog.
  - Confirm TextButton error-varilla.
  - Confirm disabled, jos toiminto kaynnissa.

### 11.7 DisplayAndFeaturesSection

- Section title `labelMd`, spacer `space3`.
- DisplayAppearanceCard:
  - `SettingsCardColumn`.
  - Title `bodyLg`.
  - Chip groups:
    - ThemeMode entries.
    - WaveformStyle entries.
    - MeterRefreshRate entries.
  - Group label `bodyLg`.
  - Optional helper `bodyMd`, spacer `space1`.
  - Chip row FlowRow gap `space2 x space2`.
- FeatureTogglesCard:
  - ProLockOverlay locked jos !Pro.
  - `SettingsCardColumn`.
  - Title `bodyLg`.
  - Toggles: technical metadata, dosimeter card, sound detection, sleep card.
- VoiceBaselineCard:
  - ProLockOverlay.
  - `SettingsCardColumn`.
  - Title `bodyLg`.
  - Subtitle `bodyMd`.
  - Current baseline `labelMd`, primary.
  - Secondary button full width, height `space12`.
- LockscreenMeterSection:
  - Jos showTitle false, ProLockOverlay + `DbCheckCard`.
  - Controls Column gap `space3`.
  - Toggle lockscreen meter.
  - Toggle public visibility, enabled vain kun lockscreen meter enabled.
  - Warning `bodyMd`.

### 11.8 ProUpsellCard

- `DbCheckCard` full width.
- Border `1dp` signatureGradient, shape `24dp`.
- Title `headlineMd`.
- Spacer `8dp`.
- Subtitle `bodyMd`.
- Spacer `16dp`.
- Primary purchase button height `48dp`.
- Purchase/success/error messages `bodyMd`, ylapuolella `12dp`.
- Debug force-free row, jos debug:
  - Spacer `16dp`.
  - Row SpaceBetween.
  - Text column title `bodyLg`, subtitle `bodyMd`.
  - Toggle.

## 12. Sleep setup

- Kayttaa `DbCheckSetupScaffold`.
- Route ohjaa locked-kayttajan upgradeen heti, jos availability on Locked.
- Keep screen awake effect on paalla vain, jos `isRecording && keepAwakeEnabled`.
- Content vertical arrangement `spacedBy(space4)`.
- Header:
  - Phase/window title `labelMd`, primary.
  - Title `headlineLg`.
  - Description `bodyLg`, `onSurfaceVariant`.
- Duration card:
  - `DbCheckCard`.
  - Column gap `space3`.
  - Card text title `headlineMd`, subtitle `bodyMd`, textien gap `space1`.
  - FlowRow gap `space2 x space2`.
  - Duration chipit 6h, 8h, 10h.
- Keep awake card:
  - `DbCheckCard`.
  - Row gap `space3`, centerVertically.
  - Text column weight `1`.
  - Toggle trailing.
- Recording action card:
  - `DbCheckCard`.
  - Column gap `space3`.
  - Error `bodyMd` error-varilla.
  - Button full width.
  - Recording-tilassa style Secondary, muuten Primary.
- Notes card:
  - `DbCheckCard`.
  - Column gap `space3`.
  - Privacy note `bodyMd`.
  - Battery note `bodyMd`, warning-varilla.
- Lopussa spacer `space8`.

## 13. Hearing test

### 13.1 Hearing setup ja recovery setup

- Molemmat kayttavat `DbCheckSetupScaffold`.
- Phase label `labelMd`, primary.
- Phase labelin jalkeen `space2`.
- Title `headlineLg`.
- Title jalkeen `space3`.
- Description `bodyLg`, `onSurfaceVariant`.
- Ennen checklistia `space8`.
- Checklist:
  - Column gap `space4`.
  - Item row gap `16dp`, verticalAlignment Top.
  - Icon circle `48dp`, background `surfaceContainerHigh`.
  - Icon `24dp`, vari primary.
  - Item title `bodyLg`.
  - Title ja description valissa `4dp`.
  - Description `bodyMd`, `onSurfaceVariant`.
- Checklistin jalkeen `space16`.
- CTA button full width, height `56dp`.
- Lopussa `space8`.

### 13.2 Hearing active

- Root `BoxWithConstraints`, background `material.background`.
- Sisalto Column fillMaxSize, horizontal padding `20dp`, centerHorizontally.
- Korkeus alle `640dp` -> verticalScroll, muuten fixed layout.
- Top spacer `space10`.
- Phase indicator `labelMd`, `onSurfaceVariant`.
- Spacer `space3`.
- LinearProgressIndicator:
  - Full width.
  - Height `6dp`.
  - Clip `RoundedCornerShape(8.dp)`.
  - Color primary.
  - Track `surfaceContainerHigh`.
- Spacer `space4`.
- Ear indicator `labelLg`, primary.
- Spacer `space16`.
- Frequency circle:
  - Size `200dp`.
  - Shape Circle.
  - Background `surfaceContainerHigh`.
  - Inner label `labelMd`, `onSurfaceVariant`.
  - Frequency `displayMd`.
- Spacer `space8`.
- Instruction `bodyLg`, centered, horizontal padding `20dp`.
- Scrollable tilassa spacer `space8`; muuten `Spacer(weight(1f))`.
- Error `bodyMd`, error-varilla, centered, horizontal padding `20dp`, jalkeen `space3`.
- Retry button primary height `56dp`, jos canRetrySave.
- Response buttons Column gap `space3`:
  - I hear it primary, height `56dp`.
  - I do not hear it secondary, height `56dp`.
- Bottom spacer `space8`.

### 13.3 Hearing results

- Loading:
  - Full screen background.
  - Padding horizontal `20dp`.
  - Centered `bodyLg`, `onSurfaceVariant`.
- Locked/missing/error:
  - Full screen background.
  - Padding horizontal `20dp`.
  - Centered Column.
  - Title `headlineLg`, centered.
  - Optional message `bodyLg`, centered, top spacer `space3`.
  - CTA full width height `56dp`, top spacer `space8`.
- Loaded:
  - Full screen background.
  - VerticalScroll.
  - Padding horizontal `20dp`.
  - CenterHorizontally.
  - Top spacer `space10`.
- Header:
  - CheckCircle icon `48dp`, success.
  - Spacer `space4`.
  - Analysis complete label `labelMd`, `onSurfaceVariant`.
  - Spacer `space2`.
  - Rating `headlineLg`; color success/good primary/fair warning/poor error.
  - Summary `labelMd`, `onSurfaceVariant`, centered.
- Audiogram card:
  - `DbCheckCard`.
  - Title `labelMd`.
  - Spacer `12dp`.
  - Legend row gap `16dp`.
  - Legend dot `12dp`, label `labelSm`.
  - Spacer `8dp`.
  - Chart height `150dp`.
  - Left line primary, right line secondary.
  - Path stroke `3f`, cap round.
  - Points radius `6f`.
  - X-axis uses log2 frequency scale.
  - Y-axis threshold scale min `-60f` to `0f`.
- KeyMetricsCard:
  - `DbCheckCard`.
  - Column gap `12dp`.
  - Title `labelMd`.
  - Metric rows SpaceBetween, label `bodyMd`, value `dataMd`.
  - Estimated note `labelSm`.
- Disclaimer:
  - `bodyMd`, `onSurfaceVariant`, centered, horizontal padding `12dp`.
- Share error:
  - `bodyMd`, error, centered, full width, horizontal padding `12dp`, bottom spacer `space3`.
- Actions:
  - Save primary full width height `56dp`.
  - Spacer `space3`.
  - Share secondary full width height `56dp`.
- Bottom spacer `space8`.

## 14. Tinnitus pitch matcher

- Kayttaa `DbCheckSetupScaffold`.
- Koko content on ProLockOverlayn sisalla.
- Header:
  - Phase `labelMd`, primary.
  - Spacer `space2`.
  - Title `headlineLg`.
  - Spacer `space3`.
  - Description `bodyLg`, `onSurfaceVariant`.
  - Spacer `space6`.
- Ear selector:
  - Row full width.
  - Gap `space2`.
  - Kaksi `AssistChip`ia equal weights.
  - Leading icon Headphones.
  - Valittu korva on disabled samassa chipissa.
- Spacer `space4`.
- Card:
  - `DbCheckCard` full width.
  - Column gap `space3`.
  - Current frequency label `labelMd`, `onSurfaceVariant`.
  - Slider range `TinnitusPitchPolicy.MIN_FREQUENCY_HZ..MAX_FREQUENCY_HZ`.
  - Slider steps perustuvat `FREQUENCY_STEP_HZ`.
  - Saved summary `labelSm`.
  - Error `labelSm`, error.
  - Save message `labelSm`, primary.
  - Disclaimer `labelSm`, `onSurfaceVariant`.
  - Action row gap `space3`.
  - Preview secondary button weight `1`, height `48dp`.
  - Save primary button weight `1`, height `48dp`.
- Lopussa setup scaffoldissa spacer `space8`.

## 15. Ambient sound playback

- Kayttaa `DbCheckSetupScaffold`.
- Koko content on ProLockOverlayn sisalla.
- Header:
  - Phase `labelMd`, primary.
  - Spacer `space2`.
  - Title `headlineLg`.
  - Spacer `space3`.
  - Description `bodyLg`, `onSurfaceVariant`.
  - Spacer `space6`.
- Card:
  - `DbCheckCard` full width.
  - Inner Column gap `space4`.
- Preset selector:
  - Column gap `space2`.
  - Label `labelMd`, `onSurfaceVariant`.
  - Row full width, gap `space2`.
  - Preset chipit equal weights.
  - Chip leading icon GraphicEq.
  - Chip horizontal padding `8dp`.
- Volume:
  - Column gap `space2`.
  - Label `labelMd`.
  - Slider range `AmbientSoundPolicy.MIN_VOLUME..MAX_VOLUME`.
  - Steps `18`.
  - Value label prosentteina.
- Timer:
  - Column gap `space2`.
  - Label `labelMd`.
  - Timer chipit equal weights.
  - Chip horizontal padding `8dp`.
- Error message `labelSm`, error.
- Button row:
  - Gap `space3`.
  - Play primary weight `1`, height `48dp`.
  - Stop secondary weight `1`, height `48dp`.
- Lopussa setup scaffoldissa spacer `space8`.

## 16. Camera overlay

### 16.1 Ruudulla oleva overlay

- Route on fullscreen, ei top-level-navigationia.
- Root `Box` fillMaxSize, background `material.background`.
- Permission granted:
  - CameraX `PreviewView` fillMaxSize, `ScaleType.FILL_CENTER`.
  - Preview sitoo back cameraan `Preview`, `ImageCapture` ja `VideoCapture<Recorder>`.
  - Video quality selector: `Quality.HD`, fallback higherQualityOrLowerThan HD.
- Permission not granted / denied / permanently denied:
  - Centered content max width `320dp`.
  - Padding `space6`.
  - Icon PhotoCamera `48dp`, primary.
  - Title `headlineMd`, top padding `space6`, centered.
  - Description `bodyMd`, top padding `space3`, centered.
  - Button primary, top padding `space8`.
- Close button:
  - TopStart.
  - Padding `space3`.
  - Size `48dp`.
  - Circle.
  - Background `material.surface.copy(alpha = 0.72f)`.
  - Close icon `onSurface`.
- Static preview fallback:
  - Background `0xFF0B1114`.
  - Band `0xFF26343D` alpha `0.58`, top `14%`, height `24%`.
  - Primary circle alpha `0.14`, radius `28%` min dimension, center `72%/34%`.
  - Secondary circle alpha `0.10`, radius `34%`, center `22%/72%`.
  - Grid color `0xFFF2F5F1` alpha `0.14`, kaksi pysty- ja vaakaviivaa, stroke `1dp`.
- Preview unavailable:
  - Background `0xFF0B1114`.
  - Centered Column max width `320dp`, padding `space6`.
  - Icon PhotoCamera `48dp`, primaryContainer.
  - Title `headlineMd`, overlay text, top `space6`.
  - Description `bodyMd`, overlay secondary, top `space3`.

### 16.2 Readout ja capture controls

- Readout:
  - Align BottomStart.
  - Full width.
  - Padding `space6`.
  - Semantics sisaltaa status, dB, level label, timestamp.
  - Status `labelMd`, primaryContainer, SemiBold.
  - dB text `displayMd`, overlay text.
  - Level label `bodyMd`, overlay secondary.
  - Timestamp `labelMd`, overlay secondary, top padding `space1`.
- Capture controls:
  - Align BottomEnd.
  - Padding `space6`.
  - Width max `240dp`.
  - Horizontal alignment End.
  - Privacy text `labelMd`, overlay secondary, textAlign End.
  - Capture/video error textit `labelMd`, overlay text, max width `220dp`, top `space2`.
  - Video button top `space3`.
  - Photo button top `space3`.
- Capture buttons:
  - Size `64dp`.
  - Circle.
  - Enabled background primary.
  - Disabled background `surface.copy(alpha = 0.56f)`.
  - Video recording background error.
  - Photo icon `PhotoCamera`, video icon `Videocam` tai `Stop`.
  - Enabled icon onPrimary tai onError, disabled onSurfaceVariant.

### 16.3 Jaettuun kuvaan poltettu overlay

- Raw JPG tallennetaan ensin cacheen, output PNG julkaistaan FileProviderin kautta.
- Output bitmap on sama width/height kuin source.
- Burn-in overlay:
  - `minDimension = min(width, height)`.
  - `scale = (minDimension / 360f).coerceAtLeast(0.8f)`.
  - Margin `24f * scale`.
  - Padding `20f * scale`.
  - Panel width `min(source.width - margin*2, 320f*scale)`.
  - Panel height `144f * scale`.
  - Panel sijoittuu vasempaan alakulmaan.
  - Source pikselit panelin alla tummennetaan retain-kertoimella `0.28f`.
  - Panel fill `0xB0000000`.
  - Corner radius `18f * scale`.
- Burn-in text:
  - Status `0xFFF7F7F7`, size `18f * scale`, bold.
  - dB `0xFFFFFFFF`, size `46f * scale`, bold.
  - Level label `0xFFE2E5E1`, size `20f * scale`.
  - Timestamp `0xFFC8D0CA`, size `16f * scale`.
  - Text x = rect.left + padding.
  - Y-positions: top + 34, 86, 114, 136 scaled.

## 17. Health Connect disclosure activity

- Erillinen `ComponentActivity`.
- Kayttaa `enableEdgeToEdge()`.
- Teema `DbCheckTheme(darkTheme = isSystemInDarkTheme())`.
- Column:
  - FillMaxSize.
  - `statusBarsPadding()`.
  - VerticalScroll.
  - Padding horizontal `24dp`, vertical `space8`.
  - Vertical arrangement `spacedBy(space5)`.
- Title `headlineMd`.
- Intro/footer `bodyMd`, `onSurfaceVariant`.
- Jokainen disclosure item:
  - Column gap `8dp`.
  - Title `bodyLg`.
  - Body `bodyMd`, `onSurfaceVariant`.
- Ennen footeria spacer `space2`.

## 18. App widget

- Toteutus on Glance AppWidget.
- Widget info:
  - minWidth `110dp`
  - minHeight `40dp`
  - minResizeWidth `110dp`
  - minResizeHeight `40dp`
  - maxResizeWidth `250dp`
  - maxResizeHeight `110dp`
  - resizeMode horizontal|vertical
  - updatePeriodMillis `1800000`
  - category home_screen
- WidgetSurface:
  - FillMaxSize.
  - Background `GlanceTheme.colors.widgetBackground`.
  - Padding `16dp`.
  - Click opens `MainActivity`.
  - Vertical alignment CenterVertically.
  - Horizontal alignment Start, paitsi Pro locked -tilassa CenterHorizontally.
- Brand label:
  - Font size `11sp`.
  - FontWeight Medium.
  - Color `onSurfaceVariant`.
- Session state:
  - Brand label, spacer `4dp`.
  - Avg dB number `28sp`, Bold, color `onSurface`.
  - Unit `14sp`, color `onSurfaceVariant`, gap `4dp`.
  - Noise level label `11sp`, Medium, color primary, top `2dp`.
  - Time ago `10sp`, color `onSurfaceVariant`, top `2dp`.
- Empty/Error:
  - Brand label, spacer `8dp`.
  - Title `14sp`, Medium, `onSurface`.
  - Subtitle `11sp`, `onSurfaceVariant`, spacer `2dp`.
- Pro locked:
  - Lock emoji `20sp`.
  - Spacer `4dp`.
  - Title `13sp`, Bold.
  - Spacer `2dp`.
  - Upgrade copy `11sp`, `onSurfaceVariant`.

## 19. Notification UI

### 19.1 Kanavat ja perusnotificationit

- Measurement channel:
  - ID `measurement_channel`.
  - Importance LOW.
  - Badge off.
- Alerts channel:
  - ID `alerts_channel`.
  - Importance HIGH.
- Ambient playback channel:
  - ID `ambient_playback_channel`.
  - Importance LOW.
  - Badge off.
- Measurement notification on ongoing ja silent.
- Measurement notificationissa on stop action `ic_notification_stop`.
- Ambient sound notification on ongoing, silent ja private visibility.
- Alert-notificationit ovat high priority ja autoCancel.

### 19.2 Custom measurement notification colors

- Background `#0E0E0E`.
- Surface `#201F1F`.
- Text primary `#E8E4E0`.
- Text secondary `#ADAAAA`.
- Dot green `#8EA58E`.
- Dot yellow `#C9A24D`.
- Dot red `#E07A7A`.

### 19.3 Collapsed measurement layout

- Root vertical LinearLayout.
- Background notification background.
- Padding `12dp`.
- Big value:
  - Font Space Grotesk Bold.
  - Size `48sp`.
  - `includeFontPadding=false`.
- Peak/duration row:
  - Manrope Medium.
  - Size `14sp`.
  - Dot `12dp`.

### 19.4 Expanded measurement layout

- Root padding `12dp`.
- Session name:
  - Manrope SemiBold.
  - Size `13sp`.
- Row top padding `6dp`.
- Big value `48sp`.
- Right panel:
  - Width `92dp`.
  - Background notification surface.
  - Padding `10dp`.
  - Dot `12dp`.
  - Noise label top padding `6dp`, size `12sp`.
- Dot drawable valitaan SAFE/ELEVATED/DANGEROUS mukaan green/yellow/red.

## 20. Share-kuvat ja PDF

### 20.1 Meter text share

- Meter share on `Intent.ACTION_SEND`, type `text/plain`.
- Se sisaltaa avg, equivalent level label, peak ja duration -tekstit.
- Ei kuvaa tai custom layoutia.

### 20.2 Hearing test share card

- Bitmap `1080 x 1080`.
- Background `0xFF0E0E0E`.
- Title:
  - X/Y `80/200`.
  - Color `0xFFE8E4E0`.
  - Text size `48f`.
  - Typeface `sans-serif-medium`.
- Score:
  - X/Y `80/500`.
  - Color `0xFFF7F7F7`.
  - Text size `180f`.
  - Typeface `sans-serif` Bold.
- Rating:
  - X/Y `80/600`.
  - Color `0xFFADAAAA`.
  - Text size `72f`.
- Disclaimer:
  - X/Y `80/760`.
  - MaxRight `1000`.
  - Color `0xFFADAAAA`.
  - Text size `30f`.
  - Ellipsized if liian levea.
- Subtitle:
  - X/Y `80/900`.
  - Color `0xFF5F5F5F`.
  - Text size `36f`.

### 20.3 Session report share card

- Bitmap `1080 x 1080`.
- Background `0xFFF9F9F9`.
- Title paint:
  - Color `0xFF2F3334`.
  - Text size `52f`.
  - Bold.
- Label paint:
  - Color `0xFF5C6060`.
  - Text size `28f`.
- Value paint:
  - Color `0xFF111111`.
  - Text size `132f`.
  - Bold.
- Metric paint:
  - Color `0xFF2F3334`.
  - Text size `44f`.
  - Bold.
- Metric cards:
  - Fill `0xFFECEEEE`.
  - Round rect radius `28f`.
  - Rectit:
    - LCpeak: `80,560,500,740`
    - TWA: `580,560,1000,740`
    - Dose: `80,780,500,960`
    - Duration: `580,780,1000,960`
  - Label x `left + 32`, y `top + 54`.
  - Value x `left + 32`, y `top + 128`, maxRight `right - 32`.
- Main value:
  - LAeq x/y `80/420`.
  - Equivalent level label x/y `86/470`.
- Date/tag/title:
  - Report title x/y `80/130`.
  - Share title x/y `80/182`.
  - Tags x/y `80/224`, date x/y `80/266` jos tagit ovat olemassa.
  - Muuten date x/y `80/224`.

### 20.4 PDF report

- PDF on natiivi `PdfDocument`.
- Sivukoko `595 x 842`.
- Marginit ja ankkurit:
  - `PAGE_LEFT = 48f`
  - `PAGE_RIGHT = 547f`
  - `PAGE_TOP = 112f`
  - Footer divider y `780f`
  - Footer text y `804f`
- Perusraportti 5 sivua; heart rate enabled -> 6 sivua.
- Sivut:
  - 1 Session summary.
  - 2 Scientific metrics + report context.
  - 3 Upstream/data availability fields.
  - 4 Time series.
  - 5 Peak events.
  - 6 Heart rate, jos enabled.
- Header:
  - App name x/y `48/46`, brandPaint.
  - Page title x/y `547/46`, right-aligned headerPaint.
  - Divider y `62`.
- Footer:
  - Divider y `780`.
  - Generated footer left.
  - Page number right.
- KPI grid:
  - KPI width `239.5f`.
  - Column gap `20f`.
  - Row spacing `130f`.
  - Card height `104f`.
  - Card radius `14f`.
  - Label x `left + 18`, y `top + 32`.
  - Value x `left + 18`, y `top + 78`.
- Metric table:
  - Labels x `PAGE_LEFT`.
  - Values x `PAGE_RIGHT`, right aligned.
  - Row step `42f`.
- Note:
  - Rect `PAGE_LEFT, top, PAGE_RIGHT, top + 76`.
  - Radius `14f`.
  - Text x `PAGE_LEFT + 18`, y `top + 44`.
- PDF palette:
  - Primary `rgb(70,105,6)`.
  - Text `rgb(47,51,52)`.
  - TextMuted `rgb(92,96,96)`.
  - Card `rgb(236,238,238)`.
  - Note `rgb(243,244,244)`.
  - Divider `rgb(216,219,220)`.
- PDF paints:
  - brand `18f`, primary, Space Grotesk Bold.
  - header `12f`, muted, Manrope SemiBold, right aligned.
  - title `28f`, text, Manrope SemiBold.
  - section `18f`, text, Manrope SemiBold.
  - body `12f`, muted, Manrope Regular.
  - footer `8f`, muted.
  - KPI label `11f`, muted, Space Grotesk SemiBold.
  - KPI value `30f`, text, Space Grotesk Bold.
  - table label `13f`, muted.
  - table value `15f`, text, Space Grotesk SemiBold, right aligned.
  - axis `10f`, muted.
- PDF chart style:
  - dB line/point `rgb(70,105,6)`, line stroke `3f`, cap/join round.
  - Heart rate line/point `rgb(204,119,0)`, line stroke `3f`.
  - Grid `rgb(220,224,224)`, stroke `1f`.
  - Border `rgb(175,178,179)`, stroke `1f`.
  - Threshold `rgb(204,119,0)`, stroke `2f`, dash `[8f, 6f]`.
  - Axis label `rgb(92,96,96)`, size `10f`.
  - Grid line count `5`.
  - Point radius `4f`.

## 21. Accessibility and semantics

- Chips expose selected/not selected state descriptions.
- Meter mode chipit override contentDescriptionilla selected/locked-stateille.
- Live chartilla on contentDescription, joka tiivistaa mittausdatan.
- Camera overlay readout contentDescription sisaltaa status-, dB-, level- ja timestamp-tiedot.
- Camera static previewlla on contentDescription.
- Audiogram chartilla on contentDescription, joka listaa korvakohtaiset thresholdit.
- Notification schedule day chipit asettavat contentDescriptionin ja stateDescriptionin aktiiviselle/inaktiiviselle paivalle.
- Calibration octave sliderit asettavat contentDescriptionin band labelin ja offsetin perusteella.
- SessionCard on clickable role Button.
- Bottom nav itemit ovat role Tab.
- Calibration selectable rows ovat role RadioButton.
- Emoji-valinta sheetissa on role RadioButton.

## 22. Koodilahteet

Tama spec perustuu seuraaviin UI-lahteisiin:

- `app/src/main/java/com/dbcheck/app/MainActivity.kt`
- `app/src/main/java/com/dbcheck/app/HealthConnectPermissionDisclosureActivity.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/Color.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/Type.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/Spacing.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/Shape.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/Gradient.kt`
- `app/src/main/java/com/dbcheck/app/ui/theme/AmbientShadow.kt`
- `app/src/main/java/com/dbcheck/app/ui/components/*`
- `app/src/main/java/com/dbcheck/app/ui/navigation/*`
- `app/src/main/java/com/dbcheck/app/ui/meter/*`
- `app/src/main/java/com/dbcheck/app/ui/analytics/*`
- `app/src/main/java/com/dbcheck/app/ui/history/*`
- `app/src/main/java/com/dbcheck/app/ui/settings/*`
- `app/src/main/java/com/dbcheck/app/ui/sleep/*`
- `app/src/main/java/com/dbcheck/app/ui/hearingtest/*`
- `app/src/main/java/com/dbcheck/app/ui/tinnitus/*`
- `app/src/main/java/com/dbcheck/app/ui/ambient/*`
- `app/src/main/java/com/dbcheck/app/ui/camera/*`
- `app/src/main/java/com/dbcheck/app/widget/DbCheckWidget.kt`
- `app/src/main/java/com/dbcheck/app/service/NotificationHelper.kt`
- `app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt`
- `app/src/main/java/com/dbcheck/app/util/ExportPdfReportUseCase.kt`
- `app/src/main/java/com/dbcheck/app/util/PdfChartRenderer.kt`
- `app/src/main/res/layout/notification_measurement.xml`
- `app/src/main/res/layout/notification_measurement_expanded.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/xml/widget_info.xml`
