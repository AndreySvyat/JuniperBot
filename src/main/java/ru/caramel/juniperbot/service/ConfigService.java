package ru.caramel.juniperbot.service;

import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

public interface ConfigService {

    ConfigDto getConfig(long serverId);

    void saveConfig(ConfigDto dto, long serverId);

    GuildConfig getOrCreate(long serverId);
}