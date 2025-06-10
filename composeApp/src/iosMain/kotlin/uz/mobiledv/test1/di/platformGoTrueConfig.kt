package uz.mobiledv.test1.di

import io.github.jan.supabase.auth.AuthConfig

actual fun AuthConfig.platformGoTrueConfig() {
    scheme = "io.supabase.documentsaver" // A unique scheme for your iOS app
    host = "login"
}