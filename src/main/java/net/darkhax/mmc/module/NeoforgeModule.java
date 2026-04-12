package net.darkhax.mmc.module;

import net.darkhax.mmc.Util;
import net.darkhax.mmc.config.*;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.StringJoiner;

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
        CurseForgeModule.setupCurseForge(rootProject, neoProject, config, Platform.NEOFORGE, game);
        ModrinthModule.setupModrinth(rootProject, neoProject, game, config, Platform.NEOFORGE);

        final File modsToml = neoProject.file("src/main/resources/META-INF/neoforge.mods.toml");
        if (!modsToml.exists()) {
            throw new GradleException("The neoforge.mods.toml file does not exist!");
        }
        try {
            Files.writeString(modsToml.toPath(), """
                    modLoader = "javafml"
                    license = "%1$s"
                    issueTrackerURL = "%2$s/issues"
                    
                    [[mods]]
                    modId = "%3$s"
                    version = "%4$s"
                    displayName = "%5$s"
                    updateJSONURL = "https://updates.blamejared.com/get?n=%3$s&gv=%6$s&ml=neoforge"
                    displayURL = "%7$s"
                    logoFile = "logo_%3$s.png"
                    logoBlur = false
                    credits = "This project is made possible with Patreon support from players like you. Thank you! %9$s"
                    authors = "%8$s"
                    description = "%10$s"
                    
                    [[mixins]]
                    config = "%3$s.common.mixins.json"
                    
                    [[mixins]]
                    config = "%3$s.neoforge.mixins.json"
                    
                    %12$s
                    """.formatted(
                    config.mod().license(),
                    config.mod().repo(),
                    config.mod().id(),
                    rootProject.getVersion().toString(),
                    config.mod().name(),
                    game.gameVersion(),
                    config.cursePage(),
                    String.join(", ", config.mod().authors()),
                    String.join(", ", PatreonModule.getPatrons(rootProject)),
                    config.mod().description(),
                    game.neoforge(),
                    buildDeps(config, game)
            ), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new GradleException("Could not write the neoforge.mods.toml file!", e);
        }
    }

    private static String buildDeps(BuildConfig config, GameTarget target) {
        final StringJoiner deps = new StringJoiner(System.lineSeparator());

        deps.add(dep(config.mod().id(), "minecraft", target.minGameVersion()));
        deps.add(dep(config.mod().id(), "neoforge", "[" + target.minNeoforge() + ",)"));

        if (config.dependencies() != null) {
            for (Dependency dependency : config.dependencies()) {
                if (dependency.type() == DependencyType.REQUIRED) {
                    final String depName = dependency.getModId(Platform.NEOFORGE);
                    if (depName != null) {
                        deps.add(dep(config.mod().id(), depName, "[0,)"));
                    }
                }
            }
        }
        return deps.toString();
    }

    private static String dep(String modId, String name, String version) {
        return """
                [[dependencies.%1$s]]
                modId = "%2$s"
                type = "required"
                versionRange = "%3$s"
                ordering = "NONE"
                side = "BOTH"
                """.formatted(modId, name, version);
    }
}