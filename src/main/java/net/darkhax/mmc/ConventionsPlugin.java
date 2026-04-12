package net.darkhax.mmc;

import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import net.darkhax.mmc.module.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;

public class ConventionsPlugin implements Plugin<Project> {

    public static final Logger LOGGER = Logging.getLogger("MCC");

    private final ExecOperations execOperations;

    @Inject
    public ConventionsPlugin(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @Override
    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new IllegalStateException("The conventions plugin must only be applied to the root plugin.");
        }
        final File buildConfigFile = project.file("build-config.json");
        if (!buildConfigFile.exists() || !buildConfigFile.canRead()) {
            throw new IllegalStateException("Unable to read build-config.json! This is required!");
        }
        final BuildConfig buildConfig = Util.read(buildConfigFile, BuildConfig.class);
        ValidationModule.validate(project, buildConfig);
        final GameTarget gameTarget = buildConfig.isCompatabilityEnabled() ? GameTarget.BUILTIN : GameTarget.BUILTIN.strictGameTarget();
        if (buildConfig.isCompatabilityEnabled()) {
            LOGGER.lifecycle("Compatibility mode is enabled for this build. Mod will try to target MC {}, Fabric {}, and NeoForge {}", gameTarget.minGameVersion(), gameTarget.minFabricLoader() + " " + gameTarget.minFabric(), gameTarget.minNeoforge());
        }
        // Project Properties
        project.setGroup(buildConfig.mod().group());
        project.setVersion(gameTarget.adjustedGameVersion() + "." + Util.buildNumber());
        LOGGER.lifecycle("Setting up {} v{} for Minecraft {}!", buildConfig.mod().name(), project.getVersion(), gameTarget.gameVersion());
        SecretLoader.loadSecrets(project);
        PatreonModule.setup(project);
        ChangelogModule.setupChangelog(project, buildConfig, execOperations, gameTarget);
        CommonModule.setupCommon(project, buildConfig, gameTarget);
        NeoforgeModule.setupNeoforge(project, buildConfig, gameTarget);
        FabricModule.setupFabric(project, buildConfig, gameTarget);
        VersionTrackerModule.setupVersionTracker(project, buildConfig, gameTarget);
        ReadmeModule.updateReadme(project, gameTarget, buildConfig);

//        // Make Gradlew executable
//        final File gradleWrapper = project.file("gradlew");
//        if (!Util.isGitExecutable(gradleWrapper)) {
//            Util.makeGitExecutable(gradleWrapper);
//            LOGGER.lifecycle("Gradle wrapper has been marked as executable! Make sure to commit the changes :)");
//        }
    }
}