package com.softmintindia.pgsdk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher

class PaymentHelper {


    // Function to initiate UPI payment with ActivityResultLauncher
    fun initiateUpiPayment(
        context: Context,
        amount: String,
        upiId: String,
        name: String,
        note: String,
        txnId: String,
        url: String,
        upiAppPackage: String,
        upiPaymentLauncher: ActivityResultLauncher<Intent>
    ) {
        // Construct the UPI payment URL
        val upiPaymentUrl = buildUpiPaymentUrl(
            payeeAddress = upiId,
            payeeName = name,
            merchantCode = "7322",
            transactionId = txnId,
            transactionReferenceId = txnId,
            transactionNote = note,
            amount = amount,
            url = url
        )

        // Create an intent to launch the UPI app
        val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(upiPaymentUrl))

        // Set the UPI app package in the intent
        upiIntent.setPackage(upiAppPackage)

        // Check if the UPI app is available and launch it
        val resolveInfo = context.packageManager.queryIntentActivities(upiIntent, 0)
        if (resolveInfo.isNotEmpty()) {
            // Launch UPI app using the ActivityResultLauncher
            upiPaymentLauncher.launch(upiIntent)
        } else {
            // Show message if the specified UPI app is not found
            Toast.makeText(context, "The specified UPI app is not installed", Toast.LENGTH_SHORT).show()
            // Launch the app store to install the app
            launchAppInStore(context, upiAppPackage)
        }
    }


    // Function to check if the app is installed
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }


    // Function to launch the app in the Play Store
    private fun launchAppInStore(context: Context, packageName: String) {
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        context.startActivity(playStoreIntent)
    }

    private fun buildUpiPaymentUrl(
        payeeAddress: String,
        payeeName: String,
        merchantCode: String,
        transactionId: String,
        transactionReferenceId: String,
        transactionNote: String,
        amount: String,
        currency: String = "INR",
        url: String
    ): String {
        return Uri.parse("upi://pay")
            .buildUpon()
            .appendQueryParameter("pa", payeeAddress)  // Payee UPI ID
            .appendQueryParameter("pn", payeeName)     // Payee Name
            .appendQueryParameter("mc", merchantCode) // Merchant Code
            .appendQueryParameter("tid", transactionId) // Transaction ID
            .appendQueryParameter("tr", transactionReferenceId) // Transaction Reference ID
            .appendQueryParameter("tn", transactionNote) // Transaction Note
            .appendQueryParameter("am", amount) // Amount
            .appendQueryParameter("cu", currency) // Currency
            .appendQueryParameter("url", url) // URL
            .build()
            .toString()
    }




    // Handle the result of the UPI payment (in your activity or composable)
    fun handleUpiPaymentResponse(
        context: Context,
        data: Intent?
    ) {
        if (data == null) {
            Toast.makeText(context, "No response received.", Toast.LENGTH_SHORT).show()
            return
        }

        val response = data.getStringExtra("response")
        if (response.isNullOrEmpty()) {
            Toast.makeText(context, "Empty response from UPI app.", Toast.LENGTH_SHORT).show()
            return
        }

        val responseMap = response.split("&")
            .mapNotNull { param ->
                val pair = param.split("=")
                if (pair.size == 2) pair[0] to pair[1] else null
            }
            .toMap()

        val responseCode = responseMap["Status"] // Extracting status from the parsed response

        when (responseCode?.uppercase()) {
            "SUCCESS" -> {
                Toast.makeText(context, "Payment Successful", Toast.LENGTH_SHORT).show()
            }
            "FAILURE" -> {
                Toast.makeText(context, "Payment Failed", Toast.LENGTH_SHORT).show()
            }
            "SUBMITTED" -> {
                Toast.makeText(context, "Payment Pending", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Payment Status Unknown", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
