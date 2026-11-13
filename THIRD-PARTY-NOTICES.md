# Drittanbieter-Komponenten / Third-Party Notices

Der eigene Quellcode dieses Projekts steht unter der MIT-Lizenz (siehe `LICENSE`).
Die ausgelieferten Programmpakete (`.zip`, `.msi`) enthalten zusätzlich die folgenden
Komponenten Dritter unter deren eigenen Lizenzen. Die Lizenztexte liegen im Ordner `licenses/`.

## LanguageTool 6.6

- Komponenten: `languagetool-core`, `language-de` und deren Abhängigkeiten
- Lizenz: GNU Lesser General Public License 2.1 (`licenses/LGPL-2.1.txt`)
- Quellcode: https://github.com/languagetool-org/languagetool (Tag `v6.6`)
- LanguageTool wird als unveränderte, eigenständige JAR-Datei im Ordner `app/` mitgeliefert und
  kann dort durch eine andere kompatible Version ersetzt werden.

## Deutsches Rechtschreibwörterbuch (in `language-de` enthalten)

- Grundlage: igerman98 von Björn Jacke (https://www.j3e.de/ispell/igerman98/),
  Erweiterung „frami“ von Franz Michael Baumann
- Lizenz: GNU General Public License Version 2 oder 3 (`licenses/GPL-2.txt`, `licenses/GPL-3.txt`)
- Ergänzt um die Wortliste von Jan Schreiber (https://sourceforge.net/projects/germandict/, Public Domain)

## German part-of-speech dictionary 1.2.4

- Daten auf Basis von Morphy (http://morphy.wolfganglezius.de) mit Erweiterungen und Korrekturen
  von Julian von Heyl (https://www.korrekturen.de/flexion/), zusammengestellt von Daniel Naber
- Lizenz: Creative Commons Attribution-ShareAlike 4.0 International
  (https://creativecommons.org/licenses/by-sa/4.0/legalcode)
- Quelle: https://github.com/languagetool-org/german-pos-dict

## Apache License 2.0 (`licenses/Apache-2.0.txt`)

- Kotlin Standardbibliothek und kotlinx.coroutines – JetBrains s.r.o.
- Compose Multiplatform, Skiko – JetBrains s.r.o.
- Jetpack Compose, Material 3, Material Icons – The Android Open Source Project / Google LLC
- hunspell-java (`com.gitlab.dumonts:hunspell`) – https://gitlab.com/dumonts/hunspell-java

## BSD-Lizenz

- Morfologik (`org.carrot2:morfologik-*`) – Dawid Weiss, Marcin Miłkowski,
  https://github.com/morfologik/morfologik-stack

Weitere transitive Bibliotheken von LanguageTool (z. B. Guava, Jackson, Apache Commons, Lucene)
stehen überwiegend unter Apache 2.0, BSD oder MIT; ihre Lizenzangaben befinden sich jeweils in den
JAR-Dateien bzw. deren Maven-POMs.

## Java-Laufzeit (Eclipse Temurin 21)

- Lizenz: GNU General Public License 2 mit Classpath Exception
- Die vollständigen Lizenzhinweise liegen im ausgelieferten Paket unter `runtime/legal/`.
- Quelle: https://adoptium.net
