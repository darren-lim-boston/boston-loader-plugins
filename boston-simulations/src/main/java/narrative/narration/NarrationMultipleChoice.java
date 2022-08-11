package narrative.narration;

import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import master.GameMaster;
import master.GenericEventHandler;
import narrative.SoundEffect;
import narrative.NarrativeEntity;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;

public class NarrationMultipleChoice extends Narration {

    private final String speaker, question;
    private final String[] answers, responses;
    private final boolean[] correctMask;
    private final SoundEffect soundEffect;

    private String id;

    public NarrationMultipleChoice(String speaker, String question, String[] answers, String[] responses, boolean[] correctMask, SoundEffect soundEffect) {
        super(90);

        this.speaker = speaker;
        this.question = question;
        this.answers = answers;
        this.responses = responses;
        this.correctMask = correctMask;
        this.soundEffect = soundEffect;

        id = UUID.randomUUID().toString();

        assert answers.length == responses.length;
        assert answers.length == correctMask.length;
    }
    
    @Override
    public void processTimestepInternal(GameMaster master) {
        if(isFirstTimestep()) {
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
                    if(!message.contains(" ")) {
                        return;
                    }
                    String[] idIndex = message.split(" ");
                    if(idIndex.length != 3) {
                        return;
                    }
                    if(!idIndex[0].equals("MULT_CHOICE_ANSWER")) {
                        return;
                    }
                    if(!idIndex[1].equals(id)) {
                        event.setCancelled(true);
                        return;
                    }

                    try {
                        master.broadcastSound(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1, 1);
                        id = null;
                        int index = Integer.parseInt(idIndex[2]);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                master.broadcastMessage("");
                                master.broadcastMessage("" + ChatColor.AQUA + ChatColor.BOLD + speakerEntity.getName() + ChatColor.WHITE + ": " + responses[index]);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if(correctMask[index]) {
                                            master.broadcastSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                                            master.broadcastMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Correct!");
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    dispose(master);
                                                }
                                            }.runTaskLater(master.plugin, 40L);
                                        } else {
                                            master.broadcastSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1, 0.5f);
                                            master.broadcastMessage("" + ChatColor.RED + ChatColor.BOLD + "Please try again...");
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    reset();
                                                    id = UUID.randomUUID().toString();
                                                }
                                            }.runTaskLater(master.plugin, 40L);
                                        }
                                    }
                                }.runTaskLater(master.plugin, 70L);
                            }
                        }.runTaskLater(master.plugin, 10L);
                    } catch(NumberFormatException ex) {
                        return;
                    }

                    event.setCancelled(true);
                }
            });
        } else if(getTimestepsRemaining() == 89) {
            master.broadcastSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            master.broadcastMessage("");
            for(int i = 0; i < answers.length; i++) {
                String answer = answers[i];
                ComponentBuilder answerBuilder;
                answerBuilder = new ComponentBuilder(ChatColor.GOLD + "[    " + ChatColor.GREEN + answer + ChatColor.GOLD + "    ]");

                answerBuilder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "MULT_CHOICE_ANSWER " + id + " " + i));
                answerBuilder.event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Choose answer: " + answer)));

                master.broadcastMessage(answerBuilder.create());
                master.broadcastMessage("");
            }

            master.broadcastMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Answer by clicking on one of the responses above.");
        } else if(getTimestepsRemaining() == 0) {
            dispose(master);
        }
    }

    @Override
    public void dispose(GameMaster master) {
        super.dispose(master);
        master.setEventHandler(null);
    }
}