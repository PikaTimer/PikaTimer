/* 
 * Copyright (C) 2017 John Garner
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
package com.pikatimer;


import com.pikatimer.event.EventDAO;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.PikaFilePathWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.internal.dbsupport.FlywaySqlScriptException;
import org.h2.store.fs.FilePath;

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
    
    @FXML private HBox OpenHBox;
    @FXML private VBox LoadingVBox;
    
    @FXML private Label pikaVersionLabel;
    
    //private final Event event = Event.getInstance(); 
    private final Stage primaryStage = Pikatimer.getPrimaryStage();
    private String jdbcURL;
    
    Preferences globalPrefs = PikaPreferences.getInstance().getGlobalPreferences();
    
    @FXML
    protected void initialize() {
        // initialize your logic here: all @FXML variables will have been injected
        //LoadingProgressBar.setVisible(false);
        //LoadingLabel.setVisible(false);
        //LoadingProgressBar.setVisible(false);
        LoadingVBox.setVisible(false);
        LoadingVBox.setManaged(false);
        
        pikaVersionLabel.setText(Pikatimer.VERSION);
    }
    
    @FXML
    protected void openDB(File dbFile) {
        
        System.out.println("Opening " + dbFile.getAbsolutePath());

        
        OpenHBox.setVisible(false);
        OpenHBox.setManaged(false);


        LoadingVBox.setManaged(true);
        LoadingVBox.setVisible(true);

        //LoadingLabel.setVisible(true);
        LoadingProgressBar.setProgress(-1);

        PikaPreferences.getInstance().setRecentFile(dbFile); // stash this for future use
        globalPrefs.put("PikaEventHome", dbFile.getParent());
        System.setProperty("user.dir", dbFile.getParent());

        System.out.println("Just hid the Open stuff and revealed the Loading stuff");
        
        Task loadPika = new Task<Void>() {

                @Override public Void call() {
                    
                    //FXMLLoader loader = null;
                    try {
                        // Create the Flyway instance
                        Flyway flyway = new Flyway();
                        //LoadingProgressBar.setProgress(0.10F);
                        //System.out.println("Progress: " + LoadingProgressBar.getProgress());
                        FilePath.register(new PikaFilePathWrapper());
                        jdbcURL = "jdbc:h2:pika:" + dbFile.getAbsolutePath().replace(".pika", "");
                        jdbcURL += ";MULTI_THREADED=TRUE;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;CACHE_SIZE=131072"; // disable trace options
                        Pikatimer.setJdbcUrl(jdbcURL);
                        //LoadingProgressBar.setProgress(0.15F);
                        
                        //LoadingProgressBar.setProgress(0.2F);

                        // Upgrade the schema (if out of date)
                        // Use a r/o access mode wrapped in a try/catch to look for a schema version db upgrade
                        // since flyway modifies the database upon issuing _any_ command and would prevent us from
                        // saving a copy that v1.0 can still read
                        try {
                            if (dbFile.exists()) {
                                Flyway flyway_check = new Flyway();
                                flyway_check.setDataSource(jdbcURL + ";ACCESS_MODE_DATA=r", "sa", null);
                                Boolean backup_needed = false;
                                try {

                                        MigrationInfo[] pending = flyway_check.info().pending();
                                        if (pending.length > 0) backup_needed = true;

                                } catch (FlywaySqlScriptException sql_ex){
                                    System.out.println("Pending metadata update, saving a copy");
                                    backup_needed = true;
                                }
                                if (backup_needed) {
                                    System.out.println("Pending Migrations, saving a copy");
                                    FileUtils.copyFile(dbFile, new File(dbFile.getAbsolutePath() + ".pre_v1.5_update.pika"));
                                }
                            }
                            flyway.setDataSource(jdbcURL, "sa", null);
                            flyway.migrate();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                Alert alert = new Alert(AlertType.ERROR);
                                alert.setTitle("Unable to Open Event");
                                alert.setHeaderText("Unable to Open Event");
                                alert.setContentText("We had an error opening the event.\nPlease make sure that this event\nis not open in another instance of PikaTimer.");

                                alert.showAndWait();
                                OpenHBox.setVisible(true);
                                OpenHBox.setManaged(true);
                                LoadingVBox.setManaged(false);
                                LoadingVBox.setVisible(false);
                            });
                            return null;
                        } 
                        //LoadingProgressBar.setProgress(0.25F);
                        //System.out.println("Progress: " + LoadingProgressBar.getProgress());
                        // Get Hibernate up and running and populate the event object.... 
                        EventDAO eDAO = new EventDAO();
                        //LoadingProgressBar.setProgress(0.35);
                        eDAO.getEvent();
                        //LoadingProgressBar.setProgress(0.40);
                        //System.out.println("Progress: " + LoadingProgressBar.getProgress());

                        // Setup the main screen
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLpika.fxml"));
                    
                        // Pre-Load stuff...
                        System.out.println("Pre-Loading the data...");
                        TimingDAO.getInstance().listTimingLocations(); //Load first to prevent deadlock!
                        RaceDAO.getInstance().listRaces();
                        ParticipantDAO.getInstance().listParticipants();
                        ParticipantDAO.getInstance().getParticipantByBib("1"); // arbitrary to block on the listParticipants() thread
                        System.out.println("Done Pre-Loading the data...");
                        PikaPreferences.getInstance().setDBLoaded();

                        try {
                            final Pane myPane = (Pane)loader.load();
                            Scene myScene = new Scene(myPane);
                            
                            // F11 to toggle fullscreen mode
                            myScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), () -> {
                                primaryStage.setFullScreen(primaryStage.fullScreenProperty().not().get());
                            });
        
                            Platform.runLater(() -> {
                                //Pane myPane = (Pane)loader.load();
                                //LoadingProgressBar.setProgress(0.75);
                                
                                primaryStage.setScene(myScene);
                                primaryStage.show();
                                
                                Platform.runLater(() -> {
                                    // Center the display
                                    Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();  

                                    //set Stage boundaries so that the main screen is centered.               
                                    primaryStage.setWidth(1000);
                                    primaryStage.setHeight(700);
                                    primaryStage.setX((primaryScreenBounds.getWidth() - primaryStage.getWidth())/2);  
                                    primaryStage.setY((primaryScreenBounds.getHeight() - primaryStage.getHeight())/2);
                                });
                                
                                //LoadingProgressBar.setProgress(0.95);
                                

                            });
                            
                        } catch (Exception ex) {
                            System.out.println("OOPS! " + getClass().getResource("FXMLpika.fxml"));
                            ex.printStackTrace();
                        }    
                    
                    
                    } catch (Exception ex) {
                        System.out.println("OOPS!");
                        ex.printStackTrace();
                    }
                    
                    return null;
                }
            };
            Thread loadPikaThread = new Thread(loadPika);
            
            loadPikaThread.start();
    }
       
    @FXML
    protected void openEvent(ActionEvent fxevent) throws IOException {
        final FileChooser fileChooser = new FileChooser();
        
        fileChooser.setTitle("Open Event");
        
        File lastEventFolder = new File(globalPrefs.get("PikaEventHome", System.getProperty("user.home")));
        if (!lastEventFolder.exists() ) {
            // we have a problem
            lastEventFolder= new File(System.getProperty("user.home"));
        } else if (lastEventFolder.exists() && lastEventFolder.isFile()){
            lastEventFolder = new File(lastEventFolder.getParent());
           
        }
        
        System.out.println("Using initial directory of " + lastEventFolder.getAbsolutePath());

        fileChooser.setInitialDirectory(lastEventFolder); 
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PikaTimer Events", "*.pika"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(rootGridPane.getScene().getWindow());
        System.out.println("Opening existing file....");
        if (file != null) {
            
            // does the file end in .mv.db? 
            // Often due to a rename that only preserves the .db part
            // This can go byby once H2 supports a custom file extension. 
//            if (!file.getName().endsWith(".mv.db")) {
//                System.out.println("File does not end in .mv.db: " + file.getAbsolutePath());
//                if (file.getName().endsWith(".db")) {
//                    File renamed = new File(file.getAbsolutePath().replace(".db",".mv.db"));
//                    if(file.renameTo(renamed))file = renamed;
//                } else {
//                    File renamed = new File(file.getAbsolutePath().concat(".mv.db"));
//                    if (file.renameTo(renamed)) file = renamed;
//                }
//                System.out.println("File is now " + file.getAbsolutePath());
//            }
            
            // Save a copy 
            // TODO
            
            eventFileName.setText(file.getAbsolutePath().replace(".pika", ""));
            
            //LoadingLabel.setVisible(true);
            //LoadingProgressBar.setProgress(0.00);
            //LoadingProgressBar.setVisible(true);
            
            this.openDB(file); 
            
        }        
    }
    
    @FXML
    protected void newEvent(ActionEvent fxevent) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Create New Event...");
        File lastEventFolder = new File(globalPrefs.get("PikaEventHome", System.getProperty("user.home")));
        if (!lastEventFolder.exists() ) {
            // we have a problem
            lastEventFolder= new File(System.getProperty("user.home"));
        } else if (lastEventFolder.exists() && lastEventFolder.isFile()){
            lastEventFolder = new File(lastEventFolder.getParent());
           
        }
        fileChooser.setInitialDirectory(lastEventFolder); 
        //fileChooser.getExtensionFilters().add(
        //        new FileChooser.ExtensionFilter("PikaTimer Events", "*.db") 
        //    );
        //fileChooser.setInitialFileName("*.pika");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PikaTimer Events", "*.pika"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showSaveDialog(rootGridPane.getScene().getWindow());
        if (file != null) {
            eventFileName.setText(file.getAbsolutePath().replace(".pika", ""));
            this.openDB(file); 
        }
    }
    
    @FXML
    protected void cloneEvent(ActionEvent fxevent) {
        
        
    }
    
    @FXML
    protected void openLink(ActionEvent fxevent) {
        Hyperlink hyperlink = (Hyperlink)fxevent.getSource();
        String link = hyperlink.getText();
        if (link.contains("(")) {
            link = link.replaceFirst("^.+\\(", "").replaceFirst("\\).*$", "");
        }
        System.out.println("Hyperlink pressed: " + link);
        Pikatimer.getInstance().getHostServices().showDocument(link);
    }
}
