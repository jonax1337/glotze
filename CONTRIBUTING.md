# Mitwirken

Das Projekt befindet sich im privaten Vorbereitungsstand. Öffentliche Mitarbeit und Nutzungsrechte werden mit der Veröffentlichung festgelegt. Siehe [Veröffentlichungscheckliste](docs/RELEASING.md).

## Lokal arbeiten

1. Lies die [Build-Anleitung](docs/DEVELOPMENT.md).
2. Lege einen Branch für eine klar abgegrenzte Änderung an.
3. Folge dem vorhandenen Java-Stil. API 25 bleibt die Kompatibilitätsbasis.
4. Baue die APK und teste betroffene Abläufe auf eigener Hardware.
5. Dokumentiere Gerät, Fire-OS-Version und tatsächlich ausgeführte Prüfungen.

Keine Test-Erfolge behaupten, die nur aus einem erfolgreichen Build abgeleitet wurden. Für UI-Änderungen Screenshots und Fokuszustände beilegen. Geräte nur fernsteuern, wenn ihre Nutzung ausdrücklich abgesprochen ist.

## Keine privaten Daten einchecken

`signing/`, APKs, `.env` und Build-Ausgaben bleiben lokal. Vor einem Commit `git status --short` und den Diff prüfen. Logs können personenbezogene Daten enthalten. Einträge in `.gitignore` schützen nicht vor `git add -f` und entfernen keine früher eingecheckten Dateien.

## Meldungen

Bugs mit Gerätedaten und nachvollziehbaren Schritten beschreiben. Sicherheitsprobleme nicht in öffentlichen Issues mit Schlüsseln, persönlichen Daten oder verwertbaren Exploit-Details veröffentlichen. Siehe [SECURITY.md](SECURITY.md).
