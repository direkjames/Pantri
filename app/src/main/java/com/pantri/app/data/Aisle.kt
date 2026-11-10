package com.pantri.app.data

/** Store aisles, in the order you usually walk them. [keywords] guess the aisle for a new item. */
enum class Aisle(val label: String, val keywords: List<String>) {
    Produce("Produce", listOf("tomato", "carrot", "onion", "garlic", "ginger", "potato", "cabbage", "lettuce", "spinach", "kangkong", "pechay", "eggplant", "squash", "papaya", "banana", "apple", "mango", "lemon", "calamansi", "orange", "pepper", "chili", "leaves", "cucumber", "corn", "fruit", "vegetable", "herb", "scallion", "celery", "okra", "beans")),
    Meat("Meat and seafood", listOf("chicken", "pork", "beef", "fish", "shrimp", "bangus", "tilapia", "squid", "sausage", "bacon", "ham", "hotdog", "longganisa", "tuna", "liempo", "ground", "meat")),
    Dairy("Dairy and eggs", listOf("milk", "egg", "cheese", "butter", "yogurt", "cream", "margarine")),
    Pantry("Pantry", listOf("rice", "noodle", "pasta", "sugar", "salt", "oil", "soy", "vinegar", "flour", "sauce", "canned", "sardines", "coffee", "tea", "oats", "cereal", "spice", "seasoning", "bean", "broth", "cube", "ketchup", "mayo", "peanut", "jam")),
    Bakery("Bakery", listOf("bread", "pandesal", "bun", "cake", "tortilla", "cookie", "pastry")),
    Frozen("Frozen", listOf("frozen", "ice cream", "nuggets", "fries")),
    Drinks("Drinks", listOf("juice", "soda", "water", "beer", "wine", "drink", "cola", "milk tea")),
    Household("Household", listOf("soap", "detergent", "tissue", "shampoo", "toothpaste", "dish", "sponge", "trash", "bleach", "napkin", "foil", "wrap", "bag", "diaper")),
    Other("Other", emptyList());

    companion object {
        fun from(name: String): Aisle = entries.firstOrNull { it.name == name } ?: Other

        /** Best guess from the item name, or [Other]. */
        fun guess(itemName: String): Aisle {
            val n = itemName.lowercase()
            return entries.firstOrNull { a -> a.keywords.any { n.contains(it) } } ?: Other
        }
    }
}
