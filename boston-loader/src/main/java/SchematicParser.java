import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.math.BlockVector3;

public class SchematicParser {

    static <T extends Tag> T requireTag(Map<String, Tag> items, String key, Class<T> expected) throws IOException {
        if (!items.containsKey(key)) {
            throw new IOException("Schematic file is missing a \"" + key + "\" tag of type "
                + expected.getName());
        }

        Tag tag = items.get(key);
        if (!expected.isInstance(tag)) {
            throw new IOException(key + " tag is not of tag type " + expected.getName() + ", got "
                + tag.getClass().getName() + " instead");
        }

        return expected.cast(tag);
    }

    public static BlockVector3 getOrigin(File schematicFile) {
        BlockVector3 origin = BlockVector3.ZERO;
        try {
            NBTInputStream inputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(new FileInputStream(schematicFile))));
            NamedTag rootTag = inputStream.readNamedTag();
            if (!rootTag.getName().equals("Schematic")) {
                throw new IOException("Tag 'Schematic' does not exist or is not first");
            }
            CompoundTag schematicTag = (CompoundTag) rootTag.getTag();

            Map<String, Tag> schematic = schematicTag.getValue();
            
            // ====================================================================
            // Metadata
            // ====================================================================

            // Get information
            int originX = requireTag(schematic, "WEOffsetX", IntTag.class).getValue();
            int originY = requireTag(schematic, "WEOffsetY", IntTag.class).getValue();
            int originZ = requireTag(schematic, "WEOffsetZ", IntTag.class).getValue();
            BlockVector3 min = BlockVector3.at(originX, originY, originZ);

            origin = min;

            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return origin;
    }
}