package theWorst;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Array;
import arc.struct.ArrayMap;
import arc.util.*;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.base.BuilderDrone;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.ServerLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.type.UnitType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import theWorst.dataBase.*;
import theWorst.helpers.CoreBuilder;
import theWorst.helpers.MapManager;
import theWorst.helpers.Tester;
import theWorst.helpers.WaveSkipper;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.LoadSave;
import theWorst.interfaces.runLoad;
import theWorst.interfaces.runSave;
import theWorst.requests.Factory;
import theWorst.requests.Loadout;
import theWorst.requests.Request;
import theWorst.requests.Requester;

import java.io.*;
import java.util.Arrays;

import static java.lang.Math.*;
import static mindustry.Vars.*;

public class Main extends Plugin {
    //TOdo test vote - there are new visual cnages - let vote fail
    //TODO test action manager - try interact with staff that verified built as griefer or newcomer
    //TODO test ban - bun person via player list, he should be able to rejoin but as griefer only
    //TODO test tester - just test if griefer cannot do test
    //TODO test vote kick - just make sure it works properly

    static final String configFile ="settings.json";
    static final String saveFile = "save.json";
    public static final String directory = "config/mods/The_Worst/";
    public static final String prefix = "[coral][[[scarlet]Server[]]:[]";
    public static final String noPerm = prefix+"You have no permission to do this. Please submit your appeal in discord";

    public static final String[] itemIcons = {"\uF838", "\uF837", "\uF836", "\uF835", "\uF832", "\uF831", "\uF82F", "\uF82E", "\uF82D", "\uF82C"};
    ArrayMap<String, LoadSave> loadSave = new ArrayMap<>();
    public static ArrayMap<String, Requester> configured = new ArrayMap<>();

    public static int transportTime = 3 * 60;

    public static Array<Item> items = new Array<>();
    public final int pageSize=15;
    static Array<Interruptible> interruptibles = new Array<>();

    Timer.Task autoSaveThread;

    int defaultAutoSaveFrequency=5;

    MapManager mapManager;
    Factory factory;
    ServerPlayer serverPlayer;

    ActionManager actionManager =new ActionManager();
    Loadout loadout = new Loadout();
    CoreBuilder builder = new CoreBuilder();
    WaveSkipper skipper = new WaveSkipper();
    Hud hud = new Hud();
    Database dataBase=new Database();
    Tester tester =new Tester();
    Vote vote = new Vote();
    VoteKick voteKick=new VoteKick();

    public Main() {

        Events.on(WorldLoadEvent.class, e -> interruptibles.forEach(Interruptible::interrupt));


        Events.on(BuildSelectEvent.class, e -> {
            Array<Request> requests = factory.getRequests();
            if (requests.size > 0) {
                boolean canPlace = true;
                for (Request r : requests) {
                    double dist = sqrt((pow(e.tile.x - (float) (r.aPackage.x / 8), 2) +
                            pow(e.tile.y - (float) (r.aPackage.y / 8), 2)));
                    if (dist < 5) {
                        canPlace = false;
                        break;
                    }
                }
                if (!canPlace) {
                    e.tile.removeNet();
                    if (e.builder instanceof BuilderDrone) {
                        ((BuilderDrone) e.builder).kill();
                        Call.sendMessage(prefix + "Builder Drone wos destroyed after it attempt to build on drop point");
                    } else if (e.builder instanceof Player) {
                        ((Player) e.builder).sendMessage(prefix + "You cannot build on unit drop point.");
                    }
                }
            }
        });

        Events.on(ServerLoadEvent.class, e -> {
            netServer.admins.addChatFilter((player,message)->{
                PlayerData pd=Database.getData(player);
                String msgColor="["+pd.textColor+"]";
                pd.lastAction= Time.millis();
                if(pd.rank==Rank.AFK){
                    Database.afkThread.run();
                }
                if(pd.trueRank==Rank.griefer){
                    if(Time.timeSinceMillis(pd.lastMessage)<10000L){
                        player.sendMessage(noPerm);
                        return null;
                    }
                    msgColor="[pink]";
                }else if((message.equals("y") || message.equals("n")) && vote.voting){
                    vote.addVote(player,message);
                    return null;
                }
                pd.lastMessage=Time.millis();
                pd.messageCount++;
                Database.updateRank(player,Stat.messageCount);
                return msgColor+message;
            });
            load_items();
            serverPlayer = new ServerPlayer();
            mapManager = new MapManager();
            mapManager.cleanup();
            if (!makeDir()) {
                Log.info("Unable to create directory " + directory + ".");
                return;
            }
            factory = new Factory(loadout);
            addToGroups();
            load();
            config();
            tester.loadQuestions();
            dataBase.loadRanks();
            autoSave(defaultAutoSaveFrequency);
            hud.update();
            hud.startCycle(10);
        });
    }

