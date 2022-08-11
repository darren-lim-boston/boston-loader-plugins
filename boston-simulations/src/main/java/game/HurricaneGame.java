package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import master.GameMaster;
import master.GenericEventHandler;
import master.Utils;
import master.Utils.GameItems;
import narrative.NarrativeEntity;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;

public class HurricaneGame extends Game {

    public enum HurricaneZone {
        EVACUATION_ZONE, DANGER_ZONE, CAUTION_ZONE
    }

    public HurricaneGame(GameMaster master) {
        super(master);
    }

    private static final String LOUDSPEAKER_INVENTORY_TITLE = "" + ChatColor.GREEN + ChatColor.BOLD + "SELECT COMMAND";

    private HurricaneInstance hurricane;

    private ClosestVillagerExtension closestVillager;
    private HashMap<Entity, NarrativeEntity> entityToNarrativeEntity;

    private HashMap<Entity, Boolean> isVillagerSafe;
    private HashMap<Entity, Boolean> hasPlayerInteractedWithVillager;
    private HashMap<Entity, Boolean> isVillagerInAir;

    @Override
    protected void runGameInit() {
        entityToNarrativeEntity = new HashMap<>();
        isVillagerSafe = new HashMap<>();
        isVillagerInAir = new HashMap<>();
        hasPlayerInteractedWithVillager = new HashMap<>();

        hurricane = new HurricaneInstance(master);

        for(NarrativeEntity nEnt : entities.values()) {
            nEnt.setTargetSpeed(0.8);

            Entity ent = nEnt.getEntity();
            entityToNarrativeEntity.put(ent, nEnt);
            isVillagerSafe.put(ent, true);
            isVillagerInAir.put(ent, false);
            hasPlayerInteractedWithVillager.put(ent, false);
            ent.setGlowing(true);
        }

        closestVillager = new ClosestVillagerExtension(master) {
            @Override
            public boolean shouldExcludeEntity(Entity ent) {
                return !ent.isGlowing();
            }
        };

        master.setEventHandler(new GenericEventHandler() {
            @Override
            public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
                Entity ent = event.getRightClicked();

                if(entityToNarrativeEntity.containsKey(ent)) {
                    NarrativeEntity nEnt = entityToNarrativeEntity.get(ent);

                    hasPlayerInteractedWithVillager.put(ent, true);

                    if(nEnt.getTargetPlayer() == null) {
                        entityToNarrativeEntity.get(ent).setTargetPlayer(event.getPlayer());
                        ent.setGlowing(false);
                        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The resident is now following you.");
                    } else {
                        entityToNarrativeEntity.get(ent).setTargetPlayer(null);
                        ent.setGlowing(ent.getLocation().getY() >= -52);
                        event.getPlayer().sendMessage(ChatColor.GRAY + "The resident is no longer following you.");
                    }
                }
            }

            @Override
            public void onPlayerInteract(PlayerInteractEvent event) {
                if(event.getItem() != null) {
                    Player p = event.getPlayer();
                    ItemStack item = event.getItem();
                    if(Utils.isSpecialItem(item, GameItems.LOUDSPEAKER)) {
                        event.setCancelled(true);

                        Inventory inventory = Bukkit.createInventory(null, 9, LOUDSPEAKER_INVENTORY_TITLE);
                        inventory.setItem(3, Utils.getSpecialItem(GameItems.HURRICANE_FOLLOW_PLAYER));
                        inventory.setItem(5, Utils.getSpecialItem(GameItems.HURRICANE_STAY_IN_PLACE));

                        p.openInventory(inventory);
                    }

                }
            }

            @Override
            public void onInventoryClick(InventoryClickEvent event) {
                if(event.getView().getTitle().equals(LOUDSPEAKER_INVENTORY_TITLE)) {
                    event.setCancelled(true);
                    if(event.getCurrentItem() != null && event.getWhoClicked() instanceof Player) {
                        ItemStack item = event.getCurrentItem();

                        Location loc = event.getWhoClicked().getLocation();
                        Vector dir = loc.getDirection().normalize().setY(0).multiply(8);
                        loc = loc.clone().add(dir);

                        boolean follow = true;
                        if(Utils.isSpecialItem(item, GameItems.HURRICANE_FOLLOW_PLAYER)) {
                            event.getWhoClicked().getWorld().playSound(loc, Sound.ENTITY_WANDERING_TRADER_TRADE, 1.5f, 1.5f);
                        } else if(Utils.isSpecialItem(item, GameItems.HURRICANE_STAY_IN_PLACE)) {
                            event.getWhoClicked().getWorld().playSound(loc, Sound.ENTITY_WANDERING_TRADER_NO, 1.5f, 1.5f);
                            follow = false;
                        }

                        for(int dx = -32; dx <= 32; dx += 2) {
                            for(int dz = -32; dz <= 32; dz += 2) {
                                for (int dy = -2; dy <= 2; dy += 2) {
                                    Location newLoc = loc.clone().add(dx, dy, dz);

                                    if(newLoc.getBlock().getType() == Material.AIR) {
                                        newLoc.getWorld().spawnParticle(Particle.GLOW, newLoc, 1);
                                    }
                                }
                            }
                        }

                        //get nearby villagers in view
                        for(Entity ent : loc.getWorld().getNearbyEntitiesByType(Villager.class, loc, 16, 2)) {
                            hasPlayerInteractedWithVillager.put(ent, true);
                            if(follow) {
                                entityToNarrativeEntity.get(ent).setTargetPlayer((Player) event.getWhoClicked());
                                ent.setGlowing(false);
                            } else {
                                entityToNarrativeEntity.get(ent).setTargetPlayer(null);
                                ent.setGlowing(ent.getLocation().getY() >= -52);
                            }
                        }

                        event.getInventory().close();
                    }
                }
            }

            @Override
            public void onFallingBlockLand(EntityChangeBlockEvent event) {
                FallingBlock block = (FallingBlock) event.getEntity();
                if(block.hasMetadata("hurricaneBlock")) {
                    for(Entity ent : block.getLocation().getNearbyEntitiesByType(Villager.class, 3, 1)) {
                        if(isVillagerSafe.containsKey(ent) && isVillagerSafe.get(ent)) {
                            markVillagerUnsafe(ent);
                        }
                    }
                }
            }
        });

