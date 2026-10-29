"""Erzeugt die statischen Figtree-Schnitte (Android 7 kennt keine variablen Schriften).

Aufruf: python tools/figtree_instances.py <Figtree[wght].ttf> <Ausgabeordner>
Quelle der variablen Datei: https://github.com/google/fonts/tree/main/ofl/figtree (SIL OFL 1.1).
Benötigt fontTools (nur Entwicklungswerkzeug, keine Laufzeitabhängigkeit).
"""
import sys
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

WEIGHTS = {"Light": 300, "Regular": 400, "Medium": 500, "SemiBold": 600, "Bold": 700}


def build_instance(source, name, weight, out_dir):
    font = TTFont(source)
    instance = instantiateVariableFont(font, {"wght": weight}, updateFontNames=True)
    instance.save(out_dir / f"Figtree-{name}.ttf")


def main(source, out_dir):
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, weight in WEIGHTS.items():
        build_instance(source, name, weight, out_dir)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
