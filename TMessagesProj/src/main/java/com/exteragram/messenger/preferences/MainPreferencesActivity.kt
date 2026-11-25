/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @exteraGram, 2022-2026

 Licensed under the GNU GPL v. 3 or later, with the additional terms
 in ADDITIONAL_TERMS.md at the root of the repository.

*/

package com.exteragram.messenger.preferences

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import com.exteragram.messenger.plugins.PluginsController
import com.exteragram.messenger.plugins.ui.PluginsActivity
import com.exteragram.messenger.preferences.appearance.AppearancePreferencesActivity
import com.exteragram.messenger.preferences.appearance.UiLayoutPreferencesActivity
import com.exteragram.messenger.preferences.chats.ChatsPreferencesActivity
import com.exteragram.messenger.preferences.components.HeaderSettingsCell
import com.exteragram.messenger.preferences.utils.SuperGramUiConfig
import com.exteragram.messenger.useSystemIconShape
import com.exteragram.messenger.utils.system.VibratorUtils
import com.exteragram.messenger.utils.ui.DesignStyleHelper
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.BuildVars
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.browser.Browser
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalAdapter.VIEW_TYPE_SHADOW
import org.telegram.ui.LaunchActivity
import java.util.ArrayList

class MainPreferencesActivity : BasePreferencesActivity() {

    enum class PreferenceItem {
        HEADER_CELL,
        GENERAL_CATEGORY,
        APPEARANCE_CATEGORY,
        CHATS_CATEGORY,
        PLUGINS_CATEGORY,
        UI_LAYOUT_CATEGORY,
        OTHER_CATEGORY,
        SOURCE_CODE;

        val id: Int = ordinal + 1
    }

    private lateinit var headerSettingsCell: HeaderSettingsCell

    override fun getTitle(): String = getString(R.string.Preferences)
    override fun needHideTitle(): Boolean = true

    override fun createView(context: Context): View {
        headerSettingsCell = HeaderSettingsCell(context)
        fragmentView = super.createView(context)
        if (parentLayout?.isRightLayout == true) actionBar.setBackButtonImage(R.drawable.ic_ab_close)
        actionBar.menu.addItem(100, R.drawable.menu_edit_appearance).apply {
            contentDescription = getString(R.string.UiCustomization)
            setOnClickListener { presentFragment(UiLayoutPreferencesActivity()) }
        }
        return fragmentView
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asCustomShadow(headerSettingsCell, 198).setId(PreferenceItem.HEADER_CELL.id))
        items.add(UItem.asHeader(getString(R.string.Categories)))

        val layout = SuperGramUiConfig.getSuperGramLayout()
        val hidden = SuperGramUiConfig.getSuperGramHidden()

        for (key in layout) {
            if (SuperGramUiConfig.isDivider(key)) {
                if (items.isNotEmpty() && items.last().viewType != VIEW_TYPE_SHADOW) {
                    items.add(UItem.asShadow())
                }
                continue
            }
            if (hidden.contains(key)) continue
            when (key) {
                SuperGramUiConfig.SG_GENERAL ->
                    items.add(uButton(PreferenceItem.GENERAL_CATEGORY, getString(R.string.General), R.drawable.msg_media, linkAlias = "general"))
                SuperGramUiConfig.SG_APPEARANCE ->
                    items.add(uButton(PreferenceItem.APPEARANCE_CATEGORY, getString(R.string.Appearance), R.drawable.msg_theme, value = DesignStyleHelper.getStyleDisplayName(), linkAlias = "appearance"))
                SuperGramUiConfig.SG_CHATS ->
                    items.add(uButton(PreferenceItem.CHATS_CATEGORY, getString(R.string.SearchAllChatsShort), R.drawable.msg_discussion, linkAlias = "chats"))
                SuperGramUiConfig.SG_PLUGINS -> if (PluginsController.isPluginEngineSupported()) {
                    items.add(uButton(PreferenceItem.PLUGINS_CATEGORY, getString(R.string.Plugins), R.drawable.msg_plugins, linkAlias = "plugins"))
                }
                SuperGramUiConfig.SG_UI_LAYOUT ->
                    items.add(uButton(PreferenceItem.UI_LAYOUT_CATEGORY, getString(R.string.UiCustomization), R.drawable.msg_list, subtext = getString(R.string.UiCustomizationInfo), linkAlias = "uiCustomization"))
                SuperGramUiConfig.SG_OTHER ->
                    items.add(uButton(PreferenceItem.OTHER_CATEGORY, getString(R.string.LocalOther), R.drawable.msg_fave, linkAlias = "other"))
                SuperGramUiConfig.SG_ATTRIBUTION ->
                    items.add(uButton(PreferenceItem.SOURCE_CODE, getString(R.string.SuperGramAttribution), R.drawable.msg_info, value = "GitHub", subtext = getString(R.string.SuperGramAttributionSubtext), linkAlias = "sourceCode"))
            }
        }

        if (items.isEmpty() || items.last().viewType != VIEW_TYPE_SHADOW) {
            items.add(UItem.asShadow())
        }
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id <= 0 || item.id > PreferenceItem.entries.size) return
        when (PreferenceItem.entries[item.id - 1]) {
            PreferenceItem.HEADER_CELL -> if (!BuildVars.PM_BUILD) (parentActivity as? LaunchActivity)?.checkAppUpdate(true)
            PreferenceItem.GENERAL_CATEGORY -> presentFragment(GeneralPreferencesActivity())
            PreferenceItem.APPEARANCE_CATEGORY -> presentFragment(AppearancePreferencesActivity())
            PreferenceItem.CHATS_CATEGORY -> presentFragment(ChatsPreferencesActivity())
            PreferenceItem.PLUGINS_CATEGORY -> presentFragment(PluginsActivity())
            PreferenceItem.UI_LAYOUT_CATEGORY -> presentFragment(UiLayoutPreferencesActivity())
            PreferenceItem.OTHER_CATEGORY -> presentFragment(OtherPreferencesActivity())
            PreferenceItem.SOURCE_CODE -> Browser.openUrl(parentActivity, "https://github.com/exteraSquad/exteraGram")
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        if (item.id <= 0 || item.id > PreferenceItem.entries.size) return false
        val clickedItem = PreferenceItem.entries[item.id - 1]
        if (clickedItem == PreferenceItem.HEADER_CELL) {
            useSystemIconShape = !useSystemIconShape
            view.performHapticFeedback(VibratorUtils.getType(HapticFeedbackConstants.KEYBOARD_TAP), HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
            view.invalidate()
            return true
        }
        return super.onLongClick(item, view, position, x, y)
    }

    override fun getListTopPadding(topInset: Int): Int =
        topInset + ActionBar.getCurrentActionBarHeight() - AndroidUtilities.dp(20f)
}
