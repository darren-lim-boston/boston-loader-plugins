package master;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import game.FloodGame;
import game.HeatWaveGame;
import game.HurricaneGame;
import narrative.narrative.ConclusionMayorNarrative;
import narrative.narrative.IntroductionMayorNarrative;
import narrative.narrative.PostFloodNarrative;
import narrative.narrative.PostHeatWaveNarrative;
import narrative.narrative.PostHurricaneNarrative;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;

public class GameMaster implements Listener {

    public enum GameDifficulty {
        EASY, NORMAL
    }

    public enum Gamemode {
        ALL_GAMES, FLOOD_ONLY, HEAT_WAVE_ONLY, HURRICANE_ONLY
    }

    public final JavaPlugin plugin;
    private boolean isInProgress;
    private List<UUID> playerIDs;
    private GenericEventHandler currentEventHandler;
    private String playerName;
    private HashMap<String, Integer> scores;

    private GameComponent[] gameComponents;
    private int currentIndex;
    private boolean forceEndGame;

    private GameDifficulty difficulty;
    
    public GameMaster(JavaPlugin plugin) {
        this.plugin = plugin;
        isInProgress = false;
        currentIndex = -1;
    }

    private void loadComponents(Gamemode gamemode, String name) {
        switch(gamemode) {
            case ALL_GAMES:
                gameComponents = new GameComponent[] {
                        new IntroductionMayorNarrative(this),
                        new FloodGame(this),
                        new PostFloodNarrative(this),
                        new HeatWaveGame(this),
                        new PostHeatWaveNarrative(this),
                        new HurricaneGame(this),
                        new PostHurricaneNarrative(this),
                        new ConclusionMayorNarrative(this)
                };
                break;
            default:
                playerName = name;
                switch(gamemode) {
                    case FLOOD_ONLY:
                        gameComponents = new GameComponent[] {
                                new FloodGame(this),
                                new PostFloodNarrative(this),
                                new ConclusionMayorNarrative(this),
                        };
                        break;
                    case HEAT_WAVE_ONLY:
                        gameComponents = new GameComponent[] {
                                new HeatWaveGame(this),
                                new PostHeatWaveNarrative(this),
                                new ConclusionMayorNarrative(this),
                        };
                        break;
                    case HURRICANE_ONLY:
                        gameComponents = new GameComponent[] {
                                new HurricaneGame(this),
                                new PostHurricaneNarrative(this),
                                new ConclusionMayorNarrative(this),
                        };
                        break;
                }
                break;
        }
    }

    public void _DEBUGPLAYERS(Player p) {
        if(playerIDs == null) {
            playerIDs = new ArrayList<>();
        }
        playerIDs.add(p.getUniqueId());
    }

    public boolean unloadReloadWorld() {
        teleport(Bukkit.getWorld("world-city").getSpawnLocation());

        World world = Bukkit.getWorld("world-game");
        if(world != null) {
            File worldFolder = world.getWorldFolder();
            if(!Bukkit.unloadWorld(world, false)) {
                return false;
            }
            //delete world folder

            try {
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException ex) {
                return false;
            }
        }

        //copy world folder over
        try {
            File mainFolder = Bukkit.getWorld("world-city").getWorldFolder().getParentFile();
            File worldCopy = new File(mainFolder, "world-backup");
            File worldNew = new File(mainFolder, "world-game");

            assert worldCopy.exists();
            assert !worldNew.exists();

            FileUtils.copyDirectory(worldCopy, worldNew);
        } catch (IOException ex) {
            return false;
        }

        Bukkit.createWorld(new WorldCreator("world-game"));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    public boolean start(boolean reloadWorld, GameDifficulty difficulty) {
        return start(reloadWorld, difficulty, Gamemode.ALL_GAMES, null);
    }

    public boolean start(boolean reloadWorld, GameDifficulty difficulty, Gamemode gamemode, String name) {
        if(isInProgress) {
            return false;
        }

        //load gamemode components
        loadComponents(gamemode, name);
        forceEndGame = false;

        //pre-load data
        isInProgress = true;
        playerIDs = new ArrayList<>();

        for(Player p : Bukkit.getOnlinePlayers()) {
            playerIDs.add(p.getUniqueId());
        }

        scores = new HashMap<>();
        this.difficulty = difficulty;

        //load the world manually, using the script
        if(reloadWorld) {
            broadcastMessage(ChatColor.GRAY + "Please wait, loading the world...");
            if(!unloadReloadWorld()) {
                broadcastMessage(ChatColor.GRAY + "Something went wrong. Please consult the owner of this server for help. (ERROR: could not delete old world)");
                isInProgress = false;
                playerIDs = null;
                return false;
            }
        }

        broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "We are starting the game!");

        //start first component
        currentIndex = 0;
        gameComponents[currentIndex].startComponent();

        return true;
    }

