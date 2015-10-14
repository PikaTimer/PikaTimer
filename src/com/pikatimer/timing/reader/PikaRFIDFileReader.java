/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing.reader;

import com.pikatimer.timing.RawTimeData;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 *
 * @author jcgarner
 */
public class PikaRFIDFileReader implements TimingReader{
    
    private TimingLocationInput timingLocationInput;
    private File sourceFile; 
    private StringProperty fileName; 
    private Pane displayPane; 
    private Button inputButton;
    private TextField inputTextField; 

    
    public PikaRFIDFileReader(){
        fileName = new SimpleStringProperty();
    }

    @Override
    public void setInput(String input) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
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
            timingLocationInput.setAttribute("RFIDFileReader:filename", sourceFile.getAbsolutePath());
            
            // read the file
            readOnce();
            
            
        }                
        
    }

    @Override
    public StringProperty getInputStringProperty() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void startReading() {
        // make sure the file exists
        MyListener listener = new MyListener();
        Tailer tailer = Tailer.create(new File("/tmp/log.txt"), listener, 500);

    }
    
    public class MyListener extends TailerListenerAdapter {

        @Override
        public void handle(String line) {
            System.out.println(line);
        }

    }
    
    public void readOnce() {
        // get the event date, just in case we need it
        LocalDate eventDate = timingLocationInput.getEventDate();
        
        // Run this in a thread.... 
        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                try {            
                    Files.lines(sourceFile.toPath())
                        .map(s -> s.trim())
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> {
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
                                    RawTimeData data = new RawTimeData();
                                    LocalDateTime fullTime = LocalDateTime.of(eventDate, time);

                                    //data.setChip(chip);
                                    //data.setTime(fullTime);
                                    timingLocationInput.addRawTime(data); // process it
                                    System.out.println("Added raw time: " + chip + " " + fullTime.toString());
                                } catch(DateTimeParseException e) {
                                    System.out.println("Unable to parse the time in " + dateTime);
                                }
                            } else {
                                System.out.println("Unable to parse the line: " + s);
                            }

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
    public void stopReading() {
        
    }

    @Override
    public void setTimingInput(TimingLocationInput t) {
        timingLocationInput = t; 
        
        // get the pane to display our stuff in
        
        // get any existing attributes
        String filename = timingLocationInput.getAttribute("RFIDFileReader:filename");
        if (filename != null) {
            System.out.println("RFIDFileReader: Found existing file setting: " + filename);
            sourceFile = new File(filename);
            fileName.setValue(sourceFile.getAbsolutePath());
        } else {
            System.out.println("RFIDFileReader: Did not find existing file setting." );
        }
        // initialize our local variables 
        inputButton = timingLocationInput.getInputButton();
        inputTextField = timingLocationInput.getInputTextField();
        inputButton.setOnAction((event) -> {
            // Button was clicked, do something...
            selectInput();
        });
        // display the controls in the pane
        
    }
    
}
