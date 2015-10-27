/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing.reader;

import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingReader;
import javafx.beans.property.BooleanProperty;
import javafx.scene.layout.Pane;

/**
 *
 * @author jcgarner
 */
public class PikaOutreachDirectReader implements TimingReader {
    
    public PikaOutreachDirectReader(){
    
    }

    @Override
    public Boolean chipIsBib() {
        return Boolean.FALSE; 
    }
    
    @Override
    public void setTimingListener(TimingListener t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showControls(Pane p) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readOnce() {
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
    public BooleanProperty getReadingStatus() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
   
    
}
