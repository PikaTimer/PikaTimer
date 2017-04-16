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
package com.pikatimer.timing.reader;

import com.pikatimer.PikaPreferences;
import com.pikatimer.event.Event;
import com.pikatimer.timing.FXMLTimingController;
import com.pikatimer.timing.RawTimeData;
import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.util.Pair;
import org.controlsfx.control.ToggleSwitch;

/**
 *
 * @author jcgarner
 */
public class PikaRFIDDirectReader implements TimingReader {

    LocalDateTime EPOC = LocalDateTime.of(LocalDate.parse("1980-01-01",DateTimeFormatter.ISO_LOCAL_DATE),LocalTime.MIDNIGHT);
    
    protected TimingListener timingListener;
    protected String ultraIP;
    
    Thread ultraConnectionThread;
    InputStream input = null;
    DataOutputStream ultraOutput = null;
    private static final BlockingQueue<String> commandResultQueue = new ArrayBlockingQueue(10);

    private Pane displayPane; 
    private Button discoverButton;
    private Button rewindButton;
    protected TextField ultraIPTextField; 
    protected TextField readsFileTextField;
    protected Label statusLabel; 
    protected Label lastReadLabel;
    private HBox displayHBox; 
    private VBox displayVBox; 
    private ProgressBar batteryProgressBar;
    private ToggleSwitch connectToggleSwitch;
    private ToggleSwitch readToggleSwitch;
    
    CheckBox saveToFileCheckBox = new CheckBox("Save to File:");
    TextField fileTextField = new TextField();
    Button fileButton = new Button("Select...");
            
    ChoiceBox<String> reader1ModeChoiceBox = new ChoiceBox(FXCollections.observableArrayList("Start", "Finish"));
    Spinner<Integer> gatingIntervalSpinner = new Spinner<>(1, 20, 3);    
    
    Button setClockButton = new Button("Sync Time...");
    Label modeLabel = new Label("Reader Mode:");
    Label gatingLabel = new Label("Gating Interval:");
    Button updateSettingsButton = new Button("Update");
            
    private Map<String,String> ultraSettings = new HashMap();
    private UltraClock ultraClock = new UltraClock();

    
    protected final BooleanProperty readingStatus = new SimpleBooleanProperty();
    protected final BooleanProperty connectedStatus = new SimpleBooleanProperty();

    private Boolean connectToUltra = false;
    private Boolean externalInitiated = false;
    
    protected final BooleanProperty clockIssues = new SimpleBooleanProperty(false);
    
    private int lastRead = -1;
    
    Semaphore okToSend = new Semaphore(1);
    
    private Boolean saveToFile = false;
    private String backupFile = null;
    private PrintWriter outputFile = null;

    
    public PikaRFIDDirectReader() {
    }

