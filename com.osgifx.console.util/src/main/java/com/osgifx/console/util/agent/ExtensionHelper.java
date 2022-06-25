package com.osgifx.console.util.agent;

import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

import com.osgifx.console.agent.Agent;

public final class ExtensionHelper {

    private ExtensionHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static <R extends DTO> R executeExtension(final Agent agent, final String name, final DTO context, final Class<R> resultType) {
        try {
            final var                 converter        = Converters.standardConverter();
            final Map<String, Object> properties       = converter.convert(context).to(new TypeReference<Map<String, Object>>() {
                                                       });
            final var                 executeExtension = agent.executeExtension(name, properties);
            return converter.convert(executeExtension).to(resultType);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
