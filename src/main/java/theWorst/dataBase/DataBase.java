package theWorst.dataBase;

import arc.util.Log;
import mindustry.entities.type.Player;
import mindustry.game.Stats;
import theWorst.Main;

import java.io.*;
import java.util.HashMap;

public class DataBase {
    static HashMap<String,PlayerData> data=new HashMap<>();
    String saveFile= Main.directory+"database.ser";




    public static PlayerData getData(Player player){
        return data.get(player.uuid);
    }

    public static Rank getRank(Player player){
        return getData(player).rank;
    }

    public void register(Player player){
        String uuid=player.uuid;
        if(data.containsKey(uuid)) {
            getData(player).originalName=player.name;
            updateName(player);
            return;
        }
        data.put(uuid,new PlayerData(player));
        updateName(player);
    }

    private static void updateName(Player player) {
        PlayerData data=getData(player);
        player.name=data.originalName+data.rank.getRank();
    }

    public static void setRank(Player player,String rank){
        getData(player).rank=Rank.valueOf(rank);
        updateName(player);
    }

    public void updateRank(Player player){
        Rank pRank=getRank(player);
        PlayerData data=getData(player);
        for(Rank r:Rank.values()){
            if(r.getValue()>pRank.getValue() && r.condition(data)){
                data.rank=r;
                updateName(player);
            }
        }
    }

    public static boolean hasPerm(Player player,int required){
        return getData(player).rank.permission>=required;
    }

    public void updateBuildCount(Player player){
        PlayerData data=getData(player);
        data.buildingsBuilt+=1;
        updateRank(player);
    }

    public void updateDeathCount(Player player) {
        PlayerData data=getData(player);
        data.deaths+=1;
        updateRank(player);
    }

    public void updateKillCount(Player player) {
        PlayerData data=getData(player);
        data.enemiesKilled+=1;
        updateRank(player);
    }

    public void updateWinCount(Player player) {
        PlayerData data=getData(player);
        data.gamesWon+=1;
        updateRank(player);
    }

    public void updateGameCount(Player player) {
        PlayerData data=getData(player);
        data.gamesPlayed+=1;
        updateRank(player);
    }




    public void save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(data);
            out.close();
            fileOut.close();
            Log.info("Serialized data is saved in "+saveFile+".");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void load(){
        try {
            FileInputStream fileIn = new FileInputStream(saveFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object obj=in.readObject();
            if(obj instanceof HashMap){
                data = (HashMap<String, PlayerData>)obj;
            }
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            Log.info("Employee class not found");
            c.printStackTrace();
        }
    }

    public String info(){
        return data.toString();
    }



}
