import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin {

    @Override
    public void onEnable() {
        //register the command
        getCommand("boston").setExecutor(new CommandHandler(this));
    }

    @Override
    public void onDisable() {

    }
}
