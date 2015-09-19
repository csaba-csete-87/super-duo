package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.Match;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;

/**
 * Retrieves and parses football scores data from server.
 * Created by yehya khaled on 3/2/2015.
 */
public class FetchDataService extends IntentService {
    public static final String LOG_TAG = "FetchDataService";

    public FetchDataService() {
        super("FetchDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        getData("n2");
        getData("p2");
    }

    private void getData(String timeFrame) {
        //Creating fetch URL
        final String BASE_URL = "http://api.football-data.org/alpha/fixtures"; //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
        //final String QUERY_MATCH_DAY = "matchday";

        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().
                appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();
        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSON_data = null;
        //Opening Connection
        try {
            URL fetch = new URL(fetch_build.toString());
            m_connection = (HttpURLConnection) fetch.openConnection();
            m_connection.setRequestMethod("GET");
            m_connection.addRequestProperty("X-Auth-Token", Utilities.AUTH_TOKEN);
            m_connection.connect();

            // Read the input stream into a String
            InputStream inputStream = m_connection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            JSON_data = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception here" + e.getMessage());
        } finally {
            if (m_connection != null) {
                m_connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error Closing Stream");
                }
            }
        }
        try {
            if (JSON_data != null) {
                //This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
                JSONArray matches = new JSONObject(JSON_data).getJSONArray("fixtures");
                if (matches.length() == 0) {
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    processJSONdata(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }


                processJSONdata(JSON_data, getApplicationContext(), true);
            } else {
                //Could not Connect
                Log.d(LOG_TAG, "Could not connect to server.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void processJSONdata(String JSONdata, Context mContext, boolean isReal) {
        final String SEASON_LINK = "http://api.football-data.org/alpha/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/alpha/fixtures/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";

        //Match data
        String League;
        String mDate;
        String mTime;
        String Home;
        String Away;
        String Home_goals;
        String Away_goals;
        String match_id;
        String match_day;

        ArrayList<Match> matchesArray = new ArrayList<>();

        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);

            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector<>(matches.length());
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match_data = matches.getJSONObject(i);
                League = match_data.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).getString("href");
                League = League.replace(SEASON_LINK, "");

                match_id = match_data.getJSONObject(LINKS).getJSONObject(SELF).
                        getString("href");
                match_id = match_id.replace(MATCH_LINK, "");
                if (!isReal) {
                    //This if statement changes the match ID of the dummy data so that it all goes into the database
                    match_id = match_id + Integer.toString(i);
                }

                mDate = match_data.getString(MATCH_DATE);
                mTime = mDate.substring(mDate.indexOf("T") + 1, mDate.indexOf("Z"));
                mDate = mDate.substring(0, mDate.indexOf("T"));
                SimpleDateFormat match_date = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.getDefault());
                match_date.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date parsedDate = match_date.parse(mDate + mTime);
                    SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm", Locale.getDefault());
                    new_date.setTimeZone(TimeZone.getDefault());
                    mDate = new_date.format(parsedDate);
                    mTime = mDate.substring(mDate.indexOf(":") + 1);
                    mDate = mDate.substring(0, mDate.indexOf(":"));

                    if (!isReal) {
                        //This if statement changes the dummy data's date to match our current date range.
                        Date fragmentDate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        mDate = simpleDateFormat.format(fragmentDate);
                    }
                } catch (Exception e) {
                    Log.d(LOG_TAG, "error here!");
                    Log.e(LOG_TAG, e.getMessage());
                }
                Home = match_data.getString(HOME_TEAM);
                Away = match_data.getString(AWAY_TEAM);
                Home_goals = match_data.getJSONObject(RESULT).getString(HOME_GOALS);
                Away_goals = match_data.getJSONObject(RESULT).getString(AWAY_GOALS);
                match_day = match_data.getString(MATCH_DAY);

                ContentValues match_values = new ContentValues();
                match_values.put(DatabaseContract.scores_table.MATCH_ID, match_id);
                match_values.put(DatabaseContract.scores_table.DATE_COL, mDate);
                match_values.put(DatabaseContract.scores_table.TIME_COL, mTime);
                match_values.put(DatabaseContract.scores_table.HOME_COL, Home);
                match_values.put(DatabaseContract.scores_table.AWAY_COL, Away);
                match_values.put(DatabaseContract.scores_table.HOME_GOALS_COL, Home_goals);
                match_values.put(DatabaseContract.scores_table.AWAY_GOALS_COL, Away_goals);
                match_values.put(DatabaseContract.scores_table.LEAGUE_COL, League);
                match_values.put(DatabaseContract.scores_table.MATCH_DAY, match_day);

                matchesArray.add(new Match(Home, Away, mDate, Home_goals, Away_goals));

                values.add(match_values);
            }
            String matchesJsonString = Utilities.getJsonStringFromMatches(matchesArray);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("matchesJsonString", matchesJsonString);
            editor.apply();

            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);
            mContext.getContentResolver().bulkInsert(DatabaseContract.BASE_CONTENT_URI, insert_data);

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

    }
}

