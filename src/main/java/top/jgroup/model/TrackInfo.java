package top.jgroup.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * <p><b>Русский:</b></p>
 * <p>
 * Класс для хранения информации о треке.
 * Содержит поля для ID, названия, исполнителя, продолжительности и URL обложки трека.
 * </p>
 *
 * <p><b>English:</b></p>
 * <p>
 * Class for storing track information.
 * Contains fields for ID, title, artist, duration, and cover URL of the track.
 * </p>
 */

public record TrackInfo(@Getter String id, @Getter String title, @Getter String artist, @Getter int durationMs,
                        @Getter String coverUrl) {

    /**
     * <p><b>Русский:</b></p>
     * <p>
     * Создает объект TrackInfo из JSON узла.
     * Если какие-либо поля отсутствуют, используются значения по умолчанию.
     * </p>
     *
     * <p><b>English:</b></p>
     * <p>
     * Creates a TrackInfo object from a JSON node.
     * If any fields are missing, default values are used.
     * </p>
     *
     * @param node JSON узел, содержащий информацию о треке
     * @return новый объект TrackInfo
     */
    public static TrackInfo fromJson(JsonNode node) {
        String id = node.path("id").asText("Unknown ID");
        String title = node.path("title").asText("Unknown title");
        int duration = node.path("durationMs").asInt(0);
        String artist = "Unknown Artist";

        JsonNode artists = node.path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            artist = artists.get(0).path("name").asText("Unknown Artist");
        }

        String coverUri = node.path("coverUri").asText("");
        if (coverUri.contains("%%")) {
            coverUri = coverUri.replace("%%", "1000x1000");
        }
        if (!coverUri.startsWith("http")) {
            coverUri = "https://" + coverUri;
        }

        return new TrackInfo(id, title, artist, duration, coverUri);
    }
}

