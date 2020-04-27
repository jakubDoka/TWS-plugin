package theWorst;

import arc.Events;
import arc.struct.Array;
import arc.struct.ArrayMap;
import arc.util.*;
import mindustry.entities.traits.BuilderTrait;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.base.BuilderDrone;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.ItemType;

import java.io.*;
import java.util.Arrays;

import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import theWorst.dataBase.*;
import theWorst.helpers.CoreBuilder;
import theWorst.helpers.MapChanger;
import theWorst.helpers.Tester;
import theWorst.helpers.WaveSkipper;
import theWorst.interfaces.Interruptible;
import theWorst.interfaces.LoadSave;
import theWorst.requests.Factory;
import theWorst.requests.Loadout;
import theWorst.requests.Request;
import theWorst.requests.Requesting;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static mindustry.Vars.*;

public class Main extends Plugin {
    public static final String configFile ="settings.json";
    public static final String saveFile = "save.json";
    public static final String directory = "config/mods/The_Worst/";
    public static final String prefix = "[scarlet][Server][]";

    public static final String[] itemIcons = {"\uF838", "\uF837", "\uF836", "\uF835", "\uF832", "\uF831", "\uF82F", "\uF82E", "\uF82D", "\uF82C"};
    public static ArrayMap<String, LoadSave> loadSave = new ArrayMap<>();
    public static ArrayMap<String, Requesting> configured = new ArrayMap<>();

    public static int transportTime = 3 * 60;

    public static Array<Item> items = new Array<>();
    Array<Interruptible> interruptibles = new Array<>();

    Timer.Task autoSaveThread;
    Timer.Task updateThread;

    int defaultAutoSaveFrequency=5;

    Loadout loadout = new Loadout();
    Factory factory;
    CoreBuilder builder = new CoreBuilder();
    MapChanger changer = new MapChanger();
    WaveSkipper skipper = new WaveSkipper();

    DataBase dataBase=new DataBase();
    Tester tester =new Tester();
    AntiGriefer antiGriefer=new AntiGriefer();
    Vote vote = new Vote();
    VoteKick voteKick=new VoteKick();

