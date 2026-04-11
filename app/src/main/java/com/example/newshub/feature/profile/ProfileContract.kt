package com.example.newshub.feature.profile

interface ProfileContract {
    interface View {
        fun showLoading(isLoading: Boolean)
        fun showMessage(messageRes: Int)
        fun navigateToHome()
        fun navigateToLogin()
        fun openImagePicker()
        fun renderName(firstName: String, lastName: String, fullName: String)
        fun renderAvatar(avatarUrl: String?)
        fun clearPasswordInputs()
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onScreenStarted()
        fun onBackClicked()
        fun onLogoutClicked()
        fun onUploadPhotoClicked()
        fun onAvatarSelected(bytes: ByteArray, mimeType: String)
        fun onNameChanged(firstName: String, lastName: String)
        fun onSaveNameClicked(firstName: String, lastName: String)
        fun onUpdatePasswordClicked(currentPassword: String, newPassword: String, confirmPassword: String)
    }
}

