/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing.reader;

import com.pikatimer.timing.TimingLocationInput;
import com.pikatimer.timing.TimingReader;
import java.io.File;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Pane;

/**
 *
 * @author jcgarner
 */
public class PikaPCTimerFileReader implements TimingReader {
    private TimingLocationInput timingLocationInput;
    private File sourceFile; 
    private SimpleStringProperty fileName; 
    private Pane displayPane; 
    
    public PikaPCTimerFileReader(){
        
    }
    
    @Override
    public void setInput(String input) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectInput() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StringProperty getInputStringProperty() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void startReading() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stopReading() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTimingInput(TimingLocationInput t) {
        timingLocationInput = t; 

    }
    
}
