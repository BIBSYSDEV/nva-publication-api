package cucumber;

import static io.cucumber.junit.CucumberOptions.SnippetType.CAMELCASE;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"junit", "summary"}, snippets = CAMELCASE,
    features = {"src/test/resources/features"}
)
public class CucumberRunner {

}
