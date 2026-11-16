# Jetpack Compose – Performance Audit (CloudStream)

Data audytu: 2026-11-16 · Commit bazowy: `5d66bbe` · Tryb: **wyłącznie analiza statyczna kodu, bez zmian w aplikacji**.

---

## 0. Kontekst i zakres (ważne przed lekturą)

### 0.1 Gdzie w ogóle jest Compose w tej aplikacji

Aplikacja jest w trakcie migracji View → Compose (patrz `COMPOSE.md`). Zweryfikowany stan:

| Obszar | Technologia | Objęty audytem Compose? |
|---|---|---|
| Ustawienia (`ui/settings/*`), Rozszerzenia (`ExtensionsScreen`), Test providerów (`TestScreen`), Logcat, Napisy (`SubtitlesScreen`, `ChromecastSubtitlesScreen`), Wybór konta (`AccountScreen`), dialog aktualizacji (`MainActivityScreen`) | **Compose** (14 plików w `app/`, ok. 49 w `shared/`) | **Tak** |
| Odtwarzacz (`ui/player/*`, ExoPlayer, kontrolki, menu źródeł/napisów) | Views/XML (`FullScreenPlayer`, `GeneratorPlayer`) | **Nie – brak Compose** |
| Home, Result (szczegóły filmu/serialu), Search, Download, listy sezonów/odcinków, siatki posterów | Views + RecyclerView/adaptery | **Nie – brak Compose** |

Sprawdzone: `grep -rln "androidx.compose"` w `ui/player`, `ui/home`, `ui/result`, `ui/search` → **0 plików**. W całym projekcie nie ma listy odcinków ani siatki posterów w Compose (jedyny `LazyVerticalGrid` to `SettingsIconGrid` w `SettingsItems.kt:437`).

**Konsekwencje dla pięciu kategorii z zadania:**
- Punkty dotyczące *odtwarzania filmu, kontrolek i menu playera, aktualizacji stanu ExoPlayera, list filmów/seriali/sezonów/odcinków, siatek posterów, ładowania metadanych przy otwieraniu szczegółów* – **nie można ocenić kryteriami Compose, bo ten kod nie jest w Compose**. Nie wymyślam tu problemów; ta część wymaga osobnego audytu View/RecyclerView (DiffUtil, ViewHolder, postępy playera, Coil w adapterach) – patrz sekcja 6.
- Cały audyt Compose dotyczy więc ekranów **ustawień / rozszerzeń / kont / napisów / logów**. Są używane rzadko, więc priorytety uwzględniają realną częstotliwość użycia.

### 0.2 Kompilator Compose – wpływ na ocenę „niestabilnych parametrów”

Kotlin Gradle Plugin = **2.4.0** (`gradle/libs.versions.toml`), Compose Multiplatform 1.12.0. Od Kotlin 2.0.20 **Strong Skipping Mode jest domyślnie włączony**, a w plikach `build.gradle.kts` / `gradle.properties` nie znalazłem konfiguracji go wyłączającej (`grep composeCompiler|enableStrongSkipping|featureFlags` → brak). Oznacza to, że:
- composable z niestabilnymi parametrami (np. `List<T>`, klasy z `var`) **są skipowalne** (porównanie przez `===`),
- lambdy są automatycznie zapamiętywane, jeśli ich przechwycone wartości się nie zmieniają.

Dlatego **nie zgłaszam** ogólnych „brak `@Stable`/`@Immutable`” ani „brak `remember` na lambdzie” jako problemów. Realny koszt pojawia się tam, gdzie **w każdej kompozycji powstają nowe instancje** (nowe `List`/`Map`/data class), bo wtedy porównanie `===` zawsze zwraca false.

### 0.3 Czego nie zrobiłem
Nie uruchamiałem aplikacji, Layout Inspectora, Compose Compiler Metrics ani profilera (środowisko chmurowe, brak urządzenia/emulatora). Pole **Pewność** rozróżnia: *Potwierdzony problem* (wynika wprost z kodu), *Prawdopodobny problem*, *Wymaga profilowania* (mechanizm jest w kodzie, ale skala zależy od danych/urządzenia). Numery linii są przybliżone (znak `~`) tam, gdzie liczyłem je z fragmentów wydruku.

---

## 1. Znaleziska

Legenda kategorii: **[1] Fazy Compose · [2] Efekty uboczne · [3] Klucze list · [4] Recomposition/stan · [5] Main thread**

---

### P-01 · Nieograniczona częstotliwość aktualizacji stanu podczas pobierania wtyczki

