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
package ru.juniperbot.api.subscriptions.integrations;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.*;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.api.ApiProperties;
import ru.juniperbot.api.model.TwitchNotification;
import ru.juniperbot.common.persistence.entity.TwitchConnection;
import ru.juniperbot.common.persistence.repository.TwitchConnectionRepository;
import ru.juniperbot.common.support.MapPlaceholderResolver;
import ru.juniperbot.common.utils.CommonUtils;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TwitchSubscriptionServiceImpl extends BaseSubscriptionService<TwitchConnection, TwitchNotification, User> implements TwitchSubscriptionService {

    private static final Color TWITCH_COLOR = CommonUtils.hex2Rgb("64439A");

    @Autowired
    private ApiProperties apiProperties;

    private TwitchClient client;

    private TwitchHelix helix;

    private Set<Long> liveStreamsCache = Collections.synchronizedSet(new HashSet<>());

    private TwitchConnectionRepository repository;

    public TwitchSubscriptionServiceImpl(@Autowired TwitchConnectionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(apiProperties.getTwitch().getClientId())
                || StringUtils.isEmpty(apiProperties.getTwitch().getSecret())) {
            log.warn("No valid Twitch credentials provided");
            return;
        }
        client = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withClientId(apiProperties.getTwitch().getClientId())
                .withClientSecret(apiProperties.getTwitch().getSecret())
                .build();
        helix = client.getHelix();
        schedule(apiProperties.getTwitch().getUpdateInterval());
    }

    @Override
    protected synchronized void update() {
        if (client == null) {
            return;
        }
        log.info("Twitch channels notification started...");

        List<Long> userIds = repository.findActiveUserIds();

        if (userIds.isEmpty()) {
            log.info("No Twitch connections found to retrieve, exiting...");
            return;
        }

        Set<Stream> liveStreams = getLiveStreams(userIds);

        if (liveStreams.isEmpty()) {
            log.info("No live streams found for {} connections, exiting...", userIds.size());
            liveStreamsCache.clear();
            return;
        }

        Set<User> users = getUsersByIds(liveStreams.stream().map(Stream::getUserId).collect(Collectors.toList()));

        if (users.isEmpty()) {
            log.info("No users found for {} streams, exiting...", liveStreams.size());
            liveStreamsCache.clear();
            return;
        }

        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<Stream> streamsToNotify = liveStreams.stream()
                .filter(e -> !liveStreamsCache.contains(e.getId()))
                .collect(Collectors.toList());

        Set<Game> games = getGamesByIds(streamsToNotify.stream()
                .map(e -> String.valueOf(e.getGameId()))
                .collect(Collectors.toList()));
        Map<String, Game> gamesMap = games.stream().collect(Collectors.toMap(Game::getId, Function.identity()));

        AtomicInteger notifyCounter = new AtomicInteger();

        Lists.partition(streamsToNotify, 1000).forEach(e -> {
            List<TwitchConnection> toSave = new ArrayList<>(e.size());
            Map<Long, Stream> streamMap = e.stream().collect(Collectors.toMap(Stream::getUserId,
                    Function.identity()));
            repository.findActiveConnections(e.stream()
                    .map(Stream::getUserId)
                    .collect(Collectors.toSet()))
                    .forEach(c -> {
                        Stream stream = streamMap.get(c.getUserId());
                        User user = userMap.get(c.getUserId());
                        if (user != null && stream != null) {
                            if (updateIfRequired(user, c)) {
                                toSave.add(c);
                            }
                            Game game = stream.getGameId() != null
                                    ? gamesMap.get(String.valueOf(stream.getGameId())) : null;
                            if (notifyConnection(new TwitchNotification(user, stream, game), c)) {
                                notifyCounter.incrementAndGet();
                            }
                        }
                    });
            repository.saveAll(toSave);
        });

        liveStreamsCache.clear();
        liveStreamsCache.addAll(liveStreams.stream().map(Stream::getId).collect(Collectors.toSet()));
        log.info("Twitch channels notification finished: [Channels={}, Online={}, Notified={}]", userIds.size(),
                liveStreams.size(), notifyCounter.intValue());
    }

    private Set<User> getUsersByIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptySet();
        }
        Set<User> result = new HashSet<>(userIds.size());
        Lists.partition(userIds, 100).forEach(e -> {
            UserList resultList = helix
                    .getUsers("", e, null)
                    .execute();
            if (resultList != null && resultList.getUsers() != null) {
                result.addAll(resultList.getUsers());
            }
        });
        return result;
    }

    private Set<User> getUsersByNames(List<String> userNames) {
        if (CollectionUtils.isEmpty(userNames)) {
            return Collections.emptySet();
        }
        Set<User> result = new HashSet<>(userNames.size());
        Lists.partition(userNames, 100).forEach(e -> {
            UserList resultList = helix
                    .getUsers("", null, e)
                    .execute();
            if (resultList != null && resultList.getUsers() != null) {
                result.addAll(resultList.getUsers());
            }
        });
        return result;
    }

    private Set<Stream> getLiveStreams(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptySet();
        }
        Set<Stream> result = new HashSet<>(userIds.size());
        Lists.partition(userIds, 100).forEach(e -> {
            Set<Stream> batch = new HashSet<>(e.size());
            Set<String> cursors = new HashSet<>();
            fetchStreams("", cursors, batch, e);
            result.addAll(batch);
        });
        return result;
    }

    private void fetchStreams(String afterCursor, Set<String> cursors, Set<Stream> streams, List<Long> userIds) {
        StreamList result = helix
                .getStreams("", afterCursor, "", 100, null, null, null, userIds, null)
                .execute();
        if (result == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(result.getStreams())) {
            streams.addAll(result.getStreams()
                    .stream()
                    .filter(e -> "live".equals(e.getType()))
                    .collect(Collectors.toList()));
        }
        if (result.getPagination() != null) {
            String cursor = result.getPagination().getCursor();
            if (StringUtils.isNotEmpty(cursor) && cursors.add(cursor)) {
                fetchStreams(cursor, cursors, streams, userIds);
            }
        }
    }

    private Set<Game> getGamesByIds(List<String> gameIds) {
        if (CollectionUtils.isEmpty(gameIds)) {
            return Collections.emptySet();
        }
        Set<Game> result = new HashSet<>(gameIds.size());
        Lists.partition(gameIds, 100).forEach(e -> {
            GameList resultList = helix
                    .getGames("", e, null)
                    .execute();
            if (resultList != null && resultList.getGames() != null) {
                result.addAll(resultList.getGames());
            }
        });
        return result;
    }

    private boolean updateIfRequired(User user, TwitchConnection connection) {
        boolean updateRequired = false;
        if (!Objects.equals(user.getProfileImageUrl(), connection.getIconUrl())) {
            connection.setIconUrl(user.getProfileImageUrl());
            updateRequired = true;
        }
        if (!Objects.equals(user.getLogin(), connection.getLogin())) {
            connection.setLogin(user.getLogin());
            updateRequired = true;
        }
        if (!Objects.equals(user.getDisplayName(), connection.getName())) {
            connection.setName(user.getDisplayName());
            updateRequired = true;
        }
        return updateRequired;
    }

    @Override
    protected WebhookMessage createMessage(TwitchNotification notification, TwitchConnection connection) {
        User user = notification.getUser();
        Stream stream = notification.getStream();
        Game game = notification.getGame();
        String streamUrl = getLink(user);
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("streamer", user.getDisplayName());
        resolver.put("link", getLink(user));
        resolver.put("game", game != null ? game.getName() : "-");
        String announce = connection.getAnnounceMessage();
        if (StringUtils.isBlank(announce)) {
            announce = getMessage(connection, "discord.twitch.announce");
        }
        String content = BaseSubscriptionService.PLACEHOLDER.replacePlaceholders(announce, resolver);

        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setContent(content);

        if (connection.isSendEmbed()) {
            WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                    .setAuthor(new WebhookEmbed.EmbedAuthor(
                            CommonUtils.trimTo(user.getDisplayName(), MessageEmbed.TITLE_MAX_LENGTH),
                            user.getProfileImageUrl(),
                            streamUrl))
                    .setThumbnailUrl(user.getProfileImageUrl())
                    .setDescription(CommonUtils.trimTo(stream.getTitle(), MessageEmbed.TITLE_MAX_LENGTH))
                    .setColor(TWITCH_COLOR.getRGB())
                    .setImageUrl(stream.getThumbnailUrl(848, 480));

            embedBuilder.addField(new WebhookEmbed.EmbedField(false,
                    getMessage(connection, "discord.viewers.title"),
                    CommonUtils.formatNumber(stream.getViewerCount())));

            if (game != null) {
                embedBuilder.addField(new WebhookEmbed.EmbedField(false,
                        getMessage(connection, "discord.game.title"),
                        CommonUtils.trimTo(game.getName(), MessageEmbed.VALUE_MAX_LENGTH)));
            }
            builder.addEmbeds(embedBuilder.build());
        }
        return builder.build();
    }

    @Override
    protected TwitchConnection createConnection(User user) {
        TwitchConnection connection = new TwitchConnection();
        connection.setUserId(user.getId());
        connection.setLogin(user.getLogin());
        connection.setName(user.getDisplayName());
        connection.setDescription(user.getDescription());
        connection.setIconUrl(user.getProfileImageUrl());
        connection.setSendEmbed(true);
        return connection;
    }

    @Override
    public User getUser(String userName) {
        Set<User> users = getUsersByNames(Collections.singletonList(userName));
        return users.isEmpty() ? null : users.iterator().next();
    }

    private String getLink(User user) {
        return user != null ? "https://www.twitch.tv/" + user.getLogin() : null;
    }
}
