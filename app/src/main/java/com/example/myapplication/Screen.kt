package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.theme.screens.ChatRoomScreen
import com.example.myapplication.ui.theme.screens.LoginScreen
import com.example.myapplication.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ChatScreen : Screen("chatscreen/{recipient}") {
        fun createRoute(recipient: String) = "chatscreen/$recipient"
    }
}



