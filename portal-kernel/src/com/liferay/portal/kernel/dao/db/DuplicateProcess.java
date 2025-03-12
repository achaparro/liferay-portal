/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import java.util.HashMap;
import java.util.List;

/**
 * @author Jorge Avalos
 */
public interface DuplicateProcess {

	public void removeDuplicates(String tableName, String index,
								 List<HashMap<String, String>> duplicatesList, boolean deleteAll);

}