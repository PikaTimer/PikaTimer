/*
 * Copyright 2014 John Garner
 * All Rights Reserved 
 * 
 */
package com.pikatimer.util;

import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import javafx.util.StringConverter;

/**
 *
 * @author John
 */
public class WaveStringConverter extends StringConverter<Wave> {
    @Override
    public String toString(Wave w) {
        System.out.println("WaveStringConverter.toString() called");

                        if (w.getRace().wavesProperty().size() == 1 ) {
                            System.out.println("WaveStringConverter returning " + w.getRace().getRaceName());
                            return w.getRace().getRaceName();
                        } else if (RaceDAO.getInstance().listRaces().size() == 1 ) {
                            System.out.println("WaveStringConverter returning " + w.getWaveName());
                            return w.getWaveName();
                        } else {
                            System.out.println("WaveStringConverter returning " +w.getRace().getRaceName() + " " + w.getWaveName());
                            return w.getRace().getRaceName() + " " + w.getWaveName();
                        } 
    }

    @Override
    public Wave fromString(String s) {
        //TODO: itterate through the list of races to get the 
        // waves and then find one that matches by calling this.toString
        return null;
    }
}

