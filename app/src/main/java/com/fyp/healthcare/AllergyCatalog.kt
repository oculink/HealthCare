package com.fyp.healthcare

import android.content.Context

/**
 * A built-in list of common allergens for the Allergies picker on the onboarding /
 * Edit Health Profile screen — so the user picks from a list instead of typing (and
 * spelling) an allergen, the same way [MedicationCatalog] backs the medication picker.
 *
 * The set is curated for this app from the source organisms in FARRP's **AllergenOnline**
 * database (allergenonline.org — a peer-reviewed allergen list, version 24 / Jan 2026),
 * plus the common **drug** and **contact** allergens that a protein-sequence database
 * doesn't cover. It is deliberately NOT the whole AllergenOnline database: that is 2,300+
 * protein sequences meant for bioinformatics, not a "what am I allergic to" list.
 *
 * Three layers, merged by [all]:
 *   1. [BUILT_IN]           — this file, always available offline.
 *   2. remote refresh       — [AllergyCatalogRemote] pulls an updated JSON when online so
 *                             the list can grow without shipping an app update.
 *   3. the user's own       — anything added via the picker's "Add an allergy not listed"
 *                             is remembered in [customAllergies] (works offline) so it
 *                             shows up pre-filled next time.
 */
object AllergyCatalog {

    enum class Category(val label: String) {
        FOOD("Food"),
        ENVIRONMENTAL("Environmental"),
        VENOM("Insect venom"),
        DRUG("Medication"),
        CONTACT("Contact / other"),
        CUSTOM("Added by you");

        companion object {
            fun of(raw: String?): Category = entries.firstOrNull {
                it.name.equals(raw?.trim(), ignoreCase = true) ||
                    it.label.equals(raw?.trim(), ignoreCase = true)
            } ?: CONTACT
        }
    }

    data class Entry(
        val name: String,
        val category: Category,
        val note: String = "",
    ) {
        /** "A".."Z" for a normal name, "#" for anything starting with a digit/symbol. */
        val section: String
            get() = name.firstOrNull()?.uppercaseChar()?.let {
                if (it in 'A'..'Z') it.toString() else "#"
            } ?: "#"

        fun matches(query: String): Boolean {
            val q = query.trim().lowercase()
            return q.isEmpty() ||
                name.lowercase().contains(q) ||
                note.lowercase().contains(q) ||
                category.label.lowercase().contains(q)
        }
    }

