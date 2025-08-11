package top.jgroup.exceptions;

public class YandexMusicException extends RuntimeException {
    public YandexMusicException(String message) {
        super(message);
    }

    public YandexMusicException(String message, Throwable cause) {
        super(message, cause);
    }
}

