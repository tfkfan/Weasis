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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
import org.dcm4che3.util.UIDUtils;
import org.opencv.osgi.OpenCVNativeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.media.data.Codec;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.StringUtil;
import org.weasis.dicom.codec.DicomCodec;
import org.weasis.imageio.codec.ImageioCodec;

import com.sun.media.imageioimpl.common.ImageioUtil;
import com.sun.media.imageioimpl.stream.ChannelImageInputStreamSpi;
import com.sun.media.imageioimpl.stream.ChannelImageOutputStreamSpi;

public class RenderingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderingManager.class);

    public RenderingManager(File nativeLib) {
        initNativeLibs(nativeLib);
        initLibraries();
    }

    public static void setSystemSpecification() {
        // Follows the OSGI specification to use Bundle-NativeCode in the bundle fragment :
        // http://www.osgi.org/Specifications/Reference
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        if (osName != null && !osName.trim().equals("") && osArch != null && !osArch.trim().equals("")) {
            if (osName.toLowerCase().startsWith("win")) {
                // All Windows versions with a specific processor architecture (x86 or x86-64) are grouped under
                // windows. If you need to make different native libraries for the Windows versions, define it in the
                // Bundle-NativeCode tag of the bundle fragment.
                osName = "windows";
            } else if (osName.equals("Mac OS X")) {
                osName = "macosx";
            } else if (osName.equals("SymbianOS")) {
                osName = "epoc32";
            } else if (osName.equals("hp-ux")) {
                osName = "hpux";
            } else if (osName.equals("Mac OS")) {
                osName = "macos";
            } else if (osName.equals("OS/2")) {
                osName = "os2";
            } else if (osName.equals("procnto")) {
                osName = "qnx";
            } else {
                osName = osName.toLowerCase();
            }

            if (osArch.equals("pentium") || osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586")
                || osArch.equals("i686")) {
                osArch = "x86";
            } else if (osArch.equals("amd64") || osArch.equals("em64t") || osArch.equals("x86_64")) {
                osArch = "x86-64";
            } else if (osArch.equals("power ppc")) {
                osArch = "powerpc";
            } else if (osArch.equals("psc1k")) {
                osArch = "ignite";
            } else {
                osArch = osArch.toLowerCase();
            }
            System.setProperty("native.library.spec", osName + "-" + osArch); //$NON-NLS-2$
        }
    }

    private static void registerCodecPlugins(Codec codec) {
        if (codec != null && !BundleTools.CODEC_PLUGINS.contains(codec)) {
            BundleTools.CODEC_PLUGINS.add(codec);
            LOGGER.info("Register Image Codec Plug-in: {}", codec.getCodecName());
        }
    }

    public static void copyDirectory(String fromPath, String toPath) throws IOException {

        CopyOption[] options =
            new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };
        Path from = Paths.get(fromPath);
        Path to = Paths.get(toPath);
        if (!to.toFile().exists()) {
            Files.copy(from, to, options);
        }
        for (File item : from.toFile().listFiles()) {
            if (item.isDirectory()) {
                copyDirectory(item.toString(), toPath + File.separator + item.getName());
            } else {
                Files.copy(Paths.get(item.toString()), Paths.get(toPath + File.separator + item.getName()), options);
            }
        }
    }

    private void initNativeLibs(File nativeLib) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File dir;
        String folder = "weasis-web-" + Integer.toHexString(hashCode());
        if (tempDir == null || tempDir.length() == 1) {
            dir = new File(System.getProperty("user.home", ""), folder);
        } else {
            dir = new File(tempDir, folder);
        }
        System.setProperty("weasis-web.tmp.dir", dir.getPath());
        try {
            copyDirectory(nativeLib.getPath(), dir.getPath());
        } catch (IOException e) {
            LOGGER.error("copy native libs", e);
        }

        String path = new File(dir, "lib").getPath();
        String oldSysPaths = System.getProperty("java.library.path");
        if (StringUtil.hasText(oldSysPaths)) {
            path = oldSysPaths + File.pathSeparator + path;
        }

        System.setProperty("java.library.path", path);
        Field sysPath;
        try {
            // Trick to reload JMV setting for "java.library.path"
            sysPath = ClassLoader.class.getDeclaredField("sys_paths");
            sysPath.setAccessible(true);
            sysPath.set(null, null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            LOGGER.error("Cannot reload java.library.path", e);
        }

        // Load OpenCV
        OpenCVNativeLoader loader = new OpenCVNativeLoader();
        loader.init();
    }

    private void initLibraries() {
        initImageioCodec();
        initDicomCodec();
    }

    private void initImageioCodec() {
        LOGGER.info("Activate ImageioCodec");
        // Do not use cache. Images must be download locally before reading them.
        ImageIO.setUseCache(false);

        // SPI Issue Resolution
        // Register imageio SPI with the classloader of this bundle
        // and unregister imageio SPI if imageio.jar is also in the jre/lib/ext folder

        Class<?>[] jaiCodecs = { ChannelImageInputStreamSpi.class, ChannelImageOutputStreamSpi.class };

        for (Class<?> c : jaiCodecs) {
            ImageioUtil.registerServiceProvider(c);
        }
        registerCodecPlugins(new ImageioCodec());
    }

    private void initDicomCodec() {
        LOGGER.info("Activate DicomCodec");

        /**
         * Set value for dicom root UID which should be registered at the
         * http://www.iana.org/assignments/enterprise-numbers <br>
         * Default value is 2.25, this enables users to generate OIDs without any registration procedure
         *
         * @see http://www.dclunie.com/medical-image-faq/html/part2.html#UUID <br>
         *      http://www.oid-info.com/get/2.25 <br>
         *      http://www.itu.int/ITU-T/asn1/uuid.html<br>
         *      http://healthcaresecprivacy.blogspot.ch/2011/02/creating-and-using-unique-id-uuid-oid.html
         */
        String weasisRootUID = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.dicom.root.uid", UIDUtils.getRoot());
        UIDUtils.setRoot(weasisRootUID);

        // Register SPI in imageio registry with the classloader of this bundle (provides also the classpath for
        // discovering the SPI files). Here are the codecs:
        ImageioUtil.registerServiceProvider(new DicomImageReaderSpi());
        registerCodecPlugins(new DicomCodec());
    }

}
