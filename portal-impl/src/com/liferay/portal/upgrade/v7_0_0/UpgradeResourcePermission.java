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

package com.liferay.portal.upgrade.v7_0_0;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.dao.orm.common.SQLTransformer;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.model.PortletConstants;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LoggingTimer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sampsa Sohlman
 */
public class UpgradeResourcePermission extends UpgradeProcess {

	protected void createIndex() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQLTemplateString(
				"create index IX_D5F1E2A2 on ResourcePermission " +
					"(name[$COLUMN_LENGTH:255$])",
				false);
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		createIndex();

		upgradeResourcePermissions();
	}

	protected void upgradeResourcePermissions() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQL(
				"update ResourcePermission set viewActionId = [$FALSE$] " +
					"where MOD(actionIds, 2) = 0");
			runSQL(
				"update ResourcePermission set viewActionId = [$TRUE$] where " +
					"MOD(actionIds, 2) = 1");

			try (PreparedStatement ps1 = connection.prepareStatement(
					"select distinct name from ResourcePermission");
				ResultSet rs1 = ps1.executeQuery();
				PreparedStatement ps2 = connection.prepareStatement(
					"select distinct primKey from ResourcePermission where " +
						"name = ?")) {

				while (rs1.next()) {
					List<String> stringPrimKeys = new ArrayList<>();

					String name = rs1.getString("name");

					ps2.setString(1, name);

					try (ResultSet rs2 = ps2.executeQuery()) {
						while (rs2.next()) {
							String primKey = rs2.getString("primKey");

							if ((GetterUtil.getLong(primKey) <= 0) &&
								!primKey.contains(
									PortletConstants.LAYOUT_SEPARATOR)) {

								stringPrimKeys.add(primKey);
							}
						}
					}

					_updatePrimKeyIdsByName(name, stringPrimKeys);
				}
			}
		}
	}

	private String _createInClause(List<String> primKeys) {
		StringBundler sb = new StringBundler(primKeys.size() + 1);

		sb.append("in (?");

		for (int i = 1; i < primKeys.size(); i++) {
			sb.append(", ?");
		}

		sb.append(")");

		return sb.toString();
	}

	private void _updatePrimKeyIds(
			String sql, String name, List<String> primKeys)
		throws Exception {

		try (PreparedStatement ps = connection.prepareStatement(
				SQLTransformer.transform(sql))) {

			ps.setString(1, name);

			for (int i = 0; i < primKeys.size(); i++) {
				String primKey = primKeys.get(i);

				ps.setString(i + 2, primKey);
			}

			ps.executeUpdate();
		}
	}

	private void _updatePrimKeyIdsByName(String name, List<String> primKeys)
		throws Exception {

		if (primKeys.isEmpty()) {
			_updatePrimKeyIds(
				"update ResourcePermission set primKeyId = CAST_LONG(" +
					"primKey) where name = ? and primKey not like '%_LAYOUT_%'",
				name, primKeys);

			_updatePrimKeyIds(
				"update ResourcePermission set primKeyId = 0 where name = ? " +
					"and primKey like '%_LAYOUT_%'",
				name, primKeys);

			return;
		}

		DB db = DBManagerUtil.getDB();

		if ((db.getDBType() == DBType.ORACLE) && (primKeys.size() > 1000)) {
			_updatePrimKeyIdsInBatches(name, primKeys);
		}
		else {
			String inClause = _createInClause(primKeys);

			_updatePrimKeyIds(
				StringBundler.concat(
					"update ResourcePermission set primKeyId = 0 where name = ",
					"? and (primKey like '%_LAYOUT_%' or primKey ", inClause,
					")"),
				name, primKeys);
		}

		runSQL(
			StringBundler.concat(
				"update ResourcePermission set primKeyId = CAST_LONG(primKey",
				") where name = '", name,
				"' and (primKey not like '%_LAYOUT_%' and primKeyId != 0)"));
	}

	private void _updatePrimKeyIdsInBatches(String name, List<String> primKeys)
		throws Exception {

		int iteration = primKeys.size() / 1000;

		for (int i = 0; i <= iteration; i++) {
			int start = i * 1000;

			int end = start + 1000;

			if (end > primKeys.size()) {
				end = primKeys.size();
			}

			List<String> sublist = ListUtil.subList(primKeys, start, end);

			String inClause = _createInClause(sublist);

			_updatePrimKeyIds(
				StringBundler.concat(
					"update ResourcePermission set primKeyId = 0 where name = ",
					"? and (primKey like '%_LAYOUT_%' or primKey ", inClause,
					")"),
				name, sublist);
		}
	}

}