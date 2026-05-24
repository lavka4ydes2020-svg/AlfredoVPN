package com.v2ray.ang.handler

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages encrypted storage of sensitive server fields (UUID, publicKey, shortId)
 * using EncryptedSharedPreferences backed by AndroidKeyStore.
 *
 * Keys follow the pattern: "{guid}_id", "{guid}_pk", "{guid}_sid"
 * where guid is the server's unique identifier in MMKV.
 */
object EncryptedPrefsManager {

    private const val PREFS_NAME = "secure_server_fields"
    private const val FIELD_ID = "id"
    private const val FIELD_PK = "pk"
    private const val FIELD_SID = "sid"

    private var prefs: SharedPreferences? = null

    /**
     * Initializes the encrypted preferences. Must be called once from Application.onCreate().
     */
    fun init(context: Context) {
        if (prefs != null) return

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Saves a secure field value for the given server guid.
     */
    fun saveSecureField(guid: String, field: String, value: String) {
        prefs?.edit()?.putString("${guid}_$field", value)?.apply()
    }

    /**
     * Saves all sensitive fields from the server config to encrypted storage.
     * Call after the profile has been saved to MMKV.
     */
    fun saveSecureFields(guid: String, id: String?, publicKey: String?, shortId: String?) {
        id?.let { saveSecureField(guid, FIELD_ID, it) }
        publicKey?.let { saveSecureField(guid, FIELD_PK, it) }
        shortId?.let { saveSecureField(guid, FIELD_SID, it) }
    }

    /**
     * Retrieves a secure field value for the given server guid.
     * Returns null if the field was never encrypted (legacy profile).
     */
    fun getSecureField(guid: String, field: String): String? {
        return prefs?.getString("${guid}_$field", null)
    }

    /**
     * Gets the encrypted UUID for a server, or null if not found (legacy profile).
     */
    fun getSecureId(guid: String): String? = getSecureField(guid, FIELD_ID)

    /**
     * Gets the encrypted publicKey for a server, or null if not found (legacy profile).
     */
    fun getSecurePublicKey(guid: String): String? = getSecureField(guid, FIELD_PK)

    /**
     * Gets the encrypted shortId for a server, or null if not found (legacy profile).
     */
    fun getSecureShortId(guid: String): String? = getSecureField(guid, FIELD_SID)

    /**
     * Removes all secure fields for the given server guid.
     * Call when a server profile is deleted from MMKV.
     */
    fun removeSecureFields(guid: String) {
        prefs?.edit()?.apply {
            remove("${guid}_$FIELD_ID")
            remove("${guid}_$FIELD_PK")
            remove("${guid}_$FIELD_SID")
            apply()
        }
    }
}
