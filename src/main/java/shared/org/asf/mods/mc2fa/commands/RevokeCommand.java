package org.asf.mods.mc2fa.commands;

import java.io.IOException;
import java.util.UUID;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.mods.mc2fa.Server2FAMod;
import org.asf.mods.mc2fa.config.Users.User;
import org.fusesource.jansi.Ansi.Color;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import modkit.commands.Command;
import modkit.commands.CommandManager;
import modkit.util.Colors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public class RevokeCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.admin.servertools.mc2fa.revoke";
	}

	@Override
	public String getId() {
		return "revoke";
	}

	@Override
	public String getDisplayName() {
		return "Revoke";
	}

	@Override
	public String getDescription() {
		return "Revokes a player's authorization";
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
	@SuppressWarnings("resource")
	public int execute(CommandExecutionContext context) {
		Server2FAMod mod = Server2FAMod.getInstance(Server2FAMod.class);
		String name = context.getArgument("player", String.class);

		boolean error = false;
		try {

			boolean found = false;
			for (String id : mod.getUsers().users.keySet()) {
				User usr = mod.getUsers().users.get(id);
				if (usr.playername.equals(name)) {
					context.success(new TextComponent(
							mod.getMessageConfig().systemMessagePrefix + " " + mod.getMessageConfig().systemMessageColor
									+ "Player " + name + " has been de-authorized!"));
					found = true;
					mod.getUsers().users.remove(id);

					try {
						if (!usr.token.equals("authorized-by-admin"))
							mod.logout(usr.token, usr.username);
					} catch (IOException e) {
						warn("Failed to fully disconnect " + name + " from " + usr.username + ".");
					}

					try {
						if (context.getServer().getPlayerList().getPlayer(UUID.fromString(id)) != null) {
							context.getServer().getPlayerList().getPlayer(UUID.fromString(id)).connection
									.disconnect(new TextComponent(
											mod.getMessageConfig().systemMessagePrefix + "\n" + Colors.LIGHT_RED
													+ "You have been de-authorized, connection terminated."));
						}
					} catch (Exception e) {
					}
					break;
				}
			}
			if (!found) {
				return 1;
			}

			mod.getUsers().writeAll();
		} catch (IOException e) {
			error("Failed to save the MC2FA configuration!", e);
			error = true;
		}

		if (context.getPlayer() != null) {
			if (error)
				context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
						+ "Failed to de-authorize " + name + "! Check the server log!"));
		}
		return 0;
	}

}