- **Problem:** Każdy callback postępu pobierania (po każdym `read()` bufora) wykonuje `updateState { copy(plugins = plugins.updating(...)) }`. Brak throttlingu.
- **Lokalizacja:** `shared/.../viewmodels/ExtensionsViewModel2.kt` `downloadPlugin()` ~L922–938; źródło wywołań: `app/.../settings/ApkUpdater.kt` `transfer()` L176–181 (`downloadProgress(transferred, length)` w pętli `read`; sygnatura odpowiada wywołaniu w `AndroidRepositoryManager.kt:166–198` – samej nazwy wywoływanej funkcji tam nie sprawdzałem). Konsument UI: `ExtensionsScreen.kt` `Plugin()` L936–~1170 oraz `PluginWidget()` L1170–~1330 (odczyt `plugin.status`, ~L1265, ~L1286).
- **Kategoria:** [4] (+ [5] – praca reducerów w wątku IO)
- **Wpływ:** Dla wtyczki kilku MB to setki aktualizacji stanu (bufor `DEFAULT_BUFFER_SIZE`). Każda: kopia `ExtensionsState`, `PersistentMap.putting`, a w `SearchableDataState.updating` dodatkowo `state.removing(oldItem).adding(newItem)` (przeliczenie stanu pochodnego, `ImmutableState.kt` ~L200–214) oraz porównanie komparatora. Po stronie UI: `plugin` to nowa instancja przy każdym ticku → **cały wiersz `Plugin` (FlowRow, teksty, `getNameNextToFlagEmoji`, `formatShortFileSize`) rekomponuje się najwyżej raz na klatkę**, choć zmienia się tylko `CircularProgressIndicator`. Przy „Aktualizuj wszystkie” działa to równolegle dla wielu wtyczek.
- **Przyczyna:** Postęp jest częścią niemutowalnego `Plugin`, więc zmiana postępu = zmiana całego obiektu wiersza. Lambda `progress = { status.progress / total }` w `PluginWidget` (~L1286–1290) jest poprawnie odroczona do fazy draw, ale ta oszczędność jest unieważniana, bo rodzic i tak dostaje nowy `plugin`.
- **Rozwiązanie:** (1) Throttling w callbacku (np. emisja co ≥100 ms, zawsze przy zakończeniu). (2) Jeśli po (1) profil nadal pokaże koszt – wydzielić `PluginWidget`, tak by tylko on czytał `status`.
- **Przykład:**
  ```kotlin
  // Obecnie (ExtensionsViewModel2.kt ~L930)
  downloadProgress = { downloadedBytes, totalBytes ->
      updateState { copy(plugins = plugins.updating(plugin.uuid) {
          copy(status = PluginStatus.Downloading(total = totalBytes, progress = downloadedBytes))
      }) }
  }

  // Propozycja (commonMain: TimeSource.Monotonic zamiast System.nanoTime)
  var lastEmit = TimeSource.Monotonic.markNow()
  downloadProgress = { downloadedBytes, totalBytes ->
      if (lastEmit.elapsedNow() >= 100.milliseconds || downloadedBytes == totalBytes) {
          lastEmit = TimeSource.Monotonic.markNow()
          updateState { /* jak wyżej */ }
      }
  }
  ```
- **Priorytet:** High
- **Pewność:** Potwierdzony brak throttlingu (kod). Skala kosztu – **wymaga profilowania** (liczba rekompozycji `Plugin` podczas pobierania).

---

### P-02 · Wyszukiwanie wtyczek: brak debounce + filtr i sortowanie (Levenshtein) w reducerze na Main

- **Problem:** Każda zmiana tekstu natychmiast wywołuje `onAction(Search(...))`. Obsługa wykonuje filtr i sortowanie wszystkich wtyczek **synchronicznie wewnątrz `updateState {}`** w wątku wywołującym (Main, bo akcja pochodzi z `LaunchedEffect`).
- **Lokalizacja:** `ExtensionsScreen.kt` L602–604 (`LaunchedEffect(textState.text) { onAction(Search(...)) }`); `ExtensionsViewModel2.kt` obsługa `ExtensionsAction.Search` ~L604–620; `PluginSortBy` L116–144 i `PluginFilterByQuery` L146–~170; analogicznie `TestScreen.kt` L466–468 (`ProviderHeader` → `TestAction.Search`; implementacji w `TestViewModel2` nie czytałem – *do potwierdzenia*). `DebounceQuery` istnieje (`Viewmodel.kt:198–214`) i **nie jest nigdzie użyte** (`grep` → tylko definicja).
- **Kategoria:** [5] (+ [2] – efekt wyzwalający pracę)
- **Wpływ:** Przy liście rzędu setek/tysięcy wtyczek: na każdy znak `O(n)` filtr + `O(n log n)` sortowanie, gdzie komparator `PluginSortBy.compare` woła `score()` dwukrotnie na porównanie, a `score()` robi `lowercase()` (alokacja) + `Levenshtein.partialRatio` (nazwa, potem opis). Może to przekroczyć budżet klatki na słabym TV. Dodatkowo `MutableStateFlow.update` **ponawia reducer przy kolizji CAS** (komentarz autora `Viewmodel.kt:74` ostrzega dokładnie przed tym), a równolegle trwają aktualizacje z P-01, więc ciężki reducer może być wykonywany wielokrotnie.
- **Przyczyna:** Praca CPU-bound w reducerze + brak debounce, mimo gotowego narzędzia w kodzie.
- **Rozwiązanie:** (1) Debounce ~200 ms po stronie UI (`snapshotFlow` + `debounce`) albo użycie istniejącego `DebounceQuery`. (2) Policzyć `score` raz na wtyczkę (mapa `Uuid → score`) zamiast w komparatorze. (3) Jeśli nadal drogie – policzyć nowe `filtered/sorted` poza `update` (`Dispatchers.Default`), a w reducerze tylko podmienić gotowy wynik. Bez zmiany architektury MVI.
- **Przykład:**
  ```kotlin
  // Obecnie
  LaunchedEffect(textState.text) { onAction(Search(textState.text.toString())) }

  // Propozycja
  LaunchedEffect(textState) {
      snapshotFlow { textState.text.toString() }
          .debounce(200)
          .distinctUntilChanged()
          .collect { onAction(Search(it)) }
  }
  ```