    public static void loadJson(String filename, runLoad load, Runnable save){
        try (FileReader fileReader = new FileReader(filename)) {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(fileReader);
            JSONObject saveData = (JSONObject) obj;
            load.run(saveData);
            fileReader.close();
            Log.info("Data from "+filename+" loaded.");
        } catch (FileNotFoundException ex) {
            Log.info("No "+filename+" found.Default one wos created.");
            save.run();
        } catch (ParseException ex) {
            Log.info("Json file "+filename+" is invalid.");
        } catch (IOException ex) {
            Log.info("Error when loading data from " + filename + ".");
        }
    }

    public static void saveJson(String filename,String success, runSave save){

        try (FileWriter file = new FileWriter(filename)) {
            file.write(save.run().toJSONString());
            file.close();
            Log.info(success==null ? "Default "+ filename+ " created" : success);
        } catch (IOException ex) {
            Log.info("Error when creating/updating "+filename+".");
        }
    }

    private void addToGroups(){
        interruptibles.add(loadout);
        interruptibles.add(factory);
        interruptibles.add(vote);
        interruptibles.add(actionManager);
        loadSave.put("loadout", loadout);
        loadSave.put("factory", factory);
        loadSave.put("Database",dataBase);
        loadSave.put("hud",hud);
        configured.put("loadout", loadout);
        configured.put("factory", factory);
    }

    public static String milsToTime(long mils){
        long sec=mils/1000;
        long min=sec/60;
        long hour=min/60;
        long days=hour/24;
        return String.format("%d:%02d:%02d:%02d",
                days%365,hour%24,min%60,sec%60);
    }

    public void load() {
        dataBase.loadData();
        mapManager.load();
        String path = directory + saveFile;
        loadJson(path,(data)->{
            for (String k : loadSave.keys()) {
                if (data.containsKey(k) && loadSave.containsKey(k)) {
                    loadSave.get(k).load((JSONObject) data.get(k));
                }
            }
        },this::save);
    }

    public void save() {
        saveJson(directory+saveFile,"Save updated.",()->{
            JSONObject saveData = new JSONObject();
            loadSave.keys().forEach((k) -> saveData.put(k, loadSave.get(k).save()));
            return saveData;
        });
        dataBase.saveData();
        mapManager.save();
    }

    public void config(){
        String path = directory + configFile;
        loadJson(path,(data)->{
            JSONObject content=(JSONObject)data.get("loadout");
            for(Object o:content.keySet()){
                loadout.getConfig().put((String)o,getInt(content.get(o)));
            }
            content=(JSONObject)data.get("factory");
            for(Object o:content.keySet()){
                factory.getConfig().put((String)o,getInt(content.get(o)));
            }
            transportTime=getInt(data.get("transTime"));
        },this::createDefaultConfig);
    }

    private void createDefaultConfig() {
        saveJson(directory+configFile,null,()->{
            JSONObject saveData = new JSONObject();
            JSONObject load = new JSONObject();
            for(String o:loadout.getConfig().keys()){
                load.put(o,getInt(loadout.getConfig().get(o)));
            }
            saveData.put("loadout",load);
            load =new JSONObject();
            for(String o:factory.getConfig().keys()){
                load.put(o,getInt(factory.getConfig().get(o)));
            }
            saveData.put("factory",load);
            saveData.put("transTime",180);
            return saveData;
        });
    }

    private void autoSave(int interval){
        if (autoSaveThread != null){
            autoSaveThread.cancel();
        }
        autoSaveThread = Timer.schedule(this::save,0,interval*60);
        Log.info("Autosave started.It will save every "+ interval +"min.");
    }

