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

package com.liferay.portal.db.partitioning.internal.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.db.partitioning.kernel.util.DBPartitioning;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.model.CompanyInfo;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyInfoLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alberto Chaparro
 */
@RunWith(Arquillian.class)
public class DBPartitioningTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_db = DBManagerUtil.getDB();

		if (_db.getDBType() != DBType.MYSQL) {
			return;
		}

		_connection = DataAccess.getConnection();

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		try {
			CompanyThreadLocal.setCompanyId(_portal.getDefaultCompanyId());

			_dbPartitioning.usePartition(_connection);
		}
		finally {
			CompanyThreadLocal.setCompanyId(currentCompanyId);

			_dbPartitioning.usePartition(_connection);
		}
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		if (_db.getDBType() != DBType.MYSQL) {
			return;
		}

		_db.runSQL("drop schema company" + _COMPANY_ID);

		DataAccess.cleanUp(_connection);
	}

	@Test
	public void testAddDefaultPartition() throws Exception {
		Assume.assumeTrue(_db.getDBType() == DBType.MYSQL);

		_dbPartitioning.addPartition(_portal.getDefaultCompanyId());
	}

	@Test
	public void testAddPartition() throws Exception {
		Assume.assumeTrue(_db.getDBType() == DBType.MYSQL);

		_dbPartitioning.addPartition(_COMPANY_ID);

		Assert.assertTrue(exists("company" + _COMPANY_ID));
	}

	@Test
	public void testUseDefaultPartition() throws Exception {
		Assume.assumeTrue(_db.getDBType() == DBType.MYSQL);

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		try {
			CompanyThreadLocal.setCompanyId(_portal.getDefaultCompanyId());

			_dbPartitioning.usePartition(_connection);

			CompanyInfo companyInfo = _companyInfoLocalService.fetchCompany(
				_portal.getDefaultCompanyId());

			Assert.assertEquals(
				_portal.getDefaultCompanyId(), companyInfo.getCompanyId());
		}
		finally {
			CompanyThreadLocal.setCompanyId(currentCompanyId);

			_dbPartitioning.usePartition(_connection);
		}
	}

	@Test
	public void testUsePartition() throws Exception {
		Assume.assumeTrue(_db.getDBType() == DBType.MYSQL);

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		try {
			CompanyThreadLocal.setCompanyIdInitialization(_COMPANY_ID);

			_dbPartitioning.usePartition(_connection);

			CompanyInfo companyInfo = _companyInfoLocalService.fetchCompany(
				_COMPANY_ID);

			Assert.assertEquals(null, companyInfo);
		}
		finally {
			CompanyThreadLocal.setCompanyIdInitialization(currentCompanyId);

			_dbPartitioning.usePartition(_connection);
		}
	}

	@Test
	public void testValidate() {
		try {
			_dbPartitioning.validate();
		}
		catch (Exception exception) {
			Assert.assertNotEquals(DBType.MYSQL, _db.getDBType());

			return;
		}

		Assert.assertEquals(DBType.MYSQL, _db.getDBType());
	}

	protected boolean exists(String schemaName) throws Exception {
		try (Statement statement = _connection.createStatement()) {
			try {
				statement.execute(
					"select 1 from " + schemaName + ".CompanyInfo");
			}
			catch (Exception exception) {
				return false;
			}
		}

		return true;
	}

	private static final long _COMPANY_ID = 1L;

	@Inject
	private static CompanyInfoLocalService _companyInfoLocalService;

	private static Connection _connection;
	private static DB _db;

	@Inject
	private static DBPartitioning _dbPartitioning;

	@Inject
	private static Portal _portal;

}