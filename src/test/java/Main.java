import top.jgroup.YandexMusicClient;
import top.jgroup.model.TrackInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {

        Properties props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        }

        String oauthToken = props.getProperty("YANDEX_MUSIC_TOKEN_OAUTH");
        String extensionToken = props.getProperty("YANDEX_MUSIC_TOKEN_EXTENSION");

        // Прокси данные с логином и паролем
        String proxyHost = "185.66.14.139";
        int proxyPort = 9037;
        String proxyUser = props.getProperty("PROXY_USERNAME");
        String proxyPassword = props.getProperty("PROXY_PASSWORD");

        // Устанавливаем глобальный Authenticator для прокси с логином и паролем
        if (proxyUser != null && proxyPassword != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                    return null;
                }
            });
        }

        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));

        YandexMusicClient oauthClient = new YandexMusicClient(oauthToken, true);
        try {
            TrackInfo oauthTrack = getTrackFromOAuthToken(oauthClient);
            System.out.println("Трек из OAuth токена: " + oauthTrack);
        } catch (Exception e) {
            System.out.println("Ошибка получения трека с OAuth токена: " + e.getMessage());
        }

        YandexMusicClient oauthClientWithProxy = new YandexMusicClient(oauthToken, true, proxy);
        try {
            TrackInfo oauthTrackProxy = getTrackFromOAuthToken(oauthClientWithProxy);
            System.out.println("Трек из OAuth токена с прокси: " + oauthTrackProxy);
        } catch (Exception e) {
            System.out.println("Ошибка получения трека с OAuth токена и прокси: " + e.getMessage());
        }

        YandexMusicClient extensionClient = new YandexMusicClient();
        extensionClient.setToken(extensionToken);
        extensionClient.setProxy(proxy);
        try {
            TrackInfo extensionTrack = getTrackFromExtensionToken(extensionClient);
            System.out.println("Трек из Extension токена с прокси: " + extensionTrack);
        } catch (Exception e) {
            System.out.println("Ошибка получения трека с Extension токена и прокси: " + e.getMessage());
        }
    }

    private static TrackInfo getTrackFromOAuthToken(YandexMusicClient client) {
        return client.getCurrentTrackIdAsync()
                .thenCompose(client::getTrackInfoAsync)
                .join();
    }

    private static TrackInfo getTrackFromExtensionToken(YandexMusicClient client) {
        return client.getCurrentTrackIdAsync()
                .thenCompose(client::getTrackInfoAsync)
                .join();
    }
}
