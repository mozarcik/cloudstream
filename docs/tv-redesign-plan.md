# CloudStream TV: plan implementacji redesignu

> Plik dla agenta AI. Czytaj go w całości przed pierwszym zadaniem. Po każdym zadaniu aktualizuj checkboxy w tym pliku.

## Źródła prawdy

- **Makiety (kanwa Design):** https://claude.ai/artifact/85SkauiKqwBDSCWS4frb8v
  48 ekranów 1920×1080 w rzędach: Home, Wyszukiwanie, Szczegóły filmu, Szczegóły serialu, Ustawienia, Biblioteka, Pobrane, Player, Komponenty. Przy każdym rzędzie jest notatka z obsługą pilotem. Plansze „…States” zawierają stany (ładowanie, pusto, błąd, warianty).
- **Opis decyzji projektowych:** `claude/tv-redesign-ux.md` (plik projektu). Jeśli makieta i opis się różnią, **makieta wygrywa**, a różnicę zgłoś użytkownikowi.
- **Aplikacja:** TV wersja CloudStream (Jetpack Compose, repo lokalne `cloudstream`), urządzenie docelowe: Chromecast with Google TV.
- **Kod nowego layoutu (Material):** aplikacja ma ustawienie „App layout” i istniejący, rozpoczęty (WIP) widok Material. To on jest modyfikowany w ramach redesignu.
  **Katalog z komponentami Material:** `app/src/main/java/com/lagradost/cloudstream3/tv` (wskazany przez użytkownika; wszędzie w tym pliku `<KATALOG_MATERIAL>` = ten katalog). Testy tego kodu leżą w `app/src/test/.../tv` i `app/src/androidTest/.../tv`.

### Jak czytać makiety
1. Otwórz artefakt narzędziem do odczytu artefaktów (`Artifact`, action `read`) i skopiuj pliki do kontenera. Z kodu makiet wyciągnij dokładne wartości: kolory, rozmiary czcionek, odstępy, marginesy, wymiary kart i przycisków, teksty etykiet.
2. Jeśli nie masz dostępu do linku, **zatrzymaj się i poproś użytkownika** o eksport ekranów jako PNG do `docs/mockups/` oraz o kod/tokeny. Nie zgaduj wyglądu.
3. Przy każdym zadaniu otwórz wskazany rząd/planszę i porównuj ekran z implementacją.

## Zasady nadrzędne (obowiązują w każdym zadaniu)

1. **Dokładne odwzorowanie makiet**: układ, rozmiary, odstępy, kolory, typografia, teksty etykiet, kolejność elementów i stany. Wartości bierz z makiet, nie z domyślnych ustawień Compose. Przy 1920×1080 jeden dp odpowiada makietowemu pikselowi, o ile projekt nie ma innego skalowania. Sprawdź to w zadaniu 0.3.
2. **Obsługa wyłącznie pilotem (D-pad).** Brak long-pressa i brak opcji dostępnych tylko przez dotyk. Rzadkie opcje trafiają do panelu „More”.
3. **Jedna reguła focusu** w całej aplikacji:
   - przyciski, taby, wiersze: biały fill,
   - karty: ring 4 px + skala 1.05 (przyciski i wiersze bez skali),
   - bez cieni i blurów,
   - animacje transform/alpha ≤ 150 ms.
4. **Nie dodawaj funkcji, których aplikacja jeszcze nie ma.** Jeśli element makiety wymaga brakującej logiki, nie implementuj jej na własną rękę. Zapisz to w sekcji „Rozbieżności i pytania” na końcu pliku i przejdź dalej (element pomiń lub zrób nieaktywny, jeśli użytkownik tak zdecyduje).
5. **Lekko i szybko**: bez ciężkich animacji, uważaj na recomposition przy zmianie focusu i na lazy listy. Docelowo ma być płynnie na Chromecaście.
6. **Stany należą do ekranu.** Ekran nie jest ukończony, dopóki nie ma stanów z odpowiedniej planszy „…States” (ładowanie, pusto, błąd, warianty).
7. **Zakres zmian: kod Material to Twoje miejsce pracy, reszta to legacy.** Redesign robisz przede wszystkim przez modyfikację istniejących widoków Material w `<KATALOG_MATERIAL>`. Przełączanie stary/nowy layout realizuje istniejące ustawienie „App layout”, więc **nie dodawaj własnej flagi**. Cały pozostały kod traktuj jako legacy:
   - nie przepisuj go i nie refaktoryzuj,
   - zmieniaj tylko to, co konieczne do wpięcia się (dostęp do danych, nawigacja, rejestracja ekranu), minimalnie i punktowo,
   - cel: brak lub niewielkie konflikty przy merge z upstream. Preferuj adaptery i wrappery wewnątrz katalogu Material zamiast edycji w legacy,
   - każdą zmianę poza katalogiem Material wpisz do sekcji „Zmiany poza katalogiem Material” (plik, powód, rozmiar zmiany).
8. **Płaskie drzewo UI po wyrenderowaniu.** Zbyt głębokie drzewa kosztują wydajność na słabych urządzeniach. Nie twórz kontenerów tylko do grupowania lub stylowania; używaj modyfikatorów (padding, rozmiar, tło) na elemencie zamiast owijania go w kolejny `Box`/`Column`/`Row`.
   - Przykład: feed z tytułem i dwoma wierszami to **jeden** kontener z tytułem i gridem w środku, a nie box feedu → box tytułu → boxy wierszy.
   - Elementy układaj płasko (np. tytuł i wiersze jako kolejne elementy jednej listy), unikaj zagnieżdżania przewijalnych kontenerów, gdy da się to zrobić płasko.
   - Po zrobieniu ekranu sprawdź głębokość drzewa (Layout Inspector lub przegląd kodu) i spłaszcz zbędne poziomy.
9. **Wspólne komponenty bazowe zamiast powtarzania.** Typografia, wymiary i stany focusu mają być zdefiniowane w jednym miejscu (tokeny i bazowe komponenty: menu, przyciski, wiersze, karty, panele, dialogi itd.), a nie powtarzane na ekranach.
   - Przed implementacją przejrzyj makiety i zrób inwentarz powtarzalnych elementów (zadanie 0.6).
   - **Komponent bazowy tworzysz tylko wtedy, gdy jest używany w co najmniej 3 miejscach.** Element użyty raz lub dwa razy (np. taby w szczegółach filmu) robisz lokalnie przy ekranie, ale nadal z tokenów i wspólnej reguły focusu, bez własnych liczb.
   - Najpierw sprawdź, czy dany komponent już istnieje w kodzie Material (WIP). Jeśli tak, zmodyfikuj go zamiast tworzyć drugi.

## Protokół pracy agenta

- Pracuj **jedno zadanie naraz**, w kolejności z pliku (zadania mają zależności).
- Po zakończeniu zadania:
  1. sprawdź wszystkie punkty „Gotowe gdy”,
  2. zmień `- [ ]` na `- [x]` przy zadaniu **oraz** przy jego spełnionych podpunktach,
  3. dopisz pod zadaniem jedną linię `Notatki:` z istotnymi decyzjami lub odchyleniami (opcjonalnie),
  4. zrób osobny commit: `tv-redesign: <ID zadania> <krótki opis>`.
- Zadania częściowo zrobione zostają `- [ ]`, a w `Notatki:` opisz, co zostało.
- Gdy zadanie jest zablokowane (brak dostępu do makiety, brak backendu, niejednoznaczność), **nie obchodź problemu**: dopisz wpis w „Rozbieżności i pytania”, zatrzymaj się i zapytaj użytkownika.
- Nie zmieniaj kolejności ani zakresu planu bez zgody użytkownika.
- Weryfikacja: kompilacja i testy muszą przechodzić; ekran sprawdzaj na emulatorze Android TV w 1080p, a kryteria pilota i wydajności oznacz jako wymagające sprawdzenia przez użytkownika na Chromecaście, jeśli nie masz do niego dostępu.

## Definicja ukończenia ekranu („DoD”)

Każde zadanie ekranowe jest ukończone, gdy:
- [ ] wygląda jak makieta (porównanie 1:1 z odpowiednim rzędem, w tym rozmiary i odstępy),
- [ ] zawiera wszystkie stany z planszy „…States”,
- [ ] cały ekran obsługuje się samym D-padem, a Back zachowuje się przewidywalnie i zgodnie z notatką przy rzędzie,
- [ ] focus jest poprawny: startowy focus, powrót na ostatni element po Back, brak „pułapek” focusu,
- [ ] brak long-pressa, cieni, blurów i animacji dłuższych niż 150 ms,
- [ ] drzewo UI jest płaskie (brak zbędnych kontenerów, zasada 8),
- [ ] ekran korzysta z tokenów i wspólnych komponentów, bez zduplikowanych wartości typografii, wymiarów i focusu (zasada 9),
- [ ] zmiany poza katalogiem Material są zerowe lub minimalne i wpisane do sekcji „Zmiany poza katalogiem Material”,
- [ ] brak widocznych przycięć w przewijaniu (zgłoś, jeśli nie możesz zmierzyć).

(W zadaniach poniżej DoD jest skrócone do „DoD ekranu”.)

---

## Etap 0: Przygotowanie

- [x] **0.1 Audyt istniejącego kodu TV**
  Zmapuj każdy rząd makiet (9) na istniejące widoki Material (WIP) w `<KATALOG_MATERIAL>`. Dla każdego wpisz: dokończyć / przebudować w Material / brak widoku (do utworzenia w Material) oraz punkty styku z legacy (skąd dane, jak rejestracja ekranu i nawigacja). Sprawdź m.in., jak działa obecnie focus i czy projekt używa `androidx.tv`.
  Wynik: sekcja „Wyniki audytu” w tym pliku (tabela: rząd makiet → widok Material → punkt wpięcia w legacy → decyzja → uwagi).
  Gotowe gdy: tabela jest uzupełniona dla wszystkich 9 rzędów.
  Notatki: Home i Dashboard mają już niezacommitowane zmiany użytkownika (21 plików, WIP w stronę redesignu: Continue watching hero, Featured, mini-grid, tokeny Home). Mój commit obejmuje wyłącznie `docs/tv-redesign-plan.md`, nie dotykam cudzych zmian. Makiety mają 10. rząd („Wybór profilu”), którego nie ma w planie, patrz „Rozbieżności i pytania” #7.

- [x] **0.2 Rozpoznanie ustawienia „App layout” i katalogu Material**
  Potwierdź katalog `<KATALOG_MATERIAL>` z użytkownikiem. Ustal, jak ustawienie „App layout” wybiera layout i gdzie ekrany Material są rejestrowane. Spisz punkty styku Material z legacy (nie dodawaj własnej flagi).
  Gotowe gdy: aplikacja buduje się, a po wybraniu layoutu Material uruchamia się obecny WIP.
  Notatki: ścieżka katalogu podana przez użytkownika w poleceniu. Szczegóły mechanizmu w „Wyniki audytu → App layout”. Uwaga: `./gradlew :app:assemblePrereleaseDebug --offline` kończy się błędem javac (databinding), bez `--offline` buduje się poprawnie.

- [x] **0.3 Odczyt makiet i ekstrakcja tokenów**
  Odczytaj makiety (patrz „Jak czytać makiety”). Spisz w sekcji „Tokeny” w tym pliku: paleta kolorów, skala typografii, odstępy, safe-area/overscan, wymiary kart (poster, odcinek, itd.), skalowanie dp↔px.
  Gotowe gdy: każda wartość używana w zadaniach 1.x ma odpowiednik w makiecie.
  Notatki: źródło tokenów to rzędy Components, Main, Movie*, Series*, Settings*, Downloads, Player*. Dwie wartości w makiecie są niespójne, patrz „Rozbieżności i pytania” #3 i #4.

- [x] **0.4 Lista rozbieżności makiet z backendem**
  Przejdź po makietach i wypisz elementy, które wymagają logiki, której aplikacja nie ma (kandydaci: panel mirrorów z „Skip Loading”, przełącznik źródła list w Bibliotece, sync napisów z listą dialogów, napisy online z logowaniem, zapowiedź `nextAiring`). Wpisz je do „Rozbieżności i pytania”. **Nie implementuj ich** przed decyzją użytkownika.
  Gotowe gdy: lista jest wpisana, a użytkownik ją zobaczył.
  Notatki: lista w „Rozbieżności i pytania”; wszystkie 7 punktów rozstrzygnięte przez użytkownika. Większość kandydatów z planu **ma już backend** (sprawdzone w kodzie); realne otwarte kwestie to #1–#7. Decyzje użytkownika potrzebne przed 1.1 (#1, #3, #4) oraz przed 2.14 (#2), 2.9 (#5), 3.1 (#6).

- [x] **0.5 Baseline wydajności**
  Zapisz bieżące pomiary (czas startu, płynność przewijania Home), jeśli da się je zmierzyć. Jeśli nie, zanotuj, że pomiar zrobi użytkownik.
  Notatki: pomiar na emulatorze w buildzie debug, więc liczby są pesymistyczne i służą tylko do porównania przed/po. Pomiar na Chromecaście zrobi użytkownik.

- [x] **0.6 Inwentarz wspólnych komponentów**
  Przejrzyj wszystkie makiety i wypisz powtarzalne elementy (menu, przyciski, taby, wiersze, karty, panele, dialogi, slider itd.) wraz z liczbą miejsc użycia i wspólnymi parametrami (typografia, wymiary, stany focusu). Sprawdź, które z nich już istnieją w kodzie Material. Wynik: sekcja „Inwentarz komponentów” w tym pliku.
  Gotowe gdy: dla każdego elementu jest decyzja: bazowy komponent (≥3 użycia) albo lokalnie przy ekranie (1–2 użycia).
  Notatki: zadania 1.8 (slider/multi-select) i część 1.3 (taby) wychodzą z inwentarza jako „lokalnie przy ekranie”.

---

## Etap 1: Fundament

Makieta: rząd **Komponenty** (+ plansze stanów). Zadania 1.x blokują wszystko poniżej.

Zakres 1.3–1.9 wynika z inwentarza (0.6) i zasady 9. Komponent, który ma mniej niż 3 użycia, nie dostaje bazy: oznacz zadanie `[x]` z notatką „pominięte: lokalnie przy ekranie”. Jeśli komponent już istnieje w kodzie Material, modyfikuj go zamiast tworzyć nowy. Wszystko w `<KATALOG_MATERIAL>`.

- [x] **1.1 Design tokeny**
  Kolory, typografia, odstępy, safe-area/overscan, wymiary, zgodnie z sekcją „Tokeny”.
  Gotowe gdy: żaden ekran nie używa „magicznych liczb”, tylko tokenów.
  Notatki: jedno źródło tokenów: `theme/TvTokens.kt` (`TvColors`, `TvDimens`, `TvShapes`, `TvTextStyles`, dp = px/2); Figtree (`FigtreeFontFamily`, variable wght 400-800) zastępuje Inter w całej typografii, `InterFontFamily` zostaje jako alias, żeby nie ruszać plików Home/Dashboard; `CloudStreamTokens` deleguje do `TvColors`; `CloudStreamCardShape` = r7 dp; usunięty `glow` z `CloudStreamSurfaceDefaults`. Skala Material `Typography` (rozmiary) bez zmian, role z makiet są w `TvTextStyles`. Ekrany nadal mają własne `*Tokens.kt` (Home), migracja w etapie 2. Fix: dodane `LabelPanel` #8790A0 (panele/Components) obok `Label` #8E95A3 (ekrany), tokeny radio, role tekstu (Hero film/featured/player/serial, tytuł odcinka, pobrane, player, `RowPanel` 13 sp, `ShowMoreTitle`, `StateTitle/Message`), wymiary paneli/dialogu/stanów/mini-grida w `TvDimens`.

- [x] **1.2 Reguła focusu (modyfikatory)**
  - wariant „biały fill” (przyciski, taby, wiersze),
  - wariant „ring 4 px + skala 1.05” (karty),
  - animacje transform/alpha ≤ 150 ms, bez cieni i blurów.
  Gotowe gdy: oba warianty działają w galerii (1.9) i wyglądają jak w makiecie.
  Notatki: modyfikatory w `focus/FocusStyles.kt`: `tvFocusFill` (biały fill, bez skali) i `tvCardFocus` (ring 2 dp + offset 2,5 dp rysowany poza granicami, scale 1.05, tween 120 ms, bez cieni). Ring wychodzi poza kartę, więc rodzic nie może go przycinać (Lazy* z contentPadding). Galeria: 1.11.

