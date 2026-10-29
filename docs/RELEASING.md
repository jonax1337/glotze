# Vor der GitHub-Veröffentlichung

[Zur Übersicht](../README.md)

## Bereits vorbereitet

- Git-Ausschlüsse für Signierung, APKs, Build-Ausgaben und lokale Konfiguration.
- README mit getrennten Installations-, Entwicklungs- und Datenschutzinformationen.
- Issue- und Pull-Request-Vorlagen, Beitrags- und Sicherheitshinweise.
- Windows-Build-Workflow mit begrenzten Rechten und gepinnten Actions.
- Projektname und Logo **Glotze** mit transparentem SVG-Assetpaket, Animationen und README-Banner.

## Noch zu entscheiden und zu prüfen

- [ ] Marken- und Namenskonflikte für Glotze recherchieren.
- [x] Repository-Namen und Sichtbarkeit bestimmen: `jonax1337/glotze`, öffentlich.
- [x] Lizenz für eigenen Code und Assets wählen und `LICENSE` ergänzen: MIT.
- [ ] Schriftlizenzen, Reserved Font Names und API-Attributionen prüfen.
- [x] Persönliche Haltestellen und Weltuhr-Konfiguration vor Veröffentlichung bewusst prüfen: Vorgaben sind nun neutral (Berliner Knotenbahnhöfe, New York).
- [ ] Signaturschlüssel offline sichern; öffentliche Release-Signierung einrichten.
- [ ] Ersten GitHub-Actions-Lauf ausführen und Artefakt prüfen.
- [ ] Getrennte Tests auf Fire OS 6 und 8 sowie echte Home-Taste dokumentieren.
- [ ] Echte Screenshots ohne persönliche Daten aufnehmen. Das README-Banner ist kein Screenshot.
- [ ] Private vulnerability reporting und gewünschte Branch-Regeln aktivieren.
- [ ] Ausschließlich beabsichtigte Dateien stagen und vor dem ersten Commit prüfen.

## Git lokal prüfen

```bash
git status --short
git status --short --ignored
git diff --check
git ls-files
```

Dieses Projekt soll keinen Signaturschlüssel, keine lokalen APK-Sicherungen und keine Gerätestatus-Dumps enthalten. Ein privates GitHub-Repository ist kein Schlüsselbackup.

## Veröffentlichungsgrenze

Die Vorbereitung erstellt keinen Remote, keinen Commit, keinen Push und kein Release. Erst nach den Entscheidungen oben das GitHub-Repository anlegen. CI-APKs verwenden Wegwerf-Schlüssel und sind keine Release-APKs.
