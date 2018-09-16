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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.opencv.core.CvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.Image2DViewer;
import org.weasis.core.api.gui.model.ViewModel;
import org.weasis.core.api.gui.util.ActionState;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.gui.util.Filter;
import org.weasis.core.api.gui.util.MathUtil;
import org.weasis.core.api.gui.util.MouseActionAdapter;
import org.weasis.core.api.gui.util.SliderChangeListener;
import org.weasis.core.api.gui.util.ToggleButtonListener;
import org.weasis.core.api.gui.util.WinUtil;
import org.weasis.core.api.image.AffineTransformOp;
import org.weasis.core.api.image.FilterOp;
import org.weasis.core.api.image.ImageOpEvent;
import org.weasis.core.api.image.ImageOpNode;
import org.weasis.core.api.image.OpManager;
import org.weasis.core.api.image.PseudoColorOp;
import org.weasis.core.api.image.WindowOp;
import org.weasis.core.api.image.op.ByteLut;
import org.weasis.core.api.image.util.ImageFiler;
import org.weasis.core.api.image.util.KernelData;
import org.weasis.core.api.image.util.MeasurableLayer;
import org.weasis.core.api.image.util.Unit;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.SeriesComparator;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.service.AuditLog;
import org.weasis.core.api.util.FontTools;
import org.weasis.core.api.util.LangUtil;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.ui.Messages;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewerEvent;
import org.weasis.core.ui.editor.SeriesViewerEvent.EVENT;
import org.weasis.core.ui.editor.image.SynchData.Mode;
import org.weasis.core.ui.editor.image.dockable.MeasureTool;
import org.weasis.core.ui.model.AbstractGraphicModel;
import org.weasis.core.ui.model.GraphicModel;
import org.weasis.core.ui.model.graphic.DragGraphic;
import org.weasis.core.ui.model.graphic.Graphic;
import org.weasis.core.ui.model.imp.XmlGraphicModel;
import org.weasis.core.ui.model.layer.LayerAnnotation;
import org.weasis.core.ui.model.layer.LayerType;
import org.weasis.core.ui.model.layer.imp.RenderedImageLayer;
import org.weasis.core.ui.model.utils.Draggable;
import org.weasis.core.ui.model.utils.bean.GraphicClipboard;
import org.weasis.core.ui.model.utils.bean.PanPoint;
import org.weasis.core.ui.model.utils.bean.PanPoint.State;
import org.weasis.core.ui.model.utils.imp.DefaultViewModel;
import org.weasis.core.ui.pref.Monitor;
import org.weasis.core.ui.util.DefaultAction;
import org.weasis.core.ui.util.MouseEventDouble;
import org.weasis.core.ui.util.TitleMenuItem;
import org.weasis.opencv.data.PlanarImage;

/**
 * @author Nicolas Roduit, Benoit Jacquemoud
 */
