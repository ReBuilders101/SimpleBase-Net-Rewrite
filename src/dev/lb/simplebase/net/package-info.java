/**
 * <h2>The {@code dev.simplebase.net} package and its subpackages contain the networking code for the SimpleBase API.</h2>
 * This document will give a short explaination of the concepts of the API.
 * <p>
 * <b>Basic concept</b><br>
 * text
 * </p>
 * <p>
 * <b>{@code NetworkID}s</b><br>
 * A NetworkID object (instance of {@link dev.lb.simplebase.net.id.NetworkID}) is an identifier for any network party (a client or a server).
 * All NetworkIDs store a name that describes the identified component. This name is not required to be unique and is meant to be descriptive for
 * logging/output etc. Comparison is done by instance equality.
 * <br><b>Functions</b><br>
 * A NetworkID on its own is nothing more than a label, but certain implementations can implement {@code NetworkIDFunction}s for additional behavior.
 * Certain functions can allow a NetworkID to be used to connect to a server or bind a server to a port. The NetworkID class offers several static methods
 * to create IDs with different functions.
 * </p>
 * <p>
 * <b>The {@code NetworkManager}</b>
 * 
 * </p>
 */
package dev.lb.simplebase.net;