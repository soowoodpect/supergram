/*
 * SuperGram Drag-and-Drop Tile Layout Editor
 * Based on exteraGram (https://github.com/exteraSquad/exteraGram), GNU GPL v3+
 */

package com.exteragram.messenger.preferences.appearance

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.exteragram.messenger.preferences.BasePreferencesActivity
import com.exteragram.messenger.preferences.utils.SuperGramUiConfig
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import java.util.ArrayList

class LayoutOrderEditorActivity(private val mode: Mode) : BasePreferencesActivity() {

    enum class Mode {
        SUPERGRAM,
        MAIN_SETTINGS
    }

    private var reorderIcon: Drawable? = null
    private var resetItem: ActionBarMenuItem? = null

    private val activeKeys = ArrayList<String>()
    private val hiddenKeys = ArrayList<String>()
    private val idToKey = HashMap<Int, String>()
    private val keyToId = HashMap<String, Int>()
    private var nextId = 1000

    override fun getTitle(): String = when (mode) {
        Mode.SUPERGRAM -> getString(R.string.UiLayoutSuperGramPrefs)
        Mode.MAIN_SETTINGS -> getString(R.string.UiLayoutMainSettings)
    }

    override fun onFragmentCreate(): Boolean {
        loadFromConfig()
        return super.onFragmentCreate()
    }

    private fun loadFromConfig() {
        activeKeys.clear()
        hiddenKeys.clear()
        if (mode == Mode.SUPERGRAM) {
            val all = SuperGramUiConfig.getSuperGramLayout()
            val hidden = SuperGramUiConfig.getSuperGramHidden()
            for (key in all) {
                if (!SuperGramUiConfig.isDivider(key) && hidden.contains(key)) {
                    hiddenKeys.add(key)
                } else {
                    activeKeys.add(key)
                }
            }
        } else {
            val all = SuperGramUiConfig.getMainSettingsLayout()
            val hidden = SuperGramUiConfig.getMainSettingsHidden()
            for (key in all) {
                if (!SuperGramUiConfig.isDivider(key) && hidden.contains(key)) {
                    hiddenKeys.add(key)
                } else {
                    activeKeys.add(key)
                }
            }
        }
    }

    private fun saveToConfig() {
        val fullOrder = ArrayList<String>(activeKeys)
        fullOrder.addAll(hiddenKeys)
        if (mode == Mode.SUPERGRAM) {
            SuperGramUiConfig.saveSuperGramLayout(fullOrder, hiddenKeys.toSet())
        } else {
            SuperGramUiConfig.saveMainSettingsLayout(fullOrder, hiddenKeys.toSet())
        }
        parentLayout.rebuildFragments(0)
    }

    private fun idForKey(key: String): Int {
        return keyToId.getOrPut(key) {
            val id = nextId++
            idToKey[id] = key
            id
        }
    }

    override fun createView(context: Context): View {
        val view = super.createView(context)
        resetItem = actionBar.createMenu().addItem(0, R.drawable.msg_reset).apply {
            contentDescription = getString(R.string.Reset)
            setOnClickListener {
                if (mode == Mode.SUPERGRAM) {
                    SuperGramUiConfig.saveSuperGramLayout(SuperGramUiConfig.defaultSuperGramLayout(), emptySet())
                } else {
                    SuperGramUiConfig.saveMainSettingsLayout(SuperGramUiConfig.defaultMainSettingsLayout(), emptySet())
                }
                loadFromConfig()
                listView.adapter?.update(true)
                parentLayout.rebuildFragments(0)
                BulletinFactory.of(this@LayoutOrderEditorActivity)
                    .createSimpleBulletin(R.raw.ic_delete, getString(R.string.UiLayoutReset))
                    .show()
            }
        }
        listView.allowReorder(true)
        listView.listenReorder(::onReorder)
        return view
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        reorderIcon = reorderIcon ?: ContextCompat.getDrawable(context, R.drawable.list_reorder)

        adapter.whiteSectionStart()
        items.add(UItem.asHeader(getString(R.string.MainMenuItems)))
        adapter.reorderSectionStart()
        for (key in activeKeys) {
            items.add(createTileItem(key))
        }
        adapter.reorderSectionEnd()
        items.add(
            uButton(
                ADD_DIVIDER_ID,
                getString(R.string.UiLayoutAddDivider),
                icon = R.drawable.msg_add,
                accent = true,
                searchable = false
            )
        )
        adapter.whiteSectionEnd()
        items.add(UItem.asShadow(getString(R.string.UiLayoutHelp)))

        if (hiddenKeys.isNotEmpty()) {
            adapter.whiteSectionStart()
            items.add(UItem.asHeader(getString(R.string.MainMenuHiddenItems)))
            for (key in hiddenKeys) {
                items.add(
                    uButton(
                        idForKey(key),
                        titleForKey(key),
                        icon = iconForKey(key),
                        searchable = false
                    )
                )
            }
            adapter.whiteSectionEnd()
            items.add(UItem.asShadow())
        }
    }

