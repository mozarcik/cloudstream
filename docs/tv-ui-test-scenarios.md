# CloudStream TV: scenariusze testów UI (pilot / D-pad)

Scenariusze przechodzone ręcznie na emulatorze przy weryfikacji redesignu (`docs/tv-redesign-plan.md`). Są podstawą przyszłych testów UI (Compose UI test / UiAutomator). Każdy scenariusz: warunki wstępne → kroki (klawisze) → oczekiwany wynik. Kolumna „Status” to ostatni wynik ręcznej weryfikacji.

Wartości w px odnoszą się do zrzutu 1920×1080 (1 dp = 2 px, ekran 960×540 dp).

## Środowisko

- AVD `Television_1080p_2` (1920×1080, 320 dpi), build `./gradlew :app:assemblePrereleaseDebug` (bez `--offline`), pakiet `com.lagradost.cloudstream3.prerelease.debug`.
- Layout TV Material: `<int name="app_layout_key" value="3" />` w `shared_prefs/<pkg>_preferences.xml`; start `adb shell am start -n <pkg>/com.lagradost.cloudstream3.tv.TvMainActivity`.
- Klawisze: `adb shell input keyevent KEYCODE_DPAD_UP|DOWN|LEFT|RIGHT|DPAD_CENTER|BACK`.
- Focus: `uiautomator dump` (`focused="true"`) bywa nieaktualny i potrafi przestawić focus tuż przed ↓ z Resume. Pewniejsze są zrzuty ekranu. W testach Compose używać `assertIsFocused()` na tagach.
- Emulator ma opóźnienie wejścia 1–4 s: między klawiszami czekać ≥2 s (w testach automatycznych: `waitForIdle`).
- Nie uruchamiać playera w emulatorze (OK na „Resume” lub karcie Continue watching), bo zawiesza emulator. Nie wciskać „Remove” na danych, których nie da się odtworzyć.

### Dane testowe (seed)

- Źródło: zainstalowany plugin z TMDB (np. StreamPlay) z feedami Home. W Bibliotece zalogowane konto TMDB z listą >12 pozycji (np. „Movie watchlist”).
- Continue watching, 12 pozycji bez użycia playera: force-stop aplikacji, skopiować `shared_prefs/rebuild_preference.xml` (`run-as <pkg> cat`), dopisać na każdą pozycję trzy klucze (wartości to JSON w stringu):
  - `download_header_cache/<id>`: `apiName`, `url`, `type` (`Movie`/`TvSeries`), `name`, `poster`, `backdrop`, `cacheTime`, `id` (bez prefiksu konta),
  - `0/result_resume_watching_2/<id>`: `parentId`, `episodeId`, `episode`, `season`, `updateTime` (kolejność na liście), `isFromDownload=false`,
  - `0/video_pos_dur/<episodeId>`: `position`, `duration` (ms).
  Zapis: `adb push` do `/data/local/tmp/`, potem `run-as <pkg> cp` do `shared_prefs/` (przekierowania w `run-as sh -c` nie działają). Mieszać filmy i odcinki, krótkie i bardzo długie tytuły.

## Komponenty (galeria debug)

Wejście: `adb shell am start -n <pkg>/com.lagradost.cloudstream3.tv.TvMainActivity --ez tv_component_gallery true` albo Ustawienia › Aktualizacje › „Component gallery (debug)”.

