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

package com.liferay.layout.admin.web.internal.upgrade.v_1_0_3;

import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Sam Ziemer
 */
public class UpgradeLayoutTemplateId extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		updateLayoutTemplateId();
	}

	protected void updateLayoutTemplateId() throws Exception {
		try (PreparedStatement ps1 = connection.prepareStatement(
				"select plid, typeSettings from Layout where typesettings " +
					"like '%layout-template-id=1_2_1_columns%'");
			PreparedStatement ps2 = AutoBatchPreparedStatementUtil.autoBatch(
				connection.prepareStatement(
					"update Layout set typeSettings = ? where plid = ?"))) {

			try (ResultSet rs = ps1.executeQuery()) {
				while (rs.next()) {
					long plid = rs.getLong("plid");

					String typeSettings = rs.getString("typeSettings");

					typeSettings = StringUtil.replace(
						typeSettings, "layout-template-id=1_2_1_columns",
						"layout-template-id=1_2_1_columns_ii");

					ps2.setString(1, typeSettings);

					ps2.setLong(2, plid);

					ps2.addBatch();
				}

				ps2.executeBatch();
			}
		}
	}

}