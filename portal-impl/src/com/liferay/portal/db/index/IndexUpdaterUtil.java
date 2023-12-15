/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.index;

import com.liferay.portal.db.DBResourceUtil;
import com.liferay.portal.kernel.concurrent.ThreadPoolExecutor;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dependency.manager.DependencyManagerSyncUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.util.BundleUtil;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.tools.DBUpgrader;

import java.sql.Connection;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author Ricardo Couso
 */
public class IndexUpdaterUtil {

	public static void updateAllIndexes() {
		if (!_updatedBundleSymbolicNames.contains("portal")) {
			updatePortalIndexes();
		}

		BundleTracker<Void> bundleTracker = new BundleTracker<>(
			SystemBundleUtil.getBundleContext(), Bundle.ACTIVE,
			new BundleTrackerCustomizer<Void>() {

				@Override
				public Void addingBundle(
					Bundle bundle, BundleEvent bundleEvent) {

					if (BundleUtil.isLiferayServiceBundle(bundle)) {
						try {
							if (!_updatedBundleSymbolicNames.contains(
									bundle.getSymbolicName())) {

								updateIndexes(bundle);
							}
						}
						catch (Exception exception) {
							_log.error(exception);
						}
					}

					return null;
				}

				@Override
				public void modifiedBundle(
					Bundle bundle, BundleEvent bundleEvent, Void tracked) {
				}

				@Override
				public void removedBundle(
					Bundle bundle, BundleEvent bundleEvent, Void tracked) {
				}

			});

		DependencyManagerSyncUtil.registerSyncFutureTask(
			new FutureTask<>(
				() -> {
					bundleTracker.open();

					DependencyManagerSyncUtil.registerSyncCallable(
						() -> {
							bundleTracker.close();

							if (DBUpgrader.isUpgradeClient()) {
								_threadPoolExecutor.awaitTermination(
									1, TimeUnit.DAYS);

								_threadPoolExecutor.shutdown();
							}

							return null;
						});

					return null;
				}),
			IndexUpdaterUtil.class.getName() + "-BundleTrackerOpener");
	}

	public static void updateIndexes(Bundle bundle) throws Exception {
		String indexesSQL = DBResourceUtil.getModuleIndexesSQL(bundle);
		String tablesSQL = DBResourceUtil.getModuleTablesSQL(bundle);

		if ((indexesSQL == null) || (tablesSQL == null)) {
			return;
		}

		DB db = DBManagerUtil.getDB();

		db.process(
			companyId -> {
				if (_DATABASE_INDEXES_UPDATE_ON_BACKGROUND) {
					_threadPoolExecutor.execute(
						() -> {
							try {
								_updateIndexes(
									db, companyId, bundle.getSymbolicName(),
									tablesSQL, indexesSQL);
							}
							catch (Exception exception) {
								_log.error(
									StringBundler.concat(
										"Unable to update database indexes ",
										"for ", bundle.getSymbolicName(),
										" due to ", exception.getMessage()));
							}
						});
				}
				else {
					_updateIndexes(
						db, companyId, bundle.getSymbolicName(), tablesSQL,
						indexesSQL);
				}
			});

		_updatedBundleSymbolicNames.add(bundle.getSymbolicName());
	}

	public static void updatePortalIndexes() {
		DB db = DBManagerUtil.getDB();

		try {
			db.process(
				companyId -> {
					if (_DATABASE_INDEXES_UPDATE_ON_BACKGROUND) {
						_threadPoolExecutor.execute(
							() -> {
								try {
									_updateIndexes(
										db, companyId, null,
										DBResourceUtil.getPortalTablesSQL(),
										DBResourceUtil.getPortalIndexesSQL());
								}
								catch (Exception exception) {
									_log.error(
										"Unable to update portal database " +
											"indexes due to " +
												exception.getMessage());
								}
							});
					}
					else {
						_updateIndexes(
							db, companyId, null,
							DBResourceUtil.getPortalTablesSQL(),
							DBResourceUtil.getPortalIndexesSQL());
					}
				});
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(exception);
			}
		}

		_updatedBundleSymbolicNames.add("portal");
	}

	private static void _updateIndexes(
			DB db, Long companyId, String bundleSymbolicName, String tablesSQL,
			String indexesSQL)
		throws Exception {

		String message = new String("Updating portal database indexes");

		if (bundleSymbolicName != null) {
			message = new String(
				"Updating database indexes for " + bundleSymbolicName);
		}

		if (Validator.isNotNull(companyId)) {
			message += " and company " + companyId;
		}

		try (Connection connection = DataAccess.getConnection();
			LoggingTimer loggingTimer = new LoggingTimer(message)) {

			db.updateIndexes(connection, tablesSQL, indexesSQL, true);
		}
	}

	private static final boolean _DATABASE_INDEXES_UPDATE_ON_BACKGROUND =
		GetterUtil.getBoolean(
			PropsUtil.get("database.indexes.update.on.background"));

	private static final Log _log = LogFactoryUtil.getLog(
		IndexUpdaterUtil.class);

	private static final ThreadPoolExecutor _threadPoolExecutor =
		new ThreadPoolExecutor(2, 5);
	private static final Set<String> _updatedBundleSymbolicNames =
		new HashSet<>();

}