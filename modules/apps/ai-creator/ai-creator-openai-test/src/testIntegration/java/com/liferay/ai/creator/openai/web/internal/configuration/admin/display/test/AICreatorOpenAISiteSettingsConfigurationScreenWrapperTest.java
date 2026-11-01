/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.creator.openai.web.internal.configuration.admin.display.test;

import com.liferay.ai.creator.openai.configuration.manager.AICreatorOpenAIConfigurationManager;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.configuration.admin.display.ConfigurationScreen;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
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
public class AICreatorOpenAISiteSettingsConfigurationScreenWrapperTest {

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
		_aiCreatorOpenAIConfigurationManager.
			saveAICreatorOpenAIGroupConfiguration(
				_group1.getGroupId(), RandomTestUtil.randomString(), false,
				true);

		String apiKey = RandomTestUtil.randomString();

		_aiCreatorOpenAIConfigurationManager.
			saveAICreatorOpenAIGroupConfiguration(
				_group2.getGroupId(), apiKey, true, false);

		Dictionary<String, Object> properties =
			_configurationScreen.exportProperties(_group1.getGroupId());

		Assert.assertNull(properties.get("apiKey"));

		_configurationScreen.importProperties(properties, _group2.getGroupId());

		properties = _configurationScreen.exportProperties(
			_group2.getGroupId());

		Assert.assertFalse(
			(boolean)properties.get("enableChatGPTToCreateContent"));
		Assert.assertTrue((boolean)properties.get("enableDALLEToCreateImages"));

		Assert.assertEquals(
			apiKey,
			_aiCreatorOpenAIConfigurationManager.getAICreatorOpenAIGroupAPIKey(
				_group2.getGroupId()));
	}

	@Test
	public void testExportProperties() throws Exception {
		Dictionary<String, Object> properties =
			_configurationScreen.exportProperties(_group1.getGroupId());

		Assert.assertTrue(properties.isEmpty());
	}

	@Inject
	private AICreatorOpenAIConfigurationManager
		_aiCreatorOpenAIConfigurationManager;

	@Inject(
		filter = "component.name=com.liferay.ai.creator.openai.web.internal.configuration.admin.display.AICreatorOpenAISiteSettingsConfigurationScreenWrapper"
	)
	private ConfigurationScreen _configurationScreen;

	@DeleteAfterTestRun
	private Group _group1;

	@DeleteAfterTestRun
	private Group _group2;

}