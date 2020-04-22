package theWorst.dataBase;

public enum Perm {
    none(-1),
    normal(0),
    loadout(1),
    factory(1),
    build(1),
    destruct(1),
    suicide(1),
    high(1),
    higher(4),
    highest(100)
    ;
    private final int value;

    Perm(int value){
        this.value=value;
    }
    public int getValue() {
        return value;
    }
}
