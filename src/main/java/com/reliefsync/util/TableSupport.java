package com.reliefsync.util;

import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class TableSupport {
  private TableSupport() {}

  public static <T> void column(
      TableView<T> table, String title, double width, Function<T, ?> value) {
    TableColumn<T, Object> column = new TableColumn<>(title);
    column.setPrefWidth(width);
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
    table.getColumns().add(column);
  }
}
