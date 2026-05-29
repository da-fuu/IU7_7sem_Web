package com.ppo.timetracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isRegisterMode) "Регистрация" else "Вход", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Логин") })
        if (isRegisterMode) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") })
        }
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation()
        )
        if (isRegisterMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                Text("Администратор")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (isRegisterMode) {
                viewModel.register(login, name, pass, isAdmin)
            } else {
                viewModel.login(login, pass)
            }
        }) {
            Text(if (isRegisterMode) "Зарегистрироваться" else "Войти")
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Регистрация")
        }

        when (val s = state) {
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            is AuthState.Success -> Text(s.message, color = MaterialTheme.colorScheme.primary)
            else -> {}
        }
    }
}
