/*
 * Copyright (C) 2020 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.race;

import com.pikatimer.PikaPreferences;
import com.pikatimer.event.Event;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.results.Result;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.timing.Segment;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.h2.tools.Csv;



/**
 * FXML Controller class
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class FXMLCourseRecordsController {

    @FXML TableView<CourseRecord> recordTableView;
    @FXML TableColumn<CourseRecord,String> categoryTableColumn;
    @FXML TableColumn<CourseRecord,String> segmentTableColumn;
    @FXML TableColumn<CourseRecord,String> timeTableColumn;
    @FXML TableColumn<CourseRecord,String> nameTableColumn;
    @FXML TableColumn<CourseRecord,String> ageTableColumn;
    @FXML TableColumn<CourseRecord,String> sexTableColumn;
    @FXML TableColumn<CourseRecord,String> yearTableColumn;
    @FXML TableColumn<CourseRecord,String> cityTableColumn;
    @FXML TableColumn<CourseRecord,String> stateTableColumn;
    @FXML TableColumn<CourseRecord,String> countryTableColumn;
    @FXML TableColumn<CourseRecord,String> newRecordHolderTableColumn;
    @FXML TableColumn<CourseRecord,String> newRecordTimeTableColumn;
    
    
    @FXML Button importButton;
    @FXML Button exportButton;
    @FXML Button mergeButton;
        
    Preferences globalPrefs = PikaPreferences.getInstance().getGlobalPreferences();
    
    Race race;
    
    public void initialize( ) {
        // TODO
        categoryTableColumn.setComparator(new AlphanumericComparator());
        categoryTableColumn.setCellValueFactory(c -> c.getValue().categoryProperty());
                
        //segmentTableColumn
        segmentTableColumn.setCellValueFactory(c -> {
            if (c.getValue().segmentProperty().isNull().get()) {
                return new SimpleStringProperty("OVERALL");
            };
            return c.getValue().segmentProperty().get().segmentNameProperty();
        });
        timeTableColumn.setComparator(new AlphanumericComparator());
        timeTableColumn.setCellValueFactory(c -> {
                return new SimpleStringProperty(DurationFormatter.durationToString(c.getValue().getRecordDuration(), 0, true) );
        });

        sexTableColumn.setCellValueFactory(c ->  c.getValue().sexProperty());
        ageTableColumn.setCellValueFactory(c ->  c.getValue().ageProperty());
        yearTableColumn.setCellValueFactory(c ->  c.getValue().yearProperty());
        cityTableColumn.setCellValueFactory(c ->  c.getValue().cityProperty());
        stateTableColumn.setCellValueFactory(c ->  c.getValue().stateProperty());
        countryTableColumn.setCellValueFactory(c ->  c.getValue().countryProperty());
        nameTableColumn.setCellValueFactory(c ->  c.getValue().nameProperty());
        
        newRecordHolderTableColumn.setCellValueFactory(c ->  {
            if (c.getValue().newRecord().isNotNull().get()) {
                return ParticipantDAO.getInstance().getParticipantByBib(c.getValue().newRecord().get().getBib()).fullNameProperty();
            };
            return new SimpleStringProperty("");
        });
        newRecordTimeTableColumn.setCellValueFactory(c -> {
                return new SimpleStringProperty(DurationFormatter.durationToString(c.getValue().newRecordDuration().getValue(), 0, true) );
        });
                
        
    }    
    
    public void setRace(Race r){
        race = r;
        recordTableView.setItems(r.raceCourseRecordsProperty());
        importButton.setOnAction(a -> importCRs());
        exportButton.setOnAction(a -> exportCRs());
        mergeButton.setOnAction(a -> mergeCRs());
        System.out.println("Race is set to " + r.getRaceName());
    }

    private void importCRs() {
        RaceDAO rDAO = RaceDAO.getInstance();
        
        
        // prompt for a file
        final FileChooser fileChooser = new FileChooser();
        
        fileChooser.setTitle("Open Course Records");
        
        File lastEventFolder = new File(globalPrefs.get("PikaEventHome", System.getProperty("user.home")));
        if (!lastEventFolder.exists() ) {
            // we have a problem
            lastEventFolder= new File(System.getProperty("user.home"));
        } else if (lastEventFolder.exists() && lastEventFolder.isFile()){
            lastEventFolder = new File(lastEventFolder.getParent());
           
        }
        
        System.out.println("Using initial directory of " + lastEventFolder.getAbsolutePath());

        fileChooser.setInitialDirectory(lastEventFolder); 
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        System.out.println("Opening existing file....");
        if (file == null) return;
        
        // if we have a valid file, loop on it.... 
        
        String csvImport = file.getAbsolutePath();
        
        // Let's play the "What type of text file is this..." game
        // Try UTF-8 and see if it blows up on the decode. If it does, default down to a platform specific type and then hope for the best
        // TODO: fix the "platform specific" part to not assume Windows in the US
        CharsetDecoder uft8Decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        String charset = "UTF-8"; 
        try {
            String result = new BufferedReader(new InputStreamReader(new FileInputStream(csvImport),uft8Decoder)).lines().collect(Collectors.joining("\n"));
        } catch (Exception ex) {
            System.out.println("Not UTF-8: " + ex.getMessage());
            charset = "Cp1252"; // Windows standard txt file stuff
        }

        try {
            ResultSet rs = new Csv().read(csvImport,null,charset);
            ResultSetMetaData meta = rs.getMetaData();
            
            // clear out existing CR's
            rDAO.clearCourseRecords(race);
            race.raceCourseRecordsProperty().clear();
            
            while (rs.next()) {

                CourseRecord c = new CourseRecord(race);
                for (int i = 0; i < meta.getColumnCount(); i++) {
                        System.out.println(rs.getString(i+1) + " -> " + meta.getColumnLabel(i+1));
                        switch(meta.getColumnLabel(i+1).toLowerCase()) {
                            case "segment":
                                System.out.println("Looking for segment " + rs.getString(i+1));
                                for (Segment s: race.getSegments()) {
                                    System.out.println("Checking " + s.getSegmentName());
                                    if (s.getSegmentName().equals(rs.getString(i+1))) {
                                        c.setSegmentID(s.getID());
                                        System.out.println("   Matched!");
                                    }
                                }
                                break;
                            case "category":
                                c.setCategory(rs.getString(i+1));
                                break;
                            case "sex":
                                c.setSex(rs.getString(i+1));
                                break;
                            case "time":
                                if (DurationParser.parsable(rs.getString(i+1)))
                                    c.setRecord(DurationParser.parse(rs.getString(i+1)).toNanos());
                                else System.out.println("Unable to parse " + rs.getString(i+1));
                                break;
                            case "year":
                                c.setYear(rs.getString(i+1));
                                break;
                            case "age":
                                c.setAge(rs.getString(i+1));
                                break;
                            case "name":
                                c.setName(rs.getString(i+1));
                                break;
                            case "city":
                                c.setCity(rs.getString(i+1));
                                break;
                            case "state":
                                c.setState(rs.getString(i+1));
                                break;
                            case "country":
                                c.setCountry(rs.getString(i+1));
                                break;
                        }
                }
                
                race.addCourseRecord(c);
                rDAO.upsertCourseRecord(c);

            }
        } catch (Exception ex) {
            System.out.println("Something bad happened... ");
            ex.printStackTrace();
        }
        
        ResultsDAO.getInstance().reprocessAllCRs(race);
    }
    
    private void exportCRs(){
        // prompt for a file, then loop on the CR's
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Course Records...");
        File lastEventFolder = new File(globalPrefs.get("PikaEventHome", System.getProperty("user.home")));
        if (!lastEventFolder.exists() ) {
            // we have a problem
            lastEventFolder= new File(System.getProperty("user.home"));
        } else if (lastEventFolder.exists() && lastEventFolder.isFile()){
            lastEventFolder = new File(lastEventFolder.getParent());
           
        }
        fileChooser.setInitialDirectory(lastEventFolder); 
        //fileChooser.getExtensionFilters().add(
        //        new FileChooser.ExtensionFilter("PikaTimer Events", "*.db") 
        //    );
        //fileChooser.setInitialFileName("*.pika");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) return;

        StringBuilder crCSV = new StringBuilder();
        // Write out the header as follows:
        crCSV.append("SEGMENT,CATEGORY,SEX,TIME,year,age,name,city,state,country").append(System.lineSeparator());
        
        race.getCourseRecords().forEach(cr -> {
            //        CSV fields
            //        SEGMENT (OVERALL, <segment name>)
            System.out.println("   Exporting CR: Segment: " + cr.getSegmentID());
            if (cr.getSegmentID() != null) crCSV.append(cr.segmentProperty().get().getSegmentName());
            else crCSV.append("OVERALL");
            crCSV.append(",");
            //        CATEGORY (OVERALL, 40-44, 60-65, etc)
            crCSV.append(cr.getCategory()).append(",");
            //        SEX
            crCSV.append(cr.getSex()).append(",");
            //        TIME
            System.out.println("CR Time: " + cr.getRecord() + " current " + cr.currentRecordTime().toString());
            crCSV.append(DurationFormatter.durationToString(cr.currentRecordTime(), 0, true)).append(",");
            //        year
             //        age
            //        name
            //        city
            //        state
            //        country
            if (cr.newRecord().isNotNull().getValue()){
                Result r = cr.newRecord().get();
                Participant p = ParticipantDAO.getInstance().getParticipantByBib(r.getBib());
                crCSV.append(Event.getInstance().getLocalEventDate().getYear()).append(",");
                crCSV.append(p.getAge()).append(",");
                crCSV.append("\"").append(p.fullNameProperty().getValueSafe()).append("\"").append(",");
                crCSV.append("\"").append(p.getCity()).append("\"").append(",");
                crCSV.append("\"").append(p.getState()).append("\"").append(",");
                crCSV.append("\"").append(p.getCountry()).append("\"").append(",");
            } else {
                crCSV.append(cr.getYear()).append(",");
                crCSV.append(cr.getAge()).append(",");
                crCSV.append("\"").append(cr.getName()).append("\"").append(",");
                crCSV.append("\"").append(cr.getCity()).append("\"").append(",");
                crCSV.append("\"").append(cr.getState()).append("\"").append(",");
                crCSV.append("\"").append(cr.getCountry()).append("\"").append(",");
            }
            crCSV.append(System.lineSeparator());

        });
        

            
            try {
                FileUtils.writeStringToFile(file, '\ufeff' + crCSV.toString(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        
    }
    
    private void mergeCRs(){
        // for each CR, update it if there is a new record
        
    }
    
}
