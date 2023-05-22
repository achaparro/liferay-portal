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

package com.liferay.portal.upgrade.online;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Kevin Lee
 */
public class OnlineUpgradeProcessFactory {

	public static OnlineUpgradeProcess addColumns(String... columnDefinitions) {
		return (tableName, onlineUpgradeSchemaDiff) -> {
			UpgradeProcess upgradeProcess = UpgradeProcessFactory.addColumns(
				tableName, columnDefinitions);

			upgradeProcess.upgrade();

			onlineUpgradeSchemaDiff.recordAddColumns(columnDefinitions);
		};
	}

	public static OnlineUpgradeProcess alterColumnName(
		String oldColumnName, String newColumnDefinition) {

		return (tableName, onlineUpgradeSchemaDiff) -> {
			UpgradeProcess upgradeProcess =
				UpgradeProcessFactory.alterColumnName(
					tableName, oldColumnName, newColumnDefinition);

			upgradeProcess.upgrade();

			onlineUpgradeSchemaDiff.recordAlterColumnName(
				oldColumnName, newColumnDefinition);
		};
	}

	public static OnlineUpgradeProcess alterColumnType(
		String columnName, String newColumnType) {

		return (tableName, onlineUpgradeSchemaDiff) -> {
			UpgradeProcess upgradeProcess =
				UpgradeProcessFactory.alterColumnType(
					tableName, columnName, newColumnType);

			upgradeProcess.upgrade();

			onlineUpgradeSchemaDiff.recordAlterColumnType(
				columnName, newColumnType);
		};
	}

	public static OnlineUpgradeProcess dropColumns(String... columnNames) {
		return (tableName, onlineUpgradeSchemaDiff) -> {
			UpgradeProcess upgradeProcess = UpgradeProcessFactory.dropColumns(
				tableName, columnNames);

			upgradeProcess.upgrade();

			onlineUpgradeSchemaDiff.recordDropColumns(columnNames);
		};
	}

}