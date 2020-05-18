package theWorst;

import arc.Events;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.entities.type.Player;

import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.world.Tile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import theWorst.dataBase.Database;
import theWorst.dataBase.Perm;
import theWorst.dataBase.PlayerData;
import theWorst.dataBase.Rank;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.Votable;
import theWorst.interfaces.LoadSave;

import java.awt.*;
import java.util.HashSet;
import java.util.TimerTask;

import static mindustry.Vars.*;

public class AntiGriefer implements Votable, Interruptible,LoadSave{
    public static final String message= Main.prefix+"[pink]Okay griefer.";


    Emergency emergency= new Emergency();

    public static HashSet<String> subNets=new HashSet<>();



    public static String getSubNet(PlayerData pd){
        String address=pd.ip;
        return address.substring(0,address.lastIndexOf("."));
    }

    public static void bunUnBunSubNet(PlayerData pd,boolean bun){
        String subNet=getSubNet(pd);
        boolean contains=subNets.contains(subNet);
        if(bun && !contains){
            subNets.add(subNet);
            return;
        }
        if(contains && !bun){
            subNets.remove(subNet);
        }
    }



    public static boolean isSubNetBanned(Player player){
        return subNets.contains(getSubNet(Database.getData(player)));
    }

    public static boolean isGriefer(Player player){
        return Database.getData(player).trueRank==Rank.griefer;
    }

    public static void abuse(Player player){
        player.kill();
        player.sendMessage(message);
    }

    public boolean canBuild(Player player){
        if(isGriefer(player)){
            abuse(player);
            return false;
        }
        if(isEmergency() && !Database.hasPerm(player, Perm.high)) {
            player.sendMessage("You don t have permission to do anything during emergency.");
            return false;
        }
        return true;
    }


    @Override
    public void launch(Package p) {
        PlayerData pd=((PlayerData)p.obj);
        Player player=playerGroup.find(pl->pl.name.equals(pd.originalName));
        if(p.object.equals("remove")){
            Call.sendMessage(Main.prefix+"[orange]"+pd.originalName+"[] lost "+Rank.griefer.getName()+" rank.");
            pd.trueRank=Rank.newcomer;
            bunUnBunSubNet(pd,false);
            Log.info(pd.originalName+" is no longer griefer.");
        }else {
            Call.sendMessage(Main.prefix+"[orange]"+pd.originalName+"[] obtained "+Rank.griefer.getName()+" rank.");
            pd.trueRank=Rank.griefer;
            bunUnBunSubNet(pd,true);
            Log.info(pd.originalName+" wos marked as griefer.");
        }
        if(player==null) return;
        Database.updateName(player,pd);

    }

    @Override
    public Package verify(Player player, String object, int amount, boolean toBase) {
        Player target;
        PlayerData pd=null;
        if(object.length() > 1 && object.startsWith("#") && Strings.canParseInt(object.substring(1))){
            int id = Strings.parseInt(object.substring(1));
            target = playerGroup.find(p -> p.id == id);
        }else{
            target = playerGroup.find(p -> p.name.equalsIgnoreCase(object));
        }
        if(target==null){
            target=Main.findPlayer(object);
            if(target==null){
                pd=Database.findData(object);
                if(pd!=null){
                    if(pd.trueRank.isAdmin){
                        player.sendMessage(Main.prefix+" You cannot mark " + pd.trueRank.getName() + ".");
                        return null;
                    }
                }
            }
        }
        if(target!=null){
            if(target.isAdmin){
                player.sendMessage(Main.prefix+"Did you really expect to be able to mark an admin?");
                return null;
            }
            if(target==player){
                player.sendMessage(Main.prefix+"You cannot mark your self.");
                return null;
            }
            pd=Database.getData(target);
        }
        if(pd==null){
            player.sendMessage(Main.prefix+"Player not found.");
            return null;
        }
        Package p=new Package(pd.trueRank==Rank.griefer ? "remove":"add",pd,player);
        if(player.isAdmin){
            launch(p);
            return null;
        }
        return p;
    }

    @Override
    public void load(JSONObject data) {
        for(Object o:(JSONArray) data.get("subNets")){
            subNets.add((String)o);
        }
    }

    @Override
    public JSONObject save() {
        JSONArray subs =new JSONArray();
        subs.addAll(subNets);
        JSONObject data =new JSONObject();
        data.put("subNets",subs);
        return data;
    }

    static class Emergency{
        Timer.Task thread;
        int time;
        boolean red;

        void start(){

            if(active()){
                restart();
            }else {
                Call.sendMessage(Main.prefix+"[scarlet]Emergency started you cannot build or break nor configure anything." +
                        "Be patient admins are currently dealing with griefer attack.[]");
            }
            time= 180;
            thread=Timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(time<=0){
                        restart();
                    }
                    time--;
                    red=!red;
                }
            }, 0,1);
        }

        boolean active(){
            return thread!=null;
        }

        String getColor(){
            return red ? "[scarlet]":"[gray]";
        }

        void restart(){
            if(thread==null){
                return;
            }
            Call.sendMessage(Main.prefix+"[green]Emergency ended.");
            thread.cancel();
            thread=null;
        }
    }

    public boolean isEmergency(){
        return emergency.active();
    }

    public void switchEmergency(boolean off) {
        if(off){
            emergency.restart();
            return;
        }
        emergency.start();
    }

    @Override
    public String getHudInfo() {
        String time=String.format("%d:%02d",emergency.time/60,emergency.time%60);
        return emergency.active() ? emergency.getColor() + time +
                "Emergency mode. Be patient [blue]admins[] have to eliminate all [pink]griefers[]" + time + "[]":null;
    }

    @Override
    public void interrupt() {
        emergency.restart();
    }

}
