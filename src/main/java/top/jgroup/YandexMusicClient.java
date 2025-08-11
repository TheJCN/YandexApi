package top.jgroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import okhttp3.*;
import top.jgroup.exceptions.OAuthTokenAbroadException;
import top.jgroup.exceptions.TokenNotSetException;
import top.jgroup.exceptions.YandexMusicException;
import top.jgroup.helpers.YnisonHelper;
import top.jgroup.model.TrackInfo;

import java.net.ProtocolException;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p><b>Русский:</b></p>
 * <p>
 * Класс для работы с Yandex Music API.
 * Позволяет асинхронно получать информацию о текущем треке и о треках по их ID.
 * Для работы требуется указать <b>обязательный OAuth токен</b>.
 * Есть несколько способов создать и настроить клиент:
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
 *   <li>Через конструктор с токеном, флагом OAuth и прокси:
 *     <pre>{@code
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080));
 * YandexMusicClient client = new YandexMusicClient(token, true, proxy);
 * }</pre>
 *   </li>
 *   <li>Если используется приватный прокси с логином и паролем:
 *     <pre>{@code
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080));
 * YandexMusicClient client = new YandexMusicClient(token, true, proxy, "userName", "password");
 * }</pre>
 *   </li>
 *   <li>Если нужно сразу указать, требуется ли авторизация на прокси:
 *     <pre>{@code
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080));
 * YandexMusicClient client = new YandexMusicClient(
 *     token,
 *     true,
 *     proxy,
 *     true,                // proxyAuthRequired
 *     "userName",
 *     "password"
 * );
 * }</pre>
 *   </li>
 * </ul>
 * <p>
 * <b>Важно:</b> если <code>token</code> не установлен или пуст,
 * то при вызове любого метода, требующего авторизации,
 * будет выброшено исключение {@link top.jgroup.exceptions.TokenNotSetException}.
 * Если используется приватный прокси с авторизацией,
 * нужно включить {@code proxyAuthRequired = true} и указать логин и пароль.
 * Если прокси не установлен, запросы выполняются напрямую.
 * </p>
 *
 * <hr>
 *
 * <p><b>English:</b></p>
 * <p>
 * A client class for working with Yandex Music API.
 * Allows asynchronous retrieval of the current track information and track info by ID.
 * A <b>valid OAuth token</b> is <b>required</b>.
 * Several ways to create and configure the client:
 * </p>
 * <ul>
 *   <li>Constructor with token and OAuth flag:
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient(token, true);
 * }</pre>
 *   </li>
 *   <li>Constructor with only token (OAuth flag optional, defaults to false):
 *     <pre>{@code
 * YandexMusicClient client = new YandexMusicClient(token);
 * }</pre>
 *   </li>
 *   <li>Constructor with token, OAuth flag and proxy:
 *     <pre>{@code
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080));
 * YandexMusicClient client = new YandexMusicClient(token, true, proxy);
 * }</pre>
 *   </li>
 *   <li>If using a private proxy with username and password:
 *     <pre>{@code
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080));
 * YandexMusicClient client = new YandexMusicClient(token, true, proxy, "userName", "password");
 * }</pre>
 *   </li>
 *   <li>If you want to specify whether proxy authorization is required:
 *     <pre>{@code
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.host", 8080));
 * YandexMusicClient client = new YandexMusicClient(
 *     token,
 *     true,
 *     proxy,
 *     true,                // proxyAuthRequired
 *     "userName",
 *     "password"
 * );
 * }</pre>
 *   </li>
 * </ul>
 * <p>
 * <b>Important:</b> if <code>token</code> is not set or is blank,
 * any method that requires authorization will throw a {@link top.jgroup.exceptions.TokenNotSetException}.
 * If using a private proxy with authorization,
 * you must set {@code proxyAuthRequired = true} and specify username and password.
 * If no proxy is set, requests are made directly.
 * </p>
 */
public class YandexMusicClient {

    private OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private @Setter String token;
    private @Setter boolean isOauth;
    private Proxy proxy;

    private @Setter String proxyUser;
    private @Setter String proxyPassword;
    private boolean proxyAuthRequired = false;

    public YandexMusicClient(String token, boolean isOauth, Proxy proxy,
                             boolean proxyAuthRequired, String proxyUser, String proxyPassword) {
        this.token = token;
        this.isOauth = isOauth;
        this.proxy = proxy;
        this.proxyAuthRequired = proxyAuthRequired;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        initClient();
    }

    public YandexMusicClient(String token, boolean isOauth, Proxy proxy, String proxyUser, String proxyPassword) {
        this.token = token;
        this.isOauth = isOauth;
        this.proxy = proxy;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        initClient();
    }

    public YandexMusicClient(String token, boolean isOauth, Proxy proxy) {
        this.token = token;
        this.isOauth = isOauth;
        this.proxy = proxy;
        initClient();
    }

    public YandexMusicClient(String token, boolean isOauth) {
        this(token, isOauth, null);
    }

    public YandexMusicClient(String token) {
        this(token, false, null);
    }

    public YandexMusicClient() {
        initClient();
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
        initClient();
    }

    public void setProxyAuthRequired(boolean proxyAuthRequired) {
        this.proxyAuthRequired = proxyAuthRequired;
        initClient();
    }

    private void initClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (proxy != null) {
            builder.proxy(proxy);
            builder.connectTimeout(5, TimeUnit.SECONDS);
            builder.readTimeout(5, TimeUnit.SECONDS);
            builder.writeTimeout(5, TimeUnit.SECONDS);

            if (proxyAuthRequired) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxyUser, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }
        this.client = builder.build();
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

