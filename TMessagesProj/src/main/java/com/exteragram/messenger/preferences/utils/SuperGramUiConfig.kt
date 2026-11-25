/*
 * SuperGram Flexible UI Layout Configuration
 * Based on exteraGram (https://github.com/exteraSquad/exteraGram), GNU GPL v3+
 */

package com.exteragram.messenger.preferences.utils

import com.exteragram.messenger.GSON
import com.exteragram.messenger.mainSettingsHiddenJson
import com.exteragram.messenger.mainSettingsLayoutJson
import com.exteragram.messenger.superGramPrefsHiddenJson
import com.exteragram.messenger.superGramPrefsLayoutJson
import com.google.gson.reflect.TypeToken
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R

object SuperGramUiConfig {

    const val DIVIDER_PREFIX = "div_"

    // SuperGram Settings (MainPreferencesActivity) item keys
    const val SG_GENERAL = "cat_general"
    const val SG_APPEARANCE = "cat_appearance"
    const val SG_CHATS = "cat_chats"
    const val SG_PLUGINS = "cat_plugins"
    const val SG_UI_LAYOUT = "cat_ui_layout"
    const val SG_OTHER = "cat_other"
    const val SG_ATTRIBUTION = "card_attribution"

    // Main Telegram Settings (SettingsActivity) item keys
    const val MAIN_SUPERGRAM = "sg_prefs"
    const val MAIN_ACCOUNT = "tg_account"
    const val MAIN_CHATS = "tg_chats"
    const val MAIN_PRIVACY = "tg_privacy"
    const val MAIN_NOTIFICATIONS = "tg_notifications"
    const val MAIN_DATA = "tg_data"
    const val MAIN_FOLDERS = "tg_folders"
    const val MAIN_DEVICES = "tg_devices"
    const val MAIN_POWER = "tg_power"
    const val MAIN_LANGUAGE = "tg_language"

    private val LIST_TYPE = object : TypeToken<ArrayList<String>>() {}.type
    private val SET_TYPE = object : TypeToken<HashSet<String>>() {}.type

    @JvmStatic
    fun defaultSuperGramLayout(): ArrayList<String> = arrayListOf(
        SG_GENERAL,
        SG_APPEARANCE,
        SG_CHATS,
        SG_PLUGINS,
        SG_UI_LAYOUT,
        SG_OTHER,
        "div_1",
        SG_ATTRIBUTION
    )

    @JvmStatic
    fun defaultMainSettingsLayout(): ArrayList<String> = arrayListOf(
        MAIN_SUPERGRAM,
        "div_1",
        MAIN_ACCOUNT,
        MAIN_CHATS,
        MAIN_PRIVACY,
        MAIN_NOTIFICATIONS,
        MAIN_DATA,
        MAIN_FOLDERS,
        MAIN_DEVICES,
        MAIN_POWER,
        MAIN_LANGUAGE
    )

    private fun defaultSuperGramItems(): List<String> = listOf(
        SG_GENERAL, SG_APPEARANCE, SG_CHATS, SG_PLUGINS, SG_UI_LAYOUT, SG_OTHER, SG_ATTRIBUTION
    )

    private fun defaultMainSettingsItems(): List<String> = listOf(
        MAIN_SUPERGRAM, MAIN_ACCOUNT, MAIN_CHATS, MAIN_PRIVACY, MAIN_NOTIFICATIONS,
        MAIN_DATA, MAIN_FOLDERS, MAIN_DEVICES, MAIN_POWER, MAIN_LANGUAGE
    )

    private fun parseList(json: String, fallback: ArrayList<String>, validItems: List<String>): ArrayList<String> {
        if (json.isBlank()) return ArrayList(fallback)
        return try {
            val parsed: ArrayList<String> = GSON.fromJson(json, LIST_TYPE) ?: return ArrayList(fallback)
            val result = ArrayList<String>()
            val validSet = validItems.toSet()
            for (token in parsed) {
                if ((token.startsWith(DIVIDER_PREFIX) || validSet.contains(token)) && !result.contains(token)) {
                    result.add(token)
                }
            }
            for (item in validItems) {
                if (!result.contains(item)) {
                    result.add(item)
                }
            }
            if (result.isEmpty()) ArrayList(fallback) else result
        } catch (_: Throwable) {
            ArrayList(fallback)
        }
    }

