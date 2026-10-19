package app.tessera.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import app.tessera.domain.Band
import app.tessera.domain.Cadence
import app.tessera.domain.Dates
import app.tessera.domain.Format
import app.tessera.domain.Library
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Someone whose answers are being laid: a child, a grown-up, a grandmother. [birth] is "yyyy-MM-dd" or "". */
@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birth: String = "",
    val band: String = Band.ADULT.name,
    /** Their own glass on the home screen, an index into the smalt colours. */
    val glass: Int = 0,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val birthday: LocalDate? get() = Dates.parse(birth)
    val bandOf: Band get() = Band.of(band)
}

/**
 * One question someone is asked. A library question keeps its [key] and is shown in the app's language;
 * one written by the family keeps its [text]. [glass] is the smalt colour its answers are laid in.
 */
@Entity(tableName = "questions", indices = [Index("personId")])
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val key: String = "",
    val text: String = "",
    val cadence: String = Cadence.DAILY.name,
    val format: String = Format.TEXT.name,
    val glass: Int = 0,
    val order: Int = 0,
    /** Put away: no longer asked, but its answers stay in the panel. */
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val cadenceOf: Cadence get() = Cadence.of(cadence)
    val formatOf: Format get() = Format.of(format)
    fun text(ru: Boolean): String = Library.of(key)?.text(ru) ?: text
}

/**
 * One smalt: an answer on a day. [file] is a video or voice file in the app's media folder, [thumb] the
 * frame the tile shows. [asked] is the question as it was worded that day, so an edited question
 * doesn't rewrite the past. [front]: filmed with the selfie camera.
 */
@Entity(tableName = "answers", indices = [Index("personId", "day"), Index("questionId", "day")])
data class Answer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val questionId: Long,
    /** LocalDate.toEpochDay of the day it belongs to. */
    val day: Long,
    val at: Long = System.currentTimeMillis(),
    val kind: String = Format.TEXT.name,
    val text: String = "",
    val file: String = "",
    val thumb: String = "",
    val durationMs: Long = 0,
    val asked: String = "",
    val starred: Boolean = false,
    val front: Boolean = true,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
    val kindOf: Format get() = Format.of(kind)
    val files: List<String> get() = listOf(file, thumb).filter { it.isNotEmpty() }
}

@Dao
abstract class TesseraDao {
    @Query("SELECT * FROM people ORDER BY `order`, id") abstract fun observePeople(): Flow<List<Person>>
    @Query("SELECT * FROM questions ORDER BY `order`, id") abstract fun observeQuestions(): Flow<List<Question>>
    @Query("SELECT * FROM answers ORDER BY day, at") abstract fun observeAnswers(): Flow<List<Answer>>

    @Query("SELECT * FROM people ORDER BY `order`, id") abstract suspend fun people(): List<Person>
    @Query("SELECT * FROM questions ORDER BY `order`, id") abstract suspend fun questions(): List<Question>
    @Query("SELECT * FROM answers ORDER BY day, at") abstract suspend fun answers(): List<Answer>
    @Query("SELECT * FROM answers WHERE id = :id") abstract suspend fun answer(id: Long): Answer?
    @Query("SELECT * FROM answers WHERE personId = :personId") abstract suspend fun answersOf(personId: Long): List<Answer>
    @Query("SELECT * FROM answers WHERE questionId = :questionId") abstract suspend fun answersTo(questionId: Long): List<Answer>
    @Query("SELECT COALESCE(MAX(`order`), -1) FROM questions WHERE personId = :personId") abstract suspend fun lastOrder(personId: Long): Int

    @Insert abstract suspend fun insert(p: Person): Long
    @Update abstract suspend fun update(p: Person)
    @Delete protected abstract suspend fun deletePersonRow(p: Person)

    @Insert abstract suspend fun insert(q: Question): Long
    @Insert abstract suspend fun insertQuestions(l: List<Question>)
    @Update abstract suspend fun update(q: Question)
    @Delete protected abstract suspend fun deleteQuestionRow(q: Question)

    @Insert abstract suspend fun insert(a: Answer): Long
    @Insert abstract suspend fun insertAnswers(l: List<Answer>)
    @Update abstract suspend fun update(a: Answer)
    @Delete abstract suspend fun delete(a: Answer)

    @Query("DELETE FROM questions WHERE personId = :personId") protected abstract suspend fun clearQuestionsOf(personId: Long)
    @Query("DELETE FROM answers WHERE personId = :personId") protected abstract suspend fun clearAnswersOf(personId: Long)
    @Query("DELETE FROM answers WHERE questionId = :questionId") protected abstract suspend fun clearAnswersTo(questionId: Long)

    /** Removes someone with every question and answer. Returns the media files to delete. */
    @Transaction
    open suspend fun deletePerson(p: Person): List<String> {
        val files = answersOf(p.id).flatMap { it.files }
        clearAnswersOf(p.id); clearQuestionsOf(p.id); deletePersonRow(p)
        return files
    }

    /** Removes a question with its answers. Returns the media files to delete. */
    @Transaction
    open suspend fun deleteQuestion(q: Question): List<String> {
        val files = answersTo(q.id).flatMap { it.files }
        clearAnswersTo(q.id); deleteQuestionRow(q)
        return files
    }

    @Query("DELETE FROM people") protected abstract suspend fun clearPeople()
    @Query("DELETE FROM questions") protected abstract suspend fun clearQuestions()
    @Query("DELETE FROM answers") protected abstract suspend fun clearAnswers()
    @Insert protected abstract suspend fun insertPeople(l: List<Person>)

    @Transaction
    open suspend fun replaceAll(all: Everything) {
        clearAnswers(); clearQuestions(); clearPeople()
        insertPeople(all.people); insertQuestions(all.questions); insertAnswers(all.answers)
    }
}

/** The whole wall, for backups. */
class Everything(val people: List<Person>, val questions: List<Question>, val answers: List<Answer>)

@Database(entities = [Person::class, Question::class, Answer::class], version = 1, exportSchema = true)
abstract class TesseraDatabase : RoomDatabase() {
    abstract fun dao(): TesseraDao

    companion object {
        fun create(context: Context): TesseraDatabase =
            Room.databaseBuilder(context.applicationContext, TesseraDatabase::class.java, "tessera.db").build()
    }
}
