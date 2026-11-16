# Audyt wydajności Jetpack Compose — CloudStream TV

Zakres: pakiet `app/src/main/java/com/lagradost/cloudstream3/tv` (UI na Android TV, `androidx.tv.material3`)
oraz mosty, z których korzysta (`compat/`, `DataStoreHelper`, `SubtitlesFragment.setSubtitleViewStyle`).
Analiza statyczna kodu; **nie** uruchamiano aplikacji, trace'ów, Layout Inspectora ani testów.
Kod aplikacji nie był modyfikowany.

Ścieżki w raporcie są względne do `app/src/main/java/com/lagradost/cloudstream3/tv/` (chyba że podano inaczej).
Numery linii są przybliżone (stan gałęzi w dniu audytu).

## Kontekst architektury (istotny dla oceny)

- Kotlin 2.4 (`gradle/libs.versions.toml`) → **strong skipping włączony**: lambdy i referencje do funkcji
  są memoizowane automatycznie, niestabilne parametry porównywane przez `===`. Raport nie rekomenduje więc
  `remember {}` wokół lambd ani dopisywania `@Stable`.
- Stan ekranów: `StateFlow` z ViewModeli, kolekcje w stanie to w dużej mierze `PersistentList` (dobrze).
- Player: `PlayerSessionController` (ExoPlayer) tworzony raz w `PlayerRoute`, stan overlayu w
  `PlayerOverlayStateHolder` (pola `mutableStateOf`), pozycja odtwarzania w `produceState` (500 ms).
- Brak skonfigurowanych raportów kompilatora Compose (`composeCompiler { reportsDestination / metricsDestination }`
  nie występuje w `app/build.gradle.kts`).

Legenda kategorii: **[1]** fazy Compose, **[2]** efekty uboczne, **[3]** klucze list,
**[4]** rekompozycje / przekazywanie stanu, **[5]** główny wątek.

---

## Problemy — priorytet High

### H1. Warstwa napisów ponownie stosuje cały styl (z odczytem pliku fontu) przy każdym pokazaniu/ukryciu kontrolek

- **Problem:** `update` w `AndroidView` warstwy napisów wywołuje `applyLegacySubtitleStyle()`, która woła
  `setSubtitleViewStyle()` → `fromSaveToStyle()` → `Typeface.createFromFile(File(path))` (gdy użytkownik ustawił
  własny font) oraz `setStyle`, `setPadding`, `setFixedTextSize` (każde wymusza `requestLayout` widoku).
  `update` wykonuje się przy każdej rekompozycji `TvPlayerSubtitleLayer`, czyli przy każdej zmianie `controlsVisible`.
- **Lokalizacja:** `presentation/screens/player/video/PlayerVideoSurface.kt`, `TvPlayerSubtitleLayer`, l. ~37–55 i ~145–154;
  `ui/subtitles/SubtitlesFragment.kt` (poza `tv/`), `fromSaveToStyle`, l. ~266–296.
- **Kategoria:** [5] główny wątek, [2] efekty (praca w `update` zamiast jednorazowo).
- **Wpływ:** synchroniczny odczyt/parsowanie pliku fontu i relayout `SubtitleView` w tej samej klatce, w której
  startuje animacja `alpha` overlayu — dokładnie w momencie, gdy użytkownik naciska klawisz na pilocie.
  Dodatkowo ten sam koszt przy każdym `onTracksChanged`.
- **Przyczyna:** jedyna rzecz zależna od `controlsVisible` to `translationY`, a reszta stylu jest stała w trakcie sesji.
- **Rozwiązanie:** w `update` zmieniać tylko `translationY` (tanie, bez layoutu); pełny styl nakładać raz
  (w `factory`) i po zmianie stylu (np. przy `onTracksChanged`, jeśli faktycznie potrzebne). Ewentualnie
  zcache'ować `Typeface` po ścieżce w moście `tv/compat` (zmiana w `SubtitlesFragment` jest poza `tv/` — wymaga zgody).
- **Przykład:**
  ```kotlin
  // obecnie
  update = { view ->
      applyLegacySubtitleStyle(view, controlsVisible, subtitleControlsVisibleOffsetPx) // font I/O + setStyle + padding
      if (subtitleView !== view) subtitleView = view
  }
  // proponowane
  update = { view ->
      view.translationY = resolvedSubtitleTranslationY(
          baseTranslationYPx = -CustomDecoder.style.elevation.toPx.toFloat(),
          controlsVisibleOffsetPx = subtitleControlsVisibleOffsetPx,
          controlsVisible = controlsVisible,
      )
  }
  ```
- **Priorytet:** High
- **Pewność:** Potwierdzony problem (koszt I/O zależy od tego, czy ustawiono własny font — warto zmierzyć w trace).

### H2. Szybko zmieniający się stan overlayu czytany w korzeniu ekranu playera

- **Problem:** `PlayerPlaybackContent` odczytuje w Composition `overlayState.seekHintDeltaMs`,
  `controlsVisible`, `showBufferingOverlay` (→ `isPlaying`, `playerPlaybackState`, `playerWantsToPlay`),
  `upNextVisible`, `panelCloseCount`, `sourceErrorDialogEffect`. Każda zmiana któregokolwiek pola rekomponuje
  cały korzeń treści playera, w tym ponowne wywołanie `rememberPlayerBottomControlBarConfig(...)` (skan list
  ścieżek, `stringResource`, formatowanie), lambdy `onPreviewKeyEvent` itd. Analogicznie `PlayerPlaybackScreen`
  czyta `isPlaying/playerWantsToPlay/playerPlaybackState` tylko po to, by policzyć argument `PlayerKeepScreenOnEffect`.
- **Lokalizacja:** `presentation/screens/player/PlayerPlaybackContent.kt`, `PlayerPlaybackContent`, l. ~69–74, 145–155,
  160–162, 217–222; `presentation/screens/player/PlayerPlaybackScreen.kt`, `PlayerPlaybackScreen`, l. ~83–88, 143–149.
- **Kategoria:** [1] fazy Compose, [4] zakres odczytu stanu.
- **Wpływ:** przytrzymanie ←/→ przy ukrytych kontrolkach (seek + „+10 s”) generuje rekompozycję całego
  ekranu playera dla każdego powtórzenia klawisza (~20/s). Przejścia BUFFERING↔READY oraz play/pause
  rekomponują dwa najwyższe poziomy ekranu playera.
