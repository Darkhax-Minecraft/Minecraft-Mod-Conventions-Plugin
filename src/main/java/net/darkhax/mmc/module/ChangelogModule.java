package net.darkhax.mmc.module;

import net.darkhax.mmc.ConventionsPlugin;
import net.darkhax.mmc.Util;
import net.darkhax.mmc.config.BuildConfig;
import net.darkhax.mmc.config.GameTarget;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.process.ExecOperations;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

public class ChangelogModule {

    public static void setupChangelog(Project project, BuildConfig config, ExecOperations exec, GameTarget target) {
        final Provider<String> changelogProvider = project.provider(() -> {
            final String latestCommit = Util.getEnv("GIT_COMMIT", "HEAD");
            final String lastCommit = Util.getEnv("GIT_PREVIOUS_SUCCESSFUL_COMMIT", null);
            final String branchDisplayName = Util.getEnv("GIT_BRANCH", target.gameVersion());
            if (lastCommit == null) {
                return "This is the first successful build for " + branchDisplayName + ". You can learn more here: " + config.mod().repo() + "/commits/" + branchDisplayName + "/";
            }
            else {
                try {
                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    exec.exec(spec -> {
                        spec.commandLine("git", "log", "--pretty=format:'%H%x1f%h%x1f%an%x1f%s%x1f%B'", latestCommit + "..." + lastCommit);
                        spec.setStandardOutput(outputStream);
                    });
                    final StringJoiner changelog = new StringJoiner(System.lineSeparator());
                    for (GitCommit commit : parse(outputStream.toString())) {
                        if (!commit.message.contains("$exclude_changelog$")) {
                            changelog.add("- " + commit.subject);
                        }
                    }
                    return changelog.toString();
                }
                catch (Exception e) {
                    return "The changelog for this build is unavailable.";
                }
            }
        });
        project.getExtensions().add("mod_changelog", changelogProvider);
    }

    public record GitCommit(String hash, String shortHash, String author, String subject, String message) {

    }

    public static String getChangelog(Project project) {
        String changelog = "";
        final Object object = project.getExtensions().getByName("mod_changelog");
        if (object instanceof Provider<?> provider && provider.get() instanceof String output) {
            changelog = output;
        }
        return changelog;
    }

    public static List<GitCommit> parse(String input) {
        final LinkedList<GitCommit> commits = new LinkedList<>();
        for (String raw : input.split("(?=\\b[0-9a-f]{40}\\b)")) {
            if (raw.isBlank()) continue;

            // Split fields using the unit separator
            String[] parts = raw.split("\u001f", 5);

            if (parts.length != 5) {
                ConventionsPlugin.LOGGER.warn("Skipping changelog item '{}'", raw);
            }

            String hash = parts[0].trim();
            String shortHash = parts[1].trim();
            String author = parts[2].trim();
            String subject = parts[3].trim();
            String message = parts[4].trim();

            commits.add(new GitCommit(hash, shortHash, author, subject, message));
        }
        return commits;
    }
}