package net.darkhax.mmc.module;

import net.darkhax.curseforgegradle.Constants;
import net.darkhax.curseforgegradle.TaskPublishCurseForge;
import net.darkhax.curseforgegradle.UploadArtifact;
import net.darkhax.mmc.config.*;
import org.gradle.api.Project;

import java.util.Set;

public class CurseForgeModule {

    public static void setupCurseForge(Project rootProject, Project subProject, BuildConfig config, Platform platform, GameTarget target) {
        final Object apiKeyProp = rootProject.findProperty("curse_auth");
        if (apiKeyProp instanceof String apiKey) {
            subProject.getPluginManager().apply("net.darkhax.curseforgegradle");
            subProject.getTasks().register("publishCurseForge", TaskPublishCurseForge.class, task -> {
                task.apiToken = apiKey;
                final UploadArtifact mainFile = task.upload(config.curseforge().id(), subProject.getTasks().named("jar").get());
                mainFile.changelogType = Constants.CHANGELOG_MARKDOWN;
                mainFile.changelog = ChangelogModule.getChangelog(rootProject);
                mainFile.releaseType = Constants.RELEASE_TYPE_RELEASE;
                mainFile.addGameVersion(target.gameVersion());
                if (config.isCompatabilityEnabled()) {
                    target.compatibleWith().forEach(mainFile::addGameVersion);
                }
                mainFile.addGameVersion("Client");
                if (!config.mod().client()) {
                    mainFile.addGameVersion("Server");
                }
                switch (platform) {
                    case FABRIC -> mainFile.addGameVersion("Fabric");
                    case NEOFORGE -> mainFile.addGameVersion("NeoForge");
                }
                if (config.dependencies() != null) {
                    for (Dependency dependency : config.dependencies()) {
                        if (dependency.type() != DependencyType.MISC && dependency.maven().containsKey(platform)) {
                            switch (dependency.type()) {
                                case REQUIRED -> mainFile.addRequirement(dependency.curseforge().slug());
                                case OPTIONAL -> mainFile.addOptional(dependency.curseforge().slug());
                            }
                        }
                    }
                }
                final Set<String> patrons = PatreonModule.getPatrons(rootProject);
                if (patrons != null && !patrons.isEmpty()) {
                    mainFile.changelog += System.lineSeparator() + System.lineSeparator() + "This project is made possible with [Patreon](https://www.patreon.com/Darkhax) support from players like you! Thank you!    ";
                    for (String patron : patrons) {
                        mainFile.changelog += System.lineSeparator() + "- " + patron + "    ";
                    }
                }
            });
        }
    }
}
