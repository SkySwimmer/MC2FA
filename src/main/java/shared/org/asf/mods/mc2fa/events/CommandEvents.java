package org.asf.mods.mc2fa.events;

import modkit.commands.CommandManager;

import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.mods.mc2fa.commands.AccountCommand;
import org.asf.mods.mc2fa.commands.LogoutCommand;
import org.asf.mods.mc2fa.commands.Mc2FACommand;

public class CommandEvents implements IEventListenerContainer {

	@AttachEvent(value = "mods.preinit", synchronize = true)
	public void preInit() {
		CommandManager.getMain().registerCommand(new Mc2FACommand());
		CommandManager.getMain().registerCommand(new AccountCommand());
		CommandManager.getMain().registerCommand(new LogoutCommand());
	}

}
