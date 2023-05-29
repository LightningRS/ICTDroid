#!/usr/bin/python3
# ! -*- coding: utf-8 -*-

import logging
import os
import logging.config
import coloredlogs


class LoggerCtrl:
    def __init__(self):
        self.log_level = logging.DEBUG if os.environ.get('ICTDROID_DEBUG') == '1' else logging.INFO
        coloredlogs.install(level=self.log_level, fmt='%(asctime)s [%(levelname)s] %(name)s: %(message)s')
        self.logger = logging.getLogger('ICTDroid')
        self.logger.setLevel(self.log_level)

    def enable_file(self, file_path: str = None):
        os.makedirs('./logs', exist_ok=True)
        fh = logging.FileHandler('logs/output.log' if not file_path else file_path)
        fh.setLevel(self.log_level)
        fh.setFormatter(logging.Formatter('%(asctime)s [%(levelname)s] %(name)s: %(message)s'))
        self.logger.addHandler(fh)

    def get_logger(self):
        return self.logger

    def debug(self, tag: str, msg: str):
        self.logger.debug("[{}] {}".format(tag, msg))

    def info(self, tag: str, msg: str):
        self.logger.info("[{}] {}".format(tag, msg))

    def warning(self, tag: str, msg: str):
        self.logger.warning("[{}] {}".format(tag, msg))

    def warn(self, tag: str, msg: str):
        self.logger.warning("[{}] {}".format(tag, msg))

    def error(self, tag: str, msg: str):
        self.logger.error("[{}] {}".format(tag, msg))

    def fatal(self, tag: str, msg: str):
        self.logger.critical("[{}] {}".format(tag, msg))

    def critical(self, tag: str, msg: str):
        self.logger.critical("[{}] {}".format(tag, msg))


logger = LoggerCtrl()
