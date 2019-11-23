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
package ru.juniperbot.common.worker.modules.audit.model;

import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import ru.juniperbot.common.model.AuditActionType;
import ru.juniperbot.common.persistence.entity.AuditAction;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.persistence.entity.base.NamedReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class AuditActionBuilder {

    @Getter(AccessLevel.NONE)
    protected final AuditAction action;

    protected Map<String, byte[]> attachments = new HashMap<>();

    protected AuditActionBuilder(long guildId, AuditActionType actionType) {
        this.action = new AuditAction(guildId);
        this.action.setActionDate(new Date());
        this.action.setActionType(actionType);
        this.action.setAttributes(new HashMap<>());
    }

    public AuditActionBuilder withUser(User user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(Member user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(LocalUser user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(LocalMember user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(User user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(Member user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(LocalUser user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(LocalMember user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withChannel(GuildChannel channel) {
        this.action.setChannel(getReference(channel));
        return this;
    }

    public AuditActionBuilder withAttribute(String key, Object value) {
        this.action.getAttributes().put(key, getReferenceForObject(value));
        return this;
    }

    public AuditActionBuilder withAttachment(String key, byte[] data) {
        this.attachments.put(key, data);
        return this;
    }

    private Object getReferenceForObject(Object object) {
        if (object instanceof User) {
            return getReference((User) object);
        }
        if (object instanceof LocalUser) {
            return getReference((LocalUser) object);
        }
        if (object instanceof Member) {
            return getReference((Member) object);
        }
        if (object instanceof LocalMember) {
            return getReference((LocalMember) object);
        }
        if (object instanceof GuildChannel) {
            return getReference((GuildChannel) object);
        }
        return object;
    }

    private NamedReference getReference(User user) {
        return user != null ? new NamedReference(user.getId(), user.getName()) : null;
    }

    private NamedReference getReference(LocalUser user) {
        return user != null ? new NamedReference(user.getUserId(), user.getName()) : null;
    }

    private NamedReference getReference(Member member) {
        return member != null ? new NamedReference(member.getUser().getId(), member.getEffectiveName()) : null;
    }

    private NamedReference getReference(LocalMember member) {
        return member != null ? new NamedReference(member.getUser().getUserId(), member.getEffectiveName()) : null;
    }

    private NamedReference getReference(GuildChannel channel) {
        return channel != null ? new NamedReference(channel.getId(), channel.getName()) : null;
    }

    public abstract AuditAction save();
}
