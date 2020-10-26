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

package com.liferay.journal.upgrade.v3_4_1.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.display.page.constants.AssetDisplayPageConstants;
import com.liferay.asset.display.page.service.AssetDisplayPageEntryLocalService;
import com.liferay.asset.display.page.service.AssetDisplayPageEntryLocalServiceUtil;
import com.liferay.journal.constants.JournalFolderConstants;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.test.util.JournalTestUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeStep;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jürgen Kappler
 */
@RunWith(Arquillian.class)
public class UpgradeAssetDisplayPageEntriesTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_company = CompanyTestUtil.addCompany();

		_user = UserTestUtil.getAdminUser(_company.getCompanyId());

		_group = GroupTestUtil.addGroup(
			_company.getCompanyId(), _user.getUserId(),
			GroupConstants.DEFAULT_PARENT_GROUP_ID);

		setUpUpgradeAssetDisplayPageEntry();
	}

	@Test
	public void testUpgradeAssetDisplayPageEntries() throws Exception {
		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				_group.getGroupId(), _user.getUserId());

		long classNameId = _portal.getClassNameId(
			JournalArticle.class.getName());

		JournalArticle article1 = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		_assetDisplayPageEntryLocalService.addAssetDisplayPageEntry(
			_user.getUserId(), _group.getGroupId(), classNameId,
			article1.getResourcePrimKey(), 0,
			AssetDisplayPageConstants.TYPE_DEFAULT, serviceContext);

		JournalArticle article2 = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		Assert.assertEquals(
			1,
			getAssetDisplayPageEntriesCount(
				_group.getGroupId(), classNameId,
				AssetDisplayPageConstants.TYPE_DEFAULT));

		Assert.assertEquals(
			0,
			getAssetDisplayPageEntriesCount(
				_group.getGroupId(), classNameId,
				AssetDisplayPageConstants.TYPE_NONE));

		_upgradeAssetDisplayPageEntries.upgrade();

		Assert.assertEquals(
			0,
			getAssetDisplayPageEntriesCount(
				_group.getGroupId(), classNameId,
				AssetDisplayPageConstants.TYPE_DEFAULT));

		Assert.assertEquals(
			1,
			getAssetDisplayPageEntriesCount(
				_group.getGroupId(), classNameId,
				AssetDisplayPageConstants.TYPE_NONE));

		StringBundler sb = new StringBundler(7);

		sb.append("select classPK, layoutPageTemplateEntryId, plid from ");
		sb.append("AssetDisplayPageEntry where groupId = ");
		sb.append(_group.getGroupId());
		sb.append(" and classNameId = ");
		sb.append(classNameId);
		sb.append(" and type_ = ");
		sb.append(AssetDisplayPageConstants.TYPE_NONE);

		Connection connection = DataAccess.getConnection();

		try (PreparedStatement ps1 = connection.prepareStatement(
				sb.toString())) {

			try (ResultSet rs = ps1.executeQuery()) {
				if (rs.next()) {
					Assert.assertEquals(
						article2.getResourcePrimKey(), rs.getLong("classPK"));
					Assert.assertEquals(
						0, rs.getLong("layoutPageTemplateEntryId"));
					Assert.assertEquals(0, rs.getLong("plid"));
				}
			}
		}
	}

	protected int getAssetDisplayPageEntriesCount(
			long groupId, long classNameId, int type)
		throws Exception {

		ActionableDynamicQuery actionableDynamicQuery =
			AssetDisplayPageEntryLocalServiceUtil.getActionableDynamicQuery();

		actionableDynamicQuery.setAddCriteriaMethod(
			dynamicQuery -> {
				dynamicQuery.add(
					RestrictionsFactoryUtil.eq("groupId", groupId));
				dynamicQuery.add(
					RestrictionsFactoryUtil.eq("classNameId", classNameId));
				dynamicQuery.add(RestrictionsFactoryUtil.eq("type", type));
			});

		return (int)actionableDynamicQuery.performCount();
	}

	protected void setUpUpgradeAssetDisplayPageEntry() {
		_upgradeStepRegistrator.register(
			new UpgradeStepRegistrator.Registry() {

				@Override
				public void register(
					String fromSchemaVersionString,
					String toSchemaVersionString, UpgradeStep... upgradeSteps) {

					for (UpgradeStep upgradeStep : upgradeSteps) {
						Class<?> clazz = upgradeStep.getClass();

						if (Objects.equals(clazz.getName(), _CLASS_NAME)) {
							_upgradeAssetDisplayPageEntries =
								(UpgradeProcess)upgradeStep;
						}
					}
				}

			});
	}

	private static final String _CLASS_NAME =
		"com.liferay.journal.internal.upgrade.v3_4_1." +
			"UpgradeAssetDisplayPageEntries";

	@Inject(
		filter = "(&(objectClass=com.liferay.journal.internal.upgrade.JournalServiceUpgrade))"
	)
	private static UpgradeStepRegistrator _upgradeStepRegistrator;

	@Inject
	private AssetDisplayPageEntryLocalService
		_assetDisplayPageEntryLocalService;

	@DeleteAfterTestRun
	private Company _company;

	private Group _group;

	@Inject
	private Portal _portal;

	private UpgradeProcess _upgradeAssetDisplayPageEntries;
	private User _user;

}