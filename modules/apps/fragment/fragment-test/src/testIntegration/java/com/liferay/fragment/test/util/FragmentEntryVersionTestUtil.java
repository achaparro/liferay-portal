/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.test.util;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryVersion;
import com.liferay.fragment.service.persistence.FragmentEntryVersionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Georgel Pop
 */
public class FragmentEntryVersionTestUtil {

	public static int getFragmentEntryVersionsCount(
			long ctCollectionId, FragmentEntry fragmentEntry)
		throws Exception {

		try (Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				"select count(*) as count from FragmentEntryVersion where " +
					"ctCollectionId = ? and fragmentEntryId = ?")) {

			preparedStatement.setLong(1, ctCollectionId);
			preparedStatement.setLong(2, fragmentEntry.getFragmentEntryId());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return (int)resultSet.getLong("count");
				}

				return 0;
			}
		}
	}

	public static List<Integer> getVersions(
			long ctCollectionId, FragmentEntry fragmentEntry)
		throws Exception {

		List<Integer> versions = new ArrayList<>();

		try (Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				"select version from FragmentEntryVersion where " +
					"ctCollectionId = ? and fragmentEntryId = ? order by " +
						"version")) {

			preparedStatement.setLong(1, ctCollectionId);
			preparedStatement.setLong(2, fragmentEntry.getFragmentEntryId());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					versions.add(resultSet.getInt("version"));
				}
			}
		}

		return versions;
	}

	public static List<Integer> insertFragmentEntryVersions(
			int count, long ctCollectionId, FragmentEntry fragmentEntry)
		throws Exception {

		List<Integer> versions = new ArrayList<>(count);

		try (Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement1 = connection.prepareStatement(
				StringBundler.concat(
					"select max(version) as maxVersion from ",
					"FragmentEntryVersion where ctCollectionId = ? and ",
					"fragmentEntryId = ?"));
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.autoBatch(
					connection,
					StringBundler.concat(
						"insert into FragmentEntryVersion (mvccVersion, ",
						"ctCollectionId, fragmentEntryVersionId, version, ",
						"fragmentEntryId, groupId, companyId, userId, ",
						"createDate, modifiedDate, name, status) values (0, ",
						"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))) {

			preparedStatement1.setLong(1, ctCollectionId);
			preparedStatement1.setLong(2, fragmentEntry.getFragmentEntryId());

			int maxVersion = 0;

			try (ResultSet resultSet = preparedStatement1.executeQuery()) {
				if (resultSet.next()) {
					maxVersion = resultSet.getInt("maxVersion");
				}
			}

			for (int i = 1; i <= count; i++) {
				int version = maxVersion + i;

				versions.add(version);

				Timestamp now = new Timestamp(System.currentTimeMillis());

				preparedStatement2.setLong(1, ctCollectionId);
				preparedStatement2.setLong(
					2,
					CounterLocalServiceUtil.increment(
						FragmentEntryVersion.class.getName()));
				preparedStatement2.setInt(3, version);
				preparedStatement2.setLong(
					4, fragmentEntry.getFragmentEntryId());
				preparedStatement2.setLong(5, fragmentEntry.getGroupId());
				preparedStatement2.setLong(6, fragmentEntry.getCompanyId());
				preparedStatement2.setLong(7, fragmentEntry.getUserId());
				preparedStatement2.setTimestamp(8, now);
				preparedStatement2.setTimestamp(9, now);
				preparedStatement2.setString(10, RandomTestUtil.randomString());
				preparedStatement2.setInt(
					11, WorkflowConstants.STATUS_APPROVED);

				preparedStatement2.addBatch();
			}

			preparedStatement2.executeBatch();
		}

		FragmentEntryVersionUtil.clearCache();

		return versions;
	}

}