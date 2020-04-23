package theWorst;

import arc.struct.ArrayMap;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.entities.type.Player;

import mindustry.gen.Call;
import theWorst.dataBase.DataBase;
import theWorst.dataBase.Perm;
import theWorst.dataBase.Rank;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.Votable;

import java.util.TimerTask;

import static mindustry.Vars.*;

public class AntiGriefer implements Votable, Interruptible {
    public static final String message= Main.prefix+"[pink]Okay griefer.";

    Emergency emergency= new Emergency();

    public static boolean isGriefer(Player player){
        return DataBase.getRank(player)==Rank.griefer;
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
        if(isEmergency() && !DataBase.hasPerm(player, Perm.high.getValue())) {
            player.sendMessage("You don t have permission to do anything during emergency.");
            return false;
        }
        return true;
    }


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

    public static boolean verifyTarget(Player target,Player player,String matter){
        if(DataBase.hasPerm(target, Perm.higher.getValue())){
            player.sendMessage(Main.prefix+"You cannot kick "+DataBase.getTrueRank(player).getRank()+".");
            return false;
        }
        if(target.isAdmin){
            player.sendMessage(Main.prefix+"Did you really expect to be able to "+matter+" an admin?");
            return false;
        }
        if(target.isLocal){
            player.sendMessage(Main.prefix+"Local players cannot be "+matter+"ed.");
            return false;
        }
        if(target==player){
            player.sendMessage(Main.prefix+"You cannot "+matter+" your self.");
            return false;
        }
        if(isGriefer(player)){
            abuse(player);
            return false;
        }
        return true;
    }

    @Override
    public Package verify(Player player, String object, int amount, boolean toBase) {
        Player target;
        if(object.length() > 1 && object.startsWith("#") && Strings.canParseInt(object.substring(1))){
            int id = Strings.parseInt(object.substring(1));
            target = playerGroup.find(p -> p.id == id);
        }else{
            target = playerGroup.find(p -> p.name.equalsIgnoreCase(object));
        }
        if(target==null){
            target=Main.findPlayer(object);
            if(target==null){
                player.sendMessage(Main.prefix+"Player not found.");
                return null;
            }
        }

        if(!verifyTarget(target,player,"mark")) return null;

        Package p=new Package(isGriefer(target) ? "remove":"add",target,player);
        if(player.isAdmin){
            launch(p);
            return null;
        }
        return p;
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
                        "Be patient admins are currently dealing with griefer attack");
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
                "Emergency mode. Be patient [blue]admins[] have to eliminate all [pink]griefers[]" + time + "\n":null;
    }

    @Override
    public void interrupt() {
        emergency.restart();
    }

}
