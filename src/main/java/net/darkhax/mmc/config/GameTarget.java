package net.darkhax.mmc.config;

import net.darkhax.mmc.Util;

import java.util.Set;

public record GameTarget(int javaVersion, String gameVersion, String minGameVersion, String neoform, String neoforge, String minNeoforge, String fabric, String minFabric, String fabricLoader, String minFabricLoader, Set<String> compatibleWith) {

    public static final GameTarget BUILTIN = new GameTarget(25, "26.2", "26.2", "26.2-1", "26.2.0.8-beta", "26.2.0.8-beta", "0.154.2+26.2", "0.154.2+26.2", "0.19.3", "0.19.3", Set.of());

    public String adjustedGameVersion() {
        return Util.countChar(this.gameVersion, '.') == 1 ? this.gameVersion + ".0" : this.gameVersion;
    }

    public GameTarget strictGameTarget() {
        return new GameTarget(javaVersion, gameVersion, gameVersion, neoform, neoforge, neoforge, fabric, fabric, fabricLoader, fabricLoader, Set.of());
    }
}