package example;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Requesting{
    final String MAX_TRANSPORT="max_transport";
    final String THREAD_COUNT="thread_count";

    ArrayList<Request> requests=new ArrayList<>();
    HashMap<String ,Integer> config=new HashMap<>();

    public Requesting(){
        config.put(MAX_TRANSPORT,5000);
        config.put(THREAD_COUNT,3);
    }
}
