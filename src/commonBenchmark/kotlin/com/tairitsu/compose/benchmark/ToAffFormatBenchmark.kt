package com.tairitsu.compose.benchmark

import com.tairitsu.compose.toAffFormat
import kotlinx.benchmark.*
import kotlin.random.Random
import kotlin.time.TimeSource

@State(Scope.Benchmark)
open class ToAffFormatBenchmark {
    @Param("100000", "500000", "1000000")
    var size: Int = 0

    private lateinit var numbers: DoubleArray
    private var seed = 0L

    @Setup
    fun prepare() {
        val random = Random(seed)
        numbers = DoubleArray(size) { random.nextDouble() * 1e6 - 5e5 }
        seed = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    }

    @TearDown
    fun clearup() {
        numbers = DoubleArray(size)
    }

    /*
    How fast we calculate toAffFormat

    jvmBenchmark summary:
    Benchmark                        (size)   Mode  Cnt   Score   Error  Units
    ToAffFormatBenchmark.benchmark   100000  thrpt    5  22.728 ± 0.293  ops/s
    ToAffFormatBenchmark.benchmark   500000  thrpt    5   4.620 ± 0.045  ops/s
    ToAffFormatBenchmark.benchmark  1000000  thrpt    5   2.315 ± 0.068  ops/s

    nativeBenchmark summary:
    Benchmark                        (size)   Mode  Cnt   Score   Error    Units
    ToAffFormatBenchmark.benchmark   100000  thrpt    5  29.983 ± 0.257  ops/sec
    ToAffFormatBenchmark.benchmark   500000  thrpt    5   6.310 ± 0.091  ops/sec
    ToAffFormatBenchmark.benchmark  1000000  thrpt    5   3.265 ± 0.097  ops/sec
     */
    @Benchmark
    fun benchmark(bh: Blackhole) {
        for (i in numbers.indices) {
            bh.consume(numbers[i].toAffFormat())
        }
    }
}