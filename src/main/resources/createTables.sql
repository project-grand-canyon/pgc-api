CREATE DATABASE core;
USE core;

-- note that order is important due to fk constraints
DROP TABLE IF EXISTS `requests`, `reminders`, `reminder_history`, `themes`, `district_offices`, `calls`, `callers_contact_methods`, `callers`, `call_targets`, `admins_districts`, `admins`, `districts`, `addresses`;


-- Create syntax for TABLE 'admins'
CREATE TABLE `admins` (
  `admin_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(20) NOT NULL,
  `token` varchar(100) NOT NULL,
  `is_root` tinyint(1) DEFAULT '0',
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `login_enabled` tinyint(1) DEFAULT '0',
  `email` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `user_name` (`user_name`)
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'districts'
CREATE TABLE `districts` (
  `district_id` int(11) NOT NULL AUTO_INCREMENT,
  `state` varchar(2) NOT NULL,
  `district_number` int(11) NOT NULL,
  `info` varchar(512) DEFAULT NULL,
  `last_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rep_first_name` varchar(50) DEFAULT NULL,
  `rep_last_name` varchar(50) DEFAULT NULL,
  `rep_image_url` varchar(512) DEFAULT NULL,
  `script_modified_time` timestamp NULL DEFAULT NULL,
  `status` enum('active','covid_paused') NOT NULL DEFAULT 'active',
  PRIMARY KEY (`district_id`),
  UNIQUE KEY `state` (`state`,`district_number`)
) ENGINE=InnoDB AUTO_INCREMENT=959 DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'admins_districts'
CREATE TABLE `admins_districts` (
  `admin_id` int(11) NOT NULL,
  `district_id` int(11) NOT NULL,
  KEY `admin_id` (`admin_id`),
  KEY `district_id` (`district_id`),
  CONSTRAINT `admins_districts_ibfk_1` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`admin_id`) ON DELETE CASCADE,
  CONSTRAINT `admins_districts_ibfk_2` FOREIGN KEY (`district_id`) REFERENCES `districts` (`district_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'call_targets'
CREATE TABLE `call_targets` (
  `district_id` int(11) NOT NULL,
  `target_district_id` int(11) NOT NULL,
  `percentage` int(11) NOT NULL,
  KEY `district_id` (`district_id`),
  KEY `target_district_id` (`target_district_id`),
  CONSTRAINT `call_targets_ibfk_1` FOREIGN KEY (`district_id`) REFERENCES `districts` (`district_id`),
  CONSTRAINT `call_targets_ibfk_2` FOREIGN KEY (`target_district_id`) REFERENCES `districts` (`district_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'callers'
CREATE TABLE `callers` (
  `caller_id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(128) NOT NULL,
  `last_name` varchar(128) NOT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `district_id` int(11) NOT NULL,
  `zipcode` varchar(10) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `paused` tinyint(1) NOT NULL,
  `ccl_id` varchar(48) DEFAULT NULL,
  `referrer` varchar(48) DEFAULT NULL,
  PRIMARY KEY (`caller_id`),
  UNIQUE KEY `contact_method` (`phone`),
  UNIQUE KEY `contact_method_2` (`email`),
  KEY `district_id` (`district_id`),
  CONSTRAINT `callers_ibfk_1` FOREIGN KEY (`district_id`) REFERENCES `districts` (`district_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1480 DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'callers_contact_methods'
CREATE TABLE `callers_contact_methods` (
  `caller_id` int(11) NOT NULL,
  `contact_method` enum('email','sms') NOT NULL,
  KEY `caller_id` (`caller_id`),
  CONSTRAINT `callers_contact_methods_ibfk_1` FOREIGN KEY (`caller_id`) REFERENCES `callers` (`caller_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'calls'
CREATE TABLE `calls` (
  `caller_id` int(11) DEFAULT NULL,
  `month` int(11) DEFAULT NULL,
  `year` int(11) DEFAULT NULL,
  `district_id` int(11) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `caller_id` (`caller_id`),
  KEY `district_id` (`district_id`),
  CONSTRAINT `calls_ibfk_1` FOREIGN KEY (`caller_id`) REFERENCES `callers` (`caller_id`),
  CONSTRAINT `calls_ibfk_2` FOREIGN KEY (`district_id`) REFERENCES `districts` (`district_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'district_offices'
CREATE TABLE `district_offices` (
  `district_office_id` int(11) NOT NULL AUTO_INCREMENT,
  `district_id` int(11) NOT NULL,
  `phone` varchar(50) NOT NULL,
  `address_line1` varchar(120) NOT NULL,
  `address_line2` varchar(120) DEFAULT NULL,
  `city` varchar(100) NOT NULL,
  `state` varchar(2) NOT NULL,
  `country` char(2) NOT NULL,
  `zipcode` varchar(10) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `opens_at` time DEFAULT NULL,
  `closes_at` time DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`district_office_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2210 DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'themes'
CREATE TABLE `themes` (
  `theme_id` int(11) NOT NULL AUTO_INCREMENT,
  `theme_name` varchar(128) NOT NULL,
  `last_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`theme_id`),
  UNIQUE KEY `theme_name` (`theme_name`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'reminder_history'
CREATE TABLE `reminder_history` (
  `caller_id` int(11) NOT NULL,
  `caller_district_id` int(11) NOT NULL,
  `target_district_id` int(11) NOT NULL,
  `time_sent` datetime DEFAULT NULL,
  `tracking_id` varchar(32) DEFAULT NULL,
  `email_delivered` tinyint(1) DEFAULT NULL,
  `sms_delivered` tinyint(1) DEFAULT NULL,
  KEY `caller_district_id` (`caller_district_id`),
  KEY `target_district_id` (`target_district_id`),
  CONSTRAINT `reminder_history_ibfk_1` FOREIGN KEY (`caller_district_id`) REFERENCES `districts` (`district_id`),
  CONSTRAINT `reminder_history_ibfk_2` FOREIGN KEY (`target_district_id`) REFERENCES `districts` (`district_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'reminders'
CREATE TABLE `reminders` (
  `caller_id` int(11) NOT NULL,
  `day_of_month` int(11) NOT NULL,
  `last_reminder_timestamp` datetime DEFAULT NULL,
  `second_reminder_timestamp` datetime DEFAULT NULL,
  `tracking_id` varchar(32) DEFAULT NULL,
  `reminder_year` int(11) DEFAULT NULL,
  `reminder_month` int(11) DEFAULT NULL,
  PRIMARY KEY (`caller_id`),
  CONSTRAINT `reminders_ibfk_1` FOREIGN KEY (`caller_id`) REFERENCES `callers` (`caller_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'requests'
CREATE TABLE `requests` (
  `request_id` int(11) NOT NULL AUTO_INCREMENT,
  `content` varchar(512) NOT NULL,
  `district_id` int(11) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  KEY `district_id` (`district_id`),
  CONSTRAINT `requests_ibfk_1` FOREIGN KEY (`district_id`) REFERENCES `districts` (`district_id`)
) ENGINE=InnoDB AUTO_INCREMENT=904 DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'reset_tokens'
CREATE TABLE `reset_tokens` (
  `token` varchar(32) NOT NULL,
  `expiration` datetime NOT NULL,
  `admin_id`  int(11) NOT NULL,
  CONSTRAINT `reset_tokens_ibfk_1` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
