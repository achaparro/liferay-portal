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

package com.liferay.sharding.kernel.util;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ServiceProxyFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Alberto Chaparro
 */
public class ShardingUtil {

	public static void addShard(long companyId) throws Exception {
		_sharding.addShard(companyId);
	}

	public static Connection useShard(Connection connection)
		throws SQLException {

		return _sharding.useShard(connection);
	}

	public static void validate() throws Exception {
		_sharding.validate();
	}

	private static final boolean _SHARDING_ENABLED = GetterUtil.getBoolean(
		PropsUtil.get(PropsKeys.SHARDING_ENABLED));

	private static volatile Sharding _sharding;

	static {
		if (_SHARDING_ENABLED) {
			_sharding = ServiceProxyFactory.newServiceTrackedInstance(
				Sharding.class, ShardingUtil.class, "_sharding", true);
		}
		else {
			_sharding = new Sharding() {
			};
		}
	}

}