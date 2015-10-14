/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing.reader;

import com.pikatimer.timing.TimingLocationInput;
import com.pikatimer.timing.TimingReader;
import javafx.beans.property.StringProperty;

/**
 *
 * @author jcgarner
 */
public class PikaOutreachDirectReader implements TimingReader {
    
    public PikaOutreachDirectReader(){
    
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
