/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.cache.memory;

import com.liferay.portal.kernel.cache.AbstractPortalCache;
import com.liferay.portal.kernel.cache.CacheListener;
import com.liferay.portal.kernel.cache.CacheListenerScope;
import com.liferay.portal.kernel.concurrent.ConcurrentHashSet;

import java.io.Serializable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Brian Wing Shun Chan
 * @author Edward Han
 * @author Shuyang Zhou
 */
public class MemoryPortalCache<K extends Serializable, V>
	extends AbstractPortalCache<K, V> {

	public MemoryPortalCache(String name, int initialCapacity) {
		_name = name;
		_concurrentMap = new ConcurrentHashMap<K, V>(initialCapacity);
	}

	public void destroy() {
		removeAll();

		_cacheListeners = null;
		_concurrentMap = null;
		_name = null;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public void registerCacheListener(CacheListener<K, V> cacheListener) {
		_cacheListeners.add(cacheListener);
	}

	@Override
	public void registerCacheListener(
		CacheListener<K, V> cacheListener,
		CacheListenerScope cacheListenerScope) {

		registerCacheListener(cacheListener);
	}

	@Override
	public void removeAll() {
		_concurrentMap.clear();

		for (CacheListener<K, V> cacheListener : _cacheListeners) {
			cacheListener.notifyRemoveAll(this);
		}
	}

	@Override
	public void unregisterCacheListener(CacheListener<K, V> cacheListener) {
		_cacheListeners.remove(cacheListener);
	}

	@Override
	public void unregisterCacheListeners() {
		_cacheListeners.clear();
	}

	@Override
	protected V doGet(K key) {
		return _concurrentMap.get(key);
	}

	@Override
	protected void doPut(K key, V value, int timeToLive, boolean quiet) {
		V oldValue = _concurrentMap.put(key, value);

		if (!quiet) {
			notifyPutEvents(key, value, oldValue != null);
		}
	}

	@Override
	protected V doPutIfAbsent(K key, V value, int timeToLive) {
		V oldValue = _concurrentMap.putIfAbsent(key, value);

		if (oldValue == null) {
			notifyPutEvents(key, value, false);
		}

		return oldValue;
	}

	@Override
	protected void doRemove(K key) {
		V value = _concurrentMap.remove(key);

		if (value != null) {
			for (CacheListener<K, V> cacheListener : _cacheListeners) {
				cacheListener.notifyEntryRemoved(this, key, value);
			}
		}
	}

	@Override
	protected boolean doRemove(K key, V value) {
		boolean removed = _concurrentMap.remove(key, value);

		if (removed) {
			for (CacheListener<K, V> cacheListener : _cacheListeners) {
				cacheListener.notifyEntryRemoved(this, key, value);
			}
		}

		return removed;
	}

	@Override
	protected V doReplace(K key, V value, int timeToLive) {
		V oldValue = _concurrentMap.replace(key, value);

		if (oldValue != null) {
			notifyPutEvents(key, value, true);
		}

		return oldValue;
	}

	@Override
	protected boolean doReplace(K key, V oldValue, V newValue, int timeToLive) {
		boolean replaced = _concurrentMap.replace(key, oldValue, newValue);

		if (replaced) {
			notifyPutEvents(key, newValue, true);
		}

		return replaced;
	}

	protected void notifyPutEvents(K key, V value, boolean updated) {
		if (updated) {
			for (CacheListener<K, V> cacheListener : _cacheListeners) {
				cacheListener.notifyEntryUpdated(this, key, value);
			}
		}
		else {
			for (CacheListener<K, V> cacheListener : _cacheListeners) {
				cacheListener.notifyEntryPut(this, key, value);
			}
		}
	}

	private Set<CacheListener<K, V>> _cacheListeners =
		new ConcurrentHashSet<CacheListener<K, V>>();
	private ConcurrentMap<K, V> _concurrentMap;
	private String _name;

}