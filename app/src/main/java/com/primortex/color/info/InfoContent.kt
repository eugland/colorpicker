package com.primortex.color.info

import com.primortex.color.screens.InfoDetailSection

object InfoContent {
    val copyrightSections = listOf(
        InfoDetailSection(
            heading = "Ownership",
            paragraphs = listOf(
                "© 2024 Color Picker by Primortex. All rights reserved. The Color Picker name, logo, and application assets are proprietary and may not be reused without permission."
            )
        ),
        InfoDetailSection(
            heading = "Third-party components",
            paragraphs = listOf(
                "Color Picker is built on open-source technologies. We acknowledge and respect the licenses of each library we rely on."
            ),
            bullets = listOf(
                "Jetpack Compose Material 3 for UI components",
                "CameraX for camera integration",
                "Coil for image loading",
                "Ktor for lightweight networking",
                "Navigation Compose and Accompanist Navigation Animation for in-app navigation"
            )
        ),
        InfoDetailSection(
            heading = "License notes",
            paragraphs = listOf(
                "All third-party libraries remain the property of their respective owners. Their original licenses are preserved and credited here so you can review them in detail."
            )
        )
    )

    val privacySections = listOf(
        InfoDetailSection(
            heading = "On-device processing",
            paragraphs = listOf(
                "Color sampling happens locally. Camera previews and picked photos are only analyzed to extract color values and are not transmitted to servers."
            )
        ),
        InfoDetailSection(
            heading = "Network usage",
            paragraphs = listOf(
                "Network requests are limited to fetching optional supporting data such as palette names. No personal images or camera frames are uploaded."
            ),
            bullets = listOf(
                "If you prefer a fully offline experience, you can revoke network access at the system level.",
                "You may revoke camera and photo permissions anytime from your device settings."
            )
        ),
        InfoDetailSection(
            heading = "Data retention",
            paragraphs = listOf(
                "Saved colors and palettes stay on your device until you delete them. Clearing the app data or uninstalling removes your saved items."
            )
        )
    )

    val usageSections = listOf(
        InfoDetailSection(
            heading = "Quick index",
            paragraphs = listOf("Jump to the task you need."),
            bullets = listOf(
                "Pick a color from Live Camera",
                "Pick a color from a photo",
                "Save a swatch to your palette",
                "Compare and copy color values",
                "Organize palettes"
            )
        ),
        InfoDetailSection(
            heading = "Pick a color from Live Camera",
            paragraphs = listOf(
                "Open Live Camera from the Explore or Camera tab. Aim the crosshair at the target, pinch to zoom for precision, then tap the swatch preview to lock it in."
            ),
            bullets = listOf(
                "Adjust crosshair size and shape in Explore > Settings for better accuracy.",
                "Use a steady grip or tripod for low-light scenes."
            )
        ),
        InfoDetailSection(
            heading = "Pick a color from a photo",
            paragraphs = listOf(
                "Choose Photo Pick, select an image from your gallery, then tap anywhere on the photo to sample the color at that point."
            ),
            bullets = listOf(
                "Zoom into the photo before tapping to isolate small details.",
                "Use back to return without saving if you just want to inspect a color."
            )
        ),
        InfoDetailSection(
            heading = "Save and reuse swatches",
            paragraphs = listOf(
                "After capturing a color, tap Save to store it in your palette. Each saved swatch records HEX, RGB, and HSL values for copying."
            ),
            bullets = listOf(
                "Tap any saved swatch to copy its HEX code or open details for more formats.",
                "Long-press or use edit actions (where available) to remove colors you no longer need."
            )
        ),
        InfoDetailSection(
            heading = "Organize palettes",
            paragraphs = listOf(
                "Open the Palette tab to group related swatches. Rearrange or remove items to keep your palette focused on the project at hand."
            )
        )
    )
}

