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
package ru.juniperbot.common.worker.metrics.service;

import com.codahale.metrics.*;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.juniperbot.common.persistence.entity.StoredMetric;
import ru.juniperbot.common.persistence.repository.StoredMetricRepository;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.configuration.WorkerProperties;
import ru.juniperbot.common.worker.metrics.model.DiscordBotsGgStats;
import ru.juniperbot.common.worker.metrics.model.DiscordBotsOrgStats;
import ru.juniperbot.common.worker.metrics.model.PersistentMetric;
import ru.juniperbot.common.worker.metrics.model.TimeWindowChart;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final Object $persistMetricsLock = new Object[0];

    private static final String ORG_ENDPOINT = "https://discordbots.org/api/bots/{clientId}/stats";

    private static final String GG_ENDPOINT = "https://discord.bots.gg/api/v1/bots/{clientId}/stats";

    private RestTemplate restTemplate = new RestTemplate(CommonUtils.createRequestFactory());

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private StoredMetricRepository metricRepository;

    @Autowired
    private WorkerProperties workerProperties;

    @Override
    public Timer getTimer(String name) {
        return metricRegistry.timer(name);
    }

    @Override
    public Meter getMeter(String name) {
        return metricRegistry.meter(name);
    }

    @Override
    public Counter getCounter(String name) {
        return metricRegistry.counter(name);
    }

    @Override
    public TimeWindowChart getTimeChart(String name, long window, TimeUnit windowUnit) {
        Metric metric = metricRegistry.getMetrics().get(name);
        if (metric instanceof TimeWindowChart) {
            return (TimeWindowChart) metric;
        }
        TimeWindowChart chart = new TimeWindowChart(window, windowUnit);
        return metricRegistry.register(name, chart);
    }

    @PostConstruct
    public void init() {
        try {
            loadMetrics();
        } catch (Exception e) {
            log.warn("Could not load metrics from database", e);
        }
    }

    @Override
    @Async
    public void notifyProviders(JDA shard) {
        notifyProvider(shard, new DiscordBotsOrgStats(shard), ORG_ENDPOINT,
                workerProperties.getStats().getDiscordbotsOrgToken());
        notifyProvider(shard, new DiscordBotsGgStats(shard), GG_ENDPOINT,
                workerProperties.getStats().getDiscordbotsGgToken());
    }

    private void notifyProvider(JDA shard, Serializable stats, String endPoint, String token) {
        if (StringUtils.isEmpty(token)) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", token);
            HttpEntity<Serializable> request = new HttpEntity<>(stats, headers);
            ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, request, String.class,
                    shard.getSelfUser().getId());
            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                log.warn("Could not report stats {} to endpoint {}: response is {}", stats, endPoint,
                        response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Could not report stats {} to endpoint {}: {}", stats, endPoint, e.getMessage());
        }
    }

    private void loadMetrics() {
        List<StoredMetric> metrics = metricRepository.findAll();
        for (StoredMetric metric : metrics) {
            if (Counter.class.isAssignableFrom(metric.getType())) {
                Counter counter = getCounter(metric.getName());
                counter.inc(metric.getCount());
            }
            if (PersistentMetric.class.isAssignableFrom(metric.getType())) {
                try {
                    PersistentMetric persistent;
                    if (TimeWindowChart.class.isAssignableFrom(metric.getType())) {
                        persistent = getTimeChart(metric.getName(), 10, TimeUnit.MINUTES);
                    } else {
                        persistent = (PersistentMetric) metric.getType().getConstructor().newInstance();
                        metricRegistry.remove(metric.getName());
                        metricRegistry.register(metric.getName(), persistent);
                    }
                    persistent.fromMap(metric.getData());
                } catch (Exception e) {
                    log.warn("Could not initialize metric[name={},type={}]", metric.getName(), metric.getType(), e);
                }
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    @Synchronized("$persistMetricsLock")
    public void persistMetrics() {
        Map<String, Metric> metricMap = metricRegistry.getMetrics();
        if (MapUtils.isNotEmpty(metricMap)) {
            metricMap = metricMap.entrySet().stream()
                    .filter(e -> e.getKey().endsWith(".persist"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (MapUtils.isEmpty(metricMap)) {
            return;
        }

        metricMap.forEach((k, v) -> {
            StoredMetric storedMetric = getOrNewMetric(k, v);
            if (v instanceof Counter) {
                Counter counter = (Counter) v;
                storedMetric.setCount(counter.getCount());
            }
            if (v instanceof PersistentMetric) {
                PersistentMetric persistentMetric = (PersistentMetric) v;
                storedMetric.setData(persistentMetric.toMap());
            }
            metricRepository.save(storedMetric);
        });
    }

    @Override
    public void doWithTimer(String name, Runnable action) {
        doWithTimer(getTimer(name), action);
    }

    @Override
    public void doWithTimer(Timer timer, Runnable action) {
        final Timer.Context context = timer.time();
        try {
            action.run();
        } finally {
            context.stop();
        }
    }

    private StoredMetric getOrNewMetric(String name, Metric metric) {
        StoredMetric storedMetric = metricRepository.findByNameAndType(name, metric.getClass());
        if (storedMetric == null) {
            storedMetric = new StoredMetric();
            storedMetric.setName(name);
            storedMetric.setType(metric.getClass());
        }
        return storedMetric;
    }
}
