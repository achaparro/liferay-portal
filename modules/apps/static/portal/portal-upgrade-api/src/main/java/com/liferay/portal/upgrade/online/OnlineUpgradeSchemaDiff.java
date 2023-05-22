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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kevin Lee
 */
public class OnlineUpgradeSchemaDiff {

	public OnlineUpgradeSchemaDiff(String[] columnNames) {
		for (String columnName : columnNames) {
			_columnDiffMap.put(columnName, new ColumnDiff(false));
		}
	}

	public String[] getAddedColumnNames() {
		List<String> addedColumnNames = new ArrayList<>();

		for (Map.Entry<String, ColumnDiff> entry : _columnDiffMap.entrySet()) {
			ColumnDiff columnDiff = entry.getValue();

			if (columnDiff.isAdded()) {
				addedColumnNames.add(entry.getKey());
			}
		}

		return ArrayUtil.toStringArray(addedColumnNames);
	}

	public String getNewColumnName(String columnName) {
		ColumnDiff columnDiff = _columnDiffMap.get(columnName);

		if (columnDiff == null) {
			return columnName;
		}

		return columnDiff.getNewColumnName();
	}

	public boolean isDroppedColumn(String columnName) {
		ColumnDiff columnDiff = _columnDiffMap.get(columnName);

		if (columnDiff == null) {
			return false;
		}

		return columnDiff.isDropped();
	}

	protected void recordAddColumns(String... columnDefinitions) {
		for (String columnDefinition : columnDefinitions) {
			String columnName = columnDefinition.substring(
				0, columnDefinition.indexOf(StringPool.SPACE));

			if (_columnDiffMap.containsKey(columnName)) {
				continue;
			}

			ColumnDiff columnDiff = new ColumnDiff(true);

			columnDiff.setNewColumnType(
				columnDefinition.substring(
					columnDefinition.indexOf(StringPool.SPACE) + 1));

			_columnDiffMap.put(columnName, columnDiff);
		}
	}

	protected void recordAlterColumnName(
		String oldColumnName, String newColumnDefinition) {

		ColumnDiff columnDiff = _columnDiffMap.get(oldColumnName);

		if ((columnDiff == null) || columnDiff.isDropped()) {
			return;
		}

		String newColumnName = newColumnDefinition.substring(
			0, newColumnDefinition.indexOf(StringPool.SPACE));

		columnDiff.setNewColumnName(newColumnName);

		_columnDiffMap.put(newColumnName, columnDiff);
	}

	protected void recordAlterColumnType(
		String columnName, String newColumnType) {

		ColumnDiff columnDiff = _columnDiffMap.get(columnName);

		columnDiff.setNewColumnType(newColumnType);
	}

	protected void recordDropColumns(String... columnNames) {
		for (String columnName : columnNames) {
			ColumnDiff columnDiff = _columnDiffMap.get(columnName);

			if (columnDiff == null) {
				continue;
			}

			columnDiff.setDropped(true);
		}
	}

	private final Map<String, ColumnDiff> _columnDiffMap = new HashMap<>();

	private class ColumnDiff {

		public ColumnDiff(boolean added) {
			_added = added;
		}

		public String getNewColumnName() {
			return _newColumnName;
		}

		public String getNewColumnType() {
			return _newColumnType;
		}

		public boolean isAdded() {
			return _added;
		}

		public boolean isDropped() {
			return _dropped;
		}

		public void setDropped(boolean dropped) {
			_dropped = dropped;
		}

		public void setNewColumnName(String newColumnName) {
			_newColumnName = newColumnName;
		}

		public void setNewColumnType(String newColumnType) {
			_newColumnType = newColumnType;
		}

		private final boolean _added;
		private boolean _dropped;
		private String _newColumnName;
		private String _newColumnType;

	}

}