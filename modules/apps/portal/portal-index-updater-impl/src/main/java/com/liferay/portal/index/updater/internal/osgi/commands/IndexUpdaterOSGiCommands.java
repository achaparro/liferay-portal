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

package com.liferay.portal.index.updater.internal.osgi.commands;

import com.liferay.portal.index.updater.IndexUpdater;
import com.liferay.portal.index.updater.exception.NotLiferayServiceException;

import org.apache.felix.service.command.Descriptor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Ricardo Couso
 */
@Component(
	immediate = true,
	property = {
		"osgi.command.function=updateIndexes",
		"osgi.command.function=updateIndexesAll", "osgi.command.scope=upgrade"
	},
	service = IndexUpdaterOSGiCommands.class
)
public class IndexUpdaterOSGiCommands {

	@Descriptor("Update database indexes for a specific module via bundle id")
	public void updateIndexes(long bundleId) {
		try {
			_indexUpdater.updateIndexes(bundleId);
		}
		catch (IllegalArgumentException | NotLiferayServiceException e) {
			System.out.println(e.getMessage());
		}
	}

	@Descriptor(
		"Update database indexes for specific a module via symbolic name"
	)
	public void updateIndexes(String bundleSymbolicName) {
		try {
			_indexUpdater.updateIndexes(bundleSymbolicName);
		}
		catch (IllegalArgumentException | NotLiferayServiceException e) {
			System.out.println(e.getMessage());
		}
	}

	@Descriptor("Update database indexes for all modules")
	public void updateIndexesAll() {
		_indexUpdater.updateIndexesAll();
	}

	@Reference
	private IndexUpdater _indexUpdater;

}