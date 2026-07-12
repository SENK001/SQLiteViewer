package com.senk.sqliteviewer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.stream.Collectors;

public class MultiSelectComboBox<T> extends HBox {

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableList<T> checkedItems = FXCollections.observableArrayList();
    private final Label displayLabel;
    private final Popup popup;
    private final ListView<T> listView;
    private final Label arrowLabel;
    private StringConverter<T> converter;
    private String promptText = "";

    public MultiSelectComboBox() {
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-border-color: #bcbcbc; -fx-border-width: 1; "
                + "-fx-border-radius: 2; -fx-background-radius: 2; "
                + "-fx-background-color: white; -fx-cursor: hand;");
        setMinHeight(26);
        setMaxHeight(26);

        displayLabel = new Label();
        displayLabel.setMaxWidth(Double.MAX_VALUE);
        displayLabel.setPadding(new Insets(2, 6, 2, 6));
        HBox.setHgrow(displayLabel, Priority.ALWAYS);

        arrowLabel = new Label("\u25BC");
        arrowLabel.setPadding(new Insets(2, 5, 2, 2));
        arrowLabel.setStyle("-fx-font-size: 8; -fx-text-fill: #666;");

        getChildren().addAll(displayLabel, arrowLabel);

        popup = new Popup();
        popup.setAutoHide(true);

        listView = new ListView<>(items);
        listView.setPrefHeight(200);
        listView.setPrefWidth(250);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setOnAction(null);
                    checkBox.setText(converter != null ? converter.toString(item) : String.valueOf(item));
                    checkBox.setSelected(checkedItems.contains(item));
                    checkBox.setOnAction(e -> {
                        if (checkBox.isSelected()) {
                            if (!checkedItems.contains(item)) {
                                checkedItems.add(item);
                            }
                        } else {
                            checkedItems.remove(item);
                        }
                    });
                    setGraphic(checkBox);
                }
            }
        });
        popup.getContent().add(listView);

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                Window window = getScene() != null ? getScene().getWindow() : null;
                if (window != null) {
                    popup.show(window,
                            localToScreen(0, getHeight()).getX(),
                            localToScreen(0, getHeight()).getY());
                    listView.requestFocus();
                }
            }
            e.consume();
        });

        checkedItems.addListener((javafx.collections.ListChangeListener<T>) change -> updateDisplay());
    }

    private void updateDisplay() {
        if (checkedItems.isEmpty()) {
            displayLabel.setText(promptText);
            displayLabel.setStyle("-fx-text-fill: #999;");
        } else {
            String text = checkedItems.stream()
                    .map(item -> converter != null ? converter.toString(item) : String.valueOf(item))
                    .collect(Collectors.joining(", "));
            displayLabel.setText(text);
            displayLabel.setStyle("-fx-text-fill: black;");
        }
    }

    public void setConverter(StringConverter<T> converter) {
        this.converter = converter;
        listView.refresh();
    }

    public void setItems(ObservableList<T> items) {
        this.items.setAll(items);
    }

    public ObservableList<T> getCheckedItems() {
        return checkedItems;
    }

    public void clearChecks() {
        checkedItems.clear();
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
        updateDisplay();
    }

    public void setPopupWidth(double width) {
        listView.setPrefWidth(width);
    }

    public void setPopupHeight(double height) {
        listView.setPrefHeight(height);
    }
}
