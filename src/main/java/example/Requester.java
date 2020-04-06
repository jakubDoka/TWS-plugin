package example;

public interface Requestable{
    void finish();
    void fail();
    void start();
    String get_progress();
}
