#!/usr/bin/python3
# ! -*- coding: utf-8 -*-

import os
import subprocess
import sys

import config

ICTDROID_CMD = [
    config.JAVA_PATH, '-jar', config.ICTDROID_JAR_PATH,
    '-k', config.APKS_PATH,
    '-i', config.ICCBOT_RESULT_PATH,
    '-c', config.TESTCASE_PATH,
    '-a', config.ADB_PATH,
    '-ln', config.LAUNCHER_PKG_NAME,
    '-ng',  # Do not generate testcases if not exists
    '-oe',  # Only exported component
    '-ce',  # Continue when error
]
if config.SCOPE_CFG_PATH is not None:
    ICTDROID_CMD.extend(['-d', str(config.DEVICE_SERIAL)])

if not os.path.exists(config.JAVA_PATH):
    print("Java.exe not found!")
    exit(0)

if not os.path.exists(config.ICTDROID_JAR_PATH):
    print("ictdroid.jar not found!")
    exit(0)

if not os.path.exists(config.ADB_PATH):
    print("adb executable not found!")
    exit(0)

if not os.path.exists(config.TEST_BRIDGE_PATH):
    print("test-bridge.apk not found!")
    exit(0)

# Install test bridge
install_cmd = [
    config.ADB_PATH,
    'install', '-g', '-t', config.TEST_BRIDGE_PATH,
]
proc = subprocess.run(install_cmd, stdout=sys.stdout, stderr=sys.stderr)
if proc.returncode != 0:
    print("Failed to install test-bridge.apk!")
    exit(1)

# Run dynamic test
print("Running ICTDroid...")
print(ICTDROID_CMD)
proc = subprocess.run(ICTDROID_CMD, cwd=config.ROOT_PATH, stdout=sys.stdout, stderr=sys.stdout)
if proc.returncode != 0:
    print("Error when running ICTDroid")
else:
    print("Finshed running ICTDroid")
