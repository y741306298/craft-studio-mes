package com.mes.application.command.graphics;

/** Reference to an image produced by imgUtil; never contains a bitmap or PNG bytes. */
public class RemotePng {
    private final String taskId, url;
    public RemotePng(String taskId, String url) { this.taskId=taskId;this.url=url; }
    public String taskId(){return taskId;}
    public String url(){return url;}
}
