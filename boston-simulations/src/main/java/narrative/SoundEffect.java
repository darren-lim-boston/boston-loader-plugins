package narrative;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import master.GameMaster;

public class SoundEffect {
    public final Sound sound;
    public final float volume, pitch;
    public final long delay;

    public SoundEffect(Sound sound, float volume, float pitch, long delay) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.delay = delay;

        assert sound != null;
    }

    public SoundEffect(Sound sound, float volume, float pitch) {
        this(sound, volume, pitch, 0);
    }

    public void play(GameMaster master, Location loc) {
        if(delay > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().playSound(loc, sound, volume, pitch);
                }
            }.runTaskLater(master.plugin, delay);
        } else {
            loc.getWorld().playSound(loc, sound, volume, pitch);
        }
    }
}