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

package com.liferay.license.manager.web.internal.upgrade.v1_0_1;

import com.liferay.license.manager.web.internal.constants.LicenseManagerPortletKeys;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;

/**
 * @author David Zhang
 */
public class UpgradePortletStatus extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		updatePortletStatus("173", 0);
		updatePortletStatus(LicenseManagerPortletKeys.LICENSE_MANAGER, 1);
	}

	protected void updatePortletStatus(String portletId, int status)
		throws Exception {

		try (PreparedStatement ps = connection.prepareStatement(
				"update Portlet set active_ = ? where portletId = ?")) {

			ps.setInt(1, status);
			ps.setString(2, portletId);

			ps.execute();
		}
	}

}