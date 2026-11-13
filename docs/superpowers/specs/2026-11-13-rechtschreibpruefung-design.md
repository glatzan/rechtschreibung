# Design: Offline-Rechtschreib- und Grammatikprüfung (Deutsch)

Stand: 2026-11-13 · Status: Entwurf, wartet auf Freigabe

## Wozu

Ein Windows-Desktop-Programm, in das man Text per Copy & Paste einfügt. Es prüft deutsche
Rechtschreibung und Grammatik, hebt Fehler im Text hervor und lässt Korrekturen einzeln
oder alle auf einmal übernehmen. Es läuft in einer Umgebung **ohne Internet**.

## Ziele

- Komplett offline, keine Netzwerkzugriffe zur Laufzeit.
- Auslieferung als Paket mit `.exe` und **eingebauter Java-Laufzeit**: Auf dem Zielrechner
  muss kein Java installiert sein.
- Moderne, ansprechende Oberfläche (hell/dunkel).
- Fehler farbig unterschlängelt, Vorschläge einzeln oder gesammelt übernehmbar.

## Nicht-Ziele

- Keine KI, keine Cloud, kein Konto.
- Keine Integration in Word/Outlook/Browser – nur eigenes Fenster mit Copy & Paste.
- Nur Windows als Zielplattform (Code bleibt aber plattformneutral).
- Keine n-Gramm-Daten von LanguageTool (mehrere GB; später optional nachrüstbar).

## Ansätze (betrachtet)

| Ansatz | Bewertung |
|---|---|
| **Kotlin + Compose Desktop + LanguageTool als Bibliothek** (gewählt) | Volle LanguageTool-Qualität, moderne deklarative Oberfläche, Packaging mit eingebauter Laufzeit direkt im Gradle-Plugin |
| Python + `language_tool_python` | Nur Hülle um Java-LanguageTool → zwei Laufzeiten, lädt standardmäßig aus dem Internet |
| C#/WPF + LanguageTool als lokaler Server | Zwei Technologien, zweiter Prozess, mehr Fehlerquellen |
| Rust + nlprule | Regelstand von 2021, keine Rechtschreibprüfung, nicht mehr gepflegt |
| Lokales KI-Modell (llama.cpp) | 4–8 GB, langsam ohne GPU, keine stabilen Fehlerpositionen |

Vom Nutzer entschieden: LanguageTool offline, Kotlin statt Java, Paket ohne externe JVM.

## Aufbau

**Technik**
- Kotlin (JVM), Gradle mit Wrapper.
- Oberfläche: Compose Multiplatform for Desktop, Material 3.
- Prüfung: `org.languagetool:language-de` (aktuelle 6.x-Version), direkt im Prozess.
- Packaging: Compose-Gradle-Plugin (nutzt `jpackage` + `jlink`):
  - `createDistributable` → Ordner mit `Rechtschreibung.exe` + eingebauter Laufzeit (zum Kopieren)
  - `packageMsi` → Windows-Installer
- Erwartete Größe: ca. 150–250 MB.

**Pakete im Code**
- `engine` – `GermanChecker`: kapselt `JLanguageTool`, liefert `List<Issue>`
  (`offset`, `length`, `message`, `kategorie` [Rechtschreibung/Grammatik/Stil/Zeichensetzung],
  `vorschläge`, `ruleId`). Wird beim Start im Hintergrund vorgewärmt (erste Prüfung ist sonst langsam).
- `model` – reine Logik: Korrektur anwenden, „Alle übernehmen“ (von hinten nach vorn, damit
  Positionen stimmen), Ignorieren, Neuberechnung der Positionen nach Textänderung.
- `ui` – Compose-Oberfläche.

**Oberfläche**

```
┌─ Rechtschreibung ──────────────────────── [Deutschland ▾] [☀/☾] ─┐
│ ┌──────────────────────────────────┐ ┌─ 3 Hinweise ───────────┐ │
│ │ Wir warten seid drei Tagen auf   │ │ ● Rechtschreibung      │ │
│ │            ~~~~                  │ │   „seid“ → [seit]      │ │
│ │ die Antwort, das ist ärgerlich.  │ │   [Übernehmen][Ignor.] │ │
│ │            ~~~~~                 │ │ ● Zeichensetzung       │ │
│ │                                  │ │   …                    │ │
│ └──────────────────────────────────┘ └────────────────────────┘ │
│ [Einfügen & Prüfen] [Prüfen] [Alle übernehmen] [Kopieren] [Leeren]│
└──────────────────────────────────────────── Bereit · 0,4 s ──────┘
```

- Links Editor, rechts Liste der Hinweise als Karten.
- Farben: Rot = Rechtschreibung, Blau/Orange = Grammatik & Zeichensetzung, Grau = Stil.
- Klick auf unterschlängeltes Wort → zugehörige Karte wird hervorgehoben (und umgekehrt).
- Bei mehreren Vorschlägen: Vorschlag direkt anklickbar.
- Automatische Prüfung kurz nach dem Tippen (ca. 800 ms Pause) plus Knopf „Prüfen“.
- Sprachvariante wählbar: Deutschland / Österreich / Schweiz.
- „Ignorieren“ gilt für die laufende Sitzung.

## Voraussetzungen

- Build-Rechner: JDK 21 (z. B. Eclipse Temurin via `winget`) – aktuell **nicht installiert**.
  Gradle-Wrapper und Bibliotheken werden beim ersten Build einmalig heruntergeladen.
- Zielrechner: nichts.

## Tests

- Unit-Tests für die Korrektur-Logik (`model`).
- Engine-Tests mit typischen Fehlersätzen („seid/seit“, „das/dass“, Kommafehler).

## Dem Entwickler überlassen

Genaue Farben/Typografie, Fenstergröße, Icon, Umsetzung der Wellenlinie in Compose,
genaue LanguageTool-Version.
