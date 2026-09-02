package com.fyp.healthcare

import android.content.Context

/**
 * A built-in list of common medical conditions for the picker on the onboarding /
 * Edit Health Profile screen — so the user picks from a list instead of typing (and
 * spelling) a diagnosis, the same way [MedicationCatalog] / [AllergyCatalog] back the
 * medication and allergy pickers.
 *
 * The set is curated for this app from the **NHS Health A–Z** (nhs.uk/conditions), whose
 * plain-language condition names and one-line summaries this mirrors. It's the common
 * subset — the NHS A–Z has ~1,000 entries, many of them rare. Anything not listed is
 * covered by the picker's "Add a condition not listed" option, which works offline;
 * entries the user adds that way are remembered in [customConditions] so they show up
 * next time. [ConditionCatalogRemote] can refresh/extend the list from a hosted JSON.
 */
object ConditionCatalog {

    enum class Category(val label: String) {
        HEART("Heart & circulation"),
        LUNGS("Lungs & airways"),
        DIGESTIVE("Stomach & gut"),
        MENTAL("Mental health"),
        BRAIN("Brain & nerves"),
        BONES("Bones, joints & muscles"),
        SKIN("Skin & hair"),
        HORMONAL("Hormones & metabolism"),
        CANCER("Cancer"),
        INFECTION("Infections"),
        URINARY("Kidney & urinary"),
        BLOOD("Blood"),
        IMMUNE("Immune & autoimmune"),
        EYE_EAR("Eyes & ears"),
        SEXUAL("Sexual & reproductive"),
        OTHER("Other"),
        CUSTOM("Added by you");

        companion object {
            fun of(raw: String?): Category = entries.firstOrNull {
                it.name.equals(raw?.trim(), ignoreCase = true) ||
                    it.label.equals(raw?.trim(), ignoreCase = true)
            } ?: OTHER
        }
    }

