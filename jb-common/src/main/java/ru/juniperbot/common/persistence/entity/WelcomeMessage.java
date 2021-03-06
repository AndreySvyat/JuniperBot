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
package ru.juniperbot.common.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.persistence.entity.base.GuildEntity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "welcome_message")
@ToString
@Getter
@Setter
@NoArgsConstructor
public class WelcomeMessage extends GuildEntity {
    private static final long serialVersionUID = -3872054410668142206L;

    @Type(type = "jsonb")
    @Column(name = "join_roles", columnDefinition = "json")
    private List<Long> joinRoles;

    @Column
    private boolean restoreRoles;

    @Column
    private boolean restoreNickname;

    @Type(type = "jsonb")
    @Column(name = "roles_to_restore", columnDefinition = "json")
    private List<Long> rolesToRestore;

    @Column(name = "join_enabled")
    private boolean joinEnabled;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "join_template_id")
    private MessageTemplate joinTemplate;

    @Column(name = "join_dm_enabled")
    private boolean joinDmEnabled;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "join_dm_template_id")
    private MessageTemplate joinDmTemplate;

    @Column(name = "leave_enabled")
    private boolean leaveEnabled;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "leave_template_id")
    private MessageTemplate leaveTemplate;

    public WelcomeMessage(long guildId) {
        this.guildId = guildId;
    }
}
