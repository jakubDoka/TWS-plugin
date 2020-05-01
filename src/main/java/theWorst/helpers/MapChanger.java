package theWorst.helpers;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Array;
import arc.util.Log;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
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
    public int waves=0;
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

    public String info(int page) {
        Array<mindustry.maps.Map> maps=Vars.maps.customMaps();
        int pageCount=(int)Math.ceil(maps.size/(float)pageSize);
        page= Mathf.clamp(page,1,pageCount);
        StringBuilder b=new StringBuilder();
        b.append("[orange]--MAPS(").append(page).append("/").append(pageCount).append(")--[]\n\n");
        for (int i=(page-1)*pageSize;i<page*pageSize && i<maps.size;i++){
            String m=maps.get(i).name();
            float r=data.get(m).getRating();
            b.append("[yellow]").append(i).append("[] | [gray]").append(m).append("[] | ");
            b.append(String.format("[%s]%f/10",r<3 ? r<6 ? "scarlet":"yellow":"green",r));
        }
        return b.toString();
    }

    public String getMapStats(String identifier){
        if (identifier==null) return data.get(world.getMap().name()).toString();
        Map map=findMap(identifier);
        if(map==null){
            return null;
        }
        if(!data.containsKey(map.name())) {
            return "[gray]No info about this map yet.";
        }
        return data.get(map.name()).toString();
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
        if(waves>md.waveRecord) md.waveRecord=waves;
        currentMap=null;
        waves=0;
    }

    public void rate(Player player,int rating){
        mapData md=data.get(world.getMap().name());
        md.ratings.put(player.uuid,(byte)rating);
        player.sendMessage(String.format(Main.prefix+"You gave [orange]%d/10[] to map [orange]%s[].",rating,world.getMap().name()));
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

        long getPlayRatio(){
            return playtime/Time.timeSinceMillis(bornDate);
        }

        public String toString(){
            return "[gray]times played:[] " + timesPlayed + "\n" +
                    "[gray]times won:[] " + timesWon + "\n" +
                    "[gray]wave record:[] " + timesWon + "\n" +
                    "[gray]server age:[] " + Main.milsToTime(Time.timeSinceMillis(bornDate)) + "\n" +
                    "[gray]total play time:[] " + Main.milsToTime(playtime) + "\n" +
                    "[gray]rating:[] " + getRating() + "/10";
        }

        public float getRating(){
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
