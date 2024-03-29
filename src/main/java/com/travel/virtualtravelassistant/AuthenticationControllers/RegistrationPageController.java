package com.travel.virtualtravelassistant.AuthenticationControllers;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.travel.virtualtravelassistant.MainApplication;
import com.travel.virtualtravelassistant.User.CurrentUser;
import com.travel.virtualtravelassistant.User.UserInfo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegistrationPageController {
    @FXML
    TextField email;

    @FXML
    PasswordField password;

    @FXML
    TextField firstName;

    @FXML
    TextField lastName;

    @FXML
    protected void onCreateAccountButtonClick(ActionEvent event) {
        String userEmail = email.getText();
        String userPassword = password.getText();
        String userFirstName = firstName.getText();
        String userLastName = lastName.getText();

        try {
            String UID = Objects.requireNonNull(createUser(userEmail, userPassword)).getUid();
            addUserToDB(UID, userPassword, userFirstName, userLastName);
            UserInfo user = new UserInfo(UID, userFirstName, userLastName, userEmail);
            CurrentUser.getInstance().setUserInfo(user);
            goToPage(event, "/com/travel/virtualtravelassistant/homeView.fxml");
        }catch (Exception e){
            System.out.println("Could not register user.");
        }

    }

    @FXML
    protected void onGoToLogInButtonClick(ActionEvent event){
        goToPage(event, "/com/travel/virtualtravelassistant/LogIn.fxml");
    }

    private UserRecord createUser(String email, String password){
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest();
        createRequest.setEmail(email);
        createRequest.setPassword(password);
        try {
            return MainApplication.fauth.createUser(createRequest);
        } catch (FirebaseAuthException e) {
            System.out.println("Error creating new user in firebase");
        }
        return null;
    }

    private void goToPage(ActionEvent event, String fxml){
        try {
            Parent parent = FXMLLoader.load((getClass().getResource(fxml)));
            Scene scene = new Scene(parent);

            // Get the Stage from the ActionEvent
            Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

            window.setScene(scene);
            window.show();
        } catch (IOException e) {
            System.out.println("Failed to load " + fxml +  " page.");
            e.printStackTrace();
        }
    }

    private void addUserToDB(String UID, String password, String first_name, String last_name){
        DocumentReference docRef = MainApplication.fstore.collection("Users").document(UID);

        Map<String, Object> data = new HashMap<>();
        data.put("first_name", first_name);
        data.put("last_name", last_name);
        data.put("password", password);

        try {
            ApiFuture<WriteResult> result = docRef.set(data);
        }catch (Exception e){
            System.out.println("Could not add new register to DB");
        }
    }

}
