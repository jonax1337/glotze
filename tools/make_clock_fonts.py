"""Cuts the lock screen clock fonts out of variable fonts (all SIL Open Font License).

Android 7 cannot select variable font axes, so every weight and width becomes its own static
file. Only digits and the colon are kept, which leaves each file at a few kilobytes.

Usage: python make_clock_fonts.py <folder with RobotoFlex.ttf, Inter.ttf, Fraunces.ttf, Nunito.ttf>
Writes to ../assets/fonts/clock/<family>_<weight>[_<width>].ttf
"""
import sys
from io import BytesIO
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

CLOCK_GLYPHS = "0123456789:"
WEIGHTS = ("thin", "light", "regular", "bold", "black")
WIDTHS = {"narrow": 50, "normal": 100, "wide": 151}

# Per family: source file, weight values in WEIGHTS order, axes fixed for every cut.
FAMILIES = {
    "roboto_flex": ("RobotoFlex.ttf", (100, 300, 400, 700, 1000), {"opsz": 144}),
    "inter": ("Inter.ttf", (100, 300, 400, 700, 900), {"opsz": 32}),
    "fraunces": ("Fraunces.ttf", (100, 300, 400, 700, 900), {"opsz": 144, "SOFT": 50}),
    "nunito": ("Nunito.ttf", (200, 300, 400, 800, 1000), {}),
    "unbounded": ("Unbounded.ttf", (200, 300, 400, 700, 900), {}),
    # Bodoni Moda starts at 400, so its two lightest cuts are the same.
    "bodoni_moda": ("BodoniModa.ttf", (400, 400, 500, 700, 900), {"opsz": 96}),
    "big_shoulders": ("BigShoulders.ttf", (100, 300, 400, 700, 900), {}),
}
# Only Roboto Flex has a real width axis.
FAMILIES_WITH_WIDTH = {"roboto_flex"}
# Families that only exist as static files, by weight in WEIGHTS order.
STATIC_FAMILIES = {
    "space_mono": ("SpaceMono-Regular.ttf", "SpaceMono-Regular.ttf", "SpaceMono-Regular.ttf",
                   "SpaceMono-Bold.ttf", "SpaceMono-Bold.ttf"),
}


def main(source_folder):
    out = Path(__file__).resolve().parent.parent / "assets" / "fonts" / "clock"
    out.mkdir(parents=True, exist_ok=True)
    for family, (file_name, weights, fixed_axes) in FAMILIES.items():
        reduced = clock_subset(Path(source_folder) / file_name)
        widths = WIDTHS if family in FAMILIES_WITH_WIDTH else {None: None}
        for weight_name, weight in zip(WEIGHTS, weights):
            for width_name, width in widths.items():
                axes = dict(fixed_axes, wght=weight)
                name = f"{family}_{weight_name}"
                if width_name:
                    axes["wdth"] = width
                    name += f"_{width_name}"
                instancer.instantiateVariableFont(TTFont(reduced), axes).save(out / f"{name}.ttf")
                print("wrote", name)
    for family, files in STATIC_FAMILIES.items():
        for weight_name, file_name in zip(WEIGHTS, files):
            TTFont(clock_subset(Path(source_folder) / file_name)).save(out / f"{family}_{weight_name}.ttf")
            print("wrote", f"{family}_{weight_name}")


def clock_subset(source):
    """Returns the reduced font as bytes, because instancing changes a font in place."""
    font = TTFont(source)
    options = subset.Options()
    options.layout_features = ["tnum", "lnum", "kern"]
    subsetter = subset.Subsetter(options)
    subsetter.populate(text=CLOCK_GLYPHS)
    subsetter.subset(font)
    buffer = BytesIO()
    font.save(buffer)
    return BytesIO(buffer.getvalue())


if __name__ == "__main__":
    main(sys.argv[1])
