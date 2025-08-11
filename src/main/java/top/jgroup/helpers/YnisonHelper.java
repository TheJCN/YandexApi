package top.jgroup.helpers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import top.jgroup.exceptions.YandexMusicException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class YnisonHelper {

    private static final String REDIRECT_WS = "wss://ynison.music.yandex.ru/redirector.YnisonRedirectService/GetRedirectToYnison";

    public static CompletableFuture<String> getCurrentTrackId(String token, OkHttpClient client, ObjectMapper mapper) {
        String deviceId = generateDeviceId();

        Map<String, Object> deviceInfo = Map.of(
                "app_name", "Chrome",
                "type", 1
        );

        Map<String, Object> wsProto = new LinkedHashMap<>();
        wsProto.put("Ynison-Device-Id", deviceId);
        try {
            wsProto.put("Ynison-Device-Info", mapper.writeValueAsString(deviceInfo));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new YandexMusicException("Ошибка сериализации deviceInfo", e));
        }

        return getRedirect(token, client, mapper, wsProto)
                .thenCompose(redirectData -> {
                    String host = (String) redirectData.get("host");
                    String redirectTicket = (String) redirectData.get("redirect_ticket");
                    wsProto.put("Ynison-Redirect-Ticket", redirectTicket);
                    Map<String, Object> payload = createPayload(deviceId);

                    Request request = new Request.Builder()
                            .url("wss://" + host + "/ynison_state.YnisonStateService/PutYnisonState")
                            .header("Sec-WebSocket-Protocol", "Bearer, v2, " + toJson(mapper, wsProto))
                            .header("Origin", "http://music.yandex.ru")
                            .header("Authorization", "OAuth " + token)
                            .build();

                    CompletableFuture<String> future = new CompletableFuture<>();

                    client.newWebSocket(request, new WebSocketListener() {
                        @Override
                        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                            try {
                                String jsonPayload = mapper.writeValueAsString(payload);
                                webSocket.send(jsonPayload);
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                                webSocket.close(1011, "Serialization Error");
                            }
                        }

                        @Override
                        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                            try {
                                JsonNode root = mapper.readTree(text);
                                JsonNode queue = root.path("player_state").path("player_queue");
                                int currentIndex = queue.path("current_playable_index").asInt(-1);

                                if (currentIndex < 0) {
                                    future.completeExceptionally(new YandexMusicException("Нет текущего трека"));
                                } else {
                                    JsonNode track = queue.path("playable_list").get(currentIndex);
                                    String playableId = track.path("playable_id").asText();
                                    future.complete(playableId);
                                }
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            } finally {
                                webSocket.close(1000, null);
                            }
                        }

                        @Override
                        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable throwable, Response response) {
                            future.completeExceptionally(throwable);
                        }
                    });

                    return future;
                });
    }

    private static CompletableFuture<Map<String, Object>> getRedirect(String token, OkHttpClient client, ObjectMapper mapper, Map<String, Object> wsProto) {
        Request request = new Request.Builder()
                .url(REDIRECT_WS)
                .header("Sec-WebSocket-Protocol", "Bearer, v2, " + toJson(mapper, wsProto))
                .header("Origin", "http://music.yandex.ru")
                .header("Authorization", "OAuth " + token)
                .build();

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                try {
                    Map<String, Object> result = mapper.readValue(text, Map.class);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    webSocket.close(1000, null);
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable throwable, Response response) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }

    private static String toJson(ObjectMapper mapper, Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации JSON", e);
        }
    }

    private static Map<String, Object> createPayload(String deviceId) {
        Map<String, Object> versionQueue = Map.of("device_id", deviceId, "version", 9021243204784341000L, "timestamp_ms", 0);
        Map<String, Object> versionStatus = Map.of("device_id", deviceId, "version", 8321822175199937000L, "timestamp_ms", 0);

        Map<String, Object> playerQueue = new LinkedHashMap<>();
        playerQueue.put("current_playable_index", -1);
        playerQueue.put("entity_id", "");
        playerQueue.put("entity_type", "VARIOUS");
        playerQueue.put("playable_list", new ArrayList<>());
        playerQueue.put("options", Map.of("repeat_mode", "NONE"));
        playerQueue.put("entity_context", "BASED_ON_ENTITY_BY_DEFAULT");
        playerQueue.put("version", versionQueue);
        playerQueue.put("from_optional", "");

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("duration_ms", 0);
        status.put("paused", true);
        status.put("playback_speed", 1);
        status.put("progress_ms", 0);
        status.put("version", versionStatus);

        Map<String, Object> playerState = new LinkedHashMap<>();
        playerState.put("player_queue", playerQueue);
        playerState.put("status", status);

        Map<String, Object> device = new LinkedHashMap<>();
        device.put("capabilities", Map.of("can_be_player", true, "can_be_remote_controller", false, "volume_granularity", 16));
        device.put("info", Map.of("device_id", deviceId, "type", "WEB", "title", "Chrome Browser", "app_name", "Chrome"));
        device.put("volume_info", Map.of("volume", 0));
        device.put("is_shadow", true);

        Map<String, Object> updateFullState = new LinkedHashMap<>();
        updateFullState.put("player_state", playerState);
        updateFullState.put("device", device);
        updateFullState.put("is_currently_active", false);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("update_full_state", updateFullState);
        payload.put("rid", UUID.randomUUID().toString());
        payload.put("player_action_timestamp_ms", 0);
        payload.put("activity_interception_type", "DO_NOT_INTERCEPT_BY_DEFAULT");

        return payload;
    }

    private static String generateDeviceId() {
        int length = 16;
        String letters = "abcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(letters.charAt(rnd.nextInt(letters.length())));
        }
        return sb.toString();
    }
}