        master.getWorld().setStorm(true);
        master.getWorld().setThundering(true);
        master.getWorld().setWeatherDuration(20000);
        master.getWorld().setThunderDuration(20000);
        master.getWorld().setTime(13000);
        master.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    private void markVillagerUnsafe(Entity ent) {
        isVillagerSafe.put(ent, false);
        ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1.5f, 0.5f);
        ent.setGlowing(false);
    }

    @Override
    protected BukkitRunnable getGameWorker() {
        BukkitRunnable run = new BukkitRunnable() {
            @Override
            public void run() {
                closestVillager.resetDistances();
                for(NarrativeEntity nEnt : entities.values()) {
                    Entity ent = nEnt.getEntity();
                    if(ent.isDead() || !ent.isValid()) {
                        continue;
                    }
                    if(!isVillagerSafe.get(ent)) {
                        continue;
                    }

                    if(ent.getLocation().clone().add(0, -1, 0).getBlock().getType() == Material.AIR && ent.getLocation().getY() > -42) {
                        if(!isVillagerInAir.get(ent)) {
                            isVillagerInAir.put(ent, true);
                        } else {
                            markVillagerUnsafe(ent);
                            continue;
                        }
                    } else {
                        isVillagerInAir.put(ent, false);
                    }

                    closestVillager.calculateDistanceSquaredToPlayer(ent);

                    if(nEnt.getTargetPlayer() == null) {
                        ent.setGlowing(ent.getLocation().getY() >= -52);
                    }
                }

                for(Player p : master.getPlayers()) {
                    closestVillager.updatePlayerCompass(p);

                    HurricaneZone zone = getZone(p.getLocation());

                    switch(zone) {
                        case DANGER_ZONE:
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.RED + "YOU ARE IN A DANGER ZONE"));
                            break;
                        case CAUTION_ZONE:
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.YELLOW + "YOU ARE IN A CAUTION ZONE"));
                            break;
                        case EVACUATION_ZONE:
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("" + ChatColor.RED + ChatColor.BOLD + "YOU ARE IN AN EVACUATION ZONE"));
                            break;
                    }
                }

                if(getSecondsLeft() <= 5 && !hurricane.isInProgress()) {
                    hurricane.startSimulation();
                }
            }
        };
        
        run.runTaskTimer(master.plugin, 0L, 20L);

        return run;
    }

    public static HurricaneZone getZone(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        if(z < -1.28 * x + 996.36) {
            if(z > -1.52 * x + 2445.84) {
                if(z >= -8300 && z <= -7800) {
                    if(z <= -8135) {
                        return HurricaneZone.EVACUATION_ZONE;
                    } else {
                        return HurricaneZone.DANGER_ZONE;
                    }
                }
            }
        }

        return HurricaneZone.CAUTION_ZONE;
    }

    @Override
    public List<ItemStack> getInitialPlayerItems() {
        return Arrays.asList(new ItemStack[] {
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            ClosestVillagerExtension.getCompass(),
            new ItemStack(Material.AIR), 
            Utils.getSpecialItem(GameItems.LOUDSPEAKER),
        });
    }

    @Override
    public String getGameName() {
        return "Hurricane";
    }

    @Override
    public String getGameGoal() {
        return "Get the residents to safety before the hurricane hits!";
    }

    @Override
    public String getGameTimeUntilEndText() {
        return "Time until hurricane";
    }

    @Override
    public int getInitialGameSeconds() {
        return master.getDifficulty() == GameMaster.GameDifficulty.EASY ? 60 * 10 : 60 * 7 + 30;
    }

    @Override
    public int getTimeoutGameSecondsBeforeEnd() {
        return 60;
    }

    @Override
    public Sound getGameMusic() {
        return Sound.MUSIC_UNDER_WATER;
    }

    @Override
    public int getCurrentScore() {
        int score = 0;

        for(Entity ent : isVillagerSafe.keySet()) {
            if(ent.isDead() || !ent.isValid()) {
                continue;
            }
            if(!hasPlayerInteractedWithVillager.get(ent)) {
                continue;
            }

            if(isVillagerSafe.get(ent)) {
                score++;
            }
        }

        return score;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(6957, -52, -8193);
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        Location[] locs = new Location[] {
            //4 buildings in the evacuation zone
            master.getLocation(7010, -52, -8161),
            master.getLocation(7025, -52, -8164),
            master.getLocation(7032, -52, -8174),
            master.getLocation(7024, -52, -8180),
            master.getLocation(7001, -51, -8134),
            master.getLocation(6995, -51, -8141),
            master.getLocation(7005, -51, -8122),
            master.getLocation(7002, -50, -8115),
            master.getLocation(7048, -52, -8134),
            master.getLocation(7033, -52, -8145),
            master.getLocation(7041, -52, -8145),
            master.getLocation(7024, -52, -8126),
            master.getLocation(7022, -51, -8113),
            master.getLocation(7009, -50, -8112),
            master.getLocation(7014, -50, -8112),
            master.getLocation(7018, -51, -8125),
            master.getLocation(7012, -50, -8104),
            master.getLocation(6988, -49, -8113),
            master.getLocation(6989, -49, -8132),
            //the park
            master.getLocation(7009, -52, -8197),
            master.getLocation(6996, -52, -8184),
            master.getLocation(6969, -52, -8175),
            master.getLocation(6953, -52, -8194),
            master.getLocation(6970, -52, -8200),
            master.getLocation(6928, -52, -8193),
            //second park
            master.getLocation(6904, -45, -8131),
            master.getLocation(6911, -46, -8135),
            master.getLocation(6930, -46, -8107),
            //along the danger zone
            //random squares and houses
            master.getLocation(6994, -49, -8127),
            master.getLocation(6993, -50, -8131),
            master.getLocation(6998, -50, -8132),
            master.getLocation(7033, -50, -8102),
            master.getLocation(7025, -50, -8100),
            master.getLocation(6982, -49, -8077),
            master.getLocation(6988, -51, -8030),
            master.getLocation(6971, -49, -8097),
            master.getLocation(6977, -49, -8106),
            master.getLocation(6973, -49, -8104),
            master.getLocation(6997, -50, -8072),
            master.getLocation(7008, -50, -8085),
            master.getLocation(6997, -50, -8097),
            master.getLocation(6983, -49, -8096),
            master.getLocation(6895, -48, -8091),
            master.getLocation(6907, -49, -8084),
            master.getLocation(6915, -49, -8079),
            master.getLocation(6932, -49, -8069),
            master.getLocation(6952, -50, -8057),
            master.getLocation(6900, -47, -8102),
            master.getLocation(6931, -48, -8087),
            master.getLocation(6943, -48, -8081),
            master.getLocation(6971, -50, -8033),
            master.getLocation(6964, -50, -8043),
            master.getLocation(6962, -50, -8051),
            master.getLocation(6943, -51, -8036),
            master.getLocation(6935, -52, -8033),
            master.getLocation(6995, -51, -8022),
            master.getLocation(6997, -51, -8015),
            master.getLocation(7006, -51, -8009),
            master.getLocation(7019, -51, -8005),
            master.getLocation(6916, -52, -8031),
            master.getLocation(6923, -51, -8043),
            master.getLocation(6917, -51, -8050),
            master.getLocation(6904, -51, -8056),
            master.getLocation(6896, -51, -8065),
            master.getLocation(6900, -51, -8054),
            master.getLocation(6902, -52, -8040),
        };

        List<NarrativeEntity> entities = new ArrayList<>();

        for(Location loc : locs) {
            entities.add(new NarrativeEntity(EntityType.VILLAGER, loc, true));
        }

        return entities;
    }
    
    @Override
    public String[] getTipMessage() {
        return new String[] {"Evacuate all residents from the Evacuation Zone, and shelter all residents in Danger Zones. Look for indoor basements for protective shelter!", "The lights on the ground will lead you to building entrances."};
    }
}
