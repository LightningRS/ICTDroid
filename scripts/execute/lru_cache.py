#!/usr/bin/python3
# ! -*- coding: utf-8 -*-

import threading
from collections import OrderedDict
from typing import Optional, TypeVar, Generic

CacheKey = TypeVar("CacheKey")
CacheValue = TypeVar("CacheValue")


class LRUCache(Generic[CacheKey, CacheValue]):
    def __init__(self, capacity: int = 32):
        """LRUCache constructor

        :param capacity: Capacity of the cache, default to 32
        """
        self._cache: OrderedDict[CacheKey, CacheValue] = OrderedDict()
        self._cache_lock = threading.Lock()
        self._capacity = capacity

    def get(self, key: CacheKey) -> Optional[CacheValue]:
        if key not in self._cache:
            return None
        else:
            with self._cache_lock:
                self._cache.move_to_end(key)
            return self._cache.get(key)

    def put(self, key: CacheKey, value: CacheValue) -> None:
        with self._cache_lock:
            self._cache[key] = value
            self._cache.move_to_end(key)
            while len(self._cache) > self._capacity:
                self._cache.popitem(last=False)

    def pop(self, key: CacheKey) -> None:
        with self._cache_lock:
            if key in self._cache:
                self._cache.pop(key)

    def __contains__(self, item):
        return item in self._cache

    def __setitem__(self, key, value):
        return self.put(key, value)

    def clear(self) -> None:
        with self._cache_lock:
            self._cache.clear()
