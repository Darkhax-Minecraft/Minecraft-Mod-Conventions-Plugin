package net.darkhax.mmc.module;

import net.darkhax.mmc.Util;
import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.process.ExecOperations;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ChangelogModule {

    private static String RS = String.valueOf((char) 0x1E);
    private static String US = String.valueOf((char) 0x1F);

    public static void setupChangelog(Project project, BuildConfig config, ExecOperations exec, GameTarget target) {
        final Provider<String> changelogProvider = project.provider(() -> {
            final String latestCommit = Util.getEnv("GIT_COMMIT", "HEAD");
            final String lastCommit = Util.getEnv("GIT_PREVIOUS_SUCCESSFUL_COMMIT", null);
            final String branchDisplayName = Util.getEnv("GIT_BRANCH", target.gameVersion());
            if (lastCommit == null) {
                return "This is the first successful build for " + branchDisplayName + ". You can learn more [here](" + config.mod().repo() + "/commits/" + branchDisplayName + "/).";
            }
            else {
                try {
                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    exec.exec(spec -> {
                        spec.commandLine("git", "log", "--pretty=format:'" + RS + "%an" + US + "%s" + US + "%B" + "'", latestCommit + "..." + lastCommit);
                        spec.setStandardOutput(outputStream);
                    });
                    final StringJoiner changelog = new StringJoiner(System.lineSeparator());
                    for (GitCommit commit : parseGitLog(outputStream.toString())) {
                        if (!commit.message.contains("$exclude_changelog$")) {
                            changelog.add("- " + commit.subject + "    ");
                        }
                    }
                    return changelog.toString();
                }
                catch (Exception e) {
                    project.getLogger().error("Failed to parse changelog file.", e);
                    return "The changelog for this build is unavailable.";
                }
            }
        });
        project.getExtensions().add("mod_changelog", changelogProvider);
    }

    public record GitCommit(String author, String subject, String message) {

    }

    public static String getChangelog(Project project) {
        String changelog = "";
        final Object object = project.getExtensions().getByName("mod_changelog");
        if (object instanceof Provider<?> provider && provider.get() instanceof String output) {
            changelog = output;
        }
        return changelog;
    }

    public static List<GitCommit> parseGitLog(String gitLogOutput) {
        final List<GitCommit> commits = new ArrayList<>();
        for (String entry : gitLogOutput.split(RS)) {
            final String[] parts = entry.split(US);
            if (parts.length == 3) {
                commits.add(new GitCommit(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            }
        }
        return commits;
    }
}