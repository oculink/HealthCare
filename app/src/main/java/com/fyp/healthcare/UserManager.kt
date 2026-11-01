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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Auth is Google Sign-In via Credential Manager, or a username + password, both backed by
 * Firebase Auth. There are no other credentials - the account IS the Firebase user.
 *
 * The username half is stored as an email address behind the scenes (Firebase only accepts
 * addresses); see [syntheticEmail]. Users never see it.
 *
 * Username + password needs the Email/Password provider switched on in the Firebase console
 * (Authentication -> Sign-in method), or both methods below fail with
 * ERROR_OPERATION_NOT_ALLOWED. Anonymous auth is not used.
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

/** Outcome of [UserManager.connectGoogle]. NeedsPassword is the caller's cue to ask once. */
sealed class LinkResult {
    data object Success : LinkResult()
    data object Cancelled : LinkResult()

    /** Firebase won't link on a stale session; the caller should collect the password and retry. */
    data object NeedsPassword : LinkResult()
    data class Failed(val message: String) : LinkResult()
}

/**
 * The Google address this device last signed in with, so the Get Started button can name the
 * account it is about to open instead of offering a generic "Continue with Google". It is a
 * hint, never an auto sign-in: the picker still runs and the person still chooses. It survives
 * sign-out (that is when the button is shown) and is dropped when the account is deleted.
 *
 * Only the address is kept, on this device, in app-private storage.
 */
object GoogleHint {

    private const val PREFS = "auth_hint"
    private const val KEY_LAST_GOOGLE_EMAIL = "last_google_email"

    fun remember(context: Context, email: String?) {
        val clean = email?.takeIf { it.isNotBlank() && !isSyntheticEmail(it) } ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_GOOGLE_EMAIL, clean).apply()
    }

    fun last(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_GOOGLE_EMAIL, null)

    fun forget(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LAST_GOOGLE_EMAIL).apply()
    }
}

class UserManager(context: Context) {

    private val appContext = context.applicationContext
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun isSignedIn(): Boolean = auth.currentUser != null

    fun currentAccount(): Account? = auth.currentUser?.let { u ->
        Account(
            uid = u.uid,
            name = u.displayName?.takeIf { it.isNotBlank() }
                ?: u.email?.substringBefore("@")
                ?: "User",
            email = displayAccountEmail(u.email),
            photoUrl = u.photoUrl?.toString(),
        )
    }

    /** Shows the Google account picker, then signs into Firebase. */
    suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        val idToken = when (val token = requestGoogleIdToken(activityContext)) {
            is TokenOutcome.Cancelled -> return SignInResult.Cancelled
            is TokenOutcome.Failed -> return SignInResult.Failed(token.message)
            is TokenOutcome.Token -> token.idToken
        }

