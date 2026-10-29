# Glotze · Assetpaket

Version 1.2.0 · 29. Oktober 2026

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="svg/banners/glotze-readme-dark.svg">
  <img src="svg/banners/glotze-readme-light.svg" alt="Glotze – Dein Fernseher. Dein Zuhause." width="100%">
</picture>

Das Glotze-Branding besteht aus der grünen Bildschirmfigur mit zwei ungleichen Augen und der Wortmarke **glotze**. Alle Logos, Icons und Banner liegen als transparente Vektoren vor.

**[Visuelle Übersicht öffnen](preview.html)** · **[Animationen ansehen](animations.html)** · [Dateiliste und Prüfsummen](manifest.json) · [Farbwerte](palette.json)

## Direkt verwenden

| Zweck | Datei |
|---|---|
| Logo auf hellem Hintergrund | [Horizontal · Farbe](svg/logos/glotze-logo-horizontal-primary.svg) |
| Logo auf dunklem Hintergrund | [Horizontal · helle Wortmarke](svg/logos/glotze-logo-horizontal-reversed.svg) |
| Hochformat | [Logo untereinander](svg/logos/glotze-logo-stacked-primary.svg) |
| Symbol ohne Schriftzug | [Grüne Bildschirmfigur](svg/marks/glotze-mark-primary.svg) |
| App-Symbol | [Quadratische Zeichenfläche](svg/icons/glotze-app-icon-primary.svg) |
| Favicon | [64 × 64](svg/icons/glotze-favicon.svg) |
| README auf hellem Hintergrund | [Banner · Light](svg/banners/glotze-readme-light.svg) |
| README auf dunklem Hintergrund | [Banner · Dark](svg/banners/glotze-readme-dark.svg) |
| Social Header | [Light](svg/banners/glotze-social-light.svg) · [Dark](svg/banners/glotze-social-dark.svg) |
| TV-Banner | [Light](svg/banners/glotze-tv-light.svg) · [Dark](svg/banners/glotze-tv-dark.svg) |
| Schriftzug ohne Figur | [Dunkel](svg/wordmarks/glotze-wordmark-ink.svg) · [Weiß](svg/wordmarks/glotze-wordmark-white.svg) · [Grün](svg/wordmarks/glotze-wordmark-lime.svg) |
| Einfarbige Logos | Dateien mit `mono-dark` oder `mono-light` im Ordner `svg/logos/` |
| Einfarbige Figur | Varianten `ink`, `white` und `lime` im Ordner `svg/marks/` |
| Augen für eigene Kompositionen | [Dunkel](svg/marks/glotze-eyes-ink.svg) · [Weiß](svg/marks/glotze-eyes-white.svg) |
| Wiederverwendbare Web-Symbole | [SVG-Sprite](svg/glotze-sprite.svg) |

## Was enthalten ist

- **39 SVG-Dateien:** Logos, Wortmarken, Figur, Augen, Icons, README-, Social- und TV-Banner, Palette, Sprite und zehn animierte Varianten.
- Acht Maskottchen-Animationen und zwei blinzelnde README-Banner mit statischem Ersatz bei reduzierter Bewegung.
- Transparente PNG-Fallbacks für Logos und Banner; das App-Symbol zusätzlich in 16, 32, 48, 64, 128, 192, 256, 512 und 1024 Pixeln.
- Offline-Vorschau, Farbwerte als JSON und CSS sowie eine Dateiliste mit SHA-256-Prüfsummen.
- Vektormaster und ein Skript zum erneuten Erzeugen der Assets.

Alle sichtbaren SVG-Assets haben **transparente Zeichenflächen**. Es gibt keine eingebetteten Bitmaps, Verläufe, Filter, Schatten oder externen Ressourcen. Wortmarke und Bannertext bestehen aus Pfaden; eine Schriftinstallation ist nicht erforderlich. Auch die Innenräume der Buchstaben bleiben durchsichtig. Bei den einfarbigen Bildschirmfiguren sind die Augen ausgespart. Das Sprite ist eine Symbolbibliothek und zeigt allein keine Grafik. Animierte SVGs enthalten SMIL-Animationen und eine kleine interne CSS-Regel für reduzierte Bewegung, aber keinen JavaScript-Code.

`light` und `dark` beschreiben den vorgesehenen Untergrund; sie fügen keinen Hintergrund hinzu. Die Raster und Flächen der HTML-Vorschau gehören ausschließlich zur Vorschau.

## Farben

| Farbe | Wert | Verwendung |
|---|---|---|
| Glotze Lime | `#B1F533` | Bildschirmfigur und Akzent |
| Ink | `#25282C` | Augen und Wortmarke auf hellen Flächen |
| Cream | `#F9F5EA` | Empfohlener warmer heller Untergrund |
| White | `#FFFFFF` | Wortmarke auf dunklen Flächen |

Die Vektoren verwenden feste flache Farben. Die Empfehlung für einen Untergrund ist kein Teil der transparenten Assets.

## Verwendung

Seitenverhältnis beibehalten. Die Figur und die beiden Augen zusammen skalieren; ihre leichte Asymmetrie ist beabsichtigt. Als Mindestabstand nach außen etwa eine schmale Augenbreite freihalten. Für kleine App-Symbole die Icon- oder Favicon-Datei verwenden; für große Platzierungen die Logos oder Banner.

