package theWorst.helpers;

import arc.math.Mathf;
import mindustry.entities.type.Player;
import theWorst.interfaces.Votable;
import theWorst.Package;

import static mindustry.Vars.logic;

public class WaveSkipper implements Votable {

    @Override
    public void launch(Package p) {
        for (int i = 0; i < p.amount; i++) {
            logic.runWave();
        }
    }

    @Override
    public Package verify(Player player, String object, String sAmount, boolean toBase) {
        int amount = Mathf.clamp(Integer.parseInt(sAmount), 1, 5);
        return new Package(object, amount, toBase, player);
    }
}
