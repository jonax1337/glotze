"""Erzeugt die statischen Schnitte einer UI-Schrift (Android 7 kennt keine variablen Schriften).

Aufruf: python tools/ui_font_instances.py <variable.ttf> <Familienname> <Ausgabeordner>
Quellen der variablen Dateien (SIL OFL 1.1):
  Figtree: https://github.com/google/fonts/tree/main/ofl/figtree
  Inter:   https://github.com/google/fonts/tree/main/ofl/inter
Weitere Achsen wie die optische Größe von Inter bleiben auf ihrem Standardwert. Behalten werden
nur die lateinischen Zeichen, das hält jede Datei bei einigen Dutzend Kilobyte.
Benötigt fontTools (nur Entwicklungswerkzeug, keine Laufzeitabhängigkeit).
"""
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

WEIGHTS = {"Light": 300, "Regular": 400, "Medium": 500, "SemiBold": 600, "Bold": 700}
# Basic Latin, Latin-1, Latin Extended-A, general punctuation, the euro sign and arrows.
LATIN = "U+0020-007E,U+00A0-017F,U+2000-206F,U+20AC,U+2190-21FF"


def build_instance(source, family, name, weight, out_dir):
    font = TTFont(source)
    axes = {axis.axisTag: axis.defaultValue for axis in font["fvar"].axes}
    axes["wght"] = weight
    instance = instantiateVariableFont(font, axes, updateFontNames=True)
    keep_latin(instance)
    instance.save(out_dir / f"{family}-{name}.ttf")


def keep_latin(font):
    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=subset.parse_unicodes(LATIN))
    subsetter.subset(font)


def main(source, family, out_dir):
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, weight in WEIGHTS.items():
        build_instance(source, family, name, weight, out_dir)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], sys.argv[3])
