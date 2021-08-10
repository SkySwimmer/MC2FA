package org.asf.mods.mc2fa.commands;

import java.io.IOException;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.mods.mc2fa.Server2FAMod;
import org.asf.mods.mc2fa.config.Users.User;
import org.fusesource.jansi.Ansi.Color;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import modkit.commands.Command;
import modkit.commands.CommandManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public class AuthorizeCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.admin.servertools.mc2fa.authorize";
	}

	@Override
	public String getId() {
		return "authorize";
	}

	@Override
	public String getDisplayName() {
		return "Authorize";
	}

	@Override
	public String getDescription() {
		return "Authorizes a player";
	}

	@Override
	public String getUsage() {
		return "<player>";
	}

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {
		
		cmd = cmd.requires(t -> hasPermission(t));

		CommandContainer cont = CommandContainer.getFor(this);
		cont.add(Commands.argument("player", StringArgumentType.string()));
		cont.attachPermission();
		cont.attachExecutionEngine();

		return cont.build(cmd);
	}

	@Override
	public int execute(CommandExecutionContext context) {
		Server2FAMod mod = Server2FAMod.getInstance(Server2FAMod.class);
		String name = context.getArgument("player", String.class);

		boolean error = false;
		try {

			boolean found = false;
			for (User usr : mod.getUsers().users.values()) {
				if (usr.playername.equals(name)) {
					context.success(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " "
							+ mod.getMessageConfig().systemMessageColor + "Player " + name
							+ " had already been authorized, made the authorization permanent instead."));
					found = true;
					usr.token = "authorized-by-admin";
					break;
				}
			}
			if (!found) {
				User usr = new User();
				usr.expiry = -1;
				usr.playername = name;
				usr.username = "not logged in";
				usr.token = "authorized-by-admin";
				context.success(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " "
						+ mod.getMessageConfig().systemMessageColor + "Player " + name + " has been authorized!"));
				mod.getUsers().users.put(name, usr);
			}

			mod.getUsers().writeAll();
		} catch (IOException e) {
			error("Failed to save the MC2FA configuration!", e);
			error = true;
		}

		if (context.getPlayer() != null) {
			if (error)
				context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
						+ "Failed to authorize " + name + "! Check the server log!"));
		}
		return 0;
	}

}
