package com.azizbek.ikeaplace.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.azizbek.ikeaplace.Catalog
import com.azizbek.ikeaplace.FurnitureItem
import com.azizbek.ikeaplace.R
import com.azizbek.ikeaplace.ScreenshotSaver
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_OBJECTS = 20

private class Placed(val item: FurnitureItem, val anchorNode: AnchorNode, val modelNode: ModelNode)

/** Checks camera permission and ARCore availability, then shows the AR experience. */
@Composable
fun ArScreen(initialItemId: String?, onBack: () -> Unit, onCaptured: (android.graphics.Bitmap) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    var hasCamera by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }

    if (!hasCamera) {
        MessageScreen(stringResource(R.string.perm_camera), stringResource(R.string.grant), { launcher.launch(Manifest.permission.CAMERA) }, onBack)
        return
    }

    var availability by remember { mutableStateOf(ArCoreApk.getInstance().checkAvailability(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { availability = ArCoreApk.getInstance().checkAvailability(context) }
    LaunchedEffect(availability) {
        if (availability.isTransient) { delay(300); availability = ArCoreApk.getInstance().checkAvailability(context) }
    }
    when (availability) {
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArContent(initialItemId, onBack, onCaptured)
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED, ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD ->
            MessageScreen(stringResource(R.string.arcore_install), stringResource(R.string.install), {
                try { ArCoreApk.getInstance().requestInstall(activity, true) } catch (_: Exception) { availability = ArCoreApk.Availability.UNKNOWN_ERROR }
            }, onBack)
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> MessageScreen(stringResource(R.string.arcore_unsupported), onBack = onBack)
        ArCoreApk.Availability.UNKNOWN_CHECKING, ArCoreApk.Availability.UNKNOWN_TIMED_OUT ->
            Box(Modifier.fillMaxSize().background(Warm), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
        else -> MessageScreen(stringResource(R.string.arcore_error), stringResource(R.string.retry),
            { availability = ArCoreApk.getInstance().checkAvailability(context) }, onBack)
    }
}

@Composable
private fun ArContent(initialItemId: String?, onBack: () -> Unit, onCaptured: (android.graphics.Bitmap) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()

    var current by remember { mutableStateOf(Catalog.byId(initialItemId) ?: Catalog.items.first()) }
    var hasPlane by remember { mutableStateOf(false) }
    val placed = remember { mutableStateListOf<Placed>() }
    var selected by remember { mutableStateOf<Placed?>(null) }
    var moveMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var fatal by remember { mutableStateOf<String?>(null) }
    var hideUi by remember { mutableStateOf(false) }
    val frameRef = remember { arrayOfNulls<Frame>(1) }

    LaunchedEffect(message) { if (message != null) { delay(3000); message = null } }

    fun buildPlaced(anchor: Anchor, item: FurnitureItem): Placed? {
        return try {
            val anchorNode = AnchorNode(engine = engine, anchor = anchor)
            val instance = modelLoader.createModelInstance(item.asset)
            val model = ModelNode(modelInstance = instance, scaleToUnits = null).apply {
                isEditable = true // one/two-finger drag, pinch scale and twist rotate (no new anchors are created)
                editableScaleRange = 0.4f..2.5f
            }
            anchorNode.addChildNode(model)
            childNodes += anchorNode
            Placed(item, anchorNode, model)
        } catch (e: OutOfMemoryError) {
            anchor.detach(); message = context.getString(R.string.memory_error); null
        } catch (e: Exception) {
            anchor.detach(); message = context.getString(R.string.model_error); null
        }
    }

    fun remove(p: Placed) {
        childNodes.remove(p.anchorNode)
        p.anchorNode.anchor.detach()
        p.anchorNode.destroy()
        placed.remove(p)
        if (selected === p) selected = null
    }

    fun findPlaced(node: Node?): Placed? {
        var n = node
        while (n != null) {
            val hit = placed.firstOrNull { it.modelNode === n || it.anchorNode === n }
            if (hit != null) return hit
            n = n.parent
        }
        return null
    }

    fun onPlaneTap(x: Float, y: Float) {
        val hit = frameRef[0]?.hitTest(x, y)?.firstOrNull {
            val t = it.trackable
            t is Plane && t.isPoseInPolygon(it.hitPose) && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING
        }
        val anchor = hit?.createAnchorOrNull()
        if (anchor == null) { message = context.getString(R.string.no_surface); return }
        val old = selected
        if (moveMode && old != null) {
            remove(old)
            buildPlaced(anchor, old.item)?.let { placed += it; selected = it }
            moveMode = false
        } else {
            if (placed.size >= MAX_OBJECTS) { anchor.detach(); message = context.getString(R.string.limit_reached); return }
            buildPlaced(anchor, current)?.let { placed += it; selected = it }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            },
            onSessionUpdated = { session, frame ->
                frameRef[0] = frame
                val any = session.getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
                if (any != hasPlane) hasPlane = any
            },
            onSessionFailed = { e ->
                fatal = when (e) {
                    is CameraNotAvailableException -> context.getString(R.string.camera_error)
                    is UnavailableException -> context.getString(R.string.arcore_unsupported)
                    else -> context.getString(R.string.ar_error, e.message ?: e.javaClass.simpleName)
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { event, node ->
                    val hit = findPlaced(node)
                    if (hit != null) { selected = hit; moveMode = false } else onPlaneTap(event.x, event.y)
                },
            ),
        )

        if (fatal != null) {
            Box(Modifier.fillMaxSize().background(Warm)) {
                MessageScreen(fatal!!, stringResource(R.string.retry), { fatal = null }, onBack)
            }
            return@Box
        }

        if (!hideUi) {
            // top bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .9f))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Ink)
                }
                Spacer(Modifier.width(10.dp))
                val hint = when {
                    moveMode -> stringResource(R.string.move_hint)
                    !hasPlane -> stringResource(R.string.scanning)
                    else -> stringResource(R.string.tap_to_place)
                }
                Text(hint, color = Ink, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .9f)).padding(horizontal = 14.dp, vertical = 10.dp))
            }
            message?.let {
                Text(it, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .75f)).padding(16.dp))
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                selected?.let { sel ->
                    ControlPanel(
                        title = stringResource(R.string.selected, stringResource(sel.item.nameRes)),
                        moveActive = moveMode,
                        onMove = { moveMode = !moveMode },
                        onRotate = { val r = sel.modelNode.rotation; sel.modelNode.rotation = Float3(r.x, r.y + 15f, r.z) },
                        onScale = { f ->
                            val s = (sel.modelNode.scale.x * f).coerceIn(0.4f, 2.5f)
                            sel.modelNode.scale = Float3(s, s, s)
                        },
                        onDelete = { remove(sel); moveMode = false },
                        onDuplicate = {
                            val frame = frameRef[0]
                            if (placed.size >= MAX_OBJECTS) message = context.getString(R.string.limit_reached)
                            else try {
                                val pose = sel.anchorNode.anchor.pose.compose(Pose.makeTranslation(0.4f, 0f, 0f))
                                val anchor = frame?.session?.createAnchor(pose)
                                if (anchor != null) buildPlaced(anchor, sel.item)?.let { copy ->
                                    copy.modelNode.rotation = sel.modelNode.rotation
                                    copy.modelNode.scale = sel.modelNode.scale
                                    placed += copy; selected = copy
                                }
                            } catch (e: Exception) { message = context.getString(R.string.model_error) }
                        },
                        onReset = {
                            sel.modelNode.position = Float3(0f, 0f, 0f)
                            sel.modelNode.rotation = Float3(0f, 0f, 0f)
                            sel.modelNode.scale = Float3(1f, 1f, 1f)
                        },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .92f)).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Catalog.items, key = { it.id }) { item ->
                            val on = item.id == current.id
                            Image(painterResource(item.thumb), stringResource(item.nameRes),
                                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                                    .border(if (on) 3.dp else 0.dp, if (on) Ink else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable { current = item; moveMode = false })
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = {
                            scope.launch {
                                hideUi = true; delay(300)
                                val bmp = ScreenshotSaver.capture(activity)
                                hideUi = false
                                if (bmp != null) onCaptured(bmp) else message = context.getString(R.string.capture_err)
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White),
                    ) { Text("📷") }
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    title: String, moveActive: Boolean, onMove: () -> Unit, onRotate: () -> Unit, onScale: (Float) -> Unit,
    onDelete: () -> Unit, onDuplicate: () -> Unit, onReset: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .95f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SoftButton(stringResource(R.string.move) + if (moveActive) " ✓" else "", Modifier.weight(1f), onMove)
            SoftButton("⟳ " + stringResource(R.string.rotate), Modifier.weight(1f), onRotate)
            SoftButton("＋", Modifier.weight(.5f)) { onScale(1.1f) }
            SoftButton("－", Modifier.weight(.5f)) { onScale(1f / 1.1f) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SoftButton(stringResource(R.string.duplicate), Modifier.weight(1f), onDuplicate)
            SoftButton(stringResource(R.string.reset), Modifier.weight(1f), onReset)
            SoftButton(stringResource(R.string.delete), Modifier.weight(1f), onDelete)
        }
    }
}
