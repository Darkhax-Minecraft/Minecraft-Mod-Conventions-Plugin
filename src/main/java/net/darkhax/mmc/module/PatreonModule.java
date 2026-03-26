package net.darkhax.mmc.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.darkhax.mmc.ConventionsPlugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PatreonModule {

    public static final Logger LOGGER = Logging.getLogger("Patreon");
    public static final String CAMPAIGN = "patreon_campaign_id";
    public static final String AUTH_TOKEN = "patreon_auth_token";
    public static final String PATRONS = "patreon_patrons";

    public static void setup(Project rootProject) {
        try {
            if (rootProject.findProperty(AUTH_TOKEN) instanceof String authToken && rootProject.findProperty(CAMPAIGN) instanceof String campaignId) {
                final Set<String> names = fetchActivePatrons(authToken, campaignId);
                rootProject.getExtensions().getExtraProperties().set(PATRONS, names);
                LOGGER.lifecycle("Loaded data for {} patrons!", names.size());
            }
            else {
                final String authMsg = rootProject.hasProperty(AUTH_TOKEN) ? "" : " " + AUTH_TOKEN + " not available.";
                final String campaignMsg = rootProject.hasProperty(CAMPAIGN) ? "" : " " + CAMPAIGN + " not available.";
                LOGGER.warn("Could not fetch Patreon data.{}{}", authMsg, campaignMsg);
                rootProject.getExtensions().getExtraProperties().set(PATRONS, Set.of());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to fetch supporters from Patreon!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getPatrons(Project project) {
        final Object patrons = project.getRootProject().getExtensions().getExtraProperties().get(PATRONS);
        return patrons instanceof Set<?> set ? (Set<String>) set : Collections.emptySet();
    }

    private static Set<String> parseUsers(JsonObject root) {
        final Map<String, JsonObject> userMap = new HashMap<>();
        for (JsonElement elem : root.getAsJsonArray("included")) {
            JsonObject obj = elem.getAsJsonObject();
            if ("user".equals(obj.get("type").getAsString())) {
                userMap.put(obj.get("id").getAsString(), obj);
            }
        }
        final Set<String> result = new HashSet<>();
        for (JsonElement elem : root.getAsJsonArray("data")) {
            final JsonObject member = elem.getAsJsonObject();
            final JsonObject attributes = member.getAsJsonObject("attributes");
            final JsonObject relationships = member.getAsJsonObject("relationships");
            final JsonObject userData = relationships.getAsJsonObject("user").getAsJsonObject("data");
            final JsonObject includedUser = userMap.get(userData.get("id").getAsString());
            if (includedUser != null) {
                final JsonObject userAttributes = includedUser.getAsJsonObject("attributes");
                final String fullName = nullableString(userAttributes, "full_name");
                final String vanity = nullableString(userAttributes, "vanity");
                final String status = nullableString(attributes, "patron_status");
                final int cents = attributes.get("campaign_lifetime_support_cents").getAsInt();
                if (cents > 0 && "active_patron".equals(status)) {
                    final String displayName = vanity != null ? vanity : fullName;
                    if (displayName != null) {
                        result.add(displayName);
                    }
                }
            }
        }
        return result;
    }

    private static String nullableString(JsonObject obj, String key) {
        final JsonElement element = obj.get(key);
        return (element == null || element.isJsonNull()) ? null : element.getAsString();
    }

    private static Set<String> fetchActivePatrons(String accessToken, String campaignId) {
        Set<String> results = new HashSet<>();
        try {
            final HttpURLConnection conn = requestPatrons(accessToken, campaignId);
            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    results = PatreonModule.parseUsers(JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
            conn.disconnect();

        }
        catch (Exception e) {
            ConventionsPlugin.LOGGER.error("Encountered an error while requesting Patreon data!", e);
        }
        return results;
    }

    private static @NonNull HttpURLConnection requestPatrons(String accessToken, String campaignId) throws IOException, URISyntaxException {
        final HttpURLConnection conn = (HttpURLConnection) new URI("https://www.patreon.com/api/oauth2/v2/campaigns/" + campaignId + "/members?include=user&page%5Bcount%5D=1000&fields%5Bmember%5D=campaign_lifetime_support_cents,patron_status&fields%5Buser%5D=vanity,full_name").toURL().openConnection();
        conn.setRequestProperty("User-Agent", "PatreonGradle (darklime@live.ca)");
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }
}