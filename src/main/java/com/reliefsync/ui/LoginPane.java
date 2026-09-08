package com.reliefsync.ui;

import com.reliefsync.model.User;
import com.reliefsync.service.AuthService;
import com.reliefsync.service.Session;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

class LoginPane extends StackPane {

    private final AuthService auth = new AuthService();

    LoginPane() {
        getStyleClass().add("login-background");

        Label title = new Label("ReliefSync");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Disaster Relief Resource Coordination");
        subtitle.getStyleClass().add("muted");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setMaxWidth(260);
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setMaxWidth(260);

        Label message = new Label();
        message.getStyleClass().add("error-text");

        Button loginButton = Ui.primary(new Button("Log in"));
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(260);
        loginButton.setOnAction(e -> {
            Optional<User> user = auth.login(username.getText().trim(), password.getText());
            if (user.isEmpty()) {
                message.setText("Invalid username or password");
                return;
            }
            Session.login(user.get());
            SceneManager.showMain();
        });

        VBox card = new VBox(12, title, subtitle, new Label(" "), username, password, loginButton, message);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.setMaxWidth(380);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        if (!auth.hasAnyUser()) {
            Label hint = new Label(
                    "No accounts exist yet.\nRestart with RELIEFSYNC_SEED_DEMO=true to create demo accounts.");
            hint.getStyleClass().add("muted");
            hint.setWrapText(true);
            card.getChildren().add(hint);
        }

        getChildren().add(card);
    }
}
