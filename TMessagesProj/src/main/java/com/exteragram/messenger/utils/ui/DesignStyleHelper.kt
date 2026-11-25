/*
 * SuperGram UI Design Systems (Material You, MIUIX, Windows 11, iOS)
 * Based on exteraGram (https://github.com/exteraSquad/exteraGram), GNU GPL v3+
 */

package com.exteragram.messenger.utils.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import androidx.core.graphics.ColorUtils
import com.exteragram.messenger.DesignStyle
import com.exteragram.messenger.centerTitle
import com.exteragram.messenger.designStyle
import com.exteragram.messenger.getSectionRadiusDp
import com.exteragram.messenger.icons.IconManager
import com.exteragram.messenger.uiCardBorder
import com.exteragram.messenger.uiCardPaddingDp
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.AndroidUtilities.dpf2
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.LaunchActivity
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object DesignStyleHelper {

    data class PluginColorOverride(
        val lightColor: Int? = null,
        val darkColor: Int? = null
    )

    data class PluginBorderOverride(
        val lightColor: Int? = null,
        val darkColor: Int? = null,
        val widthDp: Float? = null
    )

    data class PluginIconOverride(
        val replacementResId: Int = 0,
        val filePath: String? = null
    )

    data class PluginPlacementOverride(
        val fabAlign: String? = null,
        val fabBottomMarginDp: Int? = null,
        val fabSideMarginDp: Int? = null,
        val actionBarHeightDp: Int? = null,
        val mainTabsOrder: IntArray? = null,
        val hiddenMainTabs: Set<Int>? = null
    )

    data class PluginFontOverride(
        val regularPath: String? = null,
        val boldPath: String? = null,
        val monoPath: String? = null
    )

    private val pluginOrder = CopyOnWriteArrayList<String>()
    private val pluginColorOverrides = ConcurrentHashMap<String, ConcurrentHashMap<Int, PluginColorOverride>>()
    private val pluginBorderOverrides = ConcurrentHashMap<String, PluginBorderOverride>()
    private val pluginIconOverrides = ConcurrentHashMap<String, ConcurrentHashMap<Int, PluginIconOverride>>()
    private val pluginPlacementOverrides = ConcurrentHashMap<String, PluginPlacementOverride>()
    private val pluginFontOverrides = ConcurrentHashMap<String, PluginFontOverride>()
    private val themeKeyNameCache = ConcurrentHashMap<String, Int>()
    private val drawableIdNameCache = ConcurrentHashMap<String, Int>()
    private val customTypefaceCache = ConcurrentHashMap<String, Typeface>()

    @JvmStatic
    fun resolveThemeKey(keyName: String): Int {
        val normalized = if (keyName.startsWith("key_")) keyName else "key_$keyName"
        themeKeyNameCache[normalized]?.let { return it }
        return try {
            val field = Theme::class.java.getField(normalized)
            val key = field.getInt(null)
            themeKeyNameCache[normalized] = key
            key
        } catch (_: Throwable) {
            -1
        }
    }

    @JvmStatic
    fun resolveDrawableId(name: String): Int {
        val normalized = if (name.startsWith("R.drawable.")) name.removePrefix("R.drawable.") else name
        drawableIdNameCache[normalized]?.let { return it }
        return try {
            val field = R.drawable::class.java.getField(normalized)
            val id = field.getInt(null)
            drawableIdNameCache[normalized] = id
            id
        } catch (_: Throwable) {
            0
        }
    }

    private fun touchPluginOrder(pluginId: String) {
        pluginOrder.remove(pluginId)
        pluginOrder.add(pluginId)
    }

    @JvmStatic
    fun setPluginColorOverride(pluginId: String, key: Int, lightColor: Int?, darkColor: Int?) {
        if (pluginId.isBlank() || key < 0) return
        touchPluginOrder(pluginId)
        val map = pluginColorOverrides.getOrPut(pluginId) { ConcurrentHashMap() }
        if (lightColor == null && darkColor == null) {
            map.remove(key)
        } else {
            map[key] = PluginColorOverride(lightColor, darkColor)
        }
    }

    @JvmStatic
    fun setPluginBorderOverride(pluginId: String, lightColor: Int?, darkColor: Int?, widthDp: Float?) {
        if (pluginId.isBlank()) return
        touchPluginOrder(pluginId)
        if (lightColor == null && darkColor == null && widthDp == null) {
            pluginBorderOverrides.remove(pluginId)
        } else {
            pluginBorderOverrides[pluginId] = PluginBorderOverride(lightColor, darkColor, widthDp)
        }
    }

    @JvmStatic
    fun setPluginIconReplacement(pluginId: String, targetResId: Int, replacementResId: Int) {
        if (pluginId.isBlank() || targetResId == 0) return
        touchPluginOrder(pluginId)
        val map = pluginIconOverrides.getOrPut(pluginId) { ConcurrentHashMap() }
        if (replacementResId == 0) {
            map.remove(targetResId)
        } else {
            map[targetResId] = PluginIconOverride(replacementResId = replacementResId, filePath = null)
        }
        IconManager.clearPluginCaches()
    }

    @JvmStatic
    fun setPluginIconFile(pluginId: String, targetResId: Int, filePath: String?) {
        if (pluginId.isBlank() || targetResId == 0) return
        touchPluginOrder(pluginId)
        val map = pluginIconOverrides.getOrPut(pluginId) { ConcurrentHashMap() }
        if (filePath.isNullOrBlank()) {
            map.remove(targetResId)
        } else {
            map[targetResId] = PluginIconOverride(replacementResId = 0, filePath = filePath)
        }
        IconManager.clearPluginCaches()
    }

    @JvmStatic
    fun clearPluginIconOverrides(pluginId: String?): Boolean {
        if (pluginId.isNullOrBlank()) return false
        val removed = pluginIconOverrides.remove(pluginId)?.isNotEmpty() == true
        if (removed) IconManager.clearPluginCaches()
        return removed
    }

    @JvmStatic
    fun getActiveIconReplacement(resId: Int): Int {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val entry = pluginIconOverrides[id]?.get(resId) ?: continue
            if (entry.replacementResId != 0) return entry.replacementResId
        }
        return 0
    }

    @JvmStatic
    fun getActiveIconFile(resId: Int): String? {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val entry = pluginIconOverrides[id]?.get(resId) ?: continue
            if (!entry.filePath.isNullOrBlank()) return entry.filePath
        }
        return null
    }

    @JvmStatic
    fun setPluginFabLayout(pluginId: String, align: String?, bottomMarginDp: Int?, sideMarginDp: Int?) {
        if (pluginId.isBlank()) return
        touchPluginOrder(pluginId)
        val prev = pluginPlacementOverrides[pluginId] ?: PluginPlacementOverride()
        pluginPlacementOverrides[pluginId] = prev.copy(
            fabAlign = align?.trim()?.lowercase() ?: prev.fabAlign,
            fabBottomMarginDp = bottomMarginDp ?: prev.fabBottomMarginDp,
            fabSideMarginDp = sideMarginDp ?: prev.fabSideMarginDp
        )
    }

    @JvmStatic
    fun setPluginActionBarHeight(pluginId: String, heightDp: Int?) {
        if (pluginId.isBlank()) return
        touchPluginOrder(pluginId)
        val prev = pluginPlacementOverrides[pluginId] ?: PluginPlacementOverride()
        pluginPlacementOverrides[pluginId] = prev.copy(
            actionBarHeightDp = if (heightDp != null && heightDp <= 0) null else heightDp?.coerceIn(40, 84)
        )
    }

    @JvmStatic
    fun setPluginTabsLayout(pluginId: String, order: IntArray?, hidden: Set<Int>?) {
        if (pluginId.isBlank()) return
        touchPluginOrder(pluginId)
        val prev = pluginPlacementOverrides[pluginId] ?: PluginPlacementOverride()
        pluginPlacementOverrides[pluginId] = prev.copy(
            mainTabsOrder = order ?: prev.mainTabsOrder,
            hiddenMainTabs = hidden ?: prev.hiddenMainTabs
        )
    }

    @JvmStatic
    fun getEffectiveFabGravity(isRtl: Boolean): Int {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val align = pluginPlacementOverrides[id]?.fabAlign ?: continue
            return when (align) {
                "left" -> Gravity.LEFT or Gravity.BOTTOM
                "center" -> Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                "right" -> Gravity.RIGHT or Gravity.BOTTOM
                else -> (if (isRtl) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM
            }
        }
        return (if (isRtl) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM
    }

    @JvmStatic
    fun getEffectiveFabSideMarginDp(defaultDp: Int): Int {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val side = pluginPlacementOverrides[id]?.fabSideMarginDp
            if (side != null && side >= 0) return side.coerceIn(0, 80)
        }
        return defaultDp
    }

    @JvmStatic
    fun getEffectiveFabBottomMarginDp(defaultDp: Int): Int {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val bottom = pluginPlacementOverrides[id]?.fabBottomMarginDp
            if (bottom != null && bottom >= 0) return bottom.coerceIn(0, 180)
        }
        return defaultDp
    }

    @JvmStatic
    fun getActionBarHeightOverrideDp(): Int {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val h = pluginPlacementOverrides[id]?.actionBarHeightDp
            if (h != null && h > 0) return h
        }
        return -1
    }

    @JvmStatic
    fun getMainTabsOrderOverride(): IntArray? {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val order = pluginPlacementOverrides[id]?.mainTabsOrder
            if (order != null && order.isNotEmpty()) return order
        }
        return null
    }

    @JvmStatic
    fun isMainTabHidden(tabIndex: Int): Boolean {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val hidden = pluginPlacementOverrides[id]?.hiddenMainTabs
            if (hidden != null) return hidden.contains(tabIndex)
        }
        return false
    }

    @JvmStatic
    fun setPluginFontOverride(pluginId: String, regularPath: String?, boldPath: String?, monoPath: String?) {
        if (pluginId.isBlank()) return
        touchPluginOrder(pluginId)
        if (regularPath.isNullOrBlank() && boldPath.isNullOrBlank() && monoPath.isNullOrBlank()) {
            pluginFontOverrides.remove(pluginId)
        } else {
            pluginFontOverrides[pluginId] = PluginFontOverride(
                regularPath?.takeIf { it.isNotBlank() },
                boldPath?.takeIf { it.isNotBlank() },
                monoPath?.takeIf { it.isNotBlank() }
            )
        }
        clearFontTypefaceCache()
        AndroidUtilities.clearTypefaceCache()
    }

    @JvmStatic
    fun clearFontTypefaceCache() {
        customTypefaceCache.clear()
    }

    @JvmStatic
    fun getFontOverride(assetPath: String): Typeface? {
        if (pluginFontOverrides.isEmpty()) return null
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val entry = pluginFontOverrides[id] ?: continue
            val chosenPath = when (assetPath) {
                AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM,
                AndroidUtilities.TYPEFACE_ROBOTO_EXTRA_BOLD,
                AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM_ITALIC,
                AndroidUtilities.TYPEFACE_MERRIWEATHER_BOLD,
                AndroidUtilities.TYPEFACE_NUNITO_EXTRABOLD -> entry.boldPath ?: entry.regularPath
                AndroidUtilities.TYPEFACE_ROBOTO_MONO -> entry.monoPath ?: entry.regularPath
                else -> entry.regularPath
            } ?: continue
            return customTypefaceCache.getOrPut(chosenPath) {
                try {
                    val file = File(chosenPath)
                    if (file.isFile) Typeface.createFromFile(file) else return null
                } catch (t: Throwable) {
                    FileLog.e("Failed to load plugin font $chosenPath", t)
                    return null
                }
            }
        }
        return null
    }

    @JvmStatic
    fun clearPluginOverrides(pluginId: String?): Boolean {
        if (pluginId.isNullOrBlank()) return false
        val hadColors = pluginColorOverrides.remove(pluginId)?.isNotEmpty() == true
        val hadBorder = pluginBorderOverrides.remove(pluginId) != null
        val hadIcons = pluginIconOverrides.remove(pluginId)?.isNotEmpty() == true
        val hadPlacement = pluginPlacementOverrides.remove(pluginId) != null
        val hadFonts = pluginFontOverrides.remove(pluginId) != null
        pluginOrder.remove(pluginId)
        if (hadIcons) IconManager.clearPluginCaches()
        if (hadFonts) {
            clearFontTypefaceCache()
            AndroidUtilities.clearTypefaceCache()
        }
        return hadColors || hadBorder || hadIcons || hadPlacement || hadFonts
    }

    private fun activeBorderOverride(): PluginBorderOverride? {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            pluginBorderOverrides[id]?.let { return it }
        }
        return null
    }

    private fun activeColorOverride(key: Int, isDark: Boolean): Int? {
        for (i in pluginOrder.size - 1 downTo 0) {
            val id = pluginOrder.getOrNull(i) ?: continue
            val entry = pluginColorOverrides[id]?.get(key) ?: continue
            val resolved = if (isDark) (entry.darkColor ?: entry.lightColor) else (entry.lightColor ?: entry.darkColor)
            if (resolved != null) return resolved
        }
        return null
    }

    @JvmStatic
    @JvmOverloads
    fun triggerLiveUiRefresh(rebuildAll: Boolean = false) {
        AndroidUtilities.runOnUIThread {
            LaunchActivity.instance?.let { activity ->
                Theme.reloadAllResources(activity)
            }
            Theme.applyCommonTheme()
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true, true)
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.iconPackUpdated)
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged)
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface)
            LaunchActivity.instance?.let { activity ->
                if (rebuildAll) {
                    activity.rebuildAllFragments(false)
                } else {
                    activity.actionBarLayout?.let { layout ->
                        layout.lastFragment?.fragmentView?.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
                        layout.lastFragment?.actionBar?.refreshTitlePosition(true)
                        layout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun getCurrentStyle(): DesignStyle = designStyle

    @JvmStatic
    fun getStyleDisplayName(style: DesignStyle = designStyle): String = when (style) {
        DesignStyle.MATERIAL_YOU -> getString(R.string.DesignStyleMaterialYou)
        DesignStyle.MIUIX -> getString(R.string.DesignStyleMiuiX)
        DesignStyle.WINDOWS_11 -> getString(R.string.DesignStyleWindows11)
        DesignStyle.IOS -> getString(R.string.DesignStyleIos)
    }

    @JvmStatic
    fun getEffectiveSectionPaddingDp(): Int {
        val custom = uiCardPaddingDp
        if (custom >= 0) return custom.coerceIn(0, 24)
        return when (designStyle) {
            DesignStyle.MATERIAL_YOU -> 12
            DesignStyle.MIUIX -> 14
            DesignStyle.WINDOWS_11 -> 12
            DesignStyle.IOS -> 16
        }
    }

    @JvmStatic
    fun getEffectiveSectionRadiusDp(): Int = getSectionRadiusDp()

    @JvmStatic
    fun shouldDrawCardBorder(): Boolean {
        val borderOverride = activeBorderOverride()
        if (borderOverride != null) {
            return (borderOverride.widthDp ?: 1f) > 0f &&
                (borderOverride.lightColor != null || borderOverride.darkColor != null)
        }
        return uiCardBorder || designStyle == DesignStyle.WINDOWS_11 || designStyle == DesignStyle.MIUIX
    }

    @JvmStatic
    fun getCardBorderWidthPx(): Float {
        activeBorderOverride()?.widthDp?.let { w ->
            return dpf2(w.coerceIn(0.2f, 4.0f))
        }
        return when (designStyle) {
            DesignStyle.WINDOWS_11 -> dpf2(1.0f)
            DesignStyle.MIUIX -> dpf2(0.6f)
            else -> dpf2(0.8f)
        }
    }

    @JvmStatic
    fun getCardBorderColor(resourcesProvider: Theme.ResourcesProvider?, alpha: Float): Int {
        val bg = Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider)
        val isDark = AndroidUtilitiesIsDark(bg)
        val borderOverride = activeBorderOverride()
        if (borderOverride != null) {
            val overridden = if (isDark) (borderOverride.darkColor ?: borderOverride.lightColor) else (borderOverride.lightColor ?: borderOverride.darkColor)
            if (overridden != null) {
                return Theme.multAlpha(overridden, alpha)
            }
        }
        val base = when (designStyle) {
            DesignStyle.WINDOWS_11 -> if (isDark) 0x33FFFFFF else 0x24000000
            DesignStyle.MIUIX -> if (isDark) 0x1EFFFFFF else 0x12000000
            else -> if (isDark) 0x22FFFFFF else 0x18000000
        }
        return Theme.multAlpha(base, alpha)
    }

    private fun AndroidUtilitiesIsDark(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.45
    }

    @JvmStatic
    fun shouldCenterActionBarTitle(): Boolean =
        centerTitle || designStyle == DesignStyle.IOS

    @JvmStatic
    fun transformThemeColor(key: Int, color: Int, isDark: Boolean): Int {
        val alpha = Color.alpha(color)
        if (alpha == 0) return color

        val pluginReplaced = activeColorOverride(key, isDark)
        if (pluginReplaced != null) {
            return if (alpha < 255 && Color.alpha(pluginReplaced) == 255) {
                ColorUtils.setAlphaComponent(pluginReplaced, alpha)
            } else {
                pluginReplaced
            }
        }

        val style = designStyle
        if (style == DesignStyle.MATERIAL_YOU) return color

        val replaced: Int? = when (style) {
            DesignStyle.MIUIX -> when (key) {
                Theme.key_windowBackgroundGray,
                Theme.key_windowBackgroundGrayShadow -> if (isDark) 0xFF000000.toInt() else 0xFFF5F6F8.toInt()
                Theme.key_windowBackgroundWhite,
                Theme.key_dialogBackground -> if (isDark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
                Theme.key_windowBackgroundWhiteBlueHeader,
                Theme.key_windowBackgroundWhiteBlueText,
                Theme.key_windowBackgroundWhiteBlueText4,
                Theme.key_windowBackgroundWhiteBlueButton,
                Theme.key_switchTrackChecked,
                Theme.key_featuredStickers_addButton,
                Theme.key_chats_actionBackground,
                Theme.key_checkboxCheck,
                Theme.key_dialogTextBlue2,
                Theme.key_radioBackgroundChecked -> if (isDark) 0xFF2997FF.toInt() else 0xFF0D84FF.toInt()
                else -> null
            }
            DesignStyle.WINDOWS_11 -> when (key) {
                Theme.key_windowBackgroundGray,
                Theme.key_windowBackgroundGrayShadow -> if (isDark) 0xFF202020.toInt() else 0xFFF3F3F3.toInt()
                Theme.key_windowBackgroundWhite,
                Theme.key_dialogBackground -> if (isDark) 0xFF2C2C2C.toInt() else 0xFFFFFFFF.toInt()
                Theme.key_windowBackgroundWhiteBlueHeader,
                Theme.key_windowBackgroundWhiteBlueText,
                Theme.key_windowBackgroundWhiteBlueText4,
                Theme.key_windowBackgroundWhiteBlueButton,
                Theme.key_switchTrackChecked,
                Theme.key_featuredStickers_addButton,
                Theme.key_chats_actionBackground,
                Theme.key_checkboxCheck,
                Theme.key_dialogTextBlue2,
                Theme.key_radioBackgroundChecked -> if (isDark) 0xFF60CDFF.toInt() else 0xFF0067C0.toInt()
                Theme.key_divider -> if (isDark) 0x26FFFFFF else 0x1A000000
                else -> null
            }
            DesignStyle.IOS -> when (key) {
                Theme.key_windowBackgroundGray,
                Theme.key_windowBackgroundGrayShadow -> if (isDark) 0xFF000000.toInt() else 0xFFF2F2F7.toInt()
                Theme.key_windowBackgroundWhite,
                Theme.key_dialogBackground -> if (isDark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
                Theme.key_windowBackgroundWhiteBlueHeader,
                Theme.key_windowBackgroundWhiteBlueText,
                Theme.key_windowBackgroundWhiteBlueText4,
                Theme.key_windowBackgroundWhiteBlueButton,
                Theme.key_switchTrackChecked,
                Theme.key_featuredStickers_addButton,
                Theme.key_chats_actionBackground,
                Theme.key_checkboxCheck,
                Theme.key_dialogTextBlue2,
                Theme.key_radioBackgroundChecked -> if (isDark) 0xFF0A84FF.toInt() else 0xFF007AFF.toInt()
                Theme.key_divider -> if (isDark) 0x24FFFFFF else 0x1F000000
                else -> null
            }
            DesignStyle.MATERIAL_YOU -> null
        }

        if (replaced == null) return color
        return if (alpha < 255 && Color.alpha(replaced) == 255) {
            ColorUtils.setAlphaComponent(replaced, alpha)
        } else {
            replaced
        }
    }

    @JvmStatic
    fun usesCustomStyledSwitch(): Boolean =
        designStyle != DesignStyle.MATERIAL_YOU

    @JvmStatic
    fun drawStyledSwitch(
        canvas: Canvas,
        width: Int,
        height: Int,
        progress: Float,
        trackColor: Int,
        trackCheckedColor: Int,
        thumbColor: Int,
        thumbCheckedColor: Int,
        overrideAlpha: Float,
        rectF: RectF,
        fillPaint: Paint,
        strokePaint: Paint
    ) {
        val p = Utilities.clamp01(progress)
        val cx = width / 2f
        val cy = height / 2f
        val isDark = AndroidUtilitiesIsDark(Theme.getColor(Theme.key_windowBackgroundWhite))

        when (designStyle) {
            DesignStyle.MIUIX -> {
                val trackW = dpf2(44f)
                val trackH = dpf2(24f)
                val thumbD = dpf2(18f)
                val pad = (trackH - thumbD) / 2f

                val offTrack = if (isDark) 0xFF3A3A3E.toInt() else 0xFFD9DCE2.toInt()
                val onTrack = trackCheckedColor
                val curTrack = ColorUtils.blendARGB(offTrack, onTrack, p)

                rectF.set(cx - trackW / 2f, cy - trackH / 2f, cx + trackW / 2f, cy + trackH / 2f)
                fillPaint.color = Theme.multAlpha(curTrack, overrideAlpha)
                canvas.drawRoundRect(rectF, trackH / 2f, trackH / 2f, fillPaint)

                val minX = rectF.left + pad + thumbD / 2f
                val maxX = rectF.right - pad - thumbD / 2f
                val thumbCx = minX + (maxX - minX) * p
                fillPaint.color = Theme.multAlpha(Color.WHITE, overrideAlpha)
                canvas.drawCircle(thumbCx, cy, thumbD / 2f, fillPaint)
            }

            DesignStyle.WINDOWS_11 -> {
                val trackW = dpf2(40f)
                val trackH = dpf2(20f)
                val thumbR = if (p > 0.5f) dpf2(6.5f) else dpf2(5.5f)
                val pad = dpf2(4f)

                rectF.set(cx - trackW / 2f, cy - trackH / 2f, cx + trackW / 2f, cy + trackH / 2f)
                val offTrack = Color.TRANSPARENT
                val curTrack = ColorUtils.blendARGB(offTrack, trackCheckedColor, p)
                if (Color.alpha(curTrack) > 0) {
                    fillPaint.color = Theme.multAlpha(curTrack, overrideAlpha)
                    canvas.drawRoundRect(rectF, trackH / 2f, trackH / 2f, fillPaint)
                }

                if (p < 1f) {
                    val strokeColor = if (isDark) 0xFFA1A1AA.toInt() else 0xFF606060.toInt()
                    strokePaint.color = Theme.multAlpha(strokeColor, (1f - p) * overrideAlpha)
                    strokePaint.strokeWidth = dpf2(1.2f)
                    val borderRect = RectF(rectF)
                    borderRect.inset(strokePaint.strokeWidth / 2f, strokePaint.strokeWidth / 2f)
                    canvas.drawRoundRect(borderRect, trackH / 2f, trackH / 2f, strokePaint)
                }

                val minX = rectF.left + pad + thumbR
                val maxX = rectF.right - pad - thumbR
                val thumbCx = minX + (maxX - minX) * p
                val offThumb = if (isDark) 0xFFD4D4D8.toInt() else 0xFF5A5A5A.toInt()
                val onThumb = if (AndroidUtilitiesIsDark(trackCheckedColor)) Color.WHITE else Color.BLACK
                fillPaint.color = Theme.multAlpha(ColorUtils.blendARGB(offThumb, onThumb, p), overrideAlpha)
                canvas.drawCircle(thumbCx, cy, thumbR, fillPaint)
            }

            DesignStyle.IOS -> {
                val trackW = dpf2(48f)
                val trackH = dpf2(28f)
                val thumbD = dpf2(24f)
                val pad = (trackH - thumbD) / 2f

                val offTrack = if (isDark) 0xFF39393D.toInt() else 0xFFE9E9EA.toInt()
                val onTrack = if (Theme.isCurrentThemeMonet()) trackCheckedColor else 0xFF34C759.toInt()
                val curTrack = ColorUtils.blendARGB(offTrack, onTrack, p)

                rectF.set(cx - trackW / 2f, cy - trackH / 2f, cx + trackW / 2f, cy + trackH / 2f)
                fillPaint.color = Theme.multAlpha(curTrack, overrideAlpha)
                canvas.drawRoundRect(rectF, trackH / 2f, trackH / 2f, fillPaint)

                val minX = rectF.left + pad + thumbD / 2f
                val maxX = rectF.right - pad - thumbD / 2f
                val thumbCx = minX + (maxX - minX) * p
                fillPaint.color = Theme.multAlpha(0x1F000000, overrideAlpha)
                canvas.drawCircle(thumbCx, cy + dpf2(1f), thumbD / 2f, fillPaint)
                fillPaint.color = Theme.multAlpha(Color.WHITE, overrideAlpha)
                canvas.drawCircle(thumbCx, cy, thumbD / 2f, fillPaint)
            }

            DesignStyle.MATERIAL_YOU -> Unit
        }
    }
}
