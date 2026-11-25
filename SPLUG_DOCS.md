# Документация по плагинам SuperGram (`.splug`)

## 1. Назначение и разделение экосистемы
В **SuperGram** плагины разделены на два типа:
- **Плагины exteraGram (`.plugin`)** — для функциональных возможностей (утилиты, фильтры, перехваты сообщений и API). Доступ к `supergram_ui` для `.plugin` заблокирован (`PermissionError` / `SecurityException`).
- **Плагины SuperGram (`.splug`)** — **для полной кастомизации всего клиента**: цвета, системы дизайна, подмена любых иконок (ресурсы и файлы PNG/WEBP/SVG), расположение кнопок (FAB, нижняя панель, вкладки, боковое меню, настройки), высота/заголовок ActionBar, кастомные шрифты `.ttf`/`.otf` и стиль чатов через эксклюзивный модуль **`supergram_ui`**.

При выключении, выгрузке или удалении `.splug`-плагина его временные оверлеи цветов, рамок, иконок, расположения и шрифтов **автоматически снимаются**, а интерфейс сразу перерисовывается.

---

## 2. Метаданные `.splug`

```python
__id__ = "my_custom_client"       # 2-32 символа, латиница/цифры/-/_ (с буквы)
__name__ = "My Custom Client"     # Отображаемое название
__description__ = "Описание..."   # Описание
__author__ = "Author"             # Автор
__version__ = "1.0.0"             # Версия
__supergram__ = True              # Для файлов .splug ставится автоматически
__app_version__ = ">=13.2.2"      # Версия SuperGram
__sdk_version__ = ">=1.4.6.7"     # Версия SDK (с полной кастомизацией)
```

---

## 3. Эксклюзивный модуль `supergram_ui`

```python
import supergram_ui
```

### 3.1. Дизайн-системы и карточки
- `set_design_style(style, refresh=True)` / `get_design_style()` — `"material_you"`, `"miuix"`, `"windows_11"`, `"ios"`.
- `set_card_style(radius=None, padding=None, border=None, separated_headers=None, divider=None, center_title=None, refresh=True)`
- `set_card_border_style(light_color=None, dark_color=None, width=None, refresh=True)`

### 3.2. Палитры и цвета
- `set_palette(accent=None, background=None, surface=None, text=None, secondary_text=None, divider=None, action_bar=None, bubble_out=None, bubble_in=None, dark=None, refresh=True)`
- `set_color(theme_key, light=None, dark=None, refresh=True)` — любой ключ `Theme.key_*`.
- `clear_theme(refresh=True)` — снять все оверлеи плагина.

### 3.3. Иконки всего клиента и иконка приложения
- `set_icon_pack(pack, refresh=True)` — переключить базовый набор иконок (`"default"`, `"solar"`, `"remix"`) или ID установленного `.icons`-пака.
- `set_app_icon(icon_id)` — сменить иконку приложения в лаунчере (`"default"`, `"winter"`, `"monet"`, `"orbit"` и др.).
- `replace_icon(target_name, replacement_name, refresh=True)` — подменить любой `R.drawable.<target_name>` на другой системный drawable (например, `replace_icon("msg_settings", "settings_business")`). `None` сбрасывает замену.
- `set_icon_file(target_name, file_path, refresh=True)` — заменить любой `R.drawable.<target_name>` на файл **PNG / WEBP / JPG / SVG** с диска. `None` сбрасывает.
- `clear_icons(refresh=True)` — сбросить все замены иконок плагина.

### 3.4. Расположение кнопок, панелей и меню
- **Плавающая кнопка (FAB):**
  - `set_fab_layout(align=None, bottom_margin=None, side_margin=None, hide=None, square=None, new_style=None, refresh=True)`
  - `align`: `"right"` (по умолчанию), `"left"`, `"center"`.
- **Нижняя панель, вкладки и боковое меню (Drawer):**
  - `set_navigation_bar(mode=None, new_style=None, hide_labels=None, drawer_enabled=None, immersive_drawer=None, tab_icons=None, tab_counters=None, hide_all_chats=None, show_feed=None, refresh=True)`
    - `mode`: `"show"` | `"hide"` | `"floating"`.
    - `tab_icons`: `"icons_and_titles"` | `"titles_only"` | `"icons_only"`.
    - `tab_counters`: `"all"` | `"unmuted"` | `"hidden"`.
  - `set_tabs_layout(order=None, hidden=None, refresh=True)` — переставить или скрыть нижние вкладки (`"chats"`, `"feed"`, `"contacts"`, `"settings"`, `"calls"`, `"profile"`).
  - `get_drawer_menu()` / `set_drawer_menu(items, hidden=None, refresh=True)` — порядок и скрытие пунктов бокового меню (`"profile"`, `"archive"`, `"bots"`, `"new_group"`, `"contacts"`, `"new_channel"`, `"calls"`, `"saved"`, `"settings"`, `"plugins"`, `"browser"`, `"qr"`, `"feed"`, `"wallet"`, `"divider"`).
- **ActionBar и поиск:**
  - `set_action_bar(height_dp=None, center_title=None, hide_status=None, title_mode=None, hide_search_bar=None, new_chat_header=None, refresh=True)`
- **Списки настроек:**
  - `get_layout(target="main_settings")` / `set_layout(target, items, hidden=None, refresh=True)` / `reset_layouts(refresh=True)` (`"main_settings"` | `"supergram"`).

### 3.5. Шрифты и стиль чатов
- `set_font(regular_path=None, bold_path=None, mono_path=None, refresh=True)` — глобальная замена шрифтов клиента на `.ttf`/`.otf`.
- `set_chat_style(avatar_corners=None, sticker_size=None, sticker_shape=None, reply_colors=None, reply_emoji=None, reply_bg=None, wide_posts_feed=None, wide_posts_channels=None, hide_stories=None, new_player=None, new_profile_actions=None, glass_message_menu=None, force_snow=None, refresh=True)`
- `refresh(rebuild_all=False)` — перерисовать интерфейс (`rebuild_all=True` перестраивает все экраны целиком, нужно после смены иконок, вкладок, шрифтов и расположения FAB).

---

## 4. Пример `.splug` с полной кастомизацией
Смотри готовый плагин [CyberMintTheme.splug](file:///mnt/win/SuperGram/CyberMintTheme.splug) — в нём можно переключать пресеты оформления, пакет иконок (`Default`/`Solar`/`Remix`), расположение FAB (`Справа`/`Слева`/`По центру`) и порядок нижних вкладок.
