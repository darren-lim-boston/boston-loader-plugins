import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;

import com.sk89q.worldedit.math.BlockVector3;

import net.md_5.bungee.api.ChatColor;

public class TileStitcher {

    private World world;
    private BlockVector3 minA, minB, maxA, maxB;

    private BlockVector3 clone(BlockVector3 original) {
        return BlockVector3.at(original.getBlockX(), original.getBlockY(), original.getBlockZ());
    }

    ///A goes towards the left (-dx), B goes towards the right (+)
    public TileStitcher(World world, BlockVector3 minA, BlockVector3 maxA, BlockVector3 minB, BlockVector3 maxB) {
        this.world = world;
        this.minA = clone(minA);
        this.maxA = clone(maxA);
        this.minB = clone(minB);
        this.maxB = clone(maxB);
    }

    public List<BlockUpdate> stitchTiles() {
        //find blocks for A
        Bukkit.broadcastMessage(ChatColor.AQUA + "FINDING FOR A: " + minA + " TO " + maxA);
        HashMap<Integer, Location> aBlocks = new HashMap<>();

        LinkedList<Integer> lastYs = new LinkedList<>();
        for(int y = minA.getY(); y <= maxA.getY(); y++) {
            lastYs.add(y);
        }

        for(int z = minA.getZ(); z <= maxA.getZ(); z++) {
            findCandidate:
            for(int x = maxA.getX(); x >= minA.getX(); x--) {
                int index = 0;
                for(int y : lastYs) {
                    Location candidate = new Location(world, x, y, z);

                    if(candidate.getBlock().getType() != Material.AIR) {
                        aBlocks.put(z, candidate.add(0.5, 0.5, 0.5));
                        lastYs.remove(index);
                        lastYs.addFirst(y);
                        break findCandidate;
                    }
                    index++;
                }
            }
        }

        //find blocks for B
        Bukkit.broadcastMessage(ChatColor.AQUA + "FINDING FOR B: " + minB + " TO " + maxB);
        HashMap<Integer, Location> bBlocks = new HashMap<>();

        lastYs = new LinkedList<>();
        for(int y = minB.getY(); y <= maxB.getY(); y++) {
            lastYs.add(y);
        }

        for(int z : aBlocks.keySet()) {
            findCandidate:
            for(int x = minB.getX(); x <= maxB.getX(); x++) {
                int index = 0;
                for(int y : lastYs) {    
                    Location candidate = new Location(world, x, y, z);

                    if(candidate.getBlock().getType() != Material.AIR) {
                        bBlocks.put(z, candidate.add(0.5, 0.5, 0.5));
                        lastYs.remove(index);
                        lastYs.addFirst(y);
                        break findCandidate;
                    }
                    index++;
                }
            }
        }

        Bukkit.broadcastMessage(ChatColor.AQUA + "FOUND " + aBlocks.size() + " VS " + bBlocks.size());

        List<BlockUpdate> updates = new ArrayList<>();

        // for(Location block : aBlocks.values()) {
        //     updates.add(new BlockUpdate(block.getBlockX(), block.getBlockY(), block.getBlockZ(), Material.PINK_WOOL));
        // }

        // for(Location block : bBlocks.values()) {
        //     updates.add(new BlockUpdate(block.getBlockX(), block.getBlockY(), block.getBlockZ(), Material.BLUE_WOOL));
        // }

        Bukkit.broadcastMessage(ChatColor.AQUA + "COLLISIONS: " + aBlocks.size() + " VS " + bBlocks.size());

        for(int z : aBlocks.keySet()) {
            if(bBlocks.containsKey(z)) {
                Location aBlock = aBlocks.get(z);
                Location bBlock = bBlocks.get(z);
                if(aBlock.equals(bBlock)) {
                    continue;
                }

                double minX = Math.min(aBlock.getBlockX(), bBlock.getBlockX());
                double maxX = Math.max(aBlock.getBlockX(), bBlock.getBlockX());

                Material mat = aBlock.getBlock().getType();
                BlockIterator line = new BlockIterator(world, aBlock.toVector(), bBlock.toVector().subtract(aBlock.toVector()), 0, 512);
                while(line.hasNext()) {
                    Block next = line.next();
                    if(next.getType() == Material.AIR) {
                        updates.add(new BlockUpdate(next.getX(), next.getY(), next.getZ(), mat));
                    }
                    
                    if(next.getX() < minX || next.getX() > maxX) {
                        break;
                    }
                }
            }
        }

        Bukkit.broadcastMessage(ChatColor.AQUA + "FINISHED WITH " + updates.size() + " UPDATES");

        return updates;
    }
}