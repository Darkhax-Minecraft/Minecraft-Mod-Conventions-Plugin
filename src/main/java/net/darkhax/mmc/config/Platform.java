package net.darkhax.mmc.config;

import com.google.gson.annotations.SerializedName;

public enum Platform {

    @SerializedName("common")
    COMMON,

    @SerializedName("neoforge")
    NEOFORGE,

    @SerializedName("fabric")
    FABRIC;
}