    public Main() {
        Events.on(PlayerConnect.class, e -> dataBase.register(e.player));

        Events.on(PlayerLeave.class,e-> dataBase.onDisconnect(e.player));

        Events.on(GameOverEvent.class, e ->{
            for(Player p:playerGroup){
                if(p.getTeam()==e.winner) {
                    dataBase.updateStats(p, Stat.gamesWon);
                }
                dataBase.updateStats(p, Stat.gamesPlayed);

            }
        });

        Events.on(BlockBuildEndEvent.class, e->{
            
            if(e.tile.block().buildCost/60<1) return;

            dataBase.updateStats(e.player,e.breaking ? Stat.buildingsBroken:Stat.buildingsBuilt);
        });
        Events.on(BuildSelectEvent.class, e->{
            if(e.builder instanceof Player){
                boolean happen =false;
                Player player=(Player)e.builder;
                CoreBlock.CoreEntity core=Loadout.getCore(player);
                BuilderTrait.BuildRequest request = player.buildRequest();
                if(DataBase.hasSpecialPerm(player,Perm.destruct) && request.breaking){
                    happen=true;
                    for(ItemStack s:request.block.requirements){
                        core.items.add(s.item,s.amount/2);
                    }
                    Call.onDeconstructFinish(request.tile(),request.block,((Player) e.builder).id);

                }else if(DataBase.hasSpecialPerm(player,Perm.build) && !request.breaking){
                    if(core.items.has(request.block.requirements)){
                        happen=true;
                        for(ItemStack s:request.block.requirements){
                            core.items.remove(s);
                        }
                        Call.onConstructFinish(e.tile,request.block,((Player) e.builder).id,
                                (byte) request.rotation,player.getTeam(),false);
                        e.tile.configure(request.config);
                    }
                }
                if(happen)
                    Events.fire(new BlockBuildEndEvent(e.tile,player,e.team,e.breaking));
            }
                });

        Events.on(EventType.UnitDestroyEvent.class, e->{
            if(e.unit instanceof Player){
                dataBase.updateStats((Player)e.unit,Stat.deaths);
            }else if(e.unit.getTeam()==Team.crux){
                for(Player p:playerGroup){
                    dataBase.updateStats(p,Stat.enemiesKilled);
                }
            }
        });

        Events.on(PlayerChatEvent.class, e -> {
            if (vote.voting){
                if(AntiGriefer.isGriefer(e.player)){
                    AntiGriefer.abuse(e.player);
                    return;
                }
                if(e.message.equals("y") || e.message.equals("n")) {
                    vote.addVote(e.player, e.message);
                }
            }
        });

        Events.on(WorldLoadEvent.class, e -> interruptibles.forEach(Interruptible::interrupt));

        Events.on(EventType.BuildSelectEvent.class, e -> {
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
            netServer.admins.addActionFilter(action -> {
                Player player = action.player;
                if (player == null) return true;

                if (player.isAdmin) return true;

                return antiGriefer.canBuild(player);
            });
            netServer.admins.addChatFilter((player,message)->{
                if((message.equals("y") || message.equals("n")) && vote.voting){
                    return null;
                }
                if(AntiGriefer.isGriefer(player)){
                    if(Time.timeSinceMillis(DataBase.getData(player).lastMessage)<10000L){
                        AntiGriefer.abuse(player);
                        return null;
                    }

                }
                DataBase.getData(player).lastMessage=Time.millis();
                return message;
            });

            load_items();
            factory = new Factory(loadout);
            addToGroups();
            if (!makeDir()) {
                Log.info("Unable to create directory " + directory + ".");
            }
            load();
            config();
            tester.loadQuestions();
            autoSave(defaultAutoSaveFrequency);
            updateHud();
        });

    }

