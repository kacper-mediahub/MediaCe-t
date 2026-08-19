# MediaHub v1

Pierwsza wersja projektu Android dla tabletu.

Funkcje:
- lokalna biblioteka filmów/audio w pamięci aplikacji,
- import wielu plików przez systemowy picker Androida,
- lokalne odtwarzanie przez Media3/ExoPlayer,
- materiały zewnętrzne przygotowane pod tryb Online/Offline,
- Compose UI zoptymalizowane pod szerokie ekrany.

Uwaga:
- W tej wersji formularz dodawania źródła/linku oraz mapowanie aplikacji streamingowych są szkieletem do dalszej rozbudowy.
- Android/Netflix musi udostępniać odpowiedni deep link, aby można było przejść bezpośrednio do konkretnego materiału.
- Tryb offline nie omija DRM ani zabezpieczeń aplikacji streamingowych; otwiera ich aplikację.

Projekt wymaga Gradle/Android SDK do kompilacji.
