package theWorst;

import mindustry.entities.type.Player;
import mindustry.gen.Call;

import arc.util.Timer;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.Votable;

import java.util.ArrayList;

import static mindustry.Vars.*;

public class Vote implements Interruptible {
    Votable votable;
    Package aPackage;

    String report;
    String[] alerts = {"vote-50sec", "vote-40sec", "vote-30sec", "vote-20sec", "vote-10sec"};

    ArrayList<String> voted = new ArrayList<>();

    Timer.Task alert;
    Timer.Task task;

    boolean voting = false;

    int alertIdx = 0;
    int votes = 0;


    public void aVote(Votable votable, Package aPackage, String message, String report) {
        if (voting) {
            aPackage.target.sendMessage(Main.prefix + "Vote in process.");
            return;
        }
        if (AntiGriefer.isGriefer(aPackage.target)){
            AntiGriefer.abuse(aPackage.target);
            return;
        }
        this.votable = votable;
        this.aPackage = aPackage;
        this.report = report;
        alertIdx = 0;
        votes = 0;
        voting = true;
        alert = Timer.schedule(() -> {
            if (alertIdx == alerts.length) {
                return;
            }
            Call.sendMessage(Main.prefix + alerts[alertIdx]);
            alertIdx++;
        }, 10, 10);
        task = Timer.schedule(() -> {
            close(false);
        }, 60);

        Call.sendMessage(Main.prefix +"[orange]"+ aPackage.target.name +
                "[] started vote for " + message + " Send a message with \"y\" to agree or \"n\" to disagree.");

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

    public void addVote(Player player, int vote) {
        if (voted.contains(player.uuid)) {
            player.sendMessage(Main.prefix + "You already voted,sit down!");
            return;
        }
        if (AntiGriefer.isGriefer(player)){
            AntiGriefer.abuse(player);
            return;
        }
        votes += vote;
        int req=getRequired();
        if (votes >= req) {
            close(true);
            return;
        }else if(votes<=-req){
            close(false);
            return;
        }
        Call.sendMessage(Main.prefix + (getRequired() - votes) + " more votes needed.");
    }

    public void close(boolean success) {
        if (!voting) {
            return;
        }
        voting = false;
        task.cancel();
        alert.cancel();
        String result = Main.prefix + "vote-" + report;
        if (success) {
            result += "-done";
            votable.launch(aPackage);
        } else {
            result += "-failed";
        }
        Call.sendMessage(result);
    }

    @Override
    public void interrupt() {
        close(false);
    }
}