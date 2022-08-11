package narrative.narration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import master.GameMaster;
import net.md_5.bungee.api.ChatColor;

public class NarrationEndCard extends Narration {

    class NameScore {
        public final String name;
        public final int score;

        public NameScore(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public NameScore(String data) {
            String[] split = data.split(" ");
            this.name = split[0];
            this.score = Integer.parseInt(split[1]);
        }

        public String save() {
            return name + " " + score;
        }
    }

    public NarrationEndCard() {
        super(10);
    }

    public int submitScore(GameMaster master, String name, int score) {
        File pluginsFile = new File(master.plugin.getDataFolder(), "scores.data");
        LinkedList<NameScore> nameScores = new LinkedList<>();

        if(score <= 0) {
            return -1;
        }

        //load from file
        try {
            if(!pluginsFile.exists()) {
                master.plugin.getDataFolder().mkdir();
                pluginsFile.createNewFile();
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(pluginsFile));
                int i = 0;
                String read;
                while((read = reader.readLine()) != null && i < 3) {
                    nameScores.add(new NameScore(read));

                    i++;
                }

                reader.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        int finalPlace = -1;
        if(nameScores.isEmpty()) {
            finalPlace = 1;
            nameScores.add(new NameScore(name, score));
        } else {
            for(int i = 0; i < Math.min(3, nameScores.size()); i++) {
                if(score > nameScores.get(i).score) {
                    finalPlace = i + 1;
                    nameScores.add(i, new NameScore(name, score));
                    break;
                }
            }

            if(finalPlace == -1 && nameScores.size() < 3) {
                nameScores.add(new NameScore(name, score));
                finalPlace = nameScores.size();
            }
        }

        while(nameScores.size() > 3) {
            nameScores.pollLast();
        }

        //save to file
        try {
            FileWriter writer = new FileWriter(pluginsFile);
            
            for(NameScore nameScore : nameScores) {
                writer.append(nameScore.save());
                writer.append("\n");
            }

            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return finalPlace;
    }

    public List<NameScore> getScoreboard(GameMaster master) {
        List<NameScore> nameScores = new ArrayList<>();

        File pluginsFile = new File(master.plugin.getDataFolder(), "scores.data");

        //load from file
        try {
            if(!pluginsFile.exists()) {
                return nameScores;
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(pluginsFile));
                int i = 0;
                String read;
                while((read = reader.readLine()) != null && i < 3) {
                    nameScores.add(new NameScore(read));

                    i++;
                }

                reader.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return nameScores;
    }

    @Override
    public void processTimestepInternal(GameMaster master) {
        if(isFirstTimestep()) {
            int finalScore = master.getTotalScore();
            int finalPlace = submitScore(master, master.getPlayerName(), finalScore);
            for(Player p : master.getPlayers()) {
                switch(finalPlace) {
                    case 1:
                        p.sendTitle("" + ChatColor.GOLD + ChatColor.BOLD + "BOSTON: FIN - " + ChatColor.GOLD + ChatColor.BOLD + "1ST PLACE!", ChatColor.WHITE + "You win! Your final score is " + ChatColor.GREEN + finalScore, 10, 200, 20);
                        break;
                    case 2:
                        p.sendTitle("" + ChatColor.GOLD + ChatColor.BOLD + "BOSTON: FIN - " + ChatColor.YELLOW + ChatColor.BOLD + "2ND PLACE!", ChatColor.WHITE + "You win! Your final score is " + ChatColor.GREEN + finalScore, 10, 200, 20);
                        break;
                    case 3:
                        p.sendTitle("" + ChatColor.GOLD + ChatColor.BOLD + "BOSTON: FIN - " + ChatColor.BLUE + ChatColor.BOLD + "3RD PLACE!", ChatColor.WHITE + "You win! Your final score is " + ChatColor.GREEN + finalScore, 10, 200, 20);
                        break;
                    default:
                        p.sendTitle("" + ChatColor.GOLD + ChatColor.BOLD + "BOSTON: FIN", ChatColor.WHITE + "You win! Your final score is " + ChatColor.GREEN + finalScore, 10, 200, 20);
                        break;
                }
            }
            master.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 0.5f);
            master.broadcastMessage("");
            master.broadcastMessage(ChatColor.GOLD + "Congratulations!");
            master.broadcastMessage("");
            master.broadcastMessage(ChatColor.WHITE + "Your final score is " + ChatColor.GREEN + finalScore + ChatColor.WHITE + ".");
            switch(finalPlace) {
                case 1:
                    master.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 2, 2);
                    master.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "You are now 1st place!");
                    break;
                case 2:
                    master.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 2, 1.75f);
                    master.broadcastMessage("" + ChatColor.YELLOW + ChatColor.BOLD + "You are now 2nd place!");
                    break;
                case 3:
                    master.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5f);
                    master.broadcastMessage("" + ChatColor.BLUE + ChatColor.BOLD + "You are now 3rd place!");
                    break;
            }
        } else if(getTimestepsRemaining() == 5) {
            List<NameScore> scores = getScoreboard(master);

            if(scores.isEmpty()) {
                return;
            }

            master.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 2f);

            master.broadcastMessage("");
            master.broadcastMessage("");
            master.broadcastMessage("");
            master.broadcastMessage("" + ChatColor.AQUA + ChatColor.BOLD + "SCOREBOARD");
            for(int i = 0; i < Math.min(3, scores.size()); i++) {
                NameScore ns = scores.get(i);
                switch(i) {
                    case 0:
                        master.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "1ST PLACE - " + ChatColor.GOLD + ns.name);
                        break;
                    case 1:
                        master.broadcastMessage("" + ChatColor.YELLOW + ChatColor.BOLD + "2ND PLACE - " + ChatColor.YELLOW + ns.name);
                        break;
                    case 2:
                        master.broadcastMessage("" + ChatColor.BLUE + ChatColor.BOLD + "3RD PLACE - " + ChatColor.BLUE + ns.name);
                        break;
                }
                master.broadcastMessage("" + ChatColor.WHITE + ns.score + " point" + (ns.score == 1 ? "" : "s"));
                master.broadcastMessage("");
            }
        }
    }
    
}
