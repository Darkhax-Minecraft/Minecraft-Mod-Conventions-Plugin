package net.darkhax.mmc.module;

import net.darkhax.mmc.config.BuildConfig;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.File;

public class ValidationModule {

    public static void validate(Project rootProject, BuildConfig config) {
        validateLicense(rootProject);
    }

    private static void validateLicense(Project rootProject) {
        if (!rootProject.file("LICENSE").exists()) {
            throw new GradleException("LICENSE file does not exist!");
        }
    }

    public static void validateMixinConfig(Project project, String modId) {
        final File mixinConfig = project.file("src/main/resources/" + modId + "." + project.getName() + ".mixins.json");
        if (!mixinConfig.exists()) {
            throw new GradleException("Could not find mixin config for ':" + project.getName() + "' " + mixinConfig.getPath());
        }
    }
}
