package theWorst.dataBase;

import arc.util.Log;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import theWorst.Main;

import java.io.*;
import java.util.HashMap;

public class DataBase {
    static HashMap<String,PlayerData> data=new HashMap<>();
    final String saveFile= Main.directory+"database.ser";



    public static PlayerData getData(Player player){
        return data.get(player.uuid);
    }

    public static PlayerData getData(String uuid){
        return data.get(uuid);
    }

    public static PlayerData getData(int idx){
        if(idx>data.keySet().size()){
            return null;
        }
        return (PlayerData) data.values().toArray()[idx];
    }

    public static PlayerData findData(String arg){
        if(!Main.isNotInteger(arg)){
            return getData(Integer.parseInt(arg));
        }
        Player p=Main.findPlayer(arg);
        if(p!=null) return getData(p);
        return getData(arg);
    }

    public static Rank getRank(Player player){
        return getData(player).rank;
    }

    public static Rank getTrueRank(Player player){
        return getData(player).trueRank;
    }


    public static void restartRank(Player player) {
        setRank(player,Rank.newcomer);
    }

    public void register(Player player){
        String uuid=player.uuid;
        if(data.containsKey(uuid)) {
            getData(player).connect(player);
            updateName(player);
            updateRank(player);
            return;
        }
        data.put(uuid,new PlayerData(player));
    }

    public void onDisconnect(Player player) {
        getData(player).disconnect();
    }

    private static void updateName(Player player) {
        PlayerData data=getData(player);
        player.name=data.originalName+data.rank.getRank();
    }

    public static void setRank(Player player,Rank rank){
        getData(player).rank=rank;
        if(rank.permanent){
            getData(player).trueRank=rank;
        }
        updateName(player);
    }

    public static void setRank(Player player,String rank){
        setRank(player,Rank.valueOf(rank));
    }

    public static void updateRank(Player player){
        Rank rank=getRank(player);
        if(!player.isAdmin && (rank==Rank.griefer || rank==Rank.newcomer)) return;
        for(Rank r:Rank.values()){
            if(r.getValue()>rank.getValue() && r.condition(player)){
                Call.sendMessage("[orange]"+player.name+"[] obtained "+r.getRank()+" rank.");
                setRank(player,r);
                return;
            }
        }
        /*if(!rank.permanent){
            Call.sendMessage("[orange]"+player.name+"[] lost his rank.");
            setRank(player,getTrueRank(player ));
        }*/
    }

    public static boolean hasPerm(Player player,int required){
        return getTrueRank(player).permission.getValue()>=required;
    }

    public static boolean hasPerm(Player player,Perm perm){
        return getTrueRank(player).permission==perm;
    }
    public static boolean hasSpecialPerm(Player player,Perm perm){
        return getRank(player).permission==perm;
    }

    public void updateStats(Player player,Stat stat){
        PlayerData data=getData(player);
        switch (stat) {
            case deaths:
                data.deaths+=1;
                break;
            case gamesWon:
                data.gamesWon+=1;
                break;
            case buildingsBroken:
                data.buildingsBroken+=1;
                break;
            case buildingsBuilt:
                data.buildingsBuilt+=1;
                break;
            case gamesPlayed:
                data.gamesPlayed+=1;
                break;
            case enemiesKilled:
                data.enemiesKilled+=1;
        }
        updateRank(player);
    }





    public void save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(data);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void load(){
        try {
            FileInputStream fileIn = new FileInputStream(saveFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object obj=in.readObject();
            if(obj instanceof HashMap) data = (HashMap<String, PlayerData>) obj;
            in.close();
            fileIn.close();
        } catch (ClassNotFoundException c) {
            Log.info("class not found");
            c.printStackTrace();
        } catch (FileNotFoundException f){
            Log.info("database no found, creating new");
        }catch (IOException i){
            Log.info("Database is incompatible with current version.");
        }
    }

    public String report(String search){
        StringBuilder b=new StringBuilder();
        b.append("\n");
        int i=0;
        for(String u:data.keySet()){
            PlayerData pd=getData(u);
            if(search!=null && !pd.originalName.startsWith(search)) continue;
            b.append(i).append(".").append(pd.originalName).append(" | ").append(pd.rank.name());
            b.append(" | ").append(u).append("\n");
            i++;
        }
        return b.toString();
    }

}
