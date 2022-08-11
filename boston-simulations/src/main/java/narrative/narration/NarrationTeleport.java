package narrative.narration;

import org.bukkit.Location;
import org.bukkit.Material;

import master.GameMaster;

public class NarrationTeleport extends Narration {

    private final Location location;
    private final boolean preventFalling;

    public NarrationTeleport(int timesteps, Location location, boolean preventFalling) {
        super(timesteps);

        this.location = location;
        this.preventFalling = preventFalling;
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        if(isFirstTimestep()) {
            if(preventFalling) {
                if(location.clone().add(0, -1, 0).getBlock().getType() == Material.AIR) {
                    location.clone().add(0, -1, 0).getBlock().setType(Material.BARRIER);
                }
            }

            master.teleport(location);
        }
    }
}