package theWorst.interfaces;

import org.json.simple.JSONObject;

import java.util.HashMap;

public interface LoadSave {
    void load(JSONObject data);
    HashMap<String, Integer> get_config();
    JSONObject save();


}
