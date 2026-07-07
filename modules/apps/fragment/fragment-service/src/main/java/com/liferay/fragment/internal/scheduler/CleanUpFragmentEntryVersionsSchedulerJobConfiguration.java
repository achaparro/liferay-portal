/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.scheduler;

import com.liferay.fragment.configuration.FragmentEntryVersionConfiguration;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryVersion;
import com.liferay.fragment.model.FragmentEntryVersionTable;
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.sql.dsl.DSLFunctionFactoryUtil;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.scheduler.SchedulerJobConfiguration;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.TriggerConfiguration;
import com.liferay.portal.kernel.service.CompanyLocalService;

import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Georgel Pop
 */
@Component(service = SchedulerJobConfiguration.class)
public class CleanUpFragmentEntryVersionsSchedulerJobConfiguration
	implements SchedulerJobConfiguration {

	@Override
	public UnsafeRunnable<Exception> getJobExecutorUnsafeRunnable() {
		return this::_cleanUpFragmentEntryVersions;
	}

	@Override
	public TriggerConfiguration getTriggerConfiguration() {
		return TriggerConfiguration.createTriggerConfiguration(1, TimeUnit.DAY);
	}

	private void _cleanUpFragmentEntryVersions() throws Exception {
		_companyLocalService.forEachCompanyId(
			this::_cleanUpFragmentEntryVersions);
	}

	private void _cleanUpFragmentEntryVersions(long companyId) {
		try {
			List<long[]> fragmentEntryVersionCounts =
				_getFragmentEntryVersionCounts(companyId);

			for (long[] fragmentEntryVersionCount :
					fragmentEntryVersionCounts) {

				long fragmentEntryId = fragmentEntryVersionCount[0];
				int versionsToDeleteCount = (int)fragmentEntryVersionCount[1];

				_deleteFragmentEntryVersions(
					companyId, fragmentEntryId, versionsToDeleteCount);
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(exception);
			}
		}
	}

	private void _deleteFragmentEntryVersions(
		long companyId, long fragmentEntryId, int versionsToDeleteCount) {

		try {
			FragmentEntry fragmentEntry =
				_fragmentEntryLocalService.getFragmentEntry(fragmentEntryId);

			List<Integer> versions = _getVersions(
				companyId, fragmentEntryId, versionsToDeleteCount);

			for (int version : versions) {
				try {
					FragmentEntryVersion fragmentEntryVersion =
						_fragmentEntryLocalService.getVersion(
							fragmentEntry, version);

					_fragmentEntryLocalService.deleteVersion(
						fragmentEntryVersion);
				}
				catch (Exception exception) {
					if (_log.isWarnEnabled()) {
						_log.warn(exception);
					}
				}
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(exception);
			}
		}
	}

	private List<long[]> _getFragmentEntryVersionCounts(long companyId)
		throws Exception {

		FragmentEntryVersionConfiguration fragmentEntryVersionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				FragmentEntryVersionConfiguration.class, companyId);

		int maximumVersionsPerEntry =
			fragmentEntryVersionConfiguration.maximumVersionsPerEntry();

		if (maximumVersionsPerEntry <= 0) {
			return Collections.emptyList();
		}

		List<Object[]> results = _fragmentEntryLocalService.dslQuery(
			DSLQueryFactoryUtil.select(
				FragmentEntryVersionTable.INSTANCE.fragmentEntryId,
				DSLFunctionFactoryUtil.count(
					FragmentEntryVersionTable.INSTANCE.fragmentEntryVersionId
				).as(
					"versionsCount"
				)
			).from(
				FragmentEntryVersionTable.INSTANCE
			).where(
				FragmentEntryVersionTable.INSTANCE.companyId.eq(
					companyId
				).and(
					FragmentEntryVersionTable.INSTANCE.ctCollectionId.eq(
						CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION)
				)
			).groupBy(
				FragmentEntryVersionTable.INSTANCE.fragmentEntryId
			).having(
				DSLFunctionFactoryUtil.count(
					FragmentEntryVersionTable.INSTANCE.fragmentEntryVersionId
				).gt(
					(long)maximumVersionsPerEntry
				)
			));

		return TransformUtil.transform(
			results,
			result -> {
				Number count = (Number)result[1];

				return new long[] {
					(Long)result[0], count.intValue() - maximumVersionsPerEntry
				};
			});
	}

	private List<Integer> _getVersions(
		long companyId, long fragmentEntryId, int versionsToDeleteCount) {

		return _fragmentEntryLocalService.dslQuery(
			DSLQueryFactoryUtil.select(
				FragmentEntryVersionTable.INSTANCE.version
			).from(
				FragmentEntryVersionTable.INSTANCE
			).where(
				FragmentEntryVersionTable.INSTANCE.companyId.eq(
					companyId
				).and(
					FragmentEntryVersionTable.INSTANCE.fragmentEntryId.eq(
						fragmentEntryId)
				).and(
					FragmentEntryVersionTable.INSTANCE.ctCollectionId.eq(
						CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION)
				)
			).orderBy(
				FragmentEntryVersionTable.INSTANCE.version.ascending()
			).limit(
				0, versionsToDeleteCount
			));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CleanUpFragmentEntryVersionsSchedulerJobConfiguration.class);

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private FragmentEntryLocalService _fragmentEntryLocalService;

}