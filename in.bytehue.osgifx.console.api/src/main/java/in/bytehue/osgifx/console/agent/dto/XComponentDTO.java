package in.bytehue.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

import aQute.libg.tuple.Pair;

public class XComponentDTO extends DTO {

    public long                      id;
    public String                    name;
    public String                    state;
    public Pair<String, Long>        bundle;
    public String                    factory;
    public String                    scope;
    public String                    implementationClass;
    public String                    configurationPolicy;
    public List<String>              serviceInterfaces;
    public List<String>              configurationPid;
    public Map<String, String>       properties;
    public List<Map<String, String>> references;
    public String                    failure;
    public String                    activate;
    public String                    deactivate;
    public String                    modified;
    public Map<String, String>       satisfiedReferences;
    public Map<String, String>       unsatisfiedReferences;

}
