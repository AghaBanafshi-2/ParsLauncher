package net.kdt.pojavlaunch.tasks;


import static net.kdt.pojavlaunch.Architecture.archAsString;
import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AsyncAssetManager {

    private AsyncAssetManager(){}

    /**
     * Attempt to install the java 8 runtime, if necessary
     * @param am App context
     */
    private static final String RUNTIME_RELEASE_BASE =
            "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/";

    private static final RuntimeBundle[] RUNTIME_BUNDLES = {
            new RuntimeBundle("Internal", 8, "jre8-pojav.zip",
                    "a97cf40dc2eb1e642c3b5188260d7c42908a8f603556cc52492bf29d03eab6b3"),
            new RuntimeBundle("Internal-17", 17, "jre17-pojav.zip",
                    "c0f1cf02a567b07ea1e3bba2114eec1657b6e51513c9af7958cae27e4f614440"),
            new RuntimeBundle("Internal-21", 21, "jre21-pojav.zip",
                    "66f909c75319ba538b3181da15b6bf2ec3419f9628b9e9e0670a1bf2de5d7550")
    };

    private static final class RuntimeBundle {
        final String runtimeName;
        final int javaVersion;
        final String archiveName;
        final String sha256;

        RuntimeBundle(String runtimeName, int javaVersion, String archiveName, String sha256) {
            this.runtimeName = runtimeName;
            this.javaVersion = javaVersion;
            this.archiveName = archiveName;
            this.sha256 = sha256;
        }
    }

    /**
     * Download Android-compatible Java runtimes on first launch.
     * The release archives are pinned by SHA-256 so a changed or corrupted download
     * is never installed silently.
     */
    public static void unpackRuntime(AssetManager am) {
        sExecutorService.execute(() -> {
            File downloadDirectory = new File(Tools.DIR_CACHE, "runtime-downloads");
            if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
                Log.e("JREAuto", "Could not create runtime download directory");
                return;
            }

            try {
                for (RuntimeBundle bundle : RUNTIME_BUNDLES) {
                    try {
                        net.kdt.pojavlaunch.multirt.Runtime installed = MultiRTUtils.read(bundle.runtimeName);
                        if (MultiRTUtils.readInternalRuntimeVersion(bundle.runtimeName) != null
                                && installed.javaVersion == bundle.javaVersion) {
                            Log.i("JREAuto", "Java " + bundle.javaVersion + " is already installed");
                            continue;
                        }

                        ProgressLayout.setProgress(ProgressLayout.UNPACK_RUNTIME, 0);
                        installDownloadedRuntime(bundle, downloadDirectory);
                        Log.i("JREAuto", "Java " + bundle.javaVersion + " installed successfully");
                    } catch (Exception e) {
                        Log.e("JREAuto", "Failed to install Java " + bundle.javaVersion, e);
                    }
                }
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME);
            }
        });
    }

    private static void installDownloadedRuntime(RuntimeBundle bundle, File downloadDirectory)
            throws Exception {
        File zipFile = new File(downloadDirectory, bundle.archiveName);
        File universalFile = new File(downloadDirectory, bundle.runtimeName + "-universal.tar.xz");
        File platformFile = new File(downloadDirectory, bundle.runtimeName + "-bin-"
                + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz");

        if (!zipFile.isFile() || !bundle.sha256.equalsIgnoreCase(sha256(zipFile))) {
            if (zipFile.exists() && !zipFile.delete()) {
                throw new IOException("Could not remove invalid runtime archive");
            }
            DownloadUtils.downloadFile(RUNTIME_RELEASE_BASE + bundle.archiveName, zipFile);
        }

        String actualDigest = sha256(zipFile);
        if (!bundle.sha256.equalsIgnoreCase(actualDigest)) {
            zipFile.delete();
            throw new IOException("Runtime archive SHA-256 mismatch for " + bundle.archiveName);
        }

        String version;
        try (ZipFile archive = new ZipFile(zipFile)) {
            ZipEntry versionEntry = archive.getEntry("version");
            ZipEntry universalEntry = archive.getEntry("universal.tar.xz");
            ZipEntry platformEntry = archive.getEntry("bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz");
            if (versionEntry == null || universalEntry == null || platformEntry == null) {
                throw new IOException("Runtime archive is missing required files for "
                        + archAsString(Tools.DEVICE_ARCHITECTURE));
            }

            try (InputStream versionStream = archive.getInputStream(versionEntry)) {
                version = Tools.read(versionStream).trim();
            }
            if (version.isEmpty()) throw new IOException("Runtime version is empty");

            copyZipEntry(archive, universalEntry, universalFile);
            copyZipEntry(archive, platformEntry, platformFile);
        }

        try (FileInputStream universalInput = new FileInputStream(universalFile);
             FileInputStream platformInput = new FileInputStream(platformFile)) {
            MultiRTUtils.installRuntimeNamedBinpack(
                    universalInput, platformInput, bundle.runtimeName, version);
            MultiRTUtils.postPrepare(bundle.runtimeName);
        } finally {
            universalFile.delete();
            platformFile.delete();
        }
    }

    private static void copyZipEntry(ZipFile archive, ZipEntry entry, File destination)
            throws IOException {
        try (InputStream input = archive.getInputStream(entry);
             java.io.FileOutputStream output = new java.io.FileOutputStream(destination)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    /** Unpack single files, with no regard to version tracking */
    public static void unpackSingleFiles(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0);
        sExecutorService.execute(() -> {
            try {
                Tools.copyAssetFile(ctx, "options.txt", Tools.DIR_GAME_NEW, false);
                Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false);

                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
                Tools.copyAssetFile(ctx,"resolv.conf",Tools.DIR_DATA, false);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !");
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES);
        });
    }

    public static void unpackComponents(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0);
        sExecutorService.execute(() -> {
            try {
                unpackComponent(ctx, "caciocavallo", false);
                unpackComponent(ctx, "caciocavallo17", false);
                // Since the Java module system doesn't allow multiple JARs to declare the same module,
                // we repack them to a single file here
                unpackComponent(ctx, "lwjgl3", false);
                unpackComponent(ctx, "security", true);
                unpackComponent(ctx, "arc_dns_injector", true);
                unpackComponent(ctx, "forge_installer", true);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed o unpack components !",e );
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS);
        });
    }

    private static void unpackComponent(Context ctx, String component, boolean privateDirectory) throws IOException {
        AssetManager am = ctx.getAssets();
        String rootDir = privateDirectory ? Tools.DIR_DATA : Tools.DIR_GAME_HOME;

        File versionFile = new File(rootDir + "/" + component + "/version");
        InputStream is = am.open("components/" + component + "/version");
        if(!versionFile.exists()) {
            if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                FileUtils.deleteDirectory(versionFile.getParentFile());
            }
            versionFile.getParentFile().mkdir();

            Log.i("UnpackPrep", component + ": Pack was installed manually, or does not exist, unpacking new...");
            String[] fileList = am.list("components/" + component);
            for(String s : fileList) {
                Tools.copyAssetFile(ctx, "components/" + component + "/" + s, rootDir + "/" + component, true);
            }
        } else {
            FileInputStream fis = new FileInputStream(versionFile);
            String release1 = Tools.read(is);
            String release2 = Tools.read(fis);
            if (!release1.equals(release2)) {
                if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                    FileUtils.deleteDirectory(versionFile.getParentFile());
                }
                versionFile.getParentFile().mkdir();

                String[] fileList = am.list("components/" + component);
                for (String fileName : fileList) {
                    Tools.copyAssetFile(ctx, "components/" + component + "/" + fileName, rootDir + "/" + component, true);
                }
            } else {
                Log.i("UnpackPrep", component + ": Pack is up-to-date with the launcher, continuing...");
            }
        }
    }
}
