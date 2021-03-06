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
package ru.juniperbot.module.mafia.service.individual;

import net.dv8tion.jda.api.entities.PrivateChannel;
import org.springframework.stereotype.Component;
import ru.juniperbot.module.mafia.model.MafiaInstance;
import ru.juniperbot.module.mafia.model.MafiaPlayer;
import ru.juniperbot.module.mafia.model.MafiaRole;
import ru.juniperbot.module.mafia.model.MafiaState;
import ru.juniperbot.module.mafia.service.DayHandler;

@Component
public class CopHandler extends IndividualHandler<DayHandler> {

    public CopHandler() {
        super(MafiaRole.COP, MafiaState.NIGHT_COP, MafiaState.MEETING);
    }

    @Override
    protected void choiceAction(MafiaInstance instance, MafiaPlayer target, PrivateChannel channel) {
        channel.sendMessage(messageService.getMessage(target.getRole().isMafia()
                ? "mafia.cop.choice.positive" : "mafia.cop.choice.negative")).queue();
    }

    @Override
    protected Class<DayHandler> getNextType() {
        return DayHandler.class;
    }
}
