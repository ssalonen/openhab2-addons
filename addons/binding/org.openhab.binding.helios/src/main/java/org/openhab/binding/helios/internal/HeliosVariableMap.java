/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.helios.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openhab.binding.helios.HeliosBindingConstants;

/**
 * This class represents a the possible variables of the Helios modbus.
 *
 * @author Bernhard Bauer - Initial contribution
 */
public class HeliosVariableMap {

    /**
     * The map holding the variable meta info
     */
    private Map<String, HeliosVariable> vMap;

    /**
     * Constructor to generate the variable map
     */
    public HeliosVariableMap() {
        String[] descriptions;
        this.vMap = new HashMap<String, HeliosVariable>();

        this.vMap.put(HeliosBindingConstants.ARTICLE_DESCRIPTION, new HeliosVariable(0, HeliosVariable.ACCESS_RW, 31, 20, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.REF_NO, new HeliosVariable(1, HeliosVariable.ACCESS_RW, 16, 12, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.MAC_ADDRESS, new HeliosVariable(2, HeliosVariable.ACCESS_R, 18, 13, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.LANGUAGE, new HeliosVariable(3, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.DATE, new HeliosVariable(4, HeliosVariable.ACCESS_RW, 10, 9, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.TIME, new HeliosVariable(5, HeliosVariable.ACCESS_RW, 10, 9, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.SUMMER_WINTER, new HeliosVariable(6, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER));
        this.vMap.put(HeliosBindingConstants.AUTO_SW_UPDATE, new HeliosVariable(7, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER));
        this.vMap.put(HeliosBindingConstants.ACCESS_HELIOS_PORTAL, new HeliosVariable(8, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER));

        descriptions = new String[] {
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_1_EXTRACT_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_2_EXTRACT_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_3_EXTRACT_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_4_EXTRACT_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_1_SUPPLY_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_2_SUPPLY_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_3_SUPPLY_AIR,
                HeliosBindingConstants.VOLTAGE_FAN_STAGE_4_SUPPLY_AIR
        };
        for (int i = 12; i <= 19; i++) {
            this.vMap.put(descriptions[i - 12], new HeliosVariable(i, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_FLOAT, 1.6, 10.0));
        }

        this.vMap.put(HeliosBindingConstants.MIN_FAN_STAGE, new HeliosVariable(20, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        this.vMap.put(HeliosBindingConstants.KWL_BE, new HeliosVariable(21, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        this.vMap.put(HeliosBindingConstants.KWL_BEC, new HeliosVariable(22, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        this.vMap.put(HeliosBindingConstants.UNIT_CONFIG, new HeliosVariable(23, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        this.vMap.put(HeliosBindingConstants.PRE_HEATER_STATUS, new HeliosVariable(24, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));

        descriptions = new String[] {
                HeliosBindingConstants.KWL_FTF_CONFIG_0,
                HeliosBindingConstants.KWL_FTF_CONFIG_1,
                HeliosBindingConstants.KWL_FTF_CONFIG_2,
                HeliosBindingConstants.KWL_FTF_CONFIG_3,
                HeliosBindingConstants.KWL_FTF_CONFIG_4,
                HeliosBindingConstants.KWL_FTF_CONFIG_5,
                HeliosBindingConstants.KWL_FTF_CONFIG_6,
                HeliosBindingConstants.KWL_FTF_CONFIG_7
        };
        for (int i = 25; i <= 32; i++) {
            this.vMap.put(descriptions[i - 25], new HeliosVariable(i, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 3));
        }

        this.vMap.put(HeliosBindingConstants.HUMIDITY_CONTROL_STATUS, new HeliosVariable(33, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 2));
        this.vMap.put(HeliosBindingConstants.HUMIDITY_CONTROL_SET_VALUE, new HeliosVariable(34, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 20, 80));
        this.vMap.put(HeliosBindingConstants.HUMIDITY_CONTROL_STEPS, new HeliosVariable(35, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 5, 20));
        this.vMap.put(HeliosBindingConstants.HUMIDITY_STOP_TIME, new HeliosVariable(36, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 0, 24));

        this.vMap.put(HeliosBindingConstants.CO2_CONTROL_STATUS, new HeliosVariable(37, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 2));
        this.vMap.put(HeliosBindingConstants.CO2_CONTROL_SET_VALUE, new HeliosVariable(38, HeliosVariable.ACCESS_RW, 4, 6, HeliosVariable.TYPE_INTEGER, 300, 2000));
        this.vMap.put(HeliosBindingConstants.CO2_CONTROL_STEPS, new HeliosVariable(39, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, 50, 400));

        this.vMap.put(HeliosBindingConstants.VOC_CONTROL_STATUS, new HeliosVariable(40, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 2));
        this.vMap.put(HeliosBindingConstants.VOC_CONTROL_SET_VALUE, new HeliosVariable(41, HeliosVariable.ACCESS_RW, 4, 6, HeliosVariable.TYPE_INTEGER, 300, 2000));
        this.vMap.put(HeliosBindingConstants.VOC_CONTROL_STEPS, new HeliosVariable(42, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, 50, 400));

        this.vMap.put(HeliosBindingConstants.COMFORT_TEMP, new HeliosVariable(43, HeliosVariable.ACCESS_RW, 4, 6, HeliosVariable.TYPE_INTEGER, 10, 25));

        this.vMap.put(HeliosBindingConstants.TIME_ZONE_DIFFERENCE_TO_GMT, new HeliosVariable(51, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, -12, 14));
        this.vMap.put(HeliosBindingConstants.DATE_FORMAT, new HeliosVariable(52, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 2));

        this.vMap.put(HeliosBindingConstants.HEAT_EXCHANGER_TYPE, new HeliosVariable(53, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 3));

        this.vMap.put(HeliosBindingConstants.PARTY_MODE_DURATION, new HeliosVariable(91, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, 5, 180));
        this.vMap.put(HeliosBindingConstants.PARTY_MODE_FAN_STAGE, new HeliosVariable(92, HeliosVariable.ACCESS_RW, 3, 5, HeliosVariable.TYPE_INTEGER, 0, 4));
        this.vMap.put(HeliosBindingConstants.PARTY_MODE_REMAINING_TIME, new HeliosVariable(93, HeliosVariable.ACCESS_R, 3, 6, HeliosVariable.TYPE_INTEGER, 0, 180));
        this.vMap.put(HeliosBindingConstants.PART_MODE_STATUS, new HeliosVariable(94, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));

        this.vMap.put(HeliosBindingConstants.STANDBY_MODE_DURATION, new HeliosVariable(96, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, 5, 180));
        this.vMap.put(HeliosBindingConstants.STANDBY_MODE_FAN_STAGE, new HeliosVariable(97, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 4));
        this.vMap.put(HeliosBindingConstants.STANDBY_MODE_REMAINING_TIME, new HeliosVariable(98, HeliosVariable.ACCESS_R, 3, 6, HeliosVariable.TYPE_INTEGER, 0, 180));
        this.vMap.put(HeliosBindingConstants.STANDBY_MODE_STATUS, new HeliosVariable(99, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));

        this.vMap.put(HeliosBindingConstants.OPERATING_MODE, new HeliosVariable(101, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        this.vMap.put(HeliosBindingConstants.FAN_STAGE, new HeliosVariable(102, HeliosVariable.ACCESS_RW, 3, 5, HeliosVariable.TYPE_INTEGER, 0, 4));
        this.vMap.put(HeliosBindingConstants.PERCENTAGE_FAN_STAGE, new HeliosVariable(103, HeliosVariable.ACCESS_R, 3, 6, HeliosVariable.TYPE_INTEGER, 0, 100));

        this.vMap.put(HeliosBindingConstants.TEMPERATURE_OUTSIDE_AIR, new HeliosVariable(104, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));
        this.vMap.put(HeliosBindingConstants.TEMPERATURE_SUPPLY_AIR, new HeliosVariable(105, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));
        this.vMap.put(HeliosBindingConstants.TEMPERATURE_OUTGOING_AIR, new HeliosVariable(106, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));
        this.vMap.put(HeliosBindingConstants.TEMPERATURE_EXTRACT_AIR, new HeliosVariable(107, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));

        this.vMap.put(HeliosBindingConstants.VHZ_DUCT_SENSOR, new HeliosVariable(108, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));
        this.vMap.put(HeliosBindingConstants.NHZ_RETURN_SENSOR, new HeliosVariable(110, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));

        descriptions = new String[] {
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_1,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_2,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_3,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_4,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_5,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_6,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_7,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_HUMIDITY_8
        };
        for (int i = 111; i <= 118; i++) {
            this.vMap.put(descriptions[i - 111], new HeliosVariable(i, HeliosVariable.ACCESS_R, 4, 6, HeliosVariable.TYPE_INTEGER, 0, 9998));
        }

        descriptions = new String[] {
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_1,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_2,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_3,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_4,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_5,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_6,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_7,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_FTF_TEMPERATURE_8
        };
        for (int i = 119; i <= 126; i++) {
            this.vMap.put(descriptions[i - 119], new HeliosVariable(i, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));
        }

        descriptions = new String[] {
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_1,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_2,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_3,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_4,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_5,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_6,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_7,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_CO2_8
        };
        for (int i = 128; i <= 135; i++) {
            this.vMap.put(descriptions[i - 128], new HeliosVariable(i, HeliosVariable.ACCESS_R, 4, 6, HeliosVariable.TYPE_INTEGER, 0, 9998));
        }

        descriptions = new String[] {
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_1,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_2,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_3,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_4,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_5,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_6,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_7,
                HeliosBindingConstants.EXTERNAL_SENSOR_KWL_VOC_8
        };
        for (int i = 136; i <= 143; i++) {
            this.vMap.put(descriptions[i - 136], new HeliosVariable(i, HeliosVariable.ACCESS_R, 4, 6, HeliosVariable.TYPE_INTEGER, 0, 9998));
        }

        this.vMap.put(HeliosBindingConstants.NHZ_DUCT_SENSOR, new HeliosVariable(146, HeliosVariable.ACCESS_R, 7, 8, HeliosVariable.TYPE_INTEGER, -27, 9998));
        this.vMap.put(HeliosBindingConstants.WEEK_PROFILE_NHZ, new HeliosVariable(201, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 6));

        this.vMap.put(HeliosBindingConstants.SER_NO, new HeliosVariable(303, HeliosVariable.ACCESS_RW, 16, 12, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.PROD_CODE, new HeliosVariable(304, HeliosVariable.ACCESS_RW, 13, 11, HeliosVariable.TYPE_STRING));

        this.vMap.put(HeliosBindingConstants.SUPPLY_AIR_RPM, new HeliosVariable(348, HeliosVariable.ACCESS_R, 4, 6, HeliosVariable.TYPE_INTEGER, 0, 9999));
        this.vMap.put(HeliosBindingConstants.EXTRACT_AIR_RPM, new HeliosVariable(349, HeliosVariable.ACCESS_R, 4, 6, HeliosVariable.TYPE_INTEGER, 0, 9999));
        this.vMap.put(HeliosBindingConstants.LOGOUT, new HeliosVariable(403, HeliosVariable.ACCESS_W, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 1));

        this.vMap.put(HeliosBindingConstants.HOLIDAY_PROGRAMME, new HeliosVariable(601, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 2));
        this.vMap.put(HeliosBindingConstants.HOLIDAY_PROGRAMME_FAN_STAGE, new HeliosVariable(602, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 4));
        this.vMap.put(HeliosBindingConstants.HOLIDAY_PROGRAMME_START, new HeliosVariable(603, HeliosVariable.ACCESS_RW, 10, 9, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.HOLIDAY_PROGRAMME_END, new HeliosVariable(604, HeliosVariable.ACCESS_RW, 10, 9, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.HOLIDAY_PROGRAMME_INTERVAL, new HeliosVariable(605, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 1, 24));
        this.vMap.put(HeliosBindingConstants.HOLIDAY_PROGRAMME_ACTIVATION_TIME, new HeliosVariable(606, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, 5, 300));

        this.vMap.put(HeliosBindingConstants.VHZ_TYPE, new HeliosVariable(1010, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 4));
        this.vMap.put(HeliosBindingConstants.FUNCTION_TYPE_KWL_EM, new HeliosVariable(1017, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 2));
        this.vMap.put(HeliosBindingConstants.RUN_ON_TIME_VHZ_NHZ, new HeliosVariable(1019, HeliosVariable.ACCESS_RW, 3, 6, HeliosVariable.TYPE_INTEGER, 60, 120));

        this.vMap.put(HeliosBindingConstants.EXTERNAL_CONTACT, new HeliosVariable(1020, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 6));
        this.vMap.put(HeliosBindingConstants.ERROR_OUTPUT_FUNCTION, new HeliosVariable(1021, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 2));

        this.vMap.put(HeliosBindingConstants.FILTER_CHANGE, new HeliosVariable(1031, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        this.vMap.put(HeliosBindingConstants.FILTER_CHANGE_INTERVAL, new HeliosVariable(1032, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 0, 12));
        this.vMap.put(HeliosBindingConstants.FILTER_CHANGE_REMAINING_TIME, new HeliosVariable(1033, HeliosVariable.ACCESS_R, 10, 9, HeliosVariable.TYPE_INTEGER, 2, 4294967295L));

        this.vMap.put(HeliosBindingConstants.BYPASS_ROOM_TEMPERATURE, new HeliosVariable(1035, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 10, 40));
        this.vMap.put(HeliosBindingConstants.BYPASS_MIN_OUTSIDE_TEMPERATURE, new HeliosVariable(1036, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 5, 20));
        this.vMap.put(HeliosBindingConstants.TBD, new HeliosVariable(1037, HeliosVariable.ACCESS_RW, 2, 5, HeliosVariable.TYPE_INTEGER, 3, 10));

        this.vMap.put(HeliosBindingConstants.FACTORY_SETTING_WZU, new HeliosVariable(1041, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 1));
        this.vMap.put(HeliosBindingConstants.FACTORY_RESET, new HeliosVariable(1042, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 1));

        this.vMap.put(HeliosBindingConstants.SUPPLY_AIR_FAN_STAGE, new HeliosVariable(1050, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 4));
        this.vMap.put(HeliosBindingConstants.EXTRACT_AIR_FAN_STAGE, new HeliosVariable(1051, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 4));

        this.vMap.put(HeliosBindingConstants.FAN_STAGE_STEPPED_0TO2V, new HeliosVariable(1061, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 2));
        this.vMap.put(HeliosBindingConstants.FAN_STAGE_STEPPED_2TO4V, new HeliosVariable(1062, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 4));
        this.vMap.put(HeliosBindingConstants.FAN_STAGE_STEPPED_4TO6V, new HeliosVariable(1063, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 4));
        this.vMap.put(HeliosBindingConstants.FAN_STAGE_STEPPED_6TO8V, new HeliosVariable(1064, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 4));
        this.vMap.put(HeliosBindingConstants.FAN_STAGE_STEPPED_8TO10V, new HeliosVariable(1065, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 4));

        this.vMap.put(HeliosBindingConstants.OFFSET_EXTRACT_AIR, new HeliosVariable(1066, HeliosVariable.ACCESS_RW, 10, 9, HeliosVariable.TYPE_FLOAT));
        this.vMap.put(HeliosBindingConstants.ASSIGNMENT_FAN_STAGES, new HeliosVariable(1068, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));

        descriptions = new String[] {
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_1,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_2,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_3,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_4,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_5,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_6,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_7,
                HeliosBindingConstants.SENSOR_NAME_HUMIDITY_AND_TEMP_8
        };
        for (int i = 1071; i <= 1078; i++) {
            this.vMap.put(descriptions[i - 1071], new HeliosVariable(i, HeliosVariable.ACCESS_RW, 15, 12, HeliosVariable.TYPE_STRING));
        }

        descriptions = new String[] {
                HeliosBindingConstants.SENSOR_NAME_CO2_1,
                HeliosBindingConstants.SENSOR_NAME_CO2_2,
                HeliosBindingConstants.SENSOR_NAME_CO2_3,
                HeliosBindingConstants.SENSOR_NAME_CO2_4,
                HeliosBindingConstants.SENSOR_NAME_CO2_5,
                HeliosBindingConstants.SENSOR_NAME_CO2_6,
                HeliosBindingConstants.SENSOR_NAME_CO2_7,
                HeliosBindingConstants.SENSOR_NAME_CO2_8
        };
        for (int i = 1081; i <= 1088; i++) {
            this.vMap.put(descriptions[i - 1081], new HeliosVariable(i, HeliosVariable.ACCESS_RW, 15, 12, HeliosVariable.TYPE_STRING));
        }

        descriptions = new String[] {
                HeliosBindingConstants.SENSOR_NAME_VOC_1,
                HeliosBindingConstants.SENSOR_NAME_VOC_2,
                HeliosBindingConstants.SENSOR_NAME_VOC_3,
                HeliosBindingConstants.SENSOR_NAME_VOC_4,
                HeliosBindingConstants.SENSOR_NAME_VOC_5,
                HeliosBindingConstants.SENSOR_NAME_VOC_6,
                HeliosBindingConstants.SENSOR_NAME_VOC_7,
                HeliosBindingConstants.SENSOR_NAME_VOC_8
        };
        for (int i = 1091; i <= 1098; i++) {
            this.vMap.put(descriptions[i - 1091], new HeliosVariable(i, HeliosVariable.ACCESS_RW, 15, 12, HeliosVariable.TYPE_STRING));
        }

        this.vMap.put(HeliosBindingConstants.SOFTWARE_VERSION_BASIS, new HeliosVariable(1101, HeliosVariable.ACCESS_R, 5, 7, HeliosVariable.TYPE_FLOAT, 0, 99.99));

        descriptions = new String[] {
                HeliosBindingConstants.OPERATING_HOURS_SUPPLY_AIR_VENT,
                HeliosBindingConstants.OPERATING_HOURS_EXTRACT_AIR_VENT,
                HeliosBindingConstants.OPERATING_HOURS_VHZ,
                HeliosBindingConstants.OPERATING_HOURS_NHZ
        };
        for (int i = 1103; i <= 1106; i++) {
            this.vMap.put(descriptions[i - 1103], new HeliosVariable(i, HeliosVariable.ACCESS_R, 10, 9, HeliosVariable.TYPE_INTEGER, 0, 4294967295L));
        }

        descriptions = new String[] {
                HeliosBindingConstants.OUTPUT_POWER_VHZ,
                HeliosBindingConstants.OUTPUT_POWER_NHZ
        };
        for (int i = 1108; i <= 1109; i++) {
            this.vMap.put(descriptions[i - 1108], new HeliosVariable(i, HeliosVariable.ACCESS_R, 10, 9, HeliosVariable.TYPE_INTEGER, 0, 4294967295L));
        }

        // TODO: codings?
        this.vMap.put(HeliosBindingConstants.RESET_FLAG, new HeliosVariable(1120, HeliosVariable.ACCESS_R, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 1));
        this.vMap.put(HeliosBindingConstants.ERRORS, new HeliosVariable(1123, HeliosVariable.ACCESS_R, 10, 9, HeliosVariable.TYPE_INTEGER, 0, 4294967295L));
        this.vMap.put(HeliosBindingConstants.WARNINGS, new HeliosVariable(1124, HeliosVariable.ACCESS_R, 3, 6, HeliosVariable.TYPE_INTEGER, 0, 255));
        this.vMap.put(HeliosBindingConstants.INFOS, new HeliosVariable(1125, HeliosVariable.ACCESS_R, 3, 6, HeliosVariable.TYPE_INTEGER, 0, 255));
        this.vMap.put(HeliosBindingConstants.NO_OF_ERRORS, new HeliosVariable(1300, HeliosVariable.ACCESS_R, 2, 5, HeliosVariable.TYPE_INTEGER, 0, 32));
        this.vMap.put(HeliosBindingConstants.NO_OF_WARNINGS, new HeliosVariable(1301, HeliosVariable.ACCESS_R, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 8));
        this.vMap.put(HeliosBindingConstants.NO_OF_INFOS, new HeliosVariable(1302, HeliosVariable.ACCESS_R, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 8));
        this.vMap.put(HeliosBindingConstants.ERRORS_MSG, new HeliosVariable(1303, HeliosVariable.ACCESS_R, 32, 20, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.WARNINGS_MSG, new HeliosVariable(1304, HeliosVariable.ACCESS_R, 8, 8, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.INFOS_MSG, new HeliosVariable(1305, HeliosVariable.ACCESS_R, 8, 8, HeliosVariable.TYPE_STRING));
        this.vMap.put(HeliosBindingConstants.STATUS_FLAGS, new HeliosVariable(1306, HeliosVariable.ACCESS_R, 32, 20, HeliosVariable.TYPE_STRING));

        descriptions = new String[] {
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_1,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_2,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_3,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_4,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_5,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_6,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_7,
                HeliosBindingConstants.SENSOR_CONFIG_KWL_FTF_8
        };
        for (int i = 2020; i <= 2027; i++) {
            this.vMap.put(descriptions[i - 2020], new HeliosVariable(i, HeliosVariable.ACCESS_R, 1, 5, HeliosVariable.TYPE_INTEGER, 0, 1));
        }

        this.vMap.put(HeliosBindingConstants.GLOBAL_MANUAL_WEB_UPDATE, new HeliosVariable(2013, HeliosVariable.ACCESS_RW, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 1));
        this.vMap.put(HeliosBindingConstants.PORTAL_GLOBALS_ERROR_FOR_WEB, new HeliosVariable(2014, HeliosVariable.ACCESS_R, 3, 6, HeliosVariable.TYPE_INTEGER, 1, 255));
        this.vMap.put(HeliosBindingConstants.CLEAR_ERROR, new HeliosVariable(2015, HeliosVariable.ACCESS_W, 1, 5, HeliosVariable.TYPE_INTEGER, 1, 1));
    }

    /**
     * Returns the variable
     * @param variableName Variable name
     * @return The variable
     */
    public HeliosVariable getVariable(String variableName) {
        return this.vMap.get(variableName);
    }

    private Map getSortedMap() {
        List<HeliosVariable> list = new LinkedList(this.vMap.entrySet());
        Collections.sort(list, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                            .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        Map sortedMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    /**
     * Returns an HTML formatted list of variables
     * @return The HTML table
     */
    public String getHtmlList() {
        String html = new String();
        html += "<table class=\"helios\">";
        html += "<tr>";
        html += "<th>Description</th>";
        html += "<th>Access</th>";
        html += "<th>Type</th>";
        html += "<th>Count</th>";
        html += "<th>Variable</th>";
        html += "<th>Min Value</th>";
        html += "<th>Max Value</th>";
        html += "</tr>";
        int i = 0;
        for (Map.Entry<String, HeliosVariable> e : ((Map<String, HeliosVariable>) this.getSortedMap()).entrySet()) {
            html += "<tr class=\"" + (i % 2 == 0 ? "even" : "odd") + "\">";
            // description
            html += "<td>" + e.getKey() + "</td>";

            HeliosVariable v = e.getValue();
            // Access
            html += "<td>";
            switch(v.getAccess()) {
                case HeliosVariable.ACCESS_R:
                    html += "R";
                    break;
                case HeliosVariable.ACCESS_W:
                    html += "W";
                    break;
                case HeliosVariable.ACCESS_RW:
                    html += "RW";
                    break;
                default:
                    html += "N/A";
                    break;
            }
            html += "</td>";
            // Type
            html += "<td>Char[" + v.getLength() + "]</td>";
            // Count
            html += "<td>" + v.getCount() + "</td>";
            // Variable
            html += "<td>" + v.getVariableString() + "</td>";
            // Min Value
            html += "<td>" + (v.getMinVal() == null ? "-" : v.getMinVal()) + "</td>";
            // Max Value
            html += "<td>" + (v.getMaxVal() == null ? "-" : v.getMaxVal()) + "</td>";
            html += "</tr>";
        }
        html += "</table>";
        return html;
    }
}
