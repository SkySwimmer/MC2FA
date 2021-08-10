package org.asf.mods.mc2fa.commands;

import java.io.IOException;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.mods.mc2fa.Server2FAMod;
import org.fusesource.jansi.Ansi.Color;

import modkit.commands.Command;
import net.minecraft.network.chat.TextComponent;

public class ReloadCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.admin.servertools.mc2fa.reload";
	}

	@Override
	public String getId() {
		return "reload";
	}

	@Override
	public String getDisplayName() {
		return "Reload";
	}

	@Override
	public String getDescription() {
		return "Reloads all configurations";
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public int execute(CommandExecutionContext context) {
		Server2FAMod mod = Server2FAMod.getInstance(Server2FAMod.class);

		boolean error = false;
		try {
			mod.getEndpoints().readAll();
			mod.getUsers().readAll();
			mod.getEndpoints().readAll();
		} catch (IOException e) {
			error("Failed to reload the MC2FA configuration!", e);
			error = true;
		}

		if (context.getPlayer() == null) {
			if (!error)
				context.success(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " "
						+ mod.getMessageConfig().systemMessageColor + "Reloaded the configuration!"));
		} else {
			if (!error)
				context.success(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " "
						+ mod.getMessageConfig().systemMessageColor + "Reloaded the configuration!"));
			else
				context.failure(new TextComponent(mod.getMessageConfig().systemMessagePrefix + " " + Color.RED
						+ "Failed to reload the configuration! Check the server log!"));
		}
		return 0;
	}

}
