/*
 * SuperGram Flexible UI Layout Hub
 * Based on exteraGram (https://github.com/exteraSquad/exteraGram), GNU GPL v3+
 */

package com.exteragram.messenger.preferences.appearance

import android.view.View
import com.exteragram.messenger.DesignStyle
import com.exteragram.messenger.SECTION_RADIUS_MAX_DP
import com.exteragram.messenger.applyDesignStyle
import com.exteragram.messenger.designStyle
import com.exteragram.messenger.getSectionRadiusDp
import com.exteragram.messenger.preferences.BasePreferencesActivity
import com.exteragram.messenger.preferences.utils.SuperGramUiConfig
import com.exteragram.messenger.pillstack.ui.PillStackPreferencesActivity
import com.exteragram.messenger.sectionRadius
import com.exteragram.messenger.sectionsSeparatedHeaders
import com.exteragram.messenger.uiCardBorder
import com.exteragram.messenger.uiCardPaddingDp
import com.exteragram.messenger.utils.ui.DesignStyleHelper
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import java.util.ArrayList

class UiLayoutPreferencesActivity : BasePreferencesActivity() {

    enum class UiLayoutItem {
        DESIGN_STYLE,
        SUPERGRAM_TILES,
        MAIN_SETTINGS_TILES,
        DRAWER_NAV,
        PILL_STACK,
        CARD_PADDING,
        SECTION_RADIUS,
        CARD_BORDER,
        SEPARATED_HEADERS,
        RESET_LAYOUTS;

        val id: Int = ordinal + 1
    }

    override fun getTitle(): String = getString(R.string.UiCustomization)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asHeader(getString(R.string.DesignStyle)))
        items.add(
            uButton(
                UiLayoutItem.DESIGN_STYLE,
                getString(R.string.DesignStyle),
                R.drawable.msg_theme,
                value = DesignStyleHelper.getStyleDisplayName(),
                linkAlias = "designStyle"
            )
        )
        items.add(UItem.asShadow(getString(R.string.DesignStyleInfo)))

        items.add(UItem.asHeader(getString(R.string.Categories)))
        items.add(
            uButton(
                UiLayoutItem.SUPERGRAM_TILES,
                getString(R.string.UiLayoutSuperGramPrefs),
                R.drawable.msg_list,
                subtext = getString(R.string.UiCustomizationInfo),
                searchable = false
            )
        )
        items.add(
            uButton(
                UiLayoutItem.MAIN_SETTINGS_TILES,
                getString(R.string.UiLayoutMainSettings),
                R.drawable.msg_settings_old,
                subtext = getString(R.string.Preferences),
                searchable = false
            )
        )
        items.add(
            uButton(
                UiLayoutItem.DRAWER_NAV,
                getString(R.string.UiLayoutDrawerNav),
                R.drawable.msg_newphone,
                searchable = false
            )
        )
        items.add(
            uButton(
                UiLayoutItem.PILL_STACK,
                getString(R.string.UiLayoutPillStack),
                R.drawable.outline_header_search,
                searchable = false
            )
        )
        items.add(UItem.asShadow(getString(R.string.UiLayoutHelp)))

        items.add(UItem.asHeader(getString(R.string.Sections)))
        items.add(
            UItem.asIntSlideView(
                UiLayoutItem.CARD_PADDING.id,
                -1,
                uiCardPaddingDp.coerceIn(-1, 24),
                24,
                { value -> if (value < 0) getString(R.string.UiCardPaddingAuto) else "$value dp" }
            ) { value ->
                uiCardPaddingDp = value
                applyGeometryChange()
            }.apply {
                text = getString(R.string.UiCardPadding)
            }
        )
        items.add(
            UItem.asIntSlideView(
                UiLayoutItem.SECTION_RADIUS.id,
                0,
                getSectionRadiusDp(),
                SECTION_RADIUS_MAX_DP,
                { value -> if (value == 0) getString(R.string.BlurOff) else "$value dp" }
            ) { value ->
                sectionRadius = value.toFloat()
                applyGeometryChange()
            }.apply {
                text = getString(R.string.Sections)
            }
        )
        items.add(
            uCheck(
                UiLayoutItem.CARD_BORDER,
                getString(R.string.UiCardBorder),
                uiCardBorder,
                subtext = getString(R.string.UiCardBorderInfo),
                searchable = false
            )
        )
        items.add(
            uCheck(
                UiLayoutItem.SEPARATED_HEADERS,
                getString(R.string.SeparateHeaders),
                sectionsSeparatedHeaders,
                searchable = false
            )
        )
        items.add(UItem.asShadow())

        items.add(
            uButton(
                UiLayoutItem.RESET_LAYOUTS,
                getString(R.string.UiLayoutReset),
                R.drawable.msg_reset,
                red = true,
                searchable = false
            )
        )
        items.add(UItem.asShadow())
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id <= 0 || item.id > UiLayoutItem.entries.size) return
        when (UiLayoutItem.entries[item.id - 1]) {
            UiLayoutItem.DESIGN_STYLE -> {
                val names = DesignStyle.entries.map { DesignStyleHelper.getStyleDisplayName(it) }.toTypedArray()
                showListDialog(item, names, getString(R.string.DesignStyle), designStyle.ordinal) { idx ->
                    applyDesignStyle(DesignStyle.entries[idx])
                    Theme.applyCommonTheme()
                    fragmentView?.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray))
                    actionBar?.refreshTitlePosition(true)
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true, true)
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface)
                    applyGeometryChange()
                    listView.adapter?.update(true)
                    parentLayout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST)
                }
            }
            UiLayoutItem.SUPERGRAM_TILES ->
                presentFragment(LayoutOrderEditorActivity(LayoutOrderEditorActivity.Mode.SUPERGRAM))
            UiLayoutItem.MAIN_SETTINGS_TILES ->
                presentFragment(LayoutOrderEditorActivity(LayoutOrderEditorActivity.Mode.MAIN_SETTINGS))
            UiLayoutItem.DRAWER_NAV ->
                presentFragment(AppNavigationPreferencesActivity())
            UiLayoutItem.PILL_STACK ->
                presentFragment(PillStackPreferencesActivity())
            UiLayoutItem.CARD_BORDER -> {
                toggleBooleanSettingAndRefresh(item) { uiCardBorder = it }
                applyGeometryChange()
            }
            UiLayoutItem.SEPARATED_HEADERS -> {
                toggleBooleanSettingAndRefresh(item) { sectionsSeparatedHeaders = it }
                applyGeometryChange()
            }
            UiLayoutItem.RESET_LAYOUTS -> {
                SuperGramUiConfig.resetAllLayouts()
                uiCardPaddingDp = -1
                applyGeometryChange()
                listView.adapter?.update(true)
                BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_delete, getString(R.string.UiLayoutReset)).show()
            }
            else -> Unit
        }
    }

    private fun applyGeometryChange() {
        listView.setSections()
        listView.invalidate()
        listView.invalidateItemDecorations()
        parentLayout.rebuildFragments(0)
    }
}
