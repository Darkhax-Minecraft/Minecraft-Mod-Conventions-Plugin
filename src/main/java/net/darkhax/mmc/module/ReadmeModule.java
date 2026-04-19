package net.darkhax.mmc.module;

import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ReadmeModule {

    public static void updateReadme(Project rootProject, GameTarget target, BuildConfig config) {
        final File readme = rootProject.file("README.md");
        if (!readme.exists()) {
            throw new GradleException("The README.md file is missing!");
        }
        try {
            String text = Files.readString(readme.toPath());
            text = updateSection(text, "name", buildName(rootProject, target, config));
            text = updateSection(text, "description", buildIntro(config));
            text = updateSection(text, "maven", buildMavenInfo(rootProject, target, config));
            text = updateSection(text, "sponsor", buildSponsors(config));
            Files.writeString(readme.toPath(), text, StandardCharsets.UTF_8);

        }
        catch (Exception e) {
            throw new RuntimeException("Failed to update README.md", e);
        }
    }

    private static String buildName(Project rootProject, GameTarget target, BuildConfig config) {
        String encodedPath = rootProject.getGroup().toString().replace(".", "%2F");
        String projectPath = rootProject.getGroup().toString().replace(".", "/");

        String curseBadge = String.format(
                "[![CurseForge Project](https://img.shields.io/curseforge/dt/%s?logo=curseforge&label=CurseForge&style=flat-square&labelColor=2D2D2D&color=555555)](%s)",
                config.curseforge().id(),
                config.cursePage()
        );

        String modrinthBadge = String.format(
                "[![Modrinth Project](https://img.shields.io/modrinth/dt/%s?logo=modrinth&label=Modrinth&style=flat-square&labelColor=2D2D2D&color=555555)](%s)",
                config.modrinth().id(),
                config.modrinthPage()
        );

        String versionBadge = String.format(
                "[![Maven Project](https://img.shields.io/maven-metadata/v?style=flat-square&logoColor=D31A38&labelColor=2D2D2D&color=555555&label=Latest&logo=gradle&metadataUrl=https%%3A%%2F%%2Fmaven.blamejared.com%%2F%s%%2F%s-common-%s%%2Fmaven-metadata.xml)](https://maven.blamejared.com/%s)",
                encodedPath, config.mod().id(), target.gameVersion(), projectPath
        );

        return "# " + config.mod().name() + " " + curseBadge + " " + modrinthBadge + " " + versionBadge;
    }

    private static String buildIntro(BuildConfig config) {
        return "This is the official GitHub repo for the %s mod. %s You can download this mod from [CurseForge](%s) or [Modrinth](%s). Please report issues [here](%s).".formatted(
                config.mod().name(), config.mod().description(), config.cursePage(), config.modrinthPage(), config.issuesPage()
        );
    }

    private static String buildMavenInfo(Project project, GameTarget target, BuildConfig config) {
        final String group = project.getGroup().toString();
        final String version = project.getVersion().toString();
        final String modId = config.mod().id();
        final String mcVersion = target.gameVersion();

        return """
               ## Maven Dependency
               This project is available on the [BlameJared Maven](https://maven.blamejared.com).
               
               If you are using [Gradle](https://gradle.org) you can add the mod as a dependency by adding the following
               to your `build.gradle` file.
               
               ```groovy
               repositories {
                   maven {
                       url 'https://maven.blamejared.com'
                   }
               }
               
               dependencies {
                    // NeoForge
                    implementation group: '%1$s', name: '%2$s-neoforge-%3$s', version: '%4$s'
                    // Fabric
                    implementation group: '%1$s', name: '%2$s-fabric-%3$s', version: '%4$s'
                    // Common / MultiLoader / Vanilla / No Loader
                    implementation group: '%1$s', name: '%2$s-common-%3$s', version: '%4$s'
               }
               ```
               """.formatted(group, modId, mcVersion, version);
    }

    private static String buildSponsors(BuildConfig config) {
        return """
               ## Sponsors
               [![](https://assets.blamejared.com/nodecraft/darkhax.jpg)](https://nodecraft.com/r/darkhax)    
               %s is proudly sponsored by Nodecraft! Play your favorite games with your friends using their high
               performance game servers! Use code **[DARKHAX](https://nodecraft.com/r/darkhax)** for 30%% off your first
               month of service!
               """.formatted(config.mod().name());
    }

    private static String updateSection(String input, String region, String text) {
        final String startComment = commentOf(region + "-start");
        final String endComment = commentOf(region + "-end");
        final int start = input.indexOf(startComment) + startComment.length();
        final int end = input.indexOf(endComment);
        return input.substring(0, start) + System.lineSeparator() + text + System.lineSeparator() + input.substring(end);
    }

    private static String commentOf(String text) {
        return "<!-- " + text + " -->";
    }
}