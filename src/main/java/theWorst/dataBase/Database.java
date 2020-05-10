package theWorst.dataBase;

import arc.util.Log;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Administration;
import org.json.simple.JSONObject;
import theWorst.AntiGriefer;
import theWorst.Main;

import java.io.*;
import arc.struct.Array;
import java.util.HashMap;
import java.util.HashSet;

import static mindustry.Vars.*;
import static mindustry.Vars.netServer;

public class Database {
    static HashMap<String,PlayerData> data=new HashMap<>();
    final String saveFile= Main.directory+"database.ser";
    final String rankFile= Main.directory+"rankConfig.json";

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

    public static void switchSetting(Player player,Setting setting,boolean off){
        HashSet<String > settings=getData(player).settings;
        boolean contain=settings.contains(setting.name());
        if(off && contain){
            settings.remove(setting.name());
        } else if(!contain){
            settings.add(setting.name());
        }
    }

    public static boolean hasEnabled(Player player, Setting setting){
        return getData(player).settings.contains(setting.name());
    }

    public static PlayerData getData(String uuid){
        return data.get(uuid);
    }

    public static PlayerData getData(Player player){
        return getData(player.uuid);
    }

    public static PlayerData getData(int idx){
        if(idx>data.keySet().size()){
            return null;
        }
        for(PlayerData pd:data.values()){
            if(pd.serverId==idx){
                return pd;
            }
        }
        return null;
    }

    public static PlayerData findData(String arg){
        if(!Main.isNotInteger(arg)){
            return getData(Integer.parseInt(arg));
        }
        Player p=Main.findPlayer(arg);
        if(p!=null) return getData(p);
        return getData(arg);
    }

    public static String formLine(PlayerData pd){
        return "[yellow]"+pd.serverId+"[] | [gray]"+pd.originalName+"[] | "+pd.trueRank.getRankAnyway();
    }

    public static Array<String> getOnlinePlayersIndexes(){
        Array<String> ids=new Array<>();
        ids.addAll(data.keySet());
        Array<String> res=new Array<>();
        for(Player p:playerGroup){
            res.add(formLine(getData(p)));
        }
        return res;
    }

    public static Array<String> getAllPlayersIndexes(String search){
        Array<String> res=new Array<>();
        for(String uuid:data.keySet()){
            PlayerData pd=getData(uuid);
            if(search!=null && !pd.originalName.startsWith(search)) continue;
            res.add(formLine(pd));
        }
        return res;
    }

    public static void setRank(PlayerData pd,Rank rank,Player player){
        boolean wosGrifer=pd.rank==Rank.griefer;
        boolean wosAdmin=pd.rank.isAdmin;
        pd.rank=rank;
        Administration.PlayerInfo inf=pd.getInfo();
        if(rank.permanent){
            pd.trueRank=rank;
        }
        if(rank.isAdmin){
            if(!wosAdmin) netServer.admins.adminPlayer(inf.id,inf.adminUsid);
        }else if(wosAdmin && rank.permanent) {
            netServer.admins.unAdminPlayer(inf.id);
        }
        if (rank == Rank.griefer) {
            AntiGriefer.bunUnBunSubNet(pd, true);
        } else if (wosGrifer) {
            AntiGriefer.bunUnBunSubNet(pd, false);
        }
        if(player==null){
            player=playerGroup.find(p->p.name.equals(pd.originalName));
            if(player==null) return;
        }
        player.name=pd.originalName+pd.rank.getRank();
        player.isAdmin=rank.isAdmin;
    }

    public static void setRank(PlayerData pd,Rank rank){
        setRank(pd,rank,null);
    }

    public static void setRank(Player player,Rank rank){
        setRank(getData(player),rank,player);
    }

