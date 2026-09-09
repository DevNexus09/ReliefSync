package com.reliefsync.ui;

import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

/** Small UI helpers shared by all panes. */
final class Ui {

    private Ui() {
    }

    static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("heading");
        return label;
    }

    static Button primary(Button button) {
        button.getStyleClass().add("primary-button");
        return button;
    }

    static Button danger(Button button) {
        button.getStyleClass().add("danger-button");
        return button;
    }

    /** Makes columns share the available width instead of overflowing into a scrollbar. */
    static <S> TableView<S> fitColumns(TableView<S> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    static <S> TableColumn<S, String> col(String title, Function<S, Object> extractor, int width) {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(String.valueOf(extractor.apply(data.getValue()))));
        column.setPrefWidth(width);
        return column;
    }

    /** A column rendering its value as a colored status badge. */
    static <S> TableColumn<S, String> badgeCol(String title, Function<S, Object> extractor, int width) {
        TableColumn<S, String> column = col(title, extractor, width);
        column.setCellFactory(tc -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(value);
                badge.getStyleClass().add("badge");
                badge.setStyle("-fx-background-color: " + badgeColor(value) + ";");
                setGraphic(badge);
            }
        });
        return column;
    }

    private static String badgeColor(String value) {
        return switch (value) {
            case "DRAFT" -> "#8a93a3";
            case "SUBMITTED", "LOW", "HIGH", "MAINTENANCE" -> "#d98f0b";
            case "VERIFIED" -> "#3b82f6";
            case "REJECTED", "OUT OF STOCK", "CRITICAL" -> "#d64545";
            case "ALLOCATED" -> "#8b5cf6";
            case "DISPATCHED", "IN_TRANSIT" -> "#0e9aa7";
            case "DELIVERED", "OK", "AVAILABLE" -> "#1b9e4b";
            case "CANCELLED", "INACTIVE" -> "#5b6270";
            default -> "#64748b";
        };
    }

    static void info(String message) {
        show(Alert.AlertType.INFORMATION, message);
    }

    static void error(String message) {
        show(Alert.AlertType.ERROR, message);
    }

    static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(bt -> bt == ButtonType.YES).isPresent();
    }

    static Optional<String> promptText(String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("ReliefSync");
        dialog.setHeaderText(header);
        return dialog.showAndWait();
    }

    private static void show(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("ReliefSync");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Parses a required integer field, reporting the field name on failure. */
    static int intOf(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    /** Runs an action and turns any domain exception into an error dialog. */
    static void guarded(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
