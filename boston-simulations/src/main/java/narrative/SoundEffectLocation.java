package narrative;

import org.bukkit.Location;
import org.bukkit.Sound;

import master.GameMaster;

public class SoundEffectLocation extends SoundEffect {

    private Location loc;

    public SoundEffectLocation(Sound sound, float volume, float pitch, Location loc, long delay) {
        super(sound, volume, pitch);

        this.loc = loc;
    }

    public SoundEffectLocation(Sound sound, float volume, float pitch, Location loc) {
        this(sound, volume, pitch, loc, 0);
    }

    public void play(GameMaster master) {
        super.play(master, loc);
    }
}