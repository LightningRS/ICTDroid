#!/usr/bin/env python3
# ! -*- coding: utf-8 -*-
import base64
import binascii
import json
import os
import re
from typing import List, Dict, Optional, Set

import pandas as pd
import pydantic

import config
from log import logger
from lru_cache import LRUCache


class LogPathGroup(pydantic.BaseModel):
    """Log Group (only path)
    """
    sut: Optional[str]
    """generated-sut"""

    logcat: Optional[str]
    """logcat"""

    controller: Optional[str]
    """test-controller"""


class LogGroup(pydantic.BaseModel):
    """Log Group (content)
    """
    sut: Optional[str]
    """generated-sut"""

    logcat: Optional[str]
    """logcat"""

    controller: Optional[str]
    """test-controller"""


class CaseResult(pydantic.BaseModel):
    pkg_name: str
    """App package name"""

    comp_name: str
    """App component name"""

    comp_qualified_name: str
    """App component full-qualified name"""

    apk_index: Optional[int]
    """Apk index"""

    comp_index: Optional[int]
    """Component index"""

    case_index: Optional[int]
    """Testcase index"""

    strategy: Optional[str]
    """Strategy name"""

    uniq_err_id: Optional[int]
    """Unique error id"""

    uniq_crash_id: Optional[int]
    """Unique crash id"""

    comp_state: Optional[str]
    """Component final state"""

    mist_type: Optional[str]
    """Component MIST type"""

    exc_types: Set[str] = set()
    """Exception types"""

    exc_types_uniq: Set[str] = set()
    """Unique exception types"""

    exc_types_err: Set[str] = set()
    """Error-level exception types"""

    full_trace: Optional[List[str]]
    """Full stacktrace"""

    state_line: Optional[str]
    """Case result line"""


class TraceBlock(pydantic.BaseModel):
    pkg_name: str
    """App package name"""

    comp_name: str
    """App component name"""

    exc_types: Set[str] = set()
    """Exception types"""

    exc_level: Optional[str]
    """Stacktrace exception level"""

    trace_head: Optional[str]
    """Stacktrace head line"""

    trace_path: Optional[str]
    """Path information in stacktrace"""

    def __hash__(self):
        return hash('{}\n{}'.format(self.trace_head, self.trace_path))

    def __eq__(self, other):
        if not isinstance(other, TraceBlock):
            return False
        return self.trace_head == other.trace_head \
            and self.trace_path == other.trace_path


class ComponentSummary(pydantic.BaseModel):
    passed_times: int = 0
    jumped_times: int = 0
    failed_times: int = 0
    unique_exc_count: int = 0
    unique_exc_details: Dict[str, int] = dict()
    unique_err_count: int = 0
    unique_err_details: Dict[str, int] = dict()
    unique_crash_count: int = 0
    unique_crash_details: Dict[str, int] = dict()


