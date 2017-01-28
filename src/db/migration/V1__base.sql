
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
    value varchar
);

create table race (
    race_id int primary key, 
    race_name varchar, 
    race_distance numeric,
    race_dist_unit varchar,
    race_bib_start varchar,
    race_bib_end varchar,
    race_cutoff bigint,
    race_relay boolean,
    uuid varchar
); 

create table race_attributes (
    race_id int, 
    index_id int, 
    attribute varchar, 
    value varchar
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
    value varchar
);

create table race_age_groups (
    race_id int,
    ag_increment int,
    masters_start int,
    ag_start int
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
    min_time bigint,
    cutoff_time bigint
);

create table race_segment (
    segment_id int primary key, 
    race_id int, 
    start_split_id int, 
    end_split_id int,
    segment_name varchar,
    pace_unit varchar
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

create table participant_attributes (part_id int, index_id int, attribute varchar, value varchar); 

create table timing_location (
    timing_location_id int primary key, 
    timing_location_name varchar,
    filterStartDuration bigint, 
    filterEndDuration bigint,
    AUTO_ASSIGN_TO_RACE_ID int
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
    backup boolean
);
 
create table timing_location_input_attributes (
    index_id int, 
    tli_id int, 
    attribute varchar, 
    value varchar
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
    relative_to_start boolean
); 

create table race_outputs (
    id int primary key, 
    uuid varchar,
    race_id int,
    output_type varchar
);

create table race_output_attributes (
    id int, 
    output_id int, 
    attribute varchar, 
    value varchar
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

commit;
