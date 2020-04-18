package theWorst.requests;

import arc.struct.Array;

public interface Requester {
    Array<Request> getRequests();
    void fail(String object, int amount);

}
