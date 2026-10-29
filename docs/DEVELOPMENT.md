# Entwicklung und Build

[Zur Übersicht](../README.md)

## Werkzeugkette

Der Build verwendet direkt Android-SDK-Werkzeuge. Kein Gradle, kein npm, keine externen Java-Laufzeitbibliotheken.

| Werkzeug | Anforderung |
|---|---|
| Betriebssystem / Shell | Windows mit Git Bash, `find` und `cygpath` |
| JDK | 17, inklusive `javac` und `keytool` |
| Android SDK Platform | `platforms;android-35` |
| Android SDK Build-Tools | `build-tools;35.0.0` |
| Python | Python 3 unter dem Kommando `python` |
| ADB | Nur für Geräteinstallation und Tests |

Installiere SDK-Komponenten über Android Studio oder `sdkmanager`. Der Build installiert sie nicht selbst. Er kompiliert mit Platform 35, verwendet aber **minSdk 25 und targetSdk 30**. Neue APIs müssen daher auf Android 7.1 geprüft und gegebenenfalls geschützt werden.

## APK bauen

In Git Bash im Projektordner:

```bash
export ANDROID_HOME="/c/Users/DEIN_NAME/AppData/Local/Android/Sdk"
export TVHOME_JDK="/c/Program Files/Eclipse Adoptium/DEIN_JDK_17"
bash build.sh
```

Die Pfade ersetzen und bei Leerzeichen in Anführungszeichen lassen. Ohne `ANDROID_HOME` verwendet das Skript `$LOCALAPPDATA/Android/Sdk`. Ohne `TVHOME_JDK` verwendet es einen maschinenspezifischen Adoptium-Pfad aus `build.sh`. macOS und Linux werden vom aktuellen Skript nicht unterstützt: Es nutzt `cygpath`, `.bat` und `.exe`.

Der Build kompiliert Ressourcen mit `aapt2`, Java mit `javac`, erzeugt DEX mit `d8`, richtet die APK mit `zipalign` aus und signiert mit `apksigner`. Die signierte Ausgabe ist **`build/tvhome.apk`**. Der gesamte `build/`-Ordner wird vor jedem Lauf gelöscht.

## Signierung

`signing/tvhome.jks` ist für lokale Updates entscheidend. Fehlt die Datei, erzeugt das Skript einen neuen Schlüssel. Dieser kann eine bisher anders signierte Installation nicht aktualisieren. `.gitignore` schließt Schlüssel und APKs aus, ist aber kein Ersatz für ein verschlüsseltes Schlüsselbackup.

Der vorhandene Build verwendet ein im Skript hinterlegtes Standardpasswort. Für öffentliche Releases muss zuerst eine bewusst verwaltete Release-Signierung eingerichtet werden. Keine bestehenden Schlüssel stillschweigend ersetzen.

## GitHub Actions

`.github/workflows/build.yml` baut auf einem Windows-Runner mit JDK 17 und SDK 35. Die Actions sind auf Commit-SHAs festgelegt. Der Workflow benötigt keine Repository-Secrets und lädt ausschließlich die APK als kurzlebiges Build-Artefakt hoch.

**Jeder CI-Lauf erzeugt einen eigenen Wegwerf-Signaturschlüssel.** CI-APKs sind Testausgaben, keine stabil signierten Releases. Sie können vorhandene lokale Installationen und APKs aus anderen Läufen nicht zuverlässig aktualisieren. Der Workflow veröffentlicht keine GitHub Releases. Seine erste Ausführung auf GitHub steht noch aus.

## Assets erzeugen

Schriften und UI-Klänge sind bereits enthalten. Eine Neugenerierung gehört nicht zum normalen Build.

- `tools/generate_sounds.py`: Python-Standardbibliothek; `python -I tools/generate_sounds.py res/raw` ersetzt die UI-Klänge.
- `tools/make_clock_fonts.py`: benötigt `fonttools` und die Quelldateien der in `FAMILIES` und `STATIC_FAMILIES` genannten Schriftfamilien. Der Aufruf `python -I tools/make_clock_fonts.py PFAD_ZU_QUELLSCHRIFTEN` ersetzt die reduzierten Uhrenschriften. Alle zugehörigen Lizenzdateien erhalten.
- `assets/glotze/`: Glotze-Logos, Icons, Banner und Animationen. Hinweise und Befehle zum erneuten Erzeugen stehen in der [Asset-Dokumentation](../assets/glotze/README.md). Das Paket ist noch nicht in die nativen Android-App-Ressourcen integriert.

## Prüfen

Es gibt derzeit keine automatisierte Unit- oder UI-Testsuite. Ein grüner Build ersetzt keine Hardwaretests.

1. APK bauen und die im Build enthaltene Signaturprüfung abwarten.
2. Auf einem eigenen Testgerät installieren, ohne die bisherigen Nutzerdaten zu löschen.
3. Navigation, Menü, ausgeblendete Apps und beide Sortiermodi prüfen.
4. Fokus-Skalierung und Schatten an den Bildschirmrändern kontrollieren.
5. Wetter ohne Netzwerk und mit Cache prüfen.
6. Schoner, Aufwachen und Home-Umleitung getrennt prüfen.
7. Für Kompatibilitätsänderungen mindestens API 25 und API 30 testen.

Vor fernbedienten Tests sicherstellen, dass niemand den Fernseher parallel benutzt.

## Lokaler Prüfstand dieser Vorbereitung

Am 2026-10-29 wurde der unveränderte App-Code in einer separaten Projektkopie ohne den privaten Signaturschlüssel gebaut. APK-Build und die im Skript enthaltene Signaturprüfung liefen erfolgreich. Der vorhandene Build meldete dabei veraltete Android-APIs sowie wiederholt `zip: WARNING: header mismatch`. Diese Warnungen sind noch nicht untersucht; der Build ist deshalb nicht als warnungsfrei zu betrachten. Die ursprüngliche APK und der ursprüngliche Schlüssel blieben unangetastet. Es fanden keine neuen Gerätetests statt.
