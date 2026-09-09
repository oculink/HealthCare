package com.fyp.healthcare

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Auth is Google Sign-In via Credential Manager, backed by Firebase Auth.
 * There are no local passwords any more - the account IS the Firebase user.
 *
 * On sign-in we also upsert a `users/{uid}` document in Firestore; that doc is
 * the anchor the rest of the app's data (readings, medications, activity) will
 * hang off once those managers get their sync layer.
 */

data class Account(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
)

/** null on success; a short message on failure; "cancelled" when the user backed out. */
sealed class SignInResult {
    data object Success : SignInResult()
    data object Cancelled : SignInResult()
    data class Failed(val message: String) : SignInResult()
}

class UserManager(context: Context) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun isSignedIn(): Boolean = auth.currentUser != null

    fun currentAccount(): Account? = auth.currentUser?.let { u ->
        Account(
            uid = u.uid,
            name = u.displayName?.takeIf { it.isNotBlank() }
                ?: u.email?.substringBefore("@")
                ?: "User",
            email = u.email.orEmpty(),
            photoUrl = u.photoUrl?.toString(),
        )
    }

    /** Shows the Google account picker, then signs into Firebase. */
    suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        val webClientId = activityContext.getString(R.string.default_web_client_id)

        val option = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val idToken: String = try {
            val response = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
            val cred = response.credential
            if (cred !is CustomCredential ||
                cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Failed("Unexpected credential type")
            }
            GoogleIdTokenCredential.createFrom(cred.data).idToken
        } catch (e: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            return SignInResult.Failed("No Google account on this device. Add one in Settings, then try again.")
        } catch (e: GetCredentialException) {
            return SignInResult.Failed("Google sign-in failed: ${e.message ?: e.type}")
        }

        return try {
            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(firebaseCred).awaitResult()
            val user = result.user ?: return SignInResult.Failed("Firebase sign-in returned no user")
            upsertUserDoc(user)
            SignInResult.Success
        } catch (e: FirebaseNetworkException) {
            SignInResult.Failed("Couldn't reach the server. Check your internet connection and try again.")
        } catch (e: Exception) {
            SignInResult.Failed("Sign-in error: ${e.message}")
        }
    }

    /**
     * Force a token refresh so the Firestore SDK has the fresh auth state before the first
     * read. Without this, a `get()` fired right after sign-in can reach the backend
     * unauthenticated and be denied by the rules. Best-effort.
     */
    suspend fun ensureFreshToken() {
        runCatching { auth.currentUser?.getIdToken(true)?.awaitResult() }
    }

    /** Signs out of Firebase immediately, and clears the Credential Manager state in the background. */
    fun signOut(appContext: Context) {
        auth.signOut()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                CredentialManager.create(appContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }

    private fun upsertUserDoc(user: FirebaseUser) {
        // NOTE: never write "name" here - that field is the health-profile name, owned by
        // ProfileManager. Writing the Google display name to it on every sign-in used to
        // clobber the onboarding name (and blank it for accounts with no display name,
        // forcing re-onboarding). The Google name goes in a separate "accountName" field.
        db.collection("users").document(user.uid).set(
            mapOf(
                "accountName" to user.displayName.orEmpty(),
                "email" to user.email.orEmpty(),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastLoginAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
    }
}

