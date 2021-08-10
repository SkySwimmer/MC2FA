package org.asf.mods.mc2fa.commands;

import java.util.Date;

import org.asf.mods.mc2fa.Server2FAMod;
import org.asf.mods.mc2fa.config.Users.User;
import org.fusesource.jansi.Ansi.Color;

import modkit.commands.Command;
import modkit.util.Colors;
import net.minecraft.network.chat.TextComponent;

public class AccountCommand implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.player.servertools.mc2fa.account";
	}

	@Override
	public String getId() {
		return "account";
	}

	@Override
	public String getDisplayName() {
		return "Account";
	}

	@Override
	public String getDescription() {
		return "Retrieves a player's account information";
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
					+ "Only in-game players can use this command."));
			return 1;
		}

		User info = mod.getUsers().users.get(context.getPlayer().getUUID().toString());
		if (info == null) {
			context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
					+ "You have not been authorized, no account data was found."));
			return 1;
		}

		StringBuilder message = new StringBuilder();
		message.append(
				Colors.LIGHT_PURPLE + "Player name: " + Colors.LIGHT_BLUE + context.getPlayer().getName().getString())
				.append("\n");
		message.append(Colors.LIGHT_PURPLE + "Discord username: " + Colors.LIGHT_BLUE + info.username).append("\n");
		if (info.expiry != -1)
			message.append(Colors.LIGHT_PURPLE + "Login expiry: " + Colors.LIGHT_BLUE + new Date(info.expiry))
					.append("\n");
		else
			message.append(Colors.LIGHT_PURPLE + "Login expiry: " + Colors.LIGHT_BLUE + "never").append("\n");

		context.success(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " "
				+ mod.getMessageConfig().systemMessageColor + "\n\n" + message));

		return 0;
	}

}
