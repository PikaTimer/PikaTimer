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
package com.pikatimer.timing;

import com.pikatimer.timing.reader.PikaRFIDFileReader;
import com.pikatimer.util.AlphanumericComparator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
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
    @FXML private Button importButton;
    
    @FXML private Button addButton;
    @FXML private Button saveButton;
    @FXML private Button clearAllButton;
    @FXML private Button addRepeatButton;
    
    @FXML private TextField searchTextField;
    @FXML private Label mapCountLabel;
    
    @FXML private TextField bibTextField;
    @FXML private TextField chipTextField;
    
    @FXML private TextField startBibTextField;
    @FXML private TextField endBibTextField;
    @FXML private TextField chipOffsetTextField;


        
    private final ObservableList<ChipMap> chipMapList = FXCollections.observableArrayList();
    FilteredList<ChipMap> filteredchipMapList;
    SortedList<ChipMap> sortedTimeList;
            
    BooleanProperty mapModified = new SimpleBooleanProperty(false);
    
    TimingDAO timingDAO = TimingDAO.getInstance();
    
    public void initialize() {
        
        // Pull in any existing mappings into an observable list
        Map<String,String> chipMap = timingDAO.getBib2ChipMap().getChip2BibMap();
        
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
        
        bibMappingTableView.getSortOrder().add(bibTableColumn);
        
        //disable buttons 
        saveButton.disableProperty().bind(mapModified.not());
        clearAllButton.disableProperty().bind(Bindings.size(chipMapList).isEqualTo(0));
        deleteButton.disableProperty().bind(Bindings.size(bibMappingTableView.getSelectionModel().getSelectedItems()).isEqualTo(0));
        //addButton.disableProperty().bind(); // only if there is text in the chip and bib text fields
        
        // We use an arbitrary node to get the window we are in to set the exit handler
        // Wrap this in a runLater to avoid a NPE since the window does not yet exist
        Platform.runLater(()-> {
            bibMappingTableView.getScene().getWindow().setOnCloseRequest( event -> {
                if (mapModified.getValue()){
                    Alert closeConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    closeConfirmation.setContentText("There are unsaved changes to the Chip -> Bib mappings.");
                    Button closeButton = (Button) closeConfirmation.getDialogPane().lookupButton(
                            ButtonType.OK
                    );
                    closeButton.setText("Close Anyway");
                    closeConfirmation.setHeaderText("Unsaved Changes...");
                    Optional<ButtonType> closeResponse = closeConfirmation.showAndWait();
                    if (!ButtonType.OK.equals(closeResponse.get())) {
                        event.consume();
                    }
                }
            });
        });  
        
        // Integers only... 
        startBibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("startChipTextField Text Changed (newValue: " + newValue + ")");
            if (!newValue.isEmpty() && ! newValue.matches("^\\d+$")) {
                    Platform.runLater(() -> { 
                    int c = startBibTextField.getCaretPosition();
                    startBibTextField.setText(oldValue);
                    startBibTextField.positionCaret(c);
                });
            }
        });
        endBibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("startChipTextField Text Changed (newValue: " + newValue + ")");
            if (!newValue.isEmpty() && ! newValue.matches("^\\d+$")) {
                    Platform.runLater(() -> { 
                    int c = endBibTextField.getCaretPosition();
                    endBibTextField.setText(oldValue);
                    endBibTextField.positionCaret(c);
                });
            }
        });
        chipOffsetTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("startChipTextField Text Changed (newValue: " + newValue + ")");
            if (!newValue.isEmpty() && ! newValue.matches("^\\d+$")) {
                    Platform.runLater(() -> { 
                    int c = chipOffsetTextField.getCaretPosition();
                    chipOffsetTextField.setText(oldValue);
                    chipOffsetTextField.positionCaret(c);
                });
            }
        });
        // clean up the bib and chip text fields
        // no leading spaces, no leading zeroes
        // we remove trailing spaces on focus lost
        chipTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if (!newValue.isEmpty()) {
                

                Platform.runLater(() -> { 
                    int c = chipTextField.getCaretPosition();
                    chipTextField.setText(newValue.replaceFirst("^[ 0]*", ""));
                    chipTextField.positionCaret(c);
                });
                 
            }
        });
        
        chipTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                chipTextField.setText(chipTextField.getText().replaceFirst("^[ 0]*", "").replaceFirst(" *$", ""));
            }
        });
        
        bibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if (!newValue.isEmpty()) {
                

                Platform.runLater(() -> { 
                    int c = bibTextField.getCaretPosition();
                    bibTextField.setText(newValue.replaceFirst("^[ 0]*", ""));
                    bibTextField.positionCaret(c);
                });
                 
            }
        });
        
        bibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                bibTextField.setText(bibTextField.getText().replaceFirst("^[ 0]*", "").replaceFirst(" *$", ""));
            }
        });
        
        // This is stupid
        deleteButton.defaultButtonProperty().bind(deleteButton.focusedProperty());
        importButton.defaultButtonProperty().bind(importButton.focusedProperty());
        addButton.defaultButtonProperty().bind(addButton.focusedProperty());
        saveButton.defaultButtonProperty().bind(saveButton.focusedProperty());
        clearAllButton.defaultButtonProperty().bind(clearAllButton.focusedProperty());
        addRepeatButton.defaultButtonProperty().bind(addRepeatButton.focusedProperty());
        
    }
    
    public void deleteAction(ActionEvent fxevent){
        chipMapList.removeAll(bibMappingTableView.getSelectionModel().getSelectedItems());
        mapModified.setValue(Boolean.TRUE);
    }
    
    public void importAction(ActionEvent fxevent){
        FileChooser fileChooser = new FileChooser();
        File sourceFile;
        final BooleanProperty chipFirst = new SimpleBooleanProperty(false);
        
        fileChooser.setTitle("Select Bib -> Chip File");
        
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home"))); 
        
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt","*.csv"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"), 
                new FileChooser.ExtensionFilter("All files", "*")
            );
        
        sourceFile = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        if (sourceFile != null) {
            try {            
                    Optional<String> fs = Files.lines(sourceFile.toPath()).findFirst();
                    String[] t = fs.get().split(",", -1);
                    if (t.length != 2) return; 
                    
                    if(t[0].toLowerCase().contains("chip")) {
                        chipFirst.set(true);
                        System.out.println("Found a chip -> bib file");
                    } else if (t[0].toLowerCase().contains("bib")) {
                        chipFirst.set(false);
                        System.out.println("Found a bib -> chip file");
                    } else {
                        ChipMap newMapping = new ChipMap(t[0],t[1]);
                        if (chipMapList.contains(newMapping)){
                            chipMapList.get(chipMapList.indexOf(newMapping)).bibProperty.set(newMapping.bibProperty.get());
                        } else {
                            chipMapList.add(newMapping);
                        }
                        mapModified.setValue(true);    
                        
                        System.out.println("No header in file");
                        System.out.println("Mapped chip " + t[0] + " to " + t[1]);
                    }
                    Files.lines(sourceFile.toPath())
                        .map(s -> s.trim())
                        .filter(s -> !s.isEmpty())
                        .skip(1)
                        .forEach(s -> {
                            //System.out.println("readOnce read " + s); 
                            String[] tokens = s.split(",", -1);
                            if (t.length != 2) return; 
                            ChipMap newMapping;
                            if(chipFirst.get()) {
                                newMapping = new ChipMap(tokens[0],tokens[1]);
                                System.out.println("Mapped chip " + tokens[0] + " to " + tokens[1]);
                            } else {
                                newMapping = new ChipMap(tokens[1],tokens[0]);
                                System.out.println("Mapped chip " + tokens[1] + " to " + tokens[0]);
                            }
                            
                            if (chipMapList.contains(newMapping)){
                                chipMapList.get(chipMapList.indexOf(newMapping)).bibProperty.set(newMapping.bibProperty.get());
                            } else {
                                chipMapList.add(newMapping);
                            }
                            mapModified.setValue(true);
                            
                            
                        });
                    System.out.println("Found a total of " + chipMapList.size() + " mappings");
                    
                } catch (IOException ex) {
                    Logger.getLogger(PikaRFIDFileReader.class.getName()).log(Level.SEVERE, null, ex);
                    // We had an issue reading the file.... 
                }
            
            
        }
    }
    
    public void addRepeatingMappingButtonAction(ActionEvent fxevent){
        
        if (startBibTextField.getText().isEmpty() || endBibTextField.getText().isEmpty() || chipOffsetTextField.getText().isEmpty()) return;
        
        try {
            Integer start = Integer.parseInt(startBibTextField.getText());
            Integer end = Integer.parseInt(endBibTextField.getText());
            Integer offset = Integer.parseInt(chipOffsetTextField.getText());
            if (start == 0) start = 1;
            if (end < start ) {
                Integer tmp = start;
                start = end;
                end = tmp;
            }
            Integer chip = 1;
            Integer bib = 1;
            for(int i = start; i <= end; i++) {
                bib = i;
                chip = i + offset;
                ChipMap newMapping = new ChipMap(chip.toString(),bib.toString());
                if (chipMapList.contains(newMapping)){
                    chipMapList.get(chipMapList.indexOf(newMapping)).bibProperty.set(newMapping.bibProperty.get());
                } else {
                    chipMapList.add(newMapping);
                }
                mapModified.setValue(true);
            }
            
        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }
        startBibTextField.setText("");
        endBibTextField.setText("");
        chipOffsetTextField.setText("");
    }
    public void addMappingButtonAction(ActionEvent fxevent){
        ChipMap newMapping = new ChipMap(chipTextField.getText(),bibTextField.getText());
        if (chipMapList.contains(newMapping)){
            chipMapList.get(chipMapList.indexOf(newMapping)).bibProperty.set(newMapping.bibProperty.get());
        } else {
            chipMapList.add(newMapping);
        }
        mapModified.setValue(true);  
        
        chipTextField.setText("");
        bibTextField.setText("");
        chipTextField.requestFocus();
    }
    
    public void clearAllAction(ActionEvent fxevent){
        chipMapList.clear();
        mapModified.setValue(true);
    }
    
    public void saveButtonAction(ActionEvent fxevent){
        Map<String,String> bibMap = new ConcurrentHashMap();
        
        chipMapList.forEach(m -> bibMap.put(m.chipProperty().getValueSafe(), m.bibProperty().getValueSafe()));
        
        if (bibMap.isEmpty()) timingDAO.getBib2ChipMap().setUseCustomMap(Boolean.FALSE);
        else timingDAO.getBib2ChipMap().setUseCustomMap(Boolean.TRUE);
        timingDAO.getBib2ChipMap().setChip2BibMap(bibMap);
        
        timingDAO.saveBib2ChipMap(timingDAO.getBib2ChipMap());
        
        
        Task reprocessAllRawTimes = new Task<Void>() {
            @Override public Void call() {
                System.out.println("Starting reprocessAllRawTimes()");
                timingDAO.reprocessAllRawTimes();
                System.out.println("Done with reprocessAllRawTimes()");
                return null;
            }
        };
        Thread reprocessAllRawTimesThread = new Thread(reprocessAllRawTimes);
        reprocessAllRawTimesThread.setName("Thread-reprocessAllRawTimes");
        reprocessAllRawTimesThread.setPriority(1);
        reprocessAllRawTimesThread.start();
        
        if (TimingDAO.getInstance().getCookedTimes().size() > 1000 ){
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Reprocessing...");
            alert.setHeaderText("Reprocessing All Times...");
            alert.setContentText("PikaTimer may be unresponsive for a while if there are a large number of existing times");
            alert.showAndWait();
        }
        mapModified.setValue(false);
        ((Node) fxevent.getSource()).getScene().getWindow().fireEvent(
            new WindowEvent((
                    (Node) fxevent.getSource()).getScene().getWindow(),
                    WindowEvent.WINDOW_CLOSE_REQUEST
            )
        );
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
