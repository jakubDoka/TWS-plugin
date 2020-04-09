package example;

import mindustry.entities.type.Player;
import mindustry.gen.Call;

import arc.util.Timer;

import java.util.ArrayList;

import static mindustry.Vars.*;

public class Vote implements Interruptible {
    Requester requester;
    Package aPackage;

    String report;
    String[] alerts={"vote-50sec", "vote-40sec", "vote-30sec", "vote-20sec", "vote-10sec"};

    ArrayList<String > voted=new ArrayList<>();

    Timer.Task alert;
    Timer.Task task;

    boolean voting=false;

    int alertIdx=0;
    int votes=0;

    public void aVote(Requester requester, Package aPackage, String message,String report){
        if(voting){
            aPackage.target.sendMessage(Main.prefix+"Vote in process.");
            return;
        }
        this.requester=requester;
        this.aPackage=aPackage;
        this.report=report;
        alertIdx=0;
        votes=0;
        voting=true;
        alert=Timer.schedule(()->{
            if(alertIdx==alerts.length){
                return;
            }
            Call.sendMessage(Main.prefix+alerts[alertIdx]);
            alertIdx++;
        },10,10);
        task=Timer.schedule(()->{
            close(false);
        },60);

        Call.sendMessage(Main.prefix+aPackage.target.name+
                "[] started vote for "+message+"Send a message with \"y\" to agree.");

    }

    public int getRequired(){
        int count=playerGroup.size();
        if(count==2){
            return 2;
        }
        return (int)Math.ceil(count/2.0);
    }

    public void addVote(Player player,int vote){
        if(voted.contains(player.uuid)){
            player.sendMessage(Main.prefix+"You already voted,sit down!");
            return;
        }
        votes+=vote;
        if(votes>=getRequired()){

            close(true);
            return;
        }
        Call.sendMessage(Main.prefix+(votes-getRequired())+" more votes needed.");
    }

    public void close(boolean success){
        if(!voting){
            return;
        }
        voting=false;
        task.cancel();
        alert.cancel();
        String result=Main.prefix+"vote-"+report;
        if(success){
            result+="-done";
            requester.launch(aPackage);
        }else {
            result+="-failed";
        }
        Call.sendMessage(result);
    }

    @Override
    public void interrupt() {
        close(false);
    }
}
