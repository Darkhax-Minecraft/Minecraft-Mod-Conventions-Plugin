package net.darkhax.mmc.module;

import net.darkhax.mmc.Util;
import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import net.darkhax.mmc.config.Platform;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import static net.darkhax.mmc.Util.validateSubproject;

public class NeoforgeModule {

    public static void setupNeoforge(Project rootProject, BuildConfig config, GameTarget game) {
        final Project neoProject = validateSubproject(rootProject, "neoforge");
        ValidationModule.validateMixinConfig(neoProject, config.mod().id());

        // Apply defaults for Java projects.
        JavaProjectModule.setupSubProject(neoProject, config, game, Platform.NEOFORGE);

        // Setup Neoforge
        CommonModule.loadCommon(neoProject);
        neoProject.getPluginManager().apply("net.neoforged.moddev");
        final NeoForgeExtension neoforge = neoProject.getExtensions().getByType(NeoForgeExtension.class);
        neoforge.setVersion(game.neoforge());

        // Dependencies
        config.applyDependencies(neoProject, Platform.NEOFORGE);

        // Setup Runs
        final NamedDomainObjectContainer<RunModel> runs = neoforge.getRuns();
        runs.configureEach(run -> {
            run.systemProperty("neoforge.enabledGameTestNamespaces", config.mod().id());
            run.getIdeName().set("NeoForge " + Util.capitalize(run.getName()) + " (" + neoProject.getPath() + ")");
        });

        final RunModel client = runs.maybeCreate("client");
        client.client();

        final RunModel server = runs.maybeCreate("server");
        server.server();

        // Source Sets
        final SourceSetContainer sourceSets = neoProject.getExtensions().getByType(SourceSetContainer.class);
        neoforge.getMods().create(config.mod().id(), mod -> {
            mod.sourceSet(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
        });
        sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().srcDir("src/generated/resources");

        // Loader Attributes https://github.com/mcgradleconventions
        final Attribute<String> loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String.class);
        for (String variant : new String[]{"apiElements", "runtimeElements", "sourcesElements", "javadocElements"}) {
            neoProject.getConfigurations().named(variant, cfg -> cfg.getAttributes().attribute(loaderAttribute, "neoforge"));
        }
        sourceSets.configureEach(sourceSet -> {
            for (String variant : new String[]{sourceSet.getCompileClasspathConfigurationName(), sourceSet.getRuntimeClasspathConfigurationName(), sourceSet.getTaskName(null, "jarJar")}) {
                neoProject.getConfigurations().named(variant, cfg -> cfg.getAttributes().attribute(loaderAttribute, "neoforge"));
            }
        });
        CurseForgeModule.setupCurseForge(rootProject, neoProject, config, Platform.NEOFORGE);
        ModrinthModule.setupModrinth(rootProject, neoProject, game, config, Platform.NEOFORGE);
    }
}