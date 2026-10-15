package com.mes.application.command.delivery;

/** JST 身份校验或互斥操作期间，暂停普通打印。 */
public class JstPrintUnavailableException extends RuntimeException {
    public JstPrintUnavailableException(String message) {
        super(message);
    }
}
