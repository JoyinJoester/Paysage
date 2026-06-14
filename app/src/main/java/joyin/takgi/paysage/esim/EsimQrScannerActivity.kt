package joyin.takgi.paysage.esim

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.theme.AppearanceSettingsStore
import joyin.takgi.paysage.ui.theme.PaysageTheme
import joyin.takgi.paysage.ui.theme.resolveDarkTheme
import joyin.takgi.paysage.ui.theme.withPaysageLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class EsimQrScannerActivity : ComponentActivity() {
    private var completed = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withPaysageLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appearanceSettings = remember {
                AppearanceSettingsStore(this@EsimQrScannerActivity).read()
            }
            val darkTheme = appearanceSettings.themeMode.resolveDarkTheme(isSystemInDarkTheme())

            PaysageTheme(
                darkTheme = darkTheme,
                colorScheme = appearanceSettings.colorScheme,
                oledPureBlack = appearanceSettings.oledPureBlack
            ) {
                EsimQrScannerScreen(
                    onCancel = { finish() },
                    onQrCodeDetected = ::finishWithQrCode
                )
            }
        }
    }

    private fun finishWithQrCode(content: String, sourceLabel: String) {
        runOnUiThread {
            if (completed) return@runOnUiThread
            completed = true
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_QR_CONTENT, content)
                    .putExtra(EXTRA_QR_SOURCE, sourceLabel)
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_QR_CONTENT = "joyin.takgi.paysage.extra.QR_CONTENT"
        const val EXTRA_QR_SOURCE = "joyin.takgi.paysage.extra.QR_SOURCE"
    }
}

@Composable
private fun EsimQrScannerScreen(
    onCancel: () -> Unit,
    onQrCodeDetected: (String, String) -> Unit
) {
    val context = LocalContext.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }
    var isDecodingImage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
    }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isDecodingImage = true
            val content = withContext(Dispatchers.IO) {
                EsimQrCodeDecoder.decode(context, uri)
            }
            isDecodingImage = false
            if (content.isNullOrBlank()) {
                Toast.makeText(context, context.getString(R.string.toast_no_esim_qr_in_image), Toast.LENGTH_SHORT).show()
            } else {
                onQrCodeDetected(content, context.getString(R.string.source_image_recognition))
            }
        }
    }
    val pickImage = {
        if (!isDecodingImage) {
            imageLauncher.launch("image/*")
        }
    }

    LaunchedEffect(hasCameraPermission, permissionRequested) {
        if (hasCamera && !hasCameraPermission && !permissionRequested) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !hasCamera -> {
                CameraUnavailablePanel(
                    isDecodingImage = isDecodingImage,
                    onPickImage = pickImage,
                    onCancel = onCancel
                )
            }

            hasCameraPermission -> {
                CameraScannerPreview(
                    torchEnabled = torchEnabled,
                    onQrCodeDetected = { onQrCodeDetected(it, context.getString(R.string.source_qr_scan)) }
                )
                ScannerOverlay(
                    torchEnabled = torchEnabled,
                    isDecodingImage = isDecodingImage,
                    onToggleTorch = { torchEnabled = !torchEnabled },
                    onPickImage = pickImage,
                    onCancel = onCancel
                )
            }

            else -> {
                CameraPermissionPanel(
                    permissionRequested = permissionRequested,
                    isDecodingImage = isDecodingImage,
                    onRequestPermission = {
                        permissionRequested = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onPickImage = pickImage,
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun CameraScannerPreview(
    torchEnabled: Boolean,
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrCodeDetected by rememberUpdatedState(onQrCodeDetected)
    val analyzer = remember {
        EsimQrCodeAnalyzer { content ->
            currentOnQrCodeDetected(content)
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    LaunchedEffect(torchEnabled) {
        runCatching { controller.enableTorch(torchEnabled) }
    }

    DisposableEffect(lifecycleOwner, controller) {
        controller.setImageAnalysisAnalyzer(executor, analyzer)
        runCatching { controller.bindToLifecycle(lifecycleOwner) }
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        }
    )
}

@Composable
private fun ScannerOverlay(
    torchEnabled: Boolean,
    isDecodingImage: Boolean,
    onToggleTorch: () -> Unit,
    onPickImage: () -> Unit,
    onCancel: () -> Unit
) {
    var showOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showOverlay = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.46f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.66f)
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically(
                animationSpec = tween(280),
                initialOffsetY = { -it / 8 }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.title_scan_external_esim),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.subtitle_scan_external_esim),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleTorch) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = if (torchEnabled) {
                                stringResource(R.string.action_turn_flash_off)
                            } else {
                                stringResource(R.string.action_turn_flash_on)
                            }
                        )
                    }
                }
            }
        }

        ScannerFrame(modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(animationSpec = tween(320)) + slideInVertically(
                animationSpec = tween(320),
                initialOffsetY = { it / 6 }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.title_place_qr_in_frame),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    GalleryButton(
                        isDecodingImage = isDecodingImage,
                        onPickImage = onPickImage
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    val cornerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)

    Box(
        modifier = modifier
            .size(268.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
                shape = RoundedCornerShape(28.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.10f))
    ) {
        ScannerCorner(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            color = cornerColor,
            position = ScannerCornerPosition.TopStart
        )
        ScannerCorner(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            color = cornerColor,
            position = ScannerCornerPosition.TopEnd
        )
        ScannerCorner(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            color = cornerColor,
            position = ScannerCornerPosition.BottomStart
        )
        ScannerCorner(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            color = cornerColor,
            position = ScannerCornerPosition.BottomEnd
        )
    }
}

