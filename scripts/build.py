#!/usr/bin/env python3
#! -*- coding: utf-8 -*-

import os
import platform
import shutil
import zipfile
import requests
from xml.etree import ElementTree as ET

from log import logger

class ICTDroidBuilder:
    ANDROID_REPO_URL = 'https://dl.google.com/android/repository/'

    def __init__(self) -> None:
        logger.enable_file()
        self.root_dir = os.path.abspath(os.path.dirname(os.path.dirname(__file__)))
        self.build_root = os.path.join(self.root_dir, 'build')
        self.apks_root = os.path.join(self.build_root, 'apks')
        self.data_root = os.path.join(self.build_root, 'data')
        self.lib_path = os.path.join(self.build_root, 'lib')
        self.script_path = os.path.join(self.build_root, 'scripts')
        os.chdir(self.root_dir)

    def start(self):
        self.init_dirs()
        self.build_iccbot()
        self.build_adb()
        self.build_controller()
        self.build_bridge()
    
    def init_dirs(self):
        TAG = 'init_dirs'

        os.makedirs(self.build_root, exist_ok=True)

        os.makedirs(self.apks_root, exist_ok=True)
        self.write_readme_apks(self.apks_root)

        os.makedirs(self.data_root, exist_ok=True)
        self.write_readme_data(self.data_root)
        os.makedirs(os.path.join(self.data_root, 'ICCResult'), exist_ok=True)
        os.makedirs(os.path.join(self.data_root, 'Testcases'), exist_ok=True)
        
        os.makedirs(self.lib_path, exist_ok=True)

        os.makedirs(self.script_path, exist_ok=True)

        logger.info(TAG, "Finished initializing directories")

    def clean_all(self):
        TAG = 'clean_all'

        shutil.rmtree(self.build_root)
        logger.info(TAG, "Finished cleaning all")
    
    def build_iccbot(self):
        TAG = 'build_iccbot'

        iccbot_path = os.path.join(self.lib_path, 'ICCBot')
        iccbot_jar_path = os.path.join(iccbot_path, 'ICCBot.jar')
        if os.path.exists(iccbot_path) and os.path.exists(iccbot_jar_path):
            logger.info(TAG, "ICCBot.jar already exists, skip build")
            return
        
        # Unzip ICCBot
        iccbot_pack_path = os.path.join(self.root_dir, 'lib/ICCBot.zip')
        if not os.path.exists(iccbot_pack_path):
            logger.error(TAG, "Failed to build ICCBot: lib/ICCBot.zip not found!")
            return
        logger.info(TAG, "Unzipping ICCBot...")
        with zipfile.ZipFile(iccbot_pack_path) as f:
            f.extractall(iccbot_path)
        logger.info(TAG, "Finished unzipping ICCBot")

        logger.info(TAG, "Finished building ICCBot")
    
    def build_adb(self):
        TAG = 'build_adb'

        # Fetch the latest platform tools from android repository
        try:
            repo_xml_text = requests.get(f'{ICTDroidBuilder.ANDROID_REPO_URL}/repository2-1.xml').text
        except KeyboardInterrupt:
            raise
        except Exception:
            logger.error(TAG, "Failed to fetch Android repository xml!")
            return
        
        repo_xml = ET.fromstring(repo_xml_text)
        platform_tools_node = repo_xml.find('remotePackage[@path="platform-tools"]')
        if platform_tools_node is None:
            logger.error(TAG, "Could not find latest platform-tools in repository xml!")
            return
        
        target_host_os = None
        if platform.system().lower() == 'linux':
            target_host_os = 'linux'
        elif platform.system().lower().startswith('win'):
            target_host_os = 'windows'
        elif platform.system().lower().startswith('mac'):
            target_host_os = 'macosx'
        
        if target_host_os is None:
            logger.error(TAG, f"Could not detect host_os: {platform.system().lower()}")
            return
        
        download_url = None
        for archive_node in platform_tools_node.findall('archives/archive'):
            if archive_node.find('host-os').text != target_host_os:
                continue
            download_url = archive_node.find('complete/url').text
            break
        
        if download_url is None:
            logger.error(TAG, f"Failed to find url of platform-tools for host-os: {target_host_os}")
            return
        
        download_url = f'{ICTDroidBuilder.ANDROID_REPO_URL}/{download_url}'
        logger.info(TAG, f"Downloading platform-tools from: {download_url}")
        platform_tools_zip = requests.get(download_url)
        tools_path = os.path.join(self.root_dir, 'lib/platform-tools.zip')
        with open(tools_path, 'wb') as f:
            f.write(platform_tools_zip.content)
        logger.info(TAG, "Finished downloading platform-tools")

        logger.info(TAG, "Begin extracting platform-tools")
        with zipfile.ZipFile(tools_path) as f:
            for item in f.filelist:
                if os.path.basename(item.filename).lower().startswith('adb'):
                    item.filename = os.path.basename(item.filename)
                    logger.info(TAG, f"Extracting {item.filename}...")
                    f.extract(item, self.build_root)
        logger.info(TAG, "Finished extracting platform-tools")

        logger.info(TAG, "Finished building adb")

    def build_controller(self):
        TAG = 'build_controller'

        ctrl_root = os.path.join(self.root_dir, 'test-controller')
        pom_xml_path = os.path.join(ctrl_root, 'pom.xml')
        if not os.path.exists(ctrl_root) or not os.path.exists(pom_xml_path):
            logger.error(TAG, 'Failed to build test-controller: test-controller not exists!')
            return
        mvn_path = shutil.which('mvn')
        if mvn_path is None:
            logger.error(TAG, "Failed to build test-controller: mvn not found!")
            return
        
        logger.info(TAG, "Start to build ictdroid.jar")
        os.chdir(ctrl_root)
        ret_code = os.system('mvn clean')
        if ret_code != 0:
            logger.error(TAG, f"Failed to build test-controller: mvn clean returned with non-zero code: {ret_code}")
            return
        ret_code = os.system('mvn package')
        if ret_code != 0:
            logger.error(TAG, f"Failed to build test-controller: mvn package returned with non-zero code: {ret_code}")
            return
        os.chdir(self.root_dir)
        
        target_path = os.path.join(ctrl_root, 'target')
        jar_path = None
        for f_item in os.listdir(target_path):
            if f_item.endswith('-with-dependencies.jar'):
                jar_path = os.path.join(target_path, f_item)
                break
        if jar_path is None:
            logger.error(TAG, "Failed to build test-controller: packaged jar file not found!")
            return
        target_jar_path = os.path.join(self.build_root, 'ictdroid.jar')
        if os.path.exists(target_jar_path):
            os.unlink(target_jar_path)
        shutil.copyfile(jar_path, target_jar_path)

        # Copy scripts
        target_scripts_path = os.path.join(self.build_root, 'scripts')
        scripts_path = os.path.join(ctrl_root, 'scripts')
        shutil.copytree(scripts_path, target_scripts_path, dirs_exist_ok=True)
        
        logger.info(TAG, "Finished building ictdroid.jar")

    def build_bridge(self):
        TAG = 'build_bridge'

        bridge_root = os.path.join(self.root_dir, 'test-bridge')
        build_gradle_path = os.path.join(bridge_root, 'build.gradle')
        if not os.path.exists(bridge_root) or not os.path.exists(build_gradle_path):
            logger.error(TAG, 'Failed to build test-bridge: test-bridge not exists!')
            return
        gradlew_path = os.path.join(bridge_root, 'gradlew')
        if platform.system().lower().startswith('win'):
            gradlew_path = os.path.join(bridge_root, 'gradlew.bat')
        if not os.path.exists(gradlew_path):
            logger.error(TAG, "Failed to build test-bridge: gradlew not found!")
            return
        
        logger.info(TAG, "Start to build test-bridge.apk")
        os.chdir(bridge_root)
        ret_code = os.system('gradlew assembleRelease')
        if ret_code != 0:
            logger.error(TAG, f"Failed to build test-bridge: gradlew returned with non-zero code: {ret_code}")
            return
        os.chdir(self.root_dir)
        
        output_path = os.path.join(bridge_root, 'app/build/outputs')
        apk_path = None
        for f_root, _, f_files in os.walk(output_path):
            for f_file in f_files:
                if f_file.endswith('-release.apk'):
                    apk_path = os.path.join(f_root, f_file)
                    break
        if apk_path is None:
            logger.error(TAG, "Failed to build test-bridge: release apk not found!")
            return
        target_apk_path = os.path.join(self.build_root, 'test-bridge.apk')
        if os.path.exists(target_apk_path):
            os.unlink(target_apk_path)
        shutil.copyfile(apk_path, target_apk_path)
        
        logger.info(TAG, "Finished building test-bridge.apk")

    def write_readme_apks(self, apks_root: str):
        TAG = 'write_readme_apks'

        content = '''This is the APKs ROOT of ICTDroid.
* Place the apk(s) to be tested here.'''
        readme_path = os.path.join(apks_root, 'README.txt')
        with open(readme_path, 'w', encoding='utf-8') as f:
            f.write(content)
        logger.info(TAG, "Wrote README.txt to build/apks")

    def write_readme_data(self, data_root: str):
        TAG = 'write_readme_data'

        content = '''This is the data ROOT of ICTDroid.
* Place the result(s) from ICCBot to `ICCResult` directory.
* Test cases generated by ICTDroid will be saved to `Testcases` directory by default.
'''
        readme_path = os.path.join(data_root, 'README.txt')
        with open(readme_path, 'w', encoding='utf-8') as f:
            f.write(content)
        logger.info(TAG, "Wrote README.txt to build/data")


if __name__ == '__main__':
    builder = ICTDroidBuilder()
    builder.start()
