package com.example.rootsharemobile.data.model

import com.google.gson.annotations.SerializedName

data class Species(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("scientificName")
    val scientificName: String? = null,

    @SerializedName("description")
    val description: String? = null
)
