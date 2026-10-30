# NutriTrack Android 🥗📱
### Natywna aplikacja do śledzenia kalorii i makroskładników (Jetpack Compose + Room + Material 3)

NutriTrack Android to nowoczesna, w 100% natywna aplikacja na system Android napisana w języku **Kotlin**, wykorzystująca deklaratywny interfejs **Jetpack Compose** oraz wytyczne **Material 3**. Jest bezpośrednim odpowiednikiem wersji iOS, posiadając pełną integrację z bazą polskich produktów spożywczych, natywny skaner kodów kreskowych (CameraX + Google ML Kit) oraz multimodalną analizę posiłków AI Google Gemini.

---

## ✨ Kluczowe funkcje

1. **Baza produktów i Open Food Facts Global API:**
   - Przeszukiwanie pełnej bazy **Open Food Facts (ponad 3 miliony produktów)** z polską lokalizacją (`lc=pl`).
   - Elastyczny parser: obsługa przecinków dziesiętnych (np. `12,5g`), automatyczne przeliczanie z kJ na kcal (`kJ / 4.184`), oraz priorytetyzacja polskich nazw produktów (`product_name_pl` → `product_name` → `generic_name_pl`).
   - Wbudowany **lokalny katalog polskich produktów spożywczych (`PolishFoodCatalog.kt`)** zawierający 85 starannie wyselekcjonowanych artykułów (Piątnica, Mlekovita, Fruvita / Biedronka, Pilos / Lidl, Tarczyński, Kraina Wędlin, Sokołów, Lubella, Kupiec, Sonko, KFD, Olimp, Alpro, Wedel itp.) z rzeczywistymi makroskładnikami i zweryfikowanymi kodami EAN-13.
   - Filtrowanie według kategorii w wyszukiwarce: *Wszystkie, Nabiał, Mięso i Ryby, Pieczywo i Zboża, Owoce i Warzywa, Fit i Białko, Napoje, Przekąski i Słodycze*.

2. **Natywny Skaner Kodów Kreskowych (CameraX + Google ML Kit):**
   - Błyskawiczne wykrywanie kodów EAN-13, EAN-8, UPC-A, UPC-E i Code 128.
   - Natychmiastowe sprawdzanie w lokalnym katalogu (zero opóźnienia sieciowego) z automatycznym fallbackiem do API Open Food Facts v2 i v0.
   - **Inteligentna obsługa nieznanych kodów**: jeśli kod nie występuje w bazie, aplikacja umożliwia dodanie własnego produktu z automatycznie uzupełnionym kodem EAN i trwałym zapisem w bazie Room.
   - Przyciski szybkiego testu z realnymi kodami polskich produktów do natychmiastowego testowania w emulatorze bez fizycznych opakowań.

3. **Nowoczesny UI (Material 3 & Design System):**
   - Kolorystyka tożsama z wersją iOS:
     - 🟠 **Kalorie:** Flame Coral (`#FF6B4A`)
     - 🔵 **Białko:** Electric Indigo (`#4785FF`)
     - 🟢 **Węglowodany:** Fresh Mint / Emerald (`#30D158`)
     - 🟡 **Tłuszcze:** Warm Golden Amber (`#FFA626`)
   - Animowany pierścień budżetu kalorycznego z informacją o pozostałych / przekroczonych kaloriach.
   - Animowane paski postępu makroskładników ze wskaźnikami gramatury.
   - Pełne wsparcie dla motywu jasnego i ciemnego (Light / Dark Mode).

4. **Interaktywny pasek kalendarza:**
   - Poziomo przewijana lista dni ze snapem do wybranego dnia.
   - Etykiety względne w języku polskim ("Dzisiaj", "Wczoraj", "Jutro").
   - Przycisk szybkiego powrotu do "Dzisiaj" oraz pełny dialog wyboru daty.

5. **Estymacja AI ze zdjęć i Czat Dietetyczny (Google Gemini API):**
   - Multimodalna analiza zdjęć posiłków oraz opisu tekstowego za pomocą modeli Google Gemini (`gemini-3.8-flash`, `gemini-2.5-flash`, `gemini-2.0-flash`).
   - Ekran weryfikacji składników umożliwiający edycję gramatury z automatycznym przeliczaniem kalorii i makro.
   - Czat z asystentem żywieniowym Gemini z gotowymi propozycjami posiłków i 1-kliknięciowym dodawaniem dań do dziennika.
   - **Tryb Demo**: możliwość pełnego testowania działania estymacji nawet bez podawania klucza API.
   - Możliwość wprowadzenia własnego bezpłatnego klucza z Google AI Studio w Ustawieniach.

---

## 🏛️ Architektura i Stack Technologiczny

- **Język:** Kotlin 2.0 (JVM 17)
- **UI:** Jetpack Compose + Material 3 + Compose Navigation
- **Baza danych:** Room ORM (`@Database`, `@Entity`, `@Dao`, `Flow`) z kaskadowym usuwaniem
- **Kamera i Wizja:** CameraX (`camera2`, `camera-lifecycle`, `camera-view`) + Google ML Kit Barcode Scanning
- **Komunikacja sieciowa:** OkHttp 4 + Gson + Coroutines Dispatchers.IO
- **Ładowanie grafik:** Coil Compose
- **Architektura:** MVVM (Model-View-ViewModel) + Repository Pattern + Reactive UDF (Unidirectional Data Flow)

---

## 🚀 Jak uruchomić projekt

1. Otwórz folder `NutriTrack-Android` w programie **Android Studio** (Koala / Ladybug lub nowszy).
2. Poczekaj na automatyczną synchronizację projektu Gradle (`Sync Project with Gradle Files`).
3. Wybierz podłączone urządzenie fizyczne z systemem Android (min. Android 8.0, API 26) lub emulator.
4. Kliknij **Run** (zielony trójkąt / `Shift + F10`).
