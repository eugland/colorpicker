package com.primortex.color.info

import androidx.annotation.StringRes
import com.primortex.color.R
import com.primortex.color.i18n.AppStrings
import com.primortex.color.features.info.InfoDetailSection

object InfoContent {
    fun copyrightSections(): List<InfoDetailSection> = COPYRIGHT_SECTIONS.resolve()

    fun privacySections(): List<InfoDetailSection> = PRIVACY_SECTIONS.resolve()

    fun termsSections(): List<InfoDetailSection> = TERMS_SECTIONS.resolve()

    fun usageSections(): List<InfoDetailSection> = USAGE_SECTIONS.resolve()

    private val COPYRIGHT_SECTIONS = listOf(
        InfoSectionRes(
            headingRes = R.string.info_copyright_ownership_heading,
            paragraphRes = listOf(R.string.info_copyright_ownership_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_copyright_third_party_heading,
            paragraphRes = listOf(R.string.info_copyright_third_party_p1),
            bulletRes = listOf(
                R.string.info_copyright_third_party_b1,
                R.string.info_copyright_third_party_b2,
                R.string.info_copyright_third_party_b3,
                R.string.info_copyright_third_party_b4,
                R.string.info_copyright_third_party_b5
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_copyright_license_notes_heading,
            paragraphRes = listOf(R.string.info_copyright_license_notes_p1)
        )
    )

    private val PRIVACY_SECTIONS = listOf(
        InfoSectionRes(
            headingRes = R.string.info_privacy_on_device_heading,
            paragraphRes = listOf(R.string.info_privacy_on_device_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_privacy_permissions_heading,
            paragraphRes = listOf(R.string.info_privacy_permissions_p1),
            bulletRes = listOf(
                R.string.info_privacy_permissions_b1,
                R.string.info_privacy_permissions_b2
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_privacy_network_heading,
            paragraphRes = listOf(R.string.info_privacy_network_p1),
            bulletRes = listOf(R.string.info_privacy_network_b1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_privacy_retention_heading,
            paragraphRes = listOf(R.string.info_privacy_retention_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_privacy_contact_heading,
            paragraphRes = listOf(R.string.info_privacy_contact_p1)
        )
    )

    private val TERMS_SECTIONS = listOf(
        InfoSectionRes(
            headingRes = R.string.info_terms_acceptance_heading,
            paragraphRes = listOf(R.string.info_terms_acceptance_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_terms_usage_heading,
            paragraphRes = listOf(R.string.info_terms_usage_p1),
            bulletRes = listOf(
                R.string.info_terms_usage_b1,
                R.string.info_terms_usage_b2
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_terms_ip_heading,
            paragraphRes = listOf(R.string.info_terms_ip_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_terms_availability_heading,
            paragraphRes = listOf(R.string.info_terms_availability_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_terms_liability_heading,
            paragraphRes = listOf(R.string.info_terms_liability_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_terms_contact_heading,
            paragraphRes = listOf(R.string.info_terms_contact_p1)
        )
    )

    private val USAGE_SECTIONS = listOf(
        InfoSectionRes(
            headingRes = R.string.info_usage_quick_index_heading,
            paragraphRes = listOf(R.string.info_usage_quick_index_p1),
            bulletRes = listOf(
                R.string.info_usage_quick_index_b1,
                R.string.info_usage_quick_index_b2,
                R.string.info_usage_quick_index_b3,
                R.string.info_usage_quick_index_b4,
                R.string.info_usage_quick_index_b5,
                R.string.info_usage_quick_index_b6
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_usage_live_camera_heading,
            paragraphRes = listOf(R.string.info_usage_live_camera_p1),
            bulletRes = listOf(
                R.string.info_usage_live_camera_b1,
                R.string.info_usage_live_camera_b2
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_usage_photo_heading,
            paragraphRes = listOf(R.string.info_usage_photo_p1),
            bulletRes = listOf(
                R.string.info_usage_photo_b1,
                R.string.info_usage_photo_b2
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_usage_save_reuse_heading,
            paragraphRes = listOf(R.string.info_usage_save_reuse_p1),
            bulletRes = listOf(
                R.string.info_usage_save_reuse_b1,
                R.string.info_usage_save_reuse_b2
            )
        ),
        InfoSectionRes(
            headingRes = R.string.info_usage_compare_share_heading,
            paragraphRes = listOf(R.string.info_usage_compare_share_p1)
        ),
        InfoSectionRes(
            headingRes = R.string.info_usage_organize_heading,
            paragraphRes = listOf(R.string.info_usage_organize_p1)
        )
    )

    private data class InfoSectionRes(
        @StringRes val headingRes: Int,
        @StringRes val paragraphRes: List<Int>,
        @StringRes val bulletRes: List<Int> = emptyList()
    )

    private fun List<InfoSectionRes>.resolve(): List<InfoDetailSection> = map { section ->
        InfoDetailSection(
            heading = AppStrings.get(section.headingRes),
            paragraphs = section.paragraphRes.map { AppStrings.get(it) },
            bullets = section.bulletRes.map { AppStrings.get(it) }
        )
    }
}

