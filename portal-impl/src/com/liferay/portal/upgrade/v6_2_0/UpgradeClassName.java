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

package com.liferay.portal.upgrade.v6_2_0;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @author Alberto Chaparro
 */
public class UpgradeClassName extends UpgradeProcess {

	protected void addClassName(String value) throws Exception {
		Connection con = null;
		PreparedStatement ps = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"insert into ClassName_ (mvccVersion, classNameId, value) " +
					"values (?, ?, ?)");

			ps.setLong(1, 0);
			ps.setLong(2, increment());
			ps.setString(3, value);

			ps.executeUpdate();
		}
		finally {
			DataAccess.cleanUp(con, ps);
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		addClassName("com.liferay.portlet.journal.model.JournalFolder");
	}

}