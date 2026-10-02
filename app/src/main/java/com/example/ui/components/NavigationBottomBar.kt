package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.viewmodel.AppScreen

@Composable
fun NavigationBottomBar(
    currentScreen: AppScreen,
    isTelugu: Boolean,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar {
        // 1. Dashboard
        NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onNavigate(AppScreen.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Dashboard"
                )
            },
            label = {
                Text(text = if (isTelugu) "హోమ్" else "Home")
            },
            modifier = Modifier.testTag("nav_dashboard")
        )

        // 2. Field Collections (NEW)
        NavigationBarItem(
            selected = currentScreen == AppScreen.FIELD_COLLECTIONS,
            onClick = { onNavigate(AppScreen.FIELD_COLLECTIONS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Paid,
                    contentDescription = "Field Collections"
                )
            },
            label = {
                Text(text = if (isTelugu) "వసూళ్లు" else "Collections")
            },
            modifier = Modifier.testTag("nav_field_collections")
        )

        // 3. Disrub (Loan Sourcing)
        val isDisrub = currentScreen == AppScreen.DISRUB_SOURCING ||
                currentScreen == AppScreen.DISRUB_NEW_JLG ||
                currentScreen == AppScreen.DISRUB_CGT_GRT ||
                currentScreen == AppScreen.LOAN_APPLICATION
        NavigationBarItem(
            selected = isDisrub,
            onClick = { onNavigate(AppScreen.DISRUB_SOURCING) },
            icon = {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "Disrub Sourcing"
                )
            },
            label = {
                Text(text = if (isTelugu) "రుణాలు" else "Disrub")
            },
            modifier = Modifier.testTag("nav_disrub")
        )

        // 4. Centers (Kendra Meetings)
        val isCollect = currentScreen == AppScreen.COLLECT_CENTERS ||
                currentScreen == AppScreen.COLLECT_MEETING
        NavigationBarItem(
            selected = isCollect,
            onClick = { onNavigate(AppScreen.COLLECT_CENTERS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Kendra Centers"
                )
            },
            label = {
                Text(text = if (isTelugu) "కేంద్రాలు" else "Centers")
            },
            modifier = Modifier.testTag("nav_centers")
        )

        // 5. Day-End Cash Balancing
        NavigationBarItem(
            selected = currentScreen == AppScreen.RECONCILIATION,
            onClick = { onNavigate(AppScreen.RECONCILIATION) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AssignmentTurnedIn,
                    contentDescription = "Cash Reconciliation"
                )
            },
            label = {
                Text(text = if (isTelugu) "క్యాష్ లెక్క" else "Reconcile")
            },
            modifier = Modifier.testTag("nav_reconcile")
        )
    }
}