- **Priorytet:** High
- **Pewność:** Prawdopodobny problem (mechanizm potwierdzony; próg bolesności zależy od liczby wtyczek – **wymaga profilowania** na realnej liście repozytoriów).

---

### P-03 · Wyszukiwarka ustawień buduje drzewa preferencji wszystkich ekranów w kompozycji (Main) i uruchamia ich efekty

- **Problem:** Przy pierwszym znaku w wyszukiwarce `SettingSearchResults` wywołuje `deferredItems()` **bezpośrednio w ciele composable** i zapisuje wynik w `mutableStateOf` podczas kompozycji. `generateSearchItems()` woła `getPreferences()` (composable) każdego ekranu ustawień.
- **Lokalizacja:** `shared/.../settings/Search.kt` L549–552; `SettingsFragmentScreen.kt` `generateSearchItems()` L300–309; skutek uboczny: `SettingsPlayerScreen.kt` L54–60 (`LaunchedEffect(cacheCleared)` → `getFolderSize(cacheDir)` w IO) oraz `rememberAppSettings`/`collectAsState` w pozostałych `getPreferences()`.
- **Kategoria:** [5] + [2] + [4]
- **Wpływ:** Jednorazowy skok pracy na Main przy pierwszym wpisanym znaku: budowa kilkuset obiektów `Preference`, `stringResource`, listy języków (P-05). Dodatkowo samo wpisanie znaku na głównym ekranie ustawień uruchamia skanowanie rozmiaru cache z `SettingsPlayerScreen` (dane niepotrzebne wyszukiwaniu). Zapis stanu w kompozycji (`items = deferredItems()`) jest chroniony warunkiem, więc nie ma pętli, ale to wzorzec delikatny.
- **Przyczyna:** `getPreferences()` jest jednocześnie źródłem danych o układzie i miejscem odczytu stanu/efektów (`remember`, `LaunchedEffect`); nie da się go użyć „na sucho”.
- **Rozwiązanie:** Zmierzyć koszt pierwszego znaku. Jeśli >1 klatki: rozdzielić statyczne metadane wyszukiwania (tytuł/podtytuł/nawigacja) od odczytu stanu/efektów, tak by indeks wyszukiwania nie wymagał kompozycji ekranów. To dotyka modelu `SearchableSettings`, więc to **kandydat do decyzji architektonicznej, nie do doraźnej poprawki** (zakaz zmian architektury – tylko rekomendacja). Minimalny krok bez zmiany architektury: przenieść `cacheSize` do wiersza (P-04), żeby budowa drzewa nie uruchamiała skanowania.
- **Priorytet:** Medium
- **Pewność:** Mechanizm – Potwierdzony problem; skala – **wymaga profilowania**.

---

### P-04 · `getPreferences()` przebudowuje całą listę preferencji przy zmianie dowolnego stanu odczytanego w jego scope

- **Problem:** `PreferenceScaffold` wywołuje `itemsProvider()` (=`getPreferences()`) wewnątrz lambdy `content` Scaffolda. Każdy stan odczytany bezpośrednio w `getPreferences()` powoduje **zbudowanie nowej listy nowych obiektów `Preference`** (z nowymi `Map`, lambdami, `Painter`) → wszystkie widoczne wiersze dostają nowe parametry (`===` false) i rekomponują się.
- **Lokalizacja:** `PreferenceScaffold.kt` L36–47; odczyty stanu na poziomie ekranu: `SettingsPlayerScreen.kt` L54–55 i L346 (`cacheSize`), `SettingsGeneralScreen.kt` L85–86 (`bananas`, `downloadPathVisual`), `SettingsUpdatesScreen.kt` L73–74 (`visualBackupPath`, `showDialog`).
- **Kategoria:** [4]
- **Wpływ:** Niski co do częstości (zmiany po akcji użytkownika: wyczyszczenie cache, zmiana ścieżki), ale każde zdarzenie kosztuje przebudowę całego ekranu ustawień razem z P-05. Ekran napisów robi to poprawnie: `store.state` jest odczytywany w `CustomPreference.content` (`SubtitlesScreen.kt` ~L202), czyli lokalnie.
- **Przyczyna:** Stan odczytywany w scope funkcji tworzącej dane, nie w scope wiersza.
- **Rozwiązanie:** Wartości zmieniające się po interakcji odczytywać wewnątrz lambd wiersza (`widget` / `CustomPreference.content`). Nie owijać całej listy w `remember` bez kluczy – ukryłoby to problem, a nie rozwiązało.
- **Przykład (SettingsPlayerScreen, ~L343–352):**
  ```kotlin
  // Obecnie: cacheSize czytany w getPreferences() → przebudowa całej listy po wyczyszczeniu cache
  subtitle = formatShortFileSize(LocalContext.current, cacheSize),

  // Propozycja: odczyt w widgecie wiersza (rekomponuje się tylko ten wiersz)
  Preference.PreferenceItem.TextPreference(
      title = …,
      widget = { Text(formatShortFileSize(LocalContext.current, cacheSizeState.longValue)) },
      onClick = { … cacheCleared += 1 },
  )
  ```
  (`cacheSizeState` jako `MutableLongState` odczytywany dopiero w `widget`, bez delegata `by` w scope funkcji nadrzędnej.)
- **Priorytet:** Medium-Low
- **Pewność:** Potwierdzony problem (strukturalny); wpływ – wymaga profilowania.

---

