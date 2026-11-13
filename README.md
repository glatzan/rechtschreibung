# Rechtschreibung

Offline-Rechtschreib- und Grammatikprüfung für Deutsch (Deutschland, Österreich, Schweiz).
Text einfügen, Fehler werden farbig unterschlängelt, Korrekturen einzeln oder alle auf einmal übernehmen.

- **Prüf-Engine:** [LanguageTool](https://languagetool.org) 6.6 als Bibliothek (LGPL-2.1), läuft komplett lokal
- **Oberfläche:** Kotlin + Compose Multiplatform (Material 3, hell/dunkel)
- **Auslieferung:** `.exe` mit eingebauter Java-Laufzeit – auf dem Zielrechner muss nichts installiert sein

## Ausliefern

```
gradlew.bat dist
```

Ergebnis in `dist/`:

| Datei | Größe | Verwendung |
|---|---|---|
| `dist/Rechtschreibung/` | ca. 226 MB | Portabler Ordner – kopieren, `Rechtschreibung.exe` starten |
| `dist/Rechtschreibung-1.0.0-portable.zip` | ca. 154 MB | Derselbe Ordner als ZIP zum Weitergeben |
| `dist/Rechtschreibung-1.0.0.msi` | ca. 156 MB | Installer mit Startmenü- und Desktop-Verknüpfung (benötigt Admin-Rechte) |

Eine Textdatei kann per Drag & Drop auf die `.exe` gezogen werden und wird beim Start geladen.

## Entwickeln

Voraussetzung auf dem Build-Rechner: JDK 21 (z. B. `winget install EclipseAdoptium.Temurin.21.JDK`).
Beim ersten Build werden Gradle und Bibliotheken einmalig aus dem Internet geladen.

```
gradlew.bat run      # App starten
gradlew.bat test     # Tests (Logik, LanguageTool, UI)
```

**Hinweis Umlaut im Pfad:** Liegt das Projekt in einem Ordner mit Umlaut (z. B. „Rächtschreibung“),
baut Gradle automatisch nach `%LOCALAPPDATA%\rechtschreibung-build`, weil der Gradle-Testworker unter
Windows sonst scheitert. `gradlew.bat dist` kopiert das Ergebnis trotzdem nach `dist/`.

## Aufbau

```
src/main/kotlin/de/rechtschreibung/
  model/   Issue, Korrekturen anwenden, Positionen bei Textänderung verschieben (reine Logik)
  engine/  GermanChecker – Hülle um LanguageTool, Kategorie-Zuordnung
  ui/      AppState (Zustand, Hintergrund-Prüfung), Editor (Wellenlinien), IssuePanel, App, Theme
packaging/ App-Icon (icon.ico, make-icon.ps1)
```

## Bekannte Grenzen

- Ohne die optionalen n-Gramm-Daten von LanguageTool (mehrere GB) werden manche
  Verwechslungen nicht erkannt, z. B. „Ich hoffe **das** du …“ (statt „dass“), sowie einige fehlende Kommas.
- Der erste Start lädt die Wörterbücher (ca. 10–20 s); danach dauert eine Prüfung Sekundenbruchteile.
- `Rechtschreibung.exe`, `skiko-windows-x64.dll` und das MSI sind **nicht digital signiert**
  (die Java-Laufzeit-DLLs sind von ihren Herstellern signiert). Siehe „Signieren“.

## Lizenz

Der eigene Quellcode steht unter der [MIT-Lizenz](LICENSE). Die Programmpakete enthalten zusätzlich
LanguageTool (LGPL-2.1), deutsche Wörterbücher (GPL-2/3, CC BY-SA 4.0), Compose/Kotlin (Apache 2.0)
und die Java-Laufzeit Temurin (GPL-2 mit Classpath Exception) – Details in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md), Lizenztexte in [licenses/](licenses).
Im Paket liegen sie unter `app/resources/` bzw. `runtime/legal/`.

## Signieren

Unsignierte Programme können je nach Umgebung Warnungen oder Blockaden auslösen
(SmartScreen bei Dateien aus dem Internet, Smart App Control, AppLocker/WDAC-Richtlinien).
Mit einem Code-Signing-Zertifikat (z. B. aus der Firmen-PKI) lässt sich nach dem Build signieren:

```
signtool sign /fd SHA256 /a /tr http://timestamp.digicert.com /td SHA256 dist\Rechtschreibung\Rechtschreibung.exe dist\Rechtschreibung\app\skiko-windows-x64.dll
```

Das MSI enthält eine eigene Kopie der Dateien; ein durchgängig signiertes MSI (Inhalt + Installer)
sollte als Build-Schritt eingebaut werden, sobald ein Zertifikat vorhanden ist. Ohne Internet am Build-Rechner `/tr …` (Zeitstempel) weglassen – die Signatur gilt
dann nur bis zum Ablauf des Zertifikats.
