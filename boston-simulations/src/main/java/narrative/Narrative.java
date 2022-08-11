package narrative;

import java.util.LinkedList;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import master.GameComponent;
import master.GameMaster;
import narrative.narration.Narration;

public abstract class Narrative extends GameComponent {

    private LinkedList<Narration> narrations = new LinkedList<>();

    public Narrative(GameMaster master, long timeOfDay) {
        super(master, timeOfDay);
    }

    public Narrative(GameMaster master) {
        this(master, 6000);
    }

    private void startNarrationLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Narration narration = narrations.getFirst();

                narration.processTimestep(master, entities);

                if(narration.isComplete()) {
                    narrations.pop();
                }
                if(narrations.isEmpty()) {
                    //incorporated 3 second delay before narration ends
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            endComponent();
                        }
                    }.runTaskLater(master.plugin, 60L);
                    cancel();
                }
            }
        }.runTaskTimer(master.plugin, 40L, 20L);
    }

    @Override
    public void startComponent() {
        super.startComponent();

        narrations = getNarration();

        for(Player p : master.getPlayers()) {
            p.setWalkSpeed(0);
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 200000, 500, false, false));
            p.setFoodLevel(1);
        }

        master.teleport(getInitialPlayerLocation());

        assert narrations != null;
        assert narrations.size() > 0;
        
        startNarrationLoop();
    }

    @Override
    public void endComponent() {
        super.endComponent();

        if(master.getPlayers() != null) {
            for(Player p : master.getPlayers()) {
                p.setWalkSpeed(0.2f);
                p.removePotionEffect(PotionEffectType.JUMP);
                p.setFoodLevel(20);
            }
        }

        master.endGameComponent();
    }

    public abstract LinkedList<Narration> getNarration();
}
