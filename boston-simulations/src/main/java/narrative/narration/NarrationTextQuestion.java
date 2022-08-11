package narrative.narration;

import org.bukkit.Sound;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import master.GameMaster;
import master.GenericEventHandler;
import narrative.SoundEffect;
import narrative.NarrativeEntity;
import net.md_5.bungee.api.ChatColor;

public class NarrationTextQuestion extends Narration {

    private final String speaker;
    private final String question, questionPrompt, defaultAnswer;
    private final SoundEffect soundEffect;

    public NarrationTextQuestion(String speaker, String question, String questionPrompt, String defaultAnswer, SoundEffect soundEffect) {
        super(60);

        this.speaker = speaker;
        this.question = question;
        this.questionPrompt = questionPrompt;
        this.defaultAnswer = defaultAnswer;
        this.soundEffect = soundEffect;
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        if(getTimestepsRemaining() == 60) {
            NarrativeEntity speakerEntity = getNarrativeEntity(speaker);
            master.broadcastMessage("");
            master.broadcastMessage("" + ChatColor.AQUA + ChatColor.BOLD + speakerEntity.getName() + ChatColor.WHITE + ": " + ChatColor.LIGHT_PURPLE + question);

            if(soundEffect != null) {
                soundEffect.play(master, speakerEntity.getLocation());
            }

            master.setEventHandler(new GenericEventHandler() {
                @Override
                public void onPlayerChat(AsyncPlayerChatEvent event) {
                    String message = event.getMessage();

                    master.broadcastMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "You typed: " + ChatColor.GREEN + message + ChatColor.LIGHT_PURPLE + "!");

                    master.setPlayerName(message);

                    dispose(master);
                    event.setCancelled(true);
                }
            });
        } else if(getTimestepsRemaining() == 59) {
            master.broadcastSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            master.broadcastMessage("");
            master.broadcastMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + questionPrompt);
        } else if(getTimestepsRemaining() == 0) {
            dispose(master);
            master.setPlayerName(defaultAnswer);
        }
    }

    @Override
    public void dispose(GameMaster master) {
        super.dispose(master);
        master.setEventHandler(null);
    }
}