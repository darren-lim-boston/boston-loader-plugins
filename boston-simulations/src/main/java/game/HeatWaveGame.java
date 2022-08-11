package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.EntityEffect;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import master.GameMaster;
import master.GenericEventHandler;
import master.Utils;
import master.Utils.GameItems;
import narrative.NarrativeEntity;
import net.md_5.bungee.api.ChatColor;

public class HeatWaveGame extends Game {

    public class HeatWaveNarrativeResident extends NarrativeEntity {

        public HeatWaveNarrativeResident(Location loc) {
            super(EntityType.VILLAGER, loc, false);
        }

        @Override
        public void spawn(GameMaster master) {
            super.spawn(master);

            Villager v = (Villager) ent;

            v.setCustomNameVisible(true);
            v.setCustomName(ChatColor.WHITE + "☀");
        }
    }

    public class HeatWaveHomeNarrativeResident extends NarrativeEntity {

        public HeatWaveHomeNarrativeResident(Location loc) {
            super(EntityType.VILLAGER, loc, true);
        }

        @Override
        public void spawn(GameMaster master) {
            super.spawn(master);

            Villager v = (Villager) ent;

            v.setCustomNameVisible(true);
            v.setCustomName(ChatColor.WHITE + "☀");
        }
    }

    private HashMap<Entity, Integer> initialVillagerHealths;    
    private HashMap<Entity, Integer> villagerHealths;
    private Set<Entity> villagersCurrentlyHeating;
    private HashMap<Entity, NarrativeEntity> entityToNarrativeEntity;    

    private ClosestVillagerExtension closestVillager;

    private HashMap<Player, Long> lastPlayerHeatStroke;

    private HashMap<Player, Long> lastPlayerTeleport;

    private HashMap<Entity, Location> villagerACUnits;
    private HashMap<Location, Boolean> acStatuses;

    public HeatWaveGame(GameMaster master) {
        super(master);
    }

