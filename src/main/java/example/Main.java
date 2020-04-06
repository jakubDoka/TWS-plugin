package example;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;


import java.util.ArrayList;
import java.util.HashMap;

import static mindustry.Vars.*;

public class Pvp extends Plugin{

    static String prefix="[scarlet][Server][]"


    public Pvp(){
        /*Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });*/

    }

    public Player findPlayer(String name){
        for(Player p:playerGroup){
            if(p.name.equals(name)){
                return p;
            }
        }
        return null;
    }


    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
    }
}
