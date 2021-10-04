package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;

import org.osgi.dto.DTO;

import aQute.libg.tuple.Pair;

public class XServiceDTO extends DTO {

    public long                id;
    public Pair<String, Long>  bundle;
    public Map<String, String> properties;
    public Map<Long, String>   usingBundles;

}
