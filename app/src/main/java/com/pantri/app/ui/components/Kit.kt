package com.pantri.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EggAlt
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import com.pantri.app.data.Aisle
import com.pantri.app.domain.Limits
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri

val LocalHaptics = staticCompositionLocalOf { true }

/** Returns a function that taps the phone, unless haptics are switched off in Settings. */
@Composable
fun rememberHaptic(): (HapticFeedbackType) -> Unit {
    val on = LocalHaptics.current
    val h = LocalHapticFeedback.current
    return { type -> if (on) h.performHapticFeedback(type) }
}

fun Aisle.tone(): Tone = when (this) {
    Aisle.Produce, Aisle.Frozen, Aisle.Household -> Tone.Green
    Aisle.Meat -> Tone.Red
    Aisle.Dairy, Aisle.Drinks -> Tone.Yellow
    Aisle.Pantry, Aisle.Bakery, Aisle.Other -> Tone.Orange
}

fun Aisle.icon(): ImageVector = when (this) {
    Aisle.Produce -> Icons.Rounded.Eco
    Aisle.Meat -> Icons.Rounded.SetMeal
    Aisle.Dairy -> Icons.Rounded.EggAlt
    Aisle.Pantry -> Icons.Rounded.Kitchen
    Aisle.Bakery -> Icons.Rounded.BakeryDining
    Aisle.Frozen -> Icons.Rounded.AcUnit
    Aisle.Drinks -> Icons.Rounded.LocalDrink
    Aisle.Household -> Icons.Rounded.CleaningServices
    Aisle.Other -> Icons.Rounded.Category
}

private val cardShape = RoundedCornerShape(20.dp)

@Composable
fun PantriCard(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val p = MaterialTheme.pantri
    Column(
        modifier
            .shadow(6.dp, cardShape, ambientColor = p.shadow.copy(alpha = 0.10f), spotColor = p.shadow.copy(alpha = 0.14f))
            .background(p.card, cardShape)
            .padding(padding),
        content = content,
    )
}

/** Big gradient card with two soft circles behind the content for depth. */
@Composable
fun HeroCard(colors: List<Color>, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(14.dp, shape, ambientColor = colors.last().copy(alpha = 0.35f), spotColor = colors.last().copy(alpha = 0.45f))
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.12f), radius = size.width * 0.36f, center = Offset(size.width * 0.92f, size.height * 0.05f))
                drawCircle(Color(0xFFF6C62F).copy(alpha = 0.26f), radius = size.width * 0.24f, center = Offset(size.width * 0.78f, size.height * 1.02f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.18f, center = Offset(size.width * 0.05f, size.height * 0.95f))
            },
        content = content,
    )
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val (bg, fg) = MaterialTheme.pantri.tint(tone)
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = fg)
            Spacer(Modifier.size(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = Color.White)
            Spacer(Modifier.size(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
    }
}

@Composable
fun IconTile(icon: ImageVector, tone: Tone, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val (bg, fg) = MaterialTheme.pantri.tint(tone)
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = fg)
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    colors: List<Color> = MaterialTheme.pantri.heroGreen,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(10.dp, shape, ambientColor = colors.last().copy(alpha = 0.3f), spotColor = colors.last().copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: Tone = Tone.Green, icon: ImageVector? = null) {
    val (bg, fg) = MaterialTheme.pantri.tint(tone)
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(bg).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = fg)
            Spacer(Modifier.size(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    track: Color = Color.White.copy(alpha = 0.25f),
    color: Color = Color(0xFFF6C62F),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val d = Size(size.width - s, size.height - s)
            drawArc(track, 0f, 360f, false, Offset(s / 2, s / 2), d, style = androidx.compose.ui.graphics.drawscope.Stroke(s, cap = StrokeCap.Round))
            if (f > 0.001f) drawArc(color, -90f, 360f * f, false, Offset(s / 2, s / 2), d, style = androidx.compose.ui.graphics.drawscope.Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun LinearBar(fraction: Float, modifier: Modifier = Modifier, color: List<Color> = listOf(Color(0xFF6CC058), MaterialTheme.pantri.green)) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.height(9.dp).clip(CircleShape).background(MaterialTheme.pantri.line)) {
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }.clip(CircleShape).background(Brush.horizontalGradient(color)))
    }
}

