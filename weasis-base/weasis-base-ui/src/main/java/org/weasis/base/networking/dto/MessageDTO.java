package org.weasis.base.networking.dto;

import java.io.Serializable;

public class MessageDTO implements Serializable {
    private String path;
    private String layout;
    private String synchronise;
    private String scroll;

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

    public String getSynchronise() {
        return synchronise;
    }

    public void setSynchronise(String synchronise) {
        this.synchronise = synchronise;
    }

    public String getScroll() {
        return scroll;
    }

    public void setScroll(String scroll) {
        this.scroll = scroll;
    }
}
