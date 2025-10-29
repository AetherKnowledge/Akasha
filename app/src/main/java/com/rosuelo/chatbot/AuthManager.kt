package com.rosuelo.chatbot

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rosuelo.chatbot.SupabaseProvider.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.UUID

class AuthManager(private val context: Context) {
    private val supabase = SupabaseProvider.supabase

    fun signUpWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow{
        try{
            supabase.auth.signUpWith(Email){
                email = emailValue
                password = passwordValue
            }

            emit(AuthResponse.Success(getCurrentUser()!!))
        }
        catch (e: Exception){
            emit(AuthResponse.Error(convertErrorToMessage(e)))
        }
    }

    fun signInWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow{
        try{
            supabase.auth.signInWith(Email){
                email = emailValue
                password = passwordValue
            }

            emit(AuthResponse.Success(getCurrentUser()!!))
        }
        catch (e: Exception){
            emit(AuthResponse.Error(convertErrorToMessage(e)))
        }
    }

    fun createNonce(): String{
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun loginGoogleUser(): Flow<AuthResponse> = flow{
        val hashedNonce = createNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId("614257994853-49f920emdvbn5sc4p7a41ddk9etd2tg3.apps.googleusercontent.com")
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        try{
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(result.credential.data)

            val googleIdToken = googleIdTokenCredential.idToken

            supabase.auth.signInWith(IDToken){
                idToken = googleIdToken
                provider = Google
            }

            emit(AuthResponse.Success(getCurrentUser()!!))
        }
        catch(e: Exception){
            Log.e("AuthManager", "Google sign-in failed", e)
            emit(AuthResponse.Error(convertErrorToMessage(e)))
        }
    }


}

@Serializable
data class UserData(
    val id: String,
    val email: String,
    val name: String? = null,
    val avatar: String? = null
)

sealed interface AuthResponse {
    data class Success(
        val userData: UserData
    ) : AuthResponse
    data class Error(val message: String?) : AuthResponse
}

suspend fun getCurrentUser(): UserData?{
    supabase.auth.awaitInitialization()
    var session = supabase.auth.currentSessionOrNull()

    if(session == null || session.user == null || session.user?.email == null){
        return null
    }

    return supabase.from("profile").select(){
        filter {
            eq("id", session.user!!.id)
        }
    }.decodeSingle<UserData>()
}

private fun convertErrorToMessage(e: Exception): String{
    return when{
        e.localizedMessage?.lowercase()?.contains("invalid login credentials") == true -> "Invalid email or password."
        e.localizedMessage?.lowercase()?.contains("user already registered") == true -> "User already exists."
        e.localizedMessage?.lowercase()?.contains("weak_password") == true -> "The password is too weak."
        else -> "An unknown error occurred ${e.localizedMessage}"
    }
}