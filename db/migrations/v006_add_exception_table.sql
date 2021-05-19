CREATE TABLE `exceptions` (
  `exception_id` int(11) NOT NULL AUTO_INCREMENT,
  `is_resolved` enum(`resolved`, `ignored`, `unresolved`) DEFAULT `unresolved` NOT NULL,
  `exception_code` int(11) NOT NULL,
  `first_name` varchar(128) NOT NULL,
  `last_name` varchar(128) NOT NULL,
  `emial` varchar(128) DEFAULT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `time_reported` datetime DEFAULT CURRENT_TIMESTAMP,
  `time_processed` datetime DEFAULT NULL,
  PRIMARY KEY `exception_id`,
  CONSTRAINT `check_contacts` CHECK NOT (`emial` == NULL AND `phone` == NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `exception_codes` (
    `exception_code` int(11) NOT NULL AUTO_INCREMENT,
    `exception_name` varchar(128) NOT NULL,
    `last_modified` datatime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)