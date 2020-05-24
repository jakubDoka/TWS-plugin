package theWorst.discord;

import arc.util.Log;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.Role;

import java.util.Optional;

public abstract class RoleRestrictedCommand extends Command {
    public Role role = null;

    public RoleRestrictedCommand(String name) {
        super(name);
    }

    public RoleRestrictedCommand(String name,String argStruct) {
        super(name,argStruct);
    }

    @Override
    public boolean hasPerm(CommandContext ctx) {
        if (ctx.event.isPrivateMessage()) return false;
        if (role == null) return false;
        // i am simply not going to touch this
        return ctx.event.getMessageAuthor().asUser().get().getRoles(ctx.event.getServer().get()).contains(role);
    }
}
