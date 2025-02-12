package cucumber.permissions.file;

public class RoleParser {

    private final String userRole;

    public RoleParser(String userRole) {
        this.userRole = userRole;
    }

    public boolean isFileCurator() {
        return userRole.toLowerCase().contains("file curator");
    }

    public boolean isCuratorForGivenFile() {
        return userRole.toLowerCase().contains("curator at x");
    }
}
