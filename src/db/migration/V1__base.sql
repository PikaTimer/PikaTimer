create table event (
    id int primary key, 
    event_name varchar(2048), 
    event_date date
);

create table race (
    race_id int auto_increment primary key, 
    race_name varchar(2048), 
    race_distance numeric(9,3),
    race_dist_unit varchar(16),
    race_start_time varchar(16),
    race_bib_start varchar(128),
    race_bib_end varchar(128),
    race_cutoff varchar(16),
    race_relay boolean
); 

create table race_waves (
    id int primary key,
    race_id int, 
    wave_name varchar(2048), 
    wave_start_time varchar(16), 
    wave_max_start_time varchar(16),
    wave_assignment varchar(16), 
    wave_assignment_attr1 varchar(128), 
    wave_assignment_attr2 varchar(128)
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
    split_distance numeric (9,3),
    split_dist_unit varchar(16), 
    split_pace_unit varchar(16), 
    split_name varchar(256), 
    short_name varchar(256), 
    cutoff_time varchar(16)
);

create table participant ( 
    participant_id int auto_increment primary key, 
    first_name varchar(2048), 
    last_name varchar(2048), 
    age int, 
    birthday date, 
    bib_number varchar(128), 
    sex varchar(64), 
    city varchar(1024), 
    state varchar(32), 
    country varchar(256), 
    email varchar(1024)
);

create table timing_location (
    timing_loc_id int auto_increment primary key, 
    timing_loc_name varchar(256)
);
 
create table timing_location_details (timing_loc_id int primary key, timing_loc_attr varchar(256), timing_loc_value varchar(256));

create table participant_custom (particpant_id int, part_attr varchar(128), part_value varchar(128));
create table participant_custom_defs (part_custom_attr varchar(128) primary key, part_custom_type varchar(64), allowable_values array); 

create table bib2chip (bib_id varchar(128) primary key, chip_id varchar(1024) unique);

create table timing_data (timing_loc_id int, chip_id varchar(1024), chip_time bigint); 

create table results (race_id int, participant_id int, split_id int, time bigint); 

commit;
