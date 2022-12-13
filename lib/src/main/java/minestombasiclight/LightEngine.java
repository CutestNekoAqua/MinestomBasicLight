package minestombasiclight;
/*
 * Copyright Waterdev 2022, under the MIT License
 */

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;

import java.util.*;

/*
 * Copyright Waterdev 2022, under the MIT License
 */

public class LightEngine {

    private final SectionUtils utils = new SectionUtils();

    private final byte fullbright = 15; // 14
    private final byte half = 10; // 10
    private final byte dark = 7; // 7

    //https://github.com/PaperMC/Starlight/blob/6503621c6fe1b798328a69f1bca784c6f3ffcee3/src/main/java/ca/spottedleaf/starlight/common/light/SWMRNibbleArray.java#L25
    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8/4); // blocks / bytes per block

    byte[] recalcArray;
    boolean[][] exposed = new boolean[16][16];
    public void recalculateInstance(Instance instance) {
        List<Chunk> chunks = instance.getChunks().stream().toList();
        chunks.forEach((this::recalculateChunk));
    }

    public void recalculateChunk(Chunk chunk) {
        exposed = new boolean[16][16];
        sections.forEach(e -> Arrays.fill(e, true));
        List<Section> sections = new ArrayList<>(chunk.getSections());
        Collections.reverse(sections);
        sections.forEach(this::recalculateSection);
        chunk.setBlock(1,1,1,chunk.getBlock(1,1,1));
            /*if(chunk instanceof DynamicChunk) {
                DynamicChunk dynamicChunk = (DynamicChunk) chunk;
                try {
                    Field light = dynamicChunk.getClass().getDeclaredField("blockCache");
                    light.setAccessible(true);
                    CachedPacket cachedLight = (CachedPacket) light.get(chunk);
                    cachedLight.invalidate();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("not dynamic chunk");
            }*/
    }

    private void recalculateSection(Section section) {
        recalcArray = new byte[ARRAY_SIZE];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 15; y > -1; y--) {
                    if(!utils.lightCanPassThrough(Block.fromStateId((short) section.blockPalette().get(x, y, z)))) exposed[x][z] = false;
                    if(exposed[x][z]) {
                        set(utils.getCoordIndex(x,y,z), fullbright);
                    } else {
                        set(utils.getCoordIndex(x,y,z), dark);
                    }
                    //set(utils.getCoordIndex(x,y,z), 15);
                }
            }
        }
        section.setSkyLight(recalcArray);
        section.setBlockLight(recalcArray);
    }

    // operation type: updating
    public void set(final int x, final int y, final int z, final int value) {
        this.set((x & 15) | ((z & 15) << 4) | ((y & 15) << 8), value);
    }

    // https://github.com/PaperMC/Starlight/blob/6503621c6fe1b798328a69f1bca784c6f3ffcee3/src/main/java/ca/spottedleaf/starlight/common/light/SWMRNibbleArray.java#L410
    // operation type: updating
    public void set(final int index, final int value) {
        final int shift = (index & 1) << 2;
        final int i = index >>> 1;

        this.recalcArray[i] = (byte)((this.recalcArray[i] & (0xF0 >>> shift)) | (value << shift));
    }

}
