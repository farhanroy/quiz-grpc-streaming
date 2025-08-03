package dev.farhanroy.kmpgrpcclient

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform