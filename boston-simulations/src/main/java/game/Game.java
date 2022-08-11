package game;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import master.GameComponent;
import master.GameMaster;
import net.md_5.bungee.api.ChatColor;

public abstract class Game extends GameComponent {

    private boolean isInTimeout;
    private int initialSeconds, secondsLeft;

    private BossBar progressBar;
    private BukkitRunnable gameWorker;

    public Game(GameMaster master) {
        super(master, 6000);
    }

    private String getTimeLeftFormatted() {
        if(secondsLeft > 60) {
            return (secondsLeft / 60) + " minute" + ((secondsLeft / 60) == 1 ? "" : "s") + " left";
        } else {
            return secondsLeft + " second" + (secondsLeft == 1 ? "" : "s") + " left";
        }
    }

    @Override
    public void startComponent() {
        super.startComponent();

        //set pre-data
        secondsLeft = getInitialGameSeconds();
        initialSeconds = secondsLeft;
        isInTimeout = false;

        playProgressBar();
        //play title card
        //teleport players
        //give players items
        for(Player p : master.getPlayers()) {
            p.setWalkSpeed(0.2f);
            p.sendTitle("" + ChatColor.GOLD + ChatColor.BOLD + "BOSTON: " + getGameName().toUpperCase(), ChatColor.LIGHT_PURPLE + "Goal: " + ChatColor.WHITE + getGameGoal(), 10, 100, 20);
            p.teleport(getInitialPlayerLocation());
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1, 0.5f);
            p.playSound(p.getLocation(), getGameMusic(), getGameMusic().toString().startsWith("MUSIC_DISC") ? Float.MAX_VALUE : 1, 1);
            List<ItemStack> items = getInitialPlayerItems();
            if(items.size() <= 9) {
                for(int i = 0; i < items.size(); i++) {
                    p.getInventory().setItem(i, items.get(i));
                }
            } else {
                p.getInventory().addItem(getInitialPlayerItems().toArray(new ItemStack[0]));
            }
        }

        runGameInit();
        gameWorker = getGameWorker();
    }

    private void playProgressBar() {
        progressBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + getGameTimeUntilEndText() + ": " + ChatColor.GRAY + getTimeLeftFormatted(), BarColor.PURPLE, BarStyle.SEGMENTED_10);
        progressBar.setProgress(1);
        for(Player p : master.getPlayers()) {
            progressBar.addPlayer(p);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if(isInTimeout) {
                    if(secondsLeft % 2 == 0) {
                        progressBar.setTitle("" + ChatColor.DARK_GREEN + ChatColor.BOLD + "TIME'S UP!");
                    } else {
                        progressBar.setTitle("" + ChatColor.RED + ChatColor.BOLD + "TIME'S UP!");
                    }
                } else {
                    if(getSecondsLeft() == getInitialGameSeconds() - 5) {
                        String[] tipText = getTipMessage();
                        if(tipText != null) {
                            new BukkitRunnable() {
                                int index = 0;
                                @Override
                                public void run() {
                                    master.broadcastSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                                    if(tipText.length == 1) {
                                        master.broadcastMessage(ChatColor.GREEN + "TIP: " + ChatColor.WHITE + tipText[index]);
                                    } else {
                                        master.broadcastMessage(ChatColor.GREEN + "TIP #" + (index + 1) + ": " + ChatColor.WHITE + tipText[index]);
                                    }

                                    index++;

                                    if(index == tipText.length) {
                                        cancel();
                                    }
                                }
                            }.runTaskTimer(master.plugin, 0, 60);
                        }
                    }

                    progressBar.setProgress((double) secondsLeft / initialSeconds);
                    if(secondsLeft <= 10) {
                        if(secondsLeft % 2 == 0) {
                            progressBar.setTitle(ChatColor.LIGHT_PURPLE + getGameTimeUntilEndText() + ": " + ChatColor.BOLD + ChatColor.RED + getTimeLeftFormatted());
                        } else {
                            progressBar.setTitle(ChatColor.LIGHT_PURPLE + getGameTimeUntilEndText() + ": " + ChatColor.BOLD + ChatColor.DARK_RED + getTimeLeftFormatted());
                        }
                    } else {
                        progressBar.setTitle(ChatColor.LIGHT_PURPLE + getGameTimeUntilEndText() + ": " + ChatColor.GRAY + getTimeLeftFormatted());
                    }
    
                    if(secondsLeft == 120) {
                        progressBar.setColor(BarColor.YELLOW);
                    } else if(secondsLeft == 60) {
                        progressBar.addFlag(BarFlag.PLAY_BOSS_MUSIC);
                        progressBar.setColor(BarColor.RED);
                    }

                    if(secondsLeft <= 10) {
                        for(Player p : master.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                        }
                    }
                }

                secondsLeft--;

                if(secondsLeft == -1) {
                    if(isInTimeout) {
                        endComponent();
                        cancel();
                    } else {
                        isInTimeout = true;
                        secondsLeft = getTimeoutGameSecondsBeforeEnd();
                        initialSeconds = secondsLeft;

                        progressBar.setTitle("" + ChatColor.RED + ChatColor.BOLD + "TIME'S UP!");
                        progressBar.setProgress(0);

                        for(Player p : master.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 2f, 0.5f);
                            p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2f, 0.5f);
                        }
                    } 
                }
            }
        }.runTaskTimer(master.plugin, 0L, 20L);
    }

    @Override
    public void onJoin(Player p) {
        if(master.getPlayers().contains(p)) {
            progressBar.addPlayer(p);
        }
    } 

    @Override
    public void endComponent() {
        //record score before anything happens
        if(master.isInProgress()) {
            master.recordScore(getGameName(), getCurrentScore());
        }
        super.endComponent();

        if(progressBar == null) {
            return;
        }

        for(Player p : master.getPlayers()) {
            progressBar.removePlayer(p);
            p.getInventory().clear();
            p.stopSound(getGameMusic());
        }   

        progressBar = null;

        if(gameWorker != null) {
            gameWorker.cancel();
        }

        runGameEnd();

        master.endGameComponent();
    }

    protected int getSecondsLeft() {
        return secondsLeft;
    }

    protected boolean isInTimeout() {
        return isInTimeout;
    }

    protected abstract void runGameInit();
    protected void runGameEnd() {}
    protected abstract BukkitRunnable getGameWorker();
    public abstract String getGameName();
    public abstract String getGameGoal();
    public abstract String getGameTimeUntilEndText();
    public abstract int getInitialGameSeconds();
    public abstract int getTimeoutGameSecondsBeforeEnd();
    public abstract int getCurrentScore();
    public abstract List<ItemStack> getInitialPlayerItems();
    public abstract Sound getGameMusic();

    public String[] getTipMessage() {
        return null;
    }
}
