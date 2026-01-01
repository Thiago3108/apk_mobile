package com.thiago.apk_mobile.util

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

class DriveHelper(context: Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {

    private val credential = GoogleAccountCredential.usingOAuth2(
        context, setOf(DriveScopes.DRIVE_FILE)
    ).apply {
        selectedAccount = account.account
    }

    val drive: Drive = Drive.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("Apk Mobile").build()

}