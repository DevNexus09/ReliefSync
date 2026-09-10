package com.reliefsync.ui;

import javafx.scene.layout.BorderPane;

/** A main-area screen that reloads its data every time it is shown. */
abstract class ContentPane extends BorderPane {

    ContentPane() {
        getStyleClass().add("content-pane");
    }

    abstract void refresh();
}
