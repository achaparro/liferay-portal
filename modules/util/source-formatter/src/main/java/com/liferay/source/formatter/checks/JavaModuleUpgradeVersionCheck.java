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

package com.liferay.source.formatter.checks;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.util.FileUtil;

import java.io.File;

/**
 * @author Hugo Huijser
 */
public class JavaModuleUpgradeVersionCheck extends BaseUpgradeVersionCheck {

	protected String getImplementedClassName() {
		return "UpgradeStepRegistrator";
	}

	protected String getMethodName() {
		return "register";
	}

	protected String getSQLFileContent(String absolutePath, String className)
		throws Exception {

		String fileLocation = StringUtil.replaceLast(
			absolutePath, "/java/", "/resources/");

		int x = fileLocation.lastIndexOf("/com/liferay/");

		int y = className.lastIndexOf(CharPool.PERIOD);

		String packageName = className.substring(0, y);

		fileLocation = StringBundler.concat(
			fileLocation.substring(0, x + 1),
			StringUtil.replace(packageName, CharPool.PERIOD, CharPool.SLASH),
			"/dependencies/update.sql");

		File file = new File(fileLocation);

		if (file.exists()) {
			return FileUtil.read(file);
		}

		return null;
	}

}