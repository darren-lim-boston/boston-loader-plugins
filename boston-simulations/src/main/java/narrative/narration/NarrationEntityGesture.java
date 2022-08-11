package narrative.narration;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;

import master.GameMaster;
import narrative.NarrativeEntity;

public class NarrationEntityGesture extends Narration {

    private final boolean showAgreement;
    private final boolean showDisagreement;

    public NarrationEntityGesture(int timesteps, boolean showAgreement, boolean showDisagreement) {
        super(timesteps);
        
        this.showAgreement = showAgreement;
        this.showDisagreement = showDisagreement;
    }

    @Override
    public void processTimestepInternal(GameMaster master) {
        //show in up to 3 cases, when is first timestep and when halfway through
        if(isFirstTimestep() || (originalTimestepsRemaining / 2 == getTimestepsRemaining()) || (getTimestepsRemaining() == 1)) {
            if(showAgreement) {
                for(NarrativeEntity ent : entities.values()) {
                    if(ent.getEntity() instanceof LivingEntity) {
                        ((LivingEntity) ent.getEntity()).teleport(ent.getLocation().setDirection(ent.getEntity().getLocation().getDirection().setY(1.5)));
                    }
                }
            } else if (showDisagreement) {
                for(NarrativeEntity ent : entities.values()) {
                    if(ent.getEntity() instanceof Villager) {
                        ((Villager) ent.getEntity()).shakeHead();
                    }
                }
            }
        }
    }
    
}
