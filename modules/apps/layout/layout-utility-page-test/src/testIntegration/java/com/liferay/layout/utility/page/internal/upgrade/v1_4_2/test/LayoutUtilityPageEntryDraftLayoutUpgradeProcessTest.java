/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.utility.page.internal.upgrade.v1_4_2.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.layout.utility.page.kernel.constants.LayoutUtilityPageEntryConstants;
import com.liferay.layout.utility.page.model.LayoutUtilityPageEntry;
import com.liferay.layout.utility.page.service.LayoutUtilityPageEntryLocalService;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.version.Version;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;
import com.liferay.segments.constants.SegmentsExperienceConstants;
import com.liferay.segments.service.SegmentsExperienceLocalService;

import java.util.Collections;

import org.junit.After;
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
public class LayoutUtilityPageEntryDraftLayoutUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group.getGroupId());

		ServiceContextThreadLocal.pushServiceContext(_serviceContext);
	}

	@After
	public void tearDown() {
		ServiceContextThreadLocal.popServiceContext();
	}

	@Test
	@TestInfo("LPD-111384")
	public void testUpgradeProcess() throws Exception {
		LayoutUtilityPageEntry layoutUtilityPageEntry1 =
			_addLayoutUtilityPageEntry("LFR-" + RandomTestUtil.randomString());

		Layout layout1 = _layoutLocalService.getLayout(
			layoutUtilityPageEntry1.getPlid());

		Layout draftLayout1 = layout1.fetchDraftLayout();

		Layout duplicatedDraftLayout1 = _addDuplicatedDraftLayout(draftLayout1);

		Layout keptDraftLayout1 = _layoutLocalService.fetchDraftLayout(
			layout1.getPlid());

		LayoutUtilityPageEntry layoutUtilityPageEntry2 =
			_addLayoutUtilityPageEntry(RandomTestUtil.randomString());

		Layout layout2 = _layoutLocalService.getLayout(
			layoutUtilityPageEntry2.getPlid());

		Layout draftLayout2 = layout2.fetchDraftLayout();

		Layout duplicatedDraftLayout2 = _addDuplicatedDraftLayout(draftLayout2);

		_runUpgrade();

		long removedPlid = duplicatedDraftLayout1.getPlid();

		if (keptDraftLayout1.getPlid() == removedPlid) {
			removedPlid = draftLayout1.getPlid();
		}

		Assert.assertNull(_layoutLocalService.fetchLayout(removedPlid));
		Assert.assertNull(
			_segmentsExperienceLocalService.fetchSegmentsExperience(
				_group.getGroupId(), SegmentsExperienceConstants.KEY_DEFAULT,
				removedPlid));

		Layout upgradedDraftLayout1 = _layoutLocalService.fetchDraftLayout(
			layout1.getPlid());

		Assert.assertEquals(
			keptDraftLayout1.getPlid(), upgradedDraftLayout1.getPlid());
		Assert.assertEquals(
			layoutUtilityPageEntry1.getExternalReferenceCode() +
				"-layout-draft",
			upgradedDraftLayout1.getExternalReferenceCode());

		Assert.assertNotNull(
			_layoutLocalService.fetchLayout(draftLayout2.getPlid()));
		Assert.assertNotNull(
			_layoutLocalService.fetchLayout(duplicatedDraftLayout2.getPlid()));
	}

	private Layout _addDuplicatedDraftLayout(Layout draftLayout)
		throws Exception {

		_serviceContext.setAttribute(
			"layout.instanceable.allowed", Boolean.TRUE);

		return _layoutLocalService.addLayout(
			RandomTestUtil.randomString(), TestPropsValues.getUserId(),
			draftLayout.getGroupId(), draftLayout.isPrivateLayout(),
			draftLayout.getParentLayoutId(), draftLayout.getClassNameId(),
			draftLayout.getClassPK(), draftLayout.getNameMap(),
			draftLayout.getTitleMap(), draftLayout.getDescriptionMap(),
			draftLayout.getKeywordsMap(), draftLayout.getRobotsMap(),
			draftLayout.getType(), draftLayout.getTypeSettings(), true, true,
			Collections.emptyMap(), null, _serviceContext);
	}

	private LayoutUtilityPageEntry _addLayoutUtilityPageEntry(
			String externalReferenceCode)
		throws Exception {

		return _layoutUtilityPageEntryLocalService.addLayoutUtilityPageEntry(
			externalReferenceCode, TestPropsValues.getUserId(),
			_group.getGroupId(), 0, 0, false, RandomTestUtil.randomString(),
			LayoutUtilityPageEntryConstants.TYPE_STATUS, null, _serviceContext);
	}

	private void _runUpgrade() throws Exception {
		UpgradeProcess[] upgradeProcesses = UpgradeTestUtil.getUpgradeSteps(
			_upgradeStepRegistrator, new Version(1, 4, 2));

		for (UpgradeProcess upgradeProcess : upgradeProcesses) {
			upgradeProcess.upgrade();
		}

		_entityCache.clearCache();
	}

	@Inject
	private EntityCache _entityCache;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private LayoutLocalService _layoutLocalService;

	@Inject
	private LayoutUtilityPageEntryLocalService
		_layoutUtilityPageEntryLocalService;

	@Inject
	private SegmentsExperienceLocalService _segmentsExperienceLocalService;

	private ServiceContext _serviceContext;

	@Inject(
		filter = "(&(component.name=com.liferay.layout.utility.page.internal.upgrade.registry.LayoutUtilityPageEntryUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}