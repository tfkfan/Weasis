package org.weasis.dicom.explorer.utils;

import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.LoadLocalDicom;

import javax.swing.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class ImportUtils {
    public static void importDICOMLocal(DicomModel dicomModel, String path) throws URISyntaxException {
        File[] files = null;
        if (files == null) {
            if (path != null) {
                File file = new File(path);
                if (file.canRead()) {
                    files = new File[]{file};
                } else {
                    file = new File(new URI(path));
                    if (file.canRead()) {
                        files = new File[]{file};
                    }
                }
            }
        }
        if (files != null) {

            LoadLocalDicom dicom = new LoadLocalDicom(files, true, dicomModel);
            DicomModel.LOADING_EXECUTOR.execute(dicom);
            files = null;
        }
    }
}
