package net.darkhax.mmc.config;

import java.util.Map;

public record Dependency(String name, DependencyType type, HostedProject curseforge, HostedProject modrinth, Map<Platform, String> maven, ModId mod_id) {
    public String getModId(Platform platform) {
        if (mod_id != null) {
            return mod_id.getModId(platform);
        }
        return null;
    }
}