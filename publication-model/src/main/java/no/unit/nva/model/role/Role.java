package no.unit.nva.model.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum Role {
    ACADEMIC_COORDINATOR("AcademicCoordinator"),
    ACTOR("Actor"),
    ADVISOR("Advisor"),
    ARCHITECT("Architect"),
    ARCHITECTURAL_PLANNER("ArchitecturalPlanner"),
    ARTIST("Artist"),
    ARTISTIC_DIRECTOR("ArtisticDirector"),
    AUDIO_VISUAL_CONTRIBUTOR("AudioVisualContributor"),
    CHOREOGRAPHER("Choreographer"),
    COLLABORATION_PARTNER("CollaborationPartner"),
    COMPOSER("Composer"),
    CONDUCTOR("Conductor"),
    CONSULTANT("Consultant"),
    CONSERVATOR("Conservator"),
    CONTACT_PERSON("ContactPerson"),
    COSTUME_DESIGNER("CostumeDesigner"),
    CREATOR("Creator"),
    CURATOR("Curator"),
    CURATOR_ORGANIZER("CuratorOrganizer"),
    DANCER("Dancer"),
    DATA_COLLECTOR("DataCollector"),
    DATA_CURATOR("DataCurator"),
    DATA_MANAGER("DataManager"),
    DESIGNER("Designer"),
    DIRECTOR("Director"),
    DISTRIBUTOR("Distributor"),
    DRAMATIST("Dramatist"),
    DRAMATURGE("Dramaturge"),
    EDITOR("Editor"),
    EDITORIAL_BOARD_MEMBER("EditorialBoardMember"),
    EXHIBITION_DESIGNER("ExhibitionDesigner"),
    FUNDER("Funder"),
    HOSTING_INSTITUTION("HostingInstitution"),
    ILLUSTRATOR("Illustrator"),
    INTERIOR_ARCHITECT("InteriorArchitect"),
    INTERVIEW_SUBJECT("InterviewSubject"),
    JOURNALIST("Journalist"),
    LANDSCAPE_ARCHITECT("LandscapeArchitect"),
    LIBRETTIST("Librettist"),
    LIGHT_DESIGNER("LightDesigner"),
    MUSICIAN("Musician"),
    MUSEUM_EDUCATOR("MuseumEducator"),
    ORGANIZER("Organizer"),
    OTHER("RoleOther"),
    PRODUCER("Producer"),
    PHOTOGRAPHER("Photographer"),
    PRODUCTION_DESIGNER("ProductionDesigner"),
    PROGRAMME_LEADER("ProgrammeLeader"),
    PROGRAMME_PARTICIPANT("ProgrammeParticipant"),
    PROJECT_LEADER("ProjectLeader"),
    PROJECT_MANAGER("ProjectManager"),
    PROJECT_MEMBER("ProjectMember"),
    REGISTRAR("Registrar"),
    REGISTRATION_AGENCY("RegistrationAgency"),
    REGISTRATION_AUTHORITY("RegistrationAuthority"),
    RELATED_PERSON("RelatedPerson"),
    RESEARCHER("Researcher"),
    RESEARCH_GROUP("ResearchGroup"),
    RIGHTS_HOLDER("RightsHolder"),
    SCENOGRAPHER("Scenographer"),
    SCREENWRITER("Screenwriter"),
    SOLOIST("Soloist"),
    SOUND_DESIGNER("SoundDesigner"),
    SUPERVISOR("Supervisor"),
    TRANSLATOR_ADAPTER("TranslatorAdapter"),
    VFX_SUPERVISOR("VfxSupervisor"),
    VIDEO_EDITOR("VideoEditor"),
    WORK_PACKAGE_LEADER("WorkPackageLeader"),
    WRITER("Writer");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid Role, expected one of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    Role(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {

        return value;
    }



    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return enum
     */
    @JsonCreator
    public static Role parse(String value) {
        // TODO: inline "else" method following migration
        return isDeprecatedRole(value)
                ? Role.OTHER
                : inlineableParseMethod(value);
    }

    private static boolean isDeprecatedRole(String value) {
        return "Other".equalsIgnoreCase(value)
               || "Sponsor".equalsIgnoreCase(value);
    }

    private static Role inlineableParseMethod(String value) {
        return stream(values())
                .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        format(ERROR_MESSAGE_TEMPLATE, value, stream(Role.values())
                                .map(Role::toString).collect(joining(DELIMITER)))));
    }
}
