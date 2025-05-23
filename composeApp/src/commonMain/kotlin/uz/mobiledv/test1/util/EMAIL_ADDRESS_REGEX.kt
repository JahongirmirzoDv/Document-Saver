package uz.mobiledv.test1.util

// A reasonably common regex for email validation.
// For more robust validation, consider a library or more complex regex.
private val EMAIL_ADDRESS_REGEX =
    Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && EMAIL_ADDRESS_REGEX.matches(email)
}