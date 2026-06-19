package com.app.newsapp.models

/**
 * Singleton data source providing all hardcoded dummy articles,
 * featured articles, and category list for the app.
 * No network calls — all data is local for Assignment 3.
 */
object DataSource {

    fun getArticles(): List<Article> = listOf(
        Article(
            id = 1,
            title = "AI Breakthrough Changes the Way We Work Forever",
            source = "BBC News",
            category = "Technology",
            description = "Researchers at MIT have developed a new AI model that can understand and generate human-like reasoning at unprecedented speeds. The model, dubbed 'ThinkNet', has already shown promising results across medical diagnostics, legal analysis, and software development. Industry experts believe this could fundamentally reshape the global workforce over the next decade.",
            timeAgo = "2 hours ago",
            readTime = "5 min read",
            tags = listOf("AI", "Tech", "Future")
        ),
        Article(
            id = 2,
            title = "Pakistan Wins Historic Cricket Series Against India",
            source = "Dawn News",
            category = "Sports",
            description = "In a nail-biting final match at Lahore's Gaddafi Stadium, Pakistan defeated India by 3 wickets in the last over to clinch the bilateral series 3-2. Captain Babar Azam's unbeaten 87 and Shaheen Afridi's crucial 4-wicket haul were the highlights of a match that drew a record live audience of 1.2 billion viewers globally.",
            timeAgo = "4 hours ago",
            readTime = "3 min read",
            tags = listOf("Cricket", "Sports", "Pakistan")
        ),
        Article(
            id = 3,
            title = "Global Markets Surge as Inflation Fears Ease",
            source = "Reuters",
            category = "Business",
            description = "Stock markets around the world saw sharp gains on Tuesday after the US Federal Reserve signalled a potential pause in its rate-hiking cycle. The S&P 500 gained 2.3%, while European and Asian markets followed suit. Analysts credit easing consumer price data and strong corporate earnings for restoring investor confidence.",
            timeAgo = "6 hours ago",
            readTime = "6 min read",
            tags = listOf("Economy", "Markets", "Finance")
        ),
        Article(
            id = 4,
            title = "New Study Links Sleep Quality to Heart Health",
            source = "Health Today",
            category = "Health",
            description = "A landmark study published in The Lancet tracking over 90,000 adults for 12 years found that people who sleep fewer than 6 hours per night have a 34% higher risk of developing cardiovascular disease. The research underscores the critical importance of sleep hygiene and calls for new public health guidelines on rest and recovery.",
            timeAgo = "8 hours ago",
            readTime = "4 min read",
            tags = listOf("Health", "Sleep", "Heart")
        ),
        Article(
            id = 5,
            title = "SpaceX Launches 200th Mission Successfully",
            source = "Space.com",
            category = "Science",
            description = "SpaceX reached a historic milestone yesterday with the successful launch of its 200th orbital mission, deploying a batch of 60 Starlink satellites from Cape Canaveral. The Falcon 9 booster completed its 15th flight and landing, a new company record. Elon Musk celebrated the achievement calling it 'a testament to rapid reusability.'",
            timeAgo = "10 hours ago",
            readTime = "5 min read",
            tags = listOf("Space", "Science", "SpaceX")
        ),
        Article(
            id = 6,
            title = "Oscar Winners 2026 — Full List Revealed",
            source = "Variety",
            category = "Entertainment",
            description = "The 98th Academy Awards ceremony held last night at the Dolby Theatre in Hollywood delivered some of the biggest surprises in Oscar history. 'Echoes of Tomorrow' swept the major categories, winning Best Picture, Best Director, and Best Actress. The evening also saw a historic win for a South Korean production in the animated feature category.",
            timeAgo = "12 hours ago",
            readTime = "4 min read",
            tags = listOf("Oscars", "Movies", "Entertainment")
        ),
        Article(
            id = 7,
            title = "Electric Vehicles Now Outsell Petrol Cars in Europe",
            source = "The Guardian",
            category = "Business",
            description = "For the first time in history, EV sales have surpassed traditional petrol-powered vehicles across the European Union, according to data from the European Automobile Manufacturers' Association. Battery-electric vehicles accounted for 51.2% of all new car registrations in Q1 2026, driven by falling battery costs and expanded charging infrastructure.",
            timeAgo = "1 day ago",
            readTime = "6 min read",
            tags = listOf("EV", "Cars", "Environment")
        ),
        Article(
            id = 8,
            title = "Scientists Discover New Species in Amazon Rainforest",
            source = "National Geographic",
            category = "Science",
            description = "A team of biologists exploring the deep Amazon basin in Brazil has catalogued 14 new species of plants and insects, including a bioluminescent frog species that glows blue under UV light. The discovery, made possible through drone-assisted mapping and AI image recognition, highlights the Amazon's untapped biodiversity and the urgent need for conservation.",
            timeAgo = "1 day ago",
            readTime = "5 min read",
            tags = listOf("Nature", "Science", "Amazon")
        )
    )

    fun getFeaturedArticles(): List<Article> = getArticles().take(5)

    fun getCategories(): List<String> = listOf(
        "Top Stories",
        "Technology",
        "Sports",
        "Business",
        "Health",
        "Science",
        "Entertainment"
    )

    fun getSavedArticles(): List<Article> = getArticles().take(3)
}
