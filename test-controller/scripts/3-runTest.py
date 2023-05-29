#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import config

ICTDROID_EXTRA = ''
if config.DEVICE_SERIAL is not None:
    ICTDROID_EXTRA += ' -d "{config.DEVICE_SERIAL}"'.format(config=config)

ICTDROID_CMD = '{config.JAVA_PATH} -jar "{config.ICTDROID_JAR_PATH}" '
ICTDROID_CMD += '-k "{config.APKS_PATH}" -i "{config.ICCBOT_RESULT_PATH}" '
ICTDROID_CMD += '-c "{config.TESTCASE_PATH}" '
# ICTDROID_CMD += '-st "{config.STRATEGIES}" '
ICTDROID_CMD += '-a "{config.ADB_PATH}" -ln "{config.LAUNCHER_PKG_NAME}" '
ICTDROID_CMD += '-ng {config.ICTDROID_COMMON_PARAMS}'
ICTDROID_CMD = ICTDROID_CMD.format(config=config) + ICTDROID_EXTRA
print(ICTDROID_CMD)

if not os.path.exists(config.JAVA_PATH):
    print("Java.exe not found!")
    exit(0)

if not os.path.exists(config.ICTDROID_JAR_PATH):
    print("AACT.jar not found!")
    exit(0)

if not os.path.exists(config.ADB_PATH):
    print("adb.exe not found!")
    exit(0)

if os.system(ICTDROID_CMD) != 0:
    print("Failed to call AACT! cmd={}".format(ICTDROID_CMD))
else:
    print("Finished calling AACT")
