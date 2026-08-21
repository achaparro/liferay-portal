/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {
	SegmentExperience,
	hideProductMenuIfPresent,
	openConfirmModal,
	useMediaQuery,
} from '@liferay/layout-js-components-web';
import {openToast} from 'frontend-js-components-web';
import {sub} from 'frontend-js-web';
import React, {useEffect, useMemo, useState} from 'react';

import {Config, initializeConfig} from '../config';
import PageVersionService from '../services/PageVersionService';
import {PageVersion} from '../types/PageVersion';
import PagePreview from './PagePreview';
import ResponsivePanel from './ResponsivePanel';
import Toolbar from './Toolbar';
import VersionList from './VersionList';

import '../../css/VersionHistory.scss';

const CURRENT_KEY = 'current';

const LARGE_MEDIA_QUERY = '(min-width: 992px)';

interface Props {
	config: Config;
}

export default function VersionHistory({config}: Props) {
	initializeConfig(config);

	const [isPanelOpen, setIsPanelOpen] = useState(false);
	const [search, setSearch] = useState('');
	const [selectedKey, setSelectedKey] = useState<string>();

	const [versions, setVersions] = useState<PageVersion[] | null>(null);

	const [selectedExperienceERC, setSelectedExperienceERC] = useState<string>(
		config.availableSegmentsExperiences[0]?.segmentsExperienceERC ?? ''
	);
	const [selectedLocaleId, setSelectedLocaleId] = useState<string>(
		config.defaultLanguageId
	);

	const isScreenLarge = useMediaQuery(LARGE_MEDIA_QUERY);

	useEffect(() => {
		hideProductMenuIfPresent({onHide: () => setIsPanelOpen(true)});
	}, []);

	useEffect(() => {
		const controller = new AbortController();

		const loadVersions = async () => {
			const {data, error} = await PageVersionService.getPageVersions(
				controller.signal
			);

			if (controller.signal.aborted) {
				return;
			}

			if (error) {
				openToast({message: error, type: 'danger'});
			}

			setVersions(data?.items ?? []);
		};

		loadVersions();

		return () => controller.abort();
	}, []);

	const handleDelete = async (version: PageVersion) => {
		if (!version.actions?.delete) {
			return;
		}

		const confirmed = await openConfirmModal({
			buttonLabel: Liferay.Language.get('delete'),
			center: true,
			status: 'danger',
			text: Liferay.Language.get('delete-page-version-confirmation'),
			title: Liferay.Language.get('delete-version'),
		});

		if (!confirmed) {
			return;
		}

		const {error} = await PageVersionService.deletePageVersion(
			version.actions.delete.href
		);

		if (error) {
			openToast({message: error, type: 'danger'});

			return;
		}

		openToast({
			message: sub(Liferay.Language.get('x-was-deleted-successfully'), [
				version.name,
			]),
			type: 'success',
		});

		const nextVersions = versions?.filter(
			({externalReferenceCode}) =>
				externalReferenceCode !== version.externalReferenceCode
		);

		setVersions(nextVersions ?? null);

		if (selectedKey === version.externalReferenceCode) {
			setSelectedKey(CURRENT_KEY);
		}
	};

	const handleRestore = async (version: PageVersion) => {
		if (!version.actions?.restore) {
			return;
		}

		if (config.layout.status === 'draft') {
			const confirmed = await openConfirmModal({
				buttonLabel: Liferay.Language.get('restore'),
				center: true,
				status: 'warning',
				text: Liferay.Language.get(
					'you-are-about-to-restore-an-older-version-of-the-page.-all-your-unsaved-changes-will-be-lost'
				),
				title: Liferay.Language.get('restore-version'),
			});

			if (!confirmed) {
				return;
			}
		}

		const {error} = await PageVersionService.restorePageVersion(
			version.actions.restore.href
		);

		if (error) {
			openToast({message: error, type: 'danger'});

			return;
		}

		window.location.reload();
	};

	const handleSelect = (key: string) => {
		setSelectedKey(key);

		const version = versions?.find(
			({externalReferenceCode}) => externalReferenceCode === key
		);

		const previews = version?.pageSpecificationVersionPreviews ?? [];

		const preview =
			previews.find(
				({pageExperienceExternalReferenceCode}) =>
					pageExperienceExternalReferenceCode ===
					selectedExperienceERC
			) ?? previews[0];

		if (preview) {
			setSelectedExperienceERC(
				preview.pageExperienceExternalReferenceCode
			);
		}
	};

	const keywords = search.trim().toLowerCase();

	const matches = (...names: Array<string | undefined>) =>
		names.some((name) => name?.toLowerCase().includes(keywords));

	const items = versions && [
		...(matches(config.layout.name)
			? [{key: CURRENT_KEY, ...config.layout}]
			: []),
		...versions
			.filter(({creator, name, version}) =>
				matches(name, creator?.name, String(version))
			)
			.map((version) => ({
				key: version.externalReferenceCode,
				name: version.name,
				status: version.status,
				version,
			})),
	];

	const selectedVersion =
		selectedKey && selectedKey !== CURRENT_KEY
			? versions?.find(
					({externalReferenceCode}) =>
						externalReferenceCode === selectedKey
				)
			: undefined;

	const segmentsExperiences = useMemo<SegmentExperience[]>(() => {
		const currentLanguageId = Liferay.ThemeDisplay.getLanguageId();

		const scopedSegmentsExperiences: SegmentExperience[] = selectedVersion
			? (selectedVersion.pageSpecificationVersionPreviews ?? []).map(
					(preview) => {
						const name_i18n = preview.pageExperienceName_i18n ?? {};

						return {
							active: false,
							priority: preview.pageExperiencePriority,
							segmentsExperienceERC:
								preview.pageExperienceExternalReferenceCode,
							segmentsExperienceName:
								name_i18n[currentLanguageId] ??
								Object.values(name_i18n)[0] ??
								preview.pageExperienceExternalReferenceCode,
							statusLabel: '',
						};
					}
				)
			: [...config.availableSegmentsExperiences];

		return scopedSegmentsExperiences
			.map((segmentsExperience) => {
				const priority = segmentsExperience.priority ?? 0;

				let statusLabel = Liferay.Language.get('default');

				if (priority > 0) {
					statusLabel = Liferay.Language.get('active');
				}
				else if (priority < 0) {
					statusLabel = Liferay.Language.get('inactive');
				}

				return {
					...segmentsExperience,
					active: priority >= 0,
					statusLabel,
				};
			})
			.sort((a, b) => (b.priority ?? 0) - (a.priority ?? 0));
	}, [config.availableSegmentsExperiences, selectedVersion]);

	const selectedExperience = segmentsExperiences.find(
		({segmentsExperienceERC}) =>
			segmentsExperienceERC === selectedExperienceERC
	);

	return (
		<>
			<Toolbar
				isSidePanelOpen={isPanelOpen || isScreenLarge}
				onChangeExperience={setSelectedExperienceERC}
				onChangeLocale={setSelectedLocaleId}
				openSidePanel={() => setIsPanelOpen(true)}
				segmentsExperiences={segmentsExperiences}
				selectedExperienceERC={selectedExperienceERC}
				selectedLocaleId={selectedLocaleId}
			/>

			<ResponsivePanel
				onOpenChange={setIsPanelOpen}
				onSearch={setSearch}
				open={isPanelOpen || isScreenLarge}
			>
				{items ? (
					<VersionList
						items={items}
						onDelete={handleDelete}
						onRestore={handleRestore}
						onSelect={handleSelect}
						searching={Boolean(keywords)}
						selectedKey={selectedKey}
					/>
				) : (
					<ClayLoadingIndicator
						displayType="secondary"
						size="sm"
						title={Liferay.Language.get('loading')}
					/>
				)}
			</ResponsivePanel>

			<PagePreview
				languageId={selectedLocaleId}
				segmentsExperienceERC={selectedExperienceERC}
				segmentsExperienceId={selectedExperience?.segmentsExperienceId}
				version={selectedVersion}
			/>
		</>
	);
}
