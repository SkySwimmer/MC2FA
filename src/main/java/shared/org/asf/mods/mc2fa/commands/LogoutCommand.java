package org.asf.mods.mc2fa.commands;

import java.io.IOException;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.mods.mc2fa.Server2FAMod;
import org.asf.mods.mc2fa.config.Users.User;
import org.fusesource.jansi.Ansi.Color;

import modkit.commands.Command;
import modkit.util.Colors;
import net.minecraft.network.chat.TextComponent;

public class LogoutCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.player.servertools.mc2fa.logout";
	}

	@Override
	public String getId() {
		return "logout";
	}

	@Override
	public String getDisplayName() {
		return "Logout";
	}

	@Override
	public String getDescription() {
		return "Disconnects the user from the CMD-R authorization service and closes the connection.";
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public int execute(CommandExecutionContext context) {
		Server2FAMod mod = Server2FAMod.getInstance(Server2FAMod.class);

		if (context.getPlayer() == null) {
			context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
					+ "Only in-game players can use this command.\nUse 'st2fa revoke <player>' instead."));
			return 1;
		}

		User info = mod.getUsers().users.get(context.getPlayer().getUUID().toString());
		if (info == null) {
			context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
					+ "You have not been authorized, unable to log you off."));
			return 1;
		}

		boolean error = false;
		try {
			if (mod.getUsers().users.containsKey(context.getPlayer().getUUID().toString()))
				mod.getUsers().users.remove(context.getPlayer().getUUID().toString());

			mod.getUsers().writeAll();
			try {
				if (!info.token.equals("authorized-by-admin"))
					mod.logout(info.token, info.username);
			} catch (IOException e) {
				warn("Failed to fully disconnect " + context.getPlayer().getName().getString() + " from "
						+ info.username + ".");
			}
		} catch (IOException e) {
			error("Failed to save the MC2FA configuration!", e);
			error = true;
		}

		if (context.getPlayer() != null) {
			if (!error)
				context.getPlayer().connection.disconnect(new TextComponent(
						mod.getMessageConfig().systemMessagePrefix + "\n" + Colors.LIGHT_GREEN + "Logout successful."));
			else
				context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
						+ "Failed to log off! Check the server log!"));
		}

		return 0;
	}

}
