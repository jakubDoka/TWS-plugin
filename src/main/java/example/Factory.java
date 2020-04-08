package example;

import arc.util.Log;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.type.UnitType;
import org.json.simple.JSONObject;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import arc.util.Timer;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static mindustry.Vars.*;

public class Factory extends Requesting implements Requester,Interruptible, LoadSave {
    final int BUILD_LIMIT=10;
    final int BUILD_TIME=11;
    final int UNIT_COUNT=12;

    final String configFile="factoryConfig.json";

    HashMap<String,int[]> stats=new HashMap<>();
    final ArrayList<String > statKeys=new ArrayList<>();
    Loadout loadout;

    public Factory(Loadout loadout){
        super();
        this.loadout=loadout;
        for(Item item:Main.items){
            statKeys.add(item.name);
        }
        statKeys.add("build_limit");
        statKeys.add("build_time");
        config();
    }

    @Override
    public ArrayList<Request> getRequests() {
        return requests;
    }

    @Override
    public void fail(String object, int amount) {
        if(object.equals("all")){
            Call.sendMessage(Main.prefix+"Ship with all units don t have enough fuel to go back.All units are lost.");
            return;
        }
        stats.get(object)[UNIT_COUNT]+=amount;
        Call.sendMessage(Main.prefix+Main.report(object,amount)+" are going back to base.");
    }

    @Override
    public String getProgress(Request request) {
        return Main.report(request.aPackage.object,request.aPackage.amount)+" will " +
                (request.aPackage.toBase ? "arrive":"be finished")+" in "+Main.timeToString(request.time)+".\n";
    }

    @Override
    public void launch(Package p) {
        Request req;
        if(p.toBase){
            ArrayList<BaseUnit> units=new ArrayList<>();
            if (p.object.equals("all")){
                for (String name:stats.keySet()){
                    add_units(getUnitByName(name),units,p.target,-1,p.x,p.y);
                }
            }else {
                add_units(getUnitByName(p.object),units,p.target,p.amount,p.x,p.y);
            }
            req=new Request(Main.transportTime,new Timer.Task() {
                @Override
                public void run() {
                    for(BaseUnit u:units){
                        u.add();
                    }
                    Call.sendMessage(Main.prefix+Main.report(p.object,p.amount)+" units arrived.");
                }
            },this,p,true);

            world.tile(p.x,p.y).removeNet();
        }else{
            int[] thisUnitStats =stats.get(p.object);
            for (int i=0;i<Main.items.size();i++) {
                int requires = thisUnitStats[i];
                loadout.storage[i] -= requires*p.amount;
            }
            int buildTime=(int)(p.amount/(float)thisUnitStats[BUILD_LIMIT]*thisUnitStats[BUILD_TIME]*60);
            req=new Request(buildTime,new Timer.Task() {
                @Override
                public void run() {
                    stats.get(p.object)[UNIT_COUNT]+=p.amount;
                    Call.sendMessage(Main.prefix+Main.report(p.object,p.amount)+" wos finished and are waiting in a hangar.");
                }
            },this,p,false);
        }
        requests.add(req);
    }

    @Override
    public Package verify(Player player, String object, String sAmount, boolean toBase) {
        if(requests.size()==config.get(THREAD_COUNT)){
            player.sendMessage(Main.prefix+"Factory is doing maximum amount of tasks actually.");
            return null;
        }
        if(!stats.containsKey(object) && !(object.equals(Main.ALL) && toBase)){
            player.sendMessage(Main.prefix+"Factory cannot build nor send [scarlet] "+object+"[].");
            return null;
        }
        if(Main.isNotInteger(sAmount)){
            player.sendMessage(Main.prefix+"The [scarlet] "+sAmount+"[] is not a number.");
            return null;
        }
        UnitType targetUnit=getUnitByName(object);
        int amount=Integer.parseInt(sAmount);
        boolean hasEnough=true;
        if (!toBase) {
            int[] cost=stats.get(object);
            for(int i=0;i<Main.items.size();i++){
                int missing=cost[i]*amount-loadout.storage[i];
                if(missing>0){
                    hasEnough=false;
                    player.sendMessage(Main.prefix+"You are missing [scarlet]"+missing+"[]"
                            +Main.itemIcons[i]+".");
                }
            }
            if (!hasEnough){
                return null;
            }
        }else {
            int uCount=getUnitCount(object);
            if(object.equals("all")){
                amount=uCount;
            }
            if(uCount==0){
                player.sendMessage(Main.prefix+"Nothing to launch.");
                return null;
            }
            if(uCount<amount){
                player.sendMessage(Main.prefix+"There are only"+Main.report(object,uCount)+".");
                return null;
            }if(amount>config.get(MAX_TRANSPORT)){
                player.sendMessage(Main.prefix+"You can transport at most [orange]"+config.get(MAX_TRANSPORT)+
                        "[] units but you attempt to transport [scarlet]"+amount+"[] units.");
                return null;
            }
            int x = (int) player.x;
            int y = (int) player.y;
            if (world.tile(x / 8, y / 8).solid()) {
                if (  object.equals("all") || !targetUnit.flying) {
                    player.sendMessage(Main.prefix+"Land unit cant be dropped on a solid block.");
                    return null;
                }
            }
            return new Package(object,amount,true,player,x,y);
        }
        return new Package(object,amount,false,player);
    }

