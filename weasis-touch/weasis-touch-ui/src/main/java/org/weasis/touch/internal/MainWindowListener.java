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
package org.weasis.touch.internal;

import java.beans.PropertyChangeListener;

import org.weasis.dicom.explorer.DicomModel;
import org.weasis.touch.gui.WeasisWin;

public interface MainWindowListener extends PropertyChangeListener {

    void setMainWindow(WeasisWin mainWindow);

    WeasisWin getMainWindow();

    DicomModel getModel();
}