- **Przyczyna:** stan jest odczytywany tam, gdzie tylko przekazuje się go niżej.
- **Rozwiązanie:** przekazać `overlayState` (stabilny holder) lub lambdę do komponentów, które faktycznie
  używają wartości, i czytać ją tam: `PlayerSeekHint(deltaMs = { overlayState.seekHintDeltaMs }, …)`,
  `BufferingOverlay(visible = { overlayState.showBufferingOverlay })`, `PlayerKeepScreenOnEffect(overlayState)`.
- **Przykład:**
  ```kotlin
  // obecnie (PlayerPlaybackContent)
  PlayerSeekHint(deltaMs = overlayState.seekHintDeltaMs, …)
  if (overlayState.showBufferingOverlay) { BufferingOverlay() }
  // proponowane
  PlayerSeekHint(overlayState = overlayState, …)   // odczyt deltaMs wewnątrz PlayerSeekHint
  BufferingOverlayHost(overlayState)              // if (overlayState.showBufferingOverlay) BufferingOverlay()

  // obecnie (PlayerPlaybackScreen)
  PlayerKeepScreenOnEffect(keepScreenOn = shouldKeepPlaybackScreenOn(overlayState.isPlaying, …))
  // proponowane
  PlayerKeepScreenOnEffect(overlayState)          // wylicza flagę u siebie
  ```
- **Priorytet:** High
- **Pewność:** Potwierdzony problem (odczyty w Composition widoczne w kodzie); skala kosztu — do potwierdzenia
  w Layout Inspectorze (licznik rekompozycji `PlayerPlaybackContent` przy przytrzymanym ←/→).

### H3. Panel boczny (`MenuListSidePanel`) rekomponuje się w całości przy każdym ruchu D-pad

- **Problem:** `focusedItemId` jest `mutableStateOf` odczytywanym w Composition (klucz `remember(...)` dla
  `requestFocusItemId`). Każde przejście fokusu między wierszami zapisuje `focusedItemId`
  (`onRowFocusChanged`), co rekomponuje cały `MenuListSidePanel`: ponowne `items.filter{}.map{}`
  (`focusableItemIds`), `rememberFocusRequesterMap(...)` z nową listą, wyliczenie `requestFocusItemId`,
  ponowne wywołanie `SlidingSidePanel` i zawartości (nowa instancja `focusableItemIds` jest przechwycona w lambdzie
  `panelContent`, więc lambda nie jest memoizowana).
- **Lokalizacja:** `presentation/common/MenuListSidePanel.kt`, `MenuListSidePanel`, l. ~229–253 i ~392–399.
- **Kategoria:** [4] rekompozycje, [1] stan fokusu czytany w Composition.
- **Wpływ:** panele źródeł/napisów/ścieżek są rysowane nad odtwarzanym wideo; każde ↑/↓ przelicza cały panel
  (header, LazyColumn scope, wiersze z niestabilnymi parametrami). Ryzyko jank przy szybkim przewijaniu długich list
  (np. kilkadziesiąt źródeł / napisów online).
- **Przyczyna:** `focusedItemId` służy wyłącznie do decyzji „czy trzeba przywrócić fokus” — nie wpływa na wygląd.
- **Rozwiązanie:** trzymać ostatnio sfokusowane id w zwykłym holderze (np. `remember { arrayOfNulls<String>(1) }`,
  wzorzec już użyty w `FeedRow`) i liczyć `requestFocusItemId` tylko z `visible/contentKey/initialFocusedItemId/
  focusableItemIds`; `focusableItemIds` wyliczać `remember(items, expandedGroups-snapshot)`.
- **Przykład:**
  ```kotlin
  // obecnie
  var focusedItemId by remember(resolvedContentKey) { mutableStateOf<String?>(null) }
  val requestFocusItemId = remember(visible, resolvedContentKey, initialFocusedItemId, focusableItemIds, focusedItemId) { … }
  // proponowane
  val lastFocused = remember(resolvedContentKey) { arrayOfNulls<String>(1) }   // nie wywołuje rekompozycji
  val requestFocusItemId = remember(visible, resolvedContentKey, initialFocusedItemId, focusableItemIds) {
      if (!visible || focusableItemIds.isEmpty()) null
      else resolveAvailableFocusKey(focusableItemIds, lastFocused[0], initialFocusedItemId)
  }
  ```
- **Priorytet:** High
- **Pewność:** Prawdopodobny problem — mechanizm potwierdzony w kodzie, wpływ na klatki wymaga trace'u
  (Layout Inspector: liczniki `MenuListSidePanel` / `PanelRow` przy ↑/↓).

### H4. Każdy widoczny wiersz odcinka rekomponuje się przy każdej zmianie stanu pobierania/obejrzenia dowolnego odcinka

- **Problem:** `resolveDownloadState(episode)` i `resolveWatched(episode)` są wołane wewnątrz `items {}` i czytają
  `episodeDownloadStates` / `episodeWatchedStates` — pojedyncze `mutableStateOf<Map<…>>`. Każdy zapis mapy
  (postęp pobierania, hydratacja okna, status) unieważnia wszystkie widoczne wiersze, a w każdym wierszu
  ponownie wykonuje się mapowanie `toMovieDetailsDownloadActionState`.
- **Lokalizacja:** `presentation/screens/details/DetailsEpisodesTab.kt`, `DetailsEpisodesTab`, l. ~284–300;
  `DetailsEpisodesStateHolder.kt`, l. ~31–32, 62–65, 92–100; `DetailsScreenBody.kt`, l. ~294.
- **Kategoria:** [4] rekompozycje, [3]/[4] mapowanie danych w `items {}` (wbrew `AGENTS.md`).
- **Wpływ:** podczas aktywnego pobierania każdy event postępu rekomponuje ~8–10 wierszy (`EpisodeRow` z obrazem,
  tekstami, przyciskami). Hydracja przy przewijaniu wykonuje dwa zapisy (download + watched) → dwie fale rekompozycji
  wszystkich widocznych wierszy na każdy krok przewijania listy.
