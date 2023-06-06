#!/usr/bin/python3
# ! -*- coding: utf-8 -*-

import os

import config

s_ia = input("Start apkIndex: ")
s_ic = input("Start compIndex: ")
s_it = input("Start caseIndex: ")
# s_is = input("Start strategy: ")

ICTDROID_EXTRA = ''
if config.SCOPE_CFG_PATH is not None:
    ICTDROID_EXTRA += ' -o "{config.SCOPE_CFG_PATH}"'.format(config=config)
if config.RAND_SEED is not None:
    ICTDROID_EXTRA += ' -s "{config.RAND_SEED}"'.format(config=config)
if config.STR_LEN_MIN is not None:
    ICTDROID_EXTRA += ' -smin "{config.STR_LEN_MIN}"'.format(config=config)
if config.STR_LEN_MAX is not None:
    ICTDROID_EXTRA += ' -smax "{config.STR_LEN_MAX}"'.format(config=config)
if config.DEVICE_SERIAL is not None:
    ICTDROID_EXTRA += ' -d "{config.DEVICE_SERIAL}"'.format(config=config)

ICTDROID_EXTRA += ' -ia {s_ia}'.format(s_ia=s_ia)
ICTDROID_EXTRA += ' -ic {s_ic}'.format(s_ic=s_ic)
ICTDROID_EXTRA += ' -it {s_it}'.format(s_it=s_it)
# ICTDROID_EXTRA += ' -is "{s_is}"'.format(s_is=s_is)

ICTDROID_CMD = '{config.JAVA_PATH} -jar "{config.ICTDROID_JAR_PATH}" '
ICTDROID_CMD += '-k "{config.APKS_PATH}" -i "{config.ICCBOT_RESULT_PATH}" '
ICTDROID_CMD += '-c "{config.TESTCASE_PATH}" '
# ICTDROID_CMD += '-st "{config.STRATEGIES}" '
ICTDROID_CMD += '-a "{config.ADB_PATH}" -ln "{config.LAUNCHER_PKG_NAME}" '
ICTDROID_CMD += '-ng -ce'
ICTDROID_CMD = ICTDROID_CMD.format(config=config) + ICTDROID_EXTRA
print(ICTDROID_CMD)

if not os.path.exists(config.JAVA_PATH):
    print("Java.exe not found!")
    exit(0)

if not os.path.exists(config.ICTDROID_JAR_PATH):
    print("ictdroid.jar not found!")
    exit(0)

if not os.path.exists(config.ADB_PATH):
    print("adb.exe not found!")
    exit(0)

if os.system(ICTDROID_CMD) != 0:
    print("Failed to call ICTDroid! cmd={}".format(ICTDROID_CMD))
else:
    print("Finished calling ICTDroid")
