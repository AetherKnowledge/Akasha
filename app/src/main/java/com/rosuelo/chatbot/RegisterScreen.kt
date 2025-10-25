package com.rosuelo.chatbot

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun RegisterScreen(
    onRegister: ((userData: UserData) -> Unit)? = null
) {
    val isLogin = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val authManager = remember {
        AuthManager(context)
    }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Gradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(isLogin.value)

            Spacer(modifier = Modifier.height(40.dp))

            GoogleSignInButton(
                onClick = {
                    authManager.loginGoogleUser()
                        .onEach { result ->
                            if (result is AuthResponse.Success) {
                                Log.d("auth", "Google Success " + result.userData.email)
                                onRegister?.invoke(result.userData)
                            } else if (result is AuthResponse.Error) {
                                Log.e("auth", "Google Failed " + result.message)
                            }
                            else {
                                Log.e("auth", "Google Failed")
                            }
                        }
                        .launchIn(coroutineScope)
                },
                isLogin = isLogin.value

            )

            OrSeparator()

            EmailSignIn(onClick = { emailValue, passwordValue ->
                if(isLogin.value){
                    authManager.signInWithEmail(emailValue, passwordValue)
                        .onEach { result ->
                            if (result is AuthResponse.Success) {
                                Log.d("auth", "Email Success " + result.userData.email)
                                onRegister?.invoke(result.userData)
                            } else if (result is AuthResponse.Error) {
                                Log.e("auth", "Email Failed" + result.message)
                            }
                            else {
                                Log.e("auth", "Email Failed")
                            }
                        }
                        .launchIn(coroutineScope)
                }
                else{
                    authManager.signUpWithEmail(emailValue, passwordValue)
                        .onEach { result ->
                            if (result is AuthResponse.Success) {
                                Log.d("auth", "Email Success " + result.userData.email)
                                onRegister?.invoke(result.userData)
                            } else if (result is AuthResponse.Error) {
                                Log.e("auth", "Email Failed" + result.message)
                            }
                            else {
                                Log.e("auth", "Email Failed")
                            }
                        }
                        .launchIn(coroutineScope)
                }
            })

            Spacer(modifier = Modifier.height(25.dp))

            TextButton(
                onClick = {
                    isLogin.value = !isLogin.value
                }
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        ) {
                            append(if(isLogin.value) "Don't have an account?   " else "Already have an account?   ")
                        }

                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            append(if(isLogin.value) "Sign up" else "Log in")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OrSeparator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 30.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )

        Text(
            text = "Or",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )
    }
}

@Composable
private fun EmailSignIn(onClick: (emailValue: String, passwordValue: String) -> Unit){
    var emailValue by remember {
        mutableStateOf("")
    }

    var passwordValue by remember {
        mutableStateOf("")
    }

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Email",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            value = emailValue,
            onValueChange = { newValue ->
                emailValue = newValue
            },
            placeholder = {
                Text(
                    text = "john.doe@example.com",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Password",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            value = passwordValue,
            onValueChange = { newValue ->
                passwordValue = newValue
            },
            placeholder = {
                Text(
                    text = "Enter your password",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(35.dp))

    Button(
        onClick = { onClick(emailValue, passwordValue) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Sign up",
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, isLogin: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = (if(isLogin) "Login" else "Sign in") + " With Google",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun Header(isLogin: Boolean = false) {


    Text(
        text = if(isLogin) "Login To Account" else "Create An Account",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Enter your personal data to create an account",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Preview
@Composable
private fun RegisterPreview() {
    ChatbotTheme {
        RegisterScreen()
    }
}

@Composable
fun Gradient() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.35f)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    )
}