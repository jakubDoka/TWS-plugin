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
        return "[orange]==PLayer data==[]\n\n" +
                "name: " + originalName + "\n" +
                "rank: [#"+rank.color + "]" + rank.name() + "[]\n" +
                "playtime: " + Main.milsToTime(playTime) + "\n" +
                "born date: " + new Date(born).toString() + "\n" +
                "last active:" + new Date(lastActive).toString() + "\n" +
                "games played: " + gamesPlayed + "\n" +
                "games won: " + gamesWon + "\n" +
                "buildings built: " +buildingsBuilt + "\n" +
                "buildings broken: " +buildingsBroken + "\n" +
                "enemies killed: " + enemiesKilled + "\n" +
                "deaths: " + deaths;
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
