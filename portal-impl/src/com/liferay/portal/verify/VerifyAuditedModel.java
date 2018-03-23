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

package com.liferay.portal.verify;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.FullNameGenerator;
import com.liferay.portal.kernel.security.auth.FullNameGeneratorFactory;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.verify.model.VerifiableAuditedModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Michael C. Han
 * @author Shinn Lok
 */
public class VerifyAuditedModel extends VerifyProcess {

	public void verify(VerifiableAuditedModel... verifiableAuditedModels)
		throws Exception {

		List<String> unverifiedTableNames = new ArrayList<>();

		for (VerifiableAuditedModel verifiableAuditedModel :
				verifiableAuditedModels) {

			unverifiedTableNames.add(verifiableAuditedModel.getTableName());
		}

		List<VerifyAuditedModelCallable> verifyAuditedModelCallables =
			new ArrayList<>(unverifiedTableNames.size());

		while (!unverifiedTableNames.isEmpty()) {
			int count = unverifiedTableNames.size();

			for (VerifiableAuditedModel verifiableAuditedModel :
					verifiableAuditedModels) {

				if (unverifiedTableNames.contains(
						verifiableAuditedModel.getJoinByTableName()) ||
					!unverifiedTableNames.contains(
						verifiableAuditedModel.getTableName())) {

					continue;
				}

				VerifyAuditedModelCallable verifyAuditedModelCallable =
					new VerifyAuditedModelCallable(verifiableAuditedModel);

				verifyAuditedModelCallables.add(verifyAuditedModelCallable);

				unverifiedTableNames.remove(
					verifiableAuditedModel.getTableName());
			}

			if (unverifiedTableNames.size() == count) {
				throw new VerifyException(
					"Circular dependency detected " + unverifiedTableNames);
			}
		}

		doVerify(verifyAuditedModelCallables);
	}

	@Override
	protected void doVerify() throws Exception {
		Map<String, VerifiableAuditedModel> verifiableAuditedModelsMap =
			PortalBeanLocatorUtil.locate(VerifiableAuditedModel.class);

		Collection<VerifiableAuditedModel> verifiableAuditedModels =
			verifiableAuditedModelsMap.values();

		verify(
			verifiableAuditedModels.toArray(
				new VerifiableAuditedModel[verifiableAuditedModels.size()]));
	}

