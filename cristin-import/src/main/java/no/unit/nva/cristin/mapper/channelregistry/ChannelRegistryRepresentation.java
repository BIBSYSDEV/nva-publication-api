package no.unit.nva.cristin.mapper.channelregistry;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChannelRegistryRepresentation {

    @CsvBindByName(column = "GammelID")
    private Integer nsdCode;

    @CsvBindByName(column = "NyID")
    private String pid;
}
