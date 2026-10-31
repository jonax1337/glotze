# Projektaufbau

[Zur Übersicht](../README.md)

## Einstieg und Oberfläche

| Bereich | Einstiegspunkte |
|---|---|
| Bühne (Startseite) | `HomeActivity`, `HomeHints`, `GlotzeView`, `GlotzeFigure`, `WeatherChip`, `DepartureChip` |
| Kacheln und Kategorien | `TileGrid`, `TileView`, `ArtworkFrame`, `TileCategory`, `TileMenu`, `TileMover` |
| App-Liste und Anordnung | `AppCatalog`, `TileArrangement`, `UsageHistory` |
| Designs "Soft Solid" und "Glas" | `Design`, `ThemeValues`, `SurfaceDrawable`, `Fonts` |
| App-Sperre | `AppLock`, `PinDialog` |
| Einstellungen / Gerät | `SettingsActivity`, `SystemSettings`, `Platform` (Fire TV, Google TV oder Android TV), `OptionsSheet`, `ActivityStarter` |
| Orte (Haltestellen, Weltuhr) | `PlacesActivity`, `PlaceSettings` |
| Sicherung | `BackupManager` |
| Wetter | `WeatherService`, `Weather`, `WeatherCondition`, `WeatherChip`, `WeatherOutlook` |
| Abfahrten | `Departure`, `DepartureFeed`, `DepartureChip`, `DeparturesWidget` |
| Schoner | `ScreenSaver`, `LockScreenActivity`, `LockScreenDream`, `LockScreen`, `NightMode`, `NightSchedule` |
| Schoner-Editor | `LockStyleActivity`, `LockStyle`, `LockBackdrop`, `AuroraView` |
| Widgets | `LockWidgets` und die jeweiligen `*Widget`-Klassen, darunter `CalendarWidget` und `GlotzeWidget` |
| Medien und Musik-Reaktion | `MediaListener`, `MediaWatcher`, `SoundLevel`, `TempoTracker` |

Die Startseite heißt **Bühne**: oben links Uhr, Datum und Chips für Wetter und Abfahrten, rechts Glotze, darunter das Dock aus deinen Apps und die Liste aller Apps, nach Kategorien gruppiert. Alle Flächen gehören zur Gestaltung **Soft Solid**: ein einziger flacher Farbton ohne Verlauf, Rand oder Schatten. `SurfaceDrawable` zeichnet sie (`card`, `raised`, `focusableCard`, `focusRing`). Nur der Fokus trägt Farbe, einen Ring in Glotze-Lime mit schmalem Spalt in der Hintergrundfarbe, innen an der Kante des Elements.

Das zweite Design **Glas** orientiert sich an tvOS: dunkelblauer Verlauf als Grund, Karten aus mattem Glas mit hellem Rand, Inter statt Figtree, Glotze in Weiß. Der Fokus färbt nichts lime, sondern füllt Zeilen und Knöpfe weiß (die Schrift wird dunkel) und legt über Kacheln einen Lichtschein. Gewählt wird es in den Einstellungen unter "Design"; `Design` speichert die Wahl und setzt vor dem ersten Inflate jedes Bildschirms das passende Theme (`HomeTheme` oder `HomeTheme.Glass`). Alles, was ein Design entscheidet, steht als Theme-Attribut in `res/values/design_attrs.xml`; Layouts lesen es als `?attr/...`, der Code über `ThemeValues`. Kehrt man aus den Einstellungen zurück, baut sich die Startseite im neuen Design neu auf.

## Dock auf der Bühne

Das Dock unter Uhr und Chips steht immer am selben Platz, trägt keine Überschrift, zeigt höchstens fünf Apps. Darunter schaut das Raster mit allen übrigen Apps unten am Bildschirmrand hervor (Überschrift und Oberkante der ersten Reihe, weich ausgeblendet wie in den Einstellungen), und keine App steht doppelt. Blättert man ins Raster, halten die Kacheln Abstand zur Pille oben rechts. Das Dock nimmt zuerst die angehefteten, dann die meistgenutzten, dann die übrigen Apps in der Reihenfolge der Anordnung; ausgeblendete Apps erscheinen nie. Das Dock ist damit nie leer, solange Apps installiert sind; ohne Apps steht ein Hinweis an seiner Stelle.

