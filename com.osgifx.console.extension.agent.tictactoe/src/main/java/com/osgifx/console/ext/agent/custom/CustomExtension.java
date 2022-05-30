package com.osgifx.console.ext.agent.custom;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.extension.AgentExtensionName;

@Component
@AgentExtensionName("my-agent-extension")
public final class CustomExtension implements AgentExtension<MyContextDTO, MyResultDTO> {

	@Override
	public MyResultDTO execute(final MyContextDTO context) {
		final String propName  = context.propName;
		final int    propValue = context.propValue;

		System.out.println(propName);

		final MyResultDTO result = new MyResultDTO();
		result.name = "custom extension result";
		if (propValue > 10) {
			result.intValue    = 20;
			result.doubleValue = 100.25d;
		} else {
			result.intValue    = 10;
			result.doubleValue = 0.00d;
		}
		return result;
	}

	@Override
	public Class<MyContextDTO> getContextType() {
		return MyContextDTO.class;
	}

	@Override
	public Class<MyResultDTO> getResultType() {
		return MyResultDTO.class;
	}

}
