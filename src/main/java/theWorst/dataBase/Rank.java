package theWorst.dataBase;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.entities.type.Player;

public enum Rank implements java.io.Serializable{
    griefer(Color.pink,Perm.none),
    newcomer(Items.copper.color){
        {
            displayed=false;
        }
    },
    verified(Items.titanium.color){
        {
            displayed=false;
        }
    },
    candidate(Items.thorium.color,Perm.high),
    admin(Color.blue,Perm.higher){
        {
            displayed=false;
        }
        @Override
        public boolean condition(Player player) {
            return player.isAdmin;
        }
    },
    pluginDev(Color.olive,Perm.higher){
        {
            description="Its me Mlokis.";
        }
    },
    depositor(Items.surgealloy.color,Perm.loadout){
        {
            permanent=false;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return data.loadoutVotes>300 && data.loadoutVotes/(data.playTime/(1000*60*60))>4;
        }
    },
    constructor(Items.phasefabric.color,Perm.factory){
        {
            permanent=false;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return data.factoryVotes>300 && data.factoryVotes/(data.playTime/(1000*60*60))>4;
        }
    },
    kamikaze(Items.blastCompound.color,Perm.suicide){
        {
            permanent=false;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return data.deaths>5000 && data.deaths/(data.playTime/(1000*60*60))>10;
        }
    },
    builder(Items.plastanium.color,Perm.build){
        {
            permanent=false;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return data.buildingsBuilt>30000 && data.buildingsBuilt/(data.playTime/(1000*60*60))>1000;
        }
    },
    bulldozer(Items.pyratite.color,Perm.destruct){
        {
            permanent=false;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return data.buildingsBroken>4000 && data.buildingsBroken/(data.playTime/(1000*60*60))>300;
        }
    },
    owner(Color.gold,Perm.highest);
    Color color;
    Perm permission=Perm.normal;
    boolean displayed=true;
    boolean permanent=true;
    String description="missing description";

    public int getValue() {
        int value=0;
        for(Rank r: Rank.values()){
            if(r==this){
                return value;
            }
            value++;
        }
        return -1;
    }

    Rank(Color color){
        this.color=color;
    }

    Rank(Color color, Perm permission){
        this.color=color;
        this.permission=permission;
    }

    public String getRank() {
        return displayed ? "[#"+color+"]<"+name()+">[]":"";
    }

    public boolean condition(Player player) {
        return false;
    }
}
