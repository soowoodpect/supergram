/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @exteraGram, 2022-2026

 Licensed under the GNU GPL v. 3 or later, with the additional terms
 in ADDITIONAL_TERMS.md at the root of the repository.

*/

package com.exteragram.messenger.plugins.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.provider.OpenableColumns
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.exteragram.messenger.editor
import com.exteragram.messenger.pinnedPlugins
import com.exteragram.messenger.plugins.Plugin
import com.exteragram.messenger.plugins.PluginsConstants
import com.exteragram.messenger.plugins.PluginsController
import com.exteragram.messenger.plugins.catalog.ui.PluginRepositoriesActivity
import com.exteragram.messenger.plugins.ui.components.EmptyPluginsView
import com.exteragram.messenger.plugins.ui.components.PluginCell
import com.exteragram.messenger.plugins.ui.components.PluginCellDelegate
import com.exteragram.messenger.plugins.utils.PluginsWatchdog
import com.exteragram.messenger.pluginsEngine
import com.exteragram.messenger.preferences.BasePreferencesActivity
import com.exteragram.messenger.utils.text.LocaleUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.ColoredImageSpan
import org.telegram.ui.Components.CubicBezierInterpolator
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.ScaleStateListAnimator
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl
import org.telegram.ui.ProfileActivity
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import org.telegram.utils.glass.source.GlassSourceColor
import android.widget.FrameLayout
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import java.io.File
import java.io.FileOutputStream

class PluginsActivity : BasePreferencesActivity(), NotificationCenter.NotificationCenterDelegate {
    private var selectedTab = TAB_EXTERA
    private var exteraTabView: TextView? = null
    private var superGramTabView: TextView? = null
    private var importItem: ActionBarMenuItem? = null

    private var searching = false
    private var query: String? = null
    private var searchItem: ActionBarMenuItem? = null
    private var infoItem: ActionBarMenuItem? = null
    private var emptyView: EmptyPluginsView? = null
    private var isSwitchingEngineState = false
    private var catalogButton: ProfileActivity.Button2? = null
    private var catalogButtonScrollShown = true
    private var catalogButtonInset = 0
    private val catalogButtonVisibility = BoolAnimator(
        0,
        FactorAnimator.Target { _, _, _, _ -> updateCatalogButtonPosition() },
        CubicBezierInterpolator.EASE_OUT_QUINT,
        380,
        true,
    )

    override fun createView(context: Context): View {
        val contentView = super.createView(context)

        searchItem = actionBar.menu
            .addItem(0, R.drawable.outline_header_search)
            .setIsSearchField(true)
            .setActionBarMenuItemSearchListener(object :
                ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                override fun onSearchExpand() {
                    searching = true
                    setPinnedHeaderVisible(false, true)
                    listView.adapter.update(true)
                    listView.scrollToPosition(0)
                    infoItem?.visibility = View.GONE
                    importItem?.visibility = View.GONE
                    updateCatalogButton(true)
                }

                override fun onSearchCollapse() {
                    searching = false
                    query = null
                    setPinnedHeaderVisible(pluginsEngine, true)
                    listView.adapter.update(true)
                    listView.scrollToPosition(0)
                    infoItem?.visibility = View.VISIBLE
                    importItem?.visibility = View.VISIBLE
                    catalogButtonScrollShown = true
                    updateCatalogButton(true)
                }

                override fun onTextChanged(editText: EditText) {
                    query = editText.text.toString()
                    listView.adapter.update(true)
                    listView.scrollToPosition(0)
                }
            })
        searchItem?.setSearchPaddingStart(7)
        searchItem?.setSearchFieldHint(LocaleController.getString(R.string.Search))
        AndroidUtilities.updateViewVisibilityAnimated(
            searchItem,
            pluginsEngine && PluginsController.getInstance().plugins.isNotEmpty(),
            0.5f,
            false,
        )

        infoItem = actionBar.menu.addItem(1, R.drawable.msg_info)
        infoItem?.setOnClickListener { presentFragment(PluginsInfoActivity()) }

        importItem = actionBar.menu.addItem(2, R.drawable.msg_add)
        importItem?.contentDescription = LocaleController.getString(R.string.ImportPluginFile)
        importItem?.setOnClickListener { openFileImporter() }

        setPinnedHeaderVisible(pluginsEngine && !searching, false)

