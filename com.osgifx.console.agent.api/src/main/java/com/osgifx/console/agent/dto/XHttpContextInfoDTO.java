package com.osgifx.console.agent.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class XHttpContextInfoDTO extends DTO {

    public List<XServletDTO>   servlets;
    public List<XFilterDTO>    filters;
    public List<XResourceDTO>  resources;
    public List<XListenerDTO>  listeners;
    public List<XErrorPageDTO> errorPages;

}
