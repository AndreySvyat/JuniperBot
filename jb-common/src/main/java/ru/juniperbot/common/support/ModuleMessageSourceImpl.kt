/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.support

import org.springframework.context.support.ResourceBundleMessageSource
import java.text.MessageFormat
import java.util.*

class ModuleMessageSourceImpl(baseName: String) : ResourceBundleMessageSource(), ModuleMessageSource {

    init {
        setBasename(baseName)
        this.defaultEncoding = "UTF-8"
    }

    /**
     * Resolves the given message code as key in the registered resource bundles,
     * returning the value found in the bundle as-is (without MessageFormat parsing).
     */
    override fun resolveCodeWithoutArguments(code: String, locale: Locale): String? {
        return super.resolveCodeWithoutArguments(code, locale)
    }

    /**
     * Resolves the given message code as key in the registered resource bundles,
     * returning the value found in the bundle as-is (without MessageFormat parsing).
     */
    override fun resolveCode(code: String, locale: Locale): MessageFormat? {
        return super.resolveCode(code, locale)
    }
}
