package org.weasis.core.api.gui.util;


import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.weasis.core.api.Messages;
import org.weasis.core.api.util.StringUtil;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class FxUtil {

	public static double getFontHeight() {
		return getFontHeight(null);
	}

	public static double getFontHeight(Font font) {
		return getTextBounds("0", font).getHeight() + 1.5;
	}

	public static Bounds getTextBounds(String str, Font font) {
		final Text text = new Text(Objects.requireNonNull(str));
		if (font != null) {
			text.setFont(font);
		}
		return text.getBoundsInLocal();
	}

	public static Font getArialFont() {
		return Font.font("Arial", FontWeight.BOLD, FontPosture.REGULAR, 12);
	}

	public static void paintColorFontOutline(GraphicsContext gc, String str, double x, double y, Color color) {
		gc.setFill(color);
		gc.setStroke(Color.BLACK);
		gc.strokeText(str, x, y);
		gc.fillText(str, x, y);
	}

	public static void paintFontOutline(GraphicsContext gc, String str, double x, double y) {
		paintColorFontOutline(gc, str, x, y, Color.WHITE);
	}
	
    public static void openInDefaultBrowser(URL url) throws IOException, URISyntaxException {
        if (url != null) {
            if (AppProperties.OPERATING_SYSTEM.startsWith("linux")) { //$NON-NLS-1$
                    String cmd = String.format("xdg-open %s", url); //$NON-NLS-1$
                    Runtime.getRuntime().exec(cmd);

            } else if (Desktop.isDesktopSupported()) {
                final Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(url.toURI());
                }
            } else {
            	new Alert(Alert.AlertType.ERROR, Messages.getString("JMVUtils.browser") + StringUtil.COLON_AND_SPACE + url).showAndWait();
            }
        }
    }
}
