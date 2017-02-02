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

import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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
            statusLabel = new Label("");
            inputButton = new Button("Discover...");
            ultraIPTextField = new TextField();
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            connectToggleSwitch.selectedProperty().bindBidirectional(connectedStatus);
            
            ultraIPTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (newPropertyValue) return;
                if (!newPropertyValue && !ultra_ip.equals(ultraIPTextField.textProperty().getValueSafe())) {
                    
                    ultra_ip = ultraIPTextField.textProperty().getValueSafe();
                    
                    // save the ip 
                    timingListener.setAttribute("RFIDDirect:ultra_ip", ultra_ip);

                        
                } else {
                    System.out.println("No change in ip address");
                }
            });
            
            displayHBox.setSpacing(5);
            displayHBox.getChildren().addAll(ultraIPTextField, inputButton, connectToggleSwitch); 
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
        readingStatus.setValue(Boolean.TRUE);
    }

    @Override
    public void stopReading() {
        readingStatus.setValue(Boolean.FALSE);
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
                            
                            Platform.runLater(() -> {
                                connectedStatus.setValue(true);
                                statusLabel.setText("Connected to " + ultra_ip);
                            });
                            int read = -255; 
                            String line = "";
                            while (read != 100) { // 1,Connected is sent on initial connect. 100 == 'd'
                                read = input.read();
                                System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                            } 
                            
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
                                        System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
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
            new Thread(ultraConnection).start();
        
}
    
    private void disconnect(){
        statusLabel.setText("Disconecting from " + ultra_ip + "...");
        connectToUltra = false;
    }
    
    private String queryUltra(String s){
        return "";
    }
    private void setUltra(String setting, String value){
        
    }
    private void processLine(String line) {
        System.out.println("Read Line: " + line);
    }
    private void processRead(String r){
        
    }

    private void discover() {
        
    }
}
