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

package com.liferay.asset.entry.rel.internal.upgrade.v2_0_0;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

/**
 * @author William Nealeigh
 */
public class UpgradeSchema extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		upgradeSchema();
	}

	protected void upgradeSchema() throws Exception {
		if (hasTable("AssetEntries_AssetCategories")) {
			runSQL("drop table AssetEntries_AssetCategories");

			if (_log.isInfoEnabled()) {
				_log.info("Deleted table AssetEntries_AssetCategories");
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(UpgradeSchema.class);

}