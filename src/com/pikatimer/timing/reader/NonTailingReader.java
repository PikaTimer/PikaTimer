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
package com.pikatimer.timing.reader;

import com.pikatimer.PikaPreferences;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.commons.io.input.Tailer;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public abstract class NonTailingReader implements TimingReader{
    
    protected TimingListener timingListener;
    protected File sourceFile; 
    protected final StringProperty fileName; 
    protected Boolean fileValid = false;
    private Pane displayPane; 
    private Button inputButton;
    private Button rereadButton;
    protected TextField inputTextField; 
    protected Label statusLabel = new Label("");
    private HBox displayHBox1; 
    private HBox displayHBox2; 
    private VBox displayVBox; 
    private ChoiceBox offsetChoiceBox;
    protected Tailer tailer;
    protected final BooleanProperty readingStatus;
    protected Duration offset = Duration.ZERO;
    private Thread thread;
    
    public NonTailingReader(){
        fileName = new SimpleStringProperty();
        readingStatus = new SimpleBooleanProperty();
        
    }



    public void selectInput() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        
        if (sourceFile != null) {
            fileChooser.setInitialDirectory(sourceFile.getParentFile()); 
            fileChooser.setInitialFileName(sourceFile.getName());
        } else {
            fileChooser.setInitialDirectory(PikaPreferences.getInstance().getCWD()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt","*.csv"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        
        sourceFile = fileChooser.showOpenDialog(inputButton.getScene().getWindow());
        if (sourceFile != null) {
            // if we are auto-importing, stop that
            readingStatus.set(false);
            
            fileName.setValue(sourceFile.getAbsolutePath());
            // save the filename 
            timingListener.setAttribute("NonTailingReader:filename", sourceFile.getAbsolutePath());
            
            // set the text field to the filename
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // read the file
            if (!sourceFile.canRead()){
                fileValid = false;
                statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
            } else {
                fileValid = true;
                readOnce();
            }
        }                
    }

    
    @Override
    public void startReading() {
        // make sure the file exists
        if (!sourceFile.canRead()){
                statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
                Platform.runLater(() ->{readingStatus.set(false);});
        } else  if (! readingStatus.getValue() ) {
            statusLabel.setText("Reading file: " + fileName.getValueSafe());

            readOnce();
        }
    }
    
    @Override
    public void stopReading() {
        if (thread != null) thread.interrupt();
    }


    @Override
    public void showControls(Pane p) {
        
        if (displayPane == null) {
            // initialize our display
            displayHBox1 = new HBox();
            displayHBox2 = new HBox();
            displayVBox = new VBox();
            
            inputButton = new Button("Select File...");
            rereadButton = new Button("Reread");
            inputTextField = new TextField();
            Label offsetLabel = new Label("Times in File are relative to ");
            offsetChoiceBox = new ChoiceBox(FXCollections.observableArrayList("Race Start","Time of Day"));
            
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            
            inputTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue && !fileName.getValueSafe().equals(inputTextField.textProperty().getValueSafe())) {
                    // if we are auto-importing, stop that
                    readingStatus.set(false);
                    
                    sourceFile = new File(inputTextField.textProperty().getValueSafe()).getAbsoluteFile();
                    fileName.setValue(sourceFile.getPath());

                    
                    // save the filename 
                    timingListener.setAttribute("NonTailingReader:filename", inputTextField.textProperty().getValueSafe());

                    // read the file
                    if (!sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile()){
                        statusLabel.setText("No Such File or Unable to open file: " + fileName.getValueSafe());
                        fileValid = false;
                    } else {
                        fileValid = true;
                        readOnce();
                    }
                        
                } else {
                    System.out.println("No change in file name");
                }
            });
            
            displayHBox1.setSpacing(5);
            displayHBox1.setAlignment(Pos.CENTER_LEFT);
            displayHBox1.getChildren().addAll(inputTextField, inputButton, rereadButton); 
            displayHBox2.setSpacing(5);
            displayHBox2.setAlignment(Pos.CENTER_LEFT);
            displayHBox2.getChildren().addAll(offsetLabel, offsetChoiceBox); 
            displayVBox.getChildren().addAll(displayHBox1, displayHBox2,statusLabel); 
            
            // Set the action for the inputButton
            inputButton.setOnAction((event) -> {
                selectInput();
            });
            rereadButton.setOnAction((event) -> {
                readOnce();
            });
            
            rereadButton.visibleProperty().bind(inputTextField.textProperty().isEmpty().not());
            rereadButton.managedProperty().bind(inputTextField.textProperty().isEmpty().not());
            
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // set the action for the inputTextField
            
            String initialOffset = timingListener.getAttribute("NonTailingReader:offset");
            if (initialOffset == null) {
                initialOffset="Race Start";
                timingListener.setAttribute("NonTailingReader:offset",initialOffset);
            }
            
            offsetChoiceBox.getSelectionModel().select(initialOffset);
            
            offsetChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                  public void changed(ObservableValue ov, String value, String new_value) {
                      if (new_value.equals("Race Start")) {
                          timingListener.setAttribute("NonTailingReader:offset",new_value);
                          List<Wave> waves = new ArrayList(RaceDAO.getInstance().listWaves());
                          waves.sort((w1,w2) -> w1.waveStartProperty().compareTo(w2.waveStartProperty()));
                          offset = Duration.between(LocalTime.MIDNIGHT, waves.get(0).waveStartProperty());
                          System.out.println("NonTailingReader Offset now " + offset);
                      } else {
                          timingListener.setAttribute("NonTailingReader:offset",new_value);
                          offset = Duration.ZERO;
                          System.out.println("NonTailingReader Offset now " + offset);

                      }
                      
                      readOnce();
                      
                  }
                });
        }
        
        // If we were previously visible, clear the old one
        if (displayPane != null) displayPane.getChildren().clear();
        
        // Now show ourselves.... 
        displayPane = p; 
        displayPane.getChildren().clear();
        displayPane.getChildren().add(displayVBox); 
        
        
    }

    @Override
    public BooleanProperty getReadingStatus() {
        return readingStatus; 
    }
    
   
    
    
    public abstract void process(String s);
        
    @Override
    public void readOnce() {
        
        if (!fileValid) {
            statusLabel.setText("No Such File or Unable to open file: " + fileName.getValueSafe());
            return;
        }
        
        System.out.println("NonTailingReader.readOnce called. Current file is: " + sourceFile.getAbsolutePath());
        timingListener.clearReads();
        // Run this in a thread.... 
        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                try (Stream<String> s = Files.lines(sourceFile.toPath())) {
                    s.map(line -> line.trim()).filter(line -> !line.isEmpty()).forEach(line -> {
                        //System.out.println("readOnce read " + s); 
                        process(line); 
                    });
                    s.close();
                } catch (Exception ex){
                    ex.printStackTrace();
                }
                return null;
            }
        };
        thread = new Thread(task);
        thread.setDaemon(true); 
        thread.start();
    }
    
    

    @Override
    public void setTimingListener(TimingListener t) {
        timingListener = t; 
        
        // get any existing attributes
        String filename = timingListener.getAttribute("NonTailingReader:filename");
        if (filename != null) {
            System.out.println("NonTailingReader: Found existing file setting: " + filename);
            sourceFile = new File(filename).getAbsoluteFile();
            fileName.setValue(filename);
            if (!sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile()){
                statusLabel.setText("No Such File or Unable to open file: " + fileName.getValueSafe());
                fileValid = false;
            } else fileValid = true;
            
        } else {
            System.out.println("NonTailingReader: Did not find existing file setting." );
        }
        
        
    }
    
//    @Override
//    public Boolean chipIsBib() {
//        return Boolean.TRUE; 
//    }

    
}