### P-05 · Lista języków wyliczana na Main przy każdej rekompozycji ekranu (Providers/General)

- **Problem:** `APIHolder.apis.withLock { map { lang }.distinct() }.sortedBy { fromTagToLanguageName(it) ?: it }` jest wykonywane w ciele `getPreferences()` bez `remember`.
- **Lokalizacja:** `SettingsProvidersScreen.kt` L49–53; `SettingsGeneralScreen.kt` L112–115 (+ `appLanguages.associate { … nameNextToFlagEmoji() }` w General, `TvType.entries.associate { … stringResource … }` w Providers ~L66–68).
- **Kategoria:** [5] + [4]
- **Wpływ:** Per rekompozycję: przejście po wszystkich providerach pod `synchronized`, `distinct`, sortowanie z wyszukiwaniem nazwy języka w komparatorze. `withLock` to zwykłe `synchronized(this)` (`AtomicList.kt:16`) – **blokuje Main**, jeśli inny wątek akurat modyfikuje listę (ładowanie wtyczek). Rekompozycje wywołują m.in. P-03/P-04.
- **Przyczyna:** Obliczenia pochodne w composable bez zapamiętania.
- **Rozwiązanie:** `remember(<klucz zmiany listy>)`. Uwaga: `APIHolder.apis` jest mutowalną listą nieobserwowaną przez Compose, więc klucz musi być uczciwy (np. `apis.size`, jeśli to wystarcza). **Nie** polecam `remember(Unit)`, bo zamroziłoby listę po doładowaniu wtyczek (dziś odświeża się przy każdej rekompozycji).
- **Priorytet:** Low-Medium
- **Pewność:** Prawdopodobny problem (koszt zależy od liczby providerów).

---

### P-06 · `LogcatDialog`: start procesu i budowa listy na Main, `withContext(IO)` na każdy rekord, proces nie jest niszczony

- **Problem:** `LaunchedEffect` działa na Main: `Runtime.getRuntime().exec("logcat --binary -d")` startuje proces na wątku UI; pętla `parseItem()` robi `withContext(Dispatchers.IO)` **dla każdego rekordu** (`LogcatParser.kt:57`, tysiące przełączeń), a `items.toPersistentList()` (O(n)) wykonuje się na Main. `Process` nie jest zamykany (`destroy()`), zamykany jest tylko strumień.
- **Lokalizacja:** `LogcatDialog.kt` L54–76 (`LaunchedEffect(dismiss)`); `LogcatParser.kt` L57.
- **Kategoria:** [5] + [2]
- **Wpływ:** Dialog rzadko używany (ekran aktualizacji/debug), ale zacięcia przy otwarciu i nieposprzątany proces. Klucz `LaunchedEffect(dismiss)` jest semantycznie błędny (efekt ma uruchomić się raz); dziś działa, bo wywołujący (`SettingsUpdatesScreen.kt:77`) przekazuje lambdę przechwytującą stabilny `MutableState`; zmiana na lambdę przechwytującą zmienną wartość **uruchomiłaby logcat ponownie**.
- **Przyczyna:** Brak jednego `withContext(Dispatchers.IO)` wokół całego bloku I/O; zły klucz efektu; brak sprzątania procesu.
- **Rozwiązanie:** Cały odczyt w jednym `withContext(Dispatchers.IO) { … }` (włącznie z `toPersistentList()`), `finally { process.destroy() }`, klucz `Unit`.
- **Przykład:**
  ```kotlin
  LaunchedEffect(Unit) {
      isLoading = true
      list.value = withContext(Dispatchers.IO) {
          val process = Runtime.getRuntime().exec("logcat --binary -d")
          try {
              LogcatBinaryParser(process.inputStream).use { parser ->
                  buildList { while (true) add(parser.parseItem() ?: break) }
              }.toPersistentList()
          } finally { process.destroy() }
      }
      isLoading = false
  }
  ```
  (Obecny `try/catch` z `logError` należy zachować.)
- **Priorytet:** Medium
- **Pewność:** Potwierdzony problem.

---

### P-07 · Zapis stylu napisów w `SideEffect` (serializacja/zapis przy każdej zmianie)

- **Problem:** `SideEffect(state) { context.saveStyle(state) }` zapisuje styl (`setKey(...)`) w wątku UI, w trakcie interakcji.
- **Lokalizacja:** `SubtitlesScreen.kt` L145–148; `ChromecastSubtitlesScreen.kt` L52–55; `SubtitlesFragment.kt` L316–321 i `ChromecastSubtitlesFragment.kt` L97–102 (`saveStyle`).
- **Kategoria:** [2] + [5]
- **Wpływ:** Zapis przy każdej zmianie suwaka/koloru (przeciąganie = wiele zapisów) i prawdopodobnie przy pierwszym otwarciu ekranu (zapis niezmienionego stanu). `SideEffect` służy do publikowania stanu do świata nie-Compose, nie do trwałego I/O.
- **Przyczyna:** Efekt wykonuje się po udanej rekompozycji; **sygnaturę `SideEffect(state) { … }` należy zweryfikować** w użytej wersji runtime (nie jest to klasyczne `SideEffect(effect)`). Jeśli to wariant kluczowany, wywołań jest mniej, ale nadal jeden zapis na krok przeciągania.
- **Rozwiązanie:** Zapis z debounce poza Main: `LaunchedEffect(Unit) { snapshotFlow { state }.drop(1).debounce(300).collect { withContext(Dispatchers.IO) { context.saveStyle(it) } } }`. `saveStyle` modyfikuje też `subtitleStyleState`, więc trzeba rozdzielić „zapis do preferencji” od „aktualizacji stanu”. Zapis końcowy w istniejącym `onDispose` (L150–154) zostaje jako gwarancja.
- **Priorytet:** Medium-Low
- **Pewność:** Wymaga profilowania (koszt `setKey` – serializacja małego obiektu – prawdopodobnie <1 ms, ale wielokrotny).

