package net.darkhax.mmc.config;

import net.darkhax.mmc.ConventionsPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public record BuildConfig(ModData mod, HostedProject curseforge, HostedProject modrinth, @Nullable List<Dependency> dependencies, Boolean enable_compat_mode) {

    public void applyDependencies(Project project, Platform platform) {
        // To avoid adding local runtime dependencies to maven and module meta,
        // we create a local runtime and use that instead.
        final Configuration runtime = project.getConfigurations().getByName("runtimeClasspath");
        final Configuration localRuntime = project.getConfigurations().maybeCreate("localRuntime");
        runtime.extendsFrom(localRuntime);
        final DependencyHandler projectDeps = project.getDependencies();
        if (this.dependencies != null) {
            for (Dependency depInfo : this.dependencies) {
                final String depNotation = depInfo.maven().get(platform);
                if (depNotation != null) {
                    ConventionsPlugin.LOGGER.lifecycle("Adding {} dependency '{}' to {}.", depInfo.type().name().toLowerCase(Locale.ROOT), depNotation, project.getDisplayName());
                    projectDeps.add("localRuntime", depNotation);
                    projectDeps.add("compileOnly", depNotation);
                }
            }
        }
    }

    public String cursePage() {
        return "https://www.curseforge.com/minecraft/mc-mods/" + this.curseforge.slug();
    }

    public String modrinthPage() {
        return "https://modrinth.com/mod/" + this.modrinth.slug();
    }

    public String issuesPage() {
        return this.mod.repo() + "/issues";
    }

    public boolean isCompatabilityEnabled() {
        return this.enable_compat_mode == null || this.enable_compat_mode;
    }
}