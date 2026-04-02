package net.darkhax.mmc.config;

import net.darkhax.mmc.Util;

public record GameTarget(int javaVersion, String gameVersion, String neoform, String neoforge, String fabric, String fabricLoader) {

    public static final GameTarget TARGET = new GameTarget(25, "26.1.1", "26.1.1-1", "26.1.1.1-beta", "0.145.3+26.1.1", "0.18.6");

    public String adjustedGameVersion() {
        return Util.countChar(this.gameVersion, '.') == 1 ? this.gameVersion + ".0" : this.gameVersion;
    }
}