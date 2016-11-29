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

package com.liferay.source.formatter.checkstyle.checks;

import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.checkstyle.util.DetailASTUtil;
import com.liferay.source.formatter.util.FileUtil;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hugo Huijser
 */
public class ConstantInPublicAPICheck extends AbstractCheck {

	public static final String MSG_PUBLIC_API_CONSTANT = "constant.public.api";

	@Override
	public int[] getDefaultTokens() {
		return new int[] {TokenTypes.PACKAGE_DEF};
	}

	@Override
	public void visitToken(DetailAST detailAST) {
		FileContents fileContents = getFileContents();

		String fileName = StringUtil.replace(
			fileContents.getFileName(), CharPool.BACK_SLASH, CharPool.SLASH);

		Matcher matcher = _moduleSrcPattern.matcher(fileName);

		if (!matcher.find()) {
			return;
		}

		DetailAST classDefAST = _getClassDefAST(detailAST);

		if (classDefAST == null) {
			return;
		}

		DetailAST modifiersAST = classDefAST.findFirstToken(
			TokenTypes.MODIFIERS);

		if (!modifiersAST.branchContains(TokenTypes.LITERAL_PUBLIC)) {
			return;
		}

		DetailAST objBlockAST = classDefAST.findFirstToken(TokenTypes.OBJBLOCK);

		List<DetailAST> variableDefASTList = DetailASTUtil.getAllChildTokens(
			objBlockAST, false, TokenTypes.VARIABLE_DEF);

		if (variableDefASTList.isEmpty()) {
			return;
		}

		if (!ArrayUtil.contains(
				_getExportPackages(matcher.group(1)),
				_getPackageName(detailAST))) {

			return;
		}

		for (DetailAST variableDefAST : variableDefASTList) {
			modifiersAST = variableDefAST.findFirstToken(TokenTypes.MODIFIERS);

			if (modifiersAST.branchContains(TokenTypes.FINAL) &&
				modifiersAST.branchContains(TokenTypes.LITERAL_STATIC) &&
				(modifiersAST.branchContains(TokenTypes.LITERAL_PROTECTED) ||
				 modifiersAST.branchContains(TokenTypes.LITERAL_PUBLIC))) {

				DetailAST nameAST = variableDefAST.findFirstToken(
					TokenTypes.IDENT);

				log(
					variableDefAST.getLineNo(), "MSG_PUBLIC_API_CONSTANT",
					nameAST.getText());
			}
		}
	}

	private DetailAST _getClassDefAST(DetailAST packageDefAST) {
		DetailAST sibling = packageDefAST.getNextSibling();

		while (true) {
			if (sibling == null) {
				return null;
			}

			if (sibling.getType() == TokenTypes.CLASS_DEF) {
				return sibling;
			}

			sibling = sibling.getNextSibling();
		}
	}

	private String[] _getExportPackages(String modulePath) {
		String[] exportPackages = _exportPackagesMap.get(modulePath);

		if (exportPackages != null) {
			return exportPackages;
		}

		exportPackages = new String[0];

		File bndFile = new File(modulePath + "bnd.bnd");

		if (!bndFile.exists()) {
			_exportPackagesMap.put(modulePath, exportPackages);

			return exportPackages;
		}

		try {
			String bndContent = FileUtil.read(bndFile);

			Matcher matcher = _exportsPattern.matcher(bndContent);

			if (matcher.find()) {
				String packages = StringUtil.removeChars(
					matcher.group(2), CharPool.BACK_SLASH, CharPool.COMMA,
					CharPool.TAB);

				exportPackages = StringUtil.splitLines(packages);
			}
		}
		catch (IOException ioe) {
		}

		_exportPackagesMap.put(modulePath, exportPackages);

		return exportPackages;
	}

	private String _getPackageName(DetailAST packageDefAST) {
		DetailAST dotAST = packageDefAST.findFirstToken(TokenTypes.DOT);

		FullIdent fullIdent2 = FullIdent.createFullIdent(dotAST);

		return fullIdent2.getText();
	}

	private final Map<String, String[]> _exportPackagesMap = new HashMap<>();
	private final Pattern _exportsPattern = Pattern.compile(
		"\nExport-Package:(\\\\\n| )(.*?\n|\\Z)[^\t]",
		Pattern.DOTALL | Pattern.MULTILINE);
	private final Pattern _moduleSrcPattern = Pattern.compile(
		"(.*/modules/.*/)src/");

}