class ControllerLogAnalyzer:
    def __init__(self, log_content: str, out_dir: str, testcase_dir: str):
        self.log_content = log_content
        self.out_dir = out_dir
        self.testcase_dir = testcase_dir
        self.mist_json: Optional[Dict[str, Dict[str, str]]] = None
        self.log_lines: List[str] = self.log_content.splitlines()

        self.curr_line_idx: int = 0
        self.curr_case: Optional[CaseResult] = None
        self.known_traces: Dict[str, Dict[str, List[CaseResult]]] = dict()

        self.all_cases: List[CaseResult] = list()
        self.uniq_exc_cnt = 0
        self.uniq_err_cnt = 0
        self.uniq_crash_cnt = 0

        self.uniq_exc_type_sum: Dict[str, int] = dict()
        self.uniq_err_type_sum: Dict[str, int] = dict()
        self.uniq_crash_type_sum: Dict[str, int] = dict()

        self.comp_case_cache: LRUCache[str, pd.DataFrame[str, str]] = LRUCache()

    def load_mist_result(self, mist_json_path: str):
        TAG = 'load_mist_result'

        with open(mist_json_path, 'r', encoding='utf-8') as f:
            self.mist_json = json.load(f)
        logger.info(TAG, f"Loaded mist result from: {mist_json_path}")

    def start(self):
        TAG = 'start'

        logger.info(TAG, "Reading and preprocessing test-controller log...")
        for self.curr_line_idx in range(len(self.log_lines)):
            self.parse_line()
        self.output_result()
        logger.info(TAG, f"Finished analyzing, detected totally {self.uniq_exc_cnt} unique exceptions")

    @staticmethod
    def base32_decode(b32_str: str):
        TAG = 'base32_decode'

        extra_paddings = 0
        while extra_paddings <= 6:
            try:
                return base64.b32decode(b32_str + ('=' * extra_paddings)).decode('utf-8')
            except binascii.Error:
                extra_paddings += 1
        logger.error(TAG, f"Failed to decode base32 string: {b32_str}")
        return None

    def format_testcase(self, t_case: CaseResult):
        TAG = 'format_testcase'

        case_rel_path = f"{t_case.pkg_name}/{t_case.comp_name}_{t_case.strategy}.csv"
        # Fallback result when failed to load testcase csv file
        fallback_res = f"**FAILED_TO_PARSE** in {case_rel_path}"

        case_df = self.comp_case_cache.get(t_case.comp_qualified_name)
        if case_df is None:
            case_path = os.path.join(self.testcase_dir, case_rel_path)
            if not os.path.exists(case_path):
                logger.error(TAG, f"Testcase csv file not found: {case_rel_path}")
                return fallback_res
            case_df = pd.read_csv(case_path, skiprows=6)
        if t_case.case_index >= len(case_df.index):
            logger.error(TAG, f"caseIndex {t_case.case_index} of {t_case.comp_qualified_name} exceeded!")
            return fallback_res
        case_row: pd.Series = case_df.iloc[t_case.case_index]
        case_dic = case_row.to_dict()

        # Parse category
        if 'category' in case_dic:
            category_lis: List[str] = list()
            category_keys = list(filter(lambda x: x.startswith('category_'), case_dic.keys()))
            for category_key in category_keys:
                if case_dic.get(category_key):
                    category_lis.append(self.base32_decode(category_key[len('category_'):]))
                # pop raw value
                case_dic.pop(category_key)
            category_lis.sort()
            if case_dic['category'] != '$#NULL#$':
                case_dic['category'] = category_lis

        # Parse extra
        if 'extra' in case_dic:
            extra_dic: Dict[str, str | Dict] = dict()
            extra_node_map: Dict[int, Dict] = {
                0: extra_dic,
            }
            extra_keys = sorted(filter(lambda x: x.startswith('extra_'), case_dic.keys()))
            for extra_key in extra_keys:
                extra_key_exp = r'^extra_(?P<parentId>\d+)_(?P<id>\d+)_(?P<name>[^_]+)_(?P<type>[^_]+)$'
                extra_key_res = re.match(extra_key_exp, extra_key)
                if extra_key_res is None:
                    logger.error(TAG, f"Unrecognized extra parameter [{extra_key}] of component "
                                      f"[{t_case.comp_qualified_name}]")
                    continue
                extra_parent_id = int(extra_key_res.group('parentId'))
                extra_id = int(extra_key_res.group('id'))
                extra_name = self.base32_decode(extra_key_res.group('name'))
                extra_type = self.base32_decode(extra_key_res.group('type'))

                if extra_parent_id not in extra_node_map:
                    continue
                parent_node: Dict = extra_node_map.get(extra_parent_id)

                extra_val_raw: str = case_dic.get(extra_key)
                if extra_type.lower() == 'bundle' and extra_val_raw != '$#NULL#$':
                    extra_val = dict()
                    extra_node_map[extra_id] = extra_val
                    extra_dic[extra_name] = extra_val
                else:
                    extra_val = extra_val_raw
                parent_node[extra_name] = extra_val
                # pop raw value
                logger.debug(TAG, f"Processed extra parameter [{extra_key}] -> [{extra_name}]")
                case_dic.pop(extra_key)
            if case_dic['extra'] != '$#NULL#$':
                case_dic['extra'] = extra_dic
        return json.dumps(case_dic, sort_keys=True, ensure_ascii=False, separators=(', ', ': '))

    def output_comp_summary(self):
        TAG = 'output_comp_summary'

        logger.info(TAG, "Generating component test summary...")
        comp_summary_all: Dict[str, Dict[str, ComponentSummary]] = dict()
        for t_case in self.all_cases:
            pkg_all = comp_summary_all.get(t_case.pkg_name)
            if pkg_all is None:
                pkg_all: Dict[str, ComponentSummary] = dict()
                comp_summary_all[t_case.pkg_name] = pkg_all
            comp_summary = pkg_all.get(t_case.comp_name)
            if comp_summary is None:
                comp_summary = ComponentSummary()
                pkg_all[t_case.comp_name] = comp_summary

            # times count
            if t_case.comp_state == 'PASSED':
                comp_summary.passed_times += 1
            if t_case.comp_state == 'FAILED':
                comp_summary.failed_times += 1
            if t_case.comp_state == 'JUMPED':
                comp_summary.jumped_times += 1

            # unique exc
            comp_summary.unique_exc_count += len(t_case.exc_types_uniq)
            for t in t_case.exc_types_uniq:
                if t not in comp_summary.unique_exc_details:
                    comp_summary.unique_exc_details[t] = 0
                comp_summary.unique_exc_details[t] += 1

            # unique err
            comp_summary.unique_err_count += len(t_case.exc_types_err)
            for t in t_case.exc_types_err:
                if t not in comp_summary.unique_err_details:
                    comp_summary.unique_err_details[t] = 0
                comp_summary.unique_err_details[t] += 1

            # unique crash
            if t_case.comp_state == 'FAILED':
                comp_summary.unique_crash_count += len(t_case.exc_types_err)
                for t in t_case.exc_types_err:
                    if t not in comp_summary.unique_crash_details:
                        comp_summary.unique_crash_details[t] = 0
                    comp_summary.unique_crash_details[t] += 1

        # Convert ComponentSummary to dict
        for pkg_name in comp_summary_all.keys():
            pkg_all = comp_summary_all.get(pkg_name)
            for comp_name in pkg_all.keys():
                pkg_all[comp_name] = pkg_all[comp_name].dict()

        logger.info(TAG, "Writing component test summary...")
        comp_sum_path = os.path.join(self.out_dir, "comp-summary.json")
        with open(comp_sum_path, 'w', encoding='utf-8') as f:
            json.dump(comp_summary_all, f, indent=4, ensure_ascii=False, sort_keys=True)
        logger.info(TAG, "Finished writing component test summary")

    def output_result(self):
        TAG = 'output_result'

        logger.info(TAG, f"Parsed totally {len(self.all_cases)} testcases")
        logger.info(TAG, "Generating report for unique error and unique crash...")

        def get_report_str(_case: CaseResult, is_crash: bool):
            report_str = f"Unique {'Crash' if is_crash else 'Error'} "
            report_str += f"#{_case.uniq_crash_id if is_crash else _case.uniq_err_id}\n"
            report_str += f"=====================\n"
            report_str += f"Component: {_case.comp_qualified_name}\n"
            report_str += f"Exception Types: {','.join(sorted(_case.exc_types_err))}\n"
            report_str += f"Stacktrace:\n"
            report_str += '\n'.join(_case.full_trace) + '\n'
            report_str += f"Index: apkIndex={_case.apk_index}, compIndex={_case.comp_index}\n"
            report_str += f"Testcases:\n"
            report_str += f"(caseIndex={_case.case_index}, strategy={_case.strategy}): {self.format_testcase(_case)}\n"
            return report_str

        ue_reports: Dict[int, str] = dict()
        """Unique Error Reports"""
        uc_reports: Dict[int, str] = dict()
        """Unique Crash Reports"""

        for t_case in self.all_cases:
            if len(t_case.exc_types) < 1:
                logger.debug(TAG, f"Success: case {t_case.case_index} of [{t_case.comp_qualified_name}]")
            elif len(t_case.exc_types_err) < 1:
                logger.debug(TAG, f"Ignored: case {t_case.case_index} of [{t_case.comp_qualified_name}]")
            # elif t_case.uniq_err_id is None and t_case.uniq_crash_id is None:
            #     logger.info(TAG, f"Non-unique (of uc#{t_case.uniq_err_id}): case {t_case.case_index} of "
            #                      f"[{t_case.comp_qualified_name}]")
            elif t_case.uniq_crash_id is not None:
                logger.debug(TAG, f"Unique crash #{t_case.uniq_crash_id}: case {t_case.case_index} of "
                                  f"[{t_case.comp_qualified_name}]")
                if t_case.uniq_crash_id in uc_reports:
                    uc_reports[t_case.uniq_crash_id] += f"(caseIndex={t_case.case_index}, strategy={t_case.strategy})" \
                                                        f": {self.format_testcase(t_case)}\n"
                else:
                    uc_reports[t_case.uniq_crash_id] = get_report_str(t_case, True)
            else:
                assert t_case.uniq_err_id is not None
                logger.debug(TAG, f"Unique error #{t_case.uniq_err_id}: case {t_case.case_index} of "
                                  f"[{t_case.comp_qualified_name}]")
                if t_case.uniq_err_id in ue_reports:
                    ue_reports[t_case.uniq_err_id] += f"(caseIndex={t_case.case_index}, strategy={t_case.strategy}): " \
                                                      f"{self.format_testcase(t_case)}\n"
                else:
                    ue_reports[t_case.uniq_err_id] = get_report_str(t_case, False)

        logger.info(TAG, "Writing report for unique error and unique crash...")
        uc_report_path = os.path.join(self.out_dir, 'unique-crash.txt')
        ue_report_path = os.path.join(self.out_dir, 'unique-error.txt')
        uc_sorted = list(sorted(uc_reports.items()))
        ue_sorted = list(sorted(ue_reports.items()))
        if not os.path.exists(self.out_dir):
            os.makedirs(self.out_dir)
        with open(uc_report_path, 'w', encoding='utf-8') as f:
            for uc_id, uc_report in uc_sorted:
                f.write(uc_report + '\n')
        with open(ue_report_path, 'w', encoding='utf-8') as f:
            for ue_id, ue_report in ue_sorted:
                f.write(ue_report + '\n')

        # Write type summary
        logger.info(TAG, "Writing exception types summary...")
        uniq_exc_sum_path = os.path.join(self.out_dir, 'exc-types-unique.json')
        uniq_err_sum_path = os.path.join(self.out_dir, 'exc-types-err.json')
        uniq_crash_sum_path = os.path.join(self.out_dir, 'exc-types-crash.json')
        with open(uniq_exc_sum_path, 'w', encoding='utf-8') as f:
            json.dump(self.uniq_exc_type_sum, f, indent=4, ensure_ascii=False, sort_keys=True)
        with open(uniq_err_sum_path, 'w', encoding='utf-8') as f:
            json.dump(self.uniq_err_type_sum, f, indent=4, ensure_ascii=False, sort_keys=True)
        with open(uniq_crash_sum_path, 'w', encoding='utf-8') as f:
            json.dump(self.uniq_crash_type_sum, f, indent=4, ensure_ascii=False, sort_keys=True)

        # Write component test summary
        self.output_comp_summary()

        logger.info(TAG, "Finished writing report")

    def parse_line(self):
        TAG = 'parse_line'

        if self.parse_line_start():
            return
        if self.parse_line_result():
            return

    def parse_line_start(self) -> bool:
        TAG = 'parse_line_start'

        log_line = self.log_lines[self.curr_line_idx]
        patt_exp = r'^.*Started component state monitor for compo(n*)ent \[(?P<compPath>[^\]]+)\]$'
        patt_res = re.match(patt_exp, log_line)
        if patt_res is None:
            return False
        comp_path = patt_res.group('compPath')
        assert '/' in comp_path
        pkg_name, comp_unqualified_name = comp_path.split('/')
        if not comp_unqualified_name.startswith('.'):
            comp_name = comp_unqualified_name
        else:
            comp_name = f'{pkg_name}{comp_unqualified_name}'
        self.curr_case = CaseResult(
            pkg_name=pkg_name,
            comp_name=comp_name,
            comp_qualified_name=f'{pkg_name}/{comp_name}',
        )
        self.all_cases.append(self.curr_case)
        if self.mist_json is not None:
            self.curr_case.mist_type = self.mist_json.get(pkg_name).get(comp_name)
        # logger.info(TAG, f"Detected case result: comp_name={self.curr_case.comp_qualified_name}")

    def parse_line_result(self) -> bool:
        """Get the case running result (state) and extract the stacktrace
        """
        TAG = 'parse_line_result'

        log_line = self.log_lines[self.curr_line_idx]
        if '] - Case ' not in log_line:
            return False
        patt_exp = r'^.*\] - Case (?P<state>[A-Z]+)!.*apkIndex=(?P<apkIndex>\d+), compIndex=(?P<compIndex>\d+), ' \
                   r'caseIndex=(?P<caseIndex>\d+), strategy=(?P<strategyName>.*)$'
        patt_res = re.match(patt_exp, log_line)
        if patt_res is None:
            return False
        assert self.curr_case is not None
        self.curr_case.state_line = log_line
        self.curr_case.comp_state = patt_res.group('state')
        self.curr_case.apk_index = int(patt_res.group('apkIndex'))
        self.curr_case.comp_index = int(patt_res.group('compIndex'))
        self.curr_case.case_index = int(patt_res.group('caseIndex'))
        self.curr_case.strategy = patt_res.group('strategyName')

        if self.curr_case.comp_state == 'FAILED':
            logger.debug(
                TAG,
                f"Detected FAILED result: id={self.curr_case.apk_index}/{self.curr_case.comp_index}/"
                f"{self.curr_case.case_index}"
                # f"{patt_res.group('state')}"
                f", state={self.curr_case.comp_state}, "
                f"comp={self.curr_case.comp_qualified_name}"
            )

        # Catch and check stacktrace
        self.fetch_stacktrace()
        self.check_stacktrace()

    def fetch_stacktrace(self):
        """Extract the stacktrace of current case
        """
        TAG = 'fetch_stacktrace'

        assert self.curr_case is not None
        trace_lis: List[str] = list()
        trace_line_idx = self.curr_line_idx - 1
        line_str = self.log_lines[trace_line_idx]
        while not line_str.startswith('[') and trace_line_idx >= 0:
            should_append = True
            trace_exp = r'^(?P<level>[A-Z]+) (?P<tag>[^:]+).*$'
            trace_res = re.match(trace_exp, line_str)
            if trace_res is None:
                logger.error(TAG, f"Unrecognized trace line in log: {line_str}")
                should_append = False
            if should_append:
                trace_level = trace_res.group('level')
                trace_tag = trace_res.group('tag')
                if trace_level in ('D',):
                    # Ignore debug log
                    should_append = False
            if should_append:
                trace_lis.append(line_str)
            trace_line_idx -= 1
            line_str = self.log_lines[trace_line_idx]

        if trace_line_idx < 0:
            logger.error(TAG, "Incomplete stacktrace! Maybe the test-controller log is broken!")
        if self.curr_case.comp_state == 'FAILED' and len(trace_lis) < 1:
            if 'No stacktrace is caught' in line_str:
                logger.warning(TAG, f"test-controller reported no stacktrace caught: case #{self.curr_case.case_index} "
                                    f"of {self.curr_case.comp_qualified_name}")
            else:
                logger.warning(TAG, f"No stacktrace after filtering: case #{self.curr_case.case_index} "
                                    f"of {self.curr_case.comp_qualified_name}")
        if len(trace_lis) > 0:
            logger.debug(TAG, f"Got {len(trace_lis)} lines of stacktrace")
            trace_lis.reverse()
            self.curr_case.full_trace = trace_lis

    def check_stacktrace(self):
        """Check the stacktrace for current case
        """
        TAG = 'check_stacktrace'

        if self.curr_case is None or self.curr_case.full_trace is None:
            return

        # Do the stacktrace check in reverse order
        trace_lis = self.curr_case.full_trace.copy()
        trace_lis.reverse()
        trace_blocks: List[TraceBlock] = list()
        trace_path_buffer: List[str] = list()
        curr_block: Optional[TraceBlock] = None

        # Ignore stacktrace of PASSED/JUMPED cases or cases without any error
        if all(not x.startswith('E ') for x in trace_lis) and self.curr_case.comp_state != 'FAILED':
            return

        for trace_line in trace_lis:
            if ': \tat' in trace_line:
                # This is a trace path line, append `at ...` into trace_path_buffer
                trace_path_buffer.append(trace_line[trace_line.find(': ') + 2:])
                if curr_block is None:
                    # Create a new TraceBlock
                    curr_block = TraceBlock(
                        pkg_name=self.curr_case.pkg_name,
                        comp_name=self.curr_case.comp_name,
                    )
            elif ' Caused by: ' in trace_line and curr_block is not None:
                # This is a `caused by` line, extract exception types and add them into curr_block.exc_types
                if curr_block is None:
                    logger.error(TAG, f"Unrecognized `caused by` line: {trace_line}")
                    continue
                exc_exp = r'^(?P<level>[A-Z]) (?P<tag>[^:]+): Caused by: (?P<exc>[^:\s]+).*$'
                exc_res = re.match(exc_exp, trace_line)
                curr_block.exc_types.add(exc_res.group('exc'))
            else:
                # This is the header of the current stacktrace block.
                if curr_block is None:
                    logger.error(TAG, f"Unrecognized isolated stacktrace line: {trace_line}")
                    continue
                # Merge trace_path of current block
                curr_block.trace_path = '\n'.join(trace_path_buffer)

                # Extract exception types from the header
                exc_exp = r'^(?P<level>[A-Z]) (?P<tag>[^:]+): (?P<exc>[^:\s]+).*$'
                exc_res = re.match(exc_exp, trace_line)
                if exc_res is None:
                    logger.error(TAG, f"Unrecognized stacktrace header: {trace_line}")
                    # Reset parser states
                    trace_path_buffer.clear()
                    curr_block = None
                    continue
                log_tag = exc_res.group('tag')
                exc_name = exc_res.group('exc')

                # Filter by log tag
                if ControllerLogAnalyzer.filter_tag(log_tag):
                    # Reset parser states
                    logger.debug(TAG, f"Filtered tag [{log_tag}] of component {curr_block.comp_name}")
                    trace_path_buffer.clear()
                    curr_block = None
                    continue

                # Update current block data
                curr_block.trace_head = trace_line
                curr_block.exc_types.add(exc_name)
                curr_block.exc_level = exc_res.group('level')

                # Filter by exc_types and trace_head
                if ControllerLogAnalyzer.filter_exc(curr_block):
                    logger.debug(TAG, f"Filtered exception {curr_block.exc_types} of component {curr_block.comp_name}")
                    # Reset parser states
                    trace_path_buffer.clear()
                    curr_block = None
                    continue

                # Update exc_types to curr_case
                self.curr_case.exc_types.update(curr_block.exc_types)
                if curr_block.exc_level in ('E', 'F'):
                    self.curr_case.exc_types_err.update(curr_block.exc_types)

                # Save trace block and reset parser states
                trace_blocks.append(curr_block)
                trace_path_buffer.clear()
                curr_block = None

        # Check whether left incomplete trace block
        if curr_block is not None or len(trace_path_buffer) > 0:
            logger.error(TAG, f"Incomplete trace block with {len(trace_path_buffer)} in trace_path_buffer: "
                              f"{str(dict(curr_block))}")

        if len(trace_blocks) < 1:
            return

        # Check whether pkg/comp name appeared in trace blocks
        if not self.check_trace_relevant_to_pkg(trace_blocks):
            logger.warning(TAG, f"Neither package nor component name exists in stacktrace! compName="
                                f"{self.curr_case.comp_qualified_name}")
            return

        # Check the uniqueness of the stacktrace
        self.check_unique(trace_blocks)

    def check_unique(self, trace_blocks: List[TraceBlock]):
        """Check the uniqueness of the stacktrace
        Return True if the stacktrace is unique, otherwise False
        """
        TAG = 'check_unique'

        # Merge path and exception types
        path_merged = ''
        exc_types: Set[str] = set()
        err_types: Set[str] = set()
        crash_types: Set[str] = set()
        for t_block in trace_blocks:
            if t_block.trace_path not in path_merged and t_block.exc_level == 'E':
                path_merged += t_block.trace_path + '\n'
            # Merge types
            exc_types.update(t_block.exc_types)
            if t_block.exc_level == 'E':
                err_types.update(t_block.exc_types)
                if self.curr_case.comp_state == 'FAILED':
                    crash_types.update(t_block.exc_types)

        if path_merged.strip() == '':
            logger.warning(TAG, f"No error-level stacktrace after filtering: case {self.curr_case.case_index} of "
                                f"{self.curr_case.comp_qualified_name}")
            return False

        # Check whether merged path is known or not
        known_case: Optional[CaseResult] = None
        known_traces = self.known_traces.get(self.curr_case.comp_qualified_name)
        if known_traces is None:
            known_traces: Dict[str, List[CaseResult]] = dict()
            self.known_traces[self.curr_case.comp_qualified_name] = known_traces
        known_cases = known_traces.get(path_merged)
        if known_cases is None:
            known_cases = list()
            known_traces[path_merged] = known_cases
        elif len(known_cases) > 0:
            known_case = known_cases[0]
        if self.curr_case in known_cases:
            logger.fatal(TAG, "Case should not in known_cases")
            raise RuntimeError("Case should not in known_cases")
        else:
            known_cases.append(self.curr_case)

        # Double-check whether the component state is different with the known case.
        # If the state changed from other to FAILED, we should replace the known case.
        if known_case is not None:
            if known_case.comp_state != 'FAILED' and self.curr_case.comp_state == 'FAILED':
                logger.warning(TAG, f"comp_state changed from {known_case.comp_state} to {self.curr_case.comp_state}: "
                                    f"case {self.curr_case.case_index} of {self.curr_case.comp_qualified_name}")
                known_cases[0] = self.curr_case
                known_cases[-1] = known_case
                known_case = None
            elif self.curr_case.comp_state != known_case.comp_state:
                logger.debug(TAG, f"Inconsistent comp_state! known={known_case.comp_state}, "
                                  f"current={self.curr_case.comp_state}: case {self.curr_case.case_index} of "
                                  f"{self.curr_case.comp_qualified_name}")

        if known_case is None:
            # Stacktrace (case) is unique
            # Treat all exc of current case as unique exc
            self.uniq_exc_cnt += len(exc_types)
            self.curr_case.exc_types_uniq.update(exc_types)

            if self.curr_case.comp_state == 'FAILED':
                """FAILED means crash, otherwise non-crash"""
                self.uniq_crash_cnt += 1
                self.curr_case.uniq_crash_id = self.uniq_crash_cnt
            else:
                self.uniq_err_cnt += 1
                self.curr_case.uniq_err_id = self.uniq_err_cnt

            # Update type summary
            for exc_type in exc_types:
                if exc_type not in self.uniq_exc_type_sum:
                    self.uniq_exc_type_sum[exc_type] = 0
                self.uniq_exc_type_sum[exc_type] += 1
            for err_type in err_types:
                if err_type not in self.uniq_err_type_sum:
                    self.uniq_err_type_sum[err_type] = 0
                self.uniq_err_type_sum[err_type] += 1
            for crash_type in crash_types:
                if crash_type not in self.uniq_crash_type_sum:
                    self.uniq_crash_type_sum[crash_type] = 0
                self.uniq_crash_type_sum[crash_type] += 1

            return True
        else:
            self.curr_case.uniq_err_id = known_case.uniq_err_id
            self.curr_case.uniq_crash_id = known_case.uniq_crash_id
            # if self.curr_case.comp_state == 'FAILED':
            #     self.uniq_crash_cnt += 1
            #     self.curr_case.uniq_crash_id = self.uniq_crash_cnt
        return False

    def check_trace_relevant_to_pkg(self, trace_blocks: List[TraceBlock]):
        TAG = 'check_trace_relevant_to_pkg'

        # Extract group id of the package
        if self.curr_case.pkg_name.count('.') > 1:
            pkg_group_id_idx = self.curr_case.pkg_name.find('.')
            pkg_group_id_idx = self.curr_case.pkg_name.find('.', pkg_group_id_idx + 1)
            pkg_group_id = self.curr_case.pkg_name[0:pkg_group_id_idx]
        else:
            pkg_group_id = self.curr_case.pkg_name
        for t_block in trace_blocks:
            if self.curr_case.comp_name in t_block.trace_head or self.curr_case.comp_name in t_block.trace_path:
                return True
            if self.curr_case.pkg_name in t_block.trace_head or self.curr_case.pkg_name in t_block.trace_path:
                return True
            if pkg_group_id in t_block.trace_path or pkg_group_id in t_block.trace_path:
                return True
            if 'android.util.SuperNotCalledException' in t_block.trace_head:
                logger.warning(TAG, f"SuperNotCalledException detected for component: {self.curr_case.comp_name}")
                return True
        return False

    @staticmethod
    def filter_tag(log_tag: str):
        """Filter specific tags in log.
        Return True if the log should be filtered, otherwise False
        """
        return log_tag in (
            'BroadcastQueue', 'BufferQueueProducer', 'cr_CompositorSurfaceMgr',
            'RunCaseHandler', 'EGL_adreno', 'CSVTestCaseUtil',
        )

    @staticmethod
    def filter_exc(t_block: TraceBlock):
        """Filter specific exceptions.
        Return True if the exception should be filtered, otherwise False
        """
        if 'java.lang.UnsatisfiedLinkError' in t_block.exc_types:
            return True

        if 'android.os.FileUriExposedException' in t_block.exc_types:
            return True

        if 'java.lang.Exception: Toast callstack!' in t_block.trace_head:
            return True

        if 'W System.err: java.lang.Exception' in t_block.trace_head:
            return True

        if 'org.square16.ictdroid.testbridge.utils.CSVTestCaseMgr$1' in t_block.trace_head:
            return True

        return False


