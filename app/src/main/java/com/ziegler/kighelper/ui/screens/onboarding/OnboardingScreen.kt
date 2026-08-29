package com.ziegler.kighelper.ui.screens.onboarding

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.ui.MainViewModel
import com.ziegler.kighelper.ui.OnboardingStep
import com.ziegler.kighelper.ui.OnboardingViewModel
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.SocialCardViewModel
import com.ziegler.kighelper.ui.navigation.AppRoutes
import kotlinx.coroutines.launch

private const val TABLET_SMALLEST_WIDTH_DP = 600

@Composable
fun OnboardingScreen(
    onboardingViewModel: OnboardingViewModel,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    socialCardViewModel: SocialCardViewModel,
    onComplete: (String?) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val steps = onboardingViewModel.steps
    val pagerState = rememberPagerState(pageCount = { steps.size })

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
    val isExpanded = isLandscape || isTablet

    val currentPage = pagerState.currentPage
    val isFirstPage = currentPage == 0
    val isLastPage = currentPage == steps.size - 1

    fun scrollToNext() {
        val next = pagerState.currentPage + 1
        if (next < steps.size) {
            coroutineScope.launch { pagerState.animateScrollToPage(next) }
        }
    }

    fun scrollToPrev() {
        val prev = pagerState.currentPage - 1
        if (prev >= 0) {
            coroutineScope.launch { pagerState.animateScrollToPage(prev) }
        }
    }

    fun finish(route: String? = null) {
        onboardingViewModel.completeOnboarding()
        onComplete(route)
    }

    BackHandler {
        if (!isFirstPage) {
            scrollToPrev()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PagerContent(
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        pagerState = pagerState,
                        steps = steps,
                        settingsViewModel = settingsViewModel,
                        mainViewModel = mainViewModel,
                        socialCardViewModel = socialCardViewModel,
                        onboardingViewModel = onboardingViewModel,
                        onNavigateToPhraseManagement = { onNavigateToRoute(AppRoutes.PHRASE_MANAGEMENT) },
                        onNavigateToSocialCardEdit = { onNavigateToRoute(AppRoutes.SOCIAL_CARD_EDIT) }
                    )
                }

                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StepIndicator(
                        steps = steps,
                        currentPage = currentPage
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                PagerContent(
                    modifier = Modifier.weight(1f).padding(top = 16.dp, bottom = 16.dp),
                    pagerState = pagerState,
                    steps = steps,
                    settingsViewModel = settingsViewModel,
                    mainViewModel = mainViewModel,
                    socialCardViewModel = socialCardViewModel,
                    onboardingViewModel = onboardingViewModel,
                    onNavigateToPhraseManagement = { onNavigateToRoute(AppRoutes.PHRASE_MANAGEMENT) },
                    onNavigateToSocialCardEdit = { onNavigateToRoute(AppRoutes.SOCIAL_CARD_EDIT) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StepIndicator(
                        steps = steps,
                        currentPage = currentPage
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationButtons(
                        isFirstPage = isFirstPage,
                        isLastPage = isLastPage,
                        onBack = ::scrollToPrev,
                        onNext = { if (isLastPage) finish() else scrollToNext() },
                        onSkip = { finish() }
                    )
                }
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp),
                horizontalAlignment = Alignment.End
            ) {
                NavigationButtons(
                    isFirstPage = isFirstPage,
                    isLastPage = isLastPage,
                    onBack = ::scrollToPrev,
                    onNext = { if (isLastPage) finish() else scrollToNext() },
                    onSkip = { finish() }
                )
            }
        }
    }
}

@Composable
private fun PagerContent(
    modifier: Modifier = Modifier,
    pagerState: androidx.compose.foundation.pager.PagerState,
    steps: List<OnboardingStep>,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    socialCardViewModel: SocialCardViewModel,
    onboardingViewModel: OnboardingViewModel,
    onNavigateToPhraseManagement: () -> Unit,
    onNavigateToSocialCardEdit: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        userScrollEnabled = false
    ) { page ->
        when (steps[page]) {
            OnboardingStep.WELCOME -> WelcomeStep()
            OnboardingStep.IMPORT_PHRASE -> ImportPhraseStep(
                viewModel = mainViewModel,
                onNavigateToEdit = onNavigateToPhraseManagement
            )
            OnboardingStep.SOCIAL_CARD -> SocialCardStep(
                socialCardViewModel = socialCardViewModel,
                onNavigateToEdit = onNavigateToSocialCardEdit
            )
            OnboardingStep.THEME -> ThemeStep(
                settingsViewModel = settingsViewModel
            )
            OnboardingStep.PERMISSION -> PermissionStep()
            OnboardingStep.UPDATE_CHECK -> UpdateCheckStep(
                viewModel = onboardingViewModel
            )
            OnboardingStep.COMPLETION -> CompletionStep()
        }
    }
}

@Composable
private fun StepIndicator(
    steps: List<OnboardingStep>,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, _ ->
            val isSelected = index == currentPage
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(durationMillis = 300),
                label = "step_color"
            )

            Box(
                modifier = Modifier
                    .size(if (isSelected) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun NavigationButtons(
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isFirstPage) {
            OutlinedButton(
                onClick = onSkip,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("跳过")
            }
        } else {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "上一步"
                )
            }
        }

        Button(
            onClick = onNext
        ) {
            Text(
                text = if (isLastPage) "开始使用" else "下一步",
                fontWeight = FontWeight.SemiBold
            )
            if (!isLastPage) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
