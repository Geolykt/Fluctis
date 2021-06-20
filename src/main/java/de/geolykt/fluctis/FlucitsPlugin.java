package de.geolykt.fluctis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import de.geolykt.fluctis.api.Market;
import de.geolykt.fluctis.api.MarketItem;
import de.geolykt.fluctis.api.NullUtils;
import de.geolykt.fluctis.impl.CompatibillityAdapter;
import de.geolykt.fluctis.impl.InventorySlate;
import de.geolykt.fluctis.impl.MaterialMarket;
import de.geolykt.fluctis.impl.MaterialMarketItem;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.GuiPageElement.PageAction;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.milkbowl.vault.economy.Economy;

public class FlucitsPlugin extends JavaPlugin {

    private static final @NotNull Player asPlayer(HumanEntity e) {
        if (e == null) {
            throw new IllegalStateException();
        }
        if (e instanceof Player) {
            return (Player) e;
        } else {
            return NullUtils.requireNotNull(Bukkit.getPlayer(e.getUniqueId()), "Unable to find player!");
        }
    }
    /**
     * Gives the player the given Items. All items that do not fit in the inventory will be dropped.
     * 
     * @param p The player receiving the Item
     * @param base The base ItemStack, the "amount" variable will be overridden and is thus irrelevant.
     * @param amount the amount of times the player should be rewarded the item specified in the base.
     * @version 20062021
     */
    public static void givePlayerItem (Player p, ItemStack base, Integer amount) {
        double numStacks = amount/base.getMaxStackSize() - 1;
        base.setAmount(base.getMaxStackSize());
        for (int i = 0; i < numStacks; i++) {
                amount -= base.getMaxStackSize();
                var remaining = p.getInventory().addItem(base);
                if (!remaining.isEmpty()) {
                    for (var entry : remaining.entrySet()) {
                        ItemStack is = entry.getValue();
                        if (is == null) {
                            continue;
                        }
                        p.getWorld().dropItem(p.getLocation(), is);
                    }
                }
        }
        base.setAmount(amount);
        var remaining = p.getInventory().addItem(base);
        if (!remaining.isEmpty()) {
            for (var entry : remaining.entrySet()) {
                ItemStack is = entry.getValue();
                if (is == null) {
                    continue;
                }
                p.getWorld().dropItem(p.getLocation(), is);
            }
        }
    }

