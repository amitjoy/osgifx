package in.bytehue.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

import javafx.util.Pair;

public class XComponentDTO extends DTO {

    public Pair<String, Long>        bundle;
    public String                    implementationClass;
    public String                    defaultState;
    public String                    activation;
    public String                    configurationPolicy;
    public long                      serviceId;
    public String                    serviceType;
    public Map<Long, String>         services;
    public List<String>              pid;
    public Map<String, String>       properties;
    public Map<String, ReferenceDTO> references;
    public String                    activate;
    public String                    deactivate;
    public String                    modified;

}
