package theWorst.dataBase;

import arc.math.Mathf;
import arc.util.Log;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Administration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import theWorst.Main;
import java.io.*;
import java.util.HashMap;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class DataBase {
    static HashMap<String,PlayerData> data=new HashMap<>();
    final String saveFile= Main.directory+"database.ser";
    final String rankFile= Main.directory+"rankConfig.json";



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
            updateRank(player);
            updateName(player);
            player.isAdmin=getTrueRank(player).isAdmin;
            return;
        }
        data.put(uuid,new PlayerData(player));
    }

    public void onDisconnect(Player player) {
        getData(player).disconnect();
    }

    public static void updateName(Player player) {
        PlayerData data=getData(player);
        player.name=data.originalName+data.rank.getRank();
    }

    public static void setRank(PlayerData pd,Rank rank){
        pd.rank=rank;
        Administration.PlayerInfo inf=netServer.admins.getInfo(pd.id);
        if(rank.permanent){
            pd.trueRank=rank;
        }
        if(rank.isAdmin){
            netServer.admins.adminPlayer(inf.id,inf.adminUsid);
        }else {
            netServer.admins.unAdminPlayer(inf.id);
        }
        Player player=playerGroup.find(p->p.name.equals(pd.originalName));
        if(player!=null){
            updateName(player);
            player.isAdmin=rank.isAdmin;
        }
    }

    public static void setRank(Player player,Rank rank){
       setRank(getData(player),rank);
    }

    public static void setRank(Player player,String rank){
        setRank(player,Rank.valueOf(rank));
    }

    public static void updateRank(Player player){
        Rank rank=getRank(player);
        if(!player.isAdmin && (rank==Rank.griefer || rank==Rank.newcomer)) return;
        for(Rank r:Rank.values()){
            if(r.getValue()>rank.getValue() && r.condition(player)){
                Call.sendMessage("[orange]"+player.name+"[] obtained "+r.getRankAnyway()+" rank.");
                setRank(player,r);
                return;
            }
        }
        if(!rank.permanent){
            setRank(player,getTrueRank(player));
            Call.sendMessage("[orange]"+player.name+"[] lost his "+rank.getRankAnyway()+" rank.");

        }
    }

    public static boolean hasPerm(Player player,int required){
        return getTrueRank(player).permission.getValue()>=required;
    }

    public static int getIndex(PlayerData searched){
        int idx=0;
        for(PlayerData pd:data.values()){
            if(pd==searched){
                return idx;
            }
            idx++;
        }
        return -1;
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

    public String report(String search ,boolean all,int page){
        int pageSize=15;
        int maxPages=(int)Math.ceil(data.size() / (float) pageSize);

        StringBuilder b=new StringBuilder();


        int begin=0;
        int end=data.size();

        if(page!=-1){
            page= Mathf.clamp(page,1,maxPages);
            begin=(page-1)*pageSize;
            end=pageSize+begin;
        }
        b.append(all ? "\n":"[orange]--PLAYER INFO("+Mathf.clamp(page,1,maxPages)+"/"+maxPages+")--[]\n\n");

        int i=0;
        for(String u:data.keySet()){

            if(i<begin || i>=end) {
                i++;
                continue;
            }
            PlayerData pd=getData(u);
            if(search!=null && !pd.originalName.startsWith(search)) continue;
            if(!all) b.append("[yellow]");
            b.append(i);
            if(!all) b.append("[]");
            b.append(" | ");
            if(!all) b.append("[gray]");
            b.append(pd.originalName);
            if(!all) b.append("[]");
            b.append(" | ");
            b.append(all ? pd.trueRank.name():pd.rank.getRankAnyway());
            if(all) b.append(" | ").append(u);
            b.append("\n");
            i++;
        }
        return b.toString();
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
