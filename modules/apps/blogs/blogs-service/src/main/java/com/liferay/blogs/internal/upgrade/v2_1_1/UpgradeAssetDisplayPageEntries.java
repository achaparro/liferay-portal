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

package com.liferay.blogs.internal.upgrade.v2_1_1;

import com.liferay.asset.display.page.upgrade.BaseUpgradeAssetDisplayPageEntries;
import com.liferay.blogs.model.BlogsEntry;
import com.liferay.blogs.model.BlogsEntryTable;

/**
 * @author Jürgen Kappler
 */
public class UpgradeAssetDisplayPageEntries
	extends BaseUpgradeAssetDisplayPageEntries {

	@Override
	protected void doUpgrade() throws Exception {
		upgradeAssetDisplayPageTypes(
			BlogsEntryTable.INSTANCE.getTableName(),
			BlogsEntryTable.INSTANCE.entryId.getName(),
			BlogsEntry.class.getName());
	}

}