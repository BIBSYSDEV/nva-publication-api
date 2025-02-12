package cucumber.permissions.file;

public class RoleParser {

    private final String useraRole;

    public RoleParser(String useraRole) {
        this.useraRole = useraRole;
    }

    public boolean isFileCurator() {
        return useraRole.toLowerCase().contains("file curator");
    }

    public boolean isCuratorForGivenFile() {
        return useraRole.toLowerCase().contains("curator at x");
    }
}
