-- MySQL dump 10.13  Distrib 5.7.30, for Linux (x86_64)
--
-- Host: localhost    Database: core
-- ------------------------------------------------------
-- Server version	5.7.30

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `admins`
--

LOCK TABLES `admins` WRITE;
/*!40000 ALTER TABLE `admins` DISABLE KEYS */;
INSERT INTO `admins` VALUES (122, 'admin', 'OVrhTP2wUe1S8UBKZv9cCr_uVa3ZeSRKEc6RXLSm_HI', 1, '2020-08-15 04:10:01', '2020-08-15 04:10:01', 1, NULL, NULL),
                            (123, 'bAdmin', '1BbNjVv-1vi90hp-ORPBukSHyK6HuzIXuPk9qWLT1VI', 0, '2020-08-15 04:10:16', '2020-08-15 04:10:16', 1, 'testAlert@pgc.com', NULL);
/*!40000 ALTER TABLE `admins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `admins_districts`
--

LOCK TABLES `admins_districts` WRITE;
/*!40000 ALTER TABLE `admins_districts` DISABLE KEYS */;
INSERT INTO `admins_districts` VALUES (123, 965);
/*!40000 ALTER TABLE `admins_districts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `call_targets`
--

LOCK TABLES `call_targets` WRITE;
/*!40000 ALTER TABLE `call_targets` DISABLE KEYS */;
INSERT INTO `call_targets` VALUES   (959, 959, 20),
                                    (959, 962, 40),
                                    (959, 964, 40),
                                    (960, 960, 30),
                                    (960, 962, 30),
                                    (960, 964, 40),
                                    (961, 959, 20),
                                    (961, 961, 80),
                                    (962, 959, 20),
                                    (962, 962, 40),
                                    (962, 964, 40),
                                    (963, 959, 20),
                                    (963, 960, 10),
                                    (963, 962, 40),
                                    (963, 963, 30),
                                    (964, 964, 100),
                                    (965, 965, 20),
                                    (965, 961, 40),
                                    (965, 962, 40),
                                    (966, 965, 10),
                                    (966, 967, 90),
                                    (967, 965, 20),
                                    (967, 967, 80),
                                    (968, 968, 100),
                                    (969, 969, 20),
                                    (969, 970, 40),
                                    (969, 971, 40),
                                    (970, 969, 20),
                                    (970, 970, 40),
                                    (970, 971, 40),
                                    (971, 969, 100);
/*!40000 ALTER TABLE `call_targets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `callers`
--

LOCK TABLES `callers` WRITE;
/*!40000 ALTER TABLE `callers` DISABLE KEYS */;
INSERT INTO `callers` VALUES    (1480, 'Caller1FirstName', 'Caller1LastName', '111-111-1111', 'caller1@example.net', 959, '11111', '2021-03-21 13:10:16', '2021-03-21 13:10:16', 1, NULL, NULL, NULL),
                                (1481, 'Caller2FirstName', 'Caller2LastName', '111-111-1112', 'caller2@example.net', 959, '11112', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 1, NULL, NULL, NULL),
                                (1482, 'Caller3FirstName', 'Caller3LastName', '111-111-1113', 'caller3@example.net', 959, '11113', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1483, 'Caller4FirstName', 'Caller4LastName', '111-111-1114', 'caller4@example.net', 960, '11121', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1484, 'Caller5FirstName', 'Caller5LastName', '111-111-1115', 'caller5@example.net', 960, '11122', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1485, 'Caller6FirstName', 'Caller6LastName', '111-111-1116', 'caller6@example.net', 961, '11131', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1486, 'Caller7FirstName', 'Caller7LastName', '111-111-1117', 'caller7@example.net', 962, '11141', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 1, NULL, NULL, NULL),
                                (1487, 'Caller8FirstName', 'Caller8LastName', '111-111-1118', 'caller8@example.net', 962, '11142', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1488, 'Caller9FirstName', 'Caller9LastName', '111-111-1119', 'caller9@example.net', 962, '11143', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1489, 'Caller10FirstName', 'Caller10LastName', '111-111-1010', 'caller10@example.net', 962, '11143', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1490, 'Caller11FirstName', 'Caller11LastName', '111-111-1011', 'caller11@example.net', 962, '11145', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 1, NULL, NULL, NULL),
                                (1491, 'Caller12FirstName', 'Caller12LastName', '111-111-1012', 'caller12@example.net', 965, '22211', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1492, 'Caller13FirstName', 'Caller13LastName', '111-111-1013', 'caller13@example.net', 965, '22212', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 0, NULL, NULL, NULL),
                                (1493, 'Caller14FirstName', 'Caller14LastName', '111-111-1014', 'caller14@example.net', 967, '22213', '2020-08-15 04:10:16', '2020-08-15 04:10:16', 1, NULL, NULL, NULL);
