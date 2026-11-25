/*
 * SuperGram Exclusive Client Customization Bridge (.splug only)
 * Based on exteraGram (https://github.com/exteraSquad/exteraGram), GNU GPL v3+
 */

package com.exteragram.messenger.plugins

import com.exteragram.messenger.DesignStyle
import com.exteragram.messenger.DividerStyle
import com.exteragram.messenger.TabCounterMode
import com.exteragram.messenger.TabIconsMode
import com.exteragram.messenger.appicons.AppIconController
import com.exteragram.messenger.applyDesignStyle
import com.exteragram.messenger.avatarCorners
import com.exteragram.messenger.centerTitle
import com.exteragram.messenger.config.FLOATING
import com.exteragram.messenger.config.HIDE
import com.exteragram.messenger.config.SHOW
import com.exteragram.messenger.config.setMode
import com.exteragram.messenger.designStyle
import com.exteragram.messenger.dividerStyle
import com.exteragram.messenger.drawer.MainMenuItem
import com.exteragram.messenger.drawer.mainMenuHiddenItems
import com.exteragram.messenger.drawer.mainMenuLayout
import com.exteragram.messenger.drawer.saveMainMenuLayout
import com.exteragram.messenger.editor
import com.exteragram.messenger.forceSnow
import com.exteragram.messenger.glassMessageMenu
import com.exteragram.messenger.hideActionBarStatus
import com.exteragram.messenger.hideAllChats
import com.exteragram.messenger.hideDialogsSearchBar
import com.exteragram.messenger.hideFloatingButton
import com.exteragram.messenger.hideNavigationBarLabels
import com.exteragram.messenger.hideStories
import com.exteragram.messenger.icons.IconManager
import com.exteragram.messenger.icons.IconPack
import com.exteragram.messenger.icons.iconPacksHidden
import com.exteragram.messenger.icons.iconPacksLayout
import com.exteragram.messenger.icons.saveIconPacksLayout
import com.exteragram.messenger.immersiveDrawerAnimation
import com.exteragram.messenger.navigationDrawer
import com.exteragram.messenger.newChatHeaderStyle
import com.exteragram.messenger.newFabStyle
import com.exteragram.messenger.newNavigationBarStyle
import com.exteragram.messenger.newPlayerStyle
import com.exteragram.messenger.newProfileActionsStyle
import com.exteragram.messenger.preferences.utils.SuperGramUiConfig
import com.exteragram.messenger.replyBackground
import com.exteragram.messenger.replyColors
import com.exteragram.messenger.replyEmoji
import com.exteragram.messenger.sectionRadius
import com.exteragram.messenger.sectionsSeparatedHeaders
import com.exteragram.messenger.showFeedTab
import com.exteragram.messenger.squareFab
import com.exteragram.messenger.stickerShape
import com.exteragram.messenger.stickerSize
import com.exteragram.messenger.tabCounterMode
import com.exteragram.messenger.tabIcons
import com.exteragram.messenger.titleText
import com.exteragram.messenger.uiCardBorder
import com.exteragram.messenger.uiCardPaddingDp
import com.exteragram.messenger.utils.ui.DesignStyleHelper
import com.exteragram.messenger.widePostsInChannels
import com.exteragram.messenger.widePostsInFeed

object SuperGramPluginBridge {

    private val TAB_KEY_TO_INDEX = mapOf(
        "chats" to 0,
        "contacts" to 1,
        "settings" to 2,
        "calls" to 3,
        "profile" to 4,
        "feed" to 5
    )

    private val DRAWER_KEY_TO_ID = mapOf(
        "divider" to MainMenuItem.DIVIDER.id,
        "profile" to MainMenuItem.PROFILE.id,
        "archive" to MainMenuItem.ARCHIVE.id,
        "bots" to MainMenuItem.BOTS.id,
        "new_group" to MainMenuItem.NEW_GROUP.id,
        "contacts" to MainMenuItem.CONTACTS.id,
        "new_channel" to MainMenuItem.NEW_CHANNEL.id,
        "calls" to MainMenuItem.CALLS.id,
        "saved" to MainMenuItem.SAVED.id,
        "settings" to MainMenuItem.SETTINGS.id,
        "plugins" to MainMenuItem.PLUGINS.id,
        "browser" to MainMenuItem.BROWSER.id,
        "qr" to MainMenuItem.QR.id,
        "feed" to MainMenuItem.FEED.id,
        "wallet" to MainMenuItem.WALLET.id
    )

