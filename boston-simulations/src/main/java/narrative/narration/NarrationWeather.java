package narrative.narration;

import org.bukkit.Sound;

import master.GameMaster;

public class NarrationWeather extends Narration {

    private final boolean setThundering;

    public NarrationWeather(boolean setThundering) {
        super(2);

        this.setThundering = setThundering;
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        if(isFirstTimestep()) {
            if(setThundering) {
                master.broadcastSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 0.5f);
                master.getWorld().setStorm(true);
                master.getWorld().setThundering(true);
                master.getWorld().setWeatherDuration(20000);
                master.getWorld().setThunderDuration(20000);
            } else {
                master.getWorld().setThundering(false);
                master.getWorld().setClearWeatherDuration(20000);

            }
        }
    }
}