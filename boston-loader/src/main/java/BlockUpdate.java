import org.bukkit.Material;
import org.bukkit.World;

public class BlockUpdate {
    final int x, y, z;
    final Material material;

    public BlockUpdate(int x, int y, int z, Material material) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
    }

    //returns the original Material
    public BlockUpdate apply(World world) {
        Material originalMaterial = world.getType(x, y, z);
        world.setType(x, y, z, material);
        return new BlockUpdate(x, y, z, originalMaterial);
    }

    @Override
    public String toString() {
        return "BlockUpdate{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", material=" + material +
                '}';
    }
}