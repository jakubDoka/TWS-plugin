package theWorst.interfaces;

import mindustry.entities.type.Player;
import theWorst.Package;

public interface Votable{
    void launch(Package p);
    Package verify(Player player, String object, int amount, boolean toBase);
}
