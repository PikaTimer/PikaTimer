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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
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
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import org.controlsfx.control.PrefixSelectionComboBox;
import org.controlsfx.control.ToggleSwitch;

/**
 *
 * @author jcgarner
 */


/**
 * 
 * This class implements the communications to and from the RFID Ultra/Joey
 * 
 * It is a minefield of threads as the system has to mux/demux the requests 
 * and responses to and from the unit over a single TCP connection. The system
 * uses the "okToSend" semaphore to ensure that only one request is 
 * outstanding at any given time. 
 * 
 * Everything should be done in an async fashion to keep the UI responsive. 
 * Given that everything is contained in this one class, it was decided 
 * to not use a message bus library. 
 * 
 * Proceed with caution. 
 * 
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
    private Button exportButton;
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
    ChoiceBox<String> beeperVolumeChoiceBox = new ChoiceBox(FXCollections.observableArrayList("Off", "Soft", "Loud"));
    
    Button setClockButton = new Button("Sync Time...");
    Button remoteSettingsButton = new Button("Remote Server...");
    Button antennaSettingsButton = new Button("Antenna Options...");
    Label modeLabel = new Label("Reader Mode:");
    Label gatingLabel = new Label("Gating Interval:");
    Label volumeLabel = new Label("Beeper Volume:");
    Button updateSettingsButton = new Button("Update");
            
    private Map<String,String> ultraSettings = new HashMap();
    private UltraClock ultraClock = new UltraClock();

    
    protected final BooleanProperty readingStatus = new SimpleBooleanProperty();
    protected final BooleanProperty connectedStatus = new SimpleBooleanProperty();
    protected final BooleanProperty isJoey = new SimpleBooleanProperty(false);
    protected final BooleanProperty isSingleReader = new SimpleBooleanProperty(true);

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
        if (saveToFile != null) {
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
            lastReadLabel.setPrefWidth(250);
            discoverButton = new Button("Discover...");
            rewindButton = new Button("Rewind...");
            exportButton = new Button("Export...");
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
            fileTextField.setPrefWidth(150);
            fileHBox.getChildren().addAll(saveToFileCheckBox,fileTextField,fileButton);
            
            VBox advancedVBox = new VBox();
            advancedVBox.setSpacing(4);
            advancedVBox.setAlignment(Pos.CENTER_LEFT);
            
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
            
            modeLabel.managedProperty().bind(isJoey.not());
            modeLabel.visibleProperty().bind(isJoey.not());
            reader1ModeChoiceBox.managedProperty().bind(isJoey.not());
            reader1ModeChoiceBox.visibleProperty().bind(isJoey.not());
            
            beeperVolumeChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("beeperVolumeChoiceBox listener: Existing volume: " + ultraSettings.get("21"));
                if (null != newVal) switch (newVal) {
                    case "Off":
                        if (!"0".equals(ultraSettings.get("21"))) updateSettingsButton.setVisible(true);
                        break;
                    case "Soft":
                        if (!"1".equals(ultraSettings.get("21"))) updateSettingsButton.setVisible(true);
                        break;
                    case "Loud":
                        if (!"2".equals(ultraSettings.get("21"))) updateSettingsButton.setVisible(true);
                        break;
                    default:
                        break;
                }
                
            });
            
            updateSettingsButton.setVisible(false);
            updateSettingsButton.setOnAction(action -> {
                updateReaderSettings();
            });
            
            HBox advancedHBox1 = new HBox();
            advancedHBox1.setSpacing(4);
            advancedHBox1.setAlignment(Pos.CENTER_LEFT);
            HBox advancedHBox2 = new HBox();
            advancedHBox2.setSpacing(4);
            advancedHBox2.setAlignment(Pos.CENTER_LEFT);
            HBox advancedHBox3 = new HBox();
            advancedHBox3.setSpacing(4);
            advancedHBox3.setAlignment(Pos.CENTER_LEFT);
            
            advancedHBox1.getChildren().addAll(volumeLabel,beeperVolumeChoiceBox,modeLabel,reader1ModeChoiceBox,gatingLabel,gatingIntervalSpinner);
            advancedHBox2.getChildren().addAll(setClockButton,remoteSettingsButton,antennaSettingsButton);
            advancedHBox3.getChildren().addAll(updateSettingsButton);
            advancedVBox.getChildren().addAll(advancedHBox1,advancedHBox2,advancedHBox3);
                        
            advancedVBox.disableProperty().bind(connectedStatus.and(readingStatus).or(connectedStatus.not()));
            
            settingsVBox.getChildren().addAll(fileHBox,advancedVBox);
            
            Label strut = new Label();
            strut.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(strut, Priority.ALWAYS);
            
            Label strut2 = new Label();
            strut2.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(strut2, Priority.ALWAYS);
            
            settingsTitlePane.setText("Advanced Settings");
            settingsTitlePane.setContent(settingsVBox);
            settingsTitlePane.setExpanded(false);
            
            
            exportButton.setMinWidth(75);
            exportButton.setMaxWidth(75);
            HBox statusHBox = new HBox();
            statusHBox.getChildren().addAll(statusLabel,lastReadLabel);
            statusHBox.setSpacing(5);
            
            HBox batteryHBox = new HBox();
            Label batterySpacer = new Label();
            batterySpacer.setMaxWidth(100);
            batterySpacer.setPrefWidth(100);
            batteryProgressBar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(batteryProgressBar, Priority.ALWAYS);
            batteryHBox.getChildren().addAll(new Label("Battery: "),batteryProgressBar);
            batteryHBox.visibleProperty().bind(connectToggleSwitch.selectedProperty());
            batteryProgressBar.setPrefHeight(20.0);
            VBox statusVBox = new VBox();
            statusVBox.getChildren().addAll(statusHBox,batteryHBox);
            HBox.setHgrow(statusVBox, Priority.ALWAYS);
            HBox statusHBox2 = new HBox();
            statusHBox2.setSpacing(10);
            statusHBox2.getChildren().addAll(statusVBox,exportButton);
            
            rewindButton.setMinWidth(75);
            rewindButton.setMaxWidth(75);

            displayHBox.setSpacing(5);
            displayHBox.setAlignment(Pos.CENTER_LEFT);
            displayHBox.getChildren().addAll(ipLabel,ultraIPTextField, discoverButton, switchHBox,strut, rewindButton);
            
            displayVBox.setAlignment(Pos.CENTER_LEFT);
            displayVBox.getChildren().addAll(displayHBox,statusHBox2,settingsTitlePane); 
            
            // Set the action for the discoverButton
            discoverButton.setOnAction((event) -> {
                discover();
            });
            discoverButton.disableProperty().bind(connectedStatus);
            
            rewindButton.setOnAction((event) -> {
                rewind();
            });
            rewindButton.disableProperty().bind(connectedStatus.not());
            exportButton.setOnAction((event) -> {
                //export();
            });
            setClockButton.setOnAction((event) -> {
                setClockDialog();
            });
            remoteSettingsButton.setOnAction((event) -> {
                remoteDialog();
            });
            antennaSettingsButton.setOnAction((event) -> {
                antennaDialog();
            });
            
            
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
                    outputFile = null; // clear the reference to the old file
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
                            saveToFileCheckBox.setSelected(true);
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
                    saveToFileCheckBox.setSelected(true);
                    outputFile=null;
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
                                
                                // give the readers 5 seconds to start before we check
                                // Joey's take about 10 seconds to startup
                                if(isJoey.getValue()) Thread.sleep(12000); else Thread.sleep(5000); 
                                //getReadStatus(); 
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
                                //Thread.sleep(5000); // give the readers 5 seconds to stop before we check
                                //getReadStatus();
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

//    private void getReadStatus(){
//        Task ultraCommand = new Task<Void>() {
//                @Override public Void call() {
//                    if (connectedStatus.get()) {
//                        Boolean aquired = false;
//                        try {
//                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
//                                aquired = true;
//                                System.out.println("getReadStatus(): Sending ? command");
//                                ultraOutput.writeBytes("?");
//                                //ultraOutput.writeUTF("?");
//                                ultraOutput.flush();
//                                String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
//                                if (result != null) {
//                                    System.out.println("Reading Status : " + result);
//                                    Boolean currentStatus = readingStatus.getValue();
//                                    Boolean newStatus = false;
//                                    if (result.substring(2, 3).startsWith("1")) newStatus = true;
//                                    else newStatus = false;
//                                    if (!Objects.equals(newStatus, currentStatus)){
//                                        externalInitiated = true;
//                                        Platform.runLater(() -> {
//                                            if (result.substring(2, 3).startsWith("1")) readingStatus.setValue(Boolean.TRUE);
//                                            else readingStatus.setValue(Boolean.FALSE);
//                                        });
//                                    }
//                                    if (statusLabel.getText().equals("Starting Readers...") && newStatus) {
//                                        Platform.runLater(() -> {
//                                            statusLabel.setText("Connected: Waiting for a chip read...");
//                                        });
//                                    } else if (statusLabel.getText().equals("Connected: Readers stopped") && newStatus){
//                                        Platform.runLater(() -> {
//                                            statusLabel.setText("Connected: Waiting for a chip read...");
//                                        });
//                                    } else if (!newStatus){
//                                        Platform.runLater(() -> {
//                                            statusLabel.setText("Connected: Readers stopped");
//                                        });
//                                    }
//                                } else {
//                                // timeout
//                                    System.out.println("Timeout with command '?'");
//                                }
//                            } else {
//                                // timeout
//                                System.out.println("Timeout waiting to send command '?'");
//                            }
//                        } catch (Exception ex) {
//                            Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
//
//                        } finally {
//                            if (aquired) System.out.println("Relasing transmit lock");
//                            if (aquired) okToSend.release();
//                        }
//                    }
//                    return null;
//                }
//        };
//        new Thread(ultraCommand).start();
//    }
        
    @Override
    public BooleanProperty getReadingStatus() {
        return readingStatus; 
    }

    public void getSettings(CountDownLatch latch){
        
        Task ultraCommand = new Task<Void>() {
            @Override public Void call() {
                if (connectedStatus.get()) {
                    
                    Platform.runLater(() -> updateSettingsButton.setVisible(false));
                    
                    Boolean aquired = false;
                    try {
                        if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                            aquired = true;
                            ultraSettings.clear();

                            String command = "r";
                            ultraOutput.writeBytes(command);

                            ultraOutput.flush();
                            String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                            if (result != null) {
                                // result is HH:MM:SS DD-MM-YYYY 
                                // and is NOT zero padded 
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
                                System.out.println("Timeout with command 'r'");
                                return null;
                            }

                            

                            ultraOutput.flush();

                            ultraOutput.writeBytes("U");

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
                            
                            
                            //Beeper Volume Factor
                            try{
                                if (!ultraSettings.containsKey("21")) System.out.println("We don't know what the beeper volume is");
                                else {
                                    Integer volume =Integer.parseInt(ultraSettings.get("21"));
                                    Platform.runLater(() -> beeperVolumeChoiceBox.getSelectionModel().clearAndSelect(volume));
                                    System.out.println("Setting the volume to " + volume);
                                }
                            } catch (Exception e){
                                Platform.runLater(() -> beeperVolumeChoiceBox.getSelectionModel().selectFirst());
                                System.out.println("Beeper volume parse error!");
                            }
                            
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
                                
                                if ("0".equals(ultraSettings.get("14"))) Platform.runLater(() -> reader1ModeChoiceBox.getSelectionModel().select("Start"));
                                else Platform.runLater(() -> reader1ModeChoiceBox.getSelectionModel().select("Finish"));
                            } catch (Exception e){
                                Platform.runLater(() -> reader1ModeChoiceBox.getSelectionModel().selectFirst());
                            }
                            
                            // Do we have a Joey or Ultra?
                            if ("255.255.255.255".equals(ultraSettings.get("1A"))) {
                                System.out.println("We have a Joey");
                                isSingleReader.set(true);
                                Platform.runLater(() -> isJoey.set(true));
                            } else {
                                Platform.runLater(() -> isJoey.set(false));
                                System.out.println("We have an Ultra");
                                // do we have an ultra-4 or ultra-8? 
                                // ping the ip in 1A to see if it exists. 
                                isSingleReader.set(!InetAddress.getByName(ultraSettings.get("1B")).isReachable(1000));
                                // if we failed, try one more time just in case we lost a packet
                                if (!isSingleReader.get()) isSingleReader.set(!InetAddress.getByName(ultraSettings.get("1B")).isReachable(1000));
                                if (!isSingleReader.get()) System.out.println("We have an ultra 8");
                                else System.out.println("We have an ultra 4");
                            }
                        } else {
                            // timeout
                            System.out.println("Timeout waiting to get Ultra Settings");
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
                                } else {
                                    System.out.println("NULL time in setClock");
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
                                        System.out.println("Timeout with command 'u0x23' timezone string");
                                    }
                                } else {
                                    System.out.println("NULL TZ in setClock()");
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
                                        ultraSettings.put("22","1");
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x22' to set the gps flag");
                                    }
                                }
                                if (commit){
                                    System.out.println("setClock(): Sending commit (u 0xFF 0xFF) command");
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
                    Boolean readRetry = false;
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
                            
                            // Set the timeout to 20 seconds
                            // In theory, we see a voltate every 2
                            // However the system pauses for 10+ when reading is enabled
                            ultraSocket.setSoTimeout(20000); 
                            ultraOutput = new DataOutputStream(new BufferedOutputStream(rawOutput));
                            Platform.runLater(() -> {
                                connectedStatus.setValue(true);
                                statusLabel.setText("Connected to " + ultraIP);
                            });
                            int read = -255; 
                            String line = "";
                            while (read != 10) { // 1,Connected,<stuff>\n is sent on initial connect. 10 == \n
                                read = input.read();
                                System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                            } 
                            System.out.println("Read connect string");
                            
                            
                            
                        
                            if (firstConnect) {
                                onConnectSetup();
                                firstConnect = false;
                                // Enable the extended status messages
                                // 03 00 00 03 0d 0a
                                String command = Character.toString ((char) 3) ;
                                command += Character.toString ((char) 0) ;
                                command += Character.toString ((char) 0) ;
                                command += Character.toString ((char) 3) ;
                                command += Character.toString ((char) 13) ;
                                command += Character.toString ((char) 10) ;
                                ultraOutput.writeBytes(command); 
                                ultraOutput.flush();
                            }
                            
                            while(connectToUltra) {
                                read = -255; 
                                line = "";
                                try {
                                    while (read != 10 && connectToUltra) {
                                        read = input.read();
                                        readRetry = false;
                                        if (read == -1) {
                                            connectToUltra = false;
                                            System.out.println("End of stream!" + Integer.toHexString(read));
                                        } if (read != 10) {
                                            line = line +  Character.toString ((char) read);
                                            //logger.trace("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));

                                            // Look for the extended status messages
                                            // The 2nd char has the length. So we can use that to consume
                                            // the entire output since they may contain the 0x10 end of line in them
                                            // Warning, the data is in host order (little endian) 
                                            // and not network byte order (big endian). 
                                            if (line.equals(Character.toString ((char) 3))) {
                                                // Lenght includes the 0x03 and lenght chars, so N-2 are left to read
                                                int length = input.read() - 2; 
                                                //logger.trace("EX Length: " + Integer.toHexString(0x100 | length).substring(1));
                                                while (length-- > 0){
                                                    read = input.read();
                                                    //logger.trace("EX: " + Integer.toHexString(0x100 | read).substring(1));
                                                    line = line + Character.toString ((char) read);
                                                }
                                                processLine(line);
                                            }
                                        //    System.out.println("Read: " + Character.toString ((char) read) + "  " + Integer.toHexString(0x100 | read).substring(1));
                                        } else {
                                            processLine(line);
                                        }
                                    }
                                } catch(java.net.SocketTimeoutException e){
                                    System.out.println("Socket Timeout Exception...");
                                    if (readRetry) {
                                        System.out.println("...2nd One in a row so we will bail");
                                        throw e;
                                    } else {
                                        System.out.println("...First one so let's ask for status");
                                        readRetry=true;
                                        //getReadStatus();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                            if (connectToUltra){ 
                                socketError = true;
                                System.out.println("RFIDDirectReader Connection Exception: " + e.getMessage());
                                Platform.runLater(() -> {
                                    if (! success) { connectToUltra = false; connectedStatus.setValue(false);}
                                    statusLabel.setText("Error: " + e.getLocalizedMessage());
                                });
                                if (success) try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(PikaRFIDDirectReader.class.getName()).log(Level.SEVERE, null, ex);
                                } 
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
        //System.out.println("Read Line: " + line);
        
        String type = "unknown";
        if (line.startsWith("0,")) type="chip";
        else if (line.startsWith("1,")) type="chip";
        else if (line.startsWith("V")) type="voltage";
        else if (line.startsWith("S")) type="status";
        else if (line.startsWith("U")) type="command";
        else if (line.startsWith("u")) type="command"; // general command
        else if (line.startsWith(Character.toString ((char) 3))) type ="extStatus";
        else if (line.substring(0,8).matches("^\\d+:\\d+:\\d+.*")) type = "time"; //time ends with a special char
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
                //getReadStatus();
                break;
            case "time": // command response
                System.out.println("Time: " + line.substring(0,19));
                commandResultQueue.offer(line.substring(0,19));
                break;
            case "command": // command response
                System.out.println("Command response recieved");
                commandResultQueue.offer(line);
                break;
            case "extStatus":
                System.out.println(" Extended status received: " + stringToHex(line));
                processExtStatus(line.toCharArray());
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
                // Not a directory and we can write to it _or_ we can create it
                if ((!newFile.isDirectory() && newFile.isFile() && newFile.canWrite())  || newFile.createNewFile()) {
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
         // Get the settings and check for any clock issues
         
        CountDownLatch latch = new CountDownLatch(1);
        getSettings(latch);
        clockIssuesCheck(latch);
                
        
}

    private void discover() {
        System.out.println("Starting discover...");
        ObservableList<Ultra> ultras = FXCollections.observableArrayList(); 
        BooleanProperty scanCompleted = new SimpleBooleanProperty(false);
        BooleanProperty dialogClosed = new SimpleBooleanProperty(false);
        // start a discovery task in a background thread
        Task ultraSearch = new Task<Void>() {
                @Override public Void call() {
                    
                    // This is ugly but it works
                    byte one = new Integer(1).byteValue();
                    byte zero = new Integer(0).byteValue();
                    byte[] packetData = {one,zero,zero,zero,zero,zero,zero,zero};
                    
                    // Find the server using UDP broadcast
                    //Loop while the dialog box is open
                    // UDP Broadcast code borrowed from https://demey.io/network-discovery-using-udp-broadcast/
                    // with a few modifications to protect the guilty and to bring it up to date
                    // (e.g., try-with-resources 
                    while (dialogClosed.not().get()) {
                        try(DatagramSocket broadcastSocket = new DatagramSocket()) {
                            broadcastSocket.setBroadcast(true);
                            // 2 second timeout for responses
                            broadcastSocket.setSoTimeout(2000);
                            
                            // Send a packet to 255.255.255.255 on port 2000
                            DatagramPacket probeDatagramPacket = new DatagramPacket(packetData, packetData.length, InetAddress.getByName("255.255.255.255"), 2000);
                            broadcastSocket.send(probeDatagramPacket);
                            
                            System.out.println("Sent UDP Broadcast to 255.255.255.255");
                            // Broadcast the message over all the network interfaces
                            
                            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                            while (interfaces.hasMoreElements()) {
                              NetworkInterface networkInterface = interfaces.nextElement();

                              if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                                continue; // Don't want to broadcast to the loopback interface
                              }

                              for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                                InetAddress broadcast = interfaceAddress.getBroadcast();
                                if (broadcast == null) {
                                  continue;
                                }
                                // Send the broadcast package!
                                try {
                                  DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, broadcast, 8888);
                                  broadcastSocket.send(sendPacket);
                                  System.out.println("Sent UDP Broadcast to " + broadcast.getHostAddress());
                                } catch (Exception e) {
                                }
                              }
                            }


                            //Wait for a response
                            try {
                                while (true) { // the socket timeout should stop this
                                    byte[] recvBuf = new byte[1500]; // mass overkill
                                    DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                                    broadcastSocket.receive(receivePacket);
                                    
                                    
                                    String message = new String(receivePacket.getData()).trim();
                                    
                                    System.out.println("Ultra Finder Response: " + receivePacket.getAddress().getHostAddress() + " " + message);
                                    
                                    // parse the response string
                                    
                                    // Set the Ultra's IP
                                    Ultra u = new Ultra(receivePacket.getAddress().getHostAddress().toString());
                                    
                                    // Now the Type
                                    
                                    if (message.matches("(?i:.*JOEY.*)")) u.TYPE.set("Joey");
                                    if (message.matches("(?i:.*Ultra.*)")) u.TYPE.set("Ultra");
                                    
                                    // Finally the Rabbit MAC
                                    // dump the first 3 octets and save the last 3
                                    u.MAC.set(message.substring(message.indexOf(":")+7).toUpperCase( ));
                                    
                                    // If we have a new Ultra, save it. 
                                    if (!u.TYPE.getValueSafe().isEmpty() && !ultras.contains(u)) Platform.runLater(() -> {ultras.add(u);});
                                }
                            }catch (Exception ex){
                            }
                                                 
                        } catch (IOException ex) {
                          //Logger.getLogger(this.class.getName()).log(Level.SEVERE, null, ex);
                          System.out.println("oops...");
                        }
                    }
                    System.out.println("Done scanning for Ultras");
                    //Platform.runLater(() -> {scanCompleted.set(true);});
                                       

                    ultras.forEach(u -> {System.out.println("Found " + u.IP.getValueSafe());});
                    return null;
                }
        };
        Thread scanner = new Thread(ultraSearch);
        scanner.setDaemon(true);
        scanner.setName("Ultra Scanner");
        scanner.start();
        
        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(ultraSearch.progressProperty());

        
        ListView<Ultra> ultraListView = new ListView();
        ultraListView.setItems(ultras);
       
        
        // open a dialog
        Dialog<Ultra> dialog = new Dialog();
        dialog.resizableProperty().set(true);
        dialog.getDialogPane().setMaxHeight(Screen.getPrimary().getVisualBounds().getHeight()-150);
        dialog.setTitle("Discover...");
        dialog.setHeaderText("Discover Local RFIDTiming Systems");
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);
        
        // Create a scrollPane to put the tables and such in
        VBox mainVBox = new VBox();
        mainVBox.setPrefWidth(250);
        mainVBox.setStyle("-fx-font-size: 16px;"); // Make the scroll bar a bit larger
        VBox progressVBox = new VBox();
        progressVBox.setAlignment(Pos.CENTER);
        progressVBox.getChildren().add(new Label("Searching for Units..."));
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
       
        Label selectLabel = new Label("Select an RFID source:");
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
        
        scanCompleted.bind(Bindings.size(ultras).greaterThanOrEqualTo(1));
        
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
        dialogClosed.set(true);

        
        if (result.isPresent()) {
            ultraIP = result.get().IP.getValueSafe();
            ultraIPTextField.setText(ultraIP);
            timingListener.setAttribute("RFIDDirect:ultra_ip", ultraIP);
            connectToggleSwitch.selectedProperty().set(true);
        }
        
    }

//    private void export(){
//        // open a dialog
//        // promt for an export file
//        // output the raw data from the parent TimingLocation
//        // Use the RFIDFile format for output
//        
//        File outputFile =   ;
//        
//        if (outputFile != null) {
//            timingListener.getReads().;
//        }
//               
//        
//    }
    
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
                                
                                // Send 8[0x00][0x00], like RFIDServer, not "800" per the manual
                                String command = "8";
                                command += Character.toString ((char) 0) ;
                                command += Character.toString ((char) 0) ;
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
                                
                                // Send 6[0x00][0x00], like RFIDServer, not "800" per the manual
                                String command = "6";
                                command += Character.toString ((char) 0) ;
                                command += Character.toString ((char) 0) ;
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
    
    private void antennaDialog(){
        // Open a dialog box
        Dialog<Boolean> dialog = new Dialog();
        dialog.setTitle("Antenna settings");
        String type = "";
        if (isJoey.get()) type = "Joey";
        else if (isSingleReader.get()) type = "Ultra-4";
        else type = "Ultra-8";
        
        dialog.setHeaderText("Antenna and Power settings for " + type + " at "+ ultraIP);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        //VBox dialogVBox = new VBox();
        GridPane antennaGridPane = new GridPane();
        Map<String,CheckBox> antennaMap = new HashMap();
        
        antennaGridPane.setHgap(1);
        antennaGridPane.setVgap(4);
        
        Label portsLabel = new Label("Ports");
        antennaGridPane.add(portsLabel,1,0,4,1);
        GridPane.setHalignment(portsLabel, HPos.CENTER);
        
        // Power Option of 1 -> 32 (inclusive)
        PrefixSelectionComboBox<String> readerAPower = new PrefixSelectionComboBox();
        PrefixSelectionComboBox<String> readerBPower = new PrefixSelectionComboBox();
        for(Integer p = 32; p>0; p--){
            readerAPower.getItems().add(p.toString());
            readerBPower.getItems().add(p.toString());
        }
        
        // Reader 'A'
        for(int port = 1; port < 5; port++){
            antennaMap.put("A" + port,new CheckBox());
            System.out.println("Requesting ultraSetting " + String.format("%02x", 11 + port).toUpperCase());
            if (ultraSettings.containsKey(String.format("%02x", 11 + port).toUpperCase())) {
                if ("1".equals(ultraSettings.get(String.format("%02x", 11 + port).toUpperCase()))) antennaMap.get("A" + port).setSelected(true);
                else antennaMap.get("A" + port).setSelected(false);
            } else antennaMap.get("A" + port).setSelected(false);
            antennaGridPane.add(new Label(Integer.toString(port)),port,1);
            antennaGridPane.add(antennaMap.get("A" + port),port,2);
        } 
        if (!isJoey.get()) {
            antennaGridPane.add(new Label("Power"), 5,1);
            antennaGridPane.add(readerAPower, 5,2);
            if (ultraSettings.containsKey("18")){
                System.out.println("Reader A power set to " + ultraSettings.get("18"));
                readerAPower.getSelectionModel().select(ultraSettings.get("18"));
            } else readerAPower.getSelectionModel().selectFirst();
            
            antennaMap.put("ABackup",new CheckBox());
            if (ultraSettings.containsKey("26")) {
                if ("1".equals(ultraSettings.get("26"))) antennaMap.get("ABackup").setSelected(true);
                else antennaMap.get("ABackup").setSelected(false);
            } else antennaMap.get("ABackup").setSelected(false);
            antennaGridPane.add(new Label("  "),6,1);
            Label antBackupLlabel = new Label("Antenna 4\nis backup");
            antennaGridPane.add(antBackupLlabel,7,0,1,2);
            GridPane.setValignment(antBackupLlabel, VPos.BOTTOM);
            antennaGridPane.add(antennaMap.get("ABackup"),7,2);
        }
        
        // Reader 'B' but only if we have one...
        if (!isSingleReader.get()){
            Label readerALabel = new Label("Reader A ");
            antennaGridPane.add(readerALabel, 0, 2);
            Label readerBLabel = new Label("Reader B ");
            antennaGridPane.add(readerBLabel, 0, 3);
            for(int port = 1; port < 5; port++){
                antennaMap.put("B" + port,new CheckBox());
                System.out.println("Requesting ultraSetting " + Integer.toHexString(15 + port));
                if (ultraSettings.containsKey(Integer.toHexString(15 + port))) {
                    if ("1".equals(ultraSettings.get(Integer.toHexString(15 + port)))) antennaMap.get("B" + port).setSelected(true);
                    else antennaMap.get("B" + port).setSelected(false);
                } else antennaMap.get("B" + port).setSelected(false);
                antennaGridPane.add(antennaMap.get("B" + port),port,3);
            } 
            
            antennaGridPane.add(readerBPower, 5,3);
            if (ultraSettings.containsKey("19")){
                readerBPower.getSelectionModel().select(ultraSettings.get("19"));
                System.out.println("Reader B power set to " + ultraSettings.get("19"));
            } else readerBPower.getSelectionModel().selectFirst();
            
            antennaMap.put("BBackup",new CheckBox());
            if (ultraSettings.containsKey("27")) {
                if ("1".equals(ultraSettings.get("27"))) antennaMap.get("BBackup").setSelected(true);
                else antennaMap.get("BBackup").setSelected(false);
            } else antennaMap.get("BBackup").setSelected(false);
            antennaGridPane.add(antennaMap.get("BBackup"),7,3);
        }
        
        dialog.getDialogPane().setContent(antennaGridPane);
        
        // if good, save the settings
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return Boolean.TRUE;
            }
            return null;
        });
        
        
        Optional result = dialog.showAndWait();
        
        if (result.isPresent()) {
            
            // If an Ultra-8
            if (!isSingleReader.get()) {
                // Reader B, 1 -> 4
                // Power Setting
                // Backup setting
            }
            
            // Play the task game
            Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired = true;
                                Boolean commit=false;
                                Boolean restartInterface=false;
                                
                                
                                // 0x0C -> 0x0F Reader A, Antenna 1 -4
                                // 0x10 -> 0x13 Reader B, Antenna 1 -> 4
                                // 0x18 -> Reader A Power
                                // 0x26 -> Reader A Ant 4 as Backup (Ultra Only)
                                
                                // Reader A, 1 -> 4
                                for(int port = 1; port < 5; port++){
                                    String p = String.format("%02x", 11 + port).toUpperCase();
                                    if (!ultraSettings.containsKey(p) || 
                                        (antennaMap.get("A" + port).selectedProperty().get() && "0".equals(ultraSettings.get(p))) ||
                                        (!antennaMap.get("A" + port).selectedProperty().get() && "1".equals(ultraSettings.get(p)))){
                                        System.out.println("AntennaDialog(): Setting " + p + " to " + antennaMap.get("A" + port).selectedProperty().get());
                                        ultraOutput.flush();
                                        ultraOutput.writeBytes("u");
                                        ultraOutput.writeByte(11 + port);  // 0x0C -> 0x0F
                                        if (antennaMap.get("A" + port).selectedProperty().get()) ultraOutput.writeByte(1);
                                        else ultraOutput.writeByte(0);
                                        ultraOutput.writeByte(255);
                                        ultraOutput.flush();
                                        String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                        if (result != null) {
                                            commit=true;
                                        } else {
                                        // timeout
                                            System.out.println("Timeout with command '" + p + "' to enable/disable the antenna");
                                        }
                                    }
                                } 
                                if (!isJoey.get()) {
                                    // Power Setting
                                    if (!ultraSettings.containsKey("18") || 
                                            !ultraSettings.get("18").equals(readerAPower.getSelectionModel().getSelectedItem()) ){
                                        System.out.println("AntennaDialog(): Sending Reader A power (0x18) command to " + readerAPower.getSelectionModel().getSelectedItem());
                                        ultraOutput.flush();
                                        ultraOutput.writeBytes("u");
                                        ultraOutput.writeByte(24);  // 0x18
                                        ultraOutput.writeBytes(readerAPower.getSelectionModel().getSelectedItem());
                                        ultraOutput.writeByte(255);
                                        ultraOutput.flush();
                                        String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                        if (result != null) {
                                            commit=true;
                                        } else {
                                        // timeout
                                            System.out.println("Timeout with command '0x18' to set the Reader A port 4 backupflag");
                                        }
                                    }

                                    // If an Ultra backup setting
                                    if (!ultraSettings.containsKey("26") || 
                                            (antennaMap.get("ABackup").selectedProperty().get() && "0".equals(ultraSettings.get("26"))) ||
                                            (!antennaMap.get("ABackup").selectedProperty().get() && "1".equals(ultraSettings.get("26")))
                                        ){
                                        System.out.println("remoteDialog(): Sending Reader A port 4 backup (0x26) command");
                                        ultraOutput.flush();
                                        ultraOutput.writeBytes("u");
                                        ultraOutput.writeByte(38);  // 0x26
                                        if (antennaMap.get("ABackup").selectedProperty().get()) ultraOutput.writeByte(1);
                                        else ultraOutput.writeByte(0);
                                        ultraOutput.writeByte(255);
                                        ultraOutput.flush();
                                        String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                        if (result != null) {
                                            commit=true;
                                        } else {
                                        // timeout
                                            System.out.println("Timeout with command '0x18' to set the Reader A Power Command");
                                        }
                                    }
                                }
            
                                if (!isSingleReader.get()){
                                    
                                    // 0x10 -> 0x13 Reader B, Antenna 1 -> 4
                                    // 0x19 -> Reader B Power
                                    // 0x27 -> Reader B Ant 4 as Backup (Ultra Only)

                                    // Reader A, 1 -> 4
                                    for(int port = 1; port < 5; port++){
                                        String p = String.format("%02x", 15 + port).toUpperCase();
                                        if (!ultraSettings.containsKey(p) || 
                                            (antennaMap.get("B" + port).selectedProperty().get() && "0".equals(ultraSettings.get(p))) ||
                                            (!antennaMap.get("B" + port).selectedProperty().get() && "1".equals(ultraSettings.get(p)))){
                                            System.out.println("AntennaDialog(): Setting " + p + " to " + antennaMap.get("B" + port).selectedProperty().get());
                                            ultraOutput.flush();
                                            ultraOutput.writeBytes("u");
                                            ultraOutput.writeByte(15 + port);  // 0x0C -> 0x0F
                                            if (antennaMap.get("B" + port).selectedProperty().get()) ultraOutput.writeByte(1);
                                            else ultraOutput.writeByte(0);
                                            ultraOutput.writeByte(255);
                                            ultraOutput.flush();
                                            String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                            if (result != null) {
                                                commit=true;
                                            } else {
                                            // timeout
                                                System.out.println("Timeout with command '" + p + "' to enable/disable the antenna");
                                            }
                                        }

                                    } 
                                    if (!ultraSettings.containsKey("19") || 
                                        !ultraSettings.get("19").equals(readerBPower.getSelectionModel().getSelectedItem()) ){
                                        System.out.println("remoteDialog(): Sending Reader B power (0x19) command to " + readerBPower.getSelectionModel().getSelectedItem());
                                        ultraOutput.flush();
                                        ultraOutput.writeBytes("u");
                                        ultraOutput.writeByte(25);  // 0x19
                                        ultraOutput.writeBytes(readerBPower.getSelectionModel().getSelectedItem());
                                        ultraOutput.writeByte(255);
                                        ultraOutput.flush();
                                        String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                        if (result != null) {
                                            commit=true;
                                        } else {
                                        // timeout
                                            System.out.println("Timeout with command '0x19' to set the Reader B Power Command");
                                        }
                                    }

                                    // If an Ultra backup setting
                                    if (!ultraSettings.containsKey("27") || 
                                            (antennaMap.get("BBackup").selectedProperty().get() && "0".equals(ultraSettings.get("27"))) ||
                                            (!antennaMap.get("BBackup").selectedProperty().get() && "1".equals(ultraSettings.get("27")))
                                        ){
                                        System.out.println("remoteDialog(): Sending Reader B port 4 backup (0x27) command");
                                        ultraOutput.flush();
                                        ultraOutput.writeBytes("u");
                                        ultraOutput.writeByte(39);  // 0x27
                                        if (antennaMap.get("BBackup").selectedProperty().get()) ultraOutput.writeByte(1);
                                        else ultraOutput.writeByte(0);
                                        ultraOutput.writeByte(255);
                                        ultraOutput.flush();
                                        String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                        if (result != null) {
                                            commit=true;

                                        } else {
                                        // timeout
                                            System.out.println("Timeout with command '0x27' to set the Reader B port 4 backup flag");
                                        }
                                    }
                                }
                                
                                if (commit){
                                    System.out.println("remoteDialog(): Sending commit (u 0xFF 0xFF) command");
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(255);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        restartInterface = true;

                                        // Reader A
                                        for(int port = 1; port < 5; port++){
                                            String p = String.format("%02x", 11 + port).toUpperCase();
                                            if (antennaMap.get("A" + port).selectedProperty().get()) ultraSettings.put(p,"1");
                                            else ultraSettings.put(p,"0");
                                        } 
                                        if (!isJoey.get()){
                                            ultraSettings.put("18",readerAPower.getSelectionModel().getSelectedItem());
                                            if (antennaMap.get("ABackup").selectedProperty().get()) ultraSettings.put("26","1");
                                            else ultraSettings.put("26","0");
                                        }
                                        if (!isSingleReader.get()){
                                            // Reader B
                                            for(int port = 1; port < 5; port++){
                                                String p = String.format("%02x", 15 + port).toUpperCase();
                                                if (antennaMap.get("B" + port).selectedProperty().get()) ultraSettings.put(p,"1");
                                                else ultraSettings.put(p,"0");
                                            } 
                                                ultraSettings.put("19",readerBPower.getSelectionModel().getSelectedItem());
                                                if (antennaMap.get("BBackup").selectedProperty().get()) ultraSettings.put("27","1");
                                                else ultraSettings.put("27","0");
                                        }
                                        
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 'u[0xFF][0xFF]'");
                                    }
                                }
                                if (restartInterface){ // This will result in a disconnect
                                    System.out.println("Sending reset interface (0x2D) command");
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
        
        
    }
    private void remoteDialog(){
        // Settings 
        // 0x01:Remote Type (0 = off, 1 = gprs, 2 = lan)
        // 0x02: IP of remote server for GPRS. Send in hex? See 0x29
        // 0x03: Port for remote server
        // 0x04: APN name
        // 0x05: APPN User
        // 0x06: APN password
        // 0x29: URL for http uploading (er, IP)
        //       173.192.106.122 for USA1.RFIDTiming.com
        //       82.113.145.195 for EUROPE1.RFIDTiming.com
        // 0x2A: Gateway for LAN
        // 0x2B: DNS Server for (0x29) or blank if IP
        // 0x2C: ??? -- looks like our non-dhcp IP for joey's 
        // 0x2E: Enable / Disable sending to remote: 0 or 1 
        
        // Yeah, yeah, we should return an optional with an object that has the
        // settings in it. Or a boolean and then we can just yank them from 
        // the nodes and call it a good day. 
        
        // Open a dialog box
        Dialog<Boolean> dialog = new Dialog();
        dialog.setTitle("Remote settings");
        dialog.setHeaderText("Configure the remote settings for " + ultraIP);
        ButtonType setButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(setButtonType, ButtonType.CANCEL);
        Node saveButton = dialog.getDialogPane().lookupButton(setButtonType);
        
        ToggleSwitch enableRemoteToggleSwitch = new ToggleSwitch("Send data to remote Server");
        if (ultraSettings.containsKey("2E") && "1".equals(ultraSettings.get("2E"))) enableRemoteToggleSwitch.setSelected(true);
        else enableRemoteToggleSwitch.setSelected(false);
        
        // set the gprsChoiceBox
        // The options are in a specific order to reflect what the Ultra/Joey wants (0 = off, 1 = gprs, 2 = lan)
        Label gprsLabel = new Label("Connection Type");
        ChoiceBox<String> gprsChoiceBox = new ChoiceBox(FXCollections.observableArrayList("Off", "Internal Modem", "LAN"));
        HBox typeHBox = new HBox();
        typeHBox.setSpacing(4);
        typeHBox.getChildren().setAll(gprsLabel,gprsChoiceBox);
        
        
        // Port
        Label portLabel = new Label("Remote Server Port");
        TextField portTextField = new TextField();
        portTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null || newValue.isEmpty()) return;
            if (!newValue.matches("\\d+")) {
                    Platform.runLater(() -> { 
                    int c = portTextField.getCaretPosition();
                    portTextField.setText(oldValue);
                    portTextField.positionCaret(c);
                }); 
            }
        });
        
        if (ultraSettings.containsKey("03")) portTextField.setText(ultraSettings.get("03"));
        else portTextField.setText("11111");
        HBox portHBox = new HBox();
        portHBox.setSpacing(4);
        portHBox.getChildren().setAll(portLabel,portTextField);
        
        //Server
        String customIP = "";
        Label serverLabel = new Label("Remote Server");
        ChoiceBox<String> serverChoiceBox = new ChoiceBox(FXCollections.observableArrayList("USA1.RFIDTiming.com", "EUROPE1.RFIDTiming.com", "Custom"));
        Label customServerLabel = new Label("Remote Server IP");
        TextField customServerTextField = new TextField();
        VBox serverVBox = new VBox();
        serverVBox.setSpacing(4);
        HBox serverHBox = new HBox();
        serverHBox.setSpacing(4);
        
        serverHBox.getChildren().setAll(serverLabel,serverChoiceBox);
        
        HBox customServerHBox = new HBox();
        customServerHBox.setSpacing(4);
        customServerHBox.getChildren().setAll(customServerLabel,customServerTextField);
        serverVBox.getChildren().setAll(serverHBox,customServerHBox);
        
        serverChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, oldType, newType) -> {
            if ("Custom".equals(newType)){
                customServerHBox.setVisible(true);
                customServerHBox.setManaged(true);
                saveButton.disableProperty().set(true);
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            } else {
                customServerHBox.setVisible(false);
                customServerHBox.setManaged(false);
                saveButton.disableProperty().set(false);
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
        
        String currentIP ="";
        //       173.192.106.122 for USA1.RFIDTiming.com
        //       82.113.145.195 for EUROPE1.RFIDTiming.com
        try {
            Integer remoteInt = Integer.parseInt(ultraSettings.get("01"));
            
            if (remoteInt == 1 && ultraSettings.containsKey("02")) currentIP = ultraSettings.get("02");
            if (remoteInt == 2 && ultraSettings.containsKey("29")) currentIP = ultraSettings.get("29");
            else currentIP = "173.192.106.122";
            
        } catch (Exception e){
            serverChoiceBox.getSelectionModel().selectFirst();
            currentIP = "173.192.106.122";
        }
        System.out.println("Current IP: \"" + currentIP + "\"");
        if ("173.192.106.122".equals(currentIP)) serverChoiceBox.getSelectionModel().select(0);
        else if ("82.113.145.195".equals(currentIP)) serverChoiceBox.getSelectionModel().select(1);
        else {
            serverChoiceBox.getSelectionModel().selectLast();
            customServerTextField.setText(currentIP);
        }
        
        
        // This is way more complicated than it should be...
        customServerTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            Boolean revert = false;
            if (!"Custom".equals(serverChoiceBox.getSelectionModel().getSelectedItem())) {
                saveButton.disableProperty().set(false);
                return;
            }
            if (newValue.isEmpty()) saveButton.disableProperty().set(true);
            
            if (newValue.matches("[\\d\\.]+")) { // numbers and dots only for the inital pass
                String octets[] = newValue.split("\\.");
                if (octets.length != 4) saveButton.disableProperty().set(true);
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
                        System.out.println("Valid IP : " + customIP);
                        saveButton.disableProperty().set(false);
                    }
                    else{
                        saveButton.disableProperty().set(true);
                    }
                }
            } else { //just say no
                revert = true;
            }
            if (revert) {
                    Platform.runLater(() -> { 
                    int c = customServerTextField.getCaretPosition();
                    customServerTextField.setText(oldValue);
                    customServerTextField.positionCaret(c);
                }); 
            }
        });
        
        // LAN
        Label gatewayLabel = new Label("Gateway");
        TextField gatewayTextField = new TextField();
        if (ultraSettings.containsKey("2A")) gatewayTextField.setText(ultraSettings.get("2A"));

        HBox gatewayHBox = new HBox();
        gatewayHBox.setSpacing(4);
        gatewayHBox.getChildren().setAll(gatewayLabel,gatewayTextField);
        
        // 3G modem
        Label apnNameLabel = new Label("APN Name");
        TextField apnNameTextField = new TextField();
        if (ultraSettings.containsKey("04")) apnNameTextField.setText(ultraSettings.get("04"));
        HBox apnNameHBox = new HBox();
        apnNameHBox.setSpacing(4);
        apnNameHBox.getChildren().setAll(apnNameLabel,apnNameTextField);
        
        Label apnUserNameLabel = new Label("APN Username");
        TextField apnUserNameTextField = new TextField();
        if (ultraSettings.containsKey("05")) apnUserNameTextField.setText(ultraSettings.get("05"));
        HBox apnUserNameHBox = new HBox();
        apnUserNameHBox.setSpacing(4);
        apnUserNameHBox.getChildren().setAll(apnUserNameLabel,apnUserNameTextField);
        
        Label apnPasswordLabel = new Label("APN Password");
        TextField apnPasswordTextField = new TextField();
        if (ultraSettings.containsKey("06")) apnPasswordTextField.setText(ultraSettings.get("06"));
        HBox apnPasswordHBox = new HBox();
        apnPasswordHBox.setSpacing(4);
        apnPasswordHBox.getChildren().setAll(apnPasswordLabel,apnPasswordTextField);
        
        VBox apnVBox = new VBox();
        apnVBox.setSpacing(4);
        apnVBox.getChildren().setAll(apnNameHBox,apnUserNameHBox,apnPasswordHBox);
        
        VBox configVBox = new VBox();
        configVBox.setSpacing(4);
        configVBox.getChildren().setAll(portHBox,serverVBox,gatewayHBox,apnVBox);
        
        gprsChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, oldType, newType) -> {
            if (null != newType) //("Off", "Internal Modem", "LAN")
            switch (newType) {
                case "Off":
                    // Hide the config options VBox
                    configVBox.setVisible(false);
                    configVBox.setManaged(false);
                    // resize the dialog box
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                    break;
                case "Internal Modem":
                    // Show the config options VBox
                    configVBox.setVisible(true);
                    configVBox.setManaged(true);
                    // Show APN related stuff
                    apnVBox.setVisible(true);
                    apnVBox.setManaged(true);
                    // hide gateway
                    gatewayHBox.setVisible(false);
                    gatewayHBox.setManaged(false);
                    // resize the dialog box
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                    break;
                case "LAN":
                    // Show the config options VBox
                    configVBox.setVisible(true);
                    configVBox.setManaged(true);
                    // show gateway
                    gatewayHBox.setVisible(true);
                    gatewayHBox.setManaged(true);
                    // Hide APN related stuff
                    apnVBox.setVisible(false);
                    apnVBox.setManaged(false);
                    
                    // resize the dialog box
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                    break;
                default:
                    gprsChoiceBox.getSelectionModel().selectFirst();
            }
        
        });
        
        
        try {
            gprsChoiceBox.getSelectionModel().select(Integer.parseInt(ultraSettings.get("01")));
        } catch (Exception e){
            gprsChoiceBox.getSelectionModel().selectFirst();
        }
        
        VBox dialogVBox = new VBox();
        dialogVBox.setSpacing(4);
        dialogVBox.getChildren().setAll(enableRemoteToggleSwitch,typeHBox,configVBox);
        
        dialog.getDialogPane().setContent(dialogVBox);
        
        // if good, save the settings
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == setButtonType) {
                return Boolean.TRUE;
            }
            return null;
        });
        
        double labelWidth = 110;
        portLabel.setPrefWidth(labelWidth);
        gprsLabel.setPrefWidth(labelWidth);
        serverLabel.setPrefWidth(labelWidth);
        customServerLabel.setPrefWidth(labelWidth);
        gatewayLabel.setPrefWidth(labelWidth);
        apnNameLabel.setPrefWidth(labelWidth);
        apnUserNameLabel.setPrefWidth(labelWidth);
        apnPasswordLabel.setPrefWidth(labelWidth);

        Optional<Boolean> result = dialog.showAndWait();

        if (result.isPresent()) {
            // Play the task game
            Task ultraCommand = new Task<Void>() {
                @Override public Void call() {
                    if (connectedStatus.get()) {
                        Boolean aquired = false;
                        try {
                            if (okToSend.tryAcquire(10, TimeUnit.SECONDS)){
                                aquired = true;
                                Boolean commit=false;
                                Boolean restartInterface=false;
                                
                                
                                // 0x29: URL for http uploading (er, IP)
                                //       173.192.106.122 for USA1.RFIDTiming.com
                                //       82.113.145.195 for EUROPE1.RFIDTiming.com
                                // 0x2A: Gateway for LAN
                                // 0x2B: DNS Server for (0x29) or blank if IP
                                
                                
                                // 0x2E: Enable / Disable sending to remote: 0 or 1 
                                if (!ultraSettings.containsKey("2E") || 
                                        (enableRemoteToggleSwitch.selectedProperty().get() && "0".equals(ultraSettings.get("2E"))) ||
                                        (!enableRemoteToggleSwitch.selectedProperty().get() && "1".equals(ultraSettings.get("2E")))
                                    ){
                                    System.out.println("remoteDialog(): Sending enable/disable (0x2E) command");
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(46);  // 0x2E
                                    if (enableRemoteToggleSwitch.selectedProperty().get()) ultraOutput.writeByte(1);
                                    else ultraOutput.writeByte(0);
                                    ultraOutput.writeByte(255);
                                    
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                        
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x2E' to set the send to remote flag");
                                    }
                                }
                                
                                // 0x01:Remote Type (0 = off, 1 = gprs, 2 = lan)
                                if (!ultraSettings.containsKey("01") || 
                                        !Integer.toString(gprsChoiceBox.getSelectionModel().getSelectedIndex()).equals(ultraSettings.get("01"))
                                    ){
                                    System.out.println("remoteDialog(): Sending Remote Type (0x01) command to " + Integer.toString(gprsChoiceBox.getSelectionModel().getSelectedIndex()));
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(1);  // 0x01
                                    ultraOutput.writeByte(gprsChoiceBox.getSelectionModel().getSelectedIndex());
                                    ultraOutput.writeByte(255);
                                    
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                        
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x01' to set the remote type flag");
                                    }
                                }
                                
                                // 0x03: Port for remote server
                                if (!ultraSettings.containsKey("03") || 
                                        !portTextField.getText().equals(ultraSettings.get("03"))
                                    ){
                                    System.out.println("remoteDialog(): Sending Remote Port (0x03) command to " + portTextField.getText());
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(3);  // 0x03
                                    ultraOutput.writeBytes(portTextField.getText());
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x03' to set the remote type flag");
                                    }
                                }

                                // 0x04: APN name
                                if (!ultraSettings.containsKey("04") || 
                                        !apnNameTextField.getText().equals(ultraSettings.get("04"))
                                    ){
                                    System.out.println("remoteDialog(): Sending Remote Port (0x04) command to " + apnNameTextField.getText());
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(4);  // 0x03
                                    ultraOutput.writeBytes(apnNameTextField.getText());
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x04' to set the rapn name");
                                    }
                                }
                                
                                // 0x05: APPN user
                                if (!ultraSettings.containsKey("05") || 
                                        !apnUserNameTextField.getText().equals(ultraSettings.get("05"))
                                    ){
                                    System.out.println("remoteDialog(): Sending apn UserName (0x05) command to " + apnUserNameTextField.getText());
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(5);  // 0x03
                                    ultraOutput.writeBytes(apnUserNameTextField.getText());
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x05' to set the rapn name");
                                    }
                                }
                                // 0x06: APN password
                                if (!ultraSettings.containsKey("06") || 
                                        !apnPasswordTextField.getText().equals(ultraSettings.get("06"))
                                    ){
                                    System.out.println("remoteDialog(): Sending apnPassword (0x06) command to " + apnPasswordTextField.getText());
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(6);  // 0x03
                                    ultraOutput.writeBytes(apnPasswordTextField.getText());
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x06' to set the rapn name");
                                    }
                                }
                                
                                // 0x02: IP of remote server for GPRS. Send in hex? See 0x29
                                // 0x29: URL for http uploading (er, IP)
                                //       173.192.106.122 for USA1.RFIDTiming.com
                                //       82.113.145.195 for EUROPE1.RFIDTiming.com
                                // We set both 0x02 and 0x29 to the same effective value
                                // to prevent really odd things
                                
                                String newIP = "";
                                switch (serverChoiceBox.getSelectionModel().getSelectedIndex()) {
                                    case 0:
                                        newIP="173.192.106.122";
                                        break;
                                    case 1:
                                        newIP="82.113.145.195";
                                        break;
                                    default:
                                        newIP=customServerTextField.getText();
                                        break;
                                }
                                // 0x02: URL for http uploading (er, IP)
                                if ((!ultraSettings.containsKey("02") || 
                                        !newIP.equals(ultraSettings.get("02")) ) && 
                                        newIP.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")
                                    ){
                                    System.out.println("remoteDialog(): Sending Remote IP (0x02) command to " + newIP);
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(2);  // 0x02
                                    String[] ipBytes = newIP.split("\\.");
                                    for(String o: ipBytes){
                                        ultraOutput.writeByte(Integer.parseUnsignedInt(o));
                                    }
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x02' to set the remote server");
                                    }
                                }
                                // 0x29: URL for http uploading (er, IP)
                                if (!ultraSettings.containsKey("29") || 
                                        !newIP.equals(ultraSettings.get("29"))
                                    ){
                                    System.out.println("remoteDialog(): Sending Remote IP (0x29) command to " + newIP);
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(41);  // 0x29
                                    ultraOutput.writeBytes(newIP);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x29' to set the rapn name");
                                    }
                                }
                                        
                                // 0x2A: Gateway for LAN
                                if ((!ultraSettings.containsKey("2A") || 
                                        !gatewayTextField.getText().equals(ultraSettings.get("2A")) ) && 
                                        gatewayTextField.getText().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")
                                    ){
                                    System.out.println("remoteDialog(): Sending Remote IP (0x2A) command to " + gatewayTextField.getText());
                                    ultraOutput.flush();
                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(2);  // 0x02
                                    String[] ipBytes = gatewayTextField.getText().split("\\.");
                                    for(String o: ipBytes){
                                        ultraOutput.writeByte(Integer.parseUnsignedInt(o));
                                    }
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command '0x02' to set the remote server");
                                    }
                                }
                                if (commit){
                                    System.out.println("remoteDialog(): Sending commit (u 0xFF 0xFF) command");
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(255);
                                    ultraOutput.writeByte(255);
                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        // 0x2E: Enable / Disable sending to remote: 0 or 1 
                                        if (enableRemoteToggleSwitch.selectedProperty().get()) ultraSettings.put("2E","1");
                                        else ultraSettings.put("2E","0");
                                        
                                        // 0x01:Remote Type (0 = off, 1 = gprs, 2 = lan)
                                        ultraSettings.put("01",Integer.toString(gprsChoiceBox.getSelectionModel().getSelectedIndex()));
                                        
                                        // 0x03: Port for remote server
                                        ultraSettings.put("03",portTextField.getText());
                                        
                                        // 0x04: APN name
                                        ultraSettings.put("04",apnNameTextField.getText());
                                        // 0x05: APPN user
                                        ultraSettings.put("05",apnUserNameTextField.getText());
                                        // 0x06: APN password
                                        ultraSettings.put("06",apnPasswordTextField.getText());
                                        
                                        // 0x29: URL for http uploading (er, IP)
                                        if (newIP.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) ultraSettings.put("29",newIP);
                                        // 0x02: IP of remote server for GPRS. Send in hex? See 0x29
                                        if (newIP.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) ultraSettings.put("02",newIP);
                                        
                                        // 0x2A: Gateway for LAN
                                        if (gatewayTextField.getText().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) ultraSettings.put("2A",gatewayTextField.getText());

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

                            if (Duration.between(ultraClock.time, ultraClock.takenAt).abs().getSeconds() > 5) {
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
                                        ButtonType fixButtonType = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
                                        ButtonType cancelButtonType = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
                                        dialog.getDialogPane().getButtonTypes().addAll(fixButtonType, cancelButtonType);

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
    
    private void processExtStatus(char[] status) {
        
        // We are after status[42] for the battery
        // and status[45] for the state of the reader. 
        if (status.length > 60) {            
 
            Integer battery = (int) status[42]; // old tricks are still good
            Boolean reading = (char) 00 != status[45];
            System.out.println("Reading: " + status[45]);
            
            // update the reading status and status label 
            Boolean currentStatus = readingStatus.getValue();
            if (!Objects.equals(reading, currentStatus)){
                externalInitiated = true;
                Platform.runLater(() -> {
                    if (reading) readingStatus.setValue(Boolean.TRUE);
                    else readingStatus.setValue(Boolean.FALSE);
                });
            }
            if (statusLabel.getText().equals("Starting Readers...") && reading) {
                Platform.runLater(() -> {
                    statusLabel.setText("Connected: Waiting for a chip read...");
                });
            } else if (statusLabel.getText().equals("Connected: Readers stopped") && reading){
                Platform.runLater(() -> {
                    statusLabel.setText("Connected: Waiting for a chip read...");
                });
            } else if (!reading){
                Platform.runLater(() -> {
                    statusLabel.setText("Connected: Readers stopped");
                });
            }
            
            // Now do the same for the battery indicator
            // Color code it based on a 60/25 green/yellow/red split
            Platform.runLater(() -> {
                batteryProgressBar.progressProperty().setValue(battery / 100.0f);
                if (battery > 60) batteryProgressBar.setStyle("-fx-accent: green");
                else if (battery > 25) batteryProgressBar.setStyle("-fx-accent: yellow");
                else batteryProgressBar.setStyle("-fx-accent: red");
            });

        }
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

                            // Beeper Volume
                            String volume = beeperVolumeChoiceBox.getSelectionModel().getSelectedItem();
                            if (volume != null) {
                                byte val = 3;
                                if (volume.equals("Off")) val = 0;
                                else if (volume.equals("Soft")) val = 1;
                                else if (volume.equals("Loud")) val = 2;

                                System.out.println("updateReaderSettings(): Setting beeper volume (0x21) " + volume + "(" + Byte.toString(val) + ")");

                                if (val != 3) {
                                    ultraOutput.flush();

                                    ultraOutput.writeBytes("u");
                                    ultraOutput.writeByte(33);  // 0x21, volume
                                    ultraOutput.writeByte(val);
                                    ultraOutput.writeByte(255);

                                    ultraOutput.flush();
                                    String result = commandResultQueue.poll(10, TimeUnit.SECONDS);
                                    if (result != null) {
                                        ultraSettings.put("21",Byte.toString(val));
                                        commit=true;
                                    } else {
                                    // timeout
                                        System.out.println("Timeout with command 'u0x21'");
                                    }
                                }
                            } else {
                               System.out.println("updateReaderSettings(): Beeper volume is NULL!");
                            }
                            // Mode
                            String mode = reader1ModeChoiceBox.getSelectionModel().getSelectedItem();
                            if (! isJoey.get() && mode != null){
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
                                System.out.println("updateReaderSettings(): Sending commit (u 0xFF 0xFF) command");
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
        public StringProperty TYPE = new SimpleStringProperty();
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
        public StringProperty typeProperty(){
            return TYPE;
        }
        public String toString(){
            return IP.getValueSafe() + " " + TYPE.getValueSafe() + " (" + MAC.getValueSafe() + ")";
        }
    }

    static String stringToHex(String string) {
        StringBuilder buf = new StringBuilder(200);
        for (char ch: string.toCharArray()) {
          if (buf.length() > 0)
            buf.append(' ');
          buf.append(String.format("%02x", (int) ch));
        }
        return buf.toString();
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
