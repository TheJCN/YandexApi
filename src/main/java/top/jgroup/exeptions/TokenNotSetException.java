package top.jgroup.exeptions;

public class TokenNotSetException extends RuntimeException {
    public TokenNotSetException(String message) {
        super(message);
    }
}
