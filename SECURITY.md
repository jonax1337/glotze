# Sicherheit

## Unterstützungsstand

Version 0.6 ist ein privater Entwicklungsstand ohne zugesagte Sicherheitswartung. Fire-OS-Versionen unterscheiden sich stark. Eine funktionierende Home-Umleitung ist keine Sicherheitsfreigabe für Root-Verfahren.

## Probleme melden

Aktuell ist noch kein öffentlicher Meldekanal eingerichtet. Vor Veröffentlichung soll im GitHub-Repository **Private vulnerability reporting** aktiviert werden. Bis dahin sicherheitsrelevante Funde direkt mit der Projektpflege abstimmen und keine vertraulichen Details in ein öffentliches Issue schreiben. Es wird keine Antwortfrist zugesagt.

## Umgang mit Schlüsseln und Geräten

- APK-Signaturschlüssel verschlüsselt sichern und niemals ins Repository laden.
- ADB nur in vertrauenswürdigen Netzen verwenden und anschließend bei Bedarf abschalten.
- Systemberechtigungen nur für gewollte Funktionen erteilen.
- Keine Amazon-Systempakete pauschal deaktivieren oder entfernen.
- Geräte-Root, Home-Umleitung und APK-Installation getrennt behandeln.

Der vorhandene Build nutzt ein lokales Standardpasswort. Öffentliche Release-Signierung muss vor einer Veröffentlichung separat eingerichtet werden. Ein ignorierter Schlüssel ist nicht automatisch sicher verwahrt.