    @Override
    protected void runGameInit() {  
        villagerHealths = new HashMap<>();
        initialVillagerHealths = new HashMap<>();
        entityToNarrativeEntity = new HashMap<>();
        lastPlayerHeatStroke = new HashMap<>();
        villagersCurrentlyHeating = new HashSet<>();
        lastPlayerTeleport = new HashMap<>();
        villagerACUnits = new HashMap<>();
        acStatuses = new HashMap<>();

        closestVillager = new ClosestVillagerExtension(master);

        //TODO: spawn villagers and give them health of 60(?)
        for(NarrativeEntity ent : entities.values()) {
            villagerHealths.put(ent.getEntity(), 50 + (int) (Math.random() * 20));
            initialVillagerHealths.put(ent.getEntity(), villagerHealths.get(ent.getEntity()));
            entityToNarrativeEntity.put(ent.getEntity(), ent);

            closestVillager.calculateDistanceSquaredToPlayer(ent.getEntity());

            if(ent instanceof HeatWaveHomeNarrativeResident) {
                //find the nearest AC, y=-50
                LinkedList<Location> openList = new LinkedList<>();
                Set<Location> closedList = new HashSet<>();
                Location initialLoc = ent.getLocation().clone();
                initialLoc.setY(-50);
                openList.add(initialLoc);

                int timeout = 512;
                Location buttonLoc = null;
                main:
                while(timeout > 0 && !openList.isEmpty()) {
                    Location loc = openList.pop();
                    closedList.add(loc);

                    Vector[] transitions = new Vector[] {new Vector(1, 0, 0), new Vector(-1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1)};

                    for(Vector v : transitions) {
                        Location newLoc = loc.clone().add(v);

                        if(closedList.contains(newLoc)) {
                            continue;
                        }

                        Material type = newLoc.getBlock().getType();

                        if(type == Material.CRIMSON_BUTTON) {
                            buttonLoc = newLoc;
                            break main;
                        } else if(type == Material.AIR) {
                            openList.add(newLoc);
                        }
                    }

                    timeout--;
                }

                if(buttonLoc == null) {
                    master.broadcastMessage("ERROR: villager could not find AC: " + ent.getLocation().toVector());
                } else {
                    acStatuses.putIfAbsent(buttonLoc.getBlock().getLocation(), false);
                    villagerACUnits.put(ent.getEntity(), buttonLoc.getBlock().getLocation());
                }
            }
        }

        master.setEventHandler(new GenericEventHandler() {
            @Override
            public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
                Entity ent = event.getRightClicked();

                if(villagerHealths.containsKey(ent)) {
                    NarrativeEntity nEnt = entityToNarrativeEntity.get(ent);

                    if(nEnt instanceof HeatWaveHomeNarrativeResident) {
                        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The resident is refusing to leave their home.");
                        return;
                    }

                    int heatScore = villagerHealths.get(nEnt.getEntity());

                    if(heatScore == 0) {
                        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The resident is too exhausted to move...");
                        return;
                    }

                    if(nEnt.getTargetPlayer() == null) {
                        entityToNarrativeEntity.get(ent).setTargetPlayer(event.getPlayer());
                        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The resident is now following you.");
                        ent.setGlowing(false);
                    } else {
                        entityToNarrativeEntity.get(ent).setTargetPlayer(null);
                        event.getPlayer().sendMessage(ChatColor.GRAY + "The resident is no longer following you.");
                        ent.setGlowing(true);
                    }
                }
            }

            @Override
            public void onPlayerInteract(PlayerInteractEvent event) {
                if(event.getClickedBlock() != null) {
                    Player p = event.getPlayer();
                    Location loc = event.getClickedBlock().getLocation();
                    if(acStatuses.containsKey(loc) && !acStatuses.get(loc)) {
                        if(Math.random() < 0.06) {
                            p.sendMessage(ChatColor.GREEN + "The AC turned on!");
                            p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2f);
                            p.playEffect(EntityEffect.FIREWORK_EXPLODE);
                            acStatuses.put(loc, true);
                        } else {
                            p.sendMessage(ChatColor.GRAY + "You clicked the AC power button, but it didn't work... Try again?");
                            p.playSound(loc, Sound.BLOCK_TRIPWIRE_CLICK_OFF, 1, 0.5f);
                        }
                    }
                }

                if(event.getItem() != null) {
                    Player p = event.getPlayer();
                    ItemStack item = event.getItem();
                    Location teleport = null;
                    String locationName = null;
                    if(Utils.isSpecialItem(item, GameItems.HEAT_TELEPORT_BOSTON_COMMONS)) {
                        teleport = master.getLocation(6490, -48, -7465);
                        locationName = "Boston Commons";
                    } else if(Utils.isSpecialItem(item, GameItems.HEAT_TELEPORT_BPL)) {
                        teleport = master.getLocation(6047, -52, -7135).setDirection(new Vector(-0.9693087100339502,0.0862830708193057,0.23020828903910073));
                        locationName = "Boston Public Library";
                    } else if(Utils.isSpecialItem(item, GameItems.HEAT_TELEPORT_HOTEL)) {
                        teleport = master.getLocation(6084, -52, -7132).setDirection(new Vector(0.054380248693203126,0.16418355950915658,0.9849297169539402));
                        locationName = "Hotel";
                    }

                    if(teleport != null) {
                        long timeMilliSinceTeleport = System.currentTimeMillis() - lastPlayerTeleport.getOrDefault(p, 0L);

                        if(timeMilliSinceTeleport <= 5000) {
                            p.sendMessage(ChatColor.GRAY + "Please wait, the T is arriving soon...");
                        } else {
                            lastPlayerTeleport.put(p, System.currentTimeMillis());
                            p.sendMessage(ChatColor.GRAY + "You requested the T. Please wait!");
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 10, false, false));

                            final Location teleportLocFinal = teleport;
                            final String locationNameFinal = locationName;
                            new BukkitRunnable() {

                                Entity cart;
                                int count = 0;

                                @Override
                                public void run() {
                                    p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 0.75f);

                                    count++;
                                    if(count >= 5) {
                                        if(cart.getPassengers().isEmpty()) {
                                            p.sendMessage(ChatColor.RED + "You left before the T moved!");
                                            p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.5f, 1.5f);
                                        } else {
                                            p.teleport(teleportLocFinal);
                                            p.sendMessage(ChatColor.GREEN + "Welcome to the " + locationNameFinal + "!");
                                            p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.5f, 0.5f);
                                        }
                                        cart.remove();
                                        cancel();
                                    } else if(count == 3) {
                                        p.playSound(p.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.5f, 1.25f);
                                        cart.addPassenger(p);
                                    } else if(count == 2) {
                                        cart = p.getWorld().spawnEntity(p.getLocation(), EntityType.MINECART);
                                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, false));
                                    }
                                }
                            }.runTaskTimer(master.plugin, 0, 20L);
                        }
                    }
                }
            }

            @Override
            public void onPotionSplash(PotionSplashEvent event) {
                for(LivingEntity ent : event.getAffectedEntities()) {
                    if(ent instanceof Villager && villagerHealths.containsKey(ent)) {
                        int heatScore = villagerHealths.get(ent);
                        int maxHeatScore = initialVillagerHealths.get(ent);
                        boolean entityCool = isEntityCool(entityToNarrativeEntity.get(ent));

                        heatScore = Math.min(maxHeatScore, heatScore + (maxHeatScore / 5));

                        villagerHealths.put(ent, heatScore);

                        ent.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, ((LivingEntity) ent).getEyeLocation(), 3, Math.random() * 1 - 0.5, Math.random() * 0.5 - 0.25, Math.random() * 1 - 0.5);
                        
                        updateResidentName(ent, entityCool, heatScore, maxHeatScore);
                    }
                }
            }
        });

        master.getWorld().setStorm(false);
        master.getWorld().setThunderDuration(0);
        master.getWorld().setClearWeatherDuration(getInitialGameSeconds() + getTimeoutGameSecondsBeforeEnd());
        master.getWorld().setTime(6000);
        master.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    @Override
    protected BukkitRunnable getGameWorker() {
        BukkitRunnable run = new BukkitRunnable() {
            @Override
            public void run() {
                //check the villagers (if they are exposed to air... if so then maybe consider their health and raise/lower)
                closestVillager.resetDistances();
                for(NarrativeEntity nEnt : entities.values()) {
                    Entity ent = nEnt.getEntity();
                    if(ent.isDead() || !ent.isValid()) {
                        continue;
                    }

                    int heatScore = villagerHealths.get(ent);
                    int maxHeatScore = initialVillagerHealths.get(ent);

                    if(heatScore == 0) {
                        continue;
                    }

                    boolean entityCool = isEntityCool(nEnt);
                    ent.setGlowing(!entityCool);
                    if(entityCool) {
                        if(!nEnt.isCurrentlyStationary()) {
                            nEnt.setStationary(true);
                        }

                        if(villagersCurrentlyHeating.contains(ent)) {
                            villagersCurrentlyHeating.remove(ent);
                            ent.playEffect(EntityEffect.VILLAGER_HAPPY);
                            if(nEnt.getTargetPlayer() != null) {
                                nEnt.getTargetPlayer().sendMessage(ChatColor.GRAY + "The resident is no longer following you.");
                                entityToNarrativeEntity.get(ent).setTargetPlayer(null);
                            }
                        }

                        if(heatScore < maxHeatScore) {
                            if(Math.random() < 0.7) {
                                heatScore++;
                                villagerHealths.put(ent, heatScore);
                                ent.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, ((LivingEntity) ent).getEyeLocation(), 3, Math.random() * 1 - 0.5, Math.random() * 0.5 - 0.25, Math.random() * 1 - 0.5);
                            }
                        }
                    } else {
                        double closestDistSquared = closestVillager.calculateDistanceSquaredToPlayer(ent);

                        if(!villagersCurrentlyHeating.contains(ent)) {
                            villagersCurrentlyHeating.add(ent);
                            ent.playEffect(EntityEffect.VILLAGER_ANGRY);
                        }

                        if(!(nEnt instanceof HeatWaveHomeNarrativeResident) && nEnt.isCurrentlyStationary()) {
                            nEnt.setStationary(false);
                        }

                        if(heatScore > 0) {
                            //64*64 = 4096
                            if((closestDistSquared < 4096 && Math.random() < 0.3) || Math.random() < 0.1) {
                                heatScore--;
                                villagerHealths.put(ent, heatScore);

                                ent.getWorld().spawnParticle(Particle.WATER_DROP, ((LivingEntity) ent).getEyeLocation(), 3, Math.random() * 1 - 0.5, Math.random() * 0.5 - 0.25, Math.random() * 1 - 0.5);

                                if(heatScore == 0) {
                                    ent.setGlowing(false);
                                    ent.playEffect(EntityEffect.VILLAGER_ANGRY);
                                    nEnt.setStationary(true);
                                }
                            }

                            if(Math.random() < 0.05) {
                                Location offset = ent.getLocation().clone().add(Math.random() * 24 - 12, 32 + Math.random() * 16, Math.random() * 24 - 12);
                                FallingBlock block = offset.getWorld().spawnFallingBlock(offset, Material.FIRE.createBlockData());
                                block.setVelocity(new Vector(Math.random() * 0.05 - 0.025, Math.random() * 0.05 - 0.025, Math.random() * 0.05 - 0.025));
                            }
                        }
                    }

                    updateResidentName(ent, entityCool, heatScore, maxHeatScore);
                }

                //if the player is in the heat, then give them nausea every once in a while
                for(Player p : master.getPlayers()) {

                    //check homebound villagers
                    if(closestVillager.getClosestDistance(p) <= 1024) { //32
                        Entity closest = closestVillager.getClosestEntity(p);
                        if(entityToNarrativeEntity.get(closest) instanceof HeatWaveHomeNarrativeResident) {
                            if(Math.random() < 0.2) {
                                String[] prompts = new String[] {
                                    "It's so hot in here... Can somebody help me?",
                                    "I'm burning up in here!",
                                    "My body is so sweaty...",
                                    "Oh no... the AC isn't working... It's so hot...",
                                };
                                p.getWorld().playSound(closest.getLocation(), Sound.ENTITY_VILLAGER_HURT, 0.5f, 0.5f);
                                p.sendMessage(ChatColor.AQUA + "Homebound Resident: " + ChatColor.GRAY + prompts[(int) (Math.random() * prompts.length)]);
                            }
                        }
                    }

                    if(System.currentTimeMillis() - lastPlayerHeatStroke.getOrDefault(p, 0L) >= 8000) {
                        if(Math.random() < 0.5) {
                            lastPlayerHeatStroke.put(p, System.currentTimeMillis());

                            p.playEffect(EntityEffect.WOLF_SMOKE);
                            p.playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1, 0.75f);
                        }
                    }

                    closestVillager.updatePlayerCompass(p);
                }
            }
        };

        run.runTaskTimer(master.plugin, 0L, 20L);

        return run;
    }

    private void updateResidentName(Entity ent, boolean isEntityCool, int heatScore, int maxHeatScore) {
        double heatPercent = (double) heatScore / maxHeatScore;
        int heatScale = (int) Math.ceil(heatPercent * 5); //ranges from 0-5
        String name = "";
        switch(heatScale) {
            case 0:
                name = "" + ChatColor.RED + ChatColor.BOLD + "♨♨♨♨♨";
                break;
            case 1:
                name = "" + ChatColor.RED + ChatColor.BOLD + "☀☀☀☀☀";
                break;
            case 2:
                name = "" + ChatColor.RED + "☀☀☀☀";
                break;
            case 3:
                name = "" + ChatColor.GOLD + "☀☀☀";
                break;
            case 4:
                name = "" + ChatColor.YELLOW + "☀☀";
                break;
            case 5:
                name = "" + ChatColor.BLUE + "☀";
                break;
        }

        if(heatScale != 0 && !isEntityCool) {
            name += ChatColor.RED + " ♨";
        }

        ent.setCustomName(name);
    }

    @Override
    public int getCurrentScore() {
        int score = 0;
        for(NarrativeEntity nEnt : entityToNarrativeEntity.values()) {
            Entity ent = nEnt.getEntity();
            if(!ent.isValid() || ent.isDead()) {
                continue;
            }

            if(isEntityCool(nEnt) && villagerHealths.get(ent) > 0) {
                score++;
            }
        }

        return score;
    }

    @Override
    public List<ItemStack> getInitialPlayerItems() {
        return new ArrayList<>(Arrays.asList(new ItemStack[] {
            Utils.getSpecialItem(GameItems.HEAT_TELEPORT_BOSTON_COMMONS),
            Utils.getSpecialItem(GameItems.HEAT_TELEPORT_BPL),
            Utils.getSpecialItem(GameItems.HEAT_TELEPORT_HOTEL),
            new ItemStack(Material.AIR),
            ClosestVillagerExtension.getCompass(),
            new ItemStack(Material.AIR),
            Utils.getSpecialItem(GameItems.COOLING_WATER),
            Utils.getSpecialItem(GameItems.COOLING_WATER),
            Utils.getSpecialItem(GameItems.COOLING_WATER),
        }));
    }

    @Override
    public String getGameName() {
        return "Heat Wave";
    }

    @Override
    public String getGameGoal() {
        return "Protect the residents from the scorching heat!";
    }

    @Override
    public String getGameTimeUntilEndText() {
        return "Time remaining";
    }

    @Override
    public int getInitialGameSeconds() {
        return master.getDifficulty() == GameMaster.GameDifficulty.EASY ? 60 * 10 : (60 * 7) + 30;
    }

    @Override
    public int getTimeoutGameSecondsBeforeEnd() {
        return 5;
    }

    @Override
    public Sound getGameMusic() {
        return Sound.MUSIC_NETHER_SOUL_SAND_VALLEY;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(6490, -48, -7465);
    }

    @Override
    public String[] getTipMessage() {
        return new String[] {"Right-click on the residents to ask them to follow you, and guide them to shade or water to help them cool down!", "Residents will have a HEAT indicator from 1 (" + ChatColor.BLUE + "☀" + ChatColor.WHITE + ", cool) to 5 (" + ChatColor.RED + ChatColor.BOLD + "☀☀☀☀☀" + ChatColor.WHITE + ", very hot). Too hot and they will become exhausted and can no longer move!"};
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        Location[] spawnLocations = new Location[] {
            //boston commons
            master.getLocation(6596, -49, -7458),
            master.getLocation(6611, -49, -7462),
            master.getLocation(6605, -49, -7463),
            master.getLocation(6607, -49, -7451),
            master.getLocation(6597, -49, -7443),
            master.getLocation(6588, -50, -7457),

            master.getLocation(6490, -49, -7490),
            master.getLocation(6492, -48, -7480),
            master.getLocation(6501, -48, -7466),
            master.getLocation(6492, -48, -7461),
            master.getLocation(6481, -48, -7468),
            master.getLocation(6480, -48, -7478),

            master.getLocation(6329, -53, -7393),
            master.getLocation(6323, -53, -7401),
            master.getLocation(6334, -54, -7413),
            master.getLocation(6332, -53, -7420),
            master.getLocation(6330, -53, -7393),
            master.getLocation(6323, -52, -7383),
            master.getLocation(6331, -53, -7371),
            master.getLocation(6344, -53, -7357),
            master.getLocation(6381, -53, -7376),
            master.getLocation(6356, -53, -7405),
            master.getLocation(6340, -53, -7448),

            //bolyston street
            master.getLocation(5960, -52, -7142),
            master.getLocation(5978, -52, -7136),
            master.getLocation(5993, -52, -7151),
            master.getLocation(6008, -52, -7158),
            master.getLocation(6058, -52, -7177),
            master.getLocation(6122, -52, -7183),
            master.getLocation(6183, -52, -7229),
            master.getLocation(6071, -52, -7147),
            master.getLocation(6074, -52, -7147),
            master.getLocation(6073, -52, -7149),
        };

        Location[] homeLocations = new Location[] {
            master.getLocation(6106, -52, -7098),
            master.getLocation(6090, -52, -7093),
            master.getLocation(6078, -52, -7095),
            master.getLocation(6074, -52, -7103),
            master.getLocation(6073, -52, -7112),
            master.getLocation(6096, -52, -7121),
            master.getLocation(6096, -52, -7124),
            master.getLocation(6101, -52, -7113),
            master.getLocation(6108, -52, -7097),
        };

        List<NarrativeEntity> entities = new ArrayList<>();

        for(Location loc : spawnLocations) {
            entities.add(new HeatWaveNarrativeResident(loc));
        }

        for(Location loc : homeLocations) {
            entities.add(new HeatWaveHomeNarrativeResident(loc));
        }

        return entities;
    }

    public boolean isEntityCool(NarrativeEntity nEnt) {
        Entity ent = nEnt.getEntity();

        if(nEnt instanceof HeatWaveHomeNarrativeResident) {
            return acStatuses.get(villagerACUnits.get(ent));
        }

        Location loc = ent.getLocation();

        if(loc.getBlock().getType() == Material.WATER) {
            return true;
        }

        for(int dy = 0; dy <= 16; dy++) {
            loc.add(0, 1, 0);
            if(loc.getBlock().getType() != Material.AIR && loc.getBlock().getType() != Material.RED_WALL_BANNER) {
                return true;
            }
        }

        return false;
    }
}
