package theWorst;

import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.Votable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static mindustry.Vars.playerGroup;

public class Vote implements Interruptible {
    Votable votable;
    Package aPackage;

    String message;

    Set<String> voted = new HashSet<>();
    HashMap<String,Integer> recent = new HashMap<>();

    Timer.Task alert;
    Timer.Task task;

    boolean voting = false;

    int voteCooldown =60;
    int no, yes;
    int time;


    public void aVote(Votable votable, Package aPackage, String message) {
        Player requester=aPackage.target;
        if (voting) {
            requester.sendMessage(Main.prefix + "Vote in process.");
            return;
        }
        if ( AntiGriefer.isGriefer(requester)){
            AntiGriefer.abuse(requester);
            return;
        }
        if(isRecent(requester)){
            requester.sendMessage(Main.prefix+"Your last vote failed,to prevent spam you have to wait "
                    +Main.timeToString(recent.get(requester.uuid))+".");
            return;
        }
        this.votable = votable;
        this.aPackage = aPackage;
        this.message = message;
        restart();
        Call.sendMessage(Main.prefix +"[orange]"+  requester.name +
                "[] started vote for " + message + ". Send a message with \"y\" to agree or \"n\" to disagree.");
    }

    public void restart(){
        voted.clear();
        yes=0;
        no=0;
        time=voteCooldown;
        voting = true;
        alert = Timer.schedule(() -> {
            time--;
            if (time == 0) {
                alert.cancel();
            }
        }, 0, 1);
        task = Timer.schedule(() -> close(false), voteCooldown);
    }

    private boolean isRecent(Player player) {
        return recent.containsKey(player.uuid);
    }

    public void addToRecent(Player player){
        recent.put(player.uuid, voteCooldown);
        Timer.schedule(new Timer.Task(){
            @Override
            public void run() {
                int time=recent.get(player.uuid);
                if(time==0){
                    recent.remove(player.uuid);
                    this.cancel();
                    return;
                }
                recent.put(player.uuid,time-1);
            }
        },0,1);

    }

    public int getRequired() {
        int count = 0;
        for(Player p:playerGroup){
            if(AntiGriefer.isGriefer(p)) continue;
            count+=1;
        }
        if (count == 2) {
            return 2;
        }
        return (int) Math.ceil(count / 2.0);
    }

    public void addVote(Player player, String vote) {
        if (voted.contains(player.uuid)) {
            player.sendMessage(Main.prefix + "You already voted,sit down!");
            return;
        }

        voted.add(player.uuid);
        if (AntiGriefer.isGriefer(player)){
            AntiGriefer.abuse(player);
            return;
        }
        int req=getRequired();
        if (vote.equals("y")) {
            yes += 1;
            if (yes >= req) {
                close(true);
                return;
            }
        } else {
            no += 1;
            if (no >= req) {
                close(false);
                return;
            }
        }
        Call.sendMessage(Main.prefix + (getRequired() -yes) + " more votes needed.");
    }

    public void close(boolean success) {
        if (!voting) {
            return;
        }
        voting = false;
        task.cancel();
        alert.cancel();
        String result = Main.prefix + "vote-" + message;
        if (success) {
            result += "-done";
                votable.launch(aPackage);
        } else {
            result += "-failed";
            addToRecent(aPackage.target);
        }
        Call.sendMessage(result);
    }

    @Override
    public void interrupt() {
        close(false);
    }

    @Override
    public String getHudInfo() {
        if(!voting) return null;
        return "vote for "+message+" "+String.format("%02d",time)+"s [green]"+yes+" [scarlet]"+no;
    }
}
