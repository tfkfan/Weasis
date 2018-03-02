package org.weasis.dicom.explorer;

import java.time.LocalDateTime;
import java.util.Objects;

import org.controlsfx.tools.Borders;
import org.dcm4che3.data.Tag;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.TagUtil;
import org.weasis.dicom.codec.TagD;

import javafx.scene.Node;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;

public class StudyNode extends TilePane {
    private final MediaSeriesGroup study;
    private final Node titleNode;

    public StudyNode(MediaSeriesGroup study) {
        this.study = Objects.requireNonNull(study);
        String title;
        LocalDateTime studyDate = TagD.dateTime(Tag.StudyDate, Tag.StudyTime, study);
        if (studyDate == null) {
            title = TagD.getTagValue(study, Tag.StudyDescription, String.class);
        } else {
            title = TagUtil.formatDateTime(studyDate);
        }
        this.titleNode = Borders.wrap(this).lineBorder().color(Color.web("#b34711")).thickness(2).title(title).outerPadding(7).innerPadding(7).buildAll();
    }

    public MediaSeriesGroup getStudy() {
        return study;
    }

    public boolean isStudy(MediaSeriesGroup study) {
        return Objects.equals(this.study, study);
    }

    public void addThumbnail(SeriesNode thumbnail) {
        getChildren().add(thumbnail);
    }

    public void addThumbnail(int index, SeriesNode thumbnail) {
        getChildren().add(index, thumbnail);
    }

    public void removeThumbnail(SeriesNode thumbnail) {
        getChildren().remove(thumbnail);
    }

    public Node getTitleNode() {
        return titleNode;
    }
}
