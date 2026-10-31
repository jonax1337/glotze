<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/glotze/svg/banners/glotze-readme-dark.svg">
  <img src="assets/glotze/svg/banners/glotze-readme-light.svg" alt="Glotze – Dein Fernseher. Dein Zuhause." width="100%">
</picture>

Ein eigener Startbildschirm für Fire TV, Google TV und Android TV mit Bühne, Wetter und gestaltbarem Bildschirmschoner.

[Installation](docs/INSTALLATION.md) · [Selbst bauen](docs/DEVELOPMENT.md) · [Datenschutz](docs/PRIVACY.md)

</div>

> **In Entwicklung · v0.6 · Android 7.1+ · Java ohne Gradle**  
> Das Projekt und die App heißen **Glotze**. Paketkennung: `local.tvhome`.

## Ein Startbildschirm, der dir gehört

Apps direkt öffnen, den Abend mit Wetter und Uhr beginnen und den Bildschirm in einen persönlichen Schoner verwandeln. Die Oberfläche ist für Fernbedienungen und ältere Fire-TV-Hardware ausgelegt.

| Starten | Anpassen | Zur Ruhe kommen |
|---|---|---|
| Bühne als Startseite mit Uhr, Wetter und Abfahrten | Kacheln verschieben, ausblenden und anheften | Bildschirmschoner mit acht Uhrenschriften und animierten Hintergründen |
| Dock aus deinen Apps mit der Kachel „Alle Apps“ | Raster mit Kategorien, feste oder nutzungsbasierte Reihenfolge | Widgets für Wetter, Mond, Medien, Abfahrten, Kalender und Glotze |
| Apps lassen sich per PIN sperren | Orte wählen: Haltestellen und Weltuhr | Nachtmodus mit einstellbarem Zeitplan oder nach Sonnenstand |
| Sicherung und Wiederherstellung der Einstellungen | Geräteeinstellungen öffnen, passend zum erkannten System | |

Das Projekt läuft ohne eigenes Benutzerkonto. Wetter und Abfahrten verwenden externe Dienste. Einige Funktionen brauchen zusätzliche Berechtigungen; die Grundfunktion als Launcher-App nicht.

## Läuft das auf meinem Fernseher?

**APK installieren** und **Home-Taste ersetzen** sind zwei getrennte Schritte.

| Gerät / System | Launcher | Home-Taste |
|---|---|---|
| Fire TV Stick 4K, 1. Gen. / AFTMM / Fire OS 6.7.1.1 | Auf diesem Projektgerät getestet | Gerätespezifisch mit temporärem Root eingerichtet; kein allgemeiner Installationsweg |
| Fire TV Stick 4K Max, 2. Gen. / AFTKRT / Fire OS 8.1.8.5 | Auf diesem Projektgerät getestet | Home on Fire wird auf diesem Projektgerät genutzt |
| Andere Fire-OS-7/8-Geräte | Nicht durch dieses Projekt zugesichert | [Home on Fire](https://github.com/toolicious/home-on-fire) unterstützt Fire OS 7/8 laut Upstream |
| Vega OS | Nicht unterstützt | Kein Android-APK-Installationsweg |
| Google TV / Android 16 (Emulator) | Im Emulator getestet | Google-TV-Startbildschirm per ADB abschalten, siehe [Installation](docs/INSTALLATION.md#google-tv) |
| Android TV ohne Google TV | Nicht auf echter Hardware getestet | Per ADB oder Auswahldialog als Startbildschirm festlegen |

Amazons gesamtes Launcher-Paket **nicht deaktivieren oder deinstallieren**. Es enthält auch Geräteeinstellungen. Fire-OS-Updates können die Home-Umleitung beeinflussen.

## Installieren

Du brauchst eine signierte APK, [Android Platform-Tools](https://developer.android.com/tools/releases/platform-tools), aktiviertes ADB-Debugging und einen PC im selben Netzwerk. Ein öffentlicher APK-Release ist noch nicht eingerichtet. Baue die APK selbst oder verwende deine vorhandene lokale `build/tvhome.apk`.

In PowerShell im Projektordner; ersetze die Beispieladresse:

```powershell
$tv = '192.0.2.10:5555' # Durch die IP deines Fire TV ersetzen
adb connect $tv
# Die Autorisierungsabfrage auf dem Fernseher bestätigen.
adb -s $tv install -r .\build\tvhome.apk
adb -s $tv shell am start -n local.tvhome/.HomeActivity
```

Damit ist die App installiert. Für die Home-Taste unter Fire OS 7/8 installierst du zusätzlich **[Home on Fire von Toolicious](https://github.com/toolicious/home-on-fire)** und wählst dort **Glotze** als Ziel. Die App bleibt ein separates Projekt. Sie benötigt ihren Bedienungshilfendienst, kann kurz Amazon Home zeigen und unterstützt Fire OS 6 nicht.

**[Vollständige Installation →](docs/INSTALLATION.md)** mit Home-on-Fire-Einrichtung, optionalen Freigaben, Update- und Rücknahmeweg.

## Bedienung

| Taste / Menü | Funktion |
|---|---|
| Pfeile und OK | Navigieren und Apps öffnen |
| Menütaste oder OK lange halten | Menü der fokussierten App öffnen |
| Zurück auf dem Home-Bildschirm | Zur ersten App zurückkehren |
| Zahnrad oben | Eigene und Geräteeinstellungen öffnen |
| Bildschirmschoner → Gestalten | Uhr, Hintergrund und Widgets auswählen |

Auf Fire OS 8 startet der eigene Schoner vom eigenen Home-Bildschirm aus. Er ersetzt Amazons Ambient Experience nicht in jeder App. Haltestellen und Weltuhrziel wählst du in den Einstellungen unter „Orte“; bis dahin gelten Berliner BVG-Haltestellen und New York als Vorgabe.

## Entwickeln

Windows + Git Bash, JDK 17, Python 3, SDK Platform 35 und Build-Tools 35.0.0:

```bash
export ANDROID_HOME="/c/Pfad/zum/Android/Sdk"
export TVHOME_JDK="/c/Pfad/zum/jdk-17"
bash build.sh
```

Ausgabe: `build/tvhome.apk`. Das Skript ersetzt den gesamten Build-Ordner. Bestehende Installationen benötigen zum Aktualisieren denselben Signaturschlüssel. `signing/` bleibt lokal und muss separat gesichert werden.

**[Build, Signierung und CI →](docs/DEVELOPMENT.md)** · **[Architektur →](docs/ARCHITECTURE.md)**

## Dokumentation und Projektstand

[Installation](docs/INSTALLATION.md) · [Entwicklung](docs/DEVELOPMENT.md) · [Projektaufbau](docs/ARCHITECTURE.md) · [Datenschutz](docs/PRIVACY.md) · [Mitwirken](CONTRIBUTING.md) · [Sicherheit](SECURITY.md) · [Drittanbieter](THIRD_PARTY_NOTICES.md) · [Logo und Assetpaket](assets/glotze/README.md) · [Assetpaket als ZIP](docs/branding/glotze-asset-pack-v1.2.zip)

Der Code steht unter der [MIT-Lizenz](LICENSE). Es gibt noch keine automatisierte Test-Suite und keine stabile öffentliche Release-Signierung. CI-APKs verwenden Wegwerf-Schlüssel. Vor Veröffentlichung gilt die **[Release-Checkliste](docs/RELEASING.md)**.

Unabhängiges Projekt, nicht von Amazon oder Google unterstützt oder zertifiziert. Schriftlizenzen bleiben erhalten. Open-Meteo, ipapi.co, transport.rest und Home on Fire haben eigene Bedingungen.
