package example;

import arc.struct.Array;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.Gamemode;
import mindustry.gen.Call;
import mindustry.maps.Map;

import static mindustry.Vars.*;

public class MapChanger implements Votable{

    @Override
    public void launch(Package p) {
        Array<Player> all = Vars.playerGroup.all();
        Array<Player> players = new Array<>();
        players.addAll(all);

        world.loadMap(p.map, p.map.applyRules(state.rules.attackMode ? Gamemode.attack:Gamemode.survival));

        Call.onWorldDataBegin();

        for (Player player : players) {
            Vars.netServer.sendWorldData(player);
            player.reset();
        }
    }

    @Override
    public Package verify(Player player, String object, String sAmount, boolean toBase) {
        Array<Map> mapList=maps.all();
        mindustry.maps.Map map=null;
        if(Main.isNotInteger(object)){
           map= mapList.find(m -> m.name().equalsIgnoreCase(object.replace('_', ' '))
                   || m.name().equalsIgnoreCase(object));
        } else {
            int idx=Integer.parseInt(object);
            if(idx<mapList.size){
                map = mapList.get(idx);
            }

        }
        if(map == null){
            player.sendMessage(Main.prefix+"Map not found.");
            return null;
        }
        return new Package(object,map,player);
    }
}
