package com.example.billup

import java.io.Serializable

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    var isSelected: Boolean = false
) : Serializable