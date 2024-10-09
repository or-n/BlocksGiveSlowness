package com.example.BlocksGiveSlowness;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class BlocksGiveSlowness extends JavaPlugin implements Listener {

    private static final int BLOCKS_PER_SLOWNESS_LEVEL = 2;
    private static final int MAX_SLOWNESS_LEVEL = 6;
    private static final int MAX_BLOCKS =
        (MAX_SLOWNESS_LEVEL + 1) * BLOCKS_PER_SLOWNESS_LEVEL - 1;
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BlocksGiveSlowness Plugin enabled!");
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int blocks = dropExcessBlocks(player);
                    applySlownessEffect(player, blocks);
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        getLogger().info("BlocksGiveSlowness Plugin disabled!");
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (item.getType().isBlock()) {
            Player player = event.getPlayer();
            int blocks = countBlocks(player.getInventory());
            int canPickup = MAX_BLOCKS - blocks;
            if (canPickup <= 0) {
                event.setCancelled(true);
                return;
            }
            int amount = Math.min(item.getAmount(), canPickup);
            item.setAmount(amount);
            if (amount < item.getAmount()) {
                ItemStack remainingItem = item.clone();
                remainingItem.setAmount(item.getAmount() - amount);
                player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    remainingItem
                );
            }
            //applySlownessEffect(player);
        }
    }

    private int countBlocks(PlayerInventory inventory) {
        int blockCount = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType().isBlock()) {
                blockCount += item.getAmount();
            }
        }
        return blockCount;
    }

    private void applySlownessEffect(Player player, int blocks) {
        int slownessLevel = blocks / BLOCKS_PER_SLOWNESS_LEVEL;
        player.removePotionEffect(PotionEffectType.SLOW);
        if (slownessLevel == 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOW,
            Integer.MAX_VALUE,
            slownessLevel - 1,
            true,
            false
        ));
    }

    private int dropExcessBlocks(Player player) {
        int blocks = countBlocks(player.getInventory());
        if (blocks <= MAX_BLOCKS) {
            return blocks;
        }
        int excess = blocks - MAX_BLOCKS;
        List<ItemStack> droppedItems = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().isBlock()) {
                while (excess > 0 && item.getAmount() > 0) {
                    int amountToDrop = Math.min(item.getAmount(), excess);
                    droppedItems.add(new ItemStack(
                        item.getType(),
                        amountToDrop
                    ));
                    excess -= amountToDrop;
                    item.setAmount(item.getAmount() - amountToDrop);
                }
            }
        }
        for (ItemStack droppedItem : droppedItems) {
            player.getWorld().dropItemNaturally(
                player.getLocation(),
                droppedItem
            );
        }
        return MAX_BLOCKS;
    }
}