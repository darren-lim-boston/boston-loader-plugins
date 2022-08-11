package narrative.narration;

import java.util.HashMap;

import master.GameMaster;
import narrative.NarrativeEntity;

public abstract class Narration {

    protected final int originalTimestepsRemaining;
    private int timestepsRemaining;

    protected HashMap<String, NarrativeEntity> entities;

    public Narration(int timesteps) {
        timestepsRemaining = timesteps;
        originalTimestepsRemaining = timesteps;
        assert timesteps > 0;
    }

    public final void processTimestep(GameMaster master, HashMap<String, NarrativeEntity> entities) {
        if(timestepsRemaining == 0) {
            return;
        }

        this.entities = entities;
        processTimestepInternal(master);
        this.entities = null;

        timestepsRemaining--;
    }

    //set to 0 by next event
    public void dispose(GameMaster master) {
        timestepsRemaining = 1;
    }

    //method to be called by the internal timestep processing ONLY
    public final NarrativeEntity getNarrativeEntity(String name) {
        return entities.get(name);
    }

    public abstract void processTimestepInternal(GameMaster master);

    protected boolean isFirstTimestep() {
        return timestepsRemaining == originalTimestepsRemaining;
    }

    protected int getTimestepsRemaining() {
        return timestepsRemaining;
    }

    public final boolean isComplete() {
        return timestepsRemaining == 0;
    }

    public final void reset() {
        timestepsRemaining = originalTimestepsRemaining + 1;
    }
}