    public String formPage(Array<String > data,int page,String title,int pageSize){
        StringBuilder b=new StringBuilder();
        int pageCount=(int)Math.ceil(data.size/(float)pageSize);
        page=Mathf.clamp(page,1,pageCount)-1;
        int start=page*pageSize;
        int end=min(data.size,(page+1)*pageSize);
        b.append("[orange]--").append(title.toUpperCase()).append("(").append(page + 1).append("/");
        b.append(pageCount).append(")--[]\n\n");
        for(int i=start;i<end;i++){
            b.append(data.get(i)).append("\n");
        }
        return b.toString();
    }

    public static boolean isNotInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String report(String object, int amount) {
        return "[orange]" + (object.equals("all") ? "all" : amount + " " + object) + "[]";
    }



    private boolean makeDir() {
        File dir = new File(directory);
        if (!dir.exists()) {
            return dir.mkdir();
        }
        return true;
    }

    public static void loadingError(String address) {
        Log.err(address + " has invalid type of value.It has to be integer." +
                "Property will be set to default value.");
    }

    public void invalidArg(Player player, String arg){
        player.sendMessage(prefix + "Invalid argument [scarlet]"+arg+"[], make sure you pick one of the options.");
    }

    public static Integer processArg(Player player, String what, String arg) {
        if (isNotInteger(arg)) {
            if(player==null){
                Log.info(what + " has to be integer.[scarlet]" + arg + "[] is not.");
            }else {
                player.sendMessage(prefix + what + " has to be integer.[scarlet]" + arg + "[] is not.");
            }
            return null;
        }
        return Integer.parseInt(arg);
    }

    public boolean notEnoughArgs(Player p,int amount,String[] args ){
        if(args.length<amount){
            String m="Not enough arguments, expected at least "+amount+".";
            if(p!=null){
                p.sendMessage(prefix+m);
            }else{
                Log.info(m);
            }
            return true;
        }
        return false;
    }

    public static Integer getInt(Object integer) {
        if (integer instanceof Integer) {
            return (int) integer;
        }
        if (integer instanceof Long) {
            return ((Long) integer).intValue();
        }
        return null;
    }

