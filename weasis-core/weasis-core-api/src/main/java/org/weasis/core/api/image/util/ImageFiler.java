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
package org.weasis.core.api.image.util;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;

import org.weasis.opencv.op.ImageConversion;

/**
 * The Class ImageFiler.
 *
 * @author Nicolas Roduit
 */
public class ImageFiler {

    public static final int TILESIZE = 512;

    private ImageFiler() {
    }

    public static RenderedImage getReadableImage(RenderedImage source) {
        if (source != null && source.getSampleModel() != null) {
            int numBands = source.getSampleModel().getNumBands();
            if (ImageConversion.isBinary(source.getSampleModel())) {
                return ImageConversion.convertTo(source, BufferedImage.TYPE_BYTE_GRAY);
            }

            if (source.getColorModel() instanceof IndexColorModel || numBands > 3
                || (source.getSampleModel() instanceof BandedSampleModel && numBands > 1)) {
                return ImageConversion.convertTo(source, BufferedImage.TYPE_3BYTE_BGR);
            }
        }
        return source;
    }

    public static String changeExtension(String filename, String ext) {
        if (filename == null) {
            return ""; //$NON-NLS-1$
        }
        // replace extension after the last point
        int pointPos = filename.lastIndexOf("."); //$NON-NLS-1$
        if (pointPos == -1) {
            pointPos = filename.length();
        }
        return filename.substring(0, pointPos) + ext;
    }

}
