package narrative.narrative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import master.GameMaster;
import narrative.Narrative;
import narrative.NarrativeEntity;
import narrative.NarrativeEntity.NarrativePlayerEntity;
import narrative.SoundEffect;
import narrative.SoundEffectLocation;
import narrative.narration.Narration;
import narrative.narration.NarrationEndCard;
import narrative.narration.NarrationFireworks;
import narrative.narration.NarrationMessage;
import narrative.narration.NarrationSound;

public class ConclusionMayorNarrative extends Narrative {

    public static final String NAME_MAYOR = "Mayor of Boston";

    public ConclusionMayorNarrative(GameMaster master) {
        super(master, 13000);
    }

    @Override
    public LinkedList<Narration> getNarration() {
        LinkedList<Narration> narration = new LinkedList<>();

        SoundEffect introDialogueSound = new SoundEffect(Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1.25f);
        SoundEffect dialogueSound = new SoundEffect(Sound.ENTITY_VILLAGER_YES, 1f, 0.75f);

        HashMap<Integer, SoundEffectLocation[]> soundEffects = new HashMap<>();
        for(int timestep = 5; timestep >= 0; timestep--) {
            List<SoundEffectLocation> sfx = new ArrayList<>();
            for(int i = 0; i < (int) (Math.random() * 2 * timestep) + 5; i++) {
                sfx.add(new SoundEffectLocation(timestep == 5 ? Sound.ENTITY_VILLAGER_YES : Sound.ENTITY_VILLAGER_YES, 2, 1, master.getLocation(6805 + (Math.random() * 3 - 1.5), -49, -7727 + (Math.random() * 10 - 5)), (long) (Math.random() * 10)));                
            }
            soundEffects.put(timestep, sfx.toArray(new SoundEffectLocation[0]));
        }
        narration.add(new NarrationSound(1, soundEffects, true, false));

        narration.add(new NarrationMessage(NAME_MAYOR, 1, "Congratulations to $NAME and Emergency Management for continuing to protect our city from disaster!", introDialogueSound));

        narration.add(new NarrationFireworks(4, new Location[] {master.getLocation(6805, -49, -7711), master.getLocation(6781, -48, -7713), master.getLocation(6780, -49, -7729), master.getLocation(6800, -49, -7738), master.getLocation(6773, -49, -7722)}));

        narration.add(new NarrationMessage(NAME_MAYOR, 4, "We may be grateful to OEM, but we need to stay prepared as a city in the case that this happens again.", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "I'm providing some more information where you can learn more about how to prepare for various emergencies in the coming years.", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 6, "First! Make sure you are signed up to receive emergency alerts from the city. This way you always know what might impact your neighborhood: https://www.boston.gov/departments/emergency-management#sign-up-for-alerts", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "Here, you can read more about how to prepare for a flood in Boston: https://www.boston.gov/departments/environment/flooding-boston", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "And here’s how to prepare for a heat emergency: https://www.boston.gov/departments/emergency-management/keeping-cool-heat", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "Finally, here’s where to go in a hurricane emergency: https://www.boston.gov/departments/emergency-management/neighborhood-emergency-shelter-map", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 1, "Thank you for keeping our city safe!", dialogueSound));

        narration.add(new NarrationFireworks(1, new Location[] {master.getLocation(6805, -49, -7711), master.getLocation(6781, -48, -7713), master.getLocation(6780, -49, -7729), master.getLocation(6800, -49, -7738), master.getLocation(6773, -49, -7722)}));
        narration.add(new NarrationEndCard());

        return narration;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(6811, -48, -7721).setDirection(new Vector(0, 0, -1));
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        List<NarrativeEntity> entities = new ArrayList<>();
        entities.add(new NarrativePlayerEntity(master.getLocation(6811, -48, -7723).setDirection(new Vector(0, -0.5, 1)), "https://www.minecraftskins.com/uploads/skins/2022/07/07/mooshroom-in-suit--rq--20541495.png?v520", NAME_MAYOR));
        
        for(int i = 0; i < 14; i += 2) {
            for(int j = -8; j < 20; j += 2) {
                entities.add(new NarrativeEntity(EntityType.VILLAGER, master.getLocation(6805 - i + (Math.random() * 1.5 - 0.75), -49, -7727 + j + (Math.random() * 1.5 - 0.75)).setDirection(new Vector(1, 1.5, 0))));
            }
        }
        return entities;
    }
    
}
