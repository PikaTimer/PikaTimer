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
package com.pikatimer.results;

import com.pikatimer.results.reports.AgeGroup;
import com.pikatimer.results.reports.Award;
import com.pikatimer.results.reports.Overall;
import com.pikatimer.results.reports.OverallCSV;
import com.pikatimer.results.reports.OverallHTML;
import com.pikatimer.results.reports.OverallHTML5;
import com.pikatimer.results.reports.OverallJSON;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jcgarner
 */
public enum ReportTypes {
    OVERALL_HTML5,
    OVERALL_HTML,
    OVERALL,
    AGEGROUP,
    AWARD,
    CSV,
    JSON;

    
    private static final Map<ReportTypes, String> InputMap = createMap();

    private static Map<ReportTypes, String> createMap() {
        Map<ReportTypes, String> result = new HashMap<>();
        result.put(OVERALL, "Overall (Text)");
        result.put(AGEGROUP, "Age Group (Text)");
        result.put(AWARD, "Award (Text)");
        result.put(OVERALL_HTML,"Overall (Basic HTML)");
        result.put(OVERALL_HTML5,"Overall (Resonsive HTML)");
        result.put(CSV,"Overall (CSV)");
        result.put(JSON,"Overall (JSON)");

        return Collections.unmodifiableMap(result);
    }
    
    @Override 
    public String toString(){
        return InputMap.get(this);
    }

    public final  RaceReportType getReportType() {
        switch(this){
            case OVERALL:
                return new Overall();
            case OVERALL_HTML:
                return new OverallHTML();
            case OVERALL_HTML5:
                return new OverallHTML5();
            case AGEGROUP:
                return new AgeGroup();
            case AWARD:
                return new Award();
            case CSV:
                return new OverallCSV();
            case JSON:
                return new OverallJSON();
            
        }
        return null;
    }
}
