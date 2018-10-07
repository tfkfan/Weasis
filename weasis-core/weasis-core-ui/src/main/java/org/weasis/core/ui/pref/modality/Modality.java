package org.weasis.core.ui.pref.modality;

public enum Modality {
    CT("CT"), US("US"), DX("DX"), CR("CR"), DEFAULT("Default Settings");

    private String title;
    Modality(String title){
        setTitle(title);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
