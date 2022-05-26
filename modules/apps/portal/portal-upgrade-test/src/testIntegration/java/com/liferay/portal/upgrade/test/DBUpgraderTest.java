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

package com.liferay.portal.upgrade.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.model.ReleaseConstants;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.version.Version;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.tools.DBUpgrader;
import com.liferay.portal.upgrade.PortalUpgradeProcess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Luis Ortiz
 */
@RunWith(Arquillian.class)
public class DBUpgraderTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws SQLException {
		try (Connection connection = DataAccess.getConnection()) {
			_currentSchemaVersion =
				PortalUpgradeProcess.getCurrentSchemaVersion(connection);
		}

		_currentBuildNumber = _getReleaseColumnValue("buildNumber");

		_currentState = _getReleaseColumnValue("state_");
	}

	@Before
	public void setUp() throws SQLException {
		_innerPortalUpgradeProcess =
			new DBUpgraderTest.InnerPortalUpgradeProcess();
	}

	@After
	public void tearDown() throws Exception {
		_updateSchemaVersion(_currentSchemaVersion);

		_updateReleaseBuild(_currentBuildNumber);

		_updateReleaseState(_currentState);

		_innerPortalUpgradeProcess.close();
	}

	@Test
	public void testUpgradeWithErrorSchemaVersionInitialized()
		throws Exception {

		_updateReleaseState(ReleaseConstants.STATE_UPGRADE_FAILURE);

		_updateReleaseBuild(ReleaseInfo.RELEASE_6_2_0_BUILD_NUMBER);

		DBUpgrader.upgrade();
	}

	@Test
	public void testUpgradeWithErrorSchemaVersionNotInitialized()
		throws Exception {

		_updateSchemaVersion(Version.parseVersion(null));

		_updateReleaseState(ReleaseConstants.STATE_UPGRADE_FAILURE);

		_updateReleaseBuild(ReleaseInfo.RELEASE_6_2_0_BUILD_NUMBER);

		try {
			DBUpgrader.upgrade();

			Assert.fail();
		}
		catch (IllegalStateException illegalStateException) {
		}
	}

	@Test
	public void testUpgradeWithoutErrorSchemaVersionInitialized()
		throws Exception {

		_updateReleaseState(ReleaseConstants.STATE_GOOD);

		_updateReleaseBuild(ReleaseInfo.RELEASE_6_2_0_BUILD_NUMBER);

		DBUpgrader.upgrade();
	}

	private static int _getReleaseColumnValue(String columnName) {
		return ReflectionTestUtil.invoke(
			DBUpgrader.class, "_getReleaseColumnValue",
			new Class<?>[] {String.class}, columnName);
	}

	private void _updateReleaseBuild(int buildNumber) throws Exception {
		try (Connection connection = DataAccess.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
				"update Release_ set buildNumber = ? where releaseId = ?")) {

			preparedStatement.setInt(1, buildNumber);

			preparedStatement.setLong(2, ReleaseConstants.DEFAULT_ID);

			preparedStatement.executeUpdate();
		}
	}

	private void _updateReleaseState(int state) throws Exception {
		try (Connection connection = DataAccess.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
				"update Release_ set state_ = ? where releaseId = ?")) {

			preparedStatement.setInt(1, state);

			preparedStatement.setLong(2, ReleaseConstants.DEFAULT_ID);

			preparedStatement.executeUpdate();
		}
	}

	private void _updateSchemaVersion(Version version) {
		ReflectionTestUtil.invoke(
			_innerPortalUpgradeProcess, "updateSchemaVersion",
			new Class<?>[] {Version.class}, version);
	}

	private static int _currentBuildNumber;
	private static Version _currentSchemaVersion;
	private static int _currentState;

	private DBUpgraderTest.InnerPortalUpgradeProcess _innerPortalUpgradeProcess;

	private static class InnerPortalUpgradeProcess
		extends PortalUpgradeProcess {

		public void close() throws SQLException {
			connection.close();
		}

		private InnerPortalUpgradeProcess() throws SQLException {
			connection = DataAccess.getConnection();
		}

	}

}