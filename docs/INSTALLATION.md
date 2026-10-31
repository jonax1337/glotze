# Installation auf Fire TV, Google TV und Android TV

[Zur Übersicht](../README.md)

## Voraussetzungen

- Fire TV mit Fire OS, Google TV oder Android TV, mindestens Android 7.1 / API 25. Vega OS ist nicht kompatibel. Glotze erkennt das System selbst und zeigt in den Einstellungen die passenden Geräteseiten.
- PC mit [Android Platform-Tools](https://developer.android.com/tools/releases/platform-tools). `adb version` muss funktionieren; sonst die Befehle im Platform-Tools-Ordner mit `./adb.exe` ausführen.
- Fire TV und PC im selben vertrauenswürdigen Netzwerk. ADB nicht ins Internet freigeben.
- Eine signierte `tvhome.apk`: lokal unter `build/`, selbst gebaut oder später aus einem veröffentlichten Release. Aktuell ist kein Release-Download eingerichtet.

## 1. Debugging einschalten

Öffne **Einstellungen → Mein Fire TV → Info** und wähle den Gerätenamen siebenmal aus, falls die Entwickleroptionen verborgen sind. Auf Google TV und Android TV wählst du stattdessen unter **Einstellungen → System → Info** den **Android TV OS-Build** siebenmal aus. Aktiviere dann unter **Entwickleroptionen** das **ADB-Debugging** bzw. **USB-Debugging**. Menübezeichnungen können abweichen.

Notiere die Geräte-IP aus den Netzwerkeinstellungen. Die folgenden PowerShell-Beispiele verwenden Variablen statt Platzhaltern mit spitzen Klammern. Ersetze den Beispielwert durch die IP deines Fire TV.

```powershell
$tv = '192.0.2.10:5555' # Beispieladresse ersetzen
adb connect $tv
adb devices
```

Bestätige die Autorisierung auf dem Fernseher. `adb devices` muss das Ziel als `device` anzeigen, nicht als `unauthorized` oder `offline`.

## 2. APK installieren

Führe die Befehle im Projektordner aus:

```powershell
adb -s $tv install -r .\build\tvhome.apk
adb -s $tv shell am start -n local.tvhome/.HomeActivity
```

Die App heißt auf dem Gerät derzeit **Home**. `-r` aktualisiert die App ohne absichtliches Löschen ihrer Daten. Eine APK mit anderem Signaturschlüssel kann eine vorhandene Installation nicht ersetzen. Bei `INSTALL_FAILED_UPDATE_INCOMPATIBLE` den ursprünglichen Schlüssel verwenden, nicht vorschnell deinstallieren.

## 3. Home-Taste umleiten

### Fire OS 7 / 8: Home on Fire

[Home on Fire von Toolicious](https://github.com/toolicious/home-on-fire) ist eine separate App. Sie leitet die Home-Aktion über einen Bedienungshilfendienst um. Amazons Launcher bleibt installiert und bleibt der System-Home-Resolver. Root ist für diesen Weg nicht erforderlich.

1. Lade `home-on-fire.apk` aus den [offiziellen Releases](https://github.com/toolicious/home-on-fire/releases/latest) herunter.
2. Installiere sie und öffne die App:

   ```powershell
   adb -s $tv install -r .\home-on-fire.apk
   ```

3. Aktiviere ihren Bedienungshilfendienst über die Systemeinstellungen. Sind diese leer oder unbrauchbar, erteile die vom Upstream dokumentierte Freigabe und aktiviere den Dienst anschließend in Home on Fire:

   ```powershell
   adb -s $tv shell pm grant io.github.toolicious.homeonfire android.permission.WRITE_SECURE_SETTINGS
   ```

4. Wähle in Home on Fire **Home** / `local.tvhome` als Ziel. Aktiviere Start bei Boot oder Aufwachen nach Wunsch.
5. Öffne eine andere App und teste die physische Home-Taste. Prüfe danach auch Aufwachen und Neustart.

**Grenzen:** Fire OS 7 kann den Wechsel um bis zu fünf Sekunden verzögern. Auf Fire OS 8 kann Amazon Home kurz sichtbar werden. Ist Amazon Home bereits vorne, kann ein weiterer Home-Druck die Umleitung nicht auslösen. Upstream nennt OK oder Zurück bei fokussiertem Home-Symbol als Alternative. Langes Home-Drücken führt zurück zur Amazon-Oberfläche. Kindprofile und Sondertasten haben weitere Einschränkungen.

Upstream nennt Tests mit Fire OS 7.7.1.4–7.7.1.5 und 8.1.7.4–8.1.8.2. Das ist keine Garantie für jedes Gerät oder spätere Updates. Maßgeblich ist die [aktuelle Home-on-Fire-Anleitung](https://github.com/toolicious/home-on-fire).

### Google TV

Google TV schickt die Home-Taste immer an den eigenen Startbildschirm; `set-home-activity` meldet Erfolg, ändert aber nichts. Glotze wird erst Startbildschirm, wenn der Google-TV-Startbildschirm für den Nutzer abgeschaltet ist:

```powershell
adb -s $tv shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

Danach führt die Home-Taste zu Glotze; im Google-TV-Emulator mit Android 16 geprüft. Damit fehlt die Google-TV-Startseite mit ihren Empfehlungen; Apps und Einstellungen öffnest du über Glotze. Ein System-Update kann den Startbildschirm wieder einschalten; dann den Befehl wiederholen. Rückgängig:

```powershell
adb -s $tv shell pm enable com.google.android.apps.tv.launcherx
```

### Android TV ohne Google TV

Ältere Android-TV-Geräte und Projektoren lassen einen fremden Startbildschirm zu. Beim ersten Druck auf Home fragt das System, welche App den Startbildschirm übernimmt, oder du legst Glotze direkt fest:

```powershell
adb -s $tv shell cmd package set-home-activity local.tvhome/.HomeActivity
```

Dieser Weg ist bisher nicht auf echter Hardware getestet. Manche Hersteller sperren ihn wie Google TV.

### Fire OS 6: eigener Sonderfall

Die Launcher-App läuft auf dem getesteten AFTMM mit Fire OS 6.7.1.1. **Home on Fire unterstützt Fire OS 6 jedoch nicht.** Die Home-Taste dieses Testgeräts wurde separat mit temporärem Root eingerichtet. Das gehört nicht zur normalen Installation.

Die [Mantis-Anleitung bei XDA](https://xdaforums.com/t/root-guide-mantis-aftmm-ns6711-temporary-root-wolf-launcher-reversible-debloat.4802548/) ist eine geräte- und firmwareabhängige Referenz, keine universelle Empfehlung. Keine fremden Root-Befehle oder Profile übernehmen. Amazons gesamtes Launcher-Paket nicht deaktivieren oder entfernen: Es enthält auch Einstellungen. Ein `set-home-activity` mit Erfolgsmeldung beweist auf Fire OS allein noch keinen funktionierenden Home-Wechsel.

## 4. Optionale Freigaben

Die Grundfunktion als App benötigt keine der folgenden zusätzlichen ADB-Freigaben. Erteile nur, was du nutzen möchtest.

| Funktion | Freigabe / Auswirkung |
|---|---|
| Schoner-Auswahl und Ruhezeit | `WRITE_SECURE_SETTINGS`; kann geschützte Geräteeinstellungen ändern |
| Schoner-Startzeit | `WRITE_SETTINGS`; kann Systemeinstellungen ändern |
| Nutzungssortierung | `GET_USAGE_STATS`; liest die App-Nutzung |
| Musik-Reaktion | `RECORD_AUDIO`; erlaubt die Visualizer-Auswertung des Audio-Ausgangsmixes |
| Jetzt läuft | Benachrichtigungszugriff; ermöglicht Zugriff auf aktive Mediensitzungen |

```powershell
adb -s $tv shell pm grant local.tvhome android.permission.WRITE_SECURE_SETTINGS
adb -s $tv shell appops set local.tvhome WRITE_SETTINGS allow
adb -s $tv shell appops set local.tvhome GET_USAGE_STATS allow
adb -s $tv shell pm grant local.tvhome android.permission.RECORD_AUDIO
```

Für **Jetzt läuft** den Benachrichtigungszugriff in den Systemeinstellungen freigeben. Auf dem getesteten Fire OS 8 war alternativ dieser Befehl nutzbar:

```powershell
adb -s $tv shell cmd notification allow_listener local.tvhome/local.tvhome.MediaListener
```

Bei älteren Versionen kann der Befehl fehlen. Nicht die gesamte Einstellung `enabled_notification_listeners` blind überschreiben: Das entfernt andere Einträge.

## 5. Schoner einrichten

Öffne **Einstellungen → Bildschirmschoner**, wähle **Eigener** und passe Start- und Ruhezeit an. Unter **Gestalten** wählst du Uhr, Hintergrund und Widgets.

Wählt Glotze den eigenen Schoner, schaltet es auch den Start bei Inaktivität ein; Google TV lässt den Bildschirm sonst nur dunkel werden. Zurück auf den System-Schoner (etwa Google TVs Ambient-Modus) geht es mit derselben Zeile.

Auf Geräten mit Amazons Ambient Experience, insbesondere Fire OS 8, startet der eigene Schoner vom eigenen Home-Bildschirm aus etwa 30 Sekunden vor dem System-Timeout. Die App ersetzt Amazons Ambient Experience nicht systemweit. In anderen Apps besteht diese Garantie nicht. Das Deaktivieren von `com.amazon.tv.ftvambient` ist kein Standardschritt und kann dazu führen, dass der Bildschirm in anderen Apps direkt dunkel wird.

## Aktualisieren und entfernen

Updates: mit dem bisherigen Signaturschlüssel bauen und erneut `adb install -r` verwenden.

Vor dem Entfernen: Home-Umleitung ausschalten bzw. auf Google TV den Startbildschirm wieder einschalten, im Launcher den ursprünglichen System-Schoner auswählen und individuell eingerichtete Systemänderungen rückgängig machen. Notiere bestehende Timeout-Werte vor Änderungen; es gibt keinen universellen korrekten Rücksetzwert. Ein separat per Root eingerichteter Home-Wechsel benötigt seinen eigenen Rücknahmeweg.

Optional erteilte App-Freigaben lassen sich so zurücknehmen:

```powershell
adb -s $tv shell pm revoke local.tvhome android.permission.WRITE_SECURE_SETTINGS
adb -s $tv shell appops set local.tvhome WRITE_SETTINGS default
adb -s $tv shell appops set local.tvhome GET_USAGE_STATS default
adb -s $tv shell pm revoke local.tvhome android.permission.RECORD_AUDIO
adb -s $tv shell cmd notification disallow_listener local.tvhome/local.tvhome.MediaListener
```

Benachrichtigungszugriff alternativ über die Einstellungen abschalten. Danach `adb -s $tv uninstall local.tvhome` ausführen, wenn die App samt lokaler Daten wirklich gelöscht werden soll. Das Entziehen von Freigaben allein stellt zuvor geänderte Systemwerte nicht wieder her. Zum Schluss ADB-Debugging wieder deaktivieren, wenn es nicht mehr gebraucht wird.

## Häufige Probleme

| Symptom | Prüfen |
|---|---|
| `adb` wird nicht gefunden | Platform-Tools installieren, `PATH` prüfen oder `./adb.exe` im Tool-Ordner verwenden |
| `unauthorized` | Autorisierungsdialog am Fernseher bestätigen |
| Verbindung schlägt fehl | IP, WLAN, ADB-Debugging und Erreichbarkeit von Port 5555 im lokalen Netz prüfen |
| `more than one device` | Mit `-s $tv` das Ziel ausdrücklich auswählen |
| Home führt weiterhin zu Amazon | Home-on-Fire-Dienst und Ziel prüfen; APK-Installation allein reicht nicht |
| Home führt weiterhin zu Google TV | `launcherx` abgeschaltet? Nach System-Updates erneut prüfen |
| Schoner-Einstellung zeigt Freigabe fehlt | Optionale Berechtigungen für genau diese Paketkennung prüfen |
| Wetter zeigt falschen Ort | IP-Ortung kann durch VPN oder Provider abweichen; aktuell keine manuelle Ortswahl |
| Nutzungssortierung überrascht | Systemhistorie ist begrenzt; eigener Verlauf entsteht erst bei Nutzung |
| Update hat falsche Signatur | Originalen Schlüssel verwenden; Deinstallation löscht App-Daten |
