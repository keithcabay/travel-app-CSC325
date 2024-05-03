package com.travel.virtualtravelassistant.PlanNextTrip;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.travel.virtualtravelassistant.Activity;
import com.travel.virtualtravelassistant.ActivityCardController;
import com.travel.virtualtravelassistant.Utility.FirebaseStorageAction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PlanNextTripController implements Initializable {

    @FXML
    private ImageView profilePicImage;
    @FXML
    private TextField locationTextField;
    @FXML
    private TextArea locationDetailsTextArea;
    @FXML
    private HBox photosContainer;
    @FXML
    private Slider foodSlider;
    @FXML
    private TextField foodTextField;
    @FXML
    private Slider roomAndBoardSlider;
    @FXML
    private TextField roomAndBoardTextField;
    @FXML
    private Slider spendingSlider;
    @FXML
    private TextField spendingTextField;
    @FXML
    private TextField totalBudgetTextField;
    @FXML
    private TextField vacationLengthTextField;
    @FXML
    private TextArea attractionsTextArea;
    @FXML
    private TextArea reviewsTextArea;
    @FXML
    private Button continueButton;
    @FXML
    private GridPane attractionsGrid;
    @FXML
    private GridPane hotelsGrid;
    @FXML
    private GridPane reviewsGrid;
    @FXML
    private GridPane addedGrid;

    private int currGridColumn = 0;
    private  int currGridRow = 0;



    private double totalBudget;
    private int vacationLength;
    private String API_KEY;

    private static final String SEARCH_API_URL = "https://api.content.tripadvisor.com/api/v1/location/search?key=";
    private static final String REVIEWS_API_URL = "https://api.content.tripadvisor.com/api/v1/location/";
    private static final String DETAILS_API_URL = "https://api.content.tripadvisor.com/api/v1/location/";
    private static final String PHOTOS_API_URL = "https://api.content.tripadvisor.com/api/v1/location/";
    private static final String REVIEWS_API_PHOTOS_URL = "https://api.content.tripadvisor.com/api/v1/location/";
    private static final String ATTRACTIONS_API_URL = "https://api.content.tripadvisor.com/api/v1/location/search?key=";


    private String readApiKeyFromJsonFile(String jsonFilePath) throws IOException {
        FileInputStream serviceAccountStream = new FileInputStream("src/main/resources/adminSDK.json");
        JsonObject json = JsonParser.parseReader(new InputStreamReader(serviceAccountStream)).getAsJsonObject();
        API_KEY = json.get("trip_advisor_key").getAsString();
        return API_KEY;
    }



    @FXML
    private void searchLocation() throws IOException {
        System.out.println("Searching location...");
        String searchQuery = locationTextField.getText();
        OkHttpClient client = new OkHttpClient();

        String apiUrl = SEARCH_API_URL + API_KEY + "&searchQuery=" + searchQuery +
                //"&category=geos" +  // Add category parameter for geos search
                "&language=en"; // Add language parameter for English results

        String responseBody;
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseBody = response.body().string(); // Store the response body string
                System.out.println("Request successful:");
                System.out.println(responseBody);
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray locations = jsonResponse.getAsJsonArray("data");
                if (locations != null && locations.size() > 0) {
                    JsonObject firstLocation = locations.get(0).getAsJsonObject();
                    String locationId = firstLocation.get("location_id").getAsString();
                    System.out.println("Location ID: " + locationId);
                    loadLocationDetails(locationId, API_KEY);
                    loadLocationReviews(locationId, API_KEY);
                    loadLocationAndReviewPhotos(locationId, API_KEY);
                    loadLocationAttractions(locationId, API_KEY);
                } else {
                    System.out.println("Error: " + response.code() + " " + response.message());
                    System.out.println(responseBody); // Print the response body string for debugging
                }
            } else {
                throw new IOException("Error: " + response.code());
            }
        }
    }

    private void loadLocationDetails(String locationId, String apiKey) {
        String detailsUrl = DETAILS_API_URL + locationId + "/details?key=" + apiKey + "&language=en&currency=USD";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(detailsUrl)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();

                JSONObject jsonResponse = new JSONObject(responseBody);

                String name = jsonResponse.getString("name");
                String description = jsonResponse.getString("description");
                locationDetailsTextArea.setText(description);

            } else {
                System.out.println("Unsuccessful response: " + response.code());
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    private void loadLocationAndReviewPhotos(String locationId, String apiKey) {
        String photosUrl = PHOTOS_API_URL + locationId + "/photos?key=" + apiKey + "&language=en";
        String reviewsUrl = REVIEWS_API_PHOTOS_URL + locationId + "/reviews?key=" + apiKey + "&language=en";

        OkHttpClient client = new OkHttpClient();
        photosContainer.getChildren().clear();  // Clear previous photos

        // Load location photos
        Request photosRequest = new Request.Builder()
                .url(photosUrl)
                .get()
                .addHeader("accept", "application/json")
                .build();

        client.newCall(photosRequest).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray photosArray = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        try {
                            for (int i = 0; i < photosArray.length(); i++) {
                                JSONObject photoObject = photosArray.getJSONObject(i);
                                JSONObject imagesObject = photoObject.getJSONObject("images");
                                JSONObject largeImageObject = imagesObject.getJSONObject("large");
                                String photoUrl = largeImageObject.getString("url");

                                ImageView imageView = new ImageView(new Image(photoUrl));
                                imageView.setFitWidth(300);
                                imageView.setFitHeight(225);
                                photosContainer.getChildren().add(imageView);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.out.println("Failed to fetch location photos: " + response.code());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });

        // Load review photos
        Request reviewsRequest = new Request.Builder()
                .url(reviewsUrl)
                .get()
                .addHeader("accept", "application/json")
                .build();

        client.newCall(reviewsRequest).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray reviewsArray = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        try {
                            for (int i = 0; i < reviewsArray.length(); i++) {
                                JSONObject review = reviewsArray.getJSONObject(i);
                                if (review.has("user") && review.getJSONObject("user").has("avatar")) {
                                    JSONObject avatar = review.getJSONObject("user").getJSONObject("avatar");
                                    String photoUrl = avatar.optString("large", "");

                                    if (!photoUrl.isEmpty()) {
                                        ImageView imageView = new ImageView(new Image(photoUrl, true));
                                        imageView.setPreserveRatio(true);
                                        imageView.setFitHeight(220); // Set max height
                                        photosContainer.getChildren().add(imageView);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.out.println("Failed to fetch review photos: " + response.code());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadLocationReviews(String locationId, String API_KEY) {
        String reviewsUrl = REVIEWS_API_URL + locationId + "/reviews?key=" + API_KEY + "&language=en";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(reviewsUrl)
                .get()
                .addHeader("accept", "application/json")
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    System.out.println("Raw response: " + responseBody);  // Print the raw response to the console
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray reviewsArray = jsonResponse.getJSONArray("data");

                    StringBuilder reviewsContent = new StringBuilder();

                    System.out.println("Reviews for location ID: " + locationId); // Print location ID to console
                    for (int i = 0; i < reviewsArray.length(); i++) {
                        JSONObject review = reviewsArray.getJSONObject(i);
                        String title = review.optString("title", "No Title");
                        String text = review.optString("text", "No review text available.");

                        // Build the string for TextArea
                        reviewsContent.append("Review #").append(i + 1).append(": ").append(title).append("\n");
                        reviewsContent.append("Text: ").append(text).append("\n");
                        reviewsContent.append("-------------------------------\n");

                        // Print each review to the console
                        System.out.println("Review #" + (i + 1) + ": " + title);
                        System.out.println("Text: " + text);
                        System.out.println("-------------------------------");
                    }

                    String finalText = reviewsContent.toString();
                    Platform.runLater(() -> {
                        reviewsTextArea.setText(finalText); // Update the TextArea on the JavaFX thread
                    });
                } else {
                    System.err.println("Failed to fetch reviews: " + response.code());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }




    //Location Attractions
    private void loadLocationAttractions(String locationId, String apiKey) {

        String attractionsUrl = ATTRACTIONS_API_URL + apiKey + "&searchQuery=" + locationId + "&category=attraction" + "&language=en&currency=USD";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(attractionsUrl)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();

                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray locations = jsonResponse.getJSONArray("data");

                StringBuilder attractionsDetails = new StringBuilder();
                for (int i = 0; i < locations.length(); i++) {
                    JSONObject location = locations.getJSONObject(i);
                    String name = location.getString("name");
                    JSONObject addressObj = location.getJSONObject("address_obj");
                    String addressString = addressObj.getString("address_string");

                    attractionsDetails.append(name).append(" - ").append(addressString).append("\n");
                }

                
                attractionsTextArea.setText(attractionsDetails.toString());

            } else {
                System.out.println("Unsuccessful response: " + response.code() + " - " + response.message());
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            API_KEY = readApiKeyFromJsonFile("src/main/resources/adminSDK.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupFieldListeners();
        setupSliderListeners();

        profilePicImage.setImage(FirebaseStorageAction.getProfilePicture());

        locationTextField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                try {
                    searchLocation();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Set listeners for total budget and vacation length text fields
        totalBudgetTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                totalBudget = Double.parseDouble(newValue);
                updateSliders();
            }
        });

        vacationLengthTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                vacationLength = Integer.parseInt(newValue);
                updateSliders();
            }
        });

        // Set listeners for sliders
        foodSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateTextFields());
        roomAndBoardSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateTextFields());
        spendingSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateTextFields());

        // Initialize text fields based on sliders
        updateTextFields();

        //sample code for loading attractions/hotels/reviews
        List<Activity> attractions = getAttractions();
        List<Activity> hotels = getAttractions();
        List<Activity> reviews = getAttractions();

        loadGrid(attractions, attractionsGrid);
        loadGrid(hotels, hotelsGrid);
        loadGrid(reviews, reviewsGrid);
    }

    private void loadGrid(List<Activity> activities, GridPane gridPane){
        int currGridColumn = 0;
        int currGridRow = 0;

        for(Activity activity : activities) {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/com/travel/virtualtravelassistant/activityCard.fxml"));

            try {
                HBox hbox = fxmlLoader.load();
                ActivityCardController activityCardController = fxmlLoader.getController();
                activityCardController.setActivity(activity);
                activityCardController.setNode(hbox);
                activityCardController.setParentController(this);
                gridPane.add(hbox, currGridColumn, currGridRow++);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void loadToAddedGrid(Activity activity){
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/com/travel/virtualtravelassistant/smallActivityCard.fxml"));

        try {
            HBox hbox = fxmlLoader.load();
            ActivityCardController activityCardController = fxmlLoader.getController();
            activityCardController.setActivity(activity);
            activityCardController.setNode(hbox);
            activityCardController.setParentController(this);
            addedGrid.add(hbox, currGridColumn, currGridRow++);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addToSelectedActivities(Activity activity){
        loadToAddedGrid(activity);
        continueButton.setVisible(true);
    }

    public void handleContinueButton(){

    }

    private List<Activity> getAttractions(){
        Activity activity = new Activity();
        activity.setName("The Italian Restaurant");
        activity.setDescription("Rome, Italy");

        List<Activity> attractions = new ArrayList<>();
        attractions.add(activity);
        attractions.add(activity);
        attractions.add(activity);
        attractions.add(activity);
        attractions.add(activity);

        return attractions;
    }

    private void setupFieldListeners() {
        totalBudgetTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                totalBudget = Double.parseDouble(newValue);
                updateSlidersFromFields();
            } catch (NumberFormatException e) {
                // Handle invalid input
            }
        });

        vacationLengthTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                vacationLength = Integer.parseInt(newValue);
                if (vacationLength > 0) {
                    updateSlidersFromFields();
                }
            } catch (NumberFormatException e) {
            }
        });
    }



    private void setupSliderListeners() {
        roomAndBoardSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            adjustOtherSliders(newValue.doubleValue(), foodSlider, spendingSlider);
        });

        foodSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            adjustOtherSliders(newValue.doubleValue(), roomAndBoardSlider, spendingSlider);
        });

        spendingSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            adjustOtherSliders(newValue.doubleValue(), roomAndBoardSlider, foodSlider);
        });
    }

    private void adjustOtherSliders(double newValue, Slider slider1, Slider slider2) {
        double remaining = 100 - newValue;
        double slider1Value = Math.min(slider1.getValue(), remaining);
        slider1.setValue(slider1Value);
        slider2.setValue(remaining - slider1Value);
    }

    private void updateSlidersFromFields() {
        if (totalBudget > 0 && vacationLength > 0) {
            double dailyBudget = totalBudget / vacationLength;

            roomAndBoardSlider.setValue(0);
            foodSlider.setValue(0);
            spendingSlider.setValue(0);

        }
    }


    private void updateSliders() {
        // Check for valid input before updating sliders
        if (totalBudget > 0 && vacationLength > 0) {
            double dailyBudget = totalBudget / vacationLength;


            double roomAndBoardPercent = 50; // 50% of daily budget
            double foodPercent = 30;         // 30% of daily budget
            double spendingPercent = 20;     // 20% of daily budget

            double roomAndBoardBudget = (dailyBudget * roomAndBoardPercent) / 100;
            double foodBudget = (dailyBudget * foodPercent) / 100;
            double spendingBudget = (dailyBudget * spendingPercent) / 100;

            // Update sliders
            roomAndBoardSlider.setValue(roomAndBoardBudget);
            foodSlider.setValue(foodBudget);
            spendingSlider.setValue(spendingBudget);

            updateTextFields();
        } else {
            System.out.println("Invalid input for budget or vacation length");
        }
    }

    // Update text fields based on slider values changing
    private void updateTextFields() {
        roomAndBoardTextField.setText(String.format("$%.2f", roomAndBoardSlider.getValue()));
        foodTextField.setText(String.format("$%.2f", foodSlider.getValue()));
        spendingTextField.setText(String.format("$%.2f", spendingSlider.getValue()));
    }

    //Strip the $
    private double parseCurrency(String currencyStr) {
        try {
            String numericStr = currencyStr.replaceAll("[^\\d.-]", ""); // Remove $ and any other non-numeric characters
            return Double.parseDouble(numericStr);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing currency: " + e.getMessage());
            return 0; // Default to 0
        }
    }

    //TextField Listeners for room and board and food
    private void setupTextFieldListeners() {
        roomAndBoardTextField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                double value = parseCurrency(roomAndBoardTextField.getText());
                roomAndBoardSlider.setValue(value);
                adjustOtherBudgets();
            }
        });

        foodTextField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                double value = parseCurrency(foodTextField.getText());
                foodSlider.setValue(value);
                adjustOtherBudgets();
            }
        });
    }

    //Adjust sliders for room and board and food
    private void adjustOtherBudgets() {
        double totalBudget = 100; // Total budget percentage
        double roomBoard = roomAndBoardSlider.getValue();
        double food = foodSlider.getValue();
        double spending = spendingSlider.getValue();

        if (roomBoard + food + spending > totalBudget) {
            double scale = totalBudget / (roomBoard + food + spending);
            roomAndBoardSlider.setValue(roomBoard * scale);
            foodSlider.setValue(food * scale);
            spendingSlider.setValue(spending * scale);
        }
        updateTextFields();
    }
}
