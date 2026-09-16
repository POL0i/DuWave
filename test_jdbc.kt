import androidx.sqlite.driver.JdbcSQLiteDriver

fun main() {
    val driver = JdbcSQLiteDriver("jdbc:sqlite::memory:")
    println(driver)
}
