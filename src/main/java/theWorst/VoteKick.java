package theWorst;

import arc.struct.Array;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Packets;
import theWorst.dataBase.DataBase;
import theWorst.dataBase.Perm;

import static mindustry.Vars.playerGroup;

public class VoteKick{
    int kickDuration=60*60;
    Player target;
    Array<String> voted = new Array<>();
    Timer.Task task;
    int votes;
    boolean voting=false;

    public void aVoteKick(String arg,Player player){
        Player found;
        if(arg.length() > 1 && arg.startsWith("#") && Strings.canParseInt(arg.substring(1))){
            int id = Strings.parseInt(arg.substring(1));
            found = playerGroup.find(p -> p.id == id);
        }else{
            found = playerGroup.find(p -> p.name.equalsIgnoreCase(arg));
        }
        if(found == null){
            found=Main.findPlayer(arg);
            if(found == null) {
                player.sendMessage(Main.prefix+"No player[orange]'" + arg + "'[] found.");
                return;
            }
        }

        if(AntiGriefer.verifyTarget(found,player,"kick")) return;

        votes=0;
        this.target = found;
        vote(player, 1);
        this.task = Timer.schedule(() -> {
            if(!checkPass()){
                Call.sendMessage(Strings.format("[lightgray]Vote failed. Not enough votes to kick[orange] {0}[lightgray].", target.name));
                task.cancel();
                voting=false;
            }
        }, 60);
    }

    void vote(Player player, int d){
        if(voted.contains(player.uuid)){
            player.sendMessage(Main.prefix+"You already voted,sit down.");
            return;
        }
        if(player==target){
            player.sendMessage(Main.prefix+"You cannot vote on your own trial.");
        }
        votes += d;
        voted.add(player.uuid);
        if(!checkPass()){
            Call.sendMessage(Strings.format("[orange]{0}[lightgray] has voted on kicking[orange] {1}[].[accent] ({2}/{3})\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
                    player.name, target.name, votes, Vote.getRequired()));
        }

    }

    boolean checkPass(){
        if(votes >= Vote.getRequired()){
            Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] {0}[orange] will be banned from the server for {1} minutes.", target.name, (kickDuration/60)));
            target.getInfo().lastKicked = Time.millis() + kickDuration*1000;
            playerGroup.all().each(p -> p.uuid != null && p.uuid.equals(target.uuid), p -> p.con.kick(Packets.KickReason.vote));
            task.cancel();
            return true;
        }
        return false;
    }
}