    // ===================================================================
    //  Bundled entries — guaranteed offline. Sorted A→Z by [all] / [search].
    // ===================================================================
    val BUILT_IN: List<Entry> = listOf(

        Entry("Peanut", Category.FOOD, "Legume; one of the most common causes of severe food reactions"),
        Entry("Almond", Category.FOOD, "Tree nut"),
        Entry("Brazil nut", Category.FOOD, "Tree nut"),
        Entry("Cashew", Category.FOOD, "Tree nut; often cross-reacts with pistachio"),
        Entry("Hazelnut", Category.FOOD, "Tree nut; linked to birch-pollen food syndrome"),
        Entry("Macadamia nut", Category.FOOD, "Tree nut"),
        Entry("Pecan", Category.FOOD, "Tree nut; cross-reacts with walnut"),
        Entry("Pine nut", Category.FOOD, "Seed of pine; occasional severe reactions"),
        Entry("Pistachio", Category.FOOD, "Tree nut; often cross-reacts with cashew"),
        Entry("Walnut", Category.FOOD, "Tree nut"),
        Entry("Chestnut", Category.FOOD, "Tree nut; can cross-react with latex"),
        Entry("Sesame", Category.FOOD, "Seed; a top-priority allergen, hidden in tahini and many breads"),
        Entry("Sunflower seed", Category.FOOD, "Seed"),
        Entry("Poppy seed", Category.FOOD, "Seed"),
        Entry("Mustard", Category.FOOD, "Seed / condiment; a priority allergen in Europe and Canada"),

        Entry("Cow's milk", Category.FOOD, "Dairy; common in young children, often outgrown"),
        Entry("Goat's milk", Category.FOOD, "Dairy; can cross-react with cow's milk"),
        Entry("Egg", Category.FOOD, "Mostly egg white; common in children, often outgrown"),

        Entry("Wheat", Category.FOOD, "Grain; separate from coeliac disease"),
        Entry("Gluten", Category.FOOD, "Protein in wheat, barley and rye"),
        Entry("Barley", Category.FOOD, "Grain"),
        Entry("Rye", Category.FOOD, "Grain"),
        Entry("Oat", Category.FOOD, "Grain"),
        Entry("Corn (maize)", Category.FOOD, "Grain; reactions are uncommon but reported"),
        Entry("Rice", Category.FOOD, "Grain"),
        Entry("Buckwheat", Category.FOOD, "Pseudo-grain; notable allergen in East Asia"),

        Entry("Soybean (soy)", Category.FOOD, "Legume; hidden in many processed foods"),
        Entry("Lentil", Category.FOOD, "Legume; a common pulse allergy in the Mediterranean"),
        Entry("Chickpea", Category.FOOD, "Legume"),
        Entry("Pea", Category.FOOD, "Legume; includes pea-protein isolates"),
        Entry("Green bean", Category.FOOD, "Legume"),
        Entry("Lupin", Category.FOOD, "Legume; flour used in baking, cross-reacts with peanut"),

        Entry("Fish (all)", Category.FOOD, "Finned fish in general; often lifelong"),
        Entry("Cod", Category.FOOD, "Finned fish"),
        Entry("Salmon", Category.FOOD, "Finned fish"),
        Entry("Tuna", Category.FOOD, "Finned fish"),
        Entry("Haddock", Category.FOOD, "Finned fish"),
        Entry("Mackerel", Category.FOOD, "Finned fish"),
        Entry("Sardine", Category.FOOD, "Finned fish"),
        Entry("Tilapia", Category.FOOD, "Finned fish"),
        Entry("Anchovy", Category.FOOD, "Finned fish; hidden in sauces and dressings"),

        Entry("Shellfish (all)", Category.FOOD, "Crustaceans and molluscs in general"),
        Entry("Shrimp / prawn", Category.FOOD, "Crustacean; the most common shellfish allergy"),
        Entry("Crab", Category.FOOD, "Crustacean"),
        Entry("Lobster", Category.FOOD, "Crustacean"),
        Entry("Crayfish", Category.FOOD, "Crustacean"),
        Entry("Clam", Category.FOOD, "Mollusc"),
        Entry("Mussel", Category.FOOD, "Mollusc"),
        Entry("Oyster", Category.FOOD, "Mollusc"),
        Entry("Scallop", Category.FOOD, "Mollusc"),
        Entry("Squid (calamari)", Category.FOOD, "Mollusc"),
        Entry("Octopus", Category.FOOD, "Mollusc"),
        Entry("Snail", Category.FOOD, "Mollusc; cross-reacts with dust mite"),

        Entry("Beef", Category.FOOD, "Red meat"),
        Entry("Pork", Category.FOOD, "Red meat; pork-cat syndrome in some people"),
        Entry("Lamb / mutton", Category.FOOD, "Red meat"),
        Entry("Chicken", Category.FOOD, "Poultry; can link to egg allergy (bird-egg syndrome)"),
        Entry("Turkey", Category.FOOD, "Poultry"),
        Entry("Alpha-gal (red meat)", Category.FOOD, "Delayed reaction to mammalian meat after a tick bite"),
        Entry("Gelatin", Category.FOOD, "From beef or pork; also used in some vaccines and capsules"),

        Entry("Apple", Category.FOOD, "Birch-pollen food syndrome; often only raw"),
        Entry("Peach", Category.FOOD, "Rosaceae fruit; LTP allergy can be severe"),
        Entry("Apricot", Category.FOOD, "Rosaceae fruit"),
        Entry("Cherry", Category.FOOD, "Rosaceae fruit"),
        Entry("Plum", Category.FOOD, "Rosaceae fruit"),
        Entry("Pear", Category.FOOD, "Rosaceae fruit"),
        Entry("Strawberry", Category.FOOD, "Berry"),
        Entry("Kiwi", Category.FOOD, "Can cause severe reactions; cross-reacts with latex"),
        Entry("Banana", Category.FOOD, "Latex-fruit syndrome"),
        Entry("Avocado", Category.FOOD, "Latex-fruit syndrome"),
        Entry("Mango", Category.FOOD, "Also a skin (contact) allergen from the peel"),
        Entry("Pineapple", Category.FOOD, "Contains the enzyme bromelain"),
        Entry("Melon", Category.FOOD, "Ragweed-pollen food syndrome"),
        Entry("Watermelon", Category.FOOD, "Ragweed-pollen food syndrome"),
        Entry("Fig", Category.FOOD, "Cross-reacts with fig-tree pollen and latex"),
        Entry("Grape", Category.FOOD, "Also raisins and wine"),
        Entry("Citrus (orange, lemon)", Category.FOOD, "Usually mild oral symptoms"),
        Entry("Coconut", Category.FOOD, "Botanically a fruit, not a true nut"),

        Entry("Celery", Category.FOOD, "A priority allergen in Europe; root, stalk and spice"),
        Entry("Carrot", Category.FOOD, "Birch- and mugwort-pollen food syndrome"),
        Entry("Tomato", Category.FOOD, "Can trigger oral and skin symptoms"),
        Entry("Potato", Category.FOOD, "Mostly reactions to raw potato"),
        Entry("Bell pepper", Category.FOOD, "Also paprika powder"),
        Entry("Garlic", Category.FOOD, "Also a contact allergen for cooks"),
        Entry("Onion", Category.FOOD, "Cross-reacts with garlic"),
        Entry("Spinach", Category.FOOD, "Can be high in histamine"),

        Entry("Cinnamon", Category.FOOD, "Spice; also a contact allergen"),
        Entry("Coriander / cilantro", Category.FOOD, "Spice; cross-reacts with mugwort pollen"),
        Entry("Cumin", Category.FOOD, "Spice"),
        Entry("Paprika", Category.FOOD, "Ground bell/chili pepper"),
        Entry("Black pepper", Category.FOOD, "Spice"),
        Entry("Cocoa / chocolate", Category.FOOD, "True cocoa allergy is rare; often milk, soy or nut"),
        Entry("Coffee", Category.FOOD, "Green-bean dust can also be an inhalant allergen"),
        Entry("Yeast", Category.FOOD, "Baker's and brewer's yeast"),
        Entry("Mushroom", Category.FOOD, "Cross-reacts with mould spores"),
        Entry("Honey", Category.FOOD, "May carry pollen proteins"),

        Entry("Sulfites", Category.FOOD, "Preservative in wine, dried fruit; can trigger asthma"),
        Entry("Monosodium glutamate (MSG)", Category.FOOD, "Flavour enhancer"),
        Entry("Carmine (cochineal)", Category.FOOD, "Red colouring from insects (E120)"),
        Entry("Tartrazine (E102)", Category.FOOD, "Yellow food dye"),
        Entry("Benzoates", Category.FOOD, "Preservative (E210–E213)"),

        Entry("Birch pollen", Category.ENVIRONMENTAL, "Spring tree pollen; drives many food cross-reactions"),
        Entry("Oak pollen", Category.ENVIRONMENTAL, "Spring tree pollen"),
        Entry("Olive / ash pollen", Category.ENVIRONMENTAL, "Spring tree pollen, major in the Mediterranean"),
        Entry("Plane tree pollen", Category.ENVIRONMENTAL, "Spring tree pollen, common in cities"),
        Entry("Cypress / cedar pollen", Category.ENVIRONMENTAL, "Winter–spring tree pollen"),
        Entry("Grass pollen", Category.ENVIRONMENTAL, "Timothy, ryegrass, Bermuda — the main summer hay-fever trigger"),
        Entry("Ragweed pollen", Category.ENVIRONMENTAL, "Late-summer weed pollen"),
        Entry("Mugwort pollen", Category.ENVIRONMENTAL, "Late-summer weed pollen; celery-mugwort-spice syndrome"),
        Entry("Nettle / pellitory pollen", Category.ENVIRONMENTAL, "Long weed-pollen season in warm climates"),
        Entry("Plantain pollen", Category.ENVIRONMENTAL, "Weed pollen"),

        Entry("House dust mite", Category.ENVIRONMENTAL, "Year-round indoor allergen in bedding and carpets"),
        Entry("Storage mite", Category.ENVIRONMENTAL, "In flour and stored grain"),
        Entry("Mould — Alternaria", Category.ENVIRONMENTAL, "Outdoor mould, linked to severe asthma"),
        Entry("Mould — Aspergillus", Category.ENVIRONMENTAL, "Indoor/outdoor mould"),
        Entry("Mould — Cladosporium", Category.ENVIRONMENTAL, "The most common airborne mould"),
        Entry("Mould — Penicillium", Category.ENVIRONMENTAL, "Indoor mould on damp walls and food"),
        Entry("Cockroach", Category.ENVIRONMENTAL, "Indoor allergen in dust, linked to asthma"),
        Entry("Feathers", Category.ENVIRONMENTAL, "Bedding and down jackets"),

        Entry("Cat dander", Category.ENVIRONMENTAL, "From skin, saliva and urine (Fel d 1)"),
        Entry("Dog dander", Category.ENVIRONMENTAL, "From skin, saliva and urine"),
        Entry("Horse dander", Category.ENVIRONMENTAL, "Can cause strong reactions"),
        Entry("Rabbit dander", Category.ENVIRONMENTAL, "Pet and lab-animal allergen"),
        Entry("Guinea pig dander", Category.ENVIRONMENTAL, "Pet allergen"),
        Entry("Mouse / rat", Category.ENVIRONMENTAL, "Urinary proteins; home and lab exposure"),
        Entry("Hamster", Category.ENVIRONMENTAL, "Pet allergen"),

        Entry("Honeybee sting", Category.VENOM, "Venom allergy can be life-threatening"),
        Entry("Bumblebee sting", Category.VENOM, "Cross-reacts with honeybee venom"),
        Entry("Wasp / yellow jacket sting", Category.VENOM, "The most common venom allergy in Europe"),
        Entry("Hornet sting", Category.VENOM, "Cross-reacts with wasp venom"),
        Entry("Paper wasp sting", Category.VENOM, "Common venom allergy in southern Europe and the US"),
        Entry("Fire ant sting", Category.VENOM, "Sterile pustule then possible systemic reaction"),
        Entry("Mosquito bite", Category.VENOM, "Large local reactions; rarely systemic"),
        Entry("Horsefly bite", Category.VENOM, "Painful bite, occasional allergic reaction"),
        Entry("Tick bite", Category.VENOM, "Can trigger alpha-gal (red-meat) allergy"),

        Entry("Penicillin", Category.DRUG, "The most commonly reported drug allergy"),
        Entry("Amoxicillin", Category.DRUG, "Penicillin-class antibiotic"),
        Entry("Cephalosporins", Category.DRUG, "Antibiotic class; small cross-reactivity with penicillin"),
        Entry("Sulfonamides (sulfa drugs)", Category.DRUG, "Antibiotics such as co-trimoxazole"),
        Entry("Macrolides (erythromycin)", Category.DRUG, "Antibiotic class"),
        Entry("Fluoroquinolones (ciprofloxacin)", Category.DRUG, "Antibiotic class"),
        Entry("Vancomycin", Category.DRUG, "Antibiotic; 'red man' infusion reaction"),
        Entry("Tetracyclines (doxycycline)", Category.DRUG, "Antibiotic class"),
        Entry("Aspirin", Category.DRUG, "NSAID; can trigger asthma and hives"),
        Entry("Ibuprofen / NSAIDs", Category.DRUG, "Pain and anti-inflammatory drugs"),
        Entry("Paracetamol (acetaminophen)", Category.DRUG, "Reactions are rare but reported"),
        Entry("Codeine", Category.DRUG, "Opioid; often a non-allergic histamine release"),
        Entry("Morphine", Category.DRUG, "Opioid"),
        Entry("Local anaesthetics (lidocaine)", Category.DRUG, "True allergy is rare; often the additive"),
        Entry("General anaesthetic agents", Category.DRUG, "Neuromuscular blockers are a common cause during surgery"),
        Entry("Iodinated contrast dye", Category.DRUG, "Used in CT scans; not the same as iodine or shellfish"),
        Entry("Insulin", Category.DRUG, "Local or systemic reactions, now uncommon"),
        Entry("Heparin", Category.DRUG, "Blood thinner; skin reactions and HIT"),
        Entry("ACE inhibitors", Category.DRUG, "Blood-pressure drugs; can cause angioedema"),
        Entry("Anticonvulsants (carbamazepine, phenytoin, lamotrigine)", Category.DRUG, "Can cause severe skin reactions"),
        Entry("Allopurinol", Category.DRUG, "Gout medicine; risk of severe skin reaction"),
        Entry("Chemotherapy agents (platinum, taxanes)", Category.DRUG, "Infusion reactions"),
        Entry("Vaccines", Category.DRUG, "Reactions are rare; usually to a component such as gelatin or egg"),
        Entry("Corticosteroids", Category.DRUG, "Rare delayed or immediate reactions"),

        Entry("Latex (natural rubber)", Category.CONTACT, "Gloves, balloons, catheters; cross-reacts with banana, avocado, kiwi"),
        Entry("Nickel", Category.CONTACT, "Jewellery, buckles, phone cases — the most common contact allergy"),
        Entry("Cobalt", Category.CONTACT, "Often alongside nickel; in metal and pigments"),
        Entry("Chromium", Category.CONTACT, "Tanned leather and cement"),
        Entry("Fragrance / perfume", Category.CONTACT, "Common cause of contact dermatitis"),
        Entry("Balsam of Peru", Category.CONTACT, "Marker fragrance; also in some foods and spices"),
        Entry("Paraphenylenediamine (PPD)", Category.CONTACT, "Permanent hair dye and black henna tattoos"),
        Entry("Formaldehyde", Category.CONTACT, "Preservative in cosmetics, textiles and nail products"),
        Entry("Methylisothiazolinone (MI)", Category.CONTACT, "Preservative in wet wipes, shampoos and paint"),
        Entry("Adhesive / sticking plaster", Category.CONTACT, "Colophony (rosin) or acrylate in the glue"),
        Entry("Neomycin", Category.CONTACT, "Antibiotic in topical creams and ear drops"),
        Entry("Bacitracin", Category.CONTACT, "Topical antibiotic ointment"),
        Entry("Lanolin (wool alcohols)", Category.CONTACT, "Moisturisers and ointments"),
        Entry("Wool", Category.CONTACT, "Irritation and, less often, true allergy"),
        Entry("Rubber accelerators (thiuram)", Category.CONTACT, "In gloves and elastic, separate from latex"),
        Entry("Sunscreen filters (oxybenzone)", Category.CONTACT, "Can cause photo-contact reactions"),
        Entry("Poison ivy / oak / sumac", Category.CONTACT, "Urushiol resin; blistering rash"),
        Entry("Household detergents", Category.CONTACT, "Irritant and, rarely, allergic hand dermatitis"),
        Entry("Disinfectants (chlorhexidine)", Category.CONTACT, "Skin prep and mouthwash; can cause anaphylaxis"),
        Entry("Iodine / povidone-iodine", Category.CONTACT, "Antiseptic skin reactions"),
        Entry("Acrylates (gel nails)", Category.CONTACT, "Nail salons and dental materials"),
        Entry("Dust / air pollution", Category.CONTACT, "Non-specific irritant trigger"),
        Entry("Cold urticaria", Category.CONTACT, "Hives brought on by cold air or water"),
        Entry("Sunlight (photosensitivity)", Category.CONTACT, "Rash on sun-exposed skin, sometimes drug-related"),
        Entry("Exercise-induced anaphylaxis", Category.CONTACT, "Sometimes only when combined with a trigger food"),
    )

