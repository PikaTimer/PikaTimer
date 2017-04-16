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
        if (w == null) return "";
        if (w.getRace() == null) return w.getWaveName();

        if (w.getRace().wavesProperty().size() == 1 ) {
            //System.out.println("WaveStringConverter returning " + w.getRace().getRaceName());
            return w.getRace().getRaceName();
        } else if (RaceDAO.getInstance().listRaces().size() == 1 ) {
            //System.out.println("WaveStringConverter returning " + w.getWaveName());
            return w.getWaveName();
        } else {
            //System.out.println("WaveStringConverter returning " +w.getRace().getRaceName() + " " + w.getWaveName());
            return w.getRace().getRaceName() + " " + w.getWaveName();
        }
    }

    public static String getString(Wave w){
        
        if (w == null) return "";
        if (w.getRace() == null) return w.getWaveName();

        if (w.getRace().wavesProperty().size() == 1 ) {
            //System.out.println("WaveStringConverter returning " + w.getRace().getRaceName());
            return w.getRace().getRaceName();
        } else if (RaceDAO.getInstance().listRaces().size() == 1 ) {
            //System.out.println("WaveStringConverter returning " + w.getWaveName());
            return w.getWaveName();
        } else {
            //System.out.println("WaveStringConverter returning " +w.getRace().getRaceName() + " " + w.getWaveName());
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

