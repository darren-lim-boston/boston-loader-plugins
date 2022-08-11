import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import master.GameMaster;

public class MainClass extends JavaPlugin {

    public GameMaster master;

    @Override
    public void onEnable() {
        //register the EventListener class to be used in the plugin
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getPluginManager().registerEvents(master = new GameMaster(this), this);

        //load other worlds
        getServer().createWorld(new WorldCreator("world-test"));
        getServer().createWorld(new WorldCreator("world-city"));
        getServer().createWorld(new WorldCreator("world-game"));

        //register the command
        getCommand("simulation").setExecutor(new CommandHandler(this));
        getCommand("game").setExecutor(new CommandHandler(this));
    }

    @Override
    public void onDisable() {
        master.forceEndGame();
    }
}
