package org.asf.mods.mc2fa.config;

import java.io.IOException;

import modkit.config.ModConfiguration;

import org.asf.cyan.api.config.annotations.Comment;
import org.asf.mods.mc2fa.Server2FAMod;

public class EndpointConfiguration extends ModConfiguration<EndpointConfiguration, Server2FAMod> {

	public EndpointConfiguration(Server2FAMod instance) throws IOException {
		super(instance);
	}

	@Override
	public String filename() {
		return "endpoints.ccfg";
	}

	@Comment("The base server address")
	public String server = "https://aerialworks.ddns.net/authservices/cmdr";

	@Comment("The authorization realm")
	public String realm = "minecraft";

	@Comment("The Discord server ID (optional)")
	public String guild = "";

	@Comment("Channel ID (optional, requires the 'guild' option)")
	public String channel = "";

	@Comment("The token generation endpoint (receives 'realm=<realm>[&guild=<guild>&channel=<channel>]' as query)")
	public String genTokenEndpoint = "/gentoken.html?%query%";

	@Comment("The token timer endpoint (outputs in miliseconds)")
	public String sessionTimeEndpoint = "/sessiontime.html/%realm%/%token%";

	@Comment("The profile endpoint")
	public String profileEndpoint = "/getlogin.html/%realm%/%token%";

	@Comment("The session logout endpoint")
	public String logoutSessionEndpoint = "/logoutsession.html/%realm%/%token%";

	@Comment("The login option endpoint")
	public String loginOptions = "/confirmlogin.html/%realm%/%token%?time=%hourconfig%";

	@Comment("The 2FA authorization code endpoint")
	public String codeEndpoint = "/authorizelogin.html/%realm%/%token%?code=%code%";

	@Comment("The refresh endpoint")
	public String refreshEndpoint = "/refresh.html";
	
	@Comment("The logout endpoint (for authorized users)")
	public String logoutEndpoint = "/logout.html";

}
