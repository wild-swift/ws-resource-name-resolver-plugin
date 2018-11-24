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

package name.wildswift.android.resnames.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.IOException

import org.junit.Assert.assertEquals

import org.gradle.testkit.runner.TaskOutcome.*
import java.util.concurrent.TimeUnit

/**
 * Created by swift
 */
class PluginTest {
    @get:Rule val testProjectDir = TemporaryFolder()

    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    private lateinit var localPropertiesFile: File
    private lateinit var appFolder: File

    @Before
    @Throws(IOException::class)
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
        settingsFile = testProjectDir.newFile("settings.gradle")
        localPropertiesFile = testProjectDir.newFile("local.properties")
        appFolder = testProjectDir.newFolder("app")
    }

    @Test
    @Throws(IOException::class)
    fun testGenerationWithApplicationModule() {
        buildFile.writeText("""
            |buildscript {
            |    ext.kotlin_version = '1.2.51'
            |    repositories {
            |        google()
            |        jcenter()
            |    }
            |    dependencies {
            |        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
            |    }
            |}
            |
            |allprojects {
            |    repositories {
            |        google()
            |        jcenter()
            |        mavenCentral()
            |    }
            |}
            |
            |task clean(type: Delete) {
            |    delete rootProject.buildDir
            |}
            |
        """.trimMargin())

        settingsFile.writeText("""
            |include ':app'
            |
        """.trimMargin())


        File("templates/application").copyRecursively(appFolder)
        File("../local.properties").copyTo(localPropertiesFile, true)

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments("build")
                .build()
        println(result.output)

        assertEquals(SUCCESS, result.task(":app:build")?.outcome)

//        Thread.sleep(TimeUnit.MINUTES.toMillis(10))
//                                          build/generated/source/resNames/name/wildswift/names
        val attrRNames = appFolder.resolve("build/generated/source/resNames/release/name/wildswift/names/AttrRNames.java").readLines().joinToString("\n")
        assertEquals("""
            |// Generated by ws-android-resource-names gradle plugin. Do not modify!
            |package name.wildswift.names;
            |
            |import java.lang.String;
            |
            |public final class AttrRNames {
            |  public static final String textIn = "textIn";
            |
            |  public static final String visibleColor = "visibleColor";
            |}
        """.trimMargin(), attrRNames)
    }
}