    public static String getPlayerList(){
        StringBuilder builder = new StringBuilder();
        builder.append("[orange]Players to kick: \n");
        for(Player p : playerGroup.all()){
            if(p.isAdmin || p.con == null || p == player || Database.hasPerm(p,Perm.higher)) continue;

            builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id).append(")\n");
        }
        return builder.toString();
    }

    public static Player findPlayer(String name) {
       for(Player p:playerGroup){
           String pName=cleanName(p.name);
           if(pName.equalsIgnoreCase(name)){
               return p;
           }
       }
       return null;
    }

    public static String cleanName(String name){
        return cleanName(name,true);
    }

    public static String cleanName(String name, boolean withRank){
        while (name.contains("[")){
            int first=name.indexOf("["),last=name.indexOf("]");
            name=name.substring(0,first)+name.substring(last+1);
        }
        if(withRank){
            while (name.contains("<")){
                int first=name.indexOf("<"),last=name.indexOf(">");
                name=name.substring(0,first)+name.substring(last+1);
            }
            name=name.replace(" ","_");
        }

        return name;
    }

    public Team getTeamByName(String name) {
        for (Team t : Team.all()) {
            if (t.name.equals(name)) {
                return t;
            }
        }
        return null;
    }

    private void load_items() {
        for (Item i : content.items()) {
            if (i.type == ItemType.material) {
                items.add(i);
            }
        }
    }

    public static String toString(Array<String> struct){
        StringBuilder b=new StringBuilder();
        for(String s :struct){
            b.append(s).append(" ");
        }
        return b.toString();
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("say");
        handler.removeCommand("admin");

        handler.register("test", "", arg -> {
            Hud.addAd("brah",10);
            Log.info("brah");
        });

        handler.register("w-help","<ranks/factory/hud>","Shows better explanation and more information" +
                "acout entered topic.",arg->{
            switch (arg[0]){
                case "ranks":
                    SpecialRank.help();
                case "factory":
                    Log.info("Missing.");
                case "hud":
                    Log.info("Missing.");
            }
        });

        handler.register("w-hud","[speed/remove/add] [timeInMin/idx/all/message...]","Set speed of " +
                "message cycle, remove messages by index and add messages to cycle. Use /hud to see messages you added."
                ,arg->{
            if(arg.length==0){

                StringBuilder b=new StringBuilder();
                for(int i=0;i<hud.messages.size;i++){
                    b.append(i).append("-").append(hud.messages.get(i)).append("\n");
                }
                Log.info("Message will change every "+hud.speed+"min, messages are:\n"+b.toString()+"");
                return;
            }
            if(notEnoughArgs(null,2,arg)) return;
            switch (arg[0]){
                case "speed":
                    if(!Strings.canParseInt(arg[1])){
                        Log.info("Speed has to be integer.");
                        return;
                    }
                    hud.startCycle(Integer.parseInt(arg[1]));
                    Log.info("Speed changed.");
                    return;
                case "remove":
                    if(arg[1].equals("all")){
                        hud.messages.clear();
                        Log.info("All erased.");
                    }
                    if(!Strings.canParseInt(arg[1])){
                        Log.info("Index has to be integer.");
                        return;
                    }
                    int val=Integer.parseInt(arg[1]);

                    if(hud.messages.isEmpty()){
                        Log.info("No messages to erase.");
                        return;
                    }

                    if(val>=hud.messages.size){
                        Log.info("Too height number, max is "+hud.messages.size+".");
                        return;
                    }

                    Log.info("Message \""+hud.messages.remove(val)+"\" wos erased.");
                    hud.messageCycle.run();
                    return;
                case "add":
                    StringBuilder b=new StringBuilder();
                    for(int i=1;i<arg.length;i++){
                        b.append(arg[i]);
                    }
                    hud.messages.add(b.toString());
                    Log.info("Message \""+ b.toString()+"\" wos added.");
            }
        });

        handler.register("say","<text...>", "send message to all players.", arg -> {
            StringBuilder b=new StringBuilder();
            for(String s:arg){
                b.append(s).append(" ");
            }
            Call.sendMessage(prefix+b.toString());
        });

        handler.register("w-load", "Reloads theWorst saved data.", arg -> load());

        handler.register("w-save", "Saves theWorst data.", arg -> save());

        handler.register("w-unkick", "<ID/uuid>", "Erases kick status of player player.", arg -> {
            PlayerData pd = Database.findData(arg[0]);
            if (pd == null) {
                Log.info("Player not found.");
                return;
            }
            pd.getInfo().lastKicked = Time.millis();
            Log.info(pd.originalName + " is not kicked anymore.");
        });

        handler.register("w-database", "[search/online]", "Shows database,list of all players that " +
                "ewer been on server.Use search as in browser.", arg ->{
            Array<String> data;
            if(arg.length==1){
                data=arg[0].equals("online") ? Database.getOnlinePlayersIndexes() :Database.getAllPlayersIndexes(arg[0]);
            }else {
                data=Database.getAllPlayersIndexes(null);
            }
            for(String s:data){
                Log.info(cleanName(s));
            }
                });

        handler.register("w-set-rank", "<uuid/name/index> <rank/restart>", "", arg -> {
            PlayerData pd = Database.findData(arg[0]);
            if (pd == null) {
                Log.info("Player not found.");
                return;
            }
            try {
                Database.setRank(pd, Rank.valueOf(arg[1]));
                Log.info("Rank of player " + pd.originalName + " is now " + pd.rank.name() + ".");
                Call.sendMessage("Rank of player [orange]"+ pd.originalName+"[] is now " +pd.rank.getName() +".");
            } catch (IllegalArgumentException e) {
                if(arg[1].equals("restart")) {
                    pd.specialRank = null;
                    Call.sendMessage(prefix+"Rank of player [orange]" + pd.originalName + "[] wos restarted.");
                    Log.info("Rank of player " + pd.originalName + " wos restarted.");
                }else if(!Database.ranks.containsKey(arg[1])){
                    Log.info("Rank not found.\nRanks:" + Arrays.toString(Rank.values())+"\n" +
                            "Custom ranks:"+Database.ranks.keySet());
                    return;
                }else {
                    pd.specialRank=arg[1];
                    Log.info("Rank of player " + pd.originalName + " is now " + pd.specialRank + ".");
                    SpecialRank sr=Database.getSpecialRank(pd);
                    if(sr!=null){
                        Call.sendMessage(prefix+"Rank of player [orange]" + pd.originalName + "[] is now " + sr.getSuffix() + ".");
                    }
                }
            }
            Player player = findPlayer(arg[0]);
            if (player == null) {
                player = playerGroup.find(p-> p.uuid.equalsIgnoreCase(arg[0]));
                if (player == null) {
                    return;
                }
            }
           Database.updateName(player,pd);
        });

        handler.register("w-info", "<uuid/name/index>", "Displays info about player.", arg -> {
            PlayerData pd = Database.findData(arg[0]);
            if (pd == null) {
                Log.info("Player not found. Search by name applies only on online players.");
                return;
            }
            Log.info(pd.toString());
        });

        handler.register("w-spawn", "<mob_name> <count> <playerName> [team] ", "Spawn mob in player position.", arg -> {
            if (playerGroup.size() == 0) {
                Log.info("There is no one logged, why bother spawning units?");
            }
            UnitType unitType = Factory.getUnitByName(arg[0]);
            if (unitType == null) {
                Log.info(arg[0] + " is not valid unit.");
                return;
            }
            if (isNotInteger(arg[1])) {
                Log.info("count has to be integer.");
                return;
            }
            int count = Integer.parseInt(arg[1]);
            Player player = findPlayer(arg[2]);
            if (player == null) {
                Log.info("Player not found.");
                return;
            }
            Team team = arg.length > 3 ? getTeamByName(arg[3]) : Team.crux;
            for (int i = 0; i < count; i++) {
                BaseUnit unit = unitType.create(team);
                unit.set(player.x, player.y);
                unit.add();
            }
        });

        handler.register("w-map-stats","Shows all maps with statistics.",arg-> Log.info(mapManager.statistics()));

        handler.register("w-map-cleanup","Removes data about already removed maps.",arg->{
            mapManager.cleanup();
            Log.info("Data removed");
        });

        handler.register("w-apply-config","[factory/test/general/ranks]", "Applies the factory configuration,settings and " +
                "loads test quescions.", arg -> {
            if(arg.length==0){
                factory.config();
                config();
                tester.loadQuestions();
                dataBase.loadRanks();
                return;
            }
            switch (arg[0]){
                case "factory":
                    factory.config();
                    break;
                case "tast":
                    tester.loadQuestions();
                    break;
                case "general":
                    config();
                    break;
                case "ranks":
                    dataBase.loadRanks();
                    break;
                default:
                    Log.info("Invalid argument.");
            }
        });

        handler.register("w-trans-time", "[value]", "Sets transport time.", arg -> {
            if (arg.length == 0) {
                Log.info("trans-time is " + transportTime + ".");
                return;
            }
            if (isNotInteger(arg[0])) {
                Log.info(arg[0] + " is not an integer.");
                return;
            }
            transportTime = Integer.parseInt(arg[0]);
            Log.info("trans-time set to " + transportTime + ".");
        });

        handler.register("w-autoSave", "[frequency]", "Initializes autosave or stops it.", arg -> {
            Integer frequency = processArg(null, "Frequency", arg[0]);
            if (frequency == null) return;
            if (frequency == 0) {
                Log.info("If you want kill-server command so badly, you can open an issue on github.");
                return;
            }
            autoSave(frequency);
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("vote");
        handler.removeCommand("votekick");

        handler.<Player>register("mkgf","[playerName]","Adds, or removes if payer is marked, griefer mark of given " +
                        "player name.",(arg, player) ->{
            if(playerGroup.size() < 3 && !player.isAdmin) {
                player.sendMessage(prefix+"At least 3 players are needed to mark a griefer.");
                return;
            }
            if(arg.length == 0) {
                player.sendMessage(getPlayerList());
                return;
            }
            Package p=actionManager.verify(player,arg[0],0,false);
            if(p==null) return;
            vote.aVote(actionManager,p,"[pink]"+p.object+"[] griefer mark on/of [pink]"+((Player)p.obj).name+"[]");
        });

        handler.<Player>register("emergency","[off]","Starts emergency.For admins only.",(arg, player) ->{
            if(!player.isAdmin){
                player.sendMessage(prefix+"Only admin can start or disable emergency.");
                return;
            }
            actionManager.switchEmergency(arg.length==1);
        });

        handler.<Player>register("maps","[page/rate/info/rules] [mapName/mapIdx/1-10]","Displays list maps,rates current map or " +
                        "display information about current or chosen map.",
                (arg, player) -> {
            Integer page;
            if(arg.length>0){
                if(arg[0].equals("info") || arg[0].equals("rules")){
                    String stats=arg[0].equals("rules") ? mapManager.getMapRules(arg.length==2 ? arg[1]:null):
                            mapManager.getMapStats(arg.length==2 ? arg[1]:null);
                    if(stats==null){
                        player.sendMessage(prefix+"Map not found.");
                        return;
                    }
                    Call.onInfoMessage(player.con,stats);
                    return;
                }
                if(arg[0].equals("rate")){
                    if(notEnoughArgs(player,2,arg)) return;
                    Integer rating=processArg(player,"rating",arg[1]);
                    if(rating==null) return;
                    rating= Mathf.clamp(rating,1,10);
                    mapManager.rate(player,rating);
                    return;
                }
                page=processArg(player,"Page",arg[0]);
                if(page==null)return;
            }else {
                page=1;
            }
            Call.onInfoMessage(player.con,formPage(mapManager.info(),page,"mpa list",pageSize));
        });

        handler.<Player>register("l", "<fill/use/info> [itemName/all] [itemAmount]",
                "Fill loadout with resources from core/send resources from loadout to core.", (arg, player) -> {
            boolean use;
            switch (arg[0]){
                case "info":
                    Call.onInfoMessage(player.con, loadout.info());
                    return;
                case "use":
                    use=true;
                    break;
                case "fill":
                    use=false;
                    break;
                default:
                    invalidArg(player, arg[0]);
                    return;
            }
            if(notEnoughArgs(player,3,arg)) return;

            Integer amount=processArg(player,"Item amount",arg[2]);
            if(amount==null)return;

            Package p = loadout.verify(player, arg[1], amount, use);
            if (p == null) return;

            vote.aVote(loadout, p, "launch [orange]" +
                    (p.object.equals("all") ? p.amount + " of all" : p.amount + " " + p.object) + "[] to "
                    + (use ? "core" : "loadout"));
        });

        handler.<Player>register("f", "<build/send/info/price> [unitName/all] [unitAmount]",
                "Build amount of unit or Send amount of units from hangar.",
                (arg, player) -> {
            boolean send;
            switch (arg[0]){
                case "info":
                    Call.onInfoMessage(player.con, factory.info());
                    return;
                case "price":
                    if(notEnoughArgs(player,2,arg)) return;
                    int amount = arg.length == 2 || isNotInteger(arg[2]) ? 1 : Integer.parseInt(arg[2]);
                    Call.onInfoMessage(player.con, factory.price(player, arg[1], amount));
                    return;
                case "send":
                    send=true;
                    break;
                case "build":
                    send=false;
                    break;
                default:
                    invalidArg(player,arg[0]);
                    return;
            }
            if( notEnoughArgs(player,3,arg)) return;

            Integer amount=processArg(player,"Unit amount",arg[2]);
            if(amount==null)return;

            Package p = factory.verify(player, arg[1],amount, send);
            if (p == null) return;

            vote.aVote(factory, p, arg[0] + " " + report(p.object, p.amount) + " units");
        });

        handler.<Player>register("build-core", "<small/normal/big>", "Makes new core.",
                (arg, player) -> {
            Package p = builder.verify(player, arg[0], 0, true);
            if (p == null) return;

            vote.aVote(builder, p, "building " + arg[0] + " core at "+p.x+" "+p.y);
        });

        handler.<Player>register("vote", "<map/skipwave/restart/gameover/kickallafk/y/n> [indexOrName/waveAmount]", "Opens vote session or votes in case of votekick.",
                (arg, player) -> {
            if(Database.getData(player).rank==Rank.AFK){
                player.sendMessage(prefix+"You are AFK vote isn t enabled for you.");
                return;
            }
            Package p;
            String secArg = arg.length == 2 ? arg[1] : "0";
            switch (arg[0]) {
                case "map":
                    p = mapManager.verify(player, secArg, 0, false);
                    if (p == null) return;

                    vote.aVote(mapManager, p, "changing map to " + ((mindustry.maps.Map)p.obj).name());
                    return;
                case "skipwave":
                    Integer amount=processArg(player,"Wave amount",secArg);
                    if(amount==null)return;

                    p = skipper.verify(player, null, amount, false);
                    vote.aVote(skipper, p, "skipping " + p.amount + " waves");
                    return;
                case "restart":
                    vote.aVote(mapManager, new Package(null ,world.getMap(), player),
                            "restart the game");
                    return;
                case "gameover":
                    vote.aVote(mapManager, new Package(null ,null, player),
                            "gameover.");
                    return;
                case "kickallafk":
                    vote.aVote(dataBase,new Package(null,null ,player),"kick all afk players");
                case "y":
                    voteKick.vote(player,1);
                    return;
                case "n":
                    voteKick.vote(player,-1);
                    return;
                default:
                    invalidArg(player, arg[0]);
            }
        });

        handler.<Player>register("suicide","Kill your self.",(arg, player) -> {
            if(!Database.hasSpecialPerm(player,Perm.suicide)){
                //player.sendMessage("You have to be "+Rank.kamikaze.getSuffix()+" to suicide.");
                return;
            }
            player.onDeath();
            player.kill();
            Call.sendMessage(prefix+player.name+" committed suicide.");
            Timer.schedule(()->Call.sendMessage(prefix+"F..."),5);
        });

        handler.<Player>register("set","[settingName/textColor] [on/off/color]","Set your message color or " +
                        "enable/disable any setting.Write just /set for setting list.",(arg, player) ->{
            if(arg.length==0) {
                player.sendMessage(prefix+"Options:"+ Arrays.toString(Setting.values()));
                return;
            }
            if(notEnoughArgs(player,2,arg)) return;

            if(arg[0].equals("textColor")){
                Database.getData(player).textColor=arg[1];
                player.sendMessage(prefix+"["+arg[1]+"]if you see your color in brackets it probably isn t " +
                        "valid or recognized.");
                return;
            }
            try {
                Setting s=Setting.valueOf(arg[0]);
                Database.switchSetting(player,s,arg[1].equals("off"));
                player.sendMessage(prefix+"You toggled the [orange]"+s.name()+"[] "+arg[1]+".");
            } catch (IllegalArgumentException ex){
                player.sendMessage(prefix+"Non existent setting. Use /set to see options.");
            }
                });

        handler.<Player>register("search","<searchKey/chinese/sort/online/rank> [invert/normal] [sortType/rankName] ",
                "Search for player by name, display all chinese players, all online players," +
                        "all players with specified rank or sorted list of players.You can optionally invert the " +
                        "list.",(arg,player)->{
            Array<String> res=new Array<>();
            if(arg.length>=1) {
                switch (arg[0]) {
                    case "rank":
                        if(arg.length!=3) {
                            player.sendMessage(prefix + "Available ranks: " + Arrays.toString(Rank.values()) +
                                    "\nAvailable special ranks:" + Database.ranks.toString());
                            return;
                        }
                        break;
                    case "sort":
                        if(arg.length!=3){
                            player.sendMessage(prefix + "Available sort types: " + Arrays.toString(Stat.values()));
                            return;
                        }
                        break;
                    case "chinese":
                        res = Database.getAllChinesePlayersIndexes();
                        break;
                    case "online":
                        res = Database.getOnlinePlayersIndexes();
                        break;
                    default:
                        res = Database.getAllPlayersIndexes(arg[0]);
                        break;
                }

            }
            if(arg.length==3 && res.isEmpty()){
                res =arg[1].equals("sort") ? Database.getSorted(arg[1]):Database.getAllPlayersIndexesByRank(arg[1]);
                if (res == null) {
                    player.sendMessage(prefix + "Invalid sort type, for list of available see /search sort");
                    return;
                }
            }
            if(arg.length>=2 && arg[1].equals("invert")){
                res.reverse();
            }
            for(String s:res){
                player.sendMessage(s);
            }
            if(res.isEmpty()){
                player.sendMessage(prefix+"No results found.");
            }
        });

        handler.<Player>register("info","[name/ID|list/ranks] [page] ","Displays info about you or another player.",
                (arg,player)-> {
            if(arg.length>=1){
                Integer page=1;
                if(arg.length==2){
                    page=processArg(player,"Page",arg[1]);
                    if(page==null)return;
                }
                if(arg[0].equals("list")){
                    Call.onInfoMessage(player.con, formPage(Database.getAllPlayersIndexes(null),
                            page, "player list",pageSize));
                    return;
                }
                if(arg[0].equals("ranks")){
                    Call.onInfoMessage(player.con, formPage(Database.getRankInfo(), page, "rank info",4));
                    return;
                }
                PlayerData pd=Database.findData(arg[0]);
                if(pd==null){
                    player.sendMessage(prefix+"Player not found.");
                    return;
                }
                Call.onInfoMessage(player.con,pd.toString());
                return;
            }
            Call.onInfoMessage(player.con,Database.getData(player).toString());
                });

        handler.<Player>register("votekick", "[player]", "Vote to kick a player.", (args, player) -> {

            if(!Administration.Config.enableVotekick.bool()) {
                player.sendMessage("[scarlet]Vote-kick is disabled on this server.");
                return;
            }
            if(voteKick.voting){
                player.sendMessage(prefix+"Votekick in process.");
                return;
            }
            if(playerGroup.size() < 3) {
                player.sendMessage(prefix+"At least 3 players are needed to start a votekick.");
                return;
            }
            if(player.isLocal){
                player.sendMessage(prefix+"Just kick them yourself if you're the host.");
                return;

            }
            if(args.length == 0) {
                player.sendMessage(getPlayerList());
                return;
            }

            voteKick.aVoteKick(args[0],player);

        });

        handler.<Player>register("test","<start/egan/quit/numberOfOption>","Complete the test to " +
                        "become verified player.",(args, player) -> tester.processAnswer(player,args[0]));

        handler.<Player>register("set-rank","<playerName/uuid/ID> <rank/restart>","Command for admins.",(args,player)->{
            if(!player.isAdmin){
                player.sendMessage(prefix+"You are not admin.");
                return;
            }

            PlayerData pd=Database.findData(args[0]);
            if(pd==null ){
                player.sendMessage(prefix+"Player not found.");
                return;
            }

            if(pd==Database.getData(player)){
                player.sendMessage(prefix+"You cannot change your rank.");
                return;
            }

            try{
                Rank rank=Rank.valueOf(args[1]);
                if (rank.isAdmin){
                    player.sendMessage(prefix+"You cannot use this rank.");
                    return;
                }
                player.sendMessage(prefix+"Rank of player [orange]" + pd.originalName + "[] is now " + rank.getName() + ".");
                Database.setRank(pd,rank);
            }catch (IllegalArgumentException e){
                if(args[1].equals("restart")){
                    pd.specialRank=null;
                    Call.sendMessage("Rank of player [orange]" + pd.originalName + "[] wos restarted.");
                } else if(!Database.ranks.containsKey(args[1])){
                    player.sendMessage(prefix+"Rank not found.\nRanks:" + Arrays.toString(Rank.values())+"\n" +
                            "Custom ranks:"+Database.ranks.keySet());
                    return;
                }else {
                    pd.specialRank=args[1];
                    SpecialRank sr=Database.getSpecialRank(pd);
                    if(sr==null) return;
                    Call.sendMessage(prefix+"Rank of player [orange]" + pd.originalName + "[] is now " + sr.getSuffix() + ".");

                }
            }


            Player p=findPlayer(args[0]);
            if(p==null)return;
            Database.updateName(p,pd);
        });

        handler.<Player>register("dm","<player> <text...>", "Send direct message to player.", (arg,player) -> {
            StringBuilder b=new StringBuilder();
            for(String s:arg){
                b.append(s).append(" ");
            }
            Player other=findPlayer(arg[0]);
            if(other==null){
                player.sendMessage(prefix+"Player not found.");
                return;
            }
            player.sendMessage("[#ffdfba][DM to "+other.name+"][]:"+b.toString().replace(arg[0],""));
            other.sendMessage("[#ffdfba][DM from "+player.name+"][]:"+b.toString().replace(arg[0],""));
        });
    }
}
