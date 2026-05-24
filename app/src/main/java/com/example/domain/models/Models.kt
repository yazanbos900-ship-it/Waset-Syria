package com.example.domain.models

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val discountPrice: Double? = null,
    val imageUrl: String,
    val vendorId: String,
    val vendorName: String,
    val rating: Double,
    val reviewCount: Int,
    val description: String = "High quality premium product from our top vendors.",
    val specifications: List<String> = emptyList(),
    val Category: String
)

data class Vendor(
    val id: String,
    val name: String,
    val storeBannerUrl: String,
    val logoUrl: String,
    val description: String,
    val rating: Double,
    val followers: Int
)

data class Category(
    val id: String,
    val name: String,
    val iconName: String
)

data class Order(
    val id: String,
    val date: String,
    val status: OrderStatus,
    val totalAmount: Double,
    val items: List<CartItem>
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

enum class OrderStatus {
    PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
