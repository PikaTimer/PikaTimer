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
package com.pikatimer.participant;

import com.pikatimer.race.RaceDAO;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javax.annotation.PostConstruct;
import org.h2.tools.Csv;


@FXMLController("FXMLImportWizardView2.fxml")
public class ImportWizardView2Controller {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    
    @FXML
    GridPane mapGridPane;
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView2Controller.initialize()");
        ImportWizardData model = context.getRegisteredObject(ImportWizardData.class);
        //model.setFileName("Test2");
        
        class AttributeMap {
            public SimpleStringProperty key = new SimpleStringProperty();
            public SimpleStringProperty value= new SimpleStringProperty();
            Integer customKey = -1;
            private AttributeMap(String k, String v) {
                key.setValue(k);
                value.setValue(v);            
            }
            private AttributeMap(Integer ck, String v) {
                key.setValue(v);
                value.setValue(v);  
                customKey = ck;
            }
            @Override
            public String toString(){
                return value.getValueSafe();
            }
            
        }
        
        // Let's play the "What type of text file is this..." game
        // Try UTF-8 and see if it blows up on the decode. If it does, default down to a platform specific type and then hope for the best
        // TODO: fix the "platform specific" part to not assume Windows in the US
        CharsetDecoder uft8Decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        String charset = "UTF-8"; 
        try {
             String result = new BufferedReader(new InputStreamReader(new FileInputStream(model.getFileName()),uft8Decoder)).lines().collect(Collectors.joining("\n"));
         } catch (Exception ex) {
             System.out.println("Not UTF-8: " + ex.getMessage());
             charset = "Cp1252"; // Windows standard txt file stuff
         }
        
        ResultSet rs;
        ArrayList<String> csvColumns = new ArrayList<>();
        ArrayList<ComboBox> comboBoxes = new ArrayList<>();
        try {
            rs = new Csv().read(model.getFileName(),null,charset);
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 0; i < meta.getColumnCount(); i++) {
                csvColumns.add(meta.getColumnLabel(i+1));
                System.out.println(meta.getColumnLabel(i+1));
            }
            int numAdded = 0;
            while (rs.next()) { numAdded++; }
            model.setNumToAdd(numAdded);
            
        } catch (SQLException ex) {
            Logger.getLogger(ImportWizardView2Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ObservableMap<String,String> participantAttributes = Participant.getAvailableAttributes();
        
        ObservableList<AttributeMap> attList = FXCollections.observableArrayList();
        attList.add(new AttributeMap("Ignore","Ignore"));
        participantAttributes.entrySet().stream().forEach((entry) -> {
            attList.add(new AttributeMap(entry.getKey(),entry.getValue()));
        });
        ParticipantDAO.getInstance().getCustomAttributes().forEach(ca -> {
            attList.add(new AttributeMap(ca.getID(),ca.getName()));
        });
        if (model.getWaveAssignByAttribute()) {
            if (RaceDAO.getInstance().listRaces().size() > 1)
                attList.add(new AttributeMap("RACE","Race"));
            if (RaceDAO.getInstance().listWaves().size() > RaceDAO.getInstance().listRaces().size() ) 
                attList.add(new AttributeMap("WAVE","Wave"));
        }
            
            
            
        // display the colum -> attribute chooser maps
        mapGridPane.setPadding(new Insets(10,10,10,10));
        mapGridPane.setHgap(20);
        mapGridPane.setVgap(2);
        for (int i = 0; i < csvColumns.size(); i++) {
            final String csvAttr = csvColumns.get(i); 
            final ComboBox<AttributeMap> comboBox = new ComboBox(); 
            mapGridPane.add(new Label(csvColumns.get(i)),0,i+1);
            comboBoxes.add(i,comboBox);
            mapGridPane.add(comboBoxes.get(i),1,i+1);
            comboBoxes.get(i).setItems(attList);
            comboBoxes.get(i).getSelectionModel().selectFirst(); 
            comboBoxes.get(i).setOnAction((event) -> {
                //TODO: If the new selected value is "Ignore" we should remove the map entry
                if (comboBox.getSelectionModel().getSelectedItem().customKey >= 0) 
                    model.mapAttrib(csvAttr,comboBox.getSelectionModel().getSelectedItem().customKey.toString());
                else model.mapAttrib(csvAttr, comboBox.getSelectionModel().getSelectedItem().key.getValue());
            });
            for(AttributeMap entry: attList) {
                //System.out.println("Does " + csvColumns.get(i).toLowerCase() + " contain " + entry.key.toString().toLowerCase());
                if (csvColumns.get(i).toLowerCase().contains(entry.key.getValue().toLowerCase()) || 
                        entry.key.getValue().toLowerCase().contains(csvColumns.get(i).toLowerCase())) {
                    comboBoxes.get(i).setValue(entry);
                    if (entry.customKey >= 0) model.mapAttrib(csvAttr, entry.customKey.toString());
                    else model.mapAttrib(csvAttr,entry.key.getValue());
                    //System.out.println("Import: " + csvColumns.get(i).toLowerCase() + " matches " + entry.key.getValue().toLowerCase() );
                }
            }
        }
    }
    
    
    
}