        return try {
            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(firebaseCred).awaitResult()
            val user = result.user ?: return SignInResult.Failed("Couldn't sign you in. Try again.")
            GoogleHint.remember(activityContext, user.email)
            upsertUserDoc(user)
            SignInResult.Success
        } catch (e: FirebaseNetworkException) {
            SignInResult.Failed(NETWORK_ERROR)
        } catch (e: Exception) {
            SignInResult.Failed("Sign-in error: ${e.message}")
        }
    }

    /**
     * Attaches a Google account to the signed-in username account. The uid does NOT change,
     * so both doors stay open afterwards: signing in with the username or with Google lands
     * on the same account and the same `users/{uid}` data.
     *
     * Linking counts as a sensitive operation, so Firebase refuses it on a session it
     * considers stale (`ERROR_REQUIRES_RECENT_LOGIN`) - which is the normal case for anyone
     * who signed in on a previous app run, since Firebase persists the session. Rather than
     * always asking for a password up front, this returns [LinkResult.NeedsPassword] and the
     * caller asks once. [password] is that retry.
     */
    suspend fun connectGoogle(activityContext: Context, password: String? = null): LinkResult {
        val user = auth.currentUser
            ?: return LinkResult.Failed("You're not signed in, so there's nothing to connect.")
        if (isGoogleConnected()) {
            return LinkResult.Failed("Google is already connected to this account.")
        }

        if (!password.isNullOrBlank() && isUsernameAccount()) {
            // The password provider is keyed to the synthetic address on user.email, which is
            // exactly the credential to prove.
            val address = user.email
                ?: return LinkResult.Failed("This account has no username on file to confirm.")
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(address, password)).awaitResult()
            } catch (e: FirebaseAuthException) {
                return LinkResult.Failed(authMessage(e.errorCode, e.message, AuthOp.Link))
            } catch (e: FirebaseNetworkException) {
                return LinkResult.Failed(NETWORK_ERROR)
            }
        }

        val idToken = when (val token = requestGoogleIdToken(activityContext)) {
            is TokenOutcome.Cancelled -> return LinkResult.Cancelled
            is TokenOutcome.Failed -> return LinkResult.Failed(token.message)
            is TokenOutcome.Token -> token.idToken
        }

        return try {
            user.linkWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitResult()
            GoogleHint.remember(
                activityContext,
                user.providerData.firstOrNull { it.providerId == GOOGLE_PROVIDER }?.email,
            )
            upsertUserDoc(user)
            LinkResult.Success
        } catch (e: FirebaseAuthException) {
            // Only hand back to the password prompt if we haven't just come from it, so a
            // wrong password can't bounce the user straight back into the same dialog.
            if (e.errorCode == "ERROR_REQUIRES_RECENT_LOGIN" && password.isNullOrBlank()) {
                LinkResult.NeedsPassword
            } else {
                LinkResult.Failed(authMessage(e.errorCode, e.message, AuthOp.Link))
            }
        } catch (e: FirebaseNetworkException) {
            LinkResult.Failed(NETWORK_ERROR)
        } catch (e: Exception) {
            LinkResult.Failed("Couldn't connect Google: ${e.message}")
        }
    }

    /** Provider ids on the signed-in account: `password`, `google.com`, or both. */
    fun currentProviders(): Set<String> =
        auth.currentUser?.providerData?.map { it.providerId }?.toSet().orEmpty()

    fun isUsernameAccount(): Boolean = PASSWORD_PROVIDER in currentProviders()

    fun isGoogleConnected(): Boolean = GOOGLE_PROVIDER in currentProviders()

    /** A username account with nothing attached to it - the state that has no recovery path. */
    fun needsGoogleLink(): Boolean = isUsernameAccount() && !isGoogleConnected()

    /**
     * The connected Google address, read from its own provider entry rather than
     * `user.email`: the account keeps the private synthetic address as its primary, so the
     * username door stays open and users never see it.
     */
    fun connectedGoogleEmail(): String? = auth.currentUser?.providerData
        ?.firstOrNull { it.providerId == GOOGLE_PROVIDER }
        ?.email

    /**
     * Replaces this account's password. Firebase wants proof of the old one first, and there
     * is no email path to reset it, so the caller always supplies the current password.
     */
    suspend fun changePassword(current: String, next: String): SignInResult {
        val user = auth.currentUser ?: return SignInResult.Failed("You're not signed in.")
        if (!isUsernameAccount()) {
            return SignInResult.Failed("This account signs in with Google, so there's no CareApp password to change.")
        }
        if (next.length < MIN_PASSWORD_LENGTH) {
            return SignInResult.Failed("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }
        if (current == next) {
            return SignInResult.Failed("That's already your password.")
        }
        val address = user.email
            ?: return SignInResult.Failed("This account has no username on file.")

        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(address, current)).awaitResult()
            user.updatePassword(next).awaitResult()
            SignInResult.Success
        } catch (e: FirebaseAuthException) {
            SignInResult.Failed(authMessage(e.errorCode, e.message, AuthOp.Password))
        } catch (e: FirebaseNetworkException) {
            SignInResult.Failed(NETWORK_ERROR)
        }
    }

    /**
     * Permanently deletes the signed-in account: its profile document, its readings,
     * medications and appointments, any family code it owns, and its link to a patient it
     * was caring for - then the Firebase Auth user itself.
     *
     * Every write here is one the security rules already let a user make to their own data
     * or to a patient they are linked to, so no Admin SDK or Cloud Function is needed. Two
     * things it cannot undo: the counts this account added to the shared `stats/vitals`
     * baseline, and whatever a linked caregiver has cached on their own phone (they stop
     * seeing the patient, but their local copy ages out rather than being wiped by us).
     */
    suspend fun deleteAccount(password: String): SignInResult {
        val user = auth.currentUser ?: return SignInResult.Failed("You're not signed in.")
        val uid = user.uid

        // Deleting a user is a sensitive operation: prove the password first, which also
        // satisfies the recent-session requirement Firebase enforces on user.delete().
        if (isUsernameAccount()) {
            val address = user.email
                ?: return SignInResult.Failed("This account has no username on file.")
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(address, password)).awaitResult()
            } catch (e: FirebaseAuthException) {
                return SignInResult.Failed(authMessage(e.errorCode, e.message, AuthOp.Delete))
            } catch (e: FirebaseNetworkException) {
                return SignInResult.Failed(NETWORK_ERROR)
            }
        }

        return try {
            deleteOwnedCollections(uid)
            deleteOwnFamilyCode(uid)
            // Best-effort: the account must still be deletable if a stale/inconsistent link
            // can't be cleaned up. Deleting the user doc above already removes all access.
            runCatching { leaveLinkedPatient(uid) }
            db.collection("users").document(uid).delete().awaitResult()
            user.delete().awaitResult()
            GoogleHint.forget(appContext)
            SignInResult.Success
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            SignInResult.Failed("For your security, sign out and back in, then try deleting the account again.")
        } catch (e: FirebaseNetworkException) {
            SignInResult.Failed(NETWORK_ERROR)
        } catch (e: Exception) {
            SignInResult.Failed("Couldn't delete the account: ${e.message}")
        }
    }

    private suspend fun deleteOwnedCollections(uid: String) {
        val me = db.collection("users").document(uid)
        for (name in OWNED_SUBCOLLECTIONS) {
            val docs = me.collection(name).get().awaitResult().documents
            var batch = db.batch()
            docs.forEachIndexed { index, doc ->
                batch.delete(doc.reference)
                // Firestore caps a batch at 500 writes, so commit in chunks.
                if ((index + 1) % 400 == 0) {
                    batch.commit().awaitResult()
                    batch = db.batch()
                }
            }
            batch.commit().awaitResult()
        }
    }

    private suspend fun deleteOwnFamilyCode(uid: String) {
        val code = db.collection("users").document(uid).get().awaitResult()
            .getString("familyCode")
            ?.takeIf { it.isNotBlank() } ?: return
        db.collection("familyCodes").document(code).delete().awaitResult()
    }

    /**
     * If this account is acting as a caregiver, remove it from that one patient's list.
     *
     * This deliberately uses the patient uid stored on the account's own doc instead of a
     * `whereArrayContains` query across `users`/`familyCodes`. The security rules only let a
     * signed-in user list their own documents (and family-code listing is denied outright),
     * so those collection queries fail with PERMISSION_DENIED and used to block the whole
     * deletion. The single link on this account is all that needs cleaning here.
     */
    private suspend fun leaveLinkedPatient(uid: String) {
        val me = db.collection("users").document(uid).get().awaitResult()
        val patientUid = me.getString("linkedPatientUid")?.takeIf { it.isNotBlank() } ?: return
        val patient = db.collection("users").document(patientUid).get().awaitResult()
        if (!patient.exists()) return

        @Suppress("UNCHECKED_CAST")
        val remaining = (patient.get("caretakers") as? List<Map<String, Any?>>).orEmpty()
            .filterNot { it["uid"] == uid }
        patient.reference.update(
            mapOf(
                "caretakerUids" to FieldValue.arrayRemove(uid),
                "caretakers" to remaining,
            ),
        ).awaitResult()

        patient.getString("familyCode")?.takeIf { it.isNotBlank() }?.let { code ->
            runCatching {
                db.collection("familyCodes").document(code)
                    .update("caretakerUids", FieldValue.arrayRemove(uid)).awaitResult()
            }
        }
    }

    /** The Credential Manager half of the Google flow, shared by sign-in and [connectGoogle]. */
    private suspend fun requestGoogleIdToken(activityContext: Context): TokenOutcome {
        val webClientId = activityContext.getString(R.string.default_web_client_id)

        val option = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
            val cred = response.credential
            if (cred !is CustomCredential ||
                cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                TokenOutcome.Failed("Unexpected credential type")
            } else {
                TokenOutcome.Token(GoogleIdTokenCredential.createFrom(cred.data).idToken)
            }
        } catch (e: GetCredentialCancellationException) {
            TokenOutcome.Cancelled
        } catch (e: NoCredentialException) {
            TokenOutcome.Failed("No Google account on this device. Add one in Settings, then try again.")
        } catch (e: GetCredentialException) {
            TokenOutcome.Failed("Google sign-in didn't work: ${e.message ?: e.type}")
        }
    }

    private sealed interface TokenOutcome {
        data class Token(val idToken: String) : TokenOutcome
        data object Cancelled : TokenOutcome
        data class Failed(val message: String) : TokenOutcome
    }

    /**
     * Creates a username + password account and leaves it signed in. The typed username
     * becomes the Firebase display name, so every greeting in the app keeps showing it.
     */
    suspend fun signUpWithUsername(username: String, password: String): SignInResult =
        passwordAuth(AuthOp.SignUp) {
            val name = username.trim()
            val created = auth.createUserWithEmailAndPassword(syntheticEmail(name), password)
                .awaitResult().user ?: return@passwordAuth null
            created.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build(),
            ).awaitResult()
            created
        }

    /** Signs in an existing username + password account. */
    suspend fun signInWithUsername(username: String, password: String): SignInResult =
        passwordAuth(AuthOp.SignIn) {
            auth.signInWithEmailAndPassword(syntheticEmail(username.trim()), password)
                .awaitResult().user
        }

    private suspend fun passwordAuth(op: AuthOp, call: suspend () -> FirebaseUser?): SignInResult {
        val user = try {
            call()
        } catch (e: FirebaseAuthException) {
            return SignInResult.Failed(authMessage(e.errorCode, e.message, op))
        } catch (e: FirebaseNetworkException) {
            return SignInResult.Failed(NETWORK_ERROR)
        } catch (e: Exception) {
            return SignInResult.Failed("Sign-in error: ${e.message}")
        } ?: return SignInResult.Failed("Couldn't sign you in. Try again.")

        upsertUserDoc(user)
        return SignInResult.Success
    }

    /**
     * Everything that has to happen between "Firebase says you're signed in" and "the app
     * is usable": token refresh, then rebuilding the local caches that sign-out wiped.
     * Both sign-in paths call this before navigating, or [afterAuth]-style routing misroutes
     * (it sees no profile) and screens flash empty / stale.
     */
    suspend fun finishSignIn(appContext: Context) {
        ensureFreshToken()
        runCatching { FamilyLink.restoreSession(appContext) }
        runCatching { CloudHydrator.hydrate(appContext) }
    }

    /**
     * Force a token refresh so the Firestore SDK has the fresh auth state before the first
     * read. Without this, a `get()` fired right after sign-in can reach the backend
     * unauthenticated and be denied by the rules. Best-effort. Called by [finishSignIn].
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
        val fields = mapOf(
            "accountName" to user.displayName.orEmpty(),
            "email" to displayAccountEmail(user.email),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "lastLoginAt" to FieldValue.serverTimestamp(),
        )
        // Keep a plain copy of the username: it's the login identifier for this account and
        // shouldn't depend on what Firebase's profile fields end up holding once Google is
        // connected. Google accounts have no username, so they don't get the field at all.
        val username = user.email?.takeIf { isSyntheticEmail(it) }?.substringBefore("@").orEmpty()
        db.collection("users").document(user.uid).set(
            if (username.isEmpty()) fields else fields + ("username" to username),
            SetOptions.merge(),
        )
    }
}

/** Firebase Auth rejects anything shorter than this, so the form checks it first. */
const val MIN_PASSWORD_LENGTH = 6