    public UnitType getUnitByName(String name){
        for(UnitType u:content.units()){
            if(u.name.equals(name)){
                return u;
            }
        }
        return null;
    }

    public int getUnitCount(String key){
        if(key.equals("all")){
            int res=0;
            for(String k:stats.keySet()){
                res+=getUnitCount(k);
            }
            return res;
        }
        return stats.get(key)[UNIT_COUNT];
     }

    public void add_units(UnitType unitType, ArrayList<BaseUnit> units, Player player, int amount,int x,int y){
        amount=amount==-1 ? stats.get(unitType.name)[UNIT_COUNT]:amount;
        for(int i=0;i<amount;i++){
            BaseUnit unit=unitType.create(player.getTeam());
            unit.set(x,y);
            units.add(unit);
        }

        stats.get(unitType.name)[UNIT_COUNT]-=amount;
    }

    @Override
    public void interrupt() {
        requests.forEach(Request::interrupt);
        requests.clear();
    }

    public String info() {
        StringBuilder message=new StringBuilder();
        message.append("[orange]--FACTORY INFO--[]\n\nunit/in hangar\n");
        for(String name:stats.keySet()){
            message.append(name).append("/").append(getUnitCount(name)).append("\n");
        }
        message.append("\n");
        int freeThreads=config.get(THREAD_COUNT);
        for (Request r : requests) {
            message.append(getProgress(r));
            freeThreads--;
        }
        message.append("[orange]").append(freeThreads).append("[]threads are free.\n");

        return message.toString();
    }

    @Override
    public JSONObject save() {
        JSONObject data=new JSONObject();
        for (String name:stats.keySet()){
            data.put(name,getUnitCount(name));
        }
        return data;
    }

    @Override
    public void load(JSONObject data){
        for (String name:stats.keySet()){
            if(!stats.containsKey(name)){continue;}
            Integer val=Main.getInt(data.get(name));
            if(val==null){
                Main.loadingError("Factory/save/"+name);
                continue;
            }
            stats.get(name)[UNIT_COUNT]=val;
        }
    }

    @Override
    public HashMap<String, Integer> get_config() {
        return config;
    }

    public void config(){
        String path=Main.directory+configFile;
        try(FileReader fileReader = new FileReader(path)) {
            stats.clear();
            JSONParser jsonParser=new JSONParser();
            JSONObject settings =(JSONObject) jsonParser.parse(fileReader);
            for(Object setting:settings.keySet()){
                if(getUnitByName((String)setting)==null){
                    Log.info("Unit name "+setting+" is invalid.It will be ignored.");
                    StringBuilder list=new StringBuilder();
                    for(UnitType unit:content.units()){
                        list.append(unit.name).append(" ");
                    }
                    Log.info("Valid units: "+list.toString());
                    continue;
                }
                int[] data=new int[13];
                int idx=0;
                boolean fail=false;
                JSONObject jsInfo=(JSONObject)settings.get(setting);
                for(String key:statKeys){

                    if(!jsInfo.containsKey(key)){
                        Log.info("Config loading:missing property "+key+" in "+setting+". "+setting+" will be ignored.");
                        fail=true;
                        break;
                    }
                    Integer val=Main.getInt(jsInfo.get(key));
                    if(val==null){
                        Main.loadingError("Loadout/save/"+key);
                        data[idx]=1;
                    }else {
                        data[idx]=val;
                    }
                    idx++;
                }
                if(fail){continue;}
                stats.put((String)setting,data);
            }
            Log.info(stats.size()==0 ? "Nothing to load from config file.":"Config loaded.");
        }catch (FileNotFoundException ex) {
            Log.info("No config file found.");
            createDefaultConfig();
        }catch (ParseException ex){
            Log.info("Json file is invalid");
        }catch (IOException ex){
            Log.info("Error when loading data from "+path+".");
        }
    }

    public void createDefaultConfig(){
        String path=Main.directory+configFile;
        try(FileWriter file = new FileWriter(path)) {

            JSONObject unit =new JSONObject();
            for(String key:statKeys){
                unit.put(key,5);
            }
            JSONObject config =new JSONObject();
            config.put("eruptor",unit);
            file.write(config.toJSONString());
            Log.info("Default "+path+" successfully created.Edit it and use apply-config command.");
        }catch (IOException ex){
            Log.info("Error when creating default config.");
        }
    }
}
