/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mariano Álvaro Sáiz
 */
public class IndexSQLUtilTest {

	@Test
	public void testGetCreateSQL() {
		String indexName = (String)ArrayUtil.getValue(
			StringUtil.split(
				IndexSQLUtil.getCreateSQL(
					false, "Table1", new String[] {"column1", "column2"}, null,
					IndexSQLUtil.INDEX_NAME_PREFIX),
				StringPool.SPACE),
			2);

		Assert.assertEquals(
			indexName,
			(String)ArrayUtil.getValue(
				StringUtil.split(
					IndexSQLUtil.getCreateSQL(
						false, "Table1",
						new String[] {
							"column1[$COLUMN_LENGTH:75$]",
							"column2[$COLUMN_LENGTH:75$]"
						},
						null, IndexSQLUtil.INDEX_NAME_PREFIX),
					StringPool.SPACE),
				2));

		Assert.assertEquals(
			indexName,
			(String)ArrayUtil.getValue(
				StringUtil.split(
					IndexSQLUtil.getCreateSQL(
						false, "Table1",
						new String[] {"column1", "column2[$COLUMN_LENGTH:75$]"},
						null, IndexSQLUtil.INDEX_NAME_PREFIX),
					StringPool.SPACE),
				2));
	}

}