const val MIN_USERNAME_LENGTH = 3
const val MAX_USERNAME_LENGTH = 20

private const val NETWORK_ERROR =
    "Couldn't reach the server. Check your internet connection and try again."

/** Firebase's provider ids for the two doors this app offers. */
private const val GOOGLE_PROVIDER = "google.com"
private const val PASSWORD_PROVIDER = "password"

/** Everything this account owns outright, deleted with it. Mirrors firestore.rules. */
private val OWNED_SUBCOLLECTIONS = listOf("readings", "medications", "appointments")

/**
 * Firebase Auth has no bare-username credential - a password account must have an email
 * address - so a CareApp username is stored as an address on this reserved TLD. `.invalid`
 * can never be delegated or receive mail, so nothing can be sent to it, and it can't
 * collide with a real Google account. It stays hidden from the UI: see [displayAccountEmail].
 */
private const val USERNAME_EMAIL_DOMAIN = "@careapp.invalid"

/** Usernames are matched case-insensitively: `Alice` and `alice` are the same account. */
internal fun normalizeUsername(raw: String): String = raw.trim().lowercase(Locale.ROOT)

internal fun isSyntheticEmail(email: String?): Boolean =
    email?.endsWith(USERNAME_EMAIL_DOMAIN) == true

internal fun syntheticEmail(username: String): String =
    normalizeUsername(username) + USERNAME_EMAIL_DOMAIN

