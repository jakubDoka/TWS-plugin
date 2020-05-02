package theWorst.helpers;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Array;
import arc.util.Log;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.maps.Map;
import theWorst.Main;
import theWorst.Package;
import theWorst.dataBase.PlayerData;
import theWorst.interfaces.Votable;


import java.io.*;

import arc.util.Time;
import java.util.ArrayList;
import java.util.HashMap;

import static mindustry.Vars.*;

public class MapChanger implements Votable {
    final String saveFile=Main.directory+"mapData.ser";
    int pageSize=15;
    public Map currentMap=maps.all().first();
    HashMap<String,mapData> data=new HashMap<>();


    @Override
    public void launch(Package p) {
        Map map=(Map)p.obj;
        if(map==null){
            Events.fire(new EventType.GameOverEvent(Team.crux));
            return;
        }

        Array<Player> players = new Array<>();
        for(Player player : playerGroup.all()) {
            players.add(player);
            player.setDead(true);
        }
        endGame(false);
        logic.reset();
        Call.onWorldDataBegin();
        world.loadMap(map, map.applyRules(Gamemode.survival));
        state.rules = world.getMap().applyRules(Gamemode.survival);
        logic.play();


        for(Player player : players){
            if(player.con == null) continue;

            player.reset();
            netServer.sendWorldData(player);
        }
    }

    @Override
    public Package verify(Player player, String object, int amount, boolean toBase) {

        Map map = findMap(object);

        if (map == null) {
            player.sendMessage(Main.prefix + "Map not found.");
            return null;
        }

        return new Package(object, map, player);
    }

    private Map findMap(String object) {
        Array<Map> mapList = maps.all();
        if (Main.isNotInteger(object)) {
            return maps.all().find(m -> m.name().equalsIgnoreCase(object.replace('_', ' '))
                    || m.name().equalsIgnoreCase(object));
        }
        int idx = Integer.parseInt(object);
        if (idx < mapList.size) {
            return maps.all().get(idx);
        }
        return null;
    }

    public String statistics(){
        Array<mindustry.maps.Map> maps=Vars.maps.customMaps();
        StringBuilder b=new StringBuilder().append("\n");
        int i=0;
        double bestRatio=0;
        for (Map m:maps){
            if(!data.containsKey(m.name())) continue;
            double r=data.get(m.name()).getPlayRatio();
            if (r>bestRatio) bestRatio=r;
        }
        for (Map m:maps){
            String nm=m.name();
            if(!data.containsKey(m.name())) continue;
            int r=(int)data.get(nm).getRating();
            int ra=(int)(data.get(nm).getPlayRatio()/bestRatio*10);
            b.append(i).append(" | ").append(nm).append(" | ");
            b.append(String.format("%d/10",r)).append(" | ");
            b.append("<").append(new String(new char[ra]).replace("\0", "="));
            b.append(new String(new char[10-ra]).replace("\0", "-")).append(">");
            b.append("\n");
            i++;
        }
        return b.toString();
    }

    public String info(int page) {
        Array<mindustry.maps.Map> maps=Vars.maps.customMaps();
        int pageCount=(int)Math.ceil(maps.size/(float)pageSize);
        page= Mathf.clamp(page,1,pageCount);
        StringBuilder b=new StringBuilder();
        b.append("[orange]--MAPS(").append(page).append("/").append(pageCount).append(")--[]\n\n");
        for (int i=(page-1)*pageSize;i<page*pageSize && i<maps.size;i++){
            String m=maps.get(i).name();
            int r=(int)data.get(m).getRating();
            b.append("[yellow]").append(i).append("[] | [gray]").append(m).append("[] | ");
            b.append(String.format("[%s]%d/10[]",r<6 ? r<3 ? "scarlet":"yellow":"green",r));
            b.append("\n");
        }
        return b.toString();
    }

    public String getMapStats(String identifier){
        Map map= identifier==null ? world.getMap():findMap(identifier);
        if(map==null){
            return null;
        }
        if(!data.containsKey(map.name())) {
            return "[gray]No info about this map yet.";
        }
        return "[orange]--MAP STATS--[]\n\n"+data.get(map.name()).toString();
    }