    private fun createTileItem(key: String): UItem {
        val isDivider = SuperGramUiConfig.isDivider(key)
        val title = if (isDivider) getString(R.string.ArticleCommandDivider) else titleForKey(key)
        val icon = if (isDivider) R.drawable.msg_block else iconForKey(key)
        return uButton(idForKey(key), title, icon = icon, searchable = false).apply {
            object2 = reorderIcon
        }
    }

    private fun titleForKey(key: String): String = when (mode) {
        Mode.SUPERGRAM -> SuperGramUiConfig.getSuperGramItemTitle(key)
        Mode.MAIN_SETTINGS -> SuperGramUiConfig.getMainSettingsItemTitle(key)
    }

    private fun iconForKey(key: String): Int = when (mode) {
        Mode.SUPERGRAM -> when (key) {
            SuperGramUiConfig.SG_GENERAL -> R.drawable.msg_media
            SuperGramUiConfig.SG_APPEARANCE -> R.drawable.msg_theme
            SuperGramUiConfig.SG_CHATS -> R.drawable.msg_discussion
            SuperGramUiConfig.SG_PLUGINS -> R.drawable.msg_plugins
            SuperGramUiConfig.SG_UI_LAYOUT -> R.drawable.msg_list
            SuperGramUiConfig.SG_OTHER -> R.drawable.msg_fave
            else -> R.drawable.msg_info
        }
        Mode.MAIN_SETTINGS -> SuperGramUiConfig.getMainSettingsItemIcon(key)
    }

    private fun onReorder(sectionId: Int, uItems: ArrayList<UItem>) {
        if (sectionId != 0) return
        val reordered = ArrayList<String>()
        for (item in uItems) {
            val key = idToKey[item.id] ?: continue
            reordered.add(key)
        }
        if (reordered.isNotEmpty()) {
            activeKeys.clear()
            activeKeys.addAll(reordered)
            saveToConfig()
        }
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id == ADD_DIVIDER_ID) {
            val divKey = SuperGramUiConfig.nextDividerKey(activeKeys)
            activeKeys.add(divKey)
            saveToConfig()
            listView.adapter?.update(true)
            return
        }
        val key = idToKey[item.id] ?: return
        if (SuperGramUiConfig.isDivider(key)) {
            activeKeys.remove(key)
            saveToConfig()
            listView.adapter?.update(true)
            return
        }
        // Prevent hiding the flexible UI customizer itself or SuperGram entry from main settings accidentally
        if (key == SuperGramUiConfig.SG_UI_LAYOUT || key == SuperGramUiConfig.MAIN_SUPERGRAM) {
            BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.info, getString(R.string.UiLayoutHelp))
                .show()
            return
        }
        if (activeKeys.contains(key)) {
            activeKeys.remove(key)
            if (!hiddenKeys.contains(key)) hiddenKeys.add(key)
        } else if (hiddenKeys.contains(key)) {
            hiddenKeys.remove(key)
            activeKeys.add(key)
        }
        saveToConfig()
        listView.adapter?.update(true)
    }

    companion object {
        private const val ADD_DIVIDER_ID = -200
    }
}
