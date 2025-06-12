package org.griddynamics.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "lucene")
public interface LuceneProperties {
    String productsPath();
}
