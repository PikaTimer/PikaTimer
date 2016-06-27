/*
 * Copyright (C) 2016 jcgarner
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
package com.pikatimer.results.reports;

import com.pikatimer.event.Event;
import com.pikatimer.event.EventDAO;
import com.pikatimer.race.Race;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import com.pikatimer.util.Unit;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jcgarner
 */
public class Overall implements RaceReportType{
    Race race;

    @Override
    public void init(Race r) {
        race = r;
    }

    @Override
    public String process(List<ProcessedResult> prList) {
        System.out.println("Overall.process() Called... ");
        String report = new String();
        
        
        Event event = Event.getInstance();  // fun with singletons... 
        report += event.getEventName() + " " + race.getRaceName() + System.lineSeparator();
        report += event.getLocalEventDate() + System.lineSeparator();
        report += System.lineSeparator();
        
        // print the headder
        report += " OA#"; // 4R chars 
        report += " SEX#"; // 5R chars
        report += "  AG#"; // 5R chars
        report += "  BIB"; // 5R chars for the bib #
        report += " AGE"; // 4R for the age
        report += " SEX"; // 4R for the sex
        report += " GRP  "; //6L for the AG Group
        report += " Name                 "; // 22L chars for the name 
        report += " City             "; // 18L for the city
        report += "  ST"; // 4C for the state code
        
        // Insert split stuff here
        
        // Chip time
        report += "    Time "; // 9R Need to adjust for the format code
       
        // gun time
        report += "    Gun  "; // 9R ibid
        // pace
        report += "   Pace"; // 10R
        report += System.lineSeparator();
        
        final StringBuilder chars = new StringBuilder();
        
        prList.forEach(pr -> {
            chars.append(StringUtils.leftPad(pr.getOverall().toString(),4));
            chars.append(StringUtils.leftPad(pr.getSexPlace().toString(),5));
            chars.append(StringUtils.leftPad(pr.getAGPlace().toString(),5));
            chars.append(StringUtils.leftPad(pr.getParticipant().getBib(),5));
            chars.append(StringUtils.leftPad(pr.getAge().toString(),4));
            chars.append(StringUtils.leftPad(pr.getSex(),4));
            chars.append(StringUtils.center(pr.getAGCode(),7));
            chars.append(StringUtils.rightPad(pr.getParticipant().fullNameProperty().getValueSafe(),22));
            chars.append(StringUtils.rightPad(pr.getParticipant().getCity(),18));
            chars.append(StringUtils.center(pr.getParticipant().getState(),4));

            // Insert split stuff here 
            
            // chip time
            chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8));
            chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 9));
            chars.append(StringUtils.leftPad(StringUtils.stripStart(Pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish(), Pace.MPM), "0"),10));
//            System.out.println("Results: " + r.getRaceName() + ": "
//                    + r.getParticipant().fullNameProperty().getValueSafe() 
//                    + "(" + pr.getSex() + pr.getAGCode() + "): " 
//                    + DurationFormatter.durationToString(pr.getChipFinish())
//                    + " O:" + pr.getOverall() + " S:" + pr.getSexPlace() 
//                    + " AG:" + pr.getAGPlace()
//            );
            
            chars.append(System.lineSeparator());
        
        
        });
            
        report += chars.toString();
        
        
        
        return report;
    }
    
    
    
}
