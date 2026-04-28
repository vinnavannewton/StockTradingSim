package com.stock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stock.api.SupabaseManager
import kotlinx.coroutines.launch

private val Background = Color(0xFF0B0F17)
private val Surface1   = Color(0xFF111827)
private val Surface2   = Color(0xFF1F2937)
private val Accent     = Color(0xFF60A5FA)
private val Good       = Color(0xFF34D399)
private val Bad        = Color(0xFFF87171)
private val Text1      = Color(0xFFF9FAFB)
private val Text2      = Color(0xFF9CA3AF)
private val Border     = Color(0xFF334155)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var isSignUp    by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    MaterialTheme {
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                    .width(380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(32.dp),
                       horizontalAlignment = Alignment.CenterHorizontally,
                       verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo / Title
                    Text("StockFlow", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isSignUp) "Create your account" else "Welcome back",
                            color = Text2, fontSize = 14.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = "" },
                        label = { Text("Email", color = Text2) },
                                      modifier = Modifier.fillMaxWidth(),
                                      singleLine = true,
                                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                      colors = TextFieldDefaults.outlinedTextFieldColors(
                                          textColor = Text1,
                                          cursorColor = Accent,
                                          focusedBorderColor = Accent,
                                          unfocusedBorderColor = Border
                                      )
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = "" },
                        label = { Text("Password", color = Text2) },
                                      modifier = Modifier.fillMaxWidth(),
                                      singleLine = true,
                                      visualTransformation = PasswordVisualTransformation(),
                                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                      colors = TextFieldDefaults.outlinedTextFieldColors(
                                          textColor = Text1,
                                          cursorColor = Accent,
                                          focusedBorderColor = Accent,
                                          unfocusedBorderColor = Border
                                      )
                    )

                    // Error message
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = Bad, fontSize = 12.sp)
                    }

                    // Main button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMsg = "Email and password cannot be empty"
                                return@Button
                            }
//                             if (isSignUp && password.length < 6) {
//                                 errorMsg = "Password must be at least 6 characters"
//                                 return@Button
//                             }
                            scope.launch {
                                isLoading = true
                                errorMsg = ""
                                val result = if (isSignUp)
                                SupabaseManager.signUp(email.trim(), password)
                                else
                                    SupabaseManager.signIn(email.trim(), password)

                                    result.fold(
                                        onSuccess = { onLoginSuccess() },
                                                onFailure = { e ->
                                                    errorMsg = when {
                                                        e.message?.contains("Invalid login") == true -> "Invalid email or password"
                                                        e.message?.contains("already registered") == true -> "An account with this email already registered. Try logging in."
                                                        else -> "Something went wrong. Try again."
                                                    }
                                                }
                                    )
                                    isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                           enabled = !isLoading,
                           colors = ButtonDefaults.buttonColors(backgroundColor = Accent),
                           shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Background, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (isSignUp) "Create Account" else "Log In",
                                    color = Background, fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Toggle login/signup
                    TextButton(onClick = { isSignUp = !isSignUp; errorMsg = "" }) {
                        Text(
                            if (isSignUp) "Already have an account? Log in" else "New here? Create an account",
                                color = Text2, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
