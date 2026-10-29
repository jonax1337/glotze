# Daten und Berechtigungen

[Zur Übersicht](../README.md)

## Netzwerkdienste

| Dienst | Zweck | Übertragene Informationen |
|---|---|---|
| `ipapi.co/json/` | Ungefährer Standort | Der Dienst sieht die öffentliche IP; Antwort enthält unter anderem Stadt und Koordinaten |
| `api.open-meteo.com/v1/forecast` | Wetter | Standortkoordinaten und Prognoseparameter; der Dienst sieht die öffentliche IP |
| `v6.bvg.transport.rest` | Abfahrten | Hinterlegte Haltestellen-IDs und Abfrageparameter; der Dienst sieht die öffentliche IP |

Alle drei Aufrufe verwenden HTTPS. Drittanbieter haben eigene Datenschutz- und Nutzungsbedingungen. Es gibt keine Zusage zu deren Verfügbarkeit. VPNs und Provider-Routing können den wetterbezogenen Standort verfälschen. Wetterdaten werden lokal zwischengespeichert: Standort maximal einen Tag, Prognose maximal 30 Minuten vor einer erneuten Abfrage bei Bedarf.

## Lokale Daten

Die App speichert unter anderem Layout, versteckte und angeheftete Apps, Klangeinstellungen, Schoner-Stil, Wettercache und Nutzungsbewertungen in Android SharedPreferences. Die Nutzungssortierung verarbeitet mit Freigabe System-Nutzungsereignisse; ohne diese Freigabe verwendet sie eigene Launcher-Starts als Rückfallweg.

Benachrichtigungszugriff ermöglicht die Anzeige aktiver Mediensitzungen. Der aktuelle Code der Musik-Reaktion nutzt `android.media.audiofx.Visualizer` auf dem Audio-Ausgangsmix. Er implementiert keine Mikrofonaufnahme; Android verlangt dafür trotzdem `RECORD_AUDIO`. Die Berechtigung selbst bleibt weitreichend.

Im geprüften App-Quellcode ist kein eigener Konto-, Analytics- oder Telemetrie-Dienst eingebunden. Das ist keine Aussage über Fire OS, installierte Apps oder die oben genannten Drittanbieter.

## Systemänderungen

`WRITE_SECURE_SETTINGS` und `WRITE_SETTINGS` erlauben die Änderung von Schoner-Komponente, Schoner-Aktivierung, Bildschirm-Timeout und Ruhezeit. Die App merkt sich beim Schonerwechsel die vorige Komponente. Freigaben nur bewusst erteilen und die ursprünglichen Systemwerte vor Änderungen notieren.

`QUERY_ALL_PACKAGES` ermöglicht die App-Übersicht. `REQUEST_DELETE_PACKAGES` ermöglicht das Anstoßen einer Deinstallation über Androids Oberfläche. Die App erhält dadurch keinen Root-Zugriff.

## Entfernen und Diagnose

`android:allowBackup="false"` schaltet Androids reguläres App-Backup ab. Eine Deinstallation löscht lokale App-Daten. Das ersetzt keine Rücknahme zuvor geänderter Systemeinstellungen. Siehe [Installation und Rücknahme](INSTALLATION.md).

Vor dem Teilen von Logs oder Screenshots IP-Adressen, Seriennummern, Nutzungsinformationen, Benachrichtigungsinhalte und persönliche Standorte entfernen. Die im Projekt hinterlegten BVG-Stationen sind Beispielkonfiguration, keine universelle Standorterkennung.
