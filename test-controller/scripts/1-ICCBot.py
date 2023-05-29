#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import config

ICCBOT_CMD_TEMPLATE = '{config.JAVA_PATH} -Xmx128G -jar "{config.ICCBOT_JAR_PATH}" '
ICTDROID_CMD = '-path "{config.APKS_PATH}" -name "{ph_a}" '
ICTDROID_CMD += '-androidJar "{config.ANDROID_LIB_PATH}" '
ICTDROID_CMD += '-time 2880 -maxPathNumber 100 -client ICCSpecClient '
ICTDROID_CMD += '-outputDir "{config.ICCBOT_RESULT_PATH}" '
ICTDROID_CMD += '>> "{config.LOG_PATH}/iccbot-{ph_an}.log"'
ICCBOT_CMD_TEMPLATE = ICCBOT_CMD_TEMPLATE.format(
    config=config, ph_a='{apk_name}', ph_an='{apk_basename}'
)

root_path = os.path.dirname(os.path.dirname(__file__))
# apks_path = os.path.join(root_path, "data/apks")
result_path = os.path.join(root_path, "data/ICCResult")
case_path = os.path.join(root_path, "data/Testcases")

if not os.path.exists(config.APKS_PATH):
    os.makedirs(config.APKS_PATH)
if not os.path.exists(result_path):
    os.makedirs(result_path)
if not os.path.exists(case_path):
    os.makedirs(case_path)

apks_lis = []
for root, dirs, files in os.walk(config.APKS_PATH):
    for file in files:
        if not file.endswith('.apk'):
            continue
        apks_lis.append(os.path.abspath(os.path.join(root, file)))

if len(apks_lis) == 0:
    print("No apk found!")
    exit(0)

if not os.path.exists(config.JAVA_PATH):
    print("Java.exe not found!")
    exit(0)

if not os.path.exists(config.ICCBOT_JAR_PATH):
    print("ICCBot.jar not found!")
    exit(0)

if not os.path.exists(config.ANDROID_LIB_PATH):
    print("Android library (platforms) not found!")
    exit(0)

for apk_path in apks_lis:
    apk_name = str(os.path.basename(apk_path))
    apk_basename = apk_name.replace('.apk', '')
    cmd = ICCBOT_CMD_TEMPLATE.format(apk_name=apk_name, apk_basename=apk_basename)
    print(cmd)
    os.chdir(config.ICCBOT_ROOT_PATH)
    if os.system(cmd) != 0:
        print("Error when running ICCBot on [{}]".format(apk_path))
    else:
        print("Finshed running ICCBot on [{}]".format(apk_path))
