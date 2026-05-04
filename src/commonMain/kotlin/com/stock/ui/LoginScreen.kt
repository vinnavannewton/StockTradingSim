package com.stock.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stock.api.SupabaseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onWaitingForAuth: () -> Unit = {},
    onAuthFailed: () -> Unit = {}
) {
    val scope        = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var isSignUp    by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    var successMsg  by remember { mutableStateOf("") }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    // Animated pulse for the logo orb
    val pulse = rememberInfiniteTransition(label = "logoPulse")
    val orbScale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    val orbGlow by pulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "orbGlow"
    )

    // Floating particles animation
    val particleTransition = rememberInfiniteTransition(label = "particles")
    val particleY1 by particleTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "py1"
    )
    val particleY2 by particleTransition.animateFloat(
        initialValue = 15f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(3500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "py2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SFGradient.bgRadial),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background orbs
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-120).dp, y = (-200).dp)
                .graphicsLayer { translationY = particleY1 }
                .clip(CircleShape)
                .background(SFColor.Accent.copy(alpha = 0.05f))
                .blur(100.dp)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 140.dp, y = 260.dp)
                .graphicsLayer { translationY = particleY2 }
                .clip(CircleShape)
                .background(SFColor.Indigo.copy(alpha = 0.04f))
                .blur(80.dp)
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { 40 },
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Logo
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size((90 * orbScale).dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SFColor.Accent.copy(alpha = orbGlow * 0.3f))
                            .blur(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size((72 * orbScale).dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SFGradient.accentBrand),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("StockFlow", color = SFColor.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp)
                Text("Next-Gen Trading Simulator", color = SFColor.TextSecondary, fontSize = 14.sp)

                Spacer(Modifier.height(36.dp))

                // Login Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SFShape.XXLarge)
                        .background(SFGradient.loginCard)
                        .border(1.dp, SFColor.Border.copy(alpha = 0.6f), SFShape.XXLarge)
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(SFShape.Medium).background(SFColor.Surface2),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf(false to "Log In", true to "Sign Up").forEach { (su, label) ->
                            val selected = isSignUp == su
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        brush = (if (selected) SFGradient.accentBrand else SolidColor(Color.Transparent)) as Brush
                                    )
                                    .clickable { if (!isLoading) isSignUp = su }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (selected) Color.White else SFColor.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    LoginField(
                        email,
                        { email = it; errorMsg = "" },
                        "Email address",
                        { Icon(Icons.Default.Email, null, tint = SFColor.TextMuted, modifier = Modifier.size(20.dp)) },
                        null,
                        VisualTransformation.None,
                        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    LoginField(
                        password,
                        { password = it; errorMsg = "" },
                        "Password",
                        { Icon(Icons.Default.Lock, null, tint = SFColor.TextMuted, modifier = Modifier.size(20.dp)) },
                        {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = SFColor.TextMuted, modifier = Modifier.size(20.dp))
                            }
                        },
                        if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    AnimatedVisibility(
                        visible = errorMsg.isNotEmpty(),
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Text(errorMsg, color = SFColor.Loss, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) { errorMsg = "Please fill in all fields"; return@Button }
                            scope.launch {
                                isLoading = true
                                val res = if (isSignUp) SupabaseManager.signUp(email, password) else SupabaseManager.signIn(email, password)
                                res.fold(onSuccess = { onLoginSuccess() }, onFailure = { errorMsg = it.message ?: "Auth failed" })
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, disabledBackgroundColor = SFColor.Surface2),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                            .background(if (isLoading) SolidColor(SFColor.Surface2) else SFGradient.accentBrand, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) CircularProgressIndicator(color = SFColor.Accent, modifier = Modifier.size(20.dp))
                            else Text(if (isSignUp) "Create Account" else "Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Google Login
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                onWaitingForAuth() // Set state to WAITING_FOR_AUTH before launching browser
                                val result = SupabaseManager.signInWithGoogle()
                                // On Android, signInWith(Google) returns Success once the browser opens.
                                // The real auth completion happens via deep link → sessionStatus collector.
                                result.onFailure {
                                    errorMsg = it.message ?: "Google login failed"
                                    onAuthFailed() // Go back to login screen on failure
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SFColor.Border),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("Continue with Google", color = SFColor.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = SFColor.TextMuted, fontSize = 13.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = SFColor.TextPrimary,
            cursorColor = SFColor.Accent,
            focusedBorderColor = SFColor.Accent,
            unfocusedBorderColor = SFColor.Border,
            backgroundColor = SFColor.Surface2.copy(alpha = 0.5f)
        )
    )
}
