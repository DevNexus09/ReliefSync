package com.reliefsync.ui;

import com.reliefsync.model.User;
import com.reliefsync.service.AuthService;
import com.reliefsync.service.Session;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

        TabPane tabs = new TabPane(loginTab(), signupTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMaxWidth(310);

        VBox card = new VBox(12, title, subtitle, tabs);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28, 40, 28, 40));
        card.setMaxWidth(410);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        getChildren().add(card);
    }

    private Tab loginTab() {
        TextField username = field("Username");
        PasswordField password = passwordField("Password");
        Label message = messageLabel();
        Button loginButton = Ui.primary(new Button("Log in"));
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> {
            Optional<User> user = auth.login(username.getText(), password.getText());
            if (user.isEmpty()) {
                message.setText("Invalid username or password");
                password.clear();
                return;
            }
            startSession(user.get());
        });
        password.setOnAction(e -> loginButton.fire());

        return new Tab("Log in", form(new Label("Use your existing account"),
                username, password, loginButton, message));
    }

    private Tab signupTab() {
        TextField fullName = field("Full name");
        TextField username = field("Username (letters, numbers, underscore)");
        PasswordField password = passwordField("Password");
        PasswordField confirmation = passwordField("Confirm password");
        Label policy = new Label("8+ characters with uppercase, lowercase, and a number.\n"
                + "New accounts are registered as Volunteers.");
        policy.getStyleClass().add("muted");
        policy.setWrapText(true);
        Label message = messageLabel();
        Button signupButton = Ui.primary(new Button("Create account"));
        signupButton.setMaxWidth(Double.MAX_VALUE);
        signupButton.setOnAction(e -> {
            try {
                User user = auth.signup(fullName.getText(), username.getText(),
                        password.getText(), confirmation.getText());
                startSession(user);
            } catch (RuntimeException ex) {
                message.setText(ex.getMessage() == null ? "Could not create the account" : ex.getMessage());
                password.clear();
                confirmation.clear();
            }
        });
        confirmation.setOnAction(e -> signupButton.fire());

        return new Tab("Sign up", form(fullName, username, password, confirmation,
                policy, signupButton, message));
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(280);
        return field;
    }

    private static PasswordField passwordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setMaxWidth(280);
        return field;
    }

    private static Label messageLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-text");
        label.setWrapText(true);
        return label;
    }

    private static VBox form(Node... children) {
        VBox box = new VBox(10, children);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(14, 8, 14, 8));
        return box;
    }

    private static void startSession(User user) {
        Session.login(user);
        SceneManager.showMain();
    }
}
