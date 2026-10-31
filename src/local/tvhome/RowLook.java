package local.tvhome;

/** How a settings row looks: its label, and its icon (a theme attribute, the design draws it) on a tile of its own colour. */
final class RowLook {
    final int labelRes;
    final int iconAttr;
    final int colorRes;

    RowLook(int labelRes, int iconAttr, int colorRes) {
        this.labelRes = labelRes;
        this.iconAttr = iconAttr;
        this.colorRes = colorRes;
    }
}
