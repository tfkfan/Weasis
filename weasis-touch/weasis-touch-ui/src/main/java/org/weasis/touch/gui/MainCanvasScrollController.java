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

public class MainCanvasScrollController {
    private MainCanvas mainCanvas;

    public MainCanvasScrollController(MainCanvas mainCanvas) {
        this.mainCanvas = mainCanvas;
    }

    /*****************************************************************
     * SCROLL (DICOM) *
     *****************************************************************/
    public void setScroll(int value) {
        mainCanvas.scroll(value);
    }
}
