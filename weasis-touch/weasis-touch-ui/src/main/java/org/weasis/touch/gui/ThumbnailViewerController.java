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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.dcm4che3.data.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.SeriesThumbnail;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.media.data.Thumbnail;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.DicomSorter;
import org.weasis.dicom.explorer.SeriesNode;
import org.weasis.dicom.explorer.StudyNode;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class ThumbnailViewerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailViewerController.class);

    private final BooleanProperty lockedProperty = new SimpleBooleanProperty(false);

    private final HashMap<MediaSeriesGroup, PatientController> patient2ptCotroller = new HashMap<>();
    private final HashMap<MediaSeriesGroup, List<StudyNode>> patient2study = new HashMap<>();
    private final HashMap<MediaSeriesGroup, List<SeriesNode>> study2series = new HashMap<>();

    @FXML
    VBox thumbnailContener;
    @FXML
    ScrollPane thumbnailViewer;

    @FXML
    private void initialize() {
        thumbnailViewer.disableProperty().bind(lockedProperty);
    }

    public void setParam(BooleanProperty lockedProperty, Scene scene) {

        this.lockedProperty.bindBidirectional(lockedProperty);

        thumbnailViewer.prefHeightProperty().bind(scene.heightProperty());
        thumbnailContener.prefWidthProperty().bind(thumbnailViewer.widthProperty().subtract(25));

        AnchorPane.setTopAnchor(thumbnailViewer, 0.0);
        AnchorPane.setBottomAnchor(thumbnailViewer, 0.0);
        AnchorPane.setLeftAnchor(thumbnailViewer, 0.0);
        AnchorPane.setRightAnchor(thumbnailViewer, 0.0);
    }

    public void removeAll() {
        thumbnailContener.getChildren().removeAll(thumbnailContener.getChildren());
        thumbnailContener.requestLayout();
    }

    public void removePatientPane(MediaSeriesGroup patient) {
        PatientController p = patient2ptCotroller.remove(patient);
        if (p != null) {
            List<StudyNode> studies = patient2study.remove(patient);
            if (studies != null) {
                for (StudyNode studyPane : studies) {
                    study2series.remove(studyPane.getStudy());
                }
            }
            thumbnailContener.getChildren().remove(p.getPatientGroup());
            thumbnailContener.requestLayout();
        }
    }

    public PatientController getPatientPane(MediaSeriesGroup patient) {
        return patient2ptCotroller.get(patient);
    }

    private StudyNode getStudyPane(MediaSeriesGroup study, DicomModel model) {
        List<StudyNode> studies = patient2study.get(model.getParent(study, DicomModel.patient));
        if (studies != null) {
            for (int i = 0; i < studies.size(); i++) {
                StudyNode st = studies.get(i);
                if (st.isStudy(study)) {
                    return st;
                }
            }
        }
        return null;
    }

    void removeStudy(MediaSeriesGroup study, DicomModel model) {
        MediaSeriesGroup patient = model.getParent(study, DicomModel.patient);
        List<StudyNode> studies = patient2study.get(patient);
        if (studies != null) {
            for (int i = 0; i < studies.size(); i++) {
                StudyNode st = studies.get(i);
                if (st.isStudy(study)) {
                    studies.remove(i);
                    if (studies.isEmpty()) {
                        patient2study.remove(patient);
                        // throw a new event for removing the patient
                        model.removePatient(patient);
                        break;
                    }
                    study2series.remove(study);
                    return;
                }
            }
        }
        study2series.remove(study);

    }

    private SeriesNode getSeriesPane(MediaSeriesGroup series, MediaSeriesGroup study) {
        List<SeriesNode> seriesList = study2series.get(study);
        if (seriesList != null) {
            for (int j = 0; j < seriesList.size(); j++) {
                SeriesNode se = seriesList.get(j);
                if (se.isSeries(series)) {
                    return se;
                }
            }
        }
        return null;
    }

    void removeSeries(MediaSeriesGroup series, DicomModel model) {
        MediaSeriesGroup study = model.getParent(series, DicomModel.study);
        List<SeriesNode> seriesList = study2series.get(study);
        if (seriesList != null) {
            for (int j = 0; j < seriesList.size(); j++) {
                SeriesNode se = seriesList.get(j);
                if (se.isSeries(series)) {
                    SeriesNode s = seriesList.remove(j);
                    StudyNode st = getStudyPane(study, model);
                    if (st != null) {
                        st.removeThumbnail(s);
                    }
                    if (seriesList.isEmpty()) {
                        study2series.remove(study);
                        // throw a new event for removing the patient
                        model.removeStudy(study);
                    }
                    return;
                }
            }
        }
    }

    public void addDicomSeries(Series<?> series, DicomModel model) {
        MediaSeriesGroup study = model.getParent(series, DicomModel.study);
        MediaSeriesGroup patient = model.getParent(study, DicomModel.patient);
        PatientController p = createPatientPaneInstance(patient, model);
        if (p != null) {
            if (!DicomModel.isSpecialModality(series)) {
                StudyNode studyNode = createStudyPaneInstance(study, model, p);
                createSeriesPaneInstance(series, model, studyNode);
            }
            
            SeriesThumbnail thumb = (SeriesThumbnail) series.getTagValue(TagW.Thumbnail);
            if (thumb != null) {
                thumb.repaint();
            }

            if (!patient2ptCotroller.values().stream().anyMatch(PatientController::isExpanded)) {
                p.open();
            }
        }
    }

    private List<Series> getSplitSeries(Series dcm, DicomModel model) {
        MediaSeriesGroup study = model.getParent(dcm, DicomModel.study);
        Object splitNb = dcm.getTagValue(TagW.SplitSeriesNumber);
        List<Series> list = new ArrayList<>();
        if (splitNb == null || study == null) {
            list.add(dcm);
            return list;
        }
        String uid = TagD.getTagValue(dcm, Tag.SeriesInstanceUID, String.class);
        if (uid != null) {
            for (MediaSeriesGroup group : model.getChildren(study)) {
                if (group instanceof Series) {
                    Series s = (Series) group;
                    if (uid.equals(TagD.getTagValue(s, Tag.SeriesInstanceUID))) {
                        list.add(s);
                    }
                }
            }
        }
        return list;
    }

    void updateSplitSeries(Series dcmSeries, DicomModel model) {
        MediaSeriesGroup study = model.getParent(dcmSeries, DicomModel.study);
        // In case the Series has been replaced (split number = -1) and removed
        if (study == null) {
            return;
        }

        MediaSeriesGroup patient = model.getParent(study, DicomModel.patient);
        PatientController p = createPatientPaneInstance(patient, model);

        StudyNode studyPane = createStudyPaneInstance(study, model, p);
        List<Series> list = getSplitSeries(dcmSeries, model);

        List<SeriesNode> seriesList = study2series.get(study);
        if (seriesList == null) {
            seriesList = new ArrayList<>();
            study2series.put(study, seriesList);
        }

        boolean repaintStudy = false;
        for (Series dicomSeries : list) {
            createSeriesPaneInstance(dicomSeries, model, studyPane);
            SeriesThumbnail thumb = (SeriesThumbnail) dicomSeries.getTagValue(TagW.Thumbnail);
            if (thumb != null) {
                thumb.reBuildThumbnail();
            }
        }

        Integer nb = (Integer) dcmSeries.getTagValue(TagW.SplitSeriesNumber);
        // Convention -> split number inferior to 0 is a Series that has been replaced (ex. when a DicomSeries is
        // converted DicomVideoSeries after downloading files).
        if (nb != null && nb < 0) {
            model.removeSeries(dcmSeries);
            repaintStudy = true;
        }
        if (repaintStudy) {
            for (int i = 0, k = 1; i < seriesList.size(); i++) {
                SeriesNode s = seriesList.get(i);
                MediaSeries<?> seq = s.getThumbnail().getSeries();
                if (list.contains(seq)) {
                    seq.setTag(TagW.SplitSeriesNumber, k);
                    k++;
                }
            }
        } else {
            int k = 1;
            for (SeriesNode s : seriesList) {
                MediaSeries<?> seq = s.getThumbnail().getSeries();
                if (list.contains(seq)) {
                    seq.setTag(TagW.SplitSeriesNumber, k);
                    k++;
                }
            }
        }

    }

    private PatientController createPatientPaneInstance(MediaSeriesGroup patient, DicomModel model) {
        PatientController p = getPatientPane(patient);
        try {
            if (p == null) {
                FXMLLoader loaderPatient = new FXMLLoader();
                loaderPatient.setLocation(this.getClass().getResource("PatientView.fxml"));
                loaderPatient.setClassLoader(this.getClass().getClassLoader());
                Group patientGroup = loaderPatient.load();
                p = loaderPatient.getController();
                p.setPatient(patient);
                p.setAccordeonMode(true);
                p.setLockProperty(lockedProperty);
                p.setParent(thumbnailContener);
                patient2ptCotroller.put(patient, p);
                thumbnailContener.getChildren().add(patientGroup);
            }
        } catch (Exception e) {
            LOGGER.error("Adding Series to thumnail viewer", e);
        }
        return p;
    }

    private StudyNode createStudyPaneInstance(MediaSeriesGroup study, DicomModel model, PatientController p) {
        StudyNode studyPane = getStudyPane(study, model);
        if (studyPane == null) {
            studyPane = new StudyNode(study);
            MediaSeriesGroup patient = model.getParent(study, DicomModel.patient);
            List<StudyNode> studies = patient2study.get(patient);
            if (studies == null) {
                studies = new ArrayList<>();
                studies.add(studyPane);
                patient2study.put(patient, studies);
                p.addstudyPane(studyPane);
            } else {
                int index = Collections.binarySearch(studies, studyPane, DicomSorter.STUDY_COMPARATOR);
                if (index < 0) {
                    index = -(index + 1);
                } else {
                    index = studies.size();
                }
                studies.add(index, studyPane);
                p.addstudyPane(index, studyPane);
            }
        }
        return studyPane;
    }

    private synchronized SeriesNode createSeriesPaneInstance(Series<?> series, DicomModel model, StudyNode studyNode) {
        MediaSeriesGroup study = model.getParent(series, DicomModel.study);
        SeriesNode seriesPane = getSeriesPane(series, study);
        if (seriesPane == null) {
            SeriesThumbnail thumbnail = (SeriesThumbnail) series.getTagValue(TagW.Thumbnail);
            if (thumbnail == null) {
                thumbnail = DicomModel.createThumbnail(series, model, Thumbnail.DEFAULT_SIZE);
                series.setTag(TagW.Thumbnail, thumbnail);
            }

            seriesPane = new SeriesNode(thumbnail);
            thumbnail.setLockedProperty(lockedProperty);
            List<SeriesNode> seriesList = study2series.get(study);
            if (seriesList == null) {
                seriesList = new ArrayList<>();
                study2series.put(study, seriesList);
                seriesList.add(seriesPane);
                studyNode.addThumbnail(seriesPane);
            } else {
                int index = Collections.binarySearch(seriesList, seriesPane, DicomSorter.SERIES_COMPARATOR);
                if (index < 0) {
                    index = -(index + 1);
                } else {
                    index = seriesList.size();
                }
                seriesList.add(index, seriesPane);
                studyNode.addThumbnail(index, seriesPane);
            }
        }
        return seriesPane;
    }

    public boolean isSelectedPatient(MediaSeriesGroup patient) {
        PatientController p = getPatientPane(patient);
        if (p != null) {
            return p.isExpanded();
        }
        return false;
    }

    public void setSelectedPatient(MediaSeriesGroup patient) {
        PatientController p = getPatientPane(patient);
        if (p != null) {
            p.setExpanded(true);
        }
    }
}
