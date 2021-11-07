package in.bytehue.osgifx.console.ui.packages;

import java.util.List;

import com.google.common.collect.Lists;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;

public final class PackageDTO {

    public String           name;
    public String           version;
    public boolean          isDuplicateExport;
    public List<XBundleDTO> exporters = Lists.newArrayList();
    public List<XBundleDTO> importers = Lists.newArrayList();

}
