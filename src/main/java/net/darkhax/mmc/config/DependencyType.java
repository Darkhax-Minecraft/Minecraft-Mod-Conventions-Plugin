package net.darkhax.mmc.config;

import com.google.gson.annotations.SerializedName;

public enum DependencyType {
    /**
     * The dependency will be marked as required when uploading to platforms like CurseForge and Modrinth.
     */
    @SerializedName("required")
    REQUIRED,

    /**
     * The dependency will be marked as optional when uploading to platforms like CurseForge and Modrinth.
     */
    @SerializedName("optional")
    OPTIONAL,

    /**
     * The dependency is pulled into the dev environment, but will be ignored when uploading to platforms like
     * Curseforge and Modrinth.
     */
    @SerializedName("misc")
    MISC
}
