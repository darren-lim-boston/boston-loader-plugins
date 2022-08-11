package narrative.narrative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;

import master.GameMaster;
import narrative.Narrative;
import narrative.NarrativeEntity;
import narrative.SoundEffect;
import narrative.NarrativeEntity.NarrativePlayerEntity;
import narrative.narration.Narration;
import narrative.narration.NarrationMessage;
import narrative.narration.NarrationMultipleChoice;
import org.bukkit.util.Vector;

import static narrative.narrative.NarrativeUtils.NAME_OEM_MALE;
import static narrative.narrative.NarrativeUtils.NAME_OEM_FEMALE;

public class PostFloodNarrative extends Narrative {

    public PostFloodNarrative(GameMaster master) {
        super(master, 4000);
    }

    @Override
    public LinkedList<Narration> getNarration() {
        LinkedList<Narration> narration = new LinkedList<>();

        SoundEffect soundEffect = new SoundEffect(Sound.ENTITY_VILLAGER_CELEBRATE, 1.5f, 1);
        SoundEffect soundEffectNegative = new SoundEffect(Sound.ENTITY_VILLAGER_NO, 1.5f, 1);
        SoundEffect heatWaveSound = new SoundEffect(Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.5f);

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 5, "Great job, $NAME! With your help, we managed to help save $(SCORE Flood) residents from the rising tides.", soundEffect));
        narration.add(new NarrationMultipleChoice(NAME_OEM_MALE, "Something we need to think about now: what do we do with this water now? How do we rid of the flood to help the city function normally again?", new String[] {
            "We should just leave it be. The water will go away eventually.",
            "We need to fix the houses and buildings! People need places to stay.",
            "We need to build up more flood barriers in the future, so that flooding is harder to do.",
            "We need to drain the oceans, because water only causes us trouble."
        }, new String[] {
            "Hmm. I think the water is not going to go away! We can't just leave them to tackle this on their own!",
            "That is true! We definitely need to help out with the debris on the streets and buildings now to get people back on their feet. How do we solve this more sustainably in the future...",
            "Flood barriers will definitely help us with the rising water levels, but to what extent can we just put a wall over Boston?",
            "I wish, but we need water to survive!"
        }, new boolean[] {
            false,
            true,
            true,
            false
        }, soundEffectNegative));

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 4, "Oh, hm? I received news that the water is starting to go away!", soundEffectNegative));

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 6, "Oh no, $NAME, get ready! A New England heat wave is hitting us now—I’m sending you now to Boston Commons. Go and help our citizens!", heatWaveSound));

        return narration;
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        List<NarrativeEntity> entities = new ArrayList<>();

        entities.add(new NarrativePlayerEntity(master.getLocation(7280, -52, -7412), NarrativeUtils.OEM_FEMALE_SKIN_URL, NAME_OEM_FEMALE));
        entities.add(new NarrativePlayerEntity(master.getLocation(7284, -52, -7412), NarrativeUtils.OEM_MALE_SKIN_URL, NAME_OEM_MALE));

        return entities;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(7282, -50, -7407).setDirection(new Vector(0.013082028071612737,-0.05233576509056672,-0.9985438539362799));
    }
    
}