| ID | Scenariusz | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|
| G1 | Focus przycisku/chipa/wiersza | ↓/→ po elementach | Biały fill #F1F3F7, tekst #0E1015, brak skali | OK |
| G2 | Focus karty | przejść na plakat | Ring 4 px z offsetem 5 px, skala 1.05, brak cienia | OK |
| G3 | Radio w wierszu | przejść po wierszach z radiem | Radio po lewej; ramka #5A6170, na focusie #4A5160, zaznaczone #9CC1FF z kropką | OK |
| G4 | Panel boczny: trap i Back | OK na „Open side panel”, ↑/↓/←, Back | Focus startuje na zaznaczonej opcji, ↑/↓ nie wychodzi z panelu, Back zamyka, focus wraca na opener. ← zamyka panel | OK |
| G5 | Dialog: trap i Back | OK na „Open dialog”, ←/→/↑/↓, Back | Domyślny focus na Cancel, ← na akcję, strzałki nie wychodzą z dialogu, Back zamyka, focus wraca na opener | OK |
| G6 | Galeria: powrót na górę | z pierwszego przycisku ↑ | Lista przewija się na górę (sekcje Colors, Type widoczne) | OK |
| G7 | Mini-grid w galerii | przejść po 2 rzędach | Odstęp wierszy 32 px, kafel „Show more” z podtytułem | OK |

## Home

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| H1 | Start | seed 12 CW | start aplikacji | Focus na „Resume”; nagłówek „Continue Watching 12”; badge SxEy na odcinkach, paski postępu | OK |
| H2 | Przyciski hero | H1 | ←/→ | Focus Resume ↔ Details ↔ Remove, biały fill bez skali | OK |
| H3 | Karty CW: → do końca | H1 | ↓, potem → ×11 | Sfocusowana karta zawsze w całości widoczna (margines 96 px), ring nieprzycięty; 12. karta osiągalna (prawa krawędź na x=1824); hero (tytuł, meta, tło) nadąża za focusem | OK |
| H4 | Karty CW: ← do początku | po H3 | ← ×11 | Karta 1 na x=96, ring nieprzycięty, bez skoków | OK |
| H5 | Karty CW: szybkie przewijanie | H1 | ↓, → ×8 co ~150 ms | Hero zgodny z końcową kartą, karta widoczna, brak zawieszeń | OK |
| H6 | Pamięć karty | focus na 9. karcie | ↑, potem ↓ | ↑ → Resume; ↓ → ta sama 9. karta, widoczna | OK |
| H7 | Długi tytuł w hero | karta z długim tytułem | focus na karcie | Tytuł w 1 linii z elipsą, meta i przyciski nie przesuwają się | OK |
| H8 | Pionowa nawigacja sekcji | H1 | ↓ (karta) → ↓ (chip Source) → ↓ (Featured) → ↓ (1. plakat feedu), potem ↑ w odwrotnej kolejności | Kolejność CW → Source → Featured → feed; ↑ z chipa wraca na zapamiętaną kartę | OK |
| H9 | ↓ z top baru | focus na zakładce Home (hero widoczne) | ↓ | Od pierwszego naciśnięcia focus na „Resume” | OK |
| H10 | ↓ z top baru po Back z feedu | focus w feedzie | Back, potem ↓ | Back pokazuje top bar z focusem na zakładce; ↓ wraca na ostatnio sfocusowany element | OK |
| H11 | Granica rzędu Source | focus na ostatnim chipie | → | Focus zostaje na chipie (nie skacze do feedu) | OK |
| H12 | ↑ z feedu do Featured | focus na plakacie 1. wiersza 1. feedu (kolumna ≠ 1) | ↑ | Focus na Featured | OK |
| H13 | Featured: slajdy | focus na Featured | ←/→ | Zmiana slajdu z zawinięciem, kropki i licznik; top bar schowany, rząd Source w całości widoczny | OK |
| H14 | Feed: przypięcie i top bar | focus na plakacie feedu | ↓ między wierszami i feedami | Aktywny feed przypięty (nagłówek y≈72, siatka y≈124); top bar schowany; ↓ z 2. wiersza → następny feed w tej samej kolumnie; ↑ wraca | OK |
| H15 | Slot tytułu feedu | H14 | → po plakatach | Tytuł i „rok • typ” sfocusowanego plakatu w stałym slocie po prawej, bez skoków layoutu, elipsa przy długich tytułach; pod plakatem brak tytułu | OK |
| H16 | Kafel „Show more” | feed z >12 pozycjami | przejść na 12. pozycję | 11 plakatów + kafel z podtytułem „All of <feed>”; ring nieprzycięty | OK |
| H17 | Wybór źródła | focus na chipie Source | OK | Feedy przeładowane dla wybranego źródła | OK |
| H18 | Panel „More…” | >N źródeł (przepełniony rząd) | OK na „More…”, ↑/↓, OK, Back | Panel 680 px z kaflami inicjałów, zaznaczone źródło z checkiem; OK wybiera; Back zamyka, focus na „More…” | nie sprawdzono |
| H19 | Remove w CW | seed (dane do odtworzenia) | OK na „Remove” | Pozycja znika, focus na sąsiedniej karcie (nie w top barze) | nie sprawdzono |
| H20 | Stany Home | brak feedów / brak źródeł / brak CW / błąd | start | Zgodnie z planszą Home States: komunikat i ikona; bez CW ekran zaczyna się od Source | nie sprawdzono |

