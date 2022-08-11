import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import master.Utils;
import master.Utils.GameItems;

public class EventListener implements Listener {

    class BlockOperation {

        private final HashMap<Location, BlockData> previousMaterials;

        public BlockOperation(HashMap<Location, Material> locs) {
            previousMaterials = new HashMap<>();
            for(Location loc : locs.keySet()) {
                BlockData oldMaterial = loc.getBlock().getBlockData().clone();
                loc.getBlock().setType(locs.get(loc));
                previousMaterials.put(loc, oldMaterial);
            }
        }

        public void undo() {
            for(Location loc : previousMaterials.keySet()) {
                loc.getBlock().setBlockData(previousMaterials.get(loc));;
            }
        }
    }

    //variable field; allows us to use the BukkitRunnable class
    private JavaPlugin plugin;

    public EventListener(JavaPlugin plugin) {
        //set the field to the parameter
        this.plugin = plugin;
    }

    private Stack<BlockOperation> operations = new Stack<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getItem() != null) {
            Player p = event.getPlayer();
            ItemStack item = event.getItem();

            if(Utils.isSpecialItem(item, GameItems.MAGIC_WAND)) {
                if(p.isSneaking()) {
                    if(operations.isEmpty()) {
                        p.sendMessage("Nothing to undo!");
                    } else {
                        operations.pop().undo();
                    }
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_WITCH_THROW, 1, 0.75f);
                    
                    applyMagicOperation(p, 2);
                }
            }
        }
    }

    private Vector getDir(Player p) {
        Vector dir = p.getLocation().getDirection();
        Vector dirPos = new Vector(Math.abs(dir.getX()), Math.abs(dir.getY()), Math.abs(dir.getZ()));
        if(dirPos.getX() > dirPos.getZ()) {
            if(dirPos.getX() > dirPos.getY()) {
                dir = new Vector(Math.round(dir.getX()), 0, 0);
            } else {
                dir = new Vector(0, Math.round(dir.getY()), 0);
            }
        } else {
            if(dirPos.getZ() > dirPos.getY()) {
                dir = new Vector(0, 0, Math.round(dir.getZ()));
            } else {
                dir = new Vector(0, Math.round(dir.getY()), 0);
            }
        }
        return dir;
    }

    private void applyMagicOperation(Player p, int val) {
        HashMap<Location, Material> locs = new HashMap<>();
        switch(val) {
            case 0:
                Vector dir = getDir(p);

                Block target = p.getTargetBlock(32);
                if(target.getType() == Material.WHITE_CONCRETE) {
                    Location startBlock = target.getLocation().add(dir.clone().multiply(-1));
                    if(startBlock.getBlock().getType() == Material.AIR) {
                        LinkedList<Location> openList = new LinkedList<>();
                        Set<Location> closedList = new HashSet<>();
                        int timeout = 512;

                        openList.add(startBlock);

                        while(timeout > 0 && !openList.isEmpty()) {
                            Location loc = openList.pop();
                            closedList.add(loc);

                            Vector[] transitions = new Vector[] {new Vector(-1, 0, 0), new Vector(1, 0, 0), new Vector(0, 0, -1), new Vector(0, 0, 1), new Vector(0, -1, 0), new Vector(0, 1, 0)};

                            for(Vector v : transitions) {
                                Location newLoc = loc.clone().add(v);
                                if(newLoc.getBlock().getType() == Material.AIR && !closedList.contains(newLoc)) {
                                    if(newLoc.clone().add(dir).getBlock().getType() == Material.WHITE_CONCRETE) {
                                        openList.add(newLoc);
                                    }
                                }
                            }

                            timeout--;
                        }

                        for(Location loc : closedList) {
                            locs.put(loc, Material.BOOKSHELF);
                        }
                    }
                }

                break;
            case 1:
                dir = getDir(p);
                target = p.getTargetBlock(32);
                if(target.getType() == Material.WHITE_CONCRETE) {
                    Location startBlock = target.getLocation().add(dir.clone().multiply(-1));
                    locs.put(startBlock, Material.CHAIN);
                    locs.put(startBlock.add(0, -1, 0), Material.CHAIN);
                    locs.put(startBlock.add(0, -1, 0), Material.CHAIN);
                    locs.put(startBlock.add(0, -1, 0), Material.LANTERN);
                }
                break;
            case 2:
                dir = getDir(p);

                target = p.getTargetBlock(32);
                if(target.getType() != Material.AIR) {
                    Location startBlock = target.getLocation().add(dir.clone().multiply(-1));
                    if(startBlock.getBlock().getType() == Material.AIR) {
                        LinkedList<Location> openList = new LinkedList<>();
                        Set<Location> closedList = new HashSet<>();
                        int timeout = 1024;

                        openList.add(startBlock);

                        while(timeout > 0 && !openList.isEmpty()) {
                            Location loc = openList.pop();
                            closedList.add(loc);

                            Vector[] transitions = new Vector[] {new Vector(-1, 0, 0), new Vector(1, 0, 0), new Vector(0, 0, -1), new Vector(0, 0, 1), new Vector(0, -1, 0), new Vector(0, 1, 0)};

                            for(Vector v : transitions) {
                                Location newLoc = loc.clone().add(v);
                                if(newLoc.getBlock().getType() == Material.AIR && !closedList.contains(newLoc)) {
                                    if(newLoc.clone().add(dir).getBlock().getType() == target.getType()) {
                                        openList.add(newLoc);
                                    }
                                }
                            }

                            timeout--;
                        }

                        for(Location loc : closedList) {
                            locs.put(loc, target.getType());
                        }
                    }
                }

                break;
            default:
                return;
        }

        p.sendMessage(org.bukkit.ChatColor.GRAY + "applied: " + locs.size());
        operations.add(new BlockOperation(locs));
    }
}
