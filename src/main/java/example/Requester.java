package example;

import mindustry.entities.type.Player;

import java.util.ArrayList;

public interface Requester {
    ArrayList<Request> getRequests();
    void fail(String object,int amount);
    String getProgress(Request request);
    void launch(Package p);
    Package verify(Player player, String object, String sAmount,boolean toBase);
}
