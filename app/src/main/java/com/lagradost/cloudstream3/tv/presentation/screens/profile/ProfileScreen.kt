package com.lagradost.cloudstream3.tv.presentation.screens.profile

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.utils.DataStoreHelper
import kotlinx.collections.immutable.PersistentList
import qrcode.QRCode

private object ProfileScreenTags {
    const val ManageButton = "profile_manage_button"
    const val DoneButton = "profile_done_button"
    const val EditorName = "profile_editor_name"
    const val EditorImage = "profile_editor_image"
    const val EditorSave = "profile_editor_save"
    const val EditorPin = "profile_editor_pin"
    const val EditorDelete = "profile_editor_delete"
    const val PinInput = "profile_pin_input"
    const val PinConfirm = "profile_pin_confirm"
    const val PinCancel = "profile_pin_cancel"
}

private object ProfileScreenTokens {
    val ScreenPadding = PaddingValues(horizontal = 48.dp, vertical = 28.dp)
    val PanelSpacing = 28.dp
    val PanelShape = RoundedCornerShape(20.dp)
    val PanelPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
    val PanelVerticalSpacing = 20.dp
    val LeftPanelWidth = 340.dp
    val SummaryImageSize = 180.dp
    val EditorPreviewSize = 220.dp
    val ListItemImageSize = 64.dp
    val ListItemShape = RoundedCornerShape(18.dp)
    val CardShape = RoundedCornerShape(16.dp)
    val CardSpacing = 10.dp
    val FieldShape = RoundedCornerShape(12.dp)
    val FieldPadding = 14.dp
    val FieldLabelSpacing = 6.dp
    val ButtonSpacing = 12.dp
    val PinDialogWidth = 420.dp
    val PinDialogPadding = 24.dp
    val PinDialogSpacing = 16.dp
    val ScrimColor = Color.Black.copy(alpha = 0.56f)
}

