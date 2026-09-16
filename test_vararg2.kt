import java.util.Locale

fun main() {
    println(getLocalizedString("test: %1\$s", "World"))
}

fun getLocalizedString(key: String, vararg formatArgs: Any): String {
    return java.lang.String.format(Locale.US, key, *formatArgs)
}
