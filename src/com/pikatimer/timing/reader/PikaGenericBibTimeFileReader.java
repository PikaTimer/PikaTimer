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
import com.pikatimer.event.Event;
import com.pikatimer.timing.RawTimeData;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.controlsfx.control.ToggleSwitch;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class PikaGenericBibTimeFileReader implements TimingReader{
        
    protected TimingListener timingListener;
    protected File sourceFile; 
    protected final StringProperty fileName; 
    protected Pane displayPane; 
    private Button inputButton;
    protected TextField inputTextField; 
    protected Label statusLabel; 
    private HBox displayHBox; 
    protected VBox displayVBox; 
    protected Tailer tailer;
    private Thread tailingThread; 
    private Thread readingThread;
    protected final BooleanProperty readingStatus;
    ProgressIndicator watchProgressIndicator;
    ToggleSwitch autoImportToggleSwitch;
    private Semaphore reading = new Semaphore(1);
    
    private SimpleIntegerProperty bibIndex;
    private SimpleIntegerProperty timeIndex;

    
    public PikaGenericBibTimeFileReader(){

        fileName = new SimpleStringProperty();
        readingStatus = new SimpleBooleanProperty();
        
        bibIndex = new SimpleIntegerProperty();
        timeIndex = new SimpleIntegerProperty();
    }



    public void selectInput() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        
        if (sourceFile != null && sourceFile.exists()) {
            fileChooser.setInitialDirectory(sourceFile.getParentFile()); 
            fileChooser.setInitialFileName(sourceFile.getName());
        } else {
            fileChooser.setInitialDirectory(PikaPreferences.getInstance().getCWD()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        
        sourceFile = fileChooser.showOpenDialog(inputButton.getScene().getWindow());
        if (sourceFile != null) {
            // if we are auto-importing, stop that
            readingStatus.set(false);
            
            fileName.setValue(sourceFile.getAbsolutePath());
            // save the filename 
            timingListener.setAttribute("TailingReader:filename", sourceFile.getAbsolutePath());
            
            // set the text field to the filename
            inputTextField.textProperty().setValue(fileName.getValueSafe());
            // read the file
            if (!sourceFile.canRead()){
                statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
            } else readOnce();
        }                
    }

    

    @Override
    public void startReading() {
        System.out.println("TailingReader:StartReading() called");
        if (tailingThread != null && tailingThread.isAlive()) return;
        if (readingThread != null && readingThread.isAlive()) return;
        Task readingTask = new Task<Void>() {
                @Override public Void call() {
                    try {
                        // make sure the file exists

                        
                        while (readingStatus.getValue() && (sourceFile == null || !sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile())){
                            Thread.sleep(1000);
                            System.out.println("Waiting for " + sourceFile.getPath());
                            Platform.runLater(() ->{
                                statusLabel.setText("Waiting for " + sourceFile.getPath());
                            });
                        }   
                        if (readingStatus.getValue() ) {
                            Platform.runLater(() -> statusLabel.setText("Reading file: " + sourceFile.getPath()));

                            MyHandler listener = new MyHandler();
                            tailer = new Tailer(sourceFile, listener, 1000, Boolean.FALSE, Boolean.TRUE);
                            tailingThread = new Thread(tailer);
                            tailingThread.setDaemon(true); // optional
                            tailingThread.start();
                            readingStatus.setValue(Boolean.TRUE);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PikaGenericBibTimeFileReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
                }
        };
        readingThread = new Thread(readingTask);
        readingThread.setDaemon(true); // optional
        readingThread.start();
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
            autoImportToggleSwitch = new ToggleSwitch("Auto-Import File");
            autoImportToggleSwitch.selectedProperty().set(false);
            autoImportToggleSwitch.setPadding(new Insets(3, 0, 0, 0)); // this is a hack to get around a ToggleSwitch bug
            //autoImportToggleSwitch.setMaxWidth(75);
            statusLabel = new Label("");
            inputButton = new Button("Select File...");
            inputTextField = new TextField();
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            
            inputTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue && !fileName.getValueSafe().equals(inputTextField.textProperty().getValueSafe())) {
                    // if we are auto-importing, stop that
                    stopReading();
                    
                    sourceFile = new File(inputTextField.textProperty().getValueSafe()).getAbsoluteFile();
                    fileName.setValue(sourceFile.getAbsolutePath());

                    
                    // save the filename 
                    timingListener.setAttribute("TailingReader:filename", inputTextField.textProperty().getValueSafe());

                    // read the file
                    if (!sourceFile.canRead()){
                        statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
                    } else readOnce();
                        
                } else {
                    System.out.println("No change in file name");
                }
            });
            
            displayHBox.setSpacing(5);
            displayHBox.setAlignment(Pos.CENTER_LEFT);
            displayHBox.getChildren().addAll(inputTextField, inputButton, autoImportToggleSwitch, watchProgressIndicator); 
            
            Label bibIndexLabel = new Label("Chip Field Index:");
            Spinner<Integer> bibIndexSpinner = new Spinner(1, 9, 1);
            bibIndexSpinner.setPrefSize(25, 25);
            HBox bibIndexHBox = new HBox();
            bibIndexHBox.setSpacing(5);
            bibIndexHBox.setAlignment(Pos.CENTER_LEFT);
            bibIndexHBox.getChildren().addAll(bibIndexLabel,bibIndexSpinner);
            
            bibIndexSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                System.out.println("bibIndexSpinner new value: "+newValue);
                timingListener.setAttribute("TailingReader:bibIndex", newValue.toString());
            });
            
            bibIndex.bind(bibIndexSpinner.valueProperty());


            HBox timeIndexHBox = new HBox();
            Label timeIndexLabel = new Label("Time Field Index:");
            Spinner<Integer>  timeIndexSpinner = new Spinner(1, 9, 2);
            timeIndexSpinner.setPrefSize(25, 25);
            timeIndexHBox.setSpacing(5);
            timeIndexHBox.setAlignment(Pos.CENTER_LEFT);
            timeIndexHBox.getChildren().addAll(timeIndexLabel,timeIndexSpinner);
            
            timeIndexSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                System.out.println("timeIndexSpinner new value: "+newValue);
                timingListener.setAttribute("TailingReader:timeIndex", newValue.toString());
            });
            
            timeIndex.bind(timeIndexSpinner.valueProperty());

            displayVBox.getChildren().addAll(bibIndexHBox,timeIndexHBox);
            
            
            
            
            displayVBox.getChildren().addAll(displayHBox, bibIndexHBox, timeIndexHBox, statusLabel); 
            
            // Set the action for the inputButton
            inputButton.setOnAction((event) -> {
                // Button was clicked, do something...
                selectInput();
            });
            
            watchProgressIndicator.visibleProperty().bind(autoImportToggleSwitch.selectedProperty());
            watchProgressIndicator.setProgress(-1.0);
            // get the current status of the reader
            //watchProgressIndicator.setPrefHeight(30.0);
            watchProgressIndicator.setMaxHeight(30.0);
            autoImportToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    System.out.println("TailingReader: autoImportToggleSwitch event: calling startReading()");
                    startReading();
                } else {
                    System.out.println("TailingReader: autoImportToggleSwitch event: calling stopReading()");
                    stopReading();
                }
            });
            autoImportToggleSwitch.selectedProperty().bindBidirectional(readingStatus);
            
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
            System.out.println("handle: " + line);
            process(line);
        }
    }
    
    @Override
    public void readOnce() {
        // get the event date, just in case we need it
        System.out.println("TailingReader.readOnce called.");
        stopReading();
        
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile()) {
            statusLabel.setText("Unable to open file: " + fileName.getValueSafe());
            return;
        }
        System.out.println("  Current file is: \"" + sourceFile.getAbsolutePath() + "\"");
        // Run this in a tailingThread....
        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                try {
                    reading.acquire();
                    try (Stream<String> s = Files.lines(sourceFile.toPath())) {
                        s.map(line -> line.trim()).filter(line -> !line.isEmpty()).forEach(line -> {
                            //System.out.println("readOnce read " + s);
                            process(line);
                        });
                        s.close();
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    
                } catch (Exception ex){
                    ex.printStackTrace();
                }
                reading.release();
                return null;
            }
        };
        new Thread(task).start();
        
    }
    
    public void process(String s) {
        String port="";
        String chip="";
        String dateAndTime="";
        
        if (s.contains(",")) {
            String[] tokens = s.split(",", -1);
            // we only care about the following fields:
            // 0 -- The port (1->4)
            // 1 -- chip
            // 2 -- bib
            // 3 -- time (as a string)
            // 4 & 5 -- the Reader {1 or 2} and Port (again)

            // Step 1: Make sure we have a time in the 4th field
            // Find out if we have a date + time or just a time
            port = tokens[0];
            chip = tokens[1];
            //String bib = tokens[2]; // We don't care what the bib is
            dateAndTime = tokens[3].replaceAll("\"", "");
            
            if (port.equals("0") && ! chip.equals("0")) { // invalid combo
                System.out.println("Non Start time: " + s);
                return;
            } else if (!port.matches("[1234]") && !chip.equals("0")){
                System.out.println("Invalid Port: " + s);
                return;
            }
            
        } else if (s.contains("\t")){
            String[] tokens = s.split("\t", -1);
            // we only care about the following fields:
            
            // 0 -- chip
            // 1 -- time (as a string)
            // 2 -- the Reader {1 or 2} 
            // 3 -- The port (1->4)
            

            // Step 1: Make sure we have a time in the 4th field
            // Find out if we have a date + time or just a time
            chip = tokens[0];
            //String bib = tokens[2]; // We don't care what the bib is
            dateAndTime = tokens[1].replaceAll("\"", "");
        }
                
                
        String date = null;
        String time = null;
        String[] dateTime = dateAndTime.split(" ", 2);
        if (dateTime.length > 1) {
            date = dateTime[0];
            time = dateTime[1];
        } else time = dateTime[0];

        //System.out.println("Chip: " + chip);
        //System.out.println("dateTime: " + dateTime);
        

        
        
        Duration timestamp = Duration.ZERO;
        if (date != null) {
            // parse the date
            try { 
                LocalDate d = LocalDate.parse(date,DateTimeFormatter.ISO_LOCAL_DATE); 
                
                // set the timestamp to the duration between the event start
                // and this time
                timestamp = Duration.ofDays(Event.getInstance().getLocalEventDate().until(d, ChronoUnit.DAYS));
                // if it is before the event date, just return
                if (timestamp.isNegative()) {
                    String status = "Date of " + date + " is in the past, ignoring";
                    System.out.println(status);
                    Platform.runLater(() -> {
                        statusLabel.textProperty().setValue(status);
                    });
                    return;
                } 
            } catch (Exception e) {
                String status = "Unable to parse the date in \"" + date +"\" : " + e.getMessage();
                System.out.println(status);
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue(status);
                });
                return;
            }
        }

        // First look for timestams without a date attached to them
        if(time.matches("^\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$")) {
            if(time.matches("^\\d{1}:\\d{2}:\\d{2}\\.\\d{3}$")) {
                //ISO_LOCAL_TIME wants a two digit hour....
                time = "0" + time;
            }
            if (DurationParser.parsable(time)){ 
                timestamp = timestamp.plus(DurationParser.parse(time));
                //LocalTime timestamp = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME );
                RawTimeData rawTime = new RawTimeData();
                rawTime.setChip(chip);
                rawTime.setTimestampLong(timestamp.toNanos());
                String status = "Added raw time: " + chip + " at " + DurationFormatter.durationToString(timestamp, 3);
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue(status);
                });
                timingListener.processRead(rawTime); // process it
            } else {
                String status = "Unable to parse the time in " + time;
                System.out.println(status);
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue(status);
                });
            }
        } else {
            String status="Unable to parse the time: " + s;
            System.out.println(status);
            Platform.runLater(() -> {
                statusLabel.textProperty().setValue(status);
            });
            
        }

    }

    @Override
    public void setTimingListener(TimingListener t) {
        timingListener = t; 
        
        // get any existing attributes
        String filename = timingListener.getAttribute("TailingReader:filename");
        if (filename != null) {
            System.out.println("TailingReader: Found existing file setting: " + filename);
            sourceFile = new File(filename).getAbsoluteFile();
            fileName.setValue(filename);
            
        } else {
            System.out.println("TailingReader: Did not find existing file setting." );
        }
        
        
    }
    
    @Override
    public Boolean chipIsBib() {
           return Boolean.TRUE; 
    }

}
