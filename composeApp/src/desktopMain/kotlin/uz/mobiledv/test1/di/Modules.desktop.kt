package uz.mobiledv.test1.di

import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.repository.DocumentRepository
import uz.mobiledv.test1.repository.FakeDocumentRepository

actual val platformModule = module {
    single<DocumentRepository> { FakeDocumentRepository() }
}