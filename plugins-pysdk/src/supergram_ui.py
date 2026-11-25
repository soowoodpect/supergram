# SuperGram Whole-Client Customization API (exclusive to .splug plugins)

import _sdk_version
_sdk_version.Z.c()

import sys
from typing import Any, Dict, List, Optional, Union
from com.exteragram.messenger.plugins import SuperGramPluginBridge
from extera_utils.get_caller import _plugin_id_from_location
from java.lang import Boolean as JBoolean, Float as JFloat, Integer as JInteger, String as JString
from java.util import ArrayList as JArrayList


def _resolve_caller_plugin_id(plugin_id: Optional[str] = None) -> str:
    pid = plugin_id
    if not pid:
        frame = sys._getframe(1)
        this_file = __file__
        while frame is not None:
            fn = str(frame.f_code.co_filename)
            mod = str(frame.f_globals.get("__name__", ""))
            if fn != this_file and mod != __name__ and not fn.endswith("supergram_ui.py") and not fn.endswith("supergram_ui.pyc"):
                candidate = _plugin_id_from_location(fn, mod)
                if candidate and SuperGramPluginBridge.isSuperGramPlugin(candidate):
                    pid = candidate
                    break
                if not pid and candidate and (
                    "plugins" in fn.replace("\\", "/") or mod.startswith("ElyxPlugins")
                ):
                    pid = candidate
            frame = frame.f_back
    if not pid:
        raise PermissionError("Unable to identify caller plugin; supergram_ui is restricted to .splug plugins.")
    if not SuperGramPluginBridge.isSuperGramPlugin(pid):
        raise PermissionError(
            f"Plugin '{pid}' is a standard .plugin. Client customization via supergram_ui is restricted to .splug plugins."
        )
    return pid


def _parse_color(color: Union[int, str, None]) -> Optional[int]:
    if color is None:
        return None
    if isinstance(color, int):
        val = color & 0xFFFFFFFF
        if val >= 0x80000000:
            val -= 0x100000000
        return int(val)
    if isinstance(color, str):
        s = color.strip().lstrip("#")
        if len(s) == 6:
            s = "FF" + s
        if len(s) != 8:
            raise ValueError(f"Invalid hex color '{color}'. Use '#RRGGBB' or '#AARRGGBB'.")
        val = int(s, 16)
        if val >= 0x80000000:
            val -= 0x100000000
        return val
    raise TypeError(f"Unsupported color type: {type(color).__name__}")


def _to_jint(val: Optional[int]):
    return None if val is None else JInteger(int(val))


def _to_jfloat(val: Optional[float]):
    return None if val is None else JFloat(float(val))


def _to_jbool(val: Optional[bool]):
    return None if val is None else JBoolean(bool(val))


def _to_jstring_list(items: Optional[List[str]]):
    if items is None:
        return None
    arr = JArrayList()
    for x in items:
        arr.add(str(x))
    return arr


def is_allowed(plugin_id: Optional[str] = None) -> bool:
    if not plugin_id:
        try:
            _resolve_caller_plugin_id()
            return True
        except PermissionError:
            return False
    return bool(SuperGramPluginBridge.isSuperGramPlugin(plugin_id))


def get_design_style(plugin_id: Optional[str] = None) -> str:
    pid = _resolve_caller_plugin_id(plugin_id)
    return str(SuperGramPluginBridge.getDesignStyle(pid))


