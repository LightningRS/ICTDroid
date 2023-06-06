#!/usr/bin/python3
# ! -*- coding: utf-8 -*-

import json
import os
import re
import shutil
import subprocess
import sys
from typing import Dict, List
from xml.etree import ElementTree as ET

import config


def run_apktool(apk_path: str):
    apk_name, _ = os.path.splitext(os.path.basename(apk_path))
    apktool_out_path = os.path.join(config.APKTOOL_RESULT_PATH, apk_name)
    if os.path.exists(apktool_out_path):
        print(f"[ICTDroid-Mist] APKTool result already exists, skip: {apk_path}")
        return True

    ## Run Apktool
    print(f"[ICTDroid-Mist] Start to run ApkTool on: {apk_path}")
    apktool_cmd = [
        config.JAVA_PATH,
        '-jar', config.APKTOOL_JAR_PATH,
        'd', '-o', apktool_out_path,
        apk_path,
    ]
    proc = subprocess.run(apktool_cmd, stdout=sys.stdout, stderr=sys.stdout)
    if proc.returncode != 0:
        print(f"[ICTDroid-Mist] Failed to run apktool: {apk_path}")
        return False
    print(f"[ICTDroid-Mist] Finished running apktool: {apk_path}")
    return True


def run_mist(apk_path: str):
    apk_name, _ = os.path.splitext(os.path.basename(apk_path))
    apktool_out_path = os.path.join(config.APKTOOL_RESULT_PATH, apk_name)
    print(f"[ICTDroid-Mist] Start to run Mist on: {apk_path}")

    mist_root_path = os.path.join(config.ROOT_PATH, 'lib/Mist')

    ## Prepare result path
    mist_res_path = os.path.join(config.MIST_RESULT_PATH, apk_name)
    mist_pea_path = os.path.join(mist_res_path, 'pea')
    mist_serialize_path = os.path.join(mist_res_path, 'serialize')
    mist_manifest_path = os.path.join(mist_res_path, 'manifest')
    if os.path.exists(mist_res_path):
        print(f"[ICTDroid-Mist] Mist result already exists, skip: {apk_path}")
        return True
    os.makedirs(mist_pea_path, exist_ok=True)
    os.makedirs(mist_serialize_path, exist_ok=True)
    os.makedirs(mist_manifest_path, exist_ok=True)

    ## Copy Manifest
    manifest_xml_path = os.path.join(apktool_out_path, 'AndroidManifest.xml')
    if not os.path.exists(manifest_xml_path):
        print(f"[ICTDroid-Mist] Failed: AndroidManifest.xml from ApkTool not found: {apk_path}")
        return False
    manifest_xml_to_path = os.path.join(mist_manifest_path, f'{apk_name}_manifest.xml')
    shutil.copy(manifest_xml_path, manifest_xml_to_path)

    ## Copy apk to mist_apk_path
    mist_apk_path = os.path.join(mist_root_path, 'apks')
    if os.path.exists(mist_apk_path):
        shutil.rmtree(mist_apk_path)
    os.makedirs(mist_apk_path)
    shutil.copy(apk_path, os.path.join(mist_apk_path, os.path.basename(apk_path)))

    ## Run Mist
    mist_cmd = [
        config.JAVA_8_PATH,
        '-jar', config.MIST_JAR_PATH,
        mist_apk_path,
        os.path.basename(apk_path),
        mist_pea_path,
        mist_manifest_path,
        mist_serialize_path,
    ]
    proc = subprocess.run(mist_cmd, cwd=config.MIST_ROOT_PATH, stdout=sys.stdout, stderr=sys.stdout)

    ### Remove sootOutput
    soot_output_path = os.path.join(config.MIST_ROOT_PATH, 'sootOutput')
    if os.path.exists(soot_output_path):
        shutil.rmtree(soot_output_path)

    ### Remove duplicated apk
    if os.path.exists(mist_apk_path):
        shutil.rmtree(mist_apk_path)

    if proc.returncode != 0:
        print(f"[ICTDroid-Mist] Failed to run Mist, skip: {apk_path}")
        return False

    print(f"[ICTDroid-Mist] Finished running Mist: {apk_path}")
    return True


