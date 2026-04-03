package net.darkhax.mmc.module;

import net.darkhax.mmc.config.*;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.StringJoiner;

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

        final File fabricJson = fabricProject.file("src/main/resources/fabric.mod.json");
        if (!fabricJson.exists()) {
            throw new GradleException("The fabric.mod.json file could not be found!");
        }
        try {
            Files.writeString(fabricJson.toPath(), """
                    {
                      "schemaVersion": 1,
                      "id": "%1$s",
                      "version": "%2$s",
                      "name": "%3$s",
                      "description": "%4$s",
                      "authors": [
                        "%5$s"
                      ],
                      "contributors": [
                        "This project is made possible with Patreon support from players like you. Thank you!",
                        "%6$s"
                      ],
                      "contact": {
                        "sources": "%7$s",
                        "issues": "%7$s/issues",
                        "homepage": "%8$s"
                      },
                      "license": "%9$s",
                      "icon": "logo_%1$s.png",
                      "environment": "%10$s",
                      "entrypoints": {
                        "main": [
                          "%11$s.fabric.%12$sFabric"
                        ],
                        "client": [
                          "%11$s.fabric.%12$sFabricClient"
                        ]
                      },
                      "mixins": [
                        "%1$s.common.mixins.json",
                        "%1$s.fabric.mixins.json"
                      ],
                      "depends": {
                    %13$s
                      },
                      "custom": {
                        "modmenu": {
                          "links": {
                            "modmenu.curseforge": "%8$s",
                            "modmenu.modrinth": "%14$s"
                          }
                        }
                      }
                    }
                    """.formatted(
                    config.mod().id(),
                    rootProject.getVersion().toString(),
                    config.mod().name(),
                    config.mod().description(),
                    String.join(", ", config.mod().authors()),
                    String.join(", ", PatreonModule.getPatrons(rootProject)),
                    config.mod().repo(),
                    config.cursePage(),
                    config.mod().license(),
                    config.mod().client() ? "client" : "*",
                    config.mod().group(),
                    config.mod().getFileDisplayName(),
                    buildDeps(config, game),
                    config.modrinthPage()
            ), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new GradleException("Could not write the fabric.mod.json file!", e);
        }
    }

    private static String buildDeps(BuildConfig config, GameTarget target) {
        final StringJoiner deps = new StringJoiner("," + System.lineSeparator());
        deps.add(dep("fabricloader", ">=" + target.fabricLoader()));
        deps.add(dep("fabric-api", "*"));
        deps.add(dep("minecraft", ">=" + target.gameVersion()));
        deps.add(dep("java", ">=" + target.javaVersion()));

        if (config.dependencies() != null) {
            for (Dependency dependency : config.dependencies()) {
                if (dependency.type() == DependencyType.REQUIRED) {
                    final String depName = dependency.getModId(Platform.FABRIC);
                    if (depName != null) {
                        deps.add(dep(depName, "*"));
                    }
                }
            }
        }
        return deps.toString();
    }

    private static String dep(String name, String version) {
        return "    \"" + name + "\": \"" + version + "\"";
    }
}