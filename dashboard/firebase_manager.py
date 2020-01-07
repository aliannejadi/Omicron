from google.cloud import storage
import google.auth
from google.oauth2 import service_account
import os
import re
import pandas as pd
from os import path
import logging
import json
from json import JSONDecoder, JSONDecodeError
NOT_WHITESPACE = re.compile(r'[^\s]')

# https://stackoverflow.com/a/50384432
def decode_stacked(document, pos=0, decoder=JSONDecoder()):
    while True:
        match = NOT_WHITESPACE.search(document, pos)
        if not match:
            return
        pos = match.start()

        try:
            obj, pos = decoder.raw_decode(document, pos)
        except JSONDecodeError:
            # do something sensible if there's some error
            print("Bad file")
            return
            #raise
        yield obj


def find_filenames_with_prefix(path_to_dir, prefix):
    filenames = os.listdir(path_to_dir)
    return [filename for filename in filenames if filename.startswith(prefix)]


def put_one_json_per_line(inp_string, output_path):
    splitted_string = re.split('(\{.*?\})(?= *\{)', inp_string)
    with open(output_path, 'w') as out:
        for l in splitted_string:
            if len(l.strip()) > 0:
                out.write(l.strip() + '\n')


# def compose_and_write(main_bucket, blbs_to_compose, b):
#     new_blob = storage.Blob(name="composed_blobs", bucket=main_bucket)
#     new_blob.content_type = "text/plain"
#     new_blob.compose(sources=blbs_to_compose, client=client)
#     with open(data_path + b.name, 'wb') as outfile:
#         new_blob.download_to_file(outfile)
#         print('Wrote a composed blob ' + b.name + ' to file!')

def rename_appids_for_firebase(appid_list):
    output_list = []
    for app in appid_list:
        output_list.append(append_user_to_appid(app))
    return output_list


def append_user_to_appid(appid):
    return 'user{}'.format(appid)


def correct_path(bkt):
    if not bkt.endswith('/'):
        bkt += '/'
    return bkt


def check_downloaded_file_validity(file_path):
    try:
        temp_df = pd.read_json(file_path, orient='columns', lines=True, encoding='utf-8')
        if len(temp_df.loc[0, temp_df.columns[0]]) > 0:
            return True
    except ValueError:
        return False


def merge_prefix_files_multiprocess(usr, json_folder, separate_json_folder=False):
    # now we put all files together in one json file for each prefix.
    prefix_list = ['query_records', 'location_records', 'model', 'usage_records', 'relevant_result_records',
                    'input_records', 'history_records', 'battery_records', 'cell_records', 'screen_records',
                    'wlan_records', 'accelerometer_records', 'gyroscope_records', 'light_records']

    data_path = 'user-study-data/'
    for prf in prefix_list:
        file_with_pref = find_filenames_with_prefix(os.path.join(data_path, usr), prf)
        txt_stream = ''
        for f in file_with_pref:
            with open(os.path.join(data_path, usr, f)) as inp_txt:
                temp_file_read = inp_txt.read()
                txt_stream += temp_file_read.strip()
        if separate_json_folder:
            temp_json_folder = os.path.join(json_folder, usr)
        else:
            temp_json_folder = os.path.join(data_path, usr, json_folder)
        if not os.path.exists(temp_json_folder):
            os.makedirs(temp_json_folder)
        put_one_json_per_line(txt_stream, os.path.join(temp_json_folder, prf + '.json'))
    print('Done: {}'.format(usr))


def initialize_logger(log_file=''):
    if log_file:
        logging.basicConfig(filename=log_file,
                            filemode='a',
                            format='%(asctime)-25s %(name)-20s %(levelname)-8s %(message)s',
                            datefmt='%H:%M:%S',
                            level=logging.INFO)
    else:
        logging.basicConfig(format='%(asctime)-25s %(name)-20s %(levelname)-8s %(message)s',
                            datefmt='%H:%M:%S',
                            level=logging.INFO)


