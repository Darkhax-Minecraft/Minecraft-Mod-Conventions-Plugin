package net.darkhax.mmc.module;

import groovy.json.JsonOutput;
import net.darkhax.mmc.ConventionsPlugin;
import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import org.gradle.api.Project;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class VersionTrackerModule {

    public static void setupVersionTracker(Project root, BuildConfig config, GameTarget target) {
        final String apiUrl = prop(root, "versionTrackerAPI");
        final String username = prop(root, "versionTrackerUsername");
        final String apiKey = prop(root, "versionTrackerKey");
        if (apiUrl != null && username != null && apiKey != null) {
            root.getTasks().register("updateVersionTracker", task -> {
                task.doLast(_ -> {
                    try {
                        final String json = JsonOutput.toJson(Map.of(
                                "author", username,
                                "projectName", config.mod().id(),
                                "gameVersion", target.gameVersion(),
                                "projectVersion", root.getVersion().toString(),
                                "homepage", "https://www.curseforge.com/minecraft/mc-mods/" + config.curseforge().slug(),
                                "uid", apiKey
                        ));
                        final HttpURLConnection conn = (HttpURLConnection) new URI(apiUrl).toURL().openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setRequestProperty("User-Agent", config.mod().name() + " Tracker Gradle");
                        conn.setDoOutput(true);
                        try (final OutputStream os = conn.getOutputStream()) {
                            os.write(json.getBytes(StandardCharsets.UTF_8));
                        }
                        final int code = conn.getResponseCode();
                        final String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                        ConventionsPlugin.LOGGER.lifecycle("Version Check: Status {}", code);
                        ConventionsPlugin.LOGGER.lifecycle("Version Check: Response {}", response);
                    }
                    catch (Exception e) {
                        ConventionsPlugin.LOGGER.error("Version tracker update failed! {}", e.getMessage());
                    }
                });
            });
        }
    }

    private static String prop(Project project, String name) {
        final Object val = project.findProperty(name);
        return val != null ? val.toString() : null;
    }
}