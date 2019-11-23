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
package ru.juniperbot.common.worker.modules.audit.provider;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.AuditAction;
import ru.juniperbot.common.persistence.entity.base.NamedReference;

@ForwardProvider(AuditActionType.VOICE_MOVE)
public class VoiceMoveAuditForwardProvider extends VoiceAuditForwardProvider {

    public static final String OLD_CHANNEL = "old_channel";

    @Override
    protected void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder) {
        if (action.getChannel() == null || action.getUser() == null) {
            return;
        }

        embedBuilder.setDescription(messageService.getMessage("audit.message.voice.move.message",
                getReferenceContent(action.getUser(), false)));
        addChannelField(action, embedBuilder);

        NamedReference oldChannel = action.getAttribute(OLD_CHANNEL, NamedReference.class);
        if (oldChannel != null) {
            embedBuilder.addField(messageService.getMessage("audit.message.voice.move.old.title"),
                    getReferenceContent(oldChannel, true), true);
        }

        embedBuilder.setFooter(messageService.getMessage("audit.member.id", action.getUser().getId()), null);
    }
}
