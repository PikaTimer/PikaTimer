/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import com.pikatimer.results.ProcessedResult;
import java.time.Duration;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */

public class AwardWinner {
    
    public Participant participant;
    public String awardTitle;
    public Integer awardPlace;
    public Duration awardTime;
    public Boolean tie = false;
    public ProcessedResult processedResult;
           
    public AwardWinner() {
        
    }
    
}
