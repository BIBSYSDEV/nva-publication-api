package cucumber.permissions.ticket;

import static io.cucumber.junit.CucumberOptions.SnippetType.CAMELCASE;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    plugin = {"pretty", "summary"},
    snippets = CAMELCASE,
    features = {"src/test/resources/features/ticket"},
    tags = "not @ignore",
    glue = {"cucumber.permissions.ticket"}
)
public class RunTicketPermissionsCucumberTest {
}
