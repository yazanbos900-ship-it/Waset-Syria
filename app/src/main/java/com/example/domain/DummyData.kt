package com.example.domain

import com.example.domain.models.*

object DummyData {
    val categories = listOf(
        Category("1", "Electronics", "Devices"),
        Category("2", "Fashion", "Checkroom"),
        Category("3", "Groceries", "LocalPizza"),
        Category("4", "Beauty", "FaceRetouchingNatural"),
        Category("5", "Home", "Chair")
    )

    val vendors = listOf(
        Vendor("v1", "TechMart", "https://picsum.photos/800/300?random=1", "https://picsum.photos/200/200?random=2", "The best in electronics.", 4.8, 1200),
        Vendor("v2", "FashionHub", "https://picsum.photos/800/300?random=3", "https://picsum.photos/200/200?random=4", "Latest trends for everyone.", 4.5, 3400),
        Vendor("v3", "Fresh Foods", "https://picsum.photos/800/300?random=5", "https://picsum.photos/200/200?random=6", "Daily groceries and more.", 4.9, 8900)
    )

    val products = listOf(
        Product("p1", "Wireless Noise Cancelling Headphones", 299.99, 249.99, "https://picsum.photos/400/400?random=10", "v1", "TechMart", 4.7, 456, Category = "Electronics"),
        Product("p2", "Smartphone 14 Pro 256GB", 1099.99, null, "https://picsum.photos/400/400?random=11", "v1", "TechMart", 4.9, 1289, Category = "Electronics"),
        Product("p3", "Men's Classic T-Shirt", 25.00, 19.99, "https://picsum.photos/400/400?random=12", "v2", "FashionHub", 4.3, 230, Category = "Fashion"),
        Product("p4", "Women's Running Shoes", 120.00, 89.99, "https://picsum.photos/400/400?random=13", "v2", "FashionHub", 4.6, 540, Category = "Fashion"),
        Product("p5", "Organic Avocados (Pack of 4)", 8.99, null, "https://picsum.photos/400/400?random=14", "v3", "Fresh Foods", 4.8, 89, Category = "Groceries"),
        Product("p6", "Smart Watch Series 8", 399.00, 349.00, "https://picsum.photos/400/400?random=15", "v1", "TechMart", 4.8, 670, Category = "Electronics"),
    )
}
