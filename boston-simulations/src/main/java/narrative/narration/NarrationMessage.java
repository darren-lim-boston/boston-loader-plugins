package narrative.narration;

import org.bukkit.Location;

import master.GameMaster;
import narrative.NarrativeEntity;
import narrative.SoundEffect;
import narrative.SoundEffectLocation;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;

public class NarrationMessage extends Narration {

    private boolean sentMessage;
    private final String speaker;
    private final String message;
    private final SoundEffect soundEffect;

    //messages can have special characters to put in attributes later, such as:
    //$NAME will insert the player's name
    public NarrationMessage(String speakerID, int timesteps, String message, SoundEffect soundEffect) {
        super(timesteps);

        this.speaker = speakerID;
        this.message = message;
        this.soundEffect = soundEffect;
        sentMessage = false;

        assert timesteps > 0;
        assert message != null;
    }

    public NarrationMessage(String speakerID, int timesteps, String message) {
        this(speakerID, timesteps, message, null);
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        if(!sentMessage) {
            sentMessage = true;

            NarrativeEntity speakerEntity = getNarrativeEntity(speaker);
            String speakerName = null;
            Location speakerLocation = null;
            if(speakerEntity == null) {
                speakerName = speaker;
                speakerLocation = master.getRespawnLocation();
            } else {
                speakerName = speakerEntity.getName();
                speakerLocation = speakerEntity.getLocation();
            }

            String finalMessage = message;
            if(message.contains("$NAME")) {
                if(master.getPlayerName() == null) {
                    finalMessage = message.replace("$NAME", ChatColor.RED + "ERROR: INVALID NAME" + ChatColor.WHITE);
                } else {
                    finalMessage = message.replace("$NAME", ChatColor.GREEN + master.getPlayerName() + ChatColor.WHITE);
                }
            }
            int beginIndex;
            while ((beginIndex = finalMessage.indexOf("$(SCORE ")) != -1) {
                int endIndex = -1;
                if((endIndex = finalMessage.substring(beginIndex).indexOf(")")) == -1) {
                    break;
                } else {
                    String gameName = finalMessage.substring(beginIndex + 8, beginIndex + endIndex);
                    int score = master.getScore(gameName);
                    if(score != -1) {
                        finalMessage = finalMessage.substring(0, beginIndex) + score + finalMessage.substring(beginIndex + endIndex + 1);
                    } else {
                        finalMessage = finalMessage.substring(0, beginIndex) + ChatColor.RED + "ERROR: INVALID SCORE" + ChatColor.WHITE + finalMessage.substring(beginIndex + endIndex + 1);
                    }
                }
            } 

            master.broadcastMessage("");

            if(finalMessage.contains("https://")) {
                //parse the URL
                int index = finalMessage.indexOf("https://");
                String before = finalMessage.substring(0, index);

                int lastIndex = index;
                while(lastIndex != finalMessage.length() && finalMessage.charAt(lastIndex) != ' ') {
                    lastIndex++;
                }

                String after = finalMessage.substring(lastIndex);
                String website = finalMessage.substring(index, lastIndex);

                ComponentBuilder messageBuilder = new ComponentBuilder("" + ChatColor.AQUA + ChatColor.BOLD + speakerName + ChatColor.WHITE + ": " + before + ChatColor.LIGHT_PURPLE + website + ChatColor.WHITE + after);
                messageBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, website));
                messageBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.LIGHT_PURPLE + "Click to open URL")));

                master.broadcastMessage(messageBuilder.create());
            } else {
                master.broadcastMessage("" + ChatColor.AQUA + ChatColor.BOLD + speakerName + ChatColor.WHITE + ": " + finalMessage);                
            }

            if(soundEffect != null) {
                if(soundEffect instanceof SoundEffectLocation) {
                    ((SoundEffectLocation) soundEffect).play(master);
                } else {
                    soundEffect.play(master, speakerLocation);
                }
            }
        }
    }
}