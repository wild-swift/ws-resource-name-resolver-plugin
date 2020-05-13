/*
 * Copyright (C) 2019 Wild Swift
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.wildswift.android.resnames

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.lang.model.element.Modifier.*


/**
 * Created by swift
 */
@Suppress("ConstantConditionIf")
open class GenerateResourceNamesFromJarTask : DefaultTask() {
    companion object {
        private const val DEBUG = false
    }

    @Input
    var applicationId: String = ""

    @TaskAction
    fun generateResNamesFile() {
        val outputPath = outputs.files.files.firstOrNull() ?: return
        val archPath = applicationId.replace('.', '/')
        if (DEBUG) {
            inputs.files.files.joinToString(separator = "\n") { it.toString() }.let { println("input = $it") }
            println("output = $outputPath")
            println("path = $archPath")
        }
        inputs.files.files.forEach { file ->
            ZipFile(file).use { zipFile ->
                val classReader = InternalClassReader(zipFile)
                zipFile.entries()
                        .toList()
                        .filter { it.name.startsWith("$archPath/R$") }
                        .also { classes ->
                            if (DEBUG) {
                                println(classes.joinToString { it.name })
                            }
                        }
                        .map { entry ->
                            val members = classReader.loadMembersNames(entry) ?: return@map null
                            if (DEBUG) {
                                members.forEach {
                                    println("class ${entry.name}, entry = ${it}")
                                }
                            }
                            val name = entry.name.substring(archPath.length + "/R$".length).removeSuffix(".class")
                            convertToClass(name, members)
                        }
                        .map {
                            JavaFile.builder(applicationId, it)
                                    .addFileComment("Generated by ws-android-resource-names gradle plugin. Do not modify!")
                                    .build()
                        }
                        .forEach {
                            if (DEBUG) {
                                val output = ByteArrayOutputStream()
                                val outputStreamWriter = OutputStreamWriter(output)
                                it.writeTo(outputStreamWriter)
                                outputStreamWriter.flush()
                                println(String(output.toByteArray()))
                            }

                            it.writeTo(outputPath)
                        }
            }
        }
    }

    private fun convertToClass(name: String, membersList: List<String>): TypeSpec = TypeSpec
            .classBuilder(name.capitalize() + "RNames")
            .addModifiers(PUBLIC, FINAL)
            .let { typeSpec ->
                membersList
                        .map {
                            val fieldSpecBuilder = FieldSpec.builder(String::class.java, it)
                                    .addModifiers(PUBLIC, STATIC, FINAL)
                                    .initializer("\"$it\"")
                            fieldSpecBuilder.build()
                        }
                        .forEach {
                            typeSpec.addField(it)
                        }
                typeSpec
            }
            .build()

    private class InternalClassReader(private val zipFile: ZipFile) : ClassLoader() {

        fun loadMembersNames(entry: ZipEntry): List<String>? {
            val className = entry.name.replace('/', '.').removeSuffix(".class")
            try {
                return findClass(className).declaredFields?.map { it.name }
            } catch (e: ClassNotFoundException) {

            }
            val inputStream = zipFile.getInputStream(entry)
            val readBytes = inputStream.readBytes()
            return defineClass(className, readBytes, 0, readBytes.size).declaredFields?.map { it.name }
        }
    }

}