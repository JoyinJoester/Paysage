package joyin.takgi.paysage.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

private const val DurationForward = 300
private const val DurationBack = 280
private const val EasyNotesFadeDuration = 300
private const val EasyNotesScaleDuration = 400
private const val EasyNotesScale = 0.9f

private val monicaNavEasing = CubicBezierEasing(0.6f, 0.0f, 0.4f, 1.0f)

fun paysageScreenEnter(): EnterTransition =
    fadeIn(animationSpec = tween(EasyNotesFadeDuration)) +
        scaleIn(
            initialScale = EasyNotesScale,
            animationSpec = tween(EasyNotesScaleDuration)
        )

fun paysageScreenExit(): ExitTransition =
    fadeOut(animationSpec = tween(EasyNotesFadeDuration)) +
        scaleOut(
            targetScale = EasyNotesScale,
            animationSpec = tween(EasyNotesScaleDuration)
        )

fun paysageSlideInFromRight(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = DurationForward, easing = monicaNavEasing),
        initialOffsetX = { fullWidth -> fullWidth / 8 }
    ) + fadeIn(animationSpec = tween(durationMillis = DurationBack, easing = monicaNavEasing))

fun paysageSlideOutToRight(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = DurationBack, easing = monicaNavEasing),
        targetOffsetX = { fullWidth -> fullWidth / 8 }
    ) + fadeOut(animationSpec = tween(durationMillis = DurationForward / 2, easing = monicaNavEasing))

fun paysageParallaxExitToLeft(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = DurationForward, easing = monicaNavEasing),
        targetOffsetX = { fullWidth -> -fullWidth / 12 }
    ) + fadeOut(animationSpec = tween(durationMillis = DurationForward / 2, easing = monicaNavEasing))

fun paysageParallaxEnterFromLeft(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = DurationBack, easing = monicaNavEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 12 }
    ) + fadeIn(animationSpec = tween(durationMillis = DurationBack, easing = monicaNavEasing))

fun paysageEasyNotesContentTransform(): ContentTransform =
    paysageScreenEnter()
        .togetherWith(paysageScreenExit())

fun paysageForwardContentTransform(): ContentTransform =
    paysageEasyNotesContentTransform()

fun paysageBackwardContentTransform(): ContentTransform =
    paysageEasyNotesContentTransform()

@Composable
fun <T> PaysageAnimatedPage(
    targetState: T,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    contentKey: (targetState: T) -> Any? = { it },
    isForward: (initialState: T, targetState: T) -> Boolean = { _, _ -> true },
    content: @Composable AnimatedContentScope.(targetState: T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            if (isForward(initialState, targetState)) {
                paysageForwardContentTransform()
            } else {
                paysageBackwardContentTransform()
            }
        },
        contentAlignment = contentAlignment,
        contentKey = contentKey,
        label = "paysage_page_transition",
        content = content
    )
}
