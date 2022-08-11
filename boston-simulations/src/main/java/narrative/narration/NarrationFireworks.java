package narrative.narration;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import master.GameMaster;

public class NarrationFireworks extends NarrationEntityGesture {

    private Location[] fireworkLocations;

    public NarrationFireworks(int timesteps, Location... fireworkLocations) {
        super(timesteps, true, false);

        this.fireworkLocations = fireworkLocations;
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        super.processTimestepInternal(master);

        for(Location loc : fireworkLocations) {
            Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK, true);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(1 + (int) (Math.random() * 2));
            meta.addEffect(FireworkEffect.builder().flicker(true).withTrail().withColor(Color.ORANGE).build());
            firework.setFireworkMeta(meta);
        }
    }
}