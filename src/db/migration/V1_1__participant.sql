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
 * Created: Sep 7, 2017
 */


/* Participant   */
/* Unused table */
drop table participant_attributes;


create table custom_participant_attributes (
    id int,
    attribute_name varchar,
    attribute_type varchar
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

commit;