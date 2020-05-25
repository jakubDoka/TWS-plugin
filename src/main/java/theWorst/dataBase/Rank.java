package theWorst.dataBase;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.entities.type.Player;

import arc.util.Time;

public enum Rank implements java.io.Serializable{
    griefer(Color.pink,Perm.none),
    newcomer(Items.copper.color){
        {
            displayed=false;
        }
    },
    verified(Items.titanium.color,Perm.high){
        {
            displayed=false;
        }
    },
    candidate(Items.thorium.color,Perm.high),
    admin(Color.blue,Perm.higher){
        {
            displayed=true;
            isAdmin=true;
        }
    },
    pluginDev(Color.olive,Perm.higher){
        {
            description="Its me Mlokis.";
            isAdmin=true;
        }
    },
    owner(Color.gold,Perm.highest){
        {
            isAdmin=true;
        }
    },
    AFK(Color.gray,Perm.none){
        {
            permanent=false;
            required=1000*60*5;
            frequency=0;
        }
        @Override
        public boolean condition(PlayerData pd) {
            return Time.timeSinceMillis(pd.lastAction)>required;
        }
    };

    Color color;
    public Perm permission=Perm.normal;
    boolean displayed=true;
    public boolean isAdmin=false;
    public boolean permanent=true;
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

    public String getSuffix() {
        return displayed ? "[#"+color+"]<"+name()+">[]":"";
    }

    public String getName() {
        return  "[#"+color+"]<"+name()+">[]";
    }

    public boolean condition(PlayerData pd) {
        return false;
    }
}
