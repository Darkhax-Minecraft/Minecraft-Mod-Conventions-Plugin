package net.darkhax.mmc.config;

import java.util.Map;

public record Dependency(String name, DependencyType type, HostedProject curseforge, HostedProject modrinth, Map<Platform, String> maven) {
}