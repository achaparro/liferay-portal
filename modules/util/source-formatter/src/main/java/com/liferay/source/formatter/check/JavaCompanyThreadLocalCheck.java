/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.StringBundler;

/**
 * @author Alan Huang
 */
public class JavaCompanyThreadLocalCheck extends BaseFileCheck {

	@Override
	public boolean isLiferaySourceCheck() {
		return true;
	}

	@Override
	protected String doProcess(
		String fileName, String absolutePath, String content) {

		int x = -1;

		while (true) {
			x = content.indexOf("CompanyThreadLocal.setCompanyId(", x + 1);

			if (x == -1) {
				break;
			}

			addMessage(
				fileName,
				StringBundler.concat(
					"Do not use \"CompanyThreadLocal.setCompanyId\", use ",
					"\"CompanyThreadLocal.setCompanyIdWithSafeCloseable\" ",
					"instead, see LPD-49356."),
				getLineNumber(content, x));
		}

		return content;
	}

}