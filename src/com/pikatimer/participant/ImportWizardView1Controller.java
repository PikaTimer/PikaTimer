/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;

import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.File;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javax.annotation.PostConstruct;


@FXMLController("FXMLImportWizardView1.fxml")
public class ImportWizardView1Controller {
    
    @FXMLViewFlowContext
    private ViewFlowContext context;
    
    @FXML
    private Label fileNameLabel;
    
    @FXML
    private Button fileChooserButton;
    
    ImportWizardData model;
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView1Controller.initialize()");
        
        model = context.getRegisteredObject(ImportWizardData.class);
        fileNameLabel.textProperty().bind(model.fileNameProperty());
        
        fileChooserButton.setOnAction(this::chooseFile);
    }
    
    @FXML
    protected void chooseFile(ActionEvent fxevent){
        
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        
        if (model.getFileName().equals("")) {
            fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
            );
            
        } else {
            fileChooser.setInitialFileName(model.getFileName()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PikaTimer Events", "*.csv"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(fileChooserButton.getScene().getWindow());
        if (file != null) {
            model.setFileName(file.getAbsolutePath());
        }        
    }
}
