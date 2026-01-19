package studio.mevera.imperat.util;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public class PlayerUtil {

    public static PlayerRef getPlayerRefByName(String name) {
        return getPlayerRefByName(name, NameMatching.DEFAULT);
    }

    public static PlayerRef getPlayerRefByName(String name, NameMatching matching) {
        return matching.find(Universe.get().getPlayers(), name, PlayerRef::getUsername);
    }

}
