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
package org.weasis.core.ui.editor.image;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.weasis.core.api.util.Copyable;
import org.weasis.core.api.util.LangUtil;

public class SynchData implements Copyable<SynchData> {

    public enum Mode {
        NONE, STACK, TILE, MULTIPLE_SLIDE_TILE
    }

    protected final Map<String, Boolean> actions;
    protected final Mode mode;

    private boolean original;

    public SynchData(Mode mode, Map<String, Boolean> actions) {
        if (actions == null) {
            throw new IllegalArgumentException("A parameter is null!"); //$NON-NLS-1$
        }
        this.actions = actions;
        this.mode = mode;
        this.original = true;
    }

    public SynchData(SynchData synchData) {
        Objects.requireNonNull(synchData);
        this.actions = new HashMap<>(synchData.actions);
        this.mode = synchData.mode;
        this.original = synchData.original;
    }

    public Map<String, Boolean> getActions() {
        return actions;
    }

    public boolean isActionEnable(String action) {
        return LangUtil.getNULLtoFalse(actions.get(action));
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public SynchData copy() {
        return new SynchData(this);
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
    }

}