private enum class ScannerCornerPosition {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd
}

@Composable
private fun ScannerCorner(
    modifier: Modifier = Modifier,
    color: Color,
    position: ScannerCornerPosition
) {
    val horizontalAlignment = when (position) {
        ScannerCornerPosition.TopStart, ScannerCornerPosition.BottomStart -> Alignment.CenterStart
        ScannerCornerPosition.TopEnd, ScannerCornerPosition.BottomEnd -> Alignment.CenterEnd
    }
    val verticalAlignment = when (position) {
        ScannerCornerPosition.TopStart, ScannerCornerPosition.TopEnd -> Alignment.TopCenter
        ScannerCornerPosition.BottomStart, ScannerCornerPosition.BottomEnd -> Alignment.BottomCenter
    }
    val cornerAlignment = when (position) {
        ScannerCornerPosition.TopStart -> Alignment.TopStart
        ScannerCornerPosition.TopEnd -> Alignment.TopEnd
        ScannerCornerPosition.BottomStart -> Alignment.BottomStart
        ScannerCornerPosition.BottomEnd -> Alignment.BottomEnd
    }

    Box(modifier = modifier.size(30.dp)) {
        Box(
            modifier = Modifier
                .align(verticalAlignment)
                .height(5.dp)
                .width(30.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .align(horizontalAlignment)
                .width(5.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .align(cornerAlignment)
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun GalleryButton(
    isDecodingImage: Boolean,
    onPickImage: () -> Unit
) {
    FilledTonalButton(
        onClick = onPickImage,
        enabled = !isDecodingImage,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            if (isDecodingImage) {
                stringResource(R.string.action_decoding_image)
            } else {
                stringResource(R.string.action_select_qr_image)
            }
        )
    }
}

@Composable
private fun CameraUnavailablePanel(
    isDecodingImage: Boolean,
    onPickImage: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        M3ePanel(
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(24.dp),
            prominent = true
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
                Text(stringResource(R.string.title_no_camera_available), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.message_no_camera_available),
                    style = MaterialTheme.typography.bodyMedium
                )
                GalleryButton(
                    isDecodingImage = isDecodingImage,
                    onPickImage = onPickImage
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_back))
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPanel(
    permissionRequested: Boolean,
    isDecodingImage: Boolean,
    onRequestPermission: () -> Unit,
    onPickImage: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        M3ePanel(
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(24.dp),
            prominent = true
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
                Text(stringResource(R.string.title_camera_permission_required), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (permissionRequested) {
                        stringResource(R.string.message_camera_permission_denied)
                    } else {
                        stringResource(R.string.message_camera_permission_prompt)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                M3eActionButton(
                    text = stringResource(R.string.action_grant_camera),
                    onClick = onRequestPermission,
                    prominent = true,
                    icon = Icons.Default.QrCodeScanner
                )
                GalleryButton(
                    isDecodingImage = isDecodingImage,
                    onPickImage = onPickImage
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_back))
                }
            }
        }
    }
}
