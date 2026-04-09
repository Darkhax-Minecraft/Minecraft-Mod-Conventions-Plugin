package net.darkhax.mmc.module;

import com.modrinth.minotaur.ModrinthExtension;
import com.modrinth.minotaur.dependencies.ModDependency;
import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.Dependency;
import net.darkhax.mmc.config.DependencyType;
import net.darkhax.mmc.config.GameTarget;
import net.darkhax.mmc.config.Platform;
import org.gradle.api.Project;

import java.util.Locale;
import java.util.Set;

public class ModrinthModule {

    public static void setupModrinth(Project rootProject, Project subProject, GameTarget target, BuildConfig config, Platform platform) {
        final Object apiKeyProp = rootProject.findProperty("modrinth_auth");
        if (apiKeyProp instanceof String apiKey) {
            subProject.getPluginManager().apply("com.modrinth.minotaur");
            final ModrinthExtension modrinth = subProject.getExtensions().getByType(ModrinthExtension.class);
            modrinth.getToken().set(apiKey);
            modrinth.getProjectId().set(config.modrinth().id());
            modrinth.getVersionName().set(config.mod().name() + "-" + platform.name().toLowerCase(Locale.ROOT) + "-" + rootProject.getVersion());
            modrinth.getVersionType().set("release");
            switch (platform) {
                case FABRIC -> modrinth.loaders.add("fabric");
                case NEOFORGE -> modrinth.loaders.add("neoforge");
            }
            modrinth.gameVersions.add(target.gameVersion());
            if (config.isCompatabilityEnabled()) {
                target.compatibleWith().forEach(modrinth.gameVersions::add);
            }
            modrinth.getUploadFile().set(subProject.getTasks().named("jar").get());

            // changelog
            StringBuilder changelog = new StringBuilder(ChangelogModule.getChangelog(rootProject));
            final Set<String> patrons = PatreonModule.getPatrons(rootProject);
            if (patrons != null && !patrons.isEmpty()) {
                changelog.append("\n\nThis project is made possible with [Patreon](https://www.patreon.com/Darkhax) support from players like you! Thank you!");
                for (String patron : patrons) {
                    changelog.append("\n- ").append(patron);
                }
            }
            modrinth.getChangelog().set(changelog.toString());

            // dependencies
            if (platform == Platform.FABRIC) {
                modrinth.getDependencies().add(new ModDependency("fabric-api", "required"));
            }
            if (config.dependencies() != null) {
                for (Dependency dependency : config.dependencies()) {
                    if (dependency.type() != DependencyType.MISC && dependency.maven().containsKey(platform)) {
                        switch (dependency.type()) {
                            case REQUIRED -> modrinth.getDependencies().add(new ModDependency(dependency.modrinth().slug(), "required"));
                            case OPTIONAL -> modrinth.getDependencies().add(new ModDependency(dependency.modrinth().slug(), "optional"));
                        }
                    }
                }
            }
        }
    }
}