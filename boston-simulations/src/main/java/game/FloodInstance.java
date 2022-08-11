package game;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import master.GameMaster;

public class FloodInstance {

    private GameMaster master;
    private JavaPlugin plugin;
    private Location center;
    private Vector direction;

    private Set<Location> waterPoints;
    private Set<Location> closedWaterPoints;
    private int maxY, yDecreaseModulo;
    private int spreadDelay;

    private boolean hasLanded, inProgress;


    private boolean isStopped;

    private FloodInstance(GameMaster master, Location center) {
        this.master = master;
        this.plugin = master.plugin;
        this.center = center.clone();

        center.add(0, 64, 0);
        direction = new Vector(-1, 0, 0);
        groundCenter();
        calculateWaterPoints();
        inProgress = false;
    }

    public FloodInstance(GameMaster master) {
        this(master, master.getLocation(7200 + 64, -55, -7786));
        // this(master, master.getLocation(7210, -55, -7786));
    }

    public boolean isInProgress() {
        return inProgress;
    }

    private void groundCenter() {
        for(int dy = 0; dy <= 128; dy++) {
            if(center.getBlock().getType() == Material.AIR) {
                center.add(0, -1, 0);
            }
        }
    }

    private boolean canExpandWaterHere(Location loc) {
        Material mat = loc.getBlock().getType();
        BlockData data = loc.getBlock().getBlockData();

        if(mat == Material.AIR || mat == Material.WATER) {
            return true;
        }

        if(data instanceof Door) {
            if(((Door) data).isOpen()) {
                return true;
            }
        }

        if(data instanceof TrapDoor) {
            if(((TrapDoor) data).isOpen()) {
                return true;
            }
        }

        return false;
    }

    private void calculateWaterPoints() {
        waterPoints = new HashSet<>();
        closedWaterPoints = new HashSet<>();

        maxY = center.clone().add(0, 40, 0).getBlockY();
        for(int dz = -16; dz <= 16; dz++) {
            for(int dy = 0; dy <= 40; dy++) {
                Location loc = center.clone().add(0, dy, dz);
                waterPoints.add(loc);
            }
        }
    }

    //moving -dx direction
    //finds open air blocks in a 3x3 grid from the originalLocation that it can expand to (only in the -x direction)
    private Location[] findAdajcentOpenLocations(Location originalLoc) {
        Set<Location> adjacent = new HashSet<>();

        //valid transitions
        Vector[] validTransitions;
        if(!hasLanded) {
            validTransitions = new Vector[] {new Vector(-1, 0, 0)};
        } else {
            if(spreadDelay <= 0 && originalLoc.getBlockY() <= -40) {
                validTransitions = new Vector[] {new Vector(-1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1), new Vector(1, 0, 0)};
            } else {
                validTransitions = new Vector[] {new Vector(-1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1)};
            }
        }

        for(Vector transition : validTransitions) {
            Location newLoc = originalLoc.clone().add(transition);
            if(Math.abs(originalLoc.getBlockX() - newLoc.getBlockX()) > 1 || Math.abs(originalLoc.getBlockZ() - newLoc.getBlockZ()) > 1) {
                continue;
            }
            if(canExpandWaterHere(newLoc)) {
                if(!closedWaterPoints.contains(newLoc)) {
                    adjacent.add(newLoc);
                }
            }
        }

        return adjacent.toArray(new Location[0]);
    }

    public void startSimulation() {
        if(inProgress) {
            return;
        }
        inProgress = true;

        hasLanded = false;
        spreadDelay = 16;

        new BukkitRunnable() {
            @Override
            public void run() {
                if(isStopped) {
                    for(Location loc : closedWaterPoints) {
                        if(loc.getBlock().getType() == Material.WATER) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                    cancel();
                    return;
                }

                //first, ground center to figure out if on land yet
                if(!hasLanded) {
                    yDecreaseModulo++;
                    //one block every 6 cycles, aka 1 block every 60 ticks = 3 seconds
                    if(yDecreaseModulo % 6 == 0) {
                        center.add(direction);
                        if(center.getBlockX() <= 7200) {
                            hasLanded = true;
                        }
                    } else {
                        return;
                    }
                }

                Set<Location> newWaterPoints = new HashSet<>();
                for(Location loc : waterPoints) {
                    closedWaterPoints.add(loc);
                    if(canExpandWaterHere(loc)) {
                        BlockData water = Material.WATER.createBlockData();
                        loc.getBlock().setBlockData(water, true);
                        if(loc.getBlockY() < maxY) {
                            Location[] adjacent = findAdajcentOpenLocations(loc);
                            for(Location newLoc : adjacent) {
                                newWaterPoints.add(newLoc);
                            }
                        }
                    }
                }

                waterPoints = newWaterPoints;

                if(hasLanded) {
                    yDecreaseModulo++;
                    spreadDelay--;
                    //decrease by one block every 3 seconds
                    if(yDecreaseModulo % 6 == 0) {
                        maxY--;
    
                        if(maxY % 2 == 0) {
                            for(Player p : master.getPlayers()) {
                                if(p.getLocation().distanceSquared(center) <= 16384)  { //128 blocks
                                    p.playSound(p.getLocation(), Sound.ENTITY_SKELETON_HORSE_SWIM, 1.25f, 0.8f);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void stop() {
        isStopped = true;
    }
}
