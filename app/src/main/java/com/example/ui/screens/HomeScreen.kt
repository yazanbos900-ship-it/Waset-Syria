package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DummyData
import com.example.ui.components.ProductCard
import com.example.ui.components.VendorCard
import kotlinx.coroutines.delay

data class BannerData(
    val title: String,
    val subtitle: String,
    val backgroundColor: Color
)

val defaultBanners = listOf(
    BannerData("عروض نهاية الأسبوع", "خصم يصل إلى 50%", Color(0xFF1DB954)),
    BannerData("متاجر جديدة", "اكتشف أحدث المتاجر", Color(0xFF1A1A2E)),
    BannerData("توصيل مجاني", "على الطلبات فوق $30", Color(0xFF16213E))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProduct: (String) -> Unit,
    onNavigateToVendor: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(pageCount = { defaultBanners.size })
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            if (defaultBanners.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % defaultBanners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("WasetPlus", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Promotional Banner Pager
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) { page ->
                    val banner = defaultBanners[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(banner.backgroundColor)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = banner.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = banner.subtitle,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { /* TODO */ },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("تسوق الآن")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(defaultBanners.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color(0xFF1DB954) else Color.White.copy(alpha = 0.4f)
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search products, vendors...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories (simplified as text chips for now)
            SectionHeader(title = "Categories")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DummyData.categories) { category ->
                    AssistChip(
                        onClick = { /* TODO */ },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Featured Vendors
            SectionHeader(title = "Featured Vendors")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(DummyData.vendors) { vendor ->
                    VendorCard(
                        vendor = vendor,
                        onClick = { onNavigateToVendor(vendor.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trending Products
            SectionHeader(title = "Trending Products")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(DummyData.products) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onNavigateToProduct(product.id) },
                        onAddToCart = { /* TODO */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = { /* TODO */ }) {
            Text("See All", color = MaterialTheme.colorScheme.primary)
        }
    }
}
