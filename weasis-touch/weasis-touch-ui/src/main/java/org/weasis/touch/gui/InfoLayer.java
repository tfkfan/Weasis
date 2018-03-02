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

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Optional;

import javax.vecmath.Vector3d;

import org.dcm4che3.data.Tag;
import org.weasis.core.api.explorer.model.TreeModelNode;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.Filter;
import org.weasis.core.api.gui.util.FxUtil;
import org.weasis.core.api.image.OpManager;
import org.weasis.core.api.image.WindowOp;
import org.weasis.core.api.image.util.Unit;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.TagView;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.LangUtil;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.api.util.StringUtil.Suffix;
import org.weasis.core.ui.editor.image.PixelInfo;
import org.weasis.core.ui.model.layer.LayerAnnotation;
import org.weasis.core.ui.util.DecFormater;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.PresentationStateReader;
import org.weasis.dicom.codec.RejectedKOSpecialElement;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.codec.display.CornerDisplay;
import org.weasis.dicom.codec.display.CornerInfoData;
import org.weasis.dicom.codec.display.Modality;
import org.weasis.dicom.codec.display.ModalityInfoData;
import org.weasis.dicom.codec.display.ModalityView;
import org.weasis.dicom.codec.geometry.ImageOrientation;
import org.weasis.dicom.codec.geometry.ImageOrientation.Label;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.opencv.data.PlanarImage;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * The Class InfoLayer.
 *
 * @author Nicolas Roduit
 */
public class InfoLayer {
    protected static final int BORDER = 10;
    protected static final Color highlight = Color.rgb(255, 153, 153);

    protected final HashMap<String, Boolean> displayPreferences = new HashMap<>();
    protected final MainCanvas view2DPane;
    protected final Rectangle pixelInfoBound;

    protected boolean visible = true;
    protected PixelInfo pixelInfo = null;
    protected int border = BORDER;
    protected double thickLength = 15.0;
    protected boolean showBottomScale = true;
    protected String name;

    public InfoLayer(MainCanvas view2DPane) {
        this.view2DPane = view2DPane;
        this.pixelInfoBound = new Rectangle();

        displayPreferences.put(LayerAnnotation.ANNOTATIONS, true);
        displayPreferences.put(LayerAnnotation.MIN_ANNOTATIONS, false);
        displayPreferences.put(LayerAnnotation.ANONYM_ANNOTATIONS, false);
        displayPreferences.put(LayerAnnotation.SCALE, true);
        displayPreferences.put(LayerAnnotation.IMAGE_ORIENTATION, true);
        displayPreferences.put(LayerAnnotation.WINDOW_LEVEL, true);
        displayPreferences.put(LayerAnnotation.ZOOM, true);
        displayPreferences.put(LayerAnnotation.ROTATION, false);
        displayPreferences.put(LayerAnnotation.FRAME, true);
        displayPreferences.put(LayerAnnotation.PIXEL, true);

    }

    public Boolean getDisplayPreferences(String item) {
        return Optional.ofNullable(displayPreferences.getOrDefault(item, null)).orElse(Boolean.FALSE);
    }

