/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonVPN.
 * 
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.ui.login

import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.protonvpn.android.R
import com.protonvpn.android.components.BaseActivityV2
import com.protonvpn.android.components.CompressedTextWatcher
import com.protonvpn.android.components.ContentLayout
import com.protonvpn.android.databinding.ActivityLoginBinding
import com.protonvpn.android.ui.home.HomeActivity
import com.protonvpn.android.ui.onboarding.WelcomeDialog
import com.protonvpn.android.utils.AndroidUtils.launchActivity
import com.protonvpn.android.utils.Constants.SIGNUP_URL
import com.protonvpn.android.utils.Constants.URL_SUPPORT_ASSIGN_VPN_CONNECTION
import com.protonvpn.android.utils.DeepLinkActivity
import com.protonvpn.android.utils.ViewUtils.hideKeyboard
import com.protonvpn.android.utils.getThemeColor
import com.protonvpn.android.utils.overrideMemoryClear
import com.protonvpn.android.utils.toSafeUtf8ByteArray
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.exhaustive
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import javax.inject.Inject

@ContentLayout(R.layout.activity_login)
class LoginActivity : BaseActivityV2<ActivityLoginBinding, LoginViewModel>(),
        KeyboardVisibilityEventListener, Observer<LoginState> {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Prevent screen capture etc. to record user password
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        super.onCreate(savedInstanceState)
        if (viewModel.userData.isLoggedIn) {
            launchActivity<HomeActivity>()
            finish()
            return
        }
        initInputFields()
        checkIfOpenedFromWeb()
        initClickListeners()
        KeyboardVisibilityEvent.setEventListener(this, this)
        viewModel.loginState.observe(this@LoginActivity, this@LoginActivity)
    }

    private fun initClickListeners() = with(binding) {
        buttonLogin.setOnClickListener { attemptLogin() }
        textCreateAccount.setOnClickListener { openUrl(SIGNUP_URL) }
        textNeedHelp.setOnClickListener {
            val dialog =
                MaterialDialog.Builder(this@LoginActivity).theme(Theme.DARK).title(R.string.loginNeedHelp)
                    .customView(R.layout.dialog_help, true).negativeText(R.string.cancel).show()
            initNeedHelpDialog(dialog.customView!!)
        }
        buttonAssignVpnConnections.setOnClickListener { openUrl(URL_SUPPORT_ASSIGN_VPN_CONNECTION) }
        buttonReturnToLogin.setOnClickListener { viewModel.onBackToLogin() }
    }

    private fun initInputFields() {
        with(binding) {
            switchStartWithDevice.setOnCheckedChangeListener { _, isChecked ->
                viewModel.userData.connectOnBoot = isChecked
            }
            switchStartWithDevice.visibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) GONE else VISIBLE
            switchStartWithDevice.isChecked = viewModel.userData.connectOnBoot
            editEmail.addTextChangedListener(getTextWatcher(editEmail))
            editPassword.addTextChangedListener(getTextWatcher(editPassword))
            editPassword.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin()
                    return@OnEditorActionListener true
                }
                false
            })

            editEmail.setText(viewModel.userData.user)
        }
    }

    private fun checkIfOpenedFromWeb() {
        if (intent.getBooleanExtra(DeepLinkActivity.FROM_DEEPLINK, false)) {
            binding.editEmail.setText(intent.getStringExtra(DeepLinkActivity.USER_NAME))
            WelcomeDialog.showDialog(supportFragmentManager, WelcomeDialog.DialogType.WELCOME)
        }
    }

    override fun onVisibilityChanged(isOpen: Boolean) {
        val visibility = if (isOpen) View.GONE else View.VISIBLE
        with(binding) {
            textCreateAccount.visibility = visibility
            textNeedHelp.visibility = visibility
            layoutCredentials.gravity = if (isOpen) Gravity.TOP else Gravity.CENTER_VERTICAL

            val params = protonLogo.layoutParams as ConstraintLayout.LayoutParams
            params.verticalBias = if (isOpen) 0.1f else 0.5f
            protonLogo.layoutParams = params
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (currentFocus is EditText) {
                val emailRect = Rect()
                val passRect = Rect()
                binding.editEmail.getGlobalVisibleRect(emailRect)
                binding.editPassword.getGlobalVisibleRect(passRect)
                if (!emailRect.contains(event.rawX.toInt(), event.rawY.toInt()) &&
                        !passRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    hideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun getTextWatcher(editText: AppCompatEditText?): TextWatcher {
        return object : CompressedTextWatcher() {
            override fun afterTextChanged(editable: Editable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val color = if (editable.toString() == "")
                        ContextCompat.getColor(this@LoginActivity, R.color.lightGrey)
                    else
                        this@LoginActivity.getThemeColor(R.attr.colorAccent)
                    editText?.backgroundTintList = ColorStateList.valueOf(color)
                }
            }
        }
    }

    private fun initNeedHelpDialog(view: View) {
        view.findViewById<View>(R.id.buttonResetPassword)
                .setOnClickListener { openUrl("https://account.protonvpn.com/reset-password") }
        view.findViewById<View>(R.id.buttonForgotUser)
                .setOnClickListener { openUrl("https://account.protonvpn.com/forgot-username") }
        view.findViewById<View>(R.id.buttonLoginProblems)
                .setOnClickListener { openUrl("https://protonvpn.com/support/login-problems/") }
        view.findViewById<View>(R.id.buttonGetSupport)
                .setOnClickListener { openUrl("https://protonvpn.com/support") }
    }

    private fun attemptLogin() = with(binding) {
        inputEmail.error = null
        inputPassword.error = null

        val email = editEmail.text.toString()
        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(email)) {
            inputEmail.error = getString(R.string.error_field_required)
            focusView = editEmail
            cancel = true
        }

        if (TextUtils.isEmpty(editPassword.text)) {
            inputPassword.error = getString(R.string.error_field_required)
            focusView = editPassword
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            hideKeyboard()
            viewModel.userData.user = email

            login()
            loadingContainer.setRetryListener(::login)
        }
    }

    private var loginJob: Job? = null

    override fun onVpnPrepareFailed() {
        loginJob?.cancel()
        viewModel.onVpnPrepareFailed()
    }

    private fun login() = with(binding) {
        if (loginJob?.isActive != true) {
            loginJob = lifecycleScope.launch {
                viewModel.login(this@LoginActivity, editEmail.text.toString(),
                    editPassword.text!!.toSafeUtf8ByteArray())
            }
        }
    }

    override fun onBackPressed() {
        val handled = viewModel.onBackPressed()
        if (!handled) {
            super.onBackPressed()
        }
    }

    override fun onChanged(loginState: LoginState) {
        with(binding) {
            binding.layoutConnectionAllocationHelp.isVisible =
                loginState == LoginState.ConnectionAllocationPrompt
            when (loginState) {
                is LoginState.EnterCredentials -> {
                    loadingContainer.switchToEmpty()
                }
                is LoginState.Success -> {
                    launchActivity<HomeActivity>()
                    editPassword.text?.overrideMemoryClear()
                    editPassword.clearComposingText()
                    finish()
                }
                is LoginState.InProgress -> {
                    loadingContainer.switchToLoading()
                }
                is LoginState.GuestHoleActivated -> {
                    loadingContainer.switchToLoading(getString(R.string.guestHoleActivated))
                }
                is LoginState.Error -> {
                    if (loginState.retryRequest.not())
                        loadingContainer.setRetryListener(loadingContainer::switchToEmpty)
                    loadingContainer.switchToRetry(loginState.error)
                }
                is LoginState.UnsupportedAuth -> {
                    loadingContainer.switchToEmpty()
                    Toast.makeText(this@LoginActivity,
                        R.string.toastLoginAuthVersionError, Toast.LENGTH_LONG).show()
                }
                is LoginState.ConnectionAllocationPrompt -> {
                    loadingContainer.switchToEmpty()
                }
                is LoginState.RetryLogin -> attemptLogin()
            }.exhaustive
        }
    }
}
