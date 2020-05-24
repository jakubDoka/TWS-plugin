package theWorst;

import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.ItemModule;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.json.simple.JSONObject;
import theWorst.dataBase.Database;
import theWorst.dataBase.PlayerData;
import theWorst.dataBase.Rank;
import theWorst.discord.Command;
import theWorst.discord.CommandContext;
import theWorst.discord.DiscordCommands;
import theWorst.discord.RoleRestrictedCommand;
import theWorst.helpers.MapManager;

import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

public class BotThread extends Thread{

    private final Thread mt;
    private  DiscordApi api;
    private  TextChannel linkedChannel;
    //private  TextChannel mapChannel;
    private  Role adminRole;
    public static String prefix="!";
    static final String configFile =Main.directory + "discordSettings.json";
    DiscordCommands handler = new DiscordCommands();
    public BotThread(Thread mt){
        this.mt=mt;
        Main.loadJson(configFile,(data)-> {
            if(data.containsKey("prefix")) prefix =(String) data.get("prefix");
            try {
                api = new DiscordApiBuilder().setToken((String) data.get("token")).login().join();
            } catch (Exception ex){
                Log.info("Could not connect to discord");
            }

            Optional<Role> role =api.getRoleById((String) data.get("adminRoleId"));
            if (!role.isPresent()) {
                Log.info("Unable to find admin role.");
                adminRole = null;
            } else {
                adminRole = role.get();
            }

            Optional<TextChannel> channel = api.getTextChannelById((String) data.get("liveChatChannelId"));
            if (!channel.isPresent()) {
                Log.info("Unable to link the live chat channel.");
                linkedChannel = null;
            } else {
                linkedChannel = channel.get();
            }

            /*channel = api.getTextChannelById((String) data.get("mapChannelId"));
            if (!channel.isPresent()) {
                Log.info("Unable to link the map channel.");
                mapChannel = null;
            } else {
                mapChannel = channel.get();
            }*/
        },this::createDefaultConfig);

        if(api==null) return;

        api.addMessageCreateListener(handler);
        registerCommands(handler);

        setDaemon(false);
        start();

        if(linkedChannel==null) return;

        netServer.admins.addChatFilter((player,message)->{
            linkedChannel.sendMessage(Main.cleanName(player.name)+" : "+message.substring(message.indexOf("]")+1));
            return message;
        });

        api.addMessageCreateListener((event)->{
            if(event.getMessageAuthor().isBotUser()) return;
            if(event.getChannel()==linkedChannel){
                if(event.getMessageContent().startsWith(prefix)) return;
                Call.sendMessage("DC[coral][[[royal]"+event.getMessageAuthor().getName()+"[]]:[]"+event.getMessageContent());
            }
        });


    }

    private void createDefaultConfig() {
        Main.saveJson(configFile,"Default "+configFile+"was created, edit it to connect your server with discord.",
                ()->{
                    JSONObject data = new JSONObject();
                    data.put("token","Replace this with your bot token.");
                    data.put("prefix","!");
                    data.put("adminRoleId","Id of administration role goes here.");
                    data.put("liveChatChannelId","Your id of channel that will serve communication between server and discord.");
                    //data.put("mapChannelId","id of channel for map posting here");
                    return data;
                });
    }