class ICTDroidResultAnalyzer:
    def __init__(self, log_dir: Optional[str] = None, testcase_dir: Optional[str] = None):
        self.log_dir = config.LOG_PATH if log_dir is None else log_dir
        self.testcase_dir = config.TESTCASE_PATH if testcase_dir is None else testcase_dir
        self.log_path_groups: Dict[str, LogPathGroup] = dict()
        self.curr_log_group: Optional[LogGroup] = None
        pass

    def start(self):
        # Fetch logs
        self.fetch_log_groups()
        for group_id, log_group in self.log_path_groups.items():
            self.process_log_group(group_id, log_group)

    def fetch_log_groups(self):
        TAG = 'fetch_log_groups'

        for f_path in os.listdir(self.log_dir):
            if not f_path.endswith('.log'):
                continue
            group_id = 'default'
            exp = r'^(?P<logType>[A-Za-z0-9-]+)-(?P<logDate>\d+)T(?P<logTime>\d+)\.log'
            res = re.match(exp, f_path)
            if not res:
                log_type, _ = os.path.splitext(f_path)
            else:
                log_type = res.group('logType')
                group_id = f"{res.group('logDate')}T{res.group('logTime')}"
                if group_id not in self.log_path_groups:
                    self.log_path_groups[group_id] = LogPathGroup()
            log_group = self.log_path_groups[group_id]
            if log_type == 'generated-sut':
                log_group.sut = os.path.abspath(os.path.join(self.log_dir, f_path))
            elif log_type == 'logcat':
                log_group.logcat = os.path.abspath(os.path.join(self.log_dir, f_path))
            elif log_type == 'test-controller':
                log_group.controller = os.path.abspath(os.path.join(self.log_dir, f_path))
            else:
                logger.error(TAG, f"Error: unknown log type: {log_type}, filename={f_path}")
                exit(1)

        logger.info(TAG, f"There are {len(self.log_path_groups)} group(s) of log")

    def process_log_group(self, group_id: str, log_path_group: LogPathGroup):
        TAG = 'process_log_group'

        if log_path_group.sut is None:
            logger.error(TAG, f"Missing generated-sut log in {group_id}")
            return False
        if log_path_group.controller is None:
            logger.error(TAG, f"Missing test-controller log in {group_id}")
            return False

        result_dir = os.path.join(config.DATA_PATH, f"TestResult/{group_id}")
        # if os.path.exists(result_dir) and len(os.listdir(result_dir)) > 0:
        #     logger.warning(TAG, f"TestResult/{group_id} already exists! Skip analyze...")
        #     return True

        # Load log content
        self.curr_log_group = LogGroup()
        with open(log_path_group.sut, 'r', encoding='utf-8') as f:
            self.curr_log_group.sut = f.read()
        with open(log_path_group.controller, 'r', encoding='utf-8') as f:
            self.curr_log_group.controller = f.read()

        # At least one testcase execution result, treat as dynamic test log
        if self.is_done_once_dynamic_test():
            logger.info(TAG, f"Processing dynamic test log at: {group_id}")
            # Find corresponding generated-sut log
            corr_sut = self.fetch_corr_sut_content(group_id)
            if corr_sut is None:
                logger.fatal(TAG, f"Failed to find corresponding generated-sut log for dynamic test at: {group_id}")
                return False
            self.curr_log_group.sut = corr_sut
            self.analyze_dynamic_test_log(result_dir)

    def fetch_corr_sut_content(self, dyn_test_group_id: str) -> Optional[str]:
        TAG = 'fetch_corr_sut_content'

        group_ids = list(sorted(self.log_path_groups.keys()))
        dyn_idx = group_ids.index(dyn_test_group_id)
        while dyn_idx >= 0:
            path_group = self.log_path_groups.get(group_ids[dyn_idx])
            with open(path_group.sut, 'r', encoding='utf-8') as f:
                sut_content = f.read()
            if len(sut_content.strip()) > 0:
                logger.info(TAG,
                            f"Detected corresponding generated-sut log: {dyn_test_group_id} -> {group_ids[dyn_idx]}")
                return sut_content
            dyn_idx -= 1
        return None

    def is_done_once_dynamic_test(self):
        """Whether the log contains at least one testcase execution result.

        :return:
        """
        return 'Finished running testcase' in self.curr_log_group.controller

    def analyze_dynamic_test_log(self, result_dir: str):
        TAG = 'analyze_dynamic_test_log'

        ctrl_log = self.curr_log_group.controller
        log_analyzer = ControllerLogAnalyzer(ctrl_log, result_dir, self.testcase_dir)
        log_analyzer.start()
        logger.info(TAG, "====================================")


if __name__ == '__main__':
    analyzer = ICTDroidResultAnalyzer()
    analyzer.start()
