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

import com.pikatimer.event.Event;
import com.pikatimer.timing.RawTimeData;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import com.pikatimer.util.DurationFormatter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

/**
 *
 * @author jcgarner
 */
public class PikaRFIDDirectReader implements TimingReader {

    protected TimingListener timingListener;
    protected String ultra_ip;
    
    Thread ultraConnectionThread;
    InputStream input = null;
    DataOutputStream ultraOutput = null;
    private static final BlockingQueue<String> commandResultQueue = new ArrayBlockingQueue(10);

    private Pane displayPane; 
    private Button inputButton;
    protected TextField ultraIPTextField; 
    protected TextField readsFileTextField;
    protected Label statusLabel; 
    private HBox displayHBox; 
    private VBox displayVBox; 
    private ProgressBar batteryProgressBar;
    private ToggleSwitch connectToggleSwitch;
    private ToggleSwitch readToggleSwitch;
    
    protected final BooleanProperty readingStatus = new SimpleBooleanProperty();
    protected final BooleanProperty connectedStatus = new SimpleBooleanProperty();

    private Boolean connectToUltra = false;
    
    private int lastRead = -1;
    
    Semaphore okToSend = new Semaphore(1);

    
    public PikaRFIDDirectReader() {
        
        
        
    }

    @Override
    public void setTimingListener(TimingListener t) {
                timingListener = t; 
        
        // get any existing attributes
        ultra_ip = timingListener.getAttribute("RFIDDirect:ultra_ip");
        if (ultra_ip != null) {
            System.out.println("RFIDDirect: Found existing ultra ip setting: " + ultra_ip);
        } else {
            System.out.println("RFIDDirect: Did not find existing ip setting." );
            ultra_ip = "";
        }
    }

    @Override
    public void showControls(Pane p) {
        if (displayPane == null) {
            // initialize our display
            displayHBox = new HBox();
            displayVBox = new VBox();
            batteryProgressBar = new ProgressBar();
            connectToggleSwitch = new ToggleSwitch("Connect");
            connectToggleSwitch.selectedProperty().set(false);
            readToggleSwitch = new ToggleSwitch("Read");
            readToggleSwitch.selectedProperty().set(false);
            readToggleSwitch.disableProperty().bind(connectedStatus.not());
            statusLabel = new Label("");
            inputButton = new Button("Discover...");
            ultraIPTextField = new TextField();
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            connectToggleSwitch.selectedProperty().bindBidirectional(connectedStatus);
            readToggleSwitch.selectedProperty().bindBidirectional(readingStatus);
            
            ultraIPTextField.disableProperty().bind(connectToggleSwitch.selectedProperty()); // no changes when connected
            
            // This is way more complicated than it should be...
            ultraIPTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                Boolean revert = false;
                if (newValue.isEmpty()) connectToggleSwitch.disableProperty().set(true);
                if (ultra_ip == null || ultra_ip.equals(newValue)) {
                    connectToggleSwitch.disableProperty().set(false);
                    return;
                }
                if (newValue.matches("[\\d\\.]+")) { // numbers and dots only for the inital pass
                    String octets[] = newValue.split("\\.");
                    if (octets.length != 4) connectToggleSwitch.disableProperty().set(true);
                    if (octets.length > 4){ // too many octets, cut a few and it will be fine
                        revert = true;
                    } else {
                        Boolean validIP = true;
                        for(String octet: octets) {
                            try {
                                Integer o = Integer.parseInt(octet);
                                System.out.println("Octet : " + o);
                                if (o > 255) {
                                    validIP = false;
                                    revert = true;
                                }
                            } catch (Exception e){
                                System.out.println("Octet Exception: " + e.getLocalizedMessage());

                                validIP = false;
                            }
                        }
                        if (validIP && octets.length == 4) {
                            System.out.println("Valid IP : " + ultra_ip);
                            connectToggleSwitch.disableProperty().set(false);
                            // save the ip if it is new
                            if (!ultra_ip.equals(newValue)) {
                                ultra_ip = newValue;
                                timingListener.setAttribute("RFIDDirect:ultra_ip", ultra_ip);
                                System.out.println("Valid IP : " + ultra_ip);
                            }
                        }
                        else{
                            connectToggleSwitch.disableProperty().set(true);
                        }
                    }
                } else { //just say no
                    revert = true;
                }
                if (revert) {
                        connectToggleSwitch.disableProperty().set(true);
                        Platform.runLater(() -> { 
                        int c = ultraIPTextField.getCaretPosition();
                        ultraIPTextField.setText(oldValue);
                        ultraIPTextField.positionCaret(c);
                    }); 
                }
            });
            
            
            displayHBox.setSpacing(5);
            displayHBox.getChildren().addAll(ultraIPTextField, inputButton, connectToggleSwitch,readToggleSwitch); 
            displayVBox.getChildren().addAll(displayHBox, statusLabel); 
            
            // Set the action for the inputButton
            inputButton.setOnAction((event) -> {
                discover();
            });
            
