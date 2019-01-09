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

package com.liferay.portal.kernel.upgrade;

import com.liferay.exportimport.kernel.staging.StagingConstants;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.UnicodeProperties;

/**
 * This class provides a skeleton for modifying Group typesettings.
 *
 * @author Balázs Sáfrány-Kovalik
 */
public abstract class BaseUpgradeGroupTypeSettings extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		updateStagedPortletNames();
	}

	protected String getStagedPortletId(String portletId) {
		if (portletId.startsWith(StagingConstants.STAGED_PORTLET)) {
			return portletId;
		}

		return StagingConstants.STAGED_PORTLET.concat(portletId);
	}

	protected void updateStagedPortletNames() throws PortalException {
		for (Company company : companyLocalService.getCompanies()) {
			updateStagedPortletNames(company.getCompanyId());
		}
	}

	protected void updateStagedPortletNames(Long companyId)
		throws PortalException {

		ActionableDynamicQuery groupActionableDynamicQuery =
			groupLocalService.getActionableDynamicQuery();

		groupActionableDynamicQuery.setAddCriteriaMethod(
			dynamicQuery -> {
				Property companyIdProperty = PropertyFactoryUtil.forName(
					"companyId");

				dynamicQuery.add(companyIdProperty.eq(companyId));

				Property siteProperty = PropertyFactoryUtil.forName("site");

				dynamicQuery.add(siteProperty.eq(Boolean.TRUE));
			});
		groupActionableDynamicQuery.setPerformActionMethod(
			(ActionableDynamicQuery.PerformActionMethod<Group>)group -> {
				UnicodeProperties typeSettingsProperties =
					group.getTypeSettingsProperties();

				if (typeSettingsProperties.isEmpty()) {
					return;
				}

				updateTypeSettings(typeSettingsProperties, group);
			});

		groupActionableDynamicQuery.performActions();
	}

	protected abstract void updateTypeSettings(
		UnicodeProperties typeSettingsProperties, Group group);

	protected CompanyLocalService companyLocalService;
	protected GroupLocalService groupLocalService;

}