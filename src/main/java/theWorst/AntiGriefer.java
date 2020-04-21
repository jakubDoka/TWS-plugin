package theWorst;

import arc.struct.ArrayMap;
import arc.util.Log;
import arc.util.Time;
import mindustry.entities.type.Player;

import org.json.simple.JSONObject;
import theWorst.dataBase.DataBase;
import theWorst.dataBase.Rank;
import theWorst.interfaces.LoadSave;
import theWorst.interfaces.Votable;

import java.util.HashMap;

import static mindustry.Vars.*;

public class AntiGriefer implements Votable, LoadSave {
    public static final String message= Main.prefix+"[pink]Okay griefer.";
    //public static final String rank="[pink]<Griefer>";
    static ArrayMap<String ,Long> griefers=new ArrayMap<>();
    static boolean emergency=false;

    public static boolean isGriefer(Player player){
        return DataBase.getRank(player)==Rank.griefer;
    }

    public static void abuse(Player player){
        player.kill();
        player.sendMessage(message);
    }

    public boolean isEmergency(){
        return emergency;
    }

    /*public static Long getLastMessageTime(Player player){
        return griefers.get(player.uuid);
    }

    public static void updateLastMessageTime(Player player){
        griefers.put(player.uuid, Time.millis());
    }

    public void addRank(Player p){
        if(isGriefer(p)){
            p.name=p.name+rank;
        }
    }

    public void removeRank(Player p){
        p.name=p.name.replace(rank,"");
        p.sendMessage(Main.prefix+"[pink]You are not griefer anymore.");
        Log.info(p.name+" is no longer griefer.");
    }*/


    @Override
    public void launch(Package p) {
        Player player=((Player)p.obj);
        if(p.object.equals("remove")){
            DataBase.restartRank(player);
            player.sendMessage(Main.prefix+"[pink]Your rank wos restarted.");
            Log.info(player+" is no longer griefer.");
            return;
        }
        DataBase.setRank(player,Rank.griefer);
        player.sendMessage(Main.prefix+"[pink]You were marked as griefer.");
        Log.info(player+" wos marked as griefer.");

    }

    @Override
    public Package verify(Player player, String object, String sAmount, boolean toBase) {
        Player target=Main.findPlayer(object);
        if(target==null){
            StringBuilder b=new StringBuilder();
            for(Player p:playerGroup){
                b.append(Main.cleanName(p.name)).append(", ");
            }
            player.sendMessage(Main.prefix+"Player not found. Options are: "+b.toString());
            return null;
        }
        if(target == player){
            player.sendMessage(Main.prefix+"You cannot mark your self.");
            return null;
        }
        if(target.isAdmin){
            player.sendMessage(Main.prefix+"You cannot mark admin.");
            return null;
        }
        Package p=new Package(isGriefer(target) ? "remove":"add",target,player);
        if(player.isAdmin){
            launch(p);
            return null;
        }
        if(isGriefer(player)){
            abuse(player);
            return null;
        }
        return p;
    }

    @Override
    public void load(JSONObject data) {
        if(data==null){
            return;
        }
        for(Object o:data.keySet()){
            griefers.put((String)o,(Long)data.get(o));
        }
    }

    @Override
    public JSONObject save() {
        JSONObject data=new JSONObject();
        for(String key:griefers.keys()){
            data.put(key,griefers.get(key));
        }
        return data;
    }

    public void switchEmergency() {
        emergency=!emergency;
    }
}
