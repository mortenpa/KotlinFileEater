package com.hrblizz.fileapi.data.repository

import com.hrblizz.fileapi.data.entities.FileEntity
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface FileRepository: MongoRepository<FileEntity, UUID>