import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;

import net.md_5.bungee.api.ChatColor;

public class CommandHandler implements CommandExecutor, TabCompleter {

    //variable field; allows us to use the BukkitRunnable class
    private JavaPlugin plugin;

    private LinkedList<BlockUpdate> updates = new LinkedList<>();
    private LinkedList<BlockUpdate> lastUpdate = new LinkedList<>();
    private TileStitcher pendingStitch;

    private boolean isUndoing = false;

    private Lock updateLock = new ReentrantLock();

    private BlockVector3 lastDimensions, lastMinCoords, lastMaxCoords;
    private Location lastCoords;

    private long lastUpdateMessage = 0;

    private final Material[] grassyMats = new Material[] {Material.GRASS, Material.GRASS, Material.GRASS, Material.GRASS, Material.GRASS, Material.OAK_SAPLING, Material.DANDELION, Material.AZURE_BLUET, Material.ALLIUM, Material.PINK_TULIP, Material.WHITE_TULIP, Material.POPPY};

    private List<String> getSchematics() {
        List<String> schematics = new ArrayList<>();

        for(String filename : new File(plugin.getDataFolder().getPath() + "/schematics/").list()) {
            schematics.add(filename.substring(0, filename.lastIndexOf(".")));
        }

        return schematics;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player && !((Player) sender).isOp()) {
            return null;
        }

        if(label.equalsIgnoreCase("boston")) {
            if(args.length <= 1) {
                return Arrays.asList(new String[] {"build", "stitch", "undo"});
            } else if (args.length <= 2) {
                return getSchematics();
            }
        }

