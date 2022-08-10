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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by swift
 */
class WsAndroidResourceNamesPlugin : Plugin<Project> {
    companion object {
        private const val APP_ID_KEY = "applicationId"
        private const val DEBUG = false
    }


    override fun apply(target: Project) {
        target.extensions.findByType(AppExtension::class.java)?.applicationVariants?.all {
            processVariant(target, it)
        }
        target.extensions.findByType(LibraryExtension::class.java)?.libraryVariants?.all {
            processVariant(target, it)
        }
    }

    private fun processVariant(project: Project, variant: BaseVariant) {
        val pack = variant.applicationId

        variant.outputs.all { output ->
            val processResources = output.processResourcesProvider

            if (DEBUG) {
                println("dir = ${processResources.get().sourceOutputDir}, pack = $pack")
            }

            val rFile = processResources.get().outputs.files.files.firstOrNull { it.toString().endsWith("R.jar") }

            val outputDir = project.buildDir.resolve("generated/source/resNames/${variant.dirName}/${output.dirName}")

            if (rFile != null) {
                val task = project.tasks
                        .create("generateResNames${output.name.capitalize()}", GenerateResourceNamesFromJarTask::class.java)
                        .apply {
                            setProperty(APP_ID_KEY, pack)
                            inputs.file(rFile)
                            outputs.dir(outputDir)
                            dependsOn(processResources)
                        }
                variant.registerJavaGeneratingTask(task, outputDir)
            }
        }

    }
}