    data class Entry(
        val name: String,
        val category: Category,
        val note: String = "",
    ) {
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

        Entry("Acne", Category.SKIN, "Spots, oily skin and sometimes painful lumps, mainly on the face and back"),
        Entry("Acid reflux (GORD)", Category.DIGESTIVE, "Stomach acid rising into the throat, causing heartburn"),
        Entry("Addison's disease", Category.HORMONAL, "The adrenal glands don't make enough hormones"),
        Entry("ADHD", Category.MENTAL, "Attention deficit hyperactivity disorder — affects attention, impulsivity and activity"),
        Entry("Alcohol dependence", Category.MENTAL, "A strong, hard-to-control urge to drink"),
        Entry("Alzheimer's disease", Category.BRAIN, "The most common cause of dementia"),
        Entry("Anaemia (iron deficiency)", Category.BLOOD, "Too few healthy red blood cells, often from low iron"),
        Entry("Angina", Category.HEART, "Chest pain caused by reduced blood flow to the heart muscle"),
        Entry("Ankylosing spondylitis", Category.BONES, "Long-term inflammation of the spine and nearby joints"),
        Entry("Anxiety disorder", Category.MENTAL, "Ongoing, excessive worry that's hard to control"),
        Entry("Aortic aneurysm", Category.HEART, "A bulge in the body's main artery"),
        Entry("Asthma", Category.LUNGS, "Narrowed, inflamed airways causing wheezing and breathlessness"),
        Entry("Atrial fibrillation", Category.HEART, "An irregular and often abnormally fast heartbeat"),
        Entry("Autism", Category.BRAIN, "A lifelong difference in how a person communicates and experiences the world"),
        Entry("Back pain (chronic)", Category.BONES, "Long-lasting pain in the lower or upper back"),
        Entry("Bell's palsy", Category.BRAIN, "Temporary weakness or drooping on one side of the face"),
        Entry("Benign prostate enlargement", Category.URINARY, "A larger prostate making it harder to pass urine"),
        Entry("Bipolar disorder", Category.MENTAL, "Extreme mood swings from depression to mania"),
        Entry("Bladder cancer", Category.CANCER, ""),
        Entry("Bone cancer", Category.CANCER, ""),
        Entry("Bowel cancer", Category.CANCER, "Cancer of the large bowel (colon or rectum)"),
        Entry("Brain tumour", Category.CANCER, "A growth of cells in or around the brain"),
        Entry("Breast cancer", Category.CANCER, ""),
        Entry("Bronchiectasis", Category.LUNGS, "Widened airways that make chest infections more likely"),
        Entry("Bronchitis", Category.LUNGS, "Inflammation of the airways, usually after an infection"),
        Entry("Bulimia and other eating disorders", Category.MENTAL, "A serious mental health condition centred on eating"),
        Entry("Carpal tunnel syndrome", Category.BONES, "Tingling and numbness in the hand from a squashed nerve"),
        Entry("Cataracts", Category.EYE_EAR, "Cloudy patches in the lens of the eye that blur vision"),
        Entry("Cellulitis", Category.INFECTION, "A bacterial skin infection causing redness, heat and swelling"),
        Entry("Cerebral palsy", Category.BRAIN, "A group of lifelong conditions affecting movement and coordination"),
        Entry("Cervical cancer", Category.CANCER, ""),
        Entry("Chickenpox", Category.INFECTION, "A common, itchy, spotty rash, usually in children"),
        Entry("Chronic fatigue syndrome (ME/CFS)", Category.OTHER, "Extreme tiredness that doesn't improve with rest"),
        Entry("Chronic kidney disease", Category.URINARY, "A long-term drop in how well the kidneys work"),
        Entry("Chronic obstructive pulmonary disease (COPD)", Category.LUNGS, "Long-term lung damage that makes breathing hard"),
        Entry("Cirrhosis", Category.DIGESTIVE, "Scarring of the liver from long-term damage"),
        Entry("Coeliac disease", Category.DIGESTIVE, "An immune reaction to gluten that damages the gut lining"),
        Entry("Cold sores", Category.INFECTION, "Small blisters around the mouth caused by a virus"),
        Entry("Conjunctivitis", Category.EYE_EAR, "A red, sore, sticky eye"),
        Entry("Constipation (chronic)", Category.DIGESTIVE, "Infrequent or difficult bowel movements over a long time"),
        Entry("Coronary heart disease", Category.HEART, "Narrowed heart arteries from fatty build-up"),
        Entry("Crohn's disease", Category.DIGESTIVE, "A type of inflammatory bowel disease causing gut inflammation"),
        Entry("Cushing's syndrome", Category.HORMONAL, "Too much of the hormone cortisol in the body"),
        Entry("Cystic fibrosis", Category.LUNGS, "Inherited condition causing sticky mucus in the lungs and gut"),
        Entry("Cystitis", Category.URINARY, "Inflammation of the bladder, usually from a urine infection"),
        Entry("Deep vein thrombosis (DVT)", Category.HEART, "A blood clot in a deep vein, usually in the leg"),
        Entry("Dementia", Category.BRAIN, "A decline in memory, thinking and everyday function"),
        Entry("Depression", Category.MENTAL, "Persistent low mood and loss of interest that affects daily life"),
        Entry("Diabetes (type 1)", Category.HORMONAL, "The body makes no insulin, so blood sugar runs high"),
        Entry("Diabetes (type 2)", Category.HORMONAL, "The body doesn't use insulin well, so blood sugar runs high"),
        Entry("Diverticular disease", Category.DIGESTIVE, "Small bulges in the wall of the large intestine"),
        Entry("Down's syndrome", Category.OTHER, "A genetic condition present from birth"),
        Entry("Dry eye syndrome", Category.EYE_EAR, "Eyes that feel dry, gritty or sore"),
        Entry("Ear infection", Category.EYE_EAR, "Pain and sometimes hearing changes from infection in the ear"),
        Entry("Eczema (atopic)", Category.SKIN, "Dry, itchy, inflamed and cracked skin"),
        Entry("Endometriosis", Category.SEXUAL, "Tissue like the womb lining growing elsewhere, causing pain"),
        Entry("Epilepsy", Category.BRAIN, "A tendency to have recurrent seizures"),
        Entry("Erectile dysfunction", Category.SEXUAL, "Difficulty getting or keeping an erection"),
        Entry("Fibroids", Category.SEXUAL, "Non-cancerous growths in or around the womb"),
        Entry("Fibromyalgia", Category.BONES, "Widespread body pain and tenderness with fatigue and poor sleep"),
        Entry("Flu (influenza)", Category.INFECTION, "A common viral infection with fever, aches and tiredness"),
        Entry("Food intolerance", Category.DIGESTIVE, "Trouble digesting certain foods, causing gut symptoms"),
        Entry("Fungal nail infection", Category.SKIN, "Thickened, discoloured, crumbly nails"),
        Entry("Gallstones", Category.DIGESTIVE, "Hard lumps that form in the gallbladder"),
        Entry("Gastritis", Category.DIGESTIVE, "Inflammation of the stomach lining"),
        Entry("Gastroenteritis", Category.DIGESTIVE, "A short-lived stomach bug with diarrhoea and vomiting"),
        Entry("Generalised anxiety disorder", Category.MENTAL, "Long-term anxiety about many different things"),
        Entry("Genital herpes", Category.SEXUAL, "A sexually transmitted infection causing blisters and sores"),
        Entry("Gestational diabetes", Category.HORMONAL, "High blood sugar that develops during pregnancy"),
        Entry("Glandular fever", Category.INFECTION, "A viral infection with fever, sore throat and fatigue"),
        Entry("Glaucoma", Category.EYE_EAR, "Raised pressure in the eye that can damage sight"),
        Entry("Gout", Category.BONES, "Sudden, severe joint pain from uric acid crystals"),
        Entry("Graves' disease", Category.HORMONAL, "An autoimmune cause of an overactive thyroid"),
        Entry("Haemochromatosis", Category.BLOOD, "The body absorbs and stores too much iron"),
        Entry("Haemophilia", Category.BLOOD, "An inherited condition where blood doesn't clot properly"),
        Entry("Haemorrhoids (piles)", Category.DIGESTIVE, "Swollen blood vessels around the back passage"),
        Entry("Hay fever", Category.IMMUNE, "An allergic reaction to pollen causing sneezing and itchy eyes"),
        Entry("Heart failure", Category.HEART, "The heart can't pump blood around the body as well as it should"),
        Entry("Hepatitis B", Category.INFECTION, "A viral infection of the liver spread through blood and body fluids"),
        Entry("Hepatitis C", Category.INFECTION, "A blood-borne viral infection of the liver"),
        Entry("Hiatus hernia", Category.DIGESTIVE, "Part of the stomach pushing up into the chest"),
        Entry("High blood pressure (hypertension)", Category.HEART, "Blood pressure that stays higher than the healthy range"),
        Entry("High cholesterol", Category.HEART, "Too much fatty substance in the blood, raising heart-disease risk"),
        Entry("HIV", Category.IMMUNE, "A virus that damages the immune system; well controlled with treatment"),
        Entry("Hodgkin lymphoma", Category.CANCER, "A cancer of the lymphatic system"),
        Entry("Huntington's disease", Category.BRAIN, "An inherited condition that damages nerve cells over time"),
        Entry("Hyperthyroidism (overactive thyroid)", Category.HORMONAL, "The thyroid makes too much hormone, speeding the body up"),
        Entry("Hypothyroidism (underactive thyroid)", Category.HORMONAL, "The thyroid makes too little hormone, slowing the body down"),
        Entry("Impetigo", Category.INFECTION, "A contagious bacterial skin infection with golden crusts"),
        Entry("Inflammatory bowel disease", Category.DIGESTIVE, "Long-term inflammation of the gut (Crohn's or ulcerative colitis)"),
        Entry("Insomnia", Category.MENTAL, "Regular trouble getting to sleep or staying asleep"),
        Entry("Irritable bowel syndrome (IBS)", Category.DIGESTIVE, "A common gut condition with cramps, bloating and altered bowel habit"),
        Entry("Kidney cancer", Category.CANCER, ""),
        Entry("Kidney infection", Category.URINARY, "A painful urine infection that has reached the kidney"),
        Entry("Kidney stones", Category.URINARY, "Hard stones that form in the kidneys and can be very painful"),
        Entry("Labyrinthitis", Category.EYE_EAR, "Inner-ear inflammation causing dizziness and balance problems"),
        Entry("Lactose intolerance", Category.DIGESTIVE, "Trouble digesting the sugar in milk"),
        Entry("Laryngitis", Category.LUNGS, "Inflammation of the voice box causing a hoarse voice"),
        Entry("Leukaemia", Category.CANCER, "A cancer of the blood-forming cells"),
        Entry("Liver disease (non-alcoholic fatty)", Category.DIGESTIVE, "A build-up of fat in the liver"),
        Entry("Long COVID", Category.OTHER, "Symptoms that carry on for weeks or months after a COVID infection"),
        Entry("Lung cancer", Category.CANCER, ""),
        Entry("Lupus", Category.IMMUNE, "An autoimmune condition that can affect skin, joints and organs"),
        Entry("Lyme disease", Category.INFECTION, "A bacterial infection spread by infected ticks"),
        Entry("Lymphoedema", Category.OTHER, "Swelling from a build-up of fluid in the body's tissues"),
        Entry("Macular degeneration", Category.EYE_EAR, "Loss of central vision, common with older age"),
        Entry("Malaria", Category.INFECTION, "A serious infection spread by mosquito bites in some countries"),
        Entry("Measles", Category.INFECTION, "A very infectious viral illness with fever and a rash"),
        Entry("Ménière's disease", Category.EYE_EAR, "Attacks of vertigo, hearing loss and ringing in one ear"),
        Entry("Meningitis", Category.INFECTION, "Inflammation of the lining around the brain and spinal cord"),
        Entry("Menopause", Category.HORMONAL, "When periods stop and hormone levels fall, usually around age 45–55"),
        Entry("Migraine", Category.BRAIN, "Recurring moderate-to-severe headaches, often with nausea and light sensitivity"),
        Entry("Motor neurone disease", Category.BRAIN, "A rare condition that progressively damages the nerves controlling movement"),
        Entry("Mouth cancer", Category.CANCER, ""),
        Entry("Multiple sclerosis (MS)", Category.BRAIN, "The immune system attacks the protective coating around nerves"),
        Entry("Mumps", Category.INFECTION, "A viral infection causing painful swelling of the salivary glands"),
        Entry("Muscular dystrophy", Category.BONES, "A group of inherited conditions that gradually weaken the muscles"),
        Entry("Myasthenia gravis", Category.IMMUNE, "An autoimmune condition causing muscle weakness that worsens with activity"),
        Entry("Nasal polyps", Category.LUNGS, "Soft growths inside the nose that can block airflow"),
        Entry("Non-Hodgkin lymphoma", Category.CANCER, "A group of blood cancers of the lymphatic system"),
        Entry("Norovirus", Category.DIGESTIVE, "The 'winter vomiting bug'"),
        Entry("Obesity", Category.HORMONAL, "Carrying excess body weight that affects health"),
        Entry("Obsessive compulsive disorder (OCD)", Category.MENTAL, "Unwanted thoughts and repetitive behaviours that are hard to control"),
        Entry("Oesophageal cancer", Category.CANCER, "Cancer of the food pipe"),
        Entry("Osteoarthritis", Category.BONES, "Wear-related joint damage causing pain and stiffness"),
        Entry("Osteoporosis", Category.BONES, "Weak, fragile bones that break more easily"),
        Entry("Ovarian cancer", Category.CANCER, ""),
        Entry("Overactive bladder", Category.URINARY, "A sudden, frequent urge to pass urine"),
        Entry("Pancreatic cancer", Category.CANCER, ""),
        Entry("Pancreatitis", Category.DIGESTIVE, "Inflammation of the pancreas, causing severe tummy pain"),
        Entry("Panic disorder", Category.MENTAL, "Regular, unexpected panic attacks"),
        Entry("Parkinson's disease", Category.BRAIN, "A condition causing tremor, stiffness and slow movement"),
        Entry("Pelvic inflammatory disease", Category.SEXUAL, "Infection of the upper female reproductive system"),
        Entry("Peptic ulcer", Category.DIGESTIVE, "A sore in the lining of the stomach or small intestine"),
        Entry("Pericarditis", Category.HEART, "Inflammation of the sac around the heart"),
        Entry("Peripheral arterial disease", Category.HEART, "Narrowed leg arteries causing pain on walking"),
        Entry("Peripheral neuropathy", Category.BRAIN, "Nerve damage causing numbness, tingling or pain, often in the feet"),
        Entry("Pernicious anaemia", Category.BLOOD, "An autoimmune cause of vitamin B12 deficiency"),
        Entry("Pleurisy", Category.LUNGS, "Inflammation of the lining of the lungs, causing sharp chest pain"),
        Entry("Pneumonia", Category.LUNGS, "Swelling of the lung tissue, usually from an infection"),
        Entry("Polycystic ovary syndrome (PCOS)", Category.HORMONAL, "A common hormone condition affecting the ovaries"),
        Entry("Polymyalgia rheumatica", Category.BONES, "Pain and stiffness in the shoulders, neck and hips, mainly over 65"),
        Entry("Post-traumatic stress disorder (PTSD)", Category.MENTAL, "Anxiety and flashbacks after a frightening or distressing event"),
        Entry("Postnatal depression", Category.MENTAL, "Depression that develops in the weeks and months after having a baby"),
        Entry("Pre-eclampsia", Category.SEXUAL, "High blood pressure and protein in the urine during pregnancy"),
        Entry("Pressure ulcers", Category.SKIN, "Skin and tissue damage from staying in one position too long"),
        Entry("Prostate cancer", Category.CANCER, ""),
        Entry("Psoriasis", Category.SKIN, "Flaky, crusty patches of skin covered with silvery scales"),
        Entry("Psoriatic arthritis", Category.BONES, "Joint inflammation that affects some people with psoriasis"),
        Entry("Pulmonary embolism", Category.LUNGS, "A blood clot that has travelled to the lungs"),
        Entry("Pulmonary fibrosis", Category.LUNGS, "Scarring of the lungs that makes breathing harder over time"),
        Entry("Raynaud's", Category.HEART, "Fingers and toes going white and numb in the cold"),
        Entry("Reactive arthritis", Category.BONES, "Joint pain and swelling triggered by an infection elsewhere"),
        Entry("Restless legs syndrome", Category.BRAIN, "An overwhelming urge to move the legs, worse at rest"),
        Entry("Rheumatoid arthritis", Category.IMMUNE, "The immune system attacks the joints, causing pain and swelling"),
        Entry("Ringworm", Category.SKIN, "A common fungal skin infection with a ring-shaped rash"),
        Entry("Rosacea", Category.SKIN, "Redness and flushing across the nose and cheeks"),
        Entry("Scabies", Category.SKIN, "An itchy rash caused by tiny mites burrowing into the skin"),
        Entry("Scarlet fever", Category.INFECTION, "A bacterial illness with a pinkish rash, mostly in young children"),
        Entry("Schizophrenia", Category.MENTAL, "A long-term condition affecting thoughts, feelings and perception"),
        Entry("Sciatica", Category.BONES, "Pain travelling from the lower back down the leg from an irritated nerve"),
        Entry("Scoliosis", Category.BONES, "A sideways curve of the spine"),
        Entry("Seasonal affective disorder (SAD)", Category.MENTAL, "Depression that comes and goes with the seasons"),
        Entry("Sepsis", Category.INFECTION, "A life-threatening reaction of the body to an infection"),
        Entry("Shingles", Category.INFECTION, "A painful rash caused by the reactivated chickenpox virus"),
        Entry("Sickle cell disease", Category.BLOOD, "An inherited condition affecting the shape of red blood cells"),
        Entry("Sinusitis", Category.LUNGS, "Swelling of the sinuses, usually after a cold"),
        Entry("Sjögren's syndrome", Category.IMMUNE, "An autoimmune condition causing dry eyes and dry mouth"),
        Entry("Skin cancer (melanoma)", Category.CANCER, "A serious skin cancer that can spread if not caught early"),
        Entry("Skin cancer (non-melanoma)", Category.CANCER, "The most common, usually slow-growing skin cancers"),
        Entry("Sleep apnoea", Category.LUNGS, "Breathing repeatedly stops and starts during sleep"),
        Entry("Slipped disc", Category.BONES, "A disc in the spine pressing on a nerve, causing back and leg pain"),
        Entry("Stomach cancer", Category.CANCER, ""),
        Entry("Stroke", Category.BRAIN, "The blood supply to part of the brain is cut off"),
        Entry("Temporal arteritis", Category.IMMUNE, "Inflamed arteries in the head causing headache and jaw pain"),
        Entry("Testicular cancer", Category.CANCER, ""),
        Entry("Thalassaemia", Category.BLOOD, "An inherited condition affecting how the body makes haemoglobin"),
        Entry("Thrush", Category.INFECTION, "A common yeast infection of the mouth or genitals"),
        Entry("Thyroid cancer", Category.CANCER, ""),
        Entry("Tinnitus", Category.EYE_EAR, "Hearing sounds like ringing that don't come from outside"),
        Entry("Tonsillitis", Category.INFECTION, "Inflammation of the tonsils, causing a sore throat"),
        Entry("Transient ischaemic attack (TIA)", Category.BRAIN, "A 'mini-stroke' with temporary stroke-like symptoms"),
        Entry("Trigeminal neuralgia", Category.BRAIN, "Sudden, severe facial pain like an electric shock"),
        Entry("Tuberculosis (TB)", Category.INFECTION, "A bacterial infection that mainly affects the lungs"),
        Entry("Ulcerative colitis", Category.DIGESTIVE, "A type of inflammatory bowel disease affecting the colon and rectum"),
        Entry("Urinary incontinence", Category.URINARY, "Leaking urine that you can't fully control"),
        Entry("Urinary tract infection (UTI)", Category.URINARY, "An infection of the bladder, urethra or kidneys"),
        Entry("Urticaria (hives)", Category.SKIN, "A raised, itchy rash that can come and go"),
        Entry("Varicose veins", Category.HEART, "Swollen, twisted veins, usually in the legs"),
        Entry("Vertigo", Category.EYE_EAR, "A spinning sensation, often from an inner-ear problem"),
        Entry("Vitamin B12 deficiency", Category.BLOOD, "Low B12, causing tiredness, pins and needles and low mood"),
        Entry("Vitamin D deficiency", Category.HORMONAL, "Low vitamin D, which can lead to bone and muscle pain"),
        Entry("Vitiligo", Category.SKIN, "Pale white patches on the skin from loss of pigment"),
        Entry("Warts and verrucas", Category.SKIN, "Rough lumps on the skin caused by a virus"),
        Entry("Whooping cough", Category.INFECTION, "A bacterial infection causing long bouts of coughing"),
        Entry("Womb (uterus) cancer", Category.CANCER, ""),
    )

