package theWorst.dataBase;

import arc.util.Time;
import mindustry.entities.type.Player;
import mindustry.net.Administration;
import theWorst.Main;

import java.util.HashMap;
import java.util.HashSet;

import static mindustry.Vars.*;


public class PlayerData implements Cloneable,java.io.Serializable{
    public Rank rank = Rank.newcomer;
    public Rank trueRank = Rank.newcomer;
    public int serverId;
    public int playTime = 1;
    public int buildingsBuilt = 0;
    public int buildingsBroken = 0;
    public int enemiesKilled = 0;
    public int deaths = 0;
    public int gamesPlayed = 0;
    public int gamesWon = 0;
    public int factoryVotes =0;
    public int loadoutVotes =0;
    public long born = Time.millis();
    public long connected = 0;
    public long lastMessage = 0;
    public long lastActive;
    public long lastAction = Time.millis();
    public boolean banned = false;
    public long bannedUntil = 0;
    public String banReason = "";
    public String originalName = "";
    public String textColor = "white";
    public String discordLink = "";
    public String infoId;
    public String ip;


    HashSet<String> settings=new HashSet<>();
    HashMap<String,Object> advancedSettings=new HashMap<>();


    public PlayerData(Player player){
        lastActive=Time.millis();
        infoId=player.getInfo().id;
        serverId=Database.data.size();
    }

    public String toString(){
        String special=trueRank==rank ? "none":rank.getRankAnyway();
        String activity=connected>lastActive ? "[green]currently active[]":
                "[gray]inactive for []" + Main.milsToTime(Time.timeSinceMillis(lastActive));
        return "[orange]==PLayer data==[]\n\n" +
                "[yellow]Level:[]" + getLevel() + " | [yellow]server ID:[]" + serverId + "\n" +
                "[gray]name:[] " + originalName + "\n" +
                "[gray]rank:[] " + trueRank.getRankAnyway() + "\n" +
                "[gray]special rank:[] " + special + "\n" +
                "[gray]playtime:[] " + Main.milsToTime(playTime) + "\n" +
                "[gray]server age[]: " + Main.milsToTime(Time.timeSinceMillis(born)) + "\n" +
                activity + "\n" +
                "[gray]games played:[] " + gamesPlayed + "\n" +
                "[gray]games won:[] " + gamesWon + "\n" +
                "[gray]buildings built:[] " +buildingsBuilt + "\n" +
                "[gray]buildings broken:[] " +buildingsBroken + "\n" +
                "[gray]successful loadout votes:[] " +loadoutVotes+"\n" +
                "[gray]successful factory votes:[] " +factoryVotes+"\n" +
                "[gray]enemies killed:[] " + enemiesKilled + "\n" +
                "[gray]deaths:[] " + deaths;
    }

    public String socialStatus(){
        Administration.PlayerInfo info = getInfo();
        long kickTime=info.lastKicked-Time.millis();
        return "[gray][orange]--SOCIAL STATUS--[]\n\n"+
                (info.banned ? "[scarlet]BANNED[]":"[green]NOT BANNED[]") +
                "kicked [white]"+info.timesKicked+"[] times" +
                (info.admin ? "[yellow]ADMIN[]":"NOT ADMIN") +
                (kickTime>0 ? "[scarlet]kick ends after " + Main.milsToTime(kickTime) + "[]":"not kicked");
    }

    public Administration.PlayerInfo getInfo(){
        return netServer.admins.getInfo(infoId);
    }

    public int getLevel(){
        long value=buildingsBuilt*2+
                buildingsBroken*2+
                gamesWon*200+
                gamesPlayed*2+
                loadoutVotes*100+
                factoryVotes*100+
                enemiesKilled*5+
                playTime/(1000*60);
        int level=1;
        int first=500;
        while (true){
           value-=first* Math.pow(1.1,level);
           if(value<0){
               break;
           }
           level++;
        }
        return level;
    }

    public Object clone() throws CloneNotSupportedException{return super.clone();}

    public void connect(Player player) {
        originalName=player.name;
        connected=Time.millis();
        ip=player.con.address;
    }

    public void disconnect() {
        playTime+=Time.timeSinceMillis(connected);
        lastActive=Time.millis();
    }


}