class FireBaseManager:
    def __init__(self):
        self._data_path = 'user-study-data/'
        self._json_folder = 'aggregated-jsons/'
        self._credentials_file = 'gapp-3a5199568f45.json'
        self._bucket_name = 'chiir-642b9.appspot.com'
        self._prefix_list = ['query_records', 'location_records', 'model', 'usage_records', 'relevant_result_records',
                             'input_records', 'history_records', 'battery_records', 'cell_records', 'screen_records',
                             'wlan_records', 'accelerometer_records', 'gyroscope_records', 'light_records', 
                             'activities_records', 'sms_records', 'call_records']
        self._monitor_prefix = ['location_records', 'query_records',
                               'usage_records', 'activities_records',
                                'sms_records', 'call_records', 'relevant_result_records', 'history_records']

        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = self._credentials_file
        self.connect()
        initialize_logger()
        self.logger = logging.getLogger('FireBaseManager')

    def connect(self):
        # we connect
        self._credentials = service_account.Credentials.from_service_account_file(self._credentials_file)
        if self._credentials.requires_scopes:
            self._credentials = self._credentials.with_scopes(['https://www.googleapis.com/auth/devstorage.read_write'])
        self._client = storage.Client(credentials=self._credentials)
        #         self._client = storage.Client.from_service_account_json(self._credentials_file)
        self._bucket = self._client.get_bucket(self._bucket_name)

    @property
    def prefix_list(self):
        return self._prefix_list

    @prefix_list.setter
    def prefix_list(self, new_value):
        self._prefix_list = new_value

    @property
    def json_folder(self):
        return self._json_folder

    @json_folder.setter
    def json_folder(self, new_value):
        self._json_folder = new_value

    @property
    def bucket_name(self):
        return self._bucket_name

    @bucket_name.setter
    def bucket_name(self, new_value):
        self._bucket_name = new_value

    @property
    def credentials_file(self):
        return self._credentials_file

    @credentials_file.setter
    def credentials_file(self, new_value):
        self._credentials_file = new_value

    def set_logger_file(self, logger_file):
        initialize_logger(logger_file)

    def create_data_dir_if_not_exists(self, bkt):
        if not os.path.exists(self._data_path + bkt.strip('/')):
            os.makedirs(self._data_path + bkt.strip('/'))

    def merge_prefix_files(self, usr, separate_json_folder=False):
        # now we put all files together in one json file for each prefix.
        for prf in self._prefix_list:
            file_with_pref = find_filenames_with_prefix(os.path.join(self._data_path, usr), prf)
            list_content_files = []
            txt_stream = ""
            for f in file_with_pref:
                with open(os.path.join(self._data_path, usr, f)) as inp_txt:
                    print(os.path.join(self._data_path, usr, f))
                    temp_file_read = inp_txt.read().strip()

                    for obj in decode_stacked(temp_file_read):
                        list_content_files.append(json.dumps(obj))

                    # try:
                    #     json.loads(temp_file_read)
                    #     txt_stream += temp_file_read
                    # except Exception as ex:
                    #     print(ex)
            txt_stream = "\n".join(list_content_files)

            if separate_json_folder:
                temp_json_folder = os.path.join(self._json_folder, usr)
            else:
                temp_json_folder = os.path.join(self._data_path, usr, self._json_folder)
            if not os.path.exists(temp_json_folder):
                os.makedirs(temp_json_folder)
            # put_one_json_per_line(txt_stream, os.path.join(temp_json_folder, prf + '.json'))
            with open(os.path.join(temp_json_folder, prf + '.json'), 'w') as out:
                out.write(txt_stream)

    def _download_blob_if_not_exists(self, b):
        file_path = self._data_path + b.name
        if b.name.__contains__('_records_') and os.path.isfile(file_path) is False:
            with open(file_path, 'wb') as outfile:
                b.download_to_file(outfile)
                self.logger.info('Successfully downloaded {}'.format(b.name))

    def download_one_file_per_user_per_prefix(self, user_name, data_prefix, check=False):
        blobs = self._bucket.list_blobs(prefix=user_name + data_prefix)
        for b in blobs:
            self._download_blob_if_not_exists(b)
            file_path = self._data_path + b.name
            if check:
                if check_downloaded_file_validity(file_path):
                    break
            else:
                break

    def download_files_per_user_per_prefix(self, user_name, data_prefix):
        blobs = self._bucket.list_blobs(prefix=user_name + data_prefix)
        for b in blobs:
            self._download_blob_if_not_exists(b)

    def download_write_file_for_one_user(self, usr):
        usr = correct_path(usr)
        self.create_data_dir_if_not_exists(usr)
        for prf in self._prefix_list:
            self.download_files_per_user_per_prefix(usr, prf)
        self.merge_prefix_files(usr)

    def download_monitoring_one_user(self, usr):
        usr = correct_path(usr)
        self.create_data_dir_if_not_exists(usr)
        for prf in self._monitor_prefix:
            self.download_files_per_user_per_prefix(usr, prf)
        self.merge_prefix_files(usr)

    def download_write_app_usage(self, user_name):
        usr = correct_path(user_name)
        self.create_data_dir_if_not_exists(usr)
        app_usage_prefix = 'usage_records'
        self.download_one_file_per_user_per_prefix(usr, app_usage_prefix, True)

    def download_write_location(self, user_name):
        usr = correct_path(user_name)
        self.create_data_dir_if_not_exists(usr)
        location_prefix = 'location_records'
        self.download_one_file_per_user_per_prefix(usr, location_prefix)

    def download_write_queries(self, user_name):
        usr = correct_path(user_name)
        self.create_data_dir_if_not_exists(usr)
        query_prefix = 'query_records'
        self.download_files_per_user_per_prefix(usr, query_prefix)

    def download_write_all_files(self):
        blobs = self._bucket.list_blobs()
        for b in blobs:
            user_name = path.dirname(b.name)
            self.create_data_dir_if_not_exists(user_name)
            self._download_blob_if_not_exists(b)
