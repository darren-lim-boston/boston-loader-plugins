package game;

import master.Utils;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import master.GameMaster;
import master.GenericEventHandler;
import narrative.NarrativeEntity;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.util.Vector;

import java.util.*;

public class FloodGame extends Game {

    public FloodGame(GameMaster master) {
        super(master);
    }

    private FloodInstance flood;

    private ClosestVillagerExtension closestVillager;
    private HashMap<Entity, NarrativeEntity> entityToNarrativeEntity;
    private HashMap<Entity, Boolean> isWaterDamaged;

    @Override
    protected void runGameInit() {
        closestVillager = new ClosestVillagerExtension(master);

        flood = new FloodInstance(master);

        entityToNarrativeEntity = new HashMap<>();
        isWaterDamaged = new HashMap<>();

        for(NarrativeEntity nEnt : entities.values()) {
            Entity ent = nEnt.getEntity();
            entityToNarrativeEntity.put(ent, nEnt);
            isWaterDamaged.put(ent, false);

            ent.setGlowing(true);
        }

        for(Player p : master.getPlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
        }

        master.setEventHandler(new GenericEventHandler() {
            @Override
            public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
                Entity ent = event.getRightClicked();

                if(entityToNarrativeEntity.containsKey(ent)) {
                    NarrativeEntity nEnt = entityToNarrativeEntity.get(ent);

                    if(nEnt.getTargetPlayer() == null) {
                        entityToNarrativeEntity.get(ent).setTargetPlayer(event.getPlayer());
                        ent.setGlowing(false);
                        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The resident is now following you.");
                    } else {
                        entityToNarrativeEntity.get(ent).setTargetPlayer(null);
                        ent.setGlowing(true);
                        event.getPlayer().sendMessage(ChatColor.GRAY + "The resident is no longer following you.");
                    }
                }
            }

            @Override
            public void onPlayerInteract(PlayerInteractEvent event) {
                if(event.getItem() != null) {
                    ItemStack item = event.getItem();

                    if(Utils.isSpecialItem(item, Utils.GameItems.HELPING_HAND)) {
                        Player p = event.getPlayer();
                        event.setCancelled(true);

                        if(event.getClickedBlock() == null) {
                            p.sendMessage(ChatColor.RED + "You must click the top of a block to use this item.");
                        } else {
                            Block block = event.getClickedBlock();
                            BlockData blockData = block.getBlockData().clone();

                            if(event.getBlockFace() != BlockFace.UP) {
                                p.sendMessage(ChatColor.RED + "You can only use the helping hand by clicking the top of a block.");
                            } else {
                                if(block.getLocation().clone().add(0, 2, 0).getBlock().getType() != Material.AIR) {
                                    p.sendMessage(ChatColor.RED + "You can only use the helping hand with blocks not obstructed from above.");
                                    return;
                                }

                                int timeout = 64;
                                LinkedList<Location> openList = new LinkedList<>();
                                Set<Location> closedList = new HashSet<>();
                                List<Location> closedListOrdered = new ArrayList<>();

                                openList.add(block.getLocation().add(0, 1, 0));

                                while(!openList.isEmpty() && timeout > 0) {
                                    Location loc = openList.pop();
                                    closedList.add(loc);
                                    closedListOrdered.add(loc);

                                    Vector[] transitions = new Vector[] {new Vector(1, 0, 0), new Vector(-1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1)};

                                    for(Vector v : transitions) {
                                        Location newLoc = loc.clone().add(v);

                                        if(closedList.contains(newLoc)) {
                                            continue;
                                        }

                                        if(newLoc.clone().add(0, -1, 0).getBlock().getType() != blockData.getMaterial()) {
                                            continue;
                                        }

                                        if(newLoc.getBlock().getType() == Material.AIR && newLoc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                                            openList.add(newLoc);
                                        }
                                    }

                                    timeout--;
                                }

                                //check requirements now
                                if(!p.getInventory().containsAtLeast(new ItemStack(blockData.getMaterial()), closedList.size())) {
                                    p.sendMessage(ChatColor.RED + "You don't have enough " + blockData.getMaterial().name().toLowerCase().replace("_", " ") + " to use the Helping Hand.");
                                    return;
                                }

                                int removedCount = closedList.size();
                                while(removedCount > 0) {
                                    p.getInventory().removeItemAnySlot(new ItemStack(blockData.getMaterial(), Math.min(64, removedCount)));
                                    removedCount -= Math.min(64, removedCount);
                                }

                                Iterator<Location> closedIterator = closedListOrdered.iterator();
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if(closedIterator.hasNext()) {
                                            Location loc = closedIterator.next();
                                            loc.getWorld().playSound(loc.clone().add(0, 1, 0), Sound.ITEM_HOE_TILL, 1, 1.5f);
                                            loc.getWorld().spawnFallingBlock(loc.clone().add(0.5, 1, 0.5), blockData);
                                        } else {
                                            cancel();
                                        }
                                    }
                                }.runTaskTimer(master.plugin, 0L, 1L);
                            }
                        }
                    }
                }
            }
        });

        master.getWorld().setStorm(false);
        master.getWorld().setThunderDuration(0);
        master.getWorld().setClearWeatherDuration(getInitialGameSeconds() + getTimeoutGameSecondsBeforeEnd());
        master.getWorld().setTime(2000);
        master.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
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

                    closestVillager.calculateDistanceSquaredToPlayer(ent);

                    if(isInTimeout() && ent.getLocation().getBlock().getType() == Material.WATER && !isWaterDamaged.get(ent)) {
                        isWaterDamaged.put(ent, true);
                        ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_VILLAGER_HURT, 2f, 0.5f);
                        ent.setGlowing(false);
                    }
                }

                for(Player p : master.getPlayers()) {
                    closestVillager.updatePlayerCompass(p);
                }

                if(getSecondsLeft() <= 192 && !flood.isInProgress()) {
                    flood.startSimulation();
                }
            }
        };
        
        run.runTaskTimer(master.plugin, 0L, 20L);

        return run;
    }

    @Override
    protected void runGameEnd() {
        flood.stop();
    }

    @Override
    public List<ItemStack> getInitialPlayerItems() {
        return Arrays.asList(new ItemStack[] {
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            ClosestVillagerExtension.getCompass(),
            new ItemStack(Material.AIR),
            Utils.getSpecialItem(Utils.GameItems.HELPING_HAND),
        });
    }

    @Override
    public String getGameName() {
        return "Flood";
    }

    @Override
    public String getGameGoal() {
        return "Protect the residents from the rising waters!";
    }

    @Override
    public String getGameTimeUntilEndText() {
        return "Time until flood";
    }

    @Override
    public int getInitialGameSeconds() {
        return master.getDifficulty() == GameMaster.GameDifficulty.EASY ? 60 * 10 : 60 * 7 + 30;
    }

    @Override
    public int getTimeoutGameSecondsBeforeEnd() {
        return 100;
    }

    @Override
    public Sound getGameMusic() {
        return Sound.MUSIC_DISC_BLOCKS;
    }

    @Override
    public int getCurrentScore() {
        int score = 0;

        for(Entity ent : isWaterDamaged.keySet()) {
            if(ent.isDead() || !ent.isValid()) {
                continue;
            }
            boolean waterDamaged = isWaterDamaged.get(ent);
            if(!waterDamaged) {
                score++;
            }
        }

        return score;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(7122, -52, -7789);
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        Location[] locs = new Location[]{
            //at the pier, inside
            master.getLocation(7197, -53, -7823),
            master.getLocation(7190, -53, -7826),
            master.getLocation(7188, -53, -7825),
            master.getLocation(7180, -53, -7832),
            master.getLocation(7169, -53, -7833),
            master.getLocation(7158, -53, -7839),
            master.getLocation(7138, -53, -7847),
            //at the park
            master.getLocation(7114, -52, -7822),
            master.getLocation(7122, -52, -7807),
            master.getLocation(7119, -52, -7797),
            master.getLocation(7107, -52, -7800),
            master.getLocation(7081, -52, -7786),
            master.getLocation(7074, -52, -7765),
            master.getLocation(7085, -52, -7761),
            master.getLocation(7102, -52, -7752),
            master.getLocation(7113, -52, -7759),
            master.getLocation(7119, -52, -7778),
            master.getLocation(7102, -52, -7781),
            master.getLocation(7092, -52, -7779),
            master.getLocation(7092, -52, -7786),
            //around
            master.getLocation(7144, -53, -7837),
            master.getLocation(7170, -53, -7822),
            master.getLocation(7192, -53, -7814),
            master.getLocation(7217, -52, -7825),
            master.getLocation(7192, -48, -7804),
            master.getLocation(7151, -53, -7749),
            master.getLocation(7161, -53, -7752),
            master.getLocation(7163, -53, -7754),
            master.getLocation(7169, -53, -7745),
            master.getLocation(7135, -53, -7746),
            master.getLocation(7120, -52, -7743),
            //in the door building
            master.getLocation(7143, -52, -7734),
            master.getLocation(7134, -52, -7727),
            master.getLocation(7100, -52, -7727),
            master.getLocation(7121, -52, -7728),
            master.getLocation(7143, -52, -7739),
        };

        List<NarrativeEntity> entities = new ArrayList<>();

        for(Location loc : locs) {
            entities.add(new NarrativeEntity(EntityType.VILLAGER, loc, true));
        } 

        return entities;
    }
    
    @Override
    public String[] getTipMessage() {
        return new String[] {"Right-click on the residents to ask them to follow you, and click them again to let them go. There are chests at the pier containing materials that may be useful.", "You may want to learn how to use the helping hand to build flood barriers quickly!"};
    }
}