    // ===================================================================
    //  The user's own additions — remembered locally, offline.
    // ===================================================================

    private fun prefs(context: Context) =
        context.getSharedPreferences(scopedPrefsName("condition_custom"), Context.MODE_PRIVATE)

    fun customConditions(context: Context): List<String> =
        prefs(context).getString(K_CUSTOM, "").orEmpty()
            .split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    fun addCustomCondition(context: Context, raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) return
        val known = (BUILT_IN.map { it.name } +
            ConditionCatalogRemote.cachedEntries(context).map { it.name } +
            customConditions(context))
            .any { it.equals(name, ignoreCase = true) }
        if (known) return
        val updated = customConditions(context) + name
        prefs(context).edit().putString(K_CUSTOM, updated.joinToString("\n")).apply()
    }

    private const val K_CUSTOM = "custom_conditions_v1"

    // ===================================================================
    //  Merged view used by the picker.
    // ===================================================================

    fun all(context: Context): List<Entry> {
        val byKey = LinkedHashMap<String, Entry>()
        fun add(e: Entry) { byKey.putIfAbsent(e.name.lowercase(), e) }

        BUILT_IN.forEach(::add)
        ConditionCatalogRemote.cachedEntries(context).forEach(::add)
        customConditions(context).forEach { add(Entry(it, Category.CUSTOM)) }

        return byKey.values.sortedBy { it.name.lowercase() }
    }

    fun search(context: Context, query: String): List<Entry> =
        all(context).let { list -> if (query.isBlank()) list else list.filter { it.matches(query) } }
}
