/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.removal;

import com.liferay.portal.kernel.dao.db.BaseDuplicateProcess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * @author Jorge Avalos
 */
public class TicketDuplicateProcess extends BaseDuplicateProcess {
	@Override
	public void doUpgrade() throws SQLException {
		String[][] ticketUniqueIndexes ={{"IX_123465", "_key"},
										  {"IX_4252345", "column1,column2,column3"}};// for cases when there are more than one unique index

		for(String[] uniqueIndex : ticketUniqueIndexes) {
			String indexName = uniqueIndex[0];

			List<HashMap<String, String>> deletionList = new ArrayList<>();

			List<HashMap<String, String>> ticketDuplicatesList =
				getDuplicatesSQL(uniqueIndex,"Ticket","createDate DESC");// returns all duplicates as a list with all columns(needed for displaying information)

			for(HashMap<String, String> ticketDuplicate : ticketDuplicatesList) {
				//logic for special cases to check against other tables
				deletionList.add(ticketDuplicate);
			}

			removeDuplicates("Ticket",indexName,deletionList,true);

		}

	}


	private static final Log _log = LogFactoryUtil.getLog(
		TicketDuplicateProcess.class);

}