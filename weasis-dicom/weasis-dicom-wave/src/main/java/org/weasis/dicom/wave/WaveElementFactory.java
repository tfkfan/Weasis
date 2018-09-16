/*******************************************************************************
 * Copyright (c) 2009-2018 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.dicom.wave;

import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSpecialElement;
import org.weasis.dicom.codec.DicomSpecialElementFactory;

@org.osgi.service.component.annotations.Component(service = DicomSpecialElementFactory.class, immediate = false)
public class WaveElementFactory implements DicomSpecialElementFactory {

    public static final String SERIES_WAVEFORM_MIMETYPE = "wf/dicom"; //$NON-NLS-1$

    private static final String[] modalities = { "ECG", "HD" }; //$NON-NLS-1$ //$NON-NLS-2$

    @Override
    public String getSeriesMimeType() {
        return SERIES_WAVEFORM_MIMETYPE;
    }

    @Override
    public String[] getModalities() {
        return modalities;
    }

    @Override
    public DicomSpecialElement buildDicomSpecialElement(DicomMediaIO mediaIO) {
        return new DicomSpecialElement(mediaIO);
    }
}
