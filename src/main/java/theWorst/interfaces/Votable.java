package example;

import mindustry.entities.type.Player;

public interface Votable{
    void launch(Package p);
    Package verify(Player player, String object, String sAmount, boolean toBase);
}
