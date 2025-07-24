package top.jgroup.exeptions;

public class OAuthTokenAbroadException extends RuntimeException {
    public OAuthTokenAbroadException(String message) {
        super(message);
    }
}
