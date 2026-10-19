# Tessera

**The same questions, year after year.** Ask your child, yourself or your grandmother the same questions every day, week, month or birthday. Every answer is laid as a stone of coloured glass, and the years become a mosaic.

[Русская версия](README.ru.md)

## How it works

- **Each question has its own rhythm.**
  - *Every day*: small and quick, "What was the best part of today?"
  - *Every week* and *every month*: a minute by voice or on camera.
  - *Every year*: the big interview on the birthday ("What do you want to be when you grow up?").
  - Daily questions take turns, a few a day, so a long list never becomes a chore.
- **Answer on film, by voice or in words.** The camera is built in, so the question stays on screen while you speak. Last time's frame lies faintly over the picture so you stand the same way and the years line up. It counts down 3-2-1, and a gold ring shows the time left.
- **A year ago…** Right after you answer, the answer to the same question from a year ago comes back.
- **The panel.** Each person's year is a mosaic of two-week rows, one stone per day. A filmed answer shows its frame through the question's glass. Gilded stones are the ones to keep above all.
- **Through the years.**
  - The *chorus of years* plays one answer from each year back to back: a face and a voice growing up.
  - *Unchanged since…* marks the answers that held for years.
  - *Words of the year* shows what a year of daily answers kept coming back to.
  - *This day* shows what was said on today's date in earlier years.
- **A library of questions** for little ones, children, teenagers, grown-ups and elders, in English and Russian. You can also write your own.

## Privacy

There is no account, no cloud and no analytics. Films, voices and words stay in the app's own folder on the phone. *Settings → Backup* packs everything, media included, into one ZIP you keep wherever you like.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug        # app.tessera.dev
./gradlew assembleDebug -Pdemo                   # app.tessera.demo with a family and years of answers
```

Android 9+. Kotlin, Jetpack Compose, Room, CameraX.

## License

MIT
