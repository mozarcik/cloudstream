# CloudStream TV — zasady dla agentów

Nowy UI na Android TV: Kotlin + Jetpack Compose (`androidx.tv.material3`),
kod w `app/src/main/java/com/lagradost/cloudstream3/tv`.

## Priorytety (w tej kolejności)
1. Płynność — brak jank w nawigacji D-pad i w overlayach playera.
2. Prostota i czytelność kodu (KISS > SOLID).
3. Spójność wizualna.

Zasady niżej to domyślne praktyki, nie dogmaty. Jeśli któraś komplikuje kod
bez realnego zysku — odstąp i krótko napisz dlaczego (w odpowiedzi, nie w kodzie).

## Zakres zmian
- Domyślnie edytuj tylko pakiet `tv/`. Mosty do starego kodu → `tv/compat` lub `tv/data/mappers`.
- Zmiana poza `tv/` — najpierw zapytaj.
- Ekrany: `tv/presentation/screens/<feature>`, ViewModel w tym samym pakiecie.
  Nowa trasa: `Screens.kt` + rejestracja w `TvApp.kt`.
- Teksty widoczne dla użytkownika → `strings.xml`.

## Architektura
- Composable przyjmuje `state` + callbacki, bez logiki biznesowej; stan hoistowany do Route/ViewModelu.
- ViewModel wystawia jeden niemutowalny stan (por. `COMPOSE.md`). Dziel ViewModel,
  gdy ma kilka niezależnych odpowiedzialności — nie wcześniej.
- ViewModel nie woła innego ViewModelu bezpośrednio. `MediaSource`/`ExoPlayer` nie powstają w UI.

## Compose i wydajność
- Lazy listy/gridy: zawsze stabilny `key`. Mapowania danych w VM, nie w `items {}`.
- Nie twórz w ciele composable obiektów kosztownych lub tworzonych przy każdej klatce
  (formattery, `ImageRequest`, listy, `Brush` z gradientem) — użyj `remember` / przenieś do VM.
- Szybko zmieniający się stan (pozycja odtwarzania, focus) czytaj możliwie nisko w drzewie
  albo w fazie layout/draw (lambda w `Modifier.offset {}`, `graphicsLayer {}`, `drawBehind {}`).
- Projekt ma Kotlin 2.x (strong skipping włączony) — nie dodawaj `@Stable`/`@Immutable`
  ani `remember` wokół lambd „na zapas”; dodaj, gdy Layout Inspector pokazuje zbędne rekompozycje.
- Obrazy w listach: `AsyncImage`, `crossfade(false)` w gęstych gridach.

## Player (ExoPlayer)
- Player i `AndroidView`/`PlayerView` tworzone raz; nie mogą być odtwarzane przy zmianie
  widoczności overlayu, timeline'u ani focusu.
- Overlay to osobne poddrzewo; animuj `alpha`/`graphicsLayer`/`offset`, nie layout.
- Aktualizacje pozycji co ~250–500 ms, izolowane w komponencie timeline'u.
- Unikaj blur i wielu półprzezroczystych warstw nad wideo.

## Animacje na TV
- Bez animacji layoutu (rozmiar/pozycja karty, `animateContentSize`, `AnimatedVisibility` ze `slide*`)
  przy każdym ruchu focusu w listach/gridach.
- Nową animację w itemach Lazy* sprawdź w trace (ON vs OFF): bez wzrostu janky frames
  i `measureAndLayout`.

## UX na TV
- D-pad first: zdefiniuj początkowy focus, kierunki i przywracanie focusu; bez pułapek focusu.
- Wyraźny, kontrastowy stan focusu.
- Back: najpierw zamyka overlay/panel, potem cofa ekran.

## Design system
- Tokeny: `TvDimens`, `TvColors`, `TvShapes`, `TvTokens.kt` (+ lokalne `*Tokens` ekranów).
- Wartości powtarzalne lub semantyczne (odstępy siatki, kolory marki, kształty kart, focus)
  bierz z tokenów; jednorazowa wartość lokalna może zostać literałem.

## Walidacja
- Po istotnych zmianach: `./gradlew :app:assemblePrereleaseDebug`.
- Jeśli czegoś nie uruchomiłeś (testy, emulator) — napisz to wprost.

## Analiza jank
Przy zgłoszonym problemie z płynnością użyj skilla `tv-perf-analysis`
(analiza rekompozycji, stabilności, AndroidView, animacji, alokacji + trace).