- [x] **1.3 Przycisk i taby**
  Przycisk z etykietą i stałą szerokością (zgodnie z makietą), tab, wszystkie warianty i stany (focus, nieaktywny, wciśnięty).
  Notatki: baza: `common/TvButton.kt` (pill 34/36 dp, ikona + etykieta, opcjonalna stała szerokość, warianty Default/Primary/Destructive + disabled, jedna `Row`) i `common/TvChip.kt` (na `FilterChip` + `DesignChipStyle`, obramowanie 1 dp zamiast 2 dp, bez skali). `DesignButtonStyle`/`DesignChipStyle` zostają dla starych miejsc użycia i biorą wartości z `TvColors`; kolory focusu idą ze schematu (`inverseSurface`), żeby szczegóły filmu/serialu zachowały kolory dynamiczne. Taby: pominięte, lokalnie przy ekranie Szczegóły (inwentarz). Wciśnięty = ten sam biały fill co focus.

- [x] **1.4 Wiersz listy**
  Wiersz z akcjami (jak w odcinkach, Pobranych, Ustawieniach), warianty i stany focusu.
  Notatki: baza: `common/TvListRow.kt` (tytuł, podtytuł, ikona/leading, trailing: None/Chevron/Check/Radio/Toggle/Value/Custom, stan selected = tint akcentu, disabled; focus = biały fill bez skali) + `TvRadioIndicator`/`TvToggleIndicator`. `MenuListSidePanel` używa teraz `TvListRow` zamiast tv `ListItem` (tytuł panelu, nagłówki, style, animacje 150 ms). `settings/SettingsPanelComponents` (świat Ustawień) nadal ma własne wiersze: scalenie przy ekranie Ustawień w etapie 2. Fix: radio po lewej (`TvRowLeading.Radio`, ramka #5A6170 / #4A5160 na focusie / #9CC1FF), `Trailing.Radio` zostaje dla PlayerVideo; panele używają `RowPanel` 13 sp, chevron 14 dp.

- [x] **1.5 Karta (poster / odcinek)**
  Karty z makiety, w tym wskaźniki (postęp, status pobierania, jeśli występują w makiecie).
  Notatki: baza: `PosterCard` (ring 2 dp + offset + scale 1.05 przez `tvCardFocus`, placeholder #1E222B, usunięty `onLongClick`) + `common/TvProgressBar.kt` (3 dp, jeden węzeł). Karty CW/odcinka/pobranego zostają lokalnie przy ekranach (inwentarz). Tytuł pod kartą nadal pokazuje się przy focusie (makieta pokazuje tytuł w nagłówku sekcji, zmiana przy Home w 2B). Fix: `PosterCard` domyślnie bez tytułu i plakietki (`showTitle`/`showRating` opt-in), jeden węzeł focusu (clickable na plakacie zamiast Box + Surface). Uwaga dla 2B: `cardHeight` przekazywany do `FeedMiniGrid` ma być wysokością plakatu.

- [x] **1.6 Mini-grid 2×6**
  Mini-grid używany w Home i Bibliotece: 11 kart + „Show more” przy więcej niż 12 elementach. Nawigacja D-padem po siatce.
  Notatki: istniejący `screens/home/FeedMiniGrid.kt` (11 kart + Show more przy >12, nawigacja D-padem, 6 kolumn) jest już bazą dla Home; zmodyfikowany `ShowMoreCard` wg makiety (panel #171A21, obramowanie 1 dp, kółko ze strzałką, tytuł, opcjonalny `subtitle`, focus ring + scale 1.05). Podtytuł „All of <źródło>” nie jest jeszcze przekazywany z `FeedSection` (plik Home w WIP użytkownika, etap 2B). `FeedMiniGridEmptyOrError` ma jeszcze stary focus i zostanie zastąpiony stanami z 1.9 w 2B; Biblioteka ma własną siatkę, podpięcie w 2E. Fix: odstęp wierszy 16 dp (`TvDimens.MiniGridGap`), wysokość siatki liczona w `FeedMiniGrid`, `ShowMoreTitle`, puste/błąd z regułą focusu (ring bez skali). Podtytuł Show more nadal do przekazania w 2B.

- [x] **1.7 Dialog i panel boczny**
  Dialog (np. potwierdzenia) i panel (np. „More”, status, mirrory, panele Playera). Focus uwięziony w panelu, Back zamyka.
  Notatki: baza: `TvConfirmDialog` (560 dp po przeliczeniu miniatury makiety, #22262F, r20, tylko fade 120 ms, przyciski `TvButton`, nowe parametry `destructive` i `focusSecondaryFirst`; Delete na Pobranych startuje na Cancel jak w makiecie) i `SlidingSidePanel` (340 dp, scrim .64, tło #171A21 na statycznym motywie, a na ekranach z dynamicznymi kolorami `surface`, jedna `Box` zamiast `Surface`, `tvFocusTrap`, Back zamyka, animacje 150 ms); `MenuListSidePanel` dziedziczy panel. Wymiary dialogu wyliczone z miniatury 784 px, makieta nie podaje ich wprost. Fix: `SlidingSidePanel` i `TvConfirmDialog` spłaszczone (jeden progress/alpha w lambdach graphicsLayer/drawBehind zamiast AnimatedVisibility), odstępy dialogu wg makiety (12/26/10 dp).

- [x] **1.8 Slider i multi-select**
  Slider: ←/→ zmieniają wartość. Multi-select jak w makiecie.
  Notatki: pominięte: lokalnie przy ekranie (1 użycie w Ustawieniach, zgodnie z inwentarzem; multi-select zależy od #2, realizacja w 2.14).

- [x] **1.9 Komponenty stanów**
  Ładowanie, pusto, błąd, wspólne dla ekranów.
  Notatki: `common/TvStates.kt`: `tvSkeleton`/`TvSkeletonBlock`/`TvPosterSkeletonRow` (statyczny placeholder #1C2028, bez shimmeru), `TvEmptyState` (ikona, tytuł, opis, akcja), `TvErrorState` (komunikat + „Try again”/akcja dodatkowa na `TvButton`). `TvErrorScreen` składa się z `TvErrorState`; `Loading` w stylu tokenów. Szkielety konkretnych ekranów (Home hero, Library) powstają przy ekranach w etapie 2.

- [x] **1.10 Infrastruktura nawigacji pilotem**
  Początkowy focus na ekranie, zapamiętywanie focusu przy powrocie, spójna obsługa Back.
  Notatki: infrastruktura w `focus/FocusNavigation.kt`: `TvInitialFocusEffect`, `TvFocusMemory`/`rememberTvFocusMemory` (zapamiętany klucz, przeżywa recreate przez rememberSaveable), `Modifier.tvFocusTrap()` (focusGroup + exit=Cancel), `TvBackHandler`. Istniejące `FocusHelpers` i store'y Home/Library zostają, migracja ekranów w etapie 2. Zachowanie na pilocie do sprawdzenia w galerii (1.11).

- [x] **1.11 Galeria komponentów (ekran deweloperski)**
  Ekran deweloperski (tylko w buildzie debug, w katalogu Material), który pokazuje wszystkie bazowe komponenty i ich stany do porównania z makietą i testów pilotem.
  Gotowe gdy: wszystkie bazowe komponenty z rzędu „Komponenty” da się obejrzeć i obsłużyć D-padem.
  Notatki: `screens/gallery/ComponentGalleryScreen.kt` pokazuje kolory, typografię, przyciski, chipy, wiersze (wszystkie warianty trailing), karty + Show more, pasek postępu, mini-grid 2x6, stany (szkielet/pusto/błąd), dialog i panel boczny (z powrotem focusu przez `TvFocusMemory`). Tylko `BuildConfig.DEBUG`. Wejście: Ustawienia > Aktualizacje > „Component gallery (debug)” albo `adb shell am start -n com.lagradost.cloudstream3.prerelease.debug/com.lagradost.cloudstream3.tv.TvMainActivity --ez tv_component_gallery true`; trasa `Screens.ComponentGallery` w NavHoście `TvApp`. Zweryfikowane na emulatorze (render, D-pad, Back, dialog, panel). Wszystkie zmiany leżą w katalogu `tv/`, zero zmian poza katalogiem Material. Napis wpisu w Ustawieniach jest na sztywno (tylko debug). Fix: ↑ z pierwszego przycisku przewija galerię na górę (sekcje Colors/Type).

---

## Etap 2: Ekrany

Przy każdym zadaniu: odpowiedni rząd makiet + jego plansza „…States” + notatka z obsługą pilotem.

### 2A. Wspólna siatka

- [x] **2.1 Wspólna siatka z paginacją**
  Używana przez Home („Show more”), Bibliotekę i Search. Sortowanie tylko metodami wspieranymi przez źródło (z makiety).
  DoD ekranu.
  Notatki: kod gotowy i buduje się (`compilePrereleaseDebugKotlin`, `assemblePrereleaseDebug`); Sprawdzone na emulatorze (zrzuty `fix2a_*.png`): Home i Biblioteka → Show more, pełny ekran (6 kolumn jak mini-grid, slot tytułu sfocusowanego elementu w nagłówku, top bar ukryty przez `onScroll(false)`, wraca po wyjściu), nagłówek y=22 dp / siatka y=62 dp (jak feed), ↑ z 1. rzędu → Sort (dalej ↑ zostaje), panel Sort pełnej wysokości, OK wybiera i focus na 1. plakacie, dociąganie stron (Home), Back → focus na kaflu „Show more” (Home i Biblioteka). **Niesprawdzone na emulatorze:** stany ładowanie/pusto/błąd/kolejna strona oraz Search. Jeden ekran `screens/grid/TvShowMoreGridScreen` + `TvMediaGrid` + `TvGridFocusStore`; `HomeFeedGridScreen`, `LibraryFeedGridScreen`, `SearchFeedGridScreen` to cienkie adaptery (trasy i store’y bez zmian). Dane zawsze jako `LazyPagingItems`: Home = Pager (`HomeGridPagingSource`), Biblioteka/Search = `PagingData.from(lista)` (bez dociągania stron, bo lista jest już w pamięci). Usunięto `MediaGrid.kt` (w tym `MediaGridStatic`) i `HaloHost` z siatki (halo to poświata, wbrew zasadzie „bez blurów”). Sort: przycisk i panel tylko gdy lista ma wspierane metody (Biblioteka); wiersz „domyślny” (Query) zostaje. Początkowy focus na 1. plakacie (wg notatki makiety; wcześniej Biblioteka celowała w przycisk Sort). Search zyskał powrót focusu na otwarty plakat po Back (dotąd brak). Podtytuł: Home = nazwa źródła, Biblioteka = nazwa konta, Search = zapytanie. Zero zmian poza katalogiem `tv`.

### 2B. Home

- [x] **2.2 Home: struktura i kolejność rzędów**
  Kolejność: Continue watching → rząd Source → Featured → feedy jako mini-grid 2×6.
  Notatki: sprawdzone na emulatorze (`home2b_*.png`): start na „Resume”, hero zmienia tło/tekst po focusie karty z debounce 120 ms (bez animacji, bez blura), ↓ z przycisków → karta (zapamiętany indeks), ↑ z kart → Resume; „Remove” przenosi focus na sąsiednią kartę (nieprzetestowane na emulatorze, żeby nie kasować danych). Rząd Source na `TvChip` (zaznaczony akcent F2B880, „More…” jako tekst), OK przełącza źródło (sprawdzone). Featured: karta 864×340 dp r12, ring wg reguły kart (2 dp + offset 2,5 dp) **bez skali 1,05** (karta zajmuje całą szerokość, skala wypychałaby ring poza ekran), crossfade 150 ms, bez cieni tekstu, przycisk Details tylko przy focusie, ←/→ zmienia slajd (auto-rotacja biblioteki Carousel wstrzymuje się przy focusie; po utracie focusa rotuje nadal). Feed = jeden `Column` (nagłówek 28 dp + `FeedMiniGrid` 2×6), plakat 130,5×196 dp, odstęp 16 dp w obu osiach, placeholder tej samej wysokości (2 rzędy; realna siatka ma 1 rząd gdy ≤6 pozycji), tytuł + „rok • typ” sfocusowanego plakatu po prawej w stałym slocie (czyta tylko mały podkomponent, zero skoków). Aktywny feed przypięty na y=22 dp (przewijanie 150 ms przez `animateScrollBy`; własny `BringIntoViewSpec` zeruje domyślne przewijanie Compose na TV, bo walczyło z przypięciem); ↑/↓ między feedami przez przewinięcie + przywrócenie focusa w tej samej kolumnie (sąsiedni feed bywa niezałożony w LazyColumn); top bar chowa się przy focusie w feedzie, wraca po focusie na hero/Source/Featured albo Back (po powrocie z „Show more” chowany ponownie po 600 ms, bo siatka pokazuje bar w `onDispose`). Usunięto `HaloHost` z Home (poświata) i blur hero. Wspólne tokeny przeniesione do `TvTokens` (`TvDimens.Feed*/Featured*/Hero*`, `TvTextStyles.Hero*/Featured*/Feed*`, `TvColors.Accent`), lokalne `*Tokens.kt` zredukowane. `TvButton` dostał parametr `textStyle`. Niesprawdzone: ↓ z top baru do treści (nie wraca do feedu po Back; przy hero trafia w Featured zamiast „Resume” – zachowanie Dashboard, nie ruszałem), wydajność na Chromecaście.
- [x] **2.3 Home: „Show more” i stany**
  „Show more” prowadzi do siatki z 2.1. Stany z planszy Home States.
  DoD ekranu.
  Notatki: kafel „Show more” (podtytuł „All of <feed>”, nowy string `tv_home_show_more_all_of`) pojawia się przy >12 pozycjach (11 plakatów + kafel); OK otwiera siatkę z 2.1, Back wraca na kafel (sprawdzone na emulatorze). Stany: ładowanie (szkielet hero + szkielety chipów Source do końca startu źródeł `HomeSourcesUiState.isBootstrapped`, placeholdery feedów), brak feedów dla źródła (`TvEmptyState`, nowe stringi `tv_home_no_feeds_message`), brak Continue watching (sekcja znika, ekran zaczyna się od Source; sprawdzone jako układ kodu, na emulatorze mam zasiane Continue watching), brak źródeł (`TvEmptyState` „No sources found” + `tv_home_no_sources_message`). **Niesprawdzone na emulatorze:** stany „brak feedów”, „brak źródeł”, „brak Continue watching” i błąd feedu (nie umiałem ich wywołać bez zmiany danych); sprawdzone tylko kompilacją i przeglądem kodu.

  **Weryfikacja 2B (poprawki wykonane; zrzuty `fix2b_*.png` w scratchpadzie sesji):**
  - F1 naprawione i sprawdzone na emulatorze: top bar ↓ woła `LocalDashboardContentEntry.handler` z Home (jawny handler zamiast `FocusRequester.Default`): Resume gdy hero na ekranie, inaczej ostatnio sfocusowany element (feed/Featured), inaczej pierwsza widoczna sekcja. Działa od 1. naciśnięcia, także po Back z feedu (potwierdzone).
  - F2 naprawione i sprawdzone: → z ostatniego chipa = Cancel, ↑ z 1. wiersza 1. feedu (każda kolumna) → Featured → ↑ → Source.
  - W1 naprawione i sprawdzone: tytuł hero 1 linia + stały slot 48 dp; meta y=425 px, przyciski y≈517 px (makieta 425/515).
  - W2 naprawione: `Hero`/`HeroFeatured` lineHeight = fontSize, letterSpacing −0,01 em, bez font padding.
  - W3 naprawione i sprawdzone: przy focusie na Featured top bar się chowa (makieta HomeFeatured go nie ma), Source w całości widoczny; wraca po ↑ do Source/hero lub Back.
  - W4 naprawione i sprawdzone: tekst Featured od góry (padding 48 dp), tytuł 1 linia, meta „rok • typ • ★ ocena”.
  - W5/W6 częściowo: rok jest pokazywany w meta hero i w slocie feedu, gdy dane go mają; dane Continue watching (`ResumeWatchingResult`) nie zawierają roku, więc w hero zwykle go nie widać (wymagałoby dociągania `load()`). Nie naprawione.
  - W8 naprawione i sprawdzone: etykieta „Search”, aktywna zakładka jako pill rgba(255,255,255,.10), logo 26 dp + 14 dp odstępu, „Home” tekst na x=202 px.
  - W9 naprawione w kodzie, NIE sprawdzone na żywo (przy 2 źródłach chip „More…” się nie pojawia, bo jest tylko przy przepełnieniu rzędu): tytuł „Sources” + „Home source” po prawej, kafel inicjałów 44 px (#262B36, na focusie #D9DCE3), check po prawej dla zaznaczonego. Usunięto z UI licznik użyć i pinezkę; przypinanie (`onTogglePin`) zostaje pod klawiszem Menu pilota, licznik użyć dalej steruje kolejnością chipów.
  - Animacje: `DashboardScaffold` (offset/padding top bara) i wskaźnik zakładki → 150 ms, `animateScrollBy` ma jawny `tween(TvDimens.MaxAnimationMs)`, `ShimmerCard` statyczny (`tvSkeleton`).
  - Magiczne liczby przeniesione do `TvDimens`/`TvColors`; `ContinueWatchingHeroTokens.kt` to aliasy `TvDimens`.
  - Niesprawdzone: stany z planszy Home States, Remove, OK=Details na Featured, panel More…, wydajność. Znane: tuż po starcie aplikacji pierwszy ↓ bywa przed ustawieniem focusa na Resume.

### 2C. Szczegóły

- [x] **2.4 Szczegóły filmu: układ**
  Opis max 3 linie, taby (Cast & crew / More like this / Details), akcje.
  Notatki: ekran szczegółów (film i serial) to jedna kolumna: hero (`DetailsHero`, 385 dp) → taby (`DetailsTabsRow`, lokalnie z tokenów, biały pill na focusie, bez skali) → treść taba. Wejście w treść (↓ z taba, a w tabie Details sam ↓) przesuwa całą kolumnę raz o 321 dp w 150 ms (`graphicsLayer`, bez scrolla strony), hero znika, pojawia się jednoliniowy nagłówek (`DetailsCompactHeader`) i dim tła; focus na akcjach wraca do stanu górnego. Opis max 3 linie (2 przy komunikacie), nie jest focusowalny, pełny opis + metadane w tabie Details (Story, Genres, Director/Screenplay/Music/Release date/Runtime/Age rating/Original language/Status/Budget/Revenue/Score/Source – tylko pola z danymi). Cast: karty 92x120 dp z inicjałami bez zdjęcia; More like this: nagłówek „Similar to …” z nazwą sfocusowanego tytułu + siatka 6 kolumn (wszystkie rekomendacje, przewijana w obszarze treści). Tło: `DetailsBackdrop` (obraz po prawej 610x540 dp film / 680x410 dp serial, gradient z koloru tła, dim 0,88 po zwinięciu). Kolory dynamiczne z backdropu przywrócone (`rememberDetailsVisualsState` używa `DetailsDynamicColor*`, do czasu wyliczenia stały ciepły/chłodny odcień; kolor focusu = jasny `onSurface` ze schematu). Usunięte: `MovieDetails` (stary hero z blurem/glow/rozwijanym opisem), `ActionIconsPill`, `DownloadActionButton`, `CastAndCrewList`, `PosterMoviesRow`, `TvSeriesEpisodeSection`. Sprawdzone na emulatorze (zrzuty `c2c_m1`, `c2c_m3`, `c2c_m4`, `c2c_i2`): górny stan, zwinięty stan, Cast (ostatnia karta widoczna z ringiem), More like this (przewijanie siatki), Details, ↑/↓/←/→ między akcjami, tabami i treścią.
  Notatki (fix): ↓ z przycisków hero od razu przesuwa kolumnę (fokus na tabie, 150 ms), bez drugiego ↓; po przejściu na tab widać pełną treść (Details, Cast, More like this). Sprawdzone na emulatorze: Play ↓ → Cast & crew w górze, → → Details z pełną stroną.
- [x] **2.5 Szczegóły filmu: Watchlist i Download**
  Watchlist otwiera panel statusu (WatchType). Download: stany Idle / Downloading % / Downloaded / Failed. Panel mirrorów z „Skip Loading” (zależnie od decyzji z 0.4).
  Notatki: przyciski `TvButton` o stałej szerokości (116 dp Favorite/Search/More, 156 dp Play/Watchlist/Download), stan zmienia ikonę i etykietę, nie szerokość (`TvButton` dostał `tint` i `iconContent` na pierścień postępu). Watchlist → panel „Set watch status” (Watching, Completed, On-Hold, Dropped, Plan to Watch, None; zakładka-ikona pełna dla wybranego + check w kolorze akcentu, tytuł filmu pod nagłówkiem), wybór zamyka panel, przycisk pokazuje status (`bookmarkLabelRes`). Download: Idle → panel mirrorów (nagłówek + „N links” po załadowaniu, grupy jakości, ikona pobierania po prawej, ładowanie: „Loading…” + „Sources found: N” + wiersz „Skip Loading”), Downloading → „Downloading 42%” z pierścieniem, Downloaded (zielony) → OK odtwarza offline, Failed (czerwony) → OK otwiera panel ponownie (logika `handleDetailsDownloadQuickAction` bez zmian). Panele boczne: `MenuListSidePanel` dostał `eyebrow`/`subtitle`, `TvListRow` nadaje kolor ikonom (`LocalContentColor`). Sprawdzone: panel statusu, panel mirrorów (ładowanie + lista, bez uruchamiania pobierania), stany przycisku Download przez tymczasowy podgląd (zrzut `c2c_x1`). Niesprawdzone na prawdziwych pobraniach (nie startowałem pobierania). Odchylenia: „Skip Loading” jest wierszem listy (jak było), a nie osobnym przyciskiem pod licznikiem; „W toku → OK otwiera Pobrane” z planszy stanów nie ma w aplikacji (OK w trakcie pobierania nic nie robi).
  Notatki (fix): szerokość przycisku Play/Watchlist/Download liczona z etykiety aktualnego stanu (nie z najdłuższej ze wszystkich), Download w stanie Downloading zawsze wg „100%”, więc zmiana procentu nie rusza szerokości; zmiana stanu (np. dodanie do zakładek) może ją zmienić. Sprawdzone na emulatorze: Play 138 px bez zmian; stany Download/Watchlist tylko w kodzie.
  Notatki (fix 4.1): panel mirrorów 92 px (`PanelMirrorRowHeight`, stała wysokość), po Skip Loading nagłówek pokazuje „N links” zamiast „Loading… / Sources found”; panel statusu: ikony 30 px (`PanelStatusIconSize`), tekst 56 dp od krawędzi panelu jak w makiecie. Sprawdzone na emulatorze (`fix4r_mirror_skip.png`, `fix4r_watch.png`). Wiersze statusu ~2 dp wyżej niż w makiecie (nie poprawiane, bo nagłówek panelu jest w `MenuListSidePanel`).
- [x] **2.6 Szczegóły filmu: stany**
  DoD ekranu.
  Notatki: stany z `DetailsStates`: ładowanie (`DetailsLoadingPlaceholder`: prawdziwy tytuł i backdrop, gdy lista je przekazała, reszta szkielety w pozycjach hero), komunikaty providera (VPN, brak metadanych) i „Coming soon…” jako stały pasek nad przyciskami (max 2, nie focusowalne; „Coming soon” bez Play i Download, start focusu na Watchlist), „Original title: …” jako pierwszy element linii meta, ekran niedostępny (`UnavailableDetailsScreen`: kafel plakatu, „This title is currently unavailable”, Search manually + Remove from library, teksty zmienione z polskich na angielskie z makiety), tab More like this. Sprawdzone tymczasowym podglądem (zrzuty `c2c_u1` Coming soon, `c2c_u2` ładowanie, `c2c_u3` niedostępny, `c2c_t3`/`c2c_t4` komunikaty). Niesprawdzone: „Original title” na prawdziwych danych, przycisk Remove na niedostępnym (nie ma elementu w bibliotece), stan błędu sieci. Powrót focusu po Back: przy powrocie ze strony zwiniętej focus wraca do treści (ostatni odcinek / pierwszy element listy / tab), inaczej na Play (sprawdzone dla More like this → Back; powrót z playera niesprawdzony, bo nie uruchamiam playera w emulatorze).
- [x] **2.7 Szczegóły serialu: kolumna sezonów i lista odcinków**
  Taby jak w filmie, ale Episodes pierwszy. Kolumna sezonów (Extras na końcu), lista odcinków z akcjami w wierszu.
  Notatki: `DetailsEpisodesTab`: kolumna sezonów 150 dp na `TvListRow` (akcent = primary, „N episodes”), sezon 0 jako „Extras” na końcu (`withExtrasLast`), separator, lista odcinków (wiersz 100 dp, still 150x84,5 dp, badge „obejrzany” + pełny pasek, tytuł „N. …”, meta „data • czas • ocena • Watched”, opis 2 linie). ↓ z taba Episodes → odcinek do obejrzenia (ostatnio sfocusowany w sezonie → odcinek z `currentEpisode` → pierwszy nieobejrzany → pierwszy), ← z odcinka → aktualny sezon, ↑/↓ po sezonach przełącza listę po 150 ms (focus zostaje na sezonie), → z sezonu → odcinek. OK na odcinku = Play, akcje w wierszu (widoczne na focusie): Obejrzany (toggle), Download (ikona wg stanu, w trakcie z %), More; długie przytrzymanie usunięte. More = istniejący panel akcji podzielony na grupy Play / Progress / Download / Other, nagłówek „Serial • S3:E1” + tytuł odcinka. Naprawiony błąd: po akcji (np. Mark as watched) stany odcinków były czyszczone bez ponownego pobrania, więc znacznik obejrzenia nie pojawiał się (`DetailsEpisodesStateHolder.statesRevision` wymusza ponowną hydrację widocznego okna). Sprawdzone na emulatorze (`c2c_s3`..`c2c_s6`, `c2c_e4`, `c2c_x2`): ↓ do odcinka, ←, ↓/↑ po sezonach (debounce), →, menu More, toggle Obejrzany (cofnięty), przewijanie listy do ostatniego odcinka, stany pobierania w wierszu (podgląd). Odchylenia/brak w aplikacji: licznik „obejrzano X z Y” w opisie sezonu i „34 min left” z paskiem postępu odcinka (aplikacja nie liczy postępu per sezon/odcinek w szczegółach – pokazuję tylko Watched), kropka „trwa” przy sezonie z `nextAiring` jest po prawej, nie obok tytułu, „Resume S3:E2” (etykieta to „Play S3:E2”, bo `currentEpisode` to resume albo następny nieobejrzany i nie da się ich odróżnić), brak stanu szkieletu przy ładowaniu odcinków sezonu (odcinki przychodzą razem ze szczegółami), „Play in m3u8 player”/„Copy link” zależą od zainstalowanych akcji.
  Notatki (fix 4.1): panel More filmu bez „• E0” (nadtytuł tylko dla serialu, np. „Reacher • S3:E2” wg makiety; film ma sam tytuł, bez powtórzenia w podtytule). Ikony pozycji wg makiety: Play in Browser i Play in m3u8 mają ikonę external link (tak jest w makiecie), Copy to clipboard ikonę kopiowania, Play mirror ikonę serwera. Sprawdzone na emulatorze (`fix4r_more.png`).
- [x] **2.8 Szczegóły serialu: zapowiedź `nextAiring` i stany**
  (zależnie od decyzji z 0.4)
  DoD ekranu.
  Notatki: pasek „Season 2 Episode 3 will be released in 3d 4h 12m” nad listą odcinków w tabie Episodes, gdy wybrany sezon to sezon z `nextAiring` (numer sezonu albo ostatni zwykły, gdy `nextAiring.season == null`); czas liczony jednorazowo przy kompozycji (nie tyka). Status „Ongoing/Completed” w meta hero i w zwiniętym nagłówku. Stany: Extras na końcu, brak odcinków (`TvEmptyState` + „Try another source with Search.”), sezon w trakcie, wiersz z progresem pobierania/Downloaded/Failed. Sprawdzone tymczasowym podglądem z wymuszonym `nextAiring` (`c2c_t5`); na prawdziwych danych `nextAiring` nie było w żadnym dostępnym serialu. Format odliczania: istniejące stringi `next_*_format`.

  **Weryfikacja 2C (do poprawy przed odhaczeniem 2.4–2.8; zrzuty `verify2c_*.png` w scratchpadzie sesji):**
  - BLOKER: [NAPRAWIONE, potwierdzone na emulatorze: film Bad Apples i serial Cyberpunk otwierają się bez ANR; `clearEpisodeStates()` nic nie robi, gdy stan już pusty, a „obejrzany” po akcji nadal się odświeża] każdy FILM wiesza aplikację (ANR, pętla na głównym wątku). `DetailsScreenContentEffects.kt` ~67–71 zbiera `snapshotFlow { visibleKeys to episodesStateHolder.statesRevision }` → `hydrateVisibleWindow`; dla pustej listy odcinków `DetailsEpisodesStateHolder.kt` ~68–70 woła `clearEpisodeStates()`, który zwiększa `generation` (~179) → `statesRevision` się zmienia → pętla. Wprowadzone w 5ed864be; dotyczy też serialu z pustym sezonem. Poprawka: nie czyścić, gdy stan już pusty, albo hydracja zdarzeniem zamiast czytania `statesRevision` w `snapshotFlow`.
  - F-1 [NAPRAWIONE, sprawdzone: Back z panelu More odcinka i z mirrorów odcinka wraca na ten sam przycisk w wierszu; `TvFocusMemory` w `DetailsScreenBody`] (wysoka): Back z panelu More odcinka → focus na tabie Episodes zamiast na przycisku More tego odcinka (`returnEpisodeId`/`episodesFocus.requestEpisode` nie wywoływane po zamknięciu panelu).
  - F-2 [NAPRAWIONE, sprawdzone: Watchlist (wybór i Back), mirrory (Back); po wyborze statusu focus jest przywracany ponownie, gdy `details` się zmieni] (wysoka): po zamknięciu panelu Watchlist (wybór) i panelu mirrorów (Back) focus wraca na Play zamiast na przycisk, który otworzył panel.
  - F-3 [NAPRAWIONE, sprawdzone: po Back z tytułu z More like this focus wraca na otwarty plakat; klucz `similar:<id>` + przewinięcie siatki] (średnia): po Back z innego tytułu otwartego z More like this focus wraca na pierwszy plakat wiersza, a nie na otwarty (`focusRestorer()` w `DetailsSimilarTab`; trzymać indeks).
  - W1/W2 [NAPRAWIONE: lista odcinków bez górnego paddingu (y=240 px jak w makiecie), treść tabów w stanie górnym o 20 dp wyżej (karty obsady y=858 px), tytuł hero w stałym slocie linii (meta y=317 px jak w makiecie); sprawdzone na emulatorze] (niska): treść tabów w stanie górnym ~34–40 px niżej niż w makiecie, w stanie zwiniętym +12 px (rezerwa na divider, padding listy 6 dp). Meta hero ~22 px za nisko.
  - W3 [NAPRAWIONE częściowo: wiersze 84/70 px, nagłówki grup uppercase 19 px +.12em z odstępami 22/6 px; ikony pozycji panelu More nadal z zasobów aplikacji, nie z makiety, górny padding panelu statusu 28 dp zamiast 32 dp] (niska): wiersz panelu Watchlist 76 px (makieta 84), panel More odcinka 76 px (makieta 70), nagłówki grup nie uppercase 19 px/+.12em, ikony pozycji inne niż w makiecie.
  - W4 [NAPRAWIONE w kodzie: `MovieRepositoryImpl` i `createDetailsError` nie dopisują pustego dopiska po dwukropku; pusty dopisek nie wystąpił na emulatorze, sprawdzony stan z treścią błędu] (średnia): ekran błędu ładowania pokazuje „Failed to load movie details:” z pustym dopiskiem – uporządkować tekst (stan do decyzji, brak w `DetailsStates`).
  - W5 [NAPRAWIONE, sprawdzone: ring ostatniego rzędu ma ~30 px od dolnej krawędzi, `SimilarGridBottomInset`] (niska): More like this – ring ostatniego rzędu dotyka dolnej krawędzi ekranu; twarde przycięcie siatki pod nagłówkiem.
  - W6 [NAPRAWIONE: stałe loading placeholdera, hexy awaryjne i martwe `48.dp` przeniesione/usunięte, `TvTokens`] (niska): magiczne liczby w `DetailsLoadingPlaceholder.kt` ~27–37, nieużywane `48.dp` w `DetailsScreenSupport.kt` ~19, awaryjne hexy w `DetailsVisualsStateHolder.kt` ~47–65 → `TvTokens`.
  - Poprawki po ponownej weryfikacji 2C (zrzuty `fix2c2_*.png`): (1) szerokość przycisków hero z treści (najdłuższa etykieta wszystkich stanów, `rememberTextMeasurer`; Play 138, Watchlist 245, Favorite 182, Download 303, Search 169, More 148 px; zmiana statusu Watchlist nie zmienia szerokości, sprawdzone) - naprawione, tokeny `DetailsActionWidth/WideWidth` usunięte. (2) ekran błędu: tytuł „Failed to load”, treść błędu mniejsza, max 2 linie, bez prefiksu - naprawione w kodzie, ekranu po zmianie nie oglądałem. (3) lista sezonów y=240, tytuł panelu statusu +8 px - naprawione, sprawdzone. (4) More like this: fade pod nagłówkiem, nagłówek bez tytułu z pamięci po utracie focusa - naprawione, fade sprawdzony. (5) inicjały obsady z palety dynamicznej - naprawione, niesprawdzone na żywo (brak obsady bez zdjęcia). (6) dolny padding listy paneli 32 dp - naprawione, sprawdzone. (7) tytuł hero 1 linia z ellipsis - naprawione, sprawdzone. (8) Home po Back: top bar chowany bezwarunkowo, 2 razy po 600 ms - naprawione, sprawdzone (nie powtórzyłem zgłoszonego nachodzenia, tylko stan po Back).
  - OK (serial): układ, taby, zwijanie nagłówka, sezony/odcinki/Extras, przewijanie do ostatniego odcinka/obsady/rekomendacji, panel More, panel Watchlist ze stałą szerokością przycisku, panel mirrorów, Back ze szczegółów → Home na ten sam plakat.
  - Niesprawdzone: wszystkie ekrany filmu (bloker), stany z `DetailsStates`, `nextAiring` na realnych danych, stany pobierania, Mark/Remove watched up to…, powrót z playera.

### 2D. Wyszukiwanie

- [x] **2.9 Wyszukiwanie**
  Pole/klawiatura, wyniki w siatce (z 2.1), stany (puste wyniki, błąd, ładowanie).
  DoD ekranu.
  Notatki: `SearchScreen` przebudowany wg makiet Search/SearchStates i notatki pilota: pole `SearchField` (pigułka 550x38 dp, biały fill przy focusie, OK otwiera systemową klawiaturę z sugestiami; po prawej „Searching N sources…” albo „S sources • R results”), jedna sekcja na źródło (`SearchSection`: nagłówek nazwa + „N results”, tytuł sfocusowanego plakatu w `TvFocusedItemTitle`, jeden `LazyRow` plakatów 130.5x196 dp jak w Home, po 11 plakatach kafel „Show more” przy >12 wynikach, nawigacja do `SearchFeedGridScreen`). Źródło z błędem: wiersz z komunikatem zamiast plakatów (reszta działa); źródła bez wyników są pomijane (decyzja: makieta nie pokazuje pustych wierszy), a gdy żadne nic nie zwróci: ekran „No items in this list” + „None of the N sources returned results for “q””. Start: „Search” + „Press OK to type…”. Stan w toku: szkielety plakatów, N = liczba źródeł wciąż ładujących (z danych sekcji, bez nowej logiki). Focus: start na polu, ↑ z pierwszego rzędu → pole, Back z wyników → pole (kolejny Back obsługuje Dashboard: top bar, potem wyjście), powrót z detali i z „Show more” na ten sam plakat/kafel (`SearchViewModel.pendingRestore`). Back handler komponowany tylko, gdy aktywny, bo handler Dashboardu (rejestrowany po treści) inaczej miał pierwszeństwo po powrocie z nawigacji. Usunięty `HaloHost` (Home go nie używa). Sprawdzone na emulatorze (`c2d_*.png`): start, w toku, wyniki z 2 źródeł, przewijanie rzędu do ostatniego plakatu (ring nieprzycięty), ↑/Back, „Show more” (zapytanie „the”) i powrót na kafel, powrót z detali na plakat, brak wyników, wiersz błędu (przy wolnej sieci emulatora). Odchylenie: pole jest ~20 px niżej niż w makiecie (y=144 px zamiast 124 px), bo top bar Dashboardu ma wysokość 72 dp, a nie 56 dp z makiety; nagłówki sekcji dziedziczą to przesunięcie. Niesprawdzone: jeden zapytanie z mieszanką błąd + wyniki w tym samym zapytaniu (makieta Search), duża liczba źródeł (>5), długie nazwy źródeł, wydajność na Chromecaście, wpisywanie przez pilota (tylko `adb input text`). „Show more” nie ma podpisu „All of X” (makieta Search go nie pokazuje). Poprawki po weryfikacji (`fix2d_*.png`): tekst stanów startowego/brak wyników na y≈441 px (`SearchStateTop` 148 dp); Enter bez celu w dół zostawia focus na polu (bez `moveFocus`); tytuł w nagłówku zerowany przy Loading; sekcja Error ma własną wysokość (`SearchErrorSectionHeight`), a wysokość sekcji wynika z faktycznego układu (szkielet = plakat, bez 4 px przesunięcia); wiersz błędu pozostaje focusowalny (decyzja użytkownika: biały fill, bez skali, OK nic nie robi, ↓ z pola trafia na pierwszą sekcję w kolejności, także błąd); `ShowMoreCard` ma proporcje i ring plakatu (dotyczy też Home, nie oglądane na Home); `SearchListBottomPadding` w tokenach; `PosterCard` pokazuje tytuł (max 3 linie) na placeholderze, gdy obraz się nie załaduje (wszystkie ekrany; sprawdzone w Search). Sprawdzone na emulatorze: 1, 2, 3 (kod, nie zrzut), 4, 5, 7, 9.

### 2E. Biblioteka i Pobrane

- [x] **2.10 Biblioteka**
  Przełącznik źródła list (Local/TMDB/AniList/…), każda lista jako mini-grid 2×6, siatka z sortowaniem tylko wspieranymi metodami, stany.
  DoD ekranu.
  Notatki: `LibrarySourceSelectorRow` to teraz zwykły `Row` z etykietą „LISTS FROM” (nowy string `tv_library_lists_from`) i `TvChip` na każde zalogowane konto (akcent `TvColors.Selected` jak w makiecie, nie pomarańczowy z Home); wejście z góry ląduje na wybranym koncie (`focusRestorer`), zmiana źródła od razu przeładowuje listy (stan ładowania = szkielety). Pokazywany także przy jednym koncie (informuje o źródle). Lista = `FeedSection` (nagłówek z liczbą „N titles” w slocie źródła, tytuł sfocusowanego plakatu po prawej, 11 plakatów + „Show more” z podpisem „N more” przy >12 pozycjach; nowy parametr `showMoreSubtitle` w `FeedSection`/`FeedMiniGrid`). Przewijanie jak na Home: sfocusowana lista jest przypinana do góry w 150 ms (własny `LibraryBringIntoViewSpec` = 0, pin przez `animateScrollBy`), pasek górny chowa się, gdy lista jest przewinięta; wyjście ↑/↓ z mini-gridu przechodzi do sąsiedniej listy w tej samej kolumnie (`onVerticalExit`), ↑ z pierwszego rzędu pierwszej listy wraca do chipów źródeł; powrót z detali/„Show more” przewija do listy i przywraca focus na ostatni plakat/kafel (z ponownym schowaniem paska po 600 ms jak na Home). Stany: ładowanie (`LibraryLoadingSection`: pasek-szkielet nagłówka + 2x6 szkieletów), pusta lista (też gdy wszystkie listy źródła są puste: komunikat zamiast pustych siatek), brak kont (przycisk „Accounts and Security” otwiera zakładkę Ustawienia, nie od razu kategorię kont: `onOpenAccountsSettings` w `DashboardScreen`), błąd (tytuł `tv_library_load_error_title` + komunikat z API). Tytuł+opis stanów dzielone z istniejących stringów (`empty_library_*`) po nowej linii / pierwszym zdaniu. Poprawki po weryfikacji (`fix2e_*.png`): chipy 26 dp (parametr `height` w `TvChip`, Home bez zmian), odstęp chipy→nagłówek pierwszej listy 15 dp (parametr `pinnedTop` w `FeedSection`, także w szkieletach), slot tytułu „2020 • ★ 6.9” (`showRating` w `TvFocusedItemTitle`/`FeedSection`, Home i Search bez zmian), dolny odstęp pod ostatnią listą liczony z viewportu (krótka lista też się przypina), `scrollToItem(0)` zamiast springa, selektor bez opakowującego `Box`, handler „↓ z top baru” w stanie brak kont kieruje focus na przycisk „Accounts and Security” (sprawdzone tymczasowym podglądem, usuniętym). Siatka „Show more” (`LibraryFeedGridScreen`) bez zmian, otwiera się i wraca poprawnie. Sprawdzone na emulatorze (`c2e_*.png`): chipy TMDB/Local, lista z 93 pozycjami + „82 more”, focus plakatu z tytułem w nagłówku, ↓ do rzędu 2 i do kolejnej listy, ↑ z powrotem, ↑ do chipów, przełączenie na Local (listy puste → „This list is empty.”), szkielety ładowania, „Show more” → siatka → Back na kafel, powrót z detali na plakat. Odchylenia: treść zaczyna się pod paskiem 72 dp (makieta 56 dp), więc chipy i nagłówki są ok. 16 dp niżej niż w makiecie, a odstęp chipy→nagłówek listy to 22 dp zamiast 15 dp (stała `FeedPinnedTop` z Home); rząd 2 pierwszej listy wystaje pod dolną krawędź (przewija się po focusie). Niesprawdzone: stany „brak kont” i „błąd” (emulator ma konta; tylko kod), >2 kont (np. AniList/Simkl), bardzo długie nazwy kont, wydajność na Chromecaście, krótka lista ≤12 pozycji (tylko 93, 9 i 8 pozycji w TMDB: 9 i 8 sprawdzone, bez „Show more”).
- [x] **2.11 Pobrane: sekcje i akcje**
  Sekcje Downloading / Downloaded, akcje Play / Delete.
  Notatki: `DownloadItemCard` zastąpiony `DownloadItemRow` (plik przemianowany): stały wiersz 75 dp (miniatura 104x58,5 dp z backdropu lub plakatu, tytuł, meta, pasek 260x4 dp ze statusem po prawej, opis jednolinijkowy dla pobranych); jedna pozycja = jeden film/odcinek, OK otwiera szczegóły tytułu. Akcje w zarezerwowanej kolumnie 180 dp po prawej (widoczne tylko przy focusie wiersza lub akcji, szerokość stała): → z wiersza na Play (tylko pobrane) albo Delete, ← wraca; pozycje w toku, wstrzymane i błędne mają tylko Delete. Focus wiersza: tło `TvColors.FillRow` (rgba 255,255,255,.07) + ring 1,5 dp na zewnątrz (`drawBehind`, bez skali), przyciski to `TvButton` z białym fillem bez skali. Dwie sekcje z nagłówkami „DOWNLOADING • N” / „DOWNLOADED • N” (istniejące stringi, `TvTextStyles.Eyebrow`, pusta sekcja pomijana). Prędkość i ETA (dane były w modelu, nie pokazywane) dopisane do linii meta pozycji pobieranej: „Movie • Źródło • 3.4 MB/s • 8m 12s left” (nowe stringi `tv_downloads_speed_format`, `tv_downloads_time_left_format`, ETA z istniejących `download_time_left_*`); pasek: Downloading #9CC1FF, Paused #8790A0; pozycja z błędem: „Download Failed” w `ErrorText`. Tło ekranu: backdrop sfocusowanej pozycji w prawym górnym rogu 580x326 dp, alfa 0,35, gradienty do tła (dodany krótki fade od góry, bo obraz zaczyna się pod paskiem), bez animacji i blura (usunięty `Crossfade` i `blur`). Pasek górny chowa się po przewinięciu (jak w Search). Usunięte: skala 1.04/1.08 kart i przycisków, cienie, `OutlinedButton`. Poprawki po weryfikacji: akcje Play/Delete są zawsze komponowane (alpha 0 i `canFocus=false` bez focusu wiersza), więc → działa od razu po wejściu na wiersz; ponownie sprawdzone na danych testowych: →, Delete → dialog z Cancel → Back zamyka dialog i focus wraca na Delete, backdrop zmienia się z focusem. Sprawdzone na emulatorze z TYMCZASOWYMI danymi testowymi (podgląd usunięty przed commitem): 3 pozycje w toku (pobieranie, pauza, błąd) i 3 pobrane, focus wiersza, → na Play/Delete, ← z powrotem, wiersz z błędem tylko Delete, przewijanie listy z ukryciem paska. Odchylenia: tytuł odcinka bez prefiksu „S3:E2” (tytuł pochodzi z `resolveDownloadItemTitle`: „Serial • Odcinek”; numer sezonu/odcinka jest w linii meta); miniatury w makiecie to kadry odcinków, tu backdrop tytułu (brak osobnych kadrów w danych); ring wiersza i treść zaczynają się 48 dp od krawędzi, ale lista startuje pod paskiem 72 dp (makieta 56 dp). Niesprawdzone: odtwarzanie offline z Play (nie startowano playera), realne pobieranie z prędkością/ETA (tylko dane testowe), długie tytuły, wiele pozycji (przewijanie >2 ekranów), wydajność na Chromecaście.
- [x] **2.12 Pobrane: dialog usuwania i pusty stan**
  DoD ekranu.
  Notatki: dialog usuwania to wspólny `TvConfirmDialog` (`focusSecondaryFirst`, czyli focus domyślnie na Cancel, Delete w kolorze błędu, tytuł „Delete File”, treść z nazwą pozycji), bez zmian względem wcześniejszego kodu; po potwierdzeniu focus przechodzi na sąsiednią pozycję (istniejąca logika), po Cancel wraca na przycisk Delete wiersza. Pusty stan „No downloads yet” + „Downloaded movies and episodes will appear here.” to wspólny `TvEmptyState` (ikona pobierania, blok przesunięty 44 dp nad środek ekranu wg makiety). Sprawdzone na emulatorze: pusty stan (prawdziwy), dialog na danych testowych (focus na Cancel, Cancel zamyka i zwraca focus na Delete). Niesprawdzone: faktyczne usunięcie pliku (celowo nie wciskano Delete w dialogu, żeby nie ruszać prawdziwych danych), (Back zamykający dialog sprawdzony po poprawce).

### 2F. Ustawienia

- [x] **2.13 Maszyna nawigacji ustawień**
  Jak Google TV: lewa szeroka karta aktywna, prawa = podgląd następnego poziomu lub pusty box; OK/→ wchodzi głębiej, ←/Back wraca.
  Notatki: `SettingsHost` to dwie karty (aktywna 520 dp #1B1F27 r14, podgląd 324 dp #13161C, odstęp 20 dp, treść karty z paddingiem 20/16 dp). Poziom 1 pod paskiem górnym (karty 8 dp pod paskiem 72 dp), poziomy ≥2 chowają pasek (`onTopBarVisibleChanged` → `updateTopBarVisibility`, karty 24 dp od góry, jak makieta). Nagłówek poziomu: okruszki „Settings › Player” (11 sp/600 #8790A0, tytuły stosu, liczone raz na instancję) + tytuł 24 sp/800 + opcjonalny opis. Podgląd = następny poziom sfocusowanego wiersza (debounce 120 ms, preview pokazuje tytuł, wiersze 42 dp, sekcje, Toggle jako On/Off, slider jako wartość; pozycja bez podpoziomu, Loading i Error następnego poziomu = pusty box). Przejście: translateX o 1/3 szerokości + alpha 150 ms tylko treści karty (karta nie jedzie), podgląd tylko alpha. OK/→ na wierszu z „›” wchodzi (→ tylko przy `nextScreenId`; akcje bez podpoziomu tylko OK), ←/Back wraca, focus wraca na wiersz wejścia (`ScreenInstance.focusKey`); ← jest Back także na Toggle i na ekranach własnych (przez `OnBackPressedDispatcher`, więc `BackHandler` ekranu konta zamyka najpierw tryb logowania), wyjątek: Slider. Wybór (radio) zapisuje i zostaje na poziomie; wiersz wyboru startuje na zaznaczonej opcji; po zmianie wartości poziomy wyżej przeładowują się po cichu (`reloadSilently`, bez szkieletu), więc podtytuły („Internal player”, „Source Name, Resolution”) są od razu aktualne po Back. Wiersze ustawień to teraz `TvListRow` (rozszerzony o `bodyContent`, `horizontalPadding`, `gap`, `verticalPadding`, `trailingIconSize`, `tint/focusedTint`, `TvRowLeading.Checkbox`, `TvCheckboxIndicator`, toggle czytelny na białym fillu); `SettingsEntryRow` ma jedną implementację zamiast własnego `Surface` ze skalą 1.03. Nowe typy `SettingsEntryType.Radio/MultiSelect/Message`, tokeny `TvDimens.Settings*`, `TvTextStyles.Settings*`, kolory `TvColors.Settings*`. Poprawki po weryfikacji (`fix2f_*.png`): Back z poziomu 1 najpierw ustawia focus na tabie Settings (`topBarFocusRequester`/`isTopBarFocused` z Dashboardu), dopiero kolejny Back wychodzi do Home; padding dolny karty przeniesiony do `contentPadding` list (clip na krawędzi karty); App Language dopasowuje tag dokładnie, potem po samym języku, zaznacza opcję, startuje na niej i pokazuje angielską nazwę po prawej; `TvConfirmDialog` wg makiety SettingsPluginActions (400 dp, padding 26/24 dp, r14, tytuł 20 sp, tekst 13 sp, przyciski 34 dp, globalnie, w tym Pobrane: tekst do 4 linii, nie oglądane na danych); `ProviderAccountViewModel` zawsze startuje w trybie Idle (Kitsu nie otwiera formularza od razu); po Back z logowania focus wraca na „Add account/Log in”; Player: sekcja PLAYBACK, bez ikon, „Source name, Resolution” (wiersza „Subtitles” brak: ustawienia napisów nie mają ekranu w TV, więc nie dodano); wiersze kont 46 dp, QR 44 dp, bez etykiety ACCOUNTS, w podglądzie najpierw konto potem „No account”; podpowiedź formularza „Enter username and password”; liczby w plikach Ustawień przeniesione do `TvDimens.Settings*` (skala `SettingsDpN`). Sprawdzone na emulatorze (`c2f_*.png`): poziom 1 z podglądem General, wejście → i OK, Back/← z focusem na pozycji wejścia (Player, Accounts, Extensions), ukrywanie paska na poziomie 2, przewijanie długiej listy z focusem, powrót na pasek (↑ z pierwszego wiersza). Odchylenia: karty zaczynają się niżej niż w makiecie na poziomie 1 (pasek 72 dp zamiast 56 dp, zaakceptowane), podgląd Language/DNS pokazuje wiersze bez zaznaczenia, podtytuły wierszy głównych („Language, startup…”) nadal zakodowane po angielsku (jak w makiecie, bez zmian). Niesprawdzone: wydajność animacji na Chromecaście, bardzo długie tytuły w okruszkach (ucięcie z wielokropkiem), zachowanie przy zmianie języka aplikacji (recreate).
- [x] **2.14 Ustawienia: typy ekranów**
  Suwak (←/→ zmieniają wartość), multi-select, logowanie QR i formularzem, akcje pluginu z potwierdzeniem.
  Notatki: Slider (lokalnie przy Ustawieniach, zgodnie z inwentarzem): wiersz 46 dp, w polu focusu strzałki ‹ ›, tor 4 dp z gałką 12 dp (#3F6FE0) i wartość 15 sp/700, bez focusu tor 3 dp #5A6170 i wartość 13 sp; ←/→ zmieniają o krok (`sliderStep`), ← nie cofa (host pomija Slider, wiersz konsumuje), Back wraca. Multi-select (`MultiSelect`, checkbox 17 dp, zaznaczony #9CC1FF z ciemnym tickiem): Player > Show player information (z opisem pod tytułem, nowy string), Providers (języki, media, dub/sub), Layout (poster UI, jakość wyszukiwania); wcześniej to były Toggle. Radio (`Radio`) dla wszystkich list wyboru (helper `itemEntry(showCheckmark=…)` ustawia typ). Konta: lista providerów z kafelkami inicjałów (MAL, KIT, AL, S, TMDB, OS, SDL), poziom konta = karta podsumowania + „Active account” (radio: No account + konta) + „Manage” (Add account / Create account / Log out), podgląd identyczny, przyciemniony; logowanie przejmuje tę samą kartę: QR (160 dp, jasne pole, tekst, URL ucięty do 3 linii, kod, „Code expires in …” w #F2B880) + Auth Locally + Cancel, formularz (wiersze pól, OK otwiera klawiaturę, hint „Required • OK opens the keyboard”, „Log in” nieaktywne z podpowiedzią do wypełnienia pól) i przeglądarka (Log in + Cancel); okruszki z nazwą providera, tytuł „Log in to X” / „Log in”; `BackHandler` najpierw zamyka tryb logowania. Pola to `SettingsFieldRow` (wiersz z fokusem + ukryte pole tekstowe, ten sam wzorzec co pole Search: OK otwiera IME, Back z IME oddaje focus wierszowi). Plugin: nagłówek (kafelek 52 dp, nazwa, status „Downloaded • Version N” na zielono), opis, autorzy, separator, wiersze akcji Update / Download / Settings (legacy, z ikoną „otwórz”) / Delete (czerwony, na focusie czerwony na białym); usuwanie otwiera wspólny `TvConfirmDialog` (destructive, start na Cancel, nowy tekst „X will be removed from this device. You can download it again from Y.”). Dodaj repozytorium: wiersze Repository name (opcjonalne) i Repository URL + „Add repository” nieaktywne do wpisania URL („Enter a URL first”). Lista pluginów: kafelek z ikoną (fallback kolorowy inicjał), „Downloaded • v692”, ikona potwierdzenia przed „›”. Test instrumentalny `ProviderAccountScreenNavigationTest` dostosowany do kolejności z makiety (Add → Create → Log out), nie uruchamiany. Sprawdzone na emulatorze: Slider (←/→ na „Parallel downloads”/„Concurrent connections”, wartości cofnięte do 3), Radio (Preferred video player: zapis, Back z aktualnym podtytułem, przywrócenie), Multi-select (Show player information, Providers > Extension languages), toggle, konta (lista, TMDB z kontem, AniList/Simkl w trybie przeglądarki, QR na prawdziwym żądaniu TMDB „Add account” z Cancel, formularz Kitsu), plugin pobrany i niepobrany, dialog usuwania (Cancel), dodaj repo z wpisaniem URL przez `adb input text` (bez zatwierdzania). Odchylenia: kafelki kont to inicjały z makiety zamiast ikon providerów; wiersz „Update” bez „692 → 693” (dane nie mają zainstalowanej wersji, tekst „Update available • Version N” w nagłówku); focus na poziomie pluginu startuje na pierwszym wierszu akcji (makieta pokazuje Delete jako przykład); statystyki pluginów („N downloaded • …”) zostały jako nieaktywny tekst pod listą repozytoriów (makieta ich nie ma). Niesprawdzone: logowanie formularzem do końca (celowo bez wpisywania danych logowania), stan „Logging in…”/błąd logowania i „Auth Locally” (tylko kod), pobranie/aktualizacja/usunięcie pluginu naprawdę (stan „Downloading…” oglądany na tymczasowym podglądzie, usuniętym), dłuższe listy kont, plugin z aktualizacją.
- [x] **2.15 Ustawienia: stany**
  DoD ekranu.
  Notatki: Loading = 4 paski szkieletu (223/309/257/291 dp, #262B36) pod nagłówkiem poziomu; Error = czerwony box (tytuł „Error loading extensions” z nowego stringa + komunikat, `SettingsLoadException`) + sfocusowany wiersz „Try Again” (istniejący string; `SettingsScreen.onRetry()` odświeża ViewModel, potem poziom ładuje się od zera); Pusto = wiersz akcji + blok „No repositories / Add a repository to browse plugins” (`SettingsEntryType.Message`), pusta lista ogólnie „No Data”; akcja w toku = wiersz ze spinnerem („Downloading…”, „Updating…”, „Removing…”, „Logging in…”, „Adding…”), pozostałe wiersze nieaktywne; ekran tekstowy (Disclaimer) = jeden focusowalny blok z pierścieniem 2 dp, ↑/↓ przewijają o 64 dp (150 ms), strzałka ▼ gdy jest co przewijać. Sprawdzone na emulatorze na TYMCZASOWYM kodzie (usuniętym przed commitem): Loading (9 s opóźnienia), Error + Try Again → Pusto, wiersz „Downloading…” z nieaktywnym stanem; Disclaimer sprawdzony na prawdziwych danych (ring, ▼, ↑/↓). Punkty DoD sprawdzone dla Ustawień: wygląd jak makieta (porównanie CSS: wymiary, kolory, teksty), stany z „SettingsStates”, obsługa samym D-padem i Back, focus startowy i powrót, brak long-pressa/cieni/blurów i animacji >150 ms (spinner jest jedyną animacją ciągłą, 900 ms/obrót, tylko w stanie „w toku”), płaskie drzewo (`TvListRow` jedna Row na wiersz), tokeny i wspólne komponenty. Niesprawdzone: porównanie pikselowe z obrazami makiet (niedostępne), zachowanie na Chromecaście, błąd logowania w formularzu (tylko kod).

---

## Etap 3: Player (najbardziej ryzykowny, robić na końcu)

Makieta: rząd **Player** + plansze stanów.

- [x] **3.1 Rząd kontrolek**
  Stała kolejność (wg makiety): Play/Pause (pierwszy z lewej, stała szerokość), Restart, Video*, Sources, Next*, Subtitles, Sync*, Audio*, Aspect. Elementy z * są ukrywane, gdy niedostępne. Focus i zachowanie auto-ukrywania zgodnie z notatką przy rzędzie.
  Notatki: kontrolki to pigułki 30 dp (ikona 12 dp + etykieta 11 sp/600 + wartość 11 sp/500 #B8BEC9), odstęp 6 dp, Play/Pause min. 75 dp + dodatkowe 6 dp; focus = biały fill bez skali (makieta ma skalę 1.05 na Play, wg decyzji #3 pominięta). Jeden komponent `PlayerOptionButton` zamiast starych okrągłych `PlayerControlButton` z tooltipami (usunięte `PlayerTooltip`, `PlayerBottomControlLayout` i cały graf `focusProperties`: kolejność wynika ze zwykłego przeszukiwania focusu, bo niedostępne przyciski nie są komponowane; ↓ z osi czasu wraca na ostatnio sfocusowaną kontrolkę). Ikony 1:1 z SVG makiet (`PlayerIcons`). Wartości: Video = wysokość aktywnej ścieżki („1080p”), Next = „S3:E3” (nowe `TvPlayerUiState.Ready.upNext`), Subtitles = język lub „Off”, Sync = „+0.3 s” / „0 s”, Audio = język ścieżki, Aspect = „Fit”/„Stretch”/„Zoom”. Oś czasu: czas 12 sp tabular + tor 4 dp (6 dp na focusie) + pozostały czas „−32:40”, bufor jako jaśniejszy pasek, akcent serial #8FD3E0 / film #F2B880, gałka 10 dp (na focusie 16 dp biała z pierścieniem akcentu), podpowiedź „41:22 +10 s” nad gałką po ←/→ (znika po 1,5 s); gradient dolny 150 dp wg makiety; metadane (tytuł 26 sp, „S3:E2 • tytuł”, „źródło • jakość”) jako 3 teksty w jednym `Column` (bez `DotSeparatedRow`). Animacja pokazania kontrolek: sam alfa 120 ms (usunięte przesunięcie 56 dp/180 ms). Logika auto-ukrywania i startowy focus bez zmian. Sprawdzone na Chromecaście: rząd bez Video/Next/Sync (film), z Audio, pauza → metadane po 5 s, ↑ na oś czasu, ← →, podpowiedź seek (`c3_pl2`..`c3_pl5`). Niesprawdzone: wariant serialowy (Next, odcinek w metadanych), Video i Sync w rzędzie, test androidTest (przepisany, nieuruchamiany; reszta androidTest w repo już nie kompiluje się z powodów niezwiązanych z Playerem).
- [x] **3.2 Panele proste: Audio, Wideo, Źródła**
  Notatki: panel Sources (680 px, lista grup 1080P/720P/360P, „N found”, biały focus, check akcentem, błąd źródła) i Audio (pigułka radio po prawej, focus biały, wiersz 84 px) sprawdzone na Chromecaście względem makiet (`c3_32_sources2.png`, `c3_32_audio.png`). Video: panel zrobiony w kodzie tym samym komponentem, ale na testowanym strumieniu nie pojawia się przycisk Video (jedna ścieżka wideo), więc niesprawdzone na urządzeniu. Default w Audio nie ma podpisu „Chosen by the player” (brak stringa w projekcie, nie dodano). Usunięto pozostałości TEMP_PREVIEW (fałszywe napisy w sync, stałe `hasSelectedSubtitle`/`hasActiveSubtitleTrack`, wymuszone Up next).
- [x] **3.3 Panel napisów**
  Grupy językowe, encoding, z pliku, z internetu, „pierwsze dostępne”.
  Notatki: Napisy (Brak napisów z radio po lewej, Kodowanie z wartością, Wczytaj z pliku/Internetu/pierwsze dostępne, sekcja „AVAILABLE • N”, grupa z chevronem i licznikiem) i Kodowanie (wsteczny crumb, 34 dp, radio) sprawdzone na Chromecaście (`c3_33_subs3.png`, `c3_33_group.png`, `c3_33_enc.png`). Dodano radio po prawej przy napisach w grupie (makieta), wcześniej brak. Niesprawdzone na urządzeniu: radio przy napisach w grupie i wiersze z napisami zewnętrznymi (strumień testowy miał tylko napisy embedded, a po wczytaniu zewnętrznym nie testowano). Wartość „Auto” w makiecie to „Automatyczny” w tłumaczeniu aplikacji.
- [x] **3.4 Napisy online**
  Wyszukiwanie, język, logowanie (zależnie od decyzji z 0.4).
  Notatki: Wczytaj z Internetu sprawdzone na Chromecaście względem PlayerOnlineSubs i stanów: Szukaj z wartością, Język napisów z flagą, „No items in this list”, sekcje OPENSUBTITLES/SUBDL z wierszem logowania (`c3_34_online.png`), poziom wyboru języka z crumbem „Wczytaj z Internetu” i radio (`c3_34_lang.png`). Pole Szukaj otwiera klawiaturę TV po OK (zachowanie pola tekstowego). Niesprawdzone na urządzeniu: stan „Wyszukiwanie w toku”, wyniki (4 wiersze), błąd źródła + Try Again, pobieranie (spinner w wierszu) – brak działającego źródła w sieci testowej; kod istnieje, bez zrzutów.
- [x] **3.5 Sync napisów**
  Lista dialogów + ±100 ms / ±1 s / custom / reset, OK na linii dopasowuje offset (zależnie od decyzji z 0.4).
  Notatki: panel Sync (590 dp, lista z NOW i wierszem „OK: align…” na biało, prawa kolumna: wartość 72 px, ±1 s/±100 ms, Custom, Reset) sprawdzony na Chromecaście tylko na 40 SYNTETYCZNYCH liniach z tymczasowego podglądu (usuniętego przed commitem); OK przesuwa opóźnienie (`c3_35_syncpanel2.png`, `c3_35_focus_list.png`, `c3_35_ok.png`). Nie sprawdzone: prawdziwe napisy, bo wybrany napis (zewnętrzny, z listy) gasł po restarcie odtwarzania i wracał do „Off” (patrz Rozbieżności); dialog „Custom value…” (klawiatura) i nagłówek „Polish • SRT” (wymaga napisów).
- [x] **3.6 Up next**
  Z odliczaniem (autoplay) lub bez.
  Notatki: karta wg PlayerUpNext (900 px, plakat 160 dp, „UP NEXT”, „S3:E3 • tytuł”, opis 2 linie, „Play now • N s” z pierścieniem / „Play next episode” + „Watch credits” bez autoplay). Poprawki: usunięto stały fallback „S3:E3” i tymczasowe wymuszenie widoczności; karta pojawia się tylko w oknie 8 s (autoplay) lub 30 s (bez autoplay), więc licznik nie pokazuje już wartości „2147483647”. Decyzja: gdy karta jest widoczna, pasek kontrolek jest ukryty (wcześniej nachodził na Sync/Audio/Aspect i kradł focus). „Anuluj” nie jest ucinane (wcześniej „An…”); „Play now” może się skrócić. Sprawdzone na Chromecaście tylko w tymczasowym podglądzie (film bez następnego odcinka, więc karta nie pojawia się naturalnie): `c3_36_card2.png`. Nie sprawdzone: prawdziwy koniec odcinka serialu, autoplay, plakat (brak w teście).
- [x] **3.7 Stany Playera**
  Ładowanie, błędy, buforowanie.
  Notatki: Ładowanie sprawdzone na Chromecaście (tytuł, „StreamPlay”, spinner, „Sources found: N”, „Pomiń ładowanie” wyszarzony do pierwszego źródła; `c3_32_s8.png`, `c3_33_p_a.png`). Tekst w aplikacji to „Loading sources…” (istniejący string), makieta ma „Loading links…”, zostawiono string aplikacji. Błąd „Nie znaleziono linków” + Try Again (biały) / Wstecz sprawdzony (`c3_36_card.png`); linia pod tytułem w makiecie to „tytuł • S3:E2 • źródło” — w kodzie był tylko źródłem (metadane pustego stanu), poprawiono `PlayerScreenCoordinatorLoading` (tytuł z bieżącego metadata), po poprawce nie wystąpił błąd ponownie, więc niesprawdzone na urządzeniu. Niesprawdzone: dialog „Source error” (Try Again/Wstecz), toast „Trying the next source” i stan bufora, bo nie wystąpiły na testowym strumieniu.
  DoD ekranu.

---

## Etap 4: Domknięcie

- [x] **4.1 Przegląd 1:1 z makietami**
  Porównaj każdy ekran i stan z makietą. Wynik: lista odchyleń (jeśli są).
  Wynik przeglądu (2026-10-27, emulator + Chromecast, bez dużych odchyleń): zaakceptowane/odłożone: siatka Biblioteki 6 kolumn (decyzja), kolejność chipów kont i rok w slocie tytułu (dane), avatar w pasku (profile po MVP), panele boczne Playera z przyciemnionym tytułem/osią/pigułkami (makiety do dostosowania na koniec), wielkie litery linii odcinka i surowa nazwa źródła (API), opóźnienie ekranu ładowania Playera/backdrop (obrazek z API), auto-ładowanie napisów „Polish 1” (do ręcznego sprawdzenia), „Sources”/„Play now” po angielsku (wspólny string/makieta), podgląd Watch status ~2 dp niżej. Niesprawdzone na ekranie: stany Szczegółów (VPN, Coming soon, ładowanie, niedostępny), stany Home/Biblioteki/Pobranych poza pustym, wiersze Pobranych (poza danymi testowymi), Audio, komunikaty Playera (tylko podgląd), „Nie znaleziono linków”.
  Notatki (fix): dialog usuwania pluginu z pozycją 330 px (`TvConfirmDialog.topOffset`, tylko ten dialog); URL na ekranie QR 13 sp/600 (26 px); podgląd list radio/multi-select pokazuje ✓ przy wybranych opcjach (makieta ma pusty podgląd, więc to odstępstwo; sprawdzone na emulatorze); Home: nagłówek feedu pod paskiem nie został odtworzony na emulatorze (feed schowany pod nieprzezroczystym paskiem, bez nachodzenia), bez zmian; dialog i QR tylko w kodzie.
- [x] **4.2 Przegląd D-pad**
  Przejdź każdy ekran samym pilotem: brak pułapek focusu, Back przewidywalny.
  Notatki (fix): „Delete plugin” w podglądzie, „Delete” w wierszu akcji i „Delete plugin” jako tytuł dialogu są zgodne z makietami SettingsPlugin/SettingsPluginActions; bez zmian.
- [ ] **4.3 Wydajność** (odłożone, patrz notatka)
  Porównaj z baseline (0.5). Wypisz miejsca do optymalizacji.
  Odłożone na życzenie użytkownika (nie mierzymy wydajności na wolnym Chromecaście, decyzja 2026-10-26); pomiar release wykona użytkownik. Znane miejsca do ewentualnej optymalizacji: p99 przewijania Home w buildzie debug (ładowanie obrazów), nakładki Playera (stos komunikatów, górna maska), Back w Playerze ignorowany 10–15 s podczas buforowania następnego odcinka.
- [x] **4.4 Zgodność z upstream**
  Zrób diff względem upstream i wypisz wszystkie zmiany poza `<KATALOG_MATERIAL>` (sekcja „Zmiany poza katalogiem Material”). Zminimalizuj je, tam gdzie się da, przenosząc logikę do Material. Zmianę domyślnego layoutu i usuwanie czegokolwiek z legacy robisz tylko po zgodzie użytkownika.
  Notatki: względem `82278bc8` poza `tv/` (main) jest 6 plików (+191/−96; 3 testy, 2 zasoby, 1 wpięcie w `SettingsUIScreen`). Względem upstream `eb962b4f` jest 102 pliki (57 testów), +10148/−650; największe ryzyko merge to `syncproviders/SyncAPI.kt`, `AccountManager.kt`, `SubtitleRepo.kt` (przepisane) oraz `strings.xml` i legacy `MainActivity`/`ResultFragment`/`Globals`. Kandydaci do Material: `TmdbApi`, adaptery napisów i Home/Result. Bez zmian kodu; zmiana layoutu i usuwanie z legacy tylko po zgodzie.

---

## Wyniki audytu (uzupełnia agent w 0.1)

**Ścieżki poniżej są względem katalogu Material (`…/cloudstream3/tv/`), zakładka `presentation/screens/` skrócona do `screens/`.**

| Rząd makiet | Widok Material (WIP) | Punkt wpięcia w legacy / dane | Decyzja | Uwagi |
|---|---|---|---|---|
| Home (+ Featured, Feeds, panel „More…”, States) | `screens/home/*`: `HomeScreenV2(+Content)`, `ContinueWatchingHero*`, `FeaturedCarousel*`, `FeedSection`, `FeedMiniGrid`, `QuickSourcesRow`, `SourcesMorePanel`, `HomeFeedGridScreen`; top bar: `screens/dashboard/*` | NavHost w `DashboardScreen.kt` (`Screens.Home`, `HomeFeedGrid`); dane: `compat/home/*` (Continue watching, Featured, Feed, Source repo) i legacy `DataStoreHelper` | **Dokończyć** | Struktura i kolejność sekcji już jak w makiecie (niezacommitowany WIP użytkownika). Do sprawdzenia: stany z planszy Home States, animacja Featured trwa 1000 ms (limit 150 ms), tokeny w dp mają własne pliki (`*Tokens.kt`) zamiast jednego źródła. |
| Wyszukiwanie | `screens/search/*`: `SearchScreen`, `SearchViewModel`, `SearchFeedGridScreen`, `SearchSectionUiState` | `Screens.Search` w Dashboard NavHost, `SearchPrefillStore`, `SourceRepository` | **Przebudować w Material** | Sekcje per źródło są; brak licznika „Searching N sources…” (rozbieżność #5). |
| Szczegóły filmu | `screens/movies/MovieDetails.kt` (903 l.), `screens/details/*` (`DetailsScreen*`, `DetailsTabs`, `DetailsOverlayPanels`, `DetailsDownloadButton*`), `common/DownloadActionButton`, `screens/unavailable/*` | `TvApp.kt` NavHost (`MovieDetails`, `…Resolve`), `compat/MovieDetails*` (mirrory, akcje, pobieranie), `data/mappers/LoadResponseMapper`, `DetailsScreenViewModel` | **Przebudować w Material** | Duży, wielowarstwowy kod; panele: mirrory + „Skip Loading” i status oglądania już istnieją. `DetailsTabsRow` używa `tv.material3` z własnym stylem. |
| Szczegóły serialu | `screens/tvseries/TvSeriesEpisodeSection.kt` (557 l.), `screens/details/DetailsSeriesItems`, `DetailsEpisodes*` | jak wyżej (`TvSeriesDetails`), `compat/MovieDetailsEpisode*` | **Przebudować w Material** | „Extras” (sezon 0) jest obsłużony; `nextAiring` jest w modelu (`MovieDetails.kt`, mapper). `TvSeriesEpisodeSection` ma `onLongClick` (łamie zasadę 2). |
| Ustawienia | `screens/settings/masterdetail/*` (`MasterDetailSettingsScreen`, `SettingsHost`, `*SettingsFeature`, `SettingsPanelComponents`), `settings/account/*`, `settings/extensions/*` | `Screens.Settings` w Dashboard NavHost; preferencje legacy (SharedPreferences), `compat/PluginSettingsHostActivity`, `compat/ExtensionsCompat`, `compat/PluginStatsCompat` | **Dokończyć / przebudować** | Model master-detail już jest (rząd 1040/648 trzeba zweryfikować). `SettingsEntryType` = Item, Header, Toggle, Slider; **brak multi-select** (rozbieżność #2). QR i formularz logowania istnieją w `ProviderAccountScreen`. |
| Biblioteka | `screens/library/*`: `LibraryScreen`, `LibraryViewModel`, `LibrarySourceSelectorRow`, `LibraryFeedGridScreen`, `LibraryGridSorter` | `Screens.Library`/`LibraryFeedGrid`, `syncproviders/AccountManager`, `TmdbLibraryNavigation` | **Dokończyć** | Przełącznik źródeł list i sortowanie (z testami `LibraryGridSorterTest`) są. Siatka w makiecie ma 7 kolumn 220×330, mini-grid 6 kolumn 261×392. |
| Pobrane | `screens/downloads/*`: `DownloadsScreen`, `DownloadItemCard`, `DownloadsViewModel`, `DownloadsModels` | `Screens.Downloads`, `compat/downloads/*`, legacy `VideoDownloadManager` | **Dokończyć** | Stany Paused/Failed, prędkość i ETA są w modelu. `DownloadItemCard` ma własny focus (border 3 dp + scale), do ujednolicenia w 1.x. |
| Player | `screens/player/*` (`TvPlayerScreen`, `overlay/*`, `panels/*`, `subtitlesync/*`, `state/*`, `core/*`) | `TvApp.kt` route `TvPlayer`; legacy `ui/player/*` (GeneratorPlayer, decoder napisów, `PlayerGeneratorViewModel`) | **Przebudować w Material** (etap 3) | Są: Restart, Video, Sources, Next, Subtitles, Sync (lista dialogów), Audio, Aspect, napisy online z OpenSubtitles. **Kolejność w kodzie ≠ makieta** (rozbieżność #6). Player zawiesza emulator, więc weryfikuje użytkownik na urządzeniu. |
| Komponenty | `common/*` (`PosterCard`, `ChipsRow`, `DesignButtonStyle`, `DesignChipStyle`, `MenuListSidePanel`, `SlidingSidePanel`, `TvConfirmDialog`, `Loading`, `Error`, `TvErrorScreen`, `ActionIconsPill`), `theme/*`, `focus/FocusHelpers`, `utils/*` | brak (czysty Material) | **Przebudować** | Brak galerii (1.11). Focus jest zrobiony na kilka sposobów (patrz niżej). |
| Wybór profilu (dodatkowy rząd, brak w planie) | `screens/profile/ProfileScreen.kt` (1060 l.) | `Screens.Profile`, legacy `AccountSelectActivity2` | **Pytanie do użytkownika** | Rozbieżność #7. |

### Stan techniczny (wnioski z audytu)

- **`androidx.tv`**: tak. `androidx.tv:tv-material:1.0.1` (`tv.material3`: `Surface`, `Button`, `FilterChip`, `Tab`, `ClickableSurfaceDefaults`), używane w ok. 79 plikach. Compose BOM 2026.01.01. Nawigacja: `navigation-compose` (dwa NavHosty: `TvApp` na ekrany pełne i `DashboardScreen` na zakładki).
- **Focus dziś**: `FocusRequester` + `FocusHelpers.kt` (`requestFocusWithRetry`, `FocusRequestEffect`), osobne store'y focusu (`HomeFocusStore`, `LibraryFocusStore`, `*GridSelectionStore`). Brak jednej infrastruktury (zadanie 1.10). Style focusu są różne: `PosterCard` rysuje własny ring, `DownloadItemCard` border 3 dp + animowany scale, `CloudStreamSurfaceDefaults` ma `glow` i scale 1.06, przyciski/chipy mają biały fill (`DesignButtonStyle`, `DesignChipStyle`, zgodnie z regułą).
- **Naruszenia zasad do wyprostowania przy odpowiednich zadaniach**: `onLongClick` w `PosterCard` i `TvSeriesEpisodeSection` (zasada 2); `Glow`/`shadow`/`blur` w 6 miejscach (zasada 3); animacje 1000 ms (`FeaturedCarouselTransitionDurationMs`) i 1200 ms (zasada 3); `FeaturedCarouselShadow*` (cień tekstu).
- **Skala**: Chromecast/emulator 1080p to **320 dpi = 960×540 dp**, więc 1 dp = 2 px makiety. Kod Home już tak liczy (komentarz w `ContinueWatchingHeroTokens.kt`). Zweryfikowane: `wm density` = 320 na AVD `Television_1080p_2`.

### App layout (zadanie 0.2)

- Ustawienie: `AppLayoutMode` w `ui/settings/AppLayoutMode.kt`, klucz prefów `app_layout_key`: Automatic −1, Phone 0, Tv 1, Emulator 2, **TvMaterial 3** (ukryty w kreatorze `visibleInSetup = false`).
- Wybór hosta: `Context.selectedHostActivityClass()` zwraca `TvMainActivity` dla `TvMaterial`, w pozostałych przypadkach `MainActivity`. Wejścia: `TvLaunchRouterActivity` (launcher), `AppIntentRouterActivity`, `AccountSelectActivity2`; `Activity.restartIntoSelectedAppLayout()` restartuje do właściwego hosta.
- UI ustawienia: `ui/settings/SettingsUIScreen.kt` (niezacommitowana zmiana użytkownika dodaje opcję TvMaterial do listy i restart hosta).
- Rejestracja ekranów Material: `TvMainActivity` → `TvApp` (NavHost, trasy w `Screens`) → `DashboardScreen` (NavHost zakładek). Nowy ekran = nowa trasa w `Screens.kt` + `composable(...)` w jednym z dwóch NavHostów. **Nie dodawaj własnej flagi.**
- Weryfikacja (emulator, build `prereleaseDebug`): `app_layout_key=3` → `TvLaunchRouterActivity` → `TvMainActivity` → działa ekran Home (nawigacja Home/Library/Downloads/Settings/Search, siatki 6 kolumn, kafel „Show more”). Build `compilePrereleaseDebugKotlin` i `assemblePrereleaseDebug` przechodzą.

## Tokeny (uzupełnia agent w 0.3)

Źródło: pliki `.dc.html` makiet (rzędy Components, Main, Movie*, Series*, Settings*, Downloads, Library*, Player*). Wartości w **px makiety**; w kodzie `dp = px / 2` (patrz Skala).

**Skala dp↔px**: ekran 1920×1080 px = 960×540 dp (320 dpi, potwierdzone na emulatorze 1080p i zgodne z Chromecast 1080p). `sp` w `dp/2` analogicznie. Safe-area/overscan: makiety **nie definiują osobnego overscanu**; stały margines boczny to **96 px (48 dp)**, góra: top bar 112 px (56 dp) od y=0, dół: feed celowo wychodzi poza ekran (widać początek następnego rzędu). Kod ma dziś inny `ParentPadding` (58 dp) i `DashboardTopBarHorizontalPadding` (48 dp), do ujednolicenia w 1.1.

### Kolory
| Token | Wartość | Użycie |
|---|---|---|
| Ground | `#0E1015` | tło ekranów Home/Library/Downloads/Settings/Search |
| Card | `#1B1F27` | karta aktywna w Ustawieniach, Featured |
| Panel | `#171A21` | panele boczne (Sources, Sort), kafel „Show more” |
| Preview | `#13161C` | prawa karta podglądu w Ustawieniach |
| Poster placeholder | `#1E222B` | tło plakatu/stilla przed załadowaniem |
| Focus | `#F1F3F7` (tekst na focusie `#0E1015`) | biały fill i ring |
| Text / Text 2 / Body / Meta | `#F1F3F7` / `#A9B0BD` / `#D5D9E1` / `#C9CED8` | tekst |
| Label | `#8790A0` (Components) i `#8E95A3` (ekrany) | etykiety, liczniki (niespójność, #4) |
| Accent (HCT, per tytuł) | `#F4B97A` (Components, Film) / `#F2B880` (Home) / `#8FD3E0` (Serial, Featured) | eyebrow, progress, aktywny tab, chip wybrany |
| Selected / app | `#9CC1FF` | radio, wybrany chip biblioteki, pasek pobierania |
| Toggle on | `#3F6FE0` (thumb `#FFFFFF`) | przełącznik |
| Fill przycisku | `rgba(255,255,255,.13–.14)`; disabled `.06` | przyciski, ikonowe przyciski, pill |
| Fill taba bieżącego / chipa | `.10` / `.07`, border chipa `.14` | top bar, chipy |
| Progress | track `.22` (karty CW), `.18` (odcinki), `.14` (pobrane); wypełnienie = accent | 6 px (pobrane 8 px) |
| Scrim | `rgba(5,6,9,.62–.66)` pod panelami bocznymi | panele |
| Kropki separatorów | `#6B7280`, `#5A6170` | „•” w metadanych |
| Tint ekranu Film | tło `#120E0B`, tekst `#F3EFEA`, focus `#F7F4F0` | szczegóły filmu (tint z HCT, #4) |
| Tint ekranu Serial | tło `#0B1315`, tekst `#EEF4F5`, focus `#F3F7F8` | szczegóły serialu |

### Typografia (Figtree, #1)
| Rola | px / weight | Gdzie |
|---|---|---|
| Hero tytułu | 96/800 (Home), 104/800 (Film), 80/800 (Featured), 52/800 (Player), 44/800 (nagłówek Serial) | tytuły |
| Title | 72/800 | skala w Components |
| Level | 48/800 | nagłówek Ustawień |
| Section | 36/700 (panel 36–38/700; siatka 40/700) | nagłówki feedów i paneli |
| Row / tab | 28/600 (Ustawienia: 28/600 + podtytuł 22/400; sezon 27/600) | wiersze, taby |
| Body | 26/400, line-height 1.5, max 3 linie | opis |
| Meta | 22/500 (24–26 w hero) | rok, ocena, liczniki |
| Przycisk | 24–26/600 (wys. 68–72) | CTA |
| Chip | 22/500 (wys. 56, Library 52) | źródła |
| Eyebrow | 20–22/700, uppercase, letter-spacing .12em | „Continue watching”, „Source” |
| Tytuł odcinka | 30/700, meta 22/500, opis 22/400 (2 linie) | lista odcinków |
| Pobrane | tytuł 28/700, meta 21/500, opis 20/400 | wiersz pobranego |
| Player | opcja 22/600 + wartość 22/500, czas 24/600 tabular | kontrolki |

### Wymiary i odstępy (px)
| Element | Wartość |
|---|---|
| Top bar | wys. 112, padding boczny 96, logo/avatar 52, tab: font 26, padding 12×26, `radius 999` |
| Plakat mini-grid | 261×392, r14, gap 32×32, 6 kolumn, kontener 1726 px szer.; kafel „Show more” taki sam |
| Plakat siatki (Show more) | 220×330, r12, 7 kolumn, gap 30×36 |
| Karta Continue watching | 352×198 (16:9), r14, gap 28, pasek postępu 6 |
| Featured | 1728×680, r24, ring 4 + offset 5, wskaźnik kropek w prawym dolnym rogu |
| Karta obsady | 184×240, r14, imię 22/600, rola 20/400 |
| Wiersz odcinka | wys. 200, still 300×169 r12, padding 0×16, gap 32, kolumna akcji 240 (3 przyciski 64 px, gap 16), r20 |
| Kolumna sezonów | szer. 300, wiersz wys. 84, r16, gap 8; separator pionowy x=420 |
| Wiersz pobranego | wys. 150, still 208×117 r10, gap 28, kolumna akcji 360, pill 56 (r28) |
| Przycisk CTA | wys. 68 (72 w Home), padding 0 30 0 24, ikona 26, gap 12, r pill; focus scale 1.04 |
| Przyciski ikonowe (odcinek) | 64×64 koło |
| Kontrolki Playera | wys. 60, padding 0 22 0 18, r30 |
| Ustawienia | karta aktywna 1040 px (x=96, y=128, wys. 904, r28, padding 40×32), podgląd 648 px (x=1176), wiersz 96 (r18), wiersz podglądu 84; toggle 72×40 |
| Panel boczny | 680 (Sources/Player), 640 (Sort), Sync 1180; wiersz 76–84, r16; padding 56–64 góra, 32 boki |
| Wiersz listy (Components) | 520×88, r18, padding 0×26 |
| Focus kart | outline 4 px biały, offset 5, scale **1.05** (Components) / 1.04 (ekrany), bez cienia |
| Focus wierszy/przycisków | biały fill + ciemny tekst; przycisk dodatkowo scale 1.04 (#3), wiersz odcinka 1.015 |
| Animacje | transform/alpha ≤ 150 ms; debounce zmiany karty ~120 ms, sezonu ~150 ms |
| Obrazy | backdrop w780, plakat w342, still w300 |

## Baseline wydajności (zadanie 0.5)

Środowisko: emulator `Television_1080p_2` (1920×1080, 320 dpi), build `prereleaseDebug` (**debug**, więc wolniejszy niż release), layout TvMaterial, źródło TMDB, stan z niezacommitowanym WIP Home.

| Pomiar | Wynik |
|---|---|
| Zimny start `TvMainActivity` (`am start -W`, 3 próby) | 2993 / 2940 / 2935 ms (pierwsze uruchomienie po instalacji przez router: 4448 ms) |
| Przewijanie Home (8× ↓ + 4× →, odstęp 400 ms), `dumpsys gfxinfo` | 100 klatek, 39% janky; p50 32 ms, p90 200 ms, p95 250 ms, p99 1300 ms; GPU p90 24 ms |

Uwagi: emulator bez GPU urządzenia i build debug zawyżają wartości; ładowanie obrazów w trakcie pomiaru wpływa na p99. **Pomiar na Chromecaście (release) zrobi użytkownik.** Polecenia: `adb shell am start -W -n com.lagradost.cloudstream3.prerelease.debug/com.lagradost.cloudstream3.tv.TvMainActivity`, `adb shell dumpsys gfxinfo <pkg> reset` przed i bez argumentu po przewijaniu.

## Inwentarz komponentów (uzupełnia agent w 0.6)

Liczba użyć = liczba **ekranów/rzędów makiet**, w których element występuje (nie liczba instancji). Reguła: baza dla ≥ 3.

| Element | Użycia w makietach | Istnieje w Material? | Decyzja |
|---|---|---|---|
| Top bar (tabs + search + avatar) | Home, Library, Downloads, Search, Settings (5) | tak: `dashboard/DashboardTopBar` | **baza** (zmodyfikować) |
| Przycisk CTA (pill 68–72 px, ikona + etykieta, accent/domyślny/focus/disabled) | Home hero, Featured, Film, Serial, Details States, Components (6+) | częściowo: `DesignButtonStyle` (tylko kolory) | **baza** |
| Chip (źródło / źródło list) | Home, Home Sources, Library, Home States (4) | tak: `DesignChipStyle`, `ChipsRow`, `QuickSourcesRow` | **baza** (zmodyfikować) |
| Karta plakatu (z focus ring) | Home Feeds, Featured, Library, Library Grid, Search, Details „More like this” (6) | tak: `PosterCard`, `FeedPosterCard` | **baza** (zmodyfikować, usunąć long-click) |
| Mini-grid 2×6 + „Show more” | Home, Library, Search (rząd źródła) (3) | tak: `FeedMiniGrid`, `FeedSection` | **baza** |
| Wiersz listy (fill biały na focusie; ikona, tytuł, podtytuł, trailing: › / check / radio / toggle / wartość) | Ustawienia (8 plansz), panele Playera (7), Sources, Sort, Watch status, Mirror, menu odcinka (≥ 15) | częściowo: `MenuListSidePanel`, `settings/SettingsPanelComponents` (dwa różne światy) | **baza** (scalić) |
| Panel boczny (680/640 px, scrim, trap focusu, Back zamyka) | Sources, Sort, Watch status, Mirror, menu odcinka, 7× Player (≥ 11) | tak: `SlidingSidePanel`, `MenuListSidePanel` | **baza** (zmodyfikować) |
| Dialog potwierdzenia | Delete download, plugin delete, PIN, Custom value (4) | tak: `TvConfirmDialog` (animacja 180 ms, scale; do ujednolicenia) | **baza** (zmodyfikować) |
| Stany: ładowanie (szkielet) / pusto / błąd z „Try again” | Home, Search, Library, Downloads, Settings, Series, Details, Player (8) | tak: `Loading`, `Error`, `TvErrorScreen`, `ShimmerCard` | **baza** (scalić) |
| Pasek postępu (6 px, accent) | CW karta, odcinek, pobrane, Components, Player timeline (5) | rozproszony w kartach | **baza** (mały, jeden composable) |
| Pill akcji w wierszu (Play/Delete 56 px) | Pobrane (1) + Player opcje 60 px (1) | `ActionIconsPill`, `PlayerControlButton` | lokalnie przy ekranie (z tokenów) |
| Przycisk ikonowy 64 px (odcinek: watched/download/more) | Serial (1 ekran, 3 instancje) | `ActionIconsPill`, `DownloadActionButton` | lokalnie przy ekranie |
| Tab (28/600 + podkreślenie accent) | Film (5 plansz), Serial (4 plansze) (2 ekrany) | tak: `DetailsTabsRow` | lokalnie przy ekranie Szczegóły (wspólny dla filmu i serialu), z tokenów |
| Karta Continue watching (352×198, badge SxEy, pasek) | Home (1) | tak: `ContinueWatchingHeroCard` | lokalnie |
| Karta odcinka / wiersz odcinka (still 300×169) | Serial (1) | tak: `TvSeriesEpisodeSection` | lokalnie |
| Wiersz pobranego (still 208×117, status, akcje) | Pobrane (1) | tak: `DownloadItemCard` | lokalnie |
| Karta obsady (184×240) | Film (1) | tak: `CastAndCrewList` | lokalnie |
| Karuzela Featured + kropki | Home (1) | tak: `FeaturedCarousel*` | lokalnie |
| Slider (←/→) | Ustawienia (1; Player ma osobny timeline) | tak: wewnątrz `SettingsPanelComponents` | **pominięte: lokalnie przy ekranie** (zadanie 1.8) |
| Multi-select | Ustawienia (1) | nie | **pominięte: lokalnie przy ekranie** (zadanie 1.8, zależnie od #2) |
| Toggle (72×40) | Ustawienia (w wierszu listy) | tak: `SettingsEntryType.Toggle` | część wiersza listy |
| Pole tekstowe / klawiatura | Search, Login, Add repo, Edit account (4) | tak: `SearchInputField`, formularze w `settings/account` | **baza** (wiersz-pole z tego samego wiersza listy) |

Wynik dla etapu 1: 1.3 baza (przycisk, chip) + taby lokalnie, 1.4 baza, 1.5 baza (plakat) + karty CW/odcinka/pobranego lokalnie, 1.6 baza, 1.7 baza, 1.8 pominięte (lokalnie), 1.9 baza.

## Zmiany poza katalogiem Material (uzupełnia agent w trakcie pracy)

Punkt odniesienia 4.4: commit `82278bc8` („feat(TV UI): new Compose TV UI and redesign”, początek redesignu). `git diff --numstat 82278bc8..HEAD` poza `tv/` (main), `docs/`, `.gitignore` i `res/font/` daje **6 plików, +191/−96** (3 z nich to testy). Ponieważ `82278bc8` sam zawiera pierwsze wpięcia TV, to nie jest pełne odchylenie od upstream. Prawdziwy upstream to `eb962b4f` (poprzedni commit): **102 pliki (57 testowych, 45 pozostałych), +10148/−650**. Część B opisuje ten zakres, bo to on decyduje o ryzyku merge.

### A. Względem 82278bc8 (zakres zadania 4.4)

| Plik | Powód | Zmiana | Ocena |
|---|---|---|---|
| `app/src/main/java/com/lagradost/cloudstream3/ui/settings/SettingsUIScreen.kt` | opcja TvMaterial w liście „App layout” (zamiast tablicy xml) i restart hosta przy przełączeniu Compose/legacy (`1b314c8e`) | +14/−5 | (a) punktowe wpięcie |
| `app/src/main/res/values/strings.xml` | napisy `tv_*` dla Home, Search, szczegółów, Biblioteki, Ustawień i Playera; 4 napisy `tv_unavailable_details_*` zmienione z polskiego na angielski wg makiety | +119/−5 | (c) zasoby |
| `app/src/main/res/values-b+pl/strings.xml` | polskie tłumaczenia `tv_player_*` (sync, kontrolki, powiadomienia, seek) | +12/−0 | (c) zasoby |
| `app/src/androidTest/java/com/lagradost/cloudstream3/tv/presentation/screens/player/overlay/PlayerBottomControlBarTest.kt` **[TEST]** | test kontrolek Playera pod `PlayerBottomControlBarConfig` i etykiety w pigułkach (3.1, `4407bdea`) | +23/−69 | (b) test w pakiecie `tv`, przenosi się z `tv/` |
| `app/src/androidTest/java/com/lagradost/cloudstream3/tv/presentation/screens/player/overlay/PlayerOverlayContentTest.kt` **[TEST]** | test etykiet w pigułkach (3.1) | +14/−8 | (b) test w pakiecie `tv` |
| `app/src/androidTest/java/com/lagradost/cloudstream3/tv/presentation/screens/settings/account/ProviderAccountScreenNavigationTest.kt` **[TEST]** | kolejność akcji konta wg makiety (Add, Create, Log out, 2.14) | +9/−9 | (b) test w pakiecie `tv` |

Pominięte z zakresu: `res/font/figtree_variable.ttf` (zasób, c) i `.gitignore`. Poprawki względem poprzedniej wersji tabeli: `SettingsUIScreen.kt` jest już zacommitowany (wcześniej „niezacommitowane”), rozmiary przeliczone względem `82278bc8`, a brakujące wpisy dodano: `values-b+pl`, dwa testy Playera.

### B. Względem upstream eb962b4f (odchylenie do merge)

| Grupa | Pliki (+/−) | Ocena |
|---|---|---|
| Konta i synchronizacja | `syncproviders/SyncAPI.kt` +195/−194; `syncproviders/AccountManager.kt` +171/−167; `syncproviders/SubtitleRepo.kt` +97/−95; `syncproviders/providers/TmdbApi.kt` +715 (nowy); `library/src/commonMain/.../syncproviders/SyncAPI.kt` +1/−1 (kopia w module) | SyncAPI/AccountManager/SubtitleRepo: (a) przepisane w miejscu; TmdbApi: (b) kandydat do `tv/` |
| Legacy UI (punkty styku) | `MainActivity.kt` +2/−38; `ui/result/ResultFragment.kt` +8/−29; `ui/settings/Globals.kt` +10/−21; `res/layout/device_auth.xml` +58/−23; `ui/settings/SettingsAccount.kt` +51/−16; `ui/home/HomeViewModel.kt` +50/−14; `ui/player/CustomSubtitleDecoderFactory.kt` +58/−9; `ui/setup/SetupFragmentLayout.kt` +9/−5; `ui/settings/SettingsUI.kt` +6/−5; `ui/result/ResultViewModel2.kt` +5; `ui/player/GeneratorPlayer.kt` +20; `ui/player/DownloadFileGenerator.kt` +16/−3; `ui/player/PlayerGeneratorViewModel.kt` +1/−1; `ui/account/AccountSelectActivity2.kt` +9; `res/xml/settings_account.xml` +5/−1; `navigation/mobile_navigation.xml` +1 | (a) punktowe wpięcia; część logiki do adapterów (patrz kandydaci) |
| Nowe pliki i aktywności | `ui/settings/AppLayoutMode.kt` +106; `ui/player/SubtitleFetchLogSupport.kt` +99; `reporting/AppErrorReporter.kt` +116; `AppIntentRouterActivity.kt` +24; `CloudstreamAppRedirectHandler.kt` +64; `AndroidManifest.xml` +49/−9; `CloudStreamApp.kt` +17/−2; `utils/downloader/DownloadManager.kt` +44/−2; `DownloadObjects.kt` +2; `utils/DataStoreHelper.kt` +1 | (a) (routing, manifest, pobieranie) |
| Zasoby | `res/values/strings.xml` +204; `res/values-b+pl/strings.xml` +29; `res/values/donottranslate-strings.xml` +1; `res/drawable/tmdb_logo.xml` +9 (nowy); `res/drawable/tv_halo_sprite.png` (binarny, nowy) | (c) |
| Build | `app/build.gradle.kts` +39; `gradle/libs.versions.toml` +19 | (a) |
| Poza kodem | `README.md`, `fastlane/metadata/android/{en-US,it-IT,mt,ur}/full_description.txt` (+1/−1 każdy); `.codex` (pusty plik, nowy) | do decyzji przed merge; nic nie usuwam bez zgody |

Testy (57 plików w `eb962b4f..HEAD`) są w `app/src/test` i `app/src/androidTest` w pakiecie `tv`; przenoszą się razem z kodem Material.

### Kandydaci do przeniesienia do Material (lista, bez implementacji)

- `syncproviders/providers/TmdbApi.kt` (nowy, +715): repozytorium TMDB dla TV, do `tv/` jeśli legacy go nie używa.
- `ui/player/SubtitleFetchLogSupport.kt` i `ui/player/CustomSubtitleDecoderFactory.kt`: logika napisów jako adapter w `tv/`; dekoder współdzielony zostaje w legacy.
- `ui/home/HomeViewModel.kt`, `ui/result/ResultViewModel2.kt`, `ui/result/ResultFragment.kt`: mapowanie danych dla TV do wrappera w `tv/compat` (katalog już istnieje).
- `ui/player/GeneratorPlayer.kt`, `DownloadFileGenerator.kt`, `PlayerGeneratorViewModel.kt`: w legacy zostaje tylko wpięcie, reszta do adaptera.
- `ui/settings/SettingsAccount.kt`, `Globals.kt`, `ui/setup/SetupFragmentLayout.kt`, `ui/account/AccountSelectActivity2.kt`: częściowo do `tv/`.
- Zostaje w legacy (a): `AndroidManifest.xml`, `MainActivity.kt`, `AppIntentRouterActivity.kt`, `CloudstreamAppRedirectHandler.kt`, `CloudStreamApp.kt`, `AccountManager.kt`, `SyncAPI.kt`, `SubtitleRepo.kt`, `DownloadManager.kt`, `AppErrorReporter.kt`, pliki build, `strings.xml` (zasoby są centralne).

### Ryzyko konfliktów przy merge z upstream (od najwyższego)

1. `syncproviders/SyncAPI.kt` (+195/−194), `AccountManager.kt` (+171/−167), `SubtitleRepo.kt` (+97/−95): prawie cały plik przepisany; każda zmiana upstream w tych plikach to konflikt trudny do ręcznego scalenia. Kopia w `library/.../SyncAPI.kt` może się rozjechać z aplikacyjną.
2. Legacy z usuwaniem linii: `MainActivity.kt` (−38), `ResultFragment.kt` (−29), `device_auth.xml` (−23), `Globals.kt` (−21), `SettingsAccount.kt` (−16), `HomeViewModel.kt` (−14), `AndroidManifest.xml` (−9), `CustomSubtitleDecoderFactory.kt` (−9).
3. `res/values/strings.xml` (+204) i `values-b+pl` (+29): nowe napisy dopisywane na końcu pliku, tam samo dopisuje upstream, więc konflikt przy każdym merge. Dodatkowo `tv_unavailable_details_*` są po angielsku w `values/`, a `values-b+pl` ich nie ma, więc polski użytkownik zobaczy angielski tekst (do decyzji, c).
4. `providers/*`: jedyna zmiana to nowy `TmdbApi.kt`, ryzyko niskie.
5. Niskie: nowe pliki (`AppLayoutMode.kt`, `AppErrorReporter.kt`, `SubtitleFetchLogSupport.kt`, `AppIntentRouterActivity.kt`, `CloudstreamAppRedirectHandler.kt`, `tmdb_logo.xml`, `tv_halo_sprite.png`); konflikt tylko przy tej samej nazwie w upstream.

Rekomendacje (niewykonane, wymagają zgody): nie zmieniać domyślnego layoutu; rozważyć usunięcie `.codex`, `README.md` i `fastlane` (+1/−1) przed merge; dodać polskie tłumaczenia `tv_unavailable_details_*`; ewentualnie wydzielić `SyncAPI`/`AccountManager` do minimalnych wrapperów, żeby zmniejszyć przepisane linie.

## Rozbieżności i pytania (uzupełnia agent w trakcie pracy)

### Otwarte (Etap 3)

- **Poprawki po przeglądzie 4.1/4.2 (fix4q):** 1 „Pomiń ładowanie” zawsze focusowalny (biały, przejmuje focus), klik działa dopiero gdy canSkip; 2 wiersz „Ładowanie…” w napisach online nieaktywny, focus na polu Szukaj; 3 panel Video z podtytułem „Adaptive stream • źródło” i „Automatic quality” przy pierwszym wierszu; 5 zmiana proporcji bez Toast, wartość tylko w pigułce (Fit / Stretch / Zoom, PL: Dopasuj / Rozciągnij / Powiększ); 6 dialog opóźnienia 583 dp, 96 dp od góry; 7 spinner buforowania zostaje 24 dp, różnica 40 px makiety to artefakt skali miniatury (560 px); 8 stała minimalna szerokość etykiet czasu (72 dp); 9 polskie tłumaczenia kluczy tv_player_* (UP NEXT i przyciski Play now / Play next / Watch credits zostają po angielsku wg makiety). Punkt 4 (ukrywanie tytułu i pigułek przy panelach) wstrzymany decyzją użytkownika. Sprawdzone na urządzeniu: tylko build i ekran główny; punkty 1, 3, 5, 6, 8, 9 NIE sprawdzone na Chromecaście (w tej sesji nie udało się wejść do playera po restarcie, ekran wyboru konta blokował). Do sprawdzenia.

- **Zmiany makiet (player-mockup-changes):** A tytuł w lewym górnym rogu (PlayerTitleBlock, maska 340 px), zweryfikowane na Chromecaście (`fix4p_title.png`); B ładowanie w układzie playera (status y 876, „Pomiń ładowanie” y 950 jako PlayerOptionButton, `fix4p_loading.png`); D buforowanie = sam spinner (bez przyciemnienia). C stos komunikatów: ViewModel trzyma PlayerNoticeStack (max 3, 5 s, bez duplikatów), karty z różnymi ikonami; zweryfikowane zrzutem z tymczasowym wstawieniem dwóch komunikatów (`fix4p_stack_load.png`), usuniętym przed commitem; prawdziwe emisje (błąd źródła przy auto-przełączeniu, błąd napisów) NIE wywołane na urządzeniu. E czas seek w minutach (`tv_player_seek_min_sec`), sprawdzone tylko w kodzie. Pozostawione: „Sources” (wspólny string) bez pl; ładowanie przed pierwszym tytułem bez bloku tytułu (metadata jeszcze puste).
- **Po teście (fix3d):** (1) Back na pauzie: usunięto wymuszanie `controlsVisible = true` przy pauzie (PlayerPlaybackEffects); NIEZWERYFIKOWANE na urządzeniu, bo testowane źródła (Moviesmod, XPass) zwracają „Source error”, a odzyskiwanie błędu ponownie pokazuje kontrolki (`onPlayerError`); Vidy [Miami] nie udało się wybrać w liście źródeł. (2) Sync na pauzie zaraz po wyborze napisu pokazuje pustą listę: NIE naprawiono, większa zmiana (cue list pochodzi z dekodera napisów, który działa przy odtwarzaniu); do zrobienia. (3) „Własna wartość…” i Dalej/Proporcje/Wył. dodane po polsku w `values-b+pl`; „Sources” (wspólny string) bez tłumaczenia, bo to string aplikacji. (5) Zmiana źródła zeruje ręczny napis i wraca auto: znane zachowanie, bez zmian.
- **Po teście (fix3c):** focus po zamknięciu Sources i Napisów wraca na kontrolkę otwierającą (zweryfikowane na Chromecaście, `fix3c_s_back2.png`, `fix3c_n_back.png`). Back chowa kontrolki także przy pauzie (zweryfikowane `fix3c_pause_*`). Po ukryciu ↓ daje focus na Play/pierwszą kontrolkę (zweryfikowane). Buforowanie: NIE sprawdzone (brak testu). Dialog Custom: stringi PL dodane w `values-b+pl`. NIEZWERYFIKOWANE: czas cue dla napisu wybranego z listy (punkt 3, 00:00:00), zachowanie napisu po przeładowaniu (punkt 4; legacy niezbadane), bo na urządzeniu nie udało się otworzyć listy źródeł po OK ponownie.
- **Napisy (zrobione):** TV player używał `NextRenderersFactory`, który tworzy `NextTextRenderer` odrzucający text/vtt i SRT („Legacy decoding is disabled”); po błędzie odzyskiwanie wyłączało napisy. Zmieniono na `FixedNextRenderersFactory` jak w legacy. Zweryfikowane na Chromecaście: napis Polski aktywny, widoczny na ekranie, pigułka Sync pojawia się (`fix3b_hide_focus.png`). Sync na prawdziwych napisach nadal do sprawdzenia.
- **Ekran braku linków (częściowo):** gałąź bez celu odczytuje tytuł i sezon/odcinek ze strony (`repository.load`), żeby pokazać „<tytuł> • S<n>:E<n> • <źródło>”. Kod zbudowany, na urządzeniu nie udało się wywołać tej gałęzi (StreamPlay zwraca linki albo błąd innej gałęzi), więc niesprawdzone.
- **Focus kontrolek:** po zamknięciu panelu focus wraca na kontrolkę, która go otworzyła; po auto-ukryciu albo Back focus idzie na Play (flaga `returnFocusToLastControl`, ważna 1,5 s).
- **Fix po teście (3.x fix):** 1 focus po Back z panelu wraca na kontrolkę (zweryfikowane); 2 ↑ z kontrolki na oś czasu (zweryfikowane z Play, pozostałe przez ten sam handler); po auto-ukryciu focus wraca na ostatnią kontrolkę (zweryfikowane); 3 wiersze Sources 48 dp z ellipsis (zweryfikowane); 7 Play/Pause stała szerokość 92 dp i etykieta wg intencji odtwarzania (zweryfikowane wizualnie). 4 Up next: NIE odtworzono (źródło StreamPlay nie zwraca linków dla testowych odcinków); przyczyna nieustalona. 5 napis gaśnie po restarcie: NIE potwierdzono regresji. Build 8c963bae otwiera inny (legacy) odtwarzacz, więc porównanie nie jest wiarygodne. Hipoteza do sprawdzenia: `onPlayerError` przy wybranym napisie wywołuje `disableSubtitlesFromPlaybackError` (PlayerPlaybackEffects), czyli błąd przy przeładowaniu wyłącza napisy. 6 makieta PlayerOnlineSubsStates ma dokładnie tekst „No items in this list”, czyli taki sam jak w aplikacji; bez zmiany. 8 nie przeglądano pełnej listy tekstów; zmieniać tylko twardo zakodowane stringi bez resource.
- **Tytuł w ekranie błędu** („Nie znaleziono linków”) pokazuje tylko źródło, a makieta ma „tytuł • S3:E2 • źródło”. Poprawka w PlayerScreenCoordinatorLoading nie zadziałała na urządzeniu; przyczyna nieustalona.
- **3.5/3.3 Wybór napisu zewnętrznego nie utrzymuje się po restarcie odtwarzania.** Na Chromecaście napis wybrany z listy („Angielski”) po przeładowaniu odtwarzania (po wyborze napisu) wraca do „Napisy Off” (dwa razy; `c3_35_pick3.png` pokazuje wybór, `c3_35_d.png` stan po restarcie). Nie zbadane, czy to regresja z WIP czy zachowanie legacy; wymaga osobnej diagnozy przed sprawdzeniem Sync na prawdziwych napisach.
- **3.6 Up next a pasek kontrolek:** decyzja wykonawcza (ukrycie paska, gdy karta jest widoczna) — makieta PlayerUpNext nie pokazuje paska, do potwierdzenia.
- **3.2 Video:** panel wideo nie jest dostępny na testowym strumieniu (jedna ścieżka wideo), niesprawdzone na urządzeniu.

### Rozstrzygnięte przez użytkownika

1. **Font: Figtree.** Pobrany: `app/src/main/res/font/figtree_variable.ttf` (czcionka zmienna `wght`, z google/fonts, licencja OFL w `docs/figtree-OFL.txt`). Podpięcie w `Typography` robi zadanie 1.1 (`Font(R.font.figtree_variable, variationSettings = FontVariation.weight(...))`). Uwaga: `minSdk` 23, a osie czcionki zmiennej działają od API 26; na starszych waga spadnie do domyślnej (urządzenia TV docelowo ≥ 28).
2. **Multi-select w Ustawieniach:** dodać nowy typ wiersza, jeśli będzie potrzebny (2.14).
3. **Skala focusu:** karty ring 4 px + scale **1.05**, przyciski i wiersze **bez skali** (sam biały fill). Dotyczy też wszystkich miejsc, gdzie makieta pokazuje 1.04/1.015.
4. **Kolory:** tam, gdzie w makietach są sztywne kolory, używać dokładnie tych wartości. Ekrany filmu i serialu mają **dynamiczne kolory z backdropu** (jak w obecnej wersji, `DetailsDynamicColor*`), a hexy tła/focusu z makiet Film/Serial są tylko przykładami wyniku.
5. **Brakujące teksty:** jeśli makieta ma tekst, którego nie ma w kodzie, najpierw szukać podobnego istniejącego stringa w resources (bez dodawania nowego); gdy nic nie pasuje, dodać nowy string bez pytania (dotyczy też „Searching N sources…” w Search).
6. **Priorytet makiety:** każdy rozjazd kod/plan vs makieta rozstrzygać na korzyść makiety i dostosować kod oraz plan. Zastosowane: kolejność kontrolek Playera (3.1) zmieniona na makietową.
7. **Wybór profilu:** pomijany w tym redesignie, wracamy do niego po MVP (na samym końcu).

### Sprawdzone: backend istnieje (bez blokady)

Kandydaci z zadania 0.4 mają już logikę w kodzie, więc można je robić zgodnie z makietą: panel mirrorów z „Skip Loading” (`DownloadMirrorSelectionStateHolder`, `R.string.skip_loading`), przełącznik źródła list Biblioteki (`LibrarySourceSelectorRow`), sync napisów z listą dialogów (`subtitlesync/PlayerSubtitleSyncDialogList`), napisy online z logowaniem (`OpenSubtitlesAccount*`, `PlayerOnlineSubtitles*`), `nextAiring` (`MovieDetails`, `LoadResponseMapper`), status/prędkość/ETA pobierania (`DownloadsViewModel`), „Original title” i „Coming soon” w szczegółach, ekran „Title unavailable” (`UnavailableDetailsScreen`), logowanie QR (`ProviderAccountScreen`), Remove w Continue watching, sezon „Extras”.

### Do potwierdzenia w trakcie odpowiednich zadań

- 2.7 (rozstrzygnięte): menu „More” odcinka ma Mark/Remove watched (oraz „up to this episode”), Play in app/browser/m3u8/mirror (akcje providerów), Auto download, Download mirror, Download subtitles, Reload links i Copy to clipboard; brak tylko etykiety „Copy link to clipboard” (jest „Copy to clipboard”).
- 3.5: czy OK na linii dialogu faktycznie dopasowuje offset („align this line with now”).
- 2.8 (rozstrzygnięte): „Season S Episode E will be released in 3d 4h 12m” z istniejących stringów `next_season_episode_format` + `next_episode_time_*_format`, liczone względem `nextAiring.unixTime`.