    private fun parseSet(json: String): HashSet<String> {
        if (json.isBlank()) return HashSet()
        return try {
            GSON.fromJson<HashSet<String>>(json, SET_TYPE) ?: HashSet()
        } catch (_: Throwable) {
            HashSet()
        }
    }

    @JvmStatic
    fun getSuperGramLayout(): ArrayList<String> =
        parseList(superGramPrefsLayoutJson, defaultSuperGramLayout(), defaultSuperGramItems())

    @JvmStatic
    fun getSuperGramHidden(): HashSet<String> =
        parseSet(superGramPrefsHiddenJson)

    @JvmStatic
    fun saveSuperGramLayout(layout: List<String>, hidden: Set<String>) {
        superGramPrefsLayoutJson = GSON.toJson(layout)
        superGramPrefsHiddenJson = GSON.toJson(hidden)
    }

    @JvmStatic
    fun getMainSettingsLayout(): ArrayList<String> =
        parseList(mainSettingsLayoutJson, defaultMainSettingsLayout(), defaultMainSettingsItems())

    @JvmStatic
    fun getMainSettingsHidden(): HashSet<String> =
        parseSet(mainSettingsHiddenJson)

    @JvmStatic
    fun saveMainSettingsLayout(layout: List<String>, hidden: Set<String>) {
        mainSettingsLayoutJson = GSON.toJson(layout)
        mainSettingsHiddenJson = GSON.toJson(hidden)
    }

    @JvmStatic
    fun resetAllLayouts() {
        superGramPrefsLayoutJson = ""
        superGramPrefsHiddenJson = ""
        mainSettingsLayoutJson = ""
        mainSettingsHiddenJson = ""
    }

    @JvmStatic
    fun isDivider(key: String): Boolean = key.startsWith(DIVIDER_PREFIX)

    @JvmStatic
    fun nextDividerKey(existing: Collection<String>): String {
        var i = 1
        while (existing.contains(DIVIDER_PREFIX + i)) i++
        return DIVIDER_PREFIX + i
    }

    @JvmStatic
    fun getSuperGramItemTitle(key: String): String = when (key) {
        SG_GENERAL -> getString(R.string.General)
        SG_APPEARANCE -> getString(R.string.Appearance)
        SG_CHATS -> getString(R.string.SearchAllChatsShort)
        SG_PLUGINS -> getString(R.string.Plugins)
        SG_UI_LAYOUT -> getString(R.string.UiCustomization)
        SG_OTHER -> getString(R.string.LocalOther)
        SG_ATTRIBUTION -> getString(R.string.SuperGramAttribution)
        else -> getString(R.string.DividerStyleLine)
    }

    @JvmStatic
    fun getMainSettingsItemTitle(key: String): String = when (key) {
        MAIN_SUPERGRAM -> getString(R.string.Preferences)
        MAIN_ACCOUNT -> getString(R.string.SettingsAccount)
        MAIN_CHATS -> getString(R.string.SettingsChat)
        MAIN_PRIVACY -> getString(R.string.SettingsPrivacySecurity)
        MAIN_NOTIFICATIONS -> getString(R.string.SettingsNotifications)
        MAIN_DATA -> getString(R.string.SettingsData)
        MAIN_FOLDERS -> getString(R.string.SettingsFolders)
        MAIN_DEVICES -> getString(R.string.SettingsDevices)
        MAIN_POWER -> getString(R.string.SettingsPowerSaving)
        MAIN_LANGUAGE -> getString(R.string.SettingsLanguage)
        else -> getString(R.string.DividerStyleLine)
    }

    @JvmStatic
    fun getMainSettingsItemIcon(key: String): Int = when (key) {
        MAIN_SUPERGRAM -> R.drawable.ic_foreground
        MAIN_ACCOUNT -> R.drawable.settings_account
        MAIN_CHATS -> R.drawable.settings_chat
        MAIN_PRIVACY -> R.drawable.settings_privacy
        MAIN_NOTIFICATIONS -> R.drawable.settings_sounds
        MAIN_DATA -> R.drawable.settings_data
        MAIN_FOLDERS -> R.drawable.settings_folders
        MAIN_DEVICES -> R.drawable.settings_devices
        MAIN_POWER -> R.drawable.settings_power
        MAIN_LANGUAGE -> R.drawable.settings_language
        else -> R.drawable.msg_block
    }
}
