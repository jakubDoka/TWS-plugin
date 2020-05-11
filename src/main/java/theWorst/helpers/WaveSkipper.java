package theWorst.helpers;

import arc.math.Mathf;
import arc.util.Log;
import jdk.nashorn.internal.ir.Block;
import mindustry.content.Blocks;
import mindustry.ctype.Content;
import mindustry.entities.type.Player;
import mindustry.game.SpawnGroup;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import theWorst.interfaces.Votable;
import theWorst.Package;

import java.util.HashMap;

import static mindustry.Vars.*;

public class WaveSkipper implements Votable {
    int spawns=0;

    @Override
    public void launch(Package p) {
        for (int i = 0; i < p.amount; i++) {
            logic.runWave();
        }
    }

    @Override
    public Package verify(Player player, String object, int amount, boolean toBase) {
        amount = Mathf.clamp(amount, 1, 5);
        return new Package(object, amount, toBase, player);
    }

    public String getWaveInfo(){
        int mil=1000000;
        int wave=state.wave+1;
        int firstAirWave=mil;
        HashMap<UnitType,Integer> units =new HashMap<>();
        for(UnitType u:content.units()){
            units.put(u,0);
        }
        StringBuilder b=new StringBuilder();
        for(SpawnGroup sg:state.rules.spawns){
            if(sg.type.flying && sg.begin<firstAirWave) firstAirWave=sg.begin;
            if(wave<sg.begin || wave>sg.end) continue;
            int progress=wave-sg.begin;
            int unitAmount=(int)(sg.unitAmount+progress*(sg.unitScaling>mil ? 0:sg.unitScaling));
            units.put(sg.type,units.get(sg.type)+Math.min(unitAmount,sg.max));

        }
        b.append("[orange]Next wave:[]\n");
        for(UnitType u:units.keySet()){
            if(units.get(u)==0) continue;
            b.append("[gray]").append(u.name).append(":[]").append(units.get(u)).append(" ");
        }
        b.append("\n");
        if(firstAirWave!=mil && firstAirWave>wave && wave%5==2){
            b.append("[gray]First air wave at:[]").append(firstAirWave);
        }
        return b.toString();
    }

    public void countSpawns() {
        spawns=0;
        for(Tile[] row:world.getTiles()){
            for(Tile t:row){
                if(t.block()==Blocks.spawn){
                    spawns++;
                }
            }
        }
    }
}
