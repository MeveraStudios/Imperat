package studio.mevera.imperat.examples;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.type.ParameterPlayer;

class ExamplePlugin extends JavaPlugin {

    

    private BukkitImperat imperat;



    @Override

    public void onEnable() {



        //setting up our imperat

        imperat = BukkitImperat.builder(this)

                .parameterType(Player.class, new ParameterPlayer())

                .build();

        

        //registering rank command.

        imperat.registerCommand(new MsgCommand());

    }

}