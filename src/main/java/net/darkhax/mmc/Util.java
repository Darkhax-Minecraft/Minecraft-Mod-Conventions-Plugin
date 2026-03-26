package net.darkhax.mmc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class Util {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();

    private static String buildNumber;

    public static String buildNumber() {
        if (buildNumber == null) {
            // The BUILD_NUMBER property will be set by Jenkins.
            final String providedBuildNumber = System.getenv("BUILD_NUMBER");
            if (providedBuildNumber == null) {
                ConventionsPlugin.LOGGER.warn("The BUILD_NUMBER property was not defined. Defaulting to build 0.");
                buildNumber = "0";
            }
            else {
                buildNumber = providedBuildNumber;
            }
        }
        return buildNumber;
    }

    public static <T> T read(File file, Class<T> clazz) {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, clazz);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V> Map<K, V> make(Map<K, V> map, Consumer<Map<K, V>> builder) {
        builder.accept(map);
        return Collections.unmodifiableMap(map);
    }

    public static void addExclusiveRepo(RepositoryHandler repositories, String name, String url, String... groups) {
        repositories.exclusiveContent(exclusive -> {
            exclusive.forRepository(() -> repositories.maven(repo -> {
                repo.setName(name);
                repo.setUrl(url);
            }));
            exclusive.filter(filter -> {
                for (String group : groups) {
                    filter.includeGroupAndSubgroups(group);
                }
            });
        });
    }

    public static void addMavenRepo(RepositoryHandler repositories, String name, String url) {
        repositories.maven(repo -> {
            repo.setName(name);
            repo.setUrl(url);
        });
    }

    public static void renameFile(Project project, String taskName, File file, Function<String, String> nameFunc) {
        if (file.exists() && file.canRead() && file.canWrite()) {
            final Task task = project.getTasks().findByName(taskName);
            if (task instanceof AbstractCopyTask copy) {
                copy.from(file, spec -> spec.rename(nameFunc::apply));
            }
        }
    }

    public static void configureMavenPublishing(Project project) {
        project.getExtensions().getByType(PublishingExtension.class).publications(publications -> {
            publications.register("mavenJava", MavenPublication.class, mavenPub -> {
                mavenPub.setArtifactId(project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().get());
                mavenPub.from(project.getComponents().findByName("java"));
            });
        });
        project.getExtensions().getByType(PublishingExtension.class).getRepositories().maven(mavenRepo -> {
            final String localMavenUrl = System.getenv("local_maven_url");
            if (localMavenUrl != null) {
                mavenRepo.setUrl(localMavenUrl);
            }
        });
    }

    public static void setManifest(Project project, Map<String, Object> attributes) {
        project.getTasks().named("jar", Jar.class).configure(task -> task.manifest(manifest -> manifest.attributes(attributes)));
    }

    public static void replaceTokens(Project project, Map<String, Object> expandProps, String... files) {
        if (files.length < 1) {
            throw new GradleException("Can not replace tokens if no files are specified!");
        }
        project.getTasks().named("processResources", Copy.class).configure(task -> {
            task.filesMatching(Arrays.asList(files), details -> details.expand(expandProps));
            task.getInputs().properties(expandProps);
        });
    }

    public static Project validateSubproject(Project root, String name) {
        final Project project = root.findProject(name);
        if (project == null) {
            throw new GradleException("Expected subproject '" + name + "' does not exist!");
        }
        return project;
    }

    public static Configuration createConfiguration(Project project, String name) {
        return project.getConfigurations().create(name, cfg -> {
            cfg.setCanBeResolved(false);
            cfg.setCanBeConsumed(true);
        });
    }

    public static String capitalize(String str) {
        return (str == null || str.isEmpty()) ? str : str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static int countChar(String str, char target) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    public static String getEnv(String propertyName, String fallback) {
        final String property = System.getenv(propertyName);
        return property != null && !property.isBlank() ? property : fallback;
    }
}