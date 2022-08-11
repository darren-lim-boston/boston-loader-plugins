import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import master.GameMaster;
import master.GameMaster.GameDifficulty;
import master.Utils;
import master.Utils.GameItems;
import net.md_5.bungee.api.ChatColor;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private enum SimulationConfirmations {
        RESET_WORLD, BACKUP_WORLD
    }

    //variable field; allows us to use the BukkitRunnable class
    private JavaPlugin plugin;

    private HashMap<Player, SimulationConfirmations> confirmations = new HashMap<>();
    private HashMap<Player, Long> confirmationTimeouts = new HashMap<>();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(label.equalsIgnoreCase("game")) {
            if(args.length < 2) {
                return Arrays.asList("easy", "normal", "flood", "heatwave", "hurricane");
            } else if(args.length < 3) {
                if(args[0].equalsIgnoreCase("flood") || args[0].equalsIgnoreCase("heatwave") || args[0].equalsIgnoreCase("hurricane")) {
                    return Arrays.asList("easy", "normal");
                }
            }
        }

        return null;
    }

    public CommandHandler(JavaPlugin plugin) {
        //set the field to the parameter
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandName, String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("Must be player to execute this command.");
            return true;
        }
        Player p = (Player) commandSender;

        switch(commandName.toLowerCase()) {
            case "game":
                return commandGame(p, args);
            case "simulation":
                return commandSimulation(p, args);
            default:
                return false;
        }
    }

    private boolean commandGame(Player p, String[] args) {
        if(args.length < 1) {
            p.sendMessage(ChatColor.RED + "You must specify a difficulty level to start the game.");
            p.sendMessage(ChatColor.GRAY + "/game <easy/normal>");
            return true;
        }

        GameMaster.Gamemode gamemode;
        GameDifficulty difficulty;
        String name = null;

        switch(args[0].toLowerCase()) {
            case "e":
            case "easy":
                gamemode = GameMaster.Gamemode.ALL_GAMES;
                difficulty = GameDifficulty.EASY;
                break;
            case "n":
            case "normal":
                gamemode = GameMaster.Gamemode.ALL_GAMES;
                difficulty = GameDifficulty.NORMAL;
                break;
            default:
                switch(args[0].toLowerCase()) {
                    case "flood":
                    case "heatwave":
                    case "hurricane":
                        if(args.length < 3) {
                            p.sendMessage(ChatColor.RED + "You must specify a difficulty level and name to start an individual game.");
                            p.sendMessage(ChatColor.GRAY + "/game " + args[0].toLowerCase() + " <easy/normal> <name>");
                            return true;
                        }
                        if(!args[1].equalsIgnoreCase("easy") && !args[1].equalsIgnoreCase("normal")) {
                            p.sendMessage(ChatColor.RED + "You must specify a valid difficulty level to start an individual game.");
                            p.sendMessage(ChatColor.GRAY + "/game " + args[0].toLowerCase() + " <easy/normal> <name>");
                            return true;
                        }

                        name = "";
                        for(int i = 2; i < args.length; i++) {
                            name += args[i];
                            if(i != args.length - 1) {
                                name += " ";
                            }
                        }

                        difficulty = args[1].equalsIgnoreCase("easy") ? GameDifficulty.EASY : GameDifficulty.NORMAL;

                        switch(args[0].toLowerCase()) {
                            case "flood":
                                gamemode = GameMaster.Gamemode.FLOOD_ONLY;
                                break;
                            case "heatwave":
                                gamemode = GameMaster.Gamemode.HEAT_WAVE_ONLY;
                                break;
                            case "hurricane":
                                gamemode = GameMaster.Gamemode.HURRICANE_ONLY;
                                break;
                            default:
                                p.sendMessage(ChatColor.RED + "You must specify a difficulty level and name to start an individual game.");
                                p.sendMessage(ChatColor.GRAY + "/game " + args[0].toLowerCase() + " <easy/normal> <name>");
                                return true;
                        }
                        break;
                    default:
                        p.sendMessage(ChatColor.RED + "You must specify a valid difficulty level to start the game.");
                        p.sendMessage(ChatColor.GRAY + "/game <easy/normal>");
                        return true;
                }
                break;
        }

        if(!((MainClass) plugin).master.start(true, difficulty, gamemode, name)) {
            p.sendMessage(ChatColor.RED + "The game is already in session.");
        }

        return true;
    }

    private boolean commandSimulation(Player p, String[] args) {
        if(!p.isOp()) {
            p.sendMessage(ChatColor.RED + "You don't have permission to execute this command.");
            return true;
        }

        if(args.length < 1) {
            p.sendMessage(ChatColor.RED + "You must specify arguments.");
            p.sendMessage(ChatColor.GRAY + "/simulation <args>");
            return true;
        }

        GameMaster master = ((MainClass) plugin).master;

        switch(args[0].toLowerCase()) {
            case "s":
            case "start":
                boolean reloadWorld = true;
                if(args.length > 1) {
                    reloadWorld = Boolean.parseBoolean(args[1].toLowerCase());
                    p.sendMessage("Set reload world: " + reloadWorld);
                }
                ((MainClass) plugin).master.start(reloadWorld, GameDifficulty.NORMAL);
                break;
            case "stop":
                ((MainClass) plugin).master.forceEndGame();
                break;
            case "world":
                if(args.length > 1) {
                    World world = plugin.getServer().getWorld(args[1]);
                    if(world == null) {
                        p.sendMessage("That world does not exist.");
                        break;
                    }
                    p.teleport(world.getSpawnLocation());
                } else {
                    p.sendMessage("You must specify a world name");
                }
                break;
            case "speed":
                try {
                    float speed = Float.parseFloat(args[1]);
                    p.setFlySpeed(speed);
                    p.setFlySpeed(speed);
                    p.sendMessage("Done!");
                } catch(NumberFormatException ex) {
                    p.sendMessage(ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    p.sendMessage(ex.getMessage());
                }
                break;
            case "butcher":
                for(Entity ent : p.getNearbyEntities(30, 30, 30)) {
                    if(ent.getType() != EntityType.PLAYER && ent instanceof LivingEntity) {
                        ent.remove();
                    }
                }
                break;
            case "clean":
                Block block = p.getTargetBlockExact(128);
                if(block != null) {
                    Location newLocBlock = block.getLocation();
                    newLocBlock.setY(-55);
                    block = newLocBlock.getBlock();
                    p.sendMessage("Trying to clean target block: " + block.getLocation().toVector());
                    if(block.getY() == -55) {
                        int range = 64;
                        HashMap<Integer, Integer> removed = new HashMap<>();
                        for(int dx = -range; dx <= range; dx++) {
                            for(int dz = -range; dz <= range; dz++) {
                                Location loc = block.getLocation().clone().add(dx, 0, dz);
                                if(loc.getBlock().getType() == Material.WHITE_CONCRETE) {
                                    //expand and if <= 10 white blocks, then destroy them all
                                    LinkedList<Location> openList = new LinkedList<>();
                                    Set<Location> closedList = new HashSet<>();
                                    openList.add(loc);

                                    Set<Location> whiteConcrete = new HashSet<>();
                                    whiteConcrete.add(loc);

                                    int timeout = 10;
                                    while(!openList.isEmpty()) {
                                        Location openLoc = openList.pop();
                                        closedList.add(openLoc);

                                        Vector[] transitions = new Vector[] {new Vector(1, 0, 0), new Vector(-1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1)};

                                        for(Vector t : transitions) {
                                            Location newLoc = openLoc.clone().add(t);

                                            if(newLoc.getBlock().getType() == Material.WHITE_CONCRETE) {
                                                whiteConcrete.add(newLoc);
                                                openList.add(newLoc);
                                            }
                                        }

                                        timeout--;
                                        if(timeout == 0) {
                                            break;
                                        }
                                    }

                                    if(whiteConcrete.size() <= 10 && !whiteConcrete.isEmpty()) {
                                        removed.putIfAbsent(whiteConcrete.size(), 0);
                                        removed.put(whiteConcrete.size(), removed.get(whiteConcrete.size()) + 1);
                                        for(Location removeLoc : whiteConcrete) {
                                            removeLoc.getBlock().setType(Material.WATER);
                                        }
                                    }
                                }
                            }
                        }

                        p.sendMessage("Done! Purged the following stranded blocks:");
                        for(int size : removed.keySet()) {
                            p.sendMessage("Standed size: " + size + " - removed " + removed.get(size) + " island(s)");
                        }
                    }
                } else {
                    p.sendMessage("Please look at a block.");
                }
                break;
            case "sound":
                if(args.length < 4) {
                    p.sendMessage("Not enough args: /simulation sound SOUND volume pitch");
                    break;
                }
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(args[1].toUpperCase()), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
                } catch (NumberFormatException ex) {
                    p.sendMessage("Format: /simulation sound SOUND volume pitch");
                }
                break;
            case "test":
                Villager v = (Villager) p.getWorld().spawnEntity(p.getLocation(), EntityType.VILLAGER);
                new BukkitRunnable() {
                    int count = 10;
                    @Override
                    public void run() {
                        v.getPathfinder().moveTo(p.getLocation());

                        count--;
                        if(count == 0) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 20L);
                break;
            case "dir":
                Vector dir = p.getLocation().getDirection();
                Bukkit.broadcastMessage(dir.toString());
                break;
            case "look":
                p.sendMessage("" + p.getTargetBlock(32));
                break;
            case "loc":
                Location loc = p.getLocation();
                for(int i = 0; i < 64; i++) {
                    if(loc.add(0, -1, 0).getBlock().getType() != Material.AIR) {
                        loc.add(0, 1, 0);
                        break;
                    }
                }
                Bukkit.broadcastMessage("master.getLocation(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "),");
                break;
            case "reset":
                confirmations.put(p, SimulationConfirmations.RESET_WORLD);
                confirmationTimeouts.put(p, System.currentTimeMillis());
                p.sendMessage("Perform /simulation confirm within 5 seconds to execute the operation.");
                break;
            case "backup":
                confirmations.put(p, SimulationConfirmations.BACKUP_WORLD);
                confirmationTimeouts.put(p, System.currentTimeMillis());
                p.sendMessage("Perform /simulation confirm within 5 seconds to execute the operation.");
                break;
            case "confirm":
                if(!confirmationTimeouts.containsKey(p) || System.currentTimeMillis() - confirmationTimeouts.get(p) > 5000) {
                    p.sendMessage("The confirmation timed out. Please try again.");
                    break;
                }
                switch(confirmations.get(p)) {
                    case RESET_WORLD:
                        confirmationTimeouts.remove(p);
                        master._DEBUGPLAYERS(p);
                        loc = p.getLocation().clone();
                        p.sendMessage("Resetting the world...");
                        master.unloadReloadWorld();
                        p.teleport(loc);
                        p.sendMessage("Done!");
                        break;
                    case BACKUP_WORLD:
                        confirmationTimeouts.remove(p);
                        backupWorld(p, master);
                        break;
                }
                break;
            case "magic":
                p.getInventory().addItem(Utils.getSpecialItem(GameItems.MAGIC_WAND));
                break;
            case "seal":
                loc = p.getLocation().clone();
                loc.setY(-61);

                int added = 0;

                for(int dx = -128; dx <= 128; dx++) {
                    for(int dz = -128; dz <= 128; dz++) {
                        boolean fit = false;
                        Location checkLoc = loc.clone().add(dx, 1, dz);
                        //check up to 10 blocks up to see if it is collidable
                        if(checkLoc.getBlock().getType() == Material.AIR) {
                            for(int dy = 0; dy < 12; dy++) {
                                if(checkLoc.clone().add(0, dy, 0).getBlock().getType() != Material.AIR) {
                                    fit = true;
                                }
                            }
                        }

                        if(fit) {
                            Location addLoc = loc.clone().add(dx, 0, dz);
                            
                            if(!sealLocations.contains(addLoc)) {
                                sealLocations.add(addLoc);
                                p.sendBlockChange(addLoc, Material.RED_WOOL.createBlockData());
                                added++;
                            }
                        }
                    }
                }

                p.sendMessage("Added " + added + " location(s) to seal.");
                break;
            case "sealdone":
                p.sendMessage("Sealing... (" + sealLocations.size() + ")");
                for(Location sealLoc : sealLocations) {
                    for(int dy = 0; dy < 12; dy++) {
                        sealLoc.add(0, 1, 0);
                        if(sealLoc.getBlock().getType() == Material.AIR) {
                            sealLoc.getBlock().setType(Material.WHITE_CONCRETE);
                        } else {
                            break;
                        }
                    }
                }
                p.sendMessage("Done!");
                break;
            case "sealcheck":
                p.sendMessage("Seal count (" + sealLocations.size() + ")");
                for(Location sealLoc : sealLocations) {
                    p.sendBlockChange(sealLoc, Material.RED_WOOL.createBlockData());
                }
                break;
            case "block":
                p.getWorld().spawnFallingBlock(p.getLocation(), Material.LIGHT_BLUE_WOOL.createBlockData());
                break;
            default:
                p.sendMessage("incorrect command");
                break;
        }

        return true;
    }

    private Set<Location> sealLocations = new HashSet<>();

    private void backupWorld(Player p, GameMaster master) {
        p.sendMessage("Backing up the world...");
        World world = master.getWorld();
        File mainFolder = world.getWorldFolder().getParentFile();
        File worldCopy = new File(mainFolder, "world-backup");
        world.save();
        //delete backup folder
        if(world != null) {
            try {
                FileUtils.deleteDirectory(worldCopy);
            } catch (IOException ex) {
                p.sendMessage("Something went wrong...");
                return;
            }
        }
        //create new backup folder
        try {
            FileUtils.copyDirectory(world.getWorldFolder(), worldCopy);
        } catch (IOException ex) {
            p.sendMessage("Something went wrong...");
            return;
        }
        p.sendMessage("Done!");
    }
}