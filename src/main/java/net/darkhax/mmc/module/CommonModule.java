package net.darkhax.mmc.module;

import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import net.darkhax.mmc.config.Platform;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.bundling.Jar;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static net.darkhax.mmc.Util.createConfiguration;
import static net.darkhax.mmc.Util.validateSubproject;

public class CommonModule {

    public static void setupCommon(Project rootProject, BuildConfig config, GameTarget game) {
        final Project common = validateSubproject(rootProject, "common");
        validateLogo(common, config.mod().id());
        ValidationModule.validateMixinConfig(common, config.mod().id());

        // Apply defaults for Java projects.
        JavaProjectModule.setupSubProject(common, config, game, Platform.COMMON);

        // Setup NeoForge
        common.getPlugins().apply("net.neoforged.moddev");
        final NeoForgeExtension neoforge = common.getExtensions().getByType(NeoForgeExtension.class);
        neoforge.setNeoFormVersion(game.neoform());

        // Dependencies
        final DependencyHandler dependencies = common.getDependencies();
        dependencies.add("compileOnly", "org.spongepowered:mixin:0.8.5");
        dependencies.add("compileOnly", "io.github.llamalad7:mixinextras-common:0.3.5");
        dependencies.add("annotationProcessor", "io.github.llamalad7:mixinextras-common:0.3.5");
        config.applyDependencies(common, Platform.COMMON);

        // Configurations
        final SourceSetContainer sourceSets = common.getExtensions().getByType(SourceSetContainer.class);
        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        createConfiguration(common, "commonJava");
        common.getArtifacts().add("commonJava", main.getJava().getSourceDirectories().getSingleFile());
        createConfiguration(common, "commonResources");
        common.getArtifacts().add("commonResources", main.getResources().getSourceDirectories().getSingleFile());

        // Loader Attributes https://github.com/mcgradleconventions
        final Attribute<String> loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String.class);
        for (String variant : new String[]{"apiElements", "runtimeElements", "sourcesElements", "javadocElements"}) {
            common.getConfigurations().named(variant, cfg -> cfg.getAttributes().attribute(loaderAttribute, "common"));
        }
        sourceSets.configureEach(sourceSet -> {
            for (String variant : new String[]{sourceSet.getCompileClasspathConfigurationName(), sourceSet.getRuntimeClasspathConfigurationName()}) {
                common.getConfigurations().named(variant, cfg -> cfg.getAttributes().attribute(loaderAttribute, "common"));
            }
        });
    }

    private static void validateLogo(Project common, String modId) {
        final File logoFile = common.file("src/main/resources/logo_" + modId + ".png");
        if (!logoFile.exists()) {
            throw new GradleException("A logo file is required to build the mod! Expected: " + logoFile.getPath());
        }
        if (!logoFile.canRead()) {
            throw new GradleException("The logo file exists, but it can not be read!");
        }
        try {
            final BufferedImage logo = ImageIO.read(logoFile);
            if (logo.getWidth() != logo.getHeight()) {
                throw new GradleException("Logo files must be a 1:1 aspect ratio. width=" + logo.getWidth() + " height=" + logo.getHeight());
            }
        }
        catch (IOException e) {
            throw new GradleException("Encountered a critical exception when validating logo file.", e);
        }
    }

    public static void loadCommon(Project project) {
        final Attribute<String> loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String.class);

        // Configurations
        final Configuration commonJava = project.getConfigurations().create("commonJava", cfg -> cfg.setCanBeResolved(true));
        final Configuration commonResources = project.getConfigurations().create("commonResources", cfg -> cfg.setCanBeResolved(true));

        // Deps
        final DependencyHandler dependencies = project.getDependencies();
        final ModuleDependency commonDep = (ModuleDependency) dependencies.add("compileOnly", project.project(":common"));
        commonDep.attributes(attributes -> attributes.attribute(loaderAttribute, "common"));
        dependencies.add("commonJava", dependencies.project(Map.of("path", ":common", "configuration", "commonJava")));
        dependencies.add("commonResources", dependencies.project(Map.of("path", ":common", "configuration", "commonResources")));

        // Task: compileJava
        project.getTasks().withType(JavaCompile.class).named("compileJava", task -> {
            task.dependsOn(commonJava);
            task.source(commonJava);
        });

        // Task: processResources
        project.getTasks().withType(Copy.class).named("processResources", task -> {
            task.dependsOn(commonResources);
            task.from(commonResources);
        });

        // Task: javadoc
        project.getTasks().withType(Javadoc.class).named("javadoc", task -> {
            task.dependsOn(commonJava);
            task.setSource(commonJava);
        });

        // Task: sourcesJar
        project.getTasks().withType(Jar.class).named("sourcesJar", task -> {
            task.dependsOn(commonJava);
            task.from(commonJava);
            task.dependsOn(commonResources);
            task.from(commonResources);
        });
    }
}