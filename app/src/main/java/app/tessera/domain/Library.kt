package app.tessera.domain

/**
 * A question from the library. [key] is stored with the question and must never change. [bands] are who it
 * suits; [starter] are the bands that get it when a person is first set up. Russian wording avoids
 * gendered past tense so it reads right to anyone.
 */
class LibraryQuestion(
    val key: String,
    val cadence: Cadence,
    val bands: Set<Band>,
    val starter: Set<Band>,
    val en: String,
    val ru: String,
) {
    fun text(ru: Boolean) = if (ru) this.ru else en
}

object Library {
    /** Letters for bands: L little, K kid, T teen, A adult, E elder. */
    private fun bands(s: String) = s.mapNotNull {
        when (it) {
            'L' -> Band.LITTLE
            'K' -> Band.KID
            'T' -> Band.TEEN
            'A' -> Band.ADULT
            'E' -> Band.ELDER
            else -> null
        }
    }.toSet()

    private fun q(key: String, c: Cadence, who: String, starter: String, en: String, ru: String) =
        LibraryQuestion(key, c, bands(who), bands(starter), en, ru)

    val all: List<LibraryQuestion> = listOf(
        // A day: small, quick, a glance back before sleep.
        q("d_best", Cadence.DAILY, "LKTAE", "LKTAE", "What was the best part of today?", "Что сегодня было самым лучшим?"),
        q("d_word", Cadence.DAILY, "KTAE", "TA", "One word for today?", "Одно слово про сегодня?"),
        q("d_laugh", Cadence.DAILY, "LKTAE", "LK", "What made you laugh today?", "Что тебя сегодня рассмешило?"),
        q("d_colour", Cadence.DAILY, "LK", "", "What colour was today?", "Какого цвета был сегодняшний день?"),
        q("d_hard", Cadence.DAILY, "KTAE", "", "What was hard today?", "Что сегодня было трудным?"),
        q("d_thanks", Cadence.DAILY, "LKTAE", "E", "Who would you thank for today?", "Кому хочется сказать спасибо за сегодня?"),
        q("d_new", Cadence.DAILY, "LKTAE", "", "What was new today?", "Что сегодня было новым?"),
        q("d_game", Cadence.DAILY, "LK", "", "Which game was the most fun today?", "Какая игра сегодня была самой интересной?"),
        q("d_tomorrow", Cadence.DAILY, "LKTAE", "", "What are you looking forward to tomorrow?", "Чего ты ждёшь от завтра?"),
        q("d_feel", Cadence.DAILY, "TA", "", "How do you feel right now, honestly?", "Как ты себя сейчас чувствуешь, честно?"),

        // A week: a few sentences, often by voice.
        q("w_near", Cadence.WEEKLY, "LKTAE", "LKTAE", "Who was around the most this week?", "Кто был рядом чаще всех на этой неделе?"),
        q("w_learn", Cadence.WEEKLY, "KTAE", "K", "What did you find out this week?", "Что интересного удалось узнать на этой неделе?"),
        q("w_proud", Cadence.WEEKLY, "LKTA", "T", "What are you proud of this week?", "Чем ты гордишься на этой неделе?"),
        q("w_song", Cadence.WEEKLY, "KTA", "", "Which song is stuck in your head this week?", "Какая песня крутится в голове на этой неделе?"),
        q("w_redo", Cadence.WEEKLY, "TA", "A", "What would you do differently this week?", "Что в этой неделе хочется переделать?"),
        q("w_story", Cadence.WEEKLY, "E", "E", "Tell a story this week reminded you of.", "Расскажи историю, которую напомнила эта неделя."),

        // A month: a minute or two on camera.
        q("m_want", Cadence.MONTHLY, "LKTAE", "LKTA", "What do you want most right now?", "Чего тебе сейчас хочется больше всего?"),
        q("m_angry", Cadence.MONTHLY, "KTA", "", "What makes you angry lately?", "Что тебя сейчас злит?"),
        q("m_afraid", Cadence.MONTHLY, "LKT", "L", "What are you afraid of?", "Чего ты боишься?"),
        q("m_watch", Cadence.MONTHLY, "LKTA", "K", "Your favourite book, film or cartoon right now?", "Какая книга, фильм или мультик сейчас самые любимые?"),
        q("m_dream", Cadence.MONTHLY, "LKTAE", "", "What did you dream about lately?", "Что тебе недавно снилось?"),
        q("m_change", Cadence.MONTHLY, "TA", "T", "What has changed in you this month?", "Что в тебе изменилось за этот месяц?"),
        q("m_mind", Cadence.MONTHLY, "AE", "AE", "What is on your mind most these days?", "О чём ты больше всего думаешь в эти дни?"),

        // A year: the big interview on the birthday. These make the chorus of years.
        q("y_colour", Cadence.YEARLY, "LKTAE", "LKT", "What is your favourite colour?", "Какой твой любимый цвет?"),
        q("y_food", Cadence.YEARLY, "LKTAE", "LKT", "What is your favourite food?", "Какая твоя любимая еда?"),
        q("y_friend", Cadence.YEARLY, "LKTAE", "LKTA", "Who is your best friend?", "Кто твой лучший друг?"),
        q("y_be", Cadence.YEARLY, "LKT", "LKT", "What do you want to be when you grow up?", "Кем ты хочешь стать, когда вырастешь?"),
        q("y_happy", Cadence.YEARLY, "LKTAE", "LKTAE", "What makes you truly happy?", "Отчего тебе бывает по-настоящему хорошо?"),
        q("y_scared", Cadence.YEARLY, "LKTA", "LK", "What scares you most?", "Что пугает тебя больше всего?"),
        q("y_power", Cadence.YEARLY, "LKT", "LK", "If you could have one superpower, which?", "Какая суперсила тебе нужна?"),
        q("y_animal", Cadence.YEARLY, "LK", "L", "What is your favourite animal?", "Какое твоё любимое животное?"),
        q("y_good", Cadence.YEARLY, "LKTAE", "KTA", "What are you really good at?", "Что у тебя по-настоящему хорошо получается?"),
        q("y_family", Cadence.YEARLY, "LK", "LK", "Tell me about your family.", "Расскажи про свою семью."),
        q("y_grown", Cadence.YEARLY, "LK", "LK", "What is it like to be a grown-up?", "Каково это — быть взрослым?"),
        q("y_love", Cadence.YEARLY, "LKTAE", "LKTAE", "What do you love most in the world?", "Что ты любишь больше всего на свете?"),
        q("y_place", Cadence.YEARLY, "LKTAE", "KT", "Where is your favourite place?", "Где твоё любимое место?"),
        q("y_best", Cadence.YEARLY, "KTAE", "KTAE", "What was the best thing this year?", "Что было лучшим за этот год?"),
        q("y_next", Cadence.YEARLY, "KTAE", "KTAE", "What do you want from the year ahead?", "Чего ты ждёшь от следующего года?"),
        q("y_guess", Cadence.YEARLY, "KTA", "KTA", "What will be different about you in a year?", "Как думаешь, что в тебе изменится через год?"),
        q("y_believe", Cadence.YEARLY, "TAE", "TA", "What do you believe in firmly?", "Во что ты твёрдо веришь?"),
        q("y_feel_age", Cadence.YEARLY, "AE", "A", "How old do you feel?", "На сколько лет ты себя чувствуешь?"),
        q("y_matters", Cadence.YEARLY, "TAE", "A", "Who matters most to you right now?", "Кто сейчас для тебя важнее всех?"),
        q("y_taught", Cadence.YEARLY, "TAE", "A", "What has this year taught you?", "Чему тебя научил этот год?"),
        q("y_five", Cadence.YEARLY, "TA", "", "Where do you want to be in five years?", "Где ты хочешь быть через пять лет?"),
        q("y_letter", Cadence.YEARLY, "KTAE", "TA", "Say something to yourself in ten years.", "Скажи что-нибудь себе через десять лет."),

        // The older ones: their voices, their stories, before it is too late to ask.
        q("e_home", Cadence.YEARLY, "E", "E", "What do you remember of the house you grew up in?", "Что ты помнишь о доме, где прошло детство?"),
        q("e_love", Cadence.YEARLY, "AE", "E", "Tell the story of how you fell in love.", "Расскажи историю вашей любви."),
        q("e_proud", Cadence.YEARLY, "E", "E", "What in your life are you proudest of?", "Чем в жизни ты гордишься больше всего?"),
        q("e_advice", Cadence.YEARLY, "E", "E", "What advice would you give your grandchildren?", "Какой совет ты дашь внукам?"),
        q("e_song", Cadence.YEARLY, "E", "E", "Sing a song from your youth.", "Спой песню из своей молодости."),
        q("e_work", Cadence.YEARLY, "AE", "E", "What was your first job like?", "Какой была твоя первая работа?"),
        q("e_hard", Cadence.YEARLY, "AE", "E", "What was the hardest time, and what helped?", "Какое время было самым трудным и что тогда помогло?"),
        q("e_recipe", Cadence.MONTHLY, "AE", "E", "Tell a family recipe.", "Расскажи семейный рецепт."),
        q("e_wish", Cadence.YEARLY, "AE", "E", "What do you wish for the family?", "Чего ты желаешь нашей семье?"),
    )

    private val byKey = all.associateBy { it.key }

    fun of(key: String): LibraryQuestion? = byKey[key]

    fun starter(band: Band): List<LibraryQuestion> = all.filter { band in it.starter }

    /** What could still be added for [band], best fits first. */
    fun suggestions(band: Band, taken: Set<String>): List<LibraryQuestion> =
        all.filter { it.key !in taken }.sortedBy { if (band in it.bands) 0 else 1 }
}
