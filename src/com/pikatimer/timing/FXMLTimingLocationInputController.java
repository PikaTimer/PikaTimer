/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.converter.BigDecimalStringConverter;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
// Rename this to TiminglocationInput and ditch the fxml/controller crap. 
public class FXMLTimingLocationInputController{
    @FXML private TextField locationNameTextField; 
    @FXML private GridPane baseGridPane; 
    
    @FXML private Pane readerPane;
    @FXML private ChoiceBox inputTypeChoiceBox;
    //@FXML private Button inputChooserButton;
    //@FXML private TextField timingLocationInputDataTextField;
    @FXML private Label readCountLabel;
    @FXML private CheckBox timeSkewCheckBox;
    @FXML private TextField skewTextField;     
    @FXML private CheckBox backupCheckBox;
    //private VBox parentPane;  
    private TimingDAO timingLocationDAO;
    private TimingLocationInput timingLocationInput;
    //private TimingReader timingReader;
    
    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        // TODO
        //parentPane = (VBox) baseGridPane.getParent();
        //System.out.println("parentPane is a " +  parentPane.getClass().getName());

        timingLocationDAO=TimingDAO.getInstance();
        
        
        inputTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TimingInputTypes>() {
            @Override
            public void changed(ObservableValue<? extends TimingInputTypes> observableValue, TimingInputTypes o, TimingInputTypes n) {
                if (o == null || ! o.equals(n)) {
                    System.out.println("inputTypeChoiceBox event");
                    timingLocationInput.setTimingInputType(n);
                    timingLocationInput.initializeReader(readerPane);
                    timingLocationDAO.updateTimingLocationInput(timingLocationInput);


                }
            }
        });
        
    }    
    
    public void setTimingLocationInput(TimingLocationInput ti) {
        System.out.println("setTimingLocationInput called...");
        
        if(ti != null) {
            // Initialize everything.
            //timingLocationInputNameTextField.textProperty().setValue(ti.getLocationName());
            timingLocationInput = ti; 
            locationNameTextField.setText(ti.getLocationName());
            
            //Watch for text changes... Because setOnInputMethodTextChanged does not work :-( 
            locationNameTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue) {
                    System.out.println("locationNameTextfield out focus");
                    if ( ! locationNameTextField.getText().equals(timingLocationInput.getLocationName()) ) {
                        System.out.println("Name changed from " + timingLocationInput.getLocationName()+ " to " + locationNameTextField.getText());
                        timingLocationInput.setLocationName(locationNameTextField.getText());
                        timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                    } else {
                        System.out.println("No change in name");
                    }
                }
            });
            
            // Get the reader type and wire in the choice box
            ObservableList<TimingInputTypes> readerTypeList = FXCollections.observableArrayList(Arrays.asList(TimingInputTypes.values()));
            inputTypeChoiceBox.setItems(readerTypeList);
            
            
            
            
            
            if(timingLocationInput.getTimingInputType() != null) {
                inputTypeChoiceBox.setValue(timingLocationInput.getTimingInputType());
            } else {
                inputTypeChoiceBox.setValue(TimingInputTypes.RFIDFile);
            }

            timingLocationInput.initializeReader(readerPane);
            
            readCountLabel.textProperty().bind(Bindings.convert(timingLocationInput.readCountProperty())); 
            
            
            //Setup the skew
                   
            skewTextField.disableProperty().bind(timeSkewCheckBox.selectedProperty().not());
            
            
            // If we are skewing.... 
            timeSkewCheckBox.setSelected(timingLocationInput.getSkewLocationTime()); 
            
            timeSkewCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
                if (!old_val.equals(new_val)) {
                    timingLocationInput.setSkewLocationTime(new_val);
                    timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                }
            });
            skewTextField.textProperty().setValue(timingLocationInput.getSkewString());
            //skewTextField.textFormatterProperty().setValue(new TextFormatter(new BigDecimalStringConverter()));
            
            DecimalFormat format = new DecimalFormat( "0;-0" );

            skewTextField.setTextFormatter( new TextFormatter<>(c ->{
                if ( c.getControlNewText().isEmpty() )
                {
                    return c;
                }

                ParsePosition parsePosition = new ParsePosition( 0 );
                Object object = format.parse( c.getControlNewText(), parsePosition );

                if ( object == null || parsePosition.getIndex() < c.getControlNewText().length() )
                {
                    return null;
                }
                else
                {
                    return c;
                }
            }));
            
            
            
            
            

            skewTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue) {
                    System.out.println("skewTextField out focus");
                    if ( ! skewTextField.getText().equals(timingLocationInput.getSkewString()) ) {
                        System.out.println("Skew changed from " + timingLocationInput.getSkewString()+ " to " + skewTextField.getText());
                        timingLocationInput.setSkewString(skewTextField.getText());
                        timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                        if (timingLocationInput.getSkewNanos().equals(0L)) timeSkewCheckBox.setSelected(false); 
                        timingLocationInput.reprocessReads();
                    } else {
                        System.out.println("No change in skew time");
                    }
                }
            });

            //Backup check box
            //backupCheckBox.
        
        
        
        
        
        }
            
    }
    
    public void removeTimingInput(ActionEvent fxevent){
        System.out.println("parentPane is a " +  baseGridPane.getParent().getClass().getName());

        boolean remove = ((VBox) baseGridPane.getParent()).getChildren().remove(baseGridPane);
        if (timingLocationInput != null) {
            timingLocationDAO.removeTimingLocationInput(timingLocationInput);
        }
        //TimingLocationDAO.getInstance().removeTimingLocationInput(this);
        //parent.getChildren().remove(this); 
       
    }
    
    public void clearReads(ActionEvent fxevent){
        //timingLocationInput.stopReader();
        timingLocationInput.clearReads(); 
    }
    

}