    public void draw(GraphicsContext gc, DicomImageElement image) {
        if (!visible || image == null) {
            return;
        }
        OpManager disOp = view2DPane.getDisplayOpManager();
        ModalityInfoData modality;
        Modality mod = Modality.getModality(TagD.getTagValue(view2DPane.getSeries(), Tag.Modality, String.class));
        modality = ModalityView.getModlatityInfos(mod);

        final Bounds bound = view2DPane.getBoundsInLocal();
        double midx = bound.getWidth() / 2;
        double midy = bound.getHeight() / 2;

        Font font = FxUtil.getArialFont();
        gc.setFont(font);
        final double fontHeight = FxUtil.getFontHeight(font);
        thickLength = font.getSize() * 1.5f; // font 10 => 15 pixels
        thickLength = thickLength < 5.0 ? 5.0 : thickLength;

        gc.setFill(Color.BLACK);

        boolean hideMin = !getDisplayPreferences(LayerAnnotation.MIN_ANNOTATIONS);

        final double midfontHeight = fontHeight * 0.35;
        double drawY = bound.getHeight() - border - 1.5; // -1.5 for outline

        if (!image.isReadable()) {
            String message = "Cannot read this media!";
            double y = midy;
            FxUtil.paintColorFontOutline(gc, message, midx - FxUtil.getTextBounds(message, font).getWidth() / 2, y,
                Color.RED);
            String tsuid = TagD.getTagValue(image, Tag.TransferSyntaxUID, String.class);
            if (StringUtil.hasText(tsuid)) {
                tsuid = "Cannot read this media!" + StringUtil.COLON_AND_SPACE + tsuid;
                y += fontHeight;
                FxUtil.paintColorFontOutline(gc, tsuid, midx - FxUtil.getTextBounds(tsuid, font).getWidth() / 2, y,
                    Color.RED);
            }

            String[] desc = image.getMediaReader().getReaderDescription();
            if (desc != null) {
                for (String str : desc) {
                    if (StringUtil.hasText(str)) {
                        y += fontHeight;
                        FxUtil.paintColorFontOutline(gc, str, midx - FxUtil.getTextBounds(str, font).getWidth() / 2, y,
                            Color.RED);
                    }
                }
            }
        }

        if (image.isReadable() && getDisplayPreferences(LayerAnnotation.SCALE)) {
            drawScale(gc, image, bound, fontHeight);
        }

        if (image != null) {
            /*
             * IHE BIR RAD TF-­‐2: 4.16.4.2.2.5.8
             *
             * Whether or not lossy compression has been applied, derived from Lossy Image 990 Compression (0028,2110),
             * and if so, the value of Lossy Image Compression Ratio (0028,2112) and Lossy Image Compression Method
             * (0028,2114), if present (as per FDA Guidance for the Submission Of Premarket Notifications for Medical
             * Image Management Devices, July 27, 2000).
             */
            drawY -= fontHeight;
            if ("01".equals(TagD.getTagValue(image, Tag.LossyImageCompression))) { //$NON-NLS-1$
                double[] rates = TagD.getTagValue(image, Tag.LossyImageCompressionRatio, double[].class);
                StringBuilder buf = new StringBuilder("Lossy compression");
                buf.append(StringUtil.COLON_AND_SPACE);
                if (rates != null && rates.length > 0) {
                    for (int i = 0; i < rates.length; i++) {
                        if (i > 0) {
                            buf.append(","); //$NON-NLS-1$
                        }
                        buf.append(" ["); //$NON-NLS-1$
                        buf.append(Math.round(rates[i]));
                        buf.append(":1"); //$NON-NLS-1$
                        buf.append(']');
                    }
                } else {
                    String val = TagD.getTagValue(image, Tag.DerivationDescription, String.class);
                    if (val != null) {
                        buf.append(StringUtil.getTruncatedString(val, 25, Suffix.THREE_PTS));
                    }
                }

                FxUtil.paintColorFontOutline(gc, buf.toString(), border, drawY, Color.RED);
                drawY -= fontHeight;
            }

            Integer frame = TagD.getTagValue(image, Tag.InstanceNumber, Integer.class);
            RejectedKOSpecialElement koElement = DicomModel.getRejectionKoSpecialElement(view2DPane.getSeries(),
                TagD.getTagValue(image, Tag.SOPInstanceUID, String.class), frame);

            if (koElement != null) {
                double y = midy;
                String message = "Not a valid image: " + koElement.getDocumentTitle(); //$NON-NLS-1$
                FxUtil.paintColorFontOutline(gc, message, midx - FxUtil.getTextBounds(message, font).getWidth() / 2, y,
                    Color.RED);
            }
        }

        if (getDisplayPreferences(LayerAnnotation.PIXEL) && hideMin) {
            StringBuilder sb = new StringBuilder("Pixel");
            sb.append(StringUtil.COLON_AND_SPACE);
            if (pixelInfo != null) {
                sb.append(pixelInfo.getPixelValueText());
                sb.append(" - "); //$NON-NLS-1$
                sb.append(pixelInfo.getPixelPositionText());
            }
            String str = sb.toString();
            FxUtil.paintFontOutline(gc, str, border, drawY - 1);
            drawY -= fontHeight + 2;
            pixelInfoBound.setBounds(border - 2, (int) drawY + 3,
                (int) (FxUtil.getTextBounds(str, font).getWidth() + 4), (int) fontHeight + 2);
        }
        if (getDisplayPreferences(LayerAnnotation.WINDOW_LEVEL) && hideMin) {
            StringBuilder sb = new StringBuilder();
            Number window = (Number) disOp.getParamValue(WindowOp.OP_NAME, ActionW.WINDOW.cmd());
            Number level = (Number) disOp.getParamValue(WindowOp.OP_NAME, ActionW.LEVEL.cmd());
            boolean outside = false;
            if (window != null && level != null) {
                sb.append(ActionW.WINLEVEL.getTitle());
                sb.append(StringUtil.COLON_AND_SPACE);
                sb.append(DecFormater.oneDecimal(window));
                sb.append("/");//$NON-NLS-1$
                sb.append(DecFormater.oneDecimal(level));

                if (image != null) {
                    PresentationStateReader prReader =
                        (PresentationStateReader) view2DPane.getActionValue(PresentationStateReader.TAG_PR_READER);
                    boolean pixelPadding =
                        (Boolean) disOp.getParamValue(WindowOp.OP_NAME, ActionW.IMAGE_PIX_PADDING.cmd());
                    double minModLUT = image.getMinValue(prReader, pixelPadding);
                    double maxModLUT = image.getMaxValue(prReader, pixelPadding);
                    double minp = level.doubleValue() - window.doubleValue() / 2.0;
                    double maxp = level.doubleValue() + window.doubleValue() / 2.0;
                    if (minp > maxModLUT || maxp < minModLUT) {
                        outside = true;
                        sb.append(" - "); //$NON-NLS-1$
                        sb.append("Values outside of the image spectrum!");
                    }
                }
            }
            if (outside) {
                FxUtil.paintColorFontOutline(gc, sb.toString(), border, drawY, Color.RED);
            } else {
                FxUtil.paintFontOutline(gc, sb.toString(), border, drawY);
            }
            drawY -= fontHeight;
        }
        if (getDisplayPreferences(LayerAnnotation.ZOOM) && hideMin) {
            FxUtil.paintFontOutline(gc,
                "Zoom" + StringUtil.COLON_AND_SPACE + DecFormater.percentTwoDecimal(view2DPane.getViewScale()), border,
                drawY);
            drawY -= fontHeight;
        }

        if (getDisplayPreferences(LayerAnnotation.FRAME) && hideMin) {
            StringBuilder buf = new StringBuilder("Frame");
            buf.append(StringUtil.COLON_AND_SPACE);
            if (image != null) {
                Integer inst = TagD.getTagValue(image, Tag.InstanceNumber, Integer.class);
                if (inst != null) {
                    buf.append("["); //$NON-NLS-1$
                    buf.append(inst);
                    buf.append("] "); //$NON-NLS-1$
                }
            }
            buf.append(view2DPane.getFrameIndex() + 1);
            buf.append(" / "); //$NON-NLS-1$
            buf.append(view2DPane.getSeries()
                .size((Filter<DicomImageElement>) view2DPane.getActionValue(ActionW.FILTERED_SERIES.cmd())));
            FxUtil.paintFontOutline(gc, buf.toString(), border, drawY);
            drawY -= fontHeight;
        }
        Point2D.Double[] positions = new Point2D.Double[4];
        positions[3] = new Point2D.Double(border, drawY - 5);

        if (getDisplayPreferences(LayerAnnotation.ANNOTATIONS) && image != null) {
            Series series = (Series) view2DPane.getSeries();
            MediaSeriesGroup study = getParent(series, DicomModel.study);
            MediaSeriesGroup patient = getParent(series, DicomModel.patient);
            CornerInfoData corner = modality.getCornerInfo(CornerDisplay.TOP_LEFT);
            boolean anonymize = getDisplayPreferences(LayerAnnotation.ANONYM_ANNOTATIONS);
            drawY = fontHeight;
            TagView[] infos = corner.getInfos();
            for (int j = 0; j < infos.length; j++) {
                if (infos[j] != null) {
                    if (hideMin || infos[j].containsTag(TagD.get(Tag.PatientName))) {
                        for (TagW tag : infos[j].getTag()) {
                            if (!anonymize || tag.getAnonymizationType() != 1) {
                                Object value = getTagValue(tag, patient, study, series, image);
                                if (value != null) {
                                    String str = tag.getFormattedTagValue(value, infos[j].getFormat());
                                    if (StringUtil.hasText(str)) {
                                        FxUtil.paintFontOutline(gc, str, border, drawY);
                                        drawY += fontHeight;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            positions[0] = new Point2D.Double(border, drawY - fontHeight + 5);

            corner = modality.getCornerInfo(CornerDisplay.TOP_RIGHT);
            drawY = fontHeight;
            infos = corner.getInfos();
            for (int j = 0; j < infos.length; j++) {
                if (infos[j] != null) {
                    if (hideMin || infos[j].containsTag(TagD.get(Tag.SeriesDate))) {
                        Object value;
                        for (TagW tag : infos[j].getTag()) {
                            if (!anonymize || tag.getAnonymizationType() != 1) {
                                value = getTagValue(tag, patient, study, series, image);
                                if (value != null) {
                                    String str = tag.getFormattedTagValue(value, infos[j].getFormat());
                                    if (StringUtil.hasText(str)) {
                                        FxUtil.paintFontOutline(gc, str,
                                            bound.getWidth() - FxUtil.getTextBounds(str, font).getWidth() - border,
                                            drawY);
                                        drawY += fontHeight;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            positions[1] = new Point2D.Double(bound.getWidth() - border, drawY - fontHeight + 5);

            drawY = bound.getHeight() - border - 1.5f; // -1.5 for outline
            if (hideMin) {
                corner = modality.getCornerInfo(CornerDisplay.BOTTOM_RIGHT);
                infos = corner.getInfos();
                for (int j = infos.length - 1; j >= 0; j--) {
                    if (infos[j] != null) {
                        Object value;
                        for (TagW tag : infos[j].getTag()) {
                            if (!anonymize || tag.getAnonymizationType() != 1) {
                                value = getTagValue(tag, patient, study, series, image);
                                if (value != null) {
                                    String str = tag.getFormattedTagValue(value, infos[j].getFormat());
                                    if (StringUtil.hasText(str)) {
                                        FxUtil.paintFontOutline(gc, str,
                                            bound.getWidth() - FxUtil.getTextBounds(str, font).getWidth() - border,
                                            drawY);
                                        drawY -= fontHeight;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                drawY -= 5;
            }
            positions[2] = new Point2D.Double(bound.getWidth() - border, drawY - 5);

            // Boolean synchLink = (Boolean) view2DPane.getActionValue(ActionW.SYNCH_LINK);
            // String str = synchLink != null && synchLink ? "linked" : "unlinked";
            // //$NON-NLS-1$ //$NON-NLS-2$
            // paintFontOutline(g2, str, bound.width - g2.getFontMetrics().stringWidth(str)
            // - BORDER, drawY);

            double[] v = TagD.getTagValue(image, Tag.ImageOrientationPatient, double[].class);
            Integer columns = TagD.getTagValue(image, Tag.Columns, Integer.class);
            Integer rows = TagD.getTagValue(image, Tag.Rows, Integer.class);
            StringBuilder orientation = new StringBuilder(mod.name());
            if (rows != null && columns != null) {
                orientation.append(" (");//$NON-NLS-1$
                orientation.append(columns);
                orientation.append("x");//$NON-NLS-1$
                orientation.append(rows);
                orientation.append(")");//$NON-NLS-1$

            }
            String colLeft = null;
            String rowTop = null;
            if (getDisplayPreferences(LayerAnnotation.IMAGE_ORIENTATION) && v != null && v.length == 6) {
                orientation.append(" - ");//$NON-NLS-1$
                Label imgOrientation = ImageOrientation.makeImageOrientationLabelFromImageOrientationPatient(v[0], v[1],
                    v[2], v[3], v[4], v[5]);
                orientation.append(imgOrientation);

                // Set the opposite vector direction (otherwise label should be placed in
                // mid-right and mid-bottom
                Vector3d vr = new Vector3d(-v[0], -v[1], -v[2]);
                Vector3d vc = new Vector3d(-v[3], -v[4], -v[5]);

                Integer rotationAngle = (Integer) view2DPane.getActionValue(ActionW.ROTATION.cmd());
                if (rotationAngle != null && rotationAngle != 0) {
                    double rad = Math.toRadians(rotationAngle);
                    double[] normal = ImageOrientation.computeNormalVectorOfPlan(v);
                    if (normal != null && normal.length == 3) {
                        Vector3d result = new Vector3d(0.0, 0.0, 0.0);
                        Vector3d axis = new Vector3d(normal);
                        rotate(vr, axis, -rad, result);
                        vr = result;

                        result = new Vector3d(0.0, 0.0, 0.0);
                        rotate(vc, axis, -rad, result);
                        vc = result;
                    }
                }

                if (LangUtil.getNULLtoFalse((Boolean) view2DPane.getActionValue((ActionW.FLIP.cmd())))) {
                    vr.x = -vr.x;
                    vr.y = -vr.y;
                    vr.z = -vr.z;
                }

                colLeft = ImageOrientation.makePatientOrientationFromPatientRelativeDirectionCosine(vr.x, vr.y, vr.z);
                rowTop = ImageOrientation.makePatientOrientationFromPatientRelativeDirectionCosine(vc.x, vc.y, vc.z);

            } else {
                String[] po = TagD.getTagValue(image, Tag.PatientOrientation, String[].class);
                if (po != null && po.length == 2) {
                    // Do not display if there is a transformation
                    if (LangUtil.getNULLtoFalse((Boolean) view2DPane.getActionValue((ActionW.FLIP.cmd())))) {
                        colLeft = po[0];
                    } else {
                        StringBuilder buf = new StringBuilder();
                        for (char c : po[0].toCharArray()) {
                            buf.append(ImageOrientation.getImageOrientationOposite(c));
                        }
                        colLeft = buf.toString();
                    }
                    StringBuilder buf = new StringBuilder();
                    for (char c : po[1].toCharArray()) {
                        buf.append(ImageOrientation.getImageOrientationOposite(c));
                    }
                    rowTop = buf.toString();
                }
            }
            if (rowTop != null && colLeft != null) {
                if (colLeft.length() < 1) {
                    colLeft = " "; //$NON-NLS-1$
                }
                if (rowTop.length() < 1) {
                    rowTop = " "; //$NON-NLS-1$
                }
                Font oldFont = gc.getFont();
                Font bigFont = Font.font(oldFont.getSize() + 5.0);
                gc.setFont(bigFont);
                String fistLetter = rowTop.substring(0, 1);
                FxUtil.paintColorFontOutline(gc, fistLetter, midx, fontHeight + 5, highlight);
                double shiftx = FxUtil.getTextBounds(fistLetter, bigFont).getWidth();
                if (rowTop.length() > 1) {
                    gc.setFont(oldFont);
                    FxUtil.paintColorFontOutline(gc, rowTop.substring(1, rowTop.length()), midx + shiftx,
                        fontHeight + 10, highlight);
                    gc.setFont(bigFont);
                }

                FxUtil.paintColorFontOutline(gc, colLeft.substring(0, 1), border + thickLength,
                    midy + fontHeight / 2.0f, highlight);

                if (colLeft.length() > 1) {
                    gc.setFont(oldFont);
                    FxUtil.paintColorFontOutline(gc, colLeft.substring(1, colLeft.length()),
                        border + thickLength + shiftx, midy + fontHeight / 2.0f + 5, highlight);
                }
                gc.setFont(oldFont);
            }

            FxUtil.paintFontOutline(gc, orientation.toString(), border, bound.getHeight() - border - 1.5f); // -1.5 for
                                                                                                            // outline
        } else {
            positions[0] = new Point2D.Double(border, border);
            positions[1] = new Point2D.Double(bound.getWidth() - border, border);
            positions[2] = new Point2D.Double(bound.getWidth() - border, bound.getHeight() - border);
        }
    }

    private MediaSeriesGroup getParent(Series series, TreeModelNode node) {
        if (series != null) {
            Object tagValue = series.getTagValue(TagW.ExplorerModel);
            if (tagValue instanceof DicomModel) {
                return ((DicomModel) tagValue).getParent(series, node);
            }
        }
        return null;
    }

    private static void rotate(Vector3d vSrc, Vector3d axis, double angle, Vector3d vDst) {
        axis.normalize();
        vDst.x = axis.x * (axis.x * vSrc.x + axis.y * vSrc.y + axis.z * vSrc.z) * (1 - Math.cos(angle))
            + vSrc.x * Math.cos(angle) + (-axis.z * vSrc.y + axis.y * vSrc.z) * Math.sin(angle);
        vDst.y = axis.y * (axis.x * vSrc.x + axis.y * vSrc.y + axis.z * vSrc.z) * (1 - Math.cos(angle))
            + vSrc.y * Math.cos(angle) + (axis.z * vSrc.x - axis.x * vSrc.z) * Math.sin(angle);
        vDst.z = axis.z * (axis.x * vSrc.x + axis.y * vSrc.y + axis.z * vSrc.z) * (1 - Math.cos(angle))
            + vSrc.z * Math.cos(angle) + (-axis.y * vSrc.x + axis.x * vSrc.y) * Math.sin(angle);
    }

    private Object getTagValue(TagW tag, MediaSeriesGroup patient, MediaSeriesGroup study, Series series,
        ImageElement image) {
        if (tag != null) {
            if (image.containTagKey(tag)) {
                return image.getTagValue(tag);
            }
            if (series.containTagKey(tag)) {
                return series.getTagValue(tag);
            }
            if (study != null && study.containTagKey(tag)) {
                return study.getTagValue(tag);
            }
            if (patient != null && patient.containTagKey(tag)) {
                return patient.getTagValue(tag);
            }
        }
        return null;
    }

    public void drawScale(GraphicsContext gc, ImageElement image, Bounds bound, double fontHeight) {
        PlanarImage source = image.getImage();
        if (source == null) {
            return;
        }

        double zoomFactor = view2DPane.getViewScale();

        double scale = image.getPixelSize() / zoomFactor;
        double scaleSizex = ajustShowScale(scale,
            (int) Math.min(zoomFactor * source.width() * image.getRescaleX(), bound.getWidth() / 2.0));
        double strokeWidth = gc.getFont().getSize() / 10.0;
        strokeWidth = strokeWidth < 1.5 ? 1.5 : strokeWidth;

        if (showBottomScale && scaleSizex > 50.0d) {
            Unit[] unit = { image.getPixelSpacingUnit() };
            String str = ajustLengthDisplay(scaleSizex * scale, unit);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(strokeWidth);

            double posx = bound.getWidth() / 2.0 - scaleSizex / 2.0;
            double posy = bound.getHeight() - border - 1.5; // - 1.5 is for outline
            Line2D line = new Line2D.Double(posx, posy, posx + scaleSizex, posy);
            drawOutLine(gc, line);
            line.setLine(posx, posy - thickLength, posx, posy);
            drawOutLine(gc, line);
            line.setLine(posx + scaleSizex, posy - thickLength, posx + scaleSizex, posy);
            drawOutLine(gc, line);
            int divisor = str.indexOf("5") == -1 ? str.indexOf("2") == -1 ? 10 : 2 : 5; //$NON-NLS-1$ //$NON-NLS-2$
            double midThick = thickLength * 2.0 / 3.0;
            double smallThick = thickLength / 3.0;
            double divSquare = scaleSizex / divisor;
            for (int i = 1; i < divisor; i++) {
                line.setLine(posx + divSquare * i, posy, posx + divSquare * i, posy - midThick);
                drawOutLine(gc, line);
            }
            if (divSquare > 90) {
                double secondSquare = divSquare / 10.0;
                for (int i = 0; i < divisor; i++) {
                    for (int k = 1; k < 10; k++) {
                        double secBar = posx + divSquare * i + secondSquare * k;
                        line.setLine(secBar, posy, secBar, posy - smallThick);
                        drawOutLine(gc, line);
                    }
                }
            }

            gc.setStroke(Color.WHITE);
            line.setLine(posx, posy, posx + scaleSizex, posy);
            drawLine(gc, line);
            line.setLine(posx, posy - thickLength, posx, posy);
            drawLine(gc, line);
            line.setLine(posx + scaleSizex, posy - thickLength, posx + scaleSizex, posy);
            drawLine(gc, line);

            for (int i = 0; i < divisor; i++) {
                line.setLine(posx + divSquare * i, posy, posx + divSquare * i, posy - midThick);
                drawLine(gc, line);
            }
            if (divSquare > 90) {
                double secondSquare = divSquare / 10.0;
                for (int i = 0; i < divisor; i++) {
                    for (int k = 1; k < 10; k++) {
                        double secBar = posx + divSquare * i + secondSquare * k;
                        line.setLine(secBar, posy, secBar, posy - smallThick);
                        drawLine(gc, line);
                    }
                }
            }

            String pixSizeDesc = image.getPixelSizeCalibrationDescription();
            if (StringUtil.hasText(pixSizeDesc)) {
                FxUtil.paintFontOutline(gc, pixSizeDesc, posx + scaleSizex + 5, posy - fontHeight);
            }
            str += " " + unit[0].getAbbreviation(); //$NON-NLS-1$
            FxUtil.paintFontOutline(gc, str, posx + scaleSizex + 5, posy);
        }

        double scaleSizeY = ajustShowScale(scale,
            (int) Math.min(zoomFactor * source.height() * image.getRescaleY(), bound.getHeight() / 2.0));

        if (scaleSizeY > 30.0d) {
            Unit[] unit = { image.getPixelSpacingUnit() };
            String str = ajustLengthDisplay(scaleSizeY * scale, unit);

            gc.setLineWidth(strokeWidth);
            gc.setStroke(Color.BLACK);

            double posx = border - 1.5; // -1.5 for outline
            double posy = bound.getHeight() / 2.0 - scaleSizeY / 2.0;
            Line2D line = new Line2D.Double(posx, posy, posx, posy + scaleSizeY);
            drawOutLine(gc, line);
            line.setLine(posx, posy, posx + thickLength, posy);
            drawOutLine(gc, line);
            line.setLine(posx, posy + scaleSizeY, posx + thickLength, posy + scaleSizeY);
            drawOutLine(gc, line);
            int divisor = str.indexOf("5") == -1 ? str.indexOf("2") == -1 ? 10 : 2 : 5; //$NON-NLS-1$ //$NON-NLS-2$
            double divSquare = scaleSizeY / divisor;
            double midThick = thickLength * 2.0 / 3.0;
            double smallThick = thickLength / 3.0;
            for (int i = 0; i < divisor; i++) {
                line.setLine(posx, posy + divSquare * i, posx + midThick, posy + divSquare * i);
                drawOutLine(gc, line);
            }
            if (divSquare > 90) {
                double secondSquare = divSquare / 10.0;
                for (int i = 0; i < divisor; i++) {
                    for (int k = 1; k < 10; k++) {
                        double secBar = posy + divSquare * i + secondSquare * k;
                        line.setLine(posx, secBar, posx + smallThick, secBar);
                        drawOutLine(gc, line);
                    }
                }
            }

            gc.setStroke(Color.WHITE);
            line.setLine(posx, posy, posx, posy + scaleSizeY);
            drawLine(gc, line);
            line.setLine(posx, posy, posx + thickLength, posy);
            drawLine(gc, line);
            line.setLine(posx, posy + scaleSizeY, posx + thickLength, posy + scaleSizeY);
            drawLine(gc, line);
            for (int i = 0; i < divisor; i++) {
                line.setLine(posx, posy + divSquare * i, posx + midThick, posy + divSquare * i);
                drawLine(gc, line);
            }
            if (divSquare > 90) {
                double secondSquare = divSquare / 10.0;
                for (int i = 0; i < divisor; i++) {
                    for (int k = 1; k < 10; k++) {
                        double secBar = posy + divSquare * i + secondSquare * k;
                        line.setLine(posx, secBar, posx + smallThick, secBar);
                        drawLine(gc, line);
                    }
                }
            }
            FxUtil.paintFontOutline(gc, str + " " + unit[0].getAbbreviation(), posx, //$NON-NLS-1$
                posy - 5 * strokeWidth);
        }

    }

    public void drawOutLine(GraphicsContext gc, Line2D l) {
        Rectangle2D r = l.getBounds2D();
        gc.strokeRect(r.getX() - 1.0, r.getY() - 1.0, r.getWidth() + 2.0, r.getHeight() + 2.0);
    }

    public void drawLine(GraphicsContext gc, Line2D l) {
        gc.strokeLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    private double ajustShowScale(double ratio, int maxLength) {
        int digits = (int) ((Math.log(maxLength * ratio) / Math.log(10)) + 1);
        double scaleLength = Math.pow(10, digits);
        double scaleSize = scaleLength / ratio;

        int loop = 0;
        while ((int) scaleSize > maxLength) {
            scaleLength /= findGeometricSuite(scaleLength);
            scaleSize = scaleLength / ratio;
            loop++;
            if (loop > 50) {
                return 0.0;
            }
        }
        return scaleSize;
    }
    
    public static double findGeometricSuite(double length) {
        int shift = (int) ((Math.log(length) / Math.log(10)) + 0.1);
        int firstDigit = (int) (length / Math.pow(10, shift) + 0.5);
        if (firstDigit == 5) {
            return 2.5;
        }
        return 2.0;

    }

    public static String ajustLengthDisplay(double scaleLength, Unit[] unit) {
        double ajustScaleLength = scaleLength;

        Unit ajustUnit = unit[0];

        if (scaleLength < 1.0) {
            Unit down = ajustUnit;
            while ((down = down.getDownUnit()) != null) {
                double length = scaleLength * down.getConversionRatio(unit[0].getConvFactor());
                if (length > 1) {
                    ajustUnit = down;
                    ajustScaleLength = length;
                    break;
                }
            }
        } else if (scaleLength > 10.0) {
            Unit up = ajustUnit;
            while ((up = up.getUpUnit()) != null) {
                double length = scaleLength * up.getConversionRatio(unit[0].getConvFactor());
                if (length < 1) {
                    break;
                }
                ajustUnit = up;
                ajustScaleLength = length;
            }
        }
        // Trick to keep the value as a return parameter
        unit[0] = ajustUnit;
        if (ajustScaleLength < 1.0) {
            return ajustScaleLength < 0.001 ? DecFormater.scientificFormat(ajustScaleLength)
                : DecFormater.fourDecimal(ajustScaleLength);
        }
        return ajustScaleLength > 50000.0 ? DecFormater.scientificFormat(ajustScaleLength)
            : DecFormater.twoDecimal(ajustScaleLength);
    }

}