    @Override
    public void setTimingListener(TimingListener t) {
                timingListener = t; 
        
        // get any existing attributes
        ultraIP = timingListener.getAttribute("RFIDDirect:ultra_ip");
        if (ultraIP != null) {
            System.out.println("RFIDDirect: Found existing ultra ip setting: " + ultraIP);
        } else {
            System.out.println("RFIDDirect: Did not find existing ip setting." );
            ultraIP = "";
            timingListener.setAttribute("RFIDDirect:ultra_ip", ultraIP);
        }
        
        saveToFile = Boolean.valueOf(timingListener.getAttribute("RFIDDirect:saveToFile"));
        if (ultraIP != null) {
            System.out.println("RFIDDirect: Found existing saveToFile setting: " + saveToFile);
        } else {
            System.out.println("RFIDDirect: Did not find existing saveToFile setting." );
            saveToFile = false;
            timingListener.setAttribute("RFIDDirect:saveToFile", saveToFile.toString());
        }
        
        backupFile = timingListener.getAttribute("RFIDDirect:backupFile");
        if (backupFile != null) {
            System.out.println("RFIDDirect: Found existing backupFile setting: " + backupFile);
        } else {
            System.out.println("RFIDDirect: Did not find existing ip setting." );
            backupFile = "";
            timingListener.setAttribute("RFIDDirect:backupFile", backupFile);
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

            connectToggleSwitch.maxWidth(30);
            HBox.setHgrow(connectToggleSwitch, Priority.NEVER);

            
            
            connectToggleSwitch.setPadding(new Insets(3, 0, 0, 0)); // this is a hack to get around a ToggleSwitch bug

            readToggleSwitch = new ToggleSwitch("Read");
            readToggleSwitch.selectedProperty().set(false);
            readToggleSwitch.disableProperty().bind(connectedStatus.not());
            readToggleSwitch.setPadding(new Insets(3, 0, 0, 0)); // this is a hack to get around a ToggleSwitch bug
            readToggleSwitch.maxWidth(30);
            HBox.setHgrow(readToggleSwitch, Priority.NEVER);
            
            HBox switchHBox = new HBox();
            switchHBox.maxHeight(18);
            switchHBox.prefWidth(30);
            switchHBox.maxWidth(30);
            switchHBox.setSpacing(5);
            switchHBox.getChildren().addAll(connectToggleSwitch,readToggleSwitch);
            
            Label ipLabel = new Label("Ultra IP:");
            statusLabel = new Label("Disconnected");
            statusLabel.setPrefWidth(200);
            lastReadLabel = new Label("");
            lastReadLabel.setPrefWidth(300);
            discoverButton = new Button("Discover...");
            rewindButton = new Button("Rewind...");
            ultraIPTextField = new TextField();
            ultraIPTextField.setPrefWidth(90);
            ultraIPTextField.setMinWidth(USE_PREF_SIZE);
            displayVBox.setSpacing(5); 
            //displayVBox.setPadding(new Insets(5, 5, 5, 5));
            
            connectToggleSwitch.selectedProperty().bindBidirectional(connectedStatus);
            readToggleSwitch.selectedProperty().bindBidirectional(readingStatus);
            
            ultraIPTextField.disableProperty().bind(connectToggleSwitch.selectedProperty()); // no changes when connected
            
            // This is way more complicated than it should be...
            ultraIPTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                Boolean revert = false;
                if (newValue.isEmpty()) connectToggleSwitch.disableProperty().set(true);
                if (ultraIP == null || newValue.isEmpty() || ultraIP.equals(newValue)) {
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
                            System.out.println("Valid IP : " + ultraIP);
                            connectToggleSwitch.disableProperty().set(false);
                            // save the ip if it is new
                            if (!ultraIP.equals(newValue)) {
                                ultraIP = newValue;
                                timingListener.setAttribute("RFIDDirect:ultra_ip", ultraIP);
                                System.out.println("Valid IP : " + ultraIP);
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
            TitledPane settingsTitlePane = new TitledPane();
            VBox settingsVBox = new VBox();
            settingsVBox.setSpacing(4);
            
            HBox fileHBox = new HBox();
            fileHBox.setSpacing(4);
            fileHBox.setAlignment(Pos.CENTER_LEFT);

            fileHBox.getChildren().addAll(saveToFileCheckBox,fileTextField,fileButton);
            
            HBox advancedHBox = new HBox();
            advancedHBox.setSpacing(4);
            advancedHBox.setAlignment(Pos.CENTER_LEFT);
            
            gatingIntervalSpinner.setMaxWidth(60);
            gatingIntervalSpinner.setEditable(true);
            
            // REMOVE THIS once JDK-8150946 is backported or we upgrade to JDK 9
            gatingIntervalSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    gatingIntervalSpinner.increment(0); // won't change value, but will commit editor
                    Integer gating =Integer.parseInt(ultraSettings.get("1E"));
                    if (!gatingIntervalSpinner.getValue().equals(gating)) updateSettingsButton.setVisible(true);
                }
            });
            reader1ModeChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if ("Start".equals(newVal)) {
                    gatingIntervalSpinner.disableProperty().set(true);
                    if (!"0".equals(ultraSettings.get("14"))) updateSettingsButton.setVisible(true);
                }
                else {
                    gatingIntervalSpinner.disableProperty().set(false);
                    if (!"3".equals(ultraSettings.get("14"))) {
                        updateSettingsButton.setVisible(true);
                        gatingIntervalSpinner.getValueFactory().setValue(5);
                    }

                }
            });
            
            updateSettingsButton.setVisible(false);
            updateSettingsButton.setOnAction(action -> {
                updateReaderSettings();
            });
            
            advancedHBox.getChildren().addAll(setClockButton,modeLabel,reader1ModeChoiceBox,gatingLabel,gatingIntervalSpinner,updateSettingsButton);
            
            advancedHBox.disableProperty().bind(connectedStatus.and(readingStatus).or(connectedStatus.not()));
            
            settingsVBox.getChildren().addAll(fileHBox,advancedHBox);
            
            settingsTitlePane.setText("Advanced Settings");
            settingsTitlePane.setContent(settingsVBox);
            settingsTitlePane.setExpanded(false);
            HBox statusHBox = new HBox();
            statusHBox.getChildren().addAll(statusLabel,lastReadLabel);
            statusHBox.setSpacing(5);
            
            Label strut = new Label();
            strut.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(strut, Priority.ALWAYS);
            displayHBox.setSpacing(5);
            displayHBox.setAlignment(Pos.CENTER_LEFT);
            displayHBox.getChildren().addAll(ipLabel,ultraIPTextField, discoverButton, switchHBox,strut, rewindButton); 
            displayVBox.setAlignment(Pos.CENTER_LEFT);
            displayVBox.getChildren().addAll(displayHBox,statusHBox ,settingsTitlePane); 
            
            // Set the action for the discoverButton
            discoverButton.setOnAction((event) -> {
                discover();
            });
            discoverButton.disableProperty().bind(connectedStatus);
            
            rewindButton.setOnAction((event) -> {
                rewind();
            });
            rewindButton.disableProperty().bind(connectedStatus.not());
            
            setClockButton.setOnAction((event) -> {
                setClockDialog();
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
            
            readToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    if (connectToUltra && !externalInitiated) {
                        System.out.println("PikaRFIDDirectReader: readToggleSwitch event: calling startReading()");
                        startReading();
                    }
                } else {
                    if (connectToUltra && !externalInitiated) {
                        System.out.println("PikaRFIDDirectReader: readToggleSwitch event: calling stopReading()");
                        stopReading();
                    }
                }
                externalInitiated = false;
            });
            ultraIPTextField.textProperty().setValue(ultraIP);
            
            // save to file stuff
            saveToFileCheckBox.setSelected(saveToFile);
            saveToFileCheckBox.selectedProperty().addListener((ob, oldVal, newVal) -> {
                System.out.println("saveToFileCheckBox changed: " + oldVal + " -> " + newVal);
                if (!newVal.equals(saveToFile)){
                    saveToFile=newVal;
                    timingListener.setAttribute("RFIDDirect:saveToFile", saveToFile.toString());
                }
                
            });
            
            fileTextField.setText(backupFile);
            fileTextField.focusedProperty().addListener((ob, oldVal, newVal) -> {
                if (!newVal) {
                    if (fileTextField.getText().isEmpty()){
                        saveToFileCheckBox.setSelected(false);
                        if (!backupFile.isEmpty()) {
                            
                            backupFile="";
                            timingListener.setAttribute("RFIDDirect:backupFile", backupFile);
                        }
                        return;
                    }
                    
                    File newFile = new File(fileTextField.getText()).getAbsoluteFile();
                    System.out.println("Testing file " + newFile.getAbsolutePath());
                    Boolean goodFile=false;
                    try {
                        if (newFile.canWrite() || newFile.createNewFile()) {
                            backupFile=newFile.getPath();
                            timingListener.setAttribute("RFIDDirect:backupFile", fileTextField.getText());
                            goodFile=true;
                        }
                    } catch (IOException ex) {
                        //Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (!goodFile) {
                        // warn and revert
                        Platform.runLater(() -> {
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Unable to write to file");
                            alert.setHeaderText("Unable to write to the selected file!");
                            alert.setContentText("The chosen file path, " +  newFile.getPath() + "\neither does not exist or is not writable.");
                            alert.showAndWait();
                        });
                        fileTextField.setText(backupFile);
                        saveToFileCheckBox.setSelected(false);
                    }
                }
            });
                    
                    
            fileButton.setOnAction(event -> {
                final FileChooser fileChooser = new FileChooser();
        
                fileChooser.setTitle("Save File");

                File cwd = PikaPreferences.getInstance().getCWD();


                System.out.println("Using initial directory of " + cwd.getAbsolutePath());
                
                fileChooser.setInitialFileName(timingListener.getLocationName() + ".txt");
                fileChooser.setInitialDirectory(cwd); 
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                        new FileChooser.ExtensionFilter("All files", "*")
                    );
                File file = fileChooser.showSaveDialog(fileButton.getScene().getWindow());
                if (file != null) {
                    Platform.runLater(() -> fileTextField.setText(file.getAbsolutePath()));
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

                                ultraOutput.writeBytes("R");
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
                                ultraOutput.writeBytes("S");
                                ultraOutput.flush();
                                ultraOutput.writeBytes("N");
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
                                System.out.println("getReadStatus(): Sending ? command");
                                ultraOutput.writeBytes("?");
                                //ultraOutput.writeUTF("?");
                                ultraOutput.flush();
                                String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                if (result != null) {
                                    System.out.println("Reading Status : " + result);
                                    Boolean currentStatus = readingStatus.getValue();
                                    Boolean newStatus = false;
                                    if (result.substring(2, 3).startsWith("1")) newStatus = true;
                                    else newStatus = false;
                                    if (newStatus != currentStatus){
                                        externalInitiated = true;
                                        Platform.runLater(() -> {
                                            if (result.substring(2, 3).startsWith("1")) readingStatus.setValue(Boolean.TRUE);
                                            else readingStatus.setValue(Boolean.FALSE);
                                        });
                                    }
                                    if (statusLabel.getText().equals("Starting Readers...") && newStatus) {
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Connected: Waiting for a chip read...");
                                        });
                                    } else if (statusLabel.getText().equals("Connected: Readers stopped") && newStatus){
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Connected: Waiting for a chip read...");
                                        });
                                    } else if (!newStatus){
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
                                System.out.println("Timeout waiting to send command '?'");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                        } finally {
                            if (aquired) System.out.println("Relasing transmit lock");
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

    public void getSettings(CountDownLatch latch){
        
        Task ultraCommand = new Task<Void>() {
            @Override public Void call() {
                if (connectedStatus.get()) {
                    Boolean aquired = false;
                    try {
                        if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                            aquired = true;

                            String command = "r";
                            ultraOutput.writeBytes(command);

                            ultraOutput.flush();
                            String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                            if (result != null) {
                                // result is HH:MM:SS DD-MM-YYYY
                                String[] dateTime = result.split(" ");
                                String[] d = dateTime[1].split("-");
                                if (d[1].length() == 1) d[1] = "0" + d[1];
                                if (d[0].length() == 1) d[0] = "0" + d[0];
                                String[] t = dateTime[0].split(":");
                                if (t[2].length() == 1) t[2] = "0" + t[2];
                                if (t[1].length() == 1) t[1] = "0" + t[1];
                                if (t[0].length() == 1) t[0] = "0" + t[0];
                                String time = t[0] + ":" + t[1] + ":" + t[2];
                                String date = d[2] + "-" + d[1] + "-" + d[0];
                                ultraClock.date = LocalDate.parse(date);
                                ultraClock.time = LocalTime.parse(time);
                                ultraClock.takenAt = LocalTime.now();
                                ultraClock.takenAt = ultraClock.takenAt.minusNanos(ultraClock.takenAt.getNano());

                            } else {
                            // timeout
                                System.out.println("Timeout with command 't'");
                                return null;
                            }

                            System.out.println("getClock(): Sending tz (0x23) command");

                            ultraOutput.flush();

                            ultraOutput.writeBytes("U");
//                            ultraOutput.writeByte(35);  // 0x23
//                            ultraOutput.writeBytes("-9".toString());

                            ultraOutput.writeByte(10);

                            ultraOutput.flush();
                            do {
                                result = commandResultQueue.poll(5, TimeUnit.SECONDS);
                                if (result != null) {
                                    byte[] r = result.getBytes();
                                    if (result.length() > 2) {
                                        ultraSettings.put(String.format("%02X", r[1]), result.substring(2));
                                        System.out.println("Settings: " + String.format("%02X", r[1]) + " -> " +result.substring(2));
                                    } else if (result.equals("U2")) result= null; // 
                                }
                            } while(result != null);
                            
                            
                            
                            
                            //Gating Factor
                            try{
                                if (!ultraSettings.containsKey("1E")) System.out.println("We don't know what the gating is");
                                else {
                                    Integer gating =Integer.parseInt(ultraSettings.get("1E"));
                                    Platform.runLater(() -> gatingIntervalSpinner.getValueFactory().setValue(gating));
                                    System.out.println("Setting the gating factor to " + gating);
                                }
                            } catch (Exception e){
                                Platform.runLater(() -> gatingIntervalSpinner.getValueFactory().setValue(5));
                                System.out.println("Gating parse error, setting the gating factor to 5");
                            }
                            
                            // reader mode
                            try{
                                ultraSettings.get("14");
                                if ("0".equals(ultraSettings.get("14"))) Platform.runLater(() -> reader1ModeChoiceBox.getSelectionModel().select("Start"));
                                else Platform.runLater(() -> reader1ModeChoiceBox.getSelectionModel().select("Finish"));
                            } catch (Exception e){
                                Platform.runLater(() -> reader1ModeChoiceBox.getSelectionModel().selectFirst());
                            }
                        } else {
                            // timeout
                            System.out.println("Timeout waiting to send command '?'");
                            return null;
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                    } finally {
                        if (aquired) System.out.println("Relasing transmit lock");
                        if (aquired) okToSend.release();
                    }
                } else return null;


                ultraClock.tzHalfOffsetSupport = false;
        
                if (latch != null) latch.countDown();
                
                return null;
            }
        };
        new Thread(ultraCommand).start();
    }
    
    public void setClock(LocalDateTime time, Integer tz, Boolean gps){
        Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired = true;
                                Boolean commit=false;
                                Boolean restartInterface=false;
                                if (time != null) {
                                    // t[0x20]HH:MM:SS DD-MM-YYYY  
                                    LocalDateTime adjTime = time.minusNanos(time.getNano()); // strip the nanoseconds
                                    String[] date = time.format(DateTimeFormatter.ISO_LOCAL_DATE).split("-"); // YYYY-MM-DD
                                    String newTime = adjTime.format(DateTimeFormatter.ISO_LOCAL_TIME) + " " +
                                        date[2] + "-" + date[1] + "-" + date[0]; // flip the ISO_LOCAL_DATE arouond
                                    String command = "t " + newTime;
                                    System.out.println("setClock(): Sending t command for a time of " + newTime);

                                    ultraOutput.writeBytes(command);
                                    //ultraOutput.writeUTF("?");
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 't'");
                                    }
                                }
                                if (tz != null){
                                    System.out.println("setClock(): Sending tz (0x23) command");
                                    // t[0x20]HH:MM:SS DD-MM-YYYY  
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(35);  // 0x23
                                    ultraOutput.writeBytes(tz.toString());
                                    ultraOutput.writeByte(255);
                                    
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        ultraSettings.put("23",tz.toString());
                                        commit=true;
                                        restartInterface=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 't'");
                                    }
                                }
                                if (gps != null){
                                    System.out.println("setClock(): Sending auto-gps (0x22) command");
                                    // t[0x20]HH:MM:SS DD-MM-YYYY  
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(34);  // 0x22
                                    if (gps) ultraOutput.writeByte(1);
                                    else ultraOutput.writeByte(0);
                                    ultraOutput.writeByte(255);
                                    
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                        ultraSettings.put("22",tz.toString());
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 't'");
                                    }
                                }
                                if (commit){
                                    System.out.println("setClock(): Sending auto-gps (0x22) command");
                                    // t[0x20]HH:MM:SS DD-MM-YYYY  
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(255);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {

                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 't'");
                                    }
                                }
                                if (restartInterface){ // This will result in a disconnect
                                    System.out.println("setClock(): Sending reset interface (0x2D) command");
                                    
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(45);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    
                                }
                            } else {
                                // timeout
                                System.out.println("Timeout waiting to send command '?'");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                        } finally {
                            if (aquired) System.out.println("Relasing transmit lock");
                            if (aquired) okToSend.release();
                        }
                    }
                    return null;
                }
        };
        new Thread(ultraCommand).start();
        
    }
    
    @Override
    public Boolean chipIsBib() {
        return Boolean.FALSE; 
    }
    
    private void connect() {
        if (connectToUltra == true) return; // already connected
        connectToUltra = true;
        
        lastRead = -1;
        Task ultraConnection = new Task<Void>() {
                Boolean success = false;
                Boolean firstConnect = true;
                @Override public Void call() {
                    Boolean socketError = false;
                    while(connectToUltra) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Connecting to " + ultraIP + "...");
                        });

                        //connectToUltra = false; // prevent looping if the connect fails
                        try (
                            Socket ultraSocket = new Socket(ultraIP, 23); 
                            InputStream input = ultraSocket.getInputStream();
                            OutputStream rawOutput = ultraSocket.getOutputStream();
                        ) {
                            connectToUltra = true; // we got here so we have a good connection
                            success = true;
                            ultraSocket.setSoTimeout(20000); // 20 seconds. In theory we get a voltage every 10
                            ultraOutput = new DataOutputStream(new BufferedOutputStream(rawOutput));
                            Platform.runLater(() -> {
                                connectedStatus.setValue(true);
                                statusLabel.setText("Connected to " + ultraIP);
                            });
                            int read = -255; 
                            String line = "";
                            while (read != 10) { // 1,Connected,<stuff>\n is sent on initial connect. 10 == \n
                                read = input.read();
                                //System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                            } 
                            
                            if (firstConnect) {
                                onConnectSetup();
                                firstConnect = false;
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
                                        //System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                                    } else {
                                        processLine(line);
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            System.out.println(e);
                            socketError = true;
                            
                            Platform.runLater(() -> {
                                if (! success) { connectToUltra = false; connectedStatus.setValue(false);}
                                statusLabel.setText("Error: " + e.getLocalizedMessage());
                            });
                            if (success) try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
                            } 
                        } finally {
                            if (!socketError){
                                Platform.runLater(() -> {
                                    connectedStatus.setValue(false);
                                    statusLabel.setText("Disconnected");
                                });
                            }
                        }
                    }
                    
                    return null; 
                }
            }; 
            ultraConnectionThread  = new Thread(ultraConnection);
            ultraConnectionThread.setName("Thread-Ultra-" + ultraIP);
            ultraConnectionThread.setDaemon(true);
            ultraConnectionThread.start();
        
    }
    
    private void disconnect(){
        statusLabel.setText("Disconecting from " + ultraIP + "...");
        connectToUltra = false;
    }
    
    

    
    private void processLine(String line) {
        System.out.println("Read Line: " + line);
        
        String type = "unknown";
        if (line.startsWith("0,")) type="chip";
        else if (line.startsWith("1,")) type="chip";
        else if (line.startsWith("V")) type="voltage";
        else if (line.startsWith("S")) type="status";
        else if (line.startsWith("U")) type="command";
        else if (line.startsWith("u")) type="command"; // general command
        else if (line.substring(0,8).matches("^\\d+:\\d+:\\d+.*")) type = "time"; //time ens with a special char
        else System.out.println("Unknown line: \"" + line + "\"");

        switch(type){
            case "chip": // chip time
                processRead(line);
                break;
            case "status": // status 
                System.out.println("Status: " + line);
                commandResultQueue.offer(line);
                break;
            case "voltage": // voltage
                System.out.println("Voltage: " + line);
                getReadStatus();
                break;
            case "time": // command response
                System.out.println("Time: " + line.substring(0,19));
                commandResultQueue.offer(line.substring(0,19));
                break;
            case "command": // command response
                System.out.println("Command response recieved");
                commandResultQueue.offer(line);
                break;
            default: // unknown command response
                System.out.println("Unknown: \"" + line.substring(0, 1) + "\" " + line);
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
        
        if (tokens.length < 12 ) {
            System.out.println("  Chip read is missing data: " + r);
            return;
        }
        
        String chip = tokens[1];
        String port = tokens[4];
        String reader = tokens[7];
        //String antenna = tokens[x];
        //String rewind = tokens[x];
        String rewind = tokens[6];
        String logNo = tokens[11];
        
        if (rewind.equals("0")) {
            int currentRead=Integer.parseInt(logNo);
            if (lastRead + 1 == currentRead || lastRead < 0 ) {
                //System.out.println("No missing reads: Last " + lastRead + " Current: " + logNo);
                lastRead = currentRead;
            }
            else {
                //System.out.println("Missing a read: Last " + lastRead + " Current: " + logNo);
                // auto-rewind
                rewind(lastRead,currentRead);
                lastRead = currentRead;
            }
        }
        
        System.out.println("  Chip: " + chip + " logNo: " + logNo);
        
        // make sure we have what we need...
        if (port.equals("0") && ! chip.equals("0")) { // invalid combo
            System.out.println("Non Start time: " + chip);
            return;
        } else if (!port.matches("[1234]") && !chip.equals("0")){
            System.out.println("Invalid Port: " + port);
            return;
        }
        
        //LocalDate origin = LocalDate.parse("1980-01-01",DateTimeFormatter.ISO_LOCAL_DATE); 
        //LocalDateTime read_ldt = LocalDateTime.of(origin, LocalTime.MIDNIGHT);
        Long seconds = Long.parseLong(tokens[2]);
        Long millis = Long.parseLong(tokens[3]);
        LocalDateTime read_ldt = EPOC.plusSeconds(seconds).plusNanos(millis * 1000000);
        
        LocalDateTime event_ldt = LocalDateTime.of(Event.getInstance().getLocalEventDate(), LocalTime.MIN);
        
        Duration timestamp = Duration.between(event_ldt,read_ldt);
        
        // if it is before the event date, just return
        if (timestamp.isNegative()) {
            String status = "Read Timestamp of " + timestamp + " is before the event date, ignoring";
            Platform.runLater(() -> {
                lastReadLabel.textProperty().setValue(status);
            });
            System.out.println(status);
        } else {
            RawTimeData rawTime = new RawTimeData();
            rawTime.setChip(chip);
            rawTime.setTimestampLong(timestamp.toNanos());
            String status = "Read of chip " + chip + " at " + DurationFormatter.durationToString(timestamp, 3) + " Reader: " + reader + " Port: " + port;
            Platform.runLater(() -> {
                lastReadLabel.textProperty().setValue(status);
            });
            if (statusLabel.getText().equals("Connected: Waiting for a chip read...") ){
                Platform.runLater(() -> {
                    statusLabel.setText("Connected: ");
                });
            }
            timingListener.processRead(rawTime); // process it
            //3,11274,0,"11:22:47.392",1,3
            if (saveToFile) {
                String date = read_ldt.format(DateTimeFormatter.ISO_LOCAL_DATE);
                Duration t = Duration.between(LocalTime.MIDNIGHT, read_ldt.toLocalTime());
                String time = DurationFormatter.durationToString(t, 3);
                if (t.minusHours(10).isNegative()) time = "0" + time; // zero pad the hours
                saveToFile(reader + "," + chip + "," + chip + ",\"" + date + " " + time + "\"," + reader + "," + port);
            }
        }
        
    }
    private void saveToFile(String line){
//        private Boolean saveToFile = false;
//        private String backupFile = null;
//        private PrintWriter outputFile = null;

        if (outputFile == null){
            File newFile = new File(backupFile).getAbsoluteFile();
            System.out.println("PikaRFIDDirectReader::saveToFile: opening " + newFile.getAbsolutePath());
            Boolean goodFile=false;
            try {
                if (newFile.canWrite() || newFile.createNewFile()) {
                    outputFile = new PrintWriter(new FileOutputStream(newFile, true));
                }
            } catch (IOException ex) {
                //Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (outputFile != null) {
            outputFile.println(line);
            if (outputFile.checkError()) System.out.println("PikaRFIDDirectReader::saveToFile: error writing to " + backupFile);
        }
        else System.out.println("PikaRFIDDirectReader::saveToFile: error opening file " + backupFile);


    }
    private void onConnectSetup() {
        // Get the reading status
        getReadStatus();
        
        // set the clock
        
//        Integer offset = TimeZone.getDefault().getOffset(System.currentTimeMillis())/3600000;
//        setClock(LocalDateTime.now(), offset, true);
        CountDownLatch latch = new CountDownLatch(1);
        getSettings(latch);
        clockIssuesCheck(latch);
                
        
}

    private void discover() {
        System.out.println("Starting discover...");
        ObservableList<Ultra> ultras = FXCollections.observableArrayList(); 
        BooleanProperty scanCompleted = new SimpleBooleanProperty(false);
        // start a discovery task in a background thread
        Task ultraSearch = new Task<Void>() {
                @Override public Void call() {
                    try {
                        InetAddress local = Inet4Address.getLocalHost();
                        String[] localIP = local.getHostAddress().split("\\.");
                        
                        String subnet = localIP[0] + "." + localIP[1] + "." + localIP[2];

                        ForkJoinPool forkJoinPool = new ForkJoinPool(256);
                        int timeout=12000;
                        for (int i = 1; i < 255; i++) {
                            String host=subnet + "." + i;
                            int lastOctet = i;
                            forkJoinPool.submit(() -> {
                               
                                //System.out.println("Trying " + host);
                                int tries = 3;
                                try{
                                    while (tries-- >= 0) {
                                        //System.out.println("Trying " + host + "(" + tries + ")");
                                        try{
                                            Socket ultraSocket = new Socket();
                                            ultraSocket.connect(new InetSocketAddress(host,23), 4000);
                                            if (ultraSocket.isConnected()) {
                                                System.out.println("Connected to " + host);
                                                Ultra u = new Ultra(host);
                                                if (!ultras.contains(u)) Platform.runLater(() -> {ultras.add(u);});
                                                // TODO:
                                                // Work with RFIDTiming to get info on the box
                                                // MAC, Type, etc. 
                                                
                                                tries = -1;
                                            } 
                                            ultraSocket.close();
                                        } catch (Exception e){
                                            //System.out.println(e);
                                        }
                                    }
                                } catch (Exception e) {
                                    //System.out.println("Unable to connect to " + host);
                                }
                            });
                         }
                        int increment = 100;
                        for (int i = 0; i< timeout; i+=increment){
                            updateProgress(i,timeout);
                            //System.out.println(i);
                            Thread.sleep(increment);
                        }
                        forkJoinPool.shutdown();
                        forkJoinPool.awaitTermination(1, TimeUnit.SECONDS);
                        System.out.println("Done scanning for Ultras");
                        Platform.runLater(() -> {scanCompleted.set(true);});
                    } catch (Exception ex) {
                        Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    ultras.forEach(u -> {System.out.println("Found " + u.IP.getValueSafe());});
                    return null;
                }
        };
        new Thread(ultraSearch).start();
        
        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(ultraSearch.progressProperty());

        
        ListView<Ultra> ultraListView = new ListView();
        ultraListView.setItems(ultras);
       
        
        // open a dialog
        Dialog<Ultra> dialog = new Dialog();
        dialog.resizableProperty().set(true);
        dialog.getDialogPane().setMaxHeight(Screen.getPrimary().getVisualBounds().getHeight()-150);
        dialog.setTitle("Discover Available Ultras");
        dialog.setHeaderText("Discover Available Ultras");
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);
        
        // Create a scrollPane to put the tables and such in
        VBox mainVBox = new VBox();
        mainVBox.setStyle("-fx-font-size: 16px;"); // Make the scroll bar a bit larger
        VBox progressVBox = new VBox();
        progressVBox.setAlignment(Pos.CENTER);
        progressVBox.getChildren().add(new Label("Searching for Ultras..."));
        progressVBox.visibleProperty().bind(scanCompleted.not());
        progressVBox.managedProperty().bind(scanCompleted.not());
        progressVBox.getChildren().add(progress);
        Label foundCount = new Label();
        foundCount.textProperty().bind(Bindings.concat("Found ", Bindings.size(ultras).asString()));
        
        progressVBox.getChildren().add(foundCount);

        progress.setMaxWidth(500);
       
        progressVBox.setPrefHeight(175);

        VBox ultraListVBox = new VBox();
        ultraListVBox.setStyle("-fx-font-size: 16px;"); // Make everything normal again
        ultraListVBox.fillWidthProperty().set(true);
        ultraListVBox.setAlignment(Pos.CENTER_LEFT);
        Label selectLabel = new Label("Select an Ultra...");
        selectLabel.visibleProperty().bind(Bindings.size(ultras).isNotEqualTo(0));
        selectLabel.managedProperty().bind(Bindings.size(ultras).isNotEqualTo(0));
        ultraListVBox.getChildren().add(selectLabel);
        ultraListVBox.getChildren().add(ultraListView);
        
        Label notFound = new Label("No Ultras were found!.\nCheck network settings\nand try again.");
        notFound.visibleProperty().bind(Bindings.size(ultras).isEqualTo(0));
        notFound.managedProperty().bind(Bindings.size(ultras).isEqualTo(0));
        ultraListView.visibleProperty().bind(Bindings.size(ultras).greaterThanOrEqualTo(1));
        ultraListView.managedProperty().bind(Bindings.size(ultras).greaterThanOrEqualTo(1));
        
        ultraListVBox.setPrefHeight(1750);

        ultraListVBox.getChildren().add(notFound);
        ultraListVBox.visibleProperty().bind(scanCompleted);
        ultraListVBox.managedProperty().bind(scanCompleted);
        mainVBox.getChildren().add(progressVBox);
        mainVBox.getChildren().add(ultraListVBox);
        dialog.getDialogPane().setContent(mainVBox);
        
        // If they double click on an ultra, select it and close the dialog box
        ultraListView.setOnMouseClicked((MouseEvent click) -> {
            if (click.getClickCount() == 2) {
                dialog.setResult(ultraListView.getSelectionModel().getSelectedItem());
            }
        });
        
        dialog.getDialogPane().getScene().getWindow().sizeToScene();
        
        Node createButton = dialog.getDialogPane().lookupButton(selectButtonType);
        createButton.disableProperty().bind(ultraListView.getSelectionModel().selectedItemProperty().isNull());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return ultraListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        Optional<Ultra> result = dialog.showAndWait();

        
        if (result.isPresent()) {
            ultraIP = result.get().IP.getValueSafe();
            ultraIPTextField.setText(ultraIP);
            timingListener.setAttribute("RFIDDirect:ultra_ip", ultraIP);
            connectToggleSwitch.selectedProperty().set(true);
        }
        
    }

    private void rewind(){
        // open a dialog box 
        Dialog<RewindData> dialog = new Dialog();
        dialog.setTitle("Rewind");
        dialog.setHeaderText("Rewind timing data...");
        ButtonType rewindButtonType = new ButtonType("Rewind", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rewindButtonType, ButtonType.CANCEL);
        
        VBox rewindVBox = new VBox();
        rewindVBox.setStyle("-fx-font-size: 16px;");
        
        // start date / time
        HBox startHBox = new HBox();
        startHBox.setSpacing(5.0);
        Label startLabel = new Label("From:");
        startLabel.setMinWidth(40);
        DatePicker startDate = new DatePicker();
        TextField startTime = new TextField();
        startHBox.getChildren().addAll(startLabel,startDate,startTime);
        
        // end date / time
        HBox endHBox = new HBox();
        endHBox.setSpacing(5.0);
        Label endLabel = new Label("To:");
        endLabel.setMinWidth(40);
        DatePicker endDate = new DatePicker();
        TextField endTime = new TextField();
        endHBox.getChildren().addAll(endLabel,endDate,endTime);
        
        rewindVBox.getChildren().addAll(startHBox,endHBox);
        dialog.getDialogPane().setContent(rewindVBox);

        
        BooleanProperty startTimeOK = new SimpleBooleanProperty(false);
        BooleanProperty endTimeOK = new SimpleBooleanProperty(false);
        BooleanProperty allOK = new SimpleBooleanProperty(false);
       
        allOK.bind(Bindings.and(endTimeOK, startTimeOK));
        
        
        startTime.textProperty().addListener((observable, oldValue, newValue) -> {
            startTimeOK.setValue(false);
            if (DurationParser.parsable(newValue)) startTimeOK.setValue(Boolean.TRUE);
            if ( newValue.isEmpty() || newValue.matches("^[0-9]*(:?([0-5]?([0-9]?(:([0-5]?([0-9]?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good start Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = startTime.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    startTime.setText(oldValue);
                    startTime.positionCaret(c);
                });
                System.out.println("Bad start time (newValue: " + newValue + ")");
            }
        });
        endTime.textProperty().addListener((observable, oldValue, newValue) -> {
            endTimeOK.setValue(false);
            if (DurationParser.parsable(newValue)) endTimeOK.setValue(Boolean.TRUE);
            if ( newValue.isEmpty() || newValue.matches("^[0-9]*(:?([0-5]?([0-9]?(:([0-5]?([0-9]?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good start Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = endTime.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    endTime.setText(oldValue);
                    endTime.positionCaret(c);
                });
                System.out.println("Bad end time (newValue: " + newValue + ")");
            }
        });
        
        //Default to event date / 00:00 for the start time, event date 23:59:00 for the end time
        startDate.setValue(Event.getInstance().getLocalEventDate());
        startTime.setText("00:00:00");
        endDate.setValue(Event.getInstance().getLocalEventDate());
        endTime.setText("23:59:59");
        
        
        Node createButton = dialog.getDialogPane().lookupButton(rewindButtonType);
        createButton.disableProperty().bind(allOK.not());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == rewindButtonType) {
                RewindData result = new RewindData();
                result.startDate = startDate.getValue();
                result.startTime = DurationParser.parse(startTime.getText());
                result.endDate = endDate.getValue();
                result.endTime = DurationParser.parse(endTime.getText());
                return result;
            }
            return null;
        });

        Optional<RewindData> result = dialog.showAndWait();

        
        if (result.isPresent()) {
            RewindData rwd= result.get();
            // convert the date/time to seconds since 1/1/1980
            
            Long startTimestamp = Duration.between(EPOC, LocalDateTime.of(rwd.startDate, LocalTime.ofSecondOfDay(rwd.startTime.getSeconds()))).getSeconds();
            Long endTimestamp = Duration.between(EPOC, LocalDateTime.of(rwd.endDate, LocalTime.ofSecondOfDay(rwd.endTime.getSeconds()))).getSeconds();
            
            System.out.println("Rewind from " + startTimestamp + " to " + endTimestamp);
            // issue the rewind command via a background thread
            
            Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired=true;
                                String status = "Rewind from " + 
                                        rwd.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + startTime.getText() + " to " +
                                        rwd.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + endTime.getText();
                                
                                System.out.println(status);
                                Platform.runLater(() -> {
                                    statusLabel.setText(status);
                                });
                                ultraOutput.flush();
                                
                                String command = "800";
                                command += startTimestamp.toString() ;
                                command += Character.toString ((char) 13) ;
                                command += endTimestamp.toString();
                                command += Character.toString ((char) 13) ;

                                ultraOutput.writeBytes(command);
                                ultraOutput.flush();

                            } else {
                                // timeout
                                System.out.println("Timeout with AutoRewind command");
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
        
        
    }
    
    //Auto-Rewind
    private void rewind(Integer lastRead, Integer currentRead) {
           Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired=true;
                                System.out.println("AutoRewind from " + lastRead + " to " + currentRead);

                                Platform.runLater(() -> {
                                    statusLabel.setText("AutoRewind from " + lastRead + " to " + currentRead);
                                });
                                
                                ultraOutput.flush();
                                
                                String command = "600";
                                command += lastRead.toString() ;
                                command += Character.toString ((char) 13) ;
                                command += currentRead.toString();
                                command += Character.toString ((char) 13) ;

                                ultraOutput.writeBytes(command);
                                ultraOutput.flush();

                            } else {
                                // timeout
                                System.out.println("Timeout with AutoRewind command");
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
    private void setClockDialog(){
        Integer localTZ = TimeZone.getDefault().getOffset(System.currentTimeMillis())/3600000;
        Integer ultraTZ = Integer.parseInt(ultraSettings.get("23"));

        // open a dialog box 
        Dialog<Boolean> dialog = new Dialog();
        dialog.setTitle("Set Ultra Clock");
        dialog.setHeaderText("Set the clock for " + ultraIP);
        ButtonType setButtonType = new ButtonType("Set", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(setButtonType, ButtonType.CANCEL);
        
        VBox clockVBox = new VBox();
        clockVBox.setStyle("-fx-font-size: 16px;");
        
        CheckBox useComputer = new CheckBox("Sync with the local computer");
        VBox manualVBox = new VBox();
        manualVBox.setSpacing(5.0);
        manualVBox.disableProperty().bind(useComputer.selectedProperty());
        
        HBox dateHBox = new HBox();
        dateHBox.setSpacing(5.0);
        Label dateLabel = new Label("Date:");
        dateLabel.setMinWidth(40);
        DatePicker ultraDate = new DatePicker();
        dateHBox.getChildren().addAll(dateLabel,ultraDate);
        
        HBox timeHBox = new HBox();
        timeHBox.setSpacing(5.0);
        Label timeLabel = new Label("Time:");
        timeLabel.setMinWidth(40);
        TextField ultraTime = new TextField();
        timeHBox.getChildren().addAll(timeLabel,ultraTime);
        
        HBox tzHBox = new HBox();
        tzHBox.setSpacing(5.0);
        Label tzLabel = new Label("TimeZone:");
        tzLabel.setMinWidth(40);
        Spinner<Integer> tzSpinner = new Spinner<>(-23, 23, localTZ);    
        tzHBox.getChildren().addAll(tzLabel,tzSpinner);

        manualVBox.getChildren().addAll(dateHBox,timeHBox,tzHBox);
        
        CheckBox autoGPS = new CheckBox("Use GPS to auto-set the clock");
        autoGPS.setSelected(true);

        
        clockVBox.getChildren().addAll(useComputer,manualVBox,autoGPS);
        dialog.getDialogPane().setContent(clockVBox);
        
        BooleanProperty timeOK = new SimpleBooleanProperty(false);

        ultraTime.textProperty().addListener((observable, oldValue, newValue) -> {
            timeOK.setValue(false);
            if (DurationParser.parsable(newValue)) timeOK.setValue(Boolean.TRUE);
            if ( newValue.isEmpty() || newValue.matches("^[0-9]*(:?([0-5]?([0-9]?(:([0-5]?([0-9]?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good start Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = ultraTime.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    ultraTime.setText(oldValue);
                    ultraTime.positionCaret(c);
                });
                System.out.println("Bad clock time (newValue: " + newValue + ")");
            }
        });
        
        
        ultraDate.setValue(LocalDate.now());
        ultraTime.setText(LocalTime.ofSecondOfDay(LocalTime.now().toSecondOfDay()).toString());

        Node createButton = dialog.getDialogPane().lookupButton(setButtonType);
        createButton.disableProperty().bind(timeOK.not());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == setButtonType) {
                return Boolean.TRUE;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (useComputer.selectedProperty().get()) {
                System.out.println("Timezone check: Local :" + localTZ + " ultra: " + ultraTZ);
                if (localTZ.equals(ultraTZ)) setClock(LocalDateTime.now(),null,autoGPS.selectedProperty().get());
                else setClock(LocalDateTime.now(),localTZ,autoGPS.selectedProperty().get());
            } else {
                LocalTime time = LocalTime.MIDNIGHT.plusSeconds(DurationParser.parse(ultraTime.getText()).getSeconds());
                Integer newTZ = tzSpinner.getValue();
                if (newTZ.equals(ultraTZ)) setClock(LocalDateTime.of(ultraDate.getValue(), time),null,autoGPS.selectedProperty().get());
                else {
                    setClock(LocalDateTime.of(ultraDate.getValue(), time),newTZ,autoGPS.selectedProperty().get());
                }
            }
            
        }
    }
    
    private void clockIssuesCheck(CountDownLatch latch){
        Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                       
                        try {
                            if (latch != null) latch.await();
                            // time check
                            Boolean timeOK=true;
                            Boolean tzOK=true;
                            // TZ check
                            Integer localTZ = TimeZone.getDefault().getOffset(System.currentTimeMillis())/3600000;
                            Integer ultraTZ = Integer.parseInt(ultraSettings.get("23"));
                            System.out.println("Timezone check: Local :" + localTZ + " ultra: " + ultraTZ);
                            String issues = "";
                            if (!localTZ.equals(ultraTZ))  {
                                timeOK=false;
                                tzOK=false; // flip this out so that the setClock below won't adjust the TZ
                                            // adjusting the TZ requires a network reader interface reset. 
                                issues = "Timezone mismatch: Local: " + localTZ + " ultra: " + ultraTZ +"\n";
                            }
                            
                            if (!LocalDate.now().equals(ultraClock.date)) {
                                timeOK=false;
                                issues += "Clock Date Mismatch: Local: "+ LocalDate.now() + " ultra: " + ultraClock.date+"\n";
                            }
                            System.out.println("Date check: Local :" + LocalDate.now() + " ultra: " + ultraClock.date);

                            if (Duration.between(ultraClock.time, ultraClock.takenAt).abs().getSeconds() > 60) {
                                timeOK=false;
                                issues += "Clock Time Mismatch: Local: "+ ultraClock.takenAt + " ultra: " + ultraClock.time+"\n";
                            }
                             System.out.println("Time check: Local :" + ultraClock.takenAt + " ultra: " + ultraClock.time);
                             
                            if (!timeOK) {
                                
                                System.out.println("Time issues!!!");
                                
                                if (readingStatus.get()) {
                                    issues += "\nThese cannot be fixed when the Ultra is in 'Read' mode. \n" +
                                            "Either stop the reader and use the \"Sync Clock\" option "
                                            + "under the \"Advanced Settings\" area to fix the clock " +
                                            " or use the skew option on the reader input to adjust the time. "; 
                                    String timeIssues = issues;
                                    Platform.runLater(() -> {
                                        Alert alert = new Alert(AlertType.WARNING);
                                        alert.setTitle("Ultra Clock Issues");
                                        alert.setHeaderText("Issues detected with the clock...");
                                        alert.setContentText(timeIssues);
                                        alert.showAndWait();
                                    });
                                } else {
                                    issues += "\n Do you want PikaTimer to fix these for you?";
                                    String timeIssues = issues;
                                    Boolean goodTZ = tzOK;
                                    Platform.runLater(() -> {
                                        Dialog<Boolean> dialog = new Dialog();
                                        dialog.setTitle("Ultra Clock Issues");
                                        dialog.setHeaderText("Issues detected with the clock...");
                                        dialog.setContentText(timeIssues);
                                        ButtonType fixButtonType = new ButtonType("Fix", ButtonBar.ButtonData.OK_DONE);
                                        dialog.getDialogPane().getButtonTypes().addAll(fixButtonType, ButtonType.CANCEL);

                                        dialog.setResultConverter(dialogButton -> {
                                            if (dialogButton == fixButtonType) {
                                                return Boolean.TRUE;
                                            }
                                            return null;
                                        });
                                        Optional<Boolean> result = dialog.showAndWait();

                                        if (result.isPresent()) {
                                            if (goodTZ) setClock(LocalDateTime.now(),null,true);
                                            else setClock(LocalDateTime.now(),localTZ,true);
                                        }
                                    });
                                }
                            }
                            else System.out.println("Time loogs good");
                            // now let's populate the settings box
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
    
        public void updateReaderSettings(){
        Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired = true;
                                Boolean commit=false;
                                Boolean restartInterface=false;
                                
                                // Mode
                                String mode = reader1ModeChoiceBox.getSelectionModel().getSelectedItem();
                                if (mode != null){
                                    System.out.println("updateReaderSettings(): Sending reader mode (0x14/0x15) command");
                                    byte val = 0;
                                    if (mode.equals("Start")) val = 0;
                                    if (mode.equals("Finish")) val = 3;
                                           
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(20);  // 0x14, Reader 1 mode
                                    ultraOutput.writeByte(val);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        if (mode.equals("Start")) ultraSettings.put("14", "0");
                                        else ultraSettings.put("14", "3");
                                        commit=true;
                                        restartInterface=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 'u0x20'");
                                    }
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(21);  // 0x15, Reader 2 mode
                                    ultraOutput.writeByte(val);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    //result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        if (mode.equals("Start")) ultraSettings.put("15", "0");
                                        else ultraSettings.put("15", "3");
                                        commit=true;
                                        restartInterface=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 'u0x21'");
                                    }
                                }
                                
                                Integer gf = gatingIntervalSpinner.getValue();
                                if (gf != null && "Finish".equals(mode)){
                                    System.out.println("updateReaderSettings(): Sending gating interval (0x1E) command");
                                    
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(30);  // 0x1e, Gating Interval
                                    ultraOutput.writeBytes(gf.toString());
                                    ultraOutput.writeByte(255);

                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        ultraSettings.put("30",gf.toString());
                                        commit=true;
                                        restartInterface=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 'u0x1E'");
                                    }
                                } else if ("Start".equals(mode)){
                                    ultraSettings.put("30","1");
                                    Platform.runLater(() -> {gatingIntervalSpinner.getValueFactory().setValue(1);});
                                }
                                
                                if (commit){
                                    System.out.println("updateReaderSettings(): Sending auto-gps (0x22) command");
                                    // t[0x20]HH:MM:SS DD-MM-YYYY  
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(255);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {

                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 'u0xFF'");
                                    }
                                }
                                if (restartInterface){ // This will result in a disconnect
                                    System.out.println("updateReaderSettings(): Sending reset interface (0x2D) command");
                                    
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(45);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    
                                }
                            } else {
                                // timeout
                                System.out.println("Timeout waiting to update the reader settings");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);

                        } finally {
                            if (aquired) System.out.println("Relasing transmit lock");
                            if (aquired) okToSend.release();
                        }
                    }
                    
                    
                    return null;
                }
        };
        new Thread(ultraCommand).start();
        updateSettingsButton.visibleProperty().set(false);
    }

    private static class Ultra {

        @Override
        public int hashCode() {
            int hash = 7 + IP.hashCode() + MAC.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Ultra other = (Ultra) obj;
            if (!this.IP.getValueSafe().equals(other.IP.getValueSafe()) ) {
                return false;
            }
            if (!this.MAC.getValueSafe().equals(other.MAC.getValueSafe()) ) {
                return false;
            }
            return true;
        }
        public StringProperty IP  = new SimpleStringProperty();
        public StringProperty MAC  = new SimpleStringProperty();
        public Ultra() {
            
        }
        public Ultra(String host){
            IP.set(host);
        }
        public StringProperty ipProperty() {
            return IP;
        }
        public StringProperty macProperty(){
            return MAC;
        }
        public String toString(){
            return IP.getValueSafe();
        }
    }

    private static class RewindData {
        public LocalDate startDate;
        public LocalDate endDate;
        public Duration startTime;
        public Duration endTime;

        public RewindData() {
        }
    }

    private static class UltraClock {
        public LocalDate date;
        public LocalTime time;
        public Duration tzOffset;
        public Boolean tzHalfOffsetSupport;
        public LocalTime takenAt;
        public UltraClock() {
        }
    }
}
