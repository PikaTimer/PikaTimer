/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */

package com.pikatimer;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.flywaydb.core.Flyway;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLopenEventController {

    /**
     * Initializes the controller class.
     */
    
    
    @FXML private GridPane rootGridPane; // value will be injected by the FXMLLoader
    @FXML private Label eventName; // ibid
    
    //private final Event event = Event.getInstance(); 
    private final Stage primaryStage = Pikatimer.getPrimaryStage();
    private String jdbcURL;
    
    private void openDB(File dbFile) {
            // Backup the DB file first (always)
            // Open the DB file 
            // Upgrade the schema (if out of date)
                     // Create the Flyway instance
            Flyway flyway = new Flyway();

            jdbcURL = "jdbc:h2:file:" + dbFile.getAbsolutePath().replace(".mv.db", "");
            Pikatimer.setJdbcUrl(jdbcURL);
            
            flyway.setDataSource(jdbcURL , "sa", null);

            // Start the migration
            flyway.migrate();
            
            // Open the main 
            Pane myPane;
            try {
                myPane = (Pane)FXMLLoader.load(getClass().getResource("FXMLpika.fxml"));
                Scene myScene = new Scene(myPane);

                primaryStage.setScene(myScene);
                primaryStage.show();
            } catch (IOException ex) {
                System.out.println("OOPS! " + getClass().getResource("FXMLpika.fxml"));
            }
    }
       
    @FXML
    protected void openEvent(ActionEvent fxevent) throws IOException {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Event");
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        ); 
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PikaTimer Events", "*.db"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(rootGridPane.getScene().getWindow());
        if (file != null) {
            this.openDB(file); 
            
        }        
    }
    
    @FXML
    protected void newEvent(ActionEvent fxevent) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save New Event...");
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        ); 
        //fileChooser.getExtensionFilters().add(
        //        new FileChooser.ExtensionFilter("PikaTimer Events", "*.db") 
        //    );
        //fileChooser.setInitialFileName("*.pika");
        File file = fileChooser.showSaveDialog(rootGridPane.getScene().getWindow());
        if (file != null) {
            this.openDB(file); 
            //eventName.textProperty().bind(event.getEventName());
        }
    }
    
    @FXML
    protected void cloneEvent(ActionEvent fxevent) {
        
        
    }
}
