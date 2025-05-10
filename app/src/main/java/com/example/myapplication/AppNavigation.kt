package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.theme.screens.ChatScreen
import com.example.myapplication.ui.theme.screens.LoginScreen
import com.example.myapplication.ui.theme.screens.PrivateChatScreen
import com.example.myapplication.ui.theme.screens.SearchScreen
import com.example.myapplication.ui.theme.screens.SignUpScreen
import com.example.myapplication.viewmodel.AuthViewModel
@Composable
fun AppNavigation(navController: NavHostController, authViewModel: AuthViewModel) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) { LoginScreen(navController, authViewModel) }
        composable(Screen.SignUp.route) { SignUpScreen(navController, authViewModel) }
        composable(Screen.ChatScreen.route) {
            ChatScreen(navController)
        }
        composable("search") {
            SearchScreen(navController, authViewModel)
        }
        composable("private-chat/{recipient}") { backStackEntry ->
            val recipient = backStackEntry.arguments?.getString("recipient") ?: ""
            PrivateChatScreen(navController, recipient)
        }
    }
}