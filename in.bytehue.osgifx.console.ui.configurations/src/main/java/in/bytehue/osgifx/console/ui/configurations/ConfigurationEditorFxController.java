package in.bytehue.osgifx.console.ui.configurations;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.osgi.service.metatype.AttributeDefinition;

import in.bytehue.osgifx.console.agent.dto.XAttributeDefDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XObjectClassDefDTO;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public final class ConfigurationEditorFxController implements Initializable {

    @FXML
    private GridPane rootPanel;

    @FXML
    private Button saveConfigButton;

    @FXML
    private Button deleteConfigButton;

    private PropertySheet propertySheet;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    void initControls(final XConfigurationDTO config) {
        propertySheet = new PropertySheet(getConfigurationProperties(config));
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);

        GridPane.setColumnSpan(propertySheet, 2);
        GridPane.setHgrow(propertySheet, Priority.ALWAYS);
        GridPane.setRowIndex(propertySheet, 3);
        GridPane.setRowSpan(propertySheet, Integer.MAX_VALUE);
        GridPane.setVgrow(propertySheet, Priority.ALWAYS);
        GridPane.setMargin(propertySheet, new Insets(0, 0, 0, 50));

        rootPanel.getChildren().add(propertySheet);

        // setPrivateFinalField(PropertySheet.class, "items", propertySheet, getConfigurationProperties(config));
    }

    private ObservableList<Item> getConfigurationProperties(final XConfigurationDTO config) {
        final ObservableList<Item> list  = FXCollections.observableArrayList();
        final XObjectClassDefDTO   ocd   = config.ocd;
        final List<XPropertyItem>  items = new ArrayList<>();
        if (ocd != null) {
            for (final XAttributeDefDTO ad : config.ocd.attributeDefs) {
                final XPropertyItem item = new XPropertyItem(toMetatypeClazz(ad.type), ad.name, ad.description, getValue(config, ad.id));
                items.add(item);
            }
        } else {
            for (final Entry<String, Object> prop : config.properties.entrySet()) {
                final XPropertyItem item = new XPropertyItem(Object.class, prop.getKey(), null, prop.getValue());
                items.add(item);
            }
        }
        final List<XPropertyItem> genericItems         = getGenericItems(config);
        final List<Item>          genericPropertyItems = genericItems.stream().map(ConfigurationItem::new).collect(toList());
        final List<Item>          propertyItems        = items.stream().map(ConfigurationItem::new).collect(toList());

        list.addAll(genericPropertyItems);
        list.addAll(propertyItems);

        return list;
    }

    private List<XPropertyItem> getGenericItems(final XConfigurationDTO config) {
        final XPropertyItem pidItem        = new XPropertyItem(String.class, "PID", "Configuration PID", config.pid, false);
        final XPropertyItem factoryPidItem = new XPropertyItem(String.class, "Factory PID",
                "Configuration Factory PID used to create multiple PIDs", config.factoryPid, false);
        final XPropertyItem locationItem   = new XPropertyItem(String.class, "Location", "Location to which the configuration is bound",
                config.factoryPid, false);

        return Arrays.asList(pidItem, factoryPidItem, locationItem);
    }

    private Object getValue(final XConfigurationDTO config, final String id) {
        if (config.properties == null) {
            return null;
        }
        return config.properties.get(id);
    }

    private static class ConfigurationItem implements Item {

        private final XPropertyItem item;

        public ConfigurationItem(final XPropertyItem item) {
            this.item = item;
        }

        @Override
        public Class<?> getType() {
            return item.type;
        }

        @Override
        public String getCategory() {
            return "";
        }

        @Override
        public String getName() {
            return item.name;
        }

        @Override
        public String getDescription() {
            return item.description == null ? "" : item.description;
        }

        @Override
        public Object getValue() {
            return item.value;
        }

        @Override
        public void setValue(final Object value) {
            // TODO;
        }

        @Override
        public boolean isEditable() {
            return item.isEditable;
        }

        @Override
        public Optional<ObservableValue<? extends Object>> getObservableValue() {
            return Optional.empty();
        }

    }

    private static boolean setPrivateFinalField(final Class<?> clazz, final String fieldName, final Object instance, final Object value) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(instance, value);
            return true;
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("unused")
    private static class XPropertyItem {
        Class<?> type;
        String   name;
        String   description;
        Object   value;
        boolean  isEditable;

        public XPropertyItem(final Class<?> type, final String name, final String description, final Object value) {
            this(type, name, description, value, true);
        }

        public XPropertyItem(final Class<?> type, final String name, final String description, final Object value,
                final boolean isEditable) {
            this.type        = type;
            this.name        = name;
            this.description = description;
            this.value       = value;
            this.isEditable  = isEditable;
        }

    }

    private Class<?> toMetatypeClazz(final int type) {
        switch (type) {
            case AttributeDefinition.INTEGER:
                return Integer.class;
            case AttributeDefinition.FLOAT:
            case AttributeDefinition.DOUBLE:
                return Double.class;
            case AttributeDefinition.BOOLEAN:
                return Boolean.class;
            case AttributeDefinition.LONG:
                return Long.class;
            case AttributeDefinition.PASSWORD:
            case AttributeDefinition.STRING:
                return String.class;
            default: // TODO other types
                return String.class;
        }
    }

}
