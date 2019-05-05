/*
 * Copyright (C) 2018 Wild Swift
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

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import javax.lang.model.element.Modifier.*


/**
 * Created by swift
 */
@Suppress("ConstantConditionIf")
open class GenerateResourceNamesFromSourcesTask : DefaultTask() {
    companion object {
        private const val DEBUG = false
    }

    var applicationId: String = ""

    @TaskAction
    fun generateResNamesFile() {
        val outputPath = outputs.files.files.firstOrNull() ?: return
        if (DEBUG) {
            inputs.files.files.forEach { println(it) }
        }
        inputs.files.files.forEach {
            JavaParser
                    .parse(it)
                    .types[0]
                    .childNodes
                    .filter { it is ClassOrInterfaceDeclaration }
                    .map {
                        convertToClass(it as ClassOrInterfaceDeclaration)
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

    private fun convertToClass(declaration: ClassOrInterfaceDeclaration): TypeSpec = TypeSpec
            .classBuilder(declaration.nameAsString.capitalize()+ "RNames")
            .addModifiers(PUBLIC, FINAL)
            .let { typeSpec ->
                declaration.members
                        .mapNotNull { it as? FieldDeclaration }
                        .flatMap { it.variables }
                        .map {
                            val fieldSpecBuilder = FieldSpec.builder(String::class.java, it.nameAsString)
                                    .addModifiers(PUBLIC, STATIC, FINAL)
                                    .initializer("\"${it.nameAsString}\"")
                            fieldSpecBuilder.build()
                        }
                        .forEach {
                            typeSpec.addField(it)
                        }
                typeSpec
            }
            .build()

}