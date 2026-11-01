/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.locked.layouts.web.internal.configuration.admin.display.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.configuration.admin.display.ConfigurationScreen;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Dictionary;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alberto Chaparro
 */
@RunWith(Arquillian.class)
public class LockedLayoutsSiteSettingsConfigurationScreenWrapperTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group1 = GroupTestUtil.addGroup();
		_group2 = GroupTestUtil.addGroup();
	}

	@Test
	public void testExportImportProperties() throws Exception {
		_configurationScreen.importProperties(
			HashMapDictionaryBuilder.<String, Object>put(
				"allowAutomaticUnlockingProcess", false
			).put(
				"autosaveMinutes", 10
			).build(),
			_group1.getGroupId());

		Dictionary<String, Object> properties =
			_configurationScreen.exportProperties(_group1.getGroupId());

		_configurationScreen.importProperties(properties, _group2.getGroupId());

		properties = _configurationScreen.exportProperties(
			_group2.getGroupId());

		Assert.assertFalse(
			(boolean)properties.get("allowAutomaticUnlockingProcess"));
		Assert.assertEquals(10, properties.get("autosaveMinutes"));
	}

	@Test
	public void testExportProperties() throws Exception {
		Dictionary<String, Object> properties =
			_configurationScreen.exportProperties(_group1.getGroupId());

		Assert.assertTrue(properties.isEmpty());
	}

	@Inject(
		filter = "component.name=com.liferay.layout.locked.layouts.web.internal.configuration.admin.display.LockedLayoutsSiteSettingsConfigurationScreenWrapper"
	)
	private ConfigurationScreen _configurationScreen;

	@DeleteAfterTestRun
	private Group _group1;

	@DeleteAfterTestRun
	private Group _group2;

}