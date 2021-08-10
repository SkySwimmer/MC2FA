package org.asf.mods.mc2fa;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import modkit.config.ConfigManager;
import modkit.util.ContainerConditions;
import modkit.util.EventUtil;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.mods.mc2fa.config.EndpointConfiguration;
import org.asf.mods.mc2fa.config.MessageConfiguration;
import org.asf.mods.mc2fa.config.Users;

import com.google.gson.JsonObject;

public class Server2FAMod extends AbstractMod {

	private ConfigManager<Server2FAMod> configManager;
	private EndpointConfiguration config;
	private Users userConfig;
	private MessageConfiguration messageConfig;

	public MessageConfiguration getMessageConfig() {
		return messageConfig;
	}

	public EndpointConfiguration getEndpoints() {
		return config;
	}

	public Users getUsers() {
		return userConfig;
	}

	@Override
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest) {
		super.setup(modloader, side, manifest);

		EventUtil.registerContainer(ContainerConditions.COMMON, this::commonEvents);
		EventUtil.registerContainer(ContainerConditions.COMMON, this::customCommands);
	}

	@AttachEvent(value = "mods.init", synchronize = true)
	public void init() throws IOException {

		configManager = ConfigManager.getFor(Server2FAMod.class);
		config = configManager.getConfiguration(EndpointConfiguration.class);
		userConfig = configManager.getConfiguration(Users.class);
		messageConfig = configManager.getConfiguration(MessageConfiguration.class);

		info(messageConfig.systemMessagePrefix + " " + messageConfig.systemMessageColor + "Running MC2FA!");
		info(messageConfig.systemMessagePrefix + " " + messageConfig.systemMessageColor + "Loaded "
				+ userConfig.users.size() + " user(s) from the config files.");

	}

	private String commonEvents() {
		return getClass().getPackageName() + ".events.CommonEvents";
	}

	private String customCommands() {
		return getClass().getPackageName() + ".events.CommandEvents";
	}

	public String buildURL(String endpoint, Map<String, String> variables) {
		String base = getEndpoints().server;
		if (!base.endsWith("/"))
			base += "/";
		if (endpoint.startsWith("/"))
			endpoint = endpoint.substring(1);
		String url = base + endpoint;
		for (String key : variables.keySet())
			url = url.replace("%" + key + "%", variables.get(key));
		return url;
	}

	private String createAuthRequest(String token, String user) {
		JsonObject obj = new JsonObject();
		obj.addProperty("token", token);
		obj.addProperty("username", user);
		obj.addProperty("timestamp", System.currentTimeMillis());
		return obj.toString();
	}

	public void logout(String token, String user) throws IOException {
		String body = createAuthRequest(token, user);

		HttpURLConnection conn = (HttpURLConnection) new URL(buildURL(getEndpoints().logoutEndpoint, Map.of()))
				.openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestMethod("POST");
		conn.getOutputStream().write(body.getBytes());
		conn.getInputStream().readAllBytes();
		conn.disconnect();
	}

}