    @JvmStatic
    fun assertSuperGramPlugin(pluginId: String?) {
        if (pluginId.isNullOrBlank() || !PluginsController.isSuperGramPlugin(pluginId)) {
            throw SecurityException(
                "SuperGram UI customization API (supergram_ui) is restricted to .splug customization plugins."
            )
        }
    }

    @JvmStatic
    fun isSuperGramPlugin(pluginId: String?): Boolean =
        !pluginId.isNullOrBlank() && PluginsController.isSuperGramPlugin(pluginId)

    @JvmStatic
    fun getDesignStyle(pluginId: String?): String {
        assertSuperGramPlugin(pluginId)
        return when (designStyle) {
            DesignStyle.MATERIAL_YOU -> "material_you"
            DesignStyle.MIUIX -> "miuix"
            DesignStyle.WINDOWS_11 -> "windows_11"
            DesignStyle.IOS -> "ios"
        }
    }

    @JvmStatic
    fun setDesignStyle(pluginId: String?, styleName: String, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        val style = when (styleName.trim().lowercase()) {
            "material_you", "material", "materialyou" -> DesignStyle.MATERIAL_YOU
            "miuix", "hyperos", "miui" -> DesignStyle.MIUIX
            "windows_11", "windows11", "win11", "fluent" -> DesignStyle.WINDOWS_11
            "ios", "cupertino" -> DesignStyle.IOS
            else -> throw IllegalArgumentException("Unknown design style: $styleName")
        }
        applyDesignStyle(style)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(false)
    }

    @JvmStatic
    fun setCardStyle(
        pluginId: String?,
        radiusDp: Float?,
        paddingDp: Int?,
        cardBorder: Boolean?,
        separatedHeaders: Boolean?,
        divider: String?,
        centerBarTitle: Boolean?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        if (radiusDp != null) sectionRadius = radiusDp.coerceIn(0f, 28f)
        if (paddingDp != null) uiCardPaddingDp = paddingDp.coerceIn(-1, 24)
        if (cardBorder != null) uiCardBorder = cardBorder
        if (separatedHeaders != null) sectionsSeparatedHeaders = separatedHeaders
        if (!divider.isNullOrBlank()) {
            dividerStyle = when (divider.trim().lowercase()) {
                "line" -> DividerStyle.LINE
                "segments" -> DividerStyle.SEGMENTS
                "hidden", "none" -> DividerStyle.HIDDEN
                else -> throw IllegalArgumentException("Unknown divider style: $divider")
            }
        }
        if (centerBarTitle != null) centerTitle = centerBarTitle
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(false)
    }

    @JvmStatic
    fun getCardStyle(pluginId: String?): Map<String, Any?> {
        assertSuperGramPlugin(pluginId)
        return mapOf(
            "design_style" to getDesignStyle(pluginId),
            "radius" to sectionRadius,
            "padding" to uiCardPaddingDp,
            "border" to uiCardBorder,
            "separated_headers" to sectionsSeparatedHeaders,
            "divider" to dividerStyle.name.lowercase(),
            "center_title" to centerTitle
        )
    }

    @JvmStatic
    fun setColor(
        pluginId: String?,
        keyOrName: Any,
        lightColor: Int?,
        darkColor: Int?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        val id = pluginId!!
        val key = when (keyOrName) {
            is Number -> keyOrName.toInt()
            is String -> DesignStyleHelper.resolveThemeKey(keyOrName)
            else -> -1
        }
        if (key < 0) {
            throw IllegalArgumentException("Unknown Telegram Theme key: $keyOrName")
        }
        DesignStyleHelper.setPluginColorOverride(id, key, lightColor, darkColor)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(false)
    }

    @JvmStatic
    fun setCardBorderStyle(
        pluginId: String?,
        lightColor: Int?,
        darkColor: Int?,
        widthDp: Float?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        DesignStyleHelper.setPluginBorderOverride(pluginId!!, lightColor, darkColor, widthDp)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(false)
    }

