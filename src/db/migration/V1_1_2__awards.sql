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
/**
 * Author:  John Garner <segfaultcoredump@gmail.com>
 * Created: Oct 11, 2017
 */

create table race_award_categories (
    id int,
    uuid varchar,
    race_id int,
    category_name varchar,
    category_priority int,
    award_type varchar,
    depth_type varchar,
    category_depth int,
    masters_age int,
    pull boolean,
    chip boolean
);

create table race_award_category_depths (
    ac_id int,
    start int,
    depth int
);