/*!40000 ALTER TABLE `callers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `callers_contact_methods`
--

LOCK TABLES `callers_contact_methods` WRITE;
/*!40000 ALTER TABLE `callers_contact_methods` DISABLE KEYS */;
INSERT INTO `callers_contact_methods` VALUES    (1480, 'email'),
                                                (1480, 'sms'),
                                                (1481, 'email'),
                                                (1481, 'sms'),
                                                (1482, 'email'),
                                                (1483, 'email'),
                                                (1484, 'email'),
                                                (1484, 'sms'),
                                                (1485, 'email'),
                                                (1485, 'sms'),
                                                (1486, 'email'),
                                                (1486, 'sms'),
                                                (1487, 'email'),
                                                (1487, 'sms'),
                                                (1488, 'email'),
                                                (1488, 'sms'),
                                                (1489, 'email'),
                                                (1489, 'sms'),
                                                (1490, 'email'),
                                                (1490, 'sms'),
                                                (1491, 'email'),
                                                (1491, 'sms'),
                                                (1492, 'email'),
                                                (1492, 'sms'),
                                                (1493, 'email'),
                                                (1493, 'sms');
/*!40000 ALTER TABLE `callers_contact_methods` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `calls`
--

LOCK TABLES `calls` WRITE;
/*!40000 ALTER TABLE `calls` DISABLE KEYS */;
/*!40000 ALTER TABLE `calls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `district_offices`
--

LOCK TABLES `district_offices` WRITE;
/*!40000 ALTER TABLE `district_offices` DISABLE KEYS */;
INSERT INTO `district_offices` VALUES   (300, 959, '111-222-1111', 'AA1MainOfficeAddressLine1', 'AA1MainOfficeAddressLine2', 'Washington', 'DC', 'US', '11111', 'AA1office@OfficeOfAARep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (301, 960, '111-222-1112', 'AA2MainOfficeAddressLine1', 'AA2MainOfficeAddressLine2', 'Washington', 'DC', 'US', '11111', 'AA2office@OfficeOfAARep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (302, 961, '111-222-1113', 'AA3MainOfficeAddressLine1', 'AA3MainOfficeAddressLine2', 'Washington', 'DC', 'US', '11111', 'AA3office@OfficeOfAARep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (303, 962, '111-222-1114', 'AA4MainOfficeAddressLine1', 'AA4MainOfficeAddressLine2', 'Washington', 'DC', 'US', '11111', 'AA4office@OfficeOfAARep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (304, 963, '111-222-1115', 'AA5MainOfficeAddressLine1', 'AA5MainOfficeAddressLine2', 'Washington', 'DC', 'US', '11111', 'AA5office@OfficeOfAARep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (305, 964, '111-222-1116', 'AA6MainOfficeAddressLine1', 'AA6MainOfficeAddressLine2', 'Washington', 'DC', 'US', '11111', 'AA6office@OfficeOfAARep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (306, 965, '111-222-1117', 'BB1MainOfficeAddressLine1', 'BB1MainOfficeAddressLine2', 'Washington', 'DC', 'US', '22222', 'BB1office@OfficeOfBBRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (307, 966, '111-222-1118', 'BB2MainOfficeAddressLine1', 'BB2MainOfficeAddressLine2', 'Washington', 'DC', 'US', '22222', 'BB2office@OfficeOfBBRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (308, 967, '111-222-1119', 'BB3MainOfficeAddressLine1', 'BB3MainOfficeAddressLine2', 'Washington', 'DC', 'US', '22222', 'BB3office@OfficeOfBBRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (309, 968, '111-222-1120', 'BB4MainOfficeAddressLine1', 'BB4MainOfficeAddressLine2', 'Washington', 'DC', 'US', '22222', 'BB4office@OfficeOfBBRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (310, 969, '111-222-1121', 'CC1MainOfficeAddressLine1', 'CC1MainOfficeAddressLine2', 'Washington', 'DC', 'US', '33333', 'CC1office@OfficeOfCCRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (311, 970, '111-222-1122', 'CC2MainOfficeAddressLine1', 'CC2MainOfficeAddressLine2', 'Washington', 'DC', 'US', '33333', 'CC2office@OfficeOfCCRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14'),
                                        (312, 971, '111-222-1123', 'CC3MainOfficeAddressLine1', 'CC3MainOfficeAddressLine2', 'Washington', 'DC', 'US', '33333', 'CC3office@OfficeOfCCRep.net', '09:00:00', '17:00:00', '2020-08-15 04:10:14', '2020-08-15 04:10:14');
/*!40000 ALTER TABLE `district_offices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `district_scripts`
--

LOCK TABLES `district_scripts` WRITE;
/*!40000 ALTER TABLE `district_scripts` DISABLE KEYS */;
INSERT INTO `district_scripts` VALUES   (959, 92, 1),
                                        (960, 92, 2),
                                        (961, 92, 3),
                                        (962, 92, 4),
                                        (963, 92, 5),
                                        (964, 92, 6),
                                        (965, 92, 7),
                                        (966, 92, 8),
                                        (967, 92, 9),
                                        (968, 92, 10),
                                        (969, 92, 11),
                                        (970, 92, 12),
                                        (971, 92, 13);
/*!40000 ALTER TABLE `district_scripts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `districts`
--

LOCK TABLES `districts` WRITE;
/*!40000 ALTER TABLE `districts` DISABLE KEYS */;
INSERT INTO `districts` VALUES  (959, 'AA', 1, 'AA1Info', '2020-08-15 04:10:14', '2020-08-15 04:10:14', 'AA1FirstName', 'AA1LastName', NULL, NULL, '2020-08-15 04:10:14', 'active', 'US/Eastern', 0),
                                (960, 'AA', 2, 'AA2Info', '2020-08-15 04:10:14', '2020-08-15 04:10:14', 'AA2FirstName', 'AA2LastName', NULL, NULL, '2020-08-15 04:10:14', 'active', 'US/Eastern', 0),
                                (961, 'AA', 3, 'AA3Info', '2020-08-15 04:10:14', '2020-08-15 04:10:14', 'AA3FirstName', 'AA3LastName', NULL, NULL, '2020-08-15 04:10:14', 'active', 'US/Eastern', 0),
                                (962, 'AA', 4, 'AA4Info', '2020-08-15 04:10:14', '2020-08-15 04:10:14', 'AA4FirstName', 'AA4LastName', NULL, NULL, '2020-08-15 04:10:14', 'active', 'US/Eastern', 0),
                                (963, 'AA', 5, 'AA5Info', '2020-08-15 04:10:15', '2020-08-15 04:10:14', 'AA1FirstName', 'AA1LastName', NULL, NULL, '2020-08-15 04:10:14', 'active', 'US/Eastern', 0),
                                (964, 'AA', 6, 'AA6Info', '2020-08-15 04:10:14', '2020-08-15 04:10:14', 'AA6FirstName', 'AA6LastName', NULL, NULL, '2020-08-15 04:10:14', 'active', 'US/Eastern', 0),
                                (965, 'BB', 1, 'BB1Info', '2020-08-15 04:10:15', '2020-08-15 04:10:14', 'BB1FirstName', 'BB1LastName', NULL, NULL, '2020-08-15 04:10:14', 'covid_paused', 'US/Eastern', 0),
                                (966, 'BB', 2, 'BB2Info', '2020-08-15 04:10:15', '2020-08-15 04:10:15', 'BB2FirstName', 'BB2LastName', NULL, NULL, '2020-08-15 04:10:15', 'active', 'US/Eastern', 0),
                                (967, 'BB', 3, 'BB3Info', '2020-08-15 04:10:15', '2020-08-15 04:10:15', 'BB3FirstName', 'BB3LastName', NULL, NULL, '2020-08-15 04:10:15', 'covid_paused', 'US/Eastern', 0),
                                (968, 'BB', 4, 'BB4Info', '2020-08-15 04:10:15', '2020-08-15 04:10:15', 'BB4FirstName', 'BB4LastName', NULL, NULL, '2020-08-15 04:10:15', 'active', 'US/Eastern', 0),
                                (969, 'CC', 1, 'CC1Info', '2020-08-15 04:10:15', '2020-08-15 04:10:15', 'CC1FirstName', 'CC1LastName', NULL, NULL, '2020-08-15 04:10:15', 'covid_paused', 'US/Eastern', 0),
                                (970, 'CC', 2, 'CC2Info', '2020-08-15 04:10:15', '2020-08-15 04:10:15', 'CC2FirstName', 'CC2LastName', NULL, NULL, '2020-08-15 04:10:15', 'active', 'US/Eastern', 0),
                                (971, 'CC', 3, 'CC3Info', '2020-08-15 04:10:15', '2020-08-15 04:10:15', 'CC3FirstName', 'CC3LastName', NULL, NULL, '2020-08-15 04:10:15', 'active', 'US/Eastern', 0);
/*!40000 ALTER TABLE `districts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `reminder_history`
--

LOCK TABLES `reminder_history` WRITE;
/*!40000 ALTER TABLE `reminder_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `reminder_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `reminders`
--

LOCK TABLES `reminders` WRITE;
/*!40000 ALTER TABLE `reminders` DISABLE KEYS */;
INSERT INTO `reminders` VALUES  (1480, 21, NULL, NULL, NULL, NULL, NULL),
                                (1481, 22, NULL, NULL, NULL, NULL, NULL),
                                (1482, 23, NULL, NULL, NULL, NULL, NULL),
                                (1483, 24, NULL, NULL, NULL, NULL, NULL),
                                (1484, 5, NULL, NULL, NULL, NULL, NULL),
                                (1485, 6, NULL, NULL, NULL, NULL, NULL),
                                (1486, 7, NULL, NULL, NULL, NULL, NULL),
                                (1487, 8, NULL, NULL, NULL, NULL, NULL),
                                (1488, 9, NULL, NULL, NULL, NULL, NULL),
                                (1489, 10, NULL, NULL, NULL, NULL, NULL),
                                (1490, 11, NULL, NULL, NULL, NULL, NULL),
                                (1491, 12, NULL, NULL, NULL, NULL, NULL),
                                (1492, 13, NULL, NULL, NULL, NULL, NULL),
                                (1493, 14, NULL, NULL, NULL, NULL, NULL);
/*!40000 ALTER TABLE `reminders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `requests`
--

LOCK TABLES `requests` WRITE;
/*!40000 ALTER TABLE `requests` DISABLE KEYS */;
INSERT INTO `requests` VALUES     (1, "This is a request for district 959", 959, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (2, "This is a request for district 960", 960, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (3, "This is a request for district 961", 961, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (4, "This is a request for district 962", 962, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (5, "This is a request for district 963", 963, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (6, "This is a request for district 964", 964, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (7, "This is a request for district 965", 965, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (8, "This is a request for district 966", 966, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (9, "This is a request for district 967", 967, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (10, "This is a request for district 968", 968, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (11, "This is a request for district 969", 969, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (12, "This is a request for district 970", 970, '2020-08-15 04:10:15', '2020-08-15 04:10:15'),
                                        (13, "This is a request for district 971", 971, '2020-08-15 04:10:15', '2020-08-15 04:10:15');
/*!40000 ALTER TABLE `requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `reset_tokens`
--

LOCK TABLES `reset_tokens` WRITE;
/*!40000 ALTER TABLE `reset_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `talking_points`
--

LOCK TABLES `talking_points` WRITE;
/*!40000 ALTER TABLE `talking_points` DISABLE KEYS */;
INSERT INTO `talking_points` VALUES (92, 'BB1 Talking points', 14, '2020-08-15 04:10:17', '2020-08-15 04:10:17', 1, 'district', 123, 'http://BB1tp1.stuff', 'passed'),
                                    (93, 'BB1 Talking Point 2', 14, '2020-08-15 04:10:17', '2020-08-15 04:10:17', 1, 'district', 123, 'http://BB1tp1.stuff', 'passed');
/*!40000 ALTER TABLE `talking_points` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `talking_points_scopes`
--

LOCK TABLES `talking_points_scopes` WRITE;
/*!40000 ALTER TABLE `talking_points_scopes` DISABLE KEYS */;
INSERT INTO `talking_points_scopes` VALUES  (92, 965, NULL),
                                            (93, 965, NULL),
                                            (92, 959, NULL),
                                            (93, 962, NULL),
                                            (92, 964, NULL);
/*!40000 ALTER TABLE `talking_points_scopes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `themes`
--

LOCK TABLES `themes` WRITE;
/*!40000 ALTER TABLE `themes` DISABLE KEYS */;
INSERT INTO `themes` VALUES (14, 'This is the theme', '2020-08-15 04:10:16', '2020-08-15 04:10:16');
/*!40000 ALTER TABLE `themes` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-08-15  4:10:17