- **Przyczyna:** odczyt całej mapy w zakresie elementu listy; brak modelu wiersza z gotowymi wartościami.
- **Rozwiązanie:** budować w state holderze gotową listę modeli wiersza
  (`EpisodeRowUi(episode, watched, downloadState: MovieDetailsDownloadActionState)`) i przekazywać wartości do
  `EpisodeRow`; wiersze z niezmienionymi wartościami zostaną pominięte (strong skipping + `equals`).
  Alternatywa minimalna: łączyć oba zapisy hydratacji w jeden (`Snapshot.withMutableSnapshot {}` lub jedna mapa
  `EpisodeState(download, watched)`).
- **Przykład:**
  ```kotlin
  // obecnie
  items(episodes, key = { it.id }) { episode ->
      EpisodeRow(watched = resolveWatched(episode), downloadState = resolveDownloadState(episode), …)
  }
  // proponowane (holder wystawia gotowe modele)
  items(rows, key = { it.episode.id }) { row ->
      EpisodeRow(episode = row.episode, watched = row.watched, downloadState = row.downloadState, …)
  }
  ```
- **Priorytet:** High
- **Pewność:** Potwierdzony problem (zakres odczytu); częstotliwość zależy od częstotliwości eventów postępu.

---

## Problemy — priorytet Medium

### M1. Ruch fokusu po przyciskach playera rekomponuje cały overlay

- **Problem:** `PlayerOverlay` czyta `barState.lastFocusedControl` (mutableState) w Composition, by przekazać
  `downFocusRequester` do osi czasu. Każde ←/→ po przyciskach zapisuje `lastFocusedControl` (`onFocused`),
  więc rekomponuje się `PlayerOverlay` (tytuł, `bulletJoined`, `PlaybackTimelineSection`).
- **Lokalizacja:** `presentation/screens/player/overlay/PlayerOverlayContent.kt`, `PlayerOverlay`, l. ~117;
  `PlayerBottomControlBar.kt`, l. ~109–200 (`onFocused = { state.lastFocusedControl = … }`).
- **Kategoria:** [1] odczyt w złej fazie, [4].
- **Wpływ:** zbędna rekompozycja overlayu nad wideo przy każdym kroku D-pad w pasku kontrolek.
- **Przyczyna:** wartość potrzebna jest tylko w momencie wyszukiwania fokusu (Down z osi czasu).
- **Rozwiązanie:** przekazać do `PlaybackTimelineSection` `barState` + `barConfig` i rozwiązać requester
  w `Modifier.focusProperties { down = … }` — ta lambda jest wykonywana leniwie, odczyt stanu w niej
  nie rekomponuje.
- **Przykład:**
  ```kotlin
  // obecnie
  downFocusRequester = barState.resolveFocusRequester(barState.lastFocusedControl, barConfig),
  // proponowane (w PlaybackTimeline)
  .focusProperties {
      canFocus = controlsEnabled
      down = barState.resolveFocusRequester(barState.lastFocusedControl, barConfig)
  }
  ```
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem.

### M2. Oś czasu odświeża teksty co 500 ms również przy ukrytym overlayu

- **Problem:** `PlaybackControlsLayer` jest zawsze w kompozycji (ukrywanie przez `alpha = 0`), więc
  `PlaybackTimelineSection` rekomponuje się co 500 ms (teksty `positionMs`/remaining) przez cały seans, mimo że
  nic nie jest widoczne. Dodatkowo zapis postępu (`onPlaybackProgress`) jest „przyklejony” do tego komponentu UI.
- **Lokalizacja:** `presentation/screens/player/overlay/PlayerTimeline.kt`, `PlaybackTimelineSection`, l. ~71–114;
  `TvPlayerPlaybackTimelineState.kt`, l. ~22–61; `PlayerPlaybackControlsLayer.kt`, l. ~38–64.
- **Kategoria:** [1], [4] — praca elementów niewidocznych.
- **Wpływ:** stała (choć mała) rekompozycja + pomiar 2 tekstów co 500 ms podczas całego odtwarzania,
  w tym 2 alokacje stringów (`"%d:%02d".format`) na tick.
- **Przyczyna:** brak przełączenia trybu „hidden” w polling'u; teksty czytają stan w Composition.
- **Rozwiązanie (KISS):** przekazać `controlsVisible` do `rememberPlaybackTimelineUiState` i przy ukrytych
  kontrolkach nie aktualizować `value` (nadal wołać `onPlaybackProgress`), albo zwiększyć interwał do ~1 s.
  Nie wyrzucać overlayu z kompozycji (zgodnie z `AGENTS.md` overlay ma być osobnym poddrzewem animowanym alpha).
- **Przykład:**
  ```kotlin
  // proponowane w produceState
  if (controlsVisible() && (positionMs != value.positionMs || …)) value = PlaybackTimelineUiState(…)
  latestOnPlaybackProgress(positionMs, durationMs)
  ```
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem (wpływ na klatki prawdopodobnie niewielki — sprawdzić w trace).

### M3. Karta „Up next” rekomponuje się co 500 ms przez całą drugą połowę odcinka

- **Problem:** `remainingMs` (z `produceState`) jest czytane w Composition `PlayerUpNextLayer`. Od połowy odcinka
  wartość zmienia się co tick, więc komponent rekomponuje się co 500 ms, choć karta jest niewidoczna
  (`visible` = false aż do ostatnich 8/30 s).
- **Lokalizacja:** `presentation/screens/player/overlay/PlayerTransientLayers.kt`, `PlayerUpNextLayer`, l. ~134–145.
- **Kategoria:** [1], [4].
- **Wpływ:** zbędne rekompozycje w trakcie odtwarzania (mały zakres, ale bez przerwy).
- **Przyczyna:** szybko zmieniające się wejście, rzadko zmieniające się wyjście (`visible`, `seconds`).
- **Rozwiązanie:** to jest właściwe miejsce na `derivedStateOf` (wyjście zmienia się rzadziej niż wejście) albo
  liczenie `visible`/`seconds` bezpośrednio w pętli `produceState` i wystawianie tylko ich.
- **Przykład:**
  ```kotlin
  val remaining = produceState(Long.MAX_VALUE, exoPlayer, window) { … }
  val visible by remember(dismissed, suppressed, window) {
      derivedStateOf { !dismissed && !suppressed && remaining.value <= window }
  }
  val seconds by remember { derivedStateOf { max(1, ceil(remaining.value / 1000f).toInt()) } }
  ```
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem.

### M4. Hero „Kontynuuj oglądanie”: każdy ruch fokusu po kartach rekomponuje cały hero

