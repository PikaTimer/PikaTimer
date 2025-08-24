/*
 * Copyright (C) 2025 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.participant.rsu;

import com.pikatimer.PikaPreferences;
import com.pikatimer.event.Event;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.util.HibernateUtil;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class RunSignUpDAO {

    private static final Logger logger = LoggerFactory.getLogger(RunSignUpDAO.class);
    private RSUConfig rsuConfig;

    private final Event event = Event.getInstance();

    private final BooleanProperty isSetup = new SimpleBooleanProperty(false); 
    
    private final ParticipantDAO partDAO = ParticipantDAO.getInstance();
    private final RaceDAO raceDAO = RaceDAO.getInstance();

    private static class SingletonHolder {

        private static final RunSignUpDAO INSTANCE = new RunSignUpDAO();
    }

    public static RunSignUpDAO getInstance() {

        return SingletonHolder.INSTANCE;
    }

    public BooleanProperty isSetup() {
        if (rsuConfig == null) {
            getRSUConfig();
        }

        return isSetup;
    }

    private RSUConfig getRSUConfig() {

        if (rsuConfig == null) {
            final List<RSUConfig> list;

            // Let's see if we have anything in the DB... 
            Session s = HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();

            logger.debug("RunSignUpDAO:: loading existing RSU Config");

            try {
                list = s.createQuery("from RSUConfig").list();

                logger.debug("RunSignUpDAO::getRSUConfig found " + list.size() + " rsu configs");

                if (!list.isEmpty()) {
                    rsuConfig = list.getFirst();
                    isSetup.set(true);
                }
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
            s.getTransaction().commit();
        }

        // are we still null? 
        if (rsuConfig == null) {
            rsuConfig = new RSUConfig();
            return rsuConfig;
        }

        return rsuConfig;
    }

    public void syncFromRSU(ProgressBar progressBar, Label progressLabel) {
        logger.debug("RunSignUpDAO::syncFromRSU() Start...");

        // Check the config
        if (!isSetup.get()) {
            logger.warn("RSUConfig not setup, not syncing!");
            return;
        }

        Task resyncTask = new Task<Void>() {
            @Override
            protected Void call() {
                logger.debug("Starting up the resyncThread");

                updateProgress(0, 100);

                // if we are useing a username / password, (re)generate the tmp_key and tmp_secret
                if (!rsuConfig.rsuLoginType.equals("API")) {
                    updateRSUKeys();
                }
                
                
                Map<Integer,Participant> regIDtoParticipantMap = new HashMap<>();
                Map<Integer,Participant> userIDtoParticipantMap = new HashMap<>();
                
                partDAO.listParticipants().forEach(p -> {
                    userIDtoParticipantMap.put(p.getRegUserID(), p);
                    p.getRegID2RaceIDDMap().keySet().forEach(reg -> {regIDtoParticipantMap.put(reg,p);});
                
                });
                
                                
                               
                // Quick count for the progress bar
                Integer eventsToProcess = 0;
                Integer eventsProcessed = 0;
                for (Integer a: rsuConfig.eventToRaceMap.keySet())
                    if (!rsuConfig.eventToRaceMap.get(a).equals(-1)) eventsToProcess+=2;
                
                // Timestamp to track when we last synced w/ RSU                
                Long lastRunTS = Instant.now().getEpochSecond();
                
                // default pageSize for RSU Requests
                Integer pageSize = 1000;
                
                // For each Event_ID that is not set to "IGNORE", download the participants
                for (Integer event: rsuConfig.eventToRaceMap.keySet()) {
                    if (!rsuConfig.eventToRaceMap.get(event).equals(-1)) { 
                        Integer regReturned = 0;
                        Integer page = 1;
                        logger.info("Getting registrations for Race with event_id {} from RSU", event);
                        
                        //TODO
                        // Figure out where to stick folks (default wave or by bib # with a fallback if there is no bib number)
                        Race race = RaceDAO.getInstance().getRaceByID(rsuConfig.eventToRaceMap.get(event));
                        Boolean multipleWaves = race.getWaves().size() > 1;
                        
                        Wave defaultWave = race.getWaves().getLast();
                        
                        // TODO
                        // Only sync from last sync time
                    
                        try {
                            do {

                                StringBuilder rsuURL = new StringBuilder();
                                rsuURL.append("https://runsignup.com/Rest/race/").append(rsuConfig.rsuRaceID);
                                rsuURL.append("/participants?format=json&event_id=").append(event);
                                rsuURL.append("&page=").append(page.toString()).append("&results_per_page=").append(pageSize);
                                rsuURL.append("&modified_after_timestamp=").append(rsuConfig.rsuLastSync);
                                rsuURL.append("&include_user_anonymous_flag=T&include_questions=T&include_registration_addons=T&supports_nb=T");
                                
                                // log it now before we tack on the key and secret
                                logger.debug("Participant request for race_id={} and event_id={}: {}", rsuConfig.rsuRaceID, event, rsuURL.toString());
                                
                                if (rsuConfig.rsuLoginType.equals("API")) {
                                    rsuURL.append("&api_key=").append(rsuConfig.rsuUsername);
                                    rsuURL.append("&api_secret=").append(rsuConfig.rsuPassword);
                                } else {
                                    rsuURL.append("&tmp_key=").append(rsuConfig.rsuTempKey);
                                    rsuURL.append("&tmp_secret=").append(rsuConfig.rsuTempSecret);
                                }
                                
                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(rsuURL.toString()))
                                        .build();

                                HttpClient client = HttpClient.newHttpClient();

                                try {
                                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                                    if (response.statusCode() == 200) {

                                        if (response.body().startsWith("[{")) { // We have a json array....
                                            try {
                                                JSONArray results = new JSONArray(response.body()).getJSONObject(0).getJSONArray("participants");
                                                logger.trace(results.toString(4));

                                                regReturned = results.length();
                                                for (int j = 0; j < results.length(); j++) {
                                                    JSONObject rsuReg = results.getJSONObject(j);
                                                    
                                                    
                                                        
                                                    Participant p;
                                                    if (regIDtoParticipantMap.containsKey(rsuReg.optIntegerObject("registration_id"))) {
                                                        p = regIDtoParticipantMap.get(rsuReg.optIntegerObject("registration_id"));
                                                        logger.trace("RSUSync: Found existing RSU RegistrationID {} for {}", rsuReg.optIntegerObject("registration_id"), p.fullNameProperty().toString() );
                                                    } else if (userIDtoParticipantMap.containsKey(rsuReg.getJSONObject("user").optIntegerObject("user_id"))) { 
                                                        p = userIDtoParticipantMap.get(rsuReg.getJSONObject("user").optIntegerObject("user_id"));
                                                        logger.trace("RSUSync: Found existing RSU UserID {} for {}", rsuReg.getJSONObject("user").optIntegerObject("user_id"), p.fullNameProperty().toString() );
                                                    } else {
                                                        p = new Participant();
                                                        logger.trace("RSUSync: unable to find an existing registration or user, creating a new user....");
                                                    }
                                                        
                                                    CountDownLatch platformDone = new CountDownLatch(1);
                                                    Platform.runLater(() -> {    

                                                        // Basic RSU Attributes
                                                        // ("First_Name", "Middle_Name", "Last_Name"));
                                                        // ("Gender", "Age", "DOB", "Bib"));
                                                        // ("City", "State", "Country"));
                                                        // ("EMail", "Giveaway", "isAnonymous", "Team_Name"));
                                                        //
                                                        // Everything else is a question prompt text

                                                        p.setBib(rsuReg.optString("bib_num"));

                                                        JSONObject pJSON = rsuReg.getJSONObject("user");

                                                        // set the RSU UserID
                                                        p.setRegUserID(pJSON.optIntegerObject("user_id"));


                                                        p.setFirstName(pJSON.optString("first_name"));
                                                        p.setMiddleName(pJSON.optString("middle_name"));
                                                        p.setLastName(pJSON.optString("last_name"));
                                                        p.setEmail(pJSON.optString("email"));
                                                        p.setSex(pJSON.optString("gender"));
                                                        //if (pJSON.optString("is_anonymous").equalsIgnoreCase("T")) p.setIsAnon(Boolean.TRUE);
                                                        p.setBirthday(pJSON.optString("dob"));

                                                        JSONObject aJSON = pJSON.getJSONObject("address");
                                                        p.setCity(aJSON.optString("city"));
                                                        p.setState(aJSON.optString("state"));
                                                        p.setZip(aJSON.optString("zip"));
                                                        p.setCountry(aJSON.optString("country_code"));

                                                        // link the particpant to the race
                                                        
                                                        
                                                        List<Wave> waveList = p.wavesObservableList();
                                                        if (waveList.isEmpty()) {
                                                            if (!multipleWaves) p.setWaves(defaultWave);
                                                            else p.setWaves(partDAO.getWaveByBib(p.getBib()));
                                                        } else {
                                                            // merge / replace time
                                                            
                                                            if (!multipleWaves) {
                                                                if (!waveList.contains(defaultWave)) {
                                                                    waveList.add(defaultWave);
                                                                    p.setWaves(waveList);
                                                                }
                                                            } else {
                                                                // This kinda sucks....
                                                                Map<Race,Wave> existingRaceWaveMap = new HashMap();
                                                                waveList.forEach(w -> existingRaceWaveMap.put(w.getRace(),w)); 
                                                            
                                                                Map<Race,Wave> raceWaveMap = new HashMap();
                                                                partDAO.getWaveByBib(p.getBib()).forEach(w -> raceWaveMap.put(w.getRace(),w));
                                                                
                                                                if (existingRaceWaveMap.containsKey(race)){
                                                                    Wave newWave = defaultWave;
                                                                    if (raceWaveMap.containsKey(race)) newWave = raceWaveMap.get(race);
                                                                    
                                                                    if (newWave != existingRaceWaveMap.get(race)) {
                                                                        waveList.remove(existingRaceWaveMap.get(race));
                                                                        waveList.add(newWave);
                                                                        p.setWaves(waveList);
                                                                    }
                                                                } else { 
                                                                    if (raceWaveMap.containsKey(race)) waveList.add(raceWaveMap.get(race));
                                                                    else waveList.add(defaultWave);
                                                                    p.setWaves(waveList);
                                                                }
                                                            }
                                                        }
                                                        
                                                        p.getRegID2RaceIDDMap().put(rsuReg.optIntegerObject("registration_id"),race.getID());
                                                           
                                                        
                                                        platformDone.countDown();
                                                    });
                                                    
                                                    platformDone.await();
                                                    // save
                                                    if (p.getID() > 0) partDAO.updateParticipant(p);
                                                    else partDAO.addParticipant(p);
                                                    
                                                    regIDtoParticipantMap.put(rsuReg.optIntegerObject("registration_id"), p);
                                                    userIDtoParticipantMap.put(p.getRegUserID(), p);
                                                    
                                                    updateMessage(p.fullNameProperty().getValueSafe());
                                                    
                                                }
                                                logger.debug("Found " + results.length() + " registrations\n\n");
                                            } catch (org.json.JSONException exJ) {
                                                regReturned = 0;
                                            }
                                            page++;
                                        } else {
                                            logger.error("Error in RSU Participant request: {} ", response.body());
                                            updateRSUKeys();
                                        }
                                    } else {
                                        logger.error("Error in RSU response: {} ", response.body());
                                    }
                                } catch (Exception ex) {
                                    logger.error("Exception in HttpClient response: ", ex);
                                }
                            } while (regReturned >= 1000);
                        } catch (Exception ex) {
                            logger.debug("RSU sync Exception: " + ex.getMessage());
                        }
                        
                        updateProgress(++eventsProcessed, eventsToProcess);
                    }
                }

//              // Removed Registrations
                // For each Event_ID that is not set to "IGNORE", download the participants
                for (Integer event: rsuConfig.eventToRaceMap.keySet()) {
                    if (!rsuConfig.eventToRaceMap.get(event).equals(-1)) { 
                        Integer regReturned = 0;
                        Integer page = 1;
                        
                        Race race = RaceDAO.getInstance().getRaceByID(rsuConfig.eventToRaceMap.get(event));
                        
                        logger.info("Getting removed registrations for {} with RSU EventID {}",race.getRaceName(), event);

                        try {
                            do {

                                StringBuilder rsuURL = new StringBuilder();
                                rsuURL.append("https://runsignup.com/Rest/race/").append(rsuConfig.rsuRaceID);
                                rsuURL.append("/removed-participants?format=json&event_id=").append(event);
                                rsuURL.append("&page=").append(page.toString()).append("&results_per_page=").append(pageSize);
                                rsuURL.append("&modified_after_timestamp=").append(rsuConfig.rsuLastSync);
                                rsuURL.append("&condensed_format=F");

                                // log it now before we tack on the key and secret
                                logger.debug("Participant request for race_id={} and event_id={}: {}", rsuConfig.rsuRaceID, event, rsuURL.toString());

                                if (rsuConfig.rsuLoginType.equals("API")) {
                                    rsuURL.append("&api_key=").append(rsuConfig.rsuUsername);
                                    rsuURL.append("&api_secret=").append(rsuConfig.rsuPassword);
                                } else {
                                    rsuURL.append("&tmp_key=").append(rsuConfig.rsuTempKey);
                                    rsuURL.append("&tmp_secret=").append(rsuConfig.rsuTempSecret);
                                }

                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(rsuURL.toString()))
                                        .build();

                                HttpClient client = HttpClient.newHttpClient();

                                try {
                                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                                    if (response.statusCode() == 200) {
                                        if (response.body().startsWith("[{")) { // We have a json array....
                                            try {
                                                JSONArray results = new JSONArray(response.body()).getJSONObject(0).getJSONObject("event").getJSONArray("participants");
                                                logger.trace(results.toString(4));

                                                regReturned = results.length();
                                                for (int j = 0; j < results.length(); j++) {
                                                    JSONObject removedReg = results.getJSONObject(j);
                                                    Integer regID = removedReg.optIntegerObject("registration_id");
                                                    
                                                    logger.debug("Removed Registration for {}: RegID: {}", race,regID);


                                                    // Lookup existing registration
                                                    if (regIDtoParticipantMap.containsKey(regID)) {
                                                        Participant p = regIDtoParticipantMap.get(regID);
                                                        logger.trace("RSUSync: Found existing RSU RegistrationID {} for {}", regID, p.fullNameProperty().toString() );
                                                        
                                                        // Remove the participant from the race/wave
                                                        Set<Wave> waves = new HashSet();
                                                        p.wavesObservableList().forEach(w -> {
                                                            if (!Objects.equals(w.getRace().getID(), race.getID())) waves.add(w);
                                                        });
                                                        p.setWaves(waves);
                                                        
                                                        // Cleanup the regid -> wave map
                                                        p.getRegID2RaceIDDMap().remove(regID);

                                                        // if they are no longer registered for anything, delete them
                                                        if (p.getWaveIDs().isEmpty()) {
                                                            partDAO.removeParticipant(p);
                                                        } else {
                                                            partDAO.updateParticipant(p);
                                                        }
                                                    } else logger.trace("RSUSync: Unable to find existing registration with id {}",removedReg.optIntegerObject("registration_id"));

                                                    

                                                }
                                                logger.debug("Processed " + results.length() + " removed registrations");
                                            } catch (org.json.JSONException exJ) {
                                                regReturned = 0;
                                                logger.debug("JSON Exception in get removed-participants: " + exJ.getMessage());

                                            }//.getJSONObject(0);

                                            page++;
                                        } else {
                                            logger.error("Error in RSU Participant request: {} ", response.body());
                                            updateRSUKeys();
                                        }
                                    } else {
                                        logger.error("Error in RSU response: {} ", response.body());
                                    }
                                } catch (Exception ex) {
                                    logger.error("Exception in HttpClient response: ", ex);
                                }
                            } while (regReturned >= 1000);
                        } catch (Exception ex) {
                            logger.debug("RSU sync Exception: " + ex.getMessage());
                        }

                    updateProgress(++eventsProcessed, eventsToProcess);
                    }
                }


                updateMessage("Done!");
                updateProgress(1,1);

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    // We don't really care....
                } finally {
                    updateMessage("");
                    updateProgress(0,1);
                }

                // save lastRunTS
                rsuConfig.setRSULastSync(lastRunTS);
                // Save config to DB
                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                s.beginTransaction();
                s.saveOrUpdate(rsuConfig);
                s.getTransaction().commit();

                return null;
            }
        };

        progressBar.progressProperty().bind(resyncTask.progressProperty());
        progressLabel.textProperty().bind(resyncTask.messageProperty());

        Thread resync = new Thread(resyncTask);
        
        resync.setDaemon(true);
        resync.start();
    }

    public void syncFromRSU() {
        logger.debug("RunSignUpDAO::syncFromRSU() Start...");

        Dialog dialog = new Dialog();

        dialog.setTitle("Importing runners from RSU...");
        dialog.setHeaderText("Importing runners from RSU...");

        Label runnerName = new Label("");
        ProgressBar pb = new ProgressBar(0);

        VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.getChildren().add(pb);
        vbox.getChildren().add(runnerName);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.setAlignment(Pos.CENTER);
        pb.setPrefWidth(300);
        runnerName.setPrefWidth(300);
        vbox.setPrefSize(450, 350);
        
        dialog.getDialogPane().setContent(vbox);
        
        ButtonType loginButtonType = new ButtonType("Close", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType);

        syncFromRSU(pb, runnerName);

        dialog.showAndWait();

    }

    public void syncToRSU() {

    }

    public void showSetupWizard() {
        // Wizard flow:
        // Page 1: RSU Login
        // Page 2: RSU Race Selection
        // Page 3: Map RSU Event -> PikaTimer Race
        
        // TODO: 
        // Page 4: Map RSU Attributes to custom attributes
        // Page 5: Options 
        //          --Case Normalization
        //          --City Mapping 
        
        // Page 6: Import / Finish

//        // Default RSU -> PikaTimer user attributes
//        Map<String, String> defaultAtrributeMappings = new HashMap();
//        defaultAtrributeMappings.put("FirstName", "First_Name");
//        defaultAtrributeMappings.put("MiddleName", "Middle_Name");
//        defaultAtrributeMappings.put("LastName", "Last_Name");
//        defaultAtrributeMappings.put("Sex", "Gender");
//        defaultAtrributeMappings.put("Age", "Age");
//        defaultAtrributeMappings.put("DateOfBirth", "Date_of_Birth");
//        defaultAtrributeMappings.put("City", "City");
//        defaultAtrributeMappings.put("St", "State");
//        defaultAtrributeMappings.put("Country", "Country");
//        defaultAtrributeMappings.put("E-Mail", "EMail");
//        defaultAtrributeMappings.put("Anonymous", "isAnonymous");
//        defaultAtrributeMappings.put("Swag", "Giveaway");
//        defaultAtrributeMappings.put("Bib", "Bib");
//        defaultAtrributeMappings.put("RegID", "Registration_ID");
        // Global prefs
        PikaPreferences pikaPrefs = PikaPreferences.getInstance();

        // Existing settings
        RSUConfig rsuConf = getRSUConfig();

        // Wizard variables
        final Map<Integer, Race> eventMap = new HashMap();
        //final Map<String, String> attributeMap = new HashMap();
        final Map<String, String> setupData = new HashMap();

        // pre-fill the setupData from the existing config or pikaPrefs
        // RSU Username
        if (rsuConf.rsuUsername != null) {
            setupData.put("rsuUsername", rsuConf.rsuUsername);
        } else {
            setupData.put("rsuUsername", pikaPrefs.get("rsuUsername", ""));
        }

        // RSU Password
        if (rsuConf.rsuPassword != null) {
            setupData.put("rsuPassword", rsuConf.rsuPassword);
        } else {
            setupData.put("rsuPassword", pikaPrefs.getObfuscated("rsuPassword"));
        }

        // RSU Login Type
        if (rsuConf.rsuLoginType != null) {
            setupData.put("rsuLoginType", rsuConf.rsuLoginType);
        } else {
            setupData.put("rsuLoginType", pikaPrefs.get("rsuLoginType", ""));
        }

        // Event Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        setupData.put("eventDate", event.getLocalEventDate().format(formatter));

        // RSU RaceID
        if (rsuConf.rsuRaceID != null) {
            setupData.put("rsuRaceID", rsuConf.rsuRaceID.toString());
        }

        // RSU Event -> Race map
        if (rsuConf.eventToRaceMap != null) {
            rsuConf.eventToRaceMap.keySet().forEach(k -> {
                setupData.put(k.toString(), rsuConf.eventToRaceMap.get(k).toString());
            });
        }

        List<WizardPane> wizardPanes = new ArrayList();

        Wizard wizard = new Wizard();
        wizard.setTitle("Setup RunSignUp Sync");

        /////////////////////////////////
        //
        // Wizard Pane 1: RSU Login Information
        // 
        // Username, password
        // onExit, do a login and stash the temp key and secret
        BooleanProperty pane1OkayToGo = new SimpleBooleanProperty(false);
        int row = 0;

        GridPane rsuLoginGrid = new GridPane();
        rsuLoginGrid.setVgap(10);
        rsuLoginGrid.setHgap(10);

        rsuLoginGrid.add(new Label("Login Method:"), 0, row);
        ComboBox<String> rsuLoginTypeComboBox = new ComboBox<>();
        rsuLoginTypeComboBox.getItems().addAll("Username / Password", "API Key/Secret");
        if (setupData.get("rsuLoginType").equals("API")) {
            rsuLoginTypeComboBox.getSelectionModel().select("API Key/Secret");
        } else {
            rsuLoginTypeComboBox.getSelectionModel().select("Username / Password");
        }
        GridPane.setHgrow(rsuLoginTypeComboBox, Priority.ALWAYS);
        rsuLoginGrid.add(rsuLoginTypeComboBox, 1, row++);

        rsuLoginGrid.add(new Label("Username / Key:"), 0, row);
        TextField rsuUsernameTextField = createTextField("rsuUsername");
        rsuUsernameTextField.setText(setupData.get("rsuUsername"));
        GridPane.setHgrow(rsuUsernameTextField, Priority.ALWAYS);
        rsuLoginGrid.add(rsuUsernameTextField, 1, row++);

        rsuLoginGrid.add(new Label("Password / Secret:"), 0, row);
        PasswordField rsuPasswordTextField = new PasswordField();
        GridPane.setHgrow(rsuPasswordTextField, Priority.ALWAYS);
        rsuPasswordTextField.setText(setupData.get("rsuPassword"));
        GridPane.setHgrow(rsuPasswordTextField, Priority.ALWAYS);
        rsuLoginGrid.add(rsuPasswordTextField, 1, row++);

        rsuLoginGrid.add(new Label("Status:"), 0, row);
        Button validateButton = new Button("Validate");
        Label loginSuccessLabel = new Label("Unchecked");
        Pane loginSpring = new Pane();
        HBox.setHgrow(loginSpring, Priority.ALWAYS);
        HBox validateHBox = new HBox(loginSuccessLabel, loginSpring, validateButton);
        validateHBox.setSpacing(4);
        validateHBox.setAlignment(Pos.CENTER_LEFT);
        rsuLoginGrid.add(validateHBox, 1, row++);

        // If the username or password fields change, force a re-validation
        rsuUsernameTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            pane1OkayToGo.setValue(false);
        });
        rsuPasswordTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            pane1OkayToGo.setValue(false);
        });

        validateButton.setOnAction((e) -> {

            // Are we using an api_key/secret or a username / password
            if (rsuLoginTypeComboBox.getSelectionModel().getSelectedItem().contains("Username")) {
                StringBuilder postData = new StringBuilder();
                postData.append("email=");
                postData.append(URLEncoder.encode(rsuUsernameTextField.getText(), StandardCharsets.UTF_8));
                postData.append("&password=");
                postData.append(URLEncoder.encode(rsuPasswordTextField.getText(), StandardCharsets.UTF_8));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://runsignup.com/Rest/login?format=json&supports_nb=T"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(postData.toString()))
                        .build();

                HttpClient client = HttpClient.newHttpClient();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JSONObject rsuResponse = new JSONObject(response.body());
                        if (rsuResponse.has("tmp_key")) {
                            pane1OkayToGo.setValue(true);
                            loginSuccessLabel.setText("Valid");
                            logger.debug("RSU Response: {}", response.body());

                            setupData.put("rsuLoginType", "PASSWORD");
                            setupData.put("rsuUsername", rsuUsernameTextField.getText());
                            setupData.put("rsuPassword", rsuPasswordTextField.getText());
                            setupData.put("rsuTempKey", rsuResponse.getString("tmp_key"));
                            setupData.put("rsuTempSecret", rsuResponse.getString("tmp_secret"));
                            logger.debug(" RSU Temp Key/Secret: {} / {}", rsuResponse.get("tmp_key"), rsuResponse.get("tmp_secret"));
                        } else {
                            logger.error("Error in RSU Login: {} ", response.body());
                            pane1OkayToGo.setValue(false);
                            loginSuccessLabel.setText("Invalid Username or Password!");
                        }
                    } else {
                        logger.error("Error in RSU Login: {} ", response.body());
                        pane1OkayToGo.setValue(false);
                        loginSuccessLabel.setText("Invalid Username or Password!");
                    }

                } catch (Exception ex) {
                    logger.error("Exception in HttpClient response: ", ex);
                    pane1OkayToGo.setValue(false);
                    loginSuccessLabel.setText("Error in RSU Login Request");
                }
            } else {
                // There is no login equivalent for the key/secret
                // so we will just make a call to /Rest/races/ and
                // see how many we get back
                // get the list of from RSU
                // https://runsignup.com/Rest/races?
                // tmp_key=KEY&tmp_secret=SECRET
                // &format=json&include_event_days=F&page=1&results_per_page=50&sort=name+ASC
                // &start_date=2024-05-27&end_date=2024-05-27

                StringBuilder requestURL = new StringBuilder();
                requestURL.append("https://runsignup.com/Rest/races");
                requestURL.append("?api_key=").append(rsuUsernameTextField.getText());
                requestURL.append("&api_secret=").append(rsuPasswordTextField.getText());
                requestURL.append("&format=json").append("&include_event_days=T");
                requestURL.append("&page=1&results_per_page=50&sort=name+ASC");
                requestURL.append("&start_date=").append(setupData.get("eventDate"));
                requestURL.append("&end_date=").append(setupData.get("eventDate"));

                logger.debug("RSU Get Races Request URL: {}", requestURL.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestURL.toString()))
                        .build();

                HttpClient client = HttpClient.newHttpClient();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    logger.debug("RSU Response: {} ", response.body());

                    if (response.statusCode() == 200) {
                        JSONObject rsuResponse = new JSONObject(response.body());
                        if (rsuResponse.has("races")) {
                            int responseSize = rsuResponse.getJSONArray("races").length();
                            if (responseSize > 0 && responseSize < 50) {
                                pane1OkayToGo.setValue(true);
                                loginSuccessLabel.setText("Valid");
                                setupData.put("rsuLoginType", "API");
                                setupData.put("rsuUsername", rsuUsernameTextField.getText());
                                setupData.put("rsuPassword", rsuPasswordTextField.getText());
                            } else {
                                loginSuccessLabel.setText("Invalid API Secret / Key");
                            }
                        } else {
                            logger.error("Error in RSU Login: {} ", response.body());
                        }
                    } else {
                        logger.error("Error in RSU Login: {} ", response.body());
                    }
                } catch (Exception ex) {
                    logger.error("Exception in HttpClient response: ", ex);
                }
            }

        });

        final WizardPane rsuLoginWizardPane = new WizardPane() {
            @Override
            public void onEnteringPage(Wizard wizard) {
                wizard.invalidProperty().bind(pane1OkayToGo.not());
            }

            @Override
            public void onExitingPage(Wizard wizard) {
                wizard.invalidProperty().unbind();
            }
        };
        rsuLoginWizardPane.setHeaderText("RunSignUp Login");
        rsuLoginWizardPane.setContent(rsuLoginGrid);
        wizardPanes.add(rsuLoginWizardPane);

        /////////////////////////////////
        // 
        // Wizard Pane 2: Get list of races from RSU 
        // 
        // Prompt the user to select the Race. Snag the race_event_days_id that is between the start_date and end_date for the rsuEvent
        row = 0;

        GridPane rsuRaceListGrid = new GridPane();
        rsuRaceListGrid.setVgap(10);
        rsuRaceListGrid.setHgap(10);

        record rsuRace(String name, Integer raceID, JSONObject details) {

            @Override
            public String toString() {
                return name;
            }
        }

        ObservableList<rsuRace> raceList = FXCollections.observableArrayList();
        ListView<rsuRace> raceListView = new ListView(raceList);
        raceListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        raceListView.setPrefHeight(250);
        raceListView.setMinHeight(250);
        raceListView.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(raceListView, Priority.ALWAYS);

        rsuRaceListGrid.add(raceListView, 0, row);

        setupData.put("race_event_days_id", "0");
        raceListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            LocalDate raceDate = Event.getInstance().getLocalEventDate();
            newValue.details.getJSONArray("race_event_days").forEach((r) -> {
                if (r instanceof JSONObject eventDays) {
                    LocalDate eventStart = LocalDate.parse(eventDays.getString("start_date"), DateTimeFormatter.ofPattern("M/d/yyyy 00:00"));
                    LocalDate eventEnd = LocalDate.parse(eventDays.getString("end_date"), DateTimeFormatter.ofPattern("M/d/yyyy 00:00"));
                    Integer raceEventDaysId = eventDays.getInt("race_event_days_id");
                    if (eventStart.compareTo(raceDate) <= 0 && eventEnd.compareTo(raceDate) >= 0) {
                        logger.debug("Event Days: {} ({}) is between {} and {}", raceDate, raceEventDaysId, eventStart, eventEnd);
                        setupData.put("race_event_days_id", raceEventDaysId.toString());
                        setupData.put("race_id", newValue.raceID.toString());
                    } else {
                        logger.debug("Event Days: {} ({}) is NOT between {} and {}", raceDate, raceEventDaysId, eventStart, eventEnd);
                    }
                }
            });
        });

        final WizardPane rsuRaceListWizardPane = new WizardPane() {
            @Override
            public void onEnteringPage(Wizard wizard) {
                wizard.invalidProperty().bind(raceListView.getSelectionModel().selectedItemProperty().isNull());

                raceList.clear();

                // get the list of from RSU
                // https://runsignup.com/Rest/races?
                // tmp_key=KEY&tmp_secret=SECRET
                // &format=json&include_event_days=F&page=1&results_per_page=50&sort=name+ASC
                // &start_date=2024-05-27&end_date=2024-05-27
                StringBuilder requestURL = new StringBuilder();
                requestURL.append("https://runsignup.com/Rest/races");

                if (setupData.get("rsuLoginType").equals("API")) {
                    requestURL.append("?api_key=").append(setupData.get("rsuUsername"));
                    requestURL.append("&api_secret=").append(setupData.get("rsuPassword"));
                } else {
                    requestURL.append("?tmp_key=").append(setupData.get("rsuTempKey"));
                    requestURL.append("&tmp_secret=").append(setupData.get("rsuTempSecret"));
                }
                requestURL.append("&format=json").append("&include_event_days=T");
                requestURL.append("&page=1&results_per_page=50&sort=name+ASC");
                requestURL.append("&start_date=").append(setupData.get("eventDate"));
                requestURL.append("&end_date=").append(setupData.get("eventDate"));

                logger.debug("RSU Get Races Request URL: {}", requestURL.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestURL.toString()))
                        .build();

                HttpClient client = HttpClient.newHttpClient();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    logger.debug("RSU Response: {} ", response.body());

                    if (response.statusCode() == 200) {
                        JSONObject rsuResponse = new JSONObject(response.body());
                        if (rsuResponse.has("races")) {
                            rsuResponse.getJSONArray("races").forEach((r) -> {
                                if (r instanceof JSONObject rObj) {
                                    JSONObject race = rObj.getJSONObject("race"); // FFS
                                    rsuRace raceRecord = new rsuRace(URLDecoder.decode(race.getString("name"), StandardCharsets.UTF_8), race.getInt("race_id"), race);
                                    logger.debug("Found Race: {} ({})", URLDecoder.decode(race.getString("name"), StandardCharsets.UTF_8), race.getInt("race_id"));
                                    raceList.add(raceRecord);
                                }
                            });

                            if (setupData.containsKey("rsuRaceID")) {
                                Integer raceID = Integer.valueOf(setupData.get("rsuRaceID"));
                                raceList.forEach(r -> {
                                    if (raceID.equals(r.raceID)) {
                                        raceListView.getSelectionModel().select(r);
                                    }
                                });
                            }
                        } else {
                            logger.error("Error in RSU Login: {} ", response.body());
                        }
                    } else {
                        logger.error("Error in RSU Login: {} ", response.body());
                    }
                } catch (Exception ex) {
                    logger.error("Exception in HttpClient response: ", ex);
                }
            }

            @Override
            public void onExitingPage(Wizard wizard) {
                wizard.invalidProperty().unbind();
                if (!raceListView.getSelectionModel().isEmpty()) {
                    setupData.put("rsuRaceID", raceListView.getSelectionModel().getSelectedItem().raceID.toString());
                }

            }
        };
        wizardPanes.add(rsuRaceListWizardPane);

        rsuRaceListWizardPane.setContent(rsuRaceListGrid);
        rsuRaceListWizardPane.setHeaderText("Select RSU Race");

        /////////////////////////////////
        //
        // Page 3: Get the race details based on the race_event_days_id from #3
        // Filter event_id's based on the start_end times and create rsuEvent records
        // Show each rsuEvent and prompt the user to map each the PikaTimer Race (or set to Ignore)
        // build up the list of available questions for step 5
        
        GridPane rsuEventListGrid = new GridPane();
        rsuEventListGrid.setVgap(10);
        rsuEventListGrid.setHgap(10);

        record rsuEvent(String name, Integer eventID, SimpleObjectProperty<Race> pikaRace) {

            public rsuEvent(String name, Integer eventID, Race r) {
                this(name, eventID, new SimpleObjectProperty<>(r));
            }

        }

        ObservableList<rsuEvent> eventList = FXCollections.observableArrayList();

        TableView<rsuEvent> eventTable = new TableView(eventList);
        eventTable.setEditable(true);
        eventTable.setPrefHeight(250);
        eventTable.setMinHeight(250);
        eventTable.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(eventTable, Priority.ALWAYS);

        TableColumn<rsuEvent, String> eventNameTablecolumn = new TableColumn<>("RSU Event");
        eventNameTablecolumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));

        TableColumn<rsuEvent, Race> pikaRaceTableColumn = new TableColumn<>("PikaTimer Event");
        pikaRaceTableColumn.setCellValueFactory(cellData -> cellData.getValue().pikaRace);
        pikaRaceTableColumn.setEditable(true);

        eventTable.getColumns().add(eventNameTablecolumn);
        eventTable.getColumns().add(pikaRaceTableColumn);

        rsuEventListGrid.add(eventTable, 0, 0);

        final WizardPane rsuEventListWizardPane = new WizardPane() {
            @Override
            public void onEnteringPage(Wizard wizard) {
                logger.debug("Start onEnteringPage() Wizard page3...");

                // List of PikaTimer Races: 
                ObservableList<Race> raceList = FXCollections.observableArrayList();
                Map<Integer, Race> raceListMap = new HashMap();
                Map<String, Race> raceListNameMap = new HashMap();

                raceDAO.listRaces().forEach(e -> {
                    raceList.add(e);
                    raceListMap.put(e.getID(), e);
                    raceListNameMap.put(e.getRaceName().toLowerCase(), e);

                });

                // and an IGNORE race
                Race dummy = new Race();
                dummy.setRaceName("Ignore");
                dummy.setID(-1);
                raceList.add(dummy);
                raceListMap.put(dummy.getID(), dummy);

//                // Read in the existing event_mapping to a map
//                Map<Integer, String> eventMap = new HashMap();
//                if (setupData.has("event_mapping")) {
//                    JSONObject map = setupData.getJSONObject("event_mapping");
//                    map.keySet().forEach(k -> {
//                        eventMap.put(Integer.valueOf(k), map.optString(k));
//                    });
//                }
                // Setup the cell factory for the rsuRace
                pikaRaceTableColumn.setCellFactory(tc -> {
                    ComboBox<Race> combo = new ComboBox<>();
                    combo.getItems().addAll(raceList);
                    TableCell<rsuEvent, Race> cell = new TableCell<rsuEvent, Race>() {
                        @Override
                        protected void updateItem(Race r, boolean empty) {
                            super.updateItem(r, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                combo.setValue(r);
                                setGraphic(combo);
                            }
                        }
                    };
                    combo.setOnAction(e -> {
                        // set the display name and race id code
                        tc.getTableView().getItems().get(cell.getIndex()).pikaRace.setValue(combo.getValue());
                    });
                    return cell;
                });

                eventList.clear();

                StringBuilder requestURL = new StringBuilder();
                requestURL.append("https://runsignup.com/Rest/race/");
                requestURL.append(setupData.get("race_id"));
                if (setupData.get("rsuLoginType").equals("API")) {
                    requestURL.append("?api_key=").append(setupData.get("rsuUsername"));
                    requestURL.append("&api_secret=").append(setupData.get("rsuPassword"));
                } else {
                    requestURL.append("?tmp_key=").append(setupData.get("rsuTempKey"));
                    requestURL.append("&tmp_secret=").append(setupData.get("rsuTempSecret"));
                }
                requestURL.append("&format=json").append("&future_events_only=F&most_recent_events_only=F");
                requestURL.append("&race_event_days_id=").append(setupData.get("race_event_days_id"));
                requestURL.append("&include_questions=T");

                logger.debug("RSU Get Race Request URL: {}", requestURL.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestURL.toString()))
                        .build();

                HttpClient client = HttpClient.newHttpClient();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    logger.debug("RSU Response: {} ", response.body());

                    if (response.statusCode() == 200) {
                        JSONObject rsuResponse = new JSONObject(response.body());
                        if (rsuResponse.has("race")) {

                            // Get the events
                            LocalDate raceDate = event.getLocalEventDate();
                            rsuResponse.getJSONObject("race").getJSONArray("events").forEach((r) -> {
                                if (r instanceof JSONObject event) {
                                    LocalDate eventStart = LocalDate.parse(event.getString("start_time").replaceAll(" ..:..", ""), DateTimeFormatter.ofPattern("M/d/yyyy"));
                                    // The end_time is optional and thus can be null. So it will default to the start_date. 
                                    LocalDate eventEnd = eventStart;
                                    if (!event.isNull("end_time")) {
                                        eventEnd = LocalDate.parse(event.getString("end_time").replaceAll(" ..:..", ""), DateTimeFormatter.ofPattern("M/d/yyyy"));
                                    }
                                    if (eventStart.compareTo(raceDate) <= 0 && eventEnd.compareTo(raceDate) >= 0) {

                                        // Populate the matching race 
                                        Race race = dummy;
                                        if (setupData.containsKey(Integer.toString(event.getInt("event_id")))) {
                                            logger.debug("Found Matching raceID <-> eventID in config...");
                                            if (raceListMap.containsKey(Integer.valueOf(setupData.get(Integer.toString(event.getInt("event_id")))))) {
                                                race = raceListMap.get(Integer.valueOf(setupData.get(Integer.toString(event.getInt("event_id")))));
                                                logger.debug("Found Matching raceID <-> eventID in config: {} to {}", race.getID(), event.getInt("event_id"));
                                            } else logger.debug("Found Matching race -> event ID but NO current race matches!!! Setting to default....");
                                        } else if (raceListNameMap.containsKey(event.getString("name").toLowerCase())) {
                                            race = raceListNameMap.get(event.getString("name").toLowerCase());
                                            logger.debug("Found name match for RSU event -> Pika Race: {} -> {}", event.getString("name"), race.getRaceName());
                                        }

                                        rsuEvent eventRecord = new rsuEvent(URLDecoder.decode(event.getString("name"), StandardCharsets.UTF_8), event.getInt("event_id"), race);
                                        logger.debug("Found Event: {} ({})", URLDecoder.decode(event.getString("name"), StandardCharsets.UTF_8), event.getInt("event_id"));
                                        eventList.add(eventRecord);

                                        /*if (eventMap.containsKey(event.getInt("event_id"))) {
                                            eventRecord.pikaRace.setValue(eventMap.get(eventRecord.eventID));
                                        } */
                                    } else {
                                        logger.debug("Event {} ({}) is NOT on {}", event.getString("name"), event.getInt("event_id"), eventStart, eventEnd);
                                    }
                                }
                            });

                            // Stash the questions
                            if (rsuResponse.getJSONObject("race").has("questions")) {
                                Map<Integer, String> rsuQuestions = new HashMap();
                                rsuResponse.getJSONObject("race").getJSONArray("questions").forEach((q) -> {
                                    if (q instanceof JSONObject question) {
                                        rsuQuestions.put(question.getInt("question_id"), question.getString("question_text"));
                                    }
                                });
                                /* setupData.put("rsuQuestions", rsuQuestions); */
                            }
                        } else {
                            logger.error("Error in RSU Login: {} ", response.body());
                        }
                    } else {
                        logger.error("Error in RSU Login: {} ", response.body());
                    }
                } catch (Exception ex) {
                    logger.error("Exception in HttpClient response: ", ex);
                }
                logger.debug("End onEnteringPage() Wizard page3...");
            }

            @Override
            public void onExitingPage(Wizard wizard) {
                logger.debug("Start onExitingPage() Wizard page3...");
                wizard.invalidProperty().unbind();

                eventList.forEach(e -> {
                    setupData.put(e.eventID.toString(), e.pikaRace.getValue().getID().toString());
                    logger.debug(" Event -> RaceID: {} -> {}", e.eventID, e.pikaRace.getValue().getID());
                });

                logger.debug("End onExitingPage() Wizard page3...");
            }
        };
        wizardPanes.add(rsuEventListWizardPane);

        rsuEventListWizardPane.setContent(rsuEventListGrid);
        rsuEventListWizardPane.setHeaderText("Map RSU Event to Pika Event");