    public static void updateRank(Player player){
        PlayerData pd=getData(player);
        if(pd.trueRank==Rank.griefer) return;
        for(Rank r:Rank.values()){
            if(r.condition(player)){
                int val=pd.rank.getValue();
                if(r.getValue()>val){
                    Call.sendMessage(Main.prefix+"[orange]"+player.name+"[] obtained "+r.getRankAnyway()+" rank.");
                    setRank(pd,r,player);
                    return;
                }
                if(r.getValue()==val){
                    return;
                }
            }
        }
        if(!pd.rank.permanent){
            Call.sendMessage(Main.prefix+"[orange]"+player.name+"[] lost his "+pd.rank.getRankAnyway()+" rank.");
            pd.rank=pd.trueRank;
            player.name=pd.originalName+pd.rank.getRank();
        }
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

    public static boolean hasPerm(Player player,Perm perm){
        return getData(player).trueRank.permission.getValue()>=perm.getValue();
    }

    public static boolean hasSpecialPerm(Player player,Perm perm){
        return getData(player).rank.permission==perm;
    }

    public void onConnect(Player player){
        String uuid=player.uuid;
        if(!data.containsKey(uuid)) {
            data.put(uuid,new PlayerData(player));
        }
        PlayerData pd=getData(uuid);
        if(pd==null) return;
        pd.connect(player);

        if(AntiGriefer.isSubNetBanned(player)){
            setRank(player,Rank.griefer);
        } else {
            updateRank(player);
            player.name=pd.originalName+pd.rank.getRank();
        }

        player.isAdmin=pd.trueRank.isAdmin;
    }

    public void onDisconnect(Player player) {
        getData(player).disconnect();
    }

    public void loadRanks(){
        Main.loadJson(rankFile,(data)->{
            for(Object o:data.keySet()){
                String key=(String)o;
                try{
                    Rank rank=Rank.valueOf(key);
                    JSONObject settings=(JSONObject) data.get(key);
                    if (settings.containsKey(stb.isAdmin)) rank.isAdmin=(boolean)settings.get(stb.isAdmin);
                    if (settings.containsKey(stb.displayed)) rank.displayed=(boolean)settings.get(stb.displayed);
                    if (settings.containsKey(stb.permanent)) rank.permanent=(boolean)settings.get(stb.permanent);
                    if (settings.containsKey(stb.required)) rank.required=Main.getInt(settings.get(stb.required));
                    if (settings.containsKey(stb.frequency)) rank.frequency=Main.getInt(settings.get(stb.frequency));
                    if (settings.containsKey(stb.description)) rank.description=(String)settings.get(stb.description);
                    if (settings.containsKey(stb.permission)){
                        String perm=(String)settings.get(stb.permission);
                        try{
                            rank.permission=Perm.valueOf(perm);
                        }catch (IllegalArgumentException ex){
                            Log.info("Rank "+perm+"Does not exist.It will be ignored.");
                        }
                    }
                    rank.isAdmin=(boolean)settings.get("isAdmin");
                } catch (IllegalArgumentException ex){
                    Log.info("Rank "+key+"Does not exist.It will be ignored.");
                }

            }
        },this::createDefaultRankConfig);
    }

    private void createDefaultRankConfig() {
        Main.saveJson(rankFile,
                "Default "+rankFile+" created.Edit it adn the use apply config Command",
                ()->{
                    JSONObject data=new JSONObject();
                    for(Rank r:Rank.values()){
                        JSONObject settings=new JSONObject();
                        settings.put(stb.isAdmin,r.isAdmin);
                        settings.put(stb.displayed,r.displayed);
                        settings.put(stb.permanent,r.permanent);
                        settings.put(stb.required,r.required);
                        settings.put(stb.frequency,r.frequency);
                        settings.put(stb.description,r.description);
                        settings.put(stb.permission, r.permission.name());
                        data.put(r.name(),settings);
                    }
                    return data;
                });
    }

    enum stb{
        isAdmin,
        displayed,
        required,
        frequency,
        permanent,
        description,
        permission
    }

}
