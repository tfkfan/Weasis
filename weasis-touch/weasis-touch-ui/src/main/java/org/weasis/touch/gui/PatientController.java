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

import java.util.Objects;

import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.dicom.explorer.StudyNode;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PatientController {
    private BooleanProperty lockedProperty = new SimpleBooleanProperty(false);

    @FXML
    private TitledPane title;
    @FXML
    private Group patientGroup;
    @FXML
    private VBox patientImageVBox;

    private MediaSeriesGroup patient;
    private Boolean accordeonMode = false;

    @FXML
    private void initialize() {
        title.setExpanded(false);
        title.expandedProperty().addListener(this::accordeonListener);
    }

    public void setAccordeonMode(Boolean value) {
        accordeonMode = value;
    }

    private void accordeonListener(ObservableValue<? extends Boolean> o, Boolean oldVal, Boolean newVal) {
        if (accordeonMode && newVal) {
            VBox vb = (VBox) patientGroup.getParent();

            for (Object element : vb.getChildren()) {
                Group g = (Group) element;
                PatientController pc = (PatientController) g.getUserData();
                if (!pc.isPatient(patient)) {
                    pc.close();
                }
            }
        }
    }

    public void setLockProperty(BooleanProperty lockProperty) {
        this.lockedProperty.bindBidirectional(lockProperty);
    }

    public void addstudyPane(StudyNode studyPane) {
        patientImageVBox.getChildren().add(studyPane.getTitleNode());
    }

    public void addstudyPane(int index, StudyNode studyPane) {
        patientImageVBox.getChildren().add(index, studyPane.getTitleNode());
    }

    public void removestudyPane(StudyNode studyPane) {
        patientImageVBox.getChildren().remove(studyPane.getTitleNode());
    }

    public MediaSeriesGroup getPatient() {
        return patient;
    }

    public void setPatient(MediaSeriesGroup patient) {
        this.patient = Objects.requireNonNull(patient);
        title.setText(patient.toString());
        ObservableValueBase<String> observable = new ObservableValueBase<String>() {

            @Override
            public String getValue() {
                return patient.toString();
            }
        };
        title.textProperty().bind(observable);
    }

    public Group getPatientGroup() {
        return patientGroup;
    }

    public void setPatientGroup(Group patientGroup) {
        this.patientGroup = patientGroup;
    }

    public boolean isExpanded() {
        return title.isExpanded();
    }

    public void setExpanded(Boolean value) {
        title.setExpanded(value);
    }

    public void open() {
        title.setExpanded(true);
    }

    public void close() {
        title.setExpanded(false);
    }

    public void setParent(Node parent) {
        title.prefWidthProperty().bind(((Region) parent).widthProperty());
        patientImageVBox.prefWidthProperty().bind(((Region) parent).widthProperty());
    }

    public boolean isPatient(MediaSeriesGroup patient) {
        return Objects.equals(this.patient, patient);
    }
}
