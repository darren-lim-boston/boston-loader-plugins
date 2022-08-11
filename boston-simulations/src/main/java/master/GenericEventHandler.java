package master;

import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GenericEventHandler {

    public void onPlayerChat(AsyncPlayerChatEvent event) {}
    public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {}
    public void onPlayerInteract(PlayerInteractEvent event) {}
    public void onFallingBlockLand(EntityChangeBlockEvent event) {}
    public void onInventoryClick(InventoryClickEvent event) {}
    public void onPotionSplash(PotionSplashEvent event) {}
}