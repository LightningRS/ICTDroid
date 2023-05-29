#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import shutil

### Java Path
JAVA_PATH = shutil.which('java')

### ICTDroid Properties START
STRATEGIES = 'base'
RAND_VAL_NUM = 3
RAND_SEED = 12345678
DEVICE_SERIAL = None
LAUNCHER_PKG_NAME = 'com.android.launcher3'
SCOPE_CFG_PATH = None
STR_LEN_MIN = None
STR_LEN_MAX = None
### ICTDroid Properties END

ROOT_PATH = os.path.abspath(os.path.dirname(os.path.dirname(__file__)))

ADB_PATH = os.path.join(ROOT_PATH, "adb.exe")
ICCBOT_ROOT_PATH = os.path.join(ROOT_PATH, "ICCBot")

ICTDROID_JAR_PATH = os.path.join(ROOT_PATH, "ictdroid.jar")
ICCBOT_JAR_PATH = os.path.join(ICCBOT_ROOT_PATH, "ICCBot.jar")
ANDROID_LIB_PATH = os.path.join(ICCBOT_ROOT_PATH, "lib/platforms")

LOG_PATH = os.path.join(ROOT_PATH, "logs")
DATA_PATH = os.path.join(ROOT_PATH, "data")
APKS_PATH = os.path.join(DATA_PATH, "APKs")
ICCBOT_RESULT_PATH = os.path.join(DATA_PATH, "ICCResult")
TESTCASE_PATH = os.path.join(DATA_PATH, "Testcases")

if not os.path.exists(JAVA_PATH):
    print("Java.exe not found!")
    exit(0)

if not os.path.exists(ADB_PATH):
    print("adb.exe not found!")
    exit(0)

if not os.path.exists(ICTDROID_JAR_PATH):
    print("ictdroid.jar not found!")
    exit(0)

if not os.path.exists(ICCBOT_JAR_PATH):
    print("ICCBot.jar not found!")
    exit(0)

if not os.path.exists(ANDROID_LIB_PATH):
    print("Android library (platforms) not found!")
    exit(0)

os.makedirs(LOG_PATH, exist_ok=True)
os.makedirs(APKS_PATH, exist_ok=True)
os.makedirs(ICCBOT_RESULT_PATH, exist_ok=True)
os.makedirs(TESTCASE_PATH, exist_ok=True)
