package game;

import java.util.HashMap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import master.GameMaster;
import master.Utils;
import master.Utils.GameItems;
import net.md_5.bungee.api.ChatColor;

public class ClosestVillagerExtension {

    private GameMaster master;
    private HashMap<Player, Entity> closestVillagers;
    private HashMap<Player, Double> closestVillagerDistances;

    public ClosestVillagerExtension(GameMaster master) {
        this.master = master;

        closestVillagers = new HashMap<>();
        closestVillagerDistances = new HashMap<>();
    }

    public boolean shouldExcludeEntity(Entity ent) {
        return false;
    }

    public static ItemStack getCompass() {
        return Utils.getSpecialItem(GameItems.VILLAGER_COMPASS);
    }

    public double getClosestDistance(Player p) {
        return closestVillagerDistances.getOrDefault(p, Double.MAX_VALUE);
    }

    public Entity getClosestEntity(Player p) {
        return closestVillagers.getOrDefault(p, null);
    }

    public double calculateDistanceSquaredToPlayer(Entity ent) {
        double smallestDist = Double.MAX_VALUE;
        double distSquared;
        for(Player p : master.getPlayers()) {
            if(shouldExcludeEntity(ent)) {
                distSquared = Double.MAX_VALUE;
            } else {
                distSquared = p.getLocation().distanceSquared(ent.getLocation());
            }
            if(distSquared < closestVillagerDistances.getOrDefault(p, distSquared + 1)) {
                closestVillagerDistances.put(p, distSquared);
                closestVillagers.put(p, ent);
            }

            smallestDist = Math.min(distSquared, smallestDist);
        }

        return smallestDist;
    }

    public void updatePlayerCompass(Player p) {
        if (closestVillagers.containsKey(p)) {
            p.setCompassTarget(getClosestEntity(p).getLocation());
        }

        ItemStack compass = null;
        if(Utils.isSpecialItem(p.getInventory().getItemInMainHand(), GameItems.VILLAGER_COMPASS)) {
            compass = p.getInventory().getItemInMainHand();
        } else if(Utils.isSpecialItem(p.getInventory().getItemInOffHand(), GameItems.VILLAGER_COMPASS)) {
            compass = p.getInventory().getItemInOffHand();
        }
        if(compass != null) {
            ItemMeta meta = compass.getItemMeta();
            if (closestVillagers.containsKey(p)) {
                meta.setDisplayName(GameItems.VILLAGER_COMPASS.name + ChatColor.GRAY + " (" + (int) Math.floor(Math.sqrt(getClosestDistance(p))) + " block(s) away)");
            } else {
                meta.setDisplayName(GameItems.VILLAGER_COMPASS.name + ChatColor.GRAY + " (calculating...)");
            }
            compass.setItemMeta(meta);
        }
    }

    public void resetDistances() {
        closestVillagers.clear();
        closestVillagerDistances.clear();
    }
}
