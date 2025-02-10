package no.unit.nva.publication.example;

import java.util.List;

public record PublicationDto(String title, List<FileDto> files) {}
