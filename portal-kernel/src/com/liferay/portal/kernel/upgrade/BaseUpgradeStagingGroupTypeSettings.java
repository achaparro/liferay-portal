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

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;

/**
 * This class is used to rename a staged portlet in all Group typesettings.
 *
 * @author Gergely Mathe
 * @author Balázs Sáfrány-Kovalik
 */
public class BaseUpgradeStagingGroupTypeSettings
	extends BaseUpgradeGroupTypeSettings {

	public BaseUpgradeStagingGroupTypeSettings(
		CompanyLocalService companyLocalService,
		GroupLocalService groupLocalService, String oldPortletId,
		String newPortletId) {

		this.companyLocalService = companyLocalService;
		this.groupLocalService = groupLocalService;
		_oldPortletId = oldPortletId;
		_newPortletId = newPortletId;
	}

	protected void updateTypeSettings(
		UnicodeProperties typeSettingsProperties, Group group) {

		String oldPropertyKey = getStagedPortletId(_oldPortletId);

		String oldPropertyValue = typeSettingsProperties.getProperty(
			oldPropertyKey);

		typeSettingsProperties.remove(oldPropertyKey);

		if (Validator.isNull(oldPropertyValue)) {
			return;
		}

		String newPropertyKey = getStagedPortletId(_newPortletId);

		String newPropertyValue = typeSettingsProperties.getProperty(
			newPropertyKey);

		if (Validator.isNull(newPropertyValue)) {
			typeSettingsProperties.put(newPropertyKey, oldPropertyValue);
		}

		group.setTypeSettingsProperties(typeSettingsProperties);

		groupLocalService.updateGroup(group);
	}

	private final String _newPortletId;
	private final String _oldPortletId;

}