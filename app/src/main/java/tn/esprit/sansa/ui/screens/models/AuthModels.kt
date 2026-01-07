package tn.esprit.sansa.ui.screens.models

enum class UserRole(val displayName: String) {
    CITIZEN("Citoyen"),
    TECHNICIAN("Technicien"),
    ADMIN("Administrateur")
}

data class SecurityQuestion(
    val question: String = "",
    val answer: String = ""
)

data class UserAccount(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.CITIZEN,
    val specialty: String? = null,
    val profilePicture: String? = null,
    var isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var isFirstLogin: Boolean = true,
    val invitedAt: Long? = null,
    val activatedAt: Long? = null,
    val phoneNumber: String = "",
    val workingZone: String = "",
    val coordinates: String = "",
    val securityQuestions: List<SecurityQuestion> = emptyList()
)
