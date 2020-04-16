package theWorst;

import mindustry.entities.type.Player;

public class Package{
    public int amount;
    public int x=0;
    public int y=0;
    public boolean toBase;
    public String object;
    public Player target;
    public mindustry.maps.Map map;

    public Package(String object,int amount,boolean toBase,Player target){
        this.object=object;
        this.amount=amount;
        this.toBase=toBase;
        this.target=target;
    }

    public Package(String object,int amount,boolean toBase,Player target,int x,int y){
        this.object=object;
        this.amount=amount;
        this.toBase=toBase;
        this.target=target;
        this.x=x;
        this.y=y;
    }
    public Package(String object,mindustry.maps.Map map,Player target){
        this.object=object;
        this.map=map;
        this.target=target;
    }
}