## Siatka „Show more” (Home, Biblioteka, Search)

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| S1 | Wejście z Home | H16 | OK na kaflu „Show more” | Pełny ekran bez top baru; 6 kolumn, plakat ≈261×392 px, odstęp 32; nagłówek w stylu feedu; focus na 1. plakacie; ring 1. rzędu nieprzycięty | OK |
| S2 | Slot tytułu (Home) | S1 | → po plakatach | Slot przy prawej krawędzi, stała szerokość, elipsa, bez skoków | OK |
| S3 | Paginacja | S1 | ↓ ×12 | Dociąganie kolejnych stron, focus widoczny | OK |
| S4 | ↑ bez Sort | S1 | ↑ z 1. rzędu | Focus zostaje w siatce | OK |
| S5 | Back z siatki | S1 | Back | Powrót do Home, focus na kaflu „Show more” | OK |
| S6 | Detale i powrót | S1 | OK na plakacie, Back | Ekran detali; po Back focus na tym samym plakacie, ta sama pozycja przewinięcia | OK |
| S7 | Wejście z Biblioteki | lista TMDB >12 | top bar → Library, OK; ↓, → do kafla, OK | Siatka jak S1; przycisk Sort przy prawej krawędzi (stała szerokość); slot tytułu tuż na lewo od Sort | OK |
| S8 | ↑ do Sort | S7 | ↑ z każdego z 6 plakatów 1. rzędu; ↓ | ↑ → Sort (6/6); ↓ → 1. plakat; slot znika, gdy focus na Sort, bez skoku layoutu | OK |
| S9 | Panel Sort | S7 | ↑, OK na Sort, ↓, OK | Panel 640 px na pełną wysokość ze scrimem; radio po lewej; OK wybiera, zamyka panel, focus na 1. plakacie, lista posortowana | OK |
| S10 | Najdłuższa etykieta Sort | S7 | wybrać „Alphabetical (Z to A)” | Pełna etykieta bez ucięcia (przycisk 220 dp), slot się nie przesuwa | OK |
| S11 | Back z panelu Sort | panel otwarty | Back | Panel zamknięty, focus na Sort | OK |
| S12 | Back z siatki Biblioteki | S7 | Back | Powrót do Biblioteki, focus na kaflu „Show more” | OK |
| S13 | Wejście z Search | zapytanie z >12 wynikami w źródle | OK na „Show more” w sekcji źródła | Siatka jak S1, podtytuł = zapytanie, Back → kafel | nie sprawdzono |
| S14 | Stany siatki | brak sieci / pusta lista | wejście, dociąganie strony | Szkielety kart, pusty stan, błąd z „Try again”, błąd dociągania z „Try again” | nie sprawdzono |

## Szczegóły filmu i serialu

