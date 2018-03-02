package org.weasis.core.api.gui.util;

import org.weasis.core.api.gui.Insertable;

public interface ItemDialogPage extends PageProps, Insertable {

    void deselectPageAction();

    void selectPageAction();

}