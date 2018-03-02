/*******************************************************************************
 * Copyright (c) 2009-2018 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Gérôme Pasquier - initial API and implementation
 *     Nicolas Roduit - implementation
 *******************************************************************************/
//proto de weasis tactile avec canvas
package org.weasis.touch.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.SeriesThumbnail;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.ui.editor.MimeSystemAppViewer;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSpecialElement;
import org.weasis.dicom.codec.FilesExtractor;
import org.weasis.touch.Messages;
import org.weasis.touch.WeasisPreferences;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class MainViewController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewController.class);

    public static final MimeSystemAppViewer mimeSystemViewer = new MimeSystemAppViewer() {

        @Override
        public String getPluginName() {
            return "Default System Application";
        }

        @Override
        public void addSeries(MediaSeries series) {
            if (series instanceof FilesExtractor) {
                // As SUN JRE supports only Gnome and responds "true" for Desktop.isDesktopSupported()
                // in KDE session, but actually does not support it.
                // http://bugs.sun.com/view_bug.do?bug_id=6486393
                FilesExtractor extractor = (FilesExtractor) series;
                for (File file : extractor.getExtractFiles()) {
                    if (AppProperties.OPERATING_SYSTEM.startsWith("linux")) { //$NON-NLS-1$
                        startAssociatedProgramFromLinux(file);
                    } else if (AppProperties.OPERATING_SYSTEM.startsWith("win")) { //$NON-NLS-1$
                        // Workaround of the bug with mpg file see http://bugs.sun.com/view_bug.do?bug_id=6599987
                        startAssociatedProgramFromWinCMD(file);
                    } else if (Desktop.isDesktopSupported()) {
                        final Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.OPEN)) {
                            startAssociatedProgramFromDesktop(desktop, file);
                        }
                    }
                }
            }
        }

        @Override
        public String getDockableUID() {
            return null;
        }
    };

    private Scene scene;
    public BooleanProperty lockedProperty = new SimpleBooleanProperty(false);
    public BooleanProperty blurProperty = new SimpleBooleanProperty(false);

    ThumbnailViewerController thumbnailViewerController;
    private ScrollPane sp;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private StackPane mainPane;
    @FXML
    private BorderPane closeThumbnail;
    @FXML
    private AnchorPane mainViewPane;
    @FXML
    private AnchorPane left;
    @FXML
    private AnchorPane right;

    private Preferences prefs;
    private MenuController menu;
    public DICOMViewer dicomViewer;
    private FXMLLoader load;
    private Node node;
    private AudioListenerController audioListenerController = null;
    private VideoViewerController videoViewerController = null;

    private Timeline t;

    @FXML
    private void initialize() {

        LOGGER.info("initialize mainView");

        prefs = Preferences.userRoot().node(SettingsController.class.getName());

        closeThumbnail.setOnMouseClicked(this::handleOnClickedCloseThumbnail);
        closeThumbnail.setOnTouchReleased(this::handleOnReleasedCloseThumbnail);

        initHideShow();

        blurProperty.addListener((ChangeListener<Boolean>) (o, oldVal, newVal) -> {
            if (newVal) {
                mainViewPane.setEffect(new GaussianBlur());
            } else {
                mainViewPane.setEffect(null);
            }
        });

        closeThumbnail.setOnMouseEntered(e -> {
            if (!e.isSynthesized() && !lockedProperty.get()) {
                closeThumbnail.setScaleX(closeThumbnail.getScaleX() + 0.1);
                closeThumbnail.setScaleY(closeThumbnail.getScaleY() + 0.1);
            }
            e.consume();
        });
        closeThumbnail.setOnMouseExited(e -> {
            if (!e.isSynthesized() && !lockedProperty.get()) {
                closeThumbnail.setScaleX(closeThumbnail.getScaleX() - 0.1);
                closeThumbnail.setScaleY(closeThumbnail.getScaleY() - 0.1);
            }
            e.consume();
        });
        left.setPrefWidth(240);
        left.setMinWidth(240);

        this.right.setOnDragOver(this::handleOnDragOver);
        this.right.setOnDragEntered(this::handleOnDragEntered);
        this.right.setOnDragExited(this::handleOnDragExited);
        this.right.setOnDragDropped(this::handleOnDragDropped);

        KeyValue kv = new KeyValue(mainSplitPane.getDividers().get(0).positionProperty(), 0.79);
        KeyFrame kf = new KeyFrame(Duration.millis(1), kv);
        t = new Timeline();
        t.getKeyFrames().add(kf);
    }

    public void setParam(MenuController menu) {

        this.menu = menu;
        scene = this.closeThumbnail.getScene();
        dicomViewer = new DICOMViewer(this, this.menu);
        this.dragExitTime.bindBidirectional(dicomViewer.mainCanvas.canvasTouchController.dragExitTime);

        TutorialCanvas tutorialCanvas = new TutorialCanvas();
        tutorialCanvas.setParam(this.menu);

        try {
            FXMLLoader loaderth = new FXMLLoader();
            loaderth.setLocation(this.getClass().getResource("ThumbnailViewer.fxml"));
            loaderth.setClassLoader(this.getClass().getClassLoader());
            sp = loaderth.load();
            thumbnailViewerController = loaderth.getController();

            left.getChildren().add(sp);
            right.getChildren().add(tutorialCanvas);

            thumbnailViewerController.setParam(lockedProperty, scene);
        } catch (IOException e) {
            LOGGER.error("Building thumbnail viewer", e);
        }

        mainViewPane.prefHeightProperty().bind(scene.heightProperty());
        mainViewPane.prefWidthProperty().bind(scene.widthProperty());

        left.prefHeightProperty().bind(scene.heightProperty());
        mainPane.prefHeightProperty().bind(scene.heightProperty());

        right.widthProperty().addListener((ChangeListener<Object>) (o, oldVal, newVal) -> {
            closeThumbnail.setLayoutX(right.localToScene(0, 0).getX() <= 0 ? 10 : right.localToScene(0, 0).getX());
        });
        // left.maxWidthProperty().bind(mainSplitPane.widthProperty().multiply(0.8));
        mainSplitPane.getDividers().get(0).positionProperty()
            .addListener((ChangeListener<Object>) (o, oldVal, newVal) -> {
                if ((double) newVal > 0.8 && left.getWidth() > 240) {
                    t.play();
                }
            });
    }

    /*****************************************************************
     * HIDE SHOW THUMBNAIL*
     *****************************************************************/
    private Boolean flagThumbnailShow = true;

    private static final Duration HIDE_THUMBNAIL_TIME = Duration.millis(250);
    private ParallelTransition pt;

    private Timeline timeline;

    private RotateTransition rotateTransition;

    private void initHideShow() {
        pt = new ParallelTransition();
        rotateTransition = new RotateTransition(HIDE_THUMBNAIL_TIME, closeThumbnail);
        timeline = new Timeline();
        pt.getChildren().addAll(timeline, rotateTransition);
        pt.setOnFinished(this::finishHideShowPellicule);
    }

    public void hidePellicule() {
        if (flagThumbnailShow) {
            this.hideShowPellicule();
        }
    }

    private void handleOnReleasedCloseThumbnail(TouchEvent event) {
        hideShowPellicule();
        event.consume();
    }

    private void handleOnClickedCloseThumbnail(MouseEvent event) {
        if (!event.isSynthesized()) {
            hideShowPellicule();
        }
        event.consume();
    }

    double position;

    private void hideShowPellicule() {
        if (!lockedProperty.getValue()) {
            KeyFrame kf;
            if (flagThumbnailShow) {
                rotateTransition.setFromAngle(0);
                rotateTransition.setToAngle(180);
                position = mainSplitPane.getWidth() * mainSplitPane.getDividers().get(0).getPosition();
                KeyValue kv = new KeyValue(mainSplitPane.getDividers().get(0).positionProperty(), 0);
                kf = new KeyFrame(HIDE_THUMBNAIL_TIME, kv);
                left.setMinWidth(0);
            } else {
                rotateTransition.setFromAngle(180);
                rotateTransition.setToAngle(0);
                Double div = position / mainSplitPane.getWidth() <= 0.79 ? position / mainSplitPane.getWidth() : 0.79;
                KeyValue kv = new KeyValue(mainSplitPane.getDividers().get(0).positionProperty(), div);
                kf = new KeyFrame(HIDE_THUMBNAIL_TIME, kv);
            }
            timeline.getKeyFrames().clear();
            timeline.getKeyFrames().add(kf);
            left.prefWidthProperty().unbind();
            pt.play();
        }
    }

    private void finishHideShowPellicule(ActionEvent event) {
        Node divider = mainSplitPane.lookup(".split-pane-divider");

        if (!flagThumbnailShow) {
            left.setMinWidth(240);
            if (divider != null) {
                divider.setStyle("-fx-padding: 3;");
            }
        } else {
            if (divider != null) {
                divider.setStyle("-fx-padding: 0;");
            }
        }
        left.setDisable(flagThumbnailShow);
        flagThumbnailShow = !flagThumbnailShow;
        event.consume();
    }

    /*****************************************************************
     * DROP *
     *****************************************************************/
    private final LongProperty dragExitTime = new SimpleLongProperty(0);

    private void handleOnDragOver(DragEvent event) {
        /* data is dragged over the target */
        event.acceptTransferModes(TransferMode.COPY);
        event.consume();
    }

    private void handleOnDragEntered(DragEvent event) {
        /* the drag-and-drop gesture entered the target */
        LOGGER.trace("onDragEntered");
        this.dicomViewer.mainCanvas.setOnSwipeRight(null);
        event.consume();
    }

    private void handleOnDragExited(DragEvent event) {
        /* mouse moved away, remove the graphical cues */
        event.consume();

        this.dicomViewer.mainCanvas
            .setOnSwipeRight(this.dicomViewer.mainCanvas.canvasTouchController::handleSwipeRight);
        dragExitTime.set(System.currentTimeMillis());

    }

    private AudioInputStream getAudioInputStream(DicomSpecialElement media) {
        DicomMediaIO dicomImageLoader = media.getMediaReader();
        Attributes attributes = dicomImageLoader.getDicomObject().getNestedDataset(Tag.WaveformSequence);
        if (attributes != null) {
            VR.Holder holder = new VR.Holder();
            Object data = attributes.getValue(Tag.WaveformData, holder);
            if (data instanceof BulkData) {
                BulkData bulkData = (BulkData) data;

                try {
                    int numChannels = attributes.getInt(Tag.NumberOfWaveformChannels, 0);
                    double sampleRate = attributes.getDouble(Tag.SamplingFrequency, 0.0);
                    int bitsPerSample = attributes.getInt(Tag.WaveformBitsAllocated, 0);
                    String spInterpretation = attributes.getString(Tag.WaveformSampleInterpretation, 0);

                    // http://dicom.nema.org/medical/dicom/current/output/chtml/part03/sect_C.10.9.html
                    // SB: signed 8 bit linear
                    // UB: unsigned 8 bit linear
                    // MB: 8 bit mu-law (in accordance with ITU-T Recommendation G.711)
                    // AB: 8 bit A-law (in accordance with ITU-T Recommendation G.711)
                    // SS: signed 16 bit linear
                    // US: unsigned 16 bit linear

                    AudioFormat audioFormat;
                    if ("MB".equals(spInterpretation) || "AB".equals(spInterpretation)) { //$NON-NLS-1$ //$NON-NLS-2$
                        int frameSize =
                            (numChannels == AudioSystem.NOT_SPECIFIED || bitsPerSample == AudioSystem.NOT_SPECIFIED)
                                ? AudioSystem.NOT_SPECIFIED : ((bitsPerSample + 7) / 8) * numChannels;
                        audioFormat = new AudioFormat("AB".equals(spInterpretation) ? Encoding.ALAW : Encoding.ULAW, //$NON-NLS-1$
                            (float) sampleRate, bitsPerSample, numChannels, frameSize, (float) sampleRate,
                            attributes.bigEndian());
                    } else {
                        boolean signed = "UB".equals(spInterpretation) || "US".equals(spInterpretation) ? false : true; //$NON-NLS-1$ //$NON-NLS-2$
                        audioFormat = new AudioFormat((float) sampleRate, bitsPerSample, numChannels, signed,
                            attributes.bigEndian());
                    }

                    return new AudioInputStream(bulkData.openStream(), audioFormat,
                        bulkData.length() / audioFormat.getFrameSize());
                } catch (Exception e) {
                    LOGGER.error("Get audio stream", e); //$NON-NLS-1$
                }
            }
        }
        return null;
    }

    private void handleOnDragDropped(DragEvent event) {
        /* data dropped */
        LOGGER.trace("onDragDropped");

        /*
         * let the source know whether the string was successfully transferred and used
         */
        Object source = event.getGestureSource();
        if (source instanceof SeriesThumbnail) {
            MediaSeries<? extends MediaElement> series = ((SeriesThumbnail) source).getSeries();
            String mimeType = series.getMimeType();

            if (mimeType.compareTo("au/dicom") == 0) {
                // audio listener
                try {
                    if (!right.getChildren().isEmpty()) {
                        right.getChildren().remove(0);
                        if (audioListenerController != null) {
                            audioListenerController.stop();
                        }
                        if (videoViewerController != null) {
                            videoViewerController.stop();
                        }
                    }

                    List<DicomSpecialElement> specialElements =
                        (List<DicomSpecialElement>) series.getTagValue(TagW.DicomSpecialElementList);
                    if (specialElements != null && !specialElements.isEmpty()) {
                        // Should have only one object by series (if more, they are split in several sub-series in
                        // dicomModel)
                        try (AudioInputStream audioStream = getAudioInputStream(specialElements.get(0))) {
                            if (audioStream != null) {
                                File audioFile = File.createTempFile("audio_", ".wav", AppProperties.FILE_CACHE_DIR); //$NON-NLS-1$ //$NON-NLS-2$

                                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);

                                load = new FXMLLoader();
                                load.setResources(Messages.RESOURCE_BUNDLE);
                                load.setLocation(this.getClass().getResource("AudioListenerView.fxml"));
                                load.setClassLoader(this.getClass().getClassLoader());
                                node = load.load();
                                audioListenerController = load.getController();
                                audioListenerController.setFile(audioFile);
                                audioListenerController.lockedProperty.bind(lockedProperty);
                                right.getChildren().add(node);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Reading audio file", e);
                    mimeSystemViewer.addSeries((MediaSeries<MediaElement>) series);
                }

            } else if (mimeType.compareTo("video/dicom") == 0) {
                // Video Viewer
                try {
                    if (!right.getChildren().isEmpty()) {
                        right.getChildren().remove(0);
                        if (audioListenerController != null) {
                            audioListenerController.stop();
                        }
                        if (videoViewerController != null) {
                            videoViewerController.stop();
                        }
                    }
                    FilesExtractor extractor = (FilesExtractor) series;
                    for (File file : extractor.getExtractFiles()) {
                        load = new FXMLLoader();
                        load.setResources(Messages.RESOURCE_BUNDLE);
                        load.setLocation(this.getClass().getResource("VideoViewerView.fxml"));
                        load.setClassLoader(this.getClass().getClassLoader());
                        node = load.load();
                        videoViewerController = load.getController();
                        videoViewerController.setFile(file);
                        videoViewerController.lockedProperty.bind(lockedProperty);
                        right.getChildren().add(node);
                    }
                } catch (Exception e) {
                    LOGGER.error("Reading video file", e);
                    mimeSystemViewer.addSeries((MediaSeries<MediaElement>) series);
                }

            } else if (DicomMediaIO.SERIES_VIDEO_MIMETYPE.equals(mimeType)
                || DicomMediaIO.SERIES_ENCAP_DOC_MIMETYPE.equals(mimeType)) {
                mimeSystemViewer.addSeries((MediaSeries<MediaElement>) series);
            } else {
                if (!right.getChildren().isEmpty()) {
                    right.getChildren().remove(0);
                    if (audioListenerController != null) {
                        audioListenerController.stop();
                    }
                    if (videoViewerController != null) {
                        videoViewerController.stop();
                    }
                }
                right.getChildren().add(dicomViewer);
                dicomViewer.mainCanvas.setSeries((MediaSeries<DicomImageElement>) series, null);
            }
            
            if (prefs.getBoolean(WeasisPreferences.HIDE_THUMBNAIL.name(),
                (Boolean) WeasisPreferences.HIDE_THUMBNAIL.defaultValue())) {
                this.hidePellicule();
            }
        }
        event.setDropCompleted(true);

        event.consume();
    }
}
