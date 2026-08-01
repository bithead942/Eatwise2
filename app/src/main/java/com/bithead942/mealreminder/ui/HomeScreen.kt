package com.bithead942.mealreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bithead942.mealreminder.R
import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.data.MealReminder
import com.bithead942.mealreminder.ui.theme.BadgeGrey
import com.bithead942.mealreminder.ui.theme.Cyan
import com.bithead942.mealreminder.ui.theme.DisabledPill
import com.bithead942.mealreminder.ui.theme.Divider
import com.bithead942.mealreminder.ui.theme.InkBlue
import com.bithead942.mealreminder.ui.theme.TextMuted
import com.bithead942.mealreminder.ui.theme.TextPrimary
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: AppState,
    now: Long,
    onToggleMeal: (Int) -> Unit,
    onSetTime: (Int, LocalTime) -> Unit,
    onAddMeal: () -> Unit,
    onRemoveMeal: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }

    // Only one meal can be acted on at a time: the first that has not been eaten yet.
    val activeMealId = state.meals.firstOrNull { !it.isCompleted }?.id
    val allComplete = state.meals.isNotEmpty() && activeMealId == null

    var addPromptDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(allComplete) { if (!allComplete) addPromptDismissed = false }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val compact = maxHeight < 640.dp
        val timeSize = if (compact) 24.sp else 28.sp
        val rowHeight: Dp = if (compact) 62.dp else 74.dp

        Column(modifier = Modifier.fillMaxSize()) {
            BrandHeader(onOpenSettings = onOpenSettings)
            HorizontalDivider(color = Divider)
            // The most recently marked meal is the last completed one in list order.
            val lastCompletedIndex = state.meals.indexOfLast { it.isCompleted }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(state.meals, key = { _, meal -> meal.id }) { index, meal ->
                    val isActive = meal.id == activeMealId
                    val isMostRecentCompleted = meal.isCompleted && index == lastCompletedIndex
                    MealRow(
                        position = index + 1,
                        meal = meal,
                        now = now,
                        enabled = isActive,
                        // Edit only the next meal or the most recently marked one; hide it on
                        // earlier completed meals, disable it on later (not-yet-active) meals.
                        showEdit = !(meal.isCompleted && !isMostRecentCompleted),
                        editEnabled = isActive || isMostRecentCompleted,
                        timeSize = timeSize,
                        rowHeight = rowHeight,
                        onToggle = { onToggleMeal(meal.id) },
                        onEdit = { editing = meal.id }
                    )
                    HorizontalDivider(color = Divider)
                }
                item {
                    Text(
                        text = stringResource(R.string.extra_reminder),
                        color = Cyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAddMeal)
                            .padding(vertical = 18.dp)
                    )
                }
            }
        }

        if (allComplete && !addPromptDismissed) {
            AlertDialog(
                onDismissRequest = { addPromptDismissed = true },
                title = { Text(stringResource(R.string.add_reminder_title)) },
                text = { Text(stringResource(R.string.add_reminder_message)) },
                confirmButton = {
                    TextButton(onClick = { onAddMeal(); addPromptDismissed = true }) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { addPromptDismissed = true }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }

        val meal = state.meals.firstOrNull { it.id == editing }
        if (meal != null) {
            MealEditDialog(
                meal = meal,
                position = state.meals.indexOfFirst { it.id == meal.id } + 1,
                canRemove = state.meals.size > 1,
                onDismiss = { editing = null },
                onSetTime = { time -> onSetTime(meal.id, time); editing = null },
                onRemove = { onRemoveMeal(meal.id); editing = null }
            )
        }
    }
}

@Composable
private fun BrandHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu), tint = TextPrimary)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.brand),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.brand_tagline),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Cyan
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun MealRow(
    position: Int,
    meal: MealReminder,
    now: Long,
    enabled: Boolean,
    showEdit: Boolean,
    editEnabled: Boolean,
    timeSize: androidx.compose.ui.unit.TextUnit,
    rowHeight: Dp,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = rowHeight).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = BadgeGrey, modifier = Modifier.size(22.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("$position", color = Color.White, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(if (meal.isCompleted) R.string.ate_at else R.string.eat_at),
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = TextMuted
            )
            Text(
                text = formatTime(meal.displayTime),
                fontSize = timeSize,
                fontWeight = FontWeight.Light,
                color = TextPrimary
            )
            if (meal.isSnoozed(now)) {
                Text(
                    text = stringResource(R.string.snoozed_until, formatTime(meal.snoozedUntil)),
                    fontSize = 10.sp,
                    color = Cyan
                )
            }
        }
        if (meal.isCompleted) {
            IconButton(onClick = onToggle) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.mark_not_eaten),
                    tint = Cyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            EatButton(enabled = enabled, onClick = onToggle)
        }
        if (showEdit) {
            IconButton(onClick = onEdit, enabled = editEnabled, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_meal, position),
                    tint = if (editEnabled) InkBlue else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }
    }
}

@Composable
private fun EatButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) Cyan else DisabledPill,
        modifier = Modifier
            .widthIn(min = 76.dp)
            .height(34.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.eat),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}
