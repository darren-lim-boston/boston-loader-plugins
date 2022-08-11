package narrative.narration;

import java.util.HashMap;

import master.GameMaster;
import narrative.SoundEffectLocation;

public class NarrationSound extends NarrationEntityGesture {

    private final HashMap<Integer, SoundEffectLocation[]> soundEffectsByTimestep;

    public NarrationSound(int timesteps, HashMap<Integer, SoundEffectLocation[]> soundEffectsByTimestep, boolean showAgreement, boolean showDisagreement) {
        super(timesteps, showAgreement, showDisagreement);

        this.soundEffectsByTimestep = soundEffectsByTimestep;
    }

    public NarrationSound(int timesteps, HashMap<Integer, SoundEffectLocation[]> soundEffectsByTimestep) {
        this(timesteps, soundEffectsByTimestep, false, false);
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        super.processTimestepInternal(master);
        if(soundEffectsByTimestep.containsKey(getTimestepsRemaining())) {
            SoundEffectLocation[] soundEffects = soundEffectsByTimestep.get(getTimestepsRemaining());
            for(SoundEffectLocation sfx : soundEffects) {
                sfx.play(master);
            }
        }
    }
}