package in.bytehue.osgifx.console.application.fxml.controller;

import static javafx.scene.control.SelectionMode.MULTIPLE;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.controlsfx.control.CheckTreeView;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import in.bytehue.osgifx.console.application.dialog.CheckForUpdatesDialog.SelectedFeaturesForUpdateDTO;
import in.bytehue.osgifx.console.feature.FeatureDTO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;

public final class CheckForUpdatesDialogController {

    private static final String               PLACEHOLDER_ROOT = "<Updated Features>";
    @Log
    @Inject
    private FluentLogger                      logger;
    @FXML
    private BorderPane                        featuresPane;
    private CheckBoxTreeItem<String>          root;
    private CheckTreeView<String>             featuresView;
    private Map<TreeItem<String>, FeatureDTO> features;

    @FXML
    public void initialize() {
        root = new CheckBoxTreeItem<>(PLACEHOLDER_ROOT);
        root.setExpanded(true);

        featuresView = new CheckTreeView<>(root);
        featuresView.getSelectionModel().setSelectionMode(MULTIPLE);

        features = Maps.newHashMap();

        logger.atInfo().log("FXML controller has been initialized");
    }

    public SelectedFeaturesForUpdateDTO getSelectedFeatures() {
        final SelectedFeaturesForUpdateDTO dto = new SelectedFeaturesForUpdateDTO();
        dto.features = Lists.newArrayList();
        final ObservableList<TreeItem<String>> selectedItems = featuresView.getCheckModel().getCheckedItems();
        for (final TreeItem<String> treeItem : selectedItems) {
            final TreeItem<String> parent = treeItem.getParent();
            if (parent == null || PLACEHOLDER_ROOT.equals(parent.getValue())) {
                continue;
            }
            final FeatureDTO feature = features.get(treeItem);
            dto.features.add(feature);
        }
        return dto;
    }

    public void setFeaturesToBeUpdated(final Collection<FeatureDTO> tobeUpdatedFeatures) {
        final Map<String, CheckBoxTreeItem<String>> featureRoots = Maps.newHashMap();
        for (final FeatureDTO feature : tobeUpdatedFeatures) {
            final String archiveURL = feature.archiveURL;
            if (featureRoots.containsKey(archiveURL)) {
                continue;
            }
            final CheckBoxTreeItem<String> featureRoot = new CheckBoxTreeItem<>(archiveURL);
            featureRoot.setExpanded(true);
            root.getChildren().add(featureRoot);
            featureRoots.put(archiveURL, featureRoot);
        }
        for (final FeatureDTO feature : tobeUpdatedFeatures) {
            final CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(featureAsString(feature));
            features.put(item, feature);
            final CheckBoxTreeItem<String> featureRoot = featureRoots.get(feature.archiveURL);
            featureRoot.getChildren().add(item);
        }
        featuresPane.setCenter(featuresView);
    }

    private String featureAsString(final FeatureDTO feature) {
        return feature.id.groupId + ":" + feature.id.artifactId + ":" + feature.id.version;
    }
}
