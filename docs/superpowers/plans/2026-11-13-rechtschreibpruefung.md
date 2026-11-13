# Rechtschreibprüfung Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Offline-Desktop-App (Windows), die deutschen Text auf Rechtschreibung/Grammatik prüft, Fehler unterschlängelt und Korrekturen einzeln oder gesammelt übernimmt – ausgeliefert mit eingebauter Java-Laufzeit.

**Architecture:** Drei Pakete: `model` (reine Text-/Positionslogik, voll getestet), `engine` (LanguageTool-Hülle, liefert `Issue`s), `ui` (Compose Desktop). Prüfungen laufen auf einem einzelnen Hintergrund-Thread; Ergebnisse zu veraltetem Text werden verworfen, Positionen bei Textänderung verschoben.

**Tech Stack:** Kotlin 2.1.21, Compose Multiplatform 1.8.2 (Material 3), LanguageTool `language-de` 6.6, Gradle 8.14.3 (Wrapper), JDK 21 (Temurin), JUnit 5 via `kotlin("test")`.

**Spec:** `docs/superpowers/specs/2026-11-13-rechtschreibpruefung-design.md`

## Global Constraints

- Keine Netzwerkzugriffe zur Laufzeit; alles im Paket.
- Paket: `createDistributable` (Ordner + `Rechtschreibung.exe` + Laufzeit) und `packageMsi`; Zielrechner braucht kein Java.
- Paketname `Rechtschreibung`, Version `1.0.0`, Kotlin-Paket `de.rechtschreibung`.
- Sprachvarianten: `de-DE` (Deutschland, Standard), `de-AT` (Österreich), `de-CH` (Schweiz).
- Farben je Kategorie: Rechtschreibung rot, Grammatik blau, Zeichensetzung orange, Stil grau.
- Auto-Prüfung 800 ms nach letzter Eingabe.
- Alle UI-Texte auf Deutsch.
- Kein Git-Repo vorhanden → keine Commit-Schritte (Nutzer hat kein Git verlangt).

## Review Focus

1. **Emojis/Sonderzeichen vor einem Fehler** – Positionen müssen stimmen (LanguageTool und Compose zählen beide UTF-16-Einheiten) → Test in Task 3.
2. **Tippen während einer laufenden Prüfung** – altes Ergebnis darf keine falschen Markierungen setzen → `shiftIssues`-Tests in Task 2, Verwerfen veralteter Ergebnisse in Task 4.
3. **Überlappende Hinweise bei „Alle übernehmen“** – kein kaputter Text, Überlappende werden übersprungen → Test in Task 2.
4. **Eingefügter Text mit Windows-Zeilenenden `\r\n`** – wird zu `\n` normalisiert, damit Positionen passen → Test in Task 2.
5. **Leerer Text / sehr langer Text** – kein Absturz, UI friert nicht ein (Prüfung im Hintergrund) → Test leerer Text in Task 3; Hintergrund-Thread in Task 4.

---

### Task 1: Projektgerüst, Build und leeres Fenster

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, Gradle-Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
- Create: `src/main/kotlin/de/rechtschreibung/Main.kt`
- Create: `.gitignore`

