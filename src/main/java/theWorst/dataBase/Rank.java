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
            required=300;
            frequency=4;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return check(data,data.loadoutVotes);
        }
    },
    general(Items.phasefabric.color,Perm.factory){
        {
            permanent=false;
            required=300;
            frequency=4;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return check(data,data.factoryVotes);
        }
    },
    kamikaze(Items.blastCompound.color,Perm.suicide){
        {
            permanent=false;
            required=5000;
            frequency=10;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return check(data,data.deaths);
        }
    },
    builder(Items.plastanium.color,Perm.build){
        {
            permanent=false;
            required=30000;
            frequency=1000;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return check(data,data.buildingsBuilt);
        }
    },
    bulldozer(Items.pyratite.color,Perm.destruct){
        {
            permanent=false;
            required=10000;
            frequency=200;
        }
        @Override
        public boolean condition(Player player) {
            PlayerData data=DataBase.getData(player);
            return check(data,data.buildingsBroken);
        }
    },
    owner(Color.gold,Perm.highest);
    Color color;
    Perm permission=Perm.normal;
    boolean displayed=true;
    boolean permanent=true;
    int required = 0;
    int frequency = 0;
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

    public boolean check(PlayerData pd,int value){
        return value>required && value/(pd.playTime/(1000*60*60))>frequency;
    }
}
