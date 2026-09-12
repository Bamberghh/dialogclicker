package me.bamberghh.dialogclicker

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.datafixers.util.Pair
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream

class PairListCodec<F, S>(private val firstCodec: Codec<F>, private val secondCodec: Codec<S>) :
    Codec<Pair<F, S>> {
    override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<Pair<F, S>, T>> =
        ops.getStream(input).map(Stream<T>::toList).flatMap {
            if (it.size != 2) DataResult.error { "expected list of size 2" } else {
                firstCodec.decode(ops, it[0]).flatMap { first ->
                    secondCodec.decode(ops, it[1]).map { second ->
                        Pair.of(Pair.of(first.first, second.first), input)
                    }
                }
            }
        }

    override fun <T> encode(input: Pair<F, S>, ops: DynamicOps<T>, prefix: T): DataResult<T> =
        ops.listBuilder()
            .add(firstCodec.encodeStart(ops, input.first))
            .add(secondCodec.encodeStart(ops, input.second))
            .build(prefix)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as PairListCodec<*, *>
        return firstCodec == that.firstCodec && secondCodec == that.secondCodec
    }

    override fun hashCode(): Int {
        return Objects.hash(firstCodec, secondCodec)
    }

    override fun toString(): String {
        return "PairListCodec[$firstCodec -> $secondCodec]"
    }
}

@Suppress("SameParameterValue")
fun <K, V> mapPairListCodec(keyCodec: Codec<K>, valueCodec: Codec<V>): Codec<MutableMap<K, V>> {
    return PairListCodec(keyCodec, valueCodec).listOf().xmap(
        { list -> list.associate { pair -> pair.first to pair.second }.toMutableMap() },
        { map -> map.map { (key, value) -> Pair.of(key, value) } }
    )
}

