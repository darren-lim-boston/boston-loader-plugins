package master;
import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;

public class Utils {

    public static enum GameItems {

        //misc
        MAGIC_WAND(Material.STICK, "Magic Wand", "It's magic!"),
        
        //flood items
        HELPING_HAND(Material.GOLDEN_HOE, "Helping Hand", "Right-click above a row of", "blocks to get help", "placing a new row of blocks down!"),

        //heat game items
        VILLAGER_COMPASS(Material.COMPASS, "Find nearest resident", "Finds the nearest overheating", "resident from your current location"),
        HEAT_TELEPORT_BOSTON_COMMONS(Material.GRASS_BLOCK, "T to Boston Commons", "Takes an expedited T", "to Boston Commons"),
        HEAT_TELEPORT_BPL(Material.BRICKS, "T to BPL", "Takes an expedited T", "to the Boston Public Library"),
        HEAT_TELEPORT_HOTEL(Material.GOLD_INGOT, "T to Hotel", "Takes an expedited T", "to the hotel"),
        COOLING_WATER(Material.SPLASH_POTION, "Cooling Water", "Splash onto residents to", "temporarily cool them down!", "Use your water wisely!"),

        //hurricane items
        LOUDSPEAKER(Material.SPYGLASS, "Loudspeaker - Click to use", "Can guide nearby residents to", "perform various actions"),
        HURRICANE_FOLLOW_PLAYER(Material.PLAYER_HEAD, "Tell nearby residents: FOLLOW ME!"),
        HURRICANE_STAY_IN_PLACE(Material.LEAD, "Tell nearby residents: STAY WHERE YOU ARE!"),
        ;

        public final Material itemMat;
        public final String name;
        public final String[] description;

        private GameItems(Material itemMat, String name, String... description) {
            this.itemMat = itemMat;
            this.name = ChatColor.GREEN + name;
            String[] lore = new String[description.length + 1];
            for(int i = 0; i < description.length; i++) {
                lore[i] = ChatColor.GRAY + description[i];
            }
            lore[lore.length - 1] = ChatColor.BLACK + "" + name().hashCode();
            this.description = lore;
        }
    }

    public static ItemStack getSpecialItem(GameItems gameItem) {
        ItemStack item = new ItemStack(gameItem.itemMat);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(gameItem.name);
        meta.setLore(Arrays.asList(gameItem.description));

        item.setItemMeta(meta);

        return item;
    }

    public static boolean isSpecialItem(ItemStack item, GameItems gameItem) {
        if(item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if(meta == null) {
            return false;
        }

        if(!meta.hasDisplayName() || !meta.hasLore()) {
            return false;
        }

        return meta.getLore().get(meta.getLore().size() - 1).equals(ChatColor.BLACK + "" + gameItem.name().hashCode());
    } 
}
