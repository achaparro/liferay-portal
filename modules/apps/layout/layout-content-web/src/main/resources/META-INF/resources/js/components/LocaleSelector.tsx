/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {LanguagePicker, Provider} from '@clayui/core';
import React from 'react';

import {config} from '../config';

interface Props {
	onChangeLocale: (localeId: string) => void;
	selectedLocaleId: string;
}

export default function LocaleSelector({
	onChangeLocale,
	selectedLocaleId,
}: Props) {
	const {availableLanguages, defaultLanguageId} = config;

	const locales = Object.entries(availableLanguages).map(
		([id, language]) => ({
			id,
			label: language?.w3cLanguageId ?? id,
			symbol: language?.languageIcon ?? '',
		})
	);

	if (!locales.length) {
		return null;
	}

	const spritemap = `${Liferay.ThemeDisplay.getPathThemeImages()}/clay/icons.svg`;

	return (
		<Provider spritemap={spritemap}>
			<LanguagePicker
				defaultLocaleId={defaultLanguageId}
				hideTriggerText
				locales={locales}
				onSelectedLocaleChange={(key) => onChangeLocale(String(key))}
				selectedLocaleId={selectedLocaleId}
				small
				spritemap={spritemap}
			/>
		</Provider>
	);
}
