package theWorst.dataBase;

import arc.util.Time;
import mindustry.entities.type.Player;
import theWorst.Main;

import java.util.Date;

public class PlayerData implements Cloneable,java.io.Serializable{
    public Rank rank = Rank.newcomer;
    public Rank trueRank = Rank.newcomer;
    public int playTime = 0;
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
    public boolean hudEnabled=true;
    public boolean waveInfo=false;
    public boolean banned = false;
    public long bannedUntil = 0;
    public String banReason = "";
    public String originalName = "";
    public String discordLink = "";




    public PlayerData(Player player){
        lastActive=Time.millis();
        connect(player);
    }

    public String toString(){
        String special=trueRank==rank ? "none":"[#"+rank.color + "]" + rank.name() + "[]";
        String activity=connected>lastActive ? "[green]currently active[]":
                "[gray]inactive for []" + Main.milsToTime(Time.timeSinceMillis(lastActive)) + "\n";
        return "[orange]==PLayer data==[]\n\n" +
                "[gray]name:[] " + originalName + "\n" +
                "[gray]rank:[] [#"+trueRank.color + "]" + trueRank.name() + "[]\n" +
                "[gray]special rank:[] " + special + "\n" +
                "[gray]playtime:[] " + Main.milsToTime(playTime) + "\n" +
                "[gray]server age:" + Main.milsToTime(Time.timeSinceMillis(born)) + "\n" +
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

    public Object clone() throws CloneNotSupportedException{return super.clone();}

    public void connect(Player player) {
        originalName=player.name;
        connected=Time.millis();
    }

    public void disconnect() {
        playTime+=Time.timeSinceMillis(connected);
        lastActive=Time.millis();
    }
}
