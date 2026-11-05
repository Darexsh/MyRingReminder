# Polityka prywatności – MyRingReminder

Last updated: 2026-11-05

## 1. Administrator danych
Daniel Sichler (Darexsh)  
E-mail: sichler.daniel@gmail.com

## 2. Zakres
Niniejsza polityka dotyczy aplikacji MyRingReminder na Androida (`com.darexsh.myringreminder`) rozpowszechnianej za pośrednictwem tego repozytorium oraz wydań APK.

## 3. Dane przetwarzane przez MyRingReminder
MyRingReminder przetwarza dane aplikacji niezbędne do śledzenia cyklu i personalizacji, w tym:
- daty cyklu i historię cykli
- wpisy dotyczące miesiączki (na przykład oznaczenie dnia miesiączki, intensywność krwawienia, objawy takie jak ból lub choroba oraz opcjonalne oznaczenia początku i końca)
- ustawienia przypomnień i godziny powiadomień
- notatki wprowadzone przez użytkownika
- preferencje wyglądu (kolory, tło i język)
- opcjonalne ustawienia blokady aplikacji

## 4. Lokalne przechowywanie danych
MyRingReminder przechowuje dane lokalnie na Twoim urządzeniu (na przykład w SharedPreferences lub lokalnym magazynie aplikacji).

MyRingReminder nie przesyła deweloperowi Twoich osobistych danych dotyczących cyklu ani notatek.

Jeśli korzystasz z kopii zapasowej lub przywracania, dane kopii są eksportowane lub importowane jako plik lokalny wybrany przez Ciebie za pomocą systemowego selektora plików Androida.

Jeśli eksportujesz szczegóły miesiączki do PDF, plik PDF jest tworzony lokalnie na Twoim urządzeniu i zapisywany wyłącznie w lokalizacji wybranej przez Ciebie w systemowym selektorze plików Androida.  
Gdy otwierasz ten plik PDF z powiadomienia aplikacji, Android może przekazać go wybranej przez Ciebie aplikacji do odczytu PDF. W takim przypadku ta aplikacja przetwarza plik zgodnie z własną polityką prywatności.

## 5. Używane uprawnienia
MyRingReminder może prosić o uprawnienia wymagane przez funkcje aplikacji, takie jak:
- powiadomienia i dokładne alarmy (przypomnienia dotyczące cyklu)
- dostęp do multimediów lub plików (wybór obrazu tła, kopia zapasowa i przywracanie)
- dane biometryczne lub dane uwierzytelniające urządzenia (opcjonalna blokada aplikacji)

Uprawnienia są używane wyłącznie do zapewnienia żądanych funkcji aplikacji.

## 6. Analityka, śledzenie i raportowanie awarii
Zgodnie z obecną implementacją MyRingReminder **nie** zawiera zewnętrznych pakietów SDK do analityki ani raportowania awarii.

## 7. Sieć i linki zewnętrzne
Podstawowe funkcje MyRingReminder działają lokalnie i nie wymagają dedykowanego interfejsu API backendu do śledzenia cyklu.

W celu sprawdzania aktualizacji MyRingReminder może pobierać metadane wydań z publicznego interfejsu GitHub Releases tego projektu.

Po otwarciu **Ustawienia → Ulotka dla pacjenta** aplikacja pobiera publiczny plik `leaflet-packages.json` z repozytorium GitHub tego projektu. Po otwarciu **Informacje o aplikacji → Polityka prywatności** pobiera plik polityki w wybranym języku aplikacji; jeśli tłumaczenie nie jest dostępne, używana jest wersja angielska. Żądania te nie zawierają Twoich danych cyklu, notatek ani ustawień aplikacji. GitHub może przetwarzać zwykłe informacje o połączeniu, takie jak adres IP i nagłówki żądania, zgodnie ze swoją polityką prywatności. Aplikacja przechowuje na urządzeniu ostatnią pomyślnie pobraną kopię każdego języka, aby można ją było przeczytać także wtedy, gdy kolejne żądanie się nie powiedzie.

Na Twoje żądanie MyRingReminder może otwierać zewnętrzne adresy za pomocą systemowych intentów, w tym:
- poczta e-mail (`mailto:`)
- Linktree
- Telegram
- GitHub
- Buy Me a Coffee
- ulotki udostępniane przez zewnętrzne serwisy informacji o lekach lub producentów

Po otwarciu ulotki Android przekazuje jej adres URL wybranej przez Ciebie przeglądarce lub przeglądarce dokumentów. Usługa hostująca otrzymuje żądanie; adres URL może ujawniać produkt i język ulotki. Przeglądarka, aplikacja do wyświetlania dokumentów i usługa hostująca mogą przetwarzać zwykłe informacje o połączeniu zgodnie z własnymi politykami prywatności. MyRingReminder nie dołącza do tych żądań lokalnie przechowywanych danych cyklu, notatek ani ustawień.

Po otwarciu linków zewnętrznych obowiązują polityki prywatności odpowiednich usług zewnętrznych.

### Usługi zewnętrzne wymienione w linkach
- GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement
- Telegram: https://telegram.org/privacy
- Linktree: https://linktr.ee/s/privacy
- Buy Me a Coffee: https://www.buymeacoffee.com/privacy-policy

## 8. Udostępnianie danych
Zgodnie z obecną implementacją aplikacji MyRingReminder nie przekazuje deweloperowi lokalnie przechowywanych danych cyklu, notatek ani ustawień.

## 9. Informacja medyczna
MyRingReminder służy wyłącznie do organizacji i śledzenia.

Nie jest wyrobem medycznym, nie diagnozuje i nie zastępuje profesjonalnej porady medycznej.

W razie pytań medycznych lub problemów zdrowotnych skonsultuj się z wykwalifikowanym pracownikiem ochrony zdrowia.

## 10. Twoje możliwości
- Możesz edytować lub usuwać dane w aplikacji.
- Możesz cofnąć uprawnienia aplikacji w ustawieniach Androida.
- Możesz nie otwierać linków zewnętrznych.
- Możesz odinstalować aplikację i usunąć dane lokalne w ustawieniach Androida.

## 11. Zmiany w tej polityce
Polityka może być aktualizowana w przypadku zmian funkcji aplikacji lub wymogów prawnych.
