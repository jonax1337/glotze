package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/** The per-tile options shown on long-press OK or the menu key. */
final class TileMenu {
    interface Actions {
        /** Tiles can only be placed by hand while the order is fixed, not while it follows usage. */
        boolean canMoveTiles();

        void moveFocusedTile();

        boolean isPinned(AppTile tile);

        void togglePin(AppTile tile);

        void hideTile(AppTile tile);

        boolean isLocked(AppTile tile);

        void toggleLock(AppTile tile);

        TileCategory categoryOf(AppTile tile);

        void setCategory(AppTile tile, TileCategory category);
    }

    private final Context context;
    private final OptionsSheet sheet;
    private final ActivityStarter starter;
    private final Actions actions;

    TileMenu(Context context, OptionsSheet sheet, ActivityStarter starter, Actions actions) {
        this.context = context;
        this.sheet = sheet;
        this.starter = starter;
        this.actions = actions;
    }

    void show(AppTile tile) {
        List<OptionsSheet.Option> options = new ArrayList<>();
        if (actions.canMoveTiles()) {
            options.add(option(R.string.action_move, actions::moveFocusedTile));
        } else {
            int pinLabel = actions.isPinned(tile) ? R.string.action_unpin : R.string.action_pin;
            options.add(option(pinLabel, () -> actions.togglePin(tile)));
        }
        options.add(option(R.string.home_action_category, () -> showCategories(tile)));
        int lockLabel = actions.isLocked(tile) ? R.string.home_action_unlock : R.string.home_action_lock;
        options.add(option(lockLabel, () -> actions.toggleLock(tile)));
        options.add(option(R.string.action_hide, () -> actions.hideTile(tile)));
        if (!actions.isLocked(tile)) {
            // App info offers "Open" and uninstalling removes the app, so a locked tile hides both.
            options.add(option(R.string.action_app_info, () -> starter.start(
                    packageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, tile), tile.label)));
            options.add(option(R.string.action_uninstall, () -> starter.start(
                    packageIntent(Intent.ACTION_DELETE, tile), tile.label)));
        }
        sheet.show(tile.label, options);
    }

    private void showCategories(AppTile tile) {
        TileCategory current = actions.categoryOf(tile);
        List<OptionsSheet.Option> options = new ArrayList<>();
        for (TileCategory category : TileCategory.values()) {
            String label = context.getString(category.labelRes);
            if (category == current) {
                label = context.getString(R.string.home_category_current, label);
            }
            options.add(new OptionsSheet.Option(label, () -> actions.setCategory(tile, category)));
        }
        sheet.show(tile.label, options);
    }

    private OptionsSheet.Option option(int labelRes, Runnable action) {
        return new OptionsSheet.Option(context.getString(labelRes), action);
    }

    private static Intent packageIntent(String action, AppTile tile) {
        return new Intent(action, Uri.fromParts("package", tile.key, null));
    }
}
