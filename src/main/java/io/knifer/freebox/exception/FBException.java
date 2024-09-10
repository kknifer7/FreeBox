package io.knifer.freebox.exception;

/**
 * 通用异常
 *
 * @author Knifer
 */
public class FBException extends RuntimeException {

    public FBException(String message) {
        super(message);
    }

    public FBException(String message, Throwable cause) {
        super(message, cause);
    }
}