---

### P-08 · Animacje i obramowania fokusu czytane w fazie Composition zamiast Layout/Draw (TV)

- **Problem:** Stan fokusu i wartości animowane są odczytywane w kompozycji i przekazywane jako wartości do `Modifier`.
- **Lokalizacja i detale:**
  1. `Modifiers.kt` `Modifier.focusOutline()` L62–74 → `hasFocus` czytany w kompozycji i przekazany do `whiteOutline()` (`border(...)`, L76–86). Użycia m.in.: `BasePreferenceWidget.kt:54` (każdy wiersz preferencji), `Button.kt:70`, `TextField.kt:21`, `ColorDialog.kt:196,229`, `ExtensionsScreen.kt:732`, `SettingsFragmentScreen.kt:237,337,467`, `AccountScreen.kt:157,414,456`.
  2. `SettingsFragmentScreen.kt` `SettingsSearch()` L316 (`animateFloatAsState`) i L333 (`.padding(horizontal = 24.dp - 12.dp * focusProgress)`) → przez czas animacji **co klatkę rekompozycja `TextField` i ponowny layout**.
  3. `AccountScreen.kt` L503–521 i L747–774: `animateDpAsState` → `.padding(innerPadding)` czytane w kompozycji (kafelek konta 100 dp na TV przy każdym ruchu D-pada).
- **Kategoria:** [1] (+ [4])
- **Wpływ:** Na TV każdy ruch D-pada = dwie zmiany fokusu → 2 rekompozycje wiersza/przycisku + realokacja `border`. Dla wiersza z `Text` to tanie; realny koszt rośnie dla kafelka konta (`AsyncImage` + padding co klatkę) i pola wyszukiwania. Skala: pojedyncze elementy, nie listy.
- **Przyczyna:** Wartość trafia do `Modifier` jako parametr (composition) zamiast być czytana w lambdzie `drawWithContent` / `layout` / `graphicsLayer`.
- **Rozwiązanie:** (a) obramowanie fokusu rysować w fazie draw, odczytując `hasFocus` w `drawWithContent`; (b) animowany padding zastąpić `Modifier.layout { … }` czytającym stan w bloku layout, albo – jeśli wygląd pozwala – skalą w `graphicsLayer { scaleX/scaleY }`. (b) zmienia wygląd, więc wymaga decyzji wizualnej.
- **Przykład (a):**
  ```kotlin
  // Obecnie (Modifiers.kt L62-74)
  var hasFocus by remember { mutableStateOf(false) }
  return this.onFocusChanged { hasFocus = it.isFocused }
      .whiteOutline(hasFocus = hasFocus, shape = shape)

  // Propozycja – odczyt w fazie draw
  val hasFocus = remember { mutableStateOf(false) }
  val color = MaterialTheme.colorScheme.onBackground
  return this.onFocusChanged { hasFocus.value = it.isFocused }
      .drawWithContent {
          drawContent()
          if (hasFocus.value) {
              drawOutline(shape.createOutline(size, layoutDirection, this),
                  color = color, style = Stroke(1.dp.toPx()))
          }
      }
  ```
  (Trzeba sprawdzić, czy geometria jest identyczna z `border()`, jeśli wygląd ma pozostać bez zmian.)
- **Priorytet:** Low-Medium
- **Pewność:** Prawdopodobny problem; skala – **wymaga profilowania na urządzeniu TV**.

---

### P-09 · `LazyListState.IsScrolling` czyta `firstVisibleItemIndex` w kompozycji całego `ExtensionsScreen.Content`

- **Problem:** Rozszerzenie `IsScrolling` czyta `firstVisibleItemIndex` jako klucz `LaunchedEffect` w kompozycji, więc scope wywołujący (`ExtensionsScreen.Content`) rekomponuje się przy każdej zmianie indeksu pierwszego widocznego elementu. Autor wyciszył ostrzeżenie `@Suppress("FrequentlyChangingValue")`.
- **Lokalizacja:** `LazyList.kt` L251–259; wywołanie: `ExtensionsScreen.kt` L559. Wariant dla siatki (`LazyGridState`, L242–250) czyta `lastScrolledBackward/Forward` jako klucz i **nie jest nigdzie użyty**.
- **Kategoria:** [1] + [4]
- **Wpływ:** Dodatkowe rekompozycje ciała `Content` przy przekraczaniu granic elementów. Dzięki Strong Skipping `LazyColumn` i jego lambda zapewne są pomijane, więc realny koszt to głównie wykonanie ciała `Content`. Stąd niski priorytet.
- **Przyczyna:** Odczyt szybko zmieniającego się stanu poza lambdą/`snapshotFlow`.
- **Rozwiązanie:** `snapshotFlow` czytający stan w efekcie oraz `rememberUpdatedState` dla callbacków.
- **Przykład:**
  ```kotlin
  @Composable
  fun LazyListState.IsScrolling(up: Runnable, down: Runnable) {
      val currentUp by rememberUpdatedState(up)
      val currentDown by rememberUpdatedState(down)
      LaunchedEffect(this) {
          snapshotFlow { firstVisibleItemIndex }.drop(1).collect {
              if (lastScrolledBackward) currentUp.run()
              if (lastScrolledForward) currentDown.run()
          }
      }
  }
  ```
