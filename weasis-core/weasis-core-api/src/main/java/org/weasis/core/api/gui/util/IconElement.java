package org.weasis.core.api.gui.util;

import javafx.scene.image.Image;

public class IconElement {
    public static final Image EMPTY_ICON = new Image(IconElement.class.getResourceAsStream("/icon/transparent.png"));

    private final String name;
    private final Image icon;
    private String command;

    public IconElement(String name, Image icon) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.name = name;
        this.icon = icon == null ? EMPTY_ICON : icon;
    }

    public Image getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public double getIconWidth() {
        return icon.getRequestedHeight();
    }

    public double getIconHeight() {
        return icon.getRequestedWidth();
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
