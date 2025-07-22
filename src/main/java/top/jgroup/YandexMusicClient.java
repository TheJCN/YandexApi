package top.jgroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import okhttp3.*;
import top.jgroup.exeptions.TokenNotSetException;
import top.jgroup.exeptions.YandexMusicException;
import top.jgroup.helpers.YnisonHelper;
import top.jgroup.model.TrackInfo;

import java.net.ProtocolException;
import java.util.concurrent.CompletableFuture;

/** * <p><b>Русский:</b></p>
 * <p>
 * Класс для работы с Yandex Music API.
 * Позволяет асинхронно получать информацию о текущем треке и его ID.
 * </p>
 *
 * <p><b>English:</b></p>
 * <p>
 * Class for working with Yandex Music API.
 * Allows asynchronous retrieval of the current track information and its ID.
 * </p>
 */
public class YandexMusicClient {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * <p><b>Русский:</b></p>
     * <p>
     * Устанавливает OAuth токен для доступа к Yandex Music API.
     * Если токен не установлен, будет выброшено исключение {@link TokenNotSetException}.
     * </p>
     *
     * <p><b>English:</b></p>
     * <p>
     * Sets the OAuth token for accessing the Yandex Music API.
     * If the token is not set, a {@link TokenNotSetException} will be thrown.
     * </p>
     */
    private @Setter String token;

    private void checkToken() {
        if (this.token == null || this.token.isBlank()) {
            throw new TokenNotSetException("OAuth токен не установлен.");
        }
    }

    /**
     * <p><b>Русский:</b></p>
     * <p>
     * Асинхронно получает ID текущего трека.
     * Возвращает {@link java.util.concurrent.CompletableFuture}, который завершится,
     * когда ID трека будет получен.
     * Если токен не установлен, будет выброшено исключение {@link TokenNotSetException}.
     * </p>
     *
     * <p><b>English:</b></p>
     * <p>
     * Asynchronously gets the ID of the current track.
     * Returns a {@link java.util.concurrent.CompletableFuture} that completes
     * when the track ID is obtained.
     * If the token is not set, a {@link TokenNotSetException} will be thrown.
     * </p>
     *
     * @return CompletableFuture с ID текущего трека / CompletableFuture with the current track ID
     */
    public CompletableFuture<String> getCurrentTrackIdAsync() {
        checkToken();

        return YnisonHelper.getCurrentTrackId(token, client, mapper);
    }

    /**
     * <p><b>Русский:</b></p>
     * <p>
     * Асинхронно получает информацию о треке по его ID.
     * Возвращает {@link java.util.concurrent.CompletableFuture}, который завершится,
     * когда информация о треке будет получена.
     * Если токен не установлен, будет выброшено исключение {@link TokenNotSetException}.
     * </p>
     *
     * <p><b>English:</b></p>
     * <p>
     * Asynchronously gets track information by its ID.
     * Returns a {@link java.util.concurrent.CompletableFuture} that completes
     * when the track information is obtained.
     * If the token is not set, a {@link TokenNotSetException} will be thrown.
     * </p>
     *
     * @param trackId ID трека / Track ID
     * @return CompletableFuture с информацией о треке / CompletableFuture with track information
     */
    public CompletableFuture<TrackInfo> getTrackInfoAsync(String trackId) {
        checkToken();

        return getTrackRawInfoAsync(trackId).thenApply(node -> {
            if (node == null || node.isMissingNode()) {
                throw new YandexMusicException("Трек не найден в JSON");
            }
            return TrackInfo.fromJson(node);
        });
    }

    /**
     * <p><b>Русский:</b></p>
     * <p>
     * Асинхронно получает необработанную информацию о треке по его ID.
     * Возвращает {@link java.util.concurrent.CompletableFuture}, который завершится,
     * когда необработанная информация о треке будет получена.
     * Если токен не установлен, будет выброшено исключение {@link TokenNotSetException}.
     * </p>
     *
     * <p><b>English:</b></p>
     * <p>
     * Asynchronously gets raw track information by its ID.
     * Returns a {@link java.util.concurrent.CompletableFuture} that completes
     * when the raw track information is obtained.
     * If the token is not set, a {@link TokenNotSetException} will be thrown.
     * </p>
     *
     * @param trackId ID трека / Track ID
     * @return CompletableFuture с необработанной информацией о треке / CompletableFuture with raw track information
     */
    public CompletableFuture<JsonNode> getTrackRawInfoAsync(String trackId) {
        checkToken();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.music.yandex.net/tracks/" + trackId)
                        .header("Authorization", "OAuth " + token)
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful())
                        throw new ProtocolException("Ошибка при получении информации о треке: HTTP " + response.code());

                    JsonNode node = mapper.readTree(response.body().string()).path("result").get(0);
                    if (node == null || node.isMissingNode())
                        throw new YandexMusicException("Трек не найден в JSON");

                    return node;
                }
            } catch (Exception e) {
                throw new YandexMusicException("Ошибка при получении информации о треке", e);
            }
        });
    }
}

