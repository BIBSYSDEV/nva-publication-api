package no.unit.nva.immutables;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@JsonSerialize // This uses the abstract/interface public static builder methods automatically.
@Value.Style(
    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
    init = "with*",
    strictBuilder = true,
    visibility = ImplementationVisibility.PUBLIC, // Generated class will be always public
    defaults = @Value.Immutable(copy = false)) // Disable copy methods by default
public @interface NvaStyle {
}



