package app.tessera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.tessera.AppContainer
import app.tessera.domain.Band
import app.tessera.domain.Cadence
import app.tessera.domain.Dates
import app.tessera.domain.Format
import app.tessera.domain.Library
import app.tessera.ru
import app.tessera.ui.theme.Smalt
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import kotlin.random.Random

/**
 * The demo build's family (`-Pdemo`): a girl growing up since 2023, her little brother, and a grandmother,
 * with years of answers. There are no real videos; filmed answers get painted stand-in frames.
 */
object DemoSeed {
    private class Line(val en: String, val ru: String)

    private fun l(en: String, ru: String) = Line(en, ru)

    /** What Mila said to the birthday questions, one line per year from 2023. */
    private val milaYears = mapOf(
        "y_be" to listOf(l("A dinosaur!", "Динозавром!"), l("A police officer", "Полицейской"), l("A vet, for cats", "Ветеринаром, для котиков"), l("An artist and a cosmonaut", "Художницей и космонавтом")),
        "y_colour" to listOf(l("Pink", "Розовый"), l("Pink and gold", "Розовый и золотой"), l("Purple", "Фиолетовый"), l("Turquoise. Pink is for babies", "Бирюзовый. Розовый — для малышей")),
        "y_food" to listOf(l("Pasta", "Макароны"), l("Pasta with cheese", "Макароны с сыром"), l("Grandma's pancakes", "Бабушкины блины"), l("Grandma's pancakes, still", "Всё ещё бабушкины блины")),
        "y_friend" to listOf(l("Bunny (the toy)", "Зайка (игрушка)"), l("Sonya from nursery", "Соня из садика"), l("Sonya and Masha", "Соня и Маша"), l("Masha. Sonya moved away", "Маша. Соня переехала")),
        "y_happy" to listOf(l("Swings", "Качели"), l("When dad throws me up high", "Когда папа подкидывает"), l("Drawing at the big table", "Рисовать за большим столом"), l("When everyone is home", "Когда все дома")),
        "y_scared" to listOf(l("The dark", "Темноты"), l("The dark and wolves", "Темноты и волков"), l("Wasps", "Ос"), l("Being late for school", "Опоздать в школу")),
        "y_power" to listOf(l("Flying", "Летать"), l("Turning into a cat", "Превращаться в кошку"), l("Being invisible", "Невидимость"), l("Stopping time", "Останавливать время")),
        "y_family" to listOf(l("Mum, dad, me and Bunny", "Мама, папа, я и Зайка"), l("Mum, dad, me and baby Tyoma", "Мама, папа, я и маленький Тёма"), l("Lots of us, and grandma", "Нас много, и бабушка"), l("Five of us if you count grandma", "Пятеро, если считать бабушку")),
        "y_grown" to listOf(l("You can eat sweets", "Можно есть конфеты"), l("You drive a car", "Водишь машину"), l("You get tired", "Устаёшь"), l("You decide things", "Сам решаешь")),
        "y_love" to listOf(l("Mum", "Маму"), l("Mum and the sea", "Маму и море"), l("Everyone and drawing", "Всех и рисовать"), l("My family and the sea", "Свою семью и море")),
        "y_animal" to listOf(l("Bunny", "Зайчик"), l("A cat", "Кошка"), l("A cat called Pirozhok", "Кот по имени Пирожок"), l("Snow leopard", "Снежный барс")),
    )

