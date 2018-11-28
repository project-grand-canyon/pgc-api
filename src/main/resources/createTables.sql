CREATE TABLE districts (district_id INT NOT NULL AUTO_INCREMENT,
state VARCHAR(2) NOT NULL,
district_number INT NOT NULL,
representative VARCHAR(128),
info VARCHAR(512),
PRIMARY KEY (district_id) )

CREATE TABLE callers (caller_id INT NOT NULL AUTO_INCREMENT,
first_name VARCHAR(128) NOT NULL,
last_name VARCHAR(128) NOT NULL,
contact_method ENUM ('email', 'sms') NOT NULL,
phone VARCHAR(50),
email VARCHAR(100),
district_id INT NOT NULL,
zipcode VARCHAR(10),
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE (contact_method, phone),
UNIQUE (contact_method, email),
PRIMARY KEY (caller_id),
FOREIGN KEY (district_id) REFERENCES districts (district_id)  )

CREATE TABLE themes (theme_id INT NOT NULL AUTO_INCREMENT,
theme_name VARCHAR(128) NOT NULL,
PRIMARY KEY (theme_id) )

CREATE TABLE talking_points (talking_point_id INT NOT NULL AUTO_INCREMENT,
content VARCHAR(512) NOT NULL,
theme_id INT NOT NULL,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (talking_point_id),
FOREIGN KEY (theme_id) REFERENCES themes (theme_id) )

