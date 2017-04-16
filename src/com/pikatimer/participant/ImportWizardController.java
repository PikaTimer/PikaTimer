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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.container.AnimatedFlowContainer;
import io.datafx.controller.flow.container.ContainerAnimations;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.annotation.PostConstruct;

/**
 * This class defines the main controller of the wizard. The complete action toolbar is managed here. In addition a
 * flow that contains all the custom views of the wizard is added to the view. The navigation for this internal flow
 * is managed here.
 */
@FXMLController("FXMLImportWizard.fxml")
public class ImportWizardController {

    @FXMLViewFlowContext
    private ViewFlowContext context;
    
    @FXML
    @ActionTrigger("back")
    private Button backButton;
    @FXML
    @ActionTrigger("close")
    private Button closeButton;
    @FXML
    @ActionTrigger("next")
    private Button nextButton;

    @FXML
    private StackPane centerPane;

    private FlowHandler flowHandler;
    private ImportWizardData model;

    /**
     * The {@code init} method defines a internal flow that contains the steps of the wizard as separate views.
     * This internal flow will use animations for the navigation between different views.
     * @throws FlowException if the internal flow can't be created
     */
    @PostConstruct
    public void init() throws FlowException {
        ViewFlowContext flowContext = new ViewFlowContext();
        model = new ImportWizardData();
        flowContext.register(model);
        Flow flow = new Flow(ImportWizardView1Controller.class).
                withLink(ImportWizardView1Controller.class, "next", ImportWizardView2Controller.class).
                withLink(ImportWizardView2Controller.class, "next", ImportWizardView3Controller.class);

        flowHandler = flow.createHandler(flowContext);
        centerPane.getChildren().add(flowHandler.start(new AnimatedFlowContainer(Duration.millis(320), ContainerAnimations.ZOOM_IN)));
        
        backButton.setDisable(true);
        closeButton.setDisable(false);
        
        nextButton.disableProperty().bind(model.nextButtonDisabledProperty());
        
        //Platform.runLater(() -> nextButton.getScene().setTitle("Import Participants..."));
    }

    /**
     * This method will be called when the {@code back} action will be executed. The method handles the navigation of
     * the internal flow that contains the steps of the wizard as separate views. In addition the states of the action
     * buttons will be managed.
     * @throws VetoException If the navigation can't be executed
     * @throws FlowException If the navigation can't be executed
     */
    @ActionMethod("back")
    public void onBack() throws VetoException, FlowException {
        flowHandler.navigateBack();
        if(flowHandler.getCurrentViewControllerClass().equals(ImportWizardView1Controller.class)) {
            backButton.setDisable(true);
            // bind the nextButton to the existance of a valid file name
        } else {
            backButton.setDisable(false);
        }
        closeButton.setDisable(false);
        //nextButton.setDisable(false);
    }

    /**
     * This method will be called when the {@code next} action will be executed. The method handles the navigation of
     * the internal flow that contains the steps of the wizard as separate views. In addition the states of the action
     * buttons will be managed.
     * @throws VetoException If the navigation can't be executed
     * @throws FlowException If the navigation can't be executed
     */
    @ActionMethod("next")
    public void onNext() throws VetoException, FlowException {
        flowHandler.handle("next");
        
        // bind the nextButton to the existance of a valid file name
        // unbind it when we get to View2
        
        if(flowHandler.getCurrentViewControllerClass().equals(ImportWizardView3Controller.class)) {
            //nextButton.setDisable(true);
            model.nextButtonDisabledProperty().set(true);
            backButton.setDisable(true);
            closeButton.setText("Close");
            closeButton.setDisable(false);
        } else {
            //nextButton.setDisable(false);
            backButton.setDisable(false);
            closeButton.setDisable(false);
        }
        
    }

    /**
     * This method will be called when the {@code finish} action will be executed. The method handles the navigation of
     * the internal flow that contains the steps of the wizard as separate views. In addition the states of the action
     * buttons will be managed.
     * @throws VetoException If the navigation can't be executed
     * @throws FlowException If the navigation can't be executed
     */
    @ActionMethod("close")
    public void onFinish() throws VetoException, FlowException {
        Window stage = closeButton.getScene().getWindow();
//        flowHandler.navigateTo(ImportWizardView3Controller.class);
//        closeButton.setDisable(true);
//        nextButton.setDisable(true);
//        backButton.setDisable(false);
        stage.hide();
    }
}