def set_design_style(style: str, refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    """Switch client design system: 'material_you', 'miuix', 'windows_11', or 'ios'."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setDesignStyle(pid, str(style), bool(refresh))


def get_card_style(plugin_id: Optional[str] = None) -> Dict[str, Any]:
    pid = _resolve_caller_plugin_id(plugin_id)
    raw = SuperGramPluginBridge.getCardStyle(pid)
    return {str(k): raw.get(k) for k in raw.keySet().toArray()}


def set_card_style(
    radius: Optional[float] = None,
    padding: Optional[int] = None,
    border: Optional[bool] = None,
    separated_headers: Optional[bool] = None,
    divider: Optional[str] = None,
    center_title: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Configure card radius (dp), side padding (dp or -1=auto), border, divider ('line'|'segments'|'hidden'), and centered ActionBar title."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setCardStyle(
        pid,
        _to_jfloat(radius),
        _to_jint(padding),
        _to_jbool(border),
        _to_jbool(separated_headers),
        None if divider is None else str(divider),
        _to_jbool(center_title),
        bool(refresh),
    )


def set_card_border_style(
    light_color: Union[int, str, None] = None,
    dark_color: Union[int, str, None] = None,
    width: Optional[float] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Set custom card border color (light/dark) and stroke width in dp."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setCardBorderStyle(
        pid,
        _to_jint(_parse_color(light_color)),
        _to_jint(_parse_color(dark_color)),
        _to_jfloat(width),
        bool(refresh),
    )


def set_color(
    theme_key: Union[str, int],
    light: Union[int, str, None] = None,
    dark: Union[int, str, None] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Override a Telegram Theme color key for light and/or dark mode."""
    pid = _resolve_caller_plugin_id(plugin_id)
    key_arg = JInteger(theme_key) if isinstance(theme_key, int) else JString(str(theme_key))
    SuperGramPluginBridge.setColor(
        pid,
        key_arg,
        _to_jint(_parse_color(light)),
        _to_jint(_parse_color(dark)),
        bool(refresh),
    )


def set_palette(
    accent: Union[int, str, None] = None,
    background: Union[int, str, None] = None,
    surface: Union[int, str, None] = None,
    text: Union[int, str, None] = None,
    secondary_text: Union[int, str, None] = None,
    divider: Union[int, str, None] = None,
    action_bar: Union[int, str, None] = None,
    bubble_out: Union[int, str, None] = None,
    bubble_in: Union[int, str, None] = None,
    dark: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Apply a cohesive palette across main UI surfaces, accents, bubbles, and text."""
    pid = _resolve_caller_plugin_id(plugin_id)

    def _apply(keys, color_val):
        if color_val is None:
            return
        parsed = _to_jint(_parse_color(color_val))
        l_col = None if dark is True else parsed
        d_col = None if dark is False else parsed
        for k in keys:
            SuperGramPluginBridge.setColor(pid, JString(k), l_col, d_col, False)

    _apply([
        "windowBackgroundWhiteBlueHeader",
        "windowBackgroundWhiteBlueText",
        "windowBackgroundWhiteBlueText4",
        "windowBackgroundWhiteBlueButton",
        "switchTrackChecked",
        "featuredStickers_addButton",
        "chats_actionBackground",
        "checkboxCheck",
        "dialogTextBlue2",
        "radioBackgroundChecked",
    ], accent)
    _apply(["windowBackgroundGray", "windowBackgroundGrayShadow"], background)
    _apply(["windowBackgroundWhite", "dialogBackground", "chats_menuBackground"], surface)
    _apply(["windowBackgroundWhiteBlackText", "dialogTextBlack"], text)
    _apply(["windowBackgroundWhiteGrayText", "windowBackgroundWhiteGrayText2", "dialogTextGray"], secondary_text)
    _apply(["divider"], divider)
    _apply(["actionBarDefault"], action_bar)
    _apply(["chat_outBubble", "chat_outBubbleSelected"], bubble_out)
    _apply(["chat_inBubble", "chat_inBubbleSelected"], bubble_in)

    if refresh:
        SuperGramPluginBridge.refreshUi(pid, False)


def clear_theme(refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    """Remove all theme, icon, placement and font overrides registered by this plugin."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.clearTheme(pid, bool(refresh))


def set_icon_pack(pack: str, refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    """Switch base icon pack ('default', 'solar', 'remix') or an installed custom pack ID."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setIconPack(pid, str(pack), bool(refresh))


def set_app_icon(icon_id: str, plugin_id: Optional[str] = None) -> None:
    """Switch launcher icon alias (e.g. 'default', 'winter', 'monet', 'orbit', ...)."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setAppIcon(pid, str(icon_id))


def replace_icon(target_name: str, replacement_name: Optional[str], refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    """Remap any client R.drawable.<target_name> to R.drawable.<replacement_name> (None resets)."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.replaceIcon(pid, str(target_name), None if replacement_name is None else str(replacement_name), bool(refresh))


def set_icon_file(target_name: str, file_path: Optional[str], refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    """Replace any R.drawable.<target_name> with a PNG/WEBP/JPG/SVG file path on disk (None resets)."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setIconFile(pid, str(target_name), None if file_path is None else str(file_path), bool(refresh))


def clear_icons(refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.clearIcons(pid, bool(refresh))


def set_fab_layout(
    align: Optional[str] = None,
    bottom_margin: Optional[int] = None,
    side_margin: Optional[int] = None,
    hide: Optional[bool] = None,
    square: Optional[bool] = None,
    new_style: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Customize Floating Action Button alignment ('right'|'left'|'center'), margins (dp), shape and visibility."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setFabLayout(
        pid,
        None if align is None else str(align),
        _to_jint(bottom_margin),
        _to_jint(side_margin),
        _to_jbool(hide),
        _to_jbool(square),
        _to_jbool(new_style),
        bool(refresh),
    )


def set_action_bar(
    height_dp: Optional[int] = None,
    center_title: Optional[bool] = None,
    hide_status: Optional[bool] = None,
    title_mode: Optional[int] = None,
    hide_search_bar: Optional[bool] = None,
    new_chat_header: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Customize ActionBar height (40..84 dp or -1 auto), centered title, status hiding, search bar and title text mode."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setActionBar(
        pid,
        _to_jint(height_dp),
        _to_jbool(center_title),
        _to_jbool(hide_status),
        _to_jint(title_mode),
        _to_jbool(hide_search_bar),
        _to_jbool(new_chat_header),
        bool(refresh),
    )


def set_navigation_bar(
    mode: Optional[str] = None,
    new_style: Optional[bool] = None,
    hide_labels: Optional[bool] = None,
    drawer_enabled: Optional[bool] = None,
    immersive_drawer: Optional[bool] = None,
    tab_icons: Optional[str] = None,
    tab_counters: Optional[str] = None,
    hide_all_chats: Optional[bool] = None,
    show_feed: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Configure bottom navigation bar ('show'|'hide'|'floating'), M3 style, labels, drawer vs tabs, and folder tabs."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setNavigationBar(
        pid,
        None if mode is None else str(mode),
        _to_jbool(new_style),
        _to_jbool(hide_labels),
        _to_jbool(drawer_enabled),
        _to_jbool(immersive_drawer),
        None if tab_icons is None else str(tab_icons),
        None if tab_counters is None else str(tab_counters),
        _to_jbool(hide_all_chats),
        _to_jbool(show_feed),
        bool(refresh),
    )


def set_tabs_layout(
    order: Optional[List[str]] = None,
    hidden: Optional[List[str]] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Reorder or hide bottom navigation tabs ('chats', 'feed', 'contacts', 'settings', 'calls', 'profile')."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setTabsLayout(pid, _to_jstring_list(order), _to_jstring_list(hidden), bool(refresh))


def get_drawer_menu(plugin_id: Optional[str] = None) -> Dict[str, List[str]]:
    pid = _resolve_caller_plugin_id(plugin_id)
    raw = SuperGramPluginBridge.getDrawerMenu(pid)
    items_raw = raw.get("items")
    hidden_raw = raw.get("hidden")
    return {
        "items": [str(x) for x in items_raw.toArray()] if items_raw else [],
        "hidden": [str(x) for x in hidden_raw.toArray()] if hidden_raw else [],
    }


def set_drawer_menu(
    items: List[str],
    hidden: Optional[List[str]] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Reorder/hide drawer items ('profile','archive','bots','new_group','contacts','new_channel','calls','saved','settings','plugins','browser','qr','feed','wallet','divider')."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setDrawerMenu(pid, _to_jstring_list(items), _to_jstring_list(hidden), bool(refresh))


def get_layout(target: str = "main_settings", plugin_id: Optional[str] = None) -> Dict[str, List[str]]:
    """Read tile order and hidden keys for 'supergram' or 'main_settings'."""
    pid = _resolve_caller_plugin_id(plugin_id)
    raw = SuperGramPluginBridge.getLayout(pid, str(target))
    items_raw = raw.get("items")
    hidden_raw = raw.get("hidden")
    return {
        "items": [str(x) for x in items_raw.toArray()] if items_raw else [],
        "hidden": [str(x) for x in hidden_raw.toArray()] if hidden_raw else [],
    }


def set_layout(
    target: str,
    items: List[str],
    hidden: Optional[List[str]] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Reorder/hide tiles and place dividers ('div_1', ...) in 'supergram' or 'main_settings'."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setLayout(pid, str(target), _to_jstring_list(items), _to_jstring_list(hidden), bool(refresh))


def reset_layouts(refresh: bool = True, plugin_id: Optional[str] = None) -> None:
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.resetLayouts(pid, bool(refresh))


def set_font(
    regular_path: Optional[str] = None,
    bold_path: Optional[str] = None,
    mono_path: Optional[str] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Override Telegram fonts client-wide with .ttf/.otf file paths (None restores defaults)."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setFont(
        pid,
        None if regular_path is None else str(regular_path),
        None if bold_path is None else str(bold_path),
        None if mono_path is None else str(mono_path),
        bool(refresh),
    )


def set_chat_style(
    avatar_corners: Optional[float] = None,
    sticker_size: Optional[float] = None,
    sticker_shape: Optional[int] = None,
    reply_colors: Optional[bool] = None,
    reply_emoji: Optional[bool] = None,
    reply_bg: Optional[bool] = None,
    wide_posts_feed: Optional[bool] = None,
    wide_posts_channels: Optional[bool] = None,
    hide_stories: Optional[bool] = None,
    new_player: Optional[bool] = None,
    new_profile_actions: Optional[bool] = None,
    glass_message_menu: Optional[bool] = None,
    force_snow: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setChatStyle(
        pid,
        _to_jfloat(avatar_corners),
        _to_jfloat(sticker_size),
        _to_jint(sticker_shape),
        _to_jbool(reply_colors),
        _to_jbool(reply_emoji),
        _to_jbool(reply_bg),
        _to_jbool(wide_posts_feed),
        _to_jbool(wide_posts_channels),
        _to_jbool(hide_stories),
        _to_jbool(new_player),
        _to_jbool(new_profile_actions),
        _to_jbool(glass_message_menu),
        _to_jbool(force_snow),
        bool(refresh),
    )


def set_ui_tweaks(
    avatar_corners: Optional[float] = None,
    tab_icons: Optional[str] = None,
    hide_stories: Optional[bool] = None,
    hide_fab: Optional[bool] = None,
    square_fab: Optional[bool] = None,
    force_snow: Optional[bool] = None,
    refresh: bool = True,
    plugin_id: Optional[str] = None,
) -> None:
    """Quick shortcut for avatar corners, tab icons, stories, FAB, snow."""
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.setUiTweaks(
        pid,
        _to_jfloat(avatar_corners),
        None if tab_icons is None else str(tab_icons),
        _to_jbool(hide_stories),
        _to_jbool(hide_fab),
        _to_jbool(square_fab),
        _to_jbool(force_snow),
        bool(refresh),
    )


def refresh(rebuild_all: bool = False, plugin_id: Optional[str] = None) -> None:
    pid = _resolve_caller_plugin_id(plugin_id)
    SuperGramPluginBridge.refreshUi(pid, bool(rebuild_all))
