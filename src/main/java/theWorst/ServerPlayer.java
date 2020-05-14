package theWorst;

import arc.graphics.Color;
import mindustry.entities.type.Player;
import mindustry.game.Team;

import static mindustry.Vars.player;

public class ServerPlayer {

    public ServerPlayer(){
        player=new Player();
        player.name="Server";
        player.color= Color.scarlet;
        player.setTeam(Team.sharded);
    }
}
