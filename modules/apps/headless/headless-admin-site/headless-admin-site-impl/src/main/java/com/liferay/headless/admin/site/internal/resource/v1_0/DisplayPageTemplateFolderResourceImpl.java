/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.admin.site.internal.resource.v1_0;

import com.liferay.headless.admin.site.dto.v1_0.DisplayPageTemplateFolder;
import com.liferay.headless.admin.site.internal.resource.v1_0.util.DisplayPageTemplateFolderUtil;
import com.liferay.headless.admin.site.internal.resource.v1_0.util.GroupUtil;
import com.liferay.headless.admin.site.resource.v1_0.DisplayPageTemplateFolderResource;
import com.liferay.layout.page.template.constants.LayoutPageTemplateCollectionTypeConstants;
import com.liferay.layout.page.template.model.LayoutPageTemplateCollection;
import com.liferay.layout.page.template.service.LayoutPageTemplateCollectionService;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.PermissionService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.vulcan.aggregation.Aggregation;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.permission.Permission;
import com.liferay.portal.vulcan.permission.PermissionUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Rubén Pulido
 * @author Bárbara Cabrera
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/display-page-template-folder.properties",
	scope = ServiceScope.PROTOTYPE,
	service = DisplayPageTemplateFolderResource.class
)
public class DisplayPageTemplateFolderResourceImpl
	extends BaseDisplayPageTemplateFolderResourceImpl {

	@Override
	public void deleteSiteDisplayPageTemplateFolder(
			String siteExternalReferenceCode,
			String displayPageTemplateFolderExternalReferenceCode)
		throws Exception {

		if (!FeatureFlagManagerUtil.isEnabled("LPD-35443")) {
			throw new UnsupportedOperationException();
		}

		_layoutPageTemplateCollectionService.deleteLayoutPageTemplateCollection(
			displayPageTemplateFolderExternalReferenceCode,
			GroupUtil.getGroupId(
				false, contextCompany.getCompanyId(),
				siteExternalReferenceCode));
	}

	@Override
	public Page<DisplayPageTemplateFolder>
			doGetSiteDisplayPageTemplateFoldersPage(
				String siteExternalReferenceCode, String search,
				Aggregation aggregation, Filter filter, Pagination pagination,
				Sort[] sorts)
		throws Exception {

		if (!FeatureFlagManagerUtil.isEnabled("LPD-35443")) {
			throw new UnsupportedOperationException();
		}

		long groupId = GroupUtil.getGroupId(
			true, contextCompany.getCompanyId(), siteExternalReferenceCode);

		return Page.of(
			transform(
				_layoutPageTemplateCollectionService.
					getLayoutPageTemplateCollections(
						groupId, search,
						LayoutPageTemplateCollectionTypeConstants.DISPLAY_PAGE,
						pagination.getStartPosition(),
						pagination.getEndPosition(), null),
				layoutPageTemplateCollection -> _toDisplayPageTemplateFolder(
					layoutPageTemplateCollection)),
			pagination,
			_layoutPageTemplateCollectionService.
				getLayoutPageTemplateCollectionsCount(
					groupId, search,
					LayoutPageTemplateCollectionTypeConstants.DISPLAY_PAGE));
	}

	@Override
	public DisplayPageTemplateFolder doPostSiteDisplayPageTemplateFolder(
			String siteExternalReferenceCode,
			DisplayPageTemplateFolder displayPageTemplateFolder)
		throws Exception {

		if (!FeatureFlagManagerUtil.isEnabled("LPD-35443")) {
			throw new UnsupportedOperationException();
		}

		return _addDisplayPageTemplateFolder(
			displayPageTemplateFolder,
			GroupUtil.getGroupId(
				false, contextCompany.getCompanyId(),
				siteExternalReferenceCode));
	}

	@Override
	public DisplayPageTemplateFolder getSiteDisplayPageTemplateFolder(
			String siteExternalReferenceCode,
			String displayPageTemplateFolderExternalReferenceCode)
		throws Exception {

		if (!FeatureFlagManagerUtil.isEnabled("LPD-35443")) {
			throw new UnsupportedOperationException();
		}

		return _toDisplayPageTemplateFolder(
			_layoutPageTemplateCollectionService.
				getLayoutPageTemplateCollection(
					displayPageTemplateFolderExternalReferenceCode,
					GroupUtil.getGroupId(
						true, contextCompany.getCompanyId(),
						siteExternalReferenceCode)));
	}

	@Override
	public Page<Permission> getSiteDisplayPageTemplateFolderPermissionsPage(
			String siteExternalReferenceCode,
			String displayPageTemplateFolderExternalReferenceCode,
			String roleNames)
		throws Exception {

		if (!FeatureFlagManagerUtil.isEnabled("LPD-35443")) {
			throw new UnsupportedOperationException();
		}

		long groupId = GroupUtil.getGroupId(
			false, contextCompany.getCompanyId(), siteExternalReferenceCode);

		String resourceName = LayoutPageTemplateCollection.class.getName();

		LayoutPageTemplateCollection layoutPageTemplateCollection =
			_layoutPageTemplateCollectionService.
				getLayoutPageTemplateCollection(
					displayPageTemplateFolderExternalReferenceCode, groupId);

		_permissionService.checkPermission(
			groupId, resourceName,
			layoutPageTemplateCollection.getLayoutPageTemplateCollectionId());

		return toPermissionPage(
			HashMapBuilder.put(
				"get",
				addAction(
					ActionKeys.PERMISSIONS,
					"getSiteDisplayPageTemplateFolderPermissionsPage",
					resourceName,
					layoutPageTemplateCollection.
						getLayoutPageTemplateCollectionId())
			).put(
				"replace",
				addAction(
					ActionKeys.PERMISSIONS,
					"putSiteDisplayPageTemplateFolderPermissionsPage",
					resourceName,
					layoutPageTemplateCollection.
						getLayoutPageTemplateCollectionId())
			).build(),
			layoutPageTemplateCollection.getLayoutPageTemplateCollectionId(),
			resourceName, roleNames);
	}

	@Override
	public DisplayPageTemplateFolder putSiteDisplayPageTemplateFolder(
			String siteExternalReferenceCode,
			String displayPageTemplateFolderExternalReferenceCode,
			DisplayPageTemplateFolder displayPageTemplateFolder)
		throws Exception {

		if (!FeatureFlagManagerUtil.isEnabled("LPD-35443")) {
			throw new UnsupportedOperationException();
		}

		long groupId = GroupUtil.getGroupId(
			false, contextCompany.getCompanyId(), siteExternalReferenceCode);

		LayoutPageTemplateCollection layoutPageTemplateCollection =
			_layoutPageTemplateCollectionService.
				fetchLayoutPageTemplateCollection(
					displayPageTemplateFolderExternalReferenceCode, groupId);

		if (layoutPageTemplateCollection == null) {
			return _addDisplayPageTemplateFolder(
				displayPageTemplateFolder, groupId);
		}

		long parentLayoutPageTemplateCollectionId =
			DisplayPageTemplateFolderUtil.
				getParentLayoutPageTemplateCollectionId(
					displayPageTemplateFolder, groupId,
					contextHttpServletRequest);

		if (!Objects.equals(
				layoutPageTemplateCollection.
					getParentLayoutPageTemplateCollectionId(),
				parentLayoutPageTemplateCollectionId)) {

			layoutPageTemplateCollection =
				_layoutPageTemplateCollectionService.
					moveLayoutPageTemplateCollection(
						layoutPageTemplateCollection.
							getLayoutPageTemplateCollectionId(),
						parentLayoutPageTemplateCollectionId);
		}

		return _toDisplayPageTemplateFolder(
			_layoutPageTemplateCollectionService.
				updateLayoutPageTemplateCollection(
					layoutPageTemplateCollection.
						getLayoutPageTemplateCollectionId(),
					displayPageTemplateFolder.getName(),
					displayPageTemplateFolder.getDescription()));
	}

	protected Page<Permission> toPermissionPage(
			Map<String, Map<String, String>> actions, long id,
			String resourceName, String roleNames)
		throws Exception {

		List<ResourceAction> resourceActions =
			resourceActionLocalService.getResourceActions(resourceName);

		if (Validator.isNotNull(roleNames)) {
			return Page.of(
				actions,
				PermissionUtil.getPermissions(
					contextCompany.getCompanyId(), resourceActions, id,
					resourceName, StringUtil.split(roleNames)));
		}

		return Page.of(
			actions,
			PermissionUtil.getPermissions(
				contextCompany.getCompanyId(), resourceActions, id,
				resourceName, null));
	}

	private DisplayPageTemplateFolder _addDisplayPageTemplateFolder(
			DisplayPageTemplateFolder displayPageTemplateFolder, long groupId)
		throws Exception {

		return _toDisplayPageTemplateFolder(
			DisplayPageTemplateFolderUtil.addLayoutPageTemplateCollection(
				displayPageTemplateFolder, groupId, contextHttpServletRequest));
	}

	private DisplayPageTemplateFolder _toDisplayPageTemplateFolder(
			LayoutPageTemplateCollection layoutPageTemplateCollection)
		throws Exception {

		return _displayPageTemplateFolderDTOConverter.toDTO(
			layoutPageTemplateCollection);
	}

	@Reference(
		target = "(component.name=com.liferay.headless.admin.site.internal.dto.v1_0.converter.DisplayPageTemplateFolderDTOConverter)"
	)
	private DTOConverter
		<LayoutPageTemplateCollection, DisplayPageTemplateFolder>
			_displayPageTemplateFolderDTOConverter;

	@Reference
	private LayoutPageTemplateCollectionService
		_layoutPageTemplateCollectionService;

	@Reference
	private PermissionService _permissionService;

}