package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsColors
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsFontFamily

@Composable
internal fun AchievementCupAnimationOverlay(
    achievementTitle: String,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val currentOnFinished = rememberUpdatedState(onFinished)
    val contentDescription = stringResource(R.string.achievement_celebration_animation)
    val shape = RoundedCornerShape(16.dp)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(shape)
                .background(AchievementsColors.Card)
                .border(1.dp, AchievementsColors.Border, shape)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = {
                        var completionDispatched = false

                        LottieAnimationView(context).apply {
                            setAnimation(R.raw.cup_achievement)
                            repeatCount = 0
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            this.contentDescription = contentDescription
                            addAnimatorListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    if (!completionDispatched) {
                                        completionDispatched = true
                                        currentOnFinished.value()
                                    }
                                }
                            })
                            playAnimation()
                        }
                    },
                    modifier = Modifier.size(280.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.achievement_celebration_message,
                    achievementTitle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = AchievementsColors.White,
                fontFamily = AchievementsFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
