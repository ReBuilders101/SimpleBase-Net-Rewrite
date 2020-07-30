/**
 * The {@code io} package contains classes to convert bytes and byte arrays to and
 * from primitives or more complex java objects.
 * <p>
 * The two main classes {@link dev.lb.simplebase.net.io.ReadableByteData} and
 * {@link dev.lb.simplebase.net.io.WritableByteData} are used when serializing and deserializing
 * network packets.
 * <p>
 * The {@link dev.lb.simplebase.net.io.ByteDataHelper} class contains static
 * helper methods to quickly convert {@code int}s to/from {@code byte}s.
 * <p>
 * Implementations of the two interfaces are available in the {@code dev.lb.simplebase.net.io.read} and
 * {@code dev.lb.simplebase.net.io.write} sub-packages. Those implementations are meant for API-internal
 * use only and are supplied by the API 
 */
package dev.lb.simplebase.net.io;