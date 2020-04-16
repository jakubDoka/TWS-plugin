package theWorst;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.base.BuilderDrone;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemType;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import theWorst.helpers.CoreBuilder;
import theWorst.helpers.MapChanger;
import theWorst.helpers.WaveSkipper;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.LoadSave;
import theWorst.requests.Factory;
import theWorst.requests.Loadout;
import theWorst.requests.Request;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static mindustry.Vars.*;

public class Main extends Plugin{
    public static final String ALL="all";
    public static final String saveFile="save.json";
    public static final String directory="config/mods/The_Worst/";
    public static final String prefix="[scarlet][Server][]";

    public static final String[] itemIcons={"\uF838","\uF837","\uF836","\uF835","\uF832","\uF831","\uF82F","\uF82E","\uF82D","\uF82C"};
    public static final HashMap<String , LoadSave> saveConfigReq=new HashMap<>();

    public static int transportTime=30;

    public static ArrayList<Item> items=new ArrayList<>();
    ArrayList<Interruptible> interruptibles=new ArrayList<>();

    Loadout loadout=new Loadout();
    Factory factory;
    CoreBuilder builder=new CoreBuilder();
    MapChanger changer=new MapChanger();
    WaveSkipper skipper=new WaveSkipper();
    Vote vote=new Vote();


    public Main(){

        Events.on(PlayerChatEvent.class,e->{
            if(vote.voting && e.message.equals("y")){
                vote.addVote(e.player,1);
            }
                });

        Events.on(WorldLoadEvent.class,e-> interruptibles.forEach(Interruptible::interrupt));

        Events.on(EventType.BuildSelectEvent.class, e->{
            ArrayList<Request> requests=factory.getRequests();
            if(requests.size()>0) {
                boolean canPlace=true;
                for(Request r:requests){
                    double dist=sqrt((pow(e.tile.x-(float)(r.aPackage.x/8),2)+
                            pow(e.tile.y-(float)(r.aPackage.y/8),2)));
                    if (dist<5){
                        canPlace=false;
                        break;
                    }
                }
                if(!canPlace){
                    e.tile.removeNet();
                    if(e.builder instanceof BuilderDrone){
                        ((BuilderDrone)e.builder).kill();
                        Call.sendMessage(prefix+"Builder Drone wos destroyed after it attempt to build on drop point");
                    }else if(e.builder instanceof Player){
                        ((Player)e.builder).sendMessage(prefix+"You cannot build on unit drop point.");
                    }
                }
            }
        });

        Events.on(ServerLoadEvent.class,e->{
            load_items();
            factory=new Factory(loadout);
            interruptibles.add(loadout);
            interruptibles.add(factory);
            interruptibles.add(vote);
            saveConfigReq.put("loadout",loadout);
            saveConfigReq.put("factory",factory);
            if(!makeDir()){
                Log.info("Unable to create directory "+directory+".");
            }

            load();
        });

    }

    public void load(){
        String path=directory+saveFile;
        try(FileReader fileReader = new FileReader(path)) {
            JSONParser jsonParser=new JSONParser();
            Object obj=jsonParser.parse(fileReader);
            JSONObject saveData=(JSONObject)obj;
            for(String r:saveConfigReq.keySet()){
                if(!saveData.containsKey(r)){
                    Log.info("Failed to load save file.");
                    return;
                }
            }
            saveConfigReq.keySet().forEach((k)->saveConfigReq.get(k).load((JSONObject) saveData.get(k)));
            fileReader.close();
            Log.info("Data loaded.");
        }catch (FileNotFoundException ex) {
            Log.info("No saves found.New save file " + path + " will be created.");
            save();
        }catch (ParseException ex){
            Log.info("Json file is invalid.");
        }catch (IOException ex){
            Log.info("Error when loading data from "+path+".");
        }
    }

    public void save(){
        JSONObject saveData=new JSONObject();
        saveConfigReq.keySet().forEach((k)->saveData.put(k,saveConfigReq.get(k).save()));
        try(FileWriter file = new FileWriter(directory+saveFile))
        {
            file.write(saveData.toJSONString());
            file.close();
            Log.info("Data saved.");
        }catch (IOException ex){
            Log.info("Error when saving data.");
        }
    }

