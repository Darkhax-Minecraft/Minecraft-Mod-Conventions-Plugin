package net.darkhax.mmc.module;

import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import net.darkhax.mmc.config.Platform;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.tasks.SourceSetContainer;

import static net.darkhax.mmc.Util.validateSubproject;

public class FabricModule {

    public static void setupFabric(Project rootProject, BuildConfig config, GameTarget game) {
        final Project fabricProject = validateSubproject(rootProject, "fabric");
        ValidationModule.validateMixinConfig(fabricProject, config.mod().id());

        // Apply defaults for Java projects.
        JavaProjectModule.setupSubProject(fabricProject, config, game, Platform.FABRIC);
        CommonModule.loadCommon(fabricProject);
        fabricProject.getPluginManager().apply("net.fabricmc.fabric-loom");
        final LoomGradleExtensionAPI loom = fabricProject.getExtensions().getByType(LoomGradleExtensionAPI.class);

        // Dependencies
        final DependencyHandler dependencies = fabricProject.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + game.gameVersion());
        dependencies.add("implementation", "net.fabricmc:fabric-loader:" + game.fabricLoader());
        dependencies.add("implementation", "net.fabricmc.fabric-api:fabric-api:" + game.fabric());
        config.applyDependencies(fabricProject, Platform.FABRIC);

        // Loom configuration
        final RunConfigSettings clientRun = loom.getRuns().maybeCreate("client");
        clientRun.client();
        clientRun.setName("Fabric Client");
        clientRun.ideConfigGenerated(true);
        clientRun.runDir("runs/client");

        final RunConfigSettings serverRun = loom.getRuns().maybeCreate("server");
        serverRun.server();
        serverRun.setConfigName("Fabric Server");
        serverRun.ideConfigGenerated(true);
        serverRun.runDir("runs/server");

        // Attribute setup
        final Attribute<String> loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String.class);
        for (String variant : new String[]{"apiElements", "runtimeElements", "sourcesElements", "javadocElements", "includeInternal", "modCompileClasspath"}) {
            final Configuration conf = fabricProject.getConfigurations().getByName(variant);
            conf.attributes(attrs -> attrs.attribute(loaderAttribute, "fabric"));
        }

        // Source set classpath configurations
        final SourceSetContainer sourceSets = fabricProject.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.configureEach(sourceSet -> {
            fabricProject.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).attributes(attrs -> attrs.attribute(loaderAttribute, "fabric"));
            fabricProject.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).attributes(attrs -> attrs.attribute(loaderAttribute, "fabric"));
        });

        // Publishing
        CurseForgeModule.setupCurseForge(rootProject, fabricProject, config, Platform.FABRIC);
        ModrinthModule.setupModrinth(rootProject, fabricProject, game, config, Platform.FABRIC);
    }
}