	protected Object[] getDefaultUserArray(Connection con, long companyId)
		throws Exception {

		try (PreparedStatement ps = con.prepareStatement(
				"select userId, firstName, middleName, lastName from User_ " +
					"where companyId = ? and defaultUser = ?")) {

			ps.setLong(1, companyId);
			ps.setBoolean(2, true);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					long userId = rs.getLong("userId");
					String firstName = rs.getString("firstName");
					String middleName = rs.getString("middleName");
					String lastName = rs.getString("lastName");

					FullNameGenerator fullNameGenerator =
						FullNameGeneratorFactory.getInstance();

					String userName = fullNameGenerator.getFullName(
						firstName, middleName, lastName);

					Timestamp createDate = new Timestamp(
						System.currentTimeMillis());

					return new Object[] {
						companyId, userId, userName, createDate, createDate
					};
				}

				return null;
			}
		}
	}

	protected String getUserName(Connection con, long userId) throws Exception {
		try (PreparedStatement ps = con.prepareStatement(
				"select firstName, middleName, lastName from User_ where " +
					"userId = ?")) {

			ps.setLong(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String firstName = rs.getString("firstName");
					String middleName = rs.getString("middleName");
					String lastName = rs.getString("lastName");

					FullNameGenerator fullNameGenerator =
						FullNameGeneratorFactory.getInstance();

					return fullNameGenerator.getFullName(
						firstName, middleName, lastName);
				}

				return StringPool.BLANK;
			}
		}
	}

	protected void verifyAuditedModel(
			VerifiableAuditedModel verifiableAuditedModel)
		throws Exception {

		try (LoggingTimer loggingTimer = new LoggingTimer(
				verifiableAuditedModel.getTableName());
			Connection con = DataAccess.getUpgradeOptimizedConnection()) {

			String relatedModelName =
				verifiableAuditedModel.getRelatedModelName();

			if (relatedModelName != null) {
				_bulkVerifyFromRelatedModel(
					con, verifiableAuditedModel, "userId");

				if (verifiableAuditedModel.isUpdateDates()) {
					_bulkVerifyFromRelatedModel(
						con, verifiableAuditedModel, "createDate");

					_bulkVerifyFromRelatedModel(
						con, verifiableAuditedModel, "modifiedDate");
				}

				DBInspector dbInspector = new DBInspector(con);

				if (dbInspector.hasColumn(relatedModelName, "userName")) {
					_bulkVerifyFromRelatedModel(
						con, verifiableAuditedModel, "userName");
				}
			}

			_bulkVerifyRegularUsers(con, verifiableAuditedModel);

			_bulkVerifyUnresolvedUsers(con, verifiableAuditedModel);
		}
	}

	private void _bulkVerifyFromRelatedModel(
			Connection con, VerifiableAuditedModel verifiableAuditedModel,
			String columnName)
		throws Exception {

		String sql = StringBundler.concat(
			"update ", verifiableAuditedModel.getTableName(), " set ",
			columnName, " = (select ", columnName, " from ",
			verifiableAuditedModel.getRelatedModelName(), " where ",
			verifiableAuditedModel.getTableName(), StringPool.PERIOD,
			verifiableAuditedModel.getJoinByTableName(), " = ",
			verifiableAuditedModel.getRelatedModelName(), StringPool.PERIOD,
			verifiableAuditedModel.getRelatedPKColumnName(),
			") where userName is null");

		runSQL(con, sql);
	}

	private void _bulkVerifyRegularUsers(
			Connection con, VerifiableAuditedModel verifiableAuditedModel)
		throws Exception {

		String selectQuery = StringBundler.concat(
			"select distinct userId from ",
			verifiableAuditedModel.getTableName(), " where userName is null");

		String updateQuery = StringBundler.concat(
			"update ", verifiableAuditedModel.getTableName(),
			" set userName = ? where userId = ?");

		try (PreparedStatement ps1 = con.prepareStatement(selectQuery);
			ResultSet rs = ps1.executeQuery();
			PreparedStatement ps2 = AutoBatchPreparedStatementUtil.autoBatch(
				con.prepareStatement(updateQuery))) {

			Map<Long, String> fullNames = new HashMap<>();

			while (rs.next()) {
				long userId = rs.getLong("userId");

				String fullName = getUserName(con, userId);

				if (Validator.isNotNull(fullName)) {
					fullNames.put(userId, fullName);
				}
			}

			for (Map.Entry<Long, String> entry : fullNames.entrySet()) {
				ps2.setString(1, entry.getValue());
				ps2.setLong(2, entry.getKey());

				ps2.addBatch();
			}

			ps2.executeBatch();
		}
	}

	private void _bulkVerifyUnresolvedUsers(
			Connection con, VerifiableAuditedModel verifiableAuditedModel)
		throws Exception {

		String createDateSQL = StringBundler.concat(
			"update ", verifiableAuditedModel.getTableName(),
			" set createDate = ? where createDate is null");

		String modifiedDateSQL = StringBundler.concat(
			"update ", verifiableAuditedModel.getTableName(),
			" set modifiedDate = ? where modifiedDate is null");

		String userNameSQL = StringBundler.concat(
			"update ", verifiableAuditedModel.getTableName(),
			" set userName = ? where userName is null");

		try (PreparedStatement ps1 = AutoBatchPreparedStatementUtil.autoBatch(
				con.prepareStatement(createDateSQL));
			PreparedStatement ps2 = AutoBatchPreparedStatementUtil.autoBatch(
				con.prepareStatement(modifiedDateSQL));
			PreparedStatement ps3 = AutoBatchPreparedStatementUtil.autoBatch(
				con.prepareStatement(userNameSQL))) {

			if (verifiableAuditedModel.isAnonymousUserAllowed()) {
				Timestamp auditDate = new Timestamp(System.currentTimeMillis());

				ps1.setTimestamp(1, auditDate);

				ps1.addBatch();

				ps2.setTimestamp(1, auditDate);

				ps2.addBatch();

				ps3.setString(1, "Anonymous");

				ps3.addBatch();
			}
			else {
				for (long companyId : PortalUtil.getCompanyIds()) {
					Object[] defaultUserArray = getDefaultUserArray(
						con, companyId);

					if (verifiableAuditedModel.isUpdateDates()) {
						Timestamp createDate = (Timestamp)defaultUserArray[3];

						ps1.setTimestamp(1, createDate);

						ps1.addBatch();

						Timestamp modifiedDate = (Timestamp)defaultUserArray[4];

						ps2.setTimestamp(1, modifiedDate);

						ps2.addBatch();
					}

					String defaultUserFullName = (String)defaultUserArray[2];

					ps3.setString(1, defaultUserFullName);

					ps3.addBatch();
				}
			}

			ps1.executeBatch();
			ps2.executeBatch();
			ps3.executeBatch();
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		VerifyAuditedModel.class);

	private class VerifyAuditedModelCallable implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			verifyAuditedModel(_verifiableAuditedModel);

			return null;
		}

		private VerifyAuditedModelCallable(
			VerifiableAuditedModel verifiableAuditedModel) {

			_verifiableAuditedModel = verifiableAuditedModel;
		}

		private final VerifiableAuditedModel _verifiableAuditedModel;

	}

}