Warunki wspólne: wejście OK na plakacie w feedzie Home lub z Featured. Nie wciskać Play/Resume ani OK na odcinku (player). Zmiany Watchlist/Favorite/„Obejrzany” cofać po teście.

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| D1 | Otwarcie szczegółów filmu | plakat filmu w feedzie | OK | Ekran szczegółów filmu, focus na Play, aplikacja responsywna | OK |
| D2 | Układ góry serialu | plakat serialu | OK | Hero (tytuł, meta, opis max 3 linie), przyciski 68 px stałej szerokości, taby, treść taba Episodes | OK (szerokość przycisków do poprawy) |
| D3 | Taby przełączają treść focusem | D2 | ↓ z akcji, →/← po tabach | Treść zmienia się od razu, tab: biały fill bez skali | OK |
| D4 | Zwijanie nagłówka | D2 | ↓ z taba | Jeden przesuw strony, nagłówek w 1 linii | OK |
| D5 | Episodes: startowy odcinek | serial | ↓ z taba Episodes | Focus na odcinku do obejrzenia | OK |
| D6 | Sezony ↔ odcinki | serial z kilkoma sezonami i Extras | ← z odcinka, ↓/↑ po sezonach, → | ← → aktualny sezon; ↑/↓ przełącza listę od razu; Extras na końcu; → wraca na zapamiętany/pierwszy nieobejrzany odcinek | OK |
| D7 | Ostatni odcinek | sezon z ~25 odcinkami | ↓ ×24 | Ostatni wiersz w całości widoczny, ring nieprzycięty | OK |
| D8 | Panel More odcinka | D7 | → ×3 (More), OK, ↓ do końca | Grupy akcji, ostatni wiersz widoczny | OK |
| D9 | Back z panelu More | D8 | Back | Focus na przycisku More tego odcinka | OK |
| D10 | Obsada do końca | serial z długą obsadą | tab Cast, → do końca | Ostatnia karta z ringiem, nieprzycięta | OK |
| D11 | More like this | serial z ≥20 rekomendacjami | tab More like this, ↓ do ostatniego rzędu | Ostatni plakat z ringiem | OK (ring przy dolnej krawędzi) |
| D12 | Powrót z innego tytułu | D11 | OK na plakacie, Back | Focus na otwartym plakacie | OK |
| D13 | Tab Details | serial | tab Details | Pełny opis i metadane, treść niefocusowalna | OK |
| D14 | Watchlist: panel statusu | serial | → na Watchlist, OK, wybór statusu | Panel 6 statusów, wybór zamyka panel, przycisk pokazuje status, szerokość stała | OK |
| D15 | Focus po panelu Watchlist | D14 | wybór statusu | Focus na Watchlist | OK |
| D16 | Download: panel mirrorów | serial/film bez pobrania | → na Download, OK | Panel z „Loading…”, licznikiem źródeł, „Skip Loading” | OK |
| D17 | Back z panelu mirrorów | D16 | Back | Focus na Download | OK |
| D18 | Back ze szczegółów | dowolne szczegóły | Back (z hero lub zwiniętych tabów) | Home, focus na tym samym plakacie/Featured | OK |
| D19 | Stany szczegółów | VPN / brak metadanych / Coming soon / ładowanie / niedostępny | wejście | Zgodnie z planszą DetailsStates: stały niefocusowalny pasek nad przyciskami, bez Play/Download dla Coming soon | nie sprawdzono |
| D20 | nextAiring | sezon w trakcie | tab Episodes | Pasek „… will be released in …” nad listą | nie sprawdzono |
| D21 | Stany pobierania | Idle/Downloading/Downloaded/Failed | przycisk Download i ikona w wierszu odcinka | Ikona i etykieta wg stanu, % postępu, stała szerokość | nie sprawdzono |
| D22 | Tytuł z błędem ładowania | film → More like this → tytuł bez danych | OK na plakacie, Back | Ekran błędu (komunikat + Try Again / Go Back); Back wraca na ten plakat | OK (komunikat do ujednolicenia) |
| D23 | Film: obsada do końca | film z długą obsadą | tab Cast, → do końca | Ostatnia karta z ringiem, nieprzycięta | OK |
| D24 | Serial: „Obejrzany” w wierszu | dowolny odcinek | → na przycisk check, OK, OK | Badge i „Watched” w meta; drugie OK cofa | OK |
| D25 | Back ze zwiniętego filmu | film, zwinięty tab Details | Back | Home, focus na tym samym plakacie | OK |
| D26 | Stała szerokość przycisków hero | film i serial | zmiana statusu Watchlist, stany Download | Każdy przycisk ma szerokość najdłuższej etykiety swoich stanów; zmiana stanu nie zmienia szerokości ani położenia sąsiadów | OK (Play 138, Watchlist 245, Favorite 182, Download 303, Search 169, More 148 px) |

