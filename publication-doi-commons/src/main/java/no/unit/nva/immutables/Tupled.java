package no.unit.nva.immutables;

import org.immutables.value.Value;

/**
 * Tupled annotation will be used to generate simple tuples in reverse-style, having construction methods of all
 * annotations.
 */
@Value.Style(// Tupled annotation will serve as both immutable and meta-annotated style annotation
    typeAbstract = "*Def",
    typeImmutable = "*",
    allParameters = true, // all attributes will become constructor parameters
    // as if they are annotated with @Value.Parameter
    visibility = Value.Style.ImplementationVisibility.PUBLIC, // Generated class will be always public
    defaults = @Value.Immutable(builder = false)) // Disable copy methods and builder
public @interface Tupled {}