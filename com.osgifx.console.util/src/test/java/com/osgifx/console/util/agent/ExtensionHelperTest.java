package com.osgifx.console.util.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.dto.DTO;

import com.osgifx.console.agent.Agent;

@ExtendWith(MockitoExtension.class)
public class ExtensionHelperTest {

    public static class TestContextDTO extends DTO {
        public String value;
    }

    public static class TestResultDTO extends DTO {
        public String result;
    }

    @Mock
    private Agent agent;

    @Test
    @SuppressWarnings("unchecked")
    public void executeExtensionConvertsContextToMap() throws Exception {
        TestContextDTO context = new TestContextDTO();
        context.value = "test-value";
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("result", "test-result");
        
        when(agent.executeExtension(eq("my.extension"), anyMap())).thenReturn(mockResult);

        ExtensionHelper.executeExtension(agent, "my.extension", context, TestResultDTO.class);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(agent).executeExtension(eq("my.extension"), mapCaptor.capture());
        
        Map<String, Object> capturedMap = mapCaptor.getValue();
        assertEquals("test-value", capturedMap.get("value"));
    }

    @Test
    public void executeExtensionConvertsResultToDto() throws Exception {
        TestContextDTO context = new TestContextDTO();
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("result", "test-result");
        
        when(agent.executeExtension(eq("my.extension"), anyMap())).thenReturn(mockResult);

        TestResultDTO result = ExtensionHelper.executeExtension(agent, "my.extension", context, TestResultDTO.class);

        assertEquals("test-result", result.result);
    }

    @Test
    public void executeExtensionPropagatesAgentException() throws Exception {
        TestContextDTO context = new TestContextDTO();
        
        when(agent.executeExtension(eq("error.extension"), anyMap())).thenThrow(new RuntimeException("Agent failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            ExtensionHelper.executeExtension(agent, "error.extension", context, TestResultDTO.class);
        });

        assertTrue(ex.getMessage().contains("Agent failed"));
    }

    @Test
    public void cannotInstantiateExtensionHelper() throws Exception {
        Constructor<ExtensionHelper> constructor = ExtensionHelper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(ex.getCause() instanceof IllegalAccessError);
        assertEquals("Cannot be instantiated", ex.getCause().getMessage());
    }
}