        return null;
    }

    public CommandHandler(JavaPlugin plugin) {
        //set the field to the parameter
        this.plugin = plugin;

        //looper
        new BukkitRunnable() {
            @Override
            public void run() {
                updateLock.lock();
                if(!updates.isEmpty()) {
                    long time = System.currentTimeMillis();
                    if(time - lastUpdateMessage > 2000) {
                        Bukkit.broadcastMessage(ChatColor.GRAY + "UPDATES: " + updates.size());
                        lastUpdateMessage = time;
                    }

                    for(int i = 0; i < Math.min(updates.size(), 30000); i++) {
                        try {
                            BlockUpdate update = updates.pop();
                            if(i == 0 && lastUpdateMessage == time) {
                                Bukkit.broadcastMessage(ChatColor.GRAY + "---example: " + update.toString());
                            }
                            lastUpdate.add(update.apply(Bukkit.getWorlds().get(0)));
                        } catch (NoSuchElementException ex) {
                            updates.clear();
                        }
                    }

                    if(updates.isEmpty()) {
                        if(isUndoing) {
                            isUndoing = false;
                        }
                    }
                }

                if(updates.isEmpty() && pendingStitch != null) {
                    TileStitcher stitcher = pendingStitch;
                    pendingStitch = null;
                    Bukkit.broadcastMessage(ChatColor.GRAY + "Adding stiching asynchronously...!");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            updates.addAll(stitcher.stitchTiles());
                        }
                    }.runTaskAsynchronously(plugin);
                }
                updateLock.unlock();
            }
        }.runTaskTimer(plugin, 0, 5L);
    }

    private void build(Player p, String filename, boolean shouldResetLocation, Location lastCoordsArg) {
        Location baseLocP = p.getLocation();
        new BukkitRunnable(){
            @Override
            public void run() {
                Location baseLoc = baseLocP;
                File schematicFile = new File(plugin.getDataFolder().getPath() + "/schematics/" + filename + ".schematic");
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))){
                    Clipboard clipboard = reader.read();

                    BlockVector3 offset = SchematicParser.getOrigin(schematicFile);
                    Bukkit.broadcastMessage(ChatColor.GRAY + "OFFSET: " + offset);
            
                    /*
                    * Example dimensions: (401, 63, 3735)
                    *                      X    Y   Z
                    * To make this work, need to place the border blocks first, conserving location and double checking until it is correct
                    * Alternatively, the memory can store the locations of the previous schematic, and place the next one in the correct location
                    * So the next one should be placed next to this one? lastMaxCoords + (1, 0, 0)
                    */

                    // if (lastMaxCoords != null) {
                    //     baseLoc = new Location(baseLoc.getWorld(), lastMaxCoords.getX() + 1, lastMinCoords.getY(), lastMinCoords.getZ());
                    //     Bukkit.broadcastMessage("AUTOMATICALLY ADJUSTING TO ADJACENT COORDINATES OF THE LAST PLACED SCHEMATIC " + baseLoc.toVector().toString());

                    //     // subtract the X coordinate slightly until it lines up with the previous tile
                    //     baseLoc = fixTileXCoord(baseLoc);
                    //     Bukkit.broadcastMessage("ADJUSTED LOCATION: " + baseLoc.toVector().toString());
                    // }

                    //determine based off of last coords
                    boolean adjustY = false;
                    if (lastCoords != null && !shouldResetLocation) {
                        baseLoc = lastCoords.clone();
                        Bukkit.broadcastMessage("===AUTOMATICALLY ADJUSTING TO ADJACENT COORDINATES OF THE LAST COORDS");
                    } else { 
                        if(lastCoordsArg != null) {
                            lastCoords = lastCoordsArg;
                            baseLoc = lastCoords.clone();
                        } else {
                            adjustY = true;
                            lastCoords = baseLoc.clone();
                        }
                    }

                    //ROUNDS TO NEAREST 16 for X
                    //ALSO, tiles seem to be flipped on the X side... negate them TODO
                    // Bukkit.broadcastMessage("offset: " + (16 * (Math.round((double) offset.getX()/16))) + ", " + offset.getY() + ", " + offset.getZ());
                    // Bukkit.broadcastMessage(ChatColor.GRAY + "offset: " + offset.getX() + ", " + offset.getY() + ", " + offset.getZ());
                    // baseLoc.add(64 * (Math.round((double) offset.getX()/64)), offset.getY(), offset.getZ());
                    baseLoc.add(offset.getX(), offset.getY(), offset.getZ());

                    if(adjustY) {
                        for(int dy = 0; dy < 128; dy++) {
                            if(baseLoc.clone().add(0,-1,0).getBlock().getType() == Material.AIR) {
                                baseLoc.add(0,-1,0);
                            }
                        }
                    }

                    Bukkit.broadcastMessage("---BASELOC IS " + baseLoc.toVector().toString());

                    if(lastMinCoords != null) {
                        //create the stitcher
                        Bukkit.broadcastMessage(ChatColor.GRAY + "Added pending tile stitcher to current updates...");
                        pendingStitch = new TileStitcher(p.getWorld(), lastMinCoords, lastMaxCoords, clipboard.getMinimumPoint().add(baseLoc.getBlockX(), baseLoc.getBlockY(), baseLoc.getBlockZ()), clipboard.getMaximumPoint().add(baseLoc.getBlockX(), baseLoc.getBlockY(), baseLoc.getBlockZ()));
                    }

                    lastDimensions = clipboard.getDimensions();
                    lastMinCoords = clipboard.getMinimumPoint().add(baseLoc.getBlockX(), baseLoc.getBlockY(), baseLoc.getBlockZ());
                    lastMaxCoords = clipboard.getMaximumPoint().add(baseLoc.getBlockX(), baseLoc.getBlockY(), baseLoc.getBlockZ());

                    Bukkit.broadcastMessage(ChatColor.GREEN + "DIMENSIONS ARE " + lastDimensions);
                    Bukkit.broadcastMessage(ChatColor.GREEN + "MIN COORDS ARE " + lastMinCoords + " (" + clipboard.getMinimumPoint().toString() + ")");
                    Bukkit.broadcastMessage(ChatColor.GREEN + "MAX COORDS ARE " + lastMaxCoords + " (" + clipboard.getMaximumPoint().toString() + ")");

                    try {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(p != null && p.isOnline()) {
                                    p.teleport(new Location(p.getWorld(), lastMinCoords.getX(), lastMinCoords.getY() + 32, lastMinCoords.getZ()));
                                }
                            }
                        }.runTask(plugin);
                    } catch(Exception ex) {}

                    for(int x = clipboard.getMinimumPoint().getX(); x < clipboard.getMaximumPoint().getX(); x++) {
                        for(int y = clipboard.getMinimumPoint().getY(); y < clipboard.getMaximumPoint().getY(); y++) {
                            for(int z = clipboard.getMinimumPoint().getZ(); z < clipboard.getMaximumPoint().getZ(); z++) {
                                BlockVector3 pos = BlockVector3.at(x, y, z);
                                Material mat = BukkitAdapter.adapt(clipboard.getBlock(pos)).getMaterial();

                                if(mat == Material.AIR) {
                                    continue;
                                } else if(mat == Material.GREEN_CONCRETE) {
                                    //RENDER ALL GREEN CONCRETE AS GRASS
                                    mat = Material.GRASS_BLOCK;

                                    double random = Math.random();
                                    if(random < 0.03) {
                                        updates.add(new BlockUpdate(pos.getX() + baseLoc.getBlockX(),
                                        pos.getY() + baseLoc.getBlockY() + 1, pos.getZ() + baseLoc.getBlockZ(), grassyMats[new Random().nextInt(grassyMats.length)]));
                                    }
                                }
                                 else if(mat == Material.BLUE_CONCRETE) {
                                    //RENDER ALL BLUE CONCRETE AS WATER
                                    mat = Material.WATER;
                                }

                                updates.add(new BlockUpdate(pos.getX() + baseLoc.getBlockX(),
                                        pos.getY() + baseLoc.getBlockY(), pos.getZ() + baseLoc.getBlockZ(), mat));
                            }
                        }
                    }

                    Bukkit.broadcastMessage(ChatColor.GRAY + "UPDATE SIZE IS NOW: " + updates.size());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandName, String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("Must be player to execute this command.");
            return true;
        }
        Player p = (Player) commandSender;
        if(!p.isOp()) {
            commandSender.sendMessage("Must be op to execute this command.");
            return true;
        }
        if(args.length < 1) {
            p.sendMessage("Format: /boston <build/undo/stitch> [options]");
            p.sendMessage(ChatColor.GRAY + "Example: /boston build schematic_A_512");
            p.sendMessage(ChatColor.GRAY + "Example: /boston undo");
            return true;
        }

        switch(args[0].toLowerCase()) {
            case "undo":
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "undoing the last update!");
                if (updates.size() > 0 || pendingStitch != null) {
                    commandSender.sendMessage(ChatColor.RED + "please wait for the last update to finish before undoing!");
                    break;
                }

                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "starting to undo the last update!");
                isUndoing = true;
                updates.addAll(new LinkedList<>(lastUpdate));
                break;
            case "stitch":
                //this command is used to try to stitch GAPS between Combined tiles... see boston-loader-convert for information about what a gap is. This is not a permanent solution.
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Stitching from current location... Ensure that your Z is the min value of the two tiles!");
                Location l = p.getLocation();
                int deltaX = 16;
                try {
                    if(args.length > 1) {
                        deltaX = Integer.parseInt(args[1]);
                    }
                } catch (NumberFormatException ex) {
                    p.sendMessage(ChatColor.RED + "INVALID DELTAX (must be a number)");
                }
                pendingStitch = new TileStitcher(p.getWorld(), 
                        BlockVector3.at(l.getBlockX() - deltaX, l.getBlockY() - 8, l.getBlockZ()),
                        BlockVector3.at(l.getBlockX(), l.getBlockY() + 12, l.getBlockZ() + 2000),
                        BlockVector3.at(l.getBlockX(), l.getBlockY() - 8, l.getBlockZ()),
                        BlockVector3.at(l.getBlockX() + deltaX, l.getBlockY() + 12, l.getBlockZ() + 2000)
                );
                break;
            case "build":
                if (updates.size() > 0 || pendingStitch != null) {
                    commandSender.sendMessage(ChatColor.RED + "please wait for the last update to finish before undoing!");
                    break;
                }
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "loading the schematic...");

                updates.clear();
                lastUpdate.clear();

                Location loc = null;
                try {
                    if(args.length >= 5) {
                        loc = new Location(p.getWorld(), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                    }
                } catch(NumberFormatException ex) {
                    p.sendMessage(ChatColor.RED + "Syntax: /boston build " + args[1] + " X Y Z");
                }
                

                build(p, args[1], args.length >= 3, loc);
                break;
        }
        return true;
    }
}