    public void forceEndGame() {
        if(currentIndex != -1 && currentIndex < gameComponents.length) {
            forceEndGame = true;
            gameComponents[currentIndex].endComponent();
            endGame();
        }
    }

    private void endGame() {
        if(isInProgress) {
            broadcastMessage("");
            broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "We have finished the game!");

            getWorld().setStorm(false);
            getWorld().setThundering(false);
            
            //cleanup data
            isInProgress = false;
            playerIDs = null; 
        }
    }

    public void endGameComponent() {
        if(forceEndGame) {
            return;
        }

        //remove any listeners
        currentEventHandler = null;

        currentIndex++;
        if(currentIndex == gameComponents.length) {
            endGame();
        } else {
            gameComponents[currentIndex].startComponent();
        }
    }

    public Location getRespawnLocation() {
        if(isInProgress) {
            return gameComponents[currentIndex].getInitialPlayerLocation();
        } else {
            return null;
        }
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();

        for(UUID id : playerIDs) {
            Player p = Bukkit.getPlayer(id);
            if(!p.isOnline()) {
                continue;
            }
            players.add(p);
        }

        return players;
    }

    public void broadcastMessage(String message) {
        for(Player p : getPlayers()) {
            p.sendMessage(message);
        }
    }

    public void broadcastMessage(BaseComponent... message) {
        for(Player p : getPlayers()) {
            p.spigot().sendMessage(message);
        }
    }

    public void broadcastSound(Sound sound, float volume, float pitch) {
        for(Player p : getPlayers()) {
            p.playSound(p.getLocation(), sound, volume, pitch);
        }
    }

    public void teleport(Location loc) {
        for(Player p : getPlayers()) {
            p.teleport(loc);
        }
    }

    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    public World getWorld() {
        return Bukkit.getWorld("world-game");
    }

    public Location getLocation(int x, int y, int z) {
        return new Location(getWorld(), x + 0.5, y, z + 0.5);
    }

    public Location getLocation(double x, double y, double z) {
        return new Location(getWorld(), x + 0.5, y, z + 0.5);
    }

    //persistent storage

    public void setPlayerName(String name) {
        playerName = name;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void recordScore(String gameName, int score) {
        scores.put(gameName, score);
    }

    public int getScore(String gameName) {
        return scores.getOrDefault(gameName, -1);
    }

    public boolean isInProgress() {
        return isInProgress;
    }

    public int getTotalScore() {
        int score = 0;

        for(int gameScore : scores.values()) {
            score += gameScore;
        }

        return score;
    }

    //unmodifiable event handlers

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(isInProgress) {
            gameComponents[currentIndex].onJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if(getRespawnLocation() != null) {
            event.setRespawnLocation(getRespawnLocation());
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.setRespawnLocation(getRespawnLocation());
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if(isInProgress) {
            ItemStack item = event.getItemDrop().getItemStack();

            if(item.hasItemMeta() && item.getItemMeta().hasLore()) {
                if(item.getItemMeta().getLore().size() > 1 && item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1).startsWith("" + ChatColor.BLACK)) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop this item right now.");
                    event.setCancelled(true);
                }
            }
        }
    }

    //event handlers

    public void setEventHandler(GenericEventHandler handler) {
        currentEventHandler = handler; 
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if(currentEventHandler != null) {
            currentEventHandler.onPlayerChat(event);
        }
    }

    @EventHandler
    public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
        if(currentEventHandler != null) {
            currentEventHandler.onPlayerEntityInteract(event);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(currentEventHandler != null) {
            currentEventHandler.onPlayerInteract(event);
        }
    }

    @EventHandler
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
        if(currentEventHandler != null && event.getEntityType() == EntityType.FALLING_BLOCK) {
            currentEventHandler.onFallingBlockLand(event);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(currentEventHandler != null) {
            currentEventHandler.onInventoryClick(event);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if(currentEventHandler != null) {
            currentEventHandler.onPotionSplash(event);
        }
    }
}