Für HTML und Markdown genügt ein normaler Bildverweis. Für Inline-SVG lässt sich die Figur über die Gruppen-/Pfadkennungen `screen`, `left-eye` und `right-eye` ansprechen. Die Wortmarke wird im Fließtext **Glotze** geschrieben, im Logo **glotze**.

### README mit automatischer heller/dunkler Variante

Aus dem Stammverzeichnis dieses Projekts:

```html
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/glotze/svg/banners/glotze-readme-dark.svg">
  <img src="assets/glotze/svg/banners/glotze-readme-light.svg" alt="Glotze – Dein Fernseher. Dein Zuhause." width="100%">
</picture>
```

### SVG-Sprite

```html
<svg viewBox="0 0 673 504" role="img" aria-label="Glotze">
  <use href="assets/glotze/svg/glotze-sprite.svg#glotze-mark"></use>
</svg>
```

Für `glotze-mark-mono` und `glotze-wordmark` steuert `color` die Farbe über `currentColor`. Ein normaler SVG-Bildverweis funktioniert auch ohne Sprite und ohne JavaScript.

## Animationen

**[Interaktive Vorschau mit Pause und Wiederholung](animations.html)** · [Dateien und Laufzeiten](animations.json)

| Bewegung | Datei | Ablauf |
|---|---|---|
| Blinzeln | [glotze-blink.svg](svg/animations/glotze-blink.svg) | Ein kurzer Lidschlag alle 4,8 Sekunden |
| Doppelblinzeln | [glotze-double-blink.svg](svg/animations/glotze-double-blink.svg) | Zwei schnelle Lidschläge, 6,2-Sekunden-Schleife |
| Zwinkern | [glotze-wink.svg](svg/animations/glotze-wink.svg) | Nur das rechte Auge, 4,4-Sekunden-Schleife |
| Umschauen | [glotze-look-around.svg](svg/animations/glotze-look-around.svg) | Blick nach rechts und links, 6-Sekunden-Schleife |
| Hüpfen | [glotze-bounce.svg](svg/animations/glotze-bounce.svg) | Zwei kleine Hüpfer, 2,8-Sekunden-Schleife |
| Einschlafen | [glotze-doze.svg](svg/animations/glotze-doze.svg) | Schwere Augen und leicht geneigter Körper, 7,2-Sekunden-Schleife |
| Ruhiger Leerlauf | [glotze-idle.svg](svg/animations/glotze-idle.svg) | Sanftes Schweben mit einem Lidschlag, 6,4-Sekunden-Schleife |
| Auftauchen | [glotze-appear.svg](svg/animations/glotze-appear.svg) | Kurze Einblendung mit leichtem Nachfedern, einmalig 0,7 Sekunden |
| Blinzelnder README-Banner | [Light](svg/animations/glotze-readme-blink-light.svg) · [Dark](svg/animations/glotze-readme-blink-dark.svg) | Wortmarke und Claim bleiben stehen; die Figur blinzelt |

Für eine ruhige dauerhafte Platzierung passen **Blinzeln** oder **Leerlauf**. **Auftauchen** eignet sich für einen einmaligen Auftritt. Die SVGs benötigen keine zusätzliche Bibliothek und können beispielsweise direkt als Bild eingebunden werden:

```html
<img src="assets/glotze/svg/animations/glotze-blink.svg"
     width="168" height="126" alt="Glotze">
```

Bei aktiver Systemeinstellung `prefers-reduced-motion: reduce` zeigen alle animierten SVGs automatisch die statische Figur. SVG-Animationen und ihre Einbindung als Bild werden in der Browser-Vorschau geprüft. Die native Android-App wird durch das Assetpaket noch nicht auf animierte App-Ressourcen umgestellt.

Für eine animierte README können im oben gezeigten `<picture>` die beiden Bannerpfade durch `svg/animations/glotze-readme-blink-light.svg` und `svg/animations/glotze-readme-blink-dark.svg` ersetzt werden. In der Projekt-README ist weiterhin die statische Variante eingebaut.

## Erneut erzeugen

```powershell
python assets/glotze/source/build_assets.py
```

Die SVG-Erzeugung benötigt ausschließlich Python 3.10 oder neuer. Optional lassen sich mit `resvg_py` und `Pillow` transparente PNGs exportieren und alle sichtbaren SVGs auf Transparenz und abgeschnittene Formen prüfen:

```powershell
python assets/glotze/source/build_assets.py --render --archive docs/branding/glotze-asset-pack-v1.2.zip
```

`source/master.json` ist die gemeinsame Vektorquelle. [source/wordmark.png](source/wordmark.png) ist die Bildvorlage der aktuellen Wortmarke und kann für weitere Bildbearbeitungen verwendet werden. Zum erneuten Erzeugen der Assets genügt der Vektormaster; die Dateien in `svg/` können außerdem direkt in Vektoreditoren geöffnet werden.

Der Bannertext wurde aus der vorhandenen Inter-Schrift in Pfade umgewandelt. Die zugehörige [SIL Open Font License](licenses/Inter-OFL.txt) liegt bei. Die Wortmarke wurde aus der Bildvorlage in Vektorpfade umgewandelt.