    private void addToGroups(){
        interruptibles.add(loadout);
        interruptibles.add(factory);
        interruptibles.add(vote);
        interruptibles.add(antiGriefer);
        loadSave.put("loadout", loadout);
        loadSave.put("factory", factory);
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

    public void updateHud(){
        updateThread=Timer.schedule(()-> {
            try {
                StringBuilder b = new StringBuilder();
                for (Interruptible i : interruptibles) {
                    String msg = i.getHudInfo();
                    if (msg == null) continue;
                    b.append(msg).append("\n");
                }
                for (Player p : playerGroup.all()) {
                    if (DataBase.getData(p).hudEnabled) {
                        Call.setHudText(p.con, b.toString().substring(0, b.length() - 1));
                    } else {
                        Call.setHudText(p.con, "");
                    }
                }
            }catch (Exception ex){
                Log.info("something horrible happen");
            }
        },0,1);

    }

    public void load() {
        String path = directory + saveFile;
        try (FileReader fileReader = new FileReader(path)) {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(fileReader);
            JSONObject saveData = (JSONObject) obj;
            for (String r : loadSave.keys()) {
                if (!saveData.containsKey(r)) {
                    Log.info("Failed to load save file.");
                    return;
                }
            }
            loadSave.keys().forEach((k) -> loadSave.get(k).load((JSONObject) saveData.get(k)));
            fileReader.close();
            Log.info("Data loaded.");
        } catch (FileNotFoundException ex) {
            Log.info("No saves found.New save file " + path + " will be created.");
            save();
        } catch (ParseException ex) {
            Log.info("Json file "+path+" is invalid.");
        } catch (IOException ex) {
            Log.info("Error when loading data from " + path + ".");
        }
        dataBase.load();
    }

    public void save() {
        JSONObject saveData = new JSONObject();
        loadSave.keys().forEach((k) -> saveData.put(k, loadSave.get(k).save()));
        try (FileWriter file = new FileWriter(directory + saveFile)) {
            file.write(saveData.toJSONString());
            file.close();
            Log.info("Data saved.");
        } catch (IOException ex) {
            Log.info("Error when saving data.");
        }
        dataBase.save();
    }

    public void config(){
        String path = directory + configFile;
        try (FileReader fileReader = new FileReader(path)) {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(fileReader);
            JSONObject saveData = (JSONObject) obj;
            JSONObject data =(JSONObject)saveData.get("loadout");
            for(Object o:data.keySet()){
                loadout.getConfig().put((String)o,getInt(data.get(o)));
            }
            data =(JSONObject)saveData.get("factory");
            for(Object o:data.keySet()){
                factory.getConfig().put((String)o,getInt(data.get(o)));
            }
            transportTime=getInt(saveData.get("transTime"));
        }catch (FileNotFoundException ex) {
            Log.info("No settings found.New save file " + path + " will be created.");
            createDefaultConfig();
        } catch (ParseException ex) {
            Log.info("Json file "+path+" is invalid.");
        } catch (IOException ex) {
            Log.info("Error when loading settings from " + path + ".");
        }
    }

    private void createDefaultConfig() {
        JSONObject configData = new JSONObject();
        JSONObject load = new JSONObject();
        for(String o:loadout.getConfig().keys()){
            load.put(o,getInt(loadout.getConfig().get(o)));
        }
        configData.put("loadout",load);
        load =new JSONObject();
        for(String o:factory.getConfig().keys()){
           load.put(o,getInt(factory.getConfig().get(o)));
        }
        configData.put("factory",load);
        configData.put("transTime",180);
        try (FileWriter file = new FileWriter(directory + configFile)) {
            file.write(configData.toJSONString());
        } catch (IOException ex) {
            Log.info("Error when creating settings.");
        }
    }

    private void autoSave(int interval){
        if (autoSaveThread != null){
            autoSaveThread.cancel();
        }
        interval*=60;
        autoSaveThread = Timer.schedule(this::save,interval,interval);
        Log.info("Autosave started.It will save every "+ interval/60 +"min.");
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

    public static String timeToString(int time) {
        return time / 60 + "min" + time % 60 + "sec";
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
        if(args.length!=amount){
            String m="Not enough arguments.";
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
            if(p.isAdmin || p.con == null || p == player || DataBase.hasPerm(p,Perm.higher.getValue())) continue;

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
        while (name.contains("[")){
            int first=name.indexOf("["),last=name.indexOf("]");
            name=name.substring(0,first)+name.substring(last+1);
        }
        while (name.contains("<")){
            int first=name.indexOf("<"),last=name.indexOf(">");
            name=name.substring(0,first)+name.substring(last+1);
        }
        name=name.replace(" ","_");
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
    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("say");

        handler.register("w-load", "Reloads theWorst saved data.", arg -> load());

        handler.register("w-save", "Saves theWorst data.", arg -> save());

        handler.register("w-unkick","<ID/uuid>","Erases kick status of player player.",arg->{
            PlayerData pd=DataBase.findData(arg[0]);
            if(pd==null){
                Log.info("Player not found.");
                return;
            }
            pd.getInfo().lastKicked=Time.millis();
            Log.info(pd.originalName+" is not kicked anymore... hopefully.");
                });

        handler.register("w-database","[search]", "Shows database,list of all players that " +
                "ewer been on server.Use search as in browser.", arg ->
                Log.info(dataBase.report(arg.length==1 ? arg[0]:null,true,-1)));

        handler.register("w-set-rank","<uuid/name/index> <rank>","",arg->{
            try{
                Rank.valueOf(arg[1]);
            }catch (IllegalArgumentException e){
                Log.info("Rank not found. Ranks:"+ Arrays.toString(Rank.values()));
                return;
            }
            PlayerData pd=DataBase.findData(arg[0]);
            if(pd==null ){
                Log.info("Player not found.");
                return;
            }
            DataBase.setRank(pd,Rank.valueOf(arg[1]));
            Log.info("Rank of player " + pd.originalName + " is now " + pd.rank.name() + ".");
            Player p=findPlayer(arg[0]);
            if(p==null){
                p=playerGroup.find(player->player.uuid.equalsIgnoreCase(arg[0]));
                if(p==null){
                    return;
                }
            }
            DataBase.updateName(p);
        });

        handler.register("w-info","<uuid/name/index>","Displays info about player.",arg->{
            PlayerData pd=DataBase.findData(arg[0]);
            if(pd==null ) {
                Log.info("Player not found. Search by name applies only on online players.");
                return;
            }
            Log.info(pd.toString());
        });

        handler.register("spawn", "<mob_name> <count> <playerName> [team] ", "Spawn mob in player position.", arg -> {
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

        handler.register("w-apply-config", "Applies the factory configuration,settings and " +
                "loads test quescions.", arg -> {
            factory.config();
            config();
            tester.loadQuestions();
            Log.info("Config applied.");
        });

        handler.register("w-trans-time", "[value]", "Sets transport time.", arg -> {
            if(arg.length==0){
                Log.info("trans-time is "+transportTime+".");
                return;
            }
            if (isNotInteger(arg[0])) {
                Log.info(arg[0] + " is not an integer.");
                return;
            }
            transportTime = Integer.parseInt(arg[0]);
            Log.info("trans-time set to "+transportTime+".");
        });

        handler.register("w-options","shows options for w command",arg->{
            for(String key:configured.keys()){
                Log.info(key+":"+ toString(configured.get(key).getConfig().keys().toArray()));
            }
        });

        handler.register("w-autoSave","[frequency]","Initializes autosave or stops it.",arg->{
            Integer frequency=processArg(null,"Frequency",arg[0]);
            if(frequency==null) return;
            if( frequency==0){
                Log.info("If you want kill-server command so badly, you can open an issue on github.");
                return;
            }
            autoSave(frequency);
        });

        handler.register("w", "<target> <property> <value>", "Sets property of target to value/integer.", arg -> {
            if (!configured.containsKey(arg[0])) {
                Log.info("Invalid target.Valid targets:" + toString(configured.keys().toArray()));
                return;
            }

            ArrayMap<String, Integer> config = configured.get(arg[0]).getConfig();
            if (!config.containsKey(arg[1])) {
                Log.info(arg[0] + " has no property " + arg[1] + ". Valid properties:" + toString(config.keys().toArray()));
                return;

            }
            Integer value=processArg(null,"Value",arg[2]);
            if(value==null) return;
            config.put(arg[1], value);
            Log.info("Property changed.");
        });

        handler.register("say","<text...>","Send message to all players.",
                arg-> Call.sendMessage(prefix+arg[0]));

    }
    public static String toString(Array<String> struct){
        StringBuilder b=new StringBuilder();
        for(String s :struct){
            b.append(s).append(" ");
        }
        return b.toString();
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("vote");
        handler.removeCommand("votekick");

        handler.<Player>register("mkgf","[playerName]","Adds, or removes if payer is marked, griefer mark of given " +
                        "player name.",(arg, player) ->{
            if(playerGroup.size() < 3) {
                player.sendMessage(prefix+"At least 3 players are needed to mark a griefer.");
                return;
            }
            if(arg.length == 0) {
                player.sendMessage(getPlayerList());
                return;
            }
            Package p=antiGriefer.verify(player,arg[0],0,false);
            if(p==null) return;
            vote.aVote(antiGriefer,p,"[pink]"+p.object+"[] griefer mark on/of [pink]"+((Player)p.obj).name+"[]");
        });

        handler.<Player>register("emergency","[off]","Starts emergency.For admins only.",(arg, player) ->{
            if(!player.isAdmin){
                player.sendMessage(prefix+"Only admin can start or disable emergency.");
                return;
            }
            antiGriefer.switchEmergency(arg.length==1);
        });

        handler.<Player>register("maps","[page]","displays all maps",
                (arg, player) -> {
            Integer page;
            if(arg.length>0){
                page=processArg(player,"Page",arg[0]);
                if(page==null)return;
            }else {
                page=1;
            }
            Call.onInfoMessage(player.con, changer.info(page));
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

            vote.aVote(builder, p, "building " + arg[0] + " core");
        });

        handler.<Player>register("vote", "<map/skipwave/restart/gameover/y/n> [indexOrName/waveAmount]", "Opens vote session or votes in case of votekick.",
                (arg, player) -> {
            Package p;
            String secArg = arg.length == 2 ? arg[1] : "0";
            switch (arg[0]) {
                case "map":
                    p = changer.verify(player, secArg, 0, false);
                    if (p == null) return;

                    vote.aVote(changer, p, "changing map to " + ((mindustry.maps.Map)p.obj).name() + ". ");
                    return;
                case "skipwave":
                    Integer amount=processArg(player,"Wave amount",secArg);
                    if(amount==null)return;

                    p = skipper.verify(player, null, amount, false);
                    vote.aVote(skipper, p, "skipping " + p.amount + " waves");
                    return;
                case "restart":
                    vote.aVote(changer, new Package(null ,world.getMap(), player),
                            "restart the game");
                    return;
                case "gameover":
                    vote.aVote(changer, new Package(null ,null, player),
                            "gameover.");
                    return;
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
            if(!DataBase.hasSpecialPerm(player,Perm.suicide)){
                player.sendMessage("You have to be "+Rank.kamikaze.getRank()+" to suicide.");
                return;
            }
            player.onDeath();
            player.kill();
            Call.sendMessage(prefix+player.name+" committed suicide.");
            Timer.schedule(()->Call.sendMessage(prefix+"F..."),5);
        });

        handler.<Player>register("hud","<on/off>","Enable or disable hud information."
                ,(arg, player) -> DataBase.getData(player).hudEnabled=arg[0].equals("on"));

        handler.<Player>register("info","[name/ID/list] [page]","Displays info about you or another player.",
                (arg,player)-> {
            if(arg.length>=1){
                if(arg[0].equals("list")){
                    Integer page=1;
                    if(arg.length==2){
                        page=processArg(player,"Page",arg[1]);
                        if(page==null)return;
                    }
                    Call.onInfoMessage(player.con,dataBase.report(null,false,page));
                    return;
                }
                PlayerData pd=DataBase.findData(arg[0]);
                if(pd==null){
                    player.sendMessage(prefix+"Player not found.");
                    return;
                }
                Call.onInfoMessage(player.con,pd.toString());
                return;
            }
            Call.onInfoMessage(player.con,DataBase.getData(player).toString());
                });

        handler.<Player>register("votekick", "[player]", "Vote to kick a player.", (args, player) -> {

            if(!Administration.Config.enableVotekick.bool()) {
                player.sendMessage("[scarlet]Vote-kick is disabled on this server.");
                return;
            }
            if(voteKick.voting){
                player.sendMessage(prefix+"Votekick in process.");
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
        handler.<Player>register("set-Rank","<playerName/uuid/ID> <rankName>","Command for admins.",(args,player)->{
            if(!player.isAdmin){
                player.sendMessage(prefix+"You are not admin.");
                return;
            }
            try{
                Rank.valueOf(args[1]);
            }catch (IllegalArgumentException e){
                player.sendMessage(prefix+"Rank not found. Ranks:"+ Arrays.toString(Rank.values()));
                return;
            }
            PlayerData pd=DataBase.findData(args[0]);
            if(pd==null ){
                player.sendMessage(prefix+"Player not found.");
                return;
            }

            DataBase.setRank(pd,Rank.valueOf(args[1]));
            player.sendMessage(prefix+"Rank of player " + pd.originalName + " is now " + pd.rank.getRankAnyway() + ".");
            Player p=findPlayer(args[0]);
            if(p==null){
                return;
            }
            DataBase.updateName(p);
        });
    }
}
