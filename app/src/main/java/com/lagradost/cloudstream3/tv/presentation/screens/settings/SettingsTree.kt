package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.buildExtensionsSettingsTree

/**
 * Builds the settings tree with placeholder data.
 * Each SettingsNode can have unlimited depth - nodes with children navigate deeper,
 * nodes with content show the leaf widget.
 */
@Composable
fun buildSettingsTree(
    extensionsViewModel: ExtensionsViewModel? = null
): List<SettingsNode> {
    val extensionIcon = ImageVector.vectorResource(id = R.drawable.ic_baseline_extension_24)

    android.util.Log.d("SettingsTree", "buildSettingsTree() called")

    // Level 1 titles from string resources
    val generalTitle = stringResource(R.string.category_general)
    val playerTitle = stringResource(R.string.category_player)
    val providersTitle = stringResource(R.string.category_providers)
    val uiTitle = stringResource(R.string.category_ui)
    val updatesTitle = stringResource(R.string.category_updates)
    val accountTitle = stringResource(R.string.category_account)
    val extensionsTitle = stringResource(R.string.extensions)

    return listOf(
        // General
        SettingsNode(
            id = "general",
            title = generalTitle,
            description = null,
            icon = Icons.Default.Settings,
            children = listOf(
                // Sekcje w Ogólne
                SettingsNode(
                    id = "general_language",
                    title = "Język",
                    description = "Ustawienia języka aplikacji",
                    children = listOf(
                        SettingsNode(
                            id = "general_language_app",
                            title = "Język aplikacji",
                            children = listOf(
                                SettingsNode("lang_polish", "Polski") { onBack ->
                                    LanguageSelector("Polski", onBack)
                                },
                                SettingsNode("lang_english", "English") { onBack ->
                                    LanguageSelector("English", onBack)
                                },
                                SettingsNode("lang_german", "Deutsch") { onBack ->
                                    LanguageSelector("Deutsch", onBack)
                                },
                                SettingsNode("lang_french", "Français") { onBack ->
                                    LanguageSelector("Français", onBack)
                                },
                                SettingsNode("lang_spanish", "Español") { onBack ->
                                    LanguageSelector("Español", onBack)
                                }
                            )
                        ),
                        SettingsNode(
                            id = "general_language_subtitles",
                            title = "Język napisów",
                            children = listOf(
                                SettingsNode("sub_polish", "Polski") { onBack ->
                                    LanguageSelector("Polski", onBack)
                                },
                                SettingsNode("sub_english", "English") { onBack ->
                                    LanguageSelector("English", onBack)
                                },
                                SettingsNode("sub_none", "Wyłączone") { onBack ->
                                    LanguageSelector("Wyłączone", onBack)
                                }
                            )
                        )
                    )
                ),
                SettingsNode(
                    id = "general_device",
                    title = "Urządzenie",
                    description = "Nazwa i identyfikacja urządzenia",
                    children = listOf(
                        SettingsNode("device_name", "Nazwa urządzenia") { onBack ->
                            DeviceNameEditor(onBack)
                        },
                        SettingsNode("device_id", "ID urządzenia") { onBack ->
                            DeviceIdViewer(onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "general_startup",
                    title = "Uruchamianie",
                    description = "Zachowanie przy starcie",
                    children = listOf(
                        SettingsNode("startup_autoplay", "Automatyczne odtwarzanie") { onBack ->
                            SwitchSettingWidget(
                                title = "Automatyczne odtwarzanie",
                                description = "Kontynuuj oglądanie ostatniego filmu po uruchomieniu",
                                onBack = onBack
                            )
                        },
                        SettingsNode("startup_remember", "Zapamiętaj pozycję") { onBack ->
                            SwitchSettingWidget(
                                title = "Zapamiętaj pozycję",
                                description = "Wznów odtwarzanie od miejsca przerwania",
                                onBack = onBack
                            )
                        }
                    )
                ),
                SettingsNode(
                    id = "general_storage",
                    title = "Pamięć",
                    description = "Zarządzanie pamięcią",
                    children = listOf(
                        SettingsNode("storage_cache", "Wyczyść pamięć podręczną") { onBack ->
                            ActionButton("Wyczyść pamięć podręczną", "Usuń wszystkie pliki tymczasowe", onBack)
                        },
                        SettingsNode("storage_data", "Wyczyść dane") { onBack ->
                            ActionButton("Wyczyść dane", "Usuń wszystkie dane aplikacji", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "general_network",
                    title = "Sieć",
                    description = "Ustawienia połączenia",
                    children = listOf(
                        SettingsNode("network_wifi", "Tylko WiFi") { onBack ->
                            SwitchSettingWidget(
                                title = "Tylko WiFi",
                                description = "Pobieraj tylko przez WiFi",
                                onBack = onBack
                            )
                        }
                    )
                ),
                SettingsNode(
                    id = "general_test1",
                    title = "Sieć",
                    description = "Ustawienia połączenia1",
                    children = listOf(
                        SettingsNode("network_wifi1", "Tylko WiFi") { onBack ->
                            SwitchSettingWidget(
                                title = "Tylko WiFi",
                                description = "Pobieraj tylko przez WiFi",
                                onBack = onBack
                            )
                        }
                    )
                ),
                SettingsNode(
                    id = "general_test2",
                    title = "Sieć",
                    description = "Ustawienia połączenia2",
                    children = listOf(
                        SettingsNode("network_wifi2", "Tylko WiFi") { onBack ->
                            SwitchSettingWidget(
                                title = "Tylko WiFi",
                                description = "Pobieraj tylko przez WiFi",
                                onBack = onBack
                            )
                        }
                    )
                )
            )
        ),

        // Player
        SettingsNode(
            id = "player",
            title = playerTitle,
            description = null,
            icon = Icons.Default.PlayArrow,
            children = listOf(
                SettingsNode(
                    id = "player_quality",
                    title = "Jakość",
                    description = "Ustawienia jakości wideo",
                    children = listOf(
                        SettingsNode(
                            id = "player_quality_streaming",
                            title = "Streaming",
                            children = listOf(
                                SettingsNode("quality_auto", "Automatyczna") { onBack ->
                                    QualitySelector("Automatyczna", onBack)
                                },
                                SettingsNode("quality_1080p", "1080p") { onBack ->
                                    QualitySelector("1080p", onBack)
                                },
                                SettingsNode("quality_720p", "720p") { onBack ->
                                    QualitySelector("720p", onBack)
                                },
                                SettingsNode("quality_480p", "480p") { onBack ->
                                    QualitySelector("480p", onBack)
                                },
                                SettingsNode("quality_360p", "360p") { onBack ->
                                    QualitySelector("360p", onBack)
                                }
                            )
                        ),
                        SettingsNode(
                            id = "player_quality_download",
                            title = "Pobieranie",
                            children = listOf(
                                SettingsNode("download_best", "Najlepsza jakość") { onBack ->
                                    QualitySelector("Najlepsza jakość", onBack)
                                },
                                SettingsNode("download_medium", "Średnia jakość") { onBack ->
                                    QualitySelector("Średnia jakość", onBack)
                                },
                                SettingsNode("download_low", "Niska jakość") { onBack ->
                                    QualitySelector("Niska jakość", onBack)
                                }
                            )
                        )
                    )
                ),
                SettingsNode(
                    id = "player_subtitles",
                    title = "Napisy",
                    description = "Ustawienia napisów",
                    children = listOf(
                        SettingsNode("subtitles_size", "Rozmiar napisów") { onBack ->
                            SubtitleSizeSelector(onBack)
                        },
                        SettingsNode("subtitles_color", "Kolor napisów") { onBack ->
                            SubtitleColorSelector(onBack)
                        },
                        SettingsNode("subtitles_background", "Tło napisów") { onBack ->
                            SwitchSettingWidget(
                                title = "Tło napisów",
                                description = "Wyświetl półprzezroczyste tło pod napisami",
                                onBack = onBack
                            )
                        }
                    )
                ),
                SettingsNode(
                    id = "player_controls",
                    title = "Sterowanie",
                    description = "Skróty i gesty",
                    children = listOf(
                        SettingsNode("controls_seek", "Przewijanie") { onBack ->
                            SeekDurationSelector(onBack)
                        },
                        SettingsNode("controls_gestures", "Gesty") { onBack ->
                            SwitchSettingWidget(
                                title = "Gesty dotykowe",
                                description = "Włącz sterowanie gestami",
                                onBack = onBack
                            )
                        }
                    )
                ),
                SettingsNode(
                    id = "player_advanced",
                    title = "Zaawansowane",
                    description = "Opcje zaawansowane",
                    children = listOf(
                        SettingsNode("advanced_decoder", "Dekoder") { onBack ->
                            DecoderSelector(onBack)
                        },
                        SettingsNode("advanced_buffer", "Buforowanie") { onBack ->
                            BufferSizeSelector(onBack)
                        }
                    )
                )
            )
        ),

        // Providers
        SettingsNode(
            id = "providers",
            title = providersTitle,
            description = null,
            icon = Icons.Default.Build,
            children = listOf(
                SettingsNode(
                    id = "providers_active",
                    title = "Aktywni dostawcy",
                    description = "Włączone źródła",
                    children = listOf(
                        SettingsNode("provider_1", "Provider 1") { onBack ->
                            SwitchSettingWidget("Provider 1", "Włącz źródło Provider 1", onBack)
                        },
                        SettingsNode("provider_2", "Provider 2") { onBack ->
                            SwitchSettingWidget("Provider 2", "Włącz źródło Provider 2", onBack)
                        },
                        SettingsNode("provider_3", "Provider 3") { onBack ->
                            SwitchSettingWidget("Provider 3", "Włącz źródło Provider 3", onBack)
                        },
                        SettingsNode("provider_4", "Provider 4") { onBack ->
                            SwitchSettingWidget("Provider 4", "Włącz źródło Provider 4", onBack)
                        },
                        SettingsNode("provider_5", "Provider 5") { onBack ->
                            SwitchSettingWidget("Provider 5", "Włącz źródło Provider 5", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "providers_order",
                    title = "Kolejność",
                    description = "Priorytet źródeł",
                    children = listOf(
                        SettingsNode("order_auto", "Automatyczna") { onBack ->
                            InfoDisplay("Kolejność", "Automatyczne wybieranie najlepszego źródła", onBack)
                        },
                        SettingsNode("order_manual", "Ręczna") { onBack ->
                            InfoDisplay("Kolejność", "Wybierz źródło ręcznie przed odtwarzaniem", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "providers_timeout",
                    title = "Limit czasu",
                    description = "Timeout dla źródeł",
                    children = listOf(
                        SettingsNode("timeout_value", "Wartość") { onBack ->
                            TimeoutEditor(onBack)
                        }
                    )
                )
            )
        ),

        // UI/Layout
        SettingsNode(
            id = "ui",
            title = uiTitle,
            description = null,
            icon = Icons.Default.Info,
            children = listOf(
                SettingsNode(
                    id = "ui_theme",
                    title = "Motyw",
                    description = "Kolorystyka aplikacji",
                    children = listOf(
                        SettingsNode("theme_dark", "Ciemny") { onBack ->
                            ThemeSelector("Ciemny", onBack)
                        },
                        SettingsNode("theme_light", "Jasny") { onBack ->
                            ThemeSelector("Jasny", onBack)
                        },
                        SettingsNode("theme_amoled", "AMOLED") { onBack ->
                            ThemeSelector("AMOLED", onBack)
                        },
                        SettingsNode("theme_system", "Systemowy") { onBack ->
                            ThemeSelector("Systemowy", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "ui_layout",
                    title = "Układ",
                    description = "Rozmieszczenie elementów",
                    children = listOf(
                        SettingsNode("layout_grid", "Siatka") { onBack ->
                            LayoutSelector("Siatka", onBack)
                        },
                        SettingsNode("layout_list", "Lista") { onBack ->
                            LayoutSelector("Lista", onBack)
                        },
                        SettingsNode("layout_compact", "Kompaktowy") { onBack ->
                            LayoutSelector("Kompaktowy", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "ui_animations",
                    title = "Animacje",
                    description = "Efekty wizualne",
                    children = listOf(
                        SettingsNode("animations_enabled", "Włączone") { onBack ->
                            SwitchSettingWidget("Animacje", "Włącz animacje interfejsu", onBack)
                        },
                        SettingsNode("animations_reduced", "Ograniczone") { onBack ->
                            SwitchSettingWidget("Ograniczone animacje", "Zmniejsz ilość animacji", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "ui_poster",
                    title = "Plakaty",
                    description = "Styl plakatów",
                    children = listOf(
                        SettingsNode("poster_rounded", "Zaokrąglone") { onBack ->
                            SwitchSettingWidget("Zaokrąglone rogi", "Plakaty z zaokrąglonymi rogami", onBack)
                        },
                        SettingsNode("poster_shadow", "Cienie") { onBack ->
                            SwitchSettingWidget("Cienie", "Dodaj cienie do plakatów", onBack)
                        }
                    )
                )
            )
        ),

        // Updates
        SettingsNode(
            id = "updates",
            title = updatesTitle,
            description = null,
            icon = Icons.Default.Refresh,
            children = listOf(
                SettingsNode(
                    id = "updates_app",
                    title = "Aplikacja",
                    description = "Aktualizacje aplikacji",
                    children = listOf(
                        SettingsNode("app_auto_update", "Automatyczne") { onBack ->
                            SwitchSettingWidget("Automatyczne aktualizacje", "Aktualizuj aplikację automatycznie", onBack)
                        },
                        SettingsNode("app_check_now", "Sprawdź teraz") { onBack ->
                            ActionButton("Sprawdź aktualizacje", "Wyszukaj nowe wersje aplikacji", onBack)
                        },
                        SettingsNode("app_beta", "Wersje beta") { onBack ->
                            SwitchSettingWidget("Wersje beta", "Otrzymuj wersje testowe", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "updates_extensions",
                    title = "Rozszerzenia",
                    description = "Aktualizacje rozszerzeń",
                    children = listOf(
                        SettingsNode("ext_auto_update", "Automatyczne") { onBack ->
                            SwitchSettingWidget("Automatyczne aktualizacje", "Aktualizuj rozszerzenia automatycznie", onBack)
                        },
                        SettingsNode("ext_check_now", "Sprawdź teraz") { onBack ->
                            ActionButton("Sprawdź aktualizacje rozszerzeń", "Wyszukaj nowe wersje rozszerzeń", onBack)
                        }
                    )
                )
            )
        ),

        // Account
        SettingsNode(
            id = "account",
            title = accountTitle,
            description = null,
            icon = Icons.Default.AccountCircle,
            children = listOf(
                SettingsNode(
                    id = "account_profile",
                    title = "Profil",
                    description = "Dane profilu",
                    children = listOf(
                        SettingsNode("profile_name", "Nazwa") { onBack ->
                            ProfileNameEditor(onBack)
                        },
                        SettingsNode("profile_avatar", "Awatar") { onBack ->
                            AvatarSelector(onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "account_sync",
                    title = "Synchronizacja",
                    description = "Synchronizacja danych",
                    children = listOf(
                        SettingsNode("sync_enabled", "Włączona") { onBack ->
                            SwitchSettingWidget("Synchronizacja", "Synchronizuj dane między urządzeniami", onBack)
                        },
                        SettingsNode("sync_now", "Synchronizuj teraz") { onBack ->
                            ActionButton("Synchronizuj", "Rozpocznij synchronizację", onBack)
                        }
                    )
                ),
                SettingsNode(
                    id = "account_backup",
                    title = "Kopia zapasowa",
                    description = "Backup i przywracanie",
                    children = listOf(
                        SettingsNode("backup_create", "Utwórz kopię") { onBack ->
                            ActionButton("Utwórz kopię zapasową", "Zapisz wszystkie ustawienia", onBack)
                        },
                        SettingsNode("backup_restore", "Przywróć") { onBack ->
                            ActionButton("Przywróć z kopii", "Wczytaj zapisane ustawienia", onBack)
                        }
                    )
                )
            )
        ),

        // Extensions - dynamic tree from ExtensionsViewModel
        if (extensionsViewModel != null) {
            val extensionsNode = buildExtensionsSettingsTree(extensionsViewModel)
            // Add icon to the generated node
            extensionsNode.copy(
                icon = extensionIcon,
                title = extensionsTitle
            )
        } else {
            // Fallback placeholder if ViewModel not available
            SettingsNode(
                id = "extensions",
                title = extensionsTitle,
                description = null,
                icon = extensionIcon,
                children = listOf(
                    SettingsNode(
                        id = "extensions_placeholder",
                        title = "Ładowanie...",
                        content = { _ ->
                            androidx.tv.material3.Text("Extensions ViewModel not initialized")
                        }
                    )
                )
            )
        }
    )
}