/** The address to show the user: blank for username accounts, real for Google ones. */
internal fun displayAccountEmail(email: String?): String =
    if (isSyntheticEmail(email)) "" else email.orEmpty()

private val USERNAME_PATTERN = Regex("^[a-z0-9][a-z0-9._-]*[a-z0-9]$")

/**
 * Letters, numbers, dot, underscore, dash; [MIN_USERNAME_LENGTH]-[MAX_USERNAME_LENGTH]
 * long; no leading or trailing punctuation. Also stays inside what Firebase accepts in the
 * local part of an address, so a valid username is never rejected by the backend.
 */
internal fun isValidUsername(raw: String): Boolean {
    val name = normalizeUsername(raw)
    return name.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH &&
        USERNAME_PATTERN.matches(name)
}

/**
 * Which operation a Firebase error came from. The same code means different things in
 * different places: `ERROR_EMAIL_ALREADY_IN_USE` during sign-up is "username taken", but
 * during a Google connect it is "that Google account is already someone's CareApp account".
 */
internal enum class AuthOp { SignIn, SignUp, Link, Password, Delete }

/**
 * Firebase hands back a machine code on every auth failure; show the user a sentence
 * instead. Codes are the Auth REST ones (also on [FirebaseAuthException.errorCode]);
 * anything unmapped falls back to the SDK's own message.
 */
