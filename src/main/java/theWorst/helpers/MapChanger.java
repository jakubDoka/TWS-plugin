package theWorst.helpers;

import arc.Events;
import arc.struct.Array;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.maps.Map;
import theWorst.Main;
import theWorst.Package;
import theWorst.interfaces.Votable;


import static mindustry.Vars.*;

public class MapChanger implements Votable {
    int pageSize=15;
    @Override
    public void launch(Package p) {
        mindustry.maps.Map map=(mindustry.maps.Map)p.obj;
        if(map==null){
            Events.fire(new EventType.GameOverEvent(Team.crux));
            return;
        }

        Array<Player> players = new Array<>();
        for(Player player : playerGroup.all()) {
            players.add(player);
            player.setDead(true);
        }

        logic.reset();
        Call.onWorldDataBegin();
        world.loadMap(map, map.applyRules(Gamemode.survival));
        state.rules = world.getMap().applyRules(Gamemode.survival);
        logic.play();


        for(Player player : players){
            if(player.con == null) continue;

            player.reset();
            netServer.sendWorldData(player);
        }
    }

    @Override
    public Package verify(Player player, String object, String sAmount, boolean toBase) {
        Array<Map> mapList = maps.all();
        mindustry.maps.Map map = null;
        if (Main.isNotInteger(object)) {
            map = maps.all().find(m -> m.name().equalsIgnoreCase(object.replace('_', ' '))
                    || m.name().equalsIgnoreCase(object));
        } else {
            int idx = Integer.parseInt(object);
            if (idx < mapList.size) {
                map = maps.all().get(idx);
            }

        }
        if (map == null) {
            player.sendMessage(Main.prefix + "Map not found.");
            return null;
        }
        return new Package(object, map, player);
    }

    public String info(int page) {
        Array<mindustry.maps.Map> maps=Vars.maps.all();
        int pageCount=(int)Math.ceil(maps.size/(float)pageSize);


        StringBuilder b=new StringBuilder();
        b.append("[orange]--MAPS--[]\n\n");
        if(page>pageCount){
            b.append("there are only ").append(pageCount).append(" pages.\n");
            page=pageCount;
        }

        for (int i=(page-1)*pageSize;i<page*pageSize && i<maps.size;i++){
            b.append("[yellow]").append(i).append(".[]").append(maps.get(i).name()).append("\n");
        }
        return b.toString();
    }
}
