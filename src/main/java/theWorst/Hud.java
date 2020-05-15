package theWorst;

import arc.Events;
import arc.struct.Array;
import arc.util.Log;
import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.gen.Call;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import theWorst.dataBase.Database;
import theWorst.dataBase.Setting;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.LoadSave;

import static mindustry.Vars.playerGroup;

public class Hud implements LoadSave {
    public static Timer.Task update;
    Timer.Task messageCycle;
    Timer.Task coreDamageAlert;
    boolean coreDamaged=false;
    boolean alertIsRed=false;
    public Array<String> messages= new Array<>();
    int speed=10;
    static Array<Ad> adQueue= new Array<>();
    int current=0;


    public Hud(){
        Events.on(EventType.Trigger.teamCoreDamage,()->{
            int alertDuration=10;
            if(coreDamageAlert!=null){
                return;
            }
            coreDamaged=true;
            coreDamageAlert=Timer.schedule(()->{
                coreDamaged=false;
                coreDamageAlert=null;
            },alertDuration);
        });
    }

    void startCycle(int speed){
        this.speed=speed;
        messageCycle=Timer.schedule(()->{
            if(messages.isEmpty()){
                return;
            }

            current+=1;
            current%=messages.size;
        },0,speed*60);
    }

    void update(){
        update=Timer.schedule(()-> {
            try {
                StringBuilder b = new StringBuilder();
                for (Interruptible i : Main.interruptibles) {
                    String msg = i.getHudInfo();
                    if (msg == null) continue;
                    b.append(msg).append("\n");
                }
                if(!messages.isEmpty()){
                    b.append(messages.get(current)).append("\n");
                }
                if(coreDamaged){
                    alertIsRed=!alertIsRed;
                    b.append(alertIsRed ? "[scarlet]" : "[gray]").append("!!CORE UNDER ATTACK!![]\n");
                }
                for(Ad ad:adQueue){
                    b.append(ad.getMessage()).append("\n");
                }
                for (Player p : playerGroup.all()) {
                    if (Database.hasEnabled(p, Setting.hud)) {
                        Call.setHudText(p.con, b.toString().substring(0, b.length() - 1));
                    } else {
                        Call.hideHudText(p.con);
                    }
                }

            }catch (Exception ex){
                Log.info("something horrible happen");
                ex.printStackTrace();
            }
        },0,1);
    }

    static class Ad{
       Timer.Task expiration;
       private final String message;
       String[] colors=null;
       int idx=0;

        Ad(String message,int liveTime){
            this.message=message;
            expiration = Timer.schedule(()->{
                adQueue.remove(this);
            },liveTime);
        }

       Ad(String message,int liveTime,String[] colors){
           this.colors=colors;
           this.message=message;
           expiration = Timer.schedule(()->{
               adQueue.remove(this);
           },liveTime);
       }

       String getMessage(){
           String currentColor;
           if (colors==null){
               currentColor="white";
           }else {
               currentColor=colors[idx];
               idx++;
               idx%=colors.length;
           }
           return "["+currentColor+"]"+message+"[]";
       }
    }

    public static void addAd(String  message,int time,String[] colors){
        adQueue.add(new Ad(message,time,colors));
    }

    public static void addAd(String  message,int time){
        adQueue.add(new Ad(message,time));
    }

    @Override
    public void load(JSONObject data) {
        for(Object o:(JSONArray)data.get("messages")){
            messages.add((String)o);
        }
    }

    @Override
    public JSONObject save() {
        JSONObject data=new JSONObject();
        JSONArray messages=new JSONArray();
        for(String s:this.messages){
            messages.add(s);
        }
        data.put("messages",messages);
        return data;
    }


}