## Wyszukiwanie

Warunki wspólne: zakładka Search w top barze, tekst wpisywany `adb shell input text <zapytanie>` (spacja jako `%s`), Enter uruchamia wyszukiwanie. Błąd źródła wywoływany spowolnieniem sieci emulatora (`adb emu network speed gsm`, `adb emu network delay gprs`; potem `speed full`, `delay none`).

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| W1 | Stan startowy | Home | ↑ do top baru, → do Search | Focus na polu (biały fill), tekst startowy pośrodku zgodnie z makietą | OK |
| W2 | Klawiatura i wyszukanie | pole sfocusowane | OK, wpisanie, Enter | Klawiatura systemowa (z sugestiami), wyszukiwanie uruchomione | OK |
| W3 | Stan w toku | zapytanie | Enter, zrzut od razu | „Searching N sources…”, szkielety per źródło, pusty slot tytułu | OK (zerowanie slotu sprawdzone tylko w kodzie) |
| W4 | Wyniki per źródło | zapytanie z wynikami w ≥2 źródłach | Enter | Sekcja na źródło z nagłówkiem i liczbą wyników, plakaty 261×392 px | OK |
| W5 | Ostatni plakat rzędu | ≤12 wyników | → do końca | Ring nieprzycięty, tytuł w slocie nagłówka | OK |
| W6 | Kafel „Show more” | >12 wyników | → ×11 | Kafel na końcu rzędu, ring nieprzycięty | OK |
| W7 | ↑ do pola i ↓ z powrotem | rząd przewinięty | ↑, ↓ | ↑ → pole; ↓ → pierwszy plakat | OK |
| W8 | Między sekcjami | ≥2 sekcje z wynikami | ↓, ↑ | Ta sama kolumna, lista przewija sekcje | OK |
| W9 | Detale i powrót | wyniki | OK na plakacie, Back | Powrót na ten sam plakat | OK |
| W10 | Back z wyników | po W9 | Back ×3 | Pole → top bar (Search) → Home | OK |
| W11 | Siatka „Show more” | >12 wyników | OK na kaflu, Back | Siatka 6 kolumn; Back na kafel | OK |
| W12 | Brak wyników | zapytanie bez wyników | Enter | „No items in this list” + komunikat z zapytaniem | OK |
| W13 | Błąd jednego źródła | wolna sieć | Enter, ~25 s | Wiersz błędu przy jednym źródle, plakaty przy drugim, bez dużej pustki pod błędem | OK |
| W14 | Nawigacja przez wiersz błędu | po W13 | ↑, ↑, ↓ | Wiersz błędu jest focusowalny (biały fill, OK bez akcji); ↑/↓ przechodzą sekcja po sekcji bez pomijania, ↓ z pola trafia na pierwszą sekcję, także gdy to błąd | OK |
| W15 | Back zamykający klawiaturę | pole sfocusowane | OK, Back, ↓ | Focus na polu, D-pad działa | OK |
| W16 | Focus po pustym wyszukiwaniu | jak W12 | Enter | Focus zostaje na polu | OK |

## Biblioteka

