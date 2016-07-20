/* 
 * Copyright (C) 2016 John Garner
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

import com.pikatimer.timing.RawTimeData;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 *
 * @author jcgarner
 */
public class PikaRFIDFileReader implements TimingReader{
    
    private TimingListener timingListener;
    private File sourceFile; 
    private final StringProperty fileName; 
    private Pane displayPane; 
    private Button inputButton;
    private TextField inputTextField; 
    private Label statusLabel; 
    private HBox displayHBox; 
    private VBox displayVBox; 
    private Tailer tailer;
    private Thread thread; 
    private final BooleanProperty readingStatus;
    ProgressIndicator watchProgressIndicator;
    ToggleButton watchToggleButton;
    
    public PikaRFIDFileReader(){
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
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home"))); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        
        sourceFile = fileChooser.showOpenDialog(inputButton.getScene().getWindow());
        if (sourceFile != null) {
            fileName.setValue(sourceFile.getAbsolutePath());
            // save the filename 
            timingListener.setAttribute("RFIDFileReader:filename", sourceFile.getAbsolutePath());
            
            // set the text field to the filename
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // read the file
            readOnce();
            
            
        }                
        
    }

    

    @Override
    public void startReading() {
        // make sure the file exists
        
        // If a tailer already exists, stop it
        if (! readingStatus.getValue() ) {
            MyHandler listener = new MyHandler();
            tailer = Tailer.create(sourceFile, listener, 500, Boolean.FALSE, Boolean.TRUE);
            thread = new Thread(tailer);
            thread.setDaemon(true); // optional
            thread.start();
            
            

            readingStatus.setValue(Boolean.TRUE);
        }
    }
    
    @Override
    public void stopReading() {
        if (tailer != null) {
            tailer.stop();
        }
        readingStatus.setValue(Boolean.FALSE);
    }


    @Override
    public void showControls(Pane p) {
        
        if (displayPane == null) {
            // initialize our display
            displayHBox = new HBox();
            displayVBox = new VBox();
            watchProgressIndicator = new ProgressIndicator();
            watchToggleButton = new ToggleButton("Watch File...");
            
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            
            inputButton = new Button("File...");
            inputTextField = new TextField();
            statusLabel = new Label("");
            
            displayHBox.setSpacing(5);
            displayHBox.getChildren().addAll(inputTextField, inputButton, watchToggleButton, watchProgressIndicator); 
            displayVBox.getChildren().addAll(displayHBox, statusLabel); 
            
            // Set the action for the inputButton
            inputButton.setOnAction((event) -> {
                // Button was clicked, do something...
                selectInput();
            });
            
            watchProgressIndicator.visibleProperty().bind(watchToggleButton.selectedProperty());
            watchProgressIndicator.setProgress(-1.0);
            // get the current status of the reader
            //watchProgressIndicator.setPrefHeight(30.0);
            watchProgressIndicator.setMaxHeight(30.0);
            watchToggleButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                
                
                if(newValue) {
                    System.out.println("PikaRFIDFileReader: watchToggleButton event: calling startReading()");
                    startReading();
                } else {
                    System.out.println("PikaRFIDFileReader: watchToggleButton event: calling stopReading()");
                    stopReading();
                }
            });
            watchToggleButton.selectedProperty().bindBidirectional(readingStatus);
            
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // set the action for the inputTextField
            
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
    
    private class MyHandler extends TailerListenerAdapter {

        @Override
        public void handle(String line) {
            PikaRFIDFileReader.this.handle(line);
            //System.out.println(line);
        }

    }
    
    
    public void handle(String s) {
        
        
        String[] tokens = s.split(",", -1);
        // we only care about the following fields:
        // 0 -- The port (1->4)
        // 1 -- chip
        // 2 -- bib
        // 3 -- time (as a string)

        // Step 1: Make sure we have a time in the 4th field
        // Find out if we have a date + time or just a time
        String reader = tokens[0];
        String chip = tokens[1];
        String dateTime = tokens[3].replaceAll("\"", "");

        //System.out.println("Chip: " + chip);
        //System.out.println("dateTime: " + dateTime);
        
        if (reader.equals("0") && ! chip.equals("0")) { // invalid combo
            System.out.println("Non Start time: " + s);
            return;
        } else if (!reader.matches("[1234]") && !chip.equals("0")){
            System.out.println("Invalid Port: " + s);
            return;
        }

        if(dateTime.matches("^\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$")) {
            if(dateTime.matches("^\\d{1}:\\d{2}:\\d{2}\\.\\d{3}$")) {
                //ISO_LOCAL_TIME wants a two digit hour....
                dateTime = "0" + dateTime;
            }
            try { 
                
                LocalTime timestamp = LocalTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_TIME );
                
                RawTimeData rawTime = new RawTimeData();
                              
                rawTime.setChip(chip);
                rawTime.setTimestampLong(timestamp.toNanoOfDay());

                //data.setChip(chip);
                //data.setTime(fullTime); 
                
                //System.out.println("Added raw time: " + chip + " " + timestamp.toString());
                
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue("Added raw time: " + chip + " " + timestamp.toString());
                });
                
                timingListener.processRead(rawTime); // process it
               
            } catch(DateTimeParseException e) {
                System.out.println("Unable to parse the time in " + dateTime);
            }
        } else {
            System.out.println("Unable to parse the line: " + s);
            
            /* Odds are it is a Date + Time. In which case we need to pull the date of the event and then 
             * get the duration between the start of the event date and the time read. 
             * LocalDate eventDate = timingListener.getEventDate();
             */
        }

    }
        
    @Override
    public void readOnce() {
        // get the event date, just in case we need it
        System.out.println("PikaRFIDFileReader.readOnce called. Current file is: " + sourceFile.getAbsolutePath());
        
        // Run this in a thread.... 
        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                try {            
                    Files.lines(sourceFile.toPath())
                        .map(s -> s.trim())
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> {
                            //System.out.println("readOnce read " + s); 
                            handle(s); 
                        });
                } catch (IOException ex) {
                    Logger.getLogger(PikaRFIDFileReader.class.getName()).log(Level.SEVERE, null, ex);
                    // We had an issue reading the file.... 
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    

    @Override
    public void setTimingListener(TimingListener t) {
        timingListener = t; 
        
        // get any existing attributes
        String filename = timingListener.getAttribute("RFIDFileReader:filename");
        if (filename != null) {
            System.out.println("RFIDFileReader: Found existing file setting: " + filename);
            sourceFile = new File(filename);
            fileName.setValue(sourceFile.getAbsolutePath());
            
        } else {
            System.out.println("RFIDFileReader: Did not find existing file setting." );
        }
        
        
    }
    
    @Override
    public Boolean chipIsBib() {
        return Boolean.FALSE; 
    }
    
}