def run_mist_result_analyzer(apk_path: str):
    apk_name, _ = os.path.splitext(os.path.basename(apk_path))
    print(f"[ICTDroid-Mist] Start to run Mist Result Analyzer on: {apk_path}")

    mist_res_path = os.path.join(config.MIST_RESULT_PATH, apk_name)
    mist_serialize_path = os.path.join(mist_res_path, 'serialize')
    mist_summarize_path = os.path.join(mist_res_path, 'summarize')
    mist_data_sum_path = os.path.join(mist_res_path, 'dataSummary')
    mist_misexpose_path = os.path.join(mist_res_path, 'misexpose')
    if os.path.exists(mist_misexpose_path) and len(os.listdir(mist_misexpose_path)) > 0:
        print(f"[ICTDroid-Mist] Mist analyzed result already exists, skip: {apk_path}")
        return True
    os.makedirs(mist_summarize_path, exist_ok=True)
    os.makedirs(mist_data_sum_path, exist_ok=True)
    os.makedirs(mist_misexpose_path, exist_ok=True)

    analyzer_cmd = [
        config.JAVA_8_PATH,
        '-jar', config.MIST_ANALYZER_JAR_PATH,
        mist_serialize_path,
        mist_summarize_path,
        mist_data_sum_path,
        mist_misexpose_path,
    ]
    proc = subprocess.run(analyzer_cmd, cwd=config.MIST_ROOT_PATH, stdout=sys.stdout, stderr=sys.stdout)
    if proc.returncode != 0:
        print(f"[ICTDroid-Mist] Failed to run Mist Result Analyzer, skip: {apk_path}")
        return False

    print(f"[ICTDroid-Mist] Finished running Mist Result Analyzer: {apk_path}")
    return True


def classify_ea(facts: Dict[str, bool]):
    if facts['mainAct']:
        result = 'mustEA'
        reason = '0 mainAct'
    elif (not facts['mainAct']) and facts['ifTrue'] and not facts['exTrue'] and facts['sysActNoData']:
        result = 'mustIA'
        reason = '1 ifTrue and not exTrue and sysActNoData'
    elif (not facts['mainAct']) and facts['noDefault']:
        result = 'mustIA'
        reason = '3 noDefault'
    elif facts['debug']:
        result = 'mustIA'
        reason = '3 debug'
    elif facts['clsInvoke'] or facts['actInvoke']:
        result = 'mustEA'
        reason = '4 clsInvoke or actInvoke'
    elif facts['clsDeclare']:
        result = 'mustEA'
        reason = '5 clsDeclare'
    elif facts['priority'] or facts['permission']:
        result = 'mustEA'
        reason = '6 priority or permission'
    elif facts['similar']:
        result = 'mayIA'
        reason = '7 similar'
    elif facts['highRatio']:
        result = 'mayIA'
        reason = '8 highRatio'
    elif facts['exTrue']:
        result = 'mayEA'
        reason = '9 exTrue'
    elif (not facts['exTrue']) and facts['ifTrue']:
        result = 'mayIA'
        reason = '10 ifTrue and not exTrue'
    else:
        result = None
        reason = None
    return result, reason


def convert_mist_result(apk_path: str):
    apk_name, _ = os.path.splitext(os.path.basename(apk_path))
    print(f"[ICTDroid-Mist] Start to convert Mist result on: {apk_path}")

    mist_res_path = os.path.join(config.MIST_RESULT_PATH, apk_name)
    mist_misexpose_path = os.path.join(mist_res_path, 'misexpose')
    res_content = ''

    for f_file in os.listdir(mist_misexpose_path):
        if not f_file.endswith('_misExpose.txt'):
            continue
        f_path = os.path.join(mist_misexpose_path, f_file)
        with open(f_path, 'r', encoding='utf-8') as f:
            f_content = f.read()

        facts: Dict[str, bool] = dict()
        for cond_name, cond_val in re.findall(r'(?P<condName>[A-Za-z0-9]+)\((?P<condVal>[A-Za-z0-9]+)\)\.', f_content):
            facts[cond_name] = (cond_val == 'true')
        cls_result, cls_reason = classify_ea(facts)

        if res_content != '':
            res_content += '\n'
        res_content += f'filename: {f_file}\n'
        res_content += f'reason: {cls_reason}\n'
        res_content += f'result: {cls_result}\n'
        print(f"[ICTDroid-Mist] Finished classifing Mist result on: {f_file}")

    mist_res_txt_path = os.path.join(mist_res_path, 'mist_result.txt')
    with open(mist_res_txt_path, 'w', encoding='utf-8') as f:
        f.write(res_content)
    print(f"[ICTDroid-Mist] Finished converting Mist result for apk: {apk_path}")
    return True


