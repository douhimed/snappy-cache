package org.adex.models.benchmarks;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class LRUCachePerformanceTest {

    @Test
    public void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
                .include(LruCacheBenchmark.class.getSimpleName())
                .result("results.json")
                .resultFormat(ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }
}

