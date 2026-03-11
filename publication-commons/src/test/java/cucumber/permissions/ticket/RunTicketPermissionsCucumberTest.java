package cucumber.permissions.ticket;

import io.cucumber.core.options.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(
    key = Constants.FEATURES_PROPERTY_NAME,
    value = "src/test/resources/features/ticket")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "cucumber.permissions.ticket")
public class RunTicketPermissionsCucumberTest {}