| Klasse | Zweck |
|---|---|
| `HomeShelf` | Wählt (`dockOf`) und baut das Dock und legt die Zellen auf dieselben fünf Spalten wie das Raster. |
| `HomeNavigation` | Lenkt die Pfeiltasten zwischen Einstellungs-Knopf, Dock und Raster, damit der Fokus nie in eine entfernte Zeile springt. |

Java-Code liegt unter `src/local/tvhome/`. Android-Einstiegspunkte und Berechtigungen stehen in `AndroidManifest.xml`. Der Projektname ist **Glotze**; die Paketkennung bleibt **`local.tvhome`**.

## Ressourcen und Konfiguration

- `res/values/strings.xml` und die Dateien `*_strings.xml`: sichtbarer App-Name `Glotze` und deutsche Texte, je nach Bereich getrennt.
- `res/values/surface_colors.xml`: die Farben von Soft Solid (`surface_ground`, `surface_card`, `surface_card_raised`, `text_primary`, `text_muted`); `glotze_lime` und `glotze_ink` stehen in `colors.xml`. Die Farben von Glas stehen in `glass_colors.xml`, beide Themes in `styles.xml`.
- `res/values/surface_dimens.xml`, `stage_dimens.xml`: Radien, Fokusring und alle Maße der Bühne. Die Vorlage misst 1920 × 1080 Pixel und der Fernseher 960 × 540 dp, ein dp entspricht also zwei Vorlagen-Pixeln. Maße kommen aus Dimens-Ressourcen, nicht aus dem Java-Code.
- `res/values/lock_style.xml`: Schoner-Auswahl sowie die Vorgaben für Haltestellen, Weltuhr-Stadt und Zeitzone.
- `res/values/dimens.xml`: Abstände und Größen; weitere Schoner-Maße stehen in `schoner_dimens.xml`.
- `res/drawable/`: unter anderem App-Symbol und TV-Banner.
- `assets/fonts/`: Figtree (Soft Solid) und Inter (Glas) als UI-Schriften (Light, Regular, Medium, SemiBold, Bold, erzeugt mit `tools/ui_font_instances.py`) und die Uhrenschriften unter `clock/`, jeweils mit Lizenz. Das Gewicht wählt `android:tag` (`display`, `medium`, `semibold`, `bold`), `Fonts` setzt es in der Familie des aktiven Designs.
- `tools/`: optionale Asset-Erzeugung, nicht Teil des APK-Builds.
- `assets/glotze/`: Glotze-Logos, Icons, Banner und Animationen samt Quellen und Vorschauen; getrennt von den Android-App-Ressourcen.
- `docs/branding/`: das aktuelle Glotze-Assetpaket als ZIP.

Haltestellen (bis zu vier) und Weltuhr-Stadt wählt man in den Einstellungen unter "Orte"; `PlaceSettings` speichert sie auf dem Gerät. Bis dahin gelten die Vorgaben aus `lock_style.xml` (Berliner BVG-Haltestellen, New York).

## Kompatibilitätsgrenzen

Die UI muss auf API 25 funktionieren. Flächen und Fokus-Animationen dürfen schwache Fire-TV-GPUs nicht durch zusätzliche Vollbild-Ebenen, Verläufe oder Dauer-Animationen überlasten; unsichtbare Animationen pausieren. Skalierte Kacheln benötigen genug Platz innerhalb scrollender Container, sonst werden sie beim Fokussieren abgeschnitten.

Home-Umleitung und Schoner sind getrennte Funktionen. Eine erfolgreiche APK-Installation oder ein korrekter HOME-Intent garantiert unter Fire OS keine Umleitung der physischen Home-Taste. Home on Fire ist eine separat installierte Anwendung und keine eingebettete Bibliothek.
