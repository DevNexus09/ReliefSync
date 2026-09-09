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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Authentication screen shared by returning users and new volunteer registrations. */
class LoginPane extends StackPane {

    private final AuthService auth = new AuthService();
    private final StackPane formHost = new StackPane();
    private final Button loginSelector = selector("Log in");
    private final Button signupSelector = selector("Sign up");
    private Button loginAction;
    private Button signupAction;
    private Node loginForm;
    private Node signupForm;

    LoginPane() {
        getStyleClass().add("login-background");
        setPadding(new Insets(30));

        loginForm = loginForm();
        signupForm = signupForm();
        formHost.getChildren().addAll(loginForm, signupForm);

        HBox selector = new HBox(4, loginSelector, signupSelector);
        selector.getStyleClass().add("auth-switch");
        HBox.setHgrow(loginSelector, Priority.ALWAYS);
        HBox.setHgrow(signupSelector, Priority.ALWAYS);
        loginSelector.setMaxWidth(Double.MAX_VALUE);
        signupSelector.setMaxWidth(Double.MAX_VALUE);
        loginSelector.setOnAction(e -> showLogin());
        signupSelector.setOnAction(e -> showSignup());

        Label eyebrow = new Label("SECURE ACCESS");
        eyebrow.getStyleClass().add("auth-eyebrow");
        Label heading = new Label("Welcome to ReliefSync");
        heading.getStyleClass().add("auth-heading");
        Label introduction = new Label("Sign in to continue, or create a volunteer account in a few steps.");
        introduction.getStyleClass().add("auth-introduction");
        introduction.setWrapText(true);

        VBox authentication = new VBox(10, eyebrow, heading, introduction, selector, formHost);
        authentication.getStyleClass().add("auth-form-panel");
        authentication.setAlignment(Pos.TOP_LEFT);
        authentication.setPrefWidth(470);
        HBox.setHgrow(authentication, Priority.ALWAYS);

        HBox shell = new HBox(brandPanel(), authentication);
        shell.getStyleClass().add("auth-shell");
        shell.setMaxWidth(900);
        shell.setMaxHeight(660);
        getChildren().add(shell);

        showLogin();
    }

    private static VBox brandPanel() {
        Label mark = new Label("RS");
        mark.getStyleClass().add("auth-mark");
        Label title = new Label("ReliefSync");
        title.getStyleClass().add("auth-brand-title");
        Label tagline = new Label("Coordinate relief.\nDeliver hope.");
        tagline.getStyleClass().add("auth-tagline");
        Label description = new Label(
                "A secure workspace for coordinating requests, inventory, allocations, and dispatch.");
        description.getStyleClass().add("auth-brand-copy");
        description.setWrapText(true);

        VBox features = new VBox(12,
                feature("01", "Verified relief requests"),
                feature("02", "Traceable stock allocation"),
                feature("03", "Coordinated field dispatch"));
        features.getStyleClass().add("auth-features");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label footer = new Label("DISASTER RELIEF COORDINATION");
        footer.getStyleClass().add("auth-brand-footer");

        VBox panel = new VBox(12, mark, title, tagline, description, features, spacer, footer);
        panel.getStyleClass().add("auth-brand-panel");
        panel.setPrefWidth(360);
        panel.setMinWidth(360);
        return panel;
    }

    private static HBox feature(String number, String text) {
        Label badge = new Label(number);
        badge.getStyleClass().add("auth-feature-number");
        Label label = new Label(text);
        label.getStyleClass().add("auth-feature-text");
        HBox row = new HBox(12, badge, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node loginForm() {
        TextField username = textField("Enter your username");
        PasswordField password = passwordField("Enter your password");
        Label message = messageLabel();

        loginAction = actionButton("Log in to ReliefSync");
        loginAction.setOnAction(e -> {
            message.setText("");
            Optional<User> user = auth.login(username.getText(), password.getText());
            if (user.isEmpty()) {
                message.setText("The username or password is incorrect. Please try again.");
                password.clear();
                password.requestFocus();
                return;
            }
            startSession(user.get());
        });
        password.setOnAction(e -> loginAction.fire());

        VBox form = new VBox(14,
                fieldGroup("Username", username),
                fieldGroup("Password", password),
                loginAction,
                message);
        form.getStyleClass().add("auth-form");
        return form;
    }

    private Node signupForm() {
        TextField fullName = textField("Enter your full name");
        TextField username = textField("Choose a username");
        PasswordField password = passwordField("Create a strong password");
        PasswordField confirmation = passwordField("Enter the password again");
        Label policy = new Label("Use 8+ characters with uppercase, lowercase, and a number.\n"
                + "Public accounts are created with the Volunteer role.");
        policy.getStyleClass().add("auth-policy");
        policy.setWrapText(true);
        Label message = messageLabel();

        signupAction = actionButton("Create volunteer account");
        signupAction.setOnAction(e -> {
            message.setText("");
            try {
                User user = auth.signup(fullName.getText(), username.getText(),
                        password.getText(), confirmation.getText());
                startSession(user);
            } catch (RuntimeException ex) {
                message.setText(ex.getMessage() == null ? "Could not create the account" : ex.getMessage());
                password.clear();
                confirmation.clear();
                password.requestFocus();
            }
        });
        confirmation.setOnAction(e -> signupAction.fire());

        VBox form = new VBox(10,
                fieldGroup("Full name", fullName),
                fieldGroup("Username", username),
                fieldGroup("Password", password),
                fieldGroup("Confirm password", confirmation),
                policy,
                signupAction,
                message);
        form.getStyleClass().add("auth-form");
        return form;
    }

    private void showLogin() {
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        signupForm.setVisible(false);
        signupForm.setManaged(false);
        setSelected(loginSelector, true);
        setSelected(signupSelector, false);
        loginAction.setDefaultButton(true);
        signupAction.setDefaultButton(false);
    }

    private void showSignup() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        signupForm.setVisible(true);
        signupForm.setManaged(true);
        setSelected(loginSelector, false);
        setSelected(signupSelector, true);
        loginAction.setDefaultButton(false);
        signupAction.setDefaultButton(true);
    }

    private static void setSelected(Button button, boolean selected) {
        if (selected && !button.getStyleClass().contains("selected")) {
            button.getStyleClass().add("selected");
        } else if (!selected) {
            button.getStyleClass().remove("selected");
        }
    }

    private static Button selector(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("auth-switch-button");
        return button;
    }

    private static TextField textField(String prompt) {
        TextField field = new TextField();
        configureField(field, prompt);
        return field;
    }

    private static PasswordField passwordField(String prompt) {
        PasswordField field = new PasswordField();
        configureField(field, prompt);
        return field;
    }

    private static void configureField(TextField field, String prompt) {
        field.setPromptText(prompt);
        field.getStyleClass().add("auth-input");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setPrefHeight(42);
    }

    private static VBox fieldGroup(String title, TextField field) {
        Label label = new Label(title);
        label.getStyleClass().add("auth-field-label");
        label.setLabelFor(field);
        return new VBox(6, label, field);
    }

    private static Button actionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("auth-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(44);
        return button;
    }

    private static Label messageLabel() {
        Label label = new Label();
        label.getStyleClass().addAll("error-text", "auth-message");
        label.setWrapText(true);
        label.setMinHeight(34);
        return label;
    }

    private static void startSession(User user) {
        Session.login(user);
        SceneManager.showMain();
    }
}
