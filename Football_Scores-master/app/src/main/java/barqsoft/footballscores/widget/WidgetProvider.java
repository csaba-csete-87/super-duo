package barqsoft.footballscores.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Random;

import barqsoft.footballscores.Match;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

public class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String data = sharedPreferences.getString("matchesJsonString", "");

            ArrayList<Match> matches = Utilies.getMatchesFromJsonString(data);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);

            if (matches != null && matches.size() > 0) {
                int min = 0;
                int max = matches.size() - 1;

                Random r = new Random();
                int j = r.nextInt(max - min + 1) + min;

                Match m = matches.get(j);

                // Get the layout for the App Widget and attach an on-click listener
                // to the button
                views.setTextViewText(R.id.date_textview, m.getDate());
                views.setTextViewText(R.id.home_name, m.getHomeName());
                views.setTextViewText(R.id.score_textview, String.format("%s - %s", m.getHomeGoals(), m.getAwayGoals()));
                views.setTextViewText(R.id.away_name, m.getAwayName());
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
