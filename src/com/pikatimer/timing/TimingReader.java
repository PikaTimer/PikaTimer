/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import javafx.beans.property.StringProperty;

/**
 *
 * @author jcgarner
 */
public interface TimingReader {
    
    public void setInput(String input);
    
    public void setTimingInput(TimingLocationInput t); 
    
    public void selectInput(); 
          
    public StringProperty getInputStringProperty();
    
    public void startReading();
    
    public void stopReading(); 
    
}
