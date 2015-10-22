/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing.reader;

import com.pikatimer.timing.RawTimeData;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingLocationInput;
import com.pikatimer.timing.TimingReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
    private StringProperty fileName; 
    private Pane displayPane; 
    private Button inputButton;
    private TextField inputTextField; 
    private Label statusLabel; 
    private HBox displayHBox; 
    private VBox displayVBox; 
    private Tailer tailer;
    private Thread thread; 
    private BooleanProperty readingStatus;
    
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

            readingStatus.setValue(Boolean.FALSE);
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
            
            displayVBox.setSpacing(5); 
            displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            inputButton = new Button("File...");
            inputTextField = new TextField();
            statusLabel = new Label();
            
            displayHBox.setSpacing(5);
            displayHBox.getChildren().addAll(inputTextField, inputButton); 
            displayVBox.getChildren().addAll(displayHBox, statusLabel); 
            
            // Set the action for the inputButton
            inputButton.setOnAction((event) -> {
                // Button was clicked, do something...
                selectInput();
            });
            
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
        
        LocalDate eventDate = timingListener.getEventDate();
        String[] tokens = s.split(",", -1);
        // we only care about the following fields:
        // 0 -- The reader
        // 1 -- chip
        // 2 -- bib
        // 3 -- time (as a string)

        // Step 1: Make sure we have a time in the 4th field
        // Find out if we have a date + time or just a time
        String chip = tokens[1];
        String dateTime = tokens[3].replaceAll("\"", "");

        //System.out.println("Chip: " + chip);
        //System.out.println("dateTime: " + dateTime);

        if(dateTime.matches("^\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$")) {
            if(dateTime.matches("^\\d{1}:\\d{2}:\\d{2}\\.\\d{3}$")) {
                //ISO_LOCAL_TIME wants a two digit hour....
                dateTime = "0" + dateTime;
            }
            try { 
                LocalTime time = LocalTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_TIME );
                RawTimeData rawTime = new RawTimeData();
                LocalDateTime fullTime = LocalDateTime.of(eventDate, time);
                
                rawTime.setChip(chip);
                rawTime.setTimestamp(fullTime);

                //data.setChip(chip);
                //data.setTime(fullTime); 
                
                System.out.println("Added raw time: " + chip + " " + fullTime.toString());
                timingListener.processRead(rawTime); // process it
               
            } catch(DateTimeParseException e) {
                System.out.println("Unable to parse the time in " + dateTime);
            }
        } else {
            System.out.println("Unable to parse the line: " + s);
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
                            System.out.println("readOnce read " + s); 
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
    
}