@Composable
fun PantriCheck(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, label: String = "") {
    val p = MaterialTheme.pantri
    val haptic = rememberHaptic()
    val bg by animateColorAsState(if (checked) p.green else Color.Transparent, tween(180), label = "check")
    val scale by animateFloatAsState(if (checked) 1f else 0.96f, spring(Spring.DampingRatioMediumBouncy), label = "checkScale")
    // The box you see is 26 dp, but the tap area is 48 dp so it is easy to hit with a thumb.
    Box(
        modifier
            .size(48.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox) {
                haptic(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                onChange(!checked)
            }
            .semantics { contentDescription = if (checked) "Done, $label" else "Not done, $label" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(9.dp))
                .background(bg)
                .border(2.dp, if (checked) p.green else p.muted.copy(alpha = 0.5f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(checked, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)), exit = scaleOut()) {
                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.pantri.sub)
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val p = MaterialTheme.pantri
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(128.dp).clip(CircleShape).background(p.tint(Tone.Green).first))
            Box(Modifier.size(38.dp).offset(44.dp, (-38).dp).clip(CircleShape).background(p.yellow.copy(alpha = 0.85f)))
            Box(Modifier.size(22.dp).offset((-48).dp, 40.dp).clip(CircleShape).background(p.tomato.copy(alpha = 0.8f)))
            Box(
                Modifier.size(70.dp).shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = p.shadow.copy(alpha = 0.15f)).background(p.card, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(36.dp), tint = p.green) }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = p.sub, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun TopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        } else {
            Spacer(Modifier.size(8.dp))
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        actions()
    }
}

data class Tab(val route: String, val label: String, val icon: ImageVector)

val Tabs = listOf(
    Tab("home", "Lists", Icons.Rounded.ShoppingCart),
    Tab("recipes", "Recipes", Icons.AutoMirrored.Rounded.MenuBook),
    Tab("plan", "Plan", Icons.Rounded.CalendarMonth),
    Tab("budget", "Budget", Icons.Rounded.Wallet),
)

@Composable
fun FloatingNav(current: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = MaterialTheme.pantri
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(14.dp, shape, ambientColor = p.shadow.copy(alpha = 0.16f), spotColor = p.shadow.copy(alpha = 0.22f))
            .background(p.card, shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tabs.forEach { t ->
            val sel = t.route == current
            val bg by animateColorAsState(if (sel) p.tint(Tone.Green).first else Color.Transparent, tween(200), label = "navBg")
            val fg by animateColorAsState(if (sel) p.tint(Tone.Green).second else p.muted, tween(200), label = "navFg")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).selectable(selected = sel, role = Role.Tab) { onSelect(t.route) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(t.icon, null, Modifier.size(24.dp), tint = fg)
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = fg)
            }
        }
    }
}

@Composable
fun PantriField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    error: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    ime: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    maxLength: Int = Limits.NAME,
) {
    val p = MaterialTheme.pantri
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        modifier = modifier,
        label = { Text(label) },
        prefix = prefix?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onSend = { onDone?.invoke() }),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = p.card, unfocusedContainerColor = p.card, errorContainerColor = p.card,
            focusedIndicatorColor = p.green, unfocusedIndicatorColor = p.muted.copy(alpha = 0.4f), cursorColor = p.green,
            focusedLabelColor = p.green,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantriSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = MaterialTheme.pantri
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp), content = content)
    }
}

@Composable
fun PantriDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: String,
    onConfirm: () -> Unit,
    dismissText: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = MaterialTheme.pantri
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.card,
        shape = RoundedCornerShape(26.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, fontWeight = FontWeight.SemiBold, color = p.green) } },
        dismissButton = { TextButton(onDismiss) { Text(dismissText, color = p.sub) } },
    )
}
