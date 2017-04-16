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

import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

            private AttributeMap(String k, String v) {
                key.setValue(k);
                value.setValue(v);            
            }
            
            @Override
            public String toString(){
                return value.getValueSafe();
            }
            
        }
        
        ResultSet rs;
        ArrayList<String> csvColumns = new ArrayList<>();
        ArrayList<ComboBox> comboBoxes = new ArrayList<>();
        try {
            rs = new Csv().read(model.getFileName(),null,null);
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
        
        // display the colum -> attribute chooser maps
        mapGridPane.setPadding(new Insets(10,10,10,10));
        mapGridPane.setHgap(20);
        mapGridPane.setVgap(2);
        for (int i = 0; i < csvColumns.size(); i++) {
            final String csvAttr = csvColumns.get(i); 
            final ComboBox comboBox = new ComboBox(); 
            mapGridPane.add(new Label(csvColumns.get(i)),0,i+1);
            comboBoxes.add(i,comboBox);
            mapGridPane.add(comboBoxes.get(i),1,i+1);
            comboBoxes.get(i).setItems(attList);
            comboBoxes.get(i).getSelectionModel().selectFirst(); 
            comboBoxes.get(i).setOnAction((event) -> {
                //TODO: If the new selected value is "Ignore" we should remove the map entry
                model.mapAttrib(csvAttr, ((AttributeMap)comboBox.getSelectionModel().getSelectedItem()).key.getValue());
            });
            for(AttributeMap entry: attList) {
                //System.out.println("Does " + csvColumns.get(i).toLowerCase() + " contain " + entry.key.toString().toLowerCase());
                if (csvColumns.get(i).toLowerCase().contains(entry.key.getValue().toLowerCase()) || 
                        entry.key.getValue().toLowerCase().contains(csvColumns.get(i).toLowerCase())) {
                    comboBoxes.get(i).setValue(entry);
                    model.mapAttrib(csvAttr,entry.key.getValue());
                    //System.out.println("Import: " + csvColumns.get(i).toLowerCase() + " matches " + entry.key.getValue().toLowerCase() );
                }
            }
        }
    }
    
    
    
}
