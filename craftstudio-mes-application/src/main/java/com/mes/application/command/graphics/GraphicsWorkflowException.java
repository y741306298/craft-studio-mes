package com.mes.application.command.graphics;

public class GraphicsWorkflowException extends RuntimeException {
    private final int status;
    public GraphicsWorkflowException(int status,String message){super(message);this.status=status;}
    public int status(){return status;}
}
