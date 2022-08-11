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
import org.bukkit.util.Vector;

import static narrative.narrative.NarrativeUtils.NAME_OEM_MALE;
import static narrative.narrative.NarrativeUtils.NAME_OEM_FEMALE;

public class PostHeatWaveNarrative extends Narrative {

    public PostHeatWaveNarrative(GameMaster master) {
        super(master, 6000);
    }

    @Override
    public LinkedList<Narration> getNarration() {
        LinkedList<Narration> narration = new LinkedList<>();

        SoundEffect soundEffect = new SoundEffect(Sound.ENTITY_VILLAGER_CELEBRATE, 1.5f, 1);
        SoundEffect soundEffectNegative = new SoundEffect(Sound.ENTITY_VILLAGER_NO, 1.5f, 1);
        SoundEffect soundEffectScared = new SoundEffect(Sound.ENTITY_VILLAGER_DEATH, 1.5f, 0.5f);

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 2, "You did a great job!", soundEffect));
        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 5, "With your help, we brought $(SCORE Heat Wave) residents to safety from the scorching heat! Surely we must be safe now from harm.", soundEffect));
        narration.add(new NarrationMultipleChoice(NAME_OEM_MALE, "What are we supposed to do when heat emergencies become more regular? How are we going to protect ourselves and our community?", new String[] {
            "We should put a shade over the entirety of Boston to protect us from this sun!",
            "We need to put more shade, like trees and other cooling centers, to help people stay cool during hot days.",
            "We need to get rid of all gasoline and go electric, and then we will make the heat waves less hot in the future!",
            "We need to make sure residents can cool down at the park and other outdoor areas!"
        }, new String[] {
            "That would be too expensive! We also need the sun to give us essential vitamin D!",
            "That's a great idea! We can only do so much to get rid of heat waves, but at the moment, it's great for people to have shade to protect themselves from the scorching sun!",
            "That is true, but this is harder to do in practice! In an ideal world we would be saving our planet, but we need to do something now to help people out.",
            "Absolutely! The parks and water are great ways to get people to cool down, and it only makes the point that our parks are needed more clear."
        }, new boolean[] {
            false,
            true,
            false,
            true
        }, soundEffectNegative));

        narration.add(new NarrationWeather(true));

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 2, "Uh...", soundEffectNegative));

        narration.add(new NarrationMessage(NAME_OEM_MALE, 3, "What is happening...?", soundEffectNegative));

        narration.add(new NarrationMessage(NAME_OEM_FEMALE, 5, "Oh no... oh no! Boston is facing a hurricane! I’m assigning you to go help Langone Park. Quickly, $NAME, get out there and warn the residents before it's too late!", soundEffectScared));

        return narration;
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        List<NarrativeEntity> entities = new ArrayList<>();

        entities.add(new NarrativePlayerEntity(master.getLocation(5847, -31, -7105), NarrativeUtils.OEM_FEMALE_SKIN_URL, NAME_OEM_FEMALE));
        entities.add(new NarrativePlayerEntity(master.getLocation(5848, -31, -7107), NarrativeUtils.OEM_MALE_SKIN_URL, NAME_OEM_MALE));

        return entities;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(5849, -31, -7106).setDirection(new Vector(-0.9947616662933177,-0.007853903801453939,-0.10191929880262844));
    }
    
}
