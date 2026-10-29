package local.tvhome;

/** How a settings row looks: its label, and its icon on a tile of its own colour. */
final class RowLook {
    final int labelRes;
    final int iconRes;
    final int colorRes;

    RowLook(int labelRes, int iconRes, int colorRes) {
        this.labelRes = labelRes;
        this.iconRes = iconRes;
        this.colorRes = colorRes;
    }
}
