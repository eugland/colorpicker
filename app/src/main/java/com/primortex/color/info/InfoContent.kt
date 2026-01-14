package com.primortex.color.info

import com.primortex.color.screens.InfoDetailSection

object InfoContent {
    val copyrightSections = listOf(
        InfoDetailSection(
            heading = "Ownership",
            paragraphs = listOf(
                "© 2025 Primortex. All rights reserved. Color Picker, including its name, logo, UI, and application assets, is owned by Primortex and may not be reused without written permission."
            )
        ),
        InfoDetailSection(
            heading = "Third-party components",
            paragraphs = listOf(
                "Color Picker is built with open-source technologies. We acknowledge and comply with the licenses of each library we rely on."
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
                "All third-party libraries remain the property of their respective owners. Their original licenses are preserved and credited so you can review them in detail."
            )
        )
    )

    val privacySections = listOf(
        InfoDetailSection(
            heading = "On-device processing",
            paragraphs = listOf(
                "Color sampling happens locally. Camera previews and picked photos are only analyzed to extract color values and are not transmitted to our servers."
            )
        ),
        InfoDetailSection(
            heading = "Permissions and data",
            paragraphs = listOf(
                "The app requests access only to features needed for color picking, such as the camera and photo library. You can revoke these permissions at any time in your device settings."
            ),
            bullets = listOf(
                "Camera access is used for live sampling and is never recorded or stored by us.",
                "Photo access is used only when you select an image to sample."
            )
        ),
        InfoDetailSection(
            heading = "Network usage",
            paragraphs = listOf(
                "Network requests are limited to fetching optional supporting data such as palette names or usage guides. No personal images or camera frames are uploaded."
            ),
            bullets = listOf(
                "If you prefer a fully offline experience, you can revoke network access at the system level."
            )
        ),
        InfoDetailSection(
            heading = "Data retention",
            paragraphs = listOf(
                "Saved colors and palettes stay on your device until you delete them. Clearing app data or uninstalling removes your saved items."
            )
        ),
        InfoDetailSection(
            heading = "Contact and feedback",
            paragraphs = listOf(
                "If you choose to contact support, we will only use the information you provide to respond to your request."
            )
        )
    )

    val termsSections = listOf(
        InfoDetailSection(
            heading = "Acceptance of terms",
            paragraphs = listOf(
                "By using Color Picker, you agree to these terms of service. If you do not agree, please discontinue use of the app."
            )
        ),
        InfoDetailSection(
            heading = "App usage",
            paragraphs = listOf(
                "Color Picker is provided for personal and professional color reference. You are responsible for how you use the app and any results from your color selections."
            ),
            bullets = listOf(
                "Do not use the app in ways that violate applicable laws or regulations.",
                "Do not attempt to reverse engineer, tamper with, or misuse the app’s services."
            )
        ),
        InfoDetailSection(
            heading = "Intellectual property",
            paragraphs = listOf(
                "The app and all related content are owned by Primortex or its licensors. You may not copy, modify, or redistribute any part of the app except as allowed by law."
            )
        ),
        InfoDetailSection(
            heading = "Content and availability",
            paragraphs = listOf(
                "We may update or discontinue features at any time. Optional online content is provided as-is and may change without notice."
            )
        ),
        InfoDetailSection(
            heading = "Limitation of liability",
            paragraphs = listOf(
                "Color Picker is provided “as is” without warranties of any kind. To the extent permitted by law, we are not liable for damages arising from use of the app."
            )
        ),
        InfoDetailSection(
            heading = "Contact",
            paragraphs = listOf(
                "If you have questions about these terms, please reach out through the feedback form in the app."
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
                "Organize palettes",
                "Share or export color values"
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
            heading = "Compare and share values",
            paragraphs = listOf(
                "Open a saved swatch to compare formats side by side. Use the copy or share actions to send values to your design or development tools."
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