Warunki wspólne: zakładka Library, zalogowane konto TMDB z kilkoma listami (jedna >12 pozycji) oraz konto Local.

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| L1 | Wejście w Bibliotekę | TMDB + Local | top bar → Library, OK | „LISTS FROM” z chipami kont; pierwsza lista: 11 plakatów + „Show more” | OK |
| L2 | Focus i slot tytułu | L1 | ↓ na chip, ↓ na plakat, → | Ring + skala 1.05; w slocie nagłówka „tytuł rok • typ/★” | OK |
| L3 | Między listami | ≥4 listy | ↓/↑ z plakatu w kolumnie 3 | Lista przypięta do góry, ta sama kolumna (lub ostatnia dostępna) | OK |
| L4 | Ostatnia lista | L3 | ↓ do ostatniej listy | Bez pułapki focusu; lista przypięta jak pozostałe | OK |
| L5 | Chipy i top bar | L1 | ↑ z listy → chip → top bar, ↓ | Focus: chip → zakładka Library → chip | OK |
| L6 | Przełączenie konta | TMDB aktywne | ↓ na chip Local, OK | Od razu szkielety, potem listy albo „This list is empty.” | OK |
| L7 | Powrót do konta | po L6 | chip TMDB, OK | Szkielety, potem listy TMDB | OK |
| L8 | Ostatni plakat i „Show more” | lista >12 pozycji | → do kafla | Ring kafla nieprzycięty, podpis „N more” | OK |
| L9 | Siatka z sortowaniem | L8 | OK, ↑ do Sort, OK, wybór, Back, Back | Siatka, panel „Sort by” z metodami listy; Back wraca na kafel | OK |
| L10 | Detale i powrót | L2 | OK na plakacie, Back | Powrót na ten sam plakat | OK |
| L11 | Stany „brak kont” i „błąd” | brak kont / błąd API | wejście w Library; ↓ z top baru | Komunikat; przy braku kont focus na „Accounts and Security” | częściowo (brak kont: focus OK na danych testowych; błąd tylko w kodzie) |

## Pobrane

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| P1 | Pusty stan | 0 pobrań | top bar → Downloads | „No downloads yet” + podpis + ikona, bez backdropu | OK |
| P2 | Focus na pustym ekranie | P1 | ↓ z top baru | Focus zostaje na top barze | OK |
| P3 | Sekcje i wiersze | pobrania w toku, wstrzymane, z błędem i ukończone | wejście | „DOWNLOADING • N” / „DOWNLOADED • N”; wiersz 150 px z miniaturą, metą, paskiem/statusem; tło = przyciemniony backdrop sfocusowanej pozycji | nie sprawdzono (tylko dane testowe) |
| P4 | Akcje w wierszu | P3 | → od razu po wejściu na wiersz, ←, → | Focus na Play (pobrane) lub Delete (w toku/błąd); ← wraca na wiersz; szerokość wiersza bez zmian | OK na danych testowych, do potwierdzenia na prawdziwych pobraniach |
| P5 | Dialog usuwania | P4 | OK na Delete, Back | Dialog z focusem na Cancel; Back zamyka, focus na Delete | OK na danych testowych, do potwierdzenia na prawdziwych pobraniach |
| P6 | OK na wierszu | P3 | OK | Szczegóły tytułu | nie sprawdzono |

## Ustawienia

Warunki wspólne: zakładka Settings. Zmienione wartości cofać po teście. Nie zmieniać „App layout” ani języka aplikacji, nie wylogowywać kont, nie usuwać pluginów ani repozytoriów (dialog otworzyć i anulować), nie wpisywać danych logowania.

