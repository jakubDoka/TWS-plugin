package example;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
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

public class Pvp extends Plugin{

    //register event handlers and create variables in the constructor
    public Pvp(){
        /*Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });*/
        Events.on(ServerLoadEvent.class,e->{
            updateState(1);
        });
        Events.on(GameOverEvent.class,e->{
            updateState(2);
        });
        Events.on(PlayerConnect.class,e->{
            updateState(1);
        });
        Events.on(PlayerLeave.class,e->{
            updateState(3);

        });
    }
    public void updateState(int count){
        boolean start=playerGroup.size()>=count;
        state.rules.waves=start;
        state.rules.waveTimer=start;
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
    }
}
