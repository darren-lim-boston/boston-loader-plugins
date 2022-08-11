package narrative;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Type;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import master.GameMaster;

public class NarrativeEntity {

    public static class NarrativePlayerEntity extends NarrativeEntity {

        private String skinURL;

        private long skinRetryDelay;

        public NarrativePlayerEntity(Location loc, String skinURL, String name) {
            super(EntityType.PANDA, loc, name);

            this.skinURL = skinURL;
        }

        public void setSkinURLLoadRetryDelay(int ticks) {
            skinRetryDelay = ticks;
        }

        @Override
        public void spawn(GameMaster master) {
            JavaPlugin plugin = master.plugin;
            String locString = loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getWorld().getName();
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "npc create \"" + name + "\" --at " + locString);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "npc id");
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "npc skin --url " + skinURL);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "npc look");

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "npc select \"" + name + "\"");
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "npc skin --url " + skinURL);
                }
            }.runTaskLater(master.plugin, skinRetryDelay);
        }

        @Override
        public void dispose(GameMaster master) {
            master.plugin.getServer().dispatchCommand(master.plugin.getServer().getConsoleSender(), "npc remove \"" + name + "\"");
        }
    }

    private final EntityType type;
    protected final Location loc;
    protected final String name;
    private final boolean stationary;

    protected Entity ent;
    private LivingEntity lEnt;
    
    private Location targetLocation;
    private Player targetPlayer; 
    private double targetSpeed = 0.5;

    private BukkitRunnable run;


    public NarrativeEntity(EntityType type, Location loc, String name, boolean stationary) {
        this.type = type;
        this.loc = loc;
        this.name = name;
        this.stationary = stationary;
    }

    public NarrativeEntity(EntityType type, Location loc, String name) {
        this(type, loc, name, true);
    }

    public NarrativeEntity(EntityType type, Location loc, boolean stationary) {
        this(type, loc, UUID.randomUUID().toString(), stationary);
    }

    public NarrativeEntity(EntityType type, Location loc) {
        this(type, loc, UUID.randomUUID().toString(), true);
    }

    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = targetSpeed;
    }

    public void spawn(GameMaster master) {
        ent = loc.getWorld().spawnEntity(loc, type, true);
        ent.setMetadata("persistent", new FixedMetadataValue(master.plugin, true));

        modifyLivingEntity(master);
    }

    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
    }

    public void setTargetPlayer(Player p) {
        this.targetPlayer = p;
    }

    public boolean isCurrentlyStationary() {
        return lEnt.hasPotionEffect(PotionEffectType.SLOW);
    }

    public void setStationary(boolean stationary) {
        if(stationary) {
            if(!lEnt.hasPotionEffect(PotionEffectType.SLOW)) {
                lEnt.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100000, 1000, false, false));
                lEnt.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 1000, false, false));
            }
        } else {
            if(lEnt.hasPotionEffect(PotionEffectType.SLOW)) {
                lEnt.removePotionEffect(PotionEffectType.SLOW);
                lEnt.removePotionEffect(PotionEffectType.JUMP);
            }
        }
    }

    protected void looper() {
        if(targetPlayer != null) {
            targetLocation = targetPlayer.getLocation();
        }

        if(targetLocation != null) {
            if(lEnt.getLocation().distanceSquared(targetLocation) < 1.5) {
                targetLocation = null;
            } else {
                if(lEnt instanceof Mob) {
                    if(stationary) {
                        setStationary(false);
                    }

                    ((Mob) lEnt).getPathfinder().moveTo(targetLocation, targetSpeed);
                }
            }
        }

        if(targetLocation == null) {
            if(stationary) {
                setStationary(true);
            }
        }
    }

    private void modifyLivingEntity(GameMaster master) {
        if(ent instanceof LivingEntity) {
            lEnt = (LivingEntity) ent;

            lEnt.setRemoveWhenFarAway(false);

            if(stationary) {
                lEnt.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100000, 1000, false, false));
                lEnt.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 1000, false, false));
            }

            run = new BukkitRunnable() {
                @Override
                public void run() {
                    looper();
                }
            };
            run.runTaskTimer(master.plugin, 0L, 20L);

            //modify things for specific types
            if(ent instanceof Villager) {
                Villager villager = (Villager) ent;
                villager.setVillagerType(Type.values()[(int) (Math.random() * Type.values().length)]);
            }
        }
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return loc.clone();
    }

    public Entity getEntity() {
        return ent;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public void dispose(GameMaster master) {
        ent.remove();
        run.cancel();
    }
}