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
    public Package verify(Player player, String object, int amount, boolean toBase) {
        amount = Mathf.clamp(amount, 1, 5);
        return new Package(object, amount, toBase, player);
    }
}
