package in.bytehue.osgifx.console.ui.configurations;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;

import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public final class ConfigurationEditorFxController implements Initializable {

    @FXML
    private Label pidLabel;

    @FXML
    private Label locationLabel;

    @FXML
    private Label factoryPidLabel;

    @FXML
    private Button saveConfigButton;

    @FXML
    private Button deleteConfigButton;

    @FXML
    private PropertySheet propertySheet;

    private final Map<String, Object> customDataMap = new LinkedHashMap<>();
    {
        customDataMap.put("1. Name#First Name", "Jonathan");
        customDataMap.put("1. Name#Last Name", "Giles");
        customDataMap.put("1. Name#Birthday", LocalDate.of(1985, Month.JANUARY, 12));
        customDataMap.put("2. Billing Address#Address 1", "");
        customDataMap.put("2. Billing Address#Address 2", "");
        customDataMap.put("2. Billing Address#City", "");
        customDataMap.put("2. Billing Address#State", "");
        customDataMap.put("2. Billing Address#Zip", "");
        customDataMap.put("3. Phone#Home", "123-123-1234");
        customDataMap.put("3. Phone#Mobile", "234-234-2345");
        customDataMap.put("3. Phone#Work", "");
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    void initControls(final XConfigurationDTO config) {
        propertySheet.setModeSwitcherVisible(false);
        setPrivateFinalField(PropertySheet.class, "items", propertySheet, getCustomModelProperties());
    }

    private ObservableList<Item> getCustomModelProperties() {
        final ObservableList<Item> list = FXCollections.observableArrayList();
        for (final String key : customDataMap.keySet()) {
            list.add(new CustomPropertyItem(key));
        }
        return list;
    }

    private class CustomPropertyItem implements Item {

        private final String key;
        private final String category, name;

        public CustomPropertyItem(final String key) {
            this.key = key;
            final String[] skey = key.split("#");
            category = skey[0];
            name     = skey[1];
        }

        @Override
        public Class<?> getType() {
            return customDataMap.get(key).getClass();
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public Object getValue() {
            return customDataMap.get(key);
        }

        @Override
        public void setValue(final Object value) {
            customDataMap.put(key, value);
        }

        @Override
        public Optional<ObservableValue<? extends Object>> getObservableValue() {
            return Optional.empty();
        }

    }

    public static boolean setPrivateFinalField(final Class<?> clazz, final String fieldName, final Object instance, final Object value) {
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

}
