package com.azizbek.ikeaplace

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Furniture item. Dimensions are real-world values in centimetres (width x depth x height). */
data class FurnitureItem(
    val id: String,
    @StringRes val nameRes: Int,
    val asset: String,
    @DrawableRes val thumb: Int,
    val widthCm: Int,
    val depthCm: Int,
    val heightCm: Int,
)

object Catalog {
    val items: List<FurnitureItem> = listOf(
        FurnitureItem("sofa_modern", R.string.m_sofa_modern, "models/sofa_modern.glb", R.drawable.thumb_sofa_modern, 220, 90, 90),
        FurnitureItem("sofa_classic", R.string.m_sofa_classic, "models/sofa_classic.glb", R.drawable.thumb_sofa_classic, 208, 90, 110),
        FurnitureItem("armchair", R.string.m_armchair, "models/armchair.glb", R.drawable.thumb_armchair, 88, 85, 93),
        FurnitureItem("dining_table", R.string.m_dining_table, "models/dining_table.glb", R.drawable.thumb_dining_table, 160, 90, 75),
        FurnitureItem("office_desk", R.string.m_office_desk, "models/office_desk.glb", R.drawable.thumb_office_desk, 140, 70, 76),
        FurnitureItem("modern_chair", R.string.m_modern_chair, "models/modern_chair.glb", R.drawable.thumb_modern_chair, 45, 45, 100),
        FurnitureItem("bed", R.string.m_bed, "models/bed.glb", R.drawable.thumb_bed, 170, 205, 100),
        FurnitureItem("wardrobe", R.string.m_wardrobe, "models/wardrobe.glb", R.drawable.thumb_wardrobe, 120, 60, 200),
        FurnitureItem("floor_lamp", R.string.m_floor_lamp, "models/floor_lamp.glb", R.drawable.thumb_floor_lamp, 40, 40, 155),
        FurnitureItem("bookshelf", R.string.m_bookshelf, "models/bookshelf.glb", R.drawable.thumb_bookshelf, 83, 30, 180),
    )

    fun byId(id: String?): FurnitureItem? = items.firstOrNull { it.id == id }
}
