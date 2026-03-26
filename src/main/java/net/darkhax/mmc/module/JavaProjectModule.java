package net.darkhax.mmc.module;

import groovy.json.JsonGenerator;
import groovy.json.JsonSlurper;
import net.darkhax.mmc.Util;
import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import net.darkhax.mmc.config.Platform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static net.darkhax.mmc.Util.addExclusiveRepo;
import static net.darkhax.mmc.Util.addMavenRepo;
import static net.darkhax.mmc.Util.configureMavenPublishing;
import static net.darkhax.mmc.Util.renameFile;
import static net.darkhax.mmc.Util.replaceTokens;
import static net.darkhax.mmc.Util.setManifest;

public class JavaProjectModule {

    public static void setupSubProject(Project project, BuildConfig config, GameTarget target, Platform platform) {

        final String authors = String.join(",", config.mod().authors());
        final String cursePage = "https://www.curseforge.com/minecraft/mc-mods/" + config.curseforge().slug();
        final String modrinthPage = "https://modrinth.com/mod/" + config.modrinth().slug();

        // Project Properties
        project.setGroup(config.mod().group());
        project.setVersion(target.adjustedGameVersion() + "." + Util.buildNumber());

        // Plugins
        final PluginContainer plugins = project.getPlugins();
        plugins.apply("java-library");
        plugins.apply("maven-publish");

        // Repositories
        final RepositoryHandler repositories = project.getRepositories();
        repositories.mavenCentral();
        addExclusiveRepo(repositories, "Sponge", "https://repo.spongepowered.org/repository/maven-public", "org.spongepowered");
        addExclusiveRepo(repositories, "ParchmentMC", "https://maven.parchmentmc.org/", "org.parchmentmc.data");
        addExclusiveRepo(repositories, "CurseMaven", "https://cursemaven.com", "curse.maven");
        addMavenRepo(repositories, "BlameJared", "https://maven.blamejared.com");

        // Base Extension
        final BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
        base.getArchivesName().set(config.mod().id() + "-" + project.getName() + "-" + target.gameVersion());

        // Java Extension
        final JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(target.javaVersion()));
        java.withJavadocJar();
        java.withSourcesJar();
        project.getTasks().withType(Javadoc.class).configureEach(task -> ((StandardJavadocDocletOptions) task.getOptions()).addStringOption("Xdoclint:none", "-quiet"));

        // Rename license file
        final File licenseFile = project.getRootProject().file("LICENSE");
        renameFile(project, "jar", licenseFile, name -> "license_" + config.mod().id() + ".txt");
        renameFile(project, "sourcesJar", licenseFile, name -> "license_" + config.mod().id() + ".txt");

        // Manifest File
        setManifest(project, Map.of(
                "Specification-Title", config.mod().name(),
                "Specification-Vendor", authors,
                "Specification-Version", project.getVersion(),
                "Implementation-Title", project.getName(),
                "Implementation-Version", project.getVersion(),
                "Implementation-Vendor", authors,
                "Built-On-Minecraft", target.gameVersion(),
                "CurseForge", cursePage,
                "Modrinth", modrinthPage
        ));

        replaceTokens(project, Util.make(new HashMap<>(), properties -> {
            properties.put("version", project.getVersion());
            properties.put("group", project.getGroup());
            properties.put("platform", project.getName());
            properties.put("minecraft_version", target.gameVersion());
            properties.put("mod_name", config.mod().name());
            properties.put("mod_author", String.join(",", config.mod().authors()));
            properties.put("mod_id", config.mod().id());
            properties.put("mod_repo", config.mod().repo());
            properties.put("mod_license", config.mod().license());
            properties.put("mod_description", config.mod().description());
            properties.put("neoforge_version", target.neoforge());
            properties.put("fabric_version", target.fabric());
            properties.put("fabric_loader_version", target.fabricLoader());
            properties.put("java_version", target.javaVersion());
            properties.put("curse_project", config.curseforge().id());
            properties.put("curse_page", "https://www.curseforge.com/minecraft/mc-mods/" + config.curseforge().slug());
            properties.put("modrinth_project", config.mod().id());
            properties.put("modrinth_page", "https://modrinth.com/mod/" + config.modrinth().slug());
            properties.put(PatreonModule.PATRONS, PatreonModule.getPatrons(project));
            if (platform == Platform.FABRIC) {
                properties.put("mod_target_environment", config.mod().client() ? "client" : "*");
            }
        }), "pack.mcmeta", "fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "*.mixins.json");

        // Maven Config
        configureMavenPublishing(project);

        // Reduce file size by minifying files with JSON data.
        minifyJsonData(project, "**/*.json", "**/*.mcmeta");
    }

    private static void minifyJsonData(Project project, String... files) {
        project.getTasks().named("processResources", Copy.class).configure(task -> task.doLast(t -> {
            long start = System.currentTimeMillis();
            int filesMinified = 0;
            long bytesSaved = 0;

            final JsonGenerator generator = new JsonGenerator.Options().disableUnicodeEscaping().build();
            final JsonSlurper slurper = new JsonSlurper();

            for (File file : project.fileTree(task.getOutputs().getFiles().getAsPath(), spec -> spec.include(files))) {
                try {
                    final long oldLength = file.length();
                    Files.writeString(file.toPath(), generator.toJson(slurper.parse(file)), StandardCharsets.UTF_8);
                    bytesSaved += (oldLength - file.length());
                    filesMinified++;
                }
                catch (Exception e) {
                    project.getLogger().error("Failed to minify file '{}'.", file.getPath());
                    throw new RuntimeException(e);
                }
            }
            project.getLogger().lifecycle("Minified {} files. Saved {} bytes before compression. Took {}ms.", filesMinified, bytesSaved, System.currentTimeMillis() - start);
        }));
    }
}
