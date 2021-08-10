package org.asf.mods.mc2fa.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import modkit.commands.Command;
import modkit.commands.CommandManager;
import net.minecraft.commands.CommandSourceStack;

public class Mc2FACommand implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.admin.servertools.mc2fa";
	}

	@Override
	public String getId() {
		return "st2fa";
	}

	@Override
	public String getDisplayName() {
		return "ServerTools MC2FA";
	}

	@Override
	public String getDescription() {
		return "MC2FA Command Line Utilitiy";
	}

	@Override
	public String getUsage() {
		return "<reload/revoke/authorize> <arguments...>";
	}

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {
		return cmd;
	}

	@Override
	public Command[] childCommands() {
		return new Command[] { new ReloadCommand(), new AuthorizeCommand(), new RevokeCommand() };
	}

	@Override
	public int execute(CommandExecutionContext context) {
		return 1;
	}
}
