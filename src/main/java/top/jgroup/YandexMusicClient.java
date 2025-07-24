package top.jgroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import okhttp3.*;
import top.jgroup.exeptions.OAuthTokenAbroadException;
import top.jgroup.exeptions.TokenNotSetException;
import top.jgroup.exeptions.YandexMusicException;
import top.jgroup.helpers.YnisonHelper;
import top.jgroup.model.TrackInfo;

import java.net.ProtocolException;
import java.util.concurrent.CompletableFuture;

/**
 * <p><b>Русский:</b></p>
 * <p>
 * Класс для работы с Yandex Music API.
 * Позволяет асинхронно получать информацию о текущем треке и о треках по их ID.
 * Для работы требуется указать <b>обязательный OAuth токен</b>.
 * Есть три способа создать и настроить клиент:
 * </p>
 * <ul>
 *   <li>Через конструктор с токеном и флагом OAuth:
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient(token, true);
 * }</pre>
 *   </li>
 *   <li>Через конструктор только с токеном (флаг OAuth не обязателен, по умолчанию false):
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient(token);
 * }</pre>
 *   </li>
 *   <li>Через пустой конструктор с последующей установкой параметров:
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient();
 * client.setToken(token);        // обязательный вызов
 * client.setIsOauth(true);       // необязательный вызов
 * }</pre>
 *   </li>
 * </ul>
 * <p>
 * <b>Важно:</b> если <code>token</code> не установлен или пуст,
 * то при вызове любого метода, требующего авторизации,
 * будет выброшено исключение {@link top.jgroup.exeptions.TokenNotSetException}.
 * </p>
 *
 * <hr>
 *
 * <p><b>English:</b></p>
 * <p>
 * A client class for working with Yandex Music API.
 * Allows asynchronous retrieval of the current track information and track info by ID.
 * A <b>valid OAuth token</b> is <b>required</b>.
 * You can configure the client in one of the following ways:
 * </p>
 * <ul>
 *   <li>Using a constructor with token and OAuth flag:
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient(token, true);
 * }</pre>
 *   </li>
 *   <li>Using a constructor with only token (OAuth flag is optional, defaults to false):
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient(token);
 * }</pre>
 *   </li>
 *   <li>Using the default constructor and setters:
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient();
 * client.setToken(token);        // mandatory
 * client.setIsOauth(true);       // optional
 * }</pre>
 *   </li>
 * </ul>
 * <p>
 * <b>Important:</b> if <code>token</code> is not set or is blank,
 * any method that requires authorization will throw a {@link top.jgroup.exeptions.TokenNotSetException}.
 * </p>
 */

public class YandexMusicClient {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private @Setter String token;

    private @Setter boolean isOauth;

    public YandexMusicClient(String token, boolean isOauth) {
        this.token = token;
        this.isOauth = isOauth;
    }

    public YandexMusicClient(String token) {
        this(token, false);
    }

    public YandexMusicClient() {
    }

    private void checkToken() {
        if (this.token == null || this.token.isBlank()) {
            throw new TokenNotSetException("Токен не установлен. Установите токен перед использованием методов, требующих авторизации.");
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

                    if (!response.isSuccessful()) {

                        String errorBody = response.body().string();
                        if (isOauth && !errorBody.isBlank() && errorBody.contains("Unavailable For Legal Reasons")) {
                            throw new OAuthTokenAbroadException(
                                    "Доступ к ресурсу ограничен по юридическим причинам (Unavailable For Legal Reasons)."
                            );
                        }

                        throw new ProtocolException("Ошибка при получении информации о треке: HTTP " + response.code());
                    }

                    String responseString = response.body().string();
                    JsonNode jsonString = mapper.readTree(responseString);
                    JsonNode node = jsonString.path("result").get(0);
                    if (node == null || node.isMissingNode())
                        throw new YandexMusicException("Трек не найден в JSON");

                    return node;
                }
            } catch (Exception e) {
                if (e instanceof OAuthTokenAbroadException) {
                    throw (OAuthTokenAbroadException) e;
                }
                throw new YandexMusicException("Ошибка при получении информации о треке", e);
            }
        });
    }

}