//        /////////////////////////////////
//        //
//        // Page 4: Map the RSU questions -> Pikatimer custom attributes
//        // For each registration attribute, select an RSU source (native registration field or question/givaway source if available)
//        // Set all of the panes to the same height to make this a bit nicer
//        GridPane page5Grid = new GridPane();
//        page5Grid.setVgap(10);
//        page5Grid.setHgap(10);
//
//        record regAttribute(String pprrField, StringProperty rsuField) {
//
//        }
//
//        ObservableList<regAttribute> regAttributeList = FXCollections.observableArrayList();
//
//        TableView<regAttribute> regAttributeTable = new TableView(regAttributeList);
//        regAttributeTable.setEditable(true);
//        regAttributeTable.setPrefHeight(250);
//        regAttributeTable.setMinHeight(250);
//        regAttributeTable.setMaxWidth(Double.MAX_VALUE);
//        GridPane.setHgrow(regAttributeTable, Priority.ALWAYS);
//        regAttributeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
//
//        TableColumn<regAttribute, String> pprrAttributeTablecolumn = new TableColumn<>("PPRRScore Field");
//        pprrAttributeTablecolumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().pprrField));
//
//        TableColumn<regAttribute, String> rsuAttributeTableColumn = new TableColumn<>("RSU Attribute");
//        rsuAttributeTableColumn.setCellValueFactory(cellData -> cellData.getValue().rsuField);
//        rsuAttributeTableColumn.setEditable(true);
//
//        regAttributeTable.getColumns().add(pprrAttributeTablecolumn);
//        regAttributeTable.getColumns().add(rsuAttributeTableColumn);
//
//        page5Grid.add(regAttributeTable, 0, 0);
//
//        final WizardPane page4 = new WizardPane() {
//            @Override
//            public void onEnteringPage(Wizard wizard) {
//                logger.debug("Start onEnteringPage() Wizard page4...");
//
//                regAttributeList.clear();
//
//                // Setup the PPRRScore attributes that we need to map
//                /*
//                setupData.getJSONObject("PPRRScoreFieldList").getJSONArray("RegFields").iterator().forEachRemaining(e -> {
//                    if (e instanceof String regField) {
//                        if (!"Div".equals(regField)) {
//                            String def = defaultMappings.containsKey(regField) ? defaultMappings.get(regField) : "BLANK";
//                            logger.debug("Setting {} to {}", regField, def);
//                            regAttributeList.add(new regAttribute(regField, new SimpleStringProperty(def)));
//                        }
//                    }
//                });
//                */
//
//                // List of possible RSU fields 
//                List<String> rsuAttributesList = new ArrayList();
//
//                // Basic RSU Attributes
//                rsuAttributesList.addAll(Arrays.asList("First_Name", "Middle_Name", "Last_Name"));
//                rsuAttributesList.addAll(Arrays.asList("Gender", "Age", "Date_of_Birth", "Bib"));
//                rsuAttributesList.addAll(Arrays.asList("City", "State", "Country"));
//                rsuAttributesList.addAll(Arrays.asList("EMail", "Giveaway", "isAnonymous", "Team_Name", "Registration_ID"));
//
//                // Question Responses
//                /* 
//                if (setupData.has("rsuQuestions")) {
//                    JSONObject rsuQuestions = setupData.getJSONObject("rsuQuestions");
//                    rsuQuestions.keySet().forEach((q) -> {
//                        rsuAttributesList.add(rsuQuestions.optString(q));
//                    });
//                } */
//
//                // CatchAll for when we just dont care
//                rsuAttributesList.add("BLANK");
//
//                // Setup the cell factory for the attribute map
        ////                rsuAttributeTableColumn.setCellFactory(tc -> {
