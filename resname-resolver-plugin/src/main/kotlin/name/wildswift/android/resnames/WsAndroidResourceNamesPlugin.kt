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
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Created by swift
 */
class WsAndroidResourceNamesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.findByType(AppExtension::class.java)?.applicationVariants?.all {
            processVariant(target, it)
        }
        target.extensions.findByType(LibraryExtension::class.java)?.libraryVariants?.all {
            processVariant(target, it)
        }
        target.extensions.findByType(FeatureExtension::class.java)?.libraryVariants?.all {
            processVariant(target, it)
        }
        target.extensions.findByType(FeatureExtension::class.java)?.featureVariants?.all {
            processVariant(target, it)
        }
    }

    private fun processVariant(project: Project, variant: BaseVariant) {
        variant.outputs.all {

            val outputDir = project.buildDir.resolve("generated/source/resNames/${variant.dirName}/${it.dirName}")

            val task = project.tasks.create("generateResNames${it.name.capitalize()}", GenerateResourceNamesTask::class.java)
            task.outputs.dir(outputDir)
            variant.registerJavaGeneratingTask(task, outputDir)

            val processResources = it.processResources
            task.dependsOn(processResources)

            val pack = variant.applicationId

            val rFile = processResources.sourceOutputDir.resolve(pack.replace('.', File.separatorChar)).resolve("R.java")

            task.inputs.file(rFile)
        }

    }
}