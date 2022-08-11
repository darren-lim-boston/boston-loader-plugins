package narrative.narrative;

import static narrative.narrative.NarrativeUtils.NAME_MAYOR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
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
import narrative.narration.NarrationMessage;
import narrative.narration.NarrationSound;
import narrative.narration.NarrationTeleport;
import narrative.narration.NarrationTextQuestion;

public class IntroductionMayorNarrative extends Narrative {


    public IntroductionMayorNarrative(GameMaster master) {
        super(master, 2000);
    }

    @Override
    public LinkedList<Narration> getNarration() {
        LinkedList<Narration> narration = new LinkedList<>();

        SoundEffect introDialogueSound = new SoundEffect(Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1.25f);
        SoundEffect questionDialogueSound = new SoundEffect(Sound.ENTITY_VILLAGER_TRADE, 1f, 1.25f);
        SoundEffect dialogueSound = new SoundEffect(Sound.ENTITY_VILLAGER_YES, 1f, 0.75f);
        narration.add(new NarrationMessage(NAME_MAYOR, 7, "Welcome to the City of Boston! Today we will be celebrating our city's resilience and community as a city.", introDialogueSound));
        narration.add(new NarrationTeleport(4, new Location(Bukkit.getWorld("world-city"), 3745, 27, -3388).setDirection(new Vector(-0.3, -0.5, -0.8)), true));
        narration.add(new NarrationMessage(NAME_MAYOR, 1, "Today we are here to celebrate our city's dozens upon dozens of departments, which collectively keep us all safe and prosperous.", dialogueSound));
        narration.add(new NarrationTeleport(5, new Location(Bukkit.getWorld("world-city"), 3466, 13, -4961).setDirection(new Vector(0.6, -0.4, -0.7)), true));
        narration.add(new NarrationTeleport(5, new Location(Bukkit.getWorld("world-city"), 3935, 25, -5304).setDirection(new Vector(-0.3, -0.2, 0.9)), true));
        narration.add(new NarrationTeleport(1, getInitialPlayerLocation(), false));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "For instance, you may be familiar with the Boston Police Department, which handles our city's 9-1-1 calls.", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "The Department of Parks and Recreation creates and maintains our lovely parks, such as the Boston Commons!", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 4, "And the Environment Department develops our city's long term resilience plans to prepare our city for climate change!", dialogueSound));
        narration.add(new NarrationTextQuestion(NAME_MAYOR, "What is your name?", "Please type in your name.", "Steve", questionDialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 3, "Today, we'd like to highlight one department in particular.", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 3, "When they do everything right, we don't even know about them!", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 1, "I'd like to introduce $NAME from the Office of Emergency Management, or OEM!", dialogueSound));

        HashMap<Integer, SoundEffectLocation[]> soundEffects = new HashMap<>();
        for(int timestep = 5; timestep >= 0; timestep--) {
            List<SoundEffectLocation> sfx = new ArrayList<>();
            for(int i = 0; i < (int) (Math.random() * 2 * timestep) + 5; i++) {
                sfx.add(new SoundEffectLocation(timestep == 5 ? Sound.ENTITY_VILLAGER_YES : Sound.ENTITY_VILLAGER_YES, 2, 1, master.getLocation(6805 + (Math.random() * 3 - 1.5), -49, -7727 + (Math.random() * 10 - 5)), (long) (Math.random() * 10)));                
            }
            soundEffects.put(timestep, sfx.toArray(new SoundEffectLocation[0]));
        }
        narration.add(new NarrationSound(4, soundEffects, true, false));

        narration.add(new NarrationMessage(NAME_MAYOR, 5, "OEM is in charge of keeping our city prepared and safe against any hazards or disasters that can occur in Boston.", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 5, "Heat emergencies, hurricanes, floods… They help us prepare for all of these relevant and upcoming disasters, especially before they happen!", dialogueSound));

        soundEffects = new HashMap<>();
        for(int timestep = 3; timestep >= 0; timestep--) {
            List<SoundEffectLocation> sfx = new ArrayList<>();
            for(int i = 0; i < (int) (Math.random() * 2 * timestep) + 5; i++) {
                sfx.add(new SoundEffectLocation(Sound.ENTITY_VILLAGER_NO, 2, 1, master.getLocation(6805 + (Math.random() * 3 - 1.5), -49, -7727 + (Math.random() * 10 - 5)), (long) (Math.random() * 10)));                
            }
            if(timestep == 3) {
                sfx.add(new SoundEffectLocation(Sound.ENTITY_ELDER_GUARDIAN_DEATH, 2, 0.5f, getInitialPlayerLocation()));                
                sfx.add(new SoundEffectLocation(Sound.WEATHER_RAIN, 2, 0.5f, getInitialPlayerLocation()));                
                sfx.add(new SoundEffectLocation(Sound.AMBIENT_CAVE, 2, 0.5f, getInitialPlayerLocation()));                
            }
            soundEffects.put(timestep, sfx.toArray(new SoundEffectLocation[0]));
        }
        narration.add(new NarrationSound(4, soundEffects, false, true));

        narration.add(new NarrationMessage(NAME_MAYOR, 3, "What is this?", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 2, "Oh dear, the City is going to flood!", dialogueSound));
        narration.add(new NarrationMessage(NAME_MAYOR, 5, "$NAME, we need your help now! Prepare the city for this upcoming flood, and protect our residents!", dialogueSound));

        return narration;
    }

    @Override
    public Location getInitialPlayerLocation() {
        return master.getLocation(6811, -48, -7721).setDirection(new Vector(0, 0, -1));
    }

    @Override
    public List<NarrativeEntity> getInitialNarrativeEntities() {
        List<NarrativeEntity> entities = new ArrayList<>();
        entities.add(new NarrativePlayerEntity(master.getLocation(6811, -48, -7723).setDirection(new Vector(0, -0.5, 1)), NarrativeUtils.MAYOR_SKIN_URL, NAME_MAYOR));
        
        for(int i = 0; i < 14; i += 2) {
            for(int j = -8; j < 20; j += 2) {
                entities.add(new NarrativeEntity(EntityType.VILLAGER, master.getLocation(6805 - i + (Math.random() * 1.5 - 0.75), -49, -7727 + j + (Math.random() * 1.5 - 0.75)).setDirection(new Vector(1, 0, 0))));
            }
        }
        return entities;
    }
    
}
