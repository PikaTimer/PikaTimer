/*
 * Copyright (C) 2016 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.timing;

import com.pikatimer.util.AlphanumericComparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.WindowEvent;

/**
 * FXML Controller class
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class FXMLSetupBibMapController  {

    @FXML private TableView<ChipMap> bibMappingTableView;
    @FXML private TableColumn<ChipMap,String> chipTableColumn;
    @FXML private TableColumn<ChipMap,String> bibTableColumn;
    @FXML private Button deleteButton;
    
    @FXML private TextField searchTextField;
    @FXML private Label mapCountLabel;
    
    @FXML private TextField bibTextField;
    @FXML private TextField chipTextField;
    
    private final ObservableList<ChipMap> chipMapList = FXCollections.observableArrayList();
    FilteredList<ChipMap> filteredchipMapList;
    SortedList<ChipMap> sortedTimeList;
            
    Boolean mapModified = false;
    
    public void initialize() {
        
        // Pull in any existing mappings into an observable list
        Map<String,String> chipMap = TimingDAO.getInstance().getBib2ChipMap().getChip2BibMap();
        
        // create the chip to bib list
        chipMap.keySet().forEach(k -> {
            chipMapList.add(new ChipMap(k,chipMap.get(k)));
        });
        
        
        filteredchipMapList = new FilteredList<>(chipMapList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilterPredicate();
        });
        
        
        

        // 3. Wrap the FilteredList in a SortedList. 
        sortedTimeList = new SortedList<>(filteredchipMapList);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedTimeList.comparatorProperty().bind(bibMappingTableView.comparatorProperty());
        
        // 5. Set the cell factories and stort routines... 
        bibTableColumn.setCellValueFactory(b -> b.getValue().bibProperty());
        bibTableColumn.setComparator(new AlphanumericComparator());
        
        chipTableColumn.setCellValueFactory(c -> c.getValue().chipProperty());
        chipTableColumn.setComparator(new AlphanumericComparator());

        bibMappingTableView.setItems(sortedTimeList);
        bibMappingTableView.setPlaceholder(new Label("No chip mappings have been entered yet"));
        
        mapCountLabel.textProperty().bind(Bindings.concat(Bindings.size(sortedTimeList).asString(),"/",Bindings.size(chipMapList).asString()));
        
        // We use an arbitrary node to get the window we are in to set the exit handler
        // Wrap this in a runLater to avoid a NPE since the window does not yet exist
        Platform.runLater(()-> {
            bibMappingTableView.getScene().getWindow().setOnCloseRequest( event -> {
                if (mapModified){
                    Alert closeConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    closeConfirmation.setContentText("There are unsaved changes to the Chip -> Bib mappings.");
                    Button closeButton = (Button) closeConfirmation.getDialogPane().lookupButton(
                            ButtonType.OK
                    );
                    closeButton.setText("Close");
                    closeConfirmation.setHeaderText("Unsaved Changes...");
                    Optional<ButtonType> closeResponse = closeConfirmation.showAndWait();
                    if (!ButtonType.OK.equals(closeResponse.get())) {
                        event.consume();
                    }
                }
            });
        });  
    }
    
    public void deleteAction(ActionEvent fxevent){
        
        
    }
    
    public void importAction(ActionEvent fxevent){
        
    }
    public void addMappingButtonAction(ActionEvent fxevent){
        
    }
    public void clearAllAction(ActionEvent fxevent){
        chipMapList.clear();
        mapModified=true;
    }
    public void saveButtonAction(ActionEvent fxevent){
        
    }
    public void cancelButtonAction(ActionEvent fxevent){
        // We will just kick off the onClose action below
        ((Node) fxevent.getSource()).getScene().getWindow().fireEvent(
            new WindowEvent((
                    (Node) fxevent.getSource()).getScene().getWindow(),
                    WindowEvent.WINDOW_CLOSE_REQUEST
            )
        );
    }
    
    private void updateFilterPredicate(){
        filteredchipMapList.setPredicate(chipMap -> {
            // If filter text is empty, display all persons.
           // System.out.println("filteredParticpantsList.predicateProperty changing...");
            //System.out.println("...filterField="+filterField.textProperty().getValue());
            //System.out.println("...searchWaveComboBox=" + searchWaveComboBox.getCheckModel().getCheckedItems().size());
            if (searchTextField.textProperty().getValueSafe().isEmpty()) {
                //System.out.println("...both are empty: true");
                return true;
            }

            // Compare first name and last name of every person with filter text.
            String lowerCaseFilter = "(.*)(" + searchTextField.textProperty().getValueSafe() + ")(.*)";
            try {
                Pattern pattern =  Pattern.compile(lowerCaseFilter, Pattern.CASE_INSENSITIVE);

                if (    
                        pattern.matcher(chipMap.chipProperty().getValueSafe()).matches() ||
                        pattern.matcher(chipMap.bibProperty().getValueSafe()).matches()) {
                    return true; // Filter matches first/last/bib.
                } 

            } catch (PatternSyntaxException e) {
                
                return true;
            }
            return false; // Does not match.
        });
    }

    private static class ChipMap {
        private final StringProperty chipProperty = new SimpleStringProperty();
        private final StringProperty bibProperty = new SimpleStringProperty();
        
        public ChipMap() {
        }
        
        public ChipMap(String c, String b) {
            chipProperty.setValue(c);
            bibProperty.setValue(b);
        }
        
        public StringProperty chipProperty(){
            return chipProperty;
        }
        public StringProperty bibProperty(){
            return bibProperty;
        }
        
        @Override
        public boolean equals(Object obj) {
        //System.out.println("Wave.equals called for " + this.IDProperty.toString());
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChipMap other = (ChipMap) obj;
            //System.out.println("Wave.equals true");
        
        return Objects.equals(this.chipProperty.getValue(), other.chipProperty.getValue());
    }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + Objects.hashCode(this.chipProperty);
            return hash;
        }
        
    }
    
}
