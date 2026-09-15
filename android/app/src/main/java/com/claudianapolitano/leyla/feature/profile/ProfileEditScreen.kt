package com.claudianapolitano.leyla.feature.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.Avatar
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.cycle.CycleState
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.TextAction
import com.claudianapolitano.leyla.feature.together.errorRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * The account editor Home's profile button opens. Port of the parts of
 * `ProfileEditView` the Android build can already stand behind: display name,
 * profile photo, and the "I have a menstrual cycle" answer.
 *
 * Email changes are not here yet — they need the code-confirmation flow, which
 * lands with the Settings tab.
 */

data class ProfileUiState(
    val nameDraft: String = "",
    val email: String? = null,
    val avatarPath: String? = null,
    val hasCycle: Boolean = false,
    val savingName: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val savedNotice: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSaveName: Boolean get() = nameDraft.isNotBlank() && !savingName
}

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /** True once the field has been edited, so a refresh never clobbers typing. */
    private var nameTouched = false

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update {
                    it.copy(
                        nameDraft = if (nameTouched) it.nameDraft else s.user?.displayName.orEmpty(),
                        email = s.user?.email,
                        avatarPath = s.user?.avatarPath,
                    )
                }
            }
        }
        viewModelScope.launch {
            CycleState.snapshot.collect { c ->
                _state.update { it.copy(hasCycle = c.userHasCycle == true) }
            }
        }
    }

    fun typeName(value: String) {
        nameTouched = true
        _state.update { it.copy(nameDraft = value, savedNotice = false) }
    }

    fun saveName() {
        val name = _state.value.nameDraft.trim()
        if (name.isEmpty() || _state.value.savingName) return
        viewModelScope.launch {
            _state.update { it.copy(savingName = true, errorMessage = null) }
            try {
                Session.updateUser(LeylaApi.updateDisplayName(name))
                nameTouched = false
                _state.update { it.copy(savingName = false, savedNotice = true) }
            } catch (e: Exception) {
                _state.update { it.copy(savingName = false, errorMessage = e.message) }
            }
        }
    }

    fun uploadAvatar(jpeg: ByteArray) {
        if (_state.value.uploadingAvatar) return
        viewModelScope.launch {
            _state.update { it.copy(uploadingAvatar = true, errorMessage = null) }
            try {
                Session.updateUser(LeylaApi.uploadAvatar(jpeg))
                _state.update { it.copy(uploadingAvatar = false) }
            } catch (e: Exception) {
                _state.update { it.copy(uploadingAvatar = false, errorMessage = e.message) }
            }
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            _state.update { it.copy(uploadingAvatar = true, errorMessage = null) }
            try {
                Session.updateUser(LeylaApi.deleteAvatar())
                _state.update { it.copy(uploadingAvatar = false) }
            } catch (e: Exception) {
                _state.update { it.copy(uploadingAvatar = false, errorMessage = e.message) }
            }
        }
    }

    fun setHasCycle(hasCycle: Boolean) {
        viewModelScope.launch { CycleState.setUserHasCycle(hasCycle) }
    }
}

@Composable
fun ProfileEditScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.loadSquareJpeg(uri)?.let(viewModel::uploadAvatar)
    }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LeylaCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Avatar(path = state.avatarPath, name = state.nameDraft, size = 64.dp)
                    if (state.uploadingAvatar) {
                        CircularProgressIndicator(color = Theme.rose, modifier = Modifier.size(28.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextAction(
                        text = stringResource(
                            if (state.avatarPath == null) R.string.profile_add_photo else R.string.profile_change_photo,
                        ),
                        onClick = {
                            photoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        color = Theme.coral,
                        style = IOSText.subheadline.weight(FontWeight.SemiBold),
                        enabled = !state.uploadingAvatar,
                    )
                    if (state.avatarPath != null) {
                        TextAction(
                            text = stringResource(R.string.profile_remove_photo),
                            onClick = viewModel::removeAvatar,
                            color = colors.secondary,
                            enabled = !state.uploadingAvatar,
                        )
                    }
                }
            }
        }

        LeylaCard {
            Text(
                stringResource(R.string.profile_section),
                style = IOSText.caption.weight(FontWeight.Bold),
                color = colors.secondary,
            )
            OutlinedTextField(
                value = state.nameDraft,
                onValueChange = viewModel::typeName,
                label = { Text(stringResource(R.string.profile_your_name)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor = Theme.rose,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            if (state.email != null) {
                Text(
                    stringResource(R.string.profile_email, state.email!!),
                    style = IOSText.footnote,
                    color = colors.secondary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            PrimaryButton(
                text = stringResource(
                    if (state.savedNotice) R.string.profile_saved else R.string.profile_save,
                ),
                onClick = viewModel::saveName,
                enabled = state.canSaveName,
                loading = state.savingName,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        LeylaCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.profile_i_have_a_cycle),
                        style = IOSText.body,
                        color = colors.ink,
                    )
                    Text(
                        stringResource(R.string.profile_cycle_footer),
                        style = IOSText.caption,
                        color = colors.secondary,
                    )
                }
                Switch(
                    checked = state.hasCycle,
                    onCheckedChange = viewModel::setHasCycle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Theme.rose),
                )
            }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage!!, style = IOSText.footnote, color = errorRed())
        }
    }
}

/**
 * Decodes and downsamples a picked photo into the square-ish JPEG the avatar
 * endpoint expects — a full-resolution camera roll image would be megabytes of
 * upload for a 40dp circle.
 */
private fun android.content.Context.loadSquareJpeg(uri: Uri, maxDimension: Int = 1024): ByteArray? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap: Bitmap = contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.toByteArray()
        }
    }.getOrNull()
