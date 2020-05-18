package theWorst;

import arc.Events;
import arc.struct.Array;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.net.Administration;
import theWorst.dataBase.Database;
import theWorst.dataBase.Perm;
import theWorst.dataBase.PlayerData;
import theWorst.dataBase.Setting;

import static mindustry.Vars.netServer;
import static mindustry.Vars.world;

public class ActionManager {
    TileInfo[][] data;

    public ActionManager(){
        Events.on(mindustry.game.EventType.PlayEvent.class, e->{
            data=new TileInfo[world.height()][world.width()];
            for(int y=0;y<world.height();y++){
                for(int x=0;x<world.width();x++){
                    data[y][x]=new TileInfo();
                }
            }
        });

        Events.on(EventType.TapEvent.class, e->{
            if(Database.hasEnabled(e.player, Setting.inspect)) return;
            String msg;
            TileInfo ti=data[e.tile.y][e.tile.x];
            if(ti.lastInteract==null){
                msg="No one interacted with this tile.";
            }else {
                msg="name: [orange]"+ti.lastInteract.originalName+"[]\n"+
                       "id: [orange]"+ti.lastInteract.serverId+"[]\n"+
                       "rank: [orange]"+ti.lastInteract.trueRank.getSuffix()+"[]\n";
            }
            Call.onLabel(e.player.con,msg,5,e.tile.x*8,e.tile.y*8);
        });

        Events.on(EventType.BlockBuildEndEvent.class, e->{
            if(e.player==null ) return;
            TileInfo ti=data[e.tile.y][e.tile.x];
            if(e.breaking) {
                ti.lock=0;
            } else {
                if (Database.getData(e.player).trueRank.permission.getValue() > Perm.high.getValue()) {
                    ti.lock=1;
                }
            }
            ti.lastInteract=Database.getData(e.player);
        });

        Events.on(EventType.BlockDestroyEvent.class, e-> {
            TileInfo ti=data[e.tile.y][e.tile.x];
            ti.lock=0;
        });

        Events.on(EventType.ServerLoadEvent.class,e->{
            netServer.admins.addActionFilter(action -> {
                if(action.type==Administration.ActionType.tapTile) return true;
                Player player = action.player;
                if (player == null) return true;
                PlayerData pd=Database.getData(player);
                TileInfo ti=data[action.tile.y][action.tile.x];
                if(pd.trueRank.permission.getValue()>=ti.lock){
                    ti.lastInteract=pd;

                    return true;
                }
                return false;
            });
        });
    }


    static class TileInfo{
        PlayerData lastInteract=null;
        int lock= Perm.normal.getValue();
    }
}
