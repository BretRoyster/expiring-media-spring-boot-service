package com.bretroyster.inventory.service;

import com.bretroyster.inventory.domain.Media;
import com.bretroyster.inventory.service.util.RandomUtil;
import java.util.Map;
import java.util.Objects;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExpiringMediaService {

    private final Logger log = LoggerFactory.getLogger(ExpiringMediaService.class);

    private final int MEDIA_EXPIRATION_MINUTES = 1;

    // This map should be fairly empty most of the time
    private final Map<MediaKey, Media> expiringMedia = new ConcurrentHashMap<>();

    public String add(Media media) {
        MediaKey mk = new MediaKey(RandomUtil.generateActivationKey());

        if (expiringMedia.get(mk) != null) {
            log.error("Encountered duplicate Media key in expiringMedia cache!");
            return "";
        } else {
            expiringMedia.put(mk, media);
            return mk.key;
        }
    }

    public Media get(String key) {
        cleanExpired();
        return expiringMedia.remove(new MediaKey(key));
    }

    // Clean remaining links every day at 1:00 am.
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanLinks() {
        log.info("Running clean on expiring media links.");
        cleanExpired();
    }

    private void cleanExpired() {
        expiringMedia.entrySet().removeIf((entry) -> 
            entry.getKey().createTime < new DateTime().minusMinutes(MEDIA_EXPIRATION_MINUTES).getMillis());
    }

    private static class MediaKey {

        public final String key;
        public final Long createTime;

        private MediaKey(String key) {
            this.key = key;
            this.createTime = System.currentTimeMillis();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MediaKey other = (MediaKey) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }
    }
}
