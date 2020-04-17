package theWorst.Database;

import org.json.simple.JSONObject;

import java.util.Locale;

public class PlayerData{
    String name,uiid,addres;
    int built,destroyed,deaths,kills,played;
    Locale lc;

    public PlayerData(JSONObject jsonObject) {
        name=(String)jsonObject.get("name");
        uiid=(String)jsonObject.get("uiid");

    }
}