- **Problem:** `focusedCardIndex` jest czytany w Composition jako klucz `LaunchedEffect(focusedCardIndex)`
  w `ContinueWatchingHeroLoadedState`. Zapis w `onFocused` każdej karty rekomponuje cały komponent
  (info, backdrop, LazyRow scope), zanim zadziała debounce.
- **Lokalizacja:** `presentation/screens/home/ContinueWatchingHeroLoadedContent.kt`, l. ~124–130, 248–252.
- **Kategoria:** [2] klucz efektu wymusza odczyt w Composition, [4].
- **Wpływ:** rekompozycja największego elementu Home przy każdym ←/→ w rzędzie kart (debounce działa dopiero potem).
- **Przyczyna:** debounce zrealizowany przez restart efektu zamiast przez obserwację snapshotu.
- **Rozwiązanie:** jeden efekt z `snapshotFlow` — odczyt w `snapshotFlow` nie rejestruje się w zakresie rekompozycji.
- **Przykład:**
  ```kotlin
  // obecnie
  var focusedCardIndex by remember { mutableIntStateOf(selectedIndex) }
  LaunchedEffect(focusedCardIndex) { if (focusedCardIndex != selectedIndex) { delay(…); selectedIndex = focusedCardIndex } }
  // proponowane
  val focusedCardIndex = remember { mutableIntStateOf(selectedIndex) }
  LaunchedEffect(Unit) {
      snapshotFlow { focusedCardIndex.intValue }.collectLatest { index ->
          if (index != selectedIndex) { delay(TvDimens.HeroSwapDebounceMs); selectedIndex = index }
      }
  }
  ```
  (Ten sam wzorzec w mniejszej skali: `SeasonColumn.focusedSeason`, `DetailsEpisodesTab.kt` l. ~322 — Low.)
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem.

### M5. Klucz z indeksem w liście, z której użytkownik usuwa elementy

- **Problem:** klucz kart hero to `"${id}_${apiName}_${url}_$index"`. Lista obsługuje akcję „Usuń”; po usunięciu
  karty wszystkie kolejne dostają nowy klucz → są tworzone od nowa (utrata `remember`, stanu fokusu, ponowne
  żądania obrazów), a przywracanie fokusu może trafić w niewłaściwy element.
- **Lokalizacja:** `presentation/screens/home/ContinueWatchingHeroLoadedContent.kt`, l. ~233–236.
- **Kategoria:** [3].
- **Wpływ:** skok/jank i zbędna praca po usunięciu pozycji; możliwa utrata fokusu.
- **Przyczyna:** indeks dodany „na wszelki wypadek”, a repozytorium już deduplikuje po `apiName|url`
  (`compat/home/ContinueWatchingRepository.kt`, `distinctBy`).
- **Rozwiązanie:** klucz bez indeksu: `"${item.apiName}|${item.url}"`.
- **Przykład:**
  ```kotlin
  key = { _, item -> "${item.apiName}|${item.url}" }
  ```
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem.

### M6. Ekran Pobrane odświeża pełny stan co 2,5 s bez końca (także gdy nie jest widoczny)

- **Problem:** `startPeriodicRefresh()` uruchamia pętlę w `viewModelScope`, która nigdy nie jest zatrzymywana
  (brak powiązania z cyklem życia ekranu). Każdy tick przebudowuje cały stan: `getKeys` + parsowanie JSON
  wszystkich nagłówków/odcinków + odczyt informacji o plikach. Gdy użytkownik uruchomi odtwarzanie z ekranu
  Pobrane, wpis zostaje w back stacku, a praca trwa podczas odtwarzania wideo.
- **Lokalizacja:** `presentation/screens/downloads/DownloadsViewModel.kt`, `bind`/`startPeriodicRefresh`/
  `buildDownloadsState`, l. ~102–175.
- **Kategoria:** [5] (praca w tle konkurująca z playerem), [2] (brak powiązania z lifecycle).
- **Wpływ:** stałe obciążenie CPU/I/O (Dispatchers.IO) w tle, GC z parsowania JSON; na słabszych TV może
  konkurować z dekodowaniem/buforowaniem.
- **Przyczyna:** odświeżanie okresowe niezależne od widoczności ekranu.
- **Rozwiązanie:** uruchamiać pętlę tylko w stanie STARTED, np. z ekranu:
  `LifecycleStartEffect(viewModel) { viewModel.startPeriodicRefresh(); onStopOrDispose { viewModel.stopPeriodicRefresh() } }`
  albo w VM przez `uiState` z `stateIn(WhileSubscribed(5_000))`.
- **Przykład:**
  ```kotlin
  // DownloadsScreen
  LifecycleResumeEffect(viewModel) {
      viewModel.setPeriodicRefreshEnabled(true)
      onPauseOrDispose { viewModel.setPeriodicRefreshEnabled(false) }
  }
  ```
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem (koszt zależy od liczby pobranych pozycji).

### M7. Zapis postępu odtwarzania (JSON + SharedPreferences) na głównym wątku co 10 s

- **Problem:** `onPlaybackProgress` jest wołane z pętli `produceState` (Main) i co 10 s wykonuje
  `setViewPosAndResume()` → kilka `setKey()` (serializacja Jackson + `SharedPreferences.edit().apply()`),
  oraz przy zmianie statusu `setVideoWatchState`/`setLastWatched`.
- **Lokalizacja:** `presentation/screens/player/TvPlayerPlaybackProgressState.kt`, l. ~46–93;
  `PlayerScreenCoordinatorPlayback.kt`, l. ~12–26; `utils/DataStoreHelper.kt` (poza `tv/`), l. ~713.
- **Kategoria:** [5].
- **Wpływ:** okresowa praca na Main w trakcie odtwarzania (serializacja kilku obiektów); `apply()` jest
  asynchroniczne dla dysku, ale serializacja i kopiowanie mapy — nie.
- **Przyczyna:** wywołanie bezpośrednio z coroutine UI.
- **Rozwiązanie:** w koordynatorze przenieść zapis do `coroutineScope.launch(Dispatchers.IO)` (pozycja i duration
  są prymitywami — brak ryzyka wyścigów po stronie UI). Zapis wymuszony przy `onPlaybackStopped` może zostać
  synchroniczny (dispose).
