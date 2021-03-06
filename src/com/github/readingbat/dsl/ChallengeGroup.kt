/*
 * Copyright © 2020 Paul Ambrose (pambrose@mac.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.readingbat.dsl

import com.github.readingbat.InvalidConfigurationException
import com.github.readingbat.InvalidPathException
import com.github.readingbat.dsl.LanguageType.*
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

@ReadingBatDslMarker
class ChallengeGroup<T : Challenge>(internal val languageGroup: LanguageGroup<T>, internal val name: String) {
  internal val languageType = languageGroup.languageType
  internal val challenges = mutableListOf<T>()
  private val prefix by lazy { "${languageType.lowerName}/$name" }
  internal val parsedDescription
      by lazy {
        val options = MutableDataSet().apply { set(HtmlRenderer.SOFT_BREAK, "<br />\n") }
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(description.trimIndent())
        renderer.render(document)
      }

  // User properties
  var packageName = ""
  var description = ""

  fun hasChallenge(name: String) = challenges.any { it.name == name }

  fun findChallenge(name: String): T =
    challenges.firstOrNull { it.name == name }
      ?: throw InvalidPathException("Challenge $prefix/$name not found.")

  @ReadingBatDslMarker
  operator fun T.unaryPlus() {
    if (this@ChallengeGroup.hasChallenge(name))
      throw InvalidConfigurationException("Duplicate challenge name: $name")
    this@ChallengeGroup.challenges += this
  }

  @ReadingBatDslMarker
  fun challenge(name: String, block: T.() -> Unit) {
    if (hasChallenge(name))
      throw InvalidConfigurationException("Challenge $prefix/$name already exists")
    val challenge =
      when (languageType) {
        Python -> PythonChallenge(this)
        Java -> JavaChallenge(this)
        Kotlin -> KotlinChallenge(this)
      } as T

    challenges += challenge.apply { this.name = name }.apply(block).apply { validate() }
  }

  override fun toString() = "ChallengeGroup(name='$name', challenges=$challenges, packageName='$packageName')"
}
