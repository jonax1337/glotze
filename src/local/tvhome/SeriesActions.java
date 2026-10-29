package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** What a followed series offers on the home screen: opening it in its app, and the options on long press. */
final class SeriesActions {
    interface Host {
        /** Starts the series' app, after the PIN if that app is locked. */
        void startSeriesApp(Intent launch, View source, CharSequence label);
    }

    private final Context context;
    private final OptionsSheet sheet;
    private final SeriesShelf shelf;
    private final Host host;

    SeriesActions(Context context, OptionsSheet sheet, SeriesShelf shelf, Host host) {
        this.context = context;
        this.sheet = sheet;
        this.shelf = shelf;
        this.host = host;
    }

    /** Opens the series in its app; while no app is known yet, asks which one to use. */
    void open(SeriesCard card, View source) {
        Intent launch = shelf.launchIntentFor(card);
        if (launch != null) {
            host.startSeriesApp(launch, source, card.title);
        } else {
            chooseApp(card, () -> open(card, source));
        }
    }

    void showOptions(SeriesCard card) {
        List<OptionsSheet.Option> options = new ArrayList<>();
        options.add(new OptionsSheet.Option(
                context.getString(R.string.series_action_unfollow), () -> shelf.unfollow(card)));
        options.add(new OptionsSheet.Option(
                context.getString(R.string.series_action_choose_app), () -> chooseApp(card, () -> { })));
        sheet.show(card.title, options);
    }

    /** Lists the apps that can be started; the choice is remembered for the series, then {@code afterChoice} runs. */
    private void chooseApp(SeriesCard card, Runnable afterChoice) {
        List<AppChoice> choices = shelf.appChoices();
        if (choices.isEmpty()) {
            Toast.makeText(context, R.string.series_no_apps, Toast.LENGTH_SHORT).show();
            return;
        }
        List<OptionsSheet.Option> options = new ArrayList<>();
        for (AppChoice choice : choices) {
            options.add(new OptionsSheet.Option(choice.label, () -> {
                shelf.chooseApp(card, choice.packageName);
                afterChoice.run();
            }));
        }
        sheet.show(context.getString(R.string.series_choose_app_title, card.title), options);
    }
}