| ID | Scenariusz | Warunki | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|---|
| U1 | Poziom 1: karty i podgląd | focus na General | zrzut | Karta aktywna 1040 px, podgląd 648 px, wiersz 96 px, podgląd następnego poziomu | OK |
| U2 | Podgląd po zmianie focusu | poziom 1 | ↓ na Player | Podgląd Player (debounce ~120 ms) | OK |
| U3 | Wejście na poziom 2 | Player | → | Karta aktywna na pełną wysokość, top bar ukryty; pusty box podglądu dla pozycji bez podpoziomu | OK |
| U4 | Radio | Player › Preferred video player | →, ↓, OK, ←/Back | OK zapisuje i zostaje na poziomie; po Back podtytuł aktualny | OK |
| U5 | Multi-select | Player › Show player information | →, ↓, OK, Back | Checkboxy, OK przełącza, Back na pozycję wejścia | OK |
| U6 | Powrót z poziomu 2 | po U4/U5 | Back | Poziom 1, focus na Player | OK |
| U7 | Slider | General › Parallel downloads | →/← kilka razy, Back | ←/→ zmieniają wartość o krok (← nie wychodzi), Back wraca | OK |
| U8 | Język: zaznaczenie i focus | General › App Language | → | Bieżący język zaznaczony, focus startowy na nim, angielska nazwa po prawej | OK |
| U9 | Ekran tekstowy | General › Disclaimer | →, ↓ ↓, Back | Przewijanie ↑/↓, Back na pozycję wejścia | OK |
| U10 | Lista kont | Accounts and Security | → | Wiersze kont z kafelkami, podgląd konta | OK |
| U11 | Poziom konta | konto TMDB | → | Active account (radio), Manage | OK |
| U12 | Logowanie QR | TMDB › Add account | OK, Back | QR, URL, czas ważności; Back zamyka tryb logowania, focus na „Add account” | OK |
| U13 | Formularz logowania | Accounts › Kitsu | →, Back ×2 | Poziom providera; pola jako wiersze, „Log in” nieaktywny bez danych | OK |
| U14 | Poziom providera | Accounts › AniList / Kitsu | → | Log in / Create account | OK |
| U15 | Dodaj repozytorium | Extensions › Add repository | →, Back | Pola jako wiersze, komunikat przy pustym URL, Back | OK |
| U16 | Lista pluginów | Extensions › repozytorium | →, ↓ do końca | Kafelek pluginu, podgląd z akcjami, bez ucinania ostatniego wiersza | OK |
| U17 | Plugin pobrany / niepobrany | plugin | → | Nagłówek, opis, autorzy, akcje Settings/Delete lub Download | OK |
| U18 | Dialog usuwania | plugin › Delete | OK, Back | Dialog 800 px (400 dp), focus na Cancel, Back zamyka, nic nie usunięte | OK |
| U19 | Powrót przez 3 poziomy | plugin | Back ×3 | Focus na pozycji wejścia na każdym poziomie | OK |
| U20 | Back z poziomu 1 | poziom 1 | Back, Back | Pierwszy Back → tab Settings w top barze, drugi → Home | OK |
| U21 | ↑ z pierwszego wiersza | poziom 1 | ↑ | Focus na tabie Settings | OK |
| U22 | Stany poziomu | Loading / Error / Pusto / akcja w toku | wejście | Szkielet wierszy / komunikat + Try Again / komunikat / spinner | nie sprawdzono (tylko kod) |

## Player (urządzenie: Chromecast; nie uruchamiać na emulatorze)

Środowisko: odcinek serialu, źródło które gra (np. „Vidy [Miami]”; źródła 4KHDHub/HubCloud zwracają 403/429). Po każdej akcji czekać 1 s → 2 s → 4 s.

