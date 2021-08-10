package org.asf.mods.mc2fa.gui;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import modkit.enhanced.player.EnhancedPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Menu extends ChestMenu {

	private String title;
	private Consumer<Menu> ctorCallback;
	private ArrayList<Function<ClickEvent, Integer>> handlers = new ArrayList<Function<ClickEvent, Integer>>();

	public static Menu get(int id, int rows, Inventory inv) {
		if (rows == 1)
			return new Menu(MenuType.GENERIC_9x1, id, rows, inv);
		else if (rows == 2)
			return new Menu(MenuType.GENERIC_9x2, id, rows, inv);
		else if (rows == 3)
			return new Menu(MenuType.GENERIC_9x3, id, rows, inv);
		else if (rows == 4)
			return new Menu(MenuType.GENERIC_9x4, id, rows, inv);
		else if (rows == 5)
			return new Menu(MenuType.GENERIC_9x5, id, rows, inv);
		else
			return new Menu(MenuType.GENERIC_9x6, id, rows, inv);
	}

	public Menu(MenuType<?> type, int id, int rows, Inventory inv) {
		super(type, id, inv, new SimpleContainer(9 * rows), rows);
	}

	public String getTitle() {
		return title;
	}

	public void setItem(int slot, ItemStack stack) {
		getSlot(slot).set(stack);
	}

	@Override
	public ItemStack clicked(int item, int var2, ClickType type, Player player) {
		if (ctorCallback == null) {
			return super.clicked(var2, var2, type, player);
		}
		if (player instanceof ServerPlayer) {
			EnhancedPlayer pl = EnhancedPlayer.from((ServerPlayer) player);
			ClickEvent ev = new ClickEvent();
			ev.slot = item;

			if (item > 0 && getSlot(item).hasItem())
				ev.item = getSlot(item).getItem();
			ev.player = pl;
			ev.type = type;
			ev.menu = this;

			for (Function<ClickEvent, Integer> handler : handlers) {
				int res = handler.apply(ev);
				if (res == 1) {
					ev.close();
					return ItemStack.EMPTY;
				} else if (res == 2)
					return ItemStack.EMPTY;
			}

			ev.close();
			open(pl, rows, title, ctorCallback);
		} else
			return super.clicked(var2, var2, type, player);
		return ItemStack.EMPTY;
	}

	private int rows;

	public static class ClickEvent {
		public Menu menu;

		public int slot;
		public ClickType type;
		public ItemStack item;
		public EnhancedPlayer player;

		public void close() {
			player.connection.send(new ClientboundContainerClosePacket(menu.containerId));
			player.containerMenu.removed(player);
			player.containerMenu = player.inventoryMenu;
		}

	}

	public void attachOnClick(Function<ClickEvent, Integer> handler) {
		handlers.add(handler);
	}

	@SuppressWarnings("resource")
	public static void open(EnhancedPlayer player, int rows, String title, Consumer<Menu> callback) {
		new Thread(() -> {
			if (player.getServer().getPlayerList().getPlayer(player.getUUID()) != null
					&& player.getServer().getPlayerList().getPlayer(player.getUUID()).containerMenu != null
					&& player.getServer().getPlayerList().getPlayer(player.getUUID()).containerMenu instanceof Menu) {
				player.connection.send(new ClientboundContainerClosePacket(player.containerMenu.containerId));
				player.containerMenu.removed(player);
				player.containerMenu = player.inventoryMenu;
			}

			// This prevents the client from going out of sync while switching menus
			while (player.getServer().getPlayerList().getPlayer(player.getUUID()) != null
					&& player.getServer().getPlayerList().getPlayer(player.getUUID()).containerMenu != null
					&& player.getServer().getPlayerList().getPlayer(player.getUUID()).containerMenu instanceof Menu)
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					return;
				}
			if (player.getServer().getPlayerList().getPlayer(player.getUUID()) != null)
				player.openMenu(new SimpleMenuProvider((id, inv, pl) -> {
					Menu m = Menu.get(id, rows, inv);
					m.title = title;
					m.ctorCallback = callback;
					m.rows = rows;
					callback.accept(m);
					return m;
				}, new TextComponent(title)));
		}, "Player Menu Loader").start();
	}

}
