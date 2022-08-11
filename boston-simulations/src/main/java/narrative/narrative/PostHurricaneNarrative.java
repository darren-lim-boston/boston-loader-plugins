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
import narrative.narration.NarrationWeather;

import static narrative.narrative.NarrativeUtils.NAME_OEM_MALE;
import static narrative.narrative.NarrativeUtils.NAME_OEM_FEMALE;

public class PostHurricaneNarrative extends Narrative {

    public PostHurricaneNarrative(GameMaster master) {
        super(master, 13000);
    }

    @Override
    public LinkedList<Narration> getNarration() {
        LinkedList<Narration> narration = new LinkedList<>();

        SoundEffect soundEffect = new SoundEffect(Sound.ENTITY_VILLAGER_CELEBRATE, 1.5f, 1);
        SoundEffect soundEffectNegative = new SoundEffect(Sound.ENTITY_VILLAGER_NO, 1.5f, 1);

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 4, "That was difficult, but you did a great job handling this emergency situation! You helped save $(SCORE Hurricane) residents from harm's way.", soundEffect));

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 6, "So many things happened today… I think we are finally safe now, but this seems to be only the beginning for us in Boston.", soundEffect));

        narration.add(new NarrationMultipleChoice(NAME_OEM_MALE, "What are some of the best strategies to do in a hurricane?", new String[] {
            "Stay indoors, go on lower floors, shelter in place. Seal all windows.",
            "Go outdoors and hide underneath sturdy things, like trees.",
            "Open the windows and doors in your house to protect them from damage.",
            "If outside, find an indoors shelter as fast as possible."
        }, new String[] {
            "Correct! This is good because there may not be enough time to go somewhere else, and going lower makes it less dangerous if debris starts to fly around.",
            "Trees are sturdy, but it is generally not a good idea to go outdoors, because there can be flying debris and worse, the hurricane might be nearby.",
            "Opening the windows and doors is actually the opposite of what you should be doing, because flying debris and water can easily fly inside your home with an open window or door. This is dangerous!",
            "Great! Boston has emergency shelters for different neighborhoods in the city in case of this emergency. Staying indoors is better and safer than staying outdoors, because you will be protected from flying debris."
        }, new boolean[] {
            true,
            false,
            false,
            true
        }, soundEffectNegative));

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 6, "Great job, $NAME! Now, weren’t we supposed to be finishing a mayor’s speech…?", soundEffect));
        narration.add(new NarrationWeather(false));

        return narration;
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        List<NarrativeEntity> entities = new ArrayList<>();

        entities.add(new NarrativePlayerEntity(master.getLocation(6679, -52, -8267), NarrativeUtils.OEM_FEMALE_SKIN_URL, NAME_OEM_FEMALE));
        entities.add(new NarrativePlayerEntity(master.getLocation(6677, -52, -8269), NarrativeUtils.OEM_MALE_SKIN_URL, NAME_OEM_MALE));

        return entities;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(6679, -52, -8272);
    }
    
}
