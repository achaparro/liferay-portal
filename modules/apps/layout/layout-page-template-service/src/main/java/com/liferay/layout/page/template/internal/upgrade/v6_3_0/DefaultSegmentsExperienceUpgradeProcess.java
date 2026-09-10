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
import com.liferay.segments.service.SegmentsExperienceLocalService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
		_addDefaultSegmentsExperiences();

		_updateMisScopedSegmentsExperienceIds();

		_updateOrphanedSegmentsExperienceIds();
	}

	private void _addDefaultSegmentsExperience(
			long companyId, String externalReferenceCode, long groupId,
			long plid, long userId)
		throws Exception {

		Locale siteDefaultLocale = LocaleThreadLocal.getSiteDefaultLocale();

		try {
			LocaleThreadLocal.setSiteDefaultLocale(
				_portal.getSiteDefaultLocale(groupId));

			_segmentsExperienceLocalService.addDefaultSegmentsExperience(
				externalReferenceCode +
					LayoutConstants.EXTERNAL_REFERENCE_CODE_SUFFIX_DEFAULT,
				_getUserId(companyId, userId), plid, new ServiceContext());
		}
		finally {
			LocaleThreadLocal.setSiteDefaultLocale(siteDefaultLocale);
		}
	}

	private void _addDefaultSegmentsExperiences() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select Layout.companyId, Layout.ctCollectionId, ",
					"Layout.externalReferenceCode, Layout.groupId, ",
					"Layout.plid, Layout.userId from Layout where ",
					"Layout.type_ in (?, ?, ?) and not exists (select 1 from ",
					"SegmentsExperience where SegmentsExperience.groupId = ",
					"Layout.groupId and SegmentsExperience.plid = Layout.plid ",
					"and SegmentsExperience.segmentsExperienceKey = ? and ",
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

						_addDefaultSegmentsExperience(
							resultSet.getLong("companyId"),
							resultSet.getString("externalReferenceCode"),
							resultSet.getLong("groupId"),
							resultSet.getLong("plid"),
							resultSet.getLong("userId"));
					}
				}
			}
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

	private Set<Long> _getAmbiguousPlids() throws Exception {
		Map<Long, Set<Long>> orphanedSegmentsExperienceIdsMap = new HashMap<>();

		_putOrphanedSegmentsExperienceIds(
			orphanedSegmentsExperienceIdsMap,
			StringBundler.concat(
				"select distinct FragmentEntryLink.plid, ",
				"FragmentEntryLink.segmentsExperienceId from ",
				"FragmentEntryLink where ",
				"FragmentEntryLink.segmentsExperienceId > 0 and not exists ",
				"(select 1 from SegmentsExperience where ",
				"SegmentsExperience.segmentsExperienceId = ",
				"FragmentEntryLink.segmentsExperienceId)"));

		_putOrphanedSegmentsExperienceIds(
			orphanedSegmentsExperienceIdsMap,
			StringBundler.concat(
				"select distinct LayoutPageTemplateStructure.plid, ",
				"LayoutPageTemplateStructureRel.segmentsExperienceId from ",
				"LayoutPageTemplateStructureRel inner join ",
				"LayoutPageTemplateStructure on ",
				"LayoutPageTemplateStructureRel.layoutPageTemplateStructureId ",
				"= LayoutPageTemplateStructure.layoutPageTemplateStructureId ",
				"and LayoutPageTemplateStructure.ctCollectionId in (0, ",
				"LayoutPageTemplateStructureRel.ctCollectionId) where ",
				"LayoutPageTemplateStructureRel.segmentsExperienceId > 0 and ",
				"not exists (select 1 from SegmentsExperience where ",
				"SegmentsExperience.segmentsExperienceId = ",
				"LayoutPageTemplateStructureRel.segmentsExperienceId)"));

		Set<Long> ambiguousPlids = new HashSet<>();

		for (Map.Entry<Long, Set<Long>> entry :
				orphanedSegmentsExperienceIdsMap.entrySet()) {

			Set<Long> orphanedSegmentsExperienceIds = entry.getValue();

			if (orphanedSegmentsExperienceIds.size() < 2) {
				continue;
			}

			ambiguousPlids.add(entry.getKey());

			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Unable to repoint layout ", entry.getKey(),
						" because it references the orphaned segments ",
						"experiences ",
						StringUtil.merge(orphanedSegmentsExperienceIds, ", "),
						" and the correct mapping is ambiguous"));
			}
		}

		return ambiguousPlids;
	}

	private long _getSegmentsExperienceId(
			long ctCollectionId, long plid, String segmentsExperienceKey)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select segmentsExperienceId from SegmentsExperience ",
					"where plid = ? and segmentsExperienceKey = ? and ",
					"ctCollectionId in (0, ?)"))) {

			preparedStatement.setLong(1, plid);
			preparedStatement.setString(2, segmentsExperienceKey);
			preparedStatement.setLong(3, ctCollectionId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("segmentsExperienceId");
				}
			}
		}

		return 0;
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

	private void _logMissingSegmentsExperience(
		long plid, String segmentsExperienceKey) {

		if (_log.isWarnEnabled()) {
			_log.warn(
				StringBundler.concat(
					"Unable to repoint layout ", plid,
					" because it has no segments experience with the key ",
					segmentsExperienceKey));
		}
	}

	private void _putOrphanedSegmentsExperienceIds(
			Map<Long, Set<Long>> orphanedSegmentsExperienceIdsMap, String sql)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				sql)) {

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					Set<Long> orphanedSegmentsExperienceIds =
						orphanedSegmentsExperienceIdsMap.computeIfAbsent(
							resultSet.getLong("plid"), plid -> new HashSet<>());

					orphanedSegmentsExperienceIds.add(
						resultSet.getLong("segmentsExperienceId"));
				}
			}
		}
	}

	private void _updateFragmentEntryLink(
			long ctCollectionId, long groupId, long newSegmentsExperienceId,
			long oldSegmentsExperienceId, long plid)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"update FragmentEntryLink set segmentsExperienceId = ? ",
					"where groupId = ? and plid = ? and ctCollectionId = ? ",
					"and segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, newSegmentsExperienceId);
			preparedStatement.setLong(2, groupId);
			preparedStatement.setLong(3, plid);
			preparedStatement.setLong(4, ctCollectionId);
			preparedStatement.setLong(5, oldSegmentsExperienceId);

			preparedStatement.executeUpdate();
		}
	}

	private void _updateLayoutPageTemplateStructureRel(
			long ctCollectionId, long layoutPageTemplateStructureId,
			long newSegmentsExperienceId, long oldSegmentsExperienceId)
		throws Exception {

		if (_hasLayoutPageTemplateStructureRel(
				ctCollectionId, layoutPageTemplateStructureId,
				newSegmentsExperienceId)) {

			_deleteLayoutPageTemplateStructureRel(
				ctCollectionId, layoutPageTemplateStructureId,
				oldSegmentsExperienceId);

			return;
		}

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"update LayoutPageTemplateStructureRel set ",
					"segmentsExperienceId = ? where ctCollectionId = ? and ",
					"layoutPageTemplateStructureId = ? and ",
					"segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, newSegmentsExperienceId);
			preparedStatement.setLong(2, ctCollectionId);
			preparedStatement.setLong(3, layoutPageTemplateStructureId);
			preparedStatement.setLong(4, oldSegmentsExperienceId);

			preparedStatement.executeUpdate();
		}
	}

	private void _updateMisScopedFragmentEntryLinks() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct FragmentEntryLink.ctCollectionId, ",
					"FragmentEntryLink.groupId, FragmentEntryLink.plid, ",
					"FragmentEntryLink.segmentsExperienceId, ",
					"SegmentsExperience.segmentsExperienceKey from ",
					"FragmentEntryLink inner join SegmentsExperience on ",
					"SegmentsExperience.segmentsExperienceId = ",
					"FragmentEntryLink.segmentsExperienceId and ",
					"SegmentsExperience.ctCollectionId in (0, ",
					"FragmentEntryLink.ctCollectionId) where ",
					"FragmentEntryLink.segmentsExperienceId > 0 and ",
					"SegmentsExperience.plid != FragmentEntryLink.plid"))) {

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					long plid = resultSet.getLong("plid");

					String segmentsExperienceKey = resultSet.getString(
						"segmentsExperienceKey");

					long ctCollectionId = resultSet.getLong("ctCollectionId");

					long segmentsExperienceId = _getSegmentsExperienceId(
						ctCollectionId, plid, segmentsExperienceKey);

					if (segmentsExperienceId == 0) {
						_logMissingSegmentsExperience(
							plid, segmentsExperienceKey);

						continue;
					}

					_updateFragmentEntryLink(
						ctCollectionId, resultSet.getLong("groupId"),
						segmentsExperienceId,
						resultSet.getLong("segmentsExperienceId"), plid);
				}
			}
		}
	}

	private void _updateMisScopedLayoutPageTemplateStructureRels()
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ",
					"LayoutPageTemplateStructureRel.ctCollectionId, ",
					"LayoutPageTemplateStructureRel.",
					"layoutPageTemplateStructureId, ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId, ",
					"LayoutPageTemplateStructure.plid, ",
					"SegmentsExperience.segmentsExperienceKey from ",
					"LayoutPageTemplateStructureRel inner join ",
					"LayoutPageTemplateStructure on ",
					"LayoutPageTemplateStructureRel.",
					"layoutPageTemplateStructureId = ",
					"LayoutPageTemplateStructure.",
					"layoutPageTemplateStructureId and ",
					"LayoutPageTemplateStructure.ctCollectionId in (0, ",
					"LayoutPageTemplateStructureRel.ctCollectionId) inner ",
					"join SegmentsExperience on ",
					"SegmentsExperience.segmentsExperienceId = ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId and ",
					"SegmentsExperience.ctCollectionId in (0, ",
					"LayoutPageTemplateStructureRel.ctCollectionId) where ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId > 0 ",
					"and SegmentsExperience.plid != ",
					"LayoutPageTemplateStructure.plid"))) {

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					long plid = resultSet.getLong("plid");

					String segmentsExperienceKey = resultSet.getString(
						"segmentsExperienceKey");

					long ctCollectionId = resultSet.getLong("ctCollectionId");

					long segmentsExperienceId = _getSegmentsExperienceId(
						ctCollectionId, plid, segmentsExperienceKey);

					if (segmentsExperienceId == 0) {
						_logMissingSegmentsExperience(
							plid, segmentsExperienceKey);

						continue;
					}

					_updateLayoutPageTemplateStructureRel(
						ctCollectionId,
						resultSet.getLong("layoutPageTemplateStructureId"),
						segmentsExperienceId,
						resultSet.getLong("segmentsExperienceId"));
				}
			}
		}
	}

	private void _updateMisScopedSegmentsExperienceIds() throws Exception {
		_updateMisScopedLayoutPageTemplateStructureRels();

		_updateMisScopedFragmentEntryLinks();
	}

	private void _updateOrphanedFragmentEntryLinks(Set<Long> ambiguousPlids)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct FragmentEntryLink.ctCollectionId, ",
					"FragmentEntryLink.groupId, FragmentEntryLink.plid, ",
					"FragmentEntryLink.segmentsExperienceId from ",
					"FragmentEntryLink where ",
					"FragmentEntryLink.segmentsExperienceId > 0 and not ",
					"exists (select 1 from SegmentsExperience where ",
					"SegmentsExperience.segmentsExperienceId = ",
					"FragmentEntryLink.segmentsExperienceId)"))) {

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					long plid = resultSet.getLong("plid");

					if (ambiguousPlids.contains(plid)) {
						continue;
					}

					String segmentsExperienceKey =
						SegmentsExperienceConstants.KEY_DEFAULT;

					long ctCollectionId = resultSet.getLong("ctCollectionId");

					long segmentsExperienceId = _getSegmentsExperienceId(
						ctCollectionId, plid, segmentsExperienceKey);

					if (segmentsExperienceId == 0) {
						_logMissingSegmentsExperience(
							plid, segmentsExperienceKey);

						continue;
					}

					_updateFragmentEntryLink(
						ctCollectionId, resultSet.getLong("groupId"),
						segmentsExperienceId,
						resultSet.getLong("segmentsExperienceId"), plid);
				}
			}
		}
	}

	private void _updateOrphanedLayoutPageTemplateStructureRels(
			Set<Long> ambiguousPlids)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ",
					"LayoutPageTemplateStructureRel.ctCollectionId, ",
					"LayoutPageTemplateStructureRel.",
					"layoutPageTemplateStructureId, ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId, ",
					"LayoutPageTemplateStructure.plid from ",
					"LayoutPageTemplateStructureRel inner join ",
					"LayoutPageTemplateStructure on ",
					"LayoutPageTemplateStructureRel.",
					"layoutPageTemplateStructureId = ",
					"LayoutPageTemplateStructure.",
					"layoutPageTemplateStructureId and ",
					"LayoutPageTemplateStructure.ctCollectionId in (0, ",
					"LayoutPageTemplateStructureRel.ctCollectionId) where ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId > 0 ",
					"and not exists (select 1 from SegmentsExperience where ",
					"SegmentsExperience.segmentsExperienceId = ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId)"))) {

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					long plid = resultSet.getLong("plid");

					if (ambiguousPlids.contains(plid)) {
						continue;
					}

					String segmentsExperienceKey =
						SegmentsExperienceConstants.KEY_DEFAULT;

					long ctCollectionId = resultSet.getLong("ctCollectionId");

					long segmentsExperienceId = _getSegmentsExperienceId(
						ctCollectionId, plid, segmentsExperienceKey);

					if (segmentsExperienceId == 0) {
						_logMissingSegmentsExperience(
							plid, segmentsExperienceKey);

						continue;
					}

					_updateLayoutPageTemplateStructureRel(
						ctCollectionId,
						resultSet.getLong("layoutPageTemplateStructureId"),
						segmentsExperienceId,
						resultSet.getLong("segmentsExperienceId"));
				}
			}
		}
	}

	private void _updateOrphanedSegmentsExperienceIds() throws Exception {
		Set<Long> ambiguousPlids = _getAmbiguousPlids();

		_updateOrphanedLayoutPageTemplateStructureRels(ambiguousPlids);

		_updateOrphanedFragmentEntryLinks(ambiguousPlids);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DefaultSegmentsExperienceUpgradeProcess.class);

	private final Portal _portal;
	private final SegmentsExperienceLocalService
		_segmentsExperienceLocalService;
	private final UserLocalService _userLocalService;

}