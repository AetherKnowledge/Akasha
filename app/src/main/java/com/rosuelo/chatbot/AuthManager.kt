package com.rosuelo.chatbot

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

            emit(AuthResponse.Success(getCurrentUser()))
        }
        catch (e: Exception){
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    fun signInWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow{
        try{
            supabase.auth.signInWith(Email){
                email = emailValue
                password = passwordValue
            }

            emit(AuthResponse.Success(getCurrentUser()))
        }
        catch (e: Exception){
            emit(AuthResponse.Error(e.localizedMessage))
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

            emit(AuthResponse.Success(getCurrentUser()))
        }
        catch(e: Exception){
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    fun getCurrentUser(): UserData{

        if(supabase.auth.currentSessionOrNull() == null || supabase.auth.currentSessionOrNull()?.user == null || supabase.auth.currentSessionOrNull()?.user?.email == null){
            throw Exception("User not found")
        }

        return UserData(
            id = supabase.auth.currentSessionOrNull()!!.user!!.id,
            email = supabase.auth.currentSessionOrNull()!!.user!!.email!!
        )
    }
}

data class UserData(
    val id: String,
    val email: String
)

sealed interface AuthResponse {
    data class Success(
        val userData: UserData
    ) : AuthResponse
    data class Error(val message: String?) : AuthResponse
}