#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import config
import json

# Summary testcases
case_sum = 0
app_cnt = 0
comp_cnt = 0
for a_root, a_dirs, __ in os.walk(config.TESTCASE_PATH):
    for app_dir in a_dirs:
        app_case_sum = 0
        comp_sum_dic = dict()

        app_dir_full = os.path.join(a_root, app_dir)
        for root, __, files in os.walk(app_dir_full):
            for file in files:
                if not file.endswith(".csv"):
                    continue
                comp_name = file[:file.rfind('_')]
                full_path = os.path.join(root, file)
                with open(full_path, "r") as f:
                    content = f.read()
                cnt = content.count("\n") - 7

                if comp_name not in comp_sum_dic:
                    comp_sum_dic[comp_name] = cnt
                else:
                    comp_sum_dic[comp_name] += cnt
                app_case_sum += cnt
        print("App [{}] detail:\n{}".format(app_dir, json.dumps(comp_sum_dic, indent=4)))
        print("App [{}] sum: There are {} testcases in {} components".format(app_dir, app_case_sum, len(comp_sum_dic)))
        case_sum += app_case_sum
        app_cnt += 1
        comp_cnt += len(comp_sum_dic)
print("Total sum: There are {} testcases in {} apps with {} components".format(
    case_sum, app_cnt, comp_cnt
))