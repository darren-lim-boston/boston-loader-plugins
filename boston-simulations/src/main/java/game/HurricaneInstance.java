package game;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import master.GameMaster;

public class HurricaneInstance {

    public static final int MAX_LIVES = 50;

    private GameMaster master;
    private JavaPlugin plugin;

    private boolean inProgress;

    private int lives, waterLives;
    private Location center;
    private Vector direction, acceleration;
    
    public HurricaneInstance(GameMaster master) {
        this.master = master;
        this.plugin = master.plugin;

        inProgress = false;

        this.center = master.getLocation(7119, -55, -8296);
        this.direction = new Vector(-0.5277335075625592,-0.0784592284754169,0.8457786320679664).normalize();
        this.acceleration = new Vector(0.001, 0, -0.001);
        lives = MAX_LIVES;
        waterLives = MAX_LIVES * 3;
        
        groundCenter();
    }

    private void groundCenter() {
        for(int dy = 0; dy <= 128; dy++) {
            if(center.getBlock().getType() == Material.AIR) {
                center.add(0, -1, 0);
            }
        }
    }
    
    private void drawSpiral(Location location, double fullSize, double angleOffset) {
        new BukkitRunnable() {

            Location l = location.clone();
            double d = 0;

            @Override
            public void run() {
                double livesPercent = (double) lives / MAX_LIVES;

                int maxD;
                if(livesPercent > 0.5) {
                    maxD = 100;
                } else if(livesPercent > 0.25) {
                    maxD = 80;
                } else {
                    maxD = 60;
                }

                while(d < maxD) {
                    double size = (double) d / fullSize;
                    Location particleLoc = new Location(l.getWorld(), l.getX(), l.getY(), l.getZ());
                    particleLoc.setX(l.getX() + Math.cos(d + angleOffset) * size);
                    particleLoc.setZ(l.getZ() + Math.sin(d + angleOffset) * size);
                    
                    for(int i = 0; i < (livesPercent > 0.5 ? 3 : (livesPercent > 0.25 ? 2 : 1)); i++) {
                        l.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, Math.random() * 1 - 0.5, Math.random() * 3 - 1.5, Math.random() * 1 - 0.5, 0.025, null, true);
                    }

                    if(d < 30) {
                        d += 0.1;
                        l.add(Math.random() * 0.25 - 0.125, 0.08, Math.random() * 0.25 - 0.125);
                    } else if(d < 60) {
                        d += 0.05;
                        l.add(Math.random() * 0.5 - 0.25, 0.04, Math.random() * 0.5 - 0.25);
                    } else {
                        d += 0.025;
                        l.add(Math.random() * 0.6 - 0.3, 0.02, Math.random() * 0.6 - 0.3);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void startSimulation() {
        if(inProgress) {
            return;
        }
        inProgress = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                //find adjacent blocks
                HashMap<Location, BlockData> removedBlocks = new HashMap<>();
                double livesPercent = (double) lives / MAX_LIVES;
                int blockRange;
                if(livesPercent > 0.5) {
                    blockRange = 16;
                } else if(livesPercent > 0.25) {
                    blockRange = 8;
                } else {
                    blockRange = 4;
                }
                int blockRangeSquared = blockRange * blockRange;

                Location centerNoY = center.clone();
                centerNoY.setY(0);

                for(int dx = -blockRange; dx <= blockRange; dx++) {
                    for(int dz = -blockRange; dz <= blockRange; dz++) {
                        Location loc = center.clone().add(dx, 64, dz);

                        for(int dy = 0; dy < 64; dy++) {
                            if(loc.getBlock().getType() == Material.AIR) {
                                loc.add(0, -1, 0);
                            }
                        }

                        //don't toss up blocks in between other blocks
                        if(loc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR) {
                            continue;
                        }

                        Location locNoY = loc.clone();
                        locNoY.setY(0);
                        if(locNoY.distanceSquared(centerNoY) > blockRangeSquared) {
                            continue;
                        }
                        if(loc.getBlock().getType() == Material.WATER) {
                            continue;
                        }
                        removedBlocks.put(loc, loc.getBlock().getBlockData());
                        loc.getBlock().setType(Material.AIR);
                    }
                }

                //draw the hurricane
                drawSpiral(center, 1.5, (15 * lives) % 180);
                center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.7f);

                for(Player p : Bukkit.getOnlinePlayers()) {
                    if(p.getLocation().distanceSquared(center) <= 90000) { //300 blocks
                        p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
                    }
                }

                //fling nearby hurricanes into the hurricane
                Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, 16, 32, 16);
                Location goalLocation = center.clone().add(0, 16, 0);
                for(Entity e : nearbyEntities) {
                    if(e instanceof FallingBlock) {
                        continue;
                    }
                    if(e instanceof Player && ((Player) e).getGameMode() == GameMode.CREATIVE) {
                        continue;
                    }

                    e.setVelocity(goalLocation.toVector().subtract(e.getLocation().toVector()).normalize().multiply(1.5));
                    e.setFallDistance(0);
                }

                if(Math.random() < 0.5) {
                    Location loc = center.clone().add(Math.random() * 64 - 32, 0, Math.random() * 64 - 32);
                    loc.getWorld().strikeLightning(loc);
                }

                //update locations
                center.add(direction.clone().multiply(10));
                direction.add(acceleration.clone()).normalize();
                center.add(0, 32, 0);
                groundCenter();

                staggeredSpawnFallingBlock(removedBlocks, goalLocation);

                if(center.getBlock().getType() == Material.WATER) {
                    waterLives--;

                    if(waterLives <= 0) { 
                        cancel();
                    }
                } else {
                    lives--;
                }
                if(lives <= 0) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void staggeredSpawnFallingBlock(HashMap<Location, BlockData> removedBlocks, Location goalLocation) {
        HashSet<Location>[] staggered = new HashSet[3];

        for(int i = 0; i < staggered.length; i++) {
            staggered[i] = new HashSet<>();
        }

        for(Location loc : removedBlocks.keySet()) {
            staggered[(int) (Math.random() * staggered.length)].add(loc);
        }

        new BukkitRunnable() {

            int count = 0;

            @Override
            public void run() {
                for(Location loc : staggered[count]) {
                    FallingBlock ent = loc.getWorld().spawnFallingBlock(loc, removedBlocks.get(loc));
                    // FallingBlock ent = loc.getWorld().spawnFallingBlock(loc, Material.RED_WOOL.createBlockData());
                    ent.setDropItem(false);
                    ent.setVelocity(ent.getLocation().toVector().subtract(goalLocation.toVector()).normalize().setY(1.5).multiply(1.2));
                    ent.setMetadata("hurricaneBlock", new FixedMetadataValue(plugin, true));
                }

                count++;
                if(count == staggered.length) {
                    cancel();
                }
            }
        }.runTaskTimer(master.plugin, 1L, 2L);
    }
}
