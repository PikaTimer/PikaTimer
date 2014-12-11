create table event (id int primary key, event_name varchar(2048), event_date date);

create table race (race_id int auto_increment primary key, race_name varchar(2048), race_date date); 
create table race_waves (race_id int primary key, race_wave_name varchar(2048), wave_start_time int, wave_assignment varchar(32), wave_assignment_attr1 varchar(64), wave_assignment_attr2 varchar(64)); 
create table race_participants (race_id int, participant_id int); 
create table race_split (split_id int primary key, race_id int, timing_loc_id int, split_seq_number int, split_distance int, split_pace_report char, split_name varchar(256), short_name varchar(256), cutoff_time bigint);

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

create table timing_location (timing_loc_id int auto_increment primary key, timing_loc_name varchar(256)); 
create table timing_location_details (timing_loc_id int primary key, timing_loc_attr varchar(256), timing_loc_value varchar(256));

create table participant_custom (particpant_id int, part_attr varchar(128), part_value varchar(128));
create table participant_custom_defs (part_custom_attr varchar(128) primary key, part_custom_type varchar(64), allowable_values array); 

create table bib2chip (bib_id varchar(128) primary key, chip_id varchar(1024) unique);

create table timing_data (timing_loc_id int, chip_id varchar(1024), chip_time bigint); 

create table results (race_id int, participant_id int, split_id int, time bigint); 

commit;
