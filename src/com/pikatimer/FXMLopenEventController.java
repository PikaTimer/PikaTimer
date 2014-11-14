/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */

package com.pikatimer;


import com.pikatimer.event.EventDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
    @FXML private Label eventFileName; 
    @FXML private Label LoadingLabel;
    @FXML private ProgressBar LoadingProgressBar;
    
    //private final Event event = Event.getInstance(); 
    private final Stage primaryStage = Pikatimer.getPrimaryStage();
    private String jdbcURL;
    
    @FXML
    protected void initialize() {
        // initialize your logic here: all @FXML variables will have been injected
        //LoadingProgressBar.setVisible(false);
        LoadingLabel.setVisible(false);
    }
    
    @FXML
    protected void openDB(File dbFile) {
        
            // TODO
            // None of the progress bar stuff works. 
            // move the stuff to a Task so that the main visual thread can get
            // updated. 
            LoadingLabel.setVisible(true);
            LoadingProgressBar.setProgress(0.00);
            LoadingProgressBar.setVisible(true);
            
            
            // Backup the DB file first (always)
            // Open the DB file 
            
            
            // Create the Flyway instance
            Flyway flyway = new Flyway();
            LoadingProgressBar.setProgress(0.10F);
            System.out.println("Progress: " + LoadingProgressBar.getProgress());
            jdbcURL = "jdbc:h2:file:" + dbFile.getAbsolutePath().replace(".mv.db", "");
            Pikatimer.setJdbcUrl(jdbcURL);
            LoadingProgressBar.setProgress(0.15F);
            flyway.setDataSource(jdbcURL , "sa", null);
            LoadingProgressBar.setProgress(0.2F);
            
            // Upgrade the schema (if out of date)
            flyway.migrate();
            LoadingProgressBar.setProgress(0.25F);
            System.out.println("Progress: " + LoadingProgressBar.getProgress());
            // Get Hibernate up and running and populate the event object.... 
            EventDAO eDAO = new EventDAO(); 
            LoadingProgressBar.setProgress(0.35);
            eDAO.getEvent(); 
            LoadingProgressBar.setProgress(0.40);
            System.out.println("Progress: " + LoadingProgressBar.getProgress());
            
            
            // Setup the main screen
            Pane myPane;
            try {
                myPane = (Pane)FXMLLoader.load(getClass().getResource("FXMLpika.fxml"));
                LoadingProgressBar.setProgress(0.75);
                Scene myScene = new Scene(myPane);

                primaryStage.setScene(myScene);
                LoadingProgressBar.setProgress(0.95);
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
            eventFileName.setText(file.getAbsolutePath().replace(".mv.db", ""));
            
            LoadingLabel.setVisible(true);
            LoadingProgressBar.setProgress(0.00);
            LoadingProgressBar.setVisible(true);
            
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
            eventFileName.setText(file.getAbsolutePath().replace(".mv.db", ""));
            this.openDB(file); 
        }
    }
    
    @FXML
    protected void cloneEvent(ActionEvent fxevent) {
        
        
    }
}