    public static boolean isNotInteger(String str){
        if(str == null || str.trim().isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if(!Character.isDigit(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String report(String object,int amount){
        return "[orange]" + (object.equals(Main.ALL) ? Main.ALL:amount+" "+object) +"[]";
    }

    public static String timeToString(int time){
        return time/60+"sec"+time%60+"sec";
    }

    private boolean makeDir(){
        File dir=new File(directory);
        if(!dir.exists()){
            return dir.mkdir();
        }
        return true;
    }

    public static void loadingError(String address){
        Log.err(address+" has invalid type of value.It has to be integer." +
                "Property will be set to default value.");
    }

    public static void missingPropertyError(String address){
        Log.err(address+" is missing.Property will be set to default value.");
    }

    public static boolean isInvalidArg(Player player,String what,String agr){
        if(isNotInteger(agr)){
            player.sendMessage(prefix+what+" has to be integer.[scarlet]"+ agr +"[] is not.");
            return true;
        }
        return false;
    }

    public static Integer getInt(Object integer){
        if(integer instanceof Integer){
            return (int)integer;
        }if( integer instanceof Long){
            return ((Long)integer).intValue();
        }
        return null;
    }

    public static Player findPlayer(String name){
        return playerGroup.find(p->p.name.equals(name));
    }

    public static Team getTeamByName(String name){
        for(Team t:Team.all()){
            if(t.name.equals(name)){
                return t;
            }
        }
        return null;
    }

    private void load_items(){
        for(Item i:content.items()){
            if(i.type==ItemType.material){
                items.add(i);
            }
        }
    }

    public static int getStorageSize(Player player){
        int size=0;
       for(CoreBlock.CoreEntity c:player.getTeam().cores()){
           size+=c.block.itemCapacity;
       }
       return size;
    }
    
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("w-load","Reloads theWorst saved data.",arg-> load());

        handler.register("w-save","Saves theWorst data.",arg-> save());

        handler.register("spawn", "<mob_name> <count> <playerName> [team] ", "Spawn mob in player position.", arg -> {
            if(playerGroup.size()==0){
                Log.info("there is no one logged,why bother spawning units?");
            }
            UnitType unitType=Factory.getUnitByName(arg[0]);
            if(unitType==null){
                Log.info(arg[0]+" is not valid unit.");
                return;
            }
            if(isNotInteger(arg[1])){
                Log.info("count has to be integer.");
                return;
            }
            int count=Integer.parseInt(arg[1]);
            Player player=findPlayer(arg[2]);
            if(player==null){
                Log.info("Player not found.");
                return;
            }
            Team team=arg.length>3 ? getTeamByName( arg[3]):Team.crux;
            for(int i=0;i<count;i++){
                BaseUnit unit=unitType.create(team);
                unit.set(player.x,player.y);
                unit.add();
            }
        });

        handler.register("w-apply-config","Applies the factory configuration.",arg->{
            factory.config();
            Log.info("Config applied.");
        });

        handler.register("w-trans-time","<value>","Sets transport time.",arg->{
            if(isNotInteger(arg[0])){
                Log.info(arg[0]+" is not an integer.");
                return;
            }
            transportTime=Integer.parseInt(arg[0]);
                });

        handler.register("w","<target> <property> <value>","Sets property of target to value/integer.",arg->{
            if(!saveConfigReq.containsKey(arg[0])){
                Log.info("Invalid target.Valid targets:"+saveConfigReq.keySet().toString());
                return;
            }
            HashMap<String ,Integer> config=saveConfigReq.get(arg[0]).get_config();
            if(!config.containsKey(arg[1])){
                Log.info(arg[0]+" has no property "+arg[1]+". Valid properties:"+config.keySet().toString());
                return;
            }
            if(isNotInteger(arg[2])){
                Log.info(arg[2]+" is not an integer.");
                return;
            }
            config.put(arg[1],Integer.parseInt(arg[2]));
            Log.info("Property changed.");
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.removeCommand("vote");

        handler.<Player>register("l-info","Shows how mani resource you have stored in the loadout and " +
                        "traveling progress.",(arg, player) -> Call.onInfoMessage(player.con,loadout.info()));

        handler.<Player>register("f-info","Shows how mani units you have in a hangar and building or " +
                "traveling progres.",(arg, player) -> Call.onInfoMessage(player.con,factory.info()));

        handler.<Player>register("l","<fill/use> <itemName/all> <itemAmount>","."
                ,(arg, player) -> {
            if(isInvalidArg(player,"Item amount",arg[2])) return;
            boolean use=arg[0].equals("use");
            Package p=loadout.verify(player,arg[1],arg[2],use);
            if (p==null){
                return;
            }
            String where=use ? "core":"loadout";
            vote.aVote(loadout, p,"launch "+report(p.object,p.amount)+" to "+where+".",
                    "launch to "+where);
        });

        handler.<Player>register("f","<build/send> <unitName/all> [unitAmount]","."
                ,(arg, player) -> {
            if(isInvalidArg(player,"Unit amount",arg[2])) return;
            boolean send=arg[0].equals("send");
            Package p=factory.verify(player,arg[1],arg.length==3 ? arg[2]:"1" ,send);
            if (p==null){
                return;
            }
            String what=send ? "send":"build";
            vote.aVote(factory, p,what+" "+report(p.object,p.amount)+".", what);
        });

        handler.<Player>register("f-price","<unitName> [unitAmount]",
                "Shows price of given amount of units.",(arg,player)->{
            int amount=arg.length==1 || isNotInteger(arg[1]) ? 1:Integer.parseInt(arg[1]);
            Call.onInfoMessage(player.con,factory.price(player,arg[0],amount));
        });

        handler.<Player>register("build-core","<small/normal/big>", "Makes new core", (arg, player) -> {
            Package p=builder.verify(player,arg[0],null ,true);
            if (p==null){
                return;
            }
            vote.aVote(builder, p,"building "+arg[0]+" core.","core build");
        });

        handler.<Player>register("vote","<map/skipwave> [index/name/waveAmount]","",(arg, player) -> {
            Package p = null;
            String secArg=arg.length==2 ? arg[1]:"0";
            switch (arg[0]){
                case "map":
                    p = changer.verify(player,secArg,null,false);
                    if(p==null) return;
                    vote.aVote(changer,p,"changing map to "+p.map.name()+". ","map change");
                    return;
                case "skipwave":
                    if(isInvalidArg(player,"Wave amount",secArg)) return;
                    p= skipper.verify(player,null,secArg,false);
                    vote.aVote(skipper,p,"skipping "+p.amount+" waves. ","wave skip");
                    return;
                default:
                    player.sendMessage(prefix+"Invalid first argument.");
            }
        });
    }
}
