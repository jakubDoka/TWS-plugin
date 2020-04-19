package theWorst.dataBase;

import mindustry.entities.type.Player;

public class PlayerData implements Cloneable,java.io.Serializable{
    public Rank rank = Rank.newcomer;
    public int playTime = 0;
    public int buildingsBuilt = 0;
    public int enemiesKilled = 0;
    public int deaths = 0;
    public int gamesPlayed = 0;
    public int gamesWon = 0;
    public boolean banned = false;
    public boolean online = false;
    public long bannedUntil = 0;
    public String banReason = "";
    public String originalName="";
    public String discordLink = "";

    public PlayerData(Player player){
        originalName=player.name;
    }

    public Object clone() throws CloneNotSupportedException{return super.clone();}
}
