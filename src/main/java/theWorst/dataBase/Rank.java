package theWorst.dataBase;

import arc.graphics.Color;
import mindustry.content.Items;

public enum Rank implements java.io.Serializable{
    griefer(Color.pink,-1),
    newcomer(Items.copper.color),
    member(Items.lead.color){
        @Override
        public boolean condition(PlayerData data) {
            return data.playTime>50;
        }
    },
    kamikaze(Items.blastCompound.color){
        @Override
        public boolean condition(PlayerData data) {
            return data.deaths>1;
        }
    },
    builder(Items.plastanium.color,1){
        @Override
        public boolean condition(PlayerData data) {
            return data.buildingsBuilt>10;
        }
    },
    veteran(Items.phasefabric.color,2){
        @Override
        public boolean condition(PlayerData data) {
            return data.gamesPlayed>10;
        }
    },
    general(Items.surgealloy.color,2){
        @Override
        public boolean condition(PlayerData data) {
            return data.gamesWon>10;
        }
    },
    moderator(Color.royal,4),
    owner(Color.gold,5)

    ;
    Color color;
    int permission=0;

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
    Rank(Color color, int permission){
        this.color=color;
        this.permission=permission;
    }

    public String getRank() {
        return "[#"+color+"]<"+name()+">[]";
    }

    public boolean condition(PlayerData data) {
        return false;
    }
}
