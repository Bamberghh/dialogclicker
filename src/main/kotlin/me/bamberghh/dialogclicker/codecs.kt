package me.bamberghh.dialogclicker

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.datafixers.util.Pair
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

class PairListCodec<F, S>(private val firstCodec: Codec<F>, private val secondCodec: Codec<S>) :
    Codec<Pair<F, S>> {
    override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<Pair<F, S>, T>> {
        return ops.getStream(input).flatMap { stream ->
            val list = stream.limit(3).collect(Collectors.toList())
            if (list.size != 2) {
                return@flatMap DataResult.error { "expected list of size 2" }
            }
            val firstAny = list[0]
            val firstResult = firstCodec.decode(ops, firstAny)
            if (firstResult is DataResult.Error) {
                return@flatMap DataResult.Error(firstResult.messageSupplier, Optional.empty(), firstResult.lifecycle)
            }
            val secondAny = list[1]
            val secondResult = secondCodec.decode(ops, secondAny)
            if (secondResult is DataResult.Error) {
                return@flatMap DataResult.Error(secondResult.messageSupplier, Optional.empty(), secondResult.lifecycle)
            }
            val pair = Pair.of(firstResult.result().get().first, secondResult.result().get().first)
            DataResult.success(Pair.of(pair, input))
        }
    }

    override fun <T> encode(input: Pair<F, S>, ops: DynamicOps<T>, prefix: T): DataResult<T> {
        val listBuilder = ops.listBuilder()
        listBuilder.add(firstCodec.encodeStart<T>(ops, input.first))
        listBuilder.add(secondCodec.encodeStart<T>(ops, input.second))
        return listBuilder.build(prefix)
    }

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

