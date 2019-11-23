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
package ru.juniperbot.module.ranking

import lombok.extern.slf4j.Slf4j
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import ru.juniperbot.common.worker.event.intercept.Filter
import ru.juniperbot.common.worker.event.intercept.FilterChain
import ru.juniperbot.common.worker.event.intercept.MemberMessageFilter
import ru.juniperbot.module.ranking.service.RankingService

@Slf4j
@Order(Filter.POST_FILTER)
@Component
class RankingFilter : MemberMessageFilter() {

    @Autowired
    lateinit var rankingService: RankingService

    override fun doInternal(event: GuildMessageReceivedEvent, chain: FilterChain<GuildMessageReceivedEvent>) {
        try {
            rankingService.onMessage(event)
        } finally {
            chain.doFilter(event)
        }
    }
}