def merge_mist_result():
    result_files: List[str] = list()
    for f_root, _, f_files in os.walk(config.MIST_RESULT_PATH):
        for f_file in f_files:
            if f_file != 'mist_result.txt':
                continue
            result_files.append(os.path.join(f_root, f_file))

    res_dic = dict()
    for result_file in result_files:
        with open(result_file, 'r', encoding='utf-8') as f:
            res_lines = f.read().splitlines()
        i = 0
        while (i * 4 + 1) < len(res_lines):
            file_name = res_lines[i * 4].replace('filename: ', '').replace('_misExpose.txt', '')
            result = res_lines[i * 4 + 2].replace('result: ', '')
            comp_name = None
            pkg_name = None
            for apk_path in apks_lis:
                apk_name, _ = os.path.splitext(os.path.basename(apk_path))
                if f'{apk_name}_' in file_name:
                    comp_name = file_name.replace(f'{apk_name}_', '')
                    manifest_path = os.path.join(config.MIST_RESULT_PATH,
                                                 f'{apk_name}/manifest/{apk_name}_manifest.xml')
                    with open(manifest_path, 'r', encoding='utf-8') as f:
                        manifest_xml = ET.parse(f)
                    pkg_name = manifest_xml.getroot().attrib.get('package')
                    break
            if comp_name is None:
                print(f"[ICTDroid-Mist] Failed to get comp_name: {file_name}")
                i += 1
                continue
            if pkg_name is None:
                print(f"[ICTDroid-Mist] Failed to get pkg_name: {file_name}")
                i += 1
                continue
            print('[ICTDroid-Mist] Mist result for {}/{}: {}'.format(pkg_name, comp_name, result))
            if pkg_name not in res_dic:
                res_dic[pkg_name] = dict()
            res_dic[pkg_name][comp_name] = result
            i += 1

    out_json_path = os.path.join(config.MIST_RESULT_PATH, 'mist_result.json')
    with open(out_json_path, 'w', encoding='utf-8') as f:
        json.dump(res_dic, f, indent=4, sort_keys=True)

    shutil.copy(out_json_path, os.path.join(config.DATA_PATH, 'mist_result.json'))

    print(f"[ICTDroid-Mist] Finished merging Mist result")


if __name__ == '__main__':
    print("This is an optional step to perform MisExposure Prediction on components.")
    print("* It requires Java 1.8 to run the Mist Tool.")
    print("* Please make sure to set JAVA_8_PATH in config.py correctly.")
    print()

    if not os.path.exists(config.JAVA_8_PATH):
        print("[ICTDroid-Mist] Java 8 executable not found! Please modify JAVA_8_PATH in config.py!")
        exit(1)

    if not os.path.exists(config.APKTOOL_JAR_PATH):
        print("[ICTDroid-Mist] Missing lib/apktool.jar!")
        exit(1)

    if not os.path.exists(config.MIST_JAR_PATH):
        print("[ICTDroid-Mist] Missing lib/Mist/Mist.jar!")
        exit(1)

    if not os.path.exists(config.MIST_ANALYZER_JAR_PATH):
        print("[ICTDroid-Mist] Missing lib/Mist/MistResultAnalyzer.jar!")
        exit(1)

    os.makedirs(config.MIST_RESULT_PATH, exist_ok=True)
    os.makedirs(config.APKTOOL_RESULT_PATH, exist_ok=True)

    # List all apks
    apks_lis = []
    for root, dirs, files in os.walk(config.APKS_PATH):
        for file in files:
            if not file.endswith('.apk'):
                continue
            apks_lis.append(os.path.abspath(os.path.join(root, file)))

    # For all apks
    for apk_path in apks_lis:
        if not run_apktool(apk_path):
            continue
        if not run_mist(apk_path):
            continue
        if not run_mist_result_analyzer(apk_path):
            continue
        if not convert_mist_result(apk_path):
            continue

    # Merge Mist result
    merge_mist_result()