- [ ] **Step 1:** Wrapper mit Gradle 8.14.3 erzeugen (Gradle-Distribution einmalig in Scratchpad entpacken, `gradle wrapper --gradle-version 8.14.3`).
- [ ] **Step 2:** `build.gradle.kts` mit Plugins `kotlin("jvm")`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.compose`; Abhängigkeiten `compose.desktop.currentOs`, `compose.material3`, `compose.materialIconsExtended`, `org.languagetool:language-de:6.6`, `kotlinx-coroutines-swing`; `kotlin { jvmToolchain(21) }`; `compose.desktop.application` mit `mainClass = "de.rechtschreibung.MainKt"`, `nativeDistributions { targetFormats(Msi); packageName = "Rechtschreibung"; packageVersion = "1.0.0"; includeAllModules = true; windows { menu = true; shortcut = true; dirChooser = true; upgradeUuid = <feste UUID> } }`.
- [ ] **Step 3:** `Main.kt` mit `application { Window(title = "Rechtschreibung") { Text("…") } }`.
- [ ] **Step 4:** Run `gradlew.bat compileKotlin` → BUILD SUCCESSFUL.

### Task 2: Model – Issues, Korrekturen anwenden, Positionen verschieben

**Files:**
- Create: `src/main/kotlin/de/rechtschreibung/model/Issue.kt`
- Create: `src/main/kotlin/de/rechtschreibung/model/TextOps.kt`
- Test: `src/test/kotlin/de/rechtschreibung/model/TextOpsTest.kt`

**Interfaces:**
- Produces:
  - `enum class Category { SPELLING, GRAMMAR, PUNCTUATION, STYLE }` mit `val label: String` („Rechtschreibung“, „Grammatik“, „Zeichensetzung“, „Stil“)
  - `data class Issue(val offset: Int, val length: Int, val message: String, val shortMessage: String, val category: Category, val suggestions: List<String>, val ruleId: String) { val end: Int get() = offset + length }`
  - `data class ApplyResult(val text: String, val issues: List<Issue>)`
  - `fun applySuggestion(text: String, issues: List<Issue>, issue: Issue, replacement: String): ApplyResult` – ersetzt, entfernt `issue`, verschiebt Folge-Issues um Längendifferenz, entfernt mit dem Bereich überlappende.
  - `fun applyAll(text: String, issues: List<Issue>): ApplyResult` – nimmt je Issue den ersten Vorschlag, arbeitet von hinten nach vorn, überspringt Issues ohne Vorschlag (bleiben erhalten) und solche, die eine bereits ersetzte Stelle überlappen.
  - `fun shiftIssues(issues: List<Issue>, oldText: String, newText: String): List<Issue>` – bestimmt geänderten Bereich über gemeinsamen Präfix/Suffix; Issues davor bleiben, danach werden verschoben, überlappende fallen weg.
  - `fun normalizeLineEndings(s: String): String` – `\r\n` und `\r` → `\n`.
  - `fun ignoreKey(text: String, issue: Issue): String` = `"${issue.ruleId}|${text.substring(issue.offset, issue.end)}"`

- [ ] **Step 1: Failing tests** in `TextOpsTest`:
  - `applySuggestion_replacesAndShiftsFollowing`: Text `"Wir warten seid drei Tagen, das ist gut."`, Issues bei „seid“ (11,4) und „das“ (28,3); nach Ersetzen „seid“→„seit“ ist Text `"Wir warten seit drei Tagen, das ist gut."` und zweites Issue hat Offset 28.
  - `applySuggestion_longerReplacementShifts`: „seid“→„seitdem“ verschiebt Folge-Issue um +3.
  - `applyAll_appliesBackToFront`: zwei Issues mit Vorschlägen → beide ersetzt, Ergebnis-Issues leer.
  - `applyAll_keepsIssuesWithoutSuggestions`: Issue ohne Vorschlag bleibt (mit korrekt verschobenem Offset).
  - `applyAll_skipsOverlapping`: Issues (0,5) und (3,4), beide mit Vorschlag → nur eines angewendet, Text konsistent (kein Index-Fehler).
  - `shiftIssues_insertBeforeShifts`, `shiftIssues_insertAfterKeeps`, `shiftIssues_editInsideDrops`.
  - `normalizeLineEndings_convertsCrLf`: `"a\r\nb\rc"` → `"a\nb\nc"`.
- [ ] **Step 2:** Run `gradlew.bat test` → FAIL (unresolved references).
- [ ] **Step 3:** Implementieren in `Issue.kt` / `TextOps.kt`.
- [ ] **Step 4:** Run `gradlew.bat test` → PASS.

### Task 3: Engine – LanguageTool-Hülle

**Files:**
- Create: `src/main/kotlin/de/rechtschreibung/engine/GermanChecker.kt`
- Test: `src/test/kotlin/de/rechtschreibung/engine/GermanCheckerTest.kt`

**Interfaces:**
- Consumes: `Issue`, `Category` (Task 2)
- Produces:
  - `enum class Variant(val code: String, val label: String) { DE("de-DE","Deutschland"), AT("de-AT","Österreich"), CH("de-CH","Schweiz") }`
  - `class GermanChecker { fun check(text: String, variant: Variant): List<Issue>; fun warmUp(variant: Variant) }` – hält je Variante eine `JLanguageTool`-Instanz (lazy, `Languages.getLanguageForShortCode(code)`); nicht thread-sicher, Aufrufer serialisiert.
  - Kategorie-Mapping: `rule.isDictionaryBasedSpellingRule` oder Kategorie-ID in {`TYPOS`, `CASING`, `COMPOUNDING`} → SPELLING; {`PUNCTUATION`, `TYPOGRAPHY`} → PUNCTUATION; {`GRAMMAR`, `CONFUSED_WORDS`} → GRAMMAR; sonst STYLE.
  - `message`: `<suggestion>x</suggestion>` → `„x“`, übrige Tags entfernt. Vorschläge auf max. 5 begrenzt.

- [ ] **Step 1: Failing tests:**
  - `detectsSeidSeit`: `"Wir warten seid drei Tagen."` → ein Issue bei Offset 11, Länge 4, Vorschläge enthalten „seit“.
  - `detectsSpellingError`: `"Das ist ein Fehlr."` → Issue mit `Category.SPELLING` bei „Fehlr“.
  - `correctTextHasNoIssues`: `"Das ist ein korrekter Satz."` → leer.
  - `emptyTextHasNoIssues`: `""` und `"   "` → leer.
  - `offsetsCorrectAfterEmoji`: `"😀 Wir warten seid drei Tagen."` → Issue-Offset zeigt via `substring` auf „seid“.
  - `messageHasNoXmlTags`: keine Meldung enthält `<`.
- [ ] **Step 2:** Run `gradlew.bat test --tests "*GermanCheckerTest*"` → FAIL.
- [ ] **Step 3:** Implementieren.
- [ ] **Step 4:** Run → PASS.

### Task 4: Oberfläche

**Files:**
- Create: `src/main/kotlin/de/rechtschreibung/ui/Theme.kt` – hell/dunkel `ColorScheme`, Kategorie-Farben `fun Category.color(dark: Boolean): Color`
- Create: `src/main/kotlin/de/rechtschreibung/ui/AppState.kt` – Zustand + Aktionen
- Create: `src/main/kotlin/de/rechtschreibung/ui/Editor.kt` – Textfeld mit Wellenlinien
- Create: `src/main/kotlin/de/rechtschreibung/ui/IssuePanel.kt` – Kartenliste
- Create: `src/main/kotlin/de/rechtschreibung/ui/App.kt` – Layout, Toolbar, Statuszeile
- Modify: `src/main/kotlin/de/rechtschreibung/Main.kt`

**Interfaces:**
- Consumes: Task 2 + 3.
- `class AppState(scope: CoroutineScope, checker: GermanChecker)` mit Compose-State: `text: TextFieldValue`, `issues: List<Issue>`, `selected: Issue?`, `variant: Variant`, `dark: Boolean`, `status: String`, `checking: Boolean`; Aktionen `onTextChange(TextFieldValue)` (verschiebt Issues via `shiftIssues`, plant Prüfung in 800 ms), `checkNow()`, `pasteAndCheck()` (Zwischenablage via AWT, `normalizeLineEndings`), `apply(issue, replacement)`, `applyAll()`, `ignore(issue)`, `copyAll()`, `clear()`, `select(issue)`.
- Prüfungen laufen auf `Executors.newSingleThreadExecutor().asCoroutineDispatcher()`; Ergebnis wird nur übernommen, wenn der Text seit Start unverändert ist. Ignorierte (`ignoreKey`) werden herausgefiltert. Beim Start `warmUp(DE)` im Hintergrund; Status „Wörterbücher werden geladen …“.

- [ ] **Step 1:** Editor: `BasicTextField` ohne eigenes Scrollen in `verticalScroll`-Container; `onTextLayout` merkt `TextLayoutResult`; `Modifier.drawBehind` zeichnet je Issue und Zeile eine Wellenlinie (Kategorie-Farbe) unter `getLineBottom`, ausgewähltes Issue zusätzlich mit halbtransparentem Hintergrund. Cursor in einem Issue-Bereich → `select(issue)`.
- [ ] **Step 2:** IssuePanel: Karte je Issue mit farbigem Punkt + Kategorie, markiertem Originalwort, Meldung, Vorschlags-Chips (Klick = übernehmen), Knöpfen „Übernehmen“ (erster Vorschlag) und „Ignorieren“. Ausgewählte Karte hervorgehoben und in Sicht gescrollt. Leerer Zustand: „Keine Fehler gefunden ✓“.
- [ ] **Step 3:** App: Kopfzeile (Titel, Varianten-Dropdown, Hell/Dunkel-Schalter), Toolbar „Einfügen & Prüfen“, „Prüfen“, „Alle übernehmen (n)“, „Kopieren“, „Leeren“, Statuszeile (Anzahl Hinweise, Prüfdauer).
- [ ] **Step 4:** Run `gradlew.bat run`, manuell: Satz `"Wir warten seid drei Tagen auf die Antwort das ist ärgerlich."` einfügen → Markierungen + Karten erscheinen; Übernehmen/Alle übernehmen/Ignorieren funktionieren; Tippen verschiebt Markierungen korrekt.
- [ ] **Step 5:** Run `gradlew.bat test` → weiterhin PASS.

### Task 5: Paket mit eingebauter Laufzeit

**Files:**
- Modify: `build.gradle.kts` (ggf. `modules(...)` statt `includeAllModules`, nach `suggestRuntimeModules`)
- Create: `README.md` (Bauen, Ausliefern, Größe)

- [ ] **Step 1:** Run `gradlew.bat createDistributable` → `build/compose/binaries/main/app/Rechtschreibung/Rechtschreibung.exe` existiert, `runtime/` enthalten.
- [ ] **Step 2:** Smoke-Test ohne System-Java: `Rechtschreibung.exe` mit leerem `PATH`/ohne `JAVA_HOME` starten, Prozess läuft nach 10 s noch, Fenster sichtbar.
- [ ] **Step 3:** Run `gradlew.bat packageMsi` → `.msi` unter `build/compose/binaries/main/msi/`.
- [ ] **Step 4:** Größe von Ordner und MSI notieren und in `README.md` dokumentieren.
