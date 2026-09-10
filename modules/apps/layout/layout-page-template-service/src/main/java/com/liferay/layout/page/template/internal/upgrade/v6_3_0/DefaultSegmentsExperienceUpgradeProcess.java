/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.page.template.internal.upgrade.v6_3_0;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LocaleThreadLocal;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.segments.constants.SegmentsExperienceConstants;
import com.liferay.segments.model.SegmentsExperience;
import com.liferay.segments.service.SegmentsExperienceLocalService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Balázs Sáfrány-Kovalik
 */
public class DefaultSegmentsExperienceUpgradeProcess extends UpgradeProcess {

	public DefaultSegmentsExperienceUpgradeProcess(
		Portal portal,
		SegmentsExperienceLocalService segmentsExperienceLocalService,
		UserLocalService userLocalService) {

		_portal = portal;
		_segmentsExperienceLocalService = segmentsExperienceLocalService;
		_userLocalService = userLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select Layout.ctCollectionId, ",
					"Layout.externalReferenceCode, Layout.plid, ",
					"Layout.groupId, Layout.companyId, Layout.userId from ",
					"Layout where Layout.type_ in (?, ?, ?) and not exists ",
					"(select 1 from SegmentsExperience where ",
					"SegmentsExperience.groupId = Layout.groupId and ",
					"SegmentsExperience.plid = Layout.plid and ",
					"SegmentsExperience.segmentsExperienceKey = ? and ",
					"SegmentsExperience.ctCollectionId in (0, ",
					"Layout.ctCollectionId)) order by ",
					"Layout.ctCollectionId"))) {

			preparedStatement.setString(1, LayoutConstants.TYPE_CONTENT);
			preparedStatement.setString(2, LayoutConstants.TYPE_ASSET_DISPLAY);
			preparedStatement.setString(3, LayoutConstants.TYPE_UTILITY);
			preparedStatement.setString(
				4, SegmentsExperienceConstants.KEY_DEFAULT);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					long ctCollectionId = resultSet.getLong("ctCollectionId");

					try (SafeCloseable safeCloseable =
							CTCollectionThreadLocal.
								setCTCollectionIdWithSafeCloseable(
									ctCollectionId)) {

						_updateSegmentsExperienceIds(
							resultSet.getLong("companyId"), ctCollectionId,
							resultSet.getString("externalReferenceCode"),
							resultSet.getLong("groupId"),
							resultSet.getLong("plid"),
							resultSet.getLong("userId"));
					}
				}
			}
		}
	}

	private long _addDefaultSegmentsExperience(
			long companyId, String externalReferenceCode, long groupId,
			long plid, long userId)
		throws Exception {

		Locale siteDefaultLocale = LocaleThreadLocal.getSiteDefaultLocale();

		try {
			LocaleThreadLocal.setSiteDefaultLocale(
				_portal.getSiteDefaultLocale(groupId));

			SegmentsExperience segmentsExperience =
				_segmentsExperienceLocalService.addDefaultSegmentsExperience(
					externalReferenceCode +
						LayoutConstants.EXTERNAL_REFERENCE_CODE_SUFFIX_DEFAULT,
					_getUserId(companyId, userId), plid, new ServiceContext());

			return segmentsExperience.getSegmentsExperienceId();
		}
		finally {
			LocaleThreadLocal.setSiteDefaultLocale(siteDefaultLocale);
		}
	}

	private void _deleteLayoutPageTemplateStructureRel(
			long ctCollectionId, long layoutPageTemplateStructureId,
			long segmentsExperienceId)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"delete from LayoutPageTemplateStructureRel where ",
					"ctCollectionId = ? and layoutPageTemplateStructureId = ? ",
					"and segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, ctCollectionId);
			preparedStatement.setLong(2, layoutPageTemplateStructureId);
			preparedStatement.setLong(3, segmentsExperienceId);

			preparedStatement.executeUpdate();
		}
	}

	private long _getDefaultSegmentsExperienceId(
			long companyId, String externalReferenceCode, long groupId,
			long plid, long userId)
		throws Exception {

		long defaultSegmentsExperienceId =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperienceId(
				plid);

		if (defaultSegmentsExperienceId !=
				SegmentsExperienceConstants.ID_DEFAULT) {

			return defaultSegmentsExperienceId;
		}

		return _addDefaultSegmentsExperience(
			companyId, externalReferenceCode, groupId, plid, userId);
	}

	private Set<Long> _getFragmentEntryLinkSegmentsExperienceIds(
			long ctCollectionId, long groupId, long plid)
		throws Exception {

		Set<Long> segmentsExperienceIds = new TreeSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct segmentsExperienceId from ",
					"FragmentEntryLink where groupId = ? and plid = ? and ",
					"ctCollectionId = ? and segmentsExperienceId > 0"))) {

			preparedStatement.setLong(1, groupId);
			preparedStatement.setLong(2, plid);
			preparedStatement.setLong(3, ctCollectionId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					segmentsExperienceIds.add(
						resultSet.getLong("segmentsExperienceId"));
				}
			}
		}

		return segmentsExperienceIds;
	}

	private long _getLayoutPageTemplateStructureId(
			long ctCollectionId, long groupId, long plid)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select layoutPageTemplateStructureId from ",
					"LayoutPageTemplateStructure where groupId = ? and plid = ",
					"? and ctCollectionId in (0, ?)"))) {

			preparedStatement.setLong(1, groupId);
			preparedStatement.setLong(2, plid);
			preparedStatement.setLong(3, ctCollectionId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("layoutPageTemplateStructureId");
				}
			}
		}

		return 0;
	}

	private Set<Long> _getLayoutPageTemplateStructureRelSegmentsExperienceIds(
			long ctCollectionId, long layoutPageTemplateStructureId)
		throws Exception {

		Set<Long> segmentsExperienceIds = new TreeSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct segmentsExperienceId from ",
					"LayoutPageTemplateStructureRel where ctCollectionId = ? ",
					"and layoutPageTemplateStructureId = ? and ",
					"segmentsExperienceId > 0"))) {

			preparedStatement.setLong(1, ctCollectionId);
			preparedStatement.setLong(2, layoutPageTemplateStructureId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					segmentsExperienceIds.add(
						resultSet.getLong("segmentsExperienceId"));
				}
			}
		}

		return segmentsExperienceIds;
	}

	private Set<Long> _getOrphanedSegmentsExperienceIds(
		long defaultSegmentsExperienceId, long plid,
		Set<Long> segmentsExperienceIds) {

		Set<Long> orphanedSegmentsExperienceIds = new TreeSet<>();

		int deadSegmentsExperienceIdsCount = 0;

		for (long segmentsExperienceId : segmentsExperienceIds) {
			if (segmentsExperienceId == defaultSegmentsExperienceId) {
				continue;
			}

			SegmentsExperience segmentsExperience =
				_segmentsExperienceLocalService.fetchSegmentsExperience(
					segmentsExperienceId);

			if (segmentsExperience == null) {
				deadSegmentsExperienceIdsCount++;

				orphanedSegmentsExperienceIds.add(segmentsExperienceId);
			}
			else if (segmentsExperience.getPlid() != plid) {
				if (segmentsExperience.isDefault()) {
					orphanedSegmentsExperienceIds.add(segmentsExperienceId);
				}
				else if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to repoint layout ", plid,
							" because it references segments experience ",
							segmentsExperienceId, " of layout ",
							segmentsExperience.getPlid()));
				}
			}
		}

		if (deadSegmentsExperienceIdsCount > 1) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Unable to repoint layout ", plid,
						" because it references the orphaned segments ",
						"experiences ",
						StringUtil.merge(orphanedSegmentsExperienceIds, ", "),
						" and the correct mapping is ambiguous"));
			}

			orphanedSegmentsExperienceIds.clear();
		}

		return orphanedSegmentsExperienceIds;
	}

	private long _getUserId(long companyId, long userId) throws Exception {
		User user = _userLocalService.fetchUser(userId);

		if (user == null) {
			return _userLocalService.getGuestUserId(companyId);
		}

		return userId;
	}

	private boolean _hasLayoutPageTemplateStructureRel(
			long ctCollectionId, long layoutPageTemplateStructureId,
			long segmentsExperienceId)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select 1 from LayoutPageTemplateStructureRel where ",
					"ctCollectionId = ? and layoutPageTemplateStructureId = ? ",
					"and segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, ctCollectionId);
			preparedStatement.setLong(2, layoutPageTemplateStructureId);
			preparedStatement.setLong(3, segmentsExperienceId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	private void _updateFragmentEntryLinks(
			long ctCollectionId, long defaultSegmentsExperienceId, long groupId,
			long orphanedSegmentsExperienceId, long plid)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"update FragmentEntryLink set segmentsExperienceId = ? ",
					"where groupId = ? and plid = ? and ctCollectionId = ? ",
					"and segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, defaultSegmentsExperienceId);
			preparedStatement.setLong(2, groupId);
			preparedStatement.setLong(3, plid);
			preparedStatement.setLong(4, ctCollectionId);
			preparedStatement.setLong(5, orphanedSegmentsExperienceId);

			preparedStatement.executeUpdate();
		}
	}

	private void _updateLayoutPageTemplateStructureRel(
			long ctCollectionId, long defaultSegmentsExperienceId,
			long layoutPageTemplateStructureId,
			long orphanedSegmentsExperienceId, long plid)
		throws Exception {

		if (_hasLayoutPageTemplateStructureRel(
				ctCollectionId, layoutPageTemplateStructureId,
				defaultSegmentsExperienceId)) {

			_deleteLayoutPageTemplateStructureRel(
				ctCollectionId, layoutPageTemplateStructureId,
				orphanedSegmentsExperienceId);

			return;
		}

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"update LayoutPageTemplateStructureRel set ",
					"segmentsExperienceId = ? where ctCollectionId = ? and ",
					"layoutPageTemplateStructureId = ? and ",
					"segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, defaultSegmentsExperienceId);
			preparedStatement.setLong(2, ctCollectionId);
			preparedStatement.setLong(3, layoutPageTemplateStructureId);
			preparedStatement.setLong(4, orphanedSegmentsExperienceId);

			preparedStatement.executeUpdate();
		}
	}

	private void _updateSegmentsExperienceIds(
			long companyId, long ctCollectionId, String externalReferenceCode,
			long groupId, long plid, long userId)
		throws Exception {

		long defaultSegmentsExperienceId = _getDefaultSegmentsExperienceId(
			companyId, externalReferenceCode, groupId, plid, userId);

		Set<Long> fragmentEntryLinkSegmentsExperienceIds =
			_getFragmentEntryLinkSegmentsExperienceIds(
				ctCollectionId, groupId, plid);

		long layoutPageTemplateStructureId = _getLayoutPageTemplateStructureId(
			ctCollectionId, groupId, plid);

		Set<Long> layoutPageTemplateStructureRelSegmentsExperienceIds =
			_getLayoutPageTemplateStructureRelSegmentsExperienceIds(
				ctCollectionId, layoutPageTemplateStructureId);

		Set<Long> segmentsExperienceIds = new TreeSet<>(
			fragmentEntryLinkSegmentsExperienceIds);

		segmentsExperienceIds.addAll(
			layoutPageTemplateStructureRelSegmentsExperienceIds);

		for (long orphanedSegmentsExperienceId :
				_getOrphanedSegmentsExperienceIds(
					defaultSegmentsExperienceId, plid, segmentsExperienceIds)) {

			if (fragmentEntryLinkSegmentsExperienceIds.contains(
					orphanedSegmentsExperienceId)) {

				_updateFragmentEntryLinks(
					ctCollectionId, defaultSegmentsExperienceId, groupId,
					orphanedSegmentsExperienceId, plid);
			}

			if (layoutPageTemplateStructureRelSegmentsExperienceIds.contains(
					orphanedSegmentsExperienceId)) {

				_updateLayoutPageTemplateStructureRel(
					ctCollectionId, defaultSegmentsExperienceId,
					layoutPageTemplateStructureId, orphanedSegmentsExperienceId,
					plid);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DefaultSegmentsExperienceUpgradeProcess.class);

	private final Portal _portal;
	private final SegmentsExperienceLocalService
		_segmentsExperienceLocalService;
	private final UserLocalService _userLocalService;

}