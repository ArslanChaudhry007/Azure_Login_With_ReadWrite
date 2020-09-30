package com.example.azurelogin


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.example.azurelogin.model.Authloginazure
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.microsoft.graph.concurrency.ICallback
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.models.extensions.Drive
import com.microsoft.graph.requests.extensions.GraphServiceClient
import com.microsoft.identity.client.*
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileWriter
import java.io.IOException


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var SCOPES = arrayOf("User.Read")
    var AUTHORITY = "https://login.microsoftonline.com/common"
    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    var TAG = MainActivity::class.java.simpleName
    lateinit var sharedPreferences: SharedPreferences
    lateinit var data: String
    var authority = ArrayList<Authloginazure.Authority>()
    var fileName: String = "auth_cong_single_account"
    var context: Context = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        converRawIntoJson()
        writeStringAsFile(data, fileName)
        readToFile(context)
        initializeUI()


    }
    //creat json file like raw file
    private fun converRawIntoJson() {
        val audiance = Authloginazure.Audience()
        audiance.tenant_id = "6fa8f164-0360-4846-adce-875b7fd30a7d"
        audiance.type = "AzureADMyOrg"

        authority.add(
            Authloginazure.Authority(
                audiance,
                "AAD"
            )
        )
        val authLogin = Authloginazure(
            "SINGLE",
            authority,
            "WEBVIEW",
            "c5ac5b2d-ebfe-4041-8b61-2613c03109b6",
            "msauth.com.quest.iqprospects://auth"
        )

        data = Gson().toJson(authLogin)
    }
  //creat file and set json data on it
   private fun writeStringAsFile(fileContents: String, fileName: String) {
        val context: Context = this
        try {
            val out = FileWriter(File(context.filesDir, fileName))
            out.write(fileContents)
            out.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }
    //read data from file and call MSLA
    private fun readToFile(context: Context) {
        try {
            //get path of created file
            val file = File(context.filesDir, fileName)

            PublicClientApplication.createSingleAccountPublicClientApplication(
                this,
                file,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {

                        mSingleAccountApp = application
                        loadAccount()
                    }

                    override fun onError(exception: MsalException) {
                        displayError(exception)
                    }
                })
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())

        }

    }

    private fun initializeUI() {
        signIn.setOnClickListener(this)
        clearCache.setOnClickListener(this)
        callGraphSilent.setOnClickListener(this)
        callGraphInteractive.setOnClickListener(this)


//        PublicClientApplication.createSingleAccountPublicClientApplication(
//            this,
//            R.raw.new_auth_cong_single_account,
//            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
//                override fun onCreated(application: ISingleAccountPublicClientApplication) {
//
//                    mSingleAccountApp = application
//                    loadAccount()
//                }
//
//                override fun onError(exception: MsalException) {
//                    displayError(exception)
//                }
//            })
    }

    private fun displayError(exception: MsalException) {
        txt_log.setText(exception.toString())
    }

    private fun loadAccount() {
        if (mSingleAccountApp == null) {
            return
        }
        mSingleAccountApp!!.getCurrentAccountAsync(object : CurrentAccountCallback {
            override fun onAccountLoaded(@Nullable activeAccount: IAccount?) {
                // You can use the account data to update your UI or your app database.
                updateUI(activeAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (currentAccount == null) {
                    // Perform a cleanup task as the signed-in account changed.
                    performOperationOnSignOut()
                }
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }
        })
    }

    private fun performOperationOnSignOut() {
        var signOutText = "Signed Out."
        current_user.setText("")
        Toast.makeText(getApplicationContext(), signOutText, Toast.LENGTH_SHORT)
            .show()

    }

    private fun updateUI(activeAccount: IAccount?) {
        if (activeAccount != null) {
            signIn.setEnabled(false)
            clearCache.setEnabled(true)
            callGraphInteractive.setEnabled(true)
            callGraphSilent.setEnabled(true)
            current_user.setText(activeAccount.getUsername())
//            if (accessToken != null){
//                txt_log.setText(accessToken)
//            }
            sharedPreferences = getSharedPreferences("saveAccessToken", Context.MODE_PRIVATE)
            val accessToken = sharedPreferences.getString("ACCESSTOKEN", null)
            if (accessToken != null) {
                txt_log.setText(accessToken)
            }

        } else {
            signIn.setEnabled(true)
            clearCache.setEnabled(false)
            callGraphInteractive.setEnabled(false)
            callGraphSilent.setEnabled(false)
            current_user.setText("")
            txt_log.setText("")
        }

    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.signIn -> {
                if (mSingleAccountApp == null) {
                    return
                }
                mSingleAccountApp!!.signIn(
                    this,
                    "",
                    SCOPES,
                    getAuthInteractiveCallback()
                )
            }


            R.id.clearCache -> {
                if (mSingleAccountApp == null) {
                    return
                }
                mSingleAccountApp!!.signOut(object :
                    ISingleAccountPublicClientApplication.SignOutCallback {
                    override fun onSignOut() {
                        updateUI(null)
                        performOperationOnSignOut()
                    }

                    override fun onError(exception: MsalException) {
                        displayError(exception)
                    }

                })
                sharedPreferences = getSharedPreferences("saveAccessToken", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("ACCESSTOKEN", null)
                editor.apply()

            }
            R.id.callGraphInteractive -> {
                if (mSingleAccountApp == null) {
                    return;
                }
                mSingleAccountApp!!.acquireToken(this, SCOPES, getAuthInteractiveCallback())
            }

            R.id.callGraphSilent -> {
                if (mSingleAccountApp == null) {
                    return;
                }
                mSingleAccountApp!!.acquireTokenSilentAsync(
                    SCOPES,
                    AUTHORITY,
                    getAuthSilentCallback()
                )
            }
        }
    }

    fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                //Successfully got a token, use it to call a protected resource - MSGraph
                Log.d(TAG, "Successfully authenticated")
                updateUI(authenticationResult.account)
                updateUIAfterSignIn(authenticationResult.accessToken)
                sharedPreferences = getSharedPreferences("saveAccessToken", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("ACCESSTOKEN", authenticationResult.accessToken)
                editor.apply()
                //txt_log.setText(authenticationResult.toString())
                //call graph
                // callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                // Failed to acquireToken
                Log.d(TAG, "Authentication failed: $exception")
                displayError(exception)
            }

            override fun onCancel() {
                // User canceled the authentication
                Log.d(TAG, "User cancelled login.")
            }
        }
    }

    private fun updateUIAfterSignIn(authenticationResult: String) {
        txt_log.setText(authenticationResult)
    }

    private fun getAuthSilentCallback(): SilentAuthenticationCallback {
        return object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Log.d(TAG, "Successfully authenticated")
//                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                Log.d(TAG, "Authentication failed: $exception")
                displayError(exception)
            }
        }
    }

    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        val accessToken = authenticationResult.accessToken
        val graphClient = GraphServiceClient
            .builder()
            .authenticationProvider { request ->
                Log.d(TAG, "Authenticating request," + request.requestUrl)
                request.addHeader("Authorization", "Bearer $accessToken")
            }
            .buildClient()
        graphClient
            .me()
            .drive()
            .buildRequest()[object : ICallback<Drive> {
            override fun success(drive: Drive) {
                Log.d(TAG, "Found Drive " + drive.id)
                displayGraphResult(drive.rawObject)
            }

            private fun displayGraphResult(rawObject: JsonObject?) {
                txt_log.setText(rawObject.toString())

            }

            override fun failure(ex: ClientException?) {
                displayError(ex)
            }

            private fun displayError(ex: ClientException?) {
                runOnUiThread(Runnable {
                    Toast.makeText(
                        this@MainActivity,
                        ex.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                    txt_log.setText(ex.toString())
                })

            }
        }]
    }

}

