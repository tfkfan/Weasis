package org.weasis.base.networking.dto;

import java.io.Serializable;

public class MessageDTO implements Serializable {
    private String path;
    private String layout;

    public MessageDTO(){

    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
