package theWorst.interfaces;

import theWorst.requests.Request;

import java.util.ArrayList;

public interface Requester {
    ArrayList<Request> getRequests();
    void fail(String object, int amount);
    String getProgress(Request request);

}