    @Override
    public void run() {
        while (this.mt.isAlive()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        api.disconnect();
    }

    private void registerCommands(DiscordCommands handler) {
        handler.registerCommand(new RoleRestrictedCommand("setrank","<name/id> <rank>") {
            {
                description = "Sets rank of the player, available just for admins.";
                role = adminRole;
            }
            @Override
            public void run(CommandContext ctx) {
                switch (Database.setRankViaCommand(ctx.args[0],ctx.args[1],false)){
                    case notFound:
                        ctx.reply("Player not found.");
                        break;
                    case notPermitted:
                        ctx.reply("Changing or assigning admin rank can be done only thorough terminal.");
                        break;
                    case invalidRank:
                        ctx.reply("Rank not found.\nRanks:" + Arrays.toString(Rank.values())+"\n" +
                                "Custom ranks:"+Database.ranks.keySet());
                        break;
                    case success:
                        ctx.reply("Rank successfully changed.");
                }
            }
        });

        handler.registerCommand(new Command("help") {
            {
                description = "Shows all commands and their description.";
            }
            @Override
            public void run(CommandContext ctx) {
                EmbedBuilder eb =new EmbedBuilder()
                        .setTitle("COMMANDS")
                        .setColor(Color.orange);
                EmbedBuilder eb2 =new EmbedBuilder()
                        .setTitle("ROLE RESTRICTED COMMANDS")
                        .setColor(Color.orange);
                StringBuilder sb=new StringBuilder(),sb2 =new StringBuilder();
                for(String s:handler.commands.keySet()){
                    Command c =handler.commands.get(s);
                    StringBuilder to = sb;
                    if(c instanceof RoleRestrictedCommand) to =sb2;
                    to.append("**").append(prefix).append(c.name).append("**");
                    to.append("-").append(c.argStruct);
                    to.append("-").append(c.description).append("\n");
                }
                ctx.channel.sendMessage(eb.setDescription(sb.toString()));
                ctx.channel.sendMessage(eb2.setDescription(sb2.toString()));
            }
        });

        handler.registerCommand(new Command("gamestate") {
            {
                description = "Shows information about current game state.";
            }
            @Override
            public void run(CommandContext ctx) {
                EmbedBuilder eb =new EmbedBuilder().setTitle("GAME STATE");
                if(Vars.state.is(GameState.State.playing)){
                    eb
                            .addField("map", Vars.world.getMap().name())
                            .addField("mode", Vars.state.rules.mode().name())
                            .addInlineField("players",String.valueOf(Vars.playerGroup.size()))
                            .addInlineField("wave",String.valueOf(Vars.state.wave))
                            .addInlineField("enemies",String.valueOf(Vars.state.enemies))
                            .setColor(Color.green);
                } else {
                    eb
                            .setColor(Color.red)
                            .setDescription("Server is not hosting at the moment.");
                }
                ctx.channel.sendMessage(eb);
            }
        });

        handler.registerCommand(new Command("players") {
            {
                description = "Shows list of online players.";
            }
            @Override
            public void run(CommandContext ctx) {
                StringBuilder sb = new StringBuilder();
                for(Player p:Vars.playerGroup){
                    PlayerData pd = Database.getData(p);
                    sb.append(pd.originalName).append(" | ").append(pd.trueRank.name()).append(" | ").append(pd.serverId).append("\n");
                }
                EmbedBuilder eb =new EmbedBuilder()
                        .setTitle("PLAYERS ONLINE")
                        .setColor(Color.green)
                        .setDescription(sb.toString());
                if(Vars.playerGroup.size()==0) eb.setDescription("No players online.");
                ctx.channel.sendMessage(eb);
            }
        });

        handler.registerCommand(new Command("resinfo") {
            {
                description = "Check the amount of resources in the core.";
            }
            public void run(CommandContext ctx) {
                if (!state.rules.waves) {
                    ctx.reply("Only available in survival mode!");
                    return;
                }
                // the normal player team is "sharded"
                Teams.TeamData data = state.teams.get(Team.sharded);
                if(data.cores.isEmpty()){
                    ctx.reply("No cores no resources");
                    return;
                }
                //-- Items are shared between cores
                CoreBlock.CoreEntity core = data.cores.first();
                ItemModule items = core.items;
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Resources in the core:");
                items.forEach((item, amount) -> eb.addInlineField(item.name, String.valueOf(amount)));
                ctx.channel.sendMessage(eb);
            }
        });

        handler.registerCommand(new Command("downloadmap","<mapName/id>") {
            {
                description = "Preview and download a server map in a .msav file format.";
            }
            public void run(CommandContext ctx) {

                Map found = MapManager.findMap(ctx.args[0]);

                if (found == null) {
                    ctx.reply("Map not found!");
                    return;
                }

                Fi mapFile = found.file;

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(found.name())
                        .setDescription(found.description())
                        .setAuthor(found.author())
                        .setColor(Color.orange);
                ctx.channel.sendMessage(embed, mapFile.file());
            }
        });

        handler.registerCommand(new Command("maps") {
            {
                description = "Shows all server maps and ids.";
            }
            @Override
            public void run(CommandContext ctx) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("MAP LIST")
                        .setColor(Color.orange);
                StringBuilder b =new StringBuilder();
                int i=0;
                for(Map map:Vars.maps.customMaps()){
                    double rating= MapManager.getMapRating(map);
                    b.append(i).append(" | ").append(map.name()).append(" | ").append(String.format("%.2f/10",rating)).append("\n");
                    i++;
                }
                embed.setDescription(b.toString());
                ctx.channel.sendMessage(embed);
            }
        });

        /*handler.registerCommand(new Command("postmap") {
            @Override
            public void run(CommandContext ctx) {
                Message message = ctx.event.getMessage();
                if(message.getAttachments().size() != 1 || !message.getAttachments().get(0).getFileName().endsWith(".msav")){
                    ctx.reply("You must have one .msav file in the same message as the command!");
                    message.delete();
                    return;
                }

                MessageAttachment a = message.getAttachments().get(0);
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle(a.getFileName().replace(".msav",""))
                        .setAuthor(ctx.author)
                        .setColor(Color.orange);
                message.delete();
                if(mapChannel!=null){
                    mapChannel.sendMessage(eb, ((Fi) a).file());
                    ctx.channel.sendMessage("Map posted.");
                } else{
                    ctx.channel.sendMessage(eb, a);
                }
            }
        });*/

    }
}
