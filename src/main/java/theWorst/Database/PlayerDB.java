package theWorst.Database;

import arc.util.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import theWorst.Main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class PlayerDB {
    final String filename="DBSave.json";
    HashMap<String,PlayerData> data=new HashMap<>();
    public void Load(){
        String path=Main.directory+filename;
        try (FileReader fileReader = new FileReader(path)) {
            JSONParser jsonParser = new JSONParser();
            JSONObject saveData = (JSONObject) jsonParser.parse(fileReader);
            for(Object k:saveData.keySet()){
                String key=(String)k;
                PlayerData playerData=new PlayerData((JSONObject)saveData.get(key));
                data.put(key,playerData);
            }
        } catch (FileNotFoundException ex) {
            Log.info("No saves found.New save file " + path + " will be created.");

        } catch (ParseException ex) {
            Log.info("Json file is invalid.");
        } catch (IOException ex) {
            Log.info("Error when loading data from " + path + ".");
        }
    }
}
