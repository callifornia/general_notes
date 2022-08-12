package IMG

import org.mindrot.jbcrypt.BCrypt

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

object Main {
  def main(args: Array[String]): Unit = {
    println(UserCreateSqlGenerator.generateSql(
      partOfEmailBeforeCounter = "bet365user+",
      partOfEmailAfterCounter = "@imggaming.com", // result is going to be: "bet365user+301@imggaming.com" and so on ...
      userName = "Bet365 User",                   // result is going to be: "Bet365 User 301", "Bet365 User 302" ...
      accountNumberStart = 301,                   // include 301
      accountNumberEnd = 400,                     // include 400
      operatorId = 19,
      pswd = "LondonImg365",
      state = "ACTIVE",                           // can be ACTIVE or INCOMPLETE
      userType = "OPERATOR"                       // can be OPERATOR or ADMIN
    ))
  }
}


object UserCreateSqlGenerator {
  private case class User(id: UUID, name: String, operatorId: Int, email: String, hashedPswd: String, state: String, userType: String) {
    val valueStandardUserTable: String = {
      val str = (id :: email :: operatorId :: name :: hashedPswd :: state :: userType :: Nil)
        .map {
          case e: UUID => s"'$e'"
          case s: String => s"'$s'"
          case o: Any => o
        }.mkString(", ")
      s"($str)"
    }

    val valueEventUserTable = (requestType: String) => {
      val time = {
        val t = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
        if (requestType == "USER_CREATION_COMPLETED") t.plusMinutes(2) else t
      }

      val str = (id :: userType :: requestType :: time :: Nil)
        .map {
          case id: UUID => s"'$id'"
          case s: String => s"'$s'"
          case s: ZonedDateTime => s"'$s'"
          case e: Any => e
        }.mkString(", ")
      s"($str)"
    }
  }

  private val buildInsertIntoMainTable =
    (values: String) =>
      s"insert into standard_user (user_id, email_address, operator_id, full_name, password_hash, state, user_type) values $values"


  private val buildInsertIntoEventTable =
    (values: String) =>
      s"insert into standard_user_event (user_id, user_type, event_type, created) values $values"



  def generateSql(partOfEmailBeforeCounter: String,
                  partOfEmailAfterCounter: String,
                  userName: String,
                  accountNumberStart: Int,
                  accountNumberEnd: Int,
                  operatorId: Int,
                  pswd: String,
                  state: String,
                  userType: String): String = {

    val users = (accountNumberStart to accountNumberEnd)
      .map(index =>
        User(
          id = UUID.randomUUID(),
          name = userName + s" $index",
          operatorId = operatorId,
          email = partOfEmailBeforeCounter + index + partOfEmailAfterCounter,
          hashedPswd = BCrypt.hashpw(pswd, BCrypt.gensalt()),
          state = state,
          userType = userType))

    val insertMainTableQuery = buildInsertIntoMainTable(users.map(_.valueStandardUserTable).mkString("\n", ",\n", ";\n"))
    val insertEventTableQuery = buildInsertIntoEventTable(users.flatMap(u => u.valueEventUserTable("USER_CREATION_COMPLETED") :: u.valueEventUserTable("USER_CREATION_REQUESTED") :: Nil).mkString("\n", ",\n", ";\n"))

    (insertMainTableQuery :: insertEventTableQuery :: Nil).mkString("\n")
  }
}
