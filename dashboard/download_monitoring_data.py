import firebase_manager
import csv 

user = []

with open("users.csv") as users:
    for line in users.readlines():
        u = firebase_manager.append_user_to_appid(line.strip())
        user.append(u)

for usr in user:
    print(usr)
    fm = firebase_manager.FireBaseManager()
    fm.download_monitoring_one_user(usr)
    # fm.download_write_queries(user[0])
