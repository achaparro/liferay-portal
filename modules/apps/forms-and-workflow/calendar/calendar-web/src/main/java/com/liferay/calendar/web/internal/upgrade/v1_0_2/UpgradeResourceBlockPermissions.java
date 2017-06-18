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

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ResourceBlockLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

/**
 * @author José María Muñoz
 */
public class UpgradeResourceBlockPermissions extends UpgradeProcess {

	public UpgradeResourceBlockPermissions(
		CompanyLocalService companyLocalService,
		ResourceBlockLocalService resourceBlockLocalService,
		RoleLocalService roleLocalService) {

		_companyLocalService = companyLocalService;
		_resourceBlockLocalService = resourceBlockLocalService;
		_roleLocalService = roleLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		upgradeGuestResourceBlockPermissions();
	}

	protected void upgradeGuestResourceBlockPermissions() throws Exception {
		for (Company company : _companyLocalService.getCompanies()) {
			Role guestRole = _roleLocalService.getRole(
				company.getCompanyId(), RoleConstants.GUEST);

			_resourceBlockLocalService.setCompanyScopePermissions(
				company.getCompanyId(), _CALENDAR_RESOURCE_NAME,
				guestRole.getRoleId(), 0);
		}
	}

	private static final String _CALENDAR_RESOURCE_NAME =
		"com.liferay.calendar.model.CalendarResource";

	private final CompanyLocalService _companyLocalService;
	private final ResourceBlockLocalService _resourceBlockLocalService;
	private final RoleLocalService _roleLocalService;

}