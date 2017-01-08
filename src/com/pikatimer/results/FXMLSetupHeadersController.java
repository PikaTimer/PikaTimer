/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.results;

import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.HBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.WindowEvent;



/**
 * FXML Controller class
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class FXMLSetupHeadersController {
    Race race;
    
    @FXML Label raceLabel;
    @FXML CheckBox textForBothCheckBox;
    
    @FXML TextArea textHeaderTextArea;
    @FXML TextArea textMessageTextArea;
    @FXML TextArea textFooterTextArea;
    
    @FXML TextField gaTextField;
    @FXML TextField cssTextField;
    
    @FXML HBox headerHBox;
    @FXML HTMLEditor htmlHeaderHTMLEditor;
    @FXML TextArea htmlHeaderTextArea;
    
    @FXML HBox messageHBox;
    @FXML HTMLEditor htmlMessageHTMLEditor;
    @FXML TextArea htmlMessageTextArea;
    
    @FXML HBox footerHBox;
    @FXML HTMLEditor htmlFooterHTMLEditor;
    @FXML TextArea htmlFooterTextArea;
    
    @FXML Label copyFromLabel;
    @FXML ChoiceBox<Race> copyFromChoiceBox;
        
    @FXML ChoiceBox<String> htmlEditorChoiceBox;
    
    @FXML Button saveButton;
    @FXML Button cancelButton;
    
    BooleanProperty headersModified = new SimpleBooleanProperty(false);

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        htmlEditorChoiceBox.setItems(FXCollections.observableArrayList("Simple", "Advanced"));
        htmlEditorChoiceBox.getSelectionModel().selectedItemProperty().addListener((ob, oldOp, newOp) -> {
            if (newOp.equals(oldOp)) return;
            
            if (newOp.equals("Simple")){
                
                htmlHeaderTextArea.visibleProperty().set(false);
                htmlHeaderTextArea.managedProperty().set(false);
                htmlHeaderHTMLEditor.visibleProperty().set(true);
                htmlHeaderHTMLEditor.managedProperty().set(true);
                htmlHeaderHTMLEditor.setHtmlText(htmlHeaderTextArea.getText());
                
                htmlMessageTextArea.visibleProperty().set(false);
                htmlMessageTextArea.managedProperty().set(false);
                htmlMessageHTMLEditor.visibleProperty().set(true);
                htmlMessageHTMLEditor.managedProperty().set(true);
                htmlMessageHTMLEditor.setHtmlText(htmlMessageTextArea.getText());
                
                htmlFooterTextArea.visibleProperty().set(false);
                htmlFooterTextArea.managedProperty().set(false);
                htmlFooterHTMLEditor.visibleProperty().set(true);
                htmlFooterHTMLEditor.managedProperty().set(true);
                htmlFooterHTMLEditor.setHtmlText(htmlFooterTextArea.getText());
                
            } else {
                Double height = htmlHeaderHTMLEditor.getHeight();
                Double width = htmlHeaderHTMLEditor.getWidth();
                
                htmlHeaderTextArea.visibleProperty().set(true);
                htmlHeaderTextArea.managedProperty().set(true);
                htmlHeaderHTMLEditor.visibleProperty().set(false);
                htmlHeaderHTMLEditor.managedProperty().set(false);
                htmlHeaderTextArea.setText(htmlHeaderHTMLEditor.getHtmlText().replace("<html dir=\"ltr\"><head></head><body contenteditable=\"true\">","").replace("</body></html>", ""));
                htmlHeaderTextArea.setPrefHeight(height);
                htmlHeaderTextArea.setPrefWidth(width);
                
                htmlMessageTextArea.visibleProperty().set(true);
                htmlMessageTextArea.managedProperty().set(true);
                htmlMessageHTMLEditor.visibleProperty().set(false);
                htmlMessageHTMLEditor.managedProperty().set(false);
                htmlMessageTextArea.setText(htmlMessageHTMLEditor.getHtmlText().replace("<html dir=\"ltr\"><head></head><body contenteditable=\"true\">","").replace("</body></html>", ""));
                htmlMessageTextArea.setPrefHeight(height);
                htmlMessageTextArea.setPrefWidth(width);
                
                htmlFooterTextArea.visibleProperty().set(true);
                htmlFooterTextArea.managedProperty().set(true);
                htmlFooterHTMLEditor.visibleProperty().set(false);
                htmlFooterHTMLEditor.managedProperty().set(false);
                htmlFooterTextArea.setText(htmlFooterHTMLEditor.getHtmlText().replace("<html dir=\"ltr\"><head></head><body contenteditable=\"true\">","").replace("</body></html>", ""));
                htmlFooterTextArea.setPrefHeight(height);
                htmlFooterTextArea.setPrefWidth(width);
            }
        });
        htmlEditorChoiceBox.getSelectionModel().selectFirst();
        
        
        headerHBox.disableProperty().bind(textForBothCheckBox.selectedProperty());
        messageHBox.disableProperty().bind(textForBothCheckBox.selectedProperty());
        footerHBox.disableProperty().bind(textForBothCheckBox.selectedProperty());
        htmlEditorChoiceBox.disableProperty().bind(textForBothCheckBox.selectedProperty());
        textForBothCheckBox.selectedProperty().addListener((ob, oldS, newS) -> {
            if (!race.getBooleanAttribute("textOnlyHeaders").equals(newS)) headersModified.set(true);
            if (newS) {
                htmlEditorChoiceBox.getSelectionModel().selectLast();
            } else {
                htmlEditorChoiceBox.getSelectionModel().select(race.getStringAttribute("htmlEditor"));
            }
        });

        // if any races have custom headers, add them to the list
        copyFromChoiceBox.getSelectionModel().clearSelection();
        
        saveButton.disableProperty().bind(headersModified.not());
        
        gaTextField.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("GACode").equals(newT)) headersModified.set(true);
        });
        cssTextField.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("CSSUrl").equals(newT)) headersModified.set(true);
        });
        
        textHeaderTextArea.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("textHeader").equals(newT)) headersModified.set(true);
        });
        textMessageTextArea.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("textMessage").equals(newT)) headersModified.set(true);
        });
        textFooterTextArea.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("textFooter").equals(newT)) headersModified.set(true);
        });
        
        htmlHeaderTextArea.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("htmlHeader").equals(newT)) headersModified.set(true);
        });
        htmlMessageTextArea.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("htmlMessage").equals(newT)) headersModified.set(true);
        });
        htmlFooterTextArea.textProperty().addListener((op, oldT, newT) -> {
            if (!race.getStringAttribute("htmlFooter").equals(newT)) headersModified.set(true);
        });
        
        htmlHeaderHTMLEditor.addEventHandler(InputEvent.ANY, (InputEvent event) -> {
            if (!race.getStringAttribute("htmlHeader").equals(htmlHeaderHTMLEditor.getHtmlText().replace("<html dir=\"ltr\"><head></head><body contenteditable=\"true\">","").replace("</body></html>", ""))) {
                headersModified.set(true);
            }
        });
        htmlMessageHTMLEditor.addEventHandler(InputEvent.ANY, (InputEvent event) -> {
            if (!race.getStringAttribute("htmlMessage").equals(htmlMessageHTMLEditor.getHtmlText().replace("<html dir=\"ltr\"><head></head><body contenteditable=\"true\">","").replace("</body></html>", ""))) headersModified.set(true);
        });
        htmlFooterHTMLEditor.addEventHandler(InputEvent.ANY, (InputEvent event) -> {
            if (!race.getStringAttribute("htmlFooter").equals(htmlFooterHTMLEditor.getHtmlText().replace("<html dir=\"ltr\"><head></head><body contenteditable=\"true\">","").replace("</body></html>", ""))) headersModified.set(true);
        });
        
        
        // We use an arbitrary node to get the window we are in to set the exit handler
        // Wrap this in a runLater to avoid a NPE since the window does not yet exist
        Platform.runLater(()-> {
            saveButton.getScene().getWindow().setOnCloseRequest( event -> {
                if (headersModified.getValue()){
                    Alert closeConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    closeConfirmation.setContentText("There are unsaved changes report headers and footers.");
                    Button closeButton = (Button) closeConfirmation.getDialogPane().lookupButton(
                            ButtonType.OK
                    );
                    closeButton.setText("Close Anyway");
                    closeConfirmation.setHeaderText("Unsaved Changes...");
                    Optional<ButtonType> closeResponse = closeConfirmation.showAndWait();
                    if (!ButtonType.OK.equals(closeResponse.get())) {
                        event.consume();
                    }
                }
            });
        }); 
    }    
    
    public void setRace(Race r){
        race = r;
        raceLabel.setText(race.getRaceName());
        
        
        
        if (race.getStringAttribute("GACode") == null) race.setStringAttribute("GACode","");
        gaTextField.setText(race.getStringAttribute("GACode"));
        
        if (race.getStringAttribute("CSSUrl") == null) race.setStringAttribute("CSSUrl","");
        cssTextField.setText(race.getStringAttribute("CSSUrl"));
        
        if (race.getStringAttribute("textHeader") == null) race.setStringAttribute("textHeader","");
        textHeaderTextArea.setText(race.getStringAttribute("textHeader"));
        if (race.getStringAttribute("textMessage") == null) race.setStringAttribute("textMessage","");
        textMessageTextArea.setText(race.getStringAttribute("textMessage"));
        if (race.getStringAttribute("textFooter") == null) race.setStringAttribute("textFooter","");
        textFooterTextArea.setText(race.getStringAttribute("textFooter"));
        
        htmlEditorChoiceBox.getSelectionModel().selectLast();
        if (race.getStringAttribute("htmlHeader") == null) race.setStringAttribute("htmlHeader","");
        htmlHeaderTextArea.setText(race.getStringAttribute("htmlHeader"));
        if (race.getStringAttribute("htmlMessage") == null) race.setStringAttribute("htmlMessage","");
        htmlMessageTextArea.setText(race.getStringAttribute("htmlMessage"));
        if (race.getStringAttribute("htmlFooter") == null) race.setStringAttribute("htmlFooter","");
        htmlFooterTextArea.setText(race.getStringAttribute("htmlFooter"));
        htmlEditorChoiceBox.getSelectionModel().selectFirst();
        
        if (race.getBooleanAttribute("textOnlyHeaders") == null) race.setBooleanAttribute("textOnlyHeaders",false);
        textForBothCheckBox.selectedProperty().set(race.getBooleanAttribute("textOnlyHeaders"));
        
        if (race.getStringAttribute("htmlEditor") == null) race.setStringAttribute("htmlEditor",htmlEditorChoiceBox.getSelectionModel().getSelectedItem());
        Platform.runLater(()-> {
            htmlEditorChoiceBox.getSelectionModel().select(race.getStringAttribute("htmlEditor"));
        });
        
        ObservableList<Race> otherRaces = FXCollections.observableArrayList();
        RaceDAO.getInstance().listRaces().forEach(or -> {
            if (or.getID().equals(race.getID())) return;
            if (or.getBooleanAttribute("useCustomHeaders") != null && or.getBooleanAttribute("useCustomHeaders")) otherRaces.add(or);
        });
        if (otherRaces.isEmpty()) {
            copyFromLabel.visibleProperty().set(false);
            copyFromChoiceBox.visibleProperty().set(false);
        } else {
            copyFromChoiceBox.setItems(otherRaces);
            copyFromChoiceBox.getSelectionModel().clearSelection();
        }
        
        copyFromChoiceBox.getSelectionModel().selectedItemProperty().addListener((ob, oldR, newR) -> {
            if (newR == null || newR == oldR) {
                System.out.println("copyFromChoiceBox Listener: newR is null or equal to oldR");
                return;
            }
            
            Alert copyConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
                copyConfirmation.setContentText("This will overwrite all headers and footers\n and replace them with the settings\nfrom " + newR.getRaceName());
                Button copyButton = (Button) copyConfirmation.getDialogPane().lookupButton(
                        ButtonType.OK
                );
                copyButton.setText("Copy");
                copyConfirmation.setHeaderText("Overwrite all...");
                Optional<ButtonType> closeResponse = copyConfirmation.showAndWait();
                if (ButtonType.OK.equals(closeResponse.get())) {
                    System.out.println("copyFromChoiceBox Listener: copying from " + newR.getRaceName());

                    
                    if (newR.getStringAttribute("GACode") == null) newR.setStringAttribute("GACode","");
                    gaTextField.setText(newR.getStringAttribute("GACode"));

                    if (newR.getStringAttribute("CSSUrl") == null) newR.setStringAttribute("CSSUrl","");
                    cssTextField.setText(newR.getStringAttribute("CSSUrl"));

                    if (newR.getStringAttribute("textHeader") == null) newR.setStringAttribute("textHeader","");
                    textHeaderTextArea.setText(newR.getStringAttribute("textHeader"));
                    if (newR.getStringAttribute("textMessage") == null) newR.setStringAttribute("textMessage","");
                    textMessageTextArea.setText(newR.getStringAttribute("textMessage"));
                    if (newR.getStringAttribute("textFooter") == null) newR.setStringAttribute("textFooter","");
                    textFooterTextArea.setText(newR.getStringAttribute("textFooter"));

                    htmlEditorChoiceBox.getSelectionModel().selectLast();
                    if (newR.getStringAttribute("htmlHeader") == null) newR.setStringAttribute("htmlHeader","");
                    htmlHeaderTextArea.setText(newR.getStringAttribute("htmlHeader"));
                    if (newR.getStringAttribute("htmlMessage") == null) newR.setStringAttribute("htmlMessage","");
                    htmlMessageTextArea.setText(newR.getStringAttribute("htmlMessage"));
                    if (newR.getStringAttribute("htmlFooter") == null) newR.setStringAttribute("htmlFooter","");
                    htmlFooterTextArea.setText(newR.getStringAttribute("htmlFooter"));
                    htmlEditorChoiceBox.getSelectionModel().selectFirst();

                    if (newR.getBooleanAttribute("textOnlyHeaders") == null) newR.setBooleanAttribute("textOnlyHeaders",false);
                    textForBothCheckBox.selectedProperty().set(newR.getBooleanAttribute("textOnlyHeaders"));

                    if (newR.getStringAttribute("htmlEditor") == null) newR.setStringAttribute("htmlEditor",htmlEditorChoiceBox.getSelectionModel().getSelectedItem());
                    Platform.runLater(()-> {
                        htmlEditorChoiceBox.getSelectionModel().select(newR.getStringAttribute("htmlEditor"));
                    });
                }
                Platform.runLater(()-> {
                    copyFromChoiceBox.getSelectionModel().clearSelection();
                });
        });
    
    }
    
    protected void saveAll(){
        race.setBooleanAttribute("textOnlyHeaders", textForBothCheckBox.selectedProperty().get());
        
        race.setStringAttribute("GACode", gaTextField.getText());
        race.setStringAttribute("CSSUrl", cssTextField.getText());
        
        race.setStringAttribute("textHeader",textHeaderTextArea.getText());
        race.setStringAttribute("textMessage",textMessageTextArea.getText());
        race.setStringAttribute("textFooter",textFooterTextArea.getText());
        
        race.setStringAttribute("htmlEditor", htmlEditorChoiceBox.getSelectionModel().getSelectedItem());
        
        htmlEditorChoiceBox.getSelectionModel().select("Advanced");
        race.setStringAttribute("htmlHeader",htmlHeaderTextArea.getText());
        race.setStringAttribute("htmlMessage",htmlMessageTextArea.getText());
        race.setStringAttribute("htmlFooter",htmlFooterTextArea.getText());
        
        RaceDAO.getInstance().updateRace(race);
    }
    
    public void cancelButtonAction(ActionEvent fxevent){
        // We will just kick off the onClose action below
        ((Node) fxevent.getSource()).getScene().getWindow().fireEvent(
            new WindowEvent((
                    (Node) fxevent.getSource()).getScene().getWindow(),
                    WindowEvent.WINDOW_CLOSE_REQUEST
            )
        );
    }
    public void saveButtonAction(ActionEvent fxevent){
        saveAll();
        headersModified.setValue(false);
        ((Node) fxevent.getSource()).getScene().getWindow().fireEvent(
            new WindowEvent((
                    (Node) fxevent.getSource()).getScene().getWindow(),
                    WindowEvent.WINDOW_CLOSE_REQUEST
            )
        );
    }
}
