package com.reliefsync.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class FormDialogHelper {
  private FormDialogHelper() {}

  public record Field(String key, String label, String value) {}

  public static Optional<Map<String, String>> show(String title, List<Field> fields) {
    Dialog<Map<String, String>> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    GridPane grid = new GridPane();
    grid.setHgap(12);
    grid.setVgap(10);
    grid.setPadding(new Insets(18));
    Map<String, TextField> controls = new LinkedHashMap<>();
    int row = 0;
    for (Field f : fields) {
      TextField input = new TextField(f.value() == null ? "" : f.value());
      controls.put(f.key(), input);
      grid.add(new Label(f.label()), 0, row);
      grid.add(input, 1, row++);
    }
    dialog.getDialogPane().setContent(grid);
    dialog.setResultConverter(
        button -> {
          if (button != ButtonType.OK) return null;
          Map<String, String> result = new LinkedHashMap<>();
          controls.forEach((k, v) -> result.put(k, v.getText()));
          return result;
        });
    return dialog.showAndWait();
  }
}