- **Przykład:**
  ```kotlin
  // proponowane w onPlaybackProgress(context, …)
  context.coroutineScope.launch(Dispatchers.IO) {
      context.core.playbackProgressState.onPlaybackProgress(positionMs, durationMs)
  }
  ```
- **Priorytet:** Medium
- **Pewność:** Wymaga profilowania (StrictMode / trace: czas `setViewPosAndResume` na docelowym urządzeniu).

### M8. Lambdy (`@Composable` i zwykłe) wewnątrz `SidePanelMenuItem` budowanego w koordynatorze

- **Problem:** `SidePanelMenuItem` (data class w stanie `TvPlayerPanelsUiState`) zawiera `onClick`,
  `leadingContent = { PanelIcon(…) }` itd. Mappery w koordynatorze tworzą nowe instancje lambd przy każdej
  przebudowie listy, więc `equals` elementów i list jest zawsze fałszywe: `LaunchedEffect(items)` restartuje się,
  `remember(...)` przelicza się, a wszystkie wiersze dostają „zmieniony” `menuItem`.
- **Lokalizacja:** `presentation/common/MenuListSidePanel.kt`, `SidePanelMenuItem`, l. ~113–158;
  `presentation/screens/player/panels/PlayerSubtitlePanelMapper.kt`, l. ~61–104;
  `PlayerOnlineSubtitlesPanelBuilder.kt`, l. ~92–253.
- **Kategoria:** [4] niestabilne parametry, [2] restart efektów.
- **Wpływ:** przy każdej aktualizacji panelu napisów (np. strumieniowanie wyników wyszukiwania online) cały panel
  i wszystkie wiersze rekomponują się, a efekty fokusu/scrolla startują ponownie.
- **Przyczyna:** UI (lambdy Compose) w modelu stanu zamiast danych.
- **Rozwiązanie:** w stanie trzymać dane (np. `leadingIcon: ImageVector?`, `actionToken`), a lambdy Compose
  tworzyć w warstwie UI; mechanizm `actionToken` już istnieje i pokrywa kliknięcia.
- **Przykład:**
  ```kotlin
  // obecnie
  SidePanelMenuItem(id = …, leadingContent = { PanelIcon(PlayerIcons.Encoding) }, …)
  // proponowane
  SidePanelMenuItem(id = …, leadingIcon = PlayerIcons.Encoding, actionToken = …)
  // w PanelRow: leading = menuItem.leadingIcon?.let { icon -> { _ -> PanelIcon(icon) } }
  ```
- **Priorytet:** Medium
- **Pewność:** Prawdopodobny problem (zależy od częstotliwości przebudowy stanu paneli — sprawdzić w Layout Inspectorze).

---

## Problemy — priorytet Low

### L1. Gradienty tła szczegółów alokowane w każdej klatce animacji przyciemnienia

- **Problem:** w `drawBehind` tworzone są 2× `Brush.*Gradient` przy każdym rysowaniu; podczas animacji `dim()`
  (zwijanie strony, 150 ms) lambda wykonuje się co klatkę.
- **Lokalizacja:** `presentation/screens/details/DetailsBackdrop.kt`, `DetailsBackdrop`, l. ~105–131.
- **Kategoria:** [1] / [5] alokacje w fazie draw.
- **Wpływ:** alokacje shaderów co klatkę w trakcie animacji (niewielkie, ale bezcelowe).
- **Rozwiązanie:** `Modifier.drawWithCache { val side = Brush…; val bottom = Brush…; onDrawBehind { … dim() … } }` —
  pędzle przeliczą się tylko przy zmianie rozmiaru/koloru.
- **Przykład:**
  ```kotlin
  .drawWithCache {
      val side = Brush.horizontalGradient(…); val bottom = Brush.verticalGradient(…)
      onDrawBehind { drawRect(side, …); drawRect(bottom, …); dim().takeIf { it > 0f }?.let { drawRect(…) } }
  }
  ```
- **Priorytet:** Low · **Pewność:** Potwierdzony problem (wpływ mały).

### L2. Hydratacja odcinków przelicza mapę wszystkich odcinków przy każdej zmianie widocznego okna

- **Problem:** `hydrateVisibleWindow` buduje `episodeKeyByLocator = episodes.associate { … }` (O(n)) przy każdej
  emisji `snapshotFlow` z kluczami widocznych elementów, na Main.
- **Lokalizacja:** `presentation/screens/details/DetailsEpisodesStateHolder.kt`, l. ~67–76;
  `DetailsScreenContentEffects.kt`, l. ~66–77.
- **Kategoria:** [5].
- **Wpływ:** odczuwalne tylko przy bardzo długich sezonach (anime 500–1000+ odcinków) podczas przewijania.
- **Rozwiązanie:** liczyć mapę raz na zmianę `episodes` (np. w `LaunchedEffect(selectedEpisodes)` lub cache po referencji listy).
- **Priorytet:** Low · **Pewność:** Wymaga profilowania.

### L3. `HomeScreenV2`: domyślne parametry-fabryki alokowane przy każdej rekompozycji; `collectAsState` bez lifecycle

- **Problem:** `feedsViewModelFactory = HomeFeedsViewModelFactory(FeedRepositoryImpl())` (i dwie pozostałe fabryki)
  są wyliczane przy każdej rekompozycji `HomeScreenV2`, która następuje przy każdej emisji któregokolwiek
  z trzech stanów (każdy załadowany feed). `collectAsState()` zbiera też w tle.
- **Lokalizacja:** `presentation/screens/home/HomeScreenV2.kt`, l. ~25–44.
- **Kategoria:** [4] tworzenie obiektów w Composition.
- **Wpływ:** drobne alokacje; `remember(factory) { factory }` nic nie daje, bo argument jest nowy za każdym razem.
- **Rozwiązanie:** przenieść domyślne fabryki do `remember {}` w ciele (lub stałych), użyć `collectAsStateWithLifecycle()`.
  To samo dot. `collectAsState()` w `SearchScreen`, `DashboardScaffold`, `*FeedGridScreen`.
- **Priorytet:** Low · **Pewność:** Potwierdzony problem (mały wpływ).

### L4. Klucz `"$index:${item.url}"` w wierszach feedu

