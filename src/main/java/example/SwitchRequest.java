package example;

import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;

import java.util.ArrayList;

public class SwitchRequest{
    Player requesting;
    Player requested;
    boolean handled;

    public SwitchRequest(Player requesting, Player requested, ArrayList<SwitchRequest> list){
        this.requested=requested;
        this.requesting=requesting;
        requested.sendMessage("[scarlet][Server][]" + requesting.name + " would like to switch teams with you.Write /request accept or /request deny to the chat.");
        requesting.sendMessage("[scarlet][Server][]Request wos sent to " +requested.name + ".");
        Timer.schedule(()->{
            list.remove(this);
            if(handled){
               return;
            }
            requesting.sendMessage("[scarlet][Server][]Your request wos ignored.");

        },60);
    }
    public void handle(String desision){
        handled=true;
        switch(desision){
            case "accept":
                switchTeams(requesting,requested);
                break;
            case "deny":
                requesting.sendMessage("[scarlet][Server][]"+requested.name+" denied your request.");
            default:
                requested.sendMessage("[scarlet][Server][]Invalid argument you can only deny or accept.");
                handled=false;
        }
    }
    public static void switchTeams(Player a,Player b){
        Call.sendMessage("[scarlet][Server][]"+a.name+" and "+b.name+" switched teams.");
        Team aTeam=a.getTeam();
        Team bTeam=b.getTeam();
        Pvp.changeTeam(a,bTeam);
        Pvp.changeTeam(b,aTeam);

    }
}