    public String getMapRules(String identifier){
        Map map= identifier==null ? world.getMap():findMap(identifier);
        if(map==null){
            return null;
        }
        Rules rules=map.rules();
        return "[orange]--MAP RULES--[]\n\n"+
                String.format("[gray]name:[] %s\n" +
                        "[gray]mode:[] %s\n" +
                        "[orange]Multipliers[]\n" +
                        "[gray]build cost:[] %.2f\n"+
                        "[gray]build speed:[] %.2f\n"+
                        "[gray]block health:[] %.2f\n"+
                        "[gray]unit build speed:[] %.2f\n"+
                        "[gray]unit damage:[] %.2f\n"+
                        "[gray]unit health:[] %.2f\n"+
                        "[gray]player damage:[] %.2f\n"+
                        "[gray]player health:[] %.2f\n"+
                "",map.name(),rules.mode().name(),
                rules.buildCostMultiplier,
                rules.buildSpeedMultiplier,
                rules.blockHealthMultiplier,
                rules.unitBuildSpeedMultiplier,
                rules.unitDamageMultiplier,
                rules.unitHealthMultiplier,
                rules.playerDamageMultiplier,
                rules.playerHealthMultiplier);
    }

    public void startGame() {
        currentMap=world.getMap();
        String name=currentMap.name();
        if(!data.containsKey(name)) data.put(name,new mapData(currentMap));
        else {
            data.get(name).started=Time.millis();
        }

    }

    public void endGame(boolean won) {
        if(currentMap==null){
            return;
        }
        mapData md=data.get(currentMap.name());
        md.playtime+=Time.timeSinceMillis(md.started);
        md.timesPlayed++;
        if(won) md.timesWon++;
        if(state.wave>md.waveRecord) md.waveRecord=state.wave;
        currentMap=null;
    }

    public void rate(Player player,int rating){
        mapData md=data.get(world.getMap().name());
        md.ratings.put(player.uuid,(byte)rating);
        player.sendMessage(String.format(Main.prefix+"You gave [orange]%d/10[] to map [orange]%s[].",rating,world.getMap().name()));
    }

    public void cleanup() {
        for(String md:data.keySet()){
            if(maps.byName(md)==null){
                data.remove(md);
            }
        }
    }

    static class mapData implements Serializable {
        int timesPlayed=0;
        int timesWon=0;
        int waveRecord=0;
        long started=Time.millis();
        long playtime=0;
        long bornDate=Time.millis();

        String name;
        HashMap<String,Byte> ratings=new HashMap<>();

        mapData(Map map){
            name=map.name();
        };

        double getPlayRatio(){
            return playtime/(double)Time.timeSinceMillis(bornDate);
        }

        public String toString(){
            return "[gray]name:[] " + name + "\n" +
                    "[gray]author:[] " + maps.byName(name).author() + "\n" +
                    "[gray]times played:[] " + timesPlayed + "\n" +
                    "[gray]times won:[] " + timesWon + "\n" +
                    "[gray]wave record:[] " + waveRecord + "\n" +
                    "[gray]server age:[] " + Main.milsToTime(Time.timeSinceMillis(bornDate)) + "\n" +
                    "[gray]total play time:[] " + Main.milsToTime(playtime) + "\n" +
                    "[gray]rating:[] " + getRating() + "/10";
        }

        public float getRating(){
            if (ratings.size()==0) return 0;
            float total=0;
            for(byte b:ratings.values()){
                total+=b;
            }
            return total/ratings.size();
        }


    }

    public void save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(data);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void load(){
        try {
            FileInputStream fileIn = new FileInputStream(saveFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object obj=in.readObject();
            if(obj instanceof HashMap) data = (HashMap<String, mapData>) obj;
            in.close();
            fileIn.close();
        } catch (ClassNotFoundException c) {
            Log.info("class not found");
            c.printStackTrace();
        } catch (FileNotFoundException f){
            Log.info("database no found, creating new");
        }catch (IOException i){
            Log.info("Database is incompatible with current version.");
        }
    }
}
