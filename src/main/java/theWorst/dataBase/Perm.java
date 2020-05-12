package theWorst.dataBase;

public enum Perm {
    none(-1),
    normal(),
    loadout(){{
        description="Don t need vote to launch or use resources";
    }},
    factory(){{
        description="Don t need vote to send or build units";
    }},
    build(){{
        description="Builds instantly";
    }},
    destruct(){{
        description="Destroys instantly";
    }},
    suicide(){{
        description="Can use suicide command";
    }},
    high(1),
    higher(4),
    highest(100);

    private final int value;
    public String description=null;
    Perm(){
        this.value=0;
    }
    Perm(int value){
        this.value=value;
    }
    public int getValue() {
        return value;
    }
}