    // ===================================================================
    //  The user's own additions — remembered locally, offline.
    // ===================================================================

    private fun prefs(context: Context) =
        context.getSharedPreferences(scopedPrefsName("allergy_custom"), Context.MODE_PRIVATE)

    /** Allergen names the user typed via "Add an allergy not listed", newest last. */
    fun customAllergies(context: Context): List<String> =
        prefs(context).getString(K_CUSTOM, "").orEmpty()
            .split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    /** Remember a user-typed allergen so it appears in the picker next time. No-op if it's
     *  blank or already known (built-in, remote or custom). */
    fun addCustomAllergy(context: Context, raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) return
        val known = (BUILT_IN.map { it.name } +
            AllergyCatalogRemote.cachedEntries(context).map { it.name } +
            customAllergies(context))
            .any { it.equals(name, ignoreCase = true) }
        if (known) return
        val updated = customAllergies(context) + name
        prefs(context).edit().putString(K_CUSTOM, updated.joinToString("\n")).apply()
    }

    private const val K_CUSTOM = "custom_allergies_v1"

    // ===================================================================
    //  Merged view used by the picker.
    // ===================================================================

    /** [BUILT_IN] ∪ remote refresh ∪ the user's own, de-duplicated case-insensitively by
     *  name (built-in wins the note/category), sorted A→Z. */
    fun all(context: Context): List<Entry> {
        val byKey = LinkedHashMap<String, Entry>()
        fun add(e: Entry) { byKey.putIfAbsent(e.name.lowercase(), e) }

        BUILT_IN.forEach(::add)
        AllergyCatalogRemote.cachedEntries(context).forEach(::add)
        customAllergies(context).forEach { add(Entry(it, Category.CUSTOM)) }

        return byKey.values.sortedBy { it.name.lowercase() }
    }

    /** The section letters present in [all], in display order. */
    fun sections(context: Context): List<String> = all(context).map { it.section }.distinct()

    fun search(context: Context, query: String): List<Entry> =
        all(context).let { list -> if (query.isBlank()) list else list.filter { it.matches(query) } }
}
