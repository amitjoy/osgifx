package in.bytehue.osgifx.console.application.dialog;

import java.io.File;

public final class InstallBundleDTO {

    public File    file;
    public boolean startBundle;

    public InstallBundleDTO(final File file, final boolean startBundle) {
        this.file        = file;
        this.startBundle = startBundle;
    }

}