    public static boolean hasItem(Player player, Material mat, int amount) {
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() >= amount) {
                    amount = 0;
                    return true;
                } else {
                    amount -= stack.getAmount();
                }
            }
        }

        return amount == 0;
    }

    /**
     * Removes a certain number of an item stack of the given description from the
     * players inventory and returns true if the item stack was present their inventory.
     * Does not remove any items if the inventory does not have enough to cover the deducting amount.
     * Also takes in account the game mode of the player.
     *
     * @param player The player
     * @param mat The material to remove
     * @param amount The amount to remove
     * @return True if the operation succeed.
     */
    public static boolean removeItem(Player player, Material mat, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        Inventory inv = player.getInventory();

        if (!hasItem(player, mat, amount)) {
            return false;
        }

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() > amount) {
                    int remaining = stack.getAmount() - amount;
                    stack.setAmount(remaining);
                    inv.setItem(i, stack);
                    return true;
                } else {
                    amount -= stack.getAmount();
                    inv.clear(i);
                }
            }
        }
        return true;
    }

    private Economy eco;

    private final InventorySlate[] inventorySlates = new InventorySlate[27];

    private InventoryGui mainGui;

    private Market<Material> market;

    private final InventoryGui[] secondaryGuis = new InventoryGui[27];

    private GuiElement createElement(boolean isBuy, char c, Material material, double costPerItem, int amount, HumanEntity ent) {
        if (isBuy) {
            return new StaticGuiElement(c, new ItemStack(Material.BLUE_STAINED_GLASS, amount), (click) -> {
                ent.sendMessage(NullUtils.format("Buying %dx %s", amount, material.toString()));
                double totalCost = costPerItem * amount;
                Player p = asPlayer(ent);
                if (eco.has(p, totalCost)) {
                    p.playSound(ent.getEyeLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
                    eco.withdrawPlayer(p, totalCost);
                    givePlayerItem(p, new ItemStack(material), amount);
                    p.sendMessage(ChatColor.GREEN + "Deducteed " + eco.format(totalCost) + " from your balance.");
                    MarketItem<Material> marketItem = getMarket().getItem(material);
                    marketItem.setDelta(marketItem.getDelta() + amount);
                    p.closeInventory();
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        displayBuyGui(p, material, marketItem.getPrice());
                    }, 1L);
                } else {
                    p.playSound(ent.getEyeLocation(), Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);
                    p.sendMessage(ChatColor.RED + "You need at least " + eco.format(totalCost) + " to buy this.");
                }
                return true;
            }, new String[] {
                    NullUtils.format("%s%sBuy %dx", ChatColor.RESET, ChatColor.RED, amount),
                    NullUtils.format("%sCost: %s", ChatColor.RESET, eco.format(costPerItem * amount))
            });
        } else {
            return new StaticGuiElement(c, new ItemStack(Material.RED_STAINED_GLASS, amount), (click) -> {
                ent.sendMessage(NullUtils.format("Selling %dx %s", amount, material.toString()));
                double totalCost = costPerItem * amount;
                Player p = asPlayer(ent);
                if (removeItem(p, material, amount)) {
                    p.playSound(ent.getEyeLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
                    eco.depositPlayer(p, totalCost);
                    p.sendMessage(ChatColor.GREEN + "Added " + eco.format(totalCost) + " to your balance.");
                    MarketItem<Material> marketItem = getMarket().getItem(material);
                    marketItem.setDelta(marketItem.getDelta() - amount);
                    p.closeInventory();
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        displayBuyGui(p, material, marketItem.getPrice());
                    }, 1L);
                } else {
                    p.playSound(ent.getEyeLocation(), Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);
                    p.sendMessage(ChatColor.RED + "You do not have enough of this item in your inventory to sell.");
                }
                return true;
            }, new String[] {
                    NullUtils.format("%s%sSell %dx", ChatColor.RESET, ChatColor.RED, amount),
                    NullUtils.format("%sYou obtain: %s", ChatColor.RESET, eco.format(costPerItem * amount))
            });
        }
    }

    private void displayBuyGui(HumanEntity viewer, Material material, double costPerItem) {
        String title = material.toString().toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder(title);
        b.setCharAt(0, Character.toUpperCase(b.charAt(0)));
        b.insert(0, "Buy/Sell ");
        title = b.toString();
        b = null; // Prevent reuse
        InventoryGui gui = new InventoryGui(this, viewer, title, new String[] {
                " abcdefg ",
                "         ",
                " 0123456 "
        });
        gui.setFiller(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        // The buy elements
        gui.addElement(createElement(true, 'a', material, costPerItem, 1, viewer));
        gui.addElement(createElement(true, 'b', material, costPerItem, 2, viewer));
        gui.addElement(createElement(true, 'c', material, costPerItem, 4, viewer));
        gui.addElement(createElement(true, 'd', material, costPerItem, 8, viewer));
        gui.addElement(createElement(true, 'e', material, costPerItem, 16, viewer));
        gui.addElement(createElement(true, 'f', material, costPerItem, 32, viewer));
        gui.addElement(createElement(true, 'g', material, costPerItem, 64, viewer));
        // The sell elements
        gui.addElement(createElement(false, '0', material, costPerItem, 1, viewer));
        gui.addElement(createElement(false, '1', material, costPerItem, 2, viewer));
        gui.addElement(createElement(false, '2', material, costPerItem, 4, viewer));
        gui.addElement(createElement(false, '3', material, costPerItem, 8, viewer));
        gui.addElement(createElement(false, '4', material, costPerItem, 16, viewer));
        gui.addElement(createElement(false, '5', material, costPerItem, 32, viewer));
        gui.addElement(createElement(false, '6', material, costPerItem, 64, viewer));
        gui.build(viewer);
        gui.show(viewer);
    }

    private @NotNull Economy getEconomy() {
        Economy eco = this.eco;
        if (eco == null) {
            Object economy = null;
            try {
                @SuppressWarnings("unused") //It's actually used
                Class<?> clazz = net.milkbowl.vault.economy.Economy.class;
                RegisteredServiceProvider<@NotNull Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (economyProvider != null) {
                    economy = economyProvider.getProvider();
                }
            } catch (NoClassDefFoundError e) {
                getPluginLoader().disablePlugin(this);
                throw new IllegalStateException("This plugin requires an economy to function!");
            }
            eco = NullUtils.requireNotNull((Economy) economy, "This plugin requires an economy to function!");
            this.eco = eco;
        }
        return eco;
    }

    /**
     * Obtains the market that is used by the implementation.
     *
     * @return The currently used {@link Market}.
     * @throws IllegalStateException If the market is not yet registered.
     */
    public @NotNull Market<Material> getMarket() {
        Market<Material> m = market;
        if (m == null) {
            throw new IllegalStateException("The market is not yet registered.");
        }
        return m;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String @NotNull [] args) {
        InventoryGui gui = mainGui;
        if (gui == null) {
            gui = new InventoryGui(this, null, NullUtils.requireNotNull(getConfig().getString("title")), new String[] {
                    "abcdefghi",
                    "jklmnopqr",
                    "stuv%wxyz"
            });
            getEconomy();
            gui.setFiller(new ItemStack(Material.GRAY_STAINED_GLASS, 1));
            gui.addElement(new DynamicGuiElement('%', viewer -> {
                return new StaticGuiElement('%', new ItemStack (Material.PAPER), ChatColor.RESET + "Your balance: " + eco.format(eco.getBalance(asPlayer(viewer))));
            }));
            int i = 0;
            for (char c = 'a'; c <= 'z'; c++, i++) {
                if (!inventorySlates[i].isShop()) {
                    continue;
                }
                gui.addElement(c, inventorySlates[i].getIcon(), (click) -> {
                    click.getWhoClicked().closeInventory();
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        int index = click.getSlot();
                        if (index > 22) {
                            index--;
                        }
                        secondaryGuis[index].show(click.getWhoClicked());
                    }, 1L);
                    return true;
                }, (String[]) null);
                InventoryGui gui2 = new InventoryGui(this, NullUtils.requireNotNull(getConfig().getString("title")), new String[] {
                        "aaaaaaaaa",
                        "aaaaaaaaa",
                        "-aaaaaaa+"
                });
                gui2.setFiller(new ItemStack(Material.RED_STAINED_GLASS_PANE, 1));
                InventorySlate slate = inventorySlates[i];
                GuiElementGroup group = new GuiElementGroup('a');
                group.setFiller(new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1));
                Optional<Material[]> materials = slate.getMaterials();

                for (Material mat : materials.get()) {
                    final Material material = NullUtils.requireNotNull(mat);
                    mat = null; // Prevent reuse
                    MarketItem<Material> marketItem = market.getItem(material);
                    group.addElement(new DynamicGuiElement('5', viewer -> {
                        double cost = marketItem.getPrice();
                        return new StaticGuiElement('5', new ItemStack(material), (click) -> {
                            click.getWhoClicked().closeInventory();
                            Bukkit.getScheduler().runTaskLater(this, () -> {
                                displayBuyGui(asPlayer(viewer), material, cost);
                            }, 1L);
                            return true;
                        }, new String[] {
                            null, 
                            NullUtils.format("%s%sCurrent market price: %s", ChatColor.RESET, ChatColor.BOLD, eco.format(cost)),
                            NullUtils.format("%s%sCurrent market delta: %d", ChatColor.RESET, ChatColor.BOLD, marketItem.getDelta())
                        });
                    }));
                }
                gui2.addElement(group);
                gui2.addElement(new GuiPageElement('-', new ItemStack(Material.OAK_SIGN), PageAction.PREVIOUS, ChatColor.RESET + ChatColor.BOLD.toString() + "Go to previous page (%prevpage%)"));
                gui2.addElement(new GuiPageElement('+', new ItemStack(Material.OAK_SIGN), PageAction.NEXT, ChatColor.RESET + ChatColor.BOLD.toString() + "Go to next page (%nextpage%)"));
                gui2.build();
                secondaryGuis[i] = gui2;
            }
            mainGui = gui;
            mainGui.build();
        }
        if (sender instanceof HumanEntity) {
            gui.show((HumanEntity) sender);
        }
        return true;
    }

    @Override
    public void onEnable() {
        File f1 = new File(getDataFolder(), "state1.dat");
        File f2 = new File(getDataFolder(), "state2.dat");
        if (f1.exists() || f2.exists()) {
            byte[] dataF1;
            byte[] dataF2;
            try (FileInputStream fis = new FileInputStream(f1)) {
                dataF1 = fis.readAllBytes();
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (FileInputStream fis = new FileInputStream(f2)) {
                dataF2 = fis.readAllBytes();
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            loadData(dataF1.length > dataF2.length ? dataF1 : dataF2);
        }
        saveDefaultConfig();
        CompatibillityAdapter util = new CompatibillityAdapter(this);
        EnumMap<@NotNull Material, @NotNull MarketItem<Material>> items = new EnumMap<>(Material.class);
        if (market instanceof MaterialMarket) {
            items = NullUtils.requireNotNull(((MaterialMarket) market).getElements());
        }
        for (int i = 0; i < 27; i++) {
            if (i == 26 || i == 18) {
                inventorySlates[i] = new InventorySlate();
                continue; // reserved for navigation elements
            }
            boolean shop = getConfig().getBoolean(NullUtils.format("menu.%d.shop", i), false);
            if (shop) {
                Collection<@NotNull Material> materials = util.getMaterialSet(getConfig(), NullUtils.format("menu.%d.items", i));
                for (Material mat : materials) {
                    if (!items.containsKey(mat)) {
                        items.put(mat, new MaterialMarketItem(mat, 0, 1.0));
                    }
                }
                Optional<@NotNull Material[]> opt = Optional.of(materials.toArray(new @NotNull Material[0]));
                if (opt == null) {
                    throw new InternalError();
                }
                String s = getConfig().getString(NullUtils.format("menu.%d.icon.material", i));
                Material m = Material.matchMaterial(NullUtils.requireNotNull(s));
                ItemStack is = new ItemStack(NullUtils.requireNotNull(m));
                ItemMeta im = NullUtils.requireNotNull(is.getItemMeta());
                String name = getConfig().getString(NullUtils.format("menu.%d.icon.name", i));
                if (name == null) {
                    name = NullUtils.format("menu.%d.icon.name", i);
                }
                im.setDisplayName(ChatColor.RESET + name);
                is.setItemMeta(im);
                inventorySlates[i] = new InventorySlate(opt, is);
            } else {
                inventorySlates[i] = new InventorySlate();
            }
        }
        market = new MaterialMarket(items);
    }

    public void save() {
        File f1 = new File(getDataFolder(), "state1.dat");
        File f2 = new File(getDataFolder(), "state2.dat");
        byte[] data = getSaveData();
        try (FileOutputStream fos = new FileOutputStream(f1)) {
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream fos = new FileOutputStream(f2)) {
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        save();
    }

    public void loadData(byte[] data) {
        List<MarketItem<Material>> materials = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            short namelen = buffer.getShort();
            byte[] nameBytes = new byte[namelen];
            for (int i = 0; i < namelen; i++) {
                nameBytes[i] = buffer.get();
            }
            String name = new String(nameBytes, StandardCharsets.UTF_8);
            long delta = buffer.getLong();
            @SuppressWarnings("unused")
            double min = buffer.getDouble();
            double multiplier = buffer.getDouble();
            Material mat = Material.matchMaterial(name);
            if (mat == null) {
                getLogger().warning("Material " + name + " couldn't be matched during the loading process. (corrupted savestate?)");
                continue;
            }
            materials.add(new MaterialMarketItem(mat, delta, multiplier));
        }
        EnumMap<@NotNull Material, @NotNull MarketItem<Material>> m = new EnumMap<>(Material.class);
        for (MarketItem<Material> item : materials) {
            m.put(item.getBacking(), item);
        }
        market = new MaterialMarket(m);
    }
    private byte[] getSaveData() {
        MarketItem<Material>[] mItems = getMarket().getItems().toArray(new MarketItem[0]);
        byte[][] names = new byte[mItems.length][];
        int i = 0;
        int bufferlen = names.length * 2; // header for the size of the names
        for (MarketItem<Material> item : mItems) {
            byte[] name = item.getBacking().toString().getBytes(StandardCharsets.UTF_8);
            names[i++] = name;
            bufferlen += name.length;
        }
        bufferlen += mItems.length * 24; // + delta, min, mul
        ByteBuffer buffer = ByteBuffer.allocate(bufferlen);
        for (i = 0; i < mItems.length; i++) {
            buffer.putShort((short) names[i].length);
            buffer.put(names[i]);
            MarketItem<Material> item = mItems[i];
            buffer.putLong(item.getDelta());
            buffer.putDouble(0);
            buffer.putDouble(item.getWorthMultiplier());
        }
        return buffer.array();
    }
}