- **Problem:** po odświeżeniu feedu (`refreshFeed`) z przesunięciem elementów wszystkie karty zmieniają klucz.
- **Lokalizacja:** `presentation/screens/home/FeedRow.kt`, `FeedPosterRow`, l. ~289.
- **Kategoria:** [3].
- **Wpływ:** mały (lista ≤ 10, zmienia się tylko przy odświeżeniu); utrata `remember` kart i ponowne żądania obrazów.
- **Rozwiązanie:** deduplikacja w VM (`distinctBy { it.apiName to it.url }`) i klucz `item.url`/`gridKeySeed` bez indeksu.
  W gridzie (`grid/TvMediaGrid.kt`, l. ~90–97) indeks w kluczu jest uzasadniony (paging dopisuje na końcu) — bez zmian.
- **Priorytet:** Low · **Pewność:** Potwierdzony (wpływ mały).

### L5. Karta plakatu ładuje URL strony jako obraz, gdy brak plakatu

- **Problem:** `model = item.posterUri.takeIf { it.isNotBlank() } ?: item.url` — `item.url` to adres strony tytułu,
  nie obrazu. Coil pobierze HTML i zgłosi błąd dekodowania (dla każdej takiej karty, przy każdym wejściu w kadr
  po wyczyszczeniu cache).
- **Lokalizacja:** `presentation/screens/home/FeedPosterCard.kt`, `FeedPosterCard`, l. ~28.
- **Kategoria:** [5] zbędna praca sieci/dekodera przy ładowaniu obrazów.
- **Rozwiązanie:** przy pustym `posterUri` przekazać `null` (od razu placeholder z tytułem — `imageFailed`).
- **Priorytet:** Low · **Pewność:** Prawdopodobny problem (zależy od dostawców bez plakatów).

### L6. `ImageRequest` budowany w Composition bez `remember`

- **Problem:** `ImageRequest.Builder(...).build()` przy każdej rekompozycji wiersza ustawień z ikoną.
- **Lokalizacja:** `presentation/screens/settings/masterdetail/SettingsPanelComponents.kt`, `SettingsEntryLeading`, l. ~311–316.
  (Analogicznie nieużywany `presentation/common/PosterImage.kt` — dodatkowo `crossfade(true)`.)
- **Kategoria:** [4] / reguła `AGENTS.md` o `ImageRequest`.
- **Rozwiązanie:** `remember(context, entry.iconUrl) { ImageRequest.Builder(context)… .build() }`; usunąć martwy `PosterImage.kt`.
- **Priorytet:** Low · **Pewność:** Potwierdzony (wpływ mały).

### L7. Martwy stan `showExtendedMetadata` i trzy efekty, które go utrzymują

- **Problem:** `PlayerOverlayStateHolder.showExtendedMetadata` jest zapisywane przez 3 `LaunchedEffect` (+ licznik
  `metadataRevealGeneration`) przy każdej zmianie widoczności kontrolek / play-pause, ale żaden komponent produkcyjny
  go nie czyta (tylko testy).
- **Lokalizacja:** `presentation/screens/player/PlayerPlaybackEffects.kt`, `PlayerPlaybackFocusEffects`, l. ~344–405;
  `overlay/PlayerOverlayStateHolder.kt`, l. ~23, 35–56.
- **Kategoria:** [2] zbędne efekty i coroutine.
- **Rozwiązanie:** usunąć stan i efekty (oraz odpowiednie asercje testów) albo podpiąć do UI, jeśli funkcja ma wrócić.
- **Priorytet:** Low · **Pewność:** Potwierdzony.

### L8. Odczyty preferencji / DataStore w Composition

- **Problem:** synchroniczne odczyty na Main w `remember {}`: autoplay w `PlayerUpNextLayer`
  (`overlay/PlayerTransientLayers.kt`, l. ~129–132), konto w `TvNavDrawer` (`dashboard/TvNavDrawer.kt`, l. ~183,
  `getCurrentAccount()` z parsowaniem JSON), preferencje seek (`TvPlayerPlaybackPreferencesState.kt`, l. ~51–57),
  `needsWelcome` w `TvApp.kt` (l. ~119).
