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

package com.liferay.db.partitioning.kernel.util;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ServiceProxyFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Alberto Chaparro
 */
public class DBPartitioningUtil {

	public static void addPartition(long companyId) throws Exception {
		_dbPartitioning.addPartition(companyId);
	}

	public static boolean removePartition(long companyId) {
		return _dbPartitioning.removePartition(companyId);
	}

	public static Connection usePartition(Connection connection)
		throws SQLException {

		return _dbPartitioning.usePartition(connection);
	}

	public static void validate() throws Exception {
		_dbPartitioning.validate();
	}

	private static final boolean _DATABASE_PARTITIONING_ENABLED =
		GetterUtil.getBoolean(
			PropsUtil.get(PropsKeys.DATABASE_PARTITIONING_ENABLED));

	private static volatile DBPartitioning _dbPartitioning;

	static {
		if (_DATABASE_PARTITIONING_ENABLED) {
			_dbPartitioning = ServiceProxyFactory.newServiceTrackedInstance(
				DBPartitioning.class, DBPartitioningUtil.class,
				"_dbPartitioning", true);
		}
		else {
			_dbPartitioning = new DBPartitioning() {
			};
		}
	}

}