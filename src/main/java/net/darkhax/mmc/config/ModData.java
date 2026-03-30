package net.darkhax.mmc.config;

public record ModData(String name, String group, String[] authors, String id, String license, String description, String repo, boolean client) {

    public String getFileDisplayName() {
        return this.name.replaceAll("[^a-zA-Z0-9]", "");
    }
}
