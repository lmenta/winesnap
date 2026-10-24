package com.winesnap.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winesnap.app.Step
import com.winesnap.app.UiState
import com.winesnap.app.WineSnapViewModel
import com.winesnap.app.data.AnalyzeResult
import com.winesnap.app.data.Wine
import com.winesnap.app.ui.theme.Emerald50
import com.winesnap.app.ui.theme.Emerald700
import com.winesnap.app.ui.theme.Stone200
import com.winesnap.app.ui.theme.Stone400
import com.winesnap.app.ui.theme.Stone50
import com.winesnap.app.ui.theme.Stone500
import com.winesnap.app.ui.theme.Stone700
import com.winesnap.app.ui.theme.Stone900
import com.winesnap.app.ui.theme.Wine as WineRed
import com.winesnap.app.ui.theme.WineLight
import java.io.File

@Composable
fun WineSnapScreen(vm: WineSnapViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Camera: the system camera writes into a FileProvider URI we hand it.
    var pendingCaptureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingCaptureUri?.takeIf { saved }?.let(vm::onImagePicked)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::onImagePicked)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Stone50) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 520.dp)) {
                Header()
                Spacer(Modifier.height(24.dp))

                if (state.image == null) {
                    PhotoPicker(
                        onCamera = {
                            val uri = newCaptureUri(context)
                            pendingCaptureUri = uri
                            takePicture.launch(uri)
                        },
                        onGallery = {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                } else {
                    PhotoPreview(state, onChange = vm::reset)
                }

                Spacer(Modifier.height(16.dp))
                PreferenceField(state.preference, enabled = !state.loading, onChange = vm::onPreferenceChange)
                Spacer(Modifier.height(12.dp))
                AnalyzeButton(state, onClick = vm::analyze)

                state.step?.let { StepProgress(it) }
                state.error?.let { ErrorBanner(it) }
                state.result?.let { Results(it, onReset = vm::reset) }
            }
        }
    }
}

private fun newCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(WineRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.WineBar, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("WineSnap", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Stone900)
        Text("Photo a wine shelf. Get the best bottle.", fontSize = 14.sp, color = Stone500)
    }
}

@Composable
private fun PhotoPicker(onCamera: () -> Unit, onGallery: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, Stone200, RoundedCornerShape(16.dp)),
    ) {
        PickerOption(Icons.Filled.PhotoCamera, "Take Photo", "Open camera", WineRed, onCamera, Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(120.dp).background(Stone200).align(Alignment.CenterVertically))
        PickerOption(Icons.Filled.PhotoLibrary, "Choose Photo", "From gallery", Stone500, onGallery, Modifier.weight(1f))
    }
}

@Composable
private fun PickerOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Stone700)
        Text(subtitle, fontSize = 12.sp, color = Stone400)
    }
}

@Composable
private fun PhotoPreview(state: UiState, onChange: () -> Unit) {
    val image = state.image ?: return
    Box {
        Image(
            bitmap = image.bitmap.asImageBitmap(),
            contentDescription = "Wine shelf",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        TextButton(
            onClick = onChange,
            enabled = !state.loading,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            colors = ButtonDefaults.textButtonColors(containerColor = Color.Black.copy(alpha = 0.6f), contentColor = Color.White),
        ) {
            Text("Change", fontSize = 12.sp)
        }
    }
}

@Composable
private fun PreferenceField(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text("e.g. good with steak, birthday gift, under £15", fontSize = 14.sp) },
        label = { Text("What matters?") },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WineRed,
            unfocusedBorderColor = Stone200,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AnalyzeButton(state: UiState, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = state.image != null && !state.loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WineRed,
            disabledContainerColor = WineRed.copy(alpha = 0.4f),
            disabledContentColor = Color.White,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (state.loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(state.step?.label ?: "Analysing…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Filled.WineBar, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pick the best wine", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StepProgress(current: Step) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Step.entries.forEach { s ->
            val color by animateColorAsState(if (s.ordinal <= current.ordinal) WineRed else Stone200, label = "step")
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WineLight)
            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
        Text(message, fontSize = 14.sp, color = Color(0xFFB91C1C))
    }
}

@Composable
private fun Results(result: AnalyzeResult, onReset: () -> Unit) {
    var showAll by rememberSaveable(result) { mutableStateOf(false) }

    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WinnerCard(result.winner)

        if (result.allWines.size > 1) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAll = !showAll }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val extra = if (result.winesWithData < result.winesIdentified) {
                        ", ${result.winesWithData} with price/score data"
                    } else ""
                    Text(
                        "All wines (${result.winesIdentified} found$extra)",
                        fontSize = 14.sp,
                        color = Stone500,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (showAll) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (showAll) "Collapse" else "Expand",
                        tint = Stone500,
                    )
                }
                AnimatedVisibility(showAll) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.allWines.forEachIndexed { i, w -> WineRow(w, highlight = i == 0) }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Stone200),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Stone700),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Try another photo", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun WinnerCard(w: Wine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, WineRed, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = WineRed, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("BEST PICK", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WineRed, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append(w.displayName)
                w.vintage?.let { append(" "); append(it) }
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Stone900,
        )
        w.region?.let { Text(it, fontSize = 12.sp, color = Stone400) }

        val stats = listOfNotNull(
            w.formattedPrice?.let { Triple(it, "Price", Stone900) },
            w.formattedScore?.let { Triple(it, "Score/100", Stone900) },
            w.formattedValue?.let { Triple(it, "Value", WineRed) },
        )
        if (stats.isNotEmpty()) {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                stats.forEach { (value, label, color) ->
                    Column {
                        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                        Text(label, fontSize = 12.sp, color = Stone400)
                    }
                }
            }
        }

        w.reason?.let { Text(it, fontSize = 14.sp, color = Stone700, lineHeight = 21.sp, modifier = Modifier.padding(top = 12.dp)) }
        w.tastingNote?.let { Text(it, fontSize = 14.sp, color = Stone500, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 8.dp)) }
        w.valueTip?.let {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Emerald50)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                Text(it, fontSize = 12.sp, color = Emerald700)
            }
        }
    }
}

@Composable
private fun WineRow(w: Wine, highlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Stone200, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(w.displayName, w.vintage).joinToString(" "),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Stone900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(w.region ?: "—", fontSize = 12.sp, color = Stone400, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            w.formattedPrice?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone700) }
            w.formattedScore?.let { Text("${it}pts", fontSize = 12.sp, color = Stone400) }
            w.formattedValue?.let {
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (highlight) WineRed else Stone200)
            }
        }
    }
}
