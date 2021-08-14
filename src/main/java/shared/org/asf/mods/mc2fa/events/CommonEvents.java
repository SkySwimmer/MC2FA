package org.asf.mods.mc2fa.events;

import modkit.enhanced.events.objects.player.PlayerLoginEventObject;
import modkit.enhanced.events.objects.server.ServerEventObject;
import modkit.enhanced.events.player.PlayerLoginEvent;
import modkit.enhanced.events.server.ServerStartupEvent;
import modkit.enhanced.player.EnhancedPlayer;
import modkit.events.network.ServerSideConnectedEvent;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.events.objects.resources.ResourceManagerEventObject;
import modkit.events.resources.manager.ResourceManagerStartupEvent;
import modkit.resources.Resource;
import modkit.resources.Resources;
import modkit.util.Colors;
import modkit.util.server.language.ClientLanguage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.mods.mc2fa.Server2FAMod;
import org.asf.mods.mc2fa.config.Users.User;
import org.asf.mods.mc2fa.gui.Menu;
import org.asf.mods.mc2fa.gui.Menu.ClickEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CommonEvents extends CyanComponent implements IEventListenerContainer {

	private Server2FAMod mod = Server2FAMod.getInstance(Server2FAMod.class);
	private HashMap<String, String> cachedTokens = new HashMap<String, String>();
	private HashMap<String, UserInfo> authenticatingUsers = new HashMap<String, UserInfo>();

	@SimpleEvent(ServerStartupEvent.class)
	public void startServer(ServerEventObject event) {
		Menu.initServer(event.getServer());
	}

	private class UserInfo {
		public String token;
		public String realm;

		public String username;
		public String nickname;
	}

	public String buildURL(String endpoint, Map<String, String> variables) {
		String base = mod.getEndpoints().server;
		if (!base.endsWith("/"))
			base += "/";
		if (endpoint.startsWith("/"))
			endpoint = endpoint.substring(1);
		String url = base + endpoint;
		for (String key : variables.keySet())
			url = url.replace("%" + key + "%", variables.get(key));
		return url;
	}

	@SimpleEvent(value = PlayerLoginEvent.class, synchronize = true)
	public void onJoin(PlayerLoginEventObject event) {
		try {
			String msg = validate(event.getPlayer().getId().toString(), event.getPlayer().getName());
			if (msg != null) {
				event.setDisconnectMessage(new TextComponent(msg));
				event.cancel();
			}
		} catch (IOException e) {
			event.setDisconnectMessage(new TextComponent(Colors.LIGHT_RED + "ERROR:\n" + Colors.GOLD
					+ "Could not contact the CMD-R 2FA authentication service.\n\nPlease try again later."));
			event.cancel();
		}
	}

	public String validate(String id, String name) throws IOException {
		boolean reauthenticate = false;
		String token = cachedTokens.getOrDefault(id, "undefined");

		if (mod.getUsers().users.containsKey(name)) {
			mod.getUsers().users.put(id, mod.getUsers().users.get(name));
			mod.getUsers().users.remove(name);
			mod.getUsers().writeAll();
		}
		if (!mod.getUsers().users.containsKey(id)) {
			reauthenticate = true;
		} else {
			User usr = mod.getUsers().users.get(id);
			try {
				reauth(null, usr.token, usr.username, id, name, usr.username, true);
			} catch (IOException e) {
				reauthenticate = true;
			}
		}

		if (reauthenticate) {
			try {
				URL u = new URL(
						buildURL(mod.getEndpoints().profileEndpoint, Map.of("token", URLEncoder.encode(token, "UTF-8"),
								"realm", URLEncoder.encode(mod.getEndpoints().realm, "UTF-8"))));

				InputStream strm = u.openStream();
				String str = new String(strm.readAllBytes());
				strm.close();

				JsonObject obj = JsonParser.parseString(str).getAsJsonObject();

				UserInfo info = new UserInfo();
				info.token = obj.get("token").getAsString();
				info.realm = obj.get("realm").getAsString();

				info.username = obj.get("username").getAsString();
				info.nickname = obj.get("nickname").getAsString();

				authenticatingUsers.put(id, info);
			} catch (IOException e1) {
				for (int i = 0; i < 2; i++) {
					try {
						URL u = new URL(buildURL(mod.getEndpoints().sessionTimeEndpoint,
								Map.of("token", URLEncoder.encode(token, "UTF-8"), "realm",
										URLEncoder.encode(mod.getEndpoints().realm, "UTF-8"))));

						InputStream strm = u.openStream();
						String str = new String(strm.readAllBytes());
						strm.close();

						long timeLeft = Long.parseLong(str) / 1000;
						String pretty = prettyTime(timeLeft);
						return mod.getMessageConfig().disconnectSplash.replace("@p", name)
								.replace("@r", mod.getEndpoints().realm).replace("@t", token).replace("@T", pretty);
					} catch (IOException e) {
						if (i == 1)
							throw e;
						else {
							String query = "realm=" + URLEncoder.encode(mod.getEndpoints().realm, "UTF-8");
							if (!mod.getEndpoints().guild.isEmpty()) {
								query += "&guild=" + URLEncoder.encode(mod.getEndpoints().guild, "UTF-8");
								if (!mod.getEndpoints().channel.isEmpty()) {
									query += "&channel=" + URLEncoder.encode(mod.getEndpoints().channel, "UTF-8");
								}
							}

							URL u = new URL(buildURL(mod.getEndpoints().genTokenEndpoint, Map.of("query", query)));
							InputStream strm = u.openStream();
							token = new String(strm.readAllBytes());
							cachedTokens.put(id, token);
							strm.close();
						}
					}
				}
			}
		}

		return null;
	}

	private String prettyTime(long timeLeft) {
		long seconds = timeLeft;
		long minutes = 0;
		while (seconds >= 60) {
			minutes++;
			seconds -= 60;
		}
		String message = "";
		if (minutes > 0)
			message += minutes + " minute" + (minutes == 1 ? "" : "s");
		if (seconds > 0) {
			if (!message.isEmpty())
				message += ", ";
			message += seconds + " second" + (minutes == 1 ? "" : "s");
		}
		return message;
	}

	@SuppressWarnings("resource")
	@SimpleEvent(ServerSideConnectedEvent.class)
	public void onJoin(ServerConnectionEventObject event) {
		event.getPlayer().setInvulnerable(false);
		EnhancedPlayer player = EnhancedPlayer.from(event.getPlayer());

		if (authenticatingUsers.containsKey(player.getUUID().toString())) {
			UserInfo user = authenticatingUsers.get(player.getUUID().toString());

			player.setInvisible(true);
			player.setInvulnerable(true);
			Vec3 pos = player.position();

			Thread th = new Thread(() -> {
				while (event.getServer().getPlayerList().getPlayer(player.getUUID()) != null && !(event.getServer()
						.getPlayerList().getPlayer(player.getUUID()).containerMenu instanceof Menu)) {
					player.setPos(pos.x, pos.y, pos.z);
					try {
						try {
							player.addEffect(
									new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 100, false, false));
						} catch (Exception e) {
						}
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break;
					}
				}
				boolean stop = false;
				int i2 = 0;
				int i = 0;
				while (event.getServer().getPlayerList().getPlayer(player.getUUID()) != null) {
					if (!(event.getServer().getPlayerList().getPlayer(player.getUUID()).containerMenu instanceof Menu)
							|| stop) {
						i++;
						if (i == 100) {
							player.connection.disconnect(new TextComponent("Disconnected"));

							try {
								URL u = new URL(buildURL(mod.getEndpoints().logoutSessionEndpoint,
										Map.of("token", URLEncoder.encode(user.token, "UTF-8"), "realm",
												URLEncoder.encode(mod.getEndpoints().realm, "UTF-8"))));

								InputStream strm = u.openStream();
								strm.readAllBytes();
								strm.close();
							} catch (IOException e) {
								stop = true;
							}

							break;
						}
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
						continue;
					}
					i2++;
					i = 0;
					if (i2 >= 10) {
						i2 = 0;
						try {
							URL u = new URL(buildURL(mod.getEndpoints().sessionTimeEndpoint,
									Map.of("token", URLEncoder.encode(user.token, "UTF-8"), "realm",
											URLEncoder.encode(mod.getEndpoints().realm, "UTF-8"))));

							InputStream strm = u.openStream();
							String str = new String(strm.readAllBytes());
							strm.close();

							long timeLeft = Long.parseLong(str) / 1000;
							if (timeLeft < 5) {
								stop = true;
							}
						} catch (IOException e) {
							stop = true;
						}
					}
					player.setPos(pos.x, pos.y, pos.z);
					try {
						try {
							player.addEffect(
									new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 100, false, false));
						} catch (Exception e) {
						}
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break;
					}
				}
			}, "Player Freeze Thread");
			th.setDaemon(true);
			th.start();

			Menu.open(player, 2, "Select login options", m -> {
				m.setItem(0, new ItemStack(Items.PLAYER_HEAD, 1));
				m.setItem(4, new ItemStack(Items.CLOCK, 1));
				m.setItem(9, new ItemStack(Items.BARRIER, 1));

				m.getItems().get(4).setHoverName(new TextComponent("Select a session period"));
				m.getItems().get(9).setHoverName(new TextComponent("Cancel login"));

				CompoundTag properties = new CompoundTag();
				properties.putString("SkullOwner", player.getName().getString());

				ListTag lst = new ListTag();
				CompoundTag display = new CompoundTag();

				JsonObject json = new JsonObject();
				json.addProperty("text", "Logging in as " + user.username);
				display.putString("Name", json.toString());

				json = new JsonObject();
				json.addProperty("text", "");
				lst.add(StringTag.valueOf(json.toString()));

				json = new JsonObject();
				json.addProperty("text", "Player name: " + player.getName().getString());
				lst.add(StringTag.valueOf(json.toString()));

				json = new JsonObject();
				json.addProperty("text", "Discord nickname: " + user.nickname);
				lst.add(StringTag.valueOf(json.toString()));

				json = new JsonObject();
				json.addProperty("text", "");
				lst.add(StringTag.valueOf(json.toString()));

				json = new JsonObject();
				json.addProperty("text", "Realm: " + user.realm);
				lst.add(StringTag.valueOf(json.toString()));

				json = new JsonObject();
				json.addProperty("text", "Token: " + user.token);
				lst.add(StringTag.valueOf(json.toString()));

				display.put("Lore", lst);
				properties.put("display", display);
				m.getItems().get(0).setTag(properties);

				for (int i = 1; i < 9; i++) {
					if (i > 6 || i == 5)
						m.setItem(9 + i, new ItemStack(Items.BLUE_WOOL, 1));
					else if (i > 2)
						m.setItem(9 + i, new ItemStack(Items.GREEN_WOOL, 1));
					else
						m.setItem(9 + i, new ItemStack(Items.ORANGE_WOOL, 1));
				}

				m.getItems().get(10).setHoverName(new TextComponent("2 hour session (unrecommended)"));
				m.getItems().get(11).setHoverName(new TextComponent("12 hour session (unrecommended)"));
				m.getItems().get(12).setHoverName(new TextComponent("1 day session"));
				m.getItems().get(13).setHoverName(new TextComponent("2 day session"));
				m.getItems().get(14).setHoverName(new TextComponent("1 week session (recommended)"));
				m.getItems().get(15).setHoverName(new TextComponent("1 month session"));
				m.getItems().get(16).setHoverName(new TextComponent("1 year session (recommended)"));
				m.getItems().get(17).setHoverName(new TextComponent("2 year session (recommended)"));

				m.attachOnClick(t -> {
					if (t.slot == 9)
						return 1;
					else if (t.slot > 9 && t.slot < 18) {
						int s = t.slot - 9;

						int time = 0;
						switch (s) {
						case 1:
							time = 2;
							break;
						case 2:
							time = 12;
							break;
						case 3:
							time = 24;
							break;
						case 4:
							time = 48;
							break;
						case 5:
							time = 168;
							break;
						case 6:
							time = 720;
							break;
						case 7:
							time = 8760;
							break;
						case 8:
							time = 17520;
							break;
						}

						try {
							URL u = new URL(buildURL(mod.getEndpoints().loginOptions,
									Map.of("hourconfig", Integer.toString(time), "token",
											URLEncoder.encode(user.token, "UTF-8"), "realm",
											URLEncoder.encode(mod.getEndpoints().realm, "UTF-8"))));
							u.openStream().close();
							keyboard(player, "Authorization code", "", code -> {
								validate(code, player, user);
							}, c -> {
								c.close();
							});
							return 2;
						} catch (IOException e) {
							if (authenticatingUsers.containsKey(player.getUUID().toString()))
								authenticatingUsers.remove(player.getUUID().toString());
							if (cachedTokens.containsKey(player.getUUID().toString()))
								cachedTokens.remove(player.getUUID().toString());
							player.connection.disconnect(
									new TextComponent(Colors.LIGHT_RED + "Failed to confirm session:\n" + Colors.GOLD
											+ "Could not confirm your session as it has expired, please re-connect."));
						}
						return 1;
					}

					return 0;
				});
			});
		}
	}

	private void validate(String code, EnhancedPlayer player, UserInfo user) {
		try {
			URL u = new URL(buildURL(mod.getEndpoints().codeEndpoint,
					Map.of("code", URLEncoder.encode(code, "UTF-8"), "token", URLEncoder.encode(user.token, "UTF-8"),
							"realm", URLEncoder.encode(mod.getEndpoints().realm, "UTF-8"))));
			InputStream strm = u.openStream();
			String str = new String(strm.readAllBytes());
			strm.close();

			reauth(t -> {
				player.connection.disconnect(t);
			}, str, "initial", player.getUUID().toString(), player.getName().getString(), user.username, false);
		} catch (IOException e) {
			keyboard(player, "Authorization code", "", c -> {
				validate(c, player, user);
			}, c -> {
				c.close();
			});
		}
	}

	private void reauth(Consumer<Component> disconnect, String token, String user, String id, String name,
			String lastuser, boolean throwError) throws IOException {
		if (token.equals("authorized-by-admin"))
			return;

		try {
			String newToken = authorize(token, user);
			JsonObject payload = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(newToken.split("\\.")[1]))).getAsJsonObject();

			User usr = new User();
			usr.username = payload.get("username").getAsString();
			usr.token = newToken;
			usr.playername = name;
			usr.expiry = payload.get("exp").getAsLong();

			mod.getUsers().users.put(id, usr);
			mod.getUsers().writeAll();

			if (authenticatingUsers.containsKey(id))
				authenticatingUsers.remove(id);
			if (cachedTokens.containsKey(id))
				cachedTokens.remove(id);
			if (!throwError)
				disconnect.accept(new TextComponent(
						mod.getMessageConfig().successMessage.replace("@p", name).replace("@d", usr.username)));
		} catch (IOException e) {
			if (throwError)
				throw e;

			disconnect.accept(new TextComponent(Colors.LIGHT_RED + "ERROR:\n" + Colors.GOLD
					+ "Token refresh failure, please contact the server administrator."));
			error("Token refresh failure! User: initial, Player: " + name + ", Last Discord Profile: " + lastuser, e);
		}
	}

	private String authorize(String token, String user) throws IOException {
		String body = createAuthRequest(token, "initial");

		HttpURLConnection conn = (HttpURLConnection) new URL(buildURL(mod.getEndpoints().refreshEndpoint, Map.of()))
				.openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestMethod("POST");
		conn.getOutputStream().write(body.getBytes());
		String response = new String(conn.getInputStream().readAllBytes());
		conn.disconnect();

		JsonObject res = JsonParser.parseString(response).getAsJsonObject();
		return res.get("token").getAsString();
	}

	private String createAuthRequest(String token, String user) {
		JsonObject obj = new JsonObject();
		obj.addProperty("token", token);
		obj.addProperty("username", user);
		obj.addProperty("timestamp", System.currentTimeMillis());
		return obj.toString();
	}

	private void keyboard(EnhancedPlayer player, String title, String initial, Consumer<String> result,
			Consumer<ClickEvent> cancelCallback) {
		Menu.open(player, 6, title + ": " + initial, t -> {
			for (int id = 0; id < 10; id++) {
				int v = id;
				if (v == 9)
					v = 0;
				else
					v = v + 1;

				int i = id;
				if (i > 4)
					i += 4;

				switch (v) {

				case 1:
					t.setItem(11 + i, new ItemStack(Items.WHITE_WOOL, 1));
					break;
				case 2:
					t.setItem(11 + i, new ItemStack(Items.ORANGE_WOOL, 1));
					break;
				case 3:
					t.setItem(11 + i, new ItemStack(Items.MAGENTA_WOOL, 1));
					break;
				case 4:
					t.setItem(11 + i, new ItemStack(Items.LIGHT_BLUE_WOOL, 1));
					break;
				case 5:
					t.setItem(11 + i, new ItemStack(Items.YELLOW_WOOL, 1));
					break;
				case 6:
					t.setItem(11 + i, new ItemStack(Items.LIME_WOOL, 1));
					break;
				case 7:
					t.setItem(11 + i, new ItemStack(Items.PINK_WOOL, 1));
					break;
				case 8:
					t.setItem(11 + i, new ItemStack(Items.GRAY_WOOL, 1));
					break;
				case 9:
					t.setItem(11 + i, new ItemStack(Items.CYAN_WOOL, 1));
					break;
				case 0:
					t.setItem(11 + i, new ItemStack(Items.BLACK_WOOL, 1));
					break;

				}

				t.getItems().get(11 + i).setHoverName(new TextComponent(Integer.toString(v)));
			}
			for (int id = 0; id < 6; id++) {
				int v = id;
				int i = id;
				if (i > 4)
					i += 4;

				t.setItem(29 + i, new ItemStack(Items.LIGHT_GRAY_WOOL, 1));
				t.getItems().get(29 + i).setHoverName(new TextComponent(Character.toString('a' + v)));
			}

			t.setItem(42, new ItemStack(Items.FEATHER, 1));
			t.getItems().get(42).setHoverName(new TextComponent("Backspace"));

			t.setItem(45, new ItemStack(Items.BARRIER, 1));
			t.getItems().get(45).setHoverName(new TextComponent("Cancel login"));

			t.setItem(53, new ItemStack(Items.GREEN_WOOL, 1));
			t.getItems().get(53).setHoverName(new TextComponent("Confirm login"));

			t.attachOnClick(c -> {
				if (c.item != null && c.slot < 54) {
					if (c.item.getHoverName().getString().equals("Backspace")) {
						String tx = initial;
						if (!tx.isEmpty())
							tx = tx.substring(0, initial.length() - 1);
						keyboard(player, title, tx, result, cancelCallback);
						return 2;
					} else if (c.item.getHoverName().getString().equals("Confirm login")) {
						new Thread(() -> {
							result.accept(initial);
						}, "Confirm handler").start();
						return 0;
					} else if (c.item.getHoverName().getString().equals("Cancel login")) {
						new Thread(() -> {
							cancelCallback.accept(c);
						}, "Cancel handler").start();
						return 2;
					} else {
						if ((initial + c.item.getHoverName().getString()).length() != 100)
							keyboard(player, title, initial + c.item.getHoverName().getString(), result,
									cancelCallback);
						else
							keyboard(player, title, initial, result, cancelCallback);
						return 2;
					}
				}
				return 0;
			});
		});
	}

	@SimpleEvent(ResourceManagerStartupEvent.class)
	public void resourceManagerStartup(ResourceManagerEventObject event) {
		loadLanguage(Resources.getFor(AbstractMod.getInstance(Server2FAMod.class)).getResource("lang/en_us.json"));
	}

	private void loadLanguage(Resource resource) {
		JsonObject lang = JsonParser.parseString(resource.readAsString()).getAsJsonObject();

		lang.entrySet().forEach(ent -> {
			ClientLanguage.registerLanguageKey(ent.getKey(), ent.getValue().getAsString());
		});
	}

}
