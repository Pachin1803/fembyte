package com.dractical.fembyte;

import com.destroystokyo.paper.VersionHistoryManager;
import com.destroystokyo.paper.util.VersionFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public final class FembyteVersionFetcher implements VersionFetcher {
    private static final Logger LOGGER = Logger.getLogger("FembyteVersionFetcher");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson GSON = new Gson();
    private static final String GITHUB_COMPARE_FORMAT = "https://api.github.com/repos/dractical/fembyte/compare/main...%s";
    private static final long CACHE_TIME_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private static final HttpResponse.BodyHandler<JsonObject> JSON_OBJECT_BODY_HANDLER =
            responseInfo -> HttpResponse.BodySubscribers.mapping(
                    HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                    json -> GSON.fromJson(json, JsonObject.class)
            );

    @Override
    public long getCacheTime() {
        return CACHE_TIME_MILLIS;
    }

    @Override
    public @NotNull Component getVersionMessage() {
        final Optional<String> gitCommit = ServerBuildInfo.buildInfo().gitCommit();

        final Component versionComponent = gitCommit
                .map(this::fetchGithubVersion)
                .orElseGet(() -> text("Unknown server version.", RED));

        final @Nullable Component history = this.getHistory();
        if (history == null) {
            return versionComponent;
        }

        return Component.join(
                JoinConfiguration.noSeparators(),
                versionComponent,
                Component.newline(),
                history
        );
    }

    private @NotNull Component fetchGithubVersion(final @NotNull String hash) {
        final URI uri = URI.create(String.format(GITHUB_COMPARE_FORMAT, hash));

        final HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "fembyte/VersionFetcher")
                .build();

        try {
            final HttpResponse<JsonObject> response = CLIENT.send(request, JSON_OBJECT_BODY_HANDLER);

            final int status = response.statusCode();
            if (status != 200) {
                LOGGER.warning("GitHub version check returned status code " + status);
                return text("Received invalid status code (" + status + ") from version server.", RED);
            }

            final JsonObject obj = response.body();
            if (obj == null || !obj.has("behind_by") || !obj.get("behind_by").isJsonPrimitive()) {
                LOGGER.warning("GitHub version response does not contain a valid 'behind_by' field.");
                return text("Received malformed response from version server.", RED);
            }

            final int versionDiff = obj.get("behind_by").getAsInt();
            return this.getResponseMessage(versionDiff);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Version check interrupted while contacting GitHub", e);
            return text("Version check was interrupted.", RED);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to look up version from GitHub", e);
            return text("Failed to retrieve version from server.", RED);
        }
    }

    private @NotNull Component getResponseMessage(final int versionDiff) {
        if (versionDiff < 0) {
            return text("You are running an unsupported / ahead-of-main version of fembyte.", RED);
        }

        if (versionDiff == 0) {
            return text("You are on the latest version!", GREEN);
        }

        return text(
                "You are running " + versionDiff + " version" + (versionDiff == 1 ? "" : "s") + " behind. "
                        + "Please update your server when possible to maintain stability and security, and to receive the latest optimizations.",
                RED
        );
    }

    private @Nullable Component getHistory() {
        final VersionHistoryManager.VersionData data = VersionHistoryManager.INSTANCE.getVersionData();
        if (data == null) {
            return null;
        }

        final String oldVersion = data.getOldVersion();
        if (oldVersion == null) {
            return null;
        }

        return text("Previous version: " + oldVersion, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }
}
