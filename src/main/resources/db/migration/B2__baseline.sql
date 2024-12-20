/* 
 * Copyright (C) 2024 John Garner <segfaultcoredump@gmail.com>
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


-- Java -> SQL types used
-- Integer -- int (ID's, list indexes, etc)
-- Strings -- varchar
-- Duration -- bigint (durations are stored as nanoseconds)
-- LocalTime / LocalDate / LocalDateTime -- varchar (use toString() and .parse() to store/retrieve)
-- Boolean -- boolean 
-- BigDecimal -- numeric (for race distances and numbers that require exact precision, like 3.140 vs 3.13999999999999)


create table event (
    id int primary key, 
    event_name varchar, 
    event_date varchar
);

create table event_options (
    event_id int
);

create table event_options_attributes (
    event_id int, 
    index_id int, 
    attribute varchar, 
    attribute_value varchar
);

create table race (
    race_id int primary key, 
    race_name varchar, 
    race_distance numeric(20,5),
    race_dist_unit varchar,
    race_bib_start varchar,
    race_bib_end varchar,
    race_cutoff bigint,
    race_max_start bigint,
    race_relay boolean,
    uuid varchar
); 

create table race_attributes (
    race_id int, 
    attribute varchar, 
    attribute_value varchar
);

create table race_waves (
    wave_id int primary key,
    race_id int, 
    wave_name varchar, 
    wave_start_time varchar, 
    wave_max_start_time bigint,
    wave_assignment_method varchar, 
    wave_assignment_attr1 varchar, 
    wave_assignment_attr2 varchar
); 

create table race_awards (
    race_id int
);

create table race_awards_attributes (
    race_id int, 
    index_id int, 
    attribute varchar, 
    attribute_value varchar
);

create table race_age_groups (
    race_id int,
    ag_increment int,
    masters_start int,
    ag_start int,
    null_to_zero boolean,
    custom_increments boolean,
    custom_names boolean
);

create table race_participants (
    race_id int, 
    participant_id int
); 

create table race_split (
    split_id int primary key, 
    race_id int, 
    timing_loc_id int, 
    split_seq_number int, 
    split_distance numeric(20,5),
    split_dist_unit varchar, 
    split_pace_unit varchar, 
    split_name varchar, 
    short_name varchar, 
    min_time bigint,
    cutoff_time bigint,
    ignore_time boolean,
    mandatory boolean,
    CUTOFF_ABSOLUTE boolean

);

create table race_segment (
    segment_id int primary key, 
    race_id int, 
    start_split_id int, 
    end_split_id int,
    segment_name varchar,
    pace_unit varchar,
    hidden boolean,
    use_custom_pace boolean
);

create table participant ( 
    participant_id int primary key, 
    uuid varchar,
    first_name varchar, 
    middle_name varchar,
    last_name varchar, 
    age int, 
    birthday varchar, 
    bib_number varchar, 
    sex varchar, 
    city varchar, 
    state varchar, 
    zip varchar,
    country varchar,
    team_id int,
    email varchar,
    status varchar,
    note varchar,
    reg_id varchar
);


create table timing_location (
    timing_location_id int primary key, 
    timing_location_name varchar,
    filterStartDuration bigint, 
    filterEndDuration bigint,
    AUTO_ASSIGN_TO_RACE_ID int,
    announcer boolean
);
 
create table bib2chip (bib2chip_id int, custom_map boolean);
create table bib2chipmap (bib2chip_id int, bib varchar, chip varchar unique);

create table part2wave (index_id int, participant_id int, wave_id int); 

create table timing_location_input (
    id int primary key,
    timing_location_id int,
    input_name varchar,
    timing_location_type varchar,
    skew boolean,
    time_skew bigint,
    backup boolean,
    announcer boolean
);
 
create table timing_location_input_attributes (
    index_id int, 
    tli_id int, 
    attribute varchar, 
    attribute_value varchar
); 

create table raw_timing_data (
    id int, 
    timing_loc_input_id int, 
    chip_id varchar, 
    raw_time bigint, 
    ignore_time boolean
); 

create table cooked_timing_data (
    id int, 
    timing_loc_id int, 
    timing_loc_input_id int, 
    raw_time_id int, 
    raw_chip_id varchar,
    bib_id varchar, 
    cooked_time bigint, 
    backup_time boolean, 
    ignore_time boolean
);

create table results (
    result_id int, 
    race_id int,
    bib varchar, 
    waveStart bigint,
    partStart bigint,
    partFinish bigint
);

create table split_results (
    result_id int,
    split_id int,
    split_time bigint
);

create table overrides (
    override_id int, 
    bib varchar, 
    split_id int, 
    override_time bigint, 
    relative_to_start boolean,
    type varchar, 
    note varchar
); 

create table race_outputs (
    id int primary key, 
    uuid varchar,
    race_id int,
    output_type varchar
);

create table race_output_attributes (
    output_id int, 
    attribute varchar, 
    attribute_value varchar
); 

create table race_output_targets (
    id int primary key, 
    uuid varchar,
    output_id int,
    remote_target_id int,
    output_filename varchar
);

create table report_destinations (
    id int primary key, 
    uuid varchar,
    target_name varchar,
    protocol varchar,
    server varchar,
    base_path varchar,
    username varchar,
    password varchar,
    private_key varchar,
    remote_cert varchar,
    permit_any boolean,
    stripAccents boolean
);


create table custom_participant_attributes (
    id int,
    attribute_name varchar,
    attribute_type varchar,
    UUID varchar

);

create table custom_participant_attributes_values (
    id int,
    attribute_value varchar
);

create table participant_attributes (
    participant_id int,
    attribute_id int,
    attribute_value varchar
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

create table course_records (
    id int,
    race_id int,
    seg_id int,
    cr_time bigint,
    cr_category varchar,
    cr_sex varchar,
    cr_age varchar,
    cr_name varchar,
    cr_note varchar,
    cr_year varchar,
    cr_city varchar,
    cr_state varchar,
    cr_country varchar
    
);

create table race_sex_groups (
    race_id int,
    handling_method varchar
);

create table race_sex_group_code_map (
    race_id int,
    code varchar,
    display varchar
);

commit;