    @JvmStatic
    fun clearTheme(pluginId: String?, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        val changed = DesignStyleHelper.clearPluginOverrides(pluginId)
        if (refresh && changed) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setIconPack(pluginId: String?, packName: String, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        val targetBaseId = when (packName.trim().lowercase()) {
            "default", "extera", "telegram" -> IconPack.ID_DEFAULT
            "solar" -> IconPack.ID_SOLAR
            "remix" -> IconPack.ID_REMIX
            else -> {
                IconManager.setActiveCustomPack(packName)
                if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
                return
            }
        }
        val baseIds = listOf(IconPack.ID_DEFAULT, IconPack.ID_SOLAR, IconPack.ID_REMIX)
        iconPacksLayout.removeAll(baseIds.toSet())
        iconPacksHidden.removeAll(baseIds.toSet())
        iconPacksLayout.add(targetBaseId)
        baseIds.filter { it != targetBaseId }.forEach { iconPacksHidden.add(it) }
        saveIconPacksLayout()
        IconManager.initialize(true)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setAppIcon(pluginId: String?, iconId: String) {
        assertSuperGramPlugin(pluginId)
        val icon = AppIconController.findById(iconId.trim().lowercase())
            ?: throw IllegalArgumentException("Unknown app icon id: $iconId")
        AppIconController.setIcon(icon)
    }

    @JvmStatic
    fun replaceIcon(pluginId: String?, targetName: String, replacementName: String?, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        val targetId = DesignStyleHelper.resolveDrawableId(targetName)
        if (targetId == 0) throw IllegalArgumentException("Unknown target drawable: $targetName")
        val replacementId = if (replacementName.isNullOrBlank()) 0 else DesignStyleHelper.resolveDrawableId(replacementName)
        if (!replacementName.isNullOrBlank() && replacementId == 0) {
            throw IllegalArgumentException("Unknown replacement drawable: $replacementName")
        }
        DesignStyleHelper.setPluginIconReplacement(pluginId!!, targetId, replacementId)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setIconFile(pluginId: String?, targetName: String, filePath: String?, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        val targetId = DesignStyleHelper.resolveDrawableId(targetName)
        if (targetId == 0) throw IllegalArgumentException("Unknown target drawable: $targetName")
        DesignStyleHelper.setPluginIconFile(pluginId!!, targetId, filePath)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun clearIcons(pluginId: String?, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        if (DesignStyleHelper.clearPluginIconOverrides(pluginId) && refresh) {
            DesignStyleHelper.triggerLiveUiRefresh(true)
        }
    }

    @JvmStatic
    fun setFabLayout(
        pluginId: String?,
        align: String?,
        bottomMarginDp: Int?,
        sideMarginDp: Int?,
        hide: Boolean?,
        square: Boolean?,
        newStyle: Boolean?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        DesignStyleHelper.setPluginFabLayout(pluginId!!, align, bottomMarginDp, sideMarginDp)
        if (hide != null) hideFloatingButton = hide
        if (square != null) squareFab = square
        if (newStyle != null) newFabStyle = newStyle
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setActionBar(
        pluginId: String?,
        heightDp: Int?,
        centerTitleBar: Boolean?,
        hideStatus: Boolean?,
        titleMode: Int?,
        hideSearchBar: Boolean?,
        newChatHeader: Boolean?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        if (heightDp != null) DesignStyleHelper.setPluginActionBarHeight(pluginId!!, heightDp)
        if (centerTitleBar != null) centerTitle = centerTitleBar
        if (hideStatus != null) hideActionBarStatus = hideStatus
        if (titleMode != null) titleText = titleMode.coerceIn(0, 6)
        if (hideSearchBar != null) hideDialogsSearchBar = hideSearchBar
        if (newChatHeader != null) newChatHeaderStyle = newChatHeader
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setNavigationBar(
        pluginId: String?,
        mode: String?,
        newStyle: Boolean?,
        hideLabels: Boolean?,
        drawerEnabled: Boolean?,
        immersiveDrawer: Boolean?,
        tabIconsMode: String?,
        tabCounters: String?,
        hideAllChatsTab: Boolean?,
        feedTab: Boolean?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        if (!mode.isNullOrBlank()) {
            val m = when (mode.trim().lowercase()) {
                "show" -> SHOW
                "hide" -> HIDE
                "floating" -> FLOATING
                else -> throw IllegalArgumentException("Unknown navigation mode: $mode")
            }
            setMode(m)
            editor.putInt("bottomNavigationBarMode", m).apply()
        }
        if (newStyle != null) newNavigationBarStyle = newStyle
        if (hideLabels != null) hideNavigationBarLabels = hideLabels
        if (drawerEnabled != null) navigationDrawer = drawerEnabled
        if (immersiveDrawer != null) immersiveDrawerAnimation = immersiveDrawer
        if (!tabIconsMode.isNullOrBlank()) {
            tabIcons = when (tabIconsMode.trim().lowercase()) {
                "titles_only", "titles" -> TabIconsMode.TITLES_ONLY
                "icons_only", "icons" -> TabIconsMode.ICONS_ONLY
                "icons_and_titles", "titles_and_icons", "both" -> TabIconsMode.ICONS_AND_TITLES
                else -> throw IllegalArgumentException("Unknown tab_icons mode: $tabIconsMode")
            }
        }
        if (!tabCounters.isNullOrBlank()) {
            tabCounterMode = when (tabCounters.trim().lowercase()) {
                "all" -> TabCounterMode.ALL
                "unmuted" -> TabCounterMode.UNMUTED
                "hidden", "none" -> TabCounterMode.HIDDEN
                else -> throw IllegalArgumentException("Unknown tab_counters mode: $tabCounters")
            }
        }
        if (hideAllChatsTab != null) hideAllChats = hideAllChatsTab
        if (feedTab != null) showFeedTab = feedTab
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setTabsLayout(
        pluginId: String?,
        orderKeys: List<String>?,
        hiddenKeys: List<String>?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        val orderArr = orderKeys?.mapNotNull { TAB_KEY_TO_INDEX[it.trim().lowercase()] }?.toIntArray()
        val hiddenSet = hiddenKeys?.mapNotNull { TAB_KEY_TO_INDEX[it.trim().lowercase()] }?.toSet()
        DesignStyleHelper.setPluginTabsLayout(pluginId!!, orderArr, hiddenSet)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun getDrawerMenu(pluginId: String?): Map<String, List<String>> {
        assertSuperGramPlugin(pluginId)
        val idToKey = DRAWER_KEY_TO_ID.entries.associate { it.value to it.key }
        return mapOf(
            "items" to mainMenuLayout.mapNotNull { idToKey[it] },
            "hidden" to mainMenuHiddenItems.mapNotNull { idToKey[it] }
        )
    }

    @JvmStatic
    fun setDrawerMenu(
        pluginId: String?,
        items: List<String>,
        hidden: List<String>?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        val newLayout = ArrayList<Int>()
        for (k in items) {
            val id = DRAWER_KEY_TO_ID[k.trim().lowercase()]
                ?: throw IllegalArgumentException("Unknown drawer item: $k")
            newLayout.add(id)
        }
        val newHidden = ArrayList<Int>()
        if (hidden != null) {
            for (k in hidden) {
                val id = DRAWER_KEY_TO_ID[k.trim().lowercase()] ?: continue
                if (id != MainMenuItem.DIVIDER.id && !newHidden.contains(id)) {
                    newHidden.add(id)
                }
            }
        }
        mainMenuLayout.clear()
        mainMenuLayout.addAll(newLayout)
        mainMenuHiddenItems.clear()
        mainMenuHiddenItems.addAll(newHidden)
        saveMainMenuLayout()
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setFont(
        pluginId: String?,
        regularPath: String?,
        boldPath: String?,
        monoPath: String?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        DesignStyleHelper.setPluginFontOverride(pluginId!!, regularPath, boldPath, monoPath)
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun getLayout(pluginId: String?, target: String): Map<String, Any> {
        assertSuperGramPlugin(pluginId)
        return when (target.trim().lowercase()) {
            "supergram" -> mapOf(
                "items" to ArrayList(SuperGramUiConfig.getSuperGramLayout()),
                "hidden" to ArrayList(SuperGramUiConfig.getSuperGramHidden())
            )
            "main", "main_settings", "settings" -> mapOf(
                "items" to ArrayList(SuperGramUiConfig.getMainSettingsLayout()),
                "hidden" to ArrayList(SuperGramUiConfig.getMainSettingsHidden())
            )
            else -> throw IllegalArgumentException("Unknown layout target: $target")
        }
    }

    @JvmStatic
    fun setLayout(
        pluginId: String?,
        target: String,
        items: List<String>,
        hidden: List<String>?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        val hiddenSet = hidden?.toSet() ?: emptySet()
        when (target.trim().lowercase()) {
            "supergram" -> SuperGramUiConfig.saveSuperGramLayout(items, hiddenSet)
            "main", "main_settings", "settings" -> SuperGramUiConfig.saveMainSettingsLayout(items, hiddenSet)
            else -> throw IllegalArgumentException("Unknown layout target: $target")
        }
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun resetLayouts(pluginId: String?, refresh: Boolean) {
        assertSuperGramPlugin(pluginId)
        SuperGramUiConfig.resetAllLayouts()
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setChatStyle(
        pluginId: String?,
        avatarCornersDp: Float?,
        stickerSizeDp: Float?,
        stickerShapeMode: Int?,
        replyColorsFlag: Boolean?,
        replyEmojiFlag: Boolean?,
        replyBgFlag: Boolean?,
        widePostsFeedFlag: Boolean?,
        widePostsChannelsFlag: Boolean?,
        hideStoriesFlag: Boolean?,
        newPlayerFlag: Boolean?,
        newProfileActionsFlag: Boolean?,
        glassMessageMenuFlag: Boolean?,
        forceSnowFlag: Boolean?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        if (avatarCornersDp != null) avatarCorners = avatarCornersDp.coerceIn(0f, 32f)
        if (stickerSizeDp != null) stickerSize = stickerSizeDp.coerceIn(4f, 24f)
        if (stickerShapeMode != null) stickerShape = stickerShapeMode.coerceIn(0, 2)
        if (replyColorsFlag != null) replyColors = replyColorsFlag
        if (replyEmojiFlag != null) replyEmoji = replyEmojiFlag
        if (replyBgFlag != null) replyBackground = replyBgFlag
        if (widePostsFeedFlag != null) widePostsInFeed = widePostsFeedFlag
        if (widePostsChannelsFlag != null) widePostsInChannels = widePostsChannelsFlag
        if (hideStoriesFlag != null) hideStories = hideStoriesFlag
        if (newPlayerFlag != null) newPlayerStyle = newPlayerFlag
        if (newProfileActionsFlag != null) newProfileActionsStyle = newProfileActionsFlag
        if (glassMessageMenuFlag != null) glassMessageMenu = glassMessageMenuFlag
        if (forceSnowFlag != null) forceSnow = forceSnowFlag
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(true)
    }

    @JvmStatic
    fun setUiTweaks(
        pluginId: String?,
        avatarCornersDp: Float?,
        tabIconsMode: String?,
        hideStoriesFlag: Boolean?,
        hideFabFlag: Boolean?,
        squareFabFlag: Boolean?,
        forceSnowFlag: Boolean?,
        refresh: Boolean
    ) {
        assertSuperGramPlugin(pluginId)
        if (avatarCornersDp != null) avatarCorners = avatarCornersDp.coerceIn(0f, 32f)
        if (!tabIconsMode.isNullOrBlank()) {
            tabIcons = when (tabIconsMode.trim().lowercase()) {
                "titles_only", "titles" -> TabIconsMode.TITLES_ONLY
                "icons_only", "icons" -> TabIconsMode.ICONS_ONLY
                "icons_and_titles", "titles_and_icons", "both" -> TabIconsMode.ICONS_AND_TITLES
                else -> throw IllegalArgumentException("Unknown tab_icons mode: $tabIconsMode")
            }
        }
        if (hideStoriesFlag != null) hideStories = hideStoriesFlag
        if (hideFabFlag != null) hideFloatingButton = hideFabFlag
        if (squareFabFlag != null) squareFab = squareFabFlag
        if (forceSnowFlag != null) forceSnow = forceSnowFlag
        if (refresh) DesignStyleHelper.triggerLiveUiRefresh(false)
    }

    @JvmStatic
    @JvmOverloads
    fun refreshUi(pluginId: String?, rebuildAll: Boolean = false) {
        assertSuperGramPlugin(pluginId)
        DesignStyleHelper.triggerLiveUiRefresh(rebuildAll)
    }
}
