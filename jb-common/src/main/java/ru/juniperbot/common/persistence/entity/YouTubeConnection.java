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
import lombok.Setter;
import ru.juniperbot.common.persistence.entity.base.BaseSubscriptionEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "youtube_connection")
public class YouTubeConnection extends BaseSubscriptionEntity {
    private static final long serialVersionUID = 2146901528074674595L;

    @ManyToOne
    @JoinColumn(name = "channel_id")
    private YouTubeChannel channel;

    @Column
    private String description;

    @Column
    private String announceMessage;

    @Column
    private boolean sendEmbed;

}
