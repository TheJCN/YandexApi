import top.jgroup.YandexMusicClient;
import top.jgroup.model.TrackInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        }

        // --- Токены ---
        String oauthToken = props.getProperty("YANDEX_MUSIC_TOKEN_OAUTH");
        String extensionToken = props.getProperty("YANDEX_MUSIC_TOKEN_EXTENSION");

        // --- Параметры прокси ---
        String proxyHost = props.getProperty("PROXY_HOST");
        int proxyPort = Integer.parseInt(props.getProperty("PROXY_PORT"));
        String proxyUser = props.getProperty("PROXY_USERNAME");
        String proxyPassword = props.getProperty("PROXY_PASSWORD");

        System.out.println("Используем прокси: " + proxyHost + ":" + proxyPort + " с авторизацией: " + (proxyUser != null && !proxyUser.isEmpty()));
        Proxy proxyAuth = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        // --- Параметры прокси без авторизации ---
        String proxyHostNoAuth = props.getProperty("PROXY_HOST_NO_AUTH");
        int proxyPortNoAuth = Integer.parseInt(props.getProperty("PROXY_PORT_NO_AUTH"));

        System.out.println("Используем прокси без авторизации: " + proxyHostNoAuth + ":" + proxyPortNoAuth);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostNoAuth, proxyPortNoAuth));

        // --- Карта для логов ---
        Map<String, String> results = new LinkedHashMap<>();

        // ========== OAUTH БЕЗ ПРОКСИ ==========
        try {
            YandexMusicClient oauthClient = new YandexMusicClient(oauthToken, true);
            TrackInfo track = getTrack(oauthClient);
            System.out.println("✅ OAuth без прокси: " + track);
            results.put("OAuth без прокси", "✅ Успех");
        } catch (Exception e) {
            System.out.println("❌ OAuth без прокси: " + e.getMessage());
            results.put("OAuth без прокси", "❌ Неуспех");
        }

        // ========== OAUTH С ПРОКСИ (без авторизации) ==========
        try {
            YandexMusicClient oauthProxyNoAuth = new YandexMusicClient(oauthToken, true, proxy);
            // Без setProxyUser/setProxyPassword
            TrackInfo track = getTrack(oauthProxyNoAuth);
            System.out.println("✅ OAuth с прокси без авторизации: " + track);
            results.put("OAuth с прокси без авторизации", "✅ Успех");
        } catch (Exception e) {
            System.out.println("❌ OAuth с прокси без авторизации: " + e.getMessage());
            results.put("OAuth с прокси без авторизации", "❌ Неуспех");
        }

        // ========== OAUTH С ПРОКСИ (с авторизацией) ==========
        try {
            YandexMusicClient oauthProxyAuth = new YandexMusicClient(oauthToken, true, proxyAuth);
            oauthProxyAuth.setProxyUser(proxyUser);
            oauthProxyAuth.setProxyPassword(proxyPassword);
            oauthProxyAuth.setProxyAuthRequired(true);
            TrackInfo track = getTrack(oauthProxyAuth);
            System.out.println("✅ OAuth с прокси c авторизацией: " + track);
            results.put("OAuth с прокси c авторизацией", "✅ Успех");
        } catch (Exception e) {
            System.out.println("❌ OAuth с прокси c авторизацией: " + e.getMessage());
            results.put("OAuth с прокси c авторизацией", "❌ Неуспех");
        }

        // ========== EXTENSION БЕЗ ПРОКСИ ==========
        try {
            YandexMusicClient extensionNoProxy = new YandexMusicClient();
            extensionNoProxy.setToken(extensionToken);
            TrackInfo track = getTrack(extensionNoProxy);
            System.out.println("✅ Extension без прокси: " + track);
            results.put("Extension без прокси", "✅ Успех");
        } catch (Exception e) {
            System.out.println("❌ Extension без прокси: " + e.getMessage());
            results.put("Extension без прокси", "❌ Неуспех");
        }

        // ========== EXTENSION С ПРОКСИ (без авторизации) ==========
        try {
            YandexMusicClient extensionProxyNoAuth = new YandexMusicClient();
            extensionProxyNoAuth.setToken(extensionToken);
            extensionProxyNoAuth.setProxy(proxy);
            // без user/password
            TrackInfo track = getTrack(extensionProxyNoAuth);
            System.out.println("✅ Extension с прокси без авторизации: " + track);
            results.put("Extension с прокси без авторизации", "✅ Успех");
        } catch (Exception e) {
            System.out.println("❌ Extension с прокси без авторизации: " + e.getMessage());
            results.put("Extension с прокси без авторизации", "❌ Неуспех");
        }

        // ========== EXTENSION С ПРОКСИ (с авторизацией) ==========
        try {
            YandexMusicClient extensionProxyAuth = new YandexMusicClient();
            extensionProxyAuth.setToken(extensionToken);
            extensionProxyAuth.setProxy(proxyAuth);
            extensionProxyAuth.setProxyUser(proxyUser);
            extensionProxyAuth.setProxyPassword(proxyPassword);
            extensionProxyAuth.setProxyAuthRequired(true);
            TrackInfo track = getTrack(extensionProxyAuth);
            System.out.println("✅ Extension с прокси c авторизацией: " + track);
            results.put("Extension с прокси c авторизацией", "✅ Успех");
        } catch (Exception e) {
            System.out.println("❌ Extension с прокси c авторизацией: " + e.getMessage());
            results.put("Extension с прокси c авторизацией", "❌ Неуспех");
        }

        // ========== ВЫВОДИМ ТАБЛИЦУ ==========
        System.out.println("\n===== 📊 РЕЗУЛЬТАТЫ ВСЕХ ЗАПРОСОВ =====");
        System.out.printf("%-40s | %-10s%n", "Конфигурация", "Статус");
        System.out.println("---------------------------------------------------------------");
        for (Map.Entry<String, String> entry : results.entrySet()) {
            System.out.printf("%-40s | %-10s%n", entry.getKey(), entry.getValue());
        }
    }

    // Общий метод для получения трека
    private static TrackInfo getTrack(YandexMusicClient client) {
        return client.getCurrentTrackIdAsync()
                .thenCompose(client::getTrackInfoAsync)
                .join();
    }
}
