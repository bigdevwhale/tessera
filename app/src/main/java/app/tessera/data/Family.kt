package app.tessera.data

import app.tessera.domain.Band
import app.tessera.domain.Cadence
import app.tessera.domain.Format
import app.tessera.domain.Library
import app.tessera.domain.LibraryQuestion

/** Setting people up and handing them questions. */
object Family {
    /** Library questions keep their colour wherever they are asked, so a family's panels rhyme. */
    fun glassOf(key: String, fallback: Int): Int = if (key.isEmpty()) fallback else Math.floorMod(key.hashCode(), 12)

    fun question(personId: Long, lib: LibraryQuestion, band: Band, order: Int) = Question(
        personId = personId,
        key = lib.key,
        cadence = lib.cadence.name,
        format = band.format(lib.cadence).name,
        glass = glassOf(lib.key, 0),
        order = order,
    )

    /** Adds someone with the library's starting questions for their band. Returns their id. */
    suspend fun add(dao: TesseraDao, person: Person): Long {
        val order = dao.people().size
        val id = dao.insert(person.copy(order = order))
        val band = person.bandOf
        dao.insertQuestions(Library.starter(band).sortedBy { it.cadence.ordinal }.mapIndexed { i, q -> question(id, q, band, i) })
        return id
    }

    suspend fun addLibrary(dao: TesseraDao, person: Person, lib: LibraryQuestion) {
        dao.insert(question(person.id, lib, person.bandOf, dao.lastOrder(person.id) + 1))
    }

    suspend fun addOwn(dao: TesseraDao, person: Person, text: String, cadence: Cadence, format: Format, glass: Int) {
        dao.insert(Question(personId = person.id, text = text.trim(), cadence = cadence.name, format = format.name, glass = glass, order = dao.lastOrder(person.id) + 1))
    }
}