- **Priorytet:** Low
- **Pewność:** Potwierdzony problem (kod), wpływ prawdopodobnie mały.
- **Uwaga poboczna (poprawność):** `ExtensionsScreen.kt` L559 podpina `lazyRepoState`, a przy `searchPlugins` lista używa innego `lazyState` (L547–551) – FAB nie reaguje na przewijanie listy wtyczek.

---

### P-10 · `StatusWrapper` opakowuje każdy wiersz preferencji w `AnimatedVisibility`

- **Problem:** Każdy `PreferenceItem` (także statyczny) przechodzi przez `AnimatedVisibility(visible = enabled, …)` + `CompositionLocalProvider`.
- **Lokalizacja:** `PreferenceItem.kt` L38–55.
- **Kategoria:** [4]
- **Wpływ:** `AnimatedVisibility` tworzy `Transition` per wiersz; w obecnych ekranach `enabled` jest stałe w czasie życia ekranu (`isLayout(...)`, `BuildConfig.FLAVOR`), listy krótkie i leniwe – koszt zapewne marginalny.
- **Przyczyna:** Ogólność (obsługa dynamicznego `enabled`; zakomentowane w `SubtitlesScreen.kt:322,334`).
- **Rozwiązanie:** **Nie zmieniać bez profilowania.** Jeśli profil pokaże koszt – odfiltrować statycznie wyłączone pozycje w `PreferenceScreen` (jak dla grup, L430).
- **Priorytet:** Low
- **Pewność:** Wymaga profilowania.

---

### P-11 · `PreferenceScreen`: brak kluczy w `items`/`item`, niespójny indeks podświetlenia

- **Problem:** `items(preference.preferenceItems) { … }` i `item { … }` bez `key`. Dodatkowo `findHighlightedIndex` liczy indeksy dla grup, które `LazyColumn` pomija (`if (!preference.enabled) return@fastForEachIndexed`).
- **Lokalizacja:** `PreferenceScreen.kt` L426–457 (items L437) oraz L462–474.
- **Kategoria:** [3]
- **Wpływ:** Dziś znikomy: listy statyczne, a `enabled` grup nie jest dynamiczne (żaden `PreferenceGroup(enabled = …)` w ekranach – sprawdzone grepem). Stan wierszy (`isDialogShown`, `highlightFlag`) jest związany z pozycją; przy dynamicznej zmianie listy stan „przeskoczyłby” do innego wiersza. Rozbieżność indeksu podświetlenia ujawni się dopiero przy wyłączonej grupie.
- **Przyczyna:** Domyślny klucz = indeks.
- **Rozwiązanie:** Klucz dodawać dopiero po sprawdzeniu unikalności – `LazyColumn` **crashuje przy duplikatach kluczy**, a tytuły preferencji nie mają gwarancji unikalności (mechanizm podświetlania zakłada ją, ale jej nie wymusza). Bezpieczny klucz: `"$groupTitle/${item.title}"` + test na duplikaty. `findHighlightedIndex` powinien pomijać grupy z `enabled == false`.
- **Priorytet:** Low
- **Pewność:** Potwierdzony problem (kod), bez realnego wpływu na wydajność dziś.

---

### P-12 · `PreferenceData.collectAsState()` wylicza `initial = get()` przy każdej rekompozycji

- **Problem:** `flow.collectAsState(initial = get())` – argument `get()` jest wykonywany przy każdej rekompozycji wywołującego composable, choć `initial` jest używany tylko raz.
- **Lokalizacja:** `Preference.kt` L176–180; użycia: `PreferenceItem.kt` L69, 86, 104, 123, 151, 173; `SettingsItems.kt:137`; `ComposeFragment.kt` L36–38.
- **Kategoria:** [4]
- **Wpływ:** Pojedynczy odczyt preferencji na rekompozycję wiersza – prawdopodobnie pomijalny; istotny tylko jeśli `get()` deserializuje (np. `Set<String>`).
- **Rozwiązanie:** `flow.collectAsState(initial = remember(this) { get() })`. Zmiana mała i bezpieczna, ale **uzasadniona dopiero po sprawdzeniu kosztu `PreferenceData.get()`** (nie badałem implementacji dla wszystkich typów).
- **Priorytet:** Low
- **Pewność:** Wymaga profilowania.

---

### Obserwacje bez priorytetu (poprawność / kontrakty)

- `DerivedRepositoryState` jest `@Immutable`, ale zawiera `IntArray` i własne `equals`, które **pomija `languageCount`** (`ExtensionsViewModel2.kt` ~L224–260). Jeśli zmieni się wyłącznie rozkład języków przy tych samych licznikach, Compose uzna stan za równy i nie odświeży listy języków. Kontrakt `@Immutable` wymaga potwierdzenia, że `countOfTypes` nigdy nie jest mutowane po utworzeniu – **nie dodawać ani nie usuwać adnotacji bez tego sprawdzenia**.
- `ExtensionsScreen.kt` L547–551: `rememberLazyListState()` wywoływane warunkowo (`if (state.searchPlugins)`). Działa, a reset przewijania przy zmianie trybu jest zamierzony (komentarz autora) – tylko odnotowuję.

