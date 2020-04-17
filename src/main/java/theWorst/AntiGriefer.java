package theWorst;

import arc.util.Time;
import mindustry.entities.type.Player;

import arc.struct.Array;
import mindustry.net.Administration;
import org.json.simple.JSONObject;
import theWorst.interfaces.LoadSave;
import theWorst.interfaces.Votable;

import java.util.HashMap;
import java.util.Map;

import static mindustry.Vars.netServer;
import static mindustry.Vars.player;

public class AntiGriefer implements Votable, LoadSave {
    public static final String message= Main.prefix+"[pink]Okay griefer.";
    public static final String rank="[pink]<Griefer>";
    static HashMap<String ,Long> griefers=new HashMap<>();
    Array<String> voted=new Array<>();

    public static boolean isGriefer(Player player){
        return griefers.containsKey(player.uuid);
    }

    public static void abuse(Player player){
        player.kill();
        player.sendMessage(message);
    }

    public static Long getLastMessageTime(Player player){
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
        player.sendMessage(Main.prefix+"[pink]You are not griefer anymore.");
    }


    @Override
    public void launch(Package p) {
        Player player=((Player)p.obj);
        if(p.object.equals("remove")){
            griefers.remove(player.uuid);
            removeRank(player);

            return;
        }
        updateLastMessageTime(player);
        addRank(player);
        player.sendMessage(Main.prefix+"[pink]You were marked as griefer.");

    }

    @Override
    public Package verify(Player player, String object, String sAmount, boolean toBase) {
        Player target=Main.findPlayer(object);
        if(target==null){
            player.sendMessage("Player not found.");
            return null;
        }
        if(target.isAdmin){
            player.sendMessage("You cannot mark admin.");
            return null;
        }
        if(player.isAdmin){
            launch(new Package(isGriefer(target) ? "remove":"add",target,player));
        }
        if(isGriefer(player)){
            abuse(player);
            return null;
        }
        return new Package(isGriefer(target) ? "remove":"add",target,player);
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
        for(String key:griefers.keySet()){
            data.put(key,griefers.get(key));
        }
        return data;
    }
}
