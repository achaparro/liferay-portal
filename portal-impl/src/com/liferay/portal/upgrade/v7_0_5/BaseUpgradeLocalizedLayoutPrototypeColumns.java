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

package com.liferay.portal.upgrade.v7_0_5;

import com.liferay.portal.kernel.upgrade.BaseUpgradeLocalizedColumn;
import com.liferay.portal.upgrade.v7_0_5.util.LayoutPrototypeTable;

import java.sql.Types;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * @author Mariano Alvaro Saiz
 */
public abstract class BaseUpgradeLocalizedLayoutPrototypeColumns
	extends BaseUpgradeLocalizedColumn {

	@Override
	protected void executeClobUpgrades() throws Exception {
		if (!_ALREADY_EXECUTED.get()) {
			if (_isAlterNeeded(LayoutPrototypeTable.class, "name")) {
				alter(
					LayoutPrototypeTable.class,
					new AlterColumnType("name", _NAME_TYPE));
			}

			if (_isAlterNeeded(LayoutPrototypeTable.class, "description")) {
				alter(
					LayoutPrototypeTable.class,
					new AlterColumnType("description", _DESCRIPTION_TYPE));
			}

			_ALREADY_EXECUTED.set(true);
		}
	}

	private boolean _isAlterNeeded(Class<?> tableClass, String columnName)
		throws Exception {

		int columnType = getColumnDataType(tableClass, columnName);

		IntStream intStream = IntStream.of(_INVALID_TYPES);

		if (intStream.anyMatch(x -> x == columnType)) {
			return true;
		}

		return false;
	}

	private static final AtomicBoolean _ALREADY_EXECUTED = new AtomicBoolean(
		false);

	private static final String _DESCRIPTION_TYPE = "TEXT null";

	private static final int[] _INVALID_TYPES =
		{Types.CHAR, Types.NCHAR, Types.NVARCHAR, Types.VARCHAR};

	private static final String _NAME_TYPE = "TEXT null";

}