---

## 2. Odpowiedzi na dodatkowe pytania dla Android TV

| Pytanie | Wynik |
|---|---|
| Czy zmiany fokusu powodują nadmierne rekompozycje? | Umiarkowanie: każdy `focusOutline()` rekomponuje swój scope przy zmianie fokusu (P-08). Nie obejmuje całych ekranów ani list – tylko pojedyncze wiersze/przyciski. |
| Czy animacje fokusu są wydajne? | Animacje paddingu (pole wyszukiwania, kafelki kont) są czytane w composition (P-08). Animacja `alpha` list rozszerzeń jest zrobiona poprawnie przez `graphicsLayer { }` (`ExtensionsScreen.kt:652,778`). |
| Czy otwieranie/zamykanie menu blokuje UI? | Dialogi Compose (`SingleSelectDialog`, `MultiSelectDialog`, `ActionDialog`) są lekkie. Wyjątki: `LogcatDialog` (P-06) i pierwsze użycie wyszukiwarki ustawień (P-03). Menu/dialogi playera – poza Compose. |
| Czy przewijanie list jest płynne? | Listy Compose mają poprawne klucze (sekcja 5). Ryzyka: P-02 (wyszukiwanie), P-01 (postęp pobierania w trakcie przewijania). |
| Czy ładowanie obrazów powoduje zbędne aktualizacje UI? | Compose używa Coil `AsyncImage` w 4 miejscach (ikony 24 dp, awatary); `model` to stabilny String/Int, a rozmiar ikon kwantyzowany do potęg dwójki (`remapUrl`, ~L1906–1916). Obrazy w Home/Result/Search to Views – poza zakresem. |
| Czy aktualizacje ExoPlayera odświeżają całe ekrany? | Nie dotyczy: player nie jest w Compose. |
| Czy elementy niewidoczne wykonują zbędną pracę? | Nie w `LazyColumn` (komponowane tylko widoczne). Wyjątek: P-03 (wyszukiwarka komponuje wszystkie ekrany ustawień). |

---

## 3. Podsumowanie najważniejszych problemów

1. **P-02** – wyszukiwanie wtyczek bez debounce, z kosztownym sortowaniem Levenshteina w reducerze na Main (istniejący `DebounceQuery` nieużywany).
2. **P-01** – brak throttlingu postępu pobierania wtyczek → setki aktualizacji stanu, rekompozycja całego wiersza.
3. **P-03** – pierwszy znak w wyszukiwarce ustawień komponuje wszystkie ekrany ustawień na Main i uruchamia ich efekty.
4. **P-06** – `LogcatDialog`: start procesu na Main, `withContext(IO)` na rekord, niezniszczony `Process`.
5. **P-04 / P-05** – przebudowa całej listy preferencji przy zmianie odczytanego stanu; lista języków liczona w kompozycji.
6. **P-08** – animacje i obramowania fokusu czytane w fazie composition (TV).
7. Reszta (P-07, P-09–P-12) – niski wpływ.

**Ogólna ocena:** warstwa Compose jest zbudowana dojrzale (niezmienne stany MVI z kolekcjami persistentnymi, klucze UUID w listach, odroczony odczyt animacji w `graphicsLayer`, poprawny cykl życia efektów). Zakres Compose jest mały i dotyczy ekranów rzadko używanych; **problemów Critical nie znalazłem**. Najważniejsze obszary wydajności aplikacji (player, home, szczegóły, wyszukiwanie, siatki posterów) nie są w Compose i nie były przedmiotem tego audytu.

---

## 4. Ranking problemów według wpływu na wydajność

| # | ID | Priorytet | Uzasadnienie |
|---|---|---|---|
| 1 | P-02 | High | Praca CPU-bound na Main przy każdym znaku; skaluje się z liczbą wtyczek |
| 2 | P-01 | High | Setki aktualizacji stanu i rekompozycji na pobranie; nakłada się z P-02 (ponawianie reducera) |
| 3 | P-03 | Medium | Jednorazowy skok na Main + niepotrzebne efekty uboczne |
| 4 | P-06 | Medium | Praca na Main + nieposprzątany proces, ale rzadko używane |
| 5 | P-04 | Medium-Low | Przebudowa listy po interakcji |
| 6 | P-07 | Medium-Low | Zapis przy każdym kroku suwaka |
| 7 | P-08 | Low-Medium | Rekompozycje fokusu (TV), pojedyncze elementy |
| 8 | P-05 | Low-Medium | Obliczenia pochodne w kompozycji |
| 9 | P-09 | Low | Dodatkowe rekompozycje przy przewijaniu |
| 10 | P-10, P-11, P-12 | Low | Marginalne / potencjalne |

---

## 5. Elementy zaimplementowane poprawnie (nie zmieniać)

- **Klucze w listach**: `ExtensionsScreen.kt` L617, 622, 641, 759, 764, 813 (`key = uuid` / `"header"` / `"filter"` / `text`); `TestScreen.kt` L227, 231, 235–237, 328, 344; `LogcatDialog.kt` L99 (`item.uuid`); `Dialog.kt` L158, 383 (`item(key = key)`). Wzorzec `UniqueItem` z losowym UUID (`Viewmodel.kt:216–235`) jest stabilny i nie używa indeksów.
- **Modelowanie stanu**: `PersistentMap/List/Set` z `sorted`/`filtered` jako kluczami zamiast kopiowania obiektów (`ImmutableState.kt`); `SearchableData.updating` unika ponownego sortowania, gdy kolejność się nie zmienia (~L363–420).
- **Fazy**: animacja `alpha` przez `graphicsLayer { alpha = itemAlpha.value }` z `Animatable` zapamiętanym per repozytorium (`ExtensionsScreen.kt` ~L586–596, 652, 778); pozycja uchwytu pickera przez `Modifier.offset { }` (`RingColorPicker.kt`); kolory pickerów w `drawWithCache` odczytujące `() -> Float` w fazie draw; `CircularProgressIndicator(progress = { … })` w lambdzie. (Drobna uwaga: domyślny `thumb` pickera czyta `hue()` w kompozycji – tylko uchwyt, pomijalne.)
- **Efekty uboczne**: `ObserveEffect` z `repeatOnLifecycle(STARTED)` i `Dispatchers.Main.immediate` (`Viewmodel.kt:96–104`); `rememberUpdatedState` dla callbacków w pickerach i `pointerInput(Unit)`; `DisposableEffect` z `keyboardController?.hide()` (`SettingsFragmentScreen.kt` ~L425–429); `onDispose` w ekranach napisów czyta aktualny `state` przez delegat (nie przechwytuje starej wartości); `LaunchedEffect(highlighted)` w `BasePreferenceWidget.kt:95` z poprawnym kluczem.
- **Wątki**: zadania `ExtensionsViewModel2` na `Dispatchers.IO` przez `SingleActiveQuery/MultiActiveQuery` (L125–128); pomiar rozmiaru cache w `withContext(Dispatchers.IO)` (`SettingsPlayerScreen.kt:57–60`); `DebounceQuery` z `flowOn(Dispatchers.Default)`; zapis logów w `withContext(IO)` (`LogcatDialog.kt:113`).
- **Zakres stanu**: ViewModele nie są przekazywane w dół – `ExtensionsScreen.Content(state, onAction)` dostaje stan i jedną lambdę; `viewModel<…>{}` tylko w korzeniu ekranu (`ExtensionsScreen.kt:251`, `TestScreen.kt:121`).
- **Wyniki wyszukiwania ustawień** zachowywane przez `retain` (nie są liczone ponownie po powrocie, `Search.kt:545`) i ograniczone `take(10)`.
- **Obrazy**: `remapUrl` kwantyzuje rozmiar do 16/32/…/512 px, co pozwala Coil trafiać w cache.

---

## 6. Rekomendowana kolejność wdrażania

1. **Profilowanie bazowe** (sekcja 7) – zanim cokolwiek zmieniać, zapisać liczby dla P-01, P-02, P-03, P-08.
2. **P-02** (debounce + cache `score`) – największy zysk, mała zmiana, istniejący kod pomocniczy.
3. **P-01** (throttling postępu) – kilka linii w `downloadPlugin`; po zmianie ponownie zmierzyć rekompozycje `Plugin`.
4. **P-06** (`LogcatDialog`) – izolowana zmiana, niskie ryzyko.
5. **P-04 + P-05** – odczyty stanu do wierszy / `remember` z uczciwym kluczem.
6. **P-07** – debounce + IO dla zapisu stylu napisów (po weryfikacji sygnatury `SideEffect(state)`).
7. **P-03** – decyzja po pomiarze; ewentualnie osobna zmiana modelu `SearchableSettings`.
8. **P-08** – po pomiarze na prawdziwym TV; zmiana wizualna wymaga akceptacji.
9. **P-09–P-12** – tylko jeśli profil to uzasadni (P-11: z testem unikalności kluczy).
10. **Osobny audyt View/RecyclerView** dla playera, Home, Result, Search, Download (sekcja 0.1) – tam leży większość realnego kosztu UI aplikacji.

---

## 7. Problemy wymagające profilowania (nie rozstrzygalne statycznie)

| ID | Co zmierzyć | Narzędzie |
|---|---|---|
| P-01 | Liczba rekompozycji `Plugin`/`PluginWidget` i czas reducera podczas pobierania 1 i N wtyczek | Layout Inspector (recomposition counts), Perfetto |
| P-02 | Czas obsługi `ExtensionsAction.Search` na realnej liście wtyczek, słabe urządzenie TV | Perfetto + `trace("search")`, Macrobenchmark z `JankStats` |
| P-03 | Czas pierwszej kompozycji wyników wyszukiwarki ustawień po wpisaniu 1 znaku | Perfetto (`Choreographer#doFrame`), Compose tracing |
| P-07 | Czas i liczba wywołań `saveStyle` podczas przeciągania suwaka | Method trace w Android Studio Profiler |
| P-08 | Rekompozycje i czas klatek przy przemieszczaniu fokusu D-pada (ekran kont, ustawienia) | Layout Inspector + Macrobenchmark na urządzeniu z Android TV |
| P-10, P-12 | Koszt `AnimatedVisibility` per wiersz i `PreferenceData.get()` | Compose Compiler Metrics + Perfetto |

**Przydatne ustawienia (nie uruchamiane w tym audycie):** raporty kompilatora Compose, np. w module z Compose:
```kotlin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}
```
Pliki `*-composables.txt` / `*-classes.txt` potwierdzą, które composable są faktycznie `restartable skippable` i czy Strong Skipping jest aktywny, zanim ktokolwiek rozważy `@Stable`/`@Immutable`.