        val catalogText = SpannableStringBuilder("C " + LocaleController.getString(R.string.PluginCatalog)).apply {
            setSpan(ColoredImageSpan(R.drawable.settings_business), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val blurSource = GlassSourceColor().apply {
            color = getThemedColor(Theme.key_windowBackgroundWhite)
        }
        catalogButton = ProfileActivity.Button2(context).apply {
            background = BlurredBackgroundDrawableViewFactory(blurSource)
                .create(this, BlurredBackgroundColorProviderThemed(resourceProvider, Theme.key_windowBackgroundWhite))
                .setPadding(dp(8f))
                .setRadius(dp(22f).toFloat())
            ScaleStateListAnimator.apply(this, .02f, 1.2f)
            addView(
                ButtonWithCounterView(context, resourceProvider).apply {
                    setUseWrapContent(true)
                    setPadding(dp(16f), 0, dp(16f), 0)
                    setRoundRadius(19)
                    setText(catalogText, false)
                    stateListAnimator = null
                },
                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER),
            )
            setOnClickListener { openCatalog() }
        }
        (contentView as? FrameLayout)?.addView(
            catalogButton,
            LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, CATALOG_BUTTON_HEIGHT.toInt(), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        )
        updateCatalogButton(false)
        updateCatalogButtonPosition()

        listView.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(parentActivity.currentFocus)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) {
                    catalogButtonScrollShown = dy < 0
                }
                updateCatalogButton(true)
            }
        })

        fragmentView = contentView
        return contentView
    }

    override fun getTitle(): String = LocaleController.getString(R.string.Plugins)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        if (!searching) {
            items.add(
                UItem.asRippleCheck(
                    0,
                    LocaleController.getString(R.string.EnablePluginsEngine)
                ).setChecked(pluginsEngine)
            )
        }
        if (!pluginsEngine) {
            return
        }

        val plugins = HashMap(PluginsController.getInstance().plugins)
        plugins.entries.removeIf { (_, plugin) ->
            val isSuper = plugin.isSuperGramPlugin() || PluginsController.isSuperGramPlugin(plugin.getId())
            if (selectedTab == TAB_SUPERGRAM) !isSuper else isSuper
        }

        var space = UItem.asSpace(AndroidUtilities.dp(8f))
        space.transparent = true
        items.add(space)

        if (searching && !TextUtils.isEmpty(query)) {
            plugins.values.removeIf { plugin ->
                !plugin.getName().lowercase().contains(query!!.lowercase())
            }
        }

        if (plugins.isEmpty()) {
            if (emptyView == null) {
                emptyView = EmptyPluginsView(context, resourceProvider)
            }
            val localEmptyView = emptyView ?: return

            if (searching) {
                if (localEmptyView.tag == null || localEmptyView.tag as Int != 1) {
                    MediaDataController.getInstance(UserConfig.selectedAccount)
                        .setPlaceholderImage(
                            localEmptyView.backupImageView,
                            "AnimatedEmojies",
                            "\uD83D\uDD0E",
                            "100_100"
                        )
                    localEmptyView.setTitle(null)
                    localEmptyView.setText(LocaleController.getString(R.string.PluginsNotFound))
                    localEmptyView.setButton(null, null)
                    localEmptyView.tag = 1
                }
            } else if (selectedTab == TAB_SUPERGRAM) {
                if (localEmptyView.tag == null || localEmptyView.tag as Int != 3) {
                    MediaDataController.getInstance(UserConfig.selectedAccount)
                        .setPlaceholderImage(
                            localEmptyView.backupImageView,
                            "AnimatedEmojies",
                            "\uD83E\uDDE9",
                            "100_100"
                        )
                    localEmptyView.setTitle(LocaleController.getString(R.string.SuperGramPluginsEmptyTitle))
                    localEmptyView.setText(LocaleController.getString(R.string.SuperGramPluginsEmptyInfo))
                    localEmptyView.setButton(LocaleController.getString(R.string.ImportPluginFile), ::openFileImporter)
                    localEmptyView.tag = 3
                }
            } else {
                if (localEmptyView.tag == null || localEmptyView.tag as Int != 2) {
                    MediaDataController.getInstance(UserConfig.selectedAccount)
                        .setPlaceholderImage(
                            localEmptyView.backupImageView,
                            "AnimatedEmojies",
                            "\uD83D\uDCC2",
                            "100_100"
                        )
                    localEmptyView.setTitle(LocaleController.getString(R.string.PluginsEmptyTitle))
                    localEmptyView.setText(LocaleUtils.formatWithUsernames(LocaleController.getString(R.string.PluginsEmptyInfo)))
                    localEmptyView.setButton(LocaleController.getString(R.string.PluginCatalogOpen), ::openCatalog)
                    localEmptyView.tag = 2
                }
            }

            items.add(
                UItem.asFullscreenCustom(
                    localEmptyView,
                    AndroidUtilities.dp((74 * if (searching) 2 else 1).toFloat()),
                    true
                ).setTransparent(true)
            )
        } else {
            if (pinnedPlugins.isNotEmpty()) {
                for (id in pinnedPlugins) {
                    if (plugins.containsKey(id)) {
                        val plugin = plugins[id] ?: continue
                        items.add(createPluginItem(plugin))
                        items.add(UItem.asSpace(AndroidUtilities.dp(8f)))
                    }
                }
            }

            val pluginsList = ArrayList(plugins.values)
            pluginsList.sortWith(Comparator.comparing(Plugin::getName))

            for (plugin in pluginsList) {
                if (PluginsController.isPluginPinned(plugin.getId())) {
                    continue
                }
                items.add(createPluginItem(plugin))
                items.add(UItem.asSpace(AndroidUtilities.dp(8f)))
            }
        }

        space = UItem.asSpace(AndroidUtilities.dp(if (searching || plugins.isEmpty()) 4f else CATALOG_BUTTON_HEIGHT - 8f))
        space.transparent = true
        items.add(space)
    }

    private fun createPluginItem(plugin: Plugin): UItem {
        return PluginCell.Factory.asPlugin(plugin, object : PluginCellDelegate {
            override fun sharePlugin() {
                PluginsController.getInstance().getPluginEngine(plugin.getId())
                    ?.sharePlugin(plugin.getId())
            }

            override fun openInExternalApp() {
                PluginsController.getInstance().getPluginEngine(plugin.getId())
                    ?.openInExternalApp(plugin.getId())
            }

            override fun deletePlugin() {
                if (plugin.isNotResponding()) {
                    PluginsWatchdog.showNotRespondingAlert(plugin)
                    return
                }

                val builder = AlertDialog.Builder(parentActivity, resourceProvider)
                    .setTitle(LocaleController.getString(R.string.PluginDelete))
                    .setMessage(
                        AndroidUtilities.replaceTags(
                            LocaleController.formatString(
                                R.string.PluginDeleteInfo,
                                plugin.getName()
                            )
                        )
                    )
                    .setPositiveButton(LocaleController.getString(R.string.Delete)) { _: DialogInterface?, _: Int ->
                        PluginsController.getInstance().deletePlugin(plugin.getId()) { error ->
                            AndroidUtilities.runOnUIThread {
                                if (fragmentView == null || parentActivity == null) {
                                    return@runOnUIThread
                                }
                                listView.adapter.update(true)
                                if (error != null) {
                                    BulletinFactory.of(this@PluginsActivity)
                                        .createSimpleBulletin(R.raw.error, error).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)

                val dialog = builder.create()
                dialog.show()
                val button = dialog.getButton(DialogInterface.BUTTON_POSITIVE) as? TextView
                button?.setTextColor(Theme.getColor(Theme.key_text_RedBold))
            }

            override fun togglePlugin(view: View) {
                if (plugin.isNotResponding()) {
                    PluginsWatchdog.showNotRespondingAlert(plugin)
                    return
                }

                val cell = view as PluginCell
                val targetValue = !plugin.isEnabled()
                PluginsController.getInstance()
                    .setPluginEnabled(plugin.getId(), targetValue) { error ->
                        AndroidUtilities.runOnUIThread {
                            if (fragmentView == null || parentActivity == null) {
                                return@runOnUIThread
                            }
                            if (error != null) {
                                BulletinFactory.of(this@PluginsActivity)
                                    .createSimpleBulletin(
                                        R.raw.error,
                                        LocaleController.formatString(
                                            if (targetValue) R.string.PluginEnableError else R.string.PluginDisableError,
                                            plugin.getName(),
                                        ),
                                        LocaleUtils.createCopySpan(this@PluginsActivity),
                                    ) {
                                        if (AndroidUtilities.addToClipboard(error)) {
                                            BulletinFactory.of(this@PluginsActivity)
                                                .createCopyBulletin(LocaleController.getString(R.string.TextCopied))
                                                .show()
                                        }
                                    }
                                    .show()
                            } else {
                                cell.setChecked(targetValue, true)
                            }
                        }
                    }
            }

            override fun openPluginSettings() {
                PluginsController.openPluginSettings(plugin.getId())
            }

            override fun pinPlugin(view: View) {
                val isPinned = PluginsController.isPluginPinned(plugin.getId())
                PluginsController.setPluginPinned(plugin.getId(), !isPinned)
                (view as PluginCell).setPinned(!isPinned)
                listView.adapter.update(true)
                listView.smoothScrollToPosition(0)
            }

            override fun canOpenInExternalApp(): Boolean {
                val engine = PluginsController.getInstance().getPluginEngine(plugin.getId())
                return engine != null && engine.canOpenInExternalApp()
            }
        })
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.viewType == UniversalAdapter.VIEW_TYPE_CHECKRIPPLE) {
            togglePluginsEngine(view, item)
        }
    }

    private fun togglePluginsEngine(view: View, item: UItem) {
        if (isSwitchingEngineState) {
            return
        }
        isSwitchingEngineState = true

        pluginsEngine = !pluginsEngine
        editor.putBoolean("pluginsEngine", pluginsEngine).apply()

        val textCheckCell = view as TextCheckCell
        textCheckCell.isChecked = pluginsEngine.also { item.checked = it }
        textCheckCell.setBackgroundColorAnimated(
            pluginsEngine,
            Theme.getColor(if (pluginsEngine) Theme.key_windowBackgroundChecked else Theme.key_windowBackgroundUnchecked),
        )

        val onDone = Runnable {
            AndroidUtilities.runOnUIThread {
                if (fragmentView == null || parentActivity == null) {
                    return@runOnUIThread
                }
                if (searching) {
                    actionBar.closeSearchField()
                }
                AndroidUtilities.updateViewVisibilityAnimated(
                    searchItem,
                    pluginsEngine && PluginsController.getInstance().plugins.isNotEmpty(),
                    0.5f,
                    true,
                )
                setPinnedHeaderVisible(pluginsEngine && !searching, true)
                updateCatalogButton(true)
                listView.adapter.update(true)
                isSwitchingEngineState = false
            }
        }

        if (pluginsEngine) {
            PluginsController.getInstance().init(onDone)
        } else {
            PluginsController.getInstance().shutdown(onDone)
        }
    }

    private fun hasCatalogButton(): Boolean =
        pluginsEngine && !searching && PluginsController.getInstance().plugins.isNotEmpty()

    private fun updateCatalogButton(animated: Boolean) {
        catalogButtonVisibility.setValue(
            hasCatalogButton() && (catalogButtonScrollShown || !listView.canScrollVertically(1)),
            animated,
        )
    }

    private fun updateCatalogButtonPosition() {
        val button = catalogButton ?: return
        val factor = catalogButtonVisibility.floatValue
        button.translationY = -catalogButtonInset + dp(CATALOG_BUTTON_HEIGHT) * (1f - factor)
        button.alpha = factor
        button.visibility = if (factor > 0f) View.VISIBLE else View.INVISIBLE
    }

    override fun onInsets(left: Int, top: Int, right: Int, bottom: Int) {
        catalogButtonInset = bottom
        updateCatalogButtonPosition()
        super.onInsets(left, top, right, bottom)
    }

    private fun openCatalog() {
        presentFragment(PluginRepositoriesActivity())
    }

    override fun onFragmentCreate(): Boolean {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginsUpdated)
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.reloadInterface)
        NotificationCenter.getGlobalInstance()
            .addObserver(this, NotificationCenter.pluginIsNotResponding)
        return super.onFragmentCreate()
    }

    override fun onFragmentDestroy() {
        NotificationCenter.getGlobalInstance()
            .removeObserver(this, NotificationCenter.pluginsUpdated)
        NotificationCenter.getGlobalInstance()
            .removeObserver(this, NotificationCenter.reloadInterface)
        NotificationCenter.getGlobalInstance()
            .removeObserver(this, NotificationCenter.pluginIsNotResponding)
        super.onFragmentDestroy()
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        when (id) {
            NotificationCenter.pluginsUpdated -> {
                AndroidUtilities.updateViewVisibilityAnimated(
                    searchItem,
                    pluginsEngine && PluginsController.getInstance().plugins.isNotEmpty(),
                    0.5f,
                    true,
                )
                setPinnedHeaderVisible(pluginsEngine && !searching, true)
                updateCatalogButton(true)
                listView.adapter.update(true)
            }

            NotificationCenter.reloadInterface -> listView.invalidateViews()
            NotificationCenter.pluginIsNotResponding -> listView.adapter.update(true)
        }
    }

    override fun onBackPressed(invoked: Boolean): Boolean {
        if (searching) {
            if (invoked) {
                actionBar.closeSearchField()
            }
            return false
        }
        return super.onBackPressed(invoked)
    }

    override fun createPinnedHeader(
        context: Context,
        glassFactory: BlurredBackgroundDrawableViewFactory
    ): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
            background = glassFactory
                .create(this, BlurredBackgroundProviderImpl.topPanelChatActivity(resourceProvider))
                .setRadius(dp(24f).toFloat())
        }

        fun makeTab(title: String, tabIndex: Int): TextView = TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f)
            typeface = AndroidUtilities.bold()
            setPadding(dp(12f), 0, dp(12f), 0)
            ScaleStateListAnimator.apply(this, 0.02f, 1.05f)
            setOnClickListener {
                if (selectedTab != tabIndex) {
                    selectedTab = tabIndex
                    updateTabStyles()
                    listView.adapter?.update(true)
                    updateCatalogButton(true)
                }
            }
        }

        exteraTabView = makeTab(LocaleController.getString(R.string.PluginsTabExtera), TAB_EXTERA)
        superGramTabView = makeTab(LocaleController.getString(R.string.PluginsTabSuperGram), TAB_SUPERGRAM)

        container.addView(exteraTabView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f))
        container.addView(superGramTabView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f))
        updateTabStyles()
        return container
    }

    private fun updateTabStyles() {
        val activeBgColor = ColorUtils.setAlphaComponent(getThemedColor(Theme.key_featuredStickers_addButton), 34)
        val activeTextColor = getThemedColor(Theme.key_featuredStickers_addButton)
        val inactiveTextColor = getThemedColor(Theme.key_windowBackgroundWhiteGrayText)

        listOf(exteraTabView to TAB_EXTERA, superGramTabView to TAB_SUPERGRAM).forEach { (view, idx) ->
            view ?: return@forEach
            val active = selectedTab == idx
            view.setTextColor(if (active) activeTextColor else inactiveTextColor)
            view.background = if (active) {
                Theme.createRoundRectDrawable(dp(20f), activeBgColor)
            } else {
                null
            }
        }
    }

    private fun openFileImporter() {
        val activity = parentActivity ?: return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, IMPORT_PLUGIN_REQUEST_CODE)
        } catch (t: Throwable) {
            FileLog.e(t)
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, t.message ?: "Error").show()
        }
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResultFragment(requestCode, resultCode, data)
        if (requestCode != IMPORT_PLUGIN_REQUEST_CODE || resultCode != Activity.RESULT_OK) {
            return
        }
        val uri = data?.data ?: return
        val activity = parentActivity ?: return
        try {
            var fileName = "plugin" + if (selectedTab == TAB_SUPERGRAM) PluginsConstants.SPLUG_EXT else PluginsConstants.PLUGINS_EXT
            activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val resolved = cursor.getString(idx)
                        if (!resolved.isNullOrBlank()) {
                            fileName = resolved
                        }
                    }
                }
            }
            val lower = fileName.lowercase()
            if (!lower.endsWith(PluginsConstants.PLUGINS_EXT) && !lower.endsWith(PluginsConstants.SPLUG_EXT)) {
                fileName += if (selectedTab == TAB_SUPERGRAM) PluginsConstants.SPLUG_EXT else PluginsConstants.PLUGINS_EXT
            }
            val tempDir = File(ApplicationLoader.getFilesDirFixed(), "temp").apply { mkdirs() }
            val tempFile = File(tempDir, fileName)
            activity.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return
            PluginsController.getInstance().showInstallDialog(this, tempFile.absolutePath, true)
        } catch (t: Throwable) {
            FileLog.e(t)
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, t.message ?: "Import failed").show()
        }
    }

    companion object {
        private const val CATALOG_BUTTON_HEIGHT = 60f
        private const val TAB_EXTERA = 0
        private const val TAB_SUPERGRAM = 1
        private const val IMPORT_PLUGIN_REQUEST_CODE = 4201
    }
}
