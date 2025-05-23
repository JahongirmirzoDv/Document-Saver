package uz.mobiledv.test1.di

import org.koin.dsl.module
import uz.mobiledv.test1.AppViewModel

val viewModelsModule = module {
    factory { AppViewModel(get()) }
}