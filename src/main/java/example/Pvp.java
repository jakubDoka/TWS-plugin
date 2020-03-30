package example;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.EntityGroup;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.plugin.Plugin;
import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import java.io.*;
import java.util.ArrayList;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static mindustry.Vars.*;

import java.awt.*;
import java.util.HashMap;

public class Pvp extends Plugin{

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
            player.setTeam(smallestTeam);
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

    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
    }
}