            batteryProgressBar.visibleProperty().bind(connectToggleSwitch.selectedProperty());

            batteryProgressBar.setMaxHeight(30.0);
            connectToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    if (!connectToUltra) {
                        System.out.println("PikaRFIDDirectReader: connectToggleSwitch event: calling connect()");
                        connect();
                    }
                } else {
                    if (connectToUltra) {
                        System.out.println("PikaRFIDDirectReader: connectToggleSwitch event: calling disconnect()");
                        disconnect();
                    }
                }
            });
            //connectToggleSwitch.selectedProperty().bindBidirectional(connectedStatus);
            readToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    if (connectToUltra) {
                        System.out.println("PikaRFIDDirectReader: readToggleSwitch event: calling startReading()");
                        startReading();
                    }
                } else {
                    if (connectToUltra) {
                        System.out.println("PikaRFIDDirectReader: readToggleSwitch event: calling stopReading()");
                        stopReading();
                    }
                }
            });
            ultraIPTextField.textProperty().setValue(ultra_ip);
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
    public void readOnce() {
        // noop
    }

    @Override
    public void startReading() {
        Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired=true;
                                System.out.println("Sending command 'R'");
                                Platform.runLater(() -> {
                                    statusLabel.setText("Starting Readers...");
                                });

                                ultraOutput.writeChars("R" + Character.toString ((char) 10));
                                ultraOutput.flush();
                                Thread.sleep(5000); // give the readers 5 seconds to start before we check
                                getReadStatus(); 
                            } else {
                                // timeout
                                System.out.println("Timeout with command 'R'");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                        } finally {
                            if (aquired) okToSend.release();
                        }
                    }
                    return null;
                }
        };
        new Thread(ultraCommand).start();
    }

    @Override
    public void stopReading() {
        Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired = true;
                                Platform.runLater(() -> {
                                    statusLabel.setText("Stopping Readers...");
                                });
                                System.out.println("Sending command 'S\\nN\\n'");
                                ultraOutput.writeChars("S" + Character.toString ((char) 10) + "N" + Character.toString ((char) 10));
                                ultraOutput.flush();
                                Thread.sleep(5000); // give the readers 5 seconds to stop before we check
                                getReadStatus();
                            } else {
                                // timeout
                                System.out.println("Timeout with command 'S'");
                            }
                        } catch (InterruptedException ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                        } catch (IOException ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            if (aquired) okToSend.release();
                        }
                    }
                    return null;
                }
        };
        new Thread(ultraCommand).start();
    }

    private void getReadStatus(){
        Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired = true;
                                ultraOutput.writeChars("?" + Character.toString ((char) 10));
                                ultraOutput.flush();
                                String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                if (result != null) {
                                    System.out.println("Reading Status : " + result);
                                    Platform.runLater(() -> {
                                        if (result.substring(2, 3).startsWith("1")) readingStatus.setValue(Boolean.TRUE);
                                        else readingStatus.setValue(Boolean.FALSE);
                                    });
                                    if (statusLabel.getText().equals("Starting Readers...") && readingStatus.get()) {
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Waiting for a chip read...");
                                        });
                                    } else if (!readingStatus.get()){
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Connected: Readers stopped");
                                        });
                                    }
                                } else {
                                // timeout
                                System.out.println("Timeout with command '?'");
                            }
                            } else {
                                // timeout
                                System.out.println("Timeout waiting to send command '?");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                        } finally {
                            if (aquired) okToSend.release();
                        }
                    }
                    return null;
                }
        };
        new Thread(ultraCommand).start();
    }
        
    @Override
    public BooleanProperty getReadingStatus() {
        return readingStatus; 
    }

    @Override
    public Boolean chipIsBib() {
        return Boolean.FALSE; 
    }
    
    private void connect() {
        connectToUltra = true;
        lastRead = -1;
        Task ultraConnection = new Task<Void>() {
                @Override public Void call() {
                    while(connectToUltra) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Connecting to " + ultra_ip + "...");
                        });

                        connectToUltra = false; // prevent looping if the connect fails
                        try (
                            Socket ultraSocket = new Socket(ultra_ip, 23); 
                            InputStream input = ultraSocket.getInputStream();
                            OutputStream output = ultraSocket.getOutputStream();
                        ) {
                            connectToUltra = true; // we got here so we have a good connection
                            ultraSocket.setSoTimeout(20000); // 20 seconds. In theory we get a voltage every 10
                            ultraOutput = new DataOutputStream(output);
                            Platform.runLater(() -> {
                                connectedStatus.setValue(true);
                                statusLabel.setText("Connected to " + ultra_ip);
                            });
                            int read = -255; 
                            String line = "";
                            while (read != 10) { // 1,Connected,<stuff>\n is sent on initial connect. 10 == \n
                                read = input.read();
                                //System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                            } 
                            
                            onConnectSetup();
                            
                            while(connectToUltra) {
                                read = -255; 
                                line = "";
                                while (read != 10 && connectToUltra) {
                                    read = input.read();
                                    if (read == -1) {
                                        connectToUltra = false;
                                        System.out.println("End of stream!" + Integer.toHexString(read));
                                    } if (read != 10) {
                                        line = line +  Character.toString ((char) read);
                                        //System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                                    } else {
                                        processLine(line);
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            System.out.println(e);
                        } finally {
                            Platform.runLater(() -> {
                                connectedStatus.setValue(false);
                                statusLabel.setText("Disconnected");
                            });
                        }
                    }
                    
                    return null; 
                }
            }; 
            ultraConnectionThread  = new Thread(ultraConnection);
            ultraConnectionThread.setName("Thread-Ultra-" + ultra_ip);
            ultraConnectionThread.setDaemon(true);
            ultraConnectionThread.start();
        
    }
    
    private void disconnect(){
        statusLabel.setText("Disconecting from " + ultra_ip + "...");
        connectToUltra = false;
    }
    
    

    
    private void processLine(String line) {
        System.out.println("Read Line: " + line);
//        Platform.runLater(() -> {
//            statusLabel.setText("Read Line: " + line);
//        });
        switch(line.substring(0, 1)){
            case "0": // chip time
            case "1":
                processRead(line);
                break;
            
            case "S": // status 
                System.out.println("Status: " + line);
                
                commandResultQueue.offer(line);
                break;
            case "V": // voltage
                System.out.println("Voltage: " + line);
                getReadStatus();
                break;
            case "U": // command response
                System.out.println("Response: " + line);
                commandResultQueue.offer(line);
                break;
            default: // unknown command response
                System.out.println("Unknown: \"" + line.substring(0, 0) + "\" " + line);
                break;
        }
    }
    private void processRead(String r){
        System.out.println("Chip Read: " + r);
        String[] tokens = r.split(",", -1);
        // 0,11055,1170518701,698,1,-71,0,2,1,0000000000000000,0,29319
        // 0 -- junk
        // 1 -- chip
        // 2 -- time
        // 3 -- milis
        // 4 -- antenna / port
        // 5 -- RSSI (signal strength)
        // 6 -- is Rewind? (0 is live, 1 is memorex)
        // 7 -- Reader A or B (1 or 2)
        // 8 -- UltraID (zeroes)
        // 9 -- MTB Downhill Start Time
        // 10 -- LogID
        
        if (tokens.length < 11 ) {
            System.out.println("  Chip read is missing data: " + r);
        }
        
        String chip = tokens[1];
        String port = tokens[4];
        String reader = tokens[7];
        //String antenna = tokens[x];
        //String rewind = tokens[x];
        String rewind = tokens[6];
        String logNo = tokens[10];
        
        if (rewind.equals("0") && lastRead > 0 ) {
            if (lastRead != (Integer.parseInt(logNo) - 1)) {
                System.out.println("Missing a read: Last " + lastRead + " Current: " + logNo);
            }
        }
        
        System.out.println("  Chip: " + chip);
        
        // make sure we have what we need...
        if (port.equals("0") && ! chip.equals("0")) { // invalid combo
            System.out.println("Non Start time: " + chip);
            return;
        } else if (!port.matches("[1234]") && !chip.equals("0")){
            System.out.println("Invalid Port: " + port);
            return;
        }
        
        LocalDate origin = LocalDate.parse("1980-01-01",DateTimeFormatter.ISO_LOCAL_DATE); 
        LocalDateTime read_ldt = LocalDateTime.of(origin, LocalTime.MIDNIGHT);
        Long seconds = Long.parseLong(tokens[2]);
        Long millis = Long.parseLong(tokens[3]);
        read_ldt = read_ldt.plusSeconds(seconds);
        
        LocalDateTime event_ldt = LocalDateTime.of(Event.getInstance().getLocalEventDate(), LocalTime.MIN);
        
        Duration timestamp = Duration.between(event_ldt,read_ldt).plusMillis(millis);
        
        // if it is before the event date, just return
        if (timestamp.isNegative()) {
            String status = "Read Timestamp of " + timestamp + " is in the past, ignoring";
            Platform.runLater(() -> {
                statusLabel.textProperty().setValue(status);
            });
            System.out.println(status);
        } else {
            RawTimeData rawTime = new RawTimeData();
            rawTime.setChip(chip);
            rawTime.setTimestampLong(timestamp.toNanos());
            String status = "Added raw time: " + tokens[1] + " at " + DurationFormatter.durationToString(timestamp, 3) + " Reader: " + reader + " Port: " + port;
            Platform.runLater(() -> {
                statusLabel.textProperty().setValue(status);
            });
            timingListener.processRead(rawTime); // process it
        }
        
    }
    
    private void onConnectSetup() {
        // Get the reading status
        getReadStatus();
        // get the timezone
         
        // get the time 
        
        // get the antenna status
        
        // Do we have a 2nd reader?
        
        // Auto-Rewind the day's times
}

    private void discover() {
        
    }
}
