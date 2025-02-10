package no.unit.nva.publication.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.publication.example.ClosedFileModel;
import no.unit.nva.publication.example.FileDto;
import no.unit.nva.publication.example.OpenFileModel;
import no.unit.nva.publication.example.PublicationDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.junit.Assert.assertEquals;

public class ScratchPadTest {

    @Test
    void shouldRoundTripFiles() throws JsonProcessingException {
        var openFile = new OpenFileModel("Open File");
        var closedFile = new ClosedFileModel("Closed File");

        var openFileDto = openFile.toDto();
        var closedFileDto = closedFile.toDto();

        var jsonOpenFile = dtoObjectMapper.writeValueAsString(openFileDto);
        var jsonClosedFile = dtoObjectMapper.writeValueAsString(closedFileDto);

        var roundTrippedOpenFileDto = dtoObjectMapper.readValue(jsonOpenFile, FileDto.class);
        var roundTrippedClosedFileDto = dtoObjectMapper.readValue(jsonClosedFile, FileDto.class);

        var roundTrippedOpenFile = roundTrippedOpenFileDto.toModel();
        var roundTrippedClosedFile = roundTrippedClosedFileDto.toModel();

        assertEquals(openFile, roundTrippedOpenFile);
        assertEquals(closedFile, roundTrippedClosedFile);

        System.out.println(jsonOpenFile);
    }

    @Test
    void shouldRoundTripFilesInList() throws JsonProcessingException {
        var openFile = new OpenFileModel("Open File");
        var closedFile = new ClosedFileModel("Closed File");

        var openFileDto = openFile.toDto();
        var closedFileDto = closedFile.toDto();
        var publicationDto = new PublicationDto("Title", List.of(openFileDto, closedFileDto));

        var jsonOpenFile = dtoObjectMapper.writeValueAsString(openFileDto);
        var jsonClosedFile = dtoObjectMapper.writeValueAsString(closedFileDto);
        var jsonPublication = dtoObjectMapper.writeValueAsString(publicationDto);

        var roundTrippedPublicationDto = dtoObjectMapper.readValue(jsonPublication, PublicationDto.class);

        var files = roundTrippedPublicationDto.files().stream().map(FileDto::toModel).toList();

        System.out.println(jsonOpenFile);
    }
}
