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
 * Created: Dec 22, 2017
 */

/* Unused table */
drop table participant_attributes;


create table custom_participant_attributes (
    id int,
    attribute_name varchar,
    attribute_type varchar,
    UUID varchar

);

create table custom_participant_attributes_values (
    id int,
    value varchar
);

create table participant_attributes (
    participant_id int,
    attribute_id int,
    attribute_value varchar
);

alter table race_age_groups add (
    null_to_zero boolean,
    custom_increments boolean,
    custom_names boolean
);

create table race_age_group_increments (
    ag_id int,
    increment_start int,
    increment_name varchar
);

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
    chip boolean,
    timing_point_type varchar,
    timing_point_value int,
    filter boolean,
    subdivide boolean,
    skew boolean,
    skew_type varchar,
    skew_attribute int,
    visible boolean,
    visible_overall boolean
);

create table race_award_category_depths (
    ac_id int,
    start int,
    depth int
);

create table race_award_category_filters (
    ac_id int,
    attribute varchar,
    comparison_type varchar,
    reference_value varchar
);

create table race_award_category_subdivide_list (
    ac_id int,
    attribute varchar
);

alter table race_split add (
    ignore_time boolean,
    mandatory boolean,
    CUTOFF_ABSOLUTE boolean
);

alter table race_segment add (
    hidden boolean,
    use_custom_pace boolean
);

alter table overrides add (
    type varchar, 
    note varchar
); 

