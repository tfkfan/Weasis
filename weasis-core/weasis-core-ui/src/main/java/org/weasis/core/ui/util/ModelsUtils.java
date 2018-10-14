package org.weasis.core.ui.util;

import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.SynchView;

import java.util.Hashtable;

public final class ModelsUtils {
    private ModelsUtils(){

    }

    public static Hashtable<String, GridBagLayoutModel> createDefaultLayoutModels(){
        Hashtable<String, GridBagLayoutModel> layoutModels = new Hashtable<>();

        layoutModels.put("1x1 Views", ImageViewerPlugin.VIEWS_1x1);
        layoutModels.put("1x2 Views", ImageViewerPlugin.VIEWS_1x2);
        layoutModels.put("2x1 Views", ImageViewerPlugin.VIEWS_2x1);
        layoutModels.put("DICOM Information", ImageViewerPlugin.VIEWS_2x1_r1xc2_dump);
        layoutModels.put("3 views (col 2,1)", ImageViewerPlugin.VIEWS_2x2_f2);
        layoutModels.put("3 views (row 1,2)", ImageViewerPlugin.VIEWS_2_f1x2);
        layoutModels.put("1x3 Views", ImageViewerPlugin.buildGridBagLayoutModel(1, 3, ImageViewerPlugin.view2dClass.getName()));
        layoutModels.put("1x4 Views", ImageViewerPlugin.buildGridBagLayoutModel(1, 4, ImageViewerPlugin.view2dClass.getName()));
        layoutModels.put("2x4 Views", ImageViewerPlugin.buildGridBagLayoutModel(2, 4, ImageViewerPlugin.view2dClass.getName()));
        layoutModels.put("2x6 Views", ImageViewerPlugin.buildGridBagLayoutModel(2, 6, ImageViewerPlugin.view2dClass.getName()));
        layoutModels.put("2x8 Views", ImageViewerPlugin.buildGridBagLayoutModel(2, 8, ImageViewerPlugin.view2dClass.getName()));
        layoutModels.put("2x2 Views", ImageViewerPlugin.VIEWS_2x2);

        return layoutModels;
    }

    public static Hashtable<String, SynchView> createDefaultSynchViews(){
        Hashtable<String, SynchView> synchViews = new Hashtable<>();

        synchViews.put("None", SynchView.NONE);
        synchViews.put("Default Stack", SynchView.DEFAULT_STACK);
        synchViews.put("Default Tile", SynchView.DEFAULT_TILE);
        synchViews.put("Default Tile Multiple", SynchView.DEFAULT_TILE_MULTIPLE);

        return synchViews;
    }
}