////                    ComboBox<String> combo = new ComboBox<>();
////                    combo.getItems().addAll(rsuAttributesList);
////                    TableCell<regAttribute, String> cell = new TableCell<regAttribute, String>() {
////                        @Override
////                        protected void updateItem(String reason, boolean empty) {
////                            super.updateItem(reason, empty);
////                            if (empty) {
////                                setGraphic(null);
////                            } else {
////                                combo.setValue(reason);
////                                setGraphic(combo);
////                            }
////                        }
////                    };
////                    combo.setOnAction(e -> {
////                        tc.getTableView().getItems().get(cell.getIndex()).rsuField.setValue(combo.getValue());
////                    });
////                    return cell;
////                });
//                logger.debug("End onEnteringPage() Wizard page4...");
//            }
//
//            @Override
//            public void onExitingPage(Wizard wizard) {
//                wizard.invalidProperty().unbind();
//                regAttributeList.forEach(e -> {
//                    //pprrscoreFieldMap.put(e.pprrField, e.rsuField.getValue());
//                    logger.debug(" PPRRScore Fieldlist: {} -> {}", e.pprrField, e.rsuField.getValue());
//                });
//                //setupData.put("fieldlist_mapping", pprrscoreFieldMap);
//            }
//        };
//
//        wizardPanes.add(page4);
//
//        page4.setContent(page5Grid);
//        page4.setHeaderText("Map RSU Attributes to PPRRScore Fieldlist");

        //////////////////////////////////
        //
        // Showtime....
        //
        for (WizardPane p : wizardPanes) {
            p.setMinSize(450, 350);
        }

        wizard.setFlow(new Wizard.LinearFlow(wizardPanes));

        // show wizard and wait for response
        wizard.showAndWait().ifPresent(result -> {
            if (result == ButtonType.FINISH) {

                logger.debug("setupData: {} ", setupData);

                // Save the rsu Username/Password to the global prefs
                pikaPrefs.set("rsuLoginType", setupData.get("rsuLoginType"));
                pikaPrefs.set("rsuUsername", setupData.get("rsuUsername"));
                pikaPrefs.setObfuscated("rsuPassword", setupData.get("rsuPassword"));

                // Save the Username/Password/LoginType to the race db
                rsuConf.rsuPassword = setupData.get("rsuPassword");
                rsuConf.rsuUsername = setupData.get("rsuUsername");
                rsuConf.rsuLoginType = setupData.get("rsuLoginType");

                rsuConf.rsuRaceID = Integer.valueOf(setupData.get("rsuRaceID"));
                
                rsuConfig.rsuLastSync = 0L;

                rsuConf.eventToRaceMap = new HashMap();
                setupData.keySet().forEach(k -> {
                    if (k.matches("^\\d+$")) {
                        rsuConf.eventToRaceMap.put(Integer.valueOf(k), Integer.valueOf(setupData.get(k)));
                    }
                });

                // Save config to DB
                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                s.beginTransaction();
                s.saveOrUpdate(rsuConf);
                s.getTransaction().commit();

                // fire up the dialog to sync from RSU
                syncFromRSU();
            }
        });

    }

    //Utility method for the ControlsFX Wizard
    private TextField createTextField(String id) {
        TextField textField = new TextField();
        textField.setId(id);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        return textField;
    }

    private void updateRSUKeys() {

        StringBuilder postData = new StringBuilder();
        postData.append("email=");
        postData.append(URLEncoder.encode(rsuConfig.rsuUsername, StandardCharsets.UTF_8));
        postData.append("&password=");
        postData.append(URLEncoder.encode(rsuConfig.rsuPassword, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://runsignup.com/Rest/login?format=json&supports_nb=T"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject rsuResponse = new JSONObject(response.body());
                if (rsuResponse.has("tmp_key")) {

                    logger.debug("updateRSUKeys -> RSU Response: {}", response.body());
                    rsuConfig.rsuTempKey = rsuResponse.getString("tmp_key");
                    rsuConfig.rsuTempSecret = rsuResponse.getString("tmp_secret");
                    logger.debug(" RSU Temp Key/Secret: {} / {}", rsuResponse.get("tmp_key"), rsuResponse.get("tmp_secret"));
                } else {
                    logger.error("Error in RSU Login: {} ", response.body());
                }
            } else {
                logger.error("Error in RSU Login: {} ", response.body());
            }
        } catch (Exception ex) {
            logger.error("Exception in HttpClient response: ", ex);

        }
    }

}
