/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import React from 'react';

import {config} from '../config';
import {PageVersion} from '../types/PageVersion';

interface Props {
	languageId: string;
	segmentsExperienceERC: string;
	segmentsExperienceId?: string;
	version?: PageVersion;
}

export default function PagePreview({
	languageId,
	segmentsExperienceERC,
	segmentsExperienceId,
	version,
}: Props) {
	const url = version
		? buildQuery(config.getPageVersionPreviewURL, {
				externalReferenceCode: version.externalReferenceCode,
				groupId: String(Liferay.ThemeDisplay.getScopeGroupId()),
				languageId,
				segmentsExperienceERC,
			})
		: buildQuery(config.getPagePreviewURL, {
				languageId,
				segmentsExperienceId: segmentsExperienceId ?? '',
				selPlid: String(Liferay.ThemeDisplay.getPlid()),
			});

	return (
		<iframe
			className="version-history__preview"
			src={url}
			title={Liferay.Language.get('preview')}
		/>
	);
}

function buildQuery(url: string, params: Record<string, string>) {
	const search = new URLSearchParams(params);

	return `${url}?${search.toString()}`;
}