@Composable
fun ProfileScreen(
    topBarFocusRequester: FocusRequester,
    isTopBarFocused: Boolean = false,
    onTopBarVisibilityChanged: (Boolean) -> Unit,
    onTopBarFocusableChanged: (Boolean) -> Unit,
    onTopBarDownNavigationEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val selectedAccount = remember(uiState.accounts) {
        uiState.accounts.firstOrNull { account -> account.isSelected } ?: uiState.accounts.firstOrNull()
    }
    val listFocusRequesterMap = rememberFocusRequesterMap(
        remember(uiState.accounts) {
            uiState.accounts.map { account -> profileAccountFocusKey(account.keyIndex) } +
                ProfileAddAccountFocusKey
        }
    )
    val summaryButtonFocusRequester = remember { FocusRequester() }
    val doneButtonFocusRequester = remember { FocusRequester() }
    val editorNameFocusRequester = remember { FocusRequester() }
    val pinInputFocusRequester = remember { FocusRequester() }
    val pendingFocusKey = uiState.pendingListFocusKey

    val topBarVisibilityState by rememberUpdatedState(onTopBarVisibilityChanged)
    val topBarFocusableState by rememberUpdatedState(onTopBarFocusableChanged)
    val topBarDownNavigationState by rememberUpdatedState(onTopBarDownNavigationEnabledChanged)

    LaunchedEffect(Unit) {
        topBarVisibilityState(true)
        topBarFocusableState(true)
        topBarDownNavigationState(true)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    FocusRequestEffect(
        requester = pendingFocusKey?.let(listFocusRequesterMap::get),
        requestKey = uiState.listRestoreFocusToken to pendingFocusKey,
        enabled = pendingFocusKey != null
    )

    FocusRequestEffect(
        requester = editorNameFocusRequester,
        requestKey = uiState.editorRequestFocusToken,
        enabled = uiState.editorRequestFocusToken > 0 && uiState.mode == ProfileMode.Manage
    )

    FocusRequestEffect(
        requester = pinInputFocusRequester,
        requestKey = uiState.pinPrompt,
        enabled = uiState.pinPrompt != null
    )

    BackHandler(
        enabled = uiState.pinPrompt != null ||
            uiState.mode == ProfileMode.Manage ||
            !isTopBarFocused
    ) {
        when {
            uiState.pinPrompt != null -> viewModel.dismissPinPrompt()
            uiState.mode == ProfileMode.Manage -> viewModel.exitManageMode()
            !isTopBarFocused -> topBarFocusRequester.requestFocus()
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(ProfileScreenTokens.ScreenPadding)
    ) {
        when (uiState.mode) {
            ProfileMode.Select -> {
                ProfileSelectionContent(
                    accounts = uiState.accounts,
                    selectedAccount = selectedAccount,
                    topBarFocusRequester = topBarFocusRequester,
                    listFocusRequesterMap = listFocusRequesterMap,
                    summaryButtonFocusRequester = summaryButtonFocusRequester,
                    onAccountSelected = viewModel::selectAccount,
                    onAddAccount = viewModel::startNewAccountEditor,
                    onManageAccounts = viewModel::openManageMode,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ProfileMode.Manage -> {
                ProfileManageContent(
                    accounts = uiState.accounts,
                    editor = uiState.editor,
                    topBarFocusRequester = topBarFocusRequester,
                    listFocusRequesterMap = listFocusRequesterMap,
                    doneButtonFocusRequester = doneButtonFocusRequester,
                    editorNameFocusRequester = editorNameFocusRequester,
                    onDone = viewModel::exitManageMode,
                    onEditAccount = viewModel::editAccount,
                    onAddAccount = viewModel::startNewAccountEditor,
                    onNameChanged = viewModel::updateEditorName,
                    onCustomImageChanged = viewModel::updateEditorCustomImageUrl,
                    onCycleImage = viewModel::cycleEditorBackground,
                    onSave = viewModel::saveEditor,
                    onSetPin = viewModel::openSetPinPrompt,
                    onRemovePin = viewModel::openRemovePinPrompt,
                    onDelete = viewModel::deleteEditor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        uiState.pinPrompt?.let { prompt ->
            ProfilePinPrompt(
                prompt = prompt,
                inputFocusRequester = pinInputFocusRequester,
                onInputChanged = viewModel::updatePinPromptInput,
                onConfirm = viewModel::submitPinPrompt,
                onCancel = viewModel::dismissPinPrompt,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ProfileSelectionContent(
    accounts: PersistentList<ProfileAccountItemUiState>,
    selectedAccount: ProfileAccountItemUiState?,
    topBarFocusRequester: FocusRequester,
    listFocusRequesterMap: Map<String, FocusRequester>,
    summaryButtonFocusRequester: FocusRequester,
    onAccountSelected: (Int) -> Unit,
    onAddAccount: () -> Unit,
    onManageAccounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PanelSpacing)
    ) {
        ProfileAccountsPanel(
            title = stringResource(R.string.select_an_account),
            accounts = accounts,
            selectionMode = true,
            activeEditorKeyIndex = null,
            topBarFocusRequester = topBarFocusRequester,
            rightPanelFocusRequester = summaryButtonFocusRequester,
            listFocusRequesterMap = listFocusRequesterMap,
            onAccountClick = onAccountSelected,
            onAddAccount = onAddAccount,
            modifier = Modifier.width(ProfileScreenTokens.LeftPanelWidth)
        )

        ProfileSummaryPanel(
            selectedAccount = selectedAccount,
            manageButtonFocusRequester = summaryButtonFocusRequester,
            selectedAccountFocusRequester = selectedAccount
                ?.let { listFocusRequesterMap[profileAccountFocusKey(it.keyIndex)] },
            onManageAccounts = onManageAccounts,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileManageContent(
    accounts: PersistentList<ProfileAccountItemUiState>,
    editor: ProfileEditorUiState?,
    topBarFocusRequester: FocusRequester,
    listFocusRequesterMap: Map<String, FocusRequester>,
    doneButtonFocusRequester: FocusRequester,
    editorNameFocusRequester: FocusRequester,
    onDone: () -> Unit,
    onEditAccount: (Int) -> Unit,
    onAddAccount: () -> Unit,
    onNameChanged: (String) -> Unit,
    onCustomImageChanged: (String) -> Unit,
    onCycleImage: () -> Unit,
    onSave: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PanelSpacing)
    ) {
        Surface(
            modifier = Modifier
                .width(ProfileScreenTokens.LeftPanelWidth)
                .fillMaxHeight(),
            shape = ProfileScreenTokens.PanelShape,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ProfileScreenTokens.PanelPadding),
                verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PanelVerticalSpacing)
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(doneButtonFocusRequester)
                        .focusProperties {
                            up = topBarFocusRequester
                            right = editorNameFocusRequester
                        }
                        .testTag(ProfileScreenTags.DoneButton)
                ) {
                    Text(text = stringResource(R.string.setup_done))
                }

                ProfileAccountsList(
                    accounts = accounts,
                    selectionMode = false,
                    activeEditorKeyIndex = editor?.keyIndex,
                    topBarFocusRequester = topBarFocusRequester,
                    rightPanelFocusRequester = editorNameFocusRequester,
                    listFocusRequesterMap = listFocusRequesterMap,
                    onAccountClick = onEditAccount,
                    onAddAccount = onAddAccount,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        ProfileEditorPanel(
            editor = editor,
            topBarFocusRequester = topBarFocusRequester,
            leftPanelFocusRequester = editor
                ?.takeUnless { it.isNew }
                ?.let { listFocusRequesterMap[profileAccountFocusKey(it.keyIndex)] }
                ?: listFocusRequesterMap[ProfileAddAccountFocusKey],
            nameFocusRequester = editorNameFocusRequester,
            onNameChanged = onNameChanged,
            onCustomImageChanged = onCustomImageChanged,
            onCycleImage = onCycleImage,
            onSave = onSave,
            onSetPin = onSetPin,
            onRemovePin = onRemovePin,
            onDelete = onDelete,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileAccountsPanel(
    title: String,
    accounts: PersistentList<ProfileAccountItemUiState>,
    selectionMode: Boolean,
    activeEditorKeyIndex: Int?,
    topBarFocusRequester: FocusRequester,
    rightPanelFocusRequester: FocusRequester,
    listFocusRequesterMap: Map<String, FocusRequester>,
    onAccountClick: (Int) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = ProfileScreenTokens.PanelShape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ProfileScreenTokens.PanelPadding),
            verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PanelVerticalSpacing)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            ProfileAccountsList(
                accounts = accounts,
                selectionMode = selectionMode,
                activeEditorKeyIndex = activeEditorKeyIndex,
                topBarFocusRequester = topBarFocusRequester,
                rightPanelFocusRequester = rightPanelFocusRequester,
                listFocusRequesterMap = listFocusRequesterMap,
                onAccountClick = onAccountClick,
                onAddAccount = onAddAccount,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ProfileAccountsList(
    accounts: PersistentList<ProfileAccountItemUiState>,
    selectionMode: Boolean,
    activeEditorKeyIndex: Int?,
    topBarFocusRequester: FocusRequester,
    rightPanelFocusRequester: FocusRequester,
    listFocusRequesterMap: Map<String, FocusRequester>,
    onAccountClick: (Int) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.CardSpacing)
    ) {
        items(
            items = accounts,
            key = { account -> account.keyIndex }
        ) { account ->
            val focusRequester = listFocusRequesterMap[profileAccountFocusKey(account.keyIndex)]
            ProfileAccountListItem(
                account = account,
                isActive = if (selectionMode) account.isSelected else account.keyIndex == activeEditorKeyIndex,
                focusRequester = focusRequester,
                topBarFocusRequester = topBarFocusRequester,
                rightPanelFocusRequester = rightPanelFocusRequester,
                onClick = { onAccountClick(account.keyIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(profileAccountFocusKey(account.keyIndex))
            )
        }

        item(key = ProfileAddAccountFocusKey) {
            val focusRequester = listFocusRequesterMap[ProfileAddAccountFocusKey]
            ProfileAddAccountItem(
                focusRequester = focusRequester,
                topBarFocusRequester = topBarFocusRequester,
                rightPanelFocusRequester = rightPanelFocusRequester,
                onClick = onAddAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProfileAddAccountFocusKey)
            )
        }
    }
}

@Composable
private fun ProfileAccountListItem(
    account: ProfileAccountItemUiState,
    isActive: Boolean,
    focusRequester: FocusRequester?,
    topBarFocusRequester: FocusRequester,
    rightPanelFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = isActive,
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                up = topBarFocusRequester
                right = rightPanelFocusRequester
            },
        shape = SelectableSurfaceDefaults.shape(shape = ProfileScreenTokens.ListItemShape),
        colors = SelectableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileImage(
                customImageUrl = account.customImageUrl,
                defaultImageIndex = account.defaultImageIndex,
                modifier = Modifier.size(ProfileScreenTokens.ListItemImageSize),
                shape = ProfileScreenTokens.CardShape
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (account.isSelected) {
                        stringResource(R.string.logged_account, account.name)
                    } else {
                        stringResource(R.string.profiles)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (account.hasPin) {
                Icon(
                    painter = painterResource(R.drawable.video_locked),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileAddAccountItem(
    focusRequester: FocusRequester?,
    topBarFocusRequester: FocusRequester,
    rightPanelFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = false,
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                up = topBarFocusRequester
                right = rightPanelFocusRequester
            },
        shape = SelectableSurfaceDefaults.shape(shape = ProfileScreenTokens.ListItemShape),
        colors = SelectableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ProfileScreenTokens.ListItemImageSize)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = ProfileScreenTokens.CardShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_add_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_account),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.manage_accounts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileSummaryPanel(
    selectedAccount: ProfileAccountItemUiState?,
    manageButtonFocusRequester: FocusRequester,
    selectedAccountFocusRequester: FocusRequester?,
    onManageAccounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = ProfileScreenTokens.PanelShape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ProfileScreenTokens.PanelPadding),
            verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PanelVerticalSpacing),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.profiles),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            ProfileImage(
                customImageUrl = selectedAccount?.customImageUrl,
                defaultImageIndex = selectedAccount?.defaultImageIndex ?: 0,
                modifier = Modifier.size(ProfileScreenTokens.SummaryImageSize),
                shape = RoundedCornerShape(28.dp)
            )

            Text(
                text = selectedAccount?.name ?: stringResource(R.string.no_account),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = selectedAccount?.let { account ->
                    stringResource(R.string.logged_account, account.name)
                } ?: stringResource(R.string.no_account),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onManageAccounts,
                modifier = Modifier
                    .focusRequester(manageButtonFocusRequester)
                    .focusProperties {
                        left = selectedAccountFocusRequester ?: FocusRequester.Default
                    }
                    .testTag(ProfileScreenTags.ManageButton)
            ) {
                Text(text = stringResource(R.string.manage_accounts))
            }
        }
    }
}

@Composable
private fun ProfileEditorPanel(
    editor: ProfileEditorUiState?,
    topBarFocusRequester: FocusRequester,
    leftPanelFocusRequester: FocusRequester?,
    nameFocusRequester: FocusRequester,
    onNameChanged: (String) -> Unit,
    onCustomImageChanged: (String) -> Unit,
    onCycleImage: () -> Unit,
    onSave: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = ProfileScreenTokens.PanelShape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (editor == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Surface
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(ProfileScreenTokens.PanelPadding),
            verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PanelVerticalSpacing)
        ) {
            item(key = "editor_header") {
                Text(
                    text = if (editor.isNew) {
                        stringResource(R.string.add_account)
                    } else {
                        stringResource(R.string.edit_account)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item(key = "editor_preview") {
                OutlinedButton(
                    onClick = onCycleImage,
                    modifier = Modifier
                        .size(ProfileScreenTokens.EditorPreviewSize)
                        .testTag(ProfileScreenTags.EditorImage)
                ) {
                    ProfileImage(
                        customImageUrl = editor.customImageUrl.ifBlank { null },
                        defaultImageIndex = editor.defaultImageIndex,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }

            item(key = "editor_background_hint") {
                Text(
                    text = stringResource(R.string.profile_background_des),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "editor_name") {
                ProfileTextField(
                    value = editor.name,
                    onValueChange = onNameChanged,
                    label = stringResource(R.string.account),
                    placeholder = stringResource(R.string.account),
                    modifier = Modifier
                        .focusRequester(nameFocusRequester)
                        .focusProperties {
                            up = topBarFocusRequester
                            left = leftPanelFocusRequester ?: FocusRequester.Default
                        }
                        .testTag(ProfileScreenTags.EditorName)
                )
            }

            item(key = "editor_image_url") {
                ProfileTextField(
                    value = editor.customImageUrl,
                    onValueChange = onCustomImageChanged,
                    label = stringResource(R.string.edit_profile_image_title),
                    placeholder = stringResource(R.string.edit_profile_image_hint),
                    modifier = Modifier
                        .focusProperties {
                            left = leftPanelFocusRequester ?: FocusRequester.Default
                        }
                )
            }

            item(key = "editor_actions") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.ButtonSpacing)
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties {
                                left = leftPanelFocusRequester ?: FocusRequester.Default
                            }
                            .testTag(ProfileScreenTags.EditorSave)
                    ) {
                        Text(text = stringResource(R.string.setup_done))
                    }

                    OutlinedButton(
                        onClick = if (editor.lockPin == null) onSetPin else onRemovePin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties {
                                left = leftPanelFocusRequester ?: FocusRequester.Default
                            }
                            .testTag(ProfileScreenTags.EditorPin)
                    ) {
                        Text(
                            text = if (editor.lockPin == null) {
                                stringResource(R.string.lock_profile)
                            } else {
                                stringResource(R.string.pin)
                            }
                        )
                    }

                    if (editor.canDelete) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties {
                                    left = leftPanelFocusRequester ?: FocusRequester.Default
                                }
                                .testTag(ProfileScreenTags.EditorDelete)
                        ) {
                            Text(text = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePinPrompt(
    prompt: ProfilePinPromptUiState,
    inputFocusRequester: FocusRequester,
    onInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(ProfileScreenTokens.ScrimColor),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(ProfileScreenTokens.PinDialogWidth),
            shape = ProfileScreenTokens.PanelShape,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ProfileScreenTokens.PinDialogPadding),
                verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.PinDialogSpacing)
            ) {
                Text(
                    text = when (prompt.mode) {
                        ProfilePinPromptMode.UnlockSelection -> stringResource(
                            R.string.enter_pin_with_name,
                            prompt.accountName
                        )
                        ProfilePinPromptMode.SetPin -> stringResource(R.string.enter_pin)
                        ProfilePinPromptMode.RemovePin -> stringResource(R.string.enter_current_pin)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ProfileTextField(
                    value = prompt.input,
                    onValueChange = onInputChanged,
                    label = stringResource(R.string.pin),
                    placeholder = stringResource(R.string.pin),
                    modifier = Modifier
                        .focusRequester(inputFocusRequester)
                        .testTag(ProfileScreenTags.PinInput),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm() }
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )

                prompt.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ProfileScreenTokens.ButtonSpacing)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ProfileScreenTags.PinCancel)
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ProfileScreenTags.PinConfirm)
                    ) {
                        Text(
                            text = when (prompt.mode) {
                                ProfilePinPromptMode.SetPin -> stringResource(R.string.setup_done)
                                else -> stringResource(R.string.ok)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Text
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ProfileScreenTokens.FieldLabelSpacing)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyUp &&
                        event.key == Key.DirectionDown &&
                        keyboardOptions.imeAction == ImeAction.Next
                    ) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else {
                        false
                    }
                },
            shape = ProfileScreenTokens.FieldShape,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                modifier = modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ProfileScreenTokens.FieldPadding)
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileImage(
    customImageUrl: String?,
    defaultImageIndex: Int,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
) {
    val defaultRes = remember(defaultImageIndex) {
        DataStoreHelper.profileImages.getOrNull(defaultImageIndex)
            ?: DataStoreHelper.profileImages.first()
    }
    val normalizedUrl = customImageUrl?.trim().takeUnless { it.isNullOrBlank() }

    Surface(
        modifier = modifier,
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (normalizedUrl != null) {
            AsyncImage(
                model = normalizedUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(defaultRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
