package net.darkhax.mmc.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * This module loads secrets from a JSON file. This file is typically encrypted and can not be read outside the CI
 * container.
 * <p>
 * The path to the file must be specified using secretFileV2 or secretFile, with V2 being preferred when available.
 * <p>
 * The JSON file must be structured as a map of strings. Values will be directly injected into extra properties using
 * the JSON key as the property name. Keys that start with _ or end with _comment will be excluded.
 */
public class SecretLoader {

    private static final Logger LOGGER = Logging.getLogger("Secrets");
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final Gson GSON = new GsonBuilder().create();

    public static void loadSecrets(Project rootProject) {
        final String secretFilePath = findSecretFilePath(rootProject);
        if (secretFilePath == null) {
            LOGGER.warn("A secret file has not been defined. Use secretFileV2 or secretFile to set the path.");
            return;
        }
        final File file = rootProject.file(secretFilePath);
        if (!file.exists()) {
            LOGGER.error("A secret file path was defined, but the file does not exist!");
            return;
        }
        if (!file.canRead()) {
            LOGGER.error("A secret file was defined, but Gradle can not read it!");
            return;
        }
        final Map<String, Object> secrets = readSecrets(file);
        if (secrets == null) {
            return;
        }
        final int count = applySecrets(rootProject, secrets);
        if (count > 0) {
            LOGGER.lifecycle("Loaded {} secret properties!", count);
        }
        else {
            LOGGER.warn("Secret file was loaded, but contained no properties?");
        }
    }

    private static String findSecretFilePath(Project rootProject) {
        for (String key : new String[]{"secretFileV2", "secretFile"}) {
            if (rootProject.findProperty(key) instanceof String path) {
                return path;
            }
        }
        return null;
    }

    private static Map<String, Object> readSecrets(File file) {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, MAP_TYPE);
        }
        catch (Exception e) {
            LOGGER.error("Failed to read secrets from the configured secret file!", e);
            return null;
        }
    }

    private static int applySecrets(Project project, Map<String, Object> map) {
        final ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties();
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final String key = entry.getKey();
            if (isValidSecretKey(key)) {
                ext.set(key, entry.getValue());
                count++;
            }
        }
        return count;
    }

    private static boolean isValidSecretKey(String key) {
        return !key.startsWith("_") && !key.endsWith("_comment");
    }
}