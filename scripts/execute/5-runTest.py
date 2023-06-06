#!/usr/bin/python3
# ! -*- coding: utf-8 -*-

import os
import subprocess
import sys

import config

ICTDROID_EXTRA = ''
if config.DEVICE_SERIAL is not None:
    ICTDROID_EXTRA += ' -d "{config.DEVICE_SERIAL}"'.format(config=config)

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

# Install test bridge
if not os.path.exists(config.TEST_BRIDGE_PATH):
    print("test-bridge.apk not found!")
    exit(0)
install_cmd = [
    config.ADB_PATH,
    'install', '-g', '-t', config.TEST_BRIDGE_PATH,
]
proc = subprocess.run(install_cmd, stdout=sys.stdout, stderr=sys.stderr)
if proc.returncode != 0:
    print("Failed to install test-bridge.apk!")
    exit(0)

if os.system(ICTDROID_CMD) != 0:
    print("Failed to call ICTDroid! cmd={}".format(ICTDROID_CMD))
else:
    print("Finished calling ICTDroid")
