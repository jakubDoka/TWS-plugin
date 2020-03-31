package example;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;


import java.util.ArrayList;
import java.util.HashMap;

import static mindustry.Vars.*;

public class Pvp extends Plugin{

    ArrayList<SwitchRequest> switchRequests=new ArrayList<>();

    public Pvp(){
        /*Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });*/
        Events.on(ServerLoadEvent.class,e-> updateState(null,false));
        Events.on(WaveEvent.class,e-> updateState(null,false));
        Events.on(WorldLoadEvent.class,e-> updateState(null,false));
        Events.on(PlayerLeave.class,e-> updateState(e.player,false));
        Events.on(PlayerJoin.class,e-> {
            updateState(e.player,true);
            checkIfCrux(e.player);
        });


    }

    public void checkIfCrux(Player player){
        if(player.getTeam()==Team.crux){
            Team smallestTeam=getSmallestTeam();
            if(smallestTeam==null){
               smallestTeam=Team.sharded;
            }

            changeTeam(player,smallestTeam);
        }
    }

    public Team getSmallestTeam(){
        HashMap<Team,Integer> teamMap=new HashMap<>();
        for(Player p:playerGroup){
            Team pTeam=p.getTeam();
            if (teamMap.containsKey(pTeam)){
                teamMap.put(pTeam,teamMap.get(pTeam)+1);
            }else{
                teamMap.put(pTeam,1);
            }
        }
        int smallest=1000;
        Team smallestTeam=null;
        for(Team t:teamMap.keySet()){
            int thisT=teamMap.get(t);
            Log.info(t.toString()+"/"+thisT);
            if(thisT<smallest){
                smallest=thisT;
                smallestTeam=t;
            }
        }
        return smallestTeam;
    }

    public void updateState(Player player,boolean connected){
        ArrayList<Player> players=new ArrayList<>();
        for(Player p:playerGroup){
            players.add(p);
        }
        if(player!=null) {
            if (connected) {
                players.add(player);

            }else {
                players.remove(player);
            }
        }
        int teamCount=0;
        Team prevTeam=null;
        for(Player p:players){

            if(p.getTeam()==prevTeam){
                continue;
            }
            prevTeam=p.getTeam();
            teamCount++;
        }
        state.rules.waves= teamCount>1;
        Call.onSetRules(state.rules);
    }

    public static void changeTeam(Player p,Team t){
        p.resetNoAdd();
        p.setTeam(t);
        p.add();
    }

    public Player findPlayer(String name){
        for(Player p:playerGroup){
            if(p.name.equals(name)){
                return p;
            }
        }
        return null;
    }

    public Object findRequest(String name){
        for(SwitchRequest sr:switchRequests){
            if(sr.requested.name.equals(name)){
                return sr;
            }
        }
        return null;
    }
    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.register("switch-team","<player>","ask player from enemy team to switch teams",
                (arg,p)->{
            Player player=(Player) p;
            Player other=findPlayer(arg[0]);
            if (other==null){
                player.sendMessage("[Scarlet][Server][]There is no player named "+arg[0]+".");
                return;
            }
            if(player.getTeam()==other.getTeam()){
                player.sendMessage("[Scarlet][Server][]You cannot switch with a player from your own team.");
                return;
            }
            switchRequests.add(new SwitchRequest(player,other,switchRequests));
        });
        handler.register("request","<accept/deny>","accepts or denys any direct request.",
                (arg,p)->{
            Player player=(Player) p;
            SwitchRequest request=(SwitchRequest)findRequest(player.name);
            if(request==null){
                player.sendMessage("[Scarlet][Server][]You have no direct requests.");
                return;
            }
            request.handle(arg[0]);

        });
    }
}