| ID | Scenariusz | Kroki | Oczekiwany wynik | Status |
|---|---|---|---|---|
| PL1 | Rząd kontrolek | start odtwarzania | Pigułki 60 dp, kolejność wg makiety, focus startowy Play, oś czasu z czasami | OK |
| PL2 | Auto-ukrycie | bezczynność ~4 s | Kontrolki znikają; ↑/↓/OK pokazują je z focusem na Play; ←/→ tylko przewijają | OK |
| PL3 | Back przy widocznych kontrolkach | Back | Kontrolki znikają (także na pauzie); ponowne pokazanie → focus na Play | OK |
| PL4 | OK na ukrytych kontrolkach | OK | Pokazuje kontrolki i pauzuje | OK |
| PL5 | ↑ z kontrolki | ↑ | Focus na osi czasu; ←/→ przewijają, ↓ wraca na kontrolkę | OK |
| PL6 | Powrót z panelu | otwórz Sources/Napisy/Sync, Back lub wybór | Focus wraca na kontrolkę, z której otwarto panel | OK |
| PL7 | Play/Pause | OK na Play | Stała szerokość pigułki, reszta się nie przesuwa | OK |
| PL8 | Panel Sources | otwórz | Wiersze 96 px, ellipsis, wiersze błędów (403/429), radio, Back zamyka | OK |
| PL9 | Panel Audio | otwórz | Radio, 680 px | OK (Audio zależy od źródła) |
| PL10 | Napisy | wybierz napis z listy | Napis zostaje aktywny i widoczny, bez „Brak napisów” | OK |
| PL11 | Kodowanie | Napisy › Encoding | Lista z radio, wybór, Back | OK |
| PL12 | Napisy online | Napisy › Z Internetu | Szukaj, wybór języka, logowanie źródeł | częściowo (stany szukanie/wyniki/błąd niewywołane) |
| PL13 | Sync | pigułka Sync (po aktywacji napisów) | Panel 1180 px, lista z czasami, OK na linii dopasowuje offset, ±100 ms/±1 s, reset, Custom, Back → focus na Sync | OK (pusta lista, gdy otwarte na pauzie zaraz po wyborze napisu) |
| PL14 | Up next | końcówka odcinka | Karta z odliczaniem, Play now, Cancel; przejście do następnego odcinka | OK (S1:E4→E5), nakładanie na kontrolki niesprawdzone |
| PL15 | Ładowanie | start | „Loading sources… Sources found: N”, Skip Loading | OK |
| PL16 | Brak linków | źródło bez linków | „produkcja • S:E • źródło”, Try Again | niewywoływalne na urządzeniu |
| PL17 | Zmiana źródła a napis | zmień źródło | Ręcznie wybrany napis wraca do automatycznego (znane zachowanie) | znane |

### Player: zmiany makiet (2026-10-26) i zaakceptowane odchylenia

| ID | Scenariusz | Oczekiwany wynik | Status |
|---|---|---|---|
| PL18 | Blok tytułu | U góry po lewej (96/72 px): tytuł 52, odcinek 26 (serial), źródło • jakość 22; widoczny z kontrolkami, znika z nimi; brak bloku pod paskiem czasu | OK (serial); film niesprawdzony |
| PL19 | Ładowanie | Ten sam blok tytułu, status w slocie paska czasu, „Skip Loading” 60 px w focusie, backdrop 60% | OK (pierwsze ~3 s czarny ekran i brak focusu: wolne urządzenie/obraz z API, zaakceptowane) |
| PL20 | Stos komunikatów | Prawy górny róg, 560 px, max 3, 5 s, bez focusa, widoczny też przy ukrytych kontrolkach | częściowo (wygląd OK; 5 s i ukryte kontrolki niezmierzone) |
| PL21 | Czas przewijania | „+1 min 0 s”, „25 min 20 s” zamiast samych sekund | OK |
| PL22 | Szczegóły: ↓ z hero | Od razu taby na górze i pełna strona taba, bez drugiego ↓ | OK |
| PL23 | Szczegóły: szerokość przycisków | Stabilna bez akcji użytkownika; po akcji może się zmienić | OK (zmiana po akcji tylko w kodzie) |

Zaakceptowane odchylenia (użytkownik, 2026-10-26): wielkie litery w linii odcinka i surowa nazwa źródła w linii szczegółów wynikają z danych API; opóźnienie ekranu ładowania i brak backdropu wynikają z ładowania obrazka z API; problem z automatycznym ładowaniem napisów („Polish 1”) odłożony do ręcznego sprawdzenia; „Try Again”/„Wstecz” w oknie „Błąd źródła” (język), ikony notice bez koła 36 px (drobne).
