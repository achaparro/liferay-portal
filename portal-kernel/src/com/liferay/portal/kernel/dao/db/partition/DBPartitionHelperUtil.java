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

package com.liferay.portal.kernel.dao.db.partition;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ServiceProxyFactory;

import java.sql.Connection;

/**
 * @author Alberto Chaparro
 */
public class DBPartitionHelperUtil {

	public static void addPartition(long companyId) {
		_dbPartitionHelper.addPartition(companyId);
	}

	public static boolean removePartition(long companyId) {
		return _dbPartitionHelper.removePartition(companyId);
	}

	public static void setDefaultCompanyId(long companyId) {
		_dbPartitionHelper.setDefaultCompanyId(companyId);
	}

	public static void usePartition(Connection connection) {
		_dbPartitionHelper.usePartition(connection);
	}

	public static void validate() {
		_dbPartitionHelper.validate();
	}

	private static final boolean _DATABASE_PARTITION_ENABLED =
		GetterUtil.getBoolean(PropsUtil.get("database.partition.enabled"));

	private static volatile DBPartitionHelper _dbPartitionHelper;

	static {
		if (_DATABASE_PARTITION_ENABLED) {
			_dbPartitionHelper = ServiceProxyFactory.newServiceTrackedInstance(
				DBPartitionHelper.class, DBPartitionHelperUtil.class,
				"_dbPartitionHelper", true);
		}
		else {
			_dbPartitionHelper = new DBPartitionHelper() {
			};
		}
	}

}