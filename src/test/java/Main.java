import top.jgroup.YandexMusicClient;

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
        String token = props.getProperty("YANDEX_MUSIC_TOKEN");

        YandexMusicClient client = new YandexMusicClient();
        client.setToken(token);
        client.getCurrentTrackIdAsync().thenAccept(trackId -> {
            System.out.println("Current Track ID: " + trackId);
            client.getTrackInfoAsync(trackId).thenAccept(trackInfo -> {
                System.out.println("Track Title: " + trackInfo.title());
                System.out.println("Artist: " + trackInfo.artist());
                System.out.println("Duration (ms): " + trackInfo.durationMs());
                System.out.println("Cover URL: " + trackInfo.coverUrl());
            }).exceptionally(ex -> {
                System.err.println("Error fetching track info: " + ex.getMessage());
                return null;
            });
            client.getTrackRawInfoAsync(trackId).thenAccept(trackNode -> {
                System.out.println("Raw Track Info: " + trackNode.toPrettyString());
            }).exceptionally(ex -> {
                System.err.println("Error fetching raw track info: " + ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            System.err.println("Error fetching current track ID: " + ex.getMessage());
            return null;
        });
    }
}
