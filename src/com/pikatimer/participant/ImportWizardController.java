/*
 * Modifications are Copyright 2014 John Garner. All rights reserved. 
 * 
 * Originally from: 
 * https://bitbucket.org/datafx/datafx/src/b63871fc223b1cc07a64174156b411d87819e56f/modules/tutorials/flow5/src/main/java/io/datafx/tutorial/WizardController.java?at=default
 * Copyright (c) 2014, Jonathan Giles, Johan Vos, Hendrik Ebbers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of DataFX, the website javafxdata.org, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    /**
     * The {@code init} method defines a internal flow that contains the steps of the wizard as separate views.
     * This internal flow will use animations for the navigation between different views.
     * @throws FlowException if the internal flow can't be created
     */
    @PostConstruct
    public void init() throws FlowException {
        ViewFlowContext flowContext = new ViewFlowContext();
        flowContext.register(new ImportWizardData());
        Flow flow = new Flow(ImportWizardView1Controller.class).
                withLink(ImportWizardView1Controller.class, "next", ImportWizardView2Controller.class).
                withLink(ImportWizardView2Controller.class, "next", ImportWizardView3Controller.class);

        flowHandler = flow.createHandler(flowContext);
        centerPane.getChildren().add(flowHandler.start(new AnimatedFlowContainer(Duration.millis(320), ContainerAnimations.ZOOM_IN)));
        
        backButton.setDisable(true);
        closeButton.setDisable(false);
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
        nextButton.setDisable(false);
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
            nextButton.setDisable(true);
            backButton.setDisable(true);
            closeButton.setText("Close");
            closeButton.setDisable(false);
        } else {
            nextButton.setDisable(false);
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