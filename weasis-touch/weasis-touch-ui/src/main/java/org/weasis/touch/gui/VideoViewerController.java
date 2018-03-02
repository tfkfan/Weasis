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
package org.weasis.touch.gui;

import java.io.File;

import com.jfoenix.controls.JFXSlider;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

public class VideoViewerController {

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private BorderPane play;
    @FXML
    private BorderPane stop;
    @FXML
    private BorderPane preview;
    @FXML
    private BorderPane next;
    @FXML
    private MediaView mediaView;
    @FXML
    private JFXSlider timeSlider;
    @FXML
    private JFXSlider volumeSlider;
    @FXML
    private Label playTime;
    @FXML
    private SVGPath playSVG;
    @FXML
    private HBox controlPane1;
    @FXML
    private HBox controlPane2;
    
    BooleanProperty lockedProperty = new SimpleBooleanProperty(false);

    private MediaPlayer mp;
    private boolean stopRequested = false;
    private Duration duration;

    @FXML
    private void initialize() {
        play.setOnMouseClicked(this::playAction);
        stop.setOnMouseClicked(this::stopAction);
        next.setOnMouseClicked(this::nextAction);
        preview.setOnMouseClicked(this::previewAction);

        play.setOnMouseEntered(this::buttonMouseEntred);
        stop.setOnMouseEntered(this::buttonMouseEntred);
        next.setOnMouseEntered(this::buttonMouseEntred);
        preview.setOnMouseEntered(this::buttonMouseEntred);

        play.setOnMouseExited(this::buttonMouseExited);
        stop.setOnMouseExited(this::buttonMouseExited);
        next.setOnMouseExited(this::buttonMouseExited);
        preview.setOnMouseExited(this::buttonMouseExited);

        mainBorderPane.setMinHeight(0);
        mainBorderPane.setMinWidth(0);

        mediaView.fitWidthProperty().bind(mainBorderPane.widthProperty());
        mediaView.fitHeightProperty().bind(mainBorderPane.heightProperty().subtract(82));

        AnchorPane.setBottomAnchor(mainBorderPane, 0.0);
        AnchorPane.setTopAnchor(mainBorderPane, 0.0);
        AnchorPane.setLeftAnchor(mainBorderPane, 0.0);
        AnchorPane.setRightAnchor(mainBorderPane, 0.0);
        
        controlPane2.disableProperty().bind(lockedProperty);
        controlPane1.addEventFilter(Event.ANY,new EventHandler<Event>() {
            public void handle(final Event e) { 
                if(lockedProperty.get()) {
                    e.consume();
                }
             }
        });

    }

    private void buttonMouseEntred(MouseEvent event) {
        SVGPath svgPath = (SVGPath) ((BorderPane) event.getSource()).getCenter();
        svgPath.setScaleX(svgPath.getScaleX() + 0.2);
        svgPath.setScaleY(svgPath.getScaleY() + 0.2);
        event.consume();
    }

    private void buttonMouseExited(MouseEvent event) {
        SVGPath svgPath = (SVGPath) ((BorderPane) event.getSource()).getCenter();
        svgPath.setScaleX(svgPath.getScaleX() - 0.2);
        svgPath.setScaleY(svgPath.getScaleY() - 0.2);
        event.consume();
    }

    public void stop() {
        mp.stop();
    }

    public void setFile(File file) {
        Media media = new Media(file.toURI().toString());
        mp = new MediaPlayer(media);
        mp.setAutoPlay(true);

        mediaView.setMediaPlayer(mp);

        ReadOnlyObjectProperty<Duration> time = mp.currentTimeProperty();
        time.addListener(
            (ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> updateValues());

        mp.setOnStopped(() -> {
            playSVG.setContent(
                "M728,544 C732.418278,544 736,540.418278 736,536 C736,531.581722 732.418278,528 728,528 C723.581722,528 720,531.581722 720,536 C720,540.418278 723.581722,544 728,544 L728,544 Z M732,536 L725,540 L725,532 L732,536 L732,536 Z M732,536");
        });

        mp.setOnPlaying(() -> {
            if (stopRequested) {
                mp.pause();
                stopRequested = false;
            }
        });
        mp.setOnReady(() -> {
            duration = mp.getMedia().getDuration();
            updateValues();
        });
        mp.setOnEndOfMedia(() -> {
            mp.stop();
            mp.seek(Duration.ZERO);
            updateValues();
        });

        final DoubleProperty value = timeSlider.valueProperty();
        value.addListener((ObservableValue<? extends Number> observable, Number old, Number now) -> {
            if (timeSlider.isValueChanging()) {
                // multiply duration by percentage calculated by slider position
                if (duration != null) {
                    mp.seek(duration.multiply(timeSlider.getValue() / 100.0));
                }
                updateValues();
            } else if (Math.abs(now.doubleValue() - old.doubleValue()) > 1.5) {
                // multiply duration by percentage calculated by slider position
                if (duration != null) {
                    mp.seek(duration.multiply(timeSlider.getValue() / 100.0));
                }
            }
        });

        volumeSlider.valueProperty().addListener((Observable ov) -> {
        });
        volumeSlider.valueProperty()
            .addListener((ObservableValue<? extends Number> observable, Number old, Number now) -> {
                mp.setVolume(volumeSlider.getValue() / 100.0);
            });
    }

    private void previewAction(Event e) {
        Duration currentTime = mp.getCurrentTime();
        mp.seek(Duration.seconds(currentTime.toSeconds() - 5.0));
        updateValues();
    }

    private void stopAction(Event e) {
        mp.stop();
    }

    private void playAction(Event e) {
        if (mp.getStatus() == Status.PLAYING) {
            mp.pause();
            playSVG.setContent(
                "M728,544 C732.418278,544 736,540.418278 736,536 C736,531.581722 732.418278,528 728,528 C723.581722,528 720,531.581722 720,536 C720,540.418278 723.581722,544 728,544 L728,544 Z M732,536 L725,540 L725,532 L732,536 L732,536 Z M732,536");
        } else {
            mp.play();
            playSVG.setContent(
                "M296,544 C291.581722,544 288,540.418278 288,536 C288,531.581722 291.581722,528 296,528 C300.418278,528 304,531.581722 304,536 C304,540.418278 300.418278,544 296,544 L296,544 Z M299,532 L297,532 L297,536 L297,540 L299,540 L299,532 L299,532 Z M295,532 L293,532 L293,536 L293,540 L295,540 L295,532 L295,532 Z M295,532");
        }
    }

    private void nextAction(Event e) {
        if (mp.getStatus() != Status.STOPPED) {
            Duration currentTime = mp.getCurrentTime();
            mp.seek(Duration.seconds(currentTime.toSeconds() + 5.0));

        } else {
            mp.pause();
            mp.seek(Duration.seconds(5.0));
        }
        updateValues();
    }

    protected void updateValues() {
        if (playTime != null && timeSlider != null && volumeSlider != null && duration != null) {
            Platform.runLater(() -> {
                Duration currentTime = mp.getCurrentTime();
                playTime.setText(formatTime(currentTime, duration));
                timeSlider.setDisable(duration.isUnknown());
                if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !timeSlider.isValueChanging()) {
                    double relativeTime = currentTime.divide(duration.toMillis()).toMillis() * 100.0;
                    timeSlider.setValue(relativeTime);
                }
                if (!volumeSlider.isValueChanging()) {
                    int relativeVolume = (int) Math.round(mp.getVolume() * 100);
                    volumeSlider.setValue(relativeVolume);
                }
            });
        }
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;

            if (durationHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
            }
        }
    }
}