public abstract class DefaultView2d<E extends ImageElement> extends GraphicsPane implements ViewCanvas<E> {
    private static final long serialVersionUID = 4546307243696460899L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultView2d.class);

    public enum ZoomType {
        CURRENT, BEST_FIT, PIXEL_SIZE, REAL
    }

    static final Shape[] pointer;

    static {
        pointer = new Shape[5];
        pointer[0] = new Ellipse2D.Double(-27.0, -27.0, 54.0, 54.0);
        pointer[1] = new Line2D.Double(-40.0, 0.0, -5.0, 0.0);
        pointer[2] = new Line2D.Double(5.0, 0.0, 40.0, 0.0);
        pointer[3] = new Line2D.Double(0.0, -40.0, 0.0, -5.0);
        pointer[4] = new Line2D.Double(0.0, 5.0, 0.0, 40.0);
    }

    public static final String PROP_LAYER_OFFSET = "layer.offset"; //$NON-NLS-1$

    public static final GraphicClipboard GRAPHIC_CLIPBOARD = new GraphicClipboard();

    public static final Object antialiasingOff = RenderingHints.VALUE_ANTIALIAS_OFF;
    public static final Object antialiasingOn = RenderingHints.VALUE_ANTIALIAS_ON;

    public static final Cursor EDIT_CURSOR = DefaultView2d.getCustomCursor("editpoint.png", "Edit Point", 16, 16); //$NON-NLS-1$ //$NON-NLS-2$
    public static final Cursor HAND_CURSOR = DefaultView2d.getCustomCursor("hand.gif", "hand", 16, 16); //$NON-NLS-1$ //$NON-NLS-2$
    public static final Cursor WAIT_CURSOR = DefaultView2d.getNewCursor(Cursor.WAIT_CURSOR);
    public static final Cursor CROSS_CURSOR = DefaultView2d.getNewCursor(Cursor.CROSSHAIR_CURSOR);
    public static final Cursor MOVE_CURSOR = DefaultView2d.getNewCursor(Cursor.MOVE_CURSOR);
    public static final Cursor DEFAULT_CURSOR = DefaultView2d.getNewCursor(Cursor.DEFAULT_CURSOR);

    protected final FocusHandler focusHandler = new FocusHandler();
    protected GraphicMouseHandler<E> graphicMouseHandler;

    private final PanPoint highlightedPosition = new PanPoint(State.CENTER);
    private final PanPoint startedDragPoint = new PanPoint(State.DRAGSTART);
    private int pointerType = 0;

    protected final Color pointerColor1 = Color.black;
    protected final Color pointerColor2 = Color.white;
    protected final Border normalBorder = new EtchedBorder(BevelBorder.LOWERED, Color.gray, Color.white);
    protected final Border focusBorder = new EtchedBorder(BevelBorder.LOWERED, focusColor, focusColor);
    protected final Border lostFocusBorder = new EtchedBorder(BevelBorder.LOWERED, lostFocusColor, lostFocusColor);

    protected final RenderedImageLayer<E> imageLayer;
    protected Panner<E> panner;
    protected ZoomWin<E> lens;
    private final List<ViewButton> viewButtons;
    protected ViewButton synchButton;

    protected MediaSeries<E> series = null;
    protected LayerAnnotation infoLayer;
    protected int tileOffset;

    protected final ImageViewerEventManager<E> eventManager;

    public DefaultView2d(ImageViewerEventManager<E> eventManager) {
        this(eventManager, null);
    }

    public DefaultView2d(ImageViewerEventManager<E> eventManager, ViewModel viewModel) {
        super(viewModel);
        this.eventManager = Objects.requireNonNull(eventManager);
        this.viewButtons = new ArrayList<>();
        this.tileOffset = 0;

        imageLayer = new RenderedImageLayer<>();
        actionsInView.put(ActionW.LENS.cmd(), false);
        initActionWState();
        graphicMouseHandler = new GraphicMouseHandler<>(this);

        setBorder(normalBorder);
        setFocusable(true);
        // Must be larger to the screens to be resize correctly by the container
        setPreferredSize(new Dimension(4096, 4096));
        setMinimumSize(new Dimension(50, 50));
    }

    @Override
    public void registerDefaultListeners() {
        addFocusListener(this);
        ToolTipManager.sharedInstance().registerComponent(this);
        imageLayer.addLayerChangeListener(this);
    }

    protected void buildPanner() {
        panner = Optional.ofNullable(panner).orElseGet(() -> new Panner<>(this));
    }

    @Override
    public void copyActionWState(HashMap<String, Object> actionsInView) {
        actionsInView.putAll(this.actionsInView);
    }

    protected void initActionWState() {
        E img = getImage();
        actionsInView.put(ActionW.SPATIAL_UNIT.cmd(), img == null ? Unit.PIXEL : img.getPixelSpacingUnit());
        actionsInView.put(ZOOM_TYPE_CMD, ZoomType.BEST_FIT);
        actionsInView.put(ActionW.ZOOM.cmd(), 0.0);

        actionsInView.put(ActionW.DRAWINGS.cmd(), true);
        actionsInView.put(LayerType.CROSSLINES.name(), true);
        actionsInView.put(ActionW.INVERSESTACK.cmd(), false);
        actionsInView.put(ActionW.FILTERED_SERIES.cmd(), null);
        actionsInView.put(ActionW.FLIP.cmd(), false);
        actionsInView.put(ActionW.ROTATION.cmd(), 0);

        OpManager disOp = getDisplayOpManager();

        disOp.setParamValue(WindowOp.OP_NAME, WindowOp.P_APPLY_WL_COLOR,
            eventManager.getOptions().getBooleanProperty(WindowOp.P_APPLY_WL_COLOR, true));
        disOp.setParamValue(AffineTransformOp.OP_NAME, AffineTransformOp.P_INTERPOLATION,
            eventManager.getZoomSetting().getInterpolation());
        disOp.setParamValue(AffineTransformOp.OP_NAME, AffineTransformOp.P_AFFINE_MATRIX, null);
        disOp.setParamValue(FilterOp.OP_NAME, FilterOp.P_KERNEL_DATA, KernelData.NONE);
        disOp.setParamValue(PseudoColorOp.OP_NAME, PseudoColorOp.P_LUT, ByteLut.defaultLUT);
        disOp.setParamValue(PseudoColorOp.OP_NAME, PseudoColorOp.P_LUT_INVERSE, false);
    }

    @Override
    public ImageViewerEventManager<E> getEventManager() {
        return eventManager;
    }

    @Override
    public void updateSynchState() {
        if (getActionValue(ActionW.SYNCH_LINK.cmd()) != null) {
            if (synchButton == null) {
                synchButton = new ViewButton(new ShowPopup() {

                    @Override
                    public void showPopup(Component invoker, int x, int y) {
                        final SynchData synch = (SynchData) getActionValue(ActionW.SYNCH_LINK.cmd());
                        if (synch == null) {
                            return;
                        }

                        JPopupMenu popupMenu = new JPopupMenu();
                        TitleMenuItem itemTitle = new TitleMenuItem(ActionW.SYNCH.getTitle(), popupMenu.getInsets());
                        popupMenu.add(itemTitle);
                        popupMenu.addSeparator();

                        for (Entry<String, Boolean> a : synch.getActions().entrySet()) {
                            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(a.getKey(), a.getValue());
                            menuItem.addActionListener(e -> {
                                if (e.getSource() instanceof JCheckBoxMenuItem) {
                                    JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
                                    synch.getActions().put(item.getText(), item.isSelected());
                                }
                            });
                            popupMenu.add(menuItem);
                        }
                        popupMenu.show(invoker, x, y);

                    }
                }, SYNCH_ICON);
                synchButton.setVisible(true);
                synchButton.setPosition(GridBagConstraints.SOUTHEAST);
            }
            if (!getViewButtons().contains(synchButton)) {
                getViewButtons().add(synchButton);
            }
            SynchData synch = (SynchData) getActionValue(ActionW.SYNCH_LINK.cmd());
            synchButton.setVisible(!SynchData.Mode.NONE.equals(synch.getMode()));
        } else {
            getViewButtons().remove(synchButton);
        }
    }

    protected PlanarImage getPreprocessedImage(E imageElement) {
        return imageElement.getImage((OpManager) actionsInView.get(ActionW.PREPROCESSING.cmd()));
    }

    protected void fillPixelInfo(final PixelInfo pixelInfo, final E imageElement, final double[] c) {
        if (c != null && c.length > 0) {
            pixelInfo.setValues(c);
        }
    }

    @Override
    public PixelInfo getPixelInfo(final Point p) {
        PixelInfo pixelInfo = new PixelInfo();
        E imageElement = imageLayer.getSourceImage();
        PlanarImage image = imageLayer.getSourceRenderedImage();
        if (imageElement != null && image != null) {
            Rectangle2D area = viewModel.getModelArea();
            Point offset = getImageLayer().getOffset();
            if (offset != null) {
                // Offset used for Crop operation
                area.setRect(offset.getX(), offset.getY(), area.getWidth(), area.getHeight());
                p.translate(-(int) area.getX(), -(int) area.getY());
            }

            if (area.contains(p)) {
                try {
                    // Handle special case of non square pixel image
                    pixelInfo.setPosition(new Point(p.x, p.y));
                    pixelInfo.setPixelSpacingUnit(imageElement.getPixelSpacingUnit());
                    pixelInfo.setPixelSize(imageElement.getPixelSize());
                    double[] c = image.get(p.y, p.x);
                    pixelInfo.setPixelValueUnit(imageElement.getPixelValueUnit());
                    fillPixelInfo(pixelInfo, imageElement, c);
                    if (c != null && c.length >= 1) {
                        pixelInfo.setChannelNames(getChannelNames(image));
                    }
                } catch (Exception e) {
                    LOGGER.error("Get pixel value", e);//$NON-NLS-1$
                }
            }
        }
        return pixelInfo;
    }

    protected static String[] getChannelNames(PlanarImage image) {
        if (image != null) {
            int channels = CvType.channels(image.type());
            if (channels == 3) {
                return new String[] { Messages.getString("DefaultView2d.blue"), Messages.getString("DefaultView2d.green"), Messages.getString("DefaultView2d.red") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            } else if (channels == 1) {
                return new String[] { Messages.getString("DefaultView2d.gray") }; //$NON-NLS-1$
            }
        }
        return null;
    }

    protected static class BulkDragSequence implements Draggable {
        private final List<Draggable> childDS;

        BulkDragSequence(List<DragGraphic> dragGraphList, MouseEventDouble mouseEvent) {
            childDS = new ArrayList<>(dragGraphList.size());

            for (DragGraphic dragGraph : dragGraphList) {
                Draggable dragsequence = dragGraph.createMoveDrag();
                if (dragsequence != null) {
                    childDS.add(dragsequence);
                }
            }
        }

        @Override
        public void startDrag(MouseEventDouble mouseevent) {
            int i = 0;
            for (int j = childDS.size(); i < j; i++) {
                (childDS.get(i)).startDrag(mouseevent);
            }
        }

        @Override
        public void drag(MouseEventDouble mouseevent) {
            int i = 0;
            for (int j = childDS.size(); i < j; i++) {
                (childDS.get(i)).drag(mouseevent);
            }
        }

        @Override
        public Boolean completeDrag(MouseEventDouble mouseevent) {
            int i = 0;
            for (int j = childDS.size(); i < j; i++) {
                (childDS.get(i)).completeDrag(mouseevent);
            }
            return true;
        }

    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Panner getPanner() {
        return panner;
    }

    @Override
    public void closeLens() {
        if (lens != null) {
            lens.showLens(false);
            this.remove(lens);
            actionsInView.put(ActionW.LENS.cmd(), false);
            lens = null;
        }
    }

    @Override
    public void setSeries(MediaSeries<E> series) {
        setSeries(series, null);
    }

    @Override
    public void setSeries(MediaSeries<E> newSeries, E selectedMedia) {
        MediaSeries<E> oldsequence = this.series;
        this.series = newSeries;

        if (oldsequence == null && newSeries == null) {
            return;
        }
        if (oldsequence != null && oldsequence.equals(newSeries) && imageLayer.getSourceImage() != null) {
            return;
        }

        closingSeries(oldsequence);

        initActionWState();
        try {
            if (newSeries == null) {
                setImage(null);
            } else {
                E media = selectedMedia;
                if (selectedMedia == null) {
                    media = newSeries.getMedia(tileOffset < 0 ? 0 : tileOffset,
                        (Filter<E>) actionsInView.get(ActionW.FILTERED_SERIES.cmd()), getCurrentSortComparator());
                }
                imageLayer.fireOpEvent(new ImageOpEvent(ImageOpEvent.OpEvent.SeriesChange, series, media, null));
                if (lens != null) {
                    lens.setFreezeImage(null);
                }
                setImage(media);
            }
        } catch (Exception e) {
            AuditLog.logError(LOGGER, e, "Unexpected error:"); //$NON-NLS-1$
            imageLayer.setImage(null, null);
            closeLens();
        } finally {
            eventManager.updateComponentsListener(this);
        }

        // Set the sequence to the state OPEN
        if (newSeries != null) {
            newSeries.setOpen(true);
        }
    }

    protected void closingSeries(MediaSeries<E> mediaSeries) {
        if (mediaSeries == null) {
            return;
        }
        boolean open = false;
        synchronized (UIManager.VIEWER_PLUGINS) {
            List<ViewerPlugin<?>> plugins = UIManager.VIEWER_PLUGINS;
            pluginList: for (final ViewerPlugin<?> plugin : plugins) {
                List<? extends MediaSeries<?>> openSeries = plugin.getOpenSeries();
                if (openSeries != null) {
                    for (MediaSeries<?> s : openSeries) {
                        if (mediaSeries == s) {
                            // The sequence is still open in another view or plugin
                            open = true;
                            break pluginList;
                        }
                    }
                }
            }
        }
        mediaSeries.setOpen(open);
        // TODO setSelected and setFocused must be global to all view as open
        mediaSeries.setSelected(false, null);
        mediaSeries.setFocused(false);
    }

    @Override
    public void setFocused(Boolean focused) {
        if (series != null) {
            series.setFocused(focused);
        }
        if (focused && getBorder() == lostFocusBorder) {
            setBorder(focusBorder);
        } else if (!focused && getBorder() == focusBorder) {
            setBorder(lostFocusBorder);
        }
    }

    protected int getImageSize(E img, TagW tag1, TagW tag2) {
        Integer size = (Integer) img.getTagValue(tag1);
        if (size == null) {
            size = (Integer) img.getTagValue(tag2);
        }
        return (size == null) ? ImageFiler.TILESIZE : size;
    }

    protected Rectangle getImageBounds(E img) {
        if (img != null) {
            PlanarImage source = getPreprocessedImage(img);
            // Get the displayed width (adapted in case of the aspect ratio is not 1/1)
            boolean nosquarePixel = MathUtil.isDifferent(img.getRescaleX(), img.getRescaleY());
            int width = source == null || nosquarePixel
                ? img.getRescaleWidth(getImageSize(img, TagW.ImageWidth, TagW.get("Columns"))) : source.width(); //$NON-NLS-1$
            int height = source == null || nosquarePixel
                ? img.getRescaleHeight(getImageSize(img, TagW.ImageHeight, TagW.get("Rows"))) : source.height(); //$NON-NLS-1$
            return new Rectangle(0, 0, width, height);
        }
        return new Rectangle(0, 0, 512, 512);
    }

    protected void updateCanvas(E img, boolean triggerViewModelChangeListeners) {
        final Rectangle modelArea = getImageBounds(img);
        if (!modelArea.equals(getViewModel().getModelArea())) {
            DefaultViewModel m = (DefaultViewModel) getViewModel();
            boolean oldVal = m.isEnableViewModelChangeListeners();
            if (!triggerViewModelChangeListeners) {
                m.setEnableViewModelChangeListeners(false);
            }
            m.adjustMinViewScaleFromImage(modelArea.width, modelArea.height);
            m.setModelArea(modelArea);
            if (!triggerViewModelChangeListeners) {
                m.setEnableViewModelChangeListeners(oldVal);
            }
        }
    }

    @Override
    public void updateCanvas(boolean triggerViewModelChangeListeners) {
        updateCanvas(getImage(), triggerViewModelChangeListeners);
    }

    protected void setImage(E img) {
        boolean updateGraphics = false;
        imageLayer.setEnableDispOperations(false);
        if (img == null) {
            actionsInView.put(ActionW.SPATIAL_UNIT.cmd(), Unit.PIXEL);
            ActionState spUnitAction = eventManager.getAction(ActionW.SPATIAL_UNIT);
            if (spUnitAction instanceof ComboItemListener) {
                ((ComboItemListener) spUnitAction)
                    .setSelectedItemWithoutTriggerAction(actionsInView.get(ActionW.SPATIAL_UNIT.cmd()));
            }
            // Force the update for null image
            imageLayer.setEnableDispOperations(true);
            imageLayer.setImage(null, null);
            imageLayer.setEnableDispOperations(false);

            setGraphicManager(new XmlGraphicModel());
            closeLens();
        } else {
            E oldImage = imageLayer.getSourceImage();
            if (!img.equals(oldImage)) {
                updateGraphics = true;
                actionsInView.put(ActionW.SPATIAL_UNIT.cmd(), img.getPixelSpacingUnit());
                if (eventManager.getSelectedViewPane() == this) {
                    ActionState spUnitAction = eventManager.getAction(ActionW.SPATIAL_UNIT);
                    if (spUnitAction instanceof ComboItemListener) {
                        ((ComboItemListener) spUnitAction)
                            .setSelectedItemWithoutTriggerAction(actionsInView.get(ActionW.SPATIAL_UNIT.cmd()));
                    }
                }
                actionsInView.put(ActionW.PREPROCESSING.cmd(), null);
                ActionState spUnitAction = eventManager.getAction(ActionW.SPATIAL_UNIT);
                if (spUnitAction instanceof ComboItemListener) {
                    ((ComboItemListener) spUnitAction)
                        .setSelectedItemWithoutTriggerAction(actionsInView.get(ActionW.SPATIAL_UNIT.cmd()));
                }

                updateCanvas(img, false);

                imageLayer.fireOpEvent(new ImageOpEvent(ImageOpEvent.OpEvent.ImageChange, series, img, null));
                resetZoom();

                imageLayer.setImage(img, (OpManager) actionsInView.get(ActionW.PREPROCESSING.cmd()));

                if (AuditLog.LOGGER.isInfoEnabled()) {
                    PlanarImage image = img.getImage();
                    if (image != null) {
                        int elemSize = CvType.ELEM_SIZE(image.type());
                        int channels = CvType.channels(image.type());
                        int bpp = (elemSize * 8) / channels;
                        String[] elements = new String[channels];
                        for (int i = 0; i < elements.length; i++) {
                            elements[i] = Integer.toString(bpp);
                        }
                        String pixSize = String.join(",", elements); //$NON-NLS-1$

                        AuditLog.LOGGER.info("open:image size:{},{} depth:{}", //$NON-NLS-1$
                            new Object[] { image.width(), image.height(), pixSize });
                    }
                }
            }
            // Apply all image processing operation for visualization
            imageLayer.setEnableDispOperations(true);

            if (updateGraphics) {
                GraphicModel modelList = (GraphicModel) img.getTagValue(TagW.PresentationModel);
                // After getting a new image iterator, update the measurements
                if (modelList == null) {
                    modelList = new XmlGraphicModel(img);
                    img.setTag(TagW.PresentationModel, modelList);
                }
                setGraphicManager(modelList);
            }

            if (panner != null) {
                panner.updateImage();
            }
            if (lens != null) {
                lens.updateImage();
                lens.updateZoom();
            }
        }
    }

    @Override
    public double getBestFitViewScale() {
        return adjustViewScale(super.getBestFitViewScale());
    }

    @Override
    public double getRealWorldViewScale() {
        double viewScale = 0.0;
        E img = getImage();
        if (img != null) {
            Window win = SwingUtilities.getWindowAncestor(this);
            if (win != null) {
                GraphicsConfiguration config = win.getGraphicsConfiguration();
                Monitor monitor = MeasureTool.viewSetting.getMonitor(config.getDevice());
                if (monitor != null) {
                    double realFactor = monitor.getRealScaleFactor();
                    if (realFactor > 0.0) {
                        Unit imgUnit = img.getPixelSpacingUnit();
                        if (!Unit.PIXEL.equals(imgUnit)) {
                            viewScale = imgUnit.getConvFactor() * img.getPixelSize() / realFactor;
                            viewScale = -adjustViewScale(viewScale);
                        }
                    }
                }
            }
        }
        return viewScale;
    }

    protected double adjustViewScale(double viewScale) {
        double ratio = viewScale;
        if (ratio < DefaultViewModel.SCALE_MIN) {
            ratio = DefaultViewModel.SCALE_MIN;
        } else if (ratio > DefaultViewModel.SCALE_MAX) {
            ratio = DefaultViewModel.SCALE_MAX;
        }
        ActionState zoom = eventManager.getAction(ActionW.ZOOM);
        if (zoom instanceof SliderChangeListener) {
            SliderChangeListener z = (SliderChangeListener) zoom;
            // Adjust the best fit value according to the possible range of the model zoom action.
            if (eventManager.getSelectedViewPane() == this) {
                // Set back the value to UI components as this value cannot be computed early.
                z.setRealValue(ratio, false);
                ratio = z.getRealValue();
            } else {
                ratio = z.toModelValue(z.toSliderValue(ratio));
            }
        }
        return ratio;
    }

    @SuppressWarnings("rawtypes")
    protected boolean isDrawActionActive() {
        ViewerPlugin container = WinUtil.getParentOfClass(this, ViewerPlugin.class);
        if (container != null) {
            final ViewerToolBar toolBar = container.getViewerToolBar();
            if (toolBar != null) {
                return toolBar.isCommandActive(ActionW.MEASURE.cmd()) || toolBar.isCommandActive(ActionW.DRAW.cmd());
            }
        }
        return false;
    }

    @Override
    public RenderedImageLayer<E> getImageLayer() {
        return imageLayer;
    }

    @Override
    public MeasurableLayer getMeasurableLayer() {
        return imageLayer;
    }

    @Override
    public LayerAnnotation getInfoLayer() {
        return infoLayer;
    }

    @Override
    public int getTileOffset() {
        return tileOffset;
    }

    @Override
    public void setTileOffset(int tileOffset) {
        this.tileOffset = tileOffset;
    }

    @Override
    public MediaSeries<E> getSeries() {
        return series;
    }

    @Override
    public E getImage() {
        return imageLayer.getSourceImage();
    }

    @Override
    public PlanarImage getSourceImage() {
        E image = getImage();
        return image == null ? null : getPreprocessedImage(image);
    }

    @Override
    public final void center() {
        setCenter(0.0, 0.0);
    }

    @Override
    public final void setCenter(Double modelOffsetX, Double modelOffsetY) {
        // Only apply when the panel size is not zero.
        if (getWidth() != 0 && getHeight() != 0) {
            getViewModel().setModelOffset(modelOffsetX, modelOffsetY);
            Optional.ofNullable(panner).ifPresent(Panner<E>::updateImageSize);
            Optional.ofNullable(lens).ifPresent(ZoomWin<E>::updateZoom);
            updateAffineTransform();
        }
    }

    /** Provides panning */
    public final void moveOrigin(double x, double y) {
        setCenter(getViewModel().getModelOffsetX() + x, getViewModel().getModelOffsetY() + y);
    }

    @Override
    public final void moveOrigin(PanPoint point) {
        if (point != null) {
            if (PanPoint.State.CENTER.equals(point.getState())) {
                highlightedPosition.setHighlightedPosition(point.isHighlightedPosition());
                highlightedPosition.setLocation(point);
                Rectangle2D area = getViewModel().getModelArea();
                setCenter(area.getWidth() * 0.5 - point.getX(), area.getHeight() * 0.5 - point.getY());
            } else if (PanPoint.State.MOVE.equals(point.getState())) {
                moveOrigin(point.getX(), point.getY());
            } else if (PanPoint.State.DRAGSTART.equals(point.getState())) {
                startedDragPoint.setLocation(getViewModel().getModelOffsetX(), getViewModel().getModelOffsetY());
            } else if (PanPoint.State.DRAGGING.equals(point.getState())) {
                setCenter(startedDragPoint.getX() + point.getX(), startedDragPoint.getY() + point.getY());
            }
        }
    }

    @Override
    public Comparator<E> getCurrentSortComparator() {
        SeriesComparator<E> sort = (SeriesComparator<E>) actionsInView.get(ActionW.SORTSTACK.cmd());
        Boolean reverse = (Boolean) actionsInView.get(ActionW.INVERSESTACK.cmd());
        return (reverse != null && reverse) ? sort.getReversOrderComparator() : sort;
    }

    @Override
    public int getFrameIndex() {
        if (series instanceof Series) {
            return ((Series<E>) series).getImageIndex(imageLayer.getSourceImage(),
                (Filter<E>) actionsInView.get(ActionW.FILTERED_SERIES.cmd()), getCurrentSortComparator());
        }
        return -1;
    }

    @Override
    public void setActionsInView(String action, Object value) {
        setActionsInView(action, value, false);
    }

    @Override
    public void setActionsInView(String action, Object value, Boolean repaint) {
        if (action != null) {
            actionsInView.put(action, value);
            if (repaint) {
                repaint();
            }
        }
    }

    @Override
    public void setSelected(Boolean selected) {
        setBorder(selected ? focusBorder : normalBorder);
        // Remove the selection of graphics
        graphicManager.setSelectedGraphic(null);
        // Throws to the tool listener the current graphic selection.
        graphicManager.fireGraphicsSelectionChanged(imageLayer);

        if (selected && series != null) {
            AuditLog.LOGGER.info("select:series nb:{}", series.getSeriesNumber()); //$NON-NLS-1$
        }
    }

    @Override
    public Font getFont() {
        // required when used getGraphics().getFont() in DefaultGraphicLabel
        return MeasureTool.viewSetting.getFont();
    }

    @Override
    public Font getLayerFont() {
        int fontSize =
            // Set font size according to the view size
            (int) Math
                .ceil(10 / ((this.getGraphics().getFontMetrics(FontTools.getFont12()).stringWidth("0123456789") * 7.0) //$NON-NLS-1$
                    / getWidth()));
        fontSize = fontSize < 6 ? 6 : fontSize > 16 ? 16 : fontSize;
        return new Font(Font.SANS_SERIF, 0, fontSize);
    }

    /** paint routine */
    @Override
    public void paintComponent(Graphics g) {
        if (g instanceof Graphics2D) {
            draw((Graphics2D) g);
        }
    }

    protected void draw(Graphics2D g2d) {
        Stroke oldStroke = g2d.getStroke();
        Paint oldColor = g2d.getPaint();

        // Paint the visible area
        // Set font size for computing shared text areas that need to be repainted in different zoom magnitudes.
        Font defaultFont = getFont();
        g2d.setFont(defaultFont);

        Point2D p = getClipViewCoordinatesOffset();
        g2d.translate(p.getX(), p.getY());
        imageLayer.drawImage(g2d);
        drawLayers(g2d, affineTransform, inverseTransform);
        g2d.translate(-p.getX(), -p.getY());

        drawPointer(g2d);
        drawAffineInvariant(g2d);
        if (infoLayer != null) {
            g2d.setFont(getLayerFont());
            infoLayer.paint(g2d);
        }
        drawOnTop(g2d);

        g2d.setFont(defaultFont);
        g2d.setPaint(oldColor);
        g2d.setStroke(oldStroke);
    }

    private void drawAffineInvariant(Graphics2D g2d) {
    }

    protected void drawOnTop(Graphics2D g2d) {
    }

    @Override
    public void drawLayers(Graphics2D g2d, AffineTransform transform, AffineTransform inverseTransform) {
        if ((Boolean) actionsInView.get(ActionW.DRAWINGS.cmd())) {
            graphicManager.draw(g2d, transform, inverseTransform, null);
        }
    }

    @Override
    public void zoom(Double viewScale) {
        boolean defSize = MathUtil.isEqualToZero(viewScale);
        ZoomType type = (ZoomType) actionsInView.get(ZOOM_TYPE_CMD);
        double ratio = viewScale;
        if (defSize) {
            if (ZoomType.BEST_FIT.equals(type)) {
                ratio = -getBestFitViewScale();
            } else if (ZoomType.REAL.equals(type)) {
                ratio = -getRealWorldViewScale();
            }

            if (MathUtil.isEqualToZero(ratio)) {
                ratio = -adjustViewScale(1.0);
            }
        }

        actionsInView.put(ActionW.ZOOM.cmd(), ratio);
        super.zoom(Math.abs(ratio));
        if (defSize) {
            /*
             * If the view has not been repainted once (the width and the height of the view is 0), it will be done
             * later and the componentResized event will call again the zoom.
             */
            center();
        }
        updateAffineTransform();
        if (panner != null) {
            panner.updateImageSize();
        }
    }

    protected void updateAffineTransform() {
        Rectangle2D modelArea = getViewModel().getModelArea();
        double viewScale = getViewModel().getViewScale();

        double rWidth = modelArea.getWidth();
        double rHeight = modelArea.getHeight();

        OpManager dispOp = getDisplayOpManager();
        boolean flip = LangUtil.getNULLtoFalse((Boolean) actionsInView.get(ActionW.FLIP.cmd()));
        Integer rotationAngle = (Integer) actionsInView.get(ActionW.ROTATION.cmd());

        affineTransform.setToScale(flip ? -viewScale : viewScale, viewScale);
        if (rotationAngle != null && rotationAngle > 0) {
            affineTransform.rotate(Math.toRadians(rotationAngle), rWidth / 2.0, rHeight / 2.0);
        }
        if (flip) {
            affineTransform.translate(-rWidth, 0.0);
        }

        ImageOpNode node = dispOp.getNode(AffineTransformOp.OP_NAME);
        if (node != null) {
            Rectangle2D imgBounds = affineTransform.createTransformedShape(modelArea).getBounds2D();

            double diffx = 0.0;
            double diffy = 0.0;
            Rectangle2D viewBounds = new Rectangle2D.Double(0, 0, getWidth(), getHeight());
            Rectangle2D srcBounds = getImageViewBounds();

            Rectangle2D dstBounds;
            if (viewBounds.contains(srcBounds)) {
                dstBounds = srcBounds;
            } else {
                dstBounds = viewBounds.createIntersection(srcBounds);

                if (srcBounds.getX() < 0.0) {
                    diffx += srcBounds.getX();
                }
                if (srcBounds.getY() < 0.0) {
                    diffy += srcBounds.getY();
                }
            }

            double[] fmx = new double[6];
            affineTransform.getMatrix(fmx);
            // adjust transformation matrix => move the center to keep all the image
            fmx[4] -= imgBounds.getX() - diffx;
            fmx[5] -= imgBounds.getY() - diffy;
            affineTransform.setTransform(fmx[0], fmx[1], fmx[2], fmx[3], fmx[4], fmx[5]);

            // Convert to openCV affine matrix
            double[] m = new double[] { fmx[0], fmx[2], fmx[4], fmx[1], fmx[3], fmx[5] };
            node.setParam(AffineTransformOp.P_AFFINE_MATRIX, m);

            node.setParam(AffineTransformOp.P_DST_BOUNDS, dstBounds);
            imageLayer.updateDisplayOperations();
        }

        // Keep the coordinates of the original image when cropping
        Point offset = getImageLayer().getOffset();
        if (offset != null) {
            affineTransform.translate(-offset.getX(), -offset.getY());
        }

        try {
            inverseTransform.setTransform(affineTransform.createInverse());
        } catch (NoninvertibleTransformException e) {
            LOGGER.error("Create inverse transform", e); //$NON-NLS-1$
        }
    }

    @Override
    public void setDrawingsVisibility(Boolean visible) {
        if ((Boolean) actionsInView.get(ActionW.DRAWINGS.cmd()) != visible) {
            actionsInView.put(ActionW.DRAWINGS.cmd(), visible);
            repaint();
        }
    }

    @Override
    public Object getLensActionValue(String action) {
        if (lens == null) {
            return null;
        }
        return lens.getActionValue(action);
    }

    @Override
    public void changeZoomInterpolation(Integer interpolation) {
        Integer val =
            (Integer) getDisplayOpManager().getParamValue(AffineTransformOp.OP_NAME, AffineTransformOp.P_INTERPOLATION);
        boolean update = !Objects.equals(val, interpolation);
        if (update) {
            getDisplayOpManager().setParamValue(AffineTransformOp.OP_NAME, AffineTransformOp.P_INTERPOLATION,
                interpolation);
            if (lens != null) {
                lens.getDisplayOpManager().setParamValue(AffineTransformOp.OP_NAME, AffineTransformOp.P_INTERPOLATION,
                    interpolation);
                lens.updateZoom();
            }
            imageLayer.updateDisplayOperations();
        }
    }

    @Override
    public OpManager getDisplayOpManager() {
        return imageLayer.getDisplayOpManager();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (series == null) {
            return;
        }
        PlanarImage dispImage = imageLayer.getDisplayImage();
        OpManager manager = imageLayer.getDisplayOpManager();
        final String command = evt.getPropertyName();
        if (command.equals(ActionW.SYNCH.cmd())) {
            SynchEvent synch = (SynchEvent) evt.getNewValue();
            if (synch instanceof SynchCineEvent) {
                SynchCineEvent value = (SynchCineEvent) synch;

                E imgElement = getImage();
                graphicManager.deleteByLayerType(LayerType.CROSSLINES);

                if (value.getView() == this) {
                    if (tileOffset != 0) {
                        // Index could have changed when loading series.
                        imgElement = series.getMedia(value.getSeriesIndex() + tileOffset,
                            (Filter<E>) actionsInView.get(ActionW.FILTERED_SERIES.cmd()), getCurrentSortComparator());
                    } else if (value.getMedia() instanceof ImageElement) {
                        imgElement = (E) value.getMedia();
                    }
                } else if (value.getLocation() != null) {
                    Boolean cutlines = (Boolean) actionsInView.get(ActionW.SYNCH_CROSSLINE.cmd());
                    if (cutlines != null && cutlines) {
                        if (LangUtil.getNULLtoTrue((Boolean) actionsInView.get(LayerType.CROSSLINES.name()))) {
                            // Compute cutlines from the location of selected image
                            computeCrosslines(value.getLocation().doubleValue());
                        }
                    } else {
                        double location = value.getLocation().doubleValue();
                        // TODO add a way in GUI to resynchronize series. Offset should be in Series tag and related
                        // to
                        // a specific series
                        // Double offset = (Double) actionsInView.get(ActionW.STACK_OFFSET.cmd());
                        // if (offset != null) {
                        // location += offset;
                        // }
                        imgElement = series.getNearestImage(location, tileOffset,
                            (Filter<E>) actionsInView.get(ActionW.FILTERED_SERIES.cmd()), getCurrentSortComparator());

                        AuditLog.LOGGER.info("synch:series nb:{}", series.getSeriesNumber()); //$NON-NLS-1$
                    }
                } else {
                    // When no 3D information on the slice position
                    imgElement = series.getMedia(value.getSeriesIndex() + tileOffset,
                        (Filter<E>) actionsInView.get(ActionW.FILTERED_SERIES.cmd()), getCurrentSortComparator());

                    AuditLog.LOGGER.info("synch:series nb:{}", series.getSeriesNumber()); //$NON-NLS-1$
                }

                Double zoomFactor = (Double) actionsInView.get(ActionW.ZOOM.cmd());
                // Avoid to reset zoom when the mode is not best fit
                if (zoomFactor != null && zoomFactor >= 0.0) {
                    Object zoomType = actionsInView.get(ViewCanvas.ZOOM_TYPE_CMD);
                    actionsInView.put(ViewCanvas.ZOOM_TYPE_CMD, ZoomType.CURRENT);
                    setImage(imgElement);
                    actionsInView.put(ViewCanvas.ZOOM_TYPE_CMD, zoomType);
                } else {
                    setImage(imgElement);
                }
            } else {
                propertyChange(synch);
            }
        } else if (command.equals(ActionW.IMAGE_PIX_PADDING.cmd())) {
            if (manager.setParamValue(WindowOp.OP_NAME, command, evt.getNewValue())) {
                imageLayer.updateDisplayOperations();
            }
        } else if (command.equals(ActionW.PROGRESSION.cmd())) {
            actionsInView.put(command, evt.getNewValue());
            imageLayer.updateDisplayOperations();
        }

        if (Objects.nonNull(lens) && !Objects.equals(dispImage, imageLayer.getDisplayImage())) {
            /*
             * Transmit to the lens the command in case the source image has been freeze (for updating rotation and flip
             * => will keep consistent display)
             */
            lens.setCommandFromParentView(command, evt.getNewValue());
            lens.updateZoom();

        }
    }

    private void propertyChange(final SynchEvent synch) {
        SynchData synchData = (SynchData) actionsInView.get(ActionW.SYNCH_LINK.cmd());
        if (synchData != null && Mode.NONE.equals(synchData.getMode())) {
            return;
        }

        OpManager manager = imageLayer.getDisplayOpManager();

        for (Entry<String, Object> entry : synch.getEvents().entrySet()) {
            String command = entry.getKey();
            if (synchData != null && !synchData.isActionEnable(command)) {
                continue;
            }
            if (command.equals(ActionW.WINDOW.cmd()) || command.equals(ActionW.LEVEL.cmd())) {
                if (manager.setParamValue(WindowOp.OP_NAME, command, ((Number) entry.getValue()).doubleValue())) {
                    imageLayer.updateDisplayOperations();
                }
            } else if (command.equals(ActionW.ROTATION.cmd())) {
                Object old = actionsInView.put(ActionW.ROTATION.cmd(), entry.getValue());
                if (!Objects.equals(old, entry.getValue())) {
                    updateAffineTransform();
                }
            } else if (command.equals(ActionW.RESET.cmd())) {
                reset();
            } else if (command.equals(ActionW.ZOOM.cmd())) {
                double val = (Double) entry.getValue();
                // Special Cases: -200.0 => best fit, -100.0 => real world size
                if (MathUtil.isDifferent(val, -200.0) && MathUtil.isDifferent(val, -100.0)) {
                    zoom(val);
                } else {
                    Object zoomType = actionsInView.get(ViewCanvas.ZOOM_TYPE_CMD);
                    actionsInView.put(ViewCanvas.ZOOM_TYPE_CMD,
                        MathUtil.isEqual(val, -100.0) ? ZoomType.REAL : ZoomType.BEST_FIT);
                    zoom(0.0);
                    actionsInView.put(ViewCanvas.ZOOM_TYPE_CMD, zoomType);
                }
            } else if (command.equals(ActionW.LENSZOOM.cmd())) {
                if (lens != null) {
                    lens.setActionInView(ActionW.ZOOM.cmd(), entry.getValue());
                    lens.updateZoom();
                }
            } else if (command.equals(ActionW.LENS.cmd())) {
                Boolean showLens = (Boolean) entry.getValue();
                actionsInView.put(command, showLens);
                if (showLens) {
                    if (lens == null) {
                        lens = new ZoomWin<>(this);
                    }
                    // resize if to big
                    int maxWidth = getWidth() / 3;
                    int maxHeight = getHeight() / 3;
                    lens.setSize(lens.getWidth() > maxWidth ? maxWidth : lens.getWidth(),
                        lens.getHeight() > maxHeight ? maxHeight : lens.getHeight());
                    this.add(lens);
                    lens.showLens(true);

                } else {
                    closeLens();
                }

            } else if (command.equals(ActionW.PAN.cmd())) {
                Object point = entry.getValue();
                // ImageViewerPlugin<E> view = eventManager.getSelectedView2dContainer();
                // if (view != null) {
                // if(!view.getSynchView().isActionEnable(ActionW.ROTATION)){
                //
                // }
                // }
                if (point instanceof PanPoint) {
                    moveOrigin((PanPoint) entry.getValue());
                }

            } else if (command.equals(ActionW.FLIP.cmd())) {
                // Horizontal flip is applied after rotation (To be compliant with DICOM PR)
                Object old = actionsInView.put(ActionW.FLIP.cmd(), entry.getValue());
                if (!Objects.equals(old, entry.getValue())) {
                    updateAffineTransform();
                }
            } else if (command.equals(ActionW.LUT.cmd())) {
                if (manager.setParamValue(PseudoColorOp.OP_NAME, PseudoColorOp.P_LUT, entry.getValue())) {
                    imageLayer.updateDisplayOperations();
                }
            } else if (command.equals(ActionW.INVERT_LUT.cmd())) {
                if (manager.setParamValue(WindowOp.OP_NAME, command, entry.getValue())) {
                    manager.setParamValue(PseudoColorOp.OP_NAME, PseudoColorOp.P_LUT_INVERSE, entry.getValue());
                    // Update VOI LUT if pixel padding
                    imageLayer.updateDisplayOperations();
                }
            } else if (command.equals(ActionW.FILTER.cmd())) {
                if (manager.setParamValue(FilterOp.OP_NAME, FilterOp.P_KERNEL_DATA, entry.getValue())) {
                    imageLayer.updateDisplayOperations();
                }
            } else if (command.equals(ActionW.SPATIAL_UNIT.cmd())) {
                actionsInView.put(command, entry.getValue());
                // TODO update only measure and limit when selected view share graphics
                graphicManager.updateLabels(Boolean.TRUE, this);
            }
        }
    }

    protected void computeCrosslines(double location) {

    }

    @Override
    public void disposeView() {
        disableMouseAndKeyListener();
        removeFocusListener(this);
        ToolTipManager.sharedInstance().unregisterComponent(this);
        imageLayer.removeLayerChangeListener(this);
        Optional.ofNullable(lens).ifPresent(l -> l.showLens(false));
        if (series != null) {
            closingSeries(series);
            series = null;
        }
        super.disposeView();
    }

    @Override
    public synchronized void disableMouseAndKeyListener() {
        MouseListener[] listener = this.getMouseListeners();

        MouseMotionListener[] motionListeners = this.getMouseMotionListeners();
        KeyListener[] keyListeners = this.getKeyListeners();
        MouseWheelListener[] wheelListeners = this.getMouseWheelListeners();
        for (int i = 0; i < listener.length; i++) {
            this.removeMouseListener(listener[i]);
        }
        for (int i = 0; i < motionListeners.length; i++) {
            this.removeMouseMotionListener(motionListeners[i]);
        }
        for (int i = 0; i < keyListeners.length; i++) {
            this.removeKeyListener(keyListeners[i]);
        }
        for (int i = 0; i < wheelListeners.length; i++) {
            this.removeMouseWheelListener(wheelListeners[i]);
        }
        Optional.ofNullable(lens).ifPresent(l -> l.disableMouseAndKeyListener());
    }

    @Override
    public synchronized void iniDefaultMouseListener() {
        // focus listener is always on
        this.addMouseListener(focusHandler);
        this.addMouseMotionListener(focusHandler);
    }

    @Override
    public synchronized void iniDefaultKeyListener() {
        this.addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
            final ViewTransferHandler imageTransferHandler = new ViewTransferHandler();
            imageTransferHandler.exportToClipboard(DefaultView2d.this, Toolkit.getDefaultToolkit().getSystemClipboard(),
                TransferHandler.COPY);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {
            eventManager.nextLeftMouseAction();
        } else if (e.getModifiers() == 0 && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_I)) {
            eventManager.fireSeriesViewerListeners(
                new SeriesViewerEvent(eventManager.getSelectedView2dContainer(), null, null, EVENT.TOOGLE_INFO));
        } else if (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_L) {
            // Counterclockwise
            eventManager.getAction(ActionW.ROTATION, SliderChangeListener.class)
                .ifPresent(a -> a.setSliderValue((a.getSliderValue() + 270) % 360));
        } else if (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_R) {
            // Clockwise
            eventManager.getAction(ActionW.ROTATION, SliderChangeListener.class)
                .ifPresent(a -> a.setSliderValue((a.getSliderValue() + 90) % 360));
        } else if (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_F) {
            // Flip horizontal
            ActionState flipAction = eventManager.getAction(ActionW.FLIP);
            if (flipAction instanceof ToggleButtonListener) {
                ((ToggleButtonListener) flipAction).setSelected(!((ToggleButtonListener) flipAction).isSelected());
            }
        } else {
            Optional<ActionW> action = eventManager.getLeftMouseActionFromkeyEvent(e.getKeyCode(), e.getModifiers());
            if (action.isPresent()) {
                eventManager.changeLeftMouseAction(action.get().cmd());
            } else {
                eventManager.keyPressed(e);
            }
        }
    }

    private void drawPointer(Graphics2D g) {
        if (pointerType < 1) {
            return;
        }
        if ((pointerType & CENTER_POINTER) == CENTER_POINTER) {
            drawPointer(g, (getWidth() - 1) * 0.5, (getHeight() - 1) * 0.5);
        }
        if ((pointerType & HIGHLIGHTED_POINTER) == HIGHLIGHTED_POINTER && highlightedPosition.isHighlightedPosition()) {
            // Display the position on the center of the pixel (constant position even with a high zoom factor)
            double offsetX = modelToViewLength(highlightedPosition.getX() + 0.5 - viewModel.getModelOffsetX());
            double offsetY = modelToViewLength(highlightedPosition.getY() + 0.5 - viewModel.getModelOffsetY());
            drawPointer(g, offsetX, offsetY);
        }
    }

    @Override
    public int getPointerType() {
        return pointerType;
    }

    @Override
    public void setPointerType(int pointerType) {
        this.pointerType = pointerType;
    }

    @Override
    public void addPointerType(int i) {
        this.pointerType |= i;
    }

    @Override
    public void resetPointerType(int i) {
        this.pointerType &= ~i;
    }

    @Override
    public Point2D getHighlightedPosition() {
        return highlightedPosition;
    }

    @Override
    public void drawPointer(Graphics2D g, Double x, Double y) {
        float[] dash = { 5.0f };
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.setStroke(new BasicStroke(3.0f));
        g.setPaint(pointerColor1);
        for (int i = 1; i < pointer.length; i++) {
            g.draw(pointer[i]);
        }
        g.setStroke(new BasicStroke(1.0f, 0, 0, 5.0f, dash, 0.0f));
        g.setPaint(pointerColor2);
        for (int i = 1; i < pointer.length; i++) {
            g.draw(pointer[i]);
        }
        g.translate(-x, -y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }

    protected void showPixelInfos(MouseEvent mouseevent) {
        if (infoLayer != null) {
            Point2D pModel = getImageCoordinatesFromMouse(mouseevent.getX(), mouseevent.getY());
            Rectangle oldBound = infoLayer.getPixelInfoBound();
            PixelInfo pixelInfo =
                getPixelInfo(new Point((int) Math.floor(pModel.getX()), (int) Math.floor(pModel.getY())));
            oldBound.width = Math.max(oldBound.width, this.getGraphics().getFontMetrics(getLayerFont())
                .stringWidth(Messages.getString("DefaultView2d.pix") + StringUtil.COLON_AND_SPACE + pixelInfo) + 4); //$NON-NLS-1$
            infoLayer.setPixelInfo(pixelInfo);
            repaint(oldBound);
        }
    }

    @Override
    public void focusGained(FocusEvent e) {

    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class FocusHandler extends MouseActionAdapter {

        @Override
        public void mousePressed(MouseEvent evt) {
            ImageViewerPlugin<E> pane = eventManager.getSelectedView2dContainer();
            if (Objects.isNull(pane)) {
                return;
            }

            ViewButton selectedButton = null;
            // Do select the view when pressing on a view button
            for (ViewButton b : getViewButtons()) {
                if (b.isVisible() && b.contains(evt.getPoint())) {
                    selectedButton = b;
                    break;
                }
            }

            if (evt.getClickCount() == 2 && selectedButton == null) {
                pane.maximizedSelectedImagePane(DefaultView2d.this, evt);
                return;
            }

            if (pane.isContainingView(DefaultView2d.this) && pane.getSelectedImagePane() != DefaultView2d.this) {
                // register all actions of the EventManager with this view waiting the focus gained in some cases is not
                // enough, because others mouseListeners are triggered before the focus event (that means before
                // registering the view in the EventManager)
                pane.setSelectedImagePane(DefaultView2d.this);
            }
            // request the focus even it is the same pane selected
            requestFocusInWindow();

            // Do select the view when pressing on a view button
            if (selectedButton != null) {
                DefaultView2d.this.setCursor(DefaultView2d.DEFAULT_CURSOR);
                evt.consume();
                selectedButton.showPopup(evt.getComponent(), evt.getX(), evt.getY());
                return;
            }

            Optional<ActionW> action = eventManager.getMouseAction(evt.getModifiersEx());
            DefaultView2d.this.setCursor(action.isPresent() ? action.get().getCursor() : DefaultView2d.DEFAULT_CURSOR);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            showPixelInfos(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            DefaultView2d.this.setCursor(DefaultView2d.DEFAULT_CURSOR);
        }
    }

    @Override
    public List<Action> getExportToClipboardAction() {
        List<Action> list = new ArrayList<>();

        AbstractAction exportToClipboardAction =
            new DefaultAction(Messages.getString("DefaultView2d.clipboard"), new ImageIcon(DefaultView2d.class.getResource("/icon/16x16/camera.png")), event -> { //$NON-NLS-1$ //$NON-NLS-2$
                final ViewTransferHandler imageTransferHandler = new ViewTransferHandler();
                imageTransferHandler.exportToClipboard(DefaultView2d.this,
                    Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY);
            });
        exportToClipboardAction.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        list.add(exportToClipboardAction);

        // TODO exclude big images?
        exportToClipboardAction = new DefaultAction(Messages.getString("DefaultView2d.clipboard_real"), event -> { //$NON-NLS-1$
            final ImageTransferHandler imageTransferHandler = new ImageTransferHandler();
            imageTransferHandler.exportToClipboard(DefaultView2d.this, Toolkit.getDefaultToolkit().getSystemClipboard(),
                TransferHandler.COPY);
        });
        list.add(exportToClipboardAction);

        return list;
    }

    public static final AffineTransform getAffineTransform(MouseEvent mouseevent) {
        if (mouseevent != null && mouseevent.getSource() instanceof Image2DViewer) {
            return ((Image2DViewer) mouseevent.getSource()).getAffineTransform();
        }
        return null;
    }

    @Override
    public void resetZoom() {
        ZoomType type = (ZoomType) actionsInView.get(ZOOM_TYPE_CMD);
        if (!ZoomType.CURRENT.equals(type)) {
            zoom(0.0);
        }
    }

    @Override
    public void resetPan() {
        center();
    }

    @Override
    public void reset() {
        imageLayer.setEnableDispOperations(false);
        ImageViewerPlugin<E> pane = eventManager.getSelectedView2dContainer();
        if (pane != null) {
            pane.resetMaximizedSelectedImagePane(this);
        }
        Object oldUnit = actionsInView.get(ActionW.SPATIAL_UNIT.cmd());
        initActionWState();
        imageLayer.fireOpEvent(new ImageOpEvent(ImageOpEvent.OpEvent.ResetDisplay, series, getImage(), null));
        resetZoom();
        resetPan();
        imageLayer.setEnableDispOperations(true);
        eventManager.updateComponentsListener(this);
        // When pixel unit is reset
        if (!Objects.equals(oldUnit, actionsInView.get(ActionW.SPATIAL_UNIT.cmd()))) {
            graphicManager.updateLabels(Boolean.TRUE, this);
        }
    }

    @Override
    public List<ViewButton> getViewButtons() {
        return viewButtons;
    }

    protected void copyGraphicsFromClipboard() {
        List<Graphic> graphs = DefaultView2d.GRAPHIC_CLIPBOARD.getGraphics();
        if (graphs != null) {
            Rectangle2D area = getViewModel().getModelArea();
            if (graphs.stream().anyMatch(g -> !g.getBounds(null).intersects(area))) {
                int option = JOptionPane.showConfirmDialog(this,
                    "At least one graphic is outside the image.\n Do you want to continue?"); //$NON-NLS-1$
                if (option != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            graphs.forEach(g -> AbstractGraphicModel.addGraphicToModel(this, g.copy()));

            // Repaint all because labels are not drawn
            repaint();
        }
    }

    public static Cursor getNewCursor(int type) {
        return new Cursor(type);
    }

    public static Cursor getCustomCursor(String filename, String cursorName, int hotSpotX, int hotSpotY) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        ImageIcon icon = new ImageIcon(DefaultView2d.class.getResource("/icon/cursor/" + filename)); //$NON-NLS-1$
        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        Point hotSpot = new Point((hotSpotX * bestCursorSize.width) / icon.getIconWidth(),
            (hotSpotY * bestCursorSize.height) / icon.getIconHeight());
        return defaultToolkit.createCustomCursor(icon.getImage(), hotSpot, cursorName);
    }

}
