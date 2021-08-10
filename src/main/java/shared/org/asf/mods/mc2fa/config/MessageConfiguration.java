package org.asf.mods.mc2fa.config;

import java.io.IOException;

import modkit.config.ModConfiguration;
import modkit.util.Colors;

import org.asf.cyan.api.config.annotations.Comment;
import org.asf.mods.mc2fa.Server2FAMod;

public class MessageConfiguration extends ModConfiguration<MessageConfiguration, Server2FAMod> {

	public MessageConfiguration(Server2FAMod instance) throws IOException {
		super(instance);
	}

	@Override
	public String filename() {
		return "messages.ccfg";
	}

	@Comment("The system message prefix (for the chat and in the console)")
	public String systemMessagePrefix = Colors.DARK_PURPLE + "[" + Colors.GOLD + "ServerTools" + Colors.RESET + ": "
			+ Colors.DARK_AQUA + "MC2FA" + Colors.DARK_PURPLE + "]";

	@Comment("The system message color")
	public String systemMessageColor = Colors.GOLD;

	@Comment("The message displayed when disconnecting an unauthorized user")
	public String disconnectSplash = Colors.GOLD + "Hello @p,\n" + Colors.DARK_GREEN
			+ "To join this server, you will need to authenticate through CMD-R.\n\n" + Colors.LIGHT_GREEN
			+ "Authorize your player by sending the following token.\n" + Colors.LIGHT_BLUE + "Authorization token: "
			+ Colors.LIGHT_PURPLE + "@t\n" + Colors.LIGHT_BLUE + "The token is valid for " + Colors.LIGHT_AQUA + "@T"
			+ Colors.LIGHT_BLUE + ".\n\n" + Colors.LIGHT_GREEN + "Full CMD-R Command:\n" + Colors.UNDERLINE
			+ Colors.LIGHT_PURPLE + "+authorize @r @t" + Colors.RESET;

	@Comment("The displayed when disconnecting after authorizing a user")
	public String successMessage = Colors.LIGHT_GREEN
			+ "Authorization completed!\nYou can now reconnect to join the game!\n\n" + Colors.LIGHT_PURPLE
			+ "Connected Discord account: @d\nMinecraft playername: @p";

}
