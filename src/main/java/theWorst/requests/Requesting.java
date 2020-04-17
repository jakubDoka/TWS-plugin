package theWorst.requests;

import theWorst.requests.Request;

import java.util.ArrayList;
import java.util.HashMap;

public class Requesting {
    final String MAX_TRANSPORT = "max_transport";
    final String THREAD_COUNT = "thread_count";

    ArrayList<Request> requests = new ArrayList<>();
    HashMap<String, Integer> config = new HashMap<>();

    public Requesting() {
        config.put(MAX_TRANSPORT, 5000);
        config.put(THREAD_COUNT, 3);
    }

    public HashMap<String, Integer> getConfig() {
        return config;
    }
}
