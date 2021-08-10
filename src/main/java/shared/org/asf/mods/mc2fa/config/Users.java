package org.asf.mods.mc2fa.config;

import java.io.IOException;
import java.util.HashMap;

import modkit.config.ModConfiguration;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;
import org.asf.cyan.api.config.annotations.Exclude;
import org.asf.mods.mc2fa.Server2FAMod;

public class Users extends ModConfiguration<Users, Server2FAMod> {

	public Users(Server2FAMod instance) throws IOException {
		super(instance);
	}

	public static class User extends Configuration<User> {

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		@Comment("The player name")
		public String playername;

		@Comment("The discord username")
		public String username;

		@Comment("The authorization token")
		public String token;
		
		@Exclude
		public long expiry = -1;

	}

	@Override
	public String filename() {
		return "users.ccfg";
	}

	@Comment("The authorized user list")
	public HashMap<String, User> users = new HashMap<String, User>();

}
