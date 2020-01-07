
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
import re
import seaborn as sb
import codecs

def explode(df, lst_cols, fill_value=''):
    # make sure `lst_cols` is a list
    if lst_cols and not isinstance(lst_cols, list):
        lst_cols = [lst_cols]
    # all columns except `lst_cols`
    idx_cols = df.columns.difference(lst_cols)

    # calculate lengths of lists
    lens = df[lst_cols[0]].str.len()

    if (lens > 0).all():
        # ALL lists in cells aren't empty
        return pd.DataFrame({
            col: np.repeat(df[col].values, df[lst_cols[0]].str.len())
            for col in idx_cols
        }).assign(**{col: np.concatenate(df[col].values) for col in lst_cols}) \
            .loc[:, df.columns]
    else:
        # at least one list in cells is empty
        return pd.DataFrame({
            col: np.repeat(df[col].values, df[lst_cols[0]].str.len())
            for col in idx_cols
        }).assign(**{col: np.concatenate(df[col].values) for col in lst_cols}) \
            .append(df.loc[lens == 0, idx_cols]).fillna(fill_value) \
            .loc[:, df.columns]


class DataManager:
    def __init__(self):
        self._file_prefixes = ['location_records', 'query_records', 'input_records', 'relevant_result_records',
                               'usage_records', 'history_records', 'battery_records', 'cell_records', 'screen_records',
                               'wlan_records', 'accelerometer_records', 'gyroscope_records', 'light_records',
                               'activities_records', 'sms_records', 'call_records']

        self._data_path = 'user-study-data/'
        self._json_folder = 'aggregated-jsons/'

    def _read_explode_json(self, file, columns=[]):
        try:
            apps_df = pd.read_json(file, orient='columns', lines=True)
            if len(columns) > 0:
                return explode(apps_df, columns)
        except ValueError as ve:
            raise ValueError(str(ve) + ' error while reading the file \n' + file)
        except KeyError:
            print("Key not present, is the file empty?")
            return None

        return apps_df

    #https://gist.github.com/jlln/338b4b0b55bd6984f883
    #duplicated, to be removed
    def split_data_frame_list(self, df, 
                       target_column,
                      output_type=int):
        ''' 
        Accepts a column with multiple types and splits list variables to several rows.

        df: dataframe to split
        target_column: the column containing the values to split
        output_type: type of all outputs
        returns: a dataframe with each entry for the target column separated, with each element moved into a new row. 
        The values in the other columns are duplicated across the newly divided rows.
        '''
        row_accumulator = []

        def split_list_to_rows(row):
            split_row = row[target_column]
            if isinstance(split_row, list):
                for s in split_row:
                    new_row = row.to_dict()
                    new_row[target_column] = output_type(s)
                    row_accumulator.append(new_row)
            else:
                new_row = row.to_dict()
                new_row[target_column] = output_type(split_row)
                row_accumulator.append(new_row)
    
        df.apply(split_list_to_rows, axis=1)
        new_df = pd.DataFrame(row_accumulator)
    
        return new_df

    def _load_user_data(self, user_id, file_type, col_exp=[]):
        path = os.path.join(self._data_path, user_id,
                            self._json_folder, file_type)
        df_exploded = self._read_explode_json(path, col_exp)
        if df_exploded is None:
            return None
        try:
            df_exploded['timestamp'] = pd.to_datetime(
                df_exploded['timestamp'], unit='ms')
            df_exploded.set_index('timestamp', inplace=True)
        except KeyError as ke:
            print("load_user_data_exception")
            print(file_type)
            print("Error with key: " + str(ke))
            print("is empty?" + str(df_exploded.empty))
            return None
        df_exploded.sort_index(inplace=True)
        return df_exploded

    def _load_user_data_index(self, user_id, file_type, index, col_exp=[]):
        path = os.path.join(self._data_path, user_id,
                            self._json_folder, file_type)
        df_exploded = self._read_explode_json(path, col_exp)
        if df_exploded is None:
            return None
        try:
            df_exploded[index] = pd.to_datetime(
                df_exploded[index], unit='ms')
            df_exploded.set_index(index, inplace=True)
        except KeyError as ke:
            print("load_user_data_exception")
            print(file_type)
            print("Error with key: " + str(ke))
            print("is empty?" + str(df_exploded.empty))
            return None
        df_exploded.sort_index(inplace=True)
        return df_exploded


    def _append_app_usage_data(self, query_data, query_time, use_data_res_index):
        closest_uidx = np.abs(
            use_data_res_index['timestamp'] - query_time).idxmin()
        closest_usage_row = use_data_res_index.loc[closest_uidx]
        usageList = pd.DataFrame({'App': closest_usage_row.appList, 'Duration':
                                  closest_usage_row.usageList}).sort_values('Duration', ascending=False)
        eventList = pd.DataFrame({'EventType': closest_usage_row.eventType,
                                  'EventsAppName': closest_usage_row.eventsAppName,
                                  'EventTimestamp': closest_usage_row.eventTimestamp}).sort_values(
            'EventTimestamp')
        
        eventList.EventTimestamp = pd.to_datetime(
            eventList.EventTimestamp, unit='ms')
        query_data.at[query_time, 'AppUsages'] = dict(usageList)
        query_data.at[query_time, 'UsageEvents'] = dict(eventList)

    def _append_location_data(self, loc_data_res_index, query_data, query_time):
        # adding location data
        if loc_data_res_index is not None:
            try:
                closest_idx = np.abs(
                    query_time - loc_data_res_index['timestamp']).idxmin()
                closest_loc_row = loc_data_res_index.loc[closest_idx]
                query_data.at[query_time,
                              'LocationTime'] = closest_loc_row['timestamp']
                query_data.at[query_time,
                              'LocationLatitude'] = closest_loc_row['latitude']
                query_data.at[query_time,
                              'LocationLongitude'] = closest_loc_row['longitude']
            except KeyError:
                pass

    def _reset_location_index(self, location_data):
        if location_data is not None:
            loc_data_res_index = location_data.reset_index()
        else:
            loc_data_res_index = None
        return loc_data_res_index

    def _load_app_usage_data(self, curr_user, col = []):
        usage_data = self._load_user_data(
            curr_user, self._file_prefixes[4] + '.json', col_exp = col)
        return usage_data

    def _load_battery_data(self, curr_user, col = []):
        battery_data = self._load_user_data(
            curr_user, self._file_prefixes[6] + '.json',col_exp = col)
        return battery_data
    
    def _load_calls_data(self, curr_user, col = []):
        call_data = self._load_user_data(
            curr_user, self._file_prefixes[15] + '.json',col_exp = col)
        return call_data    

    def _load_sms_data(self, curr_user, col = []):
        sms_data = self._load_user_data(
            curr_user, self._file_prefixes[15] + '.json',col_exp = col)
        return sms_data    
    
    def _load_screen_data(self, curr_user, col = ["timestamp", "event"]):
        screen_data = self._load_user_data(
            curr_user, self._file_prefixes[8] + '.json', col_exp = col)
        return screen_data
    
    def _load_relevant_result_data(self, curr_user, col = ["timestamp"]):
        relevant_data = self._load_user_data(
            curr_user, self._file_prefixes[3] + '.json', col)
        return relevant_data


    def _load_history_data(self, curr_user, col = []):
        history_data = self._load_user_data(
            curr_user, self._file_prefixes[5] + '.json',col_exp = col)
        return history_data

    def _load_input_data(self, curr_user, col = []):
        input_data = self._load_user_data_index(
            curr_user, self._file_prefixes[2] + '.json', "rawTimestamp" ,col_exp = col)
        return input_data

    def _load_location_data(self, curr_user, col = []):
        try:
            location_data = self._load_user_data(
                curr_user, self._file_prefixes[0] + '.json',col_exp = col)  # TODO:refactor
        except Exception:  # TODO:add specific exception
            print(Exception)
            location_data = None
        return location_data

    def _load_query_data(self, curr_user, col = []):
        query_data = self._load_user_data(
            curr_user, self._file_prefixes[1] + '.json',col_exp = col)  # TODO:refactor
        return query_data

    def _load_activities_data(self, curr_user, col = ["timestamp", "event"]):
        query_data = self._load_user_data(
            curr_user, self._file_prefixes[13] + '.json', col_exp=col)  # TODO:refactor
        
        return query_data

    def sync_data(self, curr_user):
        query_data = self._load_query_data(curr_user)
        if query_data is None:
            print("query data is empty")
            return
        location_data = self._load_location_data(curr_user)
        usage_data = self._load_app_usage_data(curr_user)

        if usage_data is None:
            print("usage data is empty")
            return
        loc_data_res_index = self._reset_location_index(location_data)
        use_data_res_index = usage_data.reset_index()
        query_data['AppUsages'] = None
        query_data['UsageEvents'] = None
        for i in range(query_data.shape[0]):
            query_time = query_data.index[i]
            self._append_location_data(
                loc_data_res_index, query_data, query_time)
            self._append_app_usage_data(
                query_data, query_time, use_data_res_index)
        return query_data
