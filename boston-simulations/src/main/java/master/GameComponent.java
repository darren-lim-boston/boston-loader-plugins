package master;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import narrative.NarrativeEntity;
import narrative.NarrativeEntity.NarrativePlayerEntity;

public abstract class GameComponent {

    protected final GameMaster master;
    private final long time;

    protected HashMap<String, NarrativeEntity> entities;
    private Set<Chunk> entityChunks;

    public GameComponent(GameMaster master, long time) {
        this.master = master;
        this.time = time;
    }

    public void startComponent() {
        getInitialPlayerLocation().getWorld().setTime(time);
        getInitialPlayerLocation().getWorld().setSpawnLocation(getInitialPlayerLocation());

        for(Player p : master.getPlayers()) {
            //give players temporary blindness and don't let them move until the narrative ends
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            for(PotionEffect potionEffect: p.getActivePotionEffects()) {
                p.removePotionEffect(potionEffect.getType());
            }
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setSaturation(0);
            p.setExp(0);
            p.setLevel(master.getTotalScore());
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1000, false, false));
        }

        entities = new HashMap<>();
        entityChunks = new HashSet<>();

        //some narrative entities may be spawned at a delay: NPC bug when NPCs are spawned at the same time, the skins of all will not load
        HashMap<NarrativeEntity, Integer> delayedNarrativeEntities = new HashMap<>();
        int spawnedNPCCount = 1;

        for(NarrativeEntity ent : getInitialNarrativeEntities()) {
            if(ent instanceof NarrativePlayerEntity) {
                delayedNarrativeEntities.put(ent, spawnedNPCCount);
                spawnedNPCCount++;
                ((NarrativePlayerEntity) ent).setSkinURLLoadRetryDelay(spawnedNPCCount * 20);
                entities.put(ent.getName(), ent);
                entityChunks.add(ent.getLocation().getChunk());
                continue;
            }

            entities.put(ent.getName(), ent);
            ent.spawn(master);
            entityChunks.add(ent.getLocation().getChunk());
        }

        if(spawnedNPCCount > 1) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<NarrativeEntity> remove = new ArrayList<>();
                    for(NarrativeEntity ent : delayedNarrativeEntities.keySet()) {
                        int val = delayedNarrativeEntities.get(ent) - 1;

                        if(val <= 0) {
                            ent.spawn(master);
                            remove.add(ent);
                        } else {
                            delayedNarrativeEntities.put(ent, val);
                        }
                    }

                    for(NarrativeEntity ent : remove) {
                        delayedNarrativeEntities.remove(ent);
                    }

                    if(delayedNarrativeEntities.isEmpty()) {
                        cancel();
                    }
                }
            }.runTaskTimer(master.plugin, 5L, 10L);
        }

        for(Chunk chunk : entityChunks) {
            chunk.setForceLoaded(true);
        }
    }

    public void endComponent() {
        if(entities != null) {
            for(NarrativeEntity ent : entities.values()) {
                ent.dispose(master);
            }
            entities = null;

            for(Chunk chunk : entityChunks) {
                chunk.setForceLoaded(false);
            }
        }
    }

    public void onJoin(Player p) {}

    public abstract Location getInitialPlayerLocation();
    public abstract List<NarrativeEntity> getInitialNarrativeEntities();
}