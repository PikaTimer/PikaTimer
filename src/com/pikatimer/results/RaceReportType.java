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

import com.pikatimer.race.Race;
import java.util.List;

/**
 *
 * @author jcgarner
 */
public interface RaceReportType {

    public void init(Race race);

    //The List is for the results with the current standings
    //the RaceReport is how the report generator gets its config of what to show
    public String process(List<ProcessedResult> r, RaceReport rr);
    
    public Boolean optionSupport(String feature);
    
}
