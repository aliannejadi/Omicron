package ch.usi.inf.omicron;

import java.util.Arrays;
import java.util.List;

import static ch.usi.inf.omicron.background.RecordStorageManager.ACCELEROMETER_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.ACTIVITIES_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.BATTERY_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.CALL_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.CELL_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.GYROSCOPE_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.HISTORY_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.INPUT_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.LIGHT_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.LOCATION_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.POST_ANSWERS_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.QUERY_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.RELEVANT_RESULTS_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.SCREEN_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.SELECTION_ITEM_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.SMS_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.USAGE_FILE;
import static ch.usi.inf.omicron.background.RecordStorageManager.WLAN_FILE;

/**
 * Configuration file to customize Omicron
 */
public class Configuration {
    // Upload WIFI (Constants in RecordStorageManager)
    public static final List<String> filesNamesList = Arrays.asList(
            ACCELEROMETER_FILE,
            BATTERY_FILE,
            CELL_FILE,
            GYROSCOPE_FILE,
            HISTORY_FILE,
            INPUT_FILE,
            LOCATION_FILE,
            QUERY_FILE,
            RELEVANT_RESULTS_FILE,
            SCREEN_FILE,
            SELECTION_ITEM_FILE,
            USAGE_FILE,
            WLAN_FILE,
            POST_ANSWERS_FILE,
            CALL_FILE,
            SMS_FILE,
            ACTIVITIES_FILE,
            LIGHT_FILE
    );
    // Upload mobile data
    public static final List<String> lightFilesNamesList = Arrays.asList(
            BATTERY_FILE,
            CELL_FILE,
            HISTORY_FILE,
            LOCATION_FILE,
            QUERY_FILE,
            RELEVANT_RESULTS_FILE,
            SCREEN_FILE,
            SELECTION_ITEM_FILE,
            USAGE_FILE,
            WLAN_FILE,
            //POST_ANSWERS_FILE,
            CALL_FILE,
            SMS_FILE,
            ACTIVITIES_FILE
    );
    // Collectors
    public static final Boolean shouldRecordAppUsage = true;
    public static final Boolean shouldRecordLocation = true;
    public static final Boolean shouldRecordWLAN = true;
    public static final Boolean shouldRecordCell = true;
    public static final Boolean shouldRecordAccelerometer = true;
    public static final Boolean shouldRecordGyroscope = true;
    public static final Boolean shouldRecordLight = true;
    public static final Boolean shouldRecordBattery = true;
    public static final Boolean shouldRecordScreen = true;
    public static final Boolean shouldRecordActivities = true;
    public static final Boolean shouldRecordCall = false;
    public static final Boolean shouldRecordSMS = false;
    // Search Engine Key
    static final String BING_KEY = "307f8a5745f64885b15d634055b1d920";
    // Surveys Qualtrics
    static final String FIRST_INSTALLATION_SURVEY = "https://usi.eu.qualtrics.com/jfe/form/SV_9KwlyX9uKq3DFMF";
    static final String PRE_TASK_URL = "https://usi.eu.qualtrics.com/jfe/form/SV_0jpmEMTIqlg0xdr";
    static final String POST_TASK_URL = "https://usi.eu.qualtrics.com/jfe/form/SV_diD2jD2dLEIxa6h";
    // Sample rates (ms)
    // app usage interval period in milliseconds
    public static final long USAGE_INTERVAL = 1000 * 60 * 60 * 24;
    // the sample rate for location recording in milliseconds
    public static final long locationRate = 1000 * 60 * 3;
    // the sample rate in milliseconds
    public static final long sampleRate = 1000 * 60 * 2;
    // the recording submission rate
    public static final long recordRate = 1000 * 60 * 60;
}
