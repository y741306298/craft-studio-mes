package com.mes.application.command.graphics;

public final class RemoteImage extends RemotePng {
    private final int width,height;
    public RemoteImage(String taskId,String url,int width,int height){super(taskId,url);this.width=width;this.height=height;}
    public int getWidth(){return width;}
    public int getHeight(){return height;}
}
