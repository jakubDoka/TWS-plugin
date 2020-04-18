package theWorst;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
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
import theWorst.requests.Requesting;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static mindustry.Vars.*;

public class Main extends Plugin {
    public static final String ALL = "all";
    public static final String saveFile = "save.json";
    public static final String directory = "config/mods/The_Worst/";
    public static final String prefix = "[scarlet][Server][]";

    public static final String[] itemIcons = {"\uF838", "\uF837", "\uF836", "\uF835", "\uF832", "\uF831", "\uF82F", "\uF82E", "\uF82D", "\uF82C"};
    public static HashMap<String, LoadSave> loadSave = new HashMap<>();
    public static HashMap<String, Requesting> configured = new HashMap<>();

    public static int transportTime = 3 * 60;

    public static ArrayList<Item> items = new ArrayList<>();
    ArrayList<Interruptible> interruptibles = new ArrayList<>();

    Timer.Task autoSaveThread;
    Timer.Task updateThread;
    int defaultAutoSaveFrequency=5;

    Loadout loadout = new Loadout();
    Factory factory;
    CoreBuilder builder = new CoreBuilder();
    MapChanger changer = new MapChanger();
    WaveSkipper skipper = new WaveSkipper();
    AntiGriefer antiGriefer=new AntiGriefer();
    Vote vote = new Vote();