    private val milaDaily = listOf(
        l("We fed the ducks", "Кормили уточек"), l("Pancakes!", "Блины!"), l("Masha came over", "Пришла Маша"), l("I drew a horse", "Нарисовала лошадь"),
        l("The puddle by the school", "Лужа у школы"), l("Tyoma said my name", "Тёма сказал моё имя"), l("Snow!", "Снег!"), l("A five in maths", "Пятёрка по математике"),
        l("Grandma's stories", "Бабушкины истории"), l("The cartoon about the fox", "Мультик про лису"), l("Bike without the little wheels", "Велосипед без колёсиков"),
        l("Ice cream in the park", "Мороженое в парке"), l("Dad read three chapters", "Папа читал три главы"), l("The cat slept on me", "Кот спал на мне"),
    )
    private val tyomaDaily = listOf(
        l("Car!", "Машинка!"), l("Mila", "Мила"), l("Bath with boats", "Купаться с корабликами"), l("Big digger", "Экскаватор"), l("Dog!", "Собачка!"),
        l("Biscuit", "Печенька"), l("Swing high", "Качели высоко"), l("Sand", "Песок"),
    )
    private val veraYears = mapOf(
        "e_home" to listOf(l("A wooden house by the river, a stove in the middle", "Деревянный дом у реки, печка посередине")),
        "e_love" to listOf(l("A dance in 1971. He stepped on my foot twice", "Танцы в семьдесят первом. Он дважды наступил мне на ногу")),
        "e_song" to listOf(l("'Katyusha', all the verses", "«Катюша», все куплеты")),
        "e_advice" to listOf(l("Don't save the good dishes for guests", "Не берегите хороший сервиз для гостей")),
    )

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.people().isNotEmpty()) return
        val ru = context.ru
        val today = LocalDate.now()
        val rnd = Random(7)

        val mila = Person(name = if (ru) "Мила" else "Mila", birth = "2019-03-14", band = Band.KID.name, glass = 6)
        val tyoma = Person(name = if (ru) "Тёма" else "Tyoma", birth = "2023-08-02", band = Band.LITTLE.name, glass = 1)
        val vera = Person(name = if (ru) "Бабушка Вера" else "Grandma Vera", birth = "1952-11-20", band = Band.ELDER.name, glass = 3)
        val milaId = Family.add(dao, mila)
        val tyomaId = Family.add(dao, tyoma)
        val veraId = Family.add(dao, vera)
        // Mila also has the library's little-ones questions she was asked before she grew into a "kid".
        Library.of("y_animal")?.let { Family.addLibrary(dao, mila.copy(id = milaId), it) }
        Library.of("d_colour")?.let { Family.addLibrary(dao, mila.copy(id = milaId), it) }

        val qs = dao.questions()
        val answers = mutableListOf<Answer>()
        val frames = mutableMapOf<String, String>()
        fun frame(personId: Long, year: Int, hue: Int, age: Int): String = frames.getOrPut("$personId-$year-$hue") { portrait(context, hue, age, rnd) }

        fun lay(personId: Long, birth: LocalDate, from: LocalDate, daily: List<Line>, years: Map<String, List<Line>>, rate: Float) {
            val mine = qs.filter { it.personId == personId }
            val start = from.toEpochDay()
            for (dayN in start..today.toEpochDay()) {
                val d = LocalDate.ofEpochDay(dayN)
                val age = Dates.age(birth, d) ?: 0
                mine.forEach { q ->
                    val c = q.cadenceOf
                    val due = when (c) {
                        Cadence.DAILY -> rnd.nextFloat() < rate / 2f
                        Cadence.WEEKLY -> d.dayOfWeek.value == 7 && rnd.nextFloat() < 0.8f
                        Cadence.MONTHLY -> d.dayOfMonth == 1 + (q.id % 5).toInt() && rnd.nextFloat() < 0.85f
                        Cadence.YEARLY -> Dates.lastBirthday(d, birth) == d && d != birth
                    }
                    if (!due || (dayN == today.toEpochDay() && c != Cadence.YEARLY)) return@forEach
                    val yearLines = years[q.key]
                    val line = when {
                        yearLines != null -> yearLines.getOrNull(d.year - from.year)?.let { if (ru) it.ru else it.en }
                        c == Cadence.DAILY -> daily[rnd.nextInt(daily.size)].let { if (ru) it.ru else it.en }
                        else -> daily[rnd.nextInt(daily.size)].let { if (ru) it.ru else it.en }
                    } ?: return@forEach
                    val kind = if (q.formatOf == Format.VIDEO || c == Cadence.YEARLY) Format.VIDEO else q.formatOf
                    answers += Answer(
                        personId = personId, questionId = q.id, day = dayN, at = d.atTime(19, 30).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        kind = kind.name, text = line,
                        thumb = if (kind == Format.VIDEO) frame(personId, d.year, q.glass, age) else "",
                        durationMs = when (kind) {
                            Format.VIDEO -> if (c == Cadence.YEARLY) 20_000L + rnd.nextLong(40_000) else 4_000L + rnd.nextLong(6_000)
                            Format.VOICE -> 8_000L + rnd.nextLong(30_000)
                            Format.TEXT -> 0
                        },
                        asked = q.text(ru), starred = rnd.nextFloat() < 0.04f || (c == Cadence.YEARLY && q.key == "y_be"),
                    )
                }
            }
        }
        lay(milaId, LocalDate.of(2019, 3, 14), LocalDate.of(2023, 3, 1), milaDaily, milaYears, 0.85f)
        lay(tyomaId, LocalDate.of(2023, 8, 2), LocalDate.of(2025, 9, 1), tyomaDaily, emptyMap(), 0.7f)
        lay(veraId, LocalDate.of(1952, 11, 20), LocalDate.of(2025, 11, 1), milaDaily, veraYears, 0.4f)
        dao.insertAnswers(answers)
        c.settings.update { it.copy(onboarded = true) }
    }

    /**
     * A painted stand-in for a video frame: a head and shoulders against a wash of the question's glass,
     * smaller for a smaller child. Only the demo uses these.
     */
    private fun portrait(context: Context, hue: Int, age: Int, rnd: Random): String {
        val w = 360
        val h = 480
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val glass = Smalt.of(hue).toArgb()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), glass, android.graphics.Color.rgb(30, 28, 40), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        val grow = (0.72f + age.coerceAtMost(14) * 0.02f)
        val cx = w / 2f + rnd.nextInt(-14, 14)
        val headR = 78f * (1.25f - grow * 0.35f)
        val top = h * (0.62f - grow * 0.22f)
        val shirt = Smalt.of(hue + 5 + rnd.nextInt(3)).toArgb()
        p.color = shirt
        cv.drawOval(RectF(cx - 150f, top + headR * 1.6f, cx + 150f, h + 140f), p)
        p.color = android.graphics.Color.rgb(226, 182, 150)
        cv.drawOval(RectF(cx - headR * 0.85f, top - headR, cx + headR * 0.85f, top + headR), p)
        p.color = android.graphics.Color.rgb(70 + rnd.nextInt(40), 44, 28)
        cv.drawArc(RectF(cx - headR * 0.95f, top - headR * 1.1f, cx + headR * 0.95f, top + headR * 0.4f), 180f, 180f, true, p)
        val name = Media.newName("jpg")
        Media.file(context, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        return name
    }
}
