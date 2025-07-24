import top.jgroup.YandexMusicClient;
import top.jgroup.model.TrackInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {

        Properties props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        }
        String oathToken = props.getProperty("YANDEX_MUSIC_TOKEN_OAUTH");
        String extensionToken = props.getProperty("YANDEX_MUSIC_TOKEN_EXTENSION");

        YandexMusicClient oauthClient = new YandexMusicClient(oathToken, true);


        try {
            TrackInfo oauthTrack = getTrackFromOAuthToken(oauthClient);
            System.out.println("Трек из OAuth токена: " + oauthTrack);
        } catch (Exception e) {
            System.out.println("Ошибка получения трека с OAuth токена: " + e.getMessage());
        }

        YandexMusicClient extensionClient = new YandexMusicClient();
        extensionClient.setToken(extensionToken);

        try {
            TrackInfo extensionTrack = getTrackFromExtensionToken(extensionClient);
            System.out.println("Трек из Extension токена: " + extensionTrack);
        } catch (Exception e) {
            System.out.println("Ошибка получения трека с Extension токена: " + e.getMessage());
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
