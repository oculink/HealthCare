package com.fyp.healthcare

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * The picture library behind Emotional Support. Every photo is bundled in the APK rather than
 * fetched: the page has to work on a phone with no data connection, and a live source would put
 * a stranger's uptime between an isolated person and the one cheerful thing they clicked for.
 *
 * All images are Wikimedia Commons public-domain or free-licence files. [SupportPhoto.credit]
 * is surfaced on-screen because several are CC BY / CC BY-SA and require attribution.
 */
data class SupportPhoto(
    @param:DrawableRes val resId: Int,
    val group: String,
    val title: String,
    val artist: String,
    val licence: String,
    val alt: String,
) {
    /** What the licence asks us to show. CC0 and public domain still list an author, politely. */
    val credit: String get() = "$artist · $licence"
}

val SupportPhotos = listOf(
    SupportPhoto(R.drawable.es_cat_1, "animals", "Domestic shorthair cat portrait in grass", "Pixnio", "CC0",
        "A grey cat sitting in green grass"),
    SupportPhoto(R.drawable.es_cat_2, "animals", "Calico cat, - Assisi, Italy", "Terragio67", "CC BY-SA 4.0",
        "A calico cat resting on a stone step"),
    SupportPhoto(R.drawable.es_cat_3, "animals", "Chiang Mai kitties - 2017-07-09 (003)", "Iudexvivorum", "CC0",
        "Two ginger kittens playing in the grass"),
    SupportPhoto(R.drawable.es_dog_3, "animals", "Wounded Warriors enjoy friendship, the outdoors at QIMSA hunt ", "Adele Uphaus-Conner", "Public domain",
        "A golden dog resting beside a garden cart"),
    SupportPhoto(R.drawable.es_dog_4, "animals", "Elderly woman with Golden Retriever", "PatInver", "CC BY-SA 4.0",
        "An older woman smiling with her golden retriever"),
    SupportPhoto(R.drawable.es_dog_5, "animals", "20140531 German Shepard dog puppy 3151", "Jakub Hałun", "CC BY-SA 4.0",
        "A german shepherd puppy carrying a stick"),
    SupportPhoto(R.drawable.es_dog_6, "animals", "Beagle puppy sitting on grass", "Wikimedia contributor", "CC BY-SA 3.0",
        "A beagle puppy sitting on the lawn"),
    SupportPhoto(R.drawable.es_bunny_1, "animals", "2018-06-22 AT Wien 13 Hietzing, Tiergarten Schönbrunn, Oryctol", "Paul Korecky", "CC BY-SA 2.0",
        "A grey rabbit grazing in the grass"),
    SupportPhoto(R.drawable.es_bunny_2, "animals", "Conejo común (Oryctolagus cuniculus), Tierpark Hellabrunn, Mún", "Diego Delso", "CC BY-SA 3.0",
        "A white and black rabbit beside a wooden bowl"),
    SupportPhoto(R.drawable.es_flower_1, "flowers", "Sunflower field at sunset", "Summer Stock", "CC0",
        "A sunflower field at sunset"),
    SupportPhoto(R.drawable.es_flower_2, "flowers", "Sunflower Field (7848091146)", "Audrey", "CC BY 2.0",
        "A field of sunflowers under a pale sky"),
    SupportPhoto(R.drawable.es_flower_3, "flowers", "A tulip in the Tashkent Botanical Garden", "26D", "CC BY-SA 4.0",
        "A single red lily among green leaves"),
    SupportPhoto(R.drawable.es_flower_6, "flowers", "Rose Garden Flowers in Bloom (49854267063)", "The White House", "Public domain",
        "Pink roses in bloom in a garden"),
    SupportPhoto(R.drawable.es_flower_8, "flowers", "Daisy Meadow - geograph.org.uk - 8340010", "Anne Burgess", "CC BY-SA 2.0",
        "A meadow covered in white daisies"),
    SupportPhoto(R.drawable.es_green_1, "greenery", "Beech and ferns in Gullmarsskogen", "W.carter", "CC0",
        "Green ferns growing in a shaded forest"),
    SupportPhoto(R.drawable.es_green_2, "greenery", "Green forest carpet (Unsplash)", "ydmytro", "CC0",
        "A carpet of soft green foliage"),
    SupportPhoto(R.drawable.es_green_4, "greenery", "Arashiyama-Bamboo-Grove-Sunset", "Bjørn Christian Tørrissen", "CC BY-SA 4.0",
        "A path leading through a tall bamboo grove"),
    SupportPhoto(R.drawable.es_green_5, "greenery", "Mosses, Lichens, and Grasses sparkle on the forest floor in La", "Extemporalist", "CC0",
        "Sunlight falling on moss covering the forest floor"),
    SupportPhoto(R.drawable.es_green_7, "greenery", "Autumn walk in the meadow. - Flickr - enneafive", "Johan Neven", "CC BY 2.0",
        "A misty green meadow lined with trees"),
)

/**
 * The kinds of picture the person can choose to see, each with the accent its tickbox uses in
 * the picker. Untick everything and the page shows all of them again rather than nothing.
 */
data class SupportCategory(val key: String, val label: String, val tint: Color)

val SupportCategories = listOf(
    SupportCategory("animals", "Animals", Color(0xFFA16419)),
    SupportCategory("flowers", "Flowers", Color(0xFFB1537B)),
    SupportCategory("greenery", "Greenery", Color(0xFF1E8447)),
)

/**
 * Written rather than generated. The register matters more than the count: these should read as
 * a kind friend, not a motivational poster - and none of them claims to know how the person
 * using the app actually feels, because the app cannot know that.
 */
val SupportLines = listOf(
    "You showed up today. That counts.",
    "No one has to be impressive on a Tuesday.",
    "Rest is not something you have to earn.",
    "Someone, somewhere, is glad you are in the world.",
    "Small steps still move you forward.",
    "You are allowed to have a slow day.",
    "Take the sunny chair. It is yours.",
    "You have outlasted every hard day so far.",
    "Being here is enough. It really is.",
    "Say the kind thing you would say to a friend.",
    "There is no prize for pushing through.",
    "Some days the bravest thing is breakfast.",
    "Your story still has good pages left.",
    "It is alright to ask for company.",
    "Nothing wrong with a cup of tea and no plans.",
    "You are more than your worst week.",
    "Let yourself enjoy something small.",
    "Someone remembers a kindness you showed them.",
    "Call the person you keep meaning to call.",
    "You matter to people who do not say it often enough.",
    "This is a good day to be gentle with yourself.",
    "You have been through one hundred percent of your hardest days.",
    "Nobody is too old for a good afternoon.",
    "The afternoon is yours. No one else's.",
    "A little rest now is a favour to tomorrow.",
    "You are doing better than you think.",
    "Look at something green for a moment.",
    "You do not have to earn a nice day.",
)
