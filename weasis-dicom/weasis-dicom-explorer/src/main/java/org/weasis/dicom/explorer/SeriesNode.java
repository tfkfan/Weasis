package org.weasis.dicom.explorer;

import java.util.Objects;

import org.dcm4che3.data.Tag;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.SeriesThumbnail;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.explorer.wado.LoadSeries;

import javafx.beans.value.ObservableValueBase;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class SeriesNode extends VBox {
    private static final Font font = Font.font("Arial", FontWeight.NORMAL, FontPosture.REGULAR, 10);

    private final SeriesThumbnail thumbnail;

    public SeriesNode(SeriesThumbnail thumbnail) {
        this.thumbnail = Objects.requireNonNull(thumbnail);
        this.maxWidthProperty().bind(thumbnail.widthProperty());
        this.setStyle("-fx-padding: 3;"
            + "-fx-background-color: -fx-shadow-highlight-color,-fx-outer-border,-fx-inner-border,-fx-body-color;"
            + "-fx-background-insets: 0 0 -1 0, 0, 1, 2;" + "-fx-background-radius: 3px, 3px, 2px, 1px;");

        StackPane stack = new StackPane();
        stack.getChildren().add(thumbnail);
        stack.setAlignment(Pos.BOTTOM_RIGHT);
        this.getChildren().add(stack);
        
        Label l = new Label(TagD.getTagValue(thumbnail.getSeries(), Tag.SeriesDescription, String.class));
        l.setFont(font);
        l.maxWidthProperty().bind(thumbnail.widthProperty());
        ObservableValueBase<String> observable = new ObservableValueBase<String>() {

            @Override
            public String getValue() {
                return thumbnail.getSeries().toString();
            }

        };
        l.textProperty().bind(observable);
        this.getChildren().add(l);

        if (thumbnail.getSeries().getSeriesLoader() instanceof LoadSeries) {
            // In case series is downloaded or canceled
            LoadSeries loader = (LoadSeries) thumbnail.getSeries().getSeriesLoader();
            if (!loader.isDone() && loader.isWriteInCache()) {
                ProgressIndicator p = new ProgressIndicator();
                p.setMaxSize(50, 50);
                p.setStyle("-fx-background-color: #D3D3D3;");
                loader.setOnSucceeded(event -> p.setVisible(false));
                p.progressProperty().bind(loader.progressProperty());
                stack.getChildren().add(p);
            }
        }
    }

    public boolean isSeries(MediaSeriesGroup series) {
        return Objects.equals(thumbnail.getSeries(), series);
    }

    public SeriesThumbnail getThumbnail() {
        return thumbnail;
    }

    public MediaSeries<? extends MediaElement> getSeries() {
        return thumbnail.getSeries();
    }

}
