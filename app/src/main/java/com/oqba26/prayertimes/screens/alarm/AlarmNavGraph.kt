package com.oqba26.prayertimes.screens.alarm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.oqba26.prayertimes.models.Alarm
import com.oqba26.prayertimes.utils.AlarmUtils

@Composable
fun AlarmNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    // Use immutable List for state, which is a better practice in Compose
    var alarms by remember { mutableStateOf<List<Alarm>>(AlarmUtils.loadAlarms(context)) }

    fun saveAndReschedule(alarm: Alarm) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        // Always create a new list for the state update to trigger recomposition
        alarms = if (index != -1) {
            alarms.toMutableList().apply { this[index] = alarm }
        } else {
            alarms + alarm
        }
        AlarmUtils.saveAlarms(context, alarms)
        AlarmUtils.scheduleAlarm(context, alarm)
        navController.popBackStack()
    }

    fun deleteAndCancel(alarm: Alarm) {
        AlarmUtils.cancelAlarm(context, alarm)
        alarms = alarms.filter { it.id != alarm.id } // filter already returns a new List
        AlarmUtils.saveAlarms(context, alarms)
        navController.popBackStack()
    }

    NavHost(navController = navController, startDestination = "alarm_list") {
        composable("alarm_list") {
            AlarmListScreen(
                alarms = alarms,
                onAddAlarm = { navController.navigate("alarm_detail/new") },
                onAlarmClick = { alarm ->
                    val alarmJson = Gson().toJson(alarm)
                    navController.navigate("alarm_detail/$alarmJson")
                },
                onToggleAlarm = { alarm, isEnabled ->
                    val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                    if (isEnabled) {
                        AlarmUtils.scheduleAlarm(context, updatedAlarm)
                    } else {
                        AlarmUtils.cancelAlarm(context, updatedAlarm)
                    }
                    val index = alarms.indexOfFirst { it.id == alarm.id }
                    if (index != -1) {
                        // Create a new list for the state update
                        alarms = alarms.toMutableList().apply { this[index] = updatedAlarm }
                        AlarmUtils.saveAlarms(context, alarms)
                    }
                }
            )
        }

        composable(
            route = "alarm_detail/{alarmJson}",
            arguments = listOf(navArgument("alarmJson") { type = NavType.StringType })
        ) {
            val alarmJson = it.arguments?.getString("alarmJson")
            // Handle "new" case for creating a new alarm
            val alarm = if (alarmJson == "new") null else Gson().fromJson(alarmJson, Alarm::class.java)
            CreateEditAlarmScreen(
                existingAlarm = alarm,
                onSave = { updatedAlarm -> saveAndReschedule(updatedAlarm) },
                onDelete = { alarmToDelete -> deleteAndCancel(alarmToDelete) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}