- **Kategoria:** [5].
- **Wpływ:** jednorazowy, zwykle mały (SharedPreferences są cache'owane w pamięci po pierwszym załadowaniu);
  pierwszy odczyt może blokować na I/O.
- **Rozwiązanie:** dla playera — wczytać w VM/koordynatorze razem z resztą stanu (`TvPlayerUiState.Ready.autoplayNext`);
  dla drawer'a — wystawić konto z istniejącego VM. Bez pilnej potrzeby zmian.
- **Priorytet:** Low · **Pewność:** Wymaga profilowania (StrictMode `detectDiskReads`).

### L9. `FocusRequester()` tworzony w ciele composable bez `remember`

- **Lokalizacja:** `presentation/screens/home/HomeScreenFeedList.kt`, `FeedRowPlaceholderSection`, l. ~85.
- **Kategoria:** [4]. **Wpływ:** nowy obiekt przy każdej rekompozycji placeholdera, parametr zawsze „zmieniony”.
- **Rozwiązanie:** `remember { FocusRequester() }`.
- **Priorytet:** Low · **Pewność:** Potwierdzony (wpływ minimalny).

### L10. Logi diagnostyczne napisów zawsze włączone

- **Problem:** `subtitleSyncDebugLog` = `Log.i` bez warunku; wywołania budują długie stringi (m.in. w
  `PlayerPlaybackLoadEffect`, `onTracksChanged`, panelach).
- **Lokalizacja:** `presentation/screens/player/core/PlayerMediaSourceFactory.kt`, l. ~331; wywołania w 8 plikach `player/`.
- **Kategoria:** [5] alokacje. **Wpływ:** mały (wywołania nie są w pętli klatek).
- **Rozwiązanie:** `if (BuildConfig.DEBUG)` w funkcji i przekazywanie `message: () -> String`.
- **Priorytet:** Low · **Pewność:** Potwierdzony (wpływ mały).

### L11. Animacje layoutu w elementach LazyColumn panelu bocznego

- **Problem:** `AnimatedVisibility(expandVertically/shrinkVertically)` + `animateContentSize()` w `items {}`
  przy rozwijaniu grup (napisy wg języka).
- **Lokalizacja:** `presentation/common/MenuListSidePanel.kt`, l. ~443–452.
- **Kategoria:** [1] animacja layoutu (reguła `AGENTS.md`).
- **Wpływ:** tylko przy rozwinięciu/zwinięciu grupy (nie przy każdym ruchu fokusu) — może dawać `measureAndLayout`
  całej listy przez 150 ms.
- **Rozwiązanie:** zweryfikować w trace (ON vs OFF); jeśli widać wzrost janky frames — wyłączyć (`enableItemAnimations = false`
  jest już dostępne) lub zastąpić `Modifier.animateItem()`.
- **Priorytet:** Low · **Pewność:** Wymaga profilowania.

### L12. `PlayerSeekHint` czyta `exoPlayer.currentPosition` w Composition

- **Problem:** odczyt nie-snapshotowy — wartość „zamarza” na moment ostatniej rekompozycji (pasek postępu podpowiedzi
  jest nieaktualny). Po wdrożeniu H2 rekompozycji będzie mniej, więc nieaktualność wzrośnie.
- **Lokalizacja:** `presentation/screens/player/overlay/PlayerTransientLayers.kt`, `PlayerSeekHint`, l. ~72–74.
- **Kategoria:** [1].
- **Rozwiązanie:** czytać pozycję w `drawBehind {}` paska (rysowanie i tak odbywa się przy zmianie `deltaMs`), albo
  użyć istniejącego `PlaybackTimelineUiState`.
- **Priorytet:** Low · **Pewność:** Potwierdzony (poprawność wizualna, nie wydajność).

### L13. Drobiazgi

- `presentation/screens/search/SearchSection.kt`, l. ~219: `items = List(config.totalSlots) { it }` — nowa lista
  przy każdej rekompozycji wiersza; można użyć `items(count = config.totalSlots, key = …)`. [4], Low.
- `presentation/screens/home/HomeSourcesUiState.kt`: `@Immutable` na klasie z `MainAPI` (klasa mutowalna z wtyczek).
  Kontrakt formalnie niespełniony, ale instancje są tylko podmieniane — nie zmieniać bez potrzeby; nie dodawać
  kolejnych takich adnotacji. [4], Low.
- `compat/home/ContinueWatchingRepository.kt`: `colorWarmupScope` nigdy nie jest anulowany (krótkie zadania,
  ale scope „wycieka” z każdym `ContinueWatchingImagePrefetcher`). [2], Low.
- `presentation/common/PosterPaletteCache.kt` (`PaletteCache`, `DefaultPosterPaletteCache`) i `common/PosterImage.kt`
  — nieużywane (martwy kod, brak wpływu na runtime poza inicjalizacją obiektu `DefaultPosterPaletteCache` przy
  pierwszym dostępie do pliku). Low.

---

## 1. Podsumowanie najważniejszych problemów

1. **Player — praca przy przełączaniu overlayu:** warstwa napisów przy każdym show/hide kontrolek ponownie nakłada
   pełny styl (z możliwym odczytem pliku fontu i relayoutem) — H1.
2. **Player — zbyt szeroki zakres odczytu stanu:** pola `PlayerOverlayStateHolder` (seek hint, buffering, isPlaying)
   są czytane w korzeniu ekranu playera; przytrzymanie ←/→ = rekompozycja całego ekranu na każde powtórzenie klawisza — H2.
   Do tego M1 (fokus w pasku kontrolek), M2/M3 (tickery 500 ms pracujące przy niewidocznym UI).
3. **Panele boczne nad wideo:** stan fokusu w Composition + lambdy w modelu stanu → pełne rekompozycje panelu
   przy ↑/↓ i przy każdej aktualizacji danych — H3, M8.
4. **Lista odcinków:** wspólna mapa stanów czytana w każdym wierszu → fale rekompozycji przy pobieraniu i przewijaniu — H4.
5. **Praca w tle / na Main w trakcie odtwarzania:** okresowe odświeżanie Pobranych bez końca (M6), zapis postępu
   na Main (M7).

Ogólna ocena: projekt ma dojrzałe wzorce wydajnościowe (odroczone odczyty w `graphicsLayer`/`drawBehind`/`offset {}`,
leniwe efekty fokusu kart, stabilne klucze niemal wszędzie, `PersistentList`, I/O na `Dispatchers.IO`).
Problemy koncentrują się w kilku miejscach o wysokiej częstotliwości zdarzeń (player, panele, odcinki).

## 2. Ranking według wpływu na płynność

| # | Problem | Obszar | Priorytet | Pewność |
|---|---------|--------|-----------|---------|
| 1 | H2 — stan overlayu czytany w korzeniu playera (seek/buffering) | Player | High | Potwierdzony |
| 2 | H1 — pełny styl napisów (font I/O) przy każdym toggle overlayu | Player | High | Potwierdzony |
| 3 | H3 — `MenuListSidePanel` rekomponuje się przy każdym ↑/↓ | Panele nad wideo | High | Prawdopodobny |
| 4 | H4 — mapa stanów odcinków czytana w każdym wierszu | Szczegóły | High | Potwierdzony |
| 5 | M1 — `lastFocusedControl` czytane w `PlayerOverlay` | Player | Medium | Potwierdzony |
| 6 | M4 — hero CW rekomponuje się przy każdym ruchu fokusu | Home | Medium | Potwierdzony |
| 7 | M8 — lambdy w `SidePanelMenuItem` (stan) | Panele | Medium | Prawdopodobny |
| 8 | M6 — nieskończone odświeżanie Pobranych | Tło | Medium | Potwierdzony |
| 9 | M7 — zapis postępu na Main co 10 s | Player | Medium | Wymaga profilowania |
| 10 | M2 — tick osi czasu przy ukrytym overlayu | Player | Medium | Potwierdzony |
| 11 | M3 — „Up next” rekomponuje się co 500 ms | Player | Medium | Potwierdzony |
| 12 | M5 — klucz z indeksem w hero CW (usuwanie) | Home | Medium | Potwierdzony |
| 13 | L1–L13 | różne | Low | różna |

## 3. Rekomendowana kolejność wdrażania

Kolejność łączy wpływ, ryzyko i koszt zmiany (najpierw tanie i lokalne):

1. **H1** — w `update` ustawiać tylko `translationY` (zmiana lokalna w `PlayerVideoSurface.kt`).
2. **H2** — przenieść odczyty `seekHintDeltaMs`, `showBufferingOverlay`, flag keep-screen-on do komponentów-liści.
3. **M1** — rozwiązywanie `down` w `focusProperties {}` osi czasu.
4. **M5** — klucz kart CW bez indeksu (jedna linia).
5. **M4** — debounce przez `snapshotFlow` (hero CW; przy okazji `SeasonColumn`).
6. **M3** — `derivedStateOf` dla `visible`/`seconds` w „Up next”.
7. **H3** — `focusedItemId` jako zwykły holder w `MenuListSidePanel` (po weryfikacji w Layout Inspectorze).
8. **H4** — modele wierszy odcinków w `DetailsEpisodesStateHolder` (+ jeden zapis przy hydratacji).
9. **M6** — powiązanie odświeżania Pobranych z lifecycle ekranu.
10. **M7** — zapis postępu na `Dispatchers.IO` (po pomiarze).
11. **M8** — usunięcie lambd z `SidePanelMenuItem` (większy refaktor mapperów — po potwierdzeniu w profilowaniu).
12. **M2** — wstrzymanie aktualizacji tekstów osi czasu przy ukrytym overlayu.
13. Pozycje Low według okazji (przy zmianach w danym pliku).

Po każdej zmianie: `./gradlew :app:assemblePrereleaseDebug` i porównanie trace'u przed/po (zgodnie z `AGENTS.md`).

## 4. Problemy wymagające profilowania zamiast analizy statycznej

- **Liczniki rekompozycji (Layout Inspector):** `PlayerPlaybackContent` przy przytrzymanym ←/→ (H2),
  `MenuListSidePanel`/`PanelRow` przy ↑/↓ w panelach (H3), `PlayerOverlay` przy ←/→ w pasku (M1),
  `EpisodeRow` podczas aktywnego pobierania (H4), `ContinueWatchingHeroLoadedState` (M4).
- **Perfetto / System Trace (janky frames, `measureAndLayout`, `Choreographer#doFrame`):**
  toggle overlayu z własnym fontem napisów (H1), otwieranie i przewijanie paneli źródeł/napisów,
  rozwijanie grup językowych (L11), zwijanie strony szczegółów (L1).
- **StrictMode (`detectDiskReads/Writes`) w buildzie debug:** M7 (zapis postępu), L8 (preferencje w Composition), H1.
- **CPU Profiler podczas odtwarzania:** M6 (odświeżanie Pobranych w tle), M2/M3 (tickery 500 ms).
- **Raporty kompilatora Compose:** nie są skonfigurowane. Warto tymczasowo włączyć
  `composeCompiler { reportsDestination = …; metricsDestination = … }` i sprawdzić restartowalność/skippability
  `PlayerOverlay`, `PlaybackTimelineSection`, `PanelRow`, `EpisodeRow` (np. czy parametry typu `ExtractorLink`,
  `SubtitleData`, `MainAPI` nie wymuszają porównań `===` tam, gdzie instancje są odtwarzane).
- **Overdraw nad wideo:** `PlayerTopMask` + gradient dolny + panel — „Debug GPU overdraw” na docelowym TV.

## 5. Elementy zaimplementowane poprawnie (bez zmian)

- **Fokus kart:** `focus/FocusStyles.kt` `tvCardFocus` — karty nigdy nie fokusowane nie mają stanu animacji ani warstwy;
  skala w `graphicsLayer {}`, obrys w `drawWithContent` (bez animacji layoutu).
- **Oś czasu playera:** `PlaybackTimeline` rysuje postęp/bufor/kciuk w `drawBehind {}` czytając `State` w fazie draw;
  `produceState` z deduplikacją wartości, interwał 500 ms (zgodnie z `AGENTS.md`).
- **Overlay playera:** osobne poddrzewo, ukrywanie przez `alpha` w `graphicsLayer {}` (bez zmiany layoutu);
  gradienty overlayu w `remember`.
- **ExoPlayer / PlayerView:** player tworzony raz (`PlayerSessionController` w `remember`), zwalniany w `DisposableEffect`;
  `AndroidView.update` w `TvPlayerVideoSurface` zmienia `player`/`resizeMode` tylko przy różnicy; listenery
  rejestrowane/wyrejestrowywane w `DisposableEffect`, aktualne callbacki przez `rememberUpdatedState`.
- **Panele:** `SlidingSidePanel` — slide i scrim sterowane jednym `Animatable`, czytanym wyłącznie w
  `graphicsLayer`/`drawBehind`; `TvNavDrawer` — analogicznie, plus `derivedStateOf` do decyzji o kompozycji.
- **Home:** sekcja hero/chipów przesuwana w fazie placement (`Modifier.offset { }`) z ograniczeniem odczytu scrolla;
  `FeedRow` — opis fokusowanego plakatu aktualizowany z opóźnieniem 2 klatek w osobnym zakresie
  (`FeedRowFocusDescription`), dwuetapowa kompozycja wiersza, wspólny handler klawiszy dla wiersza,
  `NeighbourRowsPrefetchStrategy`.
- **Tytuł fokusowanego elementu:** `TvFocusedItemTitle` przyjmuje `State<…>` i czyta go lokalnie.
- **Szczegóły:** animacja zwijania (`progress`) czytana wyłącznie w `graphicsLayer {}` / `dim = { }` w draw.
- **Grid „Pokaż wszystko”:** `derivedStateOf` dla `isScrolled` + `alpha` cienia w `graphicsLayer`.
- **Wiersze ustawień:** skala fokusu (`animateFloatAsState`) czytana w `graphicsLayer {}`.
- **Klucze list:** stabilne `key` w praktycznie wszystkich `LazyColumn/LazyRow/LazyVerticalGrid`
  (odcinki, sezony, obsada, panele, ustawienia, wyszukiwanie, biblioteka, pobrane, drawer).
- **Obrazy:** `PosterCard` — `ImageRequest` w `remember(context, model)`, `crossfade(false)`; backdropy i miniatury
  odcinków/obsady także w `remember`.
- **Stan i wątki:** `PersistentList`/`PersistentMap` w stanach UI; repozytoria feedów, wyszukiwania, biblioteki,
  pobranych i kontynuacji oglądania pracują na `Dispatchers.IO`; kwantyzacja kolorów artworku na IO.

---

*Walidacja:* raport powstał wyłącznie na podstawie analizy statycznej. Nie uruchamiano buildu, testów, emulatora,
Layout Inspectora ani trace'ów — wszystkie oceny wpływu oznaczone jako „Prawdopodobny” / „Wymaga profilowania”
należy potwierdzić pomiarem przed wdrożeniem.
