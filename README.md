# Terpal - Typed String Interpolation for Kotlin

Unlike Kotlin string-interpolation, Scala's string interpolation allows customizeability.
This becomes very important for DSLs that need to know "the $interpolated $parts of a input string"
before they are actually spliced into the resulting string.

For example, libraries like Doobie and Quill use a the string-prefix "sql" to specify SQL snippets such as:

```scala
sql"SELECT * FROM users WHERE id = $id AND name = $name"
```
Normally the value of "id" would be spliced directly into the string e.g. "SELECT * FROM users WHERE id = 1234 AND name = 'Joe'"
however this is highly problematic as it opens up the possibility of SQL injection attacks for example:
```scala
"SELECT * FROM users WHERE id = 1234; DROP TABLE users; AND name = 'Joe'"
```

Scala's string interpolation allows the library
to know that "id" is a variable and should be escaped before splicing it into the string. It uses the following API:

```scala
implicit class SqlInterpolator(val sc: StringContext) extends AnyVal {
  // Values of $dollar_sign_variables i.e. `id`, `name` are this list i.e. [1234, "Joe"]
  def sql(args: Any*): PreparedStatement = {
    // The string-parts ["SELECT * FROM users WHERE id = ", " AND name = " and ""] are this list
    val stringParts = sc.parts
    //Then we can construct safe prepared-statement
    val stmt = connection.prepareStatement(stringParts.mkString("?"))  // i.e. joinToString("?")
    for ((arg, i) <- args.zipWithIndex) {
      stmt.setObject(i + 1, arg)
    }
    return stmt
  }
}
```

This is a very powerful feature that allows libraries to create DSLs that are both safe and easy to use.
Sadly Kotlin does not have it.

Terpal remedies this problem with a compiler-plugin that contains these exact semantics.
Using Terpal, you would write the above as the following: 
```kotlin
class SqlInterpolator(val connection: Connection): Interpolator<Any, PreparedStatement> {
  // Parts is ["SELECT * FROM users WHERE id = ", " AND name = ", ""]
  // Params is [`id`, `name`] i.e. [1234, "Joe"]
  override fun interpolate(parts: () -> List<String>, params: () -> List<Any>): PreparedStatement {
    val stringParts = parts()
    val stmt = connection.prepareStatement(stringParts.joinToString("?"))
    for ((arg, i) in params().withIndex()) {
      stmt.setObject(i + 1, arg) // remember with JDBC it's an index starting from 1
    }
    return stmt
  }
}

val sql = SqlInterpolator<Any, PreparedStatement>(connection)
val (id, name) = 1234 to "Joe"
val stmt = sql("SELECT * FROM users WHERE id = $id AND name = $name")
// I.e the `sql.invoke(...)` function forwards the parts/params to sql.interpolate
```

## Usage

In order to use Terpal in your projects, you need to add the following to your `build.gradle.kts`:

```kotlin
plugins {
  kotlin("jvm") version "1.9.0"
  id("io.exoquery.terpal-plugin") version "1.9.0-0.1.0"
}

dependencies {
  api("io.exoquery:terpal-runtime:1.0.6")
}
```

## Other Features

Terpal interpolators can be instantiated classes as well as static ones. For example, the above
interpolator could have been written as:
```kotlin
object SqlInterpolator: Interpolator<Any, PreparedStatement> { 
  fun  interpolate(parts: () -> List<String>, params: () -> List<Any>): PreparedStatement {
    ...
    return stmt
  }
}
```

## Interpolator Functions
In addition to classes/objects, you can use an annotation to assign a function to act as an interpolator:
for example:

```kotlin
@InterpolatorFunction<SqlInterpolator>(SqlInterpolator::class)
fun sql2(sqlString: String): PreparedStatement = interpolatorBody()

// usage
sql2("SELECT * FROM users WHERE id = $id AND name = $name")
```

By using this combined with an string extension function, you can create a very compact DSL for interpolation:
```kotlin
@InterpolatorFunction<SqlInterpolator>(SqlInterpolator::class)
operator fun String.unaryPlus() = interpolatorBody()

// usage
+"SELECT * FROM users WHERE id = $id AND name = $name"
```