internal fun authMessage(code: String?, fallback: String?, op: AuthOp = AuthOp.SignIn): String {
    if (op == AuthOp.Link) {
        return when (code) {
            // The Google identity picked is already owned by another CareApp account.
            "ERROR_EMAIL_ALREADY_IN_USE",
            "ERROR_CREDENTIAL_ALREADY_IN_USE",
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
            -> "That Google account already has its own CareApp account. Sign in with Google instead, or connect a Google account that's never been used here."
            "ERROR_REQUIRES_RECENT_LOGIN" ->
                "Confirm your password to finish connecting."
            "ERROR_PROVIDER_ALREADY_LINKED" -> "Google is already connected to this account."
            "ERROR_NETWORK_REQUEST_FAILED" -> NETWORK_ERROR
            else -> fallback ?: "Couldn't connect Google. Please try again."
        }
    }
    return when (code) {
        // Since 2023 sign-in returns one of these two rather than saying which half was wrong,
        // so an unknown account and a wrong password read the same way (no account enumeration).
        "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_WRONG_PASSWORD" -> "Wrong username or password."
        "ERROR_USER_NOT_FOUND" -> "No account with that username. Sign up first."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "That username is taken. Try another."
        "ERROR_INVALID_EMAIL" -> "That username isn't allowed."
        "ERROR_WEAK_PASSWORD" -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a minute, then try again."
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Username sign-in isn't enabled for this project yet. Enable Email/Password in the Firebase console (Authentication -> Sign-in method)."
        "ERROR_REQUIRES_RECENT_LOGIN" ->
            "For your security, type your password above to confirm it's you, then try again."
        "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "That Google account already has its own CareApp account. Sign in with Google instead, or connect a Google account that's never been used here."
        "ERROR_PROVIDER_ALREADY_LINKED" -> "Google is already connected to this account."
        "ERROR_NETWORK_REQUEST_FAILED" -> NETWORK_ERROR
        else -> fallback ?: "Something went wrong. Try again."
    }
}

