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

package com.liferay.calendar.web.internal.upgrade.v1_0_2;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.ResourceActionsUtil;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourceBlockLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.util.List;

/**
 * @author José María Muñoz
 */
public class UpgradeResourceBlockPermissions extends UpgradeProcess {

	public UpgradeResourceBlockPermissions(
		CompanyLocalService companyLocalService,
		ResourceActionLocalService resourceActionLocalService,
		ResourceBlockLocalService resourceBlockLocalService,
		RoleLocalService roleLocalService) {

		_companyLocalService = companyLocalService;
		_resourceActionLocalService = resourceActionLocalService;
		_resourceBlockLocalService = resourceBlockLocalService;
		_roleLocalService = roleLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		upgradeGuestResourceBlockPermissions();
	}

	protected long getCalendarResourceUnsupportedActionsBitwiseValue(
			String[] resourceActionIds)
		throws PortalException {

		List<String> guestUnsupportedActions =
			ResourceActionsUtil.getModelResourceGuestUnsupportedActions(
				_CALENDAR_RESOURCE_NAME);

		long bitwiseValue = 0;

		for (String resourceActionId : resourceActionIds) {
			if (guestUnsupportedActions.contains(resourceActionId)) {
				ResourceAction resourceAction =
					_resourceActionLocalService.getResourceAction(
						_CALENDAR_RESOURCE_NAME, resourceActionId);

				bitwiseValue |= resourceAction.getBitwiseValue();
			}
		}

		return bitwiseValue;
	}

	protected void upgradeGuestResourceBlockPermissions() throws Exception {
		String[] newUnsupportedActionIds =
			{ActionKeys.PERMISSIONS, ActionKeys.VIEW};

		long bitwiseValue = getCalendarResourceUnsupportedActionsBitwiseValue(
			newUnsupportedActionIds);

		if (bitwiseValue == 0) {
			return;
		}

		for (Company company : _companyLocalService.getCompanies()) {
			Role guestRole = _roleLocalService.getRole(
				company.getCompanyId(), RoleConstants.GUEST);

			_resourceBlockLocalService.removeCompanyScopePermissions(
				company.getCompanyId(), _CALENDAR_RESOURCE_NAME,
				guestRole.getRoleId(), bitwiseValue);
		}
	}

	private static final String _CALENDAR_RESOURCE_NAME =
		"com.liferay.calendar.model.CalendarResource";

	private final CompanyLocalService _companyLocalService;
	private final ResourceActionLocalService _resourceActionLocalService;
	private final ResourceBlockLocalService _resourceBlockLocalService;
	private final RoleLocalService _roleLocalService;

}