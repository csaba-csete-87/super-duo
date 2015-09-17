package barqsoft.footballscores;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Csabi on 17-Sep-15.
 */
public class Match {

    private String homeName;
    private String awayName;
    private String date;
    private String homeGoals;
    private String awayGoals;

    public Match(String homeName, String awayName, String date, String homeGoals, String awayGoals) {
        this.homeName = homeName;
        this.awayName = awayName;
        this.date = date;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    public String getHomeName() {
        return homeName;
    }

    public String getAwayName() {
        return awayName;
    }

    public String getDate() {
        return date;
    }

    public String getHomeGoals() {
        return homeGoals;
    }

    public String getAwayGoals() {
        return awayGoals;
    }

    public String toJson() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("homeName", getHomeName());
            jsonObject.put("awayName", getAwayName());
            jsonObject.put("date", getDate());
            jsonObject.put("homeGoals", getHomeGoals());
            jsonObject.put("awayGoals", getAwayGoals());

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

    }
}
