
-- Java -> SQL types used
-- Integer - int (ID's, list indexes, etc)
-- Strings - varchar
-- Duration -- bigint (in nanoseconds)
-- LocalTime / LocalDate / LocalDateTime -- varchar (use toString() and .parse() to store/retrieve)
-- Boolean - boolean 
-- BigDecimal - numeric (for race distances and numbers that require exact precision, like 3.140 vs 3.13999999999999)


create table event (
    id int primary key, 
    event_name varchar, 
    event_date varchar
);

create table race (
    race_id int primary key, 
    race_name varchar, 
    race_distance numeric,
    race_dist_unit varchar,
    --race_start_time varchar,
    race_bib_start varchar,
    race_bib_end varchar,
    race_cutoff bigint,
    race_relay boolean
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

create table race_participants (
    race_id int, 
    participant_id int
); 

create table race_split (
    split_id int primary key, 
    race_id int, 
    timing_loc_id int, 
    split_seq_number int, 
    split_distance numeric,
    split_dist_unit varchar, 
    split_pace_unit varchar, 
    split_name varchar, 
    short_name varchar, 
    cutoff_time bigint
);

create table participant ( 
    participant_id int primary key, 
    first_name varchar, 
    last_name varchar, 
    age int, 
    birthday varchar, 
    bib_number varchar, 
    sex varchar, 
    city varchar, 
    state varchar, 
    country varchar, 
    email varchar
);

create table timing_location (
    timing_location_id int primary key, 
    timing_location_name varchar
);
 
-- create table timing_location_details (timing_loc_id int primary key, timing_loc_attr varchar, timing_loc_value varchar);
-- 
-- create table participant_custom (particpant_id int, part_attr varchar, part_value varchar);
-- create table participant_custom_defs (part_custom_attr varchar primary key, part_custom_type varchar, allowable_values array); 

create table bib2chip (bib_id varchar, chip_id varchar unique);

create table part2wave (participant_id int, wave_id int); 

create table timing_location_input (
    id int primary key,
    timing_location_id int,
    time_skew bigint,
    timing_location_type varchar,
    backup boolean
);
 
create table raw_timing_data (timing_loc_id int, chip_id varchar, chip_time bigint); 

create table cooked_timing_data (time_loc_id int, bib_id varchar, time bigint, backup_time boolean);
 
create table results (race_id int, participant_id int, split_id int, time bigint); 

commit;