    public Main() {
        Events.on(PlayerConnect.class, e -> {
            antiGriefer.addRank(e.player);
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

        Events.on(WorldLoadEvent.class, e ->{
            interruptibles.forEach(Interruptible::interrupt);
        });

        Events.on(EventType.BuildSelectEvent.class, e -> {
            ArrayList<Request> requests = factory.getRequests();
            if (requests.size() > 0) {
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

                //if (player.isAdmin) return true;
                if(AntiGriefer.isGriefer(player)){
                    AntiGriefer.abuse(player);
                    return false;
                }
                return action.type != Administration.ActionType.rotate;
            });
            netServer.admins.addChatFilter((player,message)->{
                if((message.equals("y") || message.equals("n")) && vote.voting){
                    return null;
                }
                if(AntiGriefer.isGriefer(player)){
                    if(Time.timeSinceMillis(AntiGriefer.getLastMessageTime(player))<10000L){
                        AntiGriefer.abuse(player);
                        return null;
                    }
                    AntiGriefer.updateLastMessageTime(player);
                }
                return message;
            });

            load_items();
            factory = new Factory(loadout);
            addToGroups();
            if (!makeDir()) {
                Log.info("Unable to create directory " + directory + ".");
            }
            load();
            autoSave(defaultAutoSaveFrequency);
            updateHud();
        });

    }

    private void addToGroups(){
        interruptibles.add(loadout);
        interruptibles.add(factory);
        interruptibles.add(vote);
        loadSave.put("loadout", loadout);
        loadSave.put("factory", factory);
        loadSave.put("griefers", antiGriefer);
        configured.put("loadout", loadout);
        configured.put("factory", factory);
    }



    public void updateHud(){
        updateThread=Timer.schedule(()-> {
            StringBuilder b=new StringBuilder();
            for(Interruptible i:interruptibles){
                String msg=i.getHudInfo();
                if(msg==null) continue;
                b.append(msg).append("\n");
            }
            Call.setHudText(b.toString().substring(0,b.length()-1));
        },0,1);
    }

    public void load() {
        String path = directory + saveFile;
        try (FileReader fileReader = new FileReader(path)) {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(fileReader);
            JSONObject saveData = (JSONObject) obj;
            for (String r : loadSave.keySet()) {
                if (!saveData.containsKey(r)) {
                    Log.info("Failed to load save file.");
                    return;
                }
            }
            loadSave.keySet().forEach((k) -> loadSave.get(k).load((JSONObject) saveData.get(k)));
            fileReader.close();
            Log.info("Data loaded.");
        } catch (FileNotFoundException ex) {
            Log.info("No saves found.New save file " + path + " will be created.");
            save();
        } catch (ParseException ex) {
            Log.info("Json file is invalid.");
        } catch (IOException ex) {
            Log.info("Error when loading data from " + path + ".");
        }
    }



    public void save() {
        JSONObject saveData = new JSONObject();
        loadSave.keySet().forEach((k) -> saveData.put(k, loadSave.get(k).save()));
        try (FileWriter file = new FileWriter(directory + saveFile)) {
            file.write(saveData.toJSONString());
            file.close();
            Log.info("Data saved.");
        } catch (IOException ex) {
            Log.info("Error when saving data.");
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
        return "[orange]" + (object.equals(Main.ALL) ? Main.ALL : amount + " " + object) + "[]";
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

    public static void missingPropertyError(String address) {
        Log.err(address + " is missing.Property will be set to default value.");
    }

    public static boolean isInvalidArg(Player player, String what, String agr) {
        if (isNotInteger(agr)) {
            if(player==null){
                Log.info(what + " has to be integer.[scarlet]" + agr + "[] is not.");
                return true;
            }
            player.sendMessage(prefix + what + " has to be integer.[scarlet]" + agr + "[] is not.");
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

    public static Player findPlayer(String name) {
       for(Player p:playerGroup){
           String pName=cleanName(p.name);
           if(pName.equals(name)){
               return p;
           }
       };
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

    public static Team getTeamByName(String name) {
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

    public static int getStorageSize(Player player) {
        int size = 0;
        for (CoreBlock.CoreEntity c : player.getTeam().cores()) {
            size += c.block.itemCapacity;
        }
        return size;
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("w-load", "Reloads theWorst saved data.", arg -> load());

        handler.register("w-save", "Saves theWorst data.", arg -> save());

        handler.register("spawn", "<mob_name> <count> <playerName> [team] ", "Spawn mob in player position.", arg -> {
            if (playerGroup.size() == 0) {
                Log.info("there is no one logged,why bother spawning units?");
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

        handler.register("w-apply-config", "Applies the factory configuration.", arg -> {
            factory.config();
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
            for(String key:configured.keySet()){
                Log.info(key+":"+configured.get(key).getConfig().keySet().toString());
            }
        });

        handler.register("w-autoSave","[frequency]","Initializes autosave or stops it.",arg->{
            int frequency=defaultAutoSaveFrequency;
            if(!isInvalidArg(null,"Frequency",arg[0])) frequency=Integer.parseInt(arg[0]);
            if(frequency==0){
                Log.info("If you want kill-server command so badly, you can open an issue on github.");
                return;
            }
            autoSave(frequency);
        });
        handler.register("w", "<target> <property> <value>", "Sets property of target to value/integer.", arg -> {
            if (!configured.containsKey(arg[0])) {
                Log.info("Invalid target.Valid targets:" + configured.keySet().toString());
                return;
            }
            HashMap<String, Integer> config = configured.get(arg[0]).getConfig();
            if (!config.containsKey(arg[1])) {
                Log.info(arg[0] + " has no property " + arg[1] + ". Valid properties:" + config.keySet().toString());
                return;
            }
            if (isNotInteger(arg[2])) {
                Log.info(arg[2] + " is not an integer.");
                return;
            }
            config.put(arg[1], Integer.parseInt(arg[2]));
            Log.info("Property changed.");
        });

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("vote");

        handler.<Player>register("mkgf","<playerName>","adds ,or removes if payer is marked, griefer mark of given " +
                        "player name.",(arg, player) ->{
            Package p=antiGriefer.verify(player,arg[0],null,false);
            if(p==null) return;
            vote.aVote(antiGriefer,p,"[pink]"+p.object+"[] griefer mark on/of [pink]"+((Player)p.obj).name+"[]");
        });

        handler.<Player>register("maps","[page]","displays all maps",
                (arg, player) -> {
            int page=arg.length==0 || isInvalidArg(player,"Page",arg[0]) || arg[0].equals("0") ?
                    1:Integer.parseInt(arg[0]);
            Call.onInfoMessage(player.con, changer.info(page));
        });

        handler.<Player>register("l-info", "Shows how mani resource you have stored in the loadout and " +
                "traveling progress.", (arg, player) -> Call.onInfoMessage(player.con, loadout.info()));

        handler.<Player>register("f-info", "Shows how mani units you have in a hangar and building or " +
                "traveling progres.", (arg, player) -> Call.onInfoMessage(player.con, factory.info()));

        handler.<Player>register("l", "<fill/use> <itemName/all> <itemAmount>",
                "Fill loadout with resources from core/send resources from loadout to core", (arg, player) -> {
            if (isInvalidArg(player, "Item amount", arg[2])) return;
            boolean use = arg[0].equals("use");
            Package p = loadout.verify(player, arg[1], arg[2], use);
            if (p == null) {
                return;
            }
            String where = use ? "core" : "loadout";
            vote.aVote(loadout, p, "launch [orange]" + (p.object.equals("all") ? p.amount + " of all" : p.amount + " " + p.object) + "[] to " + where);
        });

        handler.<Player>register("f", "<build/send> <unitName/all> [unitAmount]",
                "Build amount of unit or Send amount of units from hangar.",
                (arg, player) -> {
            String thirdArg = arg.length == 3 ? arg[2] : "1";
            if (isInvalidArg(player, "Unit amount", thirdArg)) return;
            boolean send = arg[0].equals("send");
            Package p = factory.verify(player, arg[1],thirdArg, send);
            if (p == null) {
                return;
            }
            String what = send ? "send" : "build";
            vote.aVote(factory, p, what + " " + report(p.object, p.amount) + " units");
        });

        handler.<Player>register("f-price", "<unitName> [unitAmount]",
                "Shows price of given amount of units.", (arg, player) -> {
                    int amount = arg.length == 1 || isNotInteger(arg[1]) ? 1 : Integer.parseInt(arg[1]);
                    Call.onInfoMessage(player.con, factory.price(player, arg[0], amount));
                });

        handler.<Player>register("build-core", "<small/normal/big>", "Makes new core.",
                (arg, player) -> {
            Package p = builder.verify(player, arg[0], null, true);
            if (p == null) {
                return;
            }
            vote.aVote(builder, p, "building " + arg[0] + " core");
        });

        handler.<Player>register("vote", "<map/skipwave/restart/gameover> [indexOrName/waveAmount]", "Opens vote session.",
                (arg, player) -> {
            Package p;
            String secArg = arg.length == 2 ? arg[1] : "0";
            switch (arg[0]) {
                case "map":
                    p = changer.verify(player, secArg, null, false);
                    if (p == null) return;
                    vote.aVote(changer, p, "changing map to" + ((mindustry.maps.Map)p.obj).name() + ". ");
                    return;
                case "skipwave":
                    if (isInvalidArg(player, "Wave amount", secArg)) return;
                    p = skipper.verify(player, null, secArg, false);
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
                default:
                    player.sendMessage(prefix + "Invalid first argument");
            }
        });
    }
}
