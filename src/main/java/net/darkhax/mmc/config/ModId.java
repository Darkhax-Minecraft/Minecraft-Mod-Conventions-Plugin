package net.darkhax.mmc.config;

public record ModId(String id, String fabric, String neoforge) {
    String getModId(Platform platform) {
        if (platform == Platform.FABRIC && fabric != null) {
            return fabric;
        }
        else if (platform == Platform.NEOFORGE && neoforge != null) {
            return neoforge